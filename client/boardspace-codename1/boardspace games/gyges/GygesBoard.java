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
package gyges;


import online.game.*;

import java.util.*;

import lib.*;
import lib.Random;


/**
 * GygesBoard knows all about the game of Gyges, which is played
 * on a 7x7 board. It gets a lot of logistic support from 
 * common.rectBoard, which knows about the coordinate system.  
 * 
 * This class doesn't do any graphics or know about anything graphical, 
 * but it does know about states of the game that should be reflected 
 * in the graphics.
 * 
 *  The principle interface with the game viewer is the "Execute" method
 *  which processes moves.  Note that this
 *  
 *  In general, the undoInfo of the game is represented by the contents of the board,
 *  whose turn it is, and an explicit undoInfo variable.  All the transitions specified
 *  by moves are mediated by the undoInfo.  In general, my philosophy is to be extremely
 *  restrictive about what to allow in each undoInfo, and have a lot of tripwires to
 *  catch unexpected transitions.   We expect to be fed only legal moves, but mistakes
 *  will be made and it's good to have the maximum opportunity to catch the unexpected.
 *  
 * Note that none of this class shows through to the game controller.  It's purely
 * a private entity used by the viewer and the robot.
 * 
 * @author ddyer
 *
 */


class GygesBoard extends squareBoard<GygesCell> implements BoardProtocol,GygesConstants
{
	static final int GYGES_COLUMNS = 6;	// 8x6 board
	static final int GYGES_ROWS = 6;
    static final GygesId PlayerPool[] = { GygesId.First_Player_Pool, GygesId.Second_Player_Pool};
    static final GygesId PlayerGoal[] = { GygesId.First_Player_Goal,GygesId.Second_Player_Goal };
    static final int PlayerHomeRow[] = {6,1};
    static final GygesState PlayerPlayState[] = { GygesState.PlayTop, GygesState.PlayBottom} ;
    static final GygesState PlayerPlaceState[] = { GygesState.PlaceTop, GygesState.PlaceBottom} ;
    static final GygesState PlayerDropState[] = { GygesState.DropTop, GygesState.DropBottom };
	
	private GygesState board_state = GygesState.Puzzle;	
    static final int HomeDirection[] = { CELL_UP, CELL_DOWN };
	private GygesState unresign = null;	// remembers the orignal state when "resign" is hit
	public GygesState getState() { return(board_state); }
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(GygesState st) 
	{ 	unresign = (st==GygesState.Resign) ? board_state : null; 
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
    public int boardColumns = GYGES_COLUMNS;	// size of the board
    public int boardRows = GYGES_ROWS;
    public void SetDrawState() { setState(GygesState.Draw); }
    public GygesCell rack[][] = new GygesCell[2][];
    public GygesCell goalCell[] = new GygesCell[2];
    public CellStack animationStack = new CellStack();
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public GygesChip pickedObject = null;
    public CellStack pickedSourceStack = new CellStack();
    public CellStack droppedDestStack = new CellStack();
    public boolean playing = false;
    public boolean dropping = false;
    private boolean canDisplace = false;
    private IStack robotUndo = new IStack();
    public int robotDepth = 0;
    private StateStack robotState = new StateStack();
    public GygesCell dropSource = null;
    private boolean racksEmpty() 
    {	for(GygesCell row[] : rack) { for(GygesCell cell : row) 
    	{ if(cell.topChip()!=null) 
    		{ return(false); }}}
    	return(true);
    }
    private boolean myRackEmpty() 
    {	for(GygesCell cell : rack[whoseTurn]) { if(cell.topChip()!=null) { return(false); }}
    	return(true);
    }

	// factory method
	public GygesCell newcell(char c,int r)
	{	return(new GygesCell(c,r));
	}
    public GygesBoard(String init,long key) // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = GYGESGRIDSTYLE; //coordinates left and bottom
    	Random r = new Random(67246765);
	    initBoard(boardColumns,boardRows); //this sets up the board and cross links

	    for(int pl=FIRST_PLAYER_INDEX; pl<=SECOND_PLAYER_INDEX; pl++)
     	{
     	 rack[pl] = new GygesCell[GygesChip.N_STANDARD_CHIPS*2];
     	 GygesCell goal = goalCell[pl] = newcell('G',pl);
     	 goal.randomv = r.nextLong();
     	 
     	 // link all the home row cells to the goal.
     	 for(GygesCell seed = getCell('A',PlayerHomeRow[pl]); seed!=null; seed = seed.exitTo(CELL_RIGHT))
     	 {	seed.addLink(HomeDirection[pl], goal);
     	 }
      	 for(int i=0;i<GygesChip.N_STANDARD_CHIPS*2; i++)
	    	{
	       	GygesCell cell = new GygesCell(r,PlayerPool[pl]);
	       	cell.row = i;
	       	rack[pl][i]=cell;
     	}}
	    allCells.setDigestChain(r);

        doInit(init,key); // do the initialization 
     }


	public void sameboard(BoardProtocol f) { sameboard((GygesBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(GygesBoard from_b)
    {
    	super.sameboard(from_b);	// calls sameCell for each cell
    	G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(playing==from_b.playing,"playing mismatch");    
        G.Assert(dropping==from_b.dropping,"dropping mismatch");  
        G.Assert(sameCells(dropSource,from_b.dropSource),"dropSource mismatch");
        G.Assert(canDisplace==from_b.canDisplace,"canDisplace mismatch");
        
        G.Assert(sameCells(rack,from_b.rack),"rack mismatch");
        G.Assert(sameCells(goalCell,from_b.goalCell),"goalCells mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject doesn't match");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");

    }

    /** 
     * Digest produces a 64 bit hash of the game undoInfo.  This is used in many different
     * ways to identify "same" board states.  Some are germane to the ordinary operation
     * of the game, others are for system record keeping use; so it is important that the
     * game Digest be consistent both within a game and between games over a long period
     * of time which have the same moves. 
     * (1) Digest is used by the default implementation of EditHistory to remove moves
     * that have returned the game to a previous undoInfo; ie when you undo a move or
     * hit the reset button.  
     * (2) Digest is used after EditHistory to verify that replaying the history results
     * in the same game as the user is looking at.  This catches errors in implementing
     * undo, reset, and EditHistory
	 * (3) Digest is used by standard robot search to verify that move/unmove 
	 * returns to the same board undoInfo, also that move/move/unmove/unmove etc.
	 * (4) Digests are also used as the game is played to look for draw by repetition.  The undoInfo
     * after most moves is recorded in a hashtable, and duplicates/triplicates are noted.
     * (5) games where repetition is forbidden (like xiangqi/arimaa) can also use this
     * information to detect forbidden loops.
	 * (6) Digest is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * undoInfo of the game, and a midpoint undoInfo of the game. Other site machinery
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
        v ^= Digest(r,rack);
        v ^= Digest(r,goalCell);
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,dropSource);
		v ^= (playing ? 1 : 2) * r.nextLong();
		v ^= (dropping ? 1 : 2) * r.nextLong();
		v ^= (canDisplace ? 1 : 2) * r.nextLong();
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
        return (v);
    }
   public GygesBoard cloneBoard() 
	{ GygesBoard copy = new GygesBoard(gametype,randomKey);
	  copy.copyFrom(this); 
	  return(copy);
	}

   public void copyFrom(BoardProtocol b) { copyFrom((GygesBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(GygesBoard from_b)
    {	super.copyFrom(from_b);
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        playing = from_b.playing;
        dropping = from_b.dropping;
        dropSource = getCell(from_b.dropSource);
        canDisplace = from_b.canDisplace;
        pickedObject = from_b.pickedObject;	
        copyFrom(rack,from_b.rack);
        copyFrom(goalCell,from_b.goalCell);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);   
        unresign = from_b.unresign;
        robotDepth = from_b.robotDepth;
        sameboard(from_b);
    }

    /* initialize a board back to initial empty undoInfo */
    public void doInit(String gtype,long key)
    {	randomKey = key;	// not used, but for reference in this demo game
    	for(GygesCell c = allCells; c!=null; c=c.next) { c.reInit(); }
     	for(int pl=FIRST_PLAYER_INDEX; pl<=SECOND_PLAYER_INDEX; pl++)
     	{goalCell[pl].reInit();
     	 for(int i=0;i<GygesChip.N_STANDARD_CHIPS*2; i++)
	    	{
	       	GygesCell cell = rack[pl][i];
	       	cell.reInit();
	       	cell.addChip(GygesChip.getChip(i/2));
	    	}}    
     	{
     	if(Gyges_INIT_beginner.equalsIgnoreCase(gtype)) 
     		{ canDisplace = false;
     		}
     	else if(Gyges_INIT_advanced.equalsIgnoreCase(gtype))
     	{	canDisplace = true;
     	}
     	else { throw G.Error(WrongInitError,gtype); }
     	gametype = gtype;
     	}
	    setState(GygesState.Puzzle);
	    playing = false;
	    dropping = false;
	    dropSource = null;
	    whoseTurn = FIRST_PLAYER_INDEX;
		pickedSourceStack.clear();
		droppedDestStack.clear();
		robotUndo.clear();
		robotDepth = 0;
		pickedObject = null;
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
        case Draw:
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
            return (true);

        default:
            return (false);
        }
    }


    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(GygesState.Gameover);
    }
    
    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	if(board_state==GygesState.Gameover) { return(win[player]); }
    	return(false);
    }
    // estimate the value of the board position.
    public double ScoreForPlayer(int player,boolean print,double cup_weight,double ml_weight,boolean dumbot)
    {  	
    	throw G.Error("not implemented");
     }


    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement()
    {	
        pickedObject = null;
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
    	GygesCell dr = droppedDestStack.pop();
    	switch(dr.rackLocation())
	    	{
	   		default: throw G.Error("Not expecting rackLocation %s",dr.rackLocation);
			case First_Player_Pool:	
			case Second_Player_Pool:
			case BoardLocation: 
				pickedObject = dr.removeTop(); 
				break;
	    	
	    	}
	    	}
    }
    // 
    // undo the pick, getting back to base undoInfo for the move
    //
    private void unPickObject()
    {	GygesChip po = pickedObject;
    	if(po!=null)
    	{
    		GygesCell ps = pickedSourceStack.pop();
    		switch(ps.rackLocation())
    		{
    		default: throw G.Error("Not expecting rackLocation %s",ps.rackLocation);
    		case First_Player_Pool:	
    		case Second_Player_Pool:
    		case BoardLocation: ps.addChip(po); break;
    		}
    		pickedObject = null;
     	}
     }

    // 
    // drop the floating object.
    //
    private void dropObject(GygesCell c)
    {   G.Assert(pickedObject!=null,"pickedObject should not be null"); 	    		
    	switch(c.rackLocation())
		{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case First_Player_Pool:
		case Second_Player_Pool:
		case BoardLocation: c.addChip(pickedObject);
			pickedObject = null;
	       	droppedDestStack.push(c);
			break;
		}
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(GygesCell cell)
    {	return((droppedDestStack.size()>0) && (droppedDestStack.top()==cell));
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	GygesChip ch = pickedObject;
    	if(ch!=null)
    		{ return(ch.chipNumber());
    		}
        return (NothingMoving);
    }
    public int cellToY(GygesCell c)
    {	if(c.col=='G')
    		{
    		// G0 is the top goal, G1 is the bottom goal.
    		int row = (c.row!=0)? 1 : 6;
    		int row2 = (c.row!=0) ? 2 : 5;
    		int y1 = cellToY('C',row);
    		int y2 = cellToY('C',row2);
    		return(y1+(y1-y2));
    		}
    	else 
    		{ return(super.cellToY(c));
    		}
    }  
    public int cellToX(GygesCell c)
    {	if(c.col=='G')
    		{
    		int row = (c.row==0)? 1 : 6;
    		int x1 = cellToX('C',row);
    		int x2 = cellToX('D',row);
    		return((x1+x2)/2);
    		}
    	else 
    		{ return(super.cellToX(c));
    		}
    }
    public GygesCell getCell(char col,int row)
    {	if(col=='G') 
    		{ return(goalCell[row]); 
    		}
    		else 
    		{ return(super.getCell(col,row)); 
    		}
    }
    public GygesCell getCell(GygesId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	return(getCell(col,row));
        case First_Player_Goal:
        	return(goalCell[FIRST_PLAYER_INDEX]);
        case Second_Player_Goal:
        	return(goalCell[SECOND_PLAYER_INDEX]);
        case Second_Player_Pool:
        	return(rack[SECOND_PLAYER_INDEX][row]);
        case First_Player_Pool:
       		return(rack[FIRST_PLAYER_INDEX][row]);
        }
    }
    public GygesCell getCell(GygesCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(GygesCell c)
    {	G.Assert(pickedObject==null,"pickedObject should be null");
    	switch(c.rackLocation())
    	{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case BoardLocation: 
		case First_Player_Pool:	
		case Second_Player_Pool:
			pickedObject = c.removeTop(); 
			break;
   	
    	}
    	pickedSourceStack.push(c);
   }
    public boolean rackEmpty(int pl)
    {	for(GygesCell c : rack[pl]) { if(c.topChip()!=null) { return(false); }}
    	return(true);
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
        case DropTop:
        case DropBottom:
        	setState(GygesState.Confirm);
        	break;
        case PlaceTop:
        case PlaceBottom:
        	acceptPlacement();
        	if(rackEmpty(whoseTurn)) { setState(GygesState.Confirm); } 
        	break;
        case Confirm:
        case Draw:
        	setNextStateAfterDone(); 
        	break;
        case PlayTop:
        case PlayBottom:
        case Continue:
        	if(pickedObject!=null)
        	{	// continue with additional moves
        		setState(GygesState.Continue);
        	}
        	else { setState(GygesState.Confirm); }
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
    public boolean isSource(GygesCell c)
    {	return((pickedSourceStack.size()>0) && (pickedSourceStack.top()==c));
    }
    public GygesCell getSource()
    {	return((pickedSourceStack.size()>0) ? pickedSourceStack.top() : null);
    }
    public boolean isBounceSource(GygesCell c)
    {	if(pickedSourceStack.size()==1) { return(c==pickedSourceStack.top()); }
    	
    	for(int idx = pickedSourceStack.size()-2; idx>=0; idx--)
    		{
    		if(c==pickedSourceStack.elementAt(idx)) { return(true); }
    		}
    	return(false);
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterPick()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting pick in undoInfo %s", board_state);
        case Confirm:
        case Draw:
        	setState((playing?(dropping?PlayerDropState:PlayerPlayState):PlayerPlaceState)[whoseTurn]);
        	break;
        case PlayTop:
        case PlayBottom:
        case PlaceTop:
        case PlaceBottom:
			break;
        case Puzzle:
            break;
        }
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting undoInfo %s",board_state);
    	case Gameover: 
    		break;

        case Draw:
        	setGameOver(false,false);
        	break;
        case Puzzle:
        	if(racksEmpty())
        		{	// both racks empty, start normal play
        		setState(PlayerPlayState[whoseTurn]); 
        	    }
        	else 
        	{ // top rack empty, bottom rack not empty
        		if(myRackEmpty()) { setWhoseTurn(nextPlayer[whoseTurn]); }
        		setState(whoseTurn==0 ? GygesState.PlaceTop : GygesState.PlaceBottom); 
        	}
        	break;
        case PlaceBottom:
        	if(racksEmpty()) { playing = true; };
			//$FALL-THROUGH$
		case Confirm:
    	case PlayTop:
    	case PlayBottom:
    		dropping = false;
    		dropSource = null;
    		if(playing || racksEmpty())
    		{
    		playing = true;
    		if(goalCell[whoseTurn].topChip()!=null) { setGameOver(false,true); }
    		else { setState(PlayerPlayState[whoseTurn]); }
    		}
    		else
    		{  
    		   setState(whoseTurn==0 ? GygesState.PlaceTop : GygesState.PlaceBottom);
    		}
    		break;
    	}

    }
   

    
    private void doDone()
    {	
        acceptPlacement();

        if (board_state==GygesState.Resign)
        {	setGameOver(false,true);
        }
        else
        {	boolean win1 = WinForPlayerNow(whoseTurn);
        	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
        	if(win1 || win2)  { setGameOver(win1,win2); }
        	else {setNextPlayer(); setNextStateAfterDone(); }
        }
    }

    private void animatePath(GygesCell origin,GygesCell src,GygesCell dest,GygesChip po,CellStack bounces,GygesCell empty,int who)
    {	CellStack localBounces = bounces;
    	GygesCell localDest = dest;
    	if((dest!=null) && (bounces!=null))
    		{localBounces = new CellStack();
    		 localBounces.copyFrom(bounces);
    		 localBounces.push(dest);
    		 localDest = null;
    		}

    	CellStack path = getMovePath(origin,localDest,po,localBounces,empty,who);
    	boolean started = (src==origin);
		if(path!=null)
		{	GygesCell prev = origin;
			for(int i=0;i<path.size();i++) 
			{	GygesCell current = path.elementAt(i);
				if(started)
				{
				animationStack.push(prev);
				animationStack.push(current);
				}
				if((current!=empty) && (current==dest)) { break; }
				started |= (current==src);
				prev = current;
			}
		}
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	GygesMovespec m = (GygesMovespec)mm;

        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone();

            break;
        case MOVE_RACK_BOARD:
           	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in undoInfo %s",board_state);
        		case PlaceTop:
        		case PlaceBottom:
        		{	
        			G.Assert(pickedObject==null,"something is moving");
        			GygesCell src = getCell(m.source, m.from_col, m.from_row);
                    pickObject(src);
                    m.chip = pickedObject;
                    GygesCell dest = getCell(GygesId.BoardLocation,m.to_col,m.to_row);
                    dropObject(dest); 
        			if(replay!=replayMode.Replay)
        			{	animationStack.push(src);
        				animationStack.push(dest);
        			}
  
                    setNextStateAfterDrop();
                    break;
        			}
        	}
        	break;
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in undoInfo %s",board_state);
        		case PlayTop:
        		case PlayBottom:
        			{
        			G.Assert(pickedObject==null,"something is moving");
        			GygesCell src = getCell(GygesId.BoardLocation, m.from_col, m.from_row);
        			GygesCell dest = getCell(GygesId.BoardLocation,m.to_col,m.to_row); 
        			pickObject(src);
        			GygesChip po = pickedObject;
        			m.chip = po;
        			if(dest.topChip()!=null)
        			{	// drop and displace
        				GygesChip temp = dest.removeTop();
        				dropObject(dest);
        				pickedObject = temp;
        				setState(PlayerDropState[whoseTurn]);
        				dropping = true;
        				dropSource = dest;
        			}
        			else
        			{
        			dropObject(dest); 
 				    setNextStateAfterDrop();
        			}
        			if(replay!=replayMode.Replay)
        			{	animatePath(src,src,dest,po,null,null,whoseTurn);     				
        			}
        			break;
        			}
        	}
        	break;
        case MOVE_DROPB:
        case MOVE_DROPB_R:
			{
			GygesCell c = getCell(GygesId.BoardLocation, m.to_col, m.to_row);
        	G.Assert(pickedObject!=null,"something is moving");
			GygesCell src = getSource();
            if(isBounceSource(c) && (board_state!=GygesState.DropTop) && (board_state!=GygesState.DropBottom)) 
            	{ 
            	  unPickObject(); 
            	} 
            	else
            		{
            		if((replay==replayMode.Single)
            				|| ((m.op==MOVE_DROPB_R) && (replay==replayMode.Live)))
            		{	if(dropping && dropSource!=null)
            			{	animationStack.push(dropSource);
            				animationStack.push(c);
            			}
            			else if(src!=null)
            			{
            			if(src.onBoard)
            			{	
            				animatePath(pickedSourceStack.elementAt(0),src,c,pickedObject,pickedSourceStack,((c.topChip()==null)?c:null),whoseTurn);
            			}
            			else
            			{
            				animationStack.push(src);
            				animationStack.push(c);
            			}}
            			
            		}
            		if(c.topChip()!=null)
            		{
            			if(canDisplace && (c==src))
            			{	GygesChip po = c.removeTop();
            				dropObject(c);
            				c.addChip(po);
            				pickObject(c);
                       		m.chip = pickedObject;
                       		setState(PlayerDropState[whoseTurn]);
            				dropping = true;
            				dropSource = c;
            			}
            			else
            			{	// normal bounce
            				dropObject(c);
            				pickObject(c);
            				setNextStateAfterDrop();
            			}
            		}
            		else
            		{
            		switch(board_state)
            		{	case Continue:
        				case PlayTop: 
            			case PlaceTop: 
            			case Puzzle:
            			default:
            				break;
            			case DropTop:
            				m.chip = pickedObject;
            				break;
            		}
             		dropObject(c);
            		setNextStateAfterDrop();
            		}}
			}
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	if(isDest(getCell(m.from_col,m.from_row)))
        		{ unDropObject(); 
        		  setNextStateAfterPick();
        		}
        	else 
        		{ pickObject(getCell(GygesId.BoardLocation, m.from_col, m.from_row));
        			// if you pick up a gobblet and expose a row of 4, you lose immediately
        		  m.chip = pickedObject;
        		  switch(board_state)
        		  {	case Confirm:
        			  	if(!playing) { setNextStateAfterPick();  break; }
	        			//$FALL-THROUGH$
					default: throw G.Error("Not expecting pickb in undoInfo %s",board_state); 
        			case PlayTop:
        			case PlayBottom:
             		case PlaceTop:
            		case PlaceBottom:
             		  		// if we pick a piece off the board, we might expose a win for the other player
        		  		// and otherwise, we are comitted to moving the piece
         		  		break;
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
        	GygesCell c = getCell(m.source, m.from_col, m.from_row);
            pickObject(c);
            m.chip = pickedObject;
            setNextStateAfterPick();
        	}
            break;


        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            // standardize the gameover undoInfo.  Particularly importing if the
            // sequence in a game is resign/start
            setState(GygesState.Puzzle);
            playing = racksEmpty();
            {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{ 
            	setNextStateAfterDone(); 
            	}
            }
            break;

        case MOVE_RESIGN:
        	setState(unresign==null ? GygesState.Resign : unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            // standardize "gameover" is not true
            playing = false;
            setState(GygesState.Puzzle);
 
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
    public boolean LegalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting undoInfo %s", board_state);
        case Draw:
        case PlayTop:
        case DropTop:
        case DropBottom:
        case Resign:
        case PlayBottom:
        case Confirm:
        case Continue:
        	 return(false);
        case PlaceTop:
        case PlaceBottom:
        	return(player==whoseTurn);


		case Gameover:
			return(false);
        case Puzzle:
        	return(true);
        }
    }

    public boolean legalToHitBoard(GygesCell cell,Hashtable<GygesCell,GygesCell>sources,Hashtable<GygesCell,GygesCell>dests)
    {	
        switch (board_state)
        {
 		case PlaceTop:
 		case PlaceBottom:
			return( (cell.row==PlayerHomeRow[whoseTurn]) && ((pickedObject==null) != (cell.topChip()==null)));

		case Continue:
		case PlayTop:
 		case PlayBottom:
 			{	if(pickedObject==null)
 				{	
 					return(sources.get(cell)!=null);
 				}
 				else
 				{	
 					return(isSource(cell) || (dests.get(cell)!=null));
 				}
 			}
 		case Resign:
		case Gameover:
			return(false);
		case Confirm:
		case Draw:
			return(playing?isDest(cell):(cell.row==PlayerHomeRow[whoseTurn]));
		
		case DropTop:
				{
				int home = playerHomeRow(1);
				return((cell.col!='G') && (cell.row>=home) && (cell.topChip()==null));
				}
		case DropBottom:
			{ 
				int home = playerHomeRow(0);
				return((cell.col!='G') && (cell.row<=home) && (cell.topChip()==null));
			}
       default:
    	   throw G.Error("Not expecting undoInfo %s", board_state);
        case Puzzle:
        	return(pickedObject==null?(cell.height()>0):true);
        }
    }
  public boolean canDropOn(GygesCell cell)
  {		GygesCell top = (pickedObject!=null) ? pickedSourceStack.top() : null;
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
    public void RobotExecute(GygesMovespec m)
    {	robotState.push(board_state);
    	robotDepth++;
    	robotUndo.push((dropping ? 2 : 0) | (playing ? 1 : 0)); //record the starting undoInfo. The most reliable
        if((board_state==GygesState.DropTop)||(board_state==GygesState.DropBottom))
        {	m.from_col = dropSource.col;
        	m.from_row = dropSource.row;
        }
        // to undo undoInfo transitions is to simple put the original undoInfo back.
        //G.print("r "+m);
        if (Execute(m,replayMode.Replay))
        {	
        	if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {
                doDone();
            }
            else 
            {	GygesChip po = pickedObject;
            	acceptPlacement();
            	pickedObject = po;
            }
         }
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(GygesMovespec m)
    {
    	//G.print("u "+m);
    	robotDepth--;
        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;

        case MOVE_DONE:
            break;
        case MOVE_DROPB:
        case MOVE_DROPB_R:
        	{
        	acceptPlacement();
        	GygesCell dest = getCell(m.to_col,m.to_row);
        	pickedObject = dest.removeTop();
        	dropSource = getCell(m.from_col,m.from_row);	// saved in robotexecute
         	}
        	break;
        case MOVE_RACK_BOARD:
        	G.Assert(pickedObject==null,"something is moving");
        	pickObject(getCell(GygesId.BoardLocation,m.to_col,m.to_row));
        	dropObject(getCell(m.source,m.from_col, m.from_row));
       		acceptPlacement();
        	break;
        case MOVE_BOARD_BOARD:
        	GygesCell dest = getCell(GygesId.BoardLocation, m.to_col, m.to_row);
        	GygesCell src = getCell(GygesId.BoardLocation, m.from_col,m.from_row);
        	if(pickedObject!=null)
        	{
        		// unwinding after unwinding a displace
        		GygesChip po = pickedObject;
        		pickedObject = dest.removeTop();
        		dest.addChip(po);
        		dropObject(src);
        		dropSource = null;
        	}
        	else
        	{
       		pickObject(dest);
      		dropObject(src); 
        	}
       		acceptPlacement();
        	break;
        case MOVE_RESIGN:
            break;
        }
        int undo = robotUndo.pop();
        playing = (undo & 1 )!=0;
        dropping = (undo & 2) !=0;
        setState(robotState.pop());
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
 // encode a path in-cell-out as an integer
 static final int makeStep(GygesCell c,GygesCell d)
 {	int from = c.col<<8 | c.row;
 	int to = d.col<<8 | d.row;
 	int canonical = (from<to) ? ((from<<16)|to) : ((to<<16) | from);
	return(canonical);
 }
 
 
 // return true if this move through an empty space would conform to the path rule,
 // which states that the path through the cell must be unique
 boolean canFollowPathInDirection(IStack steps,GygesCell c,GygesCell d)
 {	if(steps==null) { return(true); }
	int step = makeStep(c,d);
 	for(int lim = steps.size()-1; lim>=0; lim--)
 	{	if(steps.elementAt(lim)==step) { return(false); }
 	}
 	return(true);
 }
 public boolean generateMove(CommonMoveStack all,GygesCell start,GygesCell stop,GygesCell dest,
		 CellStack bounces,CellStack steps,
		 boolean savePath,int who,boolean winningMoveOptimization)
 {	 boolean dup = false;
 	 boolean win = winningMoveOptimization && (dest.col=='G');
 	 if(win) { all.clear(); }
	 if(stop==null) 
	 {	if(!savePath)
		{for(int lim = all.size()-1; lim>=0 && !dup; lim--)
		{
		GygesMovespec m = (GygesMovespec)all.elementAt(lim);
		dup |= (m.op==MOVE_BOARD_BOARD)
			&& (m.from_col==start.col)
			&& (m.from_row==start.row)
			&& (m.to_col==dest.col)
			&& (m.to_row==dest.row);
		}} 
	 }
	 else { dup = (stop!=dest) ; }
	if(!dup)
		{
		GygesMovespec mov = new GygesMovespec(MOVE_BOARD_BOARD,start.col,start.row,dest.col,dest.row,who);
		if(savePath)
		{
		if(steps!=null)
			{
			mov.steps = new CellStack();
			mov.steps.copyFrom(steps);
			mov.steps.push(dest);
			}
		if(bounces!=null)
			{
			mov.bounces = new CellStack();
			mov.bounces.copyFrom(bounces);
			mov.bounces.push(dest);
			}
		}
		all.push(mov);
		return(win);
		}
	return(win);
}
 // 
 // this is the real move generator, with lots of options to server many masters.
 //
 // "start" is always the ultimate origin of the move,
 // "stop" can be null or the desired entpoint of the move.  If stop is specified, all the moves that go from start to stop are returned.
 // "from" is the current bounce point, or the origin.  
 // "top" is always the piece that is moving.
 // "who" is the player moving.
 // "single" is a flag which terminates the move at the first bounce.  This is used in the UI to get single steps.
 //
 // "distanceToMove" is the step count remaining in the current bounce segment.  This is decremented in recursion.
 // "bounces" always records the points where a bounce occurs.  If savePath is true, it will be copied and saved in the move.path
 // "steps" is optional, if present records every step in the path, not just the bounces.  If in use it will be saved instead of bounces.
 //
 // "path" is an encoding of the path taken through empty spaces.
 // "indir" is the incoming direction of "from" or -1
 //
 // 
 boolean getMovesFrom(CommonMoveStack all,	// the result list of moves
		 	IStack path,		// the full path, encoded as integers which indicate the cell and enter and exit directions
		 	CellStack bounces,	// the chain of places where the move bounces on an occupied space
		 	CellStack steps,		// the chain of single steps the move takes, including empty cells and bounces
		 	GygesCell start,			// the starting cell for the move, which will be treated as empty for traversal
		 	GygesCell stop,				// place to stop, or null
		 	GygesCell from,				// the origin of this particular bounce-step
		 	GygesChip top,				// the piece that is actually moving.
		 	int distanceToMove,			// how many steps remain to take
		 	int who,					// the player to move
		 	GygesCell empty,			// a cell regarded as empty (used if dest is already placed)
		 	boolean savePath,			// if true, save the path information about the move
		 	boolean winningMoveOptimization)	// if true, stop with a winning move
 {	
 	switch(distanceToMove)
 		{
 			case 1:
 				// this is the last step, so either bounce, replace, or terminate here.
 				for(int dir = 0;dir<CELL_FULL_TURN; dir++)
 				{	
  					GygesCell dest = from.exitTo(dir);
 					if((dest!=null) 
 							&& (dest!=start) 
 							&& canFollowPathInDirection(path,from,dest))		// disallow ending where we started
 					{	GygesChip dtop = dest.topChip();
 						if((dest!=empty) && (dtop!=null))
 						{	// an occupied position we can bounce from
 							if(!bounces.contains(dest))
 							{	// can't bounce on the same cell twice, no point
 								
 								if(dest==stop) 
 								{
 									boolean exit = generateMove(all,start,stop,dest,bounces,steps,savePath,who,winningMoveOptimization);
 									if(exit) { return(true); }
 								}
 								else
 								{
 								// continue with more steps
 								boolean exit = false;
 								bounces.push(dest);
 								path.push(makeStep(from,dest));
 								if(steps!=null) { steps.push(dest); }
	 							// bounce off an occupied cell
 								if(canDisplace)
 									{ exit |=  generateMove(all,start,stop,dest,bounces,steps,savePath,who,winningMoveOptimization);
 									}
	 							exit |= getMovesFrom(all,path,bounces,steps,start,stop,dest,top,dtop.getMovement(),who,empty,savePath,winningMoveOptimization);
	 							if(steps!=null) { steps.pop(); }
	 							path.pop();
	 							bounces.pop();
	 							if(exit) { return(true); }
								}
 						}}
 						else if(dest!=goalCell[who])	// no auto-goal
 	 					{ 
 						// land on an empty space
 						if(all.size()>1000)
 						{
 						G.Assert(all.size()<10000,"too many moves");
 						}
						boolean  exit = generateMove(all,start,stop,dest,bounces,steps,savePath,who,winningMoveOptimization);
						if(exit) { return(true); }
  						}
 					}
 				}
 				break;
 			default:
 				// not the last step, continue through empty spaces.
 				for(int dir=0;dir<CELL_FULL_TURN; dir++)
 				{	
  					GygesCell dest = from.exitTo(dir);
 					if((dest!=null) 
 							&& canFollowPathInDirection(path,from,dest)
 							&& ( (dest==start)					// we can continue through the staring locaiton, which is considered empty
 										|| (dest==empty) 
 										|| (dest.topChip()==null)
 										))		// or if it is really empty
 						{
 						path.push(makeStep(from,dest)); 
 						if(steps!=null) { steps.push(dest); }
 						boolean exit = getMovesFrom(all,path,bounces,steps,start,stop,dest,top,distanceToMove-1,who,empty,savePath,winningMoveOptimization);
 						if(steps!=null) { steps.pop(); }
 						path.pop();
 						if(exit) { return(true); }
 						}
 				}
 		}
 	return(false);
 }
 public int playerHomeRow(int who)
 {
	 CommonMoveStack moves =  new CommonMoveStack();
	 getStandardMoves(moves,who,false,false);
	 GygesMovespec m = (GygesMovespec)moves.elementAt(0);
	 return(m.from_row);
 }
 public Hashtable<GygesCell,GygesCell> getSources()
 {	Hashtable<GygesCell,GygesCell> h= new Hashtable<GygesCell,GygesCell>();
 
 	switch(board_state)
 	{
 	default: 
 		break;
 	case PlayTop:
 	case PlayBottom:
 		if(pickedObject==null)
 		{ CommonMoveStack moves =  new CommonMoveStack();
 		  getStandardMoves(moves,whoseTurn,false,false);
 		  for(int lim = moves.size()-1; lim>=0; lim--)
 		  {	GygesMovespec m = (GygesMovespec)moves.elementAt(lim);
 		    if(m.op==MOVE_BOARD_BOARD)
 		    {	GygesCell c = getCell(m.from_col,m.from_row);
 		    	h.put(c,c);
 		    }
 		  }
 		}
 		break;
 	}
 	return(h);
 }
 
 //
 // get the legal dests when the source is already determined and a move
 // is in progress.  This works by getting all dests from that source, and
 // filtering out those that do not match the bounces so far
 //
 public Hashtable<GygesCell,GygesCell> getDests()
 {		Hashtable<GygesCell,GygesCell> dests = new Hashtable<GygesCell,GygesCell>();
	 	switch(board_state)
	 	{
	 	default: 
	 		break;
	 	case Continue:
	 	case PlayTop:
	 	case PlayBottom:
	 		if(pickedSourceStack.size()>0)
	 		{
	 		GygesCell from = pickedSourceStack.elementAt(0);
	 		GygesChip top = pickedObject;
	 		
	 		if((from!=null)&& (top!=null))
	 		{ CommonMoveStack all = new CommonMoveStack();
	 		  CellStack bounces = new CellStack();
	 		  IStack path = new IStack();		// storage for the search
	 		  getMovesFrom(all,path,bounces,null,from,null,from,top,top.getMovement(),whoseTurn,null,true,false);
	 		  for(int lim=all.size(),idx=0;  idx<lim; idx++)
	 		  {
	 			  GygesMovespec m = (GygesMovespec)all.elementAt(idx);
	 			  if(bouncePathMatches(pickedSourceStack,m.bounces))
	 			  {
	 		 		  int pathLength = pickedSourceStack.size();
	 				  GygesCell next = m.bounces.elementAt(pathLength-1);
	 				  if(next!=null) { dests.put(next,next); }
	 			  }
	 		}}}
	 		break;
	 	}
	return(dests);
 }
 
 //
 // determine if "bounces" is compatible with "steps". Steps doesn't include the 0'th element
 // and may have more elements overall.
 //
 private boolean bouncePathMatches(CellStack bounces,CellStack steps)
 {	int pathLength = bounces.size();
	if(steps.size()+1>=pathLength)
	  {	boolean mismatch = false;
		for(int i=1;i<pathLength && !mismatch;i++) { mismatch |= bounces.elementAt(i)!=steps.elementAt(i-1); }
		return(!mismatch);
	  }
	return(false);
 }

 //
 // this variation is for the benefit of the user interface, to find paths associated
 // with the move on the board.
 //
 public CellStack getMovePath(GygesCell from,GygesCell to,GygesChip top,CellStack bounces,GygesCell empty,int whoseTurn)
 {	CommonMoveStack all = new CommonMoveStack();
 	IStack path = new IStack();
 	CellStack temp_bounces = new CellStack();
 	CellStack temp_steps = new CellStack();
 	getMovesFrom(all,path,temp_bounces,temp_steps,from,to,from,top,top.getMovement(),whoseTurn,empty,true,false);
 	if(all.size()>0)
 	{
 		// here, "all" should be a list of moves (or partial moves) from from to to.  For maximum coolness, we want the 
 		// one with the shortest and most direct path.
 		CellStack best = null;
 		for(int lim = all.size()-1; lim >=0; lim--)
 		{	GygesMovespec m = (GygesMovespec)all.elementAt(lim);
 			if((bounces==null) || (bouncePathMatches(bounces,m.bounces)))
 			{
 			CellStack cc = m.steps;
 			if(best==null || (cc.size()<best.size())) { best = cc; }
 			}
 		}
 		return(best);
 	}
 	return(null);
 }
 public CommonMoveStack  getStandardMoves(CommonMoveStack  stack,int who,boolean savePath,boolean winningMoveOptimization)
 {	// sources are from the first nonempty row
	 
	 IStack path = new IStack();
	 GygesCell row = getCell('A',PlayerHomeRow[who]);
	 CellStack bounces = new CellStack();
	 CellStack steps = new CellStack();
	 while((stack.size()==0) && (row!=null))	// repeat until we find a row that generates some moves
	 	{
		 GygesCell item = row;
		 while(item!=null) 
		 	{ GygesChip top = item.topChip();
		 	  if(top!=null) 
		 		{ 	if(path!=null) { path.clear(); }
		 			if(steps!=null) { steps.clear(); }
		 			getMovesFrom(stack,path,bounces,steps,item,null,item,top,top.getMovement(),who,null,savePath,winningMoveOptimization);
		 		}
		 	  item = item.exitTo(CELL_RIGHT);
		 	}
		 row = row.exitTo(HomeDirection[nextPlayer[who]]);
	 	}
	 return(stack);
 }
 CommonMoveStack  GetListOfMoves(boolean savePath)
 {	CommonMoveStack all = new CommonMoveStack();
 	switch(board_state)
 	{
 	case DropTop:
 	case DropBottom:
 		{
 		G.Assert(canDisplace,"in advanced rules only");
 		int home = playerHomeRow(nextPlayer[whoseTurn]);
 		for(GygesCell c = allCells; c!=null; c=c.next)
 			{
 			if((c.topChip()==null) && ((board_state==GygesState.DropTop) ? (c.row>=home) : (c.row<=home))) 
 				{ all.push(new GygesMovespec(MOVE_DROPB_R,c.col,c.row,whoseTurn));
 				}
 			}
 		}
		break;
 	case PlaceTop:
 	case PlaceBottom:
 		{	
 			for(GygesCell c : rack[whoseTurn])
 			{	if(c.topChip()!=null)
 				{
 				GygesCell dest = getCell('A',PlayerHomeRow[whoseTurn]);
 				while(dest!=null)
 					{
 					if(dest.topChip()==null)
 						{	all.push(new GygesMovespec(MOVE_RACK_BOARD,c.rackLocation(),c.row,dest.col,dest.row,whoseTurn));
 						}
 					dest = dest.exitTo(CELL_RIGHT);
 					}
 				}
 			}
 		}
 		break;
 	case PlayTop:
 	case PlayBottom:
 		getStandardMoves(all,whoseTurn,savePath,true);
 		
 		break;
 	default:
 		throw G.Error("Not implemented");
 	}
  	return(all);
 }
 
}
