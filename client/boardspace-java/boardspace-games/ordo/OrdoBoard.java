package ordo;
/**
 * TODO: needs to be more willing to accept draws
 */
import online.game.*;
import ordo.OrdoConstants.CheckerId;
import ordo.OrdoConstants.CheckerState;
import ordo.OrdoConstants.StateStack;
import ordo.OrdoConstants.Variation;

import static ordo.OrdoMovespec.*;

import java.util.*;

import lib.*;
import lib.Random;
/**
 * OrdoBoard knows all about the game of Checkers.
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

class OrdoBoard extends rectBoard<OrdoCell> implements BoardProtocol
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
    private int sweep_counter = 0;
    public OrdoCell rack[] = null;	// the pool of chips for each player.  
    public int kingRow[] = {0,0};		// will be the row checkers get promoted to king
    public CellStack animationStack = new CellStack();
    //
    // private variables
    //
    private RepeatedPositions repeatedPositions = null;		// shared with the viewer
    private CheckerState board_state = CheckerState.OrdoPlay;	// the current board state
    private CheckerState unresign = null;					// remembers the previous state when "resign"
    private CheckerState resetState = null;
    Variation variation = Variation.Ordo;

    public OrdoPlay robot = null;
    
    public boolean p1(String msg)
   	{
   		if(G.p1(msg) && robot!=null)
   		{	String dir = "g:/share/projects/boardspace-html/htdocs/ordo/ordogames/robot/";
   			robot.saveCurrentVariation(dir+msg+".sgf");
   			return(true);
   		}
   		return(false);
   	}
    
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
    public OrdoChip pickedObject = null;
    public OrdoChip lastDropped = null;
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    private StateStack dropState = new StateStack();
    
    OrdoCell selectedStart = null;
    OrdoCell selectedEnd = null;
    OrdoCell selectedDest = null;
    
    private boolean madeKing = false;
    public OrdoCell lastDest[] = {null,null};				// last spot the opponent dropped, for the UI
    public OrdoCell currentDest = null;
    private int initialStacks[] = {0,0};			// remembers the number of starting pieces
    int lastProgressMove = 0;		// last move where a pawn was advanced
    int lastDrawMove = 0;			// last move where a draw was offered
    int robotDepth = 0;		// current depth of robot search.  This is used to make faster wins look better
    						// than slower wins.  It's part of the board so multiple threads have independent values.
  	private StateStack robotState = new StateStack();
  	private CellStack robotLast = new CellStack();
  	private IStack robotCapture = new IStack();
  	private IStack robotKing = new IStack();
  	
     CellStack occupiedCells[] = new CellStack[2];	// cells occupied, per color
    
    
    private int ForwardOrdo[][] = {{CELL_UP_RIGHT,CELL_UP,CELL_UP_LEFT},
    		{CELL_DOWN_LEFT,CELL_DOWN,CELL_DOWN_LEFT}};
    private int ForwardOrSidewaysOrdo[][] =
    		{{CELL_LEFT,CELL_UP_RIGHT,CELL_UP,CELL_UP_LEFT,CELL_RIGHT},
    		{CELL_LEFT,CELL_DOWN_LEFT,CELL_DOWN,CELL_DOWN_RIGHT,CELL_RIGHT}};
    private int AllOrdo[][] = {{CELL_LEFT,CELL_UP_RIGHT,CELL_UP,CELL_UP_LEFT,CELL_RIGHT,CELL_DOWN_LEFT,CELL_DOWN,CELL_DOWN_LEFT},
    		{CELL_LEFT,CELL_DOWN_LEFT,CELL_DOWN,CELL_DOWN_LEFT,CELL_RIGHT,CELL_UP_RIGHT,CELL_UP,CELL_UP_LEFT}};
  
	// factory method to create a new cell as part of the board
	public OrdoCell newcell(char c,int r)
	{	return(new OrdoCell(c,r));
	}
    public OrdoBoard(String init,long rv,int np,RepeatedPositions rep,int map[],int rev) // default constructor
    {   repeatedPositions = rep;

    	setColorMap(map);
        doInit(init,rv,np,rev); // do the initialization 
        autoReverseY();		// reverse_y based on the color map
     }


	public void sameboard(BoardProtocol f) { sameboard((OrdoBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if {@link #clone},{@link #Digest} and {@link #sameboard} methods agree.
     * @param from_b
     */
    public void sameboard(OrdoBoard from_b)
    {
    	super.sameboard(from_b);	// calls sameCell for each cell, also for inherited class variables.
     	G.Assert(unresign==from_b.unresign,"unresign mismatch");
     	G.Assert(resetState==from_b.resetState,"resetState mismatch");
       	G.Assert(AR.sameArrayContents(win,from_b.win),"win array contents match");
       	G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor contents match");
       	G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(dropState.sameContents(from_b.dropState),"dropState mismatch");
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject doesn't match");
        G.Assert(occupiedCells[FIRST_PLAYER_INDEX].size()==from_b.occupiedCells[FIRST_PLAYER_INDEX].size(),"occupiedCells mismatch");
        G.Assert(occupiedCells[SECOND_PLAYER_INDEX].size()==from_b.occupiedCells[SECOND_PLAYER_INDEX].size(),"occupiedCells mismatch");
        G.Assert(sameCells(currentDest,from_b.currentDest),"currentDest mismatch");
        G.Assert(sameCells(selectedStart,from_b.selectedStart),"selectedStart mismatch");
        G.Assert(sameCells(selectedEnd,from_b.selectedEnd),"selectedEnd mismatch");
        G.Assert(sameCells(selectedDest,from_b.selectedDest),"selectedDest mismatch");
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
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,occupiedCells[SECOND_PLAYER_INDEX].size());	// not completely specific because the stack can be shuffled
		v ^= Digest(r,occupiedCells[FIRST_PLAYER_INDEX].size());
		v ^= Digest(r,currentDest);
		v ^= Digest(r,selectedStart);
		v ^= Digest(r,selectedEnd);
		v ^= Digest(r,selectedDest);
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
        return (v);
    }
   public OrdoBoard cloneBoard() 
	{ OrdoBoard copy = new OrdoBoard(gametype,randomKey,players_in_game,repeatedPositions,getColorMap(),revision);
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((OrdoBoard)b); }


    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(OrdoBoard from_b)
    {	
        super.copyFrom(from_b);			// copies the standard game cells in allCells list
        pickedObject = from_b.pickedObject;	
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        dropState.copyFrom(from_b.dropState);
        getCell(occupiedCells,from_b.occupiedCells);
        AR.copy(playerColor,from_b.playerColor);
        board_state = from_b.board_state;
        lastProgressMove = from_b.lastProgressMove;
        lastDrawMove = from_b.lastDrawMove;
        getCell(lastDest,from_b.lastDest);
        currentDest = getCell(from_b.currentDest);
        selectedStart = getCell(from_b.selectedStart);
        selectedEnd = getCell(from_b.selectedEnd);
        selectedDest = getCell(from_b.selectedDest);
        unresign = from_b.unresign;
        resetState = from_b.resetState;
        repeatedPositions = from_b.repeatedPositions;
        copyFrom(rack,from_b.rack);
        robot = from_b.robot;
        sameboard(from_b);
    }
    public void doInit(String gtype,long rv)
    {
    	doInit(gtype,rv,players_in_game,revision);
    }
    private int playerIndex(OrdoChip ch)
    {	int []map = getColorMap();
    	if(ch==OrdoChip.black) { return(map[SECOND_PLAYER_INDEX]);}
    	else if(ch==OrdoChip.white) { return(map[FIRST_PLAYER_INDEX]); }
    	else { return(-1); }
    }
    public OrdoChip playerChip(int pl)
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
		rack = new OrdoCell[2];
    	Random r = new Random(67246765);
    	int map[]=getColorMap();
     	for(int i=0,pl=FIRST_PLAYER_INDEX;i<2; i++,pl=pl^1)
    	{
     	occupiedCells[i] = new CellStack();
       	OrdoCell cell = new OrdoCell(r);
       	cell.rackLocation=RackLocation[i];
       	cell.addChip(OrdoChip.getChip(i));
     	rack[map[i]]=cell;
     	}    
     	
     	variation = Variation.findVariation(gtype);
     	switch(variation)
     	{
     	default:  throw G.Error(WrongInitError,gtype);
     	case Ordo:
     	case OrdoX:
       		reInitBoard(variation.cols,variation.rows);
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
	    for(OrdoCell c = allCells; c!=null; c=c.next)
	    {  int i = (c.row+c.col)%2;
	       c.addChip(OrdoChip.getTile(i^1));
	    }
	    switch(variation)
	    {
	    default: break;
	    case Ordo:
	    case OrdoX:
	    	for(int coln=0;coln<variation.cols;coln++)
	    	{
	    		int off = ((coln&2)==0) ? 2 : 1;
	    		char col = (char)('A'+coln);
	    		for(int rown = off,last=off+2;rown<last;rown++)
	    		{
	    			OrdoCell c = getCell(col,rown);
	    			OrdoCell d = getCell(col,variation.rows-rown+1);
	    			c.addChip(OrdoChip.white);
	    			d.addChip(OrdoChip.black);
	    			occupiedCells[0].push(c);
	    			occupiedCells[1].push(d);
	    		}
	    	}
	    	break;
	    }
	    lastProgressMove = 0;
	    lastDrawMove = 0;
	    robotDepth = 0;
	    robotState.clear();
	    robotLast.clear();
	    robotCapture.clear();
	    robotKing.clear();
	    whoseTurn = FIRST_PLAYER_INDEX;
		playerColor[map[FIRST_PLAYER_INDEX]]=CheckerId.White_Chip_Pool;
		playerColor[map[SECOND_PLAYER_INDEX]]=CheckerId.Black_Chip_Pool;
		initialStacks[FIRST_PLAYER_INDEX] = occupiedCells[FIRST_PLAYER_INDEX].size();
		initialStacks[SECOND_PLAYER_INDEX] = occupiedCells[SECOND_PLAYER_INDEX].size();
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
    	case OrdoPlay:
    		return((moveNumber - lastProgressMove)>10);
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
        selectedStart = selectedEnd = selectedDest = null;	// ordo moves
        madeKing = false;
        droppedDestStack.clear();
        dropState.clear();
        pickedSourceStack.clear();
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    G.Assert(pickedObject==null, "nothing should be moving");
    if(droppedDestStack.size()>0)
    	{
    	OrdoCell dr = droppedDestStack.pop();
    	setState(dropState.pop());
    	currentDest=null;
    	switch(dr.rackLocation())
	    	{
	   		default: throw G.Error("Not expecting rackLocation %s",dr.rackLocation);
			case BoardLocation: 
				OrdoChip ch = pickedObject = dr.removeTop();
				occupiedCells[playerIndex(ch)].remove(dr,false); 
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
    {	OrdoChip po = pickedObject;
    	if(po!=null)
    	{
    		OrdoCell ps = pickedSourceStack.pop();
    		switch(ps.rackLocation())
    		{
    		default: throw G.Error("Not expecting rackLocation %s",ps.rackLocation);
    		case BoardLocation: 
    				ps.addChip(po);
    				occupiedCells[playerIndex(po)].push(ps);
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
    private void dropObject(OrdoCell c)
    {   G.Assert(pickedObject!=null,"pickedObject should not be null"); 	    		
    	switch(c.rackLocation())
		{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case BoardLocation:
			c.addChip(pickedObject);
			occupiedCells[playerIndex(pickedObject)].push(c);
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
    private void pickObject(OrdoCell c)
    {	G.Assert(pickedObject==null,"pickedObject should be null");
    	G.Assert(!c.isEmpty(),"should have a chip");
    	switch(c.rackLocation())
    	{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case BoardLocation: 
			OrdoChip ch = pickedObject = c.removeTop();
			occupiedCells[playerIndex(ch)].remove(c,false);
			break;
		case White_Chip_Pool:
		case Black_Chip_Pool:	
			pickedObject = c.topChip();
			break;	// don't add back to the pool
    	
    	}
    	pickedSourceStack.push(c);
   }

    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(OrdoCell cell)
    {	return(droppedDestStack.top()==cell);
    }
    //
    // get the last dropped dest cell
    //
    public OrdoCell getDest() 
    { return(droppedDestStack.top()); 
    }
    
    public OrdoCell getPrevDest()
    {
    	return(lastDest[whoseTurn^1]);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  Returns +100 if a king is the moving object.
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	OrdoChip ch = pickedObject;
    	if(ch!=null)
    		{ return ch.chipNumber();
    		}
        return (NothingMoving);
    }
    // get a cell from a partucular source
    public OrdoCell getCell(CheckerId source,char col,int row)
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
    public OrdoCell getCell(OrdoCell c)
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
        case OrdoPlay:
        case Reconnect:
 			setState(CheckerState.Confirm);
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
    public boolean isSource(OrdoCell c)
    {	return(getSource()==c);
    }
    public OrdoCell getSource()
    {
    	return((pickedSourceStack.size()>0) ?pickedSourceStack.top() : null);
    }
    
  
    private boolean raceGoalOccupied(int who)
    {
    	OrdoCell home = getCell('A',kingRow[who]);
    	OrdoChip target = playerChip(nextPlayer[who]);
    	while(home!=null)
    	{
    		OrdoChip top = home.topChip();
    		if(top==target) { return true; }
    		home = home.exitTo(CELL_RIGHT);
    	}
    	return false;
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
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
    		switch(variation)
    		{
    		case Ordo:
    			if(occupiedCells[whoseTurn].size()==0) { setGameOver(false,true); }
    			else if(raceGoalOccupied(whoseTurn)) { setGameOver(false,true); }
    			else if(isOrdoConnected(whoseTurn,null,null)) { setState(CheckerState.OrdoPlay); }
    			else if(hasReconnectionMoves(whoseTurn)) 
    					{ setState(CheckerState.Reconnect); 
    					}
    			else { setGameOver(false,true); }
    			break;
    			
    		case OrdoX:
    			throw G.Error("Not implemented");
    		default:
    			throw G.Error("Not implemented");
    			   		
    		}
    	}
       	resetState = board_state;

    }

    private void doDone(replayMode replay)
    {	OrdoCell dest = currentDest;
    	lastDest[whoseTurn] = dest;
    	currentDest = null;
   	
     	acceptPlacement();
 
        if (board_state==CheckerState.Resign)
        {	setGameOver(false,true);
        }
        else
        {	setNextPlayer(replay); 
        	setNextStateAfterDone(); 

        }
    }

    private void doCapture(OrdoCell mid)
    {	
    	OrdoChip ch = mid.removeTop();
    	int capee = playerIndex(ch);
		occupiedCells[capee].remove(mid,false);
    }
    private void undoCapture(OrdoCell cap,int capHeight)
    {	//checkOccupied();
    	int player = whoseTurn^1;
		OrdoChip chip = rack[player].topChip();
		if(capHeight==2) { cap.addChip(chip); }
		cap.addChip(chip);
		occupiedCells[player].push(cap);
		//checkOccupied();
    }
    
    private void undoOrdoMove()
    {
    	int direction = findDirection(selectedStart,selectedEnd);
		OrdoCell from = selectedDest;
		OrdoCell sel = selectedStart;
		boolean exit = false;
		OrdoChip chip = null;
		do { pickObject(from);
			 chip = pickedObject;
			 pickedObject = null;
			 exit = sel==selectedEnd;
			 sel = sel.exitTo(direction);
			 from = from.exitTo(direction);      				 
		} while(!exit);
		OrdoCell to = selectedStart;
		exit = false;
		do {
			pickedObject = chip;
			dropObject(to);
			exit = to==selectedEnd;
			to = to.exitTo(direction);
		} while(!exit);
    }
    
    public boolean Execute(commonMove mm,replayMode replay)
    {	OrdoMovespec m = (OrdoMovespec)mm;
    	//checkOccupied();
        if(replay!=replayMode.Replay) { animationStack.clear(); }
        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone(replay);

            break;
        case MOVE_SELECT:
        	{
        	OrdoCell c = getCell(m.from_col,m.from_row);
        	if(c==selectedStart) { selectedStart = selectedEnd; selectedEnd = null; }
        	else if(selectedStart==null) { selectedStart = c; }
        	else if(c==selectedEnd) { selectedEnd = null; }
        	else if(selectedEnd==null)
        		{ if(isDest(c)) 
        			{
        			unDropObject();
        			unPickObject();
        			}
        		else 
        			{selectedEnd = c; 
        			}
        		}
        	else { // this is the undo for an ordo move
        			undoOrdoMove();
        			setState(resetState);
        		}
        	}
        	break;
        case MOVE_CAPTURE:	// ordo capturing move
        	{
        		OrdoCell src = getCell(CheckerId.BoardLocation, m.from_col, m.from_row);
    			OrdoCell dest = getCell(CheckerId.BoardLocation,m.to_col,m.to_row);
    			pickObject(src);
    			doCapture(dest);
    			dropObject(dest);
    			if(replay!=replayMode.Replay)
    			{
    				animationStack.push(dest);
    				animationStack.push(rack[whoseTurn^1]);
    				animationStack.push(src);
    				animationStack.push(dest);
    			}
        	}
        	setState(CheckerState.Confirm);
        	break;
        case MOVE_ORDO:	// ordo block move
        	{
        		OrdoCell from0 = getCell(m.from_col,m.from_row);
        		OrdoCell to0 = getCell(m.to_col,m.to_row);
        		OrdoCell target0 = getCell(m.target_col,m.target_row);
        		OrdoChip top =from0.topChip();
        		int dir = findDirection(from0,target0);
        		{
        		boolean exit = false;
        		OrdoCell from = from0;
        		OrdoCell to = to0;
        		OrdoCell target = target0;
        		selectedDest = to;	// in case of undo
        		do {
        			pickObject(from);
        			pickedObject = null;
        			if(replay!=replayMode.Replay) {
        				animationStack.push(from);
        				animationStack.push(to);
        			}
        			exit = (from==target);
        			from = from.exitTo(dir);
        			to = to.exitTo(dir);
        		} while(!exit);
        		}
        		{
        		boolean exit = false;
        		OrdoCell from = from0;
            	OrdoCell to = to0;
            	OrdoCell target = target0;
        		do {pickedObject = top;
         			dropObject(to);
         			exit = (from==target);
        			from = from.exitTo(dir);
        			to = to.exitTo(dir);
        		} while(!exit);
        		}

        		setState(CheckerState.Confirm);
        	}
        	break;
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case OrdoPlay:
        		case Reconnect:
        			G.Assert(pickedObject==null,"something is moving");
        			OrdoCell src = getCell(CheckerId.BoardLocation, m.from_col, m.from_row);
        			OrdoCell dest = getCell(CheckerId.BoardLocation,m.to_col,m.to_row);
        			pickObject(src);
        			dropObject(dest); 
        			if(replay!=replayMode.Replay)
        			{
        				animationStack.push(src);
        				animationStack.push(dest);
        			}
 				    setNextStateAfterDrop(replay);
        			break;
        	}
        	break;
        case MOVE_DROPCAP:	// for ordo ui captures
			{
			lastDropped = pickedObject;
			OrdoCell c = getCell(CheckerId.BoardLocation, m.to_col, m.to_row);
	    	G.Assert(pickedObject!=null,"something is moving");
	    	if(isSource(c)) 
	        	{ 
	        	  unPickObject(); 
	        	} 
	        	else
	        		{
	        		doCapture(c);
	        		dropObject(c);
	        		setNextStateAfterDrop(replay);
	        		if(replay!=replayMode.Replay)
	        		{
	        			animationStack.push(c);	// captured stones first
        				animationStack.push(rack[whoseTurn^1]);

	        		}
	        		if(replay==replayMode.Single)
	        			{
	        			animationStack.push(getSource());
	        			animationStack.push(c);
	        			}
	        		}
			}
	        break;
    	
        case MOVE_DROPB:
			{
			lastDropped = pickedObject;
			OrdoCell c = getCell(CheckerId.BoardLocation, m.to_col, m.to_row);
        	G.Assert(pickedObject!=null,"something is moving");
			if(isSource(c)) 
            	{ 
            	  unPickObject(); 

            	} 
            	else
            		{
            		dropObject(c);
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
        		{ pickObject(getCell(CheckerId.BoardLocation, m.from_col, m.from_row));
        		  switch(board_state)
        		  {	case Gameover:
        			  	// needed by some damaged games
        			  	if(replay!=replayMode.Live) { break; }
        		    //$FALL-THROUGH$
				default: throw G.Error("Not expecting pickb in state %s",board_state);
        		  	case OrdoPlay:
        		  	case Reconnect:
         		  	case Puzzle:
        		  		break;
        		  }
         		}
 
            break;

        case MOVE_DROP: // drop on chip pool;
        	{
        	OrdoCell c = getCell(m.source, m.to_col, m.to_row);
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
        	OrdoCell c = getCell(m.source, m.to_col, m.to_row);
        	OrdoCell cap = getCell(m.target_col,m.target_row); 
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
        	OrdoCell c = getCell(m.source, m.from_col, m.from_row);
            pickObject(c);
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
		case Gameover:
		case Resign:
		case AcceptOrDecline:
		case DeclinePending:
		case DrawPending:
		case AcceptPending:
		case Reconnect:
		case OrdoPlay:
			return(false);
        case Puzzle:
        	return((pickedObject==null)?true:(player==playerIndex(pickedObject)));
        }
    }
  

    public boolean legalToHitBoard(OrdoCell cell,Hashtable<OrdoCell,OrdoMovespec>targets)
    {	
        switch (board_state)
        {
 		case OrdoPlay:
 		case Reconnect:
 			return(cell==selectedStart||cell==selectedEnd||targets.get(cell)!=null);
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


 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(OrdoMovespec m)
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
    public void UnExecute(OrdoMovespec m)
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
         		case OrdoPlay:
         			{
        			G.Assert(pickedObject==null,"something is moving");
        			pickObject(getCell(CheckerId.BoardLocation, m.to_col, m.to_row));
        			OrdoCell from = getCell(CheckerId.BoardLocation, m.from_col,m.from_row);
       			    dropObject(from); 
       			    if(unKing) { from.removeTop(); }
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

private void loadHash(CommonMoveStack all,Hashtable<OrdoCell,OrdoMovespec>hash,boolean fetch)
{
	for(int lim=all.size()-1; lim>=0; lim--)
	{
		OrdoMovespec m = (OrdoMovespec)all.elementAt(lim);
		switch(m.op)
		{
		default: break;
		case MOVE_ORDO:
			{
			boolean showDests = selectedStart!=null && selectedEnd!=null;
			OrdoCell target = getCell(m.target_col,m.target_row);
			OrdoCell from = getCell(m.from_col,m.from_row);
			hash.put(from,m);
			hash.put(target,m);
			if(showDests){
				// for the user interface, mark all the destinations
				int direction = findDirection(from,target);
				boolean exit = false;
				OrdoCell to = getCell(m.to_col,m.to_row);
				do {
					hash.put(to,m);
					exit = from==target;
					from = from.exitTo(direction);
					to = to.exitTo(direction);
				} while (!exit);			
			}}
			break;
		case MOVE_CAPTURE:
		case MOVE_BOARD_BOARD:
			if(fetch) { hash.put(getCell(m.from_col,m.from_row),m); }
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
public Hashtable<OrdoCell,OrdoMovespec>getTargets()
{
	Hashtable<OrdoCell,OrdoMovespec>hash = new Hashtable<OrdoCell,OrdoMovespec>();
	CommonMoveStack all = new CommonMoveStack();

		switch(board_state)
		{
		default: break;
		case OrdoPlay:
		case Reconnect:
			{	addMoves(all,whoseTurn);
				loadHash(all,hash,selectedStart==null);
			}
			break;
		}
	return(hash);
}

 private int[] getSimpleOrdoDirections(boolean allowSideways,boolean allowBackwards,int who0)
 {	int who = getColorMap()[who0];
 	switch(variation)
	 	{
	 	case Ordo:
	 	case OrdoX:
	 		return allowBackwards ? AllOrdo[who] : allowSideways ? ForwardOrSidewaysOrdo[who] : ForwardOrdo[who] ; 
	 	default: throw G.Error("Not expecting %s",variation);
	 	}
 }

 // "all" may be null
 // return true if any are added
 private boolean addSimpleOrdoMoves(CommonMoveStack all,OrdoCell cell,OrdoChip ctop,boolean allowsideways,boolean reconnect,int who)
 {	boolean some = false;
 	int directions[] = getSimpleOrdoDirections(allowsideways,reconnect,who);

 	for(int direction : directions)
 		{	OrdoCell next = cell;
 			boolean exit = false;
 			while(!exit && ((next  = next.exitTo(direction))!=null))
 			{	
 				// allowBackwards also implies that we only accept reconnection moves
 				OrdoChip top = next.topChip();
				if(isOrdoConnected(who,cell,next))
 				{
 				if(all==null) { return(true); }		 
 				if(top==ctop) { exit=true; }
 				else
 				{
				some |= true;
 				all.push(new OrdoMovespec(top==null ? MOVE_BOARD_BOARD : MOVE_CAPTURE,cell,next,who));
 				
 				}}
 				if(top!=null) { exit=true; }
 			}
 		}
	 return(some);
 }

 


 private boolean addOrdoSingletonMoves(CommonMoveStack all,int who,boolean allowSideways,boolean allowBackwards)
 {	boolean some = false;
 	 if(selectedEnd==null)
 	 {
	 if(selectedStart!=null)
	 {
		some = addSimpleOrdoMoves(all,selectedStart,selectedStart.topChip(),allowSideways,allowBackwards,who); 
	 }
	 else
	 {
	 CellStack sources = occupiedCells[who];
	 for(int lim=sources.size()-1; lim>=0; lim--)
	 {
		OrdoCell from = sources.elementAt(lim);
		some |= addSimpleOrdoMoves(all,from,from.topChip(),allowSideways,allowBackwards,who);
		if(some && all==null) { return some; }
	 }}}
	 return some;
 }
 
 // place an ordo, return true if successfully placed.
 // if not successfully placed, place nothing.
 private boolean placeOrdo(OrdoCell from,OrdoChip top,int placeDirection,int size)
 {	boolean ok = false;
	 if(from!=null && from.topChip()==null)
	 {	 //G.print("Place ",from);
		 from.addChip(top);
		 if(size>1)
		 {
			 ok = placeOrdo(from.exitTo(placeDirection),top,placeDirection,size-1);
		 }
		 else { ok = true; }
		 if(!ok) { from.removeTop(); //G.print("Unplace ",from);
		 		}
	 }
	 return ok;
 }
 // remove an ordo and return the chip that was on top.
 private OrdoChip removeOrdo(OrdoCell from,int placeDirection,int size)
 {	OrdoChip top = null;
	 for(int i=0;i<size;i++)
	 {	//G.print("Remove ",from);
		 top = from.removeTop();
		 from = from.exitTo(placeDirection);
	 }
	 return top;
 }

 // add possible moves for an ordo defined by from,direction,size
 private boolean addOrdoMoves(CommonMoveStack all,OrdoCell from,int buildDirection,int size,int moveDirections[],int who)
 {	boolean some = false;
 	OrdoChip top = removeOrdo(from,buildDirection,size);
	 
 	for(int moveDirection : moveDirections)
	 {	boolean exit = false;
		OrdoCell to = from;
		while(!exit && (to=to.exitTo(moveDirection))!=null)
		 {
			 if(placeOrdo(to,top,buildDirection,size))
			 {
				 if(isOrdoConnected(who,null,to))
				 {
					 OrdoCell fromEnd = from;
					 for(int i=1;i<size;i++) { fromEnd = fromEnd.exitTo(buildDirection);}
					 some = true;
					 if(all!=null)
					 {	all.push(new OrdoMovespec(MOVE_ORDO,from,fromEnd,to,who));
					 }		
				 }
				 removeOrdo(to,buildDirection,size);
			 }
			 else { exit=true; }
			 if(some && all==null) { break; }		// finish all directions
		 }
	 }
	 
	 placeOrdo(from,top,buildDirection,size);
	 return some;
 }
 // extend an initial cell "from" in a designated direction to form an ordo
 private boolean addOrdoMoves(CommonMoveStack all,OrdoCell from,int buildDirection,int moveDirections[],int who)
 {	boolean some = false;
	 int size = 1;
	 OrdoCell to = from;
	 OrdoChip top = from.topChip();
	 while( (to=to.exitTo(buildDirection))!=null)
	 {	size++;
	 	if(to.topChip()==top)
		 {
			 some |= addOrdoMoves(all,from,buildDirection,size,moveDirections,who);
			 if(some && all==null) { return some; }
		 }
	 	else { break; }
	 }
	 return some;
 }
 
 static int UpAndRightDirection[] = { CELL_UP, CELL_RIGHT };
 static int OrthogonalDirections[] = { CELL_UP, CELL_DOWN, CELL_LEFT, CELL_RIGHT };
 static int HorizontalDirections[] = { CELL_LEFT, CELL_RIGHT};
 static int VerticalDirections[] = { CELL_UP, CELL_DOWN };
 static int UpDirection[] = { CELL_UP };

 private boolean addOrdoMoves(CommonMoveStack all,OrdoCell from,
		 	boolean allowSideways,boolean reconnect,boolean reverse,int who)
 {	boolean some = false;
	 // from is the start of the ordo chain to move. we extend the chain in up and right directions
	 for(int extendDirection : reverse ? OrthogonalDirections : UpAndRightDirection)
	 {	boolean extendHorizontal = (extendDirection==CELL_LEFT) || (extendDirection==CELL_RIGHT);
	 	int movementDirections[] = extendHorizontal 
	 								? (reverse ? VerticalDirections : UpDirection) 
	 								: HorizontalDirections; 
		some |= addOrdoMoves(all,from,extendDirection,movementDirections,who);
		if(some && all==null) { return some; }
	 }
	 return some;
 }
 private boolean addOrdoMoves(CommonMoveStack all,int who,boolean allowSideways,boolean reconnect)
 {	boolean some = false;
	 if(selectedEnd!=null)
	 {	// both ends specified
		int direction = findDirection(selectedStart,selectedEnd);
		int distance = Math.max(Math.abs(selectedStart.col-selectedEnd.col)+1,
								Math.abs(selectedEnd.row-selectedStart.row)+1);
		boolean isHorizontal = selectedStart.row==selectedEnd.row;
		int moveDirections[] = isHorizontal 
									? (reconnect ? VerticalDirections : UpDirection)
									: HorizontalDirections;
		some |= addOrdoMoves(all,selectedStart,direction,distance,moveDirections,who); 
	 }
	 else if(selectedStart!=null)
	 {	
		// one end specified
		some |= addOrdoMoves(all,selectedStart,allowSideways,reconnect,true,who); 
	 }
	 else
	 {
	 CellStack sources = occupiedCells[who];
	 for(int lim=sources.size()-1; lim>=0; lim--)
	 {
		OrdoCell from = sources.elementAt(lim);
		some |= addOrdoMoves(all,from,allowSideways,reconnect,false,who);
		if(some && all==null) { return some; }
	 }}
	return some; 
 }
 // get some cell that is not the designated empty cell
 private OrdoCell someOrdoCell(CellStack sources,OrdoCell empty)
 {
	 OrdoCell c = sources.elementAt(0);
	 if(c!=empty) { return c; }
	 return sources.elementAt(1);
 }
 //
 // count the cells connected to from, considering cells declared empty and full
 // this uses a simple recursive descent to find everyting that's connected
 //
 private int connectedSize(OrdoCell from,OrdoChip top,OrdoCell empty,OrdoCell full)
 {	int mycount = 0;
 	from.sweep_counter = sweep_counter;
 	OrdoChip atop = from==full ? top : from==empty ? null : from.topChip();
 	if(atop==top)
 	{ mycount++;
 	
	 for(int direction = 0; direction<from.geometry.n; direction++)
	 {
		 OrdoCell adj = from.exitTo(direction);
		 if(adj!=null && adj.sweep_counter!=sweep_counter)
		 {	mycount += connectedSize(adj,top,empty,full);
		 }
	 }
 	}
	 return mycount;
 }
 
 // true if all the users pieces are connected, with "empty" emptied and "full" filled.
 private boolean isOrdoConnected(int who,OrdoCell empty,OrdoCell full)
 {	sweep_counter++;
 	CellStack stack = occupiedCells[who];
 	int h = stack.size();
 	OrdoCell seed = full!=null ? full : someOrdoCell(stack,empty);
 	int sz = connectedSize(seed,playerChip(who),empty,full);
 	return sz>=h;
 }
 private boolean hasReconnectionMoves(int who)
 {
	 return addOrdoSingleReconnectMoves(null,who)
			 || addOrdoMoves(null,who,true,true);
 }

 private boolean addOrdoSingleReconnectMoves(CommonMoveStack all,int who)
 {	boolean some = false;
	 if(pickedObject!=null)
	 {
		 some = addSimpleOrdoMoves(all,pickedSourceStack.top(),pickedObject,true,true,who); 
	 }
	 else
	 {
	 CellStack sources = occupiedCells[who];
	 for(int lim=sources.size()-1; lim>=0; lim--)
	 {
		OrdoCell from = sources.elementAt(lim);
		some |= addSimpleOrdoMoves(all,from,from.topChip(),true,true,who);
		if(some && all==null) { return some; }
	 }}
	 return some;
 }

 private boolean addMoves(CommonMoveStack all,int who)
 {	boolean some = false;
 	switch(variation)
	 {
	 default: throw G.Error("Not expecting %s",variation); 
	 case Ordo:
	 case OrdoX:
		 switch(board_state)
		 {
		 default: throw G.Error("State %s not expected",board_state);
		 case OrdoPlay:
			 addOrdoSingletonMoves(all,who,true,false);
			 addOrdoMoves(all,who,true,false);
			 break;
		 case Reconnect:
			 addOrdoSingleReconnectMoves(all,who);
			 addOrdoMoves(all,who,true,true);
			 break;
		 case OrdoPlay2:
			 addOrdoSingletonMoves(all,who,false,false);
			 addOrdoMoves(all,who,false,false);
			 break;
		 }
		 break;
	 }
 	return(some);
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	addMoves(all,whoseTurn);
 	if(all.size()==0) 
 		{ p1("no moves"); 
 		}
 	return(all);
 }
 public void initRobot(OrdoPlay bot)
 {
	 robot = bot;
 }

}
