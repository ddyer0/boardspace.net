package cannon;

import java.awt.Color;
import java.util.*;

import online.game.*;
import lib.*;
import lib.Random;



/**
 * CannonBoard knows all about the game of Cannon, which is played
 * on a 10x10 board. It gets a lot of logistic support from 
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

class CannonBoard extends rectBoard<CannonCell> implements BoardProtocol,CannonConstants
{	
    static final String[] CANNONGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
    static final int DEFAULT_COLUMNS = 10;	// 8x6 board
	static final int DEFAULT_ROWS = 10;
 	CannonState unresign;
 	CannonState board_state;
 	public CellStack animationStack = new CellStack();
	public CannonState getState() {return(board_state); }
	public void setState(CannonState st) 
	{ 	unresign = (st==CannonState.RESIGN_STATE)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
    public int boardColumns = DEFAULT_COLUMNS;	// size of the board
    public int boardRows = DEFAULT_ROWS;
    public int robotDepth = 0;
    public void SetDrawState() { setState(CannonState.DRAW_STATE); }
    
    public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
    {  char ch = txt.charAt(0);
    	if(Character.isDigit(ch)) { xpos -= cellsize/3; }
    	super.DrawGridCoord(gc,clt,xpos,ypos,cellsize,txt);
    }
    
    public CannonCell capturedChips[] = new CannonCell[2];
    public CannonCell whiteCaptured = new CannonCell(CannonId.White_Captured);
    public CannonCell blackCaptured = new CannonCell(CannonId.Black_Captured);
    
    public CannonCell whiteChips[] = new CannonCell[2];
    public CannonCell blackChips[] = new CannonCell[2];
    public CannonCell rack[][] = {whiteChips,blackChips};		// unplayed pieces
    public CannonCell townLocation[] = new CannonCell[2];	// placed town location
    public boolean townCaptured[]=new boolean[2];			// true when the town is captured
    public CannonId playerId[] = new CannonId[2];
  
    //
    // private variables
    //
    private int chips_on_board[] = new int[2];			// number of chips currently on the board
    
    public int chipsOnBoard(int forPlayer)
    {
    	int n = chips_on_board[forPlayer];
    	CannonChip po = pickedObject;
    	if((po!=null) && (playerIndex(po)==forPlayer))
    	{	n++;  		
    	}
    	return(n);
    }
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public CannonChip pickedObject = null;
    private CannonCell pickedSource = null;
    public CannonCell droppedDest = null;
    public CannonChip captured = null;
    


	// factory method for the board
	public CannonCell newcell(char c,int r)
	{	return(new CannonCell(c,r,CannonId.BoardLocation));
	}
    public CannonBoard(String init,int cmap[]) // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = CANNONGRIDSTYLE; //coordinates left and bottom
    	Random r = new Random(105859505);
    	setColorMap(cmap, 2);
     	for(int i=0;i<4; i++)
 		{
 		int idx = i%2;
 		CannonCell cell = new CannonCell(r);
 		CannonChip chip = CannonChip.getChip(i);
 		cell.row = idx;
 		cell.rackLocation = chip.color;
 		cell.addChip(chip);
 		((chip.color==CannonId.White_Chip_Pool) ? whiteChips : blackChips)[idx]=cell;
 		} 
        doInit(init,0L); // do the initialization 
     }


    private boolean sameCell(CannonCell a,CannonCell b)
    {	return((a==null) ? (b==null) : a.sameCell(b)); 
    }

    public void sameboard(BoardProtocol b) { sameboard((CannonBoard)b); }
    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(CannonBoard from_b)
    {	super.sameboard(from_b);
    	G.Assert(AR.sameArrayContents(townCaptured,from_b.townCaptured), "townCaptured mismatch");
    	G.Assert(sameCells(townLocation,from_b.townLocation),"towns match");
    	G.Assert(pickedObject== from_b.pickedObject, "pickedObject matches");
        G.Assert(pickedObject== from_b.pickedObject, "pickedObject matches");
        G.Assert(sameCell(pickedSource,from_b.pickedSource), "pickedSource matches");
        G.Assert(sameCell(droppedDest,from_b.droppedDest), "droppedDest matches");
         
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Digest matches");

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

		v ^= (r.nextLong()*(board_state.ordinal()*10+whoseTurn));
		v ^= cell.Digest(r,pickedSource);
		v ^= chip.Digest(r,pickedObject);
        return (v);
    }
   public CannonBoard cloneBoard() 
	{ CannonBoard copy = new CannonBoard(gametype,getColorMap());
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((CannonBoard)b); }

   public CannonCell getCell(CannonCell o)
   {	if(o!=null)
   		{	return(getCell(o.rackLocation(),o.col,o.row));
   		}
   		return(null);
   }
    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(CannonBoard from_b)
    {	super.copyFrom(from_b);
        
    	pickedObject = from_b.pickedObject;
        pickedSource = getCell(from_b.pickedSource);
        droppedDest = getCell(from_b.droppedDest);
        robotDepth = from_b.robotDepth;
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        AR.copy(chips_on_board,from_b.chips_on_board);
        
        copyFrom(rack,from_b.rack);
        copyFrom(capturedChips,from_b.capturedChips);
        
        AR.copy(townCaptured,from_b.townCaptured);
        getCell(townLocation,from_b.townLocation);
        sameboard(from_b);
    }
    int playerIndex(CannonChip ch) { return(ch.color==playerId[0] ? 0 : 1); }
    CannonChip playerChip[]=new CannonChip[] {CannonChip.BlueTown,CannonChip.WhiteTown};
    public void addChip(CannonCell c,CannonChip chip)
    {	
    	if(c.onBoard) 
    		{ int index = playerIndex(chip);
    		  chips_on_board[index]++; 
    		  if(chip.isTown())
    		  {	townLocation[index]=c;
    		    townCaptured[index]=false;
    		  }
    		}
    	c.addChip(chip);
    }
    public CannonChip removeChip(CannonCell c)
    {	
    	CannonChip top = c.removeTop();
    	if(c.onBoard) 
    		{ int index = playerIndex(top);
    		  chips_on_board[index]--; 
    		  if(top.isTown()) 
    		  	{ 
    		  	  townLocation[index]=null;
    		  	  townCaptured[index]=(board_state==CannonState.PLAY_STATE);
    		  	}
    		}
    	return(top);
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long rv)
    {	randomKey = rv;
    	if(!Cannon_INIT.equalsIgnoreCase(gtype)) 
		{ throw G.Error(WrongInitError,gtype); 
		}
    	gametype = gtype;
    	setState(CannonState.PUZZLE_STATE);
    	
    	int map[]=getColorMap();
     	playerId[map[0]]=CannonId.White_Chip_Pool;
     	playerId[map[1]]=CannonId.Black_Chip_Pool;
     	rack[map[0]] = whiteChips;
     	rack[map[1]] = blackChips;
     	capturedChips[map[0]] = whiteCaptured;
     	capturedChips[map[1]] = blackCaptured;
     	blackCaptured.reInit();
     	whiteCaptured.reInit();
     	playerChip[map[0]] = CannonChip.WhiteTown;
     	playerChip[map[1]] = CannonChip.BlueTown;

     	reInitBoard(boardColumns,boardRows); //this sets up the board and cross links

       AR.setValue(win,false);
       AR.setValue(chips_on_board,0);
       AR.setValue(townCaptured,false);
       AR.setValue(townLocation,null);

       CannonChip soldier[]= {CannonChip.BlueSoldier,CannonChip.WhiteSoldier};
       for(int colNum=0;colNum<boardColumns;colNum+=2)
    	  {for(int row=2; row<=4; row++)
    	  	{	CannonCell blue = getCell((char)('A'+colNum),row);
	     	  	addChip(blue,soldier[map[0]]);
	     	  	CannonCell white = getCell((char)('A'+boardColumns-colNum-1),boardRows-row+1);
       	  		addChip(white,soldier[map[1]]);
    	  	}
    	  }
        moveNumber = 1;
        whoseTurn = FIRST_PLAYER_INDEX;
        robotDepth = 0;
        pickedSource = null;
        droppedDest = null;
        captured = null;
        pickedObject = null;
        
 
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


   public boolean uniformRow(int player,CannonCell cell,int dir)
    {	for(CannonCell c=cell.exitTo(dir); c!=null; c=c.exitTo(dir))
    	{    CannonChip cup = c.topChip();
    	     if(cup==null) { return(false); }	// empty cell
    	     if(cup.chipNumber()!=player) { return(false); }	// cell covered by the other player
    	}
    	return(true);
    }
    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(CannonState.GAMEOVER_STATE);
    }
    
    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	if(board_state==CannonState.GAMEOVER_STATE) { return(win[player]); }
    	return(false);
    }
    // look for a win for player.  This algorithm should work for Gobblet Jr too.
    public double ScoreForPlayer(int player,boolean print,boolean dumbot)
    {  	double chipv = 10*chips_on_board[player];
    	double finalv = chipv;
		CannonCell loc1 = townLocation[player];
		CannonCell loc2 = townLocation[nextPlayer[player]];
		String msg="";
    	if((loc1!=null)&&(loc2!=null))
    	{
    	double sum1 = 0.0;
    	double sum2 = 0.0;
    	char col1 = loc1.col;
    	int row1 = loc1.row;
    	char col2 = loc2.col;
    	int row2 = loc2.row;
    	int edges = 0;
    	int cannons = 0;
    	for(CannonCell c = allCells; c!=null; c=c.next)
    		{
    		CannonChip top = c.topChip();
    		if((top!=null)&&(top.color==playerId[player])&&top.isSoldier())
    		{	char col = c.col;
    			int row = c.row;
    			sum1 += 2.0/Math.sqrt((col-col1)*(col-col1)+(row-row1)*(row-row1));
    			sum2 += 3.0/Math.sqrt((col-col2)*(col-col2)+(row-row2)*(row-row2));
    			for(int dir=0;dir<4;dir++)
    			{	CannonCell cx = c.exitTo(dir);
    				if(cx==null) { edges++; }
    				else 
    				{ CannonChip top1 = cx.topChip();
    				  if(top1==top)
    				  {	CannonCell cx2 = c.exitTo(dir+4);
    				    if(cx2==null) { edges++; }
    				    else { CannonChip top2 = cx2.topChip();
    				     	   if(top2==top) { cannons++; }
    				    }
    				  }
    				}
    			}
    			
    		}
    		}
    	finalv += sum1+sum2;
    	finalv += cannons;
    	finalv -= edges/3.0;
    	if(print) { msg = "s1="+sum1+" s2="+sum2+" cannon="+cannons+" edge="+(edges/-3.0); }
    	}
    	    	
    	if(print) { System.out.println("For "+player+" "+msg+" ="+finalv); }
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
        captured = null;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    CannonCell dr = droppedDest;
    if(dr!=null)
    	{
    	droppedDest = null;
    	if(dr.topChip()!=null) { pickedObject = removeChip(dr); }
    	if(captured!=null) { addChip(dr,captured); captured = null;  }
    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	CannonChip po = pickedObject;
    	if(po!=null)
    	{
    	CannonCell ps = pickedSource;
    	pickedSource=null;
    	pickedObject = null;
    	if(ps.topChip()==null) { addChip(ps,po); }
     	}
     }
    // 
    // drop the floating object.
    //
    private void dropObject(CannonId dest, char col, int row)
    {
       G.Assert((pickedObject!=null)&&(droppedDest==null),"ready to drop");
       switch (dest)
        {
        default: throw G.Error("Not expecting dest %s",dest);
        case BoardLocation: // an already filled board slot.
        	droppedDest = getCell(col,row);
        	if(droppedDest.topChip()!=null) { removeChip(droppedDest); }
        	addChip(droppedDest,pickedObject);
        	pickedObject = null;
        	break;
        case Black_Chip_Pool:		// back in the pool
        case White_Chip_Pool:		// back in the pool
        	acceptPlacement();
            break;
        }
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(CannonCell cell)
    {	return(droppedDest==cell);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    {	CannonChip c = pickedObject;
    	if((c!=null)&&(droppedDest==null))
    		{ return(c.chipNumber());
    		}
        return (NothingMoving);
    }
    public CannonCell getCell(CannonId source,char col,int row)
    {
        switch (source)
        {
        default:
            throw G.Error("Not expecting source %s", source);
         case BoardLocation:
        	return(getCell(col,row));
         case White_Chip_Pool:
        	return(whiteChips[row]);
         case Black_Chip_Pool:
        	return(blackChips[row]);
         case White_Captured:
        	 return(whiteCaptured);
         case Black_Captured:
        	 return(blackCaptured);
        }
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(CannonId source, char col, int row)
    {	G.Assert((pickedObject==null)&&(pickedSource==null),"ready to pick");
    	CannonCell c = pickedSource = getCell(source,col,row);
    	
        switch (source)
        {
        default:
            throw G.Error("Not expecting source %s", source);
        case BoardLocation:
         	{
         	pickedObject = removeChip(c);
        	if(pickedObject.isTown())
        		{ int index = playerIndex(pickedObject);
        		  townLocation[index]=null; 
        		  townCaptured[index]=(board_state==CannonState.PLAY_STATE);
        		}
         	break;
         	}
        case White_Chip_Pool:
        	{
       		pickedObject = c.topChip();
        	break;
        	}
        case Black_Chip_Pool:
        	{
       		pickedObject = c.topChip();
        	break;
        	}
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
        case PLACE_TOWN_STATE:
			setState(CannonState.CONFIRM_STATE);
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
        	setNextStateAfterDone();
        	break;
        case PLAY_STATE:
        case PLACE_TOWN_STATE:
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
    	case PUZZLE_STATE:
    	case PLAY_STATE:
    	case PLACE_TOWN_STATE:
    		if(townCaptured[whoseTurn]
    		    ||((pickedObject==null)&&!hasLegalMoves(whoseTurn))) 
    			{ setGameOver(false,true); 
    			}
    			else 
    			{	setState((townLocation[whoseTurn]==null)?CannonState.PLACE_TOWN_STATE:CannonState.PLAY_STATE);
    			}
    		break;
    	}

    }
   

    
    private void doDone()
    {	
        acceptPlacement();

        if (board_state==CannonState.RESIGN_STATE)
        {	setGameOver(false,true);
        }
        else
        {	setNextPlayer(); setNextStateAfterDone();
        }
    }

    public boolean Execute(commonMove mm,replayMode replay)
    {	CannonMovespec m = (CannonMovespec)mm;

        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone();

            break;
        case MOVE_RACK_BOARD:
           	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case PLACE_TOWN_STATE:
        			G.Assert((pickedObject==null)&&(droppedDest==null),"something is moving");
                    pickObject(m.source, m.from_col, m.from_row);
                    dropObject(CannonId.BoardLocation,m.to_col,m.to_row);
                    if(replay!=replayMode.Replay)
                    {
                    	animationStack.push(pickedSource);
                    	animationStack.push(droppedDest);
                    }
                    setNextStateAfterDrop();
                    break;
        	}
        	break;
        case SHOOT2_BOARD_BOARD:
        case SHOOT3_BOARD_BOARD:
        	{
        	CannonCell dest = getCell(m.to_col,m.to_row);
        	CannonChip top = dest.topChip();
        	G.Assert(top!=null,"something captured");
        	m.capture = top;
        	captured = top;
        	removeChip(dest);
        	droppedDest = dest;
        	CannonCell deadPool = capturedChips[nextPlayer[whoseTurn]];
			deadPool.addChip(top);
        	if(replay!=replayMode.Replay)
             {	CannonCell from = getCell(m.from_col,m.from_row);
             	animationStack.push(from);
             	CannonCell d = new CannonCell(dest);
             	// operating a cannon, the destination becomes empty
             	d.addChip(from.topChip());
             	animationStack.push(d);
             	CannonCell d2 = new CannonCell(dest);
             	d2.addChip(captured);
             	animationStack.push(d2);
             	animationStack.push(d2);
             	animationStack.push(d);
             	animationStack.push(deadPool);
             } 	

        	setState(CannonState.CONFIRM_STATE);
        	}
        	break;
        case CAPTURE_BOARD_BOARD:
        	{
       		G.Assert((pickedObject==null)&&(droppedDest==null),"something is moving");
        	CannonCell dest = getCell(m.to_col,m.to_row);
        	CannonChip top = dest.topChip();
        	CannonCell deadPool = capturedChips[nextPlayer[whoseTurn]];
        	G.Assert(top!=null,"something captured");
        	m.capture = top;
        	deadPool.addChip(top);
        	pickObject(CannonId.BoardLocation, m.from_col, m.from_row);
			dropObject(CannonId.BoardLocation,m.to_col,m.to_row); 
			
            if(replay!=replayMode.Replay)
            {
            	animationStack.push(pickedSource);
            	animationStack.push(droppedDest);
            	animationStack.push(droppedDest);
            	animationStack.push(deadPool);
            }

	        setState(CannonState.CONFIRM_STATE);
        	}
        	break;

        case RETREAT_BOARD_BOARD:
        case SLIDE_BOARD_BOARD:
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case PLAY_STATE:
        			G.Assert((pickedObject==null)&&(droppedDest==null),"something is moving");
        			pickObject(CannonId.BoardLocation, m.from_col, m.from_row);
        			dropObject(CannonId.BoardLocation,m.to_col,m.to_row); 
                    if(replay!=replayMode.Replay)
                    {
                    	animationStack.push(pickedSource);
                    	animationStack.push(droppedDest);
                    }

 				    setNextStateAfterDrop();
        			break;
        	}
        	break;
        case MOVE_DROPB:
        	G.Assert(pickedObject!=null,"something is moving");
        	// annotate with the origin for the benefit of shortMoveString
        	m.from_col = pickedSource.col;
        	m.from_row = pickedSource.row;	
        	m.source = pickedSource.onBoard
        				?CannonId.BoardLocation
        				:pickedObject.color;
			{CannonCell dest = getCell(m.to_col, m.to_row); 
			 CannonChip top = dest.topChip();
			 CannonCell deadPool = capturedChips[nextPlayer[whoseTurn]];
			 boolean remove = top!=null;
			 if(remove)
			 	{ removeChip(dest);
			 	  m.capture=top; 
			 	  captured = top;
			 	  deadPool.addChip(captured);
			 	}
				// look for a cannon shot
			 boolean cannon = pickedSource.onBoard 
			 		&& (board_state!=CannonState.PUZZLE_STATE)
			 		&& (Math.max(Math.abs(pickedSource.row-dest.row),
			 					 Math.abs(pickedSource.col-dest.col))>3);
  

			if(cannon) 
				{ // cannon
				CannonChip po = pickedObject;
				dropObject(CannonId.BoardLocation,pickedSource.col,pickedSource.row);
				droppedDest = dest;

				 if(replay==replayMode.Single)
	             {
	             	animationStack.push(pickedSource);
	             	CannonCell d = new CannonCell(dest);
	             	d.addChip(po);
	             	animationStack.push(d);
	             	
	             }


				}
			else
			{
		           dropObject(CannonId.BoardLocation, m.to_col, m.to_row);

					 if(replay==replayMode.Single)
		             {
		             	animationStack.push(pickedSource);
		             	animationStack.push(droppedDest);
		             }
			}
			if(remove)
			{
				 if(replay!=replayMode.Replay)
				 {
					 animationStack.push(dest);
					 animationStack.push(deadPool);
				 }	
			}
             if(!cannon && (pickedSource==droppedDest)) 
            	{ unDropObject(); 
            	  unPickObject(); 

            	} 
            	else
            		{
            		setNextStateAfterDrop();
            		}
			}
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	if(isDest(getCell(m.from_col,m.from_row)))
        		{ unDropObject(); 
        		  setNextStateAfterDone();
        		}
        	else 
        		{ pickObject(CannonId.BoardLocation, m.from_col, m.from_row);
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
            dropObject(m.source, m.to_col, m.to_row);
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
            acceptPlacement();
            unPickObject();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(CannonState.PUZZLE_STATE);
            {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;

        case MOVE_RESIGN:
        	setState(unresign==null?CannonState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(CannonState.PUZZLE_STATE);
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
    public boolean LegalToHitChips(CannonChip ch)
    {	int player = playerIndex(ch);
        switch (board_state)
        {
        default:
            throw G.Error("Not expecting state %s", board_state);
         case PLACE_TOWN_STATE:
        	 return((player==whoseTurn) && ch.isTown());
         case CONFIRM_STATE:
         case DRAW_STATE:
         case RESIGN_STATE:
         case PLAY_STATE: 
        	return(false);
		case GAMEOVER_STATE:
			return(false);
        case PUZZLE_STATE:
        	return((pickedObject==null)?true:(pickedObject==ch));
        }
    }
  

    public boolean LegalToHitBoard(CannonCell cell)
    {	
        switch (board_state)
        {
 		case PLAY_STATE:
 			if((pickedObject!=null)&&(pickedSource!=null)&& pickedSource.onBoard)
 			{
 			if(pickedSource==cell) { return(true); }
 			Hashtable<CannonCell,CannonCell> h = movingObjectDests();
 			return(h.get(cell)!=null);
 			}
 			{
 			CannonChip top = cell.topChip();
 			return((top!=null)&&(top.color==playerId[whoseTurn])&&top.isSoldier());
 			}
 		case RESIGN_STATE:
		case GAMEOVER_STATE:
			return(false);
		case CONFIRM_STATE:
		case DRAW_STATE:
			return(isDest(cell));
        default:
            throw G.Error("Not expecting state %s", board_state);
        case PLACE_TOWN_STATE:
        	if(pickedObject==null)
        	{
        	CannonChip top = cell.topChip();
        	return((top!=null) && (top.color==playerId[whoseTurn]) && top.isTown());
        	}
        	return(cell.isEdgeCell() 
        			&& (cell.row==((whoseTurn==FIRST_PLAYER_INDEX)?boardRows:1))
        			&& (cell.col>'A')
        			&& (cell.col<(char)('A'+boardColumns-1)));
        case PUZZLE_STATE:
        	boolean can = pickedObject==null?(cell.chipIndex>=0):true;
        	return(can);
        }
    }
  public boolean canDropOn(CannonCell cell)
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
    public void RobotExecute(CannonMovespec m)
    {
        m.state = board_state; //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        robotDepth++;
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
    public void UnExecute(CannonMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	robotDepth--;
    	setState(m.state);
        switch (m.op)
        {
   	    default:
            cantUnExecute(m);
        	break;
        case MOVE_DONE:
            break;
        case MOVE_RACK_BOARD:
           	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
        		case PLAY_STATE:
        		case PLACE_TOWN_STATE:
        			G.Assert((pickedObject==null)&&(droppedDest==null),"something is moving");
        			pickObject(CannonId.BoardLocation,m.to_col,m.to_row);
        			dropObject(m.source,m.from_col, m.from_row);
       			    acceptPlacement();
                    break;
        	}
        	break;
        case CAPTURE_BOARD_BOARD:
        	{
       		CannonCell dest = getCell(m.to_col,m.to_row);
       		CannonCell src = getCell(m.from_col,m.from_row);
       		CannonChip top = removeChip(dest);
       		G.Assert(top!=null,"something at dest");
       		G.Assert(src.topChip()==null,"source empty");
       		addChip(src,top);

        	}
        	/*$FALL-THROUGH$*/
        case SHOOT2_BOARD_BOARD:
        case SHOOT3_BOARD_BOARD:
        	{
        	CannonCell dest = getCell(m.to_col,m.to_row);
        	CannonChip ch = m.capture;
        	G.Assert(ch!=null,"something captured");
        	addChip(dest,ch);
        	CannonCell deadpool = capturedChips[nextPlayer[m.player]];
        	deadpool.removeTop();
        	}
        	break;
        case MOVE_BOARD_BOARD:
        case SLIDE_BOARD_BOARD:
        case RETREAT_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
        		case PLAY_STATE:
        			G.Assert((pickedObject==null)&&(droppedDest==null),"something is moving");
        			pickObject(CannonId.BoardLocation, m.to_col, m.to_row);
       			    dropObject(CannonId.BoardLocation, m.from_col,m.from_row); 
       			    acceptPlacement();
        			break;
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(m.state);
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
 int getTownMoves(CommonMoveStack  v,int who)
 {	int n=0;
 	int homeRow = (who==FIRST_PLAYER_INDEX) ? boardRows : 1;
 	for(int i=1;i<(boardColumns-1);i++)
 	{	char col = (char)('A'+i);
 		if(getCell(col,homeRow).topChip()==null)
 		{	n++;
 			if(v!=null) { v.addElement(new CannonMovespec(MOVE_RACK_BOARD,playerId[who],who,0,col,homeRow)); }
 		}
 	}
 	return(n);
 }
 int getSoldierMovesFor(CannonCell c,CannonChip top,CommonMoveStack  v,int who)
 {	
 	int n=0;
 	if((top!=null)&&(top.color==playerId[who]) && top.isSoldier())
	{
	int direction = (who==FIRST_PLAYER_INDEX) ? CELL_DOWN : CELL_UP;
 	int firstDir = direction-2;
 	int lastDir = direction+2;
 	int retreat2 = direction+1-4;
 	int retreat1 = direction-1-4;
	//forward and capturing moves
 	boolean adjacent_to_enemy=false;
 	for(int dir=firstDir; dir<=lastDir; dir++)
 			{
 			CannonCell nx = c.exitTo(dir);
 			if(nx!=null)
 				{
 				CannonChip nxtop = nx.topChip();
 				if((nxtop==null)||(nxtop.color!=playerId[who]))
 				{
 				boolean enemy = (nxtop!=null);
 				int op = enemy?CAPTURE_BOARD_BOARD:MOVE_BOARD_BOARD;
 				adjacent_to_enemy |= enemy;
 			    if(enemy || ((dir>firstDir) && (dir<lastDir)) )
 					  {
 					  // move forward, capture sideways or forward
 					  n++;
 					  if(v!=null)
 						  {v.addElement(new CannonMovespec(op,playerId[who],who,c.col,c.row,nx.col,nx.row));
 						  }
 					  }
 					}
 				}
 				}
 				// if not already enemy adjacent, check the other directions
 				for(int dir=retreat1; !adjacent_to_enemy && (dir<=retreat2); dir++)
 				{	CannonCell nx = c.exitTo(dir);
 					if(nx!=null)
 					{ CannonChip nxtop = nx.topChip();
 					  adjacent_to_enemy |= ((nxtop!=null)&&(nxtop.color!=playerId[who]));
 					}
 				}
 				// try retreat moves
 				if(adjacent_to_enemy)
 				{for(int dir=retreat1; dir<=retreat2;dir+=1)
 				{	CannonCell nx = c.exitTo(dir);
 					if((nx!=null) && (nx.topChip()==null)) 
 					{	nx = nx.exitTo(dir);
 						if((nx!=null) && (nx.topChip()==null))
 						{
 							n++;
 							if(v!=null)
 							{v.addElement(new CannonMovespec(RETREAT_BOARD_BOARD,playerId[who],who,c.col,c.row,nx.col,nx.row));
 							}
 						}
 					}
 				}}
 				// try slide moves and cannon moves
 				for(int dir=0;dir<CELL_FULL_TURN;dir++)
 				{	// next 2 are friends, we have a cannon
 					CannonCell nx = c.exitTo(dir);
 					if(nx!=null)
 					{
 					CannonChip nxtop = nx.topChip();
 					if(top==nxtop)
 					{
 					nx = nx.exitTo(dir);
 					if(nx!=null)
 					{
 					nxtop = nx.topChip();
 					if(top==nxtop)
 					{// 3 in a row
 					nx = nx.exitTo(dir);
 					if(nx!=null)
 						{
 						nxtop=nx.topChip();
 						if(nxtop==null)
 						{	// 3 in a row followed by an empty cell
 							// we can slide or shoot
 							n++;
 							if(v!=null) 
 							{ v.addElement(new CannonMovespec(SLIDE_BOARD_BOARD,playerId[who],who,c.col,c.row,nx.col,nx.row));
 							}
 							
 						// now two possible shooting moves
 						nx = nx.exitTo(dir);
 						if(nx!=null)
 						{
 						nxtop = nx.topChip();
 						if((nxtop!=null) && (nxtop.color!=playerId[who]))
 						{	// shoot 2
 							n++;
 							if(v!=null)
 							{v.addElement(new CannonMovespec(SHOOT2_BOARD_BOARD,playerId[who],who,c.col,c.row,nx.col,nx.row));
 							}
 						}
 						if(nx!=null)
 						{	// shoot 2
 							nx = nx.exitTo(dir);
 							if(nx!=null)
 							{
 							nxtop = nx.topChip();
 							if((nxtop!=null)&&(nxtop.color!=playerId[who]))
 							{
 								n++;
 	 							if(v!=null)
 	 							{v.addElement(new CannonMovespec(SHOOT3_BOARD_BOARD,playerId[who],who,c.col,c.row,nx.col,nx.row));
 	 							}
 							}
 							}
 							
 						}
 						}}
 						}
 					}
 					}
 					}
 					}
 				}
 			}
 	return(n);
 }
 
 int getSoldierMoves(CommonMoveStack  v,int who)
 {	int n=0;

 	for(CannonCell c = allCells;
 	    c!=null;
 	    c = c.next)
 	{	
 		n += getSoldierMovesFor(c,c.topChip(),v,who);
 	}
 		
 	return(n);
 }
 boolean hasLegalMoves(int who)
 {	for(CannonCell c = allCells; c!=null; c=c.next)
 	{
	CannonChip top = c.topChip();
	if((top!=null)
			&&(top.color==playerId[who])
			&&top.isSoldier())
			{
			if(getSoldierMovesFor(c,top,null,who)>0) { return(true); }
			}
 	}
	return(false);
 }
 public Hashtable<CannonCell,CannonCell> movingObjectDests()
 {	Hashtable<CannonCell,CannonCell> h = new Hashtable<CannonCell,CannonCell>();
 	if((droppedDest==null)&&(pickedObject!=null)&&(pickedSource!=null)&&(pickedSource.onBoard))
 	{	CommonMoveStack v = new CommonMoveStack();
 		getSoldierMovesFor(pickedSource,pickedObject,v,playerIndex(pickedObject));
 		for(int i=0;i<v.size();i++)
 		{	CannonMovespec m = (CannonMovespec)v.elementAt(i);
 			CannonCell c = getCell(m.to_col,m.to_row);
 			h.put(c,c);
 		}
 	}
 	return(h);
 }
 int getMovesFor(CommonMoveStack  v,int who)
 {	int n=0;
	switch (board_state)
	{
	default: throw G.Error("Not implemented");
	case PLACE_TOWN_STATE:
		n += getTownMoves(v,who);
		break;
	case PLAY_STATE:
		n += getSoldierMoves(v,who);
		break;
	}
	return(n);
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	getMovesFor(all,whoseTurn);
  	return(all);
 }
 
}
