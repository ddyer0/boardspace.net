/* copyright notice */package meridians;


import static meridians.MeridiansMovespec.*;

import java.awt.Color;
import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;


/**
 * Meridians knows all about the game of Meridians, which is played
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
class MGroup extends OStack<MeridiansCell>
{
	public MeridiansCell[] newComponentArray(int sz) { return new MeridiansCell[sz]; }
	MeridiansChip color;
	boolean isolated = false;
	public MGroup(MeridiansChip cc) { color = cc; }
	void findGroup(MeridiansCell c,int sweep)
	{
		if(c.sweep_counter!=sweep)
		{	
			c.sweep_counter = sweep;
			c.group = this;
			push(c);
			for(int dir=c.geometry.n; dir>0; dir--)
			{
				MeridiansCell d = c.exitTo(dir);
				if((d!=null) && (d.topChip()==color)) { findGroup(d,sweep); }
			}
		}
	}
	boolean findIsolated()
	{	isolated = false;
		for(int lim=size()-1; lim>=0; lim--)
		{
			if(elementAt(lim).canSeeOther()) { return false; }
		}
		isolated = true;
		return true;
	}
}
class MGroupStack extends OStack<MGroup>
{
	public MGroup[] newComponentArray(int sz) {
		return new MGroup[sz];
	}
	
}
class MeridiansBoard 
	extends hexBoard<MeridiansCell>	// for a square grid board, this could be rectBoard or squareBoard 
	implements BoardProtocol,MeridiansConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
	static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	MeridiansVariation variation = MeridiansVariation.meridians_5p;
	private MeridiansState board_state = MeridiansState.Puzzle;	
	private MeridiansState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	private IStack robotCaptures = new IStack();
	private boolean swapped = false;
	public MeridiansState getState() { return(board_state); }
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(MeridiansState st) 
	{ 	unresign = (st==MeridiansState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

    private MeridiansId playerColor[]={MeridiansId.White,MeridiansId.Black};    
    private MeridiansChip playerChip[]={MeridiansChip.White,MeridiansChip.Black};
    private MeridiansCell playerCell[]=new MeridiansCell[2];
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public MeridiansChip getPlayerChip(int p) { return(playerChip[p]); }
	public MeridiansId getPlayerColor(int p) { return(playerColor[p]); }
	public MeridiansCell getPlayerCell(int p) { return(playerCell[p]); }
	public MeridiansChip getCurrentPlayerChip() { return(playerChip[whoseTurn]); }
	public MeridiansPlay robot = null;
	public int playerIndex(MeridiansChip ch) { return ch==playerChip[0] ? 0 : 1; }

	 public boolean p1(String msg)
		{
			if(G.p1(msg) && robot!=null)
			{	String dir = "g:/share/projects/boardspace-html/htdocs/meridians/meridiansgames/robot/";
				robot.saveCurrentVariation(dir+msg+".sgf");
				return(true);
			}
			return(false);
		}
	
// this is required even if it is meaningless for this game, but possibly important
// in other games.  When a draw by repetition is detected, this function is called.
// the game should have a "draw pending" state and enter it now, pending confirmation
// by the user clicking on done.   If this mechanism is triggered unexpectedly, it
// is probably because the move editing in "editHistory" is not removing last-move
// dithering by the user, or the "Digest()" method is not returning unique results
// other parts of this mechanism: the Viewer ought to have a "repRect" and call
// DrawRepRect to warn the user that repetitions have been seen.
	public void SetDrawState() {throw G.Error("not expected"); };	
	CellStack animationStack = new CellStack();
    int chips_on_board = 0;			// number of chips currently on the board
    private int fullBoard = 0;				// the number of cells in the board

    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public MeridiansChip pickedObject = null;
    public MeridiansChip lastPicked = null;
    private MeridiansCell blackChipPool = null;	// dummy source for the chip pools
    private MeridiansCell whiteChipPool = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private CellStack captureStack = new CellStack();
    private StateStack stateStack = new StateStack();
    
    // save strings to be shown in the game log
    StringStack gameEvents = new StringStack();
    InternationalStrings s = G.getTranslations();
    
 	void logGameEvent(String str,String... args)
 	{	//if(!robotBoard)
 		{String trans = s.get(str,args);
 		 gameEvents.push(trans);
 		}
 	}

    private CellStack emptyCells=new CellStack();
    private CellStack occupiedCells[] = { new CellStack(),new CellStack() };

    private MeridiansState resetState = MeridiansState.Puzzle; 
    public DrawableImage<?> lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public MeridiansCell newcell(char c,int r)
	{	return(new MeridiansCell(MeridiansId.BoardLocation,c,r));
	}
	
	// constructor 
    public MeridiansBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_LINES; //  draw the cells as triangulated lines
        Grid_Style = GRIDSTYLE;
        setColorMap(map, players);
        
		Random r = new Random(734687);
		// do this once at construction
	    blackChipPool = new MeridiansCell(r,MeridiansId.Black);
	    blackChipPool.addChip(MeridiansChip.Black);
	    whiteChipPool = new MeridiansCell(r,MeridiansId.White);
	    whiteChipPool.addChip(MeridiansChip.White);

        doInit(init,key,players,rev); // do the initialization 
        autoReverseY();		// reverse_y based on the color map
    }
    
    public String gameType() { return(G.concat(gametype," ",players_in_game," ",randomKey," ",revision)); }
    

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
    	win = new boolean[players];
 		setState(MeridiansState.Puzzle);
		variation = MeridiansVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		gametype = gtype;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case meridians_5:
		case meridians_6:
		case meridians_7:
 		case meridians_7p:
 		case meridians_6p:
 		case meridians_5p:

			// using reInitBoard avoids thrashing the creation of cells 
			// when reviewing games.
			reInitBoard(variation.firstInCol,variation.ZinCol,null);
			// or initBoard(variation.firstInCol,variation.ZinCol,null);
			// Random r = new Random(734687);	// this random is used to assign hash values to cells, common to all games of this type.
			// allCells.setDigestChain(r);		// set the randomv for all cells on the board
		}

 		
	    playerCell[FIRST_PLAYER_INDEX] = whiteChipPool; 
	    playerCell[SECOND_PLAYER_INDEX] = blackChipPool; 
	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    chips_on_board = 0;
	    placementNumber = 0;
	    droppedDestStack.clear();
	    captureStack.clear();
	    pickedSourceStack.clear();
	    stateStack.clear();
	    
	    pickedObject = null;
	    resetState = null;
	    lastDroppedObject = null;
	    int map[]=getColorMap();
		playerColor[map[0]]=MeridiansId.White;
		playerColor[map[1]]=MeridiansId.Black;
		playerChip[map[0]]=MeridiansChip.White;
		playerChip[map[1]]=MeridiansChip.Black;
	    // set the initial contents of the board to all empty cells
		emptyCells.clear();
		occupiedCells[0].clear();
		occupiedCells[1].clear();
		
		for(MeridiansCell c = allCells; c!=null; c=c.next) { c.reInit(); emptyCells.push(c); }
		fullBoard = emptyCells.size();
	    
        animationStack.clear();
        if(variation.pie) { doSwap(replayMode.Replay); }	// the second player will play white or swap
        swapped = false;
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }

    /** create a copy of this board */
    public MeridiansBoard cloneBoard() 
	{ MeridiansBoard dup = new MeridiansBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((MeridiansBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(MeridiansBoard from_b)
    {
        super.copyFrom(from_b);
        chips_on_board = from_b.chips_on_board;
        placementNumber = from_b.placementNumber;
        fullBoard = from_b.fullBoard;
        robotState.copyFrom(from_b.robotState);
        getCell(emptyCells,from_b.emptyCells);
        getCell(occupiedCells,from_b.occupiedCells);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        getCell(captureStack,from_b.captureStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        copyFrom(whiteChipPool,from_b.whiteChipPool);		// this will have the side effect of copying the location
        copyFrom(blackChipPool,from_b.blackChipPool);		// from display copy boards to the main board
        getCell(playerCell,from_b.playerCell);
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        swapped = from_b.swapped;
        lastPicked = null;

        AR.copy(playerColor,from_b.playerColor);
        AR.copy(playerChip,from_b.playerChip);
 
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((MeridiansBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(MeridiansBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor mismatch");
        G.Assert(AR.sameArrayContents(playerChip,from_b.playerChip),"playerChip mismatch");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(chips_on_board == from_b.chips_on_board,"chips_on_board mismatch");
        G.Assert(placementNumber == from_b.placementNumber,"placementNumber mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedsourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(sameCells(captureStack,from_b.captureStack),"captureStack mismatch");
        G.Assert(sameCells(playerCell,from_b.playerCell),"player cell mismatch");
        G.Assert(swapped==from_b.swapped,"swapped mismatch");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");

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
        long v = super.Digest(r);
		// many games will want to digest pickedSource too
		// v ^= cell.Digest(r,pickedSource);
		v ^= chip.Digest(r,playerChip[0]);	// this accounts for the "swap" button
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,revision);
		v ^= Digest(r,captureStack);
		v ^= Digest(r,swapped);
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
        return (v);
    }



    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer(replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Move not complete, can't change the current player in state ",board_state);
        case Puzzle:
            break;
        case Play:
        	// some damaged games have 2 dones in a row
        	if(replay==replayMode.Live) { throw G.Error("Move not complete, can't change the current player in state ",board_state); }
			//$FALL-THROUGH$
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
    {	return(board_state.doneState());
    }
    // this is the default, so we don't need it explicitly here.
    // but games with complex "rearrange" states might want to be
    // more selective.  This determines if the current board digest is added
    // to the repetition detection machinery.
    public boolean DigestState()
    {	
    	return(board_state.digestState());
    }



    public boolean gameOverNow() { return(board_state.GameOver()); }
    public boolean winForPlayerNow(int player)
    {	if(win[player]) { return(true); }
    	boolean win = false;
    	return(win);
    }


    // set the contents of a cell, and maintain the books
    int lastPlaced = -1;
    int placementNumber = -1;
    public MeridiansChip SetBoard(MeridiansCell c,MeridiansChip ch)
    {	MeridiansChip old = c.topChip();
    	if(c.onBoard)
    	{
    	if(old!=null) 
    		{ chips_on_board--;
    		  emptyCells.push(c);   
    		  c.lastPlaced = lastPlaced;
    		  occupiedCells[playerIndex(old)].remove(c,false); 
    		}
     	if(ch!=null) { c.lastPlaced = placementNumber++; chips_on_board++; emptyCells.remove(c,false);  }
       	if(old!=null) { c.removeTop();}
       	if(ch!=null) { c.addChip(ch); occupiedCells[playerIndex(ch)].push(c); }
    	}
    	return(old);
    }
    //
    // accept the current placements as permanent
    //
    public void acceptPlacement()
    {	
        droppedDestStack.clear();
        pickedSourceStack.clear();
        stateStack.clear();
        pickedObject = null;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private MeridiansCell unDropObject()
    {	MeridiansCell rv = droppedDestStack.pop();
    	setState(stateStack.pop());
    	pickedObject = SetBoard(rv,null); 	// SetBoard does ancillary bookkeeping
    	placementNumber--;
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	MeridiansCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	SetBoard(rv,pickedObject);
    	pickedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(MeridiansCell c)
    {
       droppedDestStack.push(c);
       stateStack.push(board_state);
       
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting dest " + c.rackLocation);
        case Black:
        case White:		// back in the pool, we don't really care where
        	pickedObject = null;
            break;
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        	SetBoard(c,pickedObject);
            pickedObject = null;
            break;
        }
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(MeridiansCell c)
    {	return(droppedDestStack.top()==c);
    }
    public MeridiansCell getDest()
    {	return(droppedDestStack.top());
    }
 
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { MeridiansChip ch = pickedObject;
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
    private MeridiansCell getCell(MeridiansId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source " + source);
        case BoardLocation:
        	return(getCell(col,row));
        case Black:
        	return(blackChipPool);
        case White:
        	return(whiteChipPool);
        } 	
    }
    /**
     * this is called when copying boards, to get the cell on the new (ie current) board
     * that corresponds to a cell on the old board.
     */
    public MeridiansCell getCell(MeridiansCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(MeridiansCell c)
    {	pickedSourceStack.push(c);
    	stateStack.push(board_state);
        switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting rackLocation " + c.rackLocation);
        case BoardLocation:
        	{
            lastPicked = pickedObject = c.topChip();
         	lastDroppedObject = null;
			SetBoard(c,null);
        	}
            break;

        case Black:
        case White:
        	lastPicked = pickedObject = c.topChip();
        }
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(MeridiansCell c)
    {	return(c==pickedSourceStack.top());
    }
    public MeridiansCell getSource()
    {	return(pickedSourceStack.top());
    }
 
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state " + board_state);
        case Confirm:
        	setNextStateAfterDone(replay);
         	break;
        case PlacePie:
        	if(! ((occupiedCells[0].size()==1) && (occupiedCells[1].size()==1))) { break;}
			//$FALL-THROUGH$
		case PlayFirst:
		case PlayOrSwap:
        case Play:
			setState(MeridiansState.Confirm);
			break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    
    private void undoCaptures(int size)
    {	
    	for(int cs = captureStack.size(); cs>size; cs--)
    	{
    		MeridiansCell c = captureStack.pop();
    		SetBoard(c,playerChip[whoseTurn]);
    	}
    }
    private void setNextStateAfterDone(replayMode replay)
    {	int nchips = occupiedCells[whoseTurn].size();
    	int nother = occupiedCells[nextPlayer[whoseTurn]].size();
    	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state "+board_state);
    	case Gameover: break;
    	case Puzzle:
    	case PlacePie:
    	case PlayOrSwap:  		
    		if(variation.pie)
    		{
    			if((nchips==1) && (nother==1)) { setState(MeridiansState.PlayOrSwap); break; }
    			else if(nchips+nother<=1) { setState(MeridiansState.PlacePie); break; }
    		}
			//$FALL-THROUGH$
		case Confirm:
		case ConfirmSwap:
    	case Play:
    		
    		setState( (variation.pie && nchips==1 && nother==1 && !swapped)
    					? MeridiansState.PlayOrSwap
    					: (nchips==0 ? MeridiansState.PlayFirst : MeridiansState.Play)); 
        	removeIsolatedGroups(nextPlayer[whoseTurn],replay);
        	if(occupiedCells[nextPlayer[whoseTurn]].size()==0 && occupiedCells[whoseTurn].size()>0) 
        		{ win[whoseTurn]=true;
        		  setState(MeridiansState.Gameover); 
        		}
    		break;
    	}
       	resetState = board_state;
    }
    private void removeIsolatedGroups(int who,replayMode replay)
    {	sweep_counter++;
    	MGroupStack mgs = findGroups(occupiedCells[who]);
    	if(occupiedCells[who].size()>1)	// not on the first move
    	{
    	for(int lim=mgs.size()-1; lim>=0; lim--)
    	{
    		MGroup group = mgs.elementAt(lim);
    		MeridiansCell pc = playerCell[who];
    		if(group.isolated) {
    			while(group.size()>0)
    			{
    				MeridiansCell c = group.pop();
    				if(replay!=replayMode.Replay)
    				{
    					animationStack.push(c);
    					animationStack.push(pc);
    				}
    				captureStack.push(c);
    				SetBoard(c,null);
    			}
    		}
    	}}
    	
    }
    private void doDone(replayMode replay)
    {
        acceptPlacement();

        if (board_state==MeridiansState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(MeridiansState.Gameover);
        }
        else
        {
        	
        	if(winForPlayerNow(whoseTurn)) 
        		{ win[whoseTurn]=true;
        		  setState(MeridiansState.Gameover); 
        		}
        	else {setNextPlayer(replay);
        		setNextStateAfterDone(replay);
        	}
        }
    }
    
    void doSwap(replayMode replay)
    {	MeridiansId c = playerColor[0];
    	MeridiansChip ch = playerChip[0];
    	playerColor[0]=playerColor[1];
    	playerChip[0]=playerChip[1];
    	playerColor[1]=c;
    	playerChip[1]=ch;
    	MeridiansCell cc = playerCell[0];
    	playerCell[0]=playerCell[1];
    	playerCell[1]=cc;
    	CellStack cs = occupiedCells[0];
    	occupiedCells[0] = occupiedCells[1];
    	occupiedCells[1] = cs;
    	swapped = !swapped;

    	switch(board_state)
    	{	
    	default: 
    		throw G.Error("Not expecting swap state "+board_state);
    	case Play:
    		// some damaged game records have double swap
    		if(replay==replayMode.Live) { G.Error("Not expecting swap state "+board_state); }
    		//$FALL-THROUGH$
    	case PlayOrSwap:
    		  setState(MeridiansState.ConfirmSwap);
    		  break;
    	case ConfirmSwap:
    		  setState(MeridiansState.PlayOrSwap);
    		  break;
    	case Gameover:
    	case Puzzle: break;
    	}
    	}
    public boolean Execute(commonMove mm,replayMode replay)
    {	MeridiansMovespec m = (MeridiansMovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }

        //G.print("E "+m+" for "+whoseTurn+" "+state);
        switch (m.op)
        {
        case MOVE_SWAP:
        	doSwap(replay);
        	break;
        case MOVE_DONE:

         	doDone(replay);

            break;

        case MOVE_DROPB:
        	{
			MeridiansChip po = pickedObject;
			if(po==null) 
				{ 
				  int pl = (board_state==MeridiansState.PlacePie) 
						  	? (occupiedCells[0].size()==0 ? 0 : 1)
						  	: whoseTurn;
				  pickedObject = playerChip[pl]; 
				  pickedSourceStack.push(getPlayerCell(pl));
				}
			MeridiansCell dest =  getCell(MeridiansId.BoardLocation,m.to_col,m.to_row);
			
			if(isSource(dest)) 
				{ unPickObject(); 
				}
				else 
				{
				m.chip = pickedObject;
		           
	            dropObject(dest);
	            /**
	             * if the user clicked on a board space without picking anything up,
	             * animate a stone moving in from the pool.  For Hex, the "picks" are
	             * removed from the game record, so there are never picked stones in
	             * single step replays.
	             */
	            if(replay!=replayMode.Replay && (po==null))
	            	{ animationStack.push(getSource());
	            	  animationStack.push(dest); 
	            	}
	            setNextStateAfterDrop(replay);
				}
        	}
             break;

        case MOVE_PICK:
 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
 			MeridiansCell src = getCell(m.source,m.to_col,m.to_row);
 			if(isDest(src)) { unDropObject(); }
 			else
 			{
        	// be a temporary p
        	pickObject(src);
        	m.chip = pickedObject;
        	switch(board_state)
        	{
        	case Puzzle:
         		break;
        	case Confirm:
        		setState(MeridiansState.Play);
        		break;
        	default: ;
        	}}}
            break;

        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
            MeridiansCell dest = getCell(m.source,m.to_col,m.to_row);
            if(isSource(dest)) { unPickObject(); }
            else 
            	{
		        if(replay==replayMode.Live)
	        	{ lastDroppedObject = pickedObject.getAltDisplayChip(dest);
	        	  //G.print("last ",lastDroppedObject); 
	        	}      	
            	dropObject(dest); 
            
            	}
        	}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            int nextp = nextPlayer[whoseTurn];
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(MeridiansState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ setState(MeridiansState.Gameover); 
               	}
            else {  setNextStateAfterDone(replay); }

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?MeridiansState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(MeridiansState.Puzzle);
 
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(MeridiansState.Gameover);
    	   break;

        default:
        	cantExecute(m);
        }
        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }

    // legal to hit the chip storage area
    public boolean legalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting Legal Hit state " + board_state);
        case PlacePie:
        	return (occupiedCells[player].size()==0);
        case Play:
        case PlayOrSwap:
        case PlayFirst:
        	// for pushfight, you can pick up a stone in the storage area
        	// but it's really optional
        	return(player==whoseTurn);
        case Confirm:
        case ConfirmSwap:
		case Resign:
		case Gameover:
			return(false);
        case Puzzle:
            return ((pickedObject!=null)?(pickedObject==playerChip[player]):true);
        }
    }

    public boolean legalToHitBoard(MeridiansCell c,Hashtable<MeridiansCell,MeridiansMovespec> targets )
    {	if(c==null) { return(false); }
        switch (board_state)
        {
        case PlacePie:
		case Play:
		case PlayOrSwap:
		case PlayFirst:
			return(targets.get(c)!=null || isDest(c) || isSource(c));
		case Gameover:
		case Resign:
			return(false);
		case Confirm:
		case ConfirmSwap:
			return(isDest(c));
        default:
        	throw G.Error("Not expecting Hit Board state " + board_state);
        case Puzzle:
            return (true);
        }
    }
    
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(MeridiansMovespec m)
    {	robotCaptures.push(captureStack.size());
        robotState.push(board_state); //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        Execute(m,replayMode.Replay);
        acceptPlacement();
       
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(MeridiansMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	MeridiansState state = robotState.pop();
        switch (m.op)
        {
        default:
   	    	throw G.Error("Can't un execute " + m);
        case MOVE_SWAP:
        	doSwap(replayMode.Replay);
        	break;
        case MOVE_DONE:
            break;
            
        case MOVE_DROPB:
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
    	undoCaptures(robotCaptures.pop());
 }
  
 private void addPlacementMoves(CommonMoveStack all,int who)
 {
	 CellStack seeds = occupiedCells[who];
	 if(seeds.size()==0)
	 	{ for(int lim=emptyCells.size()-1; lim>=0; lim--)
	 		{
	 		MeridiansCell c = emptyCells.elementAt(lim);
	 		all.push(new MeridiansMovespec(MOVE_DROPB,c.col,c.row,who));
	 		}
	 	}
	 else
	 {	sweep_counter++;
		for(int lim=seeds.size()-1; lim>=0; lim--)
		{
			MeridiansCell c = seeds.elementAt(lim);
			for(int dir = 0; dir<CELL_FULL_TURN; dir++)
			{
				MeridiansCell d = c;
				while((d=d.exitTo(dir))!=null && d.topChip()==null)
				{	
					if(d.sweep_counter!=sweep_counter)
					{
						all.push(new MeridiansMovespec(MOVE_DROPB,d.col,d.row,who));
						d.sweep_counter = sweep_counter;
					}
				}
			}
		}
	 }

 }

 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	switch(board_state)
 	{
 	case Gameover: break;
 	case Puzzle:
 		{int op = pickedObject==null ? MOVE_DROPB : MOVE_PICKB; 	
 			for(MeridiansCell c = allCells;
 			 	    c!=null;
 			 	    c = c.next)
 			 	{	if(c.topChip()==null)
 			 		{all.addElement(new MeridiansMovespec(op,c.col,c.row,whoseTurn));
 			 		}
 			 	}
 		}
 		break;
 	case PlacePie:
 		{
 		int pl = occupiedCells[0].size()==0 ? 0 : 1;
 		addPlacementMoves(all,pl);
 		break;
 		}
 	case PlayOrSwap:
 		all.push(new MeridiansMovespec(MOVE_SWAP,whoseTurn));
		//$FALL-THROUGH$
	case Play:
 	case PlayFirst:
 		addPlacementMoves(all,whoseTurn);
 		if(all.size()==0) { all.push(new MeridiansMovespec(MOVE_RESIGN,whoseTurn)); }
 		break;
 	case ConfirmSwap:
 		all.push(new MeridiansMovespec(MOVE_SWAP,whoseTurn));
		//$FALL-THROUGH$
	case Resign:
 	case Confirm:
 		all.push(new MeridiansMovespec(MOVE_DONE,whoseTurn));
 		break;
 	default:
 			G.Error("Not expecting state ",board_state);
 	}
 	//if(all.size()==0) { G.Error("No moves!"); }
 	return(all);
 }
 
 public void initRobotValues()
 {
	 for(int lim = emptyCells.size()-1; lim>=0; lim--)
	 {
		 emptyCells.elementAt(lim).initRobotValues();
	 }
 }

 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(Character.isDigit(txt.charAt(0)))
	 	{ switch(variation)
	 		{
	 		case meridians_7:
	 		case meridians_6:
	 		case meridians_5:
	 		case meridians_7p:
	 		case meridians_6p:
	 		case meridians_5p:
	 			xpos -= cellsize/2;
	 			break;
 			default: G.Error("case "+variation+" not handled");
	 		}
	 	}
 		else
 		{ 
 		  ypos += cellsize/4;
 		}
 	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
 }
 private int sweep_counter = 0;
 public void findGroups()
 {
	 sweep_counter++;
	 findGroups(occupiedCells[0]);
	 findGroups(occupiedCells[1]);
	 
 }
 private MGroupStack findGroups(CellStack from)
 {	MGroupStack mgs = new MGroupStack();
 	for(int lim=from.size()-1; lim>=0; lim--)
	 {
		 MeridiansCell c = from.elementAt(lim);
		 if(c.sweep_counter!=sweep_counter)
			 {	
			 	MGroup mm = new MGroup(c.topChip());
			 	mgs.push(mm);
			 	mm.findGroup(c,sweep_counter);
			 }
	 }
 	for(int lim=mgs.size()-1; lim>=0; lim--)
 	{
 		mgs.elementAt(lim).findIsolated();
 	}
 	return mgs;
 }
 
 /**
  *  get the board cells that are valid targets right now, intended to be used
  *  by the user interface to determine where it's legal to play.  The standard
  *  method is to call the move generator, and filter the results to generate
  *  the cells of interest.  It's usually more complicated than just that,
  *  but using the move generator to drive the selection of cells to point 
  *  at avoids duplicating a lot of tricky logic.
  *  
  * @return
  */
 public Hashtable<MeridiansCell, MeridiansMovespec> getTargets() 
 {
 	Hashtable<MeridiansCell,MeridiansMovespec> targets = new Hashtable<MeridiansCell,MeridiansMovespec>();
 	CommonMoveStack all = GetListOfMoves();
 	for(int lim=all.size()-1; lim>=0; lim--)
 	{	MeridiansMovespec m = (MeridiansMovespec)all.elementAt(lim);
 		switch(m.op)
 		{
 		case MOVE_PICKB:
 		case MOVE_DROPB:
 			targets.put(getCell(m.to_col,m.to_row),m);
 			break;
 		case MOVE_SWAP:
 		case MOVE_RESIGN:
 		case MOVE_DONE:
 			break;

 		default: G.Error("Not expecting "+m);
 		
 		}
 	}
 	
 	return(targets);
 }
 


 // most multi player games can't handle individual players resigning
 // this provides an escape hatch to allow it.
 //public boolean canResign() { return(super.canResign()); }
}
