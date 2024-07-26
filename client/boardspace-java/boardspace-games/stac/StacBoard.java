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
package stac;


import java.util.Hashtable;

import online.game.*;
import lib.*;

import static stac.StacMovespec.*;
/**
 * StacBoard knows all about the game of Stac.
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

class StacBoard extends squareBoard<StacCell> implements BoardProtocol,StacConstants
{	static int REVISION = 102;			// revision 101 fixes the double carry bug
										// revision 102 adds defense against multiple draw offers
	public int getMaxRevisionLevel() { return(REVISION); }
    public int boardColumns;			// size of the board
    public int boardRows;
    public void SetDrawState() { setState(StacState.RepetitionPending); }
    public StacCell rack[] = null;
    public CellStack animationStack = new CellStack();
    //
    // private variables
    //
    private StacState board_state = StacState.Play;	// the current board state
    private StacState unresign = null;					// remembers the previous state when "resign"
    private StateStack robotState = new StateStack();
    public int robotDepth = 0;
    private IStack robotCarry = new IStack();
    private RepeatedPositions reps = null;

    
    Variation variation = Variation.Stac;
    public StacState getState() { return(board_state); } 
	public void setState(StacState st) 
	{ 	unresign = (st==StacState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
    private StacId playerColor[]={StacId.Red_Chip_Pool,StacId.Blue_Chip_Pool};
    public StacChip playerChip[] = { StacChip.red,StacChip.blue };
    private StacCell pawnLocation[] = new StacCell[2];
    private boolean lastWasCarry[] = new boolean[2];
    public int nSingleChips = 0;
    public int lastStackMove = 0;
    public int lastDrawMove = 0;
 	public StacId getPlayerColor(int p) { return(playerColor[p]); }
	    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public StacChip pickedObject = null;
    public StacChip pickedDisk = null;
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    private StateStack droppedState = new StateStack();
    private StateStack pickedState = new StateStack();
  
	// factory method
	public StacCell newcell(char c,int r)
	{	return(new StacCell(c,r));
	}
    public StacBoard(String init,long rv,int np,RepeatedPositions re,int map[],int rev) // default constructor
    {   reps = re;
    	setColorMap(map, np);
        doInit(init,rv,np,rev); // do the initialization 
     }


	public void sameboard(BoardProtocol f) { sameboard((StacBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if clone,digest and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(StacBoard from_b)
    {
    	super.sameboard(from_b);	// calls sameCell for each cell, also for inherited class variables.
    	G.Assert(unresign==from_b.unresign,"unresign mismatch");
       	G.Assert(AR.sameArrayContents(win,from_b.win),"win array contents match");
       	G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor contents match");
       	G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(sameCells(pawnLocation,from_b.pawnLocation),"pawnlocation mismatch");
        G.Assert(droppedState.sameContents(from_b.droppedState),"droppedState mismatch");
        G.Assert(pickedState.sameContents(from_b.pickedState),"pickedState mismatch");
        G.Assert(AR.sameArrayContents(lastWasCarry,from_b.lastWasCarry),"lastWasCarry mismatch");
        G.Assert(nSingleChips==from_b.nSingleChips,"singleChips mismatch");
        //G.Assert(lastStackMove==from_b.lastStackMove,"lastStackMove mismatch");	// not in digest
        
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject doesn't match");
        G.Assert(pickedDisk==from_b.pickedDisk,"pickedDisk doesn't match");
        G.Assert(sameCells(rack,from_b.rack),"rack mismatch");
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
		v ^= chip.Digest(r,pickedDisk);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,rack);
		v ^= Digest(r,nSingleChips);
		//v ^= Digest(r,lastStackMove);	not in digest
		v ^= Digest(r,pawnLocation);
		v ^= Digest(r,lastWasCarry);
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
        return (v);
    }
   public StacBoard cloneBoard() 
	{ StacBoard copy = new StacBoard(gametype,randomKey,players_in_game,reps,getColorMap(),revision);
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((StacBoard)b); }


    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(StacBoard from_b)
    {	
        super.copyFrom(from_b);			// copies the standard game cells in allCells list
        pickedObject = from_b.pickedObject;	
        pickedDisk = from_b.pickedDisk;
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pawnLocation,from_b.pawnLocation);
        AR.copy(lastWasCarry,from_b.lastWasCarry);
        nSingleChips = from_b.nSingleChips;
        lastStackMove = from_b.lastStackMove;
        lastDrawMove = from_b.lastDrawMove;
        copyFrom(rack,from_b.rack);
        droppedState.copyFrom(from_b.droppedState);
        pickedState.copyFrom(from_b.pickedState);
        AR.copy(playerColor,from_b.playerColor);
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        robotDepth = from_b.robotDepth;
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
    	robotState.clear();
    	robotDepth = 0;
		rack = new StacCell[2];
    	Random r = new Random(67246765);
    	int map[]= getColorMap();
     	for(int i=0,pl=FIRST_PLAYER_INDEX;i<2; i++,pl=nextPlayer[pl])
    	{
       	StacCell cell = new StacCell(r);
       	cell.rackLocation=RackLocation[i];
       	for(int j=0;j<4;j++) { cell.addChip(StacChip.getChip(i)); }
    	rack[map[i]]=cell;
     	}    
     	variation = Variation.findVariation(gtype);
     	switch(variation)
     	{
     	default:  throw G.Error(WrongInitError,gtype);
     	case Stac:
      		boardColumns = variation.size;
     		boardRows = variation.size;
     		initBoard(boardColumns,boardRows);
     		gametype = gtype;
     		break;
     	}

        allCells.setDigestChain(r);
	    setState(StacState.Puzzle);
	    
	    nSingleChips = 0;
	    lastStackMove = 0;
	    lastDrawMove = 0;
	    for(StacCell c = allCells; c!=null; c=c.next)
	    {  nSingleChips++;  
	       c.addChip(StacChip.black);
	    }
    	(pawnLocation[map[FIRST_PLAYER_INDEX]] = getCell('E',1)).addChip(StacChip.redBoss);
     	(pawnLocation[map[SECOND_PLAYER_INDEX]] = getCell('A',5)).addChip(StacChip.blueBoss);
 	    AR.setValue(lastWasCarry,false);
	    whoseTurn = FIRST_PLAYER_INDEX;
	    
		playerColor[map[FIRST_PLAYER_INDEX]]=StacId.Red_Chip_Pool;
		playerColor[map[SECOND_PLAYER_INDEX]]=StacId.Blue_Chip_Pool;
		playerChip[map[FIRST_PLAYER_INDEX]] = StacChip.red;
		playerChip[map[SECOND_PLAYER_INDEX]] = StacChip.blue;
		
		acceptPlacement();
        AR.setValue(win,false);
        
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
        case Puzzle:
            break;
        case Confirm:
        case AcceptPending:
        case RepetitionPending:
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
         case DrawPending:
         case AcceptPending:
         case RepetitionPending:
         case DeclinePending:
        	 return (true);

        default:
            return (false);
        }
    }


    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(StacState.Gameover);
    }
    public boolean gameOverNow() { return(board_state.GameOver()); }
    public boolean winForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	if(board_state.GameOver()) { return(win[player]); }
    	
    	return((rack[player].height()==0) 
    			|| ((nSingleChips==0) && rack[player].height()<rack[nextPlayer[player]].height()));
     }
    // estimate the value of the board position.
    public double ScoreForPlayer(int player,boolean print,double cup_weight,double ml_weight,boolean dumbot)
    {  	double finalv=0.0;
    	G.Error("not implemented");
    	return(finalv);
    }

    public double simpleScore(int who)
    {
    	double dif = rack[nextPlayer[who]].height()-rack[who].height();	// claims remaining, max 3
    	return(dif*0.25);
    }
    public double simpleScore()
    {
    	return(simpleScore(whoseTurn)-simpleScore(nextPlayer[whoseTurn]));
    }
    //
    // finalize all the state changes for this move.
    //
    public void acceptPlacement()
    {	
        pickedObject = null;
        pickedDisk = null;
        droppedDestStack.clear();
        pickedSourceStack.clear();
        droppedState.clear();
        pickedState.clear();
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    G.Assert(pickedObject==null, "nothing should be moving");
    if(droppedDestStack.size()>0)
    	{
    	StacCell dr = droppedDestStack.pop();
    	setState(droppedState.pop());
    	switch(dr.rackLocation())
	    	{
	   		default: throw G.Error("Not expecting rackLocation %s",dr.rackLocation);
			case Red_Chip_Pool:	// treat the pools as infinite sources and sinks
			case Blue_Chip_Pool:
			case BoardLocation: 
				pickedObject = dr.removeTop(); 
				pawnLocation[playerIndex(pickedObject)]=null;
				if(pickedDisk!=null) { dr.removeTop(); }
				break;	    	
	    	}
	    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	StacChip po = pickedObject;
    	if(po!=null)
    	{
    		StacCell ps = pickedSourceStack.pop();
    		setState(pickedState.pop());
    		switch(ps.rackLocation())
    		{
    		default: throw G.Error("Not expecting rackLocation %s",ps.rackLocation);
    		case Red_Chip_Pool:
    		case Blue_Chip_Pool:	
    		case BoardLocation:
    		case ChipLocation:
    			if(pickedDisk!=null) { ps.addChip(pickedDisk); }
    			ps.addChip(po);
    			int pindex = playerIndex(po);
    			if(pindex>=0) { pawnLocation[pindex]=ps; }
    			break;
    		}
    		pickedObject = null;
    		pickedDisk = null;
     	}
     }

    // 
    // drop the floating object.
    //
    private void dropObject(StacCell c)
    {   G.Assert(pickedObject!=null,"pickedObject should not be null"); 	    		
    	switch(c.rackLocation())
		{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case ChipLocation:
		case BoardLocation:
			if(pickedDisk!=null) { c.addChip(pickedDisk); }
			c.addChip(pickedObject);
			int index = playerIndex(pickedObject);
			if(index>=0) {			pawnLocation[index]=c; }
			break;
		case Red_Chip_Pool:
		case Blue_Chip_Pool:	break;	// don't add back to the pool
		}
       	droppedDestStack.push(c);
       	droppedState.push(board_state);
       	pickedObject=null;
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(StacCell cell)
    {	return((droppedDestStack.size()>0) && (droppedDestStack.top()==cell));
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	StacChip ch = pickedObject;
    	if(ch!=null)
    		{ int base = (pickedDisk!=null) ? pickedDisk.chipNumber()*100 : 0;
    		  return(base+ch.chipNumber());
    		}
        return (NothingMoving);
    }
   
    public StacCell getCell(StacId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case ChipLocation:
        case BoardLocation:
        	return(getCell(col,row));
        case Red_Chip_Pool:
       		return(rack[getColorMap()[Red_Chip_Index]]);
        case Blue_Chip_Pool:
       		return(rack[getColorMap()[Blue_Chip_Index]]);
        }
    }
    public StacCell getCell(StacCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(StacCell c,StacId rack)
    {	G.Assert(pickedObject==null,"pickedObject should be null");
    	switch(rack)
    	{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case BoardLocation: 
		case Red_Chip_Pool:
		case Blue_Chip_Pool:	
			pickedObject = c.removeTop();
			int pindex = playerIndex(pickedObject);
			if(pindex>=0) { pawnLocation[pindex]=null; } 
			break;    	
		case ChipLocation:
			pickedObject = c.removeTop();
			pickedDisk = c.removeTop();
			break;
    	}
    	pickedSourceStack.push(c);
    	pickedState.push(board_state);
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
        case DrawPending:
        	setNextStateAfterDone(); 
        	break;
        case Play:
        case Carry:
        case CarryDrop:
			setState(StacState.Confirm);
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
    public boolean isSource(StacCell c)
    {	return(getSource()==c);
    }
    public StacCell getSource()
    {
    	return((pickedSourceStack.size()>0) ?pickedSourceStack.top() : null);
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterPick()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting pick in state %s", board_state);
        case Confirm:
        case DrawPending:
        	setState(StacState.Play);
        	break;
        case Play:
			break;
        case Puzzle:
            break;
        }
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case Gameover: 
    		break;
    	case AcceptPending:
    	case RepetitionPending:
    		setGameOver(false,false); 
    		break;
        case DrawPending:
        	lastDrawMove = moveNumber;
        	setState(StacState.AcceptOrDecline);
        	break;
    	case Confirm:
    	case Puzzle:
    	case Play:
    	case DeclinePending:
    		setState(hasCarryMoves()?StacState.Carry:StacState.Play);
     		break;
    	}

    }
   
    private void checkForClaim(replayMode replay)
    {	StacCell dest = pawnLocation[whoseTurn];
    	switch(dest.height())
    	{
    	case 0:
    	default: throw G.Error("Shouldn't occur");
    	case 2: //meeple plus one chip
    	case 1: break;	// just the meeple
    	case 3: if(pickedDisk!=null) 
    		{ nSingleChips-=2;
    		  lastStackMove = moveNumber; 
    		} 
    		break;
    	case 4:
    		{	StacChip save = dest.removeTop();
    			StacCell fromRack = rack[whoseTurn];
    			dest.addChip(fromRack.removeTop());
    			dest.addChip(save);
    			nSingleChips--;
    			lastStackMove = moveNumber; 
    			if(replay.animate)
    			{	
    			animationStack.push(fromRack);
    			animationStack.push(dest);
    			}
    		}
    		break;
    	case 5: break;
    	}
    }

    
    private void doDone(replayMode replay)
    {	switch(board_state)
    	{
    	case DeclinePending:
    	case AcceptPending:
    	case DrawPending:
    		if(revision>=101) { break; }	
    	//$FALL-THROUGH$
    	default: 
    		lastWasCarry[whoseTurn]=(pickedDisk!=null); 
    		break;
    	}
        checkForClaim(replay);
        acceptPlacement();
        if (board_state==StacState.Resign)
        {	setGameOver(false,true);
        }
        else
        {	boolean win1 = winForPlayerNow(whoseTurn);
        	boolean win2 = winForPlayerNow(nextPlayer[whoseTurn]);
        	if(win1 || win2)  { setGameOver(win1,win2); }
        	else if(nSingleChips==0) { setGameOver(false,false); }
        	else {setNextPlayer(); setNextStateAfterDone(); }
        }
    }
 
    public boolean Execute(commonMove mm,replayMode replay)
    {	StacMovespec m = (StacMovespec)mm;
        if(replay.animate) { animationStack.clear(); }
        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone(replay);

            break;
        case MOVE_CARRY:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case Carry:
        			G.Assert(pickedObject==null,"something is moving");
        			StacCell src = getCell(StacId.ChipLocation, m.from_col, m.from_row);
        			StacCell dest = getCell(StacId.BoardLocation,m.to_col,m.to_row);
        			pickObject(src,StacId.ChipLocation);
        			dropObject(dest); 
        			if(replay.animate)
        			{	animationStack.push(src);
    					animationStack.push(dest);
        				animationStack.push(src);
        				animationStack.push(dest);
        			}
 				    setNextStateAfterDrop();
        			break;
        	}
        	break;          
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case Play:
        		case Carry:
        			G.Assert(pickedObject==null,"something is moving");
        			StacCell src = getCell(StacId.BoardLocation, m.from_col, m.from_row);
        			StacCell dest = getCell(StacId.BoardLocation,m.to_col,m.to_row);
        			pickObject(src,src.rackLocation());
        			dropObject(dest); 
        			if(replay.animate)
        			{	if(pickedDisk!=null) 
        					{ 
        					animationStack.push(src);
        					animationStack.push(dest);
        					}
        				animationStack.push(src);
        				animationStack.push(dest);
        			}
 				    setNextStateAfterDrop();
        			break;
        	}
        	break;
        case MOVE_DROPB:
			{
			StacCell c = getCell(StacId.BoardLocation, m.to_col, m.to_row);
        	G.Assert(pickedObject!=null,"something is moving");
			
            if(isSource(c)) 
            	{ 
            	  unPickObject(); 

            	} 
            	else
            		{
            		dropObject(c);
            		setNextStateAfterDrop();
            		if(replay==replayMode.Single)
        			{	StacCell src = getSource();
        				if(pickedDisk!=null) 
        				{	// if we're dragging a chip do it twice
        					animationStack.push(src);
        					animationStack.push(c);
        				}
        				animationStack.push(src);
        				animationStack.push(c);
        			}
            		}
			}
            break;

        case MOVE_PICKC:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	if(isDest(getCell(m.from_col,m.from_row)))
        		{ unDropObject(); 
        		}
        	else 
        		{
        		StacCell src = getCell(StacId.ChipLocation, m.from_col, m.from_row);
        		if(replay != replayMode.Live)
        			{// provide a dummy animation to keep the picked cell visible
        			 StacCell c = new StacCell(src);
      			 	 animationStack.push(c);
      			 	 animationStack.push(c);
       			 	 animationStack.push(c);
       			 	 animationStack.push(c);
       		 		}
        		  pickObject(src,StacId.ChipLocation);
        		  setState(StacState.CarryDrop);
         		}
 
            break;
            
        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	if(isDest(getCell(m.from_col,m.from_row)))
        		{ unDropObject(); 
        		}
        	else 
        		{StacCell src = getCell(StacId.BoardLocation, m.from_col, m.from_row); 
        		if(replay != replayMode.Live)
       		 	{// provide a dummy animation to keep the picked cell visible
        		 StacCell c = new StacCell(src);
       			 animationStack.push(c);
       			 animationStack.push(c);
       		 	}
       		 pickObject(src,StacId.BoardLocation);
        		  switch(board_state)
        		  {	default: throw G.Error("Not expecting pickb in state %s",board_state);
        		  	case Carry:
              		  setState(StacState.Play);
        		  	  break;
        		  	case Play:
         		  	case Puzzle:
        		  		break;
        		  }
         		}
 
            break;

        case MOVE_DROP: // drop on chip pool;
            dropObject(getCell(m.source, m.to_col, m.to_row));
            setNextStateAfterDrop();

            break;

        case MOVE_PICK:
        	{
        	StacCell c = getCell(m.source, m.from_col, m.from_row);
            pickObject(c,c.rackLocation());
            setNextStateAfterPick();
        	}
            break;


        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(StacState.Puzzle);
            {	boolean win1 = winForPlayerNow(whoseTurn);
            	boolean win2 = winForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;

        case MOVE_RESIGN:
        	setState(unresign==null?StacState.Resign:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            // standardize "gameover" is not true
            setState(StacState.Puzzle);
 
            break;
        case MOVE_OFFER_DRAW:
			if(revision<102 || canOfferDraw())
			{
        	if(board_state==StacState.DrawPending) { setState(pickedState.pop()); }
        	else { if(pickedState.size()==0) { pickedState.push(board_state); }
        		   setState(StacState.DrawPending);
        	}}
        	break;
        case MOVE_ACCEPT_DRAW:
        	if(board_state==StacState.AcceptPending) { setState(pickedState.pop()); }
        	else { if(pickedState.size()==0) { pickedState.push(board_state); }
        		   setState(StacState.AcceptPending);
        	}
        	break;
        case MOVE_DECLINE_DRAW:
        	if(board_state==StacState.DeclinePending) { setState(pickedState.pop()); }
        	else { if(pickedState.size()==0) { pickedState.push(board_state); }
        		   setState(StacState.DeclinePending);
        	}
        	break;
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;
       	
        default:
        	cantExecute(m);
        }

 
        return (true);
    }
    int playerIndex(StacChip ch) 
    { int pi = ch.colorIndex();
      return(pi>=0 ? getColorMap()[pi] : -1);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
         case Confirm:
         case DrawPending:
         case Play: 
         case Resign:
         case AcceptOrDecline:
         case DeclinePending:
         case AcceptPending:
         case RepetitionPending:
         case Carry:
         case CarryDrop:
        	return(false);


		case Gameover:
			return(false);
        case Puzzle:
        	return((pickedObject==null)?true:(player==playerIndex(pickedObject)));
        }
    }
  

    public boolean legalToHitBoard(StacCell cell,Hashtable<StacCell,StacMovespec>targets)
    {	
        switch (board_state)
        {
        case CarryDrop:
        case Carry:
        case Play:
 			return(isSource(cell) || targets.get(cell)!=null);
        case AcceptOrDecline:
		case DrawPending:
		case DeclinePending:
		case AcceptPending:
		case RepetitionPending:
		case Gameover:
		case Resign:
			return(false);
		case Confirm:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case Puzzle:
        	return(pickedObject==null?cell.height()>0:true);
        }
    }
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(StacMovespec m)
    {	//G.print("R "+m);
    	robotState.push(board_state);
    	robotCarry.push(lastWasCarry[whoseTurn]?1:0);
    	robotDepth++;
        if (Execute(m,replayMode.Replay))
        {
            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {	// this makes draw by repetition visible to the robot.
            	// if the winning robot sees a draw by repetition pending
            	// he should be the one to break it up.
                if((robotState.size()<5) && (simpleScore()>0.0))	// we are winning 
                	{ int n = reps.numberOfRepeatedPositions(Digest());
                	  if(n>0) 
                	  { setGameOver(false,true); 	// make it look like a loss for us                	  
                	  }
                	}
                doDone(replayMode.Replay);

            }
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
    public void UnExecute(StacMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	robotDepth--;
    	switch(m.op)
    	{
    	default: throw G.Error("Not expecting move %s",m);
    	case MOVE_DONE:
    	case MOVE_OFFER_DRAW:
    	case MOVE_ACCEPT_DRAW:
    	case MOVE_DECLINE_DRAW:
    		break;
    	case MOVE_CARRY:
    		{
    			StacCell dest = getCell(m.to_col,m.to_row);
    			StacCell src = getCell(m.from_col,m.from_row);
    			StacChip move = dest.removeTop();
    			StacChip disk = dest.removeTop();
    			int pindex = playerIndex(disk);
    			if(pindex>=0) { rack[pindex].addChip(disk); disk = dest.removeTop(); nSingleChips++; }
    			else if(dest.height()>0) { nSingleChips +=2; }
    			pawnLocation[playerIndex(move)]=src;
    			src.addChip(disk);
    			src.addChip(move);
    		}
    		break;
    	case MOVE_BOARD_BOARD:
    		{
			StacCell dest = getCell(m.to_col,m.to_row);
			StacCell src = getCell(m.from_col,m.from_row);
			StacChip move = dest.removeTop();
			pawnLocation[playerIndex(move)]=src;
   			src.addChip(move);
    		}
    		break;
    	}
    	if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
    	setState(robotState.pop());
    	lastWasCarry[whoseTurn]=(robotCarry.pop()==1);
     }
 // add moves to all if legal, return true if we did.
 private boolean addCarryMoves(CommonMoveStack all, StacCell origin,StacCell to,int direction,int who)
 {	if(to!=null)
 	{	StacChip top = to.topChip();
 		boolean some = false;
 		if((top==StacChip.redBoss)||(top==StacChip.blueBoss)) { return(false); }	// can't land there, stop movement
 		if(to.height()<3)
 		{	if(all!=null) 
 			{ 
 			all.push(new StacMovespec(MOVE_CARRY,origin,to,who));
 			}
 			some = true;
 		}
 		some |= addCarryMoves(all,origin,to.exitTo(direction),direction,who);
 		return(some);
 	}
 	return(false);
 }
 
 // add moves to all, return true if we did 
 private boolean addMoveMoves(CommonMoveStack all, StacCell origin,StacCell to,int direction,int who)
 {	if(to!=null)
 	{	StacChip top = to.topChip();
 		boolean some = false;
 		if((top==StacChip.redBoss)||(top==StacChip.blueBoss)) {}	// can't land there
 		else
 		{ 	if(all==null) { return(true); }
 			all.push(new StacMovespec(MOVE_BOARD_BOARD,origin,to,who)); 
 			some = true;
 		}
 		some |= addMoveMoves(all,origin,to.exitTo(direction),direction,who);
 		return(some);
 	}
 	return(false);
 }
 
 // add meeple moves from a particular spot, not carrying anything.
 private boolean addMoveMoves(CommonMoveStack all, StacCell from,int who)
 {	boolean some = false;
 	for(int direction = from.geometry.n-1; direction>=0; direction--)
 	{	some |= addMoveMoves(all,from,from.exitTo(direction),direction,who);
 		if(some && (all==null)) { return(true); }
 	}
 	return(some);
 }
 
 // add carry moves from a particular spot
 private boolean addCarryMoves(CommonMoveStack all, StacCell from,int who)
 {	
 	boolean some = false;
	for(int direction = from.geometry.n-1; direction>=0; direction--)
		{
		some |= addCarryMoves(all,from,from.exitTo(direction),direction,who);
		if(some && (all==null)) { return(true); }
		}
		return(some);
 	
 }
 
 // add moves that carry a disc from one spot to another
 public boolean addCarryMoves(CommonMoveStack all,int who)
 { 	StacCell from = pawnLocation[whoseTurn];
 	if((!lastWasCarry[who]) && from.height()==2)
	{	return(addCarryMoves(all,from,who));
	}
	return(false);
 }
 
 // convert a list of moves to a hash indexed by sources
 private void movesToHashFrom(CommonMoveStack all,Hashtable<StacCell,StacMovespec>h)
 {	while(all.size()>0) // we depend on taking moves from top so the first on the stack will take precedence
 	{ 	StacMovespec m = (StacMovespec)all.pop();
 		switch(m.op)
 		{
 		case MOVE_BOARD_BOARD:
 		case MOVE_CARRY:
 			h.put(getCell(m.from_col,m.from_row),m);
 			break;
 		default: break;
 		}
 	}
 }
 
 // convert a list of moves to a hash indexed by the destinations
 private void movesToHashTo(CommonMoveStack all,Hashtable<StacCell,StacMovespec>h)
 {	while(all.size()>0)	// we depend on taking moves from top so the first on the stack will take precedence
 	{ 	StacMovespec m = (StacMovespec)all.pop();
 		switch(m.op)
 		{
 		case MOVE_BOARD_BOARD:
 		case MOVE_CARRY:
 			h.put(getCell(m.to_col,m.to_row),m);
 			break;
 		default: break;
 		}
 	}
 }
 public boolean hasCarryMoves()
 {	return(addCarryMoves(null,whoseTurn));
 }
 //
 // get the UI targets of the current state.  If there
 // is nothing picked up, that would be the places that can be picked up.
 // if something is picked up, that would be where it can land.
 //
 public Hashtable<StacCell,StacMovespec>getTargets()
 {	Hashtable<StacCell,StacMovespec>h = new Hashtable<StacCell,StacMovespec>();
 	CommonMoveStack all = new CommonMoveStack();
 	if(pickedObject==null)
 	{	// nothing picked up
 		switch(board_state)
 		{
 		case Carry:
 			addCarryMoves(all,whoseTurn) ;	// carry moves first, so will override non-carry 
			//$FALL-THROUGH$
		case Play:
 			addMoveMoves(all,pawnLocation[whoseTurn],whoseTurn) ;
 			break;
		default: break;
 		}
		movesToHashFrom(all,h);
	}
 	else
 	{	// something picked up
 		switch(board_state)
 		{
 		case Play:
 			addMoveMoves(all,pickedSourceStack.top(),whoseTurn) ;
			break;
 		case Carry:
 		case CarryDrop:
 			addCarryMoves(all,pickedSourceStack.top(),whoseTurn) ;
			break;
		default: break;
 		}
		movesToHashTo(all,h);
 	}

	return(h);
 }
 CommonMoveStack  GetListOfMoves(boolean offerdraw)
 {	CommonMoveStack all = new CommonMoveStack();
 	switch(board_state)
	 {
	 default: throw G.Error("Not expecting state %s",board_state);

	 case AcceptOrDecline:
 			all.push(new StacMovespec(MOVE_ACCEPT_DRAW,whoseTurn));
 			all.push(new StacMovespec(MOVE_DECLINE_DRAW,whoseTurn));
 			break;

	 case Carry:
		 addCarryMoves(all,whoseTurn);	 
		//$FALL-THROUGH$
	 case Play:
		 addMoveMoves(all,pawnLocation[whoseTurn],whoseTurn);
		 if( canOfferDraw()
				 && offerdraw
				 && ((moveNumber-lastStackMove)>(4*nSingleChips))
				 && ((moveNumber-lastDrawMove)>(2*nSingleChips)))
		 {
			 all.push(new StacMovespec(MOVE_OFFER_DRAW,whoseTurn));
		 }
		 break;
	 }
 	return(all);
 }
public boolean canOfferDraw() {
	return (moveNumber-lastDrawMove>4);
}
 
}
