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
package frogs;

import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

import lib.*;


/** 
 * Frogs uses alpha-beta only
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * @author ddyer
 *
 */
public class FrogPlay extends commonMPRobot<FrogBoard> implements Runnable,
    RobotProtocol
{	
	private final double VALUE_OF_WIN = 1000.0;
	public double valueOfWin() { return VALUE_OF_WIN; }
	
	private boolean KILLER_HEURISTIC = false;
	private boolean SAVE_TREE = false;				// debug flag for the search driver
	private int DUMBOT_DEPTH = 4;
	private int WEAKBOT_DEPTH = 3;
	private int MAX_DEPTH = DUMBOT_DEPTH;
	private int Strategy = DUMBOT_LEVEL;
    
	private int boardSearchLevel = 0;				// the current search depth

    /* constructor */
    public FrogPlay()
    {
    }


/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	FrogMovespec mm = (FrogMovespec)m;
        board.UnExecute(mm);
        boardSearchLevel--;
    }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   FrogMovespec mm = (FrogMovespec)m;
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
    // seek the end of the best_move chain for multiplayer games
    public commonMove targetMoveForEvaluation(commonMove cm)
    {	commonMove leaf = cm.best_move();
    	commonMove v = cm;
    	while(leaf!=null) { v = leaf; leaf=leaf.best_move(); }
    	return(v);
    }
    public double Static_Evaluate_Position(commonMove m)
    {	
     	int playerindex = m.player;
     	int nplay = board.nPlayers();
     	commonMPMove mm = (commonMPMove)m;
     	
     	mm.setNPlayers(nplay);
    	
    	for(int i=0;i<nplay; i++)
    	{	mm.playerScores[i] = ScoreForPlayer(board,i,false);
    	}
    	return(mm.reScorePosition(playerindex,VALUE_OF_WIN));
    }


    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    private double ScoreForPlayer(FrogBoard evboard,int player,boolean print)
    {	
     	boolean win = evboard.WinForPlayerNow(player);
     	// score wins as slightly better if in fewer moves
    	double val = win ? VALUE_OF_WIN+1.0/(1+boardSearchLevel) : 0.0;		// points for a win, less for everything else
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
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {
            FrogBoard evboard = (FrogBoard)(GameBoard.cloneBoard());
            String msg = "Eval is ";
            for(int i=0,lim=evboard.nPlayers(); i<lim; i++)
            {	msg += " "+ScoreForPlayer(evboard,i,true);
            }
            System.out.println(msg);
    }

/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.
 */
 public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,  String evaluator, int strategy)
    {
        InitRobot(newParam, info, strategy);
        GameBoard = (FrogBoard)gboard;
        board = (FrogBoard)(gboard.cloneBoard());
        Strategy = strategy;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy %s",Strategy);
        case WEAKBOT_LEVEL:	MAX_DEPTH = WEAKBOT_DEPTH; break;
        case DUMBOT_LEVEL:     MAX_DEPTH = DUMBOT_DEPTH; break;
        case SMARTBOT_LEVEL: MAX_DEPTH = DUMBOT_DEPTH+1; break;
        case BESTBOT_LEVEL: MAX_DEPTH = DUMBOT_DEPTH+2; break;
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
    board.copyFrom(GameBoard);

}
/** search for a move on behalf onf player p and report the result
 * to the game.  This is called in the robot process, so the normal
 * game UI is not encumbered by the search.
 */
 public commonMove DoAlphaBetaFullMove()
    {
        commonMove move = null;
        // it's important that the robot randomize the first few moves a little bit.
        int randomn = RANDOMIZE
        			? ((board.moveNumber <= 6) ? (20 - board.moveNumber) : 0) 
        			: 0;
        boardSearchLevel = 0;

        int depth = MAX_DEPTH;
        Search_Driver search_state = Setup_For_Search(depth, 5.0);
        try
        {
 
            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new FrogMovespec("Done", board.whoseTurn);
            }

            search_state.save_all_variations = SAVE_TREE;
            search_state.verbose=verbose;
            search_state.allow_killer = KILLER_HEURISTIC && ((Strategy!=DUMBOT_LEVEL)&&(Strategy!=WEAKBOT_LEVEL));
            search_state.save_top_digest = true;
            search_state.save_digest=false;	// debugging only
            search_state.check_duplicate_digests = false; 	// debugging only
            search_state.good_enough_to_quit = VALUE_OF_WIN;
            search_state.allow_good_enough = true;

            if (move == null)
            {
                move = search_state.Find_Static_Best_Move(randomn);
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
