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
package yinsh.common;

import com.codename1.ui.geom.Rectangle;

import online.search.*;
import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
/**
 * Yinsh uses alpha-beta, Yinsh Blitz uses monte carlo
 * 
 * @author Ddyer
 *
 */
// TODO: make bot smarter about ring clusters in blitz mode

public class YinshPlay extends commonRobot<YinshBoard> implements Runnable, YinshConstants,
    RobotProtocol
{ /* control parameters */
    boolean SAVE_TREE = false;
    
    boolean EXP_MONTEBOT = false;
    int MONTE_TIME = 20;
    boolean KILLER = false;
    static int DEFAULT_MAX_DEPTH=3;
    static int DEFAULT_OPENING_DEPTH=3;
    int OPENING_DEPTH = DEFAULT_OPENING_DEPTH;
    int MAX_DEPTH = DEFAULT_MAX_DEPTH;

    /* evaluation parameters */
    static final double RING_SCORE = 100.0; // points for a captured ring
    static final double CHIP_SCORE = 0.1; // points for a clip currently our color (material)
    static final double MOVE_SCORE = 0.1; // points for a move (mobility)
    static final double FLIP_SCORE = 0.1; // points for a flip (power)


    int boardSearchLevel = 0;

    /* constructor */
    public YinshPlay()
    {
    }

    public boolean Depth_Limit(int current, int max)
    {
        return (boardSearchLevel >= max);
    }

    public void Unmake_Move(commonMove m)
    {
        Yinshmovespec ym = (Yinshmovespec) m;
        int pl = board.whoseTurn;
        board.UnExecute(ym);

        if (pl != board.whoseTurn)
        {
            boardSearchLevel--;
        }
    }

    public void Make_Move(commonMove m)
    {
        Yinshmovespec ym = (Yinshmovespec) m;
        int pl = board.whoseTurn;
        board.RobotExecute(ym);

        if (pl != board.whoseTurn)
        {
            boardSearchLevel++;
        }
    }
    public commonMove Get_Random_Move(Random r)
    {
    	commonMove m = board.GetRandomMove(r);
    	if(m!=null) { return m; }
    	return super.Get_Random_Move(r); 
    }
    public CommonMoveStack  List_Of_Legal_Moves()
    {
        return(board.List_Of_Legal_Moves(null));
   }

    private double ScoreForPlayer(YinshBoard b, int player)
    {
        double ringscore = RING_SCORE * b.captured_rings[player]; // 100 points for each ring
        double movescore = MOVE_SCORE * b.moves[player];
        double flipscore = FLIP_SCORE * b.flips[player];
        double chipscore = CHIP_SCORE * b.bchips[player];

        return (ringscore + movescore + flipscore + chipscore);
    }
    public double Static_Evaluate_Position(commonMove m)
    {	int playerindex = m.player;
    	return(EvaluatePosition(playerindex,false));
    }
    public double EvaluatePosition(int forplayer,boolean print)
    {
        int otherplayer = nextPlayer[forplayer];
        board.CountPosition();

        return (ScoreForPlayer(board, forplayer) -  ScoreForPlayer(board, otherplayer));
    }



    /** initialize the robot, but don't run yet */
    public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gb,
        String evaluator, int strategy)
    {	
        InitRobot(newParam, info, strategy);
        GameBoard = (YinshBoard) gb;
        board = GameBoard.cloneBoard();
        boolean blitz = board.blitz;
        switch(strategy)
        {
        default: throw G.Error("Not expecting strategy %s",strategy);
        case WEAKBOT_LEVEL: 
        	OPENING_DEPTH = DEFAULT_OPENING_DEPTH-1;
            MAX_DEPTH = DEFAULT_MAX_DEPTH-1;
        	WEAKBOT = true;
        	MONTEBOT = DEPLOY_MONTEBOT;
        	break;
        case DUMBOT_LEVEL:
        	MAX_DEPTH = DEFAULT_MAX_DEPTH;
        	OPENING_DEPTH = DEFAULT_OPENING_DEPTH;
        	MONTE_TIME = 10;
        	break;
        case SMARTBOT_LEVEL: 
        	MAX_DEPTH = DEFAULT_MAX_DEPTH+1;
        	OPENING_DEPTH = DEFAULT_OPENING_DEPTH+1;
        	MONTE_TIME = 15;
        	break;
        case BESTBOT_LEVEL: 
        	MAX_DEPTH = DEFAULT_MAX_DEPTH+1;
        	OPENING_DEPTH = DEFAULT_OPENING_DEPTH+1;
        	MONTE_TIME = 15;
        	MONTEBOT = DEPLOY_MONTEBOT;
        	break;
        case MONTEBOT_LEVEL:
        	MAX_DEPTH = DEFAULT_MAX_DEPTH+1;
        	OPENING_DEPTH = DEFAULT_OPENING_DEPTH+1;
        	MONTE_TIME = 15;
        	MONTEBOT = DEPLOY_MONTEBOT;
        	break;
       	
        }
        MONTEBOT |= (blitz && DEPLOY_MONTEBOT);
    }

    public void PrepareToMove(int playerindex)
    {  
    	board.copyFrom(GameBoard);
    }
    public commonMove DoAlphaBetaFullMove()
    {
        Yinshmovespec move = null;
        try
        {

            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new Yinshmovespec("Done", board.whoseTurn);
            }

            int randomn = RANDOMIZE 
              ? ((board.moveNumber <= 12) ? (33 - board.moveNumber) : 0)
              : 0;
            boardSearchLevel = 0;

            int depth = (board.getState() == YinshState.PLACE_RING_STATE)
                ? OPENING_DEPTH : MAX_DEPTH;
            Search_Driver search_state = Setup_For_Search(depth, false);
            search_state.save_all_variations = SAVE_TREE;
            search_state.allow_killer = KILLER;
            search_state.verbose=verbose;			// debugging
            search_state.save_digest=false;	// debugging only

            if (move == null)
            {
                move = (Yinshmovespec) search_state.Find_Static_Best_Move(randomn);
            }
        }
        finally
        {
            Accumulate_Search_Summary();
            Finish_Search_In_Progress();
        }

        if (move != null)
        {
            if(G.debug()) { move.showPV("exp final pv: ");}

            return (move);
        }

        continuous = false;

        return (null);
    }

    public boolean autoPlayerEvent(int eventX, int eventY, Rectangle rect)
    {
        return (false);
    }

    public double EvaluatePosition(commonPlayer p, boolean print)
    {
        double val = 0;
        //int playerindex=p.index;
        //int otherplayerindex = nextPlayerIndex[playerindex];
        {

          val = EvaluatePosition(0,false);
        }

        return (val);
    }

    // should be the same as Static_Evaluate_Move but with
    // some details printed.
    public void StaticEval()
    {
        if (!robotRunning)
        {
            board.copyFrom(GameBoard);
            EvaluatePosition(0,true);
        }
    }

    /** get the move list from the private board */
    public void getBoardMoveList(int lvl, int forplayer, CommonMoveStack  result)
    {
        board.List_Of_Legal_Moves(result);
    }
    
    // this is the monte carlo robot, which for some games is much better then the alpha-beta robot
    // for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
    // evaluator other than winning a game.
    public commonMove DoMonteCarloFullMove()
    {	commonMove move = null;
    	try {
          if (board.DoneState())
           { // avoid problems with gameover by just supplying a done
               move = new Yinshmovespec("Done", board.whoseTurn);
           }
           else 
           {
           // it's important that the robot randomize the first few moves a little bit.
           double randomn = (RANDOMIZE && (board.moveNumber <= 6)) ? 0.1/board.moveNumber : 0.0;
           UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this);
           monte_search_state.save_top_digest = false;	// always on as a background check
           monte_search_state.save_digest=false;	// debugging only
           monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
           monte_search_state.timePerMove = MONTE_TIME;		//  seconds per move
           monte_search_state.verbose = verbose;
           monte_search_state.alpha = 0.5;
           monte_search_state.dead_child_optimization=true;
           monte_search_state.random_moves_per_second = WEAKBOT ? 1000 : 500000;
           monte_search_state.simulationsPerNode = EXP_MONTEBOT ? 10 : 1;
           monte_search_state.maxThreads = DEPLOY_THREADS;
           move = monte_search_state.getBestMonteMove();
           }
    		}
         finally { ; }
         if(move==null) { continuous = false; }
        return(move);
    }
    /**
     * for UCT search, return the normalized value of the game, with a penalty
     * for longer games so we try to win in as few moves as possible.  Values
     * must be normalized to -1.0 to 1.0
     */
    public double NormalizedScore(commonMove lastMove)
    {	int player = lastMove.player;
    	boolean win = board.WinForPlayerNow(player);
    	if(win) { return(0.8+0.2/(boardSearchLevel+1)); }
    	boolean win2 = board.WinForPlayerNow(nextPlayer[player]);
    	if(win2) { return(- (0.8+0.2/(boardSearchLevel+1))); }
    	return(0);
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

}
