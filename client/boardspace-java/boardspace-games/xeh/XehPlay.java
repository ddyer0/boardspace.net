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
package xeh;

import static xeh.XehMovespec.MOVE_DROPB;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;
import online.search.neat.Genome;
import online.search.neat.GenomeEvaluator;
import online.search.neat.NeatEvaluator;
import online.search.neat.NetIo;
import online.search.neat.NodeGene;
import online.search.nn.CoordinateMap;
import online.search.nn.Layer;
import online.search.nn.Network;

class XehEvaluator implements GenomeEvaluator
{
	/**
	 * this experiment trains a static evaluator.  The network consists
	 * of a single output, and inputs that encode only the state of the
	 * board.
	 * 
	 * training consists of running a complete playthrough of the game,
	 * pitting evaluator1 against evaluator2, for all pairs of genenes
	 * in the genome, and scoring fitness as the percentage wins. 
	 * 
	 * For current parameters, this means running 198 games against
	 * the rest if the cohort; one as white and one as black against
	 * each other member
	 */
	Genome best = null;
	XehBoard gameBoard = null;
	XehBoard board = null;
	NeatEvaluator evaluator = null;
	XehViewer viewer = null;
	XehPlay robot = null;
	private HashMap<String,Genome>matchup = new HashMap<String,Genome>();
	private HashMap<String,Genome>oldMatchup = new HashMap<String,Genome>();
	double totalFitness = 0;
	int nFitness = 0;
	
	Genome target = null;
	public void startGeneration()
	{	totalFitness = 0;
		nFitness = 0;
		oldMatchup.clear();
		for(String k : matchup.keySet())
		{
			oldMatchup.put(k,matchup.get(k));
		}
		matchup.clear();
	}
	
	public void finishGeneration()
	{
		//G.print("Generation averate fitness "+(totalFitness/nFitness));
	}

	public Genome playThroughGame(Genome player1,Genome player2)
	{	String key = player1.toString()+"-"+player2.toString();

		Genome value = matchup.get(key);
		if(value==null) 
			{ value = oldMatchup.get(key); 
			  // a lot of matchups recur in the next generation
			  if(value!=null) 
			  	{ matchup.put(key,value);
			  	//G.print("reuse "+key); 
			  	}
			}
		if(value==null)
		{
		XehBoard testBoard = board.cloneBoard();
		if(testBoard.getState().Puzzle()) 
			{ testBoard.Execute(new XehMovespec("start p0",0),replayMode.Replay);
			}
		Genome players[]=new Genome[2];
		{
		int who = testBoard.whoseTurn();
		players[who] = player1;
		players[who^1] = player2;
		}
		robot.monitor = testBoard;
		while(!testBoard.gameOverNow())
		{
			makeMove(testBoard,players[testBoard.whoseTurn()]);
		}	
		viewer.repaint(1000);
		value = testBoard.WinForPlayer(0)
				? players[0]
				: players[1]; 	// black wins

		if(player1==target || player2==target)
		{
			G.print(player1," x ",player2," = ",value);
			G.print();
		}
		matchup.put(key,value);
		}
		return value;
	}
	public static double evaluatePosition(XehBoard testBoard,Genome player)
	{	
		loadBoardPosition(testBoard,player);
		player.evaluate();
		double val = player.getOutputs().get(0).getLastValue();
		return val;
	}
	
	public void makeMove(XehBoard testBoard,Genome player)
	{
		CommonMoveStack moves = testBoard.GetListOfMoves();
		XehMovespec bestMove = null;
		double bestValue = 0;
		for(commonMove m : moves)
		{	XehMovespec mm = (XehMovespec)m;
			testBoard.RobotExecute(mm);
			double val = evaluatePosition(testBoard,player);
			//G.print(mm,val);
			if(bestMove==null || val>bestValue) { bestValue = val; bestMove=mm; }
			testBoard.UnExecute(mm);
		}
		testBoard.RobotExecute(bestMove);
		//robot.monitor = testBoard;
		//viewer.repaint();
		//G.print(bestMove);
	}
	
	public static void loadBoardPosition(XehBoard b,Genome player)
	{
		int index=0;
		ArrayList<NodeGene> inputs = player.getInputs();
		int swap = XehChip.White==b.getCurrentPlayerChip() ? 1 : -1;
		for(XehCell c = b.allCells; c!=null; c=c.next,index++) 
		{
			NodeGene cc = inputs.get(index);
			XehChip top = c.topChip();
			// set the input values to white=1 black = -1, but swap if it's black's turn
			int invalue = top==null ? 0 : top==XehChip.White ? 1*swap : -1*swap;
			//G.print(cc,index,c,top,invalue);
			cc.setInputValue(invalue);
		}
	}
	public double evaluate(Genome g) 
	{	//if(g.getName().equals("item-24.genome"))
		//	{ target = g; 
		//	}
		Genome cohort[] = evaluator.getCohort();
		int wins = 0;
		int games = 0;
		for(Genome opponent : cohort)
		{	if(opponent != g)
			{
			Genome winFirst = playThroughGame(g,opponent);
			Genome winSecond = playThroughGame(opponent,g);
			//G.print(g," ",opponent," ",winFirst==g," ",winSecond==g);
			if(winSecond==g) { wins++; }
			if(winFirst==g) { wins++; }
			games+=2;
			}
		}
		// fitness is the win percentage
		double fitness = (double)wins/games;
		//G.print(g," ",fitness);
		totalFitness += fitness;	// average fitness for the entire cohort ought to be 0.5
		nFitness++;
		// slop adds a slight preference for smaller genomes
		double slop = 1.0/(g.getNodeGenes().size()+g.getConnectionGenes().size());
		return fitness+slop;
	}

	public void setBest(Genome g) {
		best = g;
	}
	public Genome createPrototypeNetwork()
	{	int cells = board.getCellArray().length;
		return Genome.createBlankNetwork(cells,1);
	}
}

/** 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * <p>
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * <p>
 * Notwithstanding the "only" above, debugging robot players can be very
 * difficult, both at the elementary level when the robot crashes out or
 * produces obviously wrong results, or at the advanced level when the robot
 * produces an undesirable result that is not blatantly wrong.
 * <p>
 * debugging aids:
 * <p>
 * <li>{@link #List_Of_Legal_Moves} should produce only legal moves, and should
 * by default produce all legal moves, but if your board class has some consistency
 * checking, errors constructing the move list might be detected. 
 * 
 * <li>Turn on the "start evaluator" action, and experiment with board positions
 * in puzzle mode.  Each new position will print the current evaluation.
 * 
 * <li>when the robot is stopped at a breakpoint (for example in {@link #Static_Evaluate_Position}
 * turn on the "show alternate board" option to visualize the board position.  It's usually
 * not a good idea to leave the option on when the robot is running because there will be
 * two threads using the data simultaneously, which is not expected.
 *
 * <li>turn on the save_digest and check_duplicate_digest flags.
 *
 ** <li>set {@link #verbose} to 1 or 2.  These produce relatively small amounts
 * of output that can be helpful understanding the progress of the search
 *
 ** <li>set a breakpoint at the exit of {@link #DoFullMove} and example the
 * top_level_moves variable of the search driver.  It contains a lot of information
 * about the search variations that were actually examined.
 *
 * <li>for a small search (shallow depth, few nodes) turn on {@link #SAVE_TREE}
 * and set a breakpoint at the exit of {@link #DoFullMove}
 * @author ddyer
 *
 */
public class XehPlay extends commonRobot<XehBoard> implements Runnable, XehConstants,
    RobotProtocol
    {
	// this is an internal value used to affect the search in several ways.  Normal "value of position" results
	// should be well below this in magnitude.  Searches are normally called off if the value of a position exceeds
	// this, indicating "we won".   It should be at least 2x any non-winning value the evaluator normally produces.
	// but it's exact value and scale are unimportant.  The main thing is to have a convenient range of values
	// for the evaluator to work with.
	static String TrainingData = "TrainingData";
	static Network level1Net = null;
	static Network level8Net = null;
	static boolean NEURO_MONTEBOT = true;
	static boolean LIVE_TRAINING = false;	// if true, train from live games
	
	UCTMoveSearcher monte_search_state = null;
	static Network teachNet = null; 
	static double[]lastNetworkValues = null;
	Network evalNet = null;
	
	boolean NEUROBOT = false;

	static final int DUMBOT_TRAINING_LEVEL = 100;
	static final int UCT_NEUROBOT_PLAY_LEVEL = 101;
	static final int UCT_NEUROBOT_TRAIN_LEVEL = 102;
	static final int TESTBOT_HYBRID_1 = 103;
	static final int TESTBOT_HYBRID_2 = 104;
	static final int OLD_SMARTBOT_LEVEL = 106;
	static final int NEUROBOT_HYBRID = 105;
	static final double VALUE_OF_WIN = 10000.0;
    boolean UCT_WIN_LOSS = false;
    
    boolean EXP_MONTEBOT = false;
    int timePerMove = 15;
    double ALPHA = 1.0;
    double BETA = 0.25;
    double winRateWeight = 0;
    double neuroWinRateWeight = 10;
    int minMovesPerSecond = 400000;
    int maxMovesPerSecond = 500000;
	Genome neurobotNetwork;
	Random randombotRandom = null;
	
    int storedChildLimit = UCTMoveSearcher.defaultStoredChildLimit;
    double NODE_EXPANSION_RATE = 1.0;
    double CHILD_SHARE = 0.5;				// aggressiveness of pruning "hopeless" children. 0.5 is normal 1.0 is very agressive
    boolean SAVE_TREE = false;				// debug flag for the search driver.  Uses lots of memory. Set a breakpoint after the search.
    int MAX_DEPTH = 5;						// search depth.
	static final boolean KILLER = false;	// if true, allow the killer heuristic in the search
	static final double GOOD_ENOUGH_VALUE = VALUE_OF_WIN;	// good enough to stop looking
	
	boolean STORED_CHILD_LIMIT_STOP = false;	// if true, stop the search when the child pool is exhausted.

    int Strategy = DUMBOT_LEVEL;
    
    int boardSearchLevel = 0;				// the current search depth
    XehChip movingForPlayer = null;
    boolean deadChildOptimization = true;	// remove children who are no longer in the running to be a winner
    /**
     *  Constructor, strategy corresponds to the robot skill level displayed in the lobby.
     * 
     *  */

    public XehPlay()
    {
    }
    // called at the top of each random descent
    public void Start_Simulation(UCTMoveSearcher ss,UCTNode n)
    {	switch(Strategy)
    	{
    	case OLD_SMARTBOT_LEVEL:
    		board.buildUCTtree(ss,n,BETA);
    		break;
    	default: break;
    	}
    }
    public void Finish_Simulation(UCTMoveSearcher ss,UCTNode n)
    {	switch(Strategy)
    	{
    	case OLD_SMARTBOT_LEVEL:
    		board.reclaimUCTtree( ss,n);
    		break;
    	default: break;
    	}
    }
    public RobotProtocol copyPlayer(String from)	// from is the thread name
    {	RobotProtocol c = super.copyPlayer(from);
    	XehPlay cc = (XehPlay)c;
    	cc.Strategy = Strategy;
    	cc.movingForPlayer = movingForPlayer; 
    	cc.deadChildOptimization = deadChildOptimization;
    	cc.evalNet = (evalNet==null) ? null : evalNet.duplicate();
    	cc.NEUROBOT = NEUROBOT;
   
    	return(c);
    }

    public BoardProtocol monitor = null;
    public BoardProtocol disB()
    {	if(monitor!=null) 
    		{ return(monitor); }
    	return(super.disB());
    }

/** Called from the search driver to undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence. This is usually the most troublesome 
 * method to implement - everything else in the board manipulations moves "forward".
 * Among other things, Unmake_Move will have to undo captures and restore captured
 * pieces to the board, remove newly placed pieces from the board and so on.  
 * <p>
 * Among the most useful methods; 
 * <li>use the move object and the Make_Move method
 * to store the information you will need to perform the unmove.
 * It's also standard to restore the current player, the current move number,
 * and the current board state from saved values rather than try to deduce
 * the correct inverse of these state changes.
 * <li>use stacks in the board class to keep track of changes you need to undo, and
 * record only the index into the stack in the move object.
 * 
 */
    public void Unmake_Move(commonMove m)
    {	XehMovespec mm = (XehMovespec)m;
        board.UnExecute(mm);
        boardSearchLevel--;
    }
/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   XehMovespec mm = (XehMovespec)m;
        board.RobotExecute(mm);
        boardSearchLevel++;
    }

/** return a Vector of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {
        CommonMoveStack m = board.GetListOfMoves();
        if(evalNet!=null)
        {
        double values[] = null;
        double wins[] = null;
        
        synchronized (this)
        {
        netCalculatePositive(evalNet,board);
        values = evalNet.getValues("OUT");
        wins = evalNet.getValues("WINRATE");
        }
        int size = m.size();
        for(int lim=size-1; lim>=0; lim--)
        	{
        		commonMove mm = m.elementAt(lim);
        		getEvaluation(evalNet,(XehMovespec)mm,values,wins);
        	}
        }
        return(m);
    }
    public void getEvaluation(Network net,XehMovespec mm,double[]values,double[]wins)
    {	int n = getMoveIndex(net,mm);
		mm.set_local_evaluation(values[n]);	// visit rate
		mm.setEvaluation(wins[n]*2-1);		// win rate, in net is 0-1, in node -1 to 1
    }
    public int getMoveIndex(Network n,XehMovespec mm)
    {	// swap is an out-of-band move which is only available as the second move of the game.
    	if(mm.op==MOVE_SWAP ) 
    		{ CoordinateMap map = n.getCoordinateMap();
    		  return(map.getMaxIndex()); 
    		}
    	return(getMoveIndex(n,mm.to_col,mm.to_row));
    }
    public int getMoveIndex(Network n,char col,int row)
    {	CoordinateMap map = n.getCoordinateMap();
    	int newway = map.getIndex(col,row);
    	//G.print("get "+col+row+" "+newway);
     	return(newway);
    }

    /**
    * this is the static evaluation used by Dumbot.  When testing a better
    * strategy, leave this untouched as a reference point.  The real meat
    * of the evaluation is the "blobs" class which is constructed by the
    * board.  The blobs structure aggregates the stones into chains and
    * keeps some track of how close the chains are to each other and to
    * the edges of the board.
    *
    * @param evboard
     * @param blobs
     * @param player
     * @param print
     * @return
     */
     double dumbotEval(XehBoard evboard,OStack<XehBlob> blobs,int player,boolean print)
    {	// note we don't need "player" here because the blobs variable
    	// contains all the information, and was calculated for the player
    	print = false;
    	double val = 0.0;
    	double ncols = evboard.ncols;
    	for(int i=0;i<blobs.size();i++)
    	{
    		XehBlob blob = blobs.elementAt(i);
    		int span = blob.span();
    		double spanv = span/ncols;
    		if(print) 
    		{ System.out.println(blob + " span "+span + " = " + (spanv*spanv));
    		}
    		val += spanv*spanv;		// basic metric is the square of the span
    	}
    	OStack<XehBlob> merged = XehBlob.mergeBlobs(blobs);
    	for(int i=0;i<merged.size();i++)
    	{
    		XehBlob blob = merged.elementAt(i);
    		int span = blob.span();
    		double spanv = span/ncols;
    		if(print) 
    		{ System.out.println(blob + " span "+span + " = " + (spanv*spanv));
    		}
   		val += spanv*spanv;		// basic metric is the square of the span
    	}	
    	return(val);
    }
     
    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    double ScoreForPlayer(XehBoard evboard,int player,boolean print)
    {	
    	
    	BlobStack blobs = new BlobStack();
		double val = 0.0;
		// is this a won position? If so that's the evaluation.
		// note that for some games, the current position might be a win
		// for the other player and that would have be be accounted for too.
     	boolean win = evboard.winForPlayerNow(player,blobs);
 
     	// make wins in fewer moves look slightly better. Nothing else matters.
     	// note that without this little tweak, the robot might appear to "give up"
     	// when a loss is inevitable a few moves down the road, and take an unnecessary
     	// loss now rather than prolonging the game.
     	if(win) 
     		{ val = VALUE_OF_WIN+(1.0/(1+boardSearchLevel));
     		  if(print) {System.out.println(" win = "+val); }
     		  return(val); 
     		}
     	
     	// if the position is not a win, then estimate the value of the position
    	switch(Strategy)
    	{	default: throw G.Error("Not expecting strategy %s",Strategy);
    		case NEUROBOT_LEVEL:
    		case TESTBOT_LEVEL_1:
    		case TESTBOT_LEVEL_2:
    			val = XehEvaluator.evaluatePosition(board,neurobotNetwork);
    			break;
     		case DUMBOT_LEVEL: 
    		case DUMBOT_TRAINING_LEVEL:
    		case SMARTBOT_LEVEL:
    			// betterEval based on "two lines" developed by Adam Shepherd, Oct 2009
      			val = dumbotEval(evboard,blobs,player,print);
       	  		break;
    		case RANDOMBOT_LEVEL:
    			return 0;
    		case BESTBOT_LEVEL: 	// both the same for now
    		case MONTEBOT_LEVEL:
    			// this is the old dumbot based on connections
       			val = dumbotEval(evboard,blobs,player,print);
       			break;
     	}
    	// we're going to subtract two values, and the result must be inside the
    	// bounds defined by +-WIN
    	G.Assert((val<(VALUE_OF_WIN/2))&&(val>=(VALUE_OF_WIN/-2)),"value out of range");
     	return(val);
    }

    /**
     * this re-evaluates the current position from the viewpoint of forplayer.
     * for 2 player games this is to trivially negate the value, but for multiplayer
     * games it requires considering multiple player's values.
     */
    public double reScorePosition(commonMove m,int forplayer)
    {	return(m.reScorePosition(forplayer,VALUE_OF_WIN));
    }
    /** this is called from the search driver to evaluate a particular position. The driver
     * calls List_of_Legal_Moves, then calls Make_Move/Static_Evaluate_Position/UnMake_Move
     *  for each and sorts the result to preorder the tree for further evaluation
     */
    public double Static_Evaluate_Position(	commonMove m)
    {	int playerindex = m.player;
    	switch(Strategy)
    	{
    	case TESTBOT_LEVEL_2:
    	case TESTBOT_LEVEL_1:
    	case NEUROBOT_LEVEL:
    		return XehEvaluator.evaluatePosition(board,neurobotNetwork);
    	case RANDOMBOT_LEVEL:
    		return randombotRandom.nextDouble();
    	case PURE_NEUROBOT_LEVEL:
		case TESTBOT_HYBRID_2:
		case TESTBOT_HYBRID_1:
		case SMARTBOT_LEVEL:
    		{
    		// run an alpha-beta robot using the learned static evaluator
    		netCalculatePositive(evalNet,board);
    		double vals[] = evalNet.getValues();
    		// cross check against the learned network
    		//netCalculate(level1Net,m);
    		//double vals1[] = level1Net.getValues();
    		double val0 = vals[0];
    		double val1 = vals[1];
            if(val0>=VALUE_OF_WIN) { return(val0); }
            if(val1>=VALUE_OF_WIN) { return(-val1); }
            return(val0-val1);
    		}
    	default:
    	{
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,nextPlayer[playerindex],false);
        // don't dilute the value of wins with the opponent's positional score.
        // this avoids the various problems such as the robot comitting suicide
        // because it's going to lose anyway, and the position looks better than
        // if the oppoenent makes the last move.  Technically, this isn't needed
        // if there is no such thing as a suicide move, but the logic
        // is included here because this is supposed to be an example.
        if(val0>=VALUE_OF_WIN) { return(val0); }
        if(val1>=VALUE_OF_WIN) { return(-val1); }
        if(level1Net!=null)
        {
        	teachNetwork(level1Net,m,val0,val1);
        	
        }
        return(val0-val1);
    	}}
    }
    int seq = 0;
    public double WhiteValue = 1;
    public double BlackValue = -1;
    public boolean recordPermutations = false;
 

    public double[] netCalculatePositive(Network n,XehBoard b)
    {	int size = n.inputNetworkSize();
    	double inE[] = new double[size];
    	double inB[] = new double[size];
    	double inW[] = new double[size];
     	for(XehCell c = b.allCells; c!=null; c=c.next)
        	{ XehChip chip = c.topChip();
        	  int idx = getMoveIndex(n,c.col,c.row);
        	  if(chip==null) { inE[idx] = 1; }
        	  else {
        		  switch(chip.id)
        		  {
        		  case Black_Chip_Pool:
         			  inB[idx] = 1;
        			  break;
        		  case White_Chip_Pool:
        			  inW[idx] = 1;
        			  break;
        		  default: G.Error("Not expecting ",chip);
        		  }
        	  }
        	}

    	Layer mainIn = n.getLayer("INE");
    	Layer blackIn = n.getLayer("INB");
    	Layer whiteIn = n.getLayer("INW");
    	Layer colorIn = n.getLayer("INC");
    	int who = b.whoseTurn();
    	char co = b.getPlayerChar(who);
    	double color[] = new double[] { (co=='W')?1:0};
      	return(n.calculateValues(mainIn,inE,blackIn,inB,whiteIn,inW,colorIn,color));
    }

    public void printInputVector(StringBuilder trainingData)
    {
    	if(trainingData!=null)
    	{
    		trainingData.append(board.whoseTurn);
    		trainingData.append(" ");
    		trainingData.append(board.getPlayerChip(board.whoseTurn).id.shortName);
    		for(XehCell c = board.allCells; c!=null; c=c.next)
    		{
    			XehChip top = c.topChip();
    			if(top!=null) 
    			{ trainingData.append(" ");
    			  trainingData.append(c.col);
    			  trainingData.append(" ");
    			  trainingData.append(c.row);
    			  trainingData.append(" ");
    			  trainingData.append(top.id.shortName); 
    			}
    		}
    		trainingData.append("\n");
    	}
    }
    public void printTrainingVector(StringBuilder trainingData,UCTMoveSearcher monte)
    {
    	if(trainingData!=null)
    	{
    		UCTNode root = monte.root;
    		int nChildren = root.getNoOfChildren();
    		double rootvisits = root.getVisits();
    		for(int lim=nChildren-1; lim>=0; lim--)
    		{
    			XehMovespec mm = (XehMovespec)root.getChild(lim);
    			UCTNode no = mm.uctNode();
    			if(no!=null)
    			{
    			double visits = no.getVisits()/rootvisits;
    			trainingData.append(" ");
    			if(mm.op==MOVE_SWAP) { trainingData.append(SWAP); }
    			else { 	trainingData.append(mm.to_col);
    					trainingData.append(" ");
    					trainingData.append(mm.to_row);
    			}
    			trainingData.append(" ");
    			trainingData.append(visits);
    			trainingData.append(",");
    			trainingData.append(no.getWinrate());
    			}
    		}

    		trainingData.append("\n");
    	}
    }
	public void saveTrainingData(StringBuilder trainingData,String file,String comment) 
	{
		if(trainingData!=null)
		{
		OutputStream s=null;
		try {
			s = new FileOutputStream(new File(file));
			if(s!=null)
				{PrintStream ps = new PrintStream(s);
				ps.println("//VERSION 1");
				ps.println("//"+comment);
				ps.print(trainingData.toString());
				ps.close();
				s.close();
				}
		} catch (IOException e) {
			G.Error("Save training data error",e);
		}}
	}

    double rmsTot = 0;
    int rmsN = 0;
    void teachMonteNetwork(Network n,UCTMoveSearcher monte)
    {	
    	if(n!=null)
    	{
    	if(LIVE_TRAINING)
    	{
    	double rmsv;
    	synchronized(n)
    	{	
    		double values[] =netCalculatePositive(n,board); 

     		UCTNode root = monte.root;
    		int nChildren = root.getNoOfChildren();
    		double rootvisits = root.getVisits();
    		double rms = 0.0;
    		for(int lim=nChildren-1; lim>=0; lim--)
    		{
    			XehMovespec mm = (XehMovespec)root.getChild(lim);
    			UCTNode no = mm.uctNode();
    			if(no!=null)
    			{
    			int idx = getMoveIndex(n,mm);
    			double visits = no.getVisits()/rootvisits;
    			double oldv = values[idx];
    			double err = visits-oldv;
    			//G.print("M "+mm+" "+err+" "+oldv);
    			rms += err*err;
    			G.Assert(visits>=0,"impossible visits, is %s",visits);
    			values[idx] = visits;
    			}
    		}
    		rmsv = Math.sqrt(rms/nChildren);
    		if(board.moveNumber<12)
    		{	rmsTot += rmsv;
    			rmsN ++;
    		}
    		//((GenericNetwork)n).learningRate = 0.015;
    		n.learn(values);
    		if((board.moveNumber==1) && rmsN>0)
    		{	G.print(""+seq+" RMS average "+rmsTot/rmsN+" "+rmsv);
    			rmsTot = 0;
    			rmsN = 0;
    			seq++;
    		}
     		}
    	}
    		//G.print("Move "+board.moveNumber+" Rms "+rmsv);
    		//n.dumpWeights(true);
    	}
    }
    public void printTrainingData(StringBuilder trainingData,UCTMoveSearcher monte)
    {
		printInputVector(trainingData);
		printTrainingVector(trainingData,monte);
	
    }
    // teach the eval network for alpha beta processing
    void teachNetwork(Network n,commonMove m,double... values)
    {	double outVals[] =  netCalculatePositive(n,board); 
        //((GenericNetwork)n).learningRate = 0.1;
    	double error = 0;
    	double sum = 0;
    	for(int i=0;i<outVals.length;i++) { sum += outVals[i]; double e = outVals[i]-values[i]; error += Math.abs(e); }
    	n.learn(values);
     	//rule.updateNeuronWeights
    	//rule.updateNeuronWeights(nn[0]);
    	//rule.updateNetworkWeights(errorv); 
    	if((seq++ % 1000000)==0)
    	{
    	 int err = (int)(error*100/sum);
    	 G.print("Rule error ",err,"% ",outVals[0]," ",outVals[1]," ",error);
    	 
    	}
    }
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {
            XehBoard evboard = GameBoard.cloneBoard();
            double val0 = ScoreForPlayer(evboard,FIRST_PLAYER_INDEX,true);
            double val1 = ScoreForPlayer(evboard,SECOND_PLAYER_INDEX,true);
            System.out.println("Eval is "+ val0 +" "+val1+ " = " + (val0-val1));
    }
    enum TeachMode { Net, Trained, Live }
    TeachMode mode = TeachMode.Net;
    
    /**
     * this is the tap point for real time display of "goodness" as shading on
     * the empty spaces of the board.
     * @param c
     * @return
     */
    Genome guiplayer = null;
    
    public Hashtable<XehCell,Double>getEvaluations(XehBoard b)
    {
    	if(b.getState()!=XehState.Gameover)
    	{	Hashtable<XehCell,Double>evals=new Hashtable<XehCell,Double>();
    		CommonMoveStack moves =b.GetListOfMoves();
    		for(commonMove m : moves)
    		{	if(m.op==MOVE_DROPB)
    			{
    			XehMovespec mm = (XehMovespec)m;
    			XehCell c = b.getCell(mm.to_col,mm.to_row);
    			board.RobotExecute(mm);
    			double v = Static_Evaluate_Position(mm);
    			evals.put(c,v);
    			board.UnExecute(mm);
    			}
    		}
    		return evals;
    	}
    	return null;
    }
    public double getNeuroEval(XehCell c,XehBoard b)
    {	
    	double val = 0;
    	switch(Strategy)
    	{
    	case NEUROBOT_LEVEL:
    	case TESTBOT_LEVEL_1:
    	case TESTBOT_LEVEL_2:
    		{
    		if(guiplayer==null) {  guiplayer = new Genome(neurobotNetwork); }
    		int who = b.whoseTurn();
    		XehMovespec mv = new XehMovespec(MOVE_DROPB,c.col,c.row,b.getPlayerColor(who),who);
    		b.RobotExecute(mv);
    		val = XehEvaluator.evaluatePosition(b,guiplayer)/4;
    		b.UnExecute(mv);
    		}
    		break;
    	default: break;
    	}
    	return(val);
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
        GameBoard = (XehBoard) gboard;
        board = GameBoard.cloneBoard();
        // strategy with be 0,1,2 for Dumbot, Smartbot, Bestbot
        Strategy = strategy;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy %s",strategy);
        case -100:	// old dumbot, before shift in pruning and randomization 
        	MONTEBOT = DEPLOY_MONTEBOT; 
        	break;
        case OLD_SMARTBOT_LEVEL:
        	MONTEBOT=DEPLOY_MONTEBOT;
        	NODE_EXPANSION_RATE = 0.25;
        	ALPHA = 1.0;
        	name = "Smartbot";
         	break;
        case WEAKBOT_LEVEL:
        	WEAKBOT = true;
        	minMovesPerSecond = 15000;
			//$FALL-THROUGH$
		case DUMBOT_LEVEL:
           	MONTEBOT=DEPLOY_MONTEBOT;
           	name = WEAKBOT ? "WeakBot" : "Dumbot";
        	ALPHA = 0.5;
        	BETA = 0.25;
        	CHILD_SHARE = 0.85;
        	verbose=1;
        	break;
		case TESTBOT_HYBRID_2:
			break;
		case RANDOMBOT_LEVEL:
			randombotRandom = new Random();
			MAX_DEPTH = 2;
			break;
			
		case NEUROBOT_LEVEL:		// neat neuralbot with no lookahead
			MONTEBOT = false;
			MAX_DEPTH = 2;
			verbose = 3;
			RANDOMIZE = false;
			G.print("neat bot with no lookahead");
			neurobotNetwork = (Genome)NetIo.load(neatest);
			guiplayer = null;
			break;

		case SMARTBOT_LEVEL:
			name = "SMARTBOT";
			//$FALL-THROUGH$
		case TESTBOT_HYBRID_1:
		
		case UCT_NEUROBOT_PLAY_LEVEL:

		case DUMBOT_TRAINING_LEVEL:
			MONTEBOT=true;
			name = "dumbot_train";
        	verbose = 1;
        	deadChildOptimization = false;
        	storedChildLimit = 2*UCTMoveSearcher.defaultStoredChildLimit;
        	winRateWeight = 0;
        	ALPHA = 0.5;
        	BETA = 0.25;
        	CHILD_SHARE = 0.85;
			break;
		case NEUROBOT_HYBRID:
			name = "Neurobot_hybrid";
			//$FALL-THROUGH$
		case TESTBOT_LEVEL_1:
         	MONTEBOT=DEPLOY_MONTEBOT;
           	name = WEAKBOT ? "WeakBot" : "Weak MonteBot";
           	timePerMove = 1;
        	verbose=1;
        	ALPHA = 0.5;
        	BETA = 0.25;
        	deadChildOptimization = false;
        	CHILD_SHARE = 0.85;
        	break;

		case TESTBOT_LEVEL_2:	// neat neurobot with 3 ply lookahead
			MONTEBOT = false;
			MAX_DEPTH = 4;
			verbose = 1;
			RANDOMIZE = false;
			G.print("neat bot with 3 ply lookahead");
			neurobotNetwork = (Genome)NetIo.load(neatest);
			guiplayer = null;
			break;

        case MONTEBOT_LEVEL:
          	MONTEBOT=DEPLOY_MONTEBOT;
           	name = WEAKBOT ? "WeakBot" : "MonteBot";
           	timePerMove = 30;
        	verbose=1;
        	ALPHA = 0.5;
        	BETA = 0.25;
        	deadChildOptimization = false;
        	CHILD_SHARE = 0.85;
        	break;
        }
     }


 
 /**
  * this is needed to complete initialization of cloned robots
  */
// public void copyFrom(commonRobot<HavannahBoard> p)
// {	super.copyFrom(p);
	// perform any additional copying not handled by the standard method
// }
 
 /**
  * breakpoint or otherwise override this method to intercept search events.
  * This is a low level way of getting control in the middle of a search for
  * debugging purposes.
  */
//public void Search_Break(String msg)
//{	super.Search_Break(msg);
//}
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
    board.initRobotValues();
    movingForPlayer = GameBoard.getCurrentPlayerChip();
}

 public commonMove DoAlphaBetaFullMove()
 {
        XehMovespec move = null;
        try
        {
       	
            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new XehMovespec("Done", board.whoseTurn);
            }

            // it's important that the robot randomize the first few moves a little bit.
            int randomn = RANDOMIZE
            				? ((board.moveNumber <= 6) ? (14 - 2*board.moveNumber) : 0) 
            				: 0;
            boardSearchLevel = 0;

            int depth = MAX_DEPTH;	// search depth
            double dif = 0.0;		// stop randomizing if the value drops this much
            // if the "dif" and "randomn" arguments to Find_Static_Best_Move
            // are both > 0, then alpha-beta will be disabled to avoid randomly
            // picking moves whose value is uncertain due to cutoffs.  This makes
            // the search MUCH slower so depth ought to be limited
            // if ((randomn>0)&&(dif>0.0)) { depth--; }
            // for games where there are no "fools mate" type situations
            // the best solution is to use dif=0.0;  For games with fools mates,
            // set dif so the really bad choices will be avoided
            Search_Driver search_state = Setup_For_Search(depth, false);
            search_state.save_all_variations = SAVE_TREE;
            search_state.good_enough_to_quit = GOOD_ENOUGH_VALUE;
            search_state.verbose = verbose;
            search_state.allow_killer = KILLER;
            search_state.allow_best_killer = false;
            search_state.save_top_digest = true;	// always on as a background check
            search_state.save_digest=false;	// debugging only
            search_state.check_duplicate_digests = false; 	// debugging only

           if (move == null)
            {	// randomn takes the a random element among the first N
            	// to provide variability.  The second parameter is how
            	// large a drop in the expectation to accept.  For some games this
            	// doesn't really matter, but some games have disastrous
            	// opening moves that we wouldn't want to choose randomly
                move = (XehMovespec) search_state.Find_Static_Best_Move(randomn,dif);
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
 
 public commonMove getUCTRandomMove(Random rand)
 {	// the normal training for a uct network is that all values are in 0-1.0 and
	// the sum of all values should be 1.0.  Of course, depending on the actual
	// training anything is possible.
	double values[] =  netCalculatePositive(evalNet,board); 
	double vtotal = evalNet.getTotalValues();			// get the total of all values[]
 	CellStack empty = board.emptyCells;
 	int who = board.whoseTurn;
 	int sz = empty.size()-1;
 	double rval = rand.nextDouble();	// the random probability we want to select
 	double target = vtotal * rval;		// the expected sum of values that will make the target
 	
 	// on the first pass, tot up the values, and note the point at which
 	// we reach the desired sum.
 	XehCell selected = null;
 	int selectedIndex = 0;
 	double selectedTotal = 0;
 	double total = 0;
  	// we expect the total of all outputs to be approximately 1.0
 	for(int lim=0;lim<=sz;lim++)
 	{	XehCell c = empty.elementAt(lim);
 		int ind = getMoveIndex(evalNet,c.col,c.row);
 		if(total<target) 
 			{ selected = c; 
 			  selectedIndex = lim;
 			  selectedTotal = total;
 			}
 		total += values[ind];
 	}
	//
 	// depending on the actual network, we may have overshot or undershot the value
 	target = rval*total;
 	if(selectedTotal>target)
 	{	// back up
 		do {
 			selectedIndex--;
 			XehCell c = empty.elementAt(selectedIndex);
 			int ind = getMoveIndex(evalNet,c.col,c.row);
 			selectedTotal -= values[ind];
 			selected = c;
 		}
 		while(selectedTotal>target && selectedIndex>=0);
 	}
 	else if(selectedTotal<target)
 	{	// continue on
 		do {
 			XehCell c = empty.elementAt(selectedIndex);
 			int ind = getMoveIndex(evalNet,c.col,c.row);
 			selectedTotal += values[ind];
 			selected = c;
  			selectedIndex++;
 		} while(selectedTotal<target && selectedIndex<=sz);
 		if((selectedTotal<target) && (board.getState()==XehState.PlayOrSwap) )
 		{
 			return(new XehMovespec("swap",who));
 		}
 	}

 	return(new XehMovespec(MOVE_DROPB,selected.col,selected.row,board.getPlayerColor(who),who));
 }
 /** this verifies that the above is actually producing the desired percentages */
 /*
 public void getUCTRandomMoveTester(Random rand)
 {	double values[] = netCalculate(evalNet,board);
	int counts[] = new int[values.length];
 	int who = board.whoseTurn;
	boolean swap = board.getPlayerChip(who)==XehChip.Black;
	for(int i=0;i<10000;i++)
	{
		XehMovespec m = (XehMovespec)getUCTRandomMove(rand);
		int ind = getMoveIndex(evalNet,m.to_col,m.to_row,swap);
		counts[ind]++;
	}
	double sum = 0;
	for(int i=0;i<counts.length;i++) { if(counts[i]>0) { sum += values[i]; }}
	for(int i=0;i<counts.length;i++) 
	{	if(counts[i]>0) { G.print(""+counts[i]/100," "+(values[i]*100/sum)); }
	}
	G.print("");
 }
*/


 /**
 * get a random move by selecting a random one from the full list.
  * for games which have trivial move generators, this is "only" a factor of 2 or so improvement
  * in the playout rate.  For games with more complex move generators, it can by much more.
  * Diagonal-Blocks sped up by 10x 
  * 
  */
 public commonMove Get_Random_Move(Random rand)
 {	
	 switch(Strategy)
	 {
	 case RANDOMBOT_LEVEL:
		 return board.Get_Random_Hex_Move(rand);
	 case TESTBOT_HYBRID_1:
	 case TESTBOT_HYBRID_2:
	 case SMARTBOT_LEVEL:
	 case DUMBOT_LEVEL:
	 case TESTBOT_LEVEL_2:
	 case TESTBOT_LEVEL_1:
	 case NEUROBOT_HYBRID:
	 case MONTEBOT_LEVEL:
		 G.p1("using smart random hex moves");
		 return(board.Get_Localrandom_Hex_Move_M(rand));
	 case DUMBOT_TRAINING_LEVEL:
		 G.p1("using dumb random hex moves");
		 return board.Get_Random_Hex_Move(rand);
	 case OLD_SMARTBOT_LEVEL:
	 case UCT_NEUROBOT_PLAY_LEVEL:
	 case UCT_NEUROBOT_TRAIN_LEVEL:
		 G.p1("using UCTRandom hex moves");
		 return(board.getUCTRandomMove(rand));
		 
	 default:
			throw G.Error("No defined random move strategy");
	 }
	 
	 }

 // when initializing the UCT tree with predefined visit counts, based
 // on neural net readouts
 public void setInitialWinRate(UCTNode node,int visits,commonMove m,commonMove mm[]) 
 { 	
	switch(Strategy)
	{
	default: G.Error("Not expected");
		break;
	case NEUROBOT_HYBRID:
	case TESTBOT_LEVEL_1:
	case TESTBOT_LEVEL_2:
	case TESTBOT_HYBRID_2:
	case TESTBOT_HYBRID_1:
	case SMARTBOT_LEVEL:
		double evis = m.local_evaluation()*visits;
		double ewin = m.evaluation()*evis;
	 	node.setBiasVisits(ewin,evis);
	}
 }
 public String trainingString()
 {
	 switch(Strategy)
	 {
	 default: return("unknown training method");
	 case NEUROBOT_HYBRID:
	 case TESTBOT_LEVEL_1:
	 case TESTBOT_LEVEL_2:
	 case TESTBOT_HYBRID_1:
	 case TESTBOT_HYBRID_2:

		 return(teachNet.getInfo());
	 }
 }
	public commonMove getCurrentVariation()
	{	
		return getCurrent2PVariation();
	}
 
 // this is the monte carlo robot, which for some games is much better then the alpha-beta robot
 // for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
 // evaluator other than winning a game.
 public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
 	UCT_WIN_LOSS = EXP_MONTEBOT;
 	try {
       if (board.DoneState())
        { // avoid problems with gameover by just supplying a done
            move = new XehMovespec("Done", board.whoseTurn);
        }
        else 
        {
         	// this is a test for the randomness of the random move selection.
         	// "true" tests the standard slow algorithm
         	// "false" tests the accelerated random move selection
         	// the target value is 5 (5% of distributions outside the 5% probability range).
         	// this can't be left in the production applet because the actual chi-squared test
         	// isn't part of the standard kit.
        	// also, in order for this to work, the MoveSpec class has to implement equals and hashCode
         	//RandomMoveQA qa = new RandomMoveQA();
         	//qa.runTest(this, new Random(),100,false);
         	//qa.report();
         	
        // it's important that the robot randomize the first few moves a little bit.
        double randomn = (RANDOMIZE && (board.moveNumber <= 4))
        						? 0.1/board.moveNumber
        						: 0.0;
        verbose = 0;
        monte_search_state = new UCTMoveSearcher(this);
        monte_search_state.save_top_digest = true;	// always on as a background check
        monte_search_state.save_digest=false;	// debugging only
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.timePerMove = timePerMove;		// seconds per move
        monte_search_state.verbose = verbose;
        monte_search_state.alpha = ALPHA;
        monte_search_state.blitz = false;			// for hex, blitz is 2/3 the speed of normal unwinds
        monte_search_state.sort_moves = winRateWeight>0;
        monte_search_state.initialWinRateWeight = winRateWeight;
        monte_search_state.only_child_optimization = true;
        G.p1("Dead Child Optimization "+deadChildOptimization);
        monte_search_state.dead_child_optimization = deadChildOptimization;
        monte_search_state.simulationsPerNode = 1;
        monte_search_state.killHopelessChildrenShare = CHILD_SHARE;
        monte_search_state.final_depth = 9999;		// probably not needed for games which are always finite
        monte_search_state.node_expansion_rate = NODE_EXPANSION_RATE;
        monte_search_state.stored_child_limit = storedChildLimit;
        monte_search_state.randomize_uct_children = true;     
        monte_search_state.maxThreads = DEPLOY_THREADS;
        monte_search_state.random_moves_per_second = minMovesPerSecond;		// 
        monte_search_state.max_random_moves_per_second = maxMovesPerSecond;		// 
        // for some games, the child pool is exhausted very quickly, but the results
        // still get better the longer you search.  Other games may work better
        // the other way.
        monte_search_state.stored_child_limit_stop = STORED_CHILD_LIMIT_STOP;
        move = monte_search_state.getBestMonteMove();
        if(move!=null && board.moveNumber>board.ncols)
        {
        	UCTNode node = move.uctNode();
        	if(node!=null)
        	{
        	double winrate = node.getWinrate();
        	if(winrate<-0.75)
        	{	
            	G.print("resign "+board.moveNumber()+" "+move+" win "+winrate);
        		move = new XehMovespec(RESIGN,board.whoseTurn);  
        	}
        	}
        	
        }
        }
 		}
      finally { ; }
      if(move==null) { continuous = false; }
      //G.print("M "+move);
     return(move);
 }
 /**
  * for UCT search, return the normalized value of the game, with a penalty
  * for longer games so we try to win in as few moves as possible.  Values
  * must be normalized to -1.0 to 1.0
  */
 public double NormalizedScore(commonMove lastMove)
 {	int player = lastMove.player;
 	boolean win = board.hasWinningPath(player);
 	if(win) { return(UCT_WIN_LOSS? 1.0 : 0.8+0.2/boardSearchLevel); }
 	boolean win2 = board.hasWinningPath(nextPlayer[player]);
 	if(win2) { return(- (UCT_WIN_LOSS?1.0:(0.8+0.2/boardSearchLevel))); }
 	return(0);
 }
 
 
 public commonMove DoNeuroFullMove()
 {
	 commonMove m = MONTEBOT
			 		? DoMonteCarloFullMove() 
			 		: DoAlphaBetaFullMove();
	 if((evalNet!=null) && (board.moveNumber==1) && LIVE_TRAINING)
	 {	// apply learning based on the final tree
		synchronized(evalNet)
		{
     	int ncells = board.nCells();
     	int nlayers = evalNet.getLayers().length;
     	evalNet.saveNetwork("G:/temp/nn/"+evalNet.getName()+"-"+nlayers+"-"+ncells+".txt","Network at "+seq);
		evalNet.saveNetwork("G:/temp/nn/"+evalNet.getName()+"1-"+nlayers+"-"+ncells+".txt","Network at "+seq);
		}
	 }
	 if(level1Net!=null)  {
		 int ncells = board.getCellArray().length;
		 level1Net.saveNetwork("G:/temp/nn/"+level1Net.getName()+"-"+ncells+".txt","Network at "+seq); }
	 return(m);
 }
 /** search for a move on behalf of player p and report the result
  * to the game.  This is called in the robot process, so the normal
  * game UI is not encumbered by the search.
  */
  public commonMove DoFullMove()
  {	if(NEUROBOT)
    {
	  return(DoNeuroFullMove());
    }
    else if(MONTEBOT)
  	{
 	return(DoMonteCarloFullMove()); 
  	}
  	else
  	{
 	 return(DoAlphaBetaFullMove());
  	}
  }
  
  public double Static_Evaluate_Search_Move(commonMove mm,int current_depth,CommonDriver master)
  {		
	  
	if(NEUROBOT && MONTEBOT)
  		{
		return(mm.local_evaluation());	// evaluation already set by getlistofmoves
  		}

  	return(super.Static_Evaluate_Search_Move(mm,current_depth, master)); 
  	
  }

public void stopTraining()
{
	evaluator.exitRequest = true;
}
NeatEvaluator evaluator = null;
XehEvaluator trainer = null;
String neatest = "G:/share/projects/boardspace-java/boardspace-games/xeh/data/generation 3015.genome";

public void runRobotTraining(final ViewerProtocol v,BoardProtocol b,final SimpleRobotProtocol otherBot)
{	GameBoard = (XehBoard) b;
	trainer = new XehEvaluator();
	trainer.gameBoard = (XehBoard)b;
	trainer.board = (XehBoard)b.cloneBoard();
	evaluator = new NeatEvaluator("g:/temp/nn/train/",
				100,trainer,
				"g:/temp/nn/train/checkpoint.txt");
	trainer.evaluator = evaluator;
	trainer.viewer = (XehViewer)v;
	trainer.robot = this;
	
	new Thread(new Runnable() {
		public void run() 
		{ 
			evaluator.runEvaluation(10000);
		}
	}).start();
}
}
