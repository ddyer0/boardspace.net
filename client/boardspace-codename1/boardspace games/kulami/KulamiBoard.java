package kulami;

import static kulami.Kulamimovespec.*;

import java.util.*;
import lib.*;
import lib.Random;
import online.game.*;

/**
 * KulamiBoard knows all about the game of Kulami, which is played
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
class SubBoard implements Digestable
{
	int w,h;
	int rotation = 0;
	int nDistinctRotations = 0;
	long randomV=0;
	int ordinal = -1;
	KulamiCell location;
	KulamiChip chip;
	KulamiChip altChip;
	SubBoard() { }	// dummy board
	SubBoard(int ord,int across,int down,KulamiChip mainChip,KulamiChip alt,int nr)
	{	ordinal = ord;
		w = across;
		h = down;
		randomV = new Random(w*h*12423525).nextLong();
		nDistinctRotations = nr;
		chip = mainChip;
		altChip = alt;
	}
	
	public SubBoard clone()
	{
		SubBoard n = new SubBoard(ordinal,w,h,chip,altChip,nDistinctRotations);
		n.location = location;
		n.rotation = rotation;
		return(n);
	}
	public SubBoard copyFrom(SubBoard other,KulamiBoard b)
	{	ordinal = other.ordinal;
		w = other.w;
		h = other.h;
		nDistinctRotations = other.nDistinctRotations;
		chip = other.chip;
		altChip = other.altChip;
		location = b.getCell(other.location);
		rotation = other.rotation;
		randomV = other.randomV;
		return(this);
	}
	
	public String toString()
	{	return("<sb "+w+"x"+h+"@"+((location==null)?" ":(""+location.col+location.row))+" "+randomV);
	}
	
	public void setRotation(int n)
	{
		if(rotation!=n)
		{
			rotation = n;
			int ww = h;
			h = w;
			w = ww;
			KulamiChip c = chip;
			chip = altChip;
			altChip = c;
		}
	}
	public void reInit()
	{	setRotation(0);
		location = null;
	}
	public long Digest() {
		return randomV;
	}
	public long Digest(Random r) {
		return randomV^r.nextLong();
	}
	
}
class KulamiBoard extends squareBoard<KulamiCell> implements BoardProtocol,KulamiConstants
{	static int REVISION = 102;			// 100 represents the initial version of the game
										// revision 101 changes the startup to use a set of 1000
										// revision 102 limits the 
	public int getMaxRevisionLevel() { return(REVISION); }
    static final String[] KulamiGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

	KulamiVariation variation = KulamiVariation.Kulami;
	private KulamiState board_state = KulamiState.Puzzle;	
	private KulamiState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	public KulamiState getState() { return(board_state); }
	
	
	public SubBoard[] subBoards = { 
			new SubBoard(0,3,2,KulamiChip.board_3x2,KulamiChip.board_2x3,2),
			new SubBoard(1,3,2,KulamiChip.board_3x2,KulamiChip.board_2x3,2),
			new SubBoard(2,3,2,KulamiChip.board_3x2,KulamiChip.board_2x3,2),
			new SubBoard(3,3,2,KulamiChip.board_3x2,KulamiChip.board_2x3,2),
			
			// 5 2x2 boards.  There's only one real rotation of a 2x2 board,
			// but we retain 2 separate tiles for visual variety.
			new SubBoard(4,2,2,KulamiChip.board_2x2H,KulamiChip.board_2x2V,1),
			new SubBoard(5,2,2,KulamiChip.board_2x2H,KulamiChip.board_2x2V,1),
			new SubBoard(6,2,2,KulamiChip.board_2x2H,KulamiChip.board_2x2V,1),
			new SubBoard(7,2,2,KulamiChip.board_2x2H,KulamiChip.board_2x2V,1),
			new SubBoard(8,2,2,KulamiChip.board_2x2H,KulamiChip.board_2x2V,1),
			
			new SubBoard(9,3,1,KulamiChip.board_3x1,KulamiChip.board_1x3,2),
			new SubBoard(10,3,1,KulamiChip.board_3x1,KulamiChip.board_1x3,2),
			new SubBoard(11,3,1,KulamiChip.board_3x1,KulamiChip.board_1x3,2),
			new SubBoard(12,3,1,KulamiChip.board_3x1,KulamiChip.board_1x3,2),
			
			new SubBoard(13,2,1,KulamiChip.board_2x1,KulamiChip.board_1x2,2),
			new SubBoard(14,2,1,KulamiChip.board_2x1,KulamiChip.board_1x2,2),
			new SubBoard(15,2,1,KulamiChip.board_2x1,KulamiChip.board_1x2,2),
			new SubBoard(16,2,1,KulamiChip.board_2x1,KulamiChip.board_1x2,2),	
		};
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(KulamiState st) 
	{ 	unresign = (st==KulamiState.Resign)?board_state:null;
		board_state = st;
		AR.setValue(win,false); 	// make sure "win" is cleared
		if(board_state.GameOver())
		{
			int score0 = scoreForPlayer(0);
			int score1 = scoreForPlayer(1);
			win[0] = score0>score1;
			win[1] = score1>score0;
		}
	}
	public int scoreForPlayer(int pl)
	{	int n = 0;
		KulamiChip color = playerChip[pl];
		for(SubBoard sb : subBoards)
		{	KulamiCell loc0 = sb.location;
			int match = 0;
			int nomatch = 0;
			for(int row = 0;row<sb.h; row++)
			{
				KulamiCell loc = loc0;
				for(int col = 0;col<sb.w;col++)
				{
					KulamiChip ch = loc.topChip();
					if(ch==null) {}
					else if(ch==color) { match++; }
					else { nomatch++; }
					loc = loc.exitTo(CELL_RIGHT);
				}
				loc0 = loc0.exitTo(CELL_DOWN);
			}
			if(match>nomatch) { n+= sb.w*sb.h; }
		}
		return(n);
	}
    private KulamiId playerColor[]={KulamiId.Red_Chip_Pool,KulamiId.Black_Chip_Pool};    
    private KulamiChip playerChip[]={KulamiChip.Red,KulamiChip.Black};
    private KulamiCell playerCell[]=new KulamiCell[2];
    int playerNChips[] = new int[2];
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public KulamiChip getPlayerChip(int p) { return(playerChip[p]); }
	public KulamiId getPlayerColor(int p) { return(playerColor[p]); }
	public KulamiCell getPlayerCell(int p) { return(playerCell[p]); }
	public KulamiChip getCurrentPlayerChip() { return(playerChip[whoseTurn]); }
	public CellStack moves = new CellStack();
	

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
 
    private boolean swapped = false;
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public KulamiChip pickedObject = null;
    public KulamiChip lastPicked = null;
    private KulamiCell blackChipPool = null;	// dummy source for the chip pools
    private KulamiCell redChipPool = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    
    private KulamiState resetState = KulamiState.Puzzle; 
    public KulamiChip lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public KulamiCell newcell(char c,int r)
	{	return(new KulamiCell(KulamiId.BoardLocation,c,r));
	}
	
	// constructor 
    public KulamiBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = KulamiGRIDSTYLE;
        setColorMap(map);
 		Random r = new Random(734030546);	// this random is used to assign hash values to cells, common to all games of this type.
	    blackChipPool = new KulamiCell(r,KulamiId.Black_Chip_Pool);
	    redChipPool = new KulamiCell(r,KulamiId.Red_Chip_Pool);
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
		for(SubBoard b : subBoards) { b.reInit(); }

 		setState(KulamiState.Puzzle);
		variation = KulamiVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		gametype = gtype;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case Kulami: reInitBoard(8,8);
			break;
		case Kulami_R:
			reInitBoard(10,8);
			break;
		}
		
 		moves.clear();
	    
	    blackChipPool.addChip(KulamiChip.Black);
	    redChipPool.addChip(KulamiChip.Red);
	    int map[] = getColorMap();
	    playerCell[map[FIRST_PLAYER_INDEX]] = redChipPool; 
	    playerCell[map[SECOND_PLAYER_INDEX]] = blackChipPool; 
	    playerNChips[0] = playerNChips[1] = revision>=102 ? 28 : 32;
	    	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    droppedDestStack.clear();
	    pickedSourceStack.clear();
	    stateStack.clear();
	    
	    pickedObject = null;
	    resetState = null;
	    lastDroppedObject = null;
		playerColor[map[0]]=KulamiId.Red_Chip_Pool;
		playerColor[map[1]]=KulamiId.Black_Chip_Pool;
		playerChip[map[0]]=KulamiChip.Red;
		playerChip[map[1]]=KulamiChip.Black;
	    // set the initial contents of the board to all empty cells
    
        animationStack.clear();
        swapped = false;
        moveNumber = 1;
        if(revision<=100)
        {
        	getNextSolution_rev1(randomKey);
        }
        else
        {
        	if(goodStarts==null)
        	{
        		fillGoodStarts();
        	}
        	// pick a random one of the good starts, based on the given random key
        	Random rv = new Random(randomKey);
        	getNextSolution(goodStarts[rv.nextInt(goodStarts.length)]);
        }

        // note that firstPlayer is NOT initialized here
    }

    /** create a copy of this board */
    public KulamiBoard cloneBoard() 
	{ KulamiBoard dup = new KulamiBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((KulamiBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(KulamiBoard from_b)
    {
        super.copyFrom(from_b);
        for(KulamiCell c = allCells,d=from_b.allCells;
        	c!=null; c=c.next,d=d.next) 
        	{
        	SubBoard sb = d.subBoard;
        	if(sb!=null) { c.subBoard = subBoards[sb.ordinal]; }
        	}
        robotState.copyFrom(from_b.robotState);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(moves,from_b.moves);
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        lastPicked = null;
        AR.copy(playerNChips,from_b.playerNChips);
        AR.copy(playerColor,from_b.playerColor);
        AR.copy(playerChip,from_b.playerChip);
        redChipPool.copyCurrentCenter(from_b.redChipPool);
        blackChipPool.copyCurrentCenter(from_b.blackChipPool);
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((KulamiBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(KulamiBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(AR.sameArrayContents(playerNChips, from_b.playerNChips), "playerNChips mismatch");
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor mismatch");
        G.Assert(AR.sameArrayContents(playerChip,from_b.playerChip),"playerChip mismatch");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedsourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(sameCells(moves,from_b.moves),"Move history mismatch");
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
		v ^= Digest(r,playerNChips);
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
        case PlayOrSwap:
        	// some damaged games have 2 dones in a row
        	if(replay==replayMode.Live) { throw G.Error("Move not complete, can't change the current player"); }
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


    // set the contents of a cell, and maintain the books
    public KulamiChip SetBoard(KulamiCell c,KulamiChip ch)
    {	KulamiChip old = c.chip;
    	if(c.onBoard)
    	{
    	if(old!=null) { moves.remove(c,false);  }
     	if(ch!=null) {  moves.push(c);  }
    	}
    	else 
    	{
    		if(ch==null) 
    			{ playerNChips[chipColorIndex(c)]--; } 
    			else { playerNChips[chipColorIndex(c)]++; }
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
    private KulamiCell unDropObject()
    {	KulamiCell rv = droppedDestStack.pop();
    	setState(stateStack.pop());
    	pickedObject = SetBoard(rv,null); 	// SetBoard does ancillary bookkeeping
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	KulamiCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	SetBoard(rv,pickedObject);
    	pickedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(KulamiCell c)
    {
       droppedDestStack.push(c);
       stateStack.push(board_state);
       
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting dest " + c.rackLocation);
        case Black_Chip_Pool:
        	playerNChips[chipColorIndex(blackChipPool)]++;
        	pickedObject = null;
            break;
        case Red_Chip_Pool:		// back in the pool, we don't really care where
        	playerNChips[chipColorIndex(redChipPool)]++;
        	pickedObject = null;
            break;
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        case EmptyBoard:
           	SetBoard(c,pickedObject);
            lastDroppedObject = pickedObject;
            pickedObject = null;
            break;
        }
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(KulamiCell c)
    {	return(droppedDestStack.top()==c);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { KulamiChip ch = pickedObject;
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
    private KulamiCell getCell(KulamiId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source " + source);
        case BoardLocation:
        	return(getCell(col,row));
        case Black_Chip_Pool:
        	return(blackChipPool);
        case Red_Chip_Pool:
        	return(redChipPool);
        } 	
    }
    public KulamiCell getCell(KulamiCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(KulamiCell c)
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

        case Black_Chip_Pool:
        case Red_Chip_Pool:
        	playerNChips[chipColorIndex(c)]--;
        	lastPicked = pickedObject = c.topChip();
        }
    }
    int chipColorIndex(KulamiCell c) { return(c==playerCell[0]?0:1); }
    int chipColorIndex(KulamiChip c) { return(c==playerCell[0].topChip()?0:1); }
    
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(KulamiCell c)
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
        	throw G.Error("Not expecting drop in state " + board_state);
        case Confirm:
        	setNextStateAfterDone(replay);
         	break;
        case Play:
        case PlayOrSwap:
			setState(KulamiState.Confirm);
			break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    private void setNextStateAfterDone(replayMode replay)
    {	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state "+board_state);
    	case Gameover: break;
    	case ConfirmSwap: 
    		setState(KulamiState.Play); 
    		break;
    	case Confirm:
    	case Puzzle:
    	case Play:
    	case PlayOrSwap:
    		setState(((moves.size()==1)&&(whoseTurn==SECOND_PLAYER_INDEX)&&!swapped) 
    				? KulamiState.PlayOrSwap
    				: KulamiState.Play);
    		if((playerNChips[whoseTurn]==0) 
    				|| !hasMoves()) 
    			{ setState(KulamiState.Gameover); 
    			};
    		
    		break;
    	}
       	resetState = board_state;
    }
    private void doDone(replayMode replay)
    {
        acceptPlacement();

        if (board_state==KulamiState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(KulamiState.Gameover);
        }
        else
        {	setNextPlayer(replay);
        	setNextStateAfterDone(replay);
        }
    }
void doSwap(replayMode replay)
{	KulamiId c = playerColor[0];
	KulamiChip ch = playerChip[0];
	playerColor[0]=playerColor[1];
	playerChip[0]=playerChip[1];
	playerColor[1]=c;
	playerChip[1]=ch;
	KulamiCell cc = playerCell[0];
	playerCell[0]=playerCell[1];
	playerCell[1]=cc;
	int n = playerNChips[0];
	playerNChips[0] = playerNChips[1];
	playerNChips[1] = n;
	
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
		  setState(KulamiState.ConfirmSwap);
		  break;
	case ConfirmSwap:
		  setState(KulamiState.PlayOrSwap);
		  break;
	case Gameover:
	case Puzzle: break;
	}
	}
	
    public boolean Execute(commonMove mm,replayMode replay)
    {	Kulamimovespec m = (Kulamimovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }

        //G.print("E "+m+" for "+whoseTurn+" "+state);
        switch (m.op)
        {
		case MOVE_SWAP:	// swap colors with the other player
			doSwap(replay);
			break;
        case MOVE_DONE:

         	doDone(replay);

            break;

        case MOVE_DROPB:
        	{
			KulamiChip po = pickedObject;
			KulamiCell src = getCell(m.source,m.to_col,m.to_row); 
			KulamiCell dest =  getCell(KulamiId.BoardLocation,m.to_col,m.to_row);
			
			if(isSource(dest)) 
				{ unPickObject(); 
				}
				else 
				{
				
				if(po==null) { pickObject(src); }
	            dropObject(dest);
	            if(board_state==KulamiState.Puzzle) { acceptPlacement(); } 
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
	            setNextStateAfterDrop(replay);
				}
        	}
             break;

        case MOVE_PICK:
 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
 			KulamiCell src = getCell(m.source,m.to_col,m.to_row);
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
        		setState(((moves.size()==1) && !swapped) ? KulamiState.PlayOrSwap : KulamiState.Play);
        		break;
        	default: ;
        	}}}
            break;

        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
            KulamiCell dest = getCell(m.source,m.to_col,m.to_row);
            if(isSource(dest)) { unPickObject(); }
            else { dropObject(dest); }
        	}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            int nextp = nextPlayer[whoseTurn];
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(KulamiState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ setState(KulamiState.Gameover); 
               	}
            else {  setNextStateAfterDone(replay); }

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?KulamiState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(KulamiState.Puzzle);
 
            break;

		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(KulamiState.Gameover);
			break;
        default:
        	cantExecute(m);
        }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting Legal Hit state " + board_state);
        case PlayOrSwap:
        case Play:
        	// for Kulami, you can pick up a stone in the storage area
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

    public boolean legalToHitBoard(KulamiCell c,Hashtable<KulamiCell,KulamiCell> targets)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
		case Play:
		case PlayOrSwap:
			return(targets.get(c)!=null);
		case ConfirmSwap:
		case Gameover:
		case Resign:
			return(false);
		case Confirm:
			return(isDest(c));
        default:
        	throw G.Error("Not expecting Hit Board state " + board_state);
        case Puzzle:
            return (c.subBoard!=null);
        }
    }
    
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Kulamimovespec m)
    {
        robotState.push(board_state); //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        if (Execute(m,replayMode.Replay))
        {
            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {
                doDone(replayMode.Replay);
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
    public void UnExecute(Kulamimovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	KulamiState state = robotState.pop();
    	setWhoseTurn(m.player);
        switch (m.op)
        {
        default:
   	    	throw G.Error("Can't un execute " + m);
        case MOVE_DONE:
            break;
            
        case MOVE_SWAP:
        	setState(state);
        	doSwap(replayMode.Replay);
        	break;
        case MOVE_DROPB:
        	{
        	KulamiCell dest = getCell(m.to_col,m.to_row);
        	KulamiChip top = dest.topChip();
        	SetBoard(dest,null);
        	playerNChips[chipColorIndex(top)]++;
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(state);
        if(whoseTurn!=m.player)
        {	moveNumber--;
        }
 }
  

 public void initRobotValues()
 {
 }
 static long goodStarts[]= {
		 3070562002457995662L,-7995818385433353703L,-7333485403544630068L,300725299551293492L,-247629027414118531L,-708624545620853599L,8064576172400443273L,8403564419080453291L,2716836501794454521L,5505070428366577444L,
		 6234652631665161686L,-8146068211480204108L,-2329524830401436105L,9161400028156864666L,755996985089191182L,3580104179155726852L,233703483072527357L,1025204820387272332L,6373993574605970208L,-3607317916286665407L,
		 2156905661901863453L,-4733158285516315109L,-6842806246317883207L,-2949100039594744927L,-1749845222072980579L,7726259579249377327L,6672057511559548303L,-5937869754306957554L,6743603965822896559L,-5014406417787207745L,
		 8039330563396868170L,-6818388555419837042L,-7654747191129825969L,-3185858689965859258L,-3202067727225080350L,987034369501096807L,-7601994529161991141L,6231472010332170174L,956351895865889302L,-9087529854450469967L,
		 8759110608590013066L,89799878783033747L,4932238255112137746L,-6890721607799825620L,-7070987766416284353L,1802177102135077052L,-2427719170352887744L,-5077182201258156346L,4708871266532494296L,-8070788719850384013L,
		 -2208191934336514162L,8242361343398803989L,4075319359563079020L,-3423032094702987141L,1567518133025078588L,-2278076557704359608L,-7662108579846903873L,-6999551565191421864L,393718734559084801L,1996058097193364096L,
		 5041989692996703035L,-7475276775012586306L,3185115693151724717L,7489374588162788465L,-8773622707113012111L,8837650501057349056L,9209077131425245932L,6151752803950813357L,122286410864033828L,-4756627762683451301L,
		 -8479473403792177883L,-4591443439231603753L,8050231037173863945L,7827947820500210512L,93241130920679663L,-174170270216047595L,-3883993006692991686L,743948140609803958L,-5420457745662703679L,-8931747267840773036L,
		 -7898925346128400702L,411007114887502004L,4170479684726953201L,-2770289195319979060L,-1088669225119892033L,-6532151182729033747L,-8350366287469596963L,-6175092259350370968L,8257459981740934536L,-4225997365446031870L,
		 -3892144462094045124L,-5362920047002522169L,-2426891717385297907L,2150254354930561742L,4097439039014933666L,8185259883636565356L,864044006207737294L,5626948304796931868L,7421497729893830329L,7765774055919706372L,
		 5200154902470399303L,7278117374796970462L,7262196103029623863L,2586013102914259107L,-6788653450606614918L,-3512037838559565293L,-3228104541592998972L,-5445268680506432401L,-1615578322031408249L,559782763730789172L,
		 5384404271643866278L,7304604034889069296L,5465529626947260640L,-5221095137955061268L,6671555702303887749L,-7410467637725481865L,-8546598335524837684L,2618279057277821340L,4982106877072320005L,6312895333522079311L,
		 -2319777077534394813L,-1742669667642614210L,-560625652358225094L,-9086996167639736461L,6264535104233856189L,5992363481295706597L,-2599178337097581864L,-5308971081109190700L,4464418542216732291L,-4116703478159411136L,
		 -7720186416151174809L,3426245924127059033L,6991587457189226975L,1704550540345459990L,-2123624857717979460L,-2545720528896874301L,8611311424584051015L,-835273122064789712L,5118137961650321769L,87686959374508489L,
		 5112239146048569916L,2266176799053640202L,8713264735709791808L,-320338216900563631L,5228866336536190502L,4967145660990004844L,-2784499012293123613L,3125087021999598070L,-1005478322462792506L,5323598480151982875L,
		 -8204925463682053394L,-2339486033477043528L,5386195351011863453L,-6502459640252545805L,6303098108566785104L,45713313752109437L,-6858244100064878887L,-1644664420843254299L,-7188352292579212948L,-2552121458060709944L,
		 -2074245764237299216L,-4051710782641016030L,-1021535673358684476L,5666104357915879287L,1802303728531370867L,-7968023616954782082L,-8296855308217979117L,-7450501717609684200L,-8783181758713275273L,-6491684343131170431L,
		 2346165706738463230L,-2118565299158022297L,-5867733716494173380L,5896143198374122558L,-3184015337458888158L,-1057112229399699951L,-4568925421114641046L,5609865269474426087L,7176691149092966659L,-1332093318983968761L,
		 3775747784133656667L,8636631036387058121L,-1943673391342810619L,6499329432705865568L,-4467867748479671166L,8847642157704696231L,5517792559191772720L,-1977668779817371615L,3407307428367110276L,-5777317670537521118L,
		 4468204373273965530L,-3422547009726198903L,7157149695317376947L,8717577120267673567L,5457288948697951330L,5020881310598454676L,-8552458805524154454L,-1143658362436130948L,84967152124814573L,-8135441683515580798L,
		 -6115762322209224970L,8824598685245981384L,-1658193328074891428L,-2290052299925071855L,-2524552613268516006L,2628873577358380854L,-685247833685669399L,5557107139518744804L,9095654347721428302L,6091999735394119322L,
		 -4356598256994323050L,-46979637040409816L,-1263200571521651523L,-3706596357500855276L,-1447554849613736996L,-1830436071765809497L,7128279598927771254L,3731200022776761039L,8779625324874678974L,5801763629141221192L,
		 -9094211803572505002L,2415167314880293901L,-6011333756887270966L,368131290154704697L,5706251787980122398L,-1365417911728892239L,357872765286223060L,-6376493673312588090L,-7033746941594513208L,8458720480906849494L,
		 990210906824397472L,-1226742050670723188L,8842783292769967316L,-3922982519969481884L,-5694207425641783913L,3961652544730845900L,1026210658635881781L,-1966183241401271483L,-2314011865119156958L,8791300976359770907L,
		 -3131809919151786027L,-7419999730992166536L,-4777064634630755337L,7222706942990277810L,338628850262228555L,3228316834588765338L,2929075999388830240L,-567505062110717816L,1840249606532670204L,4074942034713255373L,
		 -6023268437821533899L,-2460860602914096777L,6058397245344728361L,-5344502860559887678L,-3496036373601336819L,2284505768806265934L,-4000775451124012188L,4873614552809374369L,5109928444247720748L,-1211101475578012534L,
		 -1432378913821542595L,-2736361120497823446L,6223234357572221557L,7030156437993817117L,-8127185236751816026L,-7954804677995541548L,8666529459261859992L,-2370591089213041977L,8180263736538431367L,-6657869100175055156L,
		 -4853037329646183684L,-4645090381149713738L,-1385494916395027329L,8438869827558998559L,-101121713587054556L,2214278017747007123L,5772912563231510962L,-3306690388903710625L,7153285607021034620L,3326912070460595609L,
		 3272305347939764085L,-3502786903291300257L,-2215940328992805933L,1262034781506961296L,-1108028847781571647L,-2943711891852748170L,8343601617659092922L,1575039592804505785L,1169991561065820249L,7701890518712902314L,
		 1374342649846701537L,-1607686866102307824L,-8429773377003230122L,-861857176710051150L,-6923280082593489994L,2007808118069252334L,1253603953971289542L,4224255639846542448L,7141597506402826629L,-5592566836215308004L,
		 -7003941254228946065L,9129102921314729480L,6255563603337319077L,1347740854139001456L,-7872378201113063703L,-1753217597202820218L,-8532010166652925022L,-4837666709135958047L,-2823135704744414473L,4239505236318436536L,
		 -3473666812486317395L,265590487973787510L,6503090997832993214L,-2657864372554108740L,3564983109249049795L,707230009771033647L,-3047628089390328253L,1919577615281685513L,6628258315520445284L,6020321968415543315L,
		 3829951151889796639L,-4037546777645412601L,-2548153699604545841L,-7567439997685210935L,-1390957001469856161L,5212340606609095874L,-8251852416549575386L,9126997009361190867L,-7659502688288397525L,7621998475257138834L,
		 -4446038106919135889L,-442957039599781771L,-6216873747552533966L,-8649078889864529559L,-5471232722700585363L,3571310414313820655L,1453190719481028324L,-7312070119182715015L,6948395088505898777L,3230248649560313327L,
		 6133408109902834594L,1507814773004337300L,1575544912443184875L,-1051381305498457942L,3951626458276613788L,5891152041441708722L,-7858876214247118871L,1295926762362365842L,8975449063837885642L,-8383858243531755568L,
		 591576500018638583L,-6787810637255183641L,4741813265070168343L,-282809182732181415L,4121015906894811798L,-7865408602062188789L,4323917938267346575L,621014110644533668L,-1801630460177745842L,2140366718240725767L,
		 4012557817472987081L,-4672047333754650461L,164085724120619104L,-467338485673422065L,-6499863106598392599L,662734027259822287L,-4719176797145604908L,8434382017159547970L,-3153833079962625563L,4956100677484618976L,
		 6181019654876331234L,-620527726462921852L,8373980994121100917L,-9063023542925318845L,-1415062773126212231L,5190024817885401442L,-6128928632290324108L,1812438661603497502L,189608436866450821L,-7795374761983170575L,
		 8294981281170294162L,8418951772783352463L,8661065757049793134L,-5293266327486042371L,3719666431542967239L,-8678847201596270658L,-1047979255539329641L,-2839553308749332951L,-6021460274056346250L,7869012261052831939L,
		 -1908486898058825L,-6479303946687008780L,-5849528059410373465L,-2470442226509372396L,8225290890425252728L,-3635442907841192335L,7042742671830948261L,-6911744205668807761L,6036028417331333787L,4474154738126849226L,
		 -7183150204047689798L,5625637383612090992L,9204187817595599361L,-7185589905008375613L,5523646198920187457L,5588645765418599432L,6854842145088964962L,4214921400770856421L,-3104479777211762885L,-7578957435448318179L,
		 4599458994142485977L,-2809133442891560916L,-1870561602308667239L,7452793876808224326L,-8255560666232443778L,5531787333939894759L,-3493387811442351431L,-7367813432328123606L,795208863801195709L,4482447247555485075L,
		 -510215174813358422L,6001371854472866239L,2122113110359812072L,438750367528231792L,8738303587295985093L,-2737928742781524552L,-507960157260382068L,-7437631013660455550L,-6541416750002262148L,-1956150445696203522L,
		 -485738420107058270L,-851444484408028844L,-7345068654326296243L,8912402022234088832L,3529257829868662268L,-4773556223661364459L,-7779807851310596469L,3128277965933136726L,-932938313095722107L,-372181664154627498L,
		 5944665318441908399L,1664009268653589634L,2853153233348114111L,3706025840289081527L,1286347489820481844L,7361377824362333358L,-1990859782407501701L,1512757832861296255L,-5767302442308056163L,-7750870404992855511L,
		 -8094175138019408974L,-3623976609450889517L,2382289718876192106L,2621129410586579327L,-936519328763763962L,-3118418431614644154L,1911844321625357960L,3042734183310584198L,2240406642165924107L,-3242583866690927738L,
		 4954259321925546502L,-4758282818529033985L,3248357668266738170L,-7545117623625568557L,-1322455036555961692L,6887449432522329889L,-2248581271184224798L,-660358565645451967L,8144485342932857153L,6610129189294444509L,
		 -7713030798424818440L,1618345830806833626L,-5068550028440393519L,3223483605515360469L,-8537542761504962078L,-4124736983632401047L,-9169614758174304287L,4067606505929337579L,-8833455035528203261L,-5692717169683899324L,
		 -4061269844950596261L,-4255093789876303490L,7561273397037977810L,5528927727440913949L,-8337262861677372734L,-5941887649272513664L,4726546942110173756L,6952886890508904617L,885592463521356941L,96829283446776970L,
		 -4441828060078252928L,7769210555102181767L,6080414441081154773L,-1274801284604322574L,7104269071365181333L,-1433779300413845591L,4325950673920880112L,2800007419780464661L,-33504325008842400L,2795912558467091402L,
		 -6089739233261722078L,-3564335060615323916L,3023306306986965846L,8768961777709822885L,-6656562383228127425L,7395998947028448015L,7577596689199909122L,-5743945743238486631L,-2935874970981043491L,9043527791799878033L,
		 -1043085279278087101L,7447019096909797959L,648796867564299302L,-4292957146254135214L,8892599760109051170L,1813829257318950315L,8959780276612071534L,6569276092530400484L,8268517294125420664L,2056653837979152993L,
		 -3955891575971910737L,-8052945425676525836L,-7612789908315573367L,-1745427636908806324L,-2306458189592994578L,3358949567218393465L,5128906741518980423L,-755233100900550341L,-2201616876859721871L,-3904594098839217837L,
		 8840759419784646276L,1285584906836836195L,-8357099768236252455L,639213099104645732L,3670549681971889616L,7833103083517014836L,4175923160953463736L,6103131190158022796L,-540813532508980787L,5365646178105590472L,
		 8515225208720418641L,-9080964378452585768L,-3410977984642407967L,-86155424446956092L,2413263270498284107L,-6147006872427896744L,-8833726347002642268L,-8141778952852740655L,-8435366252534998294L,7965287953481174547L,
		 6559054541764960258L,-4366439395118153962L,2055562416508227528L,4489719649233570597L,5653699105458335538L,1967674525816959060L,-348721352531877461L,-5752394107722457555L,6191622125170365019L,-6994682908958680638L,
		 -294271050513014226L,-5081572092781285197L,-8428763635830645765L,-5032620649673457842L,-8252363636811374045L,400855982379103854L,-6106480371906523912L,6743750208441717195L,-4275966464800952404L,146074355426609770L,
		 -8961523846095254514L,5776640030719971012L,5076650643667016776L,1206387528662435222L,-1371138432995653324L,-5338090590219255520L,4384614470963163002L,859465042474770418L,2073665120337193987L,-1085407371423850879L,
		 7640483336627513428L,-4704433056485600631L,-4540016381672243773L,-5972734551734748799L,-3063763920040963343L,-3297758732594513206L,-2907435218575073341L,-5063615419927617167L,-32604281748431201L,-1834888451753193587L,
		 -2068941584016510175L,8253834163947065518L,-4696778540571229910L,-4890319664771393334L,-7140732138442693674L,-7711315246331821132L,3538112104461532205L,1906153608615552078L,2065631100710482681L,4039926052677244830L,
		 6833363414342859592L,1439950615582171613L,1050639407374226197L,-498534323982372121L,703817129175236264L,2769354953364409175L,-9220916300576028835L,6058013917354756582L,7323978221420216495L,-1381877508758953551L,
		 -5350836321665478132L,2177722450737719173L,9027903832977473111L,3398360373398263511L,2098525473200254518L,-5488077082669388897L,6219627742933998245L,-2529727161232155313L,8551987180052993001L,4028143524201627977L,
		 4676137860403270916L,-5550318799393637844L,213001964610985084L,-6696426197367248783L,-5186365357315267439L,-506890674977071109L,-776803522756585413L,-3006824652880463638L,5398963028981847410L,2730248111498934022L,
		 -3303057709601111790L,-9156449024181592868L,4913417054384757342L,-1532106820908591285L,-8222929858680331976L,1085644489740684202L,2972666428709349670L,-2605780968532524371L,-4331281368943392016L,-4700972137242413216L,
		 6478765858906600659L,4328578768555026290L,-1377232298463839096L,9102169086129565393L,-6302937380897761419L,2205176249324884469L,68166648212232189L,4111700862357657629L,1766953499505835711L,3471990194879476547L,
		 969660847242456308L,7935897109030386674L,-3075533766320291465L,-6824813437551705009L,1762917811939736819L,7575576611084628330L,3497633409865019070L,-423240459757320679L,2608174899681351290L,2886606783404194452L,
		 -3699053475613935103L,2083292224372320215L,-5243718698751940218L,-8448127713577213560L,-7945687695889450108L,-3443580654048142720L,8611816477947291881L,-3345610684774120752L,-3255162683016374572L,4705985723179931932L,
		 -8440945285790251689L,-2686983425417628298L,2491268001838636050L,2985433749914056161L,-8544957654317503006L,2321537339140613090L,7511913268577888004L,-5356790100005533232L,-6792127507582081727L,6891701120572918791L,
		 8033227602323833644L,3096989188673200303L,7782621336535172852L,790547264460858684L,4443189867690194564L,8014339761718650539L,-7460818365347763847L,-6195318130194380155L,-1353186662864154754L,1316949404211228545L,
		 6408870689056715442L,-1369246159962089331L,-1231922654767151143L,-707374517375362981L,1760567355220302399L,-2959656365764043519L,-4567539510213507136L,6304098930517496812L,5183367013887295559L,5651393089007128912L,
		 -2860973712007187760L,-5566465661116880360L,-6403576928114214023L,6877979306319171374L,-1467292079265969586L,-5016222009433770762L,7022619074630683921L,-6307736100553664949L,6460408239792496637L,-3220434491785486561L,
		 -8614115915721907119L,-6882043022188720570L,-8580129833279233007L,3898378966642218270L,6779932235943637116L,7208466361690301223L,-1792600919149498475L,-4948746491358442694L,364722681881345495L,-5237432318484459161L,
		 -6754791569314446144L,-8379768449927325106L,-3403159437375555160L,1491246095966327113L,2548651742436255912L,4675594023403617230L,7738336711102198270L,8051789351592303686L,-1748200470414969309L,3347920338223401598L,
		 2582226324340178384L,-4564836657557549198L,-5650629525223268302L,7335099236816859762L,-7264439911708677354L,-1446544333561580507L,-2535697493531381509L,4989419638233693216L,-2131100858527777186L,7955433763292707600L,
		 -7408422160258998128L,-6545045418904319952L,-5918180898410983505L,9204288966310948890L,-3401229471356290793L,5717282294782818997L,3558104893206210068L,-4222587429429789013L,2880721509385234366L,-6176386511177464125L,
		 -2265819194917811349L,4842647801400459758L,759824305550455914L,-4136616313595730599L,4683076888404644753L,-8715554639691311570L,1593963987045250065L,4282933014927009400L,-8563378260706842861L,-4171374491921031719L,
		 -8846838915252808654L,-497969163935947383L,-3108483573358898555L,721082294242290254L,-3113457515356074504L,2979836576265851180L,-3163407980226978475L,-6047193642272891096L,-5252499771229352456L,-5270849622070424668L,
		 -4280305128186867837L,7604235580172428630L,4906632424938499009L,8628873501612005560L,-9196225288716862273L,-8043878806102770113L,4033710579567485569L,-4081415525550161298L,8685917952171686381L,-7235703698452876834L,
		 -4907388946306006633L,-8535899503380794298L,-6746563896655611161L,4308309594109504145L,7701011276395520221L,4025255810355422860L,-3770950410430679641L,8449129774899088996L,-5750015443763925691L,297854284131264046L,
		 -5199440182766043235L,-1784352458733411558L,-1224195522919768046L,-8536342527831379534L,-5906297736199809731L,8446282238383070235L,5900309145780893757L,8812573581843511520L,2797450179929153255L,-5076095456954895259L,
		 -2029400425538737242L,-7050595515916864078L,247849851478908352L,9137774535928918099L,-7791700552677957258L,-2662495158171544637L,722045667152742800L,2241671344253976778L,-94044019567209037L,8109041001689777453L,
		 1128516023670968049L,-2091426939316177870L,452792452795599481L,2806974984897095171L,6230055871640309785L,892431261867582182L,-4777642662046663470L,-7614734964103704078L,5614962055398006170L,-6873609872214112092L,
		 8862136031033864625L,2891299686130621703L,-9050533544948528792L,1784829069613196464L,-6644398069503800177L,2745084447533810491L,3753472166322167513L,-9113632301433100222L,1926417734833139453L,3748787086983224506L,
		 -3854361605390305195L,-7234161205552920653L,-6722483401357305904L,-7511481242401418277L,1704363931026015614L,-5810399126496912074L,-737796655601799980L,-7875092169184781178L,2755663988188787865L,-4446141065003984444L,
		 8722807753636980071L,4232321815879607826L,1643598920287425045L,1854746888275372229L,1403627891630166999L,6782374318297444814L,-73903462713605797L,6704714816713505195L,2310515503530931230L,-7732440329610573719L,
		 5465119705745919480L,4251564938728734700L,1466902201716959633L,3468122482544296691L,6385403960661816644L,4761580602695878236L,-3076124475132210616L,432701850566688405L,-7212921958146072570L,-7450975477341574562L,
		 2913631239636860509L,-724780127970269085L,6271399343708276624L,-8507258854086675879L,-6403830426089472735L,8291585699870677172L,4949988519473225397L,4944471372186181723L,2390977590280190032L,7951692466143065245L,
		 7036290273237902587L,-8811480678392229370L,6043806295034686841L,88366032949554497L,4043694842277023315L,7874238239057210198L,5961022357043593038L,3003370308615753129L,-2455651523718922719L,-7342796567013856993L,
		 3697366504384486839L,-5702967242284584816L,-7407911345129111212L,1469189971886091865L,-7232716331975712109L,-6388141151583962118L,3676108516156220945L,6082184273357390729L,-1746806329950230895L,4312095404053563226L,
		 9192023172703987520L,4160840727062360572L,1050105889264940038L,698469596910089055L,-8566039355647456087L,1482001671379606360L,1157749246384046078L,-6211456963828071984L,-1899654495665292402L,-4610398471936725525L,
		 -4455209442147536451L,-8430633907327230912L,-1776514425670379571L,8263874464605090637L,6386007332601220488L,5553557335587290100L,-3212455110635605945L,-7902733950076068519L,3868406080954377977L,5020411451350247158L,
		 9211866907466159333L,8333283243030142541L,2497663052418540263L,-5947221216116649729L,9060522511533955870L,3352699620227898029L,80015242160494489L,3306734653367565006L,-7945057701191713246L,-1981118571651713263L,
		 -183420645601672822L,-8297399508984909529L,-5528013565169535650L,7011546311422811949L,-681191657949903821L,-7703707567187829742L,3366886848718368541L,1036170408589426273L,7341932106990895563L,-4445663964793125360L,
		 -661720969427520300L,-7845199047795264352L,-6502844159019553165L,7800524051255199337L,2113221677604322618L,3625136639631420149L,2993375018275663563L,2345805328794323039L,-5094685975215843651L,6223452021806098239L,
		 4299454576625611586L,2025916891268494207L,-7647325931746161370L,-2934138524110601618L,1584922743889322055L,-5848937506714118233L,7438850733268512339L,-1582417666159742890L,-2637475197050187085L,1151260791689514842L,
		 5789376486417002220L,6371971698321108279L,-3556113990703373230L,3469620601982381976L,-7255618847861224487L,-3735951337001151584L,-5588410078559016905L,-2350148768731075000L,2416875271622526417L,-3293899894886312805L,
		 -5941890752810226334L,-2324733303700105188L,4324538823023319450L,-4840654414884174234L,-6348108215314178311L,-579911608852177359L,4877726896665516376L,2844462139575003615L,-4657290818464766769L,8095372045148551976L,
		 -6122266499665561418L,-7780421328103738122L,-8358778724971391979L,-1921198145724556690L,124167221884120787L,1735729738048271723L,5549540753022430158L,7745270797193586604L,-3211499235572598519L,5448391946581583257L,
		 8073173860762414898L,5822655554981508343L,-7715756968017153670L,7747613298419314145L,1025643088108239041L,-3092518250314001575L,4788853353632084103L,-5797974474650595726L,1729978487680135935L,4421622412113184202L,
		 9101115218609804662L,1489558769496345952L,-1370967684819060633L,-20011093571826745L,309713369459117520L,2432212518389252091L,5774763601797992896L,3177466202185270098L,-1594313747848702067L,-4505538871692237958L,
		 };
 static long knownSolution = 0;
 static int NSTARTS = 1000;
 static int STEPLIMIT = 99999999;
 public static SubBoard[] solvedBoard = null;

 //
 // some starting points are really far from start, but really easy solutions are common
 // this finds 1000 unique, easy ones, which will be used in the live program.
 //
 private void fillGoodStarts()
 {	Random r = new Random();
 	Hashtable<Long,Boolean>solvedHash = new Hashtable<Long,Boolean>();
 	
 	STEPLIMIT = 1000;
 	int dups = 0;
 	int fails = 0;
 	LStack startStack = new LStack();
 	while(startStack.size()<NSTARTS)
     {	solutionFail = false;
     	long randomv = r.nextLong();
     	getNextSolution(randomv);
     	if(!solutionFail)
     	{	long hash = solutionHash(r);
     		Boolean hv = solvedHash.get(hash);
     		if(hv==null)
     		{
     			solvedHash.put(hash, true);
     			startStack.push(randomv);
     			G.print("hash "+hash+" "+startStack.size());
     			G.print("");
     		}
     		else {
     			solutionFail = true;
     			dups++;
     			G.print("hash "+hash+" Duplicate");
     		}
     		
     	}
     	if(solutionFail) { fails++; }
     }
 	goodStarts = startStack.toArray();
 	//
 	// print the list, which can be cut and pasted into the code
 	// where it will be permanant and immutable.
 	//
 	System.out.println("goodStarts[] = {");
 	for(int i=0;i<startStack.size();i++) 
 		{ System.out.print(startStack.elementAt(i)+"L,");
 		  if(i%10==9) { System.out.println(""); }
 		}
 	System.out.println("};");
 	G.print("Fails "+fails+" Dups "+dups);
 }
 long solutionHash(Random r)
 {	long v=0;
 	for(KulamiCell c = allCells; c!=null; c=c.next) { v^=c.Digest(r); }
 	return(v);
 }

 void getNextSolution(long randomv)
 {	if((solvedBoard==null) || (randomv!=knownSolution))
 	{
	// if the solution is not known, treat it as a puzzle to be solved
	Random r = new Random(randomv);
 	CellStack cells = new CellStack();
 	solutionSteps = 0;
 	solutionFail = false;
 	
 	for(KulamiCell c = allCells; c !=null; c=c.next) 
 		{ cells.push(c);
 		  c.subBoard = null; 
 		}
 	cells.shuffle(r);	// this shuffling of the starting points makes the solution unique
 	for(SubBoard b : subBoards) { b.reInit(); };
 	//solutionStartTime = G.Date();
 	getNextSolution(subBoards,0,cells,r,null);
 	//long later = G.Date();
 	//G.print("Time : "+randomv+" "+(later-solutionStartTime)+" steps "+solutionSteps+" "+(solutionFail?"FAIL":""));
 	knownSolution = randomv;
 	solvedBoard = new SubBoard[subBoards.length];
 	for(int i=0;i<solvedBoard.length;i++) { solvedBoard[i]=subBoards[i].clone();}
 	}
 	else 
 	{	// apply the known solution to the board
 	 	for(KulamiCell c = allCells; c !=null; c=c.next) { c.subBoard = null; }
 	 	for(int i=0;i<solvedBoard.length;i++)
 	 	{
 	 		subBoards[i].copyFrom(solvedBoard[i],this);
 	 		place(subBoards[i],subBoards[i].location,null);
 	 	}
 	}
 }
 SubBoard dummyBoard = new SubBoard();
 private int solutionSteps = 0;
 //private long solutionStartTime = 0;
 private boolean solutionFail = false;
 private synchronized void getNextSolution_rev1(long randomv)
 {	Random r = new Random(randomv);
 	CellStack cells = new CellStack();
 	solutionSteps = 0;
 	for(KulamiCell c = allCells; c !=null; c=c.next) { cells.push(c); c.subBoard = null; }
 	cells.shuffle(r);
 	for(SubBoard b : subBoards) { b.reInit(); };
 	//solutionStartTime = G.Date();
 	solutionFail = false;
 	getNextSolution_rev1(subBoards,0,cells,r); 
 	// long later = G.Date();
 	// Rev1   100344 6349 steps 204727203
 	// Time : 100344 3994 steps 84153622
 	// G.print("Rev1 "+randomv+" "+(later-solutionStartTime)+" steps "+solutionSteps);
 }
 
 // find the "next" solution from a random starting point.  The randomness of the
 // starting position is assured by shuffling all the (empty) cells remaining on
 // the board.
 private synchronized boolean getNextSolution_rev1(SubBoard boards[],int idx,CellStack cells,Random r)
 {	
	if(idx>=boards.length) { return(isConnectedSolution()); }	// success, we placed them all
	
	SubBoard thisBoard = boards[idx];
 	for(int lim = cells.size()-1; lim>=0; lim--)
 	{
 		KulamiCell placement = cells.elementAt(lim);
 		int rotation = r.nextInt(2);
 		for(int i=0;i<=thisBoard.nDistinctRotations;i++)
 		{
 		solutionSteps++;
 		thisBoard.setRotation((rotation+i)&1);	// try the 2 rotations in random order, square tiles will try just one.
 		if(canPlace(thisBoard,placement))
 		{
 			place(thisBoard,placement,cells);
 			if(getNextSolution_rev1(boards,idx+1,cells,r))
 			{	return(true);
 			}
 			unPlace(thisBoard,placement,cells);
 			}
 		}}
	 return(false);
 }
 // find the "next" solution from a random starting point.  The randomness of the
 // starting position is assured by shuffling all the (empty) cells remaining on
 // the board.
 private SubBoard getNextSolution(SubBoard boards[],int idx,CellStack cells,Random r,SubBoard prevrefutation)
 {	
	if(idx>=boards.length) { return(isConnectedSolution()?null:dummyBoard); }	// success, we placed them all
	SubBoard thisBoard = boards[idx];
 	for(int lim = cells.size()-1; lim>=0; lim--)
 	{
 		KulamiCell placement = cells.elementAt(lim);
 		int rotation = r.nextInt(2);
 		for(int i=0;i<=thisBoard.nDistinctRotations;i++)
 		{
 		thisBoard.setRotation((rotation+i)&1);	// try the 2 rotations in random order, square tiles will try just one.
 		solutionSteps++;
 		if(solutionSteps>=STEPLIMIT)
 		{
 			solutionFail = true;
 			return(null);
 		}

 		if(canPlace(thisBoard,placement))
 		{
 			place(thisBoard,placement,cells);
 			if((prevrefutation==null) || canPlaceSomewhere(prevrefutation,cells))
 			{
 			SubBoard refutation = getNextSolution(boards,idx+1,cells,r,null);
 			if(refutation==null)
 			{	return(null);
 			}
 			unPlace(thisBoard,placement,cells);
 			if(idx>12 && !canPlaceSomewhere(refutation,cells)) 
 				{ return(refutation); }	// still can't place it.
 			}
 			else { unPlace(thisBoard,placement,cells); }
 			
 		}
 		}}
	 return(thisBoard);
 }
 private boolean canPlaceSomewhere(SubBoard b,CellStack cells)
 { 	for(int lim = cells.size()-1; lim>=0; lim--)
	{
		KulamiCell placement = cells.elementAt(lim);
		if(canPlace(b,placement)) { return(true); }
	}
 	return(false);
 }
 private int sweepCounter = 0;
 // check that all the subboards are connected by doing a flood fill and
 // count the cells reached.
 private boolean isConnectedSolution()
 {	 int totalSize = 0;
 	 for(SubBoard b : subBoards) { totalSize += b.w*b.h; }	// it's always going to be 64
 	 sweepCounter++;
 	 int csize = connectedSize(subBoards[0].location);
 	 return(csize==totalSize);
 }
 private int connectedSize(KulamiCell seed)
 {	int n = 0;
 	if((seed!=null) && (seed.subBoard!=null) && (seed.sweep_counter!=sweepCounter))
 	{	seed.sweep_counter = sweepCounter;
 		n++;
 		for(int dir = seed.geometry.n; dir>0; dir--)
 		{
 			n += connectedSize(seed.exitTo(dir));
 		}
 	}
 	return(n);
 }
 
 // return true if subboard b can be placed at location c
 private boolean canPlace(SubBoard b,KulamiCell c)
 {	G.Assert(c.subBoard==null,"should be available");
 	KulamiCell row = c;
 	for(int j=0;j<b.h; j++,row = row.exitTo(CELL_DOWN))
 	{
 	KulamiCell column = row;
	for(int i=0;i<b.w; i++,column = column.exitTo(CELL_RIGHT)) 
 		{ 
 		  if(column==null || column.subBoard!=null) { return(false); }  
 		}
 	}
 	return(true);
 }
 // place subboard b at location c
 private void place(SubBoard b,KulamiCell c,CellStack cells)
 {
	 	KulamiCell row = c;
	 	for(int j=0;j<b.h ; j++,row = row.exitTo(CELL_DOWN))
	 	{
	 	KulamiCell column = row;
	 	for(int i=0;i<b.w ; i++,column = column.exitTo(CELL_RIGHT)) 
	 		{ 
	 		  column.subBoard = b;
	 		  if(cells!=null) { cells.remove(column,false); }
	 		}
	 	}
	 	b.location = c;
 }
 // unplace subboard b from location c
 private void unPlace(SubBoard b,KulamiCell c,CellStack cells)
 {
	 	KulamiCell row = c;
	 	for(int j=0;j<b.h ; j++,row = row.exitTo(CELL_DOWN))
	 	{
	 	KulamiCell column = row;
	 	for(int i=0;i<b.w ; i++,column = column.exitTo(CELL_RIGHT)) 
	 		{ 
	 		  G.Assert(column.subBoard==b,"should be used");
	 		  column.subBoard = null;
	 		  cells.push(column);
	 		}
	 	}
	 	b.location = null;
 }
 
 public Hashtable<KulamiCell,KulamiCell> getTargets()
 {
	 CommonMoveStack all = new CommonMoveStack();
	 Hashtable<KulamiCell,KulamiCell> targets = new Hashtable<KulamiCell,KulamiCell>();
	 addMoves(all);
	 while(all.size()>0)
	 {
		 Kulamimovespec m = (Kulamimovespec)all.pop();
		 if(m.op==MOVE_DROPB)
		 {
		 KulamiCell c = getCell(m.to_col,m.to_row);
		 if(c!=null) { targets.put(c,c); }
		 }
	 }
	 return(targets);
 }

 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	if(board_state==KulamiState.PlayOrSwap)
 	{
 		all.addElement(new Kulamimovespec(SWAP,whoseTurn));
 	}
 	addMoves(all);
 	return(all);
 }
 
 private boolean hasMoves()
 {	int am = addMoves(null);
 	return(am>0);
 }
 private int addMoves(CommonMoveStack all)
 {
	 int n = 0;
	 int mSize = moves.size();
	 if(moves.size()==0)
	 	{	
	 		for(KulamiCell c = allCells;
		 	    c!=null;
		 	    c = c.next)
		 	{	if((c.topChip()==null) && (c.subBoard!=null))
		 		{if(all!=null) { all.addElement(new Kulamimovespec(MOVE_DROPB,c.col,c.row,playerColor[whoseTurn],whoseTurn));}
		 		 n++;
		 		}
		 	}}
	 else
	 {	switch(board_state)
		 {
	 	 default:
		 case Confirm:
		 case Gameover:
			 break;
		 case Play:
		 case PlayOrSwap:
		 	KulamiCell prev = moves.top();
			SubBoard prevBoard = prev.subBoard;
			SubBoard prevBoard2 = mSize>1 ? moves.elementAt(mSize-2).subBoard : null;
			for(int direction = CELL_UP,lastDirection=CELL_UP+CELL_FULL_TURN;
					direction<lastDirection;
					direction+=CELL_QUARTER_TURN)
				{ n+=addMovesInDirection(all,prev,direction,prevBoard,prevBoard2);
				}}
	 }
	return(n);
 }
 public KulamiCell prevLoc(int n)
 {	int sz = moves.size();
 	return(n<=sz ? moves.elementAt(sz-n) : null);
 }
 private int addMovesInDirection(CommonMoveStack all,KulamiCell from,int direction,SubBoard sb1,SubBoard sb2)
 {	int n=0;
 	KulamiCell to = from;
 	while( (to = to.exitTo(direction))!=null)
 	{
 		if(to.topChip()==null && to.subBoard!=null && to.subBoard!=sb1 && to.subBoard!=sb2)
 		{
 			if(all!=null)
 				{all.push(new Kulamimovespec(MOVE_DROPB,to.col,to.row,playerColor[whoseTurn],whoseTurn));
 				}
 			n++;
 		}
 	}
 	return(n);
 }
 
}
