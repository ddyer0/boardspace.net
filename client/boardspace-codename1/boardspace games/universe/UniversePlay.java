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
package universe;


import java.util.Hashtable;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;
interface SubsetVisitor<TYPE>
{
	public boolean visitSubset(TYPE[] v);
}
//
//this is a hack to visit all subsets of size n of type
//
class SubsetEnumerator<TYPE>
{	SubsetVisitor<TYPE> coroutine = null;
	TYPE result[] = null;
	TYPE from[] = null;
	// call enumerateSubsets with "fr" an array of TYPE, to 
	// call "visitSubset" with all possible arrays of subsets of
	// size n, where n is the size of res.
	public void enumerateSubsets(TYPE[]fr,TYPE[]res,SubsetVisitor<TYPE> tell)
	{
		coroutine = tell;
		result = res;
		from = fr;
		if(enumerateSubsets(from.length-1,result.length-1)) { return; }
	}
	public boolean enumerateSubsets(int fromIndex,int resultIndex)
	{
		if(resultIndex<0) { return coroutine.visitSubset(result); }
		if(fromIndex<0) { return(true); }
		while(fromIndex>=0)
		{
		result[resultIndex] = from[fromIndex];
		fromIndex--;
		if(enumerateSubsets(fromIndex,resultIndex-1)) { return(true); }
		}
		return(false);
	}
}

class liteMove 
{	long val = 0;
	liteMove bestReply = null;
	// constructor 
	int getPlayer() { return((int)(val&0x3)); }
	public int hashCode() { return((int)val); }
	public boolean equals(Object other)
	{
		if(other instanceof liteMove) { liteMove ot = (liteMove)other; return(ot.val == val); }
		return(false);
	}
	void setReply(liteMove m)
	{
		bestReply = m;
	}
	UniverseMovespec getReply()
	{
		if(bestReply!=null) { return(bestReply.getMove()); }
		return(null);
	}
	long getValueKey(UniverseMovespec m) 
	{
		return(m.player 
		| ((m.op&0x3ff)<<2)
		| ((m.from_col-'A')<<12) 
		| (m.from_row<<16) 
		| m.rotation<<21 
		| ((m.to_col-'A')<<24)
		| ((long)m.to_row)<<30);
	}
	liteMove(UniverseMovespec m)
	{	
		val = getValueKey(m);
		//UniverseMovespec mp = getMove(val);
		//G.Assert(m.equals(mp),"mismatches");
	}
	UniverseMovespec getMove() { return(getMove(val)); }
	UniverseMovespec getMove(long key)
	{
		UniverseMovespec m = new UniverseMovespec();
			m.player = (int)(key&3);
			m.op = (int)(key>>2)&0x3ff;
			if((m.op&0x200)!=0) { m.op |= -1<<9; }
			m.from_col = (char)('A' + ((key>>12)&0xf));
			m.from_row = (int)((key>>16)&0x1f);
			m.rotation = (int)((key>>21)&0x7);
			m.to_col = (char)('A'+((key>>24)&0x3f));
			m.to_row = (int)(key>>30);
		//G.Assert(getValueKey(m)==key,"reconstruction failed");
		return(m);
	}
}
/** 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * @author ddyer
 *
 */
public class UniversePlay extends commonRobot<UniverseBoard> implements Runnable, UniverseConstants,
    RobotProtocol,SubsetVisitor<UniverseCell>
{ 	Hashtable<liteMove,liteMove> replies = new Hashtable<liteMove,liteMove>();
	boolean SAVE_TREE = false;				// debug flag for the search driver
	int MOVE_LIMIT = 1500;					// limit on number of moves to consider
    int TIMEPERMOVE = 10;
    boolean ENABLE_LASTREPLY_CACHE = false;
    int reply_move_hits = 0;
    int reply_move_misses = 0;
    int reply_move_invalid = 0;
    int PLAYRATE = 500;
    boolean WEIGHTBYSIZE = false;
    int TREE_DEPTH = 1;
    int SEMI_HEAVY_PLAYOUTS = 0;
    boolean UCT_WIN_LOSS = true;
    boolean SKIP_LEADING_EVALS = false;		// use for polysolver only
    
    int STRATEGY = DUMBOT_DEPTH;
	UniverseViewer viewer = null;
	boolean STORED_CHILD_LIMIT_STOP = false; 
    static final int DUMBOT_DEPTH = 3;
    static final int GOODBOT_DEPTH = 4;
    static final int BESTBOT_DEPTH = 6;
	static final double VALUE_OF_WIN = 1000000.0;
    int MAX_DEPTH = BESTBOT_DEPTH;
    double ALPHA = 0.5;
     /* strategies */
    int boardSearchLevel = 0;				// the current search depth
    int lastCohortSize = 0;
    /* constructor */
    public UniversePlay()
    {
    }
    public UniversePlay(UniverseViewer v)
    {	viewer = v;
    }


/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	UniverseMovespec mm = (UniverseMovespec)m;
    	boardSearchLevel--;
    	board.UnExecute(mm);
     }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   UniverseMovespec mm = (UniverseMovespec)m;
        board.RobotExecute(mm);
        boardSearchLevel++;
        if(SKIP_LEADING_EVALS) 
        	{ m.set_local_evaluation(Static_Evaluate_PolySolver_Position(m)); 
        	}

    }

    
    /**
     * get a random move by selectong a random one from the full list.  This method
     * cab be overridden if there is a better way.
     * 
     */
    public commonMove Get_Random_Move(Random rand)
    {	switch(board.rules)
    	{
    		case Diagonal_Blocks_Duo:
    		case Diagonal_Blocks:
    		case Blokus_Duo:
     		case Diagonal_Blocks_Classic:
     		case Blokus:
     		case Blokus_Classic:
    			{	if(STRATEGY!=MONTEBOT_LEVEL)
    				{
    				commonMove m = board.Get_Random_Diagonal_Move(rand,WEIGHTBYSIZE);
    				if(boardSearchLevel<SEMI_HEAVY_PLAYOUTS)
    				{	// interesting experiment, but this made the program weaker
    					commonMove m2 = board.Get_Random_Diagonal_Move(rand,WEIGHTBYSIZE);
    					double val = super.Static_Evaluate_Move(m);
    					double val2 = super.Static_Evaluate_Move(m2);
    					if(val2>val) { m = m2; }
    				}
    				return(m);
    				}
    				else
    				{
    					return(super.Get_Random_Move(rand));
    				}
    			}
    			
    			
    		default: return(super.Get_Random_Move(rand));
    	}
    }
/** return an enumeration of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {   CommonMoveStack  moves = board.GetListOfMoves();
    	lastCohortSize = moves.size();
    	switch(board.rules)
    	{
		case Nudoku_6x6:
		case Nudoku_9x9:
    	case PolySolver_9x9:
    	case PolySolver_6x6:
    		if(boardSearchLevel==0)
    		{ 	Random r = new Random(board.Digest());
    			moves.shuffle(r);
    		}
    		break;
    	default:
    		break;
    	}
    	return(moves);
    }
    
    
    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    double ScoreForPlayer(UniverseBoard evboard,int player,boolean print)
    {	
    	switch(evboard.rules)
    	{
    	default: throw G.Error("Not implemented");

    	case PolySolver_6x6:
    	case PolySolver_9x9:
    	case Nudoku_6x6:
    	case Nudoku_12:
    	case Nudoku_11:
    	case Nudoku_10:
    	case Nudoku_9:
    	case Nudoku_8:
    	case Nudoku_7:
    	case Nudoku_6:
    	case Nudoku_5:
    	case Nudoku_4:
    	case Nudoku_3:
    	case Nudoku_2:
    	case Nudoku_1:
    	case Nudoku_1_Box:
    	case Nudoku_9x9:
    	case Nudoku_2_Box:
    	case Nudoku_3_Box:
    	case Nudoku_4_Box:	// 3 x 2 in 2x2
    	case Nudoku_5_Box:
    	case Nudoku_6_Box:
    	case Sevens_7:
	
	     	boolean win = evboard.WinForPlayerNow(player);
	    	if(win && (player==0))
	    		{ 	G.Assert(board.isValidSudoku(),"not valid");
	    			viewer.recordSolution(evboard,search_driver);
	    			return(VALUE_OF_WIN+(1.0/(1+boardSearchLevel))); 
	    		}
	    	return(evboard.ScoreForPolySolverPlayer(player,print));
	    
    	case Universe:
    	case Pan_Kai:
    	case Diagonal_Blocks:
    	case Diagonal_Blocks_Duo:
    	case Blokus_Duo:
		case Blokus:
 		case Blokus_Classic:
 		case Diagonal_Blocks_Classic:
 			board.calculateEstScores();
    		return(board.estScoreForPlayer(player));
    		
    	}

    }

    
    public double normalizedRescore(UniverseMovespec move,int forplayer)
    {	double max = move.maxPlayerScore();
    	return(Math.min(1.0,Math.max(-1.0,move.playerScores[forplayer]-max)));
    }
    
    /**
     * for UCT search, return the normalized value of the game, with a penalty
     * for longer games so we try to win in as few moves as possible.  Values
     * must be normalized to -1.0 to 1.0
     */
    public double NormalizedScore(commonMove lastMove)
    { 	UniverseMovespec move = (UniverseMovespec)lastMove;
   		int player = move.player;
   		int lim = board.nPlayers();
   		move.setNPlayers(lim);
   		if(board.getState()==UniverseState.GAMEOVER_STATE)
    	{   
	      	if(UCT_WIN_LOSS)
	      	{
	      		for(int i=0; i<lim; i++)
		      	{
		      		move.playerScores[i] = board.win[i] ? 1.0 : 0.0; 
		      	}
	      	}
	      	else
	      	{	
	     		for(int i=0; i<lim; i++)
		      	{
		      		move.playerScores[i] = board.ScoreForPlayer(i)/81.0; 
		      	}
	      	}
	      	
    	}
    	else
    	{	// game not over, make a guess
    		board.calculateEstScores();
     		for(int i=0; i<lim; i++)
	      	{
	      		move.playerScores[i] = board.estScoreForPlayer(i); 
	      	}
     	}
    	double vv = normalizedRescore(move,player);
    	return(vv);
    }
    /**
     * this is it! just tell me that the position is worth.  
     */
    public double Static_Evaluate_PolySolver_Position(commonMove m)
    {	int playerindex = m.player;
        return(ScoreForPlayer(board,playerindex,false));
    } 

    /**
     * this is it! just tell me that the position is worth.  
     */
    public double Static_Evaluate_Position(commonMove m)
    {	
     	int playerindex = m.player;
     	int nplay = board.nPlayers();
     	commonMPMove mm = (commonMPMove)m;
     	
     	mm.setNPlayers(nplay);
    	board.calculateEstScores();

    	for(int i=0;i<nplay; i++)
    	{	mm.playerScores[i] = board.estScoreForPlayer(i);
    	}
    	return(mm.reScorePosition(playerindex,VALUE_OF_WIN));
    }
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {
    	UniverseBoard evboard = (UniverseBoard)GameBoard.cloneBoard();
        double val0 = ScoreForPlayer(evboard,FIRST_PLAYER_INDEX,true);
        double val1 = ScoreForPlayer(evboard,SECOND_PLAYER_INDEX,true);
        if(val1>=VALUE_OF_WIN) { val0=0.0; }
        System.out.println("Eval is "+ val0 +" "+val1+ " = " + (val0-val1));
    }

public void initPolySolverRobot(ExtendedHashtable info,int strategy)
{
    switch(strategy)
    {case DUMBOT_LEVEL:
    	MONTEBOT = false;
    	MAX_DEPTH = 999;
    	MOVE_LIMIT = 999999;
    	SKIP_LEADING_EVALS = true;
    	break;
    default: throw G.Error("Not expecting strategy %s",strategy);
    }
	
}
/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.
 */
 public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
        String evaluator, int strategy)
    {
	 UniverseBoard gb = (UniverseBoard) gboard;
	 InitRobot(newParam, info, strategy);
	 GameBoard = (UniverseBoard)gboard;
     board = (UniverseBoard)GameBoard.cloneBoard();
     STRATEGY = strategy;
     STORED_CHILD_LIMIT_STOP = false;
     WEAKBOT = (strategy==WEAKBOT_LEVEL);
     switch(gb.rules)
	 {
	 default: throw G.Error("Rules %s not handled", gb.rules);
	 case Blokus_Duo:
	 case Blokus:
 	 case Blokus_Classic:
		 MONTEBOT = DEPLOY_MONTEBOT;
		 UCT_WIN_LOSS = true;	// experiments with pentobi show this is much better
		 MOVE_LIMIT = 500;
		 MAX_DEPTH = 100;
		 ENABLE_LASTREPLY_CACHE = false;
		 SEMI_HEAVY_PLAYOUTS = 0;		// failed experiment
		 TIMEPERMOVE = 15;
		 PLAYRATE = 80000;
		 WEIGHTBYSIZE = true;
		 ALPHA = 0.7;
		 break;
	 case Diagonal_Blocks_Classic:
 	 case Diagonal_Blocks:
	 case Diagonal_Blocks_Duo:
		 MONTEBOT = DEPLOY_MONTEBOT;
		 MOVE_LIMIT = 500;
		 MAX_DEPTH = 100;
		 ENABLE_LASTREPLY_CACHE = false;
		 PLAYRATE = 80000;
		 UCT_WIN_LOSS = true;	// experiments with pentobi show this is much better
		 TIMEPERMOVE = 15; //strategy==0?15:30;
		 ALPHA = 0.7;
		 
		 break;
	 case Universe:
	 case Pan_Kai:
	 case Phlip:
		 MONTEBOT = DEPLOY_MONTEBOT;
		 PLAYRATE = 500;
		 MAX_DEPTH = 999;
		 break;
	case PolySolver_6x6:
	case PolySolver_9x9:
	case Nudoku_6x6:
	case Nudoku_12:
	case Nudoku_11:
	case Nudoku_10:
	case Nudoku_9:
	case Nudoku_8:
	case Nudoku_7:
	case Nudoku_6:
	case Nudoku_5:
	case Nudoku_4:
	case Nudoku_3:
	case Nudoku_2:
	case Nudoku_1:
	case Nudoku_1_Box:
	case Nudoku_9x9:
	case Nudoku_2_Box:
	case Nudoku_3_Box:
	case Nudoku_4_Box:	// 3 x 2 in 2x2
	case Nudoku_5_Box:
	case Nudoku_6_Box:
	case Sevens_7:

		 initPolySolverRobot(info,strategy);
		 break;
	 }
  
    }
    /** copy the game board, in preparation for a search */
    public void InitBoardFromGame()
    {
        board.copyFrom(GameBoard);
        if(GameBoard.getState()==UniverseState.PUZZLE_STATE)
        {
        	GameBoard.setState(UniverseState.PLAY_STATE);
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
	 UniverseMovespec move = null;
	 if(board.getState()==UniverseState.PUZZLE_STATE) 
	 	{
		if(board.WinForPlayerNow(0)) { continuous=false; return(null); }
	 	board.setState(UniverseState.PLAY_STATE); 
	 	}
        try
        {

            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new UniverseMovespec("Done", board.whoseTurn);
            }

            // it's important that the robot randomize the first few moves a little bit.
            int randomn = 0;
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
            boardSearchLevel = 0;

            Search_Driver search_state = Setup_For_Search(depth, false);
            search_state.save_all_variations = SAVE_TREE;
            search_state.single_choice_optimization = false;
            search_state.allow_killer = false;
            search_state.verbose=verbose;			// debugging
            search_state.save_top_digest = true;	// always on as a background check
            search_state.save_digest=false;	// debugging only
            search_state.check_duplicate_digests = false; 	// debugging only

            if (move == null)
            {
                move = (UniverseMovespec) search_state.Find_Static_Best_Move(randomn,dif);
                if(move!=null)
                {
                switch(board.rules)
                {
            	case PolySolver_6x6:
            	case PolySolver_9x9:
            	case Nudoku_6x6:
            	case Nudoku_12:
            	case Nudoku_11:
            	case Nudoku_10:
            	case Nudoku_9:
            	case Nudoku_8:
            	case Nudoku_7:
            	case Nudoku_6:
            	case Nudoku_5:
            	case Nudoku_4:
            	case Nudoku_3:
            	case Nudoku_2:
        		case Nudoku_1_Box:
        		case Nudoku_2_Box:
           		case Nudoku_3_Box:
        		case Nudoku_4_Box:	// 3 x 2 in 2x2
           		case Nudoku_5_Box:
        		case Nudoku_6_Box:
        		case Nudoku_1:
    			case Sevens_7:
            	case Nudoku_9x9:
            		// if the result is not a solution, reject it.
            		if(move.evaluation() < VALUE_OF_WIN)
            		{	move = null;
            			G.print("No solutions");
             		}
					break;
				default: break;
                }}
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
 
 /** default do nothing */
 public commonMove Get_Reply_Move(commonMove last)
 {	if(ENABLE_LASTREPLY_CACHE && (last!=null))
 	{
	liteMove m = new liteMove((UniverseMovespec)last);
	liteMove existing = replies.get(m);
	if(existing!=null)
		{
		UniverseMovespec reply = existing.getReply();
		if(reply!=null)
			{
			if(board.validMove(reply)) 
				{ reply_move_hits++;
				  //G.print("Reply "+last+" -> "+reply);
				  return(reply); 
				}
			reply_move_invalid++;
			return(null);
			}
		}
	else
		{
		reply_move_misses++;
		replies.put(m,m);
		}
 	}
	return(null);

 }
 /** default is do nothing.  For reference, here is a sample sequence
  * 
  *  M P1[onboard B 4 1 J 10]
M P0[onboard A 5 6 D 7]
M P1[onboard B 10 3 I 3]
M P0[onboard A 13 5 A 9]
M P1[onboard B 7 1 K 6]
M P0[onboard A 19 3 E 12]
M P1[onboard B 2 1 L 9]
M P0[onboard A 11 2 F 10]
M P1[onboard B 9 0 E 11]
M P0[onboard A 0 0 G 7]
M P1[onboard B 17 3 N 9]
M P0[onboard A 3 0 C 3]
M P1[onboard B 20 1 F 3]
M P0[onboard A 12 4 G 9]
M P1[onboard B 16 0 J 12]
M P0[onboard A 2 1 A 6]
M P1[onboard B 3 1 D 2]
M P0[onboard A 1 0 G 4]
M P1[onboard B 11 6 L 1]
M P0[resign]
Update -1.0 0 P1[onboard B 11 6 L 1] P0[resign]
Update -1.0 0 P0[onboard A 1 0 G 4] P1[onboard B 11 6 L 1]
Update -1.0 0 P1[onboard B 3 1 D 2] P0[onboard A 1 0 G 4]
Update -1.0 0 P0[onboard A 2 1 A 6] P1[onboard B 3 1 D 2]
Update -1.0 0 P1[onboard B 16 0 J 12] P0[onboard A 2 1 A 6]
Update -1.0 0 P0[onboard A 12 4 G 9] P1[onboard B 16 0 J 12]
Update -1.0 0 P1[onboard B 20 1 F 3] P0[onboard A 12 4 G 9]
Update -1.0 0 P0[onboard A 3 0 C 3] P1[onboard B 20 1 F 3]
Update -1.0 0 P1[onboard B 17 3 N 9] P0[onboard A 3 0 C 3]
Update -1.0 0 P0[onboard A 0 0 G 7] P1[onboard B 17 3 N 9]
Update -1.0 0 P1[onboard B 9 0 E 11] P0[onboard A 0 0 G 7]
Update -1.0 0 P0[onboard A 11 2 F 10] P1[onboard B 9 0 E 11]
Update -1.0 0 P1[onboard B 2 1 L 9] P0[onboard A 11 2 F 10]
Update -1.0 0 P0[onboard A 19 3 E 12] P1[onboard B 2 1 L 9]
Update -1.0 0 P1[onboard B 7 1 K 6] P0[onboard A 19 3 E 12]
Update -1.0 0 P0[onboard A 13 5 A 9] P1[onboard B 7 1 K 6]
Update -1.0 0 P1[onboard B 10 3 I 3] P0[onboard A 13 5 A 9]
Update -1.0 0 P0[onboard A 5 6 D 7] P1[onboard B 10 3 I 3]
Update -1.0 0 P1[onboard B 4 1 J 10] P0[onboard A 5 6 D 7]

  *  */
 public void Update_Last_Reply(double value,int forPlayer,commonMove prev,commonMove cur)
 {	if(ENABLE_LASTREPLY_CACHE && (cur.player==forPlayer))
 	{
	UniverseMovespec mm = (UniverseMovespec)prev;
	liteMove key = new liteMove(mm);
	liteMove lite = replies.get(key);
	if(lite!=null)
		{ 
		lite.setReply((value>0) ? new liteMove((UniverseMovespec)cur) : null);
		}
 	}
 }
 
 public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
  	try {
       if (board.DoneState())
        { // avoid problems with gameover by just supplying a done
            move = new UniverseMovespec("Done", board.whoseTurn);
        }
        else 
        {
        //	RandomMoveQA qa = new RandomMoveQA();
        // 	qa.runTest(this, new Random(),10,false);
        // 	qa.report();
         	
        board.robotBoard = true;
        replies.clear();
        reply_move_hits = 0;
        reply_move_misses = 0;
        reply_move_invalid = 0;
        
        // it's important that the robot randomize the first few moves a little bit.
        double randomn = (RANDOMIZE && (board.moveNumber <= 6)) ? 0.02/board.moveNumber : 0.0;
        UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this);
        monte_search_state.save_top_digest = true;	// always on as a background check
        monte_search_state.save_digest=false;	// debugging only
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.timePerMove = TIMEPERMOVE;		// 20 seconds per move
        monte_search_state.random_moves_per_second = WEAKBOT ? PLAYRATE/20 : PLAYRATE;
        //monte_search_state.uct_tree_depth = TREE_DEPTH;
        monte_search_state.verbose = verbose;
        monte_search_state.move_limit = MOVE_LIMIT;
        monte_search_state.stored_child_limit = 100000;
        monte_search_state.alpha = ALPHA;
        monte_search_state.final_depth = MAX_DEPTH;
        monte_search_state.sort_moves = false;
        monte_search_state.uct_sort_depth = 2;
        monte_search_state.sort_weight =  1.0;
        monte_search_state.only_child_optimization = true;
        monte_search_state.dead_child_optimization = true;
        monte_search_state.simulationsPerNode = 1;
        monte_search_state.node_expansion_rate = 0.05;
        monte_search_state.randomize_uct_children = true;
        monte_search_state.maxThreads = DEPLOY_THREADS;

        monte_search_state.blitz = false;
        monte_search_state.stored_child_limit_stop = STORED_CHILD_LIMIT_STOP;
        monte_search_state.terminalNodeOptimization = terminalNodeOptimize;
        move = monte_search_state.getBestMonteMove();
        if(ENABLE_LASTREPLY_CACHE && (verbose>0))
        {
        	G.print("Reply move hit "+reply_move_hits+" Invalid "+reply_move_invalid+" miss "+reply_move_misses);
        }
        //G.print("M "+move);
        }
 		}
      finally { ; }
      if(move==null) { continuous = false; }
     return(move);
 }
//
// play elements for the polyomino solver
//

 public int Evaluate_And_Sort_Moves(Search_Driver dr,Search_Node sn,commonMove mvec[])
 {	
 	if(SKIP_LEADING_EVALS)
 		{for(int lim = mvec.length-1; lim>=0; lim--) 
 	{
 		mvec[lim].set_depth_limited(commonMove.EStatus.EVALUATED);
 		mvec[lim].set_local_evaluation(0.0); 
 	}}
 	else {
 		super.Evaluate_And_Sort_Moves(dr,sn,mvec);
 	}
 	return(mvec.length);
 }
 public boolean visitSubset(UniverseCell subset[])
 {	 board.copyFrom(GameBoard);
	 int startingSize = board.ScoreForPlayer(0)+board.ScoreForPlayer(1);
	 board.bestScore = startingSize;
	 board.mainBoard = GameBoard;
	 String rem = "";
	 // remove the designated pieces from the board and solve.
	 for(UniverseCell cc : subset)
	 { UniverseCell c = board.getCell(cc);
	   UniverseChip ch = c.topChip();
	   board.pickObject(c,ch.isomerIndex());
	   UniverseCell rr[] = board.rack[board.whoseTurn];
	   for(UniverseCell dest : rr)
	   {
		   if(dest.topChip()==null) 
		   { board.dropObject(dest); 	// back in the rack
		     break; 
		   }
	   }
	   rem += ""+c+"="+ch+" ";
	 }
	 board.cachedMovesValid = false;
	 board.setState(UniverseState.PLAY_STATE);
	 board.acceptPlacement();
	 G.print("Remove "+rem);
	 //the solution prints the moves it makes when it find a solution, but
	 //more interesting it presents the result to the main game board. It's
	 //intended to put a breakpoint there to pause.
 	 board.findWorstGame(rem);	
	 return(false);
 }
 public void runGame_train(ViewerProtocol v,final BoardProtocol b)
 {	CellStack stack = new CellStack();
 	GameBoard = (UniverseBoard)b;
 	board = (UniverseBoard)GameBoard.cloneBoard();
 	try {
 	for(UniverseCell c = GameBoard.allCells; c !=null; c=c.next)
 	{	UniverseChip top = c.topChip();
 		if(top!=null
 				&& (top.color==GameBoard.playerColor[GameBoard.whoseTurn]) 
 				&& GameBoard.isPrimaryLocation(c))
 		{
 			stack.push(c);
 		}}
 	UniverseCell all[] = stack.toArray();
 	SubsetEnumerator<UniverseCell>enumerator = new SubsetEnumerator<UniverseCell>();
 	for(int i=1;i<all.length;i++)
 	{	UniverseCell res[] = new UniverseCell[i];
 		G.print("Solutions less "+i);
 		enumerator.enumerateSubsets(all, res,this);
 	}
 	} 
 	finally 
 	{	
 	}
 }
 // this is a hack to find lowest scoring games of blokus duo, though
 // the basic concept would work for any of the omino games.  The initial
 // version tried to brute force from the top, which it seems will take
 // geologic time.  This version starts with the best known solution
 // (or whatever board position you start with) and looks for nearby
 // solutions by removing subsets of the solution and solving for a
 // better solution.   It still takes a long time.  With Mikael Piagets
 // blokus duo "46" solution, i found no nearby better solutions with
 // up to 3 removed pieces. 4 would take forever.
 public void runRobotTraining(final ViewerProtocol v,final BoardProtocol b,SimpleRobotProtocol otherBot)
 {
 	new Thread(new Runnable() {
 		public void run() { runGame_train(v,b); }
 	}).start();

 }
 
 // pentobi midgame
 //Val: 0.55, Cnt: 19198, Sim: 16529, Nds: 310620, Tm: 00:01, Sim/s: 31724
 //Len: 13.2 dev=1.7 min=7 max=17, Dp: 6.8 dev=3.0 min=1 max=17
 //Mov: 162, Sco: 0.2 dev=3.1, LGR: 26.4%
 //MaxCnt 22464
 //Reusing 187 nodes (0.1% count=0 time=0.000000)
 // blokus-duo speec 
 // Moves per second          =   596 (initial)
 // Moves per second          =   632 (remove duplicate call to canaddchip
 // Moves per second          =  2114 (precalculate diagonal points)
 // Moves per second          =  2220 (optimize out placement test)
 // Moves per second          =  3551 (low level optimize canAddDiagonalChip)
 // Moves per second          =  3241
 // Moves per second          =  3620 shortcut in move generator
 // Moves per second          =  7643 invert move generator
 // Moves per second          = 50005 generate random moves directly
 // Moves per second          = 54977 optimize gameover test 
 // more tweaks 				55597
 //								26738 with heavy playouts
 //								72503 (move 3, keep both diagonals cache)
 //								99538 with maintained dual caches
 // first move blokus 139210
 //                   161615
 /**
  * HEAVY SIMULATIONS         = 37758
Actual Time               = 15.0
Total Random Moves        = 948042
Total Traverse Moves      = 80074
Total UCT nodes           = 17867
Moves per second          = 68541
Sorts: 392 0.8
Child limited at  = 1000275

Randomize true Debug true
Thinking with alpha: 0.7 and TimePerMove: 15s
HEAVY SIMULATIONS         = 31702
Actual Time               = 15.0
Total Random Moves        = 635545
Total Traverse Moves      = 68877
Total UCT nodes           = 15172
Moves per second          = 46961
Sorts: 392 0.7
Child limited at  = 1000348

  */
 }