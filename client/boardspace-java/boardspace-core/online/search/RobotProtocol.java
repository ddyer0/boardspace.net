package online.search;


import lib.Random;
import online.game.CommonMoveStack;
import online.game.Game;
import online.game.commonMove;


/** SimpleRobotProtocol plus some extra methods to interface with the search driver
 * is all your robot has to implement to do a complete search.
 * 
 * @author ddyer
 * @see SimpleRobotProtocol
 */
public interface RobotProtocol extends SimpleRobotProtocol
{	
	public static final int DUMBOT_LEVEL = 0;
	public static final int SMARTBOT_LEVEL = 1;
	public static final int BESTBOT_LEVEL = 2;
	public static final int PALABOT_LEVEL = 3;
	public static final int WEAKBOT_LEVEL = 4;
	public static final int TESTBOT_LEVEL_1 = 5;
	public static final int TESTBOT_LEVEL_2 = 6;
	public static final int RANDOMBOT_LEVEL = 7;
	public static final int MONTEBOT_LEVEL = 10;
	public static final int NEUROBOT_LEVEL = 11;
	public static final int PURE_NEUROBOT_LEVEL = 12;
	public static final int ALPHABOT_LEVEL = 13;
	public static final int Automa = 14;
	public void setSearcher(CommonDriver c);
	public CommonDriver getSearcher();
	/**
	 * get the auxiliary search tree viewer watching this robot
	 * @return the TreeViewer, or null if none exists
	 */
	public TreeViewerProtocol getTreeViewer();
	
	/**
	 * For UCT searches, return the initial bias for "wins" of a particular move.  Nominally this should be in the range
	 * of -1 to 1.  The exact values only matter relative to the other children of the same parent.
	 * This number is combined with the search driver {@link online.search.UCTMoveSearcher#initialWinRateWeight}.  This combination 
	 * effectively provides the results for a fixed number of fake visits that bias the initial
	 * probabilities that this child will be investigated further.
	 * @param newChild a new uct node
	 * @param the nominal new initial visits
	 * @param move		the move under consideration
	 * @param children	the full list of moves at this level
	 */
	public void setInitialWinRate(UCTNode newChild,int initialVisits,commonMove move,commonMove[]children);
	/**
	 * various search events call this method.  Your robot
	 * can override this method and you can breakpoint it
	 * to intercept these events and look around.
	 * @see online.search.Search_Driver#stop_at_eval_clock stop_at_eval_clock
	 * @see online.search.Search_Driver#stop_at_search_clock stop_at_search_clock
	 */
	public void Search_Break(String msg);

    /** retutn true if the game is over at the current position.
     * for use by the alpha-beta search driver */
    public boolean Game_Over_P();


    /** get a random move using seed rand.  The default implementation of this calls List_Of_Legal_Moves
     * 
     * @param rand
     * @return one of the moves or null as an error indication
     */
    public commonMove Get_Random_Move(Random rand);
    
    /**
     * create a copy of this player, suitable for running searches as a thread
     * @param name
     * @return a RobotProtocol (ie a robot)
     */
    public RobotProtocol copyPlayer(String name);
    /**
     * get a cached reply to m.  This is used in "last reply" context to choose killer moves.
     * 
     * @param m
     * @return a move
     */
    public commonMove Get_Reply_Move(commonMove m);
    
    public void Update_Last_Reply(double value,int forPlayer,commonMove prev,commonMove cur);
    
    /** move list for next round of search */
    public CommonMoveStack  List_Of_Legal_Moves();
    /** static evaluate the current board position from the viewpoint of "forplayer"
     * At this point the move has already been made.
     * The move spec is supplied so it can be used to store values of interest.
     * @param  m the move to evaluate. 
     */
    public double Static_Evaluate_Position(commonMove m);
    /**
     * width limit (or otherwise modify) the array of moves.  The moves have been evaluated and sorted, so
     * the high scorers and terminal are first, so the appropriate thing is to remove from the end.
     * the return value is the new value of sz, the first sz moves are used.
     * @param depth			current depth of search
     * @param maxdepth		current max depth
     * @param mvec			a sorted array of moves
     * @param sz			the current number of those
     * @return the number of moves remaining
     */
    public int Width_Limit_Moves(int depth,int maxdepth,commonMove mvec[],int sz);
    /**
     * 
     * re-score the value of the move as it is being passed to the caller.  For 2 player
     * games, the default implementation will handle this correctly by negating the value if
     * the player changes.   For multiplayer games, the static evaluation should store
     * separate values for each player to allow recalculating the value for the 
     * desired player.
     */
    public double reScorePosition(commonMove cm,int forplayer);
    /**
     * return the move that should be re-evaluated for search purposes.  For 2 player games
     * this is the same move.  For multiplayer games it's the ultimate leaf node.
     * @param cm
     * @return a move spec
     */
    public commonMove targetMoveForEvaluation(commonMove cm);
    
    /**
     * undo the effect of making move m.  The result should completely
     * revert the board back to the original state. 
     * @param m
     */
    public void Unmake_Move(commonMove m);
    /**
     * call when about to backpropogate unmove of m in the random tree
     * @param m
     */
    public void Backprop_Random_Move(commonMove m,double val);
    /**
     * allow the robot to make any preparations needed for the
     * simulation phase
     * @param s the current search driver
     * @param n the current node
     */
    public void Start_Simulation(UCTMoveSearcher s,UCTNode n);
    /**
     * allow the robot to clean up after finishing a simulation run
     */
    public void Finish_Simulation(UCTMoveSearcher s,UCTNode n);
    
    /**
     * make a move, and save enough information so the board state can be
     * reverted by {@link #Unmake_Move}
     * @param cm
     */
    public void Make_Move(commonMove cm);
 
    /**
     * pause and wait.  This is provided so you can put an override
     * and breakpoint in your own robot class.
     */
    public void doPause()  throws InterruptedException;
    public void setPause(boolean val); 
    public boolean getPause();
    public long commonPause();
    /**
     * return true of the search should be terminated at the current depth
     */
    public boolean Depth_Limit(int current, int max);
    
    /**
     * for UCT searchers, return true if the tree part should be limited here.
     * default is false;
     * @param current
     * @return a boolean
     */
    public boolean Tree_Depth_Limit(int current);

    /** normalized value to -1 to 1, normally used to score endgames for UCT
     */
    public double NormalizedScore(commonMove lastMove);
  

    /**
     * normally, call dr.Evaluate_And_Sort_Moves(sn,mvec) but possibly do something
     * else, or nothing. 
     * @param dr
     * @param sn
     * @param mvec
     * @return the number of moves remaining in mvec
     */
    public int Evaluate_And_Sort_Moves(Search_Driver dr,Search_Node sn,commonMove mvec[]);
    /**
     * get the associated Game object
     */
    public Game getGame();
    /**
     * get the move with the best win rate.  If r and randomization are significant,
	 * add a random factor to each winrate.  This is used to randomize the behavior
	 * of the first few moves in a game.
	 *
	 * if randomization>1, select among the top n moves 
	 * if randomization>0, add a random bias between 0 and randomization to the visitation percent
	 *
     * @param parent the root uctnode
     * @param r	the randomization seed to use
     * @param randomization the search driver
     * @return the selected move
     */
    public commonMove getBestWinrateMove(UCTNode parent,Random r,UCTMoveSearcher randomization);
    public double Static_Evaluate_Search_Move(commonMove mm,int current_depth,CommonDriver master);
	public void prepareForDescent(UCTMoveSearcher from);

	
	// *** TEMPORARILY RESTORED ***
	   /** re-score the move/value for the new player
     * for 2 player games, this is normally -val.  
     * @param lastMove
     * @param val
     * @param forPlayer
     * @return the normalized value (in -1.0 to 1.0) for a particular player
     */
   // public double NormalizedRescore(commonMove lastMove,double val, int forPlayer);
    
    /**
     * This is used to optimize nodes where a winning move exists.
     * @param lastMove
     * @return true if the current position is a win for the player of commonMove.
     */
    //public boolean WinForPlayer(commonMove lastMove);
}

//public interface RobotProtocol 
//{
//}
