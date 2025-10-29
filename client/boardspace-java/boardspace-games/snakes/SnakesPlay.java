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
package snakes;

import java.util.Hashtable;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

/** 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * @author ddyer
 *
 */
public class SnakesPlay extends commonRobot<SnakesBoard> implements Runnable, SnakesConstants,
    RobotProtocol
{   boolean SAVE_TREE = false;						// debug flag for the search driver
	boolean REMOVE_ROTATIONS = true;		// optimize out the rotations
	boolean SKIP_LEADING_EVALS = true;
	SnakesViewer viewer = null;
	Hashtable<Long,CommonMoveStack >solutions = new Hashtable<Long,CommonMoveStack >();
	int number_of_solutions = 0;
    boolean KILLER = false;					// probably ok for all games with a 1-part move
    static final int DUMBOT_DEPTH = 3;
    static final int GOODBOT_DEPTH = 4;
    static final int BESTBOT_DEPTH = 6;
	static final double VALUE_OF_WIN = 1000000.0;
    int MAX_DEPTH = BESTBOT_DEPTH;
     /* strategies */
    double CUP_WEIGHT = 1.0;
    double MULTILINE_WEIGHT=1.0;
    boolean DUMBOT = false;
    int boardSearchLevel = 0;				// the current search depth

    /* constructor */
    public SnakesPlay(SnakesViewer v)
    {	viewer = v;
    }

    public int Evaluate_And_Sort_Moves(Search_Driver dr,Search_Node sn,commonMove mvec[])
    {	
    	if(SKIP_LEADING_EVALS)
    		{for(int lim = mvec.length-1; lim>=0; lim--) 
    	{
    		mvec[lim].set_depth_limited(commonMove.EStatus.EVALUATED);
    		mvec[lim].set_local_evaluation(0.0); 
    	}}
    	else {
    		super.Evaluate_And_Sort_Moves(dr,sn,mvec);
    	}
    	return(mvec.length);
    }

/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	SnakesMovespec mm = (SnakesMovespec)m;
    	boardSearchLevel--;
    	board.UnExecute(mm);
     }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   SnakesMovespec mm = (SnakesMovespec)m;
		if(REMOVE_ROTATIONS && (forbidden!=null))
		{	if(boardSearchLevel==0)
			{
			 	board.mirrorMoves(forbidden,mm);
			}
		}

        board.RobotExecute(mm);
        boardSearchLevel++;
        if(SKIP_LEADING_EVALS) { m.set_local_evaluation(Static_Evaluate_Position(m)); }
    }

    
    Hashtable<Integer,commonMove>forbidden = null;
    
/** return an enumeration of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {   CommonMoveStack moves = board.GetListOfMoves(forbidden);
    	return(moves);
    }
    
    

    

    
    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    private double ScoreForPlayer(SnakesBoard evboard,int player,boolean print)
    {	
     	boolean win = evboard.WinForPlayerNow(player);
    	if(win && (player==0))
    		{ 
    			long digest = evboard.Digest();
    			if(solutions.get(digest)==null)
    			{
    				number_of_solutions++;
    				String exclude = "";
    				commonMove var = search_driver.getCurrentVariation();
    				//var.showPV("Solution "+number_of_solutions+" ");
    				CommonMoveStack hist = new CommonMoveStack();
    				
    				{
    					CommonMoveStack vhist = viewer.History;
    					for(int i=0,lim=vhist.size();i<lim;i++)
    					{	// copy the game before starting
    						SnakesMovespec elem = (SnakesMovespec)vhist.elementAt(i);
    						hist.push(elem.Copy(null));
    					}
    				}
    				while(var!=null)
    				{	// add the moves in this solution
    					hist.push(var);
    					SnakesMovespec svar = (SnakesMovespec)var;
						if(svar.declined!=null) 
						{ exclude += " "+svar.declined; }
   					var = var.best_move();
    				}
    				solutions.put(digest,hist);
    				viewer.addGame(hist,"Solution #"+number_of_solutions+exclude);
     				
       			}
    			return(VALUE_OF_WIN+(1.0/(1+boardSearchLevel))); 
    		}
    	return(evboard.ScoreForPlayer(player,print,CUP_WEIGHT,MULTILINE_WEIGHT,DUMBOT));

    }
    
    /**
     * this is it! just tell me that the position is worth.  
     */
    // TODO: refactor static eval so GameOver is checked first
    public double Static_Evaluate_Position(commonMove m)
    {	int playerindex = m.player;
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,nextPlayer[playerindex],false);
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
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {
    	SnakesBoard evboard = (SnakesBoard)GameBoard.cloneBoard();
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
        //SnakesViewer = info.get(exHastable.VIEWER)
        GameBoard = (SnakesBoard) gboard;
        board = (SnakesBoard)GameBoard.cloneBoard();
        switch(strategy)
        {case DUMBOT_LEVEL:
        	MAX_DEPTH = DUMBOT_DEPTH;
         	DUMBOT=true;
        	break;
        case SMARTBOT_LEVEL:
        	MAX_DEPTH = GOODBOT_DEPTH;
        	DUMBOT=false;
        	break;
        case BESTBOT_LEVEL:
         	MAX_DEPTH = BESTBOT_DEPTH;
        	DUMBOT=false;
        	break;
        default: throw G.Error("Not expecting strategy %s",strategy);
        }
    }
    /** copy the game board, in preparation for a search */
    public void InitBoardFromGame()
    {
        board.copyFrom(GameBoard);
    }
/** search for a move on behalf onf player p and report the result
 * to the game.  This is called in the robot process, so the normal
 * game UI is not encumbered by the search.
 */
 public void PrepareToMove(int playerIndex)
 {	InitBoardFromGame();
 }

 public SnakesMovespec moveSequence = null;
 
 public commonMove DoFullMove()
    {
	 SnakesMovespec move = null;
	 if(moveSequence!=null)
	 {
		 move = moveSequence;
		 moveSequence = (SnakesMovespec)move.best_move();
		 return(move);
	 }
	 SnakeState bstate = board.getState();
	 solutions.clear();
	 number_of_solutions = 0;
	 if(REMOVE_ROTATIONS && !board.hasGivens() && board.emptyTarget())
		 {	forbidden = new Hashtable<Integer,commonMove>();
		 }
		 else 
		 { forbidden = null;			// initialize the forbidden list
		 }
	 if(board.getState()==SnakeState.PUZZLE_STATE) 
	 	{
		if(board.WinForPlayerNow(0)) { continuous=false; return(null); }
	 	board.setState(SnakeState.PLAY_STATE); 
	 	}
     // it's important that the robot randomize the first few moves a little bit.
     int randomn = 0;
     int depth = 99;	// search depth
     double dif = 0.0;		// stop randomizing if the value drops this much
     // if the "dif" and "randomn" arguments to Find_Static_Best_Move
     // are both > 0, then alpha-beta will be disabled to avoid randomly
     // picking moves whose value is uncertain due to cutoffs.  This makes
     // the search MUCH slower so depth ought to be limited
     // if ((randomn>0)&&(dif>0.0)) { depth--; }
     // for games where there are no "fools mate" type situations
     // the best solution is to use dif=0.0;  For games with fools mates,
     // set dif so the really bad choices will be avoided
     boardSearchLevel = 0;

     Search_Driver search_state = Setup_For_Search(depth, false);
     try
        {

            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new SnakesMovespec("Done", board.whoseTurn);
            }


            search_state.save_all_variations = SAVE_TREE;
            search_state.allow_killer = KILLER;
            search_state.verbose=verbose;			// debugging
            search_state.save_top_digest = true;	// always on as a background check
            search_state.save_digest=false;	// debugging only
            search_state.check_duplicate_digests = false; 	// debugging only

            if (move == null)
            {
                move = (SnakesMovespec) search_state.Find_Static_Best_Move(randomn,dif);
            }
        }
        finally
        {
            Accumulate_Search_Summary();
            Finish_Search_In_Progress();
        }
        board.setState(bstate); 
        if (move != null)
        {
            if(G.debug() && (move.op!=MOVE_DONE)) { move.showPV("exp final pv: "); }
            // normal exit with a move
            System.out.println(""+number_of_solutions+" distinct solutions "+search_state.search_clock+" steps");
            if(continuous)
            {
            	moveSequence = (SnakesMovespec)move.best_move();
            }
            return (move);
        }

        continuous = false;
        // abnormal exit
        return (null);
    }




 }