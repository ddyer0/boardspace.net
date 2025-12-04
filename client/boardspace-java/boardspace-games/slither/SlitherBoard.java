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

import java.awt.Color;
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
    
    //
    // these are treated differently from all the rest.  It mostly matters
    // for the rare case where there is no legal "drop" move but there are
    // legal "slide" moves, and the slide move creates a new legal drop move.
    //
    public SlitherCell moveFrom = null;
    public SlitherCell slideTo = null;
    public SlitherCell dropAt = null;
    
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
    
    public SlitherCell validWithRepair(SlitherChip top,SlitherCell from,SlitherCell to)
    {
    	int from_row = Math.max(1,Math.min(from.row-1,to.row-1));
    	int to_row = Math.min(nrows,Math.max(from.row+1,to.row+1));
    	char from_col = (char)Math.max('A',Math.min(from.col-1,to.col-1));
    	char to_col = (char)Math.min('A'+ncols-1,Math.max(from.col+1,to.col+1));
    	for(int row = from_row; row<=to_row; row++)
    	{
    		for(char col = from_col; col<to_col; col++)
    		{
    			SlitherCell add = getCell(col,row);
    			if(add==from || add.topChip()==top)
    			{
    				validRange(from_col,from_row,to_col,to_row,top,from,to,add);
    			}
    		}
    	}
    	return null;
    }
    /**
     * valid to place if some specific repair move is made
     * @param top
     * @param dropped
     * @param to
     * @return
     */
    public boolean addValidSlideRepairMoves(CommonMoveStack all,SlitherChip top,SlitherCell picked,SlitherCell dropped)
    {	boolean some = false;
    	for(int direction = CELL_LEFT; direction<=CELL_LEFT+CELL_FULL_TURN; direction+=CELL_QUARTER_TURN)
    	{
    		SlitherCell to = dropped.exitTo(direction);
    		if(to!=null && to.topChip()==null)
    		{	
	    		{	// something moves in to be orthogonal
	    			for(int moveDirection=CELL_LEFT; moveDirection<CELL_LEFT+CELL_FULL_TURN; moveDirection++)
	    			{
	    				SlitherCell from = to.exitTo(moveDirection);
	    				if(from!=null 
	    						&& ((picked==null) ? from.topChip()==top : picked==from)
	    						&& from!=dropped 
	    						 )
	    				{	
	    					if(validRange(top,from,to,dropped))
	    					{	if(all==null ) { return true; }
	    						all.push(getSlideMove(picked==null ? MOVE_FROM_TO : MOVE_DROPB,from,to,playerIndex(top)));
	    						some = true;
	    					}
	    				}
	    			}
	    		}
    		}
    	}
    	// diagonal cells that could move away
    	for(int direction = CELL_UP_LEFT; direction<=CELL_UP_LEFT+CELL_FULL_TURN; direction+=CELL_QUARTER_TURN)
    	{
    		SlitherCell from = dropped.exitTo(direction);
    		if(from!=null && (from==picked || from.topChip()==top))
    		{	for(int exitDirection = direction-2; exitDirection<=direction+2; exitDirection++)
    			{
    			SlitherCell exitTo = from.exitTo(exitDirection);
    			if(exitTo!=null
						&& (picked==null || from==picked)
    					&& exitTo.topChip()==null
    					&& validRange(top,from,exitTo,dropped)
    					)
    				{
    				if(all==null ) { return true; }
    				all.push(getSlideMove(picked==null ? MOVE_FROM_TO : MOVE_DROPB,from,exitTo,playerIndex(top)));
					some = true;
    				}
    			}
    		}
    	}
    	
    	return some;
    }
    /**
     * true of the range from from to to, with extra fill is valuid
     * 
     * @param top
     * @param from
     * @param to
     * @param fill
     * @return
     */
    public boolean validRange(SlitherChip top,SlitherCell from0,SlitherCell to0,SlitherCell fill0)
    {	SlitherCell from = from0==null ? fill0 : from0;
    	SlitherCell to = to0==null ? fill0 : to0;
    	int from_row = Math.max(1,Math.min(from.row-1,fill0==null ? to.row-1 : Math.min(to.row-1,fill0.row-1)));
    	int to_row = Math.min(nrows,Math.max(from.row+1,fill0==null ? to.row+1 : Math.max(to.row+1,fill0.row+1)));
    	char from_col = (char)Math.max('A',Math.min(from.col-1,fill0==null ? to.col-1 : Math.min(to.col-1,fill0.col-1)));
    	char to_col = (char)Math.min('A'+ncols-1,Math.max(from.col+1,fill0==null ? to.col+1 : Math.max(to.col+1,fill0.col+1)));
    	return validRange(from_col,from_row,to_col,to_row,top,from0,to0,fill0);
    }
    
    /** return true if all cells in the range are valid */
    public boolean validRange(char from_col,int from_row,char to_col,int to_row,
    					SlitherChip top,SlitherCell empty,SlitherCell fill,SlitherCell alsoFill)
    {
    	for(int row = from_row; row<=to_row; row++)
    	{
    		for(char col = from_col; col<=to_col; col++)
    		{	SlitherCell center = getCell(col,row);
    			if(center!=empty && (center==fill || center==alsoFill || center.topChip()==top))
    			{
    				if(!center.validDiagonalContacts(top,empty,fill,alsoFill))
    					{ return false; 
    					}
    			}
     		}
    	}
    	return true;
    }
    
    public boolean validCell(SlitherCell filled)
    {
		SlitherChip top = filled.topChip();
		return (top==null || validCell(top,filled,null,null,null));
    }
    public boolean validCell(SlitherChip top, SlitherCell filled, SlitherCell empty,SlitherCell fill,SlitherCell alsoFill)
    {	
    	return filled.validDiagonalContacts(top,empty,fill,alsoFill);
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
	    dropAt = null;
	    moveFrom = null;
	    slideTo = null;
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
        moveFrom = getCell(from_b.moveFrom);
        slideTo = getCell(from_b.slideTo);
        dropAt = getCell(from_b.dropAt);
        resetState = from_b.resetState;
        lastPicked = null;
        lastPlacedIndex = from_b.lastPlacedIndex;

        AR.copy(playerColor,from_b.playerColor);
        AR.copy(playerChip,from_b.playerChip);
 
        if(G.debug()) { sameboard(from_b); }
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
        G.Assert(sameCells(moveFrom,from_b.moveFrom),"moveFrom mismatch");
        G.Assert(sameCells(slideTo,from_b.slideTo),"slideTo mismatch");
        G.Assert(sameCells(dropAt,from_b.dropAt),"dropAt mismatch");
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
		v ^= Digest(r,moveFrom);
		v ^= Digest(r,slideTo);
		v ^= Digest(r,dropAt);
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
		case SlideAfterDrop:
		case SlideBeforeDrop:
        case Confirm:
        case Pass:
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

    private void setNextStateAfterDrop(commonMove m,replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state " + board_state);
        case Confirm:
        	setNextStateAfterDone(replay);
         	break;
        case Play:
        case PlayFix:
        case SlideFix:
        	setState(SlitherState.Confirm);
        	break;
        case SlideBeforeDrop:
        case SlideAfterDrop:
        	if(dropAt==null) 
        	{	// unusual case where we slid first when there were no drop moves.
        		// these may be some now.
        		if((m.op==MOVE_PLACE_THEN_FIX)||(m.op==MOVE_SLIDE_THEN_FIX))
        		{
        			setState(SlitherState.PlayFix);
        		}
        		else 
        			{
        			setState(hasPlacementMoves(board_state==SlitherState.SlideAfterDrop) 
        						? SlitherState.Play 
        						: SlitherState.Confirm);
        			}
        	}
        	else { setState(SlitherState.Confirm);}
        	break;
        case PlayOrSlide:
        	SlitherCell from = moveFrom;
        	if(from.onBoard)
        	{	
        		switch(m.op)
         		{
         		case MOVE_PLACE_THEN_FIX:
         		case MOVE_SLIDE_THEN_FIX:
        			setState(SlitherState.PlayFix);
        			break;
        		default:
        			setState(SlitherState.Play);
        			if(!hasPlacementMoves(false)) 
        			{ p1("illegal pass state "+moveNumber+" "+m.toString());
        		      G.Error("illegal pass");
        			  //setState(SlitherState.Pass); 
        			}
        		}
        	}
        	else
        	{	
        		setState(m.op==MOVE_PLACE_THEN_FIX ? SlitherState.SlideFix : SlitherState.SlideAfterDrop);
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
    static int seq = 0;
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
    	case Pass:
    	case SlideAfterDrop:
    	case SlideBeforeDrop:
    		if(hasPlacementMoves(true))
    		{
    		setState((occupiedCells[whoseTurn].size()==0) ? SlitherState.Play : SlitherState.PlayOrSlide);
    		}
    		else if(hasSlideMoves())
    		{
    			setState(SlitherState.SlideBeforeDrop);
    		}
    		else
    		{	if(moveNumber>80 || emptyCells.size()>2)
    				{seq++;
    				 p1("empty pass "+moveNumber+" "+seq);
    				}
    			setState(SlitherState.Pass);
    		}
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
     	if((emptyCells.size()>2) && dropAt==null)
     	{
     		p1("nothing dropped "+moveNumber+" "+emptyCells.size());
     	}
     	dropAt = null;
     	moveFrom = null;
     	slideTo = null;
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
        case MOVE_SLIDE_THEN_FIX:
        case MOVE_FROM_TO:
        	{
        	SlitherCell from = getCell(m.from_col,m.from_row);
        	SlitherCell to = getCell(m.to_col,m.to_row);
        	SetBoard(to,SetBoard(from,null));
         	if(replay.animate) {
        		animationStack.push(from);
        		animationStack.push(to);
        	}
        	moveFrom = from;
        	slideTo = to;
        	setNextStateAfterDrop(m,replay);
        	}
        	break;
        case MOVE_PLACE_THEN_FIX:
        case MOVE_DROPB:
        	{
			SlitherChip po = pickedObject;
			SlitherCell dest =  getCell(SlitherId.BoardLocation,m.to_col,m.to_row);
			
			if(isSource(dest) && (pickedObject!=null))
				{ unPickObject(); 
				}
				else 
				{
			    if(pickedObject==null) { pickObject(playerCell[m.player]); }
				m.chip = pickedObject;
	            dropObject(dest);
	            SlitherCell src = getSource();
            	moveFrom = src;
	            if(!src.onBoard) 
	            	{  dropAt = dest; 
	            	}
	            else {
	            	slideTo = dest;
	            }
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
	            setNextStateAfterDrop(m,replay);
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
            dropAt = null;
            slideTo = null;
            moveFrom = null;
            slideTo = null;
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
        case SlideAfterDrop:
        case SlideBeforeDrop:
        case SlideFix:
        	return false;
        case PlayOrSlide:
        case PlayFix:
        case Play:
        	// for pushfight, you can pick up a stone in the storage area
        	// but it's really optional
        	return(player==whoseTurn && (pickedObject==null || !getSource().onBoard));
        case Confirm:
		case ConfirmSwap:
		case Resign:
		case Pass:
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
		case SlideFix:
		case PlayFix:
		case SlideBeforeDrop:
		case Pass:
		case SlideAfterDrop:
			return(targets.get(c)!=null 
				|| (pickedObject==null ? isDest(c) : isSource(c)));
		case ConfirmSwap:
		case Gameover:
		case Resign:
			return(false);
		case Confirm:
			return(isDest(c));
        default:
        	throw G.Error("Not expecting Hit Board state " + board_state);
        case Puzzle:
        case Invalid:
            return (true);
        }
    }
    
    CellStack robotStack = new CellStack();
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Slithermovespec m)
    {	robotStack.push(dropAt);
    	robotStack.push(moveFrom);
    	robotStack.push(slideTo);
        robotState.push(board_state); //record the starting state. The most reliable
       // G.print("E "+m+" for "+whoseTurn+" "+board_state);
        Execute(m,replayMode.Replay);
        switch(m.op)
        {
        case MOVE_DROPB:
        case MOVE_PLACE_THEN_FIX:
        	robot.setFocus(dropAt);
        	break;
        default:
        	break;
        }
        acceptPlacement();
       
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(Slithermovespec m)
    {	
    	slideTo = robotStack.pop();
    	moveFrom = robotStack.pop();
    	dropAt = robotStack.pop();
    	
        //G.print("U "+m+" for "+whoseTurn);
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
        case MOVE_PLACE_THEN_FIX:
        case MOVE_DROPB:
        	SetBoard(getCell(m.to_col,m.to_row),null);
        	robot.popFocus();
        	break;
        case MOVE_FROM_TO:
        case MOVE_SLIDE_THEN_FIX:
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
 
 private boolean addValidPlayMoves(CommonMoveStack all,SlitherCell empty,SlitherCell filled,boolean include_invalid,int who)
 {
	 boolean some = false;
	 SlitherChip top = playerChip[who];
	 for(int lim = emptyCells.size()-1; lim>=0; lim--)
	 {
		 SlitherCell c = emptyCells.elementAt(lim);
		 if(c==filled 
				 ? validRange(top,empty,filled,empty)	// add an extra for the one we now know is filled
				 : validRange(top,empty,filled,c))
		 {
			if(all==null) { return true; }
			all.push(getPlayMove(MOVE_DROPB,c,who));
			some = true;
		 }
		 else if(include_invalid
				 && (empty==null && filled==null && addValidSlideRepairMoves(null,top,null,c)))
		 {
			 if(all==null) { return true; }
			 all.push(getPlayMove(MOVE_PLACE_THEN_FIX,c,who));
			 some = true;
		 }
		 else if(include_invalid)
		 {
			// p1("can never play "+c.row+c.col);
		 }
	 }
	 if(!some && emptyCells.size()>2) 
	 	{  p1("can never play "+include_invalid); 
	 	}
	 return some;
 }
 @SuppressWarnings("unused")
private boolean hasLegalMoves()
 {
	 return getListOfMoves(null,whoseTurn);
 }
 

 private boolean hasPlacementMoves(boolean includeIllegal)
 {
	 return addValidPlayMoves(null,null,null,includeIllegal,whoseTurn);
 }
 private boolean hasSlideMoves()
 {
	 return addValidSlideMoves(null,false,false,whoseTurn);
 }
 private commonMove getValidPlayMove(int op,SlitherCell c,SlitherChip top,int who)
 {
	 if(validCell(top,c,null,c,null)) { 
		 return getPlayMove(op,c,who);
	 }
	 return null;
 }
 
 private commonMove getPlayMove(int op,SlitherCell c,int who)
 {	return new Slithermovespec(op,c,who);
 }
 private boolean addValidSlideRepairMoves(CommonMoveStack all,int who)
 {
	 return addValidSlideRepairMoves(all,getPlayerChip(who),pickedObject!=null ? getSource() : null,dropAt);
 }

/**
 * 
 * @param all
 * @param includeInvalid
 * @param dropped
 * @param who
 * @return
 */
 private boolean addValidSlideMoves(CommonMoveStack all,boolean includeInvalid,boolean dropped,int who)
 {
	 boolean some = false;
	 CellStack occupied = occupiedCells[who];
	 SlitherChip top = playerChip[who];
	 if(pickedObject!=null)
	 {
		SlitherCell from = pickedSourceStack.top();
		if(from.onBoard) 
			{ some = addValidSlideMoves(MOVE_DROPB,dropped,includeInvalid ? MOVE_PLACE_THEN_FIX : 0,all,from,top,who);
			}
	 }
	 else
	 {
	 for(int lim = occupied.size()-1; lim>=0; lim--)
	 {
		 SlitherCell from = occupied.elementAt(lim);
		 if(from!=dropAt)
		 {
			some |= addValidSlideMoves(MOVE_FROM_TO,dropped,includeInvalid? MOVE_SLIDE_THEN_FIX : 0,all,from,top,who); 
			if(some && all==null) { return true; }
		 }
	 }}
	 return some;
 }
 private boolean addValidPlayRepairMoves(CommonMoveStack all,int who)
 {	boolean some = false;
	 SlitherCell from = moveFrom;
	 SlitherCell to = slideTo;
	 SlitherChip top = getPlayerChip(who);
	 if(validRange(top,from,to,from))
	 {	if(all==null) { return true; }
	 	some = true;
		all.push(getPlayMove(MOVE_DROPB,from,who));
	 }
	 for(int direction=CELL_LEFT; direction<CELL_LEFT+CELL_FULL_TURN; direction+=CELL_QUARTER_TURN)
	 {
		 SlitherCell fill = to.exitTo(direction);
		 if(fill!=null
				 && fill!=from
				 && fill.topChip()==null
				 && validRange(top,from,to,fill))
		 {	if(all==null) { return true; }
		 	some = true;
			all.push(getPlayMove(MOVE_DROPB,fill,who));
		 }
	 }
	 return some;
 }
 private boolean addValidSlideMoves(int op,boolean dropped,int invalidOp,CommonMoveStack all,SlitherCell from,SlitherChip top,int who)
 {	boolean some = false;
 	for(int direction = 0; direction<CELL_FULL_TURN; direction++)
		 {
			 SlitherCell to = from.exitTo(direction);
			 if(to!=null && to.topChip()==null)
			 {
				 if(validRange(top,from,to,null)
						 && (dropped || hasValidPlacementMoves(top,from,to)))
				 {
					 if(all==null) { return true; }
					 all.push(getSlideMove(op,from,to,who));
					 some = true;
				 }
				 else if(invalidOp>0
						 && validRange(top,from,to,from)
						 && (dropped || hasValidPlacementMoves(top,from,to))
						 )
				 {	// refill the vacated space
					 if(all==null) { return true; }
					 all.push(getSlideMove(invalidOp,from,to,who));
					 some = true;					 
				 }
				 else if(invalidOp>0)
				 {
					 for(int direction2=CELL_LEFT; direction2<CELL_LEFT+CELL_FULL_TURN; direction2+= CELL_QUARTER_TURN)
					 {
						 SlitherCell fill = to.exitTo(direction2);
						 if(fill!=null 
								 && fill.topChip()==null 
								 && validRange(top,from,to,fill)
								 && (dropped || hasValidPlacementMoves(top,from,to)))
						 {
							 if(all==null) { return true; }
							 all.push(getSlideMove(invalidOp,from,to,who));
							 some = true;					 
						 }
					 }
				 }
			 }
		 }
	 return some;
 }
 
 /**
  * return true if there are placement moves after a move from to which is not on the board yet.
  * the placement can be anywhere, not necessarily to fix a local illegal state
  * @param top
  * @param from
  * @param to
  * @return
  */
 private boolean hasValidPlacementMoves(SlitherChip top, SlitherCell from, SlitherCell to) 
 {
	 
	return addValidPlayMoves(null,from,to,false,playerIndex(top));
}
private commonMove getValidSlideMove(SlitherCell from,int firstDirection,SlitherChip top,int who)
 {	
 	for(int direction = firstDirection; direction<firstDirection+CELL_FULL_TURN; direction++)
		 {
			 SlitherCell to = from.exitTo(direction);
			 if(to!=null && to.topChip()==null)
			 {	if(validRange(top,from,to,null)
					 && hasValidPlacementMoves(top,from,to)
					 )
			 	{
				 return getSlideMove(MOVE_FROM_TO,from,to,who);
			 	}
			 }
		 }
	 return null;
 }
 
 private commonMove getSlideMove(int op,SlitherCell from,SlitherCell to,int who)
 {
	 return new Slithermovespec(op,from,to,who);
 }
 
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
	if(board_state==SlitherState.PlayOrSwap)
	{
		all.addElement(new Slithermovespec(SWAP,whoseTurn));
	}
	{
 	boolean some = getListOfMoves(all,whoseTurn);
	if(!some) 
		{ all.push(new Slithermovespec(MOVE_PASS,whoseTurn)); 
		}
 	return all;
	}
 }
 private boolean getListOfMoves(CommonMoveStack all,int who)
 {	
 	switch(board_state)
 	{
 	case Puzzle:
 	case Invalid:
 		{int op = pickedObject!=null ? MOVE_DROPB : MOVE_PICKB; 	
 		 for(SlitherCell c = allCells;
 			 	    c!=null;
 			 	    c = c.next)
 			 	{	if((pickedObject==null) != (c.topChip()==null))
 			 			{
 			 			if(all==null) { return true; }
 			 			all.addElement(new Slithermovespec(op,c.col,c.row,who));
 			 			}
 			 	}
 		return false;
 		}
 		
 	case Play:
 		return addValidPlayMoves(all,null,null,false,who);

 	case PlayOrSlide:
 		{
 		boolean some = false;
 		if(pickedObject==null || !pickedSourceStack.top().onBoard)
 			{ some = addValidPlayMoves(all,null,null,true,who);
 			  if(all==null && some) { return true; }
 			}
 		boolean some1 = some;
 		some = addValidSlideMoves(all,true,false,who);
 		if(some && !some1)
 		{
 			p1("only slide "+moveNumber);
 		}
 		if(!some) { p1("forced pass "+moveNumber); }
 		return some;
 		}
 	case SlideBeforeDrop:
 		
		// slide before a drop (no drops available)
 		addValidSlideMoves(all,true,false,who);
 		if(all!=null) { all.push(new Slithermovespec(MOVE_DONE,who)); }
 		return true;
		
	case SlideAfterDrop:
		// slide after a drop
 		addValidSlideMoves(all,false,true,who);
 		if(all!=null) { all.push(new Slithermovespec(MOVE_DONE,who)); }
 		return true;
	case PlayFix:
		return addValidPlayRepairMoves(all,who);
		
 	case SlideFix:
 		// we don't currently use this state, because all single moves are legal
 		// we don't have a mandatory slide to fix it.
 		return  addValidSlideRepairMoves(all,who);
 	case Resign:
 	case Confirm:
 	case Pass:
 		if(all!=null) { all.push(new Slithermovespec(MOVE_DONE,who)); }
 		return true;
  	case Gameover: 
  		return true;
 	default:
 			throw G.Error("Not expecting state ",board_state);
 	}

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
 		case MOVE_PLACE_THEN_FIX:
 			targets.put(getCell(m.to_col,m.to_row),m);
 			break;
 		case MOVE_SWAP:
 		case MOVE_DONE:
 			break;
 		case MOVE_SLIDE_THEN_FIX:
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
 
public commonMove getRandomMove(Random rand,CellStack focus) 
{	commonMove m = null;

	switch(board_state)
	 	{
	 	case PlayOrSwap:
	 	case Puzzle:
	 	case Resign:
	 	case Confirm:
	 	case Pass:
	 	case Invalid:
	 		break;
	 	case Play:
	 		m = randomPlayMove(rand,99999,focus);
	 		break;
	 	case PlayOrSlide:
	 		{
	 		if(rand.nextInt(100)<50) 
	 		{
	 			m = randomSlideMove(rand,9999,focus);
	 		}
	 		if(m==null)
	 		{	m = randomPlayMove(rand,9999,focus);
	 		}
	 		}
	 		break;
			
		case SlideBeforeDrop:
			
			if(rand.nextInt(100)>robot.slitherPercent)
			{
				m = new Slithermovespec(MOVE_DONE,whoseTurn);
			}
			else 
			{ m = randomSlideMove(rand,(int)(occupiedCells[whoseTurn].size()*0.25),focus);
			}	
	 		break;
		case SlideAfterDrop:
	 	case SlideFix:
	 	case PlayFix:	// these are extremely restricted states
	 		break;
	 	case Gameover: break;
	 	default:
	 			G.Error("Not expecting state ",board_state);
	 	}
	 	return(m);
	 }
private commonMove randomSlideMove(Random rand, int tries,CellStack focus)
{
	CellStack occupied = occupiedCells[whoseTurn];
	SlitherChip chip = playerChip[whoseTurn];
	int max = Math.min(tries,occupied.size());
	while(max>0 && tries>0)
	{
		int idx = rand.nextInt(max);
		SlitherCell c = occupied.elementAt(idx);
		tries--;
		if(focus==null || robot.filter(focus,c))
		{
		commonMove m = getValidSlideMove(c,rand.nextInt(CELL_FULL_TURN),chip,whoseTurn);
		if(m!=null) { return m; }
		max--;
		occupied.setElementAt(occupied.elementAt(max),idx);
		occupied.setElementAt(c,max);
		}
	}
	return (focus!=null) ? randomSlideMove(rand,tries,null) : null;
}
private commonMove randomPlayMove(Random rand, int tries,CellStack focus) 
{
	int max = Math.min(tries,emptyCells.size());
	SlitherChip chip = playerChip[whoseTurn];
	while(max>0 && tries>0)
	{
		int idx = rand.nextInt(max);
		SlitherCell c = emptyCells.elementAt(idx);
		tries--;
		if(focus==null || robot.filter(focus,c))
		{
		commonMove m = getValidPlayMove(MOVE_DROPB,c,chip,whoseTurn);
		if(m!=null) { return m; }
		max--;
		emptyCells.setElementAt(emptyCells.elementAt(max),idx);
		emptyCells.setElementAt(c,max);
		}
	}
	return (focus!=null) ? randomPlayMove(rand,tries,null) : null;
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
