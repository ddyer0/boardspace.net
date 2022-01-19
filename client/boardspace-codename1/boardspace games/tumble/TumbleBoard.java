package tumble;

import java.util.Hashtable;
import lib.*;
import online.game.*;


/**
 * TumbleBoard knows all about the game of TumblingDown, which is played
 * on a 8x8 board. It gets a lot of logistic support from 
 * common.rectBoard, which knows about the coordinate system.  
 * 
 * @author ddyer
 *
 */

class TumbleBoard extends rectBoard<TumbleCell> implements BoardProtocol,TumbleConstants
{
	private TumbleState unresign;
	private TumbleState board_state;
	public TumbleState getState() {return(board_state); }
	public void setState(TumbleState st) 
	{ 	unresign = (st==TumbleState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public int boardSize = DEFAULT_BOARDSIZE;	// size of the board
    public void SetDrawState() { setState(TumbleState.DRAW_STATE); }
    public TumbleCell blackChip = null;
    public TumbleCell whiteChip = null;
    public TumbleCell playerChipPool[] = new TumbleCell[2];
    //
    // private variables
    //
    private int chips_on_board = 0;			// number of chips currently on the board
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public TumbleChip pickedObject = null;
    private TumbleCell pickedSource = null;
    private int pickedHeight = 0;
    private TumbleCell droppedDest = null;
    public CellStack animationStack = new CellStack(); 
    public int pickKingHeight[] = {0,0};
    public int pickKingCount[]={0,0};
    public int dropKingHeight[] = {0,0};
    public int dropKingCount[]={0,0};
    public int stacksControlled[]={0,0};
    public int potentialKings[]={0,0};
    public TumbleCell[][]pickKings = null;
    public TumbleCell[][]dropKings = null;
    public TumbleChip playerChip[]=new TumbleChip[2];
    private TumbleCell[][]tempDestResource = new TumbleCell[4][];
    private int tempDestIndex=-1;
    public synchronized TumbleCell []getTempDest() 
    	{ if(tempDestIndex>=0) { return(tempDestResource[tempDestIndex--]); }
    	  return(new TumbleCell[(ncols+1)*3]);
    	}
    
    public TumbleCell getCell(TumbleCell c) { return((c==null)?null:getCell(c.rackLocation(),c.col,c.row)); }
    public synchronized void returnTempDest(TumbleCell[]d) { tempDestResource[++tempDestIndex]=d; }

	// factory method
	public TumbleCell newcell(char c,int r)
	{	return(new TumbleCell(c,r,TumbleId.BoardLocation));
	}
    public TumbleBoard(String init,int map[]) // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = TUMBLEGRIDSTYLE; //coordinates left and bottom
    	setColorMap(map);
        doInit(init); // do the initialization 
        autoReverseY();		// reverse_y based on the color map
     }
    public TumbleBoard cloneBoard() 
	{ TumbleBoard dup = new TumbleBoard(gametype,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((TumbleBoard)b); }

    // standared init for Hex.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard(String game)
    { 	if(Tumble_INIT.equalsIgnoreCase(game)) 
    		{ boardSize=DEFAULT_BOARDSIZE; 
     		}
    	else { throw G.Error(WrongInitError,game); }
        gametype = game;
        setState(TumbleState.PUZZLE_STATE);
        initBoard(boardSize,boardSize); //this sets up the board and cross links
        
        // temporary, fill the board
        for(TumbleCell c = allCells; c!=null; c=c.next)
        {  int i = ((c.row+c.col)%2)^1;
           c.addChip(TumbleChip.getTile(i));
           
        }
        int tumble_inits[][] = {{2,3,4,3,2,1},{1,2,3,2,1},{0,1,2,1},{0,0,1}};
   		TumbleChip wc = TumbleChip.getChip(FIRST_PLAYER_INDEX);
		TumbleChip bc = TumbleChip.getChip(SECOND_PLAYER_INDEX);
		int map[]=getColorMap();
		playerChip[map[FIRST_PLAYER_INDEX]] = wc;
		playerChip[map[SECOND_PLAYER_INDEX]] = bc;
        for(int row=1,invrow=nrows; row<=tumble_inits.length;row++,invrow--)
        {	int inits[]=tumble_inits[row-1];
        	for(int colnum=0,invcol=ncols-1; colnum<inits.length;colnum++,invcol--)
        	{	TumbleCell BC=getCell((char)('A'+colnum),row);
        		TumbleCell WC=getCell((char)('A'+invcol),invrow);
        		int count = inits[colnum];
        		for(int j=0;j<count;j++)
        		{	BC.addChip(bc);
        			WC.addChip(wc);
        		}
        	}
        
        	
        }
        
        
        whoseTurn = FIRST_PLAYER_INDEX;
        chips_on_board = 0;

    }
    public void sameboard(BoardProtocol f) { sameboard((TumbleBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(TumbleBoard from_b)
    {
    	super.sameboard(from_b);

        G.Assert(TumbleCell.sameCell(pickedSource,from_b.pickedSource),"pickedSource matches");
        G.Assert(TumbleCell.sameCell(droppedDest,from_b.droppedDest),"droppedDest matches");
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject matches");

    }

    /** this is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game.  Other site machinery looks for duplicate digests.
     * @return
     */
    public long Digest()
    {
    	long v = super.Digest();

        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        Random r = new Random(64 * 1000); // init the random number generator
        
		v ^= cell.Digest(r,pickedSource);
		v ^= chip.Digest(r,pickedObject);
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
        return (v);
    }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(TumbleBoard from_b)
    {	super.copyFrom(from_b);

        chips_on_board = from_b.chips_on_board;
        pickedObject = from_b.pickedObject;
        pickedSource = getCell(from_b.pickedSource);
        droppedDest = getCell(from_b.droppedDest);
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        
        sameboard(from_b);
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {	randomKey = key;
    	Random r = new Random(6873467);
    	animationStack.clear();
    	int map[]=getColorMap();
    	blackChip = new TumbleCell(r,'@',TumbleChip.BLACK_CHIP_INDEX,TumbleId.Black_Chip_Pool);
    	blackChip.addChip(TumbleChip.getChip(TumbleChip.BLACK_CHIP_INDEX));
    	whiteChip = new TumbleCell(r,'@',TumbleChip.WHITE_CHIP_INDEX,TumbleId.White_Chip_Pool);
    	whiteChip.addChip(TumbleChip.getChip(TumbleChip.WHITE_CHIP_INDEX));
    	playerChipPool[map[FIRST_PLAYER_INDEX]] = whiteChip;
    	playerChipPool[map[SECOND_PLAYER_INDEX]] = blackChip;
  
        Init_Standard(gtype);
        allCells.setDigestChain(r);
      	dropKings=new TumbleCell[2][ncols*nrows];
    	pickKings=new TumbleCell[2][ncols*nrows];
        markKings(true);
        markKings(false);
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


    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(TumbleState.GAMEOVER_STATE);
    }

    // look for a win for player.  This algorithm should work for Gobblet Jr too.
    public double ScoreForPlayer(int player,boolean print,double stack_weight,double king_weight,boolean dumbot)
    {  	double finalv=0.0;
    	finalv = stacksControlled[player]*stack_weight+potentialKings[player]*king_weight;
    	if(print) { System.out.println("stacks "+stacksControlled[player]*stack_weight+" kings "+potentialKings[player]*king_weight); }
     	return(finalv);
    }


    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement()
    {	
        pickedObject = null;
        droppedDest = null;
        pickedSource = null;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    TumbleCell dr = droppedDest;
    if(dr!=null)
    	{
    	droppedDest = null;
    	switch(board_state)
    	{
    	default:
    		dr.removeTop(pickedObject);
    		break;
    	case CONFIRM_STATE:
    	case DRAW_STATE:
    		undoDrop(pickedHeight,pickedSource.col,pickedSource.row,dr.col,dr.row);
    		pickedSource.removeTop();
    		break;
    	}
    	markKings(true);
    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	TumbleChip po = pickedObject;
    	if(po!=null)
    	{
    	TumbleCell ps = pickedSource;
    	pickedSource=null;
    	pickedObject = null;
    	if(ps.onBoard) { ps.addChip(po); }
    	markKings(true);
    	}
     }
    
    private void undoDrop(int height,char from_col,int from_row,char to_col,int to_row)
    {	int originalHeight = height;
		TumbleCell src = getCell(from_col,from_row);
		int direction = findDirection(from_col,from_row,to_col, to_row);
		TumbleCell dest = src.exitTo(direction);
		while(originalHeight > 0)
			{	TumbleCell next = dest.exitTo(direction);
			    if(next==null) break;
			    originalHeight--;
			    src.addChip(dest.removeTop());
			    dest = next;
			}
		if(originalHeight>0)
			{	// hit the wall, remove several chips
				int top = dest.chipIndex;
				for(int i=top-originalHeight+1;i<=top;i++)
				{	src.addChip(dest.chipAtIndex(i));
				}
				dest.chipIndex -= originalHeight;
			}
		G.Assert(src.chipIndex==height,"got them back");
	
    }
    // 
    // drop the floating object.
    //
    private void dropObject(TumbleId dest, char col, int row,replayMode replay)
    {
       G.Assert((pickedObject!=null)&&(droppedDest==null),"ready to drop");
       switch (dest)
        {
        default: throw G.Error("Not expecting dest %s",dest);
        case Black_Chip_Pool:
        	droppedDest = blackChip;
        	break;
        case White_Chip_Pool:
        	droppedDest = whiteChip;
        	break;
        case BoardLocation: // an already filled board slot.
        	droppedDest = getCell(col,row);
        	switch(board_state)
        	{default: throw G.Error("not expecting drop");
        	case PUZZLE_STATE:
        		
        		droppedDest.addChip(pickedObject);
        		break;
        	case PLAY_STATE:
        		if(droppedDest==pickedSource) { droppedDest=null; unPickObject(); }
        		else
        		{
        		int dir = findDirection(pickedSource.col,pickedSource.row,col,row);
        		pickedSource.addChip(pickedObject);
        		TumbleCell c = pickedSource.exitTo(dir);;
        		int finalHeight = pickedSource.chipIndex;
        		
        		for(int i=1;i<=finalHeight;i++)
        		{	c.addChip(pickedSource.chipAtIndex(i)); 
        			if(replay!=replayMode.Replay)
        			{
        				animationStack.push(pickedSource);
        				animationStack.push(c);
        			}
        			TumbleCell next = c.exitTo(dir);
        			if(next!=null) 
        				{ c=next;
        				}
         		}
        		pickedSource.chipIndex=0;
        		}
        		markKings(false);
        		break;
        	}
        	break;
        }
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(TumbleCell cell)
    {	return(droppedDest==cell);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    {	TumbleChip ch = pickedObject;
    	if((ch!=null)&&(droppedDest==null))
    		{ return(ch.chipNumber);
    		}
        return (NothingMoving);
    }
    public TumbleCell getCell(TumbleId source,char col,int row)
    {
    	   
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	return(getCell(col,row));
        case Black_Chip_Pool:
        	return(blackChip);
        case White_Chip_Pool:
        	return(whiteChip);
        }
	
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(TumbleId source, char col, int row)
    {	G.Assert((pickedObject==null)&&(pickedSource==null),"ready to pick");
    
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);

        case BoardLocation:
         	{
            markKings(true);
        	TumbleCell c = pickedSource = getCell(col,row);
        	pickedHeight = c.chipIndex;
         	pickedObject = c.removeTop();
         	break;
         	}
        case Black_Chip_Pool:
        	pickedSource = blackChip;
        	pickedObject = blackChip.topChip();
        	break;
        case White_Chip_Pool:
        	pickedSource = whiteChip;
        	pickedObject = whiteChip.topChip();
        	break;
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
        case CONFIRM_STATE:
        case DRAW_STATE:
        	if(droppedDest!=null) { setNextStateAfterDone(); }
        	break;
        case PLAY_STATE:
			setState(TumbleState.CONFIRM_STATE);
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
    public boolean isSource(char col, int row)
    {
        return ((pickedSource!=null) 
        		&& (pickedSource.col == col) 
        		&& (pickedSource.row == row));
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
        	setState(TumbleState.PLAY_STATE);
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
    	default:throw  G.Error("Not expecting state %s",board_state);
    	
    	case GAMEOVER_STATE: 
    		break;

        case DRAW_STATE:
        	setGameOver(false,false);
        	break;
    	case CONFIRM_STATE:
    	case PUZZLE_STATE:
    	case PLAY_STATE:
    		setState(TumbleState.PLAY_STATE);
    		break;
    	}

    }
   
    private boolean kingWasCaptured(int who)
    {
    	if(dropKingHeight[who]==0) 
    		{ return(true); }
    	if(dropKingHeight[who]>pickKingHeight[who]) { return(false); }
    	TumbleCell kings[]=pickKings[who];
    	for(int i=0;i<pickKingCount[who]; i++)
    		{	TumbleCell c =kings[i];
    			if(!c.isAKing && (c.chipIndex>0)) 
    				{ return(true); 
    				}
    		}
    	return(false);
    }
    
    private void doDone()
    {	
        acceptPlacement();

        if (board_state==TumbleState.RESIGN_STATE)
        {	setGameOver(false,true);
        }
        else
        {	int nextp = nextPlayer[whoseTurn];
        	if(kingWasCaptured(nextp))
        	{	setGameOver(true,false); 
        	}
        	else if(kingWasCaptured(whoseTurn)) 
        	{	setGameOver(false,true);	// suicide
        	}
        	else {setNextPlayer(); setNextStateAfterDone(); }
         }
    }
    
    public void markKings(boolean pick)
    {	int kingHb = 0;
    	int kingHw = 0;
    	int kingHeight[] = pick ? pickKingHeight : dropKingHeight;
    	int kingCount[] = pick ? pickKingCount : dropKingCount;
    	int nKb=0;
    	int nKw=0;
    	TumbleCell tempb[] = pick ? pickKings[whoseTurn] : dropKings[whoseTurn];
    	TumbleCell tempw[] = pick ? pickKings[nextPlayer[whoseTurn]] : dropKings[nextPlayer[whoseTurn]];
    	AR.setValue(stacksControlled,0);
    	AR.setValue(potentialKings, 0);
    	TumbleChip myChip = playerChip[whoseTurn];
    	for(TumbleCell c = allCells; c!=null; c=c.next)
    	{	c.isAKing = false;
    		TumbleChip topChip = (c==pickedSource) ? pickedObject : c.topChip();
    		boolean isKing = topChip.isChip();
    		int topIndex = c.chipIndex - ((c==pickedSource)?0:1); 
    		int owner = (topChip==myChip)
           					?whoseTurn
           					:nextPlayer[whoseTurn];
    		if(isKing) 
    			{ stacksControlled[owner]++; 
    			}	// count a stack we control
    		for(int i=topIndex; isKing && (i>0); i--)
    		{	TumbleChip chip = c.chipAtIndex(i);
    			isKing &= (chip==topChip);
    		}
    		if(isKing)
    		{	topIndex++;
    			if(topChip==myChip)
    			{
    				potentialKings[owner]++;	// count a pure stack
    				if(topIndex>=kingHb)
    				{
        			if(topIndex>kingHb) { nKb=0; kingHb=topIndex; }
        			tempb[nKb++] = c;
    				}}
    			else 
    				{
    				potentialKings[owner]++;	// count a pure stack
    				if(topIndex>=kingHw)
    				{
        			if(topIndex>kingHw) { nKw=0; kingHw=topIndex; }
        			tempw[nKw++] = c;
    				}}
    		}
    	}
    	for(int i=0;i<nKb;i++)
    	{	tempb[i].isAKing=true;
    	}
    	for(int i=0;i<nKw;i++)
    	{	tempw[i].isAKing=true;
    	}
    	kingHeight[whoseTurn]=kingHb;
    	kingCount[whoseTurn]=nKb;
    	kingHeight[nextPlayer[whoseTurn]]=kingHw;
    	kingCount[nextPlayer[whoseTurn]]=nKw;
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	TumbleMovespec m = (TumbleMovespec)mm;
    	if(replay==replayMode.Replay) { animationStack.clear(); }
        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:
         	doDone();
            break;
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case PLAY_STATE:
        			G.Assert((pickedObject==null)&&(droppedDest==null),"something is moving");
        			pickObject(TumbleId.BoardLocation, m.from_col, m.from_row);
           			m.originalHeight = pickedSource.chipIndex+1;	// save original height for unexecute
           				// also used by the sound generator
           		 	dropObject(TumbleId.BoardLocation,m.to_col,m.to_row,replay); 
 				    setNextStateAfterDrop();
        			break;
        	}
        	break;
        case MOVE_DROPB:
        	G.Assert(pickedObject!=null,"something is moving");
    		m.originalHeight = pickedSource.chipIndex+1;	// save original height for unexecute
				// also used by the sound generator
       	
			switch(board_state)
			{ default: throw G.Error("Not expecting drop in state %s",board_state);
			  case PUZZLE_STATE:  break;
		      case DRAW_STATE:
			  case CONFIRM_STATE: unDropObject(); unPickObject(); break;
			  case PLAY_STATE:

				  break;
			}
            dropObject(TumbleId.BoardLocation, m.to_col, m.to_row,replay);
            if(pickedSource==droppedDest) 
            	{ unDropObject(); 
            	  unPickObject(); 
            	  m.originalHeight=0;
            	} 
            	else
            		{
            		setNextStateAfterDrop();
            		}

            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	if(isDest(getCell(m.from_col,m.from_row)))
        		{ unDropObject(); 
        		  setState(TumbleState.PLAY_STATE);
        		}
        	else 
        		{ pickObject(TumbleId.BoardLocation, m.from_col, m.from_row);
        			// if you pick up a gobblet and expose a row of 4, you lose immediately
        		  switch(board_state)
        		  {	default: throw G.Error("Not expecting pickb in state %s",board_state);
        		  	case PLAY_STATE:
        		  		// if we pick a piece off the board, we might expose a win for the other player
        		  		// and otherwise, we are comitted to moving the piece
         		  		break;
        		  	case PUZZLE_STATE:
        		  		break;
        		  }
         		}
 
            break;

        case MOVE_DROP: // drop on chip pool;
            dropObject(m.source, m.to_col, m.to_row,replay);
            setNextStateAfterDrop();

            break;

        case MOVE_PICK:
            unDropObject();
            unPickObject();
            pickObject(m.source, m.from_col, m.from_row);
            setNextStateAfterPick();
            break;

         case MOVE_START:
            setWhoseTurn(m.player);
            pickedSource = droppedDest = null;
            pickedObject = null;
            setState(TumbleState.PUZZLE_STATE);

            {	boolean win1 = WinForPlayer(whoseTurn);
            	boolean win2 = WinForPlayer(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;

        case MOVE_RESIGN:
             setState(unresign==null?TumbleState.RESIGN_STATE:unresign);
             break;
        case MOVE_EDIT:
    		acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(TumbleState.PUZZLE_STATE);

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
        	throw G.Error("Not expecting state %s", board_state);
         case CONFIRM_STATE:
         case DRAW_STATE:
         case PLAY_STATE: 
         case RESIGN_STATE:
        	return(false);


		case GAMEOVER_STATE:
			return(false);
        case PUZZLE_STATE:
        	return((pickedObject==null)?true:(playerChip[player]==pickedObject));
        }
    }
  
    // true if it's legal to drop gobblet  originating from fromCell on toCell
    public boolean LegalToDropOnBoard(TumbleCell fromCell,TumbleChip gobblet,TumbleCell toCell)
    {	if(fromCell==toCell) { return(true); }
      	TumbleCell temp[] = getTempDest();
    	int n = getDestsFrom(fromCell,temp);
    	boolean ok = false;
    	for(int i=0;(i<n) && !ok;i++) { if(temp[i]==toCell) { ok = true; } }
    	returnTempDest(temp);
    	return(ok);
    }
    public boolean LegalToHitBoard(TumbleCell cell)
    {	
        switch (board_state)
        {
 		case PLAY_STATE:
			if(pickedSource==null)
			{
				return(cell.topChip()==playerChip[whoseTurn]);
			}
			return(LegalToDropOnBoard(pickedSource,pickedObject,cell));

		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
		case DRAW_STATE:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);

        case PUZZLE_STATE:
        	return(pickedObject==null?(cell.chipIndex>0):true);
        }
    }
  public boolean canDropOn(TumbleCell cell)
  {	
  	return((pickedObject!=null)				// something moving
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
    public void RobotExecute(TumbleMovespec m)
    {
        m.state = board_state; //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        if (Execute(m,replayMode.Replay))
        {
            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {
                doDone();
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
    public void UnExecute(TumbleMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);

        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;

        case MOVE_DONE:
            break;
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
        		case PLAY_STATE:
        			G.Assert((pickedObject==null)&&(droppedDest==null),"something is moving");
        			undoDrop(m.originalHeight,m.from_col,m.from_row,m.to_col,m.to_row);
         			break;
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(m.state);
        if(whoseTurn!=m.player)
        	{
        	moveNumber--;
        	setWhoseTurn(m.player);
        	}
 }
 int getDestsFrom(TumbleCell from,TumbleCell temp[])
 {	int n=0;
 	int distance = from.chipIndex + ((from==pickedSource) ? 1 : 0);
 	for(int direction=0;direction<8;direction++)
 	{	TumbleCell c = from.exitTo(direction);
 		int dis = 1;
 		while((c!=null) && dis<=distance) 
 		{	if(temp!=null) { temp[n]=c; }
 			n++;
 			c=c.exitTo(direction);
 			dis++;
 		}
 	}
 	return(n);
 }
 
 // get a hash of moves destinations, used by the viewer
 // to know where to draw dots.
 public Hashtable<TumbleCell,TumbleCell> getMoveDests()
 {	Hashtable<TumbleCell,TumbleCell> dd = new Hashtable<TumbleCell,TumbleCell>();
 	if(droppedDest!=null) 
 	{ dd.put(droppedDest,droppedDest); 
 	  dd.put(pickedSource,pickedSource);
 	}
 	else
 	if((pickedSource!=null)&&(pickedSource.onBoard))
 	{	TumbleCell[] temp = getTempDest();
 		int n = getDestsFrom(pickedSource,temp);
 		for(int i=0;i<n;i++) { dd.put(temp[i],temp[i]); }
 		dd.put(pickedSource,pickedSource);
 		returnTempDest(temp);
 		
 	}
	 return(dd);
 }
 
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
 	TumbleChip target = playerChip[whoseTurn];
 	for(TumbleCell c = allCells; c!=null; c=c.next)
 	{	if(c.topChip()==target)
 		{	for(int dir=0;dir<8;dir++)
 			{ TumbleCell d = c.exitTo(dir);
 			  if(d!=null) {  all.addElement(new TumbleMovespec(c.col,c.row,d.col,d.row,whoseTurn)); }
 			}
 		}
 	}
  	return(all);
 }
 
}
