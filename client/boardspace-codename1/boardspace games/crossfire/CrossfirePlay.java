/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.

    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/.
 */
package crossfire;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;


/** 
 * Crossfire uses alpha-beta only
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * <p>
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * <p>
 * Notwithstanding the "only" above, debugging robot players can be very
 * difficult, both at the elementary level when the robot crashes out or
 * produces obviously wrong results, or at the advanced level when the robot
 * produces an undesirable result that is not blatantly wrong.
 * <p>
 * debugging aids:
 * <p>
 * <li>{@link #List_Of_Legal_Moves} should produce only legal moves, and should
 * by default produce all legal moves, but if your board class has some consistency
 * checking, errors constructing the move list might be detected. 
 * 
 * <li>Turn on the "start evaluator" action, and experiment with board positions
 * in puzzle mode.  Each new position will print the current evaluation.
 * 
 * <li>when the robot is stopped at a breakpoint (for example in {@link #Static_Evaluate_Position}
 * turn on the "show alternate board" option to visualize the board position.  It's usually
 * not a good idea to leave the option on when the robot is running because there will be
 * two threads using the data simultaneously, which is not expected.
 *
 * <li>turn on the save_digest and check_duplicate_digest flags.
 *
 ** <li>set {@link #verbose} to 1 or 2.  These produce relatively small amounts
 * of output that can be helpful understanding the progress of the search
 *
 ** <li>set a breakpoint at the exit of {@link #DoFullMove} and example the
 * top_level_moves variable of the search driver.  It contains a lot of information
 * about the search variations that were actually examined.
 *
 * <li>for a small search (shallow depth, few nodes) turn on {@link #SAVE_TREE}
 * and set a breakpoint at the exit of {@link #DoFullMove}
 * @author ddyer
 *
 */
public class CrossfirePlay extends commonRobot<CrossfireBoard> implements Runnable, CrossfireConstants,
    RobotProtocol
    {
    private boolean SAVE_TREE = false;				// debug flag for the search driver.  Uses lots of memory
    private int MAX_DEPTH = 4;						// search depth.
    private static final boolean KILLER = false;	// if true, allow the killer heuristic in the search
    private static final double GOOD_ENOUGH_VALUE = VALUE_OF_WIN;	// good enough to stop looking
				// this is appropriate for simple games, but probably not too effective
				// until there is a much better evaluator.
    // this is an arbitrary value assigned to a winning position, so minmax
    // and alpha-beta will prefer wins to non-wins.  It's exact value is
    // unimportant, but it must at least double any non-winning score the
    // evaluator produces.  Integers are cheap, don't be stingy.  The main thing
    // is to have a convenient range of numbers to work with.
    /* strategies */
    private static final int WEAKBOT_DEPTH = 3;
    private static final int DUMBOT_DEPTH = 4;
    private static final int SMARTBOT_DEPTH = 5;
    private static final int BESTBOT_DEPTH = 6;
    private int Strategy = DUMBOT_LEVEL;
    
/* constructor */
    public CrossfirePlay()
    {	Strategy = DUMBOT_LEVEL;
    }


/** Called from the search driver to undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence. This is usually the most troublesome 
 * method to implement - everything else in the board manipulations moves "forward".
 * Among other things, Unmake_Move will have to undo captures and restore captured
 * pieces to the board, remove newly placed pieces from the board and so on.  
 * <p>
 * Among the most useful methods; 
 * <li>use the move object and the Make_Move method
 * to store the information you will need to perform the unmove.
 * It's also standard to restore the current player, the current move number,
 * and the current board state from saved values rather than try to deduce
 * the correct inverse of these state changes.
 * <li>use stacks in the board class to keep track of changes you need to undo, and
 * record only the index into the stack in the move object.
 * 
 */
    public void Unmake_Move(commonMove m)
    {	CrossfireMovespec mm = (CrossfireMovespec)m;
        board.UnExecute(mm);
    }
/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   CrossfireMovespec mm = (CrossfireMovespec)m;
        board.RobotExecute(mm);
     }

/** return a Vector of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {
        return(board.GetListOfMoves());
    }
    /**
    * this is the static evaluation used by Dumbot.  When testing a better
    * strategy, leave this untouched as a reference point.  The real meat
    * of the evaluation is the "blobs" class which is constructed by the
    * board.  The blobs structure aggregates the stones into chains and
    * keeps some track of how close the chains are to each other and to
    * the edges of the board.
    *
    * @param evboard
     * @param blobs
     * @param player
     * @param print
     * @return
     */
     double dumbotEval(CrossfireBoard evboard,int player,boolean print)
    {	// note we don't need "player" here because the blobs variable
    	// contains all the information, and was calculated for the player
    	double val = evboard.dumbotEval(player,print);
    	return(val);
    }





    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    double ScoreForPlayer(CrossfireBoard evboard,int player,boolean print)
    {	
		double val = 0.0;
		// is this a won position? If so that's the evaluation.
		// note that for some games, the current position might be a win
		// for the other player and that would have be be accounted for too.
     	boolean win = evboard.WinForPlayerNow(player);
 
     	// make wins in fewer moves look slightly better. Nothing else matters.
     	// note that without this little tweak, the robot might appear to "give up"
     	// when a loss is inevitable a few moves down the road, and take an unnecessary
     	// loss now rather than prolonging the game.
     	if(win) 
     		{ val = VALUE_OF_WIN+(1.0/(1+board.robotDepth));
     		  if(print) {System.out.println(" win = "+val); }
     		  return(val); 
     		}
     	
     	// if the position is not a win, then estimate the value of the position
    	switch(Strategy)
    	{	default: throw G.Error("Not expecting strategy %s",Strategy);
    		case WEAKBOT_LEVEL:
    		case DUMBOT_LEVEL: 
    			// betterEval based on "two lines" developed by Adam Shepherd, Oct 2009
      			val = dumbotEval(evboard,player,print);
       	  		break;
    		case SMARTBOT_LEVEL: 
    		case BESTBOT_LEVEL: 	// both the same for now
    			// this is the old dumbot based on connections
       			val = dumbotEval(evboard,player,print);
       			break;
     	}
    	// we're going to subtract two values, and the result must be inside the
    	// bounds defined by +-WIN
    	G.Assert((val<(VALUE_OF_WIN/2))&&(val>=(VALUE_OF_WIN/-2)),"value out of range");
     	return(val);
    }
    
    /** this is called from the search driver to evaluate a particular position. The driver
     * calls List_of_Legal_Moves, then calls Make_Move/Static_Evaluate_Position/UnMake_Move
     *  for each and sorts the result to preorder the tree for further evaluation
     */
    public double Static_Evaluate_Position(	commonMove m)
    {	int playerindex = m.player;
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,nextPlayer[playerindex],false);
        // don't dilute the value of wins with the opponent's positional score.
        // this avoids the various problems such as the robot comitting suicide
        // because it's going to lose anyway, and the position looks better than
        // if the oppoenent makes the last move.  Technically, this isn't needed
        // if there is no such thing as a suicide move, but the logic
        // is included here because this is supposed to be an example.
        if(val0>=VALUE_OF_WIN) { return(val0); }
        if(val1>=VALUE_OF_WIN) { return(-val1); }
        return(val0-val1);
    }
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {
    	CrossfireBoard evboard = GameBoard.cloneBoard();
    	double val0 = ScoreForPlayer(evboard,FIRST_PLAYER_INDEX,true);
    	double val1 = ScoreForPlayer(evboard,SECOND_PLAYER_INDEX,true);
    	System.out.println("Eval is "+ val0 +" "+val1+ " = " + (val0-val1));
    }


/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.  The debugging 
 * menu items "set robotlevel(n)" set the value of "strategy".  Evaluator is not
 * really used at this point, but was intended to be the class name of a plugin
 * evaluator class
 */
 public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
        String evaluator, int strategy)
    {
        InitRobot(newParam, info, strategy);
        GameBoard = (CrossfireBoard) gboard;
        board = GameBoard.cloneBoard();
        // strategy with be 0,1,2 for Dumbot, Smartbot, Bestbot
        Strategy = strategy;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy %s",strategy);
        case WEAKBOT_LEVEL: MAX_DEPTH = WEAKBOT_DEPTH; break;
        case DUMBOT_LEVEL:  MAX_DEPTH = DUMBOT_DEPTH; break;
        case SMARTBOT_LEVEL:  MAX_DEPTH = SMARTBOT_DEPTH; break;
        case BESTBOT_LEVEL:  MAX_DEPTH = BESTBOT_DEPTH;  break;
        }
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
	//use this for a friendly robot that shares the board class
	board.copyFrom(GameBoard);
    board.sameboard(GameBoard);	// check that we got a good copy.  Not expensive to do this once per move

}
/** search for a move on behalf of player p and report the result
 * to the game.  This is called in the robot process, so the normal
 * game UI is not encumbered by the search.
 */
 public commonMove DoFullMove()
    {
        CrossfireMovespec move = null;
        try
        {
       	
            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new CrossfireMovespec("Done", board.whoseTurn);
            }

            // it's important that the robot randomize the first few moves a little bit.
            int randomn = RANDOMIZE ? ((board.moveNumber <= 6) ? (14 - 2*board.moveNumber) : 0) : 0;
            board.robotDepth = 0;

            int depth = MAX_DEPTH;	// search depth
            double dif = 0.0;		// stop randomizing if the value drops this much
            // if the "dif" and "randomn" arguments to Find_Static_Best_Move
            // are both > 0, then alpha-beta will be disabled to avoid randomly
            // picking moves whose value is uncertain due to cutoffs.  This makes
            // the search MUCH slower so depth ought to be limited
            // if ((randomn>0)&&(dif>0.0)) { depth--; }
            // for games where there are no "fools mate" type situations
            // the best solution is to use dif=0.0;  For games with fools mates,
            // set dif so the really bad choices will be avoided
            Search_Driver search_state = Setup_For_Search(depth, false);
            search_state.save_all_variations = SAVE_TREE;
            search_state.good_enough_to_quit = GOOD_ENOUGH_VALUE;
            search_state.verbose = verbose;
            search_state.allow_killer = KILLER;
            search_state.save_top_digest = true;	// always on as a background check
            search_state.save_digest=false;	// debugging only
            search_state.check_duplicate_digests = false; 	// normal in some cases for this game

           if (move == null)
            {	// randomn takes the a random element among the first N
            	// to provide variability.  The second parameter is how
            	// large a drop in the expectation to accept.  For some games this
            	// doesn't really matter, but some games have disasterous
            	// opening moves that we wouldn't want to choose randomly
                move = (CrossfireMovespec) search_state.Find_Static_Best_Move(randomn,dif);
            }
        }
        finally
        {
            Accumulate_Search_Summary();
            Finish_Search_In_Progress();
        }

        if (move != null)
        {
            if(G.debug() && (move.op!=MOVE_DONE)) { move.showPV("exp final pv: "); }
            // normal exit with a move
            commonMove bm = move.best_move();
            if((bm!=null)&&(bm.op==MOVE_ROBOT_RESIGN)) 
            {
            	move.op = MOVE_RESIGN;
            }
            return (move);
        }

        continuous = false;
        // abnormal exit
        return (null);
    }




 }
