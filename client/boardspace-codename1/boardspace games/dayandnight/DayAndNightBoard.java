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
package dayandnight;


import static dayandnight.DayAndNightmovespec.*;

import java.util.*;
import lib.*;
import lib.Random;
import online.game.*;
import online.search.RobotProtocol;

/**
 * DayAndNightBoard knows all about the game of DayAndNight
 * It gets a lot of logistic support from 
 * common.hexBoard, which knows about the coordinate system.  
 * 
 * This class doesn't do any graphics or know about anything graphical, 
 * in the graphics.
 * 
 *  The principle interface with the game viewer is the "Execute" method
 *  which processes moves. 
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

class DayAndNightBoard 
	extends rectBoard<DayAndNightCell>	// for a square grid board, this could be rectBoard or squareBoard 
	implements BoardProtocol,DayAndNightConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
    static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	DayAndNightVariation variation = DayAndNightVariation.dayandnight;
	private DayAndNightState board_state = DayAndNightState.Puzzle;	
	private DayAndNightState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	public DayAndNightState getState() { return(board_state); }
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(DayAndNightState st) 
	{ 	unresign = (st==DayAndNightState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

    private DayAndNightId playerColor[]={DayAndNightId.Black,DayAndNightId.White};    
    private DayAndNightChip playerChip[]={DayAndNightChip.Black,DayAndNightChip.White};
    private DayAndNightCell playerCell[]=new DayAndNightCell[2];
    public DayAndNightChip lastDropped = null;
    
    public DayAndNightCell getHomeCell(DayAndNightChip ch)
    {
    	for(int i=0;i<playerColor.length;i++)
    	{
    		if(playerChip[i]==ch) { return(playerCell[i]); }
    	}
    	return(null);
    }
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public DayAndNightChip getPlayerChip(int p) { return(playerChip[p]); }
	public DayAndNightId getPlayerColor(int p) { return(playerColor[p]); }
	public DayAndNightCell getPlayerCell(int p) { return(playerCell[p]); }
	public DayAndNightChip getCurrentPlayerChip() { return(playerChip[whoseTurn]); }

// this is required even if it is meaningless for this game, but possibly important
// in other games.  When a draw by repetition is detected, this function is called.
// the game should have a "draw pending" state and enter it now, pending confirmation
// by the user clicking on done.   If this mechanism is triggered unexpectedly, it
// is probably because the move editing in "editHistory" is not removing last-move
// dithering by the user, or the "Digest()" method is not returning unique results
// other parts of this mechanism: the Viewer ought to have a "repRect" and call
// DrawRepRect to warn the user that repetitions have been seen.
	public void SetDrawState() {throw G.Error("not expected"); };	
	CellStack animationStack = new CellStack();
    private int chips_on_board = 0;			// number of chips currently on the board

    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public DayAndNightChip pickedObject = null;
    public DayAndNightChip lastPicked = null;
    private DayAndNightCell blackChipPool = null;	// dummy source for the chip pools
    private DayAndNightCell whiteChipPool = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    
    // move bounds and history, for robots
    private int bL = 0;
    private int bT = 0;
    private int bR = 0;
    private int bB = 0;
    private boolean robotBoard = false;
    private int robotStrategy = -1;
    
    
    // save strings to be shown in the game log
    StringStack gameEvents = new StringStack();
    InternationalStrings s = G.getTranslations();
    
 	void logGameEvent(String str,String... args)
 	{	//if(!robotBoard)
 		{String trans = s.get(str,args);
 		 gameEvents.push(trans);
 		}
 	}

    private CellStack darkCells=new CellStack();
    private CellStack occupiedCells = new CellStack();
    
    private DayAndNightState resetState = DayAndNightState.Puzzle; 
    public DrawableImage<?> lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public DayAndNightCell newcell(char c,int r)
	{	return(new DayAndNightCell(DayAndNightId.BoardLocation,c,r));
	}
	
	// constructor 
    public DayAndNightBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = GRIDSTYLE;
        setColorMap(map, players);
        
		Random r = new Random(734687);
		// do this once at construction
	    blackChipPool = new DayAndNightCell(r,DayAndNightId.Black);
	    blackChipPool.addChip(DayAndNightChip.Black);
	    whiteChipPool = new DayAndNightCell(r,DayAndNightId.White);
	    whiteChipPool.addChip(DayAndNightChip.White);

        doInit(init,key,players,rev); // do the initialization 
        autoReverseY();		// reverse_y based on the color map
    }
    
    public String gameType() { return(G.concat(gametype," ",players_in_game," ",randomKey," ",revision)); }
    

    public void doInit(String gtype,long key)
    {
    	StringTokenizer tok = new StringTokenizer(gtype);
    	String typ = tok.nextToken();
    	int np = tok.hasMoreTokens() ? G.IntToken(tok) : players_in_game;
    	long ran = tok.hasMoreTokens() ? G.IntToken(tok) : key;
    	int rev = tok.hasMoreTokens() ? G.IntToken(tok) : revision;
    	doInit(typ,ran,np,rev);
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int players,int rev)
    {	randomKey = key;
    	adjustRevision(rev);
    	players_in_game = players;
    	win = new boolean[players];
 		setState(DayAndNightState.Puzzle);
		variation = DayAndNightVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		gametype = gtype;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case dayandnight_19:
		case dayandnight_15:
		case dayandnight:
			// using reInitBoard avoids thrashing the creation of cells 
			// when reviewing games.
			reInitBoard(variation.nrows,variation.ncols);
			// or initBoard(variation.firstInCol,variation.ZinCol,null);
			// Random r = new Random(734687);	// this random is used to assign hash values to cells, common to all games of this type.
			// allCells.setDigestChain(r);		// set the randomv for all cells on the board
		}

 		
	    playerCell[FIRST_PLAYER_INDEX] = blackChipPool; 
	    playerCell[SECOND_PLAYER_INDEX] = whiteChipPool; 
	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    chips_on_board = 0;
	    droppedDestStack.clear();
	    pickedSourceStack.clear();
	    stateStack.clear();
	    
	    pickedObject = null;
	    resetState = null;
	    lastDroppedObject = null;
	    int map[]=getColorMap();
		playerColor[map[0]]=DayAndNightId.Black;
		playerColor[map[1]]=DayAndNightId.White;
		playerChip[map[0]]=DayAndNightChip.Black;
		playerChip[map[1]]=DayAndNightChip.White;
	    // set the initial contents of the board to all empty cells
		darkCells.clear();
		occupiedCells.clear();
		bL = bR = (ncols+1)/2;
		bT = bB = (nrows+1)/2-1;
		robotBoard = false;
		
		for(DayAndNightCell c = allCells; c!=null; c=c.next) 
			{ c.reInit(); 
			  if(c.dark) { darkCells.push(c); }
			}
        animationStack.clear();
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }

     /** create a copy of this board */
    public DayAndNightBoard cloneBoard() 
	{ DayAndNightBoard dup = new DayAndNightBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((DayAndNightBoard)b); }
    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(DayAndNightBoard from_b)
    {
        super.copyFrom(from_b);
        robotStrategy = from_b.robotStrategy;
        chips_on_board = from_b.chips_on_board;
        robotState.copyFrom(from_b.robotState);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        getCell(occupiedCells,from_b.occupiedCells);
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        copyFrom(whiteChipPool,from_b.whiteChipPool);		// this will have the side effect of copying the location
        copyFrom(blackChipPool,from_b.blackChipPool);		// from display copy boards to the main board
        getCell(playerCell,from_b.playerCell);
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        lastPicked = null;
        robotBoard = from_b.robotBoard;
        bL = from_b.bL;
        bT = from_b.bT;
        bR = from_b.bR;
        bB = from_b.bB;

        AR.copy(playerColor,from_b.playerColor);
        AR.copy(playerChip,from_b.playerChip);
 
        sameboard(from_b); 
    }

    public void sameboard(BoardProtocol b) { sameboard((DayAndNightBoard)b); }
    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(DayAndNightBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(robotStrategy==from_b.robotStrategy,"robotStrategy mismatch");
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor mismatch");
        G.Assert(AR.sameArrayContents(playerChip,from_b.playerChip),"playerChip mismatch");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(chips_on_board == from_b.chips_on_board,"chips_on_board mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedsourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(sameCells(playerCell,from_b.playerCell),"player cell mismatch");
        // contents may not be exact,
        G.Assert(occupiedCells.size()==from_b.occupiedCells.size(),"occupied cels mismatch");
        
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");

    }

    /** 
     * Digest produces a 64 bit hash of the game state.  This is used in many different
     * ways to identify "same" board states.  Some are relevant to the ordinary operation
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
        // the basic digestion technique is to xor a bunch of random numbers. 
    	// many object have an associated unique random number, including "chip" and "cell"
    	// derivatives.  If the same object is digested more than once (ie; once as a chip
    	// in play, and once as the chip currently "picked up", then it must be given a
    	// different identity for the second use.
        //
        Random r = new Random(64 * 1000); // init the random number generator
        long v = super.Digest(r);
		// many games will want to digest pickedSource too
		// v ^= cell.Digest(r,pickedSource);
        //System.out.println("D1 "+v);
		v ^= chip.Digest(r,playerChip[0]);	// this accounts for the "swap" button
        //System.out.println("D2 "+v);
		v ^= chip.Digest(r,pickedObject);
        //System.out.println("D3 "+v);
		v ^= Digest(r,pickedSourceStack);
        //System.out.println("D4 "+v);
		v ^= Digest(r,droppedDestStack);
        //System.out.println("D5 "+v);
		// contents may not be exact
		v ^= Digest(r,occupiedCells.size());
        //System.out.println("D6 "+v);
		v ^= Digest(r,revision);
        //System.out.println("D7 "+v);
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
        //System.out.println("D8 "+v);
        return (v);
    }



    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer(replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Move not complete, can't change the current player in state ",board_state);
        case Puzzle:
            break;
        case Play:
        	// some damaged games have 2 dones in a row
        	if(replay==replayMode.Live) { throw G.Error("Move not complete, can't change the current player in state ",board_state); }
			//$FALL-THROUGH$
        case Confirm:
        case Resign:
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
    {	return(board_state.doneState());
    }
    // this is the default, so we don't need it explicitly here.
    // but games with complex "rearrange" states might want to be
    // more selective.  This determines if the current board digest is added
    // to the repetition detection machinery.
    public boolean DigestState()
    {	
    	return(board_state.digestState());
    }



    public boolean gameOverNow() { return(board_state.GameOver()); }
    public boolean winForPlayerNow(int pl)
    {	DayAndNightChip ch = playerChip[pl];
    	for(DayAndNightCell c = allCells; c!=null; c=c.next)
    	{
    	if((c.topChip()==ch) && winningLine(c,ch)) { return(true); }
    	}
    	return(false);
    }


    // set the contents of a cell, and maintain the books
    public DayAndNightChip SetBoard(DayAndNightCell c,DayAndNightChip ch)
    {	DayAndNightChip old = c.topChip();
    	if(c.onBoard)
    	{
    	if(old!=null) 
    		{ chips_on_board--; 
    		  occupiedCells.remove(c,false); 
    	    }
     	if(ch!=null) 
     		{ chips_on_board++;
     		  occupiedCells.push(c); 
     		  if(!robotBoard)
     		  {
     			  int x = c.col-'A';
     			  int y = c.row-1;
     			  // maintain the bounding box.  This is maintained onluy
     			  // for real moves (not robot search moves) going forward.
     			  bL = Math.max(0, Math.min(x-2,bL));
     			  bT = Math.max(0, Math.min(y-2,bT));
     			  bR = Math.min(ncols-1,Math.max(bR, x+2));
     			  bB = Math.min(nrows-1,Math.max(bB, y+2));
     		  }
     		}
    	}
       	if(old!=null) { c.removeTop();}
       	if(ch!=null) { c.addChip(ch); }
    	return(old);
    }
    //
    // accept the current placements as permanent
    //
    public void acceptPlacement()
    {	
        droppedDestStack.clear();
        pickedSourceStack.clear();
        stateStack.clear();
        pickedObject = null;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private DayAndNightCell unDropObject()
    {	DayAndNightCell rv = droppedDestStack.pop();
    	setState(stateStack.pop());
    	pickedObject = SetBoard(rv,null); 	// SetBoard does ancillary bookkeeping
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	DayAndNightCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	SetBoard(rv,pickedObject);
    	pickedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(DayAndNightCell c)
    {
       droppedDestStack.push(c);
       stateStack.push(board_state);
       
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting dest " + c.rackLocation);
        case Black:
        case White:		// back in the pool, we don't really care where
        	pickedObject = null;
            break;
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        case EmptyBoard:
           	SetBoard(c,pickedObject);
            pickedObject = null;
            break;
        }
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(DayAndNightCell c)
    {	return(droppedDestStack.top()==c);
    }
    public DayAndNightCell getDest()
    {	return(droppedDestStack.top());
    }
 
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { DayAndNightChip ch = pickedObject;
      if(ch!=null)
    	{	return(ch.chipNumber()); 
    	}
      	return (NothingMoving);
    }
   /**
     * get the cell represented by a source code, and col,row
     * @param source
     * @param col
     * @param row
     * @return
     */
    private DayAndNightCell getCell(DayAndNightId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source " + source);
        case BoardLocation:
        	return(getCell(col,row));
        case Black:
        	return(blackChipPool);
        case White:
        	return(whiteChipPool);
        } 	
    }
    /**
     * this is called when copying boards, to get the cell on the new (ie current) board
     * that corresponds to a cell on the old board.
     */
    public DayAndNightCell getCell(DayAndNightCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(DayAndNightCell c)
    {	pickedSourceStack.push(c);
    	stateStack.push(board_state);
        switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting rackLocation " + c.rackLocation);
        case BoardLocation:
        	{
            lastPicked = pickedObject = c.topChip();
            G.Assert(pickedObject!=null,"must be something");
         	lastDroppedObject = null;
			SetBoard(c,null);
        	}
            break;

        case Black:
        case White:
        	lastPicked = pickedObject = c.topChip();
        }
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(DayAndNightCell c)
    {	return(c==pickedSourceStack.top());
    }
    public DayAndNightCell getSource()
    {	return(pickedSourceStack.top());
    }
 
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state " + board_state);
        case Confirm:
        	setNextStateAfterDone(replay);
         	break;
        case Play:
        case DropDark:
        case DropLight:
			setState(DayAndNightState.Confirm);
			break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    private void setNextStateAfterDone(replayMode replay)
    {	
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state "+board_state);
    	case Gameover: 
    		break;
    	
    	case Confirm:
		case Puzzle:
    	case Play:
    		setState(DayAndNightState.Play);
    		
    		break;
    	}
       	resetState = board_state;
    }
    private void doDone(replayMode replay)
    {	DayAndNightCell dest = getDest();
        acceptPlacement();

        if (board_state==DayAndNightState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(DayAndNightState.Gameover);
        }
        else if((dest!=null) && winningLine(dest,dest.topChip()))
    			{ win[whoseTurn] = true; 
    			  setState(DayAndNightState.Gameover);
    			}
        	else {setNextPlayer(replay);
        		setNextStateAfterDone(replay);
        	}

    }

    public boolean Execute(commonMove mm,replayMode replay)
    {	DayAndNightmovespec m = (DayAndNightmovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }

        //G.print("E "+m+" for "+whoseTurn+" "+state);
        switch (m.op)
        {
        case MOVE_PASS:
        	setState(DayAndNightState.Confirm);
        	break;
        case MOVE_DONE:

         	doDone(replay);

            break;

        case MOVE_FROM_TO:
        	{
        	DayAndNightCell src = getCell(m.from_col,m.from_row);
        	DayAndNightCell dest = getCell(m.to_col,m.to_row);
        	pickObject(src);
        	m.chip = pickedObject;
        	dropObject(dest);
        	if(replay!=replayMode.Replay)
        	{ animationStack.push(getSource());
        	  animationStack.push(dest); 
        	}
        	setNextStateAfterDrop(replay);
        	}
        	break;
        case MOVE_DROPB:
        	{
			DayAndNightChip po = pickedObject;
			DayAndNightCell dest =  getCell(DayAndNightId.BoardLocation,m.to_col,m.to_row);
			
			if(isSource(dest)) 
				{ 
		        lastDropped = pickedObject.getAltDisplayChip(dest);
				unPickObject(); 
				}
				else 
				{
				if(po==null)
				{	DayAndNightCell src = (board_state==DayAndNightState.Puzzle) ? getHomeCell(lastPicked) : null;
					if(src==null) { src= playerCell[whoseTurn]; }
					pickObject(src);
					m.chip = pickedObject;
				}
		        lastDropped = pickedObject.getAltDisplayChip(dest);
	            dropObject(dest);
	            
	            /**
	             * if the user clicked on a board space without picking anything up,
	             * animate a stone moving in from the pool.  For Hex, the "picks" are
	             * removed from the game record, so there are never picked stones in
	             * single step replays.
	             */
	            if(replay!=replayMode.Replay && (po==null))
	            	{ animationStack.push(getSource());
	            	  animationStack.push(dest); 
	            	}
	            setNextStateAfterDrop(replay);
				}
        	}
             break;

        case MOVE_PICK:
 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
 			DayAndNightCell src = getCell(m.source,m.to_col,m.to_row);
 			if(isDest(src)) { unDropObject(); }
 			else
 			{
        	// be a temporary p
        	pickObject(src);
        	m.chip = pickedObject;
        	lastPicked = pickedObject;
        	switch(board_state)
        	{
        	case Puzzle:
         		break;
        	case Confirm:
        	case Play:
        		setState(src.onBoard ?  DayAndNightState.DropLight : DayAndNightState.DropDark);
        		break;
        	default: ;
        	}}}
            break;

        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
            DayAndNightCell dest = getCell(m.source,m.to_col,m.to_row);
            if(isSource(dest)) { unPickObject(); }
            else 
            	{
		        if(replay==replayMode.Live)
	        	{ lastDroppedObject = pickedObject.getAltDisplayChip(dest);
	        	  //G.print("last ",lastDroppedObject); 
	        	}      	
            	dropObject(dest); 
            
            	}
        	}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            int nextp = nextPlayer[whoseTurn];
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(DayAndNightState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ setState(DayAndNightState.Gameover); 
               	}
            else {  setNextStateAfterDone(replay); }

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?DayAndNightState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(DayAndNightState.Puzzle);
 
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(DayAndNightState.Gameover);
    	   break;

        default:
        	cantExecute(m);
        }
        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }

    // legal to hit the chip storage area
    public boolean legalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting Legal Hit state " + board_state);
        case Play:
        case DropDark:
        	// for pushfight, you can pick up a stone in the storage area
        	// but it's really optional
        	return(player==whoseTurn);
        case Confirm:
		case Resign:
		case Gameover:
		case DropLight:
			return(false);
        case Puzzle:
            return ((pickedObject!=null)?(pickedObject==playerChip[player]):true);
        }
    }

    public boolean legalToHitBoard(DayAndNightCell c,Hashtable<DayAndNightCell,DayAndNightmovespec> targets )
    {	if(c==null) { return(false); }
        switch (board_state)
        {
		case Play:
		case DropDark:
		case DropLight:
			return(targets.get(c)!=null);
		case Gameover:
		case Resign:
			return(false);
		case Confirm:
			return(isDest(c));
        default:
        	throw G.Error("Not expecting Hit Board state " + board_state);
        case Puzzle:
            return (true);
        }
    }
    
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(DayAndNightmovespec m)
    {
        robotState.push(board_state); //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        Execute(m,replayMode.Replay);
        if(DoneState())  { doDone(replayMode.Replay); }
        else { G.Error("Should be in a done state"); }
        acceptPlacement();
       
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(DayAndNightmovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	DayAndNightState state = robotState.pop();
        switch (m.op)
        {
        default:
   	    	throw G.Error("Can't un execute " + m);
        case MOVE_DONE:
        case MOVE_PASS:
            break;
        case MOVE_FROM_TO:
        	{
        	DayAndNightCell src = getCell(m.from_col,m.from_row);
        	DayAndNightCell dest = getCell(m.to_col,m.to_row);
        	DayAndNightChip top = dest.topChip();

        	// subtle point here, do the setboard in reverse order
        	// so the occupiedCells stack will be a simple pop
        	// and preserve the order
        	SetBoard(dest,null);
        	SetBoard(src,top);
        	}
        	break;
        case MOVE_DROPB:
        	SetBoard(getCell(m.to_col,m.to_row),null);
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(state);
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
 // count the line length in some direction
 private int lineLength(DayAndNightCell from,DayAndNightChip ch,int direction)
 {
	 int n = 0;
	 DayAndNightCell c = from;
	 while( ((c=c.exitTo(direction))!=null) 
			 && c.topChip()==ch)
	 { n++; }
	return(n);
 }
 
 // true if a winning line is created starting at "from" containing chip "ch"
 private boolean winningLine(DayAndNightCell from,DayAndNightChip ch)
 {	
	 if(lineLength(from,ch,CELL_LEFT)+lineLength(from,ch,CELL_RIGHT) >= 4) { return(true); }
	 if(lineLength(from,ch,CELL_UP)+lineLength(from,ch,CELL_DOWN) >= 4) { return(true); }	
	 if(!from.dark) 
	 	{// light squares, also check for diagonal lines
		 if(lineLength(from,ch,CELL_UP_LEFT)+lineLength(from,ch,CELL_DOWN_RIGHT) >= 3) { return(true); }	 
		 if(lineLength(from,ch,CELL_DOWN_LEFT)+lineLength(from,ch,CELL_UP_RIGHT) >= 3) { return(true); }	 
	 	}
	 return(false);
 }
 public void getMovesFrom(CommonMoveStack all,DayAndNightCell c,DayAndNightChip myColor)
 {
	 if(c.dark)
		{	DayAndNightChip top = c.topChip();
			if(top==null)
			{
			all.push(new DayAndNightmovespec(MOVE_DROPB,c.col,c.row,whoseTurn));
			}
			else if((board_state==DayAndNightState.Play) && (top==myColor))
			{
			for(int direction=CELL_LEFT,last=CELL_LEFT+CELL_FULL_TURN;
					direction<last;direction+=CELL_QUARTER_TURN)
				{
				DayAndNightCell next = c.exitTo(direction);
				if(next!=null && next.topChip()==null)
				{
				DayAndNightmovespec mv = new DayAndNightmovespec(MOVE_FROM_TO,c.col,c.row,next.col,next.row,whoseTurn);
				all.push(mv);	
				}
				}
			}
		}
 }
 CommonMoveStack temp = new CommonMoveStack();
 public commonMove getRandomMove(Random r)
 {	
	temp.clear();
	DayAndNightChip myColor = playerChip[whoseTurn];
	switch(robotStrategy)
	{
	case RobotProtocol.MONTEBOT_LEVEL:
	case RobotProtocol.BESTBOT_LEVEL:
	case RobotProtocol.SMARTBOT_LEVEL:
		// pick a random cell in the bounding box.  The bounding box isn't expanded
		// during the random playout, so it's fixed for the duration of the search.
		// the general idea is that this prunes the alternatives to likely moves 
		// much better than full-board and so is more representative of smart play.
		for(int i=0;i<10;i++)
		{
		int col = (bT+r.nextInt(bB-bT+1));
		int row = 1+bL+r.nextInt((bR+1-bL));
		if(((row^col)&1)==1)
			{
			 col++;
			 if(col>=nrows) { col -=2; }
			}
		DayAndNightCell c = getCell((char)('A'+col),row);
		G.Assert(c.dark, "should be a dark cell)");
		getMovesFrom(temp,c,myColor);
		if(temp.size()>0) 	
			{ return(temp.elementAt(r.nextInt(temp.size())));
			}
		}
		break;
	case RobotProtocol.DUMBOT_LEVEL:
	case RobotProtocol.WEAKBOT_LEVEL:
	for(int i=0;i<10;i++)
	{	DayAndNightCell c = darkCells.elementAt(r.nextInt(darkCells.size()));
		getMovesFrom(temp,c,myColor);
		if(temp.size()>0)
		{
			return(temp.elementAt(r.nextInt(temp.size())));
		}
	}
		break;
	default: G.Error("Not expecting strategy %s",robotStrategy);
	}
	return(null);	// couldn't find a cell with moves
 }
 
 CommonMoveStack  GetListOfMoves(Generator generator)
 {	CommonMoveStack all = new CommonMoveStack();

 	switch(board_state)
 	{
 	case Gameover:
 	case Puzzle: break;
 	case DropDark:
 	case Play:
 		{
 		DayAndNightChip myColor = playerChip[whoseTurn];
 		if(robotBoard)
 		{
 			switch(robotStrategy)
 			{
 			default: throw G.Error("Not expecting strategy %s", robotStrategy);
 			case RobotProtocol.ALPHABOT_LEVEL:
 			case RobotProtocol.DUMBOT_LEVEL:
 			case RobotProtocol.WEAKBOT_LEVEL:
 		// restrict the bot to opening close to the center
 		boolean restricted = (generator!=Generator.UI) && chips_on_board<4;
 		int center = nrows/2;
 		int min = center-3;
 		int max = center+3;
 		for(int lim = darkCells.size()-1; lim>=0; lim--)
 		{	DayAndNightCell c = darkCells.elementAt(lim);
 			if(!restricted 
 					|| ((c.row>min) && (c.row<=max) && ((c.col-'A')>min) && ((c.col-'A')<=max)))
 			{
 			getMovesFrom(all,c,myColor);
 			}
 		}
 		if(all.size()==0) { all.push(new DayAndNightmovespec(MOVE_PASS,whoseTurn)); }
 				
 				break;
 			case RobotProtocol.MONTEBOT_LEVEL:			
 			case RobotProtocol.BESTBOT_LEVEL:
 			case RobotProtocol.SMARTBOT_LEVEL:
 				{
 				// this move generator only generates moves withing the current bounding box,
 				// which is 2 cells larger than the box actually used.  The box isn't maintained
 				// during robot play, so it's fixed for the duration of the search
 				for(int row = bT; row<=bB; row++)
 				{	int odd = (row^bL+1)&1;
 					for(int col = bL+odd; col<=bR; col+=2)
 					{	DayAndNightCell c = getCell((char)(col+'A'),row+1);
 						G.Assert(c.dark,"must be dark");
 						getMovesFrom(all,c,myColor);
 					}
 				}
 				}
 				break;
 				
 			}
 		}
 		else
 		{
 		// restrict the bot to opening close to the center
 		for(int lim = darkCells.size()-1; lim>=0; lim--)
 		{	DayAndNightCell c = darkCells.elementAt(lim);
 			getMovesFrom(all,c,myColor);
 		}}
 		if(all.size()==0) 
 			{ all.push(new DayAndNightmovespec(MOVE_PASS,whoseTurn));
 			}
 		}
 		break;
 		
 	case DropLight:
 		// from the user interface when a dark cell has been picked
 		{
 		DayAndNightCell c = getSource();
		for(int direction=CELL_LEFT,last=CELL_LEFT+CELL_FULL_TURN;
					direction<last;direction+=CELL_QUARTER_TURN)
				{
				DayAndNightCell next = c.exitTo(direction);
				if(next!=null && next.topChip()==null)
				{
				all.push(new DayAndNightmovespec(MOVE_DROPB,next.col,next.row,whoseTurn));	
				}
				}
 		}
 		break;
 	case Resign:
 	case Confirm:
 		all.push(new DayAndNightmovespec(MOVE_DONE,whoseTurn));
 		break;
 	default:
 			G.Error("Not expecting state ",board_state);
 	}
 	return(all);
 }
 
 public void initRobotValues(int strat)
 {
	 robotBoard = true;
	 robotStrategy = strat;
 }

 /**
  *  get the board cells that are valid targets right now
  * @return
  */
 public Hashtable<DayAndNightCell, DayAndNightmovespec> getTargets() 
 {
 	Hashtable<DayAndNightCell,DayAndNightmovespec> targets = new Hashtable<DayAndNightCell,DayAndNightmovespec>();
 	CommonMoveStack all = GetListOfMoves(Generator.UI);
 	for(int lim=all.size()-1; lim>=0; lim--)
 	{	DayAndNightmovespec m = (DayAndNightmovespec)all.elementAt(lim);
 		switch(m.op)
 		{
 		case MOVE_FROM_TO:
 			if(pickedObject==null) 
 				{ targets.put(getCell(m.from_col,m.from_row),m); 
 				  break;
 				}
			//$FALL-THROUGH$
		case MOVE_PICKB:
 		case MOVE_DROPB:
 			targets.put(getCell(m.to_col,m.to_row),m);
 			break;
 		case MOVE_DONE:
 		case MOVE_PASS:
 			break;
 		default: G.Error("Not expecting "+m);
 		
 		}
 	}
 	
 	return(targets);
 }
 
 int sweepCounter = 0;
 CellStack markedCells = new CellStack();
 private void sweepAndMark(DayAndNightCell from,DayAndNightChip ch,int direction,int weight)
 {
	 DayAndNightCell next = from;
	 int w = weight;
	 while( (next=next.exitTo(direction))!=null)
	 {
		 DayAndNightChip top = next.topChip();
		 if(top==null) { next.sumWeight(markedCells,sweepCounter,w); break; }
		 else if(top!=ch) { break; }
		 else { w++; }
	 }
 }
 private void sweepAndMark(DayAndNightCell from,DayAndNightChip ch)
 {		// orthogonal lines
	 	for(int direction = CELL_LEFT,last=CELL_LEFT+CELL_FULL_TURN; 
	 			direction<last; 
	 			direction+=CELL_QUARTER_TURN)
	 	{	sweepAndMark(from,ch,direction,1);
	 	}
	 	// diagonal light lines
	 	if(!from.dark)
	 	{
		 	for(int direction = CELL_UP_LEFT,last=CELL_UP_LEFT+CELL_FULL_TURN; 
		 			direction<last; 
		 			direction+=CELL_QUARTER_TURN)
		 	{
		 		sweepAndMark(from,ch,direction,1);
		 	}
	 		
	 	}
 }
 //
 // the sweep and mark algorithm marks the ends of lines
 // with a weighted sum, the idea being that the ends of
 // current lines are the points that need to be controlled.
 //
 private void sweepAndMark(int who)
 {	
	sweepCounter++;
	markedCells.clear();
	DayAndNightChip myColor = playerChip[who];
 	// build a field where we mark the ends of lines depending on how many
 	// stones are in the line.
	for(int lim=occupiedCells.size()-1; lim>=0; lim--)
	 {
		 DayAndNightCell from = occupiedCells.elementAt(lim);
		 DayAndNightChip ch = from.topChip();
		 if(ch==myColor)
		 {
			 sweepAndMark(from,ch);
		 }
	 }
 }
 double lineEndWeight = 1.0;
 double diagonalAttachWeight = 1.0;
		 
 public double staticEval(int who)
 {	DayAndNightChip myColor = playerChip[who];
	sweepAndMark(who);
	double value = 0.0;
	for(int lim = markedCells.size()-1; lim>=0; lim--)
	{
		DayAndNightCell from = markedCells.elementAt(lim);
		if(from.dark) { value += from.weight*lineEndWeight; }
		else {
			// light square - see if it is guarded
			boolean guarded = false;
			boolean attached = false;
			for(int direction = CELL_LEFT,last=CELL_LEFT+CELL_FULL_TURN;
					direction<last;
					direction+=CELL_QUARTER_TURN)
			{
				DayAndNightCell adj = from.exitTo(direction);
				if(adj!=null)
				{
					DayAndNightChip adjTop = adj.topChip();
					if(adjTop==null) { }
					else if(adjTop==myColor) { attached=true; }
					else { guarded = true; break; }
				}
			}
			if(attached && !guarded) { value += diagonalAttachWeight*from.weight; }
			
		}
	}
	return(value);
 }
}
