package gipf;

import lib.ExtendedHashtable;
import lib.G;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;
/**
 * Gipf uses alpha-beta only
 * 
 * @author Ddyer
 *
 */

public class GipfPlay extends commonRobot<GipfBoard> implements Runnable, GipfConstants,
    RobotProtocol
{
    static final double VALUE_OF_WIN = 1000000.0;
    boolean SAVE_TREE = false;				// debug flag for the search driver
    boolean KILLER = false;
    /* strategies */
    final int RANDOM = 0;
    static final int WEAKBOT_DEPTH = 3;
    static final int DUMBOT_DEPTH = 4;
    static final int GOODBOT_DEPTH = 6;
    static final int BESTBOT_DEPTH = 7;
    int MAX_DEPTH = DUMBOT_DEPTH;
    boolean DUMBOT = false;
    int boardSearchLevel = 0;				// the current search depth

 
    /* constructor */
    public GipfPlay()
    {
    }


    public void Unmake_Move(commonMove m)
    {	Gipfmovespec mm = (Gipfmovespec)m;
        board.UnExecute(mm);
       	boardSearchLevel--;
   }

    public void Make_Move(commonMove m)
    {	Gipfmovespec mm = (Gipfmovespec)m;
        board.RobotExecute(mm);
        boardSearchLevel++;
    }


    public CommonMoveStack  List_Of_Legal_Moves()
    {
        return(board.GetListOfMoves());
    }

    double ScoreForPlayer(GipfBoard evboard,int player,boolean print)
    {	
     	boolean win = evboard.WinForPlayerNow(player);
    	if(win) { return(VALUE_OF_WIN+(1.0/(1+boardSearchLevel))); }
    	return(evboard.ScoreForPlayer(player,print,DUMBOT));

    }
    public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
        String evaluator, int strategy)
    {
        InitRobot(newParam, info, strategy);
        GameBoard = (GipfBoard) gboard;
        board = GameBoard.cloneBoard();

        switch(strategy)
        {
        case WEAKBOT_DEPTH:
        	MAX_DEPTH = WEAKBOT_DEPTH;
        	DUMBOT = true;
        	break;
        case DUMBOT_LEVEL:
        	MAX_DEPTH = DUMBOT_DEPTH;
        	DUMBOT = true;
        	break;
        case SMARTBOT_LEVEL:
        	MAX_DEPTH = GOODBOT_DEPTH;
        	DUMBOT = false;
        	break;
        case BESTBOT_LEVEL:
        	MAX_DEPTH = BESTBOT_DEPTH;
        	DUMBOT = false;
        	break;
		default:
			break;

        }
    }

    public void PrepareToMove(int playerIndex)
    {	board.copyFrom(GameBoard);
    }
    
   Gipfmovespec nextMove = null;
   
    public commonMove DoFullMove()
       {
   
    	Gipfmovespec move = nextMove;
    	nextMove = null;
    	if(move!=null) { return(move); }
    	
    	try
           {

               if (board.DoneState())
               { // avoid problems with gameover by just supplying a done
                   move = new Gipfmovespec("Done", board.whoseTurn);
               }

               // it's important that the robot randomize the first few moves a little bit.
               int randomn =  RANDOMIZE 
               				? ((board.moveNumber <= 6) ? (20 - board.moveNumber) : 0)
               				: 0;
               boardSearchLevel = 0;

               int depth = MAX_DEPTH;
               if(randomn>0) { depth=Math.min(depth,5); }
               Search_Driver search_state = Setup_For_Search(depth, false);
               search_state.save_all_variations = SAVE_TREE;
               search_state.allow_killer = KILLER;
               search_state.verbose=verbose;			// debugging
               search_state.save_digest=false;			// debugging only
               search_state.save_top_digest = false;		// doesn't quite work, but probably not important [ddyer 5/2011]
               search_state.check_duplicate_digests = false; 	// debugging only

               if (move == null)
               {	// 15 is keyed the the current evaluation function, designed to eliminate offboard moves
                   move = (Gipfmovespec) search_state.Find_Static_Best_Move(randomn,14);
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
               if(move.op==MOVE_SLIDE)
               {
            	   move.op = MOVE_SLIDEFROM;
            	   nextMove = move;
            	   return(new Gipfmovespec(move.player,MOVE_DROPB,move.from_col,move.from_row));
               }
               return (move);
           }

           continuous = false;
           // abnormal exit
           return (null);
       }

    public void StaticEval()
    {	
    	GipfBoard evboard = GameBoard.cloneBoard();
        double val0 = ScoreForPlayer(evboard,FIRST_PLAYER_INDEX,true);
        double val1 = ScoreForPlayer(evboard,SECOND_PLAYER_INDEX,true);
        System.out.println("Eval is "+ val0 +" "+val1+ " = " + (val0-val1));
    }

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

}
