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
package tablut;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;


/** 
 * Tablut uses Alpha-beta
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * @author ddyer
 *
 */
public class TabPlay extends commonRobot<TabGameBoard> implements Runnable, TabConstants,
    RobotProtocol
    {
    private static final double VALUE_OF_WIN = 100000.0;
    private boolean SAVE_TREE = false;				// debug flag for the search driver.  Uses lots of memory
    private static final int WEAKBOT_DEPTH = 4;
    private static final int DUMBOT_DEPTH = 5;
    private static final int SMARTBOT_DEPTH = 6;
    private static final int BESTBOT_DEPTH = 7;
    private int MAX_DEPTH = DUMBOT_DEPTH;						// search depth.
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
    private int Strategy = DUMBOT_LEVEL;
    
    private int boardSearchLevel = 0;				// the current search depth

    /* constructor */
    public TabPlay()
    {
    }


/** Called from the search driver to undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	Tabmovespec mm = (Tabmovespec)m;
        board.UnExecute(mm);
        boardSearchLevel--;
    }
/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   Tabmovespec mm = (Tabmovespec)m;
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
    double SWEEP_WEIGHT = 10.0;
    double TOTAL_SWEEP_WEIGHT = -1.0;
    double WOOD_WEIGHT = 10.0;
    double OPEN_RANK_WEIGHT = 10.0;	// good for gold, lines are counted twice
    double dumbotEval(TabGameBoard evboard,int player,boolean print)
    {	double val = 0.0;
    	if(evboard.playerChip[player]==TabChip.GoldShip)
    	{
    	String msg = "";
     	evboard.classify();
    	if(evboard.flagShipLocation!=null)
    	{	double lv = (evboard.ncols-evboard.flagShipLocation.sweep_score)*SWEEP_WEIGHT;
    		val += lv;
    		if(print) { msg += "sweep "+lv; }
    	}
    	{
    	double lv = ((double)evboard.totalSweepScore/(evboard.ncols*evboard.nrows))*TOTAL_SWEEP_WEIGHT;
    	val += lv;
    	if(print) { msg += " total sweep "+lv; }
    	}
    	{
        	double lv = evboard.open_ranks*OPEN_RANK_WEIGHT;
        	val += lv;
        	if(print) { msg += " open ranks "+lv; }
        	}

    	{
    	double lv = (evboard.gold_ships*2 - evboard.silver_ships)*WOOD_WEIGHT;
    	val += lv;
    	if(print) { msg += " wood "+lv; System.out.println(msg);}
    	}
    	}
    	//G.Error("Not implemented");
     	return(val);
    }
    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    private double ScoreForPlayer(TabGameBoard evboard,int player,boolean print)
    {	
		double val = 0.0;
      	
    	switch(Strategy)
    	{	default: throw G.Error("Not expecting strategy %s",Strategy);
    		case WEAKBOT_LEVEL:
    		case DUMBOT_LEVEL: 
    		case SMARTBOT_LEVEL: 
    		case BESTBOT_LEVEL: 	// all the same for now
   			val = dumbotEval(evboard,player,print);
    	}
     	return(val);
    }
    
    /**
     * this is it! just tell me that the position is worth.  
     */
    public double Static_Evaluate_Position(commonMove m)
    {	int playerindex = m.player;
    	if(board.GameOver())
    	{
        	boolean win = board.WinForPlayerNow(playerindex);
         	// make wins in fewer moves look slightly better. Nothing else matters.
         	if(win) {   return VALUE_OF_WIN+(1.0/(1+boardSearchLevel));	}
        	boolean win2 = board.WinForPlayerNow(playerindex^1);
         	if(win2) {   return -(VALUE_OF_WIN+1-(1.0/(1+boardSearchLevel)));	}
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
    	TabGameBoard evboard = GameBoard.cloneBoard();
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
        GameBoard = (TabGameBoard) gboard;
        board = GameBoard.cloneBoard();
        Strategy = strategy;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy %s",strategy);
        case WEAKBOT_LEVEL:	MAX_DEPTH = WEAKBOT_DEPTH; break;
        case DUMBOT_LEVEL: MAX_DEPTH = DUMBOT_DEPTH; break;
        case SMARTBOT_LEVEL: MAX_DEPTH = SMARTBOT_DEPTH; break;
        case BESTBOT_LEVEL: MAX_DEPTH = BESTBOT_DEPTH; break;
        }
    }
    /** copy the game board, in preparation for a search */
    public void InitBoardFromGame()
    {
        board.copyFrom(GameBoard);
        board.sameboard(GameBoard);
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
/** search for a move on behalf of player p and report the result
 * to the game.  This is called in the robot process, so the normal
 * game UI is not encumbered by the search.
 */
 public commonMove DoFullMove()
    {
        commonMove move = null;
        try
        {
 
            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new Tabmovespec("Done", board.whoseTurn);
            }

            // it's important that the robot randomize the first few moves a little bit.
            int randomn = RANDOMIZE 
            				? ((board.moveNumber <= 6) ? (20 - board.moveNumber) : 0)
            				: 0;
            boardSearchLevel = 0;

            int depth = MAX_DEPTH;
            if(randomn>0) { depth--; // because no alpha-beta if random and threshold
            }
            Search_Driver search_state = Setup_For_Search(depth, false);
            search_state.save_all_variations = SAVE_TREE;
            search_state.good_enough_to_quit = GOOD_ENOUGH_VALUE;
            search_state.allow_good_enough = true;
            search_state.verbose = verbose;
            search_state.allow_killer = KILLER;
            search_state.save_digest = false;	// debug only
            if (move == null)
            {
                move = search_state.Find_Static_Best_Move(randomn,VALUE_OF_WIN/4);
                search_state.showResult(move,false);
            }
        }
        finally
        {
            Accumulate_Search_Summary();
            Finish_Search_In_Progress();
        }

        continuous &= move!=null;
            return (move);
    }




 }
