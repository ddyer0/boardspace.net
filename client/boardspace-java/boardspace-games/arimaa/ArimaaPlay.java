/* copyright notice */package arimaa;

import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;
import lib.*;


/** 
 * Arimaa uses Alpha-Beta only
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 *
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * 
 * strategy for dealing with repetition (shared with XiangQi)
 * (1) start with a copy of the list of repeated positions shared with the source game
 * (2) at top level, check new moves against the list.  If we hit the repetition limit, 
 *     value this move at -infinity and set the depth limit flag.
 * (3) as turns go by without a captur or advancing a rabbit, increase the value of moves that would do so.
 *     
 * Depth is tuned to be based on an 8 ply search, at step 1, 7 ply at step 2 and so on.
 * This always sees to the end of the opponent response.  Width is pruned more severely
 * at step 1 and less for subsequent steps, so overall time per move/step is similar, but
 * each step sees a more complete picture of the possible responses.  This works much better
 * for Arimaa than ordinary progressive deepening.  It's really important to have some idea
 * what damage the opponent can do using all 4 response steps.
 * @author ddyer
 *
 * TODO: investigate using https://github.com/lightvector/arimaasharp as the basis for a better bot
 * 
 */
public class ArimaaPlay extends commonRobot<ArimaaBoard> implements Runnable, ArimaaConstants,
    RobotProtocol
{   
	static final double VALUE_OF_WIN = 10000.0;		// a fairly arbitrary number
	private boolean SAVE_TREE = false;				// debug flag for the search driver
	private boolean KILLER = false;					// probably ok for all games with a 1-part move
	private boolean WIDTH_LIMITS = true;
	private boolean TRANSPOSITIONS = true;
	private boolean USE_PV_SEQUENCE = true;
	private boolean RANDOMPLAY = false;
	private int SEMI_HEAVY_PLAYOUTS = 0;
	private int PIECE_HYSTERYSIS = 0;
	private int PUNISH_RABITS = 0;
	private int BOTDEPTH = 9;
	private int DEFAULT_DEPTH = 7;
	private boolean LOCAL_NULLMOVE = false;
	private boolean GLOBAL_NULLMOVE = true;
	private IntObjHashtable<ArimaaMovespec> transpositions = new IntObjHashtable<ArimaaMovespec>();
	private int transpositions_hit = 0;
	private int transpositions_probe = 0;
	private int transpositions_stored = 0;
     /* strategies */
	private int SEARCH_LEVEL = DUMBOT_LEVEL;
	private int current_depth_limit = 0;
	private boolean casual_move = false;
	private boolean depth_limited = false;
	private boolean serious_move = false;
	private boolean nullmove_done = false;
	private boolean RANDOMIZING_RESULT = false;		// set when we start a search

	private int randomized_width_trim_level[]= { 0,15,10};
	private int casual_width_trim_level[] =    { 0,0,0,15,15,12,12,10};
	private int standard_width_trim_level[] = { 0,0,0,20,20,15,12,10};
	private int serious_width_trim_level[] =  { 0,0,0,25,22,20,15,12};
	private int bestbot_width_trim_level[] = {0,0,0,20,10,10,10,10,5};
    
    private double baseline_score = 0.0;
    /* constructor */
    public ArimaaPlay()
    {
    }

    private int[]getWidthLimits()
    {
    	int trim[] = RANDOMIZING_RESULT?randomized_width_trim_level
    			: SEARCH_LEVEL==BESTBOT_LEVEL
    				? bestbot_width_trim_level
    				: ((SEARCH_LEVEL==DUMBOT_LEVEL) || (SEARCH_LEVEL==WEAKBOT_LEVEL))
    					? (serious_move ? serious_width_trim_level : (casual_move ? casual_width_trim_level : standard_width_trim_level))
    					: (serious_move ? serious_width_trim_level : standard_width_trim_level);
		return(trim);
    }
    public int Width_Limit_Moves(int current_depth,int max_depth,commonMove mvec[],int sz)
    {	if(WIDTH_LIMITS && nullmove_done)
    	{
    	int trim[] = getWidthLimits();
    	int n = trim.length-1;
    	int idx = current_depth>n ? n : current_depth;
    	int lim = trim[idx];
    	if((lim>sz) || (lim<=0)) { return(sz); }
    	while(sz>lim)
    	{	sz--;
    		commonMove target = mvec[sz];
    		// trim non-terminal moves back to a specified level
    		if(target.depth_limited()==commonMove.EStatus.NOT_EVALUATED)
    			{ G.print("off"); }
    		if(target.depth_limited()!=commonMove.EStatus.EVALUATED)
    			{ 
    			return(sz+1); 
    			}
    	};
    	}
    	return(sz);
    }


    /** return true if the search should be depth limited at this point.
     * 
     */
    public boolean Depth_Limit(int current, int max)
    {	// for simple games where there is always one move per player per turn
    	// current>=max is good enough.  For more complex games where there could
    	// be several moves per turn, we have to keep track of the number of turn changes.
    	// it's also possible to implement quiescence search by carefully adjusting when
    	// this method returns true.
    	current_depth_limit = max;
    	if(nullmove_done)
    	{
    	 if(casual_move) { current_depth_limit--; }
    	 if ((SEARCH_LEVEL==DUMBOT_LEVEL)||(SEARCH_LEVEL==WEAKBOT_LEVEL))
    	 	{ current_depth_limit--; 
    	 	}
    	}
         return(depth_limited || ((board.robotDepth+1)>=current_depth_limit));	// ignore the real depth and use the depth we maintain
   }

    /** undo the effect of a previous Make_Move.  These
     * will always be done in reverse sequence
     */
    public void Unmake_Move(commonMove m)
    {	ArimaaMovespec mm = (ArimaaMovespec)m;
    	// this is the point at which a final value is known
    	if(TRANSPOSITIONS 
    			&& (mm.best_move()!=null) 
    			&& (board.playStep==0)
    			)
    	{
    		long dig = board.Digest();
    		transpositions.put(dig,mm);
    		transpositions_stored++;
    	}
    	switch(m.op)
    	{ 
    	case MOVE_NULL: 
    		board.robotDepth -= mm.to_row;
    		if(!nullmove_done && mm.best_move()!=null)
    		{	double scorechange = (baseline_score-mm.evaluation());
    			// after the nullmove is done, characterize the move as "casual" if nullmove doesn't produce
    			// much score change.  Similarly, call it "serious" if the score change is big.  These are
    			// used to tweak the width limit and depth limit for the remainder of the search.
    			nullmove_done = true;
    			if(scorechange<(VALUE_OF_RABBIT/2))
    				{
    				G.print("Casual move, stake = "+scorechange);
    				casual_move = true;
    				}
    			else if(scorechange>(VALUE_OF_RABBIT*2)) 
    				{ serious_move = true;
    				  G.print("Serious move, stake = "+scorechange);
    			}
    			else { G.print("Normal move, stake = "+scorechange);
    			}
    		}
    		break;
    	case MOVE_PASS: board.robotDepth -= mm.to_row;
    		break;
    	case MOVE_PUSH:
    	case MOVE_PULL:	board.robotDepth -= 2;
    		break;
    	default: board.robotDepth -= 1;
    	}
    	depth_limited = false;
    	if( board.robotDepth<=4)
    	{	board.repeatedPositions.removeFromRepeatedPositions(m);
    	}
    	board.UnExecute(mm);
     }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   ArimaaMovespec mm = (ArimaaMovespec)m;
    	int startstep = board.playStep;
    	int nreps = board.RobotExecute(mm,board.robotDepth<=4);
     	depth_limited = (nreps>=3);
    	switch(m.op)
    	{ 
    	case MOVE_NULL: 
    	case MOVE_PASS:
    		{
    		int steps = 4-startstep;
    		board.robotDepth += steps;
    		mm.to_row = steps;
    		}
    		break;
    	case MOVE_PUSH:
    	case MOVE_PULL:	board.robotDepth += 2;
    		break;
    	default: board.robotDepth += 1;
    	}
    	}

/** return an enumeration of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {  	CommonMoveStack  all = board.GetListOfMoves();
    	if(LOCAL_NULLMOVE)
    	{
    	if((board.robotDepth==0)&&(board.board_state==ArimaaState.PLAY_STATE))
    		{
    		// add a nullmove to the list
    		 all.addElement(new ArimaaMovespec(MOVE_NULL,board.whoseTurn));// null move first
    		}
    	}
    	return(all);
    }
    
    // randomplay results in approximately a 5x increase in random moves per second
    public commonMove Get_Random_Move(Random rand)
    {	if(RANDOMPLAY)
    		{	commonMove m = board.getRandomMove(rand,PIECE_HYSTERYSIS,PUNISH_RABITS);
				if(board.robotDepth<SEMI_HEAVY_PLAYOUTS)
				{	// interesting experiment, but this made the program weaker
					commonMove m2 = board.getRandomMove(rand,PIECE_HYSTERYSIS,PUNISH_RABITS);
					double val = super.Static_Evaluate_Move(m);
					double val2 = super.Static_Evaluate_Move(m2);
					if(val2>val) { m = m2; }
				}
				return(m);
    		}
    	else { return(super.Get_Random_Move(rand)); 
    	
    	}
    }  

    
    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    private double ScoreForPlayer(ArimaaBoard evboard,int player,boolean print)
    {	
     	boolean win = evboard.WinForPlayerNow(player);
    	if(win) { return(VALUE_OF_WIN+(1.0/(1+board.robotDepth))); }
    	switch(SEARCH_LEVEL)
    	{
    	case WEAKBOT_LEVEL:
    	case DUMBOT_LEVEL:
		    	{
		    	return(evboard.ScoreForPlayer(player,print));
		    	}
    	case SMARTBOT_LEVEL:
    	case BESTBOT_LEVEL:
    	default:
    		{
        	return(evboard.ScoreForPlayer1(player,print));
    		}
    	}
    }
    private double coreStaticEval(int playerindex)
    {
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,nextPlayer[playerindex],false);

        // don't dilute the value of wins with the opponent's positional score.
        // this avoids the various problems such as the robot committing suicide
        // because it's going to lose anyway, and the position looks better than
        // if the opponent makes the last move.  Technically, this isn't needed
        // if is no such thing as a suicide move, but the logic
        // is included here because this is supposed to be an example.
        if(val0>=VALUE_OF_WIN) { return(val0); }
        if(val1>=VALUE_OF_WIN) { return(-val1); }
        return(val0-val1);
    }
    
    /**
     * this is it! just tell me that the position is worth.  
     */
    public double Static_Evaluate_Position(commonMove m)
    {	
    	if(TRANSPOSITIONS 
    			&& (board.playStep==0) 
    			&& ((board.robotDepth+1)<(current_depth_limit)))
    	{
    	// note we use only results from the turnover between players.
    	// using other results is unsound, because results from alpha/beta
    	// in one part of the tree may be different in another part.
    	long digest = board.Digest();
    	ArimaaMovespec pos = transpositions.get(digest);
    	transpositions_probe++;
    	if(pos!=null)
	    	{	ArimaaMovespec mm = pos;
	    		m.set_best_move(mm.best_move());
	    		transpositions_hit++;
	    		m.set_depth_limited(commonMove.EStatus.DEPTH_LIMITED_TRANSPOSITION);
	    		m.set_local_evaluation(m.setEvaluation(mm.evaluation()));
	    		// if this move is a positional duplicate of some previous, mark it and return the 
	    		// dynamic evaluation rather than the static local value
	    		return(m.evaluation());
	    	}
    	}
    	double val = coreStaticEval(m.player);
    	if(m.op==MOVE_NULL) 
    		{
    			val += VALUE_OF_WIN/2; 
    		}		// make it look very good so it will be first
        if(depth_limited) 
        	{ val -= VALUE_OF_WIN*2; }		// terrible outcome!
        return(val);
    }
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {
    	ArimaaBoard evboard = (ArimaaBoard)GameBoard.cloneBoard();
        double val0 = ScoreForPlayer(evboard,FIRST_PLAYER_INDEX,true);
        double val1 = ScoreForPlayer(evboard,SECOND_PLAYER_INDEX,true);
        if(val1>=VALUE_OF_WIN) { val0=0.0; }
        System.out.println("Eval is "+ val0 +" "+val1+ " = " + (val0-val1));
    }


/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.
 * 
 */
 public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
        String evaluator, int strategy)
    {
        InitRobot(newParam, info, strategy);
        GameBoard = (ArimaaBoard) gboard;
        board = (ArimaaBoard)GameBoard.cloneBoard();
        board.acceptPlacement();
        SEARCH_LEVEL = strategy;
        MONTEBOT = false;
        RANDOMPLAY = true;
        SEMI_HEAVY_PLAYOUTS = 0;		// this didn't work out
        PIECE_HYSTERYSIS = 0;			// this didn't work out (strategy==MONTEBOT_LEVEL) ? 0 : 25;
        PUNISH_RABITS = 15;	// this worked a little, but bugger values made the bot blind to rabit runs
        switch(strategy)
        {
        case WEAKBOT_LEVEL:
        	BOTDEPTH = 8;
        	DEFAULT_DEPTH = 6;
        	break;
        case DUMBOT_LEVEL:
        case SMARTBOT_LEVEL:
        	BOTDEPTH=9;
        	break;
        case BESTBOT_LEVEL:
        	BOTDEPTH = 13;
        	break;
        default: G.Error("Not expecting level", strategy);
        }

    }
    /** copy the game board, in preparation for a search */
    public void InitBoardFromGame()
    {
        board.copyFrom(GameBoard);
        board.acceptPlacement();
    }
    
    private long sequence_digest = 0;
    private ArimaaMovespec sequence = null;
 private ArimaaMovespec useExpectedSequence()
    {	commonMove move = sequence;
    	long newd = board.Digest();
    	if(newd!=sequence_digest) { move = null; }
    	recordExpectedSequence(move);
      	return((ArimaaMovespec)move);
    }
 private void recordExpectedSequence(commonMove move)
    {	sequence_digest = 0;
    	sequence = null;
    	if(USE_PV_SEQUENCE && (move!=null))
    	{
    		sequence = (ArimaaMovespec)move.best_move();
    		if((sequence!=null) && (sequence.player==move.player))
    		{	// record the digest that starts the expected sequence
    			Make_Move(move);
    			sequence_digest = board.Digest();
    			Unmake_Move(move);
    		}
    	}

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
	 ArimaaMovespec move = null;
	 ArimaaMovespec search_move = null;
	 //
	 // these are the start position of the current player move
	 //
     int randomn = RANDOMIZE 
				? ((board.moveNumber <= 4) ? (7 - board.moveNumber) : 0)
				: 0;
	int botdepth = BOTDEPTH;	// 9 gets a full 2-player search
	int depth = (board.board_state==ArimaaState.INITIAL_SETUP_STATE)
		?5:
		Math.max(DEFAULT_DEPTH,botdepth-board.playStep);	// search depth
	RANDOMIZING_RESULT = randomn>0;
	if(RANDOMIZING_RESULT) { depth=Math.min(depth,6); }		// randomization turns off alpha-beta
	double dif =  board.board_state==ArimaaState.INITIAL_SETUP_STATE?1.0:5.0;		// stop randomizing if the value drops this much
	// if the "dif" and "randomn" arguments to Find_Static_Best_Move
	// are both > 0, then alpha-beta will be disabled to avoid randomly
	// picking moves whose value is uncertain due to cutoffs.  This makes
	// the search MUCH slower so depth ought to be limited
	// if ((randomn>0)&&(dif>0.0)) { depth--; }
	// for games where there are no "fools mate" type situations
	// the best solution is to use dif=0.0;  For games with fools mates,
	// set dif so the really bad choices will be avoided
	board.robotDepth = 0;
	
	//Setup_For_Search(depth,TIME_LIMIT,6);
	Search_Driver search_state = Setup_For_Search(depth,false);

	try
        {

            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new ArimaaMovespec("Done", board.whoseTurn);
            }
            if((board.board_state==ArimaaState.PLAY_STATE) && (board.WinForPlayerNow(board.whoseTurn)))
            {
            	move = new ArimaaMovespec(PASS,board.whoseTurn);
            }
            
            transpositions = new IntObjHashtable<ArimaaMovespec>();
            transpositions_hit = 0;
            transpositions_probe = 0;
            transpositions_stored = 0;
            // gradually increasing pressure to kill something or advance a rabbit
            board.advancement_weight = 0.05 * (board.moveNumber-board.lastAdvancementMoveNumber);
             // it's important that the robot randomize the first few moves a little bit.
            search_state.use_nullmove = GLOBAL_NULLMOVE;
            search_state.static_eval_optimization = true;
            search_state.return_nullmove = GLOBAL_NULLMOVE;
            search_state.save_all_variations = SAVE_TREE;
            search_state.good_enough_to_quit = VALUE_OF_WIN;
            search_state.allow_killer = KILLER;
            search_state.verbose=0;					// debugging
            search_state.save_top_digest = true;	// always on as a background check
            search_state.save_digest=false;			// debugging only
            search_state.check_duplicate_digests = false; 	// debugging only
            //search_state.stop_at_search_clock = 104866;
            if(move==null) 
            	{ move = useExpectedSequence();
            	  if(move!=null) { G.doDelay(1000); }
            	}
            if (move == null)
            {	baseline_score = coreStaticEval(board.whoseTurn);	// save the initial evaluation 
            	casual_move = false;
            	serious_move = false;
            	nullmove_done = false;
            	sequence = null;
            	{
                String msg = "";
                int trim[]=getWidthLimits();
                for(int i=0;i<trim.length;i++) { msg += " "+trim[i]; }
                G.print("P"+board.whoseTurn+" Advancement "+board.advancement_weight+" Depth "+depth+" Width"+msg);
            	}
                search_move = move = (ArimaaMovespec) search_state.Find_Static_Best_Move(randomn,dif);
                if(move!=null)
                {
                	if(move.op==MOVE_NULL)
                    {	
                		move = (ArimaaMovespec)search_state.Nth_Good_Move(1,0.0);	// second best
                    	if(move.evaluation()<=-VALUE_OF_WIN) 
                    	{
                    		move = new ArimaaMovespec(MOVE_RESIGN,board.whoseTurn);
                    	}
                    }
                	else
                		{recordExpectedSequence(move);
                		}
                		}

             }
        }
        finally
        {
            Accumulate_Search_Summary();
            Finish_Search_In_Progress();
        }

        if (search_move != null)
        {
            if(G.debug() && (search_move.op!=MOVE_DONE)) 
            	{ search_move.showPV("exp final pv: ");
            	  if(TRANSPOSITIONS)
            		  {System.out.println("Transpositions: store "+transpositions_stored+" probe "+transpositions_probe+" hit "+transpositions_hit);
            		  }
            	search_state.Describe_Search(System.out); 
            	System.out.flush();
            	}
            // normal exit with a move
            transpositions = new IntObjHashtable<ArimaaMovespec>();	// help the garbage collector
        }
        if(move!=null)
        	{return (move);
        	}
        continuous = false;
        // abnormal exit
        return (null);
    }


 }