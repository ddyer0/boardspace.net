package cookie;

import java.awt.Rectangle;

import online.game.*;
import java.util.*;

import lib.*;
import lib.Random;

/**
 * CookieBoard knows all about the game of Cookie Disco, which is played
 * on a hexagonal board. It gets a lot of logistic support from 
 * common.hexBoard, which knows about the coordinate system.  
 * 
 * @author ddyer
 *
 */

class CookieBoard extends hexBoard<CookieCell> implements BoardProtocol,CookieConstants
{	
    static final String[] CookieGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	
	CookieState unresign;
	CookieState board_state;
	public CookieState getState() {return(board_state); }
	public void setState(CookieState st) 
	{ 	unresign = (st==CookieState.RESIGN_STATE)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	boolean COUNT_CRAWL_FOR_FREEZE = true;
	public int robotDepth = 0;
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
		{ setState(CookieState.DRAW_STATE);  };	

	public CookieChip cherryChip = null;	// the chip color marked by the cherry
	public CookieCell cherryCell = null;	// the cell that contains the cherry chip
	
    //public CookieCell cherryCell = null;	// the destination of the last move.
    public CookieCell crawlCell = null;		// cell occupied by the crawl cookie
    public boolean crawlOption = false;
    public int Everything = 0;				// set the the total value of all cookies in play
    public int FullSize = 0;				// set to the number of chips in play
    public int startingPattern = 0;			// the index of the starting pattern
    
    public CellStack animationStack = new CellStack();	// in replay or robot moves, set to the path of the cookie's slide
    public CookieChip pickedObject = null;	// the location picked up
    public CookieChip lastPicked = null;	// for the aux sliders
    public CookieCell chipPool[] = new CookieCell[CookieChip.nChips+3];	//source for the player's representative chip
    public CookieCell playerChip[] = new CookieCell[2];					// the player's current chip, which can be swapped 
    public CookieCell getDest() { return(droppedDestStack[ stackIndex-((stackIndex>0)?1:0)]); }
    public CookieChip lastDropped = null;	// for image adjustment logic

	public CookieCell placedChips=null;	// linked list of nonempty cells on the board
	private CookieChip nextCherryChip = null;
	private CookieCell nextCherryCell = null;
	
	private CookieState unwind_state = CookieState.PUZZLE_STATE;
    private int sweep_counter = 0;			// used when scanning for complete groups
    private ChipStack robotChipStack = new ChipStack();
    private CellStack robotCellStack = new CellStack();
   private boolean swapped = false;
    private CookieCell pickedSourceStack[] = new CookieCell[3]; 
    private CookieCell droppedDestStack[] = new CookieCell[3];
    private CookieCell cookieLocation[] = new CookieCell[2];				// the current location of the player's cookie
    private int stackIndex = 0;
    private Hashtable<CookieCell,CookieMovespec>startingMoves;
    private CookieCell tempDests[] = new CookieCell[20];	// temporary for the move generator 
    private CookieCell crawlCellDestProxy[] = new CookieCell[3];	// maximum distance
    private CookieCell crawlCellSrcProxy = null;

    public CookieCell getSource() 
		{ return((pickedObject==null)?null:pickedSourceStack[stackIndex]); }

    private void addChip(CookieCell pos,CookieChip ch)
    {	pos.addChip(ch);
    	addPlacedChip(pos);
    }
    
    // place a pattern of cookies.  Used to initialize the board
    private void placePattern(String pat,CookieCell center)
    {	char key = pat.charAt(0);
    	switch(key)
    	{
    	default: throw G.Error("Unknown pattern key %s",key);
    	case 'C':	// center pattern
   			for(int i=1; i<=6;i++)
   			{	int tok = pat.charAt(i)-'1';
   				CookieChip ch = CookieChip.getChip(tok);
    			CookieCell pos = center.exitTo(i);
      			addChip(pos,ch);
   			}
   			break;
   		case 'T': 	// triangle pattern
   			int directions[] = { 0,0,4,5,1,1,0};
   			CookieCell pos = center;
   			for(int i=1; i<=6;i++)
   			{	int tok = pat.charAt(i)-'1';
   				CookieChip ch = CookieChip.getChip(tok);
   				addChip(pos,ch);
   				pos = pos.exitTo(directions[i]);
   			}
   			break;
    	}
    }
    
    //
    // sweep all adjacent cells and accumulate their values;
    // filled is a cell deemed to be filled and empty is a cell deemed to be empty,
    // so we can call this function with a proposed move instead of an actual one.
    //
    private int sweepGroupValue(CookieCell seed,CookieCell empty,CookieCell filled,CookieChip val)
    {	if(seed==null) { return(0); }
    	if(seed.sweep_counter == sweep_counter) { return(0); }
    	CookieChip top = (seed==empty)?null:(seed==filled)?val:seed.topChip();
    	if(top==null) { return(0); }
    	int totalv = ((seed==crawlCell)||((seed==filled)&&(empty==crawlCell))) ? 0 : top.value;				// accumulate it's value
    	seed.sweep_counter = sweep_counter;		// mark it as seen
    	for(int dir = 0;dir<CELL_FULL_TURN; dir++)
    	{	totalv += sweepGroupValue(seed.exitTo(dir),empty,filled,val);
    	}
    	return(totalv);
    }
    // calculate the total value of the group containing this cell
    // filled is a cell deemed to be filled and empty is a cell deemed to be empty,
    // so we can call this function with a proposed move instead of an actual one.
    private int groupValue(CookieCell c,CookieCell empty,CookieCell filled,CookieChip val)
    {	sweep_counter++;
    	if(c==empty) { c = filled; }
    	return(sweepGroupValue(c,empty,filled,val));   	
    }
    
    // count the number of chips in the group
    // filled is a cell deemed to be filled and empty is a cell deemed to be empty,
    // so we can call this function with a proposed move instead of an actual one.  
    private int sweepGroupSize(CookieCell seed,CookieCell empty,CookieCell filled)
    {	if(seed==null) { return(0); }
		if(seed.sweep_counter == sweep_counter) { return(0); }
		boolean isEmpty = (seed==empty)?true : (seed==filled) ? false : (seed.topChip()==null);
		if(isEmpty) { return(0); }
		int totalv = 1;
		seed.sweep_counter = sweep_counter;		// mark it as seen
		for(int dir = 0;dir<CELL_FULL_TURN; dir++)
		{	totalv += sweepGroupSize(seed.exitTo(dir),empty,filled);
		}
		return(totalv);
    }	    
    //
    // get the group size from seed, asserting that "empty" is empty and "filled" is filled.
    // this is used to see if the group would be split by a trial move
    //
    private int groupSize(CookieCell seed,CookieCell empty,CookieCell filled)
    {   sweep_counter++;
    	if(seed==empty) { seed = filled; }
    	return(sweepGroupSize( seed,empty,filled));
    }




    private int slither_internal_v1(int stepmask,	// a bitmask, 1<<n means stop after n more steps
    		CookieCell anchor,			// the filled cell we are rolling around.
    		CookieCell source,			// the current empty cell
    		CookieCell prev,			// the previous empty cell, which we don't return to
    		CookieCell dests[],			// the accumulated destinations
    		int destsIndex,				// index into the dests
    		CookieCell startingCell,			// the original cell, which is defined as filled even if it's actually empty
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
			CookieCell c = source.exitTo(dir);
			if( (c!=prev)			// not backtracking
				&& (c!=startingCell)		// not a complete circle
				&& (c.height()==0) 	// not occupied
				&& c.isAdjacentTo(anchor)	// still adjacent to the anchor cell
				// not a "gate" cell between two occupied cells
				&&  (source.exitTo(i+1).isEmptyOr(startingCell) || source.exitTo((i+len-1)).isEmptyOr(startingCell)))
				{
				CookieCell newanchor = c.pickNewAnchor(anchor,startingCell);
				CookieCell gate = c.isGatedCell_v1(startingCell,newanchor);
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
    		CookieCell anchor,			// the filled cell we are rolling around.
    		CookieCell source,			// the current empty cell
    		CookieCell prev,			// the previous empty cell, which we don't return to
    		CookieCell dests[],			// the accumulated destinations
    		int destsIndex,				// index into the dests
    		CookieCell startingCell,			// the original cell, which is defined as filled even if it's actually empty
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
			CookieCell c = source.exitTo(dir);
			if( (c!=prev)			// not backtracking
				&& (c!=startingCell)		// not a complete circle
				&& (c.height()==0) 	// not occupied
				&& c.isAdjacentTo(anchor)	// still adjacent to the anchor cell
				// not a "gate" cell between two occupied cells
				&&  (source.exitTo((i+1)).isEmptyOr(startingCell) || source.exitTo((i+len-1)).isEmptyOr(startingCell))
				) 
				{
				CookieCell newanchor = c.pickNewAnchor(anchor,startingCell);
				CookieCell gate = c.isGatedCell_v2(startingCell,newanchor);
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
    private int slither(int stepmask,CookieCell source,CookieCell dests[])
    {	boolean REGRESSION_TEST = false;	// if true, compare to previous verison
    	boolean USE_OLD = false;			// if true, use the old version's values
    	int di = 0;
    	for(int i=0;i<CELL_FULL_TURN;i++) 
    	{	CookieCell adj = source.exitTo(i);
    		if(adj.topChip()!=null) 
    		{   di = slither_internal_v2(stepmask,adj,source,null,dests,di,source,0); 
    		}
    	}
		if(REGRESSION_TEST)
		{
		int olddi = 0;
		CookieCell testdests[] = new CookieCell[20];
		for(int i=0;i<CELL_FULL_TURN;i++)
		{
		CookieCell adj = source.exitTo(i);
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
    		CookieCell anchor,			// the filled cell we are rolling around.
    		CookieCell source,			// the current empty cell
    		CookieCell dest,			// the destination of the slithering
    		CookieCell prev,			// the previous empty cell, which we don't return to
    		CellStack path,		// path we travel
    		CookieCell startingCell			// the original cell, which is defined as filled even if it's actually empty
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
			CookieCell c = source.exitTo(dir);
			if( (c!=prev)			// not backtracking
				&& (c!=startingCell)		// not a full circle
				&& (c.height()==0) 	// not occupied
				&& c.isAdjacentTo(anchor)
				// not a gate cell between two filled cells
				&& ( source.exitTo((i+1)).isEmptyOr(startingCell) || source.exitTo(-1+len+i).isEmptyOr(startingCell))) 
				{
				CookieCell newanchor = c.pickNewAnchor(anchor,startingCell);
				CookieCell gate = c.isGatedCell_v2(startingCell,newanchor);
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
    private boolean slitherPath(CookieCell from,CookieCell to,CellStack path)
    {	int mask = from.slitherDistanceMask(crawlCell);
    	CellStack newpath = new CellStack();
    	boolean cw = false;
    	path.clear();		// will be set to the shortest path
    	for(int i=0;i<CELL_FULL_TURN;i++) 
    	{	CookieCell adj = from.exitTo(i);
    		{
    		// travel clockwise from an anchor cell
    		CookieCell prev = from.exitTo(i-1);
    		if((adj.topChip()!=null)&&(prev.topChip()==null))
    		{   newpath.clear();
    			if(slitherpath_v2(mask,adj,from,to,from.exitTo(i+1),newpath,from))
    			{
    			if((path.size()==0)||(path.size()>newpath.size())) { path.copyFrom(newpath); cw = true; }
    			}
    		}}
      		{
      		// travel counterclockwise from an anchor cell 
      		CookieCell next = from.exitTo(i+1);
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
    private boolean crawlPath(int mask,CookieCell from,CookieCell to,CellStack path)
    {	if(from.sweep_counter==sweep_counter) {}
    	else if((to==from) && ((mask&1)!=0)) {  path.push(to); return(true); }
    	else if(mask>1)
    	{	from.sweep_counter = sweep_counter;
    		for(int direction = 0;direction<CELL_FULL_TURN; direction++)
    		{
    			CookieCell next = from.exitTo(direction);
    			if(next.topChip()!=null)
    				{ boolean gotthere = crawlPath(mask>>1,next,to,path);
    				  if(gotthere) { path.push(from); return(true); }
    				}
    		}
    		from.sweep_counter = 0;
    	}
    	return(false);
     }
    // when there is a picked object from the board, get a table
    // of the places it could land.
    public Hashtable<CookieCell,CookieMovespec> movingObjectDests()
    {	
    	Hashtable<CookieCell,CookieMovespec> h = new Hashtable<CookieCell,CookieMovespec>();
  
    	switch(board_state)
    	{

    	case CONFIRM_STATE:
    		break;
    	case PLACE_COOKIE_STATE:
    	case PLACE_OR_SWAP_STATE:
    		{
    		// a bug showed up that turned out to be because clone() didn't copy starting moves
    		// a consequence of useDirectDrawing();
    		getStartingMoves();
    		Hashtable<CookieCell,CookieMovespec> hv = new Hashtable<CookieCell,CookieMovespec>();
    		for(Enumeration<CookieCell> keys = startingMoves.keys(); keys.hasMoreElements();)
    		{	CookieCell c = keys.nextElement();
    			if(c.topChip()==null) { hv.put(c,startingMoves.get(c)); }
    		}
    		return(hv);
    	}
       	case PLAY_STATE:
    		if(pickedObject!=null) { h = getDests(getSource(),pickedObject); }
			break;
		case PUZZLE_STATE:
    		break;
		default:
			break;
    	}
    	return(h);
    }
	// factory method to generate a board cell
	public CookieCell newcell(char c,int r)
	{	return(new CookieCell(c,r,CookieId.BoardLocation));
	}
    public CookieBoard(String init,int map[]) // default constructor
    {
        Random r = new Random(72094);
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = CookieGRIDSTYLE;
        isTorus=true;
        setColorMap(map);
        startingMoves = new Hashtable<CookieCell,CookieMovespec>();
        for(int i=0;i<CookieChip.nChips+3;i++)
        {	chipPool[i]=new CookieCell(r,CookieId.ChipPool,CHIP_OFFSET+i);
        	chipPool[i].addChip(CookieChip.getChip(i));
        }

        reInitBoard(ZfirstInCol19, ZnInCol19, null); //this sets up a hexagonal board
        
		for(int lim = crawlCellDestProxy.length-1; lim>=0; lim--)
			{	crawlCellDestProxy[lim] = new CookieCell(r,CookieId.CrawlCell,lim);
				crawlCellDestProxy[lim].addChip(CookieChip.Crawl);
			}
        crawlCellSrcProxy = new CookieCell(r,CookieId.CrawlCell,crawlCellDestProxy.length);
        allCells.setDigestChain(r);
        doInit(init,randomKey,startingPattern); // do the initialization 
    }
    public CookieBoard cloneBoard() 
	{ CookieBoard dup = new CookieBoard(gametype,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((CookieBoard)b); }

    public void sameboard(BoardProtocol f) { sameboard((CookieBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(CookieBoard from_b)
    {
        super.sameboard(from_b); // compares the boards using sameCell
        G.Assert(sameCells(cookieLocation,from_b.cookieLocation),"cookieLocaion mismatch");
        // here, check any other state of the board to see if
        G.Assert(startingPattern==from_b.startingPattern,"starting pattern mismatch");
        G.Assert((stackIndex == from_b.stackIndex),"stackIndex matches");
        G.Assert(sameCells(crawlCell,from_b.crawlCell),"crawl Cell matches");
        G.Assert(sameCells(cherryCell,from_b.cherryCell),"cherry cell matches");
        G.Assert(cherryChip==from_b.cherryChip,"cherryChip matches");
        G.Assert(Digest()==from_b.Digest(),"Digest matches");
        
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
    	long v = super.Digest(r);
 
        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        

		v ^= randomKey;
		v ^= r.nextLong()*(startingPattern+1);
		v ^= CookieCell.Digest(r,cookieLocation[0]);
		v ^= CookieCell.Digest(r,cookieLocation[1]);
		v ^= (pickedObject==null)? 0 : pickedObject.Digest();
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
		v ^= CookieCell.Digest(r, playerChip[0]);
		v ^= CookieCell.Digest(r,playerChip[1]);
		v ^= CookieChip.Digest(r,cherryChip);
		v ^= CookieCell.Digest(r,crawlCell);
        return (v);
    }
    
    // C may be a cell from a clone of the board.  get the corresponding
    // cell on this board.
    public CookieCell getCell(CookieCell c)
    {	return(c==null?null:getCell(c.rackLocation(),c.col,c.row));
    }
    public CookieCell getCell(CookieId source,char col,int row)
    {
    	switch(source)
    	{
    	default: throw G.Error("Not expecting source %s",source);
    	case BoardLocation: return(getCell(col,row));
    	case ChipPool: return(chipPool[row-CHIP_OFFSET]);
    	}
    }
    // this visitor method implements copying the contents of a cell on the board
    public void copyFrom(CookieCell cc,CookieCell fc)
    {	super.copyFrom(cc,fc);
    	cc.nextPlaced = getCell(fc.nextPlaced);
    }
    
    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(CookieBoard from_b)
    {	super.copyFrom(from_b);
    	robotDepth = from_b.robotDepth;
        startingPattern = from_b.startingPattern;
        unwind_state = from_b.unwind_state;
        stackIndex = from_b.stackIndex;
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        lastPicked = null;
        cherryChip = from_b.cherryChip;
        nextCherryChip = from_b.cherryChip;
        nextCherryCell = getCell(from_b.nextCherryCell);
        cherryCell = getCell(from_b.cherryCell);
        crawlCell = getCell(from_b.crawlCell);
        placedChips = getCell(from_b.placedChips);
        stackIndex = from_b.stackIndex;
        getCell(playerChip,from_b.playerChip);
        getCell(cookieLocation,from_b.cookieLocation);
        // need so animations word with copy boards
        crawlCellSrcProxy.copyFrom(from_b.crawlCellSrcProxy);
        copyFrom(crawlCellDestProxy,from_b.crawlCellDestProxy);
        pickedObject = from_b.pickedObject;
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        sameboard(from_b); 
    }


    public void doInit() { doInit(gametype,randomKey,startingPattern); }
    public void doInit(String gtype,long randomKey) 
    { 
      doInit(gtype,randomKey,startingPattern);
    } 
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long randomv,int pat)
    {	randomKey = randomv;
    	if(CookieInit.equalsIgnoreCase(gtype)) {  crawlOption = false; }
    	else if(CookieCrawlInit.equalsIgnoreCase(gtype)) { crawlOption = true; }
    	else { throw G.Error(WrongInitError,gtype); }
        gametype = gtype;
        sweep_counter = 0;
        swapped = false;
        robotDepth = 0;
        currentSources = null;
        setState(CookieState.PUZZLE_STATE);
        unwind_state = board_state;
        whoseTurn = FIRST_PLAYER_INDEX;
        AR.setValue(droppedDestStack, null);
        stackIndex = 0;
        placedChips = null;
        lastDropped = null;
        cherryChip = null;
        crawlCell = null;
        nextCherryChip = null;
        nextCherryCell = null;
        cherryCell = null;
        AR.setValue(cookieLocation, null);
        // set the initial contents of the board to all empty cells
		for(CookieCell c = allCells; c!=null; c=c.next)
			{ c.reInit(); 
			}
		int map[]=getColorMap();
		playerChip[map[0]] = chipPool[CookieChip.nChips];
		playerChip[map[1]] = chipPool[CookieChip.nChips+1];
        moveNumber = 1;
        startingPattern = pat;
        placePattern(startPatterns[pat],getCell('J',9));
        // Everything is a constant for the duration of the game, the value of all chips in play
        Everything = groupValue(placedChips,null,null,null);
        FullSize = groupSize(placedChips,null,null)+2;
        getStartingMoves();
        // note that firstPlayer is NOT initialized here
    }
    
    // swap ownership of the orange and blue chips
    private void doSwap()
    {	CookieCell c = playerChip[0];
    	playerChip[0] = playerChip[1];
    	playerChip[1] = c;
    	CookieCell d = cookieLocation[0];
    	cookieLocation[0]=cookieLocation[1];
    	cookieLocation[1] = d;
    	swapped = !swapped;
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
        case PUZZLE_STATE:
            break;
        case GAMEOVER_STATE:
        	// let it go so damaged games can be replayed
        	if(replay==replayMode.Live) {  throw G.Error("Move not complete, can't change the current player"); }
			//$FALL-THROUGH$
		case CONFIRM_STATE:
        case CONFIRM_SWAP_STATE:
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
        case CONFIRM_SWAP_STATE:
        case DRAW_STATE:
        case CONFIRM_STATE:
            return (true);

        default:
            return (false);
        }
    }
    // this is the default, so we don't need it explicitly here.
    // but games with complex "rearrange" states might want to be
    // more selecteive.  This determines if the current board digest is added
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
    private boolean illegalSplitMove(int player,CookieCell from,CookieChip chip,CookieCell to)
    {	CookieCell startpos = cookieLocation[player];
    	CookieCell otherpos = cookieLocation[nextPlayer[player]];
    	int total = Everything - ((crawlCell==null) ? 0 : (crawlCell==from)?chip.value:crawlCell.topChip().value);
    	if((startpos==null) && (chip==playerChip[player].topChip())) { startpos = from; }
    	if((startpos!=null) && (otherpos!=null))
    	{
    	// seed the group from somewhere that's not moving.
    	CookieCell emptyseed = ((startpos==null)||(from==startpos)) ? cookieLocation[nextPlayer[player]] : startpos;
    	int newsize = groupSize(emptyseed,from,null);	// size with the cookie in the air
    	if((newsize+1)==FullSize) { return(false); } 	// not a disconnection
 
    	// it's a disconnection, it has to be a win
    	CookieCell filledSeed = ((startpos==null)||(startpos==from))?to:startpos;
    	
     	int val = groupValue(filledSeed,from,to,chip);
    	if(filledSeed.sweep_counter==otherpos.sweep_counter) 
			{ //G.print("Same");
			  return(true); 	// both player cookies are on the same side
			}
    	boolean isok = val*2<=total;
    	return(isok);
    	}
    	return(false);
     }

    public boolean WinForPlayerNow(int player)
    {	if(win[player]) 
    	{ return(true); }

    	if(cookieLocation[0]==null) { return(false); }
    	if(cookieLocation[1]==null) { return(false); }
    	// return true if our group is more valuable than the other guy's group
    	int val = groupValue(cookieLocation[player],null,null,null);
    	int oval = groupValue(cookieLocation[nextPlayer[player]],null,null,null);
    	if(val>oval) { return(true); }
    	if(val<oval) { return(false); }
    	return(!hasLegalMoves(nextPlayer[player]));
       }


    private void removePlacedChip(CookieCell cell)
    {	CookieCell prev = null;
    	CookieCell curr = placedChips;
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
    	throw G.Error("placed chip %s not found",cell);
    }
    private void addPlacedChip(CookieCell cell)
    {  	G.Assert(cell.nextPlaced==null,"Already placed");
    	//G.print("Add "+cell);
        cell.nextPlaced = placedChips;
    	placedChips = cell;
    }

    // set the contents of a cell, and maintian the books
    private CookieChip SetBoard(CookieCell c,CookieChip ch)
    {	CookieChip old = c.topChip();
    	if(old!=ch)
    	{
    	if(c.onBoard)
    	{
    	if(old!=null) {  if(ch==null) { removePlacedChip(c); }}
     	if(ch!=null) {  if(old==null) { addPlacedChip(c); }}
    	}
       	if(ch!=null) 
       		{ c.addChip(ch);
       		  if(ch==playerChip[0].topChip()) { cookieLocation[0] = c; }
       		  else if (ch==playerChip[1].topChip()) { cookieLocation[1] = c; }

       		} else 
       		{ CookieChip oldch = c.removeTop(); 
     		  if(oldch==playerChip[0].topChip()) { cookieLocation[0] = null; }
       		  else if (oldch==playerChip[1].topChip()) { cookieLocation[1] = null; }
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
    	pickedObject = null;
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
    		droppedDestStack[stackIndex] = null;
     	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	if((pickedObject!=null) && (pickedSourceStack[stackIndex]!=null))
    	{	if(pickedObject==CookieChip.Crawl) {}
    			else { SetBoard(pickedSourceStack[stackIndex],pickedObject);}
    	    pickedSourceStack[stackIndex] = null;
    	}
	  pickedObject=null;
    }
    // 
    // drop the floating object on the board.
    //
    private void dropBoardCell(CookieCell c,replayMode replay)
    {   CookieCell source = pickedSourceStack[stackIndex];
    	nextCherryCell = c;
    	if(c.isEmpty())
    	{
    	// dropping a regular cookie, possibly with crawl on board
    	SetBoard(c,pickedObject);
        lastDropped = pickedObject;
        droppedDestStack[stackIndex++] = c;
        nextCherryChip = pickedObject;
        pickedObject = null;
    	}
    	else if(source==crawlCell)
    	{	// dropping the crawl cookie on a new cell
    		if(pickedObject!=CookieChip.Crawl)
    			{ // put the picked source back
    			  SetBoard(source,pickedObject); 
    			}
    		lastDropped = pickedObject;
            droppedDestStack[stackIndex++] = c;
            nextCherryChip = CookieChip.Crawl;
            if((pickedObject!=CookieChip.Crawl) && (replay==replayMode.Live))
            {	// animate the pushback
            	//animationStack.push(c);
            	//animationStack.push(source);
            }
            pickedObject = null;
   		
     	}
    	else { throw G.Error("Not expecting dest %s",c); }
    	if(source==crawlCell) { crawlCell = c; }
    }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(CookieCell c)
    {	return((stackIndex>0) && (droppedDestStack[stackIndex-1]==c));
    }
    public boolean isDest(char col,int ro)
    {	return(isDest(getCell(col,ro)));
    }
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    { 	CookieChip ch = pickedObject;
    	if(ch!=null)
    	{
    	CookieCell src = pickedSourceStack[stackIndex];
    	if((src!=null) && (droppedDestStack[stackIndex]==null))
    	{	int crawl = ((src==crawlCell)&&(ch!=CookieChip.Crawl))
    						? CookieChip.Crawl.chipNumber()*100 
    						: 0; 
    		return(crawl+ch.chipNumber()); 
    	}}
      	return (NothingMoving);
    }

	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickBoardCell(CookieCell c)
    {

       	boolean wasDest = isDest(c);
       	CookieCell src = wasDest ? pickedSourceStack[stackIndex-1] : null;
       	if( wasDest && (c==crawlCell)) 
       		{ // was a crawl move
       		  crawlCell = src;
     		  if(src.topChip()!=null)
       		  {	// was a crawl move
     			if(lastDropped ==CookieChip.Crawl)
     			{
       			pickedObject = CookieChip.Crawl;
     			}
     			else 
     			{ pickedObject = src.topChip();
     			  SetBoard(src,null);
    			}
       			crawlCell = src;
       			stackIndex--;
       			droppedDestStack[stackIndex] = null; 
       		  }
       		  else
       		  {
       			  unDropObject();
       		  }
   		}
       	else
       	{
       	unDropObject(); 
        if(!wasDest)
        	{pickedSourceStack[stackIndex] = c;
        	lastPicked = pickedObject = c.topChip();
        	droppedDestStack[stackIndex] = null;
        	SetBoard(c,null);
        	}
       	}
  
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(CookieCell c)
    {	return(c==pickedSourceStack[stackIndex]);
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(replayMode replay)
    {
        switch (board_state)
        {
        case GAMEOVER_STATE:
        	// let it go so damaged games can be replayed
        	if(replay!=replayMode.Live) {  	break; }
			//$FALL-THROUGH$
		default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        
        case CONFIRM_STATE:
        case PLAY_STATE:
        case PLACE_OR_SWAP_STATE:
        case PLACE_COOKIE_STATE:
        	setState(CookieState.CONFIRM_STATE);
        	break;

        case PUZZLE_STATE:
			acceptPlacement();
            break;
        }
    }
    private void setNextStateAfterDone(replayMode replay)
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
        case GAMEOVER_STATE:
        	// let it go so damaged games can be replayed
        	if(replay!=replayMode.Live) {  	break; }
			//$FALL-THROUGH$
		case CONFIRM_SWAP_STATE:
    		
    		setState(CookieState.PLACE_COOKIE_STATE);
    		swapped = false;
    		break;
        case DRAW_STATE:
        	setState(CookieState.GAMEOVER_STATE);
        	break;
    	case CONFIRM_STATE:
        case PLACE_OR_SWAP_STATE:
        case PLACE_COOKIE_STATE:
       	case PLAY_STATE:
       	case PUZZLE_STATE:
        	boolean winForHim = WinForPlayerNow(nextPlayer[whoseTurn]);
        	if(winForHim) 
        		{ win[nextPlayer[whoseTurn]] = true;
        		  setState(CookieState.GAMEOVER_STATE);
        		}
        	else  
        		{
        		setState((cookieLocation[whoseTurn]==null)
    					?((cookieLocation[nextPlayer[whoseTurn]]!=null)?CookieState.PLACE_OR_SWAP_STATE:CookieState.PLACE_COOKIE_STATE)
    					:CookieState.PLAY_STATE);
        		}
    		break;
    	}
       	unwind_state = board_state;
    }

    private void doDone(replayMode replay)
    {	cherryChip = nextCherryChip;
    	cherryCell = nextCherryCell;

        acceptPlacement();

        if (board_state==CookieState.RESIGN_STATE)
        {
            win[nextPlayer[whoseTurn]] = true;
		  	setState(CookieState.GAMEOVER_STATE);
    		unwind_state = board_state;
        }
        else
        {	
      		if(crawlOption && (crawlCell==null)) 
      			{ crawlCell = cookieLocation[whoseTurn];
      			  if(replay!=replayMode.Replay)
      			  {	CookieCell d = crawlCellDestProxy[0];
       				animationStack.push(crawlCellSrcProxy);
      				animationStack.push(d);
					d.copyCurrentCenter(crawlCell);
       			  }
      			}
      		
       		setNextPlayer(replay);
      		setNextStateAfterDone(replay);
        }
        
    }
    public void SetDisplayRectangle(Rectangle r)
    {
    	super.SetDisplayRectangle(r);
    	crawlCellSrcProxy.setCurrentCenter((int)(G.Right(r)-displayParameters.CELLSIZE),
    						(int)(G.Top(r)+displayParameters.CELLSIZE));
    }
    public boolean activeCrawlAnimations()
    {	if(crawlCell!=null)
    	{
    	for(int lim=crawlCellDestProxy.length-1; lim>=0; lim--)
    		{	if(crawlCellDestProxy[lim].activeAnimationHeight()>0) { return(true); }
    		}
    	return(crawlCell.activeAnimationHeight()>0);
    	}
    	return(false);
    }
    public void setNextStateAfterPick(replayMode replay)
    {
    	switch(board_state)
    	{
    	case PLACE_OR_SWAP_STATE:
    	case PLAY_STATE:
    	case PUZZLE_STATE:	
    		break;
    	case CONFIRM_STATE: 
    	case DRAW_STATE:
    		setState(unwind_state);
    		break;
    	case GAMEOVER_STATE:
    		// let it go so damaged games can be replayed
    		if(replay!=replayMode.Live) { break; }
			//$FALL-THROUGH$
		default: throw G.Error("Not expecting state %s",board_state);
    	}
	
    }
    private void addAnimation(CookieCell c,CookieCell d)
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
    
    private void addCrawlAnimation(CookieCell c,CookieCell d)
    {	
    	CellStack path = new CellStack();
    	int mask = c.slitherDistanceMask(c);
		//boolean cw = 
    	sweep_counter++;
    	G.Assert(crawlPath(mask,c,d,path),"found a path");
    	CookieCell prev = path.pop();
    	int idx = 0;
    	while(path.size()>0)
    	{
    		CookieCell nextCell = path.pop();
    		CookieCell nextProxy = crawlCellDestProxy[idx++];
  			nextProxy.copyCurrentCenter(nextCell);
    		animationStack.push(prev);
    		animationStack.push(nextProxy);
    		prev = nextProxy;
    	}
     }
    
    public boolean Execute(commonMove mm,replayMode replay)
    {	CookieMovespec m = (CookieMovespec)mm;

       //G.print("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
       case MOVE_DONE:

         	doDone(replay);

            break;

       	case MOVE_RACK_BOARD:
       		CookieCell from = getCell(m.source,m.from_col,m.from_row);
       		CookieCell to = getCell(m.dest,m.to_col,m.to_row);
       		if(replay!=replayMode.Replay)
       		{
       			addAnimation(from,to);
       		}
       		pickedObject = from.topChip();
       		m.target = pickedObject;
		    pickedSourceStack[stackIndex] = from;
		    dropBoardCell(to,replay);
       		setNextStateAfterDrop(replay);
       		break;
       	case CRAWL_FROM_TO:
	   		{
	   		CookieCell c = getCell(m.from_col, m.from_row);
	    	CookieCell d = getCell(m.to_col,m.to_row);
	    	G.Assert(c==crawlCell,"starting from the crawl cell");
	   		if(replay!=replayMode.Replay)
	   		{	addCrawlAnimation(c,d);
	   		}
	   		pickedSourceStack[stackIndex] = c;
	   		m.target = c.topChip();
	   		droppedDestStack[stackIndex++] = d;	// source and dest must be available for dodone()
	        setNextStateAfterDrop(replay);
	    	crawlCell = d;
	    	nextCherryChip = CookieChip.Crawl;
	    	nextCherryCell = d;
	   		}
	   		break;
       	case MOVE_FROM_TO:
       		{
       		CookieCell c = getCell(m.from_col, m.from_row);
        	CookieCell d = getCell(m.to_col,m.to_row);
       		if(replay!=replayMode.Replay)
       		{	addAnimation(c,d);
       		}
          	pickBoardCell(c);
          	m.target = pickedObject;
            dropBoardCell(d,replay);
            setNextStateAfterDrop(replay);
       		}
       		break;
        case MOVE_DROPB:
        	{
			CookieCell c = getCell(m.to_col,m.to_row);
        	if(isSource(c))
    		{
    		unPickObject();
    		}
        	else
			{
			if(replay==replayMode.Single)
			{	CookieCell src = getSource();
				if(src!=null) { addAnimation(src,c); } 
			}
            dropBoardCell(c,replay);
            setNextStateAfterDrop(replay);
			}
        	}
            break;

        case MOVE_PICK:
 			{
 			if(pickedObject!=null) { unPickObject();  }
			CookieCell src = getCell(m.source,m.from_col,m.from_row);
			CookieChip top = src.topChip();
			if((top==CookieChip.Crawl)&&isDest(crawlCell))
			{
				pickBoardCell(crawlCell);
				setNextStateAfterPick(replay);
			}
			else
			{
			lastPicked = pickedObject = top;
			// the way we pick up the crawl cookie only is "pick crawl" but attribute the
			// crawlcell as the source.
		    pickedSourceStack[stackIndex] = (pickedObject==CookieChip.Crawl) ? crawlCell : src;
			}}
 			m.target = pickedObject;
            break;

            // fall through
        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	{
        	CookieCell c = getCell(m.from_col, m.from_row);
        	pickBoardCell(c);
        	m.target = pickedObject;
        	setNextStateAfterPick(replay);
        	}
            break;

        case MOVE_DROP: // drop on chip pool;
           	pickedObject = null;
           	pickedSourceStack[stackIndex]=null;
           break;


 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            setState(CookieState.PUZZLE_STATE);
            setNextStateAfterDone(replay); 
            if((board_state==CookieState.PLACE_COOKIE_STATE)||(board_state==CookieState.PLACE_OR_SWAP_STATE))
            {
            	getStartingMoves();
            }
            break;
       case MOVE_SWAP:
    	   doSwap();
    	   setState(swapped?CookieState.CONFIRM_SWAP_STATE:CookieState.PLACE_OR_SWAP_STATE);
    	   break;
       case MOVE_RESIGN:
            setState(unresign==null?CookieState.RESIGN_STATE:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(CookieState.PUZZLE_STATE);
            unwind_state = board_state;
            break;
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(CookieState.GAMEOVER_STATE);
			break;
        default:
        	cantExecute(m);
        }
        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips(CookieCell c)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case CONFIRM_STATE:
        case DRAW_STATE:
        case CONFIRM_SWAP_STATE:
        	if(pickedObject==null) { return(false); }
			//$FALL-THROUGH$
		case PLAY_STATE:
        case RESIGN_STATE:
        	// you can pick up a stone in the storage area
        	// but it's really optional
        	return(false);
        case PLACE_COOKIE_STATE:
        case PLACE_OR_SWAP_STATE:
        	return(c==playerChip[whoseTurn]);
		case GAMEOVER_STATE:
			return(false);
        case PUZZLE_STATE:
            return (true);
        }
    }

    public boolean LegalToHitBoard(CookieCell c)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
        case PLACE_COOKIE_STATE:
        case PLACE_OR_SWAP_STATE:
        	return((c.topChip()==null)&&(startingMoves.get(c)!=null));
		case CONFIRM_STATE:
		case DRAW_STATE:
		case CONFIRM_SWAP_STATE:
        	{
        	if(isDest(c)) { return(pickedObject==null); }	// pick up same chip
        	return(false); 
        	}
 		case PLAY_STATE:
			if(pickedObject!=null)
				{
				Hashtable<CookieCell,CookieMovespec>dests = getDests(getSource(),pickedObject);
				if(dests.get(c)!=null) { return(true); }
				if(isSource(c)) { return(true); }
				}
				else	// not moving
				{  Hashtable<CookieCell,CookieCell> sources = getSources();
				   return(sources.get(c)!=null);
				}
			return(false);
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			break;
        default:
        	throw G.Error("Not expecting state %s", board_state);

        case PUZZLE_STATE:
        	return ((pickedObject!=null) == (c.topChip()==null));
        }
        return(false);
    }
    
   StateStack robotStack = new StateStack();
   IStack undoStack = new IStack();
   
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(CookieMovespec m)
    {	robotStack.push(board_state);
        undoStack.push(stackIndex*100+(swapped?10000:0)); //record the starting state. The most reliable
        robotChipStack.push(cherryChip);
        robotCellStack.push(cherryCell);
        stackIndex = 0;
        robotDepth++;
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
    public void UnExecute(CookieMovespec m)
    {
       // G.print("U "+m+" for "+whoseTurn);
       // G.Assert(cookieLocation[0]!=null && cookieLocation[1]!=null,"missing cookie 3");
    	robotDepth--;
        switch (m.op)
        {
        default:
   	    	cantUnExecute(m);
        	break;
   	    case MOVE_SWAP:
     	   doSwap();
     	   break;
        case MOVE_DONE:
            break;
            
        case MOVE_DROPB:
        	{
        	CookieCell c = getCell(m.to_col,m.to_row);
       		SetBoard(c,null);
       		droppedDestStack[0]=pickedSourceStack[0]=null;
       		stackIndex = 0;
        	}
        	break;
       	case MOVE_RACK_BOARD:
       		{
       		CookieCell from = getCell(m.source,m.from_col,m.from_row);
       		CookieCell to = getCell(m.dest,m.to_col,m.to_row);
       		pickedObject = from.topChip();
		    pickedSourceStack[stackIndex] = from;
		    pickBoardCell(to);
		    pickedObject = null;
		    if(to==crawlCell) { crawlCell = null; }
		    stackIndex = 0;
       		}
       		break;
       	case CRAWL_FROM_TO:
       		{
           		CookieCell c = getCell(m.from_col, m.from_row);
            	crawlCell = c;
       		}
       		break;
       	case MOVE_FROM_TO:
       		{
       		CookieCell c = getCell(m.from_col, m.from_row);
        	CookieCell d = getCell(m.to_col,m.to_row);
           	pickBoardCell(d);
            dropBoardCell(c,replayMode.Replay);
            stackIndex = 0;
       		}
       		break;
        case MOVE_RESIGN:

            break;
        }
        int undoInfo = undoStack.pop();
        swapped = (undoInfo/10000)!=0;
        setState(robotStack.pop());
        cherryChip = robotChipStack.pop();
        cherryCell = robotCellStack.pop();
        stackIndex = (undoInfo%10000)/100;
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
       // G.Assert(cookieLocation[0]!=null && cookieLocation[1]!=null,"missing cookie 4");

 }
    //
    // get the crawl cookie moves.  "c" is initially the crawl cell, which may look empty 
    //
    private int crawl(int mask,CookieCell c,CookieCell []tempDests,int index)
    {	if(c==null || (c.sweep_counter == sweep_counter)) {}
    	else {
    		c.sweep_counter = sweep_counter;
    			// if can stop here, add to dests
    		if((mask&1)!=0) { if(!G.arrayContains(tempDests,c,index)) { tempDests[index++] = c; }}
    		if(mask>1)
    			{
    			for(int direction = 0; direction<CELL_FULL_TURN; direction++)
    			{	CookieCell next = c.exitTo(direction);
    				if(next.topChip()!=null)
    				{
    				index = crawl(mask>>1,next,tempDests,index);
    				}
    			}
    		}
    		c.sweep_counter = 0;	// unmark 
    	}
   	 return(index);
    }
  
  /**
   * get moves for the crawl cookie.  
   * return true if there are moves.
   * 
   * @param all may be null if all you want is the existence of moves
   * @param c
   * @param ch
   * @param player
   */
  private boolean getCrawlMoves(CommonMoveStack all,CookieCell c,CookieChip ch,int player)
    {if(c.onBoard)
   	 {sweep_counter++;
   	  int mask = c.slitherDistanceMask(crawlCell);	
   	  int ndests = crawl(mask,c,tempDests,0);
     		// for each of the possible destinations, a move is legal only if
   	  	// the group is not split, or the game is won.
   	  	while(ndests>0)
   	  	{	ndests--;
   	  		CookieCell d = tempDests[ndests];
   	  		if(all==null) { return(true); }
   	  		all.push(new CookieMovespec(CRAWL_FROM_TO,c.col,c.row,d.col,d.row,player));
   	  	}
     	}
    	return((all==null) ? false : (all.size()>0));
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
 private boolean getMoveMoves(CommonMoveStack all,CookieCell c,CookieChip ch,int player,boolean includeCrawl)
 {	if(c.onBoard)
 	{
	 	int mask = c.slitherDistanceMask(crawlCell);
	  	int ndests = slither(mask,c,tempDests);
  		// for each of the possible destinations, a move is legal only if
	  	// the group is not split, or the game is won.
	  	CookieChip hisChip = playerChip[nextPlayer[player]].topChip();
	  	if((ch!=CookieChip.Crawl) 
	  			&& (ch!=cherryChip) 
	  			&& (ch!=hisChip) 
	  			&& ((c!=crawlCell)||(cherryChip!=CookieChip.Crawl)))
	  	{
	  	while(ndests>0)
	  	{	ndests--;
	  		CookieCell d = tempDests[ndests];
	  		if(!illegalSplitMove(player,c,ch,d))
	  		{
	  		if(all==null)  { return(true); }
	  		all.push(new CookieMovespec(MOVE_FROM_TO,c.col,c.row,d.col,d.row,player));
	  		}
	  	}}
	  	if(includeCrawl && (c==crawlCell)&&(cherryCell!=c)) 
	  		{ boolean cr = getCrawlMoves(all,c,CookieChip.Crawl,player); 
	  		  if(cr && (all==null)) { return(true); }
	  		}
  	}
 	else if(includeCrawl && (ch==CookieChip.Crawl))
 	{ boolean cr = getCrawlMoves(all,crawlCell,CookieChip.Crawl,player); 
 	  if(cr && (all==null)) { return(true); }
 	}
 	return((all==null)? false : (all.size()>0));
 }
 private boolean getCrawlMoves(CommonMoveStack  all,int player)
 {	if(crawlCell!=null && (cherryCell!=crawlCell))
	 	{return(getCrawlMoves(all,crawlCell,CookieChip.Crawl,player));
	 	}
 	return(false);
 }
private  boolean getMoveMoves(CommonMoveStack  all,int player,boolean includeCrawl)
 {	

 	for(CookieCell c = placedChips;
        c!=null;
        c=c.nextPlaced)
 	{	
 		boolean some = getMoveMoves(all,c,c.topChip(),player,includeCrawl);
 		if(some && (all==null)) { return(true); }
  	}
 	return((all==null) ? false : (all.size()>0));
 }
 
 
 private void getPlacementMoves(CommonMoveStack all, int player)
 {		// clear the sweeps
	 	for(CookieCell c = placedChips; c!=null; c=c.nextPlaced)
 		{
	 	for(int dir = 0; dir < CELL_FULL_TURN; dir++)
	 		{
	 		CookieCell d = c.exitTo(dir);
	 		if((d!=null) && (d.topChip()==null)) { d.sweep_counter = 0; }
	 		}}
	 	// count the adjacent cells
 		for(CookieCell c = placedChips; c!=null; c=c.nextPlaced)
 		{
 		for(int dir = 0; dir < CELL_FULL_TURN; dir++)
 		{
 		CookieCell d = c.exitTo(dir);
 		if((d!=null) && (d.topChip()==null)) { d.sweep_counter++; }
 		}
 		}
 		
	 	// for add those with exactly 2 adjacent
 		CookieChip chip = playerChip[player].topChip();
 		int index = chip.index+CHIP_OFFSET;
 		for(CookieCell c = placedChips; c!=null; c=c.nextPlaced)
 		{
 		for(int dir = 0; dir < CELL_FULL_TURN; dir++)
 		{
 		CookieCell d = c.exitTo(dir);
 		if((d!=null) && (d.topChip()==null) && (d.sweep_counter==2))
 			{
 			CookieMovespec m = new CookieMovespec(MOVE_RACK_BOARD,index,d.col,d.row,player);
  			all.push(m);
 			}
 		}
 		}

 }
 
 private Hashtable<CookieCell,CookieCell> currentSources = null; 
 public Hashtable<CookieCell,CookieCell> getSources()
 {	if(currentSources!=null) { return(currentSources); }
 	Hashtable<CookieCell,CookieCell> res = new Hashtable<CookieCell,CookieCell>();
 	switch(board_state)
 	{
 	default: 
 	case PUZZLE_STATE:
 		for(CookieCell c = placedChips; c!=null; c=c.nextPlaced) 
 		{	res.put(c,c);
 		}
 		break;
 	case PLACE_OR_SWAP_STATE:
 	case PLACE_COOKIE_STATE:
 		break;
 	case PLAY_STATE:
 		{
 		CommonMoveStack all = new CommonMoveStack();
 		getMoveMoves(all,whoseTurn,true);
 	 	while(all.size()>0)
 	 	{	CookieMovespec mm = (CookieMovespec)all.pop();
 	 		if((mm.op==MOVE_FROM_TO)||(mm.op==CRAWL_FROM_TO))
	 			{  CookieCell from = getCell(mm.from_col,mm.from_row); 
	 				res.put(from,from);
	 			} 
	 	}
 		}
 		break;
 	}
 	currentSources = res;
 	return(res);
 }
 boolean hasLegalMoves(CookieCell c,int who,boolean includeCrawl)
 {
	 return(getMoveMoves(null,c,c.topChip(),who,includeCrawl));
 }
 boolean hasLegalMoves(int whoseTurn)
 {	 if(cookieLocation[whoseTurn]==null) { return(true); }
 	 return(getMoveMoves(null,whoseTurn,COUNT_CRAWL_FOR_FREEZE));	// maybe don't include crawl moves
 }
 public Hashtable<CookieCell,CookieMovespec> getDests(CookieCell src,CookieChip picked)
 {	
 	Hashtable<CookieCell,CookieMovespec> res = new Hashtable<CookieCell,CookieMovespec>();
 	switch(board_state)
 	{
 	default: 
 	case PLACE_OR_SWAP_STATE:
 	case PLACE_COOKIE_STATE:
 		break;
 	case PLAY_STATE:
 		{
 		CommonMoveStack all = new CommonMoveStack();
 	 	getMoveMoves(all,src,picked,whoseTurn,true);
 	 	while(all.size()>0)
 	 	{	CookieMovespec mm = (CookieMovespec)all.pop();
 	 		if((mm.op==MOVE_FROM_TO)||(mm.op==CRAWL_FROM_TO))
	 			{  CookieCell dest = getCell(mm.to_col,mm.to_row); 
	 				res.put(dest,mm);
	 			} 
	 	}
 		}
 		break;
 	}
 	return(res);
 }
 
 void getStartingMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	getPlacementMoves(all,0);
 	startingMoves.clear();
 	while(all.size()>0)
 	{	CookieMovespec m = (CookieMovespec)all.pop();
 		CookieCell dest = getCell(m.to_col,m.to_row);
 		startingMoves.put(dest,m);
 	}
 }

 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	switch(board_state)
 	{
 	default:	throw G.Error("Not implemented");
 	case PLACE_OR_SWAP_STATE:
 		// starting moves were calculated by init
 		all.addElement(new CookieMovespec(MOVE_SWAP,whoseTurn));
		//$FALL-THROUGH$
	case PLACE_COOKIE_STATE:
 		
 		for(Enumeration<CookieCell> e = startingMoves.keys(); e.hasMoreElements();)
 		{	CookieCell c = e.nextElement();
 			CookieMovespec m = startingMoves.get(c);
 			CookieChip chip = playerChip[whoseTurn].topChip();
 	 		int index = chip.index+CHIP_OFFSET;
 			if(c.topChip()==null)
 			{
 				all.push(new CookieMovespec(m.op,index,m.to_col,m.to_row,whoseTurn));
 			}
 		}
 		break;
 	case PLAY_STATE:
 		getMoveMoves(all,whoseTurn,COUNT_CRAWL_FOR_FREEZE);
 		if(all.size()==0) { all.push(new CookieMovespec(MOVE_RESIGN,whoseTurn)); }
 		else if(!COUNT_CRAWL_FOR_FREEZE){ getCrawlMoves(all,whoseTurn); }
 		break;
 	}
 	
 	return(all);
 }
 
}
