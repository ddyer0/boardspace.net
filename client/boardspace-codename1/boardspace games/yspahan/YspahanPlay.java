package yspahan;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;
import yspahan.YspahanConstants.ystate;

/** 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * 
 * Yspahan robots implemented by Gunther Rosenbaum "guenther" based on the robots
 * from his windows program.
 *  
 *  2DO: If the game is interrupted while the robot has played some but not all moves of a sequence,
 *  the game won't restart correctly.  Presumably this also applies to guests joining.
 *  
 * @author ddyer
 *
 */
public class YspahanPlay extends commonRobot<YspahanBoard> implements Runnable, YspahanPlayStratConst,
    RobotProtocol
{  	static final double VALUE_OF_WIN = 1000000.0;

	
	boolean SAVE_TREE = false;				// debug flag for the search driver
    boolean KILLER = false;					// probably ok for all games with a 1-part move
    
    static final int DUMBOT_DEPTH = 3;
    static final int GOODBOT_DEPTH = 4;
    static final int BESTBOT_DEPTH = 6;
    int MAX_DEPTH = BESTBOT_DEPTH;
     /* strategies */
    double CUP_WEIGHT = 1.0;
    double MULTILINE_WEIGHT=1.0;
    boolean DUMBOT = false;
    
  //Ro
    IYspahanPlayStrat [] robotAI;
    static boolean[] robots = {false,false,false,false};
    int player = -1;
        
    
    
    /* constructor */
    public YspahanPlay()
    {
    }


/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   YspahanMovespec mm = (YspahanMovespec)m;
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
    double ScoreForPlayer(YspahanBoard evboard,int player,boolean print)
    {	
     	boolean win = evboard.WinForPlayerNow(player);
    	if(win) { return(VALUE_OF_WIN); }
    	return(evboard.ScoreForPlayer(player,print));

    }


/** prepare the robot, but don't start making moves.  G is the game object, gboard
 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
 * are parameters from the applet that can be interpreted as desired.
 */
 public void InitRobot(ViewerProtocol v,ExtendedHashtable info, BoardProtocol gboard, String evaluator,
        int strategy)
    {
        InitRobot(v, info, strategy);
        GameBoard = (YspahanBoard) gboard;
        board = (YspahanBoard)GameBoard.cloneBoard();
        int numPlayer = GameBoard.nPlayers();

        robotAI = new IYspahanPlayStrat[numPlayer]; 
        for (int i = 0; i < robotAI.length; i++)
        {	
        	robotAI [i] =  YspahanPlayStratFactory.getStrategy(strategy,i, numPlayer,board.randomKey);
        }
        
        switch(strategy)
        {
        case WEAKBOT_LEVEL:
        case DUMBOT_LEVEL:
        	MAX_DEPTH = DUMBOT_DEPTH;
         	DUMBOT=true;
        	break;
        case SMARTBOT_LEVEL:
        	MAX_DEPTH = GOODBOT_DEPTH;
        	DUMBOT=false;
         	break;
        case BESTBOT_LEVEL:
         	MAX_DEPTH = BESTBOT_DEPTH;
        	DUMBOT=false;
        	break;
        case MONTEBOT_LEVEL:
        	MONTEBOT = true;
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
 	player = playerIndex;
 	robotAI[player].copyBoardState(board,robots);
 	robots[player] = true;
 }

 public commonMove DoAlphaBetaFullMove()
    {
	 YspahanMovespec move = null;
	 	if (myDebug) {System.out.println("player: " + player + " , state= " + board.board_state);	}
	 	
		try
        {
        	switch (board.board_state)
        	{
			case GAMEOVER_STATE:
		          move = new YspahanMovespec("Done", board.whoseTurn);
		          return (move);
        	
			case ROLL_STATE:
				if (robotAI[player].getMoveList().size() == 0)
				{				
					robotAI[player].throwDices();
				}
				break;

			case SELECT_STATE:
			case TAKE_CARD_STATE:	
				if (robotAI[player].getMoveList().size() == 0)
				{				
					 robotAI[player].doMoveMain();
				}
				break;

			case BUILD_STATE:
				if (robotAI[player].getMoveList().size() == 0)
				{			
					 robotAI[player].cardsAfterMoveMainAction();
					 robotAI[player].doMoveBuilding();
				}
				else
				{
//					System.out.println(" ");
				}
				break;

			case PAY_CAMEL_STATE:
				robotAI[player].supervisorSendCubeToCaravan(board.selectedCube);
				break;

			case DESIGNATE_CUBE_STATE:
				// 2 cubes are affected by supervisor; initiator should decide which one is send first to caravan
				robotAI[player].supervisorDesignateCube();
				break;

			case DESIGNATED_CUBE_STATE:
				// give a quit for sending cubes to caravan
				if (myDebug) {System.out.println("DONE for Designated cubes!!  player: "+ player + " , state= " + board.board_state);}
				move = new YspahanMovespec("Done", board.whoseTurn);
				return move;

			default: 
				break;
			}    	        	
        	
        	
//** Now moves are generated; provide sequences of moves to framework!        	
        	
        	if (robotAI[player].getMoveList().size()>0)
			{
				move = robotAI[player].getMoveList().remove(0);
			}
        	
        }	
		catch (Throwable e)
		{
			throw G.Error("Exeption in robot " + board.board_state + "  " + e.toString());
		}

            
        finally
        {
        	//
        }

        if (move != null)
        {
            if(G.debug() ) { move.showPV("exp final pv: "); }
            // normal exit with a move
            return (move);
        }

      if (board.DoneState())
      { // avoid problems with gameover by just supplying a done
          move = new YspahanMovespec("Done", board.whoseTurn);
          if (myDebug) {System.out.println("********** Autogenerated Done; player: "+ player + " , state= " + board.board_state);}
          return (move);
      }
       
        continuous = false;
        throw G.Error("Robot: No move generated!"  + " , state= " + board.board_state);
    }
 

 // this is the monte carlo robot, which for hex is much better then the alpha-beta robot
 // for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
 // evaluator other than winning a game.
 public commonMove DoMonteCarloFullMove()
 {	commonMove move = null;
 	try {

        {
        // it's important that the robot randomize the first few moves a little bit.
        double randomn = (RANDOMIZE && (board.moveNumber <= 6)) ? 0.1/board.moveNumber : 0.0;
        UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this);
        monte_search_state.blitz = true;
        monte_search_state.save_top_digest = false;	// always on as a background check
        monte_search_state.save_digest=false;	// debugging only
        monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
        monte_search_state.timePerMove = 10;		// 20 seconds per move
        monte_search_state.verbose = 3;
        monte_search_state.alpha = 0.5;
         monte_search_state.only_child_optimization = false;
        monte_search_state.simulationsPerNode = 1;
        move = monte_search_state.getBestMonteMove();
        }
 		}
      finally { ; }
      if(move==null) { continuous = false; }
     return(move);
 }
 public double normalizedRescore(YspahanMovespec move,int forplayer)
 {	
 	double max = move.maxPlayerScore();
	return(move.playerScores[forplayer]-max);
 }
 /**
  * for UCT search, return the normalized value of the game, with a penalty
  * for longer games so we try to win in as few moves as possible.  Values
  * must be normalized to -1.0 to 1.0
  */
 public double NormalizedScore(commonMove lastMove)
 { 	G.Assert(board.board_state==ystate.GAMEOVER_STATE, "game is over");
	YspahanMovespec move = (YspahanMovespec)lastMove;
 	int player = move.player;
 	board.getScores(move);
 	return(normalizedRescore(move,player));
 }


 }