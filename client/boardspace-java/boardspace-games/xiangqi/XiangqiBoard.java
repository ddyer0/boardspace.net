package xiangqi;

import java.awt.Color;
import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;

/**
 * XiangqiBoard knows all about the game of Xiangqi, which is played
 * on a 7x7 board. It gets a lot of logistic support from 
 * common.rectBoard, which knows about the coordinate system.  
 * 
 * This class doesn't do any graphics or know about anything graphical, 
 * but it does know about states of the game that should be reflected 
 * in the graphics.
 * 
 *  The principle interface with the game viewer is the "Execute" method
 *  which processes moves.  Note that this
 *  
 *  In general, the state of the game is represented by the contents of the board,
 *  whose turn it is, and an explicit state variable.  All the transitions specified
 *  by moves are mediated by the state.  In general, my philosophy is to be extremely
 *  restrictive about what to allow in each state, and have a lot of tripwires to
 *  catch unexpected transitions.   We expect to be fed only legal moves, but mistakes
 *  will be made and it's good to have the maximum opportunity to catch the unexpected.
 *  
 * Note that none of this class shows through to the game controller.  It's purely
 * a private entity used by the viewer and the robot.
 * 
 * @author ddyer
 *
 * todo: perfect the perpetual chase/perpetual check logic.  Muller's recommentation
 * from http://www.jcraner.com/qianhong/forum/viewtopic.php?p=428#428
 * 
 * Chases are more tricky. In the following concise definition, the term 'legal move' means a move that
creates a position that does not leave the mover in check. It does not necessarily mean that the
position from which the move starts is a legal position; King capture might be possible, e.g. to
determine if you attack something with a move that delivers check at the same time.

1) A piece is chased on a given move if there exists at least one piece of the moving side that satisfies all following criteria:
   a) it can capture the chased piece with a legal move after the given move, when the same side would be allowed to move again
   b) it could not yet legally capture the chased piece before the move (when it really was his turn)
   c) it is not a King or a Pawn
   d) it can not be captured through a legal move by the chased piece when it is of equal type
   e) after making the capture, it can not be recaptured by any legal move, or
      it is a Horse or Cannon, and it attacks a Rook

2) A sequence of moves is a perpetual chase (by one side), if every move (by that side) leading
   from the previous occurrence of the position to the repeated one is a chase.

3) A perpetual chase is forbidden, if there exists at least one piece that is chased on every move of the perpetual,
   and that piece is not a Pawn that has not crossed the River yet.

4) The outcome of a repetitive game is determined as follows (in order of proiority):
   a) When both sides are perpetually checking, it is a draw, irrespective of whether they are also chasing.
   b) When one side is perpetually checking, and the other not, the checking side loses,
      even if the other side was making a forbidden perpetual chase.
   c) when both sides make a forbidden perpetual chase, it is draw, irrespective of the type of pieces chased.
   d) when only one side makes a forbidden perpetual chase, that side loses
   e) otherwise, it is draw 
 *
 */

public class XiangqiBoard extends rectBoard<XiangqiCell> implements BoardProtocol,XiangqiConstants
{
	private XiangqiState unresign;
	private int lastDrawMove = 0;
	private XiangqiState board_state;
	public XiangqiState getState() {return(board_state); }
	public void setState(XiangqiState st) 
	{ 	unresign = ((st==XiangqiState.RESIGN_STATE)||(st==XiangqiState.OFFER_DRAW_STATE))?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public RepeatedPositions repeatedPositions = null;
    public int boardColumns = DEFAULT_COLUMNS;	// size of the board
    public int boardRows = DEFAULT_ROWS;
    public void SetDrawState() 
    { if(prev_state!=XiangqiState.CHECK_STATE) { setState(XiangqiState.ILLEGAL_MOVE_STATE); }}
    public XiangqiCell rack[][] = null;
 	public CellStack occupied[] = new CellStack[2];
    public XiangqiCell kingPosition[] = new XiangqiCell[2];
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public XiangqiChip pickedObject = null;
    public XiangqiCell pickedSource = null;
    public XiangqiCell droppedDest = null;
    public XiangqiCell prevDroppedDest = null;
    public XiangqiCell prevPickedSource = null;
    public CellStack animationStack = new CellStack();
    public XiangqiState prev_state;
    private XiangqiCell captured = null;
    private int progressMove = 0;			// move number at last pawn move or capture
    
    public void DrawGridCoord(Graphics gc, Color clr,int xpos, int ypos, int cellsize,String txt)
    {  	if(Character.isDigit(txt.charAt(0)))
    		{ xpos -= cellsize/2;
    		}
    	super.DrawGridCoord(gc,clr,xpos,ypos,cellsize,txt);
    }
    public int movesSinceProgress() { return(moveNumber-progressMove); }
    

	// factory method
	public XiangqiCell newcell(char c,int r)
	{	return(new XiangqiCell(c,r));
	}
    public XiangqiBoard(String init,long key,RepeatedPositions rep,int map[]) // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = new String[3];
    	 Grid_Style[0]=XIANGQIGRIDSTYLE[0]; //coordinates left and bottom
    	 Grid_Style[1]=XIANGQIGRIDSTYLE[1]; //coordinates left and bottom
    	 Grid_Style[2]=XIANGQIGRIDSTYLE[2]; //coordinates left and bottom
    	 repeatedPositions = rep;			//share the repeated positions list with the viewer
    	 setColorMap(map, 2);
    	 doInit(init,key); // do the initialization 
         autoReverseY();		// reverse_y based on the color map
     }

    public int aggressivePotential(int who)
    {	CellStack occ = occupied[who];
    	int potential = 0;
    	for(int i=0,lim=occ.size(); i<lim; i++)
    	{
    		XiangqiCell c = occ.elementAt(i);
    		XiangqiChip top = c.topChip();
    		switch(top.pieceType)
    		{
    		default: throw G.Error("not expecting %s",top);
    		case XiangqiChip.GENERAL_INDEX: 	
       		case XiangqiChip.GUARD_INDEX: 	
       		case XiangqiChip.ELEPHANT_INDEX: 	
       			break;
       		case XiangqiChip.SOLDIER_INDEX:
       		case XiangqiChip.HORSE_INDEX:
       		case XiangqiChip.CHARIOT_INDEX:
       		case XiangqiChip.CANNON_INDEX:
       			potential++;
       			break;
 			
    		}
    	}	
    
    	return(potential);
	}
    int placements[][] = {
    		{'E',1,XiangqiChip.GENERAL_INDEX},
    		{'F',1,XiangqiChip.GUARD_INDEX},
       		{'D',1,XiangqiChip.GUARD_INDEX},
       		{'C',1,XiangqiChip.ELEPHANT_INDEX},
       		{'G',1,XiangqiChip.ELEPHANT_INDEX},
      		{'B',1,XiangqiChip.HORSE_INDEX},
       		{'H',1,XiangqiChip.HORSE_INDEX},
     		{'A',1,XiangqiChip.CHARIOT_INDEX},
       		{'I',1,XiangqiChip.CHARIOT_INDEX},
       		{'B',3,XiangqiChip.CANNON_INDEX},
       		{'H',3,XiangqiChip.CANNON_INDEX},
       		{'A',4,XiangqiChip.SOLDIER_INDEX},
       		{'C',4,XiangqiChip.SOLDIER_INDEX},
       		{'E',4,XiangqiChip.SOLDIER_INDEX},
       		{'G',4,XiangqiChip.SOLDIER_INDEX},
       		{'I',4,XiangqiChip.SOLDIER_INDEX},
    };
    // standared init for Hex.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard(String game)
    { 	if(Xiangqi_INIT.equalsIgnoreCase(game)) 
    		{ boardColumns=DEFAULT_COLUMNS; 
    		  boardRows = DEFAULT_ROWS;
    		}
    	else { throw G.Error(WrongInitError,game); }
        gametype = game;
        setState(XiangqiState.PUZZLE_STATE);
        initBoard(boardColumns,boardRows); //this sets up the board and cross links
        reInit(occupied);
        
        for(int i=0; i<placements.length; i++)
        { int loc[] = placements[i];
          XiangqiCell c = getCell((char)loc[0],11-loc[1]);
          addChip(c,XiangqiChip.getChip(0,loc[2]));
          XiangqiCell d = getCell((char)loc[0],loc[1]);
          addChip(d,XiangqiChip.getChip(1,loc[2]));
       }
        progressMove = 0;
        whoseTurn = FIRST_PLAYER_INDEX;
 
    }

    public void sameboard(BoardProtocol f) { sameboard((XiangqiBoard)f); }
    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(XiangqiBoard from_b)
    {	super.sameboard(from_b);
    	sameCells(occupied,from_b.occupied);
    	sameCells(rack,from_b.rack);
    	sameCells(kingPosition,from_b.kingPosition);
 
        // here, check any other state of the board to see if
        G.Assert((pickedObject==from_b.pickedObject),"pickedObject matches");
        G.Assert((progressMove == from_b.progressMove),"progressMove matches");
        G.Assert(XiangqiCell.sameCell(pickedSource,from_b.pickedSource),"same pickedSource");
        G.Assert(XiangqiCell.sameCell(droppedDest,from_b.droppedDest), "same droppedDest");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");

    }

    /** 
     * Digest produces a 32 bit hash of the game state.  This is used 3 different
     * ways in the system.
     * (1) This is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game, and a midpoint state of the game. Other site machinery
     *  looks for duplicate digests.  
     * (2) Digests are also used as the game is played to look for draw by repetition.  The state
     * after most moves is recorded in a hashtable, and duplicates/triplicates are noted.
     * (3) Digests are used by the search machinery as a check on the robot's winding/unwinding
     * of the board position, this is mainly a debug/development function, but a very useful one.
     * @return
     */
   public long Digest()
    {
       Random r = new Random(64 * 1000); // init the random number generator
       long v = super.Digest(r);

        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
		v ^= chip.Digest(r,pickedObject);
		v ^= cell.Digest(r,pickedSource);
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);

        return (v);
    }
   public XiangqiBoard cloneBoard() 
	{ XiangqiBoard copy = new XiangqiBoard(gametype,randomKey,repeatedPositions,getColorMap());
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((XiangqiBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(XiangqiBoard from_b)
    {	super.copyFrom(from_b);

        repeatedPositions = from_b.repeatedPositions.copy();
        progressMove = from_b.progressMove;
        pickedObject = from_b.pickedObject;
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        pickedSource = getCell(from_b.pickedSource);
        droppedDest = getCell(from_b.droppedDest);
        copyFrom(rack,from_b.rack);
        getCell(kingPosition,from_b.kingPosition);
        getCell(occupied,from_b.occupied);

        sameboard(from_b);
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {	randomKey = key;	// not used, but for reference in this demo game
    	Random r = new Random(647637);
    	rack = new XiangqiCell[2][XiangqiChip.NPIECETYPES];
	    for(int pl=FIRST_PLAYER_INDEX; pl<=SECOND_PLAYER_INDEX;  pl++)
	    {	occupied[pl] = new CellStack();
	    	for(int j=0;j<XiangqiChip.NPIECETYPES;j++)
	    	{  
	    	XiangqiCell cell = new XiangqiCell(r,'@',j,RackLocation[pl]);
	    	rack[pl][j]=cell;
	     	}    
	   }
       Init_Standard(gtype);
       allCells.setDigestChain(r);
       animationStack.clear();
        captured = null;
        pickedObject = null;
        pickedSource = null;
        droppedDest = null;
        prevPickedSource = null;
        prevDroppedDest = null;
        moveNumber = 1;
        unresign = null;
        lastDrawMove = 0;
        prev_state = XiangqiState.PUZZLE_STATE;

        // note that firstPlayer is NOT initialized here
    }


    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer()
    {
        switch (board_state)
        {
        case ILLEGAL_MOVE_STATE:
        default:
        	throw G.Error("Move not complete, can't change the current player");
         case PUZZLE_STATE:
            break;
        case CONFIRM_STATE:
        case DRAW_STATE:
        case RESIGN_STATE:
        case OFFER_DRAW_STATE:
        case ACCEPT_DRAW_STATE:
        case DECLINE_DRAW_STATE:
            moveNumber++; //the move is complete in these states
            setWhoseTurn(nextPlayer[whoseTurn]);
            return;
        }
    }

    /** this is used to determine if the "Done" button in the UI is live
     *
     * @return
     */
    public boolean DoneState()
    {	
        switch (board_state)
        {case RESIGN_STATE:
         case CONFIRM_STATE:
         case OFFER_DRAW_STATE:
         case ACCEPT_DRAW_STATE:
         case DECLINE_DRAW_STATE:
         case DRAW_STATE:
            return (true);

        default:
            return (false);
        }
    }
    public boolean DigestState()
    {	switch(board_state)
    	{	
    	case OFFER_DRAW_STATE:
    	case ACCEPT_DRAW_STATE:
    	case DECLINE_DRAW_STATE:
    		return(false);
    	default:
    		return(DoneState());
    	}
    }
    
    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(XiangqiState.GAMEOVER_STATE);
    }
    
    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	if(board_state==XiangqiState.GAMEOVER_STATE) { return(win[player]); }
    	// general captured is a loss
    	if(rack[nextPlayer[player]][XiangqiChip.GENERAL_INDEX].topChip()!=null) { return(true); }
    	return(false);
    }
    // look for a win for player.  
    public double ScoreForPlayer(int player,boolean print)
    {  	double finalv=0.0;
    	CellStack pieces = occupied[player];
    	CellStack opieces = occupied[nextPlayer[player]];
    	int aggression = 0;
    	for(int i=0,lim=pieces.size(); i<lim;i++)
    	{
    	XiangqiCell c = pieces.elementAt(i);
    	XiangqiChip top = c.topChip();
    	
    	// count the wood
    	switch(top.pieceType)
    	{
    	default: throw G.Error("not expecting %s",top);
    	case XiangqiChip.GENERAL_INDEX:	; 
    		break;
    	case XiangqiChip.GUARD_INDEX:
    		finalv += 2;
    		break;
    	case XiangqiChip.HORSE_INDEX:
    		// horses are woth more as the board clears
    		finalv += 8 - (opieces.size()+lim)/10.0;
    		aggression++;
    		break;
    	case XiangqiChip.CHARIOT_INDEX:
    		finalv += 12;
    		aggression++;
    		break;
    	case XiangqiChip.CANNON_INDEX:
    		// cannons are worth 9 at the beginning, declining to 5
    		// if everything is captured.
    		finalv += 5+((opieces.size()+lim)/8.0);
    		aggression++;
    		break;
    	case XiangqiChip.SOLDIER_INDEX:
    		{
    		double center = Math.abs(c.col-'E')/-20.0;
    		finalv += 1 + center;
    		if(c.acrossTheRiver(player)) { finalv += 1; }
    		aggression++;
    		}
    		break;
    	case XiangqiChip.ELEPHANT_INDEX:
    		finalv += 2;
    		break;
    	}
    	
    	}
    	if(aggression==0) { finalv = finalv/2; }
    	return(finalv);
    }


    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement()
    {	prevDroppedDest = droppedDest;
    	prevPickedSource = pickedSource;
        pickedObject = null;
        droppedDest = null;
        pickedSource = null;
        captured = null;
     }
    public void addChip(XiangqiCell c,XiangqiChip oo)
    {	if(c.onBoard)
    	{
    	G.Assert(c.topChip()==null,"currently empty");
    	int pl = playerIndex(oo);
    	occupied[pl].push(c);
    	if(oo.pieceType==XiangqiChip.GENERAL_INDEX) { kingPosition[pl]=c; }
    	}
    	c.addChip(oo);
    	
    }
    public int playerIndex(XiangqiChip chip) { return(getColorMap()[chip.colorIndex]);}
    
    public XiangqiChip removeChip(XiangqiCell c)
    {   G.Assert(c.topChip()!=null,"currently empty");
    	XiangqiChip rem = c.removeTop();
    	if(c.onBoard)
    	{int pl = playerIndex(rem);
    	 occupied[pl].remove(c,false);
    	 if(rem.pieceType==XiangqiChip.GENERAL_INDEX) { kingPosition[pl]=null; }
    	}
    	return(rem);
    }  
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    XiangqiCell dr = droppedDest;
    if(dr!=null)
    	{
    	droppedDest = null;
    	pickedObject = removeChip(dr);
    	if(captured!=null) { addChip(dr,captured.removeTop()); captured = null;  }
    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	XiangqiChip po = pickedObject;
    	if(po!=null)
    	{
    	XiangqiCell ps = pickedSource;
    	pickedSource=null;
    	pickedObject = null;
    	addChip(ps,po);
    	}
     }

    private XiangqiCell dropOnRack(XiangqiChip ob)
    {
    	XiangqiCell c = rack[playerIndex(ob)][ob.pieceType];
    	addChip(c,ob);
    	return(c);
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(XiangqiCell cell)
    {	return(droppedDest==cell);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    {	XiangqiChip ch = pickedObject;
    	if((ch!=null)&&(droppedDest==null))
    		{ return(ch.chipNumber());
    		}
        return (NothingMoving);
    }
    
   public XiangqiCell getCell(XiangqiCell c) 
   { if(c==null) { return(null); }
   	switch(c.rackLocation())
   	{
   	default: throw G.Error("not expecting %s",c.rackLocation);
   	case BoardLocation: return(getCell(c.col,c.row));
   	case Red_Chip_Pool: return(rack[RED_CHIP_INDEX][c.row]);
   	case Black_Chip_Pool: return(rack[BLACK_CHIP_INDEX][c.row]);
   	}
   }

   XiangqiChip pickFromRack(XId source,int oi)
   {  	switch(source)
	   	{
	   	default: throw G.Error("not expecting %s",source);
	   	case Red_Chip_Pool: pickedSource = rack[RED_CHIP_INDEX][oi];
	   		break;
	   	case Black_Chip_Pool: pickedSource = rack[BLACK_CHIP_INDEX][oi];
	   		break;
	   	}
   	   return(pickedObject = removeChip(pickedSource));
   }

    private void setNextStateAfterDrop()
    {	prev_state = board_state;
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case CONFIRM_STATE:
        case DRAW_STATE:
        	if(droppedDest!=null) { setNextStateAfterDone(); }
        	break;
        case PLAY_STATE:
        case ILLEGAL_MOVE_STATE:
        case CHECK_STATE:
			setState(XiangqiState.CONFIRM_STATE);
			break;

        case PUZZLE_STATE:
			acceptPlacement();
            break;
        }
    }
    //	
    //true if col,row is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(XiangqiCell c)
    {	return(pickedSource==c);
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterPick()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting pick in state %s", board_state);
        case ILLEGAL_MOVE_STATE:
        case CONFIRM_STATE:
        case DRAW_STATE:
        	setCurrentPlayState();
        	break;
        case PLAY_STATE:
        case CHECK_STATE:
			break;
        case PUZZLE_STATE:
            break;
        }
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
        case ILLEGAL_MOVE_STATE:
        default: throw G.Error("Not expecting state %s",board_state);
        case OFFER_DRAW_STATE:
        	setState(XiangqiState.QUERY_DRAW_STATE);
        	lastDrawMove = moveNumber;
        	break;
        case ACCEPT_DRAW_STATE:
        	setGameOver(false,false);
        	break;
    	case GAMEOVER_STATE: 
    		break;

        case DRAW_STATE:
        	setGameOver(true,false);	//forcing a draw by repetition is a loss
        	break;
    	case CONFIRM_STATE:
    	case PUZZLE_STATE:
    	case PLAY_STATE:
    	case CHECK_STATE:
    	case DECLINE_DRAW_STATE:
    		if( (movesSinceProgress()>=40)
    			|| ((aggressivePotential(whoseTurn)==0)
    				 && (aggressivePotential(nextPlayer[whoseTurn])==0)))
    			{ setGameOver(false,false);
    			} 
    			else
    			{
    				setCurrentPlayState();
    			}
    		break;
    	}

    }
   

    
    private void doDone()
    {	
        acceptPlacement();
        prev_state = board_state;
        if (board_state==XiangqiState.RESIGN_STATE)
        {	setGameOver(false,true);
        }
        else
        {	boolean win1 = WinForPlayerNow(whoseTurn);
        	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
        	if(win1 || win2)  { setGameOver(win1,win2); }
        	else {setNextPlayer(); setNextStateAfterDone(); }
        }
    }
    public void setCurrentPlayState()
    {	setState(XiangqiState.PLAY_STATE);
    	if(attackingPosition(occupied[nextPlayer[whoseTurn]],kingPosition[whoseTurn]))
    		{ setState(XiangqiState.CHECK_STATE);
    		}
    	// stalemate or checkmate
    	if(!hasMoves(whoseTurn)) { setGameOver(false,true); }
    }

    public boolean Execute(commonMove mm,replayMode replay)
    {	XiangqiMovespec m = (XiangqiMovespec)mm;
        //System.out.println("E "+m+" for "+whoseTurn);
   	   //checkOccupied();
       switch (m.op)
        {
        case MOVE_NULL:
        	setState(XiangqiState.CONFIRM_STATE);
 			//$FALL-THROUGH$
		case MOVE_DONE:
         	doDone();
			if(board_state==XiangqiState.CHECK_STATE)
					{
		 				m.check = true;
					}

            break;
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case PLAY_STATE:
        		case CHECK_STATE:
        			G.Assert((pickedObject==null)&&(droppedDest==null),"something is moving");
        			XiangqiCell from = getCell(m.from_col, m.from_row);
        			XiangqiCell to = getCell(m.to_col,m.to_row);
        			XiangqiChip rem = to.topChip();
        			if(replay!=replayMode.Replay)
        			{
        				animationStack.push(from);
        				animationStack.push(to);
        			}
        			if(rem!=null) 
        				{ XiangqiCell tocap = rack[playerIndex(rem)][rem.pieceType];
        				  XiangqiCell fromcap = to;
        				  addChip(tocap,removeChip(to)); 
        				  if(replay!=replayMode.Replay)
        				  {
        					  animationStack.push(fromcap);
        					  animationStack.push(fromcap);
        				  }
        				}
        			XiangqiChip moving = removeChip(from);
        			m.chip = moving;
        			pickedSource = from;
        			droppedDest = to;
        			addChip(to,moving);
        			m.captures = rem;
        			if(rem!=null) { progressMove = moveNumber; }
               		if((moving.pieceType==XiangqiChip.SOLDIER_INDEX)
                			&& (from.row!=to.row))
                		{	progressMove = moveNumber;
                		}
  				    setNextStateAfterDrop();
 
        			break;
        	}
        	break;
        case MOVE_DROPB:
        	G.Assert(pickedObject!=null,"something is moving");
			XiangqiCell to = getCell(m.to_col, m.to_row);
			XiangqiChip rem = to.topChip();
			if(rem!=null) 
				{captured = dropOnRack(removeChip(to)); 
				 progressMove = moveNumber; 
				 m.captures = rem;
				 if(replay!=replayMode.Replay)
				 {
					 animationStack.push(to);
					 animationStack.push(captured);
				 }
				}
  			if(replay==replayMode.Single)
			{
				animationStack.push(pickedSource);
				animationStack.push(to);
			}

            if(pickedSource==to) 
            	{ 
            	  unDropObject(); 
            	  unPickObject(); 
            	} 
            	else
            		{
            		addChip(droppedDest = getCell(m.to_col,m.to_row),pickedObject);
            		if((pickedObject.pieceType==XiangqiChip.SOLDIER_INDEX)
            			&& (pickedSource.row!=droppedDest.row))
            		{	progressMove = moveNumber;
            		}
            		
            		setNextStateAfterDrop();
            		}

            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	{
        	XiangqiCell from = getCell(m.from_col,m.from_row);
        	if(isDest(from))
        		{ if((captured!=null)&&(replay!=replayMode.Replay))
            		{
            		animationStack.push(captured);
            		animationStack.push(from);
            		}
        		  unDropObject(); 
        		  setState(XiangqiState.PLAY_STATE);
        		  if(attackingPosition(occupied[nextPlayer[whoseTurn]],kingPosition[whoseTurn]))
          			{ setState(XiangqiState.CHECK_STATE);
          			}
        		}
        	else 
        		{ pickedSource = from;
        		  pickedObject = removeChip(from);
        		  m.chip = pickedObject;
        			// if you pick up a gobblet and expose a row of 4, you lose immediately
          		}
        	}
            break;

        case MOVE_DROP: // drop on chip pool;
        	droppedDest = dropOnRack(pickedObject);
        	pickedObject = null;
            setNextStateAfterDrop();

            break;

        case MOVE_PICK:
            unDropObject();
            unPickObject();
            pickFromRack(m.source,m.from_row);
            m.chip = pickedObject;
            setNextStateAfterPick();
            break;


        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            setState(XiangqiState.PUZZLE_STATE);
            win[FIRST_PLAYER_INDEX]=win[SECOND_PLAYER_INDEX]=false;
            {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;

       case MOVE_RESIGN:
    	   setState(unresign==null?XiangqiState.RESIGN_STATE:unresign); 
    	   break;
       case MOVE_EDIT:
    		acceptPlacement();
            setState(XiangqiState.PUZZLE_STATE);
            win[0]=win[1]=false;

            break;

        case MOVE_OFFER_DRAW:
        	if(canOfferDraw())
        	{
        	XiangqiState bs = board_state;
         	if(bs==XiangqiState.OFFER_DRAW_STATE)
        	{	
         		setState(unresign);
        	}else
        	{
        	unresign = bs;
        	setState(XiangqiState.OFFER_DRAW_STATE);
        	}}
        	break;
        	
        case MOVE_ACCEPT_DRAW:
         	setState(board_state==XiangqiState.ACCEPT_DRAW_STATE?XiangqiState.QUERY_DRAW_STATE:XiangqiState.ACCEPT_DRAW_STATE);
        	break;
        	
        case MOVE_DECLINE_DRAW:
        	setState(board_state==XiangqiState.DECLINE_DRAW_STATE?XiangqiState.QUERY_DRAW_STATE:XiangqiState.DECLINE_DRAW_STATE);
        	break;
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;
        	
        default:
        	cantExecute(m);
        }
      	//checkOccupied();

        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case CONFIRM_STATE:
        case DRAW_STATE:
        case PLAY_STATE: 
        case ILLEGAL_MOVE_STATE:
		case GAMEOVER_STATE:
		case QUERY_DRAW_STATE:
		case DECLINE_DRAW_STATE:
		case ACCEPT_DRAW_STATE:
		case OFFER_DRAW_STATE:
		case CHECK_STATE:
		case RESIGN_STATE:
			return(false);
        case PUZZLE_STATE:
        	return((pickedObject==null)?true:(player==playerIndex(pickedObject)));
        }
    }
  

    public boolean LegalToHitBoard(XiangqiCell cell)
    {	
        switch (board_state)
        {
 		case PLAY_STATE:
 		case CHECK_STATE:
 			if(pickedObject!=null)
 			{
 			return(isSource(cell) || (getDests().get(cell)!=null));
 			}
 			else
 			{ return(getSources().get(cell)!=null);
 			}
 			
		case GAMEOVER_STATE:
		case QUERY_DRAW_STATE:
		case DECLINE_DRAW_STATE:
		case ACCEPT_DRAW_STATE:
		case OFFER_DRAW_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
		case ILLEGAL_MOVE_STATE:
		case DRAW_STATE:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case PUZZLE_STATE:
        	return(pickedObject==null?(cell.chipIndex>=0):true);
        }
    }
  public boolean canDropOn(XiangqiCell cell)
  {	
  	return((pickedObject!=null)				// something moving
  			&&(pickedSource.onBoard 
  					? (cell!=pickedSource)	// dropping on the board, must be to a different cell 
  					: (cell==pickedSource))	// dropping in the rack, must be to the same cell
  				);
  }
 
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public int RobotExecute(XiangqiMovespec m,boolean digest)
    {	int num = 0;
    	m.state = board_state;
        m.undoInfo = progressMove<<8 ; //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        if (Execute(m,replayMode.Replay))
        {
            if(digest)
            {	long dig = Digest();
            	num = repeatedPositions.addToRepeatedPositions(dig,m);
            } 
        	
        	if ((m.op==MOVE_NULL) || (m.op == MOVE_DONE))
            {
            }
            else if (DoneState())
            {
                doDone();
            }
            else
            {
            	throw G.Error("Robot move should be in a done state");
            }
        }
        return(num);
    }
    
    @SuppressWarnings("unused")
	private void checkOccupied()
    {	for(int j=0;j<2;j++)
    	{
    	CellStack cs = occupied[j];
    	int targetColor = getColorMap()[j];
    	for(int i=0;i<cs.size();i++)
    		{
    			XiangqiCell c = cs.elementAt(i);
    			XiangqiChip top = c.topChip();
    			G.Assert((top!=null) && (top.colorIndex==targetColor),"incorrectly occupied");
    		}
    	}
    }

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(XiangqiMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	//checkOccupied();
        switch (m.op)
        {
        default:
   	    	cantUnExecute(m);
        	break;
   	    case MOVE_ACCEPT_DRAW:
   	    case MOVE_DECLINE_DRAW:
        case MOVE_DONE:
        case MOVE_NULL:
            break;
           
        case MOVE_BOARD_BOARD:
        	// complete move, used for the robot
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
         		case GAMEOVER_STATE:
        		case CHECK_STATE:
        		case PLAY_STATE:
        			G.Assert((pickedObject==null)&&(droppedDest==null),"something is moving");	
        			XiangqiCell from = getCell( m.from_col,m.from_row);
        			XiangqiCell to = getCell(m.to_col, m.to_row);
        			XiangqiChip rem = m.captures;
        			addChip(from,removeChip(to));
        			if(rem!=null) 
        				{ addChip(to,rack[playerIndex(rem)][rem.pieceType].removeTop());
        				}
         			break;
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(m.state);
        progressMove = m.undoInfo>>8;
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
       	//checkOccupied();

 }
 private boolean moveOrCapture(XiangqiCell c,int dir,CommonMoveStack  v,int who)
 {	XiangqiCell d = c.exitTo(dir);
 	return(moveOrCapture(c,d,v,who));
 }
 //
 // return true if a move is possible. 
 // if checkstate is true, game winning moves are required
 //
 private boolean moveOrCapture(XiangqiCell c,XiangqiCell d,CommonMoveStack  v,int who)
 {	
 	if(d!=null)
 	{	XiangqiChip dtop = d.topChip();
 		// if checkstate, only moves that attack the king
 		// if not checkstate, then capturing moves, or moves to an empty cell and do not move into check
 		if((dtop==null) || (playerIndex(dtop)!=who))
 		{
 		XiangqiCell king = kingPosition[who];
 		if((king==null)||(king==c)) { king=d; }
 			// check for this move uncovering check on my king.  If kingposition is null, that means
 			// we're evaluating a move of the king, so d is kingposition
 		if(!attackingPosition(occupied[nextPlayer[who]],king,c,d))
		{	
 			if(v!=null) 
 			{ XiangqiMovespec newmove = new XiangqiMovespec(MOVE_BOARD_BOARD,c.col,c.row,d.col,d.row,who);
 			  v.addElement(newmove); 
 			}
 			return(true);
 		}}
 	}
 	return(false);
 }
 //
 // this is the canonical check for "check" in a static position
 //
 private boolean attackingPosition(CellStack from,XiangqiCell to)
 {	return(attackingPosition(from,to,null,null));
 }
 private boolean attackingPosition(CellStack from,XiangqiCell to,XiangqiCell empty,XiangqiCell filled)
 {	if(to!=null)
	{for(int i=0,lim=from.size(); i<lim; i++)
 	{
	 XiangqiCell c = from.elementAt(i);
	 if(attackingPosition(c,c.topChip(),to,empty,filled))
	 	{ return(true); }
 	}}
	return(false);
 }
 //
 // this is used only to check for check, and the position can only be in the opposing player's palace
 // if "empty" is not null, it's a space that has been vacated to fill "filled"
 //
 boolean attackingPosition(XiangqiCell from,XiangqiChip top,XiangqiCell to,XiangqiCell empty,XiangqiCell filled)
 {	 if(from==filled) 
 		{
	 	// this corresponds to a piece that's about to be captured. so it won't be threatening anything
	 	return(false); 
 		}
	 int who = playerIndex(top);
	 switch(top.pieceType)
	 {
	 default: throw G.Error("not expecting %s",top);
	 case XiangqiChip.GENERAL_INDEX:
	 	// general also makes "flying capture" of other generals if unobstructed.
		if(from.col==to.col)
	 	{	XiangqiCell d = from;
	 		int dir = (who==FIRST_PLAYER_INDEX) ? CELL_DOWN : CELL_UP;
	 		while(d!=null)
	 		{	d = d.exitTo(dir);
	 			if(d==to) { return(true); }
	 			if(d!=null)
	 			{
	 				if( (d==empty)
	 						?false
	 						: (d==filled) ? true : (d.topChip()!=null))
	 				{	return(false);
	 				}
	 			}
	 		}
	 	}
		 break;
	 case XiangqiChip.ELEPHANT_INDEX:	// elephants can never make the winning move
	 case XiangqiChip.GUARD_INDEX:	 	// guard's can never get to the opponent's king.
		 break;
	 case XiangqiChip.SOLDIER_INDEX:
		 // forward, or sideways. empty or filled cells are not relevant.
	 	if((Math.abs(from.col-to.col)<=1) 
			 && (Math.abs(from.row-to.row)<=1))
	 	{int dir = (who==FIRST_PLAYER_INDEX) ? CELL_DOWN : CELL_UP;
	 	 return((from.exitTo(dir)==to)
	 			 || (from.exitTo(dir-2)==to)
	 			 || (from.exitTo(dir+2)==to));
	 	}
		 break;

	 case XiangqiChip.HORSE_INDEX:
		 // one step orthogonal, one step diagonal, no obstructions
		 if((Math.abs(from.col-to.col)<=2) 
			 && (Math.abs(from.row-to.row)<=2))
		 {
		 for(int step=0,dir=CELL_LEFT; step<4; step++,dir+=2)
		 {
			 XiangqiCell step1 = from.exitTo(dir);
			 if((step1!=null) 
					 && (step1!=filled) 
					 && ((step1==empty) || (step1.topChip()==null)))
			 {	if((step1.exitTo(dir-1)==to)
				 	|| (step1.exitTo(dir+1)==to)) { return(true); }
			 }
	 		}
		 }
		 break;
	 case XiangqiChip.CANNON_INDEX:
		 // orthogonally, like a rook, but can only capture after leaping something
		if((from.col==to.col) || (from.row==to.row))
	 	{
			int dir = findDirection(from.col,from.row,to.col,to.row);
		    XiangqiCell d = from;
		 	boolean capturePhase = false;
		 	while((d!=null) && ((d = d.exitTo(dir))!=null))
		 	{	if(d==to) { return(capturePhase); }
		 		boolean occ = (d==filled) || ((d==empty)?false:(d.topChip()!=null));
		 		if(capturePhase)
		 		{	// capture opponent, no move
		 			if(occ)
		 			{	d = null;	// and end
		 			}
		 		}
		 		else if(occ)
		 		{ capturePhase = true; 
		 		}
		 	}
		 }
		 break;
	 case XiangqiChip.CHARIOT_INDEX:
		 // orthogonally, like a rook
		 if((from.col==to.col) || (from.row==to.row))
		 {
		 int dir = findDirection(from.col,from.row,to.col,to.row);
		 XiangqiCell d = from;
		 while((d!=null) && ((d = d.exitTo(dir))!=null))
		 	{
			if(d==to) { return(true); }
			boolean occ = (d==filled) || ((d==empty)?false:(d.topChip()!=null));
		 	if(occ) { d=null; }
		 	}
		 }
		 break;
	 }
 	return(false);
  }
 
 //
 // if check is true, only check for moves that attack the king
 // if empty and filled are non-null, they're the temporary move which must escape check
 //
 boolean getListOfMoves(CommonMoveStack  v,XiangqiCell c,XiangqiChip top)
 {	
 	if(top!=null)
 	{int who = playerIndex(top);
	 switch(top.pieceType)
	 {
	 default: throw G.Error("not expecting %s",top);
	 case XiangqiChip.GENERAL_INDEX:
		 // general moves orthogonally within the palace
	 	{	for(int step=0,dir=CELL_LEFT; step<4; step++,dir+=2)
	 		{	XiangqiCell d = c.exitTo(dir);
	 			if((d!=null) 
	 					&& d.isPalace
	 					// check for moving into check, while leaving the former position empty
	 					&& !attackingPosition(occupied[nextPlayer[who]],d,c,d))
	 			{
	 			if(moveOrCapture(c,d,v,who) && (v==null)) { return(true); }
	 			}
	 		}
	 	}
	 	// general also makes "flying capture" of other generals if unobstructed.
	 	XiangqiCell kp = kingPosition[nextPlayer[who]];
	 	if((kp!=null) && (c.col==kp.col))
	 	{	XiangqiCell d = c;
	 		int dir = (who==FIRST_PLAYER_INDEX) ? CELL_DOWN : CELL_UP;
	 		while(d!=null)
	 		{	d = d.exitTo(dir);
	 			if(d!=null)
	 			{	XiangqiChip dtop = d.topChip();
	 				if(dtop!=null)
	 				{	if(dtop.pieceType==XiangqiChip.GENERAL_INDEX)
	 					{	// reached the other general
	 					if(v==null) { return(true); }
 						v.addElement(new XiangqiMovespec(MOVE_BOARD_BOARD,c.col,c.row,d.col,d.row,who));
	 					}
	 				d = null;	// anything else is an obstruction
	 				}
	 			}
	 		}
	 	}
		 break;
	 case XiangqiChip.GUARD_INDEX:
		 // diagonal moves within the palace.
	 	{	for(int step=0,dir=CELL_UP_LEFT; step<4; step++,dir+=2)
 		{	XiangqiCell d = c.exitTo(dir);
 			if((d!=null) && d.isPalace)
 			{
 			if(moveOrCapture(c,d,v,who) && (v==null)) { return(true); }
 			}
  		}
	 	}
		 break;
	 case XiangqiChip.SOLDIER_INDEX:
		 // forward, or sideways if across the river
	 	{int dir = (who==FIRST_PLAYER_INDEX) ? CELL_DOWN : CELL_UP;
	 	 if(moveOrCapture(c,dir,v,who) && (v==null)) { return(true); }
	 	 if(c.acrossTheRiver(who)) 
	 	 	{ if(moveOrCapture(c,dir-2,v,who) && (v==null)) { return(true); } 
	 	 	  if(moveOrCapture(c,dir+2,v,who) && (v==null)) { return(true); }
	 	 	}
	 	}
		 break;
	 case XiangqiChip.ELEPHANT_INDEX:
		 // 2 steps diagonally, no obstruction, can't cross the river
		 {// elephants can never make the winning move
		 for(int step=0,dir=CELL_UP_LEFT; step<4; step++,dir+=2)
		 {	XiangqiCell step1 = c.exitTo(dir);
		 	if((step1!=null) && (step1.topChip()==null))
		 	{	XiangqiCell step2 = step1.exitTo(dir);
		 		if((step2!=null) && !step2.acrossTheRiver(who))
		 		{ if(moveOrCapture(c,step2,v,who) && (v==null)) { return(true); }
		 		}
		 	}
		 }}
		 break;
	 case XiangqiChip.HORSE_INDEX:
		 // one step orthogonal, one step diagonal, no obstructions
		 for(int step=0,dir=CELL_LEFT; step<4; step++,dir+=2)
		 {
			 XiangqiCell step1 = c.exitTo(dir);
			 if((step1!=null) && (step1.topChip()==null))
			 {	XiangqiCell c1 = step1.exitTo(dir-1);
			 	XiangqiCell c2 = step1.exitTo(dir+1);
			 	// avoid duplicate moves, they're expensive!
			 	if(moveOrCapture(c,c2,v,who) && (v==null)) { return(true); } 
			 	if(moveOrCapture(c,c1,v,who) && (v==null)) { return(true); } 
			 }
	 	}
		 break;
	 case XiangqiChip.CANNON_INDEX:
		 // orthogonally, like a rook, but can only capture after leaping something
	 	{
		 for(int step=0,dir=CELL_LEFT; step<4; step++,dir+=2)
		 {	XiangqiCell d = c;
		 	boolean capturePhase = false;
		 	while((d!=null) && ((d = d.exitTo(dir))!=null))
		 	{	XiangqiChip dtop = d.topChip();
		 		if(capturePhase)
		 		{	// capture opponent, no move
		 			if(dtop!=null)
		 			{	if(playerIndex(dtop)!=who) 
		 					{ if(moveOrCapture(c,d,v,who) && (v==null)) { return(true); } 
		 					}
		 				d = null;	// and end
		 			}
		 		}
		 		else if(dtop==null)
		 		{ // move only
		 		 if(moveOrCapture(c,d,v,who) && (v==null)) { return(true); }
		 		}
		 		else { capturePhase = true; }
		 	}
		 }}
		 break;
	 case XiangqiChip.CHARIOT_INDEX:
		 // orthogonally, like a rook
		 for(int step=0,dir=CELL_LEFT; step<4; step++,dir+=2)
		 {	XiangqiCell d = c;
		 	while((d!=null) && ((d = d.exitTo(dir))!=null))
		 	{	XiangqiChip dtop = d.topChip();
		 		if(moveOrCapture(c,d,v,who) && (v==null)) { return(true); }
		 		if(dtop!=null)  { d = null; }	// hit something
		 	}
		 }
		 break;
	 }
 	}
 	return(false);
  }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
 	getListOfMoves(all,whoseTurn);
 	return(all);
 }
 // if v is not null, collects the list of moves
 // who is the player to move (regardless of whose turn it is)
 // check is true if checking for game winning moves.
 // returns true if moves are possible.
 private boolean getListOfMoves(CommonMoveStack  v,int who)
 {	switch(board_state)
 	{
 	default: throw G.Error("Not expecting state %s",board_state);
 	case QUERY_DRAW_STATE:
 		v.addElement(new XiangqiMovespec(ACCEPTDRAW,who));
 		v.addElement(new XiangqiMovespec(DECLINEDRAW,who));
 		break;
 	case CHECK_STATE:
 	case PLAY_STATE:
 		{
 		CellStack mine = occupied[who];
 		for(int i=0,lim=mine.size(); i<lim;  i++)
	 		{
 			XiangqiCell c = mine.elementAt(i);
 			boolean some = getListOfMoves(v,c,c.topChip());
 			if((v==null)&&some) { return(true); }
	 		}
 		}
 		break;
 	}
 	return(false);
 }
 private boolean hasMoves(int who)
 {	return(getListOfMoves(null,who));
 }
 Hashtable<XiangqiCell,XiangqiCell> getDests()
 {	Hashtable<XiangqiCell,XiangqiCell> h = new Hashtable<XiangqiCell,XiangqiCell>();
 	if((pickedObject!=null) && (pickedSource!=null) && pickedSource.onBoard)
 	{
	CommonMoveStack  v = new CommonMoveStack();
 	getListOfMoves(v,pickedSource,pickedObject);
 	for(int i=0,lim=v.size(); i<lim; i++)
 	{	XiangqiMovespec m = (XiangqiMovespec)v.elementAt(i);
 		if(m.op==MOVE_BOARD_BOARD)
 		{	XiangqiCell c = getCell(m.to_col,m.to_row);
 			h.put(c,c);
 		}
 	}
 	}
 	return(h);
	 
 }
 Hashtable<XiangqiCell,XiangqiCell> getSources()
 {	Hashtable<XiangqiCell,XiangqiCell> h = new Hashtable<XiangqiCell,XiangqiCell>();
 	if(pickedSource==null)
 	{
 	CommonMoveStack  v = new CommonMoveStack();
 	getListOfMoves(v,whoseTurn);
 	for(int i=0,lim=v.size(); i<lim; i++)
 	{	XiangqiMovespec m = (XiangqiMovespec)v.elementAt(i);
 		if(m.op==MOVE_BOARD_BOARD)
 		{	XiangqiCell c = getCell(m.from_col,m.from_row);
 			h.put(c,c);
 		}
 	}
 	}
 	return(h);
	 
 }
public boolean canOfferDraw() {
	return (moveNumber-lastDrawMove>4);
}
}
