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
package bugs;


import static bugs.BugsMovespec.*;

import bridge.Color;

import java.util.*;
import bugs.BugsChip.Terrain;
import bugs.data.Profile;
import bugs.data.Taxonomy;
import lib.*;
import lib.Random;
import online.game.*;

/**
 * BugsBoard knows all about the game of BugSpiel, which is played
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
 * TODO: make more marine predators with multiple habitats
 *
 */

public class BugsBoard 
	extends hexBoard<BugsCell>	// for a square grid board, this could be rectBoard or squareBoard 
	implements BoardProtocol,BugsConstants
{	static int REVISION = 103;			// 100 represents the initial version of the game
										// 101 fixes the scoring of herbivores to not include other prey species
										// 102 fixes the unstable initialization of the bug market
										// 103 restricts the number of predators to predator_percentage
	public int getMaxRevisionLevel() { return(REVISION); }
	static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	BugsVariation variation = BugsVariation.bugspiel_parallel;
	BugsState board_state = BugsState.Puzzle;	
	private StateStack robotState = new StateStack();

	private BugsCell activeDeckCache = null;
	private long activeDeckCacheKey = -1;
	
	public BugsCell activeDeck = null;
	public int minDeckValue = 6;
	public int cachedMindeckValue = 6;
	private BugsCell goalDeckCache = null;
	private long goalDeckCacheKey = -1;
	
	public BugsCell goalDeck = null;
	public BugsCell goalDiscards = null;
	public BugsCell bugDiscards = null;
	private BugsCell terrainCache = null;
	private long terrainCacheKey = -1;
	
	public int roundNumber = 0;
	int roundProgress = 0;
	boolean rotated = false;
	int progress = 0;
	public boolean lackOfProgress = false;
	public PlayerBoard pbs[] = null;
	public PlayerBoard allPbs[] = 
		{ new PlayerBoard(this,0),
		  new PlayerBoard(this,1),
		  new PlayerBoard(this,2),
		  new PlayerBoard(this,3)
		};
	
	public PlayerBoard getPlayerBoard(int n) { return pbs[n]; }
	public PlayerBoard getCurrentPlayerBoard() { return pbs[whoseTurn]; }
	public BugsState getState() { return(board_state); }
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(BugsState st) 
	{ 	
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public BugsCell bugMarket[] = null;
	public BugsCell goalMarket[] = null;
	// get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public BugsChip getPlayerChip(int p) { return(pbs[p].chip); }
	public BugsChip getCurrentPlayerChip() { return(getPlayerChip(whoseTurn)); }
	public BugsPlay robot = null;
	
	
	/* this can replace "G.Assert" in the this file, so if the assertion
	 * fails in a search, the state is recorded automatically.
	 */
	 public boolean p1(boolean condition,String msg,Object... args)
	 {	if(!condition)
	 	{
		 p1(G.concat(msg,args));
		 G.Error(msg,args);
	 	}
	 	return condition;
	 }
	 
	/**
	 * save the current state of the search as a file in the /robot/ directory.  This
	 * requires cooperation with the way the robot teats moves, and some behaviours
	 * tend to cause problems.  In partular "auto-done" after robot moves assumes
	 * that any time the current player changes, there was an implicit "done".
	 * The robot's saveCurrentVariation method may need to be customized. 
	 * @param msg
	 * @return
	 */
	public boolean p1(String msg)
		{
			if(G.debug() && G.p1(msg) && robot!=null)
			{	String dir = "g:/share/projects/boardspace-html/htdocs/bugs/bugsgames/robot/";
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
	public void SetDrawState() { G.Error("Not expected"); }
	CellStack animationStack = new CellStack();
     
    // save strings to be shown in the game log
    StringStack gameEvents = new StringStack();
    InternationalStrings s = G.getTranslations();
    
 	void logGameEvent(String str,String... args)
 	{	//if(!robotBoard)
 		{String trans = s.get(str,args);
 		 gameEvents.push(trans);
 		}
 	}

    private BugsState resetState = BugsState.Puzzle; 
    public DrawableImage<?> lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public BugsCell newcell(char c,int r)
	{	return(new BugsCell(this,BugsId.BoardLocation,c,r));
	}
	
	// constructor 
    public BugsBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = GRIDSTYLE;
        setColorMap(map, players);
        
		Random r = new Random(734687);
		activeDeckCache = new BugsCell(this,r,BugsId.MasterDeck);
		activeDeck =  new BugsCell(this,r,BugsId.ActiveDeck);
		goalDeckCache = new BugsCell(this,r,BugsId.MasterGoalDeck);
		goalDeck = new BugsCell(this,r,BugsId.GoalDeck);
		goalDiscards = new BugsCell(this,r,BugsId.GoalDiscards);
		bugDiscards = new BugsCell(this,r,BugsId.BugDiscards);
		terrainCache = new BugsCell(this,r,BugsId.Terrain);
	
		// do this once at construction
	    bugMarket = new BugsCell[N_MARKETS];
	    goalMarket = new BugsCell[N_GOALS];
	    for(int i=0;i<N_MARKETS;i++)
	    {
	    	bugMarket[i] = new BugsCell(this,r,BugsId.BugMarket,i);
	    	bugMarket[i].cost = COSTS[i];
	    }
	    for(int i=0;i<N_GOALS;i++)
	    {
	    	goalMarket[i] = new BugsCell(this,r,BugsId.GoalMarket,i);
	    	goalMarket[i].cost = COSTS[i];
	    }

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
    
    public int nBoardCells() { return getCellArray().length; }
    
    
    public void buildActiveDeck()
    {	long key = randomKey+154135;
    	if(activeDeckCacheKey!=key)
    	{
		Hashtable <String,Taxonomy> cats = new Hashtable<String,Taxonomy>();
		Random r = new Random(key);
		int ncards = BugCard.bugCount();
		int map[] = AR.intArray(ncards);
		int nCells = nBoardCells();
		int deckSize = (int)(DECKSIZE_MULTIPLIER*nCells*3);
		Taxonomy animals = Taxonomy.getWildType();
		int lim = ncards;
		r.shuffle(map);
		activeDeck.reInit();
		int min = 9999;
		@SuppressWarnings("unused")
		int carnivore = 0;
		while((cats.size()<N_ACTIVE_CATEGORIES
				|| activeDeck.height()<deckSize)
				&& lim-->0
				)
		{	
			BugCard ch = BugCard.getCard(map[lim]);
			Profile prof = ch.getProfile();
			Taxonomy cat = prof.getCategory();
			String catName = cat.getScientificName();
			boolean include = cat!=animals && cats.get(catName)==null;
			if(revision>=103)
			{
				int tempCarnivore = 0;
				int tempCard = 0;
				for(int i=0;i<lim; i++)
				{
					BugCard in = BugCard.getCard(i);
					Profile profile = in.getProfile();
					Taxonomy bugCat = profile.getCategory();
					if(bugCat==cat)
					{
						tempCard++;
						if(profile.isPredator()) { tempCarnivore++; }
					}
				}
				if(tempCard==0 || (double)(tempCarnivore+carnivore)/(tempCard+activeDeck.height()) > PREDATOR_PERCENTAGE)
				{	G.print("reject "+cat+" too many predators");
					include =false;
				}
			}
			if(include)
			{	
				for(int i=0;i<lim; i++)
				{
					BugCard in = BugCard.getCard(i);
					Profile profile = in.getProfile();
					Taxonomy bugCat = profile.getCategory();
					if(bugCat==cat)
						{ activeDeck.addChip(in); 
						  min = Math.min(in.pointValue(),min);
						  if(profile.isPredator()) { carnivore++; }
						}				
				}
				cats.put(catName,cat);

			}
			minDeckValue = cachedMindeckValue = min;
		}
		int nWild = (int)(deckSize*WILDPERCENT);
		lim = ncards;
		while(nWild > 0 && lim-->0)
		{
			BugCard ch =  BugCard.getCard(map[lim]);
			Taxonomy cat = ch.getProfile().getCategory();
			if(cat==animals) 
				{ nWild--; activeDeck.addChip(ch); 
				  if(ch.getProfile().isPredator()) { carnivore++; }
				}
		}
		activeDeck.shuffle(new Random(randomKey+2356366));
		//G.print("Build new "+activeDeck.Digest(new Random(1245)));
		activeDeckCacheKey = key;
		activeDeckCache.copyFrom(activeDeck);
    	}
    	else
    	{
    		activeDeck.copyFrom(activeDeckCache);
    		minDeckValue = cachedMindeckValue;
    		//G.print("Reuse new "+activeDeck.Digest(new Random(1245)));
    	}
		//G.print(cats.size()," categories"," ",activeDeck.height()," cards ",(int)(carnivore*100)/activeDeck.height(),"% carnivore");
		
    }
    private void buildGoalDeck()
    {	long key = randomKey+23525426;
    	if(goalDeckCacheKey!=key)
    	{
		GoalCard.buildGoalDeck(key,this,activeDeck.toArray(),goalDeck);
		Random r = new Random(key+14125);
		goalDeck.shuffle(r);
		goalDeckCache.copyFrom(goalDeck);
		goalDeckCacheKey = key;
    	}
    	else
    	{	goalDeck.copyFrom(goalDeckCache);
    	}
    }
    
	
	public TerrainSummary[] getTerrainSummary(BugsCell deck)
	{
		TerrainSummary v[] = TerrainSummary.makeSummary();
		for(int lim = deck.height()-1; lim>=0; lim--)
		{
			BugCard bug = (BugCard)deck.chipAtIndex(lim);
			Profile prof = bug.getProfile();
			if(prof.hasWaterHabitat()) { v[Terrain.Water.ordinal()].score(bug); }
			if(prof.hasForestHabitat()) { v[Terrain.Forest.ordinal()].score(bug); }
			if(prof.hasGroundHabitat()) { v[Terrain.Soil.ordinal()].score(bug); }
			if(prof.hasGrassHabitat()) { v[Terrain.Grass.ordinal()].score(bug); }
		}
		return v;
	}
    
    /**
     * assign habitats to the board cells, in a proportion that's appropriate
     * for the bugs that are in the deck.
     */	
    public void buildHabitat()
    {	long key = randomKey+23653264;
    	if(terrainCacheKey!=key)
    	{
    	int onlyWater = 0;
    	int onlyForest = 0;
    	int onlyGrass = 0;
    	int onlyGround = 0;
    	int hasGround = 0;
    	int hasWater = 0;
    	int hasGrass = 0;
    	int hasForest = 0;
    	// characterize the requirements of the deck
    	for(int lim=activeDeck.height()-1; lim>=0; lim--)
    	{
    		BugCard ch = (BugCard)activeDeck.chipAtIndex(lim);
    		Profile pr = ch.getProfile();
    		if(pr.hasOnlyWater()) { onlyWater=1; }
    		if(pr.hasOnlyForest()) { onlyForest=1; }
    		if(pr.hasOnlyGround()) { onlyGround=1; }
    		if(pr.hasOnlyGrass()) { onlyGrass=1; }
    		if(pr.hasGroundHabitat()) { hasGround++; }
    		if(pr.hasWaterHabitat()) { hasWater++; }
    		if(pr.hasForestHabitat()) { hasForest++; }
    		if(pr.hasGrassHabitat()) { hasGrass++; }
 	   	}
    	int nCells = nBoardCells();
    	double hTotal = hasGround + hasWater + hasForest + hasGrass;
    	int nWaterCells = Math.max(onlyWater,(int)(0.5+hasWater / hTotal * nCells));
       	int nForestCells = Math.max(onlyForest,(int)(0.5+hasForest / hTotal * nCells));
       	int nGrassCells = Math.max(onlyGrass,(int)(0.5+hasGrass / hTotal * nCells));
       	int nGroundCells = Math.max(onlyGround,(int)(0.5+hasGround / hTotal * nCells));
       	int allocCells = nWaterCells + nForestCells + nGrassCells + nGroundCells;
       	{
   		Random r = new Random(24644+key);
      	while(allocCells<nCells)
       		{
       		switch(r.nextInt(4))
       		{
       		default:
       		case 0: nWaterCells++; break;
       		case 1: nGroundCells++; break;
       		case 2: nForestCells++; break;
       		case 3: nGrassCells++; break;
       		}
       		allocCells++;
       	}}
      	{
   		Random r = new Random(236726+key);
      	while(allocCells>nCells)
       	{
       		switch(r.nextInt(4))
       		{
       		default:
       		case 0: if(nWaterCells>onlyWater) {  nWaterCells--; allocCells--; } break;
       		case 1: if(nGroundCells>onlyGround) { nGroundCells--; allocCells--; } break;
       		case 2: if(nForestCells>onlyForest) { nForestCells--; allocCells--; } break;
       		case 3: if(nGrassCells>onlyGrass) { nGrassCells--; allocCells--; } break;
       		}
       	}}
       	// we now have determined the right number of cells of appropriate types
       	BugsCell cells = terrainCache;
       	cells.reInit();
       	while(nWaterCells-- > 0) { cells.addChip(BugsChip.MarshTile); }
       	while(nForestCells-- > 0) { cells.addChip(BugsChip.ForestTile); }
       	while(nGroundCells-- > 0) { cells.addChip(BugsChip.GroundTile); }
       	while(nGrassCells-- > 0) { cells.addChip(BugsChip.PrarieTile); }
       	cells.shuffle(new Random(7337+key));
       	terrainCacheKey = key;
    	}
    	int i=0;
    	for(BugsCell cells = allCells; cells!=null; cells = cells.next,i++)
    	{
    		cells.background = terrainCache.chipAtIndex(i);
    	}
     }
    
    public BugCard random1PointBug(Random r)
    {	BugCard ch = null;
    	int index = 0;
    	
    	int loops= 1;
    	do {
    		index = r.nextInt(activeDeck.height());
    		ch = (BugCard)activeDeck.chipAtIndex(index);
    		if(loops++ % 10==0) { minDeckValue++; }
    	} while (ch.pointValue()>minDeckValue);
    	activeDeck.removeChipAtIndex(index);
    	if(revision<102) 
    		{ // backward compatibility
    		  cachedMindeckValue = minDeckValue; 
    		}
    	return ch;
    	
    }
    public void selectInitialBugs(int[]map)
    {
    	Random r = new Random(randomKey+34646);
	    for(int i=0;i<players_in_game; i++) 
	    	{ PlayerBoard pb = pbs[i] = allPbs[i];
	    	  pb.setChip(bugChips[map[i]],bugColors[map[i]]);
	    	  pb.doInit(); 
	    	  // give a random goal and a random 1 point bug
	    	  pb.goals.addChip(goalDeck.removeTop());
	    	  pb.bugs.addChip(random1PointBug(r));
	    	}
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int players,int rev)
    {	randomKey = key;
    	adjustRevision(rev);
    	players_in_game = players;
    	win = new boolean[players];
 		setState(BugsState.Puzzle);
		variation = BugsVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		gametype = gtype;
		rotated = false;
		roundNumber = 1;
		roundProgress = progress = 0;
		lackOfProgress = false;
		targetsCache = null;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case bugspiel_sequential:
		case bugspiel_sequential_large:
		case bugspiel_parallel:
		case bugspiel_parallel_large:
			// using reInitBoard avoids thrashing the creation of cells 
			// when reviewing games.
			reInitBoard(variation.firstInCol,variation.ZinCol,null);
			// or initBoard(variation.firstInCol,variation.ZinCol,null);
			// Random r = new Random(734687);	// this random is used to assign hash values to cells, common to all games of this type.
			// allCells.setDigestChain(r);		// set the randomv for all cells on the board
		}
		
		buildActiveDeck();
		buildHabitat();
		// the goal deck is built based on the active deck
		buildGoalDeck();
		reInit(bugMarket);
		reInit(goalMarket);
		switch(variation)
		{
		case bugspiel_parallel:
		case bugspiel_parallel_large:
	 		for(int i=0;i<bugMarket.length;i++)
	 		{
	 			bugMarket[i].addChip(activeDeck.removeTop());
	 		}
	 		break;
		case bugspiel_sequential:
		case bugspiel_sequential_large:
			if(revision<102)
			{	Random r = new Random(randomKey+4666646);
				for(int i=0;i<bugMarket.length;)
		 		{	int idx = r.nextInt(activeDeck.height());
		 			BugCard ch = (BugCard)activeDeck.chipAtIndex(idx);
		 			if(ch.pointValue()-minDeckValue <= COSTS[i])
		 			{	// no "gimmies" in the startup bugMarket
		 				bugMarket[i].addChip(activeDeck.removeChipAtIndex(idx));
		 				i++;
		 			}
		 		}
			}
			else
			{	Random r = new Random(randomKey+4666646);
				for(int i=0;i<bugMarket.length;i++)
				{	// no "gimmies" in the startup bugMarket
					BugCard ch = random1PointBug(r);
					bugMarket[i].addChip(ch);
				}
			}
	 		break;
	 	default: G.Error("Not expecing variation %s",variation);
		}
 		for(int i=0;i<goalMarket.length;i++)
 		{
 			goalMarket[i].addChip(goalDeck.removeTop());
 		}
	    goalDiscards.reInit();
	    bugDiscards.reInit();
	    whoseTurn = FIRST_PLAYER_INDEX;
	    resetState = null;
	    lastDroppedObject = null;
	    int map[]=getColorMap();
	    pbs = new PlayerBoard[players_in_game];
	    
	    selectInitialBugs(map);
	    
        animationStack.clear();
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }
    public void animate(replayMode replay,BugsCell from,BugsCell to)
    {
    	if(replay.animate) { animationStack.push(from); animationStack.push(to); }
    }
    /** create a copy of this board */
    public BugsBoard cloneBoard() 
	{ BugsBoard dup = new BugsBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}

    public void copyFrom(BoardProtocol b) { copyFrom((BugsBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(BugsBoard from_b)
    {
        super.copyFrom(from_b);
        robotState.copyFrom(from_b.robotState);
        copyFrom(bugMarket,from_b.bugMarket);
        copyFrom(goalMarket,from_b.goalMarket);
        copyFrom(activeDeck,from_b.activeDeck);
        copyFrom(activeDeckCache,from_b.activeDeckCache);
        activeDeckCacheKey = from_b.activeDeckCacheKey;
        minDeckValue = from_b.minDeckValue;
        cachedMindeckValue = from_b.cachedMindeckValue;
        copyFrom(goalDeck,from_b.goalDeck);
        copyFrom(goalDeckCache,from_b.goalDeckCache);
        goalDeckCacheKey = from_b.goalDeckCacheKey;
        copyFrom(terrainCache,from_b.terrainCache);
        terrainCacheKey = from_b.terrainCacheKey;
        copyFrom(goalDiscards,from_b.goalDiscards);
        copyFrom(bugDiscards,from_b.bugDiscards);
        
        board_state = from_b.board_state;
        resetState = from_b.resetState;
        rotated = from_b.rotated;
        roundNumber = from_b.roundNumber;
        roundProgress = from_b.roundProgress;
        progress = from_b.progress;
        lackOfProgress = from_b.lackOfProgress;
        for(int i=0;i<pbs.length;i++) { pbs[i].copyFrom(from_b.pbs[i]); }
        targetsCache = null;
        if(G.debug()) { sameboard(from_b); }
    }

    

    public void sameboard(BoardProtocol f) { sameboard((BugsBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(BugsBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(activeDeck.sameContents(from_b.activeDeck),"active deck mismatch");
        G.Assert(goalDiscards.sameContents(from_b.goalDiscards),"goalDiscards deck mismatch");
        G.Assert(bugDiscards.sameContents(from_b.bugDiscards),"bugDiscards deck mismatch");
        G.Assert(sameCells(bugMarket,from_b.bugMarket),"bugMarket mismatch");
        G.Assert(rotated==from_b.rotated,"rotated mismatch");
        G.Assert(progress==from_b.progress,"progress mismatch");
        G.Assert(lackOfProgress==from_b.lackOfProgress,"lackOfProgress mismatch");
        G.Assert(roundNumber==from_b.roundNumber,"roundNumber mismatch");
        G.Assert(roundProgress==from_b.roundProgress,"roundProgress mismatch");
        G.Assert(sameCells(goalMarket,from_b.goalMarket),"goalMarket mismatch");
        for(int i=0;i<pbs.length;i++) { pbs[i].sameBoard(from_b.pbs[i]); }
        
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
		v ^= activeDeckCache.Digest(r);
		v ^= activeDeck.Digest(r);
		v ^= Digest(r,activeDeckCacheKey);
		v ^= Digest(r,minDeckValue);
		v ^= Digest(r,cachedMindeckValue);
		v ^= goalDeck.Digest(r);
		v ^= goalDeckCache.Digest(r);
		v ^= Digest(r,goalDeckCacheKey);
		v ^= goalDiscards.Digest(r);
		v ^= bugDiscards.Digest(r);
		v ^= terrainCache.Digest(r);
		v ^= Digest(r,terrainCacheKey);
		v ^= Digest(r,bugMarket);
		v ^= Digest(r,goalMarket);
		v ^= Digest(r,revision);
		v ^= Digest(r,board_state);
		v ^= Digest(r,rotated);
		v ^= Digest(r,progress);
		v ^= Digest(r,lackOfProgress);
		v ^= Digest(r,roundNumber);
		v ^= Digest(r,roundProgress);
		v ^= Digest(r,whoseTurn);
		for(int i=0;i<pbs.length;i++)
		{
		v ^= pbs[i].Digest(r); }
        return (v);
    }

    public void setWhoseTurn(int w)
    {	super.setWhoseTurn(w);
    	pbs[w].setOurTurn();
    	
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
        case SequentialPlay:
        case Play:
        case Purchase:
        case Bonus:
        	moveNumber++; //the move is complete in these states
            setWhoseTurn(nextPlayer(whoseTurn));
            return;
        }
    }
    public void setNextActivePlayer(replayMode replay)
    {
    	do 
		{ setNextPlayer(replay);
		}
		while(isReady(whoseTurn));
    }

    /** this is used to determine if the "Done" button in the UI is live
     *
     * @return
     */
    public boolean DoneState()
    {
    	return DoneState(whoseTurn);
    }
    public boolean DoneState(int who)
    {	return(pbs[who].doneState());
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

    public double robotScore(int player)
    {	PlayerBoard pb = pbs[player];
    /*
    	if(pb.score>=WinningScore)
    	{
    		return Math.min(1.0,0.9 + (0.1*Math.max(0,pb.score+200))/(WinningScore+300));
    	}
    	return Math.min(0.9,(0.9*Math.max(0,pb.score+200))/(WinningScore+200));
    	*/
    	double win = winningScore();
    	return (Math.min(win,Math.max(0,pb.getScore()))/win);
    }
    public void setGameOver()
    {
    	setState(BugsState.Gameover);
    	PlayerBoard winner = null;
    	for(PlayerBoard pb : pbs)
    	{	
    		if(winner==null || pb.getScore()>winner.getScore()) { winner = pb; }
    	}
    	win[winner.boardIndex] = true;
    }
    public int scoreForPlayer(int pl)
    {
    	return pbs[pl].getScore();
    }
    // set the contents of a cell, and maintain the books
    public BugsChip SetBoard(BugsCell c,BugsChip ch)
    {	BugsChip old = c.topChip();
    	if(old!=null) { c.removeTop();}
       	if(ch!=null) { c.addChip(ch);  }
    	return(old);
    }
    public int activeMoveNumber()
    {
    	return super.activeMoveNumber();
    }
 
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { return movingObjectIndex(whoseTurn);
    }
    public int movingObjectIndex(BugsChip ch)
    {
    	if(ch!=null)
    	{	return(ch.chipNumber()); 
    	}
      	return (NothingMoving);
    }
	public int movingObjectIndex(int i) {
		return movingObjectIndex(pbs[i].pickedObject);
	}

   /**
     * get the cell represented by a source code, and col,row
     * @param source
     * @param col
     * @param row
     * @return
     */
    BugsCell getCell(BugsId source,char col,int row)
    {
    	BugsCell c = getCellInternal(source,col,row);
    	G.Assert(c.owningBoard==this,"should be mine");
    	return c;
    }
    public BugsCell getCell(char col,int row)
    {
    	BugsCell c = super.getCell(col,row%100);
    	if(row>=200) { return c.below; }
    	if(row>=100) { return c.above; }
    	return c;
    }
    BugsCell getCellInternal(BugsId source, char col, int row)
    {
        switch (source)
        {
        default:
        	return pbs[col-'A'].getCell(source,col,row);
        case GoalMarket:
        	if(col=='@')
        	{
        	return goalMarket[row];
        	}
        	else
        	{
       		return pbs[col-'A'].getCell(source,col,row);
        	}
        case GoalDiscards:
        	return goalDiscards;
        case BugDiscards:
        	return bugDiscards;
        case ActiveDeck:
        	return activeDeck;
        case GoalDeck:
        	return goalDeck;
        case BugMarket:
        	if(col=='@')
        		{
        		return bugMarket[row];
        		}
        	else
        		{
        		return pbs[col-'A'].getCell(source,col,row);
        		}
        case BoardTopLocation:
        case BoardBottomLocation:
        case BoardLocation:
        {	int rem = 0;
        	if(row>=100)
        		{  
        		rem = row/100; 
        		row = row%100; 
        		}
        	BugsCell c = getCell(col,row);
        	switch(rem)
        	{
        	default:
        	case 0: break;
        	case 1: c = c.above; break;
        	case 2: c = c.below; break;
        	}
        	return c;
        	}
        } 	
    }
    /**
     * this is called when copying boards, to get the cell on the new (ie current) board
     * that corresponds to a cell on the old board.
     */
    public BugsCell getCell(BugsCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }

    public int maxScore()
    {
    	PlayerBoard m = null;
    	for(PlayerBoard pb : pbs) { if(m==null || pb.getScore() > m.getScore()) { m = pb; }}
    	return m.getScore();
    }
    private void setNextStateAfterDone(replayMode replay)
    {	
    	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state "+board_state);
     	case Gameover: break;
     	case Purchase: break;	// when resigning this happens
    	case Bonus:
    		setState(BugsState.Bonus);
    		break;
    	case SequentialPlay:
    		setNextActivePlayer(replay);	
    		break;
    	case Play:
     		{	
     		if(allReady())
     		{
     			for(PlayerBoard pb : pbs) { pb.setUIState(UIState.Normal); }
    			lackOfProgress = progress==roundProgress;
    			setState(BugsState.Bonus);
     		}
     		else
     		{
     		setNextActivePlayer(replay);	
     		}}
    		break;
    	
    	case Puzzle:
    		switch(variation)
    		{
    		case bugspiel_parallel:
    		case bugspiel_parallel_large:
    			setState(BugsState.Purchase);
    			break;
    		case bugspiel_sequential:
    		case bugspiel_sequential_large:
    			setState(BugsState.SequentialPlay);
    			break;
    		default: G.Error("Not expecting variation %s",variation); 
    		}
    		break;
    	}
       	resetState = board_state;
    }
    private void doDone(replayMode replay)
    {	
    	switch(board_state)
    	{
    	case SequentialPlay:
      		break;
    	case Play:
        	{
        	PlayerBoard pb = pbs[whoseTurn];
 			if(pb.pickedSource.rackLocation()!=BugsId.BoardLocation)
 				{
 				progress++;
 				pbs[whoseTurn].progress = true;
 				}
        	}
        	break;
    	default: break;
    	}
    	rotated = false;
    	migrate(bugMarket,activeDeck,bugDiscards,replay);
		migrate(goalMarket,goalDeck,goalDiscards,replay);	
	   	getPlayerBoard(whoseTurn).doDone(replay);
	   	if(allPassed() || lastRound())
    	{
    		setGameOver();
    	}
    	else
    	{	
    		setNextStateAfterDone(replay);
    	}
 
    }
    public boolean allPassed()
    {
		for(PlayerBoard pb : pbs)
		{
			if(pb.uiState!=UIState.Pass) { return false; }
		}
		return true;
    }
    public boolean somePassed()
    {
		for(PlayerBoard pb : pbs)
		{
			if(pb.uiState==UIState.Pass) { return true; }
		}
		return false;
    }
	public boolean allReady()
	{
		for(PlayerBoard pb : pbs)
		{
			if(pb.uiState!=UIState.Ready) { return false; }
		}
		return true;
	}
	public boolean isReady(int p)
	{
		return pbs[p].uiState==UIState.Ready;
	}
	public void doPurchases(replayMode replay)
	{	for(BugsCell c : bugMarket ) { c.purchased = false; }
		for(BugsCell c : goalMarket) { c.purchased = false; }
		for(PlayerBoard p : pbs)
			{ p.doPurchases(replay);
			}
		migrate(bugMarket,activeDeck,bugDiscards,replay);
		migrate(goalMarket,goalDeck,goalDiscards,replay);	
		PlayerBoard newpb[] = new PlayerBoard[pbs.length];
		AR.copy(newpb,pbs);
		Sort.sort(newpb,false);	// order by score
		for(int i=0;i<newpb.length;i++) { newpb[i].turnOrder = i; }
		setFirstPlayer();
	}
	public void setFirstPlayer()
	{
		whoseTurn = findPlayer(0).boardIndex;
	}
	public PlayerBoard findPlayer(int n)
	{
		for(PlayerBoard pb : pbs) { if(pb.turnOrder==n) { return pb; }}
		throw G.Error("cant find player %s",n);
	}
	public int nextPlayer(int who)
	{
		int thisPlayer = pbs[who].turnOrder;
		PlayerBoard pb = findPlayer((thisPlayer+1)%players_in_game);
		if(pb.resigned) { return nextPlayer(pb.boardIndex); }
		return pb.boardIndex;
	}
	public int playersLeft()
	{
		int n=0;
		for(PlayerBoard pb : pbs) { if(!pb.resigned) { n++; }}
		return n;
	}
	public boolean someEmpty(BugsCell cells[])
	{
		for(BugsCell c : cells) { if(c.topChip()==null) { return true; }}
		return false;
	}
	/*
	 * migrate a row down and fill the top with new cards from the replacement deck.
	 */
	public void migrate(BugsCell group[],BugsCell from,BugsCell discards,replayMode replay)
	{	
		switch(variation)
		{
		case bugspiel_parallel:
		case bugspiel_parallel_large:
			BugsCell discard = group[group.length-1];
			discard.purchased = true;		// always discard the cheapest 
			break;
		case bugspiel_sequential:
		case bugspiel_sequential_large:
			break;
		default: throw G.Error("not expecting variation %s",variation);
		}

		for(BugsCell c : group ) { if(c.purchased) { c.reInit(); }}		// clear the purchased cells
		boolean someEmpty = someEmpty(group);
		if(someEmpty)
		{
		int fi = group.length-2;
		int ti = group.length-1;
		if(DISCARD_LOW)
		{	BugsCell f = group[ti];
			if(f.topChip()!=null)
			{
				discards.addChip(f.removeTop());
				animate(replay,f,discards);
			}
		}
		while(ti>=0)
		{	BugsCell tcell = group[ti];
			tcell.purchased = false;
			if(tcell.height()==0) 
			{ 	BugsChip ch = null;
				// shuffle down from a more expensive card
				while(fi>=0 && ch==null) 
				{ BugsCell fcell = group[fi];
				  if(fcell.height()>0) 
					{
					  ch = fcell.removeTop();
					  animate(replay,fcell,tcell);					  
					}
				  else { fi--; }
				}
				// if no more in the rack, take from the source
				if(ch==null && from.height()>0)
				{
					tcell.addChip(from.removeTop());
					animate(replay,from,tcell);
				}
				if(ch!=null) { tcell.addChip(ch); }
			}
			ti--;
			fi--;
		}}
	}
	public void findPickedObject(PlayerBoard pb)
	{
		if(board_state==BugsState.Puzzle)
		{
			// in free mode, trying to move pieces between players
			for(PlayerBoard p : pbs)
			{
				if(p!=pb && p.pickedObject!=null)
				{
					pb.pickedObject = p.pickedObject;
					pb.pickedSource = p.pickedSource;
					p.pickedObject = null;
					p.pickedSource = null;
				}
			}
		}
	}
	public int winningScore()
	{
		if(revision<101) { return variation.WinningScore; }
		else { return SmallWinningScore; }
	}
	public boolean lastRound()
	{
		return goalMarket[0].topChip()==null 
					|| bugMarket[0].topChip()==null 
					|| lackOfProgress
					|| maxScore()>=winningScore()
					|| roundNumber >= MaxRounds;
	}
    public boolean Execute(commonMove mm,replayMode replay)
    {	BugsMovespec m = (BugsMovespec)mm;
        if(replay.animate) { animationStack.clear(); }
        targetsCache = null;
        //G.print("E "+m+" for "+whoseTurn+" "+state);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone(replay);

            break;
            
        case MOVE_SETACTIVE:
        	setWhoseTurn(m.forPlayer);
        	break;


        case MOVE_ROTATECCW:
        	{	
        	BugsCell src = getCell(m.source,m.to_col,m.to_row);
        	src.rotation++;
        	rotated=true;
        	if(src.rotation>1) { src.rotation = -1; }
        	}
        	break;
        case MOVE_ROTATECW:
        	{
        	BugsCell src = getCell(m.source,m.to_col,m.to_row);
        	src.rotation--;
        	rotated = true;
        	if(src.rotation<-1) { src.rotation = 1; }
        	}
        	break;
        case MOVE_TO_PLAYER:
        case MOVE_TO_BOARD:
        case MOVE_RESIGN:
        case MOVE_DROPB:
        case MOVE_PASS:
        case MOVE_DROP:
        	// come here only where there's something to pick, which must
 			{
	 		PlayerBoard pb = getPlayerBoard(m.forPlayer);
	 		pb.Execute(m,replay);
	 		}
 			break;
 			
        case MOVE_PICK:
 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
	 		PlayerBoard pb = getPlayerBoard(m.forPlayer);
	 		pb.Execute(m,replay);
	 		}
            break;

 		case MOVE_START:
            setWhoseTurn(m.player);
            for(PlayerBoard pb : pbs) { pb.acceptPlacement(); }
            int nextp = nextPlayer[whoseTurn];
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(BugsState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ setState(BugsState.Gameover); 
               	}
            else {  setNextStateAfterDone(replay); }

            break;

       case MOVE_EDIT:
           for(PlayerBoard pb : pbs) { pb.acceptPlacement(); }
           setState(BugsState.Puzzle);
           break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(BugsState.Gameover);
    	   break;

       case MOVE_READY:    	
  			{
  				PlayerBoard pb = getPlayerBoard(m.forPlayer);
  				pb.Execute(m,replay);
  				switch(board_state)
  				{
  				default: 
  					throw G.Error("Not expecting state %s",board_state);
  				case Bonus:
  					if(allReady())
  					{
  		    			lackOfProgress = roundProgress==progress;
						for(PlayerBoard p : pbs) { p.setUIState(UIState.Normal); }
  		    			if(lastRound())
  		    			{
  		    				// ran out of bugs or goalMarket
  		    				setGameOver();
  		    			}
  		    			else
  		    			{
  		    			roundNumber++;
  						setState(BugsState.Purchase);
  		    			}
  					}
  					else
  					{
  						setNextActivePlayer(replay);
  					}
  					break;
  				case Purchase:
	  				if(allReady())
	  				{
	  				
	  				doPurchases(replay);
	  				for(PlayerBoard p : pbs) { p.setUIState(UIState.Normal); }
	      			roundProgress = progress;
	  				setState(BugsState.Play);
	  				}
	  				else
	  				{
	  					setNextActivePlayer(replay);
	  				}
	  				break;
  				case Play:
  					if(allReady())
  					{
  						doDone(replay);
  					}
  					else { 
  						setNextActivePlayer(replay);
  					}
  					break;
  				}
  			}	
  			break;
       case MOVE_SELECT:
       		{
    	   PlayerBoard pb = getPlayerBoard(m.forPlayer);
    	   pb.Execute(m,replay);
       		}	
       		break;
       		
        default:
        	cantExecute(m);
        }
        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }
        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }

   
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(BugsMovespec m)
    {
        //G.print("R "+m);
        Execute(m,replayMode.Replay);
        if(DoneState(whoseTurn))
        {
        	doDone(replayMode.Replay);
        }
       
    }
 

 void addPickDrop(CommonMoveStack all,BugsCell c,BugsChip pickedObject,int who)
 {	if(c.onBoard)
 	{
	 addPickDrop(all,c.above,pickedObject,who);
	 addPickDrop(all,c.below,pickedObject,who);
 	}
	 if(pickedObject==null)
	 {
		 if(c.topChip()!=null)
		 {	
			 all.push(new BugsMovespec(c.onBoard ? MOVE_PICKB : MOVE_PICK,c,who));
		 }
	 }
	 else
	 {
		 all.push(new BugsMovespec(c.rackLocation()==BugsId.BoardLocation ? MOVE_DROPB : MOVE_DROP,c,who));
	 }
 }
 public boolean robotWouldPlay(BugsCell c,boolean selected)
 {
	 if(robot!=null)
	 {
		 if(selected) { return false; }
		 // select if playable
	 }
	 return true;
 }
 public void addSelectMoves(CommonMoveStack all,int who)
 {	PlayerBoard pb = pbs[who];
 	boolean someAlreadySelected = false;
 	boolean some = false;
 	for(BugsCell c : goalMarket)
	 {	// robot never deselects
 		boolean selected = pb.isSelected(c);
 		if(c.height()>0 && (robot==null || !selected)) 
		 	{ all.push(new BugsMovespec(MOVE_SELECT,c,who)); 
		 	}
	 }
 	for(BugsCell c : bugMarket)
	 {	// robot never deselects
 		boolean selected = pb.isSelected(c);
 		someAlreadySelected |= selected;
		if(c.height()>0 && robotWouldPlay(c,selected))
		 	{ all.push(new BugsMovespec(MOVE_SELECT,c,who)); 
		 	  some = true;
		 	}
	 }
	 if(someAlreadySelected || !some) { all.push(new BugsMovespec(MOVE_READY,who)); }
 }
 public boolean robotShuffle(BugsCell from,BugsCell to,BugsChip target)
 {
	 if(robot==null || target!=null || from.rackLocation()!=BugsId.BoardLocation) { return false; }
	 return true;
 }
 public boolean robotCannible(BugsCell c,BugsChip card,BugsChip chip)
 {
	 if(card==null || robot==null) { return false; }
	 //if(c.player==chip) { return true; }
	 return false;
 }
// carnivores can eat herbivores but not parasites or other carnivores
public boolean canPlaceOverChip(BugCard card,BugsChip onCard)
{
	if(onCard==null) { return !card.profile.isPredator(); }	// carnivores have to eat!
	if(card.profile.isPredator() && onCard.isBugCard())
	{
		if(((BugCard)onCard).profile.isPrey()) { return true; }
	}
	return false;
}
public boolean addMovesOnBoard(CommonMoveStack all,int op, BugsCell from, BugCard chip,PlayerBoard pb)
{
		boolean some = false;
		for(BugsCell c = allCells; c!=null; c=c.next)
		{	some |= addMovesOnBoard(all,op,from,chip,c,pb);
			if(some && all==null) { return true; }
		}
		return some;
}
 public boolean addMovesOnBoard(CommonMoveStack all,int op, BugsCell from, BugCard chip,BugsCell c,PlayerBoard pb)
 {	boolean some = false;
     //if(c.rackLocation()==BugsId.PlayerBugs) { return true; }
     int who = pb.boardIndex;
     BugsChip pchip = pb.chip;
	 if(c!=from 
			 && c.canPlaceInHabitat(chip))
		{	BugsChip mainChip = c.topChip();
			if(canPlaceOverChip(chip,mainChip)
					 && !robotShuffle(from,c,mainChip)
					 && !robotCannible(c,chip,pchip))
				{ if(all==null) { return true; }
				  some = true;
				  all.push(new BugsMovespec(op,from,chip,c,who)); 
				}
			BugsCell above = c.above;
			BugsChip aboveChip = above.topChip();
			if(canPlaceOverChip(chip,aboveChip) 
					&& !robotShuffle(from,c,aboveChip)
					&& !robotCannible(above,chip,pchip)) 
				{ if(all==null) { return true; }
				  all.push(new BugsMovespec(op,from,chip,above,who)); 
				  some = true;
				}
			BugsCell below = c.below;
			BugsChip belowChip = below.topChip();
			if(canPlaceOverChip(chip,belowChip) 
					&& !robotShuffle(from,c,belowChip)
					&& !robotCannible(below,chip,pchip)) 
				{ if(all==null) { return true; }
				  all.push(new BugsMovespec(op,from,chip,below,who));
				  some = true;
				}
		} 
	 return some;
 }
 public void addMovesInBoard(CommonMoveStack all,PlayerBoard pb)
 {		for(BugsCell c = allCells; c!=null; c=c.next)
 		{
	 	addMovesInBoard(all,c,pb);
	 	addMovesInBoard(all,c.above,pb);
	 	addMovesInBoard(all,c.below,pb);
 		}
 }
 public void addMovesInBoard(CommonMoveStack all,BugsCell c,PlayerBoard pb)
 {	BugsChip top = c.topChip();
 	if(top!=null 
 			&& c.player == pb.chip
 			&& c.placedInRound!=roundNumber
 			&& top!=null
 			&& top.isBugCard()
 			&& top.getProfile().isFlying()
 			&& c.height()==1
			)
	 		{
 			BugCard bug = (BugCard)top;
 			addMovesOnBoard(all,robot==null 
 					? (pb.pickedObject==null ? MOVE_PICKB : MOVE_DROPB) 
 					: MOVE_TO_BOARD,
 					c,bug,pb);
	 		}
 }
 
 CommonMoveStack  getListOfMoves(int who)
 {	CommonMoveStack all = new CommonMoveStack();
 	switch(board_state)
 	{
 	case Puzzle:
 		{	
 			PlayerBoard pb = pbs[who];
 			for(BugsCell c = allCells;
 			 	    c!=null;
 			 	    c = c.next)
 			 	{	addPickDrop(all,c,pb.pickedObject,who);
 			 	}
  		for(BugsCell c : bugMarket) { addPickDrop(all,c,pb.pickedObject,who); }
 		for(BugsCell c : goalMarket) { addPickDrop(all,c,pb.pickedObject,who); }
 		addPickDrop(all,activeDeck,pb.pickedObject,who);
 		addPickDrop(all,goalDeck,pb.pickedObject,who);
 		for(PlayerBoard p : pbs)
 			{
 			p.getListOfMoves(all,pb.pickedObject);
 			}
 		}
 		break;
 	case Bonus:
 	case SequentialPlay:
 	case Play:
		pbs[who].getListOfMoves(all,pbs[who].pickedObject);
 		break;

	case Purchase:
 		addSelectMoves(all, who);
 		break;
	case Gameover:
		break;
 	default:
 			G.Error("Not expecting state ",board_state);
 	}
 	return(all);
 }
 
 public void initRobotValues(BugsPlay m)
 {	robot = m;

 }

 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(Character.isDigit(txt.charAt(0)))
	 	{ switch(variation)
	 		{
	 		case bugspiel_sequential_large:
	 		case bugspiel_parallel_large:
	 		case bugspiel_sequential:
		 	case bugspiel_parallel:
		 		xpos -= cellsize/5;
		 		ypos -= cellsize/5;
		 		break;
 			default: G.Error("case "+variation+" not handled");
	 		}		  
 		}
 	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
 }
 public Hashtable<BugsCell, BugsMovespec> targetsCache = null;
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
 public Hashtable<BugsCell, BugsMovespec> getTargets(int who) 
 {	if(targetsCache==null)
 	{
 	Hashtable<BugsCell,BugsMovespec> targets = new Hashtable<BugsCell,BugsMovespec>();
 	CommonMoveStack all = getListOfMoves(who);
 	PlayerBoard pb = pbs[who];
 	if(board_state==BugsState.Puzzle || simultaneousTurnsAllowed())
 		{
 		for(PlayerBoard p : pbs) { p.getUIMoves(all); }
 		}
 		else {
 		pb.getUIMoves(all);
 		}
 	for(int lim=all.size()-1; lim>=0; lim--)
 	{	BugsMovespec m = (BugsMovespec)all.elementAt(lim);
 		switch(m.op)
 		{
  		case MOVE_PICKB:
  			targets.put(getCell(m.from_col,m.from_row),m);
  			break;
 		case MOVE_PICK:
 			targets.put(getCell(m.source,m.from_col,m.from_row),m);
  			break;
 		case MOVE_DROPB:
 			targets.put(getCell(m.to_col,m.to_row),m);
 			break;
 		case MOVE_DROP:
 		case MOVE_SELECT:
 			targets.put(getCell(m.source,m.to_col,m.to_row),m);
 			break;
 		case MOVE_SWAP:
 		case MOVE_PASS:
 		case MOVE_READY:
 		case MOVE_DONE:
 			break;

 		default: G.Error("Not expecting "+m);
 		
 		}
 		
 	}
 	targetsCache = targets;
 	//G.print("copy "+targets.size()+" ",this);
 	}
 	Hashtable<BugsCell,BugsMovespec> targets = new Hashtable<BugsCell,BugsMovespec>();
 	for(Enumeration<BugsCell>e = targetsCache.keys(); e.hasMoreElements();)
 	{
 		BugsCell c = e.nextElement();
 		BugsMovespec m = targetsCache.get(c);
 		targets.put(c,(BugsMovespec)m.Copy(null));
 	}
 	return(targets);
 }
 

 //public boolean drawIsPossible() { return false; }
 // public boolean canOfferDraw() {
 //	 return false;
	 /**
	something like this:
 	return (movingObjectIndex()<0)
 			&& ((board_state==BugsState.Play) || (board_state==BugsState.DrawPending))
 			&& (moveNumber-lastDrawMove>4);
	 * @param pb 
 			*/
 //}

public boolean isSelected(PlayerBoard pb, BugsCell c) {
	switch(board_state)
	{
	case Purchase:
		return pb.isSelected(c);
	default: return false;
	}
}

public int nonWildBonusPointsForPlayer(GoalCard card,BugsCell cell,BugsChip chip)
{
	int tot = 0;
	if(cell.player==chip)
	{	int h = cell.height();
		while(h-->0)
		{
		// score only the top bug
		BugsChip ch = cell.chipAtIndex(h);
		if(ch.isBugCard()) { 
		BugCard bug = (BugCard)ch;
		if(card.cat1.matches(this,bug,false)) { tot += card.cat1.pointValue(this,bug);  }
		if(card.cat2.matches(this,bug,false)) { tot += card.cat2.pointValue(this,bug);  }
		h = -1;
		}
		}
	}
	return tot;
}

private int sweep_counter = 0;

public double bonusPointsForPlayer(Goal cat,BugsCell cell,BugsChip chip,boolean includeWild)
{
	double tot = 0;
	if(cell.player==chip)
	{	int h = cell.height();
		while(h-->0)
		{
		// score only the top bug
		BugsChip ch = cell.chipAtIndex(h);
		if(ch.isBugCard()) { 
			BugCard bug = (BugCard)ch;
			if(cat.matches(this,bug,includeWild)) { tot += cat.pointValue(this,bug); }
			h = -1;
		}
	}
	}
	return tot;
}

public double scoreBonusForPlayer(Goal cat,BugsCell cell,PlayerBoard p,boolean includeWild)
{	double tot = 0;
	G.Assert(cell.onBoard,"must be a board cell");
	sweep_counter++;
	if(!p.resigned)
	{
	BugsChip chip = p.chip;
	sweep_counter++;
	if(cell!=null && cell.sweep_counter!=sweep_counter)
	{	cell.sweep_counter = sweep_counter;
		tot =  bonusPointsForPlayer(cat,cell,chip,includeWild);
		tot += bonusPointsForPlayer(cat,cell.above,chip,includeWild); 
		tot += bonusPointsForPlayer(cat,cell.below,chip,includeWild);
		{
		BugsCell next = cell;
		while(((next = next.exitTo(cell.rotation))!=null) 
				&& (next.rotation==cell.rotation)
				&& next.isOccupied())
			{
			tot += bonusPointsForPlayer(cat,next,chip,includeWild);
			tot += bonusPointsForPlayer(cat,next.above,chip,includeWild); 
			tot += bonusPointsForPlayer(cat,next.below,chip,includeWild);
			}}
		{
		BugsCell next = cell;
		while(((next = next.exitTo(cell.rotation+CELL_HALF_TURN))!=null)
				&& next.rotation==cell.rotation
				&& next.isOccupied())
		{
		tot += bonusPointsForPlayer(cat,next,chip,includeWild);
		tot += bonusPointsForPlayer(cat,next.above,chip,includeWild); 
		tot += bonusPointsForPlayer(cat,next.below,chip,includeWild);
		}}
	}}
	return tot;
}

public double scoreBonusForPlayer(GoalCard card,BugsCell cell,PlayerBoard p)
{	
	G.Assert(cell.onBoard,"must be a board cell");
	if(!p.resigned)
	{
	// score without bonus, if it scores then rescore with bonus
	double tot1 = scoreBonusForPlayer(card.cat1,cell,p,false);
	 if(tot1>0) { tot1 = scoreBonusForPlayer(card.cat1,cell,p,true); }
	double tot2 = scoreBonusForPlayer(card.cat2,cell,p,false);
	 if(tot2>0) { tot2 = scoreBonusForPlayer(card.cat2,cell,p,true);}
	 
	return tot1+tot2;
	}
	return 0;

}
public void scoreBonusForPlayers(PlayerBoard pb,BugsCell cell,replayMode replay)
{
	GoalCard card = (GoalCard)cell.removeTop();
	goalDiscards.addChip(card);
	animate(replay,cell,goalDiscards);
	/*
	 * 
	 */
	BugsCell parentCell = getCell(cell.col,cell.row%100);	// get the parent cell on the board
	if(!ADVERSARIAL_BONUS)
	{
	int score = (int)scoreBonusForPlayer(card,parentCell,pb);
	pb.changeScore(score); 
	}
	else
	{
	for(PlayerBoard p : pbs)
	{	
		int score = (int)scoreBonusForPlayer(card,parentCell,p);
		p.changeScore(score);
	}}
}

}
