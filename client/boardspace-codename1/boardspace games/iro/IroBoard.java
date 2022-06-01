package iro;


import static iro.Iromovespec.*;

import java.util.*;

import iro.IroChip.IColor;
import lib.*;
import lib.Random;
import online.game.*;

/**
 * StymieBoard knows all about the game of Prototype, which is played
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

class IroBoard 
	extends rectBoard<IroCell>	// for a square grid board, this could be rectBoard or squareBoard 
	implements BoardProtocol,IroConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
	
	boolean robotBoard = false;				// set to true if this is a copy of the board being used by the robot
	private Random robotRandom = new Random();		// a source of randomness for the robot
	private StateStack robotState = new StateStack();
	private IStack robotStack = new IStack();
	
	static final int nCols =6;
	static final int nRows = 8;	
	static final int ChipsPerPlayer = IroChip.bchips.length;	
	static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	IroVariation variation = IroVariation.iro;
	private IroState board_state = IroState.Puzzle;	
	private IroState unresign = null;	// remembers the orignal state when "resign" is hit
	public IroState getState() { return(board_state); }
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(IroState st) 
	{ 	unresign = (st==IroState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

    private IroId playerColor[]={IroId.White,IroId.Black};    
    private IroChip playerChip[]={IroChip.wchips[0],IroChip.bchips[0]};
    private IroCell playerCell[]=new IroCell[2];
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public IroChip getPlayerChip(int p) { return(playerChip[p]); }
	public IroId getPlayerColor(int p) { return(playerColor[p]); }
	public IroCell getPlayerCell(int p) { return(playerCell[p]); }
	public IroCell getPlayerCell(IroChip ch) { return (ch.id==playerColor[0]?playerCell[0]:playerCell[1]);}
	public IroCell getOtherPlayerCell(IroChip ch) { return (ch.id==playerColor[1]?playerCell[0]:playerCell[1]);}
	public IroChip getCurrentPlayerChip() { return(playerChip[whoseTurn]); }
    private CellStack[] occupiedCells = { new CellStack(),new CellStack() };

	public void SetDrawState() { setState(IroState.Gameover); };	
	
	CellStack animationStack = new CellStack();

    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public IroChip pickedObject = null;
    public IroChip lastPicked = null;
    private IroCell blackChipPool = null;	// dummy source for the chip pools
    private IroCell whiteChipPool = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private IStack pickedIndex = new IStack();
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    
    // save strings to be shown in the game log
    StringStack gameEvents = new StringStack();
    private InternationalStrings s = G.getTranslations();
    
 	void logGameEvent(String str,String... args)
 	{	//if(!robotBoard)
 		{String trans = s.get(str,args);
 		 gameEvents.push(trans);
 		}
 	}

    private IroState resetState = IroState.Puzzle; 
    public DrawableImage<?> lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public IroCell newcell(char c,int r)
	{	return(new IroCell(IroId.BoardLocation,c,r));
	}
	
	// constructor 
    public IroBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = GRIDSTYLE;
        setColorMap(map);
        
		Random r = new Random(734687);
		// do this once at construction
	    blackChipPool = new IroCell(r,IroId.Black);
	    whiteChipPool = new IroCell(r,IroId.White);

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
    //
    // place a tile, which covers 4 cells.  "c" is the upper-left cell
    //
    private void placeTile(IroCell c,IroChip tile,int rotation)
    {	G.Assert(c.isTileAnchor,"must be an anchor cell");
		IroCell c00 = c.exitTo(CELL_DOWN);
		IroCell c11 = c.exitTo(CELL_RIGHT);
		IroCell c01 = c.exitTo(CELL_DOWN_RIGHT);
		c.tile = c00.tile = c11.tile = c01.tile = tile;
		c.rotation = c00.rotation = c11.rotation = c01.rotation = rotation;
		
		c.color = tile.colors[(0+rotation)%4];
		c11.color = tile.colors[(1+rotation)%4];
		c01.color = tile.colors[(2+rotation)%4];
		c00.color = tile.colors[(3+rotation)%4];
    }
    
    
    // check the top and bottom row tiles, must have one of each color
	boolean validRowColors(int row)
	{	int ncolors = 0;
		int colors = 0;
		IroCell c = getCell('A',row);
		while(c!=null)
		{
			int bit = 1<<c.color.ordinal();
			if((bit&colors)==0) { ncolors++; colors |= bit; }
			c = c.exitTo(CELL_RIGHT);
		}
		return(ncolors==4);
	}
	
	// randomly permute the current tiles until the board is valid.
    private void makeValidRow(Random r,int row)
    {
    	while(!validRowColors(row))
    	{
    		int tile = r.nextInt(nCols/2);
    		int newrot = r.nextInt(3);
    		IroCell c = getCell((char)('A'+tile*2),((row&1)==0) ? row : row+1);
    		placeTile(c,c.tile,(c.rotation+newrot+1)%4);
    	}
    }
    //
    // create an initial valid board covered with tiles.
    //
    private void randomInitialBoard()
    {
    	Random r = new Random(randomKey+2252463);
    	ChipStack tiles = new ChipStack();
    	for(IroChip c : IroChip.tiles) { tiles.push(c);  tiles.push(c); }
    	tiles.shuffle(r);
    	// note that this doesn't use iterators, because we always want to do it 
    	// in the same order.
    	for(int coln = nCols-1; coln>=0;coln--)
    	{
    		for(int row = nRows; row>=1; row--)
    			
    		{
    			IroCell c = getCell((char)('A'+coln),row);
    		if(c.isTileAnchor)
    		{
    			placeTile(c,tiles.pop(),r.nextInt(4));   
    		}
    	}
    	}

    	makeValidRow(r,playerFirstRow(FIRST_PLAYER_INDEX));
    	makeValidRow(r,playerFirstRow(SECOND_PLAYER_INDEX));
    }
    //
    // return the first row for a player, ie; 1 or 8
    //
    private int playerFirstRow(int n)
    {
    	return ((n==FIRST_PLAYER_INDEX) ? 1 : nrows);
    }
    //
    // true if "c" is on the player's side of the board
    //
    private boolean isOnPlayerSide(IroCell c,int p) 
    { return (p==FIRST_PLAYER_INDEX
    			? (c.row<=nrows/2)
    			: (c.row>nrows/2));
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int players,int rev)
    {	randomKey = key;
    	adjustRevision(rev);
    	players_in_game = players;
    	win = new boolean[players];
 		setState(IroState.Puzzle);
		variation = IroVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		robotStack.clear();
		gametype = gtype;
		pickedIndex.clear();
		
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case iro:
			// using reInitBoard avoids thrashing the creation of cells 
			// when reviewing games.
			reInitBoard(nCols,nRows);
			for(IroCell cell = allCells; cell!=null; cell=cell.next)
				{ 
	        	  if(((cell.row&1)==0) && ((cell.col&1)==1))	// mark the anchor cells
	        	  {	cell.isTileAnchor = true; }
	        	  
	          }
		}

		
	    int map[]=getColorMap();
		playerColor[map[0]]=IroId.White;
		playerColor[map[1]]=IroId.Black;
		playerChip[map[0]]=IroChip.wchips[0];
		playerChip[map[1]]=IroChip.bchips[0];
	    playerCell[map[0]] = whiteChipPool; 
	    playerCell[map[1]] = blackChipPool; 
		whiteChipPool.reInit();
		blackChipPool.reInit();
		
		randomInitialBoard();	// cover with tiles
	    reInit(occupiedCells);
	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    
	    acceptPlacement();

	    resetState = null;
	    lastDroppedObject = null;
		for(IroChip ch : IroChip.bchips) { blackChipPool.addChip(ch); }
		for(IroChip ch : IroChip.wchips) { whiteChipPool.addChip(ch); }
		
        animationStack.clear();
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }

    /** create a copy of this board */
    public IroBoard cloneBoard() 
	{ IroBoard dup = new IroBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((IroBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(IroBoard from_b)
    {
        super.copyFrom(from_b);
        robotState.copyFrom(from_b.robotState);
        robotStack.copyFrom(from_b.robotStack);
        
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        copyFrom(whiteChipPool,from_b.whiteChipPool);		// this will have the side effect of copying the location
        copyFrom(blackChipPool,from_b.blackChipPool);		// from display copy boards to the main board
        getCell(playerCell,from_b.playerCell);
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        lastPicked = null;
        pickedIndex.copyFrom(from_b.pickedIndex);
        AR.copy(playerColor,from_b.playerColor);
        AR.copy(playerChip,from_b.playerChip);
        getCell(occupiedCells,from_b.occupiedCells);
        robotBoard = from_b.robotBoard;
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((IroBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(IroBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor mismatch");
        G.Assert(AR.sameArrayContents(playerChip,from_b.playerChip),"playerChip mismatch");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedsourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(sameCells(playerCell,from_b.playerCell),"player cell mismatch");
        G.Assert(sameContents(pickedIndex,from_b.pickedIndex),"pickedIndex mismatch");
        G.Assert(occupiedCells[0].size()==from_b.occupiedCells[0].size(),"occupied 0 mismatch");
        G.Assert(occupiedCells[1].size()==from_b.occupiedCells[1].size(),"occupied 1 mismatch");
        
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        long d1 = Digest();
        long d2 = from_b.Digest();
        G.Assert(d1==d2,"Digest matches");

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
        long v = super.Digest();
		// many games will want to digest pickedSource too
		// v ^= cell.Digest(r,pickedSource);
		v ^= chip.Digest(r,playerChip[0]);	// this accounts for the "swap" button
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,pickedIndex);
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
        	throw G.Error("Move not complete, can't change the current player in state ",board_state);
        case Puzzle:
            break;
        case Play:
        	// some damaged games have 2 dones in a row
        	if(replay==replayMode.Live) { throw G.Error("Move not complete, can't change the current player in state ",board_state); }
			//$FALL-THROUGH$
        case ConfirmSetup:
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
    // more selecteive.  This determines if the current board digest is added
    // to the repetition detection machinery.
    public boolean DigestState()
    {	
    	return(board_state.digestState());
    }



    public boolean gameOverNow() { return(board_state.GameOver()); }

    // maintain the per-player occupied cells list
    private void removeFromOccupied(IroCell c,IroChip chip)
    {
    	CellStack occ = occupiedCells[chip.playerIndex];
    	occ.remove(c,false);
    }
    // maintain the per-player occupied cells list
       private void addToOccupied(IroCell c,IroChip chip)
    {
    	CellStack occ = occupiedCells[chip.playerIndex];
    	occ.push(c);
    }
    
    // set the contents of a cell, and maintain the books
    public IroChip SetBoard(IroCell c,IroChip ch)
    {	IroChip old = c.topChip();
    	if(old!=null) 
    		{ removeFromOccupied(c,old);
    		  c.removeTop();
    		  // return the old chip to the rack
    		  if(ch!=null) { getPlayerCell(old).addChip(old); }
    		}
       	if(ch!=null) { c.addChip(ch); addToOccupied(c,ch);}
    	return(old);
    }
    //
    // accept the current placements as permanent
    //
    public void acceptPlacement()
    {	
        droppedDestStack.clear();
        pickedSourceStack.clear();
        pickedIndex.clear();
        stateStack.clear();
        pickedObject = null;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private IroCell unDropObject()
    {	IroCell rv = droppedDestStack.pop();
    	setState(stateStack.pop());
    	IroId rack = rv.rackLocation();
    	switch(rack)
    	{
    	case BoardLocation:
    	case EmptyBoard:
    		pickedObject = SetBoard(rv,null); 	// SetBoard does ancillary bookkeeping
    		break;
    	case Black:
    	case White:
    		{
    		int row = pickedIndex.pop();
    		if(row>=0) { pickedObject = rv.removeChipAtIndex(row); }
    		else { pickedObject = rv.removeTop(); }
    		}
    		break;
    	default: G.Error("not expecting %s",rack); 
    	}
    	
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	IroCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	IroId rack = rv.rackLocation();
    	switch(rack)
    	{
    	case BoardLocation:
    	case EmptyBoard:
    		if(pickedObject.id==IroId.Tile)
    		{	rv.isPicked = false;
    			pickedObject = null;
    		}
    		else
    		{
    		SetBoard(rv,pickedObject);
    		}
    		break;
    	case White:
    	case Black:
    		{
    		int row = pickedIndex.pop();
    		if(row>=0) { rv.insertChipAtIndex(row,pickedObject); }
    		else { rv.addChip(pickedObject); }
    		}
    		break;
    	default: G.Error("Not expecting %s",rack);
    	}
    	pickedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(IroCell c,int row)
    {
       droppedDestStack.push(c);
       stateStack.push(board_state);
       
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting dest " + c.rackLocation);
        case Black:
        case White:		// back in the pool, we don't really care where
        	if(row>=0) { c.insertChipAtIndex(row,pickedObject); }
        	else { c.addChip(pickedObject); }
        	pickedIndex.push(row);
            break;
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        case EmptyBoard:
        	if(pickedObject.id==IroId.Tile)
        	{
        		IroChip dtile = c.tile;
        		int drot = c.rotation;
        		IroCell src = getSource();
        		placeTile(c,pickedObject,src.rotation);
        		placeTile(src,dtile,drot);   
        		src.isPicked = false;
        		c.isPicked = false;
        	}
        	else
        	{
        		SetBoard(c,pickedObject);   		
        	}
            break;
        }
      	pickedObject = null;
      
    }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(IroCell c)
    {	return(droppedDestStack.top()==c);
    }
    public IroCell getDest()
    {	return(droppedDestStack.top());
    }
 
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { IroChip ch = pickedObject;
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
    private IroCell getCell(IroId source, char col, int row)
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
    public IroCell getCell(IroCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(IroCell c,int row)
    {	pickedSourceStack.push(c);
    	stateStack.push(board_state);
        switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting rackLocation " + c.rackLocation);
        case BoardLocation:
        	{
            lastPicked = pickedObject = c.topChip();
            if(pickedObject==null)
            {	G.Assert(c.isTileAnchor && c.isEmptyTile(),"must be an anchor");
            	pickedObject = c.tile;
            	c.isPicked = true;
            }
         	lastDroppedObject = null;
			SetBoard(c,null);
        	}
            break;

        case Black:
        case White:
        	lastPicked = pickedObject = row>=0 ? c.removeChipAtIndex(row) : c.removeTop();
        	pickedIndex.push(row);
        }
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(IroCell c)
    {	return(c==pickedSourceStack.top());
    }
    public IroCell getSource()
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
        case FirstPlay:
        case InvalidBoard:
        	if(validRowColors(playerFirstRow(whoseTurn)))
        	{
        		if(nCols*2+getPlayerCell(whoseTurn).height()==ChipsPerPlayer) 
        			{ setState(IroState.ConfirmSetup); 
        			}
        		else { setState(IroState.FirstPlay); }
        	}
        	else
        	{	// when the board is invalid, only tile placement moves are available
        		setState(IroState.InvalidBoard);
        	}
        	break;
        case Confirm:
        	setNextStateAfterDone(replay);
         	break;
        case Play:
			setState(IroState.Confirm);
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
    	case FirstPlay:
    	case InvalidBoard:
    	case ConfirmSetup:
    	case Puzzle:
    		if(getPlayerCell(whoseTurn).height()==ChipsPerPlayer)
			{	
				setState(validRowColors(playerFirstRow(whoseTurn)) 
							? IroState.FirstPlay
							: IroState.InvalidBoard );
				return;
			}
			//$FALL-THROUGH$
    	case Confirm:
		case Play:
			setState(IroState.Play);
    		break;
    	}
       	resetState = board_state;
    }
    public boolean winForPlayerNow(int player)
    {
    	return win[player];
    }
    public boolean checkForPlayerWin(int player)
    {
    	IroCell pc = getPlayerCell(nextPlayer[player]);
    	if((pc.height()==ChipsPerPlayer) && ((board_state==IroState.Play) || (board_state==IroState.Confirm)))
    		{
    			return true;		// everything is captured
    		}
    	int row = playerFirstRow(nextPlayer[whoseTurn]);
    	IroId color = getPlayerColor(whoseTurn);
    	IroCell c = getCell('A',row);
    	
    	while(c!=null) 
    	{ IroChip top = c.topChip();
    	  if((top!=null) && (top.id==color)) { return true; }
    	  c = c.exitTo(CELL_RIGHT);
    	}
    	return false;
    }
    private void doDone(replayMode replay)
    {
        acceptPlacement();

        if (board_state==IroState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(IroState.Gameover);
        }
        else
        {	if(checkForPlayerWin(whoseTurn)) 
        		{ win[whoseTurn]=true;
        		  setState(IroState.Gameover); 
        		}
        	else {setNextPlayer(replay);
        		setNextStateAfterDone(replay);
        	}
        }
    }

	
    public boolean Execute(commonMove mm,replayMode replay)
    {	Iromovespec m = (Iromovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }

        //G.print("E "+m+" for "+whoseTurn+" "+state);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone(replay);

            break;

        case MOVE_DROPB:
        	{
			IroChip po = pickedObject;
			IroCell dest =  getCell(IroId.BoardLocation,m.to_col,m.to_row);
			IroCell src = getSource();
			if(dest==src) 
				{ unPickObject(); 
				}
				else 
				{
				IroChip top = dest.topChip();
				m.chip = top;
		           
	            dropObject(dest,m.to_row);
	            /**
	             * if the user clicked on a board space without picking anything up,
	             * animate a stone moving in from the pool.  For Hex, the "picks" are
	             * removed from the game record, so there are never picked stones in
	             * single step replays.
	             */
	            if(replay==replayMode.Single)
	            	{ animationStack.push(src);
	            	  animationStack.push(dest); 
	            	}
	            switch(board_state)
	            {
	            case Puzzle:
	            case InvalidBoard:
	            case FirstPlay:
	            	acceptPlacement();
		            if((po.id==IroId.Tile)&& (replay!=replayMode.Replay))
		            	{
		            		animationStack.push(dest);
		            		animationStack.push(src);
		            	}
		            break;
	            default: break;
	            }
	            setNextStateAfterDrop(replay);
				}
        	}
             break;

        case MOVE_PICK:
 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
 			IroCell src = getCell(m.source,m.to_col,m.to_row);
 			if(isDest(src)) { unDropObject(); }
 			else
 			{
        	// be a temporary p
        	pickObject(src,m.to_row);
        	m.chip = pickedObject;
        	switch(board_state)
        	{
        	case Puzzle:
         		break;
        	case ConfirmSetup:
        		setState(IroState.FirstPlay);
        		break;
        	case Confirm:
        		setState(IroState.Play);
        		break;
        	default: ;
        	}}}
            break;

        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
            IroCell dest = getCell(m.source,m.to_col,m.to_row);
            if(isSource(dest)) { unPickObject(); }
            else 
            	{
		        if(replay==replayMode.Live)
	        	{ lastDroppedObject = pickedObject.getAltDisplayChip(dest);
	        	  //G.print("last ",lastDroppedObject); 
	        	}      	
            	dropObject(dest,m.to_row); 
            
            	}
        	}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            int nextp = nextPlayer[whoseTurn];
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(IroState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=checkForPlayerWin(whoseTurn))
               ||(win[nextp]=checkForPlayerWin(nextp)))
               	{ setState(IroState.Gameover); 
               	}
            else {  setNextStateAfterDone(replay); }

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?IroState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(IroState.Puzzle);
 
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(IroState.Gameover);
    	   break;

       case MOVE_ROTATE:
       		{
       			IroCell dest = getCell(m.to_col,m.to_row);
      			if(robotBoard) { robotStack.push(dest.rotation); }
       			placeTile(dest,dest.tile,m.from_row);
       			setNextStateAfterDrop(replay);
        		}
       		break;
       case MOVE_CAPTURE:
       case MOVE_PLACE:
       case MOVE_FROM_TO:
       case MOVE_SWAPB:
       	{
    	   IroCell from = getCell(m.source,m.from_col,m.from_row);
    	   IroCell to = getCell(m.to_col,m.to_row);
    	   pickObject(from,m.from_row);
    	   m.chip = pickedObject;
    	   m.chip2 = to.topChip();
       	   dropObject(to,m.to_row);
       	   setNextStateAfterDrop(replay);
    	   if(replay!=replayMode.Replay) {
    		   animationStack.push(from);
    		   animationStack.push(to);
    	   switch(m.op)
    	   {
    	   case MOVE_CAPTURE:
    	   case MOVE_SWAPB:
    		   animationStack.push(to);
    		   animationStack.push(from);
    		   break;
    	   default: break;
    	   }}
       	}
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
        case FirstPlay:
        	// for pushfight, you can pick up a stone in the storage area
        	// but it's really optional
        	return(player==whoseTurn);
        case Confirm:
		case Resign:
		case ConfirmSetup:
		case Gameover:
		case InvalidBoard:
			return(false);
        case Puzzle:
        	if(pickedObject!=null)
        	{
        		return(pickedObject.id==getPlayerColor(player));
        	}
        	else
        	{
            return ((pickedObject!=null)?(pickedObject==playerChip[player]):true);
        	}
        }
    }

    public boolean legalToHitBoard(IroCell c,Hashtable<IroCell,Iromovespec> targets )
    {	if(c==null) { return(false); }
        switch (board_state)
        {
        case FirstPlay:
        case InvalidBoard:
        	return targets.get(c)!=null; 
        case Puzzle:
		case Play:
		case ConfirmSetup:
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
    public void RobotExecute(Iromovespec m)
    {	robotBoard = true;
        robotState.push(board_state); //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        Execute(m,replayMode.Replay);
        acceptPlacement();
        if(board_state.doneState()) { doDone(replayMode.Replay); }
       
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(Iromovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	IroState state = robotState.pop();
    	G.Assert(robotBoard,"should be");
        switch (m.op)
        {
        default:
   	    	throw G.Error("Can't un execute " + m);
        case MOVE_RESIGN:
        case MOVE_DONE:
            break;
            
        case MOVE_ROTATE:
        	{
    	    	IroCell from = getCell(m.to_col,m.to_row);
    	    	int rot = robotStack.pop();
    	    	placeTile(from,from.tile,rot);
    	    	acceptPlacement();
        	}
        	break;
        case MOVE_CAPTURE:
	    	{
	    	IroCell from = getCell(m.from_col,m.from_row);
	    	IroCell to = getCell(m.to_col,m.to_row);
	    	pickObject(to,-1);
	    	IroChip po = pickedObject;
	    	dropObject(from,m.from_row);
	    	IroCell rack = getCell(getOtherPlayerCell(po));
	    	pickObject(rack,-1);
	    	dropObject(to,-1);
	    	acceptPlacement();
	    	
	    	}
	    	break;

        case MOVE_FROM_TO:
	    	{
	    	IroCell from = getCell(m.from_col,m.from_row);
	    	IroCell to = getCell(m.to_col,m.to_row);
	    	pickObject(to,-1);
	    	dropObject(from,m.from_row);
	    	acceptPlacement();
	    	}
	    	break;
	    	
        case MOVE_SWAPB:
        	{
        	IroCell from = getCell(m.source,m.from_col,m.from_row);
        	IroCell to = getCell(m.to_col,m.to_row);
        	pickObject(to,-1);
        	dropObject(from,m.from_row);
        	pickObject(from,-1);
        	dropObject(to,-1);
        	acceptPlacement();
        	}
	    	break;
	        
        case MOVE_PLACE:
        	{
        	IroCell from = getCell(m.source,m.from_col,m.from_row);
        	IroCell to = getCell(m.to_col,m.to_row);
        	pickObject(to,-1);
        	dropObject(from,m.from_row);
        	acceptPlacement();
        	}
            break;
        }
        setState(state);
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
 
 private void addTileSwapMoves(CommonMoveStack all,int who,boolean any)
 {	
   	 for(IroCell c = allCells; c!=null; c=c.next)
   	 {
   		 if(c.isTileAnchor && c.isEmptyTile() && (any || isOnPlayerSide(c,who)))
   		 {
   		 for(int i=0;i<4;i++) { if(i!=c.rotation) { all.push(new Iromovespec(MOVE_ROTATE,c,i,who)); }}
   		 addTileSwapMoves(all,c,who,any);
   		 }
   	 }}
 //
 // this is called from the UI with something picked up, so it can drop anywhere
 //
 private void addTileSwapMoves(CommonMoveStack all,IroCell c,int who,boolean any)
 {
	 for(IroCell d = allCells; d!=null; d=d.next)
	 {
		 if(d!=c 
			&& d.isTileAnchor 
			&& d.isEmptyTile() 
			&& (( c.tile!=d.tile)|| (c.rotation!=d.rotation))
			&& (any || isOnPlayerSide(d,who))
			)
		 {
			 all.push(new Iromovespec(MOVE_FROM_TO,c,d,who,0));
		 }
	 }
 }
 
 // place chips from the player reserve to the two starting rows
 private void addChipPlacementMoves(CommonMoveStack all,int who)
 {
   	 for(int rown = 1; rown<=2; rown++)
   	 {
   		 int row = who==0 ? rown : nRows-rown+1;
   		 IroCell c = getCell('A',row);
   		 int n = 0;
   		 while(c!=null) 
   		 	{ if(c.isEmpty())
   		 		{ addChipPlacementMoves(all,c,who);  
   		 		  n++;
   		 		  if(robotBoard && n==3) { return; }	// robot only needs to try one at a time
   		 		}
   		 	  c = c.exitTo(CELL_RIGHT); 
   		 	}
   	 }
 }
 // place chips on a particular starting cell
 private void addChipPlacementMoves(CommonMoveStack all,IroCell c,int who)
 {	IroCell from = getPlayerCell(who);
	for(int lim= from.height()-1; lim>=0; lim--)
	{
		all.push(new Iromovespec(MOVE_PLACE,from,lim,c,who,0));
	}
 }
 // place a particular reserve chip
 private void addChipPlacementMoves(CommonMoveStack all,IroCell from,int index,int who)
 {	   
	 for(int rown = 1; rown<=2; rown++)
	 {
		 int row = who==0 ? rown : nRows-rown+1;
		 IroCell c = getCell('A',row);
		 while(c!=null) 
		 	{
			 if(c.isEmpty()) { all.push(new Iromovespec(MOVE_PLACE,from,index,c,who,0)); }  
		 	 c = c.exitTo(CELL_RIGHT); 
		 	}
	 }

 }
 
 // add moves that swap a piece with one from the rack
 private void addPieceSwapMoves(CommonMoveStack all,IroCell from,IroChip top,int idx,int who)
 {
	 CellStack occ = occupiedCells[who];
	 for(int lim = occ.size()-1; lim>=0; lim--)
	 {	IroCell c = occ.elementAt(lim);
	 	IroChip to = c.topChip();
		G.Assert(to.id==top.id,"not the same");
		all.push(new Iromovespec(MOVE_SWAPB,from,idx,c,who,0.25)) ;
	 }
 }
 
 // get moves when something has been picked up. 
 CommonMoveStack getListOfMovesFrom(IroCell from,IroChip picked,int index,int who)
 {
	 CommonMoveStack all = new CommonMoveStack();
	 switch(board_state)
	 {
	 default: throw G.Error("Not expecting state %s",board_state);
	 case ConfirmSetup:
	 case Confirm:
		 all.push(new Iromovespec(MOVE_DONE,who));
		 break;
	 case Gameover:
		 break;
	 case Play:
		 if(from.rackLocation()==IroId.BoardLocation)
			 { addPieceMoves(all,from,picked,who);
			 }
		 else {
			 addPieceSwapMoves(all,from,picked,index,who);
			 }
		 break;
	 case InvalidBoard:
	 case Puzzle:
	 case FirstPlay:
		 if(picked.id==IroId.Tile)
		 {
			 addTileSwapMoves(all,from,who,board_state==IroState.Puzzle);
		 }
		 else {
			 addChipPlacementMoves(all,from, index,who);
		 }
		 break;
	 }
	 return(all);
 }

 
 // add piece movement moves from a particular starting cell, top is the chip that
 // is moving, which might or might not be present in the cell.
 private void addPieceMoves(CommonMoveStack all,IroCell from,IroChip top,int who)
 {
	 addPieceMoves(all,from,from,from,top,0,who);	// 3 unused color positions
	 // add the piece swap moves
	 IroCell to = getPlayerCell(who);
	 if(robotBoard)
	 {	// add one random swap so it will be a possibility but not overwhelming
		int idx = robotRandom.nextInt( to.height());
		all.push(new Iromovespec(MOVE_SWAPB,to,idx,from,who,0.25));
		int idx2 = robotRandom.nextInt( to.height());
		if(idx2!=idx)
		{
		all.push(new Iromovespec(MOVE_SWAPB,to,idx2,from,who,0.25));
		}
	 }
	 else
	 {
	 for(int lim=to.height()-1; lim>=0; lim--)
	 {
		 all.push(new Iromovespec(MOVE_SWAPB,to,lim,from,who,0));
	 }}
 }
 // add piece movement moves from a particular starting cell, top is the chip that
 // is moving, which might or might not be present in the cell.
 private int countPieceMoves(IroCell from,IroChip top,int who)
 {	int nMoves = 0;
	 nMoves += countPieceMoves(from,from,from,top,0,who);	// 3 unused color positions
	 return nMoves;
	 
 }
 
 // mask is of the unused positions in the piece color map.  Start is the staring cell,from is the current intermediate cell
 private boolean addPieceMoves(CommonMoveStack all,IroCell start,IroCell mid,IroCell from,IroChip top,int mask,int who)
 {	IroId playerId = getPlayerChip(who).id;
	IColor topC[] = top.colors;
	int nColors = topC.length;
	boolean some = false;
	int allMask = (1<<nColors)-1;
	for(int dir = 0,last = from.geometry.n; dir<last; dir++)
	{
		IroCell adj = from.exitTo(dir);
		if((adj!=null) && (adj!=mid) && (adj!=start))
		{	IColor adjColor = adj.color;
			boolean matched = false;
			for(int i=0,bit=1;!matched && i<nColors;i++,bit=bit<<1) 
				{ 
				if( (topC[i]==adjColor) && ((mask&bit)==0))
				{	matched = true;
					int nextMask = mask|bit;
					IroChip atop = adj.topChip();
					if(atop==null)
						{ 
						if(nextMask==allMask)
						{
							all.push(new Iromovespec(MOVE_FROM_TO,start,adj,who,0.5)); 
							some = true;
						}
						else {
							// non terminal move, less likely to be correct
							boolean some2 = addPieceMoves(all,start,mid==start?adj:mid,adj,top,nextMask,who);
							all.push(new Iromovespec(MOVE_FROM_TO,start,adj,who,some2 ? 0.5 :0.25)); 
							some = true;
						}
						}
					else if(atop.id==playerId)
					{	// one of ours, not allowed to stop
						if(nextMask!=allMask) 
							{ some |= addPieceMoves(all,start,mid==start?adj:mid,adj,top,nextMask,who); 
							}
					}
					else 
					{	// one of opponents, must stop
						all.push(new Iromovespec(MOVE_CAPTURE,start,adj,who,1.0)); 
						some = true;
					}
					}
				}
		}
	}
	return some;
 }
 // mask is of the unused positions in the piece color map.  Start is the staring cell,from is the current intermediate cell
 private int countPieceMoves(IroCell start,IroCell mid,IroCell from,IroChip top,int mask,int who)
 {	int nMoves = 0;
 	IroId playerId = getPlayerChip(who).id;
	IColor topC[] = top.colors;
	int nColors = topC.length;
	int allMask = (1<<nColors)-1;
	for(int dir = 0,last = from.geometry.n; dir<last; dir++)
	{
		IroCell adj = from.exitTo(dir);
		if((adj!=null) && (adj!=mid) && (adj!=start))
		{	IColor adjColor = adj.color;
			boolean matched = false;
			for(int i=0,bit=1;!matched && i<nColors;i++,bit=bit<<1) 
				{ 
				if( (topC[i]==adjColor) && ((mask&bit)==0))
				{	matched = true;
					int nextMask = mask|bit;
					IroChip atop = adj.topChip();
					if(atop==null)
						{ 
						if(nextMask==allMask)
						{
							nMoves++;
						}
						else {
							// non terminal move, less likely to be correct
							nMoves+=countPieceMoves(start,mid==start?adj:mid,adj,top,nextMask,who);
							nMoves++;
						}
						}
					else if(atop.id==playerId)
					{	// one of ours, not allowed to stop
						if(nextMask!=allMask) 
							{ nMoves += countPieceMoves(start,mid==start?adj:mid,adj,top,nextMask,who); 
							}
					}
					else 
					{	// one of opponents, must stop
						nMoves++;
					}
					}
				}
		}
	}
	return nMoves;
 }
 
 //
 // this is used by get random move, it returns just one move instead of a list.
 // mask is of the unused colors
 // dir is the initial direction
 // step is the max number of move steps (so sometimes we stop short of 3)
 //
 private Iromovespec getFirstPieceMove(int direction,int step,IroCell start,IroCell mid,IroCell from,IroChip top,int mask,int who)
 {	IroId playerId = getPlayerChip(who).id;
	IColor topC[] = top.colors;
	int nColors = topC.length;
	int allMask = (1<<nColors)-1;
	int nDirections = from.geometry.n;
	for(int dir = direction,last = direction+nDirections; dir<last; dir++)
	{
		IroCell adj = from.exitTo(dir);
		if((adj!=null) && (adj!=mid) && (adj!=start))
		{	IColor adjColor = adj.color;
			boolean matched = false;
			for(int i=0,bit=1; !matched && i<nColors; i++,bit=bit<<1)
			{
			if(topC[i]==adjColor && ((bit&mask)==0))	// an unused color
			{
				IroChip atop = adj.topChip();
				matched = true;
				int nextMask = mask|bit;
				if(atop==null)
					{ 
					  if(step>0 && nextMask!=allMask)
					  	{ Iromovespec m = getFirstPieceMove(robotRandom.nextInt(nDirections),step-1,start,mid==start?adj:mid,adj,top,nextMask,who);
					  	  if(m!=null) { return m;}
					  	}
					  return new Iromovespec(MOVE_FROM_TO,start,adj,who,0.5); 
					}
				else if(atop.id==playerId)
				{	// one of ours, not allowed to stop
					if((step>0) && (nextMask!=allMask))
					{
					Iromovespec m = getFirstPieceMove(robotRandom.nextInt(nDirections),step-1,start,mid==start?adj:mid,adj,top,nextMask,who);
					if(m!=null) { return m;}
					}
				}
				else 
				{	// one of opponents, must stop
					return new Iromovespec(MOVE_CAPTURE,start,adj,who,0.5); 
				}
			}}
		}
 	}
 	return null;
 }
 
 
 private void addPieceMoves(CommonMoveStack all,int who)
 {	
 	CellStack occ = occupiedCells[who];
 	for(int lim=occ.size()-1; lim>=0; lim--)
 	{	IroCell c = occ.elementAt(lim);
 		IroChip top = c.topChip();
 		G.Assert(top.playerIndex==who,"player mismatch");
 		addPieceMoves(all,c,top,who);
	 }
 }
 
 private int countPieceMoves(int who)
 {	int nMoves = 0;
 	CellStack occ = occupiedCells[who];
 	for(int lim=occ.size()-1; lim>=0; lim--)
 	{	IroCell c = occ.elementAt(lim);
 		IroChip top = c.topChip();
 		G.Assert(top.playerIndex==who,"player mismatch");
 		nMoves += countPieceMoves(c,top,who);
	 }
 	return nMoves;
 }
 
 CommonMoveStack  GetListOfMoves()
 {	robotBoard = true;
 	return getListOfMoves(whoseTurn);
 }
 private CommonMoveStack getListOfMoves(int who)
 {	CommonMoveStack all = new CommonMoveStack();
 	switch(board_state)
 	{
 	case FirstPlay:
 		addChipPlacementMoves(all,who);
 		// the robot only considers rearranging the board at the beginning
 		if(robotBoard && getPlayerCell(who).height()<ChipsPerPlayer) { break; }
		//$FALL-THROUGH$
	case InvalidBoard:
 		addTileSwapMoves(all,who,false);
 		break;
 	case Puzzle:
 		
 		addTileSwapMoves(all,who,true);
 		break;
 	case Play:
 		addPieceMoves(all,who);
 		// if all our pieces are gone, resign.  This can happen sort of reasonably
 		// in random playouts.
 		if(all.size()==0) { all.push(new Iromovespec(MOVE_RESIGN,who)); }
 		break;
 	case Confirm:
 	case ConfirmSetup:
 	case Resign:
 		all.push(new Iromovespec(MOVE_DONE,who));
 		break;
 	case Gameover:
 		break;
 	default:
 			G.Error("Not expecting state ",board_state);
 	}
 	return(all);
 }
 
 public void initRobotValues()
 {

 }
 private void addSetupMoves(CommonMoveStack all,int who)
 {
	 switch(board_state)
	 {
	 default: break;
	 case ConfirmSetup:
	 case FirstPlay:
	 case Puzzle:
	 case InvalidBoard:
	 {
		 IroId pid = getPlayerColor(who);
		 IroCell d = getPlayerCell(who);
		 for(IroCell c = allCells; c!=null; c=c.next)
		 {
			 IroChip chip = c.topChip();
			 if((chip!=null) && (chip.id==pid))
			 {	// add moves to take chips back
				 all.push(new Iromovespec(MOVE_FROM_TO,c,d,who,0));
			 }
		 }
		 break;
	 }}
 }

 /**
  *  get the board cells that are valid targets right now
  * @return
  */
 public Hashtable<IroCell, Iromovespec> getTargets() 
 {
 	Hashtable<IroCell,Iromovespec> targets = new Hashtable<IroCell,Iromovespec>();
 	if(pickedObject==null)
 	{
 	CommonMoveStack all = getListOfMoves(whoseTurn);
 	addSetupMoves(all,whoseTurn);
	for(int lim=all.size()-1; lim>=0; lim--)
 	{	Iromovespec m = (Iromovespec)all.elementAt(lim);
 		switch(m.op)
 		{
 		case MOVE_PICK:
 		case MOVE_ROTATE:
 		case MOVE_PICKB:
 			targets.put(getCell(m.to_col,m.to_row),m);
 			break;
 		case MOVE_SWAPT:
 		case MOVE_DONE:
 		case MOVE_RESIGN:
 			break;
 		case MOVE_PLACE:
 		case MOVE_SWAPB:
 			targets.put(getCell(m.source,m.from_col,m.from_row),m);
 			break;
 		case MOVE_FROM_TO:
 		case MOVE_CAPTURE:
 			targets.put(getCell(m.from_col,m.from_row),m);
 			break;
 			
 		default: G.Error("Not expecting "+m);
 		
 		}}
 	
 	}
 	else
 	{	// picked something
 		CommonMoveStack all = getListOfMovesFrom(getSource(),pickedObject,pickedIndex.topz(-1),whoseTurn);
 		for(int lim=all.size()-1; lim>=0; lim--)
 	 	{	Iromovespec m = (Iromovespec)all.elementAt(lim);
 	 		switch(m.op)
 	 		{
 	 		case MOVE_DROPB:
 	 			targets.put(getCell(m.to_col,m.to_row),m);
 	 			break;
 	 		case MOVE_SWAPB:
 	 			targets.put(getCell(m.to_col,m.to_row),m);
 	 			break;
 	 		case MOVE_DONE:
 	 		case MOVE_RESIGN:
 	 			break;
 	 		case MOVE_FROM_TO:
 	 		case MOVE_CAPTURE:
 	 		case MOVE_PLACE:
 	 			targets.put(getCell(m.to_col,m.to_row),m);
 	 			break;
 	 			
 	 		default: G.Error("Not expecting "+m);
 	 		
 	 		}}	
 	}
 	
 	return(targets);
 }
 //
 // used in MCTS search.
 // this is a fast and dirty approximation to a random move generator.  It first 
 // selects a victim tile, then selects a random direction and a first available 
 // move starting there.  If no moves are generated, or 10% of the time, it will
 // generate a random swap move instead of a random move-move
 //
 // in setup or gameover state this returns null, and lets a true random be used.
 //
public commonMove getRandomMove(Random rand)
{
	Iromovespec m = null;
	
	if(board_state==IroState.Play)
	{
	CellStack occupied = occupiedCells[whoseTurn];
	int npieces =occupied.size();
	if(npieces>0)	// at the end, it's possible everything has been captured.
	{
	
	boolean swap = rand.nextInt(100)<10;		// 10% swap moves
	int idx = rand.nextInt(npieces);
	IroCell c = occupied.elementAt(idx);
	
	
	if(!swap)
	{
	int step = rand.nextInt(5);		// usually step all the way, occasionally stop short
	int direction = rand.nextInt(c.geometry.n);	
	IroChip top = c.topChip();
	int origin = playerFirstRow(whoseTurn);
	int distance = 0;
	for(int i=0;i<4;i++)
	{
		Iromovespec nextm =getFirstPieceMove(direction,step,c,c,c,top,0,whoseTurn);
		if(nextm == null) { break; }
		int newdistance = nextm.to_row-origin;
		if(nextm.op==MOVE_CAPTURE) { m = nextm; distance = newdistance; break; }
		if((m==null) || (newdistance>distance)) { m = nextm; distance = newdistance; }
	}}
	
	if(m==null)	// no standard moves or we decided to swap
	{
		IroCell rack = getPlayerCell(whoseTurn);
		int from = rand.nextInt(rack.height());
		m  = new Iromovespec(MOVE_SWAPB,rack,from,c,whoseTurn,0.5);
	}
	}}
	return m;
	
}
// position weight values advancement toward the goal
// wood weight values keeping pieces alive
public double simpleScore(int player,double position_weight,double wood_weight) 
{
	// value in 0-1 which indicates value of position
	CellStack occ = occupiedCells[player];
	int nleft = occ.size();
	double wood = (double)(nleft*wood_weight)/(nCols*2);// 0-0.5 for the wood
	//G.Assert(wood<=wood_weight,"wood bug");
	switch(board_state)
	{
	case FirstPlay:
	case InvalidBoard:
	case ConfirmSetup:
		{
		// use the number of available moves as a proxy for good position
		CellStack c = occupiedCells[player];
		int cs = c.size();
		if(cs==0) { return 0; }
		int nMoves = countPieceMoves(player);
		return (double)nMoves;
		}
	default:
		if(nleft>0 && position_weight>0)
		{
		double rowsum = 0;
		int frow = playerFirstRow(player);
		double quant = position_weight/(nleft*nRows);
		while(--nleft >= 0)
		{
			IroCell c = occ.elementAt(nleft);
			rowsum += Math.abs(c.row-frow);	
		}
		double position = quant*rowsum;
		//G.Assert(position<position_weight,"position bug");
		wood += position;
		}
		return (wood);
	}}
}
