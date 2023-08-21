package wyps;

import static wyps.Wypsmovespec.*;

import bridge.Color;
import java.util.*;

import dictionary.Dictionary;
import dictionary.DictionaryHash;
import dictionary.Entry;
import lib.*;
import lib.Random;
import online.game.*;
import online.game.cell.Geometry;

/**
 * Initial work October 2020
 *  
 * @author ddyer
 *
 */

// template is an ordered list of cells on the board, some of which may be are occupied by a letter.
// we search for words that are compatible with the template, based on word length and the set of
// letters in the combined rack+template
class Template
{	// constructor
	public Template(WypsCell c[]) { cells = c; }
	public Template(WypsCell c) { cells = new WypsCell[1]; cells[0]=c; }
	public int size() { return(cells.length); }
	WypsCell cells[] = null;
	// the contract for flipCell is that it is null unless this is a template
	// that flips just one cell.  Otherwise, the template is required to result
	// in all cells of the player's color.
	WypsCell flipCell = null;
	double score = 0;			// score when this template is applied to the board
	int newOnBoard = 0;			// number of new cells of color on board
	int newOffBoard = 0;		// number of the other color removed from the board
	public int firstRow() { return(cells==null?0:cells[0].row); };
	public char firstCol() { return(cells==null ? '@' : cells[0].col); }
	
	public String letters()
	{	// return the template letters, upper case for existing letters that will flip
		StringBuilder b = new StringBuilder();
		boolean linear = isLinear();
		for(int i=0;i<cells.length;i++)
			{ WypsCell c = cells[i];
			  WypsChip top = c.topChip(); 
			  if(top==null) { b.append('.'); } 
			  	else 
			  		{ char ch = top.lcChar;
			  		  if(linear || (c==flipCell)) { ch -= 'a'-'A'; }
			  		  b.append(ch);
			  		  }
			}
		return(b.toString());
	}
	public String toString() { 
		return("<template "+firstCol()+firstRow()+" "+letters()+">");
	}
	// 
	// direction maps are a string of decimal digits, left to right,
	// indicating the step direction for the next letter in the template
	// 
	public int directionMap()
	{	int map = 0;
		WypsCell prev = cells[0];
		int ndir = prev.geometry.n;
		for(int i=1;i<cells.length;i++)
		{
			WypsCell next = cells[i];
			for(int dir=0;dir<ndir;dir++)
			{
				if(prev.exitTo(dir)==next) { map = map*10+dir; break; }
			}
			prev = next;
		}
		return(map);
	}
	//
	// extend this template by 1 letter in the given direction
	// return null if it's off board or already in the template
	//
	public Template extendTemplate(int direction)
	{	int len = cells.length;
		WypsCell last = cells[len-1];
		WypsCell next = last.exitTo(direction);
		if(next!=null && !G.arrayContains(cells,next))
		{
			WypsCell newCells[] = new WypsCell[len+1];
			for(int i=0;i<len;i++) { newCells[i]=cells[i]; }
			newCells[len]=next;
			return(new Template(newCells));
		}
		return(null);
	}
	
	// prepend this template by 1 letter in the given direction, thus
	// making a template with a new first letter.  Return null if the
	// prepended cell is off board or already in the template
	public Template prependTemplate(int direction)
	{	int len = cells.length;
		WypsCell last = cells[0];
		WypsCell next = last.exitTo(direction);
		if(next!=null && !G.arrayContains(cells,next))
		{
			WypsCell newCells[] = new WypsCell[len+1];
			newCells[0]=next;
			for(int i=1;i<=len;i++) { newCells[i]=cells[i-1]; }
			return(new Template(newCells));
		}
		return(null);
	}
	// special case of making a template, used to make initial templates.
	// this makes a template from cell "seed" in the given direction 
	// it's an error if the template isn't all contained in the board
	public static Template makeTemplate(WypsCell seed,int direction,int size)
	 {
		 WypsCell temp[] = new WypsCell[size];
		 WypsCell from = seed;
		 int idx = 0;
		 while(idx<size) 
		 	{ G.Assert(from!=null, "must be a cell");
		 	  temp[idx++]=from;
		 	  from=from.exitTo(direction); 
		 	}
		 return(new Template(temp));
	 }
	// 
	// starting from the given template, entend randomly by up
	// to N steps.  prepend/append and direction of each step 
	// are random, and steps that don't work are ignored.
	//
	public static Template extendTemplate(Template seed, Random r,int nsteps)
	{	int n = 0;
		Template from = seed;
		for(int i=0;i<nsteps;i++)
		{	int dir = r.nextInt(Geometry.Hex.n);
			boolean pre = r.nextInt(2)==1;
			Template next = pre? from.prependTemplate(dir) : from.extendTemplate(dir);
			if(next!=null) { n++; from = next; }
		}
		if(n>0) { return(from); }
		return(null);
	}
	
	// count the cells with contents having color
	public int nOfColor(WypsColor wypsColor) {
		int nc = 0;
		for(WypsCell c : cells)
			{ WypsChip top = c.topChip(); if(top!=null && top.getColor()==wypsColor) { nc++; }
			}
		return(nc);
	}
	public int nFilled()
	{	int nc = 0;
		for(WypsCell c : cells)
			{ WypsChip top = c.topChip(); if(top!=null) { nc++; }
			}
		return(nc);		
	}
	// set the nth (0 origin) chip of color as flippable
	private void setFlip(WypsColor wypsColor,int n) {
		int nc = 0;
		flipCell = null;
		for(WypsCell c : cells)
			{ WypsChip top = c.topChip(); if(top!=null && top.getColor()!=wypsColor)
				{ 
				  if(nc==n) { flipCell = c; break; }
				  nc++;
				}
			}
		G.Assert(flipCell!=null,"should have flipped");
	}

	public boolean isLinear()
	{
		int len = cells.length;
		if(len<=2) { return(true); }
		WypsCell cell0 = cells[0];
		WypsCell cell1 = cells[1];
		int dir = cell0.findDirectionToAdjacent(cell1);
		for(int i=2;i<len;i++)
		{	WypsCell cell2 = cells[i];
			if(cell1.exitTo(dir)!=cell2) { return(false); }
			cell1 = cell2;
		}
		return(true);
	}
	 // revert to the reference color values
	 void revertScoreTemplate()
	    {
	    	for(WypsCell c : cells) { c.scoreColor = c.refScoreColor; } 
	    }
	 // apply a color as a temporary overlay
	 void applyScoreTemplate(WypsColor color)
	    {
	    	if(flipCell!=null)
	    	{	// flip empty cells and the flipcell
	    		for(WypsCell c : cells) { if (c.topChip()==null) { c.scoreColor = color; }
	    		flipCell.scoreColor = color;
	    		}
	    	}
	    	else
	    	{	// flip all cells to color
	    		for(WypsCell c : cells) { c.scoreColor = color; } 
	    	}
	    	
	    }
	 // set up for flipping opponent tiles using this template. 
	 // if its a linear template, will flip everything.  If
	 // not, choose a random oppoenent tile to flip.  This
	 // makes the tile-to-flip fixed for this template, which
	 // is important to how the template is scored.
	 public void setFlip(Random r,WypsColor myColor)
	 {	
	 	boolean linear = isLinear();
	 	int mc = nOfColor(myColor);		// number of desired color letter
	 	int nfil = nFilled();				// number of any filled letter
	 	int nc = nfil-mc;					// number of the other color letter
	 	int flippable = linear ? 0 : nc;
	 	if(flippable>0)
			{
				int ran = r.nextInt(flippable);
				setFlip(myColor,ran);
				newOnBoard = cells.length-mc-nc+1;
				newOffBoard = 1;
			}
	 	else 
	 		{
	 		newOnBoard = cells.length-mc;
	 		newOffBoard = mc;
	 		}
	 }
}
class TemplateStack extends OStack<Template>
{
	public Template[] newComponentArray(int sz) { return(new Template[sz]);	}
}
/*
 * in addition to the standard stack, this class has some features
 * to assist collecting plausible moves.
 */
class WordStack extends OStack<Word>
{
	public Word[] newComponentArray(int sz) {
		return new Word[sz];
	}	
	public double bestScore = 0;				// best score so far
	public double leastScore = 0;				// the lowest score currently in the stack
	
	public static final double trimSize = 0.8;			// size to trim to
	public static final double threshold = 0.001;			// threshold from current best to allow addition
	public static int sizeLimit = 10;					// max entries
	int accepted = 0;
	int declined = 0;
	double prevLeastScore = 0;
	double prevBestScore = 0;
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
	public boolean acceptScore(double score)
	{
		return (size()==0 || (score>=bestScore*threshold && score>leastScore));
	}
	public void setScoreTheshold(double v)
	{
		bestScore = leastScore = v;
	}
	// record a candidate word if it is a plausible candidate.
	// trim the active list to the prescribed size
	public Word recordCandidate(String message,Template c,Entry e)
	{	double score = c.score;
		boolean first = (size()==0);
		if(first) 
			{
			Word w = new Word(c,e.word);
			push(w);
			w.entry = e;
			w.points = score;
			w.comment = message;
			bestScore = score;
			leastScore = prevBestScore = bestScore;
			accepted++;
			return(w);
			}
		else if(acceptScore(score))
		{
		trimToSize();
		Word w = new Word(c,e.word);
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
	double points;
	Template seed;	// starting point
	String comment = null;	// for the word search
	Entry entry;
	public String toString() 
	{ StringBuilder b = new StringBuilder();
	  b.append("<word ");
	  b.append(name);
	  b.append(" ");
	  b.append(seed.firstCol());
	  b.append(seed.firstRow());
	  if(comment!=null)
	  {	  b.append(" ");
		  b.append(comment);
		  if(entry!=null)
		  {	b.append(" Order:");
		    b.append(entry.order);
		  }
	  }
	  b.append(">");
	  return(b.toString());
	}

	public Word(Template s, String n)
	{
		seed = s;
		name = n;
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
		return -compareTo(o);
	}

}

class WypsBoard extends hexBoard<WypsCell> implements BoardProtocol,WypsConstants
{	static int REVISION = 104;			// 100 represents the initial version of the game
										// revision 101 reduces the size of the rack to actual size and adds rack maps
										// revision 102 makes handSize a variable
										// revision 103 makes rack size same as handsize, not rack size
										// revision 104 changes the way draw tiles works when the draw pile is empty
	public int getMaxRevisionLevel() { return(REVISION); }
	static DictionaryHash privateDictionary = new DictionaryHash(1); 
	static final int MAX_PLAYERS = 2;
	int rackSize = 7;		// the number of filled slots in the rack.  Actual rack has some empty slots
	int handSize = 7;
	int sweep_counter = 0;
	public Entry previousWord = null;
	WypsVariation variation = WypsVariation.Wyps;
	private WypsState board_state = WypsState.Puzzle;	
	private WypsState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	public int robotVocabulary = 999999;		//	size of the robot's vocabulary
	private boolean isPass = false;
	private int nPasses = 0;
	public WypsState getState() { return(board_state); }
    StringStack gameEvents = new StringStack();
    InternationalStrings s = G.getTranslations();
    
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
	public void setState(WypsState st) 
	{ 	unresign = (st==WypsState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

	private WypsCell rack[][] = null;
	private WypsCell mappedRack[][] = null;

	public WypsCell[] getPlayerRack(int n)
	{
		if(rack!=null && rack.length>n) { return(rack[n]); }
		return(null);
	}
	public WypsCell[] getPlayerMappedRack(int n)
	{
		if(mappedRack!=null && mappedRack.length>n) { return(mappedRack[n]); }
		return(null);
	
	}

	WypsColor playerColor[] = { WypsColor.Light, WypsColor.Dark };
	private int playerIndex(WypsColor c)
	{
		return(c==playerColor[0] ? 0 : 1);
	}
	
	private int mapPick[] = null;		// which cell in the rack is picked (per player)
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

	int rackMap[][] = null;
	WypsCell drawPile = null;
	int fullBoardSize = 0;
	int chipsOnBoard = 0;
	boolean openRacks = true;			// show all players racks
	boolean openRack[];					// per player open rack
	Dictionary dictionary ;				// the current dictionary
	CellStack occupiedCells = new CellStack();
	CellStack emptyCells = new CellStack();
	int nOnBoard[] = new int[2];
	
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
    public WypsChip pickedObject = null;
    public WypsChip lastPicked = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private CellStack candidateWord = new CellStack();
    private CellStack selectedStack = new CellStack();
    private StateStack stateStack = new StateStack();
    public CellStack lastLetters = new CellStack();
    public WypsState resetState = WypsState.Puzzle; 
    public WypsChip lastDroppedObject = null;	// for image adjustment logic

    public boolean isSelected(WypsCell c)
    {
    	return(selectedStack.contains(c));
    }
    
	// factory method to generate a board cell
	public WypsCell newcell(char c,int r)
	{	return(new WypsCell(WypsId.BoardLocation,c,r,geometry()));
	}
	
	
	// constructor 
    public WypsBoard(String init,int players,long key,int map[],Dictionary di,int rev) // default constructor
    {
        dictionary = di;
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = WypsGRIDSTYLE;
        setColorMap(map, players);
    	// allocate the rack map once and for all, it's not used in the board
    	// only as part of the UI.
       	mapPick = new int[MAX_PLAYERS];
       	mapTarget = new int[MAX_PLAYERS];
		openRack = new boolean[MAX_PLAYERS];		// part of the user interface

    	// make sure the single letters are there
    	for(char code = 'a'; code<='z'; code++)
    	{	String letter = ""+code;
    		if(dictionary.get(letter)==null)
    		{	Entry e = new Entry(letter);
    			e.setDefinition("Not really a word, but allowed");
    			privateDictionary.put(letter,e);
    		}
    	}
    	rackMap = new int[MAX_PLAYERS][rackSize+2];
    	Random r = new Random(657642547);
    	drawPile = new WypsCell(r,WypsId.DrawPile);
 
       	doInit(init,key,players,rev); // do the initialization 
 
        robotVocabulary = dictionary.orderedSize;
 
    }
    private void initRackMap(int [][]map)
    {	int full = map[0].length;
    	int max = revision<103 ? full : handSize;
    	int off = (full-max)/2;
    	for(int rown = 0;rown<map.length;rown++)
    	{	int row[] = map[rown];
    		WypsCell cells[] = rack[rown];
    		int cellIdx = 0;
    		int rowIdx = off;
    		AR.setValue(row,-1);
    		while(cellIdx<cells.length)
    		{
    			if(cells[cellIdx].topChip()!=null)
    			{
    				row[rowIdx++] = cellIdx;
    			}
    			cellIdx++;
    			}
    	}
    }
    
    public String gameType() { return(gametype+" "+players_in_game+" "+randomKey+" "+revision); }

    private void construct()
    {	Random r = new Random(6278843);

        // create a grid for the tile cells
        allCells = null;
        initBoard(variation.cols,variation.rows,null);		 //this sets up the primary board and cross links
        allCells.setDigestChain(r);
  	    fullBoardSize = 0;	
  	    emptyCells.clear();
        for(WypsCell c = allCells; c!=null; c=c.next)
        	{ 
        	  emptyCells.push(c);
        	  fullBoardSize++;
        	  for(int i=0;i<c.geometry.n;i+=2)
        	  {	// mark the edges according to what voids are adjacent
        		// bottom row gets 1, left side gets 2, right side gets 4
        		// and the corners get both.  An edge mask for a winning player 
        		// path will be 7
        		  if(c.exitTo(i)==null) { c.edgeMask |= (1<<(i/2)); }
        	  }
        	}
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
    

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int players,int rev)
    {	randomKey = key;
    	adjustRevision(rev);
    	players_in_game = players;
    	win = new boolean[players];
		variation = WypsVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
    	gametype = gtype;
    	nPasses = 0;
    	isPass = false;
    	previousWord = null;
		openRacks = true;
	    for(Option o : Option.values()) { setOptionValue(o,false); }
		lastLetters.clear();
	    occupiedCells.clear();
	    AR.setValue(nOnBoard, 0);
 		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case Wyps_7:
		case Wyps_10:
		case Wyps:
			break;

		}
 		if(revision>=102) { handSize = variation.handSize; } else { handSize = rackSize; }
 		construct();
			 		
 		chipsOnBoard = 0;    
    	AR.setValue(mapPick,-1);
       	AR.setValue(mapTarget, -1);
    	
    	
    	Random r = new Random(randomKey+100);
    	r.nextLong();//  for compatibility
    	drawPile.reInit();
  
    	for(WypsChip c : WypsChip.letters)
    	{
    		drawPile.addChip(c);
    	}
    	drawPile.shuffle(r);
    	int map[] = getColorMap();
    	WypsColor colors[] = WypsColor.values();
    	
 
 		for(int i=0;i<players;i++) { playerColor[i]=colors[map[i]]; }
 		int psize = revision<103 ? rackSize+2 : handSize;
 		if((rack==null)|| (psize!=rack[0].length))
 		{
 		rack = new WypsCell[players][psize];
    	mappedRack = new WypsCell[players][rackSize+2];	// same size as the rack map

     	for(int i=0;i<players;i++)
    	{	WypsCell prack[] = rack[i];

    		for(int j = 0;j<prack.length;j++)
    			{ WypsCell c = prack[j] = new WypsCell(WypsId.Rack,(char)('A'+i),j,Geometry.Standalone);
    			  c.onBoard = false;
    			}
    		WypsCell mrack[] = mappedRack[i];
    		for(int j=0;j<mrack.length;j++)
    		{
  			  mrack[j] = new WypsCell(WypsId.RackMap,(char)('A'+i),j,Geometry.Standalone);

    		}
    	}} else
 		 {  
    		reInit(rack); 
    		reInit(mappedRack);
 		 }
 	
     	for(int i=0;i<players;i++)
     	{WypsCell prack[] = rack[i];
     	for(int j = 0;j<prack.length;j++)
		{ WypsCell c = prack[j];
		  c.onBoard = false;
    			  if((revision>=103) || (j>0 && j<=handSize)) 
    			  	{ c.addChip( drawPile.removeTop().getAltChip(playerColor[i])); }
		}}

		
		initRackMap(rackMap);
 		setState(WypsState.Puzzle);
		robotState.clear();		
	    	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    acceptPlacement();
	    
	    resetState = WypsState.Puzzle;
	    lastDroppedObject = null;
	    // set the initial contents of the board to all empty cells
        animationStack.clear();
        moveNumber = 1;
        validateMap();
        // note that firstPlayer is NOT initialized here
    }

    /** create a copy of this board */
    public WypsBoard cloneBoard() 
	{ WypsBoard dup = new WypsBoard(gametype,players_in_game,randomKey,getColorMap(),dictionary,revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((WypsBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(WypsBoard from_b)
    {
        super.copyFrom(from_b);
        robotState.copyFrom(from_b.robotState);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        chipsOnBoard = from_b.chipsOnBoard;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(selectedStack,from_b.selectedStack);
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        drawPile.copyFrom(from_b.drawPile);
        copyFrom(rack,from_b.rack);
        robotVocabulary = from_b.robotVocabulary;
        lastPicked = null;
        isPass = from_b.isPass;
        nPasses = from_b.nPasses;
        for(Option o : Option.values()) { setOptionValue(o,from_b.getOptionValue(o)); }
        getCell(occupiedCells,from_b.occupiedCells);
        getCell(emptyCells,from_b.emptyCells);
        getCell(lastLetters,from_b.lastLetters);
        AR.copy(nOnBoard,from_b.nOnBoard);
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((WypsBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(WypsBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedsourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(chipsOnBoard==from_b.chipsOnBoard,"chipsOnBoard mismatch");
        G.Assert(robotVocabulary==from_b.robotVocabulary,"robotVocabulary mismatch");
        sameCells(rack,from_b.rack);
        sameCells(drawPile,from_b.drawPile);
        G.Assert(nPasses==from_b.nPasses,"nPasses mismatch");
        G.Assert(isPass==from_b.isPass,"isPass mismatch");
        G.Assert(sameCells(selectedStack,from_b.selectedStack),"selected stack mismatch");
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
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,selectedStack);
		v ^= Digest(r,revision);
		v ^= Digest(r,rack);
		v ^= Digest(r,drawPile);
		v ^= Digest(r,chipsOnBoard);
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
        case Atari:
        case Confirm:
        case ConfirmPass:
        case ConfirmFirstPlay:
        case FirstPlay:
        case DiscardTiles:
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
    	sweep_counter++;
    	int cc = edgeContactCount(player);
    	if(cc==3)
    	{
    		win[player]=true;
    		return(true);
    	}
    	return(false);
    }
    
    public int contactCount[] = { 0, 1, 1, 2, 1, 2, 2, 3};	// index by the mask which is a bitmask
    //
    // return 0-3, the count of the number of edges this player contacts
    // with their best group
    //
    public int edgeContactCount(int player)
    {	int best = 0;
    	for(int lim=occupiedCells.size()-1; lim>=0; lim--)
    	{
    		WypsCell seed = occupiedCells.elementAt(lim);
    		if(seed.topChip().getColor()==playerColor[player])
    		{
    		int mask = seed.sweepEdgeMask(0,sweep_counter);
    		if(mask==7) 
    			{ return(contactCount[mask]);
    			}
    		int count = contactCount[mask];
    		if(count>best) { best = count; }
    			}
    		}
    	return(best);
    	}
    
    // prepare for scoring sweeps, used in move generation, by copying the
    // real colors to the scoring cells.
    private void prepareScoreSweep()
    {
    	for(WypsCell c = allCells; c!=null; c=c.next) 
    		{
    		WypsChip top = c.topChip();
    		c.scoreColor = c.refScoreColor = (top==null) ? null : top.getColor(); 
    		}
    }
    
 
    // same as edgeContact count, but uses cached values in the cells themselves
    private int scoreContactCount(Template template,int player)
    {	
    	WypsColor myColor = playerColor[player];
     	template.applyScoreTemplate(myColor);
     	int myscore = scoreContactCountFor(template,player);
     	int hiscore = scoreContactCountFor(template,nextPlayer[player]);
       	template.revertScoreTemplate();
     	return(myscore==3 ? myscore : hiscore==3 ? -3 :myscore-hiscore);
    }
    private int scoreContactCountFor(Template template,int p)
    {	WypsColor myColor = playerColor[p];
    	int best = 0;
    	sweep_counter++;
    	for(WypsCell c : template.cells)
    {
    		if(c.scoreColor==myColor) 
    	{
    			int mask = c.scoreEdgeMask(0,sweep_counter);
    			if(mask==7) { return(contactCount[mask]); }
    			int count = contactCount[mask];
        		if(count>best) { best = count; }
    			}
    	}
    	
    	for(int lim=occupiedCells.size()-1; lim>=0; lim--)
    		{
    		WypsCell seed = occupiedCells.elementAt(lim);
    		if(seed.scoreColor==myColor)
    			{
    		int mask = seed.scoreEdgeMask(0,sweep_counter);
    		if(mask==7) 
    			{ return(contactCount[mask]);
    			}
    		int count = contactCount[mask];
    		if(count>best) { best = count; }
    		}
    	}
    	return(best);
    }
    static final double EdgeMultiplier = 1000;
    static final double TileMultiplier = 1;
    
    public double scoreForPlayer(int p)
    {	// about the simplest thing that could push the scores in the right direction.
    	// count of edges and number of stones on the board are the only factors.
    	return( (edgeContactCount(p)*EdgeMultiplier+nOnBoard[p])/(3*EdgeMultiplier+fullBoardSize*TileMultiplier));
    }
    public double templateScore(Template template,int p)
    {	// about the simplest thing that could push the scores in the right direction.
    	// count of edges and number of stones on the board are the only factors.
    	return( (scoreContactCount(template,p)*EdgeMultiplier
    			 +(nOnBoard[p]+template.newOnBoard+template.newOffBoard)*TileMultiplier)/(3*EdgeMultiplier+fullBoardSize*TileMultiplier));
    }


    // set the contents of a cell, and maintain the books
    public WypsChip SetBoard(WypsCell c,WypsChip ch)
    {	WypsChip old = c.topChip();
    	if(c.onBoard)
    	{
    	if(ch==null) 
    		{ WypsChip top = c.topChip();
    		  if(top!=null)
    		  	{ chipsOnBoard--; 
    		  	  nOnBoard[playerIndex(top.getColor())]--;
    		  	}
    		  c.reInit();
    		  occupiedCells.remove(c,false);
    		  emptyCells.push(c);
    		}
    	else { G.Assert(c.isEmpty(),"not expecting to make stacks");
    		c.addChip(ch);
    		occupiedCells.push(c);
    		emptyCells.remove(c,false);
    		chipsOnBoard++;
    		nOnBoard[playerIndex(ch.getColor())]++;
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
        selectedStack.clear();
        stateStack.clear();
        pickedObject = null;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private WypsCell unDropObject(WypsCell c)
    {	WypsCell rv = droppedDestStack.remove(c,false);
    	setState(stateStack.pop());
    	if(rv==drawPile) { pickedObject = drawPile.removeTop(); }
    	else {
    	WypsChip po = SetBoard(rv,null); 	// SetBoard does ancillary bookkeeping
    	pickedObject = po;
    	}
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject(WypsCell rv)
    {	pickedSourceStack.remove(rv);
    	setState(stateStack.pop());
    	SetBoard(rv,pickedObject);
    	pickedObject = null;
    }
    
    public WypsCell lastDropped()
    {
    	return(droppedDestStack.top());
    }

    
    // true if the set of cells in target forms a word,
    // considering all possible anagrams.  Normally the 
    // first cell in the stack is a single connected cell,
    // and the word is linear from there.  But if not, we'll
    // go on to try all permutations.
    //
    private boolean isAWord(CellStack target)
    {	int lim = target.size();
    	for(int i=0;i<lim;i++)
    	{	WypsCell anchor = target.elementAt(i);
    		if(anchor.isSingleConnected(target))
    		{
    			if(isAWordPathFrom(anchor,target)) { return(true); }
    		}
    	}
    	// if no single letter is single connected, have to try them all
    	for(int i=0;i<lim;i++)
    	{	WypsCell anchor = target.elementAt(i);
    		if(isAWordPathFrom(anchor,target)) { return(true); }
    	}
    	return(false);
    }
    // true if all new words are in a line
    private boolean isALine(CellStack target)
    {	int lim = target.size()-1;
    	if(lim>0)
    	{
    	boolean allInRow = true;
    	boolean allInCol = true;
    	boolean allInDiag = true;
    	WypsCell base = target.elementAt(lim--);
    	int baseRow = base.row;
    	char baseCol = base.col;
    	while(lim>=0)
    		{
    		WypsCell next = target.elementAt(lim--);
    		allInRow &= next.row==baseRow;
    		allInCol &= next.col==baseCol;
    		allInDiag &= next.col-base.col == next.row-base.row;
    		}
    	boolean val = allInDiag || allInCol || allInRow;
    	return(val);
    	}
    	return(true);
    	}
    private boolean isSingleFlip(CellStack target)
    {	int nflip = 0;
    	WypsColor myColor = playerColor[whoseTurn];
    	for(int lim = target.size()-1; lim>=0; lim--)
    	{
    		WypsChip top = target.elementAt(lim).topChip();
    		if(top.getColor()!=myColor) { nflip++; }
    	}
    	return(nflip<=1);
    }
    // 
    // drop the floating object.
    //
    private void dropObject(WypsCell c)
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
    public boolean isADest(WypsCell c)
    {
    	return droppedDestStack.contains(c);
    }
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { WypsChip ch = pickedObject;
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
    private WypsCell getCell(WypsId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source " + source);
        case DrawPile:
        	return(drawPile);
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
    public WypsCell getCell(WypsCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(WypsCell c)
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
	    	for(int i=0;i<map.length;i++) { if(map[i]==c.row) { map[i]=-1; break; }} 
	      	WypsChip po = c.removeTop();
	        lastPicked = pickedObject = po;
	     	lastDroppedObject = null;
	    	}
			break;
		case DrawPile:
			lastPicked = pickedObject = c.removeTop();
			break;
        case BoardLocation:
        	{
        	WypsChip po = c.topChip();
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
    public boolean isASource(WypsCell c)
    {	return(pickedSourceStack.contains(c));
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(WypsCell c,WypsChip ch,replayMode replay)
    {
        switch (board_state)
        {
        case ConfirmPass:
        default:
        	throw G.Error("Not expecting drop in state " + board_state);
        case FlipTiles:
        case FirstPlay:
        	setState(WypsState.ConfirmFirstPlay);
        	break;
        case Confirm:
        case Play:
        case Atari:
        case ResolveBlank:
        	if(c==drawPile) { setState(WypsState.DiscardTiles); }
         	else if(validate()) { setState(WypsState.Confirm); }
        	else { setState(WypsState.Play); }
			break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }

    private boolean allPassed()
    {	
    	return(nPasses>=players_in_game);
    }
    // flip all, or just one tile of the new word.  If the 
    // requirement is ambiguous, he'll have to select which
    private boolean flipTiles()
    {
    	if(isALine||singleFlip) 
    	{	WypsColor myColor = playerColor[whoseTurn];
    		for(int lim=lastLetters.size()-1; lim>=0; lim--)
    		{
    			WypsCell letter = lastLetters.elementAt(lim);
    			WypsChip top = letter.topChip();
    			if(top.getColor()!=myColor)
    			{
    				letter.removeTop();
    				letter.addChip(top.getAltChip(myColor));
    			}
    		}
    		return(false);
    	}
    	return(true);
    }
    private void setNextStateAfterDone(replayMode replay)
    {	
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state "+board_state);
    	case Gameover: break;
		case Confirm:
			if((resetState!=WypsState.FlipTiles) && flipTiles()) 
				{ setState(WypsState.FlipTiles);
				  break;
				}
			//$FALL-THROUGH$
 	   	case Puzzle:
     		if(chipsOnBoard==0) { setState(WypsState.FirstPlay); break; }
			//$FALL-THROUGH$
		case ConfirmPass:
		case ConfirmFirstPlay:
    	case DiscardTiles:
       	case FirstPlay:
       	case Atari:
    	case Play:
    		{
    			drawNewTiles(replay);
    			boolean wasConnected = winForPlayerNow(whoseTurn);
            	setNextPlayer(replay);
            	boolean isConnected = winForPlayerNow(whoseTurn);
            	if(isConnected) { setState(WypsState.Gameover); }
            	else if(chipsOnBoard==fullBoardSize)
            	{
            		setState(WypsState.Gameover);
            	}
            	else if(allPassed()) { setState(WypsState.Gameover);}
            	else { setState( wasConnected ? WypsState.Atari : WypsState.Play); }
    		}
    		
    		break;
    	}
       	resetState = board_state;
    }
    private void drawNewTiles(replayMode replay)
    {	WypsCell myrack[] = rack[whoseTurn];
    	WypsCell mapped[] = mappedRack[whoseTurn];
    	int mymap[] = rackMap[whoseTurn];
    	int n=0;
    	validateMap(whoseTurn);
    	for(WypsCell c : myrack) { if(c.topChip()!=null) { n++; }}
    	for(WypsCell c : myrack)
    		{ if(drawPile.height()==0 || n==handSize) break;
    		  if(c.topChip()==null)
    			  {
    			  WypsChip newchip = drawPile.removeTop().getAltChip(playerColor[whoseTurn]);
    			  c.addChip(newchip);
    			  n++;
    			  if(replay!=replayMode.Replay) 
    			  {
    				  WypsCell cd = mapped[c.row];
    				  // the mapped cell is used only by the user interface and is loaded by
    				  // the user interface during redisplay, but we need to preload it so the
    				  // animation will know there is something to do.
    				  cd.addChip(newchip);
    				  animationStack.push(drawPile);
    				  animationStack.push(cd);
    			  }
    			  }
		 		int empty = -1;
				boolean filled = false;
				int rackIdx = c.row;
				for(int mapidx = 0;!filled && mapidx<mapped.length; mapidx++)
				{	int mapv = mymap[mapidx];
					if(mapv==rackIdx) { filled = true; }
					else if((empty<0) && (mapv<0)) { empty = mapidx; }
				}
				if(!filled) { mymap[empty] = rackIdx; }
    		}
    	if(revision>=104)
    	{ // mark the permanently empty cells are placeable
       	int ma[] = rackMap[whoseTurn];
    	for(int i=0;i<ma.length;i++)
    		{	
    		int v = ma[i];
    		if((v>=0) && (myrack[v].topChip()==null)) { ma[i] = -1; }
    		}
    	}
    	validateMap(whoseTurn);

    }
    
    StringBuilder builder = new StringBuilder();
    CellStack buildCells = new CellStack();
    boolean isNew = false;	// the word in the builder includes a new letter
	Hashtable<String,Word> wordsUsed = new Hashtable<String,Word>();
	WordStack words = new WordStack();
	WordStack candidateWords = new WordStack();	// for the robot
	
    private boolean isAWordPathFrom(WypsCell anchor,CellStack target)
    {	builder.setLength(0);
    	buildCells.clear();
    	words.setSize(0);
    	sweep_counter++;
    	boolean hasWord = buildWordPath(anchor,target);
    	return(hasWord);
    }
    private boolean buildWordPath(WypsCell from, CellStack target)
    {	int prev_sweep = from.sweep_counter;
    	from.sweep_counter = sweep_counter;
    	builder.append(from.topChip().lcChar);
    	buildCells.push(from);
    	boolean rval = false;
    	int sz = builder.length();
    	if(sz==target.size()) 
    	{
    		// end of the line, we either have a word or not
    		String word = builder.toString();
    		Entry e = dictionary.get(word); 
    		if(sz==1 && e==null) { e = privateDictionary.get(word); }	// allow the single letters
    		if(e!=null)
    			{ 	words.recordCandidate("built",new Template(buildCells.toArray()),e);
    				rval = true;
    			}
    		else
    		{
    		builder.reverse();
    		String rword = builder.toString();
    		builder.reverse();
    		Entry re = dictionary.get(rword);
    		if(re!=null)
    		{
    	  		words.recordCandidate("built",new Template(buildCells.toReverseArray()),re);
    			rval = true;  			
    		}}
    	}
    	else
    	{
    	boolean some = false;
    	for(int direction = from.geometry.n-1; direction>=0; direction--)
    	{	
    		WypsCell next = from.exitTo(direction);
    		if(next!=null && next.sweep_counter!=sweep_counter && target.contains(next))
    		{	some |= buildWordPath(next,target);
    		}
    	}
    	rval = some;
    	}
    	buildCells.pop();
    	builder.deleteCharAt(builder.length()-1);
    	from.sweep_counter = prev_sweep;
    	return(rval);
    }
	
	public String invalidReason=null;
	private boolean isALine = false;
	private boolean singleFlip = false;
	/* 
	 * this validates that all the lines on the board are words, and are all
	 * connected, the list of new words, the list of words, and the list of non words.
	 * if andScore is true, it also calculates the score change and the lists
	 * of word caps.
	 */
   private synchronized boolean validate()
    {
    	sweep_counter++;
    	builder.setLength(0);
    	isALine = singleFlip = false;
    	words.clear();
    	invalidReason = null;
    	int newtiles = droppedDestStack.size();
    	int reusedTiles = selectedStack.size();
    	int tileCount = (newtiles==0) ? -1 : validateConnectedFrom(droppedDestStack.elementAt(0));
    	boolean connected = tileCount==(newtiles+reusedTiles);
    	if(connected)
    	{
    	candidateWord.copyFrom(droppedDestStack);
    	candidateWord.union(selectedStack);
    	
    	boolean isAWord = isAWord(candidateWord);
    	if(isAWord)
    	{
    	isALine = isALine(candidateWord);
    	if(!isALine) { singleFlip = isSingleFlip(candidateWord); }
     	return(true);
    	}
    	else 
    		{invalidReason = NotAWord;
    		}
    	}
    	else { invalidReason = (newtiles==0) ? NoNewLetters : NotConnected; }
    	return( false);
    }

    // return the count of tiles encountered, considering only cells
    // in either the destingation set or the selected set.  
    private int validateConnectedFrom(WypsCell c)
    {
    	if(c==null || c.sweep_counter>=sweep_counter || (c.topChip()==null)) { return(0); }
    	if(isADest(c)||(isSelected(c)))
    	{
    		int n = 1;
        	c.sweep_counter = sweep_counter;
         	for(int dir = 0; dir<CELL_FULL_TURN; dir++)
        	{	WypsCell next = c.exitTo(dir);
         		WypsChip top = next==null ? null : next.topChip();
        		if(top!=null) { n += validateConnectedFrom(next); }
        	}
        	return(n);	
    	}
    	else 
    	{ return(0);
    	}    	
    }
    
    private void doDone(replayMode replay)
    {
    	if((robot==null) && ((resetState==WypsState.Play) || (resetState==WypsState.Atari)))
    	{
    		for(int lim=words.size()-1; lim>=0; lim--)
    		{	Word w = words.elementAt(lim);
    			Entry e = w.entry;
    			previousWord = e;
    			logGameEvent(AddWordMessage,w.name);
    			
    		}
    	}
    	lastLetters.copyFrom(candidateWord);
    	
        acceptPlacement();
        
        if(isPass)
        { nPasses++;
          isPass = false;
        }
        else { nPasses=0; }
        
        if (board_state==WypsState.Resign)
        {
            win[nextPlayer[whoseTurn]] = true;
    		setState(WypsState.Gameover);
        }
        else if(board_state==WypsState.DiscardTiles)
        {
        	for(WypsCell c : rack[whoseTurn])
        	{	WypsChip top = c.topChip();
        		if(top!=null)
        			{ drawPile.addChip(top); c.removeTop(); 
        			  if(replay!=replayMode.Replay)
        			  {
        				  animationStack.push(c);
        				  animationStack.push(drawPile);
        			  }
        			}
        	}
        	drawPile.shuffle(new Random(randomKey+moveNumber*100));
        	if(revision>=104)
        	{
        		drawNewTiles(replay);
        	}
        	else
        	{
        	for(int i=revision<103 ? 1 : 0,lim=revision<103?handSize+1:rackSize ;i<lim;i++)
        	{  
        	   if(drawPile.height()>0)
        		{
        		WypsCell c = rack[whoseTurn][i];        		   
        		c.addChip(drawPile.removeTop());
        		if(replay!=replayMode.Replay)
        		{
        			animationStack.push(drawPile);
        			animationStack.push(c);
        		}
        		}
        	}}
    		setNextStateAfterDone(replay);
        }
        else
        {	
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
		}
	}
	public void setOptionValue(Option o,boolean v)
	{
    	switch(o)
    	{
    	default: throw G.Error("Not expecting %s",o);
    	}
	}
	private void placeWord(WypsCell from,WypsCell rack[],String word,long directionMap,replayMode replay)
	{	WypsCell c = from;
		long div = 1;
		int wordlen = word.length();
		int nextDir = 0;
		lastLetters.clear();
		for(int i=2;i<wordlen;i++) { div = div*10; }
		for(int i=0,lim=wordlen; i<lim; i++,c=c.exitTo(nextDir))
		{	WypsChip top = c.topChip();
			nextDir = (int)((directionMap/div)%10);
			div = div/10;
			if(div==0) { div = 1; }
			if(top==null)
			{
			char ch = word.charAt(i);
			WypsCell placeFrom= findChar(rack,ch);
			pickObject(placeFrom);
			dropObject(c);
			lastLetters.push(c);
			if(replay!=replayMode.Replay)
				{
				animationStack.push(placeFrom);
				animationStack.push(c);
				}
			}
			else { selectedStack.push(c); }
		}
	}
	private WypsColor doFlip(WypsCell dest)
	{
		WypsChip top = dest.removeTop();
    	WypsChip ntop = top.getAltChip();
    	dest.addChip(ntop);
    	selectedStack.clear();
    	return(ntop.getColor());
	}
	
	public void dropAndSlide(int who,WypsCell from,int moving0,int pick0,int dest0,replayMode replay)
	{
    	WypsCell rcells[] = mappedRack[who];
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
    	
    	if((replay==replayMode.Single) && (from!=null))
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
	}
	

    public boolean Execute(commonMove mm,replayMode replay)
    {	Wypsmovespec m = (Wypsmovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }
        //G.print("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone(replay);

            break;
        case MOVE_SWAP:
        	{
        		WypsCell [] from = rack[whoseTurn];
        		WypsCell [] to = rack[nextPlayer[whoseTurn]];
        		WypsColor fromColor = playerColor[whoseTurn];
        		WypsColor toColor = playerColor[nextPlayer[whoseTurn]];
        		for(int lim = from.length-1; lim>=0; lim--)
        		{
        			WypsCell f = from[lim];
        			WypsCell t = to[lim];
        			WypsChip fc = f.height()==0 ? null : f.removeTop();
        			WypsChip tc = t.height()==0 ? null : t.removeTop();
        			if(tc!=null) { f.addChip(tc.getAltChip(fromColor));}
        			if(fc!=null) { t.addChip(fc.getAltChip(toColor)); }
        		}
        		setState(board_state==WypsState.FirstPlay 
        						? WypsState.ConfirmFirstPlay
        						: WypsState.FirstPlay);
        	}
        	break;
        case MOVE_PLAYWORD:
        	{
        	WypsCell c = getCell(m.to_col,m.to_row);
        	placeWord(c,rack[whoseTurn],m.word,m.directionMap,replay);   
        	int map[] = rackMap[whoseTurn];
        	WypsCell r[] = rack[whoseTurn];
        	// mark the now-empty cells as unmapped
        	for(int lim=map.length-1; lim>=0; lim--)
        	{	int idx = map[lim];
        		if((idx>=0) && (r[idx].topChip()==null)) { map[lim]=-1; }
        	}
        	setNextStateAfterDrop(null,null,replay);
        	if(flipTiles())
        		{
            	WypsCell flip = getCell(m.from_col,m.from_row);
            	G.Assert(selectedStack.contains(flip),"should be one of these");
            	doFlip(flip);
            	resetState = WypsState.FlipTiles;
            	setState(WypsState.Confirm);
        		}
        	}
        	
        	break;
        case MOVE_PASS:
        	setState((board_state==WypsState.ConfirmPass) ? resetState : WypsState.ConfirmPass);
        	isPass = board_state==WypsState.ConfirmPass;
        	break;
        case MOVE_SETOPTION:
        	{
        	Option o = Option.getOrd(m.to_row/2);
        	boolean v = ((m.to_row&1)!=0);
        	setOptionValue(o,v);
        	
        	}
        	break;
        case MOVE_SHOW:
            {	
            	openRack[m.to_col-'A'] = (m.to_row==0?false:true);
            }
        	break;
        case MOVE_FLIP:
        	{
        	WypsCell dest =  getCell(WypsId.BoardLocation,m.to_col,m.to_row);
        	WypsColor color = doFlip(dest);       	
        	if(color==playerColor[whoseTurn])
        	{
        	selectedStack.push(dest);
        	setState(WypsState.Confirm);
        	}
        	else 
        	{       	
        	setState(WypsState.FlipTiles);
        	}
        	 
        	}
        	break;
        case MOVE_SELECT:
        	{
        		WypsCell dest =  getCell(WypsId.BoardLocation,m.to_col,m.to_row);
        		if(selectedStack.contains(dest))
        		{
        			selectedStack.remove(dest);
        		}
        		else
        		{
        			selectedStack.push(dest);
        		}
        		setNextStateAfterDrop(dest,dest.topChip(),replay);
        	}
        	break;
        case MOVE_DROPB:
        	{
			WypsCell src = pickedSourceStack.top();
			WypsChip po = pickedObject;
			WypsCell dest =  getCell(WypsId.BoardLocation,m.to_col,m.to_row);
			
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

        case MOVE_REMOTEDROP:
    		{
    		int who = m.to_col-'A';
	    	mapPick[who] = -1;
	    	mapTarget[who] = -1;   	
    		}
    		break;
    		
        case MOVE_REPLACE:
	    	{
	    	int who = m.to_col-'A';
	    	WypsCell rcells[] = mappedRack[who];
	    	validateMap(who);
	    	int map[] = rackMap[who];
	    	int pick = mapPick[who];
		    	int dest = m.to_row;
	    	int moving = map[pick];
	    	map[pick] = -1;
		    	mapPick[who] = -1;
	    	mapTarget[who] = -1;
	    	dropAndSlide(who,rcells[pick],moving,pick,dest,replay);
	    	}

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
        	WypsCell src = getCell(m.source,m.from_col,m.from_row);
        	WypsCell dest = getCell(m.dest,m.to_col,m.to_row);
        	pickObject(src);
        	WypsChip po = m.chip = pickedObject;
        	dropObject(dest);
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
 			WypsCell src = getCell(m.dest,m.to_col,m.to_row);
 			if(isADest(src)) { unDropObject(src); }
 			else
 			{
        	// be a temporary p
        	pickObject(src);
        	m.chip = pickedObject;
        	switch(board_state)
        	{
        	case Confirm:
        		setState(resetState);
				//$FALL-THROUGH$
           	case Puzzle:
			case Play:
        		break;
        	default: ;
        	}}}
    		validate();
    		break;

        case MOVE_DROP: // drop on chip pool;
        	{	G.Assert(m.dest==WypsId.Rack,"should be to rack");
            WypsCell dest = getCell(m.dest,m.to_col,m.to_row);
        		if(isASource(dest)) 
        			{ unPickObject(dest); 
        			}   
        		else {
        			dropObject(dest);
            }
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
  		  		dropAndSlide(who,null,dl,-1,slot,replay);
        	}

            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(WypsState.Puzzle);	// standardize the current state
            setNextStateAfterDone(replay);

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?WypsState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(WypsState.Puzzle);
 
            break;
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(WypsState.Gameover);
			break;

        default:
        	cantExecute(m);
        }

        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }
        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        
        validateMap();
        
        return (true);
    }
    public boolean LegalToHitPool(boolean picked)
    {
    	switch(board_state)
    	{
    	default:
    	case ConfirmPass:
    		return(false);
    	case DiscardTiles:
    	case Confirm:
    	case Play:
    	case Atari:
    		return( picked 
    					? (droppedDestStack.size()==0)
    					: (lastDropped()==drawPile));
    	case Puzzle:
    		return(!picked ? drawPile.height()>0 : true);
    	}
    }
    // legal to hit the chip storage area
    public boolean LegalToHitChips(WypsCell c)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting Legal Hit state " + board_state);
        case FirstPlay: 
        case ConfirmFirstPlay:
        case FlipTiles:
        	return(false); 
        case Confirm:
        case Play:
        case Atari:
        	// for pushfight, you can pick up a stone in the storage area
        	// but it's really optional
        	return ( (c!=null) 
        			 && ((pickedObject==null) ? (c.topChip()!=null) : true)
        			);
 		case ResolveBlank:
 		case Resign:
		case Gameover:
		case ConfirmPass:
		case DiscardTiles:
			return(false);
        case Puzzle:
            return ((c!=null) && ((pickedObject==null) == (c.topChip()!=null)));
        }
    }

    public boolean LegalToHitBoard(WypsCell c,boolean picked)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
        case ConfirmFirstPlay:
        case FirstPlay:
        	return(pickedObject!=null
        			? pickedObject!=null && (pickedSourceStack.top().onBoard && c.topChip()==null)
        			: c.topChip()!=null);
		case Confirm:
			if(resetState==WypsState.FlipTiles)
			{
				return(selectedStack.contains(c));
			}
			//$FALL-THROUGH$
		case Play:
		case Atari:
			return(picked ? c.isEmpty() : !c.isEmpty());

		case ResolveBlank:
			return(c==droppedDestStack.top());
		case FlipTiles:
			return(lastLetters.contains(c) && (c.topChip().getColor()!=playerColor[whoseTurn]));
		case Gameover:
		case Resign:
		case ConfirmPass:
		case DiscardTiles:
			return(false);
        default:
        	throw G.Error("Not expecting Hit Board state " + board_state);
        case Puzzle:
            return (picked == (c.topChip()==null));
        }
    }
    
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Wypsmovespec m)
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
    public void UnExecute(Wypsmovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	WypsState state = robotState.pop();
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

int nWordsTried = 0;
int nWordsPlaced = 0;
// find words that can be placed in a different direction across an existing word.  This is used
// both to place ordinary crosswords and to place "cap words" where a letter is being added to 
// the beginning or end of an existing word.
public int checkDictionaryWords(DictionaryHash subDictionary,WypsCell rack[],long rackMask,Template template,boolean findAll)
{	WypsCell toCells[] = template.cells;
	int wordSize = toCells.length;
	int nfound = 0;
	long letterMask = letterSet(toCells,rackMask);		// include the letters in the template in the active mask
	G.Assert(wordSize==subDictionary.size,"word size must match");
	for(Enumeration<Entry> dwords = subDictionary.elements(); dwords.hasMoreElements();)
		{	Entry word = dwords.nextElement();
			nWordsTried++;
			// first a quick check that the word doesn't contain any letters that aren't available
			// this ought to filter out the vast majority of words
			if(word.order<robotVocabulary								// within the vocabulary limit 
					&& ((word.letterMask | letterMask)==letterMask)
					&& canPlaceWord(word.word,template,rack))	// if the word doesn't require any letters not in the rack
			{	nfound++;
				candidateWords.recordCandidate("built",template,word);
				if(!findAll) { return(nfound); }
			}
		}
	return(nfound);
}

 // return true if we can place this word on this template from this rack
 // we've already done the quick checks, but this is definitive
 private boolean canPlaceWord(String targetWord,Template template,WypsCell rack[])
 {	 WypsCell toCells[] = template.cells;
	 long usedLetters = 0;
	 boolean broken = false;
	 int wordSize = toCells.length;
	 int rsize = rack.length;
	 nWordsPlaced++;
	 for(int index = 0; !broken && index<wordSize;index++)
	 {	WypsCell to = toCells[index];
			WypsChip toChip = to.topChip();
			char thisLetter = targetWord.charAt(index);
			if(toChip==null)
			{	boolean found = false;
				for(int i=0;!found && i<rsize;i++)
				{	int bit = 1<<i;
					if((bit&usedLetters)==0)
					{
					WypsCell rackCell = rack[i];
					WypsChip rackChip = rackCell.topChip();
					if(rackChip==null) { usedLetters |= bit; }
					else if(rackChip.lcChar==thisLetter)
						{	usedLetters |= bit;
							found = true;
						}}
				}
				broken = !found;
			}
			else if(toChip.lcChar!=thisLetter)
				{	broken = true;
				}
			}
		return(!broken);
 }
 private WypsCell findChar(WypsCell rack[],char ch)
 {	WypsCell blank = null;
	for(int lim=rack.length-1; lim>=0; lim--)
	{	WypsCell c = rack[lim];
		WypsChip chip = c.topChip();
		if((chip!=null) && (chip.lcChar==ch)) { return(c); }
	}
	return(blank);
 }

 // this generates a mask in the same manner as Dictionary.letterSet, but
 // uses a rack as the source
 private long letterSet(WypsCell rack[],long start)
	{	long s = start;
		for(WypsCell c : rack)
		{
			WypsChip ch = c.topChip();
			if(ch!=null)
			{	char letter = ch.lcChar;
				s = Dictionary.letterMask(s,letter);
			}
		}
		return(s);
	}



 // this is used to find long words for the initial word placement.
 private void checkLongWords(Random r,WypsCell rack[],long letterMask)
 {	
		 
	for(int wordLen = handSize; wordLen>=1; wordLen--)
 	{
	 DictionaryHash sub = dictionary.getSubdictionary(wordLen);
	 char fromCol = (char)('A'+nrows-wordLen-1);
	 int fromRow = nrows-wordLen;
	 WypsCell seed = getCell(fromCol,fromRow);
	 // the initial play will be the longest word possible, placed along the top of the pyramid
	 Template template = Template.makeTemplate(seed,findDirection(fromCol,fromRow,(char)(fromCol+1),fromRow),wordLen);
	 setScore(template,r);
	 int n = checkDictionaryWords(sub,rack,letterMask,template,true);
	 if(n>0) { break; }
	 if(wordLen==1)
	 {
		// in rare cases, there may be no words at all.  Single letters are legal.
		checkDictionaryWords(privateDictionary,rack,letterMask,template,true);
	 }
 	}

 }
 private void setScore(Template extend,Random r)
 {	extend.setFlip(r, playerColor[whoseTurn]);
	extend.score = templateScore(extend,whoseTurn);
 }
 
 TemplateStack activeTemplates = new TemplateStack();
 private void checkWords(WypsCell rack[])
 {	activeTemplates.clear();
 	long letterMask = letterSet(rack,0);
 	int nTemplates = 0;
 	nWordsTried = 0;
 	nWordsPlaced = 0;
 	//recordProgress(baseProgress+0.5*progressScale);
 	Random r = new Random(randomKey*moveNumber);
	prepareScoreSweep();

 	if(emptyCells.size()==fullBoardSize)
 	{	// first word is the longest available word, played across the top
 		checkLongWords(r,rack,letterMask); 
	}
 	else 
 		{
 		boolean finished = true;
 		Word best = null;
 		//
 		// simple robot based on creating random templates and seeing
 		// if a word is available to use it.
 		// as implemented, the score is dependent only on the template,
 		// not on the word used to fill it.
 		//
 		if(board_state==WypsState.Atari) 
 			{
 			candidateWords.setScoreTheshold(-0.8);
 			}
		while(!finished || ( robot!=null && !robot.timeExceeded()))
 		{
 			int size = 1+r.nextInt(3+handSize);
 			nTemplates++;
 			// construct a random template, with a random empty cell as the seed
 			WypsCell seed = emptyCells.elementAt(r.nextInt(emptyCells.size()));
 			Template base = new Template(seed);
 			Template extend = Template.extendTemplate(base, r, size);
 			if(extend!=null)
 			{
 			int wordLen = extend.size();
 			setScore(extend,r);
 			if(candidateWords.acceptScore(extend.score))
 			{
 			DictionaryHash dict = dictionary.getSubdictionary(wordLen);
 			if(dict!=null)
 				{
 				// see if the dictionary contains anything that matches the template.
 				// a successful word is stored in "candidateWords" if it improves the
 				// best score
 				int n = checkDictionaryWords(dict,rack,letterMask,extend,false);	
 				if(n>0 && best!=candidateWords.top()) 
 					{ //G.print("E "+extend+" "+extend.score+" "+candidateWords.top()); 
 					//getScore(extend,r);
 					}
 				}
 			}}
 			}
		G.print("templates ",nTemplates," words tried ",nWordsTried," words placed ",nWordsPlaced," score ",candidateWords.bestScore);

	 }
 }
 private void checkWordsCore()
 {	candidateWords.clear();
 	checkWords(rack[whoseTurn]);
 	candidateWords.sort(true);
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

 private void addFlipTileMoves(CommonMoveStack all,int who)
 {
	 for(int lim = lastLetters.size()-1; lim>=0; lim--)
	 {	
		 WypsCell c = lastLetters.elementAt(lim);
		 WypsChip ch = c.topChip();
		 if(ch.getColor()!=playerColor[who])
		 {
			 all.push(new Wypsmovespec(MOVE_FLIP,c.col,c.row,who));
		 }
	 }
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
    recordProgress(0);
    validate();
    switch(board_state)
    {
    default: throw G.Error("Not expecting ", board_state);
    case FlipTiles:
    	addFlipTileMoves(all,whoseTurn);
    	break;
    case Confirm:
    case ConfirmPass:
    case FirstPlay:
    case ConfirmFirstPlay:
    	all.push(new Wypsmovespec(MOVE_DONE,whoseTurn));
    	break;
    case Atari:
    case Play:
    	checkWords();
     	for(int i=0;i<candidateWords.size();i++)
     	{
     		all.push(new Wypsmovespec(candidateWords.elementAt(i),whoseTurn));
     	}
     	if(all.size()==0)
     	{
     		all.push(new Wypsmovespec(MOVE_PASS,whoseTurn));
     	}
     	break;
    }
 	recordProgress(0);
 	return(all);
 }
 WypsPlay robot = null;
 private void recordProgress(double v)
 {	
	 if(robot!=null) { robot.setProgress(v); }
 }
 public void initRobotValues(WypsPlay rob,int vocab)
 {	robot = rob;
 	robotVocabulary = vocab;
 }
 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(Character.isDigit(txt.charAt(0)))
	 	{ xpos -= cellsize/2;
	 	}
 		else
 		{ 
 		  ypos += cellsize/4;
 		}
 	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
 }
 public void validateMap()
 {
	 validateMap(0);
	 validateMap(1);
 }
 @SuppressWarnings("unused")
 public void validateMap(int forplayer)
 {		int used = 0;
 		int mapped = 0;
 		int nmapped = 0;
 		WypsCell prack[] = rack[forplayer];
 		for(WypsCell c : prack)
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
 		G.Advise(nmapped==used,"map not complete");
 		}

}