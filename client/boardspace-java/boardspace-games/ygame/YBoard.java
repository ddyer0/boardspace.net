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
package ygame;

import static ygame.Ymovespec.*;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import lib.*;
import lib.Random;
import online.game.*;

/**
 * Y knows all about the game of Y, which is played
 * on an unusual board. 
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

class YBoard extends RBoard<YCell> implements BoardProtocol,YConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game
	int sweep_counter = 0;
	public int getMaxRevisionLevel() { return(REVISION); }
	YVariation variation = YVariation.Y;
	private YState board_state = YState.Puzzle;	
	private YState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	public YState getState() { return(board_state); }
	YCell board[][] = null;
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(YState st) 
	{ 	unresign = (st==YState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

    private YId playerColor[]={YId.White_Chip_Pool,YId.Black_Chip_Pool};    
    private YChip playerChip[]={YChip.White,YChip.Black};
    private YCell playerCell[]=new YCell[2];
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public YChip getPlayerChip(int p) { return(playerChip[p]); }
	public YId getPlayerColor(int p) { return(playerColor[p]); }
	public YCell getPlayerCell(int p) { return(playerCell[p]); }
	public YChip getCurrentPlayerChip() { return(playerChip[whoseTurn]); }

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
    private int fullBoard = 0;				// the number of cells in the board

    private boolean swapped = false;
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public YChip pickedObject = null;
    public YChip lastPicked = null;
    private YCell blackChipPool = null;	// dummy source for the chip pools
    private YCell whiteChipPool = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    
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
    private CellStack occupiedCells = new CellStack();
    private YState resetState = YState.Puzzle; 
    public YChip lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public YCell newcell(char c,int r)
	{	return(new YCell(YId.BoardLocation,c,r));
	}
	// constructor 
    public YBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = YGRIDSTYLE;
        setColorMap(map, players);
		initBoard(YnInCol,YNeighbors,YCoords ,YPerm );
        doInit(init,key,players,rev); // do the initialization 
        //autoReverseY();		// reverse_y based on the color map
    }
    
    public String gameType() { return(gametype+" "+players_in_game+" "+randomKey+" "+revision); }
    

    public void doInit(String gtype,long key)
    {
    	Tokenizer tok = new Tokenizer(gtype);
    	String typ = tok.nextToken();
    	int np = tok.hasMoreTokens() ? tok.intToken() : players_in_game;
    	long ran = tok.hasMoreTokens() ? tok.longToken() : key;
    	int rev = tok.hasMoreTokens() ? tok.intToken() : revision;
    	doInit(typ,ran,np,rev);
    }

    private void initBoard(int []nInCol,int[][]neighbors,double [][]coords,int perm[])
    {	// cellnumbers will correspond to the hexwiki page
    	// rows and columns start with a-1 and the lower left
    	// with i 1-5 straight down at the center.
    	// neighbors are and locations are as specified in hexwiki
    	// perm is an ad-hoc mapping to take our col,row to the cellnumber in hexwiki
    	board = new YCell[nInCol.length][0];
    	int idx = 0;
    	allCells = null;
    	forgetCellArray();
    	YCell cellArray[] = new YCell[coords.length];
    	for(int i=0;i<nInCol.length;i++) 
    		{ int nr = nInCol[i];
    		  YCell row[] = board[i]= new YCell[nr];
    		  for(int j=0;j<nr;j++)
    		  {	// this numbering scheme gives a-i across the bottom
    			// with row 1 at the bottom, row 2 above it and so on
    			  YCell c = row[j] = newcell((char)('A'+j),i+1);
    			  c.next = allCells;
    			  c.cellNumber = perm[idx];
    	     	  c.xloc = coords[idx][0];
    			  c.yloc = coords[idx][1];
    			  allCells = c;
     			  cellArray[idx++] = c;
    		  }
    		}
    	// note that the connectivity array depends on this particular order
    	// of the cells in the cellarray, so we build it ourselves rather
    	// than whatever the default build process does.
    	setHiddenCellArray(cellArray);
    	
    	G.Assert(idx==coords.length,"Coords length mismatch");
    	G.Assert(idx==neighbors.length,"Neighbors length mismatch");
    	G.Assert(perm==null || idx==perm.length,"Perm length mismatch");
     	for(int i=0;i<idx;i++)
    	{	
    		YCell from = (YCell)cellArray[i];
 	  		int nSpec[] = neighbors[i];
	   		for(int connectTo : nSpec)
    		{	from.addLink((YCell)cellArray[connectTo]);
    		}
    	}
     	// mark the edge properties of the cells
     	for(YCell c : board[0]) { c.edgeMask |= 1; } 
     	YCell last[] = board[board.length-1];
     	for(YCell c[] : board) 
     		{ c[0].edgeMask|= 2;
     		  if(c==last) { c[0].edgeMask |= 4; }
     		  	else { c[c.length-1].edgeMask|= 4;}
     		}
     }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int players,int rev)
    {	randomKey = key;
    	adjustRevision(rev);
    	players_in_game = players;
    	win = new boolean[players];
 		Random r = new Random(734687);	// this random is used to assign hash values to cells, common to all games of this type.
		setState(YState.Puzzle);
		variation = YVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		gametype = gtype;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case Y:
		}

		allCells.setDigestChain(r);		// set the randomv for all cells on the board
 		
	    
	    blackChipPool = new YCell(r,YId.Black_Chip_Pool);
	    blackChipPool.addChip(YChip.Black);
	    whiteChipPool = new YCell(r,YId.White_Chip_Pool);
	    whiteChipPool.addChip(YChip.White);
	    	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    chips_on_board = 0;
	    droppedDestStack.clear();
	    pickedSourceStack.clear();
	    stateStack.clear();
	    
	    pickedObject = null;
	    resetState = null;
	    lastDroppedObject = null;
	    int map[]=getColorMap();
	    // black moves first
	    playerCell[FIRST_PLAYER_INDEX] = blackChipPool; 
	    playerCell[SECOND_PLAYER_INDEX] = whiteChipPool; 
		playerColor[map[1]]=YId.White_Chip_Pool;
		playerColor[map[0]]=YId.Black_Chip_Pool;
		playerChip[map[1]]=YChip.White;
		playerChip[map[0]]=YChip.Black;
	    // set the initial contents of the board to all empty cells
		emptyCells.clear();
		occupiedCells.clear();
		for(YCell c = allCells; c!=null; c=c.next) { c.reInit(); emptyCells.push(c); }
		fullBoard = emptyCells.size();
		    
        animationStack.clear();
        swapped = false;
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }


    /** create a copy of this board */
    public YBoard cloneBoard() 
	{ YBoard dup = new YBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((YBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(YBoard from_b)
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
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        lastPicked = null;

        getCell(playerCell,from_b.playerCell);
        AR.copy(playerColor,from_b.playerColor);
        AR.copy(playerChip,from_b.playerChip);
 
        if(G.debug()) { sameboard(from_b); }
    }

    

    public void sameboard(BoardProtocol f) { sameboard((YBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(YBoard from_b)
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
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,revision);
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
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
        	throw G.Error("Move not complete, can't change the current player");
        case Puzzle:
            break;
        case Play:
        case PlayOrSwap:
        	// some damaged games have 2 dones in a row
        	if(replay==replayMode.Live) { throw G.Error("Move not complete, can't change the current player"); }
			//$FALL-THROUGH$
		case ConfirmSwap:
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
    
    public boolean winForPlayerNow(int player)
    {	if(win[player]) { return(true); }
    	sweep_counter++;
    	int cc = edgeContactCount(player);
    	if(cc==3)
    	{
    		win[player]=true;
    		return(true);
    	}
    	return(false);
    }
    
    public int contactCount[] = { 0, 1, 1, 2, 1, 2, 2, 3};	// index by the mask which is a bitmask
    //
    // return 0-3, the count of the number of edges this player contacts
    // with their best group
    //
    public int edgeContactCount(int player)
    {	int best = 0;
    	for(int lim=occupiedCells.size()-1; lim>=0; lim--)
    	{
    		YCell seed = occupiedCells.elementAt(lim);
    		if(seed.topChip().id==playerColor[player])
    		{
    		int mask = seed.sweepEdgeMask(0,sweep_counter);
    		if(mask==7) 
    			{ return(contactCount[mask]);
    			}
    		int count = contactCount[mask];
    		if(count>best) { best = count; }
    		}
    	}
    	return(best);
    }
    

    // set the contents of a cell, and maintain the books
    public YChip SetBoard(YCell c,YChip ch)
    {	YChip old = c.chip;
    	if(c.onBoard)
    	{
    	if(old!=null) { chips_on_board--;emptyCells.push(c); occupiedCells.remove(c,false); }
     	if(ch!=null) { chips_on_board++; emptyCells.remove(c,false); occupiedCells.push(c); }
    	}
       	c.chip = ch;
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
    private YCell unDropObject()
    {	YCell rv = droppedDestStack.pop();
    	setState(stateStack.pop());
    	pickedObject = SetBoard(rv,null); 	// SetBoard does ancillary bookkeeping
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	YCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	SetBoard(rv,pickedObject);
    	pickedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(YCell c)
    {
       droppedDestStack.push(c);
       stateStack.push(board_state);
       
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting dest " + c.rackLocation);
        case Black_Chip_Pool:
        case White_Chip_Pool:		// back in the pool, we don't really care where
        	pickedObject = null;
            break;
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        case EmptyBoard:
            lastDroppedObject = pickedObject.getAltDisplayChip(c);
           	SetBoard(c,pickedObject);
            pickedObject = null;
            break;
        }
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(YCell c)
    {	return(droppedDestStack.top()==c);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { YChip ch = pickedObject;
      if(ch!=null)
    	{	return(ch.chipNumber()); 
    	}
      	return (NothingMoving);
    }
    public YCell getCell(char col,int row)
    {
    	YCell r[] = board[row-1];
    	return(r[col-'A']);
    }
   /**
     * get the cell represented by a source code, and col,row
     * @param source
     * @param col
     * @param row
     * @return
     */
    private YCell getCell(YId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source " + source);
        case BoardLocation:
        	return(getCell(col,row));
        case Black_Chip_Pool:
        	return(blackChipPool);
        case White_Chip_Pool:
        	return(whiteChipPool);
        } 	
    }
    /**
     * this is called when copying boards, to get the cell on the new (ie current) board
     * that corresponds to a cell on the old board.
     */
    public YCell getCell(YCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(YCell c)
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

        case Black_Chip_Pool:
        case White_Chip_Pool:
        	lastPicked = pickedObject = c.topChip();
        }
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(YCell c)
    {	return(c==pickedSourceStack.top());
    }
    public YCell getSource() { return(pickedSourceStack.top()); }
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
        case PlayOrSwap:
			setState(YState.Confirm);
			break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    private void setNextStateAfterDone(replayMode replay)
    {	G.Assert(chips_on_board+emptyCells.size()==fullBoard,"empty cells incorrect");
    	G.Assert(chips_on_board==occupiedCells.size(),"occupied cells incorrect");
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state "+board_state);
    	case Gameover: break;
    	case ConfirmSwap: 
    		setState(YState.Play); 
    		break;
    	case Confirm:
    	case Puzzle:
    	case Play:
    	case PlayOrSwap:
    		setState(((chips_on_board==1)&&(whoseTurn==SECOND_PLAYER_INDEX)&&!swapped) 
    				? YState.PlayOrSwap
    				: YState.Play);
    		
    		break;
    	}
       	resetState = board_state;
    }
    private void doDone(replayMode replay)
    {
        acceptPlacement();

        if (board_state==YState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(YState.Gameover);
        }
        else
        {	if(winForPlayerNow(whoseTurn)) 
        		{ win[whoseTurn]=true;
        		  p1("gameOver");
        		  setState(YState.Gameover); 
        		}
        	else if(emptyCells.size()==0) 
        	{ G.Error("should be won"); } 
        	else
        	{
        		setNextPlayer(replay);
        		setNextStateAfterDone(replay);
        	}
        }
    }


void doSwap(replayMode replay)
{	YId c = playerColor[0];
	YChip ch = playerChip[0];
	playerColor[0]=playerColor[1];
	playerChip[0]=playerChip[1];
	playerColor[1]=c;
	playerChip[1]=ch;
	YCell cc = playerCell[0];
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
		  setState(YState.ConfirmSwap);
		  break;
	case ConfirmSwap:
		  setState(YState.PlayOrSwap);
		  break;
	case Gameover:
	case Puzzle: break;
	}
	}
	
    public boolean Execute(commonMove mm,replayMode replay)
    {	Ymovespec m = (Ymovespec)mm;
        if(replay.animate) { animationStack.clear(); }

        //G.print("E "+m+" for "+whoseTurn+" "+state);
        switch (m.op)
        {
		case MOVE_SWAP:	// swap colors with the other player
			doSwap(replay);
			break;
        case MOVE_DONE:

         	doDone(replay);

            break;

        case MOVE_DROPB:
        	{
			YChip po = pickedObject;
			YCell dest =  getCell(YId.BoardLocation,m.to_col,m.to_row);
			YCell src = getSource();
			if(isSource(dest)) 
				{ unPickObject(); 
				}
				else 
				{
				if(pickedObject==null)
				{
					pickObject(src=playerCell[whoseTurn]);
				}
				m.chip = pickedObject;
	            dropObject(dest);
	            /**
	             * if the user clicked on a board space without picking anything up,
	             * animate a stone moving in from the pool.  For Hex, the "picks" are
	             * removed from the game record, so there are never picked stones in
	             * single step replays.
	             */
	            if(replay.animate && (po==null))
	            	{ animationStack.push(src);
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
 			YCell src = getCell(m.source,m.to_col,m.to_row);
 			if(isDest(src)) { unDropObject(); }
 			else
 			{
        	// be a temporary p
        	pickObject(src);
        	switch(board_state)
        	{
        	case Puzzle:
         		break;
        	case Confirm:
        		setState(((chips_on_board==1) && !swapped) ? YState.PlayOrSwap : YState.Play);
        		break;
        	default: ;
        	}}}
            break;

        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
            YCell dest = getCell(m.source,m.to_col,m.to_row);
            if(isSource(dest)) { unPickObject(); }
            else { dropObject(dest); }
        	}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            int nextp = nextPlayer[whoseTurn];
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(YState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ setState(YState.Gameover); 
               	}
            else {  setNextStateAfterDone(replay); }

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?YState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(YState.Puzzle);
 
            break;
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(YState.Gameover);
			break;

        default:
        	cantExecute(m);
        }
        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting Legal Hit state " + board_state);
        case PlayOrSwap:
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
            return ((pickedObject!=null)?(pickedObject==playerChip[player]):true);
        }
    }

    public boolean LegalToHitBoard(YCell c)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
		case Play:
		case PlayOrSwap:
			return(c.isEmpty());
		case ConfirmSwap:
		case Gameover:
		case Resign:
			return(false);
		case Confirm:
			return(isDest(c));
        default:
        	throw G.Error("Not expecting Hit Board state " + board_state);
        case Puzzle:
            return ((pickedObject==null)==(c.topChip()!=null));
        }
    }
    
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Ymovespec m)
    {
        robotState.push(board_state); //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        if (Execute(m,replayMode.Replay))
        {
            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {	
                doDone(replayMode.Replay);
                
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
    public void UnExecute(Ymovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	YState state = robotState.pop();
        switch (m.op)
        {
        default:
   	    	throw G.Error("Can't un execute " + m);
        case MOVE_DONE:
            break;
            
        case MOVE_SWAP:
        	setState(state);
        	doSwap(replayMode.Replay);
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
  
 public commonMove getRandomMove(Random r)
 {
	 YCell c = emptyCells.elementAt(r.nextInt(emptyCells.size()));
	 return(new Ymovespec(MOVE_DROPB,c,whoseTurn));
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	
 	switch(board_state)
 	{
 	default: throw G.Error("Not expecting state %s",board_state);
 	case Confirm:
 	case Resign:
 	case ConfirmSwap:
 		all.push(new Ymovespec(MOVE_DONE,whoseTurn));
 		break;
 	case PlayOrSwap:
 		all.push(new Ymovespec(SWAP,whoseTurn));
		//$FALL-THROUGH$
	case Play:
	 	for(int lim = emptyCells.size()-1; lim>=0; lim--)
	 	{
	 	 YCell c = emptyCells.elementAt(lim);
	 	 all.addElement(new Ymovespec(MOVE_DROPB,c,whoseTurn));
	 	}
	 	break;
 	}
 	if(all.size()==0) 
 		{ p1("No moves"); 
 		}
 	return(all);
 }
 
 public void initRobotValues(YPlay ro)
 {	robot = ro;
 }

 YPlay robot = null;
 public boolean p1(String msg)
 {
 	if(G.p1(msg) && (robot!=null))
 	{	String dir = "g:/share/projects/boardspace-html/htdocs/y/ygames/robot/";
 		robot.saveCurrentVariation(dir+msg+".sgf");
 		return(true);
 	}
 	return(false);
 }

 
 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {  
 	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
 }
 /**
  * draw the grid according to the specified grid style.  This may
  * include drawing cells, lines, dots or other bits of board representation
  * as well as the usual row and column numbers.
  * @param gc
  * @param boardRect
  * @param use_grid
  * @param backgroundColor
  * @param pointColor
  * @param lineColor
  * @param gridColor
  */
 public void DrawGrid(Graphics gc, Rectangle boardRect, boolean use_grid,
         Color backgroundColor, Color pointColor, Color lineColor, Color gridColor)
     {
	 int ypos0 = G.Bottom(boardRect);
	 int xpos0 = G.Left(boardRect);
	 if(drawing_style==DrawingStyle.STYLE_LINES)
	 {
	 for(YCell c =allCells; c!=null; c=c.next)
	 {
         // draw a small dot to mark the center position
         int ypos = ypos0 - cellToY(c);
         int xpos = xpos0 + cellToX(c);
         int POINTRADIUS = 1;

         if(c.linkCount()>0) 
         	{GC.cacheAACircle(gc, xpos, ypos, POINTRADIUS, pointColor,  backgroundColor, true);
         	}
         GC.setColor(gc,lineColor);

         // draw lines to the adjacent positions
         for (int dir = 0; dir < c.nAdjacentCells(); dir++)
         { // this convolution is to find the 6 adjacent board positions
           // and draw a line to each of them if it's a valid board position
        	 YCell nc = c.exitTo(dir);
             if (nc!=null)
             {
                 int nypos = ypos0-cellToY(nc);
                 int nxpos = xpos0 + cellToX(nc);
                 gc.drawLine(xpos, ypos, nxpos, nypos);
             }
         }
     }}
	 if(use_grid)
	 	{
		 // letters across the bottom
		 {
		 YCell row0[] = board[0];
		 YCell row1[] = board[1];
		 int row0len = row0.length;
		 for(int i=0;i<row0len;i++)
		 {
		 YCell r0 = row0[i];
		 YCell r1 = row1[i];
		 drawGridPair(gc,r0,r1,gridColor,xpos0,ypos0,""+r0.col);
		 }}
		 
		 for(YCell row0[] : board)
		 {	int row0len = row0.length;
		 	YCell r0 = row0[0];
		 	drawGridPair(gc,r0,row0[1],gridColor,xpos0,ypos0,""+r0.col+r0.row);
		 	YCell r2 = row0[row0len-1];
		 	drawGridPair(gc,r2,row0[row0len-2],gridColor,xpos0,ypos0,""+r2.col+r2.row);
		 }
	 	}
     }
 private void drawGridPair(Graphics gc,YCell r0,YCell r1,Color gridColor,int xpos0,int ypos0,String msg)
 {
	 int x0 = cellToX(r0);
	 int x1 = cellToX(r1);
	 int y0 = cellToY(r0);
	 int y1 = cellToY(r1);
	 int xpos = xpos0+G.interpolate(-0.6, x0,x1);
	 int ypos = ypos0-G.interpolate(-0.6, y0,y1);
	 FontMetrics fm = GC.getFontMetrics(gc);
	 DrawGridCoord(gc, gridColor,xpos-fm.stringWidth(msg)/2,ypos+fm.getHeight()/2,G.distanceSQ(x0, x1, y0, y1),msg);
 }

 double scale = 1.0;
 double xoff = 0;
 double yoff = 0;
 public void SetDisplayRectangle(Rectangle r)
 {
	 super.SetDisplayRectangle(r);
	 int w = G.Width(r);
	 int h = G.Height(r);
	 scale = Math.min(w,h)*0.75;
	 xoff = (w-scale)/2+w*0.02;
	 yoff = (h-scale)/2-h*0.02;
	 }
 public int cellToX(YCell c) {
	 return((int)(scale*c.xloc+xoff));
}
 
public int cellToY(YCell c) {
		return((int)(scale*c.yloc+yoff));
}
	
}