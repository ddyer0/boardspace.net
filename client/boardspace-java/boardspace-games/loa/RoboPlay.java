/* copyright notice */package loa;
/**
 * loa uses alpha-beta only
 * 
 */
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

import java.util.*;

import lib.*;


public class RoboPlay extends commonRobot<Loa_Board> implements Runnable, Constants,UIC,
    RobotProtocol
{   
    int SEARCH_DEPTH = 7;
    boolean isLOAP = false;
    public RoboPlay()
    {
    }


    public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
        String evaluator, int strategy)
    {
        InitRobot(newParam, info, strategy);
        GameBoard = (Loa_Board) gboard;
        board = GameBoard.cloneBoard();

        //board.previous_player.so_weight=board.next_player.so_weight=2.0;
        search_depth = SEARCH_DEPTH;
        switch(strategy)
        {
        case WEAKBOT_LEVEL:	
        	search_depth = SEARCH_DEPTH - 3;
        	break;
        case DUMBOT_LEVEL:
        	search_depth = SEARCH_DEPTH - 2;
        	break;
        case SMARTBOT_LEVEL:
        	search_depth = SEARCH_DEPTH;
        	break;
        case BESTBOT_LEVEL:
        	search_depth = SEARCH_DEPTH+1;
        	break;
        default: G.Error("Not expecting strategy %s",strategy);
        }

        if(GameBoard.setup==Setup_Type.LOAP) 
        	{ search_depth++;
        	  isLOAP=true;
        	}
    }


    public double Static_Evaluate_Position(commonMove m)
    {	int playerIndex = m.player;
    	Player_Info pl = (playerIndex==0)
    						?board.black
    						:board.white;
    	double ev = pl.Evaluate_Position();
     	return(ev);
    }

    public void StaticEval()
    {
        if (!robotRunning)
        {
            board.copyFrom(GameBoard);

            double eval = board.next_player.Static_Eval_Current();
            System.out.println("Eval is " + eval);
        }
    }

    public CommonMoveStack  List_Of_Legal_Moves()
    {	CommonMoveStack result = new CommonMoveStack();
        List_Moves lm = board.next_player.List_Of_Legal_Moves();
        int cp = (board.next_player == board.white) ? 1 : 0;

        for (Enumeration<commonMove> em = lm.elements(); em.hasMoreElements();)
        {
            commonMove mm =  em.nextElement();
            mm.player = cp;
            result.addElement(mm);
        }

        return (result);
    }

    public void Make_Move(commonMove mp)
    {
        Move_Spec m = (Move_Spec) mp;
        Move_Reason reason = board.Test_Move(m);

        if (G.Assert(reason == Move_Reason.Ok, "Move must be legal"))
        {
            board.Make_Move(m);
        }
    }

    public void Unmake_Move(commonMove m)
    {
        board.Unmake_Move((Move_Spec) m);
    }
    public void PrepareToMove(int playerindex)
    {
        board.copyFrom(GameBoard);
  	
    }
    public commonMove DoFullMove()
    {
        Move_Spec move = null;

        try
        {
            int randomn = 
            	RANDOMIZE 
            	? ( isLOAP
            		? ((board.moveNumber()<=5) ? 5-board.moveNumber() : 0)
            		: ((board.moveNumber() <= 8) ? (10 - board.moveNumber()) : 0))
            	  : 0;
            Search_Driver search_state = Setup_For_Search(search_depth, false);
            move = (Move_Spec) search_state.Find_Static_Best_Move(randomn);
        }
        finally
        {
            setProgress(0.0);
            Accumulate_Search_Summary();
            Finish_Search_In_Progress();
        }

 
        return (move);
    }
}
