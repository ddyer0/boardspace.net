package che;


import java.awt.Color;

import online.game.*;

import java.util.*;

import che.CheChip.ChipColor;
import lib.*;
import lib.Random;



/**
 * CheBoard knows all about the game of Che.
 * 
 * @author ddyer
 *
 */

class CheBoard extends infiniteRectangularBoard<CheCell> implements BoardProtocol,CheConstants
{	static final int REVISION = 101;		// 101 switches to an infinite board
	public int getMaxRevisionLevel() { return(REVISION); }

    static int BOARDCOLUMNS = 19;
    static int BOARDROWS = 19;
    static final String[] CheGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	CheState unresign;
	CheState board_state;
	public CheState getState() {return(board_state); }
	public void setState(CheState st) 
	{ 	unresign = (st==CheState.RESIGN_STATE)?board_state:null;
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
	public CellStack occupiedCells = new CellStack();
    public int chips_on_board = 0;			// number of chips currently on the board
    private int sweep_counter = 0;
    private boolean blobs_valid[]=new boolean[2];
    private boolean blob_winner[]=new boolean[2];
    public int max_blob_size[]= new int[2];
    private int number_of_blobs[] = new int[2];
    private int blob_extensions[] = new int[2];
    private int blobs_with_1[] = new int[2];
    private int blobs_with_2[] = new int[2];
    private int blobs_with_3[] = new int[2];
    private int blobs_with_4[] = new int[2];
    private StateStack robotState = new StateStack();
    public int robotDepth = 0;
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public CheChip pickedObject = null;
    public CheChip lastPicked = null;
    public CheCell chipPool[] = new CheCell[CheChip.nChips];	// dummy source for the chip pools
    public CheCell pickedSourceStack[] = new CheCell[3];
    public CheCell droppedDestStack[] = new CheCell[3];
    public CheState undoState[] = new CheState[3];
    private ChipColor playerDotColor[] = new ChipColor[2];
    public int stackIndex = 0;
    public CheChip lastDropped = null;	// for image adjustment logic

    
    public Hashtable<CheCell,CheCell> movingObjectDests()
    {	int n = 0;
    	Hashtable<CheCell,CheCell> h = new Hashtable<CheCell,CheCell>();
    	if(pickedObject!=null)
    	{	for(int i=0;i<chips_on_board;i++)
    		{ CheCell c =  GetOccupiedCell(i);	
    			for(int dir = CELL_LEFT;dir<CELL_FULL_TURN+CELL_LEFT;dir+=CELL_QUARTER_TURN)
    			{	CheCell d = c.exitTo(dir);
    				if((d!=null) && (d.height()==0))
    				{	n++;
    					h.put(d,d);
    				}
    			}
    		}
    		if(n==0) 
    			{ CheCell c = getCell((char)('A'+ncols/2),nrows/4);
    			  h.put(c,c);
    			}
    	}
    	return(h);
    }
	// factory method to generate a board cell
	public CheCell newcell(char c,int r)
	{	return(new CheCell(c,r));
	}
    public CheBoard(String init,int map[]) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = CheGRIDSTYLE;
        revision = REVISION;
        setColorMap(map, 2);
        doInit(init,0L); // do the initialization 
    }
    public CheCell GetOccupiedCell(int i) { return(occupiedCells.elementAt(i)); }
    
    final void SetMatchingCell(char col,int row,int idx)
    {	CheCell c = getCell(col,row);
    	SetMatchingCell(c,idx);
    }
    final void SetMatchingCell(CheCell c,int idx)
    {
    	if(!c.chipColorMatches(CheChip.getChip(idx))) { idx ^=1; }	// flip the chip
    	CheChip chip = CheChip.getChip(idx);
    	lastDropped = chip;
    	SetBoard(c,chip);
    }
    private void clearBlobInfo(int i)
    {	max_blob_size[i]=0;
       	number_of_blobs[i]=0;
       	blob_extensions[i]=0;
       	blob_winner[i]=false;
       	blobs_valid[i]=false;
       	blobs_with_1[i]=0;
       	blobs_with_2[i]=0;
       	blobs_with_3[i]=0;
       	blobs_with_4[i]=0;
     }

    public void sameboard(BoardProtocol f) { sameboard((CheBoard)f); }
    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(CheBoard from_b)
    {
        super.sameboard(from_b); // compares the boards
        
        // here, check any other state of the board to see if
        G.Assert((chips_on_board == from_b.chips_on_board),"chip count matches");
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
      
        v^= (board_state.ordinal()*10+whoseTurn)*r.nextLong(); 
        v^= Digest(r,pickedObject);
        return (v);
    }
    public CheBoard cloneBoard() 
    	{ CheBoard copy = new CheBoard(gametype,getColorMap());
    	  copy.copyFrom(this); 
    	  return(copy);
    	}
    public void copyFrom(BoardProtocol b) { copyFrom((CheBoard)b); }

    public CheCell getCell(CheCell c)
    { 	if(c==null) { return null; }
    	switch(c.rackLocation())
    	{
    	case BoardLocation:
    	case EmptyBoard:
    		return getCell(c.col,c.row);
    	case ChipPool0:	return chipPool[0];
    	case ChipPool1: return chipPool[1];
    	case ChipPool2: return chipPool[2];
    	case ChipPool3: return chipPool[3];
    	default: G.Error("Not expecting %s",c.rackLocation());
    	}
    	
    	return(c==null ? null : getCell(c.col,c.row)); }
    
    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(CheBoard from_b)
    {	super.copyFrom(from_b);
        chips_on_board = from_b.chips_on_board;
        for(int i=0;i<chips_on_board;i++) 
        	{ CheCell ch = from_b.GetOccupiedCell(i);
        	  occupiedCells.push(getCell(ch));
        	}
        getCell(chipPool,from_b.chipPool);
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        stackIndex = from_b.stackIndex;
        pickedObject = from_b.pickedObject;
        lastPicked = null;
        robotDepth = from_b.robotDepth;
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
    public void doInit(String gtype,long rv)
    {  StringTokenizer tok = new StringTokenizer(gtype);
	   String typ = tok.nextToken();
	   int np = tok.hasMoreTokens() ? G.IntToken(tok) : players_in_game;
	   long ran = tok.hasMoreTokens() ? G.IntToken(tok) : rv;
	   int rev = tok.hasMoreTokens() ? G.IntToken(tok) : revision;
	   
	   doInit(typ,np,ran,rev);
    }
    public void doInit(String typ,int np,long ran,int rev)
    {
    	randomKey = rev;
    	if(Che_INIT.equalsIgnoreCase(typ)) {  reInitBoard(BOARDCOLUMNS,BOARDROWS);  }
    	else { throw G.Error(WrongInitError,typ); }
        gametype = typ;
        adjustRevision(rev);
        G.Assert(np==2,"players can only be 2");
        setIsTorus(revision<101);
        setState(CheState.PUZZLE_STATE);
        
        int map[]=getColorMap();
        playerDotColor[map[0]] = ChipColor.light;
        playerDotColor[map[1]] = ChipColor.dark;
        
        whoseTurn = FIRST_PLAYER_INDEX;
        chips_on_board = 0;
        stackIndex = 0;
        clearBlobInfo(FIRST_PLAYER_INDEX);
        clearBlobInfo(SECOND_PLAYER_INDEX);
        occupiedCells.clear();
        AR.setValue(pickedSourceStack,null);
        AR.setValue(droppedDestStack, null);
        AR.setValue(undoState,null);

        lastDropped = null;
        robotState.clear();
        robotDepth = 0;
        Random r = new Random(3585867);
	    for(int i=0;i<CheChip.nChips;i++)
	       {	chipPool[i]=new CheCell(r,ChipPool[i],CheChip.getChip(i));
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

    public boolean WinForPlayerNow(int player)
    {	if(win[player]) { return(true); }
    	return(findBlobs(player));
     }



    
    // set the contents of a cell, and maintian the books
    public CheChip SetBoard(CheCell c,CheChip ch)
    {	CheChip old = c.topChip();
    	G.Assert(c.onBoard,"cell on the board");
    	if(ch!=old)
    	{
    	if(old!=null) { chips_on_board--; if(ch==null) { occupiedCells.remove(c,false); }}
     	if(ch!=null) { chips_on_board++; if(old==null) { occupiedCells.push(c); }}
       	c.chip = ch;
       	blobs_valid[0]=blobs_valid[1]=false;
       	if(ch!=null) { c.cellName = ""+chips_on_board; } else { c.cellName=""; }
    	}
    	return(old);
    }
    //
    // accept the current placements as permanant
    //
    public void acceptPlacement()
    {	while(stackIndex>=0)
    	{
    	droppedDestStack[stackIndex]=pickedSourceStack[stackIndex]=null;
    	undoState[stackIndex]=null;
    	stackIndex--;
    	}
    	stackIndex = 0;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {	if((stackIndex>0)&&(pickedSourceStack[stackIndex]==null))
    	{stackIndex--;
    	 CheCell c = droppedDestStack[stackIndex];
    	if(c!=null)
    	{	pickedObject = SetBoard(c,null);
    		setState(undoState[stackIndex]);
    		droppedDestStack[stackIndex]=null;
     	}}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	CheCell c = pickedSourceStack[stackIndex];
    	if((c!=null) && c.onBoard) 
    		{
    		SetBoard(c,pickedObject);
    		}
    		pickedSourceStack[stackIndex]=null;
    		pickedObject = null;
    }

    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(CheCell c)
    {	return((stackIndex>0) && (droppedDestStack[stackIndex-1]==c));
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    { CheChip c = pickedObject;
    	if(c!=null)
    	{	return(c.chipNumber()); 
    	}
      	return (NothingMoving);
    }

    private void pickBoard(CheCell c)
    {	boolean wasDest = isDest(c);
        if(wasDest) { unDropObject(); }
        else
        {
          pickedSourceStack[stackIndex] = c;
          lastPicked = pickedObject = c.topChip();
          droppedDestStack[stackIndex] = null;
          undoState[stackIndex]=null;
          SetBoard(c,null);
        }
    }
    private void dropBoard(CheCell c)
    {	SetMatchingCell(c,pickedObject.index);
    	droppedDestStack[stackIndex]=c;
    	undoState[stackIndex]=board_state;
    	stackIndex++;
    	pickedObject = null;
    }
    private void pickChip(int idx)
    {	
    	CheCell src = chipPool[idx];
    	lastPicked = pickedObject = src.topChip();
    	pickedSourceStack[stackIndex] = src;
     }
    private void dropChip(int idx)
    {	
     	pickedSourceStack[idx]=null;
    	lastPicked = pickedObject = null;
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(CheCell c)
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
        	setNextStateAfterDone();
     	
        	break;
        case PLAY_STATE:
        	setState(doEndgame()?CheState.CONFIRM_STATE:CheState.PLAY2_STATE);
        	break;
        case PLAY2_STATE:
        case FIRST_PLAY_STATE:
			setState(CheState.CONFIRM_STATE);
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
    	case CONFIRM_STATE:
    	case PUZZLE_STATE:
    		setState((chips_on_board==0)?CheState.FIRST_PLAY_STATE:CheState.PLAY_STATE);
    		break;
    	}

    }
    public boolean doEndgame()
    {
    	int nextp =nextPlayer[whoseTurn];
    	boolean winNext = WinForPlayerNow(nextp);
    	boolean winCurrent = WinForPlayerNow(whoseTurn);
    	boolean end = !(winNext || winCurrent) && (chips_on_board>=CHIPS_IN_GAME);
    	WinForPlayerNow(nextp);
    	if(winNext || (end && (max_blob_size[nextp]>max_blob_size[whoseTurn])))
    		{ win[nextp]=true; 
    		  return(true);
    		}
    	else if(winCurrent || (end && (max_blob_size[whoseTurn]>max_blob_size[nextp])))
    		{ win[whoseTurn]=true; 
    		  return(true);
    		}	
    	return(false);
    }
    private void doDone()
    {
        acceptPlacement();

        if (board_state==CheState.RESIGN_STATE)
        {
            win[nextPlayer[whoseTurn]] = true;
            setState(CheState.GAMEOVER_STATE);
        }
        else
        {	if (doEndgame())
        	{    
       		setState(CheState.GAMEOVER_STATE); 
        	} 
         	else
        	{	setNextPlayer();
        		setNextStateAfterDone();
        	}
        }
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	Chemovespec m = (Chemovespec)mm;

        //System.out.println("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
       case MOVE_DONE:

         	doDone();

            break;
        case MOVE_ROTATE:
        case MOVE_DROPB:
        	{
        	CheCell c = getCell(m.to_col,m.to_row);
			if(isDest(c)) 
				{ unDropObject(); 
				}
			pickChip(m.object);
			dropBoard(c);
			createExitCells(c);
            setNextStateAfterDrop();
        	}
            break;

        case MOVE_PICK:
            if(pickedObject!=null) 
            	{     unPickObject();
            	}
            pickChip(m.object);
            break;
        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	{
        	CheCell c = getCell(m.to_col,m.to_row);
        	pickBoard(c); 
         	}
            break;

        case MOVE_DROP: // drop on chip pool;
            dropChip(stackIndex);
            //setNextStateAfterDrop();

            break;


 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            int nextp = nextPlayer[whoseTurn];
            setState(CheState.PUZZLE_STATE);
            if((win[whoseTurn]=WinForPlayerNow(whoseTurn))
               ||(win[nextp]=WinForPlayerNow(nextp)))
               	{ setState(CheState.GAMEOVER_STATE); 
               	}
            else {  setNextStateAfterDone(); }

            break;

       case MOVE_RESIGN:
            setState(unresign==null?CheState.RESIGN_STATE:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(CheState.PUZZLE_STATE);

            break;

       case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(CheState.GAMEOVER_STATE);
       	   break;
        default:
        	cantExecute(m);
        }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+board_state);

        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case CONFIRM_STATE:
        	return(pickedObject!=null);
        case FIRST_PLAY_STATE:
        case PLAY_STATE:
        case PLAY2_STATE:
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
	public boolean hasNeighbors(CheCell c)
	{	for(int dir=CELL_LEFT;  dir<CELL_FULL_TURN; dir+=CELL_QUARTER_TURN)
		{	CheCell adj = c.exitTo(dir);
			if((adj!=null) && (adj.chip!=null)) { return(true); }
		}
		return(false);
	}

    public boolean LegalToHitBoard(CheCell c)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
		case FIRST_PLAY_STATE:
			return((pickedObject==null)?true:(c.chip==null));
		case PLAY_STATE:
		case PLAY2_STATE:
			return((pickedObject==null)
					?(isDest(c)
							?true
							:((c.chip==null) && hasNeighbors(c)))
					:((c.chip==null)&& hasNeighbors(c)));
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
			return(isDest(c));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case PUZZLE_STATE:
            return ((pickedObject!=null) 
            		  ? (c.chip==null)
            		  : (chips_on_board<=1)||hasNeighbors(c));
        }
    }
    
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Chemovespec m)
    {	robotDepth++;
        robotState.push(board_state); //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        //G.print("E "+m);
        if (Execute(m,replayMode.Replay))
        {
            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {	acceptPlacement();
                doDone();
            }
            else if(board_state==CheState.PLAY2_STATE) { acceptPlacement(); }
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
    public void UnExecute(Chemovespec m)
    {
        //G.print("U "+m+" for "+whoseTurn);
    	robotDepth--;
        switch (m.op)
        {
   	    default:
   	    	cantExecute(m);
        	break;
       case MOVE_DONE:
            break;
            
        case MOVE_DROPB:
        	SetBoard(getCell(m.to_col,m.to_row),null);
        	acceptPlacement();
        	break;
        case MOVE_RESIGN:

            break;
        }

        setState(robotState.pop());
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
  
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();

 	switch(board_state)
 	{
 	case FIRST_PLAY_STATE:
 		{
 		CheCell c = getCell('J',10);
 		all.addElement(new Chemovespec(MOVE_DROPB,c.col,c.row,0,whoseTurn));
 		all.addElement(new Chemovespec(MOVE_DROPB,c.col,c.row,2,whoseTurn));
 		}
 		break;
 	case PLAY_STATE:
 	case PLAY2_STATE:
 		{
 		int dirlim = CELL_LEFT+8;
 		sweep_counter++;
 		for(int i=0,lim=occupiedCells.size();i<lim;i++)
 		{	CheCell c = occupiedCells.elementAt(i);
 			for(int dir=CELL_LEFT; dir<dirlim;dir+=2)
 			{
 			CheCell adj = c.exitTo(dir);
 			if((adj!=null) && (adj.chip==null) && adj.sweep_counter!=sweep_counter)
 			{
 			adj.sweep_counter = sweep_counter;
 			for(int idx=0;idx<4;idx++)
 			{
 				if(adj.chipColorMatches(CheChip.getChip(idx)))
 				{ all.addElement(new Chemovespec(MOVE_DROPB,adj.col,adj.row,idx,whoseTurn));
 				}
 			}
 			}}}
 		}
 		break;
	default:
		break;
 		
 	}
 	G.Assert(all.size()>1,"has moves");
 	return(all);
 }
 
 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(Character.isDigit(txt.charAt(0)))
	 	{ xpos-=2;
	 	}
 	GC.Text(gc, false, xpos-2, ypos, -1, 0,clt, null, txt);
 }

private int blob_size = 0;
private int blob_ext = 0;
private int blob_ear_size = 0;
private int blob_jaw_size = 0;
private boolean blob_open = false;


// sweep for a closed bubble of the player's color
// which we dentify by a group of 4 cells dominated
// by the ooposite color.  These four have to have
// a bubble in the central position.
private boolean sweepForBubble(CheCell c,int forplayer)
{	CheChip top = c.topChip();
	if(top.dotColor!=playerDotColor[forplayer])
	{	// check for a simple bubble win
		int present = 0;
		CheChip diag=null;
		if(connectsInDirection(top,CELL_UP_RIGHT))
		{
		CheChip right = c.exitTo(CELL_RIGHT).topChip();
			if(right!=null) { present++; if (right.dotColor==playerDotColor[forplayer]) { return(false); }}
		CheChip down = c.exitTo(CELL_DOWN).topChip();
			if(down!=null) { present++; if (down.dotColor==playerDotColor[forplayer]) { return(false); }}
		diag = c.exitTo(CELL_DOWN_RIGHT).topChip();
			if(diag!=null) {present++; if (diag.dotColor==playerDotColor[forplayer]) { return(false); }}
		}
		else
		{
			CheChip down = c.exitTo(CELL_DOWN).topChip();
				if(down!=null) { present++; if (down.dotColor==playerDotColor[forplayer]) { return(false); }}
			diag = c.exitTo(CELL_DOWN_LEFT).topChip();
				if(diag!=null) {present++; if (diag.dotColor==playerDotColor[forplayer]) { return(false); }}
			
		}
		switch(present)
		{
		case 0:	break;
		case 1: if(diag!=null) { break; } else { blob_ear_size++; break; }
		case 2: blob_jaw_size++; break;
		case 3: return(true);
		default:
			break;
		}
	
		}
		return(false); 

}
//
// sweep for big blobs which contain a cell dominated by the specified color.
//
private void sweepForBlob(CheCell c,int forplayer)
{	if(c.sweep_counter==sweep_counter) { return; }
	// it's the desired dominant color, we need to count the blob size and note if the blob is closed.
	CheChip top = c.topChip();
	blob_size++;
	c.sweep_counter = sweep_counter;
	for(int dir=0;dir<CELL_FULL_TURN;dir++)
		{	if(connectsInDirection(top,dir))
			{ CheCell adj = c.exitTo(dir);
			  CheChip adjtop = adj.topChip();
			  //if(forplayer==0) { G.print(""+dir+":"+c+top+" connect to "+adj+adjtop); }
			  if(adjtop==null)  
			  	{ blob_open = true;
			  	  blob_ext++;
			  	}
			  else if(adjtop.dotColor==playerDotColor[forplayer]) { sweepForBlob(adj,forplayer); }
			}
		}
	return;
}
//
// find any closed blob for the specified player, and as a side effect,
// find the size of the largest area (and maybe other stats in the future)
//
public boolean findBlobs(int forPlayer)
{	boolean winner = false;
	if(blobs_valid[forPlayer]) 
	{ winner=blob_winner[forPlayer];
	}
	else
	{sweep_counter++;
	int max_size = 0;
	clearBlobInfo(forPlayer);
	blob_ear_size = 0;
	blob_jaw_size = 0;

	for(int i=0;i<chips_on_board;i++)
	{
	CheCell center = GetOccupiedCell(i);
	CheChip top = center.topChip();
	if(sweepForBubble(center,forPlayer))
	{ return(true);	// win
	}
	if(center.sweep_counter!=sweep_counter)
	{	if(top.dotColor==playerDotColor[forPlayer])
		{
		blob_size = 0;
		blob_open = false;
		blob_ext = 0;
		
		sweepForBlob(center,forPlayer);
		if(!blob_open) 
			{ winner = true; }
		max_size = Math.max(max_size,blob_size);
		number_of_blobs[forPlayer]++;
		blob_extensions[forPlayer]+=blob_ext;
		switch(blob_ext)
			{	default: break;
			case 1:	blobs_with_1[forPlayer]++; break;
			case 2: blobs_with_2[forPlayer]++; break;
			case 3: blobs_with_3[forPlayer]++; break;
			case 4: blobs_with_4[forPlayer]++; break;
			}
		}}

	}
	max_blob_size[forPlayer]=max_size;
	blob_winner[forPlayer]=winner;
	blobs_with_1[forPlayer]+=blob_jaw_size;
	blobs_with_2[forPlayer]+=blob_ear_size;
	blobs_valid[forPlayer]=true;
	}

	return(winner);
}
double scoreForPlayer(int player)
{	double val = 0.0;
	findBlobs(player);
	val += max_blob_size[player];
	if(number_of_blobs[player]>0)
	{	val -= 10.0*blob_extensions[player]/number_of_blobs[player];
	}
	val += blobs_with_1[player]*100;
	val += blobs_with_2[player]*40;
	val += blobs_with_3[player]*20;
	val += blobs_with_4[player]*10;
	return(val);
}

static final int colorInDirection(CheChip chip,int dir)
{	int colorInfo[] = chip.colorInfo;
	if(colorInfo!=null)
	{	dir = (dir+8)&7;
		switch(dir)
		{	
		default: throw G.Error("Bad direction"); 
		case CELL_LEFT: return((colorInfo[3]<<2) | colorInfo[0]);
		case CELL_UP: return((colorInfo[0]<<2) | colorInfo[1]);
		case CELL_RIGHT: return((colorInfo[1]<<2) | colorInfo[2]);
		case CELL_DOWN: return((colorInfo[2]<<2) | colorInfo[3]);
		}
	}
	return((CheChip.N<<2) | CheChip.N);
}
static final boolean connectsInDirection(CheChip chip,int dir)
{	int colorInfo[] = chip.colorInfo;
	if(colorInfo!=null)
	{
	dir = (dir+8)&7;
	switch(dir)
	{	
		// Orthogonal directions always connect. This differs from 
		// DashChip which didn't allow connections in these directions 
	default: return(true);		
	case CELL_DOWN_LEFT: 
		boolean as1 = colorInfo[1]==colorInfo[4];
		return(as1);
	case CELL_UP_LEFT: 
		boolean as2 = colorInfo[2]==colorInfo[4];
		return(as2);
	case CELL_UP_RIGHT: 
		boolean as3 = colorInfo[3]==colorInfo[4];
		return(as3);
	case CELL_DOWN_RIGHT: 
		boolean as4 = colorInfo[0]==colorInfo[4];
		return(as4);
	}
	}
	return(false);
}
final static int colorInInverseDirection(CheChip chip,int dir)
{	int colorInfo[] = chip.colorInfo;
	if(colorInfo!=null)
	{	dir = (dir+8)&7;
		switch(dir)
		{	
		case CELL_LEFT: return((colorInfo[0]<<2) | colorInfo[3]);
		case CELL_UP: return((colorInfo[1]<<2) | colorInfo[0]);
		case CELL_RIGHT: return((colorInfo[2]<<2) | colorInfo[1]);
		case CELL_DOWN: return((colorInfo[3]<<2) | colorInfo[2]);
		default:
			break;
		}
	}
	return((CheChip.N<<2) | CheChip.N);
}
}
