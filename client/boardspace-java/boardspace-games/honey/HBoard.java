package honey;

import static honey.Honeymovespec.*;

import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;
import online.game.cell.Geometry;
import dictionary.Dictionary;

/**
 * Initial work September 2020
 *  
 * This shares a lot with Crosswords and other word games.
 * if any features or bug fixes occur, evaluate them too.
 * 
 * This class contains almost all the game logic, but for a single
 * player with their own private board.  Effectively, HoneyComb is
 * n-player solitaire except that you're racing against the other
 * players.
 * 
 * @author ddyer
 *
 */
class HBoard extends hexBoard<HoneyCell> implements BoardProtocol,HoneyConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game

	static final String[] CrosswordsGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	public int getMaxRevisionLevel() { return(REVISION); }
	public String toString() { return "<single "+boardIndex+">"; }
	int sweep_counter = 0;
	int boardIndex = -1;
	HoneyVariation variation = HoneyVariation.HoneyComb;
	private HoneyState board_state = HoneyState.Puzzle;	
	private HoneyState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	public int robotVocabulary = 999999;		//	size of the robot's vocabulary
	public HoneyState getState() { return(board_state); }
    StringStack gameEvents = new StringStack();
    InternationalStrings s = G.getTranslations();
    HoneyCell drawPile = null;
    CellStack selectedCells = new CellStack();
   	
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
	public void setState(HoneyState st) 
	{ 	unresign = (st==HoneyState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public void setGameOver()
	{
		setState(HoneyState.Gameover);
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
	
	public int score() { return score; }
	Dictionary dictionary ;				// the current dictionary
	
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
    public HoneyChip pickedObject = null;
    public HoneyChip lastPicked = null;
    private HoneyCell pickedSource = null; 
    private HoneyCell droppedDest= null;
    private HoneyState resetState = HoneyState.Puzzle; 
    public HoneyChip lastDroppedObject = null;	// for image adjustment logic
    public HoneyCell getSource() { if(pickedObject!=null) { return pickedSource; } else return null; }
	// factory method to generate a board cell
	public HoneyCell newcell(char c,int r)
	{	HoneyCell cd = new HoneyCell(HoneyId.BoardLocation,c,r,Geometry.Hex);
		cd.parent = this;
		return cd;
	}
	
	// constructor 
    public HBoard(String init,int bi,long key,int map[],Dictionary di,int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = CrosswordsGRIDSTYLE;
        isTorus=false;
        setColorMap(map, 1);
        boardIndex = bi;
        gametype = init;
        Random r = new Random(235256);
       	drawPile = new HoneyCell(r,null);
       	dictionary = di;
        robotVocabulary = dictionary.orderedSize;
 
    }
    
    public String gameType() { return(gametype+" "+players_in_game+" "+randomKey+" "+revision); }
    
    private void construct(int n[],int k[])
    {	
        // create a grid for the tile cells
        reInitBoard(n,k,null);		 //this sets up the primary board and cross links
    }
    public void doInit(String gtype,long key)
    {
    	doInit(gtype,key,players_in_game,revision);
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int players,int rev)
    {	randomKey = key;
    	adjustRevision(rev);
    	players_in_game = players;
    	win = new boolean[players];
		variation = HoneyVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
    	gametype = gtype;
		score = 0;
     	unresign = null;
  		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case HoneyComb:
			break;
		}
 		
 		construct(variation.firstincol,variation.nincol);
    	Random r = new Random(randomKey+100);
    	r.nextLong();//  use one random, for compatibility
     	
    	
    	mapPick = -1;
       	mapTarget = -1;
        
 		setState(HoneyState.Puzzle);
		robotState.clear();		
	    	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    acceptPlacement();
	    resetState = HoneyState.Puzzle;
	    lastDroppedObject = null;
	    // set the initial contents of the board to all empty cells
        animationStack.clear();
        moveNumber = 1;

		for(HoneyCell c = allCells; c!=null; c=c.next)
		{	c.addChip(drawPile.removeTop());
		}
		selectedCells.clear();
        // note that firstPlayer is NOT initialized here
    }
    
    /** create a copy of this board */
    public HBoard cloneBoard() 
	{ HBoard dup = new HBoard(gametype,players_in_game,randomKey,getColorMap(),dictionary,revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((HBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(HBoard from_b)
    {
        super.copyFrom(from_b);
        robotState.copyFrom(from_b.robotState);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        droppedDest = getCell(from_b.droppedDest);
        pickedSource = getCell(from_b.pickedSource);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        score=from_b.score;
        robotVocabulary = from_b.robotVocabulary;
        lastPicked = null;
        drawPile.copyFrom(from_b.drawPile);
        getCell(selectedCells,from_b.selectedCells);
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((HBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(HBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(robotVocabulary==from_b.robotVocabulary,"robotVocabulary mismatch");
        G.Assert(score==from_b.score,"score mismatch");
        G.Assert(drawPile.sameContents(from_b.drawPile),"drawPile mismatch");
        G.Assert(sameCells(selectedCells,from_b.selectedCells),"selected cells mismatch");
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

		//G.print("d1b "+v);

		v ^= Digest(r,score);
		//G.print("d2 "+v);
		v ^= Digest(r,robotVocabulary);
		v ^= Digest(r,drawPile);
		v ^= Digest(r,board_state);
		v ^= Digest(r,boardIndex);
		v ^= Digest(r,selectedCells);
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
    public HoneyChip SetBoard(HoneyCell c,HoneyChip ch)
    {	HoneyChip old = c.topChip();
    	if(c.onBoard)
    	{
    	if(ch==null) 
    		{ 
    		  c.reInit();
    		}
    	else { G.Assert(c.isEmpty(),"not expecting to make stacks");
    		c.addChip(ch);
    		
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
    
    public HoneyCell lastDropped()
    {
    	return(droppedDest);
    }

    
    // 
    // drop the floating object.
    //
    private void dropObject(HoneyCell c)
    {
        
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting dest " + c.rackLocation);
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        case EmptyBoard:
            droppedDest = c;
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
    public boolean isADest(HoneyCell c)
    {
    	return droppedDest==c;
    }
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { HoneyChip ch = pickedObject;
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
    private HoneyCell getCell(HoneyId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source " + source);
        case BoardLocation:
        case EmptyBoard:
        	return(getCell(col,row));
        } 	
    }
    /**
     * this is called when copying boards, to get the cell on the new (ie current) board
     * that corresponds to a cell on the old board.
     */
    public HoneyCell getCell(HoneyCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(HoneyCell c)
    {	pickedSource = c;
        switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting rackLocation " + c.rackLocation);
		case BoardLocation:
        	{
        	HoneyChip po = c.topChip();
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
    public boolean isASource(HoneyCell c)
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
        			setState(HoneyState.Confirm); 
        		}
        	else { setState(HoneyState.Play); }
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
    			setState( HoneyState.Play);
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
	Hashtable<String,HWord> wordsUsed = new Hashtable<String,HWord>();
	HWordStack words = new HWordStack();
	HWordStack nonWords = new HWordStack();
	HWordStack candidateWords = new HWordStack();
	
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
    	boolean connected = false;//validateBoard();
    	boolean allwords = (nonWords.size()==0);
    	HWord duplicate = null;
    	if( !connected) { invalidReason = NotConnected;}
    	else if(!allwords) { invalidReason = NotWords; }
    	else invalidReason = null;
    	if(andScore) 
    		{ findWordCaps(); 
    		  int sc = 0;
    		  for(int i=0;i<words.size(); i++) { sc += (words.elementAt(i).points - WordPenalty); }
    		  score = sc;
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
			HWord w = nonWords.elementAt(lim);
			HoneyCell c = w.seed;
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
   			HWord w = words.elementAt(lim);
   			HoneyCell c = w.seed;
   			int direction = w.direction;
   			int opp = direction+CELL_HALF_TURN;
  			int mask = (1<<direction)|(1<<opp);
   			HoneyCell beg = c.exitTo(opp);
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
    private String collectWord(HoneyCell from,int direction)
    {
    	builder.setLength(0);
    	int n=0;
    	isNew = false;
    	HoneyCell c = from;
    	while(c!=null) 
    	{
    		HoneyChip top = c.topChip();
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
    HoneyCell wordBonus[] = new HoneyCell[4];
    // score a word on the board.  wordMultiplier and wordBonus are
    // used as scratch variables during the scoring.
    private int scoreWord(HoneyCell from,int direction)
    {	int score = 0;
    	builder.setLength(0);
    	wordMultiplier = 1;
    	isNew = false;
    	HoneyCell c = from;
    	AR.setValue(wordBonus, null);
    	while(c!=null) 
    	{	
    		HoneyChip top = c.topChip();
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
    private int scoreLetter(HoneyCell c,HoneyChip top)
    {	int letterScore = top.value;
		return(letterScore);
    }

    private void addWord(HWord word)
    {
    	words.push(word); 
    	word.points = scoreWord(word.seed,word.direction); 
    }


    private void doDone(replayMode replay)
    {
        acceptPlacement();
        validate(true);
        
         
        if (board_state==HoneyState.Resign)
        {
            win[nextPlayer()] = true;
    		setState(HoneyState.Gameover);
        }
        else
        {	
         setState(HoneyState.Gameover);
 
         }
    }
	public boolean notStarted()
	{
		return((droppedDest==null) && (pickedSource==null));
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
    {	Honeymovespec m = (Honeymovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }
        //G.print("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(HoneyState.Puzzle);	// standardize the current state
            setNextStateAfterDone(replay);

            break;
        case MOVE_SELECT:
    	{
    	HoneyCell c = getCell(m.from_col,m.from_row);
    	HoneyCell top = selectedCells.top();
    		if(c==top) {}
    		else if(selectedCells.contains(c)) 
    				{	
    				setState(HoneyState.MultiUse); 			
    				}
    		else if(top==null || c.isAdjacentTo(top))
    		{	selectedCells.push(c);
   				c.selected = true;
    		}
    		else { setState(HoneyState.NotAdjacent); }
    	}
    	break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?HoneyState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(HoneyState.Puzzle);
 
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(HoneyState.Gameover);
    	   break;
       case MOVE_PASS:
    	   break;
        default:
        	cantExecute(m);
        }

        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }
        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips(HoneyCell c)
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

    public boolean LegalToHitBoard(HoneyCell c,boolean picked)
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

    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Honeymovespec m)
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
    public void UnExecute(Honeymovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	HoneyState state = robotState.pop();
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

 private int findLetter(HoneyCell fromRack[],char ch,boolean blank)
 {
	 for (int lim = fromRack.length-1; lim>=0; lim--)
	 {
		 HoneyCell c = fromRack[lim];
		 if(c.sweep_counter!=sweep_counter)
		 {	HoneyChip top = c.topChip();
		 	if(top!=null && top.lcChar==ch)
		 	{	if(top.lcChar==ch) { return lim; }
		 	}
		 }	 
	 }
	 return(-1);
 }
 // we have already verified the placement to be legal, now score the
 // word considering the new letters we will place.
 // this does not check for illegal honey
 // one complication is that the search for words using blanks
 // involves "temporarily" placing the actual letter in the rack
 private int scoreWord(String word,HoneyCell from,int direction,HoneyCell fromRack[])
 {	
	int score = 0;
	sweep_counter++;
	wordMultiplier = 1;
	AR.setValue(wordBonus, null);
	HoneyCell c = from;
 	for(int idx = 0,lastIdx = word.length(); idx<lastIdx; idx++)
 	{	
	 	HoneyChip ch = c.topChip();
	 	if(ch==null)
	 	{	// we'll place this letter, so it scores
	 		char letter = word.charAt(idx);
	 		int rackidx = findLetter(fromRack,letter,false);	// find as a real letter
	 		if(rackidx>=0)
	 			{
	 			
	 			score += scoreLetter(c,HoneyChip.getLetter(letter));
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
 

 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
    recordProgress(0);
    validate(false);
    switch(board_state)
    {
    default: throw G.Error("Not expecting ", board_state);
    case Confirm:
    	all.push(new Honeymovespec(MOVE_DONE,whoseTurn));
    	break;
    case Play:
     	if(all.size()==0)
     	{
     		all.push(new Honeymovespec(MOVE_PASS,whoseTurn));
     	}
     	break;
    }
 	recordProgress(0);
 	return(all);
 }
 HoneyPlay robot = null;
 private void recordProgress(double v)
 {	
	 if(robot!=null) { robot.setProgress(v); }
 }
 public void initRobotValues(HoneyPlay rob,int vocab)
 {	robot = rob;
 	robotVocabulary = vocab;
 }


}
