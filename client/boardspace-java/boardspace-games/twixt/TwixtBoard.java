package twixt;

import java.awt.Color;

import static twixt.Twixtmovespec.*;

import java.util.*;
import lib.*;
import lib.Random;
import online.game.*;

/**
 * TwixtBoard knows all about the game of Twixt
 * 
 * @author ddyer
 *
 */

class TwixtBoard extends rectBoard<TwixtCell> implements BoardProtocol,TwixtConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
	TwixtVariation variation = TwixtVariation.twixt;
	private TwixtState board_state = TwixtState.Puzzle;	
	private boolean ghost = false;
	public boolean robotBoard = false;
	public int boardSize = 19;
	private StateStack robotState = new StateStack();
	public TwixtState getState() { return(board_state); }
	public int nCells() { return(variation.boardSize*variation.boardSize); }
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(TwixtState st) 
	{ 	
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

    private PieceColor playerColor[]={PieceColor.Red,PieceColor.Black };
    public PieceColor getPlayerColor(int who) { return(playerColor[who]); }
    int sweep_counter=0;

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
    public TwixtChip pickedObject = null;
    public TwixtChip lastPicked = null;
    public TwixtCell lastDropped = null;
    public TwixtChip lastDroppedChip = null;
    private TwixtCell[] blackChipPool = new TwixtCell[TwixtChip.blackChips.length];	// dummy source for the chip pools
    private TwixtCell[] redChipPool = new TwixtCell[TwixtChip.blackChips.length];
    private TwixtCell[][] playerRack = {redChipPool,blackChipPool};
    public Hashtable<TwixtId,TwixtCell> idHome = new Hashtable<TwixtId,TwixtCell>();
    public TwixtCell[] getRack(int pl) { return playerRack[pl]; }
    public TwixtCell getPeg(int pl) { return(getRack(pl)[0]); } 
    public CellStack redPegs = new CellStack();
    public CellStack blackPegs = new CellStack();
    public CellStack getPlayerPegs(int pl) 
    {	return(getPlayerPegs(getPlayerColor(pl)));
    }
    public CellStack getPlayerPegs(PieceColor cl)
    {
    	switch(cl)
    	{
    	case Red: return(redPegs);
    	
    	case Black: return(blackPegs);
    	
    	default: throw G.Error("Not expected");
    	}
    }
    
    public TwixtCell[] getRack(TwixtChip forpiece) 
    {	switch(forpiece.color())
    	{
    	default: throw G.Error("not expecting %s",forpiece);
    	case Red: return(redChipPool);
    	case Black: return(blackChipPool);
    	}
    }
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private ChipStack droppedChipStack = new ChipStack();
    public CellStack history = new CellStack();
    public TwixtChip playerChip[] = { TwixtChip.red_peg,TwixtChip.black_peg };
    public boolean swapped = false;
    private StateStack stateStack = new StateStack();
    
    private TwixtState resetState = TwixtState.Puzzle; 
    public TwixtChip lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public TwixtCell newcell(char c,int r)
	{	return(new TwixtCell(TwixtId.BoardLocation,c,r));
	}
	
	// constructor 
    public TwixtBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = G.debug() ? DEBUGGRIDSTYLE : 
        		TWIXTGRIDSTYLE;
        Random rand = new Random(7028362);
        setColorMap(map);
        for(int i=blackChipPool.length-1; i>=0; i--)
        {	TwixtChip bc = TwixtChip.blackChips[i];
        	blackChipPool[i] = new TwixtCell(rand,bc.id,i);
        	blackChipPool[i].addChip(TwixtChip.blackChips[i]);
        	idHome.put(bc.id, blackChipPool[i]);
        	TwixtChip rc = TwixtChip.redChips[i];
        	redChipPool[i] = new TwixtCell(rand,rc.id,i);
           	redChipPool[i].addChip(rc);
           	idHome.put(rc.id,redChipPool[i]);
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
    	int map[] = getColorMap();
        playerRack[map[FIRST_PLAYER_INDEX]] = redChipPool;
        playerRack[map[SECOND_PLAYER_INDEX]] = blackChipPool;
        playerColor[map[FIRST_PLAYER_INDEX]] = PieceColor.Red;
        playerColor[map[SECOND_PLAYER_INDEX]] = PieceColor.Black;
        playerChip[map[FIRST_PLAYER_INDEX]] = TwixtChip.red_peg;
        playerChip[map[SECOND_PLAYER_INDEX]] = TwixtChip.black_peg;

    	players_in_game = players;
 		Random r = new Random(734687);	// this random is used to assign hash values to cells, common to all games of this type.
		setState(TwixtState.Puzzle);
		variation = TwixtVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		gametype = gtype;
		swapped = false;
		history.clear();
		redPegs.clear();
		blackPegs.clear();
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case ghost:
			ghost = true;
			//$FALL-THROUGH$
		case twixt:
			int siz = variation.boardSize;
			char last = (char)('A'+siz-1);
			initBoard(siz,siz);
			boardSize = siz;
			// corners removed
			unlinkCell(getCell('A',1));
			unlinkCell(getCell('A',siz));
			unlinkCell(getCell(last,1));
			unlinkCell(getCell(last,siz));
		}

		allCells.setDigestChain(r);		// set the randomv for all cells on the board
 		
		for(TwixtCell c = allCells; c!=null; c=c.next)
		{
			if(!((c.row>1) && (c.row<nrows))) { c.setElgible(PieceColor.Black); }
			if(!((c.col>'A') && (c.col-'A'+1<ncols))) { c.setElgible(PieceColor.Red); }
		}
		
	    whoseTurn = FIRST_PLAYER_INDEX;
	    droppedDestStack.clear();
	    pickedSourceStack.clear();
	    droppedChipStack.clear();
	    stateStack.clear();
	    
	    pickedObject = null;
	    resetState = null;
	    lastDroppedObject = null;
		
        animationStack.clear();
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }
    
    public int DRAW_THRESHOLD = 20;			// we'll report a draw likely if win requires more than this
    private boolean drawIsLikely = false;
    private boolean drawCacheValid = false;
    public boolean drawIsLikely()
    {	if(drawCacheValid) { return(drawIsLikely); }
    	switch(board_state)
    	{
    	default: break;
    	case OfferDraw: return true;
    	
    	case Play:
    	if(moveNumber>20)
    	{
    	int redpath = sweepForPossibleVictory(PieceColor.Red);
    	int blackpath = sweepForPossibleVictory(PieceColor.Black);
    	drawIsLikely = ((redpath<0) || (redpath>=DRAW_THRESHOLD))
    			&& ((blackpath<0) || (blackpath>=DRAW_THRESHOLD));
    	drawCacheValid = true;
    	return( drawIsLikely);
    	}}
    	return(false);
    }

    /** create a copy of this board */
    public TwixtBoard cloneBoard() 
	{ TwixtBoard dup = new TwixtBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((TwixtBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(TwixtBoard from_b)
    {
        super.copyFrom(from_b);
        AR.copy(playerColor,from_b.playerColor);
        getCell(history,from_b.history);
        robotState.copyFrom(from_b.robotState);
        board_state = from_b.board_state;
        getCell(droppedDestStack,from_b.droppedDestStack);
        droppedChipStack.copyFrom(from_b.droppedChipStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        swapped = from_b.swapped;
        lastPicked = null;
        robotBoard = from_b.robotBoard;
        getCell(redPegs,from_b.redPegs);
        getCell(blackPegs,from_b.blackPegs);
 
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((TwixtBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(TwixtBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(AR.sameArrayContents(playerColor, from_b.playerColor),"playerColors mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(swapped==from_b.swapped,"swapped mismatch");
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
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,revision);
		v ^= Digest(r,playerColor);
		v ^= Digest(r,swapped);
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
        case Confirm:
        case ConfirmSwap:
		case DeclineDraw:
		case AcceptDraw:
		case OfferDraw:
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
    {	return(win[player]);
    }
    
    // sweep for victory but ignore ghost components
    private boolean sweepForVictory(TwixtCell seed)
    {	
    	if((seed==null) || (seed.sweep_counter==sweep_counter) || seed.isEmptyOrGhost()) 
    		{ 
    		  return(false); 
    		}

    	seed.sweep_counter = sweep_counter;
    	if((seed.col-'A'+1 == ncols) || (seed.row==nrows)) 
    		{ return(true); }	// reached the opposite edge
    	for(int lim=seed.height()-1; lim>=1; lim--)
    	{	// follow outward links
    		TwixtChip bridge = seed.chipAtIndex(lim);
    		if(!bridge.isGhost())
    		{
    		TwixtCell to = seed.bridgeTo(bridge);
    		if(sweepForVictory(to)) { return(true); }
    		}
    	}
    	// find possible inward links
    	TwixtChip post = seed.chipAtIndex(0);
    	for(TwixtChip bridge : post.getBridges())	// these are non-ghost bridges
    	{
    		TwixtCell from = seed.bridgeFrom(bridge);
    		if((from!=null) && from.containsChip(bridge)) // only checks for non-ghost bridges
    			{ if(sweepForVictory(from)) { return(true); }
     			}
    	}
    	return(false);
    }
    
    // sweep for victory but consider ghost bridges to be connected
    private boolean sweepForGhostVictory(TwixtCell seed)
    {
    	if((seed==null) || (seed.sweep_counter==sweep_counter) || seed.isEmpty()) { return(false); }
    	seed.sweep_counter = sweep_counter;
    	if((seed.col-'A'+1 == ncols) || (seed.row==nrows)) { return(true); }	// reached the opposite edge
    	for(int lim=seed.height()-1; lim>=1; lim--)
    	{	// follow outward links
    		TwixtChip bridge = seed.chipAtIndex(lim);
     		TwixtCell to = seed.bridgeTo(bridge);
    		if(sweepForGhostVictory(to)) { return(true); }
    	}
    	// find possible inward links
    	TwixtChip post = seed.chipAtIndex(0);
    	for(TwixtChip bridge : post.getBridges())	// these are non-ghost bridges
    	{
    		TwixtCell from = seed.bridgeFrom(bridge);
    		if((from!=null) 
    				&& (from.containsChip(bridge)
    					|| from.containsChip(bridge.getGhosted()))) // only checks for non-ghost bridges
    			{ if(sweepForGhostVictory(from)) { return(true); }
     			}
    	}
    	return(false);
    }
    private boolean sweepForVictory(char seed_col,int seed_row,int direction)
    {
    	sweep_counter++;
    	TwixtCell seed = getCell(seed_col,seed_row);
    	while(seed!=null)
    	{	if(sweepForVictory(seed)) { return(true); }
    		seed = seed.exitTo(direction);
    	}
    	return(false);
    }
    private boolean sweepForGhostVictory(char seed_col,int seed_row,int direction)
    {
    	sweep_counter++;
    	TwixtCell seed = getCell(seed_col,seed_row);
    	while(seed!=null)
    	{	if(sweepForGhostVictory(seed)) { return(true); }
    		seed = seed.exitTo(direction);
    	}
    	return(false);
    }
   
    int sweep_work = 0;
    int first_direction = CELL_UP;
    // find the shortest path to victory. Allow using your own pegs but not opposing pegs
    private int sweepForPossibleVictory(PieceColor color,TwixtCell seed,int currentDis,int limit)
    {	sweep_work++;
    	if(seed==null) { return(-1); }		// can't get there
    	if(currentDis>=limit) { return(-1); }	// too far
    	if(!seed.isEmpty())
    	{
    		if(seed.topChip().color()!=color) { return(-1); }	// can't get there through the wrong color
    	}
    	switch(color)
    	{
    	default: throw G.Error("not expected");
    	case Red:
    		if(seed.col=='A') { return(-1); }
    		if(seed.col=='A'+ncols-1) { return(-1); }
    		if(seed.row==ncols) 
    			{ if(currentDis<10) { G.Error("Not possible");}
    			  return(currentDis); 	// made it!
    			}
    		break;
    	case Black:
    		if(seed.row==1) { return(-1); }
    		if(seed.row==ncols) { return(-1); }
    		if(seed.col=='A'+ncols-1) 
    			{ if(currentDis<10) { G.Error("Not possible"); }
    			  return(currentDis); 	// made it!
    			}
    		break;
    	}
    	
    	int dis = seed.sweep_counter==sweep_counter ? seed.samples : -1;
    	
    	//G.print("S "+seed+" "+currentDis+" "+dis);
    	       	
    	if(dis>=0)
    	{	// we've seen before
    		if(dis<=currentDis) { return(-1); }	// there's a shorter path
    	}
    	seed.sweep_counter = sweep_counter;
    	seed.samples = currentDis;
    	// try possible links
    	int bestDis = -1;
    	for(int direction=first_direction+CELL_FULL_TURN; direction>first_direction; direction -= CELL_QUARTER_TURN)
    	{	TwixtCell adj = seed.exitTo(direction);
    		if(adj!=null)
    		{
    		for(int dir2 = direction-1; dir2<=direction+1; dir2+=2)
    		{
    			TwixtCell next = adj.exitTo(dir2);
    			if(next!=null)
    			{	if(!crossingBridge(seed,next))
    				{
    				int nextDis = sweepForPossibleVictory(color,next,currentDis+1,limit);
    				if(nextDis>0)
    				{	if((bestDis<0) || (nextDis<bestDis)) { bestDis = nextDis; }
    				    if(bestDis<limit) { return(bestDis); }
    				}}
    				}
    			}
    		}}
    	return(bestDis);
    }
    
    private int sweepForPossibleVictory(PieceColor color)
    {
    	TwixtCell seed = getCell('B',2);
    	int direction = 0;
    	switch(color)
    	{
    	default: throw G.Error("Not expected");
    	case Red:
    		direction = CELL_RIGHT;
    		first_direction = CELL_UP;
    		break;
    	case Black:
    		direction = CELL_UP;
    		first_direction = CELL_RIGHT;
    		break;
    	}
    	int shortest = -1;
    	int limit = DRAW_THRESHOLD;
		sweep_work = 0;
    	while(seed!=null && ((shortest==-1) || (shortest>=limit)))
    	{   sweep_counter+=limit*2;
    		int newdis = sweepForPossibleVictory(color,seed,0,limit);
    		if(newdis>0 && ((newdis<shortest) || (shortest==-1)))
    			{ shortest = newdis;
    			}
    		seed = seed.exitTo(direction);
    	}
    	//G.print("sweep "+color+" "+shortest+" "+sweep_work);
    	return(shortest);
    }
    
    private boolean checkForWin(int who)
    {	sweep_counter++;
    	switch(playerColor[who])
    	{
    	case Red:
    		// connect red edges, top and bottom
    		return((ghost&&robotBoard) 
    				? sweepForGhostVictory('B',1,CELL_RIGHT) 	// robot games, check for virtual win using ghost pegs
    				: sweepForVictory('B',1,CELL_RIGHT));
   
    	case Black:
    		// connect black edges, left and right
    		return((ghost&&robotBoard) 
    				? sweepForGhostVictory('A',2,CELL_UP)		// robot games, check for virtual win using ghost pegs
    				: sweepForVictory('A',2,CELL_UP));
    	default: break;
    	}
    	return(false);
    }

    //
    // accept the current placements as permanent
    //
    public void acceptPlacement()
    {	lastDropped = droppedDestStack.top();
    	lastDroppedChip = droppedChipStack.top();
        droppedDestStack.clear();
        droppedChipStack.clear();
        pickedSourceStack.clear();
        stateStack.clear();
        drawCacheValid = false;
        pickedObject = null;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private TwixtCell unDropObject()
    {	TwixtCell rv = droppedDestStack.pop();
    	TwixtChip rc = droppedChipStack.pop();
    	setState(stateStack.pop());
    	if(rv.onBoard)
    	{
    	if(rc.isPeg())
    	{
    	removeBridges(rv);
    	pickedObject = removeChip(rv); 	// SetBoard does ancillary bookkeeping
    	if(ghost)
    	{
    		addGhostPegsAndLinks(rv);
    	}
    	}
    	else {
    		rv.removeChip(rc);
    		pickedObject = rc;
    	}
    	rv.lastPlaced = previousLastPlaced;
    	previousLastPlaced = -1;
    	}
    	else { pickedObject = rv.topChip(); }
    	return(rv);
    }
    void addChip(TwixtCell rv,TwixtChip po)
    {
    	G.Assert(po.isBridge()||rv.isEmpty(), "can't have two pegs");
    	rv.addChip(po);
    	if(po.isPeg())
    		{switch(po.color())
    			{
    			case Red: redPegs.push(rv); break;
    			case Black: blackPegs.push(rv); break;
    			default: throw G.Error("Not expected");
    		}}
    }
    private TwixtChip removeChip(TwixtCell rv)
    {
    	TwixtChip po = rv.removeTop();
    	if(po.isPeg())
    		{rv.blob = null;
    		 switch(po.color())
    			{
    			case Red: redPegs.remove(rv,false); break;
    			case Black: blackPegs.remove(rv,false); break;
    			default: throw G.Error("Not expected");
    		}}
    	return(po);
    }
    int previousLastPlaced = -1;
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	TwixtCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	if(rv.onBoard)
    	{
    		addChip(rv,pickedObject);
    		rv.lastPlaced = previousLastPlaced;
    	}
    	if(ghost)
    	{
    		addGhostPegsAndLinks(null);
    	}
    	
    	pickedObject = null;
    }

    private void removeBridges(TwixtCell c)
    {	
    	while(c.height()>1) { c.removeTop(); }
    	if(!c.isEmpty())
    	{
    	TwixtChip post = c.topChip();
    	for(TwixtChip bridge : post.getBridges())
    	{
    		TwixtCell from = c.bridgeFrom(bridge);
    		if(from!=null)
    		{
    			from.removeChip(bridge);
    			from.removeChip(bridge.getGhosted());
    		}
    	}}
    }
    public TwixtCell getCell(TwixtChip ch)
    {	TwixtCell c = idHome.get(ch.id);
    	if(c!=null) { return(c); }
    	throw G.Error("Not expecting %s",ch);
    }
    
    private void addPossibleBridgesFrom(TwixtCell c,TwixtChip post,Twixtmovespec m,replayMode replay)
    {
    	for(TwixtChip bridge0 : post.getBridges())
    	{
    		TwixtCell to = c.bridgeTo(bridge0);
    		if((to!=null) && legalToPlaceBridge(c,bridge0))
    			{ 
        		  TwixtChip bridge = bridge0;
    			  if(to.chipAtIndex(0).isGhost() || post.isGhost()) { bridge = bridge.getGhosted(); }
    			  if(!c.containsChip(bridge))
    			  {
    			  addChip(c,bridge);
    			  if(!robotBoard && (m!=null)) { m.addTarget(bridge); } 
               	  if(replay!=replayMode.Replay)
               	  {
            		animationStack.push(getCell(bridge0));
            		animationStack.push(c);
               	  }}
    			}
    	}
    }
    private void addPossibleBridgesTo(TwixtCell c,TwixtChip post,Twixtmovespec m,replayMode replay)
    {
    	for(TwixtChip bridge0 : post.getBridges())
    	{
    		TwixtCell from = c.bridgeFrom(bridge0);
    		if((from!=null) && legalToPlaceBridge(from,bridge0)) 
    			{        
    			TwixtChip bridge = bridge0;
  			  	if(from.chipAtIndex(0).isGhost() || post.isGhost()) { bridge = bridge.getGhosted(); }
  			  	if(!from.containsChip(bridge))
  			  	{
  			  	addChip(from,bridge);
    			if(!robotBoard && (m!=null)) { m.addTarget(bridge); } 
             	  if(replay!=replayMode.Replay)
             	  {
             		  animationStack.push(getCell(bridge0));
             		  animationStack.push(from);
             	  }
  			  	} 
    		}   		
    		}
    }
    private void addPossibleBridges(TwixtCell c,TwixtChip post,Twixtmovespec m,replayMode replay)
    {	
    	addPossibleBridgesFrom(c,post,m,replay);
    	addPossibleBridgesTo(c,post,m,replay);
    }
    // 
    // drop the floating object.
    //
    private void dropObject(Twixtmovespec m,TwixtCell c,replayMode replay)
    {
       droppedDestStack.push(c);
       droppedChipStack.push(pickedObject);
       stateStack.push(board_state);
       
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("not expecting dest %s", c.rackLocation);
        case Red_Peg:
        case Black_Peg:
        case Red_Bridge_30:
        case Red_Bridge_60:
        case Red_Bridge_120:
        case Red_Bridge_150:
        case Black_Bridge_30:
        case Black_Bridge_60:
        case Black_Bridge_120:
        case Black_Bridge_150:
        case Black_Chip_Pool:
        case Red_Chip_Pool:		// back in the pool, we don't really care where
        	pickedObject = null;
        	
            break;
        case BoardLocation:	// already filled board slot, which can happen in edit mode
           	TwixtChip po = pickedObject;
            lastDroppedObject = po;
            m.target = po;
            pickedObject = null;
            if(po.isPeg())
            {	c.removeGhosts();
            	addChip(c,po);
            	addPossibleBridges(c,po,m,replay);
                if(ghost)
             	{	addGhostPegsAndLinks(c);
             	}
            }
            else { c.addChip(po); }
            previousLastPlaced = c.lastPlaced;
            c.lastPlaced = moveNumber;
            break;
        }
     }
    private void removeGhostsNear(TwixtCell c)
    {
    	if(c==null) { for(TwixtCell cell = allCells; cell!=null; cell=cell.next) { cell.removeGhosts(); }}
    	else 
    	{
    	for(int colNum = Math.max(0,c.col-'A'-4),lastCol=Math.min(c.col-'A'+4,ncols-1);
    			colNum<=lastCol;
    			colNum++)
    	{ for(int rowNum = Math.max(1, c.row-4),lastRow = Math.min(c.row+4, nrows);
    			rowNum<=lastRow;
    			rowNum++)
    		{
    		TwixtCell cell = getCell((char)('A'+colNum),rowNum);
    		if(cell!=null)
    			{
    			cell.removeGhosts();
    			}
    		}
    	}}
    }
    private void addGhostsNear(TwixtCell c)
    {
    	if(c==null)
    	{
    		for(TwixtCell cell = allCells; cell!=null; cell=cell.next)
    		{
    			if(!cell.isEmpty()) 
    			{ addPossibleBridgesFrom(cell,cell.chipAtIndex(0),null,replayMode.Replay); 
    			}
    		}
    	}
    	else {
    	for(int colNum = Math.max(0,c.col-'A'-4),lastCol=Math.min(c.col-'A'+4,ncols-1);
    			colNum<=lastCol;
    			colNum++)
    	{ for(int rowNum = Math.max(1, c.row-4),lastRow = Math.min(c.row+4, nrows);
    			rowNum<=lastRow;
    			rowNum++)
    		{
    		TwixtCell cell = getCell((char)('A'+colNum),rowNum);
    		if((cell!=null)&&!cell.isEmpty())
    		{	TwixtChip ch = cell.chipAtIndex(0);
    			addPossibleBridgesFrom(cell,ch,null,replayMode.Replay); }
    		}
    	}}
    }
    private void addGhostPegsAndLinks(TwixtCell c)
    {
    	// first remove all the ghost pegs nearby
    	removeGhostsNear(c); 	
    	
    	addGhostPegs(PieceColor.Red);
    	addGhostPegs(PieceColor.Black);

    	
    	addGhostsNear(c); 	
    }
    private void addGhostPegs(PieceColor color)
    {
    	BlobStack blobs = findRealBlobs(color);
    	// mark the possible expansion points
    	sweep_counter++;
    	for(int lim=blobs.size()-1; lim>=0; lim--)
    	{
    		markExpansion(blobs.elementAt(lim),true);
    	}
    }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(TwixtCell c,TwixtChip chip)
    {	return((droppedDestStack.top()==c ) && ((chip==null)||(droppedChipStack.top()==chip)));
    }
    
    public TwixtCell getLastDest() 
    { 	TwixtCell d = droppedDestStack.top();
    	if(d==null) { d = lastDropped; }
    	return(d);
    }
    public TwixtChip getLastDestChip() 
    { 	TwixtChip d = droppedChipStack.top();
    	if(d==null) { d = lastDroppedChip; }
    	return(d);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { TwixtChip ch = pickedObject;
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
    private TwixtCell getCell(TwixtId source, char col, int row)
    {
        switch (source)
        {
        default:
        	{
        		TwixtCell c = idHome.get(source);
        		if(c!=null) { return(c); }
        	}
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	return(getCell(col,row));
        case Black_Chip_Pool:
        	return(blackChipPool[row]);
        case Red_Chip_Pool:
        	return(redChipPool[row]);
        } 	
    }
    public TwixtCell getCell(TwixtCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(TwixtCell c,TwixtId obj)
    {	pickedSourceStack.push(c);
    	stateStack.push(board_state);
    	if(c.onBoard)
    	{
    		{
    			if(obj==null)
    				{ 
    				removeBridges(c);
    	            lastPicked = pickedObject = removeChip(c);
    				}
    			else { 
    				lastPicked = pickedObject = c.removeChip(TwixtChip.getChip(obj));
    				if(pickedObject.isPeg())
    				{
    				switch(pickedObject.color()) {
    				case Red: redPegs.remove(c,false); break;
    				case Black: blackPegs.remove(c, false); break;
    				default: throw G.Error("Not expected");
    				}
    				}
    				}
            	}
    		if(ghost) { addGhostPegsAndLinks(c);}
    	}
    	else
    	{
    		lastPicked = pickedObject = c.topChip();
    	}
    	
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(TwixtCell c)
    {	return(c==pickedSourceStack.top());
    }
    public TwixtCell getSource() { return(pickedSourceStack.top()); }
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
         	break;
        case PlayOrSwap:
        case Play:
			setState(TwixtState.Confirm);
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
    	default: throw G.Error("Not expecting after Done state %s",board_state);
    	
        case OfferDraw:
        	setState(TwixtState.QueryDraw);
        	break;
        case AcceptDraw:
        	setState(TwixtState.Gameover);
        	break;
    	case Gameover: break;
        case DeclineDraw:
    	case Confirm:
    	case Puzzle:
    	case Play:
    	case PlayOrSwap:
    	case ConfirmSwap:
    		setState( ((moveNumber==2)&&!swapped)
    					? TwixtState.PlayOrSwap 
    					: TwixtState.Play);
    		
    		break;
    	}
       	resetState = board_state;
    }
    private void doDone(replayMode replay)
    {	TwixtCell c = getLastDest();
    	TwixtChip p = getLastDestChip();
    	if(c!=null && p!=null && p.isPeg()) { history.push(c); }
    	
        acceptPlacement();

        if (board_state==TwixtState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(TwixtState.Gameover);
        }
        else
        {	if(checkForWin(whoseTurn)) 
        		{ win[whoseTurn]=true;
        		  setState(TwixtState.Gameover); 
        		}
        	else {setNextPlayer(replay);
        		setNextStateAfterDone(replay);
        	}
        }
    }
    
    private void doSwap()
    {	PieceColor p = playerColor[0];
    	playerColor[0] = playerColor[1];
    	playerColor[1] = p;
    	TwixtCell r[] = playerRack[0];
    	playerRack[0] = playerRack[1];
    	playerRack[1] = r;
    	
    	swapped = !swapped;
    	setState(swapped ? TwixtState.ConfirmSwap : TwixtState.PlayOrSwap);
    }

    public boolean Execute(commonMove mm,replayMode replay)
    {	Twixtmovespec m = (Twixtmovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }

        //G.print("E "+m+" for "+whoseTurn+" "+state);
        switch (m.op)
        {

        case MOVE_OFFER_DRAW:
        {
        	TwixtState bs = board_state;
        	if(bs==TwixtState.OfferDraw)
        	{	setState(stateStack.pop());
        	}else
        	{
        		stateStack.push(bs);
        		setState(TwixtState.OfferDraw);
        	}}
        break;
        	
        case MOVE_ACCEPT_DRAW:
         	setState(board_state==TwixtState.AcceptDraw?TwixtState.QueryDraw:TwixtState.AcceptDraw);
        	break;
        	
        case MOVE_DECLINE_DRAW:
        	setState(board_state==TwixtState.DeclineDraw?TwixtState.QueryDraw:TwixtState.DeclineDraw);
        	break;

        case MOVE_DONE:

         	doDone(replay);

            break;
        case MOVE_FROM_TO:
        	{
        	TwixtCell from = getCell(m.source,m.from_col,m.from_row);
        	TwixtCell to = getCell(m.dest,m.to_col,m.to_row);
        	pickObject(from,null);
        	dropObject(m,to,replay);
        	
        	setNextStateAfterDrop(replay);
        	}
        	break;
        case MOVE_DROP:
        case MOVE_DROPB:
        	{
			TwixtCell dest =  getCell(m.dest,m.to_col,m.to_row);
			if(isSource(dest)) 
				{ unPickObject(); 
				}
				else 
				{
				if(pickedObject==null) 
					{ pickObject(getPeg(whoseTurn),null); 
					}

				TwixtChip po = pickedObject;
	            dropObject(m,dest,replay);
	            /**
	             * if the user clicked on a board space without picking anything up,
	             * animate a stone moving in from the pool.  For Hex, the "picks" are
	             * removed from the game record, so there are never picked stones in
	             * single step replays.
	             */
	            if(replay!=replayMode.Replay && (po==null))
	            	{ animationStack.push(getSource());
	            	  animationStack.push(dest); 
	            	}
	            if(po.isPeg()) { setNextStateAfterDrop(replay); }
				}
        	}
             break;

        case MOVE_PICK:
 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
 			TwixtCell src = getCell(m.source,m.from_col,m.from_row);
 			TwixtChip ch = TwixtChip.getChip(m.dest);
 			if(isDest(src,ch)) { unDropObject(); }
 			else
 			{
        	// be a temporary p
        	pickObject(src,m.dest);
        	m.target = pickedObject;
 			}}
            break;

        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            int nextp = nextPlayer[whoseTurn];
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(TwixtState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ setState(TwixtState.Gameover); 
               	}
            else {  setNextStateAfterDone(replay); }
            acceptPlacement();
            break;

       case MOVE_RESIGN:
    	    if(board_state==TwixtState.Resign) { setState(stateStack.pop()); }
    	    else { stateStack.push(board_state);
    	           setState(TwixtState.Resign);
    	    	}
            break;
        case MOVE_EDIT:
        	acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(TwixtState.Puzzle);
 
            break;
        case MOVE_SWAP:
        	doSwap();
        	break;
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(TwixtState.Gameover);
			break;
        default:
        	cantExecute(m);
        }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }

    // legal to hit the chip storage area
    public boolean legalToHitChips(TwixtCell c,int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting Legal Hit state %s", board_state);
		case Resign:
		case Gameover:
		case ConfirmSwap:
		case QueryDraw:
		case DeclineDraw:
		case AcceptDraw:
		case OfferDraw:
			return(false);
        case Confirm:
        case PlayOrSwap:
        case Play:
        	if(c.topChip().isPeg())
        	{
        		switch(board_state)
        		{
        		case Play:
        		case PlayOrSwap:
        			break;
        		default: return(false);
        		}
        	}
        	return((pickedObject!=null)
        				?(pickedObject.color()==getPlayerColor(whoseTurn))
        				:c.topChip().color()==getPlayerColor(whoseTurn));
        case Puzzle:
            return ((pickedObject!=null)?(pickedObject.color()==c.topChip().color()):true);
        }
    }
    public boolean legalToPickBridge()
    {
    	switch(board_state)
    	{
		case QueryDraw:
		case DeclineDraw:
		case AcceptDraw:
		case OfferDraw:
    	case Gameover: return(false);
    	default: return(true);
    	}
    }
    public boolean legalToHitBoard(TwixtCell c)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
        case PlayOrSwap:
		case Play:
			if(!c.isElgible(getPlayerColor(whoseTurn))) { return(false); }
			return(c.isEmptyOrGhost());
		case Gameover:
		case Resign:
		case ConfirmSwap:
		case QueryDraw:
		case DeclineDraw:
		case AcceptDraw:
		case OfferDraw:
			return(false);
		case Confirm:
			return(isDest(c,null));
        default:
        	throw G.Error("Not expecting Hit Board state %s", board_state);
        case Puzzle:
        	if(pickedObject==null)
        	{
        		return(!c.isEmpty());
        	}
        	else
        	{
        	switch(pickedObject.id)
        		{
        		default: throw G.Error("Not expecting %s",pickedObject);
        		case Red_Peg:
        		case Black_Peg:	return(c.isEmptyOrGhost());
        		
        		case Red_Bridge_30:
        		case Red_Bridge_60:
        		case Red_Bridge_120:
        		case Red_Bridge_150:
        		case Black_Bridge_30:
        		case Black_Bridge_60:
        		case Black_Bridge_120:
        		case Black_Bridge_150:
        			return legalToPlaceBridge(c,pickedObject);
       		}
        	}
        }
     }
    
    private boolean get_line_intersection(int p0_x, int p0_y, int p1_x, int p1_y, 
    		int p2_x, int p2_y, int p3_x, int p3_y)
    	{
    	int s1_x, s1_y, s2_x, s2_y;
    	    s1_x = p1_x - p0_x;     s1_y = p1_y - p0_y;
    	    s2_x = p3_x - p2_x;     s2_y = p3_y - p2_y;

    	double s, t;
    	    s = (-s1_y * (p0_x - p2_x) + s1_x * (p0_y - p2_y)) / (double)(-s2_x * s1_y + s1_x * s2_y);
    	    t = ( s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) / (double)(-s2_x * s1_y + s1_x * s2_y);

    	    if (s >= 0 && s <= 1 && t >= 0 && t <= 1)
    	    {
    	        return true;
    	    }

    	    return false; // No collision
    	}
    //
    // return true if adding a bridge from - to would cross an existing bridge
    //
    public boolean crossingBridge(TwixtCell from,TwixtCell to)
    {
    	int left = Math.max(1, Math.min(from.col, to.col)-1);
    	int right = Math.min('A'+ncols, Math.max(from.col, to.col)+1);
    	int top = Math.max(1, Math.min(from.row, to.row)-1);
    	int bottom = Math.min(nrows,Math.max(from.row,to.row));
    	
    	for(int thisColnum = left;thisColnum<=right;thisColnum++)
    	{
    		for(int row = top; row<=bottom;row++)
    		{
    			TwixtCell c = getCell((char)thisColnum,row);
    			if(c!=null && (c!=from) && (c!=to))
    			{	for(int height = 1,lim=c.height()-1; 
    					height<=lim;
    					height ++)
    				{ TwixtChip ch = c.chipAtIndex(height);
    				  if(!ch.isGhost())
    				  {
    				  TwixtCell d = c.bridgeTo(ch);
    				  if(d!=null && (d!=from) && (d!=to))
    				  {
    				  boolean in = get_line_intersection(c.col,c.row,d.col,d.row,
    						  from.col,from.row,to.col,to.row);
    				  if(in) 
    				  { return(true); }
    				  }}
    				}
    			}
    		}
    	}
    	return(false);
    }
    public boolean illegalBridge(TwixtCell c)
    {
    	for(int height=1,lim=c.height()-1; height<=lim; height++)
    	{
    		TwixtChip chip = c.chipAtIndex(height);
    		TwixtCell d = c.bridgeTo(chip);
    		if(d!=null && crossingBridge(c,d)) { return(true); }
    	}
    	return(false);
    }
    public boolean legalToPlaceBridge(TwixtCell from,TwixtChip ch)
    {
    	if(!from.isEmpty() && (from.topChip().color()==ch.color()))
		{
		TwixtCell other = from.bridgeTo(ch);
		return((other!=null)
				&& !other.isEmpty()
				&& (other.topChip().color()==ch.color())
				&& !crossingBridge(from,other)
				);
		}
    	return(false);
    }
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Twixtmovespec m)
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
    // un-execute a move.  The move should only be un executed
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(Twixtmovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	TwixtState state = robotState.pop();
        switch (m.op)
        {
        default:
   	    	throw G.Error("Can't un execute %s", m);
        case MOVE_DONE:
        case MOVE_SWAP:
        	doSwap();
            break;
            
        case MOVE_DROPB:
        	{
        	TwixtCell dest = getCell(m.to_col,m.to_row);
        	removeBridges(dest);
        	removeChip(dest);
        	TwixtCell undrop = history.pop();
        	G.Assert(dest==undrop,"remove history");
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

 commonMove Get_Random_Move(Random ran)
 {	 if(history.size()>=2)
 	{
	 PieceColor color = getPlayerColor(whoseTurn);
	 // add moves adjacent to the most recent moves
	 int lim = history.size()-1;
	 TwixtCell c1 = history.elementAt(lim);
	 int colNum1 = Math.max('A',c1.col-2);
	 int lastCol1 = Math.min('A'+ncols-1, c1.col+2);
	 int row1 = Math.max(1, c1.row-2);
	 int lastRow1 = Math.min(nrows-1,c1.row+2);
	 int nrows1 = (lastRow1-row1+1);
	 
	 TwixtCell c2 = history.elementAt(lim-1);
	 int colNum2 = Math.max('A',c2.col-2);
	 int lastCol2 = Math.min('A'+ncols-1, c2.col+2);
	 int row2 = Math.max(1, c2.row-2);
	 int lastRow2 = Math.min(nrows-1,c2.row+2);
	 int nrows2 = (lastRow2-row2+1);
	 int range1 = (lastCol1-colNum1+1)*nrows1;
	 int range2 = (lastCol2-colNum2+1)*nrows2;
	 for(int tries = 0;tries<10; tries++)
	 {	int rv = ran.nextInt(range1+range2);
	 	char col;
	 	int row;
		if(rv<range1)
		{	col = (char)(colNum1 + range1/nrows1);
			row = row1 + range1%nrows1;
		}
		else {
			int range3 = rv-range1;
			col = (char)(colNum2 + range3/nrows2);
			row = row1 + range3%nrows2;
		}
		TwixtCell target = getCell((char)col,row);
		if((target!=null)
				&& target.isElgible(color)
				&& target.isEmptyOrGhost())
			{	commonMove m = new Twixtmovespec(MOVE_DROPB,target.col,target.row,whoseTurn);
				return(m);
			}
	 }
 	}

	 return(null);
 }

 public CommonMoveStack  GetListOfMoves()
 {
	 return(getListOfMoves(whoseTurn));
 }
 public CommonMoveStack getListOfPlausibleMoves(int who,CommonMoveStack plausible,boolean include)
 {	Hashtable<TwixtCell,commonMove>used = new Hashtable<TwixtCell,commonMove>();
 	CommonMoveStack all = new CommonMoveStack();
 	PieceColor color = getPlayerColor(who);
 	// add moves adjacent to the most recent moves
 	for(int lim = history.size()-1,idx = lim-2; idx<lim; idx++)
 	{	TwixtCell c = history.elementAt(idx);
 		for(int colNum = Math.max('A',c.col-2), lastCol = Math.min('A'+ncols-1, c.col+2); colNum<=lastCol; colNum++)
 		{
 			for(int row = Math.max(1, c.row-2),lastRow = Math.min(nrows-1,c.row+2); row<=lastRow; row++)
 			{	TwixtCell target = getCell((char)colNum,row);
 				if((target!=null)
 						&& (used.get(target)==null)
 						&& target.isElgible(color)
 						&& target.isEmptyOrGhost())
 				{	commonMove m = new Twixtmovespec(MOVE_DROPB,target.col,target.row,who);
 					all.push(m);
 					used.put(target,m);
 				}
  			}
 		}
 	}
 	if(include && (plausible!=null))
 	{
 	// add moves that are seen are plausible
 	for(int i=0;i<Math.min(50, plausible.size()); i++)
 	{
 		Twixtmovespec p = (Twixtmovespec)plausible.elementAt(i);
 		TwixtCell target = getCell(p.to_col,p.to_row);
 		if((target!=null)
 						&& (used.get(target)==null)
 						&& target.isElgible(color)
 						&& target.isEmptyOrGhost())
 			{
 			commonMove m = new Twixtmovespec(MOVE_DROPB,target.col,target.row,who);
 			all.push(m);
 			used.put(target,m);
 			}
 	}}
 			
 	return(all);
 }
 public CommonMoveStack getListOfMoves(int who)
 {	CommonMoveStack all = new CommonMoveStack();
 	switch(board_state)
 	{
 	case PlayOrSwap:
 		all.addElement(new Twixtmovespec(MOVE_SWAP,who));
		//$FALL-THROUGH$
	case Play:
	PieceColor color = getPlayerColor(who);
 	for(TwixtCell c = allCells; c!=null; c=c.next)
 	{
 		if(c.isElgible(color) && c.isEmptyOrGhost())
 		{all.addElement(new Twixtmovespec(MOVE_DROPB,c.col,c.row,who));
 		}
 	}
 		break;
 	default: G.Error("Not expecting state %s",board_state);
 	}
 	if(all.size()==0) { all.addElement(new Twixtmovespec(MOVE_RESIGN,who)); }
 	return(all);
 }
 


 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(Character.isDigit(txt.charAt(0)))
 	{ switch(variation)
	 		{
	 		case twixt:
	 		case ghost:
	 			xpos -= cellsize/2;
	 			break;
 			default: G.Error("case %s not handled",variation);
	 		}
	 	}
 		else
 		{ 
 		 if((boardRect!=null) && (ypos>G.centerY(boardRect))) {  ypos += cellsize/4; }
 		}
 	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
 }
 public double scoreForPlayer0(int pl,boolean print)
 {
	 CombinedBlobStack blobs = findBlobs(pl);
	 double score=0;
	 double maxSpan=0;
	 double secondSpan=0;
	 for(int lim=blobs.size()-1; lim>=0; lim--)
	 {	
		 CombinedTwixtBlob blob = blobs.elementAt(lim);
		 double span = blob.span();
		 if(span>maxSpan) { secondSpan = maxSpan; maxSpan=span; }
		 if(print) { G.print(blob); }
	 }
	 score += maxSpan*100+secondSpan;
	 return(score);
 }

 public double scoreForPlayer1(int pl,boolean print)	// add edge awareness
 {
	 CombinedBlobStack blobs = findBlobs(pl);
	 double score=0;
	 double maxSpan=0;
	 double secondSpan=0;
	 boolean leftEdge = false;
	 boolean rightEdge = false;
	 for(int lim=blobs.size()-1; lim>=0; lim--)
	 {	
		 CombinedTwixtBlob blob = blobs.elementAt(lim);
		 // 1/blob.ncomponents punishes blobs made of extra unconnected pegs.
		 // so the evaluation will prefer simply connected spans.
		 double span = blob.span()+1.0/blob.ncomponents;
		 if(span>maxSpan) 
		 	{ secondSpan = maxSpan; maxSpan=span; 
		 	}
		 leftEdge |= blob.hasLeftEdge();
		 rightEdge |= blob.hasRightEdge();
	 }
	 score += maxSpan*100+secondSpan;
	 if(leftEdge) { score += 40; }
	 if(rightEdge) { score += 40; }
	 return(score);
 }
 
 public CommonMoveStack buildPlausibleMoveSet(int playerIndex)
 {	if(board_state==TwixtState.Play)
 	{int realWho = whoseTurn;
	 CommonMoveStack all = getListOfMoves(playerIndex);
	 int otherPlayer = nextPlayer[playerIndex];
	 PieceColor otherColor = getPlayerColor(otherPlayer);
	 double startingValue = Static_Evaluate_Position2(playerIndex,false);
	 for(int lim=all.size()-1; lim>=0; lim--)
	 {	Twixtmovespec m = (Twixtmovespec)all.elementAt(lim);
	 	whoseTurn = playerIndex;
		TwixtCell pos = getCell(m.to_col,m.to_row);
		RobotExecute(m);
		double iMove = Static_Evaluate_Position2(playerIndex,false);
		double val = iMove-startingValue;
		UnExecute(m);
		if(pos.isElgible(otherColor))
		{	whoseTurn = otherPlayer;
			Twixtmovespec m2 = new Twixtmovespec(MOVE_DROPB,pos.col,pos.row,otherPlayer);
			RobotExecute(m2);
			double heMoves =  Static_Evaluate_Position2(playerIndex,false);
			val -= heMoves-iMove;
			UnExecute(m2);
		}
		whoseTurn = realWho;
		m.setEvaluation( val);

	 }
	 all.sort(false);
	 resetDistances(-1);
	 for(int lim=all.size()-1; lim>=0; lim--)
	 {	
		 Twixtmovespec m = (Twixtmovespec)all.elementAt(lim);
		 TwixtCell c = getCell(m.to_col,m.to_row);
		 c.samples = lim;
	 }
	 
	 return(all);
 	}
 	return(null);
 }
 //
 // this evaluator uses a "unbridged distance" metric, pegs in a blob are considered
 // to be all the same distance from the goal, so measure the actual number of new pegs
 // to be added to complete the crossing.
 //
public double Static_Evaluate_Position2(int playerIndex,boolean print)
{	if(WinForPlayer(playerIndex)) { return(TwixtPlay.VALUE_OF_WIN); }
	if(WinForPlayer(nextPlayer[playerIndex])) { return(-TwixtPlay.VALUE_OF_WIN); }
	
	//long dig = Digest();
	PieceColor myColor = getPlayerColor(playerIndex);
	PieceColor hisColor = getPlayerColor(nextPlayer[playerIndex]);
	BlobStack myBlobs = findRealBlobs(myColor);
	BlobStack hisBlobs = findRealBlobs(hisColor);
	CellStack hisExtras = addExpansion(hisBlobs);
	CellStack myExtras = addExpansion(myBlobs);
	
	
	double val1 = scoreForPlayer2(hisColor,true)+(1.0-1.0/(myExtras.size()+1));
	double val0 = scoreForPlayer2(myColor,true)+(1.0-1.0/(hisExtras.size()+1));
	
	removeExpansion(myExtras);
	removeExpansion(hisExtras);
	//long dig2 = Digest();
	//G.Assert(dig==dig2,"digest changed");
	if(print)
	{	System.out.println("Steps "+findDistanceSteps);
        System.out.println("Eval is "+ val0 +" "+val1+ " = " + (val0-val1));
	}
	return(val0-val1);
}
 public void removeExpansion(CellStack cells)
 {
	 for(int lim=cells.size()-1; lim>=0; lim--)
	 {
		 TwixtCell c = cells.elementAt(lim);
		 removeBridges(c);
		 removeChip(c);
	 }
 }
 public double scoreForPlayer2(PieceColor color,boolean print)	// add edge awareness
 {	
	 double score= ncols-findDistanceToEdge(color);		// small numbers are better

	 return(score);

 }
 private int meetExpansion(CombinedTwixtBlob combo,TwixtBlob blob,TwixtBlob other)
 {	
	int links = 0;
	if(combo.potentiallyConnects(other))
	{
 	CellStack cells = other.cells;
	for(int lim=cells.size()-1; lim>=0; lim--)
	 {
		 TwixtCell cell = cells.elementAt(lim);
		 for(int direction = CELL_UP+CELL_FULL_TURN; direction>CELL_UP; direction-= CELL_QUARTER_TURN)
		 {
			 TwixtCell adj = cell.exitTo(direction);
			 if(adj!=null)
			 {
				 for(int addDir = -1; addDir<=1; addDir+=2)
				 {
					 TwixtCell target = adj.exitTo(direction+addDir);
					 if((target!=null) 
							 && (target.sweep_counter==sweep_counter)	// marked by markExpansion
							 && target.isEmpty() 
							 && target.isElgible(blob.color())
							 && !crossingBridge(cell,target)
							 )
					 {
						links++;
						blob.addConnection(other, target);
						other.addConnection(blob,target);
						//G.print("Connect "+cell+" "+target);
					 }
				 }
			 }
		 }
	 }}
	return(links);
 }
 
 // mark the expansion spots for this blob. That is, the 
 // points where would could expand by 1 bridge.
 // also, note if none of the expansion points actually expand
 // in the desired directions
 private void markExpansion(TwixtBlob blob,boolean placeGhosts)
 {	
 	CellStack cells = blob.cells;
 	PieceColor color = blob.color();
 	for(int lim=cells.size()-1; lim>=0; lim--)
	 {
		 TwixtCell cell = cells.elementAt(lim);
		 for(int direction = CELL_UP+CELL_FULL_TURN; direction>CELL_UP; direction-= CELL_QUARTER_TURN)
		 {
			 TwixtCell adj = cell.exitTo(direction);
			 if(adj!=null)
			 {
				 for(int addDir = -1; addDir<=1; addDir+=2)
				 {
					 TwixtCell target = adj.exitTo(direction+addDir);
					 if((target!=null) 
							 && target.isEmpty() 
							 && target.isElgible(color)
							 && !crossingBridge(cell,target))
					 {
						 if(placeGhosts 
								 && (target.sweep_counter == sweep_counter)
								 && !(target.blob==blob)
								 )
						 {	TwixtChip peg = blob.color.getGhostPeg();
							 addChip(target,peg);
						 }
						 target.blob = blob;
						 target.sweep_counter = sweep_counter; 
						 switch(color)
						 {	default: throw G.Error("Not expected");
						   	case Red:
					    		if(target.row<=2) { blob.hasLeftEdge++; }
					    		if(target.row+1>=ncols) { blob.hasRightEdge++; }
					    		break;
					    	case Black:
					    		if(target.col<='B') { blob.hasLeftEdge++; }
					    		if((target.col-'A'+2)>=ncols) { blob.hasRightEdge++; }
					    		break;
						 }
					 }
				 }
			 }
		 }
	 }
 }
 
 private CellStack addExpansion(BlobStack blobs)
 {	
	 CellStack added = new CellStack();

	 sweep_counter++;
	 
	 for(int lim=blobs.size()-1; lim>=0; lim--)
	 {	TwixtBlob current = blobs.elementAt(lim);
	 	addExpansion(current,added,0,false);
	 }
	 return(added);
 }
 // mark the expansion spots for this blob. That is, the 
 // points where would could expand by 1 bridge.
 // also, note if none of the expansion points actually expand
 // in the desired directions
 private boolean addExpansion(TwixtBlob blob,CellStack added,int depth,boolean toEdge)
 {	int count = 0;
 	CellStack cells = blob.cells;
 	PieceColor color = blob.color();
 	for(int lim=cells.size()-1; lim>=0; lim--)
	 {
		 TwixtCell cell = cells.elementAt(lim);
		 for(int direction = CELL_UP+CELL_FULL_TURN; direction>CELL_UP; direction-= CELL_QUARTER_TURN)
		 {
			 TwixtCell adj = cell.exitTo(direction);
			 if(adj!=null)
			 {
				 for(int addDir = -1; addDir<=1; addDir+=2)
				 {
					 TwixtCell target = adj.exitTo(direction+addDir);
					 if((target!=null) 
							 && target.isEmpty() 
							 && target.isElgible(color)
							 && !crossingBridge(cell,target))
					 {
						 if( (target.sweep_counter == sweep_counter)
							&& !(target.blob==blob)
								 )
						 {	TwixtBlob toBlob = target.blob;
						 	if(toBlob!=null)
						 	{
						 	if(toBlob.connectFrom == blob)
						 	{
							 TwixtChip peg = color.getPeg();
							 addChip(target,peg);
							 added.push(target);
							 count++; 
							 addPossibleBridges(target,peg,null,replayMode.Replay);
						 	}else
						 	{	// note 1 connection, try for 2
						 		toBlob.connectFrom = blob;
						 	}}
						 }
						 else { 
							 target.blob = blob;
							 target.sweep_counter = sweep_counter;
							 if((depth>0))
								 {boolean some = addExpansion(blob,added,depth-1,toEdge);
								  if(some && target.isEmpty())
								  {
									 TwixtChip peg = color.getPeg();
									 addChip(target,peg);
									 target.sweep_counter = 0;
									 added.push(target);
									 count++; 
									 addPossibleBridges(target,peg,null,replayMode.Replay);
  
								  }
								 }
						 }
						 boolean hasEdge = false;
						 switch(color)
						 {	default: throw G.Error("Not expected");
						   	case Red:
					    		if(target.row<=2) { blob.hasLeftEdge++;  hasEdge=true;	}
					    		if(target.row+1>=ncols) { blob.hasRightEdge++;   hasEdge = true;}
					    		break;
					    	case Black:
					    		if(target.col<='B') { blob.hasLeftEdge++; hasEdge = true;}
					    		if((target.col-'A'+2)>=ncols) { blob.hasRightEdge++; hasEdge = true;}
					    		break;
						 }
						 if(hasEdge && toEdge && target.isEmpty())
						 {	 // the toEdge option is so the final connection to the edge will
							 // remain in consideration
							 TwixtChip peg = color.getPeg();
						 	 addChip(target,peg);
							 target.sweep_counter = 0;
							 added.push(target);
							 count++; 
							 blob.expandedToEdge++;
							 addPossibleBridges(target,peg,null,replayMode.Replay);
						 }
					 }
				 }
			 }
		 }
	 }
 	if(count>0)
 	{
 		int lim = added.size()-1;
 		while(count-- > 0)
 		{
 			TwixtCell c = added.elementAt(lim--);
 			TwixtBlob target = c.blob;
 			blob.addCell(c);
 			if(target!=blob) { blob.absorb(target); }
 		}
 		return(true);
 	}
 	return(false);
 }
 // assists for the robot
 //
 // flood fill to make this blob as large as possible.  This is more elaborate
 // than it needs to be for scoring purposes, but it is also used by the robot
 // we know that all the links are UP on the grid, so by starting on the bottom,
 // we usualy only need to investigate the obvious links.
 private void expandBlob(TwixtBlob blob,TwixtCell cell)
 {	
	TwixtChip top = cell.topChip();
	G.Assert((top!=null)&&(top.color()==blob.color()),"should be a same color peg"); 
	
	if((cell.sweep_counter!=sweep_counter))
 	{
 	cell.sweep_counter = sweep_counter;
 	cell.blob = blob;
 	
  	blob.addCell(cell);
  	for(int lim = cell.height()-1; lim>=1; lim--)
  		{	// follow links
  			TwixtChip link = cell.chipAtIndex(lim);
 	   		if(!link.isGhost()) { expandBlob(blob,cell.bridgeTo(link)); }
 	   	}
 	}
	else if(cell.blob==blob) {}
	else { blob.absorb(cell.blob); }	// unusual case, ran into another blob
 }

 private void resetDistances(int dis)
 {	for(TwixtCell c = allCells; c!=null; c=c.next) { c.resetDistance(dis); } 
 }
 int maxdistance;
 int findDistanceSteps = 0;
 boolean findDistanceExit = false;
 private int findDistanceToEdge(PieceColor color)
 {	maxdistance = ncols*3;
 	//resetDistances(99);
 	findDistanceSteps = 0;
 	findDistanceExit = false;
	sweep_counter++;
	int sweepStart = sweep_counter;
	int targetRow = -1;
	char targetCol = '@';
    int direction = 0;
    TwixtCell seed = null;
    switch(color)
    	{
    	default: throw G.Error("Not expected");
    	case Red:
    		direction = CELL_RIGHT;
    		first_direction = CELL_UP;
    		seed = getCell('B',1);
    		targetRow = ncols-1;
    		targetCol = (char)('A'+nrows+1);	// unreachable
    		break;
    	case Black:
    		direction = CELL_UP;
    		first_direction = CELL_RIGHT;
    		seed = getCell('A',2);
    		targetCol = (char)('A'+ncols-2);
    		targetRow = nrows+1;	// unreachable
    		break;
    	}
    
	{
	 	while(seed!=null)
	 	{	
	 	    findDistanceToEdge(0,seed,first_direction,color,targetCol,targetRow);
	   		seed = seed.exitTo(direction);
	 	}
	}
 	int dist = maxdistance;
	{	
 	int dir2 = direction+CELL_HALF_TURN;
 	TwixtCell seed2 = getCell((char)('A'+ncols-1),nrows-1);
 	TwixtCell seed3= seed2.exitTo(direction);
 	while(seed2!=null)
 		{	
 		if(seed2.sweep_counter==sweep_counter) 
 			{int newDis = seed2.distanceToEdge();
 			 if((newDis>=0) && (newDis<dist)) { dist = newDis; }
 			}
 		seed2 = seed2.exitTo(dir2);
 		}
 	while(seed3!=null)
 		{	
 		if(seed3.sweep_counter==sweep_counter) 
 			{int newDis = seed3.distanceToEdge();
 			 if((newDis>=0) && (newDis<dist)) { dist = newDis; }
 			}
 		seed3 = seed3.exitTo(dir2);
 		}  	
	}
 	G.Assert(sweep_counter==sweepStart,"sweep counter changed");
 	//G.print("Find Distance ",color," ",dist," Steps ",findDistanceSteps);
 	return(dist);

 }
 // mark the expansion spots for this blob. That is, the 
 // points where would could expand by 1 bridge.
 // also, note if none of the expansion points actually expand
 // in the desired directions
 private int findDistanceToEdge(int distance,TwixtCell from,int firstDirection,PieceColor color,char targetCol,int targetRow)
 {	if(from==null) { return(distance); }
 	findDistanceSteps++;
 	if(findDistanceExit) { return(maxdistance); }
	if(!from.isElgible(color)) { return(maxdistance); }
 	boolean empty = from.isEmpty();
 	if(!empty && (from.chipAtIndex(0).color()!=color)) { return(maxdistance); }
 	int dis = from.distanceToEdge();
 	if((from.sweep_counter<sweep_counter) || (dis>distance))
 	{	
 		if(from.sweep_counter<sweep_counter) { from.samples = 0; }
 		from.sweep_counter = sweep_counter;
 		from.setDistanceToEdge(distance);
 		if(from.col>=targetCol || from.row>=targetRow) 
 			{ maxdistance = Math.min(maxdistance,distance); 
 			  return(distance); 
 			} 
 		//int step = Math.min(targetCol-from.col,targetRow-from.row)/2;
 		//if(distance+step<maxdistance)
 		{
 		for(int direction = firstDirection,lastDir=firstDirection+CELL_FULL_TURN;
 				direction<lastDir;
 				direction+=CELL_QUARTER_TURN)
 		{	TwixtCell next0 = from.exitTo(direction);
 			if(next0!=null)
 			{for(int dif = -1;dif<=1;dif+=2)
 			{
 			TwixtCell next = next0.exitTo(direction+dif);
 			if(next!=null && !crossingBridge(from,next))
 			{
 			int newdis = findDistanceToEdge(distance+1,next,firstDirection,color,targetCol,targetRow);
 			if(newdis<distance) 
 			{ distance = newdis; 
 			  from.setDistanceToEdge(newdis); 
 			  lastDir = direction + CELL_FULL_TURN;  
			  }
 			}}}}}
 		if(!empty)
 		{	
 			TwixtBlob b = from.blob;
 			CellStack stack = b.cells;
 			for(int lim=stack.size()-1; lim>=0; lim--) 
 			{	findDistanceToEdge(distance,stack.elementAt(lim),firstDirection,color,targetCol,targetRow);
 			}
 		}
 	}
 	return(distance);
 }
 
 // find blobs consisting of real (non-ghost) pegs for a player
 BlobStack findRealBlobs(PieceColor pch)
 {
	 sweep_counter++;
	 int elgiblesize = 0;
	 BlobStack all = new BlobStack();
	 CellStack pegs = getPlayerPegs(pch);
	 for(int lim=pegs.size()-1; lim>=0; lim--)
	 {	TwixtCell c = pegs.elementAt(lim);
	 	TwixtChip peg = c.chipAtIndex(0);
	 	if(!peg.isGhost() && (peg.color()==pch))
		  	{ elgiblesize++;
		  	  if(c.sweep_counter!=sweep_counter)
		  	  {
			  TwixtBlob b = new TwixtBlob(pch,ncols);
			  all.push(b);
			  expandBlob(b,c);
		  	  }
		  	}
		  }
	 	
	 	// some blobs may have been absorbed
	 	int totalsize = 0;
	 	for(int lim = all.size()-1; lim>=0; lim--)
	 		{
	 			TwixtBlob b = all.elementAt(lim);
	 			int sz = b.size();
	 			if(sz==0) { all.remove(lim, false); }
	 			else { totalsize += sz; }
	 		}
	 	G.Assert(elgiblesize==totalsize,"miscounted");
	 	return(all);
 }
 //
 // flood fill to make this blob as large as possible.  This is more elaborate
 // than it needs to be for scoring purposes, but it is also used by the robot
 //  OStack<hexblob> findBlobs(int forplayer,OStack<hexblob> all)
 CombinedBlobStack findBlobs(int forPlayer)
 {	BlobStack all = findRealBlobs(getPlayerColor(forPlayer));
 	CombinedBlobStack combined = new CombinedBlobStack();
 	
 	// connect blobs if possible
 	for(int lim = all.size()-1; lim>=0; lim--)
 	{	TwixtBlob thisBlob = all.elementAt(lim);
 		if(!thisBlob.combined)
 		{
 		CombinedTwixtBlob combo = new CombinedTwixtBlob(thisBlob.color,thisBlob.ncols);
 		sweep_counter++;
 		markExpansion(thisBlob,false);	// mark first, it records the expansion potential
 		combo.addBlob(thisBlob);
		combined.push(combo);
 		boolean some = true;
 		while(some)
 		{
 		some=false;
 		for(int next = lim-1; next>=0; next--)
 		{
 			TwixtBlob nextBlob = all.elementAt(next);
 			if(!nextBlob.combined)
 			{
 			int n = meetExpansion(combo,thisBlob,nextBlob);
 			if(n>=2) 
 				{ //G.print("Combine "+thisBlob+nextBlob);
 				  markExpansion(nextBlob,false);	// get the expandable sides
 				  combo.addBlob(nextBlob);
 				  nextBlob.combined=true;
 				  some = true;
 				}}
 		}}
 	}}
 	return(combined);
 }

}
