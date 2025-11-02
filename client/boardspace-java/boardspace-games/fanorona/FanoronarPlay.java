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
package fanorona;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

/** 
 * Fanorona uses alpha-beta only
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * @author ddyer
 * 
 *
 */
public class FanoronarPlay extends commonRobot<FanoronaBoard> implements Runnable, 
    RobotProtocol
{	
	static final double VALUE_OF_WIN = 10000.0;

	private boolean SAVE_TREE = false;				// debug flag for the search driver
		
	private boolean KILLER = false;					// probably ok for all games with a 1-part move
	private static final int WEAKBOT_DEPTH = 4;
	private static final int DUMBOT_DEPTH = 5;
	private static final int SMARTBOT_DEPTH = 6;
	private static final int BESTBOT_DEPTH = 8;		// depth 8 is a little slow.  7 is ok.  8 is still weaker than montebot
	private int MAX_DEPTH = BESTBOT_DEPTH;
	private boolean DUMBOT = false;

    /* constructor */
    public FanoronarPlay()
    {
    }


/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	
    	board.UnExecute(m);
     }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   FanoronaMovespec mm = (FanoronaMovespec)m;
        board.RobotExecute(mm);
    }

/** return an enumeration of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {
        return(board.GetListOfMoves(board.robotDepth==0));
    }
    
    

    
    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    private double ScoreForPlayer(FanoronaBoard evboard,int player,boolean print)
    {	
   	return(evboard.ScoreForPlayer(player,print,DUMBOT));

    }
    
    /**
     * this is it! just tell me that the position is worth.  
     */
    public double Static_Evaluate_Position(commonMove m)
    {	int playerindex = m.player;
    		if(board.GameOver())
    		{
        	if(board.win[playerindex]) { return(VALUE_OF_WIN+(1.0/(1+board.robotDepth))); }
        	if(board.win[playerindex^1]) { return -(VALUE_OF_WIN+1-(1.0/(1+board.robotDepth))); }
        	return 0;
    		}
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,playerindex^1,false);
        return(val0-val1);
    }
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {
    	FanoronaBoard evboard = GameBoard.cloneBoard();
    	double val0 = ScoreForPlayer(evboard,FIRST_PLAYER_INDEX,true);
    	double val1 = ScoreForPlayer(evboard,SECOND_PLAYER_INDEX,true);
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
        GameBoard = (FanoronaBoard) gboard;
        board = GameBoard.cloneBoard();
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
        case SMARTBOT_LEVEL:
        	MAX_DEPTH = SMARTBOT_DEPTH;
        	DUMBOT=false;
        	break;
        case BESTBOT_LEVEL:
         	MAX_DEPTH = BESTBOT_DEPTH;
        	DUMBOT=false;
         	break;
 
        default: throw G.Error("Not expecting strategy %s",strategy);
        }
    }
    /** copy the game board, in preparation for a search */
    public void InitBoardFromGame()
    {
        board.copyFrom(GameBoard);
        board.undoStack.clear();
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
	 commonMove move = null;

        try
        {
            // it's important that the robot randomize the first few moves a little bit.
            int randomn = (RANDOMIZE &&  (board.moveNumber <= 4))
            				? (20 - board.moveNumber)
            				: 0;
            board.robotDepth = 0;

            int depth = MAX_DEPTH;
            Search_Driver search_state = Setup_For_Search(depth, false);
            search_state.save_all_variations = SAVE_TREE;
            search_state.allow_killer = KILLER;
            search_state.verbose=verbose;			// debugging
            search_state.save_top_digest = true;	// always
            search_state.save_digest=false;			// debugging only
            search_state.check_duplicate_digests = false; 	// debugging only
            search_state.good_enough_to_quit = VALUE_OF_WIN;
            search_state.allow_good_enough = true;


            if (move == null)
            {
                move = search_state.Find_Static_Best_Move(randomn);
                search_state.showResult(move,false);
            }
        }
        finally
        {
            Accumulate_Search_Summary();
            Finish_Search_In_Progress();
        }

        continuous &= move!=null;
        return (move);
    }
 


/* reference game 2/7/2008 level 7 random false

 (;
 GM[Fanorona] VV[1]
 DT[]
 GN[null-null]
 SU[fanorona]
 P0[id "null"]
 P1[id "null"]
 ; P0[0 Start P0]
 ; P0[1 Capture A F 2 E 3]
 ; P0[2 done]
 ; P1[3 Capture A D 5 D 4]
 ; P1[4 done]
 ; P0[5 Capture W G 3 F 2]
 ; P0[6 done]
 ; P1[7 Capture A C 3 D 2]
 ; P1[8 done]
 ; P0[9 Capture W E 3 D 3]
 ; P0[10 done]
 ; P1[11 Capture W D 2 D 1]
 ; P1[12 Capture A D 1 E 1]
 ; P1[13 done]
 ; P0[14 Capture A F 2 F 3]
 ; P0[15 Capture A F 3 G 3]
 ; P0[16 Capture A G 3 F 4]
 ; P0[17 Capture W F 4 E 3]
 ; P0[18 done]
 ; P1[19 Capture A E 1 D 1]
 ; P1[20 done]
 ; P0[21 Capture A C 2 C 3]
 ; P0[22 Capture W C 3 D 2]
 ; P0[23 Capture W D 2 D 3]
 ; P0[24 done]
 ; P1[25 Capture A B 5 B 4]
 ; P1[26 done]
 ; P0[27 Capture W D 3 D 2]
 ; P0[28 Capture A D 2 C 3]
 ; P0[29 Capture A C 3 B 3]
 ; P0[30 done]
 ; P1[31 Capture A A 4 A 3]
 ; P1[32 done]
 ; P0[33 Capture A G 2 G 3]
 ; P0[34 done]
 ; P1[35 Capture W E 4 E 5]
 ; P1[36 Capture A E 5 F 4]
 ; P1[37 done]
 ; P0[38 Capture W B 3 C 3]
 ; P0[39 done]
 ; P1[40 Capture W I 4 I 5]
 ; P1[41 done]
 ; P0[42 Move C 3 B 4]
 ; P0[43 done]
 ; P1[44 Move I 5 H 4]
 ; P1[45 done]
 ; P0[46 Move B 4 C 5]
 ; P0[47 done]
 ; P1[48 Move H 5 G 5]
 ; P1[49 done]
 ; P0[50 Move C 5 B 4]
 ; P0[51 done]
 ; P1[52 Move G 5 F 5]
 ; P1[53 done]
 ; P0[54 Move B 4 A 3]
 ; P0[55 done]
 ; P1[56 Move H 4 G 3]
 ; P1[57 done]
 ; P0[58 Move A 3 A 2]
 ; P0[59 done]
 ; P1[60 Move F 5 E 5]
 ; P1[61 done]
 ; P0[62 Move A 2 B 2]
 ; P0[63 done]
 ; P1[64 Move F 4 E 3]
 ; P1[65 done]
 ; P0[66 Move B 2 A 3]
 ; P0[67 done]
 ; P1[68 Move E 5 D 4]
 ; P1[69 done]
 ; P0[70 Move A 3 B 3]
 ; P0[71 done]
 ; P1[72 Move E 3 D 2]
 ; P1[73 done]
 ; P0[74 Move B 3 A 3]
 ; P0[75 done]
 ; P1[76 Move G 3 F 2]
 ; P1[77 done]
 ; P0[78 Move A 3 A 2]
 ; P0[79 done]
 ; P1[80 Move D 4 C 3]
 ; P1[81 done]
 ; P0[82 Move A 2 A 1]
 ; P0[83 done]
 ; P1[84 Capture A C 3 B 2]
 ; P1[85 done]
 ;
 P0[time 0:00:47 ]
 P1[time 0:01:14 ]
 )
 */

 }