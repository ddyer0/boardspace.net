/* copyright notice */package zertz.common;
/**
 * zertz uses alpha-beta
 * 
 */
import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;

public class autoPlay2 extends commonRobot<GameBoard> implements Runnable, GameConstants,
    RobotProtocol
{	
	private boolean SAVE_TREES = false;
	private boolean KILLER = false;
	private GameBoard sequence_board = null;
    private int searchForPlayer = -1;
    private int boardSearchLevel = 0;		// boardsearchlevel reflects the number of free moves made
    private Evaluator evaluator = new StandardEvaluator();
    public Evaluator getEvaluator() { return(evaluator); }
    // if true, reconsider sequences after making the first free move.
    // this has the effect that the bot can "discover" an even better move
    // after committing itself to the best move found at the old search depth.
    // it also causes each sequence to be recomputed, which halves the speed.
    public boolean RECONSIDER = true;
    private int WeakBotDepth = 1;
    private int StandardDepth = 2;
    private int MaxDepth = StandardDepth; // number of free moves to consider
    private movespec planned_sequence = null;
    private movespec start_of_sequence = null;
    
    private boolean ALLOW_RESTRICTED_SACRIFICE = false;
    private double timeLimit = 20;	// 20 seconds
    public autoPlay2() // constructor
    {
    }
    public commonMove getCurrentVariation()
    {
    	movespec m = (movespec)super.getCurrentVariation();
    	movespec v = m;
     	while (v!=null)
    	{	movespec next = (movespec)v.best_move();
    		if((next!=null) && (next.player!=v.player) && (v.op!=MOVE_DONE))
    		{
    			movespec mid = new movespec(v.player,MOVE_DONE);
    			v.set_best_move(mid);
    			mid.set_best_move(next);
    		}
    		v = next;
    	}
    	return(m);    }

    // dynamic depth limit is important but very delicate.
    public boolean Depth_Limit(int current, int max)
    {	
        boolean done = (board.getState() == ZertzState.MOVE_STATE) &&
            (boardSearchLevel >= max);

        return (done);
    }

    /** initialize the robot, but don't run yet */
    public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
        String eva, int strategy)
    {
        InitRobot(newParam, info, strategy);
        switch(strategy)
        {
        case WEAKBOT_LEVEL: 
        	MaxDepth = WeakBotDepth;
        	WEAKBOT = true;
        	break;
        case TESTBOT_LEVEL_1:
        case TESTBOT_LEVEL_2:
        	MaxDepth = 2;
        	break;
        case SMARTBOT_LEVEL:
        	ALLOW_RESTRICTED_SACRIFICE = true;
        	evaluator.setWeights(
        			//"-4.643392592962127E-4 -0.05809297242159539 -0.9314725850461272 -0.38357945681632943 -0.2348224942862074 0.13324772755870304 0.006323938415222591 0.06184940437470446 0.044775691503741236 -1.9596622511864508 -0.925303571641741 -0.4797374367386282 -0.46035481842800785 -0.3059037393330213 -0.04216218349852471 0.04092306522616482"
        			//"-4.643392592962127E-4 -0.05096696268405527 -0.9464981873743994 -0.39200310123061943 -0.2348224942862074 0.16739788676597944 -0.017359791507502633 0.04078411478118783 0.06562562000307579 -1.943486987734204 -0.9472554932412751 -0.4797374367386282 -0.418174536711872 -0.3059037393330213 -0.04216218349852471 0.04971608048950987"
        			//"7.0E-4 -0.048126605478300846 -1.0 -0.49182134283163115 -0.1586914477736229 -0.08532858596948642 -0.014554380302798417 -0.06724392020627325 -0.031154863374711947 -1.9863870345002361 -1.0286723961058848 -0.45938880446440955 -0.41674561249950354 -0.34043774286452344 -0.21594002733728002 -0.03133256514261341"
        			" 7.0E-4 -0.1495280773603488 -1.009771345466307 -0.47653059298446837 -0.1584874230162723 -0.009522550000981891 0.014602388462273064 -0.06724392020627325 -0.028336058555932696 -2.0101426901962354 -1.0103105162295332 -0.45938880446440955 -0.3360437348314521 -0.34043774286452344 -0.19693850286095096 0.047062948116738015"
        			);
        	MaxDepth = StandardDepth+1;
        	break;
        case BESTBOT_LEVEL:
        	timeLimit = 30;	// 30 seconds
        	ALLOW_RESTRICTED_SACRIFICE = true;
        	MaxDepth = StandardDepth+1;
        	break;
		case DUMBOT_LEVEL:
        default:
        	MaxDepth = StandardDepth;
        	break;
         }
        GameBoard = (GameBoard) gboard;
        board = GameBoard.cloneBoard();
        sequence_board = GameBoard.cloneBoard();
    }

    public double Static_Evaluate_Position(commonMove m)
    {	return(evaluator.evaluate(board, m.player, false));
    }


    // should be the same as Static_Evaluate_Move but with
    // some details printed.
    public void StaticEval()
    {
        if (!robotRunning)
        {   sequence_board.copyFrom(GameBoard);
            evaluator.evaluate(sequence_board,0,true);
        }
    }

    public void Unmake_Move(commonMove mm)
    {
        movespec m = (movespec) mm;

        //System.out.println("Un "+m);
        ZertzState ostate = board.getState();
 
        if ((ostate == ZertzState.MOVE_STATE)||(ostate==ZertzState.SETRING_STATE))
        {
            boardSearchLevel--;
        }

        board.ExpressUnExecute(m);
    }

    public void Make_Move(commonMove mm)
    {
        movespec m = (movespec) mm;

        //System.out.println("Ex "+ search_state.search_clock+" "+m);
        board.ExpressExecute(m);

        ZertzState newstate = board.getState();

        if ((newstate == ZertzState.DONE_STATE) || (newstate == ZertzState.DONE_CAPTURE_STATE))
        {
            board.SetNextPlayer(replayMode.Replay);
            ZertzState nextstate = board.getState();
            if ((nextstate == ZertzState.MOVE_STATE)||(nextstate==ZertzState.SETRING_STATE))
            {
                boardSearchLevel++;
            }
        }
    }

    public CommonMoveStack  List_Of_Legal_Moves()
    {	
    	CommonMoveStack  moves = new CommonMoveStack();
    	boolean forme = (board.whoseTurn==searchForPlayer);
    	boolean restricted = (boardSearchLevel > 0) && forme;
        getBoardMoveList(restricted, moves);

        return (moves);
    }



    public movespec ExtendSequence(movespec m)
    {
        movespec pm = m;

        while (pm.best_move() != null)
        {
            movespec bm = (movespec) pm.best_move();

            if (bm.player != pm.player)
            {
                movespec splice = new movespec(pm.player, MOVE_DONE);
                splice.set_best_move(pm.best_move());
                pm.set_best_move(splice);
                pm = splice;
            }

            pm = (movespec) pm.best_move();
        }

        return (m);
    }

    public movespec FinishMove(int p, movespec m)
    {
        boolean val = false;
        planned_sequence = m;

        if (RECONSIDER && (m.op == MOVE_BtoB))
        { // if the first move in our planned sequence is a capture,
          // then don't bother with the rest of the sequence.  The 
          // free moves after the initial capture won't be fully baked anyway.

            if ((game.extraactions) && (m.best_move() != null))
            {
                if(verbose>0 && G.debug()) { m.showPV("Flushing rest of sequence led by a capture"); }
            }

            m.set_best_move(null);
        }

        do
        {
            if (planned_sequence.player != sequence_board.whoseTurn)
            {
            	sequence_board.SetNextPlayer(replayMode.Replay);
            }

            val = sequence_board.Execute(planned_sequence,replayMode.Replay); //make the local board execute the move we plan

            if (planned_sequence.op == MOVE_DONE)
            { //note, this is to avoid blindly following sequences which started
              //with a forced move, followed by a free move, but no followup.
                planned_sequence = null;
                start_of_sequence = null;
            }
            else
            {
                planned_sequence = (movespec) planned_sequence.best_move(); //remember the rest of the plan
                start_of_sequence = sequence_board.lastMove; //remember the result, as cleaned up
            }
        }
        while (val && (planned_sequence != null) &&
                (planned_sequence.player != p) &&
                (sequence_board.getState() != ZertzState.GAMEOVER_STATE));


        return (m);
    }

    public void PrepareToMove(int playerindex)
    {
        board.copyFrom(GameBoard); 
        sequence_board.copyFrom(GameBoard);
        searchForPlayer = playerindex;
        timeLimit = adjustTime(timeLimit,20-board.moveNumber());
    }
    public commonMove DoAlphaBetaFullMove()
    {
        movespec move = null;

        try
        {
            if (verbose > 0)
            {
                System.out.println("begin robot");
            }

            if (planned_sequence != null)
            {
                if ((start_of_sequence != null) && (planned_sequence != null) &&
                        (start_of_sequence.Same_Move_P(GameBoard.lastMove)))
                {
                    if ((game != null) && game.extraactions)
                    {
                       // System.out.println("Continue with " + planned_sequence);
                    }

                    return (FinishMove(searchForPlayer, planned_sequence));
                }

                planned_sequence = start_of_sequence = null;
            }

            int randomn = RANDOMIZE 
            				? ((board.moveNumber <= 2) ? (33 - board.moveNumber) : 0)
            				: 0;
            boardSearchLevel = 0;
            Search_Driver search_state = Setup_For_Search(MaxDepth, timeLimit/60,StandardDepth);
            search_state.verbose = verbose;
            search_state.save_all_variations = SAVE_TREES;
            search_state.allow_killer = KILLER;
            search_state.save_top_digest = true;
            search_state.save_digest = false;	// debugging only
            
            
            move = (movespec) search_state.Find_Static_Best_Move(randomn);
        }
        finally
        {
            Accumulate_Search_Summary();
            Finish_Search_In_Progress();
        }

        if (move != null)
        {
            if(verbose>0 && G.debug()) { move.showPV("exp final pv: "); }
            FinishMove(searchForPlayer, ExtendSequence(move));

            return (move);
        }

        continuous = false;

        return (null);
    }

    /** get the move list from the private board */
    public void getBoardMoveList(boolean restricted, CommonMoveStack  result)
    {	int emptyCount = 0;
         int p = board.whoseTurn;
        ZertzState state = board.getState();

        switch (state)
        {
        case SWAP_CONFIRM_STATE:
        case DRAW_STATE:
        case DONE_STATE:
        case DONE_CAPTURE_STATE:
            result.addElement(new movespec(p, MOVE_DONE));

            break;
            
        case MOVE_OR_SWAP_STATE:
        // trying to swap confuseds the evaluator for some undetermined reason. 
        // -- search never completes
        // result.addElement(new movespec(p,MOVE_SWAP)); 

        case BALL_STATE:
        case MOVE_STATE:
            //place a ball in each possible position
            {
                int rack_index = RESERVE_INDEX;
                int[] ball = board.balls[RESERVE_INDEX];
                int tot = 0;
                boolean useUndecided = false; //(board.whoseTurn==forplayer);
                for (int j = 0; j < ball.length; j++)
                { //note, include negative counts on deployed undecided balls
                    tot += ball[j];
                }

                if (tot == 0)
                {
                    rack_index = p;
                    ball = board.balls[rack_index];
                }

                for(zCell c = board.allCells; c!=null; c=c.next)
                {
                 boolean isEmpty = (c.contents == Empty);
                 if(isEmpty) { emptyCount++; }
                 if (isEmpty 
                     && (!((board.moveNumber == 1) && board.isEdgeCell(c))
                     && (!restricted 
                    		|| (board.Capure_Is_Possible_From(c)
                    		|| (ALLOW_RESTRICTED_SACRIFICE && board.Capure_Is_Possible_To(c))))))
                        {
                            if (!useUndecided)
                            {
                                for (int color = 0; color < NCOLORS; color++)
                                {
                                    if (ball[color] > 0)
                                    {
                                        result.addElement(new movespec(p,
                                                MOVE_RtoB, rack_index, color,
                                                c.col, c.row));
                                    }
                                }
                            }
                            else
                            { //this is part of an optimization to play balls
                              //of ambigous color, and let them flow through

                                movespec ms = new movespec(p, MOVE_RtoB,
                                        rack_index, zChip.UNDECIDED_INDEX, c.col,c.row);
                                ms.restricted = restricted;
                                result.addElement(ms);
                            }
                        }
                 else if(restricted && ALLOW_RESTRICTED_SACRIFICE && c.contents!=NoSpace)
                 	{
                	 if(board.CanBeIsolated(c))
                	 	{ 
                		 result.clear();
                		 getBoardMoveList(false,result);
                		 return;
                	 	}
                 	}
                 }
                }
        
            break;
            
        case SETRING_STATE:
        	if(board.rings_removed>25) 
        		{ result.addElement(new movespec(p, MOVE_DONE));
        		  break; 
        		}
            board.addRingMoves(result,true);
            break;
        case RING_STATE:
        // remove possible rings
        // place a ball in each possible position
        {
            boolean limitrings = false; //(lvl>=MaxDepth) && (Strategy<=BP_MAX);
            board.addRingMoves(result,limitrings);
        }

        break;

        case CAPTURE_STATE:
        	// start a capture
        	board.addCaptureMoves(result);
         	break;

        case CONTINUE_STATE:
        	// continue a capture in progress
        	board.addContinueCaptures(result);
            break;

        default:
        	throw G.Error("Can't move in this state: %s", state);
        }
        if(restricted && ((result.size()==0) || (emptyCount<5)))
        {
        	result.clear();
        	getBoardMoveList(false,result);
        	return;
        }
        //for(int i=0;i<result.size();i++)
        // { movespec el = (movespec)result.elementAt(i);
        //   System.out.println(el.toString());
        // }
    }


}
