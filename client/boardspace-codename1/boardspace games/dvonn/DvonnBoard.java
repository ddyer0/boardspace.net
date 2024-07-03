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
package dvonn;

import online.game.*;

import java.util.*;

import lib.*;
import lib.Random;


/**
 * DvonnBoard knows all about the game of Dvonn, which is played
 * on a rectangular hexagonal board. It gets a lot of logistic 
 * support from hexboard, which know about the coordinate system.  
 * 
 * This class doesn't do any graphics or know about anything graphical, 
 * but it does know about states of the game that should be reflected 
 * in the graphics.
 * 
 *  The principle interface with the game viewer is the "Execute" method
 *  which processes moves.  
 *    
 *  In general, the state of the game is represented by the contents of the board,
 *  whose turn it is, and an explicit state variable.  All the transitions specified
 *  by moves are mediated by the state.  In general, my philosophy is to be extremely
 *  restrictive about what to allow in each state, and have a lot of trip wires to
 *  catch unexpected transitions.   We expect to be fed only legal moves, but mistakes
 *  will be made and it's good to have the maximum opportunity to catch the unexpected.
 *  
 * Note that none of this class shows through to the game controller.  It's purely
 * a private entity used by the viewer and the robot.
 * 
 * @author ddyer
 *
 */

class DvonnBoard extends hexBoard<DvonnCell> implements BoardProtocol,DvonnConstants
{	// with some care to use cell.geometry instead of 8 or 6, all the logic
	// for the game works the same.  The 
	
	static final int DEFAULT_BOARDSIZE = 5;	// 8x6 board
	static final int MAXDESTS = ((DEFAULT_BOARDSIZE+1)*3);
	static final int NUMBER_OF_CHIPS = 23;
	static int[] ZfirstInCol = { 8, 7, 6, 5, 4, 3, 2, 1, 0, 1, 2 }; // these are indexes into the first ball in a column, ie B1 has index 2
	static int[] ZnInCol = { 3, 4, 5, 5, 5, 5, 5, 5, 5, 4, 3 }; // depth of columns, ie A has 4, B 5 etc.
	static int[] ZfirstCol = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2}; // number of the first visible column in this row, 
    static final String[] DVONNGRIDSTYLE = { "3",null,"A" }; // left and bottom numbers

	DvonnState unresign;
	DvonnState board_state;
	public DvonnState getState() {return(board_state); }
	public void setState(DvonnState st) 
	{ 	unresign = (st==DvonnState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	int sweep_counter = 0;
    public boolean gameOver[] = new boolean[2]; 
    public DvonnCell rack[] = new DvonnCell[2];		// unplaced rings
    public DvonnCell captures[] = new DvonnCell[2];	// captured rings
    public CellStack animationStack = new CellStack();
    
    private int previousLastPlaced = 0;
    private int previousLastEmptied = 0;
    private int previousLastPlayer = 0;

 // factory method
	public DvonnCell newcell(char c,int r)
	{	return(new DvonnCell(c,r,cell.Geometry.Hex));
	}
    
	public int topsForPlayer=0;	// tops controlled, as part of the score
	// the score is the sum of the heights of controlled stacks
    public int scoreForPlayer(int who)
    {	int sc = 0;
    	int tops = 0;
    	for(DvonnCell c = allCells;
    	    c!=null;
    	    c =c.next)
    	{	if((c.chipIndex>=0) && (getPlayerIndex(c.topChip())==who))
    		{	sc += c.chipIndex+1;
    			tops++;
    		}
    	}
    	topsForPlayer = tops;
     	return(sc);
    }
    //
    // private variables
    //
    public void SetDrawState() { throw G.Error("shouldn't happen"); }
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    //public DvonnChip pickedObject = null;			// the object in the air
    public DvonnCell pickedStack = null;
    public int droppedHeight = 0;
    private DvonnCell pickedSource=null;
    private DvonnCell droppedDest=null;
    DvonnChip lastDropped = null;
    private String undoInfo = null;
    private StateStack robotState = new StateStack();
    public int robotDepth = 0;
    public boolean isSource(DvonnCell c)
    {	return(c==pickedSource);
    }
    public int movingObjectIndex()
    {	DvonnCell ps = pickedStack;
    	return((ps.height()==0) ? NothingMoving : DvonnId.PickedStack.ordinal());
     }
    


    private void finalizePlacement()	// finalize placement, no more undo
    { 	G.Assert(pickedStack.height()==0,"picked stack empty");
    	droppedDest = null;
    	undoInfo = null;
    	pickedSource = null;
    	droppedHeight = 0;
    }   
    public Hashtable<DvonnCell,DvonnCell> getRemoved()
    {	return(undoCaptures(undoInfo,false));
    }	
    private Hashtable<DvonnCell,DvonnCell> undoCaptures(String info,boolean doit)
    {	Hashtable<DvonnCell,DvonnCell> hv = doit ? null : new Hashtable<DvonnCell,DvonnCell>();
    	if(info!=null)
    	{
    	int row = 0;
    	char col = 'x';
    	int idx = 0;
    	int len = info.length();
    	DvonnCell cell = null;
    	while(idx<len)
    	{
    	char ch = info.charAt(idx++);
    	switch(ch)
    	{
    	default: throw G.Error("not expecting %s",ch);
    	case ':':
    		col = info.charAt(idx++);		// column
    		row = (info.charAt(idx++)-'0');	// first char of row
    		cell = null;
    		break;
   		case '0': case '1': case '2': case '3': case '4':	// second char of row
   		case '5': case '6': case '7': case '8': case '9': row = row*10+(ch-'0');
   				cell = getCell(col,row);
   				if(hv!=null) { hv.put(cell,cell); }
   				break;
   		case DvonnChip.WhiteChipName:	
   			{
   			if(cell==null) { cell = getCell(col,row); if(hv!=null) { hv.put(cell,cell); } }
   			if(doit)
   				{DvonnChip pch = captures[FIRST_PLAYER_INDEX].removeTop();
   				cell.addChip(pch);
   				}
   			break;
   			}
   		case DvonnChip.BlackChipName:
   			{
   			if(cell==null) { cell = getCell(col,row); if(hv!=null) { hv.put(cell,cell); }  }
   			if(doit)
   				{DvonnChip pch = captures[SECOND_PLAYER_INDEX].removeTop();
   				cell.addChip(pch);
   				}
   			}
   			break;
    	
    	}
    	}
    	}
    	return(hv);
    }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    	DvonnCell dr = droppedDest;
    	if(dr!=null)
    	{
    	undoCaptures(undoInfo,true);
    	moveStack(dr,pickedStack,droppedHeight);
    	droppedDest =  null;
   
    	dr.lastPlaced = previousLastPlaced;    	

    	}
       	undoInfo = null;
    }
    // lowest level of uncapture.  "removed" is the most distant cell removed by the capture
    // picked and dropped are the source and destination respectively.  This handles both
    // capture by approach and by withdrawal.

    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	
    	if((pickedStack.height()>0) && (droppedDest==null))
    	{
    	moveStack(pickedStack,pickedSource,pickedStack.height());
        
        pickedSource.lastEmptied = previousLastEmptied;
        pickedSource.lastEmptiedPlayer = previousLastPlayer;
        pickedSource = null;

     	}
     }
    // 
    // drop the floating object.
    //
    private void dropObject(DvonnCell d)
    {
       G.Assert((pickedStack.height()>0)&&(droppedDest==null),"ready to drop");
       droppedDest = d;
       droppedHeight = pickedStack.height();
       moveStack(pickedStack,d,droppedHeight);
       
       previousLastPlaced = d.lastPlaced;
       d.lastPlaced = moveNumber;

       lastDropped = d.topChip();
    }
    private void pickObject(DvonnCell src,int height)
    {  	pickedSource = src;
   		int sz = (height>0) ? height : src.height();
    	moveStack(src,pickedStack,sz);
    	
		previousLastEmptied = src.lastEmptied;
		previousLastPlayer = src.lastEmptiedPlayer;
		src.lastEmptied = moveNumber;
		src.lastEmptiedPlayer = whoseTurn;

     }
    
    private DvonnCell getCell(DvonnId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case Second_Player_Captures:
        	return(captures[SECOND_PLAYER_INDEX]);
         case First_Player_Captures:
        	return(captures[FIRST_PLAYER_INDEX]);
        case Second_Player_Rack:
        	return(rack[SECOND_PLAYER_INDEX]);
         case First_Player_Rack:
        	return(rack[FIRST_PLAYER_INDEX]);
        case BoardLocation:
         	return(getCell(col,row));

       	}
    }

   
    private DvonnCell[][]tempDestResource = new DvonnCell[4][];
    private int tempDestIndex=-1;
    public synchronized DvonnCell []getTempDest() 
    	{ if(tempDestIndex>=0) { return(tempDestResource[tempDestIndex--]); }
    	  return(new DvonnCell[MAXDESTS]);
    	}
    public synchronized void returnTempDest(DvonnCell[]d) { tempDestResource[++tempDestIndex]=d; }


    public DvonnBoard(long random,String init,int map[]) // default constructor
    {  	drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_NOTHING or STYLE_CELL or STYLE_LINES
    	Grid_Style = DVONNGRIDSTYLE; //coordinates left and bottom
    	Random r = new Random(6343357);
    	rack[FIRST_PLAYER_INDEX] = new DvonnCell(r,DvonnId.First_Player_Rack);
    	rack[SECOND_PLAYER_INDEX] = new DvonnCell(r,DvonnId.Second_Player_Rack);
    	captures[FIRST_PLAYER_INDEX] = new DvonnCell(r,DvonnId.First_Player_Captures);
    	captures[SECOND_PLAYER_INDEX] = new DvonnCell(r,DvonnId.Second_Player_Captures);
    	pickedStack = new DvonnCell(r,DvonnId.PickedStack);
       	initBoard(ZfirstInCol, ZnInCol, ZfirstCol);
       	allCells.setDigestChain(r);
    	setColorMap(map, 2);            	
    	doInit(init,random); // do the initialization 
    }

    private void init_racks()
    {	reInit(rack);
    	reInit(captures);
        for(int pl = FIRST_PLAYER_INDEX; pl<=SECOND_PLAYER_INDEX;pl++)
        {	
        	for(int i=0;i<NUMBER_OF_CHIPS;i++) 
        	{	rack[pl].addChip(DvonnChip.getChip(pl));	
        	}
        }
        DvonnChip red = DvonnChip.getChip(DvonnChip.DVONN_CHIP_INDEX);
        rack[FIRST_PLAYER_INDEX].addChip(red);
        rack[FIRST_PLAYER_INDEX].addChip(red);
        rack[SECOND_PLAYER_INDEX].addChip(red);
        
  	
    }
    private void Init_Standard(String game,long key)
    {  boolean isBoard = Dvonn_INIT.equalsIgnoreCase(game);
       if(!isBoard) { throw G.Error(WrongInitError,game); }  
        gametype = game;
        randomKey = key;
        setState(DvonnState.PUZZLE_STATE);
        for(DvonnCell c=allCells;
            c!=null;
            c=c.next)
        {	c.reInit();
        }
        init_racks();
        whoseTurn = FIRST_PLAYER_INDEX;
    }
    public void sameboard(BoardProtocol f) { sameboard((DvonnBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(DvonnBoard from_b)
    {
    	super.sameboard(from_b);
    	
    	G.Assert(sameCells(rack,from_b.rack),"Rack mismatch");
    	G.Assert(sameCells(captures,from_b.captures),"Captures mismatch");
        G.Assert(sameCells(pickedSource,from_b.pickedSource),"pickedSource matches");
        G.Assert(sameCells(droppedDest,from_b.droppedDest),"droppedDest matches");
        G.Assert(sameCells(pickedStack,from_b.pickedStack),"pickedStack matches");
        G.Assert(pickedStack.height()==from_b.pickedStack.height(),"picked Height matches");
   }

    /** this is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game.  Other site machinery looks for duplicate digests.
     * @return
     */
    public long Digest()
    {
    	

        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        Random r = new Random(64 * 1124); // init the random number generator
        long v = super.Digest(r);

		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
		v ^= Digest(r,captures);
		v ^= Digest(r,rack);
		v ^= cell.Digest(r,pickedStack);
		v ^= cell.Digest(r,pickedSource);

		return (v);
    }
    
    public DvonnCell getCell(DvonnCell c)
    {	return((c==null) 
    			? null
    			: getCell(c.rackLocation(),c.col,c.row));
    }
    public DvonnBoard cloneBoard() 
	{ DvonnBoard dup = new DvonnBoard(randomKey,gametype,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((DvonnBoard)b); }
 
    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(DvonnBoard from_b)
    {	super.copyFrom(from_b);

        copyFrom(rack,from_b.rack);
        copyFrom(captures,from_b.captures);
        copyFrom(pickedStack,from_b.pickedStack);
        droppedHeight = from_b.droppedHeight;
        undoInfo = from_b.undoInfo;
        pickedSource = getCell(from_b.pickedSource);
        droppedDest = getCell(from_b.droppedDest);
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        robotDepth = from_b.robotDepth;
        
        previousLastPlaced = from_b.previousLastPlaced;
        previousLastEmptied = from_b.previousLastEmptied;
        previousLastPlayer = from_b.previousLastPlayer;

        sameboard(from_b);	// check
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {
       Init_Standard(gtype,key);
       animationStack.clear();
       AR.setValue(win,false);
       pickedStack.reInit();
       robotState.clear();
       robotDepth = 0;
       previousLastPlaced = 0;
       previousLastEmptied = 0;
       previousLastPlayer = 0;

       droppedHeight = 0;
       pickedSource = null;
       droppedDest = null;
       undoInfo = null;
       moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }


    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Move not complete, can't change the current player");
        case PUZZLE_STATE:
            break;
        case CONFIRM_STATE:
        case CONFIRM_PLACE_STATE:
        case PASS_STATE:
        case DRAW_STATE:
        case RESIGN_STATE:
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
    {	
        switch (board_state)
        {case RESIGN_STATE:
         case CONFIRM_STATE:
         case PASS_STATE:
         case CONFIRM_PLACE_STATE:
         case DRAW_STATE:
            return (true);

        default:
            return (false);
        }
    }
    public boolean DigestState()
    {	if((pickedStack.height()>0)) { return(false); }
    	switch(board_state)
    	{
    	default: return(DoneState());
    	}
    }


    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(DvonnState.GAMEOVER_STATE);
    }

    static final double ADJ_SELF_PENALTY = -0.1;
    static final double ADJ_OTHER_BONUS = 0.01;
    static final double CAPTURE_PENALTY = 0.25;
    static final double TOP_BONUS = 0.5;
    // look for a win for player.  This algorithm should work for Gobblet Jr too.
    public double ScoreForPlayer(int player,boolean print,boolean dumbot)
    {  	int sc = scoreForPlayer(player);
    	int tops = topsForPlayer;
    	double rv = 0.0;
    	switch(board_state)
    	{
    	default: throw G.Error("Not implemented");
    	case PLACE_RING_STATE:
    	case CONFIRM_PLACE_STATE:	
    	{	Random seed = new Random(randomKey);
    		int nextPlay = nextPlayer[player];
    		//
    		// theory here; we want a mostly random distribution for lack of 
    		// any better motivation, but some penalty for playing in clusters
    		// of our own color, and some bonus for playing adjacent to opposing colors.
    		//
    		for(DvonnCell c = allCells;
    			c!=null;
    			c=c.next)
    		{
    		double rr = ((seed.nextInt()%1000)/1000.0);		// a small random value 
    		DvonnChip top = c.topChip();
    		if(top==null) { rv+=rr; }						// empty spaces score the random
    		else if(getPlayerIndex(top)!=nextPlay)
    		{	for(int dir=0;dir<CELL_FULL_TURN;dir++)			// for our pieces, survey the terrain
    			{	DvonnCell adj = c.exitTo(dir);
    				if(adj!=null)
    				{	DvonnChip adp = adj.topChip();
    					if(adp!=null)
    					{
    					if(adp.colorIndex==top.colorIndex)
    						{ rv += ADJ_SELF_PENALTY; 
    						}
    					else { rv += ADJ_OTHER_BONUS; }
    					}
    				}
    			}
    		}
    		}
    	}
    	break;
    	case GAMEOVER_STATE:	// for sure only the actual score matters.
    		break;
    	case PASS_STATE:
    	case PLAY_STATE:
    	case CONFIRM_STATE:
    		rv += captures[getColorMap()[player]].chipIndex*CAPTURE_PENALTY;
    		rv += tops*TOP_BONUS;
    		// add other considerations besides the score
    		break;
    	}
      	return(sc+rv);
    }


    //
    // in the actual game, picks are optional; allowed but redundant.
    //
    private void setNextStateAfterDrop()
    {	switch(board_state)
    	{
    	default: throw G.Error("Not expected drop in state %s",board_state);
    	case PLAY_STATE: setState(DvonnState.CONFIRM_STATE); break;
    	case PLACE_RING_STATE: setState(DvonnState.CONFIRM_PLACE_STATE); break; 
    	}
    }
    private void setNextStateAfterPick()
    {        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting pick in state %s", board_state);
        case CONFIRM_STATE:
        case DRAW_STATE:
        	setState(DvonnState.PLAY_STATE);
        	break;
        case PLAY_STATE:
			break;
        case CONFIRM_PLACE_STATE:
        	setState(DvonnState.PLACE_RING_STATE);
        	break;
        case PLACE_RING_STATE:
        case PUZZLE_STATE:
            break;
        }
    }
    public boolean placingRings()
    {	switch(board_state)
    	{	
    	case PUZZLE_STATE:	return(!allPlaced());
    	case PLACE_RING_STATE:
    	case CONFIRM_PLACE_STATE: return(true);
    	default: return(false);
    	}
    }
    public boolean allPlaced()
    {
    	return((rack[FIRST_PLAYER_INDEX].chipIndex<0) && (rack[SECOND_PLAYER_INDEX].chipIndex<0));
    }
    private void doGameEnd()
    {	if(allPlaced())
    	{
		setState(DvonnState.PLAY_STATE);	// default, and makes hasLegalMoves work 
		boolean hasmoves = hasLegalMoves(whoseTurn);
		boolean othermoves = hasLegalMoves(nextPlayer[whoseTurn]);
		if(hasmoves) {  setState(DvonnState.PLAY_STATE); }
		else if(othermoves) { setState(DvonnState.PASS_STATE); }
		else	// no one has moves, gameover
			{ int myScore = scoreForPlayer(whoseTurn);
			  int nxScore = scoreForPlayer(nextPlayer[whoseTurn]);
			  setGameOver(myScore>nxScore,nxScore>myScore);
			}
    	}
    	else
    	{	setState(DvonnState.PLACE_RING_STATE);
    	}
    }
    
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting NextState in state %s",board_state);
    	
    	case GAMEOVER_STATE: 
    		break;

        case DRAW_STATE:
        	setGameOver(false,true);
        	break;
    	case CONFIRM_STATE:
    	case PASS_STATE:
    		doGameEnd();
    		break;
    	case CONFIRM_PLACE_STATE:
    	case PUZZLE_STATE:
    	case PLACE_RING_STATE:
    	case PLAY_STATE:
    		setState(allPlaced()?DvonnState.PLAY_STATE:DvonnState.PLACE_RING_STATE);
    		break;
    	}

    }
     
    private void doDone(DvonnMovespec m)
    {	
    	if(board_state==DvonnState.RESIGN_STATE) 
    	{ setGameOver(false,true);
    	}
    	else
    	{
    	switch(board_state)
    	{
    	default: throw G.Error("not expecting doDone state %s",board_state);
    	case DRAW_STATE:
    	case PUZZLE_STATE: break;
    	case CONFIRM_PLACE_STATE:
    		finalizePlacement();
    		if(allPlaced())
    		{ // keep the same player, start regular play
    		setState(DvonnState.PLAY_STATE);
            if(board_state==DvonnState.PLAY_STATE)
            {	m.setLineBreak(true);
            }

    		}
    		else
    		{
    		setNextPlayer(); 
    		setState(DvonnState.PLACE_RING_STATE);
    		}
    		break;
    	case PASS_STATE:
    	case CONFIRM_STATE:  
    		finalizePlacement();
       	 	setNextPlayer(); 
     		setNextStateAfterDone();
     		break;
    	}}
    }
    
    // move a stack of chips preserving the stack order
    public void moveStack(DvonnCell from,DvonnCell to,int depth)
    {	if((depth>0)&&(from.chipIndex>=0))
    		{ DvonnChip chip = from.removeTop();
    		  moveStack(from,to,depth-1);
    		  to.addChip(chip); 
    		}
    }
    
    // continue a sweep from "c"
    public void sweepForDvonnFrom(DvonnCell c)
    {	if((c.chipIndex>=0) && (c.sweep_counter!=sweep_counter))
    	{	c.sweep_counter=sweep_counter;
    		for(int dir=0;dir<6;dir++)
    		{ DvonnCell nxt = c.exitTo(dir);
    		  if(nxt!=null) { sweepForDvonnFrom(nxt); }
    		}
    	}
    }
    private void adjustPosition(DvonnCell an,DvonnCell c,DvonnChip prev,int off)
    {
    	int yoffset = (int)((off-1)*c.lastSize()*c.lastYScale());
		int xoffset = (int)((off-1)*c.lastSize()*c.lastXScale());
		an.duplicateCurrentCenter(c);
		an.setCurrentCenter(c.centerX()-xoffset,c.centerY()+yoffset);
		animationStack.push(an);
		animationStack.push(captures[prev.colorIndex]);
		
    }
    // remove a stack of chips, place the removed in the captured stack
    public void removeStack(DvonnCell c,replayMode replay)
    {	DvonnChip prev = null;
    	DvonnCell an = null;
    	int off = -1;
    	while(c.topChip()!=null)
    	{   
    		DvonnChip ch = c.removeTop();
    		DvonnCell dest = captures[ch.colorIndex];
        	dest.addChip(ch);
        	off++;
        	if(replay.animate)
        	{	if((prev==null) || (ch!=prev)) 
        		{ 
        		if(an!=null)
        		{	adjustPosition(an,c,prev,off);
        		}
        		an = c.copy();
        		an.reInit();
        		}
        		an.addChip(ch);
        		prev = ch;
         	}
    	}
    	if(an!=null)
    	{
    		adjustPosition(an,c,prev,off);
    	}
    	
    }
    
    // sweep the board and remove chips that are not in contact 
    // with any dvonn piece
    public String sweepForDvonnContact(replayMode replay)
    {	int dvonnTot = 0;
    	String captureString = null;
    	sweep_counter++;
    	for(DvonnCell c=allCells;
    	    (c!=null) && (dvonnTot<3);
    	    c=c.next)
    	{
    	int dcount = c.hasDvonn;
    	if(dcount>0)
    	{
    	dvonnTot+=dcount;
    	if(c.sweep_counter!=sweep_counter)
    		{ sweepForDvonnFrom(c);
    		}
    	}
    	}
    	
       	for(DvonnCell c=allCells;
       		(c!=null);
       		c=c.next)
       	{
       	if((c.chipIndex>=0) && (c.sweep_counter!=sweep_counter))
       		{	if(captureString==null) { captureString=""; }
       			captureString += c.idString();
       			removeStack(c,replay);
       		}
       	}
       	return(captureString);		// undo info or null
    }
     public boolean Execute(commonMove mm,replayMode replay)
    {	DvonnMovespec m = (DvonnMovespec)mm;
        if(replay.animate) { animationStack.clear(); }
        //System.out.println("E "+m+" for "+whoseTurn );
        switch (m.op)
        {
        case MOVE_DONE:
         	doDone(m);

            break;
        case MOVE_BOARD_BOARD:
        	{
        	DvonnCell from = getCell(m.from_col, m.from_row);
        	DvonnCell to = getCell(m.to_col, m.to_row);
        	int height = droppedHeight = from.height();
        	m.setSrcHeight(height);
        	m.setSrcChip(from.topChip());
        	if(replay.animate)
        	{
        		animationStack.push(from.copy());
        		animationStack.push(to);
        	}
        	m.setDestChip(to.topChip());
        	m.setDestHeight(to.height());
        	moveStack(from,to,height);
        	undoInfo = m.undoInfo = sweepForDvonnContact(replay);
        	
        	previousLastEmptied = from.lastEmptied;
        	previousLastPlayer = from.lastEmptiedPlayer;
        	previousLastPlaced = to.lastPlaced;
        	from.lastEmptied = moveNumber;
        	from.lastEmptiedPlayer = whoseTurn;
        	to.lastPlaced = moveNumber;

            setNextStateAfterDrop();
        	}
        	break;
        case MOVE_DROPB:
        	{
        	boolean picked = false;
        	if((board_state==DvonnState.PLACE_RING_STATE) && (pickedStack.height()==0))
        	{	pickObject(rack[getColorMap()[whoseTurn]],1);
        		picked = true;
        	}
        	
        	DvonnCell from = pickedSource;
        	DvonnCell target =  getCell(m.to_col, m.to_row);
        	if(from==target) { unPickObject(); }
        	else
        	{
            switch(board_state)
            {
            default:
            	throw G.Error("Not expecting state %s",board_state);
            case PUZZLE_STATE:
                dropObject(target);
                finalizePlacement();
            	break;
            case PLAY_STATE:
            	if(replay==replayMode.Single)
            	{	DvonnCell cop = pickedStack.copy();    				
            		cop.setScreenData(from.getScreenData());
            		animationStack.push(cop);
            		animationStack.push(target);
            	}
            	m.setDestChip(target.topChip());
                m.setDestHeight(target.height());
                dropObject(target);
         	   	undoInfo = m.undoInfo = sweepForDvonnContact(replay);
                setNextStateAfterDrop();
               break;
            case PLACE_RING_STATE:
            	{
            	//DvonnCell dest = droppedDest[stackIndex];
                	if(picked || replay==replayMode.Single)
                	{	DvonnCell cop = from.copy();
                		while(cop.height()>1) { cop.removeTop(); }
            			animationStack.push(cop);
                 		animationStack.push(target);
                	}
                   	m.setSrcChip(pickedStack.topChip());
                	m.setSrcHeight(1);
                	m.setDestChip(target.topChip());
                    m.setDestHeight(target.height());
                    dropObject(target);
                    setNextStateAfterDrop();
            	}
            }}}
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary pick
        	switch(board_state)
        	{	
           	case CONFIRM_STATE:
           	case CONFIRM_PLACE_STATE:
           		unDropObject();
           		break;
           	default:
           		DvonnCell src = getCell(DvonnId.BoardLocation, m.from_col, m.from_row);
           		m.setSrcChip(src.topChip());
           		m.setSrcHeight(src.height());
           		pickObject(src,0);
        	}
        	setNextStateAfterPick();
        	 
            break;
         case MOVE_DROP: // drop on chip pool;
            dropObject(getCell(m.source, m.to_col, m.to_row));
            finalizePlacement();
            break;

        case MOVE_PICK:
            unDropObject();
            unPickObject();
            pickObject(getCell(m.source, m.from_col, m.from_row),1);
            setNextStateAfterPick();
            break;

        case MOVE_PASS:
        	setState(DvonnState.CONFIRM_STATE);
        	break;
        	
        case MOVE_START:
            setWhoseTurn(m.player);
            finalizePlacement();
            setState(DvonnState.PUZZLE_STATE);
            doGameEnd();
            break;

        case MOVE_RESIGN:
            setState(unresign==null?DvonnState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
        	finalizePlacement();
             setState(DvonnState.PUZZLE_STATE);
  
            break;
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;

        default:
        	cantExecute(m);
        }


        return (true);
    }

    // legal to hit the chip reserve area
    public boolean LegalToHitChips(int color)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
         case CONFIRM_STATE:
         case DRAW_STATE:
         case PLAY_STATE: 
         case RESIGN_STATE:
         case PASS_STATE:
        	return(false);

         case CONFIRM_PLACE_STATE:
        	 return(false);
         case PLACE_RING_STATE:
        	 return(getColorMap()[color]==whoseTurn);
		case GAMEOVER_STATE:
			return(false);
        case PUZZLE_STATE:
        	return(true);
        }
    }
  
    // true if it's legal to drop a stack originating from fromCell on toCell
    public boolean LegalToDropOnBoard(DvonnCell fromCell,int height,DvonnCell toCell)
    {	if(fromCell==toCell) { return(true); }
      	DvonnCell temp[] = getTempDest();
    	int n = getDestsFrom(fromCell,height,temp,0);
    	boolean ok = false;
    	for(int i=0;(i<n) && !ok;i++) 
    		{ if(temp[i]==toCell) 
    		{ ok = true; } 
    		}
    	returnTempDest(temp);
    	return(ok);
    }

    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    private boolean isDest(DvonnCell cell)
    {	return(droppedDest==cell);
    }

    public boolean LegalToHitBoard(DvonnCell cell)
    {	
        switch (board_state)
        {
        case CONFIRM_PLACE_STATE:
        	return(isDest(cell));
        case PLACE_RING_STATE:
        	return(cell.chipIndex<0);
 		case PLAY_STATE:
 			if((pickedStack.height()>0) && (droppedDest==null))
 			{	return(LegalToDropOnBoard(pickedSource,pickedStack.height(),cell));
 			}
 			return((cell.chipIndex>=0)
 					&& (getPlayerIndex(cell.topChip())==whoseTurn)
 					&& (getDestsFrom(cell,cell.height(),null,0)>0));
		case GAMEOVER_STATE:
		case PASS_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
		case DRAW_STATE:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);

        case PUZZLE_STATE:
        	return((pickedStack.height()==0)
        			?(cell.chipIndex>=0)	// pick from a nonempty cell
        			: true);				// drop on any cell
        }
    }

 
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(DvonnMovespec m)
    {
        robotState.push(board_state); //record the starting state. The most reliable
        robotDepth++;
        // to undo state transitions is to simple put the original state back.
        
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        //G.print("R "+m+" for "+m.player);
        if (Execute(m,replayMode.Replay))
        {	if(m.op==MOVE_BOARD_BOARD)
        	{	if(DoneState()) 
        			{  doDone(m); 
        			}
        		else
        			{ m.undoInfo=null;
        			}
        	}
        	else if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {
                doDone(m);
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
    public void UnExecute(DvonnMovespec m)
    {	int who = whoseTurn;
    	robotDepth--;
    	setWhoseTurn(m.player);
        //G.print("U "+m+" for "+whoseTurn);

        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;

   	    case MOVE_DROPB:
   	    	{ DvonnCell c = getCell(m.to_col,m.to_row);
   	    	  DvonnChip ch = c.removeTop();
   	    	  rack[getColorMap()[m.player]].addChip(ch);
   	    	  droppedDest=null;
   	    	}
	    	break;
	    	  
        case MOVE_PASS:
        case MOVE_DONE:
            break;
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
        		case PASS_STATE:
        		case PLAY_STATE:
        			if(m.undoInfo!=null) 
        				{ 	undoCaptures(m.undoInfo,true); 
        				}
        			DvonnCell dest = getCell(m.to_col,m.to_row);
        			DvonnCell src = getCell(m.from_col,m.from_row);
        			moveStack(dest,src,m.getSrcHeight());
         			break;
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(robotState.pop());
        if(who!=m.player)
        	{
        	moveNumber--;
        	}
 }


 int getDestsFrom(DvonnCell from,int height,DvonnCell temp[],int idx)
 {	int n=idx;
 	switch(board_state)
 	{
 	default: throw G.Error("Not implemented");
 	case PLAY_STATE:
 		if(from.onBoard)
 		{	
 			if(height>0)
 			{
 			boolean hasEmpty=false;
 			for(int dir=0;dir<6;dir++)
 			{	DvonnCell nc = from.exitTo(dir);
 				if((nc==null) || (nc.chipIndex<0)) { hasEmpty=true; }
 				for(int i=1;i<height && nc!=null;i++)
 				{	nc = nc.exitTo(dir);
 				}
 				if((nc!=null) && (nc.chipIndex>=0)) 
 					{ if(temp!=null) { temp[n]=nc; };
 					n++; 
 					}
 			}
 			if(!hasEmpty) { n=idx; }	// if totally surrounded, no moves
 			}
 		}
 		break;
 	case CONFIRM_STATE:
 	case PLACE_RING_STATE:
 	case PUZZLE_STATE: 
 		break;
 	}
 	return(n);
 }
 
 // true of either player has a legal move
 boolean hasLegalMoves(int forp)
 {
	 for(DvonnCell c = allCells;
	     c!=null;
	     c = c.next)
	 {	int height = c.height();
	 	if((height>0)
	 			&& (getPlayerIndex(c.topChip())==forp)
	 			&& getDestsFrom(c,height,null,0)>0) { return(true); }
	 }
	 return(false);
 }
 // get a hash of moves destinations, used by the viewer
 // to know where to draw dots and also to determine what
 // is a legal move
 public Hashtable<DvonnCell,DvonnCell> getMoveDests()
 {	Hashtable<DvonnCell,DvonnCell> dd = new Hashtable<DvonnCell,DvonnCell>();
 	int stackHeight = pickedStack.height();
 	if(stackHeight>0)
 	{	DvonnCell temp[] = getTempDest();
 		int n = getDestsFrom(pickedSource,stackHeight,temp,0);
 		for(int i=0;i<n;i++)
 		{	DvonnCell c = temp[i];
 			dd.put(c,c);
 		}
 		returnTempDest(temp);
 	}
	return(dd);
 }
 private int getPlayerIndex(DvonnChip ch) 
 { 	int ci = ch.colorIndex;
 	return(ci<=1 ? getColorMap()[ci] : ci);
 }
 CommonMoveStack  GetListOfMoves(CommonMoveStack  all,boolean erupting,boolean nonerupting)
 {	
	switch(board_state)
	{
	default:
		throw G.Error("Not implemented");
	case CONFIRM_PLACE_STATE:
	case CONFIRM_STATE:
		all.addElement(new DvonnMovespec(MOVE_DONE,whoseTurn));
		break;
	case PASS_STATE:
		all.addElement(new DvonnMovespec(MOVE_PASS,whoseTurn));
		break;
	case PLACE_RING_STATE:
		for(DvonnCell c = allCells;
			c !=null;
			c=c.next)
		{	if(c.chipIndex<0) 
				{ all.addElement(new DvonnMovespec(MOVE_DROPB,c.col,c.row,whoseTurn)); 
				}
		}
		break;
	case PLAY_STATE:
		{
		DvonnCell temp[] = getTempDest();
		for(DvonnCell c = allCells;
		c !=null;
		c=c.next)
		{	DvonnChip cp = c.topChip();
			if((cp!=null) && (getPlayerIndex(cp)==whoseTurn))
			{ 	int n = getDestsFrom(c,c.height(),temp,0);
				for(int i=0;i<n;i++)
				{	DvonnCell dest = temp[i];
					all.addElement(new DvonnMovespec(c.col,c.row,dest.col,dest.row,whoseTurn));
					
				}
			}
		}
		returnTempDest(temp);
		}
		break;
	}
  	return(all);
 }

}
 