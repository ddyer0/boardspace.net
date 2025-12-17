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
 * 
 * 
 * 
 */
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
class Timer implements PendulumConstants
{
	long timeToRun = 0;
	long timeStart = 0;
	PendulumChip icon = null;
	boolean running = false;
	TColor color = null;
	public int totalFlips = 0;
	public Timer(PendulumChip chip,TColor c,long time)
	{
		timeStart = time;
		color = c;
		icon = chip;
	}
	public Timer(Timer from)
	{
		this(from.icon,from.color,from.timeStart);
		timeToRun = from.timeToRun;
		running = from.running;
	}
	public String toString()
	{
		return "<timer "+color+">";
	}
	void flip()
	{
		timeToRun = timeStart;
		totalFlips++;
		running = true;
	}
	public String getText(PendulumState state) 
	{
		if(timeToRun<=0) { return state==PendulumState.CouncilPlay ? "xx" : "--"; }
		return G.briefTimeString(timeToRun);
	}
	public void reInit() {
		timeToRun = 0;
		totalFlips = 0;
		running = false;
	}
	public void useTime(long n)
	{	if(running)
		{
		//long ttr = timeToRun;
		timeToRun -=n;
		//if(ttr>0 && timeToRun<0) { Plog.log.addLog("timer negative"); }
		}
	}
}
class PendulumBoard 
	extends RBoard<PendulumCell>	// for a square grid board, this could be rectBoard or squareBoard 
	implements BoardProtocol,PendulumConstants
{	static int REVISION = 103;			// 100 represents the initial version of the game
										// 101 fixes the implementation of p1p1p1
										// 102 fixes a bunch of small bugs found in testing
										//  council reward logic incorrect, and wrong cost for M3 cards
		                                //  3 military for province (council card) not working
										//  discard province not triggered properly
										//  too hard to find the place to drop strat cards
										//  vp for first, second in priority
										//  set of cards for players were swapped
										// 103 fixes the council victory point for 3 player games

	public int getMaxRevisionLevel() { return(REVISION); }
	PendulumVariation variation = PendulumVariation.pendulum;
	PendulumState board_state = PendulumState.Puzzle;	
	public int placementIndex = -1;
	private PendulumState unresign = null;	// remembers the orignal states when "resign" is hit
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
	public int scoreForPlayer(int n)
	{
		return pbs[n].winnerScore();
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
	
	public Timer blackTimer = new Timer(PendulumChip.blackTimer,TColor.Black,45*1000);
	public Timer purpleTimer = new Timer(PendulumChip.purpleTimer,TColor.Purple,3*60*1000);
	public Timer greenTimer = new Timer(PendulumChip.greenTimer,TColor.Green,2*60*1000);
	public boolean timersRunning = false;
	public int privilegeResolutions = 0;
	private int flipCount = 0;
	private int flipsSinceLastPurple = 0;
	public int purpleTimerPhase = 0;
	public PendulumCell councilCards[] = newcell(PendulumId.RewardCard,8);
	public PendulumCell councilRewardsDeck = councilCards[0];
	public PendulumCell councilRewardsUsed = newcell(PendulumId.UsedRewardCard);
	public PendulumCell timerTrack[] = newcell(PendulumId.TimerTrack,11);
	public PendulumCell trash = newcell(PendulumId.Trash);
	public PendulumCell provinceCards = newcell(PendulumId.ProvinceCardStack);
	public PendulumCell achievementCards = newcell(PendulumId.AchievementCardStack);
	public PendulumCell currentAchievement = newcell(PendulumId.AchievementCard,BC.Achievement,BB.Achievement);
	public PendulumCell greenTimers[] = newcell(PendulumId.GreenTimer,2);
	public PendulumCell purpleTimers[] = newcell(PendulumId.PurpleTimer,4);
	public PendulumCell blackTimers[] = newcell(PendulumId.BlackTimer,2);
	public PendulumCell restButton = newcell(PendulumId.Rest);
	private PlayerBoard neutralPlayer = null;
	public PColor neutralColor()
	{
		return neutralPlayer==null ? null : neutralPlayer.color;
	}
	public boolean purpleHourglassTop()
	{
		return (purpleTimers[0].topChip()==PendulumChip.purpleTimer)
				|| (purpleTimers[1].topChip()==PendulumChip.purpleTimer);
	}
	public boolean blackHourglassTop()
	{
		return blackTimers[0].topChip()==PendulumChip.blackTimer;
	}
	public boolean greenHourglassTop()
	{
		return greenTimers[0].topChip()==PendulumChip.greenTimer;
	}
	public PendulumCell privilege[] = newcell(PendulumId.Privilege,MAX_PLAYERS);
	public PendulumCell achievement[] = newcell(PendulumId.Achievement,MAX_PLAYERS);
	public PendulumCell provinces[] = newcell(PendulumId.Province,4);
	
	public PendulumCell greenMeepleA[] = newcell(PendulumId.GreenMeepleA,3);
	public PendulumCell greenMeepleB[] = newcell(PendulumId.GreenMeepleB,3);
	// these are reloaded in doinit
	public BB[] green2pRewards = new BB[] {BB.Military1Vote2,BB.Culture2,BB.RedPB};
	public BB[] green4pRewards = new BB[] {BB.RedPB,BB.Culture2,BB.Military1Vote2};

	public PendulumCell greenActionA[] = newcell(PendulumId.GreenActionA,
				new BC[] {BC.D2Board,BC.D2Board,BC.D2Board},
				green2pRewards);
	public PendulumCell greenActionB[] = newcell(PendulumId.GreenActionB,
				new BC[] {BC.D2Board,BC.D2Board,BC.D2Board},
				green4pRewards);
	
	public PendulumCell blackMeepleA[] = newcell(PendulumId.BlackMeepleA,4);
	public PendulumCell blackMeepleB[] = newcell(PendulumId.BlackMeepleB,4);
	public PendulumCell blackActionA[] = newcell(PendulumId.BlackActionA,
			new BC[] {BC.None,BC.None,BC.M4Board,BC.None},
			new BB[] {BB.YellowPB,BB.Vote1,BB.Province,BB.Resource1});
	public PendulumCell blackActionB[] = newcell(PendulumId.BlackActionB,
			new BC[] {BC.None,BC.None,BC.M4Board,BC.None},
			new BB[] {BB.YellowPB,BB.Vote1,BB.Province,BB.Resource1});

	public PendulumCell purpleMeepleA[] = newcell(PendulumId.PurpleMeepleA,3);
	public PendulumCell purpleMeepleB[] = newcell(PendulumId.PurpleMeepleB,3);
	
	private BB[] purple4pRewards = new BB[] {BB.BrownPB,BB.Popularity1Prestige1Vote1,BB.BluePB};
	private BB[] purple2pRewards = new BB[] {BB.BluePB,BB.Popularity1Prestige1Vote1,BB.BrownPB};
	
	public PendulumCell purpleActionA[] = newcell(PendulumId.PurpleActionA,
			new BC[] {BC.D2Board,BC.D2Board,BC.D2Board},
			purple4pRewards);
	public PendulumCell purpleActionB[] = newcell(PendulumId.PurpleActionB,
			new BC[] {BC.D2Board,BC.D2Board,BC.D2Board},
			purple2pRewards);

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
	public int getPlayerPrivilege(PColor color)
	{	
		for(int i=0,lim=players_in_game==2?3:players_in_game;i<lim; i++)
		{
			if(privilege[i].topChip().color==color) { return i; }
		}
		throw G.Error("Can't find %s on the privilege track",color);
	}
	public int getPlayerPrivilege(int who) { return getPlayerPrivilege(pbs[who].color); }
	
	public PlayerBoard getPlayerBoard(PendulumChip ch)
	{
		PColor color = ch.color;
		for(PlayerBoard pb : pbs) { if(pb.meeple.color==color) { return pb; }}
		// edit mode, picking up a neutral piece
		return null;
	}
	public PlayerBoard getPlayerBoard(int n) { return pbs[n]; }
	public PlayerBoard getPlayerBoard(PendulumCell c)
	{
		return getPlayerBoard(c.col);
	}
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

    // save strings to be shown in the game log
    StringStack gameEvents = new StringStack();
    InternationalStrings s = G.getTranslations();
    
 	void logGameEvent(String str,String... args)
 	{	//if(!robotBoard)
 		{String trans = s.get(str,args);
 		 gameEvents.push(trans);
 		}
 	}

    PendulumState resetState = PendulumState.Puzzle; 
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
	
	public PendulumCell newcell(PendulumId id,BC cost,BB bene)
	{	PendulumCell c = newcell(id);
		c.cost = cost;
		c.benefit = bene;
		return c;
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
    	Tokenizer tok = new Tokenizer(gtype);
    	String typ = tok.nextToken();
    	int np = tok.hasMoreTokens() ? tok.intToken() : players_in_game;
    	long ran = tok.hasMoreTokens() ? tok.longToken() : key;
    	int rev = tok.hasMoreTokens() ? tok.intToken() : revision;
    	doInit(typ,ran,np,rev);
    }
    private void loadRewards(PendulumCell row[],BB[]rewards)
    {
    	for(int lim=rewards.length-1; lim>=0;lim--) { row[lim].benefit = rewards[lim]; }
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
		purpleTimerPhase = 0;
		privilegeResolutions = 0;
		flipCount = 0;
		flipsSinceLastPurple = 0;
		robotState.clear();
		gametype = gtype;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case pendulum:
		case pendulum_notimers:
		case pendulum_advanced:
		case pendulum_advanced_notimers:
			// using reInitBoard avoids thrashing the creation of cells 
			// when reviewing games.
			// or initBoard(variation.firstInCol,variation.ZinCol,null);
			// Random r = new Random(734687);	// this random is used to assign hash values to cells, common to all games of this type.
			// allCells.setDigestChain(r);		// set the randomv for all cells on the board
		}

		for(PendulumCell c = allCells; c!=null; c=c.next) { c.reInit(); }
		
		// randomize the starting mats
	    int matmap[] = AR.intArray(initial_pbs.length);
	    {
	    Random rr = new Random(randomKey+345235);
	    rr.shuffle(matmap);
	    for(int i=0;i<matmap.length;i++) { initial_pbs[i].setMat(matmap[i]); }
	    }
		
	    whoseTurn = FIRST_PLAYER_INDEX;
	    resetState = null;
	    lastDroppedObject = null;
	    int map[]=getColorMap();
	    pbs = new PlayerBoard[players_in_game];
	    for(int i=0;i<players_in_game;i++)
	    {
	    	pbs[i] = initial_pbs[map[i]];
	    	pbs[i].doInit(variation.advanced,i);
	    }
	    if(players_in_game==2)
	    {	// create a neutral player and give them 3 votes
	    	for(int i=0;i<initial_pbs.length;i++)
	    	{	// unused color
	    	if(initial_pbs[i]!=pbs[0] && initial_pbs[i]!=pbs[1])
	    	{
	    	neutralPlayer = initial_pbs[i];
	    	neutralPlayer.doInit(variation.advanced,2);
	    	neutralPlayer.collectBenefit(null,PB.V3Exactly,false,replayMode.Replay);
	    	purpleMeepleA[1].addChip(neutralPlayer.meeple);
	    	greenMeepleA[1].addChip(neutralPlayer.meeple);
	    	purpleMeepleB[0].addChip(neutralPlayer.meeple);
	    	greenMeepleB[0].addChip(neutralPlayer.grande);
	    	privilege[2].addChip(neutralPlayer.hexagon);
	    	break;
	    	}}
	    }
	    else { neutralPlayer = null; }
	    
	    // prepare the council deck
	    {
	    Random r = new Random(randomKey);
	    for(int lim=PendulumChip.rewardcards.length-2; lim>=0; lim--)
	    {
	    	councilRewardsDeck.addChip(PendulumChip.rewardcards[lim]);
	    }
	    councilRewardsDeck.shuffle(r);
	    }
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
	    {
	    Random r = new Random(randomKey+346346);
	    provinceCards.shuffle(r);
	    for(PendulumCell c : provinces) {c.addChip(provinceCards.removeTop()); }
	    
	    // prepare the achievement deck
	    for(int lim=PendulumChip.achievementcards.length-2; lim>=0; lim--)
	    {
	    	achievementCards.addChip(PendulumChip.achievementcards[lim]);
	    }
	    achievementCards.shuffle(r);
	    }
	    currentAchievement.addChip(achievementCards.removeTop());
	    if(players_in_game >=4) 
	    	{ currentAchievement.addChip(PendulumChip.legendary); 
	    	  greenMeepleA[2].rackLocation = PendulumId.GreenMeepleA;
	    	  greenMeepleB[2].rackLocation = PendulumId.GreenMeepleB;
	    	  purpleMeepleA[2].rackLocation = PendulumId.PurpleMeepleA;
	    	  purpleMeepleB[2].rackLocation = PendulumId.PurpleMeepleB;
	    	  loadRewards(purpleActionA,purple4pRewards);
	    	  loadRewards(purpleActionB,purple4pRewards);
	    	  loadRewards(greenActionA,green4pRewards);
	    	  loadRewards(greenActionB,green4pRewards);
	    	}
	    else
	    {	// different board for 2-3 player game
	    	  greenMeepleA[2].rackLocation = PendulumId.Unused;
	    	  greenMeepleB[2].rackLocation = PendulumId.Unused;
	    	  purpleMeepleA[2].rackLocation = PendulumId.Unused;
	    	  purpleMeepleB[2].rackLocation = PendulumId.Unused;
	    	  loadRewards(purpleActionA,purple2pRewards);
	    	  loadRewards(purpleActionB,purple2pRewards);
	    	  loadRewards(greenActionA,green2pRewards);
	    	  loadRewards(greenActionB,green2pRewards);
	    }
	    
        animationStack.clear();
        moveNumber = 1;
        placementIndex = 1;
        timerTrack[0].addChip(PendulumChip.grayGlass);
        greenTimers[0].addChip(PendulumChip.greenTimer);
        blackTimers[0].addChip(PendulumChip.blackTimer);
        purpleTimers[0].addChip(PendulumChip.purpleTimer);
        timersRunning = false;
        
        // mark the future purple timer slots
        placePurpleTimers(replayMode.Replay);
        {
        Random r = new Random(randomKey+2352);
        animationStack.shuffle(r);
        // randomize the initial privilege
        int par[] = AR.intArray(players_in_game);
        r.shuffle(par);
        for(int i=0;i<players_in_game; i++) 
        	{ privilege[i].addChip(pbs[par[i]].hexagon); 
        	  achievement[i].addChip(pbs[i].cylinder);
        	}
        }
	    dealCouncilCards(replayMode.Replay);
	}
    private void dealCouncilCards(replayMode replay)
    {
    	for(int i=1;i<=5;i++)
    	{	int idx = i>3 ? i+2 : i;
    		if(councilCards[idx].topChip()!=null)
    		{
    			councilRewardsUsed.addChip(councilCards[idx].removeTop());
    			if(replay.animate)
    			{
    				animate(councilCards[idx],councilRewardsUsed);
    			}
    			
    		}
    		councilCards[idx].reInit();
    		councilCards[idx].addChip(councilRewardsDeck.removeTop());
    		if(replay.animate) { 
    			animate(councilRewardsDeck,councilCards[idx]);
    		}
    	}
    	councilCards[4].addChip(PendulumChip.defcard);
    	councilCards[5].addChip(PendulumChip.flipcard);
    	
    }
    public void placePurpleTimers(replayMode replay)
    {	purpleTimerPhase = 0;
		trash.reInit();	// don't depend on the contents of the trash, so it can be
						// used ad-hoc as a trash
    	for(PendulumCell c : purpleTimers)
    	{
    		if(c.isEmpty())
    		{
    			c.addChip(PendulumChip.purpleGlass);
    			if(replay.animate) { animate(trash,c); }
    		}
    	}
    }
    /** create a copy of this board */
    public PendulumBoard cloneBoard() 
	{ PendulumBoard dup = new PendulumBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((PendulumBoard)b); }

    private Thread executeLock = null;
    private void getLock()
    {	Thread me = Thread.currentThread();
    	while(executeLock!=me)
    	{
    		synchronized(this)
    		{
    		if(executeLock==null) { executeLock =me; }
    		}
    		if(executeLock!=me)
    		{
    			G.print("Waiting for thread "+executeLock);
    			G.doDelay(1000);
    		}
    	}
    }
    private void releaseLock()
    {	Thread me = Thread.currentThread();
    	G.Assert(executeLock==me,"should have the lock");
    	executeLock = null;
    }
 
    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(PendulumBoard from_b)
    {	
    	getLock();
    	try {
        super.copyFrom(from_b);
        
        for(int i=0;i<players_in_game;i++) { pbs[i].copyFrom(from_b.pbs[i]); }
        
        robotState.copyFrom(from_b.robotState);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        provinceCards.copyFrom(from_b.provinceCards);
        blackTimer = from_b.blackTimer;
        greenTimer = from_b.greenTimer;
        purpleTimer = from_b.purpleTimer;
        timersRunning = from_b.timersRunning;
        placementIndex = from_b.placementIndex;
        achievementCards.copyFrom(from_b.achievementCards);
        currentAchievement.copyFrom(from_b.currentAchievement);
        councilRewardsDeck.copyFrom(from_b.councilRewardsDeck);
        copyFrom(councilCards,from_b.councilCards);
        copyFrom(councilRewardsUsed,from_b.councilRewardsUsed);
        copyFrom(timerTrack,from_b.timerTrack);
        copyFrom(provinces,from_b.provinces);
        copyFrom(greenTimers,from_b.greenTimers);
        copyFrom(blackTimers,from_b.blackTimers);
        copyFrom(purpleTimers,from_b.purpleTimers);
        copyFrom(trash,from_b.trash);
        restButton.copyFrom(from_b.restButton);
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
        purpleTimerPhase = from_b.purpleTimerPhase;
        privilegeResolutions = from_b.privilegeResolutions;
        flipCount = from_b.flipCount;
        flipsSinceLastPurple = from_b.flipsSinceLastPurple;
        purpleTimer = new Timer(from_b.purpleTimer);
        greenTimer = new Timer(from_b.greenTimer);
        blackTimer = new Timer(from_b.blackTimer);

        if(G.debug()) { sameboard(from_b); }
    	}
    	finally {
        releaseLock();
    	}
    }

    public int round()
    {
    	return 4-councilRewardsDeck.height()/5;
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
        G.Assert(councilRewardsUsed.sameContents(from_b.councilRewardsUsed),"councilRewardsUsed mismatch");
        G.Assert(sameContents(timerTrack,from_b.timerTrack),"timerTrack mismatch");
        G.Assert(sameContents(privilege,from_b.privilege),"privilege mismatch");
        G.Assert(sameContents(achievement,from_b.achievement),"achievement mismatch");
        G.Assert(purpleTimerPhase==from_b.purpleTimerPhase,"purpleTimerPhase mismatch");
        G.Assert(privilegeResolutions==from_b.privilegeResolutions,"privilegeResolutions mismatch");
        G.Assert(flipCount==from_b.flipCount,"flipCount mismatch");
        G.Assert(flipsSinceLastPurple==from_b.flipsSinceLastPurple,"flipsSinceLastPurple mismatch");
        G.Assert(restButton.sameContents(from_b.restButton),"rest button mismatch");
        G.Assert(trash.sameContents(from_b.trash),"trash mismatch");
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
		v ^= Digest(r,councilRewardsUsed);
		v ^= Digest(r,timerTrack);
		v ^= Digest(r,provinceCards);
		v ^= Digest(r,achievementCards);
		v ^= Digest(r,currentAchievement);
		v ^= Digest(r,greenTimers);
		v ^= Digest(r,purpleTimers);
		v ^= Digest(r,trash);
		v ^= Digest(r,restButton);
		v ^= Digest(r,privilege);
		v ^= Digest(r,achievement);
		v ^= Digest(r,blackTimers);
		
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

	    v ^= Digest(r,purpleTimerPhase);
	    v ^= Digest(r,privilegeResolutions);
	    v ^= Digest(r,flipCount);
	    v ^= Digest(r,flipsSinceLastPurple);
		v ^= Digest(r,whoseTurn);
        return (v);
    }



    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer(replayMode replay,boolean reverse)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Move not complete, can't change the current player in state ",board_state);
        case Puzzle:
        
            break;
        case Play:
        case CouncilPlay:
        	// some damaged games have 2 dones in a row
        	if(replay==replayMode.Live) { throw G.Error("Move not complete, can't change the current player in state ",board_state); }
			//$FALL-THROUGH$
        case Confirm:
        case CouncilRewards:
        case CouncilTrim:
        case PendingPlay:
        case Resign:
            moveNumber++; //the move is complete in these states
            setWhoseTurn(nextPlayer(whoseTurn,reverse));
            return;
        }
    }
    public int nextPlayer(int n,boolean reverse)
    {
        PlayerBoard pb = pbs[n];
        int po = getPlayerPrivilege(pb.color);
        int privilegeNPlayers = (players_in_game==2?3:players_in_game);
        int inc = (reverse ? -1 : 1);
        int pn = (po+privilegeNPlayers+inc)%privilegeNPlayers;
        PendulumChip ch = privilege[pn].topChip();
        if(neutralPlayer!=null && ch == neutralPlayer.hexagon)
        {
        	pn = (pn + privilegeNPlayers +inc ) % privilegeNPlayers;
        	ch = privilege[pn].topChip();
        }
        PlayerBoard next = getPlayerBoard(ch);
        return next.boardIndex;
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
    { 	
    	return movingObjectIndex(whoseTurn);
    }
    
    public int movingObjectIndex(int who)
    {
     	PlayerBoard pb = pbs[who];
    	PendulumChip ch = pb.pickedObject;
    	if(ch!=null) { return ch.chipNumber();}
    	
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
        case Unused:
        	return null;
        case AchievementCard:
        	return currentAchievement;
        case AchievementCardStack:
        	return achievementCards;
        case ProvinceCardStack:
        	return provinceCards;
        case PlayerRefill:
        case PlayerFreeD2Card:
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
        case PlayerPowerVP:
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
        case PlayerLegendary:
        case PlayerMax3Cards:
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
        case Trash:
        	return trash;
        case Rest:
        	return restButton;
        case TimerTrack:
        	return timerTrack[row];
        case RewardCard:
        	return councilCards[row];
        case UsedRewardCard:
        	return councilRewardsUsed;
        case RewardDeck:
        	return councilRewardsDeck;
        case GreenTimer:
        	return greenTimers[row];
        case BlackTimer:
        	return blackTimers[row];
        case PurpleTimer:
        	return purpleTimers[row];
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

    private void setNextStateAfterDrop(PlayerBoard pb,PendulumCell c,replayMode replay)
    {	resetState = board_state;
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state " + board_state);
        //case Confirm:
        //	setNextStateAfterDone(replay);
        // 	break;
        case Confirm:
        	if(c.rackLocation==PendulumId.Province) { break; }
        	else { G.Error("Not expected"); }
        	break;
        case CouncilPlay:
        case Play:
        	pb.setNextStateAfterDrop(c,replay);
        	break;
        case CouncilTrim:
        	if(!pb.needsCouncilTrim())
        	{
        		setState(PendulumState.Confirm);
        	}
        	break;
        case CouncilRewards:
        	if(c.rackLocation()==PendulumId.Province) { break; }
			//$FALL-THROUGH$
		case PlayMeeple:
        case PlayGrande:
        	pb.setNextStateAfterDrop(c,replay);
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
    private void countMissedGreenActions()
    {
    	PendulumCell row[] = greenHourglassTop() ? greenMeepleB : greenMeepleA;
    	for(PlayerBoard pb : pbs)
    	{
    		int n = pb.countMeeples(row);
    		pb.greenMissedActions += n;
    	}
    }
    private void countMissedBlackActions()
    {
    	PendulumCell row[] = blackHourglassTop() ? blackMeepleB : blackMeepleA;
    	for(PlayerBoard pb : pbs)
    	{
    		int n = pb.countMeeples(row);
    		pb.blackMissedActions += n;
    	}
    }
    private void countMissedPurpleActions()
    {
    	PendulumCell row[] = purpleHourglassTop() ? purpleMeepleB : purpleMeepleA;
    	for(PlayerBoard pb : pbs)
    	{
    		int n = pb.countMeeples(row);
    		pb.purpleMissedActions += n;
    	}
    }

    private void moveTimer(Timer timer,PendulumCell from0, PendulumCell to0, replayMode replay)
    {	PendulumCell from = from0;
    	PendulumCell to = to0;
    	if(from.topChip()==null) { from = to0; to=from0; }
     	if(replay.animate) { animationStack.push(from); animationStack.push(to); }
    	timer.flip();
    	if(to.topChip()!=null) 
    		{ trash.addChip(to.removeTop()); 
    		  if(replay.animate)
    		  {
    		  animationStack.push(to);
    		  animationStack.push(trash); 
    		  }
    		}
    	to.addChip(from.removeTop());
    }
    private void flipAllTimers(replayMode replay)
    {
    	flipBlackTimer(null,replay);
    	flipGreenTimer(null,replay);
    	flipPurpleTimer(null,replay);
    	timersRunning=true;
    }
    private void acceptPlacements(PendulumCell row[])
    {
    	for(PlayerBoard pb : pbs) { pb .acceptPlacements(row); }
    }
    private void flipGreenTimer(PendulumCell c,replayMode replay)
    {	if(c==null || c.topChip()!=null)
    	{
    	unRest();
		flipsSinceLastPurple++;
    	moveTimer(greenTimer,greenTimers[0],greenTimers[1],replay);
    	countMissedGreenActions();
    	acceptPlacements(greenMeepleA);
    	acceptPlacements(greenMeepleB);
    	acceptPlacements(greenActionA);
    	acceptPlacements(greenActionB);
    	}
   		else if(c!=null)
   		{
		privilegeResolutions++; 
		logGameEvent(SimultaneousFlipMessage);
   		}
    }
    private void unRest()
    {	flipCount++;
    	for(PlayerBoard pb : pbs) { pb.unRest(); }
    }
    private void flipBlackTimer(PendulumCell c,replayMode replay)
    {	if(c==null || c.topChip()!=null)
    	{
    	unRest();
    	flipsSinceLastPurple++;
    	moveTimer(blackTimer,blackTimers[0],blackTimers[1],replay);
    	countMissedBlackActions();
       	acceptPlacements(blackMeepleA);
    	acceptPlacements(blackMeepleB);
    	acceptPlacements(blackActionA);
    	acceptPlacements(blackActionB);
    	}
    	else if(c!=null)
    	{
    		privilegeResolutions++; 
    		logGameEvent(SimultaneousFlipMessage);
    	}
    }
    private void flipPurpleTimer(PendulumCell c,replayMode replay)
    {	// first locate the timer!
    	unRest();
    	flipsSinceLastPurple = 0;
    	boolean satisfied = false;
    	for(int i=0;i<purpleTimers.length && !satisfied;i++)
    	{
    		PendulumCell from = purpleTimers[i];
    		if(from.topChip()==PendulumChip.purpleTimer)
    		{
        		if(c!=null && from!=c) 
        			{ privilegeResolutions++; 
        			  logGameEvent(SimultaneousFlipMessage);
        			return; 
        			}
        		// next locate a destination
    			int destIndex = i<2 ? 2 : 0;
    			for(int d=0;d<2 && !satisfied;d++)
    			{
    				PendulumCell to = purpleTimers[d+destIndex];
    				if(!to.isEmpty())
    				{
    					// found one
    					moveTimer(purpleTimer,from,to,replay);
    					purpleTimerPhase++;
    					if(purpleTimerPhase==3)
    					{
    						setState(resetState = PendulumState.CouncilPlay);
    					}
    					satisfied=true;
    				}
    			}
    		}
    	}
    	countMissedPurpleActions();
       	acceptPlacements(purpleMeepleA);
    	acceptPlacements(purpleMeepleB);
    	acceptPlacements(purpleActionA);
    	acceptPlacements(purpleActionB);

    }
    public void doTimers(long dif)
    {	
    	if(timersRunning) 
    	{
    		greenTimer.useTime(dif);
    		blackTimer.useTime(dif);
    		purpleTimer.useTime(dif);
    	}
    }
    private int gamePhase()
    {	
    	return 4-(councilRewardsDeck.height()/5);
    }
    private void advanceCouncilTrim(replayMode replay)
    {	
    	PlayerBoard pb = pbs[whoseTurn];
    	while(board_state==PendulumState.CouncilTrim 
    			&& (revision<102 ? !pb.needsCouncilTrim() : !pbs[whoseTurn].needsCouncilTrim()))
    	{	setNextPlayer(replay,true);
    		if(whoseTurn==privilegeLastPlayer())
    		{
    				setState(PendulumState.PendingPlay);
    				placePurpleTimers(replay);
    	          	trash.addChip(currentAchievement.chipAtIndex(0));
    	          	currentAchievement.reInit();
    	           	currentAchievement.addChip(achievementCards.removeTop());
    	     	    currentAchievement.addChip(PendulumChip.legendary); 
    	     	    if(replay.animate) {
    	     	    	animate(currentAchievement,trash);
    	     	    	animate(achievementCards,currentAchievement);
    	     	    	animate(achievementCards,currentAchievement);
    	     	    }
    	        	dealCouncilCards(replay);

    		}
    	}
    	G.Assert(councilRewardsDeck.height()%5==0,"should be a multiple");
    }
    private void doGameOver(replayMode replay)
    {	int maxScore = 0;
    	int maxPlayer = -1;
    	boolean tie = false;
     	for(PlayerBoard pb : pbs)
    	{	int who = pb.boardIndex;
    		int sc = scoreForPlayer(who);
    		if(maxPlayer<0 || sc>=maxScore)
    		{
    			tie = sc==maxScore;
    			maxScore =sc;
    			maxPlayer = who;
    		}
    	}
    	if((maxScore>PlayerBoard.LEGENDARY_SCORE) && !tie)
    	{
    		win[maxPlayer] = true;
    	}
    	setState(PendulumState.Gameover);
    	if(replay==replayMode.Live)
    	{
    	for(PlayerBoard pb : pbs)
    	{
    		pb.printStats(System.out);
    	}}
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
    				{ setState(PendulumState.PendingPlay); 
    				}
    			else { setState(resetState); }
    			break;
    		case PlayGrande:
    			if(getPlayerPrivilege(whoseTurn)==0) { setState(PendulumState.PlayMeeple); }
    			else { setState(resetState); }
    			break;
    		case CouncilRewards:
    			{
    			PlayerBoard pb = pbs[whoseTurn];
     			if(pb.uiState==UIState.Normal)	// some enter a collect province phase
    			{
    			if(whoseTurn==privilegeFirstPlayer())	// back to the start?
    				{
    				if(gamePhase()==4) { doGameOver(replay); }
    				else
    				{
    				setState(resetState = PendulumState.CouncilTrim);
    				setNextPlayer(replay,true);	// step backwards
    				advanceCouncilTrim(replay);
    				}}
    				else 
    				{ setState(resetState = PendulumState.CouncilRewards); 
    				  if(revision>=102)
    				  {
    				  int order = getPlayerPrivilege(pb.color);
    				  // second and third players get 1 point
    				  	if(revision<103 || players_in_game>3)
    				  	{
    				  		if(order<=2) { pb.setUIState(UIState.P1P1P1Once,1); }
    				  	}
    				  	else
    				  	{	// 3 player games, points for the second player 
    				  		if(order<=1) { pb.setUIState(UIState.P1P1P1Once,1); }
    				  	}
    				  }
    				}
    			}}
    			break;
    		case CouncilTrim:
    			setNextPlayer(replay,true);	// reverse the advance made by doDone
    			setState(resetState);		// confirm->counciltrim
    			advanceCouncilTrim(replay);
    			break;
    		default:
    			p1("confirm-"+resetState);
    			G.Error("Not expecting confirm from %s",resetState);
    		}
    		break;
    	case CouncilPlay:
    		setState(PendulumState.CouncilPlay);
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
    	if(resetState!=null)
    	{switch(resetState)
    	{	
    	case CouncilRewards:
    		PlayerBoard pb = pbs[whoseTurn];
    		switch(pb.uiState)
    		{
    		default: throw G.Error("Not expecting UI state %s",pb.uiState);
    		case P1P1P1Once:
    			pb.collectVPIncrease(1,replay);
    			setState(resetState);
    			return;
    			
    		case P1P1P1Twice:
    			pb.collectVPIncreaseTwice(replay);
    			setState(resetState);
    			return;
    		case P1P1P1:
    			pb.collectVPIncrease(1,replay);
    			break;
       		case P2P2P2:
    			pb.collectVPIncrease(2,replay);
    			break;
    		case Normal:
    			pb.collectCouncil(replay);
     			if(pb.uiState!=UIState.Normal)
     				{ setState(resetState);
     				  return; 
     				}
    			break;
    		case PromoteMeeple:
    			pb.doMeeplePromotion(replay);
    			pb.setUIStateNormal();
    			break;
    		case ProvinceReward:
    			pb.setUIStateNormal();
    			break;
       		}
    		pb.acceptPlacement();
    		break;
    	default: break;
    	}}

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
        	else {setNextPlayer(replay,false);
        		setNextStateAfterDone(replay);
        	}
        }
    }
	public boolean allResting()
	{
		for(PlayerBoard pb : pbs) { if(!pb.isResting()) { return false; }}
		return true;
	}
	public boolean allReady()
	{
		for(PlayerBoard pb : pbs) { if(!pb.isReady()) { return false; }}
		return true;
	}
	public void startNormalPlay(replayMode replay)
	{	restButton.reInit();
		setState(resetState=PendulumState.Play);
		restButton.reInit();
		for(PlayerBoard pb : pbs)
		{	pb.acceptPlacement();
			pb.setUIStateNormal();
		}
		whoseTurn = privilegeFirstPlayer();
	}
	public void startCouncil(replayMode replay)
	{	setState(resetState=PendulumState.CouncilRewards);
		restButton.reInit();
		for(PlayerBoard pb : pbs)
		{	pb.acceptPlacement();
			pb.setUIStateNormal();
		}
		reorderPrivilege(replay);	// reorder the privilege and clear votes
		whoseTurn = privilegeFirstPlayer();
		if(revision>=102)
		{
			pbs[whoseTurn].setUIState(UIState.P1P1P1Twice,2);	// first player gets 2 points
		}
	}
	
	// get the first player in privilege order
	private int privilegeFirstPlayer()
	{	PendulumChip ch = privilege[0].topChip();
		if(neutralPlayer!=null && neutralPlayer.hexagon==ch) { ch = privilege[1].topChip(); }
		return getPlayerBoard(ch).boardIndex;
	}
	private int privilegeLastPlayer()
	{	int first = privilegeFirstPlayer();
		int v = nextPlayer(first,true);
		return v;
	}
	private void reorderPrivilege(replayMode replay)
	{
		PlayerBoard newpb[] = new PlayerBoard[players_in_game==2?3:players_in_game];
		PendulumCell original[] = new PendulumCell[privilege.length];

		AR.copy(original,privilege);
		for(int i=0;i<players_in_game;i++)
		{	// don't use AR.copy because lengths may not match
			newpb[i]=pbs[i];
		}
		if(players_in_game==2) 
		{ // neutral player has 3 votes
		  newpb[2] = neutralPlayer; 
		}
		Sort.sort(newpb);
		for(int i=0;i<newpb.length;i++)
		{
			privilege[i].reInit();
			privilege[i].addChip(newpb[i].hexagon);
		}
		for(int i=0;i<players_in_game; i++)
			{ achievement[i].reInit();
			  achievement[i].addChip(pbs[i].cylinder);
			}
		
		if(replay.animate)
		{
			for(int i=0;i<newpb.length;i++)
			{	PlayerBoard pb = newpb[i];
				if(original[i].topChip()!=pb.hexagon)
				{
					for(int j=0;j<original.length;j++)
					{
						if(original[j].topChip()==pb.hexagon)
						{
							animate(original[j],privilege[i]);
						}
					}
				}
			}
		}
		for(PlayerBoard pb : pbs) { pb.clearVotes(replay); }
	}
	public void advanceFlipTimer(replayMode replay)
	{	for(PlayerBoard pb : pbs) { pb.setUIStateNormal(); }
		setState(PendulumState.Play);
		restButton.reInit();
		moveNumber++;
		for(int i=0,limit=timerTrack.length;i<limit;i++)
		{	PendulumCell from = timerTrack[i];
			if(!from.isEmpty())
			{	PendulumCell to = timerTrack[(i+1)%limit];
				if(replay.animate){ animate(from,to); }
				to.addChip(from.removeTop());
				switch(i+1)
				{
				case 0:	break;
				case 1:
				case 2: 
				case 4:
				case 6:
				case 8:
				case 9:
					flipBlackTimer(null,replay); 
					break;
				case 3:
				case 7:
					flipGreenTimer(null,replay);
					break;
				case 10:
					flipGreenTimer(null,replay);
					setState(PendulumState.CouncilPlay);
					//$FALL-THROUGH$
				case 5:
					flipBlackTimer(null,replay); 
					flipPurpleTimer(null,replay);
					break;
				case 11:
					startCouncil(replay);
					break;
				default: G.Error("Not expecting %s",i);
				}
			break;
			}
		}
	}
	public boolean meepleRow(PendulumCell c)
	{
		switch(c.rackLocation())
		{
		case BlackMeepleA:
		case BlackMeepleB:
		case GreenMeepleA:
		case GreenMeepleB:
		case PurpleMeepleA:
		case PurpleMeepleB:
			return true;
		default: return false;
		}
	}
    public boolean Execute(commonMove mm,replayMode replay)
    {	getLock();
    	try {
		//G.print("E "+mm);
   		PendulumMovespec m = (PendulumMovespec)mm;
    	m.player = m.forPlayer;
        if(replay.animate) { animationStack.clear(); }
        
        if(robot==null)
        {
        if(replay==replayMode.Live)
        {	
        	m.blackTimer = (int)blackTimer.timeToRun;
        	m.greenTimer = (int)greenTimer.timeToRun;
        	m.purpleTimer = (int)purpleTimer.timeToRun;
        	m.realTime = G.Date();
        }
        else
        {   
        	blackTimer.timeToRun = m.blackTimer;
        	greenTimer.timeToRun = m.greenTimer;
        	purpleTimer.timeToRun = m.purpleTimer;        	
        }}
        
        try {
        //G.print("E "+m+" for "+whoseTurn+" "+ board_state);
        switch (m.op)
        {
        case MOVE_WAIT:
        	// this is used by the robot to advance the timers in its search
        	{
        	int amount = m.from_row;
        	purpleTimer.useTime(amount);
        	greenTimer.useTime(amount);
        	blackTimer.useTime(amount);
        	}
        	break;
        case MOVE_LEGENDARY_ACHIEVEMENT:
        	{
        	PlayerBoard pb = pbs[m.forPlayer];
        	pb.legendary.addChip(currentAchievement.removeChipAtIndex(1));
        	if(replay.animate)
        		{
        		animate(currentAchievement,pb.legendary);
        		}	
        	pb.setUIStateNormal();
        	}
        	break;
        case MOVE_STANDARD_ACHIEVEMENT:
        	{
        	PlayerBoard pb = pbs[m.forPlayer];
        	PendulumChip chip = currentAchievement.chipAtIndex(0);
        	pb.collectBenefit(currentAchievement,chip.pb[0],false,replay);
        	pb.setUIStateNormal();
        	}
        	break;
        case MOVE_DONE:

         	doDone(replay);

            break;
        case MOVE_STARTPLAY:
        	// additional startplay may arrive late, just ignore them
        	if(board_state==PendulumState.StartPlay)
        	{
        	flipAllTimers(replay);
         	startNormalPlay(replay);
        	}
        	break;
        case MOVE_AUTOFLIP:
        	// additional flip may arrive late, just ignore them
        	if(board_state==PendulumState.Flip)
        	{
        	advanceFlipTimer(replay);
        	}
        	break;
        case MOVE_REFILL:
        	{
        	PlayerBoard pb = getPlayerBoard(m.forPlayer);
        	PendulumCell c = pb.refill;
        	pb.payCost(c,c.topChip(), replay);
        	pb.collectBenefit(c,c.topChip(),replay);
        	}
        	break;
        case MOVE_STARTCOUNCIL:
        	// additional start council may arrive late, just ignore them
        	if(board_state==PendulumState.StartCouncil)
        	{
        	startCouncil(replay);
        	}
        	break;
        case MOVE_READY:
        	{
        	PlayerBoard pb = getPlayerBoard(m.forPlayer);
        	// additional "ready" can arrive late, just ignore them
        	if(resetState==PendulumState.PendingPlay)
        	{
        	restButton.addChip(pb.cylinder);
        	pb.setUIState(UIState.Ready,1);
        	if(allReady())
        		{
        		for(PlayerBoard p : pbs) { p.setUIStateNormal(); }
        		setState(PendulumState.StartPlay);
        		}
        	else {
        		setWhoseTurn(nextPlayer(whoseTurn,false));
        		}
        	}}
        	break;
        case MOVE_REST:
        	if(simultaneousTurnsAllowed())
        	{
        	PlayerBoard pb = getPlayerBoard(m.forPlayer);
        	if(pb.uiState==UIState.Rest)
        	{
        		restButton.removeChip(pb.cylinder);	
        		pb.setUIStateNormal();
        	}
        	else
        	{
        	restButton.addChip(pb.cylinder);
        	pb.setUIState(UIState.Rest,1);
        	}
        	if(allResting())
        		{
        		if(!variation.timers) { setState(PendulumState.Flip); }
        		else { setState(PendulumState.StartCouncil);} 
        		}
        		else 
        		{
        			setWhoseTurn(nextPlayer(whoseTurn,false));
        		}
        	}
        	break;
        case MOVE_SWAPVOTES:
        	{
        		PlayerBoard pb = getPlayerBoard(m.forPlayer);
        		PendulumCell from = getCell(m.source,m.from_col,m.from_row);
        		PendulumCell to = getCell(m.dest,m.to_col,m.to_row);
        		pb.changeVP(from,-1,replay);
        		pb.changeVP(to,1,replay);
        		pb.setUIStateNormal();
        	}
        	break;
        case MOVE_FROM_TO:
        	{
           	PendulumCell c = getCell(m.source,m.from_col,m.from_row);
        	PlayerBoard pb = getPlayerBoard(m.forPlayer);
           	{
        	G.Assert(pb.uiState!=UIState.Rest,"shouldn't be resting");
        	if(!pb.pickObject(c,m.chip))
        		{	// the pick can fail in case of simultaneous activity
        		break;
        		}
         	}
        	{
        	PendulumCell d = getCell(m.dest,m.to_col,m.to_row);
        	if(pb.doDrop(m,d,board_state,true,replay))
        		{
        		d.lastMoved = flipCount;
            	setNextStateAfterDrop(pb,d,replay);
        		}
        	else { pb.unPickObject(m); }
        	if(replay.animate) 
        	{
        		animate(c,d);
        	}
        	}}
        	break;
        case MOVE_SELECT:
	    	{
	    	PendulumCell c = getCell(m.source,m.from_col,m.from_row);
	    	PlayerBoard pb = getPlayerBoard(m.forPlayer);
	    	PendulumCell sel = pb.selectCell(c,replay);
	    	setState(sel!=null ? PendulumState.Confirm : resetState);
	    	}
	    	break;
        case MOVE_PICK:
        	{
        	PendulumCell c = getCell(m.source,m.from_col,m.from_row);
        	PlayerBoard pb = getPlayerBoard(m.forPlayer);
        	if(pb.isDest(c) && (!meepleRow(c) || (pb.droppedObject==m.chip)))
        	{ 
        	  pb.unDropObject(); setState(resetState); 
        	}
        	else { pb.pickObject(c,m.chip); }
        	}
            break;

        case MOVE_DROP: // drop on chip pool;
        	{
        	PendulumCell c = getCell(m.dest,m.to_col,m.to_row);
        	PlayerBoard pb = getPlayerBoard(m.forPlayer);
        	if(pb.isSource(c)) { pb.unPickObject(m); }
        	else 
        	{	// drop can fail if the state has changed while in transit
        		if(pb.doDrop(m,c,board_state,false,replay))
        		{
        		c.lastMoved = flipCount;
        		setNextStateAfterDrop(pb,c,replay);
        		}
        	}}
            break;
        case MOVE_SETACTIVE:
        	setWhoseTurn(m.forPlayer);
        	break;
        	
        case MOVE_START:
        	if((purpleTimerPhase==0) && !timersRunning)
        		{ setWhoseTurn(privilegeFirstPlayer()); 
        		  setState(PendulumState.PlayGrande);
        		}
        	else
        	{
        	for(int i=0;i<purpleTimers.length;i++)
        	{
        		if(purpleTimers[i].topChip()==PendulumChip.purpleTimer)
        		{	switch(i)
        			{	
        			case 0:	purpleTimerPhase = 0; break;
        			case 1: purpleTimerPhase = 2; break;
        			case 2: purpleTimerPhase = 1; break;
        			case 3: purpleTimerPhase = 3; break;
        			default: break;
        			}
        		}
        	}
            setWhoseTurn(m.forPlayer);
            acceptPlacement();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(PendulumState.Play);	// standardize the current state
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
       case MOVE_FLIP:
       		{
       			PendulumCell c = getCell(m.source,m.from_col,m.to_row);
       			moveNumber++;
       			switch(c.rackLocation())
       			{
       			case BlackTimer:
       				flipBlackTimer(c,replay);
       				break;
       			case GreenTimer:
       				flipGreenTimer(c,replay);
       				break;
       			case PurpleTimer:
       				flipPurpleTimer(c,replay);
       				break;
       			default: G.Error("can't flip %s",c);
       			}
       		}
       		break;
        default:
        	cantExecute(m);
        }

        //G.print("p "+purpleTimer.timeToRun);
        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }
        }
        catch (Throwable err)
        {
        	if(robot!=null) { p1("error "+moveNumber); 
        	G.print("error ",m,err,"\n",err.getStackTrace());
        	}
        	throw err;
        }
        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
    	}
    	finally {
        releaseLock();
    	}
        return (true);
    }


    public boolean legalToHitBoard(PendulumCell c,Hashtable<PendulumCell,PendulumMovespec> targets )
    {	if(c==null) { return(false); }
    	G.Assert(c==getCell(c),"not from the same board %s",c);
        switch (board_state)
        {
        case Puzzle:
		case Play:
		case PendingPlay:
		case CouncilPlay:
		case PlayGrande:
		case CouncilRewards:
		case CouncilTrim:
		case PlayMeeple:
		case StartPlay:
		case StartCouncil:
		case Flip:
		case Confirm:
			return(targets.get(c)!=null);
		case Gameover:
		case Resign:
			return(false);
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
        //G.print("R "+m);
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        Execute(m,replayMode.Replay);
        
    }
 

 private boolean addActionMoves(CommonMoveStack all,int who)
 {	boolean some = false;
 	{boolean greenTop = greenHourglassTop();
 	{
 	if(greenTop)
 	{
 		some |= addAction2_1Moves(all,greenMeepleA,greenActionA,who);
 	}
 	else
 	{
 		some |= addAction1_2Moves(all,greenMeepleB,greenActionB,who);
 	}}}
 	if(some && all==null) { return some; }
 	{
	boolean purpleTop = purpleHourglassTop();
 	if(purpleTop)
 	{
 		some |= addAction2_1Moves(all,purpleMeepleA,purpleActionA,who);
 	}
 	else
 	{
 		some |= addAction1_2Moves(all,purpleMeepleB,purpleActionB,who);
 	}
 	}
 	if(some && all==null) { return some; }
 	
 	{
 	boolean blackTop = blackHourglassTop();
 	PendulumCell[] blackRow = blackTop ? blackMeepleA : blackMeepleB;
	PendulumCell[] blackARow = blackTop ? blackActionA : blackActionB;
	some |= addAction1_1Moves(all,blackRow,blackARow,who);
 	}
 	return some;
 }

 // this is used to actually pick workers for retrieval
 public boolean hasRetrieveWorkerMoves(int who)
 {

	return addRetrieveWorkerMoves(null,who); 
 }
 public boolean hasProvincesAvailable()
 {
	 if(provinceCards.height()>0) { return true; }
	 for(PendulumCell c : provinces)
	 {
		if(c.height()>0) { return true; } 
	 }
	 return false;
 }
 // this is used to validate that the action is meaningful as part of the cost
 public boolean hasRetrievableWorkers(int who)
 {	 PColor color = pbs[who].color;
 	{
	 boolean greenTop = greenHourglassTop();
	 {
	 PendulumCell[] greenRow = greenTop ? greenMeepleA : greenMeepleB;
	 if(hasRetrievableWorkers(greenRow,color)) { return true; };
	 }
	 {
	 PendulumCell[] greenARow = greenTop ? greenActionA : greenActionB;
	 if(hasRetrievableWorkers(greenARow,color))  { return true; };
	 }}
	 
 	{
 	 boolean blackTop = blackHourglassTop();
 	 {
	 PendulumCell[] blackRow = blackTop ? blackMeepleA : blackMeepleB;
	 if(hasRetrievableWorkers(blackRow,color)) { return true; };
 	 }
 	 {
 		 PendulumCell[] blackARow = blackTop ? blackActionA : blackActionB;
 		 if(hasRetrievableWorkers(blackARow,color)) { return true; };
 	 }}
 	
 	{
 		boolean purpleTop = purpleHourglassTop();
 		{
 		 PendulumCell[] purpleRow = purpleTop ? purpleMeepleA : purpleMeepleB;
 		 if(hasRetrievableWorkers(purpleRow,color)) { return true; };
 	 	 }
 	 	 {
 	 		 PendulumCell[] purpleARow = purpleTop ? purpleActionA : purpleActionB;
 	 		 if(hasRetrievableWorkers(purpleARow,color)) { return true; };
 	 	 }}
 	return false;
 }
 
 private boolean addRetrieveWorkerMoves(CommonMoveStack all,int who)
 {	boolean some = false;
 	{
	 boolean greenTop = greenHourglassTop();
	 PendulumCell[] greenRow = greenTop ? greenMeepleA : greenMeepleB;
	 some |= addWorkerMoves(all,greenRow,true,who);
	 if(some && all==null) { return true; }
	 PendulumCell[] greenARow = greenTop ? greenActionA : greenActionB;
	 some |= addWorkerMoves(all,greenARow,true,who);
	 if(some && all==null) { return some; }

 	}
 	{
 	 boolean blackTop = blackHourglassTop();
	 PendulumCell[] blackRow = blackTop ? blackMeepleA : blackMeepleB;
	 some |= addWorkerMoves(all,blackRow,true,who);
	 if(some && all==null) { return true; }
	 PendulumCell[] blackARow = blackTop ? blackActionA : blackActionB;
	 some |= addWorkerMoves(all,blackARow,true,who);
	 if(some && all==null) { return true; }
 	}	 
 	{
	 boolean purpleTop = purpleHourglassTop();
	 PendulumCell[] purpleRow = purpleTop ? purpleMeepleA : purpleMeepleB;
	 some |= addWorkerMoves(all,purpleRow,true,who);
	 if(some && all==null) { return true; }	 
	 PendulumCell[] purpleARow = purpleTop ? purpleActionA : purpleActionB;
	 some |= addWorkerMoves(all,purpleARow,true,who);
	 if(some && all==null) { return true; }
 	}
 	return some;
 }
 private boolean addAction1_2Moves(CommonMoveStack all,PendulumCell from[],PendulumCell to[],int who)
 {	boolean some = false;
 	if(players_in_game>=4)
	{
	 some |= addAction1_1Moves(all,from,to,who);
	}
 	else
 	{
 		some |= addActionMoves(all,from[0],to[0],who);
 	 	if(some && all==null) { return some; }
 		some |=  addActionMoves(all,from[1],to[1],who);
 	 	if(some && all==null) { return some; }
		some |=  addActionMoves(all,from[1],to[2],who);
 	}
 	return some;
 }
 private boolean addAction2_1Moves(CommonMoveStack all,PendulumCell from[],PendulumCell to[],int who)
 {	boolean some = false;
 	if(players_in_game>=4)
 	{
	 some |= addAction1_1Moves(all,from,to,who);
 	}
 else
 	{
	 some |= addActionMoves(all,from[0],to[0],who);
	 	if(some && all==null) { return some; }
	 some |= addActionMoves(all,from[0],to[1],who);
	 	if(some && all==null) { return some; }
	 some |= addActionMoves(all,from[1],to[2],who);
 	}
 	return some;
 }
 
 private boolean addAction1_1Moves(CommonMoveStack all,PendulumCell from[],PendulumCell to[],int who)
 {	boolean some = false;
	 for(int i=0;i<from.length;i++)
	 {	
		 some |= addActionMoves(all,from[i],to[i],who);
		if(some && all==null) { return some; }
	 }
	 return some;
 }
 private boolean addActionMoves(CommonMoveStack all,PendulumCell from,PendulumCell to,int who)
 {	 PColor color = getPlayerColor(who);
 	 boolean some = false;
 	 PlayerBoard pb = pbs[who];
 	 if(pb.pickedObject!=null)
 	 {
 	 if((from==pb.pickedSource) && pb.canPayCost(to.cost,pb.pickedObject))
 	 {
 		some = true;
 		if(all!=null) { all.push(new PendulumMovespec(MOVE_FROM_TO,from,pb.pickedObject,to,who)); }
 	 }}
 	 else
 	 {
 	 for(int lim=from.height()-1; lim>=0; lim--)
 	 {
 	 PendulumChip chip = from.chipAtIndex(lim);
 	 if((chip.color==color) && pb.canPayCost(to.cost,chip))
 	 	{some = true;
 		 if(all!=null) { all.push(new PendulumMovespec(MOVE_FROM_TO,from,from.chipAtIndex(lim),to,who)); }
 		 else { break; }
 	 	}
 	 }}
 	 return some;
 }
 private boolean addFlipTimerMoves(CommonMoveStack all,int who)
 {	 boolean some = false;
 	 int flipable = 0;
	 if(robot==null || flipsSinceLastPurple<20)
 	 {
	 // this logic prevents the game from going on forever if the
	 // robots just want to run up the green and black resource actions.
	 if(addFlipTimerMoves(all,greenTimer,greenTimers,who))
	 {
		 flipable++;
	 }
	 if(addFlipTimerMoves(all,blackTimer,blackTimers,who))
	 {
		 flipable++;
	 }
 	 }
	 if(addFlipTimerMoves(all,purpleTimer,purpleTimers,who))
	 {
		 flipable++;
	 }
	 if(flipable<3)
	 {
	 // consider a small delay
	 PendulumMovespec m = new PendulumMovespec(MOVE_WAIT,who);
	 m.from_row =m.to_row = 5000;	// 5 seconds
	 all.push(m);	// wait for the timers
	 some = true;
	 }
	 return some;
 }
 private void addClaimAchievementMoves(CommonMoveStack all,int who)
 {		PlayerBoard pb = pbs[who];
 		PColor color = pb.color;
 		
 		if(pb.pickedObject!=null)
 		{ if(pb.pickedSource.rackLocation()==PendulumId.Achievement)
 		{
 			addClaimAchievementMoves(all,pb.pickedSource,pb.pickedObject,who);
 		}}
 		else
 		{
 		for(PendulumCell c : achievement)
 		{
 			PendulumChip ch = c.topChip();
 			if(ch!=null && ch.color==color)
 			{
 				addClaimAchievementMoves(all,c,ch,who);
 			}
 		}}
 }
 //
 // return true if (as far as we know) anyone else is currently deciding
 // if they should accept the legendary achievement
 //
 PlayerBoard someoneIsDeciding()
 {	
	 for(PlayerBoard pb : pbs) { if(pb.uiState==UIState.AchievementOrLegandary) { return pb; }}
	 return null;
 }
 private void addClaimAchievementMoves(CommonMoveStack all,PendulumCell from,PendulumChip top,int who)
 {	PlayerBoard pb = pbs[who];
 	PendulumChip current = currentAchievement.chipAtIndex(0);
 	if(pb.canPayCost(current.pc))
 	{	// special conflict resolution for claiming the legendary achievement.
 		// if we could claim it, and anyone else is pondering claiming it, we
 		// can't drop on the claim card.  This puts any multiple attempts to 
 		// claim the achievement in the privilege resolution zone
 		if(pb.legendary.isEmpty()
 				&& (currentAchievement.findChip(PendulumChip.legendary)>=0)
 				&& someoneIsDeciding()!=null)
 		{
 			//p1("avoid legendary conflict");
 		}
 		else
 		{
 		all.push(new PendulumMovespec(MOVE_FROM_TO,from,top,currentAchievement,who));
 		}
 	}
 }
 private boolean addFlipTimerMoves(CommonMoveStack all,Timer timer,PendulumCell cells[],int who)
 {	PlayerBoard pb = pbs[who];
 	boolean some = false;
 	if(pb.pickedObject==null)
 	{
	 if(timer.running && timer.timeToRun<=0)
	 {	 PendulumId rack = cells[0].rackLocation();
		 for(PendulumCell c : cells)
		 {
			 PendulumChip top = c.topChip();
			 if((top!=null) && top.id==rack)
			 {
				 all.push(new PendulumMovespec(MOVE_FLIP,c,top,who));
				 some = true;
			 }
		 }
	 }}
 	return some;
 }
 private void addWorkerMoves(CommonMoveStack all,int who)
 {	
	 if(greenHourglassTop())
	 {
		 addWorkerMoves(all,greenMeepleB,false,who);
		 addWorkerMoves(all,greenActionB,false,who);
	 }
	 else
	 {
		 addWorkerMoves(all,greenMeepleA,false,who);
		 addWorkerMoves(all,greenActionA,false,who);
	 }
	 
	 if(blackHourglassTop())
	 {
		 addWorkerMoves(all,blackMeepleB,false,who);
		 addWorkerMoves(all,blackActionB,false,who);	 
	 }
	 else
	 {
		 addWorkerMoves(all,blackMeepleA,false,who);
		 addWorkerMoves(all,blackActionA,false,who);
	 }
	 
	 if(purpleHourglassTop())
	 {
		 addWorkerMoves(all,purpleMeepleB,false,who);
		 addWorkerMoves(all,purpleActionB,false,who); 
	 }
	 else
	 {
		 addWorkerMoves(all,purpleMeepleA,false,who);
		 addWorkerMoves(all,purpleActionA,false,who);
	 }

 }
 private boolean hasRetrievableWorkers(PendulumCell from[],PColor color)
 {	
	 for(PendulumCell c : from)
	 {
		 for(int lim=c.height()-1; lim>=0 ; lim--)
			 {	 PendulumChip worker = c.chipAtIndex(lim);
			 	 if(worker.color==color) { return true; }
			 }}
	 return false;
 }
 
 private boolean addWorkerMoves(CommonMoveStack all,PendulumCell from[],boolean retrieve,int who)
 {	boolean some = false;
	 for(PendulumCell c : from)
	 {
		 some |= addWorkerMoves(all,c,retrieve,who);
		 if(some && all==null) { return some; }
	 }
	 return some;
 }
 private boolean addWorkerMoves(CommonMoveStack all,PendulumCell from,boolean retrieve,int who)
 {	 PlayerBoard pb = pbs[who];
 	 PColor color = pb.color;
 	 boolean some = false;
 	 if(!retrieve && robot!=null && from.lastMoved==flipCount) 
 	 	{// the intent of this is to prevent the robot shuffling workers from one action
 		 // space to another.
 		 return false; 
 	 	}
 	 PendulumChip picked = pb.pickedObject;
 	 if((picked!=null) && ((picked.id==PendulumId.GrandeWorker)||(picked.id==PendulumId.RegularWorker)))
 	 {  // this code is encountered when dropping the retrieve worker card
 		// and at that point the pickedobject is not relevant
 		if(pb.pickedSource==from)
 	 	{
 		boolean grande = picked==pb.grande;
 		addWorkerMoves(all,from,pb.pickedObject,grande,who);
 		if(grande)
 			{
 			some |= addWorkerMoves(all,from,pb.pickedObject,pb.grandes,true,who);
 			}
 			else
 			{
 			some |= addWorkerMoves(all,from,pb.pickedObject,pb.meeples,true,who);
 			}
 	 	}
 	 }
 	 else if((picked==null) || ((picked.id==PendulumId.PlayerStratCard)&&all==null))
 	 {for(int lim=from.height()-1; lim>=0 && !(all==null && some); lim--)
	 {	 PendulumChip worker = from.chipAtIndex(lim);
		 if(worker.color==color)
		 {
			 some |= addWorkerMoves(all,from,worker,worker==pb.grande,who);
		 }
	 }}
 	 return some;
 }
 private void addGrandeMoves(CommonMoveStack all,int who)
 {
	 PlayerBoard pb = pbs[who];
	 PendulumChip top =  (pb.pickedSource==pb.grandes && pb.pickedObject!=null)
			 		? pb.pickedObject
			 		: pb.grandes.topChip();
	 if(top!=null) { addWorkerMoves(all,pb.grandes,top,true,who); }
 }
 
 private void addMeepleMoves(CommonMoveStack all,int who)
 {
	 PlayerBoard pb = pbs[who];
	 PendulumChip top = (pb.pickedSource==pb.meeples && pb.pickedObject!=null)
			 		? pb.pickedObject 
			 		: pb.meeples.topChip();
	 if(top!=null) {  addWorkerMoves(all,pb.meeples,top,false,who); }
 }
 private boolean addWorkerMoves(CommonMoveStack all,PendulumCell from,PendulumChip chip,boolean any,int who)
 {
	 PendulumCell[] greenRow = greenHourglassTop() ? greenMeepleB : greenMeepleA;
	 PendulumCell[] blackRow = blackHourglassTop() ? blackMeepleB : blackMeepleA;
	 PendulumCell[] purpleRow = purpleHourglassTop() ? purpleMeepleB : purpleMeepleA;
	 boolean some = addWorkerMoves(all,from,chip,greenRow,any,who);
	 if(all==null && some) { return some; }
	 some |= addWorkerMoves(all,from,chip,blackRow,true,who);
	 if(all==null && some) { return some; }
	 some |= addWorkerMoves(all,from,chip,purpleRow,any,who);
	 return some;
	 
 }
 private boolean addWorkerMoves(CommonMoveStack all,PendulumCell from,PendulumChip chip,PendulumCell to[],boolean any,int who)
 {	boolean some = false;
	for(PendulumCell dest : to) 
	{	if(from!=dest)
		{
		some |= addWorkerMoves(all,from,chip,dest,any,who);
		if(all==null && some) { return some; }
		}
	}
	return some;
 }
 private boolean addWorkerMoves(CommonMoveStack all,PendulumCell from,PendulumChip chip,PendulumCell to,boolean any,int who)
 {
	if((to.rackLocation()!=PendulumId.Unused) && (any || to.topChip()==null))
	{
		if(all!=null) { all.push(new PendulumMovespec(MOVE_FROM_TO,from,chip,to,who)); }
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
			 if(c.height()>0 && (c.rackLocation()!=PendulumId.Unused)) { all.push(new PendulumMovespec(MOVE_PICK,c,c.topChip(),who));}
		 }
	 }
	 else {
		 for(PendulumCell c = cells; c!=null; c=c.next) 
		 {	if(c.rackLocation()!=PendulumId.Unused)
		 	{
			 all.push(new PendulumMovespec(MOVE_DROP,c,pb.pickedObject,who));
		 	}
		 }
	 }
 }
 
	public void addCouncilRewardMoves(CommonMoveStack all, int who) 
	{	PlayerBoard pb = pbs[who];
		// skip the first element, which is the unused deck
		for(int i=1;i<councilCards.length; i++)
		{	PendulumCell card = councilCards[i];
			PendulumChip ch = card.topChip();
			if(ch!=null && ch.pb!=null)	// benefit is null for filler card backs
			{
			PC cost =ch.pc;
			if(ch.councilCardToHand || pb.canPayCost(cost))
			{	
				all.push(new PendulumMovespec(MOVE_SELECT,card,ch,pb.boardIndex));
			}
			}
		}
	}
	public void addPromoteMeepleMoves(CommonMoveStack all,int who)
	{
		addPromoteMeepleMoves(all,greenMeepleA,who);
		addPromoteMeepleMoves(all,greenMeepleB,who);
		addPromoteMeepleMoves(all,blackMeepleA,who);
		addPromoteMeepleMoves(all,blackMeepleB,who);
		addPromoteMeepleMoves(all,purpleMeepleA,who);
		addPromoteMeepleMoves(all,purpleMeepleB,who);
		addPromoteMeepleMoves(all,greenActionA,who);
		addPromoteMeepleMoves(all,greenActionB,who);
		addPromoteMeepleMoves(all,blackActionA,who);
		addPromoteMeepleMoves(all,blackActionB,who);
		addPromoteMeepleMoves(all,purpleActionA,who);
		addPromoteMeepleMoves(all,purpleActionB,who);
		pbs[who].addPromoteMeepleMoves(all);
	}
	public void addPromoteMeepleMoves(CommonMoveStack all,PendulumCell row[],int who)
	{	PlayerBoard pb = pbs[who];
		PendulumChip target = pb.meeple;
		for(PendulumCell c : row)
		{
			if(c.containsChip(target))
			{
				all.push(new PendulumMovespec(MOVE_SELECT,c,target,who));
			}
		}
	}

	public void addPendingPlayMoves(CommonMoveStack all,int who)
	{
		all.push(new PendulumMovespec(MOVE_READY,who));
	}

 CommonMoveStack  GetListOfMoves(int who)
 {	CommonMoveStack all = new CommonMoveStack();
 	getListOfMoves(all,who);
 	return all;
 }
 private void getListOfMoves(CommonMoveStack all,int who)
 {
	 if(resetState!=null && robot==null)
	 {
		 switch(resetState)
		 {
		 case CouncilRewards:
			 // allow reselects
			 getListOfMoves(all,resetState,who); 
			 break;
		 default:			 
			 break;
		 }
	 }
	 getListOfMoves(all,board_state,who);
	 if(robot!=null)
	 {
		// G.print("All "+all.size());
		 if((all.size()==0) && (board_state!=PendulumState.Gameover)) 
			{ throw new Error("No moves, but not game over"); 
			}
	 }
	 if(robot!=null && all.size()==0)
	 {
		 p1("No moves "+moveNumber+" "+board_state);
	 }
	 
 }
 public void addRefillProvinceMoves(CommonMoveStack all,int who)
 {	PlayerBoard pb = pbs[who];
 	if((provinceCards.height()>0) 
 		&& (pb.pickedObject==null 
 			|| pb.pickedSource==provinceCards))
 	{
	 	for(PendulumCell c : provinces)
	 	{
		 if(c.isEmpty()) {
			 all.push(new PendulumMovespec(MOVE_FROM_TO,provinceCards,provinceCards.topChip(),c,who));
		 }
	 	}
	 }
 }
 private void getListOfMoves(CommonMoveStack all,PendulumState state,int who)
 {
 	switch(state)
 	{
 	case Puzzle:
 		{
 		addPuzzleMoves(all,allCells,who);
 		for(PlayerBoard pb : pbs) { addPuzzleMoves(all,pb.allCells,who); }
 		}
 		break;
 	case Flip:
 		all.push(new PendulumMovespec(MOVE_AUTOFLIP,who));
 		break;
 	case StartPlay:
 		all.push(new PendulumMovespec(MOVE_STARTPLAY,who));
 		break;
 	case StartCouncil:
 		all.push(new PendulumMovespec(MOVE_STARTCOUNCIL,who));
 		break;
 	case Confirm:
 	case Resign:
 		all.push(new PendulumMovespec(MOVE_DONE,who));
 		break;
 	case PlayGrande:
 		addGrandeMoves(all,who);
 		break;
 	case PlayMeeple:
 		addMeepleMoves(all,who);
 		break;
 
 	case PendingPlay:
 		addPendingPlayMoves(all,who);
 		break;
 	case CouncilPlay:
 	case Play:
 		{
 		PlayerBoard pb = pbs[who];
		if(robot==null) { addRefillProvinceMoves(all,who); }
		
		switch(pb.uiState)
		{
		default: break;
		
		case Province:
			if(!hasProvincesAvailable()
					&& (pb.pickedObject==null || pb.pickedObject.id==PendulumId.ProvinceCard))
				{
				pb.setUIStateNormal();
				}
			break;
		case RetrieveWorker:
			if(!hasRetrieveWorkerMoves(who))
			{	// the opportunity to retrieve has gone away, probably because of a timer flip
				pb.setUIStateNormal();
			}
		}
		if((pb.uiState==UIState.Province)
				&& !hasProvincesAvailable()
				)
		{	// crash out of province state if there are none available.
			pb.setUIStateNormal();
		}
		switch(pb.uiState)
 		{
 		case Normal:
 			{
	 		addWorkerMoves(all,who);	// move a worker instead of taking the action
	 		addGrandeMoves(all,who);	// place a grande from the reserve
	 		addMeepleMoves(all,who);	// place a regular worker from the reserve
	 		boolean hasAction = addActionMoves(all,who);	// take the action a worker is elgible for
	 		pb.addPlayStrategem(all);	// play a strategem card
	 		if(variation.timers && state!=PendulumState.CouncilPlay) { addFlipTimerMoves(all,who);	}// flip timers
	 		addClaimAchievementMoves(all,who);	// claim the achievement card
	 		// don't consider resting if an action is available.  There may be occasional
	 		// cases where this is the wrong thing to do, but it's more important to get
	 		// the benefits that are available.
	 		if(!hasAction && !variation.timers) { all.push(new PendulumMovespec(MOVE_REST,who)); }
	 		if(state==PendulumState.CouncilPlay)
	 			{
	 			all.push(new PendulumMovespec(MOVE_REST,who));
	 			}
 			}
	 		break;
 		case Ready:
 			all.push(new PendulumMovespec(MOVE_READY,who));
 			break;
		case Rest:
 			all.push(new PendulumMovespec(MOVE_SETACTIVE,nextPlayer(who,false)));
 			break;
 		case RetrieveWorker:
 			addRetrieveWorkerMoves(all,who);
 			break;
 		case CollectResources:
 			pbs[who].addCollectResourceMoves(all);
 			break;
 		case Province:
 			if(robot!=null)
 			{
 				addRefillProvinceMoves(all,who);
 				// refill the provinces before choosing one
 				if(all.size()>0) { break; }
 			}
 			pbs[who].addCollectProvinceMoves(all);
 			break;
 		case AchievementOrLegandary:
 			all.push(new PendulumMovespec(MOVE_STANDARD_ACHIEVEMENT,who));
 			all.push(new PendulumMovespec(MOVE_LEGENDARY_ACHIEVEMENT,who));
 			break;
 		case PayResources:
 			pb.addPayResourceMoves(all);
 			break;
 		case SwapVotes:
 			pb.addSwapVoteMoves(all);
 			break;
 		default:
 			p1("uistate-"+pb.uiState);
 			throw G.Error("Not expecting uiState %s",pb.uiState);
 		}}
 		break;
 	case CouncilRewards:
 		{
 		PlayerBoard pb = pbs[who];
 		if(robot==null) { addRefillProvinceMoves(all,who); }
 		if(robot!=null && pb.selectedCell!=null) 
			{
			p1("allread selected "+pb.uiState);
			G.Error("already selected %s",pb.uiState); 
			}
 		switch(pb.uiState)
 		{
 		case PromoteMeeple:
 			addPromoteMeepleMoves(all,who);
 			break;
 		case Normal: 
 			addCouncilRewardMoves(all,who);
 			break;
 		case ProvinceReward:
 		case Province:
 			if(robot!=null)
 			{
 				addRefillProvinceMoves(all,who);
 				// refill the provinces before choosing one
 				if(all.size()>0) { break; }
 			}
 			pb.addCollectProvinceMoves(all);
 			break;
 		case SwapVotes:
 			pb.addSwapVoteMoves(all);
 			break;
 		case P1P1P1:
 		case P1P1P1Once:
 		case P1P1P1Twice:
 			pb.addVPChoiceMoves(all,1);
 			break;
 		case P2P2P2:
 			pb.addVPChoiceMoves(all,2);
 			break;
 		default:
 			p1("councilreward-"+pb.uiState);
 			G.Error("Not expecting ui state %s",pb.uiState);
 		}}
 		break;
 	case CouncilTrim:
 		{
 		if(robot==null) { addRefillProvinceMoves(all,who); }
 		PlayerBoard pb = pbs[who];
 		pb.addCouncilTrimMoves(all);
 		}
 		break;
 	case Gameover:
 		break;
 	default:
 			p1("getmoves-"+state);
 			G.Error("Not expecting state ",state);
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
 /**
  * this uses the same logic as canUnpick, but when prevention has failed
  * @param pb
  * @param m
  * @param c
  * @param chip
  * @return
  */
 public boolean verifyUnpickOk(PlayerBoard pb,PendulumMovespec m,PendulumCell c,PendulumChip chip)
 {	 if(!canUnpick(pb,c,chip))
 		{
	 	PendulumMovespec dropper = c.dropper;
	 	if(dropper!=null && dropper.realTime>0)	// zero is for game records without realtime in moves
	 	{	long dif = dropper.realTime-m.realTime;
	 		if(Math.abs(dif)<1000)	// looks legit
	 		{
	 		PlayerBoard other = getPlayerBoard(dropper.forPlayer);
	 		int otherPriv = getPlayerPrivilege(other.color);
	 		int myPriv = getPlayerPrivilege(pb.color);
	 		if(myPriv<otherPriv)
	 		{
	 			// we win, make the other guy undo
	 			privilegeResolutions++;
				logGameEvent(Privilege.Override.description);
				pb.pickObject(c,dropper.chip);
				return true;
	 		}
	 		else
	 		{	// we lose, do nothing
	 			privilegeResolutions++;
				logGameEvent(Privilege.Ignore.description);
				return false;
	 		}
	 		}
	 	}
 		}
	 return true;
 }
 /**
  * this attempts to prevent illegal undos using the user interface.
  * it can't actually work due to realtime considerations, but in
  * combination with privilege resolution it can be very close to enough. 
  * @param pb
  * @param c
  * @param chip
  * @return
  */
 public boolean canUnpick(PlayerBoard pb,PendulumCell c,PendulumChip chip)
 {
	 switch(c.rackLocation())
	 {
	 case ProvinceCardStack:
		 // If two players pick a card and both balk in the other order, they can in effect reorder 
		 // the deck, and different players may see the final deck differently.
		 int h = c.height();
		 return (pb.pickedIndex==h);
	 case Province:
		 return c.isEmpty();
	 case GreenMeepleA:
	 case GreenMeepleB:
	 case PurpleMeepleA:
	 case PurpleMeepleB:
		 // if someone else has slipped in a worker, for sure 2 workers aren't allowed.
		 if(chip.id==PendulumId.RegularWorker)
		 {
			for(int lim=c.height()-1; lim>=0; lim--)
			{
				PendulumChip other = c.chipAtIndex(lim);
				if(other.id==PendulumId.RegularWorker){
					return false;
				}
			}
		 }
		 return true;
		 
	 default: return true;
	 }
 }
 public void getUIMoves(CommonMoveStack all,int player)
 {	 	
 	PlayerBoard pb = pbs[player];
 	if((pb.pickedObject!=null) && (pb.pickedSource!=null))
 		{
 		// special consideration for real time play.  Something may have changed that
 		// makes undo illegal.  So the un-pick move may no longer be available
 		if(canUnpick(pb,pb.pickedSource,pb.pickedObject))
 			{
 			all.push(new  PendulumMovespec(MOVE_DROP,pb.pickedSource,pb.pickedObject,player));
 			}
 		}
 	if((pb.droppedObject!=null) && (pb.droppedDest!=null))
		{
		all.push(new PendulumMovespec(MOVE_PICK,pb.droppedDest,pb.droppedObject,player));
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
 		case MOVE_SELECT:
 			targets.put(getCell(m.source,m.from_col,m.from_row),m);
 			break;
 		case MOVE_DROP:
 		case MOVE_FLIP:
  			targets.put(getCell(m.dest,m.to_col,m.to_row),m);
 			break;
 		case MOVE_REFILL:
 			{
 			PlayerBoard pb = pbs[m.forPlayer];
 			targets.put(pb.refill,m);
 			}
 			break;
		case MOVE_SWAPVOTES:
 			{
 			PendulumCell from = getCell(m.source,m.from_col,m.from_row);
 			PlayerBoard pb = pbs[m.forPlayer];
 			m.op = MOVE_SELECT;
 			if(pb.selectedCell!=null) { targets.put(from,m); }
 			}
			//$FALL-THROUGH$
		case MOVE_FROM_TO:
 			PlayerBoard pb = pbs[m.forPlayer];
 			boolean usedest = false;
 			boolean changeOp = true;
 			switch(pb.uiState)
 			{
 			case CollectResources:
 				usedest = true;
				//$FALL-THROUGH$
			case PayResources:
 				m.op = MOVE_SELECT;
 				changeOp = false;	// already changed
				//$FALL-THROUGH$
			default:
			{
 			if(!usedest && (pb.pickedObject==null) && pb.selectedCell==null)
 				{	if(changeOp) { m.op = MOVE_PICK; }
 					targets.put(getCell(m.source,m.from_col,m.from_row),m);
 				}
 				else
 				{	if(changeOp) { m.op = MOVE_DROP; }
 					targets.put(getCell(m.dest,m.to_col,m.to_row),m);
 				}}}
 			break;
 		case MOVE_SETACTIVE: 
 		case MOVE_REST:
 		case MOVE_STARTPLAY:
 		case MOVE_READY:
 		case MOVE_AUTOFLIP:
 		case MOVE_STARTCOUNCIL:
 		case MOVE_STANDARD_ACHIEVEMENT:
 		case MOVE_LEGENDARY_ACHIEVEMENT:
 		case MOVE_WAIT:
 			break;
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
	provinces[0].setLocation(0,0,0.12, 0.52, 0.12);
	provinces[1].setLocation(0,0,0.12, 0.64, 0.12);
	provinces[2].setLocation(0,0,0.12, 0.76, 0.12);
	provinces[3].setLocation(0,0,0.12, 0.88, 0.12);
	provinceCards.setLocation(0,0,0.27, 0.64, 0.12);
	achievementCards.setLocation(0,0,0.541, 0.172, 0.117);
	currentAchievement.setLocation(0,0,0.541, 0.354, 0.117);
	// hourglasses
	greenTimers[0].setLocation(0,0,0.95, 0.15, 0.11);
	greenTimers[1].setLocation(0,0,0.95, 0.38, 0.11);
	blackTimers[0].setLocation(0,0,0.95, 0.6, 0.11);
	blackTimers[1].setLocation(0,0,0.95, 0.84, 0.11);
	purpleTimers[0].setLocation(0,0,0.045, 0.125, 0.11);
	purpleTimers[1].setLocation(0,0,0.045, 0.18, 0.11);
	purpleTimers[2].setLocation(0,0,0.045, 0.36, 0.11);
	purpleTimers[3].setLocation(0,0,0.045, 0.41, 0.11);
	trash.setLocation(0,0,0.11, 0.25, 0.05);
	if(players_in_game>=4)
		{
			PendulumCell.setHLocation(0.95,0.5,purpleMeepleA,0.125,0.42, 0.09, 0.1);
			PendulumCell.setHLocation(0.95,0.5,purpleMeepleB,0.125,0.42, 0.325, 0.1);
			PendulumCell.setHLocation(0.95,0.5,greenMeepleA,0.68,0.97, 0.09, 0.1);
			PendulumCell.setHLocation(0.95,0.5,greenMeepleB,0.68,0.97, 0.325, 0.1);
		}
		else
		{
			PendulumCell.setHLocation(0.95,0.5,purpleMeepleA,0.17,0.624, 0.092, 0.1);
			PendulumCell.setHLocation(0.95,0.5,purpleMeepleB,0.12,0.56, 0.325, 0.1);
			PendulumCell.setHLocation(0.95,0.5,greenMeepleA,0.73,1.165, 0.092, 0.1);
			PendulumCell.setHLocation(0.95,0.5,greenMeepleB,0.68,1.12, 0.325, 0.1);	
			purpleMeepleA[0].boxXscale *=2;
			greenMeepleA[0].boxXscale *=2;
			purpleMeepleB[1].boxXscale *=2;
			greenMeepleB[1].boxXscale *=2;
		}
	PendulumCell.setHLocation(0.95,0.5,purpleActionA,0.125,0.42, 0.19, 0.1);
	PendulumCell.setHLocation(0.95,0.5,purpleActionB,0.125,0.42, 0.425, 0.1);

	PendulumCell.setHLocation(0.95,0.5,greenActionA,0.68,0.97, 0.19, 0.1);
	PendulumCell.setHLocation(0.95,0.5,greenActionB,0.68,0.97, 0.425, 0.1);

	PendulumCell.setHLocation(0.95,0.5,blackMeepleA,0.545,0.94, 0.55, 0.1);
	PendulumCell.setHLocation(0.95,0.5,blackActionA,0.545,0.94, 0.65, 0.1);
	PendulumCell.setHLocation(0.95,0.5,blackMeepleB,0.545,0.94, 0.78, 0.1);
	PendulumCell.setHLocation(0.95,0.5,blackActionB,0.545,0.94, 0.875, 0.1);

	PendulumCell.setVLocation(privilege,0.41,0.11,0.35,0.1);
	PendulumCell.setVLocation(achievement,0.47,0.31,0.44,0.08);
	PendulumCell.setVLocation(timerTrack,0.28,0.21,0.84,0.45);
	restButton.setLocation(0,0,0.435, 0.7, 0.06);
	PendulumCell.setCouncilLocation(councilCards,0.22,0.795,0.29,0.7,0.2);
	councilRewardsUsed.setLocation(0,0,0.21, 0.795, 0.04);

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
public boolean isSelected(PendulumCell c)
{
	for(PlayerBoard pb : pbs) { if(pb.isSelected(c)) { return true; }}
	return false;
}

public void revertPartialMoves()
{	for(PlayerBoard pb : pbs)
	{	pb.revertPartialMoves();
	}
}
/*
 * 
 true order ABC    as received by A  ABC  as received by B BAC  as received by C  CAB
 with privilege, the resolution should match privilege in all cases
 */
//
// when a pick can't find the object to be picked, probably there is a conflict
// and it can be resolved in privilege order
//
public Privilege resolvePrivilege(PlayerBoard forPlayer, PendulumChip item) 
{	int myOrder = getPlayerPrivilege(forPlayer.color);
	privilegeResolutions++;
	for(PlayerBoard pb : pbs)
	{
		if(pb!=forPlayer)
		{
			if((pb.pickedObject==item)||(pb.droppedObject==item))
			{
				int hisOrder = getPlayerPrivilege(pb.color);
				if(myOrder>hisOrder)
				{
					pb.alwaysUnpickObject();
					logGameEvent(Privilege.Override.description);
					return Privilege.Override;
				}
				else 
				{ 	logGameEvent(Privilege.Ignore.description);
					return Privilege.Ignore; 
				}
			}
		}
	}
	logGameEvent(Privilege.Error.description);
	return Privilege.Error;
}
//
// when we're about to drop on the board, verify that it's ok given
// the placement rules.  Only a few cases are possible.
// 
public boolean verifyDropOk(PlayerBoard forPlayer,PendulumMovespec m,PendulumCell c, PendulumChip pickedObject) 
{
	switch(c.rackLocation())
	{
	case Province:
		// province card must be empty.  If not, two players are trying to fill the empty slot at the same time
		// since the slots hold only a single card, assume the second player won't try to fill a slot he sees
		// as filled, so there's a limited window.
		if(!c.isEmpty())
		{ 
			// find the other player who dropped there
			for(PlayerBoard pb : pbs)
			{
				if(pb.droppedDest==c && pb!=forPlayer)
				{
					int hisPriv = getPlayerPrivilege(pb.color);
					int myPriv = getPlayerPrivilege(forPlayer.color);
					if(hisPriv<myPriv)
					{	// we lose, ignore this drop
						privilegeResolutions++;
						logGameEvent(Privilege.Ignore.description);
						return false;
					}
					else
					{	// we win, make him undo
						privilegeResolutions++;
						logGameEvent(Privilege.Override.description);
						pb.unDropObject();
					}
				}
			}
		}
		return true;

	case GreenMeepleA:
	case GreenMeepleB:
	case PurpleMeepleA:
	case PurpleMeepleB:
		if(hasMeepleConflict(c,pickedObject,m.realTime))
		{
			// conflict to be resolved.  Either the dropped object is a regular worker
			// or the cell already contains a regular worker.
			int myPrivilege = getPlayerPrivilege(forPlayer.color);
			int highestOther = -1;
			int highestWorker = -1;
			for(int lim=c.height()-1; lim>=0; lim--)
			{
				PendulumChip chip = c.chipAtIndex(lim);
				PlayerBoard pb = getPlayerBoard(chip);
				if(pb!=null)
				{
				int hisPriv = getPlayerPrivilege(pb.color);
				if(highestOther<0 || highestOther>hisPriv) { highestOther = hisPriv; }
				if((chip.id==PendulumId.RegularWorker) 
					&& (highestWorker<0 || highestWorker>hisPriv)) { highestWorker = hisPriv; }
				}
			}
			if(highestWorker>=0) { highestOther = highestWorker; }
			if(myPrivilege<highestOther)
			{	// we win, evict any previous regular worker, then drop whatever
				for(int lim = c.height()-1; lim>=0; lim--)
				{
					PendulumChip chip = c.chipAtIndex(lim);
					if(chip.id==PendulumId.RegularWorker)
					{
						PlayerBoard pb = getPlayerBoard(chip);
						privilegeResolutions++;
						logGameEvent(Privilege.Override.description);
						// this had better be the current dropped object, or way too much is happening
						G.Assert(chip==pb.droppedObject,"should be dropped");
						pb.unDropObject();
					}
				}
			}
			else if(pickedObject.id==PendulumId.RegularWorker)
			{	// we lose, some other guy with higher privilege landed at the same time
				privilegeResolutions++;
				logGameEvent(Privilege.Ignore.description);
				return false;
			}
		}
		return true;
	default: return true;
	}
}


private boolean hasMeepleConflict(PendulumCell c,PendulumChip chip,long now)
{	int nchips = c.height();
	if(now==0) { return false; } 	// no realtime info
	boolean resolve = nchips>0 && chip.id==PendulumId.RegularWorker;
	for(int lim=nchips-1; lim>=0 && !resolve; lim--)
	{
		PendulumChip ch = c.chipAtIndex(lim);
		resolve |= ch.id==PendulumId.RegularWorker;
	}
	if(resolve)
	{
		long dif = Math.abs(now-c.dropWorkerTime);
		if(dif>1000) { resolve=false; }
	}
	c.dropWorkerTime = now; 
	return resolve;
}

}
