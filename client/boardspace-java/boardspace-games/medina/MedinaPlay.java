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
package medina;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;


/** 
 * medina uses alpha-beta only
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * @author ddyer
 *
 */
public class MedinaPlay extends commonRobot<MedinaBoard> implements Runnable, 
    RobotProtocol
{   
	private static final double VALUE_OF_WIN = 1000000.0;

	private boolean SAVE_TREE = false;				// debug flag for the search driver
	private boolean KILLER = false;					// probably ok for all games with a 1-part move
    
	private static final int WEAKBOT_DEPTH = 4;
	private static final int DUMBOT_DEPTH = 5;
	private static final int BESTBOT_DEPTH = 7;
	private int MAX_DEPTH = BESTBOT_DEPTH;
     /* strategies */
	private boolean DUMBOT = false;
	private int boardSearchLevel = 0;				// the current search depth
	private int evalForPlayer = 0;
	private double TIME_LIMIT = 0.5;		// 30 seconds
    //boolean TIME_LIMIT = false;
    /* constructor */
    public MedinaPlay()
    {
    }


/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	MedinaMovespec mm = (MedinaMovespec)m;
    	boardSearchLevel--;
    	board.UnExecute(mm);
     }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   MedinaMovespec mm = (MedinaMovespec)m;
        board.RobotExecute(mm);
        boardSearchLevel++;
    }

/** return an enumeration of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {   return(board.GetListOfMoves());
    }
    
    

    
    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    double ScoreForPlayer(MedinaBoard evboard,int player,boolean print)
    {	
     	boolean win = evboard.WinForPlayer(player);
    	if(win) { return(VALUE_OF_WIN+(1.0/(1+boardSearchLevel))); }
    	return(DUMBOT
    			? evboard.ScoreForPlayer(player,print)
    			: evboard.ScoreForPlayer(player,print));

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

    
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {
    	MedinaBoard evboard = (MedinaBoard)GameBoard.cloneBoard();
    	int nPlayers = GameBoard.nPlayers();
    	double scoreFor0 = ScoreForPlayer(evboard,0,true);
    	double scoreFor1 = ScoreForPlayer(evboard,1,true);
    	double scoreFor2 = (nPlayers>=3) ? ScoreForPlayer(evboard,2,true) : -1000;
    	double scoreFor3 = (nPlayers>=4) ? ScoreForPlayer(evboard,3,true) : -1000;
    	double val = 0;
    	double max = 0;
    	int who = evboard.whoseTurn;
    	switch(who)
    	{
    	case 0:	
    		val = scoreFor0;
    		max = Math.max(scoreFor3,Math.max(scoreFor1,scoreFor2));
    		break;
    	case 1:
    		val = scoreFor1;
    		max = Math.max(scoreFor0,scoreFor2);
    		break;
    	case 2: 
    		val = scoreFor2;
    		max = Math.max(scoreFor3,Math.max(scoreFor0,scoreFor1));
    		break;
    	case 3:
    		val =scoreFor3;
    		max = Math.max(Math.max(scoreFor0,scoreFor1),scoreFor2);
    		break;
		default:
			break;
    	}

        if(max>=VALUE_OF_WIN) { val=0.0; }
        
        System.out.println("Eval for "+who+" is "+ (val-max));
    }


/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.
 */
 public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
        String evaluator, int strategy)
    {
        InitRobot(newParam, info, strategy);
        GameBoard = (MedinaBoard) gboard;
        board = (MedinaBoard)GameBoard.cloneBoard();
        switch(strategy)
        {
        case WEAKBOT_LEVEL:
        	MAX_DEPTH = WEAKBOT_DEPTH;
        	DUMBOT = true;
        	break;
        case DUMBOT_LEVEL:
        	MAX_DEPTH = DUMBOT_DEPTH;
         	DUMBOT=true;
        	break;


        default: throw G.Error("Not expecting strategy %s",strategy);
        }
    }
    /** copy the game board, in preparation for a search */
    public void InitBoardFromGame()
    {
        board.copyFrom(GameBoard);
    }
/** search for a move on behalf of player p and report the result
 * to the game.  This is called in the robot process, so the normal
 * game UI is not encumbered by the search.
 */
 public void PrepareToMove(int playerIndex)
 {	InitBoardFromGame();
 }
 
 public commonMove DoAlphaBetaFullMove()
    {
	 MedinaMovespec move = null;

        try
        {

            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new MedinaMovespec("Done", board.whoseTurn);
            }

            // it's important that the robot randomize the first few moves a little bit.
            int randomn = RANDOMIZE 
            				? ((board.moveNumber <= 4) ? (10 - board.moveNumber) : 0)
            				: 0;
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
            evalForPlayer = board.whoseTurn;
            Search_Driver search_state = Setup_For_Search(depth, TIME_LIMIT);
            search_state.save_all_variations = SAVE_TREE;
            search_state.allow_killer = KILLER;
            search_state.verbose=verbose;			// debugging
            search_state.save_top_digest = true;	// always on as a background check
            search_state.save_digest=false;			// debugging only, robot doesn't maintain the digest well enough
            search_state.check_duplicate_digests = false; 	// debugging only

            if (move == null)
            {	move = (MedinaMovespec) search_state.Find_Static_Best_Move(randomn,dif);
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