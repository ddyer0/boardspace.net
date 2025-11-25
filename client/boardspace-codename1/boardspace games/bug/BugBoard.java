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
package bug;


import static bug.BugMovespec.*;

import bridge.Color;

import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;

/**
 * BugBoard knows all about the game of CircleOfLife, which is played
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

class BugBoard 
	extends hexBoard<BugCell>	// for a square grid board, this could be rectBoard or squareBoard 
	implements BoardProtocol,BugConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
	static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	BugVariation variation = BugVariation.Bug_4;
	private BugState board_state = BugState.Puzzle;	
	private BugState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	private BugStack bugStack = new BugStack();
	public BugState getState() { return(board_state); }
	private BugStack captureStack =  new BugStack();
	private IStack robotStack = new IStack();
	int captureSize[] = new int[2];
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(BugState st) 
	{ 	unresign = (st==BugState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
    private BugId playerColor[]={BugId.Black,BugId.White};    
    private BugChip playerChip[]={BugChip.Black,BugChip.White};
    private BugCell playerCell[]=new BugCell[2];
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public BugChip getPlayerChip(int p) { return(playerChip[p]); }
	public BugId getPlayerColor(int p) { return(playerColor[p]); }
	public BugCell getPlayerCell(int p) { return(playerCell[p]); }
	public BugChip getCurrentPlayerChip() { return(playerChip[whoseTurn]); }
	public BugPlay robot = null;
	public int maxBugSize = 1;
	public boolean p1(String msg)
		{
			if(G.p1(msg) && robot!=null)
			{	String dir = "g:/share/projects/boardspace-html/htdocs/bug/buggames/robot/";
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
	public void SetDrawState() { setState(BugState.Draw); }	CellStack animationStack = new CellStack();
    private int chips_on_board = 0;			// number of chips currently on the board
    private int fullBoard = 0;				// the number of cells in the board

    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public BugChip pickedObject = null;
    public BugChip lastPicked = null;
    private BugCell blackChipPool = null;	// dummy source for the chip pools
    private BugCell whiteChipPool = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    private BugStack latentCaptures[] = { new BugStack(), new BugStack()};
    private BugStack pendingCaptures = new BugStack();
    private BugStack growers = new BugStack();
    
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
    private BugState resetState = BugState.Puzzle; 
    public DrawableImage<?> lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public BugCell newcell(char c,int r)
	{	BugCell cc  = new BugCell(BugId.BoardLocation,c,r);
		cc.myBoard = this;
		return cc;
	}
	
	// constructor 
    public BugBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = GRIDSTYLE;
        setColorMap(map, players);
        
		Random r = new Random(734687);
		// do this once at construction
	    blackChipPool = new BugCell(r,BugId.Black);
	    blackChipPool.addChip(BugChip.Black);
	    whiteChipPool = new BugCell(r,BugId.White);
	    whiteChipPool.addChip(BugChip.White);

        doInit(init,key,players,rev); // do the initialization 
        autoReverseY();		// reverse_y based on the color map
        
        // this is for the side effect of reading the static data into the class
        @SuppressWarnings("unused")
		Bug ini = new Bug(this,BugChip.White);	// prime the pump
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
 		setState(BugState.Puzzle);
		variation = BugVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		gametype = gtype;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case Bug_3:
		case Bug_4:
		case Bug_5:
			// using reInitBoard avoids thrashing the creation of cells 
			// when reviewing games.
			reInitBoard(variation.firstInCol,variation.ZinCol,null);
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
	    captureStack.clear();
	    AR.setValue(captureSize,0);
	    pickedObject = null;
	    resetState = null;
	    maxBugSize = 1;
	    lastDroppedObject = null;
	    int map[]=getColorMap();
		playerColor[map[0]]=BugId.Black;
		playerColor[map[1]]=BugId.White;
		playerChip[map[0]]=BugChip.Black;
		playerChip[map[1]]=BugChip.White;
	    // set the initial contents of the board to all empty cells
		emptyCells.clear();
		for(BugCell c = allCells; c!=null; c=c.next) { c.reInit(); emptyCells.push(c); }
		fullBoard = emptyCells.size();
	    latentCaptures[0].clear();
	    latentCaptures[1].clear();
	    pendingCaptures.clear();
	    growers.clear();
        animationStack.clear();
        moveNumber = 1;

        robotState.clear(); //record the starting state. The most reliable
        bugStack.clear();
        robotStack.clear();
        // note that firstPlayer is NOT initialized here
    }

    /** create a copy of this board */
    public BugBoard cloneBoard() 
	{ BugBoard dup = new BugBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((BugBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(BugBoard from_b)
    {
        super.copyFrom(from_b);
        chips_on_board = from_b.chips_on_board;
        fullBoard = from_b.fullBoard;
        robotState.copyFrom(from_b.robotState);
        getCell(emptyCells,from_b.emptyCells);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        copyFrom(whiteChipPool,from_b.whiteChipPool);		// this will have the side effect of copying the location
        copyFrom(blackChipPool,from_b.blackChipPool);		// from display copy boards to the main board
        getCell(playerCell,from_b.playerCell);
        stateStack.copyFrom(from_b.stateStack);
        captureStack.copyFrom(from_b.captureStack);
        AR.copy(captureSize,from_b.captureSize);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        lastPicked = null;
        maxBugSize = from_b.maxBugSize;
        AR.copy(playerColor,from_b.playerColor);
        latentCaptures[0].deepCopyFrom(this,from_b.latentCaptures[0]);
        latentCaptures[1].deepCopyFrom(this,from_b.latentCaptures[1]);
        AR.copy(playerChip,from_b.playerChip);
        growers.deepCopyFrom(this,from_b.growers);
        if(G.debug()) { sameboard(from_b); }
    }
    public Bug copy(Bug from)
    {
    	Bug b = new Bug(this,from.top);
    	b.copyFrom(from);   	
    	return b;
    }

    public void sameboard(BoardProtocol f) { sameboard((BugBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(BugBoard from_b)
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
        G.Assert(captureStack.eqContents(from_b.captureStack),"captureStack mismatch");
        G.Assert(AR.sameArrayContents(captureSize,from_b.captureSize),"captureSize mismatch");
        G.Assert(maxBugSize==from_b.maxBugSize,"maxBugSize mismatch");
        G.Assert(latentCaptures[0].size()==from_b.latentCaptures[0].size(),"grower[0] mismatch");
        G.Assert(latentCaptures[1].size()==from_b.latentCaptures[1].size(),"grower[1] mismatch");
        G.Assert(growers.size()==from_b.growers.size(),"grower mismatch");
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
		v ^= Digest(r,board_state);
		v ^= Digest(r,whoseTurn);
		v ^= Digest(r,captureSize);
		v ^= Digest(r,captureStack);
		v ^= Digest(r,maxBugSize);
		v ^= Digest(r,latentCaptures);
		v ^= Digest(r,growers);
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
    public boolean winForPlayerNow(int player)
    {	return win[player];
    }


    // set the contents of a cell, and maintain the books
    private int lastPlaced = -1;
    public BugChip SetBoard(BugCell c,BugChip ch)
    {	BugChip old = c.topChip();
    	if(c.onBoard)
    	{
    	if(old!=null) { chips_on_board--;emptyCells.push(c); c.myCritter=null; }
     	if(ch!=null) { chips_on_board++; emptyCells.remove(c,false);  lastPlaced = c.lastPlaced; c.lastPlaced = moveNumber; }
     		else { c.lastPlaced = lastPlaced; }
    	}
       	if(old!=null) { c.removeTop();  }
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
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private BugCell unDropObject()
    {	BugCell rv = droppedDestStack.pop();
    	setState(stateStack.pop());
    	pickedObject = SetBoard(rv,null); 	// SetBoard does ancillary bookkeeping
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	BugCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	SetBoard(rv,pickedObject);
    	pickedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(BugCell c)
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
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(BugCell c)
    {	return(droppedDestStack.top()==c);
    }
    public BugCell getDest()
    {	return(droppedDestStack.top());
    }
 
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { BugChip ch = pickedObject;
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
    private BugCell getCell(BugId source, char col, int row)
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

    //
    // these are semi-magic numbers derived by dissecting celtoX00 and cellTOY00 for a maximimally large board
    // and     public double yCellRatio() { return(1.1541); }
    // these yield distances from center that are the same for radially symmertic cells.
    //
     int cellToXint(char colchar, int row)
    {	
        int col = geo_colnum(colchar);
        int col2 = ((col+1) * 65823);
        return col2;
    }
     int cellToYint(char colchar, int arow)
    {	
        int col = geo_colnum(colchar);
        int thisrow = geo_rownum(colchar,arow);
        int coffset = firstRowOffset(col);
        int y0 = ((thisrow - firstRowInCol[(col+ncols)%ncols] ) * 75966)
        		+ (coffset * 38003)
        		;      
        return (y0);
     }
    public int distanceSquared(BugCell from,BugCell to)
    {
    	int x0 = cellToXint(from.col,from.row);
    	int y0 = cellToYint(from.col,from.row);
    	int x1 = cellToXint(to.col,to.row);
    	int y1 = cellToYint(to.col,to.row);
    	long dx = (x0-x1);
    	long dy = (y0-y1);
    	double distance = (dx*dx+dy*dy)/10000000000.0;
    	if((int)(distance+0.01) != (int)distance) 
    	{
    		G.print("close "+distance);
    		distance += 0.01;
    	}
    	return (int)(distance);
    }
    public String distance(BugCell from,BugCell to)
    {
    	String v = ""+distanceSquared(from,to);
    	return v;
    }	
    /**
     * this is called when copying boards, to get the cell on the new (ie current) board
     * that corresponds to a cell on the old board.
     */
    public BugCell getCell(BugCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(BugCell c)
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
    public boolean isSource(BugCell c)
    {	return(c==pickedSourceStack.top());
    }
    public BugCell getSource()
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
        case Play:
        case Grow:
 			setState(BugState.Confirm);
			break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }

    private void captureCritter(Bug dest,replayMode replay)
    {
    	captureStack.push(dest);
    	captureSize[whoseTurn] += dest.size();
    	for(int lim=dest.size()-1; lim>=0; lim--)
    	{
    		BugCell cap = dest.elementAt(lim);
    		SetBoard(cap,null);
    		if(replay.animate)
    		{
    			animationStack.push(cap);
    			animationStack.push(playerCell[nextPlayer[whoseTurn]]);
    		}
    	}
    }
    private void unCapture()
    {
    	Bug captured = captureStack.pop();
    	captureSize[whoseTurn] -= captured.size();
    	for(int lim=captured.size()-1; lim>=0; lim--)
    	{
    		BugCell cap = captured.elementAt(lim);
    		SetBoard(cap,captured.top);
    		cap.myCritter = captured;
    	}
    }
    private boolean doCaptures(Bug myGroup,BugStack growers, BugStack otherGrowers,BugStack captured)
    {	boolean some = false;
      	BugChip top = myGroup.top;
      	String captureType = myGroup.rawIdentity;
    	for(int lim=myGroup.size()-1; lim>=0; lim--)
    	{
     		BugCell c = myGroup.elementAt(lim);
    		for(int dir = 0; dir<6; dir++)
    		{	BugCell adj = c.exitTo(dir);
				if(adj!=null)
				{
    			Bug adjCritter = adj.critter(this);
				if(adjCritter!=null 
						&& adjCritter.top != top
						&& adjCritter.rawIdentity.equals(captureType))
					{
						captured.pushNew(adjCritter);
						otherGrowers.pushNew(adjCritter);
					some = true;
					}
				}
    		}
  	
    	}
    	if(some) { growers.pushNew(myGroup); }
    	return some;
    }
    private boolean doCaptures(Bug myGroup,BugMovespec m,replayMode replay,BugStack noCaptures)
    {	
    	boolean some = false;
    	if(myGroup!=null) //this prevents a nullpointerexception, but something is already wrong; there must be a critter
    	{
    	String captureType = myGroup.rawIdentity;
    	BugChip top = myGroup.top;
    	for(int lim=myGroup.size()-1; lim>=0; lim--)
    	{
    		BugCell c = myGroup.elementAt(lim);
    		for(int dir = 0; dir<6; dir++)
    		{
    			BugCell adj = c.exitTo(dir);
    			if(adj!=null)
    			{
    				Bug adjCritter = adj.critter(this);
    				if(adjCritter!=null 
    						&& adjCritter.top != top
    						&& adjCritter.rawIdentity.equals(captureType))
    				{	
    				if(noCaptures!=null)
    					{
    					noCaptures.remove(adjCritter,false);
    					}
    				captureCritter(adjCritter,replay);
					some = true;
					adjCritter.next = m.critter;
					m.critter = adjCritter;
					//m.critter = m.critter==null ? (StackIterator)adjCritter : (m.critter).push(adjCritter);
    				}}
    			}
    		}
    	}
    	if(some) { growers.push(myGroup); }
    	return some;
    }
    
    private boolean newway = true;
    int step = 0;
    private void doDoneNewWay(BugCell dest,BugMovespec m,replayMode replay)
    {	step++;
    	// store the new latentCaptures and capturees
    	BugStack otherPossibleCaptures = latentCaptures[whoseTurn^1];
    	BugStack possibleCaptures= latentCaptures[whoseTurn];
    	//G.print("g ",grow," o ",otherGrow," x ",growers);
    	boolean cap = false;
    	
    	if(possibleCaptures.size()>0)
    	{
    		for(int lim=possibleCaptures.size()-1; lim>=0; lim--)
    		{	// enlarged it, no longer a latent growth candidate
    			Bug gr = possibleCaptures.elementAt(lim);
    			if(gr.isAdjacent(dest)) { possibleCaptures.remove(lim,false); }
    		}
    	}
    	
    	if(growers.size()>0)
    	{
    		// must have been grow state, so dest must be adjacent to just one.
    		for(int lim=growers.size()-1; lim>=0; lim--)
    		{
    			Bug grower = growers.elementAt(lim);
    			if(grower.isAdjacent(dest))
    			{
    				growers.remove(lim,false);
    				possibleCaptures.remove(grower);
    			}
    		}

    	}
    	// collect all the potentially captured bugs
    	pendingCaptures.clear();
    	for(int lim=possibleCaptures.size()-1;lim>=0; lim--) 
    		{ boolean thiscap = doCaptures(possibleCaptures.elementAt(lim),possibleCaptures,otherPossibleCaptures,pendingCaptures); 
     		  cap |= thiscap;
     		  if(!thiscap)
    		  {		// no longer a capture candidate
    			  possibleCaptures.remove(lim,false);
    		  }
    		}   	
        cap |= doCaptures(dest.critter(this),possibleCaptures,otherPossibleCaptures,pendingCaptures);
        
        if(cap)
        {	// if there are some, mark them all as empty and look for growth opportunities
        	// in each of the capturing cells.
        	cap = false;
        	m.critter = null;
        	for(BugCell c = allCells; c!=null; c=c.next)
        	{
        		c.designatedAsEmpty = false;
        	}
        	
        	for(int i=0;i<pendingCaptures.size(); i++)
        	{
        		Bug bug = pendingCaptures.elementAt(i);
        		for(int j = 0; j<bug.size(); j++)
        		{
        			bug.elementAt(j).designatedAsEmpty=true;
        		}
        	}
        	for(int lim = possibleCaptures.size()-1; lim>=0; lim--)
        	{	Bug gr = possibleCaptures.elementAt(lim);
        		gr.canGrow = addGrowMoves(null,gr,whoseTurn);
        	}
        	for(int lim = possibleCaptures.size()-1; lim>=0; lim--)
        	{	Bug gr = possibleCaptures.elementAt(lim);
       			if(gr.canGrow)
       			{
    			// really do it 
    			cap |= doCaptures(gr,m,replay,otherPossibleCaptures);
    			growers.pushNew(gr);
    			possibleCaptures.remove(lim,false);
       			}
        	}
         	for(int i=0;i<pendingCaptures.size(); i++)
        	{
        		Bug bug = pendingCaptures.elementAt(i);
        		for(int j = 0; j<bug.size(); j++)
        		{
        			bug.elementAt(j).designatedAsEmpty=false;
        		}
        	}
        	
        }
       
        if(growers.size()>0 && addGrowMoves(null,whoseTurn)) { setState(BugState.Grow); }
        else 
        { // when no more growth is possible, remove any remaining even if they haven't grown
          // this handles the unusual case where multiple groups had to grow, but one grew in
          // a way that precludes another from growing.
          growers.clear();
          setNextPlayer(replay);
          setState(BugState.Play);
        }
    	//G.print("eg ",grow," o ",otherGrow," x ",growers);
    	

        if(!hasMoves()) 
    	{ win[whoseTurn]=true; 
    	  setState(BugState.Gameover); 
    	}
    }
    
    
	private void doDestOldWay(BugCell dest,BugMovespec m,replayMode replay)
	{
		boolean cap = doCaptures(dest.critter(this),m,replay,null); 
	        if (board_state==BugState.Resign)
	        {
	            win[nextPlayer[whoseTurn]] = true;
	    		setState(BugState.Gameover);
	        }
	        else
	        {
	        if (cap ) 
	        { 
	          if(addGrowMoves(null,whoseTurn))
	          {
	              setState(BugState.Grow);
	          }
	          else
	          {
	        	  cap = false;
	          }
	        }
	        if(!cap)
	        {	
	        	setNextPlayer(replay);
	        	setState(BugState.Play);
	        }
	        if(!hasMoves()) 
	        	{ win[whoseTurn]=true; 
	        	  setState(BugState.Gameover); 
	        	}

	        }
	}
	
    private void doDone(BugMovespec m,replayMode replay)
    {	BugCell dest = getDest();
        acceptPlacement();
        if(dest!=null)
        {
	        if(newway)
	        {	doDoneNewWay(dest,m,replay);
	        }
	        else
	        {	doDestOldWay(dest,m,replay);   
	        }
        }
    }

    public boolean Execute(commonMove mm,replayMode replay)
    {	BugMovespec m = (BugMovespec)mm;
        if(replay.animate) { animationStack.clear(); }

        //G.print("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone(m,replay);

            break;

        case MOVE_DROPB:
        	{
			BugChip po = pickedObject;
			BugCell dest =  getCell(BugId.BoardLocation,m.to_col,m.to_row);
			BugCell src = getSource();
			if(src==null) 
				{ src = getCell(playerColor[whoseTurn],'@',0); 
				  pickObject(src);
				}
			if(isSource(dest)) 
				{ unPickObject(); 
				}
				else 
				{		
				if(pickedObject==null) { unDropObject(); }
	            dropObject(dest);
            	Bug bug = m.critter = dest.critter(this);  
            	maxBugSize = Math.max(maxBugSize,bug.size());
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
        	{
        	BugCell src = getCell(m.source,m.to_col,m.to_row);
        	pickObject(src);
        	}
        	break;
 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
 			BugCell src = getCell(m.source,m.to_col,m.to_row);
 			Bug cr = src.critter(this);
 			cr.forget();
 			if(isDest(src)) { unDropObject(); }
 			else
 			{
        	// be a temporary p
        	pickObject(src);
        	src.myCritter = null;
        	switch(board_state)
        	{
        	case Puzzle:
         		break;
        	case Confirm:
        		setState(BugState.Play);
        		break;
        	default: ;
        	}}
 			cr.remember(this);
 			}
            break;

        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
            BugCell dest = getCell(m.source,m.to_col,m.to_row);
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
            setState(BugState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ setState(BugState.Gameover); 
               	}
            else {  setState(BugState.Play); }

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?BugState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(BugState.Puzzle);
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(BugState.Gameover);
    	   break;

        default:
        	cantExecute(m);
        }

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
        case Grow:
        	// for pushfight, you can pick up a stone in the storage area
        	// but it's really optional
        	return(player==whoseTurn);
        case Confirm:
		case Resign:
		case Draw:
		case Gameover:
			return(false);
        case Puzzle:
            return ((pickedObject!=null)?(pickedObject==playerChip[player]):true);
        }
    }

    public boolean legalToHitBoard(BugCell c,Hashtable<BugCell,BugMovespec> targets )
    {	if(c==null) { return(false); }
        switch (board_state)
        {
        case Puzzle:
        	return (pickedObject==null)!=(c.topChip()==null);
		case Play:
		case Grow:
			return(targets.get(c)!=null || isDest(c) || isSource(c));
		case Gameover:
		case Draw:
		case Resign:
			return(false);
		case Confirm:
			return(isDest(c) || c.isEmpty());
        default:
        	throw G.Error("Not expecting Hit Board state " + board_state);
        }
    }
    
     
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(BugMovespec m)
    {	//System.out.println("R "+m+" for "+whoseTurn);
        robotState.push(board_state); //record the starting state. The most reliable
        robotStack.push(latentCaptures[whoseTurn].size());
        for(int lim=latentCaptures[whoseTurn].size()-1; lim>=0; lim--) 
        	{ bugStack.push(latentCaptures[whoseTurn].elementAt(lim)); 
        	}
        robotStack.push(maxBugSize);
        robotStack.push(captureStack.size());
       
        Execute(m,replayMode.Replay);
        doDone(m,replayMode.Replay);
    }
    public void UnExecute(BugMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
     	switch (m.op)
        {
        default:
   	    	throw G.Error("Can't un execute " + m);
        case MOVE_RESIGN:
        case MOVE_DONE:
            break;
            
        case MOVE_DROPB:
        	SetBoard(getCell(m.to_col,m.to_row),null);
        	break;
        }
        setState(robotState.pop());
       	int caps = robotStack.pop();
       	maxBugSize = robotStack.pop();
       	int sz = robotStack.pop();
 
       	if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }

       	latentCaptures[whoseTurn].clear();
       	for(int i=0;i<sz;i++) { latentCaptures[whoseTurn].push(bugStack.pop()); }

        while(caps<captureStack.size())
       	{
       		unCapture();
       	}

    }
 private boolean addLegalMoves(CommonMoveStack all,int who)
 {
	 CellStack empty = emptyCells;
	 boolean some = false;
	 BugChip top = getPlayerChip(who);
	 for(int lim = empty.size()-1; lim>=0; lim--)
	 {
		 BugCell c = empty.elementAt(lim);
		 some |= addLegalMoves(all,c,top,who);
		 if(some && all==null) 
		 	{ return true; }
	 }
	 return some;
 }
 
 private boolean addGrowMoves(CommonMoveStack all,int who)
 {	boolean some = false;
 	for(int lim=growers.size()-1; lim>=0; lim--)
	 {
		 some |= addGrowMoves(all,growers.elementAt(lim),who);
		 if(some&&all==null) { return true; }

	 }
 	return some;
 }
 int sweepCounter = 1;
 private boolean addGrowMoves(CommonMoveStack all,Bug eater,int who)
 {	boolean some = false;
 	sweepCounter++;
 	int sweep = sweepCounter;
	BugChip top = getPlayerChip(who);
	int sz = eater.size();
 	maxBugSize = Math.max(sz+1,maxBugSize);
	for(int lim=sz-1; lim>=0; lim--)
	{
		BugCell c = eater.elementAt(lim);
		for(int direction = 0; direction<CELL_FULL_TURN; direction++)
		{
			BugCell adj = c.exitTo(direction);
			if(adj!=null && adj.sweep_counter!=sweep && (adj.designatedAsEmpty || adj.topChip()==null ))
			{
				adj.sweep_counter=sweep;
				some |= addLegalMoves(all,adj,top,whoseTurn);
				if(some && all==null) { return some; }
			}
		}
	}
	 return some;
 }
 private boolean addLegalMoves(CommonMoveStack all,BugCell c,BugChip top,int who)
 {	
	 if(isLegalMove(c,top))
 		{
	 	  if(all==null) { return true; }
	 	  //if(robot!=null) { verifyBlobSize(c); }
	 	  all.push(new BugMovespec(MOVE_DROPB,c.col,c.row,who));
	 	  return true;
	 	}
 	return false;
 }
 // legal if empty and makes a blob of maxBugSize or less
 public boolean isLegalMove(BugCell c,BugChip top)
 {
		 Bug adjBug = null;
		 for(int dir = 0;dir<CELL_FULL_TURN; dir++)
		 {
			 BugCell adj = c.exitTo(dir);
			 if(adj!=null)
			 {
			 Bug cr = adj.designatedAsEmpty ? null : adj.critter(this);
			 if(cr==null) {}
			 else if(cr.top!=top) {}
			 else if(cr==adjBug) {}								// seen it already
			 else if(adjBug!=null) { return false; }			// saw someting else, merges bugs
			 else if(cr.size()>=maxBugSize) { return false; }	// too big to join
			 else { adjBug = cr; }								// keep looking
			 }
		 }
		 return true;
}
 /*
 int sweep_counter = 1;
 private int countBlobSize(BugCell from,BugChip top,int sweep)
 {	int tot = 0;
	 if(from!=null && sweep!=from.sweep_counter && from.topChip()==top)
	 {
		 tot++;
		 from.sweep_counter = sweep;
		 G.print("count "+from);
		 for(int i=0;i<6;i++)
		 {
			 tot+=countBlobSize(from.exitTo(i),top,sweep);
		 }
	 }else
	 {
	 G.print("skip "+from);
	 }
	 return tot;
 }
 */

 public boolean hasMoves()
 {
	 return addLegalMoves(null,whoseTurn);
 }
 public commonMove getRandomMove(Random r)
 {

	 return null;
 }
 
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();

 	switch(board_state)
 	{
 	case Puzzle:
 		break;
 	case Play:
 		addLegalMoves(all,whoseTurn);
 		break;
 	case Resign:
 	case Confirm:
 		all.push(new BugMovespec(MOVE_DONE,whoseTurn));
 		break;
 	case Grow:
 		addGrowMoves(all,whoseTurn);
 		break;
 	case Gameover:
 	case Draw:
 		break;
 	default:
 			G.Error("Not expecting state ",board_state);
 	}
 	return(all);
 }
 
 public void initRobotValues(BugPlay m)
 {	robot = m;
 }

 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(Character.isDigit(txt.charAt(0)))
	 	{ switch(variation)
	 		{
	 		case Bug_3:
	 		case Bug_4:
	 		case Bug_5:
	 			xpos -= cellsize/2;
	 			break;
 			default: G.Error("case "+variation+" not handled");
	 		}
	 	}
 		else
 		{ 
 		  ypos += cellsize/4;
 		}
 	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
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
 public Hashtable<BugCell, BugMovespec> getTargets() 
 {
 	Hashtable<BugCell,BugMovespec> targets = new Hashtable<BugCell,BugMovespec>();
 	CommonMoveStack all = GetListOfMoves();
 	for(int lim=all.size()-1; lim>=0; lim--)
 	{	BugMovespec m = (BugMovespec)all.elementAt(lim);
 		switch(m.op)
 		{
 		case MOVE_PICKB:
 		case MOVE_DROPB:
 			targets.put(getCell(m.to_col,m.to_row),m);
 			break;
 		case MOVE_SWAP:
 		case MOVE_DONE:
 			break;

 		default: G.Error("Not expecting "+m);
 		
 		}
 	}
 	
 	return(targets);
 }
 //public boolean drawIsPossible() { return false; }
 // public boolean canOfferDraw() {
 //	 return false;
	 /**
	something like this:
 	return (movingObjectIndex()<0)
 			&& ((board_state==BugState.Play) || (board_state==BugState.DrawPending))
 			&& (moveNumber-lastDrawMove>4);
 			*/
 //}
 LStack tempI = null;
public synchronized LStack tempLstack() {
	LStack ii = tempI;
	tempI = null;
	if(ii==null) { ii=new LStack();} else { ii.clear(); }
	return ii;
}
public void returnTempL(LStack i) { tempI = i;}

// this is a hack to find all the bugs up to size 8
// and incidentally to validate the algorithm
public void findBugs(Bug from)
{
	for(int lim = from.size()-1; lim>=0; lim--)
	{
		BugCell c = from.elementAt(lim);
		for(int dir=0; dir<CELL_FULL_TURN;dir++)
		{
			BugCell adj = c.exitTo(dir);
			if(adj!=null && adj.topChip()==null)
			{
				BugMovespec m = new BugMovespec(MOVE_DROPB,adj.col,adj.row,0);
				setState(BugState.Grow);
				latentCaptures[whoseTurn].clear();
				latentCaptures[whoseTurn].push(from);
				setWhoseTurn(0);
				RobotExecute(m);
				Bug newbug = adj.critter(this);
				if(newbug.size()<8)
				{
					findBugs(newbug);
				}
				UnExecute(m);
			}
		}
	}
}
//this is a hack to find all the bugs up to size 8
//and incidentally to validate the algorithm
public void findBugs() {
		doInit();
		maxBugSize = 8;
		BugCell c = getCell('D',4);
		SetBoard(c,BugChip.White);
		findBugs(c.critter(this));
		Bug.printBugs();
}
}
