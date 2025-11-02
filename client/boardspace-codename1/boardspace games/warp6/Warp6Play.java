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
package warp6;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

/** 
 * Warp6 uses alpha-beta
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * 
 * A special feature of Knockabout is that the dice rolls add an element of chance
 * to the outcome.  This very simple robot completely ignores the issue, it just
 * rolls and assumes that whatever it rolled is what it will get.  It seems to
 * give pretty good results anyway.  The plan if it becomes necessary to do better
 * is to do rollouts.
 * 
 * Some care has to be taken to make sure the "fixed" randomness in the robot
 * doesn't leak out to the real game, for example, the robot generates a move
 * based on an expected roll, but it shouldn't actually get that roll.
 * 
 * @author ddyer
 *
 */
public class Warp6Play extends commonRobot<Warp6Board> implements Runnable, Warp6Constants,  RobotProtocol
{   boolean SAVE_TREE = false;				// debug flag for the search driver
    boolean KILLER = false;
	static final double VALUE_OF_WIN = 10000000.0;
    static final int DUMBOT_DEPTH = 6;
    static final int GOODBOT_DEPTH = 7;
    static final int BESTBOT_DEPTH = 6;
    int MAX_DEPTH = BESTBOT_DEPTH;
     /* strategies */
    final int SIMPLE_MAX = 1;
    int Strategy = SIMPLE_MAX;
    boolean DUMBOT = false;
    int boardSearchLevel = 0;				// the current search depth

    /* constructor */
    public Warp6Play()
    {
    }

/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	Warp6Movespec mm = (Warp6Movespec)m;

        board.UnExecute(mm);
        boardSearchLevel--;
    }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   Warp6Movespec mm = (Warp6Movespec)m;
//    if((mm.from_col=='J') && (mm.to_col=='J') && (mm.to_row==5) && (mm.from_row==4))
 //   {System.out.println("maybe here");
  //  }

    	boardSearchLevel++;
        board.RobotExecute(mm);
    }

/** return an enumeration of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {	CommonMoveStack  all = new CommonMoveStack();
    	board.GetListOfMoves(all);
        return(all);
    }
    
    

    
    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    private double ScoreForPlayer(Warp6Board evboard,int player,boolean print)
    {	double sc = evboard.ScoreForPlayer(player,print,DUMBOT);
    	return(sc);

    }
    
    /**
     * this is it! just tell me that the position is worth.  
     */
    public double Static_Evaluate_Position(commonMove m)
    {	int playerindex = m.player;
    	if(board.GameOver())
    	{
         	boolean win = board.WinForPlayer(playerindex);
        	if(win) { return(VALUE_OF_WIN+(1.0/(1+boardSearchLevel))); }
         	boolean win2 = board.WinForPlayer(playerindex^1);
        	if(win2) { return -(VALUE_OF_WIN+1-(1.0/(1+boardSearchLevel))); }
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
    	Warp6Board evboard = GameBoard.cloneBoard();
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
        GameBoard = (Warp6Board) gboard;
        board = GameBoard.cloneBoard();
        switch(strategy)
        {
        case WEAKBOT_LEVEL:
        case DUMBOT_LEVEL:
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
        Strategy = SIMPLE_MAX;
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
 
 public commonMove DoFullMove()
    {
	 Warp6Movespec move = null;
     int moven = board.moveNumber;
     // it's important that the robot randomize the first few moves a little bit.
     int randomn = RANDOMIZE 
     				? ((moven <= 17) ? (17 - board.moveNumber) : 0)
     				: 0;
     boardSearchLevel = 0;
     int depth = MAX_DEPTH - ((moven<5)?1:0);
     Search_Driver search_state = Setup_For_Search(depth, false);
     
     try
        {
            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new Warp6Movespec("Done", board.whoseTurn);
            }

            search_state.save_all_variations = SAVE_TREE;
            search_state.save_top_digest = true;
            search_state.allow_killer = KILLER;
            search_state.verbose=verbose;			// debugging
            search_state.save_digest=false;	// debugging only.  Even when debugging true triggers false alerts
				// because some moves re-roll to the same configuration
           search_state.check_duplicate_digests = false; 	// debugging only
           search_state.good_enough_to_quit = VALUE_OF_WIN;
           search_state.allow_good_enough = true;

 
            if (move == null)
            {  	
                move = (Warp6Movespec) search_state.Find_Static_Best_Move(randomn,10.0);
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
            // avoid cheating in a subtle way, since the move encodes
            // the roll you get, the robot would have based its move on
            // knowing the value of the roll it gets.
            move.rollAmount = Math.abs(GameBoard.nextRandom());
            return (move);
        }

        continuous = false;
        // abnormal exit
        return (null);
    }




 }