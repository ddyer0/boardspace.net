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
package epaminondas;


import static epaminondas.EpaminondasMovespec.*;

import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;

/**
 * EpaminondasBoard knows all about the game of Epaminondas, which is played
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

@SuppressWarnings("unused")
class EpaminondasBoard 
	extends rectBoard<EpaminondasCell>	// for a square grid board, this could be rectBoard or squareBoard 
	implements BoardProtocol,EpaminondasConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
	static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	EpaminondasVariation variation = EpaminondasVariation.epaminondas;
	private EpaminondasState board_state = EpaminondasState.Puzzle;	
	private EpaminondasState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	private IStack robotStack = new IStack();
	public int placementIndex = -1;
	public EpaminondasState getState() { return(board_state); }
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(EpaminondasState st) 
	{ 	unresign = (st==EpaminondasState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

    private EpaminondasId playerColor[]={EpaminondasId.White,EpaminondasId.Black};    
    private EpaminondasChip playerChip[]={EpaminondasChip.White,EpaminondasChip.Black};
    private EpaminondasCell playerCell[]=new EpaminondasCell[2];
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public EpaminondasChip getPlayerChip(int p) { return(playerChip[p]); }
	public EpaminondasId getPlayerColor(int p) { return(playerColor[p]); }
	public EpaminondasCell getPlayerCell(int p) { return(playerCell[p]); }
	public int getChipCount(int pl) { return occupiedCells[pl].size(); }
	public EpaminondasCell getPlayerCell(EpaminondasChip ch) { return getPlayerCell(getPlayerIndex(ch)); }
	public EpaminondasChip getCurrentPlayerChip() { return(playerChip[whoseTurn]); }
	public int getPlayerIndex(EpaminondasChip ch) { return ch==playerChip[0] ? 0 : 1; }
	public int playerHomeRow(int pl) { return pl==0 ? 1 : variation.nrows; }
	public EpaminondasPlay robot = null;
	
	 public boolean p1(String msg)
		{
			if(G.p1(msg) && robot!=null)
			{	String dir = "g:/share/projects/boardspace-html/htdocs/epaminondas/epaminondasgames/robot/";
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
	public void SetDrawState() { setState(EpaminondasState.Draw); }	CellStack animationStack = new CellStack();

    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public EpaminondasChip pickedObject = null;
    public EpaminondasChip lastPicked = null;
    private EpaminondasCell blackChipPool = null;	// dummy source for the chip pools
    private EpaminondasCell whiteChipPool = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private CellStack captureDestStack = new CellStack();
    
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
 	
    CellStack occupiedCells[] = { new CellStack(),new CellStack()};
    private EpaminondasState resetState = EpaminondasState.Puzzle; 
    public DrawableImage<?> lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public EpaminondasCell newcell(char c,int r)
	{	return(new EpaminondasCell(EpaminondasId.BoardLocation,c,r));
	}
	
	// constructor 
    public EpaminondasBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = GRIDSTYLE;
        setColorMap(map, players);
        
		Random r = new Random(734687);
		// do this once at construction
	    blackChipPool = new EpaminondasCell(r,EpaminondasId.Black);
	    blackChipPool.addChip(EpaminondasChip.Black);
	    whiteChipPool = new EpaminondasCell(r,EpaminondasId.White);
	    whiteChipPool.addChip(EpaminondasChip.White);

        doInit(init,key,players,rev); // do the initialization 
        autoReverseY();		// reverse_y based on the color map
    }
    
    public String gameType() { return(G.concat(gametype," ",players_in_game," ",randomKey," ",revision)); }
    

    public void doInit(String gtype,long key)
    {
    	Tokenizer tok = new Tokenizer(gtype);
    	String typ = tok.nextToken();
    	int np = tok.hasMoreTokens() ? tok.intToken() : players_in_game;
    	long ran = tok.hasMoreTokens() ? tok.longToken()  : key;
    	int rev = tok.hasMoreTokens() ? tok.intToken()  : revision;
    	doInit(typ,ran,np,rev);
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int players,int rev)
    {	randomKey = key;
    	adjustRevision(rev);
    	players_in_game = players;
    	win = new boolean[players];
 		setState(EpaminondasState.Puzzle);
		variation = EpaminondasVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		gametype = gtype;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case epaminondas_8:
		case epaminondas:
			// using reInitBoard avoids thrashing the creation of cells 
			// when reviewing games.
			reInitBoard(variation.ncols,variation.nrows);
			// or initBoard(variation.firstInCol,variation.ZinCol,null);
			// Random r = new Random(734687);	// this random is used to assign hash values to cells, common to all games of this type.
			// allCells.setDigestChain(r);		// set the randomv for all cells on the board
		}

 		
	    playerCell[FIRST_PLAYER_INDEX] = whiteChipPool; 
	    playerCell[SECOND_PLAYER_INDEX] = blackChipPool; 
	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    droppedDestStack.clear();
	    captureDestStack.clear();
	    pickedSourceStack.clear();
	    stateStack.clear();
	    
	    pickedObject = null;
	    resetState = null;
	    lastDroppedObject = null;
	    int map[]=getColorMap();
		playerColor[map[0]]=EpaminondasId.White;
		playerColor[map[1]]=EpaminondasId.Black;
		playerChip[map[0]]=EpaminondasChip.White;
		playerChip[map[1]]=EpaminondasChip.Black;
	    // set the initial contents of the board to all empty cells

		reInit(occupiedCells);
		for(EpaminondasCell c = allCells; c!=null; c=c.next) { c.reInit(); }
		for(int i=0;i<ncols;i++)
		{	// done this somewhat complicated way to make sure homeRow and the colors are consistent.
			EpaminondasChip chip = 1==playerHomeRow(getPlayerIndex(EpaminondasChip.White))
				? EpaminondasChip.White
				: EpaminondasChip.Black;
			EpaminondasChip other = chip==EpaminondasChip.White 
									? EpaminondasChip.Black 
									: EpaminondasChip.White;
			for(int j=1;j<=2;j++)
			{	
				SetBoard(getCell((char)('A'+i),j),chip);
				SetBoard(getCell((char)('A'+i),nrows-j+1),other);
			}
		}
	    
        animationStack.clear();
        moveNumber = 1;
        placementIndex = 1;

        // note that firstPlayer is NOT initialized here
    }

    /** create a copy of this board */
    public EpaminondasBoard cloneBoard() 
	{ EpaminondasBoard dup = new EpaminondasBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((EpaminondasBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(EpaminondasBoard from_b)
    {
        super.copyFrom(from_b);
        robotState.copyFrom(from_b.robotState);
        getCell(occupiedCells,from_b.occupiedCells);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(captureDestStack,from_b.captureDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        copyFrom(whiteChipPool,from_b.whiteChipPool);		// this will have the side effect of copying the location
        copyFrom(blackChipPool,from_b.blackChipPool);		// from display copy boards to the main board
        getCell(playerCell,from_b.playerCell);
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        lastPicked = null;

        AR.copy(playerColor,from_b.playerColor);
        AR.copy(playerChip,from_b.playerChip);
 
        if(G.debug()) { sameboard(from_b); }
    }

    

    public void sameboard(BoardProtocol f) { sameboard((EpaminondasBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(EpaminondasBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor mismatch");
        G.Assert(AR.sameArrayContents(playerChip,from_b.playerChip),"playerChip mismatch");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedsourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(sameCells(captureDestStack,from_b.captureDestStack),"caprureDestStack mismatch");
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
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,captureDestStack);
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
        case Confirm:
        case Draw:
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
    {
    	return win[pl];
    }
    public int opponentCount(int player)
    {
    	int row = playerHomeRow(nextPlayer[player]);
    	int count = 0;
    	EpaminondasChip myChip = getPlayerChip(player);
    	EpaminondasCell c= getCell('A',row);
    	while(c!=null) 
    		{ if(c.topChip()==myChip) { count++; }
    		  c = c.exitTo(CELL_RIGHT);
    		}
    	return count;
    }
    
    // some simple stats collected by the move generator. 
    // these are effectively "lagging" stats because they are
    // collected on the current list of moves, not on the 
    // result of the move. 
    private int bothMoveStats[][]= {{0,0,0,0},{0,0,0,0}};
    private static int Single=0;
    private static int Phalanx=1;
    private static int PhalanxLength=2;
    private static int CaptureLength=3;
    private int moveStats[] = bothMoveStats[0];
    //
    // 10/2024 ddyer
    // I've done a bunch of ad-hoc tweaking of these parameters, and
    // conclude that other than woodMultiplier, none of them make a
    // definitive difference.  New ideas required.
    //
    public double dumbEval(int pl,boolean useStats)
    {
    	double rowMultiplier = 10;
    	double columnMultiplier = -0.2;
    	double woodMultiplier = 10;
    	double winMultiplier = 100;
    	int oc = opponentCount(pl);
    	double val = oc*winMultiplier;
    	int homeRow = playerHomeRow(pl);   
    	CellStack occupied = occupiedCells[pl];
    	int size = occupied.size();
    	int mid = 'A'+nrows/2;
    	val += woodMultiplier * size;
    	for(int lim=size-1; lim>=0; lim--)
    	{
    		EpaminondasCell c = occupied.elementAt(lim);
    		int col = Math.abs(mid-c.col);
    		int row = Math.abs(c.row-homeRow);
    		val += rowMultiplier*row;
    		val += columnMultiplier*col;
    	}
    	if(useStats)
    	{	int stats[] = bothMoveStats[pl];
    		double singleMultiplier = -0.1;
    		double phalanxMultiplier = 0.04;
    		double phalanxLengthMultiplier = 0.01;
    		double captureLengthMultiplier = 1.1;
    		
    		val += stats[Single]*singleMultiplier;
    		val += stats[Phalanx]*phalanxMultiplier;
    		val += stats[PhalanxLength]*phalanxLengthMultiplier;
    		val += stats[CaptureLength]*captureLengthMultiplier;
    	}
    	return val;
    }
    public double smartEval(int pl,boolean useStats)
    {
    	double rowMultiplier = 10;
    	double columnMultiplier = -0.2;
    	double woodMultiplier = 1;
    	double winMultiplier = 100;
    	int oc = opponentCount(pl);
    	double val = oc*winMultiplier;
    	int homeRow = playerHomeRow(pl);   
    	CellStack occupied = occupiedCells[pl];
    	int size = occupied.size();
    	int mid = 'A'+nrows/2;
    	val += woodMultiplier * size;
    	for(int lim=size-1; lim>=0; lim--)
    	{
    		EpaminondasCell c = occupied.elementAt(lim);
    		int col = Math.abs(mid-c.col);
    		int row = Math.abs(c.row-homeRow);
    		val += rowMultiplier*row;
    		val += columnMultiplier*col;
    	}
    	if(useStats)
    	{	int stats[] = bothMoveStats[pl];
    		double singleMultiplier = -0.1;
    		double phalanxMultiplier = 0.04;
    		double phalanxLengthMultiplier = 0.01;
    		double captureLengthMultiplier = 1.1;
    		
    		val += stats[Single]*singleMultiplier;
    		val += stats[Phalanx]*phalanxMultiplier;
    		val += stats[PhalanxLength]*phalanxLengthMultiplier;
    		val += stats[CaptureLength]*captureLengthMultiplier;
    	}
    	return val;
    }
    

    //
    public double bestEval(int pl,boolean useStats)
    {
    	double rowMultiplier = 1;
    	double columnMultiplier = 0.1;
    	double woodMultiplier = 40;
    	double winMultiplier = 100;
    	int oc = opponentCount(pl);
    	double val = oc*winMultiplier;
    	int homeRow = playerHomeRow(pl);   
    	CellStack occupied = occupiedCells[pl];
    	int size = occupied.size();
    	int mid = 'A'+nrows/2;
    	val += woodMultiplier * size;
    	for(int lim=size-1; lim>=0; lim--)
    	{
    		EpaminondasCell c = occupied.elementAt(lim);
    		int col = Math.abs(mid-c.col);
    		int row = Math.abs(c.row-homeRow);
    		val += rowMultiplier*row;
    		val += columnMultiplier*col;
    	}
    	if(false)
    	{	int stats[] = bothMoveStats[pl];
    		double singleMultiplier = -0.1;
    		double phalanxMultiplier = 0.04;
    		double phalanxLengthMultiplier = -0.2;
    		double captureLengthMultiplier = 1.1;
    		
    		val += stats[Single]*singleMultiplier;
    		val += stats[Phalanx]*phalanxMultiplier;
    		val += stats[PhalanxLength]*phalanxLengthMultiplier;
    		val += stats[CaptureLength]*captureLengthMultiplier;
    	}
    	return val;
    } 

    // set the contents of a cell, and maintain the books
    private int lastPickedIndex = -1;
    private int lastDroppedIndex = -1;
    public EpaminondasChip SetBoard(EpaminondasCell c,EpaminondasChip ch)
    {	EpaminondasChip old = c.topChip();
    	if(old!=ch)
    	{
       	if(old==null) { c.addChip(ch); occupiedCells[getPlayerIndex(ch)].push(c); }
       	else { c.removeTop(); occupiedCells[getPlayerIndex(old)].remove(c); }
    	}
    	return(old);
    }
    //
    // accept the current placements as permanent
    //
    public void acceptPlacement()
    {	captureDestStack.clear();
        droppedDestStack.clear();
        pickedSourceStack.clear();
        stateStack.clear();
        pickedObject = null;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private EpaminondasCell unDropObject()
    {	
    	EpaminondasCell rv = droppedDestStack.pop();
    	setState(stateStack.pop());
    	rv.lastDropped = lastDroppedIndex;
    	placementIndex--;
    	pickedObject = SetBoard(rv,null); 	// SetBoard does ancillary bookkeeping
    	EpaminondasChip other = pickedObject==EpaminondasChip.White ? EpaminondasChip.Black : EpaminondasChip.White;
       	while(captureDestStack.size()>0)
       	{
       		EpaminondasCell c = captureDestStack.pop();
       		SetBoard(c,other);
       	}
       	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	EpaminondasCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	SetBoard(rv,pickedObject);
    	rv.lastPicked = lastPickedIndex;
    	pickedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(EpaminondasCell c)
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
        	lastDroppedIndex = c.lastDropped;
        	c.lastDropped = placementIndex;
        	placementIndex++;
            pickedObject = null;
            break;
        }
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(EpaminondasCell c)
    {	return(droppedDestStack.top()==c);
    }
    public EpaminondasCell getDest()
    {	return(droppedDestStack.top());
    }
 
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { EpaminondasChip ch = pickedObject;
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
    private EpaminondasCell getCell(EpaminondasId source, char col, int row)
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
    public EpaminondasCell getCell(EpaminondasCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(EpaminondasCell c)
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
         	lastPickedIndex = c.lastPicked;
         	c.lastPicked = placementIndex;
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
    public boolean isSource(EpaminondasCell c)
    {	return(c==pickedSourceStack.top());
    }
    public EpaminondasCell getSource()
    {	return(pickedSourceStack.top());
    }
 
    private boolean isPositionSymmetric()
    {	CellStack my = occupiedCells[0];
    	CellStack his = occupiedCells[1];
    	int sz = occupiedCells[0].size();
    
    	if(sz == his.size())
    	{	EpaminondasChip ot = getPlayerChip(1);
    		for(int i=0;i<sz;i++)
    		{
    			EpaminondasCell c = my.elementAt(i);
    			EpaminondasCell d = getCell((char)('A'+ncols-(c.col-'A'+1)),nrows-c.row+1);
    			EpaminondasChip ch = d.topChip();
    			if(ch!=ot) { return false; }
    		}
    		return true;
    	}
    	return false;
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
        case Check:
        	if(isPositionSymmetric()) 
        		{ 
        			if(robot!=null) { setState(EpaminondasState.Gameover); win[nextPlayer[whoseTurn]]=true; }
        			setState(EpaminondasState.Illegal); 
        			break;
        		}
			//$FALL-THROUGH$
		case Play:
			setState(EpaminondasState.Confirm);
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
    	case Draw:
    		setState(EpaminondasState.Gameover);
    		break;
    	case Gameover:
    		break;
    	case Confirm:
    	case Puzzle:
    	case Play:
    		if(occupiedCells[whoseTurn].size()==0)
    		{	
    			setState(EpaminondasState.Gameover);
    			win[nextPlayer[whoseTurn]]=true;
       			//p1("allcaptured "+moveNumber);
       		}
    		else
    		{
    		int myopp = opponentCount(whoseTurn);
    		int hisopp = opponentCount(nextPlayer[whoseTurn]);
    		if(myopp != hisopp)
    		{
    			if(resetState==EpaminondasState.Check) 
    				{ win[whoseTurn]=true; 
    				  setState(EpaminondasState.Gameover); 
    				//p1("gameover "+moveNumber);
    				}
    			if (myopp>hisopp)
    			{	// moving out of a position of equality
    				setState(EpaminondasState.Gameover); 
    				win[whoseTurn]=true; 
    			}
    			else { setState(EpaminondasState.Check); }
    		}
    		else
    		{
    		setState(EpaminondasState.Play);  	
    		}}
    		break;
    	}
       	resetState = board_state;
    }
    private void doDone(replayMode replay)
    {
        acceptPlacement();
        placementIndex++;
        if (board_state==EpaminondasState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(EpaminondasState.Gameover);
        }
        else
        {	
        	setNextPlayer(replay);
        	setNextStateAfterDone(replay);
        }
    }
void doSwap(replayMode replay)
{	EpaminondasId c = playerColor[0];
	EpaminondasChip ch = playerChip[0];
	playerColor[0]=playerColor[1];
	playerChip[0]=playerChip[1];
	playerColor[1]=c;
	playerChip[1]=ch;
	EpaminondasCell cc = playerCell[0];
	playerCell[0]=playerCell[1];
	playerCell[1]=cc;
	switch(board_state)
	{	
	default: 
		throw G.Error("Not expecting swap state "+board_state);
	case Play:
		// some damaged game records have double swap
		if(replay==replayMode.Live) { G.Error("Not expecting swap state "+board_state); }
		//$FALL-THROUGH$

	case Gameover:
	case Puzzle: break;
	}
	}
	public void doCaptures(EpaminondasCell c,int direction,replayMode replay)
	{
		EpaminondasCell from = c;
		EpaminondasChip target = c.topChip();
		while(from!=null && from.topChip()==target)
		{
			captureDestStack.push(from);
			EpaminondasChip top = SetBoard(from,null);
			if(replay.animate)
			{
				animationStack.push(from);
				animationStack.push(getPlayerCell(top));
			}
			from = from.exitTo(direction);
		}
	}
	private void slide(EpaminondasCell src,EpaminondasChip po,EpaminondasCell dest,int direction,replayMode replay)
	{	if(po==null) { po = SetBoard(src,null); }
		
        EpaminondasCell animSrc = src;
        int numberMoving = nChipsInDirection(src,po,direction)-1;
        int backward = direction+CELL_HALF_TURN;
        while((dest=dest.exitTo(backward))!=src)
        {	SetBoard(dest,numberMoving>0 ? po : null); 
        	numberMoving--;
        	if(replay.animate)
        	{
        		animationStack.push(animSrc);
        		animationStack.push(dest);
        		animSrc=animSrc.exitTo(direction);
        	}
        }
	}
	
    public boolean Execute(commonMove mm,replayMode replay)
    {	EpaminondasMovespec m = (EpaminondasMovespec)mm;
        if(replay.animate) { animationStack.clear(); }

        //G.print("E "+m+" for "+whoseTurn+" "+state);
        switch (m.op)
        {
        case MOVE_DONE:
         	doDone(replay);
            break;

        case MOVE_FROM_TO:
        case MOVE_CAPTURE:
        	G.Assert(pickedObject==null,"should be nothing moving");
        	pickObject(getCell(m.from_col,m.from_row));
			//$FALL-THROUGH$
		case MOVE_DROPB:
        	{
			EpaminondasChip po = pickedObject;
			EpaminondasCell src = getSource();
			EpaminondasCell dest =  getCell(EpaminondasId.BoardLocation,m.to_col,m.to_row);
			
			if(isSource(dest)) 
				{ unPickObject(); 
				}
				else 
				{
				m.chip = po;
				int direction = findDirection(src,dest);
				boolean capture = dest.topChip()!=null;
		        if(capture) 
		        {	// a capture, remove all stones in the direction
		        	doCaptures(dest,direction,replay);
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
	            dropObject(dest);
		        if(board_state!=EpaminondasState.Puzzle)
		        {
		        slide(src,po,dest,direction,replay);
		        }	        
	
	            setNextStateAfterDrop(replay);
				}
        	}
             break;

        case MOVE_PICK:
 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
 			EpaminondasCell src = getCell(m.source,m.from_col,m.from_row);
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
        		setState( EpaminondasState.Play);
        		break;
        	default: ;
        	}}}
            break;

        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
            EpaminondasCell dest = getCell(m.source,m.to_col,m.to_row);
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
            setState(EpaminondasState.Puzzle);	// standardize the current state
            setNextStateAfterDone(replay); 

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?EpaminondasState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(EpaminondasState.Puzzle);
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(EpaminondasState.Gameover);
    	   break;

        default:
        	cantExecute(m);
        }
        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }

 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(EpaminondasMovespec m)
    {
        robotState.push(board_state); //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        //G.print("R "+m);
        Execute(m,replayMode.Replay);
        if(m.op==MOVE_CAPTURE)
        {
        	for(int lim=captureDestStack.size()-1; lim>=0; lim--)
    		{
    		EpaminondasCell c = captureDestStack.elementAt(lim);
    		int col = c.col<<8;
    		robotStack.push(col+c.row);
    		}
        	robotStack.push(captureDestStack.size());
        }
        if(board_state==EpaminondasState.Confirm)
        {	
        	doDone(replayMode.Replay);
        }
       
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(EpaminondasMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	EpaminondasState state = robotState.pop();
        setState(state);
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
        switch (m.op)
        {
        default:
   	    	throw G.Error("Can't un execute " + m);
        case MOVE_DONE:
        case MOVE_RESIGN:
            break;
        case MOVE_CAPTURE:
        case MOVE_FROM_TO:
        	{
        	EpaminondasCell from = getCell(m.from_col,m.from_row);
        	EpaminondasCell to = getCell(m.to_col,m.to_row);
        	int direction = findDirection(to,from);
        	if(to.exitTo(direction)==from)
        	{	// simple move
        		SetBoard(from,SetBoard(to,null));
        	}
        	else
        	{ 
        		slide(to,null,from,direction,replayMode.Replay);
        		SetBoard(from,getPlayerChip(whoseTurn));
        	}
        	if(m.op==MOVE_CAPTURE)
        	{
            	int n = robotStack.pop();
            	while(n-->0)
            	{
            		int spec = robotStack.pop();
            		EpaminondasChip ch = getPlayerChip(nextPlayer[whoseTurn]);
            		int col = (spec>>8);
            		int row = spec&0xff;
            		EpaminondasCell c = getCell((char)col,row);
            	
            		SetBoard(c,ch);
            	}
        	}}
        }
 
 }
 private int nChipsInDirection(EpaminondasCell c,EpaminondasChip top,int direction)
 {
	 int n=1;
	 EpaminondasCell from = c;
	 while( ((from=from.exitTo(direction))!=null)
			 && (from.topChip()==top))
	 {
		 n++;
	 }
	 return n;
 }
 //
 // true if there are fewer than N chips the same color in direction starting at "from"
 //
 private boolean fewerChipsInDirection(int n,EpaminondasCell from,int direction)
 {
	 int count = 1;
	 EpaminondasCell d = from;
	 EpaminondasChip ch = from.topChip();
	 while( ((d=d.exitTo(direction))!=null)
			 && (d.topChip()==ch))
	 {	count++;
	 	if(count>=n)
	 		{ return false; }
	 	
	 }
	 return true;
 }
 //
 // return true if we should quit gathering more moves (for robot random move generation)
 //
 private int moveGenerationLimit = 999999;
 private Random moveGenerationRandomizer = null;
 private boolean addMove(CommonMoveStack all,int op,EpaminondasCell c,EpaminondasCell d,int who)
 {		boolean ok = robot==null || board_state!=EpaminondasState.Check;
 		if(!ok)
	 	{	// if in check, the robot only considers moves likely to resolve it
	 		ok = ((op==MOVE_CAPTURE)
	 				&& (d.row == playerHomeRow(who)))
	 			||
	 			((op==MOVE_FROM_TO)
	 				&& (d.row==playerHomeRow(nextPlayer[who]))
	 				&& (c.row!=d.row));
	 	}
	 	if(ok)
	 	{
	 	all.push(new EpaminondasMovespec(op,c,d,who)); 
	 	if(all.size()>moveGenerationLimit) { return true; }
	 	}
	 	return false;
 }
 private boolean addPhalanxMoves(CommonMoveStack all,EpaminondasCell c,int direction,int who)
 {
	 int n = 1;
	 EpaminondasChip myChip = playerChip[who];
	 EpaminondasCell d = c;
	 EpaminondasChip ch = myChip;
	 while( ((d=d.exitTo(direction)) != null)
			 && ((ch=d.topChip())==myChip))
	 {
		 n++;
	 }
	 int nindirection = 0;
	 int phalanxLength = n;
	 moveStats[PhalanxLength] += n;
	 moveStats[Phalanx]++;
	 while(d!=null && n>0)
	 {	ch = d.topChip();
		if(ch==null) 
			{ // simple move, no captures
				
				if(addMove(all,MOVE_FROM_TO,c,d,who)) { return true; }
			}
		else if(ch==myChip) { n=0; }
		else if( (nindirection = nChipsInDirection(d,ch,direction))<phalanxLength)
		{
			// potential capture, but we need to count the opposing pieces
			moveStats[CaptureLength] += nindirection;
			if(addMove(all,MOVE_CAPTURE,c,d,who)) { return true; }
			n=0;
		}
		else { n=0; }	// stop looking anyway
	 	d=d.exitTo(direction);
	 	n--;
	 }
	 return false;
 }
 
 private void addMoves(CommonMoveStack all,int who)
 {	CellStack cells = occupiedCells[who];
 	EpaminondasChip myChip = playerChip[who];
 	int size = cells.size();
 	int offset = moveGenerationRandomizer==null ? 0 : moveGenerationRandomizer.fastUpto(size);
 	for(int lim=cells.size()-1; lim>=0; lim--)
 	{
 		EpaminondasCell c = cells.elementAt((lim+offset)%size);
 		G.Assert(c.topChip()==myChip,"#1 should be mine",c);
		if(addMoves(all,c,myChip,who)) { return; }
 	}
 }
 
 // add moves from a particular cell, myChip is the contents of that cell
 // which may or may not actually be there, so don't look.
 private boolean addMoves(CommonMoveStack all,EpaminondasCell c,EpaminondasChip myChip,int who)
 {	int n = c.geometry.n;
 	int offset = moveGenerationRandomizer==null ? 0 : moveGenerationRandomizer.fastUpto(n);
 	int limit = n+offset;
	for(int direction = offset;direction<limit; direction++)
	{	EpaminondasCell d = c.exitTo(direction);
		if(d!=null)
		{	
			EpaminondasChip ch = d.topChip();
			if(ch==null) 
				{ 
				if(addMove(all,MOVE_FROM_TO,c,d,who)) { return true; } 
				}
			else if(ch==myChip) { if(addPhalanxMoves(all,c,direction,who)) { return true; }; }
		}
	}
	return false;
 }
 
 
 CommonMoveStack  GetListOfMoves(Random rand, int limit)
 {	CommonMoveStack all = new CommonMoveStack();
 	moveGenerationLimit = limit;
 	moveGenerationRandomizer = rand;
 	moveStats  = bothMoveStats[whoseTurn];
 	AR.setValue(moveStats,0);
 	switch(board_state)
 	{
 	case Puzzle:
 		{
 			if(pickedObject==null)
 			{	all.push(new EpaminondasMovespec(MOVE_PICK,playerCell[0],whoseTurn));
 				all.push(new EpaminondasMovespec(MOVE_PICK,playerCell[1],whoseTurn));
 				for(EpaminondasCell c = allCells;
 	 			 	    c!=null;
 	 			 	    c = c.next)
 				{
 					if(c.topChip()!=null) { all.push(new EpaminondasMovespec(MOVE_PICKB,c,whoseTurn)); }
 				}
 			}
 			else
 			{	all.push(new EpaminondasMovespec(MOVE_DROP,getPlayerCell(pickedObject),whoseTurn));
 				for(EpaminondasCell c = allCells;
 	 			 	    c!=null;
 	 			 	    c = c.next)
 				{
 					if(c.topChip()==null) { all.push(new EpaminondasMovespec(MOVE_DROPB,c,whoseTurn)); }
 				}
 			}
 		}
 		break;
 	case Check:
 		all.push(new EpaminondasMovespec(MOVE_RESIGN,whoseTurn));
		//$FALL-THROUGH$
	case Play:
 		if(pickedObject!=null)
 			{ 
 			EpaminondasCell src = getSource();
 			if(src.onBoard) { all.push(new EpaminondasMovespec(MOVE_DROPB,src,whoseTurn)); }
 			else { all.push(new EpaminondasMovespec(MOVE_DROP,src,whoseTurn)); }
 			addMoves(all,getSource(),pickedObject,whoseTurn); 
 			}
 		else { addMoves(all,whoseTurn); }
 		break;
 	case Confirm:
 	case Resign:
 	case Draw:
 	case Gameover:
 		all.push(new EpaminondasMovespec(MOVE_DONE,whoseTurn));
  		break;
 	case Illegal: 
 		break;
 	default:
 			G.Error("Not expecting state ",board_state);
 	}
 	return(all);
 }
 
 public void initRobotValues(EpaminondasPlay m)
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
 public Hashtable<EpaminondasCell, EpaminondasMovespec> getTargets() 
 {
 	Hashtable<EpaminondasCell,EpaminondasMovespec> targets = new Hashtable<EpaminondasCell,EpaminondasMovespec>();
 	CommonMoveStack all = GetListOfMoves(null,99999);
 	EpaminondasCell dest = getDest();
 	if(dest!=null) { all.push(new EpaminondasMovespec(MOVE_PICKB,dest,whoseTurn)); }
 	for(int lim=all.size()-1; lim>=0; lim--)
 	{	EpaminondasMovespec m = (EpaminondasMovespec)all.elementAt(lim);
 		switch(m.op)
 		{
 		case MOVE_FROM_TO:
 		case MOVE_CAPTURE:
 			if(pickedObject!=null)
 			{	m.op = MOVE_DROPB;
 				targets.put(getCell(m.to_col,m.to_row),m);
 			}
 			else
 			{	m.op = MOVE_PICKB;
 				targets.put(getCell(m.from_col,m.from_row),m);
 			}
 			break;
 			
 		case MOVE_PICKB:
 		case MOVE_PICK:
 			targets.put(getCell(m.source,m.from_col,m.from_row),m);
 			break;
 		case MOVE_DROPB:
 		case MOVE_DROP:
 			targets.put(getCell(m.source,m.to_col,m.to_row),m);
 			break;
 		case MOVE_RESIGN:
 		case MOVE_DONE:
 			break;

 		default: G.Error("Not expecting "+m);
 		
 		}
 	}
 	
 	return(targets);
 }
 

}
