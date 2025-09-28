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
package oneday;


import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

/** 
 * Oneday uses MCTS only
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * 
 * OnedayInLondon uses a UCT robot, with a couple of slightly unusual twists.  
 *  (1) rather than run to endgame, it runs to a fixed depth and scores the result
 *  using a simple evaluator.  Effectively this is a modified hill-climbing evaluation
 *  which is well suited to the game.
 *  (2) the draw piles and other hidden cards are re-randomized for every run, so that
 *  the robot doesn't get any leaked information about the identity of the hidden cards.
 *  This is especially important in the initial setup phase, where the 10 cards that will
 *  end up in the rack are supposed to be known only one at a time.
 *    
 * @author ddyer
 *
 */
public class OnedayPlay extends commonRobot<OnedayBoard> implements Runnable, OnedayConstants,
    RobotProtocol
{   boolean SAVE_TREE = false;				// debug flag for the search driver
    boolean RESHUFFLE = false;				// if true, reshuffle the deck to conceal information
    int FINAL_DEPTH = 30;					// stopping point for the playout
    boolean UCT_WIN_LOSS = false;			// if true, score montebot strictly on win/loss
    boolean changed_to_synchronous = false;
    boolean dumbPhase = false;				// when true reduce the search time
	static final double VALUE_OF_WIN = 1.0;
     /* strategies */
    int boardSearchLevel = 0;				// the current search depth
    /* constructor */
    public OnedayPlay()
    {
    }

/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	OnedayMovespec mm = (OnedayMovespec)m;
    	boardSearchLevel--;
    	board.UnExecute(mm);
     }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   OnedayMovespec mm = (OnedayMovespec)m;
        board.RobotExecute(mm);
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
    double ScoreForPlayer(OnedayBoard evboard,int player,boolean print)
    {	
     	boolean win = evboard.WinForPlayerNow(player);
    	if(win) { return(VALUE_OF_WIN+(1.0/(1+boardSearchLevel))); }
    	return(evboard.ScoreForPlayer(player,print,false));

    }
    


/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.
 */
 public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
        String evaluator, int strategy)
    {
        InitRobot(newParam, info, strategy);
        GameBoard = (OnedayBoard) gboard;
        board = (OnedayBoard)GameBoard.cloneBoard();
    	RESHUFFLE = true;
    	MONTEBOT = true;
        switch(strategy)
        {
        case WEAKBOT_LEVEL:
        	WEAKBOT = true;
			//$FALL-THROUGH$
		case DUMBOT_LEVEL:
        case SMARTBOT_LEVEL:
        case BESTBOT_LEVEL:
        	break;
        case MONTEBOT_LEVEL:
        	break;
        default: throw G.Error("Not expecting strategy %s",strategy);
        }
    }
    /** copy the game board, in preparation for a search */
    public void InitBoardFromGame(int pl)
    {
        board.copyFrom(GameBoard);
        if(RESHUFFLE)
        {
        	board.setRobotGame(pl,rand);
         	if(board.getState()==OnedayState.Place) 
        		{ board.setState(OnedayState.SynchronousPlace);
        		  changed_to_synchronous = true;
        		}
        	if(board.getState()==OnedayState.SynchronousPlace)
        	{	if(board.pickedObject!=null) 
        			{ board.unPickObject(); }
        		board.setWhoseTurn(pl);
        	}
           	board.reRandomize();
        }
    }
/** search for a move on behalf onf player p and report the result
 * to the game.  This is called in the robot process, so the normal
 * game UI is not encumbered by the search.
 */
 public void PrepareToMove(int playerIndex)
 {	InitBoardFromGame(playerIndex);
 }

//
// this is the level in the UCT search tree at which random moves are requested.
// when search level exceeds this, we reshuffle the draw pile so each run gets a new
// card order.
//
 int random_move_search_level = 0;
 /**
 * get a random move by selecting a random one from the full list.
  * for games which have trivial move generators, this is "only" a factor of 2 or so improvement
  * in the playout rate.  For games with more complex move generators, it can by much more.
  * Diagonal-Blocks sped up by 10x 
  * 
  */
 public commonMove Get_Random_Move(Random rand)
 {	//return(board.Get_Random_Oneday_Move(rand));
	 if(RESHUFFLE)
	 {int rsl = random_move_search_level;
     random_move_search_level = boardSearchLevel;
	 if(boardSearchLevel<rsl)
	 {	// reshuffle at the top, but use the same shuffle all the way down for one run
		 return(new OnedayMovespec(OnedayMovespec.MOVE_SHUFFLE,board.whoseTurn));
	 }}
	 return(super.Get_Random_Move(rand));
 }
 /**
  * for UCT search, return the normalized value of the game, with a penalty
  * for longer games so we try to win in as few moves as possible.  Values
  * must be normalized to -1.0 to 1.0
  */
 public double NormalizedScore(commonMove m)
 {	 OnedayMovespec mm = (OnedayMovespec)m;
	 int playerindex = m.player;
	 int nplay = board.nPlayers();
	 mm.setNPlayers(nplay);
	 double scores[] = mm.playerScores;
	 boolean win = board.WinForPlayerNow(m.player);
	 if(win) { scores[playerindex] = UCT_WIN_LOSS? 1.0 : 0.8+0.2/boardSearchLevel;}
	 else
	 {
		 for(int i=0;i<nplay; i++)
		 {	scores[i] = ScoreForPlayer(board,i,dumbPhase)/101;
		 }
	 }
	 return(mm.reScorePosition(playerindex,1));
 }
 public double reScorePosition(commonMove cm,int forplayer)
 {	return cm.reScorePosition(forplayer,VALUE_OF_WIN);
 }


 // this is the monte carlo robot, which for some games is much better then the alpha-beta robot
 // for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
 // evaluator other than winning a game.
 public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
 	try {
       if (board.DoneState())
        { // avoid problems with gameover by just supplying a done
            move = new OnedayMovespec(MOVE_DONE, board.whoseTurn);
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
        dumbPhase = (board.getState()==OnedayState.SynchronousPlace);
        double randomn = (RANDOMIZE && (board.moveNumber <= 6)) ? 0.1/board.moveNumber : 0.0;
        UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this,true);
        monte_search_state.save_top_digest = true;	// always on as a background check
        monte_search_state.save_digest=false;	// debugging only
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.timePerMove = (dumbPhase?1:10);		// 10 seconds per move
        monte_search_state.verbose = verbose;
        monte_search_state.alpha = 0.5;
        monte_search_state.sort_moves = false;
        monte_search_state.final_depth = FINAL_DEPTH;	// longest possible game
        monte_search_state.only_child_optimization = true;
        monte_search_state.dead_child_optimization = true;
        monte_search_state.simulationsPerNode = 1;
        monte_search_state.node_expansion_rate = 1.0;
        monte_search_state.randomize_uct_children = true;     
        monte_search_state.random_moves_per_second = WEAKBOT ? 1000 : 120000;
        // terminal node optimizations don't work with, because the things
        // it sees are only possibilities, not facts.  So seeing the possibility of
        // a win or loss isn't a good motivation for remembering as a fact.
        //
        monte_search_state.terminalNodeOptimization = false;
        move = monte_search_state.getBestMonteMove();
        }
 		}
      finally { ; }
      if(move==null) { continuous = false; }
      if(move!=null 
    		  && changed_to_synchronous
    		  && (move.op == OnedayMovespec.MOVE_TO_RACK))
      	{// change the move back to ephemeral
    	  move.op = OnedayMovespec.EPHEMERAL_TO_RACK;
      	}
     return(move);
 }

 }