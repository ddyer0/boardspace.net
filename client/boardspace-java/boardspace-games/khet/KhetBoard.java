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
package khet;


import java.awt.Color;
import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;


/**
 * KhetBoard knows all about the game of Khet
 * 
 * @author ddyer
 *
 */

class KhetBoard extends rectBoard<KhetCell> implements BoardProtocol,KhetConstants
{
	static final int DEFAULT_COLUMNS = 10;	// 8x6 board
	static final int DEFAULT_ROWS = 8;
	static final String[] KHETGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
    static final KhetId RackLocation[] = { KhetId.White_Chip_Pool,KhetId.Black_Chip_Pool};
    static final char ForbiddenColumn[] = { 'A','J'};
    static final char SecondForbiddenColumn[] = {'I','B'};
	private KhetState unresign;
	private KhetState board_state;
	public KhetState getState() {return(board_state); }
	public void setState(KhetState st) 
	{ 	unresign = (st==KhetState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public int boardColumns = DEFAULT_COLUMNS;	// size of the board
    public int boardRows = DEFAULT_ROWS;
    public void SetDrawState() { setState(KhetState.DRAW_STATE); }
    public KhetCell rack[][] = null;
    public CellStack animationStack = new CellStack();
    private StateStack robotState = new StateStack();
    private IStack robotUndo = new IStack();
    public int robotDepth = 0;
    
    public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
    {  char ch = txt.charAt(0);
    	if(!Character.isDigit(ch) )
    	{ ypos +=cellsize/6; }
    	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
    }
    //
    // private variables
    //
	    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public KhetChip pickedObject = null;
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    public KhetCell rotatedCell = null;
    public int rotatedDirection = 0;
    public KhetCell lastDest = null;
    private CellStack capturedCells = new CellStack();
    private CellStack tempCapturedCells = new CellStack();
    
	// factory method
	public KhetCell newcell(char c,int r)
	{	return(new KhetCell(c,r));
	}
    public KhetBoard(String init,long key,int map[]) // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = KHETGRIDSTYLE; //coordinates left and bottom
    	setColorMap(map, 2);
        doInit(init,key); // do the initialization 
        autoReverseY();		// reverse_y based on the color map
     }


	public void sameboard(BoardProtocol f) { sameboard((KhetBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(KhetBoard from_b)
    {
    	super.sameboard(from_b);	// calls sameCell for each cell
    	
    	G.Assert(sameCells(rack,from_b.rack),"racks match");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject doesn't match");
        G.Assert(sameCells(rotatedCell,from_b.rotatedCell),"rotatedCell matches");
        G.Assert(rotatedDirection==from_b.rotatedDirection,"rotatedDirection matches");
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
		v ^= Digest(r,rotatedCell);
		v ^= Digest(r,rotatedDirection);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,capturedCells);
		v ^= Digest(r,rack);
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
        return (v);
    }
   public KhetBoard cloneBoard() 
	{ KhetBoard copy = new KhetBoard(gametype,randomKey,getColorMap());
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((KhetBoard)b); }


    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(KhetBoard from_b)
    {	super.copyFrom(from_b);
    	robotDepth = from_b.robotDepth;
        lastDest = getCell(from_b.lastDest);
        pickedObject = from_b.pickedObject;	
        rotatedCell = getCell(from_b.rotatedCell);
        rotatedDirection = from_b.rotatedDirection;
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(capturedCells,from_b.capturedCells);
        copyFrom(rack,from_b.rack); 
        board_state = from_b.board_state;
        unresign = from_b.unresign;

        sameboard(from_b);
    }
    public void setUpBoard(int [][]spec)
    {
    	for(int row[] : spec)
    	{
    		int pla = row[0];
    		int piece = row[1];
    		char col = (char)row[2];
    		int ro = row[3];
    		getCell(col,ro).addChip(KhetChip.getChip(pla,piece));
    	}
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {	randomKey = key;	// not used, but for reference in this demo game
    	Random r = new Random(67246765);
     	animationStack.clear();
     	int startup[][] = null;
     	boardColumns=DEFAULT_COLUMNS; 
 		boardRows = DEFAULT_ROWS;
 		
	    initBoard(boardColumns,boardRows); //this sets up the board and cross links

	    if(Khet_Classic_Init.equalsIgnoreCase(gtype)) 
 		{ startup = KhetChip.KHET_CLASSIC_START;
 		}
     	else if(Khet_Ihmotep_Init.equalsIgnoreCase(gtype))
     	{	startup = KhetChip.KHET_IHMOTEP_START;
     	}

     	else { throw G.Error(WrongInitError,gtype); }
     	gametype = gtype;
	    
	    setState(KhetState.PUZZLE_STATE);
    
	    setUpBoard(startup);
	    
	    int npieces = startup.length/2;
       	rack = new KhetCell[2][npieces];
       	for(int pl=FIRST_PLAYER_INDEX;pl<=SECOND_PLAYER_INDEX;pl++)
     	{
    	for(int i=0;i<npieces; i++)
    		{
    		rack[pl][i] = new KhetCell(r,RackLocation[pl],i);
    		//cell.addChip(KhetChip.getChip(pl,i));
    		}    
    	}
       	robotState.clear();
       	robotUndo.clear();
       	robotDepth = 0;
	    whoseTurn = FIRST_PLAYER_INDEX;
		pickedSourceStack.clear();
		droppedDestStack.clear();
		capturedCells.clear();
		tempCapturedCells.clear();
		pickedObject = null;
		rotatedCell = null;
		rotatedDirection = 0;
		lastDest = null;
		rotatedDirection = 0;
        allCells.setDigestChain(r);
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
        case DRAW_STATE:
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
        {case RESIGN_STATE:
         case CONFIRM_STATE:
         case DRAW_STATE:
            return (true);

        default:
            return (false);
        }
    }


    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(KhetState.GAMEOVER_STATE);
    }
    
    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	return(win[player]); 
    }
    private int playerIndex(KhetChip ch) { return(getColorMap()[ch.colorIndex()]) ;}
    // estimate the value of the board position.
    double mirror_facing_piece = 0.2;
    public double ScoreForPlayer(int player,boolean print)
    {  	double finalv=0.0;
    	//print = true;
    	String msg = "";
    	int nextP = nextPlayer[player];
    	KhetCell otherRack[] = rack[nextP];
    	for( KhetCell other : otherRack)
    	{
    		KhetChip top = other.topChip();
    		if(top==null) { break; }
    		//if(print) { msg += "+ "+top+top.woodValue(); }
    		finalv += top.woodValue();
    	}
    	for(KhetCell c = allCells ; c!=null; c=c.next)
    	{	
    		KhetChip top = c.topChip();
    		if(top!=null)
    		{
    			// count the mirrors pointing at us
    			for(int direction = CELL_LEFT; direction<CELL_FULL_TURN+CELL_LEFT; direction+=CELL_QUARTER_TURN)
    			{	KhetChip oppTop = c.pieceInDirection(direction);
    				if(oppTop!=null)
    				{
    				int myface = top.bounceDirection(direction+CELL_HALF_TURN);
    				if(oppTop==KhetChip.Blast)
    				{	// facing the laser. 
    					//if(myface<0) { finalv += mirror_facing_piece; }
    				}
    				else
    				{
    				int otherface = oppTop.bounceDirection(direction);
    				if(myface>=0 && (otherface==KhetChip.Smoke))
    				{	// something that can be blasted. 
    					if(playerIndex(oppTop)!=player) 
    						{ double val = mirror_facing_piece*oppTop.woodValue();
    						  finalv += val; 
    						  if(print) { msg += " +"+oppTop+"="+val; }
    						}
    				}
  
    				}}
    			}
    		}
    	}
    	if(print) { System.out.println("P"+player+"= "+msg); }
    	return(finalv);
    }


    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement()
    {	lastDest = getDest();
        pickedObject = null;
        rotatedCell = null;
        rotatedDirection = 0;
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
    	KhetCell dr = droppedDestStack.pop();
    	switch(dr.rackLocation())
	    	{
	   		default: throw G.Error("Not expecting rackLocation %s",dr.rackLocation);
			case BoardLocation: 
			case White_Chip_Pool:	// treat the pools as infinite sources and sinks
			case Black_Chip_Pool:	
				pickedObject = dr.removeTop(); 
				KhetCell src = getSource();
				if((src!=null) && (src.topChip()!=null)) { dr.addChip(src.removeTop()); }
				break;
	    	
	    	}
	    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	KhetChip po = pickedObject;
    	if(po!=null)
    	{
    		KhetCell ps = pickedSourceStack.pop();
    		switch(ps.rackLocation())
    		{
    		default: throw G.Error("Not expecting rackLocation %s",ps.rackLocation);
    		case White_Chip_Pool:
    		case Black_Chip_Pool:	
    		case BoardLocation: ps.addChip(po);
    			break;	// don't add back to the pool
    		}
    		pickedObject = null;
     	}
     }

    // 
    // drop the floating object.
    //
    private void dropObject(KhetCell c)
    {   G.Assert(pickedObject!=null,"pickedObject should not be null"); 	    		
    	switch(c.rackLocation())
		{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case White_Chip_Pool:
		case Black_Chip_Pool:
		case BoardLocation: 
			c.addChip(pickedObject);
			break;	// don't add back to the pool
		}
       	droppedDestStack.push(c);
       	pickedObject = null;
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(KhetCell cell)
    {	return(getDest()==cell);
    }
    public KhetCell getDest()
    {
    	return( (droppedDestStack.size()>0) ? droppedDestStack.top() : null);
    }
    public KhetCell getSource()
    {
    	if(pickedSourceStack.size()>0) { return(pickedSourceStack.top()); }
    	return(null);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	KhetChip ch = pickedObject;
    	if(ch!=null)
    		{ return(ch.chipNumber());
    		}
        return (NothingMoving);
    }
    
    public KhetCell getCell(KhetId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	return(getCell(col,row));
        case White_Chip_Pool:
       		return(rack[FIRST_PLAYER_INDEX][row]);
        case Black_Chip_Pool:
       		return(rack[SECOND_PLAYER_INDEX][row]);
        }
    }
    public KhetCell getCell(KhetCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(KhetCell c)
    {	G.Assert(pickedObject==null,"pickedObject should be null");
    	switch(c.rackLocation())
    	{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case BoardLocation: 
		case White_Chip_Pool:
		case Black_Chip_Pool:	
			pickedObject = c.removeTop();
			break;	
    	
    	}
    	pickedSourceStack.push(c);
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
        case DRAW_STATE:
        	setNextStateAfterDone(); 
        	break;
        case PLAY_STATE:
			setState(KhetState.CONFIRM_STATE);
			break;

        case PUZZLE_STATE:
			acceptPlacement();
            break;
        }
    }
    //	
    //true if col,row is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(KhetCell c)
    {	return((pickedSourceStack.size()>0) && (pickedSourceStack.top()==c));
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
        case CONFIRM_STATE:
        case DRAW_STATE:
        	setState(KhetState.PLAY_STATE);
        	break;
        case PLAY_STATE:
			break;
        case PUZZLE_STATE:
            break;
        }
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case GAMEOVER_STATE: 
    		break;

        case DRAW_STATE:
        	setGameOver(false,false);
        	break;
    	case CONFIRM_STATE:
    	case PUZZLE_STATE:
    	case PLAY_STATE:
    		setState(KhetState.PLAY_STATE);
    		break;
    	}

    }
    private void unCaptureCell()
    {	KhetCell dest = capturedCells.pop();
    	KhetCell src = capturedCells.pop();
    	src.addChip(dest.removeTop());
    }
    private void captureCell(KhetCell c,replayMode replay)
    {	KhetChip ch = c.removeTop();
    	int capturedPlayer = playerIndex(ch);
    	KhetCell rr[] = rack[capturedPlayer];
    	capturedCells.push(c);
    	if(ch.isPharoh())
    	{
    		win[nextPlayer[capturedPlayer]] = true;
    	}
    	for(KhetCell d : rr)
    	{
    		if(d.topChip()==null)
    			{ d.addChip(ch); 
    			  capturedCells.push(d); 
    			  if(replay.animate)
    			  {
    				  animationStack.push(c);
    				  animationStack.push(d);
    			  }
    			return; 
    			}
    	}
    	throw G.Error("Ran out of capture rack");
    }
    
    private KhetCell laserProxy(KhetCell c,boolean dest,int step,int distance)
    {	KhetChip top = c.topChip();
    	boolean isEye = top!=null && top.isEye(); 
    	KhetCell p = new KhetCell(isEye?KhetId.EyeProxy:KhetId.LaserProxy,(char)('@'+step),distance);
    	// we want to not share any structure with the original cell
    	p.duplicateCurrentCenter(c);
    	if(dest) { p.addChip(KhetChip.Blast); }
    	return(p);
    }

    private int MAXSTEPS = 100;
    private void fireLaser(KhetCell origin,KhetCell from,int direction,replayMode replay,int step,int distance)
    {	if((from!=null) && (step<MAXSTEPS))
    	{
    	KhetChip top = from.topChip();
    	if((top==null)||top.isSphinx()) 
    	{ // continue in the same direction
    		KhetCell next = from.exitTo(direction);
    		if((next==null) && (replay.animate) && (step<=20))
    		{	// limit the entertainment to 20 steps, so infinite loops don't drag on
    			int dist = Math.abs(origin.col-from.col)+Math.abs(origin.row-from.row);
	   			animationStack.push(laserProxy(origin,false,step,distance-dist));
	   			animationStack.push(laserProxy(from,true,step,distance));
   			
    		}
    		fireLaser(origin,next,direction,replay,step,distance+1);	// no obstacle 
      	}
	   	else 
	   		{
	   		int newdir = top.bounceDirection(direction);
	   		if((replay.animate)&&(step<=20))
	   		{	// limit the entertainment to 20 steps, so infinite loops don't drag on
	   			int dist = Math.abs(origin.col-from.col)+Math.abs(origin.row-from.row);
   				animationStack.push(laserProxy(origin,false,step,distance-dist));
   				animationStack.push(laserProxy(from,true,step,distance));
	   		}
	   		if(top.isEye()) 
	   		{	// also pass through
	   			fireLaser(from,from.exitTo(direction),direction,replay,step+1,distance+1); 
	   		}
	   		switch(newdir)
	   		{
	   		default: throw G.Error("Unexpected bounce direction");
	   		case 1:
	   		case 3:
	   		case 5:
	   		case 7:	// geometric directions
	   			fireLaser(from,from.exitTo(newdir),newdir,replay,step+1,distance+1);
	   			break;
	   		case KhetChip.Smoke:
	   			tempCapturedCells.pushNew(from); 	// mark the chip for capture, but leave it in place
	   				// so the beamsplitter plays out correctly.
	   			break;
	   		case KhetChip.Absorb:
	   			break;					// absorb the blast
	   		}

	   		}
    	}
    }
    private void fireLaser(replayMode replay)
    {   tempCapturedCells.clear();
    	KhetCell homeCell = (getColorMap()[whoseTurn]==0) ? getCell('J',1) : getCell('A',8);
    	KhetChip top = homeCell.topChip();
    	int direction = ((top!=null) && top.isSphinx())
    						? top.laserDirection()
    						: ((whoseTurn==0) ? CELL_UP : CELL_DOWN);

    	fireLaser(homeCell,homeCell,direction,replay,0,0);
    	while(tempCapturedCells.size()>0)
    	{	// actually remove the captured cells.
    		captureCell(tempCapturedCells.pop(),replay);
    	}
    }
    
    private void doDone(replayMode replay)
    {	
        acceptPlacement();

        if (board_state==KhetState.RESIGN_STATE)
        {	setGameOver(false,true);
        }
        else
        {	
        	fireLaser(replay);
        	boolean win1 = WinForPlayerNow(whoseTurn);
        	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
        	if(win1 || win2)  { setGameOver(win1,win2); }
        	else {setNextPlayer(); setNextStateAfterDone(); }
        }
    }

   
    public boolean Execute(commonMove mm,replayMode replay)
    {	KhetMovespec m = (KhetMovespec)mm;

        //G.print("E "+m+" "+Digest());
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone(replay);

            break;
        case MOVE_ROTATE:
        	{	KhetCell dest = getCell(m.from_col,m.from_row);
        		if(rotatedCell==null)
        		{
        		rotatedCell = dest; 
        		rotatedDirection = m.to_row; 
        		setNextStateAfterDrop();
        		if(replay.animate)
        			{
        			animationStack.push(dest);
        			animationStack.push(dest);
        			}
        		}
        		else 
        		{ G.Assert(rotatedCell==dest && rotatedDirection==-m.to_row,"unrotating");
        		rotatedCell = null;
        		rotatedDirection = 0;
        		setState(KhetState.PLAY_STATE);
        		}
        		KhetChip top = dest.removeTop();
        		m.piece = top;
        		dest.addChip(top.getRotated(m.to_row));
        		
        	}
        	break;

        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case PLAY_STATE:
        			{
        			KhetCell from = getCell(KhetId.BoardLocation, m.from_col, m.from_row);
        			KhetCell to = getCell(KhetId.BoardLocation,m.to_col,m.to_row);
        			if(replay.animate)
        			{
        				animationStack.push(from);
        				animationStack.push(to);
        			}
        			G.Assert((pickedObject==null) && (rotatedCell==null),"something is moving");
        			pickObject(from);
        			m.piece = pickedObject;
        			if(to.topChip()!=null) { from.addChip(to.removeTop()); }
        			dropObject(to); 
        			if(replay.animate)
        			{
        				animationStack.push(to);
        				animationStack.push(from);
        			}

        			setNextStateAfterDrop();
        			}
        			break;
        	}
        	break;
        case MOVE_DROPB:
			{
			KhetCell c = getCell(KhetId.BoardLocation, m.to_col, m.to_row);
			m.piece = pickedObject;
        	G.Assert(pickedObject!=null,"something is moving");
			
            if(isSource(c)) 
            	{ 
            	  unPickObject(); 

            	} 
            	else
            		{
            		if(c.topChip()!=null)
            		{	// swapping a djed
            			KhetChip back = c.removeTop();
            			KhetCell src = getSource();
            			src.addChip(back);
            			dropObject(c);
            			if(replay.animate)
            			{
            				animationStack.push(c);
            				animationStack.push(src);
            			}
            		}
            		else
            		{
            			dropObject(c);
            		}
            		setNextStateAfterDrop();
            		}
			}
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	{
        	KhetCell d = getCell(m.from_col,m.from_row);
        	if(isDest(d))
        		{ KhetCell c = getSource();
        		  unDropObject();
        		  if(c.topChip()!=null)
        		  {	// unswapping a dhed
        			  d.addChip(c.removeTop());
        		  }
        		  setState(KhetState.PLAY_STATE);
        		}
        	else 
        		{ pickObject(d);
        			// if you pick up a gobblet and expose a row of 4, you lose immediately
        		  m.piece = pickedObject;
        		  switch(board_state)
        		  {	default: throw G.Error("Not expecting pickb in state %s",board_state);
        		  	case PLAY_STATE:
        		  		// if we pick a piece off the board, we might expose a win for the other player
        		  		// and otherwise, we are comitted to moving the piece
         		  		break;
        		  	case PUZZLE_STATE:
        		  		break;
        		  }
         		}
        	}
 
            break;

        case MOVE_DROP: // drop on chip pool;
        	m.piece = pickedObject;
            dropObject(getCell(m.source, m.to_col, m.to_row));
            setNextStateAfterDrop();

            break;

        case MOVE_PICK:
        	{
        	KhetCell c = getCell(m.source, m.from_col, m.from_row);
            pickObject(c);
            m.piece = pickedObject;
            setNextStateAfterPick();
        	}
            break;


        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(KhetState.PUZZLE_STATE);
            {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;

        case MOVE_RESIGN:
            setState(unresign==null?KhetState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
    		// standardize "gameover" is not true
            setState(KhetState.PUZZLE_STATE);
 
            break;
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;

        default:
        	cantExecute(m);
        }

        //G.print("X "+m+" "+Digest());

        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips(int player,KhetCell c)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case CONFIRM_STATE:
        case DRAW_STATE:
        case PLAY_STATE: 
        case RESIGN_STATE:
		case GAMEOVER_STATE:
			return(false);
        case PUZZLE_STATE:
        	return((pickedObject==null)
        			?(c.topChip()!=null)
        			: ((c.topChip()==null) && (c.rackLocation()==RackLocation[playerIndex(pickedObject)])));
        }
    }
  

 
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(KhetMovespec m)
    {	robotState.push(board_state);
        robotUndo.push(capturedCells.size()); //record the starting state. The most reliable
        robotDepth++;
        // to undo state transistions is to simple put the original state back.
        //G.print("R "+m+" for "+whoseTurn);
       
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        if (Execute(m,replayMode.Replay))
        {
            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {
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
    public void UnExecute(KhetMovespec m)
    {
        //G.print("U "+m+" for "+whoseTurn);
    	int cap = robotUndo.pop();
    	robotDepth--;
    	while(cap<capturedCells.size())
    	{
    		unCaptureCell();
    	}
        switch (m.op)
        {
        default:
   	    	cantUnExecute(m);
        	break;

        case MOVE_DONE:
            break;
        case MOVE_ROTATE:
        	{
        	KhetCell dest = getCell(m.from_col,m.from_row);
        	KhetChip top = dest.removeTop();
        	dest.addChip(top.getRotated(-m.to_row));
        	}
        	break;

        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
        		case PLAY_STATE:
        			G.Assert(pickedObject==null,"something is moving");
        			KhetCell dest = getCell(KhetId.BoardLocation, m.to_col, m.to_row);
        			KhetCell src = getCell(KhetId.BoardLocation, m.from_col,m.from_row);
        			pickObject(dest);
        			if(src.topChip()!=null) { dest.addChip(src.removeTop()); }
       			    dropObject(src); 
       			    acceptPlacement();
        			break;
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(robotState.pop());
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
 public Hashtable<KhetCell,KhetCell> getDests()
 {	Hashtable<KhetCell,KhetCell>h = new Hashtable<KhetCell,KhetCell>();
 	if(pickedObject!=null)
 	{
 		KhetCell c = getSource();
 		if(c!=null && c.onBoard) { getDests(h,c,pickedObject); }
 	}
 	return(h);
 }
 public Hashtable<KhetCell,KhetCell> getSources()
 {	Hashtable<KhetCell,KhetCell>h = new Hashtable<KhetCell,KhetCell>();
 	switch(board_state)
 	{
 	case GAMEOVER_STATE:
 	case RESIGN_STATE:
 		break;
 	case CONFIRM_STATE:
 		{
 		KhetCell dest = getDest();
 		if(dest!=null) { h.put(dest,dest); }
 		}
 		break;
 	default:
		if((pickedObject==null))
		{
			CommonMoveStack moves = GetListOfMoves();
			while(moves.size()>0)
			{
				KhetMovespec m = (KhetMovespec)moves.pop();
				if((m.op==MOVE_BOARD_BOARD)||(m.op==MOVE_ROTATE))
				{	KhetCell c = getCell(m.from_col,m.from_row);
					h.put(c,c);
				}
			}
		}
	}
	return(h);
}
 private Hashtable<KhetCell,KhetCell> getDests(Hashtable<KhetCell,KhetCell>h,KhetCell c,KhetChip top)
 {	;
 	CommonMoveStack  all = new CommonMoveStack();
 	if(getListOfMoves(all,c,top,whoseTurn))
 	{
 		while(all.size()>0)
 		{
 			KhetMovespec m = (KhetMovespec)all.pop();
 			if((m.op==MOVE_BOARD_BOARD) && (m.from_col==c.col) && (m.from_row==c.row))
 					{
 					KhetCell dest = getCell(m.to_col,m.to_row);
 					h.put(dest,dest);
 					}
 		}
 	}
 	return(h);
 }
 private boolean getListOfMoves(CommonMoveStack all,int who)
 {	boolean some = false;
	for(KhetCell c = allCells; c!=null; c=c.next)
	{
		KhetChip top = c.topChip();
		if(top!=null)
		{
			some |= getListOfMoves(all,c,top,who);
			if(some && (all==null)) { return(true); }
		}
	}
	return(some);
 }
 
 public boolean isForbiddenSpace(KhetCell c,KhetChip po)
 {	if(po==null) { return(false); }
 	int pl = playerIndex(po);
 	return((c.col==ForbiddenColumn[pl])
 			|| ((c.col==SecondForbiddenColumn[pl]) 
 					&& ((c.row==1) || (c.row==boardRows))));
	 
 }

 private boolean getListOfMoves(CommonMoveStack all,KhetCell c,KhetChip top,int who)
 {	boolean some = false;
 	if(playerIndex(top) == who)
			{
				if((rotatedCell==null) && top.canRotate())
				{
					if(all==null) { return(true); }
					KhetChip cw = top.getRotated(RotateCW);
					KhetChip ccw = top.getRotated(RotateCCW);
					if(cw.canRotateToward(c.exitTo(cw.getRotation())))
							{
							all.push(new KhetMovespec(MOVE_ROTATE,RotateCW,c.col,c.row,who));
							}
					if(cw!=ccw && ccw.canRotateToward(c.exitTo(ccw.getRotation())))
						{ all.push(new KhetMovespec(MOVE_ROTATE,RotateCCW,c.col,c.row,who));
						}
					some = true;
				}
				if(top.canMove())
				{
					for(int dir=0;dir<CELL_FULL_TURN; dir++)
					{
						KhetCell next = c.exitTo(dir);
						if((next!=null) && !isForbiddenSpace(next,top))
						{
						KhetChip nextTop = next.topChip();
						if(nextTop==null || top.canSwapWith(nextTop))
							{
							if(all==null) { return(true);}
							all.push(new KhetMovespec(MOVE_BOARD_BOARD,c.col,c.row,next.col,next.row,who));
							some = true;
							}
						}
					}
				}
			}
	return(some);
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	if(!getListOfMoves(all,whoseTurn)) { all.push(new KhetMovespec(MOVE_RESIGN,whoseTurn)); }
  	return(all);
 }
 
}
