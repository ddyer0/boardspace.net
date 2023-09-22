/* copyright notice */package syzygy;


import java.awt.Rectangle;

import java.util.Hashtable;

import online.game.*;
import lib.*;


/**
 * CookieBoard knows all about the game of Cookie Disco, which is played
 * on a hexagonal board. It gets a lot of logistic support from 
 * common.hexBoard, which knows about the coordinate system.  
 * 
 * @author ddyer
 *
 */

class SyzygyBoard extends hexBoard<SyzygyCell> implements BoardProtocol,SyzygyConstants
{	
    static final String[] SyzygyGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
    static final String Syzygy_Init = "syzygy"; //init for standard game
	private SyzygyState unresign;
	private SyzygyState board_state;
	public SyzygyState getState() {return(board_state); }
	public void setState(SyzygyState st) 
	{ 	unresign = (st==SyzygyState.RESIGN_STATE)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	//
    // private variables
    //
// this is required even if it is meaningless for this game, but possibly important
// in other games.  When a draw by repetition is detected, this function is called.
// the game should have a "draw pending" state and enter it now, pending confirmation
// by the user clicking on done.   If this mechanism is triggered unexpectedly, it
// is probably because the move editing in "editHistory" is not removing last-move
// dithering by the user, or the "Digest()" method is not returning unique results
// other parts of this mechanism: the Viewer ought to have a "repRect" and call
// DrawRepRect to warn the user that repetitions have been seen.
	public void SetDrawState() 
		{ setState(SyzygyState.DRAW_STATE);  };	

	public SyzygyChip cherryChip = null;	// the chip color marked by the cherry
	public SyzygyCell cherryCell = null;	// the cell that contains the cherry chip
	
    //public CookieCell cherryCell = null;	// the destination of the last move.
    public int FullSize = 0;				// set to the number of chips in play
    
    public CellStack animationStack = new CellStack();	// in replay or robot moves, set to the path of the cookie's slide
    public SyzygyChip pickedObject = null;	// the location picked up
    public SyzygyChip lastPicked = null;	// for the aux sliders
    public SyzygyCell getDest() { return(droppedDestStack[ stackIndex-((stackIndex>0)?1:0)]); }
    public SyzygyChip lastDroppedDest = null;	// for image adjustment logic

	public SyzygyCell placedChips=null;	// linked list of nonempty cells on the board
	private SyzygyChip nextCherryChip = null;
	private SyzygyCell nextCherryCell = null;
	
	private SyzygyState unwind_state = SyzygyState.PUZZLE_STATE;
    private int sweep_counter = 0;			// used when scanning for complete groups
    private ChipStack robotChipStack = new ChipStack();
    private CellStack robotCellStack = new CellStack();
    private SyzygyCell pickedSourceStack[] = new SyzygyCell[3]; 
    private SyzygyCell droppedDestStack[] = new SyzygyCell[3];
    private int stackIndex = 0;
    private SyzygyCell tempDests[] = new SyzygyCell[40];	// temporary for the move generator 

    public SyzygyCell getSource() 
		{ return((pickedObject==null)?null:pickedSourceStack[stackIndex]); }


    private boolean placeChips(SyzygyCell pos, int directions[],int pattern[],int index)
    {	int lim = directions.length;
    	if(index==lim) 
    		{ return(true); }	// success
    	
    	for(int i=index; i<directions.length; i++)
    	{	SyzygyChip chip = SyzygyChip.getChip(pattern[i]);
    		boolean permitted = true;
    		// see if we can place this chip in this place
    		for(int dir = 0; permitted && dir<CELL_FULL_TURN; dir++)
    		{	SyzygyCell c = pos.exitTo(dir);
    			SyzygyChip top = c.topChip();
    			permitted &= (top!=chip);
    		}
    		if(permitted)
    		{	int temp = pattern[index];
    			pattern[index] = pattern[i];
    			pattern[i]=temp;
    			pos.addChip(chip);
    			pos.nextPlaced = placedChips;
    			placedChips = pos;
    			boolean success = placeChips(pos.exitTo(1+directions[index]),directions,pattern,index+1);
    			if(success) { return(true); }
    			pos.removeTop();
    			placedChips = pos.nextPlaced;
    			pos.nextPlaced = null;
    		}
    	}
    	return(false);	// backtrack
    }
    
    // place a pattern of cookies.  Used to initialize the board
    private void placePattern(SyzygyCell center,Random r)
    {	
   			int directions[] = { 2,4,3,1, 1,2,4,4, 4,2,1,1, 3,4,2,3};
   			int pattern[] = {0,0,0,0,  1,1,1,1,  2,2,2,2, 3,3,3,3};
   			r.shuffle(pattern);
   			placeChips(center,directions,pattern,0);

    }
    

    
    // count the number of chips in the group
    // filled is a cell deemed to be filled and empty is a cell deemed to be empty,
    // so we can call this function with a proposed move instead of an actual one.  
    private int sweepGroupSize(SyzygyCell seed,SyzygyCell empty)
    {	if(seed==null) { return(0); }
		if(seed.sweep_counter == sweep_counter) { return(0); }
		boolean isEmpty = (seed==empty)?true : (seed.topChip()==null);
		if(isEmpty) { return(0); }
		int totalv = 1;
		seed.sweep_counter = sweep_counter;		// mark it as seen
		for(int dir = 0;dir<CELL_FULL_TURN; dir++)
		{	totalv += sweepGroupSize(seed.exitTo(dir),empty);
		}
		return(totalv);
    }	    
    //
    // get the group size from seed, asserting that "empty" is empty and "filled" is filled.
    // this is used to see if the group would be split by a trial move
    //
    private int groupSize(SyzygyCell seed,SyzygyCell empty)
    {   sweep_counter++;
    	return(sweepGroupSize( seed,empty));
    }




    private int slither_internal_v1(int stepmask,	// a bitmask, 1<<n means stop after n more steps
    		SyzygyCell anchor,			// the filled cell we are rolling around.
    		SyzygyCell source,			// the current empty cell
    		SyzygyCell prev,			// the previous empty cell, which we don't return to
    		SyzygyCell dests[],			// the accumulated destinations
    		int destsIndex,				// index into the dests
    		SyzygyCell startingCell,			// the original cell, which is defined as filled even if it's actually empty
    		int steps)					// sanity check in case of bugs.
    {	
    	G.Assert(steps<100,"too many steps");
		//System.out.println("s "+source+direction);
		stepmask = stepmask >> 1;
		if(stepmask!=0)
		{
		//if(anchor==null) { G.print(""); }
		//if(startingCell.row==9 && startingCell.col=='K')
		//	{ G.print("S "+anchor+" "+source+" "+stepmask); }
		int len = source.nAdjacentCells();
		for(int i=1; (i<=len);i++)
		{	int dir = i%len;
			SyzygyCell c = source.exitTo(dir);
			if( (c!=prev)			// not backtracking
				&& (c!=startingCell)		// not a complete circle
				&& (c.height()==0) 	// not occupied
				&& c.isAdjacentTo(anchor)	// still adjacent to the anchor cell
				// not a "gate" cell between two occupied cells
				&&  (source.exitTo(i+1).isEmptyOr(startingCell) || source.exitTo(i+len-1).isEmptyOr(startingCell))
				) 
				{
				SyzygyCell newanchor = c.pickNewAnchor(anchor,startingCell);
				SyzygyCell gate = c.isGatedCell_v1(startingCell,newanchor);
				boolean stop = (newanchor!=anchor) || (gate!=null);
				//G.print("anchor "+anchor+" newanchor "+newanchor +" stop "+stop +" Gate "+gate);
				if(gate!=null) { newanchor = gate; }
				int recursivemask =  stop ? stepmask : (stepmask<<1);
				if(stop && (recursivemask&1) != 0) 
				  { if(!G.arrayContains(dests,c,destsIndex)) { dests[destsIndex++] = c;  } 
				  }
				destsIndex=slither_internal_v1(recursivemask,stop?newanchor:anchor,c,source,dests,destsIndex,startingCell,++steps);
				}
		}
		}
		return(destsIndex);
    }
    private int slither_internal_v2(int stepmask,	// a bitmask, 1<<n means stop after n more steps
    		SyzygyCell anchor,			// the filled cell we are rolling around.
    		SyzygyCell source,			// the current empty cell
    		SyzygyCell prev,			// the previous empty cell, which we don't return to
    		SyzygyCell dests[],			// the accumulated destinations
    		int destsIndex,				// index into the dests
    		SyzygyCell startingCell,			// the original cell, which is defined as filled even if it's actually empty
    		int steps)					// sanity check in case of bugs.
    {	
    	G.Assert(steps<100,"too many steps");
		//System.out.println("s "+source+direction);
		stepmask = stepmask >> 1;
		if(stepmask!=0)
		{
		//if(anchor==null) { G.print(""); }
		//if(startingCell.row==9 && startingCell.col=='K')
		//	{ G.print("S "+anchor+" "+source+" "+stepmask); }
		int len = source.nAdjacentCells();
		for(int i=1; (i<=len);i++)
		{	int dir = i%len;
			SyzygyCell c = source.exitTo(dir);
			if( (c!=prev)			// not backtracking
				&& (c!=startingCell)		// not a complete circle
				&& (c.height()==0) 	// not occupied
				&& c.isAdjacentTo(anchor)	// still adjacent to the anchor cell
				// not a "gate" cell between two occupied cells
				&&  (source.exitTo(i+1).isEmptyOr(startingCell) || source.exitTo(i+len-1).isEmptyOr(startingCell))
				) 
				{
				SyzygyCell newanchor = c.pickNewAnchor(anchor,startingCell);
				SyzygyCell gate = c.isGatedCell_v2(startingCell,newanchor);
				boolean stop = (newanchor!=anchor) || (gate!=null);
				//G.print("anchor "+anchor+" newanchor "+newanchor +" stop "+stop +" Gate "+gate);
				if(gate!=null) { newanchor = gate; }
				int recursivemask =  stop ? stepmask : (stepmask<<1);
				if(stop && (recursivemask&1) != 0) 
				  { if(!G.arrayContains(dests,c,destsIndex)) { dests[destsIndex++] = c;  } 
				  }
				destsIndex=slither_internal_v2(recursivemask,stop?newanchor:anchor,c,source,dests,destsIndex,startingCell,++steps);
				}
		}
		}
		return(destsIndex);
    }
   
    // slither around the hive.  If nsteps>0, step n times before landing (spider)
    // call with 3 for spider, 1 for queen, 0 for ant
    // stops when count reaches 0, firstC is reached
    // avoids backtracking using prevC.
    // direction direction+1 is the first direction searched.  This is used to force
    // ants to take a "left hand walk" so they don't get confused by U shaped hives.
    private int slither(int stepmask,SyzygyCell source,SyzygyCell dests[])
    {	boolean REGRESSION_TEST = false;	// if true, compare to previous verison
    	boolean USE_OLD = false;			// if true, use the old version's values
    	int di = 0;
    	for(int i=0;i<CELL_FULL_TURN;i++) 
    	{	SyzygyCell adj = source.exitTo(i);
    		if(adj.topChip()!=null) 
    		{   di = slither_internal_v2(stepmask,adj,source,null,dests,di,source,0); 
    		}
    	}
		if(REGRESSION_TEST)
		{
		int olddi = 0;
		SyzygyCell testdests[] = new SyzygyCell[20];
		for(int i=0;i<CELL_FULL_TURN;i++)
		{
		SyzygyCell adj = source.exitTo(i);
		if(adj.topChip()!=null) 
			{olddi = slither_internal_v1(stepmask,adj,source,null,USE_OLD?dests:testdests,olddi,source,0); 
			}
		}
		if(USE_OLD) { return(olddi); }
		for(int i=0;i<olddi;i++)
		{	boolean found = false;
			for(int j=0;j<di;j++)
			{
				if(dests[j]==testdests[i]) {found=true; }
			}
			G.Assert(found,"old from %s has %s, missing",source,testdests[i]);
		}
		
		for(int i=0;i<di;i++)
		{	boolean found = false;
			for(int j=0;j<olddi;j++)
			{
				if(dests[i]==testdests[j]) {found=true; }
			}
			G.Assert(found,"new from %s has %s, missing",source,dests[i]);
		}

		
		}
    	return(di);
     }


    // internal function, see slitherPath below this
    private boolean slitherpath_v2(int stepmask,	// a bitmask, 1<<n means stop after n more steps
    		SyzygyCell anchor,			// the filled cell we are rolling around.
    		SyzygyCell source,			// the current empty cell
    		SyzygyCell dest,			// the destination of the slithering
    		SyzygyCell prev,			// the previous empty cell, which we don't return to
    		CellStack path,		// path we travel
    		SyzygyCell startingCell			// the original cell, which is defined as filled even if it's actually empty
    		)
    {	
		//System.out.println("s "+source+direction);
		stepmask = stepmask >> 1;
		if(stepmask!=0)
		{
		//if(anchor==null) { G.print(""); }
		//G.print("S "+anchor+" "+source+" "+stepmask);
		int len = source.nAdjacentCells();
		for(int i=1; (i<=len);i++)
		{	int dir = i%len;
			SyzygyCell c = source.exitTo(dir);
			if( (c!=prev)			// not backtracking
				&& (c!=startingCell)		// not a full circle
				&& (c.height()==0) 	// not occupied
				&& c.isAdjacentTo(anchor)
				// not a gate cell between two filled cells
				&& (source.exitTo(i+1).isEmptyOr(startingCell) || (source.exitTo(-1+len+i).isEmptyOr(startingCell)))
				) 
				{
				SyzygyCell newanchor = c.pickNewAnchor(anchor,startingCell);
				SyzygyCell gate = c.isGatedCell_v2(startingCell,newanchor);
				boolean stop = (newanchor!=anchor) || (gate!=null);
				if(gate!=null) { newanchor = gate; }
				int recursivemask =  stop ? stepmask : (stepmask<<1);
				if(stop && (recursivemask&1) != 0) 
				  { if(c==dest) 
				  	{ path.push(source);
				  	  path.push(c); 
				  	  return(true); 
				  	} 
				  }
				// build the path on the way down.  If we get to the intended target, keep the
				// path we built, otherwise unwind on the way up
		    	path.push(source);
				if(slitherpath_v2(recursivemask,stop?newanchor:anchor,c,dest,source,path,startingCell)) 
					{ return(true); }
				path.pop();
				}
			}
		}
		return(false);
    }
 
    //
    // this is the same logic as "slither" but returns the full path of cells
    // we pass over, and true if the direction of travel is clockwise.
    // this is used to set up the animation of the movement.
    //
    private boolean slitherPath(SyzygyCell from,SyzygyCell to,CellStack path)
    {	int mask = from.slitherDistanceMask();
    	CellStack newpath = new CellStack();
    	boolean cw = false;
    	path.clear();		// will be set to the shortest path
    	for(int i=0;i<CELL_FULL_TURN;i++) 
    	{	SyzygyCell adj = from.exitTo(i);
    		{
    		// travel clockwise from an anchor cell
    		SyzygyCell prev = from.exitTo(i-1);
    		if((adj.topChip()!=null)&&(prev.topChip()==null))
    		{   newpath.clear();
    			if(slitherpath_v2(mask,adj,from,to,from.exitTo(i+1),newpath,from))
    			{
    			if((path.size()==0)||(path.size()>newpath.size())) { path.copyFrom(newpath); cw = true; }
    			}
    		}}
      		{
      		// travel counterclockwise from an anchor cell 
      		SyzygyCell next = from.exitTo(i+1);
      		if((adj.topChip()!=null)&&(next.topChip()==null))
    		{   newpath.clear();
    			if(slitherpath_v2(mask,adj,from,to,from.exitTo(i-1),newpath,from))
    			{
    			if((path.size()==0)||(path.size()>newpath.size())) { path.copyFrom(newpath); cw = false;}
    			}
    		}}
    	}
    	return(cw);
     }  

    // when there is a picked object from the board, get a table
    // of the places it could land.
    public Hashtable<SyzygyCell,SyzygyMovespec> movingObjectDests()
    {	
    	Hashtable<SyzygyCell,SyzygyMovespec> h = new Hashtable<SyzygyCell,SyzygyMovespec>();
  
    	switch(board_state)
    	{

    	case CONFIRM_STATE:
    		break;

       	case PLAY_STATE:
    		if(pickedObject!=null) { h = getDests(getSource(),pickedObject); }
			break;
		case PUZZLE_STATE:
 		default:
			break;
    	}
    	return(h);
    }
	// factory method to generate a board cell
	public SyzygyCell newcell(char c,int r)
	{	return(new SyzygyCell(c,r,SyzId.BoardLocation));
	}
    public SyzygyBoard(String init) // default constructor
    {
        Random r = new Random(72094);
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = SyzygyGRIDSTYLE;
        isTorus=true;

        initBoard(ZfirstInCol19, ZnInCol19, null); //this sets up a hexagonal board
        
        allCells.setDigestChain(r);
        doInit(init,randomKey); // do the initialization 
    }
    public SyzygyBoard cloneBoard() 
	{ SyzygyBoard dup = new SyzygyBoard(gametype); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((SyzygyBoard)b); }

    public void sameboard(BoardProtocol f) { sameboard((SyzygyBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(SyzygyBoard from_b)
    {
        super.sameboard(from_b); // compares the boards using sameCell
        
        //G.Assert(pickedObject==from_b.pickedObject,"same pickedObject");
        //G.Assert((placedChips==from_b.placedChips));
        // here, check any other state of the board to see if
        G.Assert((stackIndex == from_b.stackIndex),"stackIndex matches");
        G.Assert(SyzygyCell.sameCell(cherryCell,from_b.cherryCell),"cherry cell matches");
        G.Assert(cherryChip==from_b.cherryChip,"cherryChip matches");
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");
        
    }

    /** 
     * Digest produces a 64 bit hash of the game state.  This is used 4 different
     * ways in the system.
     * (1) This is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game, and a midpoint state of the game. Other site machinery
     *  looks for duplicate digests.  
     * (2) Digests are also used as the game is played to look for draw by repetition.  The state
     * after most moves is recorded in a hashtable, and duplicates/triplicates are noted.
     * (3) Digests are used by the search machinery as a check on the robot's winding/unwinding
     * of the board position, this is mainly a debug/development function, but a very useful one.
     * (4) when moves are effectively undone by executing a new move, the do/undo is removed
     * from the game record by comparing digests.
     * @return
     */
    public long Digest()
    {
        Random r = new Random(64 * 1000); // init the random number generator
    	long v = 0;
 
        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        
		for(SyzygyCell c=allCells; c!=null; c=c.next)
		{	
            v ^= c.Digest(r);
		}
		v ^= randomKey;
		v ^= (pickedObject==null)? 0 : pickedObject.Digest();
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
		v ^= SyzygyChip.Digest(r,cherryChip);
        return (v);
    }
    
    // C may be a cell from a clone of the board.  get the corresponding
    // cell on this board.
    public SyzygyCell getCell(SyzygyCell c)
    {	return(c==null?null:getCell(c.rackLocation(),c.col,c.row));
    }
    public SyzygyCell getCell(SyzId source,char col,int row)
    {
    	switch(source)
    	{
    	default: throw G.Error("Not expecting source %s",source);
    	case BoardLocation: return(getCell(col,row));
    	}
    }
    // this visitor method implements copying the contents of a cell on the board
    public void copyFrom(SyzygyCell cc,SyzygyCell fc)
    {	super.copyFrom(cc,fc);
    	cc.nextPlaced = getCell(fc.nextPlaced);
    }
    
    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(SyzygyBoard from_b)
    {	super.copyFrom(from_b);

        unwind_state = from_b.unwind_state;
        stackIndex = from_b.stackIndex;
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,droppedDestStack);
        lastPicked = null;
        cherryChip = from_b.cherryChip;
        nextCherryChip = from_b.cherryChip;
        nextCherryCell = getCell(from_b.nextCherryCell);
        cherryCell = getCell(from_b.cherryCell);
        placedChips = getCell(from_b.placedChips);
        stackIndex = from_b.stackIndex;
        pickedObject = from_b.pickedObject;
        board_state = from_b.board_state;
        unresign = from_b.unresign;

        sameboard(from_b); 
    }
    public void doInit() { doInit(gametype,randomKey); }
 
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long randomv)
    {	randomKey = randomv;
    	Random R = new Random(randomv);
    	if(Syzygy_Init.equalsIgnoreCase(gtype)) {   }
    	else { throw G.Error(WrongInitError,gtype); }
        gametype = gtype;
        sweep_counter = 0;
        currentSources = null;
        setState(SyzygyState.PUZZLE_STATE);
        unwind_state = board_state;
        whoseTurn = FIRST_PLAYER_INDEX;
        for(int i=0;i<droppedDestStack.length;i++) { pickedSourceStack[i]=droppedDestStack[i]=null; }
        stackIndex = 0;
        placedChips = null;
        lastDroppedDest = null;
        cherryChip = null;
        nextCherryChip = null;
        nextCherryCell = null;
        cherryCell = null;
        // set the initial contents of the board to all empty cells
		for(SyzygyCell c = allCells; c!=null; c=c.next)
			{ c.reInit(); 
			}
		
        moveNumber = 1;
        placePattern(getCell('H',9),R);
        // Everything is a constant for the duration of the game, the value of all chips in play
        FullSize = groupSize(placedChips,null);
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
        case PUZZLE_STATE:
            break;
        case CONFIRM_STATE:
        case DRAW_STATE:
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
        {
        case RESIGN_STATE:
        case DRAW_STATE:
        case CONFIRM_STATE:
            return (true);

        default:
            return (false);
        }
    }
    // this is the default, so we don't need it explicitly here.
    // but games with complex "rearrange" states might want to be
    // more selective.  This determines if the current board digest is added
    // to the repetition detection machinery.
    public boolean DigestState()
    {	
    	return(DoneState());
    }
    
    //
    // this is the trickiest bit in the game logic.  It is illegal to
    // temporarily split the cookies, but it is mandatory to permanantly
    // split when you win, and it is not leagal to split and lose or draw.
    // as usual, "from" and "to" are a move that hasn't actually been made,
    // so we must simulate their state.
    //
    private boolean illegalSplitMove(int player,SyzygyCell from)
    {	int total = FullSize;
    	SyzygyCell seed = (from==placedChips) ? placedChips.nextPlaced : placedChips;
    	int size = groupSize(seed,from) + ((from==null) ? 0 : 1);
    	return(size!=total);
     }

    public boolean WinForPlayerNow(int player)
    {	return(win[player]);
    }


    private void removePlacedChip(SyzygyCell cell)
    {	SyzygyCell prev = null;
    	SyzygyCell curr = placedChips;
    	//G.print("Remove "+cell);
    	while(curr!=null)
    	{	if(curr == cell)
    		{	if(prev==null) { placedChips = curr.nextPlaced; }
    			else { prev.nextPlaced = curr.nextPlaced; }
    			curr.nextPlaced = null;
    			return;
    		}
    	prev = curr;
    	curr = curr.nextPlaced;
    	}
    	throw G.Error("placed chip "+cell+" not found");
    }
    private void addPlacedChip(SyzygyCell cell)
    {  	G.Assert(cell.nextPlaced==null,"Already placed");
    	//G.print("Add "+cell);
        cell.nextPlaced = placedChips;
    	placedChips = cell;
    }

    // set the contents of a cell, and maintian the books
    private SyzygyChip SetBoard(SyzygyCell c,SyzygyChip ch)
    {	SyzygyChip old = c.topChip();
    	if(old!=ch)
    	{
    	if(c.onBoard)
    	{
    	if(old!=null) {  if(ch==null) { removePlacedChip(c); }}
     	if(ch!=null) {  if(old==null) { addPlacedChip(c); }}
    	}
       	if(ch!=null) 
       		{ c.addChip(ch);
       		} else 
       		{ c.removeTop(); 
      		}
    	}
    	return(old);
    }
    //
    // accept the current placements as permanant
    //
    private void acceptPlacement()
    {	while(stackIndex>=0)
    	{	droppedDestStack[stackIndex] = null;
    		pickedSourceStack[stackIndex] = null;
    		stackIndex--;
    	}
    	stackIndex=0;
    	nextCherryChip = null;
    	nextCherryCell = null;
    	currentSources = null;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    	if((stackIndex>0) && (droppedDestStack[stackIndex-1]!=null)) 
    	{	stackIndex--;
    		pickedObject = SetBoard(droppedDestStack[stackIndex],null); 
    		nextCherryCell = null;
    		nextCherryChip = null;
    		droppedDestStack[stackIndex] = null;
     	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	if(pickedObject!=null)
    	{SyzygyCell src = pickedSourceStack[stackIndex];
    	if(src!=null)
    	{	pickedSourceStack[stackIndex] = null;
    		SetBoard(src,pickedObject);
    	}
    	pickedObject=null;
    	}
    }
    // 
    // drop the floating object on the board.
    //
    private void dropBoardCell(SyzygyCell c,replayMode replay)
    {   
    	nextCherryCell = c;
    	if(c.isEmpty())
    	{
    	// dropping a regular cookie, possibly with crawl on board
    	SetBoard(c,pickedObject);
        lastDroppedDest = pickedObject;
        droppedDestStack[stackIndex++] = c;
        nextCherryChip = pickedObject;
        pickedObject = null;
    	}
    	else { throw G.Error("Not expecting dest %s",c); }
    }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(SyzygyCell c)
    {	return((stackIndex>0) && (droppedDestStack[stackIndex-1]==c));
    }
    public boolean isDest(char col,int ro)
    {	return(isDest(getCell(col,ro)));
    }
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    { 	SyzygyChip ch = pickedObject;
    	if(ch!=null)
    	{
    	SyzygyCell src = pickedSourceStack[stackIndex];
    	if((src!=null) && (droppedDestStack[stackIndex]==null))
    	{	
    		return(ch.chipNumber()); 
    	}}
      	return (NothingMoving);
    }
    public boolean hasMovingObject(HitPoint highlight)
    { 	SyzygyChip ch = pickedObject;
    	if(ch!=null)
    	{
    	SyzygyCell src = pickedSourceStack[stackIndex];
    	if((src!=null) && (droppedDestStack[stackIndex]==null))
    	{	
    		return(true); 
    	}}
      	return (false);
    }

	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickBoardCell(SyzygyCell c)
    {

       	boolean wasDest = isDest(c);
      	unDropObject(); 
        if(!wasDest)
        	{pickedSourceStack[stackIndex] = c;
        	lastPicked = pickedObject = c.topChip();
        	droppedDestStack[stackIndex] = null;
        	SetBoard(c,null);
        	}
   
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(SyzygyCell c)
    {	return(c==pickedSourceStack[stackIndex]);
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
        case PLAY_STATE:
        	setState(SyzygyState.CONFIRM_STATE);
        	break;

        case PUZZLE_STATE:
			acceptPlacement();
            break;
        }
    }
    private boolean isSyzygy(SyzygyCell c,boolean pivot)
    {	if(c==null)
    	{
    	for(SyzygyCell f = placedChips;  f!=null; f = f.nextPlaced)
    		{
    			if(isSyzygy(f,true)) 
    				{ return(true); }
    		}
    		return(false);
    	}
    	else 
    	{
    	SyzygyChip top = c.topChip();
    	for(int dir=0; dir<6; dir++)
    	{
    		SyzygyCell d1 = c.exitTo(dir);
    		if(d1.topChip()==top)
    		{
    			SyzygyCell d2 = c.exitTo(dir+3);
    			if(d2.topChip()==top) 
    				{ return(true); }
    			if(!pivot) 
    				{ if(isSyzygy(d1,true))
    					{ return(true);  }
    				}
    		}
    	}
    	return(false);
    	}
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);

        case DRAW_STATE:
        	setState(SyzygyState.GAMEOVER_STATE);
        	break;
    	case CONFIRM_STATE:
       	case PLAY_STATE:
       	case PUZZLE_STATE:
        	boolean winForHim= isSyzygy(cherryCell,false);	// already set to next player
        	if(winForHim) 
        		{ win[nextPlayer[whoseTurn]]=true;
        		  setState(SyzygyState.GAMEOVER_STATE); 
        		}
        	else  
        		{
        		setState(SyzygyState.PLAY_STATE);
        		}
    		break;
    	}
       	unwind_state = board_state;
    }

    private void doDone(replayMode replay)
    {	cherryChip = nextCherryChip;
    	cherryCell = nextCherryCell;

        acceptPlacement();

        if (board_state==SyzygyState.RESIGN_STATE)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(SyzygyState.GAMEOVER_STATE);
    		unwind_state = board_state;
        }
        else
        {	
    		
       		setNextPlayer();
      		setNextStateAfterDone();
        }
        
    }
    public void SetDisplayRectangle(Rectangle r)
    {
    	super.SetDisplayRectangle(r);
    }

    public void setNextStateAfterPick()
    {
    	switch(board_state)
    	{
    	case PLAY_STATE:
    	case PUZZLE_STATE:	break;
    	case CONFIRM_STATE: 
    	case DRAW_STATE:
    		setState(unwind_state);
    		break;
    	default: throw G.Error("Not expecting state %s",board_state);
    	}
	
    }
    private void addAnimation(SyzygyCell c,SyzygyCell d)
    {	if(c.onBoard&&d.onBoard)
    	{
    	CellStack path = new CellStack();
		//boolean cw = 
			slitherPath(c,d,path);
		for(int i=0,lim=path.size()-1; i<lim; i++)
			{	animationStack.push(path.elementAt(i));
				animationStack.push(path.elementAt(i+1));
			}
    	}
    	else
    	{
    	animationStack.push(c);
		animationStack.push(d);
    	}
    }
    
    
    public boolean Execute(commonMove mm,replayMode replay)
    {	SyzygyMovespec m = (SyzygyMovespec)mm;

       //G.print("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
       case MOVE_DONE:

         	doDone(replay);

            break;

      	case MOVE_FROM_TO:
       		{
       		SyzygyCell c = getCell(m.from_col, m.from_row);
        	SyzygyCell d = getCell(m.to_col,m.to_row);
       		if(replay!=replayMode.Replay)
       		{	addAnimation(c,d);
       		}
          	pickBoardCell(c);
          	m.chip = pickedObject;
            dropBoardCell(d,replay);
            setNextStateAfterDrop();
       		}
       		break;
        case MOVE_DROPB:
        	{
			SyzygyCell c = getCell(m.to_col,m.to_row);
        	if(isSource(c))
    		{
    		unPickObject();
    		}
        	else
			{
			if(replay==replayMode.Single)
			{	SyzygyCell src = getSource();
				if(src!=null) { addAnimation(src,c); } 
			}
            dropBoardCell(c,replay);
            setNextStateAfterDrop();
			}
        	}
            break;
        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	{
        	SyzygyCell c = getCell(m.from_col, m.from_row);
        	pickBoardCell(c);
        	setNextStateAfterPick();
        	m.chip = pickedObject;
        	}
            break;

 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            setState(SyzygyState.PUZZLE_STATE);
            setNextStateAfterDone(); 
            break;
       case MOVE_RESIGN:
            setState(unresign==null?SyzygyState.RESIGN_STATE:unresign);
            break;
            // and be like reset
       case MOVE_EDIT:
        	acceptPlacement();
            setState(SyzygyState.PUZZLE_STATE);
            unwind_state = board_state;
            break;
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(SyzygyState.GAMEOVER_STATE);
			break;

        default:
        	cantExecute(m);
        }
        return (true);
    }


    public boolean LegalToHitBoard(SyzygyCell c)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
		case CONFIRM_STATE:
		case DRAW_STATE:
        	{
        	if(isDest(c)) { return(pickedObject==null); }	// pick up same chip
        	return(false); 
        	}
 		case PLAY_STATE:
			if(pickedObject!=null)
				{
				Hashtable<SyzygyCell,SyzygyMovespec>dests = getDests(getSource(),pickedObject);
				if(dests.get(c)!=null) { return(true); }
				if(isSource(c)) { return(true); }
				}
				else	// not moving
				{  Hashtable<SyzygyCell,SyzygyCell> sources = getSources();
				   return(sources.get(c)!=null);
				}
			return(false);
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			break;
        default:
        	throw  G.Error("Not expecting state %s", board_state);

        case PUZZLE_STATE:
            return ((pickedObject!=null) 
            		  ? (c.topChip()==null)
            		  : (c.topChip()!=null));
        }
        return(false);
    }
    
    StateStack robotState = new StateStack();
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(SyzygyMovespec m)
    {	robotState.push(board_state);
        m.undoinfo = stackIndex*100; //record the starting state. The most reliable
        robotChipStack.push(cherryChip);
        robotCellStack.push(cherryCell);
        stackIndex = 0;
        //G.print("R "+m);

        if (Execute(m,replayMode.Replay))
        {
            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {
                doDone(replayMode.Replay);
            }
            else if(m.op==MOVE_DROPB) { }
            else
            {
            	throw G.Error("Robot move should be in a done state");
            }
        }
       //G.Assert(cookieLocation[0]!=null && cookieLocation[1]!=null,"missing cookie 2");

    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(SyzygyMovespec m)
    {
       // G.print("U "+m+" for "+whoseTurn);
       // G.Assert(cookieLocation[0]!=null && cookieLocation[1]!=null,"missing cookie 3");

        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;
        case MOVE_DONE:
            break;
            
        case MOVE_DROPB:
        	{
        	SyzygyCell c = getCell(m.to_col,m.to_row);
       		SetBoard(c,null);
       		droppedDestStack[0]=pickedSourceStack[0]=null;
       		stackIndex = 0;
        	}
        	break;

       	case MOVE_FROM_TO:
       		{
       		SyzygyCell c = getCell(m.from_col, m.from_row);
        	SyzygyCell d = getCell(m.to_col,m.to_row);
           	pickBoardCell(d);
            dropBoardCell(c,replayMode.Replay);
            stackIndex = 0;
       		}
       		break;
        case MOVE_RESIGN:

            break;
        }
        setState(robotState.pop());
        cherryChip = robotChipStack.pop();
        cherryCell = robotCellStack.pop();
        stackIndex = (m.undoinfo%10000)/100;
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
       // G.Assert(cookieLocation[0]!=null && cookieLocation[1]!=null,"missing cookie 4");

 }

  

  
  /**
   * get moves for the regular cookie
   * return true if there are moves.
   * 
   * @param all may be null if all you want is the existence of moves
   * @param c
   * @param ch
   * @param includeCrawl if true, include the crawl moves
   * @param player
   */
 private boolean getMoveMoves(CommonMoveStack all,SyzygyCell c,SyzygyChip ch,int player)
 {	if(c.onBoard)
 	{
	 	int mask = c.slitherDistanceMask();
	  	int ndests = slither(mask,c,tempDests);
  		// for each of the possible destinations, a move is legal only if
	  	// the group is not split, or the game is won.
	  	if(ch!=cherryChip)
	  	{
	  	while(ndests>0)
	  	{	ndests--;
	  		SyzygyCell d = tempDests[ndests];
	  		if(!illegalSplitMove(player,c))
	  		{
	  		if(all==null)  { return(true); }
	  		all.push(new SyzygyMovespec(MOVE_FROM_TO,c.col,c.row,d.col,d.row,player));
	  		}
	  	}}

  	}
 	
 	return((all==null)? false : (all.size()>0));
 }

private  boolean getMoveMoves(CommonMoveStack  all,int player)
 {	

 	for(SyzygyCell c = placedChips;
        c!=null;
        c=c.nextPlaced)
 	{	
 		boolean some = getMoveMoves(all,c,c.topChip(),player);
 		if(some && (all==null)) { return(true); }
  	}
 	return((all==null) ? false : (all.size()>0));
 }
 
 

 
 private Hashtable<SyzygyCell,SyzygyCell> currentSources = null; 
 public Hashtable<SyzygyCell,SyzygyCell> getSources()
 {	if(currentSources!=null) { return(currentSources); }
 	Hashtable<SyzygyCell,SyzygyCell> res = new Hashtable<SyzygyCell,SyzygyCell>();
 	switch(board_state)
 	{
 	default: 
 	case PUZZLE_STATE:
 		for(SyzygyCell c = placedChips; c!=null; c=c.nextPlaced) 
 		{	res.put(c,c);
 		}
 		break;

 	case PLAY_STATE:
 		{
 		CommonMoveStack  all = new CommonMoveStack();
 		getMoveMoves(all,whoseTurn);
 	 	while(all.size()>0)
 	 	{	SyzygyMovespec mm = (SyzygyMovespec)all.pop();
 	 		if(mm.op==MOVE_FROM_TO)
	 			{  SyzygyCell from = getCell(mm.from_col,mm.from_row); 
	 				res.put(from,from);
	 			} 
	 	}
 		}
 		break;
 	}
 	currentSources = res;
 	return(res);
 }
 boolean hasLegalMoves(SyzygyCell c,int who,boolean includeCrawl)
 {
	 return(getMoveMoves(null,c,c.topChip(),who));
 }
 boolean hasLegalMoves(int whoseTurn)
 {	 
 	 return(getMoveMoves(null,whoseTurn));	// maybe don't include crawl moves
 }
 public Hashtable<SyzygyCell,SyzygyMovespec> getDests(SyzygyCell src,SyzygyChip picked)
 {	
 	Hashtable<SyzygyCell,SyzygyMovespec> res = new Hashtable<SyzygyCell,SyzygyMovespec>();
 	switch(board_state)
 	{
 	default: 
 	case PLAY_STATE:
 		{
 		CommonMoveStack  all = new CommonMoveStack();
 	 	getMoveMoves(all,src,picked,whoseTurn);
 	 	while(all.size()>0)
 	 	{	SyzygyMovespec mm = (SyzygyMovespec)all.pop();
 	 		if(mm.op==MOVE_FROM_TO)
	 			{  SyzygyCell dest = getCell(mm.to_col,mm.to_row); 
	 				res.put(dest,mm);
	 			} 
	 	}
 		}
 		break;
 	}
 	return(res);
 }
 


 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
 	switch(board_state)
 	{
 	default:	throw G.Error("Not implemented");

 	case PLAY_STATE:
 		getMoveMoves(all,whoseTurn);
 		if(all.size()==0) { all.push(new SyzygyMovespec(MOVE_RESIGN,whoseTurn)); }
 		
 		break;
 	}
 	
 	return(all);
 }
 
}
