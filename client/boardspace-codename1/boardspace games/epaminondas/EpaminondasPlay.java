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
package epaminondas;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;


/** 
 * Epamandos uses alpha-beta only
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * <p>
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * <p>
 * Notwithstanding the "only" above, debugging robot players can be very
 * difficult, both at the elementary level when the robot crashes out or
 * produces obviously wrong results, or at the advanced level when the robot
 * produces an undesirable result that is not blatantly wrong.
 * <p>
 * debugging aids:
 * <p>
 * <li>{@link #List_Of_Legal_Moves} should produce only legal moves, and should
 * by default produce all legal moves, but if your board class has some consistency
 * checking, errors constructing the move list might be detected. 
 * 
 * <li>Turn on the "start evaluator" action, and experiment with board positions
 * in puzzle mode.  Each new position will print the current evaluation.
 * 
 * <li>when the robot is stopped at a breakpoint (for example in {@link #Static_Evaluate_Position}
 * turn on the "show alternate board" option to visualize the board position.  It's usually
 * not a good idea to leave the option on when the robot is running because there will be
 * two threads using the data simultaneously, which is not expected.
 *
 * <li>turn on the save_digest and check_duplicate_digest flags.
 *
 ** <li>set {@link #verbose} to 1 or 2.  These produce relatively small amounts
 * of output that can be helpful understanding the progress of the search
 *
 ** <li>set a breakpoint at the exit of {@link #DoFullMove} and example the
 * top_level_moves variable of the search driver.  It contains a lot of information
 * about the search variations that were actually examined.
 *
 * <li>for a small search (shallow depth, few nodes) turn on {@link #SAVE_TREE}
 * and set a breakpoint at the exit of {@link #DoFullMove}
 * @author ddyer
 *
 */
public class EpaminondasPlay extends commonRobot<EpaminondasBoard> implements Runnable, EpaminondasConstants,
    RobotProtocol
    {
	// this is an internal value used to affect the search in several ways.  Normal "value of position" results
	// should be well below this in magnitude.  Searches are normally called off if the value of a position exceeds
	// this, indicating "we won".   It should be at least 2x any non-winning value the evaluator normally produces.
	// but it's exact value and scale are unimportant.  The main thing is to have a convenient range of values
	// for the evaluator to work with.
	
	// common parameters
    private boolean SAVE_TREE = false;				// debug flag for the search driver.  Uses lots of memory. Set a breakpoint after the search.
    private int Strategy = DUMBOT_LEVEL;			// the init parameter for this bot
    private EpaminondasChip movingForPlayer = null;	// optional, some evaluators care
    private boolean fastRandomWalk = true;			// optimize speed of generating random descent moves
    private boolean useMoveStats = false;			// use stats from the set of moves in the evaluator
    
	// alpha beta parameters
    static public final double VALUE_OF_WIN = 10000.0;
    private int DUMBOT_DEPTH = 8;
    private int MAX_DEPTH = 8;						// search depth.
    private static final boolean KILLER = false;	// if true, allow the killer heuristic in the search
    private static final double GOOD_ENOUGH_VALUE = VALUE_OF_WIN;	// good enough to stop looking
    private int boardSearchLevel = 1;				// the current search depth
  
    
     /**
     *  Constructor, strategy corresponds to the robot skill level displayed in the lobby.
     * 
     *  */

    public EpaminondasPlay()
    {
    }

    // not needed for alpha-beta searches, which do not use threads
    public RobotProtocol copyPlayer(String from)	// from is the thread name
    {	RobotProtocol c = super.copyPlayer(from);
    	EpaminondasPlay cc = (EpaminondasPlay)c;
    	cc.Strategy = Strategy;
    	cc.movingForPlayer = movingForPlayer;
    	cc.fastRandomWalk = fastRandomWalk;
    	cc.useMoveStats = useMoveStats;
    	cc.board.initRobotValues(cc);
    	return(c);
    }

    /** return true if the search should be depth limited at this point.  current
     * is the current search depth, max is the maximum you set for the search.
     * You're free to stop the search earlier or let it continue longer, but usually
     * it's best to conduct an entire search with the same depth.
     * @param current the current depth
     * @param max the declared maximum depth.
     * 
     */
/*    public boolean Depth_Limit(int current, int max)
    {	// for simple games where there is always one move per player per turn
    	// current>=max is good enough.  For more complex games where there could
    	// be several moves per turn, we have to keep track of the number of turn changes.
    	// it's also possible to implement quiescence search by carefully adjusting when
    	// this method returns true.
        return(current>=max);
   }*/
/** Called from the search driver to undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence. This is usually the most troublesome 
 * method to implement - everything else in the board manipulations moves "forward".
 * Among other things, Unmake_Move will have to undo captures and restore captured
 * pieces to the board, remove newly placed pieces from the board and so on.  
 * <p>
 * Among the most useful methods; 
 * <li>use the move object and the Make_Move method
 * to store the information you will need to perform the unmove.
 * It's also standard to restore the current player, the current move number,
 * and the current board state from saved values rather than try to deduce
 * the correct inverse of these state changes.
 * <li>use stacks in the board class to keep track of changes you need to undo, and
 * record only the index into the stack in the move object.
 * 
 * Not needed for blitz MonteCarlo searches
 */
    public void Unmake_Move(commonMove m)
    {	EpaminondasMovespec mm = (EpaminondasMovespec)m;
        board.UnExecute(mm);
        boardSearchLevel--;
    }
/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   EpaminondasMovespec mm = (EpaminondasMovespec)m;
        board.RobotExecute(mm);
        boardSearchLevel++;
    }
    
	public void prepareForDescent(UCTMoveSearcher from)
	{
		// called at the top of the tree descent
	}
    public void startRandomDescent()
    {
    	// we detect that the UCT run has restarted at the top
    	// so we need to re-randomize the hidden state.
    	//if(randomize) { board.randomizeHiddenState(robotRandom,robotPlayer); }
    	//terminatedWithPrejudice = -1;
    }


    /** return a Vector of moves to consider at this point.  It doesn't have to be
     * the complete list, but that is the usual procedure. Moves in this list will
     * be evaluated and sorted, then used as fodder for the depth limited search
     * pruned with alpha-beta.
     */
        public CommonMoveStack  List_Of_Legal_Moves()
        {	
            CommonMoveStack all = board.GetListOfMoves(null,999999);
            return(all);
        }

        /**
         * this works very inefficiently by generating all moves and picking one.
         * for many games, this can be replaced with a slightly less random but
         * much faster process.
         */
        public commonMove Get_Random_Move(Random rand)
        {	
            // using fastRandomWalk is at least 3x more nodes investigated,
        	// but note that the distribution of moves generated this way
        	// is not necessarily the same as picking one move from a
        	// complete list of alternatives.
        	int limit = fastRandomWalk ? 1 : 999999;
        	CommonMoveStack all = board.GetListOfMoves(rand,limit);
        	// the robot move generator randomizes the starting point of move generation
        	return all.elementAt(rand.fastUpto(all.size()));
        }
        
    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * Not needed for MonteCarlo searches
     * @param player
     * @return
     */
    double ScoreForPlayer(EpaminondasBoard evboard,int player,boolean print)
    {	
		double val = 0.0; 
		double myscore = 
				Strategy==BESTBOT_LEVEL
				? evboard.bestEval(player,useMoveStats)
				: Strategy==SMARTBOT_LEVEL
					? evboard.smartEval(player,useMoveStats)
					: evboard.dumbEval(player,useMoveStats);
		val += myscore;
		if(print) { G.print("ev ",player," ",val); }
     	return(val);
    }

    /**
     * this re-evaluates the current position from the viewpoint of forplayer.
     * for 2 player games this is to trivially negate the value, but for multiplayer
     * games it requires considering multiple player's values.
     * Not needed for MonteCarlo searches
     */
    public double reScorePosition(commonMove m,int forplayer)
    {	return(m.reScorePosition(forplayer));
    }
    // TODO: back-door an expensive calculation (ie: total mobility) by recording it for
    // the parent before descending. We'll have an out of date metric that is free to
    // calculate.
    /** this is called from the search driver to evaluate a particular position. The driver
     * calls List_of_Legal_Moves, then calls Make_Move/Static_Evaluate_Position/UnMake_Move
     *  for each and sorts the result to preorder the tree for further evaluation
     * Not needed for MonteCarlo searches
     */
    // TODO: refactor static eval so GameOver is checked first.  Also probably remove alpha-beta vestiges
    public double Static_Evaluate_Position(	commonMove m)
    {	int playerindex = m.player;
    	if(board.GameOver())
    	{
    		if(board.WinForPlayer(playerindex))
    		{
    			 return(VALUE_OF_WIN*2-boardSearchLevel);
    		}
    		else if(board.WinForPlayer(nextPlayer[playerindex]))
    		{
    			return(-(VALUE_OF_WIN*2-boardSearchLevel));
    		}
    		else return 0;
    	}
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,nextPlayer[playerindex],false);

        return(val0-val1);
    }
    /**
     * this is called from UCT setup to get the evaluation, prior to ordering the UCT move lists.
     */
	public double Static_Evaluate_Uct_Move(commonMove mm,int current_depth,CommonDriver master)
	{
		throw G.Error("Not implemented");
	}

    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * Not needed for MonteCarlo searches
     * */
    public void StaticEval()
    {
            EpaminondasBoard evboard = GameBoard.cloneBoard();
            double val0 = ScoreForPlayer(evboard,FIRST_PLAYER_INDEX,true);
            double val1 = ScoreForPlayer(evboard,SECOND_PLAYER_INDEX,true);
            System.out.println("Eval is "+ val0 +" "+val1+ " = " + (val0-val1));
    }
    
    public commonMove DoAlphaBetaFullMove()
    {
           EpaminondasMovespec move = null;
           try
           {
          	
               // it's important that the robot randomize the first few moves a little bit.
               int randomn = RANDOMIZE ? ((board.moveNumber <= 6) ? (14 - 2*board.moveNumber) : 0) : 0;
               boardSearchLevel = 0;

               int depth = MAX_DEPTH;	// search depth
               double dif = 0.0;		// stop randomizing if the value drops this much
               // if the "dif" and "randomn" arguments to Find_Static_Best_Move
               // are both > 0, then alpha-beta will be disabled to avoid randomly
               // picking moves whose value is uncertain due to cutoffs.  This makes
               // the search MUCH slower so depth ought to be limited
               // if ((randomn>0)&&(dif>0.0)) { depth--; }
               // for games such as pushfight, where there are no "fools mate" type situations
               // the best solution is to use dif=0.0;  For games with fools mates,
               // set dif so the really bad choices will be avoided
               Search_Driver search_state = Setup_For_Search(depth, 0.25,depth-3);
               search_state.save_all_variations = SAVE_TREE;
               search_state.good_enough_to_quit = GOOD_ENOUGH_VALUE;
               search_state.verbose = verbose;
               search_state.allow_killer = KILLER;
               search_state.allow_best_killer = false;
               search_state.save_top_digest = true;	// always on as a background check
               search_state.save_digest=false;	// debugging only
               search_state.check_duplicate_digests = false; 	// debugging only

              if (move == null)
               {	// randomn takes the a random element among the first N
               	// to provide variability.  The second parameter is how
               	// large a drop in the expectation to accept.  For pushfight this
               	// doesn't really matter, but some games have disasterous
               	// opening moves that we wouldn't want to choose randomly
                   move = (EpaminondasMovespec) search_state.Find_Static_Best_Move(randomn,dif);
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
        GameBoard = (EpaminondasBoard) gboard;
        board = GameBoard.cloneBoard();
        // strategy with be 0,1,2 for Dumbot, Smartbot, Bestbot
        Strategy = strategy;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy "+strategy);
        case -100:	// old dumbot, before shift in pruning and randomization 
        	MONTEBOT = DEPLOY_MONTEBOT; break;
        case WEAKBOT_LEVEL:
        	WEAKBOT = true;
			//$FALL-THROUGH$
		case DUMBOT_LEVEL:
           	MONTEBOT=false;
           	MAX_DEPTH = DUMBOT_DEPTH;
           	useMoveStats = true;
         	break;
		case SMARTBOT_LEVEL:
			MONTEBOT=false;
			MAX_DEPTH = DUMBOT_DEPTH;
			useMoveStats = true;
			break;
		case BESTBOT_LEVEL:
			MONTEBOT=false;
			MAX_DEPTH = DUMBOT_DEPTH;
			useMoveStats = true;
			break;
	         	
        }
    }


 /**
  * breakpoint or otherwise override this method to intercept search events.
  * This is a low level way of getting control in the middle of a search for
  * debugging purposes.
  */
//public void Search_Break(String msg)
//{	super.Search_Break(msg);
//}
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
    board.initRobotValues(this);
    movingForPlayer = GameBoard.getCurrentPlayerChip();
}

	// in games where the robot auto-adds a done, this is needed so "save current variation" works correctly
	public commonMove getCurrentVariation()
	{	
		return getCurrent2PVariation();
	}

 }
