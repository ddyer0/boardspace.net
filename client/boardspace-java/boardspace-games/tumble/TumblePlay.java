/* copyright notice */package tumble;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

/** 
 * TumblingDown uses alpha-beta
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * @author ddyer
 *
 */
public class TumblePlay extends commonRobot<TumbleBoard> implements Runnable, TumbleConstants,
    RobotProtocol
{   
	private boolean SAVE_TREE = false;				// debug flag for the search driver
	private boolean KILLER = false;
	private static final int WEAKBOT_DEPTH = 2;
	private static final int DUMBOT_DEPTH = 3;
	private static final int GOODBOT_DEPTH = 4;
	private static final int BESTBOT_DEPTH = 5;
	private int MAX_DEPTH = BESTBOT_DEPTH;

	private double STACK_WEIGHT = 2.0;
	private double KING_WEIGHT=1.0;
	private boolean DUMBOT = false;
	private int boardSearchLevel = 0;				// the current search depth

    /* constructor */
    public TumblePlay()
    {
    }


/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	TumbleMovespec mm = (TumbleMovespec)m;
        board.UnExecute(mm);
        boardSearchLevel--;
    }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   TumbleMovespec mm = (TumbleMovespec)m;
    	boardSearchLevel++;
        board.RobotExecute(mm);
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
    double ScoreForPlayer(TumbleBoard evboard,int player,boolean print)
    {	
     	boolean win = evboard.WinForPlayer(player);
    	if(win) { return(VALUE_OF_WIN+(1.0/(1+boardSearchLevel))); }
    	return(evboard.ScoreForPlayer(player,print,STACK_WEIGHT,KING_WEIGHT,DUMBOT));

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
        // if there is no such thing as a suicide move, but the logic
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
    	TumbleBoard evboard = GameBoard.cloneBoard();
    	double val0 = ScoreForPlayer(evboard,FIRST_PLAYER_INDEX,true);
    	double val1 = ScoreForPlayer(evboard,SECOND_PLAYER_INDEX,true);
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
        GameBoard = (TumbleBoard) gboard;
        board = GameBoard.cloneBoard();
        switch(strategy)
        {
        case WEAKBOT_LEVEL:
        	MAX_DEPTH = WEAKBOT_DEPTH;
        	STACK_WEIGHT=2;
        	KING_WEIGHT = 1;
        	DUMBOT=true;
        	break;       
        case DUMBOT_LEVEL:
        	MAX_DEPTH = DUMBOT_DEPTH;
        	STACK_WEIGHT=2;
        	KING_WEIGHT = 1;
        	DUMBOT=true;
        	break;
        case SMARTBOT_LEVEL:
        	STACK_WEIGHT=2.0;
        	MAX_DEPTH = GOODBOT_DEPTH;
        	KING_WEIGHT = 1;
        	DUMBOT=false;
        	break;
        case BESTBOT_LEVEL:
        	STACK_WEIGHT=2.0;
        	MAX_DEPTH = BESTBOT_DEPTH;
        	KING_WEIGHT = 1;
        	DUMBOT=false;
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
 
 public commonMove DoFullMove()
    {
	 TumbleMovespec move = null;

        try
        {

            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new TumbleMovespec("Done", board.whoseTurn);
            }

            // it's important that the robot randomize the first few moves a little bit.
            int randomn = RANDOMIZE 
            				? ((board.moveNumber <= 4) ? (20 - board.moveNumber) : 0)
            				: 0;
            boardSearchLevel = 0;

            int depth = MAX_DEPTH;
            Search_Driver search_state = Setup_For_Search(depth, false);
            search_state.save_all_variations = SAVE_TREE;
            search_state.allow_killer = KILLER;
            search_state.verbose=verbose;			// debugging
            search_state.save_digest=false;	// debugging only
            search_state.check_duplicate_digests=false;

            if (move == null)
            {
                move = (TumbleMovespec) search_state.Find_Static_Best_Move(randomn);
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