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
package pendulum;


import static pendulum.PendulumMovespec.*;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;

/**
 * PendulumBoard knows all about the game of Pendulum, which is played
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
class Timer
{
	long timeToRun = 0;
	long timeStart = 0;
	PendulumChip icon = null;
	boolean running = false;
	
	public Timer(PendulumChip chip,long time)
	{
		timeStart = time;
		icon = chip;
	}
	void flip()
	{
		timeToRun = timeStart;
		running = true;
	}
	public String getText() {
		if(timeToRun<=0) { return "--"; }
		return G.briefTimeString(timeToRun);
	}
	public void reInit() {
		timeToRun = 0;
		running = false;
	}
	public void useTime(long n)
	{	if(running)
		{
		timeToRun -=n;
		}
	}
}
class PendulumBoard 
	extends RBoard<PendulumCell>	// for a square grid board, this could be rectBoard or squareBoard 
	implements BoardProtocol,PendulumConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
	PendulumVariation variation = PendulumVariation.pendulum;
	private PendulumState board_state = PendulumState.Puzzle;	
	private PendulumState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	public PendulumState getState() { return(board_state); }
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(PendulumState st) 
	{ 	unresign = (st==PendulumState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	private PlayerBoard initial_pbs[] = 
		{
				new PlayerBoard(this,PColor.Yellow,0),	// these are indexes to the setup, not to the player order
				new PlayerBoard(this,PColor.White,1),
				new PlayerBoard(this,PColor.Green,2),
				new PlayerBoard(this,PColor.Blue,3),
				new PlayerBoard(this,PColor.Red,4)
		};
	PlayerBoard pbs[] = null;
	
	public Timer blackTimer = new Timer(PendulumChip.blackHourglass,45*1000);
	public Timer purpleTimer = new Timer(PendulumChip.purpleHourglass,3*60*1000);
	public Timer greenTimer = new Timer(PendulumChip.greenHourglass,2*60*1000);
	public boolean timersRunning = false;
	public long lastTimerTime = 0;
	public int councilPhase = 0;
	public PendulumCell councilCards[] = newcell(PendulumId.RewardCard,8);
	public PendulumCell councilRewardsDeck = councilCards[0];
	public PendulumCell timerTrack[] = newcell(PendulumId.TimerTrack,11);
	
	public PendulumCell provinceCards = newcell(PendulumId.ProvinceCardStack);
	public PendulumCell achievementCards = newcell(PendulumId.AchievementCardStack);
	public PendulumCell currentAchievement = newcell(PendulumId.AchievementCard);
	public PendulumCell greenHourglass[] = newcell(PendulumId.GreenHourglass,2);
	public PendulumCell purpleHourglass[] = newcell(PendulumId.PurpleHourglass,4);
	public PendulumCell blackHourglass[] = newcell(PendulumId.BlackHourglass,2);
	
	public boolean purpleHourglassTop()
	{
		return (purpleHourglass[0].topChip()==PendulumChip.purpleHourglass)
				|| (purpleHourglass[1].topChip()==PendulumChip.purpleHourglass);
	}
	public boolean blackHourglassTop()
	{
		return blackHourglass[0].topChip()==PendulumChip.blackHourglass;
	}
	public boolean greenHourglassTop()
	{
		return greenHourglass[0].topChip()==PendulumChip.greenHourglass;
	}
	public PendulumCell privilege[] = newcell(PendulumId.Privilege,MAX_PLAYERS);
	public PendulumCell achievement[] = newcell(PendulumId.Achievement,MAX_PLAYERS);
	public PendulumCell provinces[] = newcell(PendulumId.Province,4);
	
	public PendulumCell greenMeepleA[] = newcell(PendulumId.GreenMeepleA,3);
	public PendulumCell greenMeepleB[] = newcell(PendulumId.GreenMeepleB,3);
	public PendulumCell greenActionA[] = newcell(PendulumId.GreenActionA,
				new BC[] {BC.D2,BC.D2,BC.D2},
				new BB[] {BB.RedPB,BB.Culture2,BB.Military1Vote2});
	public PendulumCell greenActionB[] = newcell(PendulumId.GreenActionB,
				new BC[] {BC.D2,BC.D2,BC.D2},
				new BB[] {BB.RedPB,BB.Culture2,BB.Military1Vote2});
	
	public PendulumCell blackMeepleA[] = newcell(PendulumId.BlackMeepleA,4);
	public PendulumCell blackMeepleB[] = newcell(PendulumId.BlackMeepleB,4);
	public PendulumCell blackActionA[] = newcell(PendulumId.BlackActionA,
			new BC[] {BC.None,BC.None,BC.M4,BC.None},
			new BB[] {BB.YellowPB,BB.Vote1,BB.Province,BB.Resource1});
	public PendulumCell blackActionB[] = newcell(PendulumId.BlackActionB,
			new BC[] {BC.None,BC.None,BC.M4,BC.None},
			new BB[] {BB.YellowPB,BB.Vote1,BB.Province,BB.Resource1});

	public PendulumCell purpleMeepleA[] = newcell(PendulumId.PurpleMeepleA,3);
	public PendulumCell purpleMeepleB[] = newcell(PendulumId.PurpleMeepleB,3);
	public PendulumCell purpleActionA[] = newcell(PendulumId.PurpleActionA,
			new BC[] {BC.D2,BC.D2,BC.D2},
			new BB[] {BB.BrownPB,BB.Popularity1Prestige1Vote1,BB.BluePB});
	public PendulumCell purpleActionB[] = newcell(PendulumId.PurpleActionB,
			new BC[] {BC.D2,BC.D2,BC.D2},
			new BB[] {BB.BrownPB,BB.Popularity1Prestige1Vote1,BB.BluePB});

    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public PendulumChip getPlayerChip(int p) { return(pbs[p].grande); }
	public PColor getPlayerColor(int p) { return(pbs[p].color); }
	public PendulumChip getCurrentPlayerChip() { return(pbs[whoseTurn].meeple); }
	
	// get the player index corresponding to a particular chip
	public int getPlayerIndex(PendulumChip from)
	{	PColor color = from.color;
		for(PlayerBoard pb : pbs) { if(pb.meeple.color==color) { return pb.boardIndex; }}
		throw G.Error("Can't find a player corresponding to %s",from);
	}
	
	// get the player's position on the privilege track
	public int getPlayerPrivilege(PendulumChip from)
	{	PColor color = from.color;
		for(int i=0;i<players_in_game; i++)
		{
			if(privilege[i].topChip().color==color) { return i; }
		}
		throw G.Error("Can't find %s on the privilege track",from);
	}
	public int getPlayerPrivilege(int who) { return getPlayerPrivilege(pbs[who].hexagon); }
	
	public PlayerBoard getPlayerBoard(PendulumChip ch)
	{
		PColor color = ch.color;
		for(PlayerBoard pb : pbs) { if(pb.meeple.color==color) { return pb; }}
		throw G.Error("no active player with color %s",color);
	}
	public PlayerBoard getPlayerBoard(int n) { return pbs[n]; }
	public PlayerBoard getPlayerBoard(char n) 
	{	for(PlayerBoard pb : pbs)
		{
			if(pb.gameIndex==n-'A')
			{
				return pb;
			}
		}
		throw G.Error("No active player for %s",n);
	}
	
	public PendulumPlay robot = null;
	
	 public boolean p1(String msg)
		{
			if(G.p1(msg) && robot!=null)
			{	String dir = "g:/share/projects/boardspace-html/htdocs/pendulum/pendulumgames/robot/";
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
	public void SetDrawState() { setState(PendulumState.Draw); }	CellStack animationStack = new CellStack();

	// intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    private int round = 0;
    // save strings to be shown in the game log
    StringStack gameEvents = new StringStack();
    InternationalStrings s = G.getTranslations();
    
 	void logGameEvent(String str,String... args)
 	{	//if(!robotBoard)
 		{String trans = s.get(str,args);
 		 gameEvents.push(trans);
 		}
 	}

    private PendulumState resetState = PendulumState.Puzzle; 
    public DrawableImage<?> lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public PendulumCell[] newcell(PendulumId id,int n)
	{	PendulumCell row[] = new PendulumCell[n];
		for(int i=0;i<n;i++) 
			{ PendulumCell c = row[i]=new PendulumCell(id,'@',i); 
			  c.onBoard = true;
			  c.next = allCells;
			  allCells = c;
			}
		return row;
	}
	// factory method to generate a board cell
	public PendulumCell[] newcell(PendulumId id,BC[]costs,BB[]benefits)
	{	PendulumCell row[] = new PendulumCell[costs.length];
		for(int i=0;i<costs.length;i++) 
			{ PendulumCell c = row[i]=new PendulumCell(id,'@',i); 
			  c.onBoard = true;
			  c.next = allCells;
			  c.cost = costs[i];
			  c.benefit = benefits[i];
			  allCells = c;
			}
		return row;
	}
	
	// factory method to generate a board cell
	public PendulumCell newcell(PendulumId id)
	{	PendulumCell c = new PendulumCell(id); 
		c.next = allCells;
		c.onBoard = true;
		allCells = c;
		return c;
	}

	// constructor 
    public PendulumBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        setColorMap(map, players);
        
        councilRewardsDeck.rackLocation = PendulumId.RewardDeck;

        doInit(init,key,players,rev); // do the initialization 
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
 		setState(PendulumState.Puzzle);
		variation = PendulumVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		councilPhase = 0;
		robotState.clear();
		gametype = gtype;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case pendulum:
			// using reInitBoard avoids thrashing the creation of cells 
			// when reviewing games.
			// or initBoard(variation.firstInCol,variation.ZinCol,null);
			// Random r = new Random(734687);	// this random is used to assign hash values to cells, common to all games of this type.
			// allCells.setDigestChain(r);		// set the randomv for all cells on the board
		}

		for(PendulumCell c = allCells; c!=null; c=c.next) { c.reInit(); }
 		
	    whoseTurn = FIRST_PLAYER_INDEX;
	    round = 0;
	    resetState = null;
	    lastDroppedObject = null;
	    int map[]=getColorMap();
	    pbs = new PlayerBoard[players_in_game];
	    for(int i=0;i<players_in_game;i++)
	    {
	    	pbs[i] = initial_pbs[map[i]];
	    	pbs[i].doInit(variation.advanced,i);
	    }
	    
	    // prepare the council deck
	    Random r = new Random(randomKey);
	    for(int lim=PendulumChip.rewardcards.length-2; lim>=0; lim--)
	    {
	    	councilRewardsDeck.addChip(PendulumChip.rewardcards[lim]);
	    }
	    councilRewardsDeck.shuffle(r);
	    for(int i=0;i<10;i++) { councilRewardsDeck.removeTop(); }	// discard 10
	    
	    for(int lim= PendulumChip.finalrewardcards.length-2; lim>=0; lim--) 
	    	{ // put the final cards on the bottom of the deck
	    	councilRewardsDeck.insertChipAtIndex(0,PendulumChip.finalrewardcards[lim]);
	    	}
	    // prepare the province deck
	    for(int lim=PendulumChip.provinceCards.length-2; lim>=0; lim--)
	    {
	    	provinceCards.addChip(PendulumChip.provinceCards[lim]);
	    }
	    provinceCards.shuffle(r);
	    for(PendulumCell c : provinces) {c.addChip(provinceCards.removeTop()); }
	    
	    // prepare the achievement deck
	    for(int lim=PendulumChip.achievementcards.length-2; lim>=0; lim--)
	    {
	    	achievementCards.addChip(PendulumChip.achievementcards[lim]);
	    }
	    achievementCards.shuffle(r);
	    currentAchievement.addChip(achievementCards.removeTop());
	    if(players_in_game >=4) { currentAchievement.addChip(PendulumChip.legendary); }
	    
        animationStack.clear();
        moveNumber = 1;
        timerTrack[0].addChip(PendulumChip.grayGlass);
        greenHourglass[0].addChip(PendulumChip.greenHourglass);
        blackHourglass[0].addChip(PendulumChip.blackHourglass);
        purpleHourglass[0].addChip(PendulumChip.purpleHourglass);
        timersRunning = false;
        lastTimerTime = 0;
        
        // mark the future purple timer slots
        for(int i=1;i<=3;i++) { purpleHourglass[i].addChip(PendulumChip.purpleGlass); }
        animationStack.shuffle(r);
        // randomize the initial privilege
        int par[] = AR.intArray(players_in_game);
        r.shuffle(par);
        for(int i=0;i<players_in_game; i++) 
        	{ privilege[i].addChip(pbs[par[i]].hexagon); 
        	  achievement[i].addChip(pbs[par[i]].cylinder);
        	}
	    dealCouncilCards();
	    
        
        // note that firstPlayer is NOT initialized here
    }
    private void dealCouncilCards()
    {	
    	for(int i=1;i<=5;i++)
    	{	int idx = i>3 ? i+2 : i;
    		councilCards[idx].reInit();
    		councilCards[idx].addChip(councilRewardsDeck.removeTop());
    	}
    	councilCards[4].addChip(PendulumChip.defcard);
    	councilCards[5].addChip(PendulumChip.flipcard);
    	
    }
    
    /** create a copy of this board */
    public PendulumBoard cloneBoard() 
	{ PendulumBoard dup = new PendulumBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((PendulumBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(PendulumBoard from_b)
    {
        super.copyFrom(from_b);
        
        for(int i=0;i<players_in_game;i++) { pbs[i].copyFrom(from_b.pbs[i]); }
        
        robotState.copyFrom(from_b.robotState);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        provinceCards.copyFrom(from_b.provinceCards);
        round = from_b.round;
        blackTimer = from_b.blackTimer;
        greenTimer = from_b.greenTimer;
        purpleTimer = from_b.purpleTimer;
        timersRunning = from_b.timersRunning;
        lastTimerTime = from_b.lastTimerTime;
        
        achievementCards.copyFrom(from_b.achievementCards);
        currentAchievement.copyFrom(from_b.currentAchievement);
        councilRewardsDeck.copyFrom(from_b.councilRewardsDeck);
        copyFrom(councilCards,from_b.councilCards);
        copyFrom(timerTrack,from_b.timerTrack);
        copyFrom(provinces,from_b.provinces);
        copyFrom(greenHourglass,from_b.greenHourglass);
        copyFrom(blackHourglass,from_b.blackHourglass);
        copyFrom(purpleHourglass,from_b.purpleHourglass);
        copyFrom(privilege,from_b.privilege);
        copyFrom(achievement,from_b.achievement);
        
        copyFrom(blackActionA,from_b.blackActionA);
        copyFrom(blackActionB,from_b.blackActionB);
        copyFrom(blackMeepleA,from_b.blackMeepleA);
        copyFrom(blackMeepleB,from_b.blackMeepleB);
        
        copyFrom(greenActionA,from_b.greenActionA);
        copyFrom(greenActionB,from_b.greenActionB);
        copyFrom(greenMeepleA,from_b.greenMeepleA);
        copyFrom(greenMeepleB,from_b.greenMeepleB);
 
        copyFrom(purpleActionA,from_b.purpleActionA);
        copyFrom(purpleActionB,from_b.purpleActionB);
        copyFrom(purpleMeepleA,from_b.purpleMeepleA);
        copyFrom(purpleMeepleB,from_b.purpleMeepleB);
       
 
        resetState = from_b.resetState;
        councilPhase = from_b.councilPhase;
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((PendulumBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(PendulumBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        for(int i=0;i<players_in_game;i++) { pbs[i].sameboard(from_b.pbs[i]); }
        
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(provinceCards.sameContents(from_b.provinceCards),"province cards mismatch");
        G.Assert(round==from_b.round,"round mismatch");
        G.Assert(achievementCards.sameContents(from_b.achievementCards),"achievementCards mismatch");
        G.Assert(currentAchievement.sameContents(from_b.currentAchievement),"currentAchievement mismatch");
        G.Assert(sameContents(provinces,from_b.provinces),"province mismatch");
        
        G.Assert(sameContents(blackMeepleA,from_b.blackMeepleA),"blackMeepleA mismatch");
        G.Assert(sameContents(blackMeepleB,from_b.blackMeepleB),"blackMeepleB mismatch");
        G.Assert(sameContents(blackActionA,from_b.blackActionA),"blackActionA mismatch");
        G.Assert(sameContents(blackActionB,from_b.blackActionB),"blackActionB mismatch");
        G.Assert(sameContents(greenActionA,from_b.greenActionA),"greenActionA mismatch");
        G.Assert(sameContents(greenActionB,from_b.greenActionB),"greenActionB mismatch");
        G.Assert(sameContents(greenMeepleA,from_b.greenMeepleA),"greenMeepleA mismatch");
        G.Assert(sameContents(greenMeepleB,from_b.greenMeepleB),"greenMeepleB mismatch");
        G.Assert(sameContents(purpleMeepleA,from_b.purpleMeepleA),"purpleMeepleA mismatch");
        G.Assert(sameContents(purpleMeepleB,from_b.purpleMeepleB),"purpleMeepleB mismatch");
        G.Assert(sameContents(purpleActionA,from_b.purpleActionA),"purpleActionA mismatch");
        G.Assert(sameContents(purpleActionB,from_b.purpleActionB),"purpleActionB mismatch");

        G.Assert(councilRewardsDeck.sameContents(from_b.councilRewardsDeck),"council rewards mismatch");
        G.Assert(sameContents(councilCards,from_b.councilCards),"councilCards mismatch");
        G.Assert(sameContents(timerTrack,from_b.timerTrack),"timerTrack mismatch");
        G.Assert(sameContents(privilege,from_b.privilege),"privilege mismatch");
        G.Assert(sameContents(achievement,from_b.achievement),"achievement mismatch");
        G.Assert(councilPhase==from_b.councilPhase,"councilPhase mismatch");
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
        for(PlayerBoard p : pbs) { v ^= p.Digest(r); }
        
		// many games will want to digest pickedSource too
		// v ^= cell.Digest(r,pickedSource);
		v ^= Digest(r,revision);
		v ^= Digest(r,board_state);
		v ^= Digest(r,councilRewardsDeck);
		v ^= Digest(r,councilCards);
		v ^= Digest(r,timerTrack);
		v ^= Digest(r,provinceCards);
		v ^= Digest(r,round);
		v ^= Digest(r,achievementCards);
		v ^= Digest(r,currentAchievement);
		v ^= Digest(r,greenHourglass);
		v ^= Digest(r,purpleHourglass);
		v ^= Digest(r,privilege);
		v ^= Digest(r,achievement);
		v ^= Digest(r,blackHourglass);
		
		v ^= Digest(r,provinces);
		
	    v ^= Digest(r,blackActionA);
	    v ^= Digest(r,blackActionB);
	    v ^= Digest(r,blackMeepleA);
	    v ^= Digest(r,blackMeepleB);
	        
	    v ^= Digest(r,greenActionA);
	    v ^= Digest(r,greenActionB);
	    v ^= Digest(r,greenMeepleA);
	    v ^= Digest(r,greenMeepleB);
	 
	    v ^= Digest(r,purpleActionA);
	    v ^= Digest(r,purpleActionB);
	    v ^= Digest(r,purpleMeepleA);
	    v ^= Digest(r,purpleMeepleB);

	    v ^= Digest(r,councilPhase);
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
        case Resign:
            moveNumber++; //the move is complete in these states
            PlayerBoard pb = pbs[whoseTurn];
            int po = getPlayerPrivilege(pb.hexagon);
            int pn = (po+1)%players_in_game;
            PlayerBoard next = getPlayerBoard(privilege[pn].topChip());
            setWhoseTurn(next.boardIndex);
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



	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { 	if(!simultaneousTurnsAllowed())
    	{
    	PlayerBoard pb = pbs[whoseTurn];
    	PendulumChip ch = pb.pickedObject;
    	if(ch!=null) { return ch.chipNumber();}
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
    private PendulumCell getCell(PendulumId source,char col, int row)
    {	
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source " + source);
        case AchievementCard:
        	return currentAchievement;
        case AchievementCardStack:
        	return achievementCards;
        case ProvinceCardStack:
        	return provinceCards;
        	
        case PlayerStratCard:
        case PlayerPlayedStratCard:
        case PlayerBlueBenefits:
        case PlayerBrownBenefits:
        case PlayerRedBenefits:
        case PlayerYellowBenefits:
        case PlayerMilitary:
        case PlayerCulture:
        case PlayerCash:
        case PlayerVotes:
        case PlayerMilitaryVP:
        case PlayerPrestigeVP:
        case PlayerPopularityVP:
        case PlayerGrandes:       	
        case PlayerMeeples:
        case PlayerGrandeReserves:
        case PlayerMeepleReserves:
        case PlayerVotesReserves:
        case PlayerMilitaryReserves:
        case PlayerCultureReserves:
        case PlayerCashReserves:
        	return getPlayerBoard(col).getCell(source,row);
        	
        case BlackMeepleA:
        	return blackMeepleA[row];
        case BlackMeepleB:
        	return blackMeepleB[row];
        case BlackActionA:
        	return blackActionA[row];
        case BlackActionB:
        	return blackActionB[row];
        	
        case GreenMeepleA:
        	return greenMeepleA[row];
        case GreenMeepleB:
        	return greenMeepleB[row];
        case GreenActionA:
        	return greenActionA[row];
        case GreenActionB:
        	return greenActionB[row];
           	
        case PurpleMeepleA:
        	return purpleMeepleA[row];
        case PurpleMeepleB:
        	return purpleMeepleB[row];
        case PurpleActionA:
        	return purpleActionA[row];
        case PurpleActionB:
        	return purpleActionB[row];
            	
        case TimerTrack:
        	return timerTrack[row];
        case RewardCard:
        	return councilCards[row];
        case RewardDeck:
        	return councilRewardsDeck;
        case GreenHourglass:
        	return greenHourglass[row];
        case BlackHourglass:
        	return blackHourglass[row];
        case PurpleHourglass:
        	return purpleHourglass[row];
        case Privilege:
        	return privilege[row];
        case Achievement:
        	return achievement[row];
        case Province:
        	return(provinces[row]);
         } 	
    }
    /**
     * this is called when copying boards, to get the cell on the new (ie current) board
     * that corresponds to a cell on the old board.
     */
    public PendulumCell getCell(PendulumCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }

    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(PlayerBoard pb,replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state " + board_state);
        case Confirm:
        	setNextStateAfterDone(replay);
         	break;
        case Play:
        	pb.setNextStateAfterDrop(replay);
        	break;
        case PlayMeeple:
        case PlayGrande:
        	resetState = board_state;
			setState(PendulumState.Confirm);
			break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    public void acceptPlacement()
    {
    	for(PlayerBoard pb : pbs) { pb.acceptPlacement(); }
    }
    private void moveTimer(Timer timer,PendulumCell from0, PendulumCell to0, replayMode replay)
    {	PendulumCell from = from0;
    	PendulumCell to = to0;
    	if(from.topChip()==null) { from = to0; to=from0; }
    	if(replay.animate) { animationStack.push(from); animationStack.push(to); }
    	timer.flip();
    	if(to.topChip()!=null) { to.removeTop(); }
    	to.addChip(from.removeTop());
    }
    private void flipAllTimers(replayMode replay)
    {
    	flipBlackTimer(replay);
    	flipGreenTimer(replay);
    	flipPurpleTimer(replay);
    	timersRunning=true;
    }
    private void flipGreenTimer(replayMode replay)
    {
    	moveTimer(greenTimer,greenHourglass[0],greenHourglass[1],replay);
    }
    private void flipBlackTimer(replayMode replay)
    {
    	moveTimer(blackTimer,blackHourglass[0],blackHourglass[1],replay);
    }
    private void flipPurpleTimer(replayMode replay)
    {	switch(councilPhase)
    	{
    	case 0:
    		moveTimer(purpleTimer,purpleHourglass[0],purpleHourglass[2],replay);
    		break;
    	case 1:
    		moveTimer(purpleTimer,purpleHourglass[2],purpleHourglass[1],replay);
    		break;
    	case 2:
    		moveTimer(purpleTimer,purpleHourglass[1],purpleHourglass[3],replay);
    		break;
    	case 3:
    		moveTimer(purpleTimer,purpleHourglass[3],purpleHourglass[0],replay);
    		break;
    	default: G.Error("no phase 5!");
    	}
    	councilPhase++;
    	
    }
    public void doTimers()
    {	long now = G.Date();
    	if(timersRunning) 
    	{
    		long dif = now-lastTimerTime;
    		greenTimer.useTime(dif);
    		blackTimer.useTime(dif);
    		purpleTimer.useTime(dif);
    	}
    	lastTimerTime = now;
    }
    private void setNextStateAfterDone(replayMode replay)
    {	
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state "+board_state);
    	case Gameover:
    		break;
    	case Confirm:
    		switch(resetState)
    		{
    		case PlayMeeple:
    			if(getPlayerPrivilege(whoseTurn)==0) 
    				{ setState(PendulumState.Play); 
    				  flipAllTimers(replay);
    				}
    			else { setState(resetState); }
    			break;
    		case PlayGrande:
    			if(getPlayerPrivilege(whoseTurn)==0) { setState(PendulumState.PlayMeeple); }
    			else { setState(resetState); }
    			break;
    		default:
    			G.Error("Not expecting confirm from %s",resetState);
    		}
    		break;
    	case Puzzle:
    	case Play:
    		setState(PendulumState.Play);
    		
    		break;
    	}
       	resetState = board_state;
    }
    private void doDone(replayMode replay)
    {
        acceptPlacement();

        if (board_state==PendulumState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(PendulumState.Gameover);
        }
        else
        {	if(winForPlayerNow(whoseTurn)) 
        		{ win[whoseTurn]=true;
        		  setState(PendulumState.Gameover); 
        		}
        	else {setNextPlayer(replay);
        		setNextStateAfterDone(replay);
        	}
        }
    }
	
    public boolean Execute(commonMove mm,replayMode replay)
    {	PendulumMovespec m = (PendulumMovespec)mm;
        if(replay.animate) { animationStack.clear(); }

        //G.print("E "+m+" for "+whoseTurn+" "+state);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone(replay);

            break;


        case MOVE_PICK:
        	{
        	PendulumCell c = getCell(m.source,m.from_col,m.from_row);
        	PlayerBoard pb = getPlayerBoard(m.player);
        	if(pb.isDest(c)) { pb.unDropObject(); }
        	else { pb.pickObject(c,m.from_index); }
        	}
            break;

        case MOVE_DROP: // drop on chip pool;
        	{
        	PendulumCell c = getCell(m.source,m.from_col,m.from_row);
        	PlayerBoard pb = getPlayerBoard(m.player);
        	if(pb.isSource(c)) { pb.unPickObject(); }
        	else 
        	{ pb.doDrop(c,board_state,replay);
    		  setNextStateAfterDrop(pb,replay);
        	}}
            break;
        case SETACTIVE:
        	G.Assert(simultaneousTurnsAllowed(),"should be anymove");
        	setWhoseTurn(m.player);
        	break;
        	
        case MOVE_START:
        	if(round==0)
        		{ setWhoseTurn(getPlayerIndex(privilege[0].topChip())); 
        		  setState(PendulumState.PlayGrande);
        		}
        	else
        	{
            setWhoseTurn(m.player);
            acceptPlacement();
            int nextp = nextPlayer[whoseTurn];
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(PendulumState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ setState(PendulumState.Gameover); 
               	}
            else {  setNextStateAfterDone(replay); }
        	}
            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?PendulumState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(PendulumState.Puzzle);
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(PendulumState.Gameover);
    	   break;

        default:
        	cantExecute(m);
        }
        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }


    public boolean legalToHitBoard(PendulumCell c,Hashtable<PendulumCell,PendulumMovespec> targets )
    {	if(c==null) { return(false); }
    	G.Assert(c==getCell(c),"not from the same board %s",c);
        switch (board_state)
        {
        case Puzzle:
		case Play:
		case PlayGrande:
		case PlayMeeple:
			return(targets.get(c)!=null);
		case Gameover:
		case Resign:
			return(false);
		case Confirm:
			return(getPlayerBoard(whoseTurn).isDest(getCell(c)));
        default:
        	throw G.Error("Not expecting Hit Board state " + board_state);
        }
    }
    
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(PendulumMovespec m)
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
    public void UnExecute(PendulumMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	PendulumState state = robotState.pop();
        switch (m.op)
        {
        default:
   	    	throw G.Error("Can't un execute " + m);
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
 
 private void addActionMoves(CommonMoveStack all,int who)
 {	boolean greenTop = greenHourglassTop();
 	boolean blackTop = blackHourglassTop();
 	boolean purpleTop = purpleHourglassTop();
 	PendulumCell[] greenRow = greenTop ? greenMeepleA : greenMeepleB;
 	PendulumCell[] blackRow = blackTop ? blackMeepleA : blackMeepleB;
 	PendulumCell[] purpleRow = purpleTop ? purpleMeepleA : purpleMeepleB;
 	PendulumCell[] greenARow = greenTop ? greenActionA : greenActionB;
 	PendulumCell[] blackARow = blackTop ? blackActionA : blackActionB;
 	PendulumCell[] purpleARow = purpleTop ? purpleActionA : purpleActionB;
 	addActionMoves(all,greenRow,greenARow,who);
 	addActionMoves(all,blackRow,blackARow,who);
 	addActionMoves(all,purpleRow,purpleARow,who);
 }
 public boolean hasRetrieveWorkerMoves(int who)
 {

	return addRetrieveWorkerMoves(null,who); 
 }
 private boolean addRetrieveWorkerMoves(CommonMoveStack all,int who)
 {

	 PendulumCell[] greenRow = greenHourglassTop() ? greenMeepleA : greenMeepleB;
	 PendulumCell[] blackRow = blackHourglassTop() ? blackMeepleA : blackMeepleB;
	 PendulumCell[] purpleRow = purpleHourglassTop() ? purpleMeepleA : purpleMeepleB;

	 boolean some = addWorkerMoves(all,greenRow,who);
	 some |= addWorkerMoves(all,blackRow,who);
	 some |= addWorkerMoves(all,purpleRow,who);
	 return some;
	 
 }
 
 
 private void addActionMoves(CommonMoveStack all,PendulumCell from[],PendulumCell to[],int who)
 {
	 for(int i=0;i<from.length;i++)
	 {	
		 addActionMoves(all,from[i],to[i],who);
	 }
 }
 private boolean addActionMoves(CommonMoveStack all,PendulumCell from,PendulumCell to,int who)
 {	 PColor color = getPlayerColor(who);
 	 boolean some = false;
 	 PlayerBoard pb = pbs[who];
 	 if((from==pb.pickedSource) && pb.canPayCost(to.cost))
 	 {
 		some = true;
 		if(all!=null) { all.push(new PendulumMovespec(MOVE_FROM_TO,from,pb.pickedIndex,to,who)); }
 	 }
 	 else
 	 {
 	 for(int lim=from.height()-1; lim>=0; lim--)
 	 {
 	 PendulumChip chip = from.chipAtIndex(lim);
 	 if((chip.color==color) && pb.canPayCost(to.cost))
 	 	{some = true;
 		 if(all!=null) { all.push(new PendulumMovespec(MOVE_FROM_TO,from,lim,to,who)); }
 		 else { break; }
 	 	}
 	 }}
 	 return some;
 }
 private void addWorkerMoves(CommonMoveStack all,int who)
 {	
	 if(greenHourglassTop())
	 {
		 addWorkerMoves(all,greenMeepleB,who);
		 addWorkerMoves(all,greenActionB,who);
	 }
	 else
	 {
		 addWorkerMoves(all,greenMeepleA,who);
		 addWorkerMoves(all,greenActionA,who);
	 }
	 
	 if(blackHourglassTop())
	 {
		 addWorkerMoves(all,blackMeepleB,who);
		 addWorkerMoves(all,blackActionB,who);	 
	 }
	 else
	 {
		 addWorkerMoves(all,blackMeepleA,who);
		 addWorkerMoves(all,blackActionA,who);
	 }
	 
	 if(purpleHourglassTop())
	 {
		 addWorkerMoves(all,purpleMeepleB,who);
		 addWorkerMoves(all,purpleActionB,who); 
	 }
	 else
	 {
		 addWorkerMoves(all,purpleMeepleA,who);
		 addWorkerMoves(all,purpleActionA,who);
	 }

 }
 private boolean addWorkerMoves(CommonMoveStack all,PendulumCell from[],int who)
 {	boolean some = false;
	 for(PendulumCell c : from)
	 {
		 some |= addWorkerMoves(all,c,who);
		 if(some && all==null) { return some; }
	 }
	 return some;
 }
 private boolean addWorkerMoves(CommonMoveStack all,PendulumCell from,int who)
 {	 PlayerBoard pb = pbs[who];
 	 PColor color = pb.color;
 	 boolean some = false;
 	 PendulumChip picked = pb.pickedObject;
 	 if(picked!=null)
 	 {  if(pb.pickedSource==from)
 	 	{
 		boolean grande = picked==pb.grande;
 		addWorkerMoves(all,from,pb.pickedIndex,grande,who);
 		if(grande)
 			{
 			some |= addWorkerMoves(all,from,pb.pickedIndex,pb.grandes,true,who);
 			}
 			else
 			{
 			some |= addWorkerMoves(all,from,pb.pickedIndex,pb.meeples,true,who);
 			}
 	 	}
 	 }
 	 else
 	 {for(int lim=from.height()-1; lim>=0 && !(all==null && some); lim--)
	 {	 PendulumChip worker = from.chipAtIndex(lim);
		 if(from.chipAtIndex(lim).color==color)
		 { 	
			 some |= addWorkerMoves(all,from,lim,worker==pb.grande,who);
		 }
	 }}
 	 return some;
 }
 private void addGrandeMoves(CommonMoveStack all,int who)
 {
	 PlayerBoard pb = pbs[who];
	 int from = (pb.pickedSource==pb.grandes && pb.pickedObject!=null)
			 		? pb.pickedIndex
			 		: pb.grandes.findChip(pb.grande);
	 if(from>=0) { addWorkerMoves(all,pb.grandes,from,true,who); }
 }
 
 private void addMeepleMoves(CommonMoveStack all,int who)
 {
	 PlayerBoard pb = pbs[who];
	 int from = (pb.pickedSource==pb.meeples && pb.pickedObject!=null)
			 		? pb.pickedIndex 
			 		: pb.meeples.findChip(pb.meeple);
	 if(from>=0) {  addWorkerMoves(all,pb.meeples,from,false,who); }
 }
 private boolean addWorkerMoves(CommonMoveStack all,PendulumCell from,int indx,boolean any,int who)
 {
	 PendulumCell[] greenRow = greenHourglassTop() ? greenMeepleB : greenMeepleA;
	 PendulumCell[] blackRow = blackHourglassTop() ? blackMeepleB : blackMeepleA;
	 PendulumCell[] purpleRow = purpleHourglassTop() ? purpleMeepleB : purpleMeepleA;
	 boolean some = addWorkerMoves(all,from,indx,greenRow,any,who);
	 if(all==null && some) { return some; }
	 some |= addWorkerMoves(all,from,indx,blackRow,true,who);
	 if(all==null && some) { return some; }
	 some |= addWorkerMoves(all,from,indx,purpleRow,any,who);
	 return some;
	 
 }
 private boolean addWorkerMoves(CommonMoveStack all,PendulumCell from,int indx,PendulumCell to[],boolean any,int who)
 {	boolean some = false;
	for(PendulumCell dest : to) 
	{
		some |= addWorkerMoves(all,from,indx,dest,any,who);
		if(all==null && some) { return some; }
	}
	return some;
 }
 private boolean addWorkerMoves(CommonMoveStack all,PendulumCell from,int indx,PendulumCell to,boolean any,int who)
 {
	if(any || to.topChip()==null)
	{
		if(all!=null) { all.push(new PendulumMovespec(MOVE_FROM_TO,from,indx,to,who)); }
		return true;
	}
	return false;
 }
 private void addPuzzleMoves(CommonMoveStack all,PendulumCell cells,int who)
 {
	 PlayerBoard pb = pbs[who];
	 if(pb.pickedObject==null)
	 {
		 for(PendulumCell c = cells; c!=null; c=c.next ) 
		 {
			 if(c.height()>0) { all.push(new PendulumMovespec(MOVE_PICK,c,-1,who));}
		 }
	 }
	 else {
		 for(PendulumCell c = cells; c!=null; c=c.next) 
		 {
			 all.push(new PendulumMovespec(MOVE_DROP,c,-1,who));
		 }
	 }
 }
 CommonMoveStack  GetListOfMoves(int who)
 {	CommonMoveStack all = new CommonMoveStack();
 	getListOfMoves(all,who);
 	return all;
 }
 private void getListOfMoves(CommonMoveStack all,int who)
 {
 	switch(board_state)
 	{
 	case Puzzle:
 		{
 		addPuzzleMoves(all,allCells,who);
 		for(PlayerBoard pb : pbs) { addPuzzleMoves(all,pb.allCells,who); }
 		}
 		break;
 	case Confirm:
 		all.push(new PendulumMovespec(MOVE_DONE,who));
 		break;
 	case PlayGrande:
 		addGrandeMoves(all,who);
 		break;
 	case PlayMeeple:
 		addMeepleMoves(all,who);
 		break;
 	case Play:
 		{
 		PlayerBoard pb = pbs[who];
 		switch(pb.uiState)
 		{
 		case Normal:
	 		addWorkerMoves(all,who);	// move a worker instead of taking the action
	 		addGrandeMoves(all,who);	// place a grande from the reserve
	 		addMeepleMoves(all,who);	// place a regular worker from the reserve
	 		addActionMoves(all,who);	// take the action a worker is elgible for
	 		pb.addPlayStrategem(all,who);	// play a strategem card
	 		break;
 		case RetrieveWorker:
 			addRetrieveWorkerMoves(all,who);
 			break;
 		case CollectResources:
 			pbs[who].addCollectResourceMoves(all);
 			break;
 		case Province:
 			pbs[who].addCollectProvinceMoves(all);
 			break;
 		default:
 			throw G.Error("Not expecting uiState %s",pb.uiState);
 		}}
 		break;
 	default:
 			G.Error("Not expecting state ",board_state);
 	}

 }
 
 public void initRobotValues(PendulumPlay m)
 {	robot = m;
 }

 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(Character.isDigit(txt.charAt(0)))
	 	{ switch(variation)
	 		{
	 		case pendulum:
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
 public Hashtable<PendulumCell, PendulumMovespec> getTargets(int player) 
 {
 	Hashtable<PendulumCell,PendulumMovespec> targets = new Hashtable<PendulumCell,PendulumMovespec>();
 	CommonMoveStack all = GetListOfMoves(player);
	// these become unpick and undrop actions
 	getUIMoves(all,player);
 	getTargets(targets,all);
 	return targets;
 }
 public void getUIMoves(CommonMoveStack all,int player)
 {	 	
 	PlayerBoard pb = pbs[player];
 	if((pb.pickedObject!=null) && (pb.pickedSource!=null))
 		{
 		all.push(new  PendulumMovespec(MOVE_DROP,pb.pickedSource,-1,player));
 		}
 	if((pb.droppedObject!=null) && (pb.droppedDest!=null))
		{
		all.push(new PendulumMovespec(MOVE_PICK,pb.droppedDest,-1,player));
		}
 }
 public Hashtable<PendulumCell, PendulumMovespec> getAllTargets() 
 {	CommonMoveStack all = new CommonMoveStack();
 	Hashtable<PendulumCell,PendulumMovespec> targets = new Hashtable<PendulumCell,PendulumMovespec>();
 	for(int i=0;i<players_in_game;i++)
 		{	getListOfMoves(all,i);
 			getUIMoves(all,i);
 		}
	 getTargets(targets,all);
	 return targets;
 }
 private void getTargets(Hashtable<PendulumCell,PendulumMovespec> targets,CommonMoveStack all)
 {
 	
 	for(int lim=all.size()-1; lim>=0; lim--)
 	{	PendulumMovespec m = (PendulumMovespec)all.elementAt(lim);
 		switch(m.op)
 		{
 		case MOVE_DONE:
 			break;
 		case MOVE_PICK:
 			targets.put(getCell(m.source,m.from_col,m.from_row),m);
 			break;
 		case MOVE_DROP:
 			targets.put(getCell(m.dest,m.to_col,m.to_row),m);
 			break;
 		case MOVE_FROM_TO:
 			PlayerBoard pb = pbs[m.player];
 			if(pb.pickedObject==null)
 				{	targets.put(getCell(m.source,m.from_col,m.from_row),m);
 				}
 				else
 				{
 					targets.put(getCell(m.dest,m.to_col,m.to_row),m);
 				}
 			break;
 		case SETACTIVE: break;
 		default: G.Error("Not expecting "+m);
 		
 		}
 	}
 }
 //public boolean drawIsPossible() { return false; }
 // public boolean canOfferDraw() {
 //	 return false;
	 /**
	something like this:
 	return (movingObjectIndex()<0)
 			&& ((board_state==PendulumState.Play) || (board_state==PendulumState.DrawPending))
 			&& (moveNumber-lastDrawMove>4);
 			*/
 //}

public void setLocations()
{	
	provinces[0].setLocation(0.12,0.52,0.12);
	provinces[1].setLocation(0.12,0.64,0.12);
	provinces[2].setLocation(0.12,0.76,0.12);
	provinces[3].setLocation(0.12,0.88,0.12);
	provinceCards.setLocation(0.27,0.64,0.12);
	achievementCards.setLocation(0.541,0.172,0.117);
	currentAchievement.setLocation(0.541,0.354,0.117);
	// hourglasses
	greenHourglass[0].setLocation(0.95,0.15,0.09);
	greenHourglass[1].setLocation(0.95,0.38,0.09);
	blackHourglass[0].setLocation(0.95,0.6,0.09);
	blackHourglass[1].setLocation(0.95,0.84,0.09);
	purpleHourglass[0].setLocation(0.045,0.125,0.09);
	purpleHourglass[1].setLocation(0.045,0.18,0.09);
	purpleHourglass[2].setLocation(0.045,0.36,0.09);
	purpleHourglass[3].setLocation(0.045,0.41,0.09);

	PendulumCell.setHLocation(purpleMeepleA,0.125,0.42,0.09,0.1);
	PendulumCell.setHLocation(purpleActionA,0.125,0.42,0.19,0.1);
	PendulumCell.setHLocation(purpleMeepleB,0.125,0.42,0.325,0.1);
	PendulumCell.setHLocation(purpleActionB,0.125,0.42,0.425,0.1);

	PendulumCell.setHLocation(greenMeepleA,0.68,0.97,0.09,0.1);
	PendulumCell.setHLocation(greenActionA,0.68,0.97,0.19,0.1);
	PendulumCell.setHLocation(greenMeepleB,0.68,0.97,0.325,0.1);
	PendulumCell.setHLocation(greenActionB,0.68,0.97,0.425,0.1);

	PendulumCell.setHLocation(blackMeepleA,0.55,0.94,0.55,0.1);
	PendulumCell.setHLocation(blackActionA,0.55,0.94,0.65,0.1);
	PendulumCell.setHLocation(blackMeepleB,0.55,0.94,0.775,0.1);
	PendulumCell.setHLocation(blackActionB,0.55,0.94,0.875,0.1);

	PendulumCell.setVLocation(privilege,0.41,0.11,0.35,0.1);
	PendulumCell.setVLocation(achievement,0.47,0.31,0.44,0.08);
	PendulumCell.setVLocation(timerTrack,0.28,0.21,0.84,0.45);
	
	PendulumCell.setCouncilLocation(councilCards,0.22,0.795,0.29,0.7,0.2);

	for(PlayerBoard pb : pbs) { pb.setLocations(); }

}
private Rectangle councilRect = new Rectangle();
private Rectangle timerRect = new Rectangle();
public void setCouncilRectangle(Rectangle r) 
{ 	G.copy(councilRect,r); 
}
public void setTimerRectangle(Rectangle r)
{
	G.copy(timerRect,r);
}
public int cellToX(PendulumCell c) {
	switch(c.rackLocation())
	{
	case RewardDeck:
	case RewardCard:
		return G.interpolate(c.posx,G.Left(councilRect),G.Right(councilRect));
	case TimerTrack:
		return G.interpolate(c.posx,G.Left(timerRect),G.Right(timerRect));
	default: 
		
		if(c.col=='@')
		{
		G.Assert(c.onBoard,"should be on board");
		return G.interpolate(c.posx,G.Left(boardRect),G.Right(boardRect));
		}
		return initial_pbs[c.col-'A'].cellToX(c);
	}
}

public int cellToY(PendulumCell c) {
	switch(c.rackLocation())
	{
	case RewardDeck:
	case RewardCard:
		return G.interpolate(c.posy,G.Top(councilRect),G.Bottom(councilRect));
	case TimerTrack:
		return G.interpolate(c.posy,G.Top(timerRect),G.Bottom(timerRect));
	default: 
		if(c.col=='@')
		{
		G.Assert(c.onBoard,"should be on board");
		return G.interpolate(c.posy,G.Top(boardRect),G.Bottom(boardRect));
		}
		return initial_pbs[c.col-'A'].cellToY(c);
	}
	
}
public int cellSize(PendulumCell c) 
{	
	switch(c.rackLocation())
	{
	case RewardDeck:
	case RewardCard:
		return (int)(c.scale*G.Width(councilRect));
	case TimerTrack:
		return (int)(c.scale*G.Width(timerRect));
	default:
		if(c.col=='@')
		{
		G.Assert(c.onBoard,"should be on board");
		return (int)(c.scale*G.Width(boardRect)); 
		}
		return initial_pbs[c.col-'A'].cellSize(c);
	}
}
public void animate(PendulumCell cash, PendulumCell cashReserves) {
	animationStack.push(cash);
	animationStack.push(cashReserves);
}
public boolean allQuiet() {
	for(PlayerBoard pb : pbs) { if(!pb.isUIQuiet()) { return false; }}
	return true;
}

 // most multi player games can't handle individual players resigning
 // this provides an escape hatch to allow it.
 //public boolean canResign() { return(super.canResign()); }
}
