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
package mogul;


import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

/** 
 * mogul uses MCTS only
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * 
 * This robot is simplified to support only montecarlo search.
 * 
 * @author ddyer
 *
 */
public class MogulPlay extends commonMPRobot<MogulBoard> implements Runnable, MogulConstants,
    RobotProtocol
{   
	public double valueOfWin() { return 1.0; }
	
	boolean SAVE_TREE = false;				// debug flag for the search driver
    boolean UCT_WIN_LOSS = true;			// if true, score montebot strictly on win/loss
    boolean KILLER = false;					// probably ok for all games with a 1-part move
    boolean RESHUFFLE = false;
	int strategy = 0;
     
    /* constructor */
    public MogulPlay()
    {
    }

/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	MogulMovespec mm = (MogulMovespec)m;
    	board.UnExecute(mm);

     }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   MogulMovespec mm = (MogulMovespec)m;
    	if(mm.uctNode()!=null)
    	{	// this is a replay node, which may be invalid after a shuffle
    		// revisiting a move, as in replaying from the tree.  There's a rare
    		// problem when revisiting a move, if the re-randomization at the top
    		// of the tree can make this move no longer legal.  So the ugly choices are
    		// to wait for something to go wrong, after executing an illegal move,
    		// or pay the overhead to check first.
    		CommonMoveStack all = board.GetListOfMoves();
    		boolean ok=false;
    		for(int lim = all.size()-1; !ok && lim>=0; lim--)
    		{
    			commonMove alt = all.elementAt(lim);
    			if(mm.Same_Move_P(alt)) { ok = true; }
    		}
    		if(!ok)
    			{ // we picked an illegal move, so make it look as undesirable as possible,
    			  // don't do it and continue with the search.
    			  //G.print("Rare: robot walked into an illegal move "+mm);
       			  board.terminateWithPrejudice(mm);
       			  return;
    			}
    		
    	}
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
    
    


/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.
 */
 public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
        String evaluator, int strat)
    {
        InitRobot(newParam, info, strategy);
        GameBoard = (MogulBoard) gboard;
        board = (MogulBoard)GameBoard.cloneBoard();
        strategy = strat;
        MONTEBOT = true;
        terminalNodeOptimize = true;
       	switch(strategy)
        {
       	case WEAKBOT_LEVEL:
       		WEAKBOT = true;
			//$FALL-THROUGH$
		case DUMBOT_LEVEL:
         	RESHUFFLE = true;
         	break;
        case SMARTBOT_LEVEL:
         	RESHUFFLE = true;
        	break;
        case BESTBOT_LEVEL:
         	RESHUFFLE = true;
        	break;
        case MONTEBOT_LEVEL:
         	RESHUFFLE = true;
        	break;
        default: throw G.Error("Not expecting strategy %s",strategy);
        }
    }
    /** copy the game board, in preparation for a search */
    public void InitBoardFromGame()
    {
        board.copyFrom(GameBoard);
        board.reShuffle(rand,board.deck);	// robot never sees the real deck
        if(RESHUFFLE)
        {
        	board.setRobotRandom(rand);
        }
    }
/** search for a move on behalf onf player p and report the result
 * to the game.  This is called in the robot process, so the normal
 * game UI is not encumbered by the search.
 */
 public void PrepareToMove(int playerIndex)
 {	InitBoardFromGame();
 }

 int random_move_search_level = 0;
 /**
 * get a random move by selecting a random one from the full list.
  * for games which have trivial move generators, this is "only" a factor of 2 or so improvement
  * in the playout rate.  For games with more complex move generators, it can by much more.
  * Diagonal-Blocks sped up by 10x 
  * 
  */
 public commonMove Get_Random_Move(Random rand)
 {	 if(RESHUFFLE)
	 {int rsl = random_move_search_level;
     random_move_search_level = board.robotDepth;
	 if(board.robotDepth<rsl)
	 {	// reshuffle at the top, but use the same shuffle all the way down for one run
		 return(new MogulMovespec(MogulMovespec.MOVE_SHUFFLE,board.whoseTurn));
	 }}
	 return(super.Get_Random_Move(rand));
 }

 /**
  * for UCT search, return the normalized value of the game, with a penalty
  * for longer games so we try to win in as few moves as possible.  Values
  * must be normalized to -1.0 to 1.0
  */
 public double NormalizedScore(commonMove lastMove)
 {	
 	if(lastMove==board.terminatedWithPrejudice)
 	{
 		return( -1 );
 	}
	int np = board.nPlayers();
	commonMPMove mm = (commonMPMove)lastMove;
	mm.setNPlayers(np);
	double score[] = mm.playerScores;
  	for(int i=0;i<np;i++)
 	{	boolean win =  board.WinForPlayerNow(i);
 		score[i]= win ? UCT_WIN_LOSS?1.0:(0.8+0.2/board.robotDepth) : 0;
 	}
  	return(reScorePosition(lastMove,lastMove.player));
 }

 // this is the monte carlo robot, which for some games is much better then the alpha-beta robot
 // for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
 // evaluator other than winning a game.
 public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
 	try {
       if (board.DoneState())
        { // avoid problems with gameover by just supplying a done
            move = new MogulMovespec(MOVE_DONE, board.whoseTurn);
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
        double randomn = (RANDOMIZE && (board.moveNumber <= 6)) ? 0.1/board.moveNumber : 0.0;
        UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this,true);
        monte_search_state.save_top_digest = true;	// always on as a background check
        monte_search_state.save_digest=false;		// debugging only
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.timePerMove = 4;		// 4 seconds per move
        monte_search_state.verbose = 3;
        monte_search_state.alpha = 0.5;
        monte_search_state.sort_moves = false;
        monte_search_state.final_depth = 999;	// longest possible game
        monte_search_state.only_child_optimization = true;
        monte_search_state.dead_child_optimization = true;
        monte_search_state.simulationsPerNode = 1;
        monte_search_state.node_expansion_rate = 2.0;
        monte_search_state.randomize_uct_children = true;     
        monte_search_state.random_moves_per_second = WEAKBOT ? 10000:1000000;
        monte_search_state.max_random_moves_per_second = 3000000;
        monte_search_state.maxThreads = DEPLOY_THREADS;
        monte_search_state.blitz = false;
        monte_search_state.terminalNodeOptimization = terminalNodeOptimize;
        move = monte_search_state.getBestMonteMove();
        }
 		}
      finally { ; }
      if(move==null) { continuous = false; }
     return(move);
 }

 }