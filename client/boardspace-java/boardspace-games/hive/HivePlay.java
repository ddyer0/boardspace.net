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
	private int DUMBOT_DEPTH = 5;
    //int SMARTBOT_DEPTH = 5;
	private int MAX_DEPTH = DUMBOT_DEPTH;
	private final double VALUE_OF_WIN = 100000.0;
	private final double VALUE_OF_DRAW = -10000;
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
    /* constructor */
    public HivePlay()
    {
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
        //G.print("U "+mm +" "+ mm.local_evaluation +" "+mm.evaluation);
        boardSearchLevel--;
    }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   Hivemovespec mm = (Hivemovespec)m;
        //G.print("E "+mm);
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
     double ScoreForPlayer(HiveGameBoard evboard,int player,boolean print)
    {	
     	boolean win = evboard.WinForPlayerNow(player);
     	// score wins as slightly better if in fewer moves
    	double val = win ? VALUE_OF_WIN+1.0/(1+boardSearchLevel) :evaluator.evaluate(evboard, player, print);		// points for a win, less for everything else
    	if(print && win) { System.out.println("+ win =");}

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
        	// which can take a very long time.  So until something much less general or more
        	// clever is done, this is not useful.
        	if(evaluator.pushAnalysis(board,true,m))
        	{
        		m.set_depth_limited(commonMove.EStatus.EVALUATED_CONTINUE);
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
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,nextPlayer[playerindex],false);
        // score wins alone, to avoid the "lesser loss" syndrone, where
        // the robot committs suicide because it makes the overall position 
        // look "better" than letting the opponent win.
        if(val0>=VALUE_OF_WIN) 
        	{ if(val1>=VALUE_OF_WIN) 
        		{ return(VALUE_OF_DRAW+val0-val1); // simultaneous win is a draw
        		}
        	  return(val0); 
        	}
         else if(val1>=VALUE_OF_WIN) { return(-val1); }
       
         return(val0-val1);
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
        	evaluator = new RevisedStandardEvaluator();
        	break;
        case DUMBOT_LEVEL:
        	MAX_DEPTH = DUMBOT_DEPTH;
        	MONTEBOT = false;
        	evaluator = new RevisedStandardEvaluator();
        	break;
        // this is the traditional smartbot, replaced by the august 2025 bot
        //case SMARTBOT_LEVEL: 
 		//	MAX_DEPTH = DUMBOT_DEPTH;//SMARTBOT_DEPTH;
		//	MONTEBOT = false;
	    //   	evaluator = new ThirdStandardEvaluator();
        //	break;
        case BESTBOT_LEVEL: 
        	MONTEBOT = false;
        	MAX_DEPTH = DUMBOT_DEPTH;//SMARTBOT_DEPTH;
        	COLLECT_TREE = false;
        	evaluator = new ThirdEvaluator(GameBoard.gamevariation);
        	break;
        case MONTEBOT_LEVEL:
        	MONTEBOT = true;
        	COLLECT_TREE = false;
        	evaluator = new MonteEvaluator();
        	break;
        	
        case SMARTBOT_LEVEL:
        	evaluator = new RevisedAugustEvaluator();	// for selfplay mode
        	avoidSpiderOpening = true;
        	pushToWin = false;	// see comments, can't be used in its current form.
           	MAX_DEPTH = DUMBOT_DEPTH;
        	MONTEBOT = false;
        	break;
        case TESTBOT_LEVEL_1:
        	evaluator = new RevisedAugustEvaluator();	// for selfplay mode
        	avoidSpiderOpening = true;
        	pushToWin = false;	// see comments, can't be used in its current form.
           	MAX_DEPTH = DUMBOT_DEPTH;
        	MONTEBOT = false;
        	break;

        case TESTBOT_LEVEL_2:
        	evaluator = new ThirdEvaluator();
        	//evaluator.setWeights("-0.0030393115373014957 0.0 0.0 -6.764150265716804E-4 0.005818957356462663 0.032516602242056644 0.0 0.0 0.0 0.0 0.0 0.006828758127886879 0.0 -0.0089800824176969 0.0 0.0 0.0 0.0 0.05673540886862403 0.007085739163627703 0.0 0.014891424225022947 -0.07852707816298556 0.012057557368815934 0.016648066107679466 -0.054052772020641265 0.0 0.0 -0.007753814908204941 0.01529130741164183 0.0 -0.006499017071934536 0.00861883741819886 0.013991669310243421 -6.762247264109535E-5 0.0 0.00448508541777407 0.0 0.04166365394198201 -0.0013262121259740348 0.0 -0.0043480306685558965 0.0 0.0 0.0 -0.003405745480912023 0.003940553826980168 0.002348696593720538 -0.03418999667679542 -0.014693603249188255 0.0 0.0 -0.006970370409373146 3.1798595109031345E-5 0.0");
        	MAX_DEPTH = 1;
        	verbose = 0;
        	MONTEBOT = false;
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
        Hivemovespec move = null;
        // it's important that the robot randomize the first few moves a little bit.
        int randomn = RANDOMIZE
        			? evaluator.canRandomize(board,board.whoseTurn) 
        			: 0;
        boardSearchLevel = 0;

        int depth = MAX_DEPTH+1;
        double startEval = Static_Evaluate_Position(board.whoseTurn,false);
        Search_Driver search_state = 
        		Strategy==WEAKBOT_LEVEL 
        		? Setup_For_Search(depth,TIMEPERMOVE/60,depth-1)
        		: Setup_For_Search(5,false); 
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
            search_state.allow_killer = KILLER_HEURISTIC;
            search_state.good_enough_to_quit = VALUE_OF_WIN;
            search_state.save_top_digest=true;	// always on background check on the robot
            search_state.save_digest=false;	// debugging only
            search_state.check_duplicate_digests = false; 	// debugging only


            if(move==null)
            	{ move = (Hivemovespec) search_state.Find_Static_Best_Move(randomn);
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
            	  // this is anti-draw logic for shutouts
            	  move = (Hivemovespec)evaluator.gutAnalysis(search_state,startEval,move);
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
 

 }