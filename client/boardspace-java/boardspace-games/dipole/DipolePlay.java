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
package dipole;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

/** 
 * Dipole uses alpha-beta only
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * TODO: tweak the robot to resign when too far behind.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * 
 * UCT robot notes; plays reasonably with 60 seconds per move, not competitive with standard robot.
 * 
 * @author ddyer
 *
 */
public class DipolePlay extends commonRobot<DipoleBoard> implements Runnable, DipoleConstants,
    RobotProtocol
{	
    private boolean SAVE_TREE = false;				// debug flag for the search driver
    private boolean KILLER = false;
    private static final int WEAKBOT_DEPTH = 3;
    private static final int DUMBOT_DEPTH = 4;
    private static final int GOODBOT_DEPTH = 7;
    private static final int BESTBOT_DEPTH = 9;
    private int MAX_DEPTH = BESTBOT_DEPTH;
	static final double VALUE_OF_WIN = 1000.0;

    private int boardSearchLevel = 0;				// the current search depth

    /* constructor */
    public DipolePlay()
    {
    }


/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	DipoleMovespec mm = (DipoleMovespec)m;
         board.UnExecute(mm);
       	boardSearchLevel--;
   }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   DipoleMovespec mm = (DipoleMovespec)m;
    	boardSearchLevel++;
        board.RobotExecute(mm);
    
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
    
    

    
    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
    double ScoreForPlayer(DipoleBoard evboard,int player,boolean print)
    {	
     	boolean win = evboard.WinForPlayerNow(player);
    	if(win) { return(VALUE_OF_WIN+(1.0/(1+boardSearchLevel))); }
    	return(evboard.ScoreForPlayer(player,print));

    }
    
    /**
     * this is it! just tell me that the position is worth.  
     */
    // TODO: refactor static eval so GameOver is checked first
    public double Static_Evaluate_Position(commonMove m)
    {	int playerindex = m.player;
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,nextPlayer[playerindex],false);
        // don't dilute the value of wins with the opponent's positional score.
        // this avoids the various problems such as the robot committing suicide
        // because it's going to lose anyway, and the position looks better than
        // if the opponent makes the last move.  Technically, this isn't needed
        // if there is no such thing as a suicide move, but the logic
        // is included here because this is supposed to be an example.
        if(val0>=VALUE_OF_WIN) 
        	{ return(val0); 
        	}
        if(val1>=VALUE_OF_WIN) 
        	{ return(-val1); 
        	}
       return(val0-val1);
    }
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {	
    	DipoleBoard evboard = GameBoard.cloneBoard();
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
        GameBoard = (DipoleBoard) gboard;
        board = GameBoard.cloneBoard();
        switch(strategy)
        {
        case WEAKBOT_LEVEL:
        	MAX_DEPTH = WEAKBOT_DEPTH;
         	break;
        case DUMBOT_LEVEL:
        	MAX_DEPTH = DUMBOT_DEPTH;
        	break;
        case SMARTBOT_LEVEL:
        	MAX_DEPTH = GOODBOT_DEPTH;
        	break;
        case BESTBOT_LEVEL:
        	MAX_DEPTH = BESTBOT_DEPTH;
        	break;
       default: throw G.Error("Not expecting strategy %s",strategy);
        }
     }
    /** copy the game board, in preparation for a search */
    public void InitBoardFromGame()
    {
        board.copyFrom(GameBoard);
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
	 DipoleMovespec move = null;

        try
        {

            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new DipoleMovespec("Done", board.whoseTurn);
            }

            // it's important that the robot randomize the first few moves a little bit.
            int randomn =  RANDOMIZE 
            				? ((board.moveNumber <= 4) ? (5 - board.moveNumber) : 0)
            				: 0;
            boardSearchLevel = 0;

            int depth = MAX_DEPTH;
            if(randomn>0) { depth=Math.min(depth,6); }
            Search_Driver search_state = Setup_For_Search(depth, false);
            search_state.save_all_variations = SAVE_TREE;
            search_state.allow_killer = KILLER;
            search_state.verbose=verbose;			// debugging
            search_state.save_digest=false;			// debugging only
            search_state.check_duplicate_digests = false; 	// debugging only

            if (move == null)
            {	// 15 is keyed the the current evaluation function, designed to eliminate offboard moves
                move = (DipoleMovespec) search_state.Find_Static_Best_Move(randomn,14);
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
 



/*  reference game, level 9 no random 2/7/2008
 * (;
GM[Dipole] VV[1]
DT[]
GN[null-null]
SU[dipole-s]
P0[id "null"]
P1[id "null"]
; P0[0 Start P0]
; P0[1 move E 1 F 2]
; P0[2 done]
; P1[3 move D 8 C 7]
; P1[4 done]
; P0[5 move E 1 D 2]
; P0[6 done]
; P1[7 move D 8 C 7]
; P1[8 done]
; P0[9 move E 1 F 2]
; P0[10 done]
; P1[11 move C 7 D 6]
; P1[12 done]
; P0[13 move E 1 F 2]
; P0[14 done]
; P1[15 move D 8 C 7]
; P1[16 done]
; P0[17 move E 1 D 2]
; P0[18 done]
; P1[19 move C 7 D 6]
; P1[20 done]
; P0[21 move E 1 D 2]
; P0[22 done]
; P1[23 move D 8 C 7]
; P1[24 done]
; P0[25 move E 1 D 2]
; P0[26 done]
; P1[27 move D 8 C 7]
; P1[28 done]
; P0[29 move F 2 G 3]
; P0[30 done]
; P1[31 move D 8 C 7]
; P1[32 done]
; P0[33 move D 2 C 3]
; P0[34 done]
; P1[35 move C 7 B 6]
; P1[36 done]
; P0[37 move E 1 F 2]
; P0[38 done]
; P1[39 move C 7 D 6]
; P1[40 done]
; P0[41 move D 2 C 3]
; P0[42 done]
; P1[43 move C 7 D 6]
; P1[44 done]
; P0[45 move D 2 C 3]
; P0[46 done]
; P1[47 move D 8 C 7]
; P1[48 done]
; P0[49 move E 1 F 2]
; P0[50 done]
; P1[51 move B 6 C 5]
; P1[52 done]
; P0[53 move D 2 C 3]
; P0[54 done]
; P1[55 move C 5 B 4]
; P1[56 done]
; P0[57 move G 3 H 4]
; P0[58 done]
; P1[59 move C 7 D 6]
; P1[60 done]
; P0[61 move C 3 B 4]
; P0[62 done]
; P1[63 move D 6 B 4]
; P1[64 done]
; P0[65 move E 1 B 4]
; P0[66 done]
; P1[67 move D 8 C 7]
; P1[68 done]
; P0[69 move C 3 B 4]
; P0[70 done]
; P1[71 move D 8 C 7]
; P1[72 done]
; P0[73 move H 4 G 5]
; P0[74 done]
; P1[75 move D 8 C 7]
; P1[76 done]
; P0[77 move F 2 G 3]
; P0[78 done]
; P1[79 move D 8 C 7]
; P1[80 done]
; P0[81 move G 3 F 4]
; P0[82 done]
; P1[83 move C 7 D 6]
; P1[84 done]
; P0[85 move F 4 G 5]
; P0[86 done]
; P1[87 move D 8 C 7]
; P1[88 done]
; P0[89 move F 2 E 3]
; P0[90 done]
; P1[91 move C 7 D 6]
; P1[92 done]
; P0[93 move E 3 F 4]
; P0[94 done]
; P1[95 move D 6 E 5]
; P1[96 done]
; P0[97 move F 2 F 4]
; P0[98 done]
; P1[99 move C 7 E 5]
; P1[100 done]
; P0[101 move F 4 C 7]
; P0[102 done]
; P1[103 move D 6 E 5]
; P1[104 done]
; P0[105 move G 5 E 7]
; P0[106 done]
; P1[107 move D 6 G 3]
; P1[108 done]
; P0[109 move C 3 C 5]
; P0[110 done]
; P1[111 move G 3 F 2]
; P1[112 done]
; P0[113 move C 5 C 7]
; P0[114 done]
; P1[115 move G 3 G 1]
; P1[116 done]
; P0[117 move C 7 D 8]
; P0[118 done]
; P1[119 remove G 1 1]
; P1[120 done]
; P0[121 move C 7 D 8]
; P0[122 done]
; P1[123 move E 5 E 7]
; P1[124 done]
; P0[125 move C 7 E 7]
; P0[126 done]
; P1[127 move E 5 G 3]
; P1[128 done]
; P0[129 move C 7 D 8]
; P0[130 done]
; P1[131 remove G 1 1]
; P1[132 done]
; P0[133 remove D 8 1]
; P0[134 done]
; P1[135 move G 3 F 2]
; P1[136 done]
; P0[137 remove D 8 1]
; P0[138 done]
; P1[139 move F 2 G 1]
; P1[140 done]
; P0[141 remove D 8 1]
; P0[142 done]
; P1[143 remove G 1 1]
; P1[144 done]
; P0[145 move B 4 A 5]
; P0[146 done]
; P1[147 move F 2 G 1]
; P1[148 done]
; P0[149 move A 5 B 6]
; P0[150 done]
; P1[151 move G 3 F 2]
; P1[152 done]
; P0[153 move B 6 A 7]
; P0[154 done]
; P1[155 move F 2 G 1]
; P1[156 done]
; P0[157 move A 7 B 8]
; P0[158 done]
; P1[159 remove G 1 1]
; P1[160 done]
; P0[161 move B 4 C 5]
; P0[162 done]
; P1[163 remove G 1 1]
; P1[164 done]
;
P0[time 0:19:03 ]
P1[time 0:14:26 ]
)
 */


 }