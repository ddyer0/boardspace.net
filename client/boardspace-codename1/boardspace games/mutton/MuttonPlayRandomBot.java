package mutton;

import com.codename1.ui.geom.Rectangle;

import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;
import java.util.*;
import lib.*;
import lib.Random;

/**
 * The Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * In general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * 
 * This robot player will play a random game of Mutton.
 * As the wolf:
 *    When eating, a random sheep that is a valid meal will be picked.  If there are no sheep
 *    that are valid meals, then a random wolf that is a valid meal will be eaten.
 * As the farmer:
 *    A random suspect from the most recent eaten victim will be shot.
 *    The robot will *never* go on a rampage.
 *    If shotgun is enabled, then all of the scared sheep will be moved to random empty
 *    cells on the board.
 * 
 * Yes, this is a particularly stupid farmer player... :)
 * 
 * Note:
 *    This mutton robot picks a random valid move each time.  It does not use
 *    any of the typical min/max type of board evaluation.
 *    So, this robot really wants to just implement the SimpleRobotProtocol.
 *    However, the sample code provided (Hex) uses the full min/max search engine
 *    and only exposes those interfaces used by that engine and (presumably) hides 
 *    other important work that would need to be done for a SimpleRobotProtocol
 *    (run(), for example...).
 * 
 *    So, this code pretends to do min/max, but doesn't really.
 *    It implements DoFullMove() to return a move, but the rest of the min/max
 *    functions are stubbed out and never called.
 * 
 * @author rwalter
 *
 */
public class MuttonPlayRandomBot
       extends commonRobot<MuttonGameBoard>
       implements Runnable, MuttonConstants, RobotProtocol
{	

	// Random number generator used by the robot player.
	Random randGen = new Random();

	// Debug flag that will cause the robot to be very loquacious when enabled.
	boolean bverbose = false;

	/**
	 * Constructor for a new random robot player.
	 */
	public MuttonPlayRandomBot () {
	}




	/**
	 * Prepare the robot, but don't start making moves.  G is the game object, gboard
	 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
	 * are parameters from the applet that can be interpreted as desired.  The debugging 
	 * menu items "set robotlevel(n)" set the value of "strategy".  Evaluator is not
	 * really used at this point, but was intended to be the class name of a plugin
	 * evaluator class.
	 *
	 * For Mutton, the strategy level is used by the newRobotPlayer() in MuttonGameViewer
	 * to create the correct robot player.  So, it is unused here.  This may change in the
	 * future depending on how robot player development goes...
	 */
	public void InitRobot (ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
	                       String evaluator, int strategy) {
		InitRobot(newParam, info, strategy);
		GameBoard = (MuttonGameBoard) gboard;
		board = new MuttonGameBoard (GameBoard.gametype, GameBoard.randomKey);
		if (bverbose) System.out.println("Robot: InitRobot() Original Board = " + gboard + ", my clone = " + board);
		MONTEBOT = (strategy==MONTEBOT_LEVEL);
	}

	/** PrepareToMove is called in the thread of the main game run loop at 
	 * a point where it is appropriate to start a move.  We must capture the
	 * board state at this point, so that when the robot runs it will not
	 * be affected by any subsequent changes in the real game board state.
	 * The canonical error here was the user using the < key before the robot
	 * had a chance to capture the board state.
	 */
	public void PrepareToMove (int playerIndex) {
		if (bverbose) System.out.println("Robot: Entering PrepareToMove()");

		// use this for a friendly robot that shares the board class
		board.copyFrom(GameBoard);

		// check that we got a good copy.  Not expensive to do this once per move
		board.sameboard(GameBoard);
	}

	public commonMove DoFullMove() {
		if (bverbose) System.out.println("Robot: Entering DoFullMove()");

		if (bverbose) {
			if (board.whoseTurn == board.getFarmerId()) {
				System.out.println("Robot: I'm the farmer");
			} else {
				System.out.println("Robot: I'm the wolf");
			}
		}

		MuttonMoveSpec theMove;

		switch (board.getState()) {
		case GAMEOVER_STATE :
			theMove = new MuttonMoveSpec("Done", board.whoseTurn);
			break;
		case FARMER_CONFIGURING_BOARD :
			theMove = new MuttonMoveSpec("Done_Relocating", board.whoseTurn);
			break;
		case WOLF_HIDING_STATE :
			if (board.getWolfWinTarget() >= 15) {
				// If the other player has set the wolf win target too high
				// (made it harder for the wolf player to win), then I want
				// to play as the farmer, not the wolf.
				theMove = new MuttonMoveSpec(SWAP, board.whoseTurn);
			} else {
				theMove = doWolfHiding();
			}
			break;
		case WOLF_CHOOSING_MEAL_STATE :
			theMove = doWolfEat();
			break;
		case WOLF_MOVING_SHEEP_STATE :
		case FARMER_MOVING_SHEEP_STATE :
			theMove = doRelocateMove();
			break;
		case FARMER_CHOOSING_TARGETS_STATE :
			theMove = doFarmerShoot();
			break;
		default :
			System.out.println("Robot: Unexpected board state: " + board.getState());
			theMove = null;
			break;
		}

		if (bverbose) {
			if (theMove != null) {
				System.out.println("Robot Move: " + theMove.moveString());
			} else {
				System.out.println("Robot Move: null");
			}
		}

		return theMove;
	}

	/*
	 * Pick 4 random sheep to hide the wolves in.
	 */
	private MuttonMoveSpec doWolfHiding () {
		// Create a vector of the 26 sheep.
		Vector<Integer> v = new Vector<Integer> ();
		for (int i = 0; i < 25; i++) {
			v.addElement(Integer.valueOf(i));
		}

		// Shuffle it.
		randGen.shuffle(v);

		// Create the move string 
		String moveString = "Wolves_Hidden";
		for (int i = 0; i < 4; i++) {
			moveString += (" " + v.elementAt(i).toString());
		}

		// Return the move to make
		return new MuttonMoveSpec (moveString, board.whoseTurn);
	}

	/*
	 * Pick a random animal for a wolf to eat.
	 * Prefer eating sheep to eating a fellow wolf.
	 */
	private MuttonMoveSpec doWolfEat () {
		// Try to eat a sheep...
		Vector<MuttonCell> v = board.getValidMeals(false);
		if (v.size() == 0) {
			// ... but if there aren't any, then eat a wolf.
			v = board.getValidMeals(true);
		}

		// If no meals, then we must pass.
		if (v.size() == 0) {
			return new MuttonMoveSpec ("Eat", board.whoseTurn);
		} else {
			// return the eat move.
			MuttonCell victimCell = v.elementAt(Random.nextInt(randGen, v.size()));
			return new MuttonMoveSpec ("Eat " + victimCell.getSheepId(), board.whoseTurn);
		}
	}

	/*
	 * Move a suspect away from the most recent victim to a random valid spot
	 * on the board.
	 * If all suspects have been moved, then send a done move.
	 */
	private MuttonMoveSpec doRelocateMove () {
		MuttonCell suspectCell = board.getASuspectToMove();
		if (suspectCell == null) {
			// No more suspects to move, so issue a "Done" message
			return new MuttonMoveSpec ("Done_Relocating", board.whoseTurn);
		}

		// Find a random space to move it to.
		Vector<MuttonCell> v = board.getValidEmptyCells();
		MuttonCell targetCell = v.elementAt(Random.nextInt(randGen, v.size()));
		return new MuttonMoveSpec ("Relocate " + suspectCell.getSheepId() + " " + targetCell.getCol() + " " + targetCell.getRow(), board.whoseTurn);
	}

	/*
	 * Pick a random suspect from the most recent wolf eating and shoot one of them.
	 */
	private MuttonMoveSpec doFarmerShoot () {
		// Get the most recent suspects
		Vector<MuttonCell> v = board.getMostRecentSuspects();
		if (v.size() == 0) {
			// If there are no most recent suspects, then the wolf player must have been
			// forced to pass because there were no valid meals.  In this case, we want
			// to shoot one of the sheep that has no neighbors, since that is the only
			// case left.
			v = board.getLonelySheep();
		}

		MuttonCell targetCell = v.elementAt(Random.nextInt(randGen, v.size()));
		return new MuttonMoveSpec ("Shoot " + targetCell.getSheepId(), board.whoseTurn);
	}

/***************************************************************************
    These methods are needed for commonRobot, but aren't actually used by
    the Mutton robot.
*/
	
	public boolean Depth_Limit (int current, int max) {
		if (bverbose) System.out.println("Robot Entering: Depth_Limit()");
	    //  Auto-generated method stub
		return true;
	}

    public CommonMoveStack  List_Of_Legal_Moves() {
		if (bverbose) System.out.println("Robot Entering: List_Of_Legal_Moves()");
	    //  Auto-generated method stub
	    return null;
    }

    public void Make_Move(commonMove arg0) {
		if (bverbose) System.out.println("Robot Entering: Make_Move()");
	    //  Auto-generated method stub
	    
    }

    public void StaticEval() {
		if (bverbose) System.out.println("Robot Entering: StaticEval()");
	    //  Auto-generated method stub
	    
    }

    public double Static_Evaluate_Position(commonMove arg0) {
		if (bverbose) System.out.println("Robot Entering: Static_Evaluate_Position()");
	    //  Auto-generated method stub
	    return 0;
    }

    public void Unmake_Move(commonMove arg0) {
		if (bverbose) System.out.println("Robot Entering: Unmake_Move()");
	    //  Auto-generated method stub
	    
    }

	public boolean RobotPlayerEvent(int x, int y, Rectangle startrect) {
		if (bverbose) System.out.println("Robot Entering: RobotPlayerEvent()");
		return false;
	}


}
