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
package arimaa;


import online.game.*;

import java.util.*;

import lib.*;
import lib.Random;


/**
 * ArimaaBoard knows all about the game of Arimaa, which is played
 * on a 8x8 board. It gets a lot of logistic support from 
 * squareBoard, which knows about the coordinate system and the lack of diagonal connectivity
 * 
 * @author ddyer
 *
 */

class ArimaaBoard extends squareBoard<ArimaaCell> implements BoardProtocol,ArimaaConstants
{	   static final String[] ARIMAAGRIDSTYLE = { "2", null, "A" }; // left and bottom numbers
   	   static final int STEPS_PER_TURN = 4;
		ArimaaState unresign;
		ArimaaState board_state;
		public ArimaaState getState() {return(board_state); }
		public void setState(ArimaaState st) 
		{ 	unresign = (st==ArimaaState.RESIGN_STATE)?board_state:null;
			board_state = st;
			if(!board_state.GameOver()) 
				{ AR.setValue(win,false); 	// make sure "win" is cleared
				}
		}
		// trap maps are weights for squares based on distance to traps
		// and the mix of players controlling the trap.
		public CellStack animationStack = new CellStack();
		IntObjHashtable<double[][]> trapMaps = null;
		double [][][]currentTrapMap = new double[2][][];
		public int getTrapMapKey(int player,int t1a,int t1b,int t2a,int t2b,int t3a,int t3b,int t4a,int t4b)
		{
			int k = (t1a<<3+t1b);
			k = (((k<<3)+t2a)<<3)+t2b;
			k = (((k<<3)+t3a)<<3)+t3b;
			k = (((k<<3)+t4a)<<3)+t4b;
			k = (k<<1)+player;
			return(k);
		}
		public int getTrapMapKey(int player,int t1a,int t1b)
		{
			int k = (t1a<<3+t1b);
			k = (k<<1)+player;
			return(k);
		}
		public double[][] getTrapMap(int key)
		{
			if(trapMaps==null) { trapMaps=new IntObjHashtable<double[][]>(); }
			return(trapMaps.get(key));
		}
		public double[][] makeTrapMap(int key)
		{	if(trapMaps==null) { trapMaps=new IntObjHashtable<double[][]>(); }
			double [][]map = new double[variation.nCols][variation.nRows];
			trapMaps.put(key,map);
			return(map);
		}

	public RepeatedPositions repeatedPositions = null;	// this is shared structure with the canvas
	public long recentDigest = 0;						// special digest "SubDigest" used to detect "effective pass" moves
	public long nextRecentDigest = 0;					// the Subdigest after all moves (but not yet confirmed by "done"
	public int playerColor[] = new int[2];
	
	public void SetDrawState() 
    	{ setState(ArimaaState.ILLEGAL_MOVE_STATE);
    	}
   
    public ArimaaCell rack[][] =  new ArimaaCell[2][ArimaaChip.N_STANDARD_CHIPS];	// unplaced or captured pieces
    //
    // private variables
    //
    Variation variation=Variation.Arimaa;
 	public boolean started = false;
    public int lastAdvancementMoveNumber = 0;
 	private int captured =  0;
    private int rabbitsOnBoard[] = new int[2];			// number of chips currently on the board
    private int chipsOnBoard[] = new int[2];
    private ArimaaCell trapCells[] = new ArimaaCell[4];
    private StateStack robotState = new StateStack();
    public int robotDepth = 0;
    private boolean robotGame = false;
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public ArimaaChip pickedObject = null;
    public int playStep = 0;
    private ArimaaCell pickedSourceStack[] = new ArimaaCell[14];
    private ArimaaCell droppedDestStack[] = new ArimaaCell[14];
    private ArimaaChip captureStack[] = new ArimaaChip[14];
    private ArimaaCell capturedCellStack[] = new ArimaaCell[14];
    private ArimaaChip captureStack2[] = new ArimaaChip[14];
    private ArimaaCell capturedCellStack2[] = new ArimaaCell[14];
    private ArimaaChip capturedPiece = null;
    private ArimaaCell capturedLocation = null;
    private ArimaaChip capturedPiece2 = null;
    private ArimaaCell capturedLocation2 = null;
    public ArimaaCell handicap[] = new ArimaaCell[2];
    private ArimaaCell pushPullSource = null;
    private ArimaaCell pushPullDest = null;
    private ArimaaMovespec lastRobotMove = null;
    private boolean trapMapValid = false;
    private int trap_sweep = 0;
    
    IStack previousPlaced = new IStack();
    IStack previousEmptied = new IStack();
    IStack previousPlayer = new IStack();
    int placementIndex = 0;

    CellStack robotOStack = new CellStack();
    ChipStack robotPStack = new ChipStack();
    CellStack destStack = new CellStack();

    private int stackIndex = 0;
    private ArimaaCell getDest()
    {  	if(stackIndex>0) { return(droppedDestStack[stackIndex-1]); }
    	return(null);
    }

    
    // get the board positions that have been stepped through in the current move
    public Hashtable<ArimaaCell,ArimaaCell> getSteps()
    {
    	Hashtable<ArimaaCell,ArimaaCell> h = new Hashtable<ArimaaCell,ArimaaCell>();
    	for(int i=0;i<=stackIndex; i++)
    	{
    		ArimaaCell c = droppedDestStack[i];
    		if(c!=null) { h.put(c,c); }
    		c = pickedSourceStack[i];
    		if(c!=null) { h.put(c,c); }
    		
    	}
    	return(h);
    }
	// factory method
	public ArimaaCell newcell(char c,int r)
	{	ArimaaCell art = new ArimaaCell(c,r);
		return(art);
	}
    public ArimaaBoard(String init,long key,RepeatedPositions rep,int[]cmap) // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = ARIMAAGRIDSTYLE; //coordinates left and bottom
       	repeatedPositions = rep;
    	setColorMap(cmap, 2);
        int map [] = getColorMap();
        Random r = new Random(5685343);
        for(int i=0,pl=FIRST_PLAYER_INDEX;i<=SECOND_PLAYER_INDEX; i++,pl=nextPlayer[pl])
    	{	ArimaaCell c = handicap[i] = new ArimaaCell(r);
    		ArimaaId rackid = RackLocation[map[i]];
    		c.rackLocation = rackid==ArimaaId.B ? ArimaaId.BH : ArimaaId.WH;
    		c.col = (char)('A'+i);
    		c.row = i;
	     	for(int j=0;j<ArimaaChip.N_STANDARD_CHIPS;j++)
	     	{
	     		ArimaaCell cc = new ArimaaCell(r);
	     		cc.rackLocation=rackid;
	     		cc.col = '@';
	     		cc.row = j;
	     		rack[i][j] = cc;
	
	     	}}
    	
        doInit(init,key); // do the initialization 
        autoReverseY();		// reverse_y based on the color map
    }


    private void Init_Standard(String game)
    { 	
        gametype = game;
        variation = Variation.findVariation(game);
        setState(ArimaaState.PUZZLE_STATE);
        reInitBoard(variation.nCols,variation.nRows); //this sets up the board and cross links
        robotOStack.clear(); 
        robotPStack.clear();
        destStack.clear();
        robotState.clear();
        robotDepth = 0;
        // fill the board with the background tiles
        CellStack traps = new CellStack();
        for(int []trap : variation.traps)
        {
        	ArimaaCell c = getCell((char)trap[0],trap[1]);
        	traps.push(c);
        	c.isTrap = true;
        	c.isTrapAdjacent = false;
         	c.addChip(ArimaaChip.getTile(2));
             	
        	for(int dir=CELL_FULL_TURN-1; dir>=0; dir--)
        	{
        		ArimaaCell d = c.exitTo(dir);
        		d.isTrapAdjacent = true;
        	}
        }
        for(ArimaaCell c = allCells; c!=null; c=c.next)
        {
        	if(!c.isTrap) 
        		{int i = (c.row+c.col)%2;
        		 c.addChip(ArimaaChip.getTile(i)); 
        		}
        }
        trapCells = traps.toArray();
 
        whoseTurn = FIRST_PLAYER_INDEX;
        started = false;
        captured = 0;
        lastAdvancementMoveNumber = 0;
        playStep = 0;
        for(int i=FIRST_PLAYER_INDEX;i<=SECOND_PLAYER_INDEX; i++)
        {
        rabbitsOnBoard[i] = 0;
        chipsOnBoard[i]=0;
        }
        reInit(handicap);
        AR.setValue(pickedSourceStack,null);
        AR.setValue(capturedCellStack,null);
        AR.setValue(capturedCellStack2,null);
        AR.setValue(captureStack,null);
        AR.setValue(captureStack2,null);
        AR.copy(playerColor,getColorMap());
    }
	public void sameboard(BoardProtocol f) { sameboard((ArimaaBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(ArimaaBoard from_b)
    {	super.sameboard(from_b);
		G.Assert(variation==from_b.variation,"variation mismatch");
    	G.Assert(AR.sameArrayContents(rabbitsOnBoard,from_b.rabbitsOnBoard),"rabbitsOnBoard mismatch");
        G.Assert(AR.sameArrayContents(chipsOnBoard,from_b.chipsOnBoard),"chipsOnBoard mismatch");
        G.Assert(sameContents(handicap,from_b.handicap),"handicap mismatch");
        for (int i = 0; i < win.length; i++)
        {
		   ArimaaCell row[]=rack[i];
		   ArimaaCell frow[]=from_b.rack[i];
		   for(int lim=row.length-1; lim>=0; lim--)
			   {	G.Assert(row[lim].sameCell(frow[lim]),"rack cells match");
			   }
        }

        G.Assert(stackIndex==from_b.stackIndex,"stackIndex matches");
        for(int idx=0; idx<stackIndex;idx++)
        {
        	G.Assert(ArimaaCell.sameCell(pickedSourceStack[idx],from_b.pickedSourceStack[idx]),"same source stack");
        	G.Assert(ArimaaCell.sameCell(droppedDestStack[idx],from_b.droppedDestStack[idx]),"same source stack");
        }
        // do not include deststack.  it contains similar but not identical elements
        // G.Assert(destStack.sameContents(from_b.destStack),"same dest stack");
        G.Assert(started==from_b.started, "game started matches");
        G.Assert(pickedObject==from_b.pickedObject, "pickedObject matches");
        G.Assert(captured==from_b.captured, "same capture count");
        G.Assert(playStep == from_b.playStep,"playStep matches");
        // here, check any other state of the board to see if
        G.Assert(ArimaaCell.sameCell(pushPullSource,from_b.pushPullSource),"same push source");
        G.Assert(ArimaaCell.sameCell(pushPullDest,from_b.pushPullDest),"same pull dest");
        G.Assert(capturedPiece==from_b.capturedPiece,"same captured piece");
        G.Assert(capturedPiece2==from_b.capturedPiece2,"same captured second piece");
        G.Assert(ArimaaCell.sameCell(capturedLocation,from_b.capturedLocation), "same captured location");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");

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
   public long SubDigest(Random r)
    {
        int v = 0;

        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        
		v += (started?1:2)*r.nextLong();
		v += captured^r.nextLong();
	    for (int i = 0; i < win.length; i++)
	        {	v += (win[i]?1:2)*r.nextLong();
	        	v += rabbitsOnBoard[i]*r.nextLong();
	        	v += chipsOnBoard[i]*r.nextLong();
	        	ArimaaCell row[]=rack[i];
	        	for(int lim=row.length-1; lim>=0; lim--)
				   {	v ^= row[lim].Digest(r);
				   }
	        }
		
		for(ArimaaCell c = allCells; c!=null; c=c.next)
		{	v ^= c.Digest(r);
		}

        // for most games, we should also digest whose turn it is
		return(v);
    }
public long SubDigest()
{	Random r = new Random(64 * 1000); // init the random number generator
	return(SubDigest(r));
}
public long Digest()
{   Random r = new Random(64 * 1000); // init the random number generator
	long v = SubDigest(r);
	v += playStep*r.nextLong();
	v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
	v ^= cell.Digest(r,getSource());
    v ^= chip.Digest(r,pickedObject);
    v ^= Digest(r,handicap);
    v ^= (r.nextLong()*(1+variation.ordinal()));
    return (v);
}

   public ArimaaBoard cloneBoard() 
	{ ArimaaBoard copy = new ArimaaBoard(gametype,randomKey,repeatedPositions,getColorMap());
	  copy.copyFrom(this); 
	  return(copy);
	}
   
    public void copyFrom(BoardProtocol b) { copyFrom((ArimaaBoard)b); }
   
    public ArimaaCell getCell(ArimaaCell c)
    {	return(c==null?null:getCell(c.rackLocation(),c.col,c.row));
    }
    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(ArimaaBoard from_b)
    {	super.copyFrom(from_b);
    	variation = from_b.variation;
    	started = from_b.started;
        captured = from_b.captured;
        robotDepth = from_b.robotDepth;
        robotGame = from_b.robotGame;
        robotState.copyFrom(from_b.robotState);
        lastAdvancementMoveNumber = from_b.lastAdvancementMoveNumber;
        repeatedPositions = from_b.repeatedPositions.copy();
        recentDigest = from_b.recentDigest;
        nextRecentDigest = from_b.nextRecentDigest;
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        playStep = from_b.playStep;
        copyFrom(handicap,from_b.handicap);
        robotOStack.copyFrom(from_b.robotOStack);
        robotPStack.copyFrom(from_b.robotPStack);
        destStack.copyFrom(from_b.destStack);
        AR.copy(chipsOnBoard,from_b.chipsOnBoard);
        AR.copy(rabbitsOnBoard,from_b.rabbitsOnBoard);
        copyFrom(rack,from_b.rack);
		pushPullSource = getCell(from_b.pushPullSource);
		pushPullDest = getCell(from_b.pushPullDest);
		capturedPiece = from_b.capturedPiece;
		capturedPiece2 = from_b.capturedPiece2;
		capturedLocation = getCell(from_b.capturedLocation);
		capturedLocation2 = getCell(from_b.capturedLocation2);
		getCell(pickedSourceStack,from_b.pickedSourceStack);
		getCell(droppedDestStack,from_b.droppedDestStack);
        pickedObject = from_b.pickedObject;
        stackIndex = from_b.stackIndex;
        placementIndex = from_b.placementIndex;
        
        sameboard(from_b);
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {	randomKey = key;	// not used, but for reference in this demo game
       	recentDigest = 0;
       	nextRecentDigest = 0;
       	trapMapValid = false;
       	trapMaps = null;
       	pushPullSource = null;
       	pushPullDest = null;
       	animationStack.clear();
       	int map[]=getColorMap();
        Init_Standard(gtype);
       	
    	for(int i=0,pl=FIRST_PLAYER_INDEX;i<=SECOND_PLAYER_INDEX; i++,pl=nextPlayer[pl])
    	{	int nc[] = variation.counts;
	     	for(int j=0;j<nc.length;j++)
	     	{
	     		ArimaaCell cc = rack[i][j];
	     		ArimaaChip ch = ArimaaChip.getChip(map[i],j);
	     		cc.reInit();
	     		cc.rackLocation=RackLocation[map[i]];
	     		for(int h=0;h<nc[j]; h++)
	     			{
	     			cc.addChip(ch);
	     			}
	     	}
     	}    
        moveNumber = 1;
        placementIndex = 1;
        acceptPlacement();
        // note that firstPlayer is NOT initialized here
    }




    //
    // change whose turn it is, increment the current move number
    //
    private void setNextPlayer()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Move not complete, can't change the current player in state %s",board_state);
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
    public boolean DigestState()
    {	if(started) { return(DoneState()); }
    	return(false);
    }


    private void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(ArimaaState.GAMEOVER_STATE);
    }
    private int playerIndex(ArimaaChip ch) { return(playerColor[ch.colorIndex()]); }
    
    private boolean rabbitInHomeRow(int forPlayer)
    {	int trow = (forPlayer==playerColor[FIRST_PLAYER_INDEX])?1:variation.nRows;
    	for(char col='A';col<'A'+variation.nCols; col++)
    	{	ArimaaChip top = getCell(col,trow).topChip();
    		if((top!=null) && (playerIndex(top)==forPlayer)&& top.isRabbit()) { return(true); }
    	}
    	return(false);
    }
    /**
     * The order of checking for win/lose conditions is as follows assuming player A just made the move and player B now needs to move:

   1. Check if a rabbit of player A reached goal. If so player A wins.
   2. Check if a rabbit of player B reached goal. If so player B wins.
   3. Check if player B lost all rabbits. If so player A wins.
   4. Check if player A lost all rabbits. If so player B wins.
   5. Check if player B has no possible move (all pieces are frozen or have no place to move). If so player A wins.
   6. Check if the only moves player B has are 3rd time repetitions. If so player A wins. 
   
     * @param player
     * @return
     */
    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	if(board_state==ArimaaState.GAMEOVER_STATE) { return(win[player]); }
    	if(!started) { return(false); }
    	if(rabbitInHomeRow(player)) { return(true); }
    	if(rabbitsOnBoard[nextPlayer[player]]==0) { return(true); }
    	return(false);
    }
    private boolean checked = false;
    private void checkForNoMoves()
    {
        if(!checked
        	   && (board_state==ArimaaState.PLAY_STATE)
     		   && (pickedObject==null)
     		   && (playStep==0))
        {	checked = true;
        	CommonMoveStack all = new CommonMoveStack();
        	getListOfMoves(all,whoseTurn,null,false);
     	   if(all.size()==0)
     	   { setGameOver(false,true); 
     	   }
        }
    }
     // adjustment on the static value based on the number of rabbits left.  The
    // basic premise is if you loose too many rabbits you're in trouble.
   
    private ArimaaCell opp_horses[] = new ArimaaCell[2];
    private ArimaaCell opp_cats[] = new ArimaaCell[2];
    private ArimaaCell opp_dogs[] = new ArimaaCell[2];
    private ArimaaCell opp_rabbits[] = new ArimaaCell[8];
    @SuppressWarnings("unused")
	private ArimaaCell opp_elephant = null;
    private ArimaaCell opp_camel = null;
    private ArimaaCell my_horses[] = new ArimaaCell[2];
    private ArimaaCell my_cats[] = new ArimaaCell[2];
    private ArimaaCell my_dogs[] = new ArimaaCell[2];
    private ArimaaCell my_rabbits[] = new ArimaaCell[8];
    private ArimaaCell my_elephant = null;
    private ArimaaCell my_camel = null;
    
    private int mDistance(int c1,int r1,int c2, int r2)
    {
    	return(Math.abs(c1-c2)+Math.abs(r1-r2));
    }
   
    private double disScore(ArimaaCell a,ArimaaCell b,double [][]tmap,double baseval)
    {	if((a!=null) && (b!=null))
    	{
    	int acol = a.col-'A';
    	int bcol = b.col-'A';
    	int arow = a.row-1;
    	int brow = b.row-1;
    	int dist =  mDistance(acol,arow,bcol,brow);	// part of value at risk
    	double mval = tmap[bcol][brow];
    	return((baseval*mval)/(dist+1));
    	}
    	return(0.0);
    }


    private double trapset[][][] = {
    		{	// 0 friends
    				// 0 enemies
    				{1,1, 0.5},	// distance 0 to 2    				 
    				// 1 enemy
    				{1,1, 0.5},	// distance 0 to 2
    				// 2 enemies
    				{1,1, 0.75},	// distance 0 to 2
    				// 3 enemies
    				{1,1, 0.8},	// distance 0 to 2
    				// 4 enemies
    				{1,1, 0.9}	// distance 0 to 2
    			
    		},
    		{	// 1 friends
    			// 0 enemies
				{0.5,0.5, 0.25},	// distance 0 to 2    				 
				// 1 enemy
				{0.6,0.5, 0.3},	// distance 0 to 2
				// 2 enemies
				{0.8,0.75, 0.6},	// distance 0 to 2
				// 3 ememies
				{0.85,0.8, 0.7}	// distance 0 to 2
    		},
    		{	// 2 friends
       			// 0 enemies
				{0.1,0.05, 0.0},	// distance 0 to 2    				 
				// 1 enemy
				{0.2,0.1, 0.05},	// distance 0 to 2
				// 2 enemies
				{0.3,0.1, 0.0},	// distance 0 to 2
    		}
    };
    // c is a trap cecll
    // trapmap is an 8x8 array of doubles
    // steps is an short array of doubles which are weights by manhattan distance from c
    //
    private void markTrapsFrom(double[][] trapmap,ArimaaCell c,int sweep,double steps[],int index)
    {	int lim = steps.length-1;
    	int visits = 0;
    	int ndirs =  CELL_FULL_TURN-1;
    	c.sweep_counter = sweep;
    	trapmap[c.col-'A'][c.row-1] += steps[index];
     	for(int dir = ndirs; dir>=0; dir--)
    	{	ArimaaCell nx = c.exitTo(dir);
    		visits = visits<<1;
    		if((nx!=null)&&(index<lim) && ((nx.sweep_counter!=sweep)))
    		{  	nx.sweep_counter = sweep;
    			visits |= 1;
    		}
    	}
    	for(int dir = 0; dir<=ndirs; dir++)
    	{
    		ArimaaCell nx = c.exitTo(dir);
    		if((visits&1)!=0)
    		{
    			markTrapsFrom(trapmap,nx,sweep,steps,index+1);
    		}
    		visits = visits>>1;
    	}
    }
   	
    private void prepareTrapScore(ArimaaChip mostPower,boolean map)	// the opponents most powerful piece
	{	// score for the importance of being adjacent to traps to support your pieces
    	if(trapMapValid) 
    	{ 
    	}
    	else
    	{
 
   		// count the friendly and unfriendly cells for each trap
    	{	
			int maxPower = mostPower==null ? -1 : mostPower.chipPower();
    	   	for(int lim=trapCells.length-1; lim>=0; lim--)
    		{	ArimaaCell trap = trapCells[lim];
    			trap.trap_adj[FIRST_PLAYER_INDEX] = trap.trap_adj[SECOND_PLAYER_INDEX] = 0;
    			for(int dir = 0;dir<CELL_FULL_TURN;dir++)
    			{
    				ArimaaCell ex = trap.exitTo(dir);
    				ArimaaChip top = ex.topChip();
    				if(top!=null)
    					{	trap.trap_adj[playerIndex(top)]++;
    						if(top.chipPower()>=maxPower) { trap.trap_adj[playerIndex(top)]+=100; } 
    					}
    			}
 
    		}
   			if(map)
			{
   			switch(trapCells.length)
   			{
   			default: throw G.Error("Not expected");
   			case 1:
   				{
  	   			ArimaaCell tr0 = trapCells[0];
   				for(int player=FIRST_PLAYER_INDEX; player<=SECOND_PLAYER_INDEX; player++)
  				{	int next = nextPlayer[player];
  					int key = getTrapMapKey(player,tr0.trap_adj[player],tr0.trap_adj[next]);
  					double [][] pmap = getTrapMap(key);
  					if(pmap==null)
  					{
  					pmap = makeTrapMap(key);
  					ArimaaCell trap = trapCells[0];
  					int mytraps = trap.trap_adj[player];
  					int histraps = trap.trap_adj[next];
  					boolean elephant_trap = mytraps>=100;
  					if(!elephant_trap && (mytraps<3))
  					{ markTrapsFrom(pmap,trap,++trap_sweep,trapset[mytraps%100][histraps%100],0); 
  					}
  					}
  					currentTrapMap[player]=pmap;
  				}
   				}
   				break;
   			case 4:
   			{
   			ArimaaCell tr0 = trapCells[0];
   			ArimaaCell tr1 = trapCells[1];
   			ArimaaCell tr2 = trapCells[2];
   			ArimaaCell tr3 = trapCells[3];
			for(int player=FIRST_PLAYER_INDEX; player<=SECOND_PLAYER_INDEX; player++)
			{	int next = nextPlayer[player];
				int key = getTrapMapKey(player,tr0.trap_adj[player],tr0.trap_adj[next],
						tr1.trap_adj[player],tr1.trap_adj[next],
						tr2.trap_adj[player],tr2.trap_adj[next],
						tr3.trap_adj[player],tr3.trap_adj[next]);
				double [][] pmap = getTrapMap(key);
				if(pmap==null)
				{
				pmap = makeTrapMap(key);
				for(int tr=0;tr<4;tr++)
				{	ArimaaCell trap = trapCells[tr];
					int mytraps = trap.trap_adj[player];
					int histraps = trap.trap_adj[next];
					boolean elephant_trap = mytraps>=100;
					if(!elephant_trap && (mytraps<3))
					{ markTrapsFrom(pmap,trap,++trap_sweep,trapset[mytraps%100][histraps%100],0); 
					}
				}
				}
				currentTrapMap[player]=pmap;
			}}}
    	}
    	trapMapValid = true;
    	}}
	}
    private double scoreTraps(int player,ArimaaChip mostPower,boolean map)
    {	prepareTrapScore(mostPower,map);
    	double wood_score = 0;
    	for(int lim=trapCells.length-1; lim>=0; lim--)
		{	
			ArimaaCell trap = trapCells[lim];
			int mytraps = trap.trap_adj[player];
	     	wood_score += trap_scores[mytraps%100];	// good to surround the traps

     	}
       	return(wood_score);
	}
    private void markRunwaysFrom(int player,ArimaaCell c,int baseval,int decay_enemy,int decay_depth)
    {	// c is an empty cell
    	//int decay_enemy = 10;//38;
    	//int decay_depth = 10;//25;
    	if(c.runwayScore<baseval)
    		{
    		// got a better value by this route
    		int retro_direction = (player==FIRST_PLAYER_INDEX) ? CELL_DOWN : CELL_UP;
    		c.runwayScore = baseval;
    		for(int dir=CELL_FULL_TURN-1; dir>=0; dir--)
    			{
    			if(dir!=retro_direction)
    			{
     			ArimaaCell next = c.exitTo(dir);	// adjacent cell on the runway path
     			int val = baseval;
    			if(next!=null)
    			{
    			ArimaaChip top = next.topChip();
    			if((top==null) || ((top.isRabbit()&&(playerIndex(top)==player))))
    				{
    				// dock the value depending on if the spot is frozen
    				boolean friendly = false;
    				boolean enemy = false;
    				
    				for(int dir2=CELL_FULL_TURN-1; dir2>=0; dir2--)
    				{
    				ArimaaCell adj2 = next.exitTo(dir2);
    				if(adj2!=null)
    				{	ArimaaChip top2 = adj2.topChip();
    					if(top2!=null) 
    					{
    						friendly |= (playerIndex(top2)==player);
    						enemy |= (playerIndex(top2)!=player) && (top2.chipPower()>ArimaaChip.RABBIT_POWER);
    					}
    				}}
    				// friends only, full value
    				// enemies only, -8
    				// friends and enemies -4
    				// neither -2
    				if(friendly)
    					{ if(enemy) 
    						{ val -=14; } }
    				else {if(enemy) { val -= decay_enemy; } else { val -=2; }}
    				
    				if(val>25) { markRunwaysFrom(player,next,val-decay_depth,decay_enemy,decay_depth); }
    				} // end of empty cell adjacent to the runway 
    			
    			}	// end of adjacent cell to runway
    			}	// end of not retro direction
    			}	// end of adjacent direction
    		}	// end of new score is better
    }

    public void markRunways(int forPlayer)
    {
    	int target_row = forPlayer==FIRST_PLAYER_INDEX ?  1 : 8;
    	ArimaaCell home = getCell('A',target_row);
    	while(home!=null)
    	{	if(home.topChip()==null) { markRunwaysFrom(forPlayer,home,100,15,10); }
    		home = home.exitTo(CELL_RIGHT);
    	}
    }

    // distance to previous move, adjacent moves are better.
    private double focusScore()
    {	double val = 0.0;
    	int siz = destStack.size();
    	if(siz>=2)
    	{	ArimaaCell d1 = destStack.elementAt(siz-1);
    		ArimaaCell d2 = destStack.elementAt(siz-2);
    		if((d1!=null) && (d2!=null))
    		{	// negative so we score close moves better than distant moves
    			val -= mDistance(d1.col-'A',d1.row,d2.col-'A',d2.row);
    		}
    	}
    
    	return(val);
    }
   	private double wood_value[] = {0.0,		// rough value of the wood, based on the "rabbit standard" of 10 points
			10*VALUE_OF_RABBIT,	// elephant
			8*VALUE_OF_RABBIT,	// camel
			6*VALUE_OF_RABBIT,	// horse
			4*VALUE_OF_RABBIT,	// dog
			2*VALUE_OF_RABBIT,	// cat
			VALUE_OF_RABBIT};	// rabbit
   	
   	// rabbit value depending on how many are left.  The basic model is that the value
   	// of captured rabbits should be redistributed to the others.
    private double rabbit_value[] = { 100*VALUE_OF_RABBIT,	// zero rabbits, we already lost
			   100*VALUE_OF_RABBIT,		// last rabbit, worth the world
			    4*VALUE_OF_RABBIT,		// 2 rabbits left
			    2.5*VALUE_OF_RABBIT,	// 3 rabbits left, worth 30 points apeace
			    2*VALUE_OF_RABBIT,		// 4 rabbits left
			    1.5*VALUE_OF_RABBIT,	// 5 rabbits left
			    1.2*VALUE_OF_RABBIT,	// 6 rabbits left
			    1.1*VALUE_OF_RABBIT,	// 7 rabbits left
			    VALUE_OF_RABBIT};		// 8 rabbits left

    //
    // this is a bonus for clustering multiple pieces near a trap
    // it's good to be adjacent to keep friends from falling in, and
    // also to keep unfriendly pieces from being suppored by friends
    private double trap_scores[] = {-5.0,-5.0,0.0,2.5,3.0};	// benefit for clustering around the traps

    
    // used by the viewer in some debug modes.
    public void scoreTraps()
    {	ArimaaChip mostPower = null;
    	{	//this is for use by the viewer
    		int nextTurn = nextPlayer[whoseTurn];
    		for(ArimaaCell occ = allCells; occ!=null; occ=occ.next)
    			{ ArimaaChip top = occ.topChip();
    			  if((top!=null) 
    					  && (playerIndex(top)==nextTurn) 
    					  && ((mostPower==null) || (top.chipPower()>mostPower.chipPower()))) 
    					  {
    				  	mostPower=top;
    					  }
    			}
    	}
    	prepareTrapScore(mostPower,true);
    }
    
    double advancement_weight = 0.0;
    
    public double ScoreForPlayer(int player,boolean print)
    {   // look for a win for player.  This algorithm should work for Gobblet Jr too.
     	//
    	double WOOD_WEIGHT = 2.0;
    	double RUNWAY_WEIGHT = 0.2;
    	double SUPPORT_WEIGHT = 0.05;
    	double ATTACK_WEIGHT = 0.15;
    	double TRAP_WEIGHT = -0.1;
    	// this is an overall penalty for losing rabbits.  One or two not-so-bad but exponentially worse
    	// as the number of rabbits decreases
        double rabbit_adjustment[] = { -1000,-50,-25,-15,-10,-5,-3,-1.5,0.0};
        //
        // this is a penalty for advancing the rabits toward the goal when the runway
        // is blocked.
        double rabbit_row_adjustment[] = {0, -0.3, -0.5,-1.8, -2.6,-4.5,-8.0,1000};
        // this is multiplied by the agression factor to move rabbits forward to advance the game.
        double rabbit_advancement[] = {0,1,2,3,4,5,6,7,8};
        
        //
        // this is a penalty for having your pieces frozen.  Mobility is good!
        //
        double frozen_penalty[] = {0.0,
        			-1000.0,		// elephants can't be frozen of course
        				-4.0,	// frozen camel
        				-3.0,	// frozen horse
        				-2.0,	// frozen dog
        				-1.5,	// frozen cat
        				-0.5};	// frozen rabbit
        // 
        // this is a penalty for sitting in a trap.  even if you're supported
        // it's a risky manouver
        double trap_penalty[] = {0.0,
        		-4.0,	// elephant
        		-3.0,	// camel
        		-2.0,	// horse
        		-1.0,	// dog
        		-0.5,	// cat
        		-0.3};	// rabbit
        ArimaaChip mostPower = null;
        int mostPowerInt = -1;
    	double wood_score = 0.0;
		int n_opp_horses = 0;
		int n_opp_cats = 0;
		int n_opp_dogs = 0;
		int n_opp_rabits = 0;
		int n_my_horses = 0;
		int n_my_cats = 0;
		int n_my_dogs = 0;
		int n_my_rabbits = 0;
    	opp_camel = opp_elephant = null;
    	my_camel = my_elephant = null;
    	int my_piece_count = 0;
    	

    	wood_score += rabbit_adjustment[rabbitsOnBoard[player]];		// penalise for lost rabbits

       	wood_score += focusScore()*0.10;								// bonus for locality of moves
    	
       	wood_score += my_piece_count*advancement_weight;				// bonus for agressive play, which gradually increases until there is a capture
       	
    	for(ArimaaCell c = allCells; c!=null; c=c.next)
    	{	c.runwayScore = 0;
    		ArimaaChip top = c.topChip();
    		if(top!=null)
    		{
        	int type = top.chipType();
        	if(playerIndex(top)==player)
    		{
    		// my pieces
        	my_piece_count++;
    		wood_score += WOOD_WEIGHT*wood_value[type];
    		if(c.isTrap) { wood_score += trap_penalty[type]; }
    		if(type!=ArimaaChip.ELEPHANT_INDEX)
    		{	if(getFrozenCode(c,top)==FROZEN_CODE_FROZEN)
				{	wood_score += frozen_penalty[type];
				}
    		}
    		switch(type)
    		{
    		default: throw G.Error("chip type not handled");
    		case ArimaaChip.ELEPHANT_INDEX:
    			my_elephant = c;
    			break;
    		case ArimaaChip.RABBIT_INDEX:
    			// small encouragement to advance the rabbits
    			{ //int effective_row = (player==FIRST_PLAYER_INDEX)? 8-c.row : c.row-1;
    			  //wood_score += rabbit_row_adjustment[effective_row];
    			  my_rabbits[n_my_rabbits++]=c;

    			}
    			break;
    		case ArimaaChip.CAMEL_INDEX:
    			my_camel = c;
    			break;
    		case ArimaaChip.HORSE_INDEX:
    			my_horses[n_my_horses++] = c;
    			break;
    		case ArimaaChip.DOG_INDEX:
    			my_dogs[n_my_dogs++] = c;
    			break;
    		case ArimaaChip.CAT_INDEX:
    			my_cats[n_my_cats++]=c;
    			break;
    			
    			}
    			
    		}
    		else
    		{	// opponents pieces
            	int power = top.chipPower();
    			if((mostPower==null)||(mostPowerInt<power))
    			{	mostPower = top;
    				mostPowerInt = power;
    			}
    	   		switch(type)
        		{
        		default: throw G.Error("chip type not handled");
        		case ArimaaChip.ELEPHANT_INDEX:
        			// elephants can't be frozen so there's no point in checking.
        			opp_elephant = c;
        			break;
        		case ArimaaChip.RABBIT_INDEX:
         			opp_rabbits[n_opp_rabits++] = c;
        			break;
        		case ArimaaChip.CAMEL_INDEX:
        			opp_camel = c;
        			break;
        		case ArimaaChip.HORSE_INDEX:
        			opp_horses[n_opp_horses++] = c;
        			break;
        		case ArimaaChip.DOG_INDEX:
        			opp_dogs[n_opp_dogs++] = c;
        			break;
        		case ArimaaChip.CAT_INDEX:
        			opp_cats[n_opp_cats++] = c;
        			break;
        			}
        			
    		
    		}
    		}
    	}
    	double trapscore = scoreTraps(player,mostPower,true);
     	
    	markRunways(player);	// mark the free paths to the goal
    	

    	//
    	// laws of attraction.  Strong pieces are attracted to opposing weaker pieces, and
    	// also to friendly weaker pieces.
    	// weaker pieces (cats and dogs) are attracted only to our own pieces.
    	// rabbits are attracted to all our pieces but repelled by opposing pieces.
    	//
    	double tmapscore = 0.0;
    	double my_tmap[][] = currentTrapMap[player];
    	double opp_tmap[][] = currentTrapMap[nextPlayer[player]];
    	if(my_elephant!=null)
    	{	
    		tmapscore += SUPPORT_WEIGHT*disScore(my_elephant,my_camel,my_tmap,wood_value[ArimaaChip.CAMEL_INDEX]);
    		tmapscore += ATTACK_WEIGHT*disScore(my_elephant,opp_camel,opp_tmap,wood_value[ArimaaChip.CAMEL_INDEX]);
    		for(int i=0;i<n_my_horses;i++) 
    			{  tmapscore += SUPPORT_WEIGHT*disScore(my_elephant,my_horses[i],my_tmap,wood_value[ArimaaChip.HORSE_INDEX]); 
    			}
    		for(int j=0;j<n_opp_horses;j++) 
    			{ tmapscore +=ATTACK_WEIGHT*disScore(my_elephant,opp_horses[j],opp_tmap,wood_value[ArimaaChip.HORSE_INDEX]); 
    			}
    		for(int j=0;j<n_my_dogs;j++) 
    			{ tmapscore += SUPPORT_WEIGHT*disScore(my_elephant,my_dogs[j],my_tmap,wood_value[ArimaaChip.DOG_INDEX]); 
    			}
			for(int j=0;j<n_opp_dogs;j++) 
				{ tmapscore +=ATTACK_WEIGHT*disScore(my_elephant,opp_dogs[j],opp_tmap,wood_value[ArimaaChip.DOG_INDEX]);
				}
    		for(int j=0;j<n_my_cats;j++) 
			{ tmapscore += SUPPORT_WEIGHT*disScore(my_elephant,my_cats[j],my_tmap,wood_value[ArimaaChip.CAT_INDEX]); 
			}
    		for(int j=0;j<n_opp_cats;j++) 
			{ tmapscore +=ATTACK_WEIGHT*disScore(my_elephant,opp_cats[j],opp_tmap,wood_value[ArimaaChip.CAT_INDEX]);
			}
	   		for(int j=0;j<n_my_rabbits;j++) 
			{ tmapscore += SUPPORT_WEIGHT*disScore(my_elephant,my_rabbits[j],my_tmap,wood_value[ArimaaChip.RABBIT_INDEX]); 
			}
			for(int j=0;j<n_opp_rabits;j++) 
			{ tmapscore +=ATTACK_WEIGHT*disScore(my_elephant,opp_rabbits[j],opp_tmap,wood_value[ArimaaChip.RABBIT_INDEX]);
			}

			//wood_score -= G.distance(my_elephant.col-'A',my_elephant.row,opp_center_row,opp_center_col);
			// elephants do not fear traps unless they are in them
			if(my_elephant.isTrap) 
				{ tmapscore += TRAP_WEIGHT*my_tmap[my_elephant.col-'A'][my_elephant.row-1]*wood_value[ArimaaChip.ELEPHANT_INDEX]; 
				}
    	}
    	if(my_camel!=null)
    	{	for(int i=0;i<n_my_horses;i++) 
    			{ tmapscore +=SUPPORT_WEIGHT*disScore(my_camel,my_horses[i],my_tmap,wood_value[ArimaaChip.HORSE_INDEX]);
    			}
    		for(int j=0;j<n_opp_horses;j++)
    			{ tmapscore +=ATTACK_WEIGHT*disScore(my_camel,opp_horses[j],opp_tmap,wood_value[ArimaaChip.HORSE_INDEX]);
    			}
    		for(int j=0;j<n_my_dogs;j++) 
    			{ tmapscore +=SUPPORT_WEIGHT*disScore(my_camel,my_dogs[j],my_tmap,wood_value[ArimaaChip.DOG_INDEX]); 
    			}
			for(int j=0;j<n_opp_dogs;j++) 
				{ tmapscore +=ATTACK_WEIGHT*disScore(my_camel,opp_dogs[j],opp_tmap,wood_value[ArimaaChip.DOG_INDEX]);
				}
    		for(int j=0;j<n_my_cats;j++) 
			{ tmapscore += SUPPORT_WEIGHT*disScore(my_camel,my_cats[j],my_tmap,wood_value[ArimaaChip.CAT_INDEX]); 
			}
    		for(int j=0;j<n_opp_cats;j++) 
			{ tmapscore +=ATTACK_WEIGHT*disScore(my_camel,opp_cats[j],opp_tmap,wood_value[ArimaaChip.CAT_INDEX]);
			}
	   		for(int j=0;j<n_my_rabbits;j++) 
			{ tmapscore += SUPPORT_WEIGHT*disScore(my_camel,my_rabbits[j],my_tmap,wood_value[ArimaaChip.RABBIT_INDEX]); 
			}
			for(int j=0;j<n_opp_rabits;j++) 
			{ tmapscore +=ATTACK_WEIGHT*disScore(my_camel,opp_rabbits[j],opp_tmap,wood_value[ArimaaChip.RABBIT_INDEX]);
			}
			//wood_score -= G.distance(my_camel.col-'A',my_camel.row,opp_center_row,opp_center_col);
			if(my_camel.isTrap || (mostPowerInt>ArimaaChip.CAMEL_POWER))
			{	double tm = my_tmap[my_camel.col-'A'][my_camel.row-1];
				tmapscore += TRAP_WEIGHT*tm*wood_value[ArimaaChip.CAMEL_INDEX]; 
			}
    	}
    	for(int i=0;i<n_my_horses; i++)
    	{	ArimaaCell pp = my_horses[i];
    		for(int j=0;j<n_my_dogs;j++) 
    			{ tmapscore +=SUPPORT_WEIGHT*disScore(pp,my_dogs[j],my_tmap,wood_value[ArimaaChip.DOG_INDEX]); 
    			}
			for(int j=0;j<n_opp_dogs;j++) 
				{ tmapscore +=ATTACK_WEIGHT*disScore(pp,opp_dogs[j],opp_tmap,wood_value[ArimaaChip.DOG_INDEX]);
				}
    		for(int j=0;j<n_my_cats;j++) 
			{ tmapscore += SUPPORT_WEIGHT*disScore(pp,my_cats[j],my_tmap,wood_value[ArimaaChip.CAT_INDEX]); 
			}
    		for(int j=0;j<n_opp_cats;j++) 
			{ tmapscore +=ATTACK_WEIGHT*disScore(pp,opp_cats[j],opp_tmap,wood_value[ArimaaChip.CAT_INDEX]);
			}
	   		for(int j=0;j<n_my_rabbits;j++) 
			{ tmapscore += SUPPORT_WEIGHT*disScore(pp,my_rabbits[j],my_tmap,wood_value[ArimaaChip.RABBIT_INDEX]); 
			}
			for(int j=0;j<n_opp_rabits;j++) 
			{ tmapscore +=ATTACK_WEIGHT*disScore(pp,opp_rabbits[j],opp_tmap,wood_value[ArimaaChip.RABBIT_INDEX]);
			}
			//wood_score -= G.distance(pp.col-'A',pp.row,opp_center_row,opp_center_col);
			if(pp.isTrap || (mostPowerInt>ArimaaChip.HORSE_POWER))
			{
				tmapscore += TRAP_WEIGHT*my_tmap[pp.col-'A'][pp.row-1]*wood_value[ArimaaChip.HORSE_INDEX];
			}
    	}
    	for(int i=0;i<n_my_dogs; i++)
    	{	ArimaaCell pp = my_dogs[i];
	   		for(int j=0;j<n_my_cats;j++) 
			{ tmapscore += SUPPORT_WEIGHT*disScore(pp,my_cats[j],my_tmap,wood_value[ArimaaChip.CAT_INDEX]); 
			}
			for(int j=0;j<n_opp_cats;j++) 
			{ tmapscore +=ATTACK_WEIGHT*disScore(pp,opp_cats[j],opp_tmap,wood_value[ArimaaChip.CAT_INDEX]);
			}
	   		for(int j=0;j<n_my_rabbits;j++) 
			{ tmapscore += SUPPORT_WEIGHT*disScore(pp,my_rabbits[j],my_tmap,wood_value[ArimaaChip.RABBIT_INDEX]); 
			}
			for(int j=0;j<n_opp_rabits;j++) 
			{ tmapscore +=ATTACK_WEIGHT*disScore(pp,opp_rabbits[j],opp_tmap,wood_value[ArimaaChip.RABBIT_INDEX]);
			}

			if(pp.isTrap || (mostPowerInt>ArimaaChip.DOG_POWER))
			{	double inc = TRAP_WEIGHT*my_tmap[pp.col-'A'][pp.row-1]*wood_value[ArimaaChip.DOG_INDEX];
				//G.print("trap "+pp+" "+inc);
				tmapscore += inc;
			}
    	}
    	for(int i=0;i<n_my_cats; i++)
    	{	ArimaaCell pp = my_cats[i];
			if( pp.isTrap || (mostPowerInt>ArimaaChip.CAT_POWER))
			{
				tmapscore += TRAP_WEIGHT*my_tmap[pp.col-'A'][pp.row-1]*wood_value[ArimaaChip.CAT_INDEX];
			}
    	}
    	for(int i=0;i<n_my_rabbits; i++)
    	{	ArimaaCell pp = my_rabbits[i];
    		// score for runway position
    		{
    		double runway =  pp.runwayScore;
    		int effective_row = (player==FIRST_PLAYER_INDEX)? 8-pp.row : pp.row-1;
     		if(runway!=0) 
     			{ wood_score +=runway*RUNWAY_WEIGHT; 
     			}
    			else
    			{   
    				wood_score += rabbit_row_adjustment[effective_row]; 
    			}
    		// advance our rabbits to get on with it
    		wood_score += advancement_weight*rabbit_advancement[effective_row];
    		}
    		// value rabbits increasingly if there are fewer of them left
    		{double step = TRAP_WEIGHT*my_tmap[pp.col-'A'][pp.row-1]*rabbit_value[rabbitsOnBoard[player]];
    		//if(print) { G.print("pp "+pp+" "+step); }
    		tmapscore += step;
    		}
   	}
    	if(print)
    	{
    		G.print("P"+player+" trap "+trapscore+" map "+tmapscore+" wood "+wood_score);
    	}
    	wood_score += tmapscore + trapscore;
    	return(wood_score);
    }
    public double ScoreForPlayer1(int player,boolean print)
    {   // look for a win for player.  This algorithm should work for Gobblet Jr too.
     	//
    	double RUNWAY_WEIGHT = 0.2;
    	double SUPPORT_WEIGHT = 0.15;
    	double ATTACK_WEIGHT = 0.2;
    	double TRAP_WEIGHT = -0.1;
    	// this is an overall penalty for losing rabbits.  One or two not-so-bad but exponentially worse
    	// as the number of rabbits decreases
        double rabbit_adjustment[] = { -1000,-50,-25,-15,-10,-5,-3,-1.5,0.0};
        //
        // this is a penalty for advancing the rabits toward the goal when the runway
        // is blocked.
        double rabbit_row_adjustment[] = {0, -0.3, -0.5,-1.8, -2.6,-4.5,-8.0,1000};
        // this is multiplied by the agression factor to move rabbits forward to advance the game.
        double rabbit_advancement[] = {0,1,2,3,4,5,6,7,8};
        
        //
        // this is a penalty for having your pieces frozen.  Mobility is good!
        //
        double frozen_penalty[] = {0.0,
        			-1000.0,		// elephants can't be frozen of course
        				-4.0,	// frozen camel
        				-3.0,	// frozen horse
        				-2.0,	// frozen dog
        				-1.5,	// frozen cat
        				-0.5};	// frozen rabbit
        // 
        // this is a penalty for sitting in a trap.  even if you're supported
        // it's a risky manouver
        double trap_penalty[] = {0.0,
        		-4.0,	// elephant
        		-3.0,	// camel
        		-2.0,	// horse
        		-1.0,	// dog
        		-0.5,	// cat
        		-0.3};	// rabbit
        ArimaaChip mostPower = null;
        int mostPowerInt = -1;
    	double wood_score = 0.0;
		int n_opp_horses = 0;
		int n_opp_cats = 0;
		int n_opp_dogs = 0;
		int n_opp_rabits = 0;
		int n_my_horses = 0;
		int n_my_cats = 0;
		int n_my_dogs = 0;
		int n_my_rabbits = 0;
    	opp_camel = opp_elephant = null;
    	my_camel = my_elephant = null;
    	int my_piece_count = 0;
    	

    	wood_score += rabbit_adjustment[rabbitsOnBoard[player]];		// penalise for lost rabbits

       	wood_score += focusScore()*10;									// bonus for locality of moves
    	
       	wood_score += my_piece_count*advancement_weight;				// bonus for agressive play, which gradually increases until there is a capture
       	
    	for(ArimaaCell c = allCells; c!=null; c=c.next)
    	{	c.runwayScore = 0;
    		ArimaaChip top = c.topChip();
    		if(top!=null)
    		{
        	int type = top.chipType();
        	if(playerIndex(top)==player)
    		{
    		// my pieces
        	my_piece_count++;
    		wood_score += wood_value[type];
    		if(c.isTrap) { wood_score += trap_penalty[type]; }
    		if(type!=ArimaaChip.ELEPHANT_INDEX)
    		{	if(getFrozenCode(c,top)==FROZEN_CODE_FROZEN)
				{	wood_score += frozen_penalty[type];
				}
    		}
    		switch(type)
    		{
    		default: throw G.Error("chip type not handled");
    		case ArimaaChip.ELEPHANT_INDEX:
    			my_elephant = c;
    			break;
    		case ArimaaChip.RABBIT_INDEX:
    			// small encouragement to advance the rabbits
    			{ //int effective_row = (player==FIRST_PLAYER_INDEX)? 8-c.row : c.row-1;
    			  //wood_score += rabbit_row_adjustment[effective_row];
    			  my_rabbits[n_my_rabbits++]=c;

    			}
    			break;
    		case ArimaaChip.CAMEL_INDEX:
    			my_camel = c;
    			break;
    		case ArimaaChip.HORSE_INDEX:
    			my_horses[n_my_horses++] = c;
    			break;
    		case ArimaaChip.DOG_INDEX:
    			my_dogs[n_my_dogs++] = c;
    			break;
    		case ArimaaChip.CAT_INDEX:
    			my_cats[n_my_cats++]=c;
    			break;
    			
    			}
    			
    		}
    		else
    		{	// opponents pieces
            	int power = top.chipPower();
    			if((mostPower==null)||(mostPowerInt<power))
    			{	mostPower = top;
    				mostPowerInt = power;
    			}
    	   		switch(type)
        		{
        		default: throw G.Error("chip type not handled");
        		case ArimaaChip.ELEPHANT_INDEX:
        			// elephants can't be frozen so there's no point in checking.
        			opp_elephant = c;
        			break;
        		case ArimaaChip.RABBIT_INDEX:
         			opp_rabbits[n_opp_rabits++] = c;
        			break;
        		case ArimaaChip.CAMEL_INDEX:
        			opp_camel = c;
        			break;
        		case ArimaaChip.HORSE_INDEX:
        			opp_horses[n_opp_horses++] = c;
        			break;
        		case ArimaaChip.DOG_INDEX:
        			opp_dogs[n_opp_dogs++] = c;
        			break;
        		case ArimaaChip.CAT_INDEX:
        			opp_cats[n_opp_cats++] = c;
        			break;
        			}
        			
    		
    		}
    		}
    	}
    	double trapscore = scoreTraps(player,mostPower,true);
     	
    	markRunways(player);	// mark the free paths to the goal
    	

    	//
    	// laws of attraction.  Strong pieces are attracted to opposing weaker pieces, and
    	// also to friendly weaker pieces.
    	// weaker pieces (cats and dogs) are attracted only to our own pieces.
    	// rabbits are attracted to all our pieces but repelled by opposing pieces.
    	//
    	double tmapscore = 0.0;
    	double my_tmap[][] = currentTrapMap[player];
    	double opp_tmap[][] = currentTrapMap[nextPlayer[player]];
    	if(my_elephant!=null)
    	{	
    		tmapscore += SUPPORT_WEIGHT*disScore(my_elephant,my_camel,my_tmap,wood_value[ArimaaChip.CAMEL_INDEX]);
    		tmapscore += ATTACK_WEIGHT*disScore(my_elephant,opp_camel,opp_tmap,wood_value[ArimaaChip.CAMEL_INDEX]);
    		for(int i=0;i<n_my_horses;i++) 
    			{  tmapscore += SUPPORT_WEIGHT*disScore(my_elephant,my_horses[i],my_tmap,wood_value[ArimaaChip.HORSE_INDEX]); 
    			}
    		for(int j=0;j<n_opp_horses;j++) 
    			{ tmapscore +=ATTACK_WEIGHT*disScore(my_elephant,opp_horses[j],opp_tmap,wood_value[ArimaaChip.HORSE_INDEX]); 
    			}
    		for(int j=0;j<n_my_dogs;j++) 
    			{ tmapscore += SUPPORT_WEIGHT*disScore(my_elephant,my_dogs[j],my_tmap,wood_value[ArimaaChip.DOG_INDEX]); 
    			}
			for(int j=0;j<n_opp_dogs;j++) 
				{ tmapscore +=ATTACK_WEIGHT*disScore(my_elephant,opp_dogs[j],opp_tmap,wood_value[ArimaaChip.DOG_INDEX]);
				}
    		for(int j=0;j<n_my_cats;j++) 
			{ tmapscore += SUPPORT_WEIGHT*disScore(my_elephant,my_cats[j],my_tmap,wood_value[ArimaaChip.CAT_INDEX]); 
			}
    		for(int j=0;j<n_opp_cats;j++) 
			{ tmapscore +=ATTACK_WEIGHT*disScore(my_elephant,opp_cats[j],opp_tmap,wood_value[ArimaaChip.CAT_INDEX]);
			}
	   		for(int j=0;j<n_my_rabbits;j++) 
			{ tmapscore += SUPPORT_WEIGHT*disScore(my_elephant,my_rabbits[j],my_tmap,wood_value[ArimaaChip.RABBIT_INDEX]); 
			}
			for(int j=0;j<n_opp_rabits;j++) 
			{ tmapscore +=ATTACK_WEIGHT*disScore(my_elephant,opp_rabbits[j],opp_tmap,wood_value[ArimaaChip.RABBIT_INDEX]);
			}

			//wood_score -= G.distance(my_elephant.col-'A',my_elephant.row,opp_center_row,opp_center_col);
			// elephants do not fear traps unless they are in them
			if(my_elephant.isTrap) 
				{ tmapscore += TRAP_WEIGHT*my_tmap[my_elephant.col-'A'][my_elephant.row-1]*wood_value[ArimaaChip.ELEPHANT_INDEX]; 
				}
    	}
    	if(my_camel!=null)
    	{	for(int i=0;i<n_my_horses;i++) 
    			{ tmapscore +=SUPPORT_WEIGHT*disScore(my_camel,my_horses[i],my_tmap,wood_value[ArimaaChip.HORSE_INDEX]);
    			}
    		for(int j=0;j<n_opp_horses;j++)
    			{ tmapscore +=ATTACK_WEIGHT*disScore(my_camel,opp_horses[j],opp_tmap,wood_value[ArimaaChip.HORSE_INDEX]);
    			}
    		for(int j=0;j<n_my_dogs;j++) 
    			{ tmapscore +=SUPPORT_WEIGHT*disScore(my_camel,my_dogs[j],my_tmap,wood_value[ArimaaChip.DOG_INDEX]); 
    			}
			for(int j=0;j<n_opp_dogs;j++) 
				{ tmapscore +=ATTACK_WEIGHT*disScore(my_camel,opp_dogs[j],opp_tmap,wood_value[ArimaaChip.DOG_INDEX]);
				}
    		for(int j=0;j<n_my_cats;j++) 
			{ tmapscore += SUPPORT_WEIGHT*disScore(my_camel,my_cats[j],my_tmap,wood_value[ArimaaChip.CAT_INDEX]); 
			}
    		for(int j=0;j<n_opp_cats;j++) 
			{ tmapscore +=ATTACK_WEIGHT*disScore(my_camel,opp_cats[j],opp_tmap,wood_value[ArimaaChip.CAT_INDEX]);
			}
	   		for(int j=0;j<n_my_rabbits;j++) 
			{ tmapscore += SUPPORT_WEIGHT*disScore(my_camel,my_rabbits[j],my_tmap,wood_value[ArimaaChip.RABBIT_INDEX]); 
			}
			for(int j=0;j<n_opp_rabits;j++) 
			{ tmapscore +=ATTACK_WEIGHT*disScore(my_camel,opp_rabbits[j],opp_tmap,wood_value[ArimaaChip.RABBIT_INDEX]);
			}
			//wood_score -= G.distance(my_camel.col-'A',my_camel.row,opp_center_row,opp_center_col);
			if(my_camel.isTrap || (mostPowerInt>ArimaaChip.CAMEL_POWER))
			{
				tmapscore += TRAP_WEIGHT*my_tmap[my_camel.col-'A'][my_camel.row-1]*wood_value[ArimaaChip.CAMEL_INDEX]; 
			}
    	}
    	for(int i=0;i<n_my_horses; i++)
    	{	ArimaaCell pp = my_horses[i];
    		for(int j=0;j<n_my_dogs;j++) 
    			{ tmapscore +=SUPPORT_WEIGHT*disScore(pp,my_dogs[j],my_tmap,wood_value[ArimaaChip.DOG_INDEX]); 
    			}
			for(int j=0;j<n_opp_dogs;j++) 
				{ tmapscore +=ATTACK_WEIGHT*disScore(pp,opp_dogs[j],opp_tmap,wood_value[ArimaaChip.DOG_INDEX]);
				}
    		for(int j=0;j<n_my_cats;j++) 
			{ tmapscore += SUPPORT_WEIGHT*disScore(pp,my_cats[j],my_tmap,wood_value[ArimaaChip.CAT_INDEX]); 
			}
    		for(int j=0;j<n_opp_cats;j++) 
			{ tmapscore +=ATTACK_WEIGHT*disScore(pp,opp_cats[j],opp_tmap,wood_value[ArimaaChip.CAT_INDEX]);
			}
	   		for(int j=0;j<n_my_rabbits;j++) 
			{ tmapscore += SUPPORT_WEIGHT*disScore(pp,my_rabbits[j],my_tmap,wood_value[ArimaaChip.RABBIT_INDEX]); 
			}
			for(int j=0;j<n_opp_rabits;j++) 
			{ tmapscore +=ATTACK_WEIGHT*disScore(pp,opp_rabbits[j],opp_tmap,wood_value[ArimaaChip.RABBIT_INDEX]);
			}
			//wood_score -= G.distance(pp.col-'A',pp.row,opp_center_row,opp_center_col);
			if(pp.isTrap || (mostPowerInt>ArimaaChip.HORSE_POWER))
			{
				tmapscore += TRAP_WEIGHT*my_tmap[pp.col-'A'][pp.row-1]*wood_value[ArimaaChip.HORSE_INDEX];
			}
    	}
    	for(int i=0;i<n_my_dogs; i++)
    	{	ArimaaCell pp = my_dogs[i];
	   		for(int j=0;j<n_my_cats;j++) 
			{ tmapscore += SUPPORT_WEIGHT*disScore(pp,my_cats[j],my_tmap,wood_value[ArimaaChip.CAT_INDEX]); 
			}
			for(int j=0;j<n_opp_cats;j++) 
			{ tmapscore +=ATTACK_WEIGHT*disScore(pp,opp_cats[j],opp_tmap,wood_value[ArimaaChip.CAT_INDEX]);
			}
	   		for(int j=0;j<n_my_rabbits;j++) 
			{ tmapscore += SUPPORT_WEIGHT*disScore(pp,my_rabbits[j],my_tmap,wood_value[ArimaaChip.RABBIT_INDEX]); 
			}
			for(int j=0;j<n_opp_rabits;j++) 
			{ tmapscore +=ATTACK_WEIGHT*disScore(pp,opp_rabbits[j],opp_tmap,wood_value[ArimaaChip.RABBIT_INDEX]);
			}

			if(pp.isTrap || (mostPowerInt>ArimaaChip.DOG_POWER))
			{
				tmapscore += TRAP_WEIGHT*my_tmap[pp.col-'A'][pp.row-1]*wood_value[ArimaaChip.DOG_INDEX];
			}
    	}
    	for(int i=0;i<n_my_cats; i++)
    	{	ArimaaCell pp = my_cats[i];
			if( pp.isTrap || (mostPowerInt>ArimaaChip.CAT_POWER))
			{
				tmapscore += TRAP_WEIGHT*my_tmap[pp.col-'A'][pp.row-1]*wood_value[ArimaaChip.CAT_INDEX];
			}
    	}
    	for(int i=0;i<n_my_rabbits; i++)
    	{	ArimaaCell pp = my_rabbits[i];
    		// score for runway position
    		{
    		double runway =  pp.runwayScore;
    		int effective_row = (player==FIRST_PLAYER_INDEX)? 8-pp.row : pp.row-1;
    		if(runway!=0) { wood_score +=runway*RUNWAY_WEIGHT; }
    			else
    			{ wood_score += rabbit_row_adjustment[effective_row]; 
    			}
    		// advance our rabbits to get on with it
    		wood_score += advancement_weight*rabbit_advancement[effective_row];
    		}
    		// value rabbits increasingly if there are fewer of them left
    		{double step = TRAP_WEIGHT*my_tmap[pp.col-'A'][pp.row-1]*rabbit_value[rabbitsOnBoard[player]];
    		//if(print) { G.print("pp "+pp+" "+step); }
    		tmapscore += step;
    		}
   	}
    	if(print)
    	{
    		G.print("P"+player+" trap "+trapscore+" map "+tmapscore+" wood "+wood_score);
    	}
    	wood_score += tmapscore + trapscore;
    	return(wood_score);
    }

 
    public boolean isFrozen(ArimaaCell c)
    {
    	return(0!=(getFrozenCode(c,c.topChip())&FROZEN_CODE_FROZEN));
    }
 
    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public boolean  acceptPlacement()
    {	pickedObject = null;
    	boolean captures = false;
    	while(stackIndex>=0)
    	{
        droppedDestStack[stackIndex] = null;
        pickedSourceStack[stackIndex] = null;
        captures |=captureStack[stackIndex]!=null;
        captureStack[stackIndex] = null;
        captureStack2[stackIndex] = null;
        capturedCellStack[stackIndex]=null;
        capturedCellStack2[stackIndex]=null;
        stackIndex--;
    	}
    	stackIndex=0;
    	previousEmptied.clear();
    	previousPlayer.clear();
    	previousPlaced.clear();
    	return(captures);
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    if(stackIndex>0)
    {
    stackIndex--;
    destStack.pop();
    ArimaaCell dr = droppedDestStack[stackIndex];
    ArimaaChip cap = captureStack[stackIndex];
    if(dr.onBoard) {
    	ArimaaCell target = dr;
    	if(dr.auxDisplay!=null && dr.auxDisplay.lastPlaceMoveNumber==moveNumber) { target = dr.auxDisplay; }
    	target.lastPlaced = previousPlaced.pop();
    	target.lastPlaceMoveNumber = -1;
    	placementIndex--;
    }
    if(cap!=null)
    {	ArimaaChip cap2 = captureStack2[stackIndex];
    	if(cap2!=null)
    		{
    	  	rack[playerIndex(cap2)][cap2.chipType()].removeTop();
        	captured--;
        	addChip(capturedCellStack2[stackIndex],cap2);
        	capturedCellStack2[stackIndex]=null;
        	captureStack2[stackIndex]=null;
       	    capturedPiece2 = null;
        	capturedLocation2=null;
    		}
    	rack[playerIndex(cap)][cap.chipType()].removeTop();
    	captured--;
    	addChip(capturedCellStack[stackIndex],cap);
    	capturedCellStack[stackIndex]=null;
    	captureStack[stackIndex]=null;
   	    capturedPiece = null;
    	capturedLocation=null;
    }
    if(dr!=null)
    	{
    	pickedObject = removeChip(dr);
    	droppedDestStack[stackIndex]=null;
    	}
    }
    }
    private void addChip(ArimaaCell c,ArimaaChip ch)
    {	       
    	if(c.onBoard)
    		{int player = playerIndex(ch);
    		 if(ch.isRabbit()) 
    		 	{ rabbitsOnBoard[player]++; 
    		 	  ArimaaCell src = stackIndex>0?pickedSourceStack[stackIndex-1]:null;
    		 	  if(src!=null)
    		 	  	{ if(src.row!=c.row) 
    		 	  		{ lastAdvancementMoveNumber = moveNumber; 
    		 	  		}
    		 	  	}
    		 	}
    		 if(c.isTrapAdjacent)
    		 	{ trapMapValid = false; }
    		 chipsOnBoard[player]++;
     		}
    	c.addChip(ch);

    }
    private ArimaaChip removeChip(ArimaaCell c)
    {	G.Assert(c.height()>=1,"not at the bottom");
    	ArimaaChip ch = c.removeTop();
    	if(c.onBoard)
    		{int player = playerIndex(ch);
    		 if(ch.isRabbit()) { rabbitsOnBoard[player]--; }
    		 if(c.isTrapAdjacent) 
    		 	{ trapMapValid = false; }
    		 chipsOnBoard[player]--;
    		}
    	return(ch);
    }

    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	ArimaaChip po = pickedObject;
    	if(po!=null)
    	{
    	ArimaaCell ps = pickedSourceStack[stackIndex];
    	if(ps.onBoard) {
    		ArimaaCell target = ps.auxDisplay.lastEmptyMoveNumber==moveNumber ? ps.auxDisplay : ps;
    		target.lastEmptied = previousEmptied.pop();
    		target.lastEmptiedPlayer = previousPlayer.pop();
    		target.lastEmptyMoveNumber = -1;
    	}
    	pickedSourceStack[stackIndex]=null;
    	pickedObject = null;
    	addChip(ps,po);
    	}
     }
    // 
    // drop the floating object.
    //
    
    private ArimaaCell dropObject(ArimaaCell to)
    {
    	droppedDestStack[stackIndex] = to;
    	captureStack[stackIndex]=null;
    	captureStack2[stackIndex]=null;
    	stackIndex++;
    	destStack.push(to);
    	addChip(to,pickedObject);
    	pickedObject = null;
    	ArimaaCell target = to;
    	ArimaaCell ad = to.auxDisplay;
     	if(ad!=null) { ad.lastPlaceMoveNumber=-1; }
    	previousPlaced.push(target.lastPlaced);
     	target.lastPlaced = placementIndex;
    	target.lastPlaceMoveNumber = moveNumber;
    	placementIndex++;
    	return(to);
    }
    private ArimaaCell dropObject(ArimaaId dest, char col, int row)
    {  G.Assert((pickedObject!=null)&&(droppedDestStack[stackIndex]==null),"ready to drop");
       switch (dest)
        {
        default: throw G.Error("Not expecting dest %s",dest);
        case BoardLocation: // an already filled board slot.
        	return(dropObject(getCell(col,row)));
  
        case WH:
        	return dropObject(handicap[playerColor[FIRST_PLAYER_INDEX]]);

        case BH:
        	return dropObject(handicap[playerColor[SECOND_PLAYER_INDEX]]);
        	
        case W:		// back in the pool
        	return(dropObject(rack[playerColor[FIRST_PLAYER_INDEX]][row]));
        case B:		// back in the pool
        	return(dropObject(rack[playerColor[SECOND_PLAYER_INDEX]][row]));
        }
     }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(ArimaaCell cell)
    {	return(stackIndex>0 && (droppedDestStack[stackIndex-1]==cell));
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    {	ArimaaChip c = pickedObject;
    	if(c!=null)
    		{ return(c.chipNumber());
    		}
        return (NothingMoving);
    }
    
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private ArimaaCell pickObject(ArimaaCell from)
    {   G.Assert((pickedObject==null)&&(pickedSourceStack[stackIndex]==null),"ready to pick");
    	pickedSourceStack[stackIndex] = from;
    	pickedObject = removeChip(from);
    	if(from.onBoard) {
    		ArimaaCell target = from;
    		if(from.lastEmptyMoveNumber==moveNumber) { target = from.auxDisplay; }
    		else { from.auxDisplay.lastEmptyMoveNumber = -1; }
    		previousEmptied.push(target.lastEmptied);
    		previousPlayer.push(target.lastEmptiedPlayer);
    		target.lastEmptiedPlayer = whoseTurn;
    		target.lastEmptied = placementIndex;
    		target.lastContents = pickedObject;
    		target.lastEmptyMoveNumber = moveNumber;
    	}
    	return(from);
    }
    private ArimaaCell getCell(ArimaaId source,char col,int row)
    {
        switch (source)
        {
        default:
            throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	return(getCell(col,row));
        case WH:
        	return(handicap[playerColor[FIRST_PLAYER_INDEX]]);
        case BH:
        	return(handicap[playerColor[SECOND_PLAYER_INDEX]]);
        case W:
        	return(rack[playerColor[FIRST_PLAYER_INDEX]][row]);
        case B:
        	return(rack[playerColor[SECOND_PLAYER_INDEX]][row]);
     	}
    }
    private ArimaaCell pickObject(ArimaaId source, char col, int row)
    {	
    	return(pickObject(getCell(source,col,row)));
   }
    public ArimaaChip getPieceCaptured(ArimaaCell c)
    {
    	if(c==capturedLocation) { return(capturedPiece); }
    	if(c==capturedLocation2) { return(capturedPiece2); }
    	return(null);
    }
    private boolean checkCaptured(replayMode replay)
    {	boolean cap = false;
    	for(int lim=trapCells.length-1; lim>=0; lim--)
    	{	ArimaaCell dropped = trapCells[lim];
    		if(isDead(dropped)) 
    		{
    		if(cap)
    		{
    		// in rare circumstances, 2 pieces can be captured from one move
    		capturedLocation2 = capturedCellStack2[stackIndex-1]=dropped;
    		capturedPiece2 = captureStack2[stackIndex-1]=removeChip(dropped);
    		ArimaaCell rackCell = rack[playerIndex(capturedPiece2)][capturedPiece2.chipType()];
    		rackCell.addChip(capturedPiece2);
    		captured++;
    		if(replay!=replayMode.Replay)
    			{
    			animationStack.push(dropped);
    			animationStack.push(rackCell);
    			}
    		}
    		else 
    		{capturedLocation = capturedCellStack[stackIndex-1]=dropped;
    		capturedPiece = captureStack[stackIndex-1]=removeChip(dropped);
    		ArimaaCell rackCell = rack[playerIndex(capturedPiece)][capturedPiece.chipType()];
    		rackCell.addChip(capturedPiece);
    		if(replay!=replayMode.Replay)
			{
			animationStack.push(dropped);
			animationStack.push(rackCell);
			}
    		captured++;
    		lastAdvancementMoveNumber = moveNumber;
    		cap = true;
    		}
    		}
		}
    	return(cap);
    }
    private void undoCapture()
    {	if(capturedPiece2 !=null)
    	{
    	rack[playerIndex(capturedPiece2)][capturedPiece2.chipType()].removeTop();
		captured--;
		addChip(capturedLocation2,capturedPiece2);
    	}
		if(capturedPiece !=null)
		{	rack[playerIndex(capturedPiece)][capturedPiece.chipType()].removeTop();
			captured--;
			addChip(capturedLocation,capturedPiece);
		}
   }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(ArimaaMovespec mm,replayMode replay)
    {	int op = mm.op;
    	ArimaaCell pushSrc = pushPullSource;
       	pushPullSource = null;
    	pushPullDest = null;
    	capturedPiece = null;
    	capturedLocation = null;
    	capturedPiece2 = null;
    	capturedLocation2 = null;
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case CONFIRM_STATE:
        case DRAW_STATE:
        case ILLEGAL_MOVE_STATE:
        	setNextStateAfterDone();
        	break;
        case PUSHPULL_STATE:
        	{
        	ArimaaCell dropped = getDest();
        	if(dropped!=pushSrc) 
    			{ 
        		// we actually started a push rather than finished a pull
        		setState(ArimaaState.PUSH_STATE);  
            	pushPullDest = dropped;
            	pushPullSource = getSource();
            	playStep++;
            	mm.captures = checkCaptured(replay);
            	break;
    			}
        	}
        	// otherwise fall into pull state
        	/*$FALL-THROUGH$*/
        case PULL_STATE:
        case PUSH_STATE:
        	{
        	playStep++;
           	if(playStep>=STEPS_PER_TURN) 
    			{setState(ArimaaState.CONFIRM_STATE);
    			}
        	else setState(ArimaaState.PLAY_STATE);
        	mm.captures = checkCaptured(replay);
        	}
        	break;
        case PLAY_STATE:
        	playStep++;
        	{
        	ArimaaCell dropped = getDest();
        	ArimaaChip top = dropped.topChip();
        	pushPullDest = dropped;
        	pushPullSource = getSource();

        	if((playerIndex(top)!=whoseTurn)&&(op!=MOVE_FINISH_PULL)) 
        		{ setState(ArimaaState.PUSH_STATE);  
        		}
        	else if(playStep>=STEPS_PER_TURN) 
        		{setState(ArimaaState.CONFIRM_STATE);
        		 pushPullSource = null;
        		 pushPullDest = null;
        		}
        	mm.captures = checkCaptured(replay);  
        	}
			break;
        case INITIAL_SETUP_STATE:
        	acceptPlacement();
        	if ((chipsOnBoard[whoseTurn]+handicap[whoseTurn].height())==variation.numberOfPieces)
        		{ setState(ArimaaState.CONFIRM_STATE); }
        	break;
        case PUZZLE_STATE:
			acceptPlacement();
            break;
        }
    }

    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterPick()
    {
        switch (board_state)
        {
        default:
            throw G.Error("Not expecting pick in state %s", board_state);
        case RESIGN_STATE:
        case CONFIRM_STATE:
        case DRAW_STATE:
        case ILLEGAL_MOVE_STATE:
        	setState(started?ArimaaState.PLAY_STATE:ArimaaState.INITIAL_SETUP_STATE);
        	break;
        case PLAY_STATE:
        case INITIAL_SETUP_STATE:
			break;
        case PUZZLE_STATE:
            break;
        }
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case GAMEOVER_STATE: 
    		break;
        case ILLEGAL_MOVE_STATE:
        	 setGameOver(true,false);
        	 playStep = 0;
        	 break;
        case DRAW_STATE:
        	setGameOver(false,false);
        	playStep = 0;
        	break;

        case CONFIRM_STATE:
    	case PLAY_STATE:
        	if(started || ((chipsOnBoard[whoseTurn]+handicap[whoseTurn].height())==variation.numberOfPieces)) 
    		{ setState(ArimaaState.PLAY_STATE);
    		started = true;
    		}
        	else {
        		acceptPlacement();
        		setState(ArimaaState.INITIAL_SETUP_STATE);
        	}
    		playStep = 0;
    		break;
        case PUZZLE_STATE:
        case INITIAL_SETUP_STATE:
        	if(started || ((rabbitsOnBoard[whoseTurn]>0)&&(rabbitsOnBoard[nextPlayer[whoseTurn]]>0)))
        		{ setState(ArimaaState.PLAY_STATE);
        		  started = true;
        		  playStep = 0;
        		}
        	else if ((chipsOnBoard[whoseTurn]+handicap[whoseTurn].height())==variation.numberOfPieces) 
        		{ setState(ArimaaState.CONFIRM_STATE); moveNumber=whoseTurn+2;
        		}
        	else { setState(ArimaaState.INITIAL_SETUP_STATE); }
        	break;
     	}

    }
   
   
    private boolean doDone()
    {	boolean captures = acceptPlacement();
        recentDigest = nextRecentDigest;
        placementIndex++;
        if (board_state==ArimaaState.RESIGN_STATE)
        {	setGameOver(false,true);
        }
        else
        {	boolean win1 = WinForPlayerNow(whoseTurn);
        	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
        	if(win1 || win2)  { setGameOver(win1,win2); }
        	else {setNextPlayer(); setNextStateAfterDone(); }
        }
        return(captures);
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	ArimaaMovespec m = (ArimaaMovespec)mm;
    	// G.print("M "+m);
        if(replay!=replayMode.Replay) { animationStack.clear(); }
        switch (m.op)
        {
        case MOVE_PASS:
        case MOVE_NULL:
        	setState(board_state!=ArimaaState.CONFIRM_STATE?ArimaaState.CONFIRM_STATE:ArimaaState.PLAY_STATE);
        	break;
        case MOVE_DONE:
         	doDone();
         	checkForNoMoves();
         	playStep = 0;
            capturedLocation = null;
            capturedPiece = null;
            capturedLocation2 = null;
            capturedPiece2 = null;
           checked = false;
            break;
        case MOVE_PLACE_RABBITS:
        	{
        	ArimaaCell rab = rack[whoseTurn][ArimaaChip.RABBIT_INDEX];
        	for(int idx=0,row=(whoseTurn==playerColor[FIRST_PLAYER_INDEX])?variation.nRows-1:1;  idx<=1; row++,idx++)
        		{
        		for(char col='A',lastCol=(char)('A'+variation.nCols); col<lastCol; col++)
        			{
        			ArimaaCell c = getCell(col,row);
        			if((c.height()==0) && (rab.height()>0))
        				{
        				pickObject(rab);
        				dropObject(c);

        				if(replay!=replayMode.Replay)
        					{
        					animationStack.push(rab);
        					animationStack.push(c);
        					}
        				}
        			}
        		}
        	setState(ArimaaState.CONFIRM_STATE);
        	}
        	break;
        case MOVE_RACK_BOARD:
           	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
 
        		case INITIAL_SETUP_STATE:
        			G.Assert(pickedObject==null,"something is moving");
        			ArimaaCell src = getCell(m.source, m.from_col, m.from_row);
                    pickObject(src);
                    m.picked = pickedObject;
                    ArimaaCell dest = getCell(ArimaaId.BoardLocation,m.to_col,m.to_row);
                    if(replay!=replayMode.Replay)
                    {
                    	animationStack.push(src);
                    	animationStack.push(dest);
                    }
                    dropObject(dest); 
                    setNextStateAfterDrop(m,replay);
                    break;
        	}
        	break;
        case MOVE_PULL:
    		{
        	ArimaaCell dest = getCell(m.to_col,m.to_row);
        	ArimaaCell src = getCell(m.from_col,m.from_row);
        	ArimaaCell third = src.exitTo(m.pushPullDirection);
        	// a regular step
            if(replay!=replayMode.Replay)
            {
             	animationStack.push(src);
            	animationStack.push(dest);
            	animationStack.push(third);
            	animationStack.push(src);
            }
			pickObject(src);
			m.picked = pickedObject;
			dropObject(dest); 
			// and tug the victim along
			pickObject(third);
			m.victim = pickedObject;
			dropObject(src);
        	playStep++;
        	setState(ArimaaState.PULL_STATE);
		    setNextStateAfterDrop(m,replay);
        	}
    		break;
        case MOVE_PUSH:
        	{
        	ArimaaCell dest = getCell(m.to_col,m.to_row);
        	ArimaaCell src = getCell(m.from_col,m.from_row);
        	ArimaaCell third = dest.exitTo(m.pushPullDirection);
        	// move the victim
            if(replay!=replayMode.Replay)
            {
            	animationStack.push(dest);
            	animationStack.push(third);
             	animationStack.push(src);
            	animationStack.push(dest);
            }
        	pickObject(dest);
        	m.victim = pickedObject;
        	dropObject(third);
        	playStep++;
        	// now become a regular step
			pickObject(src);
			m.picked = pickedObject;
			dropObject(dest); 
        	setState(ArimaaState.PUSH_STATE);
		    setNextStateAfterDrop(m,replay);
        	}
        	break;
        	
        case MOVE_FINISH_PULL:
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case INITIAL_SETUP_STATE:
        		case PLAY_STATE:
        			G.Assert(pickedObject==null,"something is moving");
        			ArimaaCell src = getCell(ArimaaId.BoardLocation, m.from_col, m.from_row);
        			pickObject(src);
        			m.picked = pickedObject;
         			ArimaaCell dest = getCell(ArimaaId.BoardLocation,m.to_col,m.to_row);
        			dropObject(dest); 
                    if(replay!=replayMode.Replay)
                    {
                     	animationStack.push(src);
                    	animationStack.push(dest);
                    }
 				    setNextStateAfterDrop(m,replay);
        			break;
        	}
        	break;
        case MOVE_DROPB:
        	G.Assert(pickedObject!=null,"something is moving");
        	m.picked = pickedObject;
        	ArimaaCell dest = getCell(ArimaaId.BoardLocation, m.to_col, m.to_row);
            dropObject(dest);
            if(pickedSourceStack[stackIndex-1]==droppedDestStack[stackIndex-1]) 
            	{ //remove this move - just a stutter
            	  unDropObject(); 
            	  unPickObject(); 
            	} 
            	else
            		{
                	ArimaaCell src = getSource();
                	if(replay==replayMode.Single)
                	{
                		animationStack.push(src);
                		animationStack.push(dest);
                	}
                	boolean isPull = (board_state==ArimaaState.PULL_STATE);
                	setNextStateAfterDrop(m,replay);
				    if((board_state==ArimaaState.PUSH_STATE)|| isPull)
 				    {	m.victim = m.picked;
 				    	if(isPull) { m.picked = null; }	// flag for the reviewer
 				    	m.pushPullDirection = findDirection(dest.col,dest.row,src.col,src.row);
 				    }
           		}

            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
      		{
      		  switch(board_state)
       		  {		case ILLEGAL_MOVE_STATE:
       		  		case CONFIRM_STATE:
       		  			if(started)
       		  				{
       		  				unDropObject();
      		  				setState(ArimaaState.PLAY_STATE);
       		  				playStep--;
       		  				}
       		  			else { pickObject(ArimaaId.BoardLocation,m.from_col,m.from_row);
       		  				setNextStateAfterPick();
       		  			}
       		  			break;
         		  	case PLAY_STATE:
        		  		{
        		      		ArimaaCell src = pickObject(ArimaaId.BoardLocation,m.from_col,m.from_row);
        		      		m.picked = pickedObject;
        		      		if((playerIndex(pickedObject)!=whoseTurn)
        		  				&& isPullSource(src,pickedObject,pushPullSource,pushPullDest))
        		  			{
        		      		if(canBePushed(src,pickedObject,playStep))
        		      			{
        		      			setState(ArimaaState.PUSHPULL_STATE);
        		      			}
        		      			else
        		      			{
        		      			setState(ArimaaState.PULL_STATE);
        		      			}
        		      		}
        		  				
        		  		}
        		  		break;
        		  	default: 
        		  		if(started) { throw G.Error("Not expecting pickb in state %s",board_state); }
        		  		// fall through initial initial setup state handler
        		  		/*$FALL-THROUGH$*/
        		  	case PUSH_STATE:
        		  	case INITIAL_SETUP_STATE:
        		  		// if we pick a piece off the board, we might expose a win for the other player
        		  		// and otherwise, we are comitted to moving the piece
       	      		  	pickObject(ArimaaId.BoardLocation,m.from_col,m.from_row);
       	      		  	m.picked = pickedObject;
         		  		break;
        		  	case PUZZLE_STATE:
       	      		    pickObject(ArimaaId.BoardLocation,m.from_col,m.from_row);
       	      		    m.picked = pickedObject;
        		  		break;
        		  }
         		}
 
            break;

        case MOVE_DROP: // drop on chip pool;
        	m.picked = pickedObject;
            dropObject(m.source, m.to_col, m.to_row);
            setNextStateAfterDrop(m,replay);

            break;

        case MOVE_PICK:
            pickObject(m.source, m.from_col, m.from_row);
            m.picked = pickedObject;
            setNextStateAfterPick();
            break;


        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(ArimaaState.PUZZLE_STATE);
             {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;

        case MOVE_RESIGN:
            setState((unresign==null)?ArimaaState.RESIGN_STATE:unresign);
            break;

        case MOVE_EDIT:
    		acceptPlacement();
    		playStep = 0;
             // standardize "gameover" is not true
            setState(ArimaaState.PUZZLE_STATE);
 
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
    public boolean LegalToHitChips(int player)
    {
        switch (board_state)
        {

        default:
        case CONFIRM_STATE:
            if(started) { return(false);  }
        	/*$FALL-THROUGH$*/
         case DRAW_STATE:
         case ILLEGAL_MOVE_STATE:
         case INITIAL_SETUP_STATE:
        	return((pickedObject==null)
        			?(player==whoseTurn)
        			:(player==playerIndex(pickedObject)));


        case PLAY_STATE: 
        case PUSH_STATE:
        case PULL_STATE:
		case GAMEOVER_STATE:
			return(false);
        case PUZZLE_STATE:
        	return((pickedObject==null)?true:(player==playerIndex(pickedObject)));
        }
    }

    public boolean legalToHitBoard(ArimaaCell cell)
    {	
        switch (board_state)
        {
        case CONFIRM_STATE:
        	if(started) { return(isDest(cell)); }
        	/*$FALL-THROUGH$*/
        case INITIAL_SETUP_STATE:
        	if(whoseTurn==playerColor[FIRST_PLAYER_INDEX])
        	{	if(cell.row<=(variation.nRows-2)) { return(false); }
        	}
        	else
        	{	if(cell.row>2) { return(false); }
        	}
        	/*$FALL-THROUGH$*/
        case PUSH_STATE:
        case PULL_STATE:
 		case PLAY_STATE:
 		case PUSHPULL_STATE:
 			if(pickedObject==null) { return(cell.height()==1); }
 			return(cell.height()==0);
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
		case DRAW_STATE:
		case ILLEGAL_MOVE_STATE:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case PUZZLE_STATE:
        	return((pickedObject==null) == (cell.topChip()!=null));
        }
    }
  public boolean canDropOn(ArimaaCell cell)
  {	ArimaaCell src = pickedSourceStack[stackIndex];
  	return((pickedObject!=null)				// something moving
  			&&(src.onBoard 
  					? (cell!=src)	// dropping on the board, must be to a different cell 
  					: (cell==src))	// dropping in the rack, must be to the same cell
  				);
  }
 
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public int RobotExecute(ArimaaMovespec m,boolean digest)
    {	int num=0;
    	robotDepth++;
    	robotState.push(board_state);
        m.undoInfo = 0; //record the starting state. The most reliable
        m.undoInfo = (m.undoInfo<<1) | (started?1:0);
        m.undoInfo = (m.undoInfo<<3)  | playStep;
        robotOStack.push(pushPullSource);
        robotOStack.push(pushPullDest);
        robotOStack.push(capturedLocation);
        robotPStack.push(capturedPiece);
        robotOStack.push(capturedLocation2);
        robotPStack.push(capturedPiece2);
        m.digest = recentDigest;
        
        lastRobotMove = null;
        //G.print("R "+m);
        // to undo state transistions is to simple put the original state back.
        
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        if (Execute(m,replayMode.Replay))
        {

            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {
                if(digest && (m.op!=MOVE_NULL))	//exempt nullmove from repetition checks 
                {	long dig = Digest();
                	long sub = SubDigest();
                	num = repeatedPositions.addToRepeatedPositions(dig,m);
                	if(sub == recentDigest)
                	{	setState(ArimaaState.RESIGN_STATE);
                		num = 100;		// walk in a circle, strictly forbidden
                		
                	}
                } 
            	doDone();
            }
            else 
            {	switch(m.op)
            	{
            	case MOVE_PULL:
            	case MOVE_PUSH:
            		lastRobotMove = m;
            		break;
            	case MOVE_FINISH_PULL: break;
            	case MOVE_BOARD_BOARD: 
            		lastRobotMove = m;
            		break;
               	case MOVE_RACK_BOARD: break;
            	default:
            		throw G.Error("Robot move should be in a done state");
            }
            }}
        return(num);
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(ArimaaMovespec m)
    {
        //G.print("U "+m+" for "+whoseTurn);
    	lastRobotMove = null;
    	robotDepth--;
        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;
 
   	    case MOVE_PASS:
   	    case MOVE_NULL:
        case MOVE_DONE:
            break;
        case MOVE_RACK_BOARD:
   			G.Assert((pickedObject==null),"something is moving");
   			pickObject(ArimaaId.BoardLocation,m.to_col,m.to_row);
   			dropObject(m.source,m.from_col, m.from_row);
   			destStack.pop();
   			destStack.pop();
		    acceptPlacement();
        	break;
        	
	        	
	        case MOVE_PULL:
				{
       			undoCapture();
		    	ArimaaCell dest = getCell(m.to_col,m.to_row);
		    	ArimaaCell src = getCell(m.from_col,m.from_row);
		    	ArimaaCell third = src.exitTo(m.pushPullDirection);
				ArimaaChip removed = removeChip(src);
		    	addChip(third,removed);
		    	
		    	// now become a regular step
				pickObject(dest);
				dropObject(src); 
				destStack.pop();
				destStack.pop();
				destStack.pop();
				acceptPlacement();
		    	}
				break;
		     case MOVE_PUSH:
			    	{
        			undoCapture();
			    	ArimaaCell dest = getCell(m.to_col,m.to_row);
			    	ArimaaCell src = getCell(m.from_col,m.from_row);
			    	ArimaaCell third = dest.exitTo(m.pushPullDirection);
			    	// undo a regular step
					pickObject(dest);
					dropObject(src); 
					destStack.pop();
					destStack.pop();
					destStack.pop();
			    	ArimaaChip removed = removeChip(third);
			    	addChip(dest,removed);
			    	acceptPlacement();
			    	}
			    break;
        case MOVE_FINISH_PULL:
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
        		case PULL_STATE:
        		case PLAY_STATE:
        			undoCapture();
        			pickObject(ArimaaId.BoardLocation, m.to_col, m.to_row);
       			    dropObject(ArimaaId.BoardLocation, m.from_col,m.from_row); 
					destStack.pop();
					destStack.pop();
       			    acceptPlacement();
        			break;
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        capturedPiece2 = robotPStack.pop();
        capturedLocation2 = robotOStack.pop();
        capturedPiece = robotPStack.pop();
        capturedLocation = robotOStack.pop();
        pushPullDest = robotOStack.pop();
        pushPullSource = robotOStack.pop();
        recentDigest = m.digest;
        int undo = m.undoInfo;
        playStep = undo&0x7;
        undo = undo>>3;
        started = (undo&1)!=0;
        undo=undo>>1;
        setState(robotState.pop());
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
    
    
    // place a rack piece in each empty cell in s row
   private commonMove placeInRandomRow(Random rand,ArimaaCell from,int row,int who)
    {int randomOffset = Random.nextSmallInt(rand,variation.nCols);
   	 for(int col0=0; col0<variation.nCols; col0++)
    	{
   		 int col1 = col0 + randomOffset;
   		 if(col1>=variation.nCols) { col1 -= variation.nCols; }
   		 char col = (char)('A'+col1);
   		 ArimaaCell c = getCell(col,row);
   		 if(c.height()==0)
   		 	{	return(new ArimaaMovespec(MOVE_RACK_BOARD,RackLocation[playerColor[who]],
   		 					from.col,from.row,c.col,c.row,who));
   		 	}
    	}
   	 	return(null);
    }
 
 // place a rack piece in each empty cell in s row
 private void placeInRows(CommonMoveStack  all,ArimaaCell from,int row,int who)
 {	
	 for(char col='A'; col<('A'+variation.nCols); col++)
 		{
		 ArimaaCell c = getCell(col,row);
		 if(c.height()==0)
		 	{	all.push(new ArimaaMovespec(MOVE_RACK_BOARD,RackLocation[playerColor[who]],
		 					from.col,from.row,c.col,c.row,who));
		 	}
 		}
 }
 
 // place a rack piece in the first empty cell in two rows.
 // this is used to place the rabbits.
 private commonMove placeInRandomFirstRow(Random rand,ArimaaCell from,int torow,int who)
 {	int randomRow = Random.nextSmallInt(rand,2);
 	for(int row0=torow; row0<=torow+1; row0++) 
	{	int row = row0 + randomRow;
		if(row>=torow+2) { row -= 2; };
		int randomCol = Random.nextSmallInt(rand,variation.nCols);
 		for(char col0 = 0;col0<variation.nCols; col0++)
  		{int col1 = col0 + randomCol;
  		 if(col1>=variation.nCols) { col1 -= variation.nCols; }
  		 char col = (char)('A'+col1);
		 ArimaaCell c = getCell(col,row);
		 if(c.height()==0)
		 	{	return(new ArimaaMovespec(MOVE_RACK_BOARD,RackLocation[playerColor[who]],
		 					from.col,from.row,c.col,c.row,who));
		 	}
 		}
	 }
 	return(null);
 }
 
 // place a rack piece in the first empty cell in two rows.
 // this is used to place the rabbits.
 private void placeInFirstRow(CommonMoveStack  all,ArimaaCell from,int torow,int who)
 {	for(int row=torow; row<=torow+1; row++)
	 {for(char col='A'; col<('A'+variation.nCols); col++)
 		{
		 ArimaaCell c = getCell(col,row);
		 if(c.height()==0)
		 	{	all.addElement(new ArimaaMovespec(MOVE_RACK_BOARD,RackLocation[playerColor[who]],
		 					from.col,from.row,c.col,c.row,who));
		 		return;
		 	}
 		}
	 }
 }
 
 static final int FROZEN_CODE_FROZEN = 1;	// is frozen
 static final int FROZEN_CODE_PULL = 2;		// can push or pull
 static final int FROZEN_CODE_FRIENDS = 4;	// has friends
 
 private int getFrozenCode(ArimaaCell from,ArimaaChip ch)
 {	int rc = 0;
 	int bigFriend = 0;
	int bigEnemy = 0;
	int myPower = ch.chipPower();
	int myPlayer = playerIndex(ch);
	int otherPlayer = nextPlayer[myPlayer];
 	for(int dir=0;dir<CELL_FULL_TURN;dir++)
 	{	ArimaaCell adj = from.exitTo(dir);
 		if(adj!=null)
 		{
 			ArimaaChip aTop = adj.topChip();
 			if(aTop!=null)
 			{
 			int aPlayer = playerIndex(aTop);
 			if(aPlayer==myPlayer) { bigFriend=Math.max(bigFriend,aTop.chipPower()); }
 			else if(aPlayer==otherPlayer) { bigEnemy=Math.max(bigEnemy,aTop.chipPower()); }
 			}
 		}
 	}
 	if(bigEnemy>0) { rc |= FROZEN_CODE_PULL; } 			// can push or pull (if not frozen)
 	if(bigFriend>0) { rc |= FROZEN_CODE_FRIENDS; }		// has friends (can't fall into traps or be frozen)
 	else if(myPower<bigEnemy) { rc |= FROZEN_CODE_FROZEN; }	// is actually frozen
 	
 	return(rc);
 }
 public boolean isDead(ArimaaCell c)
 {	
	 if(c.isTrap)
	 {	 ArimaaChip top = c.topChip();
		 if(top!=null)
			 {int fc = getFrozenCode(c,top);
			 return((fc&FROZEN_CODE_FRIENDS)==0);
			 }
	 }
	 return(false);
 }
 private boolean canBePushed(ArimaaCell src,ArimaaChip piece,int step)
 {
	 boolean canPushPull = ((step+1)<STEPS_PER_TURN);	// push/pull take two steps
	 if(canPushPull)
	 {	int myPower = piece.chipPower();
	 	int myPlayer = playerIndex(piece);
		 // look for push moves
		for(int pushd = 0; pushd<CELL_FULL_TURN; pushd++)
		{	ArimaaCell exitCell = src.exitTo(pushd);
			if(exitCell!=null) 
			{	ArimaaChip top = exitCell.topChip();	
				if((top!=null) 
						&& (playerIndex(top)!=myPlayer) 
						&& (top.chipPower()>myPower))
				{	// the adjacent is a piece that could push, if it's not frozen
					int code = getFrozenCode(exitCell,top);
					if((code&FROZEN_CODE_FROZEN)==0) { return(true); }
				}
				
			}
		}		 
	 }
	 return(false);
 }
 
 //this checks for moves which push or pull the opponent's rabit to the goal line.  There's
 // never any reason to do this.
 private boolean isRabbitSuicideMove(ArimaaChip top,ArimaaCell dest)
 {
	if(top.isRabbit())
	{
		int pl = playerIndex(top);
		int home = (pl==FIRST_PLAYER_INDEX)?1:8;
		return(dest.row==home);
	}
	return(false);
 }
 //
 // this checks for moves which unfreeze or unblock the opponent's rabits.
 // this is sometimes ok, but in the notorious "elephant suicide" position
 // an advanced rabbit is allowed to run home in the random playout phase,
 // largely due to your own pieces just walking away and leaving the route
 // clear.
 //
 private boolean isUnblockRabbitMove(ArimaaCell from,int player)
 {	
	 for(int dir = CELL_FULL_TURN-1; dir>=0; dir--)
	 {
		 ArimaaCell adj = from.exitTo(dir);
		 if(adj!=null)
			 {ArimaaChip top = adj.topChip();
			 if(top!=null && playerIndex(top)!=player && top.isRabbit()) 
			 	{ return(true); }
			 }
	 }
	 return(false);
 }
 private boolean isSuicideMove(ArimaaCell to,int player)
 {	 if(!to.isTrap || !robotGame) { return(false); }
	 int count = 0;
	 for(int dir = CELL_FULL_TURN-1; dir>=0 && count<2; dir--)
	 {
		 ArimaaCell adj = to.exitTo(dir);
		 if(adj!=null)
			 {ArimaaChip top = adj.topChip();
			 if(top!=null && playerIndex(top)==player) 
			 	{ count++; 
			 	}
			 }
	 }
	 return(count<2);
 }
 private void placeInSteps(CommonMoveStack  all,ArimaaCell from,ArimaaChip ch,int step,ArimaaMovespec lastMove)
 {	int fcode = getFrozenCode(from,ch);
 	if((fcode&FROZEN_CODE_FROZEN)==0)
 	{
 	int myPlayer = playerIndex(ch);
 	int myPower = ch.chipPower();
 	int otherPlayer = nextPlayer[myPlayer];
 	int backward_direction = myPlayer==playerColor[FIRST_PLAYER_INDEX] ? CELL_UP : CELL_DOWN;
 	boolean canPushPull = ((step+1)<STEPS_PER_TURN);	// push/pull take two steps
 	{
 	for(int dir=0; dir<CELL_FULL_TURN; dir++)
 		{	if((myPower!=ArimaaChip.RABBIT_POWER) || (dir!=backward_direction))
 			{
 			ArimaaCell adj = from.exitTo(dir);
 			if(adj!=null)
 			{
 			ArimaaChip aTop=adj.topChip();
 			if(aTop!=null)
 			{
 			int aPlayer = playerIndex(aTop);
 			if( canPushPull && (aPlayer==otherPlayer)&&(aTop.chipPower()<myPower))
 			{	// push moves
 				for(int pushd = 0; pushd<CELL_FULL_TURN; pushd++)
 				{	ArimaaCell exitCell = adj.exitTo(pushd);
 					if((exitCell!=null) && (exitCell.height()==0))
 					{	if(!isRabbitSuicideMove(aTop,exitCell)  && !isSuicideMove(adj,myPlayer))
 						{
  						all.addElement(new ArimaaMovespec(MOVE_PUSH,from.col,from.row,adj.col,adj.row,pushd,myPlayer));
 						}
 					}
 				}
 			}
 			else if(aPlayer==myPlayer) {}	// our friend
 			}
 			else {
 				// move and possibly pull
 				if((lastMove==null)
 						// test for simple move to-back which is a noop
 						|| (lastMove.op!=MOVE_BOARD_BOARD)
 						|| (lastMove.from_col!=adj.col)
 						|| (lastMove.from_row!=adj.row)
 						|| (lastMove.to_col!=from.col)
 						|| (lastMove.to_row!=from.row))
 					{ if(!isSuicideMove(adj,myPlayer))
 						{
 						all.addElement(new ArimaaMovespec(MOVE_BOARD_BOARD,from.col,from.row,adj.col,adj.row,myPlayer));
 						}
 					}
 				if(canPushPull && ((fcode&FROZEN_CODE_PULL)!=0))
 				{
 	 				for(int pulld = 0; pulld<CELL_FULL_TURN; pulld++)
 	 				{	ArimaaCell exitCell = from.exitTo(pulld);
 	 					if(exitCell!=null)
 	 					{
 	 					ArimaaChip exTop = exitCell.topChip();
 	 					if((exTop!=null) && (playerIndex(exTop)==otherPlayer)&&(exTop.chipPower()<myPower)) 
 	 					{	if(!isRabbitSuicideMove(exTop,from) && !isSuicideMove(adj,myPlayer))
 	 						{
 	 						all.addElement(new ArimaaMovespec(MOVE_PULL,from.col,from.row,adj.col,adj.row,pulld,myPlayer));
 	 						}
 	 					}
 	 					}
 	 				}
					
 				}
 			}}
 		}
 	}}
 	}	 
 }

 private int push_percentage = 30;			// percentage of push moves to try
 private int pull_percentage = 30;
 
 private int dirPerm[] = AR.intArray(4);
 private int pushPerm[] = AR.intArray(4);
 private int pullPerm[] = AR.intArray(4);
 private commonMove placeInRandomSteps(Random rand,ArimaaCell from,ArimaaChip ch,int step,ArimaaMovespec lastMove)
 {	int fcode = getFrozenCode(from,ch);
 	if((fcode&FROZEN_CODE_FROZEN)==0)
 	{
 	int myPlayer = playerIndex(ch);
 	int myPower = ch.chipPower();
 	int otherPlayer = nextPlayer[myPlayer];
 	int backward_direction = myPlayer==FIRST_PLAYER_INDEX ? CELL_UP : CELL_DOWN;
 	boolean canPushPull = ((step+1)<STEPS_PER_TURN);	// push/pull take two steps
 	int nDirs = CELL_FULL_TURN;
 	while(nDirs>0)
 		{	int dir0 = Random.nextInt(rand,nDirs--);
 			int dir = dirPerm[dir0];
 			dirPerm[dir0] = dirPerm[nDirs];
 			dirPerm[nDirs] = dir;
  			if((myPower!=ArimaaChip.RABBIT_POWER) || (dir!=backward_direction))
 			{
 			ArimaaCell adj = from.exitTo(dir);
 			if(adj!=null)
 			{
 			ArimaaChip aTop=adj.topChip();
 			if(aTop!=null)
 			{
 			int aPlayer = playerIndex(aTop);
 			if( canPushPull && (aPlayer==otherPlayer)&&(aTop.chipPower()<myPower) && (Random.nextSmallInt(rand,100)<push_percentage))	// 30% chance of 
 			{	// push moves
 				int nPush = CELL_FULL_TURN;
  				while(nPush>0)
 				{	
 					int pushd0 = Random.nextInt(rand,nPush--);
 					int pushd = pushPerm[pushd0];
 					pushPerm[pushd0] = pushPerm[nPush];
 					pushPerm[nPush] = pushd;
   					ArimaaCell exitCell = adj.exitTo(pushd);
 					if((exitCell!=null) && exitCell.height()==0) 
 					{	if(!isRabbitSuicideMove(aTop,exitCell)  && !isSuicideMove(adj,myPlayer))
 						{
  						return(new ArimaaMovespec(MOVE_PUSH,from.col,from.row,adj.col,adj.row,pushd,myPlayer));
 						}
 					}
 				}
 			}
 			else if(aPlayer==myPlayer) {}	// our friend
 			}
 			else {
 				// move and possibly pull 				
 				if(canPushPull && ((fcode&FROZEN_CODE_PULL)!=0) && (Random.nextSmallInt(rand,100)<pull_percentage))
 				{	int nPull = CELL_FULL_TURN;
  					while(nPull>0)
 	 				{	int pulld0 =Random.nextInt(rand,nPull--);
 	 					int pulld = pullPerm[pulld0];
 	 					pullPerm[pulld0] = pullPerm[nPull];
 	 					pullPerm[nPull] = pulld;
 						ArimaaCell exitCell = from.exitTo(pulld);
 	 					if(exitCell!=null)
 	 					{
 	 					ArimaaChip exTop = exitCell.topChip();
 	 					if((exTop!=null) && (playerIndex(exTop)==otherPlayer)&&(exTop.chipPower()<myPower)) 
 	 					{	if(!isRabbitSuicideMove(exTop,from) && !isSuicideMove(adj,myPlayer))
 	 						{
 	 						return(new ArimaaMovespec(MOVE_PULL,from.col,from.row,adj.col,adj.row,pulld,myPlayer));
 	 						}}
 	 					}
 	 				}
 				}
 				if((lastMove==null)
 						// test for simple move to-back which is a noop
 						|| (lastMove.op!=MOVE_BOARD_BOARD)
 						|| (lastMove.from_col!=adj.col)
 						|| (lastMove.from_row!=adj.row)
 						|| (lastMove.to_col!=from.col)
 						|| (lastMove.to_row!=from.row))
 					{
  					if(isSuicideMove(adj,myPlayer) 
 							|| (isUnblockRabbitMove(from,myPlayer) && Random.nextSmallInt(rand,10)<9) ) {}	// this eliminates 50% of these unblocking moves
 					else {
 						return(new ArimaaMovespec(MOVE_BOARD_BOARD,from.col,from.row,adj.col,adj.row,myPlayer));
 						}
 					}
 			}}
 		}
 	}
 	}	
 	return(null);
 }
 public ArimaaCell getSource()
 {
	if((stackIndex>=0)&&(pickedObject!=null))
		{ return(pickedSourceStack[stackIndex]);
		}
	else if(stackIndex>0) { return(pickedSourceStack[stackIndex-1]); }
	return(null);
 }
 
 public Hashtable<ArimaaCell,ArimaaCell> getDests()
 {	Hashtable<ArimaaCell,ArimaaCell> h = null;
	if(pickedObject!=null)
		{
		switch(board_state)
	 	{
		default: break;
		case PUSHPULL_STATE:
			// can push or pull
			{
				h = new Hashtable<ArimaaCell,ArimaaCell>();
				h.put(pushPullSource,pushPullSource);
				ArimaaCell src = getSource();
				for(int dir=0; dir<CELL_FULL_TURN; dir++)
		 		{	ArimaaCell ex = src.exitTo(dir);
		 			if((ex!=null)&&(ex.height()==0))
		 					{	h.put(ex,ex);
		 					}
		 		}
			}
			break;
		case PUSH_STATE:
		case PULL_STATE:
			{
			h = new Hashtable<ArimaaCell,ArimaaCell>();
			h.put(pushPullSource,pushPullSource);
			}
			break;
	 	case PLAY_STATE:
	 	{	ArimaaCell src = getSource();
	 		h = new Hashtable<ArimaaCell,ArimaaCell>();
	 		int chipPower = pickedObject.chipPower();
	 		int backward_dir = whoseTurn==playerColor[FIRST_PLAYER_INDEX] ? CELL_UP : CELL_DOWN;
	 		h.put(src,src);
	 		for(int dir=0; dir<CELL_FULL_TURN; dir++)
	 		{	if((chipPower!=ArimaaChip.RABBIT_POWER)||(dir!=backward_dir)||(whoseTurn!=playerIndex(pickedObject)))
	 			{ArimaaCell ex = src.exitTo(dir);
	 			if((ex!=null)&&(ex.height()==0))
	 					{	h.put(ex,ex);
	 					}}
	 		}
	 	}
	 	}
		}

	return(h); 	
	 
 }
 public Hashtable<ArimaaCell,ArimaaCell> getSources()
 {	Hashtable<ArimaaCell,ArimaaCell> h = null;
 	if(pickedObject==null)
 		{
 		switch(board_state)
	 	{
 		case PUSH_STATE:
 		case PULL_STATE:
	 	case PLAY_STATE:
	 	{
	 		CommonMoveStack all = new CommonMoveStack();
	 		getListOfMoves(all,whoseTurn,lastRobotMove,false);
	 		h = new Hashtable<ArimaaCell,ArimaaCell>();
	 		for(int i=all.size()-1;i>=0;i--)
	 		{
	 			ArimaaMovespec m = (ArimaaMovespec)all.elementAt(i);
	 			switch(m.op)
	 			{
	 			default: break;
	 			case MOVE_PUSH:
	 				if(board_state==ArimaaState.PUSH_STATE) { ArimaaCell c = getCell(m.from_col,m.from_row); h.put(c,c);}
	 				else { ArimaaCell c = getCell(m.to_col,m.to_row); h.put(c,c); }
	 				break;
	 			case MOVE_PULL:
	 			case MOVE_FINISH_PULL:
	 			case MOVE_BOARD_BOARD:	
	 				{ ArimaaCell c = getCell(m.from_col,m.from_row);
	 				  h.put(c,c);
	 				}
	 				break;
	 			
	 			}
	 			
	 		}
	 	}
	 		break;
		default:
			break;
	 	}
 		}
 
 	return(h);
	 
 }
 // we're definititely pushing something, get the moves that might be in progress
 private void getPushMoves(CommonMoveStack  all,int who,ArimaaCell src,ArimaaCell dest)
 {	ArimaaChip victim = (capturedLocation==dest)
	 						?capturedPiece
	 						: ((capturedLocation2==dest)?capturedPiece2:dest.topChip());
 	int victim_power = victim.chipPower();
	 // find pieces that could have done the push, these are adjacent, not frozen, and stronget
	 for(int dir=0;dir<CELL_FULL_TURN; dir++)
	 {	ArimaaCell adj = src.exitTo(dir);
	 	if((adj!=null) && (adj!=dest))
	 	{
	 		ArimaaChip top = adj.topChip();
	 		if((top!=null)
	 				&& (playerIndex(top)==who)
	 				&& (top.chipPower()>victim_power) 
	 				&& ((getFrozenCode(adj,top)&FROZEN_CODE_FROZEN)==0))
	 		{
	 		 all.addElement(new ArimaaMovespec(MOVE_PUSH,adj.col,adj.row,src.col,src.row,dir,who));
	 		}
	 	}
	 }
 }
 // we're definititely pushing something, get the moves that might be in progress
 private commonMove getRandomPushMove(Random rand,int who,ArimaaCell src,ArimaaCell dest)
 {	ArimaaChip victim = (capturedLocation==dest)
	 						?capturedPiece
	 						: ((capturedLocation2==dest)?capturedPiece2:dest.topChip());
 	int victim_power = victim.chipPower();
	 // find pieces that could have done the push, these are adjacent, not frozen, and stronget
	 for(int randomDir = Random.nextSmallInt(rand,CELL_FULL_TURN),dir0=0;
	 		dir0 < CELL_FULL_TURN;
	 		dir0++)
	 {	int dir = dir0 + randomDir;
	 	ArimaaCell adj = src.exitTo(dir);
	 	if((adj!=null) && (adj!=dest))
	 	{
	 		ArimaaChip top = adj.topChip();
	 		if((top!=null)
	 				&& (playerIndex(top)==who)
	 				&& (top.chipPower()>victim_power) 
	 				&& ((getFrozenCode(adj,top)&FROZEN_CODE_FROZEN)==0))
	 		{
	 		 return(new ArimaaMovespec(MOVE_PUSH,adj.col,adj.row,src.col,src.row,dir,who));
	 		}
	 	}
	 }
	 throw G.Error("no push move found");
 }
 // we can be pulling something, but can pull something if we want to.
 private void getPullMoves(CommonMoveStack  all,int who,ArimaaCell src,ArimaaCell dest)
 {	ArimaaChip puller = dest.topChip();
 	if((puller==null) && (capturedLocation==dest)) { puller = capturedPiece; }
 	if((puller==null) && (capturedLocation2==dest)) { puller = capturedPiece2; }
 	if((puller!=null) && (playerIndex(puller)==who))	// contrary to intuition, a piece that is falling into a trap can pull
 	{
 	int puller_power = puller.chipPower();
	 // find pieces that could have done the push, these are adjacent, not frozen, and stronger
	 for(int dir=0;dir<CELL_FULL_TURN; dir++)
	 {	ArimaaCell adj = src.exitTo(dir);
	 	if((adj!=null) && (adj!=dest))
	 	{
	 		ArimaaChip top = adj.topChip();
	 		if((top!=null)
	 				&& (playerIndex(top)!=who)
	 				&& (top.chipPower()<puller_power))
	 		{
	 		 all.addElement(new ArimaaMovespec(MOVE_FINISH_PULL,adj.col,adj.row,src.col,src.row,who));
	 		}
	 	}
	 }}
 }
 private boolean isPullSource(ArimaaCell pick,ArimaaChip ob,ArimaaCell pullSource,ArimaaCell pullDest)
 {
	 if((pullSource!=null) && (pullDest!=null) && pick.isAdjacentTo(pullSource))
	 	{
		 ArimaaChip puller = pullDest.topChip();
		 if(puller==null) { if (capturedLocation==pullDest) { puller = capturedPiece; }}
		 if(puller==null) { if (capturedLocation2==pullDest) { puller = capturedPiece2; }}
		 return((puller!=null)&&(ob.chipPower()<puller.chipPower()));
	 
	 }
	 return(false);
 }
 commonMove getRandomMove(Random rand,int hysterysis,int rabits)
 {
	 return(getRandomMove(rand,whoseTurn,lastRobotMove,hysterysis,rabits));
 }
 commonMove getRandomMove(Random rand,int who,ArimaaMovespec lastMove, int hysterysis,int rabits)
 {	 
	 switch(board_state)
	 {
	 default: throw G.Error("Not expecting state %s",board_state);
	 case ILLEGAL_MOVE_STATE:
		 break;
	 case INITIAL_SETUP_STATE:
	 	{
	 	// without loss of generality, we can place the pieces in order, and just place all the rabbits
	 	// in the remaining spaces.
	 	ArimaaCell row[]=rack[who];
	 	{
	 	int dest = (who==FIRST_PLAYER_INDEX) ? 7 : 2;
	 	for(int i=1,lim=ArimaaChip.RABBIT_INDEX; i<lim;i++)
	 		{
	 		 ArimaaCell c = row[i];
	 		 if(c.height()>0)
	 		 	{
	 			 commonMove m = placeInRandomRow(rand,c,dest,who);
	 			 if(m!=null) { return(m); }
	 		 	}
	 		}}
	 	// didn't find anything else, place a rabbits.  At this point there's one rabbit
	 	// for each available cell, so no need to offer choices.
	 	{
	 	ArimaaCell c = row[ArimaaChip.RABBIT_INDEX];
	 	int dest = (who==FIRST_PLAYER_INDEX) ? 7 : 1;
	 	if(c.height()>0)
	 	{
	 	 return(placeInRandomFirstRow(rand,c,dest,who));
	 	}}
	 	throw G.Error("Shouldn't get here, all placed");
	 	}
	 case PUSH_STATE:
	 	{ getRandomPushMove(rand,who,pushPullSource,pushPullDest);
	 	}
		 break;
	 case PLAY_STATE:
		if(hysterysis > 0 && lastMove!=null && playStep>0 && (Random.nextSmallInt(rand,100)<hysterysis))
		{
			ArimaaCell to = getCell(lastMove.to_col,lastMove.to_row);
			if(to!=null)
			{
				ArimaaChip top = to.topChip();
				if(top!=null && playerIndex(top)==whoseTurn)
				{
					commonMove m = placeInRandomSteps(rand,to, top,playStep,lastMove);
		 			if(m!=null) { return(m); }
				}
			}
		}
		cell<ArimaaCell> allCells[] = getCellArray();
		int offset = allCells.length;
		while(offset>0)
		{	int randomn = Random.nextSmallInt(rand,offset);
			ArimaaCell c = (ArimaaCell)allCells[randomn];
			allCells[randomn] = allCells[--offset];	// get a random cell and permut them
			allCells[offset] = c;
	 		ArimaaChip ch = c.topChip();
	 		if((ch!=null) && playerIndex(ch)==who)
	 		{	if((rabits==0)||(Random.nextSmallInt(rand,100)>rabits))
	 			{
	 			commonMove m = placeInRandomSteps(rand,c, ch,playStep,lastMove);
	 			if(m!=null) { return(m); }
	 			}
	 		}
	 	}
	 }
	 return(new ArimaaMovespec(MOVE_RESIGN,whoseTurn));

 }

 
 private void getListOfMoves(CommonMoveStack  all,int who,ArimaaMovespec lastMove,boolean forRobot)
 {
	 switch(board_state)
	 {
	 default: throw G.Error("Not expecting state %s",board_state);
	 case ILLEGAL_MOVE_STATE:
		 return;
	 case INITIAL_SETUP_STATE:
	 	{
	 	// without loss of generality, we can place the pieces in order, and just place all the rabbits
	 	// in the remaining spaces.
	 	ArimaaCell row[]=rack[who];
	 	{
	 	int dest = (who==playerColor[FIRST_PLAYER_INDEX]) ? variation.nRows-1 : 2;
	 	for(int i=1,lim=ArimaaChip.RABBIT_INDEX; i<lim;i++)
	 		{
	 		 ArimaaCell c = row[i];
	 		 if(c.height()>0)
	 		 	{
	 			 placeInRows(all,c,dest,who);
	 			 return;
	 		 	}
	 		}}
	 	// didn't find anything else, place a rabbits.  At this point there's one rabbit
	 	// for each available cell, so no need to offer choices.
	 	{
	 	ArimaaCell c = row[ArimaaChip.RABBIT_INDEX];
	 	int dest = (who==playerColor[FIRST_PLAYER_INDEX]) ? variation.nRows-1 : 1;
	 	if(c.height()>0)
	 	{
	 	 placeInFirstRow(all,c,dest,who);
	 	 return;
	 	}}
	 	throw G.Error("Shouldn't get here, all placed");
	 	}
	 case PUSH_STATE:
	 	{ getPushMoves(all,who,pushPullSource,pushPullDest);
	 	}
		 break;
	 case PLAY_STATE:

	 	for(ArimaaCell c = allCells; c!=null; c=c.next)
	 	{	ArimaaChip ch = c.topChip();
	 		if((ch!=null) && playerIndex(ch)==who)
	 		{
	 		placeInSteps(all,c,ch,playStep,lastMove);
	 		}
	 	}
 		if(!forRobot && (pushPullSource!=null)) { getPullMoves(all,who,pushPullSource,pushPullDest); }
	 }
 }
 
 // this is the move generator entry point for the robot
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
    robotGame = true;
 	getListOfMoves(all,whoseTurn,lastRobotMove,true);
    if(all.size()==0) { all.addElement(new ArimaaMovespec(MOVE_RESIGN,whoseTurn)); }
  	return(all);
 }
 
}
