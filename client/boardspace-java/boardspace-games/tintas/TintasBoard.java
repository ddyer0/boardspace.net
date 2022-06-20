package tintas;

import static tintas.Tintasmovespec.*;

import java.awt.Color;
import java.util.*;
import lib.*;
import lib.Random;
import online.game.*;
import online.game.cell.Geometry;


/**
 * TintasBoard knows all about the game of Tintas, which is played
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

class TintasBoard extends hexBoard<TintasCell> implements BoardProtocol,TintasConstants
{	static int REVISION = 101;			// 100 represents the initial version of the game
										// 101 enables swap and proper name of sgf game
	boolean ENABLE_SWAP = REVISION>100;	// this enables making swap moves, they are handled even if they can't be made
	public int getMaxRevisionLevel() { return(REVISION); }
	TintasVariation variation = TintasVariation.tintas;
	private TintasState board_state = TintasState.Puzzle;	
	private TintasState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	public CellStack pawnStack = new CellStack();
	private int sweep_counter = 0;
	private boolean swapped = false;
	public TintasCell[][] captures = new TintasCell[2][TintasChip.Chips.length];
	public TintasCell pawnHome = new TintasCell(TintasId.PawnHome);
	public CellStack capturedStack = new CellStack();
	public TintasState getState() { return(board_state); }
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(TintasState st) 
	{ 	unresign = (st==TintasState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
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
	// intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public TintasChip pickedObject = null;
    public TintasChip lastPicked = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    
    public TintasChip lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public TintasCell newcell(char c,int r)
	{	return(new TintasCell(TintasId.BoardLocation,c,r));
	}
	
	// constructor 
    public TintasBoard(String init,int players,long key,int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = TintasGRIDSTYLE;
        char col='A';
        for(TintasCell row[] : captures)
        {	
        	for(int i=0;i<row.length;i++)
        	{
        		row[i] = new TintasCell(TintasId.RackLocation,Geometry.Standalone,col,i);
        	}
        	col++;
        }
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
    	pawnHome.reInit();
    	pawnHome.addChip(TintasChip.Pawn);
    	pawnStack.clear();
    	pawnStack.push(pawnHome);
    	reInit(captures);
 		Random r = new Random(734687);	// this random is used to assign hash values to cells, common to all games of this type.
		setState(TintasState.Puzzle);
		variation = TintasVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		gametype = gtype;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case tintas:
			initBoard(variation.firstInCol,variation.ZinCol,null);

			
		}

		allCells.setDigestChain(r);		// set the randomv for all cells on the board
	    	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    droppedDestStack.clear();
	    pickedSourceStack.clear();
	    stateStack.clear();
	    capturedStack.clear();
	    swapped = false;
	    pickedObject = null;
	    lastDroppedObject = null;

		
	    // set the initial contents of the board 
	    int maxSize = 0;
	    do {
		ChipStack all = new ChipStack();
		for(TintasChip ch : TintasChip.Chips)
		{
			for(int i=0;i<ChipsPerColor;i++) { all.push(ch);};
		}
		all.shuffle(new Random(key));

		for(TintasCell c = allCells; c!=null; c=c.next)
		{	c.reInit();
			c.addChip(all.pop());
		}    
		maxSize =0;
		sweep_counter++;
		for(TintasCell c = allCells; c!=null; c=c.next)
		 {	maxSize = Math.max(maxSize,blobSize(c,c.topChip(),sweep_counter));
		 }    
	    } 
	    while(maxSize>=ChipsPerColor);
   
        animationStack.clear();
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }

    /** create a copy of this board */
    public TintasBoard cloneBoard() 
	{ TintasBoard dup = new TintasBoard(gametype,players_in_game,randomKey,revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((TintasBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(TintasBoard from_b)
    {
        super.copyFrom(from_b);
        pickedObject = from_b.pickedObject;
        swapped = from_b.swapped;
        unresign = from_b.unresign;
        getCell(pawnStack,from_b.pawnStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(capturedStack,from_b.capturedStack);
        copyFrom(captures,from_b.captures);
        copyFrom(pawnHome,from_b.pawnHome);
        board_state = from_b.board_state;
        stateStack.copyFrom(from_b.stateStack);
        lastPicked = null;

 
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((TintasBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(TintasBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(swapped==from_b.swapped, "swapped mismatch");
        G.Assert(sameCells(pawnStack,from_b.pawnStack), "pawnlocation mismatch");
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(sameCells(pawnHome,from_b.pawnHome),"pawnhome mismatch");
        G.Assert(sameCells(capturedStack,from_b.capturedStack),"capturedStack mismatch");
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
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,revision);
		v ^= Digest(r,swapped);
		v ^= Digest(r,pawnStack);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,capturedStack);
		v ^= Digest(r,captures);
		v ^= Digest(r,pawnHome);
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
        case PlayOrSwap:
        case FirstPlay:
        	// some damaged games have 2 dones in a row
        	if(replay==replayMode.Live) { throw G.Error("Move not complete, can't change the current player"); }
			//$FALL-THROUGH$
        case Confirm:
        case ConfirmFinal:
        case ConfirmSwap:
        case ContinuePlay:
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



    public boolean gameOverNow() { return(board_state.GameOver()); }
    public boolean winForPlayerNow(int player)
    {	if(win[player]) { return(true); }
    	int fourOrMore = 0;
    	boolean zero = false;
    	for(TintasCell c : captures[player])
    	{	int h = c.height();
    		if(h==ChipsPerColor) { return(true); }
    		else if(h==0) { zero = true; }
    		else if(h>ChipsPerColor/2) { fourOrMore+=2; }
    		
    	}
    	if(!zero && (fourOrMore>=TintasChip.nColors))
    	{	// win by 4 4's when all 7's is no longer possible
    		return(true);
    	}
    	return(false);
    }

    public int blobSize(TintasCell center,TintasChip color,int sweep)
    {	int size = 0;
    	if((center!=null) && (center.sweep_counter<sweep) && (center.topChip()==color))
    	{
    		center.sweep_counter = sweep;
    		size++;
    		for(int dir=center.geometry.n-1; dir>=0; dir--)
    		{
    			size += blobSize(center.exitTo(dir),color,sweep);
    		}
    	}
    	return(size);
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
    private TintasCell unDropObject(replayMode replay)
    {	TintasCell rv = droppedDestStack.pop();
    	setState(stateStack.pop());
    	TintasCell cc = capturedStack.top();
    	switch(rv.rackLocation())
    	{
    	default: throw G.Error("not expecting %s",rv);
    	case RackLocation:
    	case PawnHome:
       	case BoardLocation:
    		pickedObject = rv.removeTop();
    		switch(board_state)
    		{
    		case Play:
    		case PlayOrSwap:
    		case FirstPlay:
    			capturedStack.pop();
				//$FALL-THROUGH$
			case ContinuePlay:
    			rv.addChip(cc.removeTop());
    			if(replay!=replayMode.Replay)
    			{
    				animationStack.push(cc);
    				animationStack.push(rv);
    			}
    			if(pickedObject==TintasChip.Pawn)  { pawnStack.pop(); }
    			break;
    		default: break;
    		}
    	}
    	return(rv);

    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	TintasCell rv = pickedSourceStack.pop();
    	switch(rv.rackLocation())
    	{
    	default: throw G.Error("not expecting %s",rv);
    	case RackLocation:
    	case PawnHome:
    	case BoardLocation:
    		rv.addChip(pickedObject);
    		pickedObject = null;
    		break;
    	}
    	setState(stateStack.pop());
     }
    
    // 
    // drop the floating object.
    //
    private void dropObject(TintasCell c,replayMode replay)
    {
       droppedDestStack.push(c);
       stateStack.push(board_state);
       
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("not expecting dest %s", c.rackLocation);
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        	if((pickedObject==TintasChip.Pawn) && (c.topChip()!=null))
        	{
        		TintasChip cap = c.removeTop();
        		TintasCell moveTo = captures[whoseTurn][cap.chipNumber()];
        		switch(board_state)
        		{
        		default: break;
        		case Play:
        		case PlayOrSwap:
        		case FirstPlay: capturedStack.push(moveTo);
        		}
        		moveTo.addChip(cap);
        		if(replay!=replayMode.Replay)
        		{
        			animationStack.push(c);
        			animationStack.push(moveTo);
        		}
        	}
        //$FALL-THROUGH$
        case RackLocation:
        case PawnHome:
        	c.addChip(pickedObject);
           	if(pickedObject==TintasChip.Pawn) { pawnStack.push(c); }
        	lastDroppedObject = pickedObject;
        	pickedObject = null;
        	break;
        }
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(TintasCell c)
    {	return(droppedDestStack.top()==c);
    }
    public boolean isADest(TintasCell c)
    {
    	for(int lim = droppedDestStack.size()-1; lim>=0; lim--)
    	{
    		if(c==droppedDestStack.elementAt(lim)) { return(true);}
    	}
    	return(false);
    }
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { TintasChip ch = pickedObject;
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
    private TintasCell getCell(TintasId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case PawnHome:
        	return(pawnHome);
        case RackLocation:
        	return(captures[col-'A'][row]);
        case BoardLocation:
        	return(getCell(col,row));

        } 	
    }
    public TintasCell getCell(TintasCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(TintasCell c)
    {	pickedSourceStack.push(c);
    	stateStack.push(board_state);
        switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting rackLocation %s", c.rackLocation);
        case RackLocation:
        case PawnHome:
        case BoardLocation:
        	pickedObject = c.removeTop();
        	break;

        }
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(TintasCell c)
    {	return(c==pickedSourceStack.top());
    }
    public boolean isASource(TintasCell c)
    {	for(int i=pickedSourceStack.size()-1; i>=0; i--)
    	{
    	if(c==pickedSourceStack.elementAt(i)) { return(true); }
    	}
    	return(false);
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
        case ContinuePlay:
			setState(addPawnMoves(null,capturedStack.top().topChip(),whoseTurn) 
					? TintasState.ContinuePlay
					: TintasState.ConfirmFinal);
			break;
        case FirstPlay:
        	setState(TintasState.Confirm);
        	break;
        case Play:
        case PlayOrSwap:
			setState(addPawnMoves(null,capturedStack.top().topChip(),whoseTurn) 
						? TintasState.ContinuePlay
						: TintasState.Confirm);
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
    	case Gameover: break;
    	case Puzzle:
    	case Confirm:
    	case ConfirmFinal:   	
    	case ConfirmSwap:
    	case ContinuePlay:
    		
    		setState((ENABLE_SWAP && (capturedStack.size()==1) && !swapped) 
    					? TintasState.PlayOrSwap
    					: addPawnMoves(null,null,whoseTurn)
    						?TintasState.Play
    						:TintasState.FirstPlay);
    		break;
    	}
    }
    private void doDone(replayMode replay)
    {
        acceptPlacement();

        if (board_state==TintasState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(TintasState.Gameover);
        }
        else
        {	if(winForPlayerNow(whoseTurn)) 
        		{ win[whoseTurn]=true;
        		  setState(TintasState.Gameover); 
        		}
        	else {setNextPlayer(replay);
        		if(winForPlayerNow(whoseTurn)) 
        			{ win[whoseTurn]=true;
        			  setState(TintasState.Gameover);
        			}
        		else { setNextStateAfterDone(replay); }
        	}
        }
    }

	private void unwindOnDrop(TintasCell dest,replayMode replay)
	{
		while((board_state!=TintasState.Puzzle) 
				&& (replay==replayMode.Live)
				&& (isADest(dest) || isASource(dest)))
		{
			unPickObject();
			if(droppedDestStack.size()>0) { unDropObject(replay); }
		}
	}
	private void doSwap()
	{	TintasCell from = capturedStack.pop();
		TintasCell to = captures[whoseTurn][from.row];
		if(from==to) { to = captures[nextPlayer[whoseTurn]][from.row]; }
		capturedStack.push(to);
		copyFrom(to,from);
		swapped = !swapped;
		from.reInit();
	}
    public boolean Execute(commonMove mm,replayMode replay)
    {	Tintasmovespec m = (Tintasmovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }

        //G.print("E "+m);
        switch (m.op)
        {
        case MOVE_SWAP:
        	{
        	if(board_state==TintasState.ConfirmSwap)
	        	{
        		doSwap();
	    		setState(stateStack.pop());
	        	}
        	else
        		{ 
        		stateStack.push(board_state); 
        		doSwap();
        		setState(TintasState.ConfirmSwap);
        		}
        	}
        	break;
        case MOVE_DONE:

         	doDone(replay);

            break;
        case MOVE_PAWN:
		case MOVE_DROPB:
        	{
        	TintasChip po = pickedObject;
            if(pickedObject==null) { pickObject(pawnStack.top()); }
			TintasCell dest =  getCell(TintasId.BoardLocation,m.to_col,m.to_row);
			
			unwindOnDrop(dest,replay);
			
			if(pickedObject!=null)
				{if(isSource(dest)) 
				{ unPickObject(); 
				}
				else 
				{
	            dropObject(dest,replay);
	            switch(board_state)
	            {
	            case FirstPlay:
	            case PlayOrSwap:
	            case Play:
	            	// record for the viewer
	            	m.target = capturedStack.top().topChip();
	            	break;
	            default: break;
	            }
	            /**
	             * if the user clicked on a board space without picking anything up,
	             * animate a stone moving in from the pool.  For Hex, the "picks" are
	             * removed from the game record, so there are never picked stones in
	             * single step replays.
	             */
	            if(replay!=replayMode.Replay && (po==null))
	            	{ animationStack.push(pickedSourceStack.top());
	            	  animationStack.push(dest); 
	            	}
	            setNextStateAfterDrop(replay);
				}}
        	}
             break;

        case MOVE_PICK:
 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
 			TintasCell src = getCell(m.dest,m.to_col,m.to_row);
 			if(isDest(src) && (board_state!=TintasState.ContinuePlay)) { unDropObject(replay); }
 			else
 			{
        	// be a temporary p
        	pickObject(src);
        	switch(board_state)
        	{
        	case Puzzle:
         		break;
        	case ConfirmFinal:
        		setState(TintasState.ContinuePlay);
        		break;
        	case ConfirmSwap:
        	case Confirm:
        		setState(TintasState.Play);
        		break;
        	default: ;
        	}}}
            break;

        case MOVE_DROP: // drop on chip pool;
        	
        	{
            TintasCell dest = getCell(m.dest,m.to_col,m.to_row);
            unwindOnDrop(dest,replay);
            if(pickedObject!=null)
            {
            if(isSource(dest)) { unPickObject(); }
            else { dropObject(dest,replay); }
        	}}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            int nextp = nextPlayer[whoseTurn];
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(TintasState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ setState(TintasState.Gameover); 
               	}
            else {  setNextStateAfterDone(replay); }

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?TintasState.Resign:unresign);
            break;
        case MOVE_EDIT:
        	acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(TintasState.Puzzle);
 
            break;
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(TintasState.Gameover);
			break;
       	

        default:
        	cantExecute(m);
        }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips(TintasCell c)
    {
        switch (board_state)
        {
        default:
        	return(false);
        case Puzzle:
        	TintasChip top = c.topChip();
            return ( (pickedObject==null) ? (top!=null) : (top==null) || (top==pickedObject));
        }
    }

    public boolean LegalToHitBoard(TintasCell c)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
        case ContinuePlay:
        	return(isDest(c)|| isASource(c)||(c==pawnStack.top()) || isValidPawnMove(c,capturedStack.top().topChip()) );
        case Play:
        case FirstPlay:
        case PlayOrSwap:
        	return(isDest(c)|| isASource(c) || (c==pawnStack.top()) || isValidPawnMove(c,null) );
		case Gameover:
		case Resign:
		case ConfirmSwap:
			return(false);
		case Confirm:
		case ConfirmFinal:
			return(isDest(c));
        default:
        	throw G.Error("Not expecting Hit Board state %s", board_state);
        case Puzzle:
            return ((pickedObject==null) == (c.topChip()!=null));
        }
    }
    
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Tintasmovespec m)
    {	//G.print("R "+m);
        robotState.push(board_state); //record the starting state. The most reliable        
        Execute(m,replayMode.Replay);
        acceptPlacement();
        
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(Tintasmovespec m)
    {
    	//G.print("U "+m);
    	TintasState state = robotState.pop();
    	TintasCell endCap = capturedStack.top();
        switch (m.op)
        {
        case MOVE_SWAP:
        	doSwap();
        	break;
   	    default:
   	    	throw G.Error("Can't un execute %s", m);
   	    case MOVE_PAWN:
   	    	{	TintasCell dest = getCell(m.dest,m.to_col,m.to_row);
   	    		TintasChip pawn = dest.removeTop();
   	    		G.Assert(pawn==TintasChip.Pawn,"should be the pawn");
   	    		G.Assert(dest.isEmpty(),"should be empty");
   	    		dest.addChip(endCap.removeTop());
   	    		switch(state)
   	    		{
   	    		default: break;
   	    		case Play:
        		case PlayOrSwap:
   	    		case FirstPlay:
   	    			capturedStack.pop();
   	    		}
   	    		pawnStack.pop();
   	    		pawnStack.top().addChip(pawn);
   	    	}
   	    	break;
        case MOVE_DONE:
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
 private boolean addPawnMoves(CommonMoveStack all,TintasCell from,TintasChip color,int dir,int who)
 {
	 while((from=from.exitTo(dir))!=null)
	 {
		 if(!from.isEmpty())
		 { 	 if((color==null) ? true :  (color==from.topChip()))
		 	{
			 if(all!=null) 
			 {
			 all.push(new Tintasmovespec(MOVE_PAWN,from,who));
			 }
			 return(true);
		 	}
		 	else { return(false); }	// not matching color
		 }
	 }
	 return(false);
 }
 private boolean addPawnMoves(CommonMoveStack all,TintasChip color,int who)
 {	boolean some = false;
	if(pawnStack.top().onBoard)
	{
		TintasCell c = pawnStack.top();
		for(int dir = c.geometry.n-1; dir>=0; dir--)
		{
			some |= addPawnMoves(all,c,color,dir,whoseTurn);
			if(some && (all==null)) 
				{ return(some); 
				}
		}
	}
	return(some);
 }

 private boolean addAnyPawnMoves(CommonMoveStack all,int who)
 {	boolean some = false;
	for(TintasCell c = allCells; c!=null; c=c.next)
	{
		if(!c.isEmpty() && (c!=pawnStack.top()))
		{
			if(all==null) { return(true); }
			all.push(new Tintasmovespec(MOVE_PAWN,c,who));
		}
	}
	return(some);
 }
 private boolean isValidPawnMove(TintasCell c,TintasChip color)
 {
	 if(pawnStack.top().onBoard)
	 {	CommonMoveStack all = new CommonMoveStack();
	 	if(addPawnMoves(all,color,whoseTurn))
	 	{
	 		do {
	 			Tintasmovespec m = (Tintasmovespec)all.pop();
	 			if((m.to_col==c.col) && (m.to_row==c.row)) { return(true); }
	 		}
	 		while(all.size()>0);
	 		return(false);
	 	}
	 	if((color!=null) && addPawnMoves(null,null,whoseTurn)) 
	 		{ return(false);	// end of the line for multiples
	 		}
	 }
	 return(c!=pawnStack.top() && !c.isEmpty());
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();

 	switch(board_state)
 	{
 	default:throw G.Error("not expecting %s",board_state);
 	case Confirm:
 	case Resign:
 	case ConfirmFinal:
 	case ConfirmSwap:
 		all.push(new Tintasmovespec(MOVE_DONE,whoseTurn));
 		break;
 	case ContinuePlay:
 		G.Assert(addPawnMoves(all,capturedStack.top().topChip(),whoseTurn),"should be moves");
 		all.push(new Tintasmovespec(MOVE_DONE,whoseTurn));
 		break;
 	case PlayOrSwap:
 		if(ENABLE_SWAP) { all.push(new Tintasmovespec(MOVE_SWAP,whoseTurn)); }
		//$FALL-THROUGH$
	case Play:
 	case FirstPlay:
 		if(!addPawnMoves(all,null,whoseTurn))
 		{
 			addAnyPawnMoves(all,whoseTurn);
 		}
 	}

 	return(all);
 }
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {
	 if(txt.charAt(1)!='1') { ypos-= cellsize/5; }
	 super.DrawGridCoord(gc, clt, xpos, ypos, cellsize, txt);
 }

}