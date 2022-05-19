package majorities;

import java.awt.Color;
import java.util.*;
import lib.*;
import lib.Random;
import online.game.*;
import static majorities.MajoritiesMovespec.*;

/**
 * MajoritiesBoard knows all about the game of Hex, which is played
 * on a hexagonal board. It gets a lot of logistic support from 
 * common.hexBoard, which knows about the coordinate system.  
 * 
 * This class doesn't do any graphics or know about anything graphical, 
 * in the graphics.
 * 
 *  The principle interface with the game viewer is the "Execute" method
 *  which processes moves. 
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

class MajoritiesBoard extends hexBoard<MajoritiesCell> implements BoardProtocol,MajoritiesConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }

    static final String[] MAJORITIESGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

	MajoritiesVariation variation = MajoritiesVariation.majorities_3;
	private MajoritiesState board_state = MajoritiesState.Puzzle;	
	private MajoritiesState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	
	public MajoritiesState getState() { return(board_state); }
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(MajoritiesState st) 
	{ 	unresign = (st==MajoritiesState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

    private MajoritiesId playerColor[]={MajoritiesId.White_Chip_Pool,MajoritiesId.Black_Chip_Pool};   
    private MajoritiesChip playerChip[]={MajoritiesChip.White,MajoritiesChip.Black};
    private MajoritiesCell playerCell[]=new MajoritiesCell[2];
    
    // lineroots holds the root cell for each line in each direction, there are 3 directions and n line heads in each direction
    // this is built out by doInit
    public MajoritiesCell lineRoots[][] = {null,null,null	};
    //
    // these are the directions of travel from the line roots
    public static int lineDirections[] = { CELL_UP_LEFT, CELL_UP, CELL_UP_RIGHT };
    
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public MajoritiesChip getPlayerChip(int p) { return(playerChip[p]); }
	public MajoritiesId getPlayerColor(int p) { return(playerColor[p]); }
	public MajoritiesCell getPlayerCell(int p) { return(playerCell[p]); }
// this is required even though it is meaningless for Hex, but possibly important
// in other games.  When a draw by repetition is detected, this function is called.
// the game should have a "draw pending" state and enter it now, pending confirmation
// by the user clicking on done.   If this mechanism is triggered unexpectedly, it
// is probably because the move editing in "editHistory" is not removing last-move
// dithering by the user, or the "Digest()" method is not returning unique results
// other parts of this mechanism: the Viewer ought to have a "repRect" and call
// DrawRepRect to warn the user that repetitions have been seen.
	public void SetDrawState() {throw G.Error("not expected"); };	
	CellStack animationStack = new CellStack();
    private int chips_on_board = 0;			// number of chips currently on the board
    private int fullBoard = 0;				// the number of cells in the board
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public MajoritiesChip pickedObject = null;
    public MajoritiesChip lastPicked = null;
    private MajoritiesCell blackChipPool = null;	// dummy source for the chip pools
    private MajoritiesCell whiteChipPool = null;

    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    
    private CellStack emptyCells=new CellStack();
    private MajoritiesState resetState = MajoritiesState.Puzzle; 
    public MajoritiesChip lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public MajoritiesCell newcell(char c,int r)
	{	return(new MajoritiesCell(MajoritiesId.BoardLocation,c,r));
	}
	
	int majorityLine(MajoritiesCell from, int direction)
	{	int cells = 0;
		int p0 = 0;
		int p1 = 0;
		while(from!=null)
		{	if(!from.removed)
			{	MajoritiesChip ch = from.topChip();
				cells++;
				if(ch!=null) 
				{	switch(ch.id)
					{
					default: throw G.Error("Not expecting %s",ch.id);
					case White_Chip_Pool:  p0+=2; break; 
					case Black_Chip_Pool:  p1+=2; break;
					}
				}
			}
			from = from.exitTo(direction);
		}
		return(((p0>cells)? 0 : ((p1>cells)? 1 : -1)));
	}
	private int majorityDirection(int dir)	// winner of a direction is 0 1 or -1
	{	int p0 = 0;
		int p1 = 0;
		int lines = 0;
		for(MajoritiesCell c : lineRoots[dir])
		{	
			int winner = c.lineOwner[dir];
			lines++;
			if(winner<0) { winner = majorityLine(c,lineDirections[dir]); }	// not determined yet
			c.lineOwner[dir] = winner;
			switch(winner)
			{	
			case 0: p0+=2; break;
			case 1: p1+=2; break;
			case -1: break;
			default: throw G.Error("not expecting %s",winner);
			}
		}
		return((p0>lines) ? 0 : ((p1>lines)? 1 : -1)); 
	}
	private int winner()	// 0 1 or -1
	{	int p0 = 0;
		int p1 = 0;
		for(int i=0;i<3;i++)
		{	int wind = majorityDirection(i);
			switch(wind)
			{
			case 0: p0++; break;
			case 1: p1++; break;
			case -1: break;
			default: throw G.Error("not expecting %s",wind);
			}
		}
		return(p0 >= 2 ? 0 : (p1 >= 2 ? 1 : -1));
	}
	public boolean WinForPlayerNow(int p)
	{	int winner = winner();
		return(winner==getColorMap()[p]);
	}

	// constructor 
    public MajoritiesBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = MAJORITIESGRIDSTYLE;
        setColorMap(map);
 		Random r = new Random(734687);	// this random is used to assign hash values to cells, common to all games of this type.
	    blackChipPool = new MajoritiesCell(r,MajoritiesId.Black_Chip_Pool);
	    blackChipPool.chip = MajoritiesChip.Black;
	    whiteChipPool = new MajoritiesCell(r,MajoritiesId.White_Chip_Pool);
	    whiteChipPool.chip = MajoritiesChip.White;

        doInit(init,key,players,rev); // do the initialization 
    }
    
    public String gameType() { return(gametype+" "+players_in_game+" "+randomKey+" "+revision); }
    

    public void doInit(String gtype,long key)
    {
    	StringTokenizer tok = new StringTokenizer(gtype);
    	String typ = tok.nextToken();
    	int np = tok.hasMoreTokens() ? G.IntToken(tok) : players_in_game;
    	long ran = tok.hasMoreTokens() ? G.IntToken(tok) : key;
    	int rev = tok.hasMoreTokens() ? G.IntToken(tok) : revision;
    	doInit(typ,ran,np,rev);
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int players,int rev)
    {	randomKey = key;
    	adjustRevision(rev);
    	players_in_game = players;
		setState(MajoritiesState.Puzzle);
		variation = MajoritiesVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		gametype = gtype;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case majorities_7:
		case majorities_5:
		case majorities_3:
			reInitBoard(variation.firstInCol,variation.ZinCol,null);
		}
 		
		int miss[] = variation.missingCells;
		for(int i=0;i<miss.length;i+=2)
		{	char col = (char)miss[i];
			int row = miss[i+1];
			MajoritiesCell m = getCell(col,row);
			m.removed = true;
		}
	    
		int sz = ncols;
		int mid = (sz+1)/2;
		lineRoots[0] = new MajoritiesCell[sz];
		lineRoots[1] = new MajoritiesCell[sz];
		lineRoots[2] = new MajoritiesCell[sz];
		for(char ch='A',lastcol=(char)('A'+ncols-1);ch<=lastcol; ch++)
		{	int idx = ch-'A';
			MajoritiesCell cell0 = getCell(ch,1);
			MajoritiesCell cell1 = idx<mid ? getCell('A',idx+1) : getCell((char)('B'+idx-mid),1);
			MajoritiesCell cell2 = idx<mid ? getCell('A',idx+1) : getCell((char)('B'+idx-mid),idx+1);
			cell0.lineOwner = new int[3];
			cell1.lineOwner = new int[3];
			cell2.lineOwner = new int[3];
			AR.setValue(cell0.lineOwner,-1);
			AR.setValue(cell1.lineOwner,-1);
			AR.setValue(cell2.lineOwner,-1);
			lineRoots[0][idx]= cell0;		// a1 b1 ....
			lineRoots[1][idx]= cell1;		// a1-a5, b1 c1 ..
			lineRoots[2][idx]= cell2;	// a1-a5, b6 c6 .. 
		}
	    int map[]=getColorMap();
	    playerCell[map[FIRST_PLAYER_INDEX]] = whiteChipPool; 
	    playerCell[map[SECOND_PLAYER_INDEX]] = blackChipPool;
	    playerColor[map[FIRST_PLAYER_INDEX]] = MajoritiesId.White_Chip_Pool;
	    playerColor[map[SECOND_PLAYER_INDEX]] = MajoritiesId.Black_Chip_Pool;
	    playerChip[map[FIRST_PLAYER_INDEX]] = MajoritiesChip.White;
	    playerChip[map[SECOND_PLAYER_INDEX]] = MajoritiesChip.Black;
	    
	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    chips_on_board = 0;
	    droppedDestStack.clear();
	    stateStack.clear();
	    pickedSourceStack.clear();
	    pickedObject = null;
	    resetState = null;
	    lastDroppedObject = null;
	    // set the initial contents of the board to all empty cells
		emptyCells.clear();
		for(MajoritiesCell c = allCells; c!=null; c=c.next) { c.chip=null; if(!c.removed) { emptyCells.push(c); }}
		fullBoard = emptyCells.size();
		animationStack.clear();
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }


    /** create a copy of this board */
    public MajoritiesBoard cloneBoard() 
	{ MajoritiesBoard dup = new MajoritiesBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((MajoritiesBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(MajoritiesBoard from_b)
    {
        super.copyFrom(from_b);
        chips_on_board = from_b.chips_on_board;
        fullBoard = from_b.fullBoard;
        robotState.copyFrom(from_b.robotState);
        getCell(emptyCells,from_b.emptyCells);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"same");
        getCell(droppedDestStack,from_b.droppedDestStack);
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        lastPicked = null;
		blackChipPool.copyCurrentCenter(from_b.blackChipPool);
		whiteChipPool.copyCurrentCenter(from_b.whiteChipPool);
		 
        AR.copy(playerColor,from_b.playerColor);
        AR.copy(playerChip,from_b.playerChip);
 
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((MajoritiesBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(MajoritiesBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor mismatch");
        G.Assert(AR.sameArrayContents(playerChip,from_b.playerChip),"playerChip mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"pickedSourceStack mismatch");
        G.Assert(sameContents(stateStack,from_b.stateStack),"state stack mismatch");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(chips_on_board == from_b.chips_on_board,"chips_on_board mismatch");
 

        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Digest matches");

    }

    /** 
     * Digest produces a 64 bit hash of the game state.  This is used in many different
     * ways to identify "same" board states.  Some are relevant to the ordinary operation
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
        // the basic digestion technique is to xor a bunch of random numbers. 
    	// many object have an associated unique random number, including "chip" and "cell"
    	// derivatives.  If the same object is digested more than once (ie; once as a chip
    	// in play, and once as the chip currently "picked up", then it must be given a
    	// different identity for the second use.
        //
        Random r = new Random(64 * 1000); // init the random number generator
        long v = super.Digest();

		// many games will want to digest pickedSource too
		// v ^= cell.Digest(r,pickedSource);
		v ^= chip.Digest(r,playerChip[0]);	// this accounts for the "swap" button
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
        return (v);
    }



    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer()
    {
        switch (board_state)
        {
        default:
            G.Error("Move not complete, can't change the current player");
            break;
        case Puzzle:
            break;
        case Confirm:
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
    {	return(board_state.doneState());
    }
    // this is the default, so we don't need it explicitly here.
    // but games with complex "rearrange" states might want to be
    // more selecteive.  This determines if the current board digest is added
    // to the repetition detection machinery.
    public boolean DigestState()
    {	
    	return(board_state.digestState());
    }


 
    // set the contents of a cell, and maintain the books
    public MajoritiesChip SetBoard(MajoritiesCell c,MajoritiesChip ch)
    {	MajoritiesChip old = c.chip;
    	if(c.onBoard)
    	{
    	if(old!=null) { chips_on_board--;emptyCells.push(c); }
     	if(ch!=null) { chips_on_board++; emptyCells.remove(c,false); }
    	}
       	c.chip = ch;
    	return(old);
    }
    //
    // accept the current placements as permanent
    //
    public void acceptPlacement()
    {	pickedSourceStack.clear();
    	droppedDestStack.clear();
    	stateStack.clear();
    	pickedObject = null;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private MajoritiesCell unDropObject()
    {	MajoritiesCell rv = droppedDestStack.top();
    	if(rv!=null) 
    	{	pickedObject = SetBoard(rv,null);
     		droppedDestStack.pop();
     		setState(stateStack.pop());
     	}
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	if(pickedObject!=null)
    	{	MajoritiesCell c = pickedSourceStack.pop();
    		SetBoard(c,pickedObject);
    		pickedObject = null;
    	}
    }
    // 
    // drop the floating object.
    //
    private void dropObject(MajoritiesCell c)
    {
       droppedDestStack.push(c);
       stateStack.push(board_state);
       switch (c.rackLocation())
        {
        default:
            G.Error("not expecting dest %s", c.rackLocation);
            break;
        case Black_Chip_Pool:
        case White_Chip_Pool:		// back in the pool, we don't really care where
        	pickedObject = null;
            break;
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        case EmptyBoard:
           	SetBoard(c,pickedObject);
            pickedObject = null;
            break;
        }
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(MajoritiesCell c)
    {	return(droppedDestStack.top()==c);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { MajoritiesChip ch = pickedObject;
      if(ch!=null)
    	{	return(ch.chipNumber()); 
    	}
      	return (NothingMoving);
    }
   /**
     * get the cell represented by a source code, and col,row
     * @param source
     * @param col
     * @param row
     * @return
     */
    private MajoritiesCell getCell(MajoritiesId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	return(getCell(col,row));
        case Black_Chip_Pool:
        	return(blackChipPool);
        case White_Chip_Pool:
        	return(whiteChipPool);
        } 	
    }
    public MajoritiesCell getCell(MajoritiesCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private MajoritiesChip pickObject(MajoritiesCell c)
    {	pickedSourceStack.push(c);
        switch (c.rackLocation())
        {
        default:
            G.Error("Not expecting rackLocation %s", c.rackLocation);
            break;
        case BoardLocation:
        	{
         	boolean wasDest = isDest(c);
        	unDropObject(); 
        	if(!wasDest)
        	{
            lastPicked = pickedObject = c.topChip();
         	lastDroppedObject = null;
			SetBoard(c,null);
        	}}
            break;

        case Black_Chip_Pool:
        case White_Chip_Pool:
        	lastPicked = pickedObject = c.chip;
        }
        return(pickedObject);
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(MajoritiesCell c)
    {	return(c==pickedSourceStack.top());
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop()
    {
        switch (board_state)
        {
        default:
            G.Error("Not expecting drop in state %s", board_state);
            break;
        case Confirm:
        	setNextStateAfterDone();        	
        	break;
        case Play1:
        	setState(MajoritiesState.Play2);
        	break;
        case Play:
        case Play2:
			setState(MajoritiesState.Confirm);
			break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    private void setNextStateAfterDone()
    {	G.Assert(chips_on_board+emptyCells.size()==fullBoard,"cells missing");
       	switch(board_state)
    	{
    	default: G.Error("Not expecting state %s",board_state);
    		break;
    	case Puzzle:
    	case Gameover: break;

    	case Confirm:
    		setState(MajoritiesState.Play1);  		
    		break;
    	}
       	resetState = board_state;
    }
    private void doDone()
    {
        acceptPlacement();

        if (board_state==MajoritiesState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(MajoritiesState.Gameover);
        }
        else
        {	if(WinForPlayerNow(whoseTurn)) 
        		{ win[whoseTurn]=true;
        		  setState(MajoritiesState.Gameover); 
        		}
        	else {setNextPlayer();
        		setNextStateAfterDone();
        	}
        }
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	MajoritiesMovespec m = (MajoritiesMovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }

        //G.print("E "+m+" for "+whoseTurn+" "+state);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone();

            break;

        case MOVE_DROPB:
        	{
        	MajoritiesCell animsrc = null;

			{
			MajoritiesChip po = pickedObject;
			MajoritiesCell src = getCell(m.source,m.to_col,m.to_row); 
			MajoritiesCell dest =  getCell(MajoritiesId.BoardLocation,m.to_col,m.to_row);
			if(po==null) { po=pickObject(src); 
				if(replay!=replayMode.Replay)
				{
					animationStack.push(src);
					animationStack.push(dest);
				}
			}
            if(replay==replayMode.Live)
            	{lastDroppedObject = pickedObject.getAltDisplayChip(dest);
            	//G.print("Drop ",lastDroppedObject);
            	}
            dropObject(dest);
            /**
             * if the user clicked on a board space without picking anything up,
             * animate a stone moving in from the pool.  For Hex, the "picks" are
             * removed from the game record, so there are never picked stones in
             * single step replays.
             */
            if(replay==replayMode.Single || (po==null))
            	{ animationStack.push(animsrc==null?src:animsrc);
            	  animationStack.push(dest); 
            	}
			}
            setNextStateAfterDrop();
        	}
            break;

        case MOVE_PICK:
		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
			MajoritiesCell src = getCell(m.source,m.to_col,m.to_row);
			if(isDest(src))
			{
				unDropObject();
			}
			else
			{
        	pickObject(src);
			}

			break;

        case MOVE_DROP: // drop on chip pool;
        	{	
        	MajoritiesCell c = getCell(m.source,m.to_col,m.to_row);
            if(isSource(c)) { unPickObject(); }
            else  
            	{ 
            	  if(replay==replayMode.Live)
            	  {lastDroppedObject = pickedObject.getAltDisplayChip(c);
            	  //G.print("Drop "+lastDroppedObject);            	  
            	  }
            	  dropObject(c); 
            	}
        	}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            int nextp = nextPlayer[whoseTurn];
            
            if((win[whoseTurn]=WinForPlayerNow(whoseTurn))
               ||(win[nextp]=WinForPlayerNow(nextp)))
               	{ setState(MajoritiesState.Gameover); 
               	}
            else 
            {  setState( (chips_on_board==0) ? MajoritiesState.Play : MajoritiesState.Play1);
            }

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?MajoritiesState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(MajoritiesState.Puzzle);
 
            break;
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(MajoritiesState.Gameover);
			break;

        default:
            cantExecute(m);
        }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
            G.Error("Not expecting state %s", board_state);
            return(false);	// not used
        case Confirm:
        	return(false);
        case Play:
        case Play1:
        case Play2:
        	// for majorities, you can pick up a stone in the storage area
        	// but it's really optional
        	return(player==whoseTurn);
		case Resign:
		case Gameover:
			return(false);
        case Puzzle:
            return ((pickedObject!=null)?(pickedObject==playerChip[player]):true);
        }
    }

    public boolean LegalToHitBoard(MajoritiesCell c)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
		case Play2:
			return((c.removed==false) && ((c.chip==null) || ((pickedObject==null) && isDest(c))));
		case Play:
		case Play1:
			return((c.removed==false) && (c.chip==null));
		case Gameover:
		case Resign:
			return(false);
		case Confirm:
			return(isDest(c));
        default:
            G.Error("Not expecting state %s", board_state);
            return(true);	// not used
        case Puzzle:
            return ((c.removed==false) && ((pickedObject==null)!=(c.chip==null)));
        }
    }
    
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(MajoritiesMovespec m)
    {	//G.print("R "+m);
        robotState.push(board_state); //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        Execute(m,replayMode.Replay);
        if(DoneState()) { doDone(); } 

    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(MajoritiesMovespec m)
    {
       G.print("U "+m);
    	MajoritiesState state = robotState.pop();
        switch (m.op)
        {
   	    default:
            G.Error("Can't un execute %s", m);
            break;
        case MOVE_DONE:
            break;
            
        case MOVE_DROPB:
        	acceptPlacement();
        	SetBoard(getCell(m.to_col,m.to_row),null);
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(state);
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
 public commonMove Get_Random_Majorities_Move(Random rand)
 {		
	 	int sz = emptyCells.size();
 		int off = Random.nextInt(rand,sz);
 		MajoritiesCell empty = emptyCells.elementAt(off);
 		G.Assert(empty.chip==null,"isn't empty");
 		return(new MajoritiesMovespec(MOVE_DROPB,empty.col,empty.row,playerColor[whoseTurn],whoseTurn));
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack ();
 	switch(board_state)
 	{
 	case Play:
 	case Play1:
 	case Play2:
 		for(int lim = emptyCells.size()-1; lim>=0; lim--)
 		{	MajoritiesCell c = emptyCells.elementAt(lim);
 			all.addElement(new MajoritiesMovespec(MOVE_DROPB,c.col,c.row,playerColor[whoseTurn],whoseTurn));
 		}
 		break;
 	case Confirm:
 		all.addElement(new MajoritiesMovespec(MOVE_DONE,whoseTurn));
 		break;
 	default: throw G.Error("Not expecting state %s",board_state);
 	}
 	return(all);
 }
 

 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(Character.isDigit(txt.charAt(0)))
	 	{ xpos-=cellsize/4;
	 	  ypos -= cellsize/8;
	 	}
 		else
 		{ xpos += cellsize/4; 
 		  ypos += cellsize/4;
 		}
 	GC.Text(gc, true, xpos, ypos, -1, 0,clt, null, txt);
 }

}
