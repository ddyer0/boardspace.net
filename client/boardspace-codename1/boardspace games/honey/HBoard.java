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
package honey;

import static honey.Honeymovespec.*;

import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;
import online.game.cell.Geometry;
import dictionary.Dictionary;
import dictionary.DictionaryHash;
import dictionary.Entry;

/**
 * Initial work September 2023
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
{	static int REVISION = 101;			// 100 represents the initial version of the game
										// revision 101 changes the letters to be without replacement

	static final String[] CrosswordsGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	public int getMaxRevisionLevel() { return(REVISION); }
	public String toString() { return "<single "+boardIndex+">"; }
	int sweep_counter = 0;
	int boardIndex = -1;
	HoneyVariation variation = HoneyVariation.HoneyComb;
	private HoneyState board_state = HoneyState.Puzzle;	
	private HoneyState unresign = null;	// remembers the orignal state when "resign" is hit
	public int robotVocabulary = 999999;		//	size of the robot's vocabulary
	public HoneyState getState() { return(board_state); }
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
    private HoneyState resetState = HoneyState.Puzzle; 
	// factory method to generate a board cell
	public HoneyCell newcell(char c,int r)
	{	HoneyCell cd = new HoneyCell(HoneyId.BoardLocation,c,r,Geometry.Hex);
		return cd;
	}
	
	// constructor 
    public HBoard(String init,int bi,long key,int map[],Dictionary di,int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = CrosswordsGRIDSTYLE;
        isTorus=false;
        setColorMap(map, 1);
       	win = new boolean[1];
       	randomKey = key;
        boardIndex = bi;
        gametype = init;
       	dictionary = di;
        robotVocabulary = dictionary.orderedSize();
 
    }
    
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
    	setColorMap(getColorMap(),players);
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
    	if(revision<101)
    	{
    	for(HoneyCell c = allCells; c!=null; c=c.next)
    	{
    		int n = r.nextInt(HoneyChip.letters.length);	// 25 letter alphabet
    		c.addChip(HoneyChip.letters[n]);
    	}
    	}
    	else
    	{	HoneyChip letters[] = new HoneyChip[HoneyChip.letters.length];
    		AR.copy(letters,HoneyChip.letters);
    		r.shuffle(letters);
    		int i=0;
    		for(HoneyCell c = allCells; c!=null; c=c.next)
    		{
    			c.addChip(letters[i++]);
    		}  		
    	}
 		setState(HoneyState.Puzzle);
	    	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    resetState = HoneyState.Puzzle;
	    // set the initial contents of the board to all empty cells
        animationStack.clear();
        moveNumber = 1;

		nonWords.clear();
		words.clear();
		commonWords.clear();
		
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
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        resetState = from_b.resetState;
        score=from_b.score;
        robotVocabulary = from_b.robotVocabulary;
        words.copyFrom(from_b.words);
        nonWords.copyFrom(from_b.nonWords);
        commonWords.copyFrom(from_b.commonWords);
        myCommonWords.copyFrom(from_b.myCommonWords);
        if(G.debug()) { sameboard(from_b); }
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
        G.Assert(robotVocabulary==from_b.robotVocabulary,"robotVocabulary mismatch");
        G.Assert(score==from_b.score,"score mismatch");
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
        return Digest(r);
    }
    public long Digest(Random r)
    {
        long v = super.Digest(r);
        // G.print();
        // G.print("d0 "+v);
		// many games will want to digest pickedSource too
		// v ^= cell.Digest(r,pickedSource);
		v ^= Digest(r,revision);

		//G.print("d1b "+v);

		v ^= Digest(r,score);
		//G.print("d2 "+v);
		v ^= Digest(r,robotVocabulary);
		v ^= Digest(r,board_state);
		v ^= Digest(r,boardIndex);
		//G.print("dx "+v);
		v ^= words.Digest(r);
		v ^= Digest(r,nonWords);
       return (v);
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


	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { 
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
 

    public void setVocabulary(double value) {
    	Dictionary dict = Dictionary.getInstance();
    	robotVocabulary = (int)(dict.size()*value);
    }
     
	HWordStack words = new HWordStack();
	HWordStack nonWords = new HWordStack();
	HWordStack commonWords = new HWordStack();
	HWordStack myCommonWords = new HWordStack();
	
   	// score a word on the board.  wordMultiplier and wordBonus are
    // used as scratch variables during the scoring.
    private int scoreWord(CellStack from)
    {	int score = 0;
    	for(int i=0,lim=from.size(); i<lim; i++)
    	{	HoneyCell c = from.elementAt(i);
    		HoneyChip top = c.topChip();
    		if(top==null) {  break; }
    		score += top.value;
     	}
    	return(score);
    }

    private void doDone(replayMode replay)
    {       
         
         setGameOver();
        }

    private void addWordTo(HWordStack which,Honeymovespec m,boolean rejectCommon)
	{
    	CellStack seed = new CellStack();
    	Tokenizer tok = new Tokenizer(m.path,",");
    	while(tok.hasMoreElements())
    	{
    		char col = tok.parseCol();
    		int row = tok.intToken();
    		seed.push(getCell(col,row));
    	}
    	HWord newword = new HWord(seed,m.word);
    	if(rejectCommon && commonWords.contains(newword))
    		{ m.isCommon = true;
    		}
    	else { 
    		if(!which.pushNew(newword)) { m.notNew = true; }
    		if(which==words)
    		{
    		int ss = scoreWord(newword.seed); 
    		newword.points = ss;
    		score += ss;
    		}
    	}
 	}
	public void makeCommon(HWord w)
	{
		HWord a = (HWord)words.remove(w,true);
		if(a!=null)
		{
			score -= a.points;
			myCommonWords.pushNew(w);
		}
		commonWords.pushNew(w);
	}
	// we tried to add a previously know common word
	public void makeMyCommon(HWord w)
	{	myCommonWords.pushNew(w);
	}

	public HWord findWord(String w)
	{	return words.find(w);
	}
	public HWord findCommonWord(String w)
	{	return commonWords.find(w);
	}

    public boolean Execute(commonMove mm,replayMode replay)
    {	Honeymovespec m = (Honeymovespec)mm;
        if(replay.animate) { animationStack.clear(); }
        //G.print("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
        case MOVE_REJECTWORD:
        	addWordTo(nonWords,m,false);
        	break;
        case MOVE_PLAYWORD:
        	addWordTo(words,m,true);
        	break;
        case MOVE_START:
            setWhoseTurn(m.player);
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(HoneyState.Play);	// standardize the current state

            break;
        case MOVE_ENDED:
        	doDone(replay);
        	break;
        case MOVE_SELECT:
	    	G.Error("not expected");
	    	break;
        case MOVE_ENDGAME:
        	if(board_state!=HoneyState.Gameover) { setState(HoneyState.EndingGame); }
            break;
       case MOVE_RESIGN:
    	   	setState(unresign==null?HoneyState.Resign:unresign);
            break;
       case MOVE_EDIT:
            setState(HoneyState.Puzzle);
 
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(HoneyState.Gameover);
    	   break;
        default:
        	cantExecute(m);
        }

        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }
        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }

    /** find a particular word starting from a particular cell.  If stack
     * is supplied it's loaded with the precise sequence of cells.
     * @param from
     * @param word
     * @param stack
     * @return
    */
 public boolean findWord(HoneyCell from,String word,CellStack stack)
 {
 	sweep_counter++;
 	return findWordFrom(from,word,0,stack);
 }
 /**
  * find a word starting at a particular index in the word, skipping cells 
  * that have already been used in the word (marked by sweep_counter)
  * @param from
  * @param word
  * @param idx
  * @param stack
  * @return
  */
 private boolean findWordFrom(HoneyCell from,String word,int idx,CellStack stack)
 {
 	if(from.sweep_counter==sweep_counter) { return false; }
 	from.sweep_counter = sweep_counter;
 	char match = word.charAt(idx);
 	HoneyChip top = from.topChip();
 	if(top.lcChar!=match) { return false; }
 	if(match=='q') {
 		// special case, our "Q" is actually a "QU"
 		idx++;
 		if(! ((idx<word.length() && (word.charAt(idx)=='u')))) { return false; }
 	}
 	idx++;
 	if(stack!=null) { stack.push(from); }
 	if(idx>=word.length()) { return true; }
 	for(int dir=from.geometry.n-1; dir>=0; dir--)
 	{
 		HoneyCell adj = from.exitTo(dir);
 		if((adj!=null) && findWordFrom(adj,word,idx,stack)) { return true; }
 	}
 	if(stack!=null) { stack.pop(); }
 	return false;
 }
 /**
  * find the word in some cell of the grid
  * 
  * @param word
  * @return the starting cell
  */
 public HoneyCell findWord(Entry word)
 {
	 for(HoneyCell c = allCells; c!=null; c=c.next)
	    {
		 if(findWord(c,word.word,null)) { return c; }
	    }
	 return null;
 }
 /** find all the words.  This is so amazingly fast that there's
  * no need to be fancy; just feed in the dictionary and check
  * against all the starting cells.
  * @return
  */
 public HWordStack findWords()
 {
 	HWordStack all = new HWordStack();
 	//robotVocabulary = 99999999;
 	//HoneyCell lar = findWord(dictionary.get("larvae"));
 	
  	for(int wordlen = 2;wordlen<=Dictionary.MAXLEN; wordlen++)
 		{ DictionaryHash subdict = dictionary.getSubdictionary(wordlen);
 		  for(Enumeration<Entry>e = subdict.elements(); e.hasMoreElements();)
 		  {	Entry word = e.nextElement();
 		  	if(word.order<robotVocabulary)
 		  	{	HoneyCell c = findWord(word);
 		  		if(c!=null)
 		  		{	CellStack stack = new CellStack();
 		    		findWord(c,word.word,stack);
 		    		HWord neww = new HWord(stack,word.word);
 		    		int ss = scoreWord(neww.seed); 
 		    		neww.points = ss;
 		    		all.pushNew(neww);
 		    		
 		    	}
 		    }}
 		  }
 	all.sort(true);
 	return all;
 }
}
