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
package palago;

import lib.*;
import online.search.*;
import online.game.*;
import online.game.export.ViewerProtocol;
/** 
 * Palago uses alpha-beta only
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * 
 * some thoughts about the hierarchy of moves governing "next move" tactics.
 * an "opportunity" is a pattern that can construct a double threat.
 * an opponent's opponent's opportunity is a "blot"
 * 
 * 1) win directly (opportunity to win exists)
 * 2) defend against a direct threat (direct thread exists)
 * 3) convert an opportunity into a double threat (attack and win)
 * 4a) if multiple blots exist, make a direct attack
 * 4b) if a single blot exists, eliminate it or make a direct attack.
 * 4c) if no blots exist, construct a new opportunity or make a direct attack.
 * 4d) make some other strategically useful move. 
 * @author ddyer
 *
 */
public class PalagoPlay extends commonRobot<PalagoBoard> implements Runnable, PalagoConstants,
    RobotProtocol
    {
    private static final double VALUE_OF_WIN = 1000000.0;
    private boolean SAVE_TREE = false;				// debug flag for the search driver.  Uses lots of memory
    private int MAX_DEPTH = 5;						// search depth.
    private int SMARTBOT_DEPTH = 7;
    private int BESTBOT_DEPTH = 8;
    private int DUMBOT_DEPTH = 5;
    private int PALABOT_DEPTH = 4;
    private int WEAKBOT_DEPTH = 3;
	
    private double PALABOT_TIME_LIMIT = 3.0;
    private double DUMBOT_TIME_LIMIT = 3.0;
    private double SMARTBOT_TIME_LIMIT = 5.0;
    private double BESTBOT_TIME_LIMIT = 10.0;
    private double TIME_LIMIT = DUMBOT_TIME_LIMIT;
    private int MAX_RANDOM_DEPTH = 5;				// search depth when randomizing
    private static final boolean KILLER = false;	// if true, allow the killer heuristic in the search
    private static final double GOOD_ENOUGH_VALUE = VALUE_OF_WIN;	// good enough to stop looking
    
    private int Strategy = PALABOT_LEVEL;
    
    private int boardSearchLevel = 0;				// the current search depth

    /* constructor */
    public PalagoPlay()
    {
    }


/** Called from the search driver to undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	Palagomovespec mm = (Palagomovespec)m;
        board.UnExecute(mm);
        boardSearchLevel--;
    }
/** Called from the search driver to make a move, saving information needed to 
 * unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   Palagomovespec mm = (Palagomovespec)m;
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
    // this is the static evaluation used by the bots.  When testing a better
    // strategy, leave this untouched as a reference point.  The real meat
    // of the evaluation is the "blobs" class which is constructed by the
    // board.  The blobs structure aggregates the stones into chains and
    // keeps some track of how close the chains are to each other and to
    // the edges of the board.
    //
    double botEval(PalagoBoard evboard,int player,boolean print,int strategy)
    {	// note we don't need "player" here because the blobs variable
    	// contains all the information, and was calculated for the player
    	double val = 0.0;
    	int playerColor = evboard.getColorMap()[player];
     	LineInfoStack lines = evboard.scanForLoops();
    	if(lines!=null)
    	{
    	double threats = 0.0;
        int nthreats = 0;
        for(int i=0;i<lines.size();i++)
    	{
    	lineInfo ll = lines.elementAt(i);
       	String msg = "";
    	double ival = 0.0;
     	if(((strategy==SMARTBOT_LEVEL)||(strategy==BESTBOT_LEVEL)) && !ll.isLoop() && (ll.length>=2))
    	{
    	if((ll.incompleteCount(evboard,playerColor)>=0) || (ll.hoop_count<0))
	    	{
	    	// int incom = ll.incompleteCount(evboard,player);	
    		double dis = ll.next1.disSQ(ll.next2);

	    	if((dis==1)		// distance 1 is a single cell for both ends 
	    		&& (evboard.getState()==PalagoState.PLAY2_STATE)	// second move is next
	    		&&(evboard.whoseTurn==player)) 
	    	{	// a sure win given that we have another move. This is a fairly
	    		// special case for palago where you get 2 moves in a row
	    	 dis = 0.2;
	    	}

	    	if(print) { msg += " dis="+dis+" "; }
	    	double inc = 60.0/(dis);
	    	ival += inc;
	    	if(dis<=2) { threats+=inc; nthreats++; }
	    	}
     	if((strategy==SMARTBOT_LEVEL)||(strategy==BESTBOT_LEVEL))
    	{
    	if(ll.twotip_count>0) 	// computed as a side effect
	    	{
	    	if(print) { msg += "tt="+ll.twotip_count; }
	    	ival += ll.twotip_count*10;
	    	}
    	if(strategy == BESTBOT_LEVEL)
    	{
    	if(ll.fourtip_count>0)
    	{	if(print) { msg += "ff="+ll.fourtip_count; }
    		ival += ll.fourtip_count*20; 
    	}
    	if(ll.hole_count>0)
    	{	if(print) { msg += "h="+ll.hole_count; }
			ival += ll.hole_count*30; 
    	}
    	if(ll.squeeze_count>0)
    	{	if(print) { msg += "s="+ll.squeeze_count; }
			ival += ll.squeeze_count*40; 
    	}}}
   	
     	if(print) { System.out.println(""+player+ll+msg+" = "+ival) ; }
    	val += ival;
    	}
      	if(nthreats>1) { ival += threats; }
    	}}
     	return(val);
    }

    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    double ScoreForPlayer(PalagoBoard evboard,int player,boolean print)
    {	
     	boolean win = evboard.WinForPlayerNow(player);
     	// make wins in fewer moves look slightly better. Nothing else matters.
     	if(win) 
     		{ double val = VALUE_OF_WIN+(1.0/(1+boardSearchLevel));
     		  if(print) {System.out.println(" win = "+val); }
     		  return(val); 
     		}
     	// if the position is not a win, then estimate the value of the position
     	double val = botEval(evboard,player,print,Strategy);

    	// we're going to subtract two values, and the result must be inside the
    	// bounds defined by +-WIN
    	G.Assert((val<(VALUE_OF_WIN/2))&&(val>=(VALUE_OF_WIN/-2)),"value out of range");
     	return(val);
    }
    
    /** this is called from the search driver to evaluate a particular position. The driver
     * calls List_of_Legal_Moves, then calls Make_Move/Static_Evaluate_Position/UnMake_Move
     *  for each and sorts the result to preorder the tree for further evaluation
     */
    // TODO: refactor static eval so GameOver is checked first
    public double Static_Evaluate_Position(	commonMove m)
    {	int playerindex = m.player;
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,nextPlayer[playerindex],false);
        // don't dilute the value of wins with the opponent's positional score.
        // this avoids the various problems such as the robot comitting suicide
        // because it's going to lose anyway, and the position looks better than
        // if the oppoenent makes the last move.  Technically, this isn't needed
        // if there is no such thing as a suicide move, but the logic
        // is included here because this is supposed to be an example.
        
        // unusually in Palago, if both players have a win, the player moving loses.
        // so check for the opponent win first.
        if(val1>=VALUE_OF_WIN) { return(-val1); }
        if(val0>=VALUE_OF_WIN) { return(val0); }
        return(val0-val1);
    }
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {
            PalagoBoard evboard = GameBoard.cloneBoard();
            double val0 = ScoreForPlayer(evboard,FIRST_PLAYER_INDEX,true);
            double val1 = ScoreForPlayer(evboard,SECOND_PLAYER_INDEX,true);
            evboard.showLines();
            System.out.println("Eval is "+ val0 +" "+val1+ " = " + (val0-val1));
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
        GameBoard = (PalagoBoard) gboard;
        board = GameBoard.cloneBoard();
        // strategy with be 0,1,2 for Dumbot, Smartbot, Bestbot
        Strategy = strategy;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy %s",strategy);
        case WEAKBOT_LEVEL:
        	MAX_DEPTH = WEAKBOT_DEPTH;
        	TIME_LIMIT = DUMBOT_TIME_LIMIT;
        	break;
        case DUMBOT_LEVEL:  
        	MAX_DEPTH = DUMBOT_DEPTH;
        	TIME_LIMIT = DUMBOT_TIME_LIMIT;
        	break;
        case SMARTBOT_LEVEL: 
        	MAX_DEPTH = SMARTBOT_DEPTH;
        	TIME_LIMIT = SMARTBOT_TIME_LIMIT;
        	break;
        case BESTBOT_LEVEL: 
        	MAX_DEPTH = BESTBOT_DEPTH; 
        	TIME_LIMIT = BESTBOT_TIME_LIMIT;
        	break;
        case PALABOT_LEVEL:	
        	MAX_DEPTH = PALABOT_DEPTH;
        	TIME_LIMIT = PALABOT_TIME_LIMIT;
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
	//use this for a friendly robot that shares the board class
	board.copyFrom(GameBoard);
    board.sameboard(GameBoard);	// check that we got a good copy.  Not expensive to do this once per move

}
/** search for a move on behalf of player p and report the result
 * to the game.  This is called in the robot process, so the normal
 * game UI is not encumbered by the search.
 */
 public commonMove DoAlphaBetaFullMove()
    {
        Palagomovespec move = null;
        try
        {
 
            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new Palagomovespec("Done", board.whoseTurn);
            }

            // it's important that the robot randomize the first few moves a little bit.
            int randomn = RANDOMIZE ? ((board.moveNumber <= 4) ? (10 - board.moveNumber) : 0) : 0;
            boardSearchLevel = 0;

            int depth = MAX_DEPTH;	// search depth
            double dif = 15.0;		// stop randomizing if the value drops this much
            // if the "dif" and "randomn" arguments to Find_Static_Best_Move
            // are both > 0, then alpha-beta will be disabled to avoid randomly
            // picking moves whose value is uncertain due to cutoffs.  This makes
            // the search MUCH slower so depth ought to be limited
            // if ((randomn>0)&&(dif>0.0)) { depth--; }
            // for games where there are no "fools mate" type situations
            // the best solution is to use dif=0.0;  For games with fools mates,
            // set dif so the really bad choices will be avoided
            if((dif>0) && (randomn>0)) { depth = Math.min(depth, MAX_RANDOM_DEPTH); }
            else if(((Strategy==WEAKBOT_LEVEL)||(Strategy==DUMBOT_LEVEL)) && (board.chips_on_board<16)) { depth++; }	// give dummy a starting boost
            Search_Driver search_state = Setup_For_Search(depth, TIME_LIMIT);
            search_state.verbose = verbose;
            search_state.save_all_variations = SAVE_TREE;
            search_state.good_enough_to_quit = GOOD_ENOUGH_VALUE;
            search_state.allow_killer = KILLER;
            search_state.save_digest=false;	// debugging only
            search_state.check_duplicate_digests = false; 	// debugging only

           if (move == null)
            {	// randomn takes the a random element among the first N
            	// to provide variability.  The second parameter is how
            	// large a drop in the expectation to accept.  For some games this
            	// doesn't really matter, but some games have disasterous
            	// opening moves that we wouldn't want to choose randomly
                move = (Palagomovespec) search_state.Find_Static_Best_Move(randomn,dif);
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
