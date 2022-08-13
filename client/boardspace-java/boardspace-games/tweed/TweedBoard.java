package tweed;


import static tweed.TweedMovespec.*;

import java.awt.Color;
import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;

/**
 * TumbleBoard knows all about the game of TumbleWeed, which is played
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

class TweedBoard 
	extends hexBoard<TweedCell>	// for a square grid board, this could be rectBoard or squareBoard 
	implements BoardProtocol,TweedConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
	static final String[] GRIDSTYLE = { null, "1", "A1" }; // left and bottom numbers
	TweedVariation variation = TweedVariation.tumbleweed_6;
	private TweedState board_state = TweedState.Puzzle;	
	private TweedState unresign = null;	// remembers the orignal state when "resign" is hit
	public TweedState getState() { return(board_state); }
	public boolean robotBoard = false;
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(TweedState st) 
	{ 	unresign = (st==TweedState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	TweedCell neutralStack = null;
	
    private TweedId playerColor[]={TweedId.Red,TweedId.White};    
    private TweedChip playerChip[]={TweedChip.White,TweedChip.White};
    private TweedCell playerCell[]=new TweedCell[2];
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
    public int getPlayerIndex(TweedChip chip) { return (chip==playerChip[0] ? 0 : 1); }
	public TweedChip getPlayerChip(int p) { return(playerChip[p]); }
	public TweedId getPlayerColor(int p) { return(playerColor[p]); }
	public TweedCell getPlayerCell(int p) { return(playerCell[p]); }
	public TweedCell getPlayerCell(TweedChip ch)
	{	
		for(TweedCell c : playerCell) { if(c.topChip()==ch) { return c; }}
		if(ch==TweedChip.Gray) { return neutralStack; }
		return null;
	}
	public TweedChip getCurrentPlayerChip() { return(playerChip[whoseTurn]); }

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
    private int chips_on_board = 0;			// number of chips currently on the board
    private boolean countsValid = false;
    private boolean swapped = false;
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public TweedChip pickedObject = null;
    private TweedChip droppedOnColor = null;
    private int droppedOnHeight = 0;
    private boolean lastIsPass = false;
    private int fullSize = 0;
    public TweedChip lastPicked = null;
    private TweedCell whiteChipPool = null;	// dummy source for the chip pools
    private TweedCell redChipPool = null;
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

    private CellStack occupiedCells[] = { new CellStack(),new CellStack()};
    private TweedState resetState = TweedState.Puzzle; 
    public DrawableImage<?> lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public TweedCell newcell(char c,int r)
	{	return(new TweedCell(TweedId.BoardLocation,c,r));
	}
	
	// constructor 
    public TweedBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = GRIDSTYLE;
        setColorMap(map);
        
		Random r = new Random(734687);
		// do this once at construction
	    whiteChipPool = new TweedCell(r,TweedId.White);
	    whiteChipPool.addChip(TweedChip.White);
	    redChipPool = new TweedCell(r,TweedId.Red);
	    redChipPool.addChip(TweedChip.Red);
	    neutralStack = new TweedCell(r,TweedId.Neutral);
	    neutralStack.addChip(TweedChip.Gray);
	    neutralStack.setCurrentCenter(0,0);
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
 		setState(TweedState.Puzzle);
		variation = TweedVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		gametype = gtype;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case tumbleweed_6:
		case tumbleweed_8:
		case tumbleweed_10:
		case tumbleweed_11:
			// using reInitBoard avoids thrashing the creation of cells 
			// when reviewing games.
			reInitBoard(variation.firstInCol,variation.ZinCol,null);
			}
		{
 		TweedCell cc = getCell((char)('A'+ncols/2),nrows/2);
 		cc.addChip(TweedChip.Gray);
 		cc.addChip(TweedChip.Gray);
		}
	    playerCell[FIRST_PLAYER_INDEX] = whiteChipPool; 
	    playerCell[SECOND_PLAYER_INDEX] = redChipPool; 
	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    chips_on_board = 0;
	    fullSize = 0;
	    countsValid = false;
	    droppedDestStack.clear();
	    pickedSourceStack.clear();
	    stateStack.clear();
	    
	    pickedObject = null;
	    droppedOnColor = null;
	    droppedOnHeight = 0;
	    lastIsPass = false;
	    resetState = null;
	    lastDroppedObject = null;
	    int map[]=getColorMap();
		playerColor[map[0]]=TweedId.White;
		playerColor[map[1]]=TweedId.Red;
		playerChip[map[0]]=TweedChip.White;
		playerChip[map[1]]=TweedChip.Red;
	    // set the initial contents of the board to all empty cells
		reInit(occupiedCells);
	    
        animationStack.clear();
        swapped = false;
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }

    /** create a copy of this board */
    public TweedBoard cloneBoard() 
	{ TweedBoard dup = new TweedBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((TweedBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(TweedBoard from_b)
    {
        super.copyFrom(from_b);
        chips_on_board = from_b.chips_on_board;
        countsValid = from_b.countsValid;
        getCell(occupiedCells,from_b.occupiedCells);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        copyFrom(redChipPool,from_b.redChipPool);		// this will have the side effect of copying the location
        copyFrom(whiteChipPool,from_b.whiteChipPool);		// from display copy boards to the main board
        getCell(playerCell,from_b.playerCell);
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        droppedOnColor = from_b.droppedOnColor;
        droppedOnHeight = from_b.droppedOnHeight;
        lastIsPass = from_b.lastIsPass;
        resetState = from_b.resetState;
        lastPicked = null;
        robotBoard = from_b.robotBoard;
        AR.copy(playerColor,from_b.playerColor);
        AR.copy(playerChip,from_b.playerChip);
    }

    

    public void sameboard(BoardProtocol f) { sameboard((TweedBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(TweedBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor mismatch");
        G.Assert(AR.sameArrayContents(playerChip,from_b.playerChip),"playerChip mismatch");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(droppedOnColor==from_b.droppedOnColor, "droppedOnColor mismatch");
        G.Assert(droppedOnHeight==from_b.droppedOnHeight, "droppedOnHeight mismatch");
        G.Assert(lastIsPass==from_b.lastIsPass, "lastIsPass mismatch");
        G.Assert(chips_on_board == from_b.chips_on_board,"chips_on_board mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedsourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(sameCells(playerCell,from_b.playerCell),"player cell mismatch");
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
		v ^= chip.Digest(r,droppedOnColor);
		v ^= Digest(r,droppedOnHeight);
		v ^= Digest(r,lastIsPass);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
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
        case PlayOrSwap:
        	// some damaged games have 2 dones in a row
        	if(replay==replayMode.Live) { throw G.Error("Move not complete, can't change the current player in state ",board_state); }
			//$FALL-THROUGH$
		case ConfirmSwap:
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
    public boolean winForPlayerNow(int player)
    {	if(win[player]) { return(true); }
    	boolean win = false;
    	return(win);
    }
    private void changeStackColor(TweedCell c,TweedChip chip)
    {
		int n = c.height();
		if(n>0)
			{
	    	if(chip!=c.topChip())
	    	{
				for(int i=0;i<n;i++) { c.removeTop(); }
				for(int i=0;i<n;i++) { c.addChip(chip); }	// change color of existing    		
	    	}}
    }

    // set the contents of a cell, and maintain the books
    public TweedChip SetBoard(TweedCell c,TweedChip ch)
    {	
    	G.Assert(chips_on_board==(occupiedCells[0].size()+occupiedCells[1].size()),"cells missing in");
    	countsValid = false;
    	if(c.onBoard)
    	{
    	TweedChip old = c.topChip();
		
    	if(ch==null)	// setting to empty
    	{	c.removeTop();
    		if(droppedOnColor!=null) { changeStackColor(c,droppedOnColor); }
    		if(old!=null && (old!=TweedChip.Gray) && (c.topChip()==null)) 
    			{int index = getPlayerIndex(old); 
    			 chips_on_board--; 
    			 occupiedCells[index].remove(c,false);  
    			 }
     	}
    	else 
    	{
    		if(old!=ch)
    		{
    			// changing color
    			droppedOnColor = old;
    			if(old!=null)
    			{
    			changeStackColor(c,ch);
    			if(old!=TweedChip.Gray) {
    				int index = getPlayerIndex(old); 
    				chips_on_board--;
    				G.Assert(occupiedCells[index].remove(c,false)!=null,"nothing removed");
    				}}
    			int ind = getPlayerIndex(ch);
    			chips_on_board++;
    			occupiedCells[ind].push(c);
    			}		
    		c.addChip(ch);
    	}
    	G.Assert(chips_on_board==(occupiedCells[0].size()+occupiedCells[1].size()),"cells missing out");
    	return old;
    	}
    	G.Assert(chips_on_board==(occupiedCells[0].size()+occupiedCells[1].size()),"cells missing out 2");
    	return c.topChip();
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
        droppedOnColor = null;
        droppedOnHeight = 0;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private TweedCell unDropObject()
    {	TweedCell rv = droppedDestStack.pop();
    	int dh = droppedOnHeight;
    	setState(stateStack.pop());
    	pickedObject = SetBoard(rv,null); 	// SetBoard does ancillary bookkeeping
    	while(rv.height()>dh) { SetBoard(rv,null); }
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	TweedCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	SetBoard(rv,pickedObject);
    	pickedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(TweedCell c,int n)
    {
       droppedDestStack.push(c);
       stateStack.push(board_state);
       
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting dest " + c.rackLocation);
        case White:
        case Red:		// back in the pool, we don't really care where
        	pickedObject = null;
            break;
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        case EmptyBoard:
        	TweedChip po = pickedObject;
        	int ton = droppedOnHeight = c.height();
           	do { SetBoard(c,po);
           		 ton++;
           	} while (ton<n);
           	
            pickedObject = null;
            break;
        }
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(TweedCell c)
    {	return(droppedDestStack.top()==c);
    }
    public TweedCell getDest()
    {	return(droppedDestStack.top());
    }
 
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { TweedChip ch = pickedObject;
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
    private TweedCell getCell(TweedId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source " + source);
        case BoardLocation:
        	return(getCell(col,row));
        case Neutral:
        	return neutralStack;

        case White:
        	return(whiteChipPool);
        case Red:
        	return(redChipPool);
        } 	
    }
    /**
     * this is called when copying boards, to get the cell on the new (ie current) board
     * that corresponds to a cell on the old board.
     */
    public TweedCell getCell(TweedCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(TweedCell c)
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
        case White:
        case Red:
        	lastPicked = pickedObject = c.topChip();
        }
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(TweedCell c)
    {	return(c==pickedSourceStack.top());
    }
    public TweedCell getSource()
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
        case PlacePie:
        	setState(chips_on_board==2 ? TweedState.Confirm : TweedState.PlacePie);
        	break;
        case Confirm:
        	setNextStateAfterDone(replay);
         	break;
        case Play:
        case PlayOrEnd:
        case PlayOrSwap:
			setState(TweedState.Confirm);
			break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    private void setNextStateAfterDone(replayMode replay)
    {	G.Assert(chips_on_board==(occupiedCells[0].size()+occupiedCells[1].size()),"cells missing");
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state "+board_state);
    	case Gameover: break;
    	case ConfirmSwap: 
    		setState(TweedState.Play); 
    		break;
    	case Confirm:
    	case Puzzle:
    	case Play:
    	case PlayOrSwap:
    		if(lastIsPass) 
    			{ if(resetState==TweedState.PlayOrEnd) 
    					{ setState(TweedState.Gameover); 
    					  int sc0 = simpleScore(0);
    					  int sc1 = simpleScore(1);
    					  win[0] = sc0>sc1;
    					  win[1] = sc1>sc0;
    					}
    				else { setState(TweedState.PlayOrEnd); lastIsPass = false; } 
    			}
    		else if(chips_on_board<2) { setState(TweedState.PlacePie); }
    		else
    		{
    		setState((chips_on_board<2) ? TweedState.PlacePie
    				: ((chips_on_board==2)&&!swapped) 
    				? TweedState.PlayOrSwap
    				: TweedState.Play);
    		}
    		break;
    	}
       	resetState = board_state;
    }
    private void doDone(replayMode replay)
    {
        acceptPlacement();

        if (board_state==TweedState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(TweedState.Gameover);
        }
        else
        {	if(winForPlayerNow(whoseTurn)) 
        		{ win[whoseTurn]=true;
        		  setState(TweedState.Gameover); 
        		}
        	else {setNextPlayer(replay);
        		setNextStateAfterDone(replay);
        	}
        }
    }
void doSwap(replayMode replay)
{	TweedId c = playerColor[0];
	TweedChip ch = playerChip[0];
	playerColor[0]=playerColor[1];
	playerChip[0]=playerChip[1];
	playerColor[1]=c;
	playerChip[1]=ch;
	TweedCell cc = playerCell[0];
	playerCell[0]=playerCell[1];
	playerCell[1]=cc;
	CellStack cs = occupiedCells[0];
	occupiedCells[0] = occupiedCells[1];
	occupiedCells[1] = cs;
	swapped = !swapped;
	countsValid = false;
	switch(board_state)
	{	
	default: 
		throw G.Error("Not expecting swap state "+board_state);
	case Play:
		// some damaged game records have double swap
		if(replay==replayMode.Live) { G.Error("Not expecting swap state "+board_state); }
		//$FALL-THROUGH$
	case PlayOrSwap:
		  setState(TweedState.ConfirmSwap);
		  break;
	case ConfirmSwap:
		  setState(TweedState.PlayOrSwap);
		  break;
	case Gameover:
	case Puzzle: break;
	}
	}

public boolean Execute(commonMove mm,replayMode replay)
{	TweedMovespec m = (TweedMovespec)mm;
    if(replay!=replayMode.Replay) { animationStack.clear(); }

    //G.print("E "+m+" for "+whoseTurn+" "+state);
    switch (m.op)
    {
    case MOVE_PASS:
    	lastIsPass = !lastIsPass;
    	setState(lastIsPass ? TweedState.Confirm : TweedState.Play);
    	
    	break;
	case MOVE_SWAP:	// swap colors with the other player
		doSwap(replay);
		break;
    case MOVE_DONE:

     	doDone(replay);

        break;

    case MOVE_DROPB:
    	{
		TweedChip po = pickedObject;
		if(po==null) 
			{ if((board_state==TweedState.PlacePie) && (chips_on_board==1)) { pickedObject = getPlayerChip(nextPlayer[whoseTurn]); }
				else { pickedObject = getPlayerChip(whoseTurn); } 
			}
		TweedCell dest =  getCell(TweedId.BoardLocation,m.to_col,m.to_row);
		
		if(isSource(dest)) 
			{ unPickObject(); 
			}
			else 
			{
			m.chip = pickedObject;
			TweedCell picked = getPlayerCell(pickedObject);
			int originalHeight = dest.height();
			TweedChip originalTop = dest.topChip();
			TweedChip newTop = pickedObject;
            dropObject(dest,m.to_height);
            /**
             * if the user clicked on a board space without picking anything up,
             * animate a stone moving in from the pool.  For Hex, the "picks" are
             * removed from the game record, so there are never picked stones in
             * single step replays.
             */
            if(replay!=replayMode.Replay)
            {
            	int nadd = dest.height()-originalHeight;
            	if(originalTop!=newTop)
            	{	nadd += originalHeight;
            		for(int i=0;i<originalHeight;i++)
            		{
            			animationStack.push(dest);
            			animationStack.push(getPlayerCell(originalTop));
            		}
            	}
            	while(nadd>1)
            	{	nadd--;
            		animationStack.push(picked);
            		animationStack.push(dest); 
            	}
            }
            if(replay!=replayMode.Replay && (po==null))
            	{ animationStack.push(picked);
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
			TweedCell src = getCell(m.source,m.to_col,m.to_row);
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
    		setState(((chips_on_board==1) && !swapped) ? TweedState.PlayOrSwap : TweedState.Play);
    		break;
    	default: ;
    	}}}
        break;

    case MOVE_DROP: // drop on chip pool;
    	if(pickedObject!=null)
    	{
        TweedCell dest = getCell(m.source,m.to_col,m.to_row);
        if(isSource(dest)) { unPickObject(); }
        else 
        	{
	        if(replay==replayMode.Live)
        	{ lastDroppedObject = pickedObject.getAltDisplayChip(dest);
        	  //G.print("last ",lastDroppedObject); 
        	}      	
        	dropObject(dest,m.to_height);   
        	}
    	}
        break;

    case MOVE_START:
        setWhoseTurn(m.player);
        acceptPlacement();
        int nextp = nextPlayer[whoseTurn];
        // standardize the gameover state.  Particularly importing if the
        // sequence in a game is resign/start
        setState(TweedState.Puzzle);	// standardize the current state
        if((win[whoseTurn]=winForPlayerNow(whoseTurn))
           ||(win[nextp]=winForPlayerNow(nextp)))
           	{ setState(TweedState.Gameover); 
           	}
        else {  setNextStateAfterDone(replay); }

        break;

   case MOVE_RESIGN:
	   	setState(unresign==null?TweedState.Resign:unresign);
        break;
   case MOVE_EDIT:
    	acceptPlacement();
        setWhoseTurn(FIRST_PLAYER_INDEX);
        setState(TweedState.Puzzle);

        break;

   case MOVE_GAMEOVERONTIME:
	   win[whoseTurn] = true;
	   setState(TweedState.Gameover);
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
        case PlacePie:
        	return (pickedObject==null)  
        				? (occupiedCells[player].size()==0)
        				: player == getPlayerIndex(pickedObject);
        				
        case PlayOrSwap:
        case PlayOrEnd:
        case Play:
        	// for pushfight, you can pick up a stone in the storage area
        	// but it's really optional
        	return(player==whoseTurn);
        case Confirm:
		case ConfirmSwap:
		case Resign:
		case Gameover:
			return(false);
        case Puzzle:
            return ((pickedObject!=null)?(pickedObject==playerChip[player]):true);
        }
    }

    public boolean legalToHitBoard(TweedCell c,Hashtable<TweedCell,TweedMovespec> targets )
    {	if(c==null) { return(false); }
        switch (board_state)
        {
        case Puzzle:
		case Play:
		case PlayOrSwap:
		case PlayOrEnd:
		case PlacePie:
			return(targets.get(c)!=null || isDest(c) || isSource(c));
		case ConfirmSwap:
		case Gameover:
		case Resign:
			return(false);
		case Confirm:
			return(isDest(c));
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
    public void RobotExecute(TweedMovespec m)
    {
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
    public void UnExecute(TweedMovespec m)
    {
    	G.Error("Not expected");
 }
  
 private void addPlaceAnywhere(CommonMoveStack all,TweedId ch,int who)
 {
	 for(TweedCell c = allCells; c!=null; c=c.next)
	 {
		 if(c.topChip()==null) { all.push(new TweedMovespec(MOVE_DROPB,c,ch,1,who)); }
	 }
 }
 
 
private void addPlaceLineMoves(CommonMoveStack all,CellStack fromAll,int who)
{	
	// remember the number of encounters with each cell
	Hashtable<TweedCell,Integer>toAll = new Hashtable<TweedCell,Integer>();
	Hashtable<TweedCell,TweedMovespec>toMoves = new Hashtable<TweedCell,TweedMovespec>();
	TweedId id = getPlayerColor(who);
	for(int lim = fromAll.size()-1; lim>=0; lim--)
	{
		TweedCell from = fromAll.elementAt(lim);
		//G.Assert(from.topChip()==chip,"should be same color");
		for(int direction = from.geometry.n-1; direction>=0; direction--)
		{
			TweedCell to = from;
			while( (to=to.exitTo(direction))!=null)
			{	Integer countn = toAll.get(to);
				int count = countn==null ? 0 : countn;
				int toheight = to.height();
				toAll.put(to,count+1);
				
				if(toheight==count)
				{	
				TweedMovespec m = new TweedMovespec(MOVE_DROPB,to,id,count+1,who);
				all.push(m);
				toMoves.put(to,m);
				}
				else if(count>toheight)
				{	// more than necessary
					TweedMovespec m = toMoves.get(to);
					m.to_height = count+1;
				}
				if(toheight>0) { break; }
			}
		}
	}
	
}
public commonMove getRandomMove(Random r)
{	switch(board_state)
	{
	default: break;
	case Play:
		return getRandomMove(r,whoseTurn);
	}
	return null;
}
public commonMove getRandomMove(Random r,int who)
{
	countScore();
	cell<TweedCell>[] cells = getCellArray();
	int n = cells.length;
	int first = r.nextInt(n);
	TweedId id = getPlayerColor(who);
	TweedChip myChip = getPlayerChip(who);
	for(int idx = 0;idx<n;idx++)
	{
		TweedCell c = (TweedCell)cells[(first+idx) % n];
		int mysee = c.getSeen(who);
		int youSee = c.getSeen(nextPlayer[who]);
		if(canPlaceAt(c,mysee,youSee,myChip,who))
		{
			return new TweedMovespec(MOVE_DROPB,c,id,mysee,who);
		}
	}
	return null;
}
private boolean canPlaceAt(TweedCell to,int mySee,int youSee,TweedChip myChip,int who)
{	if(mySee>0 && (mySee>=youSee))
	{
	int height = to.height();
	if( mySee>height)
	{
		TweedChip top = to.topChip();
		if((top==myChip)
					? youSee>=mySee
					: (top!=null || (youSee>0) || (mySee<3)))
		{ 
			return true; 
		}
	}}
	return false;
}

private boolean addPlaceControlLineMoves(CommonMoveStack all,CellStack fromAll,int who,boolean greedy)
{	
	countScore();
	boolean use = addPlaceControlLineMovesB(all,fromAll,who,greedy);
	//lctest(all,fromAll,who);
	return use;
}
@SuppressWarnings("unused")
private void lctest(CommonMoveStack all,CellStack fromAll,int who)
{
	// test
	CommonMoveStack all1 = new CommonMoveStack();
	CommonMoveStack all2 = new CommonMoveStack();
	long now = G.nanoTime();
	boolean use1 = addPlaceControlLineMovesB(all1,fromAll,who,false);
	long later = G.nanoTime();
	boolean use2 = addPlaceControlLineMovesA(all2,fromAll,who);
	long last = G.nanoTime();
	G.print("new ",(later-now)," old ",(last-later));
	G.Assert(use2==use1 && all1.size()==all2.size(),"mismatch");
}
private boolean addPlaceControlLineMovesB(CommonMoveStack all,CellStack fromAll,int who,boolean greedy)
{	
	cell<TweedCell>[] cells = getCellArray();
	int n = cells.length;
	TweedId id = getPlayerColor(who);
	TweedChip myChip = getPlayerChip(who);
	int useFul = 0;
	boolean anyCapture = false;
	for(int idx = 0;idx<n;idx++)
	{
		boolean capture = false;
		TweedCell c = (TweedCell)cells[idx];
		int mySee = c.getSeen(who);
		int youSee = c.getSeen(nextPlayer[who]);

		if(canPlaceAt(c,mySee,youSee,myChip,who))
		{
			TweedChip top = c.topChip();
			if((top!=null && top!=myChip)
					|| (top==myChip && youSee==mySee)) 
				{ useFul++; 
				  capture = true; 
				  if(greedy && !anyCapture) 
				  	{ all.setSize(0); 
				  	  anyCapture = true; 
				  	}
				}
			if(top==null && youSee>=mySee) 
				{ useFul++; 
				}
			if(!greedy || !anyCapture || capture)
				{ all.push(new TweedMovespec(MOVE_DROPB,c,id,c.getSeen(who),who));
				}
		}
	}
	return useFul>0;
}

private boolean addPlaceControlLineMovesA(CommonMoveStack all,CellStack fromAll,int who)
{	
	boolean useFul = false;
	// remember the number of encounters with each cell
	Hashtable<TweedCell,TweedMovespec>toMoves = new Hashtable<TweedCell,TweedMovespec>();
	TweedId id = getPlayerColor(who);
	TweedChip myChip = getPlayerChip(who);
	int nextP = nextPlayer[who];
	for(int lim = fromAll.size()-1; lim>=0; lim--)
	{
		TweedCell from = fromAll.elementAt(lim);
		//G.Assert(from.topChip()==chip,"should be same color");
		for(int direction = from.geometry.n-1; direction>=0; direction--)
		{
			TweedCell to = from;
			while( (to=to.exitTo(direction))!=null)
			{	int mysee = to.getSeen(who);
				int youSee = to.getSeen(nextP);
				int height = to.height();
				if(mysee>=youSee && mysee>height)
				{
					TweedMovespec mp = toMoves.get(to);
					if(mp==null)
					{	TweedChip top = to.topChip();
						if((top==myChip)
								? youSee>mysee
								: (top!=null || (youSee>0) || (mysee<3)))
						{
						TweedMovespec m = new TweedMovespec(MOVE_DROPB,to,id,mysee,who);
						toMoves.put(to,m);
						all.push(m);
						
						if(top!=null && top!=myChip) 
							{ useFul = true; }
						if(top==null && youSee>=mysee) 
							{ useFul = true; }}
					}
				}
				if(to.height()>0) { break; }
			}
		}
	}
	return useFul;
}
 CommonMoveStack  GetListOfMoves(boolean greedy)
 {	CommonMoveStack all = new CommonMoveStack();
 	if(board_state==TweedState.PlayOrSwap)
 	{
 		all.addElement(new TweedMovespec(MOVE_SWAP,whoseTurn));
 	}
 	switch(board_state)
 	{
 	case PlacePie:
 		if(occupiedCells[0].size()==0) { addPlaceAnywhere(all,getPlayerCell(0).rackLocation(),whoseTurn); }
 		if(occupiedCells[1].size()==0) { addPlaceAnywhere(all,getPlayerCell(1).rackLocation(),whoseTurn); }
 		break;
 		
 	case Puzzle:
 		{	if(pickedObject==null)
 			{
 			for(TweedCell c = allCells;
 			 	    c!=null;
 			 	    c = c.next)
 			 	{	if(c.topChip()!=null)
 			 		{all.addElement(new TweedMovespec(MOVE_PICKB,c.col,c.row,whoseTurn));
 			 		}
 			 	}
 			}
	 		else
	 		{
	 			for(TweedCell c = allCells;
	 			 	    c!=null;
	 			 	    c = c.next)
	 			 	{	TweedChip top = c.topChip();
	 			 		if((top==null) || (top==pickedObject))
	 			 		{all.addElement(new TweedMovespec(MOVE_DROPB,c.col,c.row,whoseTurn));
	 			 		}
	 			 	}
	 		}
 		}
 		break;
 	case PlayOrEnd:
 		if(simpleScore(whoseTurn)>simpleScore(nextPlayer[whoseTurn])) 
 		{
 			all.push(new TweedMovespec(MOVE_PASS,whoseTurn)); 
 			break;
 		}
		//$FALL-THROUGH$
	case Play:
 	case PlayOrSwap:
 		boolean useful = false;
 		if(robotBoard) 
 			{ useful = addPlaceControlLineMoves(all,occupiedCells[whoseTurn],whoseTurn, greedy); }
 		else 
 			{ addPlaceLineMoves(all,occupiedCells[whoseTurn],whoseTurn); }
 		switch(board_state)
 		{
 		default:
 		case PlayOrSwap:	break;
 		case Play: if(useful 
 						&& (all.size()>0)
 						&& robotBoard )
 							{ break; }
			//$FALL-THROUGH$
		case PlayOrEnd: all.push(new TweedMovespec(MOVE_PASS,whoseTurn)); 
 		}
 		break;
 	case Confirm:
 	case ConfirmSwap:
 		all.push(new TweedMovespec(MOVE_DONE,whoseTurn));
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
	 robotBoard = true;
 }

 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(Character.isDigit(txt.charAt(0)))
	 	{ switch(variation)
	 		{
	 		case tumbleweed_6:
	 		case tumbleweed_8:
	 		case tumbleweed_10:
	 		case tumbleweed_11:
	 			xpos -= cellsize/6;
	 			ypos -= cellsize/6;
	 			break;
 			default: G.Error("case "+variation+" not handled");
	 		}
	 	}
 		else
 		{ 
 		  ypos += cellsize/5;
 		}
 	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
 }
 /**
  *  get the board cells that are valid targets right now
  * @return
  */
 public Hashtable<TweedCell, TweedMovespec> getTargets() 
 {
 	Hashtable<TweedCell,TweedMovespec> targets = new Hashtable<TweedCell,TweedMovespec>();
 	CommonMoveStack all = GetListOfMoves(false);
 	for(int lim=all.size()-1; lim>=0; lim--)
 	{	TweedMovespec m = (TweedMovespec)all.elementAt(lim);
 		switch(m.op)
 		{
 		case MOVE_PICKB:
 		case MOVE_DROPB:
 			targets.put(getCell(m.to_col,m.to_row),m);
 			break;
 		case MOVE_SWAP:
 		case MOVE_DONE:
 		case MOVE_PASS:
 			break;

 		default: G.Error("Not expecting "+m);
 		
 		}
 	}
 	
 	return(targets);
 }
 public int simpleScore(int forplayer)
 {	int score = 0;
	 countScore();
	 TweedChip my = getPlayerChip(forplayer);
	 int op = nextPlayer[forplayer];
	 for(TweedCell c = allCells; c!=null; c=c.next)
	 {
		 TweedChip top = c.topChip();
		 if(top==null) 
		 { 
			 if(c.getSeen(forplayer)>c.getSeen(op)) { score++; }
		 }
		 else if(top==my)
		 {
			 score++;
		 }
	 }
	 return score;
 }
 private void countScore()
 {	if(!countsValid)
	 {
	 fullSize = 0;
	 for(TweedCell c = allCells; c !=null; c=c.next) { c.clearSeen(); fullSize++; }
	 incrementSeen(occupiedCells[0],0);
 	 incrementSeen(occupiedCells[1],1);
 	 countsValid = true;
	 }
 }
 public boolean passIsPossible()
 {
	 countScore();
	 return (simpleScore(0)+simpleScore(1)>fullSize*0.9);
 }
 private void incrementSeen(CellStack origin,int index)
 {
	 for(int lim=origin.size()-1; lim>=0; lim--)
	 {
		 TweedCell c = origin.elementAt(lim);
		 for(int direction = c.geometry.n-1; direction>=0; direction--)
		 {
			 TweedCell from = c;
			 while ((from=from.exitTo(direction))!=null)
			 {
				 from.incrementSeen(index);
				 if(from.height()>0) { break; }
			 }
		 }
	 }
 }
 // most multi player games can't handle individual players resigning
 // this provides an escape hatch to allow it.
 //public boolean canResign() { return(super.canResign()); }
}
