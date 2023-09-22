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
package chess;



import chess.ChessConstants.Variation;
import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

/** 
 * Chess uses Alpha-beta only
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * @author ddyer
 *
 */
public class ChessPlay extends commonRobot<ChessBoard> implements Runnable
{   private boolean SAVE_TREE = false;				// debug flag for the search driver
	private boolean KILLER = false;					// probably ok for all games with a 1-part move
	private static final int DUMBOT_DEPTH = 8;
	private static final int ULTIMA_DUMBOT_DEPTH = 7;
	private static final int BESTBOT_DEPTH = 10;
	private static final double VALUE_OF_WIN = 1000000.0;
	private int MAX_DEPTH = BESTBOT_DEPTH;

	// in games like chess, where the robot auto-adds a done, this is needed so "save current variation" works correctly
	public commonMove getCurrentVariation() { return getCurrent2PVariation(); }
	
	/** constructor.  Must be zero argument so copying robot players will work.
     * 
     */
    public ChessPlay()
    {
    }
    /**
     * this is called after creating a new instance of the robot, when
     * creating a clone to use in monte carlo search.
     */
   // public void copyFrom(commonRobot<ChessBoard> from)
   // {
    //	super.copyFrom(from);
    	// perform any additional copying not handled by the standard method
   // }
    //
    // if using parallel search (ie MONTEBOT) copy any simple
    // variables that need to be copied when spawning a new player.
    //
    //public RobotProtocol copyPlayer(String newName)
    //{	RobotProtocol v = super.copyPlayer(newName);
    	// copy any instance variables that need to be copied.
    	//ChessPlay c = (ChessPlay)v;
  //  	return(v);
  //  	
    //}

/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	ChessMovespec mm = (ChessMovespec)m;
    	board.UnExecute(mm);
     }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   ChessMovespec mm = (ChessMovespec)m;
        board.RobotExecute(mm,false);
    }

/** return an enumeration of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {   CommonMoveStack all = board.GetListOfMoves();
    	if(board.robotDepth==0)
    	{
    		board.filterStalemateMoves(all);
    		if(all.size()==0) { all=board.GetListOfMoves(); }
    	}
    	return(all);
    }
    
    

    

    
    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    double ScoreForPlayer(ChessBoard evboard,int player,boolean print)
    {	
     	boolean win = evboard.winForPlayerNow(player);
     	double ss = evboard.ScoreForPlayer(player,print);
     	// including the raw evaluation in the win value is intended to
     	// avoid stalemates disguised as a win, as in king-vs-rook endings
    	if(win) { return(VALUE_OF_WIN+ss/100+(1.0/(1+board.robotDepth))); }
    	return(ss);

    }
    /**
     * this re-evaluates the current position from the viewpoint of forplayer.
     * for 2 player games this is to trivially negate the value, but for multiplayer
     * games it requires considering multiple player's values.
     */
    public double reScorePosition(commonMove m,int forplayer)
    {	return(m.reScorePosition(forplayer,VALUE_OF_WIN));
    }
 
    /**
     * this is it! just tell me that the position is worth.  
     */
    public double Static_Evaluate_Position(commonMove m)
    {	int playerindex = m.player;
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,playerindex^1,false);
        // don't dilute the value of wins with the opponent's positional score.
        // this avoids the various problems such as the robot committing suicide
        // because it's going to lose anyway, and the position looks better than
        // if the opponent makes the last move.  Technically, this isn't needed
        // for lyngk because there is no such thing as a suicide move, but the logic
        // is included here because this is supposed to be an example.
        if(val0>=VALUE_OF_WIN) { return(val0); }
        if(val1>=VALUE_OF_WIN) { return(-val1); }
        return(val0-val1);
    }
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {
    	ChessBoard evboard = (ChessBoard)GameBoard.cloneBoard();
        double val0 = ScoreForPlayer(evboard,FIRST_PLAYER_INDEX,true);
        double val1 = ScoreForPlayer(evboard,SECOND_PLAYER_INDEX,true);
        if(val1>=VALUE_OF_WIN) { val0=0.0; }
        System.out.println("Eval is "+ val0 +" "+val1+ " = " + (val0-val1));
    }


/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.
 */
 public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
        String evaluator, int strategy)
    {
        InitRobot(newParam, info, strategy);
        GameBoard = (ChessBoard) gboard;
        board = (ChessBoard)GameBoard.cloneBoard();
        boolean ultima = board.variation==Variation.Ultima;
        switch(strategy)
        {
        case WEAKBOT_LEVEL:
        	WEAKBOT = true;
			//$FALL-THROUGH$
		case DUMBOT_LEVEL:
        	// implements an unwind mode monte bot.
        	// faster and kicks ass.
        	MAX_DEPTH = ultima ? ULTIMA_DUMBOT_DEPTH : DUMBOT_DEPTH;
         	MONTEBOT = false;
        	break;

        default: throw G.Error("Not expecting strategy %s",strategy);
        }
    }
    /** copy the game board, in preparation for a search */
    public void InitBoardFromGame()
    {
        board.copyFrom(GameBoard);
        board.robotBoard = true;
        board.acceptPlacement();
    }
/** search for a move on behalf onf player p and report the result
 * to the game.  This is called in the robot process, so the normal
 * game UI is not encumbered by the search.
 */
 public void PrepareToMove(int playerIndex)
 {	InitBoardFromGame();
 }
 /**
  * breakpoint or otherwise override this method to intercept search events.
  * This is a low level way of getting control in the middle of a search for
  * debugging purposes.
  */
//public void Search_Break(String msg)
//{	super.Search_Break(msg);
//}


 public commonMove DoAlphaBetaFullMove()
    {
	 ChessMovespec move = null;

        try
        {

            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new ChessMovespec("Done", board.whoseTurn);
            }

            // it's important that the robot randomize the first few moves a little bit.
            int randomn = (RANDOMIZE && (board.captured[board.whoseTurn].height()==0)) 
            				? ((board.moveNumber <= 4) ? (20 - board.moveNumber) : 0)
            				: 0;
            int depth = MAX_DEPTH-(WEAKBOT?2:0);	// search depth
            double dif = 0.0;		// stop randomizing if the value drops this much
            // if the "dif" and "randomn" arguments to Find_Static_Best_Move
            // are both > 0, then alpha-beta will be disabled to avoid randomly
            // picking moves whose value is uncertain due to cutoffs.  This makes
            // the search MUCH slower so depth ought to be limited
            // if ((randomn>0)&&(dif>0.0)) { depth--; }
            // for games such as lyngk, where there are no "fools mate" type situations
            // the best solution is to use dif=0.0;  For games with fools mates,
            // set dif so the really bad choices will be avoided
            board.robotDepth = 0;
            Search_Driver search_state = Setup_For_Search(depth, 20.0);
            search_state.save_all_variations = SAVE_TREE;
            search_state.allow_killer = KILLER;
            search_state.verbose=verbose;			// debugging
            search_state.save_top_digest = true;	// always on as a background check
            search_state.save_digest=false;			// debugging only
            search_state.check_duplicate_digests = false; 	// debugging only

            if (move == null)
            {
                move = (ChessMovespec) search_state.Find_Static_Best_Move(randomn,dif);
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