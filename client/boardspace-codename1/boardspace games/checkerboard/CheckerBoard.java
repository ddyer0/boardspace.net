package checkerboard;
/**
 * TODO: needs to be more willing to accept draws
 */
import online.game.*;

import java.util.*;

import checkerboard.CheckerConstants.CheckerId;
import checkerboard.CheckerConstants.CheckerState;
import checkerboard.CheckerConstants.StateStack;
import checkerboard.CheckerConstants.Variation;
import lib.*;
import lib.Random;
import static checkerboard.CheckerMovespec.*;
/**
 * CheckerBoard knows all about the game of Checkers.
 * It gets a lot of logistic support from game.rectBoard, 
 * which knows about the coordinate system.  
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

class CheckerBoard extends rectBoard<CheckerCell> implements BoardProtocol
{	
	/** revision numbers are used to avoid incompatibility when small rule changes
	 * or bug fixes alter the game play.  Its fair to assume that in any particular
	 * game, all players are using the same revision, but when a "modern" build is
	 * replaying an "old" version of the game, revision numbers help keep the old
	 * games playing the same as always.
	 */
	static int REVISION = 100;			// revision numbers start at 100
    static final int White_Chip_Index = 0;
    static final int Black_Chip_Index = 1;
    static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
    static final CheckerId RackLocation[] = { CheckerId.White_Chip_Pool,CheckerId.Black_Chip_Pool};
	public int getMaxRevisionLevel() { return(REVISION); }
	
    public void SetDrawState() { setState(CheckerState.Draw); }
    
    public CheckerCell rack[] = null;	// the pool of chips for each player.  
    public int kingRow[] = {0,0};		// will be the row checkers get promoted to king
    public CellStack animationStack = new CellStack();
    //
    // private variables
    //
    private RepeatedPositions repeatedPositions = null;		// shared with the viewer
    private CheckerState board_state = CheckerState.Play;	// the current board state
    private CheckerState unresign = null;					// remembers the previous state when "resign"
    Variation variation = Variation.Checkers_10;
    public CheckerState getState() { return(board_state); } 
	public void setState(CheckerState st) 
	{ 	unresign = (st==CheckerState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
    private CheckerId playerColor[]={CheckerId.White_Chip_Pool,CheckerId.Black_Chip_Pool};
 	public CheckerId getPlayerColor(int p) { return(playerColor[p]); }
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public CheckerChip pickedObject = null;
    public CheckerChip lastDropped = null;
    private IStack pickedHeight = new IStack();		// the height that was picked, ie 1=single 2=king
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    private StateStack dropState = new StateStack();
    private CellStack captureStack = new CellStack();
    private boolean madeKing = false;
    public CheckerCell lastDest[] = {null,null};				// last spot the opponent dropped, for the UI
    public CheckerCell currentDest = null;
    private int initialStacks[] = {0,0};			// remembers the number of starting pieces
    private IStack captureHeight = new IStack();	// the height that was captured, ie 1=single 2=king
    boolean captureKing = false;
    int lastProgressMove = 0;		// last move where a pawn was advanced
    int lastDrawMove = 0;			// last move where a draw was offered
    int robotDepth = 0;		// current depth of robot search.  This is used to make faster wins look better
    						// than slower wins.  It's part of the board so multiple threads have independent values.
  	private StateStack robotState = new StateStack();
  	private CellStack robotLast = new CellStack();
  	private IStack robotCapture = new IStack();
  	private IStack robotKing = new IStack();
  	
  	int previousLastPlaced = 0;
  	int previousLastEmptied = 0;
  	CheckerChip previousLastContents = null;
  	int lastPlacedIndex = 0;
  	
     CellStack occupiedCells[] = new CellStack[2];	// cells occupied, per color
    
    private int ForwardDiagonals[][] = { { CELL_DOWN_LEFT,CELL_DOWN_RIGHT}, { CELL_UP_LEFT,CELL_UP_RIGHT}};
    private int AllDiagonals[][] ={ { CELL_DOWN_LEFT,CELL_DOWN_RIGHT, CELL_UP_LEFT,CELL_UP_RIGHT},
    		{ CELL_DOWN_LEFT,CELL_DOWN_RIGHT, CELL_UP_LEFT,CELL_UP_RIGHT}};
    //
    // directions for turkish checkers
    //
    private int AllOrthogonals[][] = {{CELL_UP,CELL_RIGHT,CELL_DOWN,CELL_LEFT},{CELL_UP,CELL_RIGHT,CELL_DOWN,CELL_LEFT}};
    private int ForwardOrthogonals[][] = {{CELL_RIGHT,CELL_DOWN,CELL_LEFT},{CELL_UP,CELL_RIGHT,CELL_LEFT}};
   
	// factory method to create a new cell as part of the board
	public CheckerCell newcell(char c,int r)
	{	return(new CheckerCell(c,r));
	}
    public CheckerBoard(String init,long rv,int np,RepeatedPositions rep,int map[],int rev) // default constructor
    {   repeatedPositions = rep;

    	setColorMap(map, np);
        doInit(init,rv,np,rev); // do the initialization 
        autoReverseY();		// reverse_y based on the color map
     }


	public void sameboard(BoardProtocol f) { sameboard((CheckerBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if {@link #clone},{@link #Digest} and {@link #sameboard} methods agree.
     * @param from_b
     */
    public void sameboard(CheckerBoard from_b)
    {
    	super.sameboard(from_b);	// calls sameCell for each cell, also for inherited class variables.
    	G.Assert(unresign==from_b.unresign,"unresign mismatch");
       	G.Assert(AR.sameArrayContents(win,from_b.win),"win array contents match");
       	G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor contents match");
       	G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
       	G.Assert(sameContents(pickedHeight,from_b.pickedHeight),"pickedHeight mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(dropState.sameContents(from_b.dropState),"dropState mismatch");
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject doesn't match");
        G.Assert(occupiedCells[FIRST_PLAYER_INDEX].size()==from_b.occupiedCells[FIRST_PLAYER_INDEX].size(),"occupiedCells mismatch");
        G.Assert(occupiedCells[SECOND_PLAYER_INDEX].size()==from_b.occupiedCells[SECOND_PLAYER_INDEX].size(),"occupiedCells mismatch");
        G.Assert(sameCells(currentDest,from_b.currentDest),"currentDest mismatch");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Digest matches");

    }

    /** 
     * Digest produces a 64 bit hash of the game state.  This is used in many different
     * ways to identify "same" board states.  Some are germane to the ordinary operation
     * of the game, others are for system record keeping use; so it is important that the
     * game Digest be consistent both within a game and between different games played
     * over a long period of time. 
     * (1) Digest is used by the default implementation of {@link #EditHistory} to remove moves
     * that have returned the game to a previous state; ie when you undo a move or
     * hit the reset button.  
     * (2) Digest is used after EditHistory to verify that replaying the history results
     * in the same game as the user is looking at.  This catches errors in implementing
     * undo, reset, and EditHistory
	 * (3) Digest is used debugging the standard robot search to verify that move/unmove 
	 * returns to the same board state, also that move/move/unmove/unmove etc.
	 * (4) Digests are also used as the game is played to look for draw by repetition.  The state
     * after most moves is recorded in a {@link online.game.repeatedPositions} table, and duplicates/triplicates are noted.
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

		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,revision);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,pickedHeight);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,captureStack);
		v ^= Digest(r,captureHeight);
		v ^= Digest(r,occupiedCells[SECOND_PLAYER_INDEX].size());	// not completely specific because the stack can be shuffled
		v ^= Digest(r,occupiedCells[FIRST_PLAYER_INDEX].size());
		v ^= Digest(r,currentDest);
		v ^= Digest(r,board_state);
		v ^= Digest(r,whoseTurn);
        return (v);
    }
   public CheckerBoard cloneBoard() 
	{ CheckerBoard copy = new CheckerBoard(gametype,randomKey,players_in_game,repeatedPositions,getColorMap(),revision);
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((CheckerBoard)b); }


    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(CheckerBoard from_b)
    {	
        super.copyFrom(from_b);			// copies the standard game cells in allCells list
        pickedObject = from_b.pickedObject;	
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        pickedHeight.copyFrom(from_b.pickedHeight);
        getCell(droppedDestStack,from_b.droppedDestStack);
        dropState.copyFrom(from_b.dropState);
        getCell(captureStack,from_b.captureStack);
        captureHeight.copyFrom(from_b.captureHeight);
        getCell(occupiedCells,from_b.occupiedCells);
        AR.copy(playerColor,from_b.playerColor);
        board_state = from_b.board_state;
        lastProgressMove = from_b.lastProgressMove;
        lastDrawMove = from_b.lastDrawMove;
        getCell(lastDest,from_b.lastDest);
        currentDest = getCell(from_b.currentDest);
        unresign = from_b.unresign;
        repeatedPositions = from_b.repeatedPositions;
        copyFrom(rack,from_b.rack);
        lastPlacedIndex = from_b.lastPlacedIndex;
        previousLastPlaced = from_b.previousLastPlaced;
        previousLastEmptied = from_b.previousLastEmptied;
        previousLastContents = from_b.previousLastContents;
        sameboard(from_b);
    }
    public void doInit(String gtype,long rv)
    {
    	doInit(gtype,rv,players_in_game,revision);
    }
    private int playerIndex(CheckerChip ch)
    {	int []map = getColorMap();
    	if(ch==CheckerChip.black) { return(map[SECOND_PLAYER_INDEX]);}
    	else if(ch==CheckerChip.white) { return(map[FIRST_PLAYER_INDEX]); }
    	else { return(-1); }
    }
    public CheckerChip playerChip(int pl)
    {
    	return(rack[pl].topChip());
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long rv,int np,int rev)
    {  	drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
		adjustRevision(rev);
    	Grid_Style = GRIDSTYLE; //coordinates left and bottom
    	randomKey = rv;
    	players_in_game = np;
    	currentDest = null;
		rack = new CheckerCell[2];
    	Random r = new Random(67246765);
    	int map[]=getColorMap();
     	for(int i=0,pl=FIRST_PLAYER_INDEX;i<2; i++,pl=pl^1)
    	{
     	occupiedCells[i] = new CellStack();
       	CheckerCell cell = new CheckerCell(r);
       	cell.rackLocation=RackLocation[i];
       	cell.addChip(CheckerChip.getChip(i));
     	rack[map[i]]=cell;
     	}    
     	
     	variation = Variation.findVariation(gtype);
     	switch(variation)
     	{
     	default:  throw G.Error(WrongInitError,gtype);
     	case Checkers_Turkish:
     	case Checkers_International:
     	case Checkers_American:
     	case Checkers_10:
     	case Checkers_8:
     	case Checkers_6:
      		reInitBoard(variation.size,variation.size);
     		kingRow[map[FIRST_PLAYER_INDEX]] = 1;
     		kingRow[map[SECOND_PLAYER_INDEX]] = nrows;
     		gametype = gtype;
     		break;
     	}

	    setState(CheckerState.Puzzle);
	    //
	    // fill the board with the background tiles. This checkers implementation
	    // is unusual, in that the squares of the board are actually chips on it.
	    // This allows for boards where the squares can be removed from play (as in
	    // the LOAP variation) or changed in the course of the play (as in Truchet)
	    //
	    for(CheckerCell c = allCells; c!=null; c=c.next)
	    {  int i = (c.row+c.col)%2;
	       c.addChip(CheckerChip.getTile(i^1));
	    }
	    int nRows = 3;
	    switch(variation)
	    {
	    default: break;
	    case Checkers_International:
	    	nRows++;
			//$FALL-THROUGH$
		case Checkers_American:
	    	// standard pattern 
	    	for(int row=1;row<=nRows;row++)
	    	{
	    		for(int coln=1+1-(row&1);coln<=ncols;coln+=2)
	    		{	CheckerCell c1 = getCell((char)('A'+coln-1),row);
	    			c1.addChip(CheckerChip.black);
	    			occupiedCells[playerIndex(CheckerChip.black)].push(c1);
	    			CheckerCell c2 = getCell((char)('A'+ncols-coln),nrows-row+1);
	    			c2.addChip(CheckerChip.white);
	    	      	occupiedCells[playerIndex(CheckerChip.white)].push(c2);
    		}
	    		
	    	}
	    	break;
	    case Checkers_Turkish:
	    	// rows 2 and 3 filled
	    	for(int row=2;row<=3;row++)
	    	{
	    		for(int coln=1;coln<=ncols;coln++)
	    		{	CheckerCell c1 = getCell((char)('A'+coln-1),row);
	    			c1.addChip(CheckerChip.black);
	    			occupiedCells[playerIndex(CheckerChip.black)].push(c1);
	    			
	    			CheckerCell c2 = getCell((char)('A'+ncols-coln),nrows-row+1);
	    			c2.addChip(CheckerChip.white);
	    			occupiedCells[playerIndex(CheckerChip.white)].push(c2);
	    		}
	    		
	    	}
	    }
	    lastProgressMove = 0;
	    lastDrawMove = 0;
	    robotDepth = 0;
	    lastPlacedIndex = 1;
	    robotState.clear();
	    robotLast.clear();
	    robotCapture.clear();
	    robotKing.clear();
	    whoseTurn = FIRST_PLAYER_INDEX;
		playerColor[map[FIRST_PLAYER_INDEX]]=CheckerId.White_Chip_Pool;
		playerColor[map[SECOND_PLAYER_INDEX]]=CheckerId.Black_Chip_Pool;
		initialStacks[FIRST_PLAYER_INDEX] = occupiedCells[FIRST_PLAYER_INDEX].size();
		initialStacks[SECOND_PLAYER_INDEX] = occupiedCells[SECOND_PLAYER_INDEX].size();
		captureKing = false;	// special bit for the animations
		acceptPlacement();
        AR.setValue(win,false);
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }

    public double simpleScore(int who)
    {	// range is 0.0 to 0.8
    	return((0.8*occupiedCells[who].size())/initialStacks[who]);
    }
    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer(replayMode replay)
    {
        switch (board_state)
        {
        case CaptureMore:
        	if(replay!=replayMode.Live)
        	{	// some damaged games need this
        		moveNumber++; //the move is complete in these states
                setWhoseTurn(whoseTurn^1);
                setState(CheckerState.Confirm);
                break;
        	}
			//$FALL-THROUGH$
		case Gameover:
        	// need by some damaged games.
        	if(replay!=replayMode.Live) { break; }
			//$FALL-THROUGH$
		default:
        	throw G.Error("Move not complete, can't change the current player %s",board_state);
        case Puzzle:
            break;
        case Confirm:
        case Draw:
        case AcceptPending:
        case DeclinePending:
        case DrawPending:
        case Resign:
            moveNumber++; //the move is complete in these states
            setWhoseTurn(whoseTurn^1);
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
         case DrawPending:
         case AcceptPending:
         case DeclinePending:
            return (true);

        default:
            return (false);
        }
    }
    
    //
    // this is used by the UI to decide when to display the OFFERDRAW box
    //
    public boolean drawIsLikely()
    {	switch(board_state)
    	{
    	case DrawPending: return true;
    	case Play:
    		return((moveNumber - lastProgressMove)>10);
    	case CaptureMore:
    	case Capture:
       	default: return(false);
    	}
    	
    }
    //
    // declare the game over, and the winner and loser
    //
    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[whoseTurn^1]=winNext;
    	setState(CheckerState.Gameover);
    }
    public boolean gameOverNow() { return(board_state.GameOver()); }
    public boolean winForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	// we maintain the wins in doDone so no logic is needed here.
    	if(board_state.GameOver()) { return(win[player]); }
    	return(false);
    }
    // estimate the value of the board position.
    public double ScoreForPlayer(int player,boolean print)
    {  	double finalv=simpleScore(player);
    	
    	return(finalv);
    }


    //
    // finalize all the state changes for this move.
    //
    public void acceptPlacement()
    {	
        pickedObject = null;
        madeKing = false;
        droppedDestStack.clear();
        dropState.clear();
        captureStack.clear();
        captureHeight.clear();
        pickedSourceStack.clear();
        pickedHeight.clear();
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    G.Assert(pickedObject==null, "nothing should be moving");
    if(droppedDestStack.size()>0)
    	{
    	CheckerCell dr = droppedDestStack.pop();
    	setState(dropState.pop());
    	currentDest=null;
    	switch(dr.rackLocation())
	    	{
	   		default: throw G.Error("Not expecting rackLocation %s",dr.rackLocation);
			case BoardLocation: 
				CheckerChip ch = pickedObject = dr.removeTop();
				if(pickedHeight.top()==2) { dr.removeTop(); }
				occupiedCells[playerIndex(ch)].remove(dr,false); 
				if((board_state==CheckerState.Capture)||(board_state==CheckerState.CaptureMore))
				{
					CheckerCell cap = captureStack.pop();
					int capHeight = captureHeight.pop();
					undoCapture(cap,capHeight);
					currentDest = droppedDestStack.top();
				}
				else { captureHeight.pop(); }
				dr.lastPlaced = previousLastPlaced;
				lastPlacedIndex--;
				
				break;
			case White_Chip_Pool:	// treat the pools as infinite sources and sinks
			case Black_Chip_Pool:	
				pickedObject = dr.topChip();
				break;	// don't add back to the pool
	    	
	    	}
	    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	CheckerChip po = pickedObject;
    	if(po!=null)
    	{
    		CheckerCell ps = pickedSourceStack.pop();
    		int h = pickedHeight.pop();
    		switch(ps.rackLocation())
    		{
    		default: throw G.Error("Not expecting rackLocation %s",ps.rackLocation);
    		case BoardLocation: 
    				ps.addChip(po);
    				if(h==2) { ps.addChip(po); }
    				occupiedCells[playerIndex(po)].push(ps);
    				ps.lastEmptied = previousLastEmptied;
    				
    				break;
    		case White_Chip_Pool:
    		case Black_Chip_Pool:	break;	// don't add back to the pool
    		}
    		pickedObject = null;
     	}
     }

    // 
    // drop the floating object.
    //
    private void dropObject(CheckerCell c)
    {   G.Assert(pickedObject!=null,"pickedObject should not be null"); 	    		
    	switch(c.rackLocation())
		{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case BoardLocation:
			c.addChip(pickedObject);
			if(pickedHeight.top()==2) { c.addChip(pickedObject); }
			occupiedCells[playerIndex(pickedObject)].push(c);
			previousLastPlaced = c.lastPlaced;
			c.lastPlaced = lastPlacedIndex;
			lastPlacedIndex++;
			break;
		case White_Chip_Pool:
		case Black_Chip_Pool:	break;	// don't add back to the pool
		}
    	dropState.push(board_state);
       	droppedDestStack.push(c);
       	currentDest=c;
       	pickedObject = null;
    }
    
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(CheckerCell c)
    {	G.Assert(pickedObject==null,"pickedObject should be null");
    	G.Assert(!c.isEmpty(),"should have a chip");
    	switch(c.rackLocation())
    	{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case BoardLocation: 
			boolean isKing = c.isKing();
			CheckerChip ch = pickedObject = c.removeTop();
			if(isKing) { c.removeTop(); }
			pickedHeight.push(isKing?2:1);
			occupiedCells[playerIndex(ch)].remove(c,false);
			previousLastContents = ch;
			previousLastEmptied = c.lastEmptied;
			c.lastEmptied = lastPlacedIndex;
			
			break;
		case White_Chip_Pool:
		case Black_Chip_Pool:	
			pickedObject = c.topChip();
			pickedHeight.push(1);
			break;	// don't add back to the pool
    	
    	}
    	pickedSourceStack.push(c);
   }

    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(CheckerCell cell)
    {	return(droppedDestStack.top()==cell);
    }
    //
    // get the last dropped dest cell
    //
    public CheckerCell getDest() 
    { return(droppedDestStack.top()); 
    }
    
    public CheckerCell getPrevDest()
    {
    	return(lastDest[whoseTurn^1]);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  Returns +100 if a king is the moving object.
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	CheckerChip ch = pickedObject;
    	if(ch!=null)
    		{ int nn = ch.chipNumber();
    		  return(nn+((pickedHeight.topz(0)==2)?100:0));
    		}
        return (NothingMoving);
    }
    // get a cell from a partucular source
    public CheckerCell getCell(CheckerId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	return(getCell(col,row));
        case White_Chip_Pool:
       		return(rack[White_Chip_Index]);
        case Black_Chip_Pool:
       		return(rack[Black_Chip_Index]);
        }
    }
    /**
     * this is called when copying boards, to get the cell on the new (ie current) board
     * that corresponds to a cell on the old board.
     */
    public CheckerCell getCell(CheckerCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }

    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(replayMode replay)
    {
        switch (board_state)
        {
        case Gameover:
        	// needed by some damaged games
        	if(replay!=replayMode.Live) { break; }
			//$FALL-THROUGH$
		default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case Confirm:
        case Draw:
        	setNextStateAfterDone(); 
        	break;
        case Play:
 			setState(CheckerState.Confirm);
			break;
        case CaptureMore:
        case Capture:
        	if(!hasCaptureMoves(currentDest)) 
        		{ // leave capture state
        			setState(CheckerState.Confirm);
        		}
        	else { setState(CheckerState.CaptureMore); }
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
    public boolean isSource(CheckerCell c)
    {	return(getSource()==c);
    }
    public CheckerCell getSource()
    {
    	return((pickedSourceStack.size()>0) ?pickedSourceStack.top() : null);
    }
    
    // the second source is the previous stage of a multiple jump
    public boolean isSecondSource(CheckerCell cell)
    {	int h =pickedSourceStack.size();
    	return((h>=2) && (cell==pickedSourceStack.elementAt(h-2)));
    }

    //
    // we don't need any special state changes for picks.
    //
    private void setNextStateAfterPick()
    {
    }
    
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	case Capture:
    	case CaptureMore:
    	default: throw G.Error("Not expecting state %s",board_state);
    	case Gameover: 
    		break;
        case DrawPending:
        	lastDrawMove = moveNumber;
        	setState(CheckerState.AcceptOrDecline);
        	break;
    	case AcceptPending:
        case Draw:
        	setGameOver(false,false);
        	break;
    	case Confirm:
       	case DeclinePending:
       	case Puzzle:
    	case Play:
    		if(hasCaptureMoves()) { setState(CheckerState.Capture); }
    		else if(hasSimpleMoves()) { setState(CheckerState.Play); }
    		else { setGameOver(false,true); } // no moves, we lose
     		break;
    	}

    }
    //
    // make dest be a king
    //
    private void makeKing(CheckerCell dest,replayMode replay)
    {	CheckerChip added = rack[whoseTurn].topChip();
    	//G.Assert(added.id==dest.topChip().id,"same color");
    	dest.addChip(added);
    	madeKing = true;
    	if(replay!=replayMode.Replay)
    	{
    		animationStack.push(rack[whoseTurn]);
    		animationStack.push(dest);
    	}
    }
    private void doDone(replayMode replay)
    {	CheckerCell dest = currentDest;
    	boolean isKing = (dest!=null) && dest.isKing();
    	boolean kingMe = (dest!=null) && !isKing && (dest.row==kingRow[whoseTurn]);
    	lastDest[whoseTurn] = dest;
    	currentDest = null;
    	lastPlacedIndex++;
    	if(!isKing) 
    		{ lastProgressMove = moveNumber; }
    	// special bit for the animations
    	captureKing = (captureHeight.size()>0) && captureHeight.top()==2;
    	
     	acceptPlacement();
    	
       	if(kingMe)
    	{	// rules for making kings vary slightly
       		makeKing(dest,replay);
    	}

        if (board_state==CheckerState.Resign)
        {	setGameOver(false,true);
        }
        else
        {	setNextPlayer(replay); 
        	setNextStateAfterDone(); 

        }
    }

    private void doCapture(CheckerCell mid)
    {	boolean isKing = mid.isKing();
    	CheckerChip ch = mid.removeTop();
    	if(isKing) { mid.removeTop(); }
    	int capee = playerIndex(ch);
		occupiedCells[capee].remove(mid,false);
		captureStack.push(mid);
		mid.lastContents = ch;
		mid.lastCaptured = lastPlacedIndex;
		captureHeight.push(isKing?2:1);
    }
    private void undoCapture(CheckerCell cap,int capHeight)
    {	//checkOccupied();
    	int player = whoseTurn^1;
		CheckerChip chip = rack[player].topChip();
		if(capHeight==2) { cap.addChip(chip); }
		cap.addChip(chip);
		occupiedCells[player].push(cap);
		//checkOccupied();
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	CheckerMovespec m = (CheckerMovespec)mm;
    	//checkOccupied();
        if(replay!=replayMode.Replay) { animationStack.clear(); }
        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone(replay);

            break;

        case MOVE_JUMP:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case Capture:
        		case CaptureMore:
        			G.Assert(pickedObject==null,"something is moving");
        			CheckerCell src = getCell(CheckerId.BoardLocation, m.from_col, m.from_row);
        			CheckerCell dest = getCell(CheckerId.BoardLocation,m.to_col,m.to_row);
        			CheckerCell mid = getCell(CheckerId.BoardLocation,m.target_col,m.target_row);
        			doCapture(mid);
        			
        			
        			pickObject(src);
        			dropObject(dest); 
        			if(replay!=replayMode.Replay)
        			{	animationStack.push(mid);	// captured stones first
        				animationStack.push(rack[whoseTurn^1]);
        				animationStack.push(src);
        				animationStack.push(dest);
        			}
        			robotCapture.push(captureHeight.top());
        			acceptPlacement();
 				    setNextStateAfterDrop(replay);
        			break;
        	}
        	break;
        	
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case Play:
        			G.Assert(pickedObject==null,"something is moving");
        			CheckerCell src = getCell(CheckerId.BoardLocation, m.from_col, m.from_row);
        			CheckerCell dest = getCell(CheckerId.BoardLocation,m.to_col,m.to_row);
        			pickObject(src);
        			dropObject(dest); 
        			captureHeight.push(0);
        			if(replay!=replayMode.Replay)
        			{
        				animationStack.push(src);
        				animationStack.push(dest);
        			}
 				    setNextStateAfterDrop(replay);
 				    acceptPlacement();
        			break;
        	}
        	break;
        case MOVE_DROPB:
			{
			lastDropped = pickedObject;
			CheckerCell c = getCell(CheckerId.BoardLocation, m.to_col, m.to_row);
        	G.Assert(pickedObject!=null,"something is moving");
			if(isSecondSource(c))
			{
				unPickObject();
				unDropObject();
				unPickObject();
			}
			else if(isSource(c)) 
            	{ 
            	  unPickObject(); 

            	} 
            	else
            		{
            		dropObject(c);
            		captureHeight.push(0);
            		setNextStateAfterDrop(replay);
            		if(replay==replayMode.Single)
            			{
            			animationStack.push(getSource());
            			animationStack.push(c);
            			}
            		}
			}
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	if((board_state!=CheckerState.CaptureMore) && isDest(getCell(m.from_col,m.from_row)))
        		{ unDropObject(); 
        		}
        	else 
        		{ pickObject(getCell(CheckerId.BoardLocation, m.from_col, m.from_row));
        		  switch(board_state)
        		  {	case Gameover:
        			  	// needed by some damaged games
        			  	if(replay!=replayMode.Live) { break; }
        		    //$FALL-THROUGH$
				default: throw G.Error("Not expecting pickb in state %s",board_state);
        		  	case Play:
        		  	case Capture:
        		  	case CaptureMore:
         		  	case Puzzle:
        		  		break;
        		  }
         		}
 
            break;

        case MOVE_DROP: // drop on chip pool;
        	{
        	CheckerCell c = getCell(m.source, m.to_col, m.to_row);
            dropObject(c);
            setNextStateAfterDrop(replay);
            if(replay==replayMode.Single)
			{
			animationStack.push(getSource());
			animationStack.push(c);
			}}
            break;

        case MOVE_DROPC: // drop and capture something
        	{
        	CheckerCell c = getCell(m.source, m.to_col, m.to_row);
        	CheckerCell cap = getCell(m.target_col,m.target_row); 
            dropObject(c);
            doCapture(cap);
            setNextStateAfterDrop(replay);
            if(replay!=replayMode.Replay)
            {
            	animationStack.push(cap);
            	animationStack.push(rack[whoseTurn^1]);
            }
            if(replay==replayMode.Single)
			{
			animationStack.push(getSource());
			animationStack.push(c);
			}
        	}
            break;

        case MOVE_PICK:
        	{
        	CheckerCell c = getCell(m.source, m.from_col, m.from_row);
            pickObject(c);
            setNextStateAfterPick();
        	}
            break;


        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(CheckerState.Puzzle);
            {	boolean win1 = winForPlayerNow(whoseTurn);
            	boolean win2 = winForPlayerNow(whoseTurn^1);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;
        case MOVE_OFFER_DRAW:
        	currentDest=null;
        	if(board_state==CheckerState.DrawPending) { setState(dropState.pop()); }
        	else { dropState.push(board_state);
        			setState(CheckerState.DrawPending);
        		}
        	break;
        case MOVE_ACCEPT_DRAW:
           	currentDest=null;
           	switch(board_state)
        	{	
        	case AcceptPending: 	// cancel accept and revert to neutral
        		setState(CheckerState.AcceptOrDecline); 
        		break;
           	case AcceptOrDecline:
           	case DeclinePending:	// accept pending
           		setState(CheckerState.AcceptPending); 
           		break;
        	default: throw G.Error("Not expecting %s",board_state);
        	}
           	break;
        case MOVE_DECLINE_DRAW:
           	currentDest=null;
        	switch(board_state)
        	{	
        	case DeclinePending:	// cancel decline and revert to neutral
        		setState(CheckerState.AcceptOrDecline); 
        		break;
        	case AcceptOrDecline:
        	case AcceptPending: setState(CheckerState.DeclinePending); break;
        	default: throw G.Error("Not expecting %s",board_state);
        	}
        	break;
        case MOVE_RESIGN:
           	currentDest=null;
        	setState(unresign==null?CheckerState.Resign:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            // standardize "gameover" is not true
            setState(CheckerState.Puzzle);
 
            break;

		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;

        default:
        	cantExecute(m);
        }

        //checkOccupied();
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
        case Capture:
        case CaptureMore:
        case Play: 
		case Gameover:
		case Resign:
		case AcceptOrDecline:
		case DeclinePending:
		case DrawPending:
		case AcceptPending:
			return(false);
        case Puzzle:
        	return((pickedObject==null)?true:(player==playerIndex(pickedObject)));
        }
    }
  

    public boolean legalToHitBoard(CheckerCell cell,Hashtable<CheckerCell,CheckerMovespec>targets)
    {	
        switch (board_state)
        {
		case CaptureMore:
			return(isSource(cell)||isSecondSource(cell)||(targets.get(cell)!=null));
 		case Play:
 		case Capture:
 			return(isSource(cell)||targets.get(cell)!=null);
 		case Resign:
		case Gameover:
		case AcceptOrDecline:
		case DeclinePending:
		case AcceptPending:
		case DrawPending:
			return(false);
		case Confirm:
		case Draw:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case Puzzle:
        	return(pickedObject==null?!cell.isEmpty():true);
        }
    }
  public boolean canDropOn(CheckerCell cell)
  {		CheckerCell top = (pickedObject!=null) ? pickedSourceStack.top() : null;
  		return((pickedObject!=null)				// something moving
  			&&(top.onBoard 			// on the main board
  					? (cell!=top)	// dropping on the board, must be to a different cell 
  					: (cell==top))	// dropping in the rack, must be to the same cell
  				);
  }
 

 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(CheckerMovespec m)
    {
        //G.print("R "+m);
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        robotState.push(board_state);
        robotLast.push(currentDest);
        robotDepth++;
        if (Execute(m,replayMode.Replay))
        {	
            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {	if((robotDepth<=6) && (repeatedPositions.numberOfRepeatedPositions(Digest())>1))
            		{
            		// this check makes game end by repetition explicitly visible to the robot
            		setGameOver(false,false);
            		}
            else { doDone(replayMode.Replay); }
            }
        }
        robotKing.push(madeKing?1:0);
        madeKing = false;
    }
 

   //
   // un-execute a move.  The move will only be un-executed
   // in proper sequence, and if it was executed by the robot in the first place.
   // If you use monte carlo bots with the "blitz" option this will never be called.
   //
    public void UnExecute(CheckerMovespec m)
    {	//checkOccupied();
        //G.print("U "+m+" for "+whoseTurn);
    	robotDepth--;
        setState(robotState.pop());
        boolean unKing = robotKing.pop()==1;
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
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
         		case Play:
         			{
        			G.Assert(pickedObject==null,"something is moving");
        			pickObject(getCell(CheckerId.BoardLocation, m.to_col, m.to_row));
        			CheckerCell from = getCell(CheckerId.BoardLocation, m.from_col,m.from_row);
       			    dropObject(from); 
       			    if(unKing) { from.removeTop(); }
       			    acceptPlacement();
         			}
        			break;
        	}
        	break;
        case MOVE_JUMP:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case CaptureMore:
        		case Capture:
        			{
        			G.Assert(pickedObject==null,"something is moving");
        			CheckerCell to = getCell(CheckerId.BoardLocation, m.to_col, m.to_row);
        			pickObject(to);
        			CheckerCell from = getCell(CheckerId.BoardLocation, m.from_col,m.from_row);
       			    dropObject(from);
       			    if(unKing) { from.removeTop(); }
       			    CheckerCell mid = getCell(CheckerId.BoardLocation, m.target_col, m.target_row);
       			    undoCapture(mid,robotCapture.pop());
       			    acceptPlacement();
         			}
        			break;
        	}
        	break;
 
        case MOVE_RESIGN:
        case MOVE_ACCEPT_DRAW:
        case MOVE_DECLINE_DRAW:
        case MOVE_OFFER_DRAW:
            break;
        }
        currentDest = robotLast.pop();
        //checkOccupied();
  }

private void loadHash(CommonMoveStack all,Hashtable<CheckerCell,CheckerMovespec>hash,boolean from)
{
	for(int lim=all.size()-1; lim>=0; lim--)
	{
		CheckerMovespec m = (CheckerMovespec)all.elementAt(lim);
		switch(m.op)
		{
		default: break;
		case MOVE_JUMP:
		case MOVE_BOARD_BOARD:
			if(from) { hash.put(getCell(m.from_col,m.from_row),m); }
			else { hash.put(getCell(m.to_col,m.to_row),m); }
		}
		}
}
/**
 * getTargets() is called from the user interface to get a hashtable of 
 * cells which the mouse can legally hit.
 * 
 * Checkers uses the move generator for most of the logic of where it's legal
 * for the mouse to pick up or drop something.  We start with the list of legal
 * moves, and select either the legal "from" spaces, or the legal "to" spaces.
 * 
 * The advantage of this approach is that the logic for "legal moves" whatever it
 * may be, is needed anyway to drive the robot, and by reusing the move list we
 * avoid having to duplicate that logic.
 * 
 * @return
 */
public Hashtable<CheckerCell,CheckerMovespec>getTargets()
{
	Hashtable<CheckerCell,CheckerMovespec>hash = new Hashtable<CheckerCell,CheckerMovespec>();
	CommonMoveStack all = new CommonMoveStack();

		switch(board_state)
		{
		default: break;
		case Capture:
		case CaptureMore:
		case Play:
			{	addMoves(all,whoseTurn);
				loadHash(all,hash,pickedObject==null);
			}
			break;
		}
	return(hash);
}
// true if there are any capturing moves at all.
public boolean hasCaptureMoves()
{	return(addCaptureMoves(null,whoseTurn,null));
}

// true if there are capturing moves from a particular cell
public boolean hasCaptureMoves(CheckerCell from)
{
	return(addCaptureMoves(null,from,from.isKing(),whoseTurn,null));
}
public boolean hasSimpleMoves()
{
	return(addSimpleMoves(null,whoseTurn));
}
    
 // add normal diagonal capture moves. 
 // all may be null, in which case we only want to know if they exist
 // return true if there are moves.
 public boolean addCaptureMoves(CommonMoveStack all,int who,CellStack empty)
 {	boolean some = false;
 	CellStack pieces = occupiedCells[who];
 	for(int lim=pieces.size()-1; lim>=0; lim--)
 	{
 		CheckerCell cell = pieces.elementAt(lim);
 		some |= addCaptureMoves(all,cell,cell.isKing(),who,empty);
 		if(some && (all==null)) { return(true); }
 	}
 	return(some);
 }
 private int[] getCaptureDirections(boolean isKing,int who0)
 {	int who = getColorMap()[who0];
 	switch(variation)
	 	{
	 	default: throw G.Error("Not expecting %s",variation);
	 	case Checkers_Turkish:
	 		return(isKing ? AllOrthogonals[who] : ForwardOrthogonals[who]);
	 	
	 	case Checkers_International:
	 		return(AllDiagonals[who]);		// international checkers go forward and backward
	 	case Checkers_American:
	 		return(isKing ? AllDiagonals[who]:ForwardDiagonals[who]);
	 	}
		
 }
 // add capture moves originating from a particular cell
 // "all" may be null.
 // return true if there are any
 public boolean addCaptureMoves(CommonMoveStack all,CheckerCell cell,boolean isKing,int who,CellStack empty)
 {	boolean some = false;
 	int diagonals[] = getCaptureDirections(isKing,who);

	for(int direction : diagonals)
 		{	CheckerCell target = cell;
 			boolean more = true;
 			while(more && ((target=target.exitTo(direction))!=null))
 			{
 				CheckerChip top = ((empty!=null) && empty.contains(target)) ? null : target.topChip();
 				if(top!=null)
 				{ 	if(playerIndex(top)!=who)
 					{
 					// something to jump
 					CheckerCell landing = target;
 					while(more && ((landing=landing.exitTo(direction))!=null) && (landing.topChip()==null))
 					{
 						// got a keeper
 						if(all==null) { return(true); }
 						some |= true;
 						all.push(new CheckerMovespec(MOVE_JUMP,cell,target,landing,who));
 						more = isKing && variation.hasFlyingKings; 
 					}}
 					more = false; // found a takeoff piece
 				}
 				else { // keep looking for a target along the current line
 					more = isKing && variation.hasFlyingKings; 
 					}
 				}
 		}
	 return(some);
 }
 // add normal checker moves
 // "all" can be null
 // return true if there are any.
 public boolean addSimpleMoves(CommonMoveStack all,int who)
 {	boolean some = false;
 	CellStack pieces = occupiedCells[who];
 	for(int lim=pieces.size()-1; lim>=0; lim--)
 	{
 		CheckerCell cell = pieces.elementAt(lim);
 		some |= addSimpleMoves(all, cell,cell.isKing(),who);
 		if(some && (all==null))  { return(true); }
 	}
 	return(some);
 }
 
 public int[] getSimpleDirections(boolean isKing,int who0)
 {	int who = getColorMap()[who0];
	 	switch(variation)
	 	{
	 	case Checkers_Turkish:
	 		return(isKing?AllOrthogonals[who] : ForwardOrthogonals[who]);
	 	case Checkers_International:
	 	case Checkers_American:
	 		return(isKing ? AllDiagonals[who]:ForwardDiagonals[who]);
	 	default: throw G.Error("Not expecting %s",variation);
	 	}
 }
 // add normal checker moves from a particular cell
 // "all" can be null
 // return true if there are any.
 public boolean addSimpleMoves(CommonMoveStack all,CheckerCell cell,boolean isKing,int who)
 {	boolean some = false;
 	int directions[] = getSimpleDirections(isKing,who);
 	
	
 	for(int direction : directions)
 		{	CheckerCell next = cell;
 			boolean more = true;
 			while(more && ((next  = next.exitTo(direction))!=null))
 			{	CheckerChip top = next.topChip();
 				
 				if(top==null)
 					{	more = isKing && variation.hasFlyingKings;
 						// got a keeper
 						if(all==null) { return(true); }
 						some |= true;
 						all.push(new CheckerMovespec(MOVE_BOARD_BOARD,cell,next,who));
 						more = isKing && variation.hasFlyingKings;
 					}
 					else 
 					{ more = false; 
 					}
 				}
 			
 		}
	 return(some);
 }
 
 //
 // find the maximum depth for continuing captures.  Empty is a stack of the cells
 // that have already been captured and should be treated as empty.  From is the
 // new takeoff cell.
 public int maximalCaptureDepth(CheckerCell from, boolean isKing,int who,CellStack empty)
 {
	 CommonMoveStack all = new CommonMoveStack();
	 int maxDepth = 0;
	 if(addCaptureMoves(all,from,isKing,who,empty)) 	// add the simple captures from here.
	 {		for(int lim=all.size()-1; lim>=0; lim--)
		 	{ CheckerMovespec m = (CheckerMovespec)all.elementAt(lim);
		 	  empty.push(getCell(m.target_col,m.target_row));	  // add the new captured cell to the stack of empties
		 	  
		 	  int depth = 1+maximalCaptureDepth(getCell(m.to_col,m.to_row),isKing,who,empty);
		 	  maxDepth = Math.max(maxDepth,depth);
		 	  
		 	  empty.pop();		// remove the new captured cell
		 	}
	 }
	 return(maxDepth); 	// no more captures
 }
 
 @SuppressWarnings("unused")
private void checkOccupied()
 {	for(int j=0;j<2;j++)
 	{
 	CellStack cs = occupiedCells[j];
 	CheckerId targetColor = playerColor[j];
 	for(int i=0;i<cs.size();i++)
 		{
 			CheckerCell c = cs.elementAt(i);
 			CheckerChip top = c.topChip();
 			G.Assert((top!=null) && (top.id==targetColor),"incorrectly occupied");
 		}
 	}
 }

 //
 // prune all to contain only captures with the maximum depth.
 // if unpicked is true, we're starting from a static board and
 // the "from" cells of all are filled.  Otherwise a stack has
 // been picked up and we need to use pickedHeight to determine 
 // if it is a king.
 //
 private void removeShortCaptures(CommonMoveStack all,boolean unpicked,int who)
 {	if(all.size()>1)
 	{int maxDepth = -1;
	 CommonMoveStack candidates = new CommonMoveStack();
	 candidates.copyFrom(all);
	 for(int lim=candidates.size()-1;lim>=0;lim--)
	 	{
		 CheckerMovespec m = (CheckerMovespec)candidates.elementAt(lim);
		 CellStack empty  = new CellStack();
		 empty.push(getCell(m.target_col,m.target_row));
		 int depth = maximalCaptureDepth(getCell(m.to_col,m.to_row),
				 		unpicked?getCell(m.from_col,m.from_row).isKing()
				 				:pickedHeight.top()==2,
				who,empty);
		 if(depth>maxDepth) { maxDepth=depth; all.clear(); }
		 if(depth==maxDepth) { all.push(m); }
	 	}
	 }
 }

 private boolean addMoves(CommonMoveStack all,int who)
 {	boolean some = false;
 	switch(variation)
	 {
	 default: throw G.Error("Not expecting %s",variation); 
 	 case Checkers_International:
 	 case Checkers_American:
 	 case Checkers_Turkish:
 		 // captures are mandatory
 		 switch(board_state)
 		 {
 		 default: throw G.Error("Not expecting state %s",board_state);
 		 case AcceptOrDecline:
 			 all.push(new CheckerMovespec(MOVE_ACCEPT_DRAW,whoseTurn));
 			 all.push(new CheckerMovespec(MOVE_DECLINE_DRAW,whoseTurn));
 			 break;
 		 case Play:
 			 if(pickedObject==null)
 			 {
 			 some = addSimpleMoves(all,whoseTurn); 
 			 }
 			 else
 			 {	// something is already moving
 			 some = addSimpleMoves(all,getSource(),pickedHeight.top()==2,whoseTurn()); 
 			 }
 			 
 			if( ((moveNumber-lastProgressMove)>8)
 					 && ((moveNumber-lastDrawMove)>4))
 			 {
 				 all.push(new CheckerMovespec(MOVE_OFFER_DRAW,whoseTurn));
 			 }
 			 break;
 			 
 		 case CaptureMore:
 		 case Capture:
 		 {	
 			 if(pickedObject==null)
 			 {
 			 some = ((board_state==CheckerState.CaptureMore) && (currentDest!=null))
 					 	? addCaptureMoves(all,currentDest,currentDest.isKing(),whoseTurn,null)
 					 	: addCaptureMoves(all,whoseTurn,null);
 			 }
 			 else
 			 {	// something is already moving
 			 some = addCaptureMoves(all,getSource(),pickedHeight.top()==2,whoseTurn,null);
  			 }
 			 
 			 switch(variation)
 			 {
 			 case Checkers_Turkish:
 			 case Checkers_International:
 				 if(all.size()>1) { removeShortCaptures(all,pickedObject==null,whoseTurn); }
 				 break;
 			 case Checkers_American: 
 				 break;
 			 default: G.Error("Not expecting %s",variation);
 			 }

 			 break;
 		 }
 		 }
	 }
 	return(some);
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	addMoves(all,whoseTurn);
 	return(all);
 }
 

}
