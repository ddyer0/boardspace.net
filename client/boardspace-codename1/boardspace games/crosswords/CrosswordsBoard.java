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
package crosswords;

import static crosswords.Crosswordsmovespec.*;

import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;
import online.game.cell.Geometry;
import dictionary.Dictionary;
import dictionary.DictionaryHash;
import dictionary.Entry;

/**
 * Initial work September 2020
 *  
 * This shares a lot with Jumbulaya and Wyps
 * if any features or bug fixes occur, evaluate them too.
 * 
 * @author ddyer
 *
 */

/*
 * in addition to the standard stack, this class has some features
 * to assist collecting plausible moves.
 */
class WordStack extends OStack<Word>
{
	public Word[] newComponentArray(int sz) {
		return new Word[sz];
	}	
	public int bestScore = 0;				// best score so far
	public int leastScore = 0;				// the lowest score currently in the stack
	
	public static final double trimSize = 0.8;			// size to trim to
	public static final double threshold = 0.5;			// threshold from current best to allow addition
	public static int sizeLimit = 10;					// max entries
	int accepted = 0;
	int declined = 0;
	int prevLeastScore = 0;
	int prevBestScore = 0;
	public void clear()
	{
		super.clear();
		bestScore = 0;
		leastScore = 0;
		accepted = 0;
		declined = 0;
		prevLeastScore = 0;
		prevBestScore = 0;
	}
	public void unAccept(Word w)
	{	if(w==top())
		{
		accepted--;
		leastScore = prevLeastScore;
		bestScore = prevBestScore;
		pop();
		}
	else {
		remove(w,true);
		}
	}
	// record a candidate word if it is a plausible candidate.
	// trim the active list to the prescribed size
	public Word recordCandidate(String message,CrosswordsCell c,String s,int direction,int score,Entry e)
	{	if(score>=bestScore*threshold && score>leastScore)
		{
		for(int lim=size()-1;lim>=0;lim--)
		{
			Word entry = elementAt(lim);
			if(entry.name.equals(e.word) && entry.seed==c) 
			{
				return(null);
			}
		}
		trimToSize();
		Word w = new Word(c,s,direction);
		push(w);
		w.entry = e;
		w.points = score;
		w.comment = message;
		prevBestScore = bestScore;
		bestScore = Math.max(score, bestScore);
		accepted++;
		return(w);
		}
		declined++;
		return(null);
	}


	public void trimToSize()
	{
		if(size()>=sizeLimit)
		{
			sort(true);
			setSize((int)(sizeLimit*trimSize));
			leastScore = top().points;
			prevLeastScore = leastScore;
		}
	}
}

/**
 * class representing a word on the board, starting at some cell
 * and extending in some direction.  These are used to check for
 * duplicate words, and also to check that all the letters played
 * are connected.
 * 
 * @author Ddyer
 *
 */
class Word implements StackIterator<Word>,CompareTo<Word>
{
	String name;			// the actual word
	CrosswordsCell seed;	// starting point
	int direction=-1;			// scan direction
	int points=-1;				// the value of the word when played
	String comment = null;	// for the word search
	Entry entry;
	public String toString() 
	{ StringBuilder b = new StringBuilder();
	  b.append("<word ");
	  b.append(name);
	  b.append(" ");
	  b.append(seed.col);
	  b.append(seed.row);
	  if(comment!=null)
	  {	  b.append(" ");
		  b.append(comment);
		  b.append(" points:");
		  b.append(points);
		  if(entry!=null)
		  {	b.append(" Order:");
		    b.append(entry.order);
		  }
	  }
	  b.append(">");
	  return(b.toString());
	}
	public CrosswordsCell lastLetter()
	{
		CrosswordsCell s = seed;
		for(int lim=name.length()-1; lim>0; lim--)
		{
			s = s.exitTo(direction);
		}
		return(s);
	}
	public int reverseDirection()
	{
		int n = seed.geometry.n;
		return((direction+n/2)%n);
	}
	public boolean sameWord(Word other)
	{
		if(name.equals(other.name))
		{
			if(seed.sameCell(other.seed) && direction==other.direction ) 
				{ return(true); 
				}
			if(seed.sameCell(other.lastLetter()) && (direction==other.reverseDirection()))
				{//G.print("Palindrone "+this);
				 return(true); 
				}
		}
		return(false);
	}
	public Word(CrosswordsCell s, String n, int di)
	{
		seed = s;
		name = n;
		direction = di%s.geometry.n;
	}
	
	// true if this word and target word share a cell
	public boolean connectsTo(Word target,int sweep)
	{
		// return true if this word and the target word share structure
		{CrosswordsCell c = seed;
		// mark the letters
		while(c!=null && (c.topChip()!=null)) { c.sweep_counter = sweep; c = c.exitTo(direction); }
		}
		{
		CrosswordsCell c = seed;
		while(c!=null && (c.topChip()!=null)) { if(c.sweep_counter==sweep) { return(true); } c=c.exitTo(direction); }
		return(false);
		}
	}

	public int size() {
		return(1);
	}
	public Word elementAt(int n) 
	{
		return(this);
	}

	public StackIterator<Word> push(Word item) {
		WordStack s = new WordStack();
		s.push(this);
		s.push(item);
		return(s);
	}

	public StackIterator<Word> remove(Word item) {
		if(item==this) { return(null); }
		return(this);
	}

	public StackIterator<Word> remove(int n) {
		if(n==0) { return(null); }
		return(this);
	}
	public StackIterator<Word> insertElementAt(Word item, int at) {
		return(push(item));		
	}

	public int compareTo(Word o) {
		return G.signum(points-o.points);
	}

	public int altCompareTo(Word o) {
		return G.signum(o.points-points);
	}
}

class CrosswordsBoard extends rectBoard<CrosswordsCell> implements BoardProtocol
{	static int REVISION = 108;			// 100 represents the initial version of the game
										// revision 101 reduces the size of the rack to actual size and adds rack maps
										// revision 102 does nothing
										// revision 103 fixed "no duplicate words" bug
										// revision 104 makes hand size same as rack size
										// revision 105 fixes rack throw back broken by 104
										// revision 106 changes the default for "backwards"
										// revision 107 fixes the split word direction bug
										// revision 108 changes the replacement of tiles when the draw pile is empty

	static final String[] CrosswordsGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	public int getMaxRevisionLevel() { return(REVISION); }
	static final int MAX_PLAYERS = 4;
	static final int rackSize = 7;		// the number of filled slots in the rack.  Actual rack has some empty slots
	static final int rackSpares = 2;
	int sweep_counter = 0;
	CrosswordsVariation variation = CrosswordsVariation.Crosswords;
	private CrosswordsState board_state = CrosswordsState.Puzzle;	
	private CrosswordsState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	public int robotVocabulary = 999999;		//	size of the robot's vocabulary
	private boolean isPass = false;
	private int nPasses = 0;
	public CrosswordsState getState() { return(board_state); }
    StringStack gameEvents = new StringStack();
    InternationalStrings s = G.getTranslations();
    public int scoreForPlayer(int i)
    {
    	return(score[i]);
    }
 	void logGameEvent(String str,String... args)
 	{	//if(!robotBoard)
 		{String trans = s.get(str,args);
 		 gameEvents.push(trans);
 		}
 	}

    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(CrosswordsState st) 
	{ 	unresign = (st==CrosswordsState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public void setGameOver()
	{
		setState(CrosswordsState.Gameover);
		int maxv = -1;
		int maxp = -1;
		int np = 0;
		for(int i=0;i<players_in_game;i++)
		{
			if(score[i]>=maxv)
			{	if(score[i]==maxv) { np ++; }
				else { np = 1;
					maxv = score[i];
				}
			maxp = i;
			}
		}
		if(np==1) { win[maxp] = true; }
	}
	CrosswordsCell rack[][] = null;
	private CrosswordsCell mappedRack[][] = null;
	
	public CrosswordsCell[] getPlayerRack(int n)
	{
		if(rack!=null && rack.length>n) { return(rack[n]); }
		return(null);
	}
	public CrosswordsCell[] getPlayerMappedRack(int n)
	{
		if(mappedRack!=null && mappedRack.length>n) { return(mappedRack[n]); }
		return(null);
	
	}

	private int mapPick[] = null;
	private int mapTarget[] = null;		// which actual cell is picked (per player)
	
	public int getMapPick(int playerIndex)
	{
		if(mapPick!=null && mapPick.length>playerIndex)  { return(mapPick[playerIndex]); }
		return(-1);
	}
	public int getMapTarget(int playerIndex)
	{
		if(mapTarget!=null && mapTarget.length>playerIndex)  { return(mapTarget[playerIndex]); }
		return(-1);
	}

	private int rackMap[][] = null;
	public int[] getRackMap(int n)
	{
		if(rackMap!=null && rackMap.length>n) { return(rackMap[n]); }
		return(null);
	}
	public boolean someRackIsEmpty()
	{
		for(CrosswordsCell r[] : rack) { if(rackIsEmpty(r)) { return(true); }}
		return(false);
	}
	private boolean rackIsEmpty(CrosswordsCell r[])
	{	for(CrosswordsCell c : r) { if(c.topChip()!=null) { return(false); }}
		return(true);
	}
	public boolean hiddenVisible[] = null;
	int score[] = null;
	CrosswordsCell drawPile = null;
	int chipsOnBoard = 0;
	boolean diagonals = false;			// allow diagonal words
	boolean backwards = false;			// allow backwards words
	boolean allConnected = false;		// allow multiple words as long as they are connected
	boolean noduplicates = false;		// no duplicate words
	boolean openRacks = false;			// show all players racks
	boolean openRack[];					// per player open rack
	Dictionary dictionary ;				// the current dictionary
	CellStack occupiedCells = new CellStack();
	
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
    public CrosswordsChip pickedObject = null;
    public CrosswordsChip lastPicked = null;
    private CrosswordsCell seedLocation = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    public CellStack lastLetters = new CellStack();
    private CrosswordsState resetState = CrosswordsState.Puzzle; 
    public CrosswordsChip lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public CrosswordsCell newcell(char c,int r)
	{	return(new CrosswordsCell(CrosswordsId.BoardLocation,c,r,Geometry.Oct));
	}
	
	// constructor 
    public CrosswordsBoard(String init,int players,long key,int map[],Dictionary di,int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = CrosswordsGRIDSTYLE;
        setColorMap(map, players);
    	// allocate the rack map once and for all, it's not used in the board
    	// only as part of the UI.
       	mapPick = new int[MAX_PLAYERS];
       	mapTarget = new int[MAX_PLAYERS];

		hiddenVisible = new boolean[MAX_PLAYERS];
		openRack = new boolean[MAX_PLAYERS];		// part of the user interface

       	rackMap = new int[MAX_PLAYERS][rackSize+rackSpares];
      	Random r = new Random(2975564);
       	drawPile = new CrosswordsCell(r,CrosswordsId.DrawPile);

       	doInit(init,key,players,rev); // do the initialization 


        dictionary = di;
 
    }
    private void initRackMap(int [][]map)
    {	int full = map[0].length;
    	int max = revision<104 ? full : rackSize;
    	int off = (full-max)/2;
    	for(int []row : map)
    	{	for(int i=0; i<row.length;i++) { row[i] = (i>=off && i<max+off) ? i-off : -1; }
    	}
    }
    
    public String gameType() { return(gametype+" "+players_in_game+" "+randomKey+" "+revision); }
    
    public CrosswordsCell allPostCells = null;
    private CrosswordsCell bonusCells[] = null;
    
    public cell<?> postBoard[][] = null;
    @SuppressWarnings("unchecked")
	public CrosswordsCell getPostCell(char col,int row)
    {	int coln = col-'A';
    	if(coln>=0 && coln<postBoard.length)
    	{
    		cell<CrosswordsCell>[] crow =(cell<CrosswordsCell>[])postBoard[coln];
    		if(row>=1 && row<=crow.length) {
    			return((CrosswordsCell)crow[row-1]);
    		}
    	}
    	return(null);
    }
    private void construct(int n,int k)
    {	Random r = new Random(6278843);
    	// create a grid for the post cells
        initBoard(n-1,k-1);		 //this sets up the primary board and cross links
        allCells.setDigestChain(r);
        postBoard = getBoardArray();
        for(CrosswordsCell c = allCells; c!=null; c=c.next) { c.isPostCell = true; }
        allPostCells = allCells;
        // create a grid for the tile cells
        allCells = null;
        initBoard(n,k);		 //this sets up the primary board and cross links
        allCells.setDigestChain(r);
        for(CrosswordsCell c = allCells; c!=null; c=c.next) { c.isTileCell = true; }
  
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
    
    private void positionBonusSpaces()
    {	int numberOfBonuses = variation.boardsize;
 		Random r = new Random(randomKey);
		CellStack st = new CellStack();
		bonusCells = new CrosswordsCell[numberOfBonuses];
 		// position the bonus squares randomly
 		for(CrosswordsCell c = allPostCells; c!=null; c=c.next) { st.push(c); c.addChip(CrosswordsChip.Post); }
 		st.shuffle(r);
 		for(int i=0;i<numberOfBonuses;i++)
			{	CrosswordsCell c = st.pop();
				int v = r.nextInt(10);
				c.removeTop();
				bonusCells[i]=c;
				switch(v)
				{
				default:
				case 0:
				case 1:
				case 2: c.addChip(CrosswordsChip.DoubleLetterGreen);
						break;
				case 3:
				case 4:
						c.addChip(CrosswordsChip.TripleLetterYellow);
						break;
				case 5:
				case 6:
				case 7:
						c.addChip(CrosswordsChip.DoubleWordBlue);
						break;
				case 8:
				case 9: c.addChip(CrosswordsChip.TripleWordRed);
						break;
						
					
				}		
			}
    }
    
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int players,int rev)
    {	randomKey = key;
    	adjustRevision(rev);
    	players_in_game = players;
    	win = new boolean[players];
		variation = CrosswordsVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		score = new int[players];
    	gametype = gtype;
    	nPasses = 0;
    	isPass = false;
		noduplicates = false;
		openRacks = false;
	    for(Option o : Option.values()) { setOptionValue(o,false); }
		lastLetters.clear();
	    occupiedCells.clear();
	    initRackMap(rackMap);
 		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case Crosswords:
			diagonals = false;
			allConnected = false;
			backwards = revision>=106;
			break;
		case Crosswords17:
			diagonals = true;
			allConnected = true;
			backwards = true;
		}
 		
 		construct(variation.boardsize,variation.boardsize);
			
 		positionBonusSpaces();
			
 		//position the starting blank
 		seedLocation = getCell((char)('A'+variation.boardsize/2),1+variation.boardsize/2);
 		seedLocation.addChip(CrosswordsChip.A);
 		occupiedCells.push(seedLocation);
 		
 		chipsOnBoard = 1;    
    	Random r = new Random(randomKey+100);
    	r.nextLong();//  use one random, for compatibility
    	drawPile.reInit();
    	for(CrosswordsChip c : CrosswordsChip.letters)
    	{
    		drawPile.addChip(c);
    	}
    	drawPile.shuffle(r);
    	
 		int racks = revision<104 ? rackSize+rackSpares : rackSize;
 		if(rack==null || rack.length!=players || racks!=rack[0].length)
 		{
    	rack = new CrosswordsCell[players][racks];
       	mappedRack = new CrosswordsCell[players][rackSize+rackSpares];
        
    	for(int i=0;i<players;i++)
    	{	CrosswordsCell prack[] = rack[i];
    		for(int j = 0;j<prack.length;j++)
    			{ CrosswordsCell c = rack[i][j] = new CrosswordsCell(CrosswordsId.Rack,(char)('A'+i),j,Geometry.Standalone);
    			  c.onBoard = false;
    			}
    		CrosswordsCell mrack[] = mappedRack[i];
    		for(int j = 0;j<mrack.length; j++)
    		{
  			  mrack[j] = new CrosswordsCell(CrosswordsId.RackMap,(char)('A'+i),j,Geometry.Standalone);

    		}
    	}
    	}
    	else 
    		{ reInit(rack); 
    		  reInit(mappedRack);
 		}
 		
    	for(int i=0;i<players;i++)
    	{	CrosswordsCell prack[] = rack[i];
    		for(int j = 0,least = (revision<=103)?0:-1;j<prack.length;j++)
    			{ CrosswordsCell c = rack[i][j];
    			  if(j>least && j<=rackSize) { c.addChip(drawPile.removeTop()); }
    			}
    	}
 
    	AR.setValue(mapPick,-1);
       	AR.setValue(mapTarget, -1);
    	
    	

		setState(CrosswordsState.Puzzle);
		robotState.clear();		
	    	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    
	    acceptPlacement();

	    resetState = CrosswordsState.Puzzle;
	    lastDroppedObject = null;
	    // set the initial contents of the board to all empty cells
        animationStack.clear();
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }

    /** create a copy of this board */
    public CrosswordsBoard cloneBoard() 
	{ CrosswordsBoard dup = new CrosswordsBoard(gametype,players_in_game,randomKey,getColorMap(),dictionary,revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((CrosswordsBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(CrosswordsBoard from_b)
    {
        super.copyFrom(from_b);
        robotState.copyFrom(from_b.robotState);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        chipsOnBoard = from_b.chipsOnBoard;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        drawPile.copyFrom(from_b.drawPile);
        copyFrom(rack,from_b.rack);
        AR.copy(score,from_b.score);
        robotVocabulary = from_b.robotVocabulary;
        lastPicked = null;
        isPass = from_b.isPass;
        nPasses = from_b.nPasses;
        for(Option o : Option.values()) { setOptionValue(o,from_b.getOptionValue(o)); }
        seedLocation = getCell(from_b.seedLocation);
        getCell(occupiedCells,from_b.occupiedCells);
        words.copyFrom(from_b.words);
        AR.copy(mapPick,from_b.mapPick);
        AR.copy(mapTarget,from_b.mapTarget);
        AR.copy(rackMap,from_b.rackMap);
        copyFrom(mappedRack,from_b.mappedRack);
        AR.copy(openRack,from_b.openRack);
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((CrosswordsBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(CrosswordsBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(chipsOnBoard==from_b.chipsOnBoard,"chipsOnBoard mismatch");
        G.Assert(robotVocabulary==from_b.robotVocabulary,"robotVocabulary mismatch");
        sameCells(rack,from_b.rack);
        sameCells(seedLocation,from_b.seedLocation);
        sameCells(drawPile,from_b.drawPile);
        G.Assert(AR.sameArrayContents(score,from_b.score),"score mismatch");
        G.Assert(nPasses==from_b.nPasses,"nPasses mismatch");
        G.Assert(isPass==from_b.isPass,"isPass mismatch");
        for(Option o : Option.values()) { G.Assert(getOptionValue(o)==from_b.getOptionValue(o),"Option %s mismatch",o); }
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
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,revision);
		v ^= Digest(r,rack);
		v ^= Digest(r,drawPile);
		v ^= Digest(r,seedLocation);
		v ^= Digest(r,chipsOnBoard);
		v ^= Digest(r,score);
		v ^= Digest(r,isPass);
		v ^= Digest(r,nPasses);
		v ^= Digest(r,robotVocabulary);
		for(Option o : Option.values()) { v ^= Digest(r,getOptionValue(o)); }
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
        case Confirm:
        case ConfirmFirstPlay:
        case FirstPlay:
        case DiscardTiles:
        case Resign:
            moveNumber++; //the move is complete in these states
            setWhoseTurn(nextPlayer());
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
    public CrosswordsChip SetBoard(CrosswordsCell c,CrosswordsChip ch)
    {	CrosswordsChip old = c.topChip();
    	if(c.onBoard)
    	{
    	if(ch==null) 
    		{ if(c.topChip()!=null) { chipsOnBoard--; }
    		  c.reInit();
    		  occupiedCells.remove(c,false);
    		}
    	else { G.Assert(c.isEmpty(),"not expecting to make stacks");
    		c.addChip(ch);
    		occupiedCells.push(c);
    		chipsOnBoard++;
    		}
    	}
    	else {
        	if(ch==null) 
        		{ 
        		  c.reInit();
        		}
        	else { G.Assert(c.isEmpty(),"not expecting to make stacks");
        		c.addChip(ch);
        		}
    	}
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
    private CrosswordsCell unDropObject(CrosswordsCell c)
    {	CrosswordsCell rv = droppedDestStack.remove(c,false);
    	setState(stateStack.pop());
    	if(rv==drawPile) { pickedObject = drawPile.removeTop(); }
    	else {
    	CrosswordsChip po = SetBoard(rv,null); 	// SetBoard does ancillary bookkeeping
    	if(po.isBlank()) { po = CrosswordsChip.Blank; }
    	pickedObject = po;
    	}
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject(CrosswordsCell rv)
    {	pickedSourceStack.remove(rv);
    	setState(stateStack.pop());
    	SetBoard(rv,pickedObject);
    	pickedObject = null;
    }
    
    public CrosswordsCell lastDropped()
    {
    	return(droppedDestStack.top());
    }
    public boolean dropIsHorizontal()
    {
    	if(droppedDestStack.size()<2) { return(true); }
    	CrosswordsCell c1 = droppedDestStack.top();
    	CrosswordsCell c0 = droppedDestStack.elementAt(0);
    	return(c1.row==c0.row);
    }
    
    // true if all new words are in a line
    public boolean isDropLine()
    {	int sz = droppedDestStack.size();
    	if(sz<=1) { return(true); }
    	CrosswordsCell root = droppedDestStack.elementAt(0);
    	boolean fixedrow = true;
    	boolean fixedcol = true;
    	for(int lim=droppedDestStack.size()-1; lim>=1; lim--)
    		{	CrosswordsCell c = droppedDestStack.elementAt(lim);
    			fixedcol &= c.col==root.col;
    			fixedrow &= c.row==root.row;
    		}
    	if(fixedrow||fixedcol) { return(true); }
    	
    	if(diagonals)
    	{
    		boolean fixedDiag1 = true;
    		boolean fixedDiag2 = true;
        	for(int lim=droppedDestStack.size()-1; lim>=1; lim--)
    		{	CrosswordsCell c = droppedDestStack.elementAt(lim);
    			int dx = (c.row-root.row);
    			int dy = (c.col-root.col);
    			fixedDiag1 &= (dx==dy);
    			fixedDiag2 &= (dx==-dy);
    		}
        	return(fixedDiag1||fixedDiag2);
    	}
    	return(false);
    	}
    
    // true if all new words are connected
    public boolean isNewConnected()
    {	int nnew = newWords.size();
    	if(nnew<=1) { return(true); }
    	int unconnectedIndex = 1;
    	// word 0, whatever it is, is connectected to itself.  Find some other word
    	// that is connected to a previously connected word
    	while(unconnectedIndex<nnew)
    	{
       		boolean newConnection = false;
       		for(int targetIndex = unconnectedIndex; !newConnection && targetIndex<nnew; targetIndex++)
    		{
    		Word target = newWords.elementAt(targetIndex);
    		// look for a new word to connect to the already connected
    		for(int connectedIndex = 0; connectedIndex<unconnectedIndex;connectedIndex++)
    		{	sweep_counter++;
    			if(newWords.elementAt(connectedIndex).connectsTo(target,sweep_counter))
    			{
    				// we found a new word that is connected to the corpus, swap it into
    				// position and proceed
    				if(targetIndex!=unconnectedIndex)
    				{
    				Word swap = newWords.elementAt(unconnectedIndex);
    				newWords.setElementAt(target,connectedIndex);
    				newWords.setElementAt(swap, targetIndex);
    				}
    				unconnectedIndex++;
    				newConnection=true;
    				break;
    			}
    		 }
    		}
       		if(!newConnection) { return(false); }
    	}
    	return(true);
    }
    	
    
    // 
    // drop the floating object.
    //
    private void dropObject(CrosswordsCell c)
    {
       
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting dest " + c.rackLocation);
        case DrawPile:
            droppedDestStack.push(c);
            c.addChip(pickedObject);
            stateStack.push(board_state);
            lastDroppedObject = pickedObject;
            pickedObject = null;
            break;
        case Rack:
           	G.Assert(c.topChip()==null && pickedObject!=null,"drop something on empty %s %s",c,pickedObject);
        	c.addChip(pickedObject);
        	pickedObject = null;
        	break;
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        case EmptyBoard:
            droppedDestStack.push(c);
            stateStack.push(board_state);
           	SetBoard(c,pickedObject);
            lastDroppedObject = pickedObject;
            pickedObject = null;
            break;
        }
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isADest(CrosswordsCell c)
    {
    	return droppedDestStack.contains(c);
    }
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { CrosswordsChip ch = pickedObject;
      if(ch!=null)
    	{	return(ch.chipNumber()); 
    	}
      	return (NothingMoving);
    }
    private CrosswordsCell getMapItem(CrosswordsCell c[],int item)
    {
       	for(int i=0;i<c.length;i++)
    	{
    		if(c[i].row==item) { return c[i]; }
    	}
       	throw G.Error("Item %d not found",item);
    }
   /**
     * get the cell represented by a source code, and col,row
     * @param source
     * @param col
     * @param row
     * @return
     */
    private CrosswordsCell getCell(CrosswordsId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source " + source);
        case DrawPile:
        	return(drawPile);
        case RackMap:
        	return getMapItem(mappedRack[col-'A'],row);
        case Rack:
        	return(rack[col-'A'][row]);
        case BoardLocation:
        case EmptyBoard:
        	return(getCell(col,row));
        } 	
    }
    /**
     * this is called when copying boards, to get the cell on the new (ie current) board
     * that corresponds to a cell on the old board.
     */
    public CrosswordsCell getCell(CrosswordsCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(CrosswordsCell c)
    {	pickedSourceStack.push(c);
    	stateStack.push(board_state);
        switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting rackLocation " + c.rackLocation);
        case Rack:
        	{
        	int idx = c.col-'A';
        	mapPick[idx]=-1;
        	mapTarget[idx]=-1;
        	int map[] = rackMap[idx];
        	validateMap(idx);
        	for(int i=0;i<map.length;i++) { if(map[i]==c.row) { map[i]=-1; break; }} 
          	CrosswordsChip po = c.removeTop();
            lastPicked = pickedObject = po;
         	lastDroppedObject = null;
 			validateMap(c.col-'A'); 
        	}
 			break;
        case DrawPile:
			{
			lastPicked = pickedObject = c.removeTop();
			}
			break;
        case BoardLocation:
        	{
        	CrosswordsChip po = c.topChip();
        	if(po.isBlank()) { po = CrosswordsChip.Blank; }
            lastPicked = pickedObject = po;
         	lastDroppedObject = null;
			SetBoard(c,null);
        	}
            break;

        }
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isASource(CrosswordsCell c)
    {	return(pickedSourceStack.contains(c));
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(CrosswordsCell c,CrosswordsChip ch,replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state " + board_state);
        case FirstPlay:
        	setState(CrosswordsState.ConfirmFirstPlay);
        	break;
        case Confirm:
        case Play:
        case ResolveBlank:
        	if(c==drawPile) { setState(CrosswordsState.DiscardTiles); }
        	else if(ch==CrosswordsChip.Blank && c.onBoard) { setState(CrosswordsState.ResolveBlank); }
        	else if(validate(false)) { setState(CrosswordsState.Confirm); }
        	else { setState(CrosswordsState.Play); }
			break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    
    public void setVocabulary(double value) {
    	Dictionary dict = Dictionary.getInstance();
    	robotVocabulary = (int)(dict.size()*value);
    }

    private boolean rackIsEmpty(int who)
    {	
    	for(CrosswordsCell c : rack[who]) { if(c.topChip()!=null) { return(false); }}
    	return(true);
    }
    private boolean allPassed()
    {	
    	return(nPasses>=players_in_game);
    }
    private void setNextStateAfterDone(replayMode replay)
    {	
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state "+board_state);
    	case Gameover: break;
    	case Puzzle:
     		if(chipsOnBoard==1) { setState(CrosswordsState.FirstPlay); break; }
			//$FALL-THROUGH$
		case Confirm:
		case ConfirmFirstPlay:
    	case DiscardTiles:
       	case FirstPlay:
    	case Play:
    		if(rackIsEmpty(whoseTurn)||allPassed()) 
    			{ setGameOver();
    			}
    		else {
    			setState( CrosswordsState.Play);
    		}
    		
    		break;
    	}
       	resetState = board_state;
    }
    public int nextPlayer()
    {
    	return((whoseTurn+1)%players_in_game);
    }
    
    private void drawNewTiles(replayMode replay)
    {	CrosswordsCell myrack[] = rack[whoseTurn];
    	CrosswordsCell mapped[] = mappedRack[whoseTurn];
    	int mymap[] = rackMap[whoseTurn];
    	int n=0;
    	validateMap(whoseTurn);

    	for(CrosswordsCell c : myrack) { if(c.topChip()!=null) { n++; }}
    	for(CrosswordsCell c : myrack)
    		{ if(drawPile.height()==0 || n==rackSize) break;
    		  if(c.topChip()==null)
    			  {
    			  CrosswordsChip newchip = drawPile.removeTop();
    			  c.addChip(newchip);
    			  n++;
    			  if(replay!=replayMode.Replay) 
    			  {	  CrosswordsCell cd = mapped[c.row];
    				  // the mapped cell is used only by the user interface and is loaded by
    				  // the user interface during redisplay, but we need to preload it so the
    				  // animation will know there is something to do.
    			      cd.addChip(newchip);
    				  animationStack.push(drawPile);
    				  animationStack.push(cd);
    			  }
				int empty = -1;
				boolean filled = false;
				int rackIdx = c.row;
				for(int mapidx = 0;!filled && mapidx<mymap.length; mapidx++)
				{	int mapv = mymap[mapidx];
					if(mapv==rackIdx) { filled = true; }
					else if((empty<0) && (mapv<0)) { empty = mapidx; }
				}
				if(!filled) { mymap[empty] = rackIdx; }

    			  }
    		}
    	if(revision>=108)
    	{
       	int ma[] = rackMap[whoseTurn];
    	for(int i=0;i<ma.length;i++)
    		{	
    		int v = ma[i];
    		if((v>=0) && (myrack[v].topChip()==null)) { ma[i] = -1; }
    		}}

    	validateMap(whoseTurn);


    }
    
    StringBuilder builder = new StringBuilder();
    boolean isNew = false;	// the word in the builder includes a new letter
	Hashtable<String,Word> wordsUsed = new Hashtable<String,Word>();
	WordStack words = new WordStack();
	WordStack newWords = new WordStack();
	WordStack nonWords = new WordStack();
	WordStack candidateWords = new WordStack();
	
	public String invalidReason=null;
	public int scoreChange = 0;
	/* 
	 * this validates that all the lines on the board are words, and are all
	 * connected, the list of new words, the list of words, and the list of non words.
	 * if andScore is true, it also calculates the score change and the lists
	 * of word caps.
	 */
   private synchronized boolean validate(boolean andScore)
    {
    	sweep_counter++;
    	builder.setLength(0);
    	words.clear();
    	nonWords.clear();
    	newWords.clear();
    	scoreChange = andScore ? 0 : -1;
    	int tileCount = validateFrom(seedLocation);
    	boolean connected = tileCount==chipsOnBoard;
    	boolean allwords = (nonWords.size()==0);
    	boolean dropline = allConnected || isDropLine();
    	boolean newconnected = isNewConnected();
    	boolean hasnew = newWords.size()>0;
    	Word duplicate = noduplicates ? findDuplicateWord() : null;
    	if(!dropline || !connected) 
    		{ invalidReason = NotALine; }
    	else if(!allwords) { invalidReason = NotWords; }
    	else if(!newconnected) { invalidReason = NotNewConnected; }
    	else if(duplicate!=null) { invalidReason = DuplicateWord; }
    	else invalidReason = null;
    	if(andScore) { findWordCaps(); }
    	return( connected 
    			&& allwords 
    			&& (revision<107 || (invalidReason==null)) // can't make valid words in 2 directions
    			&& ((revision<103) || (duplicate==null)) && hasnew);
    }

   	CellStack endCaps = new CellStack();	// cells at the end of words
   	CellStack startCaps = new CellStack(); 	// cells at the start of words
   	
   	// startCaps and endCaps contain all the cells at the ends of words
   	// the cells themselves contain an iterator of the words that share
   	// that endpoint.  Normally there is only one, but words from all
   	// directions could theoretically share an end or beginning
   	private void findWordCaps()
   	{	sweep_counter++;
   		endCaps.clear();
   		startCaps.clear();
   		for(int lim = words.size()-1; lim>=0; lim--)
   		{
   			Word w = words.elementAt(lim);
   			CrosswordsCell c = w.seed;
   			int direction = w.direction;
   			int opp = direction+CELL_HALF_TURN;
  			int mask = (1<<direction)|(1<<opp);
   			CrosswordsCell beg = c.exitTo(opp);
   			if(beg!=null)
   			{ if(beg.sweep_counter!=sweep_counter) 
   				{ beg.wordDirections = 0;
   				  beg.sweep_counter = sweep_counter;
   				  beg.wordHead = null;
   				}
   			  beg.wordDirections |= mask;
   			  beg.addWordHead(w);
   			  startCaps.pushNew(beg); beg.wordDirections |= (1<<direction); 
   			}
   			
   			while((c = c.exitTo(direction))!=null)
   			{	if(c.topChip()==null) 
   				{ if(c.sweep_counter!=sweep_counter) 
   					{ c.wordDirections = 0; 
   					  c.sweep_counter = sweep_counter;
   					  c.wordHead = null;
   					}
   				  endCaps.pushNew(c);
   				  c.wordDirections |= (1<<direction); 
   				  c.wordDirections |= mask;
   				  c.addWordHead(w);
   				  break; 
   				}
		
   			}
   		}
   	}
    // return some duplicate word if there are any.
   	// we don't need a complete list, just an example
   	// to enforce the "no duplicates" option
   	private Word findDuplicateWord()
   	{
   		wordsUsed.clear();
   		for(int lim=words.size()-1; lim>=0; lim--)
   		{	Word target = words.elementAt(lim);
   			String name = target.name;
   			if(wordsUsed.get(name)!=null) { return(target); }
   			wordsUsed.put(name,target);
   		}
   		return(null);
   	}
   	// 
   	// collect a word from a starting cell with a given direction
   	// 
    private String collectWord(CrosswordsCell from,int direction)
    {
    	builder.setLength(0);
    	int n=0;
    	isNew = false;
    	CrosswordsCell c = from;
    	while(c!=null) 
    	{
    		CrosswordsChip top = c.topChip();
    		if(top==null) {  break; }
    		builder.append(top.lcChar);
    		n++;
    		isNew |= isADest(c);			// keep track of new words for scoring and connection testing
    		c = c.exitTo(direction);
    	}
    	// leave builder primed with the letters, so it can be reversed
    	return(n>1 ? builder.toString() : null);
    }
    
    // keep track of the word level bonus cells that
    // have been encountered, so each one counts 
    // just once.
    private boolean addWordBonus(CrosswordsCell c)
    {
    	boolean is = G.arrayContains(wordBonus,c);
    	if(is) { return(false); }
    	wordBonus[0]=wordBonus[1];
    	wordBonus[1]=wordBonus[2];
    	wordBonus[2]=wordBonus[3];
    	wordBonus[3]=c;
    	return(true);
    }
    
    int wordMultiplier = 1;
    CrosswordsCell wordBonus[] = new CrosswordsCell[4];
    // score a word on the board.  wordMultiplier and wordBonus are
    // used as scratch variables during the scoring.
    private int scoreWord(CrosswordsCell from,int direction)
    {	int score = 0;
    	builder.setLength(0);
    	wordMultiplier = 1;
    	isNew = false;
    	CrosswordsCell c = from;
    	AR.setValue(wordBonus, null);
    	while(c!=null) 
    	{	
    		CrosswordsChip top = c.topChip();
    		if(top==null) {  break; }
    		c.wordDirections |= (1<<direction);
    		builder.append(top.lcChar);
    		isNew |= isADest(c);
    		if(isADest(c))
    		{	// only score bonuses for new letters
    			score += scoreLetter(c,top);
    		}
    		else { 
    			score += top.value;
    		}
    		c = c.exitTo(direction);
    	}
    	score *= wordMultiplier;
    	return(score);
    }
    
    // check for extra crosswords above and below the intended word
    // which must be words, too. return the total score associated
    // with any new words that are formed.  CrossIndex is the index 
    // of the known crossword - ie the word we are building from,
    // which has already been checked and also won't score
    public int checkIllegalCrosswords(String word,int crossIndex,CrosswordsCell from,int inDirection,CrosswordsCell fromRack[])
    {	int extraScore = 0;
    	CrosswordsCell c = from;
    	int oppdir = (inDirection+CELL_HALF_TURN)%CELL_FULL_TURN;
    	for(int idx = 0,len=word.length(); idx<len; idx++,c = c.exitTo(inDirection))
    	{
    		if(idx!=crossIndex)
    		{
    		 	int directionStep = diagonals ? 1 : CELL_QUARTER_TURN;
    		 	int lastDir = backwards ? CELL_FULL_TURN : diagonals ? CELL_FULL_TURN/2 : CELL_FULL_TURN;
    		 	int firstDir = diagonals ? 0 : backwards ? CELL_LEFT : CELL_RIGHT;
    		 	// be a little more general than needed for square crosswords, to allow for diagonal and backwards crosswords
    		 	for(int direction = firstDir; direction<lastDir; direction+=directionStep)
    		 	{	
    		 	
    		 		if((direction!=inDirection) && (oppdir!=direction))
    		 		{
    		 		// first back up to the beginning of the possible word
    		 		int revDir = direction+CELL_HALF_TURN;
    		 		CrosswordsCell head = c;
    		 		CrosswordsCell prev = c.exitTo(revDir);
    		 		CrosswordsCell next = c.exitTo(direction);
    		 		int nchars = 0;
    		 		if(prev!=null) 
    		 		{
    		 			CrosswordsChip ptop = prev.topChip();
    		 			if(ptop!=null)
    		 			{
    		 				builder.setLength(0);
    		 				do 
    		 					{ builder.append(ptop.lcChar);
    		 					  head = prev;
   		 					      nchars++;
    		 					  prev = prev.exitTo(revDir);
    		 					  ptop = prev==null ? null :prev.topChip();
    		 					} while(ptop!=null);
    		 				builder.reverse();
    		 				}
    		 			}
    		 		if(nchars>0) { builder.append(word.charAt(idx)); nchars++; }
    		 		if(next!=null)
    		 			{
    		 			CrosswordsChip ntop = next.topChip();
    		 			if(ntop!=null)
    		 				{
    		 				if(nchars==0) { builder.setLength(0); builder.append(word.charAt(idx)); nchars++; }
    		 				nchars++;
    		 				do { 
    		 					builder.append(ntop.lcChar);
    		 					nchars++;
    		 					next = next.exitTo(direction);
    		 					ntop = next==null ? null : next.topChip();
    		 					}
    		 					while(ntop!=null);
    		 				}}
    		 			if(nchars>1)
    		 			{
    		 			String newWord = builder.toString();
    		 			Entry e = lookupRobotWord(newWord);
    		 			if(e==null) { // found an illegal crossword
    		 					return(-1);
    		 				}
    		 			extraScore += scoreWord(newWord,head,direction,fromRack);
    		 			}
    		 		}
    		 	}}
    	}
    	return(extraScore);
    }
    // score a letter on the board, and change wordMultiplier as a side
    // effect.  This is used both for scoring actually placed words and
    // for proposed words that are not yet on the board
    // wordBonus and wordMultiplier are used as scratch
    private int scoreLetter(CrosswordsCell c,CrosswordsChip top)
    {	int letterScore = top.value;
		for(int dx = -1; dx<=0; dx++)
		{ for(int dy = -1; dy<=0; dy++)
		{	CrosswordsCell post = getPostCell((char)(c.col+dx),c.row+dy);
			if(post!=null)
			{
				CrosswordsChip bonus = post.topChip();
				if(bonus!=null)
				{
					if(bonus==CrosswordsChip.DoubleLetterGreen) { letterScore *=2; }
					else if(bonus==CrosswordsChip.TripleLetterYellow) { letterScore *=3; }
					else if(bonus==CrosswordsChip.DoubleWordBlue) 
						{ if(addWordBonus(post))
							{wordMultiplier *=2; 
							}
						}
					else if(bonus==CrosswordsChip.TripleWordRed) 
						{ if(addWordBonus(post))
							{wordMultiplier *=3; 
							}
						}
					else if(bonus==CrosswordsChip.Post) {} 
					else { G.Error("Not expecting bonus",bonus); }
				}
			}
		}}
		return(letterScore);
    }
    private Entry lookupRobotWord(String word)
    {	Entry entry = dictionary.get(word);
    	if(entry!=null) { if(entry.order>robotVocabulary) { entry = null; }}
    	return(entry);
    }

    // backwards words allows palindromes to be entered twice, which we do not want
    private boolean isUnique(Word newWord,WordStack existingWords)
    {	String word = newWord.name;
    	for(int lim = existingWords.size()-1; lim>=0; lim--)
    	{
    	Word e = existingWords.elementAt(lim);
    	if(e.name.equals(word))
    		{
    		// same word is allowed, but it has to use different letters
    		if(newWord.sameWord(e)) { return(false); }
    		}
    	}
    	return(true);
    }
    private void addWord(Word word)
    {
    	words.push(word); 
	  	if(isNew && (!backwards || isUnique(word,newWords)) ) 
	  	{ 	newWords.push(word); 
	  		if(scoreChange>=0)
	  		{ int pts = scoreWord(word.seed,word.direction);
	  		  word.points = pts;
	  	  	  scoreChange += pts;
	  	  	}
	  	 }
    }
    
    private int validateFrom(CrosswordsCell c)
    {
    	if(c==null || c.sweep_counter>=sweep_counter || (c.topChip()==null)) { return(0); }
    	int n = 1;
    	c.sweep_counter = sweep_counter;
    	c.wordDirections = 0;		// clear here, so markDirections doesn't have to.
    	int directionStep = diagonals ? 1 : CELL_QUARTER_TURN;
    	// don't mess with this, it's correct!
    	int lastDir = backwards ? CELL_FULL_TURN : diagonals ? CELL_FULL_TURN/2 : CELL_RIGHT;
    	// don't mess with this, it's correct!
     	int firstDir = diagonals ? 0 : CELL_LEFT;
    	// don't mess with this, it's correct!
     	for(int dir = firstDir; dir<CELL_FULL_TURN; dir+= directionStep)
    	{	CrosswordsCell next = c.exitTo(dir);
     		CrosswordsChip top = next==null ? null : next.topChip();
    		if(top!=null) { n += validateFrom(next); }
    		else if(dir<lastDir)
    		{ 	int oppDir = dir+CELL_HALF_TURN;
    			String w = collectWord(c,oppDir);
    			if(w!=null) 
    				{ Entry e = dictionary.get(w);		// this deliberately uses the unlimited dictionary
    				  Word word = new Word(c,w,oppDir);
    				  if(e!=null) 
    				  	{ addWord(word);
    				  	}
    				  	else if(backwards)
    					  {
    				  		builder.reverse();
    				  		String rev = builder.toString();
    				  		if(dictionary.get(rev)!=null) {}	// reverse would be ok, we'll score it when it is forward
    				  		else { nonWords.push(word); }
    				  	}
    				  	else { 
    					  nonWords.push(word); 
    					  }

    				  }
    				}
     		}
    	return(n);
    }
    
    private void doDone(replayMode replay)
    {
    	validate(true);
    	score[whoseTurn]+=scoreChange;
    	if(robot==null)
    	{
    		for(int lim=newWords.size()-1; lim>=0; lim--)
    		{	Word w = newWords.elementAt(lim);
    			logGameEvent(AddWordMessage,w.name,""+w.points);
    		}
    	}
    	lastLetters.copyFrom(droppedDestStack);
        acceptPlacement();

        drawNewTiles(replay);
        if(isPass)
        { nPasses++;
          isPass = false;
        }
        else { nPasses=0; }
        
        if (board_state==CrosswordsState.Resign)
        {
            win[nextPlayer()] = true;
    		setState(CrosswordsState.Gameover);
        }
        else if(board_state==CrosswordsState.DiscardTiles)
        {
        	for(CrosswordsCell c : rack[whoseTurn])
        	{	CrosswordsChip top = c.topChip();
        		if(top!=null)
        			{// do this the slow way to keep the bookkeeping straight
        			 pickObject(c);
        			 dropObject(drawPile);
        			  if(replay!=replayMode.Replay)
        			  {
        				  animationStack.push(c);
        				  animationStack.push(drawPile);
        			  }
        			}
        		acceptPlacement();
        	}
        	drawPile.shuffle(new Random(randomKey+moveNumber*100));
        	if(revision>=108)
        		{ drawNewTiles(replay);
        		}
        	else
        	{
        	CrosswordsCell ra[] = rack[whoseTurn];
        	for(int i=revision<105?1:0,max=revision<105?rackSize+1:rackSize;i<max;i++)
        	{  
        	   if(drawPile.height()>0)
        		{
        		CrosswordsCell c = ra[i];        		   
        		c.addChip(drawPile.removeTop());
            		if(replay!=replayMode.Replay && (c!=null))
        		{
        			animationStack.push(drawPile);
        			animationStack.push(c);
        		}
        		}
        	}
        	
        		}
        	setNextPlayer(replay);
    		setNextStateAfterDone(replay);
        }
        else
        {	setNextPlayer(replay);
        	setNextStateAfterDone(replay);
         }
    }
	public boolean notStarted()
	{
		return((droppedDestStack.size()==0) && (pickedSourceStack.size()==0));
	}
	public boolean getOptionValue(Option o)
	{
		switch(o)
		{
		default: throw G.Error("Not expecting %s",o);
		case Backwards: return(backwards);
		case Diagonals: return(diagonals);
		case NoDuplicate: return(noduplicates);
		case Connected: return(allConnected);
		case OpenRacks: return(openRacks);
		}
	}
	public void setOptionValue(Option o,boolean v)
	{
    	switch(o)
    	{
    	default: throw G.Error("Not expecting %s",o);
    	case Diagonals:	diagonals = v; break;
    	case Backwards: backwards = v; break;
    	case Connected: allConnected = v; break;
    	case NoDuplicate: noduplicates = v; break;
    	case OpenRacks: openRacks = v; break;
    	}
	}
	private void placeWord(CrosswordsCell from,CrosswordsCell rack[],String word,int direction,replayMode replay)
	{	CrosswordsCell c = from;
		for(int i=0,lim=word.length(); i<lim; i++,c=c.exitTo(direction))
		{	CrosswordsChip top = c.topChip();
			if(top==null)
			{
			char ch = word.charAt(i);
			CrosswordsCell placeFrom= findChar(rack,ch);
			pickObject(placeFrom);
			if(pickedObject==CrosswordsChip.Blank) {
				pickedObject = CrosswordsChip.assignedBlanks[ch-'a'];
			}
			dropObject(c);
			if(replay!=replayMode.Replay)
				{
				animationStack.push(placeFrom);
				animationStack.push(c);
				}
			}
		}
	}
	public void dropAndSlide(int who,CrosswordsCell from,int moving0,int pick0,int dest0,replayMode replay)
	{	// the map is invalid when we enter, because we've dropped a tile
		// and haven't mapped it yet.
		CrosswordsCell rcells[] = mappedRack[who];
    	int pick = pick0;
    	int dest = dest0;
    	int moving = moving0;
    	int map[] = rackMap[who];
    	{
    	int i=0;
    	int len = map.length;
    	while(pick<0)
    	{
    		if((dest+i<len) && map[dest+i]<0) { pick = dest+i; }
    		else if((dest-i>=0) && map[dest-i]<0) { pick = dest-i; }
    		i++;
    	}}
    	int direction = dest>=pick ? -1 : 1;
    	
    	if(replay==replayMode.Single && from!=null)
		{
			animationStack.push(from);
			animationStack.push(rcells[dest]);
		}
		while(map[dest]!=-1)
		{	if(replay!=replayMode.Replay)
			{
			animationStack.push(rcells[dest]);
			animationStack.push(rcells[dest+direction]);
			}
			int mov = map[dest];
			map[dest] = moving;
			moving = mov;
			pick = dest;
			dest += direction;
		}
		map[dest] = moving;
		validateMap(who);
	}
	
    public boolean Execute(commonMove mm,replayMode replay)
    {	Crosswordsmovespec m = (Crosswordsmovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }
        validateMap(m.player);
        //G.print("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
        case MOVE_DONE:
        	//(replay==replayMode.Live) { G.print(Plog.log.finishLog()); }
         	doDone(replay);

            break;
        case MOVE_PLAYWORD:
        	{
        	CrosswordsCell c = getCell(m.to_col,m.to_row);
        	placeWord(c,rack[whoseTurn],m.word,m.direction,replay);   
        	
        	CrosswordsCell r[] = rack[whoseTurn];
        	int map[] = rackMap[whoseTurn];
        	for(int lim=map.length-1; lim>=0; lim--)
        	{	int idx = map[lim];
        		if((idx>=0) && (r[idx].topChip()==null)) { map[lim]=-1; }
        	}
        	validateMap(whoseTurn);
 
        	setNextStateAfterDrop(null,null,replay);
        	}
        	break;
        case MOVE_PASS:
        	setState((board_state==CrosswordsState.Confirm) ? CrosswordsState.Play : CrosswordsState.Confirm);
        	isPass = board_state==CrosswordsState.Confirm;
        	break;
        case MOVE_SETOPTION:
        	{
        	Option o = Option.getOrd(m.to_row/2);
        	boolean v = ((m.to_row&1)!=0);
        	setOptionValue(o,v);
        	
        	}
        	break;
        case MOVE_SEE:
        	{	// see tiles in the hidden rack on hand held device
        		hiddenVisible[m.to_col-'A'] = (m.to_row==0?false:true);
        	}
        	break;
        case MOVE_SHOW:
            {	
            	openRack[m.to_col-'A'] = (m.to_row==0?false:true);
            }
        	break;
        case MOVE_SELECT:
        	{
        	CrosswordsCell last = lastDropped();
        	G.Assert(last.topChip().isBlank(),"must be a blank");
        	SetBoard(last,null);
        	CrosswordsChip po = CrosswordsChip.assignedBlanks[m.to_col-'A'];
        	SetBoard(last,po);
        	setNextStateAfterDrop(last,po,replay);
        	}
        	break;
        case MOVE_DROPB:
        	{
			CrosswordsCell src = pickedSourceStack.top();
			if(src==null && board_state==CrosswordsState.FirstPlay)
			{
				src = seedLocation;
				pickObject(src);
			}
			CrosswordsChip po = pickedObject;
			CrosswordsCell dest =  getCell(CrosswordsId.BoardLocation,m.to_col,m.to_row);
			if((board_state==CrosswordsState.FirstPlay)||(chipsOnBoard==0)) { seedLocation = dest; }
			
			if(isASource(dest)) 
				{ unPickObject(dest); 
				}
				else 
				{
				m.chip = po;
	            dropObject(dest);
	           
	            /**
	             * if the user clicked on a board space without picking anything up,
	             * animate a stone moving in from the pool.  For Hex, the "picks" are
	             * removed from the game record, so there are never picked stones in
	             * single step replays.
	             */
	            if(replay!=replayMode.Replay && (po==null))
	            	{ animationStack.push(src);
	            	  animationStack.push(dest); 
	            	}
	            setNextStateAfterDrop(dest,po,replay);
				}
        	}
             break;

        case MOVE_REPLACE:
	    	{
	    	int who = m.to_col-'A';
	    	CrosswordsCell rcells[] = mappedRack[who];
	    	int map[] = rackMap[who];
	    	int pick = mapPick[who];
	    	if(pick>=0)
	    	{
	    	int dest = m.to_row;
	    	int moving = map[pick];
	    	map[pick] = -1;
	    	mapPick[who] = -1;
	    	mapTarget[who] = -1;
	    	dropAndSlide(who,rcells[pick],moving,pick,dest,replay);
	    	}}
	    	break;

        case MOVE_REMOTELIFT:
	    	{
	    	int who = m.to_col-'A';
	    	mapTarget[who] = m.mapped_row;
	    	}
	    	break;
            
        case MOVE_LIFT:
        	{
        	int who = m.to_col-'A';
        	mapPick[who] = m.to_row;
        	mapTarget[who] = m.mapped_row;
        	}
        	break;
        case MOVE_MOVETILE:
        	{
           	validateMap(whoseTurn);
        	CrosswordsCell src = getCell(m.source,m.from_col,m.from_row);
        	CrosswordsCell dest = getCell(m.dest,m.to_col,m.to_row);
        	pickObject(src);
        	CrosswordsChip po = m.chip = pickedObject;
        	dropObject(dest);
           	validateMap(whoseTurn);
        	// no animation needed because this is really from 
        	// a pick/drop pair sourced in the rack
            if(replay==replayMode.Single)
        	{ animationStack.push(src);
        	  animationStack.push(dest); 
        	}
 
        	setNextStateAfterDrop(dest,po,replay);

        	}
        	break;
        case MOVE_PICK:		// pick from the draw pile
 		case MOVE_PICKB:	// pick from the board
        	// come here only where there's something to pick, which must
 			{
 			CrosswordsCell src = getCell(m.dest,m.to_col,m.to_row);
 			if(isADest(src)) { unDropObject(src); }
 			else
 			{
        	// be a temporary p
        	pickObject(src);
        	m.chip = pickedObject;
        	switch(board_state)
        	{
        	case Confirm:
        		setState(CrosswordsState.Play);
				//$FALL-THROUGH$
           	case Puzzle:
			case Play:
        		break;
        	default: ;
        	}}}
    		validate(false);
    		break;

 		case MOVE_REMOTEDROP:
	    	{
	    		int who = m.to_col-'A';
		    	mapPick[who] = -1;
		    	mapTarget[who] = -1;   	
	    	}
	    	break;
	    	
        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
            CrosswordsCell dest = getCell(m.dest,m.to_col,m.to_row);
        	if(isASource(dest)) 
        		{ CrosswordsChip po = pickedObject;
        		  unPickObject(dest);
        		  setNextStateAfterDrop(dest,po,replay);
        		}
            else 
            { dropObject(dest); 
            }
	  		  switch(dest.rackLocation())
	  		  {
	  		  default: break;
	  		  case Rack:
	  		  	{
	  		  		int who = m.to_col-'A';
	  		  		int dl = m.to_row; 
	  		  		int slot = m.mapped_row;
	  		  		int map[] = rackMap[who];
	  		  		int mapl = map.length;
  		  			int dx = 0;
	  		  		while((slot<0) && dx<mapl)
	  		  		{	// find an empty slot
	  		  			if((dl+dx<mapl) && (map[dl+dx]<0)) { slot = dl+dx; }
	  		  			else if((dl-dx>=0) && (map[dl]<0)) { slot = dl-dx; }
	  		  			dx++;
	  		  		}
	  		  		dropAndSlide(who, pickedSourceStack.top(),dl,-1,slot,replay);
	  		  	}
	  		  }

        	}
        	else { 
        		G.print("empty drop "+m);
        		m.op = MOVE_CANCELLED;
        	}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(CrosswordsState.Puzzle);	// standardize the current state
            setNextStateAfterDone(replay);

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?CrosswordsState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(CrosswordsState.Puzzle);
 
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(CrosswordsState.Gameover);
    	   break;

       case MOVE_DROPONRACK:
    	   // remote screens drop a tile on the rack.
    	   // we do nothing now, but this gets transmitted back to the main.
    	   break;
        default:
        	cantExecute(m);
        }
        validateMap(m.player);

        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }
        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }
    public boolean LegalToHitPool(boolean picked)
    {
    	switch(board_state)
    	{
    	default:
    		return(false);
    	case DiscardTiles:
    	case Confirm:
    	case Play:
    		return( picked 
    					? (droppedDestStack.size()==0)
    					: (lastDropped()==drawPile));
    	case Puzzle:
    		return(!picked ? drawPile.height()>0 : true);
    	}
    }
    // legal to hit the chip storage area
    public boolean LegalToHitChips(CrosswordsCell c)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting Legal Hit state " + board_state);
        case FirstPlay: 
        	return(false); 
        case Confirm:
        case ConfirmFirstPlay:
        case Play:
        	// for pushfight, you can pick up a stone in the storage area
        	// but it's really optional
        	return ( (c!=null) 
        			 && ((pickedObject==null) ? (c.topChip()!=null) : true)
        			);
 		case ResolveBlank:
 		case Resign:
		case Gameover:
		case DiscardTiles:
			return(false);
        case Puzzle:
            return ((c!=null) && ((pickedObject==null) == (c.topChip()!=null)));
        }
    }

    public boolean LegalToHitBoard(CrosswordsCell c,boolean picked)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
        case ConfirmFirstPlay:
        case FirstPlay:
        	return(pickedObject!=null
        			? pickedObject!=null && (pickedSourceStack.top().onBoard && c.topChip()==null)
        			: c.topChip()!=null);
		case Confirm:
		case Play:
			return(picked ? c.isEmpty() : isADest(c));

		case ResolveBlank:
			return(c==droppedDestStack.top());

		case Gameover:
		case Resign:
		case DiscardTiles:
			return(false);
        default:
        	throw G.Error("Not expecting Hit Board state " + board_state);
        case Puzzle:
            return (picked == (c.topChip()==null));
        }
    }
    public void validateMap(int forplayer)
    {		int used = 0;
    		int mapped = 0;
    		int nmapped = 0;
    		CrosswordsCell prack[] = rack[forplayer];
    		for(CrosswordsCell c : prack)
    		{
    			if(c.topChip()!=null) { used++; }
    		}
    		int mapi[] = rackMap[forplayer];
    		for(int i=0;i<mapi.length;i++)
    		{	int idx = mapi[i];
    			if(idx>=0) { 
    				int bit = 1<<idx;
    				if((bit&mapped)!=0) { G.Error("idx "+idx+" is mapped twice"); }
    				else if(prack[idx].topChip()!=null)
    					{ nmapped++; mapped|=bit; 
    					}
    				else { G.Error("index "+idx+" maps to empty", idx); }
    			}
    		}
    		G.Assert(nmapped==used,"map not complete");
    		
    		}

    
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Crosswordsmovespec m)
    {
        robotState.push(board_state); //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
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
    public void UnExecute(Crosswordsmovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	CrosswordsState state = robotState.pop();
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
 // true if we could place the word as specified.  Any cells already occupied
 // must match letters from the word.  Its assumed the letters in the word 
 // are available from the rack. Note that the -1 and n+1 letters of the word
 // are checked to be empty.  Words on other directions that may also being
 // formed are NOT checked here.  From should be a cell that contains letter of the word at firstIndex
 // return the cell that would be the head of the new word
 // this is used both the place ordinary crosswords (where one letter is already placed)
 // and "cap words" where we are placing a crossword at the beginning or end of an existnig word
 private CrosswordsCell canPlaceWord(String word,int firstIndex,CrosswordsCell from,int direction)
 {	int reverse = direction+CELL_HALF_TURN;
 	CrosswordsCell head = from;
 	int usedLetters = 0;
 	// check the previous letters for space
 	{
 	 	CrosswordsCell c = from.exitTo(reverse);
 	 	for(int idx = firstIndex-1; idx>=0; idx--)
 	 	{	if(c==null) { return(null); }		// can't fall off the edge
 		 	CrosswordsChip ch = c.topChip();
 		 	if(ch==null) { usedLetters++; }
 		 	else if(ch.lcChar!=word.charAt(idx)) { return(null); }
 			head = c;	// this might be the head cell of the word
 			c = c.exitTo(reverse);
 	 	}
 	 	// must be an empty space previous to the beginning
 	 	if((c!=null) && (c.topChip()!=null)) { return(null); }
 	}
 	
 	// check the rest of the word for space
 	{
 	CrosswordsCell c = from.exitTo(direction);
 	for(int idx = firstIndex+1,lastIdx = word.length(); idx<lastIdx; idx++)
 	{	
 		if(c==null) { return(null); }		// can't fall off the edge
	 	CrosswordsChip ch = c.topChip();
	 	if(ch==null) { usedLetters++; }
	 	else if(ch.lcChar!=word.charAt(idx)) { return(null); }
		c = c.exitTo(direction);
 	}
 	// word matches, c is either the next position or null if we reached
 	// the edge of the board
 	if(usedLetters==0) { return(null); }	// we didn't use any of our own letters
 	if((c==null) || (c.topChip()==null)) { return(head); }	// need a empty space next
 	return(null);
 	}
 }
 
 private int findLetter(CrosswordsCell fromRack[],char ch,boolean blank)
 {
	 for (int lim = fromRack.length-1; lim>=0; lim--)
	 {
		 CrosswordsCell c = fromRack[lim];
		 if(c.sweep_counter!=sweep_counter)
		 {	CrosswordsChip top = c.topChip();
		 	if(top!=null && top.lcChar==ch)
		 	{
		 		if(blank == c.isBlank) { return(lim); }
		 	}
		 }	 
	 }
	 return(-1);
 }
 // we have already verified the placement to be legal, now score the
 // word considering the new letters we will place.
 // this does not check for illegal crosswords
 // one complication is that the search for words using blanks
 // involves "temporarily" placing the actual letter in the rack
 private int scoreWord(String word,CrosswordsCell from,int direction,CrosswordsCell fromRack[])
 {	
	int score = 0;
	sweep_counter++;
	wordMultiplier = 1;
	AR.setValue(wordBonus, null);
	CrosswordsCell c = from;
 	for(int idx = 0,lastIdx = word.length(); idx<lastIdx; idx++)
 	{	
	 	CrosswordsChip ch = c.topChip();
	 	if(ch==null)
	 	{	// we'll place this letter, so it scores
	 		char letter = word.charAt(idx);
	 		int rackidx = findLetter(fromRack,letter,false);	// find as a real letter
	 		if(rackidx>=0)
	 			{
	 			
	 		score += scoreLetter(c,CrosswordsChip.getLetter(letter));
	 			fromRack[rackidx].sweep_counter = sweep_counter;	// mark it used
	 			}
	 		else {
	 			int blankIdx = findLetter(fromRack,letter,true);
	 			G.Assert(blankIdx>=0,"blank letter not found");
	 			fromRack[blankIdx].sweep_counter = sweep_counter;
	 		}
	 	}
	 	else { 
	 		score += ch.value;
	 	}
		c = c.exitTo(direction);
 	}
 	
 	return(score*wordMultiplier);
 }
 
 // true if this word can be placed from the rack.  This is called
 // if the set of letters is plausible, but there might be more duplicates
 // in the targetWord than there are in the rack
 private boolean canPlaceFromRack(String targetWord,CrosswordsCell rack[],char targetLetter)
 {	char seedLetter = targetLetter;
 	sweep_counter++;
 	for(int lim=targetWord.length()-1; lim>=0; lim--)
 	{
 		char wordLetter = targetWord.charAt(lim);
 		if(wordLetter==seedLetter) { seedLetter=(char)0; }
 		else
 		{	boolean found = false;
 			for(CrosswordsCell rackCell : rack)
 			{
 				CrosswordsChip ch = rackCell.topChip();
 				if((ch!=null) && (rackCell.sweep_counter!=sweep_counter) && (ch.lcChar==wordLetter))
 				{	rackCell.sweep_counter=sweep_counter;
 					found = true;
 					break;
 				}
 			}
 			if(!found) { return(false); }
 		}
 	}
	return(true);
 }
 private int findLetter(String targetString,char wordLetter,int usedLetters)
 {
	 int used = 1;
	 for(int lim=targetString.length()-1; lim>=0; lim--)
	 {
		 if(((used & usedLetters)==0) && (targetString.charAt(lim)==wordLetter)) { return(used); }
		 used = used<<1;
	 }
	 return(0);
 }
 private boolean canPlaceFromRack(String targetWord,CrosswordsCell rack[],String targetString)
 {	int usedLetters = 0;
 	sweep_counter++;
 	for(int lim=targetWord.length()-1; lim>=0; lim--)
 	{
 		char wordLetter = targetWord.charAt(lim);
 		int newletter = findLetter(targetString,wordLetter,usedLetters);
 		if(newletter>0) { usedLetters |= newletter; }
 		else
 		{	boolean found = false;
 			for(CrosswordsCell rackCell : rack)
 			{
 				CrosswordsChip ch = rackCell.topChip();
 				if((ch!=null) && (rackCell.sweep_counter!=sweep_counter) && (ch.lcChar==wordLetter))
 				{	rackCell.sweep_counter=sweep_counter;
 					found = true;
 					break;
 				}
 			}
 			if(!found) { return(false); }
 		}
 	}
	return(true);
 }
 // find words that can be placed in a different direction across an existing word.  This is used
 // both to place ordinary crosswords and to place "cap words" where a letter is being added to 
 // the beginning or end of an existing word.
 public int checkCrossWords(DictionaryHash subDictionary,CrosswordsCell fromRack[],long letterMask,
		 		CrosswordsCell startingAt,char targetLetter,
		 		int notInDirection,int inDirection)
 {	
 	int directionStep = diagonals ? 1 : CELL_QUARTER_TURN;
 	int lastDir = inDirection>=0 ? inDirection+1 : backwards ? CELL_FULL_TURN : diagonals ? CELL_FULL_TURN/2 : CELL_FULL_TURN;
 	int firstDir = inDirection>=0 ? inDirection : diagonals ? 0 : backwards ? CELL_LEFT : CELL_RIGHT;
 	int total = 0;
 	for(Enumeration<Entry> words = subDictionary.elements(); words.hasMoreElements();)
 		{	Entry word = words.nextElement();
 			String targetWord = word.word;
 			// first a quick check that the word doesn't contain any letters that aren't available
 			// this ought to filter out the vast majority of words
 			if(word.order<robotVocabulary								// within the vocabulary limit 
 					&& ((word.letterMask | letterMask)==letterMask))	// if the word doesn't require any letters not in the rack
 			{
			 int targetIndex = targetWord.indexOf(targetLetter);
			 while(targetIndex>=0)
			 {
			 for(int direction = firstDir; direction<lastDir; direction+=directionStep )
			 {	 if(direction!=notInDirection)
			 	{
				 if(canPlaceFromRack(targetWord,fromRack,targetLetter))
			 	 {
				 CrosswordsCell head = canPlaceWord(targetWord,targetIndex,startingAt,direction);
				 if(head!=null)
				 {
				 int value = scoreWord(targetWord,head,direction,fromRack);
			     Word newword = candidateWords.recordCandidate(notInDirection<0?"Ordinary Crossword":"Cap word",
			    		 				head, targetWord, direction,value,word);
			     if(newword!=null)
			     {
			    	 int extra = checkIllegalCrosswords(targetWord,targetIndex,head,direction,fromRack);
			    	 if(extra<0) { candidateWords.unAccept(newword); }
			    	 else if(extra>0)
			    	 	{ newword.points += extra;
			    	 	 // G.print("extra crosswords ",extra," for ",targetWord);
			    	 	}
			     }
				 total++;
				 }}}
			 }
			 targetIndex = targetWord.indexOf(targetLetter,targetIndex+1);
			 }
 			}
 		}
	 return(total);
 }
 // find words that can be placed in a different direction across an existing word.  This is used
 // both to place ordinary crosswords and to place "cap words" where a letter is being added to 
 // the beginning or end of an existing word.
 public int checkExtensionWords(DictionaryHash subDictionary,CrosswordsCell fromRack[],long letterMask, Word seed)
 {	int total = 0;
 	int direction = seed.direction;
 	for(Enumeration<Entry> words = subDictionary.elements(); words.hasMoreElements();)
 	{	Entry word = words.nextElement();
 		String targetWord = word.word;
 		// first a quick check that the word doesn't contain any letters that aren't available
 		// this ought to filter out the vast majority of words
 		if(word.order<robotVocabulary								// within the vocabulary limit 
 				&& ((word.letterMask | letterMask)==letterMask)	// if the word doesn't require any letters not in the rack
 				)		// can actually be placed
 		{	int position = targetWord.indexOf(seed.name);
 			if(position>=0 && canPlaceFromRack(targetWord,fromRack,seed.name))
 				{CrosswordsCell head = canPlaceWord(targetWord,position,seed.seed,direction);
 				if(head!=null)
 				{
 				int value = scoreWord(targetWord,head,direction,fromRack);
			    Word newword = candidateWords.recordCandidate("Extension Word",head, targetWord, direction,value,word);
			    if(newword!=null)
			     {
			    	 int extra = checkIllegalCrosswords(targetWord,position,head,direction,fromRack);
			    	 if(extra<0) { candidateWords.unAccept(newword); }
			    	 else if(extra>0)
			    	 	{ newword.points += extra;
			    	 	  //G.print("extra crosswords ",extra," for ",targetWord);
			    	 	}
			     }
				total++;
 				}
 		}}}
	 return(total);
 }
 // check crosswords against a particular dictionary
 // which will be one of the word length subdictionaryies
 // which are tried starting with the shortest words.
 private int checkCrossWords(DictionaryHash subDictionary,CellStack fromPlaces,CrosswordsCell rack[],long letterMask)
 {	
 	int total = 0;
 	for(int lim=fromPlaces.size()-1; lim>=0; lim--)
 	{
 		CrosswordsCell seed = fromPlaces.elementAt(lim);
 		CrosswordsChip top = seed.topChip();
 		char targetLetter = top.lcChar;
 		total += checkCrossWords(subDictionary,rack,Dictionary.letterMask(letterMask,targetLetter),seed,targetLetter,-1,-1); 
 	}
 	return(total);
 }
 
 // find a blank in the rack
 private int blankIndex(CrosswordsCell rack[],int from)
 {
	 for(int i=from,lim=rack.length; i<lim; i++)
	 {
		 CrosswordsCell c = rack[i];
		 if(c.topChip()==CrosswordsChip.Blank) { return(i); } 
	 }
	 return(-1);
 }
 private CrosswordsCell findChar(CrosswordsCell rack[],char ch)
 {	CrosswordsCell blank = null;
	for(int lim=rack.length-1; lim>=0; lim--)
	{	CrosswordsCell c = rack[lim];
		CrosswordsChip chip = c.topChip();
		if((chip!=null) && (chip.lcChar==ch)) { return(c); }
		if(chip==CrosswordsChip.Blank) { blank = c; } 
	}
	return(blank);
 }

 // this generates a mask in the same manner as Dictionary.letterSet, but
 // uses a rack as the source
 private long letterSet(CrosswordsCell rack[])
	{	long s = 0;
		for(CrosswordsCell c : rack)
		{
			CrosswordsChip ch = c.topChip();
			if(ch!=null)
			{	char letter = ch.lcChar;
				s = Dictionary.letterMask(s,letter);
			}
		}
		return(s);
	}

 //
 // check for words that can be made with rack. This finds all simple crosswords
 // that don't join another word, and a few "double tower" crosswords where
 // the word extends to intersect another word.  To final all, it would 
 // have to include the additional letters in the rack mask, and it would
 // have to add one to the max size word for each additional intercept.
 // 
 private void checkCrossWords(CellStack fromPlaces,CrosswordsCell rack[],long letterMask,double baseProgress,double progressFraction)
 {	int totalsize = dictionary.size();
 	int usedSize = 0;
	for(int wordlen=2;wordlen<=rackSize+1;wordlen++)
 	{	DictionaryHash sub = dictionary.getSubdictionary(wordlen);
 		checkCrossWords(sub,fromPlaces,rack,letterMask);
 		usedSize += sub.size();
 		recordProgress(baseProgress+usedSize*progressFraction/totalsize);
 	}
 }
 
 
 // look for a single letter from the rack added to the beginning or end of a word,
 // then add complete crosswords from the rack using that letter.
 private int checkCapWords(CrosswordsCell wordHead,CrosswordsCell rack[],long letterMask,boolean atStart)
 {	int total = 0;
 	for(int lim=wordHead.wordHead.size()-1; lim>=0; lim--)
	 {
		 Word w = wordHead.wordHead.elementAt(lim);
		 String name = w.name;
		 for(CrosswordsCell r : rack)
		 {
			 CrosswordsChip top = r.topChip();
			 if(top!=null)
			 {
				 String newWord = atStart ? top.lcChar+name : name+top.lcChar;
				 Entry e = lookupRobotWord(newWord);
				 if(e != null) 
				 {	
					 for(int len=2; len<rackSize;len++)
					 {
						 DictionaryHash subDict = dictionary.getSubdictionary(len);
						 total += checkCrossWords(subDict,rack,letterMask,wordHead,(char)0,w.direction,-1);
					 }
				 }
			 }
		 }
	 }
 	return(total);
 }
 
 // look for adding multiple letters to the beginning or end of a word
 private int checkExtensionWords(Word w,CrosswordsCell rack[],long letterMask,boolean atStart)
 {	int total = 0;
 	String name = w.name;
 	Entry e = lookupRobotWord(name);
 	if(e != null) 
		 {	int namelen = name.length();
		    for(int len= namelen+2,max = Math.min(Dictionary.MAXLEN, namelen+rackSize); len<=max;len++)
		    {
		    	DictionaryHash subDict = dictionary.getSubdictionary(len);
		    	total += checkExtensionWords(subDict,rack,letterMask|e.letterMask,w);
		    }
		 }
 	return(total);
 }

 // check words formed by adding a top or bottom letter to an existing word
 private void checkCapWords(CellStack fromPlaces,CrosswordsCell rack[],long letterMask,boolean atStart)
 {	
	 for(int lim=fromPlaces.size()-1; lim>=0; lim--)
	 {
		 checkCapWords(fromPlaces.elementAt(lim),rack,letterMask,atStart);
	 }
 }
 
 // look for adding multiple letters to the beginning or end of a word
 private void checkExtensionWords(WordStack words,CrosswordsCell rack[],long letterMask,boolean atStart)
 {	
 	for(int lim=words.size()-1; lim>=0; lim--)
	 {
		 checkExtensionWords(words.elementAt(lim),rack,letterMask,atStart);
	 }
 }
 private void checkWords(CrosswordsCell rack[],double baseProgress,double progressScale)
 {	
	 int bi = blankIndex(rack,0);
	 if(bi>=0)
	 {	// replace the blank with each possible letter, using the
		// blank values so the score is unaffected by the replacement letter.
		// this multiplies the search by 26x for each blank
		 double quant = progressScale/CrosswordsChip.assignedBlanks.length;
		for(CrosswordsChip letter : CrosswordsChip.assignedBlanks)
		 {
			 rack[bi].removeTop();
			 rack[bi].addChip(letter);
			 rack[bi].isBlank = true;
			 checkWords(rack,baseProgress,quant);
			 rack[bi].isBlank = false;
			 baseProgress += quant;
			 // time for normal search or one blank is pretty good in all cases,
			 // but with multiple blanks it can be excessive, especially on 
			 // mobiles.  This provides an emergency exit.  The rest of the
			 // search strategy leaves a partially complete search with
			 // the best value so far.
			 if((robot!=null) && robot.timeExceeded()) 
			 	{ G.print("Time exceeded");
			 	  return; 
			 	}
		 }
		 rack[bi].removeTop();
		 rack[bi].addChip(CrosswordsChip.Blank);
	 }
	 else {
		 
	 // check ordinary crosswords
     long letterMask = letterSet(rack);
	 checkCrossWords(occupiedCells,rack,letterMask,baseProgress,0.5*progressScale);		// ordinary crosswords
	 recordProgress(baseProgress+0.5*progressScale);
	 
	 // cap words are single letters added to the beginning or end of an existing word
	 checkCapWords(startCaps,rack,letterMask,true);		// add a new prefix letter + a crossword
	 recordProgress(baseProgress+0.6*progressScale);
	 checkCapWords(endCaps,rack,letterMask,false);			// add a new final letter + a crossword
	 recordProgress(baseProgress+0.7*progressScale);
	 
	 // extension words are multiple letters added to an existing word to make a longer word
	 checkExtensionWords(words,rack,letterMask,true);		// add multiple letters to the beginning or end
	 }
 }
 private void checkWordsCore()
 {	candidateWords.clear();
 	for(CrosswordsCell c : rack[whoseTurn]) { c.isBlank = false; }	// just to be sure
 	checkWords(rack[whoseTurn],0,1);
 	candidateWords.sort(true);
 }
 
 /**
  * do a word search for the user interface review mode
  * @return
  */
 public WordStack checkLikelyWords()
 {	checkWordsCore();
 	return(candidateWords);
	 
 }
 /**
  * the basic algorithm is to check words in the dictionary to see if they
  * can be placed on the board.
  */
 public void checkWords()
 {	checkWordsCore();
 	for(int i=0;i<candidateWords.size();i++)
 	{	G.print(candidateWords.elementAt(i));
 	}
 	G.print("accepted ",candidateWords.accepted," declined ",candidateWords.declined);
 }
 private double distanceFromBonuses(CrosswordsCell c)
 {	double score = 0;
 	for(CrosswordsCell b : bonusCells)
 	{	double distance = Math.abs(c.col-b.col)+(Math.abs(c.row-b.row));
 		score += distance* b.topChip().value;
 	}
 	return(score);
 }
/**
 * find the most distant cell from bonuses, weighted for their likely power,
 * but don't put the first tile on the edge.
 * @return
 */
 private CrosswordsCell mostDistantFromBonuses()
 {
	 double bestv=0;
	 CrosswordsCell best = null;
	 int size = variation.boardsize;
	 for(CrosswordsCell c =allCells; c!=null; c=c.next)
	 {	if((c.row>2) 
			 && (c.row<size-2) 
			 && (c.col>'A'+2)
			 && (c.col<('A'+size-2)))
	 	{
		 double score = distanceFromBonuses(c);
		 if(score>bestv) { bestv = score; best=c; }
	 	}
	 }
	 return(best);
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
    recordProgress(0);
    validate(false);
    switch(board_state)
    {
    default: throw G.Error("Not expecting ", board_state);
    case Confirm:
    case ConfirmFirstPlay:
    	all.push(new Crosswordsmovespec(MOVE_DONE,whoseTurn));
    	break;
    case FirstPlay:
    	{
    	// look for a good place for someone else to place the first word.
    	CrosswordsCell best = mostDistantFromBonuses();
    	all.push(new Crosswordsmovespec(MOVE_DROPB,best.col,best.row,CrosswordsId.BoardLocation,whoseTurn));
    	}
    	break;
    case Play:
 	checkWords();
 	for(int i=0;i<candidateWords.size();i++)
 	{
 		all.push(new Crosswordsmovespec(candidateWords.elementAt(i),whoseTurn));
 	}
     	if(all.size()==0)
     	{
     		all.push(new Crosswordsmovespec(MOVE_PASS,whoseTurn));
     	}
     	break;
    }
 	recordProgress(0);
 	return(all);
 }
 CrosswordsPlay robot = null;
 private void recordProgress(double v)
 {
	 if(robot!=null) { robot.setProgress(v); }
 }
 public void initRobotValues(CrosswordsPlay rob,int vocab)
 {	robot = rob;
 	robotVocabulary = vocab;
 }


}
