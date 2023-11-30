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
package veletas;

import online.game.*;

import java.util.*;

import lib.*;
import lib.Random;
import static veletas.VeletasMovespec.*;
/**
 * VeletasBoard knows all about the game of Veletas.
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

class VeletasBoard extends rectBoard<VeletasCell> implements BoardProtocol,VeletasConstants
{	public static int REVISION = 104;		// revision 101 fixes the surround rule
											// revision 102 changes the opening sequence
											// revision 103 changes the opening sequence again
											// revision 104 changes shooter placement and movement rules
	public int getMaxRevisionLevel() { return(REVISION); }
    public int boardColumns;	// size of the board
    public int boardRows;
    public void SetDrawState() { G.Error("Not expected"); }
    public VeletasCell whiteChips = null;
    public VeletasCell blackChips = null;
    public VeletasCell rack[] = null;	// the pool of chips for each player.  
    public CellStack animationStack = new CellStack();
    public VeletasId playerColor[] = new VeletasId[2];
    public VeletasChip playerChip[] = new VeletasChip[2];
    public DrawableImage<?>lastDroppedObject = null;
    public boolean firstMove = true;
    public int placementCount = 0;
    //
    // private variables
    //
    private VeletasState board_state = VeletasState.Play;	// the current board state
    private VeletasState unresign = null;					// remembers the previous state when "resign"
    Variation variation = Variation.Veletas_10;
    public VeletasState getState() { return(board_state); } 
	public void setState(VeletasState st) 
	{ 	unresign = (st==VeletasState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
 	public VeletasId getPlayerColor(int p) { return(playerColor[p]); }
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public VeletasChip pickedObject = null;
    public VeletasCell shooters = null;				// initial stack of shooters
    public CellStack claimedShooters = new CellStack();
    private int nUnclaimedShooters = 0;				// number of shooters not claimed by either player
    public int[]ownedShooters = null;				// the number of shooters owned by each player
    private CellStack shooterLocations = new CellStack();	// all the shooters on the board
    private CellStack pickedSourceStack = new CellStack();	
    private CellStack droppedDestStack = new CellStack();
    private StateStack dropState = new StateStack();
    public VeletasCell lastDest[] = {null,null};				// last spot the opponent dropped, for the UI
    int robotDepth = 0;		// current depth of robot search.  This is used to make faster wins look better
    						// than slower wins.  It's part of the board so multiple threads have independent values.
  	private StateStack robotState = new StateStack();
    private CellStack robotStack = new CellStack();
    private IStack robotShooters = new IStack();
    private boolean robotBoard = false;
	// factory method
	public VeletasCell newcell(char c,int r)
	{	return(new VeletasCell(c,r));
	}
    public VeletasBoard(String init,long rv,int np,int map[],int rev) // default constructor
    {  	setColorMap(map, 2);
    
		Random r = new Random(67246765);
    	rack = new VeletasCell[2];
        blackChips = new VeletasCell(r,VeletasId.Black_Chip_Pool);
        blackChips.addChip(VeletasChip.Black);
        whiteChips = new VeletasCell(r,VeletasId.White_Chip_Pool);
        whiteChips.addChip(VeletasChip.White);
    	shooters = new VeletasCell(r,VeletasId.Shooter_Chip_Pool);
		ownedShooters = new int[2];

    	doInit(init,rv,np,rev); // do the initialization 
     }


	public void sameboard(BoardProtocol f) { sameboard((VeletasBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if clone,digest and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(VeletasBoard from_b)
    {
    	super.sameboard(from_b);	// calls sameCell for each cell, also for inherited class variables.
    	G.Assert(unresign==from_b.unresign,"unresign mismatch");
       	G.Assert(AR.sameArrayContents(win,from_b.win),"win array contents match");
       	G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(dropState.sameContents(from_b.dropState),"dropState mismatch");
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject doesn't match");
        G.Assert(sameCells(shooters,from_b.shooters),"shooters mismatch");
        G.Assert(nUnclaimedShooters==from_b.nUnclaimedShooters,"unclaimed shooters mismatch");
        G.Assert(sameCells(shooterLocations,from_b.shooterLocations),"placed shooters");
        G.Assert(claimedShooters.size()==from_b.claimedShooters.size(),"claimed shooters mismatch");
        G.Assert(AR.sameArrayContents(ownedShooters,from_b.ownedShooters),"owned shooters mismatch");
        G.Assert(firstMove==from_b.firstMove,"firstMove mismatch");
        G.Assert(AR.sameArrayContents(playerChip, from_b.playerChip), "playerchips mismatch");
        G.Assert(AR.sameArrayContents(playerColor, from_b.playerColor), "playerColor mismatch");
        G.Assert(sameCells(rack,from_b.rack), "rack mismatch");
        G.Assert(placementCount==from_b.placementCount,"placementCount mismatch");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");

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

		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,shooters);
		v ^= Digest(r,nUnclaimedShooters);
		v ^= Digest(r,ownedShooters);
		v ^= Digest(r,claimedShooters.size());
		v ^= Digest(r,firstMove);
		v ^= Digest(r,placementCount);
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
		v ^= Digest(r,shooterLocations.size());	// digest only the size because the order gets scrambled
        return (v);
    }
   public VeletasBoard cloneBoard() 
	{ VeletasBoard copy = new VeletasBoard(gametype,randomKey,players_in_game,getColorMap(),revision);
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((VeletasBoard)b); }


    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(VeletasBoard from_b)
    {	
        super.copyFrom(from_b);			// copies the standard game cells in allCells list
        pickedObject = from_b.pickedObject;	
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(rack,from_b.rack);
        copyFrom(blackChips,from_b.blackChips);	// side effect of copying the location
        copyFrom(whiteChips,from_b.whiteChips);
        dropState.copyFrom(from_b.dropState);
        shooters.copyFrom(from_b.shooters);
        board_state = from_b.board_state;
        AR.copy(playerColor,from_b.playerColor);
        AR.copy(playerChip,from_b.playerChip);
        getCell(lastDest,from_b.lastDest);
        unresign = from_b.unresign;
        firstMove = from_b.firstMove;
        nUnclaimedShooters = from_b.nUnclaimedShooters;
        AR.copy(ownedShooters,from_b.ownedShooters);
        getCell(shooterLocations,from_b.shooterLocations);
        getCell(claimedShooters,from_b.claimedShooters);
        placementCount = from_b.placementCount;
        robotStack.clear();
        sameboard(from_b);
    }
    public void doInit(String gtype,long rv)
    {
    	doInit(gtype,rv,players_in_game,revision);
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long rv,int np,int rev)
    {  	drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
		adjustRevision(rev);
    	Grid_Style = GRIDSTYLE; //coordinates left and bottom
    	randomKey = rv;
    	players_in_game = np;
    	AR.setValue(ownedShooters, 0);
    	
    	int map[]=getColorMap();
    	{
    	int bi = map[FIRST_PLAYER_INDEX];
        rack[bi] = blackChips;
        playerColor[bi] = VeletasId.Black_Chip_Pool;
        playerChip[bi] = VeletasChip.Black;
    	}
    	{
    	int wi = map[SECOND_PLAYER_INDEX];
        rack[wi] = whiteChips;
        playerColor[wi] = VeletasId.White_Chip_Pool;
        playerChip[wi] = VeletasChip.White;
    	}      
       	claimedShooters.clear();
     	
     	variation = Variation.findVariation(gtype);
     	switch(variation)
     	{
     	default:  throw G.Error(WrongInitError,gtype);
     	case Veletas_10:
     	case Veletas_9:
     	case Veletas_7:
     		shooterLocations.clear();
     		boardColumns = variation.size;
     		nUnclaimedShooters = variation.nShooters;
     		boardRows = variation.size;
     		reInitBoard(boardColumns,boardRows);
     		shooters.reInit();
     		for(int i=0;i<variation.nShooters;i++) { shooters.addChip(VeletasChip.shooter); }
      		gametype = gtype;
     		break;
     	}

	    setState(VeletasState.Puzzle);
	    
	    // fill the board with the background tiles
	    for(VeletasCell c = allCells; c!=null; c=c.next)
	    {  int i = (c.row+c.col)%2;
	       c.addChip(VeletasChip.getTile(i^1));
	    }
	    switch(variation)
	    {
	    default: break;

	    }
	    robotDepth = 0;
	    robotBoard = false;
	    robotStack.clear();
	    robotState.clear();
	    whoseTurn = FIRST_PLAYER_INDEX;
		acceptPlacement();
        AR.setValue(win,false);
        firstMove = true;
        moveNumber = 1;
        placementCount = 0;

        // note that firstPlayer is NOT initialized here
    }
    private void doSwap()
    {
    	VeletasId temp = playerColor[0];
    	playerColor[0] = playerColor[1];
    	playerColor[1] = temp;
    	
    	VeletasChip ch = playerChip[0];
    	playerChip[0] = playerChip[1];
    	playerChip[1] = ch;
    	
    	VeletasCell c = rack[0];
    	rack[0] = rack[1];
    	rack[1] = c;
    }
    public double simpleScore(int who)
    {	// range is 0.0 to 0.8
    	return((double)ownedShooters[who]/variation.nSecondPlayerShooters());
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
        case ConfirmSwap:
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
         case ConfirmSwap:
            return (true);

        default:
            return (false);
        }
    }
    

    //
    // declare the game over, and the winner and loser
    //
    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(VeletasState.Gameover);
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

    private VeletasCell placedShooter()
    {	VeletasCell dest = droppedDestStack.top();
    	if(dest==null && robotBoard) { dest = robotStack.top(); }
    	if(dest!=null && (dest.topChip()==VeletasChip.shooter)) { return(dest); }
    	return(null);
    }
    //
    // finalize all the state changes for this move.
    //
    public void acceptPlacement()
    {	
        pickedObject = null;
        droppedDestStack.clear();
        dropState.clear();
        pickedSourceStack.clear();
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private int lastPlacementCount = -1;
    private void unDropObject()
    {
    G.Assert(pickedObject==null, "nothing should be moving");
    if(droppedDestStack.size()>0)
    	{
    	VeletasCell dr = droppedDestStack.pop();
    	setState(dropState.pop());
    	switch(dr.rackLocation())
	    	{
	   		default: throw G.Error("Not expecting rackLocation %s",dr.rackLocation);
			case BoardLocation: 
				VeletasChip ch = pickedObject = dr.removeTop();
				placementCount--;
				dr.lastPlaced = lastPlacementCount;
				if(ch.isShooter()) 
					{ shooterLocations.remove(dr,false); 
					  if(board_state==VeletasState.PlayStone) { setState(VeletasState.Play); }
					} 
				break;
			case Shooter_Chip_Pool:
				pickedObject = dr.removeTop();
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
    {	VeletasChip po = pickedObject;
    	if(po!=null)
    	{
    		VeletasCell ps = pickedSourceStack.pop();
    		switch(ps.rackLocation())
    		{
    		default: throw G.Error("Not expecting rackLocation %s",ps.rackLocation);
    		case BoardLocation: 
    				ps.addChip(po);
    				ps.lastEmptied = lastEmptied;
    				lastEmptied = -1;
    				if(po.isShooter())
    					{ shooterLocations.push(ps);
    					}
    				break;
    		case Shooter_Chip_Pool:
    			shooters.addChip(pickedObject);
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
    private void dropObject(VeletasCell c)
    {   G.Assert(pickedObject!=null,"pickedObject should not be null"); 	    		
    	switch(c.rackLocation())
		{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case BoardLocation:
			c.addChip(pickedObject);
			lastPlacementCount = c.lastPlaced;
			c.lastPlaced = placementCount++;
			if(pickedObject.isShooter()) 
				{	shooterLocations.push(c);
				}
			break;
		case Shooter_Chip_Pool:
			c.addChip(pickedObject);
			break;
		case White_Chip_Pool:
		case Black_Chip_Pool:	break;	// don't add back to the pool
		}
    	dropState.push(board_state);
       	droppedDestStack.push(c);
       	pickedObject = null;
    }
    
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private int lastEmptied  = -1;
    private void pickObject(VeletasCell c)
    {	G.Assert(pickedObject==null,"pickedObject should be null");
    	G.Assert(!c.isEmpty(),"should have a chip");
    	switch(c.rackLocation())
    	{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case BoardLocation: 
			
			VeletasChip ch = pickedObject = c.removeTop();
			lastEmptied = c.lastEmptied;
			c.lastEmptied = placementCount;
			if(ch.isShooter()) 
				{ shooterLocations.remove(c,false);
				}
			break;
		case Shooter_Chip_Pool:
			pickedObject = c.removeTop();
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
    public boolean isDest(VeletasCell cell)
    {	return(droppedDestStack.top()==cell);
    }
    //
    // get the last dropped dest cell
    //
    public VeletasCell getDest() 
    { return(droppedDestStack.top()); 
    }
    
    public VeletasCell getPrevDest()
    {
    	return(lastDest[nextPlayer[whoseTurn]]);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  Returns +100 if a king is the moving object.
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	VeletasChip ch = pickedObject;
    	if(ch!=null)
    		{ int nn = ch.chipNumber();
    		  return(nn);
    		}
        return (NothingMoving);
    }
    // get a cell from a partucular source
    public VeletasCell getCell(VeletasId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case Shooter_Chip_Pool:
        	return(shooters);
        case BoardLocation:
        	return(getCell(col,row));
        case White_Chip_Pool:
       		return(whiteChips);
        case Black_Chip_Pool:
       		return(blackChips);
        }
    }
    //
    // get the local cell which is the same role as c, which might be on
    // another instance of the board
    public VeletasCell getCell(VeletasCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
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
        	setNextStateAfterDone(); 
        	break;
        case PlaceSingleStone:
        case PlayStone:
        	setState(VeletasState.Confirm);
        	break;
        case PlaceOrSwap:
        	setState(VeletasState.PlaceShooters);
			//$FALL-THROUGH$
        case PlaceShooters:
        	{
        	int nLeft = shooters.chipHeight();
        	if((nLeft==0) || (nLeft==variation.nSecondPlayerShooters()))
        		{
        		if(revision>=102) {
        			setState(VeletasState.PlaceSingleStone);
        		}
        		else {
        		setState(VeletasState.Confirm);
        		}}
        	else { }
        	}
        	break;
        
        case PlayOrSwap:
        case Play:
        	if(placedShooter()!=null) { setState(VeletasState.PlayStone); }
        	else { setState(VeletasState.Confirm);}
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
    public boolean isSource(VeletasCell c)
    {	return(getSource()==c);
    }
    public VeletasCell getSource()
    {
    	return((pickedSourceStack.size()>0) ?pickedSourceStack.top() : null);
    }
    
    //
    // we don't need any special state changes for picks.
    //
    private void setNextStateAfterPick()
    {
    }
    private int sweep_counter=0;
    private int groupSize_count(VeletasCell c,VeletasChip top)
    {	int sz = 0;
    	if((c!=null) 
    			&& (c.chipHeight()==1)
    			&& (c.topChip()==top)
    			&& (c.sweep_counter!=sweep_counter))
    	{	c.sweep_counter = sweep_counter;
    		sz++;
    		for(int dir=CELL_LEFT,lim=CELL_LEFT+CELL_FULL_TURN;
    				dir<lim;
    				dir+=CELL_QUARTER_TURN)
    		{
    			sz += groupSize_count(c.exitTo(dir),top);
    		}
    	}
    	return(sz);
    }
    private int groupSize(VeletasCell c)
    {
    	sweep_counter++;
    	VeletasChip top = c.topChip();
    	if(top!=null && (top!=VeletasChip.shooter))
    	{
    	if(c.chipHeight()==1)
    		{
    		return(groupSize_count(c,top));
    		}
    		else {
    		return(0);	 // return zero for claimed shooters
    		}
    	}
    	return(-1);	// return -1 for no chip or a shooter
    }
    
    boolean shooterCanEverMoveFrom(VeletasCell c)
    {	if(c==null) { return(false); }
    	if(c.sweep_counter==sweep_counter) { return(false); }
    	c.sweep_counter = sweep_counter;
      
    	switch (c.chipHeight())
    	{
    	case 0: return(true);	// empty
    	case 2: return(false);	// claimed shooter
    	case 1:	
    		VeletasChip top = c.topChip();
    		if(top==VeletasChip.shooter) 
    			{ 
    				for(int dir=0;dir<c.geometry.n;dir++)
    				{	if(shooterCanEverMoveFrom(c.exitTo(dir))) { return(true); }
    				}
    			}
    		break;
       	default: G.Error("Not expected");
    	}
    	return(false);
    }
    boolean shooterCanMoveInDirection(VeletasCell from,int dir)
    {
    	while((from = from.exitTo(dir))!=null)
    	{	VeletasChip top = from.topChip();
    		if(top==null) { return(true); }
    		if(top!=VeletasChip.shooter) { return false; }
    	}
    	return(false);
    }
    boolean shooterCanMoveFrom(VeletasCell c)
    {	if(c==null) { return(false); }
    	if(revision<104)
    	{
    	for(int dir = c.geometry.n-1; dir>=0; dir--)
    	{
    		VeletasCell adj = c.exitTo(dir);
    		if((adj!=null) && adj.isEmpty()) { return(true); }
    	}}
    	else
    	{
    	   	for(int dir = c.geometry.n-1; dir>=0; dir--)
        	{	if(shooterCanMoveInDirection(c,dir)) { return(true); }
        	}	
    	}
    	return(false);
    }
    
    boolean shooterCanEverMove(VeletasCell c)
    {	if(revision>=101) { return(shooterCanMoveFrom(c)); }
    	sweep_counter++;
    	return(shooterCanEverMoveFrom(c));
    }
    
    private int playerIndex(VeletasChip ch)
    {
    	return((ch==playerChip[0]) ? 0 : 1);
    }
    private void assignClaimedShooters()
    {	boolean some = true;
    	while(some)
    	{
    	some = false;	// in rare circumstances, a new shooter can be frozen by
    					// the freezing of some other shooter, so we need to iterate
    	for(int lim = shooterLocations.size()-1; lim>=0; lim--)
    	{
    		VeletasCell c = shooterLocations.elementAt(lim);
    		if((c.chipHeight()==1) && !shooterCanEverMove(c))
    			{	VeletasChip biggroup = null;
    				int biggroupSize = 0;
    				boolean conflict = false;
    				// it is unclaimed and should be claimed by the owner
    				// of the largest adjacent group
    				for(int dir = CELL_LEFT; 
    						dir< CELL_LEFT+CELL_FULL_TURN;
    						dir+= CELL_QUARTER_TURN)
    				{	VeletasCell adj = c.exitTo(dir);
    					if(adj!=null)
    					{
    						int sz = groupSize(adj);
    						if(sz>biggroupSize)
    						{	biggroup = adj.topChip();
    							biggroupSize = sz;
    							conflict = false;
    						}
    						else
    						if(((sz==0)&&(biggroup==null)) || ((sz==biggroupSize) && adj.topChip()!=biggroup))
    						{	// size 0 is a claimed shooter
    							conflict = true;
    						}
    					}
    				}
    				if(conflict || (biggroup==null)) 
    					{ biggroup = playerChip[whoseTurn];	// whoseTurn has already changed
    					}
    				int bigPlayer = playerIndex(biggroup);
    				c.addChip(biggroup);
    				claimedShooters.push(c);
    				nUnclaimedShooters--;
    				ownedShooters[bigPlayer]++;
    				some = true;
    				
    			}
    		}
    	}
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case Gameover: 
    		break;
    	case ConfirmSwap:
    	case Confirm:
       	case Puzzle:
    		assignClaimedShooters();
    		if(ownedShooters[whoseTurn]>=variation.shootersToWin())
    		{	setGameOver(true,false); 
    		}
    		else if(ownedShooters[nextPlayer[whoseTurn]]>=variation.shootersToWin())
    		{	setGameOver(false,true);
    		}
    		else if(hasMoves()) 
			{ 
    			if(firstMove && (whoseTurn==1) && revision>=103)
    			{	firstMove = false;
    				setState(VeletasState.PlaceOrSwap);
    			}
    			else if(shooters.chipHeight()==0)
				{ if((revision>=102) && firstMove && (whoseTurn==1)) 
					{
						firstMove = false;
						setState(VeletasState.PlayOrSwap);
					}
				else { setState(VeletasState.Play); }
				}
				else { setState(VeletasState.PlaceShooters); }
			}
    		break;
    	}

    }

    private void doDone(replayMode replay)
    {	VeletasCell dest = getDest();
    	lastDest[whoseTurn] = dest;
    	// special bit for the animations
    	
     	acceptPlacement();


        if (board_state==VeletasState.Resign)
        {	setGameOver(false,true);
        }
        else
        {	setNextPlayer(); 
        	setNextStateAfterDone(); 

        }
    }

    public boolean Execute(commonMove mm,replayMode replay)
    {	VeletasMovespec m = (VeletasMovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }
        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone(replay);

            break;

        case MOVE_RACK_BOARD:
        	{
        	VeletasCell from = getCell(m.source,m.from_col,m.from_row);
        	VeletasCell to = getCell(m.to_col,m.to_row);
        	G.Assert(to.topChip()==null,"isempty "+m);
        	pickObject(from);
        	m.target = pickedObject;
        	dropObject(to);
        	setNextStateAfterDrop();
        	if(replay!=replayMode.Replay)
			{
				animationStack.push(from);
				animationStack.push(to);
			}
        	}
        	break;
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case Play:
        		case PlayOrSwap:
        		case PlaceOrSwap:
        		case PlayStone:
        			G.Assert(pickedObject==null,"something is moving");
        			VeletasCell src = getCell(VeletasId.BoardLocation, m.from_col, m.from_row);
        			VeletasCell dest = getCell(VeletasId.BoardLocation,m.to_col,m.to_row);
        	       	G.Assert(dest.topChip()==null,"isempty");
        			pickObject(src);
        			m.target = pickedObject;
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
			VeletasCell c = getCell(VeletasId.BoardLocation, m.to_col, m.to_row);
        	G.Assert(pickedObject!=null,"something is moving");
			if(isSource(c)) 
            	{ 
            	  unPickObject(); 
            	} 
            	else
            		{
            		if(replay==replayMode.Live)
            		{
            			lastDroppedObject = pickedObject.getAltDisplayChip(c);
            			//G.print("Drop ",lastDroppedObject);
            		}
            		m.target = pickedObject;
            		dropObject(c);
            		setNextStateAfterDrop();
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
        		{
        		VeletasCell c = getCell(VeletasId.BoardLocation, m.from_col, m.from_row);
        		if(isDest(c)) { unDropObject(); }
        		else
        		{ pickObject(c);
        		  switch(board_state)
        		  {	default: throw G.Error("Not expecting pickb in state %s",board_state);
        		    case Confirm:
        		  	case Play:
        		  	case PlayOrSwap:
        		  	case PlaceOrSwap:
          		  	case Puzzle:
        		  		break;
        		  }
         		}
        		}
            break;

        case MOVE_DROP: // drop on chip pool;
        	{
        	VeletasCell c = getCell(m.source, m.to_col, m.to_row);
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
        	VeletasCell c = getCell(m.source, m.from_col, m.from_row);
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
            setState(VeletasState.Puzzle);
            {	boolean win1 = winForPlayerNow(whoseTurn);
            	boolean win2 = winForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;

        case MOVE_RESIGN:
        	setState(unresign==null?VeletasState.Resign:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            // standardize "gameover" is not true
            setState(VeletasState.Puzzle);
 
            break;
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;

		case MOVE_SWAP:
			doSwap();
			setState( (VeletasState.PlayOrSwap==board_state||(VeletasState.PlaceOrSwap==board_state))
						? VeletasState.ConfirmSwap 
						: (revision==102)?VeletasState.PlayOrSwap:VeletasState.PlaceOrSwap);
			break;
        default:
        	cantExecute(m);
        }

 
        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips(VeletasCell c)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case PlaceShooters:
        case PlaceOrSwap:
        	return(c==shooters);
        case PlaceSingleStone:
        case PlayStone:
        case PlayOrSwap:
        case Play:
        	return(c==rack[whoseTurn]);
        case Confirm:
 		case Gameover:
		case Resign:
		case ConfirmSwap:
			return(false);
        case Puzzle:
        	if(pickedObject==null) { return(true); }
        	if(pickedObject == VeletasChip.shooter) { return(c==shooters); }
        	return(c.rackLocation()==pickedObject.id);
        }
    }
  

    public boolean legalToHitBoard(VeletasCell cell,Hashtable<VeletasCell,VeletasMovespec>targets)
    {	
        switch (board_state)
        {
 		case Play:
 		case PlayOrSwap:
 		case PlaceOrSwap:
 		case PlayStone:
 			return((isDest(cell)&&(pickedObject==null)) || isSource(cell)||targets.get(cell)!=null);
 		case Resign:
		case Gameover:
		case ConfirmSwap:
			return(false);
		case Confirm:
			return(isDest(cell));
		case PlaceShooters:
		case PlaceSingleStone:
			return((targets.get(cell)!=null) || (pickedObject==null && isDest(cell)) );
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case Puzzle:
        	return(pickedObject==null?!cell.isEmpty():true);
        }
    }
  public boolean canDropOn(VeletasCell cell)
  {		VeletasCell top = (pickedObject!=null) ? pickedSourceStack.top() : null;
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
    public void RobotExecute(VeletasMovespec m)
    {	robotBoard = true;
        //G.print("R "+m);
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        robotState.push(board_state);
        robotShooters.push(claimedShooters.size());
        robotDepth++;
        if (Execute(m,replayMode.Replay))
        {	
            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {	doDone(replayMode.Replay); 
            }
            else if(board_state==VeletasState.PlayStone)
            { 
            robotStack.push(pickedSourceStack.top());
        	robotStack.push(droppedDestStack.top());
         	}
           	acceptPlacement(); 
       }
     }
 
    private void unclaimOneShooter()
    {
    	VeletasCell loc = claimedShooters.pop();
    	VeletasChip top = loc.removeTop();
    	nUnclaimedShooters++;
    	ownedShooters[playerIndex(top)]--;
    }

   //
   // un-execute a move.  The move should only be un-executed
   // in proper sequence, and if it was executed by the robot in the first place.
   // If you use monte carlo bots with the "blitz" option this will never be called.
   //
    public void UnExecute(VeletasMovespec m)
    {
        //G.print("U "+m+" for "+whoseTurn);
    	VeletasState fromState = board_state;
    	robotDepth--;
        setState(robotState.pop());
        int ns = robotShooters.pop();
        while(ns<claimedShooters.size()) { unclaimOneShooter(); }
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
        

        if((fromState==VeletasState.PlayOrSwap)||(fromState==VeletasState.PlaceOrSwap))
        	{ firstMove = true; 
        	}
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
        case MOVE_RACK_BOARD:
        	{
       		VeletasCell src = getCell(m.source,m.from_col,m.from_row);
    		VeletasCell dest = getCell(m.to_col,m.to_row);

    		switch(board_state)
        	{
        	default: throw G.Error("Not expecting robot in state %s",board_state);
        	case Play:
        	case PlaceSingleStone:
        	case PlayStone:
        	case PlaceShooters:
        	case PlaceOrSwap:
        	case PlayOrSwap:
        		{
         		pickObject(dest);
         		G.Assert(pickedObject==src.rackLocation().chip,"wrong chip");
        		dropObject(src);
        		acceptPlacement();
        		}
        	}
    		if(board_state==VeletasState.PlayStone)
    		{	if(robotStack.size()>0)	// can be zero at top level
    		{	robotStack.pop();
    			robotStack.pop();
    			 }
    			
    		}
        	}
        	break;
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        	case PlayOrSwap:
        	case PlaceOrSwap:
         		case Play:
         			{
        			G.Assert(pickedObject==null,"something is moving");
        			VeletasCell to = getCell(VeletasId.BoardLocation, m.to_col, m.to_row);
        			pickObject(to);
        			VeletasCell from = getCell(VeletasId.BoardLocation, m.from_col,m.from_row);
       			    dropObject(from); 
       			    acceptPlacement();
       			    acceptPlacement();
          			}
        			break;
        	}
        	break;
  
        case MOVE_RESIGN:
            break;
        }
   }

private void loadHash(CommonMoveStack all,Hashtable<VeletasCell,VeletasMovespec>hash,boolean from)
{
	for(int lim=all.size()-1; lim>=0; lim--)
	{
		VeletasMovespec m = (VeletasMovespec)all.elementAt(lim);
		switch(m.op)
		{
		default: break;
		case MOVE_RACK_BOARD:
		case MOVE_BOARD_BOARD:
			if(from) 
				{ hash.put(getCell(m.source,m.from_col,m.from_row),m);
				  hash.put(getCell(m.to_col,m.to_row),m);
				}
			else { hash.put(getCell(m.to_col,m.to_row),m); }
		}
		}
}
/**
 * getTargets() is called from the user interface to get a hashtable of 
 * cells which the mouse can legally hit.
 * 
 * Veletas uses the move generator for most of the logic of where it's legal
 * for the mouse to pick up or drop something.  We start with the list of legal
 * moves, and select either the legal "from" spaces, or the legal "to" spaces.
 * 
 * The advantage of this approach is that the logic for "legal moves" whatever it
 * may be, is needed anyway to drive the robot, and by reusing the move list we
 * avoid having to duplicate that logic.
 * 
 * @return
 */
public Hashtable<VeletasCell,VeletasMovespec>getTargets()
{
	Hashtable<VeletasCell,VeletasMovespec>hash = new Hashtable<VeletasCell,VeletasMovespec>();
	CommonMoveStack all = new CommonMoveStack();

		switch(board_state)
		{
		default: break;
		case PlaceShooters:
		case PlaceSingleStone:
		case PlayOrSwap:
		case PlaceOrSwap:
		case PlayStone:
		case Play:
			{	if(pickedObject!=null) { addMoves(all,whoseTurn,pickedSourceStack.top()); }
					else { addMoves(all,whoseTurn); }
				loadHash(all,hash,pickedObject==null);
			}
			break;
		}
	return(hash);
}

public boolean hasMoves()
{	if(shooters.chipHeight()>0) { return(true); }
	return(nUnclaimedShooters>0);
}

 public boolean addPlaceShooterMoves(CommonMoveStack all,int who)
 {	boolean some = false;
 	if(revision<104)
 	{
	 for(VeletasCell c = allCells; c!=null; c=c.next)
	 {
		 if(c.isEmpty() && c.allEmptyAdjacent())
		 {
			 if(all==null) { return(true); }
			 all.push(new VeletasMovespec(MOVE_RACK_BOARD,shooters,c,who));
		 }
	 }}
 	else
 	{	// revised rule, shooters can be adjacent to each other, but can't be on the periphery
 		 for(VeletasCell c = allCells; c!=null; c=c.next)
 		 {
 			 if(c.isEmpty() && !c.isEdgeCell())
 			 {
 				 if(all==null) { return(true); }
 				 all.push(new VeletasMovespec(MOVE_RACK_BOARD,shooters,c,who));
 			 }
 		 }	
 		
	 }
	 return(some);
 }
 private boolean addShooterMoves(CommonMoveStack all,int who,VeletasCell c)
 {	boolean some = false;
	 for(int dir = c.geometry.n-1; dir>=0; dir--)
	 {
		 VeletasCell adj = c;
		 if(revision<104)
		 {
		 while((adj = adj.exitTo(dir))!=null && (adj.topChip()==null))
		 {	
			 if(all==null) { return(true); }
			 some = true;
			 all.push(new VeletasMovespec(MOVE_BOARD_BOARD,c,adj,who));
		 }}
		 else
		 {	// shooters do not block each other
			 VeletasChip top = null;
			 while( ((adj = adj.exitTo(dir))!=null)
					 && (((top=adj.topChip())==null)|| (top==VeletasChip.shooter)) )
			 {	 if(top==null)
			 	{
				 if(all==null) { return(true); }
				 some = true;
				 all.push(new VeletasMovespec(MOVE_BOARD_BOARD,c,adj,who));
			 	}
			 } 
		 }
	 }
	 return(some);
 }
 private boolean addPlacementMoves(CommonMoveStack all,int who,VeletasCell c)
 {	boolean some = false;
	for(int dir = c.geometry.n-1; dir>=0; dir--)
	 {
		 VeletasCell adj = c;
		 if(revision<104)
		 {
		 while(((adj = adj.exitTo(dir))!=null)
				 && (adj.topChip()==null))
		 {
			 if(all==null) { return(true); }
			 some = true;
			 all.push(new VeletasMovespec(MOVE_RACK_BOARD,rack[who],adj,who));
		 }}
		 else
		 {	VeletasChip top = null;
			 while(((adj = adj.exitTo(dir))!=null)
					 && (((top=(adj.topChip()))==null) || (top==VeletasChip.shooter)))
			 {	if(top==null)
			 {
				 if(all==null) { return(true); }
				 some = true;
				 all.push(new VeletasMovespec(MOVE_RACK_BOARD,rack[who],adj,who));
			 }
		 }
	 }
	 }
	 return(some);
 }
 public boolean addShooterMoves(CommonMoveStack all,int who)
 {	boolean some = false;
 	for(int i=shooterLocations.size()-1; i>=0; i--)
 	{
	VeletasCell c = shooterLocations.elementAt(i);
	if(c.chipHeight()==1)
	{
	G.Assert(VeletasChip.isShooter(c.topChip()),"is a shooter");
	if(addShooterMoves(all,who,c))
	{
		if(all==null) { return(true); }
		some = true;
 	}}}
	 return(some);
 }
 
 // add a stone in any location extension from a shooter
 public boolean addPlacementMoves(CommonMoveStack all,int who)
 {	boolean some = false;
 	for(int i=shooterLocations.size()-1; i>=0; i--)
 	{
	VeletasCell c = shooterLocations.elementAt(i);
	if(c.chipHeight()==1)
	{
	G.Assert(VeletasChip.isShooter(c.topChip()),"is a shooter");
	if(addPlacementMoves(all,who,c))
	{
		if(all==null) { return(true); }
		some = true;
 	}}}
	 return(some);
 }
 
 // add a stone in any location extension from a shooter
 public boolean addSinglePlacementMoves(CommonMoveStack all,int who)
 {	boolean some = false;
 	for(VeletasCell c = allCells; c!=null; c=c.next)
 	{
 		if(c.topChip()==null)
 		{ if(all==null) { return(true); }
 		  some = true;
 		  all.push(new VeletasMovespec(MOVE_RACK_BOARD,rack[who],c,who));
 		}
 	}
	 return(some);
 }
 
 public boolean addMoves(CommonMoveStack all,int who,VeletasCell from)
 {
	 switch(from.rackLocation())
	 {
	 default: throw G.Error("not expected");
	 case BoardLocation:
		 // must be a shooter moving
		 return(addShooterMoves(all,who,from));

	 case White_Chip_Pool:
	 case Black_Chip_Pool:
	 	{
		 if(board_state==VeletasState.PlaceSingleStone)
		 {
			 return addSinglePlacementMoves(all,who); 
		 }
		 else 
		 {
		 VeletasCell shooter =  placedShooter();
		 return((shooter==null)
				 	? addPlacementMoves(all,who) 
				 	: addPlacementMoves(all,who,shooter));
		 }
	 	}
	 case Shooter_Chip_Pool:
		 return(addPlaceShooterMoves(all,who));
	 }
 }
 public boolean addMoves(CommonMoveStack all,int who)
 {	boolean some = false;
 	switch(board_state)
 	{
 	case PlaceOrSwap:
 		all.push(new VeletasMovespec(MOVE_SWAP,who));
		//$FALL-THROUGH$
 	case PlaceShooters:
 		addPlaceShooterMoves(all,who);
 		break;
 	case PlayOrSwap:
 		all.push(new VeletasMovespec(MOVE_SWAP,who));
		//$FALL-THROUGH$
 	case Play:
 		addShooterMoves(all,who);
 		addPlacementMoves(all,who);
 		break;
 	case PlaceSingleStone:
 		addSinglePlacementMoves(all,who); 
 		break;
 	case PlayStone:
 		{ VeletasCell c = placedShooter();
 		  if(c==null) { addPlacementMoves(all,who); } else { addPlacementMoves(all,who,c); }
 		}
 		break;
 	default:  G.Error("Not expecting %s",board_state);

	}
 	return(some);
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	addMoves(all,whoseTurn);
	if(all.size()==0)
	{	G.Error("No moves generated");
	}
		 
 	return(all);
 }
 

}
