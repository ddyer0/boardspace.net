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
package sprint;

import static sprint.Sprintmovespec.*;

import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;
import sprint.SprintConstants.SprintState;
import dictionary.Dictionary;


/**
 * Initial work May2023
 *  
 * This shares a lot with Crosswords and other word games.
 * if any features or bug fixes occur, evaluate them too.
 * 
 * Sprint has a unique board structure.  Effectively this class is just
 * a dispatcher to an instance of SingleBoard for each player, where almost
 * all the game logic is implemented.   Only a few operations, notably
 * the "pull" operation the brings in more tiles, and handled here.
 * 
 * @author ddyer
 *
 */
class SprintBoard extends BaseBoard implements BoardProtocol
{	static int REVISION = 102;			// 100 represents the initial version of the game
										// 101 introduces the EndingGame state when finishing
										// 102 switches to "pullnew" instead of "pull"

	public int getMaxRevisionLevel() { return(REVISION); }
	
	/**
	 * the drawpile is the main data kept here rather than in the per-player copies.
	 * it contains the tiles yet to be drawn.
	 * 
	 */
	SprintCell drawPile = null;
	SprintVariation variation = SprintVariation.Sprint;
	private SprintState board_state = SprintState.Puzzle;	
	public SprintState getState() { return(board_state); }
    StringStack gameEvents = new StringStack();
    InternationalStrings s = G.getTranslations();
    
    int startingTiles = 20;	// reloaded from the variation
    int maxTiles = 40;		// reloaded from the variation
    long drawTimer = -1;
    int lastActivePlayer  = -1;
    int endgamePlayer = -1;
 	void logGameEvent(String str,String... args)
 	{	//if(!robotBoard)
 		{String trans = s.get(str,args);
 		 gameEvents.push(trans);
 		}
 	}
    public int scoreForPlayer(int idx)
    {
    	return(getPlayerBoard(idx).highScore());
    }
  
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(SprintState st) 
	{ 	
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

	public int score(int n) { return pbs[n].score(); }
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

    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public SprintChip lastPicked = null;
    public SprintChip lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public SprintCell newcell(char c,int r)
	{	throw G.Error("shouldn't be used");
	}
	
	// constructor 
    public SprintBoard(String init,int players,long key,int map[],Dictionary di,int rev) // default constructor
    {
       	dictionary = di;

        setColorMap(map, players);
    	// allocate the rack map once and for all, it's not used in the board
    	// only as part of the UI.

      	Random r = new Random(2975564);
       	drawPile = new SprintCell(r,SprintId.DrawPile);

       	doInit(init,key,players,rev); // do the initialization 


 
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
    	if(pbs==null || pbs.length!=players)
    	{	pbs = new SingleBoard[players];
    		for(int i=0;i<players;i++)
    		{
    		pbs[i] = new SingleBoard(gtype, i, key,getColorMap(),dictionary,rev);
    		}
    	}
    	else {
    		for(SingleBoard p : pbs) { p.doInit(); }
    	}
    	win = new boolean[players];
		variation = SprintVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
    	gametype = gtype;
	    maxTiles = variation.maxTiles;
	    startingTiles = variation.startTiles;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case Sprint:
			break;
		}
     	Random r = new Random(randomKey+100);
    	drawPile.reInit();
    	for(SprintChip c : SprintChip.letters)
    	{
    		drawPile.addChip(c);
    	}
		for(SprintChip ch : SprintChip.Vowels)
		{	// remove the set of vowels that will be placed on the rack
			drawPile.removeChip(ch);
		}
   	
    	drawPile.shuffle(r);
    	while(drawPile.height()>maxTiles){ drawPile.removeTop(); }
  	
    	for(SingleBoard p : pbs) { p.drawPile.copyFrom(drawPile); }
    	
    	for(int i=0;i<startingTiles;i++) { placeNewTile(); }

 		setState(SprintState.Puzzle);
	    	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    drawTimer = -1;
	    endgamePlayer = lastActivePlayer = -1;
	    lastDroppedObject = null;
	    // set the initial contents of the board to all empty cells
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }
    public int moveNumber()
    {
    	return super.moveNumber();
    }
    private void placeNewTile()
    {	SprintChip chip = drawPile.removeTop();
    	for(SingleBoard p : pbs) { p.placeNewTile(chip); }
    }
    /** create a copy of this board */
    public SprintBoard cloneBoard() 
	{ SprintBoard dup = new SprintBoard(gametype,players_in_game,randomKey,getColorMap(),dictionary,revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((SprintBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(SprintBoard from_b)
    {
        super.copyFrom(from_b);
        for(int i=0;i<pbs.length;i++) { pbs[i].copyFrom(from_b.pbs[i]); }
        board_state = from_b.board_state;
        drawPile.copyFrom(from_b.drawPile);
        lastActivePlayer = from_b.lastActivePlayer;
        endgamePlayer = from_b.endgamePlayer;
        lastPicked = null;
        sameboard(from_b); 
    }


    public void sameboard(BoardProtocol f) { sameboard((SprintBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(SprintBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        for(int i=0;i<pbs.length;i++) { pbs[i].sameboard(from_b.pbs[i]); }
        
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(drawPile.sameContents(from_b.drawPile),"drawPile mismatch");
        G.Assert(lastActivePlayer==from_b.lastActivePlayer,"lastActivePlayer mismatch");
        G.Assert(endgamePlayer==from_b.endgamePlayer,"endgamePlayer mismatch");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");

    }

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
        for(SingleBoard p : pbs) { v ^= p.Digest(r); }
		// many games will want to digest pickedSource too
		// v ^= cell.Digest(r,pickedSource);
		v ^= Digest(r,drawPile);
		v ^= Digest(r,lastActivePlayer);
		v ^= Digest(r,endgamePlayer);
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
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



    public boolean gameOverNow() { return(board_state.GameOver()); }
    public boolean winForPlayerNow(int player)
    {	if(win[player]) { return(true); }
    	boolean win = false;
    	return(win);
    }
    public boolean winForPlayer(int pl)
    {	int me = -1;
    	int other = -1;
    	for(int i=0;i<pbs.length;i++)
    	{	int sco =  pbs[i].highScore();
    		if(pl==i) { me = sco ; }
    		else { other = Math.max(other,sco );}
    	}
    	return me>other;
    }

	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { SprintChip ch = pbs[whoseTurn].pickedObject;
      if(ch!=null)
    	{	return(ch.chipNumber()); 
    	}
      	return (NothingMoving);
    }

   SingleBoard pbs[] = null;


    public boolean Execute(commonMove mm,replayMode replay)
    {	Sprintmovespec m = (Sprintmovespec)mm;
        switch(m.op)
        {
        case MOVE_START:
        	for(SingleBoard p : pbs) { p.Execute(m,replay); }
        	setState(SprintState.Play);
        	drawTimer = G.Date()+InitialDrawTime;
        	
    		break;
        case MOVE_ENDEDGAME:
        	{
        	SingleBoard pb = pbs[m.player];
        	pb.Execute(mm,replay);
        	boolean all = true;
        	for(SingleBoard p : pbs) { all &= (p.getState()==SprintState.Gameover); }
        	lastActivePlayer = m.player;
        	if(all) 
        		{ setState(SprintState.Gameover); 
        		}
        	break;
        	}
        case MOVE_ENDGAME:
        	{
        	for(SingleBoard pb : pbs) { pb.Execute(mm,replay); }
        	setState(revision<101 ? SprintState.Gameover : SprintState.EndingGame);
        	endgamePlayer = lastActivePlayer = mm.player;
        	break;
        	}
        case MOVE_SWITCH:
        	break;
        case MOVE_PULL:
         	while(drawPile.height()>m.to_row)
        	{	SprintChip chip = drawPile.removeTop();
        		for(SingleBoard p : pbs)
        		{
        			p.placeNewTile(chip);
        		}
        	}
         	restartTimer();
        	break;
        case MOVE_PULLSTART:
        	for(int lim = Math.min(pullCount(),drawPile.height()); lim>0;lim--) { drawPile.removeTop(); }
        	break;
        case MOVE_PULLNEW:
        	{
        	SingleBoard pb = pbs[m.player];        	
        	pb.Execute(mm,replay);
        	restartTimer();
        	setState(SprintState.Play);
        	}
        	break;
        	
        default:
        	SingleBoard pb = pbs[m.player];
        	int mn = pb.moveNumber();
        	pb.Execute(mm,replay);
        	int mn2 = pb.moveNumber();
        	if(mn2>mn) { moveNumber++; }
        	if(pb.getState()==SprintState.Endgame) { drawTimer =  G.Date()+EndGameTime; }
        }
        
        return (true);
    }
 public void restartTimer()
 {
 	drawTimer = G.Date()+(drawPile.height()==0 ? FinalDrawTime : NextDrawTime);
 }
 public int pullCount()
 {
	 return 2;
 }
 public String getPullMove(int p)
 {	
	 return pbs[p].getPullMove();
 }
 
 public int nextTileCount()
 {
	 return Math.max(0,drawPile.height()-pullCount());
 }
 public int tilesLeft() { return drawPile.height(); }
 
 public SingleBoard getPlayerBoard(int p)
 {
	 return pbs[p];
 }


}
