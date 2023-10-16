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
package havannah;

import static havannah.HavannahMovespec.*;

import java.util.*;

import bridge.Color;
import lib.*;
import lib.Random;
import online.game.*;
import online.search.UCTMoveSearcher;
import online.search.UCTNode;

/**
 * HavannahBoard knows all about the game of Havannah, which is played
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

class HavannahBoard extends hexBoard<HavannahCell> implements BoardProtocol,HavannahConstants
{	static int REVISION = 101;			// 100 represents the initial version of the game
										// revision fixes the definition of a ring
	public int getMaxRevisionLevel() { return(REVISION); }

	static final String[] HEXGRIDSTYLE = { null, "A1", "A1" }; // left and bottom numbers

	HavannahVariation variation = HavannahVariation.havannah_8;
	private HavannahState board_state = HavannahState.Puzzle;	
	private HavannahState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	private CellStack moveStack = new CellStack();
	public HavannahState getState() { return(board_state); }
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(HavannahState st) 
	{ 	unresign = (st==HavannahState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

    private HavannahId playerColor[]={HavannahId.White_Chip_Pool,HavannahId.Black_Chip_Pool};
    
    private HavannahChip playerChip[]={HavannahChip.White,HavannahChip.Black};
    private HavannahCell playerCell[]=new HavannahCell[2];
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public HavannahChip getPlayerChip(int p) { return(playerChip[p]); }
	public HavannahId getPlayerColor(int p) { return(playerColor[p]); }
	public HavannahCell getPlayerCell(int p) { return(playerCell[p]); }
	public HavannahChip getCurrentPlayerChip() { return(playerChip[whoseTurn]); }

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
    int chips_on_board = 0;			// number of chips currently on the board
    private int fullBoard = 0;				// the number of cells in the board
    private int sweep_counter=0;			// used when scanning for blobs
    private int startingPlayer = 0;
    private boolean swapped = false;
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public HavannahChip pickedObject = null;
    public HavannahChip lastPicked = null;
    private HavannahCell blackChipPool = null;	// dummy source for the chip pools
    private HavannahCell whiteChipPool = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    
    private CellStack emptyCells=new CellStack();
    private HavannahState resetState = HavannahState.Puzzle; 
    public DrawableImage<?> lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public HavannahCell newcell(char c,int r)
	{	return(new HavannahCell(HavannahId.BoardLocation,c,r));
	}
	// constructor 
    public HavannahBoard(String init,int players,long key,int []map,int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = HEXGRIDSTYLE;
        setColorMap(map, players);
		Random r = new Random(734687);	// this random is used to assign hash values to cells, common to all games of this type.
	    blackChipPool = new HavannahCell(r,HavannahId.Black_Chip_Pool);
	    blackChipPool.addChip(HavannahChip.Black);
	    whiteChipPool = new HavannahCell(r,HavannahId.White_Chip_Pool);
	    whiteChipPool.addChip(HavannahChip.White);
        doInit(init,key,players,rev); // do the initialization
    }
    
    public String gameType() { return(gametype+" "+players_in_game+" "+randomKey+" "+revision); }
    

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
		setState(HavannahState.Puzzle);
		variation = HavannahVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		moveStack.clear();
		gametype = gtype;
		
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case havannah_6:
		case havannah_8:
		case havannah_10:		
			reInitBoard(variation.firstInCol,variation.ZinCol,null);
		  	setBorderDirections();	// mark the border cells for use in painting
		}

 		
	    startingPlayer = 0;
 	    playerCell[FIRST_PLAYER_INDEX] = whiteChipPool; 
	    playerCell[SECOND_PLAYER_INDEX] = blackChipPool; 
	    
	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    chips_on_board = 0;
	    droppedDestStack.clear();
	    pickedSourceStack.clear();
	    stateStack.clear();
	    
	    pickedObject = null;
	    resetState = null;
	    lastDroppedObject = null;
		playerColor[0]=HavannahId.White_Chip_Pool;
		playerColor[1]=HavannahId.Black_Chip_Pool;
		playerChip[0]=HavannahChip.White;
		playerChip[1]=HavannahChip.Black;
	    // set the initial contents of the board to all empty cells
		emptyCells.clear();
		for(HavannahCell c = allCells; c!=null; c=c.next) { c.reInit(); emptyCells.push(c); }
		fullBoard = emptyCells.size();
        animationStack.clear();
        if(getColorMap()[0]!=0) { swapDetails(); }
        swapped = false;
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }


    /** create a copy of this board */
    public HavannahBoard cloneBoard() 
	{ HavannahBoard dup = new HavannahBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((HavannahBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(HavannahBoard from_b)
    {
        super.copyFrom(from_b);
        chips_on_board = from_b.chips_on_board;
        startingPlayer = from_b.startingPlayer;
        fullBoard = from_b.fullBoard;
        robotState.copyFrom(from_b.robotState);
        getCell(emptyCells,from_b.emptyCells);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        stateStack.copyFrom(from_b.stateStack);
        getCell(moveStack,from_b.moveStack);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        lastPicked = null;

        AR.copy(playerColor,from_b.playerColor);
        AR.copy(playerChip,from_b.playerChip);
 
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((HavannahBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(HavannahBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(startingPlayer==from_b.startingPlayer,"starting player mismatch");
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor mismatch");
        G.Assert(AR.sameArrayContents(playerChip,from_b.playerChip),"playerChip mismatch");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(chips_on_board == from_b.chips_on_board,"chips_on_board mismatch");
        G.Assert(sameCells(moveStack,from_b.moveStack),"movestack mismatch");

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
		v ^= Digest(r,moveStack);
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn+100*startingPlayer);
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
    //
    // flood fill to make this blob as large as possible.  This is more elaborate
    // than it needs to be for scoring purposes, but it is also used by the robot
    //
    private void expandHexBlob(HavannahBlob blob,HavannahCell cell)
    {	if(cell==null) {}
    	else if((cell.sweep_counter!=sweep_counter))
    	{
    	cell.sweep_counter = sweep_counter;
    	cell.blob = blob;
    	     	
    	if(cell.topChip()==blob.color)
    	  {
    	   	blob.addCell(cell);
    	   	for(int dir = 0; dir<6; dir++)
    		{	expandHexBlob(blob,cell.exitTo(dir));
    		}
    	  }
    	}
    	else if(cell.topChip()==null)
    	{	// cell was previously encountered on this sweep
    		HavannahBlob other = cell.blob;
    		if((other!=blob)&&(other.color==blob.color))
    		{	// a connection
    			other.addConnection(blob,cell);
    			blob.addConnection(other,cell);
    		}
    	}
    }

    
    //
    // flood fill to make this blob as large as possible.  This is more elaborate
    // than it needs to be for scoring purposes, but it is also used by the robot
    //  OStack<HavannahBlob> findBlobs(int forplayer,OStack<HavannahBlob> all)
    BlobStack findBlobs(int forplayer,BlobStack all)
    {	sweep_counter++;
    	HavannahChip pch = playerChip[forplayer];
    	for(HavannahCell cell = allCells;  cell!=null; cell=cell.next)
    	{	if((cell.sweep_counter!=sweep_counter) && (cell.topChip()==pch))
    		{
    		HavannahBlob blob = new HavannahBlob(pch);
    		all.push(blob);
    		expandHexBlob(blob,cell);
     		}
    	}
       	return(all);
    }


    // flood fill the blob to make it as large as possible, but don't
    // look for connections.  This is used when the blob os only being
    // checked as a possible win
    private void expandBlobForWin(HavannahBlob blob,HavannahCell cell)
    {	if(cell==null) {}
    	else if((cell.sweep_counter!=sweep_counter))
    	{
      	if(cell.topChip()==blob.color)
    	  {
        	cell.sweep_counter = sweep_counter;
    	   	blob.addCell(cell);
    	   	cell.blob = blob;
    	   	for(int dir = 0; dir<6; dir++)
    		{	expandBlobForWin(blob,cell.exitTo(dir));
    		}
    	  }
    	}
    }
    
    // scan blobs only connected to the home row for the player
    // this is the fast version that only checks for a win
    private boolean findWinningBlob(int player)
    {
    	HavannahChip pch = playerChip[player];
    	sweep_counter++;
    	for(HavannahCell home=allCells; home!=null; home = home.next)
    	{	
       	if((home.topChip()==pch) && (home.sweep_counter!=sweep_counter))
        {
        	HavannahBlob blob = new HavannahBlob(pch);
        	if(buildBlobForWin(blob,home)) 
        		{ return(true); }
        }
     	}    	
    	return(false);
    }
    public boolean buildBlobForWin( HavannahBlob blob,HavannahCell home)
    {
    	expandBlobForWin(blob,home);
    	blob.checkForRing(sweep_counter,revision>=101);
    	return blob.isWin();
    }
    public boolean gameOverNow() { return(board_state.GameOver()); }
    public boolean winForPlayerNow(int player)
    {	if(win[player]) { return(true); }
    	boolean win = findWinningBlob(player);
    	return(win);
    }
    // this method is also called by the robot to get the blobs as a side effect
    public boolean winForPlayerNow(int player,BlobStack blobs)
    {
     	findBlobs(player,blobs);
    	return(someBlobWins(blobs,player));
   	
    }

    public boolean someBlobWins(BlobStack blobs,int player)
    {	// if the span of any blobs is the whole board, we have a winner
    	// in Hex, there is only one winner.
    	for(int i=0;i<blobs.size(); i++)
    	{	HavannahBlob blob = blobs.elementAt(i);
    		boolean win = blob.isWin();
    		if(win)	{ return(true); }
     	}
        return (false);
    }


    private int previousPlaced = 0;
    
    // set the contents of a cell, and maintain the books
    // this logic uses ch==null to clear the cell, needs
    // to be adjusted if HavannahCell is based on stackCell
    public HavannahChip SetBoard(HavannahCell c,HavannahChip ch)
    {	HavannahChip old = c.chip;
    	if(c.onBoard)
    	{
    	if(old!=null) { chips_on_board--;emptyCells.push(c); moveStack.pop(); c.lastPlaced = previousPlaced; }
     	if(ch!=null) { chips_on_board++; emptyCells.remove(c,false); moveStack.push(c); previousPlaced = c.lastPlaced; c.lastPlaced = moveNumber; }
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
    private HavannahCell unDropObject()
    {	HavannahCell rv = droppedDestStack.pop();
    	setState(stateStack.pop());
    	pickedObject = SetBoard(rv,null); 	// SetBoard does ancillary bookkeeping
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	HavannahCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	SetBoard(rv,pickedObject);
    	pickedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(HavannahCell c)
    {
       droppedDestStack.push(c);
       stateStack.push(board_state);
       
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("not expecting dest %s", c.rackLocation);
        case Black_Chip_Pool:
        case White_Chip_Pool:		// back in the pool, we don't really care where
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
    public boolean isDest(HavannahCell c)
    {	return(droppedDestStack.top()==c);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { HavannahChip ch = pickedObject;
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
    private HavannahCell getCell(HavannahId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
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
    public HavannahCell getCell(HavannahCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(HavannahCell c)
    {	pickedSourceStack.push(c);
    	stateStack.push(board_state);
        switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting rackLocation %s", c.rackLocation);
        case BoardLocation:
        	{
            lastPicked = pickedObject = c.topChip();
         	lastDroppedObject = null;
			SetBoard(c,null);
        	}
            break;

        case Black_Chip_Pool:
        case White_Chip_Pool:
        	lastPicked = pickedObject = c.topChip();	// add a copy, don't remove it
        }
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(HavannahCell c)
    {	return(c==pickedSourceStack.top());
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case Confirm:
        	setNextStateAfterDone(replay);
         	break;
        case Play:
        case PlayOrSwap:
			setState(HavannahState.Confirm);
			break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    private void setNextStateAfterDone(replayMode replay)
    {	G.Assert(chips_on_board+emptyCells.size()==fullBoard,"cells missing");
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state %s",board_state);
    	case Gameover: break;
    	case ConfirmSwap: 
    		setState(HavannahState.Play); 
    		break;
    	case Confirm:
    	case Puzzle:
    	case Play:
    	case PlayOrSwap:
    		setState(((chips_on_board==1)&&(whoseTurn!=startingPlayer)&&!swapped) 
    				? HavannahState.PlayOrSwap
    				: HavannahState.Play);
    		
    		break;
    	}
       	resetState = board_state;
    }
    private void doDone(replayMode replay)
    {
        acceptPlacement();

        if (board_state==HavannahState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
            setState(HavannahState.Gameover);
        }
        else
        {	if(winForPlayerNow(whoseTurn)) 
        		{ win[whoseTurn] = true;
        		  setState(HavannahState.Gameover);
        		}
        	else if(emptyCells.size()==0) { setState(HavannahState.Gameover); }
        	else {setNextPlayer(replay);
        		  setNextStateAfterDone(replay);
        	}
        }
    }
private void swapDetails()
{
	HavannahId c = playerColor[0];
	HavannahChip ch = playerChip[0];
	playerColor[0]=playerColor[1];
	playerChip[0]=playerChip[1];
	playerColor[1]=c;
	playerChip[1]=ch;
	HavannahCell cc = playerCell[0];
	playerCell[0]=playerCell[1];
	playerCell[1]=cc;
	swapped = !swapped;
}
void doSwap(replayMode replay)
{	swapDetails();
	switch(board_state)
	{	
	default: 
		throw G.Error("Not expecting swap state %s",board_state);
	case Play:
		// some damaged game records have double swap
		if(replay==replayMode.Live) { G.Error("Not expecting swap state %s",board_state); }
		//$FALL-THROUGH$
	case PlayOrSwap:
		  setState(HavannahState.ConfirmSwap);
		  break;
	case ConfirmSwap:
		  setState(HavannahState.PlayOrSwap);
		  break;
	case Gameover:
	case Puzzle: break;
	}
	}

	
    public boolean Execute(commonMove mm,replayMode replay)
    {	HavannahMovespec m = (HavannahMovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }

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
			HavannahChip po = pickedObject;
			HavannahCell src = getCell(m.source,m.to_col,m.to_row); 
			HavannahCell dest =  getCell(HavannahId.BoardLocation,m.to_col,m.to_row);
			
			if(isSource(dest)) 
				{ unPickObject(); 
				}
				else 
				{
				switch(board_state)
				{
				case Puzzle:	
					acceptPlacement(); 
					break;
				default:
					// drop twice in a row, undo the previous drop.  This is peculiar to havannah.
					// where we allow you to change your mind by just clicking somewhere else.
					if(droppedDestStack.size()>0) { unDropObject(); } 
					if(pickedSourceStack.size()>0) { unPickObject(); }
					break;
				}
				
				pickObject(src);
				if(replay==replayMode.Live)
				{
		            lastDroppedObject = pickedObject.getAltDisplayChip(dest);
				}
	            dropObject(dest);
	            /**
	             * if the user clicked on a board space without picking anything up,
	             * animate a stone moving in from the pool.  For Hex, the "picks" are
	             * removed from the game record, so there are never picked stones in
	             * single step replays.
	             */
	            if(replay!=replayMode.Replay && (po==null))
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
 			HavannahCell src = getCell(m.source,m.to_col,m.to_row);
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
        		setState(((chips_on_board==1) && !swapped) ? HavannahState.PlayOrSwap : HavannahState.Play);
        		break;
        	default: ;
        	}}}
            break;

        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
            HavannahCell dest = getCell(m.source,m.to_col,m.to_row);
            if(isSource(dest)) { unPickObject(); }
            else { dropObject(dest); }
        	}
            break;
 
        case MOVE_START:
            setWhoseTurn(startingPlayer = m.player);
            acceptPlacement();
            int nextp = nextPlayer[whoseTurn];
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(HavannahState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ setState(HavannahState.Gameover); 
               	}
            else {  setNextStateAfterDone(replay); }

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?HavannahState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(HavannahState.Puzzle);
 
            break;
        case MOVE_GAMEOVERONTIME:
        	win[whoseTurn] = true;
        	setState(HavannahState.Gameover);
        	break;

        default:
        	cantExecute(m);
        }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting Legal Hit state %s", board_state);
        case PlayOrSwap:
        case Play:
        	// you can pick up a stone in the storage area
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

    public boolean LegalToHitBoard(HavannahCell c)
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
			return(isDest(c) || c.isEmpty());
        default:
        	throw G.Error("Not expecting Hit Board state %s", board_state);
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
    public void RobotExecute(HavannahMovespec m)
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
    public void UnExecute(HavannahMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	HavannahState state = robotState.pop();
        switch (m.op)
        {
   	    default:
   	    	throw G.Error("Can't un execute %s", m);
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
  
 public commonMove Get_Localrandom_Hex_Move(Random rand)
 {	HavannahCell last = moveStack.top();
	if(last!=null)
	{	HavannahChip top = last.topChip();
		double rv = rand.nextDouble();
		if(last.isPossibleBridge(top))
		{
		int count = 0;
		HavannahCell bridge = null;
		HavannahCell bridge2 = null;
		HavannahCell bridge3 = null;
		if(rv<0.99)
		{
		for(int dir = last.geometry.n-1; dir>=0; dir--)
		{
			HavannahCell c = last.exitTo(dir);
			if((c!=null) && c.isEmpty() && c.isPossibleBridge(top))
			{ count++; 
			  bridge3 = bridge2;
			  bridge2 = bridge;
			  bridge = c;
			}
		}
		switch(count)
		{
		case 0: break;
		default:
			{
			double r2 = rand.nextInt(count);
			for(int dir = last.geometry.n-1; dir>=0; dir--)
			{
				HavannahCell c = last.exitTo(dir);
				if((c!=null)&&c.isEmpty()&&c.isPossibleBridge(top))
				{ if(r2==0) 
					{ return(new HavannahMovespec(MOVE_DROPB,c.col,c.row,playerColor[whoseTurn],whoseTurn));
					}
				  r2--;
				}
			}}
			break;
		case 3:
			if(rv<0.66) { bridge = bridge3; }
			//$FALL-THROUGH$
		case 2:
			if(rv<0.33) { bridge = bridge2; }
			//$FALL-THROUGH$
		case 1:
			return(new HavannahMovespec(MOVE_DROPB,bridge.col,bridge.row,playerColor[whoseTurn],whoseTurn));
		}}
		}
		else if(rv<0.66)
		{
			// play close
			char col = (char)(last.col+rand.nextInt(5)-2);
			int row = last.row+rand.nextInt(5)-2;
			HavannahCell c = getCell(col,row);
			if(c!=null && (c.topChip()==null))
			{
				new HavannahMovespec(MOVE_DROPB,col,row,playerColor[whoseTurn],whoseTurn);
			}
		}
		
	}
	return(Get_Random_Hex_Move(rand)); 
}
 public commonMove Get_Random_Hex_Move(Random rand)
 {		int sz = emptyCells.size();
 		int off = Random.nextInt(rand,sz);
 		HavannahCell empty = emptyCells.elementAt(off);
 		G.Assert(empty.isEmpty(),"isn't empty");
 		return(new HavannahMovespec(MOVE_DROPB,empty.col,empty.row,playerColor[whoseTurn],whoseTurn));
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	if(board_state==HavannahState.PlayOrSwap)
 	{
 		all.addElement(new HavannahMovespec(SWAP,whoseTurn));
 	}
 	for(HavannahCell c = allCells;
 	    c!=null;
 	    c = c.next)
 	{	if(c.isEmpty())
 		{all.addElement(new HavannahMovespec(MOVE_DROPB,c.col,c.row,playerColor[whoseTurn],whoseTurn));
 		}
 	}

 	return(all);
 }
 
 public void initRobotValues()
 {
	 for(int lim = emptyCells.size()-1; lim>=0; lim--)
	 {
		 emptyCells.elementAt(lim).initRobotValues();
	 }
 }
 
 // precompute which border cell decorations needs to be drawn 
 // this is peculiar to the way we draw the borders of the havannah board
 // not a general game requirement.
 private void setBorderDirections()
 {	for(HavannahCell c = allCells;
 		c!=null;
 		c = c.next)
 	{	c.setBorderMask(c.borderDirectionMask());
 	}
 }
 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   int n = G.IntToken(txt.substring(1));
 	 if(n>1)
	 	{ //int n = G.intToken(txt);
 		  int ch = txt.charAt(0)-'A';
 		  if(ch>ncols/2) { ypos -= cellsize/4; }
	 			xpos -= cellsize/2;
	 	}
 		else
 		{ 
 		  ypos += cellsize/4;
 		}
 	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
 }

 class UCTProxy implements Comparable<UCTProxy>
 {
	 double uct;
	 commonMove move; 
	 UCTProxy(double v,commonMove m)
	 {
		 uct = v;
		 move = m;
	 }
	public int compareTo(UCTProxy o) {
		return(G.signum(uct-o.uct));
	}
 };
 class UCTProxyStack extends OStack<UCTProxy> 
 {
	public UCTProxy[] newComponentArray(int sz) {
		return(new UCTProxy[sz]);
	}
 }
 // interface with UCT searcher to generate weighted random moves
 public UCTProxyStack[] proxyStack = null;
 public void buildUCTtree(UCTMoveSearcher ss,UCTNode n,double beta)
 {	UCTNode blackNode = null;
 	UCTNode whiteNode = n;
 	UCTNode parent;
 	Random rand = ss.rand;
 	reclaimUCTtree(ss,n);
 	// find the root of the UCT tree.  This will
 	// give us a pair of black and white nodes 
 	while( (parent = whiteNode.getParent())!=null)
 	{
 		blackNode = whiteNode;
 		whiteNode = parent;
 	}
 	if(blackNode!=null && whiteNode!=null)
 	{
 		UCTProxyStack btree = buildUCTtreeFor(ss,blackNode,rand,beta);
 		UCTProxyStack wtree = buildUCTtreeFor(ss,whiteNode,rand,beta);
 		if((btree!=null) && (wtree!=null))
 		{
 		int bplayer = blackNode.getPlayer();
 		int wplayer = whiteNode.getPlayer();
 		if(bplayer>=0 && wplayer>=0)
 		{
 		proxyStack = new UCTProxyStack[2];
 		proxyStack[bplayer]=btree;
 		proxyStack[wplayer]=wtree;
 		}}
 	}
 }
 public UCTProxyStack buildUCTtreeFor(UCTMoveSearcher ss,UCTNode n,Random rand,double beta)
 {
	 int nc = n.getNoOfChildren();
	 if(nc>0)
	 {
	 UCTProxyStack stack = new UCTProxyStack();
	 HavannahCell last = moveStack.top();
	 double visits = n.getVisits();
	 for(int i=0;i<nc;i++)
	 {
		 HavannahMovespec child = (HavannahMovespec)n.getChild(i);
		 if(child!=null)
		 {	HavannahCell childCell = getCell(child.to_col,child.to_row);
			UCTNode childNode = child.uctNode();
			double val = (last.isAdjacentTo(childCell) ? 0.5 : 0.0) + rand.nextDouble()*beta;
			if(childNode!=null)
			{
			int childVisits = childNode.getVisits();
			if(childVisits>=0)
			{
			// start with the current UCT value for the move, 
			// add a random variation in value
			val += childVisits/visits;
			}}			
			stack.push(new UCTProxy(val,child));
		}
	 }
	 stack.sort();
	 return(stack);
	 }
	 return(null);
 }
 public void reclaimUCTtree(UCTMoveSearcher ss,UCTNode n)
 {	proxyStack = null;
 }
 public commonMove getUCTRandomMove(Random rand)
 {	if(proxyStack!=null)
	 {	UCTProxyStack stack = proxyStack[whoseTurn];
	 	if(stack!=null)
	 	{
	 		while(stack.size()>0)
	 		{
	 			UCTProxy proxy = stack.pop();
	 			commonMove m = proxy.move;
	 			// in general, need to be sure this move is legal
	 			// in the current context. Most likely reason not
	 			// is that it has already been played
	 			if(isStillLegal(m))
	 			{ HavannahMovespec mm = (HavannahMovespec)m.Copy(null);
	 			  // if the previous move was a swap, then all the
	 			  // players are reversed. This is only a factor generating
	 			  // white's second move after a swap.
	 			  mm.player = whoseTurn;
	 			  mm.source = playerColor[whoseTurn];
	 			  return(mm);
	 			}
	 		}
	 	}
	 }
 	// fall back on the old unbiased mode
 	return(Get_Random_Hex_Move(rand));
 }
 private boolean isStillLegal(commonMove m)
 {	HavannahMovespec mm = (HavannahMovespec)m;
 	switch(mm.op)
 	{
 	case MOVE_DROPB:
 		{
 		HavannahCell c = getCell(mm.to_col,mm.to_row);
 		if((c!=null) && c.isEmpty()) 
 			{ return(true); }
 		}
 		break;
 	default: break;
 	}
	 return(false);
 }
}
