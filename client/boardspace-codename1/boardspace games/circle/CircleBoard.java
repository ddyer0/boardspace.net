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
package circle;


import static circle.CircleMovespec.*;

import java.util.*;

import bridge.Color;
import lib.*;
import lib.Random;
import online.game.*;

/**
 * CircleBoard knows all about the game of CircleOfLife, which is played
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

class CircleBoard 
	extends hexBoard<CircleCell>	// for a square grid board, this could be rectBoard or squareBoard 
	implements BoardProtocol,CircleConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
	static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	CircleVariation variation = CircleVariation.CircleOfLife;
	private CircleState board_state = CircleState.Puzzle;	
	private CircleState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	public CircleState getState() { return(board_state); }
	private CritterStack captureStack =  new CritterStack();
	int captureSize[] = new int[2];
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(CircleState st) 
	{ 	unresign = (st==CircleState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	private int playerIndex(CircleChip ch) { return( (ch==playerChip[0]) ? 0 : 1); }
    private CircleId playerColor[]={CircleId.White,CircleId.Black};    
    private CircleChip playerChip[]={CircleChip.White,CircleChip.Black};
    private CircleCell playerCell[]=new CircleCell[2];
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public CircleChip getPlayerChip(int p) { return(playerChip[p]); }
	public CircleId getPlayerColor(int p) { return(playerColor[p]); }
	public CircleCell getPlayerCell(int p) { return(playerCell[p]); }
	public CircleChip getCurrentPlayerChip() { return(playerChip[whoseTurn]); }
	public CirclePlay robot = null;
	
	 public boolean p1(String msg)
		{
			if(G.p1(msg) && robot!=null)
			{	String dir = "g:/share/projects/boardspace-html/htdocs/circle/circlegames/robot/";
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
	public void SetDrawState() { setState(CircleState.Draw); }	CellStack animationStack = new CellStack();
    private int chips_on_board = 0;			// number of chips currently on the board
    private int fullBoard = 0;				// the number of cells in the board

    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public CircleChip pickedObject = null;
    public CircleChip lastPicked = null;
    private CircleCell blackChipPool = null;	// dummy source for the chip pools
    private CircleCell whiteChipPool = null;
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
    private CircleState resetState = CircleState.Puzzle; 
    public DrawableImage<?> lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public CircleCell newcell(char c,int r)
	{	return(new CircleCell(CircleId.BoardLocation,c,r));
	}
	
	// constructor 
    public CircleBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = GRIDSTYLE;
        setColorMap(map, players);
        
		Random r = new Random(734687);
		// do this once at construction
	    blackChipPool = new CircleCell(r,CircleId.Black);
	    blackChipPool.addChip(CircleChip.Black);
	    whiteChipPool = new CircleCell(r,CircleId.White);
	    whiteChipPool.addChip(CircleChip.White);

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
 		setState(CircleState.Puzzle);
		variation = CircleVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		gametype = gtype;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case CircleOfLife:
			// using reInitBoard avoids thrashing the creation of cells 
			// when reviewing games.
			reInitBoard(variation.firstInCol,variation.ZinCol,null);
			// or initBoard(variation.firstInCol,variation.ZinCol,null);
			// Random r = new Random(734687);	// this random is used to assign hash values to cells, common to all games of this type.
			// allCells.setDigestChain(r);		// set the randomv for all cells on the board
		}

 		
	    playerCell[FIRST_PLAYER_INDEX] = whiteChipPool; 
	    playerCell[SECOND_PLAYER_INDEX] = blackChipPool; 
	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    chips_on_board = 0;
	    droppedDestStack.clear();
	    pickedSourceStack.clear();
	    stateStack.clear();
	    captureStack.clear();
	    AR.setValue(captureSize,0);
	    pickedObject = null;
	    resetState = null;
	    lastDroppedObject = null;
	    int map[]=getColorMap();
		playerColor[map[0]]=CircleId.White;
		playerColor[map[1]]=CircleId.Black;
		playerChip[map[0]]=CircleChip.White;
		playerChip[map[1]]=CircleChip.Black;
	    // set the initial contents of the board to all empty cells
		emptyCells.clear();
		for(CircleCell c = allCells; c!=null; c=c.next) { c.reInit(); emptyCells.push(c); }
		fullBoard = emptyCells.size();
	    
        animationStack.clear();
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }

    /** create a copy of this board */
    public CircleBoard cloneBoard() 
	{ CircleBoard dup = new CircleBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((CircleBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(CircleBoard from_b)
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

        AR.copy(playerColor,from_b.playerColor);
        AR.copy(playerChip,from_b.playerChip);
 
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((CircleBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(CircleBoard from_b)
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
    {	if(win[player]) { return(true); }
    	boolean win = false;
    	return(win);
    }


    // set the contents of a cell, and maintain the books
    private int lastPlaced = -1;
    public CircleChip SetBoard(CircleCell c,CircleChip ch)
    {	CircleChip old = c.topChip();
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
    private CircleCell unDropObject()
    {	CircleCell rv = droppedDestStack.pop();
    	setState(stateStack.pop());
    	pickedObject = SetBoard(rv,null); 	// SetBoard does ancillary bookkeeping
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	CircleCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	SetBoard(rv,pickedObject);
    	pickedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(CircleCell c)
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
    public boolean isDest(CircleCell c)
    {	return(droppedDestStack.top()==c);
    }
    public CircleCell getDest()
    {	return(droppedDestStack.top());
    }
 
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { CircleChip ch = pickedObject;
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
    private CircleCell getCell(CircleId source, char col, int row)
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
    public CircleCell getCell(CircleCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(CircleCell c)
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
    public boolean isSource(CircleCell c)
    {	return(c==pickedSourceStack.top());
    }
    public CircleCell getSource()
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
 			setState(CircleState.Confirm);
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
    	default: throw G.Error("Not expecting after Done state "+board_state);
    	case Gameover: break;
     	case Confirm:
    	case Puzzle:
    	case Play:
    		setState(CircleState.Play);
    		
    		break;
    	}
       	resetState = board_state;
    }
    
    private void captureCritter(Critter dest,replayMode replay)
    {
    	captureStack.push(dest);
    	captureSize[whoseTurn] += dest.size();
    	for(int lim=dest.size()-1; lim>=0; lim--)
    	{
    		CircleCell cap = dest.elementAt(lim);
    		SetBoard(cap,null);
    		if(replay.animate)
    		{
    			animationStack.push(cap);
    			animationStack.push(playerCell[nextPlayer[whoseTurn]]);
    		}
    	}
    }
    private void doCaptures(CircleCell dest,CircleMovespec m,replayMode replay)
    {	m.critter = null;
    	Critter myGroup = dest.critter();
    	if(myGroup!=null) //this prevents a nullpointerexception, but something is already wrong; there must be a critter
    	{
    	CR captureType = myGroup.identity().Eats;
    	for(int lim=myGroup.size()-1; lim>=0; lim--)
    	{
    		CircleCell c = myGroup.elementAt(lim);
    		for(int dir = 0; dir<6; dir++)
    		{
    			CircleCell adj = c.exitTo(dir);
    			if(adj!=null)
    			{
    				Critter adjCritter = adj.critter();
    				if(adjCritter!=null && adjCritter.identity()==captureType)
    				{
    					captureCritter(adjCritter,replay);
    					adjCritter.next = m.critter;
    					m.critter = adjCritter;
    					//m.critter = m.critter==null ? (StackIterator)adjCritter : (m.critter).push(adjCritter);
    				}
    			}
    		}
    	}}
    }
    
    
    private void doDone(CircleMovespec m,replayMode replay)
    {	CircleCell dest = getDest();
 
        acceptPlacement();

        if(dest!=null) { doCaptures(dest,m,replay); }
        
        if(captureSize[whoseTurn]>=20) { win[whoseTurn]=true; }
        
        if (board_state==CircleState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(CircleState.Gameover);
        }
        else
        {	if(winForPlayerNow(whoseTurn)) 
        		{ win[whoseTurn]=true;
        		  setState(CircleState.Gameover); 
        		}
        	else {setNextPlayer(replay);
        	    if(!hasMoves()) { win[whoseTurn]=true; setState(CircleState.Gameover); }
        	    else {	setNextStateAfterDone(replay); }
        	}
        }
    }
	
    public boolean Execute(commonMove mm,replayMode replay)
    {	CircleMovespec m = (CircleMovespec)mm;
        if(replay.animate) { animationStack.clear(); }

        //G.print("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone(m,replay);

            break;

        case MOVE_DROPB:
        	{
			CircleChip po = pickedObject;
			CircleCell dest =  getCell(CircleId.BoardLocation,m.to_col,m.to_row);
			CircleCell src = getSource();
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
            	m.critter = dest.critter();     
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
 			CircleCell src = getCell(m.source,m.to_col,m.to_row);
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
        		setState(CircleState.Play);
        		break;
        	default: ;
        	}}}
            break;

        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
            CircleCell dest = getCell(m.source,m.to_col,m.to_row);
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
            setState(CircleState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ setState(CircleState.Gameover); 
               	}
            else {  setNextStateAfterDone(replay); }

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?CircleState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(CircleState.Puzzle);
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(CircleState.Gameover);
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

    public boolean legalToHitBoard(CircleCell c,Hashtable<CircleCell,CircleMovespec> targets )
    {	if(c==null) { return(false); }
        switch (board_state)
        {
        case Puzzle:
		case Play:
			return(targets.get(c)!=null || isDest(c) || isSource(c));
		case Gameover:
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
    public void RobotExecute(CircleMovespec m)
    {
        robotState.push(board_state); //record the starting state. The most reliable

        Execute(m,replayMode.Replay);
        doDone(m,replayMode.Replay);
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(CircleMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	CircleState state = robotState.pop();
        switch (m.op)
        {
        default:
   	    	throw G.Error("Can't un execute " + m);
        case MOVE_DONE:
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
  
 private boolean addLegalMoves(CommonMoveStack all,int who)
 {
	 CellStack empty = emptyCells;
	 boolean some = false;
	 CircleChip top = getPlayerChip(who);
	 for(int lim = empty.size()-1; lim>=0; lim--)
	 {
		 CircleCell c = empty.elementAt(lim);
		 some |= addLegalMoves(all,c,top,who);
		 if(some && all==null) { return true; }
	 }
	 return some;
 }
 
 private boolean addLegalMoves(CommonMoveStack all,CircleCell c,CircleChip top,int who)
 {	
	 if(isLegalMove(c,top))
 		{
	 	  if(all==null) { return true; }
	 	  //if(robot!=null) { verifyBlobSize(c); }
	 	  all.push(new CircleMovespec(MOVE_DROPB,c.col,c.row,who));
	 	  return true;
	 	}
 	return false;
 }
 // legal if empty and makes a blob of size 4 or less
 public boolean isLegalMove(CircleCell c,CircleChip top)
 {
		 Critter adj1 = null;
		 Critter adj2 = null;
		 Critter adj3 = null;
		 int totalsize = 0;
		 for(int dir = 0;dir<6 && totalsize<=3; dir++)
		 {
			 CircleCell adj = c.exitTo(dir);
			 if(adj!=null)
			 {
			 Critter cr = adj.critter();
			 if(cr!=null && cr!=adj1 && cr!=adj2 && cr!=adj3)
			 {	adj1 = adj2;
			 	adj2 = adj3;
			 	adj3 = cr;
				 if(cr.top==top) 
				 	{ totalsize+= cr.size(); 
				 	
				 	}
		 }
	 }
 }
		 return totalsize<=3;
}
 /*
 int sweep_counter = 1;
 private int countBlobSize(CircleCell from,CircleChip top,int sweep)
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
 /** this is an independent check of the blob structure, for debugging 
 private void verifyBlobSize(CircleCell from)
 {	int totalsize = 0;
 	int sweep = ++sweep_counter;
 	CircleChip top = getPlayerChip(whoseTurn);
 	G.print("\nadj to "+from);
 	for(int i=0;i<6;i++)
 	{	CircleCell adj = from.exitTo(i);
 		int size = countBlobSize(adj,top,sweep);
 		totalsize +=size;
 		G.Assert(size==0 || size==adj.myCritter.size(),"wrong size");
 		G.Assert(totalsize<=3,"totalsize too many");
 		}
 }
 */
 public boolean hasMoves()
 {
	 return addLegalMoves(null,whoseTurn);
 }
 public commonMove getRandomMove(Random r)
 {
	 int sz = emptyCells.size();
	 int rx = r.fastUpto(sz);
	 CircleChip top = getPlayerChip(whoseTurn);
	 for(int i=0;i<sz;i++)
	 {
		 CircleCell c = emptyCells.elementAt((i+rx)%sz);
		 if(isLegalMove(c,top))
		 {
			 return new CircleMovespec(MOVE_DROPB,c.col,c.row,whoseTurn);
		 }
	 }
	 return null;
 }
 
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();

 	switch(board_state)
 	{
 	case Puzzle:
 		if(pickedObject==null) { addLegalMoves(all,whoseTurn); }
 		else { addLegalMoves(all,playerIndex(pickedObject)); }
 		for(CircleCell c = allCells; c!=null; c=c.next){
 			{
 				if(c.topChip()!=null) { all.push(new CircleMovespec(MOVE_PICKB,c.col,c.row,whoseTurn));}
 			}
 		}
 		break;
 	case Play:
 		addLegalMoves(all,whoseTurn);
 		break;
 	case Resign:
 	case Confirm:
 		all.push(new CircleMovespec(MOVE_DONE,whoseTurn));
 		break;
 	case Gameover:
 		break;
 	default:
 			G.Error("Not expecting state ",board_state);
 	}
 	return(all);
 }
 
 public void initRobotValues(CirclePlay m)
 {	robot = m;
 }

 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(Character.isDigit(txt.charAt(0)))
	 	{ switch(variation)
	 		{
	 		case CircleOfLife:
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
 public Hashtable<CircleCell, CircleMovespec> getTargets() 
 {
 	Hashtable<CircleCell,CircleMovespec> targets = new Hashtable<CircleCell,CircleMovespec>();
 	CommonMoveStack all = GetListOfMoves();
 	for(int lim=all.size()-1; lim>=0; lim--)
 	{	CircleMovespec m = (CircleMovespec)all.elementAt(lim);
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
 			&& ((board_state==CircleState.Play) || (board_state==CircleState.DrawPending))
 			&& (moveNumber-lastDrawMove>4);
 			*/
 //}

 // most multi player games can't handle individual players resigning
 // this provides an escape hatch to allow it.
 //public boolean canResign() { return(super.canResign()); }
}
