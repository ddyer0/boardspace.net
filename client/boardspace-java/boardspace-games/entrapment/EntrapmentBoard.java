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
package entrapment;

import java.awt.Color;

/* below here should be the same for codename1 and standard java */
import online.game.*;
import java.util.*;
import lib.*;
import lib.Random;


/**
 * EntrapmentBoard knows all about the game of Entrapment, which is played
 * on a 7x7 board. It gets a lot of logistic support from 
 * common.rectBoard, which knows about the coordinate system.  
 * 
 * The basic board is a 7x7 grid with square connectivity - no diagonals
 * cells have an additional "adjacency grid of unplacedBarriers, with one barrier 
 * between every pair of cells.  Barriers have Geometry.Line connectivity
 * 
 * This class doesn't do any graphics or know about anything graphical, 
 * but it does know about states of the game that should be reflected 
 * in the graphics.
 * 
 * The principle interface with the game viewer is the "Execute" method
 * which processes moves.  Note that this
 *  
 * In general, the state of the game is represented by the contents of the board,
 * whose turn it is, and an explicit state variable.  All the transitions specified
 * by moves are mediated by the state.  In general, my philosophy is to be extremely
 * restrictive about what to allow in each state, and have a lot of tripwires to
 * catch unexpected transitions.   We expect to be fed only legal moves, but mistakes
 * will be made and it's good to have the maximum opportunity to catch the unexpected.
 *  
 * Note that none of this class shows through to the game controller.  It's purely
 * a private entity used by the viewer and the robot.
 * 
 * @author ddyer
 *
 */

class EntrapmentBoard extends squareBoard<EntrapmentCell> implements BoardProtocol,EntrapmentConstants
{	
	static int REVISION = 102;		// revision 101 restores standard undo behavior
									// revision 102 changes the dead/capture behavior to prefer adjacent to barriers played
	static final int DEFAULT_COLUMNS = 7;	// 8x6 board
	static final int DEFAULT_ROWS = 7;
	static final int STARTING_BARRIERS = 25;
	static final int STARTING_ROAMERS = 3;
 	static final int White_Chip_Index = 0;
	static final int Black_Chip_Index = 1;
  	static final EntrapmentId RackLocation[] = { EntrapmentId.White_Barriers,EntrapmentId.Black_Barriers};
    static final EntrapmentId RoamerLocation[] = { EntrapmentId.White_Roamers,EntrapmentId.Black_Roamers};
    static final EntrapmentId DeadRoamerLocation[] = { EntrapmentId.DeadWhiteRoamers,EntrapmentId.DeadBlackRoamers };
    static final String[] ENTRAPMENTGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

    private int prevLastPicked = -1;
    private int prevLastDropped = -1;
    int dropState = 0;
    
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
				if(DEBUG && G.p1(msg) && robot!=null)
				{	String dir = "g:/share/projects/boardspace-html/htdocs/entrapment/entrapmentgames/robot/";
					robot.saveCurrentVariation(dir+msg+".sgf");
					return(true);
				}
				return(false);
			}
	public int getMaxRevisionLevel() { return(REVISION); }
	
	EntrapmentState unresign;
	EntrapmentState board_state;
	public EntrapmentState getState() {return(board_state); }
	public void setState(EntrapmentState st) 
	{ 	unresign = (st==EntrapmentState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public int boardColumns = DEFAULT_COLUMNS;	// vertical size of the board
    public int boardRows = DEFAULT_ROWS;		// horizontal size
    public boolean is6x7 = false;
    public int roamers_per_player;
     //
    // public variables which the viewer uses.
    //
    public EntrapmentCell unplacedBarriers[] = new EntrapmentCell[2];		// unused unplacedBarriers per player
    public EntrapmentCell unplacedRoamers[] = new EntrapmentCell[2];			// unplaced unplacedRoamers for both players
    public EntrapmentCell deadRoamers[] = new EntrapmentCell[2];

    public int nTrapped[] = new int[2];
    public int robotDepth = 0;
    public EntrapmentCell roamerCells[][] = null;	// active unplacedRoamers on the board
    CellStack deadCells  = new CellStack();
    public CellStack animationStack = new CellStack();
    
    private boolean inInitalSetup() 
    { 	// if no unplacedBarriers are missing from the stacks, and some unplacedRoamers remain to be placed
    	// then we're in initial setup phase
    	return( (unplacedBarriers[0].height()==STARTING_BARRIERS)
    				&& (unplacedBarriers[1].height()==STARTING_BARRIERS)
    				&& ((unplacedRoamers[0].height()>0) || (unplacedRoamers[1].height()>0)));
    }
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public boolean initialSetupPhase = true;
    public EntrapmentChip pickedObject = null;
    public EntrapmentCell pickPart1 = null;
    public EntrapmentCell droppedPart1 = null;
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    public IStack stateIStack = new IStack();
    public StateStack stateStack = new StateStack();
    //
    // private variables
    //
    private EntrapmentCell allBarriers[] = null;	// all the unplacedBarriers interspersed among the regular cells

    public int flippedBarriers[] = new int[2];
	   
	// factory method
	public EntrapmentCell newcell(char c,int r)
	{	EntrapmentCell cx = new EntrapmentCell(c,r);
		return cx;
	}
    public EntrapmentBoard(String init,long rv,int[]map,int rev) // default constructor
    {   
        setColorMap(map, 2);
        doInit(init,rv,rev); // do the initialization 
      }
    public void SetDrawState() { setState(EntrapmentState.DRAW_STATE); }

    // unplacedBarriers are fetched to the right and top from some regular cell
    // row A1 gets the horizontal barrier between row A1 and A2
    public EntrapmentCell getHBarrier(char col,int row)
    {	EntrapmentCell c = getCell(col,row);
    	return(c.barriers[CELL_UP]);
    }
	public EntrapmentCell getHBarrier(EntrapmentCell c)
	{
		return(c.barriers[CELL_UP]);
	}
    // column A1 gets the vertical barrier between A1 and B1
     public EntrapmentCell getVBarrier(char col,int row)
    {	EntrapmentCell c = getCell(col,row);
    	return(c.barriers[CELL_RIGHT]);
    }
     public EntrapmentCell getVBarrier(EntrapmentCell c)
     {
     	return(c.barriers[CELL_RIGHT]);
     }

    public void initBoard(int boardColumns,int boardRows)
    {	Random r = new Random(6362670);
    	super.initBoard(boardColumns,boardRows);
    	allCells.setDigestChain(r);
    	EntrapmentCell barriers = null;
    	int nb = 0;
    	// create barrier calls between each linked pair of cells.
    	for(EntrapmentCell c = allCells; c!=null;  c=c.next)
    	{	
    		for(int dir = CELL_FULL_TURN-1; dir>=0; dir--)
    		{	int inv = (dir+CELL_HALF_TURN)%CELL_FULL_TURN;	// inverse direction
    			EntrapmentCell adj = c.exitTo(dir);
    			EntrapmentCell bar = c.barriers[dir];
    			if((adj!=null) && (bar==null))
    			{	bar = new EntrapmentCell(r,cell.Geometry.Line,((dir&1)==0) ? EntrapmentId.VBarriers : EntrapmentId.HBarriers);
    				// label the barrier row and column with the lower left regular cell.
    				bar.col = (char)Math.min(c.col,adj.col);
    				bar.row = Math.min(c.row,adj.row);
    				bar.onBoard = false;
    				bar.addLink(1,adj);
    				bar.addLink(0,c);
    				c.barriers[dir] = bar;
    				adj.barriers[inv] = bar;
    				bar.next = barriers;
    				barriers = bar;
    				nb++;
    			}
    		}
    	}
    	allBarriers = new EntrapmentCell[nb];
    	while(nb-- > 0)
    	{	
    		allBarriers[nb] = barriers;
    		barriers = barriers.next;
    	}
    }

    
    // standared init for Hex.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard(String game)
    { 	roamers_per_player = (Entrapment7x4_INIT.equalsIgnoreCase(game) || Entrapment6x4_INIT.equalsIgnoreCase(game))
    								? STARTING_ROAMERS+1
    								: STARTING_ROAMERS;
    		
    		if(Entrapment7_INIT.equalsIgnoreCase(game)
    		|| Entrapment7x4_INIT.equalsIgnoreCase(game)
    		|| Entrapment_INIT.equalsIgnoreCase(game))
    		{ boardColumns=DEFAULT_COLUMNS; 
    		  boardRows = DEFAULT_ROWS;
    		  is6x7 = false;
    		}
    		else if((Entrapment6_INIT.equalsIgnoreCase(game) 
    				|| (Entrapment6x4_INIT.equalsIgnoreCase(game))))
    		{
    		  boardColumns=DEFAULT_COLUMNS; 
       		  boardRows = DEFAULT_ROWS-1;
       		  is6x7 = true;
    		}
    	else {throw G.Error(WrongInitError,game); }
        gametype = game;
        setState(EntrapmentState.PUZZLE_STATE);
        initBoard(boardColumns,boardRows); //this sets up the board and cross links
        // fill the board with the background tiles
        //for(EntrapmentCell c = allCells; c!=null; c=c.next)
        //{  int i = (c.row+c.col)%2;
        //   c.addChip(EntrapmentChip.getTile(i));
        //}
        
        whoseTurn = FIRST_PLAYER_INDEX;

    }
	public void sameboard(BoardProtocol f) { sameboard((EntrapmentBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(EntrapmentBoard from_b)
    {
    	super.sameboard(from_b);
        G.Assert(AR.sameArrayContents(nTrapped,from_b.nTrapped),"nTrapped mismatch");
        G.Assert(AR.sameArrayContents(flippedBarriers,from_b.flippedBarriers),"flippedBarriers mismatch");
        //G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
        //G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        for (int pl = 0; pl < win.length; pl++)
        {
            G.Assert(unplacedRoamers[pl].height()==from_b.unplacedRoamers[pl].height(),"same unplaced roamers");
            G.Assert(deadRoamers[pl].height()==from_b.deadRoamers[pl].height(),"same dead roamers");
            G.Assert(unplacedBarriers[pl].height()==from_b.unplacedBarriers[pl].height(), "same unplaced barriers");
            for(int i=0;i<roamers_per_player;i++) 
            	{ EntrapmentCell.sameCellLocation(roamerCells[pl][i],from_b.roamerCells[pl][i]); }
        }
        G.Assert(initialSetupPhase == from_b.initialSetupPhase,"inInitalSetupPhase matches");
        G.Assert(pickedObject==from_b.pickedObject, "pickedObject mismatch");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        checkDigestSame(from_b);
    }

    /** 
     * Digest produces a 32 bit hash of the game state.  This is used 3 different
     * ways in the system.
     * (1) This is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game, and a midpoint state of the game. Other site machinery
     *  looks for duplicate digests.  
     * (2) Digests are also used as the game is played to look for draw by repetition.  The state
     * after most moves is recorded in a hashtable, and duplicates/triplicates are noted.
     * (3) Digests are used by the search machinery as a check on the robot's winding/unwinding
     * of the board position, this is mainly a debug/development function, but a very useful one.
     * @return
     */
   public long Digest()
    {
        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        Random r = new Random(64 * 1000); // init the random number generator
        long v = super.Digest(r);
        v ^= Digest(r,allBarriers);
        if(!robotBoard)
       	{
        	// don't include for the bot, it uses them slightly differently
        	v ^= Digest(r,pickedSourceStack);
        	v ^= Digest(r,droppedDestStack);
        }
		for(int pl=0; pl<2; pl++)
		{	
			v ^= unplacedRoamers[pl].height()*r.nextLong();
			v ^= unplacedBarriers[pl].height()*r.nextLong();
			v ^= deadRoamers[pl].height()*r.nextLong();
			v ^= flippedBarriers[pl]*r.nextLong();
			v ^= nTrapped[pl]*r.nextLong();
		}
		v ^= (initialSetupPhase ? 1 : 2) * r.nextLong();
		v ^= (whoseTurn+board_state.ordinal()*10)*r.nextLong();
		v ^= Digest(r,revision);

        return (v);
    }
   public EntrapmentBoard cloneBoard() 
	{ EntrapmentBoard copy = new EntrapmentBoard(gametype,randomKey,getColorMap(),revision);
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((EntrapmentBoard)b); }
   public EntrapmentCell getCell(EntrapmentCell c)
   {	if(c==null){ return(null); }
   		EntrapmentCell n = getCell(c.rackLocation(),c.col,c.row);
   		G.Assert(n!=null,"Missing cell for %s",c);
   		return(n);
   }
    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(EntrapmentBoard from_b)
    {	super.copyFrom(from_b);
    	revision = from_b.revision;  // not redundant, keep this
    	randomKey = from_b.randomKey;
        robotDepth = from_b.robotDepth;
        pickedObject = from_b.pickedObject;
    	pickPart1 = getCell(from_b.pickPart1);
        droppedPart1 = getCell(from_b.droppedPart1);
        initialSetupPhase = from_b.initialSetupPhase;
        getCell(deadCells,from_b.deadCells);
        AR.copy(flippedBarriers,from_b.flippedBarriers);
        AR.copy(nTrapped,from_b.nTrapped);
        copyFrom(unplacedBarriers,from_b.unplacedBarriers);
        copyFrom(unplacedRoamers,from_b.unplacedRoamers);
        copyFrom(deadRoamers,from_b.deadRoamers);
        getCell(roamerCells,from_b.roamerCells);
        copyFrom(allBarriers,from_b.allBarriers);
		getCell(droppedDestStack,from_b.droppedDestStack);
		getCell(pickedSourceStack,from_b.pickedSourceStack);
		copyFrom(allBarriers,from_b.allBarriers);
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        robotBoard = from_b.robotBoard;
        if(G.debug()) { sameboard(from_b); }
	}
    public void doInit(String game, long rv) {
     	doInit(game,randomKey,revision);
    }
 	/* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int rev)
    {	randomKey = key;	// not used, but for reference in this demo game

    	prevLastPicked = -1;
    	prevLastDropped = -1;
    	dropState = 0;
    	
    	adjustRevision(rev);
    	AR.setValue(nTrapped, 0);
    	AR.setValue(flippedBarriers,0);
    	robotDepth = 0;
    	drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
		Grid_Style = ENTRAPMENTGRIDSTYLE; //coordinates left and bottom
		Init_Standard(gtype);
    	sweepCounter = 0;
    	animationStack.clear();
    	roamerCells = new EntrapmentCell[2][roamers_per_player];
    	int map[]=getColorMap();
     	for(int pl=FIRST_PLAYER_INDEX;pl<=SECOND_PLAYER_INDEX; pl++)
    	{
    	EntrapmentCell cell = unplacedBarriers[pl]=new EntrapmentCell(RackLocation[map[pl]]);
      	for(int i=0;i<STARTING_BARRIERS;i++) 
      		{ cell.addChip(EntrapmentChip.getVBarrier(pl,false));
      		}
      	 
    	cell = deadRoamers[pl] = new EntrapmentCell(DeadRoamerLocation[map[pl]]);
    	cell = unplacedRoamers[pl] = new EntrapmentCell(RoamerLocation[map[pl]]);
    	for(int i=0;i<roamers_per_player;i++) { cell.addChip(EntrapmentChip.getChip(map[pl])); }
    	
    	win[pl] = false;
     	}    
 
       // now link the unplacedBarriers
        pickedSourceStack.clear();
        droppedDestStack.clear();
        stateStack.clear();
        stateIStack.clear();
        initialSetupPhase = true;
        pickedObject = null;
        droppedPart1 = pickPart1 = null;
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
         case DRAW_STATE:
            return (true);

        default:
            return (false);
        }
    }

    public Hashtable<EntrapmentCell,EntrapmentCell> getSources()
    {	Hashtable<EntrapmentCell,EntrapmentCell> h = new Hashtable<EntrapmentCell,EntrapmentCell>();;
    	if(pickedObject==null)
    	{
    		switch(board_state)
    		{
    		default: break;
    		case ROAMER_ESCAPE_1_STATE:
    		case ESCAPE_OR_PLACE_STATE:
    			{
    			CommonMoveStack all = new CommonMoveStack();
    			addRoamerEscapeMoves(all,1,1,whoseTurn);
     			while(all.top()!=null)
    			{	EntrapmentMovespec m = (EntrapmentMovespec)all.pop();
    				EntrapmentCell c = getCell(m.source);
    				h.put(c,c);
    			}
    			}
    		}
    	}
    	return(h.size()>0?h:null);
    }
    public CellStack getDead()
    {	return(deadCells);
    }
    //
    // this is used by the robot to prepare the board, specifically
    // to normalize any state that the robot doesn't maintin the
    // same as the human UI.
    //
    public void clearDead()
    {	while(deadCells.size()>0)
    	{	EntrapmentCell c = deadCells.pop();
    		c.deadChip = null;
    	}
    	acceptPlacement();	// make sure the stacks start empty
    }
    // restore the "currently dead" table.
    public void restoreDead(int code)
    {	deadCells.clear();
    	while(code!=0)
    	{	int lc = code & 0xff;
    		code = code>>8;
    		char col = (char)((lc>>4)+'A'-1);
    		int row = ((lc&0xf)>>1)+1;
		    int pl = lc&1;
		    EntrapmentChip ch =deadRoamers[pl].topChip();
		    EntrapmentCell c = getCell(col,row);
		    c.deadChip = ch;
		    deadCells.push(c);
    	}
    }
    // forget the accumulated dead, and return an undo code
    public int removeDead()
    {	int undo=0;
    	while(deadCells.size()>0)
    	{
    		EntrapmentCell c = deadCells.pop();
    		EntrapmentChip ch = c.deadChip;
    		c.deadChip = null;
    		undo = (undo<<8) + ((c.col-'A'+1)<<4)+((c.row-1)<<1)+(playerIndex(ch));
    	}
    	return(undo);
    }
    
    public Hashtable<EntrapmentCell,EntrapmentCell> getDests()
    {	Hashtable<EntrapmentCell,EntrapmentCell> h = null;
    	if(pickedObject!=null)
    	{
    		switch(board_state)
    		{
    		default: break;
    		case ROAMER_ESCAPE_1_STATE:
    		case ESCAPE_OR_PLACE_STATE:
    		case ESCAPE_OR_MOVE_STATE:
    		case MOVE_ROAMER_STATE:
    		case MOVE_OR_MOVE_STATE:
    		case MOVE_OR_PLACE_STATE:
    			{
    			CommonMoveStack all = new CommonMoveStack();
    			if(pickedObject.isRoamer()) 
    				{   h = new Hashtable<EntrapmentCell,EntrapmentCell>();
    					addMoveRoamerMoves(all,1,1,pickedSourceStack.top(),whoseTurn);
    				
	     			while(all.top()!=null)
	    			{	EntrapmentMovespec m = (EntrapmentMovespec)all.pop();
	    				EntrapmentCell c = getCell(m.dest);
	    				h.put(c,c);
	    			}
    				}
    			}
    		}
    	}
    	return(h);
    }
    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(EntrapmentState.GAMEOVER_STATE);
    }
    
    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	return(!initialSetupPhase && (deadRoamers[nextPlayer[player]].height()==roamers_per_player));
    }

    int sweepCounter = 0;
    int escapeSize(int who,EntrapmentCell from, int depth)
    {	int cells = 1;
    		from.sweepCounter = sweepCounter;
    		if(depth>0)
    		{
    		for(int direction=CELL_FULL_TURN-1; direction>=0; direction--)
    		{	EntrapmentCell bar = from.barriers[direction];
    			EntrapmentChip barTop = bar!=null ? bar.topChip() : null;
    			if(barTop==null || ((playerIndex(barTop)==who)&& !barTop.isUp()))
    				{ EntrapmentCell adj = from.fastExitTo(direction);
    				  if(adj!=null && sweepCounter!=adj.sweepCounter)
    				  { if(adj.topChip()==null)
    					  {//if(barTop!=null) { cells -= 0.25; }	// downgrade if using a one time bridge
    				       cells += escapeSize(who,adj,depth-1); 
    				      } 
    				}}
    		}
    	}
    	return(cells);
    }
    

    //
    // return true if balls[unplacedBarriers][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement()
    {	
        pickedObject = null;
        stateStack.clear();
        stateIStack.clear();
        droppedDestStack.clear();
        pickedSourceStack.clear();
     }
    

    //
    // undo the drop, restore the moving object to moving status.
    //
    private EntrapmentCell unDropObject(replayMode replay)
    {
    EntrapmentCell dr = droppedDestStack.top();
    if(dr!=null)
    	{
    	// restore dead if appropriate
    	dr.lastDropped = prevLastDropped;
    	prevLastDropped = -1;
    	dropState--;
    	
    	unCheckDead();
    	droppedDestStack.pop();
    	pickedObject = removeChip(dr);
    	droppedPart1 = null;
		restoreDead(stateIStack.pop());
    	setState(stateStack.pop());
    	checkTrapped(false,replay);
     	}
    	return(dr);
    }
    private void checkTrapped(boolean kill,replayMode replay)
    {	boolean killed = false;
    	for(int pl=FIRST_PLAYER_INDEX; pl<=SECOND_PLAYER_INDEX; pl++)
    	{	EntrapmentCell row[] = roamerCells[pl];
    		int prevTrapped = nTrapped[pl];
    		for(int idx = 0,lim=row.length; idx<lim; idx++)
    		{
    			EntrapmentCell c = row[idx];
    			if(c!=null)
    				{boolean isTrapped = isTrapped(c);
	    			if(isTrapped!=c.trapped) 
	    			{
	    				c.trapped = isTrapped;
	    				if(c.trapped) 
	    					{ nTrapped[pl]++;
	    						// double trap is a kill
	    					  if(kill && prevTrapped>0) { removeDead(c,replay); killed = true;}
	    					} 
	    					else
	    					{ nTrapped[pl]--; }
	    			}
    				}
    		}
    	}
    	if(killed) { checkTrapped(false,replay); }
    }
    private void removeDead(EntrapmentCell ps,replayMode replay)
    {	
    	//Plog.log.addLog("dead "+ps);

    	EntrapmentChip ch = ps.deadChip = removeChip(ps); 
	    deadCells.push(ps);
	    deadRoamers[playerIndex(ch)].addChip(ch);
	    if(replay.animate)
	    {
	    	animationStack.push(ps);
	    	animationStack.push(deadRoamers[playerIndex(ch)]);
	    }
	    if(ps.trapped) { nTrapped[playerIndex(ch)]--; ps.trapped=false; }
    }
    private void doDead(EntrapmentCell ps,replayMode replay)
    { 
   	  boolean isDead = isDead(ps);
   	  // newly placed cells get the first opportunity to be dead.
   	  if(isDead) 
   	  { removeDead(ps,replay);
   	     
   	  }
    }
    private void undoDead(EntrapmentCell ps)
    {   
    	EntrapmentChip ch = ps.deadChip;
    	if(ch!=null)
    	{
     		ps.deadChip = null;
    		deadRoamers[playerIndex(ch)].removeTop();
    		addChip(ps,ch);
    		//Plog.log.addLog("undead "+ps);
    	}
    }
    private void checkDead(EntrapmentCell first,replayMode replay)
    {	if(first!=null)
    		{ if(first.onBoard)
    			{	
    			doDead(first,replay); 
    			}
    		else if((revision>=102) && first.isBarrier())
    			{
    			// first is a barrier. Give first death priorirty to adjacent cells.
    			doDead(first.exitTo(0),replay);
    			doDead(first.exitTo(1),replay);
    			}
    		
    		}
    	for(int pl=FIRST_PLAYER_INDEX; pl<=SECOND_PLAYER_INDEX; pl++)
    	{	EntrapmentCell row[] = roamerCells[pl];
    		for(int idx = 0,lim=row.length; idx<lim; idx++)
    		{
    			EntrapmentCell c = row[idx];
    			if((c!=null) && (c!=first))
    			{ doDead(c,replay);
    			}
    		}
    	}
    }
    private void unCheckDead()
    {	while(deadCells.size()>0)
    	{	EntrapmentCell c = deadCells.pop();
    		undoDead(c);
    	}
    }
    private void addChip(EntrapmentCell ps,EntrapmentChip po)
    {  	
    	ps.addChip(po);
    	ps.deadChip = null;
    	ps.trapped = false;
    	deadCells.remove(ps);
		switch(ps.rackLocation())
		{
		case BoardLocation:
			{
	    	EntrapmentCell r[] = roamerCells[playerIndex(po)];
	    	for(int i=0,lim=r.length;i<lim;i++) 
	    		{ if(r[i]==null) 
	    			{ r[i]=ps; 
	    			return; 
	    			}
	    	}
	    	throw G.Error("No place for new roamer");
			}
		case HBarriers:
		case VBarriers:
			break;
		default:
			break;
		}
    }
    private EntrapmentChip removeChip(EntrapmentCell ps)
    {  	
    	EntrapmentChip po = ps.removeTop();
    	switch(ps.rackLocation())
    	{
    	case BoardLocation:
			{
			G.Assert(po.isRoamer(),"unplacing a roamer");
			EntrapmentCell r[] = roamerCells[playerIndex(po)];
			if(ps.trapped) { ps.trapped=false; nTrapped[playerIndex(po)]--; }
	    	for(int i=0,lim=r.length;i<lim;i++) 
	    		{ if(r[i]==ps)
	    			{
	    			r[i]=null;
	    			return(po); }
	    			}
	    	throw G.Error("Old place for roamer not found");
			}
    	case HBarriers:
    	case VBarriers:
    		break;
    	default: ;
		}
		return(po);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private EntrapmentCell unPickObject(replayMode replay)
    {	EntrapmentChip po = pickedObject;
    	if(po!=null)
    	{
    	EntrapmentCell ps = pickedSourceStack.pop();
    	ps.lastPicked = prevLastPicked;
    	prevLastPicked = -1;
    	
    	pickedObject = null;
    	pickPart1 = null;
    	addChip(ps,switchDroppedObject(ps,po));
    	if(ps.deadChip!=null) { ps.deadChip=null; deadCells.remove(ps); }
    	checkTrapped(false,replay);
    	return(ps);
    	}
    	return(null);
    }
    // 
    // drop the floating object.
    //
    public EntrapmentChip switchDroppedObject(EntrapmentCell dest,EntrapmentChip c)
    {	if(c.isBarrier())
    	{
    	switch(dest.rackLocation())
    		{
    		case VBarriers:
    			return(c.getVBarrier());
    		case HBarriers:
    		case Black_Barriers:
    		case White_Barriers:
    			return(c.getHBarrier());
		default:
			break;
    		}
    	}
    	return(c);
    }
    public void switchPickedObject(EntrapmentCell dest)
    {	if(pickedObject!=null)
    	{
    	pickedObject = switchDroppedObject(dest,pickedObject);
    	}
    }
    private EntrapmentChip dropObject(EntrapmentCell c,replayMode replay)
    {
       G.Assert((pickedObject!=null),"ready to drop");
       int dead = removeDead();		// remove dead from previous move
       EntrapmentChip dropped = switchDroppedObject(c,pickedObject);
       prevLastDropped = c.lastDropped;
       c.lastDropped = dropState;
       dropState++;
       addChip(c,dropped);
       checkDead(c,replay);
       checkTrapped(true,replay);
       droppedDestStack.push(c);
       stateIStack.push(dead);
       stateStack.push(board_state);
       if(droppedPart1==null) { droppedPart1 = c; }
       pickedObject = null;
       return(dropped);
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(EntrapmentCell cell)
    {	return(droppedDestStack.top()==cell);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	EntrapmentChip ch = pickedObject;
    	if(ch!=null)
    		{ return(ch.chipNumber());
    		}
        return (NothingMoving);
    }
    
    public EntrapmentCell getCell(EntrapmentId source) { return getCell(source,'@',-1); }
    
    public EntrapmentCell getCell(EntrapmentId source,char col,int row)
    {	EntrapmentCell c = null;
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	c = getCell(col,row);
          	break;
        case HBarriers:
        	c = getHBarrier(col,row);
        	break;
        case VBarriers:
        	c = getVBarrier(col,row);
        	break;
        case White_Barriers:	// stack of white unplacedBarriers
       		c = unplacedBarriers[getColorMap()[White_Chip_Index]];
        	break;
        case Black_Barriers:	// stack of black unplacedBarriers
       		c = unplacedBarriers[getColorMap()[Black_Chip_Index]];
        	break;
        case White_Roamers:		// stack of white roamiers
         	c = unplacedRoamers[getColorMap()[White_Chip_Index]];
        	break;
        case Black_Roamers:		// stack of black unplacedRoamers
    		c = unplacedRoamers[getColorMap()[Black_Chip_Index]];
        	break;
        case DeadWhiteRoamers:
        	c = deadRoamers[getColorMap()[White_Chip_Index]];
        	break;
        case DeadBlackRoamers:
        	c = deadRoamers[getColorMap()[Black_Chip_Index]];
        	break;
        }
        return(c);
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(EntrapmentCell c,replayMode replay)
    {	G.Assert((pickedObject==null),"ready to pick");
        pickedSourceStack.push(c);
        prevLastPicked = c.lastPicked;
        c.lastPicked = dropState;
        pickedObject = removeChip(c);
        checkTrapped(true,replay);
        if(pickPart1==null) 
        	{ pickPart1 = c; 
        	  droppedPart1 = null;
        	}
   }
    

    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop()
    {	    	
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);

        case MOVE_OR_PLACE_STATE:
        case ESCAPE_OR_PLACE_STATE:
        case MOVE_OR_MOVE_STATE:
        case PLACE_ROAMER_STATE:
        case ESCAPE_OR_MOVE_STATE:
        case SELECT_KILL_SELF2_STATE:
        case SELECT_KILL_OTHER2_STATE:
        	if(roamerIsKilled(whoseTurn)) 
        	{
        		setState(EntrapmentState.SELECT_KILL_SELF2_STATE);
        	}
        	else if(roamerIsKilled(nextPlayer[whoseTurn])) 
        	{
        		setState(EntrapmentState.SELECT_KILL_OTHER2_STATE);
        	}
        	else setState(EntrapmentState.CONFIRM_STATE);
			break;
        case SELECT_KILL_OTHER1_STATE:
        case SELECT_KILL_SELF1_STATE:
        case MOVE_ROAMER_STATE:
        case ROAMER_ESCAPE_1_STATE:
        	{
        	boolean hasBarriers = unplacedBarriers[whoseTurn].height()>0;
        	
           	{
           	boolean win1 = WinForPlayerNow(whoseTurn);
           	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
        	if(win1 || win2)  { setState(EntrapmentState.CONFIRM_STATE); }
        	else {
        	if(roamerIsKilled(whoseTurn)) 
        	{
        		setState(EntrapmentState.SELECT_KILL_SELF1_STATE);
        	}
        	else if(roamerIsKilled(nextPlayer[whoseTurn])) 
        	{
        		setState(EntrapmentState.SELECT_KILL_OTHER1_STATE);
        	}
        	else if(roamerMustEscape(whoseTurn)) 
        		{ setState(hasBarriers?EntrapmentState.ESCAPE_OR_PLACE_STATE:EntrapmentState.ESCAPE_OR_MOVE_STATE); 
        		}
        	else { setState(hasBarriers ? EntrapmentState.MOVE_OR_PLACE_STATE :EntrapmentState.MOVE_OR_MOVE_STATE); }
        	}}}
        	break;
        case PUZZLE_STATE:
			acceptPlacement();
            break;
        }
    }
    //	
    //true if col,row is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(EntrapmentCell c)
    {	return(c==pickedSourceStack.top());
    }

    private void setNextStateAfterDone()
    {	pickPart1 = null;
    	droppedPart1 = null;
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case GAMEOVER_STATE: 
    		break;

        case DRAW_STATE:
        	setGameOver(false,false);
        	break;
    	case CONFIRM_STATE:
    		{
    		boolean escape = roamerMustEscape(whoseTurn);
    		if(initialSetupPhase)
    		{	if(unplacedRoamers[whoseTurn].height()==0) 
    				{ // start regular play
    				  initialSetupPhase = false;
    				  if(escape) { setState(EntrapmentState.ESCAPE_OR_PLACE_STATE); }
    				  else { setState(EntrapmentState.MOVE_OR_PLACE_STATE); }
    				}
    		else
    			{ // continue setup
    			setState(EntrapmentState.PLACE_ROAMER_STATE);
    			}
    		}
    		else
    			{	// in regular play, check for endgame or escape
    			setState(escape ? EntrapmentState.ROAMER_ESCAPE_1_STATE:EntrapmentState.MOVE_ROAMER_STATE);
    			}
    		}
    		break;
    	case PUZZLE_STATE:
    		break;
    	}

    }
   
    void doDone(EntrapmentMovespec m,replayMode replay)
    {	
        acceptPlacement();
        dropState++;
        if (board_state==EntrapmentState.RESIGN_STATE)
        {	setGameOver(false,true);
        }
        else
        {	removeDead();
        	boolean win1 = WinForPlayerNow(whoseTurn);
        	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
        	if(win1 || win2)  { setGameOver(win1,win2); }
        	else
        	{
         	 setNextPlayer();
        	 setNextStateAfterDone(); 
        	}
        }
    }
    private void flipBarriers(EntrapmentCell c,EntrapmentCell d,int dif)
    {	if(c.onBoard && d.onBoard)
    	{
    	int dir = findDirection(c.col,c.row,d.col,d.row);
    	while(c!=d)
    	{	EntrapmentCell bar = c.barriers[dir];
    		int color = bar.flipBarrier();
    		if(color>=0) { flippedBarriers[getColorMap()[color]] += dif; }
    		c=c.exitTo(dir);
    	}}
    }
    EntrapmentMovespec currentMove = null;
    public boolean Execute(commonMove mm,replayMode replay)
    {	EntrapmentMovespec m = (EntrapmentMovespec)mm;
    	currentMove = m;
    	//Plog.log.addLog("execute "+mm);
        //G.print("E "+m+" for "+pickPart1);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone(m,replay);

            break;
        
        case MOVE_ADD:
        case MOVE_RACK_BOARD:
        case MOVE_BOARD_BOARD:
     		{
        	G.Assert((pickedObject==null),"something is already moving");
        	EntrapmentCell src = getCell(m.source);
        	if(m.op==MOVE_ADD) {  src.addChip(EntrapmentChip.getBarrier(whoseTurn)); }
        	pickObject(src,replay);
        	EntrapmentCell dst = getCell(m.dest);
           	if(src.rackLocation==EntrapmentId.BoardLocation) { flipBarriers(src,dst,1); }
        	m.chip = dropObject(dst,replay);
        	
        	if(replay.animate)
        	{
        		animationStack.push(src);
        		animationStack.push(dst);
        	}
        	stateIStack.top();	// support the robot
        	stateStack.top();
            setNextStateAfterDrop();
     		}
        	break;
        case MOVE_DROPB:
        case MOVE_DROP:
        	{
        	EntrapmentCell dest = getCell(m.dest);
            if(isSource(dest)) { unPickObject(replay); }
            else { 
            	if(board_state!=EntrapmentState.PUZZLE_STATE)
            		{
            		EntrapmentCell src = pickedSourceStack.top();
            		if(src.rackLocation==EntrapmentId.BoardLocation) { flipBarriers(src,dest,1); }
            		}
            		else 
            		{
            		pickPart1 = null; 
            		droppedPart1 = null;
            		}
            	EntrapmentChip dropped = dropObject(dest,replay);
            	if(!dropped.isRoamer()) { m.chip = dropped; }
                if(replay==replayMode.Single)
                	{ EntrapmentCell src = pickedSourceStack.top();
                	  animationStack.push(src);
                	  animationStack.push(dest); 
                	}
            	setNextStateAfterDrop(); }
        	}
            break;
        case MOVE_REMOVE:
        	// is not a legal move, but the robot makes them as part of his search
        	// for a valid move-from-to barrier move.
        	{
        	EntrapmentCell src = getCell(m.source);
        	pickObject(src,replay);			// vaporize a barrier
        	m.chip = pickedObject;
        	pickedObject = null;
        	acceptPlacement();
        	setState(EntrapmentState.CONFIRM_STATE);
        	}
        	break;
        case MOVE_PICKB:
        case MOVE_PICK:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	{
        	EntrapmentCell src = getCell(m.source);
        	if((board_state==EntrapmentState.CONFIRM_STATE) && isDest(src))
        		{ 
        			EntrapmentCell src2 = pickedSourceStack.top();
        			flipBarriers(src2,src,-1);
        			unDropObject(replay); 
        		}
        	else 
        		{ 
                pickObject(src,replay);
                if(pickedObject.isRoamer()) { m.chip = pickedObject; }
         		}
        	}
            break;


        case MOVE_START:
            {
            unPickObject(replay);
            acceptPlacement();
            initialSetupPhase = inInitalSetup();
            setWhoseTurn(m.player);
            setState(EntrapmentState.CONFIRM_STATE);
            setNextStateAfterDone();
            }
            break;

        case MOVE_RESIGN:
            setState(unresign==null?EntrapmentState.RESIGN_STATE:unresign);
            break;
       case MOVE_EDIT:
    		acceptPlacement();
            // standardize "gameover" is not true
            setState(EntrapmentState.PUZZLE_STATE);
 
            break;
		case MOVE_LOSEGAMEONTIME:
			setGameOver(false,true);
			break;
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;

        default:
        	cantExecute(m);
        }


        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitBarriers(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
         case CONFIRM_STATE:
         case MOVE_ROAMER_STATE: 
         case DRAW_STATE:
         case REMOVE_BARRIER_STATE:
         case MOVE_OR_MOVE_STATE:
         case ROAMER_ESCAPE_1_STATE:
         case ESCAPE_OR_MOVE_STATE:
         case PLACE_ROAMER_STATE:
         case SELECT_KILL_SELF2_STATE:
         case SELECT_KILL_OTHER2_STATE:
         case SELECT_KILL_SELF1_STATE:
         case SELECT_KILL_OTHER1_STATE:
         case RESIGN_STATE:
        	 return(false);
    
         case MOVE_OR_PLACE_STATE:
         case ESCAPE_OR_PLACE_STATE:
        	       
        	return((pickedObject==null)
        			?(player==whoseTurn)
        			:((pickedObject.isBarrier())&&(player==whoseTurn)));

		case GAMEOVER_STATE:
			return(false);
        case PUZZLE_STATE:
        	return((pickedObject==null)?true:(player==playerIndex(pickedObject)));
        }
    }
  
    // legal to hit the chip storage area
    public boolean LegalToHitRoamers(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
         case CONFIRM_STATE:
        	 return((pickedObject==null)&&isDest(deadRoamers[player]));
         case ESCAPE_OR_MOVE_STATE:
         case MOVE_ROAMER_STATE: 
         case DRAW_STATE:
         case MOVE_OR_MOVE_STATE:
         case REMOVE_BARRIER_STATE:
         case ROAMER_ESCAPE_1_STATE:
         case ESCAPE_OR_PLACE_STATE:
         case RESIGN_STATE:
        	 return(false);
         case SELECT_KILL_SELF2_STATE:
         case SELECT_KILL_SELF1_STATE:
        	 return((pickedObject!=null)&&(player==whoseTurn));

         case SELECT_KILL_OTHER2_STATE:
         case SELECT_KILL_OTHER1_STATE:
        	 return((pickedObject!=null)&&(player!=whoseTurn));
        	 
         case MOVE_OR_PLACE_STATE:
         	 return(false);
         	 
   
         case PLACE_ROAMER_STATE:
         	return((pickedObject==null)
        			?(player==whoseTurn)
        			:(player==whoseTurn));

		case GAMEOVER_STATE:
			return(false);
        case PUZZLE_STATE:
        	return((pickedObject==null)?true:(player==playerIndex(pickedObject)));
        }
    }
  
    private int playerIndex(EntrapmentChip ch) { return(getColorMap()[ch.colorIndex]); }
    
    public boolean LegalToHitBoard(EntrapmentCell cell)
    {	
        switch (board_state)
        {
        case ESCAPE_OR_MOVE_STATE:
		case MOVE_OR_MOVE_STATE:
 			if(pickedObject==null)
 			{	EntrapmentChip ch = cell.topChip();
 				return((ch==null)?false:((playerIndex(ch)==whoseTurn)
 											&& (ch.isRoamer() || (!ch.isUp()))));
 			}
 			else
 			{	return(cell.topChip()==null);
 			}
 			
		case SELECT_KILL_SELF1_STATE:
		case SELECT_KILL_SELF2_STATE:
			if(pickedObject==null)
			{	if(cell.trapped)
				{
				EntrapmentChip ch = cell.topChip();
 				return(playerIndex(ch)==whoseTurn);
				}
				return(false);
			}
			else { 
				return(isSource(cell));
			}

		case SELECT_KILL_OTHER1_STATE:
		case SELECT_KILL_OTHER2_STATE:
			if(pickedObject==null)
			{	if(cell.trapped)
				{
				EntrapmentChip ch = cell.topChip();
 				return(playerIndex(ch)!=whoseTurn);
				}
				return(false);
			}
			else { 
				return(isSource(cell));
			}
		case MOVE_OR_PLACE_STATE:
 		case MOVE_ROAMER_STATE:
 		case REMOVE_BARRIER_STATE:
		case ROAMER_ESCAPE_1_STATE:
		case ESCAPE_OR_PLACE_STATE:
 			if(pickedObject==null)
 			{	EntrapmentChip ch = cell.topChip();
 				return((ch==null)?false:(ch.isRoamer()&&(playerIndex(ch)==whoseTurn)));
 			}
 			else
 			{	return(cell.topChip()==null);
 			}

		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
		case PLACE_ROAMER_STATE:
			return((pickedObject!=null) ? (cell.topChip()==null) : isDest(cell)); 
		case CONFIRM_STATE:
		case DRAW_STATE:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case PUZZLE_STATE:
        	return(pickedObject==null?(cell.chipIndex>=0):(cell.chipIndex<0));
        }
    }

    boolean robotBoard = false;
    EntrapmentPlay robot = null;
    public void initRobotValues(EntrapmentPlay bot)
    {
    	robotBoard = true;
    	robot = bot;
    }
    private StateStack robotState = new StateStack();
    private IStack robotIStack = new IStack();
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(EntrapmentMovespec m)
    {
        // to undo state transitions is to simple put the original state back.
    	//Plog.log.addLog("RE "+m);
        //G.print("R "+m);
    	robotDepth++;
    	robotIStack.push(removeDead());
    	robotState.push(board_state);
    	int dead = 0;
        if (Execute(m,replayMode.Replay))
        {	dead = removeDead();
            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {
                doDone(m,replayMode.Replay);
            }
            else if(m.op==MOVE_BOARD_BOARD) { }
            else if(m.op==MOVE_RACK_BOARD) {}
            else
            {
            	throw G.Error("Robot move should be in a done state");
            }
        }
        robotIStack.push(dead);
        //Plog.log.addLog("RX "+m);
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(EntrapmentMovespec m)
    {	//Plog.log.addLog("UE "+m);
        //G.print("U "+m+" for "+whoseTurn);
    	//Plog.log.addLog("U ",m);
    	robotDepth--;
    	int dead = robotIStack.pop();
        int undo = robotIStack.pop();
        EntrapmentState state = robotState.pop();
        restoreDead(dead);
         
       	switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;

        case MOVE_DONE:
        	setState(state);
        	acceptPlacement();
             break;
        case MOVE_REMOVE:
        	{	// non-canonical move used only by the robot
        		EntrapmentCell dest = getCell(m.source);
        		EntrapmentCell src = unplacedBarriers[m.player];
        		EntrapmentChip po = EntrapmentChip.getBarrier(m.player);
        		src.addChip(po);
        		pickObject(src,replayMode.Replay);
        		dropObject(dest,replayMode.Replay);
         		setState(state);
        	}
        	break;
        case MOVE_RACK_BOARD:
        case MOVE_ADD:
        case MOVE_BOARD_BOARD:
        	{	
        		EntrapmentCell dst = getCell(m.dest);
        		EntrapmentCell src = getCell(m.source);
        		if(src.rackLocation==EntrapmentId.BoardLocation) { flipBarriers(src,dst,-1); }
        		droppedDestStack.push(dst);
        		pickedSourceStack.push(src);
        		stateIStack.push(undo);
        		stateStack.push(state);
        		unDropObject(replayMode.Replay);
        		unPickObject(replayMode.Replay);
        		if(m.op==MOVE_ADD) { src.removeTop(); }
       		    acceptPlacement();
        	}
        	break;
        case MOVE_RESIGN:
        	setState(state);
            break;
        }
        initialSetupPhase = (board_state==EntrapmentState.PLACE_ROAMER_STATE);
        restoreDead(undo>>8);
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
        //Plog.log.addLog("UX "+m);
 }
 void addPlaceRoamerMoves(CommonMoveStack  all,int offset,int skip,int who)
 {	if(unplacedRoamers[who].height()>0)
	 	{	cell<EntrapmentCell> cells[] = getCellArray();
	 		for(int i=offset-1,lim=cells.length; i<lim; i+= skip)
	 		{
	 		EntrapmentCell c = (EntrapmentCell)cells[i];
	 		if((c.topChip()==null) && (c.linkCount==CELL_FULL_TURN))
	 		{
	 		// robot never places on the edges
	 		all.addElement(new EntrapmentMovespec(MOVE_RACK_BOARD,unplacedRoamers[who],c,who));
	 		}}
	 	}
 }
 void addPlaceBarrierMoves(CommonMoveStack  all,int offset,int skip,int who)
 {	if(unplacedBarriers[who].height()>0)
	{	for(int i=offset-1,lim = allBarriers.length; i<lim; i+= skip)
		{
		EntrapmentCell c = allBarriers[i];
		if(c.topChip()==null)
		{
		all.addElement(new EntrapmentMovespec(MOVE_RACK_BOARD,unplacedBarriers[who],c,who));
		}}
	}
}
 void addPlaceNewBarrierMoves(CommonMoveStack  all,int offset,int skip,int who)
 {		for(int i=offset-1,lim=allBarriers.length;i<lim;i+=skip)
		{
	 	EntrapmentCell c = allBarriers[i];
		if(c.topChip()==null)
		{
		all.addElement(new EntrapmentMovespec(MOVE_ADD,unplacedBarriers[who],c,who));
		}}
}
 
 boolean isDead(EntrapmentCell from)
 {
	 return(from.onBoard && isDead(from,false));
 }
 // dead means you have no moves
 boolean isDead(EntrapmentCell from,boolean recurse)
 {	EntrapmentChip top = from.topChip();
 	from.notDeadReason = null;
	if(top!=null)
 	{
 	int pl = playerIndex(top);
 	for(int dir=CELL_FULL_TURN-1; dir>=0; dir--)
 	{	EntrapmentCell dest = from.exitTo(dir);
 		if(dest!=null)
 		{
 		EntrapmentCell barr = from.barriers[dir];
 		EntrapmentChip btop = barr.topChip();
 		EntrapmentChip dtop = dest.topChip();
 		int barriers = 0;
 		do { 
 		if(isImpassableBarrier(pl,btop)) { break; }		// can't move this direction
 		if(isImpassableBarrier(pl,dtop)) { break; }
 		if(isMovableBarrier(pl,btop)) { barriers++;  }
 		
 		if(isMovableBarrier(pl,dtop))		// dest cell is a friendly roamer 
 			{ barriers++; 
 			 if(!recurse && (barriers==1) && !isDead(dest,true)) 
 			   { from.notDeadReason = "Adjacent can move";
 			     return(false); 	// can move first roamer
 			   }
 			}
 			else
 			{ from.notDeadReason = "Adjacent empty"; 
 			  return(false); 	// can move
 			} 

 		if(barriers<2)
 		{
		EntrapmentCell d2 = dest.exitTo(dir);
		if(d2!=null)
		{ 	
  			EntrapmentChip d2top = d2.topChip();
  			if(isImpassableBarrier(pl,d2top)) { break; }			// can't go to space 2
 			EntrapmentChip b2top = dest.barriers[dir].topChip();	
 			if(isImpassableBarrier(pl,b2top)) { break; }			// barrier blocks space 2
			// possible to escape by moving a friendly roamer
 			if(isMovableBarrier(pl,b2top)) { barriers++; }	
 			if(barriers<2)
 			{
 			if(isMovableBarrier(pl,d2top))
 					{
					if(!recurse && !isDead(d2,true)) 
						{ from.notDeadReason = "Adjacent 2 can move";
						  return(false);	// can move second roamer
						}
					}
 			from.notDeadReason = "Adjacent 2 is empty";
 			return(false);		// dest2 is empty
 			}
		}}
 		
 		} while(false);
 		}}
 		return(true);
 	}
	return(false);
 }
 // trapped means you must move if you can
 boolean isTrapped(EntrapmentCell from)
 {	if((from!=null) && from.onBoard && (from.topChip()!=null))
 	{
	 for(int dir=CELL_FULL_TURN-1; dir>=0; dir--)
	 	{	EntrapmentCell dest = from.exitTo(dir);
	 		if((dest!=null) 							// there's a cell
	 				&& (from.barriers[dir].topChip()==null)	// and no barrier
	 				&& (dest.topChip()==null))			// and a place to go
	 		{	return(false); //not trapped
	 		}
	 	}
 	return(true);
 	}
 	return(false);
 }

 boolean roamerMustEscape(int who)
 {	return(nTrapped[who]>0);
 }
 boolean roamerIsKilled(int who)
 {	return(nTrapped[who]>=2);
 }

 boolean isImpassableBarrier(int who,EntrapmentChip ch)
 {
	 if(ch==null) { return(false); }
	 if(playerIndex(ch)!=who) { return(true); }
	 if(ch.isUp()) { return(true); }
	 return(false);
 }
 boolean isMovableBarrier(int who,EntrapmentChip ch)
 {
	 if(ch==null) { return(false); }
	 if(playerIndex(ch)!=who) { return(false); }
	 if(ch.isUp()) { return(false); }
	 return(true);	// either a friendly roamer or a friendly down barrier
 }


 //
 // move a roamer 1 or 2 spaces in any direction, with restrictions due to unplacedBarriers and unfriendly unplacedRoamers.
 //
 void addMoveRoamerMoves(CommonMoveStack  all,int offset,int skip,EntrapmentCell src,int who)
 {	
 	for(int dir = offset-1; dir<CELL_FULL_TURN; dir += skip)
 	{
	 EntrapmentCell dest = src.exitTo(dir);
	 if(dest!=null)
	 {
	 int barriers = 0;
	 EntrapmentChip dtop = dest.topChip();
	 EntrapmentChip btop = src.barriers[dir].topChip();
	 do {
		 if(isImpassableBarrier(who,btop)) { break; }
		 if(isImpassableBarrier(who,dtop)) { break; }
		 if(isMovableBarrier(who,dtop)) { barriers++; }
		 	else  
		 	{// dest top is empty
				 all.addElement(new EntrapmentMovespec(MOVE_BOARD_BOARD,src,dest,who));
		 	}
		 if(isMovableBarrier(who,btop)) { barriers++; }
		 if(barriers<2)
		 {
		 EntrapmentCell d2 = dest.exitTo(dir);
		 if((d2!=null) && (d2.topChip()==null))
		 	{
			 // possible second space
			 EntrapmentChip b2Top = dest.barriers[dir].topChip();
			 if(isImpassableBarrier(who,b2Top)) { break; }
			 if(isMovableBarrier(who,b2Top) && (barriers==1)) { break; }
			 all.addElement(new EntrapmentMovespec(MOVE_BOARD_BOARD,src,d2,who));
		 	}
		 }
		 } while(false); 
	 }
 	}
 }
 
 void addMoveBarrierMoves(CommonMoveStack  all,int offset,int skip,EntrapmentCell src,int who)
 {	
 	for(int i=offset-1,lim=allBarriers.length; i<lim; i+=skip)
 	{	EntrapmentCell dest = allBarriers[i];
 		EntrapmentChip ch = dest.topChip();
 		if((ch==null)&&(dest!=src))
 		{	all.addElement(new EntrapmentMovespec(MOVE_BOARD_BOARD,src,dest,who));
 		}
 	}
 }
 void addRoamerKillMoves(CommonMoveStack  all,int offset,int skip,int who,int whoToKill)
 {	EntrapmentCell r[] = roamerCells[whoToKill];
 	EntrapmentCell dest = deadRoamers[whoToKill];
 	if(offset==1)
 	{
 	for(int i=0,lim=r.length;i<lim;i++)
 	{	EntrapmentCell c = r[i];
 		if((c!=null)&&c.trapped) 
 			{ all.addElement(new EntrapmentMovespec(MOVE_BOARD_BOARD,c,dest,who)); 
 			}
	}}
 }
 void addMoveRoamerMoves(CommonMoveStack  all,int offset,int skip,int who)
 {	EntrapmentCell r[] = roamerCells[who];
 	for(int i=0,lim=r.length;i<lim;i++)
 	{	EntrapmentCell c = r[i];
 		if(c!=null) { addMoveRoamerMoves(all,offset,skip,c,who); }
	}
 }
 void addMoveBarrierMoves(CommonMoveStack  all,int offset,int skip,int who)
 {	for(int i=offset-1,lim=allBarriers.length; i<lim; i += skip)
 	{EntrapmentCell c = allBarriers[i];
 	 EntrapmentChip top = c.topChip();
 	 if((top!=null) && (playerIndex(top)==who) && !top.isUp())
 	 {
	 for(int j=0;j<lim;j++)
	 	{	
		EntrapmentCell d = allBarriers[j];
 		if(d.topChip()==null) { all.addElement(new EntrapmentMovespec(MOVE_BOARD_BOARD,c,d,who)); }
	 	}
 	 }
 	}
 }
 void addRemoveBarrierMoves(CommonMoveStack  all,int offset,int skip,int who)
 {	EntrapmentCell dest = droppedDestStack.top();
 	for(int i=offset-1,lim=allBarriers.length; i<lim;i+=skip)
 	{	EntrapmentCell c = allBarriers[i];
 		if (dest!=c)
 		{EntrapmentChip top = c.topChip();
	 		if((top!=null) && (playerIndex(top)==who) && !top.isUp())
	 		{
	 		 all.addElement(new EntrapmentMovespec(MOVE_REMOVE,c,who));
	 		}
	 	}
 	}
 }
  void addRoamerEscapeMoves(CommonMoveStack  all,int offset,int skip,int who)
 {	EntrapmentCell r[] = roamerCells[who];
 	for(int i=0,lim=r.length;i<lim;i++)
 	{	EntrapmentCell c = r[i];
 		if(c!=null)
 		{
 		boolean escape = c.trapped;
 		c.escapeCell = escape;
 		if(escape) 
 			{ 
 			  addMoveRoamerMoves(all,offset,skip,c,who); 
 			}
 		}
	}
 	// escape can also be accomplished by moving a friendly roamer
 	for(int i=0,lim=r.length;i<lim;i++)
 	{	EntrapmentCell c = r[i];
 		if((c!=null) && !c.escapeCell)
 		{	boolean added = false;
 			for(int dir=CELL_FULL_TURN-1; dir>=0 && !added; dir--)
 			{
 				EntrapmentCell a = c.exitTo(dir);
 				if((a!=null) && (c.barriers[dir].topChip()==null) && a.escapeCell)
 				{	EntrapmentChip top = a.topChip();
 					if((top!=null) && playerIndex(top)==who)
 					{
 						addMoveRoamerMoves(all,offset,skip,c,who);
 						added = true;
 					}
 				}
 			}
 		}
 	}
 }
 
 public CommonMoveStack  getMoveList(CommonMoveStack all,int offset,int skip)
 {	
 	switch(board_state)
 	{
 	default:	throw G.Error("Not expecting state %s",board_state);
 	case SELECT_KILL_OTHER1_STATE:
 	case SELECT_KILL_OTHER2_STATE:
 		addRoamerKillMoves(all,offset,skip,whoseTurn,nextPlayer[whoseTurn]);
 		break;
	case SELECT_KILL_SELF1_STATE:
 	case SELECT_KILL_SELF2_STATE:
 		addRoamerKillMoves(all,offset,skip,whoseTurn,whoseTurn);
 		break;
 	
 	case PLACE_ROAMER_STATE:
 		addPlaceRoamerMoves(all,offset,skip,whoseTurn);
 		break;
 	case MOVE_OR_PLACE_STATE:
 		addPlaceBarrierMoves(all,offset,skip,whoseTurn);
 		addMoveRoamerMoves(all,offset,skip,whoseTurn);
 		break;

 	case MOVE_ROAMER_STATE:
 		addMoveRoamerMoves(all,offset,skip,whoseTurn);
 		break;
 		
 	case ESCAPE_OR_MOVE_STATE:
 		// move the escaping roamer again, or move a barrier
 		addMoveBarrierMoves(all,offset,skip,whoseTurn);
 		addRoamerEscapeMoves(all,offset,skip,whoseTurn);
 		break;
	case ESCAPE_OR_PLACE_STATE:
		//move the escaping roamer again, or place a barrier
		addPlaceBarrierMoves(all,offset,skip,whoseTurn);
		addRoamerEscapeMoves(all,offset,skip,whoseTurn);
		break;
	case ROAMER_ESCAPE_1_STATE:
		addRoamerEscapeMoves(all,offset,skip,whoseTurn);
		break;
	case MOVE_OR_MOVE_STATE:
		addMoveRoamerMoves(all,offset,skip,whoseTurn);
		//addMoveBarrierMoves(all,whoseTurn);
		addPlaceNewBarrierMoves(all,offset,skip,whoseTurn);
		break;
	case REMOVE_BARRIER_STATE:
		addRemoveBarrierMoves(all,offset,skip,whoseTurn);
		break;
 	}
 	return(all);
 }
 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(Character.isDigit(txt.charAt(0)))
	 	{ xpos+=cellsize/8;
	 	}
 		else
 		{ //xpos += cellsize/8; 
 		  ypos += cellsize/4;
 		}
 	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
 }

}
