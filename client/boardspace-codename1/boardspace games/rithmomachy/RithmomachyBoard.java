package rithmomachy;

import online.game.*;

import java.util.Hashtable;

import lib.*;

import static rithmomachy.RithmomachyMovespec.*;
/**
 * Rules open questions
 * 
 * Starting position: back row or +2 rows
 * General moves: Queen Style vs Irregular Knight Style
 * Capturing: As you move vs Instead of a move
 * Glorious victories: Instant vs delayed (like captures)
 * Glorious victories: sequences starting with 1
 * Glorious victories: conditioned on position (which line) or capture of pyramid
 * Pyramid: value used for glorious victories
 * Capture by deceit: Sum, Product, Difference of values
 * 
 * @author ddyer
 *
 */
class RithmomachyBoard extends rectBoard<RithmomachyCell> implements BoardProtocol,RithmomachyConstants
{	// rules options

	// starting position is described as starting in the back row, or
	// starting 2 rows forward in various texts.  
	public int startingColumnOffset[] = {-2,2};
	enum StartingPosition { zero, two };
	public StartingPosition startingPosition = StartingPosition.two;
	public void setStartingPosition(StartingPosition pos)
	{
		startingPosition = pos;
		switch(startingPosition)
		{
		case zero: 
			startingColumnOffset[0] = startingColumnOffset[1] = 0;
			break;
		case two:
			startingColumnOffset[0] = -2;
			startingColumnOffset[1] = 2;
			break;
		default:
			break;
		}
		setForwardColumn(forwardColumn);
	}
	
	// the "enemy camp" clause for glorious victories, sometimes
	// seems to be the most forward enemy starting position, sometimes
	// the center of the board, sometimes the most forward friendy position.
	// the last is untenable.
	public char forwardColumnOffset[] = {'I','H'};
	enum ForwardColumn { center, opponent };
	public ForwardColumn forwardColumn = ForwardColumn.center;
	public void setForwardColumn(ForwardColumn r)
	{
		forwardColumn = r;
		switch(forwardColumn)
		{
		case center:
			forwardColumnOffset[0] = 'I';
			forwardColumnOffset[1] = 'H';
			break;
		case opponent:
			forwardColumnOffset[0] = (char)('A'+4+startingColumnOffset[1]);
			forwardColumnOffset[1] = (char)('P'-4+startingColumnOffset[0]);
			break;
		default:
			break;
		}
	}
	
	//
	// movement style is either linear in any direction, 
	// or diagonal for rounds, and either orthoginal or knight style for triangles and squares
	//
	enum MovementStyle { linear, flying};
	MovementStyle movementStyle = MovementStyle.flying;
	public void setMovementStyle(MovementStyle m)
	{
		movementStyle = m;
	}
	
	enum DeceitStyle { custodial, replacement }
	DeceitStyle deceitStyle = DeceitStyle.custodial;
	public void setDeceitStyle(DeceitStyle st)
	{
		deceitStyle = st;
	}
	public double CapturedPart = 0.5;
	public double CapturedValuePart = 0.75;
	public WinDescription winDescription = null;
    public int boardColumns = DEFAULT_COLUMNS;	// size of the board
    public int boardRows = DEFAULT_ROWS;
    public void SetDrawState() { setState(RithmomachyState.Draw); }
    public RithmomachyCell captured[] = null;		// captured per player
    public RithmomachyChip playerChip[] = { RithmomachyChip.getChip(0,0),RithmomachyChip.getChip(1,0)};
    public int capturedValue[] = {0,0};
    public int startingValue[] = {0,0};
    public CellStack animationStack = new CellStack();
    public CellStack captureStack = new CellStack(); 		// captured during the game
    public CaptureStack availableCaptures = new CaptureStack();
    public boolean mixedPyramidCaptures = false;
    
    //
    // private variables
    //
    private RithmomachyState board_state = RithmomachyState.Play;	// the current board state
    private RithmomachyState unresign = null;					// remembers the previous state when "resign"
    public RithmomachyState getState() { return(board_state); } 
	public void setState(RithmomachyState st) 
	{ 	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

   public int chips_on_board[] = new int[2];			// number of chips currently on the board
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public RithmomachyCell pickedStack = null;
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    
	// factory method
	public RithmomachyCell newcell(char c,int r)
	{	return(new RithmomachyCell(c,r));
	}
    public RithmomachyBoard(String init,long rv,int np,int map[]) // default constructor
    {   
        setColorMap(map);
        doInit(init,rv,np); // do the initialization 
     }


	public void sameboard(BoardProtocol f) { sameboard((RithmomachyBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(RithmomachyBoard from_b)
    {
    	super.sameboard(from_b);	// calls sameCell for each cell, also for inherited class variables.
    	G.Assert(unresign==from_b.unresign,"unresign mismatch");
       	G.Assert(AR.sameArrayContents(win,from_b.win),"win array contents match");
       	G.Assert(AR.sameArrayContents(chips_on_board,from_b.chips_on_board),"chips_on_board contents match");
       	G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(sameCells(pickedStack,from_b.pickedStack),"pickedStack doesn't match");
        G.Assert(AR.sameArrayContents(capturedValue,from_b.capturedValue),"capturedValue doesn't match");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Digest matches");

    }
    
    public long DigestCap(Random r,CaptureStack caps)
    {	long v=0;
    	for(int i=caps.size()-1; i>=0; i--)
    	{
    		v ^= caps.elementAt(i).Digest(r);
    	}
    	return(v);
    }
    public void copyCap(CaptureStack caps,CaptureStack fromcaps)
    {	caps.clear();
    	for(int i=0;i<fromcaps.size();i++)
    	{	caps.addElement(fromcaps.elementAt(i).copy());
    	}
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

		v ^= Digest(r,pickedStack);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,captureStack);
		v ^= DigestCap(r,availableCaptures);
		v ^= Digest(r,capturedValue);
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
        return (v);
    }
   public RithmomachyBoard cloneBoard() 
	{ RithmomachyBoard copy = new RithmomachyBoard(gametype,randomKey,players_in_game,getColorMap());
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((RithmomachyBoard)b); }


    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(RithmomachyBoard from_b)
    {	
        super.copyFrom(from_b);			// copies the standard game cells in allCells list
        pickedStack.copyFrom(from_b.pickedStack);	
        captureStack.copyFrom(from_b.captureStack);
        copyFrom(captured,from_b.captured);
        copyCap(availableCaptures,from_b.availableCaptures);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        AR.copy(capturedValue,from_b.capturedValue);
        AR.copy(chips_on_board,from_b.chips_on_board);
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        sameboard(from_b);
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
    	captured = new RithmomachyCell[2];
    	captureStack.clear();
    	availableCaptures.clear();
    	setStartingPosition(startingPosition);
    	setForwardColumn(forwardColumn);
       	Random r = new Random(67246765);

     	{
     	if(Rithmomachy_INIT.equalsIgnoreCase(gtype)) 
     		{ 
     		boardColumns=DEFAULT_COLUMNS; 
     		boardRows = DEFAULT_ROWS;
     		initBoard(boardColumns,boardRows); //this sets up the board and cross links
     		}
     	else { throw G.Error(WrongInitError,gtype); }
     	gametype = gtype;
     	}
        allCells.setDigestChain(r);
	    setState(RithmomachyState.Puzzle);
	    
	    capturedValue[0] = capturedValue[1] = startingValue[0] = startingValue[1] = 0;
	    int map[]=getColorMap();
    	for(int pl = FIRST_PLAYER_INDEX; pl<=SECOND_PLAYER_INDEX; pl++)
    	{	RithmomachyCell cell = captured[map[pl]] = new RithmomachyCell(r);
    		cell.rackLocation = RackLocation[pl];
    		playerChip[map[pl]]=RithmomachyChip.getChip(pl,0);
    		for(int i=0,lim=RithmomachyChip.nChipsOfColor(pl); i<lim; i++)
    		{	
    	       	RithmomachyChip ch = RithmomachyChip.getChip(pl,i);
    	       	startingValue[pl] += ch.value;
     	       	RithmomachyCell c = getCell((char)(ch.home_col+startingColumnOffset[pl]), ch.home_row);
    	       	c.addChip(ch);
    	       	c.sort();
    	       	
 	       	}
    	}
	    
	    whoseTurn = FIRST_PLAYER_INDEX;
		pickedSourceStack.clear();
		droppedDestStack.clear();
		pickedStack = new RithmomachyCell(r);
		pickedStack.rackLocation = RithId.PickedStack;
        AR.setValue(win,false);
        AR.setValue(chips_on_board,0);
        unresign = null;
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
        case Puzzle:
            break;
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
    {	
        switch (board_state)
        {case Resign:
         case Confirm:
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
    	setState(RithmomachyState.Gameover);
    }
    
   public  boolean isArithmetic(int v1,int v2,int v3)
    {	return((v2-v1) == (v3-v2));
    }
   public boolean isGeometric(int v1,int v2,int v3)
    {	return( v2*v2 == v1*v3);
    }
   public boolean isHarmonic(int v1,int v2,int v3)
    {	// check that v1*v3*2/(v1+v3)=v2;
    	return( (v1*v3*2)==v2*(v1+v3));
    }
    
    // this is the final stage, winDescription has been incrementally filled in as we descend the stack
    // so only the actual values remain to be filled in.
    void recordWin(int a,int p,int e,Wintype t)
    {  	winDescription.anchor = a;
    	winDescription.pivot = p;
    	winDescription.end = e;
    	winDescription.type = t;
    }
    
    // check for a victory with 3 chips which have the appropriate geometry. 
    private boolean gloriousEnd(WinStack collection,int anchorValue, int pivotValue,int endValue)
    {	int v1 = anchorValue;
    	int v2 = pivotValue;
    	int v3 = endValue;
    	if(isArithmetic(v1,v2,v3)) 
    		{ recordWin(anchorValue,pivotValue,endValue,Wintype.arithmetic);
    		  if(collection!=null) { collection.push(winDescription.copy()); }
    		  return(true); 
    		}
    	if(isGeometric(v1,v2,v3)) 
    		{ recordWin(anchorValue,pivotValue,endValue,Wintype.geometric);
  		  	  if(collection!=null) { collection.push(winDescription.copy()); }
  		      return(true); 
    		}
    	if(isHarmonic(v1,v2,v3))
    		{ recordWin(anchorValue,pivotValue,endValue,Wintype.harmonic);
    		  if(collection!=null) { collection.push(winDescription.copy()); }
		      return(true); 
    		}
    	return(false);
    }
    private boolean isForwardColumn(RithmomachyCell end,int pl)
    {
    	char col = forwardColumnOffset[pl];
    	return((pl==0) ? (end.col<col) : (end.col>col));
    }
    

    private boolean gloriousEnd(int player,WinStack collection,
    					int anchorValue,RithmomachyCell pivot,int pivotValue,
    					int direction,int distance)
    {	boolean some = false;
    	RithmomachyCell end = pivot;
    	int dis = 0;
    	// continue along the same line, horizontal, verrical or diagonal
    	while(end!=null && dis<distance)
    		{ end = end.exitTo(direction);
  		  	  dis++; 
    		  if((end!=null) && (dis<distance) && (end.height()>0)) { end = null; }
    		}
    	if((end!=null) && (isForwardColumn(end,player)))
    	{	// took "distance" steps and we're still on the board.
    		int eheight = end.height();
    		winDescription.endCell = end;
    		if(eheight>1)
    		{	// end is a pyramid
    			if(gloriousEnd(collection,anchorValue,pivotValue,end.stackValue()))
    			{
    				if(collection==null) { return(true); }
    				some = true;
    			}
    		}
    		for(int h=eheight-1; h>=0; h--)	// usually only one, but might be a pyramid
    		{
    		RithmomachyChip echip = end.chipAtIndex(h);
    		int evalue = echip.value;
    		if(gloriousEnd(collection,anchorValue,pivotValue,evalue)) 
    			{ if(collection==null)  return(true); 
    			  some = true;
    			}
    		}
    	}
    	return(some);
    }
    
    // check for correct continuations from the pivot cell in the middle
    private boolean gloriousContinuation(int player,WinStack collection,int anchor,RithmomachyCell pivot,int pivotValue,int direction,int distance)
    {	boolean some = false;
    	// first check geometry, either 3-in-a-row 3-in-a-square or 3-in-a-diagonal.
    	winDescription.pivotCell = pivot;
   		if(gloriousEnd(player,collection,anchor,pivot,pivotValue,direction,distance))
   			{ if(collection==null) {  return(true); } 
   			  some = true;
   			}
   		// if not a diagonal direction, also check left and right turns
    	if((direction & 1) != 0)
    	{
    		if(gloriousEnd(player,collection,anchor,pivot,pivotValue,direction+2,distance)) 
    			{ 
    			  if(collection==null) { return(true); } 
    			  some = true;
    			}
    		if(gloriousEnd(player,collection,anchor,pivot,pivotValue,direction-2,distance)) 
    			{ 
    			  if(collection==null) { return(true); } 
    			  some = true;
    			}
    	}
    	return(some);
    }

	private boolean winByGloriousVictory(int pl,WinStack collection,RithmomachyCell c,int anchorValue)
	{	boolean some = false;
		// c is an anchor, look for a second piece in
		// horizontal or vertical lines

		for (int direction = 0; direction < 8; direction++) {
			int distance = 0;
			RithmomachyCell middle = c.exitTo(direction);
			while (middle != null) {
				distance++;
				if (!isForwardColumn(middle, pl)) {
					middle = null;
				} // likewise has to be forward
				else 
					{
					int midheight = middle.height();
					if (midheight > 0) { // found a
												// pivot
												// cell
					if(midheight>1)
					{	// middle is a pyramid, try it's whole value
						some |= gloriousContinuation(pl,collection,anchorValue,middle,middle.stackValue(),direction,distance);
						if(some&&(collection==null)) { return(true); }
					}
					for (int h = midheight - 1; h >= 0; h--) {
						RithmomachyChip mchip = middle.chipAtIndex(h);
						int mvalue = mchip.value;
						if (gloriousContinuation(pl, collection, anchorValue,
								middle, mvalue, direction, distance)) 
							{
							some = true;
							if (collection == null) { return (true); }
							}
					}
					middle = null;
					}
				}
				if (middle != null) {
					middle = middle.exitTo(direction);
				}
			}
		}
		return(some);
	}
    /* it's a win if this is a win by arithmetic, geometric, or harmonic progresion.
     * if collection is supplied, collect all of them, otherwise just find the first 
     */
	public boolean winByGloriousVictory(int playerIndex,	WinStack collection)
	{
		boolean some = false;
		if (winDescription == null) {
			winDescription = new WinDescription();
		}
		int playerColor = getColorMap()[playerIndex];
		for (RithmomachyCell c = allCells; c != null; c = c.next) {
			if (isForwardColumn(c, playerColor)) // on columns on the opponent's side of
										// the board
			{
				RithmomachyChip ctop = c.topChip();
				if (ctop != null) {
					int ctopPlayer = playerIndex(ctop);
					if (ctopPlayer == playerIndex) {
						int ctopHeight = c.height();
						winDescription.anchorCell = c;
						if(ctopHeight>1)
						{
						// if a pyramid, try the full pyramid
						some |= winByGloriousVictory(playerColor,collection,c,c.stackValue());
						if(some && (collection==null)) { return(true); }
						}
						for (int height = ctopHeight - 1; height >= 0; height--)
						{
							RithmomachyChip topchip = c.chipAtIndex(height);
							int anchor = topchip.value;
							some |= winByGloriousVictory(playerColor,collection,c,anchor);
							if(some && (collection==null)) { return(true); }
						}

					}
				}
			}
		}
		return (some);
	}
	
    // entry point for the viewer
    public WinStack collectWinsByGloriousVictory(int pl,WinStack collection)
    {
    	if(collection==null) { collection = new WinStack(); }
    	winByGloriousVictory(pl,collection);
    	return(collection);
    }
    public int captureTarget(int pl)
    {
    	return((int)(CapturedPart*RithmomachyChip.nChipsOfColor(pl)));
    }
    public int valueTarget(int next)
    {
    	return((int)(CapturedValuePart*startingValue[next]));
    }
    private void addNumber(IStack numbers,RithmomachyCell c,boolean include_stack)
    {
    	int h = c.height();
		if(include_stack && (h>1)) { numbers.pushNew(c.stackValue()); }
		while(h-- > 0) { numbers.pushNew(c.chipAtIndex(h).value); }
    }
    public IStack availableNumbers(int pl,boolean include_stack)
    {
    	IStack numbers = new IStack();
    	for(RithmomachyCell c = allCells; c!=null; c=c.next)
    	{	if((pl==-1)||playerIndex(c)==pl) { addNumber(numbers,c,include_stack); }
    	}
    	if((pickedStack.height()>0)&&((playerIndex(pickedStack)==pl)||(pl==-1))) { addNumber(numbers,pickedStack,include_stack); }
    	return(numbers);
    }
    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	if(board_state==RithmomachyState.Gameover) { return(win[player]); }
    	if(winByGloriousVictory(player,null)) { return(true); }
    	int next = nextPlayer[player];
    	boolean winCount = (captured[next].height() > captureTarget(next));
    	boolean winValue =  (capturedValue[next] > valueTarget(next));
    	return( winCount || winValue );
    	
    }
    public RithmomachyCell getPyramid(int pl)
    {
    	for(RithmomachyCell c = allCells; c!=null; c=c.next) { if((c.height()>1)&&(playerIndex(c)==pl)) { return(c); }}
    	return(null);
    }
    // estimate the value of the board position.
    public double ScoreForPlayer(int player,boolean print)
    {  	int next = nextPlayer[player];
    	double finalv=(double)captured[next].height()/RithmomachyChip.nChipsOfColor(next) + 0.5*capturedValue[next]/startingValue[next];
    	return(finalv);
    }


    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement()
    {	
        pickedStack.reInit();
        droppedDestStack.clear();
        pickedSourceStack.clear();
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    if(droppedDestStack.size()>0)
    	{
    	RithmomachyCell dr = droppedDestStack.pop();
    	switch(dr.rackLocation())
	    	{
	   		default: throw G.Error("Not expecting rackLocation %s",dr.rackLocation);
			case BoardLocation: 
				pickedStack.moveStack(dr); 
				break;
			case White_Chip_Pool:	// treat the pools as infinite sources and sinks
			case Black_Chip_Pool:	
				pickedStack.moveStack(dr);
				break;	// don't add back to the pool
	    	
	    	}
	    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	
    	if(pickedStack.height()>0)
    	{
    		RithmomachyCell ps = pickedSourceStack.pop();
    		switch(ps.rackLocation())
    		{
    		default: throw G.Error("Not expecting rackLocation %s",ps.rackLocation);
    		case White_Chip_Pool:
    		case Black_Chip_Pool:	
    		case BoardLocation: ps.moveStack(pickedStack); 
    			break;	// don't add back to the pool
    		}
     	}
     }

    // 
    // drop the floating object.
    //
    private void dropObject(RithmomachyCell c)
    {   	    		
    	switch(c.rackLocation())
		{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case White_Chip_Pool:
		case Black_Chip_Pool:	
		case BoardLocation: 
			c.moveStack(pickedStack); 
			break;
		}
       	droppedDestStack.push(c);
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(RithmomachyCell cell)
    {	return((droppedDestStack.size()>0) && (droppedDestStack.top()==cell));
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	
    	if(pickedStack.height()>0)
    		{ return(pickedStack.rackLocation().ordinal());
    		}
        return (NothingMoving);
    }
    
    public RithmomachyCell getCell(RithId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	return(getCell(col,row));
        case White_Chip_Pool:
       		return(captured[White_Chip_Index]);
        case Black_Chip_Pool:
       		return(captured[Black_Chip_Index]);
        }
    }
    public RithmomachyCell getCell(RithmomachyCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(RithmomachyCell c,int row)
    {	
    	switch(c.rackLocation())
    	{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case BoardLocation: 
			pickedStack.moveStack(c); 
			break;
		case White_Chip_Pool:
		case Black_Chip_Pool:	
			pickedStack.addChip(c.removeChipNumber(row));
			break;	// don't add back to the pool
    	
    	}
    	pickedSourceStack.push(c);
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
        case Confirm:
        case Draw:
        	setNextStateAfterDone(); 
        	break;
        case Play:
        	availableCaptures.clear();
        	getCaptures(availableCaptures,whoseTurn);
			setState(RithmomachyState.Confirm);
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
    public boolean isSource(RithmomachyCell c)
    {	return(getSource()==c);
    }
    public RithmomachyCell getSource()
    {
    	return((pickedSourceStack.size()>0) ?pickedSourceStack.top() : null);
    }
    public RithmomachyCell getDest()
    {	return((droppedDestStack.size()>0) ? droppedDestStack.top() : null);
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
        case Confirm:
        case Draw:
        	availableCaptures.clear();
        	setState(RithmomachyState.Play);
        	break;
        case Play:
        	availableCaptures.clear();
			break;
        case Puzzle:
        	availableCaptures.clear();
            break;
        }
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
     	case Gameover: 
    		break;

        case Draw:
        	setGameOver(false,false);
        	break;
    	case Confirm:
    	case Puzzle:
    	case Play:
    		setState(RithmomachyState.Play);
    		break;
    	}

    }
    private void undoCaptures(int height)
    {	while(captureStack.size()>height)
    	{	RithmomachyCell dest = captureStack.pop();
    		int next = nextPlayer[whoseTurn];
    		RithmomachyChip ch = captured[next].removeTop();
     		capturedValue[next]-= ch.value;
    		dest.insertInOrder(ch);
    		// note, when capturing from inside a stack, will need additional info
    	}
    }
    private void doCaptures(RithmomachyCell dest,replayMode replay)
    {	for(int lim = availableCaptures.size()-1; lim>=0; lim--)
    	{
    		CaptureDescription cap = availableCaptures.elementAt(lim);
    		RithmomachyCell c = cap.victim;
    		RithmomachyChip ch = cap.victimChip;
    		if(ch==null)
    		{	RithmomachyChip top =  c.topChip();
    			if(top!=null)
    				{int next =playerIndex(top);
    			// capture everything
    			if(replay!=replayMode.Replay)
    			{
    				animationStack.push(c);
    				animationStack.push(captured[next]);
    			}
    			while( (ch = c.topChip())!=null) 
    			{
    			 captured[next].addChip(ch);
    			 c.removeTop();
    			 capturedValue[next] += ch.value;
   			     captureStack.push(c);
    			}}
    		}
    		else
    		{
    		int ind = c.findChip(ch);
    		// can be legitimately empty if the same chip could be captured two ways.
			if(ind>=0)
				{int chColor = playerIndex(ch);
				 c.removeChipAtIndex(ind);
				 capturedValue[chColor]+=ch.value;
    		     captured[chColor].addChip(ch);
			     captureStack.push(c);
			     if(replay!=replayMode.Replay)
			     {
			    	 animationStack.push(c);
			    	 animationStack.push(captured[chColor]);
			     }
				}}
    	}
    	availableCaptures.clear();
    }

    private void doDone(replayMode replay)
    {	RithmomachyCell dest = getDest();
    	doCaptures(dest,replay);
    	//doCapturesByEruption(dest);
    	//doCapturesByDeceit(dest);
        acceptPlacement();

        if (board_state==RithmomachyState.Resign)
        {	setGameOver(false,true);
        }
        else
        {	boolean win1 = WinForPlayerNow(whoseTurn);
        	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
        	if(win1 || win2)  { setGameOver(win1,win1?false:win2); }
        	else {setNextPlayer(); setNextStateAfterDone(); }
        }
    }

    public boolean Execute(commonMove mm,replayMode replay)
    {	RithmomachyMovespec m = (RithmomachyMovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }
        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone(replay);

            break;

        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case Play:
        			RithmomachyCell src = getCell(RithId.BoardLocation, m.from_col, m.from_row);
        			RithmomachyCell dest = getCell(RithId.BoardLocation,m.to_col,m.to_row);
        			pickObject(src,m.from_row);
        			m.chip = pickedStack.topChip();
        			dropObject(dest); 
        			if(replay!=replayMode.Replay)
        			{
        				animationStack.push(src);
        				animationStack.push(dest);
        			}
 				    setNextStateAfterDrop();
        			break;
        	}
        	break;
        case MOVE_DROPB:
			{
			RithmomachyCell c = getCell(RithId.BoardLocation, m.to_col, m.to_row);
			if(replay==replayMode.Single)
			{
				animationStack.push(getSource());
				animationStack.push(c);
			}
			
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
        		  setState(RithmomachyState.Play);
        		}
        	else 
        		{ pickObject(getCell(RithId.BoardLocation, m.from_col, m.from_row),m.from_row);
        		  m.chip = pickedStack.topChip();
         		}
 
            break;
        case MOVE_DROP: // drop on chip pool;
            dropObject(getCell(m.source, m.to_col, m.to_row));
            setNextStateAfterDrop();

            break;

        case MOVE_PICK:
        	{
        	RithmomachyCell c = getCell(m.source, m.from_col, m.from_row);
            pickObject(c,m.from_row);
            setNextStateAfterPick();
        	}
            break;


         case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(RithmomachyState.Puzzle);
            {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;

        case MOVE_RESIGN:
        	if(unresign!=null) { setState(unresign); unresign = null; }
        	else{ unresign = board_state; setState(RithmomachyState.Resign); }
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            // standardize "gameover" is not true
            setState(RithmomachyState.Puzzle);
 
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
         case Confirm:
         case Draw:
         case Play: 
         case Resign:
         case Gameover:
			return(false);
        case Puzzle:
        	return(true);
        }
    }
    private int playerIndex(RithmomachyChip chip) { return(getColorMap()[chip.colorIndex()]); }
    int playerIndex(RithmomachyCell c)
    {
    	RithmomachyChip ch = c.topChip();
    	return( (ch==null) ? -1 : playerIndex(ch));
    }
    public boolean LegalToHitBoard(RithmomachyCell cell)
    {	
        switch (board_state)
        {

        	// otherwise fall through
 		case Play:
 			if(pickedStack.height()==0)
 			{
 				return((cell.height()>0) 
 						&& (playerIndex(cell.topChip())==whoseTurn));
 			}
 			else
 			{	Hashtable<RithmomachyCell,RithmomachyMovespec>dests = getDests();
 				return(dests.get(cell)!=null);
 			}
 			// can't get here.
		case Gameover:
		case Resign:
			return(false);
		case Confirm:
		case Draw:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case Puzzle:
        	return((pickedStack.height()>0) ? true : (cell.height()>0));
        }
    }

 StateStack robotState = new StateStack();
 IStack robotCaptures = new IStack();
 
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(RithmomachyMovespec m)
    {
    	robotState.push(board_state); //record the starting state. The most reliable
        robotCaptures.push(captureStack.size());
        
        // to undo state transistions is to simple put the original state back.
        
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        if (Execute(m,replayMode.Replay))
        {	switch(m.op)
        	{
            case MOVE_DONE:
            	break;
            default:
            	if (DoneState())
                {
                    doDone(replayMode.Replay);
                }
                else
                {
                	throw G.Error("Robot move should be in a done state");
                }
        	}
        }
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(RithmomachyMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
        undoCaptures(robotCaptures.pop());
        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;

        case MOVE_DONE:
            break;

        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case Gameover:
        		case Play:
        			pickObject(getCell(RithId.BoardLocation, m.to_col, m.to_row),m.to_row);
       			    dropObject(getCell(RithId.BoardLocation, m.from_col,m.from_row)); 
       			    acceptPlacement();
        			break;
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        unresign = null;
        setState(robotState.pop());

 }
 public Hashtable<RithmomachyCell,RithmomachyMovespec>getDests()
 {	Hashtable<RithmomachyCell,RithmomachyMovespec> h = new Hashtable<RithmomachyCell,RithmomachyMovespec>();
 	RithmomachyCell from = getSource();
 	if((from!=null) && from.onBoard && pickedStack.height()>0)
 	{	CommonMoveStack add = new CommonMoveStack();
 		getMovesFrom(add,from,pickedStack.getDistanceMask(),whoseTurn);
 		while(add.top()!=null)
 		{	RithmomachyMovespec m = (RithmomachyMovespec)add.pop();
 			RithmomachyCell c = getCell(m.to_col,m.to_row);
 			h.put(c, m);
 		}
 	}
 
 	return(h);
 }
 
 // return true if c can move to victim cell based on ctop's movement
 boolean canMoveLinearFromTo(RithmomachyCell c,RithmomachyChip ctop,RithmomachyCell victim)
 {	
 	int distance = ctop.moveDistance();
 	for(int dir = 0; dir<8;dir++)
 	{	RithmomachyCell d = c;
 		int dist = distance;
 		while((d!=null) && (dist-- > 0))
 			{ d = d.exitTo(dir); 
 			  if((dist>0) && (d!=null) && (d.topChip()!=null))
 			  	{ d=null; }		// obstruction on the way to the end
 			}
 		if(d==victim) { return(true); }
 	}
 	return(false);
 }
 
 
 boolean getMovesFromLinear(CommonMoveStack  all,RithmomachyCell c,int distances,int who)
 {	boolean some = false;
 	for(int direction = 0; direction<CELL_FULL_TURN; direction++)
 	{	int dist = distances;
 		RithmomachyCell d = c;
 		while(d!=null && dist!=0)
 			{ dist = dist>>1;
 	 		 d = d.exitTo(direction);
	 		if((d!=null) && (d.height()==0))
	 		{
 	 		 if( (dist & 1)!=0)	// dist is a mask of the distances the piece can move
 	 		 	{

 	 	 				 some = true;
 	 	 				 if(all!=null) { all.addElement(new RithmomachyMovespec(MOVE_BOARD_BOARD,c.col,c.row,d.col,d.row,who)); }
 		 	    }
	 		}
 	 		else { d  = null; }
 	 		
 			}
 	}
 	return(some);
 }
 
 public boolean getDiagonalMovesFrom(CommonMoveStack  all,RithmomachyCell src,RithmomachyCell c,int direction,int who,int stepby)
 {	boolean some = false;
	 for(int dir = direction-1; dir<=direction+1; dir+=stepby)
	 {
		 RithmomachyCell d = c.exitTo(dir);
		 if((d!=null) && d.topChip()==null)
		 {
			 some = true;
			 if(all!=null) { all.addElement(new RithmomachyMovespec(MOVE_BOARD_BOARD,src.col,src.row,d.col,d.row,who)); }
		 }
	 }
	 return(some);
 }
 
 //
 // return true if c can move to victim based on the movement of ctop
 //
 boolean canMoveFlyingFromTo(RithmomachyCell c,RithmomachyChip ctop,RithmomachyCell victim)
 {	int distance = ctop.moveDistance();
 	for(int step=0; step<8; step+=2)
 	{
 		RithmomachyCell d = c;
 		int dis = distance;
 		// if the original distance was more than 1, take n-1 steps in the direction
 		// and require that they be unobstructed
 		while((d!=null) && (dis-->1))
 		{	d = d.exitTo(CELL_LEFT+step);
 			if((d!=null) && (d.topChip()!=null)) { d = null; }	// blocked
 		}
 		if(d!=null)
 		{	if(d.exitTo(CELL_LEFT+step-1)==victim) { return(true); }
 			if(distance>1)
 			{
 				if(d.exitTo(CELL_LEFT+step)==victim) { return(true); }
 				if(d.exitTo(CELL_LEFT+step+1)==victim) { return(true); }
 			}
 		}
 	}
 	return(false);
 }
 
 boolean getMovesFromFlying(CommonMoveStack  all,RithmomachyCell c,int distances,int who)
 {	boolean some = false;
 	if((distances & 2)!=0)
 	{
 		some |= getDiagonalMovesFrom(all,c,c,CELL_LEFT,who,2);
 		some |= getDiagonalMovesFrom(all,c,c,CELL_RIGHT,who,2);
 	}
 	distances = distances>>1;
	if(distances>1)
	{
 	for(int dir = CELL_RIGHT,steps=0; steps<4; dir+=2,steps++)
 	{	RithmomachyCell d = c;
 		int dirsteps = distances;
 		boolean blocked = false;
 		while((d!=null) && (dirsteps!=0))
 		{	d = d.exitTo(dir);
 			dirsteps = dirsteps>>1;
 			if(d==null) {}
 			else 
 			{if(d.topChip()!=null) { blocked=true; }	// blocked;
 			 if((dirsteps&1)!=0)
 			 {	// vacant, step diagonally
	 			
 				some |= getDiagonalMovesFrom(all,c,d,dir,who,blocked ? 2 : 1);
  	 
 			}}
 		} 		
 	}}

 	return(some);
 }
 
 boolean getMovesFrom(CommonMoveStack  all,RithmomachyCell c,int distances,int who)
 {
	 switch(movementStyle)
	 {
	 default: throw G.Error("Not expecting style "+movementStyle);
	 case linear:	return(getMovesFromLinear(all,c,distances,who));
	 case flying:	return(getMovesFromFlying(all,c,distances,who));
	 }
 }
 
 // true if value1 * distance = value2
 boolean isCaptureByEruption(int srcval,int distance,int dstval)
 {
	 return((srcval*distance==dstval) || (dstval*distance==srcval));
 }
 
 private boolean getCapturesByEruption(CaptureStack all,int distance,
		 			RithmomachyCell src,RithmomachyChip srcChip,int srcVal,
		 			RithmomachyCell dst,RithmomachyChip dstChip, int dstVal)
 {	if( isCaptureByEruption(srcVal,distance,dstVal))
 	{	if(all==null) { return(true); }
 		all.addElement( new CaptureDescription(CaptureType.Eruption,dst,dstChip,src,srcChip,null,null));
 		return(true);
 	}
 	return(false);
 }
 
 private boolean getCapturesByEruption(CaptureStack all,int distance,RithmomachyCell src,RithmomachyChip srcChip,int srcVal,RithmomachyCell dst,int who)
 {	boolean some = false;
 	int dstH = dst.height();
 	if(dstH > 1)
 	{	// pyramid being captured as a whole
 		some = getCapturesByEruption(all,distance,src,srcChip,srcVal,dst,null,dst.stackValue());
 		if(some && (all==null)) { return(true); }
 	}
 	while(dstH-- > 0)
 	{	//individual chips being captured
 		RithmomachyChip dstChip = dst.chipAtIndex(dstH);
 		some |= getCapturesByEruption(all,distance,src,srcChip,srcVal,dst,dstChip,dstChip.value);
 		if(some && (all==null)) { return(true); }
 	}
 	return(some);
 }
 
 private boolean getCapturesByEruption(CaptureStack all,int distance,RithmomachyCell src,RithmomachyCell d,int who)
 {	boolean some = false;
 	int srch = src.height();
 	if(srch>1)
 		{ // pyramid capturing as a whole
 		  some = getCapturesByEruption(all,distance,src,null,src.stackValue(),d,who);
 		  if(some && (all==null)) { return(true); }
 		}
 	while(srch-- > 0)
 	{	// individual chips doing the capturing
 		RithmomachyChip srcChip = src.chipAtIndex(srch);
 		some |= getCapturesByEruption(all,distance,src,srcChip,srcChip.value,d,who);
 		if(some && (all==null)) { return(true); }
 	}
 	return(some);
 }
 


private boolean getCapturesByEquality(CaptureStack all,RithmomachyCell c,RithmomachyChip srcChip,int srcValue,int who)
{
	boolean some = false;
	int distances = c.getDistanceMask();
	boolean encounter = c.canCaptureByEncounter();
	for(int direction = 0; direction<CELL_FULL_TURN; direction++)
	{	int dist = distances;	// distances the center cell can move
		RithmomachyCell d = c;
		int distance = 1;
		while(d!=null)
			{ dist = dist>>1;
	 		 d = d.exitTo(direction);
	 		 distance++;
	 		if(d!=null)
	 		{
	 		int owner = playerIndex(d);
	 		if(owner==who) { d=null; }
	 		else
	 		{
	 		int dHeight = d.height();
	 		if(dHeight>0)
	 		{
	 		for(int srcH = c.height()-1; srcH>=0; srcH--)
	 		{	RithmomachyChip src = c.chipAtIndex(srcH);
	 			for(int destH = dHeight-1; destH>=0; destH--)
	 			{
	 			RithmomachyChip dst = d.chipAtIndex(destH);
	 			if(encounter
	 					&& ((dist&1)!=0)		// see if this stack can move this far
	 					&& dst.canCaptureByEncounter()
	 					&& (src.value==dst.value))
	 				{
	 				// capture a piece of equal value.  Possible ambiguity for the pyramid, if the whole
	 				// puramid can make this move, but the cell doing the capturing can't.
	 				if(all==null) { return(true); }
	 				all.addElement( new CaptureDescription(CaptureType.Equality,d,dst,c,src,null,null));
	 				some = true;
	 				}
	 			if(isCaptureByEruption(src.value,distance,dst.value))
	 			{	// capture by eruption 
	 				if(all==null) { return(true); }
	 				all.addElement( new CaptureDescription(CaptureType.Eruption,d,dst,c,src,null,null));
	 				some = true;
	 			}
	 			
	 			}
	 			
	 		}
	 		d = null;	// no capture, so this is an obstruction that stops the search
	 		}
	 		
	 		}}}

	}
	return(some);
}


//
// get captures by eruption starting from src by player who
//
private boolean getCapturesByEruption(CaptureStack all,RithmomachyCell src, int who) 
{
		boolean some = false;
		for (int direction = 0; direction < CELL_FULL_TURN; direction++) 
		{
			RithmomachyCell dst = src;
			int distance = 1;
			while (dst != null) 
			{
				dst = dst.exitTo(direction);
				distance++;
				if (dst != null) 
				{
					RithmomachyChip dtop = dst.topChip();
					if(dtop!=null) 
						{ if (playerIndex(dtop)==nextPlayer[who]) 
							{  some |= getCapturesByEruption(all, distance, src, dst, who); 
							   if(some && (all==null)) { return(true); }
							}
					     dst = null;
					   }
				}
			}
		}
		return (some);
	}

private boolean getCapturesByEquality(CaptureStack all,RithmomachyCell src,int who)
{	int srcH = src.height();
	boolean some = false;
	if(srcH>1) { some = getCapturesByEquality(all,src,null,src.stackValue(),who); 
		if(some && (all==null)) { return(true); }
	}
	while(srcH-- > 0)
	{	RithmomachyChip srcChip = src.chipAtIndex(srcH);
		some |= getCapturesByEquality(all,src,srcChip,srcChip.value,who);
		if(some && (all==null)) {return(true); }
	}
	return(some);
}

 //
 // get rithmomachy captures which originate at src
 //
 private boolean getCapturesByEncounterOrEquality(CaptureStack all,RithmomachyCell src,int who)

 {	boolean some = getCapturesByEruption(all,src,who);
 	if(some && (all==null)) { return(true); }
 	some |= getCapturesByEquality(all,src,who);
 	return(some);
 }
 
 private boolean isAttackByDeceit(int a1,int a2,int v)
 {	// sum difference, product, or quotient of two 
	 return( (a1+a2==v) || (a1-a2==v) || (a2-a1==v) || (a1*a2==v) || (a1 == a2*v) || (a2==a1*v));
 }
 
 private boolean getCapturesByCustodialDeceit(CaptureStack all,
		 RithmomachyCell a1,RithmomachyChip a1Chip,int a1Value,
		 RithmomachyCell a2,RithmomachyChip a2Chip,int a2Value,
		 RithmomachyCell victim,RithmomachyChip victimChip,int victimValue)
 {		
	 if(isAttackByDeceit(a1Value,a2Value,victimValue))
 		{	if(all!=null)
 			{
 			all.addElement(new CaptureDescription(CaptureType.Deceit,victim,victimChip,a1,a1Chip,a2,a2Chip));
 			}
 			return(true);
 		}
	 return(false);
 }
 
 private boolean getCapturesByCustodialDeceit(CaptureStack all,
		 RithmomachyCell a1,RithmomachyChip a1Chip,int a1Value,RithmomachyCell a2,RithmomachyCell victim,RithmomachyChip victimChip,int victimValue)
 {	boolean some = false;
 	int a2Height = a2.height();
 	if((a2Height>1) && a2.canAttackAsPyramid())
 	{
 		some = getCapturesByCustodialDeceit(all,a1,a1Chip,a1Value,a2,null,a2.stackValue(),victim,victimChip,victimValue);
 		if(some && (all==null)) { return(true); }
 	}
 	while(a2Height-- > 0)
 	{
 		RithmomachyChip a2Chip = a2.chipAtIndex(a2Height);
 		if(getCapturesByCustodialDeceit(all,a1,a1Chip,a1Value,a2,a2Chip,a2Chip.value,victim,victimChip,victimValue))
 		{	if(all==null) { return(true); }
 			some = true;
 		}
 	}
 	return(some);
 }
 

 // all components fixed
 private boolean getCapturesByCustodialDeceit(CaptureStack all,
		 RithmomachyCell a1,RithmomachyCell a2,RithmomachyCell victim,RithmomachyChip victimChip,int victimValue)
 {	boolean some = false;
 	int a1Height = a1.height();
 	if((a1Height>1) && a1.canAttackAsPyramid()) 
 		{ some |= getCapturesByCustodialDeceit(all,a1,null,a1.stackValue(),a2,victim,victimChip,victimValue);
 		  if(some && (all==null)) { return(true); }
 		}
 	while(a1Height-- > 0)
 	{	RithmomachyChip a1Chip = a1.chipAtIndex(a1Height);
 		some |= getCapturesByCustodialDeceit(all,a1,a1Chip,a1Chip.value,a2,victim,victimChip,victimValue);
 		if(some && (all==null)) { return(true); }
 	}
 	return(some);
 }
 
 //
 // a1 and a2 attack a victim, either as a whole or as any of its components.
 //
 private boolean getCapturesByCustodialDeceit(CaptureStack all,RithmomachyCell a1,RithmomachyCell a2,RithmomachyCell victim)
 {	boolean some = false;
 	
 	int vh = victim.height();
 	if(vh>1) 
 	{ some = getCapturesByCustodialDeceit(all,a1,a2,victim,null,victim.stackValue());
 	  if(some) { return(some); }	// attack as a whole, no need for component checks
 	}
	while(vh-- > 0)
		{	RithmomachyChip victimChip = victim.chipAtIndex(vh);
			some |= getCapturesByCustodialDeceit(all,a1,a2,victim,victimChip,victimChip.value);
			if(some && (all==null)) { return(some); }
		}
	return(some);
 }
 // 
 // capture by deceit if two attackers on opposite sides with values sum to the value of the victim
 // if a stack is supplied, collect all possible captures
 // otherwise just return true if an attack exists.
 //
 private boolean getCapturesByCustodialDeceit(CaptureStack all,RithmomachyCell victim,int who)
 {	
 	boolean some = false;
 	for(int direction = 0; direction<CELL_HALF_TURN; direction++)
 	{	RithmomachyCell a1 = victim.exitTo(direction);
 		if(a1!=null)	// potential attacker
 			{
 			if(playerIndex(a1)==who)
 			{
 				RithmomachyCell a2 = victim.exitTo(direction+CELL_HALF_TURN);
 				if(a2!=null)
 				{
 					if(playerIndex(a2)==who)
 					{	// a properly positioned pair of attackers
 						some |= getCapturesByCustodialDeceit(all,a1,a2,victim);
 						if(some && (all==null)) { return(some);} 	
 					}
 				}
 			}
 			}}
 	return(some);
 }
 
 //
 // true if from can move to to, including pyramids moving by the movement of any of their components.
 //
 private boolean canMoveFromTo(RithmomachyCell from,RithmomachyChip fromChip,RithmomachyCell to)
 {	if(fromChip==null)
 	{// see if some component of the pyramid can reach the destination
	 for(int height=from.height()-1; height>=0; height--)
	 {
		 if(canMoveFromTo(from,from.chipAtIndex(height),to)) { return(true); }
	 }
	 return(false);
 	}
 	else
 	{
 		switch(movementStyle)
 		{
 		default: throw G.Error("not expecting style "+movementStyle);
 		case linear: return(canMoveLinearFromTo(from,fromChip,to));
 		case flying: return(canMoveFlyingFromTo(from,fromChip,to));
 		}
 	} 
 }
 // all attack elements are fixed.
 private boolean getCapturesByReplacementDeceit(CaptureStack all,
		 				RithmomachyCell victim,RithmomachyChip victimChip,int victimValue,
		 				RithmomachyCell from,RithmomachyChip fromChip,int fromValue,
		 				RithmomachyCell from2,RithmomachyChip from2Chip,int from2Value)
 {
	 boolean some = false;
	 if( isAttackByDeceit(fromValue,from2Value,victimValue)	// is this a valid attack?
		  && canMoveFromTo(from,fromChip,victim)			// and can the attackers actually reach the victim
		  && canMoveFromTo(from2,from2Chip,victim))			// ...
	 {
		 if(all==null) { return(true); }
		 some = true;
		 all.addElement(new CaptureDescription(CaptureType.Deceit,victim,victimChip,from,fromChip,from2,from2Chip));
	 }
	 return(some);
 }
 
 // attack element of the victim and one attacker is fixed
 private boolean getCapturesByReplacementDeceit(CaptureStack all,RithmomachyCell victim,RithmomachyChip victimTop,int victimValue,RithmomachyCell from,RithmomachyChip fromChip,int fromValue,RithmomachyCell from2)
 {
	 boolean some = false;
	 int from2Height = from2.height();
	 if((from2Height>1) && from2.canAttackAsPyramid())
	 {
		 some |= getCapturesByReplacementDeceit(all,victim,victimTop,victimValue,from,fromChip,fromValue,from2,null,from2.stackValue());
	 	 if(some && (all==null)) { return(true); }
	 }
	 while(from2Height-- > 0)
	 {	RithmomachyChip from2Chip = from2.chipAtIndex(from2Height);
		 some |= getCapturesByReplacementDeceit(all,victim,victimTop,victimValue,from,fromChip,fromValue,from2,from2Chip,from2Chip.value);
	 	 if(some && (all==null)) { return(true); }
	 }
	 return(some);
 }
 
 // attack element of the victim is fixed
 private boolean getCapturesByReplacementDeceit(CaptureStack all,
		 	RithmomachyCell victim,RithmomachyChip victimTop,int victimValue,
		 	RithmomachyCell from,RithmomachyCell from2)
 {	boolean some = false;
 	int fromHeight = from.height();
 	if((fromHeight>1) && from.canAttackAsPyramid()) 
 	{
 		some |= getCapturesByReplacementDeceit(all,victim,victimTop,victimValue,from,null,from.stackValue(),from2);
 		if(some && (all==null)) { return(true); }
 	}
 	while(fromHeight-- > 0)
 	{
 		RithmomachyChip fromChip = from.chipAtIndex(fromHeight);
 		some |= getCapturesByReplacementDeceit(all,victim,victimTop,victimValue,from,fromChip,fromChip.value,from2);
 		if(some && (all==null)) { return(true); }
 	}
 	return(some);
 }
 //
 // victim, from, and from2 are appropriate colors and within geometric range, but at this point
 // we don't know if a capture or movement is actually possible.
 // any of victim, from and from2 may be pyramids, in which case they use either their full stack value
 // or the value of any components as the attack element
 //
 private boolean getCapturesByReplacementDeceit(CaptureStack all,RithmomachyCell victim,RithmomachyCell from,RithmomachyCell from2)
 {	boolean some = false;
 	int vheight = victim.height();
 	if(vheight>1) 
 		{ some |=getCapturesByReplacementDeceit(all,victim,null,victim.stackValue(),from,from2);
 		  if(all==null) { return(true); }
 		}
 	while(vheight-- >0)
 	{
 		RithmomachyChip vtop = victim.chipAtIndex(vheight);
 		some |= getCapturesByReplacementDeceit(all,victim,vtop,vtop.value,from,from2);
 	}
 	return(some);
 }
 
 private boolean getCapturesByReplacementDeceit(CaptureStack all,RithmomachyCell dest,int who)
 {	boolean some = false;
	for(int xdistance=-3; xdistance<=3; xdistance++)
	{ for(int ydistance=-3; ydistance<=3; ydistance++)
	  {	RithmomachyCell from = getCell((char)(dest.col+xdistance),dest.row+ydistance);
	    if(from!=null)
	    {
	    RithmomachyChip ftop = from.topChip();
	    if((ftop!=null) && (playerIndex(ftop)==who))
	    	{
	    	// complete the 3x3 raster starting with the next cell in the scan
	    	for(int xdistance2=xdistance; xdistance2<=3; xdistance2++)
	    	{ for(int ydistance2=(xdistance2!=xdistance)?-3:ydistance+1; ydistance2<=3; ydistance2++)
	    		{
	    		RithmomachyCell from2 = getCell((char)(dest.col+xdistance2),dest.row+ydistance2);
	    		if(from2!=null)
	    		{
	    			RithmomachyChip ftop2 = from2.topChip();
	    			if((ftop2!=null) && (playerIndex(ftop2)==who))
	    			{
	    	    		// from and from2 are two candidate attackers
	    				some |= getCapturesByReplacementDeceit(all,dest,from,from2);
	    				if(some && (all==null)) { return(true); }
	    			}
	    		}
	    		}
	    	}
	    	}
	    }
	  }
	}
 	return(some);
 }

 
 //
 // captures by deceit use two attacking pieces whose sum difference product or quotient match
 //
 private boolean getCapturesByDeceit(CaptureStack all,RithmomachyCell dest,int who)
 {	
	 switch(deceitStyle)
	 {
	 default: throw G.Error("not expecting %s",deceitStyle);
	 case custodial: return(getCapturesByCustodialDeceit(all,dest,who));
	 case replacement: return(getCapturesByReplacementDeceit(all,dest,who));
	 }
 }
 //
 // captures by siege are all 4 othogonal or all four diagonal directions
 //
 private boolean getCapturesBySeige(CaptureStack all,RithmomachyCell c,int who)
 {	boolean some = false;
   	for(int direction = 0; direction<2; direction++)	// include a diagonal start and an orthogonal start
 	{	boolean match = true;
 		RithmomachyCell src = null;
 		for(int inc = 0; match && inc<8; inc+=2)		// four directions in the same set
 		{
 		RithmomachyCell d1 = c.exitTo(direction+inc);
 		match &= ((d1==null) || (playerIndex(d1)==who));
 		if(d1!=null) { src = d1; }
 		}
 		if(match)
 		{	some = true;
 			if(all==null) { return(true); }
 			all.addElement(new CaptureDescription(CaptureType.Siege,c,null,src,null,null,null));
  		}
 	}
 	return(some);
 }
 

 // get captures, who is the player moving.
 // results is a stack of descriptions or null
 // return true if any are found
 private boolean getCaptures(RithmomachyCell c,CaptureStack results,int who)
 {	boolean some=false;
	int owner = playerIndex(c);
	if(owner==who)
			{ 
			// get captures which originate from c 
			some |= getCapturesByEncounterOrEquality(results,c,who);
			}
		else if(owner==nextPlayer[who])
			{ 
			// get captures by deceit which can capture this
			some |= getCapturesByDeceit(results,c,who);
			if((results!=null) || !some) { some |= getCapturesBySeige(results,c,who); }
			}
	return(some);
 }
 
 // return true if any captures are available.  If st is not null, 
 // collect a complete list of moves that are captures.
 private boolean getCaptures(CaptureStack results,int who)
 {	boolean some = false;
	for(RithmomachyCell c=allCells; ((results!=null) || !some) && c!=null; c=c.next)
		{
		some |= getCaptures(c,results,who);
		if(some && (results==null)) {return(some); }
		}
	return(some);
 }
 // return a complete list of captures available to who
 public CaptureStack getCaptures(int who)
 {
	CaptureStack st = new CaptureStack();
	getCaptures(st,who);
	return(st);
 }

 public boolean capturesAvailable()
 {	return(getCaptures(null,whoseTurn));
 }
 
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
 	switch(board_state)
 	{
 	case Confirm:
 		all.addElement(new RithmomachyMovespec(MOVE_DONE,whoseTurn));
 		break;

 	case Play:
 		{
 		boolean some = false;
 		for(RithmomachyCell c = allCells; c!=null; c=c.next)
 		{	if(c.height()>0)
 			{
 			if( playerIndex(c)==whoseTurn)
 			{	
 				// potential moves
 				some |= getMovesFrom(all,c,c.getDistanceMask(),whoseTurn);
 				// potential capture by encounter or eruption
 			}
 			}	
 		}
 		
 		if(!some) { all.addElement(new RithmomachyMovespec(MOVE_RESIGN,whoseTurn)); }
 		}
 		
 		break;
 	case Gameover:
 		break;
 	default:
 		throw G.Error("Not implemented");
 	}
 	G.Assert(all.size()>0,"some moves generated");
  	return(all);
 }
 
}
