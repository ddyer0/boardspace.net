/* copyright notice */package blooms;

import java.awt.Color;

import static blooms.Bloomsmovespec.*;
import java.util.*;

import blooms.BloomsConstants.BloomsId;
import blooms.BloomsConstants.BloomsState;
import blooms.BloomsConstants.BloomsVariation;
import blooms.BloomsConstants.StateStack;
import lib.*;
import lib.Random;
import online.game.*;

/**
 * 
 * BloomdBoard knows all about the game of Hex, which is played
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

class BloomsBoard extends hexBoard<BloomsCell> implements BoardProtocol
{	static int REVISION = 101;			// 100 represents the initial version of the game
										// 101 adds the endgame condition selection
	public int getMaxRevisionLevel() { return(REVISION); }
    static final String[] BloomsGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	
	boolean allowSelfCapture = false;
	BloomsVariation variation = BloomsVariation.blooms_4;
	BloomsState board_state = BloomsState.Puzzle;	
	private BloomsState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	private IStack robotCapture = new IStack();
	private CellStack robotPlayed = new CellStack();
	private CellStack captureStack = new CellStack();
	private ChipStack chipStack = new ChipStack();
	private int captured[] = new int[2];
	public int lastPlacement = 0;
	EndgameCondition endgameCondition = EndgameCondition.Territory;
	boolean endgameApproved[] = new boolean[2];
	boolean allApproved()
	{
		return endgameApproved[0]&&endgameApproved[1];
	}
	boolean endgameSelected = false;
	private int sweep_counter = 0;
	public BloomsState getState() { return(board_state); }
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(BloomsState st) 
	{ 	unresign = (st==BloomsState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
    
	public BloomsChip.ColorSet[] playerColorSet = new BloomsChip.ColorSet[2];
	public int playerIndex(BloomsId source)
	{
		switch(source)
		{
		case Red_Chip_Pool:
		case Orange_Chip_Pool: return 0;
		case Green_Chip_Pool:
		case Blue_Chip_Pool:	return 1;
		default: throw G.Error("Not expecting %s",source);
		}
	}
	
    public BloomsChip playerColors[][]=new BloomsChip[2][2];
    public BloomsCell playerCells[][]=new BloomsCell[2][2];
    public BloomsCell firstPlayedLocation = null;
    
    public BloomsChip playerColor(int n)
    {
    	return playerColors[n][0];
    }
// this is required even if it is meaningless for this game, but possibly important
// in other games.  When a draw by repetition is detected, this function is called.
// the game should have a "draw pending" state and enter it now, pending confirmation
// by the user clicking on done.   If this mechanism is triggered unexpectedly, it
// is probably because the move editing in "editHistory" is not removing last-move
// dithering by the user, or the "Digest()" method is not returning unique results
// other parts of this mechanism: the Viewer ought to have a "repRect" and call
// DrawRepRect to warn the user that repetitions have been seen.
	public void SetDrawState() { setState(BloomsState.Draw); };	
	CellStack animationStack = new CellStack();
    int chips_on_board = 0;			// number of chips currently on the board
    private int fullBoard = 0;				// the number of cells in the board
    public int chips[] = new int[2];
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public BloomsChip pickedObject = null;
    public BloomsChip lastPicked = null;
    private BloomsCell redChipPool = null;	// dummy source for the chip pools
    private BloomsCell blueChipPool = null;
    private BloomsCell orangeChipPool = null;	// dummy source for the chip pools
    private BloomsCell greenChipPool = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    public int score[] = new int[2];

    public BloomsChip otherColor()
    {
    	if(firstPlayedLocation!=null) { return(firstPlayedLocation.topChip().otherColor());  }
    	return(null);
    }
    private CellStack emptyCells=new CellStack();
    public BloomsChip lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public BloomsCell newcell(char c,int r)
	{	return(new BloomsCell(BloomsId.BoardLocation,c,r));
	}
	
	// constructor 
    public BloomsBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = BloomsGRIDSTYLE;
        
 		Random r = new Random(734687);	// this random is used to assign hash values to cells, common to all games of this type.
 		redChipPool = new BloomsCell(r,BloomsId.Red_Chip_Pool);
 		blueChipPool = new BloomsCell(r,BloomsId.Blue_Chip_Pool);
 		orangeChipPool = new BloomsCell(r,BloomsId.Orange_Chip_Pool);
 		greenChipPool = new BloomsCell(r,BloomsId.Green_Chip_Pool);
	    redChipPool.addChip(BloomsChip.Red);
	    blueChipPool.addChip(BloomsChip.Blue);
	    orangeChipPool.addChip(BloomsChip.Orange);  
	    greenChipPool.addChip(BloomsChip.Green);

        doInit(init,key,players,rev); // do the initialization
        setColorMap(map, players);
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
		setState(BloomsState.Puzzle);
		variation = BloomsVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		gametype = gtype;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case blooms_4:
		case blooms_5:
		case blooms_6:
		case blooms_7:
			reInitBoard(variation.firstInCol,variation.ZinCol,null);
		}
 		
	    int map[] = getColorMap();
	    playerColorSet[map[FIRST_PLAYER_INDEX]] = BloomsChip.ColorSet.RedOrange;
	    playerColorSet[map[SECOND_PLAYER_INDEX]] = BloomsChip.ColorSet.BlueGreen;
	    
	    playerCells[map[FIRST_PLAYER_INDEX]][0]= redChipPool;
	    playerColors[map[FIRST_PLAYER_INDEX]][0]=BloomsChip.Red;
	    
	    playerCells[map[SECOND_PLAYER_INDEX]][0]=blueChipPool;
	    playerColors[map[SECOND_PLAYER_INDEX]][0] = BloomsChip.Blue; 
	    
	    playerCells[map[FIRST_PLAYER_INDEX]][1]=orangeChipPool;
	    playerColors[map[FIRST_PLAYER_INDEX]][1] = BloomsChip.Orange;	 
	    
	    playerCells[map[SECOND_PLAYER_INDEX]][1]=greenChipPool;
	    playerColors[map[SECOND_PLAYER_INDEX]][1] = BloomsChip.Green; 
	   
	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    chips_on_board = 0;
	    AR.setValue(chips, 0);
	    droppedDestStack.clear();
	    pickedSourceStack.clear();
	    stateStack.clear();
    	captureStack.clear();
    	chipStack.clear();
    	AR.setValue(captured,0);
	    
	    pickedObject = null;
	    lastDroppedObject = null;
	    firstPlayedLocation = null;

	    // set the initial contents of the board to all empty cells
		emptyCells.clear();
		for(BloomsCell c = allCells; c!=null; c=c.next) { c.reInit(); emptyCells.push(c); }
		fullBoard = emptyCells.size();
		AR.setValue(score, 0);
        animationStack.clear();
        moveNumber = 1;
        endgameCondition = EndgameCondition.Territory;
        AR.setValue(endgameApproved,false);
        endgameSelected = revision==100;
        lastPlacement = 0;

        // note that firstPlayer is NOT initialized here
    }


    /** create a copy of this board */
    public BloomsBoard cloneBoard() 
	{ BloomsBoard dup = new BloomsBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((BloomsBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(BloomsBoard from_b)
    {
        super.copyFrom(from_b);
        chips_on_board = from_b.chips_on_board;
        AR.copy(chips,from_b.chips);
        fullBoard = from_b.fullBoard;
        robotState.copyFrom(from_b.robotState);
        getCell(emptyCells,from_b.emptyCells);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(captureStack,from_b.captureStack);
        copyFrom(playerCells,from_b.playerCells);
        chipStack.copyFrom(from_b.chipStack);
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        lastPicked = from_b.lastPicked;
        firstPlayedLocation = getCell(from_b.firstPlayedLocation);
        endgameCondition = from_b.endgameCondition;
        endgameSelected = from_b.endgameSelected;
        AR.copy(endgameApproved,from_b.endgameApproved);
        AR.copy(captured,from_b.captured);
        lastPlacement = from_b.lastPlacement;
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((BloomsBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(BloomsBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(chips_on_board == from_b.chips_on_board,"chips_on_board mismatch");
        G.Assert(AR.sameArrayContents(chips, from_b.chips), "chips mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedsourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(sameCells(captureStack,from_b.captureStack),"captureStack mismatch");
        G.Assert(sameContents(chipStack,from_b.chipStack),"chipStack mismatch");
        G.Assert(AR.sameArrayContents(captured,from_b.captured),"captured mismatch");
        G.Assert(sameCells(firstPlayedLocation,from_b.firstPlayedLocation),"playedLocation mismatch");
        
        G.Assert(endgameCondition == from_b.endgameCondition,"endgame condition mismatch");
        G.Assert(endgameSelected == from_b.endgameSelected,"endgame selected mismatch");
        G.Assert(AR.sameArrayContents(endgameApproved,from_b.endgameApproved),"endgame approved mismatch");
        
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
		v ^= Digest(r,revision);
		//v ^= Digest(r,captured);	// captured not included in the digest,	because it's not part of the official game state
		v ^= Digest(r,firstPlayedLocation);
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
        v ^= Digest(r,endgameCondition.ordinal());
        v ^= Digest(r,endgameSelected);
        v ^= Digest(r,endgameApproved);

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
        case PlayLast:
        case PlayFirst:
        case Confirm:
        case Play1:
        case Play1Capture:
        case Resign:
        case Draw:
            moveNumber++; //the move is complete in these states
            setWhoseTurn(whoseTurn^1);
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
    
    public int scoreForPlayer(int p)
    {	int n=0;
    	sweep_counter++;
    	for(BloomsCell c = allCells; c!=null; c=c.next)
    	{
    		if(c.getSweep()!=sweep_counter)
    		{	
    			BloomsChip top = c.topChip();
    			if(top==null) 
    				{ n += c.sizeOfTerritory(playerColorSet[p],sweep_counter); 
    				}
    			else if(top.colorSet==playerColorSet[p]) { n++; }
    		}
    	}
    	return(n);
    }
    public int capturesForPlayer(int p)
    {
    	return captured[p];
    }
    public double estScoreForPlayer(int p)
    {	double n=0;
    	sweep_counter++;
    	for(BloomsCell c = allCells; c!=null; c=c.next)
    	{
    		if(c.getSweep()!=sweep_counter)
    		{	
    			BloomsChip top = c.topChip();
    			if(top==null) 
    				{ n += (c.sizeOfTerritory(playerColorSet[p],sweep_counter)*1.01);	// slight preference for territory over stones 
    				}
    			else if(ownerIndex(top)==p) { n++; }
    		}
    	}
    	return(n);
    }

    private int ownerIndex(BloomsChip ch)
    {
    	return((ch.colorSet==playerColorSet[0])?0:1);
    }
    // set the contents of a cell, and maintain the books
    public BloomsChip SetBoard(BloomsCell c,BloomsChip ch)
    {	BloomsChip old = c.chip;
    	if(c.onBoard)
    	{
    	if(old!=null) { chips_on_board--;emptyCells.push(c); chips[ownerIndex(old)]--; }
     	if(ch!=null) { chips_on_board++; emptyCells.remove(c,false); chips[ownerIndex(ch)]++; }
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
    int previousLastPlacement = -1;
    private BloomsCell unDropObject()
    {	BloomsCell rv = droppedDestStack.pop();
    	setState(stateStack.pop());
    	pickedObject = SetBoard(rv,null); 	// SetBoard does ancillary bookkeeping
    	firstPlayedLocation = droppedDestStack.top();;
    	rv.lastPlaced = previousLastPlacement;
    	lastPlacement--;
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	BloomsCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	SetBoard(rv,pickedObject);
     	pickedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(BloomsCell c)
    {
       droppedDestStack.push(c);
       stateStack.push(board_state);
       
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("not expecting dest %s", c.rackLocation);
        case Red_Chip_Pool:
        case Orange_Chip_Pool:
        case Blue_Chip_Pool:
        case Green_Chip_Pool:		// back in the pool, we don't really care where
        	pickedObject = null;
            break;
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        case EmptyBoard:
           	SetBoard(c,pickedObject);
            lastDroppedObject = pickedObject;
            firstPlayedLocation = c;
            previousLastPlacement = c.lastPlaced;
            c.lastPlaced = lastPlacement++;
            pickedObject = null;
            break;
        }
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(BloomsCell c)
    {	return(droppedDestStack.top()==c);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { BloomsChip ch = pickedObject;
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
    private BloomsCell getCell(BloomsId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("not expecting source %s", source);
        case BoardLocation:
        	return(getCell(col,row));
        case Red_Chip_Pool:
        	return(redChipPool);
        case Green_Chip_Pool:
        	return(greenChipPool);
        case Blue_Chip_Pool:
        	return(blueChipPool);
        case Orange_Chip_Pool:
        	return(orangeChipPool);
        } 	
    }
    public BloomsCell getCell(BloomsCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
    public BloomsCell getCell(BloomsId v)
    {
    	switch(v)
    	{
    	default: throw G.Error("Not expecting source %s",v);
    	case Blue_Chip_Pool: return(blueChipPool);
    	case Red_Chip_Pool: return(redChipPool);
    	case Green_Chip_Pool: return(greenChipPool);
    	case Orange_Chip_Pool: return(orangeChipPool);
    	}
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(BloomsCell c)
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

        case Red_Chip_Pool:
        case Green_Chip_Pool:
        case Blue_Chip_Pool:
        case Orange_Chip_Pool:
        	lastPicked = pickedObject = c.topChip();
        }
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(BloomsCell c)
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
        case PlayLast:
			setState(lastPlayedHasLiberties(firstPlayedLocation) 
						? BloomsState.Play1 
						: BloomsState.Play1Capture);
			break;
        case Play1:
        case Play1Capture:
        case PlayFirst:
        	setState(BloomsState.Confirm);
        	break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    // after a free play, determine if there is a self-atari left on
    // the board, in which case it must be resolved on the second move
    private boolean lastPlayedHasLiberties(BloomsCell c)
    {	
     	int sweep = ++sweep_counter;
     	BloomsChip top = c.topChip();
    	int pl = ownerIndex(top);
    	boolean hasLibs = c.sweepHasLiberties(top,sweep);
    	StackIterator<BloomsCell>friendsInNeed = null;
      	for(int dir=c.geometry.n; dir>0; dir--)
     	{
     		BloomsCell adj = c.exitTo(dir);
     		if((adj!=null) && (adj.getSweep()!=sweep))
     		{	BloomsChip atop = adj.topChip();
     			if((atop!=null) && (ownerIndex(atop)==pl))
     			{
     				boolean has = adj.sweepHasLiberties(atop, sweep);
     				if(!has) 
     					{ if(friendsInNeed==null)
     						{ friendsInNeed=adj; } 
     						else { friendsInNeed=friendsInNeed.push(adj);}
     					hasLibs = false;
     					}
     			}   			
     		}
     	}
     	if(hasLibs) { return(true); }
     	// here, some group has become atari, so we need to check
     	// if all such groups are freed by captures or not
     	int captureTag = ++sweep_counter;
     	// if enemy groups have no liberties, mark them with captureTag
     	boolean hasCap = c.sweepHasCaptures(top,sweep,friendsInNeed==null?-1:captureTag);
     	if(hasCap)
     	{
     	// the central group captures something, but other friendly groups
     	// may be atari, and several enemy groups may be captured
     	if(friendsInNeed!=null)
     	{
     		for(int lim=friendsInNeed.size()-1; lim>=0; lim--)
     		{
     			BloomsCell adj = friendsInNeed.elementAt(lim);
     			// has to be adjacent to some of the captured stones
     			if(!adj.sweepIsAdjacent(++sweep_counter,captureTag)) { return(false); }
     		}
     	}}
     	return(hasCap);
    }
    
    private void setNextStateAfterDone(replayMode replay)
    {	G.Assert(chips_on_board+emptyCells.size()==fullBoard,"cells missing");
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state %s",board_state);
    	case PlayLast:
    		setState(BloomsState.Gameover);
    		score[0] = scoreForPlayer(0);
    		score[1] = scoreForPlayer(1);
    		int next = whoseTurn^1;
    		win[whoseTurn] = score[whoseTurn]>=score[next];
    		win[next] = score[next]>score[whoseTurn];
    		break;
    	case Gameover: break;
    	case Play:
    		setState(BloomsState.PlayLast);
    		break;
    	case Confirm:
    	case Play1:
    	case Play1Capture:
    	case PlayFirst:
    	case Puzzle:
    	case SelectEnd:
    		if(endgameSelected)
    		{
    		setState( (chips_on_board==0) ? BloomsState.PlayFirst : BloomsState.Play);
    		if(endgameCondition.ncaptured>0)
    			{
    			int cap = endgameCondition.ncaptured;
    			if(captured[whoseTurn]>=cap) { win[1^whoseTurn] = true; setState(BloomsState.Gameover); break;}
    			else if(captured[whoseTurn^1]>=cap) { win[whoseTurn]=true; setState(BloomsState.Gameover); break; }
    			}
    		}
    		else
    		{
    			setState(BloomsState.SelectEnd);
    		}
    		break;
    	}
    }
    
    private void killGroup(BloomsCell seed,replayMode replay)
    {
    	BloomsChip top = seed.removeTop();
    	captureStack.push(seed);
    	chipStack.push(top);
    	emptyCells.push(seed);
    	chips_on_board--;
    	int owner = ownerIndex(top);
    	chips[owner]--;
    	captured[owner]++;
    	if(replay!=replayMode.Replay){
    		animationStack.push(seed);
    		animationStack.push(getCell(top.id));
    	}
    	for(int dir = seed.geometry.n; dir>0; dir--)
    	{
    		BloomsCell adj = seed.exitTo(dir);
    		if(adj!=null)
    		{
    			if(adj.topChip()==top)
    			{
    				killGroup(adj,replay);
    			}
    		}
    	}
    }
    private StackIterator<BloomsCell> doCaptureSweep(StackIterator<BloomsCell> it,BloomsCell c,BloomsChip top,int sweep)
    {	if(c.getSweep()!=sweep)
    	{
		int libs = c.sweepCountLiberties(top,sweep,++sweep_counter);
		if(libs==0) 
			{ if(it==null) { it = c; }
				else { it = it.push(c); } 
			}
    	}
		return(it);
    }

    // true if playing top at c is not self capture or can capture something
    // this is very complicated if it is the first move of the pair, and
    // the second move of the pair would be required to complete a capture
    synchronized boolean sweepCanCapture(BloomsCell c,BloomsChip top,BloomsCell first,boolean mustCapture)
    {	
    	int borders = 0;
    	int friends = 0;
     	int n = c.geometry.n;
     	
     	{
     	// first a quick check that there's no possible problem, but
     	// this is also where we reject filling eyes
    	boolean hasFriends = false;
    	boolean hasDirectLiberties = false;
    	for(int dir = n; dir>0; dir--)
    	{
    		BloomsCell adj = c.exitTo(dir);
    		if(adj==null) { borders++; }
    		else 
    		{	BloomsChip adjTop = adj.topChip();
    			if(adjTop == null) { hasDirectLiberties = true; }		
    			else if(adjTop==top) { friends++; }
    			else if(adjTop.colorSet==top.colorSet) 
    						{ 
    						  hasFriends = true;
    						} 
    		}}
    	if(friends+borders==n) 
    		{ return(false); } // filling an eye
    	if(hasDirectLiberties && !hasFriends && !mustCapture) 
    		{ return(true); }
     	}
    	
    	StackIterator<BloomsCell> capturedFriend = null;
    	StackIterator<BloomsCell> capturedEnemy = null;
     	
    	{
    	int capturedProbe = ++sweep_counter;
     	
    	// now check the neighborhood of the actual proposed move
    	// sweep the central group for self atari
    	capturedFriend = doCaptureSweep(capturedFriend,c,top,capturedProbe);
    	// sweep adjacent groups for atari
    	for(int dir = n; dir>0; dir--)
    	{
    		BloomsCell adj = c.exitTo(dir);
    		if(adj!=null)
    		{	
    			BloomsChip adjTop = adj.topChip();
    			if(adjTop != null) 
    			{	
     				if(adjTop.colorSet==top.colorSet)
    					{	
    					    capturedFriend = doCaptureSweep(capturedFriend,adj,adjTop,capturedProbe);
    					}
    				else 
    					{
     					capturedEnemy = doCaptureSweep(capturedEnemy,adj,adjTop,capturedProbe);
 
    					}
    			}
    		}}
    	}
   
    	// if this is the second move in the pair, likewise sweep
    	// the first move and its neighbors
    	if(first!=null)
    	{	// if mustCapture, the first move required a capture
    		// assisted by the second move
    		int capturedProbe = ++sweep_counter;
       		BloomsChip firstTop = first.topChip();      		
       		capturedFriend = doCaptureSweep(capturedFriend,first,firstTop,capturedProbe);
    		
    		for(int dir = n; dir>0; dir--)
    		{
    		BloomsCell adj = first.exitTo(dir);
    		if(adj!=null)
    		{	
    			BloomsChip adjTop = adj.topChip();
    			if(adjTop != null) 
    			{	
     				if(adjTop.colorSet==firstTop.colorSet)
    					{	
     					capturedFriend = doCaptureSweep(capturedFriend,adj,adjTop,capturedProbe);
    					}
    				else 
    					{
     					capturedEnemy = doCaptureSweep(capturedEnemy,adj,adjTop,capturedProbe);   
     					}
    				}
    			}
    		}}
    
    	// if any friendly groups are atari, be sure they are (or can be) freed by capture
    	if(capturedFriend!=null)
    	{	
    	   	// mark the captured enemy groups
           	if(capturedEnemy!=null)
        	{
        	int capturedEnemyTag = ++sweep_counter;
         		for(int lim = capturedEnemy.size()-1; lim>=0; lim--)
        		{
        			capturedEnemy.elementAt(lim).sweepAndMark(capturedEnemyTag);
        		}
        	
    		boolean allok = true;
         	// all captured friends have to be adjacent to some captured enemy
        	for(int i=0;i<capturedFriend.size() && allok;i++)
        		{ BloomsCell friend = capturedFriend.elementAt(i);
        		  if(!friend.sweepIsAdjacent(++sweep_counter,capturedEnemyTag))
        		  	{ allok = false;
        		  	}
        		}
        	if(allok) 
        		{ return(true); 
        		}
        	}
           	if(!mustCapture && (first==null)) 
           		{ // recurse to check if a second is possible to fix
           		  return(canPlaceSecondAfter(c,top)); 
           		}
           	return(false);
           	}
        	
    	return(true);
    }
    private boolean canPlaceSecondAfter(BloomsCell first,BloomsChip top)
    {
       	// tricky case, some friend needs to capture something
       	try {
       		first.addChip(top);
       		BloomsChip otherColor = top.otherColor();
       		for(int lim=emptyCells.size()-1; lim>=0; lim--)
       		{
       		BloomsCell second = emptyCells.elementAt(lim);
       		if(second!=first)
       			{
       			if(sweepCanCapture(second,otherColor,first,true))
       				{
       				return(true);
       				}
       			}
       		}
       		return(false);
       	}
       	finally { first.removeTop(); }
    }
    private boolean doCaptures(int who,boolean doit,replayMode replay)
    {	
    	StackIterator<BloomsCell> killed = null;
    	boolean some = false;
    	sweep_counter++;
    	for(BloomsCell c = allCells; c!=null; c=c.next)
    	{	if(c.getSweep()!=sweep_counter)
    		{
    		BloomsChip top = c.topChip();
    		if((top!=null) && (ownerIndex(top)==who))
    			{	
    				if(!c.sweepHasLiberties(top,sweep_counter))
    				{
    					if(doit) 
    					{ if(killed==null) { killed = c; }
    						else { killed = killed.push(c); }
    					}
    					some = true;
    				}
    			}
    		}
    	}
    	if(doit && some)
    	{
    		for(int lim = killed.size()-1; lim>=0; lim--)
    		{
    			BloomsCell seed = killed.elementAt(lim);
    			killGroup(seed,replay);
    		};
    	}
    	return(some);
    }
    private void doDone(replayMode replay)
    {
        acceptPlacement();
        lastPlacement++;
        firstPlayedLocation = null;
        if(board_state==BloomsState.Draw)
        {	AR.setValue(score, -1);
        	setState(BloomsState.Gameover);
        }
        else if (board_state==BloomsState.Resign)
        {
        	win[whoseTurn^1] = true;
            AR.setValue(score, -1);
        	setState(BloomsState.Gameover);
        }
        else
        {
        doCaptures(whoseTurn^1,true,replay);
        boolean selfCapture = doCaptures(whoseTurn,allowSelfCapture,replay);
        if (selfCapture && !allowSelfCapture)
        {	// reject illegal moves.  This shouldn't happen
        	// at all in the UI, but the robot can be run
        	// with illegal moves allowed; this makes those 
        	// moves look very bad so they'll never be chosen.
            win[whoseTurn^1] = true;
            AR.setValue(score, -1);
    		setState(BloomsState.Gameover);
        }
        else
        {	setNextPlayer(replay);
        	setNextStateAfterDone(replay);
        }}
    }


    public boolean Execute(commonMove mm,replayMode replay)
    {	Bloomsmovespec m = (Bloomsmovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }

        //G.print("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
        case MOVE_DONE:
         	doDone(replay);

            break;

        case MOVE_DROPB:
        	{
			BloomsChip po = pickedObject;
			BloomsCell src = getCell(m.source,m.to_col,m.to_row); 
			BloomsCell dest =  getCell(BloomsId.BoardLocation,m.to_col,m.to_row);
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
					if(pickedObject!=null) { unPickObject(); }
					break;
				}
				pickObject(src);
				m.target = pickedObject;
				G.Assert((board_state==BloomsState.Puzzle)
							|| (ownerIndex(pickedObject)==whoseTurn),"color mismatch");

				if(replay!=replayMode.Replay && ((po==null) || (replay==replayMode.Single)))
	            	{ animationStack.push(src);
	            	  animationStack.push(dest); 
	            	}
	            dropObject(dest);
	            setNextStateAfterDrop(replay);
				}
        	}
             break;

        case MOVE_PICK:
 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
 			BloomsCell src = getCell(m.source,m.to_col,m.to_row);
 			if(isDest(src)) { unDropObject(); }
 			else
 			{
        	// be a temporary p
        	pickObject(src);
        	m.target = pickedObject;
 			}}
            break;

        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
            BloomsCell dest = getCell(m.source,m.to_col,m.to_row);
            if(isSource(dest)) { unPickObject(); }
            else { m.target = pickedObject; dropObject(dest); }
        	}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            firstPlayedLocation = null;
            int nextp = whoseTurn^1;
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(BloomsState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ setState(BloomsState.Gameover); 
               	}
            else {  setNextStateAfterDone(replay); }

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?BloomsState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
        	firstPlayedLocation = null;
            setState(BloomsState.Puzzle);
 
            break;
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			AR.setValue(score,-1);
			setState(BloomsState.Gameover);
			break;

		case EPHEMERAL_SELECT:
			endgameCondition = EndgameCondition.values()[m.to_row];
			AR.setValue(endgameApproved,false);
			break;
		case EPHEMERAL_APPROVE:
			{
			int ind = playerIndex(m.source);
			endgameApproved[ind] = true;
			m.target = playerColor(ind);
			}
			break;
		case SELECT:
			endgameSelected = true;
			endgameCondition = EndgameCondition.values()[m.to_row];
			AR.setValue(endgameApproved,true);
			setNextStateAfterDone(replayMode.Live);
			break;
        default:
        	cantExecute(m);
        }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips(BloomsChip ch)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting Legal Hit state %s", board_state);
        case Play:
        case PlayLast:
        case PlayFirst:
        	// for blooms, you can pick up a stone in the storage area
        	// but it's really optional
        	return(ownerIndex(ch)==whoseTurn);
        case Play1:
        case Play1Capture:
        	return((ownerIndex(ch)==whoseTurn) && (ch!=firstPlayedLocation.topChip()));
        case Confirm:
		case Resign:
		case Draw:
		case Gameover:
		case SelectEnd:
			return(false);
        case Puzzle:
            return ((pickedObject!=null)?(pickedObject.colorSet==ch.colorSet):true);
        }
    }
    public boolean LegalToHitBoard(BloomsCell c)
    {
    	return(LegalToHitBoard(c,pickedObject,firstPlayedLocation));
    }
    public boolean LegalToHitBoard(BloomsCell c,BloomsChip ch)
    {
    	return(LegalToHitBoard(c,ch,firstPlayedLocation));
    }
    public BloomsCell firstChipPlayedAtari()
    {
		return ((firstPlayedLocation==null)||(board_state!=BloomsState.Play1Capture))
				? null
				: firstPlayedLocation;

    }
    public boolean LegalToHitBoard(BloomsCell c,BloomsChip po,BloomsCell first)
    {	if(c==null) { return(false); }
    	if((po==null) && (first!=null)) { po = first.topChip().otherColor(); }
    	boolean mustCapture = false;
    	switch (board_state)
        {
		case Play:
		case PlayFirst:
		case PlayLast:
			if((po!=null) && c.isEmpty())
			{
				return(sweepCanCapture(c,po,first,false));
			}
			return(false);
			
		case Play1Capture:
			mustCapture = true;
			//$FALL-THROUGH$
		case Play1:
			if(isDest(c) && (pickedObject==null)) { return(true); }
			BloomsChip top = firstPlayedLocation.topChip();
			if((po!=null) && c.isEmpty() && (top!=po))
			{	
				return(sweepCanCapture(c,po,first,mustCapture));
			}
			return(false);
		case Draw:
			return(isDest(c));
		case Gameover:
		case SelectEnd:
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
    public void RobotExecute(Bloomsmovespec m)
    {
        robotState.push(board_state); //record the starting state. The most reliable
        robotCapture.push(captureStack.size());
        robotPlayed.push(firstPlayedLocation);
        if (Execute(m,replayMode.Replay))
        {	acceptPlacement();
            if (m.op == MOVE_DONE)
            {
            }
            else if (board_state==BloomsState.Confirm)
            {
                doDone(replayMode.Replay);
            }
        }
    }
 
    private void undoCaptures(int n)
    {
    	while(captureStack.size()>n)
    	{	
    		BloomsCell to = captureStack.pop();
    		BloomsChip ch = chipStack.pop();
    		captured[ownerIndex(ch)]--;
    		SetBoard(to,ch);
    	}
    }
   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(Bloomsmovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	BloomsState state = robotState.pop();
    	undoCaptures(robotCapture.pop());
    	firstPlayedLocation = robotPlayed.pop();
        switch (m.op)
        {
   	    default:
   	    	throw G.Error("Can't un execute %s", m);
        case MOVE_DONE:
            break;
            
        case MOVE_DROPB:
        	{
        	BloomsCell to = getCell(m.to_col,m.to_row);
        	SetBoard(to,null);
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
 public boolean isLegal(Bloomsmovespec m)
 {
	 switch(m.op){
	 default: return(true);
	 case MOVE_DROPB:
	 	{
		boolean legal = LegalToHitBoard(getCell(m.to_col,m.to_row),getCell(m.source,m.to_col,m.to_row).topChip(),firstPlayedLocation);
		return(legal);
	 	}
	 }
 }
 public boolean isEyeFill(Bloomsmovespec m)
 {	if(m.op==MOVE_DROPB)
 	{
	 BloomsCell c = getCell(m.to_col,m.to_row);
	 BloomsChip ch = BloomsChip.getChip(m.source);
	 for(int dir=c.geometry.n; dir>0; dir--)
	 {
		 BloomsCell adj = c.exitTo(dir);
		 if(adj!=null)
		 {
			 BloomsChip top = adj.topChip();
			 if(top==null) { return(false); }
			 if(top.colorSet!=ch.colorSet) { return(false); }
		 }
	 }
	 return(true);
 	}
 	return(false);
 }
 public BloomsChip getRandomColor(Random rand)
 {
	 BloomsChip ch = firstPlayedLocation==null
	 			? playerColors[whoseTurn][rand.nextInt(2)] 
	 			: firstPlayedLocation.topChip().otherColor(); 
	 return(ch);
 }
 public BloomsCell getRandomEmptyCell(Random rand,boolean mustCapture)
 {
	 int sz = emptyCells.size();
	 int idx = rand.nextInt(sz+(mustCapture ? 0 : 1));
	 if(idx==sz) { return(null); }
	 BloomsCell c = emptyCells.elementAt(idx);
	 return(c);
 }

 CommonMoveStack  GetListOfAnyMoves(int who)
 {	CommonMoveStack all = new CommonMoveStack();
 	switch(board_state)
 	{
 	default: throw G.Error("Not expecting state %s",board_state);
 	case SelectEnd:
 		addAsyncMoves(all,who);
 		break;
 	case Confirm:
 	case Resign:
 	case Draw:
 		all.push(new Bloomsmovespec(MOVE_DONE,who));
 		break;
 	case Play1Capture:	// mandatory capture
 	case Play:
 	case Play1:
 	case PlayFirst:
 	case PlayLast:
 	BloomsChip firstColor = firstPlayedLocation!= null ? firstPlayedLocation.topChip() : null;
  	for(int lim=emptyCells.size()-1; lim>=0; lim--)
 	{	BloomsCell c = emptyCells.elementAt(lim);
 		// this gets all moves, without considering legality.  The bot works
 		// by preferring legal moves, because illegal moves lead to an immediate
 		// loss.  If the bot is forced to prefer an illegal move, the overall
 		// bot controller will resign instead of playing an illegal move.
 		for(BloomsChip ch : playerColors[who])
 			{ 
 			  if(ch!=firstColor)
 			  	{ 
 				  all.addElement(new Bloomsmovespec(MOVE_DROPB,c.col,c.row,ch.id,who));
 			  	}
 			}
 		}
 	}
 	return(all);
 }
 private void addAsyncMoves(CommonMoveStack all,int who)
 {
		if(!endgameApproved[who])
		{
			all.push(new Bloomsmovespec(EPHEMERAL_APPROVE,playerColor(who).id,who));
		}
		else if(allApproved())
		{
			all.push(new Bloomsmovespec(SELECT,playerColor(who).id,endgameCondition,who));
		}
 }
 CommonMoveStack  GetListOfLegalMoves(int who)
 {	CommonMoveStack all = new CommonMoveStack();
 	boolean mustCapture = false;
 	switch(board_state)
 	{
 	default: throw G.Error("Not expecting state %s",board_state);
 	case SelectEnd:
 		addAsyncMoves(all,who);
 		break;
 	case Confirm:
 	case Resign:
 	case Draw:
 		all.push(new Bloomsmovespec(MOVE_DONE,who));
 		break;
 	case Play1Capture:	// mandatory capture
 		mustCapture = true;
		//$FALL-THROUGH$
	case Play:
 	case Play1:
 	case PlayFirst:
 	case PlayLast:
 	if(!mustCapture) { all.push(new Bloomsmovespec(MOVE_DONE,who)); }
 	BloomsChip firstColor = firstPlayedLocation!= null ? firstPlayedLocation.topChip() : null;
  	for(int lim = emptyCells.size()-1; lim>=0; lim--)
 	{	BloomsCell c = emptyCells.elementAt(lim);
 		// this gets all moves, without considering legality.  The bot works
 		// by preferring legal moves, because illegal moves lead to an immediate
 		// loss.  If the bot is forced to prefer an illegal move, the overall
 		// bot controller will resign instead of playing an illegal move.
 		for(BloomsChip ch : playerColors[who])
 			{ if(ch!=firstColor)
 			  if(sweepCanCapture(c,ch,firstPlayedLocation,mustCapture))
 			  	{ 
 				  all.addElement(new Bloomsmovespec(MOVE_DROPB,c.col,c.row,ch.id,who));
 			  	}
 			}
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

 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(Character.isDigit(txt.charAt(0)))
	 	{ switch(variation)
	 		{
	 		case blooms_4:
	 		case blooms_5:
	 		case blooms_6:
	 		case blooms_7:
	 			xpos -= cellsize/2;
	 			break;
 			default: G.Error("case %s not handled",variation);
	 		}
	 	}
 		else
 		{ 
 		  ypos += cellsize/4;
 		}
 	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
 }

}
