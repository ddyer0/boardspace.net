package morris;

import online.game.*;

import java.util.*;

import lib.*;
import lib.Random;
import static morris.MorrisMovespec.*;
/**
 * MorrisBoard knows all about the game of 9 Men Morris.
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

class MorrisBoard extends squareBoard<MorrisCell> implements BoardProtocol,MorrisConstants
{	
    public int boardColumns;	// size of the board
    public int boardRows;
    public void SetDrawState() { setState(MorrisState.Draw); }
    
    public MorrisCell rack[] = null;	// the pool of chips for each player.  
    public MorrisCell sample[] = null;
    private MorrisCell whiteCaptured = null;
    private MorrisCell blackCaptured = null;
    private MorrisCell whitePool = null;
    private MorrisCell blackPool = null;
    public MorrisCell captured[] = null;
    public MorrisChip playerChip[] = new MorrisChip[2];
    public CellStack animationStack = new CellStack();
    //
    // private variables
    //
    private RepeatedPositions repeatedPositions = null;		// shared with the viewer
    private MorrisState board_state = MorrisState.Play;	// the current board state
    private MorrisState unresign = null;					// remembers the previous state when "resign"
    Variation variation = Variation.Morris_9;
    public MorrisState getState() { return(board_state); } 
	public void setState(MorrisState st) 
	{ 	unresign = (st==MorrisState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
    private MorrisId playerColor[]={MorrisId.White_Chip_Pool,MorrisId.Black_Chip_Pool};
 	public MorrisId getPlayerColor(int p) { return(playerColor[p]); }
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public MorrisChip pickedObject = null;
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    private StateStack dropState = new StateStack();
    private CellStack captureStack = new CellStack();
    public MorrisCell lastDest[] = {null,null};				// last spot the opponent dropped, for the UI
    private int initialStacks[] = {0,0};			// remembers the number of starting pieces
    int lastProgressMove = 0;		// last move where a pawn was advanced
    int lastDrawMove = 0;			// last move where a draw was offered
    int robotDepth = 0;		// depth of robot search
  	private StateStack robotState = new StateStack();
  	private IStack robotCapture = new IStack();
  	
    CellStack occupiedCells[] = new CellStack[2];	// cells occupied, per color
   
	// factory method
	public MorrisCell newcell(char c,int r)
	{	return(new MorrisCell(c,r));
	}
    public MorrisBoard(String init,long rv,int np,RepeatedPositions rep,int map[]) // default constructor
    {   repeatedPositions = rep;
    	setColorMap(map);
        doInit(init,rv,np); // do the initialization 
     }


	public void sameboard(BoardProtocol f) { sameboard((MorrisBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if clone,digest and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(MorrisBoard from_b)
    {
    	super.sameboard(from_b);	// calls sameCell for each cell, also for inherited class variables.
    	G.Assert(unresign==from_b.unresign,"unresign mismatch");
       	G.Assert(AR.sameArrayContents(win,from_b.win),"win array contents match");
       	G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor contents match");
       	G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(sameCells(rack,from_b.rack),"rack mismatch");
        G.Assert(sameCells(captured,from_b.captured),"captured mismatch");
        G.Assert(dropState.sameContents(from_b.dropState),"dropState mismatch");
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject doesn't match");
        G.Assert(occupiedCells[FIRST_PLAYER_INDEX].size()==from_b.occupiedCells[FIRST_PLAYER_INDEX].size(),"occupiedCells mismatch");
        G.Assert(occupiedCells[SECOND_PLAYER_INDEX].size()==from_b.occupiedCells[SECOND_PLAYER_INDEX].size(),"occupiedCells mismatch");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Digest matches");

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
        long v = super.Digest();

		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,captureStack);
		v ^= Digest(r,occupiedCells[SECOND_PLAYER_INDEX].size());	// not completely specific because the stack can be shuffled
		v ^= Digest(r,occupiedCells[FIRST_PLAYER_INDEX].size());
		v ^= Digest(r,captured);
		v ^= Digest(r,rack);
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
        return (v);
    }
   public MorrisBoard cloneBoard() 
	{ MorrisBoard copy = new MorrisBoard(gametype,randomKey,players_in_game,repeatedPositions,getColorMap());
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((MorrisBoard)b); }


    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(MorrisBoard from_b)
    {	
        super.copyFrom(from_b);			// copies the standard game cells in allCells list
        pickedObject = from_b.pickedObject;	
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        dropState.copyFrom(from_b.dropState);
        getCell(captureStack,from_b.captureStack);
        getCell(occupiedCells,from_b.occupiedCells);
        AR.copy(playerColor,from_b.playerColor);
        copyFrom(captured,from_b.captured);
        copyFrom(rack,from_b.rack);
        board_state = from_b.board_state;
        lastProgressMove = from_b.lastProgressMove;
        lastDrawMove = from_b.lastDrawMove;
        unresign = from_b.unresign;
        repeatedPositions = from_b.repeatedPositions;
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
		rack = new MorrisCell[2];
		sample = new MorrisCell[2];
		captured = new MorrisCell[2];
		
    	Random r = new Random(67246765);
    	int map[]=getColorMap();
     	for(int i=0,pl=FIRST_PLAYER_INDEX;i<2; i++,pl=nextPlayer[pl])
    	{
     	occupiedCells[i] = new CellStack();
       	MorrisCell cell = new MorrisCell(r,RackLocation[i]);
       	MorrisChip chip = MorrisChip.getChip(i);
       	for(int j=0;j<9;j++) { cell.addChip(chip); }
       	int mi = map[i];
       	playerChip[mi] = chip;
     	rack[mi]=cell;
     	sample[mi] = new MorrisCell(r,MorrisId.Display);
     	sample[mi].addChip(chip);
     	captured[mi] = new MorrisCell(r,CaptureLocation[i]);
     	captured[mi].row = i;
     	}    
     	whitePool = rack[map[0]];
     	blackPool = rack[map[1]];
     	whiteCaptured = captured[map[0]];
     	blackCaptured = captured[map[1]];
     	variation = Variation.findVariation(gtype);
     	if(variation!=null)
     	{
     	switch(variation)
     	{
     	default:  throw G.Error("Unknown init named %s",gtype);
     	case Morris_9:
     		boardColumns = variation.size;
     		boardRows = variation.size;
     		initBoard(boardColumns,boardRows);

     		// ad hoc deconstruction of a regular 7x7 board into
     		// a morris board
     		for(int []pairs : RemoveCells_9)
     		{
     			removeCell(getCell((char)pairs[0],pairs[1]));
     		}
     		// add new links bypassing the cells that don't exist any more
     		for(int []pairs : AddLinks_9)
     		{	MorrisCell from = getCell((char)pairs[0],pairs[1]);
     			MorrisCell to = getCell((char)pairs[2],pairs[3]);
     			from.addLink(findDirection(from,to),to);
     			to.addLink(findDirection(to,from),from);
     		}     		
     		gametype = gtype;
     		break;
     	}}
     	else { G.Error(WrongInitError,gtype); }

        allCells.setDigestChain(r);
	    setState(MorrisState.Puzzle);
	    
	    lastProgressMove = 0;
	    lastDrawMove = 0;
	    robotDepth = 0;
	    robotState.clear();
	    robotCapture.clear();
	    whoseTurn = FIRST_PLAYER_INDEX;
		playerColor[FIRST_PLAYER_INDEX]=MorrisId.White_Chip_Pool;
		playerColor[SECOND_PLAYER_INDEX]=MorrisId.Black_Chip_Pool;
		initialStacks[FIRST_PLAYER_INDEX] = rack[FIRST_PLAYER_INDEX].height();
		initialStacks[SECOND_PLAYER_INDEX] = rack[SECOND_PLAYER_INDEX].height();
		acceptPlacement();
        AR.setValue(win,false);
        AR.setValue(lastDest,null);
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
        case AcceptPending:
        case DeclinePending:
        case DrawPending:
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
    	case Play:
    		return((moveNumber - lastProgressMove)>10);
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
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(MorrisState.Gameover);
    }
    public boolean gameOverNow() { return(board_state==MorrisState.Gameover); }
    public boolean winForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	// we maintain the wins in doDone so no logic is needed here.
    	if(board_state==MorrisState.Gameover) { return(win[player]); }
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
        droppedDestStack.clear();
        dropState.clear();
        captureStack.clear();
        pickedSourceStack.clear();
     }
    private int playerIndex(MorrisChip ch) { return(ch==playerChip[0] ? 0 : 1); }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    G.Assert(pickedObject==null, "nothing should be moving");
    if(droppedDestStack.size()>0)
    	{
    	MorrisCell dr = droppedDestStack.pop();
    	setState(dropState.pop());
     	switch(dr.rackLocation())
	    	{
	   		default: throw G.Error("Not expecting rackLocation %s",dr.rackLocation);
			case BoardLocation: 
				MorrisChip ch = pickedObject = dr.removeTop();
				occupiedCells[playerIndex(ch)].remove(dr,false); 
				break;
			case White_Captured:
			case Black_Captured:
			case White_Chip_Pool:	// treat the pools as infinite sources and sinks
			case Black_Chip_Pool:	
				pickedObject = dr.removeTop();
				break;	// don't add back to the pool
	    	
	    	}
	    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	MorrisChip po = pickedObject;
    	if(po!=null)
    	{
    		MorrisCell ps = pickedSourceStack.pop();
    		switch(ps.rackLocation())
    		{
    		default: throw G.Error("Not expecting rackLocation %s",ps.rackLocation);
    		case BoardLocation: 
    				ps.addChip(po);
    				occupiedCells[playerIndex(po)].push(ps);
    				break;
    		case White_Captured:
    		case Black_Captured:
    		case White_Chip_Pool:
    		case Black_Chip_Pool:	
    			ps.addChip(po);
    			break;	// don't add back to the pool
    		}
    		pickedObject = null;
     	}
     }

    // 
    // drop the floating object.
    //
    private void dropObject(MorrisCell c)
    {   G.Assert(pickedObject!=null,"pickedObject should not be null"); 	    		
    	switch(c.rackLocation())
		{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case BoardLocation:
			c.addChip(pickedObject);
			occupiedCells[playerIndex(pickedObject)].push(c);
	       	lastDest[whoseTurn]=c;
			break;
		case White_Captured:
		case Black_Captured:
		case White_Chip_Pool:
		case Black_Chip_Pool:	
			c.addChip(pickedObject);
			break;	// don't add back to the pool
		}
    	dropState.push(board_state);
       	droppedDestStack.push(c);
       	pickedObject = null;
    }
    
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(MorrisCell c)
    {	G.Assert(pickedObject==null,"pickedObject should be null");
    	switch(c.rackLocation())
    	{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case BoardLocation: 
			MorrisChip ch = pickedObject = c.removeTop();
			occupiedCells[playerIndex(ch)].remove(c,false);
			break;
		case Black_Captured:
		case White_Captured:
		case White_Chip_Pool:
		case Black_Chip_Pool:	
			pickedObject = c.removeTop();
			break;	// don't add back to the pool
    	
    	}
    	pickedSourceStack.push(c);
   }

    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(MorrisCell cell)
    {	return(droppedDestStack.top()==cell);
    }
    //
    // get the last dropped dest cell
    //
    public MorrisCell getDest() 
    { return(droppedDestStack.top()); 
    }
    
    public MorrisCell getPrevDest()
    {
    	return(lastDest[nextPlayer[whoseTurn]]);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  Returns +100 if a king is the moving object.
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	MorrisChip ch = pickedObject;
    	if(ch!=null)
    		{ return(ch.chipNumber());
    		}
        return (NothingMoving);
    }
    // get a cell from a partucular source
    public MorrisCell getCell(MorrisId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	return(getCell(col,row));
        case White_Captured:
        	return(whiteCaptured);
        case Black_Captured:
        	return(blackCaptured);
        case White_Chip_Pool:
       		return(whitePool);
        case Black_Chip_Pool:
       		return(blackPool);
        }
    }
    //
    // get the local cell which is the same role as c, which might be on
    // another instance of the board
    public MorrisCell getCell(MorrisCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
    public boolean isCapturingPosition(MorrisCell dest)
    {	MorrisChip top = dest.topChip();
    	for(int direction = dest.geometry.n-1; direction>=0; direction--)
    	{	MorrisCell next = dest.exitTo(direction);
    		if((next!=null)&&(next.topChip()==top))
    		{
    			MorrisCell opp = dest.exitTo(direction+CELL_HALF_TURN());
    			if((opp!=null)&&(opp.topChip()==top))
    					{
    					return(true);
    					}
    			MorrisCell cont = next.exitTo(direction);
    			if((cont!=null) && (cont.topChip()==top))
    			{
    				return(true);
    			}
    		}
    	}
    	return(false);
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
        case Place:
 			setState(isCapturingPosition(getDest())?MorrisState.Capture:MorrisState.Confirm);
			break;
        case Capture:
   			setState(MorrisState.Confirm);
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
    public boolean isSource(MorrisCell c)
    {	return(getSource()==c);
    }
    public MorrisCell getSource()
    {
    	return((pickedSourceStack.size()>0) ?pickedSourceStack.top() : null);
    }
    
    // the second source is the previous stage of a multiple jump
    public boolean isSecondSource(MorrisCell cell)
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
    	default: throw G.Error("Not expecting state %s",board_state);
    	case Gameover: 
    		break;
        case DrawPending:
        	lastDrawMove = moveNumber;
        	setState(MorrisState.AcceptOrDecline);
        	break;
    	case AcceptPending:
        case Draw:
        	setGameOver(false,false);
        	break;
        case Place:	
        case DeclinePending:
      	case Confirm:
        case Puzzle:
        case Play:
         	if(rack[whoseTurn].height()>0) { setState(MorrisState.Place); lastProgressMove = moveNumber; }
         	else if(occupiedCells[whoseTurn].size()<=2) { setGameOver(false,true); }
        	else if(hasSimpleMoves()) { setState(MorrisState.Play); }
        	else { setGameOver(false,true); }
        	break;

    	}

    }

    private void doDone(replayMode replay)
    {	MorrisCell src = getSource();
    	MorrisCell dest = getDest();
    	if((src!=null)&&(dest!=null)&&!(src.onBoard && dest.onBoard)) { lastProgressMove = moveNumber; }
     	acceptPlacement();
     	switch(board_state)
     	{
     	default: throw G.Error("Not expecting state %s",board_state);
     	case Draw:
     		setGameOver(false,false);
     		break;
     	case Resign: 
     		setGameOver(false,true);
     		break;
     	case Capture:
     		lastProgressMove = moveNumber; 
     		setNextStateAfterDone(); 
     		break;
     	case Confirm:
     	case DeclinePending:
     	case AcceptPending:
     	case DrawPending:
     		setNextPlayer(); 
     		setNextStateAfterDone(); 
     		break;
      	}
    }

    public boolean Execute(commonMove mm,replayMode replay)
    {	MorrisMovespec m = (MorrisMovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }
        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:
         	doDone(replay);
            break;
        case MOVE_CAPTURE:
	    	{
	    	MorrisCell dest = getCell(m.source,'@',0);
	    	MorrisCell src = getCell(MorrisId.BoardLocation,m.from_col,m.from_row);
	    	pickObject(src);
			dropObject(dest); 
			if(replay!=replayMode.Replay)
			{
			animationStack.push(src);
			animationStack.push(dest);
			}
			setNextStateAfterDrop();
			acceptPlacement();
	    	}
	    	break;        	
	    case MOVE_RACK_BOARD:
        	{
        	MorrisCell src = getCell(m.source,'@',0);
        	MorrisCell dest = getCell(MorrisId.BoardLocation,m.to_col,m.to_row);
        	pickObject(src);
    		dropObject(dest); 
    		if(replay!=replayMode.Replay)
			{
			animationStack.push(src);
			animationStack.push(dest);
			}
    		setNextStateAfterDrop();
    		acceptPlacement();
        	}
        	break;
        case MOVE_BOARD_BOARD:
        	{
        		G.Assert(pickedObject==null,"something is moving");
        		MorrisCell src = getCell(MorrisId.BoardLocation, m.from_col, m.from_row);
        		MorrisCell dest = getCell(MorrisId.BoardLocation,m.to_col,m.to_row);
        		pickObject(src);
        		dropObject(dest); 
        		if(replay!=replayMode.Replay)
        			{
        			animationStack.push(src);
        			animationStack.push(dest);
        			}
 			    setNextStateAfterDrop();
 			    acceptPlacement();
        	}
        	break;
        case MOVE_DROPB:
			{
			MorrisCell c = getCell(MorrisId.BoardLocation, m.to_col, m.to_row);
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
            		MorrisCell src = getSource();
            		dropObject(c);
            		setNextStateAfterDrop();
            		if(replay==replayMode.Single)
            			{
            			
            			if(src!=null)
            			{
            			animationStack.push(src);
            			animationStack.push(c);
            			}
            			}
            		}
			}
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	if(isDest(getCell(m.from_col,m.from_row)))
        		{ unDropObject(); 
        		}
        	else 
        		{ pickObject(getCell(MorrisId.BoardLocation, m.from_col, m.from_row));
        		  switch(board_state)
        		  {	default: throw G.Error("Not expecting pickb in state %s",board_state);
        		  	case Play:
        		  	case Capture:
         		  	case Puzzle:
        		  		break;
        		  }
         		}
 
            break;

        case MOVE_DROP: // drop on chip pool;
        	{
        	MorrisCell c = getCell(m.source, m.to_col, m.to_row);
        	if(isSource(c)) { unPickObject(); }
        	else
        	{
            dropObject(c);
            setNextStateAfterDrop();
            if(replay==replayMode.Single)
			{
			animationStack.push(getSource());
			animationStack.push(c);
			}}}
            break;


        case MOVE_PICK:
        	{
        	MorrisCell c = getCell(m.source, m.from_col, m.from_row);
        	if(isDest(c))
        	{
        		unDropObject();
        	}
        	else {
            pickObject(c);
            setNextStateAfterPick();
        	}}
            break;


        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(MorrisState.Puzzle);
            {	boolean win1 = winForPlayerNow(whoseTurn);
            	boolean win2 = winForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;
        case MOVE_OFFER_DRAW:
        	if(board_state==MorrisState.DrawPending) { setState(dropState.pop()); }
        	else { dropState.push(board_state);
        			setState(MorrisState.DrawPending);
        		}
        	break;
        case MOVE_ACCEPT_DRAW:
           	switch(board_state)
        	{	
        	case AcceptPending: 	// cancel accept and revert to neutral
        		setState(MorrisState.AcceptOrDecline); 
        		break;
           	case AcceptOrDecline:
           	case DeclinePending:	// accept pending
           		setState(MorrisState.AcceptPending); 
           		break;
        	default: throw G.Error("Not expecting %s",board_state);
        	}
           	break;
        case MOVE_DECLINE_DRAW:
        	switch(board_state)
        	{	
        	case DeclinePending:	// cancel decline and revert to neutral
        		setState(MorrisState.AcceptOrDecline); 
        		break;
        	case AcceptOrDecline:
        	case AcceptPending: setState(MorrisState.DeclinePending); break;
        	default: throw G.Error("Not expecting %s",board_state);
        	}
        	break;
        case MOVE_RESIGN:
        	setState(unresign==null?MorrisState.Resign:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            // standardize "gameover" is not true
            setState(MorrisState.Puzzle);
 
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
    public boolean LegalToHitChips(MorrisCell from,int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case Capture:
        	return((player==whoseTurn)
        				&& (pickedObject!=null)
        				&& ((from.rackLocation()==MorrisId.Black_Captured)
        						|| (from.rackLocation()==MorrisId.White_Captured))
        				);
        case Place:
        	return(isSource(from) 
        			|| ((player==whoseTurn)
        				&& (pickedObject==null) 
        				&& (from.height()>0))
        				&& ((from.rackLocation()==MorrisId.Black_Chip_Pool)
        						|| (from.rackLocation()==MorrisId.White_Chip_Pool))	
        			);
        case Confirm:
        	return(isDest(from));
        case Draw:
        case Play: 
		case Gameover:
		case Resign:
		case AcceptOrDecline:
		case DeclinePending:
		case DrawPending:
		case AcceptPending:
			return(false);
        case Puzzle:
        	return((pickedObject==null)
        			?(from.topChip()!=null)
        			:(playerChip[player]==pickedObject));
        }
    }
  
    public int adjustX(int column,int coord)
    {
    	switch(column)
    	{
    	case 0:	break;
    	case 1: coord *= 0.97; break;
    	case 2: coord *= 0.95; break;
    	case -1: coord *= 1.05; break;
    	case -2: coord *= 1.14; break;
    	default: break;
    	}
    	return(coord);
    }
    public int cellToX(MorrisCell c)
    {
    	int n = super.cellToX(c);
    	int cols = boardColumns/2;
    	int rel = c.col-('A'+cols);
    	return(adjustX(rel,n));
    	
    }
    
    public int adjustY(int column,int coord)
    {
    	switch(column)
    	{
    	case 0: break;
    	case 1: coord *= 0.97; break;
    	case 2: coord *= 0.95; break;
    	case -1: coord *= 1.05; break;
    	case -2: coord *= 1.14; break;
    	default: break;
    	}
    	return(coord);
    }
    
    public int cellToY(MorrisCell c)
    {
    	int n = super.cellToY(c);
    	int cols = boardRows/2;
    	int rel = c.row-cols-1;
    	return(adjustX(rel,n));
    	
    }
    public boolean legalToHitBoard(MorrisCell cell,Hashtable<MorrisCell,MorrisMovespec>targets)
    {	
        switch (board_state)
        {
 		case Play:
 		case Place:
 		case Capture:
 			return(isDest(cell)||isSource(cell)||targets.get(cell)!=null);
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
  public boolean canDropOn(MorrisCell cell)
  {		MorrisCell top = (pickedObject!=null) ? pickedSourceStack.top() : null;
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
    public void RobotExecute(MorrisMovespec m)
    {
        //G.print("R "+m);
    	G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        robotState.push(board_state);
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
    }
 

   //
   // un-execute a move.  The move should only be un-executed
   // in proper sequence, and if it was executed by the robot in the first place.
   // If you use monte carlo bots with the "blitz" option this will never be called.
   //
    public void UnExecute(MorrisMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	robotDepth--;
        setState(robotState.pop());
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
        case MOVE_CAPTURE:
        	{
        	MorrisCell dest = getCell(m.source,'@',1);
        	MorrisCell src = getCell(MorrisId.BoardLocation,m.from_col,m.from_row);
        	pickObject(dest);
        	dropObject(src);
        	acceptPlacement();
        	}
        	break;
        case MOVE_RACK_BOARD:
        	{
        	MorrisCell dest = getCell(MorrisId.BoardLocation,m.to_col,m.to_row);
        	MorrisCell src = getCell(m.source,'@',1);
        	pickObject(dest);
        	dropObject(src);
        	acceptPlacement();
        	}
        	break;
        case MOVE_BOARD_BOARD:
   			{
      			pickObject(getCell(MorrisId.BoardLocation, m.to_col, m.to_row));
      			MorrisCell from = getCell(MorrisId.BoardLocation, m.from_col,m.from_row);
   			    dropObject(from); 
   			    acceptPlacement();
        	}
        	break;

        case MOVE_RESIGN:
        case MOVE_ACCEPT_DRAW:
        case MOVE_DECLINE_DRAW:
        case MOVE_OFFER_DRAW:
            break;
        }
  }

private void loadHash(CommonMoveStack all,Hashtable<MorrisCell,MorrisMovespec>hash,boolean from)
{
	for(int lim=all.size()-1; lim>=0; lim--)
	{
		MorrisMovespec m = (MorrisMovespec)all.elementAt(lim);
		switch(m.op)
		{
		default: break;
		case MOVE_RACK_BOARD:
			if(!from) { hash.put(getCell(m.to_col,m.to_row),m); }
			break;
		case MOVE_CAPTURE:
			if(from) { hash.put(getCell(m.from_col,m.from_row),m); }
			break;
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
public Hashtable<MorrisCell,MorrisMovespec>getTargets()
{
	Hashtable<MorrisCell,MorrisMovespec>hash = new Hashtable<MorrisCell,MorrisMovespec>();
	CommonMoveStack all = new CommonMoveStack();

		switch(board_state)
		{
		default: break;
		case Capture:
		case Play:
		case Place:
			{	addMoves(all,whoseTurn);
				loadHash(all,hash,pickedObject==null);
			}
			break;
		}
	return(hash);
}
 public boolean addCaptureMoves(CommonMoveStack all,MorrisCell from,int who)
 {	 if(all==null) { return(true); }
 	all.push(new MorrisMovespec(MOVE_CAPTURE,from,captured[who].rackLocation(),who));
	 return(false);
 }
 public boolean addCaptureMoves(CommonMoveStack all,int who)
 {	OStack<MorrisCell> pieces = occupiedCells[nextPlayer[who]];
	for(int lim=pieces.size()-1; lim>=0; lim--)
	{	MorrisCell cell = pieces.elementAt(lim);
		addCaptureMoves(all,cell,who);
	}
	 return(false);
 }
 
 public boolean hasSimpleMoves()
 {
	 return(addSimpleMoves(null,whoseTurn));
 }

 // add normal checker moves
 // "all" can be null
 // return true if there are any.
 public boolean addSimpleMoves(CommonMoveStack all,int who)
 {	boolean some = false;
 	OStack<MorrisCell> pieces = occupiedCells[who];
 	for(int lim=pieces.size()-1; lim>=0; lim--)
 	{
 		MorrisCell cell = pieces.elementAt(lim);
 		some |= addSimpleMoves(all, cell,who);
 		if(some && (all==null))  { return(true); }
 	}
 	return(some);
 }
 
 public boolean addPlacementMoves(CommonMoveStack all,MorrisCell cell,int who)
 {	boolean some = false; 	
	for(MorrisCell c = allCells; c!=null; c=c.next)
	{
		if(c.topChip()==null)
		{
			if(all==null) { return(true); }
			all.push(new MorrisMovespec(MOVE_RACK_BOARD,cell.rackLocation(),c,who));
			some = true;
		}
	}
	 return(some);
 }
 

 // add normal checker moves
 // "all" can be null
 // return true if there are any.
 public boolean addPlacementMoves(CommonMoveStack all,int who)
 {	MorrisCell cell = rack[who];
 	boolean some = addPlacementMoves(all, cell,who);
 	return(some);
 }

 // add normal checker moves from a particular cell
 // "all" can be null
 // return true if there are any.
 public boolean addSimpleMoves(CommonMoveStack all,MorrisCell cell,int who)
 {	boolean some = false; 	
	boolean anywhere = occupiedCells[who].size()<=3;
	if(anywhere)
	{	// endgame, only 3 men left
		for(MorrisCell dest = allCells; dest !=null; dest = dest.next)
		{
			if((dest!=cell) && (dest.topChip()==null))
			{
				if(all==null) { return(true); }
				some |= true;
				all.push(new MorrisMovespec(MOVE_BOARD_BOARD,cell,dest,who));
			}
		}
	}
	else
	{	// normal, only move adjacent
 	for(int direction = cell.geometry.n-1; direction>=0;direction--)
 		{	MorrisCell next = cell.exitTo(direction);
 			if((next!=null) && (next.topChip()==null))
 					{
 					if(all==null) { return(true); }
 					some |= true;
 					all.push(new MorrisMovespec(MOVE_BOARD_BOARD,cell,next,who));
 					}
 		}}
	 return(some);
 }
 
 public boolean addMoves(CommonMoveStack all,int who)
 {	boolean some = false;
 	switch(variation)
	 {
	 default: throw G.Error("Not expecting %s",variation); 
 	 case Morris_9:
 		 switch(board_state)
 		 {
 		 default: throw G.Error("Not expecting state %s",board_state);
 		 case AcceptOrDecline:
 			 all.push(new MorrisMovespec(MOVE_ACCEPT_DRAW,whoseTurn));
 			 all.push(new MorrisMovespec(MOVE_DECLINE_DRAW,whoseTurn));
 			 break;
 		 case Place:
 			 if(pickedObject==null)
 			 {
 			 some = addPlacementMoves(all,whoseTurn); 
 			 }
 			 else
 			 {	// something is already moving
 			 some = addPlacementMoves(all,getSource(),whoseTurn()); 
 			 } 			 
 			 break;
 		 case Play:
 			 if(pickedObject==null)
 			 {
 			 some = addSimpleMoves(all,whoseTurn); 
 			 }
 			 else
 			 {	// something is already moving
 			 some = addSimpleMoves(all,getSource(),whoseTurn()); 
 			 }
 			 
 			if( ((moveNumber-lastProgressMove)>8)
 					 && ((moveNumber-lastDrawMove)>4))
 			 {
 				 all.push(new MorrisMovespec(MOVE_OFFER_DRAW,whoseTurn));
 			 }
 			 break;
 			 
 		 case Capture:
  		 {	
 			 if(pickedObject==null)
 			 {
 			 some = addCaptureMoves(all,whoseTurn);
 			 }
 			 else
 			 {	// something is already moving
 			 some = addCaptureMoves(all,getSource(),whoseTurn);
  			 }

 			 break;
 		 }
 		 }
	 }
 	return(some);
 }
 CommonMoveStack GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	addMoves(all,whoseTurn);
 	return(all);
 }
 

}
