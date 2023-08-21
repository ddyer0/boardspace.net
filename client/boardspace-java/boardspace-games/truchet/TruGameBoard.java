package truchet;

import java.awt.Color;

import java.util.Hashtable;

import lib.*;
import online.game.*;
import truchet.TruChip.ChipColor;

/**
 * TruchetBoard knows all about the game of Truchet, which is played
 * on a 7x7 board. It gets a lot of logistic support from 
 * common.rectBoard, which knows about the coordinate system.  
 * 
 * This class doesn't do any graphics or know about anything graphical, 
 * but it does know about states of the game that should be reflected 
 * in the graphics.
 * 
 *  The principle interface with the game viewer is the "Execute" method
 *  which processes moves.  Note that this
 *  
 *  In general, the state of the game is represented by the contents of the board,
 *  whose turn it is, and an explicit state variable.  All the transitions specified
 *  by moves are mediated by the state.  In general, my philosophy is to be extremely
 *  restrictive about what to allow in each state, and have a lot of tripwires to
 *  catch unexpected transitions.   We expect to be fed only legal moves, but mistakes
 *  will be made and it's good to have the maximum opportunity to catch the unexpected.
 *  
 * Note that none of this class shows through to the game controller.  It's purely
 * a private entity used by the viewer and the robot.
 * 
 * @author ddyer
 *
 */

class TruGameBoard extends rectBoard<TruCell> implements BoardProtocol,TruConstants
{
	
	private TruchetState unresign;
	private TruchetState board_state;
	public TruchetState getState() {return(board_state); }
	public void setState(TruchetState st) 
	{ 	unresign = (st==TruchetState.RESIGN_STATE)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public int boardSize = DEFAULT_BOARDSIZE;	// size of the board
    public void SetDrawState() { setState(TruchetState.DRAW_STATE); }
    public TruCell tTiles[] = null;
    public TruCell whiteChips = null;
    public TruCell blackChips = null;
    
    public CellStack animationStack = new CellStack();
    public ChipColor playerColor[]=new ChipColor[2];
    public TruChip playerChip[] = new TruChip[2];
    public TruCell playerPool[] = new TruCell[2];
    //
    // private variables
    //
    private int sweep_counter=0;
	
    public int chips_on_board[] = new int[2];			// number of chips currently on the board
    public TruCell bases[][] = new TruCell[2][4];
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    // note that these are intended to be used by and maintained by the GUI, not the robot
    //
    private TruCell s_focus = null;			// focus for split/merge operations
    private TruCell m_focus = null;
    public TruCell flippedCell = null;
    private TruCell sm_source[] = new TruCell[22];	// undo information for sm operations
    private TruCell sm_dest[]=new TruCell[22];		// undo stack of destinations
    public TruChip sm_picked[]=new TruChip[22];
    private TruchetState sm_state[] = new TruchetState[22];			// undo stack of states
    public int sm_step = 0;	
    public TruCell blackCaptures;
    public TruCell whiteCaptures;
    public TruCell captures[] = new TruCell[2];
    private CommonMoveStack  pickedRiverMoves=null;
    private CommonMoveStack  pickedSplitMoves=null;
    private CommonMoveStack  pickedMergeMoves=null;
    
	// factory method
	public TruCell newcell(char c,int r)
	{	return(new TruCell(c,r));
	}
    public TruGameBoard(String init,int map[]) // default constructor
    {   setColorMap(map, 2);
    	doInit(init,0L); // do the initialization 
    }
    public TruGameBoard cloneBoard() 
	{ TruGameBoard dup = new TruGameBoard(gametype,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((TruGameBoard)b); }
    public TruGameBoard(String init,String gameid,int map[])
    {	// convert a game name to the random key in a unique but specific way.
    	// this initializer determines the initial layout of the flippable tiles
    	long n=0L;
    	for(int i=0;i<gameid.length();i++) { n = (n*4+gameid.charAt(i))%100000000; };
    	setColorMap(map, 2);
    	doInit(init,n);
    }
    // override method.  xpos ypos is the desired centerpoint for the text
    // truchet needs two tweaks to make the grid right.  Offset to label
    // the intersections instead of the squares, and skip the last row/col
    public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
    {   char ch = txt.charAt(0);
    	boolean isDig = Character.isDigit(ch);
    	if(isDig) 
    	{ if(G.IntToken(txt)>=nrows) { return; } ;
    	  ypos -= 5*cellsize/10 ; 
    	}
    	else 
    	{ if((ch-('A'-1))>=ncols)
    		{ return; } ; 
    	  xpos += 9*cellsize/20; 
    	}
   		GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
    }


    final TruCell SetCell(char col,int row,int idx)
    {	TruCell c = getCell(col,row);
    	c.addTile(tTiles[idx].topChip());
    	return(c);
    }
    final void SetMatchingCell(char col,int row,int idx)
    {	TruCell c = getCell(col,row);
    	if(!c.chipColorMatches(tTiles[idx].topChip())) { idx ^=1; }	// flip the chip
		c.addTile(tTiles[idx].topChip());
    }
    // standared init for Hex.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard(String game,long rk)
    { 	if(Truchett_INIT.equalsIgnoreCase(game)) { boardSize=DEFAULT_BOARDSIZE+2;  }
    	else {throw  G.Error(WrongInitError,game); }
        gametype = game;
        randomKey = rk;
        setState(TruchetState.PUZZLE_STATE);
        initBoard(boardSize,boardSize); //this sets up the board and cross links
        int map[]=getColorMap();
        // temporary, fill the board
        char last = (char)('A'+boardSize-1);
        SetCell('A',1,TruChip.SW_tile_index);
        SetCell(last,1,TruChip.SE_tile_index).onBoard=false;
        SetCell('A',boardSize,TruChip.NW_tile_index);
        SetCell(last,boardSize,TruChip.NE_tile_index).onBoard=false;
        Random r = new Random(randomKey);
        for(int i=FIRST_PLAYER_INDEX;i<=SECOND_PLAYER_INDEX;i++)
        { chips_on_board[i]=0;
        }
        for(int i=2; i<boardSize;i++)
        {	char col = (char)('A'+i-1);
        	SetCell('A',i,((i&1)==0)?TruChip.LA_tile_index:TruChip.LB_tile_index);
        	SetCell(last,i,((i&1)==0)?TruChip.RA_tile_index:TruChip.RB_tile_index).onBoard=false;
        	SetCell(col,1,((i&1)!=0)?TruChip.BA_tile_index:TruChip.BB_tile_index);
        	SetCell(col,boardSize,((i&1)!=0)?TruChip.TA_tile_index:TruChip.TB_tile_index).onBoard=false;
        	for(int j=2;j<boardSize;j++)
        	{	SetMatchingCell(col,j,TruChip.R0_tile_index+(Random.nextInt(r,4)));
        	}
        }
		TruChip bchip = blackChips.topChip();
		TruChip wchip = whiteChips.topChip();
		
		{
		int fp = map[FIRST_PLAYER_INDEX];
		playerChip[fp] = wchip;
        playerColor[fp] = wchip.getColor();
        playerPool[fp] = whiteChips;
    	}
        {
        int sp = map[SECOND_PLAYER_INDEX];
        playerChip[sp] = bchip;
        playerColor[sp] = bchip.getColor();
        playerPool[sp] = blackChips;
        }
        G.Assert(playerColor[0].ordinal()==playerIndex(wchip),"match");
        boolean reverse = map[0]!=0;
		for(int row=1; row<=3; row++)
    	{	int invrow = boardSize-row;
			for(int colnum=1,idx=0;colnum<boardSize;idx++,colnum+=2)
			{	char col = (char)('A'+colnum-1+((row&1)^1));
				char invcol = (char)('A'+colnum-1+(invrow&1));
        		TruCell c = getCell(col,row);
        		TruCell d = getCell(invcol,invrow);
        		c.addChip(reverse?wchip:bchip);
       		    d.addChip(reverse?bchip:wchip);
       		    c.region_size=-1;
       		    d.region_size=-1;
        		if(row==1) 
        			{ c.isBase=true; 
            		  d.isBase=true;
        			  bases[FIRST_PLAYER_INDEX][idx]=c; 
        			  bases[SECOND_PLAYER_INDEX][idx]=d;
        			  
        			}
        		chips_on_board[SECOND_PLAYER_INDEX]++;
        		chips_on_board[FIRST_PLAYER_INDEX]++;
       	}
        }
		for(TruCell c = allCells; c!=null; c=c.next)
		{	if(c.region_size<0) { mark1RegionSize(c); }
		}
        whoseTurn = FIRST_PLAYER_INDEX;
    }
    public void sameboard(BoardProtocol f) { sameboard((TruGameBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(TruGameBoard from_b)
    {	super.sameboard(from_b);
    	G.Assert(AR.sameArrayContents(chips_on_board,from_b.chips_on_board),"chips_on_board mismatch");
 
        // here, check any other state of the board to see if
        G.Assert(sameCells(s_focus,from_b.s_focus), "focus not the same");
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");
    }

    /** this is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game.  Other site machinery looks for duplicate digests.
     * @return
     */
    public long Digest()
    {
        Random r = new Random(64 * 1000); // init the random number generator
        long v = super.Digest(r);

        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
		//v ^= chip.Digest(r,sm_picked[sm_step]);//if(pickedObject!=null) { v^= (pickedObject.Digest()<<1); }
		v ^= chip.Digest(r,sm_picked[sm_step]);
		v ^= Digest(r,captures);
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
		return (v);
    }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(TruGameBoard from_b)
    {	super.copyFrom(from_b);
    	s_focus = getCell(from_b.s_focus);
       	m_focus = getCell(from_b.m_focus);
        board_state = from_b.board_state;
        unresign = from_b.unresign;
    	getCell(sm_source,from_b.sm_source);
    	getCell(sm_dest,from_b.sm_dest);
    	copyFrom(captures,from_b.captures);
    	AR.copy(sm_picked,from_b.sm_picked);
    	AR.copy(sm_picked,from_b.sm_picked);
       	AR.copy(sm_state,from_b.sm_state);
       	AR.copy(chips_on_board,from_b.chips_on_board);
       	pickedMergeMoves = from_b.pickedMergeMoves;
       	pickedSplitMoves = from_b.pickedSplitMoves;
       	pickedRiverMoves = from_b.pickedRiverMoves;
        sm_step = from_b.sm_step;

        sameboard(from_b);
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long rk)
    {	drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
		Grid_Style = TRUGRIDSTYLE; //coordinates left and bottom
		randomKey = rk;
		animationStack.clear();
		Random r = new Random(63742);
    	tTiles = new TruCell[TruChip.N_STANDARD_TILES];
		int map[] = getColorMap();
    	for(int i=0;i<TruChip.N_STANDARD_TILES;i++)
    	{	
    		TruCell cell = new TruCell(r,TruId.Tile);
    		tTiles[i]=cell;
    		cell.addTileNoCheck(TruChip.getChip(i));
    	}
    	blackChips = new TruCell(r,TruId.Black_Chip_Pool);
    	whiteChips = new TruCell(r,TruId.White_Chip_Pool);
    	blackChips.addChip(TruChip.getChip(TruChip.BLACK_CHIP_INDEX));
    	whiteChips.addChip(TruChip.getChip(TruChip.WHITE_CHIP_INDEX));	
    	blackCaptures = new TruCell(r,TruId.Black_Captures);
    	whiteCaptures = new TruCell(r,TruId.White_Captures);
		captures[map[FIRST_PLAYER_INDEX]] = whiteCaptures;
		captures[map[SECOND_PLAYER_INDEX]] = blackCaptures;
       s_focus = null;
       m_focus = null;
       Init_Standard(gtype,randomKey);
       allCells.setDigestChain(r);
       sm_step = 0;
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
        case PLAY_STATE:
        case STARTS_STATE:
        case STARTM_STATE:
        case STARTSM_STATE:
        case SORM_STATE:
        case CONFIRM_STATE:
        case DRAW_STATE:
        case M_DONE_STATE:
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
        {case STARTSM_STATE:
         case CONFIRM_STATE:
         case STARTS_STATE:
         case STARTM_STATE:
         case M_DONE_STATE:
         case DRAW_STATE:
         case RESIGN_STATE:
            return (true);

        default:
            return (false);
        }
    }
   public boolean DigestState()
   {	
   		switch(board_state)
	   	{case CONFIRM_STATE: 
	   	 case M_DONE_STATE:
	   		 return(true);
	   	 default: return(false);
	   	}
   }
   public boolean MandatoryDoneState()
   {
    switch (board_state)
    {case RESIGN_STATE:
     case CONFIRM_STATE:
     case M_DONE_STATE:
     case DRAW_STATE:
        return (true);

    default:
        return (false);
    }
}

    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(TruchetState.GAMEOVER_STATE);
    }
    int enemyBasesOccupied(int forPlayer)
    {	TruCell b[] = bases[forPlayer];
    	TruChip desiredColor = playerChip[forPlayer];
    	int n=0;
    	for(int i=0;i<b.length;i++)
    	{	if(b[i].topChip()==desiredColor) { n++; }
    	}
    	return(n);
    }
    // look for a win for player.  This algorithm should work for Gobblet Jr too.
    public boolean WinForPlayerNow(int player)
    {	if(board_state==TruchetState.GAMEOVER_STATE) { return(win[player]); }
        if((chips_on_board[player]>0)
        		&& (chips_on_board[nextPlayer[player]]==0)) 
        	{ // captured everything
        		return(true); 
        	} 
 
     	return(enemyBasesOccupied(player)>=3);
     }

    // look for a win for player.  This algorithm should work for Gobblet Jr too.
    double base_tropism_score = 0.1;
    double region_size_score = 0.02;
    //double color_tropism_score = 0.01;
    double height_multiplier[]={1.0,1.0,2.205,3.31,4.42,4.0,4.0};
    public double ScoreForPlayer(int player,boolean print,
    			double base_weight,
    			double piece_weight,
    			boolean dumbot)
    { 
    	double base_score = enemyBasesOccupied(player)*base_weight;
    	double piece_score = chips_on_board[player]*piece_weight;
    	double sub=0.0;
    	double reg=0.0;
    	for(TruCell c = allCells; c!=null; c=c.next)
    	{	if(c.onBoard && (c.topChip()==playerChip[player]))
    		{	double baserow = 1.0+base_tropism_score*((player!=FIRST_PLAYER_INDEX) ? c.row : (nrows-c.row));
    			sub += height_multiplier[c.chipIndex]*baserow;
    			if(!dumbot) { reg += c.chipIndex*Math.sqrt(region_size_score)*c.region_size; }
    		}
    	}
    	if(print)
    	{	System.out.println("pl "+player+" base "+base_score+" piece "+piece_score + " sub "+sub+" reg "+reg);
    	}
    	return(base_score+piece_score+sub+reg);
    }
    

    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement()
    {	sm_source[++sm_step] = null;
    	sm_dest[sm_step]=null;
    	sm_picked[sm_step]=null;
        sm_state[sm_step] = board_state;
    }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    G.Assert(sm_step>0,"something dropped");
    if(sm_source[sm_step]==null) { sm_step--; }
    TruCell dr = sm_dest[sm_step];
    TruChip pickedObject = sm_picked[sm_step];
    if(dr!=null)
    	{
    	sm_dest[sm_step] = null;
    	if(pickedObject!=null)
    	{
    	dr.removeTop(pickedObject);
    	chips_on_board[playerIndex(pickedObject)]--;
    	}}
    setState(sm_state[sm_step]);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private TruCell unPickObject()
    {	TruChip po = sm_picked[sm_step];
    	TruCell pickedSource = sm_source[sm_step];
    	TruchetState nextState = sm_state[sm_step];
    	sm_source[sm_step]=null;
    	if(po!=null)
    	{
     	sm_picked[sm_step] = null;
    	if((pickedSource!=null) && pickedSource.onBoard)
    		{ 
    		chips_on_board[playerIndex(po)]++; 
    		pickedSource.addChip(po);}
    		}
    	setState(nextState);
    	return(pickedSource);
     }
    private void dropBoardLocation(TruCell c)
    {
    	TruCell droppedDest = sm_dest[sm_step] = c;
    	TruChip pickedObject = sm_picked[sm_step];
		droppedDest.addChip(pickedObject);
		if(c.onBoard) { chips_on_board[playerIndex(pickedObject)]++; }
    }

    // 
    // drop the floating object.
    //
    private void dropObject(TruId dest, char col, int row)
    {  
       G.Assert((sm_picked[sm_step]!=null)&&(sm_dest[sm_step]==null),"ready to drop");
       switch (dest)
        {
        default: throw G.Error("Not expecting dest %s",dest);
        case EmptyBoard:
        case BoardLocation: // an already filled board slot.
        	dropBoardLocation(getCell(col,row));
        	break;
        case White_Captures:
        	dropBoardLocation(whiteCaptures);
        	break;
        case Black_Captures:
        	dropBoardLocation(blackCaptures);
        	break;
        case Black_Chip_Pool:		// back in the pool
        case White_Chip_Pool:		// back in the pool
        	sm_step=-1;
        	acceptPlacement();		// we're not counting
        	unStep();
            break;
        }
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(TruCell cell)
    {	return( (sm_step>0)
    			&& (sm_source[sm_step]==null) 
    			&& (sm_dest[sm_step-1]==cell));
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    {	TruChip ch = sm_picked[sm_step];
    	if((ch!=null)&&(sm_dest[sm_step]==null))
    		{ return(ch.chipNumber);
    		}
        return (NothingMoving);
    }

    private void pickBoardLocation(TruCell c)
    {  	G.Assert(c.chipIndex>0,"something is there to remove");
    	TruChip pickedObject = sm_picked[sm_step] = c.removeTop();
    	sm_source[sm_step] = c;
		chips_on_board[playerIndex(pickedObject)]--;
    }
    
    // do any captures associated with cell C. We assume that the stacks
    // will be uniform, as they must be in normal play
    private int doCaptures(TruCell c,replayMode replay)
    {	int ncap=0;
		int topIndex = c.chipIndex;
		if(topIndex>1)
    	{	int botIndex = 1;
    		while(botIndex<topIndex)
    		{ TruChip bot = c.chipAtIndex(botIndex);
    		  TruChip top = c.chipAtIndex(topIndex);
    		  if(bot!=top) 
    		  {	chips_on_board[playerIndex(bot)]--;
    		  	captures[whoseTurn].addChip(bot);
    		    if(replay!=replayMode.Replay)
    		    {
    		    	animationStack.push(c);
    		    	animationStack.push(captures[whoseTurn]);
    		    }
    			c.setChipAtIndex(botIndex,c.removeTop());
    			topIndex--;
    			botIndex++;
    			ncap++;
    		  }
    		  else { botIndex=topIndex; }
    		}
    	}
    	if(G.debug())
    	{ // verify that the stack is now uniform in color
    	  int height = c.chipIndex;
    	  if(height>=2)
    	  {
    	  TruChip bc = c.chipAtIndex(1);
    	  int topp = playerIndex(bc);
    	  for(int i=2;i<=c.chipIndex;i++) 
    	  	{ G.Assert(playerIndex(c.chipAtIndex(i))==topp,"uniform color");
    	  }
    	}
    	}
    	return(ncap);
    }
    public void undoCaptures(TruCell c,int playerIndex,int n)
    {	G.Assert(c.onBoard,"on board");
    	if(n>0)
    	{
    	for(int i=0;i<n;i++)
	    	{
	    	TruChip removed = captures[nextPlayer[playerIndex]].removeTop();
	    	c.addChip(removed);
	    	chips_on_board[playerIndex]++;
	    	}
    	}
    }
    public TruCell getCell(TruCell c)
    {
    	if(c!=null)
    	{
    		return(getCell(c.rackLocation(),c.col,c.row));
    	}
    	return(null);
    }
    public TruCell getCell(TruId source,char col,int row)
    {
    	switch(source)
    	{
        default:
        	throw G.Error("Not expecting source %s", source);
  	
        case BoardLocation:
     		return(getCell(col,row));
        case White_Captures:
        	return(whiteCaptures);
        case Black_Captures:
        	return(blackCaptures);
        case Black_Chip_Pool:
        	return(blackChips);
        case White_Chip_Pool:
        	return(whiteChips);
    	}
     }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(TruId source, char col, int row)
    {	G.Assert((sm_picked[sm_step]==null)&&(sm_source[sm_step]==null),"ready to pick");
    
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);

        case BoardLocation:
         	{
        	TruCell c = getCell(col,row);
        	pickBoardLocation(c);
        	switch(board_state)
        	{
        	case M_STATE:
        	case M_DONE_STATE:
        	case S_STATE:
        	default: break;
        	case PLAY_STATE:
        	case MSM_STATE:
        		pickedRiverMoves =new CommonMoveStack();
        		getRiverMovesFrom(c,pickedRiverMoves);
				//$FALL-THROUGH$
			case STARTSM_STATE:
			case STARTS_STATE:
        		pickedSplitMoves = new CommonMoveStack();
        		getSplitMovesFrom(c,pickedSplitMoves);
        		s_focus = c;
        		if(board_state==TruchetState.S_STATE) { break; }
				//$FALL-THROUGH$
			case STARTM_STATE:
        		pickedMergeMoves =new CommonMoveStack();
        		getMergeMovesFrom(c,pickedMergeMoves);
        		break;
         	}}
        	break;
        case Black_Captures:
        	{
        	sm_picked[sm_step] = blackCaptures.removeTop();
        	sm_source[sm_step] = blackCaptures;
        	}
        	break;
        case White_Captures:
        	{
        	sm_picked[sm_step] = whiteCaptures.removeTop();
        	sm_source[sm_step] = whiteCaptures;
        	         	}
        	break;
        case White_Chip_Pool:
        	{
        	TruCell c  = sm_source[sm_step] = whiteChips;
        	sm_picked[sm_step] = c.topChip();
        	break;
        	}
        case Black_Chip_Pool:
        	{
        	TruCell c = sm_source[sm_step] = blackChips;
        	sm_picked[sm_step] = c.topChip();
        	break;
        	}
        }
   }

    //
    // in the actual game, picks are optional; allowed but redundant.
    //
    private boolean canBeSplit()
    {	Hashtable<TruCell,TruCell> moves = splitMoveDests();
    	return(moves.size()>0);
    }
    private boolean canBeMerged()
    {	Hashtable<TruCell,TruCell> moves = mergeMoveDests();
    	return(moves.get(sm_dest[sm_step])!=null); 
    }
    private void setNextStateAfterDrop(replayMode replay)
    {
      TruCell droppedDest = sm_dest[sm_step];
      TruCell pickedSource = sm_source[sm_step];
      
      if(pickedSource==droppedDest)
      {	sm_dest[sm_step] = sm_source[sm_step] = null;
      	sm_picked[sm_step]=null;
      }
      else
      {
      switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case CONFIRM_STATE:
        case DRAW_STATE:
        	// TODO: should also check that at least 2 cells are split to
        	if(sm_dest[sm_step]!=null) { setNextStateAfterDone(); }
        	break;
        	
         case SORM_STATE:
        	 // in sorm state, the second pick/drop pair should decide between
        	 // split or merge.
        	 if((pickedSource!=s_focus) || ((droppedDest==m_focus)&&(s_focus.chipIndex==0))) 
        	 	{ setState(TruchetState.M_STATE); 
        	 	}
        	 else if(droppedDest!=m_focus) 
        	 	{ setState((s_focus.chipIndex==0)?TruchetState.CONFIRM_STATE:TruchetState.S_STATE); 
        	 	}
        	 acceptPlacement();
        	 break;
         case PLAY_STATE:
         case STARTS_STATE:
         case STARTSM_STATE:
         case STARTM_STATE:
         case MSM_STATE:
        	int source = pickedSource.intersectionColor();
        	int dest = droppedDest.intersectionColor();
        	if(source==dest) 
        		{ 
        		// finishing a river move
        		moveRestOfStack(replay);
        		pickedMergeMoves =new CommonMoveStack();
        		pickedSplitMoves = new CommonMoveStack();
        		int nsplit = getSplitMovesFrom(droppedDest,pickedSplitMoves);
        		int nmerge = getMergeMovesFrom(droppedDest,pickedMergeMoves);
        		if((nsplit>0)&&(nmerge>0)) { setState(TruchetState.STARTSM_STATE); }
        		else if(nsplit>0) { setState(TruchetState.STARTS_STATE); }
        		else if (nmerge>0) { setState(TruchetState.STARTM_STATE); }
        		else { setState(TruchetState.CONFIRM_STATE); }
        		m_focus = null;
        		s_focus = droppedDest;
        		acceptPlacement();
        		}
         	else {
        		// starting a split or join
        		boolean canbesplit = canBeSplit();
        		boolean canbemerge = canBeMerged();
        		if(canbesplit && canbemerge) { setState(TruchetState.SORM_STATE); }
        		else if(canbesplit) { setState(TruchetState.S_STATE); }
        		else if(canbemerge) { setState(TruchetState.M_STATE); }
        		else { throw G.Error("Can't be"); }
        		m_focus = droppedDest;
           		moveRestOfStack(replay);	// second chance to move the rest
        		acceptPlacement();
        	}
			//setState(TruchetState.CONFIRM_STATE);
			break;
        case S_STATE:
        	G.Assert(s_focus!=null,"in a split");
        	if(s_focus.chipIndex==0) { setState(TruchetState.CONFIRM_STATE); }
        	acceptPlacement();
        	break;
        case M_DONE_STATE:
        case M_STATE:
        	 G.Assert(droppedDest==m_focus,"continuing the merge");
        	 acceptPlacement();
        	 Hashtable<TruCell,TruCell> ms = mergeMoveSources();
        	 // TODO: note, if it's a merge capture, should also consider
        	 // the height of the stack.
        	 switch(ms.size())
        	 {
        	 default: setState(TruchetState.M_DONE_STATE);
        	 	break;
        	 case 0: setState(TruchetState.CONFIRM_STATE);
        	 	break;
        	 }
         	 break;
        case PUZZLE_STATE:
        	acceptPlacement();
            break;
        }
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
        case CONFIRM_STATE:
        case DRAW_STATE:
        	setState(TruchetState.PLAY_STATE);
        	break;
        case PLAY_STATE:
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

        case DRAW_STATE:
        	setGameOver(false,false);
        	break;
    	case CONFIRM_STATE:
    	case M_DONE_STATE:
    	case STARTSM_STATE:
    	case STARTS_STATE:
    	case STARTM_STATE:
    		setState(TruchetState.PLAY_STATE);
    		if(!has_legal_moves()) 
    		{ setGameOver(false,true); 
    		}
  		  break; 
    	case SORM_STATE:	// needed by some damaged games
    	case PUZZLE_STATE:
    		setState(TruchetState.PLAY_STATE);
			break;
		case PLAY_STATE:
     		break;
    	}
		s_focus = m_focus = null;
   		sm_step=-1;
   	 	acceptPlacement();

    }
   
    private void unStep()
    {
       	while(sm_step>=0)
    	{	sm_source[sm_step]=null;
    		sm_dest[sm_step]=null;
    		sm_picked[sm_step]=null;
    		sm_step--;
    	}
       	sm_step = 0;
    }
    
    private void doDone(replayMode replay)
    {	
        acceptPlacement();

        if (board_state==TruchetState.RESIGN_STATE)
        {	setGameOver(false,true);
        }
        else
        {	
        	if(sm_step>=0)
        	{	for(int i=0;i<sm_step;i++)
        		{ TruCell c = sm_dest[i];
        		if(c!=null) { doCaptures(c,replay); }
        		}
        	}

        	boolean win1 = WinForPlayerNow(whoseTurn);
        	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
        	unStep();
        	flippedCell=null;
        	if(win1 || win2)  { setGameOver(win1,win2); }
        	else {setNextPlayer(); setNextStateAfterDone(); }
        }
    }
    private void flipCell (TruCell c)
    {
   	 G.Assert(c.chipIndex==0,"Cell is empty");
   	 invalidateRegionSize(c);
	 TruChip chip = c.removeTop();
	 TruChip newchip = tTiles[chip.chipNumber^3].topChip();
	 c.addTile(newchip);
	 markRegionSizes(c);
    }
    public void reallyMoveRestOfStack(TruCell pickedSource,TruCell droppedDest,replayMode replay)
    {
    	if(pickedSource!=droppedDest)
    	{
    	// one already moved
    	while(pickedSource.chipIndex>0)
    	{	acceptPlacement();
    		pickBoardLocation(pickedSource);
    		dropBoardLocation(droppedDest);
    		if(replay!=replayMode.Replay)
    		{
    			animationStack.push(pickedSource);
    			animationStack.push(droppedDest);
    		}
    	}}
	
    }
    public void moveRestOfStack(replayMode replay)
    {	TruCell pickedSource = sm_source[sm_step];
    	TruCell droppedDest = sm_dest[sm_step];
     	
    	int source = pickedSource.intersectionColor();
    	int dest = droppedDest.intersectionColor();
    	switch(board_state)
    	{
    	default: throw G.Error("not handled yet");
    	case PUZZLE_STATE:
    	case CONFIRM_STATE:
    		break;
    	case M_STATE:
    		reallyMoveRestOfStack(pickedSource,droppedDest,replay); 
    		break;
    	case STARTS_STATE:
    	case STARTM_STATE:
    	case STARTSM_STATE:
    		if(source==dest) 
    		{	// here, he moved a stack to a new location, and apparently changed his mind and
    			// moved to a new location.  
       			//pickedSource=sm_source[0];
       			//if(pickedSource==flippedCell) { pickedSource=sm_source[1]; }
       			pickedSource = resetToBase(false);
    			if(pickedSource!=droppedDest) 
    				{ pickBoardLocation(pickedSource);	
    				  dropBoardLocation(droppedDest);
    				  if(replay!=replayMode.Replay)
    				  {
    					  animationStack.push(pickedSource);
    					  animationStack.push(droppedDest);
    				  }
    				}
     		}
			//$FALL-THROUGH$
		case M_DONE_STATE:
    	case PLAY_STATE:
    	case MSM_STATE:
    	case GAMEOVER_STATE:
    		if(source==dest) 
    			{ reallyMoveRestOfStack(pickedSource,droppedDest,replay);
    			  break; 
    			}
    		break;
    	case S_STATE:
    	case SORM_STATE:
   		break;
    	}
    }
 
    public void doFlip(TruMovespec m)
    {
     TruCell c = getCell(m.from_col,m.from_row);
   	 flipCell(c);
   	 switch(board_state)
   	 {	
   	 default: throw G.Error("not expecting flip in state %s",board_state);
   	 case PUZZLE_STATE:	break;
   	 case PLAY_STATE:	
  		 sm_source[sm_step] = sm_dest[sm_step] = c; 
   		 flippedCell = c;
   		 setState(TruchetState.MSM_STATE); 
   		 acceptPlacement();
   		 break;
   	 case MSM_STATE:
   		 flippedCell = null;
   		 setState(TruchetState.PLAY_STATE);
   		 break;
   	 }
   	 
     }
    public void doMove(TruMovespec m,replayMode replay)
    {	TruCell dest = getCell(m.to_col,m.to_row);
		TruCell source = getCell(m.from_col, m.from_row);
		pickBoardLocation(source);
		dropBoardLocation(dest); 
		if(replay!=replayMode.Replay)
		{
			animationStack.push(source);
			animationStack.push(dest);
		}
		reallyMoveRestOfStack(source,dest,replay);
    }
    public void undoMove(TruMovespec m)
    {	TruCell dest = getCell(m.to_col,m.to_row);
    	TruCell source = getCell(m.from_col, m.from_row);
    	pickBoardLocation(dest);
		dropBoardLocation(source); 
		reallyMoveRestOfStack(dest,source,replayMode.Replay);	
    }
    public int directionNumber(char ch)
    {
    	switch(ch)
    	{
     	default: throw G.Error("not expecting %s",ch);
    	case 'N':
    	case 'n': return(CELL_UP);
    	case 'S':
    	case 's': return(CELL_DOWN);
    	case 'E':
    	case 'e': return(CELL_RIGHT);
    	case 'W':
    	case 'w': return(CELL_LEFT);
    	}
    }

    public int playerIndex(TruChip ch)
    {	int ind = ch.colorIndex();
      	return((ind>=0)?getColorMap()[ind]:ind); 
    }
    public void doMerge(TruMovespec m,replayMode replay)
    {	TruCell center = getCell(m.to_col,m.to_row);
    	TruCell from = getCell(m.from_col,m.from_row);
    	String info = m.splitInfo;
    	int len = info.length();
    	if(from!=center) { reallyMoveRestOfStack(from,center,replay); }
    	for(int i=0;i<len;i++)
    	{	char ch = info.charAt(i);
    	 	int dir = directionNumber(ch);
    	 	TruCell dest = center.exitTo(dir);
    	 	pickBoardLocation(dest);
    		dropBoardLocation(center); 
    		if(replay!=replayMode.Replay)
    		{
    			animationStack.push(dest);
    			animationStack.push(center);
    		}
    	}
    	//if(debug)
    	{
       	for(int i=0;i<len;i++)
    	{	char ch = info.charAt(i);
    	 	int dir = directionNumber(ch);
    	 	TruCell dest = center.exitTo(dir);
    	 	G.Assert(dest.chipIndex==0,"Merge took everything");
    	}}

    	m.undoInfo = doCaptures(center,replay);
    	G.Assert(center.chipIndex==len,"merge ends with correct height");
    }
    public void undoMerge(TruMovespec m)
    {
    	TruCell center = getCell(m.to_col,m.to_row);
       	String info = m.splitInfo;
       	int len = info.length();
       	G.Assert(center.chipIndex==len,"unmerge starts with height ok");
       	for(int i=0;i<len;i++)
    	{	char ch = info.charAt(i);
    	 	int dir = directionNumber(ch);
    	 	TruCell dest = center.exitTo(dir);
    	 	pickBoardLocation(center);
    		dropBoardLocation(dest); 
     	}
       	G.Assert(center.chipIndex==0,"unmerge ends empty");
       	undoCaptures(center,nextPlayer[m.player],m.undoInfo);
   }
    
    public void doSplit(TruMovespec m,replayMode replay)
    {	TruCell center = getCell(m.to_col,m.to_row);
   		TruCell from = getCell(m.from_col,m.from_row);
      	if(from!=center) { reallyMoveRestOfStack(from,center,replay); }
       	String info = m.splitInfo;
    	int len = info.length();
    	G.Assert(center.chipIndex==len,"Right number to move");

      	for(int i=0;i<len;i++)
    	{
    		char ch = info.charAt(i);
    	 	int dir = directionNumber(ch);
    	 	TruCell dest = center.exitTo(dir);
    	 	G.Assert(dest.topChip()!=playerChip[m.player],"free to split to");
       	}
 
       	for(int i=0;i<len;i++)
    	{
    		char ch = info.charAt(i);
    	 	int dir = directionNumber(ch);
    	 	TruCell dest = center.exitTo(dir);
    	 	pickBoardLocation(center);
    	 	dropBoardLocation(dest);
    	 	if(replay!=replayMode.Replay)
    	 	{
    	 		animationStack.push(center);
    	 		animationStack.push(dest);
    	 	}
       	}
       	m.undoInfo=0;
       	for(int i=0,multiplier=1;i<len;i++,multiplier*=10)
    	{	// encode the number of captures for each position of the split as 10*pos
    		char ch = info.charAt(i);
    	 	int dir = directionNumber(ch);
    	 	TruCell dest = center.exitTo(dir);
    	 	m.undoInfo += multiplier*doCaptures(dest,replay);
       	}
       	G.Assert(center.chipIndex==0,"split ends empty");
    }
    public void undoSplit(TruMovespec m)
    {
    	TruCell center = getCell(m.to_col,m.to_row);
   		TruCell from = getCell(m.from_col,m.from_row);
      	if(from!=center) { reallyMoveRestOfStack(from,center,replayMode.Replay); }
       	String info = m.splitInfo;
    	int len = info.length();
    	G.Assert(center.chipIndex==0,"unsplit starts empty");
      	for(int i=0;i<len;i++)
    	{	char ch = info.charAt(i);
    	 	int dir = directionNumber(ch);
    	 	TruCell dest = center.exitTo(dir);
    	 	pickBoardLocation(dest);
    	 	dropBoardLocation(center);
       	}
       	for(int i=0,capinfo=m.undoInfo;i<len;i++,capinfo/=10)
    	{	// encode the number of captures for each position of the split as 10*pos
    		int thiscap = capinfo%10;
    		if(thiscap>0)
    		{
    		char ch = info.charAt(i);
    	 	int dir = directionNumber(ch);
    	 	TruCell dest = center.exitTo(dir);
    	 	undoCaptures(dest,nextPlayer[m.player],thiscap);
    	 	G.Assert(dest.topChip()!=playerChip[m.player],"no chips left after unsplit");
    		}
       	}
       	G.Assert(center.chipIndex==len,"unsplit ends with height ok");
    }
    private TruCell resetToBase(boolean unflip)
    { TruCell c = null;
   	  while((sm_step>0) && (unflip || (sm_source[sm_step-1]!=flippedCell))) 
   	  { unDropObject(); 
   	    c = unPickObject(); 
   	  }
   	  if(sm_picked[sm_step]!=null) { c = unPickObject(); }
   	  return(c);
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	TruMovespec m = (TruMovespec)mm;
        TruChip pickedObject = sm_picked[sm_step];
        if(replay==replayMode.Replay) { animationStack.clear(); }
        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:
         	doDone(replay);
            break;
        case MOVE_FLIP:
          	 G.Assert((pickedObject==null),"can't flip; something is moving");
        	doFlip(m);
        	break;
        case MOVE_BOARD_BOARD:
        	// used by the robot
    		G.Assert((pickedObject==null),"something is moving");
        	switch(board_state)
        	{	default: throw G.Error("Not expecting move in state %s",board_state);
        		case PLAY_STATE:
        		case MSM_STATE:
           			doMove(m,replay);
           		    TruCell droppedDest = sm_dest[sm_step];
        			acceptPlacement();
               		int nsplit = getSplitMovesFrom(droppedDest,null);
            		int nmerge = getMergeMovesFrom(droppedDest,null);
            		if((nsplit>0)&&(nmerge>0)) { setState(TruchetState.STARTSM_STATE); }
            		else if(nsplit>0) { setState(TruchetState.STARTS_STATE); }
            		else if (nmerge>0) { setState(TruchetState.STARTM_STATE); }
            		else { setState(TruchetState.CONFIRM_STATE); }
            		m_focus = null;
            		s_focus = droppedDest;
        			break;
        	}
        	break;
        case MOVE_DROPB:
        	G.Assert(pickedObject!=null,"something is moving");
			switch(board_state)
			{ default: throw G.Error("Not expecting drop in state %s",board_state);
			  case MSM_STATE:
			  case S_STATE:
			  case STARTS_STATE:
			  case STARTSM_STATE:
			  case STARTM_STATE:
			  case M_STATE:
			  case M_DONE_STATE:
			  case SORM_STATE:
			  case PUZZLE_STATE:  break;
		      case DRAW_STATE:
			  case CONFIRM_STATE: unDropObject(); unPickObject(); break;
			  case PLAY_STATE:

				  break;
			}
            dropObject(TruId.BoardLocation,m.to_col, m.to_row);
            if(replay==replayMode.Single)
            {
            	animationStack.push(sm_source[sm_step]);
            	animationStack.push(getCell(TruId.BoardLocation,m.to_col, m.to_row));
            }
            if(sm_source[sm_step]==sm_dest[sm_step]) 
            	{ sm_source[sm_step]=sm_dest[sm_step]=null;
            	  sm_picked[sm_step]=null;
            	} 
            	else
           		{
            	moveRestOfStack(replay);
           		setNextStateAfterDrop(replay);
           		}
            
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	{
        		  switch(board_state)
        		  {	default:throw  G.Error("Not expecting pickb in state %s",board_state);
 
        		  case SORM_STATE:
        		  case S_STATE:
        		  case M_STATE:
        		  case M_DONE_STATE:
        		  case CONFIRM_STATE:
        			  TruCell dcell = getCell(m.from_col,m.from_row);
        	        	if(isDest(dcell))
        	        		{ // picked what we dropped, undo
        	        		unDropObject(); 
        	        		break;
        	        		}
        	        	// fall through
					//$FALL-THROUGH$
        		  case MSM_STATE:
        		  case STARTS_STATE:
        		  case STARTM_STATE:
        		  case STARTSM_STATE:
      		  	  case PLAY_STATE:
         		  case PUZZLE_STATE:
         		     // stay in the same state until we drop somewhere.
      		  		 pickObject(TruId.BoardLocation, m.from_col, m.from_row);
      		  		 break;      			  
        		  }
         		}
 
            break;

        case MOVE_DROP: // drop on chip pool;
        	TruCell c = getCell(m.object, m.to_col, m.to_row);
            dropObject(m.object, m.to_col, m.to_row);
            if(replay==replayMode.Single)
            {
            	animationStack.push(sm_source[sm_step]);
            	animationStack.push(c);
            }
            setNextStateAfterDrop(replay);

            break;

        case MOVE_PICK:
            pickObject(m.object, m.from_col, m.from_row);
            setNextStateAfterPick();
            break;

        case MOVE_START:
            setWhoseTurn(m.player);
            sm_step=0;
            sm_source[0]=sm_dest[0]=null;
            sm_picked[0]=null;
            s_focus = null;
            m_focus = null;
            flippedCell = null;
            setState(TruchetState.PUZZLE_STATE);

             {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
   
            break;

        case MOVE_RESIGN:
            setState(unresign==null?TruchetState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            setState(TruchetState.PUZZLE_STATE);

            break;
        case MOVE_AND_MERGE:
        	doMove(m,replay);
        	acceptPlacement();
			//$FALL-THROUGH$
		case MOVE_MERGE:
        	doMerge(m,replay);
        	setState(TruchetState.CONFIRM_STATE);
      		break;
        case MOVE_AND_SPLIT:
        	doMove(m,replay);
        	acceptPlacement();
			//$FALL-THROUGH$
		case MOVE_SPLIT:
        	doSplit(m,replay);
        	setState(TruchetState.CONFIRM_STATE);
      		break;
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;
        default:
        	cantExecute(m);
        }


        return (true);
    }

    public boolean LegalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
			return(false);
        case PUZZLE_STATE:
        	return(true);
        }
    }
  
    // true if it's legal to drop a chip  originating from fromCell on toCell
    public boolean LegalToDropOnBoard(TruCell fromCell,TruChip gobblet,TruCell toCell)
    {	
		return(false);

    }
    
    public boolean flippable(TruCell c)
    {	if(!c.isTileOnly()) {return(false); }
    	if(c.isEdgeCell()) { return(false); }
    	TruCell cm1 = c.exitTo(CELL_DOWN);
    	if((cm1==null) || !cm1.isTileOnly()) { return(false); }
    	TruCell cm2 = c.exitTo(CELL_LEFT);
    	if((cm2==null) || !cm2.isTileOnly()) { return(false); }
    	TruCell cm3 = c.exitTo(CELL_DOWN_LEFT);
    	if((cm3==null) || !cm3.isTileOnly()) {return(false); }
    	return(true);
     }
    public boolean LegalToHitIntersection(TruCell cell)
    {	boolean rval = false;
    	TruCell pickedSource = sm_source[sm_step];
    	TruCell droppedDest = (sm_step>0) ? sm_dest[sm_step-1] : null;

    	if(pickedSource==null)
    	{
    	if((sm_step>0) && (cell==sm_dest[sm_step-1])) { return(true); }
    	switch(board_state)
    	{
    	case PLAY_STATE:
    	case MSM_STATE:
       		if((cell.topChip()==playerChip[whoseTurn]) 
       			&& ((getRiverMovesFrom(cell,null)>0)
       				|| (getMergeMovesFrom(cell,null)>0)
       			    || (getSplitMovesFrom(cell,null)>0)))
       			{ rval = true; }
       		break;
    	case STARTSM_STATE:
    	case STARTS_STATE:
    	case STARTM_STATE:
     	case SORM_STATE:
     		if((cell==s_focus)||(cell==m_focus)) 
     			{ return(true); }
 			break;
    	case S_STATE:
    		rval = (cell==s_focus) || (cell==droppedDest);
    		break;
    	case M_DONE_STATE:
    	case M_STATE:
    		{Hashtable<TruCell,TruCell> h = mergeMoveSources();
    		if(h.get(cell)!=null) { rval=true; }
    		else { rval = cell==m_focus; }
    		}
    		break;
    	default: throw G.Error("Not expecting state %s",board_state);
    	case PUZZLE_STATE:
    		rval = cell.chipIndex>0;	// something to hit
    		break;
    	case GAMEOVER_STATE: 
    	case DRAW_STATE:
    	case RESIGN_STATE:
    		rval = false;
    		break;
    	case CONFIRM_STATE: 
    		G.Assert(sm_step>=0,"something moved");
    		rval = (cell==sm_dest[sm_step]);
    		break;
    	}
     }
    else
    {	if(cell==pickedSource) { return(true); }
    	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case DRAW_STATE:
    	case GAMEOVER_STATE: 
    		return(false);
    	case M_STATE:
    	case M_DONE_STATE:
    		return(cell==m_focus);
    	case S_STATE:
       		{
    			Hashtable<TruCell,TruCell> h = splitMoveDests();
    			return((h.get(cell)!=null));
    		}
    	case PLAY_STATE: // flip move split or join
    	case STARTSM_STATE:
    	case MSM_STATE:	 // move split or join
    	case SORM_STATE:
    	case STARTS_STATE:
    	case CONFIRM_STATE:
    	case STARTM_STATE:
    		{ Hashtable<TruCell,TruCell> moves = riverMoveDests();
    			  if(moves.get(cell)!=null) { return(true); }
    			}
    			{ Hashtable<TruCell,TruCell> moves = splitMoveDests();
    			  if(moves.get(cell)!=null) { return(true); }
    			}
    			{ Hashtable<TruCell,TruCell> moves = mergeMoveDests();
    			  if(moves.get(cell)!=null) { return(true); }
    			}
    			return(false);
    			
      	case PUZZLE_STATE:
    		rval = pickedSource.chipIndex<MAX_CHIP_HEIGHT*2;	// can drop anywhere
    	}
    }
   	return(rval);
   }
    // true if legal to hit this BOARD to flip it over.  The point appears at the
    // upped left corner
   public boolean LegalToHitBoard(TruCell cell)
    {	
	TruChip pickedObject = sm_picked[sm_step];
    switch (board_state)
        {

		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
		case DRAW_STATE:
			return(isDest(cell));
        default:
        	throw  G.Error("Not expecting state %s", board_state);
        case MSM_STATE:
        	return((pickedObject==null) 
        			&& ((cell == sm_source[0])||(cell==flippedCell)));
        case S_STATE:
        case STARTSM_STATE:
        case STARTS_STATE:
        case STARTM_STATE:
        case SORM_STATE:
        case M_STATE:
	    case M_DONE_STATE:
	    	return(false);
	   case PLAY_STATE:
       case PUZZLE_STATE:
        	return((pickedObject==null) && flippable(cell));
        }
    }
  public boolean canDropOn(TruCell cell)
  {	
  	TruCell pickedSource = sm_source[sm_step];
  	return((pickedSource!=null)				// something moving
  			&&(pickedSource.onBoard 
  					? (cell!=pickedSource)	// dropping on the board, must be to a different cell 
  					: (cell==pickedSource))	// dropping in the rack, must be to the same cell
  				);
  }
 
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(TruMovespec m)
    {
        m.state = board_state; //record the starting state. The most reliable
        m.undoFocus = s_focus;
        // to undo state transistions is to simple put the original state back.
        int lvl = sm_step;
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        //System.out.println("Ex "+m+" "+getCell('H',4).region_size);
        if (Execute(m,replayMode.Replay))
        {	switch(m.op)
        	{
        	case MOVE_FLIP:
        	case MOVE_BOARD_BOARD:
        		if(board_state==TruchetState.CONFIRM_STATE) { doDone(replayMode.Replay); }
        		break;
        	case MOVE_DONE:	break;
            default:
            	if (DoneState())
            	{
                doDone(replayMode.Replay);
            	}
            	else
            	{
            		throw G.Error("Robot move should be in a done state");
            	}
        	}
        }
        sm_step = lvl;
        sm_source[lvl] =null;
        sm_dest[lvl]=null;		// undo stack of destinations
        sm_picked[lvl]=null;
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(TruMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	int lvl = sm_step;
     	//System.out.println("un "+m+" "+getCell('H',4).region_size);
        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;
   	    case MOVE_FLIP:
   	    	{
   	    	TruCell c = getCell(m.from_col,m.from_row);
   	   	 	flipCell(c);
   	    	}
   	    	break;
        case MOVE_DONE:
            break;
        case MOVE_SPLIT:
        	undoSplit(m);
        	break;
        case MOVE_MERGE:
        	undoMerge(m);
        	break;
         case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
        		case PLAY_STATE:
        		case STARTS_STATE:
        		case STARTSM_STATE:
        		case SORM_STATE:
        		case STARTM_STATE:
        			undoMove(m);
        		    break;
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
		sm_step=lvl;
		flippedCell = null;
		sm_dest[lvl]=null;
		sm_source[lvl]=null;
		sm_picked[lvl]=null;
        s_focus = m.undoFocus;
        setState(m.state);
        if(m.player!=whoseTurn) 
        	{ moveNumber--; 
        	setWhoseTurn(m.player);
        	}
 }
    
// get the list of tiles that can be flipped.  If "all" is null then just get the count
 int getListOfFlipMoves(CommonMoveStack  all,int n)
 {	
	switch(board_state)
	{
	case PLAY_STATE:
		for(TruCell c = allCells; c!=null; c=c.next)
		{	if(flippable(c)) 
 			{ if(all!=null) { all.addElement(new TruMovespec(c.col,c.row,whoseTurn)); }
 			  n++;
 			}
		}
		break;
	default: break;
	}
	return(n); 
 }
 //
 // dests for river moves from "from" starting at "first"
 // if from is null initialize a new sweep
 // if dests is null just count
 //
 private int legalRiverMoveDests(TruCell from,TruCell first,TruCell dests[],int n,CommonMoveStack  all)
 {	 if(from==null) 
 		{ from = first; sweep_counter++; first.sweep_counter = sweep_counter; 
 		}
 		else
 		{ from.sweep_counter = first.sweep_counter;
 		}
	 for(int step = 0,direction = CELL_DOWN_LEFT;
	 	 step<4;
	 	 direction+=2,step++)
	 	{
		 TruCell dest = from.canMoveInDirection(direction);
		 if((dest!=null) && (dest.sweep_counter!=first.sweep_counter))
		 {	if(dests!=null)
			 { dests[n]=dest;
			 }
		 	if(all!=null)
		 	{	all.addElement(new TruMovespec(first.col,first.row,dest.col,dest.row,whoseTurn));
		 	}
		 	n++;
		 	n=legalRiverMoveDests(dest,first,dests,n,all);
		 }
	 	}
	 return(n);
 }
 //
 // dests for river moves from "from" starting at "first"
 // if from is null initialize a new sweep
 // if dests is null just count
 //
 private int markRegion(TruCell from,int n,int sz)
 {	 from.sweep_counter = sweep_counter;
 	 from.region_size=sz;
	 for(int step = 0,direction = CELL_DOWN_LEFT;
	 	 step<4;
	 	 direction+=2,step++)
	 	{
		 TruCell dest = from.riverInDirection(direction);
		 if((dest!=null) && (dest.sweep_counter!=sweep_counter))
		 {	
		 	n++;
		 	n=markRegion(dest,n,sz);
		 }
	 	}
	 return(n);
 }
 // fill the "size" cell of the region
 private int fillRegionSize(TruCell from,int sz)
 {
	sweep_counter++;
	return(markRegion(from,0,sz));
 }
 // invalidate the region size of all the cells associated with "from"
 // this is done for a cell that's about to flip
 private void invalidateRegionSize(TruCell flipped)
 {	
 	TruCell from = flipped.exitTo(CELL_DOWN_LEFT);
 	invalidateRegion(from);
 	invalidateRegion(flipped.exitTo(CELL_LEFT));
 }
 private void invalidateRegion(TruCell from)
 {	boolean moved = false;
 	for(int step = 0,direction = CELL_DOWN_LEFT;
 	 		step<4;
 	 		direction+=2,step++)
 		{
		 TruCell otherCell = from.exitTo(direction);	// cell that represents the desgination
		 if(otherCell!=null)
		 {
			 TruCell river = TruGameBoard.riverInDirectionOfCell(from,direction,otherCell);
			 if(river!=null)
			 {	if(!moved) { moved = true; fillRegionSize(river,-1); }
			 }
			 else { fillRegionSize(otherCell,-1); }
		 }
 		}
 }
 // mark the region associated with from with its proper size
 private void mark1RegionSize(TruCell from)
 { 	int n = fillRegionSize(from,-1);
 	int nn = fillRegionSize(from,n+1);
 	G.Assert(nn==n,"same size");
 }
 private void markRegionSizes(TruCell flipped)
 {	
 	TruCell from = flipped.exitTo(CELL_DOWN_LEFT);
	 markRegion(from);	// might have formed a single cell region
	 markRegion(flipped.exitTo(CELL_LEFT));
 }
 private void markRegion(TruCell from)
 {	boolean moved = false;
	 mark1RegionSize(from);
	 for(int step = 0,direction = CELL_DOWN_LEFT;
 	 		step<4;
 	 		direction+=2,step++)
 		{
		 TruCell otherCell = from.exitTo(direction);	// cell that represents the desgination
		 if(otherCell!=null)
		 {
			 TruCell river = TruGameBoard.riverInDirectionOfCell(from,direction,otherCell);
			 if(river!=null)
			 {	if(!moved) { moved = true; mark1RegionSize(river); }
			 }
			 else { mark1RegionSize(otherCell); }
		 }
 		}
 }
 // this is a complicated recursive and interative function which gets
 // a list of merge moves to a particular cell.  This is called intially
 // from some particular cell which initiates the merge, we then have to
 // look at the neighbors to find at least one partner, and we can't exceed
 // the maximum height of 4
 private int getMergeMovesTo
 (TruCell dest,		// The cell we split from
  int fromPlayer,	// whose stacks we can suck up
  int step,			// scan step, 1-3
  int direction,	// current direction
  int heightSoFar,	// how much height we're packing in so far
  CommonMoveStack  all,		// gets list of moves if not null
  String directionsSoFar)	// the combined directions we're from
 {	int n=0;
 	while(step<4)
 	{	TruCell c = dest.exitTo(direction);
 		int oldDirection = direction;
 		step++;
 		direction = (direction+2)%8;
 	
 		if(c!=null)
 		{	int thisHeight= c.chipIndex;
 			int totalHeight = thisHeight+heightSoFar;
	 		if((c.topChip()==playerChip[fromPlayer]) && (totalHeight<=4))
	 		{	String directions = directionsSoFar;
	 			while(thisHeight-->0) { directions += DIRECTION_NAMES[oldDirection]; }
	 			if((totalHeight>=dest.chipIndex) && (heightSoFar>0))
	 			{
		 			if(all!=null)
		 			{	all.addElement(new TruMovespec(MOVE_MERGE,dest.col,dest.row,directions,whoseTurn));
		 			}
		 			n++;
	 			}
	 			n += getMergeMovesTo(dest,fromPlayer,step,direction,totalHeight,all,directions);
	 			
	 		}
 		}
  	}
 	return(n);
 }
 


 // this is a complicated recursive and interative function which gets
 // a list of split moves which start from a particular cell.  The current
 // rules for splits are that the entire stack has to move, and must move
 // onto a stack that is empty or contains a shorter or equal height stack
 // of enemy pieces.
 private int getSplitMovesFrom
 (TruCell from,		// The cell we split from
  int from_height,	// the remaining height of chips stacked, initially at least 2
  int splitStep,	// how many stacks we're currently split into, starting with 0
  int dirStep,		// the direction number we're currently checking, 0-3
  int direction,	// the adjacency direction, initially CELL_LEFT
  CommonMoveStack  all,		// receives the result
  String original_str)		// as we recurse, is built into the split specifier
 {	
 	TruChip myChip = from.topChip();
	int last_from_height = (splitStep==0) ? from_height-1 : from_height;	// max we can put there.  If first step, we need to save 1
	int n=0;
	while(dirStep<4)
 	{
	TruCell destCell =from.exitTo(direction);			// the cell we're going to stack onto
	if((destCell!=null) && destCell.onBoard && (destCell.topChip()!=myChip))		// either empty of enemy
	{	int dest_height = destCell.chipIndex;
		int min_from_height = (dest_height==0) ? 1 : dest_height;
		for(int h = min_from_height; h<=last_from_height; h++)
			{	// try moving H cells to dest and continuing
				int remaining_height = from_height-h;
				String str = original_str;
				if(all!=null) { for(int x=0;x<h;x++) { str += DIRECTION_NAMES[direction]; }}
				if(remaining_height==0)
				{	// we found a way to split the stack and use all the chips
					G.Assert(splitStep>0,"something split");
					if(all!=null)
						{ TruMovespec splitm = new TruMovespec(MOVE_SPLIT,from.col,from.row,str,whoseTurn);
						  //System.out.println("spl: "+splitm);
						  all.addElement(splitm);
						}
					n++;
				}
				else
				{
				n += getSplitMovesFrom(from,remaining_height,splitStep+1,dirStep+1,direction+2,all,str);
				}
			}
	}
	dirStep++;
	direction= (direction+2)%8;	// move to the next position
 	}
	return(n);
 }
 private int getSplitMovesFrom(TruCell from,CommonMoveStack  all)
 {	TruCell pickedSource = sm_source[sm_step];
 	int from_height = from.chipIndex+((from==pickedSource)?1:0);
 	if(from_height>=2)
 		{ return(getSplitMovesFrom(from,from_height,0,0,CELL_LEFT,all,""));
 		}
 	return(0);
 }
 private int getMergeMovesFrom(TruCell from,CommonMoveStack  all)
 {	TruChip pickedObject = sm_picked[sm_step];
 	TruCell pickedSource = sm_source[sm_step];
 	TruChip myChip = (pickedObject==null) ? from.topChip() : pickedObject;
    int from_height = from.chipIndex+((from==pickedSource)?1:0);
 	int n = 0;
 	for(int step=0,direction = CELL_LEFT; step<4;step++,direction+=2)
 	{	TruCell dest = from.exitTo(direction);
 		if((dest!=null) && dest.onBoard && (dest.topChip()!=myChip))
 		{	int invdir = (direction+4)%8;
 			String nameso = "";
 			for(int i=0;i<from_height;i++) { nameso += DIRECTION_NAMES[invdir]; }
 			n+= getMergeMovesTo(dest,playerIndex(myChip),1,(invdir+2)%8,from_height,all,nameso);
 		}
 	}
	 return(n);
 }
 

 private int getRiverMovesFrom(TruCell c,CommonMoveStack  v)
 {	if(c.onBoard && (c.topChip()==playerChip[whoseTurn]))
 		{ return(legalRiverMoveDests(null,c,null,0,v));
 		}
 	return(0);
 }
 
 private int getListOfSplitMoves(CommonMoveStack  all,int n)
 {	
	switch(board_state)
	{
	default: break;
	case STARTS_STATE:
	case STARTSM_STATE:
	case S_STATE:
		G.Assert(s_focus!=null,"s focus");
		n += getSplitMovesFrom(s_focus,all);
		break;
	case PLAY_STATE:
	case MSM_STATE:
		for(TruCell c = allCells; c!=null; c=c.next)
		{	if(c.onBoard && c.chipIndex>0) 
			{ TruChip chip = c.topChip();
			  if(chip==playerChip[whoseTurn])
			  { n+= getSplitMovesFrom(c,all);
			  }
			}
	}
	}
 	return(n);
 }
 private int getListOfMergeMoves(CommonMoveStack  all,int n)
 {	
	switch(board_state)
	{
	default: break;
	case M_STATE:
	case STARTM_STATE:
	case STARTSM_STATE:
		G.Assert(s_focus!=null,"s focus for merge");
		n += getMergeMovesFrom(s_focus,all);
		break;
	case PLAY_STATE:
	case MSM_STATE:
		for(TruCell c = allCells; c!=null; c=c.next)
		{	if(c.onBoard && c.chipIndex>=0) 
			{ TruChip chip = c.topChip();
			  if(chip!=playerChip[whoseTurn])
			  { n+= getMergeMovesTo(c,whoseTurn,0,CELL_LEFT,0,all,"");
			  }
			}
	}
	}
 	return(n);
 }

 int getListOfRiverMoves(CommonMoveStack  all,int n)
 {	
	switch(board_state)
	{
	case PLAY_STATE:
	case MSM_STATE:
		{
		for(TruCell c = allCells; c!=null; c=c.next)
		{	if(c.chipIndex>0) 
 			{ TruChip chip = c.topChip();
  			  if(chip==playerChip[whoseTurn])
 				  {
  				  n = legalRiverMoveDests(null,c,null,n,all);
  				  }
 			}
		}
		}
		break;
	default: break;
	}
	return(n); 
 }
 boolean has_legal_moves()
 {
	 if(getListOfRiverMoves(null,0)>0) { return(true); }
	 if(getListOfSplitMoves(null,0)>0) { return(true); }
	 if(getListOfMergeMoves(null,0)>0) { return(true); }
	 return(false);
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
 	int n = 0;
 	n=getListOfFlipMoves(all,n);
 	n=getListOfRiverMoves(all,n);
 	n=getListOfSplitMoves(all,n);
 	n=getListOfMergeMoves(all,n);
 	switch(board_state)
 	{
 	case STARTS_STATE:
 	case STARTM_STATE:
 	case STARTSM_STATE:
 		{	all.addElement(new TruMovespec(MOVE_DONE,whoseTurn));
 		}
 		break;
 	case PLAY_STATE:
 	case MSM_STATE:
 		if(n==0) 
 			{ all.addElement(new TruMovespec(MOVE_RESIGN,whoseTurn));
 			}
 		break;
 	default: break;
 	}
  	return(all);
 }
 // temporary list of destination cells allocate as a resource for speed
 private TruCell[][]tempDestResource = new TruCell[6][];
 private int tempDestIndex=-1;
 public synchronized TruCell []getTempDest() 
 	{ if(tempDestIndex>=0) { return(tempDestResource[tempDestIndex--]); }
 	  return(new TruCell[((ncols-2)*(nrows*2))/2]);
 	}
 public synchronized void returnTempDest(TruCell[]d) { tempDestResource[++tempDestIndex]=d; }

 
 public Hashtable<TruCell,TruCell> splitMoveDests()
 {	Hashtable<TruCell,TruCell> dd = new Hashtable<TruCell,TruCell>();
 	TruCell pickedSource = sm_source[sm_step];
 	TruCell droppedDest = sm_dest[sm_step];
 	if((pickedSource!=null) && pickedSource.onBoard && (pickedSplitMoves!=null))
 	{
 	switch(board_state)
 	{
 	default: break;
 	case PLAY_STATE:
 	case MSM_STATE:
 	case STARTSM_STATE:
 	case STARTS_STATE:
 	case SORM_STATE:
 	case S_STATE:
 		for(int i=0;i<pickedSplitMoves.size();i++)
 		{	TruMovespec sp = (TruMovespec)pickedSplitMoves.elementAt(i);
 			TruCell c = getCell(sp.from_col,sp.from_row);
 			for(int step=0,direction=CELL_LEFT; step<4; direction+=2,step++)
 			{	char name = DIRECTION_NAMES[direction].charAt(0);
 				TruCell dcell = c.exitTo(direction);
 				String info = sp.splitInfo;
 				if((info.indexOf(name)>=0) && consistantWithBoard(sp,dcell,(droppedDest==null))) 
 				{ // we need to also determine if the destination is already fully used
 				  // and adding one more to this dest is consistant with the move in hand
 				  TruCell ex = c.exitTo(direction);
 				  dd.put(ex,ex); 
 				}
 			}
 		}
 	}
 	}
 	return(dd);
 }
 
 public Hashtable<TruCell,TruCell> mergeMoveDests()
 {	Hashtable<TruCell,TruCell> dd = new Hashtable<TruCell,TruCell>();
 	TruCell pickedSource = sm_source[sm_step];
 	if((pickedSource!=null) && pickedSource.onBoard && (pickedMergeMoves!=null))
 	{
 	switch(board_state)
 	{
 	default: break;
 	case M_DONE_STATE:
	case M_STATE:
		dd.put(m_focus,m_focus);
		break;
 	case PLAY_STATE:
 	case MSM_STATE:
 	case STARTSM_STATE:
 	case STARTM_STATE:
 	case SORM_STATE:
  		for(int i=0;i<pickedMergeMoves.size();i++)
 		{	TruMovespec sp = (TruMovespec)pickedMergeMoves.elementAt(i);
 			TruCell c = getCell(sp.to_col,sp.to_row);
 			dd.put(c,c);
 		}
 	}
 	}
 	return(dd);
 }
 public Hashtable<TruCell,TruCell> mergeMoveSources()
 {	Hashtable<TruCell,TruCell> dd = new Hashtable<TruCell,TruCell>();
 	TruCell pickedSource = sm_source[sm_step];
 	if((pickedSource==null) && (m_focus!=null))
 	{
 	switch(board_state)
 	{
 	case PLAY_STATE:
 	case MSM_STATE:
 	case SORM_STATE:
	default: break;
 	case STARTSM_STATE:
 	case M_DONE_STATE:
 	case M_STATE:
 		{
 		TruCell c = m_focus;
 		int topH = c.topHeight(playerColor[whoseTurn]);
// 		int botH = c.bottomHeight();
 		for(int step=0,direction=CELL_LEFT; step<4; direction+=2,step++)
 			{	TruCell n = c.exitTo(direction);
 				TruChip tc = (n==null) ? null : n.topChip();
 				if(tc==playerChip[whoseTurn]) 
 				{ if((n.topHeight(playerColor[whoseTurn])+topH)<=MAX_CHIP_HEIGHT) 
 					{ dd.put(n,n); 
 					}
 				}
 			}
 		}
 	}}
 	return(dd);
 }
 // get the list of moves from the current picked source
 public Hashtable<TruCell,TruCell>  riverMoveDests()
 {	return(riverMoveDests(sm_source[sm_step]));
 }
 public Hashtable<TruCell,TruCell>  riverMoveDests(TruCell cell)
 {	Hashtable<TruCell,TruCell>  dd = new Hashtable<TruCell,TruCell> ();
  	if((cell!=null) && cell.onBoard)
 	{	switch(board_state)
	 	{
		default: break;
		case PLAY_STATE:
		case MSM_STATE:
		case STARTS_STATE:
		case STARTM_STATE:
		case STARTSM_STATE:
		  TruCell tempDests[]=getTempDest();
 		  int dests = legalRiverMoveDests(null,cell,tempDests,0,null);
  		  for(int i=0;i<dests;i++) { dd.put(tempDests[i],tempDests[i]); }
  		  returnTempDest(tempDests);
  	  	}
 	}
 return(dd);
 }
 
 public int countInDirection(String mspec,int direction)
 {	int len = mspec.length();
 	String dname = DIRECTION_NAMES[direction];
 	char dc = dname.charAt(0);
 	int count = 0;
 	G.Assert(dname.length()==1,"primary directions only");
 	for(int i=0;i<len;i++)
 	{	if(mspec.charAt(i)==dc) { count++; }
 	}
 	return(count);
 }
 boolean consistantWithBoard(TruMovespec m,TruCell nextDest,boolean somepicked)
 {	switch(m.op)
	{
	default: throw G.Error("not expecting %s",m);
	case MOVE_SPLIT:
		{	TruCell c = getCell(m.from_col,m.from_row);
			int center_height = (somepicked ? 1 : 0) + c.topHeight(playerColor[m.player]);
			int play_height = m.splitInfo.length();
			int distributed_height = 0;
 			for(int step=0,direction=CELL_LEFT; step<4; direction+=2,step++)
 			{	TruCell dcell = c.exitTo(direction);
 				// intended height at the destination
 				int boostHeight = ((nextDest==dcell)?1:0);
 				int dheight = (dcell==null) ? 0 : (boostHeight+dcell.topHeight(playerColor[m.player]));
 				int mheight = countInDirection(m.splitInfo,direction);
  				if(mheight>0) 
 					{ if(dheight>mheight) 
 						{return(false); 
 						}
 					  distributed_height += dheight;
 					  center_height -= boostHeight;
 					}
 			
  			}
 			return((center_height + distributed_height) == play_height);
		}
		
	}
  }
  static final int colorInDirection(TruChip chip,int dir)
	{	int colorInfo[]=chip.colorInfo;
		if(colorInfo!=null)
		{	dir = (dir+8)&7;
			switch(dir)
			{	
			default: throw G.Error("Bad direction");
			case CELL_LEFT: return((colorInfo[3]<<2) | colorInfo[0]);
			case CELL_UP: return((colorInfo[0]<<2) | colorInfo[1]);
			case CELL_RIGHT: return((colorInfo[1]<<2) | colorInfo[2]);
			case CELL_DOWN: return((colorInfo[2]<<2) | colorInfo[3]);
			}
		}
		return((TruChip.N<<2) | TruChip.N);
	}
	static final int colorInInverseDirection(TruChip chip,int dir)
	{	int colorInfo[] = chip.colorInfo;
		if(colorInfo!=null)
		{	dir = (dir+8)&7;
			switch(dir)
			{	
			case CELL_LEFT: return((colorInfo[0]<<2) | colorInfo[3]);
			case CELL_UP: return((colorInfo[1]<<2) | colorInfo[0]);
			case CELL_RIGHT: return((colorInfo[2]<<2) | colorInfo[1]);
			case CELL_DOWN: return((colorInfo[3]<<2) | colorInfo[2]);
			default:
				break;
			}
		}
		return((TruChip.N<<2) | TruChip.N);
	}
	
	static final boolean connectsInDirection(TruChip chip,int dir)
	{	int colorInfo[] = chip.colorInfo;
		if(colorInfo!=null)
		{
		dir = (dir+8)&7;
		switch(dir)
		{	
		default: throw G.Error("Bad direction");
		case CELL_DOWN_LEFT: 
			boolean as1 = colorInfo[1]==colorInfo[4];
			return(as1);
		case CELL_UP_LEFT: 
			boolean as2 = colorInfo[2]==colorInfo[4];
			return(as2);
		case CELL_UP_RIGHT: 
			boolean as3 = colorInfo[3]==colorInfo[4];
			return(as3);
		case CELL_DOWN_RIGHT: 
			boolean as4 = colorInfo[0]==colorInfo[4];
			return(as4);
		}
		}
		return(false);
	}
	
	static final public TruCell riverInDirectionOfCell(TruCell thisCell,int dir,TruCell otherCell)
	{	if(otherCell!=null)
		{
		switch(dir)
		{
		default: throw G.Error("Bad Direction");
		case CELL_DOWN_LEFT:
			if(connectsInDirection(thisCell.chipAtIndex(0),dir)) 
				{ connectsInDirection(thisCell.chipAtIndex(0),dir);
				return(otherCell); 
				}
			break;
		case CELL_UP_RIGHT:
				{	
				TruCell adjCell = thisCell.exitTo(CELL_UP_RIGHT);
				if(adjCell!=null) 
					{ if(connectsInDirection(adjCell.chipAtIndex(0),dir))
						{ connectsInDirection(adjCell.chipAtIndex(0),dir);
						return(otherCell);
						}
					}
				break;
				}

		case CELL_DOWN_RIGHT:
			{	TruCell adjCell = thisCell.exitTo(CELL_RIGHT);
				if(adjCell!=null) 
					{ if(connectsInDirection(adjCell.chipAtIndex(0),dir))
						{ connectsInDirection(adjCell.chipAtIndex(0),dir);
						return(otherCell);
						}
					}
				break;
			}
		case CELL_UP_LEFT:
			{	TruCell adjCell = thisCell.exitTo(CELL_UP);
			if(connectsInDirection(adjCell.chipAtIndex(0),dir)) 
				{ connectsInDirection(adjCell.chipAtIndex(0),dir);
				return(otherCell);
				}
			break;
			}
		}}
		return(null);
	}
 }
