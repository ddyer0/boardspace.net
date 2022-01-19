package exxit;

import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

import lib.*;

/** 
 * Exxit uses alpha-beta only
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * TODO: make the robot exit if it has lost
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * @author ddyer
 *
 */
public class ExxitPlay extends commonRobot<ExxitGameBoard> implements Runnable,
    RobotProtocol
{	private boolean KILLER_HEURISTIC = false;
	private boolean SAVE_TREE = false;				// debug flag for the search driver
	private int DUMBOT_DEPTH = 4;
	private int SMARTBOT_DEPTH = 6;
	private int BESTBOT_DEPTH = 9;
	private int WEAKBOT_DEPTH = 3;
	private int MAX_DEPTH = DUMBOT_DEPTH;
	private final double VALUE_OF_WIN = 1000.0;
	private final double VALUE_OF_DRAW = -100;
	    /* strategies */
	private int Strategy = DUMBOT_LEVEL;
    

    /* constructor */
    public ExxitPlay()
    {
    }


/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	Exxitmovespec mm = (Exxitmovespec)m;
        board.UnExecute(mm);
    }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   Exxitmovespec mm = (Exxitmovespec)m;
        board.RobotExecute(mm);
    }

/** return an enumeration of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {
        return(board.GetListOfMoves());
    }
    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    double ScoreForPlayer(ExxitGameBoard evboard,int player,boolean print)
    {	boolean win = evboard.WinForPlayer(player);
     	// score wins as slightly better if in fewer moves
    	double val = win ? VALUE_OF_WIN+1.0/(1+board.robotDepth) : 0.0;		// points for a win, less for everything else
    	if(print && win) { System.out.println("+ win =");}

    	// basic evaluation, mobility weighted by piece importance
    	if(!win) 
    	{ switch(Strategy)
    		{
    		default: throw G.Error("Not expecting strategy %s",Strategy);
    		case WEAKBOT_LEVEL:
   			case DUMBOT_LEVEL: val = evboard.simpleEvaluation(player,print);
   				break;
   			case SMARTBOT_LEVEL: val = evboard.nextEvaluation(player,print);
   				break;
   			case BESTBOT_LEVEL: val = evboard.maxEvaluation(player,print);
   				break;
    			
    		}
    	}

     	return(val);
    }
    
    /**
     * this is it! just tell me that the position is worth.  
     */
    public double Static_Evaluate_Position(commonMove m)
    {	int playerindex = m.player;
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,nextPlayer[playerindex],false);
        // score wins alone, to avoid the "lesser loss" syndrone, where
        // the robot commits suicide because it makes the overall position 
        // look "better" than letting the opponent win.
        if(val0>=VALUE_OF_WIN) 
        	{ if(val1>=VALUE_OF_WIN) 
        		{ return(VALUE_OF_DRAW+val0-val1); // simultaneous win is a draw
        		}
        	  return(val0); 
        	}
         else if(val1>=VALUE_OF_WIN) { return(-val1); }
        return(val0-val1);
    }
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {
            ExxitGameBoard evboard = GameBoard.cloneBoard();
            double val0 = ScoreForPlayer(evboard,FIRST_PLAYER_INDEX,true);
            double val1 = ScoreForPlayer(evboard,SECOND_PLAYER_INDEX,true);
            System.out.println("Eval is "+ val0 +" "+val1+ " = " + (val0-val1));
    }


/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.
 */
 public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,  String evaluator, int strategy)
    {
        InitRobot(newParam, info, strategy);
        GameBoard = (ExxitGameBoard) gboard;
        board = GameBoard.cloneBoard();
        Strategy = strategy;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy %s",Strategy);
        case WEAKBOT_LEVEL: MAX_DEPTH = WEAKBOT_DEPTH; break;
        case DUMBOT_LEVEL:   MAX_DEPTH = DUMBOT_DEPTH; break;
        case SMARTBOT_LEVEL: MAX_DEPTH = SMARTBOT_DEPTH;break;
        case BESTBOT_LEVEL: MAX_DEPTH = BESTBOT_DEPTH; break;
        }
    }
    /** copy the game board, in preparation for a search */
    public void InitBoardFromGame()
    {
        board.copyFrom(GameBoard);
    }
    
/** PrepareToMove is called in the thread of the main game run loop at 
 * a point where it is appropriate to start a move.  We must capture the
 * board state at this point, so that when the robot runs it will not
 * be affected by any subsequent changes in the real game board state.
 * The canonical error here was the user using the < key before the robot
 * had a chance to capture the board state.
 */
public void PrepareToMove(int playerIndex)
{
    InitBoardFromGame();

}
/** search for a move on behalf onf player p and report the result
 * to the game.  This is called in the robot process, so the normal
 * game UI is not encumbered by the search.
 */
 public commonMove DoFullMove()
    {
        Exxitmovespec move = null;

        // it's important that the robot randomize the first few moves a little bit.
        int randomn = ((RANDOMIZE&&(board.moveNumber <= 17)) ? (20 - board.moveNumber) : 0);
        board.robotDepth = 0;
        int depth = MAX_DEPTH;
        Search_Driver search_state = Setup_For_Search(depth, false);
       try
        {
 
            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new Exxitmovespec("Done", board.whoseTurn);
            }
            search_state.save_all_variations = SAVE_TREE;
            search_state.verbose=verbose;
            search_state.allow_killer = KILLER_HEURISTIC && ((Strategy!=DUMBOT_LEVEL)&&(Strategy!=WEAKBOT_LEVEL));
            search_state.save_digest=false;			// debugging only
            search_state.check_duplicate_digests = false; 	// debugging only

            if (move == null)
            {
                move = (Exxitmovespec) search_state.Find_Static_Best_Move(randomn);
            }
        }
        finally
        {
            Accumulate_Search_Summary();
            Finish_Search_In_Progress();
        }

        if (move != null)
        {
            if(G.debug() && (move.op!=MOVE_DONE)) 
            { move.showPV("exp final pv: ");
            // normal exit with a move
            search_state.Describe_Search(System.out);
            System.out.flush();
            }
            return (move);
        }

        continuous = false;
        // abnormal exit
        return (null);
    }


 }
