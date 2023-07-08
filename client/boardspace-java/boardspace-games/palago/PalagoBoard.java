package palago;

import java.awt.Color;

import online.game.*;
import java.util.*;
import lib.*;
import lib.Random;

/**
 * PalagoBoard knows all about the game of Palago, which is played
 * on a hexagonal board. It gets a lot of logistic support from 
 * common.hexBoard, which knows about the coordinate system.  
 * 
 * This class doesn't do any graphics or know about anything graphical, 
 * but it does know about states of the game that should be reflected 
 * in the graphics.
 * 
 *  The principle interface with the game viewer is the "Execute" method
 *  which processes moves.  Note that this
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

class PalagoBoard extends infiniteHexBoard<PalagoCell> implements BoardProtocol,PalagoConstants
{	static final int REVISION = 101;		// 101 switches to an infinite board
	public int getMaxRevisionLevel() { return(REVISION); }

    static final String[] PalagoGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
    /* the "external representation for the board is A1 B2 etc.  This internal representation is X,Y
    where adjacent X's are separated by 2.  This gives the board nice mathematical properties for
    calculating adjacency and connectivity. */
	private PalagoState unresign;
	private PalagoState board_state;
	public PalagoState getState() {return(board_state); }
	public void setState(PalagoState st) 
	{ 	unresign = (st==PalagoState.RESIGN_STATE)?board_state:null;
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
	public void SetDrawState() {throw G.Error("not expected"); };	
	public int scan_clock = 0;
    int chips_on_board = 0;			// number of chips currently on the board.  This is used
    										// to number the chips as they are placed.
    private PalagoCell placedChips=null;	// linked list of nonempty chips
    private LineInfoStack lines=null;				// stack of lineInfo objects
 
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public PalagoChip pickedObject = null;
    public PalagoChip lastPicked = null;
    public PalagoCell chipPool[] = new PalagoCell[PalagoChip.nChips];	// dummy source for the chip pools
    public PalagoCell pickedSourceStack[] = new PalagoCell[3]; 
    public PalagoCell droppedDestStack[] = new PalagoCell[3];
    public PalagoCell getSource() 
    	{ return((pickedObject==null)?null:pickedSourceStack[stackIndex]); }
    public PalagoCell getDest() { return(droppedDestStack[ stackIndex-((stackIndex>0)?1:0)]); }
    public PalagoCell firstDestCell() { return(droppedDestStack[0]); }
    public int stackIndex = 0;
    public PalagoChip lastDroppedDest = null;	// for image adjustment logic

    private int addAdj(Hashtable<PalagoCell,PalagoCell> h,PalagoCell c)
    {	int n=0;
 		for(int dir = 0;dir<CELL_FULL_TURN;dir++)
		{	PalagoCell d = c.exitTo(dir);
			if(d!=null && d.height()==0)
			{	n++;
				h.put(d,d);
			}
		}
		return(n);
    }
    public Hashtable<PalagoCell,PalagoCell> movingObjectDests()
    {	int n = 0;
    	Hashtable<PalagoCell,PalagoCell> h = new Hashtable<PalagoCell,PalagoCell>();
    	switch(board_state)
    	{
    	case PLAY2_STATE:
        case CONFIRM2_STATE:
    	case CONFIRM_STATE:
    		{
    		PalagoCell dest = firstDestCell();
    		if(dest!=null) { addAdj(h,dest); }
    		}
    		break;
    	case PUZZLE_STATE:
    	case PLAY_STATE:
	    	if((pickedObject!=null)||(placedChips==null))
	    	{	for(PalagoCell c = placedChips; c!=null; c=c.nextPlaced)
	    		{	n += addAdj(h,c);
	    		}
	    		if(n==0) 
	    			{ PalagoCell c = getCell((char)('A'+ncols/2),nrows/2);
	    			  h.put(c,c);
	    			}
	    	}
			break;
		default:
			break;
    	}
    	return(h);
    }
	// factory method to generate a board cell
	public PalagoCell newcell(char c,int r)
	{	return(new PalagoCell(c,r,PalagoId.BoardLocation));
	}
    public PalagoBoard(String init,int map[]) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = PalagoGRIDSTYLE;
        revision = REVISION;
        setColorMap(map, 2);
        doInit(init); // do the initialization 
    }

    public PalagoBoard cloneBoard() 
	{ PalagoBoard dup = new PalagoBoard(gametype,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((PalagoBoard)b); }


    // standared init for Hex.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard(String game)
    {	
    	if(Palago_INIT.equalsIgnoreCase(game)) {  }
    	else { throw G.Error(WrongInitError,game); }
        gametype = game;
        setState(PalagoState.PUZZLE_STATE);
        reInitBoard(19,19); //this sets up a hexagonal board
         
        whoseTurn = FIRST_PLAYER_INDEX;
        chips_on_board = 0;
        for(int i=0;i<droppedDestStack.length;i++) { pickedSourceStack[i]=droppedDestStack[i]=null; }
        stackIndex = 0;
        lines = null;
        placedChips = null;
        lastDroppedDest = null;

        // set the initial contents of the board to all empty cells
		for(PalagoCell c = allCells; c!=null; c=c.next)
			{ c.chip=null; 
			  c.nextPlaced=null; 
			 }
		
		//for(int i=1;i<ncols;i++)
		//{	getCell((char)('@'+i),1).addChip(PalagoChip.getChip(i%3));
		//    if(i>1) { getCell('A',i).addChip(PalagoChip.getChip(i%3));}
		//}
    }
    public void sameboard(BoardProtocol f) { sameboard((PalagoBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(PalagoBoard from_b)
    {
        super.sameboard(from_b); // hexboard compares the boards

        //G.Assert(pickedObject==from_b.pickedObject,"same pickedObject");
        G.Assert((placedChips==from_b.placedChips)
        		|| ((placedChips!=null)&&placedChips.sameCellLocation(from_b.placedChips)),"same placed list");
        // here, check any other state of the board to see if
        G.Assert((stackIndex == from_b.stackIndex),"stackIndex matches");
        G.Assert((chips_on_board == from_b.chips_on_board),"chips on board matches");
        G.Assert(Digest()==from_b.Digest(),"Digest matches");
        
    }

    /** 
     * Digest produces a 32 bit hash of the game state.  This is used 3 different
     * ways in the system.
     * (1) This is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game, and a midpoint state of the game. Other site machinery
     *  looks for duplicate digests.  
     * (2) Digests are also used as the game is played to look for draw by repetition.  The state
     * after most moves is recorded in a hashtable, and duplicates/triplicates are noted.
     * (3) Digests are used by the search machinery as a check on the robot's winding/unwinding
     * of the board position, this is mainly a debug/development function, but a very useful one.
     * @return
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

		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
		v ^= Digest(r,pickedObject);
        return (v);
    }
    public PalagoCell getCell(PalagoCell c)
    {	return(c==null?null:getCell(c.rackLocation(),c.col,c.row));
    }
    public PalagoCell getCell(PalagoId source,char col,int row)
    {
    	switch(source)
    	{
    	default: throw G.Error("Not expecting source %s",source);
    	case BoardLocation: return(getCell(col,row));
    	case ChipPool: return(chipPool[row]);
    	}
    }
    // this visitor method implements copying the contents of a cell on the board
    public void copyFrom(PalagoCell cc,PalagoCell fc)
    {
    	super.copyFrom(cc,fc);
    	cc.nextPlaced = getCell(fc.nextPlaced);
    }
    
    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(PalagoBoard from_b)
    {	super.copyFrom(from_b);
 
        chips_on_board = from_b.chips_on_board;
        stackIndex = from_b.stackIndex;
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        lastPicked = null;
        lines = null;
        placedChips = getCell(from_b.placedChips);
        stackIndex = from_b.stackIndex;
        pickedObject = from_b.pickedObject;
        board_state = from_b.board_state;
        unresign = from_b.unresign;

        sameboard(from_b); 
    }
    
    public String gameType()
    { 
      if(revision<101) { return gametype; }
      // the lower case is a secret clue that we're in 4 token mode
      // instead of 1 token      else
      return(G.concat(gametype.toLowerCase()," ",players_in_game," ",randomKey," ",revision));
    }

    public void reInit(String d)
    {
    	revision = 0;
    	doInit(d);
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {  
       
   	   StringTokenizer tok = new StringTokenizer(gtype);
   	   String typ = tok.nextToken();
   	   int np = tok.hasMoreTokens() ? G.IntToken(tok) : players_in_game;
   	   long ran = tok.hasMoreTokens() ? G.IntToken(tok) : key;
   	   int rev = tok.hasMoreTokens() ? G.IntToken(tok) : revision;
   	   
   	   doInit(typ,np,ran,rev);
    }
    public void doInit(String typ,int np,long ran,int rev)
    {  
       Random r = new Random(72094);
   	   adjustRevision(rev);
   	   randomKey =ran;
   	   G.Assert(np==2,"players can only be 2");
   	   setIsTorus(rev<101);
       Init_Standard(typ);
       
       for(int i=0;i<PalagoChip.nChips;i++)
       {	chipPool[i]=new PalagoCell(r,PalagoId.ChipPool,i,PalagoChip.getChip(i));
       }
        moveNumber = 1;
        lines = null;
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
        case CONFIRM2_STATE:
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
        case CONFIRM2_STATE:
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

    public boolean WinForPlayerNow(int player)
    {	if(win[player]) 
    	{ return(true); }
    	switch(getColorMap()[player])
    	{
    	case 0: return(isWinForYellow());
    	case 1: return(isWinForBlue());
    	default: throw G.Error("Not defined");
    	}
      }


    private void removePlacedChip(PalagoCell cell)
    {	PalagoCell prev = null;
    	PalagoCell curr = placedChips;
    	lines = null;
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
    private void addPlacedChip(PalagoCell cell)
    {  	G.Assert(cell.nextPlaced==null,"Not already placed");
    	//G.print("Add "+cell);
        cell.nextPlaced = placedChips;
        lines = null;
    	placedChips = cell;
    }
    
    // set the contents of a cell, and maintian the books
    public PalagoChip SetBoard(PalagoCell c,PalagoChip ch)
    {	PalagoChip old = c.topChip();
    	if(old!=ch)
    	{
    	if(c.onBoard)
    	{
    	if(old!=null) { chips_on_board--; if(ch==null) { removePlacedChip(c); }}
     	if(ch!=null) { chips_on_board++; if(old==null) { addPlacedChip(c); }}
    	}
       	c.chip = ch;
       	if(ch!=null) { c.cellName = ""+chips_on_board; } else { c.cellName=""; }
       	lines = null;
    	}
    	return(old);
    }
    //
    // accept the current placements as permanant
    //
    public void acceptPlacement()
    {	while(stackIndex>=0)
    	{	droppedDestStack[stackIndex] = null;
    		pickedSourceStack[stackIndex] = null;
    		stackIndex--;
    	}
    	stackIndex=0;
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
    	{	 SetBoard(pickedSourceStack[stackIndex],pickedObject);
    	    pickedSourceStack[stackIndex] = null;
    	}
	  pickedObject=null;
    }
    // 
    // drop the floating object on the boad.
    //
    private void dropBoardCell(PalagoCell c)
    {     	SetBoard(c,pickedObject);
            lastDroppedDest = pickedObject;
            droppedDestStack[stackIndex++] = c;
            pickedObject = null;
    }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(PalagoCell c)
    {	return((stackIndex>0) && (droppedDestStack[stackIndex-1]==c));
    }
    public boolean isDest(char col,int ro)
    {	return(isDest(getCell(col,ro)));
    }
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    { 	PalagoChip ch = pickedObject;
    	if(( ch!=null) && (pickedSourceStack[stackIndex]!=null) && (droppedDestStack[stackIndex]==null))
    	{	return(ch.chipNumber()); 
    	}
      	return (NothingMoving);
    }

	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickBoardCell(PalagoCell c)
    {

       	boolean wasDest = isDest(c);
       	unDropObject(); 
       	if(!wasDest)
       	{
        pickedSourceStack[stackIndex] = c;
        lastPicked = pickedObject = c.topChip();
       	droppedDestStack[stackIndex] = null;
       	SetBoard(c,null);
       	}
  
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(PalagoCell c)
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
        case CONFIRM2_STATE:
        	if(droppedDestStack[stackIndex]==null)
        	{setNextStateAfterDone();
        	}
        	break;
        case PLAY_STATE:
        	boolean winForMe = WinForPlayerNow(whoseTurn);
        	boolean winForHim = WinForPlayerNow(nextPlayer[whoseTurn]);
        	if(winForMe || winForHim) { setState(PalagoState.CONFIRM2_STATE); }
        	else  {	setState(PalagoState.PLAY2_STATE); }
        	break;
        case PLAY2_STATE:
			setState(PalagoState.CONFIRM_STATE);
			break;
        case PUZZLE_STATE:
			acceptPlacement();
            break;
        }
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
        case CONFIRM2_STATE:
    	case CONFIRM_STATE:
    	case PUZZLE_STATE:
    	case PLAY_STATE:
    		setState(PalagoState.PLAY_STATE);
			break;
		case PLAY2_STATE:
    		break;
    	}

    }
    private boolean doWin()
    {
    	boolean winForMe = WinForPlayerNow(whoseTurn);
    	boolean winForHim = WinForPlayerNow(nextPlayer[whoseTurn]);
    	if(winForHim) 
    		{ win[nextPlayer[whoseTurn]] = true;
    		  setState(PalagoState.GAMEOVER_STATE);
    		  return(true); 
    		}	// simultaneous win is a loss for me
    	else if(winForMe)
    		{ win[whoseTurn] = true;
    		  setState(PalagoState.GAMEOVER_STATE);
    		  return(true); 
    		}	
    	
    	return(false);
    }
    private void doDone()
    {
        acceptPlacement();

        if (board_state==PalagoState.RESIGN_STATE)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(PalagoState.GAMEOVER_STATE);
        }
        else
        {	if(doWin()) {}
        	else {setNextPlayer();
        		setNextStateAfterDone();
        	}
        }
    }
    public void setNextStateAfterPick()
    {
    	switch(board_state)
    	{
    	case PUZZLE_STATE:	break;
    	case CONFIRM_STATE: setState(PalagoState.PLAY2_STATE); break;
    	case CONFIRM2_STATE:
    	case PLAY2_STATE: setState(PalagoState.PLAY_STATE); break;
    	default: throw G.Error("Not expecting state %s",board_state);
    	}
	
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	Palagomovespec m = (Palagomovespec)mm;

        //G.print("E "+m+" for "+whoseTurn+" "+board_state+" "+Digest());
        switch (m.op)
        {
       case MOVE_DONE:

         	doDone();

            break;

        case MOVE_DROPB:
			{
			PalagoCell c = getCell(m.to_col,m.to_row);
			PalagoChip top = c.topChip();
			if(top!=null) { pickBoardCell(c); setNextStateAfterPick(); }
 			switch(board_state)
			{ case PUZZLE_STATE: acceptPlacement(); break;

			  case CONFIRM_STATE: unDropObject(); unPickObject(); setState(PalagoState.PLAY2_STATE); break;
		      case CONFIRM2_STATE:
			  case PLAY_STATE:
			  case PLAY2_STATE:
				  break;
			default:
				break;
			}
			pickedObject = PalagoChip.getChip(m.object);		// get this object
			pickedSourceStack[stackIndex] = chipPool[m.object];
            dropBoardCell(c);
            createExitCells(c);
            setNextStateAfterDrop();
            lines = null;
			}
            break;

        case MOVE_PICK:
 			{
 			if(pickedObject!=null) { unPickObject();  }
			PalagoCell src = chipPool[m.to_row];
			lastPicked = pickedObject = src.topChip();
		    pickedSourceStack[stackIndex] = src;
 			}
            break;

            // fall through
        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	{
        	PalagoCell c = getCell(m.to_col, m.to_row);
        	pickBoardCell(c);
        	setNextStateAfterPick();
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
            setState(PalagoState.PUZZLE_STATE);
            if(doWin()) {}
            else { setNextStateAfterDone(); }

            break;

       case MOVE_RESIGN:
            setState(unresign==null?PalagoState.RESIGN_STATE:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(PalagoState.PUZZLE_STATE);

            break;
       case MOVE_GAMEOVERONTIME:
     	   win[whoseTurn] = true;
     	   setState(PalagoState.GAMEOVER_STATE);
     	   break;
       default:
        	cantExecute(m);
        }
        //G.print("X "+m+" for "+whoseTurn+" "+board_state+" "+Digest());

        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case CONFIRM2_STATE:
        case CONFIRM_STATE:
        	if(pickedObject==null) { return(false); }
			//$FALL-THROUGH$
		case PLAY2_STATE:
        case PLAY_STATE:
        	// you can pick up a stone in the storage area
        	// but it's really optional
        	return(true);
        	
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
        case PUZZLE_STATE:
            return (true);
        }
    }

    public boolean LegalToHitBoard(PalagoCell c)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
        case CONFIRM2_STATE:
        	if(isDest(c)) { return(true); }	// pick up same chip
        	break;
		case CONFIRM_STATE:
        case PLAY2_STATE:
        	{
        	if(isDest(c)) { return(pickedObject==null); }	// pick up same chip
        	if(c.topChip()!=null) { return(false); }
        	PalagoCell first = firstDestCell();
        	for(int dir=0;dir<CELL_FULL_TURN; dir++)
        	{	PalagoCell nx = c.exitTo(dir);
        		if(nx==first) { return(true); }	// adjacent to previous cell
        	}}
        	break;
 		case PLAY_STATE:
			if(c.chip==null)
			{  if(chips_on_board==0) { return(true); }
			   for(int i=0;i<CELL_FULL_TURN;i++) 
			   {	PalagoCell nx = c.exitTo(i);
			   		// must be adjacent to the existing matrix
			   		if(nx!=null && nx.topChip()!=null) { return(true); }
			   }
			   
			}
			break;
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			break;
        default:
        	throw G.Error("Not expecting state %s", board_state);

        case PUZZLE_STATE:
            return ((pickedObject!=null) 
            		  ? (c.topChip()==null)
            		  : true);
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
    public void RobotExecute(Palagomovespec m)
    {
        m.state = board_state;
        m.undoInfo = stackIndex; //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        stackIndex = 0;
        m.dstack = droppedDestStack[0];
        if (Execute(m,replayMode.Replay))
        {
            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {
                doDone();
            }
            else if(m.op==MOVE_DROPB) { }
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
    public void UnExecute(Palagomovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);

        switch (m.op)
        {
        default:
   	    	cantExecute(m);
        	break;

        case MOVE_DONE:
            break;
            
        case MOVE_DROPB:
        	SetBoard(getCell(m.to_col,m.to_row),null);
        	droppedDestStack[0]=pickedSourceStack[0]=null;
         	stackIndex = 0;
        	break;
        case MOVE_RESIGN:

            break;
        }

        setState(m.state);
        stackIndex = m.undoInfo;
        droppedDestStack[0]=m.dstack;
        lines = null;
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
    
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
 	switch(board_state)
 	{
 	default:	throw G.Error("Not implemented");
 	case PLAY_STATE:
 		if(chips_on_board==0) 
 			{ // always open with tile 0
 				all.addElement(new Palagomovespec(whoseTurn,MOVE_DROPB,(char)('A'+ncols/2),nrows/2,0)); 
 			}
 		else 
 		{ Hashtable<PalagoCell,PalagoCell> moves = new Hashtable<PalagoCell,PalagoCell>();
 		  for(PalagoCell c = placedChips; c!=null; c=c.nextPlaced) 
		  {	for(int dir=0; dir<CELL_FULL_TURN; dir++)
		  	{
			 PalagoCell adj = c.exitTo(dir);
			 if((adj.topChip()==null) && (moves.get(adj)==null))
			 {
				moves.put(adj,adj); 
				for(int i=0;i<PalagoChip.nChips;i++)
				{
				all.addElement(new Palagomovespec(whoseTurn,MOVE_DROPB,adj.col,adj.row,i));
				}
			 }
		  	}
		  }
 		}
 		break;
 	case PLAY2_STATE:
 		{ PalagoCell destc = firstDestCell();
 		  G.Assert(destc!=null,"previous dest cell known");
 		  int nMoves = 0;
 		  if(chips_on_board==1)
 	 			{	// follow with 0/2 or 1/3, the prescribed openings
 	 				PalagoCell center = placedChips;
 	 				PalagoCell c2 = center.exitTo(2);
 	 				PalagoCell c3 = center.exitTo(3);
 	 				all.addElement(new Palagomovespec(whoseTurn,MOVE_DROPB,c2.col,c2.row,0));
 	 				all.addElement(new Palagomovespec(whoseTurn,MOVE_DROPB,c3.col,c3.row,1));
 	 			}
 		  else
 		  {
 		  for(int dir=0;dir<CELL_FULL_TURN; dir++)
 		  {	PalagoCell adj = destc.exitTo(dir);
 		  	if(adj.topChip()==null) 
 		  		{ 
 		  		  for(int i=0;i<PalagoChip.nChips;i++)
 		  			{ all.addElement(new Palagomovespec(whoseTurn,MOVE_DROPB,adj.col,adj.row,i)); 
 		  			  nMoves++;
 		  			}
 		  		}
 		  }
 		  	if(nMoves==0) { all.addElement(new Palagomovespec(whoseTurn,MOVE_RESIGN)); }
 		  }
 		}
 		break;
 	}
 	
 	return(all);
 }
 
 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(Character.isDigit(txt.charAt(0)))
	 	{ xpos-=2;
	 	}
 	GC.Text(gc, false, xpos-2, ypos, -1, 0,clt, null, txt);
 }

boolean isSmallLoop(PalagoCell current,int mask,int entry,int exit)
{	// recognize donut holes.  These are lines that have not been scanned at all,
	// and which have two nonblank neighbors (making a group of 3).  The only way
	// a group of 3 can be unscanned is if it is a closed loop with no arch.
	if((current.loopCode&mask)==0)		// not scanned, so no backbone
	{	PalagoCell enterNext = current.exitTo(entry);
		if(((enterNext.loopCode&PalagoChip.center_loop_code) !=0)
			&& (enterNext.topChip()!=null))
		{	PalagoCell exitNext = current.exitTo(exit);
			if((exitNext.scan_clock==current.scan_clock)
				&& enterNext.topChip()!=null)
			{	return(true);
			}
		}
	}
	return(false);
	}
void twoTipPattern(lineInfo info,PalagoCell current,PalagoCell nextCell,int entry,PalagoChip x1,PalagoChip x2)
{	PalagoCell ex1 = current.exitTo(entry+1);
	PalagoChip exit1 = ex1.topChip();
	PalagoCell nx1 = nextCell.exitTo(entry+2);
	PalagoChip next2 = nx1.topChip();
	PalagoCell ex2 = current.exitTo(entry+2);
	PalagoChip exit2 = ex2.topChip();
	if( (exit2 == null)
		&& ( ((exit1==null) && (next2==null))
			|| ((exit1 == x1) && ((next2==null)||(next2==x2)))
			|| ((next2 == x2) && ((exit1==null)||(exit1==x1))))
		)
	{	info.twotip_count++;
		if(exit1!=null)
		{	PalagoChip ex1top=ex1.exitTo(entry+2).topChip();
			if(x1==ex1top) 
				{ info.fourtip_count++; 
				}
		}
		if(next2!=null)
		{	PalagoChip nx1top=nx1.exitTo(entry+1).topChip();
			if(x2==nx1top) 
				{ info.fourtip_count++; 
				}
		}
		if((exit1!=null)&&(next2!=null)&& (ex2.isHole()))
		{	info.hole_count++;
		}
		
	}
}

void squeezePattern(lineInfo info,PalagoCell current,int entry,PalagoChip entryChip,PalagoChip exitChip)
{	PalagoCell ex1 = current.exitTo(entry);
	PalagoChip exit1 = ex1.topChip();
	if(exit1!=entryChip) { return; }
	PalagoCell ex2 = current.exitTo(entry+4);
	PalagoCell ex3 = current.exitTo(entry+5);
	PalagoChip exit2 = ex2.topChip();
	PalagoChip exit3 = ex3.topChip();
	if((exit2==exitChip)&&(exit3==null)) { info.squeeze_count++; }
	else if((exit3==entryChip)&&(exit2==null)) { info.squeeze_count++; }
}

// perform service functions for lineinfo "traceline"
void visitLine(lineInfo info,int op,PalagoCell current,PalagoChip top,int entry,int exit)
{
	switch(op)
	{
	case lineInfo.SetClock_Op:
		current.scan_clock = scan_clock;
		break;
	case lineInfo.BlueIncomplete_Op:
	{
		int mainline = top.mainLine;
		if((entry==mainline)||(exit==mainline))
		{	if((current.loopCode&(PalagoChip.blue_loop_code|PalagoChip.partial_blue_code))!=0)
				{ info.incomplete_count++; }
			else 
				{ info.incomplete_count--; }
		{
			PalagoCell nextCell = current.exitTo(exit);
			PalagoChip nextTop = nextCell.topChip();
			if(nextTop==top)	// 2 in a row;
			{
			switch(top.index)
			{
			case PalagoChip.left_index:
				//normalize the entry direction
				if(entry==3) { PalagoCell nn = nextCell; nextCell=current; current=nn; entry=0; }
					else { G.Assert(entry==0,"normal entry"); }
				twoTipPattern(info,current,nextCell,entry,PalagoChip.Right,PalagoChip.Down);	
				break;
			case PalagoChip.right_index:
				if(entry==5) { PalagoCell nn = nextCell; nextCell=current; current=nn; entry=2; }
					else { G.Assert(entry==2,"normal entry"); }
				twoTipPattern(info,current,nextCell,entry,PalagoChip.Down,PalagoChip.Left);	
				break;
			case PalagoChip.down_index:
				if(entry==1) { PalagoCell nn = nextCell; nextCell=current; current=nn; entry=4;}
					else { G.Assert(entry==4,"normal entry"); }
				twoTipPattern(info,current,nextCell,entry,PalagoChip.Left,PalagoChip.Right);	
				break;
			default:
				break;
				
				}
			
			}
			else 
			{	// look for the squeeze pattern
				switch(top.index)
				{
				case PalagoChip.left_index:
					//normalize the entry direction
					if(entry==3) { nextCell=current.exitTo(3); entry=0; }
						else { G.Assert(entry==0,"normal entry"); }
					if(nextCell.topChip()==PalagoChip.Down)
						{squeezePattern(info,current,entry,PalagoChip.Right,PalagoChip.Down);
						}
					break;
				case PalagoChip.right_index:
					if(entry==5) { nextCell = current.exitTo(5); entry=2; }
						else { G.Assert(entry==2,"normal entry"); }
					if(nextCell.topChip()==PalagoChip.Left)
					{squeezePattern(info,current,entry,PalagoChip.Down,PalagoChip.Left);
					}
					break;
				case PalagoChip.down_index:
					if(entry==1) { nextCell=current.exitTo(1); entry=4;}
						else { G.Assert(entry==4,"normal entry"); }
					if(nextCell.topChip()==PalagoChip.Right)
					{squeezePattern(info,current,entry,PalagoChip.Left,PalagoChip.Right);
					}
					break;
				default:
					break;
					
				
				}
			}
			}
		
		}
			else if((top.blueLine==entry) || (top.blueLine==exit))
			{ info.hoop_count++;
			}
			else if((top.yellowLine==entry) || (top.yellowLine==exit))
				{
				info.hoop_count--;
				}
		}
		break;
	case lineInfo.YellowIncomplete_Op:
		{
		int mainline = top.mainLine;
		if((entry==mainline)||(exit==mainline))
		{	if((current.loopCode&(PalagoChip.yellow_loop_code|PalagoChip.partial_yellow_code))!=0)
				{ info.incomplete_count++; }
				else 
				{ info.incomplete_count--; }
		
		
		{
			PalagoCell nextCell = current.exitTo(exit);
			PalagoChip nextTop = nextCell.topChip();
			if(nextTop==top)	// 2 in a row;
			{
			switch(top.index)
			{
			case PalagoChip.left_index:
				if(entry==0) { PalagoCell nn = nextCell; nextCell=current; current=nn; entry=3; }
				else { G.Assert(entry==3,"normal entry"); }
				twoTipPattern(info,current,nextCell,entry,PalagoChip.Right,PalagoChip.Down);
				break;
			case PalagoChip.right_index:
				if(entry==2) { PalagoCell nn = nextCell; nextCell=current; current=nn; entry=5; }
				else { G.Assert(entry==5,"normal entry"); }
				twoTipPattern(info,current,nextCell,entry,PalagoChip.Down,PalagoChip.Left);	
				break;
			case PalagoChip.down_index:
				if(entry==4) { PalagoCell nn = nextCell; nextCell=current; current=nn; entry=1; }
				else { G.Assert(entry==1,"normal entry"); }
				twoTipPattern(info,current,nextCell,entry,PalagoChip.Left,PalagoChip.Right);	
				break;
			default:
				break;
				
			}}
			else
			{	// look for the squeeze pattern
				switch(top.index)
				{
				case PalagoChip.left_index:
					//normalize the entry direction
					if(entry==0) { nextCell=current.exitTo(0); entry=3; }
						else { G.Assert(entry==3,"normal entry"); }
					if(nextCell.topChip()==PalagoChip.Down)
						{squeezePattern(info,current,entry,PalagoChip.Right,PalagoChip.Down);
						}
					break;
				case PalagoChip.right_index:
					if(entry==2) { nextCell=current.exitTo(2); entry=5; }
						else { G.Assert(entry==5,"normal entry"); }
					if(nextCell.topChip()==PalagoChip.Left)
					{squeezePattern(info,current,entry,PalagoChip.Down,PalagoChip.Left);
					}
					break;
				case PalagoChip.down_index:
					if(entry==4) { nextCell = current.exitTo(4); entry=1;}
						else { G.Assert(entry==1,"normal entry"); }
					if(nextCell.topChip()==PalagoChip.Right)
					{squeezePattern(info,current,entry,PalagoChip.Left,PalagoChip.Right);
					}
					break;
				default:
					break;
					
				
				}}

			}

		}
		else if((top.blueLine==entry) || (top.blueLine==exit))
		{ info.hoop_count--;
		}
		else if((top.yellowLine==entry) || (top.yellowLine==exit))
			{
			info.hoop_count++;
			}
		
		}
		break;
	case lineInfo.YellowWin_Op:
		{
		int mainline = top.mainLine;
		if((entry==mainline)||(exit==mainline))
			{
			if(((current.loopCode&PalagoChip.yellow_loop_code)==0)
						&& !isSmallLoop(current,PalagoChip.incomplete_yellow_code,
								top.yellowLine,top.nextInLine(top.yellowLine)))
					{info.end_scan_now = true;
					 info.win_result = false;
					}
			}
		}
		break;
	case lineInfo.BlueWin_Op:
		{
		// scan a center line to determine if it is a blue win.  To be a blue win,
		// every arch that is part of the line must have it's blue corner also be
		// part of a loop.   This will be either lines that have been scanned and found
		// to be part of a loop, or lines that have not been scanned at all and are
		// part of a hole.
		int mainline = top.mainLine;
		if((entry==mainline)||(exit==mainline))		// this is an arch segment
			{
			if(((current.loopCode&PalagoChip.blue_loop_code)==0)			// blue corner is part of a loop
				&& !isSmallLoop(current,PalagoChip.incomplete_blue_code,	// blue corner is a hole
						top.blueLine,top.nextInLine(top.blueLine)))
			{info.end_scan_now = true;		// found an unconnected corner, stop processing
			 info.win_result = false;
			}
			}
		}
		break;
	case lineInfo.MapLoop_Op:
		{
		// mark the cells and roles for a known loop
		int mainline = top.mainLine;
		//int ecode = current.loopCode;
		if((entry==mainline) || (exit==mainline))
			{ current.loopCode &= ~(PalagoChip.incomplete_center_code); 
			}
		current.loopCode |= top.lineColorCode(entry);
		//G.print(""+current+" "+ecode +" "+current.loopCode);
		}
		break;
	case lineInfo.MapLine_Op:
	{
	// mark the cells and roles for a known loop
	//int ecode = current.loopCode;
	if(current.scan_clock == scan_clock)
			{ // mark only self intersecting lines
			current.loopCode |= top.partialLineColorCode(entry);
			}
	}
	break;		
	case lineInfo.Extend_Op:
		{
		// build the line extension
		int mainline = top.mainLine;
		int blueline = top.blueLine;
		int yellowline = top.yellowLine;
		info.length++;
		if((entry==mainline)||(exit==mainline)) 				// if we reached another straight segment
			{ current.loopCode |= PalagoChip.incomplete_center_code;
		      current.scan_clock = scan_clock;					// mark the curent arches
			}
		else if((entry==blueline)||(exit==blueline))
			{ current.loopCode |= PalagoChip.incomplete_blue_code;
			}
		else if((entry==yellowline)||(exit==yellowline))
			{ current.loopCode |= PalagoChip.incomplete_yellow_code;
			}
		else { throw G.Error("Not part of any line");}
		break;
		}
	default: 
		throw G.Error("Op %s not handled",op);
	}
}

// given an unscanned tile, extend it in both directions to make a complete line or loop.
lineInfo traceLineFrom(PalagoCell center)
	{	lineInfo info = new lineInfo();
		PalagoChip ctop = center.topChip();
		G.Assert(ctop!=null,"start from an occupied chip");
		scan_clock++;
		PalagoChip top = center.topChip();
		int entry = top.mainLine;
		info.traceLine(lineInfo.Extend_Op,center,entry,this);
		
		if(info.isLoop())
		{ // map the participants in the loop
		  info.traceLine(lineInfo.MapLoop_Op,center,entry,this);
		}
		else
		{ // extend an open line in the other direction
		  int next = ctop.nextInLine(ctop.mainLine);
		  info.traceLine(lineInfo.Extend_Op,center,next,this); 
		  // not used, doesn't seem to be valuable
		  info.traceLine(lineInfo.MapLine_Op,null,0,this);
		}
		return(info);
};	

// build the line and loop desctiptions for the matrix
LineInfoStack scanForLoops()
{	if(lines==null)
	{lines = new LineInfoStack();
	for(PalagoCell c = placedChips;
		c!=null;
		c = c.nextPlaced) { c.loopCode = 0; }
	for(PalagoCell c = placedChips;
		c!=null;
		c = c.nextPlaced)
	{	
		if((c.loopCode&(PalagoChip.incomplete_center_code|PalagoChip.center_loop_code))==0)	// not already scanned
		{
		lineInfo ll = traceLineFrom(c);
		lines.push(ll);
		}
	}
	}
	return(lines);
}
// examine the set of loops and look for blue wins.  This requires that
//the loops have all been found before we start examining each one to see if it is a win
boolean isWinForBlue()
{  	scanForLoops();				// find all the loops
	int sz = lines.size();
	for(int i=0;i<sz;i++)
	{	lineInfo ll = lines.elementAt(i);
		if(ll.isBlueWin(this))
		{ return(true); }
	}
	return(false);
}
//examine the set of loops and look for yellow wins.  This requires that
//the loops have all been found before we start examining each one to see if it is a win
boolean isWinForYellow()
{  scanForLoops();			// find all the loops.
	int sz = lines.size();
	for(int i=0;i<sz;i++)
	{	lineInfo ll = lines.elementAt(i);
		if(ll.isYellowWin(this)) 
			{ return(true); }
	}
	return(false);
}
// utility funtion to print the line set
void showLines()
{	scanForLoops();
	int sz = lines.size();
	for(int i=0;i<sz; i++)
	{	lineInfo ll = lines.elementAt(i);
		ll.isYellowWin(this);
		ll.isBlueWin(this);
		System.out.println(""+ll);
	}
	}
}

