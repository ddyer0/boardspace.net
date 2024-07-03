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
package takojudo;

import online.game.*;
import static takojudo.TakojudoMovespec.*;

import lib.*;


/**
 * TakojudoBoard knows all about the game of Taco Judo.  
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
 */

class TakojudoBoard extends rectBoard<TakojudoCell> implements BoardProtocol,TakojudoConstants
{
	private TakojudoState unresign;
	private TakojudoState board_state;
	public TakojudoState getState() {return(board_state); }
	public void setState(TakojudoState st) 
	{ 	unresign = (st==TakojudoState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}	
	boolean REPETITION_IS_LOSS = false;			// effectively a compile-time switch
    public int boardColumns = DEFAULT_COLUMNS;	// size of the board
    public int boardRows = DEFAULT_ROWS;
    public void SetDrawState() 
    	{ setState(REPETITION_IS_LOSS ? TakojudoState.RESIGN_DRAW_STATE : TakojudoState.DRAW_STATE); 
    	}
    
    // where to find the lower-left corner of the heads.  null while the heads are being moved.
    public TakojudoCell headLocation[] = new TakojudoCell[2];
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public TakojudoChip pickedObject = null;
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    private int sweep_counter = 0;
    
    // this is a structure shared with the viewer, used by the robot to detect
    // repetitions and score them harshly if repetition is a loss
    private RepeatedPositions repeatedPositions = null;
    // the usual simple animation stack
    public CellStack animationStack = new CellStack();
	// factory method to creatge board cells
	public TakojudoCell newcell(char c,int r)
	{	return(new TakojudoCell(c,r));
	}
    public TakojudoBoard(String init,long key,RepeatedPositions rep,int map[]) // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = TACOJUDOGRIDSTYLE; //coordinates left and bottom
    	repeatedPositions = rep;
    	setColorMap(map, 2);
        doInit(init,key); // do the initialization 
        autoReverseY();		// reverse_y based on the color map
     }


	public void sameboard(BoardProtocol f) { sameboard((TakojudoBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(TakojudoBoard from_b)
    {
    	super.sameboard(from_b);	// calls sameCell for each cell
    	
    	G.Assert(AR.sameArrayContents(win,from_b.win),"win array contents match");

        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(sameCells(headLocation,from_b.headLocation),"head mismatch");
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject doesn't match");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");

    }

    /** 
     * Digest produces a 64 bit hash of the game state.  This is used in many different
     * ways to identify "same" board states.  Some are germane to the ordinary operation
     * of the game, others are for system record keeping use; so it is important that the
     * game Digest be consistent both within a game and between games over a long period
     * of time which have the same moves. 
     * (1) Digest is used by the default implementation of EditHistory to remove moves
     * that have returned the game to a previous state; ie when you undo a move or
     * hit the reset button.  
     * (2) Digest is used after EditHistory to verify that replaying the history results
     * in the same game as the user is looking at.  This catches errors in implementing
     * undo, reset, and EditHistory
	 * (3) Digest is used by standard robot search to verify that move/unmove 
	 * returns to the same board state, also that move/move/unmove/unmove etc.
	 * (4) Digests are also used as the game is played to look for draw by repetition.  The state
     * after most moves is recorded in a hashtable, and duplicates/triplicates are noted.
     * (5) games where repetition is forbidden (like xiangqi/arimaa) can also use this
     * information to detect forbidden loops.
	 * (6) Digest is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game, and a midpoint state of the game. Other site machinery
     * looks for duplicate digests.  
     * (7) digests are also used in live play to detect "parroting" by running two games
     * simultaneously and playing one against the other.
     */
   public long Digest()
    {
       Random r = new Random(64 * 1000); // init the random number generator
       long v = super.Digest(r);

		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
        return (v);
    }
   public TakojudoBoard cloneBoard() 
	{ TakojudoBoard copy = new TakojudoBoard(gametype,randomKey,repeatedPositions,getColorMap());
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((TakojudoBoard)b); }


    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(TakojudoBoard from_b)
    {
        super.copyFrom(from_b);
        
        repeatedPositions = from_b.repeatedPositions.copy();
        pickedObject = from_b.pickedObject;	
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(headLocation,from_b.headLocation);

        sameboard(from_b);
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {  	int specs[][] = {{'C',1,0},{'C',2,0},{'C',3,0},{'D',3,0},{'E',3,0},{'F',3,0},{'F',2,0},{'F',1,0},
    					 {'C',8,1},{'C',7,1},{'C',6,1},{'D',6,1},{'E',6,1},{'F',6,1},{'F',7,1},{'F',8,1}};
    	Random r = new Random(67246765);
		randomKey = key;	// not used, but for reference in this demo game
   
      	{
     	if(Tacojudo_INIT.equalsIgnoreCase(gtype)) 
     		{ boardColumns=DEFAULT_COLUMNS; 
     		boardRows = DEFAULT_ROWS;
     		}
     	else { throw G.Error(WrongInitError,gtype); }
     	gametype = gtype;
     	}
	    setState(TakojudoState.PUZZLE_STATE);
	    initBoard(boardColumns,boardRows); //this sets up the board and cross links
	    
	    whoseTurn = FIRST_PLAYER_INDEX;
		pickedSourceStack.clear();
		droppedDestStack.clear();
		pickedObject = null;
        allCells.setDigestChain(r);
        moveNumber = 1;
        addChip(getCell('D',1),TakojudoChip.getChip(1,TakojudoChip.HEAD_INDEX));
        addChip(getCell('D',7),TakojudoChip.getChip(0,TakojudoChip.HEAD_INDEX));
        for(int[]spec : specs)
        {	TakojudoChip ch = TakojudoChip.getChip(spec[2]^1,TakojudoChip.TENTACLE_INDEX);
        	TakojudoCell c = getCell((char)spec[0],spec[1]);
        	addChip(c,ch);
        }

        // note that firstPlayer is NOT initialized here
    }

 
    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Move not complete, can't change the current player");
        case PUZZLE_STATE:
            break;
        case CONFIRM_STATE:
        case DRAW_STATE:
        case RESIGN_DRAW_STATE:
        case OFFER_DRAW_STATE:
        case DECLINE_DRAW_STATE:
        case ACCEPT_DRAW_STATE:
        case RESIGN_STATE:
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
         case RESIGN_DRAW_STATE:
            return (true);

        default:
            return (false);
        }
    }


    private void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(TakojudoState.GAMEOVER_STATE);
    }
    
    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	if(board_state==TakojudoState.GAMEOVER_STATE) { return(win[player]); }
    	return(false);
    }
    int playerIndex(TakojudoChip ch) { return(getColorMap()[ch.colorIndex()]); }
    
    // estimate the value of the board position.
    public double ScoreForPlayer(int player,boolean print,boolean quant)
    {  	double finalv= quant
    					? countHeadMoves(headLocation[player],player)*1.0 
    					: headIsMobile(player)?1.0:0;
    	TakojudoCell head = headLocation[player];
    	TakojudoCell ohead = headLocation[nextPlayer[player]];
    	finalv -= G.distance(head.col,head.row,'D',4);
    	finalv += Math.sqrt(scoreMobileTenticles(player))*2.0;
    	double ocol = ohead.col+0.5;
    	double orow = ohead.row+0.5;
    	double dscore = 0.0;
    	for(TakojudoCell c = allCells; c!=null; c=c.next)
    	{
    		TakojudoChip ch = c.topChip();
    		if((ch!=null) && (playerIndex(ch)==player))
    		{
    			dscore += G.distance(c.col,c.row,ocol,orow);
    		}
    	}
    	finalv -= dscore * 0.5;
    	return(finalv);
    }


    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement()
    {	
        pickedObject = null;
        droppedDestStack.clear();
        pickedSourceStack.clear();
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    G.Assert(pickedObject==null, "nothing should be moving");
    if(droppedDestStack.size()>0)
    	{
    	TakojudoCell dr = droppedDestStack.pop();
    	pickedObject = removeChip(dr);
    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	TakojudoChip po = pickedObject;
    	if(po!=null)
    	{	
    		TakojudoCell ps = pickedSourceStack.pop();
    		addChip(ps,pickedObject);
    		pickedObject = null;
     	}
     }
    public boolean canDropOn(TakojudoCell c,TakojudoChip ch)
    {	if(c.topChip()==null)
    	{
    		if(ch.isHead())
    		{	
    			TakojudoCell c1 = c.exitTo(CELL_UP);
    			if((c1==null) || (c1.topChip()!=null)) { return(false); }
    			c1 = c.exitTo(CELL_RIGHT);
    			if((c1==null) || (c1.topChip()!=null)) { return(false); }
    			c1 = c.exitTo(CELL_UP_RIGHT);
    			if((c1==null) || (c1.topChip()!=null)) { return(false); }
    		}
    		return(true);
    	}
    	return(false);
    }
    private void addChip(TakojudoCell c,TakojudoChip ch)
    {	c.addChip(ch);
    	if(ch.isHead()) 
    	{	headLocation[playerIndex(ch)]=c;
    		TakojudoCell c1 = c.exitTo(CELL_UP);
    		c1.addChip(ch);
    		c1.dontDraw = true;
    		c1 = c.exitTo(CELL_RIGHT);
    		c1.addChip(ch);
    		c1.dontDraw = true;
    		c1 = c.exitTo(CELL_UP_RIGHT);
    		c1.addChip(ch);
    		c1.dontDraw = true;
    	}
    }
    private TakojudoChip removeChip(TakojudoCell c)
    {	TakojudoChip ch = c.removeTop();
    	if(ch.isHead()) 
    	{	headLocation[playerIndex(ch)]=null;
    		TakojudoCell c1 = c.exitTo(CELL_UP);
    		c1.removeChip(ch);
    		c1.dontDraw = false;
    		c1 = c.exitTo(CELL_RIGHT);
    		c1.removeChip(ch);
    		c1.dontDraw = false;
    		c1 = c.exitTo(CELL_UP_RIGHT);
    		c1.removeChip(ch);
    		c1.dontDraw = false;
    	}
    	return(ch);
    }
    // 
    // drop the floating object.
    //
    private void dropObject(TakojudoCell c)
    {   G.Assert(pickedObject!=null,"pickedObject should not be null"); 	    		
    	addChip(c,pickedObject);
    	pickedObject = null;
       	droppedDestStack.push(c);
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(TakojudoCell cell)
    {	return((droppedDestStack.size()>0) && (droppedDestStack.top()==cell));
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	TakojudoChip ch = pickedObject;
    	if(ch!=null)
    		{ return(ch.chipNumber());
    		}
        return (NothingMoving);
    }
   

    public TakojudoCell getCell(TakojudoCell c)
    {
    	return((c==null)?null:getCell(c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(TakojudoCell c)
    {	G.Assert(pickedObject==null,"pickedObject should be null");
    	pickedObject = removeChip(c);
    	
    	pickedSourceStack.push(c);
   }

    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case CONFIRM_STATE:
        case DRAW_STATE:
        case RESIGN_DRAW_STATE:
        	setNextStateAfterDone(); 
        	break;
        case PLAY_STATE:
			setState(TakojudoState.CONFIRM_STATE);
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
    public boolean isSource(TakojudoCell c)
    {	return(getSource()==c);
    }
    public TakojudoCell getSource()
    {
    	return((pickedSourceStack.size()>0) ? pickedSourceStack.top() : null);
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case GAMEOVER_STATE: 
    		break;
    	case RESIGN_DRAW_STATE:
    		setGameOver(true,false);
    		break;
        case DRAW_STATE:
        	setGameOver(false,false);
        	break;
        case OFFER_DRAW_STATE:
        	setState(TakojudoState.QUERY_DRAW_STATE);
        	break;
        case DECLINE_DRAW_STATE:
        	setState(TakojudoState.PLAY_STATE);
        	break;
        case ACCEPT_DRAW_STATE:
        	setGameOver(false,false);
        	break;
    	case CONFIRM_STATE:
    	case PUZZLE_STATE:
    	case PLAY_STATE:
    		setState(TakojudoState.PLAY_STATE);
    		break;
    	}

    }
   

    
    private void doDone()
    {	
        acceptPlacement();

        if (board_state==TakojudoState.RESIGN_STATE)
        {	setGameOver(false,true);
        }
        else
        {	boolean ok1 = hasLegalMoves(nextPlayer[whoseTurn]);
        	boolean ok0 = hasLegalMoves(whoseTurn);
        	if(ok0 && ok1) { setNextPlayer(); setNextStateAfterDone(); }
        	else if(!ok1)  { setGameOver(true,false); }
        	else { setGameOver(false,true); }
        }
    }

    public boolean Execute(commonMove mm,replayMode replay)
    {	TakojudoMovespec m = (TakojudoMovespec)mm;
        if(replay.animate) { animationStack.clear(); }
        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_OFFER_DRAW:
        	setState(board_state==TakojudoState.PLAY_STATE?TakojudoState.OFFER_DRAW_STATE:TakojudoState.PLAY_STATE);
        	break;
        case MOVE_DECLINE_DRAW:
        	setState(board_state==TakojudoState.QUERY_DRAW_STATE?TakojudoState.DECLINE_DRAW_STATE:TakojudoState.QUERY_DRAW_STATE);
        	break;
        case MOVE_ACCEPT_DRAW:
        	setState(board_state==TakojudoState.QUERY_DRAW_STATE?TakojudoState.ACCEPT_DRAW_STATE:TakojudoState.QUERY_DRAW_STATE);
        	break;
        case MOVE_DONE:

         	doDone();

            break;

        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
        			// needed by some damaged games
        			if(replay.animate)
        			{
        				throw G.Error("Not expecting robot in state %s",board_state);
        			}
        			setState(TakojudoState.PLAY_STATE);
        		//$FALL-THROUGH$
        		case PLAY_STATE:
        			G.Assert(pickedObject==null,"something is moving");
        			TakojudoCell src = getCell(m.from_col, m.from_row);
        			TakojudoCell dest = getCell(m.to_col,m.to_row); 
        			pickObject(src);
        			dropObject(dest); 
        			if(replay !=replayMode.Replay)
        			{
        				animationStack.push(src);
        				animationStack.push(dest);
        			}
 				    setNextStateAfterDrop();
        			break;
        	}
        	break;
        case MOVE_DROPB:
			{
			TakojudoCell c = getCell(m.to_col, m.to_row);
        	G.Assert(pickedObject!=null,"something is moving");
			
            if(isSource(c)) 
            	{ 
            	  unPickObject(); 

            	} 
            	else
            		{
            		dropObject(c);
            		setNextStateAfterDrop();
            		if(replay==replayMode.Single)
            			{
            			TakojudoCell src = getSource();
            			if(src!=null)
            			{
            			animationStack.push(src);
            			animationStack.push(c);
            			}}
            		}
			}
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	if(isDest(getCell(m.from_col,m.from_row)))
        		{ unDropObject(); 
        		  setState(TakojudoState.PLAY_STATE);
        		}
        	else 
        		{ pickObject(getCell(m.from_col, m.from_row));
        			// if you pick up a gobblet and expose a row of 4, you lose immediately
        		  switch(board_state)
        		  {	default: throw G.Error("Not expecting pickb in state %s",board_state);
        		    case GAMEOVER_STATE:
        		    	// needed for some damaged games
        		    	if(replay==replayMode.Live)
        		    	{
        		    		 throw G.Error("Not expecting pickb in state %s",board_state);
        		    	}
        		    	setState(TakojudoState.PLAY_STATE);
	        		  	//$FALL-THROUGH$
					case PLAY_STATE:
        		  		// if we pick a piece off the board, we might expose a win for the other player
        		  		// and otherwise, we are comitted to moving the piece
         		  		break;
        		  	case PUZZLE_STATE:
        		  		break;
        		  }
         		}
 
            break;


        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(TakojudoState.PUZZLE_STATE);
            {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;

        case MOVE_RESIGN:
            setState(unresign==null?TakojudoState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            // standardize "gameover" is not true
            setState(TakojudoState.PUZZLE_STATE);
 
            break;
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;

        default:
        	cantExecute(m);
        }


        return (true);
    }

    public TakojudoCell headLocation(TakojudoChip ch)
    {	return(headLocation[playerIndex(ch)]);
    }
    public boolean LegalToHitBoard(TakojudoCell cell)
    {	
        switch (board_state)
        {
 		case PLAY_STATE:
			return((pickedObject==null)
						? (isDest(cell) || hasLegalMoves(cell,whoseTurn))
						: canMoveFromTo(getSource(),cell,pickedObject));

		case GAMEOVER_STATE:
		case OFFER_DRAW_STATE:
		case QUERY_DRAW_STATE:
		case DECLINE_DRAW_STATE:
		case ACCEPT_DRAW_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
		case DRAW_STATE:
		case RESIGN_DRAW_STATE:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case PUZZLE_STATE:
        	return(pickedObject==null?(cell.chip!=null):canDropOn(cell,pickedObject));
        }
    }
  public boolean canDropOn(TakojudoCell cell)
  {		TakojudoCell top = (pickedObject!=null) ? pickedSourceStack.top() : null;
  		return((pickedObject!=null)				// something moving
  			&&(top.onBoard 			// on the main board
  					? (cell!=top)	// dropping on the board, must be to a different cell 
  					: (cell==top))	// dropping in the rack, must be to the same cell
  				);
  }
 
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(TakojudoMovespec m,boolean digest)
    {
        m.state = board_state; //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
       // G.print("e "+m);
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        if (Execute(m,replayMode.Replay))
        {	
        	// if we're scoring repetition as a loss, it's done ouside the normal flow
        	// this moves it back inside, so the robot can avoid the third repetition
        	// if there seems to be an alternative.
         	if(digest && REPETITION_IS_LOSS)
        	{	long dig = Digest();
        		int num = repeatedPositions.addToRepeatedPositions(dig,m);
	               	//
	            	// note that changing the threshold to 2 makes it avoid repetitions entirely,
	            	// but that will also break the "likely draw" logic which is triggered by
	            	// the second repetition
	            	//
	        		if(num>=3) 
        			{
        			// turn the repetition into a loss.   If we want the robot to
        			// offer and/or accept draws, we need to let it make the second
        			// repetitive move. 
	        		setState(TakojudoState.RESIGN_STATE);
        			}
        	} 
            if (m.op == MOVE_DONE)
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
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(TakojudoMovespec m,boolean digest)
    {
        //G.print("U "+m+" for "+whoseTurn);
       	if(digest && REPETITION_IS_LOSS)
       		{	repeatedPositions.removeFromRepeatedPositions(m);
       		}

        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;
   	    case MOVE_OFFER_DRAW:
   	    case MOVE_ACCEPT_DRAW:
   	    case MOVE_DECLINE_DRAW:
        case MOVE_DONE:
            break;

        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
        		case PLAY_STATE:
        			G.Assert(pickedObject==null,"something is moving");
        			pickObject(getCell(m.to_col, m.to_row));
       			    dropObject(getCell(m.from_col,m.from_row)); 
       			    acceptPlacement();
        			break;
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(m.state);
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
 // get the list of moves starting from a tentacle cell.  return true if there are some.
 // if all is not null, accumulate the list of moves.
 boolean getListOfMovesFromTentacle(CommonMoveStack  all, TakojudoCell c,int who)
 {	boolean some = false;
	 for(int direction = 0;direction<CELL_FULL_TURN; direction++)
	 {	TakojudoCell d = c;
	 	boolean blocked = false;
	 	while((!blocked && (d = d.exitTo(direction))!=null))
	 	{	
	 		if(d.topChip()==null)
	 		{	if(all==null) { return(true); }
	 			some = true;
	 			TakojudoMovespec newmove = new TakojudoMovespec(c.col,c.row,d.col,d.row,who);
	 			all.pushNew(newmove);
	 		}
	 		else { blocked = true; }
	 	}
	 }
	 return(some);
 }
 
 // true if c can see the head owned by who
 boolean canSeeHead(TakojudoCell c,int who)
 {	for(int direction=0; direction<CELL_FULL_TURN; direction++)
 	{	TakojudoCell d = c;
 		boolean blocked = false;
 		while(!blocked && (d=d.exitTo(direction))!=null)
 				{	TakojudoChip ch = d.topChip();
 					if(ch!=null)
 						{ if(ch.isHead() && (playerIndex(ch)==who)) 
 							{ return(true); }
 							else { blocked = true; }
 						}
 				}
 	}
 	return(false);
 }
 
 // true if c has legal moves, and therefore can be moved.
 boolean hasLegalMoves(TakojudoCell c,int who)
 {	TakojudoChip ch = c.topChip();
 	if((ch!=null) && (playerIndex(ch)==who)) 
 		{ if(ch.isHead()) 
 			{
 			 return(getListOfHeadMoves(null,headLocation[who],who));
 			}
 			else 
 			{ return(canSeeHead(c,who) && getListOfMovesFromTentacle(null,c,who)); }
 		}
 	return(false);
 }
 
// look for a movable piece from a head cell
// must have incremented sweep counter
 boolean getListOfMovesFromHead(CommonMoveStack  all, TakojudoCell c,int who)
 {	boolean some = false;
 	for(int direction = 0; direction<CELL_FULL_TURN; direction++)
 	{	TakojudoCell d = c;
 		boolean blocked = false;
 		while(!blocked && (d=d.exitTo(direction))!=null)
 		{	
 			TakojudoChip dtop = d.topChip();
 			if(dtop!=null)
 			{
 			if((playerIndex(dtop)==who) && !dtop.isHead() && (d.sweep_counter!=sweep_counter))
 			{
 			d.sweep_counter = sweep_counter;		// avoid scanning the same tentacle twice
 			some |= getListOfMovesFromTentacle(all,d,who);
  			}
 			blocked = true; 
 			}
 		}
 	}
	 return(some);
 }
 
//look score for a movable piece from a head cell, and also for
// tentacles along the same line, which could move if previous tentacles
// were moved out of the way
 double scoreTentaclesFromHead(TakojudoCell c,int who)
 {	double some = 0;
 	for(int direction = 0; direction<CELL_FULL_TURN; direction++)
 	{	TakojudoCell d = c;
 		double val = 1.0;
 		boolean blocked = false;
 		int step = 0;
 		while(!blocked && (d=d.exitTo(direction))!=null)
 		{	TakojudoChip dtop = d.topChip();
  			if(dtop!=null)
 			{
  			if(dtop.isHead() || (sweep_counter==d.sweep_counter))
 			if(playerIndex(dtop)==who)
 			{
 			d.sweep_counter = sweep_counter;
 			// note we continue so the next tentacle in the line of sight will still score
 			if(step>0 || getListOfMovesFromTentacle(null,d,who))
 				{
 				some += val;
 				val = val*0.5;
 				}
 				else { // tentacle can't move
 						blocked = true; 
 					 }
 				}
  			}
 			else 
 			{	
 			blocked = true; 
 			}
  			step++;
 		}
 	}
	 return(some);
 }
 
//count the tentacles that are mobile from head space c
int countTentaclesFromHead(TakojudoCell c,int who)
{	int some = 0;
	for(int direction = 0; direction<CELL_FULL_TURN; direction++)
	{	TakojudoCell d = c;
		boolean blocked = false;
		int step = 0;
		while(!blocked && (d=d.exitTo(direction))!=null)
		{	TakojudoChip dtop = d.topChip();
			if(dtop!=null)
			{
			boolean head = dtop.isHead();
			if(head || (sweep_counter==d.sweep_counter)) { blocked = true; }
			else if((playerIndex(dtop)==who))
			{
			d.sweep_counter = sweep_counter;
			// if this tentacle can actually move
			if((step>0) || getListOfMovesFromTentacle(null,d,who)) {	some++; }
			}
			blocked = true;
			}
			step++;
		}
	}
	 return(some);
}
 //
 // this is used for scoring tentacles that can move.
 // or that are in a line where they could move is another
 // friendly tentacle moved out of the way.
 private double scoreMobileTenticles(int who)
 {	TakojudoCell h0 = headLocation[who];
 	sweep_counter++;
	double total = scoreTentaclesFromHead(h0,who) 
		+ scoreTentaclesFromHead(h0.exitTo(CELL_RIGHT),who)
		+ scoreTentaclesFromHead(h0.exitTo(CELL_UP),who)
		+ scoreTentaclesFromHead(h0.exitTo(CELL_UP_RIGHT),who);
	return(total);
 }
 private int countMobileTenticles(int who)
 {	TakojudoCell h0 = headLocation[who];
 	sweep_counter++;
 	if(h0==null) { h0 = getSource(); }
	int total = countTentaclesFromHead(h0,who) 
		+ countTentaclesFromHead(h0.exitTo(CELL_RIGHT),who)
		+ countTentaclesFromHead(h0.exitTo(CELL_UP),who)
		+ countTentaclesFromHead(h0.exitTo(CELL_UP_RIGHT),who);
	return(total);
 }
 // this is a summary string of the current pieces, passed to the lobby and also displayed with 
 // the head.
 public String pieceSummary(int who)
 {
	 String head = ((headLocation[who]==null) || headIsMobile(who)) ? "H+" : "";
	 int me = ((pickedObject!=null) && !pickedObject.isHead() && (playerIndex(pickedObject)==who)) ? 1 : 0;
	 int mobile = countMobileTenticles(who);
	 return(head+(me+mobile));
 }
 // true if from-to is a legal move
 boolean canMoveFromTo(TakojudoCell from,TakojudoCell to,TakojudoChip ch)
 {
	 int pl = playerIndex(ch);
	 CommonMoveStack moves = new CommonMoveStack();
	 
	 if(ch.isHead())
	 {
	 getListOfHeadMoves(moves,from,pl);
	 }
	 else
	 {
		 getListOfMovesFromTentacle(moves,from,pl);
	 }
	 while(moves.size()>0)
	 {
		 TakojudoMovespec m = (TakojudoMovespec)moves.pop();
		 if((m.op==MOVE_BOARD_BOARD)&&(m.to_col==to.col)&&(m.to_row==to.row)) { return(true); }
	 }
	 return(false);
 }
 // get list of moves by thhe head at h0
 boolean getListOfHeadMoves(CommonMoveStack  all,TakojudoCell h0,int who)
 {
	 if(h0!=null)
	 {
		 TakojudoCell h1 = h0.exitTo(CELL_RIGHT);
		 TakojudoCell h2 = h0.exitTo(CELL_UP);
		 TakojudoCell h3 = h0.exitTo(CELL_UP_RIGHT);
		 return(getListOfHeadMoves(all,h0,h1,h2,h3,who));
	 }
	 return(false);
	 
 }
 // get list of moves by thhe head at h0
 int countHeadMoves(TakojudoCell h0,int who)
 {
	 if(h0!=null)
	 {
		 TakojudoCell h1 = h0.exitTo(CELL_RIGHT);
		 TakojudoCell h2 = h0.exitTo(CELL_UP);
		 TakojudoCell h3 = h0.exitTo(CELL_UP_RIGHT);
		 return(countHeadMoves(h0,h1,h2,h3,who));
	 }
	 return(0); 
 }
 // get list of head moved, considering all four squares occupied by the
 // head.  The move is legal only if all four head squares are individually clear.
 //
 boolean getListOfHeadMoves(CommonMoveStack  all,
		 	TakojudoCell head,TakojudoCell head_right,
		 	TakojudoCell head_up,TakojudoCell head_diagonal,int who)
 {	boolean some = false;
 	for(int direction=0; direction<CELL_FULL_TURN; direction++)
 		{
 		TakojudoCell h0 = head;
 		TakojudoCell h1 = head_right;
 		TakojudoCell h2 = head_up;
 		TakojudoCell h3 = head_diagonal;
 		boolean isOk = true;
 		while( isOk  
 				&& ((h0 = h0.exitTo(direction))!=null)
 				&& ((h1 = h1.exitTo(direction))!=null)
 				&& ((h2 = h2.exitTo(direction))!=null)
 				&& ((h3 = h3.exitTo(direction))!=null))
 		{	isOk = false;
 			TakojudoChip ch0 = h0.topChip();
 			if(ch0==null || (ch0.isHead() && (playerIndex(ch0)==who)))
 			{
 			TakojudoChip ch1 = h1.topChip();
 			if(ch1==null || (ch1.isHead() && (playerIndex(ch1)==who)))
 			{
 			TakojudoChip ch2 = h2.topChip();
 			if(ch2==null || (ch2.isHead() && (playerIndex(ch2)==who)))
 			{
 			TakojudoChip ch3 = h3.topChip();
 			if(ch3==null || (ch3.isHead() && (playerIndex(ch3)==who)))
 			{
 			if(all==null) { return(true); }
 			all.push(new TakojudoMovespec(head.col,head.row,h0.col,h0.row,who));
 			isOk = true;
 			}
 			}}}
 			
 		}}
	 return(some);
 }
 int countHeadMoves(TakojudoCell head,TakojudoCell head_right,
		 	TakojudoCell head_up,TakojudoCell head_diagonal,int who)
{	int some = 0;
	for(int direction=0; direction<CELL_FULL_TURN; direction++)
		{
		TakojudoCell h0 = head;
		TakojudoCell h1 = head_right;
		TakojudoCell h2 = head_up;
		TakojudoCell h3 = head_diagonal;
		boolean isOk = true;
		while( isOk  
				&& ((h0 = h0.exitTo(direction))!=null)
				&& ((h1 = h1.exitTo(direction))!=null)
				&& ((h2 = h2.exitTo(direction))!=null)
				&& ((h3 = h3.exitTo(direction))!=null))
		{	isOk = false;
			TakojudoChip ch0 = h0.topChip();
			if(ch0==null || (ch0.isHead() && (playerIndex(ch0)==who)))
			{
			TakojudoChip ch1 = h1.topChip();
			if(ch1==null || (ch1.isHead() && (playerIndex(ch1)==who)))
			{
			TakojudoChip ch2 = h2.topChip();
			if(ch2==null || (ch2.isHead() && (playerIndex(ch2)==who)))
			{
			TakojudoChip ch3 = h3.topChip();
			if(ch3==null || (ch3.isHead() && (playerIndex(ch3)==who)))
			{
			some++;
			isOk = true;
			}
			}}}
			
		}}
	 return(some);
}
 // true of the head can move
 public boolean headIsMobile(int who)
 {
	 return(getListOfHeadMoves(null,headLocation[who],who));
 }
 boolean getListOfMoves(CommonMoveStack  all, int who,boolean includeHeadMoves)
 {	TakojudoCell head  = headLocation[who];
 	if(head!=null)
 	{	sweep_counter++;
 		boolean headMoves = getListOfMovesFromHead(all,head,who);
 		if(headMoves && (all==null)) { return(true); }
 		TakojudoCell head_right = head.exitTo(CELL_RIGHT);
 		headMoves |= getListOfMovesFromHead(all,head_right,who);
 		if(headMoves && (all==null)) { return(true); }
 		TakojudoCell head_up = head.exitTo(CELL_UP);
 		headMoves |= getListOfMovesFromHead(all,head_up,who);
 		if(headMoves && (all==null)) { return(true); }
 		TakojudoCell head_diagonal = head.exitTo(CELL_UP_RIGHT);
 		headMoves |= getListOfMovesFromHead(all,head_diagonal,who);
 		if(headMoves && (all==null)) { return(true); }
 		if(includeHeadMoves) { headMoves |=getListOfHeadMoves(all,head,head_right,head_up,head_diagonal,who); }
 		return(headMoves);
 	}
 	return(false);
 }
 boolean hasLegalMoves(int who)
 {	return(getListOfMoves(null,who,true));
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
 	switch(board_state)
 	{
 	case QUERY_DRAW_STATE:
 		all.push(new TakojudoMovespec(MOVE_ACCEPT_DRAW,whoseTurn));
 		all.push(new TakojudoMovespec(MOVE_DECLINE_DRAW,whoseTurn));
 		break;
 	case PLAY_STATE:
 		getListOfMoves(all,whoseTurn,true);
 		break;
 	default: throw G.Error("Not expecting state %s",board_state);
 	}
  	return(all);
 }
 
}
