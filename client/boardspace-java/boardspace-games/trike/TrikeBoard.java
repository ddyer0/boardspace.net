package trike;


import static trike.Trikemovespec.*;

import java.awt.Color;
import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;

/**
 * TrikeBoard knows all about the game of Trike, which is played
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

class TrikeBoard 
	extends hexBoard<TrikeCell>	// for a square grid board, this could be rectBoard or squareBoard 
	implements BoardProtocol,TrikeConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
	static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	TrikeVariation variation = TrikeVariation.Trike_7;
	private TrikeState board_state = TrikeState.Puzzle;	
	private TrikeState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	public TrikeState getState() { return(board_state); }
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(TrikeState st) 
	{ 	unresign = (st==TrikeState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

    private TrikeId playerColor[]={TrikeId.White,TrikeId.Black};    
    private TrikeChip playerChip[]={TrikeChip.White,TrikeChip.Black};
    private TrikeCell playerCell[]=new TrikeCell[2];
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public TrikeChip getPlayerChip(int p) { return(playerChip[p]); }
	public TrikeId getPlayerColor(int p) { return(playerColor[p]); }
	public TrikeCell getPlayerCell(int p) { return(playerCell[p]); }
	public TrikeChip getCurrentPlayerChip() { return(playerChip[whoseTurn]); }
	public TrikePlay robot = null;
	
	 public boolean p1(String msg)
		{
			if(G.p1(msg) && robot!=null)
			{	String dir = "g:/share/projects/boardspace-html/htdocs/trike/trikegames/robot/";
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

    private boolean swapped = false;
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public TrikeChip pickedObject = null;
    public TrikeChip lastPicked = null;
    public TrikeCell pawnLocation = null;
    private TrikeCell blackChipPool = null;	// dummy source for the chip pools
    private TrikeCell whiteChipPool = null;
    private TrikeCell pickedSource = null; 
    private TrikeCell droppedDest = null;
    TrikeCell pawnHome = null;
    
    // save strings to be shown in the game log
    StringStack gameEvents = new StringStack();
    InternationalStrings s = G.getTranslations();
    
 	void logGameEvent(String str,String... args)
 	{	//if(!robotBoard)
 		{String trans = s.get(str,args);
 		 gameEvents.push(trans);
 		}
 	}

    private TrikeState resetState = TrikeState.Puzzle; 
    public TrikeChip lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public TrikeCell newcell(char c,int r)
	{	return(new TrikeCell(TrikeId.BoardLocation,c,r));
	}
	
	// constructor 
    public TrikeBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = GRIDSTYLE;
        setColorMap(map, players);
        
		Random r = new Random(734687);
		// do this once at construction
	    blackChipPool = new TrikeCell(r,TrikeId.Black);
	    blackChipPool.addChip(TrikeChip.Black);
	    whiteChipPool = new TrikeCell(r,TrikeId.White);
	    whiteChipPool.addChip(TrikeChip.White);
	    pawnHome = new TrikeCell(r,TrikeId.Pawn);

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
 		setState(TrikeState.Puzzle);
		variation = TrikeVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		gametype = gtype;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case Trike_13:
		case Trike_11:
		case Trike_9:
		case Trike_7:
			// using reInitBoard avoids thrashing the creation of cells 
			// when reviewing games.
			reInitBoard(variation.firstInCol,variation.ZinCol,null);
			// or initBoard(variation.firstInCol,variation.ZinCol,null);
			// Random r = new Random(734687);	// this random is used to assign hash values to cells, common to all games of this type.
			// allCells.setDigestChain(r);		// set the randomv for all cells on the board
		}

 		
	    playerCell[FIRST_PLAYER_INDEX] = whiteChipPool; 
	    playerCell[SECOND_PLAYER_INDEX] = blackChipPool; 
	    pawnHome.reInit();
	    pawnHome.addChip(TrikeChip.Pawn);
	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    droppedDest = null;
	    pickedSource = null;
	    pawnLocation = null;
	    
	    pickedObject = null;
	    resetState = TrikeState.Puzzle;
	    lastDroppedObject = null;
	    int map[]=getColorMap();
		playerColor[map[0]]=TrikeId.White;
		playerColor[map[1]]=TrikeId.Black;
		playerChip[map[0]]=TrikeChip.White;
		playerChip[map[1]]=TrikeChip.Black;
	    
        animationStack.clear();
        swapped = false;
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }

    /** create a copy of this board */
    public TrikeBoard cloneBoard() 
	{ TrikeBoard dup = new TrikeBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((TrikeBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(TrikeBoard from_b)
    {
        super.copyFrom(from_b);
        robotState.copyFrom(from_b.robotState);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        pawnLocation = getCell(from_b.pawnLocation);
        droppedDest = getCell(from_b.droppedDest);
        pickedSource = getCell(from_b.pickedSource);
        pawnHome.copyFrom(from_b.pawnHome);
        copyFrom(whiteChipPool,from_b.whiteChipPool);		// this will have the side effect of copying the location
        copyFrom(blackChipPool,from_b.blackChipPool);		// from display copy boards to the main board
        getCell(playerCell,from_b.playerCell);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        lastPicked = null;

        AR.copy(playerColor,from_b.playerColor);
        AR.copy(playerChip,from_b.playerChip);
 
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((TrikeBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(TrikeBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor mismatch");
        G.Assert(AR.sameArrayContents(playerChip,from_b.playerChip),"playerChip mismatch");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(sameCells(pickedSource,from_b.pickedSource),"pickedsource mismatch");
        G.Assert(sameCells(pawnLocation,from_b.pawnLocation),"pawnlocation mismatch");
               
        G.Assert(sameCells(droppedDest,from_b.droppedDest),"droppedDest mismatch");
        G.Assert(sameCells(playerCell,from_b.playerCell),"player cell mismatch");
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
		v ^= chip.Digest(r,playerChip[0]);	// this accounts for the "swap" button
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSource);
		v ^= Digest(r,droppedDest);
		v ^= Digest(r,pawnLocation);
		v ^= Digest(r,revision);
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
        	throw G.Error("Move not complete, can't change the current player in state ",board_state);
        case Puzzle:
            break;
        case Play:
        case PlayOrSwap:
        	// some damaged games have 2 dones in a row
        	if(replay==replayMode.Live) { throw G.Error("Move not complete, can't change the current player in state ",board_state); }
			//$FALL-THROUGH$
		case ConfirmSwap:
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
    {	if(board_state==null) { board_state=TrikeState.Puzzle; }
    	return(board_state.doneState());
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
    //
    // accept the current placements as permanent
    //
    public void acceptPlacement()
    {	
        droppedDest = null;
        pickedSource = null;
        pickedObject = null;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private TrikeCell unDropObject()
    {	TrikeCell rv = droppedDest;
    	setState(resetState);
    	pickedObject = rv.removeTop(); 	// SetBoard does ancillary bookkeeping
    	if((pickedObject==TrikeChip.Pawn) && !rv.isEmpty())
    	{
    		rv.removeTop();
    		pawnLocation = pickedSource;
    	}
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	TrikeCell rv = pickedSource;
    	setState(resetState);
    	rv.addChip(pickedObject);
    	pickedObject = null;
    	pickedSource = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(TrikeCell c)
    {
       droppedDest = c;
       
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting dest " + c.rackLocation);
        case Pawn:
        	c.addChip(pickedObject);
        	break;
        case Black:
        case White:		// back in the pool, we don't really care where
        	pickedObject = null;
            break;
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        	c.addChip(pickedObject);
        	c.lastPlaced = moveNumber;
        	pickedObject = null;
            break;
        }
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(TrikeCell c)
    {	return(droppedDest==c);
    }
    public TrikeCell getDest()
    {	return(droppedDest);
    }
 
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { TrikeChip ch = pickedObject;
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
    private TrikeCell getCell(TrikeId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s" , source);
        case Pawn:
        	return pawnHome;
        	
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
    public TrikeCell getCell(TrikeCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(TrikeCell c)
    {	pickedSource=c;
        switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting rackLocation " + c.rackLocation);
        case BoardLocation:
        	{
            lastPicked = pickedObject = c.removeTop();
            c.lastEmptied = moveNumber;
         	lastDroppedObject = null;
        	}
            break;
        case Pawn:
        	pickedObject = c.removeTop();
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
    public boolean isSource(TrikeCell c)
    {	return(c==pickedSource);
    }
    public TrikeCell getSource()
    {	return(pickedSource);
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
        case FirstPlay:
        case PlayOrSwap:
			setState(TrikeState.Confirm);
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
    	default: throw G.Error("Not expecting after Done state "+board_state);
    	case Gameover: break;
    	case ConfirmSwap: 
    		setState(TrikeState.Play); 
    		break;
    	case Confirm:
    	case Puzzle:
    	case Play:
    	case FirstPlay:
    	case PlayOrSwap:
    		setState(pawnLocation==null ? TrikeState.FirstPlay
    				: ((resetState==TrikeState.FirstPlay)&&(whoseTurn==SECOND_PLAYER_INDEX)&&!swapped) 
    					? TrikeState.PlayOrSwap
    					: hasPawnMoves() ? TrikeState.Play : TrikeState.Gameover);
    		if(board_state==TrikeState.Gameover)
    		{
    			win[whoseTurn] = majorityLinks();
    			win[nextPlayer[whoseTurn]] = !win[whoseTurn];
    		}
    		
    		break;
    	}
       	resetState = board_state;
    }
    private boolean majorityLinks()
    {	int count = 0;
    	int links = 1;
    	TrikeChip target = getPlayerChip(whoseTurn);
    	TrikeCell center = pawnLocation;
    	if(center.chipAtIndex(0)==target) { count++; }
    	for(int i=0;i<geometry.n;i++)
    	{	TrikeCell adj = center.exitTo(i);
    		if(adj!=null)
    		{
    			links++;
    			if(adj.topChip()==target) { count++; }
    	}	
    	}
    	return count*2>links;
    }
    private void doDone(replayMode replay)
    {
        acceptPlacement();

        if (board_state==TrikeState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(TrikeState.Gameover);
        }
        else
        {	if(winForPlayerNow(whoseTurn)) 
        		{ win[whoseTurn]=true;
        		  setState(TrikeState.Gameover); 
        		}
        	else {setNextPlayer(replay);
        		setNextStateAfterDone(replay);
        	}
        }
    }
void doSwap(replayMode replay)
{	TrikeId c = playerColor[0];
	TrikeChip ch = playerChip[0];
	playerColor[0]=playerColor[1];
	playerChip[0]=playerChip[1];
	playerColor[1]=c;
	playerChip[1]=ch;
	TrikeCell cc = playerCell[0];
	playerCell[0]=playerCell[1];
	playerCell[1]=cc;
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
		  setState(TrikeState.ConfirmSwap);
		  break;
	case ConfirmSwap:
		  setState(TrikeState.PlayOrSwap);
		  break;
	case Gameover:
	case Puzzle: break;
	}
	}
	
    public boolean Execute(commonMove mm,replayMode replay)
    {	Trikemovespec m = (Trikemovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }

        //G.print("E "+m+" for "+whoseTurn+" "+state);
        switch (m.op)
        {
		case MOVE_SWAP:	// swap colors with the other player
			doSwap(replay);
			break;
        case MOVE_DONE:

         	doDone(replay);

            break;

        case MOVE_DROPB:
        	{
			TrikeChip po = pickedObject;
			TrikeCell dest =  getCell(TrikeId.BoardLocation,m.to_col,m.to_row);
			
			if(isSource(dest)) 
				{ unPickObject(); 
				}
				else 
				{
				m.chip = pickedObject;		
		        
				if(board_state==TrikeState.Puzzle && (pickedObject!=TrikeChip.Pawn))
				{
					TrikeChip added = pickedObject;
					if(added==null) { added = lastDroppedObject; }
					if(added==null) { added = getPlayerChip(whoseTurn); }
					lastDroppedObject = added;
					m.chip = added;
					dest.addChip(added);
				}
				else
				{
				if(pickedSource==null)
			        {
			        	pickObject((pawnLocation==null)? pawnHome : pawnLocation);
			        }
		        if(pickedObject==TrikeChip.Pawn)
		        {	if(dest.isEmpty())
		        	{TrikeChip added = getPlayerChip(whoseTurn); 
		        	lastDroppedObject = added;
		        	m.chip = added;
		        	dest.addChip(added); 
		        	}
		        	pawnLocation = dest;
		        }
		        
	        
	            dropObject(dest);
	            
	            if(replay!=replayMode.Replay)
            	{
            	if(po==null)
            	{ animationStack.push(getSource());
            	  animationStack.push(dest); 
            	}
            	}
				}

	            	
	            setNextStateAfterDrop(replay);
				}
        	}
             break;

        case MOVE_PICK:
 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
 			TrikeCell src = getCell(m.source,m.to_col,m.to_row);
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
        		setState(resetState);
        		break;
        	default: ;
        	}}}
            break;

        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
            TrikeCell dest = getCell(m.source,m.to_col,m.to_row);
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
            setState(TrikeState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ setState(TrikeState.Gameover); 
               	}
            else {  setNextStateAfterDone(replay); }

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?TrikeState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(TrikeState.Puzzle);
 
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(TrikeState.Gameover);
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
        case PlayOrSwap:
        case FirstPlay:
        case Play:
         case Confirm:
		case ConfirmSwap:
		case Resign:
		case Gameover:
			return(false);
        case Puzzle:
            return ((pickedObject!=null)?(pickedObject==playerChip[player]):true);
        }
    }

    public boolean legalToHitBoard(TrikeCell c,Hashtable<TrikeCell,Trikemovespec> targets )
    {	if(c==null) { return(false); }
        switch (board_state)
        {
		case Play:
		case PlayOrSwap:
		case FirstPlay:
			return(c==pawnLocation || (targets.get(c)!=null) || isDest(c) || isSource(c));
		case ConfirmSwap:
		case Gameover:
		case Resign:
			return(false);
		case Confirm:
			return(isDest(c));
        default:
        	throw G.Error("Not expecting Hit Board state %s", board_state);
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
    public void RobotExecute(Trikemovespec m)
    {
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
    public void UnExecute(Trikemovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	TrikeState state = robotState.pop();
        switch (m.op)
        {
        default:
   	    	throw G.Error("Can't un execute %s" , m);
        case MOVE_DONE:
            break;
            
        case MOVE_SWAP:
        	setState(state);
        	doSwap(replayMode.Replay);
        	break;
        case MOVE_DROPB:
        	G.Error("Not");
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
 public boolean hasPawnMoves()
 {
	 return addPawnMoves(null,pawnLocation,whoseTurn);
 }
 public boolean addPawnMoves(CommonMoveStack all,TrikeCell from,int who)
 {	boolean some = false;
	 for(int direction = 0; direction<geometry.n; direction++)
	 {
		 some |= addPawnMoves(all,from,direction,who);
		 if(some && all==null) { return true; }
	 }
	 return some;
 }
 public boolean addPawnMoves(CommonMoveStack all,TrikeCell from,int direction,int who)
 {
	 TrikeCell next = from;
	 boolean some = false;
	 while( (next=next.exitTo(direction))!=null)
			 {
		 		if(next.topChip()!=null) { break; }
		 		if(all==null) { return true; }
		 		all.push(new Trikemovespec(MOVE_DROPB,next,who));
			 }
	 return some;
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	if(board_state==TrikeState.PlayOrSwap)
 	{
 		all.addElement(new Trikemovespec(SWAP,whoseTurn));
 	}
 	switch(board_state)
 	{
 	case Puzzle:
 		{int op = pickedObject==null ? MOVE_DROPB : MOVE_PICKB; 	
 			for(TrikeCell c = allCells;
 			 	    c!=null;
 			 	    c = c.next)
 			 	{	if(c.topChip()==null)
 			 		{all.addElement(new Trikemovespec(op,c,whoseTurn));
 			 		}
 			 	}
 		}
 		break;
 	case FirstPlay:
 		for(TrikeCell c = allCells; c!=null; c=c.next)
 		{
 			all.push(new Trikemovespec(MOVE_DROPB,c,whoseTurn));
 		}
 		break;
 	case PlayOrSwap:
 		all.push(new Trikemovespec(MOVE_SWAP,whoseTurn));
		//$FALL-THROUGH$
	case Play:
 		addPawnMoves(all,pawnLocation,whoseTurn);
 		break;
 	case Confirm:
 	case Resign:
 	case ConfirmSwap:
 		all.push(new Trikemovespec(MOVE_DONE,whoseTurn));
 		break;
 	case Gameover:
 		break;
 	default:
 			G.Error("Not expecting state %s",board_state);
 	}
 	return(all);
 }
 
 public void initRobotValues()
 {
 }

 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(Character.isDigit(txt.charAt(0)))
	 	{ switch(variation)
	 		{
	 		case Trike_13:
	 		case Trike_11:
	 		case Trike_9:
	 		case Trike_7:
	 			xpos -= cellsize/2;
	 			break;
 			default: G.Error("case %s not handled",variation);
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
 public Hashtable<TrikeCell, Trikemovespec> getTargets() 
 {
 	Hashtable<TrikeCell,Trikemovespec> targets = new Hashtable<TrikeCell,Trikemovespec>();
 	CommonMoveStack all = GetListOfMoves();
 	for(int lim=all.size()-1; lim>=0; lim--)
 	{	Trikemovespec m = (Trikemovespec)all.elementAt(lim);
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

 		default: G.Error("Not expecting %s",m);
 		
 		}
 	}
 	
 	return(targets);
 }
 


 // most multi player games can't handle individual players resigning
 // this provides an escape hatch to allow it.
 //public boolean canResign() { return(super.canResign()); }
}
