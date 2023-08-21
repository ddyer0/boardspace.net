package trench;


import static trench.Trenchmovespec.*;

import java.awt.Color;
import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;

/**
 * TrenchBoard knows all about the game of Trench, which is played
 * on a diagonal board. It gets a lot of logistic support from 
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

class TrenchBoard 
	extends rectBoard<TrenchCell>	// for a square grid board, this could be rectBoard or squareBoard 
	implements BoardProtocol,TrenchConstants
{	static int REVISION = 101;			// 100 represents the initial version of the game
										// revision 101 adds defense for multiple draw offers
	public int getMaxRevisionLevel() { return(REVISION); }
	static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	TrenchVariation variation = TrenchVariation.trench;
	private TrenchState board_state = TrenchState.Puzzle;	
	private TrenchState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	private IStack robotStack = new IStack();
	public TrenchState getState() { return(board_state); }
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(TrenchState st) 
	{ 	unresign = (st==TrenchState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

    private TrenchId playerColor[]={TrenchId.Black,TrenchId.White};    
    private TrenchChip playerChip[]={TrenchChip.black_5p,TrenchChip.white_5p};
    // occupied index is always black=0 white=1
    private CellStack occupied(TrenchChip ch) 
    	{ return (ch.color==TrenchId.Black)
    				?occupiedCells[FIRST_PLAYER_INDEX]
    				:occupiedCells[SECOND_PLAYER_INDEX];
    	}
    private CellStack occupied(int who)
    {
    	return occupiedCells[getColorMap()[who]];
    }
    private TrenchCell playerCell[]=new TrenchCell[2];
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public TrenchChip getPlayerChip(int p) { return(playerChip[p]); }
	public TrenchId getPlayerColor(int p) { return(playerColor[p]); }
	public TrenchCell getPlayerCell(int p) { return(playerCell[p]); }
	public TrenchChip getCurrentPlayerChip() { return(playerChip[whoseTurn]); }
	public TrenchPlay robot = null;
	
	 public boolean p1(String msg)
		{
			if(G.p1(msg) && robot!=null)
			{	String dir = "g:/share/projects/boardspace-html/htdocs/trench/trenchgames/robot/";
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
	public void SetDrawState() { setState(TrenchState.Draw); };	
	CellStack animationStack = new CellStack();

    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public TrenchChip pickedObject = null;
    public TrenchChip lastPicked = null;
    private TrenchCell blackCaptured = null;	// dummy source for the chip pools
    private TrenchCell whiteCaptured = null;
    private int lastProgressMove = 0;
    private int lastDrawMove = 0;
    
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private CellStack captureStack = new CellStack();
    private IStack captureHeight = new IStack();
    private StateStack stateStack = new StateStack();
    
    private TrenchCell captured(TrenchChip top)
    {
    	switch(top.color) {
    	case Black: return blackCaptured;
    	case White: return whiteCaptured;
    	default: throw G.Error("Not expecting %s",top.color);
    	}
    }
    public TrenchCell captured(int who)
    {
    	return captured(playerChip[who]);
    }
    // save strings to be shown in the game log
    StringStack gameEvents = new StringStack();
    InternationalStrings s = G.getTranslations();
    
 	void logGameEvent(String str,String... args)
 	{	//if(!robotBoard)
 		{String trans = s.get(str,args);
 		 gameEvents.push(trans);
 		}
 	}

    private CellStack occupiedCells[] = { new CellStack(),new CellStack()};
    
    private TrenchState resetState = TrenchState.Puzzle; 
    public DrawableImage<?> lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public TrenchCell newcell(char c,int r)
	{	return(new TrenchCell(TrenchId.BoardLocation,c,r));
	}
	
	// constructor 
    public TrenchBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = GRIDSTYLE;
        setColorMap(map, players);
        
		Random r = new Random(734687);
		// do this once at construction
	    blackCaptured = new TrenchCell(r,TrenchId.Black);
	    whiteCaptured = new TrenchCell(r,TrenchId.White);

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
    private void markTrenchCells(TrenchCell d, int direction)
    {	int n = 0;
		while( ((d = d.exitTo(direction))!=null)
				&& (++n<=5))
			{
			 d.visibleFromTrench += 6-n;
			}
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int players,int rev)
    {	randomKey = key;
    	adjustRevision(rev);
    	players_in_game = players;
    	win = new boolean[players];
 		setState(TrenchState.Puzzle);
		variation = TrenchVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		gametype = gtype;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case trench:
			// using reInitBoard avoids thrashing the creation of cells 
			// when reviewing games.
			reInitBoard(variation.size,variation.size);
		}

		for(TrenchCell c = allCells; c!=null; c=c.next)
		{	if(c.cellType==TrenchId.Trench)
			{
			markTrenchCells(c,CELL_UP_RIGHT);
			markTrenchCells(c,CELL_RIGHT);
			markTrenchCells(c,CELL_DOWN_LEFT);
			markTrenchCells(c,CELL_DOWN);

			}
		}
	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    lastProgressMove = 0;
	    lastDrawMove = 0;
	    droppedDestStack.clear();
	    pickedSourceStack.clear();
	    captureStack.clear();
	    captureHeight.clear();
	    stateStack.clear();
	    
	    pickedObject = null;
	    resetState = null;
	    lastDroppedObject = null;
	    int map[]=getColorMap();
	    blackCaptured.reInit();
	    whiteCaptured.reInit();
	    playerCell[map[FIRST_PLAYER_INDEX]] = whiteCaptured; 
	    playerCell[map[SECOND_PLAYER_INDEX]] = blackCaptured; 
		playerColor[map[FIRST_PLAYER_INDEX]]=TrenchId.Black;
		playerColor[map[SECOND_PLAYER_INDEX]]=TrenchId.White;
		playerChip[map[FIRST_PLAYER_INDEX]]=TrenchChip.black_5;
		playerChip[map[SECOND_PLAYER_INDEX]]=TrenchChip.white_5;
	    // set the initial contents of the board to all empty cells
		occupiedCells[FIRST_PLAYER_INDEX].clear();
		occupiedCells[SECOND_PLAYER_INDEX].clear();
		for(TrenchCell c = allCells; c!=null; c=c.next) { c.reInit(); }
		
		int inits[][] = { {5,'A',1},{4,'A',2},{4,'B',1},
						  {3,'A',3},{3,'B',2},{3,'C',1},
						  {2,'A',4},{2,'B',3},{2,'C',2},{2,'D',1},
						  {1,'B',4},{1,'C',3},{1,'D',2},
						  {1,'C',4},{1,'D',3},
						  {1,'D',4}
						};
						  
		for(int spec[] : inits)
		{	
			TrenchChip b = TrenchChip.getChip(TrenchId.Black,spec[0]);
			TrenchCell c = getCell((char)spec[1],spec[2]);
			c.addChip(b);
			occupiedCells[FIRST_PLAYER_INDEX].push(c);
			
			TrenchChip w = TrenchChip.getChip(TrenchId.White,spec[0]);
			TrenchCell d = getCell((char)('A'+variation.size-(spec[1]-'A')-1),variation.size+1-spec[2]);
			d.addChip(w);
			occupiedCells[SECOND_PLAYER_INDEX].push(d);
		}
	    
        animationStack.clear();
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }

    /** create a copy of this board */
    public TrenchBoard cloneBoard() 
	{ TrenchBoard dup = new TrenchBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((TrenchBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(TrenchBoard from_b)
    {
        super.copyFrom(from_b);
        robotState.copyFrom(from_b.robotState);
        getCell(occupiedCells,from_b.occupiedCells);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        lastProgressMove = from_b.lastProgressMove;
        lastDrawMove = from_b.lastDrawMove;
        getCell(captureStack,from_b.captureStack);
        captureHeight.copyFrom(from_b.captureHeight);
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        copyFrom(whiteCaptured,from_b.whiteCaptured);		// this will have the side effect of copying the location
        copyFrom(blackCaptured,from_b.blackCaptured);		// from display copy boards to the main board
        getCell(playerCell,from_b.playerCell);
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        lastPicked = null;

        AR.copy(playerColor,from_b.playerColor);
        AR.copy(playerChip,from_b.playerChip);
 
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((TrenchBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(TrenchBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor mismatch");
        G.Assert(AR.sameArrayContents(playerChip,from_b.playerChip),"playerChip mismatch");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedsourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(sameCells(captureStack,from_b.captureStack),"captureStack mismatch");
        G.Assert(lastProgressMove==from_b.lastProgressMove,"lastProgressMove mismatch");
        G.Assert(lastDrawMove==from_b.lastDrawMove,"lastDrawMove mismatch");
        G.Assert(sameContents(captureHeight,from_b.captureHeight),"captureHeight mismatch");
        G.Assert(sameCells(playerCell,from_b.playerCell),"player cell mismatch");
        G.Assert(blackCaptured.sameContents(from_b.blackCaptured),"blackCaptured mismatch");
        G.Assert(whiteCaptured.sameContents(from_b.whiteCaptured),"whiteCaptured mismatch");
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
		v ^= Digest(r,lastProgressMove);
		v ^= Digest(r,revision);
		v ^= Digest(r,blackCaptured);
		v ^= Digest(r,whiteCaptured);		
		// a rare numerical coincidence made the standard formulation board_state.ordinal*10+whoseTurn
		// result in a "no change" digest. 
		v ^= Digest(r,board_state);
		v ^= Digest(r,whoseTurn);
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
        case AcceptPending:
        case DeclinePending:
        case DrawPending:
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
    {	return win[player];
    }
    
    public boolean checkForWin(int player)
    {	
    	if(totalCaptured(nextPlayer[player])>=WIN)
    	{
		win[whoseTurn] = true;
		return true;
    	}
    	return(false);
    }


    // set the contents of a cell, and maintain the books
    private int lastPlaced = -1;
    public TrenchChip SetBoard(TrenchCell c,TrenchChip ch)
    {	TrenchChip old = c.topChip();
    	if(c.onBoard)
    	{
    	if(old!=null) { occupied(old).remove(c); c.lastEmptied = moveNumber; }
     	if(ch!=null) 
     		{	occupied(ch).push(c);
     		    lastPlaced = c.lastPlaced;
     		    c.lastPlaced = moveNumber; 
     		}
     		else { c.lastPlaced = lastPlaced; }
    	}
       	if(old!=null) { c.removeTop();}
       	if(ch!=null) { c.addChip(ch);  }
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
    private TrenchCell unDropObject()
    {	TrenchCell rv = droppedDestStack.pop();
    	setState(stateStack.pop());
    	pickedObject = SetBoard(rv,null); 	// SetBoard does ancillary bookkeeping
    	if(board_state!=TrenchState.Puzzle) { undoCaptures(whoseTurn); }
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	TrenchCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	SetBoard(rv,pickedObject);
    	pickedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(TrenchCell c,int index)
    {  
       droppedDestStack.push(c);
       stateStack.push(board_state);
       
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting dest " + c.rackLocation);
        case Black:
        case White:		// back in the pool, we don't really care where
        	if(index<0 || index>=c.height()) { c.addChip(pickedObject); }
        	else { c.insertChipAtIndex(index,pickedObject); }
            break;
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        	SetBoard(c,pickedObject);
             break;
        }
       pickedObject = null;
       
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(TrenchCell c)
    {	return(droppedDestStack.top()==c);
    }
    public TrenchCell getDest()
    {	return(droppedDestStack.top());
    }
 
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { TrenchChip ch = pickedObject;
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
    private TrenchCell getCell(TrenchId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source " + source);
        case BoardLocation:
        	return(getCell(col,row));
        case Black:
        	return(blackCaptured);
        case White:
        	return(whiteCaptured);
        } 	
    }
    /**
     * this is called when copying boards, to get the cell on the new (ie current) board
     * that corresponds to a cell on the old board.
     */
    public TrenchCell getCell(TrenchCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(TrenchCell c,int index)
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
        	lastPicked = pickedObject = ((index<0)||index>=c.height()) ? c.removeTop() : c.removeChipAtIndex(index);
        }
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(TrenchCell c)
    {	return(c==pickedSourceStack.top());
    }
    public TrenchCell getSource()
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
        case Play:
			setState(TrenchState.Confirm);
			break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    private void setNextStateAfterDone(replayMode replay)
    {	
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state=%s ",board_state);
    	
    	case Gameover: break;
    	case DrawPending:
    		lastDrawMove = moveNumber;
    		setState(TrenchState.AcceptOrDecline);
    		break;
    	case Draw:
       	case AcceptPending:
       		setState(TrenchState.Gameover);
       		break;
    	case DeclinePending:
     	case Confirm:
    	case Puzzle:
    	case Play:
    		
    		setState(TrenchState.Play);
    		
    		break;
    	}
       	resetState = board_state;
    }
    private void doDone(replayMode replay)
    {
        acceptPlacement();
        if (board_state==TrenchState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(TrenchState.Gameover);
        }
        else
        {	if(checkForWin(whoseTurn)) 
        		{ 
        		  setState(TrenchState.Gameover); 
        		}
        	else {setNextPlayer(replay);
        		setNextStateAfterDone(replay);
        	}
        }
    }
    public boolean isQuiet()
    {
    	return captureHeight.top()==captureStack.size();
    }
    private void undoCaptures(int who)
    {	
    	int h = captureHeight.pop();
    	lastProgressMove = captureHeight.pop();
    	while(captureStack.size()>h)
    	{
    		TrenchCell dest = captureStack.pop();
    		TrenchCell cap = captured(nextPlayer[who]);
    		TrenchChip top = cap.removeTop();
    		dest.lastCaptured = -1;
    		dest.lastContents = null;
    		SetBoard(dest,top);   		
    	}
    }
    private void doCapture(TrenchCell from,replayMode replay)
    {
    	captureStack.push(from);
    	lastProgressMove = moveNumber;
    	TrenchChip top = SetBoard(from,null);
    	TrenchCell to = captured(top);
    	from.lastCaptured = moveNumber;
    	from.lastContents = top;
    	to.addChip(top);
    	if(replay!=replayMode.Replay)
    	{
    		animationStack.push(from);
    		animationStack.push(to);
    	}
    }
	private void moveFromTo(Trenchmovespec m,TrenchCell from,TrenchCell to,TrenchChip po,replayMode replay)
	{	
		if(po==null) { pickObject(from,-1); }
		m.chip = pickedObject;
		m.captures = 0;
        /**
         * if the user clicked on a board space without picking anything up,
         * animate a stone moving in from the pool.  For Hex, the "picks" are
         * removed from the game record, so there are never picked stones in
         * single step replays.
         */
		int direction = findDirection(from,to);
		TrenchCell next = from;
		if(board_state!=TrenchState.Puzzle)
		{
		captureHeight.push(lastProgressMove);
		captureHeight.push(captureStack.size());
		while( (next=next.exitTo(direction))!=null)
		{
				TrenchChip top = next.topChip();
				if(top!=null)
				{	m.captures++;
					doCapture(next,replay);
				}
				if(next==to) { break; }
			}
			
		}
		
		dropObject(to,-1);
	

        if(replay!=replayMode.Replay && (po==null))
        	{ animationStack.push(from);
        	  animationStack.push(to); 
        	}
        setNextStateAfterDrop(replay);
	}
    public boolean Execute(commonMove mm,replayMode replay)
    {	Trenchmovespec m = (Trenchmovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }

       // G.print("E "+m+" for "+whoseTurn+" "+board_state+" "+Digest());
        switch (m.op)
        {
        case MOVE_OFFER_DRAW:
        	if(revision<101 || canOfferDraw())
        	{
        	if(board_state==TrenchState.DrawPending) { setState(TrenchState.Play); }
        	else { 
        			setState(TrenchState.DrawPending);
        		}
        	}
        	break;
        case MOVE_ACCEPT_DRAW:
           	switch(board_state)
        	{	
        	case AcceptPending: 	// cancel accept and revert to neutral
        		setState(TrenchState.AcceptOrDecline); 
        		break;
           	case AcceptOrDecline:
           	case DeclinePending:	// accept pending
           		setState(TrenchState.AcceptPending); 
           		break;
        	default: throw G.Error("Not expecting %s",board_state);
        	}
           	break;
        case MOVE_DECLINE_DRAW:
        	switch(board_state)
        	{	
        	case DeclinePending:	// cancel decline and revert to neutral
        		setState(TrenchState.AcceptOrDecline); 
        		break;
        	case AcceptOrDecline:
        	case AcceptPending: setState(TrenchState.DeclinePending); break;
        	default: throw G.Error("Not expecting %s",board_state);
        	}
        	break;

        case MOVE_DONE:

         	doDone(replay);

            break;

        case MOVE_DROPB:
        	{
			TrenchCell dest =  getCell(TrenchId.BoardLocation,m.to_col,m.to_row);
			
			if(isSource(dest)) 
				{ unPickObject(); 
				}
				else 
				{
				lastDroppedObject = pickedObject;
	            moveFromTo(m,getSource(),dest,pickedObject,replay);

				}
        	}
             break;
        case MOVE_FROM_TO:
        case MOVE_CAPTURE:
        case MOVE_ATTACK:
        	{
        	TrenchCell from = getCell(m.from_col,m.from_row);
        	TrenchCell to = getCell(m.to_col,m.to_row);
        	moveFromTo(m,from,to,null,replay);
        	}
        	break;
        case MOVE_PICK:
			{
			TrenchCell src = getCell(m.source,m.to_col,m.to_row);
			pickObject(src,m.to_row);
			m.chip = pickedObject;
			}
			break;
 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
 			TrenchCell src = getCell(m.source,m.to_col,m.to_row);
 			if(isDest(src)) { unDropObject(); }
 			else
 			{
        	pickObject(src,-1);
        	m.chip = pickedObject;	
        	}}
            break;

        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
            TrenchCell dest = getCell(m.source,m.to_col,m.to_row);
            if(replay==replayMode.Live)
            	{ lastDroppedObject = pickedObject.getAltDisplayChip(dest);
            	}      	
            dropObject(dest,m.to_row); 
        	acceptPlacement();
        	}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            int nextp = nextPlayer[whoseTurn];
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(TrenchState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=checkForWin(whoseTurn))
               ||(win[nextp]=checkForWin(nextp)))
               	{ setState(TrenchState.Gameover); 
               	}
            else {  setNextStateAfterDone(replay); }

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?TrenchState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(TrenchState.Puzzle);
 
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(TrenchState.Gameover);
    	   break;

        default:
        	cantExecute(m);
        }
        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        //G.print("X "+m+" for "+whoseTurn+" "+board_state+" "+Digest());
        
        return (true);
    }

    // legal to hit the chip storage area
    public boolean legalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting Legal Hit state " + board_state);
        case Play:
        	return(false);
        case Confirm:
		case Resign:
		case Gameover:
		case AcceptOrDecline:
		case DeclinePending:
		case AcceptPending:
		case DrawPending:
		case Draw:
			return(false);
        case Puzzle:
            return ((pickedObject!=null)?(pickedObject.color==playerColor[player]):true);
        }
    }

    public boolean legalToHitBoard(TrenchCell c,Hashtable<TrenchCell,Trenchmovespec> targets )
    {	if(c==null) { return(false); }
        switch (board_state)
        {
		case Play:
			return(targets.get(c)!=null || isDest(c) || isSource(c));
		case Gameover:
		case Resign:
		case AcceptOrDecline:
		case DeclinePending:
		case AcceptPending:
		case DrawPending:
		case Draw:
			return(false);
		case Confirm:
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
    public void RobotExecute(Trenchmovespec m)
    {
        robotState.push(board_state); //record the starting state. The most reliable
        robotStack.push(lastDrawMove);
        // to undo state transistions is to simple put the original state back.
        
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        //G.print("R "+m);
        Execute(m,replayMode.Replay);
        if(board_state.doneState()) { doDone(replayMode.Replay); }
        else {  acceptPlacement(); }
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(Trenchmovespec m)
    {
        //G.print("U "+m);
        TrenchState state = robotState.pop();
        lastDrawMove = robotStack.pop();
        switch (m.op)
        {
        default:
   	    	throw G.Error("Can't un execute " + m);
        case MOVE_DONE:
        case MOVE_OFFER_DRAW:
        case MOVE_ACCEPT_DRAW:
        case MOVE_DECLINE_DRAW:
            break;
        case MOVE_FROM_TO:
        case MOVE_CAPTURE:
        case MOVE_ATTACK:
            {
            	TrenchCell from = getCell(m.from_col,m.from_row);
            	TrenchCell to = getCell(m.to_col,m.to_row);
            	TrenchChip top = SetBoard(to,null);
            	undoCaptures(m.player);
            	SetBoard(from,top);
             	}
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
 private void addPieceMoves(CommonMoveStack all,int who)
 {
	 CellStack occupied = occupied(who);
	 if(pickedObject==null)
	 {
	 for(int lim = occupied.size()-1; lim>=0; lim--)
	 {
		 TrenchCell from = occupied.elementAt(lim);
		 addPieceMoves(all,from,from.topChip(),who);
	 }}
	 else
	 {
		 addPieceMoves(all,pickedSourceStack.top(),pickedObject,who);
	 }
 }
 //
 // the move generation logic is mostly driven by the "Type" field which has
 // the map of allowable directions and the distance a piece can move.  The
 // complications are that black and white pieces have opposite "home" direction
 // arrows, so all the directions of travel have to be inverted for one color.
 // the major ad-hoc logic is that the trench is special, and pieces have different
 // rules depending on if they are moving from, to, or within the trench.
 //
 private void addPieceMoves(CommonMoveStack all,TrenchCell from,TrenchChip top,int who)
 {
	 TrenchChip.Type p = top.type;
	 int reverse = top.color==TrenchId.Black ? CELL_HALF_TURN : 0;
	 for(int direction0 : p.directions)
	 {	TrenchCell to = from;
	 	int direction = direction0+reverse;	// which pieces move in opposite directions
		int distance = p.distance;
		while( (distance-- > 0) && ((to = to.exitTo(direction))!=null))
		{
			TrenchChip victim = to.topChip();
			if(victim==null)
			{	// move to empty
				all.push(new Trenchmovespec(MOVE_FROM_TO,from,to,who));
			}
			else if(victim.color == top.color) { distance = 0; }	// blocked
			else // capture, maybe
			{	switch(from.cellType)
				{	
					case Trench:
						switch(to.cellType)
						{
						case Trench:
							// pieces in the trench can't attack each other
							distance = 0;
							break;
						case Black:
						case White:
							
							if(to.cellType==top.color) 
								{ distance = 0; 
								  // can't capture back into our own territory
								}
								else
								{ // attacking into enemy territory, don't need to stop
								  all.push(new Trenchmovespec(MOVE_ATTACK,from,to,who));
								}
							break;
						default: G.Error("Not expecting %s",to.cellType);
							break;
						}
						break;
						
					case White:
					case Black:
						distance = 0;
						if((to.cellType==TrenchId.Trench) && (from.cellType==top.color))
						{
							// can't attack the trench from own territory
						}
						else 
						{
							all.push(new Trenchmovespec(MOVE_CAPTURE,from,to,who));
						}
						break;
			default: G.Error("Not expecting %s",from.cellType);
				break;
				}
			}
		}
	 }
	 
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();

 	switch(board_state)
 	{
 	case Puzzle:
 		{int op = pickedObject==null ? MOVE_DROPB : MOVE_PICKB; 	
 			for(TrenchCell c = allCells;
 			 	    c!=null;
 			 	    c = c.next)
 			 	{	if(c.topChip()==null)
 			 		{all.addElement(new Trenchmovespec(op,c.col,c.row,whoseTurn));
 			 		}
 			 	}
 		}
 		break;
 	case Play:
 		addPieceMoves(all,whoseTurn);
 		if(drawIsLikely()
 				&& (robotStack.size()==0)
 				&& (moveNumber-lastDrawMove)>4)
 		{
 			all.push(new Trenchmovespec(MOVE_OFFER_DRAW,whoseTurn));
 		}
 		break;
 	case DrawPending:
 	case DeclinePending:
 	case Draw:
 	case AcceptPending:
 	case Confirm:
 		all.push(new Trenchmovespec(MOVE_DONE,whoseTurn));
 		break;
 	case Gameover:
 	case Resign:
 		break;
 	case AcceptOrDecline:
			 if(drawIsLikely()) { all.push(new Trenchmovespec(MOVE_ACCEPT_DRAW,whoseTurn)); }
			 all.push(new Trenchmovespec(MOVE_DECLINE_DRAW,whoseTurn));
			 break;

 	default:
 			G.Error("Not expecting state ",board_state);
 	}
 	return(all);
 }
 
 public void initRobotValues(TrenchPlay m)
 {	robot = m;

 }

 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(Character.isDigit(txt.charAt(0)))
	 	{ switch(variation)
	 		{
	 		case trench:
	 			xpos -= cellsize/4;
	 			ypos += cellsize/4;
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
 public Hashtable<TrenchCell, Trenchmovespec> getTargets() 
 {
 	Hashtable<TrenchCell,Trenchmovespec> targets = new Hashtable<TrenchCell,Trenchmovespec>();
 	CommonMoveStack all = GetListOfMoves();
 	for(int lim=all.size()-1; lim>=0; lim--)
 	{	Trenchmovespec m = (Trenchmovespec)all.elementAt(lim);
 		switch(m.op)
 		{
 		case MOVE_PICKB:
 		case MOVE_DROPB:
 			targets.put(getCell(m.to_col,m.to_row),m);
 			break;
 		case MOVE_SWAP:
 		case MOVE_DONE:
 		case MOVE_OFFER_DRAW:
 		case MOVE_ACCEPT_DRAW:
 		case MOVE_DECLINE_DRAW:
 			break;
 		case MOVE_FROM_TO:
 		case MOVE_ATTACK:
 		case MOVE_CAPTURE:
 			if(pickedObject==null) 
 				{ 
 				targets.put(getCell(m.from_col,m.from_row),m);
 				}
 			else
 			{
 				targets.put(getCell(m.to_col,m.to_row),m);
 			}
 			break;
 		default: G.Error("Not expecting %s",m);
 		
 		}
 	}
 	
 	return(targets);
 }
 
public int totalCaptured(int player)
{
	TrenchCell cap = captured(player);
	int sum = 0;
	for(int lim=cap.height()-1; lim>=0; lim--)
	{
		sum += cap.chipAtIndex(lim).type.distance;
	}
	return sum;
}
public double simpleScore(int player)
{
	double cap = totalCaptured(nextPlayer[player]);
	return cap;
}

public double simpleScoreTest(int player)
{	double trenchVisibleWeight = 0.001;
	double cap = totalCaptured(nextPlayer[player]);
	double trench = 0;
	CellStack occupied = occupied(player);
	TrenchId myColor = playerColor[player];
	for(int lim = occupied.size()-1;lim>=0; lim--)
	{
		TrenchCell c = occupied.elementAt(lim);
		TrenchId ctype = c.cellType;
		if(ctype==TrenchId.Trench)
		{
		}
		else if(ctype!=myColor)
		{	trench += trenchVisibleWeight*c.visibleFromTrench*c.topChip().type.distance;
		}
	}
	return cap+trench;
}

double trenchWeight = 0.05;
public double smartScore(int player)
{	int nextP = nextPlayer[player];

double cap = totalCaptured(nextP);
if(cap>=WIN) { return WIN*2; }
TrenchId myColor = playerColor[player];
double trenchVisibleWeight = 0.01;
double trenchAttackWeight = 0.01;
double intruderAttackWeight = 0.01;
double trench = 0;
int piecesInEnemy = 0;
int piecesInTrench = 0;
int sumInEnemy = 0;
int sumInTrench = 0;
int intruders = 0;
int sumIntruders = 0;
int enemyInTrench = 0;
int enemyTrenchWeight = 0;
{
CellStack occupied = occupied(player);
for(int lim = occupied.size()-1;lim>=0; lim--)
{
	TrenchCell c = occupied.elementAt(lim);
	TrenchId ctype = c.cellType;
	if(ctype==TrenchId.Trench)
	{
		piecesInTrench++;
		sumInTrench += c.topChip().type.distance;
	}
	else if(ctype!=myColor)
	{
		piecesInEnemy++;
		sumInEnemy += c.topChip().type.distance;
		trench += trenchVisibleWeight*c.visibleFromTrench*c.topChip().type.distance;
	}
}}

{
CellStack occupied = occupied(nextP);
for(int lim = occupied.size()-1;lim>=0; lim--)
{
	TrenchCell c = occupied.elementAt(lim);
	TrenchId ctype = c.cellType;
	if(ctype==TrenchId.Trench)
	{
		enemyInTrench++;
		enemyTrenchWeight += c.topChip().type.distance;
	}
	if(ctype==myColor)
	{
		intruders++;
		sumIntruders += c.topChip().type.distance;
	}
}}

cap -= intruderAttackWeight*sumIntruders*sumInTrench*piecesInTrench*intruders;
cap += intruderAttackWeight*sumInEnemy*enemyTrenchWeight*enemyInTrench*piecesInEnemy;
cap += trenchAttackWeight*sumInTrench;
cap += trench;

return cap;
}
public double smartScoreTest(int player)
{	int nextP = nextPlayer[player];

	double cap = totalCaptured(nextP);
	if(cap>=WIN) { return WIN*2; }
	TrenchId myColor = playerColor[player];
	double trenchVisibleWeight = 0.005;
	double trenchAttackWeight = 0.01;
	double intruderAttackWeight = 0.01;
	double trench = 0;
	int piecesInEnemy = 0;
	int piecesInTrench = 0;
	int sumInEnemy = 0;
	int sumInTrench = 0;
	int intruders = 0;
	int sumIntruders = 0;
	int enemyInTrench = 0;
	int enemyTrenchWeight = 0;
	{
	CellStack occupied = occupied(player);
	for(int lim = occupied.size()-1;lim>=0; lim--)
	{
		TrenchCell c = occupied.elementAt(lim);
		TrenchId ctype = c.cellType;
		if(ctype==TrenchId.Trench)
		{
			piecesInTrench++;
			sumInTrench += c.topChip().type.distance;
		}
		else if(ctype!=myColor)
		{
			piecesInEnemy++;
			sumInEnemy += c.topChip().type.distance;
			trench += trenchVisibleWeight*c.visibleFromTrench*c.topChip().type.distance;
		}
	}}
	
	{
	CellStack occupied = occupied(nextP);
	for(int lim = occupied.size()-1;lim>=0; lim--)
	{
		TrenchCell c = occupied.elementAt(lim);
		TrenchId ctype = c.cellType;
		if(ctype==TrenchId.Trench)
		{
			enemyInTrench++;
			enemyTrenchWeight += c.topChip().type.distance;
		}
		if(ctype==myColor)
		{
			intruders++;
			sumIntruders += c.topChip().type.distance;
		}
	}}
	
	cap -= intruderAttackWeight*sumIntruders*sumInTrench*piecesInTrench*intruders;
	cap += intruderAttackWeight*sumInEnemy*enemyTrenchWeight*enemyInTrench*piecesInEnemy;
	cap += trenchAttackWeight*sumInTrench;
	cap += trench;
	
	return cap;
}

//
// this is used by the UI to decide when to display the OFFERDRAW box
//
public boolean drawIsLikely()
{	switch(board_state)
	{
	case AcceptOrDecline:
	case DeclinePending:
	case DrawPending: return true;
	case Play:
		return( ((moveNumber - lastProgressMove)>10)
				&& (totalCaptured(whoseTurn)>10));
	default: return(false);
	}
	
}
public boolean canOfferDraw() {
	return (moveNumber-lastDrawMove>4);
}

 // most multi player games can't handle individual players resigning
 // this provides an escape hatch to allow it.
 //public boolean canResign() { return(super.canResign()); }
}
