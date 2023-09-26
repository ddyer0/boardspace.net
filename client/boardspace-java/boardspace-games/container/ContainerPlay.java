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
package container;

import lib.*;
import container.ContainerBoard.ContainerGoalSet;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

/** 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * 
 * MONTEBOT notes.  Looks pretty promising, but leakage of actual goal cards in final score
 * needs to be reconsidered.  Also specialized search times for different phases.
 * 
 * @author ddyer
 *
 */
public class ContainerPlay extends commonRobot<ContainerBoard> implements Runnable, ContainerConstants,
    RobotProtocol
{   
	static final double VALUE_OF_WIN = 1000000.0;
	boolean SAVE_TREE = false;				// debug flag for the search driver
    boolean KILLER = false;					// probably ok for all games with a 1-part move
    //double TIME_LIMIT = 0.5;		// 30 seconds
    boolean TIME_LIMIT = false;
    static final int WEAKBOT_DEPTH = 7;
    static final int STANDARD_DEPTH = 9;
     int MAX_DEPTH = STANDARD_DEPTH;
     /* strategies */
    
    enum bot_type { V4Bot, V5Bot, V6Bot, V7Bot };
    bot_type strategy = bot_type.V4Bot; 
     
    int boardSearchLevel = 0;				// the current search depth
    int evalForPlayer = 0;
    int extra_search_depth = 0;
    
    /* constructor */
    public ContainerPlay()
    {
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
        return(current>=(max+extra_search_depth));
   }
/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	ContainerMovespec mm = (ContainerMovespec)m;
    	boardSearchLevel--;
    	extra_search_depth -= mm.extra_search_depth;
    	board.UnExecute(mm);
     }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   ContainerMovespec mm = (ContainerMovespec)m;
        board.RobotExecute(mm);
        mm.extra_search_depth=0;
        switch(strategy)
        {	default: throw G.Error("Not expecting strategy %s",strategy);
       	case V4Bot:
            	if((mm.op==MOVE_FROM_TO)
        			&& (mm.source==ContainerId.LoanLocation))
        		{	// combat the horizon effect - loans make delay the next real move
        		extra_search_depth+=2;
        		mm.extra_search_depth = 2;
        		}
            	else if (mm.op==MOVE_BID)
            		{ extra_search_depth += 1;
            		  mm.extra_search_depth = 1; 
            		}
        	break;
       		case V5Bot:
            case V6Bot:
            case V7Bot:
            	if((mm.op==MOVE_FROM_TO)
            			&& (mm.source==ContainerId.LoanLocation))
            		{	// combat the horizon effect - loans make delay the next real move
            		extra_search_depth+=2;
            		mm.extra_search_depth = 2;
            		}
                	else if (mm.op==MOVE_BID)
                		{ extra_search_depth += 1;
                		  mm.extra_search_depth = 1; 
                		}

        	switch(board.board_state)
        	{
        	case PLAY1_STATE:
        		// this stops the lookahead when the player changes.
        		if(board.whoseTurn!=evalForPlayer) { extra_search_depth -=100; mm.extra_search_depth=-100; }
        		break;
        	default: 
        		break;
        	}
        }

        boardSearchLevel++;
    }

    public ContainerBoard.ContainerGoalSet robotGoalSet()
    {
    	ContainerBoard.playerBoard bd = board.getPlayer(evalForPlayer);
    	return(bd.possibleGoalSets[0]);
    }
    
/** return an enumeration of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {   
    	return(board.GetListOfMoves(boardSearchLevel,this));
    }
    public int overbidAmount()
    {	
    		return(0);
    }
    

    
    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    double ScoreForPlayer(ContainerBoard evboard,int player,boolean print)
    {	// avoid a subtle form of "reality leakage" by supplying this robot's goalset
    	// so the win he predicts will be based on his possibly flawed estimate of the
    	// other player's goals.
     	boolean win = evboard.WinForPlayer(player,robotGoalSet());	
    	if(win) { return(VALUE_OF_WIN+(1.0/(1+boardSearchLevel))); }
    	switch(strategy)
    	{
    	default: throw G.Error("Not expecting strategy %s",strategy);
    	case V4Bot: return(evboard.ScoreForPlayer4(this,player,print));
    	case V5Bot: 
    	case V6Bot:
    		return(evboard.ScoreForPlayer5(this,player,print));
    	case V7Bot:
    		return(evboard.ScoreForPlayer6(this,player,print));
    	}

    }
    double game_stage()
    {
    	return(game_stage(estimated_turns_remaining()));
    }
    public double game_stage(double turns)
    {	switch(strategy)
    	{
    	default: throw G.Error("Not expecting strategy %s",strategy);
    	case V4Bot: 	return(board.game_stage_v4(turns));
    	case V5Bot:
    	case V6Bot:
    	case V7Bot:
    		return(board.game_stage_v5(turns));
   
    	}
    }
    double estimated_turns_remaining()
    {	switch(strategy)
    	{
    	default: throw G.Error("Not expecting strategy %s",strategy);
    	case V4Bot: 	return(board.estimated_turns_remaining_v4());
    	case V5Bot:
    	case V6Bot:
    	case V7Bot:
    		return(board.estimated_turns_remaining_v5());
    	}	
    }
    void getAuctionMoves(CommonMoveStack  all,int who,ContainerCell source,ContainerChip chip,ContainerBoard.ContainerGoalSet goalSet)
    {
    	switch(strategy)
    	{
       	default: throw G.Error("Not expecting strategy %s",strategy);
    	case V4Bot: 	board.getAuctionMoves_v4(all,who,source,chip,this,goalSet);
    		break;
    	case V5Bot:
    	case V6Bot:
    	case V7Bot:
    		board.getAuctionMoves_v5(all,who,source,chip,this,goalSet);
    		break;
  	
    	}
    }
    int estimatedIslandGoodValue(ContainerGoalSet goalSet,ContainerChip good,int who)
    {
    	switch(strategy)
    	{
       	default: throw G.Error("Not expecting strategy %s",strategy);
    	case V4Bot: 	
    		return(board.estimatedIslandGoodValue_v4(goalSet,good));
    	case V5Bot:
    	case V6Bot:
    	case V7Bot:
    		return(board.estimatedIslandGoodValue_v5(goalSet,good,who));
  	
    	}
    }
    /**
     * this is it! just tell me that the position is worth.  
     */
    public double Static_Evaluate_Position_AgainstMe(int playerindex)
    {
    	// note on multiplayer game evaluations.  This logic evaluates
    	// every move for every player according to how well it scores
    	// against the top level player.  This is effectively the paranoid
    	// view, that "everyone else is just out to get me", which is
    	// not quite the most natural. 
    	//
    	// however, the obvious alternative, to evaluate each player's moves
    	// according to how well is scores against the field leads to perverse
    	// results - the top level player will play moves that are terrible for
    	// himself if those moves let one of his opponents advance against the
    	// leading player.
    	//
    	// the completely correct solution would split the difference, let
    	// the current player score against the field, but the caller score
    	// his choices as most favorable to himself.
    	//
   
        double val0 = ScoreForPlayer(board,evalForPlayer,false);
        int pl2 = board.getNextPlayer(evalForPlayer);
        double val1 = ScoreForPlayer(board,pl2,false);
        
        for(int nx = 2; nx<board.nPlayers(); nx++)
        { pl2 = board.getNextPlayer(pl2);
          double val3 = ScoreForPlayer(board,pl2,false);
          if(val3>val1) { val1=val3; }
        }
        // don't dilute the value of wins with the opponent's positional score.
        // this avoids the various problems such as the robot committing suicide
        // because it's going to lose anyway, and the position looks better than
        // if the opponent makes the last move.  Technically, this isn't needed
        // if there is no such thing as a suicide move, but the logic
        // is included here because this is supposed to be an example.
        if(val0>=VALUE_OF_WIN) { return(val0); }
        if(val1>=VALUE_OF_WIN) { return(-val1); }
        return( (evalForPlayer==playerindex) ? (val0-val1) : (val1-val0) );
    }
    
    // seek the end of the best_move chain for multiplayer games
    public commonMove targetMoveForEvaluation(commonMove cm)
    {	commonMove leaf = cm.best_move();
    	commonMove v = cm;
    	while(leaf!=null) { v = leaf; leaf=leaf.best_move(); }
    	return(v);
    }
 
    //
    // rescore the position for a different player.  The underlying
    // assertion here is that the player component scores are accurate
    // ie; the players don't score themselves differently if they are
    // the player to move. 
    //
    public double reScorePosition(commonMove m,int forplayer)
    {	return(m.reScorePosition(forplayer,VALUE_OF_WIN));
    }

    public double Static_Evaluate_Position(commonMove m)
    {	
     	int playerindex = m.player;
     	int nplay = board.nPlayers();
     	commonMPMove mm = (commonMPMove)m;
     	
     	mm.setNPlayers(nplay);
    	
    	for(int i=0;i<nplay; i++)
    	{	mm.playerScores[i] = ScoreForPlayer(board,i,false);
    	}
    	return(mm.reScorePosition(playerindex,VALUE_OF_WIN));
    }
 
void showFairBids(ContainerBoard evboard)
    {	if(evboard.board_state==ContainerState.AUCTION_STATE)
    	{
    	int auctioneer = evboard.playerRequestingBid();
    	ContainerBoard.ContainerGoalSet set = robotGoalSet();
    	double stage = game_stage();
    	for(int i=0;i<evboard.players_in_game;i++)
    		{	ContainerBoard.playerBoard bd = evboard.getPlayer(i);
    			int max = bd.cash;
    			evboard.fairBid_v5(auctioneer,i,max,true,set,stage);
    		}
    	}
    }
    
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {
    	ContainerBoard evboard = (ContainerBoard)GameBoard.cloneBoard();
    	StaticEvalPosition(evboard,evboard.whoseTurn,true);
    	showFairBids(evboard);
    }
    public double StaticEvalPosition(ContainerBoard evboard,int who,boolean print)
    {
     	int nplay = evboard.nPlayers();
     	double scoreFor0 = ScoreForPlayer(evboard,0,print);
    	double scoreFor1 = ScoreForPlayer(evboard,1,print);
    	double scoreFor2 = ScoreForPlayer(evboard,2,print);
    	double scoreFor3 = (nplay>3)? ScoreForPlayer(evboard,3,print):0.0;
    	double scoreFor4 = (nplay>4)? ScoreForPlayer(evboard,4,print):0.0;
    	double val = 0;
    	double max = 0;
    	switch(who)
    	{
    	case 0:	
    		val = scoreFor0;
    		max = Math.max(scoreFor1,scoreFor2);
       		if(nplay>3) { max = Math.max(max,scoreFor3); }
    		if(nplay>4) { max = Math.max(max,scoreFor4); }
    		break;
    	case 1:
    		val = scoreFor1;
    		max = Math.max(scoreFor0,scoreFor2);
    		if(nplay>3) { max = Math.max(max,scoreFor3); }
    		if(nplay>4) { max = Math.max(max,scoreFor4); }
    		break;
    	case 2: 
    		val = scoreFor2;
    		max = Math.max(scoreFor0,scoreFor1);
       		if(nplay>3) { max = Math.max(max,scoreFor3); }
    		if(nplay>4) { max = Math.max(max,scoreFor4); }
    		break;
    	case 3:
    		val = scoreFor3;
    		max = Math.max(Math.max(scoreFor0,scoreFor1),scoreFor2);
    		if(nplay>4) { max = Math.max(max,scoreFor4); }  		
    		break;
    	case 4:
    		val = scoreFor4;
    		max = Math.max(Math.max(scoreFor0,scoreFor1),Math.max(scoreFor3,scoreFor2));
    		break;
		default:
			break;
   		
    	}

        if(max>=VALUE_OF_WIN) { val=0.0; }
        
        if(print) { System.out.println("Eval for "+who+" is "+ (val-max)); }
        return(val-max);
    }



/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.
 */
 public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
        String evaluator, int strat)
    {
        InitRobot(newParam, info, strat);
        GameBoard = (ContainerBoard) gboard;
        board = (ContainerBoard)GameBoard.cloneBoard();
        switch(strat)
        {
        case WEAKBOT_LEVEL:
        	MAX_DEPTH = WEAKBOT_DEPTH;
        	strategy = bot_type.V7Bot;
        	break;
        case DUMBOT_LEVEL:
        	MAX_DEPTH = STANDARD_DEPTH;
        	strategy = bot_type.V7Bot;
        	break;

        default: throw G.Error("Not expecting strategy %s",strat);
        }
    }
    /** copy the game board, in preparation for a search */
    public void InitBoardFromGame()
    {
        board.copyFrom(GameBoard);
        board.makeFastLoans = true;	// robot doesn't need to consider the round robin
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
	 ContainerMovespec move = null;

        try
        {
        	BoardProtocol clone = board.cloneBoard();
        	board.sameboard(clone);
        	
            // it's important that the robot randomize the first few moves a little bit.
            int randomn = RANDOMIZE 
            				? ((board.moveNumber <= 4) ? (10 - board.moveNumber) : 0)
            				: 0;
            int depth = MAX_DEPTH;	// search depth
            if((board.board_state==ContainerState.REPRICE_WAREHOUSE_STATE)||(board.board_state==ContainerState.REPRICE_FACTORY_STATE))
            {
            	depth -= 2;
            }
            double dif = 2.0;		// stop randomizing if the value drops this much
            // if the "dif" and "randomn" arguments to Find_Static_Best_Move
            // are both > 0, then alpha-beta will be disabled to avoid randomly
            // picking moves whose value is uncertain due to cutoffs.  This makes
            // the search MUCH slower so depth ought to be limited
            // if ((randomn>0)&&(dif>0.0)) { depth--; }
            // for games where there are no "fools mate" type situations
            // the best solution is to use dif=0.0;  For games with fools mates,
            // set dif so the really bad choices will be avoided
            
            for(int i=board.players_in_game-1; i>=0; i--) 
            	{ //revalue the ship goods before the search
            	board.getPlayer(i).current_ship_island_value = -100; 
            	}
            boardSearchLevel = 0;
            evalForPlayer = board.whoseTurn;
            extra_search_depth = 0;
            Search_Driver search_state = Setup_For_Search(depth,TIME_LIMIT );
            search_state.save_all_variations = SAVE_TREE;
            search_state.allow_killer = KILLER;
            search_state.verbose=verbose;			// debugging
            search_state.save_top_digest = true;
            search_state.save_digest=false;	// debugging only
            search_state.check_duplicate_digests = false; 	// debugging only
//search_state.stop_at_eval_clock = 32576;
//search_state.stop_at_eval_clock = 10000005;
            if (move == null)
            {	move = (ContainerMovespec) search_state.Find_Static_Best_Move(randomn,dif);
            
            board.sameboard(clone);
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
            if(move.op==MOVE_DONE) { G.doDelay(1000); }
            // normal exit with a move
            return (move);
        }

        continuous = false;
        // abnormal exit
        return (null);
    }


 }