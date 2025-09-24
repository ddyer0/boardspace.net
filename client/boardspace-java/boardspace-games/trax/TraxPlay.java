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
package trax;

import java.util.Vector;

import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;
import lib.*;

/** 
 * Trax uses both alpha-beta and mcts
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * @author ddyer
 *
 */
//
// 9/2021 removed 2 slots from Traxmovespec and reimplemented
// the undo mechanism so make the monte carlo bot compatible
// with multi threading.   Re-tuned bestbot to use less alpha
// and lower move depth threshold, and use threads.  The net
// effect is to make the "line" test position work better.
//
public class TraxPlay extends commonRobot<TraxGameBoard> implements Runnable, TraxConstants,
    RobotProtocol
{	static final double WIN_VALUE = 10000.0;	// value of a win, all others are less
	static final double GOOD_ENOUGH_VALUE = WIN_VALUE-1000;	// good enough to stop looking
	static final double MOVE_PENALTY = 5;
	static final boolean KILLER = false;
	static final double LOOP_WEIGHT = 10.0;
	static final double LINE_WEIGHT = 10.0;
	static final double CORNER_WEIGHT= 1.0;
	
	boolean EXP_MONTEBOT = false;
	
    boolean SAVE_TREE = false;				// debug flag for the search driver
    int MAX_DEPTH = 6;
    int WEAKBOT_MAX_DEPTH = 3;
    int RANDOM_MAX_DEPTH = 5;
    int DUMBOT_MAX_DEPTH = 4;
    int GOODBOT_MAX_DEPTH = 5;
    int BESTBOT_MAX_DEPTH = 6;
    /* strategies */
    final int RANDOM = 0;
    final int SIMPLE_MAX = 1;
    int Strategy = SIMPLE_MAX;
    double MONTE_ALPHA = 0.25;
    boolean MONTE_SORT = true;
    boolean MONTE_BLITZ = false;
    int MONTE_THREADS = DEPLOY_THREADS;
    int MONTE_CHILD_LIMIT = 300000;
    int MONTE_FINAL_DEPTH = 20;
    
    int boardSearchLevel = 0;				// the current search depth

    /* constructor */
    public TraxPlay()
    {
    }


/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	Traxmovespec mm = (Traxmovespec)m;
        board.UnExecute(mm);
        boardSearchLevel--;
    }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   Traxmovespec mm = (Traxmovespec)m;
    	boardSearchLevel++;
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
    double ScoreForPlayer(TraxGameBoard evboard,int player,boolean print)
    {	double val=0.0 ;
    	if(evboard.WinForPlayerNow(player))	// also calculate lines array
    	{	if(print) { System.out.println("Win for "+player); }
    		val = WIN_VALUE-evboard.tilesOnBoard*MOVE_PENALTY;
    	}
    	{	Vector<lineinfo> plines = evboard.lines[player];
    		int minloop = 0;
    		int maxline = 0;
    		int corners=0;
    		lineinfo bestline = null;
    		lineinfo bestloop = null;
    		lineinfo secondbestline = null;
    		lineinfo secondbestloop = null;
    		int secondminloop = 0;
    		int secondmaxline = 0;
    		int nelem = plines.size();
    		for(int i=0;i<nelem;i++)
    		{	lineinfo line = plines.elementAt(i);
    			int thisloop = line.loopDistanceSQ();
    			int thisline = line.potentialSpan();
    			if((bestline==null)||(thisline>maxline))
    			{	secondbestline=bestline;
    				secondmaxline = maxline;

    				bestline = line;
     				maxline = thisline;
    				
    			} 
    			else if((secondbestline==null)||(thisline>secondmaxline))
    			{secondbestline=line;
				 secondmaxline = thisline;
    			}
    		if(thisloop==3) { corners++; }
   			if((bestloop==null)||(thisloop<minloop))
    			{	secondbestloop = bestloop;
   					secondminloop = minloop;
   			    	bestloop = line;
    				minloop = thisloop;
    			}	
   			else if((secondbestloop==null) || (thisloop<secondminloop))
	   			{	secondbestloop = line;
					secondminloop = thisloop;
	   			}
    		}
       double loopv = (100-minloop)*LOOP_WEIGHT;
       double linev = Math.max(0,maxline)*LINE_WEIGHT;
       double loopv2 = (100-secondminloop)*LOOP_WEIGHT/2;
       double linev2 = Math.max(0,secondmaxline)*LINE_WEIGHT/2;
       double cornerv = corners*CORNER_WEIGHT;
       val += loopv + linev+loopv2+linev2+cornerv;
       if(print)
       {
    	  System.out.println("line "+bestline+" = "+ maxline + " = " + linev +" & " + secondbestline+" = "+ secondmaxline + " = " + linev2);
       	  System.out.println("loop "+bestloop+" = "+ minloop + " = " + loopv+" & "+ secondbestloop + " = " + loopv2);
       	  System.out.println("corners "+corners+" = " + cornerv);
       	  System.out.println(" = "+(val));
       }
    	}
    	return(val);
    }
    
    /**
     * this is it! just tell me that the position is worth.  
     */
    // TODO: refactor static eval so GameOver is checked first
   public double Static_Evaluate_Position(commonMove m)
    {	int playerindex = m.player;
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,nextPlayer[playerindex],false);
        if((val0>=WIN_VALUE) && (val1>=WIN_VALUE)) { val1 = val1/2; }
        return(val0-val1);
    }
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {
    	TraxGameBoard evboard = GameBoard.cloneBoard();
    	double val0 = ScoreForPlayer(evboard,FIRST_PLAYER_INDEX,true);
    	double val1 = ScoreForPlayer(evboard,SECOND_PLAYER_INDEX,true);
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
        GameBoard = (TraxGameBoard) gboard;
        board = GameBoard.cloneBoard();
       
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy %s",strategy);
        case DUMBOT_LEVEL:	MAX_DEPTH = DUMBOT_MAX_DEPTH; break;
        case SMARTBOT_LEVEL: MAX_DEPTH = GOODBOT_MAX_DEPTH; break;
        case WEAKBOT_LEVEL:
        	WEAKBOT = true;
        	MAX_DEPTH = WEAKBOT_MAX_DEPTH;
        	MONTEBOT = DEPLOY_MONTEBOT;
        	break;
        case BESTBOT_LEVEL:
        	MAX_DEPTH = BESTBOT_MAX_DEPTH; 
        	MONTEBOT = DEPLOY_MONTEBOT; 
            MONTE_ALPHA = 0.25;
            MONTE_BLITZ = false;
            MONTE_THREADS = 0;
            MONTE_CHILD_LIMIT = 300000;
            MONTE_FINAL_DEPTH = 20;
            terminalNodeOptimize = true;
            MONTE_SORT = true;
            break;
        case TESTBOT_LEVEL_2:
        	MAX_DEPTH = BESTBOT_MAX_DEPTH; 
        	MONTEBOT = DEPLOY_MONTEBOT; 
            MONTE_ALPHA = 0.25;
            MONTE_BLITZ = false;
            // 
            // trax doesn't work with threads, for somewhat mysterious reasons.
            // my current theory is the shifting coordinate system used on the board.
            //
            MONTE_THREADS = DEPLOY_THREADS;
            MONTE_CHILD_LIMIT = 300000;
            MONTE_FINAL_DEPTH = 20;
            terminalNodeOptimize = true;
            MONTE_SORT = true;
            break;

        case TESTBOT_LEVEL_1:
           	MAX_DEPTH = BESTBOT_MAX_DEPTH; 
        	MONTEBOT = DEPLOY_MONTEBOT; 
            MONTE_ALPHA = 0.5;
            MONTE_BLITZ = false;
            MONTE_CHILD_LIMIT = UCTMoveSearcher.defaultStoredChildLimit;
            MONTE_FINAL_DEPTH = 100;
            break;
        	
        case MONTEBOT_LEVEL: MONTEBOT = true; EXP_MONTEBOT=DEPLOY_MONTEBOT; break;
         }
        Strategy = SIMPLE_MAX;
        //System.out.println("Depth "+MAX_DEPTH);
    }
    /** copy the game board, in preparation for a search */
    public void InitBoardFromGame()
    {
        board.copyFrom(GameBoard);
    }
    
    public void PrepareToMove(int playerindex)
    {
        InitBoardFromGame();
   	 
    }
/** search for a move on behalf onf player p and report the result
 * to the game.  This is called in the robot process, so the normal
 * game UI is not encumbered by the search.
 */
 public commonMove DoAlphaBetaFullMove()
    {
        Traxmovespec move = null;

        try
        {
            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new Traxmovespec("Done", board.whoseTurn);
            }

            // it's important that the robot randomize the first few moves a little bit, but Trax
            // has a pecuriar problem because even the first few moves can be immediate wins or losses.
            // Consequently, we have to severly limit the amount of randomness.
            int randomn = RANDOMIZE 
            				? ((board.moveNumber <= 8) ? (20 - board.moveNumber) : 0)
            				: 0;
            boardSearchLevel = 0;

            int depth = MAX_DEPTH;
            if(randomn>0)
            {	// limit the depth of randomized searches, because we will also eliminate alpha-beta
            	// and otherwise it's just too damn slow!
            	depth=Math.min(depth,RANDOM_MAX_DEPTH);
            }
            Search_Driver search_state = Setup_For_Search(depth, false);
            search_state.save_all_variations = SAVE_TREE;
            search_state.good_enough_to_quit = GOOD_ENOUGH_VALUE;
            search_state.verbose = verbose;
            search_state.allow_killer = KILLER;
            search_state.save_digest=false;	// debugging only
 
            if (move == null)
            {
                move = (Traxmovespec) search_state.Find_Static_Best_Move(randomn,100.0);
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

// this is the monte carlo robot, which for some games is much better then the alpha-beta robot
// for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
// evaluator other than winning a game.
public commonMove DoMonteCarloFullMove()
{	commonMove move = null;
	try {
      if (board.DoneState())
       { // avoid problems with gameover by just supplying a done
           move = new Traxmovespec("Done", board.whoseTurn);
       }
       else 
       {
       // it's important that the robot randomize the first few moves a little bit.
       double randomn = (RANDOMIZE && (board.moveNumber <= 6)) ? 0.1/board.moveNumber : 0.0;
       UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this);
       monte_search_state.save_top_digest = true;	// always on as a background check
       monte_search_state.save_digest=false;	// debugging only
       monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
       monte_search_state.timePerMove = WEAKBOT ? 5 : EXP_MONTEBOT ? 20:20;		// 10 seconds per move
       monte_search_state.verbose = verbose;
       monte_search_state.alpha = MONTE_ALPHA;
       monte_search_state.sort_moves = MONTE_SORT;
       monte_search_state.stored_child_limit = MONTE_CHILD_LIMIT;
       monte_search_state.blitz = MONTE_BLITZ;
       monte_search_state.only_child_optimization = true;
       monte_search_state.dead_child_optimization = true;
       monte_search_state.final_depth = MONTE_FINAL_DEPTH;			// tree tends to be very deep, so restrain it.
       monte_search_state.simulationsPerNode = 1;
       monte_search_state.random_moves_per_second = WEAKBOT ? 1000:180000;
       // trax unmove  doesn't work with threads, despite the movespec being compatible looking
       monte_search_state.maxThreads = MONTE_THREADS;	// trax not organized to support threads
       board.blitz = monte_search_state.blitz;
       monte_search_state.terminalNodeOptimization = terminalNodeOptimize;
       move = monte_search_state.getBestMonteMove();
       }
		}
     finally { ; }
     if(move==null) { continuous = false; }
    return(move);
}
/**
 * for UCT search, return the normalized value of the game, with a penalty
 * for longer games so we try to win in as few moves as possible.  Values
 * must be normalized to -1.0 to 1.0
 */
public double NormalizedScore(commonMove lastMove)
{	int player = lastMove.player;
	boolean win = board.WinForPlayerNow(player);
	if(win) { return(0.8+0.2/boardSearchLevel); }
	boolean win2 = board.WinForPlayerNow(nextPlayer[player]);
	if(win2) { return(- (0.8+0.2/boardSearchLevel)); }
	return(0);
}
}

/* standard game, level 1, no randomization time 2:44 with depth limit opt 2:28 
 * (;
GM[16] VV[1]
DT[]
GN[null-null]
SU[trax]
P0[id "null"]
P1[id "null"]
; P0[0 Start P0]
; P0[1 dropb 4 @ 0 /]
; P0[2 done]
; P1[3 dropb 5 B 1 \\]
; P1[4 done]
; P0[5 dropb 2 A 2 /]
; P0[6 done]
; P1[7 dropb 3 @ 2 \\]
; P1[8 done]
; P0[9 dropb 2 C 0 /]
; P0[10 done]
; P1[11 dropb 4 D 1 /]
; P1[12 done]
; P0[13 dropb 5 B 4 \\]
; P0[14 done]
; P1[15 dropb 1 C 3 +]
; P1[16 done]
; P0[17 dropb 5 A 2 \\]
; P0[18 done]
; P1[19 dropb 3 A 1 \\]
; P1[20 done]
; P0[21 dropb 4 C 0 /]
; P0[22 done]
; P1[23 dropb 4 D 4 /]
; P1[24 done]
; P0[25 dropb 0 E 3 +]
; P0[26 done]
; P1[27 dropb 3 D 5 \\]
; P1[28 done]
; P0[29 dropb 0 E 4 +]
; P0[30 done]
; P1[31 dropb 0 E 5 +]
; P1[32 done]
; P0[33 dropb 0 F 4 +]
; P0[34 done]
; P1[35 dropb 5 G 4 \\]
; P1[36 done]
; P0[37 dropb 5 F 5 \\]
; P0[38 done]
; P1[39 dropb 3 F 6 \\]
; P1[40 done]
; P0[41 dropb 3 G 5 \\]
; P0[42 done]
; P1[43 dropb 2 F 3 /]
; P1[44 done]
; P0[45 dropb 5 G 2 \\]
; P0[46 done]
; P1[47 dropb 4 E 1 /]
; P1[48 done]
; P0[49 dropb 2 C 0 /]
; P0[50 done]
; P1[51 dropb 1 H 3 +]
; P1[52 done]
; P0[53 dropb 4 H 2 /]
; P0[54 done]
; P1[55 dropb 2 B 7 /]
; P1[56 done]
; P0[57 dropb 4 A 7 /]
; P0[58 done]
; P1[59 dropb 3 F 1 \\]
; P1[60 done]
; P0[61 dropb 0 @ 6 +]
; P0[62 done]
;
P0[time 0:00:00 ]
P1[time 0:00:00 ]
) */

