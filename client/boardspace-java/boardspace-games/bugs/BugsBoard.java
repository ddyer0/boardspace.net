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

import java.awt.Color;
import java.util.*;

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
 */

class BugsBoard 
	extends hexBoard<BugsCell>	// for a square grid board, this could be rectBoard or squareBoard 
	implements BoardProtocol,BugsConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
	static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	BugsVariation variation = BugsVariation.bugspiel;
	BugsState board_state = BugsState.Puzzle;	
	private BugsState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	public BugsCell masterDeck = null;
	public BugsCell activeDeck = null;
	public BugsCell masterGoalDeck = null;
	public BugsCell goalDeck = null;
	
	public PlayerBoard pbs[] = null;
	public PlayerBoard allPbs[] = 
		{ new PlayerBoard(this,BugsId.Yellow,BugsChip.Yellow,0),
		  new PlayerBoard(this,BugsId.Green,BugsChip.Green,1),
		  new PlayerBoard(this,BugsId.Blue,BugsChip.Blue,2),
		  new PlayerBoard(this,BugsId.Red,BugsChip.Red,3)
		};
	
	public PlayerBoard getPlayerBoard(int n) { return pbs[n]; }
	
	public BugsState getState() { return(board_state); }
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(BugsState st) 
	{ 	unresign = (st==BugsState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public BugsCell market[] = null;
	public BugsCell goals[] = null;
	
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public BugsChip getPlayerChip(int p) { return(pbs[p].chip); }
	public BugsId getPlayerColor(int p) { return(pbs[p].color); }
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
			{	String dir = "g:/share/projects/boardspace-html/htdocs/bugs/prototypegames/robot/";
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
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public BugsChip pickedObject = null;
    public BugsChip lastPicked = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    
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
	{	return(new BugsCell(BugsId.BoardLocation,c,r));
	}
	
	// constructor 
    public BugsBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = GRIDSTYLE;
        setColorMap(map, players);
        
		Random r = new Random(734687);
		masterDeck =  new BugsCell(r,BugsId.MasterDeck);
		activeDeck =  new BugsCell(r,BugsId.ActiveDeck);
		masterGoalDeck = new BugsCell(r,BugsId.MasterGoalDeck);
		goalDeck = new BugsCell(r,BugsId.GoalDeck);
		
		for(int i=0;i<BugCard.bugCount();i++) { masterDeck.addChip(BugCard.getCard(i)); }
		
		// do this once at construction
	    market = new BugsCell[N_MARKETS];
	    goals = new BugsCell[N_GOALS];
	    for(int i=0;i<N_MARKETS;i++)
	    {
	    	market[i] = new BugsCell(r,BugsId.Market,i);
	    }
	    for(int i=0;i<N_GOALS;i++)
	    {
	    	goals[i] = new BugsCell(r,BugsId.Goal,i);
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
    {
		Hashtable <String,Taxonomy> cats = new Hashtable<String,Taxonomy>();
		Random r = new Random(randomKey+154135);
		int map[] = AR.intArray(masterDeck.height());
		int nCells = nBoardCells();
		int deckSize = (int)(DECKSIZE_MULTIPLIER*nCells*3);
		Taxonomy animals = Taxonomy.get("Animalia");
		int ncards = masterDeck.height();
		int lim = ncards;
		r.shuffle(map);
		activeDeck.reInit();
		while((cats.size()<N_ACTIVE_CATEGORIES
				|| activeDeck.height()<deckSize)
				&& lim-->0
				)
		{	
			BugCard ch = (BugCard)masterDeck.chipAtIndex(map[lim]);
			Profile prof = ch.getProfile();
			Taxonomy cat = prof.getCategory();
			String catName = cat.getScientificName();
			boolean include = cat!=animals && cats.get(catName)==null;
			if(include)
			{	cats.put(catName,cat);
				for(int i=0;i<lim; i++)
				{
					BugCard in = (BugCard)masterDeck.chipAtIndex(i);
					Taxonomy bugCat = in.getProfile().getCategory();
					if(bugCat==cat) { activeDeck.addChip(in); }				
				}
			}
		}
		int nWild = (int)(deckSize*WILDPERCENT);
		lim = ncards;
		while(nWild > 0 && lim-->0)
		{
			BugCard ch = (BugCard)masterDeck.chipAtIndex(map[lim]);
			Taxonomy cat = ch.getProfile().getCategory();
			if(cat==animals) 
				{ nWild--; activeDeck.addChip(ch); 
				}
		}
		G.print("cats ",cats.size()," deck ",activeDeck.height());
		activeDeck.shuffle(r);
    }
    
    /**
     * assign habitats to the board cells, in a proportion that's appropriate
     * for the bugs that are in the deck.
     */	
    public void buildHabitat()
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
       	while(allocCells<nCells)
       	{
       		Random r = new Random(2626+randomKey);
       		switch(r.nextInt(4))
       		{
       		default:
       		case 0: nWaterCells++; break;
       		case 1: nGroundCells++; break;
       		case 2: nForestCells++; break;
       		case 3: nGrassCells++; break;
       		}
       		allocCells++;
       	}
       	while(allocCells>nCells)
       	{
       		Random r = new Random(236726+randomKey);
       		switch(r.nextInt(4))
       		{
       		default:
       		case 0: if(nWaterCells>onlyWater) {  nWaterCells--; allocCells--; } break;
       		case 1: if(nGroundCells>onlyGround) { nGroundCells--; allocCells--; } break;
       		case 2: if(nForestCells>onlyForest) { nForestCells--; allocCells--; } break;
       		case 3: if(nGrassCells>onlyGrass) { nGrassCells--; allocCells--; } break;
       		}
       	}
       	// we now have determined the right number of cells of appropriate types
       	BugsCell cells = allCells;
       	cells.reInit();
       	while(nWaterCells-- > 0) { cells.addChip(BugsChip.MarshTile); }
       	while(nForestCells-- > 0) { cells.addChip(BugsChip.ForestTile); }
       	while(nGroundCells-- > 0) { cells.addChip(BugsChip.GroundTile); }
       	while(nGrassCells-- > 0) { cells.addChip(BugsChip.PrarieTile); }
       	cells.shuffle(new Random(7337+randomKey));
       	while(cells!=null) { cells.background = allCells.removeTop(); cells=cells.next; }
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
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case bugspiel:
		case bugspiel2:
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
		GoalCard.buildGoalDeck(activeDeck.toArray(),goalDeck);
		
		Random r = new Random(randomKey+14125);
		goalDeck.shuffle(r);

		reInit(market);
		reInit(goals);
 		for(int i=0;i<market.length;i++)
 		{
 			market[i].addChip(activeDeck.removeTop());
 		}
 		for(int i=0;i<goals.length;i++)
 		{
 			goals[i].addChip(goalDeck.removeTop());
 		}
	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    droppedDestStack.clear();
	    pickedSourceStack.clear();
	    stateStack.clear();
	    
	    pickedObject = null;
	    resetState = null;
	    lastDroppedObject = null;
	    int map[]=getColorMap();
	    pbs = new PlayerBoard[players_in_game];
	    for(int i=0;i<players_in_game; i++) { pbs[i] = allPbs[map[i]]; pbs[i].doInit(); }
	    
        animationStack.clear();
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
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
        copyFrom(market,from_b.market);
        copyFrom(goals,from_b.goals);
        copyFrom(activeDeck,from_b.activeDeck);
        copyFrom(goalDeck,from_b.goalDeck);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        lastPicked = null;
        for(int i=0;i<pbs.length;i++) { pbs[i].copyFrom(from_b.pbs[i]); }
 
        sameboard(from_b); 
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
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedsourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(activeDeck.sameContents(from_b.activeDeck),"active deck mismatch");
        G.Assert(sameCells(market,from_b.market),"market mismatch");
        G.Assert(sameCells(goals,from_b.goals),"goals mismatch");
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
		v ^= activeDeck.Digest(r);
		v ^= goalDeck.Digest(r);
		v ^= Digest(r,market);
		v ^= Digest(r,goals);
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,revision);
		v ^= Digest(r,board_state);
		v ^= Digest(r,whoseTurn);
		for(int i=0;i<pbs.length;i++) { v ^= pbs[i].Digest(r); }
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
    {	if(win[player]) { return(true); }
    	boolean win = false;
    	return(win);
    }


    // set the contents of a cell, and maintain the books
    public BugsChip SetBoard(BugsCell c,BugsChip ch)
    {	BugsChip old = c.topChip();
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
    private BugsCell unDropObject()
    {	BugsCell rv = droppedDestStack.pop();
    	setState(stateStack.pop());
    	pickedObject = SetBoard(rv,null); 	// SetBoard does ancillary bookkeeping
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	BugsCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	SetBoard(rv,pickedObject);
    	pickedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(BugsCell c)
    {
       droppedDestStack.push(c);
       stateStack.push(board_state);
       
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting dest " + c.rackLocation);
        case PlayerGoals:
        case PlayerBugs:
        case ActiveDeck:
        case Green:
        case Goal:
        case Market:
        case Yellow:		// back in the pool, we don't really care where
        	c.addChip(pickedObject);
        	pickedObject = null;
            break;
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        	SetBoard(c,pickedObject);
            pickedObject = null;
            break;
        }
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(BugsCell c)
    {	return(droppedDestStack.top()==c);
    }
    public BugsCell getDest()
    {	return(droppedDestStack.top());
    }
 
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { BugsChip ch = pickedObject;
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
    private BugsCell getCell(BugsId source, char col, int row)
    {
        switch (source)
        {
        default:
        	return pbs[col-'A'].getCell(source,col,row);
        case Goal:
        	return goals[row];
        case MasterDeck:
        	return masterDeck;
        case ActiveDeck:
        	return activeDeck;
        case GoalDeck:
        	return goalDeck;
        case Market:
        	return market[row];
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
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(BugsCell c)
    {	pickedSourceStack.push(c);
    	stateStack.push(board_state);
        switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting rackLocation " + c.rackLocation);
        case Market:
        case Goal:
        case GoalDeck:
        case ActiveDeck:
        case BoardLocation:
        	{
            lastPicked = pickedObject = c.topChip();
         	lastDroppedObject = null;
			SetBoard(c,null);
        	}
            break;

        case Green:
        case Yellow:
        	lastPicked = pickedObject = c.topChip();
        }
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(BugsCell c)
    {	return(c==pickedSourceStack.top());
    }
    public BugsCell getSource()
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
			setState(BugsState.Confirm);
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
     	case Confirm:
    	case Puzzle:
    	case Play:
    		setState(BugsState.Play);
    		
    		break;
    	}
       	resetState = board_state;
    }
    private void doDone(replayMode replay)
    {
        acceptPlacement();

        if (board_state==BugsState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(BugsState.Gameover);
        }
        else
        {	if(winForPlayerNow(whoseTurn)) 
        		{ win[whoseTurn]=true;
        		  setState(BugsState.Gameover); 
        		}
        	else {setNextPlayer(replay);
        		setNextStateAfterDone(replay);
        	}
        }
    }
	
    public boolean Execute(commonMove mm,replayMode replay)
    {	BugsMovespec m = (BugsMovespec)mm;
        if(replay.animate) { animationStack.clear(); }

        //G.print("E "+m+" for "+whoseTurn+" "+state);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone(replay);

            break;

        case MOVE_DROPB:
        	{
			BugsChip po = pickedObject;
			BugsCell dest =  getCell(BugsId.BoardLocation,m.to_col,m.to_row);
			
			if(isSource(dest)) 
				{ unPickObject(); 
				}
				else 
				{
				m.chip = pickedObject;
		           
	            dropObject(dest);
	            /**
	             * if the user clicked on a board space without picking anything up,
	             * animate a stone moving in from the pool.  For Hex, the "picks" are
	             * removed from the game record, so there are never picked stones in
	             * single step replays.
	             */
	            if(replay.animate && (po==null))
	            	{ animationStack.push(getSource());
	            	  animationStack.push(dest); 
	            	}
	            setNextStateAfterDrop(replay);
				}
        	}
             break;
        case MOVE_ROTATECCW:
        	{	
        	BugsCell src = getCell(m.source,m.to_col,m.to_row);
        	src.rotation++;
        	if(src.rotation>1) { src.rotation = -1; }
        	}
        	break;
        case MOVE_ROTATECW:
        	{
        	BugsCell src = getCell(m.source,m.to_col,m.to_row);
        	src.rotation--;
        	if(src.rotation<-1) { src.rotation = 1; }
        	}
        	break;
        case MOVE_PICK:
 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
 			BugsCell src = getCell(m.source,m.to_col,m.to_row);
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
        		setState( BugsState.Play);
        		break;
        	default: ;
        	}}}
            break;

        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
            BugsCell dest = getCell(m.source,m.to_col,m.to_row);
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
            setState(BugsState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ setState(BugsState.Gameover); 
               	}
            else {  setNextStateAfterDone(replay); }

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?BugsState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(BugsState.Puzzle);
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(BugsState.Gameover);
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
        case Play:
        	// for pushfight, you can pick up a stone in the storage area
        	// but it's really optional
        	return(player==whoseTurn);
        case Confirm:
		case Resign:
		case Gameover:
			return(false);
        case Puzzle:
            return ((pickedObject!=null)?(pickedObject==getCurrentPlayerChip()):true);
        }
    }

    public boolean legalToHitBoard(BugsCell c,Hashtable<BugsCell,BugsMovespec> targets )
    {	if(c==null) { return(false); }
        switch (board_state)
        {
		case Play:
		case Gameover:
		case Resign:
			return(false);
		case Confirm:
			return(isDest(c) || c.isEmpty());
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
    public void RobotExecute(BugsMovespec m)
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
    public void UnExecute(BugsMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	BugsState state = robotState.pop();
        switch (m.op)
        {
        default:
   	    	throw G.Error("Can't un execute " + m);
        case MOVE_DONE:
            break;
        case MOVE_DROPB:
        	SetBoard(getCell(m.to_col,m.to_row),null);
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
 
 void addPickDrop(CommonMoveStack all,BugsCell c,int who)
 {	if(c.onBoard)
 	{
	 addPickDrop(all,c.above,who);
	 addPickDrop(all,c.below,who);
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
		 all.push(new BugsMovespec(c.onBoard ? MOVE_DROPB : MOVE_DROP,c,who));
	 }
 }

 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	switch(board_state)
 	{
 	case Puzzle:
 		{
 			for(BugsCell c = allCells;
 			 	    c!=null;
 			 	    c = c.next)
 			 	{	addPickDrop(all,c,whoseTurn);
 			 	}
  		}
 		for(BugsCell c : market) { addPickDrop(all,c,whoseTurn); }
 		for(BugsCell c : goals) { addPickDrop(all,c,whoseTurn); }
 		addPickDrop(all,activeDeck,whoseTurn);
 		addPickDrop(all,goalDeck,whoseTurn);
 		for(PlayerBoard pb : pbs)
 		{
 			pb.getListOfMoves(all,whoseTurn);
 		}
 		break;
 	case Play:
 	case Confirm:
 		all.push(new BugsMovespec(MOVE_DONE,whoseTurn));
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
	 		case bugspiel2:
	 		case bugspiel:
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
 public Hashtable<BugsCell, BugsMovespec> getTargets() 
 {
 	Hashtable<BugsCell,BugsMovespec> targets = new Hashtable<BugsCell,BugsMovespec>();
 	CommonMoveStack all = GetListOfMoves();
 	for(int lim=all.size()-1; lim>=0; lim--)
 	{	BugsMovespec m = (BugsMovespec)all.elementAt(lim);
 		switch(m.op)
 		{
 		case MOVE_PICKB:
 		case MOVE_DROPB:
 		case MOVE_DROP:
 		case MOVE_PICK:
 			targets.put(getCell(m.source,m.to_col,m.to_row),m);
 			break;
 		case MOVE_SWAP:
 		case MOVE_DONE:
 			break;

 		default: G.Error("Not expecting "+m);
 		
 		}
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
 			*/
 //}

 // most multi player games can't handle individual players resigning
 // this provides an escape hatch to allow it.
 //public boolean canResign() { return(super.canResign()); }
}
