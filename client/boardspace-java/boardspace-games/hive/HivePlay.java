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
    
    TODO: check that alpha beta checks the refutation first for losing positions - speedup!
 */
package hive;

import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;
import lib.*;
/*
 * hive uses only alpha-beta
 */

public class HivePlay extends commonRobot<HiveGameBoard> implements Runnable, HiveConstants,
    RobotProtocol
{	
	private boolean KILLER_HEURISTIC = false;
	private boolean SAVE_TREE = false;				// debug flag for the search driver
	private int WEAKBOT_DEPTH = 3;
	private boolean ProgressiveSearch = false;
	private int DUMBOT_DEPTH = 5;
    //int SMARTBOT_DEPTH = 5;
	private int MAX_DEPTH = DUMBOT_DEPTH;
	private final double VALUE_OF_WIN = 100000.0;
	private final double VALUE_OF_DRAW = 0;
	private Evaluator evaluator = null;
    public Evaluator getEvaluator() { return(evaluator); }
    /* strategies */
    private int Strategy = DUMBOT_LEVEL;
    private HiveGameViewer viewer = null;
    private int boardSearchLevel = 0;				// the current search depth
    private double TIMEPERMOVE = 45.0;				// 45 seconds as a limit
    private boolean COLLECT_TREE = true;	// special hack to collect the move tree
    private int MONTE_DEPTH_LIMIT = 30;
    private boolean avoidSpiderOpening = false;
    private boolean pushToWin = false;
    private double sprintThreshold = 0;
    private double sprintProgressThreshold = 0;
    private boolean sprintEnabled = false;
    private int sprintPlayer = 0;
    private int pushCount = 0;
    private int sprintCount = 0;
    private int trustCount = 0;
     int pushCountSummary = 0;
     int sprintCountSummary = 0;
     int trustCountSummary = 0;

    /* constructor */
    public HivePlay()
    {
    }

    public void initStats()
    {
    	pushCount = 0;
    	sprintCount = 0;
    	trustCount = 0;
    }
    public String reportStats()
    {	String msg = "";
    	
    	if(pushCount>0) { /* msg += "push "; */ pushCountSummary += pushCount; }
    	if(sprintCount>0) { msg += "sprint "; sprintCountSummary += sprintCount; }
    	if(trustCount>0) { msg += " trust "; trustCountSummary += trustCount;}
    	if(msg!="") { return msg; }  	
    	return null;
    }
   public boolean Tree_Depth_Limit(int current)
   {	if(COLLECT_TREE
		   	&& board.hasPlayedQueen(board.whoseTurn)
		   	&& board.hasPlayedQueen(nextPlayer[board.whoseTurn])	
		   ) { return(true); }
	   return(false);
   }
/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	Hivemovespec mm = (Hivemovespec)m;
        board.UnExecute(mm);
        extendedSearch = false;
        //G.print("U "+mm +" "+ mm.local_evaluation +" "+mm.evaluation);
        boardSearchLevel--;
    }
    
    boolean extendedSearch = false;
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   Hivemovespec mm = (Hivemovespec)m;
        //G.print("E "+mm);
    	commonMove.EStatus stat = mm.depth_limited();
    	extendedSearch = stat==commonMove.EStatus.EVALUATED_CONTINUE;
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
        CommonMoveStack moves = board.GetListOfMoves(extendedSearch);
        return moves;
    }
    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    private double ScoreForPlayer(HiveGameBoard evboard,int player,boolean print)
    {	
     	// score wins as slightly better if in fewer moves
    	double val = evaluator.evaluate(evboard, player, print);	
     	return(val);
    }
    
    public double Static_Evaluate_Depth_Limited_Position(commonMove m)
    {
    	double val =  Static_Evaluate_Position(m.player,true);
        if(pushToWin)
        {	// the theory behind this is that if we end up with Q surrounded by 5, a win
        	// could be 1 move away, so we should extend the search by 1 ply to see if its
        	// the case.  This works great if there is a win in one more ply, but if not,
        	// this simple strategy has the effect of adding a complete ply to the search
        	// which can take a very long time.  There's also the real possibility that seeing
        	// one ply deeper, but only in part of the tree, will result in a different, wrong
        	// move being selected.   One way that might work is to search but only for winning
        	// moves, discard anything else completely.
         	if((boardSearchLevel < MAX_DEPTH) && evaluator.pushAnalysis(board,true,m))
        	{
        		m.set_depth_limited(commonMove.EStatus.EVALUATED_CONTINUE);
        		pushCount++;
        	}
        }

    	return val;
    }
    
    /**
     * this is it! just tell me that the position is worth.  
     */
    public double Static_Evaluate_Position(commonMove m)
    {	return Static_Evaluate_Position(m.player,false);
    }
    private double Static_Evaluate_Position(int playerindex,boolean depth_limited)
    {
        if(board.GameOver())
        {
        	if(board.WinForPlayerNow(playerindex))
        	{	// a quick win is better than a slow one
        		return VALUE_OF_WIN+1.0/(1+boardSearchLevel);
        	}
        	else if(board.WinForPlayerNow(nextPlayer[playerindex]))
        	{	// a slow loss is better than a quick one
        		return -(VALUE_OF_WIN+(1-1.0/(1+boardSearchLevel)));
        	}
        	// otherwise a draw. This has the curious effect of making
        	// the bot try something else rather than doing 3 reps
        	return VALUE_OF_DRAW;
        }

        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,nextPlayer[playerindex],false);
        
         if(sprintEnabled)
         {
        	 if(sprintPlayer==playerindex) { val1 = val1/sprintThreshold; }
        	 else { val0 = val0/sprintThreshold; }
         }
         double val = val0-val1;
   
         return(val);
    }
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {	if((GameBoard.pickedObject==null) || ((GameBoard.pickedSource!=null)&&(GameBoard.droppedDest!=null)))
    	{
            HiveGameBoard evboard = GameBoard.cloneBoard();
            evboard.pickedSource = null;
            evboard.droppedDest = null;
            evboard.pickedObject=null;
            double val0 = ScoreForPlayer(evboard,FIRST_PLAYER_INDEX,true);
         //   ScoreForPlayer(GameBoard,0,false);
            double val1 = ScoreForPlayer(evboard,SECOND_PLAYER_INDEX,true);
            System.out.println("Eval is "+ val0 +" "+val1+ " = " + (val0-val1));
    	}
    }
// reference
//Reference is <Mutant 1 Gen 0 NaN% 0>  -2.0 5.0 -10.0 -2.0 5.0 0.19 0.15 0.3 0.1 0.18 0.3 0.1 0.1 0.2 0.0 0.0 -1.0 -15.0 -40.0 -65.0 -120.0 -120.0 -120.0 -120.0 0.1 0.03 0.4 0.9 1.0 0.2 0.5 0.5 0.5 0.2
//Best is <Mutant 1641 Gen 10 15/23>  -1.8761271808040296 5.0 -10.0 -2.0 5.0 0.19 0.15 0.3198784530256497 0.1 0.18 0.3241070101129835 0.1 0.1 0.23517425633392552 0.0 0.10368355114835512 -1.0 -15.0 -40.0 -65.0 -120.0 -120.0 -120.0 -120.0 0.15699961041110627 0.03 0.4 0.9 1.1049315389555592 0.3186212061081777 0.5 0.6088599036495812 0.5 0.2
//Best is <Mutant 1634 Gen 16 22/35>  -1.8761271808040296 5.0 -9.840229710294532 -2.0 5.0 0.19 0.19071160523768804 0.3198784530256497 0.20418881309477493 0.18 0.3 0.1 0.1 0.2 0.026168391537827823 0.10368355114835512 -1.0 -14.812931198141351 -40.0 -64.96448888050489 -120.0 -120.0 -119.89700626077656 -120.0 0.13683349675325776 0.20308597439165973 0.423890975045258 0.9 1.0 0.2 0.5252136006403627 0.5415655228574109 0.5 0.2321128151411322
//Best is <Mutant 1653 Gen 12 22/38>  -1.8761271808040296 5.0 -9.840229710294532 -2.0 5.016235853882387 0.19 0.15 0.3198784530256497 0.1 0.18 0.3 0.1 0.1 0.23551280580420708 0.0794783206688099 0.0015408854436659205 -0.9854809497347041 -15.0 -40.0 -65.0 -119.94459726360358 -120.0 -120.0 -120.0 0.13683349675325776 0.03 0.4 0.9623774893588992 1.1049315389555592 0.2 0.5 0.6088599036495812 0.5 0.2
//Best is <Mutant 7897 Gen 233 60% 27>  -1.4900855185584436 5.391213625565254 -9.731346119706972 -1.7400461246257026 5.297779085151372 0.4795918952111696 0.3645690478674867 0.7403331853317793 0.3025043895565763 0.34972879381131117 0.6429499417013564 0.3716600046988636 0.34135240649712845 0.29421058856306465 0.10568120141267136 0.420261591643475 -0.7250807769248186 -14.749268488306836 -39.87054297684719 -64.99448550405083 -119.7244607358777 -119.68826677548203 -119.74636478225014 -119.47193793648694 0.29420335132439346 0.3345635523390911 0.5798587539706551 0.9250440560596794 1.2298555805154614 0.7778392414440484 0.6227657644439892 0.6000046079992983 0.7672523934154857 0.4371918501584282

static String ref1 = "-1.8761271808040296 5.0 -9.840229710294532 -2.0 5.0 0.19 0.19071160523768804 0.3198784530256497 0.20418881309477493 0.18 0.3 0.1 0.1 0.2 0.026168391537827823 0.10368355114835512 -1.0 -14.812931198141351 -40.0 -64.96448888050489 -120.0 -120.0 -119.89700626077656 -120.0 0.13683349675325776 0.20308597439165973 0.423890975045258 0.9 1.0 0.2 0.5252136006403627 0.5415655228574109 0.5 0.2321128151411322";
static String ref2 = "-1.4900855185584436, 5.391213625565254, -9.72420254617754, -1.7400461246257026, 5.310842840553082, 0.4795918952111696, 0.3645690478674867, 0.7403331853317793, 0.3025043895565763, 0.34972879381131117, 0.6429499417013564, 0.2749259681835597, 0.34135240649712845, 0.2852400506055991, 0.21034796041252124, 0.29137374095625246, -0.7250807769248186, -14.749268488306836, -39.66502472711038, -64.94029456306168, -119.6726413924168, -119.68826677548203, -119.74636478225014, -119.47193793648694, 0.29420335132439346, 0.3345635523390911, 0.5798587539706551, 0.9266316330472247, 1.2298555805154614, 0.9363361430368098, 0.7003492966715437, 0.6070508087466412, 0.7672523934154857, 0.36439824601369286";
static String ref3 = "-1.4900855185584436 5.391213625565254 -9.731346119706972 -1.7400461246257026 5.297779085151372 0.4795918952111696 0.3645690478674867 0.7403331853317793 0.3025043895565763 0.34972879381131117 0.6429499417013564 0.3716600046988636 0.34135240649712845 0.29421058856306465 0.10568120141267136 0.420261591643475 -0.7250807769248186 -14.749268488306836 -39.87054297684719 -64.99448550405083 -119.7244607358777 -119.68826677548203 -119.74636478225014 -119.47193793648694 0.29420335132439346 0.3345635523390911 0.5798587539706551 0.9250440560596794 1.2298555805154614 0.7778392414440484 0.6227657644439892 0.6000046079992983 0.7672523934154857 0.4371918501584282";

/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.
 */
 public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,  String ev, int strategy)
    {	mutantFile = "g:/temp/mutants-hive.txt";
        InitRobot(newParam, info, strategy);
        GameBoard = (HiveGameBoard) gboard;
        board = GameBoard.cloneBoard();
    	viewer = (HiveGameViewer)newParam;
    	Strategy = strategy;
    	switch(strategy)
        {
        default: throw G.Error("Not expecting strategy %s",Strategy);
        case WEAKBOT_LEVEL:
        	MAX_DEPTH = WEAKBOT_DEPTH;
        	MONTEBOT = false;
           	KILLER_HEURISTIC = true;
           	ProgressiveSearch = true;
        	evaluator = new RevisedStandardEvaluator();
        	break;
        case DUMBOT_LEVEL:
        	MAX_DEPTH = DUMBOT_DEPTH;
        	MONTEBOT = false;
           	KILLER_HEURISTIC = true;
           	evaluator = new RevisedStandardEvaluator();
        	break;
           	
        case SMARTBOT_LEVEL:
        	evaluator = new RevisedAugustEvaluator();
        	avoidSpiderOpening = true;
        	pushToWin = false;	// see comments, can't be used in its current form.
           	MAX_DEPTH = DUMBOT_DEPTH;
           	KILLER_HEURISTIC = true;
        	ProgressiveSearch = false;
        	MONTEBOT = false;
        	break;
        // this is the traditional smartbot, replaced by the august 2025 bot
        //case SMARTBOT_LEVEL: 
 		//	MAX_DEPTH = DUMBOT_DEPTH;//SMARTBOT_DEPTH;
		//	MONTEBOT = false;
	    //   	evaluator = new ThirdStandardEvaluator();
        //	break;
        case BESTBOT_LEVEL: 
        	evaluator = new RevisedSeptemberEvaluatorJJ();	// previous bestbot was EvaluatorN
        	avoidSpiderOpening = true;
        	ProgressiveSearch = false;
        	pushToWin = false;	// see comments, can't be used in its current form.
           	MAX_DEPTH = DUMBOT_DEPTH;
           	sprintThreshold = 70;
           	KILLER_HEURISTIC = true;
           	sprintProgressThreshold = 10;
        	MONTEBOT = false;
        	break;
        case MONTEBOT_LEVEL:
        	MONTEBOT = true;
        	COLLECT_TREE = false;
        	evaluator = new MonteEvaluator();
        	break;
         case TESTBOT_LEVEL_1:
        	evaluator = new RevisedSeptemberEvaluator();	// 
        	avoidSpiderOpening = true;
        	ProgressiveSearch = false;
        	pushToWin = false;	// see comments, can't be used in its current form.
           	MAX_DEPTH = DUMBOT_DEPTH;
           	KILLER_HEURISTIC = true;
           	sprintThreshold = 70;
           	sprintProgressThreshold = 10;
        	MONTEBOT = false;
        	break;

        case TESTBOT_LEVEL_2:
        	// this is an experiment with an evaluator that does not generate moves or consider mobility
        	// this result is that it gets - maybe - 2 ply more, but still gets crushed by slower evaluators
           	evaluator = new RevisedSeptemberEvaluatorX();	// 
        	avoidSpiderOpening = true;
        	ProgressiveSearch = false;
        	pushToWin = false;	// see comments, can't be used in its current form.
           	MAX_DEPTH = DUMBOT_DEPTH;
           	sprintThreshold = 70;
           	sprintProgressThreshold = 10;
         	break;
 

        case ALPHABOT_LEVEL:
        	// this is an experiment with an evaluator that does not generate moves or consider mobility
        	// this result is that it gets - maybe - 2 ply more, but still gets crushed by slower evaluators
           	evaluator = new NolookEvaluator();	// for selfplay mode
        	avoidSpiderOpening = true;
        	pushToWin = false;	// see comments, can't be used in its current form.
           	ProgressiveSearch = true;
           	MAX_DEPTH = DUMBOT_DEPTH+2;
           	sprintThreshold = 80;
           	sprintProgressThreshold = 10;
           	MONTEBOT = false;
        	break;

        }
    }

private int forPlayer = 0;
/** PrepareToMove is called in the thread of the main game run loop at 
 * a point where it is appropriate to start a move.  We must capture the
 * board state at this point, so that when the robot runs it will not
 * be affected by any subsequent changes in the real game board state.
 * The canonical error here was the user using the < key before the robot
 * had a chance to capture the board state.
 */
public void PrepareToMove(int playerIndex)
{	forPlayer = playerIndex;
    board.copyFrom(GameBoard);
    board.robotCanOfferDraw = viewer.canOfferDraw(board);
    int movesRemaining = Math.max(10, 20-board.moveNumber());
    TIMEPERMOVE = adjustTime(TIMEPERMOVE,movesRemaining);
}

// this is a hack for improved bot openings, to avoid the spider-spider openings
public commonMove Random_Good_Move(Search_Driver search,int n,double dif)
{	boolean ok = true;
	Hivemovespec selection = null;
	do
	{
		selection = (Hivemovespec)super.Random_Good_Move(search,n,dif);
		if(avoidSpiderOpening && board.moveNumber()<=4)
		{
				ok = selection.object.type!=PieceType.SPIDER;
			}
	
	}
	while (!ok);
	return selection;
}

/** search for a move on behalf onf player p and report the result
 * to the game.  This is called in the robot process, so the normal
 * game UI is not encumbered by the search.
 */
 public commonMove DoAlphaBetaFullMove()
 {
	 double startEval = Static_Evaluate_Position(board.whoseTurn,false);
	 long digest = GameBoard.Digest();
	 int reps = ((commonCanvas)viewer).repeatedPositions.numberOfRepeatedPositions(digest);
	 sprintEnabled = false;
	 initStats();
	 commonMove move = alphaBetaFullMove(startEval,reps);
	 if(move!=null)
	 {	
	  	if((sprintThreshold>0) && startEval>sprintThreshold)
	    {	double finalEval = move.evaluation();
	    
	    	if(finalEval>sprintThreshold 
	    			&& Math.abs(startEval-finalEval)<sprintProgressThreshold)
	    	{
	    		// retry search with sprint logic
	    		G.print("retry Using SPRINT logic");
	    		PrepareToMove(forPlayer);
	    		sprintEnabled = true;
	    		sprintPlayer = forPlayer;
	    		sprintCount++;
	    		commonMove move2 = alphaBetaFullMove(startEval,reps);
	    		sprintEnabled = false;
	    		if(move2!=null && !move2.Same_Move_P(move))
	    			{ G.print("sprint changed move from ",move," to ",move2);
	    			move = move2;
	    			}
	    		else { 
	    			G.print("Sprint chose the same move ",move);
	    		}
	    	}
	    }
	 }
	 if(move!=null)
	 {
		 String comment = reportStats();
		 if(comment!=null) { move.setComment(comment); }
	 }
	 return move;
 }
 private commonMove alphaBetaFullMove(double startEval,int repetitions)
 {
        Hivemovespec move = null;
        // it's important that the robot randomize the first few moves a little bit.
        int randomn = RANDOMIZE
        			? evaluator.canRandomize(board,board.whoseTurn) 
        			: 0;
        boardSearchLevel = 0;
        int depth = MAX_DEPTH;
        Search_Driver search_state = 
        		ProgressiveSearch 
        		? Setup_For_Search(depth+2,TIMEPERMOVE/60,depth-1)
        		: Setup_For_Search(depth,false); 
        try
        {
 
            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new Hivemovespec("Done", board.whoseTurn);
            }
            else if (board.pickedObject!=null)
            {	// unusual case where the robot takes over after someone
            	// else has picked up a piece.  Just cancel and start the
            	// move from scratch.
            	move = new Hivemovespec(RESET,board.whoseTurn);
            }

            //Setup_For_Search(5,false);
            search_state.save_all_variations = SAVE_TREE;
            //search_state.use_nullmove = NULLMOVE;
            search_state.verbose = verbose;
            //search_state.allow_killer = true;
            search_state.allow_best_killer = KILLER_HEURISTIC;
            search_state.good_enough_to_quit = VALUE_OF_WIN;
            search_state.save_top_digest=true;	// always on background check on the robot
            search_state.save_digest=false;	// debugging only
            search_state.check_duplicate_digests = false; 	// debugging only

 
            if(move==null)
            	{ G.print("evaluator ",evaluator);
            	  move = (Hivemovespec) search_state.Find_Static_Best_Move(randomn);
            	  
          	  	// this is anti-draw logic for repetitions and shutouts
                if(move!=null) 
                  	{ Hivemovespec newmove = (Hivemovespec)evaluator.gutAnalysis(search_state,startEval,move, repetitions); 
                  	  if(newmove!=move) { trustCount++; }
                  	  move = newmove;
                  	} 
       
            	  if(move!=null)
            	  {
            		 switch(move.op)
            		 {
            		 default: break;
            		 case MOVE_PMOVE_DONE: move.op = MOVE_PMOVE;
            		 	break;
            		 case MOVE_MOVE_DONE: move.op = MOVE_MOVE;
            		 	break;
            		 }
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
            if(G.debug() && (move.op!=MOVE_DONE)) 
            { 
            //move.showPV("exp final pv: ");
            // normal exit with a move
            //search_state.Describe_Search(System.out);
            //System.out.flush();
            }
            return (move);
        }

        continuous = false;
        // abnormal exit
        return (null);
    }


 public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
 	UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this);
 	try {
       if (board.DoneState())
        { // avoid problems with gameover by just supplying a done
            move = new Hivemovespec("Done", board.whoseTurn);
        }
        else 
        {
        boolean collecting = COLLECT_TREE && !board.hasPlayedQueen(board.whoseTurn);
        monte_search_state.save_top_digest = true;	// always on as a background check
        monte_search_state.save_digest=false;	// debugging only
        monte_search_state.timePerMove = 20;	// 20 seconds
        monte_search_state.alpha = 0.75;
        monte_search_state.verbose = 1;
        monte_search_state.final_depth = MONTE_DEPTH_LIMIT*3;
        monte_search_state.random_moves_per_second = 10000;
        monte_search_state.max_random_moves_per_second = 40000;	
        monte_search_state.maxThreads = 0;// DEPLOY_THREADS; threads don't work yet
        // setup for saving the tree
        if(collecting)
        {
        monte_search_state.dead_child_optimization = false;
        monte_search_state.uct_tree_depth = 100;			// save all moves up to tree depth limit
        monte_search_state.stored_child_limit = 10000000;	// save 10 million nodes
        monte_search_state.max_random_moves_per_second = 0;	// allow full bore	
        monte_search_state.timePerMove = 60*60;	// one hour
        }
        move = monte_search_state.getBestMonteMove();
        if(collecting)
        {
        	monte_search_state.saveAsGame(viewer,0.00001);	// threshold of 1/100,000 of the work
        }
        }
 		}
      finally { ; }
      if(move==null) { continuous = false; }
     return(move);
 }
 
 /**
  * for UCT search, return the normalized value of the game, with a small penalty
  * for longer games so we try to be effecient.
  */
 
 public double NormalizedScore(commonMove lastMove)
 {	int player = lastMove.player;
 	double win = ScoreForPlayer(board,player,false);
 	double win2 = ScoreForPlayer(board,player,false);
 	if(win>=1) return(1);
 	if(win2>=1) return(-1);
 	return(win-win2);
 }
 
	public void runGame_selfplay(ViewerProtocol viewer,commonRobot<?> otherBot)
	 {	commonRobot<?> robots[] = new commonRobot[] { this,otherBot};
		int rep = 0;
		while(true)
		{
		RepeatedPositions positions = new RepeatedPositions();
		commonMove start = viewer.ParseNewMove("start p0", 0);
		GameBoard.doInit();
		GameBoard.Execute(start,replayMode.Replay);
		Mutant m1 = Mutant.getLeastUsedMutant();
		Mutant m2 = Mutant.getRandomMutant();
		getEvaluator().setWeights(m1.parameters);
		otherBot.getEvaluator().setWeights(m2.parameters);
	 	while(!GameBoard.GameOver())
	 	{	int who = GameBoard.whoseTurn();
	 		robots[who].PrepareToMove(who);
	 		commonMove m = robots[who].DoFullMove();
	 		GameBoard.Execute(m,replayMode.Replay); 
	 		positions.checkForRepetition(GameBoard,m);
	 	}
	 	if(GameBoard.WinForPlayer(0)) { m1.update(1); m2.update( 0); }
	 	else if(GameBoard.WinForPlayer(1)) { m2.update(1); m1.update(0); }
	 	else { m1.updateDraw();m2.updateDraw();}
	 	rep++;
	 	if(rep%(Mutant.Pruning_Threshold*2)==0)
	 		{ Mutant.removeLowest(true);
	 		  Mutant.saveState(mutantFile);
	 		}
		}
	 }

	public void runBotGame(commonCanvas viewer,commonRobot<?>white,commonRobot<?> black)
	 {	// this and otherbot are running from the same board, but it's a copy of the viewer board
		commonRobot<?> robots[] = new commonRobot[] {white,black};

		BoardProtocol board = viewer.getBoard();
		boolean exit = false;
	 	while(!exit && !board.GameOver())
	 	{	int who = board.whoseTurn();
	 		robots[who].PrepareToMove(who);
	 		commonMove m = robots[who].DoFullMove();
	 		if(m!=null)
	 		{
	 		viewer.PerformAndTransmit(m); 
	 		String comment = m.getComment();
	 		if(comment!=null)
	 		{
	 			commonMove cm = viewer.getCurrentMove();
	 			cm.setComment(comment);
	 		}
	 		}
	 		else { exit = true; }	// strange to return null, 
	 	}
	 }

	 String gamebase = "g:/share/projects/boardspace-html/htdocs/hive/hivegames/smarttest/";
	 String testgames[] = new String[]{ 
			 "start-01","start-02",
			 "start-03","start-04", 
			 "start-05","start-06",
			 "start-07","start-08",
			 
			 "start-09","start-10",
			 "start-11","start-12", 
			 "start-13","start-14",
			 "start-15","start-16",
			 "start-17","start-18",
			 "start-19","start-20",
			 "start-21","start-22",
			 "start-23","start-24",
			 "start-25",
	 };
	 public void runRobotTraining(ViewerProtocol vv,BoardProtocol b,SimpleRobotProtocol otherBotf)
	 {	Mutant.Pruning_Percent = 0;
	 	final commonCanvas v = (commonCanvas)vv;
	 	final commonRobot<?>self = (commonRobot<?>)v.newRobotPlayer();
	 	final SimpleRobotProtocol otherBot = otherBotf;
	 	final String series = "M";
	 	Bot testbot = Bot.TestBot_1;
	 	StopRobot();
	 	new Thread(new Runnable() {
	 		public void run() 
	 		{ 
	 		// runRobotTraining(v,"-b-"+series,(commonRobot<?>)self,(commonRobot<?>)otherBot, Bot.Montebot,Bot.Bestbot,false	); 
			// runRobotTraining(v,"-w-"+series,(commonRobot<?>)self,(commonRobot<?>)otherBot, Bot.Montebot,Bot.Bestbot,true); 
	 			
		   // runRobotTraining(v,"-b-"+series,(commonRobot<?>)self,(commonRobot<?>)otherBot, Bot.Dumbot,Bot.Smartbot,false	); 
		 	//runRobotTraining(v,"-w-"+series,(commonRobot<?>)self,(commonRobot<?>)otherBot, Bot.Dumbot,Bot.Smartbot,true); 

	 		runRobotTraining(v,"-b-x"+series,(commonRobot<?>)self,(commonRobot<?>)otherBot,	Bot.Bestbot,testbot,false	); 
	 		
	 		runRobotTraining(v,"-b-s"+series,(commonRobot<?>)self,(commonRobot<?>)otherBot,	Bot.Smartbot,testbot,false	); 
	 		
	 		runRobotTraining(v,"-b-d"+series,(commonRobot<?>)self,(commonRobot<?>)otherBot, Bot.Dumbot,testbot,false);

	 		runRobotTraining(v,"-w-x"+series,(commonRobot<?>)self,(commonRobot<?>)otherBot, Bot.Bestbot,testbot,true);
	 		  
	 		runRobotTraining(v,"-w-s"+series,(commonRobot<?>)self,(commonRobot<?>)otherBot, Bot.Smartbot,testbot,true); 
	 		runRobotTraining(v,"-w-d"+series,(commonRobot<?>)self,(commonRobot<?>)otherBot, Bot.Dumbot,testbot,true); 

		 		}
		 	}).start();	 	
	 }
 	public void runRobotTraining(commonCanvas v,String series,commonRobot<?> white,commonRobot<?> black,
 			Bot bwhite,Bot bblack,boolean reverse)
 	{
 		v.ignoreRunThread = true;
	 	for(String gamename : testgames)
	 	{	
	 		trainGameForSure(gamename,v,series,white,black,bwhite,bblack,reverse);
	 	}
 	}
 	public boolean trainGameForSure(String gamename,commonCanvas v,String series,commonRobot<?> white,commonRobot<?> black,
 			Bot bwhite,Bot bblack,boolean reverse)
 	{
 		boolean complete = trainGame(gamename,v,series,white,black,bwhite,bblack,reverse);
 		if(!complete)
 		{	
 			complete = trainGame(gamename,v,series,white,black,bwhite,bblack,reverse);
 			if(!complete)
 			{
 				G.Error("double fail for ",gamename);
 			}
 		}
 		return complete;

 	}
 	public boolean trainGame(String gamename,commonCanvas v,String series,commonRobot<?> white,commonRobot<?> black,
 			Bot bwhite,Bot bblack,boolean reverse)
 	{		String fullname = "file:///" + gamebase + gamename + ".sgf";
			BoardProtocol cloneBoard = v.getBoard();
	 		try {
	 		v.replayGame(fullname);
	 		Bot whiteBot = reverse ? bblack :  bwhite;
	 		Bot blackBot = reverse ? bwhite : bblack;

	 		white.setName("white playing "+whiteBot.name);
	 		black.setName("black playing "+blackBot.name);

	 		white.InitRobot(v,v.getSharedInfo(),cloneBoard,null,whiteBot.idx);
	 		black.InitRobot(v,v.getSharedInfo(),cloneBoard,null,blackBot.idx);
	 		
	 		commonPlayer p0 = v.getPlayerOrTemp(0);
	 		commonPlayer p1 = v.getPlayerOrTemp(1);
	 		p0.setPlayerName(whiteBot.name,false,null);
	 		p1.setPlayerName(blackBot.name,false,null);
	 		black.StopRobot();
	 		white.StopRobot();
	 		G.print("running ",fullname);
	 		runBotGame(v,(commonRobot<?>)white,(commonRobot<?>)black); 
	 		boolean gameover = cloneBoard.GameOver();
	 		if(!gameover)
	 		{
	 			runBotGame(v,(commonRobot<?>)white,(commonRobot<?>)black); 
	 			gameover = cloneBoard.GameOver();
	 		}
	 		String result = !gameover 
	 					? "-incomplete"
	 					: cloneBoard.WinForPlayer(0)
	 					 ? (reverse ? "-win" : "-loss")
	 				     : cloneBoard.WinForPlayer(1)
	 				     	? (reverse ? "-loss" : "-win")
	 				     	: "-tie";
	 		String finalName = gamebase+gamename+series+result+".sgf";
	 		G.print("finished ",finalName);
	 		v.saveGame(finalName);
	 		if(!gameover)
	 		{
	 			G.print("game ",finalName," not complete");
	 		}
	 		return gameover;
	 		}
	 		catch (Throwable err)
	 		{  throw G.Error("error processing "+gamename+"\n"+err+"\n"+err.getStackTrace());
	 		}
	 		
	 }
 }