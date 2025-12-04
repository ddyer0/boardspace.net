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
package stymie;

import static stymie.Stymiemovespec.*;

import java.util.*;
import lib.*;
import lib.Random;
import online.game.*;

/**
 * StymieBoard knows all about the game of Stymie, which is played
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

class StymieBoard extends rectBoard<StymieCell> implements BoardProtocol,StymieConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
	StymieVariation variation = StymieVariation.stymie;
	private StymieState board_state = StymieState.Puzzle;	
	private StymieState unresign = null;	// remembers the orignal state when "resign" is hit
	public StymieState getState() { return(board_state); }
	private static int CAPTURE_LOSS_THRESHOLD = 7;
	private static int CENTER_WIN_THRESHOLD = 7;
	private CellStack emptyPath = new CellStack();	// always remains empty
	private CellStack path = new CellStack();
	private CellStack playerPath[] = { new CellStack(),new CellStack() };
	
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(StymieState st) 
	{ 	unresign = (st==StymieState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	private int centerCount[] = new int[2];
    private StymieId playerColor[]={StymieId.Gold_Chip_Pool,StymieId.Silver_Chip_Pool};    
    private StymieChip playerChip[]={StymieChip.Gold,StymieChip.Silver};
    private StymieChip playerAntipode[]={StymieChip.Antipode_g,StymieChip.Antipode_s};
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public StymieChip getPlayerChip(int p) { return(playerChip[p]); }
	public int playerIndex(StymieChip ch)
	{
		return((ch==playerChip[0]) ? 0
				: ch==playerChip[1] ? 1 : -1);
				
	}
	public StymieId getPlayerColor(int p) { return(playerColor[p]); }
	public StymieCell getPlayerCell(int p) { return(playerCell[p]); }
	public StymieCell getPlayerCaptives(int p) { return(playerCaptives[p]); }
	public StymieChip getAntipode(int p) { return(playerAntipode[p]); }
	public StymieChip getCurrentPlayerChip() { return(playerChip[whoseTurn]); }

// this is required even if it is meaningless for this game, but possibly important
// in other games.  When a draw by repetition is detected, this function is called.
// the game should have a "draw pending" state and enter it now, pending confirmation
// by the user clicking on done.   If this mechanism is triggered unexpectedly, it
// is probably because the move editing in "editHistory" is not removing last-move
// dithering by the user, or the "Digest()" method is not returning unique results
// other parts of this mechanism: the Viewer ought to have a "repRect" and call
// DrawRepRect to warn the user that repetitions have been seen.
	public void SetDrawState() { setState(StymieState.Gameover);};	
	CellStack animationStack = new CellStack();

    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public StymieChip pickedObject = null;
    public int pickedAltChipIndex = 0;
    public StymieChip lastPicked = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private CellStack captureStack = new CellStack();
    private StateStack stateStack = new StateStack();

    public StymieCell GoldChips = new StymieCell(StymieId.Gold_Chip_Pool);
    public StymieCell SilverChips = new StymieCell(StymieId.Silver_Chip_Pool);
    public StymieCell GoldCaptives = new StymieCell(StymieId.Gold_Captives);
    public StymieCell SilverCaptives = new StymieCell(StymieId.Silver_Captives);
    public StymieCell jumpStart = null;	// previous jump from
    public StymieCell jumpEnd = null;	// previous jump to

    private StymieCell playerCell[]=new StymieCell[] {GoldChips,SilverChips};
    private StymieCell playerCaptives[]=new StymieCell[] {GoldCaptives,SilverCaptives};
   
    // save strings to be shown in the game log
    StringStack gameEvents = new StringStack();
    InternationalStrings s = G.getTranslations();
    private CellStack robotCells = new CellStack();
	private StateStack robotState = new StateStack();
	
 	void logGameEvent(String str,String... args)
 	{	//if(!robotBoard)
 		{String trans = s.get(str,args);
 		 gameEvents.push(trans);
 		}
 	}

 	private StymieState resetState = StymieState.Puzzle; 
 	private boolean setupDone = false;
 	
    public StymieChip lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public StymieCell newcell(char c,int r)
	{	return(new StymieCell(StymieId.BoardLocation,c,r));
	}
	
	// constructor 
    public StymieBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = GRIDSTYLE;
        setColorMap(map, players);
        

        doInit(init,key,players,rev); // do the initialization 
        autoReverseY();		// reverse_y based on the color map
    }
    
    public String gameType() { return(G.concat(gametype," ",players_in_game," ",randomKey," ",revision)); }
    

    public void doInit(String gtype,long key)
    {
    	Tokenizer tok = new Tokenizer(gtype);
    	String typ = tok.nextToken();
    	int np = tok.hasMoreTokens() ? tok.intToken() : players_in_game;
    	long ran = tok.hasMoreTokens() ? tok.longToken() : key;
    	int rev = tok.hasMoreTokens() ? tok.intToken() : revision;
    	doInit(typ,ran,np,rev);
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int players,int rev)
    {	randomKey = key;
    	adjustRevision(rev);
    	players_in_game = players;
    	win = new boolean[players];
 		setState(StymieState.Puzzle);
		variation = StymieVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		gametype = gtype;
		setupDone = false;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case stymie:
		case stymie_revised:
			// using reInitBoard avoids thrashing the creation of cells 
			// when reviewing games.
			reInitBoard(7,7);
			for(StymieCell c = allCells; c!=null; c=c.next)
				{ 
				if(c.isEdgeCell()) { c.edgeArea = true; }
				else if(c.row==4&&c.col=='D') {  /* center is not a prime space */ }
				else { c.primeArea=true; } 
				}
		}
		reInit(playerPath);
		path.clear();
		reInit(playerCell);
		reInit(playerCaptives);
		for(int i=0;i<variation.nChips;i++)
		{
			GoldChips.addChip(StymieChip.Gold);
			SilverChips.addChip(StymieChip.Silver);
		}
		getCell((char)('A'+3),4).addChip(StymieChip.Antipode_s);
	    whoseTurn = FIRST_PLAYER_INDEX;
	    droppedDestStack.clear();
	    pickedSourceStack.clear();
	    captureStack.clear();
	    stateStack.clear();
	    AR.setValue(centerCount,0);
	    pickedObject = null;
	    resetState = null;
	    lastDroppedObject = null;
	    int map[]=getColorMap();
		playerColor[map[0]]=StymieId.Gold_Chip_Pool;
		playerColor[map[1]]=StymieId.Silver_Chip_Pool;
		playerChip[map[0]]=StymieChip.Gold;
		playerChip[map[1]]=StymieChip.Silver;
		playerAntipode[map[0]] =StymieChip.Antipode_g;
		playerAntipode[map[1]] =StymieChip.Antipode_s;
   
        animationStack.clear();
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }


    /** create a copy of this board */
    public StymieBoard cloneBoard() 
	{ StymieBoard dup = new StymieBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((StymieBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(StymieBoard from_b)
    {
        super.copyFrom(from_b);
        robotState.copyFrom(from_b.robotState);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        jumpStart = getCell(from_b.jumpStart);
        jumpEnd = getCell(from_b.jumpEnd);
        copyFrom(playerCaptives,from_b.playerCaptives);
        copyFrom(playerCell,from_b.playerCell);
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(captureStack,from_b.captureStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        stateStack.copyFrom(from_b.stateStack);
        AR.copy(centerCount,from_b.centerCount);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        lastPicked = null;
        setupDone = from_b.setupDone;
        AR.copy(playerColor,from_b.playerColor);
        AR.copy(playerChip,from_b.playerChip);
        AR.copy(playerAntipode,from_b.playerAntipode);
        getCell(playerPath,from_b.playerPath);
        getCell(path,from_b.path);
        if(G.debug()) { sameboard(from_b); }
    }

    

    public void sameboard(BoardProtocol f) { sameboard((StymieBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(StymieBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(sameCells(jumpStart,from_b.jumpStart),"jumpstart mismatch");
        G.Assert(sameCells(jumpEnd,from_b.jumpEnd),"jumpend mismatch");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedsourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(sameCells(captureStack,from_b.captureStack),"captureStack mismatch");
        G.Assert(setupDone==from_b.setupDone,"setupdone mismatch");
        G.Assert(AR.sameArrayContents(centerCount,from_b.centerCount),"center count mismatch");
        G.Assert(sameCells(path,from_b.path), "path mismatch");
        G.Assert(sameCells(playerPath,from_b.playerPath),"playerPath mismatch");
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
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,captureStack);
		v ^= Digest(r,jumpStart);
		v ^= Digest(r,jumpEnd);
		v ^= Digest(r,revision);
		v ^= Digest(r,centerCount);
		v ^= Digest(r,setupDone);
		v ^= Digest(r,path);
		v ^= Digest(r,playerPath);
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
        	throw G.Error("Move not complete, can't change the current player in state ",board_state);
        case Puzzle:
            break;
        case Setup:
         	// some damaged games have 2 dones in a row
        	if(replay==replayMode.Live) { throw G.Error("Move not complete, can't change the current player"); }
			//$FALL-THROUGH$
        case Confirm:
        case Resign:
        case Jump:
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
    	return (win[player] = ((playerCaptives[player].height()>=CAPTURE_LOSS_THRESHOLD) 
    							|| (centerCount[player]>=CENTER_WIN_THRESHOLD
    							|| ( (variation!=StymieVariation.stymie)
    									&& setupDone
    									&& (centerCount[nextPlayer[player]]==0))
    									)));
    }

    // set the contents of a cell, and maintain the books
    private int prevDropped = -1;
    private int prevPicked = -1;
    
    public StymieChip SetBoard(StymieCell c,StymieChip ch)
    {	//checkCount();
    	StymieChip old = c.topChip();
    	if(ch!=null)
    		{ c.addChip(ch);
    		  if(c.primeArea) 
    		  	{ int ind = playerIndex(ch);
    		  	  if(ind>=0) { centerCount[ind]++; }
    		  	}
    		} 
    		else 
    		{ 	
    			c.removeTop(); 
    		}
    	if(old!=null && c.primeArea)
    	{
    		int ind = playerIndex(old);
    		if(ind>=0) { centerCount[ind]--; }
    	}
    	//checkCount();
    	return(old);
    }
    @SuppressWarnings("unused")
	private void checkCount()
    {
    	int csilver=0;
    	int cgold = 0;
    	for(StymieCell d = allCells; d!=null; d=d.next) {
    		if(!d.isEdgeCell()) {
    			StymieChip top = d.topChip();
    			if(top==StymieChip.Gold) { cgold++; }
    			else if(top==StymieChip.Silver) { csilver++; } 
    		}
    	}
    	G.Assert(csilver==centerCount[1] && cgold==centerCount[0],"mismatch");
    }
    //
    // accept the current placements as permanent
    //
    public void acceptPlacement()
    {	
        droppedDestStack.clear();
        pickedSourceStack.clear();
        captureStack.clear();
        stateStack.clear();
        path.clear();
        pickedObject = null;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private StymieCell unDropObject()
    {	StymieCell rv = droppedDestStack.pop();
    	if(rv.onBoard) 
    		{ G.Assert(rv==path.pop(),"should be the same");
    		} 
    	setState(stateStack.pop());
    	pickedObject = SetBoard(rv,null); 	// SetBoard does ancillary bookkeeping
    	pickedAltChipIndex = rv.altChipIndex;
    	rv.lastDropped = prevDropped;
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	StymieCell rv = pickedSourceStack.pop();
    	if(rv.onBoard)
    	{
    		G.Assert(rv==path.pop(),"path should match");
    	}
    	setState(stateStack.pop());
    	SetBoard(rv,pickedObject);
    	rv.lastPicked = prevPicked;
    	rv.altChipIndex = pickedAltChipIndex;
    	pickedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(StymieCell c)
    {
       droppedDestStack.push(c);
       stateStack.push(board_state);
       
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting dest " + c.rackLocation);
        case Silver_Chip_Pool:
        case Gold_Chip_Pool:		// back in the pool, we don't really care where
        case Silver_Captives:
        case Gold_Captives:
        	c.addChip(pickedObject);
        	pickedObject = null;
            break;
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        case EmptyBoard:
           	SetBoard(c,pickedObject);
           	prevDropped = c.lastDropped;
           	c.lastDropped = moveNumber;
           	path.push(c);
           	c.altChipIndex = pickedAltChipIndex;
            pickedObject = null;
            break;
        }
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(StymieCell c)
    {	return(droppedDestStack.top()==c);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { StymieChip ch = pickedObject;
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
    private StymieCell getCell(StymieId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source " + source);
        case BoardLocation:
        	return(getCell(col,row));
        	
        case Silver_Captives:
        	return(SilverCaptives);
        case Gold_Captives:
        	return(GoldCaptives);
        case Silver_Chip_Pool:
        	return(SilverChips);
        case Gold_Chip_Pool:
        	return(GoldChips);
        } 	
    }
    /**
     * this is called when copying boards, to get the cell on the new (ie current) board
     * that corresponds to a cell on the old board.
     */
    public StymieCell getCell(StymieCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private StymieChip pickObject(StymieCell c)
    {	pickedSourceStack.push(c);
    	stateStack.push(board_state);
        switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting rackLocation " + c.rackLocation);
        case BoardLocation:
        	{
            lastPicked = pickedObject = c.topChip();
            pickedAltChipIndex = c.altChipIndex;
            path.push(c);
         	lastDroppedObject = null;
			SetBoard(c,null);
			prevPicked = c.lastPicked;
			c.lastPicked = moveNumber;
        	}
            break;

        case Silver_Chip_Pool:
        case Silver_Captives:
        case Gold_Captives:
        case Gold_Chip_Pool:
        	lastPicked = pickedObject = c.removeTop();
        	pickedAltChipIndex = c.height();
        }
        return(pickedObject);
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(StymieCell c)
    {	return(c==pickedSourceStack.top());
    }
    public StymieCell getSource() { return(pickedSourceStack.top()); }
    public StymieCell getDest() { return(droppedDestStack.top()); }
    public boolean isASource(StymieCell c) { return pickedSourceStack.contains(c); }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(replayMode replay,boolean isJump)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state " + board_state);
        case Confirm:
        	setNextStateAfterDone(replay);
         	break;
        case Play:
        case Jump:
        	if(isJump && hasJumpMoves()) { setState(StymieState.Jump); }
        	else { setState(StymieState.Confirm); }
        	break;
        case Setup:
			setState(StymieState.Confirm);
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
    	case Gameover: break;
    	case Jump:
    	case Confirm:
    		if(!setupDone && hasPlacenewMoves()) { setState(StymieState.Setup); }
    		else { setupDone = true;setState(StymieState.Play); }
    		break;
    	case Puzzle:
    	case Setup:
    		setState(StymieState.Setup);
    		
    		break;
    	}
       	resetState = board_state;
    }
    private void doDone(replayMode replay)
    {
    	playerPath[whoseTurn].copyFrom(path);
        acceptPlacement();

        if (board_state==StymieState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(StymieState.Gameover);
        }
        else
        {	if(winForPlayerNow(whoseTurn)) 
        		{ 
        		  setState(StymieState.Gameover); 
        		}
        	else {setNextPlayer(replay);
        		setNextStateAfterDone(replay);
        	}
        }
    }
    public Hashtable<StymieCell,Stymiemovespec> getTargets()
    {
    	Hashtable <StymieCell,Stymiemovespec> targets = new Hashtable<StymieCell,Stymiemovespec>();
    	CommonMoveStack all = GetListOfMoves();
    	
    	for(int lim = all.size()-1; lim>=0; lim--)
    	{
    		Stymiemovespec m = (Stymiemovespec)all.elementAt(lim);
    		switch(m.op) 
    		{
    		default: G.Error("not expecting ",m);
    			break;
    		case MOVE_RESIGN:
    		case MOVE_DONE: break;
    		case MOVE_JUMP:
    		case MOVE_CAPTURE:
    		case MOVE_FLIP:
    		case MOVE_MOVE:
    			if(pickedObject==null)
    			{
    				StymieCell c = getCell(m.from_col,m.from_row);
    				targets.put(c,m);
    				break;
    			}
				//$FALL-THROUGH$
			case MOVE_DROPB:
			case MOVE_DROPFLIP:
			case MOVE_DROPJUMP:
			case MOVE_DROPCAP:
    			StymieCell c = getCell(m.to_col,m.to_row);
    			targets.put(c,m);
    			break;
    		}
    	}
    	
    	return(targets);
    }
    
    public boolean Execute(commonMove mm,replayMode replay)
    {	Stymiemovespec m = (Stymiemovespec)mm;
        if(replay.animate) { animationStack.clear(); }

        //G.print("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone(replay);

            break;
        case MOVE_DROPCAP:
	    	{	
			StymieCell src = getSource();
			StymieCell dest = getCell(m.to_col,m.to_row);
			int dir = findDirection(src,dest);
			StymieCell mid = src.exitTo(dir);
			StymieChip midTop = SetBoard(mid,null);
			StymieCell caps = playerCaptives[whoseTurn];
			caps.addChip(midTop);
			captureStack.push(mid);
			if(replay==replayMode.Single)
			{
				animationStack.push(src);
				animationStack.push(dest);
			}
			if(replay.animate)
			{
				animationStack.push(mid);
				animationStack.push(caps);
			}
			dropObject(dest);
			
			jumpStart = src;
			jumpEnd = dest;
			setNextStateAfterDrop(replay,true);
	    	}
			break;
        case MOVE_CAPTURE:
        	{	
    		StymieCell src = getCell(m.source,m.from_col,m.from_row);
    		StymieCell dest = getCell(m.to_col,m.to_row);
    		int dir = findDirection(src,dest);
    		StymieCell mid = src.exitTo(dir);
    		StymieChip midTop = SetBoard(mid,null);
    		StymieCell caps = playerCaptives[whoseTurn];
    		caps.addChip(midTop);
    		if(replay.animate)
    		{
    			animationStack.push(src);
    			animationStack.push(dest);
    			animationStack.push(mid);
    			animationStack.push(caps);
    		}
    		pickObject(src);
    		m.chip = pickedObject;
    		dropObject(dest);
    		jumpStart = src;
    		jumpEnd = dest;
    		setNextStateAfterDrop(replay,true);
        	}
    		break;
        case MOVE_FLIP:
	    	{	
			StymieCell src = getCell(m.source,m.from_col,m.from_row);
			StymieCell dest = getCell(m.to_col,m.to_row);
			int dir = findDirection(src,dest);
			StymieCell mid = src.exitTo(dir);
			StymieChip midTop = mid.removeTop();
			StymieChip newmid = null;
			if(midTop==StymieChip.Antipode_g) {	newmid = StymieChip.Antipode_s; }
			else if(midTop==StymieChip.Antipode_s) { newmid = StymieChip.Antipode_g; }
			else { G.Error("Not expecting flip ",midTop); }
			
			mid.addChip(newmid);
			if(replay.animate)
			{
				animationStack.push(src);
				animationStack.push(dest);
			}
			pickObject(src);
			m.chip = pickedObject;
			dropObject(dest);
			jumpStart = src;
			jumpEnd = dest;
			setNextStateAfterDrop(replay,true);
	    	}
	    	break;
        case MOVE_DROPFLIP:
	    	{	
			StymieCell src = getSource();
			StymieCell dest = getCell(m.to_col,m.to_row);
			int dir = findDirection(src,dest);
			m.chip = pickedObject;
			StymieCell mid = src.exitTo(dir);
			StymieChip midTop = mid.removeTop();
			StymieChip newmid = null;
			captureStack.push(null);
			if(midTop==StymieChip.Antipode_g) {	newmid = StymieChip.Antipode_s; }
			else if(midTop==StymieChip.Antipode_s) { newmid = StymieChip.Antipode_g; }
			else { G.Error("Not expecting flip ",midTop); }
			
			mid.addChip(newmid);
			if(replay.animate)
			{
				animationStack.push(src);
				animationStack.push(dest);
			}
			dropObject(dest);
			jumpStart = src;
			jumpEnd = dest;
			setNextStateAfterDrop(replay,true);
	    	}
			break;
        case MOVE_JUMP:
        	{	// robot onboard, move, or jump
    		StymieCell src = getCell(m.source,m.from_col,m.from_row);
    		StymieCell dest = getCell(m.to_col,m.to_row);
    		pickObject(src);
    		m.chip = pickedObject;
    		dropObject(dest);
    		if(replay.animate)
    		{
    			animationStack.push(src);
    			animationStack.push(dest);
    		}
    		jumpStart = src;
    		jumpEnd = dest;
    		setNextStateAfterDrop(replay,true);
        	}
        	break;
        case MOVE_MOVE:
        	{	// robot onboard, move, or jump
        		StymieCell src = getCell(m.source,m.from_col,m.from_row);
        		StymieCell dest = getCell(m.to_col,m.to_row);
        		pickObject(src);
        		m.chip = pickedObject;
        		dropObject(dest);
        		captureStack.push(null);
        		if(replay.animate)
        		{
        			animationStack.push(src);
        			animationStack.push(dest);
        		}
        		setNextStateAfterDrop(replay,false);
        	}
        	break;
        case MOVE_DROPB:
        	{
			StymieChip po = pickedObject;
			StymieCell src = getSource();
			StymieCell dest =  getCell(StymieId.BoardLocation,m.to_col,m.to_row);
			if(isSource(dest)) 
				{ unPickObject(); 
				}
				else 
				{
				if(src==null || !src.onBoard) { m.chip = pickedObject; }
				if(po==null) { src = getCell(m.source,m.from_col,m.from_row); pickObject(src); }
				StymieChip dropped = pickedObject;
				dropObject(dest);
				captureStack.push(null);
				switch(board_state)
				{
				case Puzzle:	
					acceptPlacement(); 
					break;
				default: break;
				}
				
		        if(replay==replayMode.Live)
		        	{ lastDroppedObject = dropped.getAltDisplayChip(dest);
		        	  //G.print("last ",lastDroppedObject); 
		        	}
		           
	            /**
	             * if the user clicked on a board space without picking anything up,
	             * animate a stone moving in from the pool.  For Hex, the "picks" are
	             * removed from the game record, so there are never picked stones in
	             * single step replays.
	             */
	            if(replay==replayMode.Single || (po==null))
	            	{ animationStack.push(src);
	            	  animationStack.push(dest); 
	            	}
	            setNextStateAfterDrop(replay,false);
				}
        	}
             break;
        case MOVE_DROPJUMP:
    	{
		StymieChip po = pickedObject;
		StymieCell src = getSource();
		StymieCell dest =  getCell(StymieId.BoardLocation,m.to_col,m.to_row);
		
		if(isSource(dest)) 
			{ unPickObject(); 
			}
			else 
			{
			StymieChip dropped = m.chip = pickedObject;
			dropObject(dest);
			captureStack.push(null);
			
	        if(replay==replayMode.Single)
	        	{ lastDroppedObject = dropped.getAltDisplayChip(dest);
	        	  //G.print("last ",lastDroppedObject); 
	        	}
	           
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
    		jumpStart = src;
    		jumpEnd = dest;
    		setNextStateAfterDrop(replay,true);
			}
    	}
    	
         break;

        case MOVE_PICK:
 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
 			StymieCell src = getCell(m.source,m.to_col,m.to_row);
 			if(isDest(src) && (board_state!=StymieState.Jump))
 			{
 				unDropObject();
 				StymieCell mid = captureStack.pop();
 				if(mid!=null)
 				{
 					G.Assert(mid.topChip()==null,"should be empty");
 					// undo a capture
 					SetBoard(mid, playerCaptives[whoseTurn].removeTop());
 				}
 				if(board_state==StymieState.Jump) 
 				{
 				jumpEnd=jumpStart;
 				jumpStart = pickedSourceStack.elementAt(pickedSourceStack.size()-2);
 				}else
 				{
 					jumpStart = jumpEnd = null;
 					
 				}
 			}
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
        		setState(StymieState.Setup);
        		break;
        	default: ;
        	}}}
            break;

        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
            StymieCell dest = getCell(m.source,m.to_col,m.to_row);
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
            setState(StymieState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ setState(StymieState.Gameover); 
               	}
            else {  setNextStateAfterDone(replay); }

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?StymieState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(StymieState.Puzzle);
 
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(StymieState.Gameover);
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
        case Jump:
        	return((player==whoseTurn) && hasPlacenewMoves());
        case Setup:
        	// for pushfight, you can pick up a stone in the storage area
        	// but it's really optional
        	return(player==whoseTurn);
        case Confirm:
		case Resign:
		case Gameover:
			return(false);
        case Puzzle:
            return ((pickedObject!=null)?(pickedObject==playerChip[player]):true);
        }
    }

    public boolean legalToHitBoard(StymieCell c,Hashtable<StymieCell,Stymiemovespec>targets)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
        case Play:
		case Setup:
		case Jump:
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
    
    public double scoreEstimateForPlayer(int player)
    {	
    	int center = centerCount[player];
    	int captive = playerCaptives[player].height();
    	double value_for_center = 0.5;
    	double value_for_reserve = -0.5;
    	double value_for_capture = 2.0;
     	
    	int reserve = playerCell[player].height();
    	//
    	// chips in reserve are a small penalty, chips in center are a plus
    	// captives are a liability.  This makes it useful to avoid capture
    	// and move chips onto the board
    	//
       	double val = captive*value_for_capture
       					+ center*value_for_center
       					+ reserve*value_for_reserve;

       	
    	return(val);
    }
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Stymiemovespec m)
    {	//G.print("E "+m+" "+setupDone);
        robotState.push(board_state); //record the starting state. The most reliable
        robotCells.push(jumpEnd);
        robotCells.push(jumpStart);
        // to undo state transistions is to simple put the original state back.
        
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        Execute(m,replayMode.Replay);
        acceptPlacement();
        
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(Stymiemovespec m)
    {
    	StymieState state = robotState.pop();
    	jumpStart = robotCells.pop();
    	jumpEnd = robotCells.pop();
    	
        switch (m.op)
        {
        default:
   	    	throw G.Error("Can't un execute " + m);
        case MOVE_DONE:
            break;
            
        case MOVE_CAPTURE:
   			{
	    	StymieCell src = getCell(m.from_col,m.from_row);
	    	StymieCell dest = getCell(m.to_col,m.to_row);
	    	StymieCell mid = getCell((char)((m.from_col+m.to_col)/2),(m.from_row+m.to_row)/2);
	    	StymieCell chip = getPlayerCaptives(m.player);
	    	SetBoard(mid,chip.removeTop());
	    	SetBoard(src,SetBoard(dest,null));
   			}
   		break;

        case MOVE_FLIP:
       		{
        	StymieCell src = getCell(m.from_col,m.from_row);
        	StymieCell dest = getCell(m.to_col,m.to_row);
        	StymieCell mid = getCell((char)((m.from_col+m.to_col)/2),(m.from_row+m.to_row)/2);
        	StymieChip ant = mid.removeTop();
        	SetBoard(mid,ant==StymieChip.Antipode_g ? StymieChip.Antipode_s : StymieChip.Antipode_g);
        	SetBoard(src,SetBoard(dest,null));
        	}
       		break;
        case MOVE_MOVE:
        case MOVE_JUMP:
        	{
        	StymieCell src = getCell(m.from_col,m.from_row);
        	StymieCell dest = getCell(m.to_col,m.to_row);
        	SetBoard(src,SetBoard(dest,null));
        	}
        	break;
        case MOVE_DROPB:
        	{
        	StymieCell dest = getCell(m.to_col,m.to_row);
        	StymieCell src = getPlayerCell(m.player);
        	SetBoard(src,SetBoard(dest,null));
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(state);
        if(robotState.top()==StymieState.Setup) { setupDone = false; }
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
       // System.out.println("U "+m+" "+setupDone);
 }
 private boolean hasPlacenewMoves()
 {
	 return(addPlaceNewMoves(null,whoseTurn));
 }
 private boolean addPlaceNewMoves(CommonMoveStack all,int who)
 {   boolean some = false;
 	 if(((pickedObject==null)||!getSource().onBoard)
 		 && (getPlayerCell(who).height()>0))
 	 {
	 for(StymieCell c = allCells;
			 c!=null;
			 c = c.next)
	 	 {	if((c.topChip()==null) && !c.hasAdjacentNonEmpty())
	 	 	{if(all==null) { return(true); }
	 	 	 some = true;
	 	 	 all.addElement(new Stymiemovespec(MOVE_DROPB,c.col,c.row,playerColor[who],who));
	 	 	}
	 	 }}
	 return(some);
 }
 private boolean hasJumpMoves()
 {		for(int direction=CELL_LEFT(),lastDir = direction+CELL_FULL_TURN(); direction<lastDir; direction+= CELL_QUARTER_TURN())
 		{
	 	StymieCell mid = jumpEnd.exitTo(direction);
	 	if(mid!=null)
	 		{
	 		StymieChip midTop = mid.topChip();
	 		if(midTop!=null)
	 			{
	 			StymieCell to = mid.exitTo(direction);
	 			if(to!=null && (to!=jumpStart))
	 				{
	 				if(addJumpMove(null,whoseTurn,jumpEnd,mid,midTop,to,false)) { return(true); }
	 				}
	 			}
	 		}
 		}
 	return(false);
 }
 private boolean addJumpMove(CommonMoveStack all,int who,StymieCell from,StymieCell mid,StymieChip midtop,StymieCell to,boolean prePicked)
 {	boolean some = false;
	 StymieChip myChip = getPlayerChip(who);
	 StymieChip hisChip = getPlayerChip(nextPlayer[who]);
	 if(to!=null 
			 && (to.topChip()==null)
			 && ((variation==StymieVariation.stymie) 
						||!reversePathMatches(from,to,path,playerPath[who]))
			 )
	    {
	    if(midtop==hisChip)
	    	{	// jump/capture, except can't jump in from an edge to a non edge
	    	if((variation==StymieVariation.stymie_revised) || !from.edgeArea|| to.edgeArea)
	    	{	if(all==null) { return(true); }
	    		all.addElement(new Stymiemovespec(prePicked ? MOVE_DROPCAP : MOVE_CAPTURE,from.col,from.row,to.col,to.row,who));
	    		some = true;
	    	}}
	    else if(midtop==myChip)
	    	{	// own chip
	    		if(all==null) { return(true); }
	    		all.addElement(new Stymiemovespec(prePicked ? MOVE_DROPJUMP: MOVE_JUMP,from.col,from.row,to.col,to.row,who));
	    		some = true;
	    	}
	    else 
		 	{	//the antipode
	    		if(all==null) { return(true); }
	    		all.addElement(new Stymiemovespec(prePicked ? MOVE_DROPFLIP:MOVE_FLIP,from.col,from.row,to.col,to.row,who));
	    		some = true;
		 	}
	    }
	 	return(some);
 }
 
 private void addSimpleMoves(CommonMoveStack all,int who, StymieCell c,StymieChip mTop,boolean prePicked)
 {
	 StymieChip myChip = getPlayerChip(who);
	 StymieChip hisChip = getPlayerChip(nextPlayer[who]);
	 StymieChip myAntipode = getAntipode(who);
	 if(mTop==myChip)
		{
		
		for(int direction = CELL_LEFT(),last = direction+CELL_FULL_TURN(); direction<last  ; direction += CELL_QUARTER_TURN())
		{	StymieCell adj = c.exitTo(direction);
		 	if(adj!=null)
		 	{
		 	StymieChip top = adj.topChip();
		    if(top==null)
		    	{ // simple move to an empty adjacent space
		    	if((variation==StymieVariation.stymie) 
							||!reversePathMatches(c,adj,emptyPath,playerPath[who]))
		    	{
		    		all.addElement(new Stymiemovespec(prePicked ? MOVE_DROPB : MOVE_MOVE,c.col,c.row,adj.col,adj.row,who));	 		    	
		    	}}
		    else {
		    	StymieCell to = adj.exitTo(direction);
		    	addJumpMove(all,who,c,adj,top,to,prePicked);
	 		    }
		    }	// end of adj not null
		}	// end of directions
		}	// end of mychip moves
	else if(mTop==hisChip)	{}
	else if(mTop==myAntipode)
		{	// moving the antipode.  Cannot jump but can move in a line
			for(int direction = CELL_LEFT(),last = direction+CELL_FULL_TURN(); direction<last  ; direction += CELL_QUARTER_TURN())
			{	StymieCell adj = c.exitTo(direction);
				while((adj!=null)&&(adj.topChip()==null))
				{
					all.addElement(new Stymiemovespec(prePicked ? MOVE_DROPB : MOVE_MOVE,c.col,c.row,adj.col,adj.row,who));	
					adj = adj.exitTo(direction);
				}
			}
		}
 }
 private void addSimpleMoves(CommonMoveStack all,int who)
 {	
	if(pickedObject!=null)
	{	StymieCell src = getSource();
		if(src.onBoard) { addSimpleMoves(all,who,src,pickedObject,true); }
	}
	else
	{
 	for(StymieCell c = allCells;
 			c!=null;
 			c = c.next)
 	{	StymieChip mTop = c.topChip();
 		if(mTop!=null)
 		{
 			addSimpleMoves(all,who,c,mTop,false);
 		}
 	}}
 }
 
//
// return true if activePath+c matches matchPath.  This is used to implement
// the anti-twiddle rule from the revised variant of stymie
//
private boolean reversePathMatches(StymieCell from,StymieCell to,CellStack activePath,CellStack matchPath)
{	int pSize = activePath.size();
	if(pSize+2!=matchPath.size()) { return(false); }
	if(to!=matchPath.elementAt(0)) { return(false); }
	if(from!=matchPath.elementAt(1)) { return(false); }
	for(int i=0;i<pSize;i++) { if(activePath.elementAt(i)!=matchPath.elementAt(pSize-i+1)) { return(false); }}
	return(true);
}


// add "continued jump" moves, which can't just jump back to the previous jump point
public void addJumpMoves(CommonMoveStack all,StymieCell pfrom,StymieCell from,StymieChip picked,int who)
 {	
 		for(int direction=CELL_LEFT(),last=direction+CELL_FULL_TURN();
 			direction<last;
 			direction+=CELL_QUARTER_TURN())
 			{	
 			StymieCell mid = from.exitTo(direction);
 			if(mid!=null)
 			{
 				StymieChip adjTop = mid.topChip();
 				if(adjTop!=null)
 				{
 					StymieCell to = mid.exitTo(direction);
 					if(to!=pfrom)
 					{	
  						addJumpMove(all,who,from,mid,adjTop,to,picked!=null);
  					}
 				}
  			}
 	}
 }

 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	
 	switch(board_state)
 	{
 	case Resign:
 	case Confirm:
 		all.push(new Stymiemovespec(MOVE_DONE,whoseTurn));
 		break;
 	case Jump:
 		addJumpMoves(all,jumpStart,jumpEnd,pickedObject,whoseTurn);
 		all.push(new Stymiemovespec(MOVE_DONE,whoseTurn)); 		
 		break;
 	case Play:
 		addSimpleMoves(all,whoseTurn); 		
		//$FALL-THROUGH$
	case Setup:
 		addPlaceNewMoves(all,whoseTurn);
 		if(all.size()==0) { all.push(new Stymiemovespec(MOVE_RESIGN,whoseTurn)); 		}
 		break;
 	default: break;
 	}


 	return(all);
 }
 
 public void initRobotValues()
 {	robotState.clear();
 	robotCells.clear();
 }


}
