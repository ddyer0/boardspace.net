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
package slither;


import static slither.Slithermovespec.*;

import bridge.Color;
import java.util.*;
import lib.*;
import lib.Random;
import online.game.*;

/**
 * SliBoard knows all about the game of Slither, which is played
 * on a hexagonal board. It gets a lot of logistic support from 
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

class SlitherBoard 
	extends rectBoard<SlitherCell>	// for a square grid board, this could be rectBoard or squareBoard 
	implements BoardProtocol,SlitherConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
	static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	SlitherVariation variation = SlitherVariation.slither;
	private SlitherState board_state = SlitherState.Puzzle;	
	private SlitherState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	public SlitherState getState() { return(board_state); }
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(SlitherState st) 
	{ 	unresign = (st==SlitherState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

    private SlitherId playerColor[]={SlitherId.Black,SlitherId.White};    
    private SlitherChip playerChip[]={SlitherChip.black,SlitherChip.white};
    private SlitherCell playerCell[]=new SlitherCell[2];
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
    public int playerIndex(SlitherChip ch) { return ((ch==playerChip[0]) ? 0 : 1); }
	public SlitherChip getPlayerChip(int p) { return(playerChip[p]); }
	public SlitherId getPlayerColor(int p) { return(playerColor[p]); }
	public SlitherCell getPlayerCell(int p) { return(playerCell[p]); }
	public SlitherChip getCurrentPlayerChip() { return(playerChip[whoseTurn]); }
	public SlitherPlay robot = null;
	
 	public int lastPlacedIndex = 0;
	
	/* this can replace "G.Assert" in the this file, so if the assertion
	 * fails in a search, the state is recorded automatically.
	 */
	 public boolean p1(boolean condition,String msg,Object... args)
	 {	if(!condition)
	 	{
		 p1(G.concat(msg,args));
		 G.Error(msg,args);
	 	}
	 	return condition;
	 }
	private int sweep_counter =0;
	public boolean winForPlayerNow(int who)
	{
		CellStack occ = occupiedCells[who];
		if(occ.size()<ncols) { return false; }
		SlitherChip chip = playerChip[who];
		SlitherCell seed = getCell('A',1);
		int direction = (chip==SlitherChip.black) ? CELL_RIGHT : CELL_UP;
		while(seed!=null)
		{
			sweep_counter++;
			int span = chip==SlitherChip.black	
					       ? rowSpan(seed,chip) 
					       : colSpan(seed,chip);
			if(span==ncols) { return true; }
					    
			seed = seed.exitTo(direction);
		}
		return false;	
	}
	// sweep, return the highest row reached
	private int rowSpan(SlitherCell seed,SlitherChip top)
	{
		if(seed.sweep_counter==sweep_counter) { return 0; }
		seed.sweep_counter = sweep_counter;
		if(seed.topChip()!=top) { return 0; }
		int max = seed.row;
		for(int direction = CELL_UP; direction<CELL_FULL_TURN+CELL_UP && max<ncols; direction += CELL_QUARTER_TURN)
		{	SlitherCell adj = seed.exitTo(direction);
			if(adj!=null) { max  = Math.max(max,rowSpan(adj,top)); }
		}	
		return max;
	}
	// sweep, return the highest column reached.
	private int colSpan(SlitherCell seed,SlitherChip top)
	{
		if(seed.sweep_counter==sweep_counter) { return 0; }
		seed.sweep_counter = sweep_counter;
		if(seed.topChip()!=top) { return 0; }
		int max = (seed.col-'A')+1;
		for(int direction = CELL_RIGHT; direction<CELL_FULL_TURN+CELL_RIGHT && max<ncols; direction += CELL_QUARTER_TURN)
		{	SlitherCell adj = seed.exitTo(direction);
			if(adj!=null) { max  = Math.max(max,colSpan(adj,top)); }
		}	
		return max;
	}
	

	/**
	 * save the current state of the search as a file in the /robot/ directory.  This
	 * requires cooperation with the way the robot teats moves, and some behaviours
	 * tend to cause problems.  In partular "auto-done" after robot moves assumes
	 * that any time the current player changes, there was an implicit "done".
	 * The robot's saveCurrentVariation method may need to be customized. 
	 * @param msg
	 * @return
	 */
	public boolean p1(String msg)
		{
			if(G.debug() && G.p1(msg) && robot!=null)
			{	String dir = "g:/share/projects/boardspace-html/htdocs/slither/slithergames/robot/";
				robot.saveCurrentVariation(dir+msg+".sgf");
				return(true);
			}
			return(false);
		}
	
// this is required even if it is meaningless for this game, but possibly important
// in other games.  When a draw by repetition is detected, this function is called.
// the game should have a "draw pending" state and enter it now, pending confirmation
// by the user clicking on done.   If this mechanism is triggered unexpectedly, it
// is probably because the move editing in "editHistory" is not removing last-move
// dithering by the user, or the "Digest()" method is not returning unique results
// other parts of this mechanism: the Viewer ought to have a "repRect" and call
// DrawRepRect to warn the user that repetitions have been seen.
	public void SetDrawState() { setState(SlitherState.Draw); }	CellStack animationStack = new CellStack();
    private int chips_on_board = 0;			// number of chips currently on the board
    private int fullBoard = 0;				// the number of cells in the board

    private boolean swapped = false;
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public SlitherChip pickedObject = null;
    public SlitherChip lastPicked = null;
    private SlitherCell blackChipPool = null;	// dummy source for the chip pools
    private SlitherCell whiteChipPool = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    public SlitherCell slideFrom = null;
    public SlitherCell slideTo = null;
    
    // save strings to be shown in the game log
    StringStack gameEvents = new StringStack();
    InternationalStrings s = G.getTranslations();
    
 	void logGameEvent(String str,String... args)
 	{	//if(!robotBoard)
 		{String trans = s.get(str,args);
 		 gameEvents.push(trans);
 		}
 	}

    private CellStack emptyCells=new CellStack();
    private CellStack occupiedCells[] = { new CellStack(),new CellStack()};
    
    public boolean validBoard()
    {
    	for(SlitherCell c = allCells; c!=null; c=c.next)
    	{	if(c!=null && !validCell(c)) 
    			{ return false; 
    			}
    	}
    	return true;
    }
    public boolean validCell(SlitherCell filled)
    {
		SlitherChip top = filled.topChip();
		return (top==null || validCell(top,filled,null,null));
    }
    public boolean validCell(SlitherChip top, SlitherCell filled, SlitherCell empty,SlitherCell fill)
    {
    	return filled.validDiagonalContacts(top,empty,fill);

    }

    private SlitherState resetState = SlitherState.Puzzle; 
    public DrawableImage<?> lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public SlitherCell newcell(char c,int r)
	{	return(new SlitherCell(SlitherId.BoardLocation,c,r));
	}
	
	// constructor 
    public SlitherBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_ORTHOGONAL_LINES; // draw only the orthoginal lines
        Grid_Style = GRIDSTYLE;
        setColorMap(map, players);
        
		Random r = new Random(734687);
		// do this once at construction
	    blackChipPool = new SlitherCell(r,SlitherId.Black);
	    blackChipPool.addChip(SlitherChip.black);
	    whiteChipPool = new SlitherCell(r,SlitherId.White);
	    whiteChipPool.addChip(SlitherChip.white);

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
 		setState(SlitherState.Puzzle);
		variation = SlitherVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		gametype = gtype;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case slither:
		case slither_13:
		case slither_19:
			// using reInitBoard avoids thrashing the creation of cells 
			// when reviewing games.
			reInitBoard(variation.size,variation.size);
			// or initBoard(variation.firstInCol,variation.ZinCol,null);
			// Random r = new Random(734687);	// this random is used to assign hash values to cells, common to all games of this type.
			// allCells.setDigestChain(r);		// set the randomv for all cells on the board
		}

 		
	    playerCell[FIRST_PLAYER_INDEX] = blackChipPool; 
	    playerCell[SECOND_PLAYER_INDEX] = whiteChipPool; 
	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    chips_on_board = 0;
	    
	    acceptPlacement();
	    
	    resetState = null;
	    lastDroppedObject = null;
	    lastPlacedIndex = 0;
	    int map[]=getColorMap();
		playerColor[map[1]]=SlitherId.White;
		playerColor[map[0]]=SlitherId.Black;
		playerChip[map[1]]=SlitherChip.white;
		playerChip[map[0]]=SlitherChip.black;
	    // set the initial contents of the board to all empty cells
		emptyCells.clear();
		occupiedCells[0].clear();
		occupiedCells[1].clear();
		
		for(SlitherCell c = allCells; c!=null; c=c.next) { c.reInit(); emptyCells.push(c); }
		fullBoard = emptyCells.size();
	    
        animationStack.clear();
        swapped = false;
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }

    /** create a copy of this board */
    public SlitherBoard cloneBoard() 
	{ SlitherBoard dup = new SlitherBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((SlitherBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(SlitherBoard from_b)
    {
        super.copyFrom(from_b);
        chips_on_board = from_b.chips_on_board;
        fullBoard = from_b.fullBoard;
        robotState.copyFrom(from_b.robotState);
        getCell(emptyCells,from_b.emptyCells);
        getCell(occupiedCells,from_b.occupiedCells);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        copyFrom(whiteChipPool,from_b.whiteChipPool);		// this will have the side effect of copying the location
        copyFrom(blackChipPool,from_b.blackChipPool);		// from display copy boards to the main board
        getCell(playerCell,from_b.playerCell);
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        slideFrom = getCell(from_b.slideFrom);
        slideTo = getCell(from_b.slideTo);
        resetState = from_b.resetState;
        lastPicked = null;
        lastPlacedIndex = from_b.lastPlacedIndex;

        AR.copy(playerColor,from_b.playerColor);
        AR.copy(playerChip,from_b.playerChip);
 
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((SlitherBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(SlitherBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor mismatch");
        G.Assert(AR.sameArrayContents(playerChip,from_b.playerChip),"playerChip mismatch");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(chips_on_board == from_b.chips_on_board,"chips_on_board mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedsourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(sameCells(playerCell,from_b.playerCell),"player cell mismatch");
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
		v ^= chip.Digest(r,playerChip[0]);	// this accounts for the "swap" button
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,slideFrom);
		v ^= Digest(r,slideTo);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,revision);
		v ^= Digest(r,board_state);
		v ^= Digest(r,whoseTurn);
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
		case ConfirmSwap:
		case SlideOrDone:
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
 

    // set the contents of a cell, and maintain the books
    private int lastPlaced = -1;
    public SlitherChip SetBoard(SlitherCell c,SlitherChip ch)
    {	SlitherChip old = c.topChip();
    	if(c.onBoard)
    	{
    	if(old!=null) 
    		{ chips_on_board--;
    		  emptyCells.push(c);  
    		  c.lastPicked = lastPlacedIndex;
    		  occupiedCells[playerIndex(old)].remove(c,false);
    		}
     	if(ch!=null) 
     		{ chips_on_board++; 
     		  emptyCells.remove(c,false); 
     		  occupiedCells[playerIndex(ch)].push(c);
     		  lastPlaced = c.lastPlaced; 
     		  c.lastPlaced = lastPlacedIndex; 
     		}
     	else { c.lastPlaced = lastPlaced; }
    	}
       	if(old!=null) { c.removeTop();}
       	if(ch!=null) { c.addChip(ch);  }
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
        slideFrom = null;
        slideTo = null;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private SlitherCell unDropObject()
    {	SlitherCell rv = droppedDestStack.pop();
    	setState(stateStack.pop());
    	pickedObject = SetBoard(rv,null); 	// SetBoard does ancillary bookkeeping
    	lastPlacedIndex--;
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	SlitherCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	SetBoard(rv,pickedObject);
    	pickedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(SlitherCell c)
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
        	SetBoard(c,pickedObject);
            pickedObject = null;
            break;
        }
       lastPlacedIndex++;
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(SlitherCell c)
    {	return(droppedDestStack.top()==c);
    }
    public SlitherCell getDest()
    {	return(droppedDestStack.top());
    }
 
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { SlitherChip ch = pickedObject;
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
    private SlitherCell getCell(SlitherId source, char col, int row)
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
    public SlitherCell getCell(SlitherCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(SlitherCell c)
    {	pickedSourceStack.push(c);
    	stateStack.push(board_state);
        switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting rackLocation " + c.rackLocation);
        case BoardLocation:
        	{
            lastPicked = pickedObject = c.topChip();
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
    public boolean isSource(SlitherCell c)
    {	return(c==pickedSourceStack.top());
    }
    public SlitherCell getSource()
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
        case SlideOrDone:
        	setState(SlitherState.Confirm);
        	break;
        case PlayOrSlide:
        	SlitherCell from = pickedSourceStack.top();
        	if(from.onBoard)
        	{
        		setState(SlitherState.Play);
        		slideFrom = from;
        		slideTo = droppedDestStack.top();
        	}
        	else
        	{
        		setState(SlitherState.SlideOrDone);
        	}
        	break;
        	
        case Invalid:
        	if(validBoard()) { setState(SlitherState.Puzzle); }
			//$FALL-THROUGH$
		case Puzzle:
			acceptPlacement();
            break;
        }
    }
    private void setNextStateAfterDone(replayMode replay)
    {	G.Assert(emptyCells.size()+occupiedCells[0].size()+occupiedCells[1].size()==fullBoard,
    			"cells miscounted");
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state "+board_state);
    	case Gameover: break;
    	case ConfirmSwap: 
    		setState(SlitherState.Play); 
    		break;
    	case Confirm:	
    	case SlideOrDone:
    		setState((occupiedCells[whoseTurn].size()==0) ? SlitherState.Play : SlitherState.PlayOrSlide);
    		break;
    	case Puzzle:
     		setState(SlitherState.Play);		
    		break;
    	}
       	resetState = board_state;
    }
    private void doDone(replayMode replay)
    {
        acceptPlacement();
     	lastPlacedIndex++;

        if (board_state==SlitherState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(SlitherState.Gameover);
        }
        else
        {	if(winForPlayerNow(whoseTurn)) 
        		{ win[whoseTurn]=true;
        		  setState(SlitherState.Gameover); 
        		}
        	else {setNextPlayer(replay);
        		setNextStateAfterDone(replay);
        	}
        }
    }
void doSwap(replayMode replay)
{	SlitherId c = playerColor[0];
	SlitherChip ch = playerChip[0];
	playerColor[0]=playerColor[1];
	playerChip[0]=playerChip[1];
	playerColor[1]=c;
	playerChip[1]=ch;
	SlitherCell cc = playerCell[0];
	playerCell[0]=playerCell[1];
	playerCell[1]=cc;
	swapped = !swapped;
	switch(board_state)
	{	
	default: 
		throw G.Error("Not expecting swap state "+board_state);
	case Play:
		// some damaged game records have double swap
		if(replay==replayMode.Live) { G.Error("Not expecting swap state "+board_state); }
		//$FALL-THROUGH$
	case PlayOrSwap:
		  setState(SlitherState.ConfirmSwap);
		  break;
	case ConfirmSwap:
		  setState(SlitherState.PlayOrSwap);
		  break;
	case Gameover:
	case Puzzle: break;
	}
	}
	
    public boolean Execute(commonMove mm,replayMode replay)
    {	Slithermovespec m = (Slithermovespec)mm;
        if(replay.animate) { animationStack.clear(); }

        //G.print("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
		case MOVE_SWAP:	// swap colors with the other player
			doSwap(replay);
			break;
        case MOVE_DONE:

         	doDone(replay);

            break;
        case MOVE_FROM_TO:
        	{
        	SlitherCell from = getCell(m.from_col,m.from_row);
        	SlitherCell to = getCell(m.to_col,m.to_row);
        	pickObject(from);
        	dropObject(to);
        	if(replay.animate) {
        		animationStack.push(from);
        		animationStack.push(to);
        	}
        	setNextStateAfterDrop(replay);
        	}
        	break;
        case MOVE_DROPB:
        	{
			SlitherChip po = pickedObject;
			SlitherCell dest =  getCell(SlitherId.BoardLocation,m.to_col,m.to_row);
			
			if(isSource(dest)) 
				{ unPickObject(); 
				}
				else 
				{
			    if(pickedObject==null) { pickObject(playerCell[m.player]); }
				m.chip = pickedObject;
	            dropObject(dest);
	            /**
	             * if the user clicked on a board space without picking anything up,
	             * animate a stone moving in from the pool.  For Hex, the "picks" are
	             * removed from the game record, so there are never picked stones in
	             * single step replays.
	             */
	            if(replay.animate && (po==null))
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
 			SlitherCell src = m.to_col=='@' ? playerCell[m.to_row] : getCell(m.to_col,m.to_row);
 			if(isDest(src)) { unDropObject(); }
 			else
 			{
        	// be a temporary p
        	pickObject(src);
        	m.chip = pickedObject;
        	switch(board_state)
        	{
        	case Puzzle:
         		break;
        	case Confirm:
        		setState(((chips_on_board==1) && !swapped) ? SlitherState.PlayOrSwap : SlitherState.Play);
        		break;
        	default: ;
        	}}}
            break;

        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
            SlitherCell dest = m.from_col=='@' ? playerCell[m.to_row] : getCell(m.to_col,m.to_row);
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
            if(validBoard())
            {	boolean opening = chips_on_board<2;
            	setState(opening ? SlitherState.Play : SlitherState.PlayOrSlide);
            }
            else {
            	setState(SlitherState.Invalid);
            }
            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?SlitherState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(SlitherState.Puzzle);
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(SlitherState.Gameover);
    	   break;
       case MOVE_PASS:
    	   setState(SlitherState.Confirm);
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
        case PlayOrSlide:
        case SlideOrDone:
        case Play:
        	// for pushfight, you can pick up a stone in the storage area
        	// but it's really optional
        	return(player==whoseTurn);
        case Confirm:
		case ConfirmSwap:
		case Resign:
		case Gameover:
			return(false);
        case Puzzle:
        case Invalid:
            return ((pickedObject!=null)?(pickedObject==playerChip[player]):true);
        }
    }

    public boolean legalToHitBoard(SlitherCell c,Hashtable<SlitherCell,Slithermovespec> targets )
    {	if(c==null) { return(false); }
        switch (board_state)
        {
		case Play:
		case PlayOrSlide:
		case SlideOrDone:
			return(targets.get(c)!=null || isDest(c) || isSource(c));
		case ConfirmSwap:
		case Gameover:
		case Resign:
			return(false);
		case Confirm:
			return(isDest(c) || c.isEmpty());
        default:
        	throw G.Error("Not expecting Hit Board state " + board_state);
        case Puzzle:
        case Invalid:
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
    public void RobotExecute(Slithermovespec m)
    {
        robotState.push(board_state); //record the starting state. The most reliable
        Execute(m,replayMode.Replay);
       
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(Slithermovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	SlitherState state = robotState.pop();
        switch (m.op)
        {
        default:
   	    	throw G.Error("Can't un execute " + m);
        case MOVE_DONE:
        case MOVE_PASS:
            break;
            
        case MOVE_SWAP:
        	setState(state);
        	doSwap(replayMode.Replay);
        	break;
        case MOVE_DROPB:
        	SetBoard(getCell(m.to_col,m.to_row),null);
        	break;
        case MOVE_FROM_TO:
        	{
        	SlitherCell from = getCell(m.from_col,m.from_row);
        	SlitherCell to = getCell(m.to_col,m.to_row);
        	SlitherChip top = to.topChip();
        	SetBoard(to,null);
        	SetBoard(from,top);
        	}
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
 
 private boolean addValidPlayMoves(CommonMoveStack all,int who)
 {
	 boolean some = false;
	 SlitherChip top = playerChip[who];
	 for(int lim = emptyCells.size()-1; lim>=0; lim--)
	 {
		 SlitherCell c = emptyCells.elementAt(lim);
		 if(validCell(top,c,null,c)) { 
			 if(all==null) { return true; }
			 all.push(new Slithermovespec(MOVE_DROPB,c,who));
			 some = true;
		 }
	 }
	 return some;
 }
 
 private boolean addValidSlideMoves(CommonMoveStack all,int who)
 {
	 boolean some = false;
	 CellStack occupied = occupiedCells[who];
	 SlitherChip top = playerChip[who];
	 SlitherCell dropped = droppedDestStack.top();
	 if(pickedObject!=null)
	 {
		SlitherCell from = pickedSourceStack.top();
		if(from.onBoard) 
			{ some = addValidSlideMoves(all,from,top,who);
			}
	 }
	 else
	 {for(int lim = occupied.size()-1; lim>=0; lim--)
	 {
		 SlitherCell from = occupied.elementAt(lim);
		 if(from!=dropped)
		 {
			some |= addValidSlideMoves(all,from,top,who); 
			if(some && all==null) { return true; }
		 }
	 }}
	 return some;
 }
 private boolean addValidSlideMoves(CommonMoveStack all,SlitherCell from,SlitherChip top,int who)
 {	boolean some = false;
 	for(int direction = 0; direction<CELL_FULL_TURN; direction++)
		 {
			 SlitherCell to = from.exitTo(direction);
			 if(to!=null && to.topChip()==null)
			 {
				 if(validCell(top,to,from,to))
				 {
					 boolean valid = true;
					 // also need to check that the cells left behind are valid
					 for(int d2 = CELL_LEFT; valid && d2<CELL_LEFT+CELL_FULL_TURN; d2+=CELL_QUARTER_TURN)
					 {
						 SlitherCell adj = from.exitTo(d2);
						 if(adj!=null && adj.topChip()==top)
						 {
							 valid = validCell(top,adj,from,to);	// still valid without this neighbo
						 }
					 }
				 if(valid)
				 {
					 if(all==null) { return true; }
					 all.push(new Slithermovespec(MOVE_FROM_TO,from,to,who));
					 some = true;
				 }
				 }
			 }
	 }
	 return some;
 }
 
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	if(board_state==SlitherState.PlayOrSwap)
 	{
 		all.addElement(new Slithermovespec(SWAP,whoseTurn));
 	}
 	switch(board_state)
 	{
 	case Puzzle:
 	case Invalid:
 		{int op = pickedObject==null ? MOVE_DROPB : MOVE_PICKB; 	
 			for(SlitherCell c = allCells;
 			 	    c!=null;
 			 	    c = c.next)
 			 	{	if(c.topChip()==null)
 			 		{all.addElement(new Slithermovespec(op,c.col,c.row,whoseTurn));
 			 		}
 			 	}
 		}
 		break;
 	case Play:
 		{
 		boolean some = addValidPlayMoves(all,whoseTurn);
 		if(!some) 
 			{ all.push(new Slithermovespec(MOVE_PASS,whoseTurn)); }
 		}
 		break;
 	case PlayOrSlide:
 		{
 		boolean some = false;
 		if(pickedObject==null || !pickedSourceStack.top().onBoard)
 			{ some |= addValidPlayMoves(all,whoseTurn);
 			}
 		some |= addValidSlideMoves(all,whoseTurn);
		if(!some) 
			{ all.push(new Slithermovespec(MOVE_PASS,whoseTurn)); }		
 		}
 		break;
		
	case SlideOrDone:
 		addValidSlideMoves(all,whoseTurn);
 		all.push(new Slithermovespec(MOVE_DONE,whoseTurn));
 		break;
 	case Slide:
 		// we don't currently use this state, because all single moves are legal
 		// we don't have a mandatory slide to fix it.
 		{
 		boolean some = addValidSlideMoves(all,whoseTurn);
 		if(!some) 
 			{ all.push(new Slithermovespec(MOVE_PASS,whoseTurn)); }
 		}
 		break;
 	case Confirm:
 		all.push(new Slithermovespec(MOVE_DONE,whoseTurn));
 		break;
 	case Gameover: break;
 	default:
 			G.Error("Not expecting state ",board_state);
 	}
 	return(all);
 }
 
 public void initRobotValues(SlitherPlay m)
 {	robot = m;
 }


 /**
  *  get the board cells that are valid targets right now, intended to be used
  *  by the user interface to determine where it's legal to play.  The standard
  *  method is to call the move generator, and filter the results to generate
  *  the cells of interest.  It's usually more complicated than just that,
  *  but using the move generator to drive the selection of cells to point 
  *  at avoids duplicating a lot of tricky logic.
  *  
  * @return
  */
 public Hashtable<SlitherCell, Slithermovespec> getTargets() 
 {
 	Hashtable<SlitherCell,Slithermovespec> targets = new Hashtable<SlitherCell,Slithermovespec>();
 	CommonMoveStack all = GetListOfMoves();
 	for(int lim=all.size()-1; lim>=0; lim--)
 	{	Slithermovespec m = (Slithermovespec)all.elementAt(lim);
 		switch(m.op)
 		{
 		case MOVE_PICKB:
 		case MOVE_DROPB:
 			targets.put(getCell(m.to_col,m.to_row),m);
 			break;
 		case MOVE_SWAP:
 		case MOVE_DONE:
 			break;
 		case MOVE_FROM_TO:
 			{
 			SlitherCell c = (pickedObject==null) ? getCell(m.from_col,m.from_row) : getCell(m.to_col,m.to_row);
 			targets.put(c,m);
 			}
 			break;
 		case MOVE_PASS: break;
 		default: G.Error("Not expecting "+m);
 		
 		}
 	}
 	
 	return(targets);
 }
 

 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(Character.isDigit(txt.charAt(0)))
 	{ 
	xpos -= cellsize/3;
 	}
 	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
 }
 
 //public boolean drawIsPossible() { return false; }
 // public boolean canOfferDraw() {
 //	 return false;
	 /**
	something like this:
 	return (movingObjectIndex()<0)
 			&& ((board_state==SlitherState.Play) || (board_state==SlitherState.DrawPending))
 			&& (moveNumber-lastDrawMove>4);
 			*/
 //}


}
