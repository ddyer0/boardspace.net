package magnet;

import static magnet.Magnetmovespec.*;

import java.util.*;

import lib.*;
import lib.Random;
import magnet.MagnetConstants.MagnetId;
import magnet.MagnetConstants.MagnetState;
import magnet.MagnetConstants.MagnetVariation;
import magnet.MagnetConstants.StateStack;
import online.game.*;


/**
 * MagnetBoard knows all about the game of Magnet, which is played
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

class MagnetBoard extends hexBoard<MagnetCell> implements BoardProtocol
{	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
	
    static final String[] MAGNETGRIDSTYLE = { null, "A1", "A1" }; // left and bottom numbers
    static MagnetId playerCaptures[] = { MagnetId.Red_Captures, MagnetId.Blue_Captures};
    static MagnetId playerRack[] = { MagnetId.Red_Chip_Pool, MagnetId.Blue_Chip_Pool};
    static final boolean ALLOW_ASYNC_PLAY = true;
    
    MagnetVariation variation = MagnetVariation.magnet;
	private MagnetState board_state = MagnetState.Puzzle;	
	private MagnetState unresign = null;	// remembers the orignal state when "resign" is hit
	private boolean robotBoard = false;
	public MagnetState getState() { return(board_state); }	
	
	public int movementCount[] = new int[2];				// records the height of movestack as of the last Done
	private CellStack moveStack = new CellStack();			// every piece movement is recorded here
	// this holds the chips that haven't been placed yet.
	public MagnetCell rack[][] = new MagnetCell[2][MagnetChip.nChips];
	// this holds the chips that were captured
	public int captureCount[] = new int[2];					// records the height of captures as of the last Done
	public MagnetCell captures[][] = new MagnetCell[2][MagnetChip.nChips];
	public CellStack magnetLocation = new CellStack();
	public MagnetCell center = null;
	public MagnetCell kingLocation[] = new MagnetCell[2];
	// this holds the locations chips were captured from
	public int capturesThisTurn = 0;
	public CellStack captureStack[] = new CellStack[]{new CellStack(),new CellStack()};
	public CellStack movementStack[] = new CellStack[]{new CellStack(),new CellStack()};
	public commonPlayer myPlayer = null;
	boolean setupDone[] = {false,false};
    private StringBuilder recorder = null;
    private int selectedDirection = -1;
    public MagnetCell selectedCell = null;
    private void record(String prefix,MagnetCell moving)
    {
		 if(recorder!=null)
		 {
			 recorder.append(prefix);
			 recorder.append(moving.col);
			 recorder.append(moving.row);
		 }

    }
    public double countPips(MagnetCell row[],int max)
    {	int pips = 0;
    	for(int i=0;i<max;i++)
    	{
    		MagnetCell c = row[i];
    		MagnetChip top = c.topChip();
    		pips += (top.isTrap()?1:top.maxFace())*5 - (top.upFace() - 1);
    	}
    	return(pips);
    }
    double maxPips ;
    
    public double scoreForPlayer(int pl)
    {	int pln = nextPlayer[pl];
    	return( (maxPips-countPips(captures[pln],captureCount[pln]))/maxPips);
    }
    // true if the only things left are both kings.
	private boolean kingsOnlyRemain() 
	{ 	return ((captureCount[FIRST_PLAYER_INDEX]==(MagnetChip.nChips-1))
				&& (captureCount[SECOND_PLAYER_INDEX]==(MagnetChip.nChips-1)));
	}
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(MagnetState st) 
	{ 	unresign = (st==MagnetState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

    
    private MagnetChip playerChip[]={MagnetChip.blue_back_1,MagnetChip.red_back_1};
    private MagnetCell playerCell[]=new MagnetCell[2];
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public MagnetChip getPlayerChip(int p) { return(playerChip[p]); }
	public MagnetId getPlayerColor(int p) { return(playerRack[p]); }
	public MagnetCell getPlayerCell(int p) { return(playerCell[p]); }
	public MagnetChip getCurrentPlayerChip() { return(playerChip[whoseTurn]); }

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

    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public MagnetChip pickedObject = null;
    public MagnetChip lastPicked = null;
    private MagnetCell blueChipPool = null;	// dummy source for the chip pools
    private MagnetCell redChipPool = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    boolean asyncPlay = ALLOW_ASYNC_PLAY;
    boolean reinitAsyncPlay = ALLOW_ASYNC_PLAY;
    public void setSimultaneousPlay(boolean val)
    {
    	asyncPlay = reinitAsyncPlay = val;
    }
    public MagnetChip lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public MagnetCell newcell(char c,int r)
	{	return(new MagnetCell(MagnetId.BoardLocation,c,r));
	}
	
	// constructor 
    public MagnetBoard(String init,int players,long key,commonPlayer activePlayer,int rev) // default constructor
    {	myPlayer = activePlayer;
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = MAGNETGRIDSTYLE;
        for(int player=FIRST_PLAYER_INDEX;player<=SECOND_PLAYER_INDEX; player++)
        { MagnetCell row[] = rack[player];
          MagnetCell cap[] = captures[player];
          
          for(int piece = 0; piece<row.length; piece++)
          {
        	  row[piece] = new MagnetCell(playerRack[player],'@',piece);
        	  cap[piece] = new MagnetCell(playerCaptures[player],'@',piece);
          }
        }
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
 		Random r = new Random(734687);	// this random is used to assign hash values to cells, common to all games of this type.
		setState(MagnetState.Puzzle);
		variation = MagnetVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		moveStack.clear();
		gametype = gtype;
		selectedDirection = -1;
		selectedCell = null;
		asyncPlay = reinitAsyncPlay;
		AR.setValue(setupDone,false);
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case magnet:
			initBoard(variation.firstInCol,variation.ZinCol,null);
	        for(int player=FIRST_PLAYER_INDEX;player<=SECOND_PLAYER_INDEX; player++)
	        { MagnetCell row[] = rack[player];
	          MagnetCell cap[] = captures[player];
	          MagnetChip pieces[] = MagnetChip.startingChips[player];
	          for(int piece = 0; piece<row.length; piece++)
	          {
	        	  MagnetCell c = row[piece];
	        	  c.reInit();
	        	  MagnetChip p = pieces[piece];
	        	  c.addChip(p);
	              if(p.isKing()) { kingLocation[player] = c; }

	        	  cap[piece].reInit();
	          }
	        }
	        maxPips = countPips(rack[0],rack[0].length);

		}

		allCells.setDigestChain(r);		// set the randomv for all cells on the board
 		

	    blueChipPool = new MagnetCell(r,MagnetId.Blue_Chip_Pool);
	    blueChipPool.addChip(MagnetChip.blue_back_1);
	    redChipPool = new MagnetCell(r,MagnetId.Red_Chip_Pool);
	    redChipPool.addChip(MagnetChip.red_back_1);
	    playerCell[FIRST_PLAYER_INDEX] = redChipPool; 
	    playerCell[SECOND_PLAYER_INDEX] = blueChipPool; 
	    	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    droppedDestStack.clear();
	    pickedSourceStack.clear();
	    stateStack.clear();
	    
	    pickedObject = null;
	    lastDroppedObject = null;

		playerChip[0]=MagnetChip.red_back_1;
		playerChip[1]=MagnetChip.blue_back_1;
	    // set the initial contents of the board to all empty cells
		for(MagnetCell c = allCells; c!=null; c=c.next) { c.reInit(); }
        magnetLocation.clear();
		MagnetCell mag =  center = getCell('F',6);
		mag.addChip(MagnetChip.magnet);
		magnetLocation.push(mag);
		
        animationStack.clear();
        moveNumber = 1;
        AR.setValue(captureCount, 0);
        AR.setValue(movementCount,0);
        capturesThisTurn = 0;
        for(CellStack cs : captureStack) { cs.clear(); }
        for(CellStack cs : movementStack) { cs.clear(); }
        // note that firstPlayer is NOT initialized here
    }


    /** create a copy of this board */
    public MagnetBoard cloneBoard() 
	{ MagnetBoard dup = new MagnetBoard(gametype,players_in_game,randomKey,myPlayer,MagnetBoard.REVISION); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((MagnetBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(MagnetBoard from_b)
    {
        super.copyFrom(from_b);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        selectedDirection = from_b.selectedDirection;
        selectedCell = getCell(from_b.selectedCell);
        asyncPlay = from_b.asyncPlay;
        reinitAsyncPlay = from_b.reinitAsyncPlay;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        copyFrom(rack,from_b.rack);
        copyFrom(captures,from_b.captures);
        getCell(kingLocation,from_b.kingLocation);
        AR.copy(setupDone,from_b.setupDone);
        AR.copy(captureCount,from_b.captureCount);
        AR.copy(movementCount,from_b.movementCount);
        getCell(captureStack,from_b.captureStack);
        capturesThisTurn = from_b.capturesThisTurn;
        getCell(movementStack,from_b.movementStack);
        getCell(magnetLocation,from_b.magnetLocation);
        stateStack.copyFrom(from_b.stateStack);
        getCell(moveStack,from_b.moveStack);
        pickedObject = from_b.pickedObject;
        lastPicked = null;
        center = getCell(center);
 
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((MagnetBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(MagnetBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(selectedDirection==from_b.selectedDirection,"selectedDirection mismatch");
        G.Assert(sameCells(selectedCell,from_b.selectedCell),"selected Cell match");
        G.Assert(AR.sameArrayContents(setupDone,from_b.setupDone),"setupDone mismatch");
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSource mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"pickedSource mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(sameCells(kingLocation, from_b.kingLocation),"kingLocation mismatch");
        G.Assert(AR.sameArrayContents(playerChip,from_b.playerChip),"playerChip mismatch");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(sameCells(moveStack,from_b.moveStack),"movestack mismatch");
        G.Assert(sameCells(rack,from_b.rack),"rack mismatch");
        G.Assert(AR.sameArrayContents(captureCount,from_b.captureCount),"captureCount mismatch");
        G.Assert(AR.sameArrayContents(movementCount,from_b.movementCount),"movementCount mismatch");
        G.Assert(sameCells(captures,from_b.captures),"captures mismatch");
        G.Assert(sameCells(captureStack,from_b.captureStack),"captureStack mismatch");
        G.Assert(capturesThisTurn==from_b.capturesThisTurn,"capturesThisTurn mismatch");
        G.Assert(sameCells(magnetLocation,from_b.magnetLocation),"magnetLocation mismatch");
        G.Assert(AR.sameArrayContents(setupDone,from_b.setupDone),"setupDone mismatch");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Digest matches");

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
		v ^= Digest(r,selectedDirection);
		v ^= Digest(r,selectedCell);
		v ^= Digest(r,moveStack);
		v ^= Digest(r,rack);
		v ^= Digest(r,kingLocation);
		v ^= Digest(r,captureCount);
		v ^= Digest(r,movementCount);
		v ^= Digest(r,captures);
		v ^= Digest(r,captureStack);
		v ^= Digest(r,capturesThisTurn);
		v ^= Digest(r,movementStack);
		v ^= Digest(r,setupDone);
		v ^= Digest(r,magnetLocation);
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
        	throw G.Error("Move not complete, can't change the current player");
        case Puzzle:
            break;
        case Play:
        	// some damaged games have 2 dones in a row
        	if(replay==replayMode.Live) { throw G.Error("Move not complete, can't change the current player"); }
			//$FALL-THROUGH$
        case Confirm:
        case Confirm_Synchronous_Setup:
        case Confirm_Setup:
        case Promote:
        case Resign:
        case Setup:
        case Synchronous_Setup:
        case NormalStart:
        case WaitForStart:
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
    private MagnetCell unDropObject()
    {	MagnetCell rv = droppedDestStack.pop();
    	setState(stateStack.pop());
    	pickedObject = rv.removeTop(); 	// SetBoard does ancillary bookkeeping
    	if(pickedObject.isKing()) 
    		{ kingLocation[pickedObject.playerIndex()] = null; 
    		}
    	switch(board_state)
    	{
    	default: break;
    	case Puzzle:
    		switch(rv.rackLocation())
    		{
    		default: break;
    		case Blue_Captures:
    		case Red_Captures:
    			{
    			int pl = nextPlayer[pickedObject.playerIndex()];
    			captureCount[pl]--;
    			captureStack[pl].pop();
    			}
    			break;
    		}
    		break;
    	case Play: magnetLocation.pop();
    		break;
    	}
    	undoCaptures();
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    void unPickObject()
    {	MagnetCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	if(pickedObject.isKing()) { kingLocation[pickedObject.playerIndex()] = rv; }
    	switch(rv.rackLocation())
    	{
    	default: break;
     	case Red_Captures:
    	case Blue_Captures:
    		{
    		int pl = nextPlayer[pickedObject.playerIndex()];
    		captureCount[pl]++;
    		captureStack[pl].push(rv);
    		}
    		break;
    	}
    	rv.addChip(pickedObject);
    	pickedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(MagnetCell c,replayMode replay)
    {
       droppedDestStack.push(c);
       stateStack.push(board_state);
   	
       if(pickedObject.isKing()) { kingLocation[pickedObject.playerIndex()] = c; }

       switch (c.rackLocation())
        {
        default:
        	throw G.Error("not expecting dest %s", c.rackLocation);
        case Blue_Captures:
        case Red_Captures:
        	int pl = pickedObject.playerIndex();
        	c.addChip(pickedObject);
        	pickedObject = null;
        	{
        	int pl2 = nextPlayer[pl];
        	captureCount[pl2]++;
        	captureStack[pl2].push(c);
        	}
        	break;
        case Blue_Chip_Pool:
        case Red_Chip_Pool:		// back in the pool, we don't really care where
        	c.addChip(pickedObject);
        	pickedObject = null;
            break;
        case BoardLocation:	// already filled board slot, which can happen in edit mode
           	c.addChip(pickedObject);;
            lastDroppedObject = pickedObject;
            pickedObject = null;
            switch(board_state)
            {
            default: break;
            case Play:
            	magnetLocation.push(c);
             	break;
            }
            break;
        }
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(MagnetCell c)
    {	return(droppedDestStack.top()==c);
    }
    public MagnetCell getDest()
    {
    	return(droppedDestStack.top());
    }
    public MagnetCell getSource()
    {
    	return(pickedSourceStack.top());
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { MagnetChip ch = pickedObject;
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
    private MagnetCell getCell(MagnetId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	return(getCell(col,row));
        case Red_Captures:
        	return(captures[FIRST_PLAYER_INDEX][row]);
        case Blue_Captures:
        	return(captures[SECOND_PLAYER_INDEX][row]);
        case Blue_Chip_Pool:
        	return(rack[SECOND_PLAYER_INDEX][row]);
        case Red_Chip_Pool:
        	return(rack[FIRST_PLAYER_INDEX][row]);
        } 	
    }
    public MagnetCell getCell(MagnetCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private MagnetChip pickObject(MagnetCell c)
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
			c.removeTop();
        	}
            break;
        case Red_Captures:
        case Blue_Captures:
        	lastPicked = pickedObject = c.removeTop();
        	{
        	int pl = nextPlayer[pickedObject.playerIndex()];
        	captureCount[pl]--;
        	captureStack[pl].pop();
        	}
        	break;
        case Blue_Chip_Pool:
        case Red_Chip_Pool:
        	lastPicked = pickedObject = c.removeTop();
        	break;
        }
    	if(pickedObject.isKing()) 
    		{ kingLocation[pickedObject.playerIndex()] = null; 
    		}
    	return(pickedObject);
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(MagnetCell c)
    {	return(c==pickedSourceStack.top());
    }
    boolean allStartupPlaced(int forPlayer)
    {
    	for(MagnetCell c = allCells; c!=null; c=c.next)
    	{	MagnetChip top = c.topChip();
    		if((top!=MagnetChip.magnet)
    				&& (c.isStartingCell(forPlayer)
    						&& (top==null)))
    				{ return(false); }
    	}
    	return(true);
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(int opcode,MagnetCell dest,replayMode replay)
    {

        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case Select:
        case Gameover:
        case FirstSelect:
        	break;
        //case Confirm_Synchronous_Setup:
        //	acceptPlacement();
        //	if(!allStartupPlaced(forPlayer)) { setState(MagnetState.Setup);}
        //	break;
        case Setup:
        case WaitForStart:
		case Confirm_Setup:
        	if((opcode==MOVE_FROM_TO) && robotBoard)
            {	// the robot issues move_from_to even in async state.
            	// these don't need to be confirmed.  The robot will stop
            	// moving until the human player finishes.
            	int pl = dest.topChip().playerIndex();
            	if(allStartupPlaced(pl))
            		{setupDone[pl] = true;
            		 if(setupDone[nextPlayer[pl]] && (board_state!=MagnetState.Confirm_Setup))
            		 {
            		 setState(MagnetState.NormalStart);
            		 }
            		 }
            	break;
            }
        	else // player moves
        	{
        		int pl = dest.topChip().playerIndex();
            	if(allStartupPlaced(pl) && (pl==myPlayer.boardIndex))
            	{
            		setState(MagnetState.Confirm_Setup);
            	}
        	}
        	break;
        case Synchronous_Setup:
        	if(allStartupPlaced(whoseTurn)) 
        		{ setState(MagnetState.Confirm_Synchronous_Setup);
        		}
        	break;
       case Play:
    	   G.Assert(dest.topChip()==MagnetChip.magnet,"not the magnet moving");
    	   if(firstMove() && multiplePiecesMove(dest,whoseTurn))
       		{
       		setState(MagnetState.FirstSelect);
       		}
       		else if(multiplePiecesReachMagnet(dest,whoseTurn))
       		{
       		setState(MagnetState.Select);
       		}
       		else { setState(MagnetState.Confirm); }
			break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    private void setNextStateAfterDone(int forPlayer,replayMode replay)
    {	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state %s",board_state);
    	case Setup:	// other player hit done for himself during setup
    	case Gameover: break;
    	case Confirm_Setup:
    		if(myPlayer.boardIndex == forPlayer)
    		{
    		setState( (setupDone[FIRST_PLAYER_INDEX]&&setupDone[SECOND_PLAYER_INDEX])
    					? MagnetState.NormalStart : MagnetState.WaitForStart);
    		}
    		break;
    		
    	case WaitForStart:
    		setState(MagnetState.NormalStart);
    		break;
    	case Confirm_Synchronous_Setup:
    		
    	case NormalStart:
    		capturesThisTurn = 0;
    		if(!setupDone[nextPlayer[forPlayer]])
    		{
    			setState(asyncPlay?MagnetState.Setup:MagnetState.Synchronous_Setup);
    		}
    		else 
    			{ 
    			  setState((board_state==MagnetState.Confirm_Synchronous_Setup)
    					  	?MagnetState.NormalStart
    					  	:MagnetState.Play);
    			}
    		break;
    	case Confirm:
    		setState(MagnetState.Promote);
    		break;
    	case Promote:
    	case Puzzle:
    	case Synchronous_Setup:
    		capturesThisTurn = 0;
    		setState(MagnetState.Play);   		
    		break;
    	}
    }
    private boolean pieceMoves(MagnetCell magnet,MagnetCell c,int forPlayer)
    {	
    	for(int direction=0; direction<magnet.geometry.n; direction++)
    	{
    	MagnetCell moving = causesMovement(magnet,forPlayer,direction);
		if(moving==c)
			{ 
			return(true);
			}
    	}
    	return(false);
    }
    private boolean pieceReachesMagnet(MagnetCell magnet,MagnetCell c,int forPlayer)
    {	
    	for(int direction=0; direction<magnet.geometry.n; direction++)
    	{
    	MagnetCell moving = causesMovement(magnet,forPlayer,direction);
		if(moving==c)
			{ 
			MagnetChip chip = moving.topChip();
			int dis = chip.upFace();
			int invDir = direction+CELL_HALF_TURN;
			while((dis-- > 0) && (moving!=magnet)) { moving = moving.exitTo(invDir); }
			if(moving==magnet) { return(true); }
			}
    	}
    	return(false);
    }
    private boolean multiplePiecesReachMagnet(MagnetCell magnet,int forPlayer)
    {	int reached = 0;
    
    	for(int direction=0; direction<magnet.geometry.n; direction++)
    	{
    	MagnetCell moving = causesMovement(magnet,forPlayer,direction);
		if(moving!=null)
			{ 
			MagnetChip chip = moving.topChip();
			int dis = chip.upFace();
			int invDir = direction+CELL_HALF_TURN;
			while((dis-- > 0) && (moving!=magnet)) { moving = moving.exitTo(invDir); }
			if(moving==magnet) { reached++; if(reached>1) { return(true); }}
			}
    	}
    	return(false);
    }
    
    private boolean multiplePiecesMove(MagnetCell magnet,int forPlayer)
    {	int reached = 0;
    
    	for(int direction=0; direction<magnet.geometry.n; direction++)
    	{
    	MagnetCell moving = causesMovement(magnet,forPlayer,direction);
		if(moving!=null)
			{ 
			reached++; 
			if(reached>1) { return(true); }
			}
    	}
    	return(false);
    }
    private void doSingleMovementAndCaptures(MagnetCell magnet,replayMode replay,int forPlayer,int direction)
    {
    	MagnetCell moving = causesMovement(magnet,forPlayer,direction);
    	if(moving!=null)
    	{	MagnetChip chip = moving.topChip();
    		MagnetCell firstMoving = moving;
    		boolean friendlyMagnet = magnet.isEmpty() ? false : magnet.chipAtIndex(0).playerIndex()==forPlayer;
    		int distance = chip.upFace();
    		int reverse = direction+CELL_HALF_TURN;
    		int otherPlayer = nextPlayer[forPlayer];
    		if(friendlyMagnet) { magnet = magnet.exitTo(direction); }
    		while((distance-- > 0) && (moving!=magnet))
    		{
    			moving = moving.exitTo(reverse);
    			if(!moving.isEmpty())
    			{MagnetChip victim = moving.chipAtIndex(0);	
    			 if(victim.playerIndex()==otherPlayer)
    			 {	
    				{CellStack cs = captureStack[forPlayer];
    			    MagnetCell cap = captures[forPlayer][cs.size()];
    			    capturesThisTurn++;
    			    cap.addChip(victim);
    			    if(victim.isKing())
    			    	{ kingLocation[otherPlayer] = cap; 
    			    	} 
    				cs.push(moving);
    				 if(replay!=replayMode.Replay)
    				 {
    					 animationStack.push(moving);
    					 animationStack.push(cap);
    				 }
					 record(" x",moving);
    				 moving.removeChipAtIndex(0);
    				 }
    				 if(victim.isTrap() && (chip!=null))
    				 	{ CellStack cs = captureStack[otherPlayer];
    				 	  MagnetCell cap = captures[otherPlayer][cs.size()];
    				 	  cap.addChip(chip);
    				 	  cs.push(firstMoving);
    				 	  capturesThisTurn++;
    				 	  if(chip.isKing()) 
    				 	  	{ kingLocation[forPlayer]=cap; 
    				 	  	}
    				 	  chip = null;
    				 	  if(replay!=replayMode.Replay)
    				 	  {
    				 		  animationStack.push(moving);
    				 		  animationStack.push(cap);
    				 	  }
				 		  record(" x",moving);
    				 	}
    			 }
    			 
    			}
    		}


    		if(chip!=null) 
    			{ moving.insertChipAtIndex(0,chip); 
    			  if(chip.isKing()) { kingLocation[forPlayer] = moving; }
    			  movementStack[forPlayer].push(firstMoving);
    			  movementStack[forPlayer].push(moving);
    			}
    		firstMoving.removeTop();
        	if(replay!=replayMode.Replay)
        	{
        		animationStack.push(firstMoving);
        		animationStack.push(moving);
        	}
    		record(" >",moving);


    	}
    }
    private boolean firstMove()
    {
    	return((movementStack[FIRST_PLAYER_INDEX].size()==0)
    			&& (movementStack[SECOND_PLAYER_INDEX].size()==0)
    			&& setupDone[whoseTurn]);
    }
    private void doMovementAndCaptures(MagnetCell magnet,replayMode replay)
    {
    	doMovementAndCaptures(magnet,replay,selectedDirection);
    	selectedDirection = -1; 
    	selectedCell = null;
    }
    private void doMovementAndCaptures(MagnetCell magnet,replayMode replay,int firstDirection)
    {
    	// the easy case where at most one piece reaches tower

		if(firstMove())
		{
			doSingleMovementAndCaptures(magnet,replay,whoseTurn,firstDirection);	
		}
		else
		{
    	for(int dir=0;dir<magnet.geometry.n;dir++)
    		{
    		doSingleMovementAndCaptures(magnet,replay,whoseTurn,dir+firstDirection);
    		}}   	
    }
    public boolean isNewlyPromoted(MagnetCell c)
    {	CellStack stack = movementStack[whoseTurn];
   		for(int start = movementCount[whoseTurn],end=stack.size(); start<end; start+=2)
   		{
   			MagnetCell from = stack.elementAt(start);
   			MagnetCell to = stack.elementAt(start+1);
   			if((from==to)&&(from==c)) 
   				{ return(true); }
   		}
   		return(false);
    }
    private void undoCaptures()
    {
    	for(int player=FIRST_PLAYER_INDEX; player<=SECOND_PLAYER_INDEX; player++)
    	{
    		while(movementStack[player].size()>movementCount[player])
    		{
    			MagnetCell to = movementStack[player].pop();
    			MagnetCell from = movementStack[player].pop();
    			MagnetChip rem = to.removeChipAtIndex(0);
    			if(from==to)
    			{	// was a promotion
    				rem = rem.getDemoted();
    			}	
    			if(rem.isKing()) { kingLocation[player]=from; }
    			from.insertChipAtIndex(0,rem);
    		}
    		int sz = 0;
    		capturesThisTurn = 0;
    		while( (sz = captureStack[player].size())>captureCount[player])
    		{	MagnetCell cs = captures[player][sz-1];
    			MagnetCell captureLoc = captureStack[player].pop();
    			MagnetChip capturePiece = cs.removeTop();
    			if(capturePiece.isKing())
    				{ kingLocation[nextPlayer[player]]=captureLoc; 
    				}
    			captureLoc.insertChipAtIndex(0,capturePiece);
    		}
 
    	}
    }
    private boolean checkForWin()
    {
    	if(setupDone[whoseTurn])
    		{for(int player = FIRST_PLAYER_INDEX;player<=SECOND_PLAYER_INDEX; player++)
    	{	MagnetCell king = kingLocation[player];
    		if(king!=null)
    		{	int next = nextPlayer[player];
    			if(king.rackLocation()==playerCaptures[next])
    				{ win[next]=true; 
    				  return(true); 
    				}
    		}}
    	}
    	// win by staring your turn on the center
    	if(kingLocation[nextPlayer[whoseTurn]] == center)
    		{ win[nextPlayer[whoseTurn]] = true; 
    		  return(true);
    		}
    	if(kingsOnlyRemain() && (kingLocation[whoseTurn]== center))
    	{
    		win[whoseTurn] = true;
    		return(true);
    	}
    	return(false);
    }
    
    private void doDone(int forPlayer,replayMode replay)
    {	
        acceptPlacement();
        switch(board_state)
        {
        default:
        	throw G.Error("Not expecting done in state %s",board_state);
        case Resign:
        	win[nextPlayer[whoseTurn]] = true;
        	setState(MagnetState.Gameover);
    		break;
        case Gameover:
         	break;

		case Confirm_Synchronous_Setup:
		case Confirm_Setup:	// asynchronous moves
	    case Setup:
	    case Synchronous_Setup:
		case WaitForStart:
			setupDone[forPlayer] = true;
			setNextPlayer(replay);
			//$FALL-THROUGH$
        case NormalStart:
			setNextStateAfterDone(forPlayer,replay);
			break;
        case Confirm:
           	doMovementAndCaptures(magnetLocation.top(),replay);
			if(checkForWin()) 
    		{ 
    		  setState(MagnetState.Gameover); 
    		}
        	else {
    			setNextStateAfterDone(whoseTurn,replay);
        	}
			break;
        case Promote:
        	setNextPlayer(replay);
            for(int player = FIRST_PLAYER_INDEX; player<=SECOND_PLAYER_INDEX; player++)
            {
            captureCount[player] = captureStack[player].size();
            movementCount[player] = movementStack[player].size();
            }
        	setNextStateAfterDone(whoseTurn,replay);
        }   

    }
    public MagnetState resetState()
    {
    	if(stateStack.size()>0) { return stateStack.top(); }
    	return board_state;
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	Magnetmovespec m = (Magnetmovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }
        recorder = robotBoard ? null : new StringBuilder();
        //G.print("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
        case EPHEMERAL_DONE:
        case MOVE_DONE:

         	doDone(m.player,replay);

            break;
        case MOVE_DEMOTE:
        	{
            	MagnetCell dest = getCell(MagnetId.BoardLocation,m.to_col,m.to_row);
            	MagnetChip chip = dest.chipAtIndex(0);
            	dest.setChipAtIndex(0, chip.getDemoted());
            	movementStack[whoseTurn].remove(dest,false);
            	movementStack[whoseTurn].remove(dest,false);
        	}
        	break;
        case MOVE_PROMOTE:
        	{
        	MagnetCell dest = getCell(MagnetId.BoardLocation,m.to_col,m.to_row);
        	MagnetChip chip = dest.chipAtIndex(0);
        	MagnetChip promo = chip.getPromoted();
        	G.Assert(promo!=null,"%s can't be promoted",chip);
        	dest.setChipAtIndex(0, promo);
        	movementStack[whoseTurn].push(dest);
        	movementStack[whoseTurn].push(dest);
        	}
        	break;
        case EPHEMERAL_MOVE:
        case MOVE_RANDOM:
		case MOVE_FROM_TO:
        	{
    			MagnetCell src = getCell(m.source,m.from_col,m.from_row);
    			MagnetCell dest =  getCell(m.dest,m.to_col,m.to_row);
    			boolean unpick = pickedObject!=null && (src==getSource());
    			if(unpick) { unPickObject(); }
    			MagnetChip po = src.removeTop();
	            dest.addChip(po);
	            if(po==MagnetChip.magnet) { magnetLocation.push(dest); }
	            else if(po.isKing()) { kingLocation[po.playerIndex()] = dest; }
	            /**
	             * if the user clicked on a board space without picking anything up,
	             * animate a stone moving in from the pool.  For Hex, the "picks" are
	             * removed from the game record, so there are never picked stones in
	             * single step replays.
	             */
	            if(!unpick && replay!=replayMode.Replay)
	            	{ animationStack.push(src);
	            	  animationStack.push(dest); 
	            	}
	            setNextStateAfterDrop(m.op,dest,replay);
	            
        	}
        	break;
		case MOVE_DROPB:
        	{
			MagnetChip po = pickedObject;
			MagnetCell src = getSource(); 
			MagnetCell dest =  getCell(MagnetId.BoardLocation,m.to_col,m.to_row);
			
	            /**
	             * if the user clicked on a board space without picking anything up,
	             * animate a stone moving in from the pool.  For Hex, the "picks" are
	             * removed from the game record, so there are never picked stones in
	             * single step replays.
	             */
	            if((replay==replayMode.Single) && (po!=null))
	            	{ animationStack.push(src);
	            	  animationStack.push(dest); 
	            	}

            dropObject(dest,replay);
            setNextStateAfterDrop(m.op,dest,replay);
    	}
             break;
		case MOVE_SELECT:
			{	
				MagnetCell magnet = magnetLocation.top();
				MagnetCell selected = getCell(MagnetId.BoardLocation,m.from_col,m.from_row);
				if(selected==selectedCell)
				{
				selectedDirection = -1;
				selectedCell = null;
				setState(stateStack.pop());
				}
				else{
				selectedDirection = findDirection(magnet.col,magnet.row,selected.col,selected.row);
				selectedCell = selected;
				stateStack.push(board_state);
				setState(MagnetState.Confirm);
				}
			}
			break;
        case EPHEMERAL_PICK:
        case EPHEMERAL_PICKB:
        case MOVE_PICK:
 		case MOVE_PICKB:
         	// come here only where there's something to pick, which must
 			{
 			MagnetCell src = getCell(m.source,m.from_col,m.from_row);
 			if(isDest(src)) { unDropObject(); }
 			else
 			{
        	// be a temporary p
        	pickObject(src);
        	switch(board_state)
        	{
        	case Puzzle:
         		break;
        	case Confirm_Setup:
        		setState(MagnetState.Setup);
        		break;
        	case Confirm_Synchronous_Setup:
        		setState(MagnetState.Synchronous_Setup);
        		break;
        	case Confirm:
        	case Promote:
        		
        		setState(MagnetState.Play);
        		break;
        	default: ;
        	}}}
            break;

        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
            MagnetCell dest = getCell(m.dest,m.to_col,m.to_row);
            if(isSource(dest)) { unPickObject(); }
            else { dropObject(dest,replay); }
        	}
            break;
        case NORMALSTART:
        	setWhoseTurn(FIRST_PLAYER_INDEX);
        	doDone(FIRST_PLAYER_INDEX,replay);
        	break;
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            int nextp = nextPlayer[whoseTurn];
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            if(setupDone[whoseTurn])
            {
            setState(MagnetState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ setState(MagnetState.Gameover); 
               	}
            else {  setNextStateAfterDone(whoseTurn,replay); }
            }
            else { setState(asyncPlay?MagnetState.Setup:MagnetState.Synchronous_Setup); }
            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?MagnetState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(MagnetState.Puzzle);
 
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
       		setState(MagnetState.Gameover);
       		break;
        default:
        	cantExecute(m);
        }
        if(recorder!=null) { m.movements = recorder.toString(); recorder = null; }
        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }

    // legal to hit the chip storage area
    public boolean legalToHitChips(MagnetCell c,int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting Legal Hit state %s", board_state);
        	
        case Setup:
        	return( (c.rackLocation()==playerRack[myPlayer.boardIndex])
        			&& ((pickedObject!=null) == c.isEmpty())
        			);

        case Synchronous_Setup:
        	return( (c.rackLocation()==playerRack[whoseTurn])
        			&& ((pickedObject!=null) == c.isEmpty())
        			);
        case Puzzle:
        	return( (c.rackLocation()==(setupDone[player] ? playerCaptures[player] : playerRack[player]))
        			&& ((pickedObject!=null) == c.isEmpty())
        			);
        case Play:
        case Confirm:
        case Confirm_Setup:
        case Confirm_Synchronous_Setup:
		case Resign:
		case Gameover:
		case Select:
		case FirstSelect:
		case NormalStart:
		case WaitForStart:
		case Promote:
			return(false);
        }
    }
    // return the cell that contains the moving piece, or null
    private MagnetCell causesMovement(MagnetCell magnet,int forPlayer,int direction)
    {
     	if(magnet!=null)
    	{
     	MagnetCell next = magnet;
     	boolean friendlyMagnet = magnet.isEmpty() ? false : magnet.chipAtIndex(0).playerIndex()==forPlayer;
     	int steps = 0;
     	while( (next=next.exitTo(direction))!=null) 
    	{	steps++;
    		MagnetChip top = next.isEmpty()?null:next.chipAtIndex(0);
    		if((top!=null) && (top.playerIndex()==forPlayer))
    		{	if((steps==1)&&friendlyMagnet) { return(null); }
    			return(next);
    		}
    	}
    	}
    	return(null);
    }
    
    private boolean causesMovement(MagnetCell c,int forPlayer)
    {
    	for(int direction=0; direction<c.geometry.n; direction++)
    	{
    		if(causesMovement(c,forPlayer,direction)!=null) { return(true); }
      	}
    	return(false);
    }
    public boolean LegalToHitBoard(MagnetCell c,int player)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
        case Synchronous_Setup:
        	return(c.isStartingCell(whoseTurn)
        			&& ((pickedObject!=null)==c.isEmpty()));
        	
        case Setup:
        	return(c.isStartingCell(myPlayer.boardIndex)
        			&& ((pickedObject!=null)==c.isEmpty()));

        case Confirm_Synchronous_Setup:
        case Confirm_Setup:
        	return(c.isStartingCell(myPlayer.boardIndex)
        			&& ((pickedObject!=null)==c.isEmpty()));
		case Play:
			return((pickedObject==null) 
					? (c.topChip()==MagnetChip.magnet) 
					: causesMovement(c,whoseTurn)) ;
		case Gameover:
		case Resign:
		case NormalStart:
		case WaitForStart:
			return(false);
		case FirstSelect:
			return(pieceMoves(magnetLocation.top(),c,whoseTurn));

		case Select:
			return(pieceReachesMagnet(magnetLocation.top(),c,whoseTurn));
		case Promote:
			{
				for(int lim=movementStack[whoseTurn].size()-1,start=movementCount[whoseTurn];
					lim>start;
					lim-=2)
				{
				MagnetCell cell = movementStack[whoseTurn].elementAt(lim);
				if((cell==c) 
						&& (isNewlyPromoted(cell) || cell.chipAtIndex(0).canPromote()))
							{ return(true); 
							}
				}
				return(false);
			}
		case Confirm:
			return((selectedCell!=null)?(c==selectedCell) : isDest(c));
        default:
        	throw G.Error("Not expecting Hit Board state %s", board_state);
        case Puzzle:
            return ((pickedObject==null) == (c.topChip()!=null));
        }
    }
    
	// these are used by the robot to unwind
	private StateStack robotState = new StateStack();
	private IStack robotLevel = new IStack();

	public void terminateWithExtremePrejudice()
	{
		setState(MagnetState.Gameover);
		win[nextPlayer[whoseTurn]] = true;
	}
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Magnetmovespec m)
    {	//G.print("E "+m);
    	robotBoard = true;
        
        robotState.push(board_state); //record the starting state. The most reliable
     
        robotLevel.push(captureCount[FIRST_PLAYER_INDEX]);
        robotLevel.push(captureCount[SECOND_PLAYER_INDEX]);
        robotLevel.push(movementCount[FIRST_PLAYER_INDEX]);
        robotLevel.push(movementCount[SECOND_PLAYER_INDEX]);

    	boolean ok = true;
    	if(m.op == MOVE_PROMOTE)
    	{	// promotions can fail in robot replay because the state swap substituted
    		// a non-promotible piece, or because a promotible piece was captured by 
    		// a bomb.  We try to make these moves disappear from the tree by declaring
    		// them to be losses.
    		MagnetCell dest = getCell(m.to_col,m.to_row);
        	MagnetChip chip = dest.chipAtIndex(0);
        	if(chip==null) { ok = false; }
        	else 
        	{	MagnetChip promo = chip.getPromoted();
        		if(promo==null) { ok = false; }
        	}
    	}
    	if(ok)
    	{
        Execute(m,replayMode.Replay);
        if(board_state!=MagnetState.Gameover) { checkKings(); }
        
        if(board_state==MagnetState.Confirm)
        	{
        		doDone(m.player,replayMode.Replay);
        	}
    	}
    	else {
    		// make this move look very unattractive
    		terminateWithExtremePrejudice();
    	}

    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(Magnetmovespec m)
    {	//G.print("U "+m);
    	MagnetState state = robotState.pop();
    	movementCount[SECOND_PLAYER_INDEX] = robotLevel.pop();
    	movementCount[FIRST_PLAYER_INDEX] = robotLevel.pop();
    	captureCount[SECOND_PLAYER_INDEX] = robotLevel.pop();
    	captureCount[FIRST_PLAYER_INDEX] = robotLevel.pop();
        switch (m.op)
        {
        default:
   	    	throw G.Error("Can't un execute %s", m);
        case MOVE_DONE:
        	 if(state==MagnetState.Confirm_Synchronous_Setup)
        	 {
        		 setupDone[m.player] = false;
        	 }
             break;
        case MOVE_PROMOTE:
        	{
        		if(board_state!=MagnetState.Gameover)
        		{
        		// if we made a gameover, the promote was illegal
        		MagnetCell dest = getCell(m.dest,m.to_col,m.to_row);
        		MagnetChip chip = dest.chipAtIndex(0);
        		MagnetChip demoted = chip.getDemoted();
        		dest.setChipAtIndex(0, demoted);
        		movementStack[whoseTurn].pop();
        		movementStack[whoseTurn].pop();
        		}
        		break;
        	}
        case MOVE_SELECT:
        	undoCaptures();
        	break;
        case MOVE_FROM_TO:
        	{	MagnetCell dest = getCell(m.dest,m.to_col,m.to_row);
        		MagnetCell src = getCell(m.source,m.from_col,m.from_row);
        		undoCaptures();
        		MagnetChip top = dest.removeTop();
        		if(top.isKing()) { kingLocation[top.playerIndex()] = src; }
                
        		src.addChip(top);
        		switch(state)
        		{
        		default: break;
        		case Play:	magnetLocation.pop();
        			break;
        		}

        	}
        	
        	break;
        case NORMALSTART:
        case MOVE_RESIGN:
            break;
        }
        setState(state);
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
 private void checkKings()
 {
	 for(MagnetCell c : kingLocation)
	 {
		 G.Assert(c!=null,"kinglocation exists");
		 MagnetChip top = c.chipAtIndex(0);
		 G.Assert(top!=null && top.isKing(),"is king");
	 }
 }
 
 private void addSetupMoves(CommonMoveStack all,int who)
 {	CellStack dests = new CellStack();
 	for(MagnetCell c = allCells; c!=null; c=c.next)
 	{
 		if(c.isEmpty() && c.isStartingCell(who)) { dests.push(c);}
 	}
 	MagnetCell pieces[] = rack[who];
 	for(int i=0;i<pieces.length;i++)
 	{
 		MagnetCell src = pieces[i];
 		if(!src.isEmpty())
 		{	for(int lim=dests.size()-1; lim>=0; lim--)
 			{
 			all.push(new Magnetmovespec((board_state==MagnetState.Synchronous_Setup)
 										? MOVE_FROM_TO
 										: EPHEMERAL_MOVE,src,dests.elementAt(lim),who));
 			}
 		}
 	}
 }
 private void addMagnetMoves(CommonMoveStack all,int who)
 {	MagnetCell magnet = magnetLocation.top();
	 for(MagnetCell c = allCells; c!=null; c=c.next)
	 {
		 if(causesMovement(c,who))
		 {
			 all.push(new Magnetmovespec(MOVE_FROM_TO,magnet,c,who));
		 }
	 }
 }
 public void addFirstSelectMoves(CommonMoveStack all,int who)
 {	MagnetCell magnet = magnetLocation.top();
 	for(MagnetCell c = allCells; c!=null; c=c.next)
 	{
 		if(pieceMoves(magnet,c,whoseTurn))
 		{
 			all.push(new Magnetmovespec(MOVE_SELECT,c,who));
 		}
 	}
 }
 public void addSelectMoves(CommonMoveStack all,int who)
 {	MagnetCell magnet = magnetLocation.top();
 	for(MagnetCell c = allCells; c!=null; c=c.next)
 	{
 		if(pieceReachesMagnet(magnet,c,whoseTurn))
 		{
 			all.push(new Magnetmovespec(MOVE_SELECT,c,who));
 		}
 	}
 }

 public boolean addPromotionMoves(CommonMoveStack all,int who)
 {	CellStack stack = movementStack[who];
 	boolean some = false;
 	for(int lim=stack.size(),start=movementCount[who];
				start<lim;
				start+=2)
			{
			MagnetCell from = stack.elementAt(start);
			MagnetCell to = stack.elementAt(start+1);
			if((from!=to) 
				&& !isNewlyPromoted(to)
				&& to.chipAtIndex(0).canPromote())
						{ if(all==null) { return(true); }
						  all.push(new Magnetmovespec(MOVE_PROMOTE,to,who));
						  some = true;
						}
			}
 	return(some);
 }
 
 public boolean hasPromotionMoves(int who)
 {
	 return(addPromotionMoves(null,who));
 }
 
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();

 	switch(board_state)
 	{
 	case Puzzle:	
 	default: throw G.Error("Not expecting robot in state %s",board_state);
 	
 	case NormalStart:
 		all.push(new Magnetmovespec(NORMALSTART,whoseTurn));
 		break;
 	case Setup:
 	case Synchronous_Setup:
 	case WaitForStart:
 		// all possible setup moves
 		addSetupMoves(all,whoseTurn);
 		if(all.size()==0) { all.push(new Magnetmovespec(MOVE_DONE,whoseTurn)); }
 		break;
 	case Play:
 		addMagnetMoves(all,whoseTurn);
 		break;
 	case FirstSelect:
 		addFirstSelectMoves(all,whoseTurn);
 		break;
 	case Select:
 		addSelectMoves(all,whoseTurn);
 		break;
 	case Promote:
 		addPromotionMoves(all,whoseTurn);
		//$FALL-THROUGH$
	case Confirm:
	case Resign:
 	case Confirm_Synchronous_Setup:
 	case Confirm_Setup:
 		all.push(new Magnetmovespec(MOVE_DONE,whoseTurn));
 		break;
 	}
 	G.Assert(all.size()>0,"no moves");
 	return(all);
 }
 


private CellStack replacable = new CellStack();
private ChipStack replacements = new ChipStack();
public void randomizeHiddenState(Random robotRandom, int robotPlayer) 
{	replacements.clear();
	replacable.clear();
	//G.print("Randomize");
	int siz = 0;
    checkKings();
	StringBuilder msg = new StringBuilder();
	for(MagnetCell c = allCells; c!=null; c=c.next)
	{	if(!c.isEmpty())
		{ MagnetChip top = c.chipAtIndex(0);
		  if((top!=MagnetChip.magnet)
			  && (top.playerIndex()!=robotPlayer)
			  && (top.upFace()<4))
		  {	replacable.push(c);
			replacements.push(top);
			siz++;
		  }
		}
	}
	replacements.shuffle(robotRandom);
	while(siz-- > 0)
	{
		MagnetCell rep = replacable.pop();
		MagnetChip oldChip = rep.chipAtIndex(0);
		int oldFace = oldChip.upFace();
		boolean found = false;
		for(int idx = siz; !found && idx>=0; idx--)
		{
			MagnetChip newChip = replacements.elementAt(idx);
			if(newChip.upFace()==oldFace)
			{
				rep.setChipAtIndex(0,newChip);
				if(newChip.isKing()) { kingLocation[newChip.playerIndex()] = rep; }
				replacements.remove(idx, false);
				found = true;
				msg.append("\nReplace "+oldChip+" = "+rep);

			}
		}
		G.Assert(found,"replacement not found for %s after%s",rep,msg);
	}
    checkKings();
    
}

}
