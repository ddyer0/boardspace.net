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
 */
package online.search;

import bridge.ThreadDeath;
import lib.*;

import online.common.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.game.sgf.sgf_reader;



/**
 * this is the base class for all robots.  It contains the connective
 * tissue to connect the robot to the game framework and the search engine,
 * so all your actual robot class needs to do is move and evaluate.
 * 
 * @author ddyer
 *
 */


public abstract class commonRobot<BOARDTYPE extends BoardProtocol> implements Runnable, RobotProtocol,Opcodes,OnlineConstants
{	public boolean MONTEBOT = false;
	public boolean WEAKBOT = false;
	protected CommonDriver search_driver = null;
	public boolean beingMonitored = false;
	public boolean beingMonitored() { return(beingMonitored || Running()); }
	public String name = "robot";
	public String getName() { return(name); }
	public String toString() { return("<robot "+name+">"); }
	public void setName(String n) { name=n; }
	public void Start_Simulation(UCTMoveSearcher s,UCTNode n) {};
	public void Finish_Simulation(UCTMoveSearcher s,UCTNode n) {};
	public void Notify(commonMove m) { throw G.Error("Not expected"); }
	public void setSearcher(CommonDriver s) 
		{ search_driver = s; 
		}
	
	public CommonDriver getSearcher() { return(search_driver); }
	

	public double Static_Evaluate_Position(commonMove m) {
		throw G.Error("Not implemented, needed for Alpha-Beta searches");
	}
	// used by the "start evaluator" option, not usually implemented for mcts searches
	public void StaticEval() {
		return;
	}
    //
    // stuff for nonrecursive alpha-beta search
    //
	/** our local copy of the game board */
	public BOARDTYPE board;
	/** the actual game board */
	public BOARDTYPE GameBoard;
	/**
	 * if true, the first few moves of a game are randomized.  If false, the 
	 * fixed "best move" line is played out.  This is actually implemented in
	 * your {@link #DoFullMove} method
	 */
	public boolean RANDOMIZE = true;
	public long RANDOMSEED = 0;
	/**
	 * this is the "default default" value, but normally overridden by the
	 * value supplied to {@link #Setup_For_Search}
	 */
    public static int DEFAULT_SEARCH_DEPTH = 5;
    /**
     * default for using alpha-beta pruning is true.  There's no reason
     * to change this except for desperate debugging measures.
     */
    public static boolean DEFAULT_ALPHA_BETA = true;
    /**
     * default for cutting off a search when the result is "good enough", where
     * the good enough value is a winning move.  The effect of this is to find
     * "a" winning move, not necessarily keep looking for the best winning move.
     */
    public static boolean DEFAULT_GOOD_ENOUGH = true;
    /**
     * if true, true a progressive deepening search.  This works well for
     * some games.  The actual value if this is normally determined by the
     * call to {@link #Setup_For_Search}
     */
    public static boolean DEFAULT_PROGRESSIVE_SEARCH = false;
    /**
     * true if the robot is running (ie; searching) "right now"
     */
    public boolean robotRunning = false; // running right now
    /**
     * if true, continue waiting for the next turn, otherwise exit after
     * completing one search.  This is normally true, but will be false
     * if using the "test one robot move" menu item.
     */
    public boolean continuous = false; // move continuously until error or end of game
    /**
     * this is set to true when the robot is being asked to stop early.
     */
    public boolean exitFlag = false;	// true when we're supposed to get out
    /** 
     * this is set to true when the robot should pause for debugging
     */
    private boolean pauseFlag = false;	// pause for debugging
    public void setPause(boolean val) { pauseFlag = val; }
    public boolean getPause() { return(pauseFlag); }
    
    public boolean paused = false;		// true only while paused
    /** 
     * this is the game controller from which we started.
     */
    public Game game = null; // the game from which we are running
    public ViewerProtocol viewer = null;
    public Game getGame() { return(game); }
    boolean robotMoveNow = false; // true if we should start the robot
    public Random rand = null; // source of randomness
    public int verbose = G.debug()?1:0; // default flag for noisy messages
    public boolean terminalNodeOptimize = true;	// default value for terminal node optimizaion
    public int search_depth = DEFAULT_SEARCH_DEPTH;
    public boolean progressive_search = DEFAULT_PROGRESSIVE_SEARCH;
    public boolean alpha_beta_cutoff = DEFAULT_ALPHA_BETA;
    public boolean good_enough_cutoff = DEFAULT_GOOD_ENOUGH;
    //public Search_Driver search_state=null;
    //public UCTMoveSearcher monte_search_state;
    public Search_Driver search_summary;
    public commonPlayer myPlayer=null;
    private commonMove theResult=null;
    public String threadName = "main";
    public static final boolean DEPLOY_MONTEBOT = true;
    /**
     * the maximum number of threads to use in a monte carlo seach.  Fewer may
     * be used if the environment claims there are fewer available.
     */
    public static final int DEPLOY_THREADS = 4;
    public static final int NO_THREADS = 0;
    
    public void setInitialWinRate(UCTNode node,int visits,commonMove m,commonMove mm[]) 
    { node.setBiasVisits(0,visits);
    }
    
    public boolean Tree_Depth_Limit(int current) { return(false); }
    
    public UCTThread[] getThreads()
    {	return(search_driver.getThreads());
    }
    
    public void selectThread(Thread t)
    {	search_driver.selectThread(t);
    };
    
    public double Static_Evaluate_Move(commonMove m)
    {
    	Make_Move(m);
    	double val = Static_Evaluate_Position(m);
    	Unmake_Move(m);
    	return(val);
    }
    
    public void Unmake_Move(commonMove m) {
    	throw G.Error("needed except in blitz monte carlo games");
    }
    
    public void Backprop_Random_Move(commonMove m,double val) {}
    
    /**
     * this is a manual replacement for clone() for codename1
     * @param other
     */
    public void copyFrom(commonRobot<BOARDTYPE> other)
    {	search_driver = other.search_driver;
    	sharedInfo = other.sharedInfo;
    	board = other.board;
    	WEAKBOT = other.WEAKBOT;
    	MONTEBOT = other.MONTEBOT;
    	GameBoard = other.GameBoard;
    	RANDOMIZE = other.RANDOMIZE;
    	RANDOMSEED = other.RANDOMSEED;
    	robotRunning = other.robotRunning;
    	continuous = other.continuous;
    	exitFlag = other.exitFlag;
    	pauseFlag = other.pauseFlag;
    	paused = other.paused;
    	game = other.game;
    	viewer = other.viewer;
    	robotMoveNow = other.robotMoveNow;
    	rand = other.rand;
    	verbose = other.verbose;
    	search_depth = other.search_depth;
    	progressive_search = other.progressive_search;
    	alpha_beta_cutoff = other.alpha_beta_cutoff;
    	good_enough_cutoff = other.good_enough_cutoff;
    	search_summary = other.search_summary;
    	myPlayer = other.myPlayer;
    	theResult = other.theResult;
    	threadName = other.threadName;
    	
    }
    @SuppressWarnings("unchecked")
	public commonRobot<BOARDTYPE> newInstance() 
    {	Exception err;
    	try {
    	Class<?>cl = getClass();
    	@SuppressWarnings("deprecation")
    	Object ob = cl.newInstance();
    	return((commonRobot<BOARDTYPE>)ob);
    	}
    	catch (Exception e) { err = e; }
     	throw G.Error("newInstance %s failed %s",this,err);
    }
    
    @SuppressWarnings("unchecked")
	public RobotProtocol copyPlayer(String name)
	{
		commonRobot<BOARDTYPE> c = newInstance();
		// don't call initrobot, as that starts the robot thread
		c.copyFrom(this);	// don't use clone for codename1 compatibility
		c.board = (BOARDTYPE)board.cloneBoard();
		threadName = name;
		return(c);
	}
    public commonMove getCurrentVariation()
    {	return(search_driver.getCurrentVariation());
    }
    public commonMove getCurrent2PVariation()
    {
    	return(search_driver.getCurrent2PVariation());
    }
    /**
     * Limit the move vector to a specified size.  The vector is full of moves
     * that have been static evaluated and sorted, so removing from teh end is appropriate.  
     */
    public int Width_Limit_Moves(int depth,int max,commonMove mvec[],int sz) { return(sz); }
    /**
     * this is used to make the result of a search available to the game that
     * requested it.
     * @param m
     */
    public synchronized void setResult(commonMove m)
    {	if (!exitFlag)
    	{
    	if(robotRunning) { theResult=m; }	// we were not stopped in progress
    	// if exitFlag is set, we may be trying to stop the robot so don't complain if
    	// it manages to squeeze out a move
    	else  { throw G.Error("set a result when not running %s",m);}
    	}
    }
    /**
     * return the active board for the "show alternate board" option.  This method is typically 
     * overridden.
     */
    public BOARDTYPE getBoard()
    {
        return (board);
    } 
    

    /**
     * get a display board from the currently running robot.  This could be for
     * entertainment, or debugging display of a robot that has been stopped.
     */
    public BoardProtocol disB()
    {	CommonDriver sd = search_driver;
    	BoardProtocol result = sd!=null ? sd.disB() : null;   	
    	if(result==null) 
    		{  BoardProtocol cb = (robotRunning&&board!=null) ? board : GameBoard;
    		   result = cb.cloneBoard();  	// no active search, clone the game board in case a search is starting up
    		   result.setName("robot "+cb.getName());
    		}
    	return(result);
    }

    /**
     * get the result of a search, and reset it to null.
     */
    public synchronized commonMove getResult() 
    { commonMove m = theResult;
      if(m!=null) 
      { robotRunning=false;   
        theResult=null; 
        if(!continuous) 
        	{ exitFlag = true; 
        	  setPause(false);
        	  paused = false;
        	  notify(); 
        	}
      }
      return(m);
    }
    public double progress = 0.0;
    /** set the current progress level.  This is used to make sure
     * the progress level is zero inbetween searches.
     */
    public void setProgress(double val)
    {
    	progress=val;
    }
    /** get the current progress level */
    public double getProgress() 
    { return(progress); }
    
    /**
     * override this method to capture the state of the board before
     * starting a search.
     * @param playerindex
     */
    public abstract void PrepareToMove(int playerindex);

    /**
     *  set up for a search, but don't actually start it. 
     *  
      * @param depth the target search depth
     * @param pro  if true, a progressive search with no time limit, if false, a single pass search
    */
    public Search_Driver Setup_For_Search(int depth,boolean pro)
    {	return(Setup_For_Search(depth,pro?9999999.0:0.0));
    }
    /** 
     * set up for a progressive deepening search, but don't actually start it
     * @param depth is the maximum depth
     * @param progressive_time time limit, in minutes
     */
    public Search_Driver Setup_For_Search(int depth, double progressive_time)
    {
    	return(Setup_For_Search(depth,progressive_time,0));
    }
    /** set up for a progressive deepening search, but don't actually start it.
     * 
     * @param depth max depth
     * @param progressive_time a double, time limit in minutes
     * @param first_depth depth of the first search in the progressive search
     */
    public Search_Driver Setup_For_Search(int depth, double progressive_time,int first_depth)
    {	
        if (search_summary == null)
        {
            search_summary = new Search_Driver();
        }

        Search_Driver actual = new Search_Driver(this, depth, progressive_time,first_depth,
                alpha_beta_cutoff, good_enough_cutoff);
        return(actual);
        

    }
    // this allows the robot class to override (and especially, eliminate)
    // the eval/sort phase
    public int Evaluate_And_Sort_Moves(Search_Driver dr,Search_Node sn,commonMove mvec[])
    {
    	return(dr.Evaluate_And_Sort_Moves(sn,mvec));
    }
    
    public void Accumulate_Search_Summary()
    {
        if (search_summary != null)
        {
            search_summary.Accumulate_Search_Summary(search_driver);
        }
    }
    public TreeViewerProtocol getTreeViewer() { return(viewer.getTreeViewer()); }
    /** initialize the robot, but don't run yet */
    public ExtendedHashtable sharedInfo;
    public TimeControl robotTime = null;
    public int robotStrategy = DUMBOT_LEVEL;
    
    /* adjust the time for the current move, based on the current time 
     * controls and an estimate of the length of the game
     */
    public double adjustTime(double defaultTime, int movesRemaining)
    {
        double time = defaultTime;
        int playerIndex = board.whoseTurn();
        double timeRemaining = viewer.timeRemaining(playerIndex);
        switch(robotTime.kind)
        {
        default: G.Error("Time mode %s not expected", robotTime.kind);
    		//$FALL-THROUGH$
    	case None:	
        	break;
        case Differential:
        case Fixed:
        case PlusTime:
        	
        	time = timeRemaining / (1000*Math.max(5, movesRemaining));
        }
        return(time);
    }
    

    public void InitRobot(ViewerProtocol v, ExtendedHashtable info, int strategy)
    {	sharedInfo = info;
        game = (Game)info.get(GAME);
        viewer = v;
        
    	robotTime = v.timeControl().copy();
     	robotStrategy = strategy;
     	
        RANDOMIZE = G.getBoolean(RANDOMIZEBOT,RANDOMIZE);
        RANDOMSEED = G.getInt(OnlineConstants.RANDOMSEED,0);
        rand = (RANDOMIZE && (RANDOMSEED==0)) ? new Random() : new Random(RANDOMSEED);
        search_summary = null;
        exitFlag = false;
        setPause(false);
        paused = false;
        if(!RANDOMIZE || G.debug()) { G.print("Randomize "+RANDOMIZE+" Debug "+G.debug()); }
        search_depth = DEFAULT_SEARCH_DEPTH;
        alpha_beta_cutoff = DEFAULT_ALPHA_BETA;
        good_enough_cutoff = DEFAULT_GOOD_ENOUGH;
        progressive_search = DEFAULT_PROGRESSIVE_SEARCH;

        Thread  robo = new Thread(this,"robot "+game);
        robo.start();
        robo.setPriority(robo.getPriority()-1);
    }

    public synchronized void doWait()
    {
        try
        {
            while(!(robotMoveNow || exitFlag || pauseFlag) ) { wait(); }
        }
        catch (InterruptedException e)
        {
        }
    }

    public synchronized void Finish_Search_In_Progress()
    {	
        if (search_driver != null)
        {	search_driver.Abort_Search_In_Progress(null);
        }
        notify(); /* somebody may be waiting */
    }


    /** 
     * start a robot, either continuous or single move.
     */
    public void StartRobot(boolean contin,commonPlayer who)
    {	exitFlag = false;
    	setPause(false);
    	paused = false;
        continuous = contin;
        if(who!=null) { myPlayer = who; }
    }

    public boolean Auto()
    {
        return (continuous);
    }

    public void StopRobot()
    {
        exitFlag = true;
        beingMonitored = false;
        setPause(false);
        paused = false;
        continuous = robotRunning = false;
        robotMoveNow = false;
        Finish_Search_In_Progress();
    }

    public synchronized boolean Running()
    {	notify();		// if there was a race that prevented the robot starting, start it now
        return (!exitFlag && (robotRunning || (theResult!=null)));
    }

    public synchronized void DoTurnStep(int playerindex)
    {
        if (robotRunning)
        {
        	throw G.Error("robot already running");
        }
        if(theResult==null)	// don't start a new turn if we already have a result pending
        {
        exitFlag = false;
        setPause(false);
        paused = false;
        robotRunning=true;
        PrepareToMove(playerindex);			// capture the board state
        robotMoveNow = true;				// let the robot awaken
        notify();
    }
    }
    
    public void DoGameOver()
    {
        StopRobot();
        if (game != null)
        {
            G.doDelay(1000);
        }
    }

    public synchronized void Quit()
    {
        exitFlag = true;
        robotRunning = false;
        setPause(false);
        paused = false;
        Finish_Search_In_Progress();
        notify();
    }

    public void Pause()
    {	setPause(true);
    }
    public long commonPause()
    {	long pausedTime = 0;
    	if(pauseFlag)
    	{
    	while(pauseFlag) 
    		{ long pausedStart = G.Date();
    		  synchronized (this)
    			{ paused = true;
    			  
    			  try
    				{ doPause();
    				}
    		  catch (InterruptedException e) {};
      		pausedTime += G.Date()-pausedStart;
    		}
        	paused = false;	
   		}
    	}
    	return(pausedTime);
    }
    
    public synchronized void doPause()  throws InterruptedException
    { wait(); 
    }

    public synchronized void Resume()
    {	setPause(false);
        notify();
    }
	 /* 
	  * {@inheritDoc}
	  */    
    public commonMove targetMoveForEvaluation(commonMove cm)
    {	return(cm);
    }

    public double reScorePosition(commonMove cm,int forplayer)
    {	
    	return((cm.player == forplayer)
    			? cm.evaluation()			// player doarsn't change 
    			: -cm.evaluation());		// player changes, negate the value
    }
    private boolean checking_digest()
    {	if(search_driver!=null)
    	 {
    		return(search_driver.save_digest||search_driver.save_top_digest);
    	 }
    	return(false);
    }
    
    public void run()
    {
        @SuppressWarnings("deprecation")
		String cname = this.getClass().getName();
        G.setThreadName(Thread.currentThread(),cname);
        try {
        for (; !exitFlag && ((game == null) ? true : !game.exitFlag());)
        {
            try
            {
                if (robotMoveNow)
                {
                    robotMoveNow = false;

                    //System.out.println("Delay");
                    	//G.doDelay(5000);
                    	//System.out.println("Start");
                    	BoardProtocol b = getBoard();
                    	if(b!=null) 
                    		{
                    		int moven = b.moveNumber();
                    		long dig = b.Digest();
                    		
                    		// make a copy and verify it before we start
                    		BoardProtocol clone = b.cloneBoard();
                    		b.sameboard(clone);
                    		G.Assert(dig==clone.Digest(),"digest matches before");
                    		
                        	commonMove m = DoFullMove();
                        	// these are basic consistency checks on the robots
                        	if(checking_digest())
                        		{int newmoven = b.moveNumber();
                        		G.Assert(moven==newmoven,"move number preserved");
                        		b.sameboard(clone);
                        		long newdig = b.Digest();
                        		G.Assert(dig==newdig,"digest preserved");
                        		}
                        	setResult(m);
                    		}
                    	else
                        {
                    		commonMove m = DoFullMove();
                    		setResult(m);
                        }
                    	  

                }
                else
                {
                    doWait();
                }
            }
        	catch (ThreadDeath err) { throw err;}
            catch (Throwable err)
            {
                StopRobot();
                if(game!=null)
                	{ game.logExtendedError(cname + " Robot main loop", err, true);
                	}
                else
                {	Http.postError(this,"Error in Robot",err);
                }
             }
        }}
        finally { StopRobot(); }

        System.out.println("Robot exit");
    }

    public void Search_Break(String message)
    {	System.out.println(message);
    }
    public double NormalizedRescore(commonMove m,double score,int forPlayer) { return((forPlayer==m.player)?score:-score); }

    public double NormalizedScore(commonMove m) { throw G.Error("NormalizedScore not implemented"); }

    
    public commonMove Get_Random_Move(Random rand)
    {
    	CommonMoveStack moves = List_Of_Legal_Moves();
    	int siz = moves.size();
    	if(siz==0) { return(null); }
    	if(siz==1) { return(moves.elementAt(0)); }
		int idx = Random.nextInt(rand,siz);
		return(moves.elementAt(idx));
    }

    public commonMove Get_Reply_Move(commonMove last)
    {	return(null);
    }

    public void Update_Last_Reply(double value,int forPlayer,commonMove pre,commonMove cur)
    {
    }
    
    public Evaluator getEvaluator() 
    { G.infoBox(null,"Evaluators not supported by this robot");
      return(null);
    }

    public String mutantFile = "g:/temp/mutants.txt";;
	public void runGame(ViewerProtocol viewer,commonRobot<?> otherBot)
	 {	// this and otherbot are running from the same board, but it's a copy of the viewer board
	 	while(true)
		{
		boolean mefirst = rand.nextDouble()>0.5;
		commonRobot<?> robots[] = new commonRobot[] { mefirst ? this : otherBot, mefirst?otherBot:this};
		commonMove start = viewer.ParseNewMove("start p0", 0);
	 	RepeatedPositions positions = new RepeatedPositions();
		GameBoard.doInit();
		GameBoard.Execute(start,replayMode.Replay);
		boolean useLeast = rand.nextDouble()>0.2;
		Mutant m1 = useLeast 
				? Mutant.getLeastUsedMutant()
				: Mutant.getRandomMutant();
		otherBot.getEvaluator().setWeights(m1.parameters);
	 	while(!GameBoard.GameOver())
	 	{	int who = GameBoard.whoseTurn();
	 		robots[who].PrepareToMove(who);
	 		commonMove m = robots[who].DoFullMove();
	 		
	 		GameBoard.Execute(m,replayMode.Replay); 
	 		positions.checkForRepetition(GameBoard,m);
	 		
	 	}
	 	if(GameBoard.WinForPlayer(0)) 
	 		{ if(mefirst) { m1.update(0); }
	 		}
	 	else if(GameBoard.WinForPlayer(1))
	 		{ if(mefirst) { m1.update(1); }
	 		}
	 	else { m1.updateDraw(); }

	 	if(useLeast && m1.nGames>=Mutant.Pruning_Threshold*2)
	 		{ 
	 		  Mutant.removeLowest(false);
	 		  Mutant.saveState(mutantFile);
	 		}
		}
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

	 public void runRobotGameDumbot(ViewerProtocol vv,BoardProtocol b,SimpleRobotProtocol otherBotf)
	 {	Mutant.Pruning_Percent = 0;
	 	final SimpleRobotProtocol otherBot = otherBotf;
	 	final ViewerProtocol v = vv;
	 	BoardProtocol cloneBoard = b.cloneBoard();
	 	InitRobot(v,v.getSharedInfo(),cloneBoard,null,RobotProtocol.TESTBOT_LEVEL_1);
	 	otherBot.InitRobot(v,v.getSharedInfo(),cloneBoard,null,RobotProtocol.TESTBOT_LEVEL_2);
	 	otherBot.StopRobot();
	 	StopRobot();
	 	
	 	Mutant.restoreState(otherBot.getEvaluator(),mutantFile);

	 	Evaluator evaluator = otherBot.getEvaluator();
	 	double w1[] = evaluator.getWeights();
	 	//G.setValue(w1, 0);
	 	Mutant.prepareMutants(w1);
	 	Mutant.saveState(mutantFile);

	 	new Thread(new Runnable() {
	 		public void run() { runGame(v,(commonRobot<?>)otherBot); }
	 	}).start();

	 }
	 public void runRobotTraining(final ViewerProtocol v,BoardProtocol b,final SimpleRobotProtocol otherBot)
	 {
		 G.infoBox("","Not defined for this robot");
	 }
	 
	 public void runRobotGameSelf(final ViewerProtocol v,BoardProtocol b,final SimpleRobotProtocol otherBot)
	 {	
	 	BoardProtocol cloneBoard = b.cloneBoard();
	 	InitRobot(v,v.getSharedInfo(),cloneBoard,null,RobotProtocol.TESTBOT_LEVEL_1);
	 	otherBot.InitRobot(v,v.getSharedInfo(),cloneBoard,null,RobotProtocol.TESTBOT_LEVEL_2);
	 	otherBot.StopRobot();
	 	StopRobot();
	 	
	 	Mutant.restoreState(otherBot.getEvaluator(),mutantFile);

		Evaluator evaluator = getEvaluator();
	 	double w1[] = evaluator.getWeights();
	 	//G.setValue(w1, 0);
	 	Mutant.prepareMutants(w1);
	 	Mutant.saveState(mutantFile);

	 	new Thread(new Runnable() {
	 		public void run() { runGame_selfplay(v,(commonRobot<?>)otherBot); }
	 	}).start();
	 
	 }

	 public commonMove getBestWinrateMove(UCTNode parent,Random r,UCTMoveSearcher search)
	 {
		 double rate = -1;
		 commonMove best = null;
		 commonMove salvage = null;
		 int nChildren = parent.getNoOfChildren();
		 double randomization = search.win_randomization;
		 if(randomization>1.0)
		 {	parent.sortBy(UCTNode.key.visits);
		 // select randomly among the top n choices
		 for(int i=0,lim=Math.min(nChildren,r.nextInt((int)randomization)+1);
				 i<lim;
				 i++)
		 {
			 commonMove child = parent.getChild(i);
						UCTNode node = child.uctNode();
			 if(salvage==null) { salvage = child; }
						if(node!=null && (node.getVisits()>=0))
						{
						double win = r.nextDouble();	// assign a random win rate
						if((best==null) || (win>rate)) 
						{
							best = child;
							rate = win;
						}}}
					}
				else
				{
				// add a random amount to the winrate of each node, select
				// the best among these.
				@SuppressWarnings("unused")
				int bestIndex = -1;
				for(int i=0,lim=nChildren; i<lim; i++)
				{
					commonMove child = parent.getChild(i);
					if(salvage==null) { salvage = child; }
					UCTNode node = child.uctNode();
					if(node!=null && (node.getVisits()>=0))
					{
					double win = node.getWinrate();
					if(r!=null) { win +=r.nextDouble()*randomization; }
					if((best==null) || (win>rate)) 
					{
						best = child;
						rate = win;
						bestIndex = i;
					}}}
				
				//if(bestIndex!=0) { G.print("randomized winner #"+(bestIndex+1)+" scale "+randomization); }
				
				}
		 		if(best==null)
		 		{	// if the root has no viable moves, it's because all the moves lead to direct losses
		 			// at this time, this bookkeeping result is only possible if TerminalNodeOptimization
		 			// is on
		 			if(board.nPlayers()<=2)
		 			{
		 			best = parent.getChild(0).Copy(null);
		 			best.op = MOVE_RESIGN;
		 			}
		 			else if(salvage!=null) 
		 				{ best = salvage; 
		 				}
		 		}
				return(best);
			}
	 public double Static_Evaluate_Uct_Move(commonMove mm,int current_depth,CommonDriver master)
	 {
		 return Static_Evaluate_Search_Move(mm,current_depth,master);
	 }
	 public double Static_Evaluate_Search_Move(commonMove mm,int current_depth,CommonDriver master)
	    {  
	     	Make_Move(mm);
	    	if(master.check_duplicate_digests)
		    	{ 
	    		// this is a debugging mode to detect duplicate moves by comparing digests
	    		BoardProtocol b = getBoard();
	    		long mydig = b.Digest();
		  		commonMove oldmm = master.allDigests.get(mydig);
		  		if(oldmm!=null)
		  		{ throw G.Error("move %s results in the same digest as %s",mm,oldmm);
		  		}
		  		else { master.allDigests.put(mydig,mm); }
		    	}
	    	// note we supply the player here, because it's tricky to determine
	    	// exactly when and if the "current player" flips.
	    	mm.set_depth_limited (commonMove.EStatus.EVALUATED);
	    	
	    	// static_evaluate_position may set depth_limited to if it is using a transposition table
	    	double val = Static_Evaluate_Position(mm);
	        mm.set_local_evaluation(val);
	        mm.setEvaluation(val);
	        mm.setGameover(Game_Over_P());
	        if(mm.depth_limited()==commonMove.EStatus.EVALUATED)
		        {
		        mm.set_depth_limited (mm.gameover()
		        		? commonMove.EStatus.DEPTH_LIMITED_GAMEOVER 			// gameover so search stops
		        		: (Depth_Limit(current_depth+1,master.max_depth)
		        				? commonMove.EStatus.DEPTH_LIMITED_SEARCH 		// search bottoms out
		        				: commonMove.EStatus.EVALUATED));		// search should continue
		        }
	        Unmake_Move(mm);
	        
	        return (val);
	    }
	 public void prepareForDescent(UCTMoveSearcher m) { }
	 
	 public void saveCurrentVariation(String file)
	 {		commonMove var = getCurrentVariation();
			commonCanvas v = (commonCanvas)viewer;
			CommonMoveStack vhist = v.History;
	  		CommonMoveStack newgame = new CommonMoveStack();
	  		commonMove lastMove = null;
	  		
			for(int i=0,lim=vhist.size();i<lim;i++)
			{	// copy the game before starting
				commonMove elem = vhist.elementAt(i).Copy(null);
				if(lastMove!=null) { lastMove.next = elem; }
				newgame.push(elem);
				lastMove = elem;
			}
			while(var!=null)
			{	// add the moves in this solution
				var.setIndex(lastMove.index()+1);
				lastMove.next = var;
				lastMove = var;
				newgame.push(var);
				var = var.best_move();
			}
	  		sgf_reader.sgf_save(file,v.save_game(newgame));
	  }
	 /** return true if the game is over.
	  * 
	  */
	 public boolean Game_Over_P()
	 {
		 return (board.GameOver());
	 }
	 public commonMove DoAlphaBetaFullMove()
	 {
		 throw G.Error("DoAlphaBetaFullMove Not implemented");
	 }
	 public commonMove DoMonteCarloFullMove()
	 {
		 throw G.Error("DoMonteCarloFullMove Not implemented");
	 }
	 /** search for a move on behalf of player p and report the result
	  * to the game.  This is called in the robot process, so the normal
	  * game UI is not encumbered by the search.
	  */
	  public commonMove DoFullMove()
	  {	if(MONTEBOT)
	  	{
	 	return(DoMonteCarloFullMove()); 
	  	}
	  	else
	  	{
	 	 return(DoAlphaBetaFullMove());
	  	}
	  }
	    /** return true if the search should be depth limited at this point.  current
	     * is the current search depth, max is the maximum you set for the search.
	     * You're free to stop the search earlier or let it continue longer, but usually
	     * it's best to conduct an entire search with the same depth.
	     * @param current the current depth
	     * @param max the declared maximum depth.
	     * 
	     */
	    public boolean Depth_Limit(int current, int max)
	    {	// for simple games where there is always one move per player per turn
	    	// current>=max is good enough.  For more complex games where there could
	    	// be several moves per turn, we have to keep track of the number of turn changes.
	    	// it's also possible to implement quiescence search by carefully adjusting when
	    	// this method returns true.
	        return(current>=max);
	   }
	    /**
	     * this is called as the robot begins the random playout phase of a MCTS search
	     * this is intended to be used to do bookkeeping and rearrangement related to 
	     * any hidden state in the game, which has to be concealed or re-randomized
	     */
	   public void startRandomDescent()
	   {
		   
	   }
	   /** for use by getCurrentVariation, return true of the current move
	    * should have a "done" inserted between previous and current
	    */
	   public boolean needDoneBetween(commonMove previous,commonMove current)
	   {
		   return (previous.player!=current.player);
	   }
	    
	   public void setGameover(commonMove m,boolean v) { m.setGameover(v); }
	   public void setEvaluation(commonMove m,double v) { m.setEvaluation(v); }
	   public void setEvaluations(commonMove m,double v) { m.setEvaluations(v); }
	   public void set_depth_limited(commonMove m,commonMove.EStatus v) { m.set_depth_limited(v); }
	    // ** TEMPORARILY RESTORED ***
	   // public boolean WinForPlayer(commonMove p)
	   // {	return(getBoard().WinForPlayer(p.player));
	   // }

}
