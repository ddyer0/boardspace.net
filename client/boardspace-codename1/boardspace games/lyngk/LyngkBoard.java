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
package lyngk;

import static lyngk.LyngkMovespec.*;

import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;

/**
 * LyngkBoard knows all about the game of Lyngk, which is played
 * on a "snowflake" hexagonal board. It gets a lot of logistic support
 * from common.hexBoard, which knows about the coordinate system.  
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

class LyngkBoard extends hexBoard<LyngkCell> implements BoardProtocol,LyngkConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
    static final String[] LYNGKGRIDSTYLE = { null,"A1", "A1"}; // left and bottom numbers
	LyngkVariation variation = LyngkVariation.lyngk;
	private LyngkState board_state = LyngkState.Puzzle;	
	private LyngkState unresign = null;	// remembers the orignal state when "resign" is hit
	public LyngkState getState() { return(board_state); }
	private int sweep_counter = 0;
	private StateStack robotState = new StateStack();	// state undo for the bot
	private IStack robotHeight = new IStack();			// move height undo for the bot
	private CellStack robotClaim = new CellStack();
	private boolean robotBoard=false;
	private boolean instantWin = false;
	public void setRobotBoard() { robotBoard = true; }
	/**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(LyngkState st) 
	{ 	unresign = (st==LyngkState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	
	public LyngkCell lastMove[] = new LyngkCell[2];
	public LyngkCell captures[] = new LyngkCell[2];	// captured stones per player
    public LyngkCell unclaimedColors[] = new LyngkCell[LyngkChip.nColors-1];
    public LyngkCell playerColors[]=new LyngkCell[2];	// claimed colors per player
    public LyngkId getPlayerId(int pl) { return(playerIds[pl]); }
    public CellStack filledCells = new CellStack();
    
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public LyngkCell getPlayerCell(int p) { return(playerColors[p]); }

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
    public LyngkCell pickedStack = new LyngkCell();
    public CellStack captureStack = new CellStack();	// cells captured by claim
    public LyngkChip lastPicked = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private IStack pickedLevel = new IStack();
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    
    public LyngkChip lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public LyngkCell newcell(char c,int r)
	{	return(new LyngkCell(LyngkId.BoardLocation,c,r));
	}
	
	// constructor 
    public LyngkBoard(String init,int players,long key,int rev) // default constructor
    {	Random r = new Random(010342);
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = LYNGKGRIDSTYLE;
        for(int i=0;i<unclaimedColors.length;i++) { unclaimedColors[i]=new LyngkCell(r,colorIds[i]); }
        for(int i=0;i<playerIds.length;i++) { playerColors[i] = new LyngkCell(r,playerIds[i]);}
        for(int i=0;i<playerIds.length;i++) { captures[i] = new LyngkCell(r,captureIds[i]);}
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
 		Random r = new Random(734687);	// this random is used to assign hash values to cells, common to all games of this type.
		setState(LyngkState.Puzzle);
		variation = LyngkVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		robotHeight.clear();
		robotClaim.clear();
		gametype = gtype;
		AR.setValue(lastMove,null);
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case lyngk:
		case lyngk_6:
			initBoard(variation.firstInCol,variation.ZinCol,variation.firstCol);
			LyngkCell stack = new LyngkCell();
			for(int i=0;i<nWhitePieces;i++) { stack.addChip(LyngkChip.White); }
			for(int i=0;i<nColoredPieces;i++) 
			{	for(int j=1;j<LyngkChip.nColors;j++)
				{ stack.addChip(LyngkChip.getChip(j));
				}
			}
			Random rs = new Random(key);
			stack.shuffle(rs);
			allCells.setDigestChain(r);		// set the randomv for all cells on the board
		
		// fill the board with randomized chips
		filledCells.clear();
		for(LyngkCell c = allCells; c!=null; c=c.next)
			{	c.addChip(stack.removeTop());
				filledCells.push(c);
			}
		
		
		// initialize the unclaimed colors
		for(LyngkCell c : unclaimedColors)
				{ c.reInit();
				  c.addChip(LyngkChip.getChip(c.rackLocation()));
				}
		// initialize the captures colors
		for(LyngkCell c : captures)	{ c.reInit();}

		// initialize the claimed colors
		for(LyngkCell c : playerColors) { c.reInit(); }
		}
	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    acceptPlacement();
	    // set the initial contents of the board to all empty cells
		
        animationStack.clear();
        moveNumber = 1;
        dropStep = 1;

        // note that firstPlayer is NOT initialized here
    }

    /** create a copy of this board */
    public LyngkBoard cloneBoard() 
	{ LyngkBoard dup = new LyngkBoard(gametype,players_in_game,randomKey,revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((LyngkBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(LyngkBoard from_b)
    {	
        super.copyFrom(from_b);
        robotState.copyFrom(from_b.robotState);
        prevLastPlaced = from_b.prevLastPlaced;
        dropStep = from_b.dropStep;
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        getCell(lastMove,from_b.lastMove);
        robotBoard = from_b.robotBoard;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(captureStack,from_b.captureStack);
        getCell(filledCells,from_b.filledCells);
        pickedLevel.copyFrom(from_b.pickedLevel);
        stateStack.copyFrom(from_b.stateStack);
        pickedStack.copyFrom( from_b.pickedStack);
        lastPicked = null;
        copyFrom(playerColors,from_b.playerColors);
        copyFrom(unclaimedColors,from_b.unclaimedColors);
        copyFrom(captures,from_b.captures);
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((LyngkBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(LyngkBoard from_b)
    {	
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(pickedStack.sameCell(from_b.pickedStack), "picked stack mismatch");
        G.Assert(pickedLevel.sameContents(from_b.pickedLevel), "pickedLevel mismatch");
        G.Assert(sameCells(playerColors, from_b.playerColors),"playerColors mismatch");
        G.Assert(sameCells(unclaimedColors,from_b.unclaimedColors),"unclaimedColors mismatch");
        G.Assert(sameCells(captures,from_b.captures),"captures mismatch");
        G.Assert(filledCells.size()==from_b.filledCells.size(),"filledCells mismatch");   
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
		v ^= pickedStack.Digest(r);
		v ^= Digest(r,captureStack);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,pickedLevel);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,revision);
		v ^= Digest(r,unclaimedColors);
		v ^= Digest(r,captures);
		v ^= Digest(r,playerColors);
		v ^= Digest(r,filledCells.size());
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
        case PlayOrClaim:
        case Claim:
        default:
        	throw G.Error("Move not complete, can't change the current player");
        case Puzzle:
            break;
        case Play:
        case Pass:
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
    // more selective.  This determines if the current board digest is added
    // to the repetition detection machinery.
    public boolean DigestState()
    {	
    	return(board_state.digestState());
    }

    public boolean gameOverNow() { return(board_state.GameOver()); }
    public boolean winForPlayerNow(int player)
    {	return(win[player]);
    }

    //
    // accept the current placements as permanent
    //
    public void acceptPlacement()
    {	
        droppedDestStack.clear();
        pickedSourceStack.clear();
        pickedStack.reInit();
        captureStack.clear();
        pickedLevel.clear();
        stateStack.clear();
    }
    
    private int prevLastPlaced = -1;
    private int prevLastPicked = -1;
    int dropStep = -1;
    
    //
    // undo the drop, restore the moving object to moving status.
    //
    private LyngkCell unDropObject()
    {	LyngkCell rv = droppedDestStack.pop();
    	int lvl = rv.height()-pickedLevel.top();
    	setState(stateStack.pop());
    	rv.transferTo(pickedStack,lvl); 	// SetBoard does ancillary bookkeeping
    	rv.lastPlaced = prevLastPlaced;
    	prevLastPicked = -1;
    	while(captureStack.size()>0)
    	{	LyngkCell dest = captureStack.pop();
    		LyngkCell src = captures[whoseTurn];
    		src.transferTo(dest,src.height()-CaptureHeight);
    		filledCells.push(dest);
    	}
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	LyngkCell rv = pickedSourceStack.pop();
    	rv.lastPicked = prevLastPicked;
    	prevLastPicked = -1;
    	pickedLevel.pop();
    	setState(stateStack.pop());
    	pickedStack.transferTo(rv,0);
    	lastDroppedObject = rv.topChip();
    	if(rv.onBoard) { filledCells.push(rv); }
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(LyngkCell c,replayMode replay,CellStack robot)
    {
       droppedDestStack.push(c);
       stateStack.push(board_state);
//       G.Assert(!c.onBoard || (c.height()+pickedStack.height()<=5), "illegal stack");
       pickedStack.transferTo(c,0);
       if(c.onBoard) { filledCells.pushNew(c); }
       LyngkChip color = c.topChip();
       lastDroppedObject = color;
       switch (board_state)
       {
       case Pass:
       case Confirm:
       case PlayOrClaim:
       default:
    	   throw G.Error("Not expecting drop in state %s", board_state);
       case Claim:
    	   // if the only thing you can do is claim, you must claim
    	   // after claiming, you might have moves or might still have to pass
    	   
       	   checkAllCaptures(color,replay,robot);
       	   
    	   G.Assert(hasBoardMoves(whoseTurn)
    			    ||hasMovesAfterClaiming(whoseTurn,color),"must have moves after claiming");

    	   setState(LyngkState.Play);
    	   break;
       case Play:
			setState(LyngkState.Confirm);
			break;
       case Puzzle:
			acceptPlacement();
           break;
       }
   
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(LyngkCell c)
    {	return(droppedDestStack.top()==c);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { 	LyngkChip top = pickedStack.topChip();
      if(top!=null)
    	{	return(top.chipNumber()); 
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
    private LyngkCell getCell(LyngkId source, char col, int row)
    {
        switch (source)
        {
        case BoardLocation:
        	return(getCell(col,row));
        case FirstCaptures:
        case SecondCaptures:
        	for(LyngkCell c : captures) { if(c.rackLocation()==source) { return(c); }}
        	//$FALL-THROUGH$
        case FirstPlayer:
        case SecondPlayer:
        	for(LyngkCell c : playerColors) { if(c.rackLocation()==source) { return(c); }}
			//$FALL-THROUGH$
		case Blue_Chip:
        case Red_Chip:
        case Black_Chip:
        case Ivory_Chip:
        case Green_Chip:
        	for(LyngkCell c : unclaimedColors) { if(c.rackLocation()==source) { return(c); }}
        
			//$FALL-THROUGH$
		default:
            	throw G.Error("Not expecting source %s", source);
       	}
    }
    public LyngkCell getCell(LyngkCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(LyngkCell c,int lvl)
    {	pickedSourceStack.push(c);
    	prevLastPicked = c.lastPicked;
    	c.lastPicked = dropStep;
    	pickedLevel.push(c.height()-lvl);
    	stateStack.push(board_state);
       	c.transferTo(pickedStack,lvl);
        lastPicked = pickedStack.topChip();
        if(c.onBoard && (lvl==0))
        	{ filledCells.remove(c, false);
        	}
     	switch(board_state)
     	{
     	case Pass:
     	default: G.Error("Not expecting pick in state ",board_state);
			//$FALL-THROUGH$
		case Claim:
     	case Play:
     	case Puzzle: 
     		break;
     	case PlayOrClaim:
     		setState( (c.rackLocation()==LyngkId.BoardLocation)
     					? LyngkState.Play
     					: LyngkState.Claim);
     		break;
     	}
   }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(LyngkCell c)
    {	return(c==pickedSourceStack.top());
    }
    public LyngkCell getSource() { return(pickedSourceStack.top()); }
    
    void setGameOver(boolean win0,boolean win1)
    {	
    	win[0]=win0;
    	win[1]=win1;
    	setState(LyngkState.Gameover);
    }
    long adjustedScoreForPlayer(int n)
    {
    	long val = 0;
    	int startingVal = captures[n].height()/CaptureHeight;
    	
    	for(int target_height=CaptureHeight; target_height>0; target_height--)
    	{	int nstacks = startingVal;
    		startingVal = 0;
    		for(int lim = filledCells.size()-1; lim>=0; lim--)
    		{	LyngkCell c = filledCells.elementAt(lim);
    			if((c.height()==target_height)
    				&& isOwnedBy(c.topChip(),n))
    			{ nstacks++;
    			}}
    		val = val*100+nstacks;
    			
    		}
	return(val);

    }

    private void setNextStateAfterDone(replayMode replay)
    {	
    	switch(board_state)
    	{
    	default: 
    		throw G.Error("Not expecting after Done state %s",board_state);
    	case Puzzle:
    	case Confirm:
    	case Pass:
    		boolean canClaim = playerColors[whoseTurn].height()<2;
    		boolean canMove = hasBoardMoves(whoseTurn);
    		// There's an unusual case that no moves are available, but
    		// a claiming move is still possible which makes board
    		// moves possible.
    		boolean canMoveAfterClaim = canClaim && !canMove && hasMovesAfterClaiming(whoseTurn);
    		setState(canMove 
    					? (canClaim
    						? LyngkState.PlayOrClaim
    						: LyngkState.Play)
    					: (canMoveAfterClaim
    						? LyngkState.Claim		// can't only claim
    						: LyngkState.Pass));
    		if(!canMove)
    		{	int nx = nextPlayer[whoseTurn];
    			boolean canMove2 = hasBoardMoves(nx);
    			if(!canMove2)
    			{
    				long score1 = adjustedScoreForPlayer(0);
    				long score2 = adjustedScoreForPlayer(1);
    				setGameOver(score1>score2,score1<score2);
    			}
    		}
 
    		break;
    	}
    }
    private int checkAllCaptures(LyngkChip color,replayMode replay,CellStack fromStack)
    {	int n = 0;
    	// in the standard game, stacks of 5 are removed from the board
    	if(variation.removeStacksOf5())
    	{for(int lim=filledCells.size()-1; lim>=0; lim--)
    	{
    		LyngkCell c = filledCells.elementAt(lim);
    		if((c.height()==CaptureHeight) && (c.topChip()==color))
			{	captureStack.push(c);
				LyngkCell dest = captures[whoseTurn];
				c.transferTo(dest, 0);
				if(c.onBoard) { filledCells.remove(c, false); }
				n++;	// remember how many unusual captures
				if(fromStack!=null) { fromStack.push(c); }
				if(replay!=replayMode.Replay)
				{	for(int i=0;i<CaptureHeight;i++)
					{
					animationStack.push(c);
					animationStack.push(dest);
					}
				}
			}
    	}}
    	if(fromStack!=null) {  robotHeight.push(n); }

    	return(n);
    }
    private void doDone(replayMode replay)
    {	LyngkCell dest = droppedDestStack.top();
        lastMove[whoseTurn] = dest;
        acceptPlacement();
 		dropStep++;

        // check for captures
        if(dest!=null)
        {
        int height = dest.height();
        if(variation.removeStacksOf5()
        		&& (height==CaptureHeight)
        		&& isOwnedBy(dest.topChip(),whoseTurn))
        {
        	dest.transferTo(captures[whoseTurn],0);
        	filledCells.remove(dest,false);
        	if(replay!=replayMode.Replay)
        	{	for(int i=0;i<CaptureHeight;i++)
				{
        		animationStack.push(dest);
        		animationStack.push(captures[whoseTurn]);
				}
        	}
        };
        
        if((variation==LyngkVariation.lyngk_6)
        	&& (height==InstantWinHeight)
        	&& isOwnedBy(dest.topChip(),whoseTurn))
        {
        	win[whoseTurn] = true;
        	setState(LyngkState.Gameover);
        	return;
        }}
        
        if (board_state==LyngkState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(LyngkState.Gameover);
        }
        else
        {	setNextPlayer(replay);
        		setNextStateAfterDone(replay);
        	}
        }

	void checkFilled()
	{	
		int n = 0;
		for(LyngkCell c = allCells; c!=null; c=c.next)
		{
			if(c.height()>0)
			{
				n++;
				G.Assert(filledCells.contains(c),"not in filled");
			}
		}
		G.Assert(n==filledCells.size(),"filled size wrong");
	}
	
    public boolean Execute(commonMove mm,replayMode replay)
    {	LyngkMovespec m = (LyngkMovespec)mm;
    	//checkFilled();
        if(replay!=replayMode.Replay) { animationStack.clear(); }

        //G.print("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone(replay);

            break;

        case MOVE_DROPB:
        	{
			LyngkCell dest =  getCell(LyngkId.BoardLocation,m.to_col,m.to_row);
			boolean stepped = false;
			if(isSource(dest)) 
				{ unPickObject(); 
				}
				else 
				{
	            if(replay!=replayMode.Replay)
	            	{
	            	// call before the actual drop
	            	addAnimationPath(getSource(),dest,pickedStack.topChip(),pickedStack.height());
	            	stepped = true;
	            	}
	            if(!robotBoard)
	            {
	            	m.target = new LyngkCell(dest);
	            }
	            dropObject(dest,replay,null);
	            if(!stepped) 
	            	{     prevLastPlaced = dest.lastPlaced;
	            	      dest.lastPlaced = dropStep;
	            	      dropStep++;
	            	}
				}
        	}
             break;
        case MOVE_FROM_TO:
        case MOVE_BOARD_BOARD:
        	{
        	LyngkCell from = getCell(m.source,m.from_col,m.from_row);
        	LyngkCell to = getCell(m.dest,m.to_col,m.to_row);
        	boolean stepped = false;
        	if(!robotBoard)
        	{
        		m.target = new LyngkCell(from);
        		m.target2 = new LyngkCell(to);
        	}
        	G.Assert(!to.onBoard || (from.height()+to.height()<=variation.heightLimit()), "illegal stack");
        	pickObject(from,0);
      	  	if(replay!=replayMode.Replay)
  	  		{ 	// call before the actual move
      	  		addAnimationPath(from,to,pickedStack.topChip(),pickedStack.height());
      	  		stepped = true;
  	  		}
        	dropObject(to,replay,robotClaim);
            if(!stepped) 
        	{     prevLastPlaced = to.lastPlaced;
        	      to.lastPlaced = dropStep;
        	      dropStep++;
        	}
     	  	if(m.op==MOVE_FROM_TO) 
      	  		{ 
      	  		// this will be a robot move which is not reversible, 
      	  		// but we need to save undo info in the unusual case
      	  		// that claiming captures something.
       	  		  acceptPlacement();
     	  		}
        	}
        	break;
        case MOVE_PICK:
 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
 			LyngkCell src = getCell(m.source,m.from_col,m.from_row);
 			if(isDest(src)) { unDropObject(); }
 			else
 			{
        	// be a temporary p
 			if(!robotBoard)
 			{
 				m.target = new LyngkCell(src);
 	        }

        	pickObject(src,m.to_row);	// to_row is starting level
        	switch(board_state)
        	{
        	case Puzzle:
         		break;
        	case Confirm:
        		setState(LyngkState.PlayOrClaim);
        		break;
        	default: ;
        	}}}
            break;

        case MOVE_DROP: // drop on chip pool;
        	if(pickedStack.height()>0)
        	{
            LyngkCell dest = getCell(m.dest,m.to_col,m.to_row);
            if(isSource(dest)) { unPickObject(); }
            else
            	{ dropObject(dest,replay,null);
            	  if(replay==replayMode.Single)
            	  { animationStack.push(pickedSourceStack.top());
            	    animationStack.push(dest); 
            	  }
            	}
        	}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            int nextp = nextPlayer[whoseTurn];
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(LyngkState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ setState(LyngkState.Gameover); 
               	}
            else {  setNextStateAfterDone(replay); }

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?LyngkState.Resign:unresign);
            break;
        case MOVE_EDIT:
        	acceptPlacement();
            setState(LyngkState.Puzzle);
 
            break;
        case MOVE_PASS:
        	doDone(replay);
        	break;
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;
        default:
        	cantExecute(m);
        }
        //checkFilled();

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips(LyngkCell c,int player,Hashtable<LyngkCell,LyngkMovespec> targets)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting Legal Hit state %s", board_state);
        case Claim:
        	LyngkCell home = playerColors[player];
        	if(movingObjectIndex()>0)
        	{	// something moving
        		return(isSource(c) || (c==home)); 
        	}
        	else {
        		return (targets.get(c)!=null);
        	}
         case PlayOrClaim:
        	// nothing moving
        	return(targets.get(c)!=null);
		case Pass:
        case Play:
        case Confirm:
        	return(isDest(c)&&(movingObjectIndex()<0));
		case Resign:
		case Gameover:
			return(false);
        case Puzzle:
        	return ((movingObjectIndex()>=0) ? true : !c.isEmpty());
        }
    }

    public boolean LegalToHitBoard(LyngkCell c,Hashtable<LyngkCell,LyngkMovespec> targets)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
		case PlayOrClaim:
		case Play:
			if(targets.get(c)!=null) { return(true); }
			if(movingObjectIndex()>=0)
			{	LyngkCell src = pickedSourceStack.top();
				return(c==src);
			}
			return(false);
		case Gameover:
		case Claim:
		case Resign:
		case Pass:
			return(false);
		case Confirm:
			return(isDest(c));
        default:
        	throw G.Error("Not expecting Hit Board state %s", board_state);
        case Puzzle:
        	return ((movingObjectIndex()>=0) ? true : !c.isEmpty());
        }
    }
    
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(LyngkMovespec m)
    {	//G.print("R "+m+" "+board_state);
        robotState.push(board_state); //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        if (Execute(m,replayMode.Replay))
        {	
        	if ((m.op==MOVE_FROM_TO)||(m.op == MOVE_DONE)||(m.op==MOVE_PASS))
            {
            }
            else if (DoneState())
            {	if(m.op==MOVE_BOARD_BOARD) 
            		{ 
            		  robotHeight.push(pickedLevel.top()); 
            		}
                doDone(replayMode.Replay);
            }
            else
            {
            	throw G.Error("Robot move should be in a done state, is %s",board_state);
            }
        }
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(LyngkMovespec m)
    {	//checkFilled();
        //G.print("U "+m+" for "+whoseTurn);
    	LyngkState state = robotState.pop();
        switch (m.op)
        {
   	    default:
   	    	throw G.Error("Can't un execute %s", m);
        case MOVE_DONE:
            break;
        	
        case MOVE_FROM_TO:	// prints as CLAIM
        	{	int ncap = robotHeight.pop();
        		LyngkCell from = getCell(m.source,m.from_col,m.from_row);
        		LyngkCell to = getCell(m.dest,m.to_col,m.to_row);
        		to.transferTo(from, to.height()-1);
        		while(ncap-- > 0)
        		{	LyngkCell src = captures[whoseTurn];
        			LyngkCell dest = robotClaim.pop();
        			src.transferTo(dest,src.height()-CaptureHeight);
        			filledCells.push(dest);
        		}        		
        	}
        	break;
        case MOVE_BOARD_BOARD:
        	{
        		LyngkCell from = getCell(m.source,m.from_col,m.from_row);
        		LyngkCell to = getCell(m.dest,m.to_col,m.to_row);
        		int h = robotHeight.pop();
        		if(to.height()==0)	// was a capture
        			{	
        			LyngkCell cap = captures[m.player];
        			cap.transferTo(to, cap.height()-CaptureHeight);
        			filledCells.push(to);
        			}
        		to.transferTo(from, to.height()-h);
        		filledCells.push(from);
        	}
        	break;
        case MOVE_PASS:
        case MOVE_RESIGN:
            break;
        }
        setState(state);
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
        //checkFilled();
 }
 private boolean canBeMovedBy(LyngkChip top,int who)
 {	
 	// can't move a stack if the opponent claims the top chip
	if(top==LyngkChip.White) { return(false); }
 	LyngkCell opp = playerColors[nextPlayer[who]];
 	return(!opp.containsChip(top));
 }
 boolean isOwnedBy(LyngkChip top,int who)
 {	return(playerColors[who].containsChip(top));
 }
 private boolean canCombine(LyngkCell from,LyngkCell to,boolean topIsOwned)
 {	int fh = from.height();
 	int th = to.height();

 	if((fh+th)<=variation.heightLimit())
 	{
 	if( topIsOwned || (fh>=th)  )
 		{	// we own it, or it's not a taller stack
 			return((from.colorMask()&to.colorMask())==0);
 		}}
 	return(false);
 }


 // moves that claim a color
 private boolean addClaimMoves(CommonMoveStack all,int who,boolean andMove)
 {	boolean some = false;
 	LyngkCell dest = playerColors[who];
 	if(dest.height()<2)
 	{
 		for(LyngkCell c : unclaimedColors )
 		{
 			if(c.height()>0)
 			{
 				if(!andMove && (all==null)) { return(true); }
 				if(!andMove || hasMovesAfterClaiming(who,c.topChip()))
 				{
 				if(all!=null) { all.push(new LyngkMovespec(c.rackLocation(),dest.rackLocation(),who)); }
 				some = true;
 				}
 			}
 		}
 	}
	return(some);
 }
 private Hashtable<LyngkCell,LyngkCell[]>pathHash = new Hashtable<LyngkCell,LyngkCell[]>();
 private CellStack activePath = new CellStack();
 
 private void addAnimationPath(LyngkCell src,LyngkCell dest,LyngkChip top,int height)
 {	
	pathHash.clear();
	activePath.clear();
	sweep_counter++;
 	addLyngkMoves(null,whoseTurn,src,src,top,isOwnedBy(top,whoseTurn),true);
 	LyngkCell p[] = pathHash.get(dest);
 	LyngkCell from = src;
 	LyngkCell realFrom = from;
 	// add one copy of each from:to for each chip being moved.
 	if(p!=null)
 	{	
 		for(LyngkCell to:p)
	 	{
 		LyngkCell cop = new LyngkCell();
 		// need to make a scratch copy so the animation destination 
 		// for the intermediate points won't be the same as the one
 		// actually drawn.  This prevents the future steps of the 
 		// animation from disappearing
 		cop.copyAllFrom(to);
 		realFrom.lastPicked = dropStep;
 		to.lastPlaced = dropStep;
 		dropStep++;
	 	for(int i=0;i<height;i++)
	 		{
	 		animationStack.push(from);
	 		animationStack.push(cop);
	 		}
	 	from = cop;
	 	realFrom = to;
	 	}

 	}
 	realFrom.lastPicked = dropStep;
 	dest.lastPlaced = dropStep;
	dropStep++;
	
 	for(int i=0;i<height;i++)
 	{
 		animationStack.push(from);
 		animationStack.push(dest);
	
 	}
 	
  	pathHash.clear();
 }
 private boolean addLyngkMoves(CommonMoveStack all,int who,LyngkCell from,LyngkCell moving,
		 LyngkChip top,boolean topIsOwned,boolean recordPath)
 {	boolean some = false;
 	if(from.sweep_counter!=sweep_counter)
 	{
 	from.sweep_counter = sweep_counter;
	for(int dir=from.geometry.n-1; dir>=0; dir--)
	 	{
	 		LyngkCell to=from;
	 		while( ((to= to.exitTo(dir))!=null) 
	 				&& ((to.height()==0)
	 				    || (topIsOwned 	// stack that will be removed
	 				    		&& (to.height()==CaptureHeight)
	 				    		&& (to.topChip()==top)
	 				    		&& variation.removeStacksOf5()
	 				    		)))
	 				{};		// step in direction to a stack
	 		if(to!=null)
	 		{
	 		if(topIsOwned
	 				&& (to.topChip()==top) 
	 				&& (!variation.removeStacksOf5() || (to.height()<CaptureHeight))	// beware unusual captures
	 				&& (!recordPath || !activePath.contains(to))
	 				)
	 			{
	 			// continuing a lyngk move chain
	 			if(recordPath)	{  	activePath.push(to); }
	 			some |= addLyngkMoves(all,who,to,moving,top,topIsOwned,recordPath);
	 			if(recordPath) { activePath.pop(); to.sweep_counter--; }
	 			else { if(some && (instantWin||(all==null))) { return(some); }}
	 			}
	 		else if(canCombine(moving,to,topIsOwned))
	 		{ if(recordPath) 
	 		  {
	 			  LyngkCell oldPath[] = pathHash.get(to);
	 			  if(oldPath==null ||activePath.size()<oldPath.length)
	 			  {	 
	 			  	 pathHash.put(to,activePath.toArray());
	 			  }
	 			  to.sweep_counter--;	// allow it to be found again
	 		  }
	 		  else if(all==null) { return(true); }
	 		  int newHeight = moving.height()+to.height();
	 		  int limit = variation.heightLimit();
	 		  G.Assert(!to.onBoard || (newHeight<=limit), "illegal stack plan");
	 		  if(all!=null) 
	 		  	{ 
	 		  	  if(robotBoard && (newHeight==InstantWinHeight))
	 		  	  {	// return just the winning move
	 		  		all.clear();  
	 		  		all.push(new LyngkMovespec(moving,to,who)); 
	 		  		instantWin = true;
	 		  		return(true);
	 		  	  }
	 		  	  all.push(new LyngkMovespec(moving,to,who)); 
	 		  	}
	 		  some = true;
	 		}}
	 	}}
	 return(some);
 }
 private boolean addLyngkMoves(CommonMoveStack all,int who,LyngkCell from,LyngkCell moving,boolean recordPath)
 {	boolean some = false;
 	LyngkChip top = moving.topChip();
 	if(top!=null && canBeMovedBy(top,who))
 	{
 	sweep_counter++;
	some |= addLyngkMoves(all,who,from,moving,top,isOwnedBy(top,who),recordPath);
	if(some && (instantWin || (all==null))) { return(some); }
 	}
	return(some);
 }
 
 private boolean addLyngkMoves(CommonMoveStack all,int who)
 {	boolean some = false;
 	instantWin = false;
 	for(int lim = filledCells.size()-1; lim>=0; lim--)
	 	 {	LyngkCell c = filledCells.elementAt(lim);
	 	 	some |= addLyngkMoves(all,who,c,c,false);
	 	 	if(some && (all==null)) 
	 	 		{ return(true); } 
	 	 }
	 	return(some);
 }

 // get a list of the targets on the board 
 public Hashtable<LyngkCell,LyngkMovespec> getTargets()
 {
	 Hashtable<LyngkCell,LyngkMovespec> targets = new Hashtable<LyngkCell,LyngkMovespec>();
	 switch(board_state)
	 {
	 case Pass:
	 default: 
		 break;
	 case PlayOrClaim:
	 case Claim:
	 case Play:
	 	{
	 	CommonMoveStack all = new CommonMoveStack();
	 	if(movingObjectIndex()>=0)
	 		{	getListOfMoves(all,pickedSourceStack.top(),pickedStack);
		 	while(all.size()>0)
		 	{
		 		LyngkMovespec p = (LyngkMovespec)all.pop();
		 		switch(p.op)
		 		{
		 		default: break;
		 		case MOVE_FROM_TO:
		 		case MOVE_BOARD_BOARD: 
		 		{
		 			LyngkCell c = getCell(p.dest,p.to_col,p.to_row);
		 			targets.put(c,p);
		 		}
		 		}
		 	}}
	 		
	 		else 
	 		{	getListOfMoves(all);
		 	while(all.size()>0)
		 	{
		 		LyngkMovespec p = (LyngkMovespec)all.pop();
		 		switch(p.op)
		 		{
		 			default: break;
		 			case MOVE_FROM_TO:
		 			case MOVE_BOARD_BOARD: 
		 			LyngkCell c = getCell(p.source,p.from_col,p.from_row);
		 			targets.put(c,p);
		 		}
		 	}
	 		}

	 	}
	 }
	 return(targets);
 }
 // get a random move, which in this case is differently distributed
 // than the "natural" randomness of simply picking one of the possible
 // moves. The particular choices here have unknown effects, but the
 // overall strength of play seems to be at least as good as the natural
 public commonMove Get_Random_Move(Random rand)
 {
	 instantWin = false;
	 switch(board_state)
	 {
	 default: throw G.Error("Not expecting random move in state ",board_state);
	 case Pass:
	 case Confirm:
	 case Resign:
	 case Claim:	
		 return(null);

	 case PlayOrClaim:
		 //
		 // if you can still claim, do so 1/10 of the time.  This is an
		 // arbitrary number, and untested what effect it might have.
		 //
		 if(rand.nextDouble()<0.1)
		 {	int lim = unclaimedColors.length;
			int start = rand.nextInt(lim);
			for(int i=0; i<lim;i++)
			{	int idx = (start+i)%lim;
				LyngkCell c = unclaimedColors[idx];
				if(c.topChip()!=null)
				{
					return(new LyngkMovespec(c.rackLocation(),
								playerColors[whoseTurn].rackLocation(),
								whoseTurn));
				}
			}
		 }
		//$FALL-THROUGH$
	case Play:
	 {	int lim = filledCells.size();
		int start = rand.nextInt(lim);
		CommonMoveStack all = new CommonMoveStack();
		// if regular moves are possible, select a random starting piece
		// and pick a random one of it's possible moves.  This tends to 
		// over-represent the non-lyngk moves relative to the set of all
		// moves.  It's untested exactly what effect this has on the 
		// outcome, but it's much faster.
		for(int i=0; i<lim;i++)
		{	int idx = (start+i)%lim;
			LyngkCell c = filledCells.elementAt(idx);
			boolean some = addLyngkMoves(all,whoseTurn,c,c,false);
			if(some) 
			{	int n = rand.nextInt(all.size());
				return(all.elementAt(n));
			}
		}
		throw G.Error("Not expcting no moves in state %s",board_state);
	 }
 
	 }
 }
 // entry point for getTargets, plotting the moves for board pieces
 private boolean getListOfMoves(CommonMoveStack all,LyngkCell from,LyngkCell moving)
 {	boolean some = false;
 	//some |= addNormalMoves(all,whoseTurn,from,moving);
 	some |= addLyngkMoves(all,whoseTurn,from,moving,false);
 	return(some);
 }
 
 // fast test to determine if the state is "pass" or "move"
 private boolean hasBoardMoves(int who)
 {	return(addLyngkMoves(null,who));
 }
 
 // true if the player can claim a color and then move.
 // this is only called if it's known he can't move without
 // claiming something, and that he can claim something.
 private boolean hasMovesAfterClaiming(int who)
 {	return(addClaimMoves(null,who,true));
 }
 
 // return true if the player can claim the top color 
 // and then make a move with that color.
 private boolean hasMovesAfterClaiming(int who,LyngkChip top)
 {
	 for(int lim = filledCells.size()-1; lim>=0; lim--)
		{	LyngkCell from = filledCells.elementAt(lim);
			if(from.topChip()==top)
			{
			sweep_counter++;
			boolean some = addLyngkMoves(null,who,from,from,top,true,false);
			
			if(some)
				{ return(true); }
			}
		}
	 return(false);
 }
 // entry point for the robot
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	if(!getListOfMoves(all))
		{ all.push(new LyngkMovespec(MOVE_PASS,whoseTurn)); 
		}
 	return(all);
 }
 
 private boolean getListOfMoves(CommonMoveStack all) // all may be null
 {	boolean some = false;
 	instantWin = false;
 	switch(board_state)
 	{
	case Claim:
 		some |= addClaimMoves(all,whoseTurn,true);
 		break;
 	case PlayOrClaim:
 		some |= addClaimMoves(all,whoseTurn,false);
		//$FALL-THROUGH$
	case Play:
 		some |= addLyngkMoves(all,whoseTurn);
 		break;
 	case Pass:
 		all.push(new LyngkMovespec(MOVE_PASS,whoseTurn));
 		break;
 	default: G.Error("Not expecting GetListOfMoves in state ",board_state);
 	}
 	return(some);
 }



 public commonMove getUCTRandomMove(Random rand)
 {	throw G.Error("Not implemented");
 }

}
