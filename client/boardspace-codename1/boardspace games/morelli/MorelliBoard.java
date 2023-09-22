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
package morelli;

import online.game.*;

import java.util.*;

import lib.*;
import lib.Random;
import static morelli.MorelliMovespec.*;
/**
 * MorelliBoard knows all about the game of Morelli.
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

public class MorelliBoard extends rectBoard<MorelliCell> implements BoardProtocol,MorelliConstants
{	static int REVISION = 102;
	// revision 101 adds setup number 4, the alternating blocks setup, selection of setup at start
	// revision 102 adds the alternating-opposite setup and removes all the others
	public int getMaxRevisionLevel() { return(REVISION); }
	public Variations variation = Variations.morelli_13;
	public Setup setup = null;
	boolean setupSelected = false;
	boolean firstPlayerSelected = false;
	
    public int boardColumns = DEFAULT_COLUMNS;	// size of the board
    public int boardRows = DEFAULT_ROWS;
    public void SetDrawState() 
    	{ setState(MorelliState.Draw); }
    public CellStack occupiedCells = new CellStack();
    public CellStack animationStack = new CellStack();
    //
    // private variables
    //
    private boolean robotGame = false;
    public void setRobotGame() { robotGame=true; }
    private MorelliState board_state = MorelliState.Play;	// the current board state
    private MorelliState unresign = null;					// remembers the previous state when "resign"
    private MorelliCell center = null;
    public MorelliState getState() { return(board_state); } 
	public void setState(MorelliState st) 
	{ 	unresign = (st==MorelliState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	
   public int chips_on_board[] = new int[2];			// number of chips currently on the board
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public MorelliChip pickedObject = null;
    private int currentCaptures = 0;
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    public MorelliCell lastDest = null;
    public MorelliCell lastSrc = null;
    public MorelliCell prevLastDest = null;
    public MorelliCell prevLastSrc = null;
	// factory method
	public MorelliCell newcell(char c,int r)
	{	return(new MorelliCell(c,r));
	}
    public MorelliBoard(String init,long rv,int np,int map[],int rev) // default constructor
    {   setColorMap(map, np);
        doInit(init,rv,rev,setup); // do the initialization 
     }

    public void doInit(String game,long rv)
	{ 	doInit(game,rv,revision,setup);
	}
	public void sameboard(BoardProtocol f) { sameboard((MorelliBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if clone,digest and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(MorelliBoard from_b)
    {
    	super.sameboard(from_b);	// calls sameCell for each cell, also for inherited class variables.
    	G.Assert(unresign==from_b.unresign,"unresign mismatch");
    	G.Assert(currentCaptures==from_b.currentCaptures,"current captures mismatch");
    	G.Assert(setup==from_b.setup,"setup mismatch");
    	G.Assert(setupSelected==from_b.setupSelected,"setupSelected mismatch");
    	G.Assert(firstPlayerSelected==from_b.firstPlayerSelected,"firstPlayerSelected mismatch");
       	G.Assert(AR.sameArrayContents(win,from_b.win),"win array contents match");
       	G.Assert(AR.sameArrayContents(chips_on_board,from_b.chips_on_board),"chips_on_board contents match");
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

        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
	    //System.out.println("");
        Random r = new Random(64 * 1000); // init the random number generator
        long v = super.Digest(r);
        //System.out.println("D1 "+v);

		v ^= chip.Digest(r,pickedObject);
        //System.out.println("D2 "+v);
		v ^= Digest(r,pickedSourceStack);
        //System.out.println("D3 "+v);
		v ^= Digest(r,droppedDestStack);
        //System.out.println("D4 "+v);
		v ^= currentCaptures;
        //System.out.println("D5 "+v);
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
        //System.out.println("D6 "+v);
        return (v);
    }
   public MorelliBoard cloneBoard() 
	{ MorelliBoard copy = new MorelliBoard(gametype,randomKey,players_in_game,getColorMap(),revision);
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b)
   {	copyFrom((MorelliBoard)b);
   }
    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(MorelliBoard from_b)
    {	
        super.copyFrom(from_b);			// copies the standard game cells in allCells list
        pickedObject = from_b.pickedObject;	
        setup = from_b.setup;
        firstPlayerSelected = from_b.firstPlayerSelected;
        setupSelected = from_b.setupSelected;
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        AR.copy(chips_on_board,from_b.chips_on_board);
        getCell(occupiedCells, from_b.occupiedCells);
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        currentCaptures = from_b.currentCaptures;
        sameboard(from_b);
    }

    private void placeRow(int mode, int corner,int[]side)
    {	int xstep,ystep,firstx,firsty;
    	switch(mode)
    	{
    	default: throw G.Error("not expecting %s",mode);
    	case 0: xstep = 1; ystep = 0; firstx =0; firsty=1; break;
    	case 1: xstep = -1; ystep = 0; firstx = boardRows-1; firsty=boardRows; break;
    	case 2: xstep = 0;ystep=-1; firstx = 0; firsty=boardRows; break;
    	case 3: xstep = 0;ystep=1; firstx=boardRows-1;firsty = 1; break;
    	}
    	addChip(getCell((char)('A'+firstx),firsty),MorelliChip.getChip(corner));
    	for(int i=0;i<side.length;i++)
    	{	firstx += xstep;
    		firsty += ystep;
    		addChip(getCell((char)('A'+firstx),firsty),MorelliChip.getChip(side[i]));
    		
    	}
    }
    public void addChip(MorelliCell c,MorelliChip d)
    {	c.addChip(d);
    	occupiedCells.push(c);
    }
    public MorelliChip removeChip(MorelliCell c)
    {	
    	occupiedCells.remove(c, false);
    	return(c.removeTop());
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long rv,int rev,Setup set)
    {  	drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = GRIDSTYLE; //coordinates left and bottom
    	randomKey = rv;
     	Random r = new Random(randomKey);
    	players_in_game =2;
    	adjustRevision(rev);
    	firstPlayerSelected = setupSelected = ((revision<=100) || (revision>=102));	// setup preselected for early games
    	occupiedCells.clear();
     	{
     	variation = Variations.findVariation(gtype);
     	if(variation!=null)
     	{
     	switch(variation)
     	{
     	default: throw G.Error("Unknown variation named %s",gtype);
     	case morelli_13:
     	case morelli_11:
     	case morelli_9:	
     		boardRows = boardColumns = variation.size;
     		reInitBoard(boardColumns,boardRows);
     		gametype = gtype;
     		setup = set;
     		// note that this bizarre construction for firstInt is crafted
     		// so all archived games are playable.  Any tweak to the init
     		// procedures is likely to perturb this.
     		int firstInt = rev>=101 ? 0 : r.nextInt(3);	// always take the random so the sequence is stable
     		if(set==null)
     		{  firstInt = rev>=101 ?  r.nextInt( 4 ) : firstInt;
     			setup = rev>=102 ? Setup.RandomOpposite
     					: Setup.getSetup(firstInt);		// exclude free
     		}
     	}
     	}
     	else { throw G.Error("No variation named %s",gtype); }
     	}
	    setState(MorelliState.Puzzle);
	    

	    int half = (boardRows+1)/2;
	    int off = (Variations.morelli_13.size-boardRows)/2;
	    
	    // fill the board with the background tiles
	    for(MorelliCell c = allCells; c!=null; c=c.next)
	    {  int row = c.row-1;
	       int col = c.col-'A';
	       int i = Math.min((row>=half?(boardRows-row-1) : row),(col>=half ? (boardRows-col-1) : col));
	       c.addChip(MorelliChip.getTile(i+off));
	       c.ring = i;
	    }
	    switch(setup)
	    {
	    default: throw G.Error("Not expecting setup %s",setup);
	    case Adjacent:
	    	for(int i=0;i<boardRows;i++)
	    	{	addChip(getCell('A',i+1),MorelliChip.getChip(0));
	    		addChip(getCell((char)('A'+boardRows-1),i+1),MorelliChip.getChip(1));
	    		
	    		if(i>1)
    			{ addChip(getCell((char)('A'+i-1),1),MorelliChip.getChip(0)); 
    			  addChip(getCell((char)('A'+boardRows-i),boardRows),MorelliChip.getChip(1));
    			}
	    	}
	    	break;
	    case Opposing:
	    	for(int i=1;i<boardRows;i++)
	    	{	addChip(getCell((char)('A'+i-1),1),MorelliChip.getChip(0));
	    		addChip(getCell((char)('A'+boardRows-i),boardRows),MorelliChip.getChip(0));
	    		addChip(getCell('A',i+1),MorelliChip.getChip(1)); 
	    		addChip(getCell((char)('A'+boardRows-1),boardRows-i),MorelliChip.getChip(1));
	    	}
	    	break;
	    case RandomOpposite:
	    	{	for(int i=0;i<boardRows;i++)
	    		{
	    			int v = r.nextInt(2);
	    			addChip(getCell('A',i+1),MorelliChip.getChip(v));
	    			addChip(getCell((char)('A'+boardRows-1),boardRows-i),MorelliChip.getChip(v^1));
	    		}
	    		for(int i=1;i<boardRows-1;i++)
	    		{
	    			int v2 = r.nextInt(2);
	    			addChip(getCell((char)('A'+i),1),MorelliChip.getChip(v2));
	    			addChip(getCell((char)('A'+boardRows-i-1),boardRows),MorelliChip.getChip(v2^1));
	    		}
	    	}
	    	break;
	    case Random:
	    	{	int corners[] = {0,0,1,1};
	    		int side[] = new int[boardRows-2];
	    		r.shuffle(corners);
	    		for(int i=0;i<4;i++) 
	    			{ 	for(int j=0;j<side.length;j++) { side[j]=(corners[i]^1)^(j&1);}
	    				r.shuffle(side);
	    				placeRow(i,corners[i],side); 
	    			}
	    	}
	    	break;
	    case Blocks:
	    	int mod[] = null;
	    	int m13[] = { 3,7,11,12};
	    	int m11[] = { 3,6,9,10};
	    	int m9[] = { 3,5,7,8};
	    	switch(boardRows)
	    	{
	    	case 13: mod = m13; break;
	    	case 11: mod = m11; break;
	    	case 9: mod = m9; break;
	    	default: throw G.Error("Not expecing size %s",boardRows);
	    	}
	    	{	int i=0;
	    		int idx = 0;
	    		int chip = 0;
	    		while(idx<mod.length)
	    		{while(i<mod[idx])
	    		{
	    			addChip(getCell('A',i+1),MorelliChip.getChip(chip));
	    			addChip(getCell((char)('A'+i),boardRows),MorelliChip.getChip(chip^1));
	    			addChip(getCell((char)('A'+boardRows-1),boardRows-i),MorelliChip.getChip(chip));
	    			addChip(getCell((char)('A'+boardRows-i-1),1),MorelliChip.getChip(chip^1));
	    			i++;
	    		}
	    		chip ^= 1;
	    		idx++;
	    		}
	    	}
	    	break;
	    case Free: // no setup, players play in
	    	break;
	    }    
	    whoseTurn = FIRST_PLAYER_INDEX;
		pickedSourceStack.clear();
		droppedDestStack.clear();
		center = getCell((char)('A'+boardRows/2),1+boardColumns/2);
		pickedObject = null;
		lastSrc = null;
		lastDest = null;
		prevLastDest = null;
		prevLastSrc = null;
		currentCaptures = 0;
        AR.setValue(win,false);
        AR.setValue(chips_on_board,0);
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
        case Gameover:
        case Resign:
        case SecondPlay:
        case FirstPlay:
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
         case FirstPlay:
         case SecondPlay:
            return (true);

        default:
            return (false);
        }
    }


    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(MorelliState.Gameover);
    }
    
    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	if(board_state==MorelliState.Gameover) { return(win[player]); }
    	return(false);
    }
    // estimate the value of the board position.
    public double ScoreForPlayer(int player,boolean print,double cup_weight,double ml_weight,boolean dumbot)
    {  	
    	return(0);
    }


    //
    // finalize all the state changes for this move.
    //
    public void acceptPlacement()
    {	currentCaptures = 0;
        pickedObject = null;
        prevLastSrc = lastSrc;
        prevLastDest = lastDest;
        droppedDestStack.clear();
        pickedSourceStack.clear();
     }
    
    private int previousLastPlaced = 0;
    private int previousLastEmptied = 0;
    private int previousLastPlayer = 0;

    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    G.Assert(pickedObject==null, "nothing should be moving");
    if(droppedDestStack.size()>0)
    	{
    	MorelliCell dr = droppedDestStack.pop();
		undoCaptures(dr,currentCaptures);
		currentCaptures = 0;
		pickedObject = removeChip(dr); 
		lastDest = null;
		dr.lastPlaced = previousLastPlaced;    
	  	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	MorelliChip po = pickedObject;
    	if(po!=null)
    	{
    		MorelliCell ps = pickedSourceStack.pop();
    		pickedObject = null;
    		lastSrc = prevLastSrc;
    		lastDest = prevLastDest;
        	ps.lastEmptied = previousLastEmptied;
        	ps.lastEmptiedPlayer = previousLastPlayer;
   			addChip(ps,po);
     	}
     }

    // 
    // drop the floating object.
    //
    private void dropObject(MorelliCell c)
    {   G.Assert(pickedObject!=null,"pickedObject should not be null"); 	    		
		addChip(c,pickedObject);
		droppedDestStack.push(c);
        previousLastPlaced = c.lastPlaced;
        c.lastPlaced = moveNumber;
		pickedObject = null;
		lastDest = c;
    	
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(MorelliCell cell)
    {	return((droppedDestStack.size()>0) && (droppedDestStack.top()==cell));
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	MorelliChip ch = pickedObject;
    	if(ch!=null)
    		{ return(ch.chipNumber());
    		}
        return (NothingMoving);
    }
    
    public MorelliCell getCell(MorelliId source,char col,int row)
    {      	return(getCell(col,row));
    }
    public MorelliCell getCell(MorelliCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(MorelliCell c)
    {	G.Assert(pickedObject==null,"pickedObject should be null");

		pickedObject = removeChip(c); 
		lastDest = null;
		lastSrc = c;		// for the viewer
		previousLastEmptied = c.lastEmptied;
		previousLastPlayer = c.lastEmptiedPlayer;
		c.lastEmptied = moveNumber;
		c.lastEmptiedPlayer = whoseTurn;

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
        case Confirm:
        case Draw:
        	setNextStateAfterDone(); 
        	break;
        case Play:
        case SecondPlay:
			setState(MorelliState.Confirm);
			break;

        case Puzzle:
			acceptPlacement();
            break;
            
        case Gameover:
        	break;
        }
    }
    //	
    //true if col,row is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(MorelliCell c)
    {	return(getSource()==c);
    }
    public MorelliCell getSource()
    {
    	return((pickedSourceStack.size()>0) ?pickedSourceStack.top() : null);
    }

    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case Gameover: 
    		break;
    	case FirstPlay:
    		setupSelected = true;
    		setState(MorelliState.SecondPlay);
    		break;
        case Draw:
        	setGameOver(false,false);
        	break;
    	case Puzzle:
    		if(!setupSelected) { setState(MorelliState.FirstPlay); }
    		else if(!firstPlayerSelected) { setState(MorelliState.SecondPlay); }
    		else { setState(MorelliState.Play); }
    		break;
    	case SecondPlay:
    	case Play:
    	case Confirm:
    		firstPlayerSelected = true;
    		setState(MorelliState.Play);
    		break;
    	}

    }
   

    
    private void doDone(replayMode replay)
    {	acceptPlacement();
        if (board_state==MorelliState.Resign)
        {	setGameOver(false,true);
        }
        else
        {	if(!robotGame)
        	{
        		if(!hasLegalMoves(nextPlayer[whoseTurn]))
        				{
        				doGameOver();
        				}
        	}
        	boolean win1 = WinForPlayerNow(whoseTurn);
        	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
        	if(win1 || win2)  { setGameOver(win1,win2); }
        	else {setNextPlayer(); setNextStateAfterDone(); }
        }
    }

    private int playerIndex(MorelliChip ch) { return(getColorMap()[ch.colorIndex()]);}
    
    private int captureCenter(MorelliCell c)
    {	MorelliChip top = c.topChip();
    	int pl = playerIndex(top);
    	int oldState = center.isEmpty()?-1:playerIndex(center.topChip());
    	int newState = 0;
    	if(oldState!=pl)
    	{	// maybe capture center if rotational symmetry is present
    		int row = c.row-1;
    		int col = c.col-'A';
    		MorelliCell opposite = getCell((char)('A'+boardColumns-col-1),boardRows-row);
    		if(opposite.isOccupied()&&(playerIndex(opposite.topChip())==pl))
    		{	MorelliCell swapRow = getCell((char)('A'+row),boardRows-col);
    			if(swapRow.isOccupied()&&(playerIndex(swapRow.topChip())==pl))
    			{	MorelliCell swapCol = getCell((char)('A'+boardColumns-row-1),col+1);
    				if(swapCol.isOccupied()&&(playerIndex(swapCol.topChip())==pl))
    				{	newState = ((oldState<0) ? 1 : 2)<<8;
    					if(oldState>=0) { center.removeTop();  }
    					center.addChip(MorelliChip.getChip(2+top.colorIndex()));
    				}
    			}
    		}
    	}
    	// return 0 for center not captured
    	// return 1 for center captured, was previously empty
    	// return 2 for center captured, was previously not empty
    	return(newState);
    }
    private void unCaptureCenter(int code)
    {	switch(code)
    	{
    	default: throw G.Error("not expecting %s",code);
    	case 0: break;	// no action
    	case 1: 
    		center.removeTop();
    		break;
    	case 2:
    		MorelliChip ch = center.removeTop();
    		MorelliChip ne = MorelliChip.getChip(2+ch.colorIndex()^1);
    		center.addChip(ne);
    		break;
    	}
    }
    private int doCaptures(MorelliCell c)
    {	int mask = captureCenter(c);
    	MorelliChip top = c.topChip();
    	int pl = playerIndex(top);
    	for(int dir = c.geometry.n-1; dir>=0; dir--)
    	{	MorelliCell d1 = c.exitTo(dir);
    		if((d1!=null) && d1.isOccupied() && (d1!=center))
    		{	MorelliChip dtop = d1.topChip();
    			if(playerIndex(dtop)!=pl)
    			{	MorelliCell d2 = d1.exitTo(dir);
     				if((d2!=null) && d2.isOccupied()&&(d2!=center))
    				{	if(d2.topChip()==top)
    					{
    					mask |= 1<<dir;
    					d1.removeTop();
    					d1.addChip(top);
    					mask |= captureCenter(d1);
    					}
    				}
    			}
    		}
    	}
    	return(mask);
    }
    private void undoCaptures(MorelliCell c,int mask)
    {	int dir = 0;
    	unCaptureCenter(mask>>8);
    	mask = mask&0xff;
    	while(mask!=0)
    	{	int bit = (1<<dir);
    		if((mask & bit)!=0)
    		{
    		MorelliCell d = c.exitTo(dir);
    		MorelliChip top = d.removeTop();
    		d.addChip(MorelliChip.getChip(top.colorIndex()^1));
   			mask ^= bit;
    		}
    		dir++;
    	}
    }
    private void doGameOver()
    {
    	setState(MorelliState.Gameover);
    	if(!center.isEmpty())
    	{	win[playerIndex(center.topChip())]=true;
    	}
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	MorelliMovespec m = (MorelliMovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }
        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_GAMEOVER:
        	// user by the robot when no moves
        	doGameOver();
        	setNextPlayer();
        	break;
        case MOVE_DONE:

         	doDone(replay);

            break;

        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		// note, a bunch of games have moves in "gameover" state, it's unclear how this happened,
        		// but including a few permissive Gameover states allows these games to be replayed without
        		// error.  It's still the case that the UI doesn't produce these moves.
        		// see U!MO-davedoma-Dumbot-2020-09-16-2247
        		case Gameover:
        		case Play:
        			G.Assert(pickedObject==null,"something is moving");
        			MorelliCell src = getCell(MorelliId.BoardLocation, m.from_col, m.from_row);
        			MorelliCell dest = getCell(MorelliId.BoardLocation,m.to_col,m.to_row);
        			pickObject(src);
        			dropObject(dest); 
        			currentCaptures = doCaptures(dest);
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
			MorelliCell c = getCell(MorelliId.BoardLocation, m.to_col, m.to_row);
        	G.Assert(pickedObject!=null,"something is moving");
			
            if(isSource(c)) 
            	{ 
            	  unPickObject(); 

            	} 
            	else
            		{
            		dropObject(c);
            		currentCaptures = doCaptures(c);
        			if(replay==replayMode.Single)
        			{
        				animationStack.push(getSource());
        				animationStack.push(c);
        			}

            		setNextStateAfterDrop();
            		}
			}
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	MorelliCell dest = getCell(m.from_col,m.from_row);
        	if(isDest(dest))
        		{ 
        		  unDropObject(); 
        		  setState(MorelliState.Play);
        		}
        	else 
        		{ pickObject(getCell(MorelliId.BoardLocation, m.from_col, m.from_row));
        		  switch(board_state)
        		  {	default: throw G.Error("Not expecting pickb in state %s",board_state);
        		  	case Play:
        		  	case SecondPlay:
         		  	case Puzzle:
         		  	case Gameover:
        		  		break;
        		  }
         		}
 
            break;

        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(MorelliState.Puzzle);
            {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;

        case MOVE_RESIGN:
        	setState(unresign==null?MorelliState.Resign:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            // standardize "gameover" is not true
            setState(MorelliState.Puzzle);
 
            break;
        case CHOOSE_SETUP:
        	doInit(gametype,randomKey,revision,Setup.getSetup(m.from_row));
        	setState(MorelliState.FirstPlay);
        	break;
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;
        default:
        	cantExecute(m);
        }

 
        return (true);
    }


    public boolean LegalToHitBoard(MorelliCell cell)
    {	
        switch (board_state)
        {
        case SecondPlay:
 		case Play:
 			if(pickedObject==null)
 			{	if(cell.isOccupied())
 				{	MorelliChip top = cell.topChip();
 					if(playerIndex(top)==whoseTurn())
 					{ return(hasLegalMoves(cell,top,whoseTurn));
 					}
 				}
 			}
 			else
 			{	Hashtable<MorelliCell,MorelliCell>dests = getDests(getSource(),pickedObject,whoseTurn);
 				return((cell==getSource()) || (dests.get(cell)!=null));
 			}
 			return(false);
 		case Resign:
		case Gameover:
		case FirstPlay:
			return(false);
		case Confirm:
		case Draw:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case Puzzle:
        	return(pickedObject==null?cell.isOccupied():true);
        }
    }
  public boolean canDropOn(MorelliCell cell)
  {		MorelliCell top = (pickedObject!=null) ? pickedSourceStack.top() : null;
  		return((pickedObject!=null)				// something moving
  			&&(top.onBoard 			// on the main board
  					? (cell!=top)	// dropping on the board, must be to a different cell 
  					: (cell==top))	// dropping in the rack, must be to the same cell
  				);
  }
 
 StateStack robotState = new StateStack();
 IStack robotCaptures = new IStack();
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(MorelliMovespec m)
    {
        robotState.push(board_state); //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        //G.print("E ",m);
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        if (Execute(m,replayMode.Replay))
        {	robotCaptures.push(currentCaptures);
            if ((m.op == MOVE_DONE)||(m.op==MOVE_GAMEOVER))
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
    public void UnExecute(MorelliMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	//G.print("U ",m);
    	int undocap = robotCaptures.pop();
        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;
   	    case MOVE_GAMEOVER:
        case MOVE_DONE:
            break;

        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case Gameover:
        		case Play:
        			MorelliCell dest = getCell(MorelliId.BoardLocation, m.to_col, m.to_row);
        			MorelliCell src = getCell(MorelliId.BoardLocation, m.from_col,m.from_row);
        			undoCaptures(dest,undocap);
        			pickObject(dest);
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
public Hashtable<MorelliCell,MorelliCell>getDests()
{	return(getDests(pickedObject!=null?getSource():null,pickedObject,whoseTurn));
}
public Hashtable<MorelliCell,MorelliCell>getDests(MorelliCell c,MorelliChip top,int who)
{	Hashtable<MorelliCell,MorelliCell> val = new Hashtable<MorelliCell,MorelliCell>();
	CommonMoveStack all = new CommonMoveStack();
	if(c!=null)
		{getMovesFor(all,c,top,who);
		while(all.size()>0)
		{	MorelliMovespec m = (MorelliMovespec)all.pop();
			MorelliCell d = getCell(m.to_col,m.to_row);
			val.put(d, d);
		}	
		}
	return(val);
}
public boolean hasLegalMoves(int who)
{
	for(int lim = occupiedCells.size()-1; lim>=0; lim--)
	{	MorelliCell c = occupiedCells.elementAt(lim);
		if(hasLegalMoves(c,c.topChip(),who)) 
			{ return(true); }
	}
	return(false);
}
public boolean hasLegalMoves(MorelliCell c,MorelliChip top,int who)
{	return(getMovesFor(null,c,top,who));
}

 public int forwardPotential(int pl)
 {	int pot = 0;
	 for(int lim=occupiedCells.size()-1; lim>=0; lim--)
	 {	MorelliCell c = occupiedCells.elementAt(lim);
	 	MorelliChip t = c.topChip();
	 	if(playerIndex(t)==pl)
	 	{	pot += forwardPotential(c,t,pl);
	 	}
	 	}
	 return(pot);
 }
 public int forwardPotential(MorelliCell c,MorelliChip top,int pl)
 {	int max = 0;
 	CommonMoveStack all = new CommonMoveStack();
	getMovesFor(all,c,top,pl);
	while(all.size()>0)
	{
		MorelliMovespec m = (MorelliMovespec)all.pop();
		int dy = m.from_row-m.to_row;
		int dx = m.from_col-m.to_col;
		max = Math.max(max,Math.max(Math.abs(dx),Math.abs(dy)));
	}
	return(max);
 }
 public boolean getMovesFor(CommonMoveStack  all,MorelliCell c,MorelliChip top,int who)
 {	boolean some = false;
 	if(playerIndex(top)==who)
 	{
 	for(int dir=0;dir<c.geometry.n; dir++)
 	{	MorelliCell d = c.exitTo(dir);
 		while((d!=null) && (d.ring>c.ring) && d.isEmpty())
 		{	if(d!=center)
 				{ if(all==null) { return(true); }
 				  all.push(new MorelliMovespec(MOVE_BOARD_BOARD,c.col,c.row,d.col,d.row,who)); 
 				}
 			d = d.exitTo(dir);
 		}
 	}}
 	return(some);
 }
 CommonMoveStack GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	switch(board_state)
 	{
 	default: throw G.Error("Not expecting state %s", board_state);
 	case Play:
 		for(int lim=occupiedCells.size()-1; lim>=0; lim--)
 		{	MorelliCell c = occupiedCells.elementAt(lim);
 			getMovesFor(all,c,c.topChip(),whoseTurn);
 		}
 		if(all.size()==0) { all.addElement(new MorelliMovespec(MOVE_GAMEOVER,whoseTurn)); }
 		break;
 	}
  	return(all);
 }

 
}
