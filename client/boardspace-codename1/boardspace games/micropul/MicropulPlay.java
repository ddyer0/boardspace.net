package micropul;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;
/** 
 * Micropul uses alpha-beta only
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * @author ddyer
 *
 */
public class MicropulPlay extends commonRobot<MicropulBoard> implements Runnable,
    RobotProtocol
    {
    static final double VALUE_OF_WIN = 100000.0;
    boolean SAVE_TREE = false;				// debug flag for the search driver.  Uses lots of memory
    final int WEAKBOT_DEPTH = 4;
    final int DUMBOT_DEPTH = 5;
    final int SMARTBOT_DEPTH = 6;
    final int BESTBOT_DEPTH = 7;
    int MAX_DEPTH = DUMBOT_DEPTH;						// search depth.
    
	static final boolean KILLER = false;	// if true, allow the killer heuristic in the search
	static final double GOOD_ENOUGH_VALUE = VALUE_OF_WIN;	// good enough to stop looking
				// this is appropriate for simple games like hex, but probably not too effective
				// until there is a much better evaluator.
    // this is an arbitrary value assigned to a winning position, so minmax
    // and alpha-beta will prefer wins to non-wins.  It's exact value is
    // unimportant, but it must at least double any non-winning score the
    // evaluator produces.  Integers are cheap, don't be stingy.  The main thing
    // is to have a convenient range of numbers to work with.
    /* strategies */
	int Strategy = DUMBOT_LEVEL;
    
    int boardSearchLevel = 0;				// the current search depth

    /* constructor */
    public MicropulPlay()
    {
    }

/** Called from the search driver to undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	Micropulmovespec mm = (Micropulmovespec)m;
        board.UnExecute(mm);
        boardSearchLevel--;
    }
/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   Micropulmovespec mm = (Micropulmovespec)m;
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

    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    double ScoreForPlayer(MicropulBoard evboard,int player,boolean print)
    {	
		double val = 0.0;
     	boolean win = evboard.WinForPlayerNow(player);
     	// make wins in fewer moves look slightly better. Nothing else matters.
     	if(win) 
     		{ val = VALUE_OF_WIN+(1.0/(1+boardSearchLevel));
     		  if(print) {System.out.println(" win = "+val); }
     		  return(val); 
     		}
     	// if the position is not a win, then estimate the value of the position
      	
    	switch(Strategy)
    	{	default: throw G.Error("Not expecting strategy %s",Strategy);
    		case WEAKBOT_LEVEL:
    		case DUMBOT_LEVEL: 
    		case BESTBOT_LEVEL: 	// all the same for now
   			val = evboard.dumbotEval(player,print);
   			break;
    		case SMARTBOT_LEVEL:
    		val = evboard.smartbotEval(player,print);
    		break;
    	}
    	// we're going to subtract two values, and the result must be inside the
    	// bounds defined by +-WIN
    	G.Assert((val<(VALUE_OF_WIN/2))&&(val>=(VALUE_OF_WIN/-2)),"value out of range");
     	return(val);
    }
    
    /** this is called from the search driver to evaluate a particular position. The driver
     * calls List_of_Legal_Moves, then calls Make_Move/Static_Evaluate_Position/UnMake_Move
     *  for each and sorts the result to preorder the tree for further evaluation
     */
    public double Static_Evaluate_Position(commonMove m)
    {	int playerindex = m.player;
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,nextPlayer[playerindex],false);
        // don't dilute the value of wins with the opponent's positional score.
        // this avoids the various problems such as the robot comitting suicide
        // because it's going to lose anyway, and the position looks better than
        // if the oppoenent makes the last move.  Technically, this isn't needed
        // for hex because there is no such thing as a suicide move, but the logic
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
    	MicropulBoard evboard = GameBoard.cloneBoard();
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
        GameBoard = (MicropulBoard) gboard;
        board = GameBoard.cloneBoard();
        // strategy with be 0,1,2 for Dumbot, Smartbot, Bestbot
        Strategy = strategy;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy %s",strategy);
        case WEAKBOT_LEVEL: MAX_DEPTH = WEAKBOT_DEPTH; break;
        case DUMBOT_LEVEL: MAX_DEPTH = DUMBOT_DEPTH; break;
        case SMARTBOT_LEVEL: MAX_DEPTH = SMARTBOT_DEPTH; break;
        case BESTBOT_LEVEL: MAX_DEPTH = BESTBOT_DEPTH; break;
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

}
/** search for a move on behalf of player p and report the result
 * to the game.  This is called in the robot process, so the normal
 * game UI is not encumbered by the search.
 */
 public commonMove DoFullMove()
    {
        Micropulmovespec move = null;
        try
        {
 
            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new Micropulmovespec("Done", board.whoseTurn);
            }

            // it's important that the robot randomize the first few moves a little bit.
            int randomn = RANDOMIZE ? ((board.moveNumber <= 6) ? (40 - board.moveNumber) : 0) : 0;
            boardSearchLevel = 0;

            int depth = MAX_DEPTH;	// search depth
            double dif = 0.0;		// stop randomizing if the value drops this much
            // if the "dif" and "randomn" arguments to Find_Static_Best_Move
            // are both > 0, then alpha-beta will be disabled to avoid randomly
            // picking moves whose value is uncertain due to cutoffs.  This makes
            // the search MUCH slower so depth ought to be limited
            // if ((randomn>0)&&(dif>0.0)) { depth--; }
            // for games such as hex, where there are no "fools mate" type situations
            // the best solution is to use dif=0.0;  For games with fools mates,
            // set dif so the really bad choices will be avoided
            Search_Driver search_state = Setup_For_Search(depth, false);
            search_state.save_all_variations = SAVE_TREE;
            search_state.good_enough_to_quit = GOOD_ENOUGH_VALUE;
            search_state.verbose = verbose;
            search_state.allow_killer = KILLER;
            search_state.save_top_digest=false;	// doesn't pass because of rotation in jewels..
            search_state.save_digest=false;	// debugging only
            search_state.check_duplicate_digests = false; 	// debugging only

           if (move == null)
            {	// randomn takes the a random element among the first N
            	// to provide variability.  The second parameter is how
            	// large a drop in the expectation to accept.  For hex this
            	// doesn't really matter, but some games have disasterous
            	// opening moves that we wouldn't want to choose randomly
                move = (Micropulmovespec) search_state.Find_Static_Best_Move(randomn,dif);
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




 }