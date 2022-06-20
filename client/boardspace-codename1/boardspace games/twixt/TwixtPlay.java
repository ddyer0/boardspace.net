package twixt;


import bridge.BufferedReader;
import bridge.File;
import bridge.FileInputStream;
import bridge.FileOutputStream;

import static twixt.Twixtmovespec.MOVE_DROPB;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.game.sgf.sgf_game;
import online.game.sgf.export.sgf_names;
import online.game.sgf.sgf_node;
import online.game.sgf.export.sgf_names.Where;
import online.search.*;
import online.search.nn.*;




/** 
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


// replaced x1j move class, bridges to twixtmovespec
class Move
{
	int x;
	int y;
	public Move(int rx, int ry) {
		x=rx;
		y=ry;
	}
	public int getX() {	return(x); }
	public int getY() { return(y);}
	public char getColumn() { return((char)('A'+x)); }
	public int getRow() { return(y+1); }
}
class Board 
{
	/** playing East-West. */
	public static final int XPLAYER = 1;

	/** playing North-South. */
	public static final int YPLAYER = -1;

}
class Match
{  	TwixtBoard board;
	CellStack history;
	public Match(TwixtBoard b) 
	{ board = b; 
	  history = board.history;
	}
	
	public boolean pieRule() { return(true); }
	public int getMoveNr() {
		return(history.size());
	}

	public int getMoveX(int i) // get the move X, first column = 0
	{
		TwixtCell c = history.elementAt(i-1);
		return(c.col-'A');
	}
	public int getMoveY(int i)	// get the move Y, first row = 0
	{
		TwixtCell c = history.elementAt(i-1);
		return(c.row-1);
	}

	public int getYsize() {
		return(board.nrows);
	}

	public int getXsize() {
		return(board.ncols);
	}
	
}

/**
 * Compute the initial moves. (The first four pins on the board)
 * opening book logic imported from x1j.
 * @author Johannes Schwagereit (mail(at)johannes-schwagereit.de)
 */
final class InitialMoves
{
   private static final int FOUR = 4;
   private static final int FIVE = 5;
   private static final int SIX = 6;

   private Match match;
   private int currentPlayer;

   
   /**
    * Constructor - no external instance.
    */
   InitialMoves(TwixtBoard b)
   {	match = new Match(b);
   }
   private boolean testForSwap()
   {   if(match.board.swapped) { return(false); }
       int x = match.getMoveX(1);
       int y = match.getMoveY(1);
       boolean swapping =
             x > 2 
             && x < match.getXsize() - 3 
             && y > 2 
             && y < match.getYsize() - 3;
        return(swapping);
   }
   /**
    * Check for pie-rule and initial moves.
    *
    * @param matchIn the match
    * @param player player who has next turn
    * @return An initial move if it exists
    */
   public Move initialMove(int pplayer)	// player is 0 or 1, 
   {
	  Move retMove = null;
	  if(match.board.swapped) { pplayer=pplayer^1; }
	  switch(pplayer)
      {
      default: throw G.Error("Not expecting %s",pplayer);
      case 1: currentPlayer = Board.XPLAYER; break;
      case 0: currentPlayer = Board.YPLAYER; break;
      }

	  int moven = match.getMoveNr();
	  if (moven == 0)
      {
         retMove = firstMove();
      }
      else if (moven <= 3) // simple answer with predefined moves
      {	 if((moven==1) && testForSwap())
      		{
    	  	return(new Move(-1,-1));
      		}
         retMove = secondToFourthMove();
      }
      else if (moven <= 5)
      {
         retMove = fifthOrMoreMove();
      }
      return (retMove != null && retMove.getX() >= 0) ? retMove : null;
   }

   /**
    * Compute fifth (or more) move. This is a defensive move.
    * @return a move if possible
    */
   private Move fifthOrMoreMove()
   {
      Move retMove = null;
      int mostFar = 0;
      int distance;
      int bx;
      int by;
      //check all opponent pins
      // find conter-move which is not near other pins
      for (int i = match.getMoveNr(); i >= 1; i-=2)
      {
         bx = match.getMoveX(i);
         by = match.getMoveY(i);
         Move possMove = getCounterPin(bx,by);
         distance = nearestPin(possMove, i);
         if (distance > 3 && distance > mostFar)
         {
            mostFar = distance;
            retMove = possMove;
         }

      }
      return retMove;
   }

   /**
    * Calculate smallest distance to any other pin.
    * @param someMove a move
    * @param except movenumber to exclude from computation
    * @return distance to next pin
    */
   private int nearestPin(Move someMove, int except)
   {
      int dist = Integer.MAX_VALUE;
      int cDist;
      if (someMove == null)
      {
         return -1;
      }
      //check for smallest dist to any other pin
      for (int i = match.getMoveNr(); i >= 1; i--)
      {
         cDist = Math.abs(match.getMoveX(i) - someMove.getX())
               + Math.abs(match.getMoveY(i) - someMove.getY());
         if (except == match.getMoveNr())
         {
            //little bonus for last move
            cDist--;
         }
         if (i != except && cDist < dist)
         {
            dist = cDist;
         }
      }
      return dist;
   }

   /**
    * Try to find a move which blocks oppenent pin.
    * @param bx x of pin to block
    * @param by y of pin to block
    * @return a possible Move
    */
   private Move getCounterPin(final int bx, final int by)
   {
      int rx, ry;
      // ignore if too near to boarder
      if (bx < 4 || by < 4  || by > match.getYsize() - 4 || bx > match.getXsize() - 4)
      {
         return null;
      }
      if (currentPlayer == Board.XPLAYER)
      {
         ry = (by < match.getYsize() / 2) ? by + 4 : by - 4;
         rx = bx;
         if (bx < match.getXsize() / 2 - 3)
         {
            rx = bx + 1;
         }
         else if (bx > match.getXsize() / 2 + 3)
         {
            rx = bx - 1;
         }
      }
      else
      {
         rx = (bx < match.getXsize() / 2) ? bx + 4 : bx - 4;
         ry = by;
         if (by < match.getYsize() / 2 - 3)
         {
            ry = by + 1;
         }
         else if (by > match.getYsize() / 2 + 3)
         {
            ry = by - 1;
         }
      }
      return new Move(rx, ry);
   }

   /**
    * Compute second, third and fourth move.
    * @return a move if possible
    */
   private Move secondToFourthMove()
   {
      Random rand = new Random();
      int bx;
      int by;
      // find previous pin
      bx = match.getMoveX(match.getMoveNr());
      by = match.getMoveY(match.getMoveNr());
      if (currentPlayer == Board.XPLAYER)
      {
         if (by < match.getYsize() / 2)
         {
            by = ((match.getYsize() - by) / 2 + by);
         }
         else
         {
            by /= 2;
         }
         // some random
         by = by - 1 + rand.nextInt(3);
         if (bx <= SIX)
         {
            bx = bx + 2 + rand.nextInt(2);
            if (bx <= FOUR)
            {
               bx++;
            }
         }
         if (bx >= match.getXsize() - SIX - 1)
         {
            bx = bx - 2 - rand.nextInt(2);
            if (bx >= match.getYsize() - FOUR - 1)
            {
               bx--;
            }
         }
      }
      else
      {
         if (bx <= match.getXsize() / 2)
         {
            bx = ((match.getXsize() - bx) / 2 + bx);
         }
         else
         {
            bx /= 2;
         }
         // some random
         bx = bx - 1 + rand.nextInt(3);
         if (by <= SIX)
         {
            by = by + 2 + rand.nextInt(2);
            if (by <= FOUR)
            {
               by++;
            }
         }
         if (by >= match.getYsize() - SIX - 1)
         {
            by = by - 2 - rand.nextInt(2);
            if (by >= match.getYsize() - FOUR - 1)
            {
               by--;
            }
         }
      }
      if (match.getMoveNr() == 2 || match.getMoveNr() == 3)
      {
         // check if 3rd move is really different from 1st
         if (Math.abs(bx - match.getMoveX(match.getMoveNr() - 1)) <= 2
               && Math.abs(by - match.getMoveY(match.getMoveNr() - 1)) <= 2)
         {
            // in this unlikely case an Ares (5/0) will improve my situation
            bx = match.getMoveX(match.getMoveNr() - 1);
            by = match.getMoveY(match.getMoveNr() - 1);
            if (currentPlayer == Board.XPLAYER)
            {
               bx += (bx >= match.getXsize() / 2) ? -FIVE : FIVE;
            }
            else
            {
               by += (by >= match.getYsize() / 2) ? -FIVE : FIVE;
            }
         }
         if (match.getMoveNr() == 3
               && Math.abs(bx - match.getMoveX(match.getMoveNr() - 2)) <= 2
               && Math.abs(by - match.getMoveY(match.getMoveNr() - 2)) <= 2)
         {
            bx = -1;
            by = -1; // should be extremely seldom - take normal mechanism
         }
      }
      return new Move(bx, by);
   }

   /**
    * Compute the first pin.
    * @return Pin
    */
   private Move firstMove()
   {
      Random rand = new Random();

      int[][] startPos = {{2, 10}, {3, 4}, {3, FIVE}, {3, SIX}, {4, 3}, {FIVE, 3}, {SIX, 3}};
      //int[][] startPos = { { 4, 11 }, { 4, 10 }, { FIVE, 10 }, { FIVE, 9 }, { SIX, 8 },
      //      { 6, 6 }, { 5, 6 }, { 6, 5 }, { 9, SIX }, { 4, TWELVE } };

      int bx;
      int by;
      if (match.pieRule()) // first move with pie-rule
      {
         // previous fixed pin for other board than 24x24
         //bx = match.getXsize() / 6;
         //by = match.getYsize() / 6;

         // for any board-size
         int p = rand.nextInt(startPos.length);
         bx = startPos[p][0] - 1;
         by = startPos[p][1] - 1;

         if (currentPlayer == Board.XPLAYER)
         { // swap
            int k = bx;
            bx = by;
            by = k;
         }
         // mirror coordinates
         if (rand.nextBoolean())
         {
            bx = match.getXsize() - bx - 1;
         }
         if (rand.nextBoolean())
         {
            by = match.getYsize() - by - 1;
         }
         // std::cout << (int) p << " " << (int) bx << "-" << (int) by <<
         // "\n";
      }
      else
      {
         // firstmove is simple without pie-rule
         // some random elements to make playing witout pierule more variable
         int var = match.getXsize() / 4;
         bx = match.getXsize() / 2 + rand.nextInt(var) - var / 2;
         var = match.getYsize() / 4;
         by = match.getYsize() / 2 + rand.nextInt(var) - var / 2;
      }
      return new Move(bx, by);
   }

}

public class TwixtPlay extends commonRobot<TwixtBoard> implements Runnable, TwixtConstants,
    RobotProtocol
    {
	// this is an internal value used to affect the search in several ways.  Normal "value of position" results
	// should be well below this in magnitude.  Searches are normally called off if the value of a position exceeds
	// this, indicating "we won".   It should be at least 2x any non-winning value the evaluator normally produces.
	// but it's exact value and scale are unimportant.  The main thing is to have a convenient range of values
	// for the evaluator to work with.
    static final double VALUE_OF_WIN = 10000.0;
    boolean UCT_WIN_LOSS = false;
    
    boolean EXP_MONTEBOT = false;
    double ALPHA = 1.0;
    double BETA = 0.25;
    double NODE_EXPANSION_RATE = 1.0;
    double CHILD_SHARE = 0.5;				// aggressiveness of pruning "hopeless" children. 0.5 is normal 1.0 is very agressive
    boolean SAVE_TREE = false;				// debug flag for the search driver.  Uses lots of memory. Set a breakpoint after the search.
    int MAX_DEPTH = 5;						// search depth.
    int FIRST_DEPTH = 4;
    int MAX_WIDTH = 999;
    double MAX_TIME = 0.5;
    
    // monte carlo parameters
    int timePerMove = 10;
    int minMovesPerSecond = 200000;
    int maxMovesPerSecond = 1085000;
    int storedChildLimit = UCTMoveSearcher.defaultStoredChildLimit;
    double winRateWeight = 0;
    boolean deadChildOptimization = true;
    
	static final boolean KILLER = false;	// if true, allow the killer heuristic in the search
	static final double GOOD_ENOUGH_VALUE = VALUE_OF_WIN;	// good enough to stop looking
	
	boolean STORED_CHILD_LIMIT_STOP = false;	// if true, stop the search when the child pool is exhausted.

    int Strategy = DUMBOT_LEVEL;
    InitialMoves initialMoves = null;
    CommonMoveStack plausibleMoves = null;
    
    int boardSearchLevel = 0;				// the current search depth
    /**
     *  Constructor, strategy corresponds to the robot skill level displayed in the lobby.
     * 
     *  */

    public TwixtPlay()
    {
    }

    public int Width_Limit_Moves(int depth,
            int max,
            commonMove[] mvec,
            int sz)
    {	return(Math.min(sz,MAX_WIDTH));
    }
    public RobotProtocol copyPlayer(String from)	// from is the thread name
    {	RobotProtocol c = super.copyPlayer(from);
    	TwixtPlay cc = (TwixtPlay)c;
    	cc.Strategy = Strategy;
    	cc.evalNet = evalNet==null ? null : evalNet.duplicate();
    	return(c);
    }

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
 */
    public void Unmake_Move(commonMove m)
    {	Twixtmovespec mm = (Twixtmovespec)m;
        board.UnExecute(mm);
        boardSearchLevel--;
    }
/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   Twixtmovespec mm = (Twixtmovespec)m;
        board.RobotExecute(mm);
        boardSearchLevel++;
    }

/** return a Vector of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {	if(plausibleMoves!=null)
    {
    		return(board.getListOfPlausibleMoves(board.whoseTurn,plausibleMoves,boardSearchLevel<2));
    	}
    	switch(Strategy)
    	{
    	case SMARTBOT_LEVEL:
		case WEAKBOT_LEVEL:
    	case DUMBOT_LEVEL:
    		if(boardSearchLevel>=2) 
    			{ return(board.getListOfPlausibleMoves(board.whoseTurn,plausibleMoves,boardSearchLevel<2));
    			}
			//$FALL-THROUGH$
    	default:
        return(board.GetListOfMoves());
    }

    }
 
    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    double ScoreForPlayer(TwixtBoard evboard,int player,boolean print)
    {	double val = 0;
    	if(evboard.WinForPlayer(player))
    {	
    		val = VALUE_OF_WIN;
    	}
    	else
    	{
    	switch(Strategy)
    	{
    	default: throw G.Error("not expecting strategy %s",Strategy);
    	case DUMBOT_LEVEL: 
    	case WEAKBOT_LEVEL:
    	case NEUROBOT_LEVEL:
    	case PALABOT_LEVEL:
    	case SMARTBOT_LEVEL:
    		val = evboard.scoreForPlayer1(player, print)-boardSearchLevel;
			break;
    	}}
     	return(val);
    }

    /**
     * this re-evaluates the current position from the viewpoint of forplayer.
     * for 2 player games this is to trivially negate the value, but for multiplayer
     * games it requires considering multiple player's values.
     */
    public double reScorePosition(commonMove m,int forplayer)
    {	return(m.reScorePosition(forplayer,VALUE_OF_WIN));
    }
    /** this is called from the search driver to evaluate a particular position. The driver
     * calls List_of_Legal_Moves, then calls Make_Move/Static_Evaluate_Position/UnMake_Move
     *  for each and sorts the result to preorder the tree for further evaluation
     */
    public double Static_Evaluate_Position(	commonMove m)
    {	int playerindex = m.player;
    	switch(Strategy)
    	{
    	case BESTBOT_LEVEL:	
    		return(board.Static_Evaluate_Position2(playerindex,false));
    	default:
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,nextPlayer[playerindex],false);
        // don't dilute the value of wins with the opponent's positional score.
        // this avoids the various problems such as the robot comitting suicide
        // because it's going to lose anyway, and the position looks better than
        // if the oppoenent makes the last move.  Technically, this isn't needed
        // for twixt because there is no such thing as a suicide move, but the logic
        // is included here because this is supposed to be an example.
        if(val0>=VALUE_OF_WIN) { return(val0); }
        if(val1>=VALUE_OF_WIN) { return(-val1); }
        return(val0-val1);
     		
    	}
    }
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {	TwixtBoard evboard = GameBoard.cloneBoard();
    	switch(Strategy)
    {
    	case BESTBOT_LEVEL:
    		evboard.Static_Evaluate_Position2(0,true);
    		break;
    	default:
            double val0 = ScoreForPlayer(evboard,FIRST_PLAYER_INDEX,true);
            double val1 = ScoreForPlayer(evboard,SECOND_PLAYER_INDEX,true);
            System.out.println("Eval is "+ val0 +" "+val1+ " = " + (val0-val1));
    }
    }

    private GenericNetwork createF8PCB()
    {	int ncells = board.nCells();
    	return new GenericNetwork(
    				"NAME F8PCB"
    				+ " LEARNING_RATE 0.0005"
   				// f8p with an extra input layer for the last 2 moves
    				+ " LAYER 0 (TYPE I ID IN )"
    				+ " LAYER 1 (TYPE I ID IN2 )"
    				
    				// these two layers get context free information so they can
    				// learn board features
    				+ " LAYER 2 (TO IN TYPE FILTER)"
    				+ " LAYER 3 (TO 2 TYPE FILTER)"
    				+ " LAYER 4 (TO 3 TYPE FILTER)"
    				+ " LAYER 5 (TO 4 TYPE POOL ID POOL1 )"
    				
    				+ " LAYER 6 (TO IN TYPE FILTER)"
    				+ " LAYER 7 (TO 6 TYPE FILTER)"								
    				+ " LAYER 8 (TO 7 TYPE FILTER)"
    				+ " LAYER 9 (TO 8 TYPE POOL ID POOL2)"
    				
    				// these 2 layers also include "last move" information
    				// so they can learn response to local activity
    				+ " LAYER 10 (TO IN TYPE FILTER)"
    				+ " LAYER 11 (TO 10 TYPE FILTER)"								
    				+ " LAYER 12 (TO 11 TYPE FILTER)"
    				+ " LAYER 13 (TO 12 TYPE POOL ID POOL3)"
    				
    				+ " LAYER 14 (TO IN TYPE FILTER)"
    				+ " LAYER 15 (TO 14 TYPE FILTER)"								
    				+ " LAYER 16 (TO 15 TYPE FILTER)"
    				+ " LAYER 17 (TO 16 TYPE POOL ID POOL4)"
    																				
    				
    				+ " LAYER 18 ( TYPE O ID OUT TO IN2 TO POOL1 TO POOL2 TO POOL3 TO POOL4)"
     				
    				+ " COORDINATES SQUARE "+board.ncols+" "+board.nrows
    				+ " TRANSFER_FUNCTION SIGMOID"
    				,
    				ncells,			// 0 input layer
    				ncells,			// 1 extra input layer
    				ncells,ncells,ncells,ncells,	// 2 3 4 5 filter layers
    				ncells,ncells,ncells,ncells,	// 6 7 8 9 filter layers
    				ncells,ncells,ncells,ncells,	// 10 11 12 13 filter layers with extra input
    				ncells,ncells,ncells,ncells,	// 14 15 16 17 filter layers with extra input
     			ncells+1				// 18 fully connected layer 	
    				);	// learn with all sigmoid nodes
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
            GameBoard = (TwixtBoard) gboard;
        board = GameBoard.cloneBoard();
            // strategy with be 0,1,2 for Dumbot, Smartbot, Bestbot
            Strategy = strategy;
        terminalNodeOptimize = true;
            switch(strategy)
            {
            default: throw G.Error("Not expecting strategy %s",strategy);
            case -100:	// old dumbot, before shift in pruning and randomization 
        	MONTEBOT = DEPLOY_MONTEBOT;
        	break;
        	
        case BESTBOT_LEVEL:	// experimental bot that chooses first moves selectively
        case SMARTBOT_LEVEL:
        case DUMBOT_LEVEL:	// we're using this as a reference robot, 4 ply full width
        	MAX_DEPTH = 8;
         	FIRST_DEPTH = 4;
         	break;
         	
        case PALABOT_LEVEL:

            	MAX_DEPTH = 4;
         	FIRST_DEPTH = 3;
            	break;
        case WEAKBOT_LEVEL:
        	MAX_DEPTH = 3;
         	FIRST_DEPTH = 2;
         	MAX_TIME = 0.25;
            	WEAKBOT = true;
            	break;
            	
        case MONTEBOT_LEVEL: ALPHA = .25; MONTEBOT=true; EXP_MONTEBOT = true;
        	break;
        
        case TESTBOT_LEVEL_1:
        	// bot vs bot testing
        	
        case TESTBOT_LEVEL_2:
        	// bot vs bot testing
        	
        case NEUROBOT_LEVEL:
        	// robot to produce test data for neural net learning
        	MONTEBOT = true;
			name = "Neurobot";
			timePerMove = 5;	// after partial training, 10, before 5
			evalNet = teachNet;
			if(evalNet ==null)
			{
				try {
					evalNet =
							GenericNetwork.loadNetwork(
							"g:/temp/twixt/f8pcb-upto-f8pcb-15.txt"	// reduced learning rate
							);

					}
					catch (ErrorX x)
					{}
					if(evalNet==null)
						{	
						evalNet = 
								createF8PCB();	// 4 filter layers, lastmove only at output
								;			
						}
					}
				((GenericNetwork)evalNet).testNetwork();
				teachNet = evalNet;
				minMovesPerSecond = 100;	// train at a constant playout rate
				maxMovesPerSecond = 100;
				MONTEBOT=true;
	        	verbose = 1;
	        	deadChildOptimization = false;
	        	storedChildLimit = 2*UCTMoveSearcher.defaultStoredChildLimit;
	        	winRateWeight = 0;
	        	ALPHA = 0.5;
	        	BETA = 0.25;
	        	CHILD_SHARE = 0.85;
				NEUROBOT = true;
			break;
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
	//use this for a friendly robot that shares the board class
	board.copyFrom(GameBoard);
    board.sameboard(GameBoard);	// check that we got a good copy.  Not expensive to do this once per move
    board.robotBoard = true;
	initialMoves = new InitialMoves(board);
 }

public commonMove Get_Random_Move(Random rand)
{	
	switch(Strategy)
	{
	case TESTBOT_LEVEL_1:
	case TESTBOT_LEVEL_2:
	case NEUROBOT_LEVEL:
		return getUCTRandomMove(rand);
	case DUMBOT_LEVEL:
	default:
		commonMove ran = board.Get_Random_Move(rand);
	if(ran==null) { ran = super.Get_Random_Move(rand); }
	return(ran);
		
	}
}
 public commonMove DoAlphaBetaFullMove()
 {
        Twixtmovespec move = null;
        try
        {
       	
            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                return(new Twixtmovespec("Done", board.whoseTurn));
            }
            
            Move in = initialMoves.initialMove(board.whoseTurn);
            if(in!=null)
            { char col = in.getColumn();
              int row = in.getRow();
              if(row<=0)
              	{ return(new Twixtmovespec(MOVE_SWAP,board.whoseTurn));
              	}
              else {
            	  return(new Twixtmovespec(MOVE_DROPB,col,row,board.whoseTurn));
              	}
            }

            switch(Strategy)
            {
            case BESTBOT_LEVEL:
            	plausibleMoves = board.buildPlausibleMoveSet(board.whoseTurn);
            	break;
            default:
            	break;
            }

            // it's important that the robot randomize the first few moves a little bit.
            int randomn = RANDOMIZE ? ((board.moveNumber <= 6) ? (14 - 2*board.moveNumber) : 0) : 0;
            boardSearchLevel = 0;

            int depth = MAX_DEPTH;	// search depth
            int firstDepth = FIRST_DEPTH;
            double dif = 0.0;		// stop randomizing if the value drops this much
            // if the "dif" and "randomn" arguments to Find_Static_Best_Move
            // are both > 0, then alpha-beta will be disabled to avoid randomly
            // picking moves whose value is uncertain due to cutoffs.  This makes
            // the search MUCH slower so depth ought to be limited
            // if ((randomn>0)&&(dif>0.0)) { depth--; }
            // for games such as twixt, where there are no "fools mate" type situations
            // the best solution is to use dif=0.0;  For games with fools mates,
            // set dif so the really bad choices will be avoided
            Search_Driver search_state = Setup_For_Search(depth, MAX_TIME,firstDepth);
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
            	// large a drop in the expectation to accept.  For twixt this
            	// doesn't really matter, but some games have disasterous
            	// opening moves that we wouldn't want to choose randomly
                move = (Twixtmovespec) search_state.Find_Static_Best_Move(randomn,dif);
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


	public commonMove getCurrentVariation()
	{	
		return getCurrent2PVariation();
	}
 
 // this is the monte carlo robot, which for twixt is much better then the alpha-beta robot
 // for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
 // evaluator other than winning a game.
 public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
 	UCT_WIN_LOSS = EXP_MONTEBOT;
 	try {
       if (board.DoneState())
        { // avoid problems with gameover by just supplying a done
            move = new Twixtmovespec("Done", board.whoseTurn);
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
        //VERBOSE=0;
        // it's important that the robot randomize the first few moves a little bit.
        double randomn = (RANDOMIZE && (board.moveNumber <= 4))
        						? 0.1/board.moveNumber
        						: 0.0;
        monte_search_state = new UCTMoveSearcher(this);
        monte_search_state.save_top_digest = true;	// always on as a background check
        monte_search_state.save_digest=false;	// debugging only
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.timePerMove = timePerMove;		// 20 seconds per move
        monte_search_state.stored_child_limit = storedChildLimit;
        monte_search_state.verbose = verbose;
        monte_search_state.alpha = ALPHA;
        monte_search_state.blitz = false;			// for twixt, blitz is 2/3 the speed of normal unwinds
        monte_search_state.sort_moves = winRateWeight>0;
        monte_search_state.initialWinRateWeight = winRateWeight;

        monte_search_state.only_child_optimization = true;
        monte_search_state.dead_child_optimization = deadChildOptimization;

        monte_search_state.simulationsPerNode = 1;
        monte_search_state.killHopelessChildrenShare = CHILD_SHARE;
        monte_search_state.final_depth = 100;
        monte_search_state.node_expansion_rate = NODE_EXPANSION_RATE;
        monte_search_state.randomize_uct_children = true;     
        monte_search_state.maxThreads = DEPLOY_THREADS;
        monte_search_state.random_moves_per_second = minMovesPerSecond;		// 
        monte_search_state.max_random_moves_per_second = maxMovesPerSecond;		// 
        // for some games, the child pool is exhausted very quickly, but the results
        // still get better the longer you search.  Other games may work better
        // the other way.
        monte_search_state.stored_child_limit_stop = STORED_CHILD_LIMIT_STOP;
        monte_search_state.terminalNodeOptimization = terminalNodeOptimize;
        move = monte_search_state.getBestMonteMove();

        }
       	printTrainingData(trainingData,board,monte_search_state);
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
 	boolean win = board.winForPlayerNow(player);
 	if(win) { return(UCT_WIN_LOSS? 1.0 : 0.8+0.2/boardSearchLevel); }
 	boolean win2 = board.winForPlayerNow(nextPlayer[player]);
 	if(win2) { return(- (UCT_WIN_LOSS?1.0:(0.8+0.2/boardSearchLevel))); }
 	return(0);
 }

  
  // support for neural net development
	UCTMoveSearcher monte_search_state = null;
	static Network teachNet = null; 
	static double[]lastNetworkValues = null;
	Network evalNet = null;
	boolean NEUROBOT = false;
	static int BlackValue = -1;
	static int RedValue = 1;
	
    private int getMoveIndex(Network n,char col,int row,boolean swap)
    {	CoordinateMap map = n.getCoordinateMap();
    	
    	if(swap)
    	{
     	int newway = map.getIndex((char)('A'+row-1),col-'A'+1);
    	return(newway);
 }
    	else
    	{
    	int newway = map.getIndex(col,row);
     	return(newway);
    	}
    }

    public void setInd(double []in,int ind,PieceColor piece,boolean swap)
    {	
    	switch(piece)
    	{
    		case Red: in[ind] = swap ? BlackValue : RedValue; break;
    		case Black: in[ind] = swap ? RedValue : BlackValue; break;
    		default: G.Error("opps, bad color %s",piece);
    		}

    }
    public double[] netCalculate(Network n,TwixtBoard b)
    {	int size = n.inputNetworkSize();
    	boolean swap =b.getPlayerColor(b.whoseTurn)==PieceColor.Black;
    	double in[] = new double[size];

    	{
        	for(TwixtCell c = b.allCells; c!=null; c=c.next)
        	{ TwixtChip chip = c.topChip();
        	  if(chip!=null) { 
            	  int idx = getMoveIndex(n,c.col,c.row,swap);
        		  setInd(in,idx,chip.color(),swap);
        	  }
        	}
    	}

     	return(n.calculateValues(0,in));
    }

    public commonMove getUCTRandomMove(Random rand)
	 {	
	 	netCalculate(evalNet,board);
	 	double values[] = evalNet.getValues();
	 	double vtotal = evalNet.getTotalValues();
	 	int who = board.whoseTurn;
	 	PieceColor color = board.getPlayerColor(who);
	 	boolean swap = color==PieceColor.Black;
	 	double rval = rand.nextDouble();	// the random probability we want to select
	 	double target = vtotal * rval;		// the expected sum of values that will make the target
	 	
	 	TwixtCell selected = null;
	 	double selectedTotal = 0;
	 	int selectedIndex = 0;
	 	double total = 0;
	 	cell<TwixtCell> allCells[] = (cell<TwixtCell>[])board.getCellArray();
	 	int sz = allCells.length;
	 	for(int lim = allCells.length,idx = 0; idx<lim; idx++)
	 	{	TwixtCell c = (TwixtCell)allCells[idx];
	 		if(c.isEmpty() && c.isElgible(color))
	 		{
	 		int ind = getMoveIndex(evalNet,c.col,c.row,swap);
	 		if(total<target) 
 			{ selected = c; 
 			  selectedIndex = idx;
 			  selectedTotal = total;
 			}
	 		total += values[ind];
	 		}
	 	}
	 	
		//
	 	// depending on the actual network, we may have overshot or undershot the value
	 	target = rval*total;
	 	if(selectedTotal>target)
	 	{	// back up
	 		do {
	 			selectedIndex--;
	 			TwixtCell c = (TwixtCell)allCells[selectedIndex];
	 			if(c.isEmpty())
	 	{	
	 			int ind = getMoveIndex(evalNet,c.col,c.row,swap);
	 			selectedTotal -= values[ind];
	 			selected = c;
	 			}
	 		}
	 		while(selectedTotal>target && selectedIndex>=0);
	 	}
	 	else if(selectedTotal<target)
	 	{	// continue on
	 		do {
	 			TwixtCell c = (TwixtCell)allCells[selectedIndex];
	 		if(c.isEmpty())
	 		{
	 		int ind = getMoveIndex(evalNet,c.col,c.row,swap);
	 			selectedTotal += values[ind];
	 			selected = c;
	  			selectedIndex++;
	 			}
	 		} while(selectedTotal<target && selectedIndex<=sz);
	 		if((selectedTotal<target) && (board.getState()==TwixtState.PlayOrSwap) )
	 		{
	 			return(new Twixtmovespec(SWAP,who));
	 		}
	 	}

	 	if(selected==null)
	 	{
	 	return(new Twixtmovespec(MOVE_RESIGN,who));	
	 	}
	 	else 
	 	{
	 	return(new Twixtmovespec(MOVE_DROPB,selected.col,selected.row,who));
	 	}
	 }
    //
    // training data gathered during self play
    public void printTrainingData(StringBuilder trainingData,TwixtBoard b,UCTMoveSearcher monte)
    {	if(trainingData!=null && monte!=null && monte.root!=null)
    	{
		printInputVector(trainingData,b);
		printTrainingVector(trainingData,monte);
    	}
	
    }
    public void printInputVector(StringBuilder trainingData,TwixtBoard b)
    {
    	trainingData.append(b.whoseTurn);
    	trainingData.append(" ");
    	trainingData.append(b.getPlayerColor(b.whoseTurn).shortName);
    	CellStack history = b.history;
    	int sz = history.size();
    	for(int idx = Math.max(0, sz-2);idx<sz;idx++)
    	{
    		TwixtCell c = history.elementAt(idx);
    		TwixtChip top = c.topChip();
    		G.Assert(top!=null,"should be an occupied cell");
    		trainingData.append(" ");
    		trainingData.append(c.col);
    		trainingData.append(" ");
    		trainingData.append(c.row);
    		trainingData.append(" ");
    		trainingData.append(top.color().shortName);
    	}
    	trainingData.append("\n");
    	
    	for(TwixtCell c = b.allCells; c!=null; c=c.next)
    		{
    			TwixtChip top = c.topChip();
    			if(top!=null) 
    			{ trainingData.append(" ");
    			  trainingData.append(c.col);
    			  trainingData.append(" ");
    			  trainingData.append(c.row);
    			  trainingData.append(" ");
    			  trainingData.append(top.color().shortName); 
    			}
    		}
    		trainingData.append("\n");
    	
    }
    public void printTrainingVector(StringBuilder trainingData,UCTMoveSearcher monte)
    {
		UCTNode root = monte.root;
		int nChildren = root.getNoOfChildren();
		double rootvisits = root.getVisits();
		for(int lim=nChildren-1; lim>=0; lim--)
		{
			Twixtmovespec mm = (Twixtmovespec)root.getChild(lim);
			UCTNode no = mm.uctNode();
			if(no!=null)
			{
			double visits = no.getVisits()/rootvisits;
			trainingData.append(" ");
			if(mm.op==MOVE_SWAP) { trainingData.append(SWAP); }
			else { 	trainingData.append(mm.to_col);
					trainingData.append(" ");
					trainingData.append(mm.to_row);
			}
			trainingData.append(" ");
			trainingData.append(visits);
			}
		}

		trainingData.append("\n");
}
    public BoardProtocol monitor = null;
    public BoardProtocol disB()
    {	if(monitor!=null) 
    		{ return(monitor); }
    	return(super.disB());
    }
	
	static int rep = 0;
	boolean recordPermutations = true;
	static BSDate startTime = new BSDate();
	static String TrainingData = "TrainingData";	// property name
	StringBuilder trainingData = null;
	public void runGame_playself(ViewerProtocol viewer)
	 {	
		int wins[] = new int[2];
	 	beingMonitored = true;
	 	monitor = GameBoard;
	 	
		while(beingMonitored())
		{
		boolean firstPlayer = ((rep&1)==0);
		RepeatedPositions positions = new RepeatedPositions();
		commonMove start = viewer.ParseNewMove("start p0", 0);
		GameBoard.doInit();
		GameBoard.Execute(start,replayMode.Replay);
		trainingData = new StringBuilder();
 		
	
	 	while(!GameBoard.GameOver() && beingMonitored())
	 	{	int who = GameBoard.whoseTurn();
	 		PrepareToMove(who);
	 		commonMove m = DoFullMove();
	 		if(m!=null) 
	 			{GameBoard.Execute(m,replayMode.Replay); 
	 			positions.checkForRepetition(GameBoard,m);
	 			}
	 	}
	 	wins[firstPlayer?0:1] += GameBoard.WinForPlayer(0)?1:0;
	 	wins[firstPlayer?1:0] += GameBoard.WinForPlayer(1)?1:0;
	 	rep++;
	 	if(beingMonitored() && trainingData!=null)
	 		{
	 		saveTrainingData(
	 				trainingData,
	 				"g:/temp/twixt/train/twixt-"+startTime.DateString()+"-"+rep+".txt",
	 				"training initial training set"
	 				);
	 		}
	 	}
	 }
	public void saveTrainingData(StringBuilder trainingData,String file,String comment) 
	{
		if(trainingData!=null)
		{
		OutputStream s=null;
		try {
			s = new FileOutputStream(new File(file));
			if(s!=null)
				{PrintStream ps = new PrintStream(s);
				ps.println("//VERSION 1");
				ps.println("//"+comment);
				ps.print(trainingData.toString());
				ps.close();
				s.close();
				}
		} catch (IOException e) {
			G.Error("Save training data error",e);
		}}
	}


	 // play neurobot against itself, train the
	 // monte carlo priors based on each search
	 public void runRobotGameSelf(final ViewerProtocol v,BoardProtocol b,SimpleRobotProtocol otherBot)
	 {	
	 	BoardProtocol cloneBoard = b.cloneBoard();

	 	InitRobot(v,v.getSharedInfo(),cloneBoard,null,NEUROBOT_LEVEL);
	 	StopRobot();
	 	new Thread(new Runnable() {
	 		public void run() { runGame_playself(v); }
	 	}).start();
	 
	 		}
	 
	 public double trainNetworkI(GenericNetwork n,double inputs[],double inputs2[],double values[],boolean rmsonly)
	 {	double oldvals[] = n.calculateValues(n.getLayer("IN"),inputs,n.getLayer("IN2"),inputs2);
	 	double setvals[] = new double[oldvals.length];
	 	double rms = 0;
	 	double rmsn = 0;
	 	for(int lim=values.length-1; lim>=0; lim--)
	 	{	double v = values[lim];
	 		if(v!=-1)
	 			{ double dif = oldvals[lim]-v;
	 			  setvals[lim]=v;
	 			  rms += dif*dif;
	 			  rmsn++;
	 			}
	 		else { 
	 			double dif = oldvals[lim];
	 			setvals[lim]=0; 
	 			rms += dif*dif;
	 			rmsn++;
	 			}
	 	}
	 	if(rmsn>0)
	 	{
	 	//((GenericNetwork)n).learningRate=0.001;
	 	if(!rmsonly) { n.learn(setvals); }
	 	//double []newvals = n.calculateValues(inputs);
	 	lastNetworkValues = null;//setvals;
	 	return(Math.sqrt(rms/rmsn));
	 	}
	 	return(0);
	 }
	 public void setInd(double []in,int ind,char piece,boolean swap)
	 {	
	 	switch(piece)
	 	{
	 		case 'r':
	 		case 'R': in[ind] = swap ? BlackValue : RedValue; break;
	 		case 'b':
	 		case 'B': in[ind] = swap ? RedValue : BlackValue; break;
	 		default: G.Error("opps, bad color %s",piece);
	 		}

	 }

	// parse the inputs of a training data entry
	 public boolean parseInputs(TrainingData td,GenericNetwork n,String str,String str2,int permutation,boolean canSwap)
	 {	int size = n.inputNetworkSize();
	 	double in[] = new double[size];
	 	boolean has[] = new boolean[size];
	 	Layer in2Layer = n.getLayer("IN2");
	 	double in2[] = (in2Layer!=null) ? new double[size] : null;
	 	boolean swapColor = false;
	 	StringTokenizer tok = new StringTokenizer(str);
	 	td.playerToMove = G.IntToken(tok);	// the player, we don't need it.
	 	char color = G.CharToken(tok);
	 	td.color = color;
	 	// normalize colors
	 	if(canSwap)
	 	{
	 	switch(color)
	 	{
	 	case 'b':
	 	case 'B': swapColor = true;
	 		break;
	 	case 'r':
	 	case 'R': swapColor = false;
	 		break;
	 	default: G.Error("oops, bad color %s",color);
	 	}}
	 	readRest(n,tok,in2,has,permutation,swapColor);
	 	StringTokenizer tok2 = new StringTokenizer(str2);
	 	readRest(n,tok2,in,has,permutation,swapColor);
	 	td.hasInputs = has;
	 	td.inputs = in;
	 	td.inputs2 = in2;
	 	
	 	return(swapColor);
	 }
	 public void readRest(Network n,StringTokenizer tok,double[]in,boolean has[],int permutation,boolean swapColor)
	 {
		 	int ncols = board.ncols;
		 	while(tok.hasMoreTokens())
		 	{
		 		char col = G.CharToken(tok);
		 		int row = G.IntToken(tok);
		 		if((permutation&1)!=0) { row = ncols+1-row; }
		 		if((permutation&2)!=0) { col = (char)(('A'+ncols-1)-col+'A'); }
		 		char piece = G.CharToken(tok);
		 		int ind = getMoveIndex(n,col,row,swapColor);
		 		setInd(in,ind,piece,swapColor);
		 		has[ind] = true;	 		
		 	}
	 }
	 // parse the values part of a training data entry
	 public void parseValues(TrainingData td,GenericNetwork n,String str,boolean swap,int permutation)
	 {	double values[] = n.getValues();
	 	StringTokenizer tok = new StringTokenizer(str);
	 	int ncols = board.ncols;
	 	AR.setValue(values, -1);
	 	int lastIndex = 0;
	 	int nvalues=0;
	 	while(tok.hasMoreTokens())
	 	{
	 		String column = tok.nextToken();
	 		if(SWAP.equalsIgnoreCase(column))
	 		{	
	 			double v = Math.abs(G.DoubleToken(tok));
	 			CoordinateMap map = n.getCoordinateMap();
	 			lastIndex = 0;
	 			values[map.getMaxIndex()] = v;
	 			nvalues++;
	 		}
	 		else {
	 			char col = column.charAt(0);
	 			int row = G.IntToken(tok);
	 			double v = Math.abs(G.DoubleToken(tok));

	 			if((permutation&1)!=0) { row = ncols+1-row; }
	 			if((permutation&2)!=0) { col = (char)(('A'+ncols-1)-col+'A'); }
	 			int ind = getMoveIndex(n,col,row,swap);
	 			values[ind] = v;
	 			lastIndex = ind;
	 			nvalues++;
	 			
	 		}
	 	}
	 	// special case, reported percentages are arbitrary where there is
	 	// only one choice.
	 	if(nvalues==1) { values[lastIndex]=0.99; }	
	 	td.values = values;
	 }
	 public commonMove constructMove(GenericNetwork n,TrainingData current,TrainingData prev)
	 {	CoordinateMap map = n.getCoordinateMap();
	 	boolean hasIn[] = current.hasInputs;
	 	boolean prevIn[] = prev.hasInputs;
	 	for(int idx = hasIn.length-1; idx>=0; idx--)
	 	{
	 		if(hasIn[idx]!=prevIn[idx])
	 		{
	 			char col = map.getColForIndex(idx);
	 			int row = map.getRowForIndex(idx);
	 			G.Assert(idx==map.getIndex(col, row),"index inverse map");

	 			return (new Twixtmovespec(MOVE_DROPB,col,row,prev.playerToMove));
	 			//m.putProp(TrainingData,prev);
	 		}
	 	}
	 	// no difference, must be a swap move
	 	//G.Assert(current.moveNumber==2,"should be move 2");
	 	return(new Twixtmovespec(SWAP,prev.playerToMove));

	 }
	 // parse and train on the data in one file
	 public double trainNetwork(GenericNetwork n,BufferedReader ps,String file,TDstack save,sgf_game moves) throws IOException
	 {	int rmsn = 0;
	 	double rms = 0;
	 	String line = null;
	 	int lastPerm = recordPermutations ? 3 : 0;
	 	int moveNumber = 0;
	 	TrainingData prevTd = null;
	 	sgf_node rootNode = moves.getRoot();
	 	sgf_node prevNode = rootNode;
	 	{
	 	sgf_node startNode = new sgf_node();
	 	commonMove start = new Twixtmovespec("Start p0",0);
	 	startNode.set_property(start.playerString(), start.longMoveString());
	     prevNode.addElement(startNode, Where.atEnd);
	     prevNode = startNode;
	 	}
	 	String prevLine2 = null;

	 	while((line=ps.readLine())!=null)
	 	{
	 		if(!"".equals(line))
	 		{
	 			if(line.charAt(0)!='/') 
	 			{
	 			String line1A = ps.readLine();
	 			if(line1A!=null)
	 			{
	 			String line2 = ps.readLine();
	 			if(line2!=null)
	 			{	
	 				for(int perm0 = 0;perm0<=lastPerm;perm0++)
	 				{
	 				TrainingData td = new TrainingData();
	 				int perm = perm0;
	 				boolean swap = parseInputs(td,n,line,line1A,perm,true);
	 				parseValues(td,n,line2,swap,perm);
	 				td.moveNumber = moveNumber;
	 				td.file = file;
	 				save.push(td); 
	 				
	 				if(perm==0 && moves!=null)
	 				{
	 					TrainingData rawTD = new TrainingData();
	 					// construct a training data with no permutations or black/white swap
	 					parseInputs(rawTD,n,line,line1A,0,false);
	 					rawTD.moveNumber = moveNumber;
	 					if(prevTd!=null)
	 					{
	 					commonMove m = constructMove(n,rawTD,prevTd);
	 					sgf_node node = new sgf_node();
	 				    node.set_property(m.playerString(), m.longMoveString());
	 				    if(prevLine2!=null) { node.set_property(TrainingData,prevLine2); }
	 				    prevNode.addElement(node);
	 				    prevNode = node;
	 					{
	 						sgf_node doneNode = new sgf_node();
	 						commonMove done = new Twixtmovespec("done",m.player);
	 						doneNode.set_property(done.playerString(), done.longMoveString());
	 					    prevNode.addElement(doneNode, Where.atEnd);
	 					    prevNode = doneNode;
	 						}}
	 				prevTd = rawTD;
	 				}
	 				}
	 			prevLine2 = line2;
	 			moveNumber++;
	 			}
	 		}}
	 		}
	 	}
	 	return(rmsn>0?0:rms/rmsn);
	 }

	 // train on one file, or collect the training data from one file
	 public sgf_game trainNetwork(GenericNetwork n,String file,TDstack ts,sgf_game theGame)
	 {	
	 	InputStream s=null;
	 	G.print("Train from "+file);
	 	try {
	 		s = new FileInputStream(new File(file));
	 		if(s!=null)
	 			{BufferedReader ps = new BufferedReader(new InputStreamReader(s));
	 			 trainNetwork(n,ps,file,ts,theGame);
	 			s.close();
	 			}
	 	} catch (IOException e) {
	 		throw G.Error("Load network error in %s %s",file,e);
	 	}
	 	return(theGame);
	 	}
	 
public double trainNetwork(GenericNetwork n,double inputs[],double values[],boolean rmsonly)
{	double oldvals[] = n.calculateValues(n.getLayer("IN"),inputs);
	double setvals[] = new double[oldvals.length];
	double rms = 0;
	double rmsn = 0;
	for(int lim=values.length-1; lim>=0; lim--)
	{	double v = values[lim];
		if(v!=-1)
			{ double dif = oldvals[lim]-v;
			  setvals[lim]=v;
			  rms += dif*dif;
			  rmsn++;
			}
		else { 
			double dif = oldvals[lim];
			setvals[lim]=0; 
			rms += dif*dif;
			rmsn++;
			}
	 }
	if(rmsn>0)
	{
	//	((GenericNetwork)n).learningRate=0.0001;
	if(!rmsonly) { n.learn(setvals); }
	//double []newvals = n.calculateValues(inputs);
	lastNetworkValues = null;//setvals;
	return(Math.sqrt(rms/rmsn));
	}
	return(0);
}
	// run the training in its own thread.  Must edit the source folder
	// for the current training set
	public String trainingSequence = "";
	public void runGame_train(ViewerProtocol v,final BoardProtocol b,String from,int maxPass,boolean untilWorse)
	{	boolean onceThrough = false;
		GameBoard = (TwixtBoard)b;
		board = (TwixtBoard)GameBoard.cloneBoard();
	 	InitRobot(v,v.getSharedInfo(),board,null,RobotProtocol.NEUROBOT_LEVEL);
		
		int ncells = board.nCells();
		File dirfile = new File(
				from
				);
		File files[] = dirfile.listFiles();
		//G.shuffle(new Random(),files);
		TDstack data = new TDstack();
		GenericNetwork net = (GenericNetwork)evalNet;
		trainingSequence += from+" ";
		for(File f : files)
			{ // collect an internal representation of the training data
				String path = f.getPath();
				sgf_game theGame = new sgf_game();
				sgf_game moves = trainNetwork(net,path,data,theGame);
				sgf_node root = moves.getRoot();
				moves.set_short_name(f.getName());
				root.set_property(sgf_names.setup_property, ((commonCanvas)v).gameType());
			    root.set_property(sgf_names.game_property,((commonCanvas)v).sgfGameType());
				v.addGame(moves);
				G.print(""+moves);
			}
		G.print(""+files.length+" games in "+from+" using "+net.getName());
		int rmsn2 = 0;
		double rms2 = 0;
		double prev_rms2 = 999;
		int pass = 0;
		Network savedNet = null;
		do
		{
		int rmsn = 0;
		double rms = 0;
		savedNet = net.duplicate();
		if(rmsn2>0) { prev_rms2 = rms2/rmsn2; }
		rms2 = 0;
		rmsn2 = 0;
		pass++;
		int resn=0;
		int last = data.size();
			int reserve = onceThrough ? last-1 : ((last*9/10)/4)*4;			// use the last 10% as training validation
			for(int lim=0; lim<last; lim++)
			{	
				TrainingData d = data.elementAt(lim);
				boolean res = lim>=reserve;
				double rmsv;
				if(res) { resn++; }
				//net.learningRate = 0.0001;
				if(d.inputs2!=null)
						{
						rmsv = trainNetworkI(net,d.inputs,d.inputs2,d.values,res);
						}
					else {
						rmsv = trainNetwork(net,d.inputs,d.values,res);
						}
				if(res)
				{
				rms2 += rmsv;
				rmsn2++;
				}
				else {
				rms += rmsv;
				rmsn++;
				}
			}

			G.print("Pass "+pass+" rms "+rmsn+"="+(rms/rmsn)+" control "+resn+"="+(rms2/rmsn2));
			net.testNetwork();
			net.saveNetwork("g:/temp/nn/"+net.getName()+"-"+ncells+"-"+(pass%100)+".txt", "trained from "+trainingSequence);
			 
		}
		while(pass<maxPass && (!untilWorse || ((rms2/rmsn2)<prev_rms2)));
		if((pass<maxPass) && (savedNet!=null)) { net.copyWeights(savedNet); }
		G.print("training done "+from+" using "+net.getName());
	}
	public void runRobotTraining(final ViewerProtocol v,final BoardProtocol b,SimpleRobotProtocol otherBot)
	{
		new Thread(new Runnable() {
			public void run() 
			{ trainingSequence = "";
			   runGame_train(v,b,"g:/temp/twixt/train/",50,true);
			}
		}).start();
	}
    enum TeachMode { Net, Trained, Live }
    TeachMode mode = TeachMode.Net;
    double trainedValues[] = null;
    public double getNeuroEval(TwixtCell c)
    {	mode = TeachMode.Net;
    	UCTMoveSearcher ss = monte_search_state;
    	if(robotRunning && ss!=null)
    	{
    		UCTNode root = ss.root;
    		if(root!=null)
    			{ commonMove children[] = root.cloneChildren();
    			  if(children!=null)
    			  {	
    				  for(commonMove child0 : children)
    				  {	  Twixtmovespec child = (Twixtmovespec)child0;
    					  if(c.col==child.to_col
    						  && c.row==child.to_row
    						  )
    					  {	
    						 UCTNode node = child.uctNode();
     						 if(node!=null)
     						 {
     							 double minv = 999;
     							 double maxv = 0;
    					  	
     							for(commonMove ch : children) 
    					  		{ UCTNode n = ch.uctNode();
    					  		  if(n!=null)
    					  		  {
    					  		  int vis = n.getVisits();
    					  		  minv = Math.min(Math.max(0,vis),minv);
    					  		  maxv = Math.max(vis, maxv);
    					  		}}
    						 
    							 int vis = node.getVisits();
    							 if(vis>0)
    							 {	return((Math.max(0, vis)-minv)/(maxv-minv));
    							 }
     						 }
     						 return(0);
    					  }}
    				  }
    			  }
     	return(0);
     	}
    	else if(teachNet!=null)
    	{	double minvalue = 999;
    		double maxvalue = -1;
    		double values[] = null;
    		mode = TeachMode.Net;
    		switch(mode)
    		{
    		case Net:
        		netCalculate(teachNet,GameBoard);
        		values = teachNet.getValues();
        		break;
    		case Live:
    			values = lastNetworkValues;
    			break;
    		case Trained:
    			values = trainedValues;
    			break;
    		default: break;
    		}
    		//teachNet.dumpWeights(true);
    		if(values!=null)
    		{
    		boolean swap = GameBoard.getPlayerColor(GameBoard.whoseTurn)==PieceColor.Black;
    		int index = getMoveIndex(teachNet,c.col,c.row,swap);
    		for(int lim=values.length-1; lim>0; lim--) { double v=values[lim]; minvalue = Math.min(Math.max(0, minvalue), v); maxvalue = Math.max(maxvalue, v); }
    		//if(c.col=='F'&&c.row==1) {G.print(""+c+" "+values[index]);}
    		double target = values[index];
    		double v = ((target-minvalue)/(maxvalue-minvalue));
    		//if(c.col=='A'&&c.row==1) { G.print("T "+target); }
    		return(v/*10*target*/);
    		//return(board.ncols*2*target);
    		}
    		return(0);
    	
    	}
    	
    	return(0);
    }


 }
class TrainingData
{
	String file;
	int moveNumber;
	int playerToMove;
	char color;
	commonMove lastMove;
	boolean[] hasInputs;
	double[] inputs;
	double[] inputs2;
	double [] values;
}
class TDstack extends OStack<TrainingData>
{
	public TrainingData[] newComponentArray(int sz) {
		return(new TrainingData[sz]);
	}
}
