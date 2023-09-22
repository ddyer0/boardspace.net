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
package carnac;

import online.game.*;

import java.util.*;

import carnac.CarnacChip.FaceColor;
import carnac.CarnacChip.FaceOrientation;
import lib.*;
import lib.Random;

/**
 * CarnacBoard knows all about the game of Carnac.
 * 
 * @author ddyer
 *
 */

class CarnacBoard extends squareBoard<CarnacCell> implements BoardProtocol,CarnacConstants
{   
	static final String[] CARNACGRIDSTYLE = { "1",null, "A" }; // left and bottom numbers
	static final int N_STARTING_MENHIR = 28;

	CarnacState unresign;
	CarnacState board_state;
	public CarnacState getState() {return(board_state); }
	public void setState(CarnacState st) 
	{ 	unresign = (st==CarnacState.RESIGN_STATE)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public variation rules = null;
    public int boardColumns = 0;	// size of the board
    public int boardRows = 0;
    public void SetDrawState() { throw G.Error("Not expected"); }
    public FaceColor playerColor[] = new FaceColor[2];
    public CarnacCell pool[] = null;			// samples of the four canonical chips
    public int poolSize;						// number of chips remaining in the pool
    public int emptySize;						// number of empty squares remaining
	public CarnacCell lastPlaced = null;		// last placed standing menhir
	public CarnacCell lastTipped = null;		// last tipped menhir
	public CarnacChip lastPlacedChip = null;	// the chip the was placed (of the 4 possible)
	private CarnacCell pickedLastPlaced = null;
	private CarnacCell pickedLastTipped = null;
	private CarnacChip pickedLastChip = null;
	
	private int nDolmonds[] = new int[2];		// dolmonds per player
	private int dolmondCounts[][] = null;		// the counts of sizes, highest count last
	private boolean nDolmondsValid = false;		// true if nDolmonds and domnondCounts are valid
	private int sweep_counter = 0;
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public CarnacChip pickedObject = null;
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    public CellStack placedMenhir = new CellStack();
	// factory method
	public CarnacCell newcell(char c,int r)
	{	return(new CarnacCell(c,r));
	}
    public CarnacBoard(String init,long key,int map[]) // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = CARNACGRIDSTYLE; //coordinates left and bottom
       	pool = new CarnacCell[4];
		Random r = new Random(67246765);
		setColorMap(map, 2);
    	for(int i=0;i<4;i++)
    	{	pool[i] = new CarnacCell(r,CarnacId.Chip_Pool,i);
    		pool[i].addChip(CarnacChip.getChip(i));
    	}
        doInit(init,key); // do the initialization 
     }
    
    // this is an ad-hoc adjustment needed because the 
    // cell size is thrown off by the absurdly large margins
    // on the smaller board sizes.
    public double adjustedCellSize()
    {
    	switch(rules)
    	{
    	case carnac_8x5: return(displayParameters.CELLSIZE*0.55);
    	case carnac_10x7: return(displayParameters.CELLSIZE*0.75);
    	case carnac_14x9: return(displayParameters.CELLSIZE);
    	default: throw G.Error("Not expecting %s",rules);
    	}
    }
    // find the size of the dolmond which contains c
    private int dolmondSize(CarnacCell c,FaceColor color)
    {	if(c==null) { return(0); }
    	if(c.sweep_counter==sweep_counter) { return(0); }
    	CarnacChip top = c.topChip();
    	if(top==null) { return(0); }
    	if(top.getTopColor()!=color) { return(0); }
    	c.sweep_counter = sweep_counter;
    	int sz = 1;
    	for(int dir=0;dir<CELL_FULL_TURN; dir++)
    	{
    		sz += dolmondSize(c.exitTo(dir),color);
    	}
    	return(sz);
    }
    // return the max number of dolmonds for either player.
    // if sz is not null, if must be a suitably sized array and is filled
    // with the sizes of the dolmonds
    Thread myThread = null;
    private int countDolmonds(int sz[][])
    {	
    	AR.setValue(nDolmonds,0);
    	sweep_counter++;
    	for(CarnacCell c = allCells; c!=null; c=c.next)
    	{	
    		if(c.sweep_counter!=sweep_counter)
    		{	CarnacChip top = c.topChip();
     			if(top!=null)
    			{	FaceColor color = top.getTopColor();
     				int ord = (color==playerColor[0])?0:1;
    				int count = dolmondSize(c,color);
    				if(count>=3)
    					{if(sz!=null)
		    				{	sz[ord][nDolmonds[ord]] = count;
		    				}
	    				nDolmonds[ord]++;
    					}
    			}
    		}
    	}
    	return(Math.max(nDolmonds[0],nDolmonds[1]));
    }
    
    // get the number of dolmonds for player
    public int getNDolmonds(int pl)
    {
    	if(!nDolmondsValid) { countDolmonds(null); }
    	return(nDolmonds[pl]);
    }
    // get the dolmond counts, given nn is the length of the array needed.
    private int[][] getDolmondCounts(int nn)
    {	int sz[][] = new int[2][nn];
		int sz0[] = sz[0];
		int sz1[] = sz[1];
		countDolmonds(sz);	// count again, saving the sizes
		Arrays.sort(sz0);
		Arrays.sort(sz1);
		return(sz);
    }
    // get the counts of dolmonds for each player, sorted in ascending order of size
    public int[][] getDolmondCounts()
    {	if(nDolmondsValid) 
    		{
    			dolmondCounts = getDolmondCounts(Math.max(nDolmonds[0],nDolmonds[1]));
    		}
    		else
    		{	dolmondCounts = getDolmondCounts(Math.max(getNDolmonds(0),getNDolmonds(1)));
    			nDolmondsValid = true;
    		}

    	G.Assert(dolmondCounts!=null,"counts");
     	return(dolmondCounts);
    }
    
    // call this to end the game when we run out of menhir or places to put them
    public void setGameOver()
    {	setState(CarnacState.GAMEOVER_STATE);
    	countDolmonds(null);
    	win[0] = nDolmonds[0]>nDolmonds[1];
    	win[1] = nDolmonds[1]>nDolmonds[0];
    	if( nDolmonds[0]==nDolmonds[1] )
    	{	// same number, have to count and so on
    		int nn = nDolmonds[0];
    		int sz[][] = getDolmondCounts();
    		int sz0[] = sz[0];
    		int sz1[] = sz[1];
    		for(int i=nn-1;i>=0;i--) 
    		{
    			if(sz0[i]!=sz1[i])
    			{
    				win[0] = sz0[i]>sz1[i];
    		    	win[1] = sz1[i]>sz0[i];
    		    	return;
    			}
    		}
    	}
    
    	
    }
	public void sameboard(BoardProtocol f) { sameboard((CarnacBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(CarnacBoard from_b)
    {
    	super.sameboard(from_b);	// calls sameCell for each cell
    	
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject doesn't match");
        G.Assert(poolSize==from_b.poolSize,"pool size mismatch");
        G.Assert(emptySize==from_b.emptySize,"emptysize mismatch");
        G.Assert(sameCells(lastPlaced,from_b.lastPlaced),"lastPlaced mismatch");
        G.Assert(sameCells(lastTipped,from_b.lastTipped),"lastTipped mismatch");
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
		v ^= Digest(r,lastPlaced);
		v ^= Digest(r,lastTipped);
		v ^= Digest(r,poolSize);
		v ^= Digest(r,emptySize);
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
        return (v);
    }
   public CarnacBoard cloneBoard() 
	{ CarnacBoard copy = new CarnacBoard(gametype,randomKey,getColorMap());
	  copy.copyFrom(this); 
	  return(copy);
	}

   public void copyFrom(BoardProtocol b) { copyFrom((CarnacBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(CarnacBoard from_b)
    {	super.copyFrom(from_b);
        
        poolSize = from_b.poolSize;
        emptySize = from_b.emptySize;
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        AR.copy(nDolmonds,from_b.nDolmonds);
        robotDepth = from_b.robotDepth;
        pickedObject = from_b.pickedObject;	
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(placedMenhir,from_b.placedMenhir);
        lastPlaced = getCell(from_b.lastPlaced);
        lastTipped = getCell(from_b.lastTipped);
        lastPlacedChip = from_b.lastPlacedChip;
        pickedLastPlaced = getCell(from_b.pickedLastPlaced);
        pickedLastTipped = getCell(from_b.pickedLastTipped);
        pickedLastChip = lastPlacedChip;
        nDolmondsValid = false;
        sameboard(from_b);
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long rv)
    {	randomKey = rv;
     	variation game = rules = variation.find(gtype);
     	if(game!=null) 
     		{ 
     		boardColumns=game.columns; 
     		boardRows = game.rows;
     		}
     	else { throw G.Error(WrongInitError,gtype); }
     	int map[]=getColorMap();
     	playerColor[map[0]] = FaceColor.Red;
     	playerColor[map[1]] = FaceColor.White;
     	gametype = gtype;
 	    setState(CarnacState.PUZZLE_STATE);
	    reInitBoard(boardColumns,boardRows); //this sets up the board and cross links

	    poolSize = N_STARTING_MENHIR;
	    emptySize = boardColumns*boardRows;
	    nDolmondsValid = false;
	    AR.setValue(nDolmonds,0);
	    whoseTurn = FIRST_PLAYER_INDEX;
		pickedSourceStack.clear();
		droppedDestStack.clear();
		placedMenhir.clear();
		pickedObject = null;
		lastPlaced = null;
		lastTipped = null;
		lastPlacedChip = null;
		pickedLastPlaced = null;
		pickedLastTipped = null;
		pickedLastChip = null;
		robotDepth = 0;
		robotState.clear();
		robotStack.clear();
		robotChipStack.clear();
		AR.setValue(win,false);
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
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
        case CONFIRM_TIP_STATE:
        case PUZZLE_STATE:
            break;
        case CONFIRM_STATE:
        case TIP_OR_PASS_STATE:
        case CONFIRM_PASS_STATE:
        case RESIGN_STATE:
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
        {case RESIGN_STATE:
         case CONFIRM_STATE:
         case CONFIRM_TIP_STATE:
         case CONFIRM_PASS_STATE:
            return (true);

        default:
            return (false);
        }
    }


    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(CarnacState.GAMEOVER_STATE);
    }
    
    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	if(board_state==CarnacState.GAMEOVER_STATE) { return(win[player]); }
     	return(false);
    }
    // estimate the value of the board position.
    public double ScoreForPlayer(int player,boolean print)
    {  	
    	throw G.Error("not implemented");
    }


    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement()
    {	
        pickedObject = null;
        droppedDestStack.clear();
        pickedSourceStack.clear();
     }
    private CarnacChip removeChip(CarnacCell c)
    {
		CarnacChip po = c.removeTop(); 
		G.Assert(placedMenhir.remove(c,false)!=null,"removed it");
		switch(po.getFaceOrientation())
		{
			case Horizontal:
				c.exitTo(CELL_RIGHT).removeChip(po);
				emptySize += 2;
				break;
			case Vertical:
				c.exitTo(CELL_UP).removeChip(po);
				emptySize += 2;
				break;
			case Up:
				emptySize += 1;
				break;
			default: G.Error("Not expected");
		}
		return(po);
    }
    private void addChip(CarnacCell c,CarnacChip ch)
    {
    	c.addChip(ch); 
    	placedMenhir.push(c);
    	switch(ch.getFaceOrientation())
    	{
    	case Horizontal:
    		c.exitTo(CELL_RIGHT).addChip(ch);
    		emptySize -= 2;
    		break;
    	case Vertical:
    		c.exitTo(CELL_UP).addChip(ch);
    		emptySize -= 2;
    		break;
    	case Up:
    		emptySize -= 1;
    		break;
		default: G.Error("Not expected");
    	}

    }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    G.Assert(pickedObject==null, "nothing should be moving");
    if(droppedDestStack.size()>0)
    	{
    	CarnacCell dr = droppedDestStack.pop();
    	switch(dr.rackLocation())
	    	{
	   		default: throw G.Error("Not expecting rackLocation %s",dr.rackLocation);
			case BoardLocation: 
				pickedObject = removeChip(dr);
				lastPlaced = null;
				lastTipped = null;
				lastPlacedChip = null;
				nDolmondsValid = false;
				break;
			case Chip_Pool:	
				pickedObject = dr.topChip();
				poolSize--;
				break;	// don't add back to the pool
	    	
	    	}
	    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	CarnacChip po = pickedObject;
    	if(po!=null)
    	{	
    		CarnacCell ps = pickedSourceStack.pop();
    		switch(ps.rackLocation())
    		{
    		default: throw G.Error("Not expecting rackLocation %s",ps.rackLocation);
    		case BoardLocation:
    			addChip(ps,po);
	    		lastPlacedChip = pickedLastChip;
	    		lastPlaced = pickedLastPlaced;
	    		lastTipped = pickedLastTipped;
	    		pickedLastChip = null;
	    		pickedLastPlaced = null;
	    		pickedLastTipped = null;
	    		nDolmondsValid = false;
    			break;
     		case Chip_Pool:	
     			poolSize ++;
     			break;	// don't add back to the pool
    		}
    		pickedObject = null;
     	}
     }

	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private CarnacChip pickObject(CarnacCell c)
    {	G.Assert(pickedObject==null,"pickedObject should be null");
    	switch(c.rackLocation())
    	{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case BoardLocation: 
			pickedObject = removeChip(c);
			pickedLastPlaced = lastPlaced;
			pickedLastTipped = lastTipped;
			pickedLastChip = lastPlacedChip;
			lastPlaced = null;
			lastTipped = null;
			lastPlacedChip = null;
			nDolmondsValid = false;
			break;
		case Chip_Pool:	
			pickedObject = c.topChip();
			poolSize--;
			break;	// don't add back to the pool
    	
    	}
    	pickedSourceStack.push(c);
    	return(pickedObject);
   }
    // 
    // drop the floating object.
    //
    private void dropObject(CarnacCell c)
    {   G.Assert(pickedObject!=null,"pickedObject should not be null"); 	    		
    	switch(c.rackLocation())
		{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case BoardLocation: 
			addChip(c,pickedObject);
	    	lastPlaced = c;
	    	lastTipped = null;
	    	lastPlacedChip = pickedObject;
	    	pickedObject = null;
	    	nDolmondsValid = false;
			break;
		case Chip_Pool:	
			poolSize ++;
			break;	// don't add back to the pool
		}
       	droppedDestStack.push(c);
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(CarnacCell cell)
    {	return((droppedDestStack.size()>0) && (droppedDestStack.top()==cell));
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	CarnacChip ch = pickedObject;
    	if(ch!=null)
    		{ return(ch.chipNumber());
    		}
        return (NothingMoving);
    }
    
    public CarnacCell getCell(CarnacId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	return(getCell(col,row));
        case Chip_Pool:
       		return(pool[row]);
        }
    }
    public CarnacCell getCell(CarnacCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }


    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case CONFIRM_STATE:
        	setNextStateAfterDone(); 
        	break;
        case PLAY_STATE:
			setState(CarnacState.CONFIRM_STATE);
			break;

        case PUZZLE_STATE:
			acceptPlacement();
            break;
        }
    }
    //	
    //true if col,row is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(CarnacCell c)
    {	return((pickedSourceStack.size()>0) && (pickedSourceStack.top()==c));
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterPick()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting pick in state %s", board_state);
        case CONFIRM_STATE:
        	setState((lastPlaced==null)?CarnacState.TIP_OR_PASS_STATE:CarnacState.PLAY_STATE);
        	break;
        case PLAY_STATE:
			break;
        case PUZZLE_STATE:
            break;
        }
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case CONFIRM_PASS_STATE:
    	case CONFIRM_TIP_STATE:
    		lastPlaced = null;
    		lastTipped = null;
    		lastPlacedChip = null;
    		if((poolSize==0) || (emptySize==0))
    		{
    			setGameOver();
    		}
    		else
    		{	lastPlacedChip = null;
    			lastPlaced = lastTipped = null;
    			setState(CarnacState.PLAY_STATE);
    		}
    		break;
    	case GAMEOVER_STATE: 
    		break;

     	case CONFIRM_STATE:
    	case PUZZLE_STATE:
    	case PLAY_STATE:
    		boolean notip = (lastPlaced==null)
    								||(lastTipped!=null)
    								||(lastPlacedChip.getFaceOrientation()!=FaceOrientation.Up)
    								||!canBeTipped(lastPlaced);
    		if(notip) { lastPlacedChip = null; ; lastPlaced = lastTipped = null; }
    		if(notip && (poolSize==0) || (emptySize==0)) { setGameOver(); }
    		else	setState(notip	?CarnacState.PLAY_STATE	:CarnacState.TIP_OR_PASS_STATE);
    		break;
    	}
    }
   

    
    private void doDone()
    {	
        acceptPlacement();

        if (board_state==CarnacState.RESIGN_STATE)
        {	setGameOver(false,true);
        }
        else
        {	boolean win1 = WinForPlayerNow(whoseTurn);
        	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
        	if(win1 || win2)  { setGameOver(win1,win2); }
        	else {setNextPlayer(); setNextStateAfterDone(); }
        }
    }

    private void doUntip()
    {
    	CarnacCell lp = lastPlaced;
    	CarnacChip ch = lastPlacedChip;
    	removeChip(lastTipped);
    	addChip(lp,ch);
		lastTipped = null;
		nDolmondsValid = false;
    	setState(CarnacState.TIP_OR_PASS_STATE);
    }
	public CarnacChip getTippedChip(CarnacChip chip,int dir)
	{	CarnacChip tipped[] = chip.tipped;
		CarnacChip next = tipped[dir];
		if(next==null)
		{
			switch(dir)
			{
			default: throw G.Error("Not expecting direction %s",dir);
			case CELL_LEFT:
				next = tipped[CELL_LEFT] = 
					CarnacChip.findChip(FaceOrientation.Horizontal,chip.sideColor,chip.topColor.oppositeColor(),chip.frontColor);
				break;
			case CELL_RIGHT:
				next = tipped[CELL_RIGHT] = 
						CarnacChip.findChip(FaceOrientation.Horizontal,chip.sideColor,chip.topColor,chip.frontColor);
				break;
			case CELL_UP:
				next = tipped[CELL_UP] =
						CarnacChip.findChip(FaceOrientation.Vertical, chip.frontColor,chip.sideColor, chip.topColor.oppositeColor());
				break;
			case CELL_DOWN:
				next = tipped[CELL_DOWN] =
						CarnacChip.findChip(FaceOrientation.Vertical,chip.frontColor,chip.sideColor,chip.topColor);
				break;
			}
		}
		return(next);
	}
    public boolean Execute(commonMove mm,replayMode replay)
    {	CarnacMovespec m = (CarnacMovespec)mm;

        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone();

            break;
        case MOVE_RACK_BOARD:
           	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case PLAY_STATE:
        			CarnacCell src = getCell(CarnacId.Chip_Pool, m.from_col, m.from_row);
                    CarnacCell dest = getCell(CarnacId.BoardLocation,m.to_col,m.to_row);
                    pickObject(src);
                    dropObject(dest); 
                    setNextStateAfterDrop();
                    break;
        	}
        	break;
        case MOVE_TIP:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting state %s",board_state);
        		case TIP_OR_PASS_STATE:
        		case CONFIRM_PASS_STATE:
        		case PUZZLE_STATE:
        			G.Assert(pickedObject==null,"something is moving");
        			G.Assert(lastPlaced!=null,"something placed");
        			CarnacCell fromCell = getCell(CarnacId.BoardLocation, m.from_col, m.from_row);
        			CarnacChip ch = removeChip(fromCell);
        			int dir = findDirection(m.from_col,m.from_row,m.to_col,m.to_row);
        			CarnacChip replacement = getTippedChip(ch,dir);		// get the replacement chip
        			
        			// since tipped cells occupy 2 spaces, we need to place at +1 or -2 from the picked cell
        			switch(replacement.getFaceOrientation())
        			{
        			default: throw G.Error("Not expected");
        			case Up: throw G.Error("replacement should not be up");
        			case Vertical:
        				{
        				CarnacCell drop = fromCell;
        				switch(dir)
        				{
        				default: throw G.Error("Not expecting direction");
        				case CELL_DOWN:
        					drop = fromCell.exitTo(CELL_DOWN).exitTo(CELL_DOWN);
        					break;
        				case CELL_UP:
        					drop = fromCell.exitTo(CELL_UP);
        				}
        				addChip(drop,replacement);
        				lastTipped = drop;
        				}
        				break;
        			case Horizontal:
       				{
        				CarnacCell drop = fromCell;
        				switch(dir)
        				{
        				default: throw G.Error("Not expecting direction");
        				case CELL_LEFT:
        					drop = fromCell.exitTo(CELL_LEFT).exitTo(CELL_LEFT);
        					break;
        				case CELL_RIGHT:
        					drop = fromCell.exitTo(CELL_RIGHT);
        				}
        				addChip(drop,replacement);
        				lastTipped = drop;
        				}
       					break;
        			}
  				    if(board_state==CarnacState.TIP_OR_PASS_STATE) 
  				    	{ setState(CarnacState.CONFIRM_TIP_STATE); 
  				    	} 
  				    	else if(board_state==CarnacState.PUZZLE_STATE)
  				    	{ lastTipped = null; 
  				    	  lastPlaced = null;
  				    	  lastPlacedChip = null;
  				    	}
  				    nDolmondsValid = false;
        			break;
        	}
        	break;
        case MOVE_UNTIP:
        	doUntip();
        	break;
        case MOVE_DROPB:
			{
			CarnacCell c = getCell(CarnacId.BoardLocation, m.to_col, m.to_row);
        	G.Assert(pickedObject!=null,"something is moving");
			
            if(isSource(c)) 
            	{ 
            	  unPickObject(); 

            	} 
            	else
            		{
            		dropObject(c);
            		setNextStateAfterDrop();
            		}
			}
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	if(isDest(getCell(m.from_col,m.from_row)))
        		{ unDropObject(); 
        		  setState(CarnacState.PLAY_STATE);
        		}
        	else 
        		{ pickObject(getCell(CarnacId.BoardLocation, m.from_col, m.from_row));
        			// if you pick up a gobblet and expose a row of 4, you lose immediately
        		  switch(board_state)
        		  {	default: throw G.Error("Not expecting pickb in state %s",board_state);
        		  	case PLAY_STATE:
        		  		// if we pick a piece off the board, we might expose a win for the other player
        		  		// and otherwise, we are comitted to moving the piece
         		  		break;
        		  	case PUZZLE_STATE:
        		  		break;
        		  }
         		}
 
            break;

        case MOVE_DROP: // drop on chip pool;
            dropObject(getCell(m.source, m.to_col, m.to_row));
            setNextStateAfterDrop();

            break;

        case MOVE_PICK:
        	{
        	CarnacCell c = getCell(m.source, m.from_col, m.from_row);
            pickObject(c);
            setNextStateAfterPick();
        	}
            break;


        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(CarnacState.PUZZLE_STATE);
            {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;

        case MOVE_RESIGN:
            setState(unresign==null?CarnacState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            // standardize "gameover" is not true
            lastPlaced = lastTipped = null;
 			lastPlacedChip = null;
            setState(CarnacState.PUZZLE_STATE);
 
            break;
        case MOVE_PASS:
        	setState((board_state==CarnacState.CONFIRM_PASS_STATE) ? CarnacState.TIP_OR_PASS_STATE : CarnacState.CONFIRM_PASS_STATE);
        	break;
        case MOVE_GAMEOVERONTIME:
        	setGameOver(true,false);
        	break;
        default:
        	cantExecute(m);
        }


        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
         case PLAY_STATE: 
         case PUZZLE_STATE:
        	return(pickedObject==null?(poolSize>0) : true);

         case CONFIRM_STATE:
         case CONFIRM_TIP_STATE:
         case TIP_OR_PASS_STATE:
         case CONFIRM_PASS_STATE:
         case RESIGN_STATE:
         case GAMEOVER_STATE:
			return(false);
        }
    }
  
    public boolean canTipInDirection(CarnacCell c, int dir)
    {
    	CarnacCell t1 = c.exitTo(dir);
    	if(t1==null || (t1.topChip()!=null)) { return(false); }
    	CarnacCell t2 = t1.exitTo(dir);
    	if((t2==null) || (t2.topChip()!=null)) { return(false); }
    	return(true);
    }
    public boolean canBeTipped(CarnacCell c)
    {
    	for(int dir=0;dir<CELL_FULL_TURN; dir++)
    	{
    		if(canTipInDirection(c,dir))
    		{
    			return(true);
    		}
    	}
    	return(false);
    }
    public boolean LegalToHitBoard(CarnacCell cell)
    {	
        switch (board_state)
        {
 		case PLAY_STATE:
 			if(cell.canTip) { return(true); }
 			else if(pickedObject==null) { return(isDest(cell)); }
 			else { return(cell.topChip()==null); }
 		case CONFIRM_TIP_STATE:
 			if(cell==lastTipped) { return(true); }
 			if(lastTipped!=null)
 			{	CarnacChip top = lastTipped.topChip();
 				if(top!=null)
 				{
 					switch(top.getFaceOrientation())
 					{
 					case Up: break;
 					case Horizontal: return(cell==lastTipped.exitTo(CELL_RIGHT));
 					case Vertical: return(cell==lastTipped.exitTo(CELL_UP)); 
					default: G.Error("not expected");
 					}
 				}
 			}
			return(false);
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
			return(isDest(cell));
		case TIP_OR_PASS_STATE:
		case CONFIRM_PASS_STATE:
			return(cell.canTip);
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case PUZZLE_STATE:
        	if(pickedObject!=null)
        	{
        	if(cell.topChip()!=null) { return(false); }
        	CarnacCell next = null;
        	switch(pickedObject.getFaceOrientation())
        	{
        	default: return(true);
        	case Vertical: next = cell.exitTo(CELL_UP); break;
        	case Horizontal: next = cell.exitTo(CELL_RIGHT); break;
        	}
        	return((next!=null) && (next.topChip()==null));
        	}
        	else
        	{
        	if(cell.canTip) { return(true); }
        	return(cell.topChip()!=null);
        	}
        }
    }
  public boolean canDropOn(CarnacCell cell)
  {		CarnacCell top = (pickedObject!=null) ? pickedSourceStack.top() : null;
  		return((pickedObject!=null)				// something moving
  			&&(top.onBoard 			// on the main board
  					? (cell!=top)	// dropping on the board, must be to a different cell 
  					: (cell==top))	// dropping in the rack, must be to the same cell
  				);
  }
 
  private CellStack robotStack = new CellStack();
  private StateStack robotState = new StateStack();
  private ChipStack robotChipStack = new ChipStack();
  public int robotDepth = 0;
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(CarnacMovespec m)
    {	robotStack.push(lastPlaced);
    	robotStack.push(lastTipped);
    	robotDepth++;
    	robotChipStack.push(lastPlacedChip);
    	robotState.push(board_state);//record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
       // G.print("R "+m);
        if (Execute(m,replayMode.Replay))
        {	robotStack.push(lastTipped);
        	robotStack.push(lastPlaced);
			robotChipStack.push(lastPlacedChip);
            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {	
                doDone();
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
    public void UnExecute(CarnacMovespec m)
    {	
       // G.print("U "+m+" for "+whoseTurn);
    	robotDepth--;
        lastPlaced = robotStack.pop();
   		lastTipped = robotStack.pop();
   		lastPlacedChip = robotChipStack.pop();
 
        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;
   	    case MOVE_PASS:
        case MOVE_DONE:
            break;
        case MOVE_RACK_BOARD:
   			G.Assert(pickedObject==null,"something is moving");
   			pickObject(getCell(CarnacId.BoardLocation,m.to_col,m.to_row));
   			dropObject(getCell(CarnacId.Chip_Pool,m.from_col, m.from_row));
		    acceptPlacement();
            break;
        case MOVE_TIP:
         	doUntip();
         	break;
        case MOVE_RESIGN:
            break;
        }
        setState(robotState.pop());
   		lastTipped = robotStack.pop();
   		lastPlacedChip = robotChipStack.pop();
        lastPlaced = robotStack.pop();
         if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }

    }
 commonMove get_random_onboard_move(Random rand)
 {	
 	cell<CarnacCell> cells[] = getCellArray();
 	int sz = cells.length;
 	CarnacCell piece = pool[ Random.nextSmallInt(rand,pool.length)];
 	while (sz>0)
 	{
 	int idx = Random.nextInt(rand,sz--);
 	CarnacCell dest = (CarnacCell)cells[idx];
 	cells[idx] = cells[sz];
 	cells[sz] = dest;
 	if(dest.topChip()==null)
 	{	return(new CarnacMovespec(MOVE_RACK_BOARD,piece.col,piece.row,dest.col,dest.row,whoseTurn));
 	}}
 	throw G.Error("shouldn't run out of board");
 }
 
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	switch(board_state)
 	{
 	default:
 	case PUZZLE_STATE:
 	case GAMEOVER_STATE:
 		throw G.Error("Not expecting state");
	case CONFIRM_PASS_STATE:
 	case RESIGN_STATE:
	case CONFIRM_STATE:
	case CONFIRM_TIP_STATE:
		all.push(new CarnacMovespec(MOVE_DONE,whoseTurn));
		break;
 	case TIP_OR_PASS_STATE:
 		all.push(new CarnacMovespec(MOVE_PASS,whoseTurn));
 		for(int dir=0; dir<CELL_FULL_TURN; dir++)
 		{
 			if(canTipInDirection(lastPlaced,dir))
 			{
 				CarnacCell dest = lastPlaced.exitTo(dir);
 				all.push(new CarnacMovespec(MOVE_TIP,lastPlaced.col,lastPlaced.row,dest.col,dest.row,whoseTurn));
 			}
 		}
 		break;
 	case PLAY_STATE:
 		for(CarnacCell dest = allCells; dest!=null; dest=dest.next)
 		{	if(dest.topChip()==null)
 			{
 			for(CarnacCell piece : pool)
 			{
 				all.push(new CarnacMovespec(MOVE_RACK_BOARD,piece.col,piece.row,dest.col,dest.row,whoseTurn));
 			}}
 		}
 		
 		break;
 	}
  	return(all);
 }
 
}
