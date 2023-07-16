package fanorona;

import bridge.Color;
import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;

/**
 *  
 * FanoronaBoard knows all about the game of Fanorona, which is played
 * on a 9x5 board. It gets a lot of logistic support from 
 * common.rectBoard, which knows about the coordinate system.  
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

class FanoronaBoard extends rectBoard<FanoronaCell> implements BoardProtocol,FanoronaConstants
{
	
	static final int MAX_MOVES_PER_TURN = 20;	// arbitrary, but 10 is possible..
	static final int DEFAULT_COLUMNS = 9;	// 9x5 board
	static final int DEFAULT_ROWS = 5;	//
    static final String[] FANORONAGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

	FanoronaState unresign;
	FanoronaState board_state;
	public FanoronaState getState() {return(board_state); }
	public void setState(FanoronaState st) 
	{ 	unresign = (st==FanoronaState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
    public int boardColumns = DEFAULT_COLUMNS;	// size of the board
    public int boardRows = DEFAULT_ROWS;
    public void SetDrawState() 
    	{ setState(FanoronaState.DRAW_STATE); 
     	}
    public FanoronaCell[] fChips=new FanoronaCell[2];
    //
    // private variables
    //
	private int sweep_counter=0;
    public int chips_on_board[] = new int[2];	// number of chips currently on the board
    public int point_strength[] = new int[2];	// sum of the number of connections of all chips on board
    public CellStack animationStack = new CellStack();
                           
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public FanoronaChip pickedObject = null;	// the object in the air
    public FanoronaCell unDropped = null;		// single undropped location
    public FanoronaCell pickedSource[] = new FanoronaCell[MAX_MOVES_PER_TURN];
    public FanoronaCell droppedDest[]=new FanoronaCell[MAX_MOVES_PER_TURN];
    public FanoronaCell lastCaptured[]=new FanoronaCell[MAX_MOVES_PER_TURN];
    public int stackIndex = 0;
    public int robotDepth = 0;
    int lastProgressMove = 0;		// last move where a pawn was advanced
    int lastDrawMove = 0;			// last move where a draw was offered

    private StateStack robotState = new StateStack();
    private CellStack robotCapture = new CellStack();
    public IStack robotIndex = new IStack();
    public FanoronaChip playerChip[] = {FanoronaChip.getChip(0), FanoronaChip.getChip(1) };
    CellStack undoStack = new CellStack();
    /** get all the cells in the current move stack.  This is used by the
     * viewer to present a move trail
     */
    public Hashtable<FanoronaCell,String> stackOrigins()
    {	Hashtable<FanoronaCell,String> h = new Hashtable<FanoronaCell,String>();
    	for(int i=0;i<=stackIndex;i++)
    		{ FanoronaCell c = pickedSource[i]; 
    			if(c!=null) 
    			{ h.put(c,""+(i+1)); 
    			}
    		  FanoronaCell d = droppedDest[i];
    		  	if(d!=null)
    		  	{ h.put(d,""+(i+2));
    		  	}
    		}
    	return(h);
    }

    // temporary list of destination cells allocate as a resource for speed
    private FanoronaCell[][]tempDestResource = new FanoronaCell[6][];
    private int tempDestIndex=-1;
    private synchronized FanoronaCell []getTempDest() 
    	{ if(tempDestIndex>=0) { return(tempDestResource[tempDestIndex--]); }
    	  return(new FanoronaCell[DEFAULT_ROWS*DEFAULT_COLUMNS]);
    	}
    private synchronized void returnTempDest(FanoronaCell[]d) { tempDestResource[++tempDestIndex]=d; }

	// factory method
	public FanoronaCell newcell(char c,int r)
	{	return(new FanoronaCell(c,r));
	}
    public FanoronaBoard(String init,int[]map) // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = FANORONAGRIDSTYLE; //coordinates left and bottom
    	Random r = new Random(8434563);
        initBoard(DEFAULT_COLUMNS,DEFAULT_ROWS); //this sets up the board and cross links
        allCells.setDigestChain(r);
    
        // count the number of exits for each cell
        for(FanoronaCell c = allCells;
        	c!=null;
        	c=c.next)
        {	c.setPointType();
        }
    	for(int i=0,pl=FIRST_PLAYER_INDEX;i<=SECOND_PLAYER_INDEX; i++,pl=nextPlayer[pl])
	    	{
	    	FanoronaChip chip =FanoronaChip.getChip(i);
	    	FanoronaCell cell = new FanoronaCell(r,chipPoolIndex[i],'@',i);
	    	addChip(cell,chip);
	    	fChips[i]=cell;
	    	}          
    	gametype = init;
        setColorMap(map, 2);
        autoReverseY();		// reverse_y based on the color map
     }
    public void setColorMap(int[]map, int players)
    {
    	super.setColorMap(map, players);
    	doInit(gametype);
    }
    public FanoronaBoard cloneBoard() 
	{ FanoronaBoard dup = new FanoronaBoard(gametype,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}

    public void copyFrom(BoardProtocol b) { copyFrom((FanoronaBoard)b); }
  
    // this is used only during initialization, to remove links that
    // can't be traversed by moves.  Initially, the board is fully
    // connected, we add the "cantMove" properties to mark the links
    // that can't be crossed.  Use moveTo to instead of exitToward
    // to respect these forbidden links.
    private void disconnectRow(FanoronaCell c,int dir)
    {
    	while(c!=null) 
		{FanoronaCell cn =c.exitTo(dir);
		 if(cn!=null)
		 { if(!c.cantMove[dir])
			 {c.cantMove[dir] = true; 
			  c.point_strength--;
			 }
	       if(!cn.cantMove[(dir+4)%8])
	       	{  cn.cantMove[(dir+4)%8]=true;
	       	   cn.point_strength--;
	       	}
	       if((c.point_strength<3)||(cn.point_strength<3)) 
	       {throw G.Error("low power");	// too many links removed, must be a mistake
	       }
		 }
         c = cn;
	    }
    }

    int playerIndex(FanoronaChip ch)  { return(getColorMap()[ch.colorIndex]); }
    // add a chip to the board and do associated bookkeeping
    private FanoronaChip addChip(FanoronaCell c,FanoronaChip chip)
    {	c.addChip(chip);	
    	int who = playerIndex(chip);
       	chips_on_board[who]++;
       	point_strength[who]+=c.point_strength;
       	return(chip);
    }
    // add a chip to the board, perform bookkeeping
    private FanoronaChip removeChip(FanoronaCell c)
    {	FanoronaChip chip = c.removeTop();
    	int who = playerIndex(chip);
       	chips_on_board[who]--;
      	point_strength[who]-=c.point_strength;
      	return(chip);
    }  
    // add a chip to the board, perform bookkeeping
    //private FanoronaChip removeChip(FanoronaCell c,FanoronaChip ch)
    //{	c.removeChip(ch);
    //   	chips_on_board[ch.playerIndex]--;
    //   	return(ch);
    //}   
    // standared init for Fanorona.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard(String game)
    { 	if(Fanorona_INIT.equalsIgnoreCase(game)) 
    		{ 
    		}
    	else { throw G.Error(WrongInitError,game); }
        gametype = game;
        setState(FanoronaState.PUZZLE_STATE);

        // remove some links so the board connectivity reflects the standard diagram.
        for(int col=1;col<boardColumns;col+=2)
        {   char firstcol=(char)('A'+col);
        	char lastcol=(char)('A'+boardColumns-col-1);
        	FanoronaCell c = getCell(firstcol,1);
        	disconnectRow(c,CELL_UP_RIGHT);
        	FanoronaCell d = getCell(firstcol,boardRows);
        	disconnectRow(d,CELL_DOWN_RIGHT);
        	FanoronaCell e = getCell(lastcol,1);
        	disconnectRow(e,CELL_UP_LEFT);
        	FanoronaCell f = getCell(lastcol,boardRows);
        	disconnectRow(f,CELL_DOWN_LEFT);
        }
        AR.setValue(point_strength,0);
        AR.setValue(chips_on_board,0);

        for(FanoronaCell c = allCells; c!=null; c=c.next) { c.reInit(); }
        
        // fill the board with the starting position
        for(int col=0,midrow=boardRows/2,midcol=boardColumns/2;
        	col<boardColumns;
        	col++)
        {  	for(int row=0;row<boardRows;row++)
        	{	FanoronaCell ch = row<midrow 
        							? fChips[0]
        							: (row>midrow) 
        								? fChips[1]
        								: (col==midcol)
        									? null
        									: ((col&1)==0)^(col<midcol)
        										? fChips[0]
        										: fChips[1];
        		FanoronaCell cell = getCell((char)('A'+col),row+1);
        	    if(ch!=null) { addChip(cell,ch.topChip()); }
        	}
        }
        whoseTurn = FIRST_PLAYER_INDEX;
        int map[] = getColorMap();
        playerChip[map[FIRST_PLAYER_INDEX]] = FanoronaChip.getChip(0);
        playerChip[map[SECOND_PLAYER_INDEX]] = FanoronaChip.getChip(1);
    }
    public void sameboard(BoardProtocol f) { sameboard((FanoronaBoard)f); }


    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(FanoronaBoard from_b)
    {
    	super.sameboard(from_b);
    	
    	G.Assert(AR.sameArrayContents(chips_on_board,from_b.chips_on_board),"chips_on_board mismatch");
    	G.Assert(AR.sameArrayContents(point_strength,from_b.point_strength),"point_strength mismatch");

        G.Assert(stackIndex == from_b.stackIndex,"stack Index Matches");
        G.Assert(pickedObject == from_b.pickedObject,"picked matches");
        G.Assert(unDropped == from_b.unDropped,"undropped matches");
        
        for(int i=0;i<=stackIndex;i++)
        {	G.Assert(sameCells(pickedSource[i],from_b.pickedSource[i]),"pickedSource[%d] mismatch",i);
        	G.Assert(sameCells(droppedDest[i],from_b.droppedDest[i]),"droppedDest[%d] mismatch",i);
        	G.Assert(sameCells(lastCaptured[i],from_b.lastCaptured[i]),"lastCaptured[%d] mismatch",i);
        }
  

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
        Random r = new Random(64 * 1000); // init the random number generator
        long v = super.Digest(r);

		v ^= chip.Digest(r,pickedObject);
		v ^= cell.Digest(r,getSource());
		v ^= Digest(r,board_state);
		v ^= Digest(r,whoseTurn);
        return (v);
    }
    
    public FanoronaCell getCell(FanoronaCell c)
    {	if(c==null) { return(null); }
    	if(c.onBoard) { return(getCell(c.col,c.row)); }
    	if(c.col=='@') { return(fChips[c.row]); }
    	throw G.Error("Can't rerecognize local copy of %s",c); 
     }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(FanoronaBoard from_b)
    {	super.copyFrom(from_b);
    	stackIndex = from_b.stackIndex;
        pickedObject = from_b.pickedObject;
        unDropped = from_b.unDropped; 
        robotDepth = from_b.robotDepth;
        for(int i=0;i<=stackIndex;i++)
        {	pickedSource[i] = getCell(from_b.pickedSource[i]);
        	droppedDest[i] =  getCell(from_b.droppedDest[i]);
        	lastCaptured[i] = getCell(from_b.lastCaptured[i]);
        }
        AR.copy(chips_on_board,from_b.chips_on_board);
        AR.copy(point_strength,from_b.point_strength);
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        lastProgressMove = from_b.lastProgressMove;		// last move where a pawn was advanced
        lastDrawMove = from_b.lastDrawMove;			// last move where a draw was offered

        sameboard(from_b); 
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long random)
    {	randomKey = random;
	   Init_Standard(gtype);
	   robotState.clear();
	   robotIndex.clear();
	   robotCapture.clear();
       moveNumber = 1;
       stackIndex = 0;
       robotDepth = 0;
       lastProgressMove = 0;		// last move where a pawn was advanced
       lastDrawMove = 0;			// last move where a draw was offered

       unDropped = null;
       pickedObject = null;
       for(int i=0;i<pickedSource.length;i++) { pickedSource[i]=droppedDest[i]=lastCaptured[i]=null; }
        // note that firstPlayer is NOT initialized here
    }



    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer(replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Move not complete, can't change the current player, state %s",board_state);
        case PUZZLE_STATE:
            break;
        case GAMEOVER_STATE:
        case DESIGNATE_STATE:
        	if(replay==replayMode.Live)
        	{
        	  	throw G.Error("Move not complete, can't change the current player, state %s",board_state);  		
        	}
			//$FALL-THROUGH$
        case CONFIRM_STATE:
        case CONFIRM_PLAY_STATE:
		case CONFIRM_REMOVE_STATE:
        case PLAY2_STATE:
        case DRAW_STATE:
        case RESIGN_STATE:
        case AcceptPending:
        case DeclinePending:
        case DrawPending:
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
         case CONFIRM_PLAY_STATE:
         case CONFIRM_REMOVE_STATE:
         case PLAY2_STATE:
         case DRAW_STATE:
         case DrawPending:
         case AcceptPending:
         case DeclinePending:
            return (true);

        default:
            return (false);
        }
    }


    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(FanoronaState.GAMEOVER_STATE);
    }
    public boolean WinForPlayerNow(int player)
    {	if(board_state==FanoronaState.GAMEOVER_STATE) { return(win[player]); }
    	return((chips_on_board[player]>0) 
    			&& (chips_on_board[nextPlayer[player]]==0));
     }
    // true if some adjacent cell is occupied by an enemy
    private boolean enemyAdj(FanoronaCell point,int forPlayer)
    {	for(int dir=0;dir<8;dir++) 
    	{ FanoronaCell adj = point.moveTo(dir); 
    		if(adj!=null && (adj.topChip()==playerChip[forPlayer])) { return(true); }
    	}
    	return(false);
    }
    // count the number of spaces we could capture from here and haven't seen before
    private int sweepForCoverageFrom(FanoronaCell c)
    {	int n=0;
    	int pl = nextPlayer[playerIndex(c.topChip())];
    	for(int direction=0;direction<8;direction++)
    	{	FanoronaCell nxt = c.moveTo(direction);
    		if((nxt!=null) && (nxt.chip==null))
    		{
    			FanoronaCell inv = c.moveTo(direction+4);
    			if((inv!=null)&& (inv.chip==null) && (nxt.sweep_counter!=sweep_counter))
    			{	nxt.sweep_counter=sweep_counter;
    				if( enemyAdj(inv,pl))
    					{ n++; }
    			}
    			FanoronaCell nxt2 = nxt.moveTo(direction);
    			if((nxt2!=null)
    				&& (nxt2.sweep_counter!=sweep_counter) 
    				&& (nxt2.chip==null)   				
    				)
    				{	nxt2.sweep_counter=sweep_counter;
    					if(enemyAdj(nxt2,pl)) 
    						{ n++;			// we've seen this spot
    						}
    				}
    		}
    	}
    return(n);
    }
    // count the sweep coverage of all the local players
    private int sweep_for_coverage(int forPlayer,int threshold)
    {	
    	int cover=0;
    	FanoronaChip chip = playerChip[forPlayer];
    	for(FanoronaCell c = allCells;
    		c!=null;
    		c=c.next)
    	{	FanoronaChip top = c.topChip();
    		if((top==chip) && (c.sweep_distance<=threshold))
    		{	sweep_counter++;
    			int local_cover = c.sweep_coverage = sweepForCoverageFrom(c);
    			cover += local_cover;
    		}
    	}
    	return(cover);
    }
    // this is used to encourage retrograde pieces to get into the fight
    private int minimumClosingDistance(FanoronaChip myPiece,FanoronaCell from,int distance,int minsofar)
    {	int rval = minsofar;
    	for(int direction = 0; direction<8;direction++)
    	{	FanoronaCell next = from.moveTo(direction);
    		if((next!=null) 
    			&& ((next.sweep_counter!=sweep_counter) || (next.sweep_distance>distance)))
    		{
    		FanoronaChip top = next.topChip();
    		if(top == myPiece) {}	// our own piece
    		else if(top==null) 			// more empty space
    			{	next.sweep_counter = sweep_counter;
    				next.sweep_distance = distance;
    				if(distance<minsofar)
    					{ rval = minimumClosingDistance(myPiece,next,distance+1,rval);
    					}
    			}
    		else 
    			{	// opposing piece
    				rval = distance;
    				break;			// no need to do the rest
    			}
    		}
     	}
    	return(rval);
    }
    private int minimumClosingDistance(FanoronaCell from)
    {	FanoronaChip top = from.topChip();
    	if(top!=null)
    		{	sweep_counter++;
    			from.sweep_counter=sweep_counter;
    			from.sweep_distance=1;
    			from.sweep_coverage = -1;
    			int dis = minimumClosingDistance(top,from,1,9);
    			from.sweep_distance=dis;
    			return(dis);
    		}
    	return(0);
    }
    private void minimumClosingDistance(int player)
    {	FanoronaChip chip = playerChip[player];
       	for(FanoronaCell c=allCells;
		c!=null;
		c=c.next)
		{	FanoronaChip top = c.topChip();
			if(top==chip) 
			{	minimumClosingDistance(c);
			}
		}
    }
    
    // look for a win for player.  
    public synchronized double ScoreForPlayer(int player,boolean print,boolean dumbot)
    {  	// dumbest possible evaluator, probably a good start for this game
    	// point_strength measures the number of choices available
    	// separation measures distance between our and opponents pieces, we want to close..
    	if(board_state==FanoronaState.GAMEOVER_STATE) 
    		{ return(0); 
    		}
    	double nchips = chips_on_board[player];
     	double chips = nchips*100;
    	double points = point_strength[player]/5.0;
    	double mcd = 0.0;
    	
    	minimumClosingDistance(player);				// calculate distances and prepare for sweep coverage
    	double cover = 1*sweep_for_coverage(player,3);	//calculate coverage for pieces in the fight
    	FanoronaChip chip = playerChip[player];
    	for(FanoronaCell c=allCells;
    		c!=null;
    		c=c.next)
    		{	FanoronaChip top = c.topChip();
    			if(top==chip)
    			{	double local = c.sweep_distance;

    				if(local>0)
    				{	switch(c.point_type)	// compensate for weak points poor connectivity
    					{
    					case FanoronaCell.CORNER:
    						break;
    					case FanoronaCell.SIDE:
    						if(c.point_strength==3) { local -= 0.5; }
    						break;
    					case FanoronaCell.BODY:
    						if(c.point_strength==4) { local -= 0.5; }
    						break;
					default:
						break;
    					}
      					mcd -= local*nchips*0.4;	/* penalty */ 
    				}
    			}
    		}
    	double sum = chips+points+mcd+cover;
    	if(print)
    	{	System.out.println("Ev"+player+" "+sum+" = wood="+chips+" loc="+points+" dis="+mcd+" cover="+cover);
    	}
     	return(sum);
     }
    


    public void finalizePlacement()	// finalize placement, no more undo
    {	while(stackIndex>=0)
    	{   pickedObject = null;
        	droppedDest[stackIndex]  = null;
        	pickedSource[stackIndex]  = null;
        	lastCaptured[stackIndex]  = null;
        	stackIndex--;
    	}
    	unDropped = null;
    	stackIndex = 0;
    }
    
    // lowest level of uncapture.  "removed" is the most distant cell removed by the capture
    // picked and dropped are the source and destination respectively.  This handles both
    // capture by approach and by withdrawal.
    // call this after the pick-drop has been undone, so "picked" contains the color moving
    private void unCaptureObject(FanoronaCell picked,FanoronaCell dropped,FanoronaCell removed)
    {
    	int toindex = playerIndex(picked.topChip());
    	FanoronaChip newchip = playerChip[nextPlayer[toindex]];
    	int dir = findDirection(removed.col,removed.row,dropped.col,dropped.row);
		while((removed!=null) && (removed!=dropped) && (removed!=picked))
		{	addChip(removed,newchip);
			removed = removed.exitTo(dir);
		}
    }
    public Hashtable<FanoronaCell,FanoronaCell> lastCapturedCells()
    {	Hashtable<FanoronaCell,FanoronaCell> h = new Hashtable<FanoronaCell,FanoronaCell>();
    	if(stackIndex>0)
    	{FanoronaCell removed = lastCaptured[stackIndex-1];
		if(removed!=null)
		{	FanoronaCell picked = pickedSource[stackIndex-1];
			FanoronaCell dropped = droppedDest[stackIndex-1];
		   	int dir = findDirection(removed.col,removed.row,dropped.col,dropped.row);
			while((removed!=null) && (removed!=dropped) && (removed!=picked))
			{	h.put(removed,removed);
				removed = removed.exitTo(dir);
			}
		}}
		return(h);
}
   
    
    private void unCaptureObject()
    {	FanoronaCell removed = lastCaptured[stackIndex];
    	if(removed!=null)
    	{	
    		FanoronaCell dropped = unDropped;
    		FanoronaCell picked = pickedSource[stackIndex];
    		lastCaptured[stackIndex]=null;
    		unDropped = null;
    		unCaptureObject(picked,dropped,removed);
    	}
    }
    private FanoronaCell deleteFrom(FanoronaCell first,int dir,FanoronaCell to,replayMode replay)
    {  
    	FanoronaChip toChip = to.topChip();
    	while(first!=null)
    	{	FanoronaChip c = first.topChip();
    		if(c!=null)
    		{ if(c==toChip)
    			{ first = null; }	// terminate, hit our own guy
    		  else 
    		  	{ lastCaptured[stackIndex]=first;
    		  	  removeChip(first);
    		  	  if(replay!=replayMode.Replay)
    		  	  {	animationStack.push(first);
    		  	    animationStack.push(fChips[toChip.colorIndex^1]);
    		  	  }
    		  	  first = first.exitTo(dir);
    		  	}
    		
    		}
    		else { first=null; }	// terminate, empty guy
    	}
    	unDropped=null;
    	return(lastCaptured[stackIndex]);
    }
    private FanoronaCell doCaptureByApproach(FanoronaCell from,FanoronaCell to,replayMode replay)
    {	int dir = findDirection(from.col,from.row,to.col,to.row);
    	return(deleteFrom(to.exitTo(dir),dir,to,replay));
    }
    private FanoronaCell doCaptureByWithdrawal(FanoronaCell from,FanoronaCell to,replayMode replay)
    {	int dir = 4+findDirection(from.col,from.row,to.col,to.row);
    	return(deleteFrom(from.exitTo(dir),dir,to,replay));
    }

    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    FanoronaCell dr = droppedDest[stackIndex];
    if(dr!=null)
    	{
    	unDropped = dr;
    	droppedDest[stackIndex] =  null;
    	pickedObject = removeChip(dr);
    	}
    }
    private FanoronaCell getSource() { return(pickedSource[stackIndex]); }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	FanoronaChip po = pickedObject;
    	if(po!=null)
    	{
        FanoronaCell ps = pickedSource[stackIndex];
       	addChip(ps,po);
        unCaptureObject();
        pickedSource[stackIndex]=null;
    	pickedObject = null;
    	unDropped = null;
  	}
     }
    
    private void dropOnBoard(FanoronaCell dcell)
    {
    	droppedDest[stackIndex] = dcell;
    	unDropped = null;
    	addChip(dcell,pickedObject);
    	pickedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(FanId dest, char col, int row)
    {
       G.Assert((pickedObject!=null)&&(droppedDest[stackIndex]==null),"ready to drop");
       switch (dest)
        {
        default: throw G.Error("Not expecting dest %s",dest);
        case BoardLocation: // an already filled board slot.
        	dropOnBoard(getCell(col,row));
         	break;
        case Black_Chip:		// back in the pool
        case White_Chip:		// back in the pool
        	finalizePlacement();	// only in puzzle state
            break;
        }
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(FanoronaCell cell)
    {	return((stackIndex>0)&&(droppedDest[stackIndex-1]==cell));
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    {	FanoronaChip ch = pickedObject;
    	if((ch!=null)&&(droppedDest[stackIndex]==null))
    		{ return(ch.colorIndex);
    		}
        return (NothingMoving);
    }
    
    private void pickFromBoard(FanoronaCell src)
    {  	pickedSource[stackIndex] = src;
    	pickedObject = removeChip(src);
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(FanId source, char col, int row)
    {	G.Assert((pickedObject==null)&&(pickedSource[stackIndex]==null),"ready to pick");
    
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);

        case BoardLocation:
         	{
         	pickFromBoard(getCell(col,row));
         	break;
         	}
        case White_Chip:
        	{
        	FanoronaCell c = pickedSource[stackIndex] = fChips[FIRST_PLAYER_INDEX];
        	pickedObject = c.topChip();
        	}
        	break;
        case Black_Chip:
        	{
        	FanoronaCell c = pickedSource[stackIndex] = fChips[SECOND_PLAYER_INDEX];
        	pickedObject = c.topChip();
        	}
                	
       }
   }
    // true if from-to can be a capture by wirhdrawal.  If Andhit is not null, also
    // check that andhit is on the line that gets capured.
    boolean captureByWithdrawal(FanoronaCell from,FanoronaCell to,FanoronaCell andHit)
    {	if(from==to) { return(false); }
    	int dir = 4+findDirection(from.col,from.row,to.col,to.row);
    	FanoronaChip toChip = to.topChip();
    	FanoronaCell prev = from.exitTo(dir);
    	if(prev!=null)
    	{	FanoronaChip top = prev.topChip();
    		if(top!=null)
    		{	if(top!=toChip) 
    				{ if(andHit==null) {return(true); }
    				  while((prev!=null) && (top!=null))
    				  {	if(top==toChip) { return(false); }
    				  	if(prev==andHit) { return(true); }
    				  	prev = prev.exitTo(dir);
    				  	top = (prev==null) ? null : prev.topChip();
    				  }
    				  return(false);
    				}
    		}
    	}
    	return(false);
    }
    // true if from-to can be a capture by approach.  If andhit is not null also
    // check that andHit is on the line of captures
    boolean captureByApproach(FanoronaCell from,FanoronaCell to,FanoronaCell andHit)
    {	if(from==to) { return(false); }
    	int dir = findDirection(from.col,from.row,to.col,to.row);
    	FanoronaChip toChip = to.topChip();
    	FanoronaCell next = to.exitTo(dir);
    	if(next!=null)
    	{	FanoronaChip top = next.topChip();
    		if(top!=null)
    		{	if(top!=toChip) 
    			{ if(andHit==null) { return(true); }
    			  while((next!=null) && (top!=null)) 
    			  { 
    				if(top==toChip) { return(false); }
    				if(next==andHit) { return(true); }
    				next = next.exitTo(dir);
    				if(next!=null) { top = next.topChip(); }
    			  }
    			  return(false);
    			}
    		}
    	}
    	return(false);
    }
    
    // choose the next state, which will be "confirm" if no further captures
    // are possible, and "play2" if additional captures are possible.
    private void setNextStateAfterCapture(FanoronaCell dst)
    {
       	setState((captureDestsFrom(dst,true,true,null,0)>0)
       				?FanoronaState.PLAY2_STATE
       				:((board_state==FanoronaState.DESIGNATE_STATE) ? FanoronaState.CONFIRM_REMOVE_STATE:FanoronaState.CONFIRM_STATE));	
    }
    
    // finish a capturing move and set the next state.  If the victim group is known
    // the capture is complete and the next state will be chosen.  If the victim is
    // not known, enter "designate" state to choose it.
    private void finishCapture(FanoronaCell src,FanoronaCell dst,FanoronaCell andhit,replayMode replay)
    {
        boolean capw = captureByWithdrawal(src,dst,andhit);
    	boolean capa = captureByApproach(src,dst,andhit);
    	if(capa && capw) 
    		{ G.Assert(andhit==null,"should be ambiguous");
    		  setState(FanoronaState.DESIGNATE_STATE);  
    		  return; 
    		 }
    	else if(capa) 
    		{ doCaptureByApproach(src,dst,replay);
    		  stackIndex++;
       		}
    	else if(capw) 
    		{ doCaptureByWithdrawal(src,dst,replay);
    		  stackIndex++;
    		}
    	else {	throw G.Error("should have been a capture"); }
    	
    	setNextStateAfterCapture(dst);
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(replayMode replay)
    {
        FanoronaCell src = pickedSource[stackIndex];
        FanoronaCell dst = droppedDest[stackIndex];
        if(src==dst) 
        { unDropObject();
          unPickObject(); 
        }
        else
        if((stackIndex>0) && (dst==pickedSource[stackIndex-1]))
        	{	// single step undo
        		unDropObject();
        		unPickObject();
        		stackIndex--;
        		unDropObject();
        		unPickObject();
        		if(stackIndex>0) 
        			{ setNextStateAfterCapture(dst); 
        			} else 
        			{ setNextStateAfterDone(replay); 
        			}
        	}
        else
        {
        switch (board_state)
        {
        case GAMEOVER_STATE:
        	// allow some old damaged games to pass
        	if(replay!=replayMode.Live) { break; }
			//$FALL-THROUGH$
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case DRAW_STATE:
        	if(dst!=null) { setNextStateAfterDone(replay); }
        	break;
        case PLAY_STATE:
        	stackIndex++;
        	setState(FanoronaState.CONFIRM_PLAY_STATE);	// no captures possible
        	break;
        case PLAY1_STATE:
        case PLAY2_STATE:
            finishCapture(src,dst,null,replay);
 			break;

        case PUZZLE_STATE:
			finalizePlacement();
            break;
        }
        }
    }
    //	
    //true if col,row is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(char col, int row)
    {
        return ((pickedSource!=null) 
        		&& (pickedSource[stackIndex].col == col) 
        		&& (pickedSource[stackIndex].row == row));
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
        case DRAW_STATE:
        case CONFIRM_PLAY_STATE:
           	setState(FanoronaState.PLAY_STATE);
           	break;
        case CONFIRM_REMOVE_STATE:
        	// stack already incremented
        	setState((stackIndex>1) ? FanoronaState.PLAY2_STATE:FanoronaState.PLAY1_STATE);
        	break;
        case DESIGNATE_STATE:
        case CONFIRM_STATE:
   			setState((stackIndex>0) ? FanoronaState.PLAY2_STATE : FanoronaState.PLAY1_STATE); 
        	break;
        case PLAY_STATE:
        case PLAY1_STATE:
        case PLAY2_STATE:
        case PUZZLE_STATE:
   		 
            break;
        }
    }
    
    // current player is the new player
    private void setNextStateAfterDone(replayMode replay)
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	
        case DrawPending:
        	lastDrawMove = moveNumber;
        	setState(FanoronaState.AcceptOrDecline);
        	break;
    	case AcceptPending:
        	setGameOver(false,false);
        	break;
   	
    	case GAMEOVER_STATE: 
    		break;

        case DRAW_STATE:
        	setGameOver(false,false);
        	break;
        	
        case DESIGNATE_STATE:
        	if(replay==replayMode.Live)
        	{	// allow some damaged games to proceed
        	  	throw G.Error("Not expecting state %s",board_state);  		
        	}

			//$FALL-THROUGH$
    	case CONFIRM_STATE:
    	case CONFIRM_PLAY_STATE:
    	case PUZZLE_STATE:
		case CONFIRM_REMOVE_STATE:
		case PLAY2_STATE:
    	case PLAY1_STATE:
     	case DeclinePending:
    	case PLAY_STATE:
    		// if captures are possible, enter play1 state
    		if(captureDestsFor(whoseTurn,null,0)==0) 
    			{	if(hasFreeMoves(whoseTurn)) { setState(FanoronaState.PLAY_STATE); }
    			else { setGameOver(false,true); }
    			}
    		else {setState(FanoronaState.PLAY1_STATE); }
    	}

    }
   
    private void doDone(replayMode replay)
    {	
        finalizePlacement();

        if (board_state==FanoronaState.RESIGN_STATE)
        {	setGameOver(false,true);
        }
        else
        {	boolean win1 = WinForPlayerNow(whoseTurn);
        	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
        	if(win1 || win2)  { setGameOver(win1,win2); }
        	else {setNextPlayer(replay); setNextStateAfterDone(replay); }
        }
    }
    public boolean drawIsLikely()
    {
    	return((board_state==FanoronaState.DrawPending)
    			|| ((moveNumber-lastDrawMove)>4)
    				&& ((moveNumber-lastProgressMove)>10));
    }
    public boolean canOfferDraw()
    {
    	return (moveNumber-lastDrawMove>4);
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	FanoronaMovespec m = (FanoronaMovespec)mm;

        //G.print("E "+m+" for "+whoseTurn+" : "+stackIndex);
        switch (m.op)
        {
        
        case MOVE_OFFER_DRAW:
        	if(canOfferDraw())
        	{
        	// ignore repeated draw offers
        	if(board_state==FanoronaState.DrawPending) { setState(FanoronaState.PLAY_STATE); }
        	else { 
        			setState(FanoronaState.DrawPending);
        		}}
        	break;
        case MOVE_ACCEPT_DRAW:
           	switch(board_state)
        	{	
        	case AcceptPending: 	// cancel accept and revert to neutral
        		setState(FanoronaState.AcceptOrDecline); 
        		break;
           	case AcceptOrDecline:
           	case DeclinePending:	// accept pending
           		setState(FanoronaState.AcceptPending); 
           		break;
        	default: throw G.Error("Not expecting %s",board_state);
        	}
           	break;
        case MOVE_DECLINE_DRAW:
        	switch(board_state)
        	{	
        	case DeclinePending:	// cancel decline and revert to neutral
        		setState(FanoronaState.AcceptOrDecline); 
        		break;
        	case AcceptOrDecline:
        	case AcceptPending: setState(FanoronaState.DeclinePending); break;
        	default: throw G.Error("Not expecting %s",board_state);
        	}
        	break;

        	
        case MOVE_DONE:
         	doDone(replay);
            break;
        case MOVE_CAPTUREA:
	        { 	
	        	FanoronaCell src = getCell(m.from_col, m.from_row);
	        	FanoronaCell dst = getCell(m.to_col, m.to_row);
	        	if(replay!=replayMode.Replay)
	        	{
	        		animationStack.push(src);
	        		animationStack.push(dst);
	        	}
	        	pickFromBoard(src);
	        	dropOnBoard(dst);
	        	robotCapture.push(doCaptureByApproach(src,dst,replay));
	        	stackIndex++;
	        	setNextStateAfterCapture(dst);
	        }
        	break;
        case MOVE_CAPTUREW:
	        {
	        FanoronaCell src = getCell(m.from_col, m.from_row);
	    	FanoronaCell dst = getCell(m.to_col, m.to_row);
	    	if(replay!=replayMode.Replay)
        	{
        		animationStack.push(src);
        		animationStack.push(dst);
        	}
	    	pickFromBoard(src);
	    	dropOnBoard(dst);
	    	robotCapture.push(doCaptureByWithdrawal(src,dst,replay));
	    	stackIndex++;
	    	setNextStateAfterCapture(dst);
	        }
      	
        	break;
        case MOVE_BOARD_BOARD:
        {	FanoronaCell src = getCell(m.from_col, m.from_row);
        	FanoronaCell dest = getCell(m.to_col, m.to_row);
        	pickFromBoard(src);
        	dropOnBoard(dest);
        	setNextStateAfterDrop(replay);
        	if(replay!=replayMode.Replay)
        	{
        		animationStack.push(src);
        		animationStack.push(dest);
        	}
        }
        	break;
        case MOVE_DROPB:
        	G.Assert(pickedObject!=null,"something is moving");
        	FanoronaCell dest = getCell(m.to_col, m.to_row);
            if(replay==replayMode.Single)
            {	animationStack.push(getSource());
            	animationStack.push(dest);
            }
            dropOnBoard(dest);
 			setNextStateAfterDrop(replay);
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary pick
        	switch(board_state)
        	{	
        	case DESIGNATE_STATE:
    			{ unDropObject();
    			}
    			break;
        	case CONFIRM_PLAY_STATE:
        		stackIndex--;
        		unDropObject();
        		break;
           	case CONFIRM_REMOVE_STATE:
           	case CONFIRM_STATE:
           	default:
        		pickObject(FanId.BoardLocation, m.from_col, m.from_row);
        	}
        	setNextStateAfterPick();
        	 
            break;
        case MOVE_REMOVE:
        	switch(board_state)
        	{
        	default: throw G.Error("Not expecting remove in state %s",board_state);
        	case DESIGNATE_STATE:
        		{
        		FanoronaCell dst = droppedDest[stackIndex];			// dest of the capturing move
        		FanoronaCell src = pickedSource[stackIndex];		// source of the capturing move
        		FanoronaCell cell = getCell(m.from_col,m.from_row);	// designated to be removed
        		finishCapture(src,dst,cell,replay);
         		}
        	}
        	break;
        case MOVE_DROP: // drop on chip pool;
            dropObject(m.source, m.to_col, m.to_row);
            setNextStateAfterDrop(replay);

            break;

        case MOVE_PICK:
            unDropObject();
            unPickObject();
            pickObject(m.source, m.from_col, m.from_row);
            setNextStateAfterPick();
            break;

 
        case MOVE_START:
            setWhoseTurn(m.player);
            finalizePlacement();
            setState(FanoronaState.PUZZLE_STATE);
            {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(replay); 
            	}
            }
            break;

        case MOVE_RESIGN:
            setState(unresign==null?FanoronaState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
        	finalizePlacement();
            setState(FanoronaState.PUZZLE_STATE);
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
		case AcceptOrDecline:
		case DeclinePending:
		case AcceptPending:
		case DrawPending:
        default:
 			return(false);
        case PUZZLE_STATE:
        	return((pickedObject==null)?true:(playerChip[player]==pickedObject));
        }
    }
  
    // true if it's legal to drop chip  originating from fromCell on toCell
    public boolean LegalToPickOrDrop(FanoronaCell fromCell,FanoronaChip gobblet,FanoronaCell toCell,boolean forFree)
    {	boolean val = false;
    	if(toCell==null)
    	{throw G.Error("not expected");
    	}
    	else if(fromCell!=null)
    	{	FanoronaCell temp[] = getTempDest();
    		int n= forFree ? freeDestsFrom(fromCell,temp,0) : captureDestsFrom(fromCell,true,true,temp,0);
    		for(int i=0;i<n;i++) { if (temp[i]==toCell) { val = true;  break; }}
    		returnTempDest(temp);
    	}
    	else
    	{	FanoronaChip top = toCell.topChip();
    		if(top==playerChip[whoseTurn])
    		{
    		int n = forFree ? freeDestsFrom(toCell,null,0) : captureDestsFrom(toCell,true,true,null,0);
    		val = (n>0);
    		}
    	}
		return(val);
    }
    private boolean reconsidering(FanoronaCell cell)
    {	return((cell==droppedDest[stackIndex]) 
				|| (cell==unDropped)
				|| ((stackIndex>0) && (droppedDest[stackIndex-1]==cell))
				|| ((pickedObject!=null) && (stackIndex>0) && (pickedSource[stackIndex-1]==cell))
				|| (cell==pickedSource[stackIndex]));
    }
    public boolean LegalToHitBoard(FanoronaCell cell)
    {	
        switch (board_state)
        {
 		case PLAY_STATE:
 			if(reconsidering(cell))
 				{ return(true); 
 				}
			return(LegalToPickOrDrop(pickedSource[stackIndex],pickedObject,cell,true));
 		case PLAY2_STATE:
 			// in play2 state, we've made one capture and can make another, or can reconsider the previous move
 			if(pickedObject==null) 
 				{ if(reconsidering(cell))	
 					{ return(true); }
 				  return(false); 
 				}
 			// fall through
			//$FALL-THROUGH$
		case PLAY1_STATE:
 			if(reconsidering(cell))	
 				{ return(true); }
 			return(LegalToPickOrDrop(pickedSource[stackIndex],pickedObject,cell,false));
 		case DESIGNATE_STATE:
 			// need to designate a line to kill, determined by pickedSource and droppedDest
 			if(cell==droppedDest[stackIndex]) { return(true); }
 			if(captureByApproach(pickedSource[stackIndex],droppedDest[stackIndex],cell)) { return(true); }
 			if(captureByWithdrawal(pickedSource[stackIndex],droppedDest[stackIndex],cell)) { return(true); }
 			return(false);
		case GAMEOVER_STATE:
		case RESIGN_STATE:
		case AcceptOrDecline:
		case DeclinePending:
		case AcceptPending:
		case DrawPending:
			return(false);
		case CONFIRM_STATE:
		case CONFIRM_PLAY_STATE:
		case CONFIRM_REMOVE_STATE:
		case DRAW_STATE:
			return(reconsidering(cell) || isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);

        case PUZZLE_STATE:
        	return(pickedObject==null?(cell.chip!=null):cell.canDrop(pickedObject));
        }
    }
  public boolean canDropOn(FanoronaCell cell)
  {	
  	return((pickedObject!=null)				// something moving
  			&&(pickedSource[stackIndex].onBoard 
  					? (cell!=pickedSource[stackIndex])	// dropping on the board, must be to a different cell 
  					: (cell==pickedSource[stackIndex]))	// dropping in the rack, must be to the same cell
  				);
  }
 
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(FanoronaMovespec m)
    {	//G.print("R "+m+" for "+whoseTurn);
    	robotState.push(board_state);
    	robotIndex.push(stackIndex);
    	robotDepth++;
                // to undo state transitions is to simple put the original state back.
       G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
 
       {
    	   for(int i=0;i<=stackIndex;i++)
    	   {
    		   undoStack.push(pickedSource[i]);
    		   undoStack.push(droppedDest[i]);
    		   undoStack.push(lastCaptured[i]);
    	   }
       }
       
       if(Execute(m,replayMode.Replay))
       {  
    	  switch(board_state)
    	  {	
    	  case RESIGN_STATE:
    	  case DRAW_STATE:
    	  case CONFIRM_STATE:	
    	  case CONFIRM_PLAY_STATE:
    	  case DrawPending:
    	  case AcceptPending:
    	  case DeclinePending:
  		  case CONFIRM_REMOVE_STATE:
     		  doDone(replayMode.Replay);
     		  break;
    	  
    	  case AcceptOrDecline:
    	  case PLAY_STATE:
    	  case PLAY1_STATE: 
    	  case GAMEOVER_STATE:
    	  case PLAY2_STATE:	break;
    	  
    	  default:
    		  throw G.Error("Not expecting state %s",board_state);
    	  }
       }
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(FanoronaMovespec m)
    {
        //G.print("U "+m+" for "+whoseTurn);
    	robotDepth--;
        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;
   	    case MOVE_ACCEPT_DRAW:
   	    case MOVE_DECLINE_DRAW:
   	    case MOVE_OFFER_DRAW:
        case MOVE_DONE:
             break;
        case MOVE_CAPTUREA:
        case MOVE_CAPTUREW: 
        	{ FanoronaCell src = getCell(m.from_col,m.from_row);
        	  FanoronaCell dst = getCell(m.to_col,m.to_row);
        	  FanoronaChip target = removeChip(dst);
        	  addChip(src,target);
      	      unCaptureObject(src,dst,robotCapture.pop());
        	}
         	break;
        case MOVE_BOARD_BOARD:
       		{ FanoronaCell src = getCell(m.from_col,m.from_row);
       		  FanoronaCell dst = getCell(m.to_col,m.to_row);
       		  FanoronaChip target = removeChip(dst);
       		  addChip(src,target);
       		}
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(robotState.pop());
        stackIndex = robotIndex.pop();
        
        {
      	   for(int lim=stackIndex; lim>=0; lim--)
      	   {
      		  lastCaptured[lim] = undoStack.pop();
      		  droppedDest[lim] = undoStack.pop();
      		  pickedSource[lim] = undoStack.pop();
      	   }
         }

        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
 
  private int freeDestsFrom(FanoronaCell c,FanoronaCell dests[],int idx)
    {	
    	for(int direction=0;direction<8;direction++) 
    	{ FanoronaCell d = c.moveTo(direction);
    	  if(d!=null)
    	  {
    		FanoronaChip dtop = d.topChip();
    		if(dtop==null)	// adjacent cell is empty
    		{	if(dests==null) { return(idx+1); }
    			dests[idx] = d; 
    			idx++;
    		}
    	  }
    	}
    	return(idx);
    }
 private boolean isAPreviousSource(FanoronaCell d)
 {
	for(int i=0;i<stackIndex;i++)
	{	if(pickedSource[i]==d) { return(true); }
	}
	return(false);
 }
 private boolean isSameDirection(int direction)
 {	if(stackIndex>0)
 	{	FanoronaCell prevSource = pickedSource[stackIndex-1];
 		FanoronaCell prevDest = droppedDest[stackIndex-1];
 		int prevdir = findDirection(prevSource.col,prevSource.row,prevDest.col,prevDest.row);
 		return(prevdir==direction);
 	}
 	return(false);
 }

 private int captureDestsFrom(FanoronaCell c,boolean approach,boolean withdrawal,FanoronaCell dests[],int idx)
 {	FanoronaChip ctop = (c==pickedSource[stackIndex]) ? pickedObject : c.topChip();
 	if(ctop!=null)
    {for(int direction=0;direction<8;direction++) 
 	{ FanoronaCell d = c.moveTo(direction);
 	  if(d!=null)
 	  {
 		FanoronaChip dtop = d.topChip();
 		if((dtop==null)	// adjacent cell is empty
 			&& !isAPreviousSource(d)
 			&& !isSameDirection(direction)
 			)
 			{
 			if(approach)
 			{	// check for capture by approach
 			FanoronaCell e = d.moveTo(direction);
 			if(e!=null)
 			{
 			FanoronaChip etop = e.topChip();
 			if((etop!=null) && (etop!=ctop)) 
 				{ if(dests!=null) { dests[idx] = d; }
 				  idx++;
 				}
  			}}
 			if(withdrawal)
  			{
 			// check for capture by withdrawal
 			FanoronaCell f = c.exitTo(direction+4);	// exit because we don't need to move there
 			if(f!=null)
 			{	FanoronaChip ftop = f.topChip();
 				if((ftop!=null) && (playerIndex(ftop)!=playerIndex(ctop)))
 				{	if(dests!=null) { dests[idx] = d; }
 					idx++;
 				}
 			}}}
  	  }
 	}}
 	return(idx);
 }
 private int captureDestsFor(int player,FanoronaCell dest[],int idx)
 {	G.Assert(stackIndex==0,"start of sequence only");
	for(FanoronaCell c = allCells;
		c!=null;
		c = c.next)
	{
		FanoronaChip ctop = c.topChip();
		if(ctop==playerChip[player])
		{	idx = captureDestsFrom(c,true,true,dest,idx);
		}
	}
	return(idx);
 }
 
//private int freeDestsFor(int player,FanoronaCell dest[],int idx)
//{	G.Assert(stackIndex==0,"start of sequence only");
//	for(FanoronaCell c = (FanoronaCell)allCells;
//		c!=null;
//		c = (FanoronaCell)c.next)
//	{
//		FanoronaChip ctop = c.topChip();
//		if((ctop!=null) && (ctop.playerIndex==player))
//		{	idx = freeDestsFrom(c,dest,idx);
//		}
//	}
//	return(idx);
// }
 
 void freeMovesFor(int player,CommonMoveStack  result)
 {	G.Assert(stackIndex==0,"start of sequence only");
 	FanoronaCell temp[] = getTempDest();
 	FanoronaChip chip = playerChip[player];
	for(FanoronaCell c = allCells;
		c!=null;
		c =c.next)
	{
		FanoronaChip ctop = c.topChip();
		if(ctop==chip)
		{	int idx = freeDestsFrom(c,temp,0);
			for(int i=0;i<idx;i++) 
			{	FanoronaCell dst = temp[i];
				result.addElement(new FanoronaMovespec(MOVE_BOARD_BOARD,c.col,c.row,dst.col,dst.row,whoseTurn));
			}
		}
	}
	returnTempDest(temp);
}
 boolean hasFreeMoves(int player)
 {	G.Assert(stackIndex==0,"start of sequence only");
 	FanoronaChip chip = playerChip[player];
	for(FanoronaCell c = allCells;
		c!=null;
		c =c.next)
	{
		FanoronaChip ctop = c.topChip();
		if(ctop==chip)
		{	int idx = freeDestsFrom(c,null,0);
			if(idx>0) { return(true); }
		}
	}
	return(false);
}
 void captureMovesFrom(FanoronaCell from,CommonMoveStack  result)
 {	
	FanoronaCell temp[]=getTempDest();
	{
	int n = captureDestsFrom(from,true,false,temp,0);
	for(int i=0;i<n;i++) 
		{ //result.addElement(new FanoronaMovespec(MOVE_
		FanoronaCell dst = temp[i];
		result.addElement(new FanoronaMovespec(MOVE_CAPTUREA,from.col,from.row,dst.col,dst.row,whoseTurn));
		}
	}
	{
	int n = captureDestsFrom(from,false,true,temp,0);
	for(int i=0;i<n;i++) 
		{ //result.addElement(new FanoronaMovespec(MOVE_
		FanoronaCell dst = temp[i];
		result.addElement(new FanoronaMovespec(MOVE_CAPTUREW,from.col,from.row,dst.col,dst.row,whoseTurn));
		}
	}
	returnTempDest(temp);
 }
 
 // all capture moves for player.  Valid only at the start of a move sequence
 void captureMovesFor(int player,CommonMoveStack  result)
 {	G.Assert(stackIndex==0,"start of sequence only");
	for(FanoronaCell c = allCells;
		c!=null;
		c =c.next)
	{
		FanoronaChip ctop = c.topChip();
		if(ctop==playerChip[player])
		{	captureMovesFrom(c,result);
		}
	}
 }
 
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
  	switch(board_state)
 	{
  	case AcceptOrDecline:
  		if(drawIsLikely()) { all.addElement(new FanoronaMovespec(MOVE_ACCEPT_DRAW,whoseTurn)); }
  		all.addElement(new FanoronaMovespec(MOVE_DECLINE_DRAW,whoseTurn));
  		break;
 	case CONFIRM_STATE:
 	case AcceptPending:
 	case DeclinePending:
 	case DrawPending:
 	case CONFIRM_PLAY_STATE:
 	case CONFIRM_REMOVE_STATE:
 	case DRAW_STATE:
		all.addElement(new FanoronaMovespec(MOVE_DONE,whoseTurn));	// stopping now is an option
		break;
 	case PLAY_STATE:
		freeMovesFor(whoseTurn,all);
		// we should always find moves, otherwise should be gameover_state
		if(((moveNumber-lastProgressMove)>15)
				&& ((moveNumber-lastDrawMove)>8))
			{ 
			all.addElement(new FanoronaMovespec(MOVE_OFFER_DRAW,whoseTurn));
			}
		G.Assert(all.size()>0, "no free moves found");
		break;
 	case PLAY2_STATE:
 		{
 		G.Assert(stackIndex>0,"one capture done");
		FanoronaCell dst = droppedDest[stackIndex-1];
 		all.addElement(new FanoronaMovespec(MOVE_DONE,whoseTurn));	// stopping now is an option
  		captureMovesFrom(dst,all);
 		}
 		break;
  	case PLAY1_STATE:
 		G.Assert(stackIndex==0,"stack empty");
 		captureMovesFor(whoseTurn,all); 
 		break;
 	default: throw G.Error("not implemented");
 	}
 	return(all);
 }
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {  char ch = txt.charAt(0);
 	if(Character.isDigit(ch) )
	{ if((ch-'0')>boardRows) {return; }
	  xpos -= cellsize/3;
	}
 	else { ypos +=cellsize/2; }
 	GC.Text(gc, true, xpos, ypos, -1, 0,clt, null, txt);
 }
}
