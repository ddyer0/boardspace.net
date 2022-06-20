package pushfight;

import static pushfight.Pushfightmovespec.*;

import java.awt.Color;
import java.util.*;
import lib.*;
import lib.Random;
import online.game.*;
import pushfight.PushfightPlay.PlayoutStrategy;

/**
 * PushfightBoard knows all about the game of Hex, which is played
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

class PushfightBoard extends squareBoard<PushfightCell> implements BoardProtocol,PushfightConstants
{	static int REVISION = 101;			// 100 represents the initial version of the game
										// rev 101 switches the movement from rectangular to free
	public int getMaxRevisionLevel() { return(REVISION); }
	PushFightVariation variation = PushFightVariation.pushfight;
	private PushfightState board_state = PushfightState.Puzzle;	
	private PushfightState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	public PushfightState getState() { return(board_state); }
	static int [][]BlackPositions = {
			{'B',2},
			{'C',2},
			{'B',3},
			{'C',3},
			{'C',1},
	};
	static int [][]WhitePositions = {
			{'H',3},
			{'I',3},
			{'H',2},
			{'I',2},
			{'H',4},
	};
	
	static int[][]OffboardPositions = {
			{'A',1},{'A',2},{'A',3},{'A',4},
			{'J',1},{'J',2},{'J',3},{'J',4},	
			{'B',1},{'B',4},
			{'I',1},{'I',4},
			{'C',4},
			{'H',1}
	};
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(PushfightState st) 
	{ 	unresign = (st==PushfightState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	
	// locations of the pieces.  They're always on the board, somewhere
	// black pieces are always in row 1, white pieces in row 0
	private PushfightCell pieces[][] = new PushfightCell[2][5];
	
	private PushfightCell boardCells[] = null;
    private PushColor playerColor[]={PushColor.White,PushColor.Brown};
    private int playerColorIndex[] = {0,1};
    private PushfightChip playerChip[]={PushfightChip.White,PushfightChip.Brown};
    private PushfightChip playerSquare[] = { PushfightChip.white_square,PushfightChip.black_square};
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public PushfightChip getPlayerChip(int p) { return(playerChip[p]); }
	public PushColor getPlayerColor(int p) { return(playerColor[p]); }
	public PushfightChip getCurrentPlayerChip() { return(playerChip[whoseTurn]); }
	public int getPlayerIndex(PushfightChip ch) 
	{	return(ch.color==playerColor[0] ? 0 : 1);
	}

// this is required even if it is meaningless for this game, but possibly important
// in other games.  When a draw by repetition is detected, this function is called.
// the game should have a "draw pending" state and enter it now, pending confirmation
// by the user clicking on done.   If this mechanism is triggered unexpectedly, it
// is probably because the move editing in "editHistory" is not removing last-move
// dithering by the user, or the "Digest()" method is not returning unique results
// other parts of this mechanism: the Viewer ought to have a "repRect" and call
// DrawRepRect to warn the user that repetitions have been seen.
	public void SetDrawState() {setState(PushfightState.Draw_State); };	
	CellStack animationStack = new CellStack();

    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public PushfightChip pickedObject = null;
    public PushfightChip lastPicked = null;
    public PushfightCell pushedEnd = null;		// the end of the current push string
    public PushfightCell pushedDest = null;		// the direct target of the push
    public PushfightCell holdingCell = null;
    public PushfightCell firstMoved = null;
    public PushfightCell secondMoved = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    private CellStack robotStack = new CellStack();
    private IStack robotIstack = new IStack();
    private boolean RECTANGULAR = true;
    private boolean initialPositionDone[] = new boolean[2];
    public int initMoves = 0;	// the number of initialization moves done for this player
    private PushfightState resetState = PushfightState.Puzzle; 
    public PushfightChip lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public PushfightCell newcell(char c,int r)
	{	return(new PushfightCell(PushfightId.BoardLocation,c,r));
	}
	
	// constructor 
    public PushfightBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = PushfightGRIDSTYLE;
        setColorMap(map);
		initBoard(10,4);
 		Random r = new Random(734687);	// this random is used to assign hash values to cells, common to all games of this type.
		allCells.setDigestChain(r);		// set the randomv for all cells on the board

		CellStack cells = new CellStack();
		
		for(int []pos : OffboardPositions)
		{
			getCell((char)pos[0],pos[1]).offBoard = true;
		}
		// construct an array of just the cells on the board
		for(PushfightCell c = allCells; c!=null; c=c.next)
		{
			if(!c.offBoard) 
			{ cells.push(c);
			  c.edgeCount = 0;
			  for(int direction = c.geometry.n-1 ; direction>=0; direction--)
			  {
				  PushfightCell d = c.exitTo(direction);
				  if(d!=null && d.offBoard) 
				  { c.addEdge(direction);
				  } 
			  }
			} 
		}
		boardCells = cells.toArray();

        doInit(init,key,players,rev); // do the initialization 
        autoReverseY();		// reverse_y based on the color map
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
    	RECTANGULAR = revision<=100;
		setState(PushfightState.Puzzle);
		variation = PushFightVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		initMoves = 0;
		gametype = gtype;
		firstMoved = null;
		secondMoved = null;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case pushfight:break;
		}

 		
		
	    AR.setValue(initialPositionDone,false);
	    	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    droppedDestStack.clear();
	    pickedSourceStack.clear();
	    stateStack.clear();
	    
	    pickedObject = null;
	    resetState = PushfightState.Puzzle;
	    pushedEnd = null;
	    pushedDest = null;
	    lastDroppedObject = null;
	    int map[]=getColorMap();
	    AR.copy(playerColorIndex,map);
		playerColor[map[0]]=PushColor.White;
		playerColor[map[1]]=PushColor.Brown;
		playerChip[map[0]]=PushfightChip.White;
		playerChip[map[1]]=PushfightChip.Brown;
	    // set the initial contents of the board to all empty cells
		for(PushfightCell c = allCells; c!=null; c=c.next) { c.reInit();}
		  
		for(int i=0;i<PushfightChip.BlackPieces.length;i++)
		{	{int loc[] =BlackPositions[i];
			PushfightCell c = getCell((char)loc[0],loc[1]);
			// black pieces in row 1
			pieces[PushColor.Brown.ordinal()][i] = c;
			c.addChip(PushfightChip.BlackPieces[i]);
			}
			{int loc[] = WhitePositions[i];
			PushfightCell c = getCell((char)loc[0],loc[1]);
			// white pieces in row 0
			pieces[PushColor.White.ordinal()][i] =c;
			c.addChip(PushfightChip.WhitePieces[i]);
			}
		}
		pushedDest = holdingCell = getCell('J',1);
		holdingCell.reInit();
		holdingCell.addChip(PushfightChip.Anchor);
		cachedTargets = null;
        animationStack.clear();
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }


    /** create a copy of this board */
    public PushfightBoard cloneBoard() 
	{ PushfightBoard dup = new PushfightBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((PushfightBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(PushfightBoard from_b)
    {
        super.copyFrom(from_b);
        robotState.copyFrom(from_b.robotState);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        initMoves = from_b.initMoves;
        pushedEnd = getCell(from_b.pushedEnd);
        pushedDest = getCell(from_b.pushedDest);
        lastPicked = null;
        firstMoved = getCell(from_b.firstMoved);
        secondMoved = getCell(from_b.secondMoved);
        robot = from_b.robot;
        AR.copy(initialPositionDone,from_b.initialPositionDone);
        AR.copy(playerColor,from_b.playerColor);
        AR.copy(playerChip,from_b.playerChip);
        getCell(pieces,from_b.pieces);
        cachedTargets = null;
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((PushfightBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(PushfightBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(resetState==from_b.resetState,"resetState mismatch");
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(sameCells(firstMoved,from_b.firstMoved),"firstMoved matches");
        G.Assert(sameCells(secondMoved,from_b.secondMoved),"secondMoved matches");
        G.Assert(sameCells(pushedEnd,from_b.pushedEnd),"pushedEnd matches");
        G.Assert(sameCells(pushedDest,from_b.pushedDest),"pushedDest matches");
        G.Assert(AR.sameArrayContents(initialPositionDone,from_b.initialPositionDone),"initialPositionDone mismatch");
        G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor mismatch");
        G.Assert(AR.sameArrayContents(playerChip,from_b.playerChip),"playerChip mismatch");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedsourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");

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
		v ^= Digest(r,revision);
		v ^= Digest(r,initialPositionDone);
		v ^= Digest(r,pushedEnd);
		v ^= Digest(r,firstMoved);
		v ^= Digest(r,secondMoved);
		v ^= Digest(r,pushedDest);
		v ^= Digest(r,resetState.ordinal());
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
        	throw G.Error("not expecting setNextPlayer in ",board_state);
        case Puzzle:
            break;
        case Draw_State:
        case PushMove1:
        case PushMove2:
        case Push:
        case InitialPosition:
        case Confirm:
        case GameAllButOver:
        case Resign:
            moveNumber++; //the move is complete in these states
            initMoves = 0;
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
    {	return(win[player]);
    }


    // set the contents of a cell, and maintain the books
    public PushfightChip SetBoard(PushfightCell c,PushfightChip ch)
    {	PushfightChip old = c.topChip();
    	if((ch!=PushfightChip.Anchor) && (old!=null)) { c.removeTop(); }
       	if(ch!=null) { c.addChip(ch); }
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
        firstMoved = null;
        secondMoved = null;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    @SuppressWarnings("unused")
	private PushfightCell unDropObject()
    {	PushfightCell rv = droppedDestStack.pop();
    	setState(stateStack.pop());
    	pickedObject = SetBoard(rv,null); 	// SetBoard does ancillary bookkeeping
    	unsetPiece(rv,pickedObject);
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    @SuppressWarnings("unused")
	private void unPickObject()
    {	PushfightCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	SetBoard(rv,pickedObject);
    	setPiece(rv,pickedObject);
    	pickedObject = null;
    }
    
    // move a piece from C to TO, it has already been moved
    private void setPiece(PushfightCell c,PushfightCell to)
    {	PushfightChip chip = to.topChip();
    	PushfightCell row[] = pieces[chip.color.ordinal()];
    	for(int lim=row.length-1; lim>=0; lim--) { if(row[lim]==c) { row[lim]=to; return; }}
    	G.Error("no empty slot for ",c);
    }
    // set location of a piece containing "chip", some slot will be empty
    private void setPiece(PushfightCell c,PushfightChip chip)
    {	PushColor color = chip.color;
    	if(color!=null)
    	{
    	PushfightCell row[] = pieces[color.ordinal()];
    	for(int lim=row.length-1; lim>=0; lim--) { if(row[lim]==null) { row[lim]=c; return; }}
    	G.Error("no empty slot for ",c);
    	}
    }
    
    // empty the slot that previously contained chip
    private void unsetPiece(PushfightCell c,PushfightChip chip)
    {	PushColor co = chip.color;
    	if(co!=null)
    	{
       	PushfightCell row[] = pieces[co.ordinal()];
    	for(int lim=row.length-1; lim>=0; lim--) { if(row[lim]==c) { row[lim]=null; return; }}
    	G.Error("no slot for ",c);
    	}
    }
    // do a plausibility check on the pieces array, supposedly
    // only needed when initially debugging
    private void checkPieces()
    {	int empty = 0;
    	for(PushfightCell row[] : pieces)
    	{ for(PushfightCell c : row)
    		{	if(c==null) { empty++; }
    			else 
    			{ PushfightChip top = c.topChip();
    			  G.Assert(top!=null,"must be occupied"); 
    			}
    		}
    	}
    	G.Assert(empty<=1,"only one empty");
    }
    // 
    // drop the floating object.
    //
    private void dropObject(PushfightCell c)
    {	dropObject(c,pickedObject);
    	pickedObject = null; 
    }
    private void dropObject(PushfightCell c,PushfightChip pickedObject)
    {
       droppedDestStack.push(c);
       stateStack.push(board_state);
 
       SetBoard(c,pickedObject);
       setPiece(c,pickedObject);
       lastDroppedObject = pickedObject;
             
    }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(PushfightCell c)
    {	return(droppedDestStack.top()==c);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { PushfightChip ch = pickedObject;
      if(ch!=null)
    	{	return(ch.chipNumber()); 
    	}
      	return (NothingMoving);
    }

    public PushfightCell getCell(PushfightCell c)
    {	if(c==null) { return(null); }
    	if(c.rackLocation()==PushfightId.HoldingCell) { return(holdingCell); } 
    	return(getCell(c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(PushfightCell c)
    {	pickedSourceStack.push(c);
    	stateStack.push(board_state);
    	lastPicked = pickedObject = c.topChip();
    	lastDroppedObject = null;
    	SetBoard(c,null);
    	unsetPiece(c,pickedObject);
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(PushfightCell c)
    {	return(c==pickedSourceStack.top());
    }
    public boolean isAsource(PushfightCell c)
    {
    	return(pickedSourceStack.contains(c));
    }
    public PushfightCell getSource() { return(pickedSourceStack.top()); }
    public PushfightCell getDest() { return(droppedDestStack.top()); }
    
    private void setNextStateAfterDrop(PushfightCell fm)
    {
        switch(board_state)
    	{
    	default: 
    		throw G.Error("Not expecting state %s",board_state);
    	case Puzzle:
    	case Gameover:
    	case InitialPosition:
    		initMoves++;
    		break;
    	case Push: 
    		setState(PushfightState.Confirm);
    		break;
    	case PushMove2:
    		firstMoved = fm;
    		setState(PushfightState.PushMove1);
    		break;
    	case PushMove1:
    		{	secondMoved = fm;
    			// must do a push next
    			if(hasPushMoves(whoseTurn))
    			{
    				setState(PushfightState.Push);
    			}
    			else {
    				// can't push, you lose
    				win[nextPlayer[whoseTurn]]=true;
    				setState(PushfightState.GameAllButOver);
    			}
    		}
    		// otherwise continue in PushMove2 state
    	}
    }
    private void setNextStateAfterDone(PushfightCell dest,replayMode replay)
    {	
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state "+board_state);
    	case Gameover: break;
    	case Draw_State:
    		setState(PushfightState.Gameover);
    		break;
       	case GameAllButOver:
       	case Resign:
    		win[whoseTurn] = true;
    		setState(PushfightState.Gameover);
            break;
    	case Confirm:
    	case InitialPosition:
    	case Puzzle:            
            if(dest!=null && dest.offBoard)
       		{
       		 win[nextPlayer[getPlayerIndex(dest.topChip())]]=true;
       		 setState(PushfightState.Gameover);
       		}
    		else if(initialPositionDone[0]&&initialPositionDone[1])
       		{	firstMoved = secondMoved = null;
       			if(initialPositionDone[0] && initialPositionDone[1])
       				{ setState(PushfightState.PushMove2);}
       				else { setState(PushfightState.InitialPosition);};
       		}
       		else 
       		{ setState(PushfightState.InitialPosition);
       		}
    	}
       	resetState = board_state;
    }
    private void doDone(replayMode replay)
    {
    	switch(resetState)
    	{
    	case PushMove1:
    	case PushMove2:
    		PushfightCell e = pushedEnd;
    		setNextPlayer(replay);
    		moveNumber++;
    		acceptPlacement();
    		setNextStateAfterDone(e,replay);
    		break;
    	case InitialPosition:
    		acceptPlacement();
    		initialPositionDone[whoseTurn] = true;
    		setNextPlayer(replay);
    		moveNumber++;
    		setNextStateAfterDone(null,replay);
    		break;
    	case Gameover: break;
    	default:
    		G.Error("Not expecting resetState %s",resetState);
    	}
    }
    PushfightCell pushProxy = new PushfightCell(PushfightId.BoardLocation);
	public void doPush(PushfightCell from,PushfightCell dest0,replayMode replay)
	{	PushfightChip po = pickedObject;
		PushfightCell dest = dest0;
		int direction = findDirection(from,dest);
		PushfightCell source = from;
		pickedObject = null;
		//checkPieces();
		while(dest.topChip()!=null)
		{
			PushfightChip top = dest.removeTop();
			unsetPiece(dest,top);
			G.Assert(dest.topChip()==null,"should be empty now");
			if(dest==dest0)
				{
				dropObject(dest,po);
				}
				else
				{dest.addChip(po);
				setPiece(dest,po);
				}
			po = top;
			source = dest;
			dest = dest.exitTo(direction);
			if(replay!=replayMode.Replay)
			{	
				animationStack.push(source);
				animationStack.push(dest);
			}
		}
		pushedEnd = dest;
		dest.addChip(po);
		setPiece(dest,po);
		if(pushedDest!=null) { pushedDest.removeTop(PushfightChip.Anchor); }
		dest0.addChip(PushfightChip.Anchor);
		if(replay!=replayMode.Replay)
		{	if(replay==replayMode.Single)
			{pushProxy.copyAllFrom(dest0);
			pushProxy.removeTop();
			animationStack.push(from);
			animationStack.push(pushProxy);
			}
			animationStack.push(pushedDest);
			animationStack.push(dest0);
		}
		pushedDest = dest0;
		
		//checkPieces();
		setState(PushfightState.Confirm);
	}
	public void checkGameOver()
	{
		for(PushfightCell c = allCells; c!=null; c=c.next)
		{
			if(c.offBoard)
				{ PushfightChip top = c.topChip();
				  if(top!=null && top.color!=null)
				  {
					  int pl = getPlayerIndex(top);
					  win[nextPlayer[pl]] = true;
					  setState(PushfightState.Gameover);
					  return;
				  }
				}
		}
	}
    public boolean Execute(commonMove mm,replayMode replay)
    {	Pushfightmovespec m = (Pushfightmovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }
        cachedTargets = null;
        //G.print("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone(replay);

            break;
            
        case MOVE_PUSH:
 		case MOVE_FROM_TO:
			{
				PushfightCell src = getCell(m.from_col,m.from_row);
				pickObject(src);	
				if(robot==null) { m.from = pickedObject; }
			}
			//$FALL-THROUGH$
		case MOVE_DROPB:
        	{
			PushfightCell dest =  getCell(m.to_col,m.to_row);
			if(isSource(dest)) { unPickObject(); }
			else
			{
			PushfightChip top = dest.topChip();
			if(robot==null) { m.to = top; }
			if(top==null)
				{dropObject(dest);
	            if((replay==replayMode.Single) || (m.op==MOVE_FROM_TO))
	            	{ animationStack.push(getSource());
	            	  animationStack.push(dest); 
	            	}
	            setNextStateAfterDrop(dest);
				}
				else if(pickedObject.color==null)
				{
					dest.addChip(pickedObject);
					pickedObject = null;
				}
				else
				{ // push move
				
				doPush(getSource(),dest,replay);
				
				}
        	}}
             break;

 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
 			PushfightCell src = getCell(m.from_col,m.from_row);
 			if(board_state==PushfightState.Confirm)
 			{	G.Error("this should be handled by undo");
 			}
 			else
 			{
        	// be a temporary p
        	pickObject(src);
        	m.from = pickedObject;
 			}}
            break;

       case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            setState(PushfightState.Puzzle);	// standardize the current state
            setNextStateAfterDone(null,replay);
            checkGameOver();

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?PushfightState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(resetState=PushfightState.Puzzle);
 
            break;
       case MOVE_GAMEOVERONTIME:
     	   win[whoseTurn] = true;
     	   setState(PushfightState.Gameover);
     	   break;

        default:
        	cantExecute(m);
        }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }


    public boolean legalToHitBoard(PushfightCell c,Hashtable<PushfightCell,Pushfightmovespec>targets)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
		case Gameover:
		case Resign:
		case Draw_State:
			return(false);
		case PushMove1:
			if((pickedObject==null) && isDest(c)) { return(true); }
			//$FALL-THROUGH$
		case Push:
		case InitialPosition:
		case PushMove2:
			return(targets.get(c)!=null || ((pickedObject!=null) && isSource(c)));
		case Confirm:
		case GameAllButOver:
			return(isDest(c));
        default:
        	throw G.Error("Not expecting Hit Board state " + board_state);
        case Puzzle:
        	PushfightChip top = c.topChip();
        	if(pickedObject!=null && pickedObject.color==null)
        	{
        	return(c.offBoard ? top==null : top!=null && top.shape==PushShape.Square	);
        	}
        	else
        	{
            return (pickedObject==null ? top!=null : top==null);
        	}
        }
    }
    
    @SuppressWarnings("unused")
    private int robotDepth = 0;
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Pushfightmovespec m)
    {	robotDepth++;
        robotState.push(board_state); 	// record the starting state. The most reliable
        robotState.push(resetState);
        robotStack.push(pushedDest);	// pushedDest before
       	robotStack.push(pushedEnd);		// how far we pushed
       	robotStack.push(firstMoved);
       	robotStack.push(secondMoved);
       	if(board_state==PushfightState.InitialPosition) { robotIstack.push(initMoves); } 
        // to undo state transistions is to simple put the original state back.
        //G.print("R "+m+" "+whoseTurn+" "+board_state);
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        Execute(m,replayMode.Replay);
 
       	//G.print(" .. "+whoseTurn+" "+board_state);
        acceptPlacement();
       
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(Pushfightmovespec m)
    {	robotDepth--;
		//G.print("U "+m+" for "+whoseTurn+" "+board_state);
    	secondMoved = robotStack.pop();
    	firstMoved = robotStack.pop();
     	PushfightCell end = robotStack.pop();
    	PushfightCell anc = robotStack.pop();
    	PushfightState resstate = robotState.pop();
    	PushfightState state = robotState.pop();
       	if(state==PushfightState.InitialPosition) { initMoves = robotIstack.pop(); } 

    	//checkPieces();
       	if(anc!=pushedDest)
		{
		PushfightChip top = pushedDest.removeTop();
		G.Assert(top==PushfightChip.Anchor,"should be the anchor");
		anc.addChip(top);
		}
		
		setState(state);
    	resetState = resstate;
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }

        switch (m.op)
        {
        default:
   	    	throw G.Error("Can't un execute " + m);
        case MOVE_DONE:
        	if(resetState==PushfightState.InitialPosition)
        	{
        		initialPositionDone[whoseTurn] = false;
        	}
            break;
        case MOVE_FROM_TO:
        	{
        	PushfightCell from = getCell(m.from_col,m.from_row);
        	PushfightCell to = getCell(m.to_col,m.to_row);
        	from.addChip(to.removeTop());
        	setPiece(to,from);
        	}
        	break;
        case MOVE_PUSH:
        	{
        	PushfightCell from = getCell(m.from_col,m.from_row);
            PushfightCell to = getCell(m.to_col,m.to_row);
            PushfightChip top = to.removeTop();
        	from.addChip(top);
            setPiece(to,from);
        	if(pushedEnd!=to)
        		{	// undo push       		
        		int direction = findDirection(from,to);
        		while(to!=pushedEnd)
        			{
        			PushfightCell next = to.exitTo(direction);
        			to.addChip(next.removeTop());
        			setPiece(next,to);
        			to = next;
        			}
        		}
        	}
        }
		pushedDest = anc;
		pushedEnd = end;
		//checkPieces();
  }
  
 public void addInitialMoves(CommonMoveStack all,int who)
 {
	 PushColor myColor = getPlayerColor(who);
	 if(pickedObject!=null)
	 {
		addInitialMoves(all,getSource(),pickedObject,who); 
	 }
	 else
	 {
	 for(PushfightCell c : pieces[playerColorIndex[who]])
	 {	if(c!=null)
	 	{
		 PushfightChip top = c.topChip();	
		 if((top!=null) && (top.color==myColor))
			{
			 addInitialMoves(all,c,top,who);
			}
	 }}}
 }
 
 public Pushfightmovespec getRandomInitialMove(Random r,int who)
 {	// without losing much randomness, we can place a specific piece next
	 for(PushfightCell c : pieces[playerColorIndex[who]])
	 {	if((c!=null) && !c.onCenterline())
	 	{
		 PushfightChip top = c.topChip();	
		 if(top!=null)
			{randomMoveCells.clear();
			 collectRandomInitialMoves(randomMoveCells,c,who);
			 int rn = randomMoveCells.size();
			 if(rn>0)
			 {	PushfightCell dest = randomMoveCells.elementAt(r.nextInt(rn));
				 return(Pushfightmovespec.create(MOVE_FROM_TO,c,dest,who));
			 }
			}
	 }}
	 return(null);
 }
 public void collectRandomInitialMoves(CellStack all,PushfightCell from, int who)
 {
	 boolean fromHalf = from.halfBoard();
	 boolean some = false;
	 for(PushfightCell to : boardCells)
	 	{ 
		if(from!=to 
				&& (to.topChip()==null)
				&& to.onCenterline()
				&& (fromHalf==to.halfBoard()))
				{
				all.push(to);
				some = true;
				}
	 	}
	 if(!some)
	 {
	 if(initMoves<5)
	 {
		for(PushfightCell to : boardCells)
		 { 
			if(from!=to 
					&& (to.topChip()==null)
					&& (fromHalf==to.halfBoard()))
					{
					all.push(to);
					}
		 }}
	 }
 }
 public void addCenterlineMoves(CommonMoveStack all,int who)
 {	
	 if(pickedObject!=null)
	 {
		addCenterlineMoves(all,getSource(),pickedObject,who); 
	 }
	 else {
	 for(PushfightCell c : pieces[playerColorIndex[who]])
	 {	if(c!=null && !c.onCenterline())
	 	{
		 PushfightChip top = c.topChip();	
		 if(top!=null)
			{
			 addCenterlineMoves(all,c,top,who);
			}}
	 }}
 }
 public void addCenterlineMoves(CommonMoveStack all,PushfightCell from, PushfightChip top,int who)
 {
	 boolean fromHalf = from.halfBoard();
	 boolean some = false;
	 if(initMoves<5)
	 {
	 for(PushfightCell to : boardCells)
	 	{ 
		if(from!=to 
				&& (to.topChip()==null)
				&& to.onCenterline()
				&& (fromHalf==to.halfBoard()))
				{
				all.push(Pushfightmovespec.create(MOVE_FROM_TO,from,to,who));
				some = true;
				}
	 	}
	 	if(!some)
	 	{
		for(PushfightCell to : boardCells)
		 { 
			if(from!=to 
					&& (to.topChip()==null)
					&& (fromHalf==to.halfBoard()))
					{
					all.push( Pushfightmovespec.create(MOVE_FROM_TO,from,to,who));
					}
		 }}}
	 if(!some) { all.push(Pushfightmovespec.create(MOVE_DONE,who)); }
	 
 }
 private void addInitialMoves(CommonMoveStack all,PushfightCell from,PushfightChip top,int who)
 {	boolean fromHalf = from.halfBoard();
 	for(PushfightCell to : boardCells)
	 { 
		if(from!=to 
				&& (to.topChip()==null)
				&& (fromHalf==to.halfBoard()))
				{
				all.push(Pushfightmovespec.create(MOVE_FROM_TO,from,to,who));
				}
	}
 }
 
 private void addLinearMoves(CommonMoveStack all,int who)
 {
	 PushColor myColor = getPlayerColor(who);
	 if(pickedObject!=null)
	 {
		if(RECTANGULAR) { addLinearMoves(all,getSource(),pickedObject,who); }
		else { addFreeMoves(all,getSource(),pickedObject,who); }
	 }
	 else
	 {
	 for(PushfightCell c : pieces[playerColorIndex[who]])
	 {	if(c!=null && (c!=firstMoved))
	 	{
		 PushfightChip top = c.topChip();	
		 if((top!=null) && (top.color==myColor))
			{
			 if(RECTANGULAR) { addLinearMoves(all,c,top,who); }
			 else { addFreeMoves(all,c,top,who); }
			}
		 }
	 }}	 
 }
 
 int sweep_counter = 0;
 private void addFreeMoves(CommonMoveStack all,PushfightCell from,PushfightChip top,int who)
 {	sweep_counter++;
 	addFreeMovesFrom(all,from,from,top,who);
 }
 private void addFreeMovesFrom(CommonMoveStack all,PushfightCell origin,PushfightCell from,PushfightChip top,int who)
 {	if(from!=null)
	{ 
	from.sweep_counter = sweep_counter;
	for(int direction=from.geometry.n; direction>0;direction--)
	{
	 PushfightCell c = from.exitTo(direction);
	 if((c!=null) 
			 && !c.offBoard 
			 && (c.topChip()==null) 
			 && (c.sweep_counter!=sweep_counter)) 
	 	{	
		 all.push( Pushfightmovespec.create(MOVE_FROM_TO,origin,c,who));
		 addFreeMovesFrom(all,origin,c,top,who);
	 	}
	}}
 }
 // collect all the cells that are destinations from
 private void collectFreeMovesFrom(CellStack all,PushfightCell origin,PushfightCell from,boolean avoidEdges)
 {	if(from!=null)
	{ 
	from.sweep_counter = sweep_counter;
	for(int direction=from.geometry.n; direction>0;direction--)
	{
	 PushfightCell c = from.exitTo(direction);
	 if((c!=null) 
			 && !c.offBoard 
			 && (c.topChip()==null) 
			 && (c.sweep_counter!=sweep_counter)) 
	 	{	
		 all.push(c);
		 if(avoidEdges)
		 {	// insert the non-edge places more than once, to make them more likely to be chosen
			 switch(c.edgeCount)
			 {
			 case 0:	all.push(c);
				//$FALL-THROUGH$
			 case 1:	all.push(c);
			 	break;
			 default:	break;
			 }
		 }
		 collectFreeMovesFrom(all,origin,c,avoidEdges);
	 	}
	}}
 }
 // add linear moves from a particular place
 private void addLinearMoves(CommonMoveStack all,PushfightCell from,PushfightChip top,int who)
 {	
 	for(int direction=from.geometry.n; direction>0;direction--)
 	{
 		PushfightCell c = from.exitTo(direction);
 		while( (c!=null) && !c.offBoard && (c.topChip()==null)) 
 			{	
 			all.push(Pushfightmovespec.create(MOVE_FROM_TO,from,c,who));
 			c = c.exitTo(direction);
 			}
 	}
 }
 
 private CellStack randomMoveCells = new CellStack();
		
 public commonMove getRandomMove(Random r,PlayoutStrategy strategy)
 {	boolean avoidEdges = false;
 	switch(strategy)
	 {
	 default: throw G.Error("Not expecting playout ",strategy);
	 case PushMore:
	 	{
		 double pc = 0;
		 switch(board_state)
		 {
		 default: return(null);
		 case Confirm:
			 return(Pushfightmovespec.create(MOVE_DONE,whoseTurn));
		 case InitialPosition:
			 return(getRandomInitialMove(r,whoseTurn));
		 case PushMove1: pc += 0.17;
			//$FALL-THROUGH$
		 case PushMove2: pc += 0.33;
		 	if(r.nextDouble()>=pc)
		 	{
		 		Pushfightmovespec m = fastRandomMove(r,true,whoseTurn);
		 		if(m!=null) { return(m); }
		 	}
			//$FALL-THROUGH$
		case Push:
			 return(getRandomPushMove(r,whoseTurn));
		 }}
	 case Slow:
		return(null); 
	 case EasyWin:
	 	{
	 	 switch(board_state)
	 	 {
	 	 default: break;
	 	 case PushMove1:
	 	 case PushMove2:
	 		 Pushfightmovespec win = findEasyWin(whoseTurn);
	 		 if(win!=null)
	 		 	{	return(win);
	 		 	}
	 	 }}
		//$FALL-THROUGH$
	case EdgeAvoidance:
		avoidEdges = true;
		//$FALL-THROUGH$
	case Fast:
		switch(board_state)
		{
		default: return(null);
		 case Confirm:
			 return(Pushfightmovespec.create(MOVE_DONE,whoseTurn));
		 case InitialPosition:
			 return(getRandomInitialMove(r,whoseTurn));
		case PushMove1:
		case PushMove2:
			Pushfightmovespec m = fastRandomMove(r,avoidEdges,whoseTurn);
			if(m!=null) { return(m); }
			//$FALL-THROUGH$
		case Push:
			return(getRandomPushMove(r,whoseTurn));
		}
	 }
 }
 private Pushfightmovespec fastRandomMove(Random r,boolean avoidEdges,int whoseTurn)
 {
	 switch(board_state)
	 {
	 case PushMove1:
	 case PushMove2:
	 	{	
	 	Pushfightmovespec m = avoidEdges
	 				    ? getRandomAvoidEdgeMove(r,whoseTurn)
	 				    : getRandomLinearMove(r,whoseTurn);
	 	// in rare situations, there can be no moves because
	    // of crowding
	    return(m);
	 	}
	 default: return(null);
	 }
 }
	 
 private Pushfightmovespec findEasyWin(int who)
 {	switch(board_state)
	 {
	 default: break;

	 case PushMove1:
	 case PushMove2:
	 case Push:

	 PushfightCell opponents[] = pieces[playerColorIndex[nextPlayer[who]]];
	 PushfightCell possible1 = null;
	 PushfightCell possible2 = null;
	 for(int i=0,lim=opponents.length;i<lim;i++)
	 {	PushfightCell opponent = opponents[i];
	    if(opponent!=null && (opponent.topChip()!=PushfightChip.Anchor))
	 	{
		 int dirs[] = opponent.edgeDirections;
		 if(dirs!=null)
		 {	
			 for(int direction : dirs)
			 {
				 PushfightCell adj = opponent;
				 PushfightCell adj1 = adj;
				 boolean fin=false;
				 while (!fin && ((adj=adj.exitTo(direction+CELL_HALF_TURN))!=null))
				 {	
					 PushfightChip top = adj.topChip();
					 if((top==null) && !adj.offBoard)
					 	{
						possible2 = possible1; 
					 	possible1 = adj; 
					 	fin = true;
					 	}
					 else if(top==PushfightChip.Anchor) { fin=true; }	// can't push the anchor 
					 else if(top==playerSquare[who])
					 {
						 // we have a winner
						 return(Pushfightmovespec.create(MOVE_PUSH, adj, adj1, who));
					 }
					 adj1=adj;
				 }
			 }
			 switch(board_state)
			 {
			 default: break;
			 case PushMove1:
			 case PushMove2:
			 
			 PushfightCell from1 = squareCanReach(possible1,who);
			 if(from1!=null) 
			 	{ return(Pushfightmovespec.create(MOVE_FROM_TO,from1,possible1,who));
			 	}
			 PushfightCell from2 = squareCanReach(possible2,who);
			 if(from2!=null) 
			 	{ return(Pushfightmovespec.create(MOVE_FROM_TO,from2,possible2,who));
			 	}
		 }}}
	 }}
	 return(null);
 }
 public PushfightCell squareCanReach(PushfightCell target,int who)
 {
	 if(target!=null)
	 {
		if(RECTANGULAR) { return squareCanReachRect(target, who); }
		sweep_counter++;
		return(squareCanReachAny(target,who));
	 }
	 return(null);
 }
 private PushfightCell squareCanReachRect(PushfightCell target,int who)
 {
	 throw G.Error("Not implemented");
 }

 // sweep from someplace to see if we can reach a square belonging to who
 private PushfightCell squareCanReachAny(PushfightCell from,int who)
 {	from.sweep_counter = sweep_counter;
 	for(int direction=from.geometry.n; direction>0;direction--)
 	{
 		PushfightCell c = from.exitTo(direction);
 		if( (c!=null) && !c.offBoard)
 			{	
 			PushfightChip top = c.topChip();
 			if(top==playerSquare[who]) { return(c); }
 			if(top==null && (c.sweep_counter!=sweep_counter))
 			{
 				PushfightCell found = squareCanReachAny(c,who);
 				if(found!=null) { return(found); }
 			}
 			}
 	}
 	return(null);
 }
 private PushfightCell possibleSources[] = new PushfightCell[15];	// max of 3 instances for 5 pieces
 private Pushfightmovespec getRandomAvoidEdgeMove(Random r,int who)
 {	PushfightCell sources[]=pieces[playerColorIndex[whoseTurn]];
 	int count = 0;
 	for(PushfightCell c : sources)
	 	{
	 		if(c!=null && c!=firstMoved)
	 		{	// list cells with edges adjacent to the offboad area multuple times
	 			// to make them more likely to be selected
	 			switch(c.edgeCount)
	 			{
	 			case 2: possibleSources[count++] = c;
					//$FALL-THROUGH$
				case 1: possibleSources[count++] = c;
					//$FALL-THROUGH$
				default: possibleSources[count++] = c;
	 			}
	 		}
	 	}
	 	int off = r.nextInt(count);
	 	for(int i=0;i<count;i++)
	 	{
	 		PushfightCell c = possibleSources[(i+off)%count];

			 randomMoveCells.clear();
			 if(RECTANGULAR) { collectLinearMoves(randomMoveCells,c,true); }
			 else { sweep_counter++; collectFreeMovesFrom(randomMoveCells,c,c,true); }
			 int choices = randomMoveCells.size();
			 if(choices>0)
			 {
				 PushfightCell dest = randomMoveCells.elementAt(r.nextInt(choices));
				 return Pushfightmovespec.create(MOVE_FROM_TO,c,dest,who);
			 }

	 	}
	 return(null);
 }
 
 private Pushfightmovespec getRandomLinearMove(Random r,int who)
 {	PushfightCell row[] = pieces[playerColorIndex[who]];
 	int n = row.length;
 	int fromInt = r.nextInt(n);
	for(int i=0;i<n;i++)
	{	PushfightCell c = row[(i+fromInt)%n];
		if(c!=firstMoved)
		{
		 randomMoveCells.clear();
		 if(RECTANGULAR) { collectLinearMoves(randomMoveCells,c,false); }
		 else { sweep_counter++; collectFreeMovesFrom(randomMoveCells,c,c,false); }
		 int choices = randomMoveCells.size();
		 if(choices>0)
		 {
			 PushfightCell dest = randomMoveCells.elementAt(r.nextInt(choices));
			 return Pushfightmovespec.create(MOVE_FROM_TO,c,dest,who);
		 }
	 }}
	 return(null);
 }

 // collect cell that are destinations of linear moves from a particular place
 private void collectLinearMoves(CellStack all,PushfightCell from,boolean avoidEdges)
 {	
 	for(int direction=from.geometry.n; direction>0;direction--)
 	{
 		PushfightCell c = from.exitTo(direction);
 		while( (c!=null) && !c.offBoard && (c.topChip()==null)) 
 			{	
 			all.push(c);
 			if(avoidEdges)
 			{
 				switch(c.edgeCount)
 				{
 				case 0:	all.push(c);
					//$FALL-THROUGH$
				case 1: all.push(c);
					break;
 				default: break;
 				}
 			}
 			c = c.exitTo(direction);
 			}
 	}
 }
 private boolean hasPushMoves(int who)
 {
	 return(addPushMoves(null,who));
 }
 
 private Pushfightmovespec getRandomPushMove(Random r,int who)
 {	PushfightCell row[] = pieces[playerColorIndex[who]];
 	int n = row.length;
 	int rowN = r.nextInt(n);
 	for(int i=0;i<n;i++)
	{PushfightCell  c =row[(rowN+i)%n];
	if(c!=null)
	 	{
		 PushfightChip top = c.topChip();	
		 if((top!=null) && (top.shape==PushShape.Square))
			{
			 randomMoveCells.clear();
			 collectPushMoves(randomMoveCells,c,who);
			 int np = randomMoveCells.size();
			 if(np>0)
				 {PushfightCell dest = randomMoveCells.elementAt(r.nextInt(np));
				 return Pushfightmovespec.create(MOVE_PUSH,c,dest,who);
				 }
			}
	 	}
	 }
 	return(null);
 }
 

 private boolean addPushMoves(CommonMoveStack all,int who)
 {
	 PushColor myColor = getPlayerColor(who);
	 if(pickedObject!=null)
	 { if(pickedObject.shape==PushShape.Square)
		{return addPushMoves(all,getSource(),pickedObject,who); 
		}
	 	return(false);
	 }
	 else
	 {
	 boolean some = false;
	 for(PushfightCell c : pieces[playerColorIndex[who]])
	 {	if(c!=null)
	 	{
		 PushfightChip top = c.topChip();	
		 if((top!=null) && (top.color==myColor) && (top.shape==PushShape.Square))
			{
			 some |= addPushMoves(all,c,top,who);
			 if(some && all==null) { return(true); }
			}
	 	}
	 }
	 return(some);
	 }	 
 }
 //collect the destinations that can be push moves "from"
 private void collectPushMoves(CellStack all,PushfightCell from,int who)
 {	
 	for(int direction=from.geometry.n; direction>0;direction--)
 	{
 		PushfightCell c = from.exitTo(direction);
 		if( (c!=null) && !c.offBoard && (c.topChip()!=null))
 			{	
 			// these has to be a vacant cell in the direction of the push
 			// and you can't push the red top
 			PushfightCell d = c;
 			boolean done = false;
 			while(d!=null && !done)
 			{	PushfightChip tp = d.topChip();
 				if(tp==PushfightChip.Anchor) { done = true; }	// can't push the red
 				else if(tp==null)
 					{ 
 					  all.push(c);
 					  done = true;
 					}
 				d=d.exitTo(direction);
 			}
 			}
 	}
 }

 private boolean addPushMoves(CommonMoveStack all,PushfightCell from,PushfightChip top,int who)
 {	boolean some = false;
 	for(int direction=from.geometry.n; direction>0;direction--)
 	{
 		PushfightCell c = from.exitTo(direction);
 		if( (c!=null) && !c.offBoard && (c.topChip()!=null))
 			{	
 			// these has to be a vacant cell in the direction of the push
 			// and you can't push the red top
 			PushfightCell d = c;
 			boolean done = false;
 			while(d!=null && !done)
 			{	PushfightChip tp = d.topChip();
 				if(tp==PushfightChip.Anchor) { done = true; }	// can't push the red
 				else if(tp==null)
 					{ if(all==null) { return(true); }
 					  all.push( Pushfightmovespec.create(MOVE_PUSH,from,c,who));
 					  some = done = true;
 					}
 				d=d.exitTo(direction);
 			}
 			}
 	}
 	return(some);
 }

 private Hashtable<PushfightCell,Pushfightmovespec> cachedTargets = null;
 public Hashtable<PushfightCell,Pushfightmovespec> getTargets()
 {	if(cachedTargets!=null) { return(cachedTargets); }
    CommonMoveStack all = GetListOfMoves();
	 Hashtable<PushfightCell,Pushfightmovespec>targets = new Hashtable<PushfightCell,Pushfightmovespec>();
	 for(int lim=all.size()-1; lim>=0; lim--)
	 {
	 Pushfightmovespec m = (Pushfightmovespec)all.elementAt(lim);
	 if(pickedObject==null)
	 {
		 switch(resetState)
		 {
		 case Gameover:
		 case Puzzle:
			 break;
		 case InitialPosition:
		 case PushMove2:
		 case PushMove1:
		 case Push:
			 switch(m.op)
			 {
			 case MOVE_PUSH:
			 case MOVE_FROM_TO: 
				targets.put(getCell(m.from_col,m.from_row),m);
			 	break;
			 default: break;
			 }
			 break;
		 default: 
			 G.Error("Not expecting state %s",resetState);	 
		 }
	 }
	 else
	 {	// something moving
		 switch(resetState)
		 {
		 case Puzzle:
			 break;
		 case InitialPosition:
		 case PushMove2:
		 case Push:
			 switch(m.op)
			 {
			 case MOVE_PUSH:
			 case MOVE_FROM_TO: 
				targets.put(getCell(m.to_col,m.to_row),m);
			 	break;
			 default: break;
			 }
			 break;
		 default: 
			 G.Error("Not expecting state %s",resetState);
		 }
	 }}
	 cachedTargets = targets;
	 return(targets);
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	checkPieces();
 	switch(board_state)
 		{
 		case Resign:
 		case Confirm:
 		case GameAllButOver:
 		case Draw_State:
 			all.push( Pushfightmovespec.create(MOVE_DONE,whoseTurn));		
 			break;
 		case InitialPosition:
 			if(robot!=null) { addCenterlineMoves(all,whoseTurn); }
 			else {	addInitialMoves(all,whoseTurn);
 					all.push( Pushfightmovespec.create(MOVE_DONE,whoseTurn));
 			}
 			break;
 		case PushMove1:
 		case PushMove2:
 			addLinearMoves(all,whoseTurn);
 			//$FALL-THROUGH$
 		case Push:
 			addPushMoves(all,whoseTurn);
 			break;
 		case Gameover:
 		case Puzzle: break;
 		default: 
 			G.Error("Not expecting state %s", board_state);
 		}
 	if(all.size()==0)
 	{
 		p1("no moves, state "+board_state);
 	}
 	return(all);
 }

 PushfightPlay robot = null;
 public boolean p1(String msg)
	{	if((robot!=null) && G.p1(msg))
		{	robot.p1(msg); 
			return(true);
		}
		return(false);
	}
 

 public void initRobotValues()
 {
 }

 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   char col = txt.charAt(0);
 	 if((col=='A')||(col=='J')) { return; }
 	 else if ((col>'A')&&(col<'J'))
 	 	{txt = ""+(char)(col-1); 
 	 	 super.DrawGridCoord(gc,clt,xpos,ypos,cellsize,txt);
 	 	}
 	 else { super.DrawGridCoord(gc, clt, xpos+cellsize, ypos, cellsize, txt); }
 }

}
