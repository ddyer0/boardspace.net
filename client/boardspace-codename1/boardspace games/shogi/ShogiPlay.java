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
package shogi;

import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;
import lib.*;

/** 
 * Shogi uses alpha-beta
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * 
 * @author ddyer
 *
 */
public class ShogiPlay extends commonRobot<ShogiBoard> implements Runnable, ShogiConstants,
    RobotProtocol
{   
	private boolean SAVE_TREE = false;				// debug flag for the search driver
	private boolean KILLER = true;					// probably ok for all games with a 1-part move
	private static final double VALUE_OF_WIN = 1000000.0;
	private static final int WEAKBOT_DEPTH = 7;
	private static final int DUMBOT_DEPTH = 8;
	private static final int GOODBOT_DEPTH = 10;
	private static final int BESTBOT_DEPTH = 12;
	private static final double DUMBOT_TIME_LIMIT = 0.5;
	private static final double GOODBOT_TIME_LIMIT = 1.0;
	private static final double BESTBOT_TIME_LIMIT = 2.0;
	private int MAX_DEPTH = BESTBOT_DEPTH;
	private double TIME_LIMIT = BESTBOT_TIME_LIMIT;
	private boolean FIXED_DEPTH_SEARCH = false;
     /* strategies */
	private int boardSearchLevel = 0;				// the current search depth
	private boolean depth_limited = false;
    /* constructor */
    public ShogiPlay()
    {
    }

    /** return true if the search should be depth limited at this point.
     * 
     */
    public boolean Depth_Limit(int current, int max)
    {	// for simple games where there is always one move per player per turn
    	// current>=max is good enough.  For more complex games where there could
    	// be several moves per turn, we have to keep track of the number of turn changes.
    	// it's also possible to implement quiescence search by carefully adjusting when
    	// this method returns true.
        return(depth_limited || (current>=max));
   }
/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	ShogiMovespec mm = (ShogiMovespec)m;
    	boardSearchLevel--;
    	if(boardSearchLevel==0)
    	{	board.repeatedPositions.removeFromRepeatedPositions(m);
    	}
    	depth_limited = false;
    	board.UnExecute(mm);
     }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   ShogiMovespec mm = (ShogiMovespec)m;
    	depth_limited = (board.RobotExecute(mm,boardSearchLevel==0)>=3);
        boardSearchLevel++;
    }

/** return an enumeration of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {   return(board.GetListOfMoves());
    }
    
    

    
    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    double ScoreForPlayer(ShogiBoard evboard,int player,boolean print)
    {	
     	boolean win = evboard.WinForPlayerNow(player);
    	if(win) { return(VALUE_OF_WIN+(1.0/(1+boardSearchLevel))); }
    	return(evboard.ScoreForPlayer(player,print));

    }
    
    /**
     * this is it! just tell me that the position is worth.  
     */
    public double Static_Evaluate_Position(commonMove m)
    {	int playerindex = m.player;
    	if(m.op==MOVE_ACCEPT_DRAW)
    	{
   		depth_limited = true;
    	return(0.0);
    	}
    	else if(m.op==MOVE_DECLINE_DRAW)
    	{ depth_limited = true;
    	  return(1.0);
    	}
    	else
    	{
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,nextPlayer[playerindex],false);
        if(depth_limited) 
        	{ val1 = VALUE_OF_WIN*2; }	// terrible outcome!
        
        // don't dilute the value of wins with the opponent's positional score.
        // this avoids the various problems such as the robot committing suicide
        // because it's going to lose anyway, and the position looks better than
        // if the opponent makes the last move.  Technically, this isn't needed
        // if there is no such thing as a suicide move, but the logic
        // is included here because this is supposed to be an example.
        if(val0>=VALUE_OF_WIN) { return(val0); }
        if(val1>=VALUE_OF_WIN) { return(-val1); }
        return(val0-val1);
    	}
    }
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {
    	ShogiBoard evboard = (ShogiBoard)GameBoard.cloneBoard();
        double val0 = ScoreForPlayer(evboard,FIRST_PLAYER_INDEX,true);
        double val1 = ScoreForPlayer(evboard,SECOND_PLAYER_INDEX,true);
        if(val1>=VALUE_OF_WIN) { val0=0.0; }
        System.out.println("Eval is "+ val0 +" "+val1+ " = " + (val0-val1));
    }


/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.
 */
 public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
        String evaluator, int strategy)
    {
        InitRobot(newParam, info, strategy);
        GameBoard = (ShogiBoard) gboard;
        board = (ShogiBoard)GameBoard.cloneBoard();
        switch(strategy)
        {
        case WEAKBOT_LEVEL:
        	MAX_DEPTH = WEAKBOT_DEPTH;
        	TIME_LIMIT = DUMBOT_TIME_LIMIT;
        	break; 	
        case DUMBOT_LEVEL:
        	MAX_DEPTH = DUMBOT_DEPTH;
        	TIME_LIMIT = DUMBOT_TIME_LIMIT;
        	break;
        case SMARTBOT_LEVEL:
        	MAX_DEPTH = GOODBOT_DEPTH;
        	TIME_LIMIT = GOODBOT_TIME_LIMIT;
        	break;
        case BESTBOT_LEVEL:
         	MAX_DEPTH = BESTBOT_DEPTH;
         	TIME_LIMIT = BESTBOT_TIME_LIMIT;
        	break;
        default: throw G.Error("Not expecting strategy %s",strategy);
        }
    }

 /** search for a move on behalf onf player p and report the result
 * to the game.  This is called in the robot process, so the normal
 * game UI is not encumbered by the search.
 */
 public void PrepareToMove(int playerIndex)
 {	board.copyFrom(GameBoard);
 }

 public commonMove DoAlphaBetaFullMove()
    {
	 ShogiMovespec move = null;

        try
        {

            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new ShogiMovespec("Done", board.whoseTurn);
            }

            // it's important that the robot randomize the first few moves a little bit.
            int randomn = RANDOMIZE 
            				? ((board.moveNumber <= 6) ? (10 - board.moveNumber) : 0)
            				: 0;
            int depth = MAX_DEPTH;	// search depth
            double dif = 1.3;		// stop randomizing if the value drops this much
            // if the "dif" and "randomn" arguments to Find_Static_Best_Move
            // are both > 0, then alpha-beta will be disabled to avoid randomly
            // picking moves whose value is uncertain due to cutoffs.  This makes
            // the search MUCH slower so depth ought to be limited
            // if ((randomn>0)&&(dif>0.0)) { depth--; }
            // for games where there are no "fools mate" type situations
            // the best condition is to use dif=0.0;  For games with fools mates,
            // set dif so the really bad choices will be avoided
            boardSearchLevel = 0;
            if(randomn>0) { depth = Math.min(5,depth); }
            Search_Driver search_state = FIXED_DEPTH_SEARCH 
            		? Setup_For_Search(depth,false) 
	            	: Setup_For_Search(depth, TIME_LIMIT);
            	
            search_state.save_all_variations = SAVE_TREE;
           // search_state.return_nullmove = true;
           // search_state.use_nullmove = NULLMOVE;
            search_state.allow_killer = KILLER;
            search_state.verbose=verbose;			// debugging
            search_state.save_digest=false;			// debugging only
            search_state.check_duplicate_digests = false; 	// debugging only

            if (move == null)
            {
                move = (ShogiMovespec) search_state.Find_Static_Best_Move(randomn,dif);
               	if((move!=null) && (move.op==MOVE_NULL))
                {	
            		move = (ShogiMovespec)search_state.Nth_Good_Move(1,0.0);	// second best
                	if(move.evaluation()<=-VALUE_OF_WIN) 
                	{
                		move = new ShogiMovespec(MOVE_RESIGN,board.whoseTurn);
                	}
                }

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
            return (move);
        }

        continuous = false;
        // abnormal exit
        return (null);
    }



 }