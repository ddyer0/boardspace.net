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
package sixmaking;


import static sixmaking.SixmakingMovespec.*;
import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

/** 
 * Sixmaking uses Mcts
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * @author ddyer
 *
 */
public class SixmakingPlay extends commonRobot<SixmakingBoard> implements Runnable, SixmakingConstants,
    RobotProtocol
{   
	private boolean UCT_WIN_LOSS = true;			// if true, score montebot strictly on win/loss
	private boolean USE_BLITZ = false;
	private int TIMEPERMOVE = 5;
	private double NODE_EXPANSION_RATE = 1.0;
	private double ALPHA = 0.5;
	private int UCT_CHILD_LIMIT = 100000;
	private boolean CHILD_LIMIT_STOP = true; 	// stop when child limit is reached.
	private double RANDOMSCALE = 0.1;
    /**
     * winning move optimization, specific to sixmaking for now, checks the list of moves
     * and if there are any winning move, returns only the winning move.  The thought behind
     * this is that the 40 or so non-winning moves dilute the signal that a winning move is
     * available as a direct successor of this node.
     */
	private boolean WINNINGMOVEOPTIMIZATION = false;
 
    /* constructor */
    public SixmakingPlay()
    {
    }


/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	SixmakingMovespec mm = (SixmakingMovespec)m;
    	board.UnExecute(mm);
     }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   SixmakingMovespec mm = (SixmakingMovespec)m;
        board.RobotExecute(mm);
    }

/** return an enumeration of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {   CommonMoveStack  moves = board.GetListOfMoves();
    	if(WINNINGMOVEOPTIMIZATION)
    	{
    		for(int lim=moves.size()-1; lim>=0; lim--)
    		{
    			SixmakingMovespec m = (SixmakingMovespec)moves.elementAt(lim);
    			switch(m.op)
    			{
    			case MOVE_BOARD_BOARD:
    				SixmakingCell c = board.getCell(m.to_col,m.to_row);
    				if(m.height+c.height()>=6)
    				{	SixmakingCell src = board.getCell(m.from_col,m.from_row);
    					if(src.topChip()==PlayerChip[m.player])
    					{
    					// we have a winner
    					moves.clear();
    					moves.push(m);
    					return(moves);
    					}
    				}
    				break;
    			default: break;
    			}
    		}
    	}
    	return(moves);
    }
    

/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.
 */
 public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
        String evaluator, int strategy)
    {
        InitRobot(newParam, info, strategy);
        GameBoard = (SixmakingBoard) gboard;
        board = (SixmakingBoard)GameBoard.cloneBoard();
        MONTEBOT = true;
        terminalNodeOptimize = true;
        switch(strategy)
        {
        case WEAKBOT_LEVEL:
        	WEAKBOT = true;
			//$FALL-THROUGH$
        case DUMBOT_LEVEL:
        	// implements an unwind mode monte bot.
        	// faster and kicks ass.
         	UCT_WIN_LOSS = false;
         	TIMEPERMOVE = 1;
         	NODE_EXPANSION_RATE=1.0;
         	WINNINGMOVEOPTIMIZATION = false;
         	UCT_CHILD_LIMIT = 3000;
         	CHILD_LIMIT_STOP = false;
         	RANDOMSCALE = 0.3;
         	ALPHA = 0.5;
         	USE_BLITZ = false;
        	break;
        case SMARTBOT_LEVEL:
        	// implements a blitz mode monte bot
        	// slower than unwind mode
        	UCT_WIN_LOSS = false;
        	TIMEPERMOVE = 5;
        	NODE_EXPANSION_RATE=1.0;
        	UCT_CHILD_LIMIT = 200000;
        	RANDOMSCALE = 0.1;
        	ALPHA = 0.5;
        	CHILD_LIMIT_STOP = true;
        	WINNINGMOVEOPTIMIZATION = true;
         	USE_BLITZ = false;
        	break;
        case BESTBOT_LEVEL:
        	UCT_WIN_LOSS = false;
           	TIMEPERMOVE = 60;
           	NODE_EXPANSION_RATE=1.0;
           	UCT_CHILD_LIMIT = 200000;
           	RANDOMSCALE = 0.1;
        	CHILD_LIMIT_STOP = false;
        	ALPHA = 0.5;
        	WINNINGMOVEOPTIMIZATION = true;
        	USE_BLITZ = false;
        	break;
        case MONTEBOT_LEVEL:
         	UCT_WIN_LOSS = false;
           	TIMEPERMOVE = 60;
           	UCT_CHILD_LIMIT = 200000;
           	RANDOMSCALE = 0.1;
          	NODE_EXPANSION_RATE=0.05;
        	CHILD_LIMIT_STOP = true;
           	ALPHA = 1.0;
        	WINNINGMOVEOPTIMIZATION = true;
        	USE_BLITZ = false;
        	break;
        default: throw G.Error("Not expecting strategy %s",strategy);
        }
    }
    /** copy the game board, in preparation for a search */
    public void InitBoardFromGame()
    {
        board.copyFrom(GameBoard);
        board.acceptPlacement();
    }
/** search for a move on behalf onf player p and report the result
 * to the game.  This is called in the robot process, so the normal
 * game UI is not encumbered by the search.
 */
 public void PrepareToMove(int playerIndex)
 {	InitBoardFromGame();
 }



 /**
 * get a random move by selecting a random one from the full list.
  * for games which have trivial move generators, this is "only" a factor of 2 or so improvement
  * in the playout rate.  For games with more complex move generators, it can by much more.
  * Diagonal-Blocks sped up by 10x 
  * 
  */
 public commonMove Get_Random_Move(Random rand)
 {	
	 return(super.Get_Random_Move(rand));
 }
 
 public double simpleScore(int who)
 {
	 return(board.simpleScore(who));
	 
 }
 
 /**
  * for UCT search, return the normalized value of the game, with a penalty
  * for longer games so we try to win in as few moves as possible.  Values
  * must be normalized to -1.0 to 1.0
  */
 int games = 0;
 int wonGames = 0;
 public double NormalizedScore(commonMove lastMove)
 {	int player = lastMove.player;
 	games++;
 	if(board.gameOverNow())
 	{
 	wonGames++;
 	// for games where we may be depth limited without a definite win or loss,
    // its important for actual draws to be scored properly at 0
 	boolean win = board.winForPlayerNow(player);
 	if(win) { return(UCT_WIN_LOSS? 1.0 : 0.8+0.2/board.robotDepth); }
 	boolean win2 = board.winForPlayerNow(nextPlayer[player]);
 	if(win2) { return(- (UCT_WIN_LOSS?1.0:(0.8+0.2/board.robotDepth))); }
 	return(0);	// draw
 	}
 	return(simpleScore(player));
 }

 // this is the monte carlo robot, which for some games is much better then the alpha-beta robot
 // for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
 // evaluator other than winning a game.
 public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
 	try {
       if (board.DoneState())
        { // avoid problems with gameover by just supplying a done
            move = new SixmakingMovespec(MOVE_DONE, board.whoseTurn);
        }
        else 
        {
         	// this is a test for the randomness of the random move selection.
         	// "true" tests the standard slow algorithm
         	// "false" tests the accelerated random move selection
         	// the target value is 5 (5% of distributions outside the 5% probability range).
         	// this can't be left in the production applet because the actual chi-squared test
         	// isn't part of the standard kit.
        	// also, in order for this to work, the MoveSpec class has to implement equals and hashCode
         	//RandomMoveQA qa = new RandomMoveQA();
         	//qa.runTest(this, new Random(),100,false);
         	//qa.report();
         	
        // it's important that the robot randomize the first few moves a little bit.
        double randomn = (RANDOMIZE && (board.moveNumber <= 10)) ? RANDOMSCALE/board.moveNumber : 0.0;
        UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this);
        monte_search_state.save_top_digest = true;	// always on as a background check
        monte_search_state.save_digest=!USE_BLITZ && false;	// debugging only, not available in blitz mode
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.timePerMove = TIMEPERMOVE;		// seconds per move
        monte_search_state.verbose = verbose;
        monte_search_state.alpha = ALPHA;
        monte_search_state.sort_moves = false;
        monte_search_state.final_depth = 200;	// longest possible game
        monte_search_state.only_child_optimization = true;
        monte_search_state.dead_child_optimization = true;
        monte_search_state.simulationsPerNode = 1;
        //
        //development note, with node_expansion_rate = 1.0 and the default stored_child_limit
        //of 200000, 30 seconds per move lost every game to 5 seconds per move.  Increasing the
        //node limit of reducing the expansion rate restored the normal time relationship
        monte_search_state.node_expansion_rate = NODE_EXPANSION_RATE;
       // monte_search_state.stored_child_limit = 1400000;
        monte_search_state.stored_child_limit = UCT_CHILD_LIMIT;
        monte_search_state.stored_child_limit_stop = CHILD_LIMIT_STOP;

        monte_search_state.randomize_uct_children = true;     
        monte_search_state.random_moves_per_second = 
        		WEAKBOT ? 10000 : USE_BLITZ ? 200000 : 227550;	// unexecute mode
        monte_search_state.blitz = USE_BLITZ;
        monte_search_state.maxThreads = DEPLOY_THREADS;
        monte_search_state.terminalNodeOptimization = terminalNodeOptimize;
        move = monte_search_state.getBestMonteMove();
        }
 		}
      finally { ; }
      if(move==null) { continuous = false; }
     return(move);
 }


 }