package santorini;
import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

/** 
 * Santorini uses alpha-beta
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * @author ddyer
 *
 */
public class SantoriniPlay extends commonRobot<SantoriniBoard> implements Runnable, SantoriniConstants,
    RobotProtocol
{   
	private boolean SAVE_TREE = false;				// debug flag for the search driver
	
	private boolean KILLER = false;					// probably ok for all games with a 1-part move
	private int WEAKBOT_DEPTH = 12;
	private int DUMBOT_DEPTH = 14;
	private int GOODBOT_DEPTH = 16;
	private int BESTBOT_DEPTH = 18;
	private int MAX_DEPTH = BESTBOT_DEPTH;
	private int boardSearchLevel = 0;				// the current search depth
	private int TIMEPERMOVE = 10;
    /* constructor */
    public SantoriniPlay()
    {
    }


/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	SantoriniMovespec mm = (SantoriniMovespec)m;
    	boardSearchLevel--;
    	board.UnExecute(mm);
     }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   SantoriniMovespec mm = (SantoriniMovespec)m;
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
    double ScoreForPlayer(SantoriniBoard evboard,int player,boolean print)
    {	
     	boolean win = evboard.WinForPlayerByStepping(player);
    	if(win) { return(VALUE_OF_WIN+(1.0/(1+boardSearchLevel))); }
    	return(evboard.ScoreForPlayer(player,print));

    }
    
    /**
     * this is it! just tell me that the position is worth.  
     */
    public double Static_Evaluate_Position(commonMove m)
    {	int playerindex = m.player;
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,nextPlayer[playerindex],false);
        // don't dilute the value of wins with the opponent's positional score.
        // this avoids the various problems such as the robot committing suicide
        // because it's going to lose anyway, and the position looks better than
        // if the opponent makes the last move.  Technically, this isn't needed
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
    	SantoriniBoard evboard = GameBoard.cloneBoard();
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
        GameBoard = (SantoriniBoard) gboard;
        board = GameBoard.cloneBoard();
        switch(strategy)
        {
        case WEAKBOT_LEVEL:
        	MAX_DEPTH = WEAKBOT_DEPTH;
        	WEAKBOT = true;
        	break;
        case DUMBOT_LEVEL:
        	MAX_DEPTH = DUMBOT_DEPTH;
        	TIMEPERMOVE = 10;
        	break;
        case SMARTBOT_LEVEL:
        	TIMEPERMOVE = 15;
        	MAX_DEPTH = GOODBOT_DEPTH;
        	break;
        case BESTBOT_LEVEL:
        	TIMEPERMOVE = 20;
         	MAX_DEPTH = BESTBOT_DEPTH;
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
 
 public commonMove DoAlphaBetaFullMove()
    {
	 SantoriniMovespec move = null;
	 Search_Driver search_state =null;
        try
        {

            // it's important that the robot randomize the first few moves a little bit.
            int randomn = RANDOMIZE 
            				? ((board.moveNumber <= 4) ? (20 - board.moveNumber) : 0)
            				: 0;
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
            boardSearchLevel = 0;

            search_state = Setup_For_Search(depth, (double)(TIMEPERMOVE/60.0));
            search_state.save_all_variations = SAVE_TREE;
            search_state.allow_killer = KILLER;
            search_state.verbose=verbose;			// debugging
            search_state.save_top_digest = true;
            search_state.save_digest=false;	// debugging only
            search_state.check_duplicate_digests = false; 	// debugging only

            if (move == null)
            {
                move = (SantoriniMovespec) search_state.Find_Static_Best_Move(randomn,dif);
            }
        }
        finally
        {
            Accumulate_Search_Summary();
            Finish_Search_In_Progress();
        }

        if (move != null)
        {
            if(verbose>0)
            	{move.showPV("exp final pv: ");
            // normal exit with a move
            	search_state.Describe_Search(System.out);
            	}
            return (move);
        }

        continuous = false;
        // abnormal exit
        return (null);
    }


 /** search for a move on behalf of player p and report the result
  * to the game.  This is called in the robot process, so the normal
  * game UI is not encumbered by the search.
  */
  public commonMove DoFullMove()
  {	
	boolean godselection = false;
	switch(board.getState())
	{
	default: break;
	case GodSelect:
	case GodChoose:
	case MoveOrPassState:
		godselection = true;
	
	}
	if(MONTEBOT || godselection)
  	{
 	return(DoMonteCarloFullMove()); 
  	}
  	else
  	{
 	 return(DoAlphaBetaFullMove());
  	}
  }


 }