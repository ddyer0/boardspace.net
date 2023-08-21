package volo;

import java.util.Hashtable;

import lib.*;
import online.game.*;
/**
 * VoloBoard knows all about the game of Hex, which is played
 * on a hexagonal board. It gets a lot of logistic support from 
 * common.hexBoard, which knows about the coordinate system.  
 * 
 * This class doesn't do any graphics or know about anything graphical, 
 * but it does know about states of the game that should be reflected 
 * in the graphics.
 * 
 *  The principle interface with the game viewer is the "Execute" method
 *  which processes moves. 
 *  
 *  In general, the state of the game is represented by the contents of the board,
 *  whose turn it is, and an explicit state variable.  All the transitions specified
 *  by moves are mediated by the state.  In general, my philosophy is to be extremely
 *  restrictive about what to allow in each state, and have a lot of trip wires to
 *  catch unexpected transitions.   We expect to be fed only legal moves, but mistakes
 *  will be made and it's good to have the maximum opportunity to catch the unexpected.
 *  
 * Note that none of this class shows through to the game controller.  It's purely
 * a private entity used by the viewer and the robot.
 * 
 * @author ddyer
 *
 */

class VoloBoard extends hexBoard<VoloCell> implements BoardProtocol,VoloConstants
{	
	private VoloState unresign;
	private VoloState board_state;
	public VoloState getState() {return(board_state); }
	public void setState(VoloState st) 
	{ 	unresign = (st==VoloState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	// this is required even though it is meaningless for Volo, but possibly important
	// in other games. 
	public void SetDrawState() {  setState(VoloState.GAMEOVER_STATE); };	
	public boolean smallBoard = false;
	public VoloCell playerCells[] = new VoloCell[2];
	public CellStack animationStack = new CellStack();
	public VoloCell hub = null;		// the nonexistant center cell, so we can ignore it
	// this is the index from voloblob bit number to cells on the board
	private VoloCell[]cellIndex = new VoloCell[129];	// 128 is the maximum set size supported by voloblob 
    private int sweep_counter=0;			// used when scanning for blobs
    private CommonMoveStack moveStack = new CommonMoveStack();	// moves in the game
    private BlobStack zoneStack = new BlobStack();	// moves in the game
    private StateStack stateStack = new StateStack();
    private StateStack robotStateStack = new StateStack();
    private BlobStack robotZoneStack = new BlobStack();
   // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public VoloChip pickedObject = null;	// object being moved
    public VoloChip lastPicked = null;		// last object picked up
    private VoloCell blueChipPool = null;	// pool of blue cells
    private VoloCell orangeChipPool = null;	// pool of orange cells
    private VoloCell pickedSource = null; 	// the cell from which pickedObject came
    public VoloCell startOfLine = null;	// the designated start of a line of birds
    public VoloCell endOfLine = null;		// the designated end of a line of birds
    private VoloCell droppedDest = null;	// the destination cell
    public VoloChip lastDroppedObject = null;	// for image adjustment logic
    private CellStack playerChips[];
    VoloId playerColor[]={VoloId.Orange_Chip_Pool,VoloId.Blue_Chip_Pool};
    VoloChip playerChip[]={VoloChip.Orange,VoloChip.Blue};
 
	public int chipsOnBoard()
	{	int n = 0;
		for(VoloCell c = allCells; c!=null; c=c.next) 
			{ if(c.chip!=null) { n++; }}
		return(n); 
	}
	// factory method to generate a board cell
	public VoloCell newcell(char c,int r)
	{	return(new VoloCell(VoloId.BoardLocation,c,r));
	}
	// get the cell on this board corresponding to a cell that may be on another board
	public VoloCell getCell(VoloCell c)
	{	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
	}

	public VoloBoard(String init,int map[]) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_LINES; // Yinsh style lines
        Grid_Style = VOLOGRIDSTYLE;
        playerChips = new CellStack[2];
        playerChips[0] = new CellStack();
        playerChips[1] = new CellStack();
        setColorMap(map, 2);
        doInit(init); // do the initialization 
    }
    public VoloBoard cloneBoard() 
	{ VoloBoard dup = new VoloBoard(gametype,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((VoloBoard)b); }

    public void sameboard(BoardProtocol f) { sameboard((VoloBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(VoloBoard from_b)
    {
        super.sameboard(from_b); // hexboard compares the boards
        sameCells(playerChips,from_b.playerChips);
        G.Assert(pickedObject==from_b.pickedObject, "picked Object matches");
        if(board_state==VoloState.PUZZLE_STATE) { G.Assert(sameCells(pickedSource,from_b.pickedSource),"pickedsource matches"); }
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");

    }

    /** 
     * Digest produces a 64 bit hash of the game state.  
     */
    public long Digest()
    {
        Random r = new Random(64 * 1000); // init the random number generator
    	long v = super.Digest(r);
 
        // the basic digestion technique is to xor a bunch of random numbers. 
    	// many object have an associated unique random number, including "chip" and "cell"
    	// derivatives.  If the same object is digested more than once (ie; once as a chip
    	// in play, and once as the chip currently "picked up", then it must be given a
    	// different identity for the second use.
        //
        
		// many games will want to digest pickedSource too
		if(board_state==VoloState.PUZZLE_STATE) { v ^= cell.Digest(r,pickedSource); }
		v ^= chip.Digest(r,pickedObject);
		//v ^= Digest(playerChips);
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
        return (v);
    }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(VoloBoard from_b)
    {	super.copyFrom(from_b);
    	endOfLine = getCell(from_b.endOfLine);
        startOfLine = getCell(from_b.startOfLine);
        moveStack.copyFrom(from_b.moveStack);
        zoneStack.copyFrom(from_b.zoneStack);
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        if(board_state==VoloState.PUZZLE_STATE) { pickedSource = getCell(from_b.pickedSource); }
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        getCell(playerChips,from_b.playerChips);
        copyFrom(playerCells,from_b.playerCells);
        sameboard(from_b); 
    }
    public void constructBoard(boolean small)
    {
        if(small)
        	{
        	initBoard(SfirstInCol, SnInCol, SfirstCol);
        	hub = getCell('F',6);
        	}
        	else
        	{initBoard(ZfirstInCol, ZnInCol, ZfirstCol); //this sets up the volo board
        	hub = getCell('G',7);
        	}
        hub.unCrossLink();
        
       	Random r = new Random(734687);
        blueChipPool = new VoloCell(r,VoloId.Blue_Chip_Pool);
        blueChipPool.chip = VoloChip.Blue;
        orangeChipPool = new VoloCell(r,VoloId.Orange_Chip_Pool);
        orangeChipPool.chip = VoloChip.Orange;
        allCells.setDigestChain(r);		// set the randomv for all cells on the board
        {int n=1;
          for(VoloCell c = allCells; c!=null; c=c.next) 
	         { if(c.next==hub) { c.next = c.next.next; }
	           c.cellNumber = n;
	           cellIndex[n] = c;
	           VoloBlob.nameString[n] = ""+c.col+c.row;
	           n++;
	         };
         }
    	
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {	randomKey = key;
    	gametype = gtype;
      	if(Volo_Init.equalsIgnoreCase(gametype)||Volo_84.equalsIgnoreCase(gametype)) { }
    	else { throw G.Error(WrongInitError,gtype); }
        smallBoard = gametype.equalsIgnoreCase(Volo_84);
        
        constructBoard(smallBoard);
        
        stateStack.clear();
        zoneStack.clear();
        moveStack.clear();
        robotStateStack.clear();
        robotZoneStack.clear();
        setState(VoloState.PUZZLE_STATE);
        animationStack.clear();
        whoseTurn = FIRST_PLAYER_INDEX;
        droppedDest = null;
        pickedSource = null;
        pickedObject = null;
        lastPicked = null;
        lastDroppedObject = null;
        int map[]=getColorMap();
		playerColor[map[0]]=VoloId.Orange_Chip_Pool;
		playerColor[map[1]]=VoloId.Blue_Chip_Pool;
		playerChip[map[0]]=VoloChip.Orange;
		playerChip[map[1]]=VoloChip.Blue;
		playerCells[map[0]] = orangeChipPool;
		playerCells[map[1]] = blueChipPool;
		reInit(playerChips);
        // set the initial contents of the board to all empty cells
		for(VoloCell c = allCells; c!=null; c=c.next) { c.reInit(); }
		int [][] smallChips = {{0,2,2},{0,6,10},{0,10,2},{1,2,6},{1,6,2},{1,10,6}};
		int [][] largeChips = { {0,2,2},{0,7,12},{0,12,2},{1,2,7},{1,7,2},{1,12,7}};
     	int chips[][] = smallBoard ? smallChips : largeChips;	// starting chips color and position
      	for(int []coor : chips)
      	{	VoloCell c = getCell((char)('A'+coor[1]-1),coor[2]);
      		VoloChip chip = playerChip[coor[0]];
      		c.addChip(chip);
      		playerChips[coor[0]].push(c);
      	}
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
         case PUZZLE_STATE:
            break;
        case CONFIRM_STATE:
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
    // flood fill to make this blob as large as possible.  This is more elaborate
    // than it needs to be for scoring purposes, but it is also used by the robot
    // if halo, include the empty cells adjacent to the target cells
    private void expandVoloBlob(VoloBlob blob,VoloCell cell,boolean halo)
    {	if(cell==null) {}
    	else if((cell.sweep_counter!=sweep_counter))
    	{
    	cell.sweep_counter = sweep_counter;
    	if(halo && (cell.chip==null)) { blob.addCell(cell); }
     	
    	if(blob.inverse ? (cell.chip!=blob.color) : (cell.chip==blob.color))
    	  {	
    	   	blob.addCell(cell);
    	   	for(int dir = 0; dir<6; dir++)
    		{	expandVoloBlob(blob,cell.exitTo(dir),halo);
    		}
    	  }
    	}
    }
    // get a representative cell from the blob members
    VoloCell seedMember(VoloBlob blob)
    {	return(cellIndex[blob.lowestMemberOrdinal()]);
    }
    
    //
    // flood fill to make this blob as large as possible.  This is more elaborate
    // than it needs to be for scoring purposes, but it is also used by the robot
    //  BlobStack findBlobs(int forplayer,BlobStack all)
    public BlobStack findBlobs(int forplayer,boolean inverse,boolean halo)
    {	
    	BlobStack all = new BlobStack();
    	VoloChip pch = playerChip[forplayer];
		sweep_counter++;
    	for(VoloCell cell = allCells;  cell!=null; cell=cell.next)
    	{	if((cell.sweep_counter!=sweep_counter) && (inverse?(cell.chip!=pch) : (cell.chip==pch)))
    		{
    		VoloBlob blob = new VoloBlob(pch,forplayer,inverse);
    		expandVoloBlob(blob,cell,halo);
    		G.Assert(blob.contains(cell),"seed %s not included",cell);
    		all.push(blob);
     		}
    	}
       	return(all);
    }
    
    // find the blob containing c, or if "inverse" the complement of c, and if "halo" expanded by 1
    VoloBlob findBlob(VoloCell c, int player,boolean inverse, boolean halo)
    {	VoloBlob blob = new VoloBlob(c.chip,player,inverse);
    	sweep_counter++;
    	expandVoloBlob(blob,c,halo);
    	return(blob);
    }
    // find one of a stack of blobs that contains c
    VoloBlob findBlob(BlobStack blobs,VoloCell c)
    {	for(int lim=blobs.size()-1; lim>=0; lim--)
    	{	VoloBlob bb = blobs.elementAt(lim);
    		if(bb.contains(c)) { return(bb); }
    	}
    	throw G.Error("Cell %s not found",c);
    }
    // merge all blobs into one massive blob for the player
    public VoloBlob getBlobSet(int forplayer,boolean inverse)
    {	BlobStack blobs = findBlobs(forplayer,inverse,false);
    	VoloBlob master = blobs.elementAt(0);
    	return(getBlobSet(blobs,master));
    }
    // digest a stack of blobs to their union
    public VoloBlob getBlobSet(BlobStack blobs,VoloBlob master)
    {	
    	for(int lim = blobs.size()-1; lim>=1; lim--)
    	{	VoloBlob slave = blobs.elementAt(lim);
    		master.union(slave);
    	}
    	return(master);
    } 
    // get the set of areas of the player which contain no cells of the
    // other player.  this corresponds to the forbidden zone for the other
    // player, where he can't drop any chips
    public VoloBlob getReserveSet(int player)
    {	BlobStack playerZones = findBlobs(player,true,false);	// player controlled zones
    	return(getReserveSet(player,playerZones));
    }
    
    // get the reserve set for the player, starting with his known blob set.
    public VoloBlob getReserveSet(int player,BlobStack playerZones)
    {
    	VoloBlob emptySet = null;
    	if(playerZones.size()>1)
    	{	// player controls more than one zone, see which ones are empty of opponents
    		VoloBlob otherSet = getBlobSet(nextPlayer[player],false);
    		for(int lim=playerZones.size()-1; lim>=0; lim--)
    		{	VoloBlob zone = playerZones.elementAt(lim);
    			if(otherSet.emptyIntersection(zone)) 
    			{
    			if(emptySet==null) { emptySet = new VoloBlob(playerChip[player],player,true); }
    			emptySet.union(zone); 
    			}
    		}
    	}
    	return(emptySet);
    }
    //
    // return true if this player has just one flock
    //
    boolean hasOneFlock(int forplayer)
    {	sweep_counter++;
    	VoloChip pch = playerChip[forplayer];
    	VoloBlob first = null;
    	for(VoloCell cell = allCells;  cell!=null; cell=cell.next)
    	{	if((cell.sweep_counter!=sweep_counter) && (cell.chip==pch))
    		{
    		if(first!=null) { return(false); }
    		first = new VoloBlob(pch,forplayer,false);
    		sweep_counter++;
    		expandVoloBlob(first,cell,false);
     		}
    	}
       	return(true);
    }

    public boolean WinForPlayerNow(int player)
    {	if(win[player]) { return(true); }
    	return(hasOneFlock(player));
    }


    //
    // accept the current placements as permanant
    //
    public void acceptPlacement()
    {	
        droppedDest = null;
        pickedSource = null;
     	endOfLine = null;
    	startOfLine = null;
    	moveStack.clear();
    	stateStack.clear();
    	zoneStack.clear();
    }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    	if(droppedDest!=null) 
    	{	pickedObject = droppedDest.topChip();
    		droppedDest.chip = null; 
    		droppedDest = null;
     	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	if((pickedSource!=null)&&(pickedSource.onBoard)) 
    		{ pickedSource.chip = pickedObject;
    		  pickedSource = null;
    		}
		  pickedObject = null;
  }
    // 
    // drop the floating object.
    //
    private void dropObject(VoloCell dest)
    {
       droppedDest = dest;
       switch (dest.rackLocation())
        {
        default:
        	throw G.Error("not expecting dest %s", dest);
        case Blue_Chip_Pool:
        case Orange_Chip_Pool:		// back in the pool, we don't really care where
        	pickedObject = null;
        	pickedSource = null;
        	droppedDest = null;
            break;
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        case EmptyBoard:
           	dest.chip = pickedObject;
            lastDroppedObject = pickedObject;
            droppedDest = dest;
            ((pickedObject==playerChip[0]) ? playerChips[0] : playerChips[1]).push(dest);
            pickedObject = null;
            break;
        }
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(VoloCell c)
    {	return(droppedDest==c);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { VoloChip ch = pickedObject;
      if(ch!=null)
    	{	return(ch.chipNumber()); 
    	}
      	return (NothingMoving);
    }
    private VoloCell getCell(VoloId source,char col,int row)
    {	switch(source)
    	{
    	default:  throw G.Error("Not expecting source %s", source);
    	case BoardLocation: return(getCell(col,row));
    	case Blue_Chip_Pool: return(blueChipPool);
    	case Orange_Chip_Pool: return(orangeChipPool);
    	}
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(VoloCell source)
    {
        pickedSource = source;
        switch (source.rackLocation())
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	{
        	boolean wasDest = isDest(source);
        	unDropObject(); 
        	if(!wasDest)
        	{
             lastPicked = pickedObject = source.topChip();
             ((playerChip[0]==pickedObject) ? playerChips[0] : playerChips[1]).remove(source,false);
         	droppedDest = null;
			source.chip = null;
        	}}
            break;

        case Orange_Chip_Pool:
        case Blue_Chip_Pool:
        	lastPicked = pickedObject = source.chip;
        	break;
        }
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(VoloCell c)
    {	return(c==pickedSource);
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
        	if(droppedDest==null)
        	{setNextStateAfterDone();
        	}
        	break;
        case PLAY_STATE:
        case PLAY_OR_SLIDE_STATE:
  			setState(VoloState.CONFIRM_STATE);
			break;
        case PUZZLE_STATE:
			acceptPlacement();
            break;
        }
    }
    private void setStartState()
    {
    	VoloState newstate = hasSlideMoves() ? VoloState.PLAY_OR_SLIDE_STATE:VoloState.PLAY_STATE;
		setState(newstate);
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case GAMEOVER_STATE: break;
    	case CONFIRM_STATE:
    	case PUZZLE_STATE:
    	case PLAY_STATE:
    		acceptPlacement();
    		setStartState();
     		break;
    	}

    }
    private void doDone()
    {
        acceptPlacement();

        if (board_state==VoloState.RESIGN_STATE)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(VoloState.GAMEOVER_STATE);
        }
        else
        {	if(WinForPlayerNow(whoseTurn)) 
        		{ win[whoseTurn]=true;
        		  setState(VoloState.GAMEOVER_STATE); 
        		}
        	else {setNextPlayer();
        		if(WinForPlayerNow(whoseTurn)) { win[whoseTurn]=true; setState(VoloState.GAMEOVER_STATE); }
        		else {   		setNextStateAfterDone(); }
        	}
        }
    }
    void animateMove(replayMode replay,VoloCell from,VoloCell to,int direction,int distance)
    {
    	if(replay!=replayMode.Replay)
    	{	// request an animation
    		while(distance-- >= 0)
    		{
    		animationStack.push(from);
    		animationStack.push(to);
    		from = from.exitTo(direction);
    		to = to.exitTo(direction);
    		}
    	}
    }
    void doSlideMove(VoloMovespec m,replayMode replay)
    {	VoloCell from = getCell(m.from_col,m.from_row);
    	int direction = m.direction.getDirection();
    	int distance = m.nchips;
    	VoloCell to = getCell(m.to_col,m.to_row);
    	droppedDest = to;
    	removeLine(null,playerChip[whoseTurn],from,direction,distance);
    	placeLine(null,playerChip[whoseTurn],to,direction,distance);
    	animateMove(replay,from,to,direction,distance);
    }
    void unSlideMove(VoloMovespec m,replayMode replay)
    {	VoloCell from = getCell(m.from_col,m.from_row);
    	int direction = m.direction.getDirection();
    	int distance = m.nchips;
    	int player = m.player;
    	VoloCell to = getCell(m.to_col,m.to_row);
    	removeLine(null,playerChip[player],to,direction,distance);
    	placeLine(null,playerChip[player],from,direction,distance);   
    	droppedDest = null;
    	animateMove(replay,to,from,direction,distance);
    }
 
    // check for the case where we enclosed an area, and if so require
    // the current player to select one to clear
    void setNextStateAfterSlide()
    {	if(WinForPlayerNow(whoseTurn)) { setState(VoloState.CONFIRM_STATE); }
    	else
    	{
    	BlobStack playerZones = findBlobs(whoseTurn,true,false);	// player controlled zones
    	VoloBlob otherSet = getBlobSet(nextPlayer[whoseTurn],false);
    	int mixedZones = 0;
    	for(int lim = playerZones.size()-1; lim>=0; lim--)
    	{
    		if(!playerZones.elementAt(lim).emptyIntersection(otherSet)) { mixedZones++; }
    	}
    	setState((mixedZones > 1)?VoloState.DESIGNATE_ZONE_STATE:VoloState.CONFIRM_STATE); 
    }}
    
    // remove all the opponnet chips contained in the same zone as "center"
    // return a new blob consisting of all the cleared cells, for undo
    VoloBlob clearZoneContaining(VoloCell center,replayMode mode)
    {	int nextplayer = nextPlayer[whoseTurn];
    	VoloChip oppchip = playerChip[nextplayer];
    	VoloBlob cleared = new VoloBlob(oppchip,nextplayer,false);
    	BlobStack zones = findBlobs(whoseTurn,true,false);
    	VoloBlob mainZone = findBlob(zones,center);
    	CellStack chips =playerChips[nextplayer];
    	for(VoloCell c = allCells; c !=null; c=c.next)
    	{
    		if((c.chip==oppchip) && mainZone.contains(c)) 
    			{ c.chip = null; cleared.addCell(c); 
    			  chips.remove(c,false);
    			  if(mode!=replayMode.Replay)
    			  {
    				  animationStack.push(c);
    				  animationStack.push(playerCells[nextplayer]);
    			  }
    			}
    	}
    	return(cleared);
    }
    
    void restoreClearedZone(VoloBlob cleared,replayMode mode)
    {	int pl = cleared.playerIndex;
    	VoloCell src = playerCells[pl];
    	CellStack chips = playerChips[pl];
		while(cleared.size>0)
			{
				int low = cleared.lowestMemberOrdinal();
				VoloCell cell = cellIndex[low];
				cleared.removeOrdinal(low);
				
				cell.chip = cleared.color;
				chips.push(cell);
				if(mode!=replayMode.Replay)
				{
					animationStack.push(src);
					animationStack.push(cell);
				}
			}
	
    }
    void unwindMoves(replayMode mode)
    {	boolean done = false;
    	while(!done && (moveStack.size()>0))
    	{	VoloMovespec m = (VoloMovespec)moveStack.pop();
    		VoloState state = stateStack.pop();
    		switch(m.op)
    		{
    		default: throw G.Error("Not expecting to undo a move like %s",m);
    		case MOVE_DONE: 
    		case MOVE_EDIT:
    		case MOVE_START:
    			done = true;
    			moveStack.push(m);
    			stateStack.push(state);
    			break;
    		case MOVE_SELECT:	// designating a zone to clear
    		{
    			VoloBlob cleared = zoneStack.pop();
    			restoreClearedZone(cleared,mode);
    			setState(state);
    		}
    			break;
    		case MOVE_DROPB:
    			unDropObject();
    			break;
    		case MOVE_SLIDE:	// sliding a flock
    			unSlideMove(m,mode);
    			setState(state);

    		}
     	}
    	unDropObject();
    	unPickObject();
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	VoloMovespec m = (VoloMovespec)mm;
        //G.print("E "+m+" for "+whoseTurn+" "+board_state);
     	//verifyChips();

        switch (m.op)
        {
        case MOVE_PASS:
        	setState(VoloState.CONFIRM_STATE);
        	break;
        case MOVE_DONE:
        	moveStack.push(m);	// mark the end
        	stateStack.push(board_state);
         	doDone();

            break;

        case MOVE_DROPB:
			{
			VoloCell c = getCell(m.source,m.to_col,m.to_row);
			VoloCell src = pickedSource;
			VoloCell originalSrc = src;
			droppedDest = c;
			moveStack.push(m);
			stateStack.push(board_state);
			switch(board_state)
			{ case PUZZLE_STATE: 
					acceptPlacement();
					if(src==null)
					{
					pickedObject = lastPicked;
					if(lastPicked==VoloChip.Blue) { src = blueChipPool;  }
					else if(lastPicked==VoloChip.Orange) { src = orangeChipPool; }
					}
					break;
			  case PLAY_STATE:
			  case PLAY_OR_SLIDE_STATE:
				  lastPicked=null;
				  break;
			  case CONFIRM_STATE: 
				  	unDropObject(); 
				  	unPickObject(); 
				  	break;
			default:
				break;
			}
			pickedSource = src;
			if(pickedObject==null) { pickedObject = playerChip[whoseTurn]; }	

			if(pickedObject==VoloChip.Blue) { src = blueChipPool;  }
			else if(pickedObject==VoloChip.Orange) { src = orangeChipPool; }

            dropObject(c);
            if((src!=null) && (originalSrc==null) && (replay!=replayMode.Replay))
            {
            	animationStack.push(src);
            	animationStack.push(c);
            }
            setNextStateAfterDrop();
			}
            break;

        case MOVE_SELECT:
        	{	
        	VoloCell c = getCell(m.from_col, m.from_row);
        	switch(board_state)
        	{
        	default: throw G.Error("Not expecting state ",board_state);
        	case DESIGNATE_ZONE_STATE:
        		zoneStack.push(clearZoneContaining(c,replay));
        		moveStack.push(m);
            	stateStack.push(board_state);

        		setNextStateAfterSlide();
        		break;
        	case PLAY_OR_SLIDE_STATE:
        		startOfLine = c;
        		endOfLine = null;
        		setState(c.hasFriendlyNeighbors()?VoloState.SECOND_SLIDE_STATE:VoloState.LAND_FLOCK_STATE);
        		break;
        	case SECOND_SLIDE_STATE:
        		endOfLine = c;
        		setState(VoloState.LAND_FLOCK_STATE);
        		break;
        	}
        	}
        	break;
        case MOVE_PICK:
            unDropObject();
            unPickObject();
 			//$FALL-THROUGH$
		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	{
        	VoloCell c = getCell(m.source,m.from_col, m.from_row);
        	if(c==droppedDest)
        		{ if(moveStack.size()>0)
        			{ VoloMovespec lastMove = (VoloMovespec)moveStack.pop();
        			  VoloState state = stateStack.pop();
        			  switch(lastMove.op)
        			  {
        			  case MOVE_SLIDE: 
        				  unSlideMove(lastMove,replay);
        				  setState(state);
        			  		break;
        			  default:
        				  unDropObject();
        				  setState(state);
        				  break;
        			  }
        			}
        		}
        	else {
        		pickObject(c);
	        	
	        	switch(board_state)
	        	{
	        	case PUZZLE_STATE:
	         		break;
	        	case CONFIRM_STATE:
	        		setState(VoloState.PLAY_STATE);
	        		break;
	        	default: ;
        	}}
        	}
            break;

        case MOVE_DROP: // drop on chip pool;
        	{
        	VoloCell dest = getCell(m.source,m.to_col,m.to_row);
            dropObject(dest);
            if(replay==replayMode.Single)
            {	VoloCell src = pickedSource;
            	if(src!=null)
            	{
            	animationStack.push(src);
            	animationStack.push(dest);
            	}
            }
            //setNextStateAfterDrop();
        	}
            break;

        case MOVE_SLIDE:
        	{
        	doSlideMove(m,replay);
        	moveStack.push(m);
        	stateStack.push(board_state);
        	setNextStateAfterSlide();
        	}
        	break;
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            moveStack.push(m);
            stateStack.push(board_state);
            int nextp = nextPlayer[whoseTurn];
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(VoloState.PUZZLE_STATE);	// standardize the current state
            if((win[whoseTurn]=WinForPlayerNow(whoseTurn))
               ||(win[nextp]=WinForPlayerNow(nextp)))
               	{ setState(VoloState.GAMEOVER_STATE); 
               	}
            else {  setNextStateAfterDone(); }

            break;

       case MOVE_RESIGN:
            setState(unresign==null?VoloState.RESIGN_STATE:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(VoloState.PUZZLE_STATE);
            moveStack.push(m);
            stateStack.push(board_state);
            break;
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(VoloState.GAMEOVER_STATE);
			break;

        default:
        	cantExecute(m);
        }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+board_state);
     	//verifyChips();

        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case CONFIRM_STATE:
        	return(false);
        case PLAY_OR_SLIDE_STATE:
        case PLAY_STATE:
        	// for volo, you can pick up a stone in the storage area
        	// but it's really optional
        	return(player==whoseTurn);
        case SECOND_SLIDE_STATE:
        case LAND_FLOCK_STATE:
		case GAMEOVER_STATE:
		case DESIGNATE_ZONE_STATE:
		case RESIGN_STATE:
			return(false);
        case PUZZLE_STATE:
            return ((pickedObject!=null)?(pickedObject==playerChip[player]):true);
        }
    }
    private int findDistance(VoloCell from,VoloCell to,int dir)
    {	int dis = 0;
    	while(from!=to) { dis++; from = from.exitTo(dir); G.Assert(from!=null,"found"); }
    	return(dis);
    }
    public Hashtable<VoloCell,VoloMovespec>getSlideDests()
    {	Hashtable<VoloCell,VoloMovespec> res = new Hashtable<VoloCell,VoloMovespec>();
    	if((board_state==VoloState.LAND_FLOCK_STATE) && (startOfLine!=null))
    	{
        	CommonMoveStack all = new CommonMoveStack();
        	BlobStack playerBlobs = findBlobs(whoseTurn,false,false);
         	VoloBlob anchorBlob = findBlob(playerBlobs,startOfLine);
    		if(endOfLine==null) { getSlideMovesForLine(all,anchorBlob,startOfLine,0,0,whoseTurn); }
    		else 
    		{ int dir = findDirection(startOfLine.col,startOfLine.row,endOfLine.col,endOfLine.row);
    		  int dis = findDistance(startOfLine,endOfLine,dir);
    		  getSlideMovesForLine(all,anchorBlob,startOfLine,dir,dis,whoseTurn);
    		};
    		
    		while(all.size()>0) 
    			{ VoloMovespec m = (VoloMovespec)all.pop();
    			  VoloCell dest = getCell(m.to_col,m.to_row);
    			  VoloCell dest2 = dest.getEndOfLine(m.direction.getDirection(),m.nchips);
    			  if(dest2.chip==null) { res.put(dest2,m); }
    			  if(dest.chip==null) { res.put(dest,m); }
    			}
    	}
    	return(res);
    }
    
    public boolean LegalToHitBoard(VoloCell c)
    {	if(c==null) { return(false); }
    	if(c==hub) { return(false); }
        switch (board_state)
        {
        case DESIGNATE_ZONE_STATE:
        	{
        	// hit an empty space outside the player's reserve zone.
        	VoloBlob b = getReserveSet(whoseTurn);
        	return( ((b==null) || !b.contains(c))
        			&& (c.chip!=playerChip[whoseTurn]) );
 
        	}
        case LAND_FLOCK_STATE:
        	{	Hashtable<VoloCell,VoloMovespec>dests = getSlideDests();
        		return(dests.get(c)!=null);
        	}
       case SECOND_SLIDE_STATE:
    	   	return(hasSlideMoves(startOfLine,c));
       case PLAY_OR_SLIDE_STATE:
        	if((pickedObject==null) && hasSlideMoves(c)) { return(true); }
			//$FALL-THROUGH$
		case PLAY_STATE:
			{
			VoloBlob reserved = getReserveSet(nextPlayer[whoseTurn]);
	 		VoloChip myChip = playerChip[whoseTurn];
	 		return(isLegalDropMove(c,reserved,myChip)); 
			}
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
			return(isDest(c));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case PUZZLE_STATE:
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
    public void RobotExecute(VoloMovespec m)
    {
        robotStateStack.push(board_state);
        if (Execute(m,replayMode.Replay))
        {	VoloBlob b = zoneStack.top();
        	if(b!=null) { robotZoneStack.push(b); }
            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {
                doDone();
            }
         }
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(VoloMovespec m)
    {
        //G.print("U "+m+" for "+whoseTurn);
    	//verifyChips();
    	VoloState state = robotStateStack.pop();
 
        switch (m.op)
        {
        default:
   	    	throw G.Error("Can't un execute %s", m);
   	    case MOVE_PASS:
        case MOVE_DONE:
            break;
        case MOVE_SELECT:
        	switch(state)
        	{
        	case DESIGNATE_ZONE_STATE:
        		restoreClearedZone(robotZoneStack.pop(),replayMode.Replay);
        		break;
        	default: break;
        	}
        	
        	break;
        case MOVE_SLIDE:
        	unSlideMove(m,replayMode.Replay);
        	break;
        case MOVE_DROPB:
        	VoloCell dest = getCell(m.to_col,m.to_row);
        	VoloChip chip = dest.chip;
        	dest.chip = null;
        	((chip==playerChip[0]) ? playerChips[0] : playerChips[1]).remove(dest,false);
        	break;
        case MOVE_RESIGN:

            break;
        }
    	//verifyChips();

        setState(state);
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
    
    
 // true of this cell can be a "drop" site for a new bird.
 boolean isLegalDropMove(VoloCell c,VoloBlob reserved,VoloChip myChip)
 {
		VoloChip top = c.chip;
 		boolean ok = c!=hub && (top==null) && ((reserved==null) || !reserved.contains(c));
 		for(int dir=CELL_FULL_TURN; ok && dir>=0; dir--)
 				{	VoloCell adj = c.exitTo(dir);
 					ok &= ((adj==null) || (adj.chip!=myChip));
 				}
 		return(ok);
 }
 void getDropMoves(CommonMoveStack all,int forPlayer)
 {		VoloBlob reserved = getReserveSet(nextPlayer[forPlayer]);
 		VoloBlob myReserve = getReserveSet(forPlayer);
 		VoloChip myChip = playerChip[forPlayer];
 		for(VoloCell c = allCells; c!=null; c=c.next)
 		{
 		if(((myReserve==null) || !myReserve.contains(c)) && isLegalDropMove(c,reserved,myChip))
 			{all.push(new VoloMovespec(MOVE_DROPB,c.col,c.row,forPlayer));
 			}
  		}
 }
 
 commonMove getRandomDropMove(Random rand,int forPlayer)
 {		VoloBlob reserved = getReserveSet(nextPlayer[forPlayer]);
 		VoloBlob myReserve = getReserveSet(forPlayer);
 		VoloChip myChip = playerChip[forPlayer];
		cell<VoloCell> cellArray[] = getCellArray();
 		for(int boardSize = cellArray.length, randomCell=Random.nextInt(rand,boardSize),idx = 0;
 			idx<boardSize;
 			idx++)
 		{
 		int bi = idx + randomCell;
 		if(bi>=boardSize) { bi -= boardSize; }
 		VoloCell c = (VoloCell)cellArray[bi];
 		if(((myReserve==null) || !myReserve.contains(c)) && isLegalDropMove(c,reserved,myChip))
 			{return(new VoloMovespec(MOVE_DROPB,c.col,c.row,forPlayer));
 			}
  		}
 		return(null);
 }
 void verifyChips()
 {
	 for(VoloCell c = allCells; c!=null; c=c.next)
	 {
		 VoloChip top = c.chip;
		 boolean in0 = playerChips[0].contains(c);
		 boolean in1 = playerChips[1].contains(c);
		 
		 if(top==null) { G.Assert(!in0 && !in1,"not in"); }
		 else if(top==playerChip[0]) { G.Assert(in0 && !in1,"in 0 only"); }
		 else if(top==playerChip[1]) { G.Assert(in1 && !in0,"in 1 only"); }
	 }
 }
 
 void removeLine(VoloBlob anchorBlob,VoloChip chiptype,VoloCell anchor,int direction,int nsteps)
 {
 	VoloCell c = anchor;
 	int step = nsteps;
 	//verifyChips();
 	CellStack chips = (chiptype==playerChip[0]) ? playerChips[0] : playerChips[1];
 	do 
 	{	
 		G.Assert((c!=null) && (c.chip==chiptype),"expected a line");
 		chips.remove(c,false);
 		c.chip = null;
 		if(anchorBlob!=null) { anchorBlob.removeCell(c); }
 		c = c.exitTo(direction);
 	} while(step-->0);
 	//verifyChips();

 }
 // place a line of n chips in a specified direction
 void placeLine(VoloBlob anchorBlob,VoloChip chiptype,VoloCell anchor,int direction,int nsteps)
 {	
 	VoloCell c = anchor;
 	int step = nsteps;
 	CellStack chips = (chiptype==playerChip[0]) ? playerChips[0] : playerChips[1];
 	//verifyChips();

 	do 
 	{	
 		G.Assert((c!=null) && (c.chip==null),"expected an empty line");
 		chips.push(c);
 		c.chip = chiptype;
 		if(anchorBlob!=null) { anchorBlob.addCell(c); }
 		c = c.exitTo(direction);
 	} while(step-->0);
 	//verifyChips();

 }
 // true if we can place a row of n chips in empty cells in direction
 boolean canPlaceLine(VoloCell anchor0,int direction, int nsteps0)
 {	VoloCell anchor = anchor0;
 	int nsteps=nsteps0;
 	while(nsteps-->=0)
 	{
	 	if(anchor==null) { return(false); }
	 	if(anchor.chip!=null) { return(false); }
	 	anchor = anchor.exitTo(direction);
 	}
	return(true);
 }
 // a line segment can move in any of 6 directions, and continues to slide until it
 // hits the edge or an obstruction.  At each point in it's travel, if it contacts both
 // the original group and a new friendly group, it's a legal move.   Special case where
 // the entire flock is moving.  Semi-special cases where we rejoin the original flock.
 void getSlideMovesForLine(CommonMoveStack all,VoloBlob anchorBlob,VoloCell anchor,int direction, int nsteps,int forPlayer)
 {	int original_size = anchorBlob.size;
 	VoloChip color = anchorBlob.color; 
 	removeLine(anchorBlob,color,anchor,direction,nsteps);
 	//
 	// we've removed the moving segment, try to slide in each of the 6 possible directions and test for
 	// a legal reconfiguration, which must contact the original blob, and also be larget than the original
 	//
 	for(int dir=0; dir<6; dir++)
 	{	
 		VoloCell c = anchor;
 		c = c.exitTo(dir);
  		while(c!=null)
 		{
 		if(canPlaceLine(c,direction,nsteps))
 		{	placeLine(null,color,c,direction,nsteps);
 			VoloBlob newBlob = findBlob(c,forPlayer,false,false);	// find the new blob
 			if((newBlob.size>original_size)
 					&& newBlob.containsAllOf(anchorBlob))
 			{	VoloMovespec newmove = new VoloMovespec(MOVE_SLIDE,anchor.col,anchor.row,Directions[direction],nsteps,c.col,c.row,forPlayer); 
 				all.push(newmove);
 			}
 			removeLine(null,color,c, direction, nsteps);	// remove the temporary stones
 			c = c.exitTo(dir);	// next step
 		}
 		else { c = null; }
 		}
 	}
 	placeLine(anchorBlob,color,anchor,direction,nsteps);
 }
 
 commonMove getRandomSlideMoveForLine(Random rand,VoloBlob anchorBlob,VoloCell anchor,int direction, int nsteps,int forPlayer)
 {	int original_size = anchorBlob.size;
 	commonMove value = null;
 	VoloChip color = anchorBlob.color; 
 	removeLine(anchorBlob,color,anchor,direction,nsteps);
 	//
 	// we've removed the moving segment, try to slide in each of the 6 possible directions and test for
 	// a legal reconfiguration, which must contact the original blob, and also be larget than the original
 	//
 
 	for(int dir=0; (dir<6) && (value==null); dir++)
 	{	
 		VoloCell c = anchor;
 		c = c.exitTo(dir);
 		while((c!=null) && (value==null))
 		{
 		if(canPlaceLine(c,direction,nsteps))
 		{	placeLine(null,color,c,direction,nsteps);
 			VoloBlob newBlob = findBlob(c,forPlayer,false,false);	// find the new blob
 			if((newBlob.size>original_size)
 					&& newBlob.containsAllOf(anchorBlob))
 			{	value  = new VoloMovespec(MOVE_SLIDE,anchor.col,anchor.row,Directions[direction],nsteps,c.col,c.row,forPlayer); 
 			}
 			removeLine(null,color,c, direction, nsteps);	// remove the temporary stones
 			c = c.exitTo(dir);	// next step
 		}
 		else { c = null; }
 		}
 	}
 	placeLine(anchorBlob,color,anchor,direction,nsteps);
 	return(value);
 }
 
 void getSlideMoves(CommonMoveStack all,BlobStack playerBlobs,VoloCell c,int forPlayer,boolean omnidirectional)
 {	VoloChip myChip = playerChip[forPlayer];
	if(c.chip==myChip)
		{	// first the chip alone
			VoloBlob anchorBlob = findBlob(playerBlobs,c);
			getSlideMovesForLine(all,anchorBlob,c,0,0,forPlayer);	
			// map over all possible line segments anchored by this chip.  
			for(int dir=0;dir<(omnidirectional?6:3);dir++)
			{	
			int nsteps = 0;
			for(VoloCell d = c.exitTo(dir); d!=null; )
				{	if((d!=null) && (d.chip==myChip))
					{	nsteps++;
						getSlideMovesForLine(all,anchorBlob,c,dir,nsteps,forPlayer);
						d = d.exitTo(dir);
					}
					else 
					{ d = null; 
					}
				}
			}
		}
 }
 
 commonMove getRandomSlideMove(Random rand,BlobStack playerBlobs,VoloCell c,int forPlayer)
 {	VoloChip myChip = playerChip[forPlayer];
	G.Assert(c.chip==myChip,"piece mismatch");
		{	// first the chip alone
			VoloBlob anchorBlob = findBlob(playerBlobs,c);
			// map over all possible line segments anchored by this chip.  
			for(int dir=0;dir<=3;dir++)
			{	
			if(dir==3)
			{
				commonMove val = getRandomSlideMoveForLine(rand,anchorBlob,c,0,0,forPlayer);
				if(val!=null) { return(val); }
			}
			else
			{
			int nsteps = 0;
			for(VoloCell d = c.exitTo(dir); d!=null; )
				{	if((d!=null) && (d.chip==myChip))
					{	nsteps++;
					commonMove val = getRandomSlideMoveForLine(rand,anchorBlob,c,dir,nsteps,forPlayer);
					if(val!=null) { return(val); }
					d = d.exitTo(dir);
					}
					else 
					{ d = null; 
					}
				}
			}}
		}
		return(null);
 }
 
 void getSlideMoves(CommonMoveStack all,int forPlayer,boolean omnidirectional)
 {	BlobStack playerBlobs = findBlobs(forPlayer,false,false);
 	for(VoloCell c = allCells; c!=null; c=c.next)
 		{	getSlideMoves(all,playerBlobs,c,forPlayer,omnidirectional);
 		}
 }
 
 commonMove getRandomSlideMove(Random rand,int forPlayer)
 {	BlobStack playerBlobs = findBlobs(forPlayer,false,false);
 	CellStack chips = playerChips[forPlayer];
 	for(int nChips = chips.size(), randomChip=Random.nextInt(rand,nChips),idx=0;
 		idx<nChips;
 		idx++)
 		{
 		int chipidx = idx + randomChip;
 		if(chipidx>=nChips) { chipidx -= nChips; }
 		VoloCell c = chips.elementAt(chipidx);
 		commonMove val = getRandomSlideMove(rand,playerBlobs,c,forPlayer);
 		if(val!=null) { return(val); }
 		}
 	return(null);
 }
 
 public CommonMoveStack  getSlideMoves(VoloCell c)
 {	if(c.chip==playerChip[whoseTurn])
	 {
	 BlobStack playerBlobs = findBlobs(whoseTurn,false,false);
 	 CommonMoveStack  all = new CommonMoveStack();
 	 getSlideMoves(all,playerBlobs,c,whoseTurn,true);
 	 return(all);
	 }
 	return(null);
 }
 
 //
 // true if c is a party to any slide move
 //
 public boolean hasSlideMoves(VoloCell c)
 {	 CommonMoveStack all = getSlideMoves(c);
	 if(all!=null) { return(all.size()>0); }
	 return(false);
 }
 
 public Hashtable<VoloCell,VoloMovespec>getSelectDests()
 {	Hashtable<VoloCell,VoloMovespec> val  = new Hashtable<VoloCell,VoloMovespec>();
 	if(startOfLine!=null)
 	{
	CommonMoveStack all = getSlideMoves(startOfLine);
	if(all!=null)
	 	{	while(all.size()>0)
	 		{	VoloMovespec m = (VoloMovespec)all.pop();
	 			VoloCell lineDest = startOfLine.getEndOfLine(m.direction.getDirection(),m.nchips);
	 			val.put(lineDest,m);
	 		}
	 	}
 	}
	return(val);
 }
 //
 // true of there are slide moves for a line from C to D
 //
 public boolean hasSlideMoves(VoloCell c,VoloCell d)
 {	CommonMoveStack all = getSlideMoves(c);
 	if(all!=null)
 	{	while(all.size()>0)
 		{	VoloMovespec m = (VoloMovespec)all.pop();
 			VoloCell lineDest = c.getEndOfLine(m.direction.getDirection(),m.nchips);
 			if(d==lineDest) { return(true); }
 		}
 	}
 	return(false);
 }
 //
 // true of we have any slide moves at all, used to decide between 
 // PLAY_STATE and PLAY_OR_SLIDE_STATE
 //
 public boolean hasSlideMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
 	getSlideMoves(all,whoseTurn,false);
 	return(all.size()>0);
 }
 public void getDesignateZoneMoves(CommonMoveStack all,int player)
 {	BlobStack playerZones = findBlobs(player,true,false);	// player controlled zones
 	VoloBlob reserve = getReserveSet(player,playerZones);
 	while(playerZones.size()>0)
 	{
 		VoloBlob z = playerZones.pop();
		int low = z.lowestMemberOrdinal();
		if((reserve==null) || !reserve.containsOrdinal(low))
		{	VoloCell cell = cellIndex[low];
			all.addElement(new VoloMovespec(MOVE_SELECT,cell.col,cell.row,player));
		}
 	}
 }
 public commonMove getRandomDesignateZoneMoves(Random rand,int player)
 {	BlobStack playerZones = findBlobs(player,true,false);	// player controlled zones
 	VoloBlob reserve = getReserveSet(player,playerZones);
 	for(int maxZone=playerZones.size(),randomZone=Random.nextInt(rand,maxZone), zone0 = 0;
 		zone0 < maxZone;
 		zone0++)
 	{	int zone = zone0 + randomZone;
 		if(zone>=maxZone) { zone -= maxZone; }
 		VoloBlob z = playerZones.elementAt(zone);
		int low = z.lowestMemberOrdinal();
		if((reserve==null) || !reserve.containsOrdinal(low))
		{	VoloCell cell = cellIndex[low];
			return(new VoloMovespec(MOVE_SELECT,cell.col,cell.row,player));
		}
 	}
 	return(null);
 }
 
 commonMove Get_Random_Move(Random rand)
 {
	 	switch(board_state)
	 	{
	 	default: throw G.Error("Not expecting state ",board_state);
	 	case DESIGNATE_ZONE_STATE:
	 		return(getRandomDesignateZoneMoves(rand,whoseTurn));
	 	case PLAY_OR_SLIDE_STATE:
	 		int empties = getCellArray().length - playerChips[0].size()-playerChips[1].size();
	 		int mychips = playerChips[whoseTurn].size()*3;			// assume average of 3 moves per chip
	 		int total = empties + mychips;
	 		int randSplit = Random.nextInt(rand,total);
	 		// we want to bias the "slide" verses "drop" moves fairly, even though we don't
	 		// want to count the slide moves. So it's a guess.
	 		for(int startOff = (randSplit>empties) ? 0 : 1,idx=0; 
	 			idx<2;
	 			idx++)
	 		{
	 		int pass = idx + startOff;
	 		if(pass>=2) { pass -=2; }
	 		if(pass==0)
	 			{ commonMove val = getRandomSlideMove(rand,whoseTurn);
	 			  if(val!=null) { return(val); }
	 	 		}
	 			else 
	 			{ commonMove val = getRandomDropMove(rand,whoseTurn);
	 			  if(val!=null) { return(val); }
	 			}
	 		}
	 		return(new VoloMovespec(MOVE_PASS,whoseTurn));
	 		
	 		// fall through
	 	case PLAY_STATE:
	 		commonMove val = getRandomDropMove(rand,whoseTurn);
	 		if(val!=null) { return(val); }
	 		return(new VoloMovespec(MOVE_PASS,whoseTurn));
	 	}
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();

 	switch(board_state)
 	{
 	default: throw G.Error("Not expecting state ",board_state);
 	case DESIGNATE_ZONE_STATE:
 		getDesignateZoneMoves(all,whoseTurn);
 		break;
 	case PLAY_OR_SLIDE_STATE:
 		getSlideMoves(all,whoseTurn,false);
		//$FALL-THROUGH$
	case PLAY_STATE:
 		getDropMoves(all,whoseTurn);
 		if(all.size()==0) { all.addElement(new VoloMovespec(MOVE_PASS,whoseTurn)); }
 		break;
 	}
 	return(all);
 }
 

}
