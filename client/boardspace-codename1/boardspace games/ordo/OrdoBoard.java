/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.

    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/.
 */
package ordo;

import online.game.*;
import ordo.OrdoConstants.OrdoId;
import ordo.OrdoConstants.OrdoState;
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
	static int REVISION = 101;			// revision numbers start at 100
										// revision 101 adds protection against multiple draw offers
    static final int White_Chip_Index = 0;
    static final int Black_Chip_Index = 1;
    static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
    static final OrdoId RackLocation[] = { OrdoId.White_Chip_Pool,OrdoId.Black_Chip_Pool};
	public int getMaxRevisionLevel() { return(REVISION); }
	
    public void SetDrawState() { setState(OrdoState.Draw); }
    private int sweep_counter = 0;
    public OrdoCell rack[] = null;	// the pool of chips for each player.  
    public int goalRow[] = {0,0};		// will be the row checkers get promoted to king
    public CellStack animationStack = new CellStack();
    //
    // private variables
    //
    private RepeatedPositions repeatedPositions = null;		// shared with the viewer
    private OrdoState board_state = OrdoState.OrdoPlay;	// the current board state
    private OrdoState unresign = null;					// remembers the previous state when "resign"
    private OrdoState resetState = null;
    Variation variation = Variation.Ordo;
    private boolean sidewaysMove = false;		// last ordo move was sideways
    private boolean equalOrdo = false;			// last ordo move left equal ordo groups
    private CellStack captureStack = new CellStack();
    private IStack captureSize = new IStack();
    
    public OrdoPlay robot = null;
    
  	int lastPlacedIndex = 0;
 
    public boolean p1(String msg)
   	{
   		if(G.p1(msg) && robot!=null)
   		{	String dir = "g:/share/projects/boardspace-html/htdocs/ordo/ordogames/robot/";
   			robot.saveCurrentVariation(dir+msg+".sgf");
   			return(true);
   		}
   		return(false);
   	}
    
    public OrdoState getState() { return(board_state); } 
	public void setState(OrdoState st) 
	{ 	unresign = (st==OrdoState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
    private OrdoId playerColor[]={OrdoId.White_Chip_Pool,OrdoId.Black_Chip_Pool};
 	public OrdoId getPlayerColor(int p) { return(playerColor[p]); }
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public OrdoChip pickedObject = null;
    public OrdoChip lastDropped = null;
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    
    OrdoCell selectedStart = null;
    OrdoCell selectedEnd = null;
    OrdoCell selectedDest = null;
    public OrdoCell lastDest[] = {null,null};				// last spot the opponent dropped, for the UI
    int lastProgressMove = 0;		// last move where a pawn was advanced
    int lastDrawMove = 0;			// last move where a draw was offered
    int robotDepth = 0;		// current depth of robot search.  This is used to make faster wins look better
    						// than slower wins.  It's part of the board so multiple threads have independent values.
  	private StateStack robotState = new StateStack();
  	private IStack robotData = new IStack();
  	
     CellStack occupiedCells[] = new CellStack[2];	// cells occupied, per color
    
    
    private int ForwardOrdo[][] = {{CELL_UP_RIGHT,CELL_UP,CELL_UP_LEFT},
    		{CELL_DOWN_LEFT,CELL_DOWN,CELL_DOWN_RIGHT}};
    private int ForwardOrSidewaysOrdo[][] =
    		{{CELL_LEFT,CELL_UP_RIGHT,CELL_UP,CELL_UP_LEFT,CELL_RIGHT},
    		{CELL_LEFT,CELL_DOWN_LEFT,CELL_DOWN,CELL_DOWN_RIGHT,CELL_RIGHT}};
    private int AllOrdo[] = {CELL_LEFT,CELL_UP_RIGHT,CELL_UP,CELL_UP_LEFT,CELL_RIGHT,CELL_DOWN_LEFT,CELL_DOWN,CELL_DOWN_RIGHT};
	// factory method to create a new cell as part of the board
	public OrdoCell newcell(char c,int r)
	{	return(new OrdoCell(c,r));
	}
    public OrdoBoard(String init,long rv,int np,RepeatedPositions rep,int map[],int rev) // default constructor
    {   repeatedPositions = rep;

    	setColorMap(map, np);
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
     	G.Assert(sidewaysMove==from_b.sidewaysMove,"sidewaysMove mismatch");
     	G.Assert(equalOrdo==from_b.equalOrdo,"equalOrdo mismatch");
       	G.Assert(AR.sameArrayContents(win,from_b.win),"win array contents match");
       	G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor contents match");
       	G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(sameCells(captureStack,from_b.captureStack),"captureStack mismatch");
        G.Assert(sameContents(captureSize,from_b.captureSize),"captureSize mismatch");
        
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject doesn't match");
        G.Assert(occupiedCells[FIRST_PLAYER_INDEX].size()==from_b.occupiedCells[FIRST_PLAYER_INDEX].size(),"occupiedCells mismatch");
        G.Assert(occupiedCells[SECOND_PLAYER_INDEX].size()==from_b.occupiedCells[SECOND_PLAYER_INDEX].size(),"occupiedCells mismatch");
        G.Assert(sameCells(selectedStart,from_b.selectedStart),"selectedStart mismatch");
        G.Assert(sameCells(selectedEnd,from_b.selectedEnd),"selectedEnd mismatch");
        G.Assert(sameCells(selectedDest,from_b.selectedDest),"selectedDest mismatch");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");

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
		v ^= Digest(r,selectedStart);
		v ^= Digest(r,selectedEnd);
		v ^= Digest(r,selectedDest);
		v ^= Digest(r,sidewaysMove);
		v ^= Digest(r,equalOrdo);
		v ^= Digest(r,captureStack);
		v ^= Digest(r,captureSize);
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
        getCell(occupiedCells,from_b.occupiedCells);
        AR.copy(playerColor,from_b.playerColor);
        board_state = from_b.board_state;
        lastProgressMove = from_b.lastProgressMove;
        lastDrawMove = from_b.lastDrawMove;
        selectedStart = getCell(from_b.selectedStart);
        selectedEnd = getCell(from_b.selectedEnd);
        selectedDest = getCell(from_b.selectedDest);
        unresign = from_b.unresign;
        resetState = from_b.resetState;
        sidewaysMove = from_b.sidewaysMove;
        equalOrdo = from_b.equalOrdo;
        repeatedPositions = from_b.repeatedPositions;
        getCell(captureStack,from_b.captureStack);
        captureSize.copyFrom(from_b.captureSize);
        copyFrom(rack,from_b.rack);
        robot = from_b.robot;
        lastPlacedIndex = from_b.lastPlacedIndex;
        if(G.debug()) { sameboard(from_b); }
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
     		goalRow[map[FIRST_PLAYER_INDEX]] = nrows;
     		goalRow[map[SECOND_PLAYER_INDEX]] = 1;
     		gametype = gtype;
     		break;
     	}

	    setState(OrdoState.Puzzle);
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
	    sidewaysMove = false;
	    equalOrdo = false;
	    robotDepth = 0;
	    robotState.clear();
	    robotData.clear();
	    whoseTurn = FIRST_PLAYER_INDEX;
		playerColor[map[FIRST_PLAYER_INDEX]]=OrdoId.White_Chip_Pool;
		playerColor[map[SECOND_PLAYER_INDEX]]=OrdoId.Black_Chip_Pool;
		acceptPlacement();
		captureStack.clear();
		captureSize.clear();
        AR.setValue(win,false);
        moveNumber = 1;
	    lastProgressMove = 0;
	    lastDrawMove = 0;
	    robotDepth = 0;
	    lastPlacedIndex = 1;

        // note that firstPlayer is NOT initialized here
    }

    public double simpleScore(int who)
    {	// consider 3 factors, compactness, number of chips, and advancement toward the goal
    	CellStack cells = occupiedCells[who];
    	int nCells = cells.size();
    	int goal = goalRow[who];
    	int minRow = goal;
    	int maxRow = 1;
    	int minCol = ncols;
    	int maxCol = 1;
    	for(int lim = nCells-1; lim>=0; lim--)
    	{
    		OrdoCell c = cells.elementAt(lim);
    		int row = Math.abs(c.row-goal);		// distance from goal
    		int col = c.col-'A';
    		minRow = Math.min(minRow,row);
    		maxRow = Math.max(maxRow,row);
    		minCol = Math.min(minCol,col);
    		maxCol = Math.max(maxCol,col);   		
    	}
    	double dim = Math.max(maxCol-minCol,maxRow-minRow);
    	double cluster = 1-((dim*dim)/(double)(ncols*nrows));
    	double goalish = (double)(nrows-maxRow)/nrows;
    	double sizeish = (20-nCells)/20.0;
    	
    	return sizeish*0.1 + goalish *0.5 + cluster*0.4;
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
       	case DrawPending: return true;
       	 
    	case AcceptOrDecline:
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
    	setState(OrdoState.Gameover);
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
        droppedDestStack.clear();
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
    	switch(dr.rackLocation())
	    	{
	   		default: throw G.Error("Not expecting rackLocation %s",dr.rackLocation);
			case BoardLocation: 
				unDropObject(dr);
				break;
			case White_Chip_Pool:	// treat the pools as infinite sources and sinks
			case Black_Chip_Pool:	
				pickedObject = dr.topChip();
				break;	// don't add back to the pool
	    	
	    	}
	    	}
    }
    private void unDropObject(OrdoCell dr)
    {
    	OrdoChip ch = pickedObject = dr.removeTop();
		occupiedCells[playerIndex(ch)].remove(dr,false); 
		dr.lastPlaced = dr.previousLastPlaced;
		lastPlacedIndex--;
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
    				ps.lastEmptied = -1;
    				occupiedCells[playerIndex(po)].push(ps);
    				break;
    		case White_Chip_Pool:
    		case Black_Chip_Pool:	break;	// don't add back to the pool
    		}
    		pickedObject = null;
     	}
     }
    private void unPickObject(OrdoCell c)
    {
    	c.addChip(pickedObject);
    	c.lastContents = c.previousLastContents;
    	c.lastEmptied = c.previousLastEmptied;
    	occupiedCells[playerIndex(pickedObject)].push(c);
    	pickedObject = null;
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
			c.previousLastPlaced = c.lastPlaced;
			c.lastPlaced = lastPlacedIndex;
			lastPlacedIndex++;
			break;
		case White_Chip_Pool:
		case Black_Chip_Pool:	break;	// don't add back to the pool
		}
       	droppedDestStack.push(c);
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
			c.previousLastContents = c.lastContents;
			c.lastContents =ch;
			c.previousLastEmptied = c.lastEmptied;
			c.lastEmptied = lastPlacedIndex;
	
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
    public boolean isADest(OrdoCell cell)
    {
    	return droppedDestStack.contains(cell);
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
    public OrdoCell getCell(OrdoId source,char col,int row)
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
        case OrdoPlay2:
        case Reconnect:
 			setState(OrdoState.Confirm);
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
    	OrdoCell home = getCell('A',goalRow[who]);
    	OrdoChip target = playerChip(who);
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
        	setState(OrdoState.AcceptOrDecline);
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
    			else if(raceGoalOccupied(nextPlayer[whoseTurn])) { setGameOver(false,true); }
    			else if(isOrdoConnected(whoseTurn,null,null)) 
    				{ // in ordoX, we get connected by removing groups
    				setState(OrdoState.OrdoPlay); 
    				}
    			else if(hasReconnectionMoves(whoseTurn)) 
    					{ setState(OrdoState.Reconnect); 
    					}
    			else { setGameOver(false,true); }
    			break;
    			
    		case OrdoX:
       			if(occupiedCells[whoseTurn].size()==0) { setGameOver(false,true); }
    			else if(sidewaysMove) { setState(OrdoState.OrdoPlay2); }
    			else if(raceGoalOccupied(nextPlayer[whoseTurn])) { setGameOver(false,true); }
       			else if(equalOrdo) { setState(OrdoState.OrdoRetain); equalOrdo = false; }
    			else { setState(OrdoState.OrdoPlay); }
       			sidewaysMove = false;
    			break;
    		default:
    			throw G.Error("Not implemented");
    			   		
    		}
    	}
       	resetState = board_state;

    }

    private void doDone(replayMode replay)
    {	
    	lastDest[whoseTurn] = droppedDestStack.top();   	
    	lastPlacedIndex++;
     	acceptPlacement();
 
        if (board_state==OrdoState.Resign)
        {	setGameOver(false,true);
        }
        else
        {	if(!sidewaysMove && resetState!=OrdoState.OrdoRetain)
        		{ setNextPlayer(replay); 
        		} 
        	setNextStateAfterDone(); 

        }
    }
    
    private void doRetain(OrdoCell from,int who,replayMode replay)
    {
    	sweep_counter++;
    	captureSize.push(captureStack.size());
    	// this has the effect of marking all the cells in the group with sweep_counter;
    	groupSize(from,from.topChip());
    	CellStack cells = occupiedCells[who];
    	for(int lim=cells.size()-1; lim>=0; lim--)
    	{
    		OrdoCell c = cells.elementAt(lim);
    		if(c.sweep_counter!=sweep_counter) 
    		{ doCapture(c); 
    		if(replay.animate)
    		{
    			animationStack.push(c);
    			animationStack.push(rack[who]);
    		}}
    	}
    	
    	
    }

    private void doCapture(OrdoCell mid)
    {	
    	OrdoChip ch = mid.removeTop();
    	int capee = playerIndex(ch);
    	mid.lastContents = ch;
    	mid.lastCaptured = lastPlacedIndex;
    	
		occupiedCells[capee].remove(mid,false);
		captureStack.push(mid);
    }
    private void undoCapture(OrdoChip chip)
    {
    	OrdoCell c = captureStack.pop();
    	int capee = playerIndex(chip);
    	c.addChip(chip);
    	occupiedCells[capee].push(c);
    }
    private void undoOrdoMove(OrdoCell toStart,OrdoCell toEnd,OrdoCell from)
    {
    	int direction = findDirection(toStart,toEnd);
		boolean exit = false;
		do { 
			unDropObject(from);
			unPickObject(toStart);
			 
			 pickedObject = null;
			exit = toStart==toEnd;
			toStart = toStart.exitTo(direction);
			from = from.exitTo(direction);      				 
		} while(!exit);

		droppedDestStack.clear();
		pickedSourceStack.clear();
    }
    
    public boolean Execute(commonMove mm,replayMode replay)
    {	OrdoMovespec m = (OrdoMovespec)mm;
    	//checkOccupied();
        if(replay.animate) { animationStack.clear(); }
        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_RETAIN:
        	doRetain(getCell(m.from_col,m.from_row),whoseTurn,replay);
        	setState(OrdoState.Confirm);
        	break;
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
        		{ if(isADest(c)) 
        			{
        			unDropObject();
        			unPickObject();
        			setState(resetState);
        			}
        		else 
        			{selectedEnd = c; 
        			}
        		}
        	else if(selectedDest==null) { selectedEnd = c; }
        	else { // this is the undo for an ordo move
        			undoOrdoMove(selectedStart,selectedEnd,selectedDest);
        			selectedDest = null;
        			setState(resetState);
        		}
        	}
        	break;
        case MOVE_CAPTURE:	// ordo capturing move
        	{	captureSize.push(captureStack.size());
        		OrdoCell src = getCell(OrdoId.BoardLocation, m.from_col, m.from_row);
    			OrdoCell dest = getCell(OrdoId.BoardLocation,m.to_col,m.to_row);
    			
    			
    			pickObject(src);
    			doCapture(dest);
    			dropObject(dest);
    			lastProgressMove = moveNumber;
    			if(replay.animate)
    			{
    				animationStack.push(dest);
    				animationStack.push(rack[whoseTurn^1]);
    				animationStack.push(src);
    				animationStack.push(dest);
    			}
    			sidewaysMove = (variation==Variation.OrdoX) && m.from_row==m.to_row;
    			

    			if((variation==Variation.OrdoX) 
    					&& !isOrdoConnected(nextPlayer[whoseTurn],null,null))
    			{
    				int bigsize = 0;
    				int nbig = 0;
    				int nextp = nextPlayer[whoseTurn];
    				OrdoChip chip = playerChip(nextp);
    				CellStack cells = occupiedCells[nextp];
    				sweep_counter++;
    				// in the case of a second move, we may have already
    				// disconnected and the big group may not be adjacent to the move
    				// so we have to look at all cells.
    				for(int lim=cells.size()-1; lim>=0; lim--)
    				{
    					OrdoCell d = cells.elementAt(lim);
    					if(d!=null && (d.topChip()==chip))
    					{
    						int sz = groupSize(d,chip);
    						if(sz>bigsize) { bigsize=sz; nbig=1; }
    						else if(sz==bigsize) { nbig++; }
    					}
    				}
    				equalOrdo = nbig>1;
    				//if(robot!=null && equalOrdo && nbig>2) 	{ p1("equal3 "+moveNumber); }
    				sweep_counter++;
    				for(int direction=0;direction<dest.geometry.n;direction++)
    				{
    					OrdoCell d = dest.exitTo(direction);
    					if(d!=null && (d.sweep_counter!=sweep_counter) && (d.topChip()==chip))
    					{
    						int sz = groupSize(d,chip);
    						if(sz<bigsize) { removeGroup(d,chip,replay); }
    					}
    				}
    				
  				
    			}
        	}
        	setState(OrdoState.Confirm);
        	break;
        case MOVE_ORDO:	// ordo block move
        	{
        		OrdoCell from = getCell(m.from_col,m.from_row);
        		OrdoCell to = getCell(m.to_col,m.to_row);
        		OrdoCell target = getCell(m.target_col,m.target_row);
        		if(target.row!=from.row) { lastProgressMove = moveNumber; }
        		int dir = findDirection(from,target);
        		sidewaysMove = (variation==Variation.OrdoX) && m.from_row==m.to_row;

        		{
        		boolean exit = false;
        		selectedDest = to;	// in case of undo
        		do {
        			pickObject(from);
        			dropObject(to);	// this might overlap and temporarily create a stack of 2
        			if(replay.animate) {
        				animationStack.push(from);
        				animationStack.push(to);
        			}
        			exit = (from==target);
        			from = from.exitTo(dir);
        			to = to.exitTo(dir);
        		} while(!exit);
        		}
            	
        		setState(OrdoState.Confirm);
        	}
        	break;
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting state %s",board_state);
        		case Gameover:	// not legal, but permissive for damaged game OR-ddyer-bobc-2022-09-07-0143
        		case OrdoPlay:
        		case OrdoPlay2:
        		case Reconnect:
        			G.Assert(pickedObject==null,"something is moving");
        			OrdoCell src = getCell(OrdoId.BoardLocation, m.from_col, m.from_row);
        			OrdoCell dest = getCell(OrdoId.BoardLocation,m.to_col,m.to_row);
            		if(src.row!=dest.row) { lastProgressMove = moveNumber; }
            		pickObject(src);
        			dropObject(dest); 
        			sidewaysMove = (variation==Variation.OrdoX) && m.from_row==m.to_row;
        			if(replay.animate)
        			{
        				animationStack.push(src);
        				animationStack.push(dest);
        			}
 				    setNextStateAfterDrop(replay);
        			break;
        	}
        	break;
    	
        case MOVE_DROPB:
			{
			lastDropped = pickedObject;
			OrdoCell c = getCell(OrdoId.BoardLocation, m.to_col, m.to_row);
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
        		{ pickObject(getCell(OrdoId.BoardLocation, m.from_col, m.from_row));
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
            setState(OrdoState.Puzzle);
            {	boolean win1 = winForPlayerNow(whoseTurn);
            	boolean win2 = winForPlayerNow(whoseTurn^1);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;
        case MOVE_OFFER_DRAW:
        	{
        	if(board_state==OrdoState.DrawPending) { setState(resetState); }
        	else { 	setState(OrdoState.DrawPending);
        		}
        	}
        	break;
        case MOVE_ACCEPT_DRAW:
           	switch(board_state)
        	{	
        	case AcceptPending: 	// cancel accept and revert to neutral
        		setState(OrdoState.AcceptOrDecline); 
        		break;
           	case AcceptOrDecline:
           	case DeclinePending:	// accept pending
           		setState(OrdoState.AcceptPending); 
           		break;
        	default: throw G.Error("Not expecting %s",board_state);
        	}
           	break;
        case MOVE_DECLINE_DRAW:
        	switch(board_state)
        	{	
        	case DeclinePending:	// cancel decline and revert to neutral
        		setState(OrdoState.AcceptOrDecline); 
        		break;
        	case AcceptOrDecline:
        	case AcceptPending: setState(OrdoState.DeclinePending); break;
        	default: throw G.Error("Not expecting %s",board_state);
        	}
        	break;
        case MOVE_RESIGN:
        	setState(unresign==null?OrdoState.Resign:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            // standardize "gameover" is not true
            setState(OrdoState.Puzzle);
 
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
		case OrdoPlay2:
		case OrdoRetain:
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
 		case OrdoRetain:
 		case OrdoPlay2:
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
			return(isADest(cell));
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
    public void RobotExecute(commonMove m)
    {
        //G.print("R "+m);
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        robotState.push(board_state);
        robotState.push(resetState);
        robotData.push( equalOrdo ? 1 : 0);
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
   // un-execute a move.  The move will only be un-executed
   // in proper sequence, and if it was executed by the robot in the first place.
   // If you use monte carlo bots with the "blitz" option this will never be called.
   //
    public void UnExecute(commonMove m0)
    {	//checkOccupied();
        //G.print("U "+m+" for "+whoseTurn);
    	OrdoMovespec m = (OrdoMovespec)m0;
    	robotDepth--;
        resetState = robotState.pop();
        setState(robotState.pop());
        int data = robotData.pop();
        equalOrdo = (data&1)!=0;
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
       case MOVE_ORDO:
       		{	OrdoCell from = getCell(m.from_col,m.from_row);
       			OrdoCell fromEnd = getCell(m.target_col,m.target_row);
       			OrdoCell to = getCell(m.to_col,m.to_row);
       			undoOrdoMove(from,fromEnd,to);
       			acceptPlacement();
       		}
       		break;
       case MOVE_RETAIN:
       		{
       			int sz = captureSize.pop();
       			OrdoChip chip = playerChip(whoseTurn);
       			while(captureStack.size()>sz) { undoCapture(chip); }
       		}
       		break;
       case MOVE_CAPTURE:
			{	int sz = captureSize.pop();
				OrdoCell to = getCell(OrdoId.BoardLocation, m.to_col, m.to_row);
				pickObject(to);
				OrdoCell from = getCell(OrdoId.BoardLocation, m.from_col,m.from_row);
				dropObject(from); 
				int np = nextPlayer[whoseTurn];
				OrdoChip chip = playerChip(np);
				while(captureStack.size()>sz) { undoCapture(chip); }
				acceptPlacement();
			}
			break;
       case MOVE_BOARD_BOARD:
			{
    			pickObject(getCell(OrdoId.BoardLocation, m.to_col, m.to_row));
    			OrdoCell from = getCell(OrdoId.BoardLocation, m.from_col,m.from_row);
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
					OrdoMovespec prev = hash.get(to);
					boolean doit = true;
					if(prev!=null)
					{
					boolean currentParallel = (m.from_col==m.to_col) || (m.from_row==m.to_row);
					boolean prevParallel = (prev.from_col==prev.to_col || prev.from_row==prev.to_row);
					// the existing move is a parallel move.  prefer parallel moves
					// so the default will orthogonal rather than diagonal
					// if both are parallel, prefer the smaller move.
					if(!currentParallel && prevParallel) { doit=false; }
					if(currentParallel 
							&& prevParallel
							&& (G.distanceSQ(m.from_col,prev.from_row,m.to_col,m.to_row)
									>= G.distanceSQ(prev.from_col,prev.from_row,prev.to_col,prev.to_row)))
					{ doit = false; }
					}
					if(doit)
					{					
					hash.put(to,m);
					}
					
					exit = from==target;
					from = from.exitTo(direction);
					to = to.exitTo(direction);
				} while (!exit);			
			}}
			break;
		case MOVE_CAPTURE:
		case MOVE_RETAIN:
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
		case OrdoPlay2:
		case OrdoRetain:
		case Reconnect:
			{	addMoves(all,true,whoseTurn);
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
	 		return allowBackwards ? AllOrdo : allowSideways ? ForwardOrSidewaysOrdo[who] : ForwardOrdo[who] ; 
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
 private void addRetainMoves(CommonMoveStack all,int who)
 {	CellStack cells = occupiedCells[who];
 	for(int lim=cells.size()-1; lim>=0; lim--)
 	{
 		OrdoCell c = cells.elementAt(lim);
 		all.push(new OrdoMovespec(MOVE_RETAIN,c,c,who));
 	}
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
 
 static int ExtendUpAndRightDirection[] = { CELL_UP, CELL_RIGHT };
 static int ExtendOrthogonalDirections[] = { CELL_UP, CELL_DOWN, CELL_LEFT, CELL_RIGHT };
 static int HorizontalDirections[] = { CELL_LEFT, CELL_RIGHT};
 static int VerticalDirections[] = { CELL_UP, CELL_DOWN };
 static int UpDirection[][] = {{ CELL_UP },{CELL_DOWN}};

 private boolean ExtendUpRight(CommonMoveStack all,OrdoCell from,
		 	boolean allowSideways,boolean reconnect,boolean reverse,int who)
 {	boolean some = false;
	 // from is the start of the ordo chain to move. we extend the chain in up and right directions
	 for(int extendDirection : reverse ? ExtendOrthogonalDirections : ExtendUpAndRightDirection)
	 {	boolean extendHorizontal = (extendDirection==CELL_LEFT) || (extendDirection==CELL_RIGHT);
	 	int movementDirections[] = 
	 			 (variation==Variation.OrdoX) 
	 					? ((board_state==OrdoState.OrdoPlay2) ?  ForwardOrdo[who] : ForwardOrSidewaysOrdo[who]) 
	 					: extendHorizontal 
	 						? (reverse ? VerticalDirections : UpDirection[who]) 
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
		int moveDirections[] = (variation==Variation.OrdoX) 
								? ((board_state==OrdoState.OrdoPlay2) ? ForwardOrdo[who] : ForwardOrSidewaysOrdo[who])
								: (selectedStart.row==selectedEnd.row)		// horizontal ordo 
									? (reconnect ? VerticalDirections : UpDirection[who])
									: HorizontalDirections;
		some |= addOrdoMoves(all,selectedStart,direction,distance,moveDirections,who); 
	 }
	 else if(selectedStart!=null)
	 {	
		// one end specified
		some |= ExtendUpRight(all,selectedStart,allowSideways,reconnect,true,who); 
	 }
	 else
	 {
	 CellStack sources = occupiedCells[who];
	 for(int lim=sources.size()-1; lim>=0; lim--)
	 {
		OrdoCell from = sources.elementAt(lim);
		some |= ExtendUpRight(all,from,allowSideways,reconnect,false,who);
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
 // this uses a simple recursive descent to find everything that's connected
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
 	if(h==0) { return true; }	// will be gameover for other reasons
 	OrdoCell seed = full!=null ? full : someOrdoCell(stack,empty);
 	int sz = connectedSize(seed,playerChip(who),empty,full);
 	return sz>=h;
 }
 // sweep counter has been incremented, count those who have a different sweep
 // and match the top
 private int groupSize(OrdoCell from,OrdoChip chip)
 {
	 return connectedSize(from,chip,null,null);
 }
 private void removeGroup(OrdoCell from,OrdoChip chip,replayMode replay)
 {	int who = playerIndex(chip);
	 if(from!=null && from.topChip()==chip) 
	 	{ doCapture(from);
	 	  if(replay.animate)
	 	  {
	 		  animationStack.push(from);
	 		  animationStack.push(rack[who]);
	 	  }
	 	  for(int direction = 0; direction<from.geometry.n; direction++)
	 	  {
	 		  removeGroup(from.exitTo(direction),chip,replay);
	 	  }
	 	}
	 
	 
 }
 private boolean hasReconnectionMoves(int who)
 {
	 return addOrdoSingleReconnectMoves(null,who)
			 || addOrdoMoves(null,who,true,true);
 }

 private boolean addOrdoSingleReconnectMoves(CommonMoveStack all,int who)
 {	boolean some = false;
 
 	if(selectedStart!=null)
	 {
		 some = addSimpleOrdoMoves(all,selectedStart,selectedStart.topChip(),true,true,who); 
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

 private boolean addMoves(CommonMoveStack all,boolean offerdraw,int who)
 {	boolean some = false;
 	switch(variation)
	 {
	 default: throw G.Error("Not expecting %s",variation); 
	 case Ordo:
	 case OrdoX:
		 switch(board_state)
		 {
		 default: 
			 p1("state "+board_state+" unexpected");
			 throw G.Error("State %s not expected",board_state);
 		 case AcceptOrDecline:
 			if(drawIsLikely()) { all.push(new OrdoMovespec(MOVE_ACCEPT_DRAW,whoseTurn)); }
 			 all.push(new OrdoMovespec(MOVE_DECLINE_DRAW,whoseTurn));
 			 break;
 		 case DeclinePending:
 		 case DrawPending:
 		 case AcceptPending:
		 case Confirm:
			 all.push(new OrdoMovespec(MOVE_DONE,who));
			 break;
		 case OrdoRetain:
			 addRetainMoves(all,who);
			 break;
		 case OrdoPlay:
			 addOrdoSingletonMoves(all,who,true,false);
			 addOrdoMoves(all,who,true,false);
			 if( offerdraw 
					 && canOfferDraw()
					 && ((moveNumber-lastProgressMove)>8))
			 {
				 all.push(new OrdoMovespec(MOVE_OFFER_DRAW,who));
			 }
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
 CommonMoveStack  GetListOfMoves(boolean offerdraw)
 {	CommonMoveStack all = new CommonMoveStack();
 	addMoves(all,offerdraw,whoseTurn);
 	if(all.size()==0) 
 		{ p1("no moves "+moveNumber); 
 		}
 	return(all);
 }
 public void initRobot(OrdoPlay bot)
 {
	 robot = bot;
 }

public commonMove Get_Random_Move(Random rand,double ordoProbability) 
{	CellStack sources = occupiedCells[whoseTurn];
	int sz = sources.size();
	int startIndex = rand.nextInt(sz);
	boolean ordo = rand.nextDouble()<ordoProbability;
	CommonMoveStack all = new CommonMoveStack();
	boolean allowSideways = board_state!=OrdoState.OrdoPlay2;
	boolean allowBackwards = false;
	boolean secondOrdo = false;
	switch(board_state)
	{	case Reconnect:	
			allowBackwards = true;
			secondOrdo = !ordo;
			break;
		case OrdoPlay2:
		case OrdoPlay:
			break;
		default: return null;
	}
	for(int i=startIndex,n=0; n<sz; i++,n++)
	{	if(i==sz) { i=0; }
		OrdoCell from = sources.elementAt(i);
		boolean some = ordo && addSimpleOrdoMoves(all,from,from.topChip(),allowSideways,allowBackwards,whoseTurn);
		if(!some) {
				some = addSimpleOrdoMoves(all,from,from.topChip(),allowSideways,allowBackwards,whoseTurn); 
				}
		if(!some && secondOrdo) {
			some = addSimpleOrdoMoves(all,from,from.topChip(),allowSideways,allowBackwards,whoseTurn);
			}
	if(some) { 
		return all.elementAt(rand.nextInt(all.size()));
	}
	}

	return null;
}

public boolean canOfferDraw() {
	return (moveNumber-lastDrawMove>4)
			&& (movingObjectIndex()<0)
			&& ((board_state==OrdoState.OrdoPlay)||(board_state==OrdoState.DrawPending));
}

}
