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
package ponte;

import online.game.*;
import java.util.*;

import lib.AR;
import lib.G;
import lib.OStack;
import lib.Random;
import static ponte.PonteMovespec.*;

class BlobStack extends OStack<PonteBlob>
{
	public PonteBlob[] newComponentArray(int n) { return(new PonteBlob[n]); }
}

/**
 * PonteBoard knows all about the game of Ponte Del Diavolo, which is played
 * on a 10x10 board.
 * 
 * 
 * 
 * @author ddyer
 *
 */
class PonteBlob extends OStack<PonteCell>
{	PonteBlob(PonteChip ch) { super(); color = ch; }
	public PonteCell[] newComponentArray(int n) { return(new PonteCell[n]); }
	int sweepCounter = 0;
	int networkNumber = 0;
	PonteChip color;
	int countBridges()
	{	int n=0;
		for(int lim=size()-1; lim>=0; lim--)
		{
			PonteCell c = elementAt(lim);
			if(c.topChip().bridge!=null) { n++; }
		}
		return(n);
	}
}	

public class PonteBoard extends rectBoard<PonteCell> implements BoardProtocol,PonteConstants
{	
    public int boardColumns = DEFAULT_COLUMNS;	// size of the board
    public int boardRows = DEFAULT_ROWS;
    public void SetDrawState() { setState(PonteState.Draw); }
    public PonteCell rack[] = new PonteCell[PonteChip.N_STANDARD_PIECES];

    public int score[] = {0,0};
    public int playerRack[] = {0,1};
    private boolean swapped = false;
    private boolean robotGame = false;
    int robotDepth = 0;
    void setRobotGame() { robotGame = true; }
    public PonteCell getPlayerChips(int forp)
    {
    	return((forp<=1) ? rack[playerRack[forp]] : rack[forp]);
    }
    private PonteChip chips[]= {PonteChip.white,PonteChip.red};
    public PonteChip getPlayerChip(int forplayer)
    {
    	return(chips[playerRack[forplayer]]);
    }
    public CellStack animationStack = new CellStack();
    private CommonMoveStack alternateBridgeMoves = null;
    public PonteCell prev_tile_1=null;
    public PonteCell prev_tile_2=null;
    
 
    public BlobStack blobs = new BlobStack();
    boolean blobsValid = false;
    private int sweepCounter = 0;
    private void doSwap()
    {	int pl = playerRack[1];
    	playerRack[1]=playerRack[0];
    	playerRack[0]=pl;
    	swapped = !swapped;
    }
    
    //
    // private variables
    //
    private PonteState board_state = PonteState.Puzzle;	// the current board state
     
    private PonteState unresign = null;					// remembers the previous state when "resign"
    private boolean lastMove = false;
    private boolean lastMoveAtStart = false;
    public PonteState getState() { return(board_state); } 
	public void setState(PonteState st) 
	{ 	unresign = (st==PonteState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
    private void addChip(PonteCell c,PonteChip targetChip)
    {	if(c.topChip()==null)
    	{
    	occupiedCells.push(c);
		emptyCells.remove(c,false);
    	}
		c.addChip(targetChip);
    }
    private PonteChip removeChip(PonteCell c)
    {  	PonteChip po = c.removeTop();
    	if(c.topChip()==null)
    	{
		occupiedCells.remove(c,false);
		emptyCells.push(c);
    	}
    	return(po);
    }    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public PonteChip pickedObject = null;
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    private CellStack emptyCells = new CellStack();
    private CellStack occupiedCells = new CellStack();
    private CommonMoveStack cachedTileMoves[] = null;
    private int cachedBridgeMoves[] = null;
	// factory method
	public PonteCell newcell(char c,int r)
	{	return(new PonteCell(c,r));
	}
    public PonteBoard(String init,long rv,int np,int map[]) // default constructor
    {   setColorMap(map, np);
    
		Random r = new Random(0xf62736f5);
		for(int i=0;i<PonteChip.N_STANDARD_PIECES; i++)
		{
			PonteCell cell = new PonteCell(r);
			cell.rackLocation=RackLocation[i];
			rack[i]=cell;
		} 
        doInit(init,rv,np); // do the initialization 
     }
    public void doInit(String gtype,long rv)
    {
    	doInit(gtype,rv,players_in_game);
    }


    /* initialize a board back to initial empty state */
	public void doInit(String gtype,long rv,int np)
    {  	drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = GRIDSTYLE; //coordinates left and bottom
    	randomKey = rv;
    	players_in_game = np;
    	blobs.clear();
    	blobsValid = false;
    	robotGame = false;
    	robotDepth = 0;
    	prev_tile_1 = null;
    	prev_tile_2 = null;
    	lastMove = false;
    	lastMoveAtStart = false;
    	int map[] =getColorMap();
    	playerRack[map[0]] = 0;
    	playerRack[map[1]] = 1;
    	alternateBridgeMoves = null;
    	cachedTileMoves = new CommonMoveStack[2];
    	cachedBridgeMoves = new int[2];
    	swapped = false;
   
     	for(PonteCell c : rack) { c.reInit(); }
     	for(int i=0;i<INITIAL_CHIPS;i++)
     		{ rack[White_Chip_Index].addChip(PonteChip.getPiece(White_Chip_Index));
     		  rack[Red_Chip_Index].addChip(PonteChip.getPiece(Red_Chip_Index));
     		}
     	for(int i=0;i<INITIAL_BRIDGES;i++)
     		{ rack[Bridge_Index].addChip(PonteChip.getPiece(Bridge_Index));
     		}
     		
     	{
     	if(Ponte_INIT.equalsIgnoreCase(gtype)) 
     		{ 
     		boardColumns=DEFAULT_COLUMNS; 
     		boardRows = DEFAULT_ROWS;
     		reInitBoard(boardColumns,boardRows)  ;        
     		}
     	else { throw G.Error(WrongInitError,gtype); }
     	gametype = gtype;
     	}
 	    setState(PonteState.Puzzle);
	    
    	emptyCells.clear();
    	occupiedCells.clear();
    	for(PonteCell c = allCells; c!=null; c=c.next) { emptyCells.push(c); } 
	    
	    
	    whoseTurn = FIRST_PLAYER_INDEX;
		pickedSourceStack.clear();
		droppedDestStack.clear();
		stateStack.clear();
		pickedObject = null;
        AR.setValue(win,false);
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }
    
    public PonteBoard cloneBoard() 
 	{ PonteBoard copy = new PonteBoard(gametype,randomKey,players_in_game,getColorMap());
 	  copy.copyFrom(this); 
 	  return(copy);
 	}
    public void copyFrom(BoardProtocol b) { copyFrom((PonteBoard)b); }

     /* make a copy of a board.  This is used by the robot to get a copy
      * of the board for it to manupulate and analyze without affecting 
      * the board that is being displayed.
      *  */
     public void copyFrom(PonteBoard from_b)
     {	
         super.copyFrom(from_b);			// copies the standard game cells in allCells list
         pickedObject = from_b.pickedObject;	
         getCell(pickedSourceStack,from_b.pickedSourceStack);
         getCell(droppedDestStack,from_b.droppedDestStack);
         AR.copy(playerRack,from_b.playerRack);
         swapped = from_b.swapped;
         lastMove = from_b.lastMove;
         lastMoveAtStart = from_b.lastMoveAtStart;
         stateStack.copyFrom(from_b.stateStack);
         copyFrom(rack,from_b.rack);
         getCell(emptyCells,from_b.emptyCells);
         getCell(occupiedCells,from_b.occupiedCells);
         robotGame = from_b.robotGame;
         robotDepth = from_b.robotDepth;
         board_state = from_b.board_state;
         unresign = from_b.unresign;
         if(from_b.alternateBridgeMoves!=null)
        	 { alternateBridgeMoves = new CommonMoveStack();
        	   alternateBridgeMoves.copyFrom(from_b.alternateBridgeMoves);
        	 }
         blobsValid = false;	// force rebuilding of blobs
         sameboard(from_b);
     }
    boolean updateBlobs(PonteCell c,PonteChip chip)
    {	if(!blobsValid) { return(false); }
    	Bridge b = chip.bridge;
    	if(b!=null)
    	{
		int endx = b.otherEnddx;
		int endy = b.otherEnddy;
		c.markShadowB(endx,endy,b);
		c.markShadowA(endx,endy,b);
		return(true);
    	}
    	else
    	{
    	PonteBlob firstBlob = null;
    	for(int direction = PonteBoard.CELL_LEFT(),lim=direction+PonteBoard.CELL_FULL_TURN(),step = PonteBoard.CELL_QUARTER_TURN();
    			direction<lim;
    			direction+=step)
    		{
    		PonteCell adj = c.exitTo(direction);
    		if((adj!=null) && (adj.blob!=null) && (adj.blob.color==chip))
    			{
    			if((firstBlob==null)||(firstBlob==adj.blob))
    				{
    				firstBlob = adj.blob;
    				firstBlob.push(c);
    				c.blob = firstBlob;
    				}
    				else
    				{	// a second blob needs merging
    					PonteBlob deadBlob = adj.blob;
     					for(int dsize=deadBlob.size()-1; dsize>=0; dsize--)
    					{	
    	   					PonteCell dcell = deadBlob.elementAt(dsize);
    	   					firstBlob.push(dcell);
    	   					dcell.blob = firstBlob;
    					}
    					blobs.remove(deadBlob,false);
    				}
    			}   			
    		}
    	if(firstBlob==null)
    	{
    		firstBlob = new PonteBlob(chip);
    		firstBlob.push(c);
    		blobs.push(firstBlob);
    		c.blob = firstBlob;
    	}
    	return(true); 	// we succeeded
    	}
    }
    void findBlobs()
    {	
    	if(!blobsValid)
    	{
   		sweepCounter++;
   		blobs.clear();
   		for(PonteCell c = allCells; c!=null; c=c.next) 
   			{ //boolean hasChips = c.topChip()!=null;
   			  //G.Assert(occupiedCells.contains(c)==hasChips,"occupied mismatch");
   			  //G.Assert(emptyCells.contains(c)!=hasChips,"empty mismatch");
   			  c.shadowBridge = null;
   			  c.blob = null; 
   			}
   		for(int lim = occupiedCells.size()-1; lim>=0; lim--)
    	{	PonteCell c = occupiedCells.elementAt(lim);
    		if(c.sweepCounter != sweepCounter)
    		{
    		c.sweepCounter = sweepCounter;
    		PonteBlob newBlob = new PonteBlob(c.chipAtIndex(0));
    		blobs.push(newBlob);
    		c.extendBlob(newBlob);
     		}
    	}
    	blobsValid = true;
    	}
    }
    


	public void sameboard(BoardProtocol f) { sameboard((PonteBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(PonteBoard from_b)
    {
    	super.sameboard(from_b);	// calls sameCell for each cell, also for inherited class variables.
    	G.Assert(unresign==from_b.unresign,"unresign mismatch");
       	G.Assert(AR.sameArrayContents(win,from_b.win),"win array contents match");
       	G.Assert(sameCells(rack,from_b.rack),"rack mismatch");
       	G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(stateStack.sameContents(from_b.stateStack),"state stack mismatch");
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject doesn't match");
        G.Assert(swapped==from_b.swapped,"swapped mismatch");
        G.Assert(AR.sameArrayContents(playerRack,from_b.playerRack),"playerRack mismatch");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");

    }


    
    /** 
     * Digest produces a 64 bit hash of the game state.  This is used in many different
     * ways to identify "same" board states.  Some are germane to the ordinary operation
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

        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        Random r = new Random(64 * 1000); // init the random number generator
        long v = super.Digest(r);

		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,swapped);
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
        return (v);
    }

    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Move not complete, can't change the current player");
         case Puzzle:
            break;
        case Confirm:
        case PlayBridge:
        case Draw:
        case DoNothing:
        case ConfirmSwap:
        case PlaySecondTile:
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
    {	
        switch (board_state)
        {case Resign:
         case Confirm:
         case ConfirmSwap:
         case PlayBridge:
         case DoNothing:
         case Draw:
            return (true);

        default:
            return (false);
        }
    }


    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(PonteState.Gameover);
    }
    
    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	if(board_state==PonteState.Gameover) { return(win[player]); }
     	return(false);
    }


    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement()
    {	
        pickedObject = null;
        if(!robotGame)
        {
        	int sz = droppedDestStack.size();
        	prev_tile_2 = (sz>=2) ? droppedDestStack.elementAt(1) : null;
        	prev_tile_1 = (sz>=1) ? droppedDestStack.elementAt(0) : null;
        }
        	
        droppedDestStack.clear();
        pickedSourceStack.clear();
        stateStack.clear();
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    G.Assert(pickedObject==null, "nothing should be moving");
    if(droppedDestStack.size()>0)
    	{
    	PonteCell dr = droppedDestStack.pop();
    	PonteState st = stateStack.pop();
    	setState(st);
    	switch(dr.rackLocation())
	    	{
	   		default: throw G.Error("Not expecting rackLocation %s",dr.rackLocation);
			case BoardLocation: 
				pickedObject = removeChip(dr); 
				if(pickedObject.bridge!=null)
					{ PonteCell d = dr.bridgeEnd(pickedObject.bridge);
					  if(d!=null) { d.bridgeEnd = null; }
					}
				break;
			case White_Chip_Pool:	// treat the pools as infinite sources and sinks
			case Red_Chip_Pool:	
				pickedObject = dr.topChip();
				break;	// don't add back to the pool
	    	
	    	}
	    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	PonteChip po = pickedObject;
    	if(po!=null)
    	{
    		PonteCell ps = pickedSourceStack.pop();
    		switch(ps.rackLocation())
    		{
    		default: throw G.Error("Not expecting rackLocation %s",ps.rackLocation);
    		case BoardLocation:
    			addChip(ps,po);
    			break;
    		case White_Chip_Pool:
    		case Red_Chip_Pool:	
    			ps.addChip(po); 
    			break;
    		case Bridge_30:
    		case Bridge_45:
    		case Bridge_60:
    		case Bridge_90:
    		case Bridge_120:
    		case Bridge_150:
    		case Bridge_135:
    		case Bridge_180:
    			ps.addChip(PonteChip.getPiece(Bridge_Index));
    			break;	// don't add back to the pool
    		}
    		pickedObject = null;
     	}
     }

    // 
    // drop the floating object.
    //
    private void dropObject(PonteCell c)
    {   G.Assert(pickedObject!=null,"pickedObject should not be null"); 	    		
    	switch(c.rackLocation())
		{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		
		case BoardLocation:
			addChip(c,pickedObject); 
			if(pickedObject.bridge!=null)
			{	PonteCell d = c.bridgeEnd(pickedObject.bridge);
				if(d!=null) { d.bridgeEnd = pickedObject; }
			}
			break;
		case White_Chip_Pool:
		case Red_Chip_Pool:
			c.addChip(pickedObject); 
			break;
		case Bridge_30:
		case Bridge_45:
		case Bridge_60:
		case Bridge_90:
		case Bridge_135:
		case Bridge_120:
		case Bridge_150:
		case Bridge_180:
			c.addChip(PonteChip.Bridge_30); 
			break;	// don't add back to the pool
		}
       	droppedDestStack.push(c);
       	stateStack.push(board_state);
       	pickedObject = null;
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(PonteCell cell)
    {	return((droppedDestStack.size()>0) && (droppedDestStack.top()==cell));
    }
    public PonteCell getPrevDest() 
    {
    	if(droppedDestStack.size()>=2) { return(droppedDestStack.elementAt(0)); } 
    	return(null);
    }
    public PonteCell getDest()
    {
    	return((droppedDestStack.size()>0) ? droppedDestStack.top() : null);
    }
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	PonteChip ch = pickedObject;
    	if(ch!=null)
    		{ return(ch.chipNumber());
    		}
        return (NothingMoving);
    }
 
    public PonteCell getCell(PonteId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	return(getCell(col,row));
        case Bridge_30:
        case Bridge_45:
        case Bridge_60:
        case Bridge_90:
        case Bridge_120: 
        case Bridge_135:
        case Bridge_150:
        case Bridge_180:       	 
        	return(rack[Bridge_Index]);
        	 
        case White_Chip_Pool:
       		return(rack[White_Chip_Index]);
        case Red_Chip_Pool:
       		return(rack[Red_Chip_Index]);
        }
    }
    public PonteCell getCell(PonteCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(PonteCell c)
    {	G.Assert(pickedObject==null,"pickedObject should be null");
    	switch(c.rackLocation())
    	{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation());
		case BoardLocation: 
			pickedObject = removeChip(c); 
			if(pickedObject.bridge!=null)
			{ PonteCell d = c.bridgeEnd(pickedObject.bridge);
			  if(d!=null) { d.bridgeEnd = null; }
			}
			break;
		case Bridge_30:
		case Bridge_45:
		case Bridge_60:
		case Bridge_90:
		case Bridge_120: 
		case Bridge_135:
		case Bridge_150:
		case Bridge_180:       	 
		case White_Chip_Pool:
		case Red_Chip_Pool:	
			pickedObject = c.removeTop();
			break;	// don't add back to the pool
    	
    	}
    	pickedSourceStack.push(c);
   }
   void pickObject(PonteCell c,PonteId src)
   {
	   pickObject(c);
	   pickedObject = src.chip;
   }

    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(boolean placedTile)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case Confirm:
        case Draw:
        	setNextStateAfterDone(); 
        	break;
        case PlayOrSwap:
        case PlayBridge:
        case PlayTilesOrBridge:
        case PlayTiles:
        	setState(placedTile?PonteState.PlaySecondTile:PonteState.Confirm);
			break;
        case PlaySecondTile:
        	setState(PonteState.Confirm);
        	break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    //	
    //true if col,row is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(PonteCell c)
    {	return(getSource()==c);
    }
    public PonteCell getSource()
    {
    	return((pickedSourceStack.size()>0) ?pickedSourceStack.top() : null);
    }
    private boolean canPlayBridgeMoves()
    {
		return(addBridgeMoves(null,whoseTurn));
    }

    private boolean canPlayTwoTileMoves()
    {	PonteCell target = rack[playerRack[whoseTurn]];
    	if(target.height()<2) { return(false); }
		CommonMoveStack tileMoves = new CommonMoveStack();
		boolean some = false;
		addTileMoves(tileMoves,target);
		if(tileMoves.size()>=2)
		{	PonteChip targetChip = rack[playerRack[whoseTurn]].topChip();
			for(int lim = tileMoves.size()-1; !some && lim>=0; lim--)
			{
				PonteMovespec m = (PonteMovespec)tileMoves.elementAt(lim);
				PonteCell c = getCell(m.col,m.row);
				addChip(c,targetChip);
				blobsValid = false;
				some = addTileMoves(null,target);
				removeChip(c);
				blobsValid = false;
			}
		}
		return(some);
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: G.Error("Not expecting state %s",board_state);
    		break;
    	case Gameover: 
    		break;

        case Draw:
        	setGameOver(false,false);
        	break;
        case DoNothing:			// pass after having no moves available.
        case PlayBridge:		// pass after only having bridges available.
        	lastMove = true;
			//$FALL-THROUGH$
		case Confirm:
    	case Puzzle:
    	case ConfirmSwap:
    	case PlayTilesOrBridge:
    		
    		if(!swapped
    				&& (rack[White_Chip_Index].height()==(INITIAL_CHIPS-2))
    				&& (rack[Red_Chip_Index].height()==INITIAL_CHIPS))
    		{
    		// red's first move, gets play_or_swap
    		setState(PonteState.PlayOrSwap);	
    		}
    		else if(lastMove && (playerRack[whoseTurn]==0)) { setGameOver(); }
    		else
    		{
    		setState(PonteState.PlayTilesOrBridge);
    	
    		if(!robotGame)
    			{
    			// test if we can play 2 tiles, or any tiles at all, so the user
    		    // interface has the right prompts and restrictions.
    			boolean canPlayBridges = canPlayBridgeMoves();
    			if(canPlayTwoTileMoves())
    				{
    				if(!canPlayBridges) { setState(PonteState.PlayTiles); }
    				}
    				else if(canPlayBridges) 
    					{ setState(PonteState.PlayBridge); 
    					}
    				else if(playerRack[whoseTurn]==0) 
    					{ setState(PonteState.DoNothing); 
    					}
    				else { setGameOver();
    					}
    			}
    		}
    		break;
    	}
       	lastMoveAtStart = lastMove;
    }
   

    
    private void doDone()
    {	
        acceptPlacement();

        if (board_state==PonteState.Resign)
        {	setGameOver(false,true);
        }
        else
        {	boolean win1 = WinForPlayerNow(whoseTurn);
        	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
        	if(win1 || win2)  { setGameOver(win1,win2); }
        	else {setNextPlayer(); setNextStateAfterDone(); }
        }
    }
    public int scoreForPlayer(int who)
    {
    	return(extendedScoreForPlayer(who)/10000);
    }
    public int extendedScoreForPlayer(int who)
    {	// connected islands + islands + bridges
    	PonteChip targetColor = PonteChip.getTile(playerRack[who]);
     	int bridges = 0;
    	int islands = 0;
    	int connection = 0;
    	int networkNumber = 0;
    	int connectedScore[] = { 0, 1, 3, 6, 10, 15, 21, 28, 36, 45, 55};	// index by number of connected islands
    	findBlobs();
    	sweepCounter++;
    	for(int lim=blobs.size()-1; lim>=0; lim--)
    	{	PonteBlob b = blobs.elementAt(lim);
    		if(b.color==targetColor)
    		{	int bridgesOut = b.countBridges();
    			if(b.size()==4) { islands++; }
    			bridges += bridgesOut;
    			if((b.sweepCounter!=sweepCounter))
    			{	// count the islands in the network.  This is a new one or we would have been counted already.
    				b.sweepCounter = sweepCounter;
    				int networkSize =(b.size()==4)?1:0;
    				b.networkNumber = ++networkNumber;
    				//G.print("\nFrom "+b);
    				if(bridgesOut>0)
    				{	
    					networkSize += extendBlobNetwork(b,networkNumber);
    				}
    				int prevSize = 0;
    				do
    				{
    				prevSize = networkSize;
    				for(int more = lim-1; more>=0; more--)
					{	// get the unprocessed networks which eventually link to our network
						PonteBlob b2 = blobs.elementAt(more);
						if(b2.color==b.color)
							{int conn = connectBlobNetwork(b2,networkNumber);
							if(conn>0)
								{ networkSize += conn; }
							}
					}} while(prevSize!=networkSize);
  				
    				connection += connectedScore[networkSize];
    			}
    		}
    		else { b.sweepCounter = sweepCounter; }	// ignore off-color blobs
    	}
    	return(connection*10000+islands*100+bridges);
    }
    
    // extend blob network b by following bridges
    // return the number of blobs tagged
    public int extendBlobNetwork(PonteBlob b,int networkNumber)
    {	int networkSize = 0;
    	//G.print("Extend "+b);
    	for(int lim=b.size()-1; lim>=0; lim--)
    	{	PonteCell c = b.elementAt(lim);
    		PonteChip top = c.topChip();
    		if(top.bridge!=null)
    		{
    			PonteCell d = c.bridgeEnd(top.bridge);
    			if(d!=null)
    			{
    			PonteBlob bb = d.blob;
    			if((bb!=null) && (bb.sweepCounter!=sweepCounter))
    			{
    				if(bb.size()==4)
    					{ networkSize++; 
    					}
    				bb.sweepCounter = sweepCounter;
    				bb.networkNumber = networkNumber;
    				networkSize += extendBlobNetwork(bb,networkNumber);
    			}}
    		}
    	}
    	//G.print("Extended "+b+" "+networkSize);
    	return(networkSize);
    }
    //
    // if this blob is previously seen, return 0 if part of networkNumber or -1 if not
    // if this blob is unseen and recursively connected to networkNumber, return number of links deep
    public int connectBlobNetwork(PonteBlob b,int networkNumber)
    {	
    	if(b.sweepCounter==sweepCounter)
    	{	
    		if(networkNumber==b.networkNumber) { return(0); } else { return(-1); }
    	}
    	else
    	{ 	// previously unseen node
    		b.sweepCounter = sweepCounter;
    		for(int lim=b.size()-1; lim>=0; lim--)
    		{	PonteCell c = b.elementAt(lim);
    			PonteChip top = c.topChip();
    			if(top.bridge!=null)
    			{
    			PonteCell d = c.bridgeEnd(top.bridge);
    			if(d!=null)
    			{
    			PonteBlob bb = d.blob;
    			if(bb!=null)
    				{int n = connectBlobNetwork(bb,networkNumber);
    			if(n>=0)
    				{ // we're part of the network
    				 //G.print("Connected "+b);
     				  b.sweepCounter = sweepCounter;
    				  b.networkNumber =networkNumber;
    				  if(b.size()==4) 
    				  	{ n++; 
    				  	}
				  	  n += extendBlobNetwork(b,networkNumber);	// make sure we get them all
				    	//G.print("Connect "+b+" "+n);
    				  return(n);
    				}}
    			
    			}}
     		}
    		// not connected
    		b.sweepCounter = 0;	// and still unknown
    		return(-1);
    	}
    }
    public void setGameOver()
    {	setState(PonteState.Gameover);
    	int score0 = extendedScoreForPlayer(0);
    	int score1 = extendedScoreForPlayer(1);
    	score[0] = score0;
    	score[1] = score1;
    	win[0] = score0 > score1;
    	win[1] = score1 > score0;
    }

    private int playerIndex(PonteChip ch) { return(playerRack[ch.colorIndex()]); }

    public boolean Execute(commonMove mm,replayMode replay)
    {	PonteMovespec m = (PonteMovespec)mm;
    	CommonMoveStack nextAB = null;
    	boolean nextBlobsValid = false;
        if(replay.animate) { animationStack.clear(); }
        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_PLACE_BRIDGE:
        	// robot places a bridge
        	{PonteCell src = getCell(m.source,m.col,m.row);
    		PonteCell dest = getCell(PonteId.BoardLocation,m.col,m.row);
    		pickObject(src,m.source);
    		if(!robotGame)
    			{PonteCell end = dest.bridgeEnd(pickedObject.bridge);
    			if(end!=null) { m.bridgeEnd = ""+end.col+end.row; }
    			}
    		dropObject(dest);
    		
    		setState(PonteState.Confirm);
    		if(replay.animate)
    			{
    			animationStack.push(src);
    			animationStack.push(dest);
    			}
    		break;
        	}
        case MOVE_PLACE_TILE:
        	// robot places a tile
        	{	PonteCell src = getCell(m.source,m.col,m.row);
        		PonteCell dest = getCell(PonteId.BoardLocation,m.col,m.row);
        		pickObject(src);
        		PonteChip po = pickedObject;
        		dropObject(dest);
        		nextBlobsValid = updateBlobs(dest,po);
        		//if(nextBlobsValid)
        		//{	// debug code
        		//	int h = blobs.size();
        		//	OStack<PonteBlob>cp = new OStack<PonteBlob>(PonteBlob.class);
        		//	cp.copyFrom(blobs);
        		//	blobsValid = false;
        		//	findBlobs();
        		//	G.Assert(h==blobs.size(),"update worked");
        		//}
        		if(replay.animate)
    			{
    			animationStack.push(src);
    			animationStack.push(dest);
    			}
        		setNextStateAfterDrop(true);
        	}
        	break;
        case MOVE_DONE:
         	doDone();
            break;
        case MOVE_PASS:
        	setState(PonteState.Confirm);
        	lastMove = true;
        	break;
        case MOVE_BRIDGE_END:
        	if(alternateBridgeMoves!=null)
        	{	nextAB = alternateBridgeMoves;
        		for(int lim = alternateBridgeMoves.size()-1; lim>=0; lim--)
        		{	PonteCell dest = getCell(m.col,m.row);
        			PonteMovespec mv = (PonteMovespec)alternateBridgeMoves.elementAt(lim); 
        			PonteCell start = getCell(mv.col,mv.row);
        			PonteChip bridge = mv.source.chip;
        			PonteCell end = start.bridgeEnd(bridge.bridge);
        			if(end==dest)
        			{	// no net change in occupied status
        				PonteChip oldBridge = start.removeTop();
        				PonteCell oldEnd = start.bridgeEnd(oldBridge.bridge);
        				if(oldEnd!=null) { oldEnd.bridgeEnd = null; }
        				start.addChip(bridge);
        				end.bridgeEnd = bridge;
        			}
        			if(!robotGame)
        			{
        			m.bridgeEnd = ""+dest.col+dest.row;
        			}
        		}
        	}
        	break;
        case MOVE_DROPB:
			{
			PonteCell dest = getCell(PonteId.BoardLocation, m.col, m.row);
			PonteCell src = getSource();
        	G.Assert(pickedObject!=null,"something is moving");
			
            if(src==dest)
            	{ 
            	  unPickObject(); 

            	} 
            	else 
            	{if(pickedObject.bridge!=null)
            	{	// dropping a bridge, get the orientation right
            		
            		CommonMoveStack all = new CommonMoveStack();
            		findBlobs();
            		addBridgeMoves(all,playerIndex(dest.topChip()),dest);
            		nextAB = all;		// save for the GUI
            		if(all.size()>0) 
            			{  PonteMovespec bm = (PonteMovespec)all.top(); 
            			   pickedObject = bm.source.chip;
            			}
            		if(!robotGame)
        			{PonteCell end = dest.bridgeEnd(pickedObject.bridge);
        			if(end!=null) 
        				{ m.bridgeEnd = ""+end.col+end.row;            			  
        				  m.bridgeId = pickedObject.id;
        			    }
        			}
            		dropObject(dest);
            		setNextStateAfterDrop(false);
            	}
            	else
            	{
            		dropObject(dest);
            		setNextStateAfterDrop(true);
            		}
            	if(replay==replayMode.Single)
    			{
    			animationStack.push(src);
    			animationStack.push(dest);
    			}
            	}
			}
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	if(isDest(getCell(m.col,m.row)))
        		{ unDropObject(); 
         		}
        	else 
        		{ pickObject(getCell(PonteId.BoardLocation, m.col, m.row));
        			// if you pick up a gobblet and expose a row of 4, you lose immediately
        		  switch(board_state)
        		  {	default: throw G.Error("Not expecting pickb in state %s",board_state);
        		  	case PlayTilesOrBridge:
        		  		// if we pick a piece off the board, we might expose a win for the other player
        		  		// and otherwise, we are comitted to moving the piece
         		  		break;
        		  	case Puzzle:
        		  		break;
        		  }
         		}
 
            break;

        case MOVE_DROP: // drop on chip pool;
        	{
        	PonteCell c = getCell(m.source, m.col, m.row);
        	if(isSource(c)) { unPickObject(); }
        	else {
        		dropObject(c);
        		setNextStateAfterDrop(false);
        		}
        	}
            break;

        case MOVE_PICK:
        	{
        	PonteCell c = getCell(m.source, m.col, m.row);
        	if(isDest(c)) { unDropObject(); }
        	else { pickObject(c); }
        	}
            break;


        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(PonteState.Puzzle);
            {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;

        case MOVE_RESIGN:
        	setState(unresign==null?PonteState.Resign:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            // standardize "gameover" is not true
            setState(PonteState.Puzzle);
 
            break;
        case MOVE_SWAP:
        	setState((board_state==PonteState.ConfirmSwap)?PonteState.PlayOrSwap:PonteState.ConfirmSwap);
        	doSwap();
        	AR.setValue(cachedTileMoves,null);	// keep cached moves in sync
        	AR.setValue(cachedBridgeMoves,0);	// shouldn't matter, but the form says do it here too.
        	break;
        case MOVE_GAMEOVER:
        	setGameOver();
        	break;
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;
		default:
        	cantExecute(m);
        }

        blobsValid = nextBlobsValid;
        alternateBridgeMoves = nextAB;
        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips(int player,PonteCell cell)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
         case PlayBridge:
        	 return(cell==rack[Bridge_Index]);
         case PlaySecondTile:
         case PlayTiles:
        	 if(cell==rack[Bridge_Index]) { return(false); }
        	 // or fall through
			//$FALL-THROUGH$
		case Draw:
         case PlayOrSwap:
         case PlayTilesOrBridge: 
         	return((pickedObject==null)
        			?(player==whoseTurn)
        			:isSource(cell));

         case Confirm:
         case ConfirmSwap:
         case Resign:
         case DoNothing:
         case Gameover:
			return(false);
        case Puzzle:
        	return((pickedObject==null)
        			?true
        			:((cell==rack[Bridge_Index]) || (playerRack[player]==pickedObject.chipNumber())));
        }
    }
  

    public boolean legalToHitBoard(PonteCell cell,Hashtable<PonteCell,PonteMovespec>dests)
    {	
        switch (board_state)
        {
 		case PlayTilesOrBridge:
 		case PlaySecondTile:
 		case PlayTiles:
 		case PlayBridge:
 		case PlayOrSwap:
 			if(pickedObject!=null)
	 			{
	 			return(dests.get(cell)!=null);	
	 			}
 			else 
 			{ return(isDest(cell));
 			}
 		case ConfirmSwap:
 		case Resign:
		case Gameover:
		case DoNothing:
			return(false);
		case Confirm:
		case Draw:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case Puzzle:
        	return(pickedObject==null
        		?(cell.chipIndex>=0)
        		:pickedObject.bridge==null
        			?(cell.chipIndex<0)
        			:(cell.chipIndex>=0));
        }
    }
  public boolean canDropOn(PonteCell cell)
  {		PonteCell top = (pickedObject!=null) ? pickedSourceStack.top() : null;
  		return((pickedObject!=null)				// something moving
  			&&(top.onBoard 			// on the main board
  					? (cell!=top)	// dropping on the board, must be to a different cell 
  					: (cell==top))	// dropping in the rack, must be to the same cell
  				);
  }
 
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(PonteMovespec m)
    {
    	// to undo state transistions is to simple put the original state back.
        //G.print("R "+m);
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        robotDepth++;
        if (Execute(m,replayMode.Replay))
        {
            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {
                doDone();
            }
        }
    }
 

 boolean addBridgeMoves(CommonMoveStack  all,int whoseTurn,PonteCell c)
 { 	
	boolean some = false;
 	if(c.shadowBridge==null)
 	{
	PonteChip top = c.topChip();
	PonteChip targetChip = PonteChip.getTile(playerRack[whoseTurn]);
	 if(top==targetChip)
	 	{	for(Bridge b : bridges)
	 		{
	 		if(c.canPlaceBridge(b))
	 			{	some = true;
	 				if(all==null) { return(true); }
	 				all.push(new PonteMovespec(c,b,whoseTurn));
	 			}
	 		}
	 	}}
 	return(some);
 }
 boolean addBridgeMoves(CommonMoveStack  all,int whoseTurn)
 {	// tiles of the appropriate color can anchor a bridge
	 findBlobs();
	 boolean some = false;
	 if(rack[Bridge_Index].height()>0)
	 {
	 for(int lim = occupiedCells.size()-1; lim>=0; lim--)
	 {	PonteCell c = occupiedCells.elementAt(lim);
	 	some |= addBridgeMoves(all,whoseTurn,c);
	 	if(some && (all==null)) { return(some); }
	 }}
	 return(some);
 }
 private int sumOfAdjacentBlobs(PonteCell c,PonteChip targetColor)
 {	int n = 0;
 	sweepCounter++;
 	for(int direction = PonteBoard.CELL_LEFT(),lim=direction+PonteBoard.CELL_FULL_TURN(),step = PonteBoard.CELL_QUARTER_TURN();
			direction<lim;
			direction+=step)
 		{	PonteCell adj = c.exitTo(direction);
 			if(adj!=null)
 				{ PonteBlob blob = adj.blob;
 					// count each blob once. There's a possibility that the same blob
 					// is adjacent twice.
 				 if((blob!=null)
 						 && (blob.sweepCounter!=sweepCounter)
 						 && (adj.chipAtIndex(0)==targetColor)
 						 )
 				 	{	n += blob.size();
 				 		blob.sweepCounter=sweepCounter;
 				 	}
 				}
 		}
 	return(n);
 }
 boolean anyAdjacentToIsland(PonteCell c,PonteChip targetColor,boolean islandsOnly)
 {	if(c.diagonallyAdjacentToIsland(targetColor,islandsOnly,true,c)) { return(true); }
 	for(int direction = PonteBoard.CELL_LEFT(),lim=direction+PonteBoard.CELL_FULL_TURN(),step = PonteBoard.CELL_QUARTER_TURN();
			direction<lim;
			direction+=step)
 		{	PonteCell adj = c.exitTo(direction);
 			if(adj!=null)
 			{	PonteBlob blob = adj.blob;
 				if(blob!=null && (adj.chipAtIndex(0)==targetColor))
 				{
 				for(int sz = blob.size()-1; sz>=0; sz--)
 				{	PonteCell bcell = blob.elementAt(sz);
 					if(bcell.diagonallyAdjacentToIsland(targetColor,islandsOnly,false,c)) { return(true); }
 				}}
 			}
 		}
 	return(false);
 }
 boolean validTileMove(PonteCell c,PonteChip targetColor)
 {
	 if(c.shadowBridge==null)
	 {	int sum = sumOfAdjacentBlobs(c,targetColor);
	 	switch(sum)
	 	{
	 	default: return(false);
	 	case 3:
	 		return(!anyAdjacentToIsland(c,targetColor,false));
	 	case 2:
	 	case 1:
	 	case 0: return(!anyAdjacentToIsland(c,targetColor,true));
	 	}
	 }
	 return(false);
 }
 boolean addTileMoves(CommonMoveStack  all,PonteCell targetCell,PonteCell c)
 {	
 	PonteChip targetColor = targetCell.topChip();
 	if(validTileMove(c,targetColor))
 	{
 		if(all!=null) 
 		{ 
 		all.push(new PonteMovespec(c,targetCell,whoseTurn));
 		}
 		return(true);
 	}
 	return(false);
 }
 	
 boolean addTileMoves(CommonMoveStack  all,PonteCell target)
 {	findBlobs();
 	boolean some = false;
 	for(int lim = emptyCells.size()-1; lim>=0; lim--)
 	{	PonteCell c = emptyCells.elementAt(lim);
 		some |= addTileMoves(all,target,c);	
 		if(some && (all==null)) { return(some); }
	 }
 	return(some);
 }
 
 void getDests(int who,PonteChip obj, Hashtable<PonteCell,PonteMovespec>dests,CommonMoveStack  all )
 {	if(obj==null) { }
 	else if(obj.bridge!=null)
		{
		addBridgeMoves(all,who);
		}
	else 
		{
		addTileMoves(all,rack[playerRack[who]]);
		}
	
 	while(all.size()>0)
		{
			PonteMovespec m = (PonteMovespec)all.pop();
			PonteCell c = getCell(m.col,m.row);
			m.next = dests.get(c);		// link all the moves that use a cell, for bridges
			dests.put(c,m);
		}
 }

 Hashtable<PonteCell,PonteMovespec> getDests(int who,PonteChip obj)
 {	Hashtable<PonteCell,PonteMovespec>dests = new Hashtable<PonteCell,PonteMovespec>();
 	CommonMoveStack  all = new CommonMoveStack();
 	if(obj!=null)
 	{	getDests(who,obj,dests,all);
	}
 	else { 
 		getDests(who,rack[playerRack[who]].topChip(),dests,all);
 		getDests(who,rack[Bridge_Index].topChip(),dests,all);
 		}
	return(dests);
 }
 
 Hashtable<PonteCell,PonteMovespec> getDests()
 {	
	 Hashtable<PonteCell,PonteMovespec> dess =
			 getDests(whoseTurn, pickedObject);
	 return(dess);
 } 
 Hashtable<PonteCell,PonteMovespec> getAlternateBridgeEnds()
 {	Hashtable<PonteCell,PonteMovespec> result = new Hashtable<PonteCell,PonteMovespec>();
 	if(board_state==PonteState.Confirm)
 	{	if(alternateBridgeMoves!=null)
 		{
 		for(int lim=alternateBridgeMoves.size()-1; lim>=0; lim--)
 		{
 			PonteMovespec m = (PonteMovespec)alternateBridgeMoves.elementAt(lim);
 			PonteCell c = getCell(m.col,m.row);
 			PonteCell d = c.bridgeEnd(m.source.chip.bridge);
 			result.put(d,m);
 		}
 		}
 	}
 	return(result);
 }
 void addCachedBridgeMoves(CommonMoveStack all,int whoseTurn)
 {	int sz = all.size();
 	// we don't try to maintain a cache of bridge moves, but note how many there were
 	addBridgeMoves(all,whoseTurn);
 	cachedBridgeMoves[playerRack[whoseTurn]] = all.size()-sz;
 }
 
 void addCachedTileMoves(CommonMoveStack  all,int whoseTurn)
 {	CommonMoveStack moves = cachedTileMoves[playerRack[whoseTurn]];
 	findBlobs();
 	//int oldsize = all.size();
 	if(moves==null)
 	{
 	moves = new CommonMoveStack();
 	addTileMoves(moves,rack[playerRack[whoseTurn]]);
 	cachedTileMoves[playerRack[whoseTurn]] = moves;
 	if(all!=null) 
 		{for(int lim=moves.size()-1; lim>=0; lim--)
 			{
 			all.push(moves.elementAt(lim));
 			}
 		}
 	}
 	else {
 		PonteChip targetColor = rack[playerRack[whoseTurn]].topChip();
 		for(int lim = moves.size()-1; lim>=0; lim--)
 		{
 			PonteMovespec m = (PonteMovespec)moves.elementAt(lim);
 			PonteCell target = getCell(m.col,m.row);
 			if((target.topChip()==null) && validTileMove(target,targetColor))
 			{
 				if(all!=null) { all.push(m); }
 			}
 			else {
 				moves.remove(m,false);
 			}
 		}
 	}
 	// debugging code
 	//{
 	//	CommonMoveStack newmoves = new CommonMoveStack();
 	//	addTileMoves(newmoves,whoseTurn);
 	//	G.Assert(newmoves.size()==all.size()-oldsize,"same moves generated");
 	//}
	 
 }
 
 private commonMove getRandomTileMove(int whoseTurn,int randomN,Random ran)
 {	int rackIndex = playerRack[whoseTurn];
 	PonteChip targetColor = rack[rackIndex].topChip();
 	CommonMoveStack tileMoves = cachedTileMoves[rackIndex];
 	int tilen = tileMoves.size();
 	if((targetColor==null) || (tilen==0)) { return(null); }
 	if (randomN<0) { randomN = ran.nextInt(tilen); }
	 do {
			PonteMovespec m = (PonteMovespec)tileMoves.elementAt(randomN);	// may not be a valid move
			PonteCell target = getCell(m.col,m.row);
			if((target.topChip()==null) && validTileMove(target,targetColor))
				{
				return(m);
				}
			// not a valid move, so remove it and try again
			tileMoves.remove(randomN,false);
			tilen--;
			if(tilen>0) { randomN = ran.nextInt(tilen); }
			} while(tilen>0);
	 return(null);
 }
 public commonMove getRandomBridgeMove(int whoseTurn,Random ran,int extra)
 {	CommonMoveStack  all = new CommonMoveStack();
	addCachedBridgeMoves(all,whoseTurn);
	int sz = all.size();
	int n =(sz==0)? 0 : ran.nextInt(sz+extra);
	if(n<sz) { return(all.elementAt(n));}
	return(null);
 }
 
 public commonMove getRandomMove(Random ran,double bridgeMultiplier)
 {	int rackIndex = playerRack[whoseTurn];
 	switch(board_state)
 	{
 	default: throw G.Error("Not implemented");
 	case PlayBridge:
 		{
 		commonMove m = getRandomBridgeMove(whoseTurn,ran,1);
 		if(m!=null) { return(m);}
 		return(new PonteMovespec( (rackIndex==0) ? MOVE_PASS : MOVE_GAMEOVER,whoseTurn));
 		}
 	case PlayTilesOrBridge:
 		{
 	 	CommonMoveStack tileMoves = cachedTileMoves[rackIndex];
 	 	if(tileMoves==null) { addCachedTileMoves(null,whoseTurn); tileMoves =cachedTileMoves[rackIndex]; }		
	 	commonMove tileMove = getRandomTileMove(whoseTurn,-1,ran);
		commonMove bridgeMove = getRandomBridgeMove(whoseTurn,ran,(tileMove==null)?1:0);	// get a bridge move
 		int tilen = tileMoves.size();							// approximate count of tile moves
 		int bridgen = cachedBridgeMoves[rackIndex];				// approximate count of bridge moves
 
 		if(tileMove==null)
 		{
 			if(bridgeMove==null) { bridgeMove = new PonteMovespec( (rackIndex==0) ? MOVE_PASS : MOVE_GAMEOVER,whoseTurn); }
 			return(bridgeMove);
 		}
 		else if(bridgeMove==null) { return(tileMove); }
 		else { 
 			// proportionally choose tile or bridge
 			int randomN = ran.nextInt((int)(bridgen*bridgeMultiplier)+tilen);
 			return( (randomN<tilen) ? tileMove : bridgeMove);
 		}}

 	case PlayOrSwap:
 		// special case, there can only be 98 tile moves, swap is number 99
 		int maxm = boardColumns*boardRows-2;
 		int randN = ran.nextInt(maxm+1);
 		if(randN>=maxm) { return(new PonteMovespec(MOVE_SWAP,whoseTurn)); }
		//$FALL-THROUGH$
	case PlayTiles:
 	case PlaySecondTile:
 		{
 		CommonMoveStack tileMoves = cachedTileMoves[rackIndex];
 		if(tileMoves==null) { addCachedTileMoves(null,whoseTurn); tileMoves =cachedTileMoves[rackIndex]; }		
 		commonMove m = getRandomTileMove(whoseTurn,-1,ran);
 		 if(m!=null) { return(m); }
 		 return(new PonteMovespec( (playerRack[whoseTurn]==0) ? MOVE_PASS : MOVE_GAMEOVER,whoseTurn));
 		}

 	}
 }
 
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
 	switch(board_state)
 	{
 	default: throw G.Error("Not implemented");
 	case Confirm:
 	case ConfirmSwap:
 	case DoNothing:
 	case Resign:
    case Draw:
 		all.push(new PonteMovespec(MOVE_DONE,whoseTurn));
 		break;
 	case PlayBridge:
 		addCachedBridgeMoves(all,whoseTurn);
 		// white playayer can pass, dark player can end the game.
 		all.push(new PonteMovespec( (playerRack[whoseTurn]==0) ? MOVE_PASS : MOVE_GAMEOVER,whoseTurn)); 
 		break;
 	case PlayTilesOrBridge:
 		addCachedBridgeMoves(all,whoseTurn);
		//$FALL-THROUGH$
	case PlayTiles:
 	case PlayOrSwap:
 	case PlaySecondTile:
 		{int sz = all.size();
 	 	 PonteCell targetCell = rack[playerRack[whoseTurn]];
 	 	 int modeHeight = (board_state==PonteState.PlaySecondTile) ? 1 : 2;
 	 	 if(targetCell.height()>=modeHeight)
 	 	 	{
 	 		 addCachedTileMoves(all,whoseTurn);
 	 	 	}
 		 if((sz==all.size()) && (all.size()==0))
 		 	{	
 			 	if(whoseTurn==0) { all.push(new PonteMovespec(MOVE_PASS,whoseTurn)); }		// can pass
 		 		else { all.push(new PonteMovespec(MOVE_GAMEOVER,whoseTurn)); }
 		 	}
 		 if(board_state==PonteState.PlayOrSwap) { all.push(new PonteMovespec(MOVE_SWAP,whoseTurn)); }
 		break;
 	}
 	}
  	return(all);
 }
 
}
