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
    
    /**
     * this is it! just tell me that the position is worth.  
     */
    public double Static_Evaluate_Position(commonMove m)
    {	int playerindex = m.player;
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
        	evaluator = new StandardEvaluator();
        	break;
        case DUMBOT_LEVEL:
        	MAX_DEPTH = DUMBOT_DEPTH;
        	MONTEBOT = false;
        	evaluator = new StandardEvaluator();
        	break;
        case SMARTBOT_LEVEL: 
 			MAX_DEPTH = DUMBOT_DEPTH;//SMARTBOT_DEPTH;
			MONTEBOT = false;
	       	evaluator = new RevisedStandardEvaluator();
	               	// version 2.19b
        	//"-2.5786549406209645 4.800823174160682 -9.48394627709441 -1.9099927861583068 4.781466938063065 0.7958562211974698 -0.20381335122965796 0.04538651894310872 0.06387665589419513 0.44442048230765707 0.25540749033684107 0.6612525195375272 -0.35564165719252794 0.5071881942155357 -0.36655825615123594 0.4564096711050024 -1.709126955140426 -15.251097492355413 -40.10250117851505 -65.13793067577012 -119.30719408953658 -119.96661677358534 -119.66248222490707 -119.96466996887226 0.41287956607205595 -0.04381823269309279 0.35204261176134993 0.12387363390710082 0.4376434743992227 0.09272641392964134 0.3761376782987065 0.7783975492909965 0.06846917495773097 0.141087966660209 1.8899387496795756 0.27153318680839944 1.3483572299058881 0.4838656571958469 1.0375147851480415 0.9122953863870906 1.3791361201518177 0.5335906519247893 1.621121557107937 0.9859719528371595 1.1022706981954276 1.3230898165311575 -0.04101100844937525 0.593718305244147 0.08849505508892525 -0.2008830015270762 -0.34166486539803076 -0.3169094306563969 0.02070988370647396 0.6572007916702362 0.2663708018187917 0.18634265215930815"
        	// mutant 9
        	//  "-2.5624088122609896 4.779774180313183 -9.612490092231857 -1.892352824659297 4.666100981959816 0.718083187322641 -0.18586996671760564 -0.020780218845105076 0.049193548641501966 0.3241872618114961 0.25540749033684107 0.6237280504743589 -0.36017415666207897 0.39923320312780153 -0.36655825615123594 0.38978170403444257 -1.6773162180211818 -15.357856121903328 -40.124866688753485 -65.05509982536118 -119.34225577806049 -120.02841507014365 -119.67503608761969 -119.91825314406482 0.4668963048227552 -0.06376666547989826 0.24168274571318088 0.1188587443705522 0.48421951544481795 0.1661096137366818 0.41630400204730544 0.7823603828436484 0.06846917495773097 0.10928587435550063 1.8899387496795756 0.2479657501689901 1.3609905941416012 0.5655431985745488 1.1013918849799613 0.8908088894043427 1.3791361201518177 0.6211379462431859 1.5697242410053927 0.9645971254413253 1.073918977876749 1.268101425950429 0.07843465378368084 0.564760129181633 0.12931683460779542 -0.2577486440137482 -0.35483335984301606 -0.07315395798773446 0.027006029622080974 0.6458307652426705 0.35745444288072226 0.10346521124757578"
        	// mutant 87, after 15 hours of 2 ply search
        	//		" -2.5726520620174327 4.817647963262021 -9.481984198605417 -1.9511098229342685 4.666056872229193 0.8051043841470136 -0.22997038710232792 0.02268155776705978 0.028011858352439734 0.2717385249097382 0.2552391750319432 0.5602864570258579 -0.41357203964311473 0.4747216865211541 -0.4349245127200305 0.4109477760849157 -1.6217331596375921 -15.336682660612823 -40.119192401000134 -65.06740238173875 -119.29730190889015 -119.99945502057406 -119.66419187281298 -120.1770867166336 0.47144344478855527 -0.04381823269309279 0.14615693682098913 0.11926554222476415 0.48421951544481795 0.4197836860481109 0.36588260107923465 0.8113506650188688 0.06846917495773097 0.11262232468633664 1.8912122135942835 0.31659231507481167 1.350353357010852 0.5256207884279871 1.160155530230105 0.8559084311713676 1.451849132481963 0.5596831683470269 1.6491202663945368 1.003188782087613 1.1709568432075697 1.2448111955040233 0.14169860951484953 0.4820800058145688 0.16929195472720174 -0.26605289968297047 -0.34166486539803076 -0.3046501724257093 0.023286902251115238 0.8359936432582699 0.33223182660166567 0.12791661132284102"
        	break;
        case BESTBOT_LEVEL: 
        	MONTEBOT = false;
        	MAX_DEPTH = DUMBOT_DEPTH;//SMARTBOT_DEPTH;
        	COLLECT_TREE = false;
        	evaluator = new ThirdEvaluator(GameBoard.gamevariation);
        	break;
        case MONTEBOT_LEVEL:
        	MONTEBOT = true;
        	COLLECT_TREE = false;
        	evaluator = new StandardEvaluator();
        	break;
        case TESTBOT_LEVEL_1:
        	evaluator = new ThirdEvaluator();	// for selfplay mode
        	MAX_DEPTH = 1;
        	verbose = 0;
        	MONTEBOT=false;
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
/** search for a move on behalf onf player p and report the result
 * to the game.  This is called in the robot process, so the normal
 * game UI is not encumbered by the search.
 */
 public commonMove DoAlphaBetaFullMove()
    {
        Hivemovespec move = null;
        // it's important that the robot randomize the first few moves a little bit.
        int randomn = RANDOMIZE
        			? ((board.moveNumber <= 8) ? (30 - board.moveNumber) : 0) 
        			: 0;
        boardSearchLevel = 0;

        int depth = MAX_DEPTH+1;

        Search_Driver search_state = Setup_For_Search(depth,TIMEPERMOVE/60,depth-1);	// 
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
            search_state.verbose= verbose;
            search_state.allow_killer = KILLER_HEURISTIC;
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
/*

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
        monte_search_state.final_depth = MONTE_DEPTH_LIMIT;
        monte_search_state.random_moves_per_second = 30000;
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
*/
 /**
  * for UCT search, return the normalized value of the game, with a small penalty
  * for longer games so we try to be effecient.
  */
 /*
 public double NormalizedScore(commonMove lastMove)
 {	int player = lastMove.player;
 	boolean win = board.WinForPlayerNow(player);
 	if(win) { return(0.99+0.01/boardSearchLevel); }
 	boolean win2 = board.WinForPlayerNow(nextPlayer[player]);
 	if(win2) { return(- (0.99+0.01/boardSearchLevel)); }
 	return(0);
 }
 */

 }