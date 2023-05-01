package barca;

import static barca.Barcamovespec.*;

import com.codename1.ui.geom.Rectangle;
import bridge.Color;

import java.util.*;
import lib.*;
import lib.Random;
import online.game.*;

/**
 * BarcaBoard knows all about the game of Barca, which is played
 * on a 10x10 board. It gets a lot of logistic support from 
 * common.rectBoard, which knows about the coordinate system.  
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

class BarcaBoard extends rectBoard<BarcaCell> implements BoardProtocol,BarcaConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
    static final String[] BARCAGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	BarcaVariation variation = BarcaVariation.barca;
	private BarcaState board_state = BarcaState.Puzzle;	
	private BarcaState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	private CellStack moveStack = new CellStack();
	public BarcaState getState() { return(board_state); }
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	private void setState(BarcaState st) 
	{ 	unresign = (st==BarcaState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

// this is required even though it is meaningless for Hex, but possibly important
// in other games.  When a draw by repetition is detected, this function is called.
// the game should have a "draw pending" state and enter it now, pending confirmation
// by the user clicking on done.   If this mechanism is triggered unexpectedly, it
// is probably because the move editing in "editHistory" is not removing last-move
// dithering by the user, or the "Digest()" method is not returning unique results
// other parts of this mechanism: the Viewer ought to have a "repRect" and call
// DrawRepRect to warn the user that repetitions have been seen.
	public void SetDrawState() 
		{ setState(BarcaState.Draw);
		};	
	CellStack animationStack = new CellStack();
   // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public BarcaChip pickedObject = null;
    public BarcaChip lastPicked = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    int playerColor[] = AR.intArray(2);
    private CellStack filledCells[]={new CellStack(),new CellStack()};
    private BarcaCell holes[] = new BarcaCell[4];
    private BarcaState resetState = BarcaState.Puzzle; 
    public BarcaChip lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public BarcaCell newcell(char c,int r)
	{	return(new BarcaCell(BarcaId.BoardLocation,c,r));
	}
	
	// constructor 
    public BarcaBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = BARCAGRIDSTYLE;
        setColorMap(map);
        doInit(init,key,players,rev); // do the initialization
        autoReverseYNormal();		// reverse_y based on the color map
    }
    
    public String gameType() { return(gametype+" "+players_in_game+" "+randomKey+" "+revision); }
    
    // a very simple score based on the manhattan distance to the watering holes.
    // which are either empty or occupied by opponents
    public double scoreForPlayer(int pl)
    {	double val = 0;
    	CellStack from = filledCells[pl];
    	for(int lim = from.size()-1; lim>=0; lim--)
    	{
    		BarcaCell c = from.elementAt(lim);
    		BarcaChip animal = c.topChip();
    		BarcaId atype = animal.id;
    		double mindistance = 6;
    		for(BarcaCell hole : holes)
    		{	BarcaChip top = hole.topChip();
    			if((c==hole) || (top==null) || (top.colorIndex()!=playerColor[pl]))
    			{
    			// don't score holes that are occupied by our own other pieces
    			switch(atype)
    			{
    			default: throw G.Error("Not expecting %s",atype);
    			case Black_Lion:
    			case White_Lion:
    				{
    				if(c.isOdd()==hole.isOdd())	// lion can reach this hole
    				{
    				int dis = Math.max(Math.abs(c.col-hole.col),Math.abs(c.row-hole.row));
    				mindistance = Math.min(mindistance, dis);
    				}}
    				break;
    			case Black_Mouse:
    			case White_Mouse:
    				{
    				int dis = Math.abs(c.col-hole.col) + Math.abs(c.row-hole.row);
    				mindistance = Math.min(mindistance, dis);
    				}
    				break;
					
    			case White_Elephant:
    			case Black_Elephant:
    				{
    				int dis = Math.max(Math.abs(c.col-hole.col),Math.abs(c.row-hole.row));
    				mindistance = Math.min(mindistance, dis);
    				}
    				break;
    			}}
 
    		}
   			val += mindistance;
    	}
    	return(1.0-val/30.0);
    }   
    // a very simple score based on the manhattan distance to the watering holes.
    // which are either empty or occupied by opponents
    public double scoreForPlayer_2(int pl)
    {	double val = 0;
    	int scared = 0;
    	CellStack from = filledCells[pl];
    	for(int lim = from.size()-1; lim>=0; lim--)
    	{
    		BarcaCell c = from.elementAt(lim);
    		BarcaChip animal = c.topChip();
    		BarcaId atype = animal.id;
    		if(isAfraid(c,atype)) { scared++; }
    		double mindistance = 6;
    		for(BarcaCell hole : holes)
    		{	BarcaChip top = hole.topChip();
    			if((c==hole) || (top==null) || (top.colorIndex()!=playerColor[pl]))
    		{
    			// don't score holes that are occupied by our own other pieces
    			switch(atype)
    			{
    			default: throw G.Error("Not expecting %s",atype);
    			case Black_Lion:
    			case White_Lion:
    				{
    				if(c.isOdd()==hole.isOdd())	// lion can reach this hole
    				{
    				int dis = Math.max(Math.abs(c.col-hole.col),Math.abs(c.row-hole.row));
    				mindistance = Math.min(mindistance, dis);
    				}}
    				break;
    			case Black_Mouse:
    			case White_Mouse:
    				{
    				int dis = Math.abs(c.col-hole.col) + Math.abs(c.row-hole.row);
    				mindistance = Math.min(mindistance, dis);
    				}
    				break;
					
    			case White_Elephant:
    			case Black_Elephant:
    				{
    				int dis = Math.max(Math.abs(c.col-hole.col),Math.abs(c.row-hole.row));
    				mindistance = Math.min(mindistance, dis);
    				}
    				break;
    			}}
 
    		}
    		val += mindistance; 
    	}
    	val += scared*5;
    	double aval = 1.0-val/30.0;
    	return(aval);
    }
    public void doInit(String gtype,long key)
    {
    	StringTokenizer tok = new StringTokenizer(gtype);
    	String typ = tok.nextToken();
    	int np = tok.hasMoreTokens() ? G.IntToken(tok) : players_in_game;
    	long ran = tok.hasMoreTokens() ? G.IntToken(tok) : key;
    	int rev = tok.hasMoreTokens() ? G.IntToken(tok) : revision;
    	doInit(typ,ran,np,rev);
    }
    private int playerIndex(BarcaChip ch) { return(playerColor[ch.colorIndex()]); }
    
    private void drop(BarcaCell c,BarcaChip ch)
    {
    	c.addChip(ch);
    	filledCells[playerIndex(ch)].push(c);
    }
    static int holeLoc[][] = {{'D',4},{'D',7},{'G',4},{'G',7}};
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int players,int rev)
    {	randomKey = key;
    	adjustRevision(rev);
    	players_in_game = players;
		setState(BarcaState.Puzzle);
		variation = BarcaVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		moveStack.clear();
		gametype = gtype;
		AR.copy(playerColor,getColorMap());
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case barca:
			reInitBoard(10,10);
		}

	    // set the initial contents of the board to all empty cells
	    reInit(filledCells); 

	    drop(getCell('E',1),BarcaChip.White_Elephant);
		drop(getCell('F',1),BarcaChip.White_Elephant);
		drop(getCell('D',2),BarcaChip.White_Lion);
		drop(getCell('E',2),BarcaChip.White_Mouse);
		drop(getCell('F',2),BarcaChip.White_Mouse);
		drop(getCell('G',2),BarcaChip.White_Lion);

		drop(getCell('E',10),BarcaChip.Black_Elephant);
		drop(getCell('F',10),BarcaChip.Black_Elephant);
		drop(getCell('D',9),BarcaChip.Black_Lion);
		drop(getCell('E',9),BarcaChip.Black_Mouse);
		drop(getCell('F',9),BarcaChip.Black_Mouse);
		drop(getCell('G',9),BarcaChip.Black_Lion);

		for(int i=0;i<holeLoc.length;i++)
		{ holes[i] = getCell((char)holeLoc[i][0],holeLoc[i][1]);
		}

		whoseTurn = FIRST_PLAYER_INDEX;
	    droppedDestStack.clear();
	    pickedSourceStack.clear();
	    stateStack.clear();
	    
	    pickedObject = null;
	    resetState = null;
	    lastDroppedObject = null;
        animationStack.clear();
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }

    /** create a copy of this board */
    public BarcaBoard cloneBoard() 
	{ BarcaBoard dup = new BarcaBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((BarcaBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(BarcaBoard from_b)
    {
        super.copyFrom(from_b);
        robotState.copyFrom(from_b.robotState);
        getCell(filledCells,from_b.filledCells);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        stateStack.copyFrom(from_b.stateStack);
        getCell(moveStack,from_b.moveStack);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        lastPicked = null;


        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((BarcaBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(BarcaBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(sameCells(moveStack,from_b.moveStack),"movestack mismatch");
        G.Assert(filledCells[0].size()==from_b.filledCells[0].size(),"filledCells 0 mismatch");
        G.Assert(filledCells[1].size()==from_b.filledCells[1].size(),"filledCells 1 mismatch");

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
        long v = super.Digest(r);
		// many games will want to digest pickedSource too
		// v ^= cell.Digest(r,pickedSource);
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,filledCells[0].size());
		v ^= Digest(r,filledCells[1].size());
		v ^= Digest(r,revision);
		v ^= Digest(r,moveStack);
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
        	throw G.Error("Move not complete, can't change the current player");
        case Puzzle:
            break;
        case Play:
        	// some damaged games have 2 dones in a row
        	if(replay==replayMode.Live) { throw G.Error("Move not complete, can't change the current player"); }
			//$FALL-THROUGH$
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


    public boolean winForPlayerNow(int player)
    {	return(win[player]);
    }



    //
    // accept the current placements as permanent
    //
    private void acceptPlacement()
    {	
        droppedDestStack.clear();
        pickedSourceStack.clear();
        stateStack.clear();
        pickedObject = null;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private BarcaCell unDropObject()
    {	BarcaCell rv = droppedDestStack.pop();
    	setState(stateStack.pop());
    	pickedObject = rv.removeTop(); 	// SetBoard does ancillary bookkeeping
    	filledCells[playerIndex(pickedObject)].remove(rv,false);
    	rv.lastPlaced = previousLastPlaced;    	
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	BarcaCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	filledCells[playerIndex(pickedObject)].push(rv);
    	rv.addChip(pickedObject);
    	rv.lastEmptied = previousLastEmptied;
    	rv.lastEmptiedPlayer = previousLastPlayer;
    	pickedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(BarcaCell c)
    {
       droppedDestStack.push(c);
       stateStack.push(board_state);
       
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("not expecting dest %s", c.rackLocation);
        case BoardLocation:	// already filled board slot, which can happen in edit mode
           	filledCells[playerIndex(pickedObject)].push(c);
           	c.addChip(pickedObject);
            lastDroppedObject = pickedObject;
            previousLastPlaced = c.lastPlaced;
            c.lastPlaced = moveNumber;
            pickedObject = null;
            break;
        }
     }
    private int previousLastPlaced = 0;
    private int previousLastEmptied = 0;
    private int previousLastPlayer = 0;
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(BarcaCell c)
    {	return(droppedDestStack.top()==c);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { BarcaChip ch = pickedObject;
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
    private BarcaCell getCell(BarcaId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	return(getCell(col,row));
        } 	
    }
    public BarcaCell getCell(BarcaCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(BarcaCell c)
    {	pickedSourceStack.push(c);
    	stateStack.push(board_state);
        switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting rackLocation %s", c.rackLocation);
        case BoardLocation:
        	{
            lastPicked = pickedObject = c.removeTop();
         	lastDroppedObject = null;
			filledCells[playerIndex(pickedObject)].remove(c, false);
			previousLastEmptied = c.lastEmptied;
			previousLastPlayer = c.lastEmptiedPlayer;
			c.lastEmptied = moveNumber;
			c.lastEmptiedPlayer = whoseTurn;
        	}
            break;

        }
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(BarcaCell c)
    {	return(c==pickedSourceStack.top());
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case Confirm:
        	setNextStateAfterDone(replay);
         	break;
        case Play:
        case Escape:
			setState(BarcaState.Confirm);
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
    	default: throw G.Error("Not expecting after Done state %s",board_state);
    	case Draw:
    		setState(BarcaState.Gameover);
    		break;
    	case Gameover: break;
    	case Confirm:
    	case Puzzle:
    	case Play:
    	{	int win0 = 0;
	    	int win1 = 0;
	    	for(BarcaCell c : holes)
	    	{
	    		BarcaChip top = c.topChip();
	    		if(top!=null)
	    		{
	    			switch(playerIndex(top))
	    			{
	    			default: break;
	    			case 0: win0++; break;
	    			case 1: win1++; break;
	    			}
	    		}
	    	}
	    	if(win0>=3) { win[0] = true; setState(BarcaState.Gameover); }
	    	else if(win1>=3) { win[1] = true; setState(BarcaState.Gameover); }
	    	else 
	    	{ 	CellStack stack = filledCells[whoseTurn];
	    		boolean afraid = false;
	    		for(int lim = stack.size()-1; !afraid && lim>=0; lim--)
	    		{	BarcaCell c = stack.elementAt(lim);
	    			BarcaChip top = c.topChip();
	    			afraid |= isAfraid(c,top.id);
	    		}
	    		setState( afraid ? BarcaState.Escape : BarcaState.Play); 
	    	}
    	
    	}  
    
    		break;
    	}
       	resetState = board_state;
    }
    private void doDone(replayMode replay)
    {
        acceptPlacement();

        if (board_state==BarcaState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(BarcaState.Gameover);
        }
        else
        {	if(winForPlayerNow(whoseTurn)) 
        		{ win[whoseTurn]=true;
        		  setState(BarcaState.Gameover); 
        		}
        	else {setNextPlayer(replay);
        		setNextStateAfterDone(replay);
        	}
        }
    }

	
    public boolean Execute(commonMove mm,replayMode replay)
    {	Barcamovespec m = (Barcamovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }

        //G.print("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {

        case MOVE_DONE:

         	doDone(replay);

            break;
        case MOVE_FROM_TO:
        	{
        	BarcaCell from = getCell(m.from_col,m.from_row);
        	BarcaCell to = getCell(m.to_col,m.to_row);
        	pickObject(from);
        	m.target = pickedObject;
        	dropObject(to);
        	acceptPlacement();
        	setState(BarcaState.Confirm);
        	if(replay!=replayMode.Replay)
        	{
        		animationStack.push(from);
        		animationStack.push(to);
        	}
        	}
        	break;
        case MOVE_DROPB:
        	{
			BarcaChip po = pickedObject;
			BarcaCell dest =  getCell(m.to_col,m.to_row);
			
			if(isSource(dest)) 
				{ unPickObject(); 
				}
				else 
				{

	            /**
	             * if the user clicked on a board space without picking anything up,
	             * animate a stone moving in from the pool.  For Hex, the "picks" are
	             * removed from the game record, so there are never picked stones in
	             * single step replays.
	             */
	            if((replay==replayMode.Single) && (po!=null))
	            	{ animationStack.push(pickedSourceStack.top());
	            	  animationStack.push(dest); 
	            	}
	            dropObject(dest);
	            setNextStateAfterDrop(replay);
				}
        	}
             break;

 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
 			BarcaCell src = getCell(m.from_col,m.from_row);
 			if(isDest(src)) { unDropObject(); }
 			else
 			{
        	// be a temporary p
        	pickObject(src);
        	m.target = pickedObject;
        	switch(board_state)
        	{
        	case Puzzle:
         		break;
        	case Confirm:
        		setState(BarcaState.Play);
        		break;
        	default: ;
        	}}}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            int nextp = nextPlayer[whoseTurn];
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(BarcaState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ setState(BarcaState.Gameover); 
               	}
            else {  setNextStateAfterDone(replay); }

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?BarcaState.Resign:unresign);
            break;
        case MOVE_EDIT:
        	acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(BarcaState.Puzzle);
 
            break;

		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(BarcaState.Gameover);
			break;
        default:
        	cantExecute(m);
        }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+board_state+" "+this);
        return (true);
    }


    public boolean LegalToHitBoard(BarcaCell c,Hashtable<BarcaCell,commonMove>targets)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
		case Play:
		case Escape:
			return(isSource(c) || (targets.get(c)!=null));
		case Gameover:
		case Resign:
			return(false);
		case Confirm:
		case Draw:
			return(isDest(c));
        default:
        	throw G.Error("Not expecting Hit Board state %s", board_state);
        case Puzzle:
            return ((pickedObject==null)!=c.isEmpty());
        }
    }
    
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Barcamovespec m)
    {
        robotState.push(board_state); //record the starting state. The most reliable
        // to undo state transitions is to simple put the original state back.
        
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

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
    public void UnExecute(Barcamovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	BarcaState state = robotState.pop();
        switch (m.op)
        {
   	    default:
   	    	throw G.Error("Can't un execute %s", m);
        case MOVE_DONE:
            break;
        case MOVE_FROM_TO:
        	{
        	BarcaCell from = getCell(m.from_col,m.from_row);
        	BarcaCell to = getCell(m.to_col,m.to_row);
        	pickObject(to);
        	dropObject(from);
        	acceptPlacement();
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
 private boolean addMovesInDirection(CommonMoveStack all,BarcaCell c,BarcaChip top,int direction,boolean trapped,int who)
 {
	 boolean some = false;
	 BarcaCell next = c;
	 while( (next=next.exitTo(direction))!=null)
	 {	if(next.isEmpty())
	 	{
		 if(trapped || !isAfraid(next,top.id))
		 {
			 if(all==null) { return(true); }
			 all.push(new Barcamovespec(MOVE_FROM_TO,c,next,who));
			 some = true;
		 }
	 	}
	 else { break; }	// not an empty, ends
	 }
	 return(some);
 }
 private boolean addMoves(CommonMoveStack all,BarcaCell c,BarcaChip top,boolean trapped,int who)
 {	boolean some = false;
 	switch(top.id)
 	{
 	default: throw G.Error("Not expecting %s",top);
 	case White_Elephant:	// any direction moves
 	case Black_Elephant:
 		for(int direction=c.geometry.n; direction>0;direction--)
 		{
 			some |= addMovesInDirection(all,c,top,direction,trapped,who);
 		}
 		break;
 	case White_Lion:	// diagonal moves
 	case Black_Lion:
 		for(int direction=CELL_UP_LEFT,limit=CELL_UP_LEFT+c.geometry.n;direction<limit;direction+=CELL_QUARTER_TURN)
 		{
 			some |= addMovesInDirection(all,c,top,direction,trapped,who);
 		}
 		break;
 	
 	case White_Mouse:	// othogonal moves
 	case Black_Mouse:
 		for(int direction=CELL_UP,limit=CELL_UP+c.geometry.n;direction<limit;direction+=CELL_QUARTER_TURN)
 		{
 			some |= addMovesInDirection(all,c,top,direction,trapped,who);
 		}
 		break;
		
 	}
 	return(some);
 }
 public boolean isAfraid(BarcaCell c,BarcaId top)
 {	for(int dir = c.geometry.n; dir>0; dir--)
 	{ BarcaCell adj = c.exitTo(dir);
 	  if(adj!=null)
 	  {
 	  BarcaChip atop = adj.topChip();
 	  if(atop!=null)
 		  {
 		  if(top.afraidOf(atop.id)) { return(true); }  
 		  }
 	  }
 	}
 	return(false);
 }
 public Hashtable<BarcaCell,commonMove> getTargets()
 {
	 Hashtable<BarcaCell,commonMove> targets = new Hashtable<BarcaCell,commonMove>();
	 switch(board_state)
	 {
	 default: break;
	 case Confirm: break;
	 case Play:
	 case Escape:
		 CommonMoveStack all = GetListOfMoves();
		 while(all.size()>0)
		 {
			 Barcamovespec m = (Barcamovespec)all.pop();
			 if(m.op==MOVE_FROM_TO)
			 {
				 if(pickedObject==null)
				 {
					 targets.put(getCell(m.from_col,m.from_row),m);
				 }
				 else { targets.put(getCell(m.to_col,m.to_row),m);}
			 }
		 }
	 }
	 return(targets);
 }
 private boolean addEscapeMoves(CommonMoveStack all,BarcaCell aLoc,BarcaChip animal,boolean trapped,int who)
 {
	 if(isAfraid(aLoc,animal.id))
	 {
	 return(addMoves(all,aLoc,animal,trapped,whoseTurn));
	 }
	 return(false);
 }
 private boolean addEscapeMoves(CommonMoveStack all,boolean trapped,int who)
 {	CellStack filled = filledCells[whoseTurn];
 	boolean some = false;
 	for(int lim = filled.size()-1; lim>=0; lim--)
	 	{	BarcaCell aLoc = filled.elementAt(lim);
	 		some |= addEscapeMoves(all,aLoc,aLoc.topChip(),trapped,who);
	 	}
 	if(!some && !trapped)
 	{	// an afraid animal with no moves can move into a feared location
 		addEscapeMoves(all,true,who);
 	}
 	return(some);
 }
 private boolean addMoves(CommonMoveStack all,BarcaCell aLoc,BarcaChip animal,int who)
 {	
 	return addMoves(all,aLoc,animal,false,whoseTurn);

 }
 private boolean addMoves(CommonMoveStack all,int who)
 {	CellStack filled = filledCells[whoseTurn];
 	boolean some = false;
 	for(int lim = filled.size()-1; lim>=0; lim--)
	 	{	BarcaCell aLoc = filled.elementAt(lim);
	 		some |= addMoves(all,aLoc,aLoc.topChip(),who);
	 	}
 	return(some);
 }
 
 public commonMove getRandomMove(Random r)
 {
	 switch(board_state)
	 {
	 case Play:
		 // in play mode, choose a random animal
		 CellStack filled = filledCells[whoseTurn];
		 BarcaCell from = filled.elementAt(r.nextInt(filled.size()));
		 CommonMoveStack all = new CommonMoveStack();
		 addMoves(all,from,from.topChip(),whoseTurn);
		 int asize = all.size();
		 if(asize>0) 
		 	{ return(all.elementAt(r.nextInt(asize))); 	 	
		 	}
		 break;
	 default: break;
	 }
	 return(null);
 }
 public CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	
 	switch(board_state)
 	{
 	case Play:
 		if(pickedObject==null) { addMoves(all,whoseTurn); }
 		else { addMoves(all,pickedSourceStack.top(),pickedObject,whoseTurn); }
 		break;
 	case Escape:
 		if(pickedObject==null) 
 		{ if(!addEscapeMoves(all,false,whoseTurn))
 			{
 				addMoves(all,whoseTurn);
 			}
 		}
 		else { 
 			if(!addEscapeMoves(all,pickedSourceStack.top(),pickedObject,false,whoseTurn))
 			{
 				addMoves(all,pickedSourceStack.top(),pickedObject,false,whoseTurn);
 			}
 		}
 		break;
 	default: G.Error("Not expecting state %s",board_state);
 	}

 	return(all);
 }
	public int positionToX(BarcaCell c)
	{	BarcaCell c2 = displayParameters.reverse_y ? getCell((char)('A'+('A'+ncols)-c.col-1),nrows-c.row+1) : c;
		return((int)(((G.Width(boardRect)*c2.xloc))));
	}
	public void SetDisplayRectangle(Rectangle r)
	{	super.SetDisplayRectangle(r);
		boardRect = r;
	}
	public int cellToX(BarcaCell c,boolean perspective) 
	{	if(perspective)
	{
		if((c!=null)&&(boardRect!=null))
		{	
			return(positionToX(c));
		}
		return(0);
	}
		else {
		return(super.cellToX(c));
		}
	}
	public int positionToY(BarcaCell c)
	{	BarcaCell c2 = displayParameters.reverse_y ? getCell((char)('A'+('A'+ncols)-c.col-1),nrows-c.row+1) : c;
		return((int)((G.Height(boardRect)*c2.yloc)));
	}
	public int cellToY(BarcaCell c,boolean perspective) 
	{	if(perspective)
	{
		if((c!=null)&&(boardRect!=null))
		{
		return(positionToY(c));
		}
		return 0;
		}
		else { 
			return(super.cellToY(c));
		}
	}
	 // small ad-hoc adjustment to the grid positions
	 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
	 {   if(Character.isDigit(txt.charAt(0)))
	 		{
		 	ypos -= cellsize/10; 
	 		}
	 		else
	 		{ 
	 		  ypos += cellsize/3;
	 		  xpos -= cellsize/5;
	 		}
	 	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
	 }
};
