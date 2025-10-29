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
package triad;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;


/** 
 * Triad uses alpha-beta
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * @author ddyer
 *
 */
public class TriadPlay extends commonMPRobot<TriadBoard> implements Runnable, TriadConstants,
    RobotProtocol
    {
	private static final double VALUE_OF_WIN = 1.0;
	public double valueOfWin() { return VALUE_OF_WIN; }
	
	private double TIME_LIMIT = 2.0;				// progressive search time limit
	private boolean SAVE_TREE = false;				// debug flag for the search driver.  Uses lots of memory
	private static final int WEAKBOT_DEPTH = 6;
	private static final int DUMBOT_DEPTH = 7;
	private static final int SMARTBOT_DEPTH = 10;
	private static final double DUMBOT_TIME = 1.0;
	private static final double SMARTBOT_TIME = 2.0;
    private int MAX_DEPTH = 10;						// search depth.
    private static final boolean KILLER = false;	// if true, allow the killer heuristic in the search
    private static final double GOOD_ENOUGH_VALUE = VALUE_OF_WIN;	// good enough to stop looking
				// this is appropriate for simple games, but probably not too effective
				// until there is a much better evaluator.
    
    private int boardSearchLevel = 0;				// the current search depth

    /* constructor */
    public TriadPlay()
    {
    }


/** Called from the search driver to undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	TriadMovespec mm = (TriadMovespec)m;
         board.UnExecute(mm);
        boardSearchLevel--;
    }
/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   TriadMovespec mm = (TriadMovespec)m;
        board.RobotExecute(mm);
        boardSearchLevel++;
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
    // this is the static evaluation used by Dumbot.  When testing a better
    // strategy, leave this untouched as a reference point.  The real meat
    // of the evaluation is the "blobs" class which is constructed by the
    // board.  The blobs structure aggregates the stones into chains and
    // keeps some track of how close the chains are to each other and to
    // the edges of the board.
    //

    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    private double ScoreForPlayer(TriadBoard evboard,int player,boolean print)
    {	
     	boolean win = evboard.WinForPlayer(player);
    	if(win) { return(VALUE_OF_WIN+(1.0/(1+boardSearchLevel))); }
    	return(evboard.dumbotEval(player,print));

    }
    // seek the end of the best_move chain for multiplayer games
    public commonMove targetMoveForEvaluation(commonMove cm)
    {	commonMove leaf = cm.best_move();
    	commonMove v = cm;
    	while(leaf!=null) { v = leaf; leaf=leaf.best_move(); }
    	return(v);
    }

    // TODO: refactor static eval so GameOver is checked first
   public double Static_Evaluate_Position(commonMove m)
    {	
     	int playerindex = m.player;
     	int nplay = board.nPlayers();
     	commonMPMove mm = (commonMPMove)m;
     	
     	mm.setNPlayers(nplay);
    	
    	for(int i=0;i<nplay; i++)
    	{	mm.playerScores[i] = ScoreForPlayer(board,i,false);
    	}
    	return(reScorePosition(mm,playerindex));
    }


    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {
            TriadBoard evboard = (TriadBoard)(GameBoard.cloneBoard());
            String msg = "Eval is ";
            for(int i=0,lim=evboard.nPlayers(); i<lim; i++)
            {	msg += " "+ScoreForPlayer(evboard,i,true);
            }
            System.out.println(msg);
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
        GameBoard = (TriadBoard) gboard;
        board = GameBoard.cloneBoard();
        // strategy with be 0,1,2 for Dumbot, Smartbot, Bestbot
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy %s",strategy);
        case WEAKBOT_LEVEL:
        	MAX_DEPTH = WEAKBOT_DEPTH;
        	TIME_LIMIT = DUMBOT_TIME;
        	break;
        case DUMBOT_LEVEL:
        	MAX_DEPTH = DUMBOT_DEPTH;
        	TIME_LIMIT = DUMBOT_TIME;
        	break;
        case SMARTBOT_LEVEL: 
        	MAX_DEPTH = SMARTBOT_DEPTH;
        	TIME_LIMIT = SMARTBOT_TIME;
        	break;
        case BESTBOT_LEVEL: 
        	break;
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
 public commonMove DoAlphaBetaFullMove()
    {
        TriadMovespec move = null;
        try
        {
 
            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new TriadMovespec("Done", board.whoseTurn);
            }

            // it's important that the robot randomize the first few moves a little bit.
            int randomn = RANDOMIZE ? ((board.moveNumber <= 8) ? (10 - board.moveNumber) : 0) : 0;
            boardSearchLevel = 0;

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
            Search_Driver search_state = Setup_For_Search(depth, TIME_LIMIT);
            search_state.save_all_variations = SAVE_TREE;
            search_state.good_enough_to_quit = GOOD_ENOUGH_VALUE;
            search_state.verbose = verbose;
            search_state.allow_killer = KILLER;
            search_state.save_digest=false;	// debugging only
            search_state.check_duplicate_digests = false; 	// debugging only

           if (move == null)
            {	// randomn takes the a random element among the first N
            	// to provide variability.  The second parameter is how
            	// large a drop in the expectation to accept.  For some games this
            	// doesn't really matter, but some games have disasterous
            	// opening moves that we wouldn't want to choose randomly
                move = (TriadMovespec) search_state.Find_Static_Best_Move(randomn,dif);
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
