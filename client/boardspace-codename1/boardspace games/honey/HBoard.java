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
       	win = new boolean[1];
       	randomKey = key;
        boardIndex = bi;
        gametype = init;
       	dictionary = di;
        robotVocabulary = dictionary.orderedSize();
 
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
    	for(HoneyCell c = allCells; c!=null; c=c.next)
    	{
    		int n = r.nextInt(HoneyChip.letters.length);	// 25 letter alphabet
    		c.addChip(HoneyChip.letters[n]);
    	}
    	
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
        words.copyFrom(from_b.words);
        nonWords.copyFrom(from_b.nonWords);
        commonWords.copyFrom(from_b.commonWords);
        myCommonWords.copyFrom(from_b.myCommonWords);
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
		v ^= chip.Digest(r,pickedObject);
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

    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isASource(HoneyCell c)
    {	return(pickedSource==c);
    }
 

    public void setVocabulary(double value) {
    	Dictionary dict = Dictionary.getInstance();
    	robotVocabulary = (int)(dict.size()*value);
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
     
    boolean isNew = false;	// the word in the builder includes a new letter
	Hashtable<String,HWord> wordsUsed = new Hashtable<String,HWord>();
	HWordStack words = new HWordStack();
	HWordStack nonWords = new HWordStack();
	HWordStack commonWords = new HWordStack();
	HWordStack myCommonWords = new HWordStack();
	
	public String invalidReason=null;


   	CellStack endCaps = new CellStack();	// cells at the end of words
   	CellStack startCaps = new CellStack(); 	// cells at the start of words

   	// score a word on the board.  wordMultiplier and wordBonus are
    // used as scratch variables during the scoring.
    private int scoreWord(CellStack from)
    {	int score = 0;
    	for(int i=0,lim=from.size(); i<lim; i++)
    	{	HoneyCell c = from.elementAt(i);
    		HoneyChip top = c.topChip();
    		if(top==null) {  break; }
    		isNew |= isADest(c);
    		score += top.value;
     	}
    	return(score);
    }

    private void doDone(replayMode replay)
    {
        acceptPlacement();
        
         
        if (board_state==HoneyState.Resign)
        {
            win[nextPlayer()] = true;
    		setGameOver();
        }
        else
        {	
         setGameOver();
         }
    }
	public boolean notStarted()
	{
		return((droppedDest==null) && (pickedSource==null));
	}
	private void addWordTo(HWordStack which,Honeymovespec m,boolean rejectCommon)
	{
    	CellStack seed = new CellStack();
    	Tokenizer tok = new Tokenizer(m.path,",");
    	while(tok.hasMoreElements())
    	{
    		char col = G.parseCol(tok.nextElement());
    		int row = G.IntToken(tok.nextElement());
    		seed.push(getCell(col,row));
    	}
    	HWord newword = new HWord(seed,m.word);
    	if(rejectCommon && commonWords.contains(newword)) { m.isCommon = true; }
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
        if(replay!=replayMode.Replay) { animationStack.clear(); }
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
            acceptPlacement();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(HoneyState.Puzzle);	// standardize the current state
            setNextStateAfterDone(replay);

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

 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
    recordProgress(0);
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
