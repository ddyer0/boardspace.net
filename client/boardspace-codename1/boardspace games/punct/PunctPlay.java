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
package punct;

import lib.*;
import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;
/** 
 * Punct uses alpha-beta only
 * 
 * the Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * in general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * 
 * The big problem for punct is that the move width is initially about 4K, so 
 * both search depth and width have to be severly curtailed to get anything like
 * a reasonable speed for a recreational program.  For "drop" moves, it's reasonable
 * to just omit a lot of them, and for "move" moves, it's reasonable to omit moves
 * that do not increase connectivity of contact.  The point where this becomes 
 * unreasonable as a strategy is when a win is in sight, and at that point you
 * have to be very careful to not exclude the winning drop moves or any of the 
 * moves that could successfully prevent them.
 * 
 * @author ddyer
 *
 *
 *tofix: 
 * avoid draws by tweaking the evaluation
 * avoid "suicide" drops when a center win is inevitable
 */
public class PunctPlay extends commonRobot<PunctGameBoard> implements Runnable, PunctConstants,
    RobotProtocol
{	// evaluator weights
	static double span_weight=5.0;
	static double thick_weight=0.05;		// value per multiple path in a span
	static double contact_weight=-0.2;		// negative value for contacts (potential bridges)
	static double halfPincer_weight=-0.1;	// contact matched by non-drop zone
	static double pincer_weight=-1.0;		// contact matched by a corresponding contact
	static double empty_weight=0.01;		// value for empty spaces (effecient shapes)
	static double center_weight=0.05;		// value for empty center adjacent (harder to bridge)
	static double pline_weight=0.1;		// value of punct lines crossing
	static double center_score_weight = 0.1;// value for occupying a center point
	static int xtended_points = 40;			// extended points to consider a connection solid
	static double threat_bonus = 50.0;		// bonus for making a winning threat
	
	boolean useExtendedSpan=true;
    boolean SAVE_TREE = false;				// debug flag for the search driver
    int DEFAULT_DEPTH = 3;
    int MAX_DEPTH = DEFAULT_DEPTH;
	static final boolean KILLER = false;	// if true, allow the killer heuristic in the search
    /* strategies */
    final int SIMPLE_MAX = 0;
    final int BETTER_MAX = 1;
    punctBlob defendFromBlob=null;
    punctBlob attackingBlob=null;
    boolean raceToCenter = false;
    boolean attackMode = false;
    int Strategy = 	SIMPLE_MAX;
    
    int boardSearchLevel = 0;					// the current search depth

    static final double WIN_VALUE = 100000.0;
    static final double MOVE_PENALTY = 1.0;
    
    int requireDropBloBits = 0;
    int requireMoveBloBits = 0;
    
    /* constructor */
    public PunctPlay()
    {
    }
    
    /** prepare the robot, but don't start making moves.  G is the game object, gboard
     * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
     * are parameters from the applet that can be interpreted as desired.
     */
     public void InitRobot(ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
            String evaluator, int strategy)
        {
            InitRobot(newParam, info, strategy);
            GameBoard = (PunctGameBoard) gboard;
            board = GameBoard.cloneBoard();
            //if(board.whoseTurn==0) { useExtendedSpan=false; }
            Strategy = ((strategy==WEAKBOT_LEVEL) || (strategy==DUMBOT_LEVEL)) ? SIMPLE_MAX : BETTER_MAX;
            useExtendedSpan = true;
            System.out.println("Starting robot "+Strategy);
            MAX_DEPTH = (strategy==WEAKBOT_LEVEL) ? DEFAULT_DEPTH-1 : DEFAULT_DEPTH;
        }
     

/** undo the effect of a previous Make_Move.  These
 * will always be done in reverse sequence
 */
    public void Unmake_Move(commonMove m)
    {	Punctmovespec mm = (Punctmovespec)m;
    	if((requireDropBloBits!=0) && (mm.op==MOVE_MOVE))
    		{ board.setDropZoneValue=true;
    		  board.setDropZoneToValue=false;
    		}
        board.UnExecute(mm);
        board.setDropZoneValue=false;
        boardSearchLevel--;
    }
/** make a move, saving information needed to unmake the move later.
 * 
 */
    public void Make_Move(commonMove m)
    {   Punctmovespec mm = (Punctmovespec)m;
    	boardSearchLevel++;
    //if((mm.op==MOVE_MOVE)
    //		&& (mm.from_col=='H')
    //		&& (mm.to_col=='H')
    //		&& (mm.from_row==15)
    //		&& (mm.to_row==14))
    //{ System.out.println("MM "+mm);
    //}
	   	if((requireDropBloBits!=0) && (mm.op==MOVE_MOVE))
		{ board.clearDropZoneValue=true; 
		}
        board.RobotExecute(mm);
        board.clearDropZoneValue=false;
    }

/** return an enumeration of moves to consider at this point.  It doesn't have to be
 * the complete list, but that is the usual procedure. Moves in this list will
 * be evaluated and sorted, then used as fodder for the depth limited search
 * pruned with alpha-beta.
 */
    public CommonMoveStack  List_Of_Legal_Moves()
    {	return(Vector_Of_Legal_Moves(false));
    }
    
    // true if the opponent will win with any drop move
    public boolean anyOpponentDropWins()
    { 	int player = board.whoseTurn;
		return(((NUMREALPIECES-board.piecesOnBoard[nextPlayer[player]])==1)
				&& (board.centerScore[player]<board.centerScore[nextPlayer[player]]));
    }
    public CommonMoveStack  Vector_Of_Legal_Moves(boolean printOnce)
    {  	CommonMoveStack  moves = new CommonMoveStack();
    	int typesRemaining = board.pieceTypesRemaining();
    	int skip = (int)(20.0 *  (1.0-((NUMPIECETYPES-(double)typesRemaining)/NUMPIECETYPES)));
 		boolean limit_moves = false;	// if true, limit move-moves to plausible
 		boolean substitute_drops = false;	//if true, filter moves that should be drops
 		int move_require_drop = requireMoveBloBits|requireDropBloBits;
 		int drop_require_drop = requireDropBloBits;
 		boolean rotationFilter = false;
 		boolean anyOpponentDropWins = anyOpponentDropWins();
 		String comment = "";
 		if(requireDropBloBits!=0) 
 			{ 
 			  limit_moves=true; 	// consider only contact or center "move" moves
 			  substitute_drops=true;// dont consider moves moves that should be drop moves
 			  if(drop_require_drop==CENTER_BLOBIT)
 			  { // aiming for the center, but still need places to drop
 				  drop_require_drop=0;
 				  if(move_require_drop==CENTER_BLOBIT)
 				  {	// if only center moves got noticed (no attacks or defense) then don't filter
 					// on blobits at all.  In that case, moves to center are allowed anyway, but
 					// also shuffling around moves that could be useful in jockying for position
 					if(anyOpponentDropWins) 
					{ // we don't need to consider drop moves, since they won't help
 					  // we also don't need to consider shuffling moves, since there is no time
 					  // for them to be effective.
					}
 					else
 					{ move_require_drop=0;	// allow moves that just shuffle around 
					  rotationFilter=true; 	// restrict the number of rotations tried
					  comment=" shuffle but don't rotate";
  					}
   				  }
 			  }
 			  else
 			  {
 			  if((requireDropBloBits==KILLER_BLOBIT))
 				   { comment = " avoid immediate loss";
 				   	 skip = 1;				// consider all "drop" moves, but there's a list of spots
 				   }
 			  else { skip=5; comment = "\nDefend "+defendFromBlob+" Attack "+attackingBlob; }
 			  }
 			}
 		else
 		    {  
 		       limit_moves = true;
 		       substitute_drops = true;
  		    }
 		
 		if(anyOpponentDropWins)
 		{
		  comment=" any drop wins";
		  skip = 100;
 		}
 		
 		board.GetListOfDropMoves(moves,skip,drop_require_drop);
 
 		int dropCount = moves.size();
 		
 		if(board.piecesOnBoard[board.whoseTurn]>4)
 		{ board.GetListOfMoveMoves(moves,limit_moves,rotationFilter,substitute_drops,move_require_drop);
 		}
 		
 		int totalCount = moves.size();
 		int moveCount = totalCount-dropCount;
 		
    	if(printOnce) 
    	{ System.out.println("Skip="+skip+"="+dropCount+" drop moves + "+moveCount+" move moves="+totalCount+comment); 
     	}
         return (moves);
    }
    
	double blobScore(punctBlob bb,boolean print)
	{	int span = bb.maxSpan();
		double xspan = useExtendedSpan ? bb.extendedSpan(xtended_points): span;
		double threatscore = useExtendedSpan ? ((bb.extendedSpan(1)==WINNING_SPAN) ? threat_bonus : 0.0) : 0.0;
		double span2=span*xspan;
		double spanscore = span2*span_weight;
		double thickscore = bb.thickness*thick_weight;
		double contactscore = bb.contacts*contact_weight*span2;
		double halfPincerscore = bb.halfPincers*halfPincer_weight*span2;
		double pincerscore = bb.pincers*pincer_weight*span2;
		double emptyscore = bb.empties*empty_weight;
		double centerascore = bb.emptyCenterAdjacent*center_weight;
		double plinescore = bb.plineCount*pline_weight;
		if(print)
		{ System.out.println(
			"  span="+spanscore
			+ ((threatscore>0.0)? (" threat "+threatscore) : "")
			+ " thick="+thickscore
			+ " contact="+contactscore
			+ " halfp="+halfPincerscore
			+ " pincer="+pincerscore
			+" empty="+emptyscore
			+ " centera="+centerascore
			+ " pline="+plinescore);
		}
		return(
				spanscore + threatscore
				+ thickscore
				+ contactscore
				+ pincerscore
				+ halfPincerscore
				+ emptyscore
				+ centerascore
				+ plinescore
				);
	}
	
    /** return a value of the current board position for the specified player.
     * this should be greatest for a winning position.  The evaluations ought
     * to be stable and greater scores should indicate some degree of progress
     * toward winning.
     * @param player
     * @return
     */
	private double ScoreForPlayer(PunctGameBoard evboard,int player,boolean print)
    {	double val=0.0;
    	// player with control of the center gets square of margin
    	// other player gets zero
    	double dscore = evboard.centerScore[player]-evboard.centerScore[nextPlayer[player]];
    	dscore = (dscore>0) ? dscore*dscore : 0;
    	int pieces = Math.max(evboard.piecesOnBoard[player]-10,1);
    	double center = center_score_weight*pieces*pieces*dscore;
    	if(print) { System.out.println(); }
       	if(evboard.WinForPlayerNow(player))	// also calculate lines array
    	{	if(print) { System.out.println("Win for "+player); }
    		// the extra half win is slop, so that whatever plusses and minuses are added, it's still more than a win
    		val = WIN_VALUE+WIN_VALUE/2-evboard.moveNumber*MOVE_PENALTY;	
    		evboard.WinForPlayerNow(player);
    	}
       	else if(evboard.piecesOnBoard[player]==NUMREALPIECES)
       	{	// a center drtaw
       		val = (-WIN_VALUE/2.0)-evboard.moveNumber*MOVE_PENALTY;
       		
       	}	
    	punctBlob[] blobs = evboard.blobs;
    	double bigblob = 0.0;
    	double allblob = 0.0;
    	int nblob = 0;
    	// we have to be very careful about how multiple blobs
    	// are counted.  If creating or destroying blobs greatly
    	// affects the score, then odd moves will result, for example
    	// if merging two blobs results in a smaller value than two separate
    	// blobs...
    	PunctColor color = board.playerColors[player];
    	for(int i=0;i<evboard.numBlobs;i++) 
    	{ punctBlob bl = blobs[i];
    	  if(bl.color==color) 
    	  { double sc = blobScore(bl,print);
    	  	bigblob = Math.max(sc,bigblob);
    	  	nblob++;
    	    allblob += sc;
    	    if(print) { System.out.println("blob "+bl+" += "+sc); }
    	  }
    	}
    	if(nblob>0) { val += bigblob + (allblob-bigblob)/(nblob*100.0); }
    	if(center>0)
    	{	val += center;
    		if(print) { System.out.println("Center += "+center); }
    	}
     	return(val);
    }
    
    /**
     * this is it! just tell me that the position is worth.  
     */
    // TODO: refactor static eval so GameOver is checked first
    public double Static_Evaluate_Position(commonMove m)
    {	int playerindex = m.player;
        double val0 = ScoreForPlayer(board,playerindex,false);
        double val1 = ScoreForPlayer(board,nextPlayer[playerindex],false);
        if((val0>=WIN_VALUE) && (val1>=WIN_VALUE)) { val1 = val1/2; }
       return(val0-val1);
    }
    /**
     * called as a robot debugging hack from the viewer.  Print debugging
     * information about the static analysis of the current position.
     * */
    public void StaticEval()
    {
            PunctGameBoard evboard = GameBoard.cloneBoard();
            evboard.countBlobs(false);
            double val0 = ScoreForPlayer(evboard,FIRST_PLAYER_INDEX,true);
            double val1 = ScoreForPlayer(evboard,SECOND_PLAYER_INDEX,true);
            System.out.println("Eval is W "+ val0 +"- B "+val1+ " = " + (val0-val1));
    }

    /** this is called from the search driver to evaluate a particular move
     * 
     */
    public double Static_Evaluate_Move(commonMove m)
    {	Punctmovespec mm = (Punctmovespec)m;
    	if(mm.evaluated) { return(mm.local_evaluation()); }
    	
        board.RobotExecute(mm);
        double val = Static_Evaluate_Position(mm);
        mm.set_local_evaluation(val);
        mm.setEvaluation(val);
        mm.evaluated = true;
        
        board.UnExecute(mm);
        return (val);
      }

    /** copy the game board, in preparation for a search */
    public void InitBoardFromGame()
    {
        board.copyFrom(GameBoard);
    }
 
 void addBlobits(punctBlob cb)
 {
	if(cb!=null)
	{	requireDropBloBits |= cb.bloBit;
		G.Assert(cb.bloBit!=0,"has to have a bit");
		for(crossLink conn = cb.crossLinks; conn!=null; conn=conn.next)
		{	requireDropBloBits |= conn.to.bloBit;
		}
		requireDropBloBits |= KILLER_BLOBIT;	// this gets the place moves came from in depth search
		attackMode = true;
	}
 }
 void ConsiderAttacks(int attackSpan)
 {	attackMode = false;
	defendFromBlob=null;
	attackingBlob=null;
	PunctColor whichColor = board.playerColors[board.whoseTurn];
	for(int i=0;i<board.numBlobs;i++)
	{	punctBlob cb = board.blobs[i];
		double extc = cb.extendedSpan(xtended_points);
		if(extc>=attackSpan)
		{	if(cb.color==whichColor)
			{
			if( (attackingBlob==null)||(extc>attackingBlob.extendedSpan(xtended_points)))
					{ attackingBlob = cb;
					}
			}
			else
			{
				if((defendFromBlob==null)||(extc>defendFromBlob.extendedSpan(xtended_points)))
				{ defendFromBlob = cb;
				}
			}
		}
	}
	addBlobits(defendFromBlob);
	addBlobits(attackingBlob);
 }
 Punctmovespec WinningMoveAnalysis()
 {	// make sure we don't miss a winning move, or miss defending
	// against a winning move
	int player = board.whoseTurn;
	board.countBlobs(true);
	raceToCenter= (((NUMREALPIECES-board.piecesOnBoard[player])<=2)
			||((NUMREALPIECES-board.piecesOnBoard[nextPlayer[player]])<=3));
	
	// in the advanced robot, consider attacking or defending a particular blob
	// we have to do this first, while the blobs still have their original values
	if(Strategy!=SIMPLE_MAX) 
		{ 
		ConsiderAttacks(raceToCenter ? WINNING_SPAN : WINNING_SPAN-4); 
		}

	{
	CommonMoveStack  firsts = board.GetListOfDropMoves(null,1,0);	// guaranteed to include any winning moves
	board.GetListOfMoveMoves(firsts,true,false,true,0);// ok to filter here because the winning moves are never filtered
	int nmoves = firsts.size();

	// if we have an immediate win, just do it.
	for(int i=0; i<nmoves; i++)
	{	Punctmovespec mm = (Punctmovespec)firsts.elementAt(i);
		double mv = Static_Evaluate_Move(mm);
		if(mv>=WIN_VALUE) { return(mm); }
	}
	
	// if we're approaching the end by running out of pieces, be sure all moves into the center are considered
	if(raceToCenter) 
	{	for(punctCell c = board.allCells; c!=null; c=c.next)
		{ if(c.centerArea) { c.bloBits |= CENTER_BLOBIT; } 
		}
		// there are no drop moves in the center, but this acts as a flag
		// to severely restrict the number of drop moves considered.   
		requireDropBloBits |= CENTER_BLOBIT;	
		requireMoveBloBits |= CENTER_BLOBIT;	// can be combined with attack and defend moves
	}


	}
	
	// consider the opponent's immediately winning moves
	{
	board.togglePlayer();
	CommonMoveStack  secondMoves = board.GetListOfDropMoves(null,1,0);
	board.GetListOfMoveMoves(secondMoves,true,false,true,0);	// ok to filter here because the winning moves are never filtered
	int nmoves = secondMoves.size();
	int nkiller = 0;
	boolean anyDropWins = ((NUMREALPIECES-board.piecesOnBoard[board.whoseTurn])==1);
	for(int i=0; i<nmoves; i++)
	{	Punctmovespec mm = (Punctmovespec)secondMoves.elementAt(i);
		double mv = Static_Evaluate_Move(mm);
		if(mv>=WIN_VALUE && (!anyDropWins || (mm.op!=MOVE_DROPB))) 
			{ // this causes setBoard to accumulate the inclusive or of the bloBits
			  // of all the cells that are covered by the move.   This enables attacks
			  // on the whole line as well as blocking moves. If any drop move wins,
			  // we're not interested in drop moves here.
			  board.setDropZoneValue=true;
			  board.setDropZoneToValue=true;
		      board.RobotExecute(mm);
		      board.setDropZoneValue=false;
		      board.setDropZoneToValue=false;
		      board.UnExecute(mm);
		      nkiller++;
			}
	}
	if(nkiller>0)
		{
		// opponent has some winning possibilities
		// if all the drop moves were wins, we don't need the list of all
		// the legal drops
		 requireDropBloBits = KILLER_BLOBIT; 	// deliberately ignore attackable/attacking
		 defendFromBlob=null;
		 attackingBlob=null;
		 System.out.println(nkiller + " winning moves to counter");
		// include attacks on the winning lines
		requireMoveBloBits |= (board.playerBloBits[nextPlayer[player]] & board.dropBloBits);
		}
	 if(raceToCenter) { System.out.println("racing to center"); }
	 board.togglePlayer();
	}
	return(null);
 }
 public void PrepareToMove(int playerindex)
 {
     InitBoardFromGame();
	 
 }
/** search for a move on behalf onf player p and report the result
 * to the game.  This is called in the robot process, so the normal
 * game UI is not encumbered by the search.
 */
 public commonMove DoAlphaBetaFullMove()
    {
        Punctmovespec move = null;
        requireDropBloBits=0;
        try
        {
            if (board.DoneState())
            { // avoid problems with gameover by just supplying a done
                move = new Punctmovespec("Done", board.whoseTurn);
            }

            if(move==null) { move = WinningMoveAnalysis(); }
            
            if(move==null)
            {
            // it's important that the robot randomize the first few moves a little bit.
            int randomn = RANDOMIZE 
            				? ((board.moveNumber <= 5) ? (20 - board.moveNumber*3) : 0)
            				: 0;
            boardSearchLevel = 0;

            int depth = MAX_DEPTH;
            CommonMoveStack  vm = Vector_Of_Legal_Moves(true);
            int nmoves = vm.size();
            // heuristically adjust the depth
            if(nmoves<110 && (board.piecesOnBoard[board.whoseTurn]>5)) { depth++; }
            if(nmoves<160 
            	|| anyOpponentDropWins() 
            	|| (raceToCenter && (Strategy!=SIMPLE_MAX)))
            	{ depth++; 
            	}
            Search_Driver search_state = Setup_For_Search(depth, false);
            search_state.save_all_variations = SAVE_TREE;
            search_state.allow_killer = KILLER;
            search_state.verbose=1;
            search_state.save_digest=false;	// debugging only
            move = (Punctmovespec) search_state.Find_Static_Best_Move(randomn);
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
