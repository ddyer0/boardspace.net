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
package snakes;

import online.game.*;

import java.util.*;

import lib.*;
import lib.Random;




/**
 * SnakesBoard knows all about the game of Snakes, which is played
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

class SnakesBoard extends squareBoard<SnakesCell> implements BoardProtocol,SnakesConstants
{
	private SnakeState unresign;
	private SnakeState board_state;
	public SnakeState getState() {return(board_state); }
	public void setState(SnakeState st) 
	{ 	unresign = (st==SnakeState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public boolean REQUIRE_SPLITS = false;
	public int numberOfHeads = 0;
    public int boardColumns = DEFAULT_COLUMNS;	// size of the board
    public int boardRows = DEFAULT_ROWS;
    public void SetDrawState() 
    	{ setState(SnakeState.DRAW_STATE); }
    public SnakesCell rack[] = null;		// rack for unplaced snakes
    public SnakesCell location[] = null;	// the current location of each snake
    public SnakesCell air = null;			// the snake being moved is here.
    public targetType default_target = targetType.two_two;
    public targetType target = default_target;
    public SnakesCell targetCells[] = null; 
    //
    // private variables
    //
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public SnakesChip pickedObject = null;
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    
	// factory method
	public SnakesCell newcell(char c,int r)
	{	return(new SnakesCell(c,r));
	}
    public SnakesBoard(String init,long key) // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = SNAKESGRIDSTYLE; //coordinates left and bottom
        doInit(init,key); // do the initialization 
     }
    public void saveGivens()
    {
    	for(SnakesCell c = allCells; c!=null; c=c.next)
    	{
    		c.requiredRole = c.getTileRole();
    		if(c.requiredRole!=null) { G.print(""+c);}
    	}
    }
    public boolean isSolved()
    {	for(int lim = targetCells.length-1; lim>=0; lim--) { if(targetCells[lim].cover.type==CellType.blank) { return(false); }}
    	return(true);
    }
	public void sameboard(BoardProtocol f) { sameboard((SnakesBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(SnakesBoard from_b)
    {
    	super.sameboard(from_b);
    	G.Assert(numberOfHeads == from_b.numberOfHeads,"heads mismatch");
        G.Assert(target==from_b.target,"target doesn't match");
        G.Assert(sameCells(rack,from_b.rack),"rack matches");
        G.Assert(sameCells(location,from_b.location),"location matches");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject doesn't match");
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
        long v = 0;

        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        Random r = new Random(64 * 1000); // init the random number generator
        
		for(SnakesCell c = allCells; c!=null; c=c.next)
		{	v ^= c.Digest(r);
		}
		v ^= r.nextLong()*target.ordinal();
		v ^= chip.Digest(r,pickedObject);
		v ^= SnakesCell.Digest(r,pickedSourceStack);
		v ^= SnakesCell.Digest(r,droppedDestStack);
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
        return (v);
    }
   public SnakesBoard cloneBoard() 
	{ SnakesBoard copy = new SnakesBoard(gametype,randomKey);
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((SnakesBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(SnakesBoard from_b)
    {	super.copyFrom(from_b);

        setTarget(from_b.target);
        numberOfHeads = from_b.numberOfHeads;
        pickedObject = from_b.pickedObject;	
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        copyFrom(rack,from_b.rack);
        getCell(location,from_b.location);	// get local cells
		air.copyFrom(from_b.air);
        board_state = from_b.board_state;
        unresign = from_b.unresign;

        sameboard(from_b);
    }
    public void setTarget(targetType t)
    {	default_target = target = t;
    	int w = t.width;
    	int h = t.height;
    	targetCells = new SnakesCell[w*h];
    	int ix = boardColumns/2 - w/2;
    	int iy = boardRows/2 - h/2;
    	int idx = 0;
    	for(SnakesCell c = allCells; c!=null; c=c.next)
    	{	c.onTarget = false;
    	}
    	for(int x0=0;x0<w; x0++)
    	{	// this odd calculation stores the target cells in order of distance from the edges, which was intended
    		// to speed the search up by dividing the board in the center as the first step.  It did exacty that, but
    		// resulted in a dramatic slowdown.  Apparently the extra freedom of a stripe down the center made the 
    		// search tree a lot wider, and therefore slower.
    		//int x = ((x0&1)!=0) ? (w-x0/2-1) : x0/2;
    		// 
    		// sort the cells in order of distance from the center, so solition
    		// will start at the corners and move in
    		//
    		int x = x0; // w/2 + (((x0&1)==0) ? (-x0/2-1) : (x0/2));
    		// solve in raster order from right to left
    		// int x = x0;
    	    for(int y0=0; y0<h; y0++)
    		{	int y = y0; //h/2 + ( ((y0&1)!=0) ? (-y0/2-1) : (y0/2)); 
    			SnakesCell c = getCell((char)('A'+x+ix),y+iy+1);
    				{c.onTarget = true;
    				targetCells[idx++] = c;
    				}
    		}
    		}
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {	randomKey = key;	// not used, but for reference in this demo game
    	rack = new SnakesCell[SnakesChip.N_STANDARD_CHIPS];
    	location = new SnakesCell[SnakesChip.N_STANDARD_CHIPS];
    	Random r = new Random(67246765);
    	air = new SnakesCell(r,SnakeId.InTheAir);
    	// set up the rack
     	for(int i=0;
     		i<SnakesChip.N_STANDARD_CHIPS;
     		i++)
    	{
       	SnakesCell cell = new SnakesCell(r,SnakeId.Snake_Pool);
       	cell.row=i;
       	cell.addChip(SnakesChip.getChip(i));
       	location[i] = cell;
    	rack[i]=cell;
     	}    
    	int oldcols = boardColumns;
     	int oldrows = boardRows;
    	{
      	if(Snakes_INIT.equalsIgnoreCase(gtype)) 
     		{ boardColumns=DEFAULT_COLUMNS; 
     		boardRows = DEFAULT_ROWS;
     		}
     	else { throw G.Error(WrongInitError,gtype); }
     	gametype = gtype;
     	}
	    setState(SnakeState.PUZZLE_STATE);
	    if((oldrows!=boardRows) || (oldcols!=boardColumns) || (allCells==null) )
	    		{ 
	    		initBoard(boardColumns,boardRows); //this sets up the board and cross links
	    		}
	    		else {
	    			for(SnakesCell c = allCells; c!=null; c=c.next) { c.reInit(); }
	    		}
	    whoseTurn = FIRST_PLAYER_INDEX;
		pickedSourceStack.clear();
		droppedDestStack.clear();
		pickedObject = null;
        allCells.setDigestChain(r);
        moveNumber = 1;
        setTarget(default_target);

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
    {	
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(SnakeState.GAMEOVER_STATE);
    }
    public boolean unsolvable()
    {	for(int lim = targetCells.length-1; lim>=0; lim--)
    	{	SnakesCell c = targetCells[lim];
    		if(c.cover.type==CellType.blank)
    		{	boolean isolated = true;
    			for(int dir=0;dir<4 && isolated;dir++)
    			{	SnakesCell ex = c.exitTo(dir);
    				boolean isfilled = ((ex==null) || !ex.onTarget || (ex.cover.type!=CellType.blank));
    				if(!isfilled) { isolated=false; }
    			}
    			if(isolated) { return(true); }
    		}
    	}
    	return(false);
    }
    boolean isSplit()
    {	int target = boardColumns/2;
    	char targetCol = (char)('A'+target-1);
    	SnakesCell split = getCell(targetCol,1);
    	while(split!=null)
    	{
    		if(split.onTarget)
    		{
    			SnakesCell right = split.exitTo(CELL_RIGHT);
    			if((split.topChip()==right.topChip())
    					
    					|| (split.cover.exits[CELL_RIGHT])
    					//|| (right.cover.exits[CELL_LEFT])
     				)
    			{ return(false);
    			}
    		}
		split= split.exitTo(CELL_UP);
    	}
    	return(true);
    }
    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	int heads = 0;
    	if(board_state==SnakeState.GAMEOVER_STATE) 
    		{ return(win[player]); }
    	for(int lim = targetCells.length-1; lim>=0; lim--)
    	{	SnakesCell c = targetCells[lim];
    		if(c.cover.type==CellType.blank) { return(false); }
    		if(c.cover.type==CellType.head) { heads++; }
    	}
    	boolean win = ((numberOfHeads==0) || (heads==numberOfHeads));
    	//if(win)
    	//{ G.print("a win");
    	//}
    	if(win && REQUIRE_SPLITS) { win = isSplit(); }
    	return(win);
    }
    // estimate the value of the board position.
    public double ScoreForPlayer(int player,boolean print,double cup_weight,double ml_weight,boolean dumbot)
    {  	double finalv=0.0;
	   	if(player==0)
	   		{for(int lim = targetCells.length-1; lim>=0; lim--)
			{	SnakesCell c = targetCells[lim];
				if(c.cover.type!=CellType.blank) { finalv++; }
			}}
    	return(finalv);
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
    	SnakesCell dr = droppedDestStack.pop();
    	switch(dr.rackLocation())
	    	{
	   		default: throw G.Error("Not expecting rackLocation %s",dr.rackLocation);
			case Snake_Pool:	
			case BoardLocation: 
				pickedObject = dr.removeTop(); 
				location[pickedObject.chipNumber()]=air;
				air.addChip(pickedObject);
				break;	// don't add back to the pool
	    	
	    	}
	    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	SnakesChip po = pickedObject;
    	if(po!=null)
    	{
    		SnakesCell ps = pickedSourceStack.pop();
    		switch(ps.rackLocation())
    		{
    		default: throw G.Error("Not expecting rackLocation %s",ps.rackLocation);
    		case Snake_Pool:
    		case BoardLocation:
    				location[po.chipNumber()] = ps;
    				ps.addChip(po,air.rotation); air.removeTop(); break;
    		}
    		pickedObject = null;
     	}
     }

    // 
    // drop the floating object.
    //
    private void dropObject(SnakesCell c)
    {   G.Assert(pickedObject!=null,"pickedObject should not be null"); 	    		
    	switch(c.rackLocation())
		{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case Snake_Pool:
			air.removeTop();
			location[pickedObject.chipNumber()] = c;
			c.addChip(pickedObject,0);
			pickedObject = null;
			break;
		case BoardLocation: 
			air.removeTop();
			location[pickedObject.chipNumber()] = c;
			c.addChip(pickedObject,air.rotation);
			pickedObject = null;
			break;
		}
       	droppedDestStack.push(c);
    }

    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(SnakesCell cell)
    {	return((droppedDestStack.size()>0) && (droppedDestStack.top()==cell));
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	SnakesChip ch = pickedObject;
    	if(ch!=null)
    		{ return(ch.chipNumber()+air.rotation*100);
    		}
        return (NothingMoving);
    }
  
    public SnakesCell getCell(SnakeId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
         case BoardLocation:
        	return(getCell(col,row));
        case InTheAir: return(air);
        case Snake_Pool:
       		return(rack[row]);
        }
    }
    public SnakesCell getCell(SnakesCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(SnakesCell c)
    {	G.Assert(pickedObject==null,"pickedObject should be null");
    	switch(c.rackLocation())
    	{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case Snake_Pool:	
		case BoardLocation: 
			SnakesChip obj = pickedObject = c.topChip();
			int num = obj.chipNumber();
			SnakesCell loc = location[num];
			loc.removeTop();
			air.addChip(obj,loc.rotation);
			location[num]=air;
			pickedSourceStack.push(loc);
			break;
   	
    	}
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
			setState(SnakeState.CONFIRM_STATE);
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
    public boolean isSource(SnakesCell c)
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
        	setState(SnakeState.PLAY_STATE);
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
    		setState(SnakeState.PLAY_STATE);
    		break;
    	}

    }
   

    
    private void doDone()
    {	
        acceptPlacement();

        if (board_state==SnakeState.RESIGN_STATE)
        {	setGameOver(false,true);
        }
        else
        {	boolean win1 = WinForPlayerNow(whoseTurn);
        	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
        	if(win1 || win2)  { setGameOver(win1,win2); }
        	else if(unsolvable()) { setGameOver(false,false); }
        	else {setNextPlayer(); setNextStateAfterDone(); }
        }
    }
 
    public boolean Execute(commonMove mm,replayMode replay)
    {	SnakesMovespec m = (SnakesMovespec)mm;
 
        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone();

            break;
        case MOVE_RACK_BOARD:
        	{
        	SnakesCell src = getCell(m.source, m.from_col, m.from_row);
            SnakesCell dest = getCell(SnakeId.BoardLocation,m.to_col,m.to_row);
            pickObject(src);
            air.rotation = m.to_rotation;
            dropObject(dest); 
            if(dest.isMutant())
            { setGameOver(false,false);
            }
            else
            {
            setNextStateAfterDrop();
            }
        	}
        	break;

        case MOVE_DROPB:
			{
			SnakesCell c = getCell(SnakeId.BoardLocation, m.to_col, m.to_row);
        	G.Assert(pickedObject!=null,"something is moving");
			
            if(isSource(c)) 
            	{ 
            	  unPickObject(); 

            	} 
            	else
            		{
            		dropObject(c);
            		setNextStateAfterDrop();
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
        		{ pickObject(getCell(SnakeId.BoardLocation, m.from_col, m.from_row));
        			// if you pick up a gobblet and expose a row of 4, you lose immediately
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
 
            break;

        case MOVE_DROP: // drop on chip pool;
            dropObject(getCell(SnakeId.Snake_Pool, m.to_col, m.to_row));
            setNextStateAfterDrop();

            break;

        case MOVE_ROTATE:
        	{
        	SnakesCell de = getCell(SnakeId.BoardLocation, m.to_col, m.to_row);
        	int rot = de.rotation;
        	pickObject(de);
        	SnakesChip ch = pickedObject;
        	for(int i=0;i<4;i++)
        	{
        		rot = (rot+1)%4;
        		if(de.canPlaceChipLegally(ch,rot,false,false)) { break; }
        	}
        	air.rotation = rot;
        	dropObject(de);
        	}
        	break;
        case MOVE_PICK:
        	{
        	SnakesCell c = getCell(m.source, m.from_col, m.from_row);
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
            setState(SnakeState.PUZZLE_STATE);
            {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;

        case MOVE_RESIGN:
            setState(unresign==null?SnakeState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            // standardize "gameover" is not true
            setState(SnakeState.PUZZLE_STATE);
 
            break;
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(SnakeState.GAMEOVER_STATE);
			break;

        default:
        	cantExecute(m);
        }

 
        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips(SnakesCell c)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
         case CONFIRM_STATE:
         case DRAW_STATE:
         case PLAY_STATE: 
 		case GAMEOVER_STATE:
			return(false);
        case PUZZLE_STATE:
        	return((pickedObject==null)
        			?(c.topChip()!=null)
        			:(c.topChip()==null));
        }
    }
  
    // true if it's legal to drop gobblet  originating from fromCell on toCell
    public boolean LegalToDropOnBoard(SnakesCell fromCell,SnakesChip gobblet,SnakesCell toCell)
    {	
		return(false);

    }

    public boolean LegalToHitBoard(SnakesCell cell)
    {	
        switch (board_state)
        {
 		case PLAY_STATE:
			return(LegalToDropOnBoard(pickedSourceStack.top(),pickedObject,cell));

		case GAMEOVER_STATE:
			return(false);
		case CONFIRM_STATE:
		case DRAW_STATE:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case PUZZLE_STATE:
        	return(pickedObject==null?(cell.height()>0):true);
        }
    }
  public boolean canDropOn(SnakesCell cell)
  {		SnakesCell top = (pickedObject!=null) ? pickedSourceStack.top() : null;
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
    public void RobotExecute(SnakesMovespec m)
    {
        m.state = board_state; //record the starting state. The most reliable
        m.undoInfo = moveNumber;
        // to undo state transistions is to simple put the original state back.
        
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        //G.print("E "+m);
        if (Execute(m,replayMode.Replay))
        {
            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {
                doDone();
            }
         }
    }

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(SnakesMovespec m)
    {
        //G.print("U "+m+" for "+whoseTurn);

        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;

        case MOVE_DONE:
            break;
        case MOVE_RACK_BOARD:
           	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
        		case PLAY_STATE:
        			{
        			SnakesCell dest = getCell(m.source,m.from_col, m.from_row);
        			SnakesCell src = getCell(SnakeId.BoardLocation,m.to_col,m.to_row);
        			pickObject(src);
        			dropObject(dest);
       			    acceptPlacement();
        			}
                    break;
        	}
        	break;
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
        		case PLAY_STATE:
        			G.Assert(pickedObject==null,"something is moving");
        			pickObject(getCell(SnakeId.BoardLocation, m.to_col, m.to_row));
       			    dropObject(getCell(SnakeId.BoardLocation, m.from_col,m.from_row)); 
       			    acceptPlacement();
        			break;
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(m.state);
        moveNumber = m.undoInfo;
       	setWhoseTurn(m.player);
 }

 public int moveHashKey(int chip,char col,int row,int rot)
 {
  	 return( (col-'A')+5 + ((chip+1)<<6) + ((row+3)<<12) + ((rot+6)<<18) );
 }
 
 public boolean emptyTarget()
 {	// true if the entire target area is empty
	 for(int lim=targetCells.length-1; lim>=0 ; lim--)
	 {
		 if(targetCells[lim].topChip()!=null) { return(false); }
	 }
	 return(true);
 }
 public void mirrorMoves(Hashtable<Integer,commonMove>forbidden,SnakesMovespec m)
 {	// generate hashes for 4 permutations of "m" and add them to the forbidden list.
		 if(m.op==MOVE_RACK_BOARD)
			 {
			 int oddw = target.width&1;
			 int oddh = target.height&1;
			 char col = m.to_col;
			 int row = m.to_row;
			 int rot = m.to_rotation;
			 int chip = m.from_row;
			 if(target.width==target.height)
			 {
			 char r1col = (char)('A'+row-1);
			 int r1row = boardRows-(col-'A')+oddh;
			 int key1 = moveHashKey(chip,r1col,r1row,(rot+1)%4);
			 //G.Assert(forbidden.get(key1)==null, "not used 1 %s is same as %s",forbidden.get(key1),m);
			 forbidden.put(key1,m);
			 }
			 char r2col = (char)('A'+(boardColumns-(col-'A'+1))+oddw);
			 int r2row = boardRows-row+1+oddh;
			 int key2 = moveHashKey(chip,r2col,r2row,(rot+2)%4);
			 //G.Assert(forbidden.get(key2)==null, "not used 2");
			 forbidden.put(key2,m);
			 if(target.width==target.height)
			 {
			 char r3col = (char)('A'+(boardColumns-row)+oddw);
			 int r3row =  (col-'A')+1;
			 int key3 = moveHashKey(chip, r3col,r3row, (rot+3)%4);
			 //G.Assert(forbidden.get(key3)==null, "not used 3");
			 forbidden.put(key3, m);
			 }
			 }
		 
 }
    
 // version 2 of the move generator.  This version picks the first empty cell in the
 // target area and adds all possible moves that would fill that cell.  If none are
 // found then the puzzle is unsolvable and we will backtrack.
 public boolean trialPlacement(
		 CommonMoveStack all,
		 Hashtable<Integer,commonMove>forbidden,
		 SnakesCell c,
		 SnakesChip ch,
		 int rot,
		 boolean hasGivens)
 {	int chipNumber = ch.chipNumber();
 	char col = c.col;
 	int row = c.row;
 	if(forbidden!=null)
 	{//
 	 // as each top level move is investigated, add all four permutations
 	 // of that move to the forbidden list.  This will prevent the solver 
 	 // from finding the other three solutions which have the top level 
 	 // move in a rotated position.  This works to keep the set of solutions
 	 // small, but dissapointingly only results in a factor or 2 speedup.
 	 //
	 int key = moveHashKey(chipNumber,col,row,rot);
	 if(forbidden.get(key)!=null) { return(false); }
 	}
 	c.declined = null;
	if(c.canPlaceChipLegally(ch,rot,true,!hasGivens))
	{	SnakesMovespec spec = new SnakesMovespec(MOVE_RACK_BOARD,location[ch.chipNumber()].row,c.col,c.row,rot,whoseTurn);
		spec.declined = c.declined;
		all.addElement(spec);
		return(true);
	}
 	return(false);
 }

 void getSomeMoves(CommonMoveStack all,Hashtable<Integer,commonMove>forbidden,boolean hasGivens)
 {	boolean added = false;
 	boolean pessimize = false;
 	for(int lim=targetCells.length-1; lim>=0 && !added; lim--)
 	{
	 SnakesCell c = targetCells[lim];
	 CellType ctype = c.cover.type;
	 if(ctype==CellType.blank)
	 { 	// an empty cell in the target area
		for(int lastloc = rack.length-1; lastloc>=0; lastloc--)
		{	SnakesChip ch = rack[lastloc].topChip();	
			if(ch!=null)
			{	/// a chip not yet on the board might fit at any rotation from the target cell
				Coverage exits[] = ch.cover;
				for(int rotation = 0; rotation<4 ; rotation++)
				{	
					added |= trialPlacement(all,forbidden,c,ch,rotation,hasGivens);
				}
				// now reaching the target cell from other positions
				{SnakesCell down = c.exitTo(CELL_DOWN);
				if(down!=null)
				{// from the down cell, rotations 0 and 3 might cover the target cell
				 if(pessimize || (exits[1].type!=CellType.blank)) 
				 	{ added |= trialPlacement(all,forbidden,down,ch,0,hasGivens); 
				 	}
				 { if(pessimize || (exits[3].type!=CellType.blank)) 
				 	{ added |= trialPlacement(all,forbidden,down,ch,3,hasGivens); }
				 	} 
				 SnakesCell downleft = down.exitTo(CELL_LEFT);
				 if(downleft!=null)
				 { // from the downleft cell, only the 0 rotation might cover the target cell
					 if(pessimize || (exits[2].type!=CellType.blank)) 
					 	{ added |= trialPlacement(all,forbidden,downleft,ch,0,hasGivens); 
					 	}
				 }
				 
				 {
				 SnakesCell downright = down.exitTo(CELL_RIGHT);
				 if(downright!=null)
				 { // from the downleft cell, only the 0 rotation might cover the target cell
					 if(pessimize || (exits[3].type!=CellType.blank)) 
					 	{ added |= trialPlacement(all,forbidden,downright,ch,3,hasGivens); 
					 	}
				 }}

				}}
				
				{SnakesCell left = c.exitTo(CELL_LEFT);
				if(left!=null)
				{
				// from the left cell, rotations 0 and 1 might cover the target cell
				if(pessimize || (exits[3].type!=CellType.blank)) 
					{ added |= trialPlacement(all,forbidden,left,ch,0,hasGivens); 
					} 
				if(pessimize || (exits[1].type!=CellType.blank)) 
					{ added |= trialPlacement(all,forbidden,left,ch,1,hasGivens); 
					}
				SnakesCell leftup = left.exitTo(CELL_UP);
				if(pessimize || (exits[2].type!=CellType.blank)) 
					{ added |= trialPlacement(all,forbidden,leftup,ch,1,hasGivens); 
					}			
				}}
				
				{
					SnakesCell right = c.exitTo(CELL_RIGHT);
					if(right!=null)
					{
					// from the left cell, rotations 0 and 1 might cover the target cell
					if(pessimize || (exits[3].type!=CellType.blank)) 
						{ added |= trialPlacement(all,forbidden,right,ch,2,hasGivens); 
						}
					if(pessimize || (exits[1].type!=CellType.blank)) 
						{ added |= trialPlacement(all,forbidden,right,ch,3,hasGivens); 
						}
					SnakesCell rightup = right.exitTo(CELL_UP);
					if(pessimize || (exits[2].type!=CellType.blank)) 
						{ added |= trialPlacement(all,forbidden,rightup,ch,2,hasGivens); 
						}				
					}}	
					
				
			{
			SnakesCell up = c.exitTo(CELL_UP);
				if(up!=null)
				{
					added |= trialPlacement(all,forbidden,up,ch,1,hasGivens);
					if(exits[3].type!=CellType.blank) { 
						{ added |= trialPlacement(all,forbidden,up,ch,1,hasGivens); } 
						}
					if(exits[1].type!=CellType.blank) 
						{  { added |= trialPlacement(all,forbidden,up,ch,2,hasGivens); } 
						}
				}
			}
				
			}
	 	}
		if(!added) 
		{ // if no legal moves were found to cover the target cell, the puzzle is blocked.
			all.addElement(new SnakesMovespec(MOVE_RESIGN,whoseTurn)); 
		}
		return;	// if none added, we have reached an impasse
 	}
 	}
 }
 public boolean hasGivens()
 {	 	boolean hasGivens = false;
	 	for(SnakesCell c  = allCells; !hasGivens && (c!=null); c=c.next) { hasGivens |= c.requiredRole!=null; }
	 	return(hasGivens);
 }
 CommonMoveStack  GetListOfMoves(Hashtable<Integer,commonMove>forbidden)
 {	CommonMoveStack  all = new CommonMoveStack();
 	getSomeMoves(all,forbidden,hasGivens());
  	return(all);
 }
 
}
