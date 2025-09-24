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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.game.sgf.sgf_game;
import online.game.sgf.sgf_node;
import online.game.sgf.export.sgf_names;
import online.game.sgf.export.sgf_names.Where;
import online.search.*;
import online.search.nn.CoordinateMap;
import online.search.nn.GenericNetwork;
import online.search.nn.Layer;
import online.search.nn.Network;

class NNTrainingData
{
	String file;
	int moveNumber;
	int playerToMove;
	char color;
	boolean[] hasInputs;
	double[] inputs;
	double[] inputs2;
	double[] inputs3;
	double [] values;
	double [] winrates;
}
class TrainingData
{
	String file;
	int moveNumber;
	int playerToMove;
	char color;
	boolean[] hasInputs;
	double[] inputs;
	double[] inputs2;
	double[] inputs3;
	double [] values;
	double [] winrates;
}
class TDstack extends OStack<TrainingData>
{
	public TrainingData[] newComponentArray(int sz) {
		return(new TrainingData[sz]);
	}
}

class NNTDstack extends OStack<TrainingData>
{
	public TrainingData[] newComponentArray(int sz) {
		return(new TrainingData[sz]);
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
public class XehPlayNN extends commonRobot<XehBoard> implements Runnable, XehConstants,
    RobotProtocol
    {
	// this is an internal value used to affect the search in several ways.  Normal "value of position" results
	// should be well below this in magnitude.  Searches are normally called off if the value of a position exceeds
	// this, indicating "we won".   It should be at least 2x any non-winning value the evaluator normally produces.
	// but it's exact value and scale are unimportant.  The main thing is to have a convenient range of values
	// for the evaluator to work with.
	static String TrainingData = "NNTrainingData";
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

    public XehPlayNN()
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
    	XehPlayNN cc = (XehPlayNN)c;
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
    		case TESTBOT_LEVEL_1:
    		case TESTBOT_LEVEL_2:
    		case TESTBOT_HYBRID_1:
    		case TESTBOT_HYBRID_2:
    		case DUMBOT_LEVEL: 
    		case DUMBOT_TRAINING_LEVEL:
    		case PURE_NEUROBOT_LEVEL:
    		case NEUROBOT_HYBRID:
    		case NEUROBOT_LEVEL:
    		case SMARTBOT_LEVEL:
    			// betterEval based on "two lines" developed by Adam Shepherd, Oct 2009
      			val = dumbotEval(evboard,blobs,player,print);
       	  		break;
    		case OLD_SMARTBOT_LEVEL: 
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
    // TODO: refactor static eval so GameOver is checked first
    public double Static_Evaluate_Position(	commonMove m)
    {	int playerindex = m.player;
    	switch(Strategy)
    	{
    	case PURE_NEUROBOT_LEVEL:
		case TESTBOT_HYBRID_2:
		case TESTBOT_HYBRID_1:
    	case TESTBOT_LEVEL_2:
    	case TESTBOT_LEVEL_1:
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
    public double getNeuroEval(XehCell c)
    {	mode = TeachMode.Net;
    	UCTMoveSearcher ss = monte_search_state;
    	if(robotRunning && ss!=null)
    	{
    		UCTNode root = ss.root;
    		if(root!=null)
    			{ commonMove children[] = root.cloneChildren();
    			  if(children!=null)
    			  {	
    				  for(commonMove child0 : children)
    				  {	  XehMovespec child = (XehMovespec)child0;
    					  if(c.col==child.to_col
    						  && c.row==child.to_row
    						  )
    					  {	
    						 UCTNode node = child.uctNode();
     						 if(node!=null)
     						 {
     							 double minv = 999;
     							 double maxv = 0;
    					  	
     							for(commonMove ch : children) 
    					  		{ UCTNode n = ch.uctNode();
    					  		  if(n!=null)
    					  		  {
    					  		  int vis = n.getVisits();
    					  		  minv = Math.min(Math.max(0,vis),minv);
    					  		  maxv = Math.max(vis, maxv);
    					  		}}
    						 
    							 int vis = node.getVisits();
    							 if(vis>0)
    							 {	return((Math.max(0, vis)-minv)/(maxv-minv));
    							 }
     						 }
     						 return(0);
    					  }}
    				  }
    			  }
     	return(0);
     	}
    	else if(teachNet!=null)
    	{	double minvalue = 999;
    		double maxvalue = -1;
    		double values[] = null;
    		mode = TeachMode.Net;
    		switch(mode)
    		{
    		case Net:
    	        netCalculatePositive(teachNet,GameBoard);
        		values = teachNet.getValues();
        		break;
    		case Live:
    			values = lastNetworkValues;
    			break;
    		case Trained:
    			values = trainedValues;
    			break;
    		default: break;
    		}
    		//teachNet.dumpWeights(true);
    		if(values!=null)
    		{
    		int index = getMoveIndex(teachNet,c.col,c.row);
    		for(int lim=values.length-1; lim>0; lim--) { double v=values[lim]; minvalue = Math.min(Math.max(0, minvalue), v); maxvalue = Math.max(maxvalue, v); }
    		//if(c.col=='F'&&c.row==1) {G.print(""+c+" "+values[index]);}
    		double target = values[index];
    		double v = ((target-minvalue)/(maxvalue-minvalue));
    		//if(c.col=='A'&&c.row==1) { G.print("T "+target); }
    		return(v/*10*target*/);
    		//return(board.ncols*2*target);
    		}
    		return(0);
    	
    	}
    	
    	return(0);
    }
    double [] trainedValues = null;
    // set the training data for visualization
    public void setTrainingData(commonMove cm)
    {	String data = (String)cm.getProperty(TrainingData);
    	trainedValues = null;
    	if(evalNet!=null && data!=null)
    	{	TrainingData td = new TrainingData();
    		parseValues(td,(GenericNetwork)evalNet,data,0);
    		long now = G.Date();
    		trainedValues = now%1000<500? td.winrates : td.values;
    	}
    }
    // net with an extra input layer for last 2 moves, which
    // feeds into 2 of the 4 filter stacks
   @SuppressWarnings("unused")
private GenericNetwork createF8PIB()
    {	int ncells = board.nCells();
    	return new GenericNetwork(
    				"NAME F8PIB"
    				// f8p with an extra input layer for the last 2 moves
    				+ " LAYER 0 (TYPE I ID IN )"
    				+ " LAYER 1 (TYPE I ID IN2 )"
    				
    				// these two layers get context free information so they can
    				// learn board features
    				+ " LAYER 2 (TO 0 TYPE FILTER)"
    				+ " LAYER 3 (TO 2 TYPE FILTER)"
    				+ " LAYER 4 (TO 3 TYPE FILTER)"
    				+ " LAYER 5 (TO 4 TYPE POOL)"
    				
    				+ " LAYER 6 (TO 0 TYPE FILTER)"
    				+ " LAYER 7 (TO 6 TYPE FILTER)"								
    				+ " LAYER 8 (TO 7 TYPE FILTER)"
    				+ " LAYER 9 (TO 8 TYPE POOL)"
    				
    				// these 2 layers also include "last move" information
    				// so they can learn response to local activity
    				+ " LAYER 10 (TO 0 TYPE FILTER TOSINGLE IN2)"
    				+ " LAYER 11 (TO 10 TYPE FILTER)"								
    				+ " LAYER 12 (TO 11 TYPE FILTER)"
    				+ " LAYER 13 (TO 12 TYPE POOL)"
    				
    				+ " LAYER 14 (TO 0 TYPE FILTER TOSINGLE IN2)"
    				+ " LAYER 15 (TO 14 TYPE FILTER)"								
    				+ " LAYER 16 (TO 15 TYPE FILTER)"
    				+ " LAYER 17 (TO 16 TYPE POOL)"
    																				
    				
    				+ " LAYER 18 (TYPE FC TO 5 TO 9 TO 13 TO 17)"
     				
    				+ " COORDINATES HEX "+board.ncols
    				+ " LEARNING_RATE 0.005"
    				+ " TRANSFER_FUNCTION SIGMOID"
    				,
    				ncells,			// 0 input layer
    				ncells,			// 1 extra input layer
    				ncells,ncells,ncells,ncells,	// 2 3 4 5 filter layers
    				ncells,ncells,ncells,ncells,	// 6 7 8 9 filter layers
    				ncells,ncells,ncells,ncells,	// 10 11 12 13 filter layers with extra input
    				ncells,ncells,ncells,ncells,	// 14 15 16 17 filter layers with extra input
     				ncells,				// 18 fully connected layer 	
    				ncells+1			// 20 output layer
    				);	// learn with all sigmoid nodes
    }
   // net with an extra input layer for last 2 moves, which
   // feeds into 2 of the 4 filter stacks
  @SuppressWarnings("unused")
private GenericNetwork createF8PIC()
   {	int ncells = board.nCells();
   	return new GenericNetwork(
   				"NAME F8PIC"
   				+ " LEARNING_RATE 0.0001"
  				// f8p with an extra input layer for the last 2 moves
   				+ " LAYER 0 (TYPE I ID IN )"
   				+ " LAYER 1 (TYPE I ID IN2 )"
   				
   				// these two layers get context free information so they can
   				// learn board features
   				+ " LAYER 2 (TO IN TOSINGLE IN2 TYPE FILTER)"
   				+ " LAYER 3 (TO 2 TYPE FILTER)"
   				+ " LAYER 4 (TO 3 TYPE FILTER)"
   				+ " LAYER 5 (TO 4 TYPE POOL ID POOL1 )"
   				
   				+ " LAYER 6 (TO IN TOSINGLE IN2 TYPE FILTER)"
   				+ " LAYER 7 (TO 6 TYPE FILTER)"								
   				+ " LAYER 8 (TO 7 TYPE FILTER)"
   				+ " LAYER 9 (TO 8 TYPE POOL ID POOL2)"
   				
   				// these 2 layers also include "last move" information
   				// so they can learn response to local activity
   				+ " LAYER 10 (TO IN TYPE FILTER TOSINGLE IN2)"
   				+ " LAYER 11 (TO 10 TYPE FILTER)"								
   				+ " LAYER 12 (TO 11 TYPE FILTER)"
   				+ " LAYER 13 (TO 12 TYPE POOL ID POOL3)"
   				
   				+ " LAYER 14 (TO IN TYPE FILTER TOSINGLE IN2)"
   				+ " LAYER 15 (TO 14 TYPE FILTER)"								
   				+ " LAYER 16 (TO 15 TYPE FILTER)"
   				+ " LAYER 17 (TO 16 TYPE POOL ID POOL4)"
   																				
   				
   				+ " LAYER 18 (TYPE FC TO POOL1 TO POOL2 TO POOL3 TO POOL4)"
    				
   				+ " COORDINATES HEX "+board.ncols
   				+ " TRANSFER_FUNCTION SIGMOID"
   				,
   				ncells,			// 0 input layer
   				ncells,			// 1 extra input layer
   				ncells,ncells,ncells,ncells,	// 2 3 4 5 filter layers
   				ncells,ncells,ncells,ncells,	// 6 7 8 9 filter layers
   				ncells,ncells,ncells,ncells,	// 10 11 12 13 filter layers with extra input
   				ncells,ncells,ncells,ncells,	// 14 15 16 17 filter layers with extra input
    				ncells,				// 18 fully connected layer 	
   				ncells+1			// 19 output layer
   				);	// learn with all sigmoid nodes
   }
   // net with an extra input layer for last 2 moves, which
   // feeds into 2 of the 4 filter stacks
  @SuppressWarnings("unused")
private GenericNetwork createF8PICW()
   {	int ncells = board.nCells();
   	return new GenericNetwork(
   				"NAME F8PICW"
   				+ " LEARNING_RATE 0.0005"
  				// f8p with an extra input layer for the last 2 moves
   				+ " LAYER 0 (TYPE I ID IN )"
   				+ " LAYER 1 (TYPE I ID IN2 )"
   				
   				// these two layers get context free information so they can
   				// learn board features
   				+ " LAYER 2 (TO IN TOSINGLE IN2 TYPE FILTER)"
   				+ " LAYER 3 (TO 2 TYPE FILTER)"
   				+ " LAYER 4 (TO 3 TYPE FILTER)"
   				+ " LAYER 5 (TO 4 TYPE POOL ID POOL1 )"
   				
   				+ " LAYER 6 (TO IN TOSINGLE IN2 TYPE FILTER)"
   				+ " LAYER 7 (TO 6 TYPE FILTER)"								
   				+ " LAYER 8 (TO 7 TYPE FILTER)"
   				+ " LAYER 9 (TO 8 TYPE POOL ID POOL2)"
   				
   				// these 2 layers also include "last move" information
   				// so they can learn response to local activity
   				+ " LAYER 10 (TO IN TYPE FILTER TOSINGLE IN2)"
   				+ " LAYER 11 (TO 10 TYPE FILTER)"								
   				+ " LAYER 12 (TO 11 TYPE FILTER)"
   				+ " LAYER 13 (TO 12 TYPE POOL ID POOL3)"
   				
   				+ " LAYER 14 (TO IN TYPE FILTER TOSINGLE IN2)"
   				+ " LAYER 15 (TO 14 TYPE FILTER)"								
   				+ " LAYER 16 (TO 15 TYPE FILTER)"
   				+ " LAYER 17 (TO 16 TYPE POOL ID POOL4)"
   																				
   				
   				+ " LAYER 18 (TYPE FC TO POOL1 TO POOL2 TO POOL3 TO POOL4)"
    				
   				+ " COORDINATES HEX "+board.ncols
   				+ " TRANSFER_FUNCTION SIGMOID"
   				,
   				ncells,			// 0 input layer
   				ncells,			// 1 extra input layer
   				ncells,ncells,ncells,ncells,	// 2 3 4 5 filter layers
   				ncells,ncells,ncells,ncells,	// 6 7 8 9 filter layers
   				ncells,ncells,ncells,ncells,	// 10 11 12 13 filter layers with extra input
   				ncells,ncells,ncells,ncells,	// 14 15 16 17 filter layers with extra input
    			ncells*4,				// 18 fully connected layer 	
   				ncells+1			// 19 output layer
   				);	// learn with all sigmoid nodes
   }

  // net with an extra input layers for white black and empty
 @SuppressWarnings("unused")
private GenericNetwork createF93I()
  {	int ncells = board.nCells();
  	return new GenericNetwork(
  				"NAME F93I"
  				+ " LEARNING_RATE 0.0005"
 				// f8p with an extra input layer for the last 2 moves
  				+ " LAYER 0 (TYPE I ID INE )"	// empty
  				+ " LAYER 1 (TYPE I ID INB )"	// white cells (normalized to white to move)
  				+ " LAYER 2 (TYPE I ID INW )"	// black cells (normalized to white to move)
  
  				+ " LAYER 3 (TO INE TO INB TO INW TYPE FILTER)"
  				+ " LAYER 4 (TO 3 TYPE FILTER)"
  				+ " LAYER 5 (TO 4 TYPE FILTER)"
  				+ " LAYER 6 (TO 5 TYPE FC)"
				
  				+ " LAYER 7 (TYPE O TO 6 ID OUT)"
   				+ " LAYER 8 (TYPE I TO 6 ID WINWATE)"
  				
  				+ " COORDINATES HEX "+board.ncols
  				+ " TRANSFER_FUNCTION SIGMOID"
  				,
  				ncells,			// empty input layer
  				ncells,			// white input layer
  				ncells,			// black input layer
  				ncells,ncells,ncells,ncells,	// 3 4 5 6 filter layers
  				ncells+1			// 19 output layer
  				);	// learn with all sigmoid nodes
  }
   // net with an extra input layer for last 2 moves, which
   // feeds into 2 of the 4 filter stacks
  @SuppressWarnings("unused")
private GenericNetwork createF8PC()
   {	int ncells = board.nCells();
   	return new GenericNetwork(
   				"NAME F8PC"
   				+ " LEARNING_RATE 0.0005"
  				// f8p with an extra input layer for the last 2 moves
   				+ " LAYER 0 (TYPE I ID IN )"
   				+ " LAYER 1 (TYPE I ID IN2 )"
   				
   				// these two layers get context free information so they can
   				// learn board features
   				+ " LAYER 2 (TO IN TYPE FILTER)"
   				+ " LAYER 3 (TO 2 TYPE FILTER)"
   				+ " LAYER 4 (TO 3 TYPE FILTER)"
   				+ " LAYER 5 (TO 4 TYPE POOL ID POOL1 )"
   				
   				+ " LAYER 6 (TO IN TYPE FILTER)"
   				+ " LAYER 7 (TO 6 TYPE FILTER)"								
   				+ " LAYER 8 (TO 7 TYPE FILTER)"
   				+ " LAYER 9 (TO 8 TYPE POOL ID POOL2)"
   				
   				// these 2 layers also include "last move" information
   				// so they can learn response to local activity
   				+ " LAYER 10 (TO IN TYPE FILTER TOSINGLE IN2)"
   				+ " LAYER 11 (TO 10 TYPE FILTER)"								
   				+ " LAYER 12 (TO 11 TYPE FILTER)"
   				+ " LAYER 13 (TO 12 TYPE POOL ID POOL3)"
   				
   				+ " LAYER 14 (TO IN TYPE FILTER TOSINGLE IN2)"
   				+ " LAYER 15 (TO 14 TYPE FILTER)"								
   				+ " LAYER 16 (TO 15 TYPE FILTER)"
   				+ " LAYER 17 (TO 16 TYPE POOL ID POOL4)"
   																				
   				
   				+ " LAYER 18 (TYPE O ID OUT TO POOL1 TO POOL2 TO POOL3 TO POOL4)"
    				
   				+ " COORDINATES HEX "+board.ncols
   				+ " TRANSFER_FUNCTION SIGMOID"
   				,
   				ncells,			// 0 input layer
   				ncells,			// 1 extra input layer
   				ncells,ncells,ncells,ncells,	// 2 3 4 5 filter layers
   				ncells,ncells,ncells,ncells,	// 6 7 8 9 filter layers
   				ncells,ncells,ncells,ncells,	// 10 11 12 13 filter layers with extra input
   				ncells,ncells,ncells,ncells,	// 14 15 16 17 filter layers with extra input
    			ncells+1				// 18 fully connected layer 	
   				);	// learn with all sigmoid nodes
   }
  @SuppressWarnings("unused")
private GenericNetwork createF8PCB()
  {	int ncells = board.nCells();
  	return new GenericNetwork(
  				"NAME F8PCB"
  				+ " LEARNING_RATE 0.0005"
 				// f8p with an extra input layer for the last 2 moves
  				+ " LAYER 0 (TYPE I ID IN )"
  				+ " LAYER 1 (TYPE I ID IN2 )"
  				
  				// these two layers get context free information so they can
  				// learn board features
  				+ " LAYER 2 (TO IN TYPE FILTER)"
  				+ " LAYER 3 (TO 2 TYPE FILTER)"
  				+ " LAYER 4 (TO 3 TYPE FILTER)"
  				+ " LAYER 5 (TO 4 TYPE POOL ID POOL1 )"
  				
  				+ " LAYER 6 (TO IN TYPE FILTER)"
  				+ " LAYER 7 (TO 6 TYPE FILTER)"								
  				+ " LAYER 8 (TO 7 TYPE FILTER)"
  				+ " LAYER 9 (TO 8 TYPE POOL ID POOL2)"
  				
  				// these 2 layers also include "last move" information
  				// so they can learn response to local activity
  				+ " LAYER 10 (TO IN TYPE FILTER)"
  				+ " LAYER 11 (TO 10 TYPE FILTER)"								
  				+ " LAYER 12 (TO 11 TYPE FILTER)"
  				+ " LAYER 13 (TO 12 TYPE POOL ID POOL3)"
  				
  				+ " LAYER 14 (TO IN TYPE FILTER)"
  				+ " LAYER 15 (TO 14 TYPE FILTER)"								
  				+ " LAYER 16 (TO 15 TYPE FILTER)"
  				+ " LAYER 17 (TO 16 TYPE POOL ID POOL4)"
  																				
  				
  				+ " LAYER 18 ( TYPE O ID OUT TO IN2 TO POOL1 TO POOL2 TO POOL3 TO POOL4)"
   				
  				+ " COORDINATES HEX "+board.ncols
  				+ " TRANSFER_FUNCTION SIGMOID"
  				,
  				ncells,			// 0 input layer
  				ncells,			// 1 extra input layer
  				ncells,ncells,ncells,ncells,	// 2 3 4 5 filter layers
  				ncells,ncells,ncells,ncells,	// 6 7 8 9 filter layers
  				ncells,ncells,ncells,ncells,	// 10 11 12 13 filter layers with extra input
  				ncells,ncells,ncells,ncells,	// 14 15 16 17 filter layers with extra input
   			ncells+1				// 18 fully connected layer 	
  				);	// learn with all sigmoid nodes
  }
  private GenericNetwork createSimple()
  {	int ncells = board.nCells();
  	return new GenericNetwork(
  				"NAME F8PCB"
  				+ " LEARNING_RATE 0.0005"
  				+ " LAYER 0 (TYPE I ID IN )"		// normalized to white pieces
  				+ " LAYER 1 (TYPE I ID IN2 )"		// normalized to black pieces
  				
  				+ " LAYER 2 (TO IN TO IN2 TYPE FC)"
  				+ " LAYER 3 (TO 2 TYPE FC)"
 				+ " LAYER 4 (TO 3 TYPE FC)"
 				+ " LAYER 5 (TO 4 TYPE FC)"
 				+ " LAYER 6 (TO 5 TYPE FC)"
 				+ " LAYER 7 (TO 6 TYPE FC)"
 				+ " LAYER 8 (TO 7 TYPE O)"
   				
  				+ " COORDINATES HEX "+board.ncols
  				+ " TRANSFER_FUNCTION SIGMOID"
  				,
  				ncells,			// 0 input layer
  				ncells,			// 1 extra input layer
  				// 7 connected layers
  				ncells*2,ncells*2,ncells*2,ncells*2,ncells*2,ncells*2,ncells*2,
  				// output layer 	
  				ncells				
  				);	// learn with all sigmoid nodes
  }

  // net with an extra input layer for last 2 moves, which
  // feeds into 2 of the 4 filter stacks
 @SuppressWarnings("unused")
private GenericNetwork createF8PID()
  {	int ncells = board.nCells();
  	return new GenericNetwork(
  				"NAME F8PID"
  				+ " LEARNING_RATE 0.0001"
 				// f8pi with an extra input layer and deeper network
  				+ " LAYER 0 (TYPE I ID IN )"
  				+ " LAYER 1 (TYPE I ID IN2 )"
  				
  				// these two layers get context free information so they can
  				// learn board features
  				+ " LAYER 2 (TO IN TOSINGLE IN2 TYPE FILTER)"
  				+ " LAYER 3 (TO 2 TYPE FILTER)"
  				+ " LAYER 4 (TO 3 TYPE FILTER)"
  				+ " LAYER 5 (TO 4 TYPE FILTER)"
  				+ " LAYER 6 (TO 5 TYPE POOL ID POOL1 )"
  				
  				+ " LAYER 7 (TO IN TYPE FILTER)"
  				+ " LAYER 8 (TO 7 TYPE FILTER)"								
 				+ " LAYER 9 (TO 8 TYPE FILTER)"
 				+ " LAYER 10 (TO 9 TYPE FILTER)"
  				+ " LAYER 11 (TO 10 TYPE POOL ID POOL2)"
  				
   				
  				+ " LAYER 12 (TYPE FC TO POOL1 TO POOL2)"
 				+ " LAYER 13 (TYPE FC TO 12 )"
 				+ " LAYER 14 (TYPE O ID OUT TO 13)"
				   				
  				+ " COORDINATES HEX "+board.ncols
  				+ " TRANSFER_FUNCTION SIGMOID"
  				,
  				ncells,			// 0 input layer
  				ncells,			// 1 extra input layer
  				ncells,ncells,ncells,ncells,0,	// 2 3 4 5 filter layers 6 pool layer
  				ncells,ncells,ncells,ncells,0,	// 7 8 9 10 filter layers 11 pool layer
  				ncells,ncells,		// 12 13 Fc layers 
  				ncells+1	// 14 output layer
  				);	// learn with all sigmoid nodes
  }
 
 @SuppressWarnings("unused")
private GenericNetwork createF8PCBW()
 {	int ncells = board.nCells();
 	return new GenericNetwork(
 				"NAME F8PCBW"
 				+ " LEARNING_RATE 0.0005"
				// f8p with an extra input layer for the last 2 moves
 				+ " LAYER 0 (TYPE I ID IN )"
 				+ " LAYER 1 (TYPE I ID IN2 )"
 				
 				// these two layers get context free information so they can
 				// learn board features
 				+ " LAYER 2 (TO IN TYPE FILTER)"
 				+ " LAYER 3 (TO 2 TYPE FILTER)"
 				+ " LAYER 4 (TO 3 TYPE FILTER)"
 				+ " LAYER 5 (TO 4 TYPE POOL ID POOL1 )"
 				
 				+ " LAYER 6 (TO IN TYPE FILTER)"
 				+ " LAYER 7 (TO 6 TYPE FILTER)"								
 				+ " LAYER 8 (TO 7 TYPE FILTER)"
 				+ " LAYER 9 (TO 8 TYPE POOL ID POOL2)"
 				
 				// these 2 layers also include "last move" information
 				// so they can learn response to local activity
 				+ " LAYER 10 (TO IN TYPE FILTER)"
 				+ " LAYER 11 (TO 10 TYPE FILTER)"								
 				+ " LAYER 12 (TO 11 TYPE FILTER)"
 				+ " LAYER 13 (TO 12 TYPE POOL ID POOL3)"
 				
 				+ " LAYER 14 (TO IN TYPE FILTER)"
 				+ " LAYER 15 (TO 14 TYPE FILTER)"								
 				+ " LAYER 16 (TO 15 TYPE FILTER)"
 				+ " LAYER 17 (TO 16 TYPE POOL ID POOL4)"
 																				
 				
 				+ " LAYER 18 ( TYPE O ID OUT TO IN2 TO POOL1 TO POOL2 TO POOL3 TO POOL4 )"
  				+ " LAYER 19 ( TYPE O ID WINRATE TO POOL1 TO POOL2 TO POOL3 TO POOL4 )"
 				+ " COORDINATES HEX "+board.ncols
 				+ " TRANSFER_FUNCTION SIGMOID"
 				,
 				ncells,			// 0 input layer
 				ncells,			// 1 extra input layer
 				ncells,ncells,ncells,ncells,	// 2 3 4 5 filter layers
 				ncells,ncells,ncells,ncells,	// 6 7 8 9 filter layers
 				ncells,ncells,ncells,ncells,	// 10 11 12 13 filter layers with extra input
 				ncells,ncells,ncells,ncells,	// 14 15 16 17 filter layers with extra input
  			ncells+1,			// 18 fully connected layer for visit counts
  			ncells+1			// 19 fully connected layer for winrates
 				);	// learn with all sigmoid nodes
 }
 // no bias for recent moves
 @SuppressWarnings("unused")
private GenericNetwork createF8PW()
 {	int ncells = board.nCells();
 	return new GenericNetwork(
 				"NAME F8PW"
 				+ " LEARNING_RATE 0.0005"
				+ " LAYER 0 (TYPE I ID IN )"
 				
 				// these two layers get context free information so they can
 				// learn board features
 				+ " LAYER 1 (TO IN TYPE FILTER)"
 				+ " LAYER 2 ( TYPE FILTER)"
 				+ " LAYER 3 ( TYPE FILTER)"
 				+ " LAYER 4 ( TYPE POOL ID POOL1 )"
 				
 				+ " LAYER 5 (TO IN TYPE FILTER)"
 				+ " LAYER 6 ( TYPE FILTER)"								
 				+ " LAYER 7 ( TYPE FILTER)"
 				+ " LAYER 8 ( TYPE POOL ID POOL2)"
 				
 				+ " LAYER 9 (TO IN TYPE FILTER)"
 				+ " LAYER 10 ( TYPE FILTER)"								
 				+ " LAYER 11 ( TYPE FILTER)"
 				+ " LAYER 12 ( TYPE POOL ID POOL3)"
 				
 				+ " LAYER 13 (TO IN TYPE FILTER)"
 				+ " LAYER 14 ( TYPE FILTER)"								
 				+ " LAYER 15( TYPE FILTER)"
 				+ " LAYER 16 ( TYPE POOL ID POOL4)"
 																				
 				
 				+ " LAYER 17 ( TYPE O ID OUT TO POOL1 TO POOL2 TO POOL3 TO POOL4 )"
  				+ " LAYER 18 ( TYPE O ID WINRATE TO POOL1 TO POOL2 TO POOL3 TO POOL4 )"
 				+ " COORDINATES HEX "+board.ncols
 				+ " TRANSFER_FUNCTION SIGMOID"
 				,
 				ncells,			// 0 input layer
 				ncells,ncells,ncells,ncells,	// 1 2 3 4  filter layers
 				ncells,ncells,ncells,ncells,	// 5 6 7 8  filter layers
 				ncells,ncells,ncells,ncells,	// 9 10 11 12 filter layers with extra input
 				ncells,ncells,ncells,ncells,	// 13 14 15 16 filter layers with extra input
  			ncells+1,			// 17 fully connected layer for visit counts
  			ncells+1			// 18 fully connected layer for winrates
 				);	// learn with all sigmoid nodes
 }
 
 //
//no bias for recent moves.  Same as F8PX, just a different name
//
private GenericNetwork createF8PXC()
{	int ncells = board.nCells();
	return new GenericNetwork(
				"NAME F8PXC"
				+ " LEARNING_RATE 0.001"	// reduced learning rate, was 0.0005 then 0.0002
				+ " LAYER NEXT (TYPE I ID INE )"	// empty stones
				+ " LAYER NEXT (TYPE I ID INW )"	// white stones
				+ " LAYER NEXT (TYPE I ID INB )"	// black stones
				+ " LAYER NEXT (TYPE I ID INC )"	// constant color to move 
				
				+ " LAYER NEXT (TO INE TO INW TO INB TO INC TYPE FC)"
				+ " LAYER NEXT (TYPE FC)"
				+ " LAYER NEXT (TYPE FC)"
				+ " LAYER NEXT (TYPE FC)"																				
				+ " LAYER NEXT (TYPE FC ID FINAL)"
				+ " LAYER NEXT ( TYPE O ID OUT TO FINAL)"
				+ " LAYER NEXT ( TYPE O ID WINRATE TO FINAL)"
				+ " COORDINATES HEX "+board.ncols
				+ " TRANSFER_FUNCTION SIGMOID"
				,
				ncells,	ncells,ncells,1,		// 0,1,2,3 input layers
				ncells,ncells,ncells,ncells,ncells,	// 5 fully connected filter layers
				ncells+1,			// fully connected layer for visit counts
				ncells+1			// fully connected layer for winrates
				);	// learn with all sigmoid nodes
}

//
//no bias for recent moves.  Same as F8PX, just a different name
//
 GenericNetwork createF8PXA()
{	int ncells = board.nCells();
	return new GenericNetwork(
				"NAME F8PXA"
				+ " LEARNING_RATE 0.0002"	// reduced learning rate, was 0.0005
				+ " LAYER 0 (TYPE I ID IN )"
				
				// these two layers get context free information so they can
				// learn board features
				+ " LAYER 1 (TO IN TYPE FILTER)"
				+ " LAYER 2 ( TYPE FILTER)"
				+ " LAYER 3 ( TYPE FILTER)"
				+ " LAYER 4 ( TYPE FILTER)"
				+ " LAYER 5 ( TYPE POOL ID POOL1 )"
				
				+ " LAYER 6 (TO IN TYPE FILTER)"
				+ " LAYER 7 ( TYPE FILTER)"								
				+ " LAYER 8 ( TYPE FILTER)"
				+ " LAYER 9 ( TYPE FILTER)"
				+ " LAYER 10 ( TYPE POOL ID POOL2)"
				
				+ " LAYER 11 (TO IN TYPE FILTER)"
				+ " LAYER 12 ( TYPE FILTER)"								
				+ " LAYER 13 ( TYPE FILTER)"
				+ " LAYER 14 ( TYPE FILTER)"
				+ " LAYER 15 ( TYPE POOL ID POOL3)"
				
				+ " LAYER 16 (TO IN TYPE FILTER)"
				+ " LAYER 17 ( TYPE FILTER)"								
				+ " LAYER 18( TYPE FILTER)"
				+ " LAYER 19( TYPE FILTER)"
				+ " LAYER 20 ( TYPE POOL ID POOL4)"
																				
				+ " LAYER 21 (TO IN TYPE FILTER)"
				+ " LAYER 22 ( TYPE FILTER)"								
				+ " LAYER 23( TYPE FILTER)"
				+ " LAYER 24( TYPE FILTER)"
				+ " LAYER 25 ( TYPE POOL ID POOL5)"
				
				+ " LAYER 26 ( TYPE O ID OUT TO POOL1 TO POOL2 TO POOL3 TO POOL4 TO POOL5)"
				+ " LAYER 27 ( TYPE O ID WINRATE TO POOL1 TO POOL2 TO POOL3 TO POOL4 TO POOL5)"
				+ " COORDINATES HEX "+board.ncols
				+ " TRANSFER_FUNCTION SIGMOID"
				,
				ncells,			// 0 input layer
				ncells,ncells,ncells,ncells,ncells,	// 1 2 3 4 5  filter layers
				ncells,ncells,ncells,ncells,ncells,	// 6 7 8 9 10 filter layers
				ncells,ncells,ncells,ncells,ncells,	// 11 12 13 14 15 filter layers with extra input
				ncells,ncells,ncells,ncells,ncells,	// 16 17 18 19 20 filter layers with extra input
				ncells,ncells,ncells,ncells,ncells,	// 21 22 23 34 35 filter layers with extra input
			ncells+1,			// 26 fully connected layer for visit counts
			ncells+1			// 27 fully connected layer for winrates
				);	// learn with all sigmoid nodes
}
@SuppressWarnings("unused")
private GenericNetwork createF8PI()
{	int ncells = board.nCells();
	return new GenericNetwork(
				"NAME F8PI"
				+ " LAYER 0 (TYPE I ID IN )"
				// f8p with an extra input layer for the last 2 moves
				+ " LAYER 1 (TO 0 TYPE FILTER)"
				+ " LAYER 2 (TO 1 TYPE FILTER)"
				+ " LAYER 3 (TO 2 TYPE FILTER)"
				+ " LAYER 4 (TO 3 TYPE POOL)"
				
				+ " LAYER 5 (TO 0 TYPE FILTER)"
				+ " LAYER 6 (TO 5 TYPE FILTER)"								
				+ " LAYER 7 (TO 6 TYPE FILTER)"
				+ " LAYER 8 (TO 7 TYPE POOL)"
				
				+ " LAYER 9 (TO 0 TYPE FILTER)"
				+ " LAYER 10 (TO 9 TYPE FILTER)"								
				+ " LAYER 11 (TO 10 TYPE FILTER)"
				+ " LAYER 12 (TO 11 TYPE POOL)"
				
				+ " LAYER 13 (TO 0 TYPE FILTER)"
				+ " LAYER 14 (TO 13 TYPE FILTER)"								
				+ " LAYER 15 (TO 14 TYPE FILTER)"
				+ " LAYER 16 (TO 15 TYPE POOL)"
																				
				+ " LAYER 17 (TYPE I ID IN2 )"
				+ " LAYER 18 (TYPE FC TO 17 TO 4 TO 8 TO 12 TO 16)"

				+ " COORDINATES HEX "+board.ncols
				+ " LEARNING_RATE 0.005"
				+ " TRANSFER_FUNCTION SIGMOID"
				,
				ncells,			// 0 input layer
				ncells,ncells,ncells,ncells,	// 1 2 3 4 filter layers
				ncells,ncells,ncells,ncells,	// 5 6 7 8 filter layers
				ncells,ncells,ncells,ncells,	// 9 10 11 12 filter layers
				ncells,ncells,ncells,ncells,	// 13 14 15 16 filter layers
				ncells,				// 17 extra input layer
				ncells,				// 18 fully connected layer 		
				ncells+1			// 19 output layer
				);	// learn with all sigmoid nodes
}
@SuppressWarnings("unused")
private GenericNetwork createF8PS()
{	int ncells = board.nCells();
	return new GenericNetwork(
				"NAME F8PS"
				+ " LAYER 0 (TYPE I ID IN )"
				+ " LAYER 1 (TO IN TYPE FILTER)"
				+ " LAYER 2 (TO 1 TYPE FILTER)"
				+ " LAYER 3 (TO 2 TYPE POOL ID POOL1)"
				
				+ " LAYER 4 (TO IN TYPE FILTER)"
				+ " LAYER 5 (TO 4 TYPE FILTER)"								
				+ " LAYER 6 (TO 5 TYPE POOL ID POOL2)"
				
				+ " LAYER 7 (TO IN TYPE FILTER)"
				+ " LAYER 8 (TO 7 TYPE FILTER)"								
				+ " LAYER 9 (TO 8 TYPE POOL ID POOL3)"
				
				+ " LAYER 10 (TO IN TYPE FILTER)"
				+ " LAYER 11 (TO 10 TYPE FILTER)"								
				+ " LAYER 12 (TO 11 TYPE POOL ID POOL4)"
				
				+ " LAYER 13 (TO IN TYPE FILTER)"
				+ " LAYER 14 (TO 13 TYPE FILTER)"
				+ " LAYER 15 (TO 14 TYPE POOL ID POOL5)"
																				
				+ " LAYER 16 (TYPE FC TO POOL1 TO POOL2 TO POOL3 TO POOL4 TO POOL5)"

				+ " COORDINATES HEX "+board.ncols
				+ " LEARNING_RATE 0.0025"
				+ " TRANSFER_FUNCTION SIGMOID"
				,
				ncells,			// 0 input layer
				ncells,ncells,ncells,	// 1 2 3  filter layers
				ncells,ncells,ncells,	// 4 5 6  filter layers
				ncells,ncells,ncells,	// 7 8 9  filter layers
				ncells,ncells,ncells,	// 10 11 12 filter layers
				ncells,ncells,ncells,	// 13 14 15 filter layers
				ncells,				// 16 fully connected layer 		
				ncells+1			// 17 output layer
				);	// learn with all sigmoid nodes
}
@SuppressWarnings("unused")
private GenericNetwork createF8P()
{	int ncells = board.nCells();
	return new GenericNetwork(
				"NAME F8P"
				+ " LAYER 0 (TYPE I ID IN )"
				+ " LAYER 1 (TO IN TYPE FILTER)"
				+ " LAYER 2 (TO 1 TYPE FILTER)"
				+ " LAYER 3 (TO 2 TYPE FILTER)"
				+ " LAYER 4 (TO 3 TYPE POOL ID POOL1)"
				
				+ " LAYER 5 (TO IN TYPE FILTER)"
				+ " LAYER 6 (TO 5 TYPE FILTER)"								
				+ " LAYER 7 (TO 6 TYPE FILTER)"
				+ " LAYER 8 (TO 7 TYPE POOL ID POOL2)"
				
				+ " LAYER 9 (TO IN TYPE FILTER)"
				+ " LAYER 10 (TO 9 TYPE FILTER)"								
				+ " LAYER 11 (TO 10 TYPE FILTER)"
				+ " LAYER 12 (TO 11 TYPE POOL ID POOL3)"
				
				+ " LAYER 13 (TO IN TYPE FILTER)"
				+ " LAYER 14 (TO 13 TYPE FILTER)"								
				+ " LAYER 15 (TO 14 TYPE FILTER)"
				+ " LAYER 16 (TO 15 TYPE POOL ID POOL4)"
																				
				+ " LAYER 17 (TYPE FC TO POOL1 TO POOL2 TO POOL3 TO POOL4)"

				+ " COORDINATES HEX "+board.ncols
				+ " LEARNING_RATE 0.0025"
				+ " TRANSFER_FUNCTION SIGMOID"
				,
				ncells,			// 0 input layer
				ncells,ncells,ncells,ncells,	// 1 2 3 4 filter layers
				ncells,ncells,ncells,ncells,	// 5 6 7 8 filter layers
				ncells,ncells,ncells,ncells,	// 9 10 11 12 filter layers
				ncells,ncells,ncells,ncells,	// 13 14 15 16 filter layers
				ncells,				// 17 extra input layer
				ncells,				// 18 fully connected layer 		
				ncells+1			// 19 output layer
				);	// learn with all sigmoid nodes
}

//small network with fully connected layers
@SuppressWarnings("unused")
private GenericNetwork createS4()
{	int ncells = board.nCells();
	return new GenericNetwork(
		"NAME S4"
		+ " COORDINATES HEX "+board.ncols
		+ " LEARNING_RATE 0.0025"
		+ " TRANSFER_FUNCTION SIGMOID",
		ncells,			// 0 input layer
		ncells,ncells,	// 1 2 filter layers
		ncells+1		// 3 output layer
		);
}
// medium network with filters
@SuppressWarnings("unused")
private GenericNetwork createF2()
{	int ncells = board.nCells();
	return new GenericNetwork (
			"NAME F2"
			+ " LAYER 1 (TO 0 TYPE FILTER)"
			+ " LAYER 2 (TO 1 TYPE FILTER)"
			+ " LAYER 3 (TO 0 TYPE FILTER)"
			+ " LAYER 4 (TO 3 TYPE FILTER)"
		
			+ " LAYER 5 (TYPE FC TO 2 TO 4 )"
			+ " LAYER 6 (TYPE O TO 5)"
			+ " COORDINATES HEX "+board.ncols
			+ " LEARNING_RATE 0.001"
			+ " TRANSFER_FUNCTION SIGMOID",
	ncells,			// 0 input layer
	ncells,ncells,	// 1 2 filter layers
	ncells,ncells,	// 3 4 filter layers 
	ncells*2,		// 5 fully connected layer 		
	ncells+1		// 6 output layer
	);
}
@SuppressWarnings("unused")
private GenericNetwork createF4()
{	int ncells = board.nCells();
	return new GenericNetwork(
			"NAME F4"
	+ " LAYER 1 (TO 0 TYPE FILTER)"
	+ " LAYER 2 (TO 1 TYPE FILTER)"
	
	+ " LAYER 3 (TO 0 TYPE FILTER)"
	+ " LAYER 4 (TO 3 TYPE FILTER)"
	
	+ " LAYER 5 (TO 0 TYPE FILTER)"
	+ " LAYER 6 (TO 5 TYPE FILTER)"
	
	+ " LAYER 7 (TO 0 TYPE FILTER)"
	+ " LAYER 8 (TO 7 TYPE FILTER)"
	
	+ " LAYER 9 (TYPE FC TO 2 TO 4 TO 6 TO 8)"

	+ " COORDINATES HEX "+board.ncols
			
	+ " LEARNING_RATE 0.005"
	+ " TRANSFER_FUNCTION SIGMOID",
	ncells,			// 0 input layer
	ncells,ncells,	// 1 2 filter layers
	ncells,ncells,	// 3 4 filter layers 
	ncells,ncells,	// 5 6 filter layers
	ncells,ncells,	// 7 8 filter layers 
	
	ncells*4,			// 9 fully connected layer 		
	ncells*2,			// 10 fully connected layer 		
	ncells+1		// 11 output layer
	);

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
			name = "TESTBOT_HYBRID_2";
			//$FALL-THROUGH$
		case TESTBOT_LEVEL_2:
		{	// run an alpha-beta bot using the learned network as the static eval
			if(strategy==TESTBOT_LEVEL_2) { name = "TESTBOT2"; }
			teachNet = evalNet = GenericNetwork.loadNetwork(
					//"g:/temp/nn/F8PX-121-upto-f8px-121-4b.txt"
					//"g:/temp/nn/F8PX-121-upto-f8px-121-4a.txt"
					//"g:/temp/nn/F8PXA-25-hex5-raw-5.txt"
					//"g:/temp/nn/F8PXB-121-hex11-raw-3.txt"
					//"g:/temp/nn/F8PXB-25-xeh_5-raw-54.txt"
					//"g:/temp/nn/F8PXB-49-xeh_7-raw-17a.txt"	// 0.0002 learning rate
					//"g:/temp/nn/F8PXB-49-xeh_7-raw-3b.txt"	// 0.001 learning rate
					//"g:/temp/nn/F8PXB-49-xeh_7-1-85a.txt" // rms 68360=0.3935216907750162 control 7596=0.39891015698433085
					"g:/temp/nn/F8PXB-49-xeh_7-2-13a.txt"	//  rms 68768=0.37521682999399714 control 7644=0.37722832170526016
					);
			evalNet.saveNetwork("g:/temp/nn/testbot2save.txt","resaved");
			MONTEBOT = true;
			//minMovesPerSecond = 100000;	// train at a constant playout rate
			//maxMovesPerSecond = 100000;
			winRateWeight = neuroWinRateWeight;
			timePerMove = 1;
        	ALPHA = 0.5;	// standard alpha
        	BETA = 0.25;
        	CHILD_SHARE = 0.85;
			NEUROBOT = true;
		}
		break;
		case PURE_NEUROBOT_LEVEL:
		{	// run an alpha-beta bot using the learned network as the static eval
			teachNet = evalNet = GenericNetwork.loadNetwork(
					//"g:/temp/nn/F8PX-121-upto-f8px-121-4b.txt"
					//"g:/temp/nn/F8PX-121-upto-f8px-121-4a.txt"
					//"g:/temp/nn/F8PXB-25-hex5-raw-5.txt"
					//"g:/temp/nn/F8PXB-49-xeh_7-2-13a.txt"
					"g:/temp/nn/F8PXC-49-train-44.txt"
					);
			//evalNet.saveNetwork("g:/temp/nn/testbot2save.txt","resaved");
			MONTEBOT = false;
			MAX_DEPTH = 1;
			RANDOMIZE = false;
			//minMovesPerSecond = 100000;	// train at a constant playout rate
			//maxMovesPerSecond = 100000;
			winRateWeight = neuroWinRateWeight;
			deadChildOptimization = false;
        	ALPHA = 0.5;	// standard alpha
        	BETA = 0.25;
        	CHILD_SHARE = 0.85;
			NEUROBOT = true;
		}
		break;

		case SMARTBOT_LEVEL:
			name = "SMARTBOT";
			//$FALL-THROUGH$
		case TESTBOT_HYBRID_1:
			if(strategy==TESTBOT_HYBRID_1) { name = "TESTBOT_HYBRID_1"; }
			//$FALL-THROUGH$
		//case TESTBOT_LEVEL_1:
		{	// run an alpha-beta bot using the learned network as the static eval
			if(strategy==TESTBOT_LEVEL_1) { name = "TESTBOT1"; }
			teachNet = evalNet = GenericNetwork.loadNetwork(

					"g:/temp/nn/hex-5-raw.txt"
					);
			evalNet.saveNetwork("g:/temp/nn/testbot1save.txt","resaved");
			MONTEBOT = true;
			timePerMove = 30;
        	winRateWeight = neuroWinRateWeight*2;
        	deadChildOptimization = false;
        	ALPHA = 0.5;
        	BETA = 0.25;
        	CHILD_SHARE = 0.85;
			NEUROBOT = true;
		}
		break;
		
		case UCT_NEUROBOT_PLAY_LEVEL:
		{	// run an alpha-beta bot using the learned network as the static eval
			teachNet = evalNet = GenericNetwork.loadNetwork(
					"g:/temp/nn/F8PXB-25-hex5-raw-5.txt" // 
					);
			evalNet.saveNetwork("g:/temp/nn/testbot1save.txt","resaved");
			MONTEBOT = true;
			name = "Testbot1";
			//minMovesPerSecond = 20000;	// train at a constant playout rate
			//maxMovesPerSecond = 20000;
			minMovesPerSecond = 0;
        	winRateWeight = 0;
        	ALPHA = 0.5;
        	BETA = 0.25;
        	CHILD_SHARE = 0.85;

			NEUROBOT = true;
		}
		break;

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
		case UCT_NEUROBOT_TRAIN_LEVEL:
			name = "Neurobot";
			if(NEURO_MONTEBOT)
			{	// start from scratch
	        	timePerMove = 5;	// after partial training, 10, before 2
	        		evalNet = teachNet;
					if(evalNet ==null)
					{
					try {
					evalNet =
							GenericNetwork.loadNetwork(
							//"g:/temp/nn/f8pcb-upto-f8pcb-16.txt"	// best 3/9/2008
							//"g:/temp/nn/f8pcb-upto-f8pcb-17.txt"	// not significantly better
							//"g:/temp/nn/f8pcb-upto-f8pcb-18.txt" 	// not significantly better 
							//"g:/temp/nn/F8PCBW-121-upto-f8pcbw-2a.txt" // 22:16
									"g:/temp/nn/F8PCBW-neutral-121-upto-f8pcbw-1a.txt"
							);

					}
					catch (ErrorX x)
					{}
					if(evalNet==null)
						{	
						evalNet = 
								//createS4();
								//createF4();
								//createF8P();	// 4 filter layers
								//createF8PS();	// shallower, 5 filter layers
								//createF8PI()	// 4 filter layers with lastmove info after pooling
								//createF8PIB()
								//createF8PIC()	// 4 filter layers with lastmove at the head
								//createF8PID()	// 2 filter layers, deeper network
								//createF8PC()
								//createF8PCB()	// 4 filter layers, lastmove only at output
								//createF8PICW()
						createSimple()	// 7 fully connected layers
								;			
						}
					}
				((GenericNetwork)evalNet).testNetwork();
				teachNet = evalNet;
				minMovesPerSecond = 200000;	// train at a constant playout rate
				maxMovesPerSecond = 200000;
				MONTEBOT=true;
	        	verbose = 1;
	        	deadChildOptimization = false;
	        	storedChildLimit = 2*UCTMoveSearcher.defaultStoredChildLimit;
	        	winRateWeight = 0;
	        	ALPHA = 0.5;
	        	BETA = 0.25;
	        	CHILD_SHARE = 0.85;
				NEUROBOT = true;
			}
			else
			{
			if(level1Net==null)
				{
				// learn the static evaluation function used by dumbot
				// start fresh
				//level1Net = new GenericNetwork("OUTPUT_TRANSFER_FUNCTION LINEAR",49,20,20,2);
				//level8Net = new GenericNetwork("OUTPUT_TRANSFER_FUNCTION LINEAR",49,20,20,20,20,1);
				// resume learning
				int ncells = board.getCellArray().length;

				level1Net = GenericNetwork.loadNetwork("g:/temp/nn/hexnet1-"+ncells+".txt");
				level8Net = GenericNetwork.loadNetwork("g:/temp/nn/hexnet8-"+ncells+".txt");
				}
			
			MAX_DEPTH = 6;
			NEUROBOT = true;
			}
			break;			
		case NEUROBOT_HYBRID:
			name = "Neurobot_hybrid";
			//$FALL-THROUGH$
		case NEUROBOT_LEVEL:
			if(strategy==NEUROBOT_LEVEL) { name = "Neurobot"; }
			if(NEURO_MONTEBOT)
			{	// start from scratch
	        	timePerMove = 2;	// weak playouts
	        	evalNet = teachNet;
	        	if(evalNet ==null)
					{
					try {
					evalNet =
							GenericNetwork.loadNetwork(
									//"g:/temp/nn/hex5-raw.txt"
									//"g:/temp/nn/"+board.variation+"-raw.txt"
									//"g:/temp/nn/F8PXB-49-xeh_7-raw-3b.txt"	// 0.001 learning rate
									"g:/temp/nn/F8PXB-49-xeh_7-1-85a.txtx"
									);

					}
					catch (ErrorX x)
					{}
					if(evalNet==null)
						{	
						evalNet = 
								createF8PXC();	// 5 filter layers, lastmove only at output, + winrates
								;			
						}
					}
				((GenericNetwork)evalNet).testNetwork();
				teachNet = evalNet;
				MONTEBOT=true;
	        	verbose = 1;
	        	deadChildOptimization = false;
	        	storedChildLimit = 2*UCTMoveSearcher.defaultStoredChildLimit;
	        	winRateWeight = neuroWinRateWeight;
	        	ALPHA = 0.5;	// standard exploration
	        	BETA = 0.25;
	        	CHILD_SHARE = 0.85;
				NEUROBOT = true;
			}
			else
			{
			if(level1Net==null)
				{
				// learn the static evaluation function used by dumbot
				// start fresh
				//level1Net = new GenericNetwork("OUTPUT_TRANSFER_FUNCTION LINEAR",49,20,20,2);
				//level8Net = new GenericNetwork("OUTPUT_TRANSFER_FUNCTION LINEAR",49,20,20,20,20,1);
				// resume learning
				int ncells = board.getCellArray().length;

				level1Net = GenericNetwork.loadNetwork("g:/temp/nn/hexnet1-"+ncells+".txt");
				level8Net = GenericNetwork.loadNetwork("g:/temp/nn/hexnet8-"+ncells+".txt");
				}
			
			MAX_DEPTH = 6;
			NEUROBOT = true;
			}
			break;
			
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
            int randomn = RANDOMIZE ? ((board.moveNumber <= 6) ? (14 - 2*board.moveNumber) : 0) : 0;
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
                if((move!=null) && (level8Net!=null))
                {
                	double eval = move.evaluation();
        	        netCalculatePositive(level8Net,board);
                	level8Net.learn(new double[] {eval});
                	int ncells = board.getCellArray().length;
                	level8Net.saveNetwork("G:/temp/nn/"+level8Net.getName()+"-"+ncells+".txt","Level 8 network");
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
	 case NEUROBOT_LEVEL:
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
	case NEUROBOT_LEVEL:
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
	 case NEUROBOT_LEVEL:
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
        if(LIVE_TRAINING) { teachMonteNetwork(evalNet,monte_search_state); }
        else { printTrainingData(trainingData,monte_search_state); }
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
 	boolean win = board.winForPlayerNow(player);
 	if(win) { return(UCT_WIN_LOSS? 1.0 : 0.8+0.2/boardSearchLevel); }
 	boolean win2 = board.winForPlayerNow(nextPlayer[player]);
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
	public void runGame_play(ViewerProtocol viewer,commonRobot<?> otherBot)
	 {	
		int rep = 0;
		int wins[] = new int[2];
		beingMonitored = true;
	 	monitor = GameBoard;
		while(beingMonitored())
		{
		CommonMoveStack gameMoves = new CommonMoveStack();
		boolean firstPlayer = ((rep&1)==0);
		commonRobot<?> robots[] = new commonRobot[] 
									{ firstPlayer ? this : otherBot,
									  firstPlayer ? otherBot : this};
		RepeatedPositions positions = new RepeatedPositions();
		commonMove start = viewer.ParseNewMove("start p0", 0);
		gameMoves.push(start);
		GameBoard.doInit();
		GameBoard.Execute(start,replayMode.Replay);
	 	while(!GameBoard.GameOver() && beingMonitored())
	 	{	int who = GameBoard.whoseTurn();
	 		robots[who].PrepareToMove(who);
	 		commonMove m = robots[who].DoFullMove();
	 		gameMoves.push(m);
	 		GameBoard.Execute(m,replayMode.Replay); 
	 		positions.checkForRepetition(GameBoard,m);
	 	}
	 	wins[firstPlayer?0:1] += GameBoard.WinForPlayer(0)?1:0;
	 	wins[firstPlayer?1:0] += GameBoard.WinForPlayer(1)?1:0;
	 	if(beingMonitored()) { G.print("Wins "+getName()+" "+firstPlayer+" ="+wins[0]+" "+otherBot.getName()+" ="+wins[1]); }
	 	viewer.addGame(gameMoves,null);
	 	rep++;
	 	}
	 }
	
	//
	// play self and save training data with the currently selected bot
	//
	static int rep = 0;
	static BSDate startTime = new BSDate();
	StringBuilder trainingData = null;
	public void runGame_playself(ViewerProtocol viewer)
	 {	
		int wins[] = new int[2];
	 	beingMonitored = true;
	 	monitor = GameBoard;
	 	
		while(beingMonitored())
		{
		boolean firstPlayer = ((rep&1)==0);
		RepeatedPositions positions = new RepeatedPositions();
		commonMove start = viewer.ParseNewMove("start p0", 0);
		GameBoard.doInit();
		GameBoard.Execute(start,replayMode.Replay);
		trainingData = LIVE_TRAINING ? null : new StringBuilder();
 		String trainingMessage = trainingString();
	
	 	while(!GameBoard.GameOver() && beingMonitored())
	 	{	int who = GameBoard.whoseTurn();
	 		PrepareToMove(who);
	 		commonMove m = DoFullMove();
	 		if(m!=null) 
	 			{GameBoard.Execute(m,replayMode.Replay); 
	 			positions.checkForRepetition(GameBoard,m);
	 			}
	 	}
	 	wins[firstPlayer?0:1] += GameBoard.WinForPlayer(0)?1:0;
	 	wins[firstPlayer?1:0] += GameBoard.WinForPlayer(1)?1:0;
	 	rep++;
	 	if(beingMonitored() && trainingData!=null)
	 		{
	 		saveTrainingData(
	 				trainingData,
	 				"g:/temp/nn/train/hex-"+board.nCells()+"-"+startTime.DateString()+"-"+rep+".txt",
	 				trainingMessage
	 				);
	 		}
	 	}
	 }
	 // play neurobot against itself, train
	 // monte carlo priors based on each search
	 public void runRobotGameSelf(final ViewerProtocol v,BoardProtocol b,SimpleRobotProtocol otherBot)
	 {	
	 	BoardProtocol cloneBoard = b.cloneBoard();

	 	InitRobot(v,v.getSharedInfo(),cloneBoard,null,MONTEBOT_LEVEL);
	 	StopRobot();
	 	new Thread(new Runnable() {
	 		public void run() { runGame_playself(v); }
	 	}).start();
	 
	 }
	 // play two different robots against each other
	 // report wins and losses
	 public void runRobotGameDumbot(final ViewerProtocol v,BoardProtocol b,final SimpleRobotProtocol otherBot)
	   {	
		 	BoardProtocol cloneBoard = b.cloneBoard();
		 	// g:/temp/nn/F8PXB-49-xeh_7-1-85a.txt Wins TESTBOT2 false =12 MonteBot =64
		 	// train from 1000 games Wins Wins TESTBOT2 false =23 MonteBot =105 "g:/temp/nn/F8PXB-49-xeh_7-raw-3b.txt"
		 	// train from 1000 games Wins TESTBOT2 false =14 MonteBot =64 "g:/temp/nn/F8PXB-49-xeh_7-raw-17a.txt"
		 	// Wins Weak MonteBot false =8 MonteBot =62 using testbot_level_1 (1 sec) vs montebot (30 sec) on hex-7
		 	InitRobot(v,v.getSharedInfo(),cloneBoard,null,TESTBOT_LEVEL_2);
		 	otherBot.InitRobot(v,v.getSharedInfo(),cloneBoard,null,MONTEBOT_LEVEL);
		 	otherBot.StopRobot();
		 	StopRobot();

		 	new Thread(new Runnable() {
		 		public void run() { runGame_play(v,(commonRobot<?>)otherBot); }
		 	}).start();
	 }
	
// train a network with last-played layer as well as the standard input layer
public double trainNetworkP(GenericNetwork n,double inputsE[],double inputsB[],double inputsW[],
	 			double values[],double winrates[],boolean rmsonly,double color[])
	 	 {	double oldvals[] = n.calculateValues(n.getLayer("INE"),inputsE,n.getLayer("INB"),inputsB,n.getLayer("INW"),inputsW,n.getLayer("INC"),color);
	 	 	double setvals[] = new double[oldvals.length];
	 	 	double rms = 0;
	 	 	double rmsn = 0;
	 	 	for(int lim=values.length-1; lim>=0; lim--)
	 	 	{	double v = values[lim];
	 	 		if(v!=-1)
	 	 			{ double dif = oldvals[lim]-v;
	 	 			  setvals[lim]=v;
	 	 			  rms += dif*dif;
	 	 			  rmsn++;
	 	 			}
	 	 		else { 
	 	 			double dif = oldvals[lim];
	 	 			setvals[lim]=0; 
	 	 			rms += dif*dif;
	 	 			rmsn++;
	 	 			}
	 	 	}
	 	 	Layer winlayer = n.getLayer("WINRATE");
	 	 	double oldwins[] = winlayer!=null ? n.getValues(winlayer) : null;
	 	 	double setwins[] = oldwins!=null ? new double[oldwins.length] : null;
	 	 	if(setwins!=null)
	 	 	{
	 	 	for(int lim=winrates.length-1; lim>=0; lim--)
	 	 	{	double v = winrates[lim];
	 	 		if(v!=-1)
	 	 			{ double dif = oldwins[lim]-v;
	 	 			  setwins[lim]=(v+1)/2;	// normalize to 0.0-1.0
	 	 			  rms += dif*dif;
	 	 			  rmsn++;
	 	 			}
	 	 		else { 
	 	 			double dif = oldwins[lim];
	 	 			setwins[lim]=0.5; 
	 	 			rms += dif*dif;
	 	 			rmsn++;
	 	 			}
	 	 	}}
	 	 	if(rmsn>0)
	 	 	{
	 	 	//((GenericNetwork)n).learningRate=0.001;
	 	 	if(!rmsonly) 
	 	 		{ n.learn(setvals); 
	 	 		  if(winlayer!=null) { n.learn(winlayer,setwins); } 
	 	 		}
	 	 	//double []newvals = n.calculateValues(inputs);
	 	 	lastNetworkValues = null;//setvals;
	 	 	return(Math.sqrt(rms/rmsn));
	 	 	}
	 	 	return(0);
	 	 }
// train a network with last-played layer as well as the standard input layer
public double trainNetworkI(GenericNetwork n,double inputs[],double inputs2[],
			double values[],double winrates[],boolean rmsonly)
	 {	double oldvals[] = n.calculateValues(n.getLayer("IN"),inputs,n.getLayer("IN2"),inputs2);
	 	double setvals[] = new double[oldvals.length];
	 	double rms = 0;
	 	double rmsn = 0;
	 	for(int lim=values.length-1; lim>=0; lim--)
	 	{	double v = values[lim];
	 		if(v!=-1)
	 			{ double dif = oldvals[lim]-v;
	 			  setvals[lim]=v;
	 			  rms += dif*dif;
	 			  rmsn++;
	 			}
	 		else { 
	 			double dif = oldvals[lim];
	 			setvals[lim]=0; 
	 			rms += dif*dif;
	 			rmsn++;
	 			}
	 	}
	 	Layer winlayer = n.getLayer("WINRATE");
	 	double oldwins[] = winlayer!=null ? n.getValues(winlayer) : null;
	 	double setwins[] = oldwins!=null ? new double[oldwins.length] : null;
	 	if(setwins!=null)
	 	{
	 	for(int lim=winrates.length-1; lim>=0; lim--)
	 	{	double v = winrates[lim];
	 		if(v!=-1)
	 			{ double dif = oldwins[lim]-v;
	 			  setwins[lim]=(v+1)/2;	// normalize to 0.0-1.0
	 			  rms += dif*dif;
	 			  rmsn++;
	 			}
	 		else { 
	 			double dif = oldwins[lim];
	 			setwins[lim]=0.5; 
	 			rms += dif*dif;
	 			rmsn++;
	 			}
	 	}}
	 	if(rmsn>0)
	 	{
	 	//((GenericNetwork)n).learningRate=0.001;
	 	if(!rmsonly) 
	 		{ n.learn(setvals); 
	 		  if(winlayer!=null) { n.learn(winlayer,setwins); } 
	 		}
	 	//double []newvals = n.calculateValues(inputs);
	 	lastNetworkValues = null;//setvals;
	 	return(Math.sqrt(rms/rmsn));
	 	}
	 	return(0);
	 }
// train a network with only the standard input layer
public double trainNetwork(GenericNetwork n,double inputs[],double values[],double winrates[],boolean rmsonly)
{	double oldvals[] = n.calculateValues(n.getLayer("IN"),inputs);
	double setvals[] = new double[oldvals.length];
	double rms = 0;
	double rmsn = 0;
	for(int lim=values.length-1; lim>=0; lim--)
	{	double v = values[lim];
		if(v!=-1)
			{ double dif = oldvals[lim]-v;
			  setvals[lim]=v;
			  rms += dif*dif;
			  rmsn++;
			}
		else { 
			double dif = oldvals[lim];
			setvals[lim]=0; 
			rms += dif*dif;
			rmsn++;
			}
	}
 	Layer winlayer = n.getLayer("WINRATE");
 	double oldwins[] = winlayer!=null ? n.getValues(winlayer) : null;
 	double setwins[] = oldwins!=null ? new double[oldwins.length] : null;
 	if(setwins!=null)
 	{
 		for(int lim=winrates.length-1; lim>=0; lim--)
 		{	double v = winrates[lim];
 			if(v!=-1)
 				{ double dif = oldwins[lim]-v;
 				  setwins[lim]=(v+1)/2;		// normalized to 0-1
 				  rms += dif*dif;
 				  rmsn++;
 				}
 			else { 
 				double dif = oldwins[lim];
 				setwins[lim]=0; 
 				rms += dif*dif;
 				rmsn++;
 				}
 		}
		
 	}
	if(rmsn>0)
	{
		((GenericNetwork)n).learningRate=0.001;
	if(!rmsonly) 
		{ n.learn(setvals); 
		  if(winlayer!=null)
		  	{  n.learn(winlayer,setwins);
		  	}
	}
	//double []newvals = n.calculateValues(inputs);
	lastNetworkValues = null;//setvals;
	return(Math.sqrt(rms/rmsn));
	}
	return(0);
}

public void setInd(double []in,int ind,char piece)
{	
	switch(piece)
	{
		case 'w':
		case 'W': in[ind] = WhiteValue; break;
		case 'b':
		case 'B': in[ind] = BlackValue; break;
		default: G.Error("opps, bad color %s",piece);
		}

}

double [][] inm2 = new double[4][];
double [][] inm1 = new double[4][];

// parse the inputs of a training data entry
public void parseInputs(TrainingData td,GenericNetwork n,String str,int permutation)
{	int size = n.inputNetworkSize();
	double in[] = new double[size];
	boolean has[] = new boolean[size];
	Layer in2Layer = n.getLayer("IN2");
	double in2[] = (in2Layer!=null) ? new double[size] : null;
	double in3[] = null;
	double in2S[] = null;
	double in3S[] = null;
	

	boolean swapColor = false;
	int ncols = board.ncols;
	StringTokenizer tok = new StringTokenizer(str);
	td.playerToMove = G.IntToken(tok);	// the player, we don't need it.
	char color = G.CharToken(tok);
	int hits = 0;
	td.color = color;
	
	if(n.getLayer("INE")!=null)
	{	// positive inputs for empty, black and white
		in2 = new double[size];
		in3 = new double[size];
		AR.setValue(in,1);	// default to filled
		in2S = swapColor ? in3 : in2;
		in3S = swapColor ? in2 : in3;
	}
	
	while(tok.hasMoreTokens())
	{
		char col = G.CharToken(tok);
		int row = G.IntToken(tok);
		if((permutation&1)!=0) { row = ncols+1-row; }
		if((permutation&2)!=0) { col = (char)(('A'+ncols-1)-col+'A'); }
		char piece = G.CharToken(tok);
		int ind = getMoveIndex(n,col,row);
		hits++;
		has[ind] = true;
		if(in3!=null)
		{	in[ind] = 0;
			// positive input for empty black and white
			switch(piece)
			{
			case 'B': case 'b':
				in2S[ind] = 1;
				break;
			case 'W':
				in3S[ind] = 1;
				break;
			default:
				G.Error("Not expecting "+piece);
			}
		}
		else
		{
		setInd(in,ind,piece);
		if(in2!=null)
		{
		if(inm2[permutation]!=null)
		{
			if(inm2[permutation][ind]==0) { setInd(in2,ind,piece); }
		}
		else if(inm1[permutation]!=null)
		{
			if(inm1[permutation][ind]==0) { setInd(in2,ind,piece); }
		}}}
		
	}
	td.hasInputs = has;
	td.inputs = in;
	td.inputs2 = in2;
	td.inputs3 = in3;
	
	if(hits==0) { inm2[permutation] = null; inm1[permutation] = null; }
	else 
		{ 	inm2[permutation] = inm1[permutation];
			inm1[permutation] = in;
		}
}

// parse the values part of a training data entry
public void parseValues(TrainingData td,GenericNetwork n,String str,int permutation)
{	double values[] = n.getValues("OUT");
	double wins[] = n.getValues("WINRATE");
	StringTokenizer tok = new StringTokenizer(str);
	int ncols = board.ncols;
	AR.setValue(values, -1);
	int lastIndex = 0;
	int nvalues=0;
	while(tok.hasMoreTokens())
	{
		String column = tok.nextToken();
		if(SWAP.equalsIgnoreCase(column))
		{	
			String value = tok.nextToken();
			String pairvalue[] = G.split(value, ',');
			double v = Math.abs(G.DoubleToken(pairvalue[0]));
			double v2 = (pairvalue.length>1) ? G.DoubleToken(pairvalue[1]) : v; 
			CoordinateMap map = n.getCoordinateMap();
			lastIndex = 0;
			int ind = map.getMaxIndex();
			values[ind] = v;
			wins[ind] = v2;			
			nvalues++;
		}
		else {
			char col = column.charAt(0);
			int row = G.IntToken(tok);
			String value = tok.nextToken();
			String pairvalue[] = G.split(value, ',');
			double v = Math.abs(G.DoubleToken(pairvalue[0]));
			double v2 = (pairvalue.length>1) ? G.DoubleToken(pairvalue[1]) : v; 

			if((permutation&1)!=0) { row = ncols+1-row; }
			if((permutation&2)!=0) { col = (char)(('A'+ncols-1)-col+'A'); }
			int ind = getMoveIndex(n,col,row);
			values[ind] = v;
			wins[ind]=v2;
			lastIndex = ind;
			nvalues++;
			
		}
	}
	// special case, reported percentages are arbitrary where there is
	// only one choice.
	if(nvalues==1) { values[lastIndex]=0.99; }	
	td.values = values;
	td.winrates = wins;
}
public commonMove constructMove(GenericNetwork n,TrainingData current,TrainingData prev)
{	CoordinateMap map = n.getCoordinateMap();
	boolean hasIn[] = current.hasInputs;
	boolean prevIn[] = prev.hasInputs;
	for(int idx = hasIn.length-1; idx>=0; idx--)
	{
		if(hasIn[idx]!=prevIn[idx])
		{
			char col = map.getColForIndex(idx);
			int row = map.getRowForIndex(idx);
			G.Assert(idx==map.getIndex(col, row),"index inverse map");

			return (new XehMovespec(MOVE_DROPB,col,row,
					XehId.get(""+prev.color),
					prev.playerToMove));
			//m.putProp(NNTrainingData,prev);
		}
	}
	// no difference, must be a swap move
	//G.Assert(current.moveNumber==2,"should be move 2");
	return(new XehMovespec(SWAP,prev.playerToMove));

}
// parse and train on the data in one file
public double trainNetwork(GenericNetwork n,BufferedReader ps,String file,TDstack save,sgf_game moves) throws IOException
{	int rmsn = 0;
	double rms = 0;
	String line = null;
	int lastPerm = recordPermutations ? 3 : 0;
	int moveNumber = 0;
	TrainingData prevTd = null;
	sgf_node rootNode = moves.getRoot();
	sgf_node prevNode = rootNode;
	{
	sgf_node startNode = new sgf_node();
	commonMove start = new XehMovespec("Start p0",0);
	startNode.set_property(start.playerString(), start.longMoveString());
    prevNode.addElement(startNode, Where.atEnd);
    prevNode = startNode;
	}
	String prevLine2 = null;

	while((line=ps.readLine())!=null)
	{
		if(!"".equals(line))
		{
			if(line.charAt(0)!='/') 
			{
			String line2 = ps.readLine();
			if(line2!=null)
			{	
				for(int perm0 = 0;perm0<=lastPerm;perm0++)
				{
				TrainingData td = new TrainingData();
				int perm = perm0;
				parseInputs(td,n,line,perm);
				parseValues(td,n,line2,perm);
				td.moveNumber = moveNumber;
				td.file = file;
				save.push(td); 
				
				if(perm==0 && moves!=null)
				{
					TrainingData rawTD = new TrainingData();
					// construct a training data with no permutations or black/white swap
					parseInputs(rawTD,n,line,0);
					rawTD.moveNumber = moveNumber;
					if(prevTd!=null)
					{
					commonMove m = constructMove(n,rawTD,prevTd);
					sgf_node node = new sgf_node();
				    node.set_property(m.playerString(), m.longMoveString());
				    if(prevLine2!=null) { node.set_property(TrainingData,prevLine2); }
				    prevNode.addElement(node);
				    prevNode = node;
					{
						sgf_node doneNode = new sgf_node();
						commonMove done = new XehMovespec("done",m.player);
						doneNode.set_property(done.playerString(), done.longMoveString());
					    prevNode.addElement(doneNode, Where.atEnd);
					    prevNode = doneNode;
						}}
				prevTd = rawTD;
				}
				}
			prevLine2 = line2;
			moveNumber++;
			}
		}
		}
	}
	return(rmsn>0?0:rms/rmsn);
}

// train on one file, or collect the training data from one file
public sgf_game trainNetwork(GenericNetwork n,String file,TDstack ts,sgf_game theGame)
{	
	InputStream s=null;
	G.print("Train from "+file);
	try {
		s = new FileInputStream(new File(file));
		if(s!=null)
			{BufferedReader ps = new BufferedReader(new InputStreamReader(s));
			 trainNetwork(n,ps,file,ts,theGame);
			s.close();
			}
	} catch (IOException e) {
		throw G.Error("Load network error in %s %s",file,e);
	}
	return(theGame);
}

// run the training in its own thread.  Must edit the source folder
// for the current training set
public String trainingSequence = "";
public void runGame_train(ViewerProtocol v,BoardProtocol b,String from,int maxPass,boolean untilWorse)
{	boolean onceThrough = false;
	GameBoard = (XehBoard)b;
	board = (XehBoard)GameBoard.cloneBoard();
 	InitRobot(v,v.getSharedInfo(),board,null,RobotProtocol.NEUROBOT_LEVEL);
	
	int ncells = board.nCells();
	File dirfile = new File(
			from
			);
	String trainingName = dirfile.getName();
	File files[] = dirfile.listFiles();
	new Random().shuffle(files);
	TDstack data = new TDstack();
	GenericNetwork net = (GenericNetwork)evalNet;
	trainingSequence += from+" ";
	for(File f : files)
		{ // collect an internal representation of the training data
			String path = f.getPath();
			sgf_game theGame = new sgf_game();
			sgf_game moves = trainNetwork(net,path,data,theGame);
			sgf_node root = moves.getRoot();
			moves.set_short_name(f.getName());
			root.set_property(sgf_names.setup_property, ((commonCanvas)v).gameType());
		    root.set_property(sgf_names.game_property,((commonCanvas)v).sgfGameType());
			v.addGame(moves);
			G.print(""+moves);
		}
	G.print(""+files.length+" games in "+from+" using "+net.getName());
	int rmsn2 = 0;
	double rms2 = 0;
	double prev_rms2 = 999;
	int pass = 0;
	Network savedNet = null;
	do
	{
	int rmsn = 0;
	double rms = 0;
	savedNet = net.duplicate();
	if(rmsn2>0) { prev_rms2 = rms2/rmsn2; }
	rms2 = 0;
	rmsn2 = 0;
	pass++;
	int resn=0;
	int last = data.size();
		int reserve = onceThrough ? last-1 : ((last*9/10)/4)*4;			// use the last 10% as training validation
		for(int lim=0; lim<last; lim++)
		{	
			TrainingData d = data.elementAt(lim);
			boolean res = lim>=reserve;
			double rmsv;
			if(res) { resn++; }
			//net.learningRate = 0.0001;
			if(d.inputs3!=null)
			{
				rmsv = trainNetworkP(net,d.inputs,d.inputs2,d.inputs3,d.values,d.winrates,res,new double[] {d.color=='W'?1.0:0});
			}
			else if(d.inputs2!=null)
					{
					rmsv = trainNetworkI(net,d.inputs,d.inputs2,d.values,d.winrates,res);
					}
				else {
					rmsv = trainNetwork(net,d.inputs,d.values,d.winrates,res);
					}
			if(res)
			{
			rms2 += rmsv;
			rmsn2++;
			}
			else {
			rms += rmsv;
			rmsn++;
			}
		}
		String msg = "Pass "+pass+" rms "+rmsn+"="+(rms/rmsn)+" control "+resn+"="+(rms2/rmsn2);
		G.print(msg);
		net.testNetwork();
		net.saveNetwork("g:/temp/nn/"+net.getName()+"-"+ncells+"-"+trainingName+"-"+(pass%100)+".txt", "trained from "+trainingSequence+"\n//"+msg);
		 
	}
	while(pass<maxPass && (!untilWorse || ((rms2/rmsn2)<prev_rms2)));
	if((pass<maxPass) && (savedNet!=null)) { net.copyWeights(savedNet); }
	G.print("training done "+from+" using "+net.getName());
}
public void runRobotTraining(final ViewerProtocol v,BoardProtocol b,final SimpleRobotProtocol otherBot)
{
	new Thread(new Runnable() {
		public void run() 
		{ trainingSequence = "";
		//runGame_train(v,b,"g:/temp/nn/"+((XehBoard)b).variation+"-2/",500,false);
		runGame_train(v,b,"g:/temp/nn/train/",500,false);
		}
	}).start();
}
}
