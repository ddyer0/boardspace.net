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
package entrapment;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

/** 
 * Entrapment uses alpha-beta only 
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * @author ddyer
 *
 */
public class EntrapmentPlay extends commonRobot<EntrapmentBoard> implements Runnable, EntrapmentConstants,
    RobotProtocol
{   
	static final double VALUE_OF_WIN = 1000000.0;

	
	private boolean SAVE_TREE = false;				// debug flag for the search driver
	
	private boolean KILLER = false;					// probably ok for all games with a 1-part move
	private static final int WEAKBOT_DEPTH = 5;
	private static final int DUMBOT_DEPTH = 6;
	private static final int GOODBOT_DEPTH = 6;
	private static final int BESTBOT_DEPTH = 7;
	private int MAX_DEPTH = BESTBOT_DEPTH;
	
	     /* strategies */
	private int strategy = DUMBOT_LEVEL;
	private Evaluator evaluator = null;
	
    public Evaluator getEvaluator() 
    { return(evaluator);
    }
    /* constructor */
    public EntrapmentPlay()
    {
    }
 

/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	EntrapmentMovespec mm = (EntrapmentMovespec)m;
    	board.UnExecute(mm);
     }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   EntrapmentMovespec mm = (EntrapmentMovespec)m;
        board.RobotExecute(mm);
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
    private double ScoreForPlayer(EntrapmentBoard evboard,int player,boolean print)
    {	
     	return evaluator.evaluate(evboard, player, print);
    }
    
    /**
     * this is it! just tell me that the position is worth.  
     */
    public double Static_Evaluate_Position(commonMove m)
    {	int playerindex = m.player;
    	if(board.GameOver())
    	{
		boolean win = board.WinForPlayerNow(playerindex);
		if(win) { return(VALUE_OF_WIN+(1.0/(1+board.robotDepth))); }
		boolean win2 = board.WinForPlayerNow(playerindex^1);
		if(win2) { return -(VALUE_OF_WIN+1-1.0/(1+board.robotDepth)); }
		return 0;
    	}
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,nextPlayer[playerindex],false);
        return(val0-val1);
    }
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {
    	EntrapmentBoard evboard = (EntrapmentBoard)GameBoard.cloneBoard();
        double val0 = ScoreForPlayer(evboard,FIRST_PLAYER_INDEX,true);
        double val1 = ScoreForPlayer(evboard,SECOND_PLAYER_INDEX,true);
        System.out.println("Eval is "+ val0 +" "+val1+ " = " + (val0-val1));
    }


/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.
 */
 public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
        String ev, int strat)
    {
        InitRobot(newParam, info, strategy);
        GameBoard = (EntrapmentBoard) gboard;
        board = (EntrapmentBoard)GameBoard.cloneBoard();
        strategy = strat;
        switch(strat)
        {
        case WEAKBOT_LEVEL:
        	evaluator = new StandardEvaluator();
        	MAX_DEPTH = WEAKBOT_DEPTH;
        	break;
        case DUMBOT_LEVEL:
        	evaluator = new StandardEvaluator();
        	MAX_DEPTH = DUMBOT_DEPTH;
        	break;
        case SMARTBOT_LEVEL:
        	evaluator = new StandardEvaluator();
        	MAX_DEPTH = GOODBOT_DEPTH;
        	evaluator.setWeights(
        			"172.141092850531 93.29493879252769 1.4571232769516058 75.92416506154834 19.544549021086734 27.229769362994556"
        			//"6031.738638209788 234.97518888478518 17.38035419862232 179.27058001100278 103.03929597702069 160.93655217410202");
        			);
        	break;
        case BESTBOT_LEVEL:
        	evaluator = new StandardEvaluator();
         	MAX_DEPTH = BESTBOT_DEPTH;
        	break;
        case TESTBOT_LEVEL_1:
        	MAX_DEPTH = 4;
        	evaluator = new StandardEvaluator();
        	break;
        case TESTBOT_LEVEL_2:
        	MAX_DEPTH = 4;
        	evaluator = new StandardEvaluator();
        	break;
        default: throw G.Error("Not expecting strategy %s",strategy);
        }
    }
    /** copy the game board, in preparation for a search */
    public void InitBoardFromGame()
    {
        board.copyFrom(GameBoard);
        board.clearDead();
    }
/** search for a move on behalf onf player p and report the result
 * to the game.  This is called in the robot process, so the normal
 * game UI is not encumbered by the search.
 */
 public void PrepareToMove(int playerIndex)
 {	InitBoardFromGame();
 }
 
 // final a move from-to for a barrier is slow, because there are a lot of mostly redudnant "froms"
 // so we generate an extra "drop" barrier move, and if it's selected we find a matching "remove"
 // move using a mostly valid method.
 private EntrapmentMovespec convertToMoveBarrier(EntrapmentMovespec move)
 {
	 EntrapmentState state = board.board_state;
	 int turn = board.whoseTurn;
	 
	 board.setState(EntrapmentState.REMOVE_BARRIER_STATE);
	 board.setWhoseTurn(move.player);
	 EntrapmentMovespec newMove = (EntrapmentMovespec)DoFullMove();
	 G.Assert(newMove.op == MOVE_REMOVE,"generated a remove move");
	 
	 newMove.op = MOVE_BOARD_BOARD;
	 newMove.dest = move.dest;
	 newMove.to_col = move.to_col;
	 newMove.to_row = move.to_row;
	 board.setState(state);
	 board.setWhoseTurn(turn);
	 Make_Move(newMove);
	 Unmake_Move(newMove);
	 return(newMove);
 }

 public commonMove DoAlphaBetaFullMove()
    {
	 EntrapmentMovespec move = null;

        try
        {

            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new EntrapmentMovespec("Done", board.whoseTurn);
            }

            // it's important that the robot randomize the first few moves a little bit.
            int randomn = RANDOMIZE 
            				? ((board.moveNumber <= 4) ? (20 - board.moveNumber) : 0)
            				: 0;
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
            board.robotDepth = 0;

            if(board.board_state==EntrapmentState.REMOVE_BARRIER_STATE) { depth -=2; }
            
            Search_Driver search_state = Setup_For_Search(depth, false);
            search_state.save_all_variations = SAVE_TREE;
            search_state.allow_killer = KILLER;
            search_state.verbose=verbose;			// debugging
            search_state.save_top_digest = true;	// always on as a background check
            search_state.save_digest=false;	// debugging only
            search_state.check_duplicate_digests = false; 	// debugging only
            search_state.good_enough_to_quit = VALUE_OF_WIN;
            search_state.allow_good_enough = true;

            if (move == null)
            {
                move = (EntrapmentMovespec) search_state.Find_Static_Best_Move(randomn,dif);
            }
        }
        finally
        {
            Accumulate_Search_Summary();
            Finish_Search_In_Progress();
        }

        if (move != null)
        {
            //if(G.debug() && (move.op!=MOVE_DONE)) { move.showPV("exp final pv: "); }
            // normal exit with a move
            if(move.op==MOVE_ADD)
            {	// if we generated an add barrier move, which is not a legal move, convert
            	// it to a move barrier move.
            	move = convertToMoveBarrier(move);
            	
            }
            return (move);
        }

        continuous = false;
        // abnormal exit
        return (null);
    }




 }