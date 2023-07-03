package sprint;

import static sprint.Sprintmovespec.*;

import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;
import online.game.cell.Geometry;
import dictionary.Dictionary;
import dictionary.Entry;

/**
 * Initial work September 2020
 *  
 * This shares a lot with Crosswords and other word games.
 * if any features or bug fixes occur, evaluate them too.
 * 
 * This class contains almost all the game logic, but for a single
 * player with their own private board.  Effectively, Sprint is
 * n-player solitaire except that you're racing against the other
 * players.
 * 
 * @author ddyer
 *
 */
class SingleBoard extends infiniteSquareBoard<SprintCell> implements BoardProtocol,SprintConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game

	static final String[] CrosswordsGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	public int getMaxRevisionLevel() { return(REVISION); }
	public String toString() { return "<single "+boardIndex+">"; }
	int sweep_counter = 0;
	int boardIndex = -1;
	SprintVariation variation = SprintVariation.Sprint;
	private SprintState board_state = SprintState.Puzzle;	
	private SprintState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	public int robotVocabulary = 999999;		//	size of the robot's vocabulary
	public SprintState getState() { return(board_state); }
    StringStack gameEvents = new StringStack();
    InternationalStrings s = G.getTranslations();
    int maxTiles = -1;
    int tilesPlaced = 0;
    public SprintCell unplacedTiles[] = null;
    int unplacedCount = 0;
   	SprintCell drawPile = null;
   	
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
	public void setState(SprintState st) 
	{ 	unresign = (st==SprintState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public void setGameOver()
	{
		setState(SprintState.Gameover);
	}
	SprintCell rack[] = null;
	private SprintCell mappedRack[] = null;
	
	public SprintCell[] getPlayerRack()
	{
		return rack;
	}
	public SprintCell[] getPlayerMappedRack()
	{	return mappedRack;
	}

	private int mapPick = -1;
	private int mapTarget = -1;		// which actual cell is picked (per player)
	
	public int getMapPick()
	{	return mapPick;
	}
	public int getMapTarget()
	{	return mapTarget;
	}

	private int rackMap[] = new int[rackSize+rackSpares];
	public int[] getRackMap()
	{	return rackMap;
	}

	public boolean hiddenVisible = false;
	int score = 0;
	int highScore = 0;
	
	public int score() { return score; }
	public int highScore() { return highScore; }
	int chipsOnBoard = 0;
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
    public SprintChip pickedObject = null;
    public SprintChip lastPicked = null;
    private SprintCell pickedSource = null; 
    private SprintCell droppedDest= null;
    private SprintState resetState = SprintState.Puzzle; 
    public SprintChip lastDroppedObject = null;	// for image adjustment logic
    public SprintCell getSource() { if(pickedObject!=null) { return pickedSource; } else return null; }
	// factory method to generate a board cell
	public SprintCell newcell(char c,int r)
	{	SprintCell cd = new SprintCell(SprintId.BoardLocation,c,r,Geometry.Square);
		cd.parent = this;
		return cd;
	}
	
	// constructor 
    public SingleBoard(String init,int bi,long key,int map[],Dictionary di,int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = CrosswordsGRIDSTYLE;
        isTorus=false;
        setColorMap(map, 1);
        boardIndex = bi;
       	Random r = new Random(235256);
       	drawPile = new SprintCell(r,SprintId.DrawPile);

       	doInit(init,key,1,rev); // do the initialization 

       	dictionary = di;
        robotVocabulary = dictionary.orderedSize;
 
    }
    private void initRackMap()
    { 	int full = rackMap.length;
    	int max = rackSize;
    	int off = (full-max)/2;
    	for(int i=0; i<rackMap.length;i++) { rackMap[i] = (i>=off && i<max+off) ? i-off : -1; }
    }
    
    public String gameType() { return(gametype+" "+players_in_game+" "+randomKey+" "+revision); }
    
    private void construct(int n,int k)
    {	
        // create a grid for the tile cells
        reInitBoard(n,k);		 //this sets up the primary board and cross links
        if(unplacedTiles==null || unplacedTiles.length!=maxTiles)
        {
        	unplacedTiles = new SprintCell[maxTiles];

            for(int i=0;i<unplacedTiles.length;i++)
            {
            	unplacedTiles[i] = new SprintCell(SprintId.Unplaced,(char)('A'+boardIndex),i);
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
		variation = SprintVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		score = 0;
		highScore = 0;
    	gametype = gtype;
     	tilesPlaced = 0;
     	unresign = null;
    	maxTiles = variation.maxTiles;
 	    occupiedCells.clear();
	    initRackMap();
 		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case Sprint:
			break;
		}
 		
 		construct(variation.boardsize,variation.boardsize);
		reInit(unplacedTiles);	 		
 		chipsOnBoard = 0;    
    	Random r = new Random(randomKey+100);
    	r.nextLong();//  use one random, for compatibility
     	
    	// create the player racks
 		int racks = rackSize+rackSpares;
 		if(rack==null || racks!=rack.length)
 		{
    	rack = new SprintCell[racks];
       	mappedRack = new SprintCell[racks];
        
       	for(int j = 0;j<rack.length;j++)
       	{ SprintCell c = rack[j] = new SprintCell(SprintId.Rack,(char)('A'+boardIndex),j,Geometry.Standalone);
       	  c.onBoard = false;
       	}
       	for(int j = 0;j<mappedRack.length; j++)
       	{
       		mappedRack[j] = new SprintCell(SprintId.RackMap,(char)('A'+boardIndex),j,Geometry.Standalone);
       	}
  		}
    	else 
    		{ reInit(rack); 
    		  reInit(mappedRack);
    		}
 		
 		// load the racks with vowels
    	int idx = 0;
    	for(char l : new char[]{ 'a','e','i','o','u'})
    	{
    		SprintChip letter = SprintChip.getLetter(l);
     		rack[idx].addChip(letter);		
    		idx++;
    	}
    	
    	mapPick = -1;
       	mapTarget = -1;
        
 		setState(SprintState.Puzzle);
		robotState.clear();		
	    	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    drawPile.reInit();
	    acceptPlacement();
	    unplacedCount = 0;
	    resetState = SprintState.Puzzle;
	    lastDroppedObject = null;
	    // set the initial contents of the board to all empty cells
        animationStack.clear();
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }
    
    public void placeNewTile(SprintChip chip)
    {	SprintChip top = drawPile.removeTop();
    	//G.print("Placenew "+boardIndex+" "+top);
    	G.Assert(top==chip,"draw mismatch");
    	for(int i=0;i<unplacedTiles.length;i++)
    	{
    		SprintCell c = unplacedTiles[i];
    		if(c.topChip()==null)
    			{ c.addChip(chip);
    			  unplacedCount++; 
    			  tilesPlaced++;
    			  setState(SprintState.Play);
    			  break; 
    			}
    	}
    }
    
    /** create a copy of this board */
    public SingleBoard cloneBoard() 
	{ SingleBoard dup = new SingleBoard(gametype,players_in_game,randomKey,getColorMap(),dictionary,revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((SingleBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(SingleBoard from_b)
    {
        super.copyFrom(from_b);
        robotState.copyFrom(from_b.robotState);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        chipsOnBoard = from_b.chipsOnBoard;
        droppedDest = getCell(from_b.droppedDest);
        pickedSource = getCell(from_b.pickedSource);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        copyFrom(rack,from_b.rack);
        copyFrom(unplacedTiles,from_b.unplacedTiles);
        score=from_b.score;
        highScore = from_b.highScore;
        robotVocabulary = from_b.robotVocabulary;
        lastPicked = null;
        tilesPlaced = from_b.tilesPlaced;
        unplacedCount = from_b.unplacedCount;
        getCell(occupiedCells,from_b.occupiedCells);
        drawPile.copyFrom(from_b.drawPile);
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((SingleBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(SingleBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(chipsOnBoard==from_b.chipsOnBoard,"chipsOnBoard mismatch");
        G.Assert(robotVocabulary==from_b.robotVocabulary,"robotVocabulary mismatch");
        G.Assert(sameContents(rack,from_b.rack),"rack mismatch");
        G.Assert(score==from_b.score,"score mismatch");
        G.Assert(highScore==from_b.highScore,"highscore mismatch");
        G.Assert(tilesPlaced==from_b.tilesPlaced,"tilesPlaced mismatch");
        G.Assert(sameContents(unplacedTiles,from_b.unplacedTiles),"unplaced tiles mismatch");
        G.Assert(unplacedCount==from_b.unplacedCount,"unplacedCount mismatch");
        G.Assert(drawPile.sameContents(from_b.drawPile),"drawPile mismatch");
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
        return Digest(r);
    }
    public long Digest(Random r)
    {
        long v = super.Digest(r);
        // G.print();
        // G.print("d0 "+v);
		// many games will want to digest pickedSource too
		// v ^= cell.Digest(r,pickedSource);
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,revision);
		//G.print("d1 "+v);

		v ^= Digest(r,rack);
		//G.print("d1a "+v);
		v ^= Digest(r,chipsOnBoard);
		//G.print("d1b "+v);

		v ^= Digest(r,score);
		//G.print("d2 "+v);
		v ^= Digest(r,highScore);
		v ^= Digest(r,robotVocabulary);
		v ^= Digest(r,unplacedTiles);
		v ^= Digest(r,unplacedCount);
		v ^= Digest(r,tilesPlaced);
		v ^= Digest(r,drawPile);
		v ^= Digest(r,board_state);
		v ^= Digest(r,boardIndex);
		//G.print("dx "+v);
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
    public SprintChip SetBoard(SprintCell c,SprintChip ch)
    {	SprintChip old = c.topChip();
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
        droppedDest = null;
        pickedSource = null;
        pickedObject = null;
     }
    
    public SprintCell lastDropped()
    {
    	return(droppedDest);
    }

    
    // 
    // drop the floating object.
    //
    private void dropObject(SprintCell c)
    {
        
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting dest " + c.rackLocation);
        case Rack:
           	G.Assert(c.topChip()==null && pickedObject!=null,"drop something on empty %s %s",c,pickedObject);
        	c.addChip(pickedObject);
        	pickedObject = null;
        	break;
        case Unplaced:
        	unplacedCount++;      	
        //$FALL-THROUGH$
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        case EmptyBoard:
            droppedDest = c;
           	SetBoard(c,pickedObject);
        	createExitCells(c);
        	lastDroppedObject = pickedObject;
            pickedObject = null;
            break;
        }
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isADest(SprintCell c)
    {
    	return droppedDest==c;
    }
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { SprintChip ch = pickedObject;
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
    private SprintCell getCell(SprintId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source " + source);
        case Rack:
        	return(rack[row]);
        case Unplaced:
        	return(unplacedTiles[row]);
        case BoardLocation:
        case EmptyBoard:
        	return(getCell(col,row));
        } 	
    }
    /**
     * this is called when copying boards, to get the cell on the new (ie current) board
     * that corresponds to a cell on the old board.
     */
    public SprintCell getCell(SprintCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(SprintCell c)
    {	pickedSource = c;
        switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting rackLocation " + c.rackLocation);
        case Rack:
        	{
        	mapPick=-1;
        	mapTarget=-1;
        	int map[] = rackMap;
        	validateMap();
        	for(int i=0;i<map.length;i++) { if(map[i]==c.row) { map[i]=-1; break; }} 
          	SprintChip po = c.removeTop();
            lastPicked = pickedObject = po;
         	lastDroppedObject = null;
 			validateMap(); 
        	}
 			break;
        case Unplaced:
        	unplacedCount--;
			//$FALL-THROUGH$
		case BoardLocation:
        	{
        	SprintChip po = c.topChip();
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
    public boolean isASource(SprintCell c)
    {	return(pickedSource==c);
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
        case Gameover:
        	break;
        case Confirm:
        case Endgame:
        case EndingGame:
        case Play:
        	if(validate(true)) 
        		{ 	
        			setState(tilesPlaced==maxTiles ? SprintState.Endgame : SprintState.Confirm); 
        		}
        	else { setState(SprintState.Play); }
        	moveNumber++;
			break;
        case Puzzle:
			acceptPlacement();
			validate(true);
            break;
        }
    }

    public void setVocabulary(double value) {
    	Dictionary dict = Dictionary.getInstance();
    	robotVocabulary = (int)(dict.totalSize*value);
    }

    public boolean rackIsFull()
    {
    	for(SprintCell c : rack) { if(c.topChip()==null) { return false; }}
    	return mapPick<0;
    }
    private void setNextStateAfterDone(replayMode replay)
    {	
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state "+board_state);
    	case Gameover: break;
    	case Puzzle:
			//$FALL-THROUGH$
		case Confirm:
    	case Play:
    		{
    			setState( SprintState.Play);
    		}
    		
    		break;
    	}
       	resetState = board_state;
    }
    public int nextPlayer()
    {
    	return((whoseTurn+1)%players_in_game);
    }
     
    StringBuilder builder = new StringBuilder();
    boolean isNew = false;	// the word in the builder includes a new letter
	Hashtable<String,Word> wordsUsed = new Hashtable<String,Word>();
	WordStack words = new WordStack();
	WordStack nonWords = new WordStack();
	WordStack candidateWords = new WordStack();
	
	public String invalidReason=null;
	/* 
	 * this validates that all the lines on the board are words, and are all
	 * connected, the list of new words, the list of words, and the list of non words.
	 * if andScore is true, it also calculates the score change and the lists
	 * of word caps.
	 */
   synchronized boolean validate(boolean andScore)
    {
    	builder.setLength(0);
    	words.clear();
    	nonWords.clear();
    	boolean connected = validateBoard();
    	boolean allwords = (nonWords.size()==0);
    	Word duplicate = null;
    	if( !connected) { invalidReason = NotConnected;}
    	else if(!allwords) { invalidReason = NotWords; }
    	else if((unplacedCount>0) || (pickedObject!=null)) { invalidReason = NotAllPlaced; }
    	else invalidReason = null;
    	if(andScore) 
    		{ findWordCaps(); 
    		  int sc = 0;
    		  for(int i=0;i<words.size(); i++) { sc += (words.elementAt(i).points - WordPenalty); }
    		  score = sc;
    		  highScore = Math.max(score,highScore);
    		  markNonWords();
    		}
     	return( connected 
    			&& allwords 
    			&& (invalidReason==null) // can't make valid words in 2 directions
    			&& (duplicate==null));
    }
    private void markNonWords()
    {
    	for(int lim = nonWords.size()-1; lim>=0; lim--)
		  {
			Word w = nonWords.elementAt(lim);
			SprintCell c = w.seed;
			int direction = w.direction;
			while(c!=null && c.topChip()!=null)
			{
				c.nonWord = true;
				c = c.exitTo(direction);
			}
		  }
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
   			SprintCell c = w.seed;
   			int direction = w.direction;
   			int opp = direction+CELL_HALF_TURN;
  			int mask = (1<<direction)|(1<<opp);
   			SprintCell beg = c.exitTo(opp);
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

   	// 
   	// collect a word from a starting cell with a given direction
   	// 
    private String collectWord(SprintCell from,int direction)
    {
    	builder.setLength(0);
    	int n=0;
    	isNew = false;
    	SprintCell c = from;
    	while(c!=null) 
    	{
    		SprintChip top = c.topChip();
    		if(top==null) {  break; }
    		builder.append(top.lcChar);
    		n++;
    		isNew |= isADest(c);			// keep track of new words for scoring and connection testing
    		c = c.exitTo(direction);
    	}
    	// leave builder primed with the letters, so it can be reversed
    	//G.print("Collect from "+from+" "+direction+" "+builder.toString());
    	return(n>1 ? builder.toString() : null);
    }
 
    int wordMultiplier = 1;
    SprintCell wordBonus[] = new SprintCell[4];
    // score a word on the board.  wordMultiplier and wordBonus are
    // used as scratch variables during the scoring.
    private int scoreWord(SprintCell from,int direction)
    {	int score = 0;
    	builder.setLength(0);
    	wordMultiplier = 1;
    	isNew = false;
    	SprintCell c = from;
    	AR.setValue(wordBonus, null);
    	while(c!=null) 
    	{	
    		SprintChip top = c.topChip();
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
    

    // score a letter on the board, and change wordMultiplier as a side
    // effect.  This is used both for scoring actually placed words and
    // for proposed words that are not yet on the board
    // wordBonus and wordMultiplier are used as scratch
    private int scoreLetter(SprintCell c,SprintChip top)
    {	int letterScore = top.value;
		return(letterScore);
    }
    

    private void addWord(Word word)
    {
    	words.push(word); 
    	word.points = scoreWord(word.seed,word.direction); 
    }

    private boolean validateBoard()
    {	
    	int segments = 0;
       	sweep_counter++;
        for(int lim = occupiedCells.size()-1; lim>=0;lim--) { occupiedCells.elementAt(lim).nonWord = false; }
    	for(int lim = occupiedCells.size()-1; lim>=0; lim--)
    	{	int newletters = validateFrom(occupiedCells.elementAt(lim));
    		if(newletters>0)
    			{
    			segments++;
    			
    			}
    	}
    	return segments==1;
    }
    private int validateFrom(SprintCell c)
    {
    	if(c==null || c.sweep_counter>=sweep_counter || (c.topChip()==null)) { return(0); }
    	int n = 1;
    	c.sweep_counter = sweep_counter;
    	c.wordDirections = 0;		// clear here, so markDirections doesn't have to.
    	int directionStep = CELL_QUARTER_TURN;
    	// don't mess with this, it's correct!
    	int lastDir = CELL_RIGHT;
    	// don't mess with this, it's correct!
     	int firstDir =  CELL_LEFT;
    	// don't mess with this, it's correct!
     	boolean some = false;
     	for(int dir = firstDir; dir<CELL_FULL_TURN; dir+= directionStep)
    	{	SprintCell next = c.exitTo(dir);
     		SprintChip top = next==null ? null : next.topChip();
    		if(top!=null) { n += validateFrom(next); some = true;}
    		else if(dir<lastDir)
    		{ 	int oppDir = dir+CELL_HALF_TURN;
    			
    			String w = collectWord(c,oppDir);
    			if(w!=null) 
    				{ Entry e = dictionary.get(w);		// this deliberately uses the unlimited dictionary
    				  Word word = new Word(c,w,oppDir);
    				  
    				  if(e!=null) 
    				  	{ addWord(word);
    				  	  
    				  	}
    				  	else { 
    				  		nonWords.push(word);
    				  	}

    				}
     		}
    	}
     	if(!some) { c.nonWord = true; }
    	return(n);
    }
    private void doDone(replayMode replay)
    {
        acceptPlacement();
        validate(true);
        
         
        if (board_state==SprintState.Resign)
        {
            win[nextPlayer()] = true;
    		setState(SprintState.Gameover);
        }
        else
        {	
         setState(SprintState.Gameover);
 
         }
    }
	public boolean notStarted()
	{
		return((droppedDest==null) && (pickedSource==null));
	}

	public void dropAndSlide(SprintCell from,int moving0,int pick0,int dest0,replayMode replay)
	{	// the map is invalid when we enter, because we've dropped a tile
		// and haven't mapped it yet.
		SprintCell rcells[] = mappedRack;
    	int pick = pick0;
    	int dest = dest0;
    	int moving = moving0;
    	int map[] = rackMap;
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
		validateMap();
	}
	// when receive a move that includes a rack map slot, it may
	// not be accurate since we don't track the opposing players 
	// rack.
	private void setRackMap(int rackMap[],int slot,int v)
	{	if(rackMap[slot]!=-1)
		{	for(int i=0;i<rackMap.length;i++)
			{
				if(rackMap[i]==-1) { rackMap[i] = v; return; }
			}
			G.Error("No empty slot found");
		}
		rackMap[slot] = v; 

	}
	
    public boolean Execute(commonMove mm,replayMode replay)
    {	Sprintmovespec m = (Sprintmovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }
        validateMap();
        //G.print("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
        case MOVE_PULLNEW:
        	{
        	SprintChip ch = SprintChip.getLetter(m.from_col);
        	SprintChip top = drawPile.removeTop();
           	//G.print("pull "+boardIndex+" "+top);
        	G.Assert(top==ch,"matching draw");
        	SprintCell c = unplacedTiles[m.to_row];
        	c.addChip(ch);
        	unplacedCount++; 
			tilesPlaced++;
   			setState(SprintState.Play);
        	}
        	break;
        case MOVE_ENDEDGAME:
        	//(replay==replayMode.Live) { G.print(Plog.log.finishLog()); }
         	doDone(replay);
         	break;
         	
        case MOVE_ENDGAME:
        	if(board_state!=SprintState.Gameover) { setState(SprintState.EndingGame); }
            break;
        case MOVE_SEE:
        	{	// see tiles in the hidden rack on hand held device
        		hiddenVisible = (m.to_row==0?false:true);
        	}
        	break;
        case MOVE_DROPB:
        	{
                G.Assert(m.player==boardIndex,"mismatch");
			SprintCell src = pickedSource;
			SprintChip po = pickedObject;
			SprintCell dest =  getCell(SprintId.BoardLocation,m.to_col,m.to_row);		
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
	            setNextStateAfterDrop(replay);
				}

             break;

        case MOVE_REPLACE:
        	
	    	{
	    	SprintCell rcells[] = mappedRack;
	    	int map[] = rackMap;
	    	int pick = mapPick;
	    	if(pick>=0)
	    	{
	    	int dest = m.to_row;
	    	int moving = map[pick];
	    	map[pick] = -1;
	    	mapPick = -1;
	    	mapTarget = -1;
	    	dropAndSlide(rcells[pick],moving,pick,dest,replay);
	    	}}
	    	break;
        case MOVE_LIFT:
        	{
        	mapPick = m.to_row;
        	mapTarget = m.mapped_row;
        	}
        	break;
        case MOVE_MOVETILE:
        	{
           	validateMap();
           	SprintCell src = getCell(m.source,m.from_col,m.from_row);
        	SprintCell dest = getCell(m.dest,m.to_col,m.to_row);
        	pickObject(src);
        	dropObject(dest);
           	validateMap();
           	// no animation needed because this is really from 
        	// a pick/drop pair sourced in the rack
            if(replay==replayMode.Single)
        	{ animationStack.push(src);
        	  animationStack.push(dest); 
        	}
 
        	setNextStateAfterDrop(replay);

        	}
        	break;
        case MOVE_PICK:		// pick from the draw pile
 		case MOVE_PICKB:	// pick from the board
        	// come here only where there's something to pick, which must
 			{
 			SprintCell src = getCell(m.dest,m.to_col,m.to_row);
 			{
        	// be a temporary p
        	pickObject(src);
        	m.chip = pickedObject;
        	if(src.onBoard)
        	{
        	switch(board_state)
        	{
        	case Endgame:
        	case Confirm:
        		setState(SprintState.Play);
				//$FALL-THROUGH$
           	case Puzzle:
           	case EndingGame:
			case Play:
        		break;
        	default: ;
        	}
        	validate(true);
        	}
        	
 			}}
    		
    		break;
	    	
        case MOVE_DROP: // drop on chip pool;
        	if((pickedObject==null) && mapPick>=0) 
        	{
        		pickObject(getCell(SprintId.Rack,'@',rackMap[mapPick]));
        	}
        	if(pickedObject!=null)
        	{	
        	switch(m.dest)
	  		  {
	  		  default: 
	  		  	{
	  			SprintCell dest = getCell(m.dest,m.to_col,m.to_row);
	        	dropObject(dest);
	  		  	}
	  		  	break;
	  		  case Rack:
	  		  	{	
	  		  		for(int i=0;i<rack.length;i++)
	  		  		{
	  		  			SprintCell dest = rack[i];
	  		  			if(dest.topChip()==null)
	  		  			{	
	  		  				dropObject(dest);
	  		  				setRackMap(rackMap,m.to_row,i);
	  		  				break;
	  		  			}
	  		  		}
	  		  	}
	  		  }
        	setNextStateAfterDrop(replay);
        	}
        	else { 
        		G.Error("no chip to drop");
        	}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(SprintState.Puzzle);	// standardize the current state
            setNextStateAfterDone(replay);

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?SprintState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(SprintState.Puzzle);
 
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(SprintState.Gameover);
    	   break;
       case MOVE_PULLSTART:
       case MOVE_PASS:
    	   break;
        default:
        	cantExecute(m);
        }
        validateMap();

        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }
        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips(SprintCell c)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting Legal Hit state " + board_state);
        case Confirm:
		case EndingGame:
        case Endgame:
        case Play:
        	return ( (c!=null) 
        			 && ((pickedObject==null) ? (c.topChip()!=null) : true)
        			);
 		case Resign:
		case Gameover:
			return(false);
        case Puzzle:
            return ((c!=null) && ((pickedObject==null) == (c.topChip()!=null)));
        }
    }

    public boolean LegalToHitBoard(SprintCell c,boolean picked)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
 		case Confirm:
		case EndingGame:
 		case Endgame:
		case Play:
			return(picked ? c.isEmpty() : (c.topChip()!=null));

		case Gameover:
		case Resign:
			return(false);
        default:
        	throw G.Error("Not expecting Hit Board state " + board_state);
        case Puzzle:
            return (picked == (c.topChip()==null));
        }
    }
    public void validateMap()
    {		int used = 0;
    		int mapped = 0;
    		int nmapped = 0;
    		SprintCell prack[] = rack;
    		for(SprintCell c : prack)
    		{
    			if(c.topChip()!=null) { used++; }
    		}
    		int mapi[] = rackMap;
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
    public void RobotExecute(Sprintmovespec m)
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
    public void UnExecute(Sprintmovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	SprintState state = robotState.pop();
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

 

 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
    recordProgress(0);
    validate(false);
    switch(board_state)
    {
    default: throw G.Error("Not expecting ", board_state);
    case Confirm:
    	all.push(new Sprintmovespec(MOVE_DONE,whoseTurn));
    	break;
    case Play:
     	if(all.size()==0)
     	{
     		all.push(new Sprintmovespec(MOVE_PASS,whoseTurn));
     	}
     	break;
    }
 	recordProgress(0);
 	return(all);
 }
 SprintPlay robot = null;
 private void recordProgress(double v)
 {	
	 if(robot!=null) { robot.setProgress(v); }
 }
 public void initRobotValues(SprintPlay rob,int vocab)
 {	robot = rob;
 	robotVocabulary = vocab;
 }
 //
 // get the string for a pull move for chip
 //
 public String getPullMove()
 {
	 SprintChip chip = drawPile.topChip();
	 if(chip!=null)
	 {
     for(int i=0;i<unplacedTiles.length;i++)
     {
    	 SprintCell c = unplacedTiles[i];
    	 if((c!=pickedSource) && (c.topChip()==null))
    	 {
    		 return "Pullnew "+boardIndex+" "+chip.lcChar+" "+i;
    	 }
     }}
     return null;
 }


}
