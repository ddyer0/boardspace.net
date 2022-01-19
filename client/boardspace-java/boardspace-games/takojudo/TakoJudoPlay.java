package takojudo;


import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

/** 
 * Takojudo uses alpha-beta
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * 
 * Tako Judo uses alpha-beta, with a fairly simple evaluator, and progressive deepening.
 * This worked a lot better than UCT.
 * 
 * @author ddyer
 *
 */
public class TakoJudoPlay extends commonRobot<TakojudoBoard> implements Runnable, TakojudoConstants,
    RobotProtocol
{
	// 
	// common parameters
	private boolean SAVE_TREE = false;				// debug flag for the search driver
    				// which type of robot to use
	private TakojudoViewer viewer = null;			// the viewer, so we can access the game history
	private boolean likelyDraw = false;				// true if both players are repeating positions
	private int boardSearchLevel = 0;				// the current search depth
	private int TIMEPERMOVE = 5;					// time per move (in seconds)

    // alpha-beta parameters
	private double VALUE_OF_DRAW = 10.0;			// in draw-is situations, the threshold for accepting the draw
	private boolean KILLER = false;					// probably ok for all games with a 1-part move
	private boolean QSCORE = true;					// quantitative scoring for head mobility
	private int MAX_DEPTH = 13;						// final max depth for progressive search
	private int WEAKBOT_DEPTH = 11;					// lower limit for weakbot
	private static final double VALUE_OF_WIN = 1000000.0;
	
 
    /* constructor */
    public TakoJudoPlay(TakojudoViewer v)
    {	viewer = v;
    }

/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	TakojudoMovespec mm = (TakojudoMovespec)m;
    	boardSearchLevel--;
    	board.UnExecute(mm,boardSearchLevel==0);
     }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   TakojudoMovespec mm = (TakojudoMovespec)m;
    	board.RobotExecute(mm,boardSearchLevel==0);
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
     * @param evboard the board to eval the score on
     * @param player
     * @param if true print some component scores
     * @return the evaluation for the position for the player
     */

    double ScoreForPlayer(TakojudoBoard evboard,int player,boolean print)
    {	
     	boolean win = evboard.WinForPlayerNow(player);
    	if(win) { return(VALUE_OF_WIN+(1.0/(1+boardSearchLevel))); }
    	return(evboard.ScoreForPlayer(player,print,QSCORE));

    }
    
    /**
     * this is it! just tell me that the position is worth.  This is called
     * from the search engine
     */
    public double Static_Evaluate_Position(commonMove m)
    {	int playerindex = m.player;
    	return(Static_Eval_POsitition(board,playerindex));
    }
    public double Static_Eval_POsitition(TakojudoBoard bb,int playerindex)
    {
        double val0 = ScoreForPlayer(bb,playerindex,false);
        double val1 = ScoreForPlayer(bb,nextPlayer[playerindex],false);
        // don't dilute the value of wins with the opponent's positional score.
        // this avoids the various problems such as the robot committing suicide
        // because it's going to lose anyway, and the position looks better than
        // if the opponent makes the last move.  
        if(val0>=VALUE_OF_WIN) { return(val0); }
        if(val1>=VALUE_OF_WIN) { return(-val1); }

        if(bb.getState()==TakojudoState.GAMEOVER_STATE) 
        	{ // this makes accepting a draw look like a good idea
        	  return(0.0); 
        	}

        return(val0-val1);
        
    }
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {
    	TakojudoBoard evboard = (TakojudoBoard)GameBoard.cloneBoard();
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
        GameBoard = (TakojudoBoard) gboard;
        board = (TakojudoBoard)GameBoard.cloneBoard();
        switch(strategy)
        {
        case WEAKBOT_LEVEL:
        	MAX_DEPTH = WEAKBOT_DEPTH;
			//$FALL-THROUGH$
		case DUMBOT_LEVEL:
        	TIMEPERMOVE = 20;
        	KILLER = true;
        	break;
        case SMARTBOT_LEVEL:
        	TIMEPERMOVE = 20;
        	break;
        case BESTBOT_LEVEL:
        	break;
        default: throw G.Error("Not expecting strategy %s",strategy);
        }
    }
    /** copy the game board, in preparation for a search */
    public void InitBoardFromGame()
    {	// record at top level if both players are in repetitive loops
    	likelyDraw = (viewer.currentRepetitionCount(0)>1) && (viewer.currentRepetitionCount(1)>1);
        board.copyFrom(GameBoard);
    }
/** search for a move on behalf onf player p and report the result
 * to the game.  This is called in the robot process, so the normal
 * game UI is not encumbered by the search.
 */
 public void PrepareToMove(int playerIndex)
 {	InitBoardFromGame();
 }

// if this is looking like a draw (because both players are repeating themselves)
// then in the absence of a breakthrough score increase, offer the draw or if 
// offered, accept it.
//
public TakojudoMovespec handleDraws(TakojudoMovespec move,double draw_threshold)
{
	
    if(likelyDraw && (move!=null) && (move.evaluation()<draw_threshold))
    {	switch(board.getState())
    	{
    		case PLAY_STATE:
    			return(new TakojudoMovespec(MOVE_OFFER_DRAW,board.whoseTurn));
    		case QUERY_DRAW_STATE:
    			return(new TakojudoMovespec(MOVE_ACCEPT_DRAW,board.whoseTurn));
    		default: break;
    	}
    }
    return(move);
}

 public commonMove DoAlphaBetaFullMove()
    {
	 TakojudoMovespec move = null;

        try
        {

            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new TakojudoMovespec("Done", board.whoseTurn);
            }

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
            double timelimit = TIMEPERMOVE/60.0;
            Search_Driver search_state = Setup_For_Search(depth+(likelyDraw?2:0),likelyDraw?timelimit*4:timelimit,5);		// time in minutes, start at depth 5
            search_state.save_all_variations = SAVE_TREE;
            search_state.allow_killer = KILLER;
            search_state.verbose=verbose;			// debugging
            search_state.save_top_digest = true;	// always on as a background check
            search_state.save_digest=false;	// debugging only
            search_state.check_duplicate_digests = false; 	// debugging only

            if (move == null)
            {	if(likelyDraw) { G.print("Likely draw, trying harder"); }
                move = (TakojudoMovespec) search_state.Find_Static_Best_Move(randomn,dif);
                if(likelyDraw) 
                	{ move = handleDraws(move,Static_Eval_POsitition(board,board.whoseTurn)+VALUE_OF_DRAW);
                	}
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