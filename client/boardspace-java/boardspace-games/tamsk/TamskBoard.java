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
package tamsk;

import java.awt.Color;

import static tamsk.Tamskmovespec.*;

import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;
import tamsk.TamskConstants.TamskId;

/**
 * TamskBoard knows all about the game of Tamsk, which is played
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
class TamskTimer implements Digestable
{
	TamskId id;
	boolean ghost = false;
	long capacity = 3*1000*60;
	long count = 0;
	long startTime = 0;
	boolean active = false;
	TamskCell location = null;
	TamskTimer ghostCopy = null;
    
	// constructor
	TamskTimer(TamskChip in,TamskId tid,long cap)
	{	this(tid,cap);
		tid.chip = in;
	}
	// constructor
	TamskTimer(TamskId tid,long cap)
	{
		id = tid;
		capacity = cap;
		
	}
	public long timeRemaining(long gameTime)
	{
   		long timeUsed = gameTime - startTime;
		long remaining = (count - timeUsed);
		return remaining;
	}
	
	public TamskTimer getCopy() 
	{
		if(ghostCopy==null) { ghostCopy=new TamskTimer(id,capacity); ghostCopy.ghost = true; }
		return ghostCopy;
	}
	public void flip(long gameTime)
	{
		if(active)
		{	long used = (gameTime - startTime);
			long remaining = count-used;
			count = Math.min(capacity,Math.max(0,capacity-remaining));
			startTime = gameTime;
		}
		else { active = true;
			   startTime = gameTime;
			   count = capacity;
		}
	}
	public void restart(long gameTime)
	{
		active = false;
		flip(gameTime);
	}
	public boolean isExpired(long now)	// now is a game time
	{	if(!active) { return false; }
		long timeUsed = now - startTime;
		long remaining = (count - timeUsed);
		return remaining<0;
	}
	public void reInit()
	{
		active = false;
		count = capacity;
		startTime = 0;
		location = null;
	}
	public boolean sameContents(TamskTimer o)
	{
		return (active==o.active) 
				&& (startTime==o.startTime)
				&& (count==o.count);
	}
	public void copyFrom(TamskTimer o)
	{	ghostCopyFrom(o);
		if(o.ghostCopy!=null)
		{
			getCopy().copyFrom(o.ghostCopy);
		}
	}
	public void ghostCopyFrom(TamskTimer o)
	{
		capacity = o.capacity;
		count = o.count;
		active = o.active;
		location = o.location;
		startTime = o.startTime;
	}
	public String toString() { return "<timer "+id+">";}
	
	public long Digest(Random r) {
		return id.chip.Digest(r)*id.ordinal() ^ (r.nextLong()*(active ? 35235 : 352646));
	}
	public double duration() {
		return capacity;
	}
}
class TimerStack extends OStack<TamskTimer> implements Digestable
{	TamskTimer fullSet[]=null;
	TimerStack(TamskTimer[] st) { fullSet = st; }
	public void reInit()
	{
		clear();
	    for(TamskTimer id : fullSet) {id.reInit(); push(id); }
	}
	public TamskTimer[] newComponentArray(int sz) {
		return new TamskTimer[sz];
	}


	public long Digest(Random r) {
		long v = 0;
		for(int i=0;i<size();i++) { v ^= elementAt(i).Digest(r); }
		return v;
	}
	public void copyFrom(TimerStack other)
	{	clear();
		for(int i=0,len = fullSet.length;i<len;i++) 
			{ fullSet[i].copyFrom(other.fullSet[i]);
			}
		for(int i=0,len=other.size();i<len;i++)
		{
			push(findTimer(other.elementAt(i).id));
		}
	}
	public TamskTimer findTimer(TamskTimer ot)
	{	return findTimer(ot.id);
	}
	public TamskTimer findTimer(TamskId otherId)
	{
		for(TamskTimer t : fullSet)
		{
			if(t.id==otherId) { return(t);}
		}
		throw G.Error("No match for %s",otherId);
	}
	public boolean sameContents(TimerStack other)
	{	boolean ok = size()==other.size();
		if(ok)
			{ for(int i=0;i<size();i++) 
				{ ok &= elementAt(i).sameContents(other.elementAt(i)); 
				}
			}
		return ok;
	}
}
class TamskBoard 
	extends hexBoard<TamskCell>	// for a square grid board, this could be rectBoard or squareBoard 
	implements BoardProtocol,TamskConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
	static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	TamskVariation variation = TamskVariation.tamsk;
	private TamskState board_state = TamskState.Puzzle;	
	private TamskState unresign = null;	// remembers the orignal state when "resign" is hit
	private boolean robotBoard = false;
	private long robotDoneTime = FAST_TIMER_TIME;
	public long robotDelayTime = 10*1000;
	public boolean robotRandomPhase = false;
    private int prevLastPicked = -1;
    private int prevLastDropped = -1;
	
	public TamskState getState() { return(board_state); }
	
	/*
	 * all events in Tamsk occur at a particular time, measured in milliseconds from the start
	 * of the game.
	 */
	private boolean timeRunning = false;// if true, the clock is running
	public boolean timeRunning() { return timeRunning; }
	
	public long masterGameTime = 0;		// the current game event time
	public long masterClockTime = 0;	// the real time corresponding to masterGameTime
	public long turnStartTime = 0;		// time when the most recent move started
	public long turnExpiredTime = 0;	// future time at which a 15 second timer expires
    public boolean passOnTimerExpired = false;	// true if the 15 second time is counting

	// time since the current move started 
	public long shotTime() { return (masterGameTime-turnStartTime); }
	
	// this is used by the robot to advance time and therefore make decisions based on a
	// future state of the times
	public void skipTime(long n)
	{
		masterGameTime += n;
		
	}
	public void stopTime() 
	{	long now = G.Date();
		masterGameTime += extraTime(now);
		masterClockTime = now;
		timeRunning = false;
	}
	public void restartTime()
	{	masterClockTime = G.Date();
		timeRunning = true;
	}

	public long extraTime(long now)
	{	if(masterClockTime==0) { masterClockTime = now; }
		if(timeRunning) { return now-masterClockTime; }
		else { return 0; }
	}
	
	public boolean fastTimerExpired(long now)
	{	if(passOnTimerExpired)
		{	// effectively, the fast timer is for display only,
			// but turnExpiredTime ought to be the same  as testing
			// fastTimer.isExpired(now);
			return (now>turnExpiredTime);
		}
		return false;
	}
	public boolean allTimersExpired(long now)
	{
		for(TamskTimer t : playerTimers(whoseTurn))
		{
			if(!t.isExpired(now)) { return false; }
		}
		return true;
	}
	public boolean pickedTimerIsExpired(long now)
	{
		if(pickedTimer!=null)
		{	TamskTimer t = findTimer(pickedTimer);
			return (t.isExpired(now));
		}
		return false;
	}
	// if the player has placed an hourglass but not clicked on done,
	// it continues ticking and can expire.  In that case, if he tried
	// to take the move back the timer would be lost.
	public boolean currentTimerExpired(long now)
	{	if(board_state==TamskState.Confirm)
		{
		TamskCell d = getDest();
		if(d!=null)
			{
			TamskTimer t = findTimer(d.timer);
			return (t!=null && t.isExpired(now));
			}
		}
		return false;
	}

    public long officialGameTime()
    {
    	return masterGameTime;
    }

   /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(TamskState st) 
	{ 	unresign = (st==TamskState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

    private TamskId playerColor[]={TamskId.White,TamskId.Black};    
    private TamskChip playerChip[]={TamskChip.White,TamskChip.Black};
    private TamskCell playerRing[]=new TamskCell[2];
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public TamskChip getPlayerChip(int p) { return(playerChip[p]); }
	public TamskId getPlayerColor(int p) { return(playerColor[p]); }
	public TamskCell getPlayerRing(int p) { return(playerRing[p]); }
	public TamskChip getCurrentPlayerChip() { return(playerChip[whoseTurn]); }

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
    public TamskChip pickedObject = null;
    public TamskId pickedTimer = null;
    
    public TamskChip lastPicked = null;
    private TamskCell blackRings = null;	// dummy source for the chip pools
    private TamskCell whiteRings = null;
    private TamskCell blackTimer = null;
    private TamskCell whiteTimer = null;
    private TamskCell playerTimer[] = new TamskCell[2];
    public TamskCell getPlayerTimer(int n) { return(playerTimer[n]); }
    
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
	public TimerStack blackTimerStack = new TimerStack(new TamskTimer[]{ new TamskTimer(TamskChip.Black,TamskId.Timer_0B,SLOW_TIMER_TIME),
    		new TamskTimer(TamskChip.Black,TamskId.Timer_1B,SLOW_TIMER_TIME),
    		new TamskTimer(TamskChip.Black,TamskId.Timer_2B,SLOW_TIMER_TIME),	
    }); 
	public TimerStack whiteTimerStack = new TimerStack(new TamskTimer[]{ new TamskTimer(TamskChip.White,TamskId.Timer_0W,SLOW_TIMER_TIME),
		new TamskTimer(TamskChip.White,TamskId.Timer_1W,SLOW_TIMER_TIME),
		new TamskTimer(TamskChip.White,TamskId.Timer_2W,SLOW_TIMER_TIME),	
	}); 
	
	private TamskTimer[] playerTimers(int who)
	 {
		 return (who==0) ? whiteTimerStack.fullSet : blackTimerStack.fullSet;
	 }
	 

    TamskTimer fastTimer = new TamskTimer(TamskChip.Neutral,TamskId.Timer_F,FAST_TIMER_TIME);
    
    public int skipTurns[] = {0,0};
    public TamskTimer findTimer(TamskId id)
    {	if(id==null) { return null;}
    	switch(id)
    	{
    	case Timer_0B:
    	case Timer_1B:
    	case Timer_2B:
    		return blackTimerStack.findTimer(id);
    	case Timer_0W:
    	case Timer_1W:
    	case Timer_2W:
    		return whiteTimerStack.findTimer(id);
    	case Timer_F:
    		return fastTimer;
    	default: throw G.Error("Timer %s not found",id);
    	}
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

    private TamskState resetState = TamskState.Puzzle; 
    public DrawableImage<?> lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public TamskCell newcell(char c,int r)
	{	return(new TamskCell(TamskId.BoardRing,c,r));
	}
	
	// constructor 
    public TamskBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = GRIDSTYLE;
        setColorMap(map, players);
        
		Random r = new Random(734687);
		// do this once at construction
	    blackRings = new TamskCell(r,TamskId.BlackRing);
	    blackTimer = new TamskCell(r,TamskId.Black);
	    blackTimer.addChip(TamskChip.Black);
	    whiteRings = new TamskCell(r,TamskId.WhiteRing);
	    whiteTimer = new TamskCell(r,TamskId.White);
	    whiteTimer.addChip(TamskChip.White);
        doInit(init,key,players,rev); // do the initialization 
	    loadPositions();
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
 		setState(TamskState.Puzzle);
		variation = TamskVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		gametype = gtype;
		prevLastPicked = -1;
		prevLastDropped = -1;
		
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case tamsk:
		case tamsk_u:
		case tamsk_f:
			// using reInitBoard avoids thrashing the creation of cells 
			// when reviewing games.
			reInitBoard(variation.firstInCol,variation.ZinCol,null);
			// or initBoard(variation.firstInCol,variation.ZinCol,null);
			// Random r = new Random(734687);	// this random is used to assign hash values to cells, common to all games of this type.
			// allCells.setDigestChain(r);		// set the randomv for all cells on the board
		}

	    playerRing[FIRST_PLAYER_INDEX] = whiteRings; 
	    playerRing[SECOND_PLAYER_INDEX] = blackRings; 
	    playerTimer[FIRST_PLAYER_INDEX] = whiteTimer;
	    playerTimer[SECOND_PLAYER_INDEX] = blackTimer;
	    reInit(playerRing);
	    for(int i=0;i<NRINGS;i++) 
	    	{ whiteRings.addChip(TamskChip.Ring);
	    	  blackRings.addChip(TamskChip.Ring);
	    	}
	    blackTimerStack.reInit();
	    whiteTimerStack.reInit();
	    fastTimer.reInit();
	    masterGameTime = 0;
	    turnStartTime = -1;
	    turnExpiredTime = -1;
	    animationStack.clear();
        moveNumber = 1;
	    passOnTimerExpired = false;
	    AR.setValue(skipTurns,0);
	    whoseTurn = FIRST_PLAYER_INDEX;
	    droppedDestStack.clear();
	    pickedSourceStack.clear();
	    stateStack.clear();
	    
	    pickedObject = null;
	    pickedTimer = null;
	    resetState = null;
	    lastDroppedObject = null;
	    int map[]=getColorMap();
		playerColor[map[0]]=TamskId.White;
		playerColor[map[1]]=TamskId.Black;
		playerChip[map[0]]=TamskChip.White;
		playerChip[map[1]]=TamskChip.Black;
        // place the initial timers
        positionTimers( new int[][]{{'A',1},{'G',1},{'D',7}},whiteTimerStack);
        positionTimers(new int[][]{{'D',1},{'G',4},{'A',4}},blackTimerStack);


        // note that firstPlayer is NOT initialized here
    }
    private void positionTimers(int bloc[][],TimerStack timers)
    {
    	for(int loc[] : bloc)
        {
        	TamskCell c = getCell((char)loc[0],loc[1]);
        	TamskTimer time = timers.pop();
        	time.location = c;
        	c.timer = time.id;
        }
    }
    /** create a copy of this board */
    public TamskBoard cloneBoard() 
	{ TamskBoard dup = new TamskBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((TamskBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(TamskBoard from_b)
    {
        super.copyFrom(from_b);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        copyFrom(whiteRings,from_b.whiteRings);		// this will have the side effect of copying the location
        copyFrom(blackRings,from_b.blackRings);		// from display copy boards to the main board
        getCell(playerRing,from_b.playerRing);
        stateStack.copyFrom(from_b.stateStack);
        blackTimerStack.copyFrom(from_b.blackTimerStack);
        whiteTimerStack.copyFrom(from_b.whiteTimerStack);
        fastTimer.copyFrom(from_b.fastTimer);
        passOnTimerExpired = from_b.passOnTimerExpired;
        AR.copy(skipTurns,from_b.skipTurns);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        lastPicked = null;
        masterClockTime = from_b.masterClockTime;
        masterGameTime =from_b.masterGameTime;
        turnStartTime = from_b.turnStartTime;
        turnExpiredTime = from_b.turnExpiredTime;
        
        timeRunning = from_b.timeRunning;
        
        AR.copy(playerColor,from_b.playerColor);
        AR.copy(playerChip,from_b.playerChip);
 
        robotBoard = from_b.robotBoard;
        robotDoneTime = from_b.robotDoneTime;
        robotDelayTime = from_b.robotDelayTime;
        robotRandomPhase = from_b.robotRandomPhase;
        
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((TamskBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(TamskBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor mismatch");
        G.Assert(AR.sameArrayContents(playerChip,from_b.playerChip),"playerChip mismatch");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedsourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(sameCells(playerRing,from_b.playerRing),"player ring mismatch");
        G.Assert(blackTimerStack.sameContents(from_b.blackTimerStack),"black timer matches");
        G.Assert(whiteTimerStack.sameContents(from_b.whiteTimerStack),"white timer matches");
        G.Assert(fastTimer.sameContents(from_b.fastTimer),"fastTimer mismatch");
        G.Assert(passOnTimerExpired==from_b.passOnTimerExpired,"fastTimerPass mismatch");
        G.Assert(AR.sameArrayContents(skipTurns,from_b.skipTurns),"skipturns mismatch");
        G.Assert(masterGameTime==from_b.masterGameTime,"masterGameTime mismatch");
        G.Assert(turnStartTime==from_b.turnStartTime,"turnStartTime mismatch");
        G.Assert(turnExpiredTime==from_b.turnExpiredTime,"turnExpiredTime mismatch");
        G.Assert(masterClockTime==from_b.masterClockTime,"masterClockTime mismatch");
        G.Assert(timeRunning == from_b.timeRunning, "timeRunning mismatch");
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
		v ^= Digest(r,blackTimerStack);
		v ^= Digest(r,whiteTimerStack);
		v ^= Digest(r,fastTimer);
		v ^= Digest(r,skipTurns);
		v ^= Digest(r,turnExpiredTime);
		v ^= Digest(r,timeRunning);
		v ^= Digest(r,passOnTimerExpired);
		v ^= r.nextLong()*(board_state.ordinal()*10);
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
        case Confirm:
        case Resign:
            do { moveNumber++; //the move is complete in these states
                skipTurns[whoseTurn]--;
            	setWhoseTurn(nextPlayer[whoseTurn]);
            } while (skipTurns[whoseTurn]>0);
            skipTurns[whoseTurn] = 0;
            passOnTimerExpired = false;
            turnExpiredTime = -1;
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

    //
    // accept the current placements as permanent
    //
    public void acceptPlacement()
    {	
        droppedDestStack.clear();
        pickedSourceStack.clear();
        stateStack.clear();
        passOnTimerExpired = false;
        turnExpiredTime = -1;
        pickedObject = null;
        pickedTimer = null;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private TamskCell unDropObject()
    {	TamskCell rv = droppedDestStack.pop();
    	setState(stateStack.pop());
    	pickedTimer = rv.timer;
    	rv.lastDropped = prevLastDropped;
    	prevLastDropped = -1;
    	if(pickedTimer!=null) 
    	{ findTimer(pickedTimer).location = rv; 
    	  pickedObject = pickedTimer.chip;
    	  rv.timer = null;
          if(board_state==TamskState.Play)
          {
        	  getPlayerRing(whoseTurn).addChip(rv.removeTop());
          }
    	}
    	else {
        	pickedObject = rv.removeTop(); 	
    	}
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	TamskCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	if(pickedObject==TamskChip.Ring) { rv.addChip(pickedObject); }
    	rv.timer = pickedTimer;
    	rv.lastPicked = prevLastPicked;
    	prevLastPicked = -1;
    	if(pickedTimer!=null) { findTimer(pickedTimer).location = rv; pickedTimer = null; }
    	pickedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(TamskCell c,replayMode replay)
    {
       droppedDestStack.push(c);
       stateStack.push(board_state);
       prevLastDropped = c.lastDropped;
       c.lastDropped = moveNumber;
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting dest " + c.rackLocation);
        case Black:
        case White:		// back in the pool, we don't really care where
        	pickedObject = null;
        	pickedTimer = null;
            break;
        case WhiteRing:
        case BlackRing:
        case BoardRing:	// already filled board slot, which can happen in edit mode
        	switch(pickedObject.id) {
        	case Ring:
        		c.addChip(pickedObject);
        		break;
        	case White:
        	case Black:
        		c.timer = pickedTimer;
        		if(pickedTimer!=null)
        		{	TamskTimer timer = findTimer(pickedTimer);
        			timer.location = c;
        			TamskTimer ghost = timer.getCopy();
        			ghost.ghostCopyFrom(timer);
        			ghost.flip(masterGameTime);
        			
    	            if(board_state==TamskState.Play)
    	            {	TamskCell ring = getPlayerRing(whoseTurn);
    	            	c.addChip(ring.removeTop());
    		            if(replay.animate)
    		            {
    		            	animationStack.push(ring);
    		            	animationStack.push(c);
    		            }
    	            }
    			
        		}
        		pickedTimer = null;
        		break;
        	default: break;
         	}
        	pickedObject = null;
            break;
        }
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(TamskCell c)
    {	return(droppedDestStack.top()==c);
    }
    public TamskCell getDest()
    {	return(droppedDestStack.top());
    }
 
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { TamskChip ch = pickedObject;
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
    private TamskCell getCell(TamskId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source " + source);
        case White:
        	return whiteTimer;
        case Black: 
        	return blackTimer;
        case BoardRing:
        case BoardLocation:
        	return(getCell(col,row));
        case BlackRing:
        	return(blackRings);
        case WhiteRing:
        	return(whiteRings);
        } 	
    }
    /**
     * this is called when copying boards, to get the cell on the new (ie current) board
     * that corresponds to a cell on the old board.
     */
    public TamskCell getCell(TamskCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(TamskCell c,TamskId where)
    {	pickedSourceStack.push(c);
    	stateStack.push(board_state);
        switch (where)
        {
        default:
        	throw G.Error("Not expecting rackLocation " + c.rackLocation);
        case BoardLocation:
        	pickedTimer = c.timer;
        	pickedObject = pickedTimer.chip;
        	prevLastPicked = c.lastPicked;
        	c.lastPicked = moveNumber;
        	c.timer = null;
        	if(pickedTimer!=null) { findTimer(pickedTimer).location = null; }
        	break;
        case BoardRing:
        	{
            lastPicked = pickedObject = c.removeTop();
         	lastDroppedObject = null;
        	}
            break;
        case Black:
        	lastPicked = pickedObject = c.topChip();
        	pickedTimer = blackTimerStack.pop().id;
        	break;
       case White:
        	lastPicked = pickedObject = c.topChip();
        	pickedTimer = whiteTimerStack.pop().id;
        	break;
        case BlackRing:
        case WhiteRing:
        	lastPicked = pickedObject = c.removeTop();
        }
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(TamskCell c)
    {	return(c==pickedSourceStack.top());
    }
    public TamskCell getSource()
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
			setState(TamskState.Confirm);
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
    	case Gameover: 
    		break;
     	case Confirm:
    	case Puzzle:
    	case Play:
    		if(hasMoves(whoseTurn,masterGameTime)) { setState(TamskState.Play); }
    		else { 	setNextPlayer(replay);
    				int next = nextPlayer[whoseTurn];
    				int myRings = getPlayerRing(whoseTurn).height();
    				int hisRings = getPlayerRing(next).height();
    				// keep moving if you're behind
    				if(hasMoves(whoseTurn,masterGameTime) && (myRings>=hisRings)) { setState(TamskState.Play); }
    				else {
    				   setState(TamskState.Gameover);   
    				   timeRunning = false;
    				   win[whoseTurn] = myRings<hisRings;
    				   win[next] = hisRings<myRings;
    			   }
    		}
     		break;
    	}
       	resetState = board_state;
    }
    public boolean showTimers()
    {
    	return variation.showTimers;
    	
    }
    public boolean showFastTimer()
    {
    	return variation==TamskVariation.tamsk_f;
    }
    private void doDone(replayMode replay)
    {	
    	if(showTimers())
    	{
    	TamskCell dest = droppedDestStack.top();
    	if(dest!=null)
    		{
    			TamskTimer timer = findTimer(dest.timer);
    			if(timer!=null) 
    				{ timer.ghostCopyFrom(timer.getCopy());
    				}
    		}
    	}
        acceptPlacement();

        if (board_state==TamskState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
            timeRunning = false;
    		setState(TamskState.Gameover);
        }
        else
        {	
            turnStartTime = masterGameTime;
        	setNextPlayer(replay);
        	setNextStateAfterDone(replay);
        }
    }
    // do the things associated with the fifteen second timer.
    // this is a separate function so it can be called directly
    // from the robot
    public void doFifteen()
    {
     	fastTimer.restart(masterGameTime);
    	turnExpiredTime = FAST_TIMER_TIME+masterGameTime;
    	passOnTimerExpired = true;
    }
    
    public boolean canStartFastTimer(long gameTime)
    {
        if(showFastTimer())
    	{
    	TamskTimer ft = fastTimer;
    	boolean cantStart = passOnTimerExpired || (ft.active && !ft.isExpired(gameTime));
    	return !cantStart;
		}
        return false;
    }
    public boolean ignoreFastTimer(long gameTime)
    {
    	return !passOnTimerExpired && !fastTimer.isExpired(gameTime);
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	Tamskmovespec m = (Tamskmovespec)mm;
    
    	masterGameTime = m.gameTime;
    	if(replay==replayMode.Live) { masterClockTime = G.Date(); }
        if(replay.animate) { animationStack.clear(); }

        //G.print("E "+m+" "+Digest());
        switch (m.op)
        {
        case MOVE_DONE:
        	
         	doDone(replay);

            break;
            
        case MOVE_DELAY:
        	masterGameTime += m.to_row;
        	break;
        	
        case MOVE_TIMEEXPIRED:
           	if(passOnTimerExpired)
        	{	
           	passOnTimerExpired = false;
           	if(pickedObject!=null) { unPickObject();}  	// if he hasn't dropped the timer, un-pick it    	
        	passOnTimerExpired = false;
        	skipTurns[whoseTurn]= (board_state==TamskState.Confirm) ? 3 : 2;
        	doDone(replay);
        	}
           	else { m.rejected = true;}
        	break;
        case MOVE_STOPTIME:
        	timeRunning = false;
         	break;
        case MOVE_STARTTIME:
        	timeRunning = true;
        	break;
        case MOVE_FIFTEEN:
        	doFifteen();
        	m.chip = TamskChip.Neutral;
        	
        	break;
        case MOVE_FROM_TO:
        	TamskCell srct = getCell(m.from_col,m.from_row);
        	pickObject(srct,m.source);
        	m.chip = pickedObject;
	        if(pickedTimer!=null)
	        {
	        	TamskTimer timer = findTimer(pickedTimer);
	        	m.flipTime = timer.timeRemaining(masterGameTime); 
	        	if(timer.isExpired(masterGameTime))
	        	{	// too late!
	        		unPickObject();
	        		m.rejected = true;
	        		break;
	        	}
	        }

			//$FALL-THROUGH$
		case MOVE_DROPB:
        case MOVE_DROPRINGB:
        	{
			TamskChip po = pickedObject;
			TamskCell dest =  getCell(m.to_col,m.to_row);
			if(isSource(dest)) 
				{ unPickObject(); 
				}
				else 
				{
	            dropObject(dest,replay);
	            lastDroppedObject = (dest.timer!=null)
	            					? dest.timer.chip
	            					: TamskChip.getRingOverlay(dest);
	            /**
	             * if the user clicked on a board space without picking anything up,
	             * animate a stone moving in from the pool.  For Hex, the "picks" are
	             * removed from the game record, so there are never picked stones in
	             * single step replays.
	             */
	            if(replay==replayMode.Single || (replay.animate && ((m.op==MOVE_FROM_TO)||(po==null))))
	            	{ animationStack.push(getSource());
	            	  animationStack.push(dest); 
	            	}
	            setNextStateAfterDrop(replay);
				}
        	}
             break;

        case MOVE_PICK:
        case MOVE_PICKRING:
        case MOVE_PICKRINGB:
 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
 			TamskCell src = getCell(m.source,m.to_col,m.to_row);
 			if(isDest(src)) { unDropObject(); }
 			else
 			{
        	// be a temporary p
        	pickObject(src,m.source);
        	m.chip = pickedObject;
        	if(pickedTimer!=null)
        		{	TamskTimer timer = findTimer(pickedTimer);
        			m.flipTime = timer.timeRemaining(masterGameTime); 
        			m.chip = pickedObject;
        		}

        	switch(board_state)
        	{
        	case Puzzle:
         		break;
        	case Confirm:
        		setState(TamskState.Play);
        		break;
        	default: ;
        	}}}
            break;
 		case MOVE_DROPRING:
        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
            TamskCell dest = getCell(m.source,m.to_col,m.to_row);
            if(isSource(dest)) { unPickObject(); }
            else 
            	{
		        if(replay==replayMode.Live)
	        	{ lastDroppedObject = pickedObject.getAltDisplayChip(dest);
	        	  //G.print("last ",lastDroppedObject); 
	        	}      	
            	dropObject(dest,replay); 
            
            	}
        	}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            timeRunning = true;
            turnStartTime = masterGameTime;
            int nextp = nextPlayer[whoseTurn];
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(TamskState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ timeRunning = false;
               	  setState(TamskState.Gameover); 
               	}
            else {  setNextStateAfterDone(replay); }

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?TamskState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
        	timeRunning = false;
            setState(TamskState.Puzzle);
 
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(TamskState.Gameover);
    	   break;

        default:
        	cantExecute(m);
        }
        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }

        //G.print("X "+m+" "+Digest());
        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }

    // legal to hit the chip storage area
    public boolean legalToHitChips(TamskCell c,int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting Legal Hit state " + board_state);
        case Play:
        case Confirm:
		case Resign:
		case Gameover:
			return(false);
        case Puzzle:
        	if(pickedObject==null)
        	{	TamskChip top = c.topChip();
        		if(top!=null)
        		{
        			switch(top.id)
        			{
        				case Ring: return true;
        				case Black: return blackTimerStack.size()>0;
        				case White: return whiteTimerStack.size()>0;
        				default: return false;
        			}
     
        		}
        		return false;
        	}      	
        	else
        	{	return pickedObject==c.topChip();
        	}
        }
  }

    public boolean legalToHitBoard(TamskCell c,Hashtable<TamskCell,Tamskmovespec> targets )
    {	if(c==null) { return(false); }
        switch (board_state)
        {
		case Play:
			return(targets.get(c)!=null || isDest(c) || isSource(c));
		case Gameover:
		case Resign:
			return(false);
		case Confirm:
			return(isDest(c));
        default:
        	throw G.Error("Not expecting Hit Board state " + board_state);
        case Puzzle:
        	if(pickedObject!=null)
        	{
        		if(pickedObject==TamskChip.Ring)
        			{	return c.height()<c.maxRings; 
        			}
        		else { return c.timer == null; }
        	}
        	else
        	{
        		return c.height()>0 || c.timer!=null;
        	}
        }
    }
    
    public long minimumTimer(int who)
    {
    	long min = passOnTimerExpired ? FAST_TIMER_TIME : SLOW_TIMER_TIME;
    	TamskTimer timers[] = playerTimers(who);
    	for(TamskTimer t : timers)
    	{
    		if(t.active)
    		{	long ex = t.timeRemaining(masterGameTime);
    			if(ex>0) { min = Math.min(ex,min); }
    		}
    	}
    	return min;
    }
    public long maximumTimer(int who)
    {
    	long max = 0;
    	TamskTimer timers[] = playerTimers(who);
    	for(TamskTimer t : timers)
    	{
    		if(t.active)
    		{
    			max = Math.max(t.timeRemaining(masterGameTime),max);
    		}
    	}
    	return max;
    }
 
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Tamskmovespec m)
    {
       // to undo state transistions is to simple put the original state back.
        
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        m.gameTime = masterGameTime;
        Execute(m,replayMode.Replay);
        // crudely advance the clock
        if(DoneState()) 
        	{ doDone(replayMode.Replay); 
        	  skipTime(robotDoneTime); 
        	  if(!robotRandomPhase) { skipTime(robotDoneTime); }
        }
        else if(!robotRandomPhase)
        {
        switch(m.op)
        {
        case MOVE_DELAY: 
        		break;
        case MOVE_DONE:	
        		skipTime(robotDoneTime);
        		break;
        default: 
        		skipTime(2000);
        	break;
        }}
        acceptPlacement();
       
    }
 

 private boolean addMovesFrom(CommonMoveStack all, TamskCell c, int who)
 {	boolean some = false;
	 for(int direction = 0,n=c.geometry.n; direction<n; direction++)
	 {
		 TamskCell d = c.exitTo(direction);
		 if((d!=null) && (d.timer==null) && (d.height()<d.maxRings))
		 {	if(all==null) { return true; }
			 all.push(new Tamskmovespec(MOVE_FROM_TO,c,d,who));
			 some |= true;
		 }
	 }
	 return some;
 }
 
 private boolean addMoves(CommonMoveStack all,int who,long now)
 {	boolean some = false;
 
 	boolean requireEmpty = false;
 	boolean requireLive = variation.showTimers;
	TamskTimer[] timers = playerTimers(who);
	long mintime = robotBoard ? SLOW_TIMER_TIME-maximumTimer(nextPlayer[whoseTurn]) : SLOW_TIMER_TIME;
	long skipTimer = SLOW_TIMER_TIME*2;
	boolean show = showTimers();
	if(show)
		{for(TamskTimer t : timers)
		{	TamskCell loc = getCell(t.location);	// timers may point to cells on the master board
			requireEmpty |= loc!=null && loc.height()==0;
		}}

	if(robotBoard && show)
 	{
 			// if any timer is on an empty spot, one of them has to be moved.
 			long minTimer = minimumTimer(whoseTurn);
 			if(minTimer<30*1000) 
 				{ skipTimer = 30*1000; 
 			      //G.print("Considering only short timers");
 				}
 	}
 	TamskCell ring = getPlayerRing(who);
 	if(ring.height()>0)
		{
		for(TamskTimer timer : timers)
			{
				TamskCell c =getCell(timer.location); // timers may point to cells on the master board
				if(robotBoard)
				{	double remaining = timer.timeRemaining(now);
					if(remaining>=skipTimer) 
						{ c = null; 
						}
					else if(!requireEmpty 
							&& showTimers() 
							&& !showFastTimer()
							&& (remaining>mintime))
						{
						c = null;	// skip
						}
				}
				if(c!=null
						&& c.onBoard
						&& (!requireEmpty || c.height()==0) 
						&& !(requireLive && timer.isExpired(now)))
				{
					some |= addMovesFrom(all,getCell(c.col,c.row),who);
				}
			}
		}
	return some;
 }
 
 private boolean hasMoves(int who,long now)
 {
	 return(addMoves(null,who,now));
 }
 CommonMoveStack  GetListOfMoves(long now)
 {	CommonMoveStack all = new CommonMoveStack();
 	switch(board_state)
 	{
 	case Puzzle:
 		{int op = pickedObject!=null ? MOVE_DROPB : MOVE_PICKB; 	
 			for(TamskCell c = allCells;
 			 	    c!=null;
 			 	    c = c.next)
 			 	{	if(c.topChip()==null)
 			 		{all.addElement(new Tamskmovespec(op,c.col,c.row,whoseTurn));
 			 		}
 			 	}
 		}
 		break;
 	case Gameover:
 	case Play:
 		{
 		if(pickedObject!=null)
 		{
 			addMovesFrom(all,getSource(),whoseTurn);
 		}
 		else
 		{	
 		addMoves(all,whoseTurn,now);
 		if(robotBoard && (variation!=TamskVariation.tamsk_u))
 			{
 			long minTimer = minimumTimer(whoseTurn);
 			if(robotDelayTime>0)
 				{ if(minTimer-2000>robotDelayTime) 
 					{	all.push(new Tamskmovespec(MOVE_DELAY,'@',(int)robotDelayTime,whoseTurn)); 
 					}
 				}
 			//if(minTimer>20*1000) {  all.push(new Tamskmovespec(MOVE_DELAY,'@',20*1000,whoseTurn)); }
 			}
 		if(all.size()==0) { all.push(new Tamskmovespec(MOVE_DONE,whoseTurn)); }
 		  		
 		}
 		}
 		break;
 	case Resign:
 	case Confirm:
 		all.push(new Tamskmovespec(MOVE_DONE,whoseTurn));
 		break;
 	default:
 			G.Error("Not expecting state ",board_state);
 	}
 	return(all);
 }
 
 public void initRobotValues(long doneTime,long delayTime)
 {	robotBoard = true;
 	robotDoneTime = doneTime;
 	robotDelayTime = delayTime;
 	robotRandomPhase = false;

 }

 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(!Character.isDigit(txt.charAt(0)))
	 	{ 
	 	xpos -= cellsize/2;
	 	}

 	GC.Text(gc, false, xpos, ypos, -1, 0,Color.yellow, null, txt);
 }
 /**
  *  get the board cells that are valid targets right now
  * @return
  */
 public Hashtable<TamskCell, Tamskmovespec> getTargets(long now) 
 {
 	Hashtable<TamskCell,Tamskmovespec> targets = new Hashtable<TamskCell,Tamskmovespec>();
 	CommonMoveStack all = GetListOfMoves(now);
 	for(int lim=all.size()-1; lim>=0; lim--)
 	{	Tamskmovespec m = (Tamskmovespec)all.elementAt(lim);
 		if(pickedObject!=null)
 		{
 	 		switch(m.op)
 	 		{
 	 		case MOVE_FROM_TO:
 	 		case MOVE_DROPB:
 	 			targets.put(getCell(m.to_col,m.to_row),m);
 	 			break;
 	 		case MOVE_DONE:
 	 			break;
 	 		default: G.Error("Not expecting "+m);
 	 		
 	 		}
 			
 		}
 		else
 		{
 		switch(m.op)
 		{
 		case MOVE_PICKB:
 		case MOVE_FROM_TO:
  			targets.put(getCell(m.from_col,m.from_row),m);
 			break;
 		case MOVE_DONE:
 			break;

 		default: G.Error("Not expecting "+m);
 		}
 		}
 	}
 	
 	return(targets);
 }
 
 void pos(int rings,char col,int row, double px, double py)
 {
	 TamskCell c = getCell(col,row);
	 c.xpos = px;
	 c.ypos = py;
	 c.maxRings = rings;
 }
 void loadPositions()
 {
	 pos(1,'A',1,0.376,0.295);
	 pos(1,'B',1,0.458,0.295);
	 pos(1,'C',1,0.538,0.295);
	 pos(1,'D',1,0.62,0.295);
	 
	 pos(1,'A',2,0.345,0.394);
	 pos(2,'B',2,0.420,0.394);
	 pos(2,'C',2,0.498,0.394);
	 pos(2,'D',2,0.575,0.394);
	 pos(1,'E',1,0.65,0.394);
	 
	 pos(1,'A',3,0.31,0.482);
	 pos(2,'B',3,0.385,0.482);
	 pos(3,'C',3,0.459,0.482);
	 pos(3,'D',3,0.538,0.482);
	 pos(2,'E',2,0.61,0.482);
	 pos(1,'F',1,0.69,0.482);

	 pos(1,'A',4,0.28,0.57);
	 pos(2,'B',4,0.354,0.57);
	 pos(3,'C',4,0.428,0.57);
	 pos(4,'D',4,0.50,0.57);
	 pos(3,'E',3,0.574,0.57);
	 pos(2,'F',2,0.648,0.57);
	 pos(1,'G',1,0.72,0.57);
	 
	 pos(1,'B',5,0.325,0.65);
	 pos(2,'C',5,0.398,0.65);
	 pos(3,'D',5,0.466,0.65);
	 pos(3,'E',4,0.54,0.65);
	 pos(2,'F',3,0.61,0.65);
	 pos(1,'G',2,0.68,0.65);

	 pos(1,'C',6,0.365,0.727);
	 pos(2,'D',6,0.436,0.727);
	 pos(2,'E',5,0.505,0.727);
	 pos(2,'F',4,0.576,0.727);
	 pos(1,'G',3,0.64,0.727);
	 
	 pos(1,'D',7,0.405,0.8);
	 pos(1,'E',6,0.47,0.80);
	 pos(1,'F',5,0.54,0.80);
	 pos(1,'G',4,0.608,0.80);

 }
 public int cellToX(TamskCell c)
 {		//return super.cellToX(c);
	 int w = G.Width(boardRect);
	 // 24/20 is a fudge factor from when we changed the 
	 // aspect ratio of the board rectangle from 24/15 to 20/15.
	 //
	 return ((int)(w*((c.xpos*24/20)-(2.0/20))));
 }
 public int cellToY(TamskCell c)
 {		//return super.cellToY(c);//
	 return ((int)(c.ypos*G.Height(boardRect)));
 }

 
}
