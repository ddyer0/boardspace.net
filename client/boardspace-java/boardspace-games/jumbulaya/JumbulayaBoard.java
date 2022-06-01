package jumbulaya;

import static jumbulaya.Jumbulayamovespec.*;

import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;
import online.game.cell.Geometry;
import dictionary.Dictionary;
import dictionary.DictionaryHash;
import dictionary.Entry;

/**
 * Initial work August 2021
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
	public int bestScore = -999999;				// best score so far
	public int leastScore = -999999;			// the lowest score currently in the stack
	
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
		bestScore = -999999;
		leastScore = -999999;
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
	public Word recordCandidate(String message,Word w,Entry e)
	{	int score = w.points;
		if((score>leastScore) || (accepted==0))
		{
		trimToSize();
		push(w);
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
	Word predecessor=null;
	JumbulayaCell seed;		// starting point
	private CellStack path = null;
	public boolean isJumbulaya() { return(path!=null); }
	public CellStack getPath() { return(path); }
	
	int points=-1;			// the value of the word when played
	int nTiles = 0;
	String comment = null;	// for the word search
	Entry entry;
	long letterMask = 0;
	public String toString() 
	{ StringBuilder b = new StringBuilder();
	  b.append("<word ");
	  b.append(name);
	  b.append(" ");
	  if(isJumbulaya()) { b.append("Jumbulaya "); }
	  else {
	  b.append(seed.col);
	  b.append(seed.row);
	  b.append(" ");
	  }
	  b.append(points);
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
	public boolean sameWord(Word other)
	{
		return(name.equals(other.name));
	}
	public Word(JumbulayaCell s,int ntiles, String n, Word pre)
	{
		this(s,ntiles,n);
		predecessor = pre;
	}
	public Word(JumbulayaCell s,int ntiles, String n)
	{
		seed = s;
		name = n;
		nTiles = ntiles;
	}
	// constructor for Jumbulayas
	public Word(CellStack s,String n)
	{
		path = s;
		seed = path.top();
		name = n;
		nTiles = path.size();
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
	
	public int wordScore()
	{
		int tileScore = JumbulayaBoard.WordScores[nTiles];
		return(tileScore + name.length()-nTiles);
	}
	public static int wordScore(int nTiles)
	{
		return(JumbulayaBoard.WordScores[nTiles]);
	}
	public int jumbulayaScore()
	{	int jscore = JumbulayaBoard.JumbulayaScores[nTiles];
		return(jscore);
	}
	public static int jumbulayaScore(int nTiles)
	{
		return(JumbulayaBoard.JumbulayaScores[nTiles]);
	}
	
	public boolean equals(Word to)
	{
		if(this==to) { return(true); }
		if(!name.equals(to.name)) { return(false); }
		if((path==null) != (to.path==null)) { return(false); }
		if(path!=null && !path.sameContents(to.path)) { return(false); }
		return(seed.sameCell(to.seed));
	}
	static public boolean sameWord(Word from,Word to)
	{
		if(from==to) { return(true);}
		if((from==null)!=(to==null)) { return(false); }
		return(from.equals(to));
	}
    static public boolean sameWords(Word[]from,Word[]to)
    {	if((from!=null) != (to!=null)) { return(false); }
    	if((from==null) || (from.length!=to.length)) { return(false); }
    	for(int lim=from.length-1; lim>=0; lim--)
    	{
    		if(!sameWord(from[lim],to[lim])) 
    			{ return(false); }
    	}
    	return(true);
    }
    //
    // true if this word occurs in the history of words on this line. 
    //
	public boolean containsInHistory(String targetWord) {
		if(name.equals(targetWord)) { return(true); }
		if(predecessor==null) { return(false); }
		if(predecessor.name.length()*2<targetWord.length()) { return(false); }	//definitely too short
		// there could be some nasty cases with double letters substituting
		// so just cool it and do the search
		return(predecessor.containsInHistory(targetWord));
	}
	//
	// get the lettermask for this word
	//
	public long letterMask()
	{	long l = letterMask;
		if(l==0)
			{	
			l = letterMask = Dictionary.letterMask(l, name);
			}
		return(l);
	}
}

class JumbulayaBoard extends squareBoard<JumbulayaCell> implements BoardProtocol,JumbulayaConstants
{	static int REVISION = 101;			// 100 represents the initial version of the game
										// revision 101 fixes the inconsistent rack refill problem
	static final String[] JumbulayaGRIDSTYLE = { null, null, "A","1" }; // left and bottom numbers
	public static final int WordScores[] = {0,0,0,3,4,5,6,10,12,15,20};				// horizontal word scores for 3+
	public static final int JumbulayaScores[] = { 0, 0, 0, 0, 0, 0, 0, 10, 12, 15};	// vertical word scores for 7+

	public int getMaxRevisionLevel() { return(REVISION); }
	public static int VALUEOFROW = 10;					// nudge the robot to fill new rows
	static final int MAX_PLAYERS = 4;	// 4 players in the game
	static final int NROWS = 9;			// rows on the board
	static final int NCOLS = 10;		// columns in each row
	static final int rackSize = 5;		// the number of filled slots in the rack.  Actual rack has some empty slots
	int sweep_counter = 0;
	JumbulayaVariation variation = JumbulayaVariation.Jumbulaya;
	private JumbulayaState board_state = JumbulayaState.Puzzle;	
	private JumbulayaState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	public int robotVocabulary = 999999;		//	size of the robot's vocabulary
	public int robotWinThreshold = Word.wordScore(NCOLS);
	private boolean isPass = false;
	private int nPasses = 0;
	public JumbulayaState getState() { return(board_state); }
	public JumbulayaCell claimed[] = new JumbulayaCell[NROWS];
	int nLinesClaimed = 0;
	int maxLineTiles = 0;
	int revertJumbulayaPlayer = -1;
	JumbulayaState revertJumbulayaState;
	boolean needShuffle = false;
	
	boolean skipTurn[] = new boolean[MAX_PLAYERS];
	boolean resigned[] = new boolean[MAX_PLAYERS];
	
	private void setUnclaimed(int row)
	{
	   	JumbulayaCell c = claimed[row-1];
	   	JumbulayaChip top = c.topChip();
	   	if(top!=null)
	   	{	Word currentWord = getCurrentWord(row);
	   		int who = playerOwning(top);
	   		c.removeTop();
	   		nLinesClaimed--;
	   		score[who] -= currentWord.wordScore();
	   	}
	}
	
    private Word setClaimed(int row,int who)
    {	
    	JumbulayaCell c = claimed[row-1];
    	Word currentWord = getCurrentWord(row);
    	G.Assert(c.topChip()==null,"should be unclaimed");
    	nLinesClaimed++;
    	score[who] += currentWord.wordScore();
    	c.addChip(playerColor(who));
    	return(currentWord);
    }
    
    
    // get the player index who owns the line containing C, or -1 if no one owns it
    private int rowOwner(JumbulayaCell c)
    {	return(rowOwner(c.row));
    }
    private int rowOwner(int row)
    {
   	 JumbulayaCell claim = claimed[row-1];
   	 JumbulayaChip top = claim.topChip();
   	 if(top==null) { return(-1); }
   	 return(playerOwning(top));	 
    }
    
    // get the player index associated with a given color chip
    private int playerOwning(JumbulayaChip color)
    {	int map[] = getColorMap();
   	for(int i=0;i<map.length;i++)
   	{
   		if(JumbulayaChip.Colors[map[i]]==color) { return(i); }
   	}
   	throw G.Error("not a color chip");
    }

	public Word currentWord[] = new Word[NROWS];
	public Word getCurrentWord(int row) { return(currentWord[row-1]); }
	private void setCurrentWord(int row,Word w)
	{	w.predecessor = currentWord[row-1];
		resetCurrentWord(row,w);
	}
	private void resetCurrentWord(int row,Word w)
	{
		currentWord[row-1] = w;
		maxLineTiles = Math.max(maxLineTiles, w.nTiles);
		//G.print("Current "+w+" "+maxLineTiles);
	}
	
	public JumbulayaCell playerCell[] = new JumbulayaCell[MAX_PLAYERS];
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
	public void setState(JumbulayaState st) 
	{ 	
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public void setGameOver()
	{
		setState(JumbulayaState.Gameover);
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
	private JumbulayaCell rack[][] = null;
	private JumbulayaCell mappedRack[][] = null;
	public JumbulayaCell[] getPlayerRack(int n)
	{
		if(rack!=null && rack.length>n) { return(rack[n]); }
		return(null);
	}
	public JumbulayaCell[] getPlayerMappedRack(int n)
	{
		if(mappedRack!=null && mappedRack.length>n) { return(mappedRack[n]); }
		return(null);
	
	}
	public JumbulayaChip getPlayerChip(int pidx)
	{	int map[] = getColorMap();
		return(JumbulayaChip.Colors[map[pidx]]);
	}
	public JumbulayaCell getPlayerCell(int pidx)
	{
		return(playerCell[pidx]);
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
	public int activeRow = -1;		// activerow is only used by the user interface
	public int previousRow = -1;	// previous activerow
	private int rackMap[][] = null;
	public int[] getRackMap(int n)
	{
		if(rackMap!=null && rackMap.length>n) { return(rackMap[n]); }
		return(null);
	}
	public boolean someRackIsEmpty()
	{
		for(JumbulayaCell r[] : rack) { if(rackIsEmpty(r)) { return(true); }}
		return(false);
	}
	private boolean rackIsEmpty(JumbulayaCell r[])
	{	for(JumbulayaCell c : r) { if(c.topChip()!=null) { return(false); }}
		return(true);
	}
	public boolean hiddenVisible[] = null;
	int score[] = null;
	
	// simple evaluation for a player, his value minus the best other value
	public int staticEval(int forPlayer)
	{	int my = score[forPlayer];
		int ot = -1;
		for(int i=0;i<players_in_game;i++)
		{
			if(i!=forPlayer) 
			{	int sc = score[i];
				if((ot==-1) || (sc>ot)) { ot = sc; }
			}
		}
		return(my-ot);		// influence to claim new lines by scoring +10 for them
	}
	
	JumbulayaCell drawPile = null;
	boolean openRacks = false;			// show all players racks
	boolean openRack[];					// per player open rack
	Dictionary dictionary ;				// the current dictionary
	
// When a draw by repetition is detected, this function is called.
// the game should have a "draw pending" state and enter it now, pending confirmation
// by the user clicking on done.   If this mechanism is triggered unexpectedly, it
// is probably because the move editing in "editHistory" is not removing last-move
// dithering by the user, or the "Digest()" method is not returning unique results
// other parts of this mechanism: the Viewer ought to have a "repRect" and call
// DrawRepRect to warn the user that repetitions have been seen.
	public void SetDrawState() { setState(JumbulayaState.Gameover); };	
	CellStack animationStack = new CellStack();

    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public JumbulayaChip pickedObject = null;
    public boolean pickedFromRack = false;
    public JumbulayaChip lastPicked = null;
    CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    private JumbulayaState resetState = JumbulayaState.Puzzle; 
    public JumbulayaChip lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public JumbulayaCell newcell(char c,int r)
	{	return(new JumbulayaCell(JumbulayaId.BoardLocation,c,r,Geometry.Square));
	}
	
	// constructor 
    public JumbulayaBoard(String init,int players,long key,int map[],Dictionary di,int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = JumbulayaGRIDSTYLE;
        setColorMap(map);
    	// allocate the rack map once and for all, it's not used in the board
    	// only as part of the UI.
       	mapPick = new int[MAX_PLAYERS];
       	mapTarget = new int[MAX_PLAYERS];
		hiddenVisible = new boolean[MAX_PLAYERS];
		openRack = new boolean[MAX_PLAYERS];		// part of the user interface

       	rackMap = new int[MAX_PLAYERS][rackSize+2];
       	Random r = new Random(26624564);
       	for(int lim = claimed.length-1; lim>=0; lim--)
       	{
       		claimed[lim] = new JumbulayaCell(r,JumbulayaId.Claimed,lim+1);
       	}
       	for(int i=0;i<MAX_PLAYERS;i++) { playerCell[i]=new JumbulayaCell(r,JumbulayaId.PlayerCell,i); }
       	drawPile = new JumbulayaCell(r,JumbulayaId.DrawPile);
       	doInit(init,key,players,rev); // do the initialization 


       	dictionary = di;
        robotVocabulary = dictionary.orderedSize;
 
    }
    private void initRackMap(int [][]map)
    {	
   		int last = Math.min(map.length, rack.length);
   		for(int i=0;i<last;i++)
    	{
    		int row[] = map[i];
    		JumbulayaCell cs[] = rack[i];
    		for(int j=0,css=cs.length;j<row.length;j++) 
    		{ row[j] = ((j<css) && (cs[j].topChip()!=null)) ? j : -1;
    		}
    	}
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
    	win = new boolean[players];
		variation = JumbulayaVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		score = new int[players];
    	gametype = gtype;
    	nPasses = 0;
    	isPass = false;
		openRacks = false;
		previousWord = null;
		pendingWord = null;
		
	    for(Option o : Option.values()) { setOptionValue(o,false); }
 		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case Jumbulaya:
			break;
		}
 		
 		reInitBoard(NCOLS,NROWS);
						 		
    	Random r = new Random(randomKey+100);
    	drawPile.reInit();
    	for(JumbulayaChip c : JumbulayaChip.letters)
    	{
    		drawPile.addChip(c);
    	}
    	drawPile.shuffle(r);
    	needShuffle = false;
  	    reInit(claimed);
  	    nLinesClaimed = 0;
  	    maxLineTiles = 0;
  	    revertJumbulayaPlayer = -1;
  	    revertJumbulayaState = null;
  	    AR.setValue(skipTurn, false);
  	    AR.setValue(resigned,false);
  	    int map[] = getColorMap();
  	    int mp = Math.min(MAX_PLAYERS, players_in_game);
  	    for(int i=0;i<mp; i++)
  	    {	playerCell[i].reInit();
  	    	playerCell[i].addChip(JumbulayaChip.Colors[map[i]]);
  	    }
 		int racks = rackSize+2;
 		if(rack==null || rack.length!=players || racks!=rack[0].length)
 		{
    	rack = new JumbulayaCell[players][rackSize+2];
    	mappedRack = new JumbulayaCell[players][rackSize+2];
    	for(int i=0;i<players;i++)
    	{	JumbulayaCell prack[] = rack[i];
    		for(int j = 0;j<prack.length;j++)
    			{ JumbulayaCell c = rack[i][j] = new JumbulayaCell(JumbulayaId.Rack,(char)('A'+i),j,Geometry.Standalone);
    			  c.onBoard = false;
    			  mappedRack[i][j] = new JumbulayaCell(JumbulayaId.RackMap,(char)('A'+i),j,Geometry.Standalone);
    			}
    	}
 		}
    	else 
    	{ reInit(rack); 
    	  reInit(mappedRack);
    	}
 		
 		// fill the racks
    	for(int i=0;i<players;i++)
    	{	JumbulayaCell prack[] = rack[i];
    		for(int j = 1;j<=rackSize;j++)
    			{ JumbulayaCell c = prack[j];
    			  c.addChip(drawPile.removeTop()); 
    			  c.fromRack = true;
    			}
    	}
	    initRackMap(rackMap);
	    AR.setValue(currentWord,null);
    	AR.setValue(mapPick,-1);
    	AR.setValue(mapTarget, -1);
    	// fill the center columns of the board
    	for(int row = 1;row<=nrows; row++)
    	{
    		for(int coln = 0;coln<3;coln++)
    		{
    			char col = (char)('D'+coln);
    			JumbulayaCell c = getCell(col,row);
    			c.addChip(drawPile.removeTop());
    			c.fromRack = false;
    		}
    	}

 		setState(JumbulayaState.Puzzle);
		robotState.clear();		
	    	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    acceptPlacement();
	    
	    resetState = JumbulayaState.Puzzle;
	    lastDroppedObject = null;
	    // set the initial contents of the board to all empty cells
        animationStack.clear();
        moveNumber = 1;
	    
	    setCurrentWords();
        
        // note that firstPlayer is NOT initialized here
    }

    /** create a copy of this board */
    public JumbulayaBoard cloneBoard() 
	{ JumbulayaBoard dup = new JumbulayaBoard(gametype,players_in_game,randomKey,getColorMap(),dictionary,revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((JumbulayaBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(JumbulayaBoard from_b)
    {
        super.copyFrom(from_b);
        robotState.copyFrom(from_b.robotState);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        AR.copy(currentWord,from_b.currentWord);
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        pickedFromRack = from_b.pickedFromRack;
        activeRow = from_b.activeRow;
        previousRow = from_b.previousRow;
        resetState = from_b.resetState;
        drawPile.copyFrom(from_b.drawPile);
        copyFrom(rack,from_b.rack);
        copyFrom(claimed,from_b.claimed);
        nLinesClaimed = from_b.nLinesClaimed;
        maxLineTiles = from_b.maxLineTiles;
        revertJumbulayaPlayer = from_b.revertJumbulayaPlayer;
        revertJumbulayaState = from_b.revertJumbulayaState;
        AR.copy(skipTurn,from_b.skipTurn);
        AR.copy(resigned,from_b.resigned);
        copyFrom(playerCell,from_b.playerCell);
        AR.copy(score,from_b.score);
        robotVocabulary = from_b.robotVocabulary;
        needShuffle = from_b.needShuffle;
        lastPicked = null;
        isPass = from_b.isPass;
        nPasses = from_b.nPasses;
        for(Option o : Option.values()) { setOptionValue(o,from_b.getOptionValue(o)); }
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((JumbulayaBoard)f); }


    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(JumbulayaBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(pickedFromRack==from_b.pickedFromRack, "pickedFromRack mismatch");
        G.Assert(activeRow==from_b.activeRow, "activeRow mismatch");
        G.Assert(previousRow==from_b.previousRow, "previousRow mismatch");       
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(robotVocabulary==from_b.robotVocabulary,"robotVocabulary mismatch");
        G.Assert(nLinesClaimed==from_b.nLinesClaimed,"lines claimed mismatch");
        G.Assert(maxLineTiles==from_b.maxLineTiles,"maxLineTiles mismatch");
        G.Assert(AR.sameArrayContents(skipTurn, from_b.skipTurn), "skipturn mismatch");
        G.Assert(revertJumbulayaPlayer==from_b.revertJumbulayaPlayer, "revertJumbulayaPlayer mismatch");
        G.Assert(revertJumbulayaState==from_b.revertJumbulayaState, "revertJumbulayaState mismatch");
        G.Assert(sameCells(rack,from_b.rack),"rack mismatch");
        G.Assert(sameCells(claimed,from_b.claimed),"claimed mismatch");
        G.Assert(sameCells(playerCell,from_b.playerCell),"playercell mismatch");
        G.Assert(sameCells(drawPile,from_b.drawPile),"drawPile mismatch");
        G.Assert(Word.sameWords(currentWord, from_b.currentWord),"current word mismatch");
        G.Assert(AR.sameArrayContents(score,from_b.score),"score mismatch");
        G.Assert(nPasses==from_b.nPasses,"nPasses mismatch");
        G.Assert(isPass==from_b.isPass,"isPass mismatch");
        G.Assert(needShuffle==from_b.needShuffle,"needshuffle mismatch");
        for(Option o : Option.values()) { G.Assert(getOptionValue(o)==from_b.getOptionValue(o),"Option %s mismatch",o); }
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
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedFromRack);
		v ^= Digest(r,revision);
		v ^= Digest(r,activeRow);
		v ^= Digest(r,previousRow);
		v ^= Digest(r,rack);
		v ^= Digest(r,drawPile);
		v ^= Digest(r,score);
		v ^= Digest(r,isPass);
		v ^= Digest(r,nPasses);
		v ^= Digest(r,claimed);
		v ^= Digest(r,nLinesClaimed);
		v ^= Digest(r,maxLineTiles);
		v ^= Digest(r,revertJumbulayaPlayer);
		v ^= Digest(r,revertJumbulayaState==null ? 0 : revertJumbulayaState.ordinal()*23550);
		v ^= Digest(r,skipTurn);
		v ^= Digest(r,resigned);
		v ^= Digest(r,playerCell);
		v ^= Digest(r,robotVocabulary);
		v ^= Digest(r,needShuffle);
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
        case CanJumbulaya:
        case ConfirmJumbulaya:
        case DiscardTiles:
        case Resign:
            moveNumber++; //the move is complete in these states
            boolean repeat = false;
            do { setWhoseTurn(nextPlayer());
                 repeat = skipTurn[whoseTurn] || resigned[whoseTurn];
                 if(repeat)
                 {
                	 logGameEvent(SkipTurnMessage,playerColor(whoseTurn).tip);
                 }
                 skipTurn[whoseTurn]=false;
            } while (repeat);
        }
    }

    /** this is used to determine if the "Done" button in the UI is live
     *
     * @return
     */
    public boolean DoneState()
    {	return(pickedObject==null && board_state.doneState());
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
    {	if(win[player]) { return(true); }
    	boolean win = false;
    	return(win);
    }


    // set the contents of a cell, and maintain the books
    public JumbulayaChip SetBoard(JumbulayaCell c,JumbulayaChip ch)
    {	JumbulayaChip old = c.topChip();
    	if(c.onBoard)
    	{
    	if(ch==null) 
    		{ 
    		if(old!=null) { c.removeTop();}
    		}
    	else { G.Assert(c.isEmpty(),"not expecting to make stacks");
    		c.addChip(ch);
    		}
    	setActiveRow();
    	}
    	else {
        	if(ch==null) 
        		{ 
        		  c.removeTop();
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
        activeRow = -1;
        pickedFromRack = false;
        
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private JumbulayaCell unDropObject(JumbulayaCell c)
    {	JumbulayaCell rv = droppedDestStack.remove(c,false);
    	setState(stateStack.pop());
    	if(rv==drawPile) 
    	{ pickedObject = drawPile.removeTop(); 
    	  pickedFromRack = true;
    	}
    	else {
    	JumbulayaChip po = SetBoard(rv,null); 	// SetBoard does ancillary bookkeeping
    	pickedObject = po;
    	pickedFromRack = rv.fromRack;
    	if(rv.rackLocation()==JumbulayaId.Rack)
    		{ int idx = c.col-'A';;
    		  int map[]=rackMap[idx];
    		  for(int i=0;i<map.length;i++) { if(map[i]==c.row) { map[i]=-1; break; }}   		
    		}
    	}
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject(JumbulayaCell rv)
    {	pickedSourceStack.pop();
    	setState(stateStack.pop());
    	switch(rv.rackLocation())
    	{
    	case PlayerCell:	// just disappear it
    		break;
    	default: 
        	SetBoard(rv,pickedObject);
    	}
    	pickedObject = null;
    }
    
    public JumbulayaCell lastDropped()
    {
    	return(droppedDestStack.top());
    }
    public boolean dropIsHorizontal()
    {
    	if(droppedDestStack.size()<2) { return(true); }
    	JumbulayaCell c1 = droppedDestStack.top();
    	JumbulayaCell c0 = droppedDestStack.elementAt(0);
    	return(c1.row==c0.row);
    }

    public void setActiveRow()
    {
    	int n = -1;
    	for(int lim=droppedDestStack.size()-1; n<0 && lim>=0; lim--)
    	{
    		JumbulayaCell c = droppedDestStack.elementAt(lim);
    		if(c.onBoard) { n = c.row; } 
    	}
    	for(int lim=pickedSourceStack.size()-1; n<0 && lim>=0; lim--)
    	{
    		JumbulayaCell c = pickedSourceStack.elementAt(lim);
    		if(c.onBoard) { n = c.row; } 
    	}
    	activeRow = n;
    }
       
    // 
    // drop the floating object.
    //
    private void dropObject(JumbulayaCell c)
    {
       stateStack.push(board_state);
      
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting dest " + c.rackLocation);
        case DrawPile:
            droppedDestStack.push(c);
            c.addChip(pickedObject);
            lastDroppedObject = pickedObject;
            pickedObject = null;
            break;
        case Rack:
        	G.Assert(c.topChip()==null && pickedObject!=null,"drop something on empty %s %s",c,pickedObject);
        	c.addChip(pickedObject);
        	if(board_state!=JumbulayaState.DiscardTiles) 
        		{ droppedDestStack.push(c);
        		}
        	c.fromRack = pickedFromRack;
        	pickedObject = null;
        	break;
        case PlayerCell:	// just disappear it
        	pickedObject = null;
        	break;
        case Claimed:
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        case EmptyBoard:
            droppedDestStack.push(c);
            c.fromRack = pickedFromRack;
            SetBoard(c,pickedObject);
            lastDroppedObject = pickedObject;
            pickedObject = null;
            break;
        }
       pickedFromRack = false;

     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(JumbulayaCell c)
    {
    	return droppedDestStack.top()==c;
    }
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { JumbulayaChip ch = pickedObject;
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
    private JumbulayaCell getCell(JumbulayaId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source " + source);
        case Claimed:
        	return(claimed[row-1]);
        case PlayerCell:
        	return(playerCell[row]);
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
    public JumbulayaCell getCell(JumbulayaCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(JumbulayaCell c)
    {	pickedSourceStack.push(c);
    	stateStack.push(board_state);
        switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting rackLocation " + c.rackLocation);
        case Claimed:
        	pickedObject = c.removeTop();
        	break;
        	
        case PlayerCell:	// the player color, only hit in puzzle mode
        	pickedObject = c.topChip();
        	pickedFromRack = false;
        	return;
  
		case DrawPile:
			lastPicked = pickedObject = c.removeTop();
			pickedFromRack = true;
			break;
        case Rack:
        	{
        	int idx = c.col-'A';
        	mapPick[idx]=-1;
        	mapTarget[idx]=-1;
        	int map[] = rackMap[idx];
        	validateMap(idx,"before pick");
        	for(int i=0;i<map.length;i++) { if(map[i]==c.row) { map[i]=-1; break; }} 
          	JumbulayaChip po = c.removeTop();
            lastPicked = pickedObject = po;
         	lastDroppedObject = null;
         	pickedFromRack = c.fromRack;
 			validateMap(c.col-'A',"after pick"); 
        	}
 			break;
        case BoardLocation:
        	{
        	JumbulayaChip po = c.topChip();
            lastPicked = pickedObject = po;
         	lastDroppedObject = null;
         	pickedFromRack = c.fromRack;
 			SetBoard(c,null);
        	}
            break;

        }
    }

    public boolean isSource(JumbulayaCell c)
    {
    	return(c==pickedSourceStack.top());
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(JumbulayaCell c,JumbulayaChip ch,replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state " + board_state);
        	
        case DiscardTiles:
        	if(droppedDestStack.size()==0) 
        		{ setState(resetState);        		
        		}
        	break;
        case Confirm:
        case CanJumbulaya:
        case Play:
        	if(c==drawPile) { setState(JumbulayaState.DiscardTiles); }
        	else if(validate(false))
        		{ 	if(canCallJumbulaya(true)) 
        				{
        				setState(JumbulayaState.CanJumbulaya); 
        				}
        			else {
        				setState(JumbulayaState.Confirm); 
        			}
        		}
        	else { setState(JumbulayaState.Play); }
			break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    
    private boolean rackIsEmpty(int who)
    {	
    	for(JumbulayaCell c : rack[who]) { if(c.topChip()!=null) { return(false); }}
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
		case ConfirmJumbulaya:
			setGameOver();
			break;
		case Gameover: break;
    	case Puzzle:
		case Confirm:
		case CanJumbulaya:
		case DiscardTiles:
		case Resign:
    	case Play:
    		if(rackIsEmpty(whoseTurn)||allPassed()) 
    			{ setGameOver();
    			}
    		else {
    			setState( JumbulayaState.Play);
    		}
    		
    		break;
    	}
       	resetState = board_state;
    }
    public int nextPlayer()
    {
    	return((whoseTurn+1)%players_in_game);
    }
    private void drawNewTiles_incorrect(replayMode replay)
    {	JumbulayaCell myrack[] = rack[whoseTurn];
    	JumbulayaCell mapped[] = mappedRack[whoseTurn];
    	int mymap[] = rackMap[whoseTurn];
    	int n=0;
    	for(JumbulayaCell c : myrack) 
    		{ c.fromRack = true;
    		  if(c.topChip()!=null) { n++; }
    		}
    	for(int i=0;i<mymap.length ;i++)
    	{	int ind = mymap[i];
    		if(ind>=0)
    		{	// fill into the lowest mapped slot that is empty
    			JumbulayaCell c = myrack[ind];
    			if(c.topChip()==null)
    			{ if(n<rackSize && drawPile.height()>0)
	    			{	JumbulayaChip newchip = drawPile.removeTop();
	    				c.addChip(newchip);
	    				n++;
	    				if(replay!=replayMode.Replay) 
	    				{ JumbulayaCell cd = mapped[ind];
	    				// the mapped cell is used only by the user interface and is loaded by
	    				// the user interface during redisplay, but we need to preload it so the
	    				// animation will know there is something to do.
	    				  cd.addChip(newchip);
	      				  animationStack.push(drawPile);
	      				  animationStack.push(cd);
	    				}
	    			}
    			else {
    				mymap[i]=-1;
    			}
    			}
	    			
    		}
    	}
    }
    private void drawNewTiles_correct(replayMode replay)
    {	JumbulayaCell myrack[] = rack[whoseTurn];
    	JumbulayaCell mapped[] = mappedRack[whoseTurn];
    	int mymap[] = rackMap[whoseTurn];
    	int n=0;
    	for(JumbulayaCell c : myrack) 
    		{ c.fromRack = true;
    		  if(c.topChip()!=null) { n++; }
    		}
    	validateMap(whoseTurn,"before draw");
    	// fill the rack from 0, then fill mapping slots
    	for(int rackIdx=0;n<rackSize && rackIdx<myrack.length;rackIdx++)
    	{	JumbulayaCell c = myrack[rackIdx];
    		if(c.topChip()==null)
    		{	JumbulayaChip newchip = drawPile.removeTop();
    			c.addChip(newchip);
				n++;
				if(replay!=replayMode.Replay) 
				{
				  // the mapped cell is used only by the user interface and is loaded by
				  // the user interface during redisplay, but we need to preload it so the
				  // animation will know there is something to do.
				  JumbulayaCell cm = mapped[rackIdx];
				  cm.addChip(newchip);
  				  animationStack.push(drawPile);
  				  animationStack.push(cm);
				}
				
				int empty = -1;
				boolean filled = false;
				for(int mapidx = 0;!filled && mapidx<mymap.length; mapidx++)
				{	int mapv = mymap[mapidx];
					if(mapv==rackIdx) { filled = true; }
					else if((empty<0) && (mapv<0)) { empty = mapidx; }
				}
				if(!filled) { mymap[empty] = rackIdx; }
					
				}
    		}
    } 
    
    private void drawNewTiles(replayMode replay)
    {	// revision 101 fixes the problem that players sometimes see different
    	// tile draws because of disagreements about the order of the rack
    	if(revision<101)
    	{
    		drawNewTiles_incorrect(replay);
    	}
    	else 
    	{
    		drawNewTiles_correct(replay);
    	}
    	validateMap(whoseTurn,"after draw");

    }
    
    StringBuilder builder = new StringBuilder();
	WordStack words = new WordStack();
	
	public String invalidReason=null;
	public Word previousWord = null;
	public Word pendingWord = null;
	
	
	/* 
	 * this validates that all the lines on the board are words, and are all
	 * connected, the list of new words, the list of words, and the list of non words.
	 * if andScore is true, it also calculates the score change and the lists
	 * of word caps.
	 * 
	 * @param andScore also commit and score the word
	 * @return true if the word is acceptable
	 */
   private synchronized boolean validate(boolean andScore)
    {	invalidReason = null;
	   	if(activeRow<0) { return(false); }
	   	Word word = findValidWord(getCell('A',activeRow));
	   	// count the letters from rack
	   	int rackCells = 0;
	   	if(word!=null)
	   		{ JumbulayaCell seed = word.seed;
	   		  while(seed!=null && seed.topChip()!=null)
	   		  {
	   			  if(seed.fromRack) { rackCells++; }
	   			  seed = seed.exitTo(CELL_RIGHT);
	   		  }
	   		  Word current = getCurrentWord(activeRow);
	   		  if(current.nTiles > word.nTiles)
	   		  {
	   			  invalidReason = FewerTiles;
	   			  return(false);
	   		  }
	   		  if(rackCells>=3)
	   		  {
	   			invalidReason = TooManyRack;
	   			return(false);
	   		  }
	   		  if(current.containsInHistory(word.name))
	   		  {
	   			  invalidReason = DuplicateWord;
	   			  return(false);
	   		  }
	   		  if(andScore)
	   		  	{ setCurrentWord(activeRow,word); 
	   		  	  word.points = word.wordScore();
	   		  	}
	   		  pendingWord = word;
	   		  return(true);
	   		}
	   	else { invalidReason = InvalidWord; }
	   	return(false);
    }

   	// 
   	// collect a word from a starting cell with a given direction
   	// 
    private Word collectWord(JumbulayaCell from,boolean endcheck)
    {
    	builder.setLength(0);
    	int nTiles=0;		// count of tiles
     	JumbulayaCell c = from;
    	while(c!=null) 
    	{
    		JumbulayaChip top = c.topChip();
    		if(top==null) {  break; }
    		int exc = top.extendedCharCode();
    		builder.append((char)(exc&0xff));
    		if(exc>=0xff) { builder.append((char)(exc>>8)); }
    		nTiles++;
    		c = c.exitTo(CELL_RIGHT);
    	}
    	if(endcheck)
    	{
    		while(c!=null)
    			{ if(c.topChip()!=null) { return(null); }
    			  c = c.exitTo(CELL_RIGHT);
    			}
    	}
    	// leave builder primed with the letters, so it can be reversed
    	if(nTiles>0)
    	{
    	 return(new Word(from,nTiles,builder.toString()));
    	}
    	return(null);
    }
    
    // get a word starting from a cell that is not empty.
    // for Jumbulaya we always move left to right
    private Word findCurrentWord(JumbulayaCell from)
    {
   	 while(from!=null && from.topChip()==null) 
   	 {
   		 from = from.exitTo(CELL_RIGHT);
   	 }
   	 G.Assert(from!=null,"no letters!");
   	 return(collectWord(from,false));
    }
    
    private Word findValidWord(JumbulayaCell from)
    {
      	 while(from!=null && from.topChip()==null) 
       	 {
       		 from = from.exitTo(CELL_RIGHT);
       	 }
      	 Word word = collectWord(from,true);
      	 if(word!=null)
      	 {	Entry entry = dictionary.get(word.name);	// lookup with an unlimited vocabulary
      	 	if(entry!=null) { return(word); }
      	 }
      	 return(null);
    }
    
    // scan the board and get the current word in each row.
    // initially some won't be words.
    private void setCurrentWords()
    {
    	JumbulayaCell c = getCell('A',1);
    	while(c!=null) 
    		{ 
    		  setCurrentWord(c.row,findCurrentWord(c)); 
    		  c = c.exitTo(CELL_UP);
    		}	    
    }
 
    public JumbulayaChip playerColor(int who)
    {
    	return JumbulayaChip.Colors[getColorMap()[who]];
    }
    private boolean totalControl()
    {	// true if one player owns all the lines
    	if(nLinesClaimed==NROWS)
    	{	JumbulayaChip ch = claimed[0].topChip();
    		for(JumbulayaCell c : claimed) { if(ch!=c.topChip()) { return(false); }}
    		return(true);
    	}
    	return(false);
    }
    public Word collectJumbulaya()
    {	StringBuilder b = new StringBuilder();
    	CellStack cells = new CellStack();
    	int ntiles = 0;
    	for(int lim=NROWS; lim>0; lim--)
    	{	Word cw = getCurrentWord(lim);
    		JumbulayaCell c = cw.seed;
    		JumbulayaChip top = null;
    		while(c!=null && ((top=c.topChip())!=null))
    		{
    			if(c.getSelected())
    			{	ntiles++;
    				c = null;
    				b.append(top.letter);
    				cells.push(c);
    			}
    			else { c=c.exitTo(CELL_RIGHT); }
    		}}
    	
    	String name = b.toString().toLowerCase();
    	if((Word.jumbulayaScore(ntiles)>0)
    			&& Dictionary.getInstance().get(name)!=null)
    	{	return(new Word(cells,name));
    	}
    	return(null);
    }
    
    private void doDone(Jumbulayamovespec m,replayMode replay)
    {	Word currentWord = null;
    	if(activeRow>0)
    	{
    		setUnclaimed(activeRow);	// unclaim and revert the score
    	}
    	validate(true);
    	JumbulayaCell dest = droppedDestStack.top();
    	if((revision>=102) && (dest!=null) && (dest.rackLocation()==JumbulayaId.DrawPile))
    	{
    		needShuffle = true;
    	}
    	if(activeRow>0)
		{ 
    	
		setClaimed(activeRow,whoseTurn); 
		currentWord = getCurrentWord(activeRow);
		previousWord = currentWord;
		pendingWord = null;
		logGameEvent(PlayWord,currentWord.name);
		}
    	if(board_state==JumbulayaState.ConfirmJumbulaya)
    	{
    		Word w = collectJumbulaya();
    		G.Assert(w!=null,"must be a jumbulaya");
    		score[whoseTurn] += w.jumbulayaScore();
    		logGameEvent(PlayJumbulaya,w.name);
    		previousWord = w;
    		skipTurn[whoseTurn]=false;
    		
    	}
    	// mark everything as stable
    	for(JumbulayaCell c = allCells; c!=null; c=c.next) { c.fromRack = false; }
    	previousRow = activeRow;
        acceptPlacement();
        
        if(needShuffle)
        {	needShuffle = false;
        	drawPile.shuffle(new Random(randomKey+moveNumber*100));
        }

        drawNewTiles(replay);
        
        if(isPass)
        { nPasses++;
          isPass = false;
        }
        else { nPasses=0; }
        
        if((m.op==MOVE_JUMBULAYA)
        		|| ((currentWord!=null) && (currentWord.nTiles==NCOLS))
        		|| totalControl()
        		)
        {
        	setGameOver();
        }
        else if (board_state==JumbulayaState.Resign)
        {	doResignPlayer(replay);
        }
        else
        {	setNextPlayer(replay);
        	setNextStateAfterDone(replay);
         }
    }
    public void doResignPlayer(replayMode replay)
    {
    	resigned[whoseTurn] = true;
    	score[whoseTurn] = -1;
    	int res = 0;
    	int playing = -1;
    	
    	for(int i=0;i<players_in_game;i++) { if(resigned[i]) { res++; } else { playing = i; }}
    	if(res+1==players_in_game)
    	{
    		// sole remaining player is the winner
    		win[playing] = true;
    		setGameOver();
    	}
    	else { 
    		setNextPlayer(replay);
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
		case OpenRacks: return(openRacks);
		}
	}
	public void setOptionValue(Option o,boolean v)
	{
    	switch(o)
    	{
    	default: throw G.Error("Not expecting %s",o);
    	case OpenRacks: openRacks = v; break;
    	}
	}
	private void placeWord(JumbulayaCell from,JumbulayaCell rack[],String word,replayMode replay)
	{	JumbulayaCell c = from;
		placeFromRack(getCurrentWord(c.row),rack,word,from,replay);
	}
	private void doStopJumbulaya(replayMode replay)
	{
    	int who = whoseTurn;
    	whoseTurn = revertJumbulayaPlayer;     
    	skipTurn[whoseTurn] = true;
    	for(JumbulayaCell c = allCells ; c!=null; c=c.next) { c.setSelected(false); }
    	setState(JumbulayaState.Play);
    	
    	if(who==whoseTurn)
    		{
    		setNextPlayer(replay);
    		
    		if(revertJumbulayaState==JumbulayaState.CanJumbulaya)
    			{
    			// already got our word, so skip next time
    			skipTurn[who] = true;
    			}      		
    		}
    		else 
    		{
    		skipTurn[who] = true;     	               			
    		}
	}
	public void dropAndSlide(int who,JumbulayaCell from,int moving0,int pick0,int dest0,replayMode replay)
	{	// the map is invalid when we enter, because we've dropped a tile
		// and haven't mapped it yet.
		JumbulayaCell rcells[] = mappedRack[who];
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
		validateMap(who,"after drop and slide");
	}
	
    public boolean Execute(commonMove mm,replayMode replay)
    {	Jumbulayamovespec m = (Jumbulayamovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }

        //G.print("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
        case MOVE_DONE:
        	if(G.debug())
        	{
        		G.Assert(DoneState(),"not in a DONE state");
        	}
         	doDone(m,replay);

            break;
        case MOVE_STOPJUMBULAYA:
        	doStopJumbulaya(replay);
        	break;
        case MOVE_STARTJUMBULAYA:
        {	revertJumbulayaState = board_state;
        	if(board_state==JumbulayaState.CanJumbulaya)
        	{	int who = whoseTurn();
        		doDone(m,replayMode.Replay);
        		revertJumbulayaPlayer = whoseTurn;
        		whoseTurn = who;
        	}
         	setState((board_state==JumbulayaState.Jumbulaya)
        			? resetState
        			: JumbulayaState.Jumbulaya);
         	String colorName = m.word;
         	if(colorName!=null)
         	{
         		JumbulayaChip color = JumbulayaChip.findColor(colorName);
         		if(color!=null)
         		{
         			int owner = playerOwning(color);
         			revertJumbulayaPlayer = whoseTurn;
         			whoseTurn = owner;
         		}
         	}
        }
        	break;
        case MOVE_SELECT:
        	{
        	JumbulayaCell dest = getCell(m.to_col,m.to_row);
        	JumbulayaCell src = getCell('A',m.to_row);
        	while(src!=null)
        		{
        		src.setSelected(src==dest?!src.getSelected():false);
        		src = src.exitTo(CELL_RIGHT);
        		}
        	}
        	setState(collectJumbulaya()!=null ? JumbulayaState.ConfirmJumbulaya : JumbulayaState.Jumbulaya);
        	break;
        	
        case MOVE_JUMBULAYA:
        	{
        	for(JumbulayaCell c = allCells; c!=null; c=c.next) { c.setSelected(false); }
        	CellStack path = m.path;
        	for(int lim = path.size()-1; lim>=0; lim--)
        		{
        		JumbulayaCell c = getCell(path.elementAt(lim));
        		c.setSelected(true);
        		}
        	setState(JumbulayaState.ConfirmJumbulaya);
        	}
        	break;
        case MOVE_PLAYWORD:
        	{
        	JumbulayaCell c = getCell(m.to_col,m.to_row);
        	JumbulayaCell r[] = rack[whoseTurn];
        	placeWord(c,r,m.word,replay);  
        	int map[] = rackMap[whoseTurn];
        	for(int lim=map.length-1; lim>=0; lim--)
        	{	int idx = map[lim];
        		if((idx>=0) && (r[idx].topChip()==null)) { map[lim]=-1; }
        	}
        	//validateMap(whoseTurn);
        	activeRow = c.row;
        	setNextStateAfterDrop(null,null,replay);
        	}
        	break;
        case MOVE_PASS:
        	isPass = false;
        	switch(board_state)
        	{
        	case Jumbulaya:
        		// trying to pass out of a jumbulaya state, not permitted
        		doStopJumbulaya(replay);
        		break;
        	case Confirm:
        		setState(resetState);
        		break;
        	default:
        		setState(JumbulayaState.Confirm);
        		isPass = true;
        	}

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
        case MOVE_DROPB:
        	{
			JumbulayaCell src = pickedSourceStack.top();
			JumbulayaChip po = pickedObject;
			JumbulayaCell dest =  getCell(JumbulayaId.BoardLocation,m.to_col,m.to_row);
			
			if(isSource(dest)) 
				{ unPickObject(dest); 
				  validate(false);
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
	            if((replay==replayMode.Single) && (po!=null))
	            	{ animationStack.push(src);
	            	  animationStack.push(dest); 
	            	}
				}
			setNextStateAfterDrop(dest,po,replay);

        	}
             break;
        case MOVE_DROPFROMBOARD:
        	// drop a letter on the rack which was originally picked from the board
        	// if remote screens are in use, it's possible a tile can be dropped twice.
        	// the current requirement is that the dest cell will be the same, and the
        	// visible cell doesn't matter, so the second drop can be turned into a noop
        	if(pickedObject!=null)
        	{
 	    	int who = m.to_col-'A';
	    	int slot = m.to_row;
	       	validateMap(who,"before dropfromboard");
	    	JumbulayaCell dcell = getCell(JumbulayaId.Rack,m.to_col,slot);
	    	int dest = m.from_row; 
	    	JumbulayaChip ch = pickedObject;
	    	dropObject(dcell);
	    	dropAndSlide(who, pickedSourceStack.top(),slot,-1,dest,replay);
	    	setNextStateAfterDrop(dcell,ch,replay);
        	validateMap(who,"after dropfromboard");
        	}
        	else 
        	{ G.print("Emptry drop from board "+m ); 
        	  m.op = MOVE_CANCELLED;
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
	    	JumbulayaCell rcells[] = mappedRack[who];
	    	validateMap(who,"before replace");
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
        case MOVE_DISCARDRACK:
        	for(JumbulayaCell c : rack[whoseTurn])
        	{
        		if(c.topChip()!=null)
        		{
        			drawPile.addChip(c.removeTop());
        			if(replay!=replayMode.Replay)
        			{
        				animationStack.push(c);
        				animationStack.push(drawPile);
        			}
        		}
        	}
        	AR.setValue(rackMap[whoseTurn],-1);
        	needShuffle = true;
        	setState((board_state==JumbulayaState.Confirm) ? resetState : JumbulayaState.Confirm);
        	isPass = (board_state==JumbulayaState.Confirm);
        	break;
        case MOVE_MOVETILE:
        	{
        	validateMap(whoseTurn,"before move");
        	JumbulayaCell src = getCell(m.source,m.from_col,m.from_row);
        	JumbulayaCell dest = getCell(m.dest,m.to_col,m.to_row);
        	if(isDest(src) && isSource(dest)) 
        	{ validateMap(whoseTurn,"before unmove");
        	  unDropObject(src);
        	  validateMap(whoseTurn,"mid unmove");
        	  unPickObject(dest); 
        	  validateMap(whoseTurn,"after unmove");
        	}
        	else
        		{pickObject(src); 
        		JumbulayaChip po = m.chip = pickedObject;
        		dropObject(dest);
        		validateMap(whoseTurn,"after move");

	        	// no animation needed because this is really from 
	        	// a pick/drop pair sourced in the rack
	            if(replay==replayMode.Single)
	        	{ animationStack.push(src);
	        	  animationStack.push(dest); 
	        	}
 
	            setNextStateAfterDrop(dest,po,replay);
        		}
        	}
        	break;
        case MOVE_PICK:		// pick from the draw pile
 		case MOVE_PICKB:	// pick from the board
        	// come here only where there's something to pick, which must
 			{
 			JumbulayaCell src = getCell(m.dest,m.to_col,m.to_row);
 			if(isDest(src)) 
 				{	
 					unDropObject(src); 
 				}
 			else
 			{
        	// be a temporary p
        	pickObject(src);
        	m.chip = pickedObject;
        	switch(board_state)
        	{
        	case Confirm:
        	case CanJumbulaya:
        		setState(resetState);
				//$FALL-THROUGH$
           	case Puzzle:
			case Play:
        		break;
        	default: ;
        	}}}
    		validate(false);
    		break;

        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
        	JumbulayaCell dest = getCell(m.dest,m.to_col,m.to_row);
        	if(isSource(dest)) 
        		{ JumbulayaChip po = pickedObject;
        		  unPickObject(dest);
        		  setNextStateAfterDrop(dest,po,replay);
        		}
            else 
            { dropObject(dest); 
            }
        	}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(JumbulayaState.Puzzle);	// standardize the current state
            setNextStateAfterDone(replay);

            break;

       case MOVE_RESIGN:
    	   	if(board_state==JumbulayaState.Resign)
    	   		{	setState(unresign);
    	   			unresign = null;
    	   		}
    	   	else { unresign = board_state;
    	   			setState(JumbulayaState.Resign);
    	   	}
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(JumbulayaState.Puzzle);
 
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(JumbulayaState.Gameover);
    	   break;
       case MOVE_DROPONRACK:
    	   // remote screens drop a tile on the rack.
    	   // we do nothing now, but this gets transmitted back to the main.
    	   break;
        default:
        	cantExecute(m);
        }

        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }
        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }
    public boolean LegalToHitPool(boolean picked)
    {
    	switch(board_state)
    	{
    	case DiscardTiles:
    		return(true);
    	case Jumbulaya:
    	case ConfirmJumbulaya:
    	default:
    		return(false);
    	case Confirm:
    	case Play:
    		// pickedObject will be null when we have picked something
    		// that isn't from the rack
    		return( picked 
    					? (pickedObject==null) && (droppedDestStack.size()==0)
    					: (lastDropped()==drawPile));
    	case Puzzle:
    		return(!picked ? drawPile.height()>0 : true);
    	}
    }
    // legal to hit the chip storage area
    public boolean LegalToHitChips(JumbulayaCell c)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting Legal Hit state " + board_state);
        case Confirm:
        case CanJumbulaya:
		case DiscardTiles:
        case Play:
        	return ( (c!=null) 
         			 && ((pickedObject==null) ? (c.topChip()!=null) : true)
        			);
 		case Resign:
		case Gameover:
		case Jumbulaya:
		case ConfirmJumbulaya:
			return(false);
        case Puzzle:
            return ((c!=null) && ((pickedObject==null) == (c.topChip()!=null)));
        }
    }
    public boolean LegalToHitBoard(JumbulayaCell c,boolean picked)
    {	if(c==null) { return(false); }
    	// once we hit some tile on a row, that is the only row we can hit
    	
        switch (board_state)
        {
        case CanJumbulaya:
		case Confirm:
		case Play:
		   	if(activeRow>0 && c.row!=activeRow) { return(false); }
		   	return(picked == c.isEmpty());
        case Puzzle:
        	return(picked == c.isEmpty());
        case Jumbulaya:
        case ConfirmJumbulaya:
        	return(!c.isEmpty());
        	
		case Gameover:
		case Resign:
		case DiscardTiles:
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
    public void RobotExecute(Jumbulayamovespec m)
    {
        robotState.push(board_state); //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        Execute(m,replayMode.Replay);
        
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(Jumbulayamovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	JumbulayaState state = robotState.pop();
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



 // find words that can be placed in a different direction across an existing word.  This is used
 // both to place ordinary crosswords and to place "cap words" where a letter is being added to 
 // the beginning or end of an existing word.
 private void checkWords(WordStack saveWords,JumbulayaCell rack[],long letterMask,boolean unclaimedOnly)
 {	
	 for(Word cw : currentWord)
	 {	if(!unclaimedOnly || (cw.predecessor==null))
		 checkRow(saveWords,rack,letterMask,cw);
	 }
 }
 
 IStack pairsFromBoard = new IStack();
 IStack pairsFromRack = new IStack();
 
 // build a table of letter pairs from a row on the board
 private boolean getPairsFromBoard(JumbulayaCell c)
 {	boolean hasPairs = false;
	 pairsFromBoard.clear();
	// collect the pairs from the board, if any
	 JumbulayaChip top;
	 while((c!=null) && ((top=c.topChip())!=null))
	 {	int exc = top.extendedCharCode();
	 	if(exc>0xff) 
	 		{
	 		pairsFromBoard.push(exc); 
	 		hasPairs = true; 
	 		}
		 c = c.exitTo(CELL_RIGHT);
	 }
	 return(hasPairs);
 }
 // build a table of letter pairs from a player rack
 private boolean getPairsFromRack(JumbulayaCell rack[])
 {	boolean hasRackPairs = false;
	pairsFromRack.clear();
		// collect the pairs from the rack, if any
		for(JumbulayaCell c : rack)
		{
			JumbulayaChip top = c.topChip();
			if(top!=null)
			{	int exc = top.extendedCharCode();
				if(exc>0xff) 
					{ 	
					pairsFromRack.push(exc); 
					hasRackPairs = true;
					}
			}
		}
		return(hasRackPairs);
 }

//
// do the actual placement of a word blessed by testFromRack
// placeAt is where to actually place it.
// this was originally rolled into combined function with testFromRack, but it
// became too complicated.  Be aware that any changes or bug fixes here likely
// also require changes to testFromRack
// 
 ChipStack boardReplacementChars = new ChipStack();
 CellStack boardReplacementCells = new CellStack();
 private Word placeFromRack(Word current,JumbulayaCell rack[],String targetWord,JumbulayaCell placeAt,replayMode replay)
 {	
	boolean failed = false;
	boolean hasBoardPairs = getPairsFromBoard(current.seed);
	boolean hasRackPairs = getPairsFromRack(rack);

	JumbulayaCell seed = current.seed;
	
	{
	// load the replacement stack with the letters in the word
	// and clear the word
	boardReplacementChars.clear();
	boardReplacementCells.clear();
	JumbulayaChip top=null;	
	while(seed!=null && ((top=seed.topChip())!=null)) 
		{ // load the replacement list with the current word
		boardReplacementChars.push(top); 
		boardReplacementCells.push(seed);
		seed.removeTop();
		seed = seed.exitTo(CELL_RIGHT); 
		}}

	JumbulayaCell place = placeAt;
	String reason="";
	int nTiles = 0;
	
	for(int charIndex = 0,lastCharIndex = targetWord.length()-1;!failed && charIndex<=lastCharIndex;charIndex++)
	{	boolean matched = false;
		char ch = targetWord.charAt(charIndex);
		// match a letter pair on the board
		if(hasBoardPairs && (charIndex<lastCharIndex))
		{
			int charPair = JumbulayaChip.makeExc(ch, targetWord.charAt(charIndex+1));
			for(int idx = boardReplacementChars.size()-1; !matched && idx>=0; idx--)
			{	
				JumbulayaChip top = boardReplacementChars.elementAt(idx);
				int exc = top.extendedCharCode();
				if(exc==charPair)
					{
					charIndex++;
					nTiles++;
					matched = true;
					// replace can leave holes in the word
					placeAt.addChip(top);
					boardReplacementChars.remove(idx,false);	
					JumbulayaCell from = boardReplacementCells.remove(idx, false);
					if((from!=placeAt) && (replay!=replayMode.Replay))
					{
						animationStack.push(from);
						animationStack.push(placeAt);
					}
					placeAt = placeAt.exitTo(CELL_RIGHT);
			}}
		}
		for(int idx = boardReplacementChars.size()-1; !matched && idx>=0; idx--)
		{	
			JumbulayaChip top = boardReplacementChars.elementAt(idx);
			int exc = top.extendedCharCode();
			if(exc==ch)
				{
				nTiles++;
				matched = true;
				// replace can leave holes in the word
				placeAt.addChip(top);
				boardReplacementChars.remove(idx,false);	
				JumbulayaCell from = boardReplacementCells.remove(idx, false);
				if((from!=placeAt) && (replay!=replayMode.Replay))
				{
					animationStack.push(from);
					animationStack.push(placeAt);
				}
				placeAt = placeAt.exitTo(CELL_RIGHT);
		}}
		
		// match with a letter pair on the board
		if(hasRackPairs && charIndex<lastCharIndex)
		{
		int charPair = JumbulayaChip.makeExc(ch, targetWord.charAt(charIndex+1));
		for(int ridx = rack.length-1; !matched && ridx>=0; ridx--)
		{	JumbulayaCell c = rack[ridx];
			JumbulayaChip top = c.topChip();
			if(top!=null)
			{	int exc = top.extendedCharCode();
				if(exc==charPair)
				{	charIndex++;	// eat an extra char
					matched = true;
					nTiles++;
					placeAt.addChip(top);
					c.removeTop();
					if(replay!=replayMode.Replay)
					{
						animationStack.push(c);
						animationStack.push(placeAt);
					}

					placeAt = placeAt.exitTo(CELL_RIGHT);
				}
			}}}

				
		// match with a letter from the rack
		for(int ridx = rack.length-1; !matched && ridx>=0; ridx--)
		{	JumbulayaCell c = rack[ridx];
			JumbulayaChip top = c.topChip();
			if(top!=null)
			{	int exc = top.extendedCharCode();
				if(exc==ch)
				{
					matched = true;
					nTiles++;
					placeAt.addChip(top);
					c.removeTop();
					if(replay!=replayMode.Replay)
					{
						animationStack.push(c);
						animationStack.push(placeAt);
					}

					placeAt = placeAt.exitTo(CELL_RIGHT);
				}
			}
		}
		
		if(!matched)
		{ failed = true;
		  reason = "failed to place "+ch;
		}
	}	// end of letter loop

	if(!failed)
	{	
		while(boardReplacementChars.size()>0)
		{	// push any remaing letters from the board back to the rack (trade)
			JumbulayaChip ch = boardReplacementChars.pop();
			JumbulayaCell from = boardReplacementCells.pop();
			// move this back to the rack
			// there must be an empty slot on the rack
			int map[] = rackMap[whoseTurn];
			boolean placed = false;
			for(int i=0;!placed && i<map.length;i++)
			{	int idx = map[i];
				if((idx>=0) && (rack[idx].topChip()==null))
				{
					rack[idx].addChip(ch);
					placed = true;
					if(replayMode.Replay!=replay)
					{
						animationStack.push(from);
						animationStack.push(rack[idx]);
					}
				}
			}
			G.Assert(placed,"placing %s at %s didn't place spare letter %s from the board",
						targetWord,placeAt,ch);
			}

		return(new Word(place,nTiles,targetWord,current));			
	}
	G.Assert(!failed, "placing word %s at %s: %s",targetWord,place,reason);
	return(null);
 }
 
// check if a plausible word can actually be placed, except for the duplicate word test
// but for jumbulaya it can only require 2 letters from the rack
// and must make a new word
// and must require changing at most 2 of the tiles in the current word.
// and must not decrease the number of tiles.
// if placeAt is not null actually place the word 
//
// any changes or bug fixes in this method probably also need to be made
// in the placeFromRack method
//
// this probably doesn't find all possible words, because it absolutely
// prioritizes letters in this order
// pairs-from-board singles-from-board pairs-from-rack singles-from-rack
//
private Word testFromRack(Word current,JumbulayaCell rack[],String targetWord)
{	int tilesFromBoard = 0;
	int lettersFromBoardMask = 0;
	int lettersFromRack = 0;
	int lettersFromRackMask = 0;
	boolean failed = false;
	boolean hasBoardPairs = getPairsFromBoard(current.seed);
	boolean hasRackPairs = getPairsFromRack(rack);
	//if(targetWord.equals("smother") && (current.seed.row==6))
	//		{
	//		G.print("M");
	//		}
	for(int charIndex = 0,lastCharIndex = targetWord.length()-1;!failed && charIndex<=lastCharIndex;charIndex++)
	{	boolean matched = false;
		int ch = targetWord.charAt(charIndex);
		
		if(hasBoardPairs && charIndex<lastCharIndex)
		{
		// match with a letter pair on the board
		int charPair =JumbulayaChip.makeExc(ch, targetWord.charAt(charIndex+1));
		{	
			JumbulayaCell c =  current.seed;
			int mask = 1;
			JumbulayaChip top = null;
			while(!matched && (c!=null) && ((top=c.topChip())!=null))
			{	
				if((mask&lettersFromBoardMask)==0)	// haven't used this tile yet
				{	int exc = top.extendedCharCode();
					if(exc==charPair)
					{	charIndex++;
						tilesFromBoard++;
						matched = true;
						lettersFromBoardMask|=mask;		
						
					}
				}
				c = c.exitTo(CELL_RIGHT);
				mask = mask<<1;			
			}}}
		{	// match with a letter on the board
			JumbulayaCell c =  current.seed;
			int mask = 1;
			JumbulayaChip top = null;
			while(!matched && (c!=null) && ((top=c.topChip())!=null))
			{	
				if((mask&lettersFromBoardMask)==0)	// haven't used this tile yet
				{	int exc = top.extendedCharCode();
					if(exc==ch)
					{	
						tilesFromBoard++;
						matched = true;
						lettersFromBoardMask|=mask;		
						
					}
				}
				c = c.exitTo(CELL_RIGHT);
				mask = mask<<1;			
			}}
		
		if(hasRackPairs && charIndex<lastCharIndex)
		{	int charPair =JumbulayaChip.makeExc(ch, targetWord.charAt(charIndex+1));
			// match with a letter pair from the rack
			for(int ridx = rack.length-1, mask = 1; !matched && ridx>=0; ridx--,mask = mask<<1)
			{	JumbulayaCell c = rack[ridx];
				JumbulayaChip top = c.topChip();
				if(top!=null && (mask&lettersFromRackMask)==0)
				{	int exc = top.extendedCharCode();
					if(exc==charPair)
					{	charIndex++;
						lettersFromRack++;
						matched = true;
						lettersFromRackMask|=mask;											
						if(lettersFromRack>2) 
							{ failed = matched = true; 
							}
					}
				}
			}
		}
				
		// match with a letter from the rack
		for(int ridx = rack.length-1, mask = 1; !matched && ridx>=0; ridx--,mask = mask<<1)
		{	JumbulayaCell c = rack[ridx];
			JumbulayaChip top = c.topChip();
			if(top!=null && (mask&lettersFromRackMask)==0)
			{	int exc = top.extendedCharCode();
				if(exc==ch)
				{
					lettersFromRack++;
					matched = true;
					lettersFromRackMask|=mask;											
					if(lettersFromRack>2) 
						{ failed = matched = true; 
						}
				}
			}
		}
		failed |= !matched;
	}	// end of letter loop
	failed |= (tilesFromBoard==0);
	failed |= (tilesFromBoard+lettersFromRack)<current.nTiles;
	if(!failed)
	{	
		// check that the word didn't previously exist in the row
		if(!current.containsInHistory(targetWord))
		{
			int nt = tilesFromBoard+lettersFromRack;
			if(nt>=current.nTiles)	// the number of tiles is not allowed to shrink
			{
			JumbulayaCell seed = current.seed;
			{
			int tnt = nt;
			while((tnt>current.nTiles) && seed.col>'A') 
				{ seed = getCell((char)(seed.col-1),seed.row); 
				  tnt--;
				}}
			return(new Word(seed,nt,targetWord));	
			}
		}
	}
	return(null);
}


 
 // rack is a player rack, with its lettermask precomputed
 // current is the current word on some row of the board
 private int checkRow(WordStack saveWords,JumbulayaCell rack[],long letterMask,Word current)
 {	
	 int total = 0;
	 long rowMask = Dictionary.letterMask(letterMask, current.name);	// mask that includes the rack and the letters on the board
	 int minTiles = current.nTiles;
	 int maxTiles = Math.min(NCOLS, current.name.length()+4);	// could add 2 tiles with 2 letters each
	 Dictionary dict = Dictionary.getInstance();
	 int baseScore = staticEval(whoseTurn);
	 
	 for(int len = minTiles; len<=maxTiles; len++)
	 {
		 DictionaryHash subDictionary = dict.getSubdictionary(len);
		 for(Enumeration<Entry> words = subDictionary.elements(); words.hasMoreElements();)
		 {
			 Entry word = words.nextElement();
			 if(word.order<robotVocabulary								// within the vocabulary limit 
				&& ((word.letterMask & ( letterMask|rowMask))==word.letterMask))
			 {	String targetWord = word.word;
			 	{
				 Word placed = testFromRack(current,rack,targetWord);
				 if((placed!=null)
						 // check for duplicates after verifying placement, it should be a rare
						 // occurrence
						)// must be a new word
				 {	
					 int value = placed.wordScore();
					 int owner = rowOwner(placed.seed);
					 if(owner==whoseTurn)
					 {	// if we already own the row, we get fewer points
						 value -= current.wordScore();
					 }
					 else if((owner==-1) && baseScore>0)
					 	{ // if we're ahead, value claiming new rows
						 value += VALUEOFROW; 
					 	}
					 
					 int score = placed.points = value+baseScore;
					 boolean skip = false;
					 boolean gameEnd = (placed.nTiles==NCOLS);		// this would end the game
					 if(gameEnd) 
					 {	if(score>0) { score+=900; }
					 	else { skip = true; }
					 }
					 if(score<=robotWinThreshold)
					 {	// after playing this word, we're still behind
						
						if((placed.nTiles>=NCOLS-2)
								&& (score-Word.wordScore(NCOLS))<0)
						{	// this is a 8 or more letter word, and someone
							// could flip this, make a 10 letter word that wins
							skip = true;
						}
						
					 }
					 if(!skip)
						 {saveWords.recordCandidate("Ordinary Word",	placed,word);
						 total++;
						 }
					 if((owner<0) && ( nLinesClaimed==NROWS-1))
					 {
						 // claiming the last row would enable jumbulayas
						 skip = true;
					 }
				 }
			 	}
			 }
		 }
	 }

	 return(total);
 }


 // this generates a mask in the same manner as Dictionary.letterSet, but
 // uses a rack as the source
 private long letterSet(JumbulayaCell rack[])
	{	long s = 0;
		for(JumbulayaCell c : rack)
		{
			JumbulayaChip ch = c.topChip();
			if(ch!=null)
			{	int exc = ch.extendedCharCode();
				s = Dictionary.letterMask(s,(char)(exc&0xff));
				if(exc>0xff)
				{
					s = Dictionary.letterMask(s, (char)(exc>>8));
				}				
			}
		}
		return(s);
	}



 private void checkWords(WordStack saveWords,JumbulayaCell rack[],boolean unclaimedOnly)
 {	
	 // check ordinary crosswords
     long letterMask = letterSet(rack);
     checkWords(saveWords,rack,letterMask,unclaimedOnly);		// ordinary crosswords
 }
 
 private WordStack checkWordsCore(boolean unclaimedOnly)
 {	WordStack words = new WordStack();
 	checkWords(words,rack[whoseTurn],unclaimedOnly);
 	words.sort(true);
 	return(words);
 }

 public JumbulayaCell findLetter(Word currentWord,int letter)
 {
	 JumbulayaCell seed = currentWord.seed;
	 JumbulayaChip top = null;
	 while((seed!=null) && ((top = seed.topChip())!=null))
	 {
		 if(top.extendedCharCode() == letter) { return(seed); }
		 seed = seed.exitTo(CELL_RIGHT);
	 }
	 return(null);
 }
 private boolean placeJumbulaya(String word,int wordLen,int fromRow,int fromLetter,int matches,CellStack path)
 {
	 if(fromRow<=NROWS)
 	{
	 char ch = word.charAt(fromLetter);
	 long mask = Dictionary.letterMask(0, ch);
	 Word currentWord = getCurrentWord(NROWS-fromRow+1);
	 long wordMask = currentWord.letterMask();
	 if((wordMask & mask)!=0)
		{	// this row can supply the next letter
		 boolean found = (fromLetter+1==wordLen)
				 			? true
				 			: placeJumbulaya(word,wordLen,fromRow+1,fromLetter+1,matches+1,path);
		 if(found)
		 {	JumbulayaCell c = findLetter(currentWord,ch);
			if(c==null) { found = false; }
			else if(path!=null) {path.push(c);}
		 }
		 return(found);
		}
		else if((NROWS-fromRow)>=(wordLen-fromLetter))
		{	// there are enough rows left to supply all the letters left
			// but skip this row
			return placeJumbulaya(word,wordLen,fromRow+1,fromLetter,matches,path);
		}	} 
	 return(false);
 }
 
 private void checkJumbulayas(WordStack candidateWords,Entry entry,CellStack path,boolean onlyIfWinning)
 {	 String name = entry.word;
 	 path.clear();
	 boolean plausible = placeJumbulaya(name,name.length(),1,0,0,path);
	 if(plausible)
	 {	CellStack cp = new CellStack();
		cp.copyFrom(path);
		Word word = new Word(cp,name);
		int score =  word.jumbulayaScore()+staticEval(whoseTurn);
		if(!onlyIfWinning || score>0)
		{
		score += 1000;		// make winning better
		word.points = score;
	 	candidateWords.recordCandidate("Jumbulaya Word",	word,entry);
		}
	 }
 }
 // check for words of a certain size as a jumbulaya
 private void checkJumbulays(WordStack candidateWords,DictionaryHash subDictionary,CellStack path,boolean onlyIfWinning)
 {	
	 for(Enumeration<Entry> words = subDictionary.elements(); words.hasMoreElements();)
	 {
		 Entry word = words.nextElement();
		 if(word.order<robotVocabulary)
		 {
		 checkJumbulayas(candidateWords,word,path,onlyIfWinning);
		 }
	 }
 }
 
 private void checkJumbulayaCore(WordStack candidateWords,boolean onlyIfWinning)
 {	
 	int baseScore = staticEval(whoseTurn);
 	int minsize = 7;
 	int maxsize = 12;	
 			// only need 9 really, but allow a couple extra letters for two letter tiles
 			// when we figure out how to handle letter pairs, go up more
 	Dictionary dict = Dictionary.getInstance();
 	CellStack path = new CellStack();
 	for(int sz = minsize;sz<=maxsize;sz++)
 	{	if(!onlyIfWinning || baseScore+Word.jumbulayaScore(Math.min(nrows,sz))>0)
		{
 		checkJumbulays(candidateWords,dict.getSubdictionary(sz),path,onlyIfWinning);
		}
 	}
 	candidateWords.sort(true);
 }
 
 /**
  * the basic algorithm is to check words in the dictionary to see if they
  * can be placed on the board.
  */
 public WordStack checkWords()
 {	WordStack ws = checkWordsCore(false);
  	return(ws);
 }
 /**
  * do a word search for the user interface review mode
  * @return
  */
 public WordStack checkLikelyWords()
 {	WordStack words = new WordStack();
 	JumbulayaCell playerRack[] = rack[whoseTurn];
 	// check ordinary crosswords
 	long letterMask = letterSet(playerRack);
 	for(Word cw : currentWord)
 	{ 		
 	WordStack saveWords = new WordStack();
 	checkRow(saveWords,playerRack,letterMask,cw);
 	saveWords.sort(true);
 	for(int i=0;i<Math.min(2, saveWords.size()); i++)
 		{
 		words.push(saveWords.elementAt(i));
 		}
 	}
 	return(words);
	 
 }
 
 public WordStack checkJumbulayas(boolean onlyIfWinning)
 {	WordStack candidateWords = new WordStack();
 	if(canCallJumbulaya(true))
	 {	
		checkJumbulayaCore(candidateWords,onlyIfWinning);
		
	 }
 	return(candidateWords);
 }
 
 public WordStack checkLikelyJumbulayas()
 {	WordStack candidateWords = new WordStack();
 	checkJumbulayaCore(candidateWords,false);
	return(candidateWords);
 }

 // jumbulaya logic
 public boolean canCallJumbulaya(boolean claimCurrentRow)
 {	if(canCallJumbulaya()) return(true); 
 	if(claimCurrentRow)
 	{	// true if clicking done on the currently active word
 		// will reach the threshhold.
 		return ( 
 				// either all lines are claimed, or claiming this line will be the last.
 				((nLinesClaimed==NROWS)
 					|| ((nLinesClaimed==NROWS-1)
 							&& (activeRow>0) 
 							&& (rowOwner(activeRow)<0)))	// currently unclaimed
 				// some line is 7 tiles, or this pending line is
 				&& ((maxLineTiles>=7)
 						|| ((activeRow>0)
 								&& (pendingWord!=null)
 								&& (pendingWord.nTiles>=7)))
 				);
 	}
	 return(false);
 }
 

private boolean canCallJumbulaya()
{	// all rows are claimed, and some row has 7 tiles
	return ((nLinesClaimed==NROWS)
			&& (maxLineTiles>=7));
}
private void addWords(WordStack ws,CommonMoveStack all)
{
	for(int i=0;i<ws.size();i++)
 	{
 		all.push(new Jumbulayamovespec(ws.elementAt(i),whoseTurn));
 	}
}

 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
    recordProgress(0);
    validate(false);
    switch(board_state)
    {
    default: throw G.Error("Not expecting ", board_state);
    case ConfirmJumbulaya:
    case Confirm:
    	all.push(new Jumbulayamovespec(MOVE_DONE,whoseTurn));
    	break;
    case CanJumbulaya:
    {
    	int arow = activeRow;
		int owner = -1;
		int tiles = maxLineTiles;
		Word current = null;
		if(arow>=0)
			{  
			owner = rowOwner(arow);
		    current = getCurrentWord(arow);
			// this isn't stictly kosker, we change the state to accept
			// the pending word, and don't ever reverse it.  This is ok
			// in the current context because the robot isn't doing a search.
			// and this is a copy of the real board.
			validate(true);
			setUnclaimed(arow);
			setClaimed(arow,whoseTurn); 
			}
    	WordStack candidateWords = checkJumbulayas(true);
    	if(arow>=0)
    	{
    		setUnclaimed(arow);
    		if(owner>=0) { setClaimed(arow,owner); }
    		// reset doesn't modify the target word
    		resetCurrentWord(arow,current);
    		maxLineTiles = tiles;
    	}
    	
    	for(int i=0;i<candidateWords.size();i++)
     	{
     		all.push(new Jumbulayamovespec(candidateWords.elementAt(i),whoseTurn));
     	}
    	if(all.size()==0)
    	{
    		all.push(new Jumbulayamovespec(MOVE_DONE,whoseTurn));
    	}
    }
    	break;
    case Play:
    	{
    	// if we're one line away from allowing jumbulayas,
    	// and we're going to win if we can claim one,
    	// then go for it.  Bot is very good at finding them.
    	if((nLinesClaimed==NROWS-1)
    			&& ( staticEval(whoseTurn)+Word.jumbulayaScore(7)+Word.wordScore(3)>0))
    	{	// if we could end it all with a jumbulaya
    		WordStack ws = checkWordsCore(true);	// see if we can claim the last row
    		if(ws.size()>0)
    		{
    			addWords(ws,all);
    			return(all);
    		}
    		
    	}}
    	{
     	WordStack ws = checkJumbulayas(true);
     	addWords(ws,all);
    	}
     	if(all.size()==0)
	     	{
	    	WordStack ws = checkWords();
	     	addWords(ws,all);
     	}
     	if(all.size()==0)
     	{
     		all.push(new Jumbulayamovespec(MOVE_DISCARDRACK,whoseTurn));
     	}
     	break;
    }
 	recordProgress(0);
 	return(all);
 }
 JumbulayaPlay robot = null;
 private void recordProgress(double v)
 {	
	 if(robot!=null) { robot.setProgress(v); }
 }
 public void initRobotValues(JumbulayaPlay rob,int vocab)
 {	robot = rob;
 	robotVocabulary = vocab;
 }

public void setVocabulary(double value) {
	Dictionary dict = Dictionary.getInstance();
	robotVocabulary = (int)(dict.totalSize*value);
}

public void validateMap(int forplayer,String msg)
{		int used = 0;
		int mapped = 0;
		int nmapped = 0;
		JumbulayaCell prack[] = rack[forplayer];
		for(JumbulayaCell c : prack)
		{
			if(c.topChip()!=null) { used++; }
		}
		int mapi[] = rackMap[forplayer];
		for(int i=0;i<mapi.length;i++)
		{	int idx = mapi[i];
			if(idx>=0) { 
				int bit = 1<<idx;
				if((bit&mapped)!=0) { G.Error("%s idx %s is mapped twice",msg,idx); }
				else if(prack[idx].topChip()!=null)
					{ nmapped++; mapped|=bit; 
					}
				else { G.Error("%s index %s maps to empty", msg,idx); }
			}
		}
		G.Assert(nmapped==used,"%s map not complete",msg);
		}


CommonMoveStack  GetandomSteps0()
{  CommonMoveStack all = new CommonMoveStack();
   recordProgress(0);
   validate(false);
   switch(board_state)
   {
   default: throw G.Error("Not expecting ", board_state);
   case ConfirmJumbulaya:
   case Confirm:
   	all.push(new Jumbulayamovespec(MOVE_DONE,whoseTurn));
   	break;
   case CanJumbulaya:
   case Play:
   	{
   	Random r = new Random(randomKey+moveNumber);
   	int row = (activeRow<=0) ? r.nextInt(NROWS)+1 : activeRow;
   	JumbulayaCell playerRack[] = rack[whoseTurn];
   	for(int col=1;col<=NCOLS;col++)
   	{	JumbulayaCell boardCell = getCell((char)('A'+col-1),row);
   		JumbulayaChip boardTop = boardCell.topChip();
   		for(int slot = 0;slot<playerRack.length; slot++)
   		{
   			JumbulayaCell rackCell = playerRack[slot];
   			JumbulayaChip rackTop = rackCell.topChip();
   			if((rackTop==null)!=(boardTop==null))
   			{
   				// one is empty
   				if(rackTop==null)
   				{
   		        all.push(new Jumbulayamovespec(MOVE_MOVETILE,
   		        		JumbulayaId.BoardLocation,boardCell.col,boardCell.row,
   		        		JumbulayaId.Rack,rackCell.col,rackCell.row,
   		        		whoseTurn));
  
   				}
   				else
   				{
   					
   	  		        all.push(new Jumbulayamovespec(MOVE_MOVETILE,
   	  		        		JumbulayaId.Rack,rackCell.col,rackCell.row,
   	   		        		JumbulayaId.BoardLocation,boardCell.col,boardCell.row,
   	   		        		whoseTurn));
 				}
  
   			}
   		}
   	}}}

   return(all);
 
}

CommonMoveStack  GetandomSteps()
{  CommonMoveStack all = new CommonMoveStack();
   recordProgress(0);
   validate(false);
   switch(board_state)
   {
   default: throw G.Error("Not expecting ", board_state);
   case ConfirmJumbulaya:
   case Confirm:
   	all.push(new Jumbulayamovespec(MOVE_DONE,whoseTurn));
   	break;
   case CanJumbulaya:
   case Play:
   	{
   	Random r = new Random();
   	int row = (activeRow<=0) ? r.nextInt(NROWS)+1 : activeRow;
   	JumbulayaCell playerRack[] = rack[whoseTurn];
   	int map[] = rackMap[whoseTurn];
   	if(pickedObject==null)
   	{
   		for(int col=1;col<=NCOLS;col++)
   	   	{	JumbulayaCell boardCell = getCell((char)('A'+col-1),row);
   	   		JumbulayaChip boardTop = boardCell.topChip();
   	   		if(boardTop!=null)
   	   		{
   	   			all.push(new Jumbulayamovespec(MOVE_PICKB,
   	   					null,'@',0,
   	   					JumbulayaId.BoardLocation,boardCell.col,boardCell.row,
   	   					whoseTurn));
   	   		}}
   		for(int slot = 0;slot<playerRack.length; slot++)
   		{
   			JumbulayaCell rackCell = playerRack[slot];
   			JumbulayaChip rackTop = rackCell.topChip();
   			if(rackTop!=null)
   			{	boolean mapped = false;
   				for(int i=0;i<map.length;i++)
   				{
   					if(i==slot)
   					{	if(mapped) { G.Error("slot "+slot+" already mapped"); }
   					else {
   						mapped = true;
   		 	   			all.push(new Jumbulayamovespec(MOVE_PICK,
   		 	   					null,'@',0,
   		   	   					JumbulayaId.Rack,rackCell.col,rackCell.row,
   		   	   					whoseTurn));
   		    					}
   					}
   				}
   				if(!mapped)
   				{
   					G.Error("slot "+slot+" not mapped"); 
   				}
   			}
   		}
   	}
   	else
   	{	// something is picked
   		for(int col=1;col<=NCOLS;col++)
   	   	{	JumbulayaCell boardCell = getCell((char)('A'+col-1),row);
   	   		JumbulayaChip boardTop = boardCell.topChip();
   	   		if(boardTop==null)
   	   		{
   	   			all.push(new Jumbulayamovespec(MOVE_DROPB,
   	   					null,'@',0,
   	   					JumbulayaId.BoardLocation,boardCell.col,boardCell.row,
   	   					whoseTurn));
   	   		}}
   		for(int slot = 0;slot<playerRack.length; slot++)
   		{
   			JumbulayaCell rackCell = playerRack[slot];
   			JumbulayaChip rackTop = rackCell.topChip();
   			if(rackTop==null)
   			{	
   				for(int i=0;i<map.length;i++)
   				{
   					
   					all.push(new Jumbulayamovespec(MOVE_DROP,
   							null,rackCell.col,i,
   							JumbulayaId.Rack,rackCell.col,rackCell.row,
   							whoseTurn));
   				}
   			}
   		}
   	}
   	

   	}}

   return(all);
 
}
public boolean canResign() { return(true); }
}


