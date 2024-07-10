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
package dipole;

import online.game.*;

import java.util.*;

import lib.*;
import lib.Random;

//
/**
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
class CellStack extends OStack<DipoleCell>
{
	public DipoleCell[] newComponentArray(int n) { return(new DipoleCell[n]); }
}
class DipoleBoard extends rectBoard<DipoleCell> implements BoardProtocol,DipoleConstants
{ 	static int FIRST_FORWARD_DIRECTION[]={CELL_UP_LEFT,CELL_DOWN_RIGHT};
	static final int INITIAL_CHIP_HEIGHT = 12;	// chips in the initial stack
	static final int BIG_CHIP_HEIGHT = 20;	// more
	static final int DEFAULT_BOARDSIZE = 8;	// 8x8 board
	static final int BIG_BOARDSIZE = 10;	// 10x10 board
	static final String[] DIPOLEGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	private int prevLastDropped = -1;
	private int prevLastPicked = -1;
 	DipoleState unresign;
	DipoleState board_state;
	public DipoleState getState() {return(board_state); }
	public void setState(DipoleState st) 
	{ 	unresign = (st==DipoleState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public DipoleCell darkCells[] = null;
    public int boardSize = DEFAULT_BOARDSIZE;	// size of the board
    private boolean symmetric_start = false;
    public void SetDrawState() { throw G.Error("this shoulnd't happen"); }
    public DipoleCell rack[] = null;
    public DipoleCell Waste_Cell = null;
    public DipoleChip playerChip[]=new DipoleChip[2];
	
   public int chips_on_board[] = new int[2];			// number of chips currently on the board
   public int sum_of_rows[] = new int[2];
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public DipoleChip pickedObject = null;
    DipoleCell pickedSource = null;
    DipoleCell droppedDest = null;
    private int droppedOriginalHeight = 0;		// the original height of the dropped stack
    public CellStack animationStack = new CellStack();
    

	// factory method
	public DipoleCell newcell(char c,int r)
	{	return(new DipoleCell(c,r));
	}
    public DipoleBoard(String init,int map[]) // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = DIPOLEGRIDSTYLE; //coordinates left and bottom
    	setColorMap(map, 2);
        doInit(init); // do the initialization 
     }

    // temporary list of destination cells allocate as a resource for speed
    private DipoleCell[][]tempDestResource = new DipoleCell[4][];
    private int tempDestIndex=-1;
    public synchronized DipoleCell []getTempDest() 
    	{ if(tempDestIndex>=0) { return(tempDestResource[tempDestIndex--]); }
    	  return(new DipoleCell[(ncols+1)*3]);
    	}
    public synchronized void returnTempDest(DipoleCell[]d) { tempDestResource[++tempDestIndex]=d; }


    // standared init for Hex.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard(String game)
    { 	if(Dipole_INIT.equalsIgnoreCase(game)) 
    		{ symmetric_start=false;
    		  boardSize=DEFAULT_BOARDSIZE; 
     		}
    	else if(Dipole_10_INIT.equalsIgnoreCase(game))
    	{	symmetric_start=true;
    		boardSize=BIG_BOARDSIZE; 
    	}
    else if(Dipole_s_INIT.equalsIgnoreCase(game))
    {	symmetric_start=true;
    	boardSize=DEFAULT_BOARDSIZE; 
    }
    	else { throw G.Error(WrongInitError,game); }
        gametype = game;
        setState(DipoleState.PUZZLE_STATE);
        reInitBoard(boardSize,boardSize); //this sets up the board and cross links'
        AR.setValue(chips_on_board, 0);
        AR.setValue(sum_of_rows, 0);

        // temporary, fill the board
        darkCells=new DipoleCell[nrows*ncols/2];
        int idx=0;
        for(DipoleCell c = allCells; c!=null; c=c.next)
        {  int i = ((c.row+c.col)%2)^1;
           c.addChip(DipoleChip.getTile(i));
           if((i&1)!=0) 
           { darkCells[idx++] = c;	// we play only with the dark cells 
             c.isDark = true;
           }
           
        }
        { int firstcol = nrows/2-1;
          int chipheight = (nrows>8) ? BIG_CHIP_HEIGHT : INITIAL_CHIP_HEIGHT;
          DipoleCell WC = getCell( (char)('A'+firstcol) ,nrows);
          DipoleCell BC = symmetric_start ? getCell((char)('A'+ncols-firstcol-1),1) : getCell('C',1);
          int map[] = getColorMap();
          for(int i=0;i<chipheight;i++)
          {	BC.addChip(DipoleChip.getChip(map[0]));
            WC.addChip(DipoleChip.getChip(map[1]));
            chips_on_board[FIRST_PLAYER_INDEX]++;
            chips_on_board[SECOND_PLAYER_INDEX]++;
            sum_of_rows[FIRST_PLAYER_INDEX]+=rowscore(BC,FIRST_PLAYER_INDEX);
            sum_of_rows[SECOND_PLAYER_INDEX]+=rowscore(WC,SECOND_PLAYER_INDEX);
          }
        }
        whoseTurn = FIRST_PLAYER_INDEX;

    }

    public void sameboard(BoardProtocol f) { sameboard((DipoleBoard)f); }
    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(DipoleBoard from_b)
    {	super.sameboard(from_b);
    	G.Assert(AR.sameArrayContents(chips_on_board, from_b.chips_on_board),"Chip count mismatch");
       	G.Assert(AR.sameArrayContents(sum_of_rows, from_b.sum_of_rows),"sum_of_rows mismatch");

        G.Assert(DipoleCell.sameCell(droppedDest,from_b.droppedDest), "droppedDest matches");
        G.Assert(DipoleCell.sameCell(pickedSource,from_b.pickedSource), "pickedSource matches");
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
       
		v ^= cell.Digest(r,pickedSource);
		v ^= chip.Digest(r,pickedObject);
		v ^= r.nextLong()*(board_state.ordinal()*10+1);
        return (v);
    }
    public DipoleBoard cloneBoard() 
	{ DipoleBoard dup = new DipoleBoard(gametype,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((DipoleBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(DipoleBoard from_b)
    {	super.copyFrom(from_b);
        pickedSource = getCell(from_b.pickedSource);
        droppedDest = getCell(from_b.droppedDest);
        pickedObject = from_b.pickedObject;
        copyFrom(Waste_Cell,from_b.Waste_Cell);
        AR.copy(chips_on_board,from_b.chips_on_board);
        AR.copy(sum_of_rows,from_b.sum_of_rows);
        copyFrom(darkCells,from_b.darkCells);         
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        sameboard(from_b);
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long randomv)
    {	randomKey = randomv;
    	rack = new DipoleCell[2];
    	Random r =  new Random(22346752);
    	animationStack.clear();
    	prevLastPicked = -1;
    	prevLastDropped = -1;
    	int map[]=getColorMap();
    	for(int i=0,pl=FIRST_PLAYER_INDEX;i<2; i++,pl=nextPlayer[pl])
    	{
       	DipoleCell cell = new DipoleCell(r,DipoleId.Common_Pool,i);
    	cell.onBoard=false;
    	DipoleChip chip = DipoleChip.getChip(map[pl]);
    	playerChip[i]=chip;
        rack[i]=cell;
        cell.addChip(chip);
    	}  
        Waste_Cell = new DipoleCell(r,DipoleId.Waste_Rect,0);
        Waste_Cell.col = 'X';
        Waste_Cell.addChip(DipoleChip.Waste);	// so it has the same base level as everything else

       Init_Standard(gtype);
       pickedObject = null;
        droppedDest = null;
        pickedSource = null;
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
        case PASS_STATE:
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
            return (true);

        default:
            return (false);
        }
    }
    public boolean DigestState()
    {	
    	switch(board_state)
    	{	case PASS_STATE: return(false);
    		default: return(DoneState());
    	}
    }

   public boolean uniformRow(int player,DipoleCell cell,int dir)
    {	for(DipoleCell c=cell.exitTo(dir); c!=null; c=c.exitTo(dir))
    	{ 
    		if(c.topChip()!=playerChip[player]) { return(false); }	// cell covered by the other player
    	}
    	return(true);
    }
    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(DipoleState.GAMEOVER_STATE);
    }
    public boolean WinForPlayerNow(int player)
    {	if(board_state==DipoleState.GAMEOVER_STATE) { return(win[player]); }
    	return(chips_on_board[nextPlayer[player]]==0);
     }

    // look for a win for player.  This algorithm should work for Gobblet Jr too.
    public double ScoreForPlayer(int player,boolean print)
    {  	double finalv=sum_of_rows[player];
    	if(print) { System.out.println("sum "+finalv); }
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
        droppedOriginalHeight = 0;
        Waste_Cell.chipIndex = 0;
        pickedSource = null;
     }
    private int playerIndex(DipoleChip ch) 
    {
    	return((ch==playerChip[0]) ? 0 : 1);
    }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    DipoleCell dr = droppedDest;
	droppedDest = null;
    if(dr!=null)
    	{
    	dr.lastDropped = prevLastDropped;
    	prevLastDropped = -1;
    	if(dr==Waste_Cell)
    	{	while(Waste_Cell.chipIndex > 0) 
    			{ Waste_Cell.chipIndex--;
    			  if(Waste_Cell.chipIndex>0)
    				  {pickedSource.addChip(pickedObject);
    				   int pl = playerIndex(pickedObject);
    				   chips_on_board[pl]++;
    				   sum_of_rows[pl] += rowscore(pickedSource,pl);
    				  }
    			}
    	}
    	else if(board_state==DipoleState.CONFIRM_STATE)
    	{	// unmove the stack, uncapture the captures
    		int movedChips = dr.chipIndex - droppedOriginalHeight;
    		int pl = playerIndex(pickedObject);
    		int otherPlayer = nextPlayer[pl];
    		chips_on_board[pl]--;
    		while(movedChips-->0)
    		{	dr.removeTop(pickedObject);
    			sum_of_rows[pl] -= rowscore(dr,pl);
    			if(movedChips>0) { pickedSource.addChip(pickedObject); sum_of_rows[pl] += rowscore(pickedSource,pl); }
    		}
    		int chips = Waste_Cell.chipIndex;
    		if(chips>0)
    		{
    		DipoleChip other = playerChip[nextPlayer[otherPlayer]];
		    chips_on_board[otherPlayer] += chips;
    		sum_of_rows[otherPlayer] += rowscore(dr,otherPlayer)*chips;
    		Waste_Cell.chipIndex = 0;
    		while(chips-- > 0) 
			{ dr.addChip(other); 
			}
    		}
    	}
    	else
    	{
    	int pl = playerIndex(pickedObject);
    	dr.removeTop(pickedObject);
    	chips_on_board[pl]--;
    	sum_of_rows[pl] -= rowscore(pickedSource,pl);
    	}
    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	DipoleChip po = pickedObject;
    	if(po!=null)
    	{
    	DipoleCell ps = pickedSource;
    	pickedSource=null;
    	pickedObject = null;
    	ps.lastPicked = prevLastPicked;
    	prevLastPicked = -1;
    	ps.addChip(po);
    	if(ps.onBoard) 
    		{int pl = playerIndex(po);
    		 chips_on_board[pl]++; 
    		 sum_of_rows[pl] += rowscore(ps,pl); 
    		}
    	}
     }
    public int distanceToEdge() { return(distanceToEdge(pickedSource)); }

    public int distanceToEdge(DipoleCell from)
    {  	int mindis = 999;
		if((from!=null) && from.onBoard)
    	{	int player = (from==pickedSource)
    			? playerIndex(pickedObject) 
    			: playerIndex(from.topChip());
	       	for(int direction = FIRST_FORWARD_DIRECTION[player],step=0; step<3; step++,direction++)
	    	{
	    	DipoleCell c = from;
	    	int n=0;
	    	while(c!=null) { c=c.exitTo(direction); n++; }
	    	mindis = Math.min(mindis,n);
	    	}
    	}
    	return(mindis);
    }
    public boolean legalToHitWaste()
    {	if(pickedSource!=null)
    	{
    	switch(board_state)
    	{
       	default: return(false);
    	case PUZZLE_STATE: 
    			return(true); 
    	case CONFIRM_STATE:
    	case PLAY_STATE:
    		return((pickedSource.chipIndex+1)>=distanceToEdge(pickedSource));
    		
    	}
    	}
    	return(false);
     }
    // 
    // drop the floating object.
    //
    final int rowscore(DipoleCell c,int pl)
    { 	
    	int v = (pl==FIRST_PLAYER_INDEX) ? (nrows-c.row+1) : (c.row);
    	return(v);	
    }
    private void dropObject(DipoleId dest, char col, int row,replayMode replay)
    {
       G.Assert((pickedObject!=null)&&(droppedDest==null),"ready to drop");
       switch (dest)
        {
        default: throw G.Error("Not expecting dest %s",dest);
        case Waste_Rect:
        	droppedDest = Waste_Cell;
        	droppedOriginalHeight = 0;		// the original height of the dropped stack
            for(int i=0;i<row;i++) 
            	{ Waste_Cell.addChip(pickedObject); 
            	  if(replay.animate)
            	  {
            		  animationStack.push(pickedSource);
            		  animationStack.push(Waste_Cell);
            	  }
            	}
            switch(board_state)
            {
            default:
            case PUZZLE_STATE: break;
            case PLAY_STATE:
            	int player = playerIndex(pickedObject);
            	int removed = row-1;
            	pickedSource.chipIndex -= removed;
            	chips_on_board[player] -= removed;
            	sum_of_rows[player] -= rowscore(pickedSource,player)*removed;
            	break;
            }
            break;
        case BoardLocation: // an already filled board slot.
        	droppedDest = getCell(col,row);
        	DipoleChip capChip = droppedDest.topChip();
        	prevLastDropped =droppedDest.lastDropped;
        	droppedDest.lastDropped = moveNumber;
            if(capChip!=pickedObject)
           		{	// capture the destination stack
            	int ndeleted = droppedDest.chipIndex;
          		Waste_Cell.chipIndex=0;
           		for(int i=0;i<ndeleted;i++) 
           		{ Waste_Cell.addChip(capChip); 
           		  animationStack.push(droppedDest);
           		  animationStack.push(Waste_Cell);
           		}
           		droppedOriginalHeight = 0;
           		int destPlayer = playerIndex(capChip);
           		chips_on_board[destPlayer] -= ndeleted;
           		sum_of_rows[destPlayer] -= rowscore(droppedDest,destPlayer)*ndeleted;
           		droppedDest.chipIndex=0;
          		}
           	else
           	{
            droppedOriginalHeight = droppedDest.chipIndex;
           	Waste_Cell.chipIndex = 0;
           	}
           	droppedDest.addChip(pickedObject);
           	int pl = playerIndex(pickedObject);
           	chips_on_board[pl]++;
           	sum_of_rows[pl] += rowscore(droppedDest,pl);
           	if(board_state==DipoleState.PLAY_STATE)
           	{
           	int distance = Math.max(Math.abs(droppedDest.col-pickedSource.col),Math.abs(droppedDest.row-pickedSource.row));
           	if(distance>0)
           	{
           	int moved = (distance-1);
           	pickedSource.chipIndex -= moved;
           	sum_of_rows[pl] -= rowscore(pickedSource,pl)*moved;
           	sum_of_rows[pl] += rowscore(droppedDest,pl)*moved;
           	while(moved-- > 0)
           	{	droppedDest.addChip(pickedObject);
           		if(replay.animate)
           			{animationStack.push(pickedSource);
           			animationStack.push(droppedDest);
           			}
           	}}
        	}
        	break;
        case Common_Pool:
        	droppedDest = rack[row];
        	break;
         }
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(DipoleCell cell)
    {	return(droppedDest==cell);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    {	DipoleChip ch = pickedObject;
    	if((ch!=null)&&(droppedDest==null))
    		{ return(ch.chipNumber);
    		}
        return (NothingMoving);
    }
     public DipoleCell getCell(DipoleCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
    private DipoleCell getCell(DipoleId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
            return(getCell(col,row));
        case Waste_Rect:
        	return(Waste_Cell);
        case Common_Pool:
            return(rack[row]);
         }	
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(DipoleId source, char col, int row)
    {	
    
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
         	{
            DipoleCell c = getCell(col,row);
            prevLastPicked = c.lastPicked;
            c.lastPicked = moveNumber;
         	if((c==pickedSource)&&(droppedDest==Waste_Cell)) 
         	{ int pl = playerIndex(pickedObject);
         	  c.removeTop();
         	  Waste_Cell.addChip(pickedObject);
         	  chips_on_board[pl]--;
         	  sum_of_rows[pl] -= rowscore(c,pl);
          	}
         	else
         	{
            G.Assert((pickedObject==null)&&(pickedSource==null),"ready to pick");
            pickedSource=c;
         	pickedObject = c.removeTop();
         	int pl = playerIndex(pickedObject);
        	chips_on_board[pl]--;
        	sum_of_rows[pl] -= rowscore(pickedSource,pl);
         	}
         	break;
         	}
        case Common_Pool:
            G.Assert((pickedObject==null)&&(pickedSource==null),"ready to pick");
            pickedSource = rack[row];
        	pickedObject = DipoleChip.getChip(row);
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
        	if(droppedDest!=null) { setNextStateAfterDone(); }
        	break;
        case PLAY_STATE:
			setState(DipoleState.CONFIRM_STATE);
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
        	setState(DipoleState.PLAY_STATE);
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
    	case PASS_STATE:
    	case CONFIRM_STATE:
    	case PUZZLE_STATE:
    	case PLAY_STATE:
    		setState(hasMoves()?DipoleState.PLAY_STATE:DipoleState.PASS_STATE);
    		break;
    	}

    }
   

    
    private void doDone()
    {	
        acceptPlacement();

        if (board_state==DipoleState.RESIGN_STATE)
        {	setGameOver(false,true);
        }
        else
        {	boolean win1 = WinForPlayerNow(whoseTurn);
        	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
        	if(win1 || win2)  { setGameOver(win1,win2); }
        	else
        	{setNextPlayer(); 
        	 setNextStateAfterDone(); 
        	}
        }
    }

    public boolean Execute(commonMove mm,replayMode replay)
    {	DipoleMovespec m = (DipoleMovespec)mm;
    	if(replay==replayMode.Replay) { animationStack.clear(); }
       // System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone();

            break;
        case MOVE_BOARD_WASTE:
        	switch(board_state)
        	{
        	default:throw G.Error("Not expecting robot in state %s",board_state);
        	case PLAY_STATE:
        		pickObject(DipoleId.BoardLocation,m.from_col,m.from_row);
        		dropObject(DipoleId.Waste_Rect,m.to_col,m.to_row,replay);
        		setNextStateAfterDrop();
        		break;
        	}
        	break;
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case PLAY_STATE:
        			G.Assert((pickedObject==null)&&(droppedDest==null),"something is moving");
        			pickObject(DipoleId.BoardLocation, m.from_col, m.from_row);
        			dropObject(DipoleId.BoardLocation,m.to_col,m.to_row,replay); 
        			m.captures = Waste_Cell.chipIndex;	// save for undo
 				    setNextStateAfterDrop();
        			break;
        	}
        	break;
        case MOVE_DROPB:
        	G.Assert(pickedObject!=null,"something is moving");
			switch(board_state)
			{ default: throw G.Error("Not expecting drop in state %s",board_state);
			  case PUZZLE_STATE:  break;
			  case CONFIRM_STATE: unDropObject(); unPickObject(); break;
			  case PLAY_STATE:
				  break;
			}
            dropObject(DipoleId.BoardLocation, m.to_col, m.to_row,replay);
            if(pickedSource==droppedDest) 
            	{ unDropObject();
            	  unPickObject(); 
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
        		  setState(DipoleState.PLAY_STATE);
        		}
         	else 
        		{ pickObject(DipoleId.BoardLocation, m.from_col, m.from_row);
        			// if you pick up a gobblet and expose a row of 4, you lose immediately
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
            unDropObject();
            unPickObject();
            setState(DipoleState.PUZZLE_STATE);
            {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;

       case MOVE_RESIGN:
            setState(unresign==null?DipoleState.RESIGN_STATE:unresign);
            break;
       case MOVE_EDIT:
    		acceptPlacement();
            setState(DipoleState.PUZZLE_STATE);
 
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
         case PASS_STATE:
         case PLAY_STATE: 
         case RESIGN_STATE:
        	return(false);


		case GAMEOVER_STATE:
			return(false);
        case PUZZLE_STATE:
        	return(true);
        }
    }
  
    // true if it's legal to drop gobblet  originating from fromCell on toCell
    public boolean LegalToDropOnBoard(DipoleCell fromCell,DipoleCell toCell)
    {	DipoleCell temp[] = getTempDest();
    	int n = getDestsFrom(fromCell,temp);
    	boolean ok = false;
    	for(int i=0;(i<n) && !ok;i++) { if(temp[i]==toCell) { ok = true; } }
    	returnTempDest(temp);
    	return(ok);
    }
    public boolean LegalToHitBoard(DipoleCell cell)
    {	
        switch (board_state)
        {
		case PLAY_STATE:
			if(pickedSource==null)
			{
				return(cell.topChip()==playerChip[whoseTurn]);
			}
			if(droppedDest==null)
				{ return((cell==pickedSource)||LegalToDropOnBoard(pickedSource,cell));
				}
			return(false);
		case PASS_STATE:
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
			if((pickedSource==cell)&&(droppedDest==Waste_Cell)&&(pickedSource.chipIndex>=0))
			  { return(true); 
			  }
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case PUZZLE_STATE:
        	return(cell.isDark && (pickedObject==null?(cell.chipIndex>0):true));
        }
    }
  public boolean canDropOn(DipoleCell cell)
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
    public void RobotExecute(DipoleMovespec m)
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
    public void UnExecute(DipoleMovespec m)
    {
       // System.out.println("U "+m+" for "+whoseTurn);

        switch (m.op)
        {
  	    default:
   	    	cantUnExecute(m);
        	break;
         case MOVE_DONE:
            break;
        case MOVE_BOARD_WASTE:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
        		case PASS_STATE:
        		case PLAY_STATE:
        			int who = m.player;
        			DipoleChip chip = playerChip[who];
        			DipoleCell dest = getCell(m.from_col,m.from_row);
        			int ntoadd = m.to_row;
        			for(int i=0;i<ntoadd;i++) { dest.addChip(chip); }
        			chips_on_board[who] += ntoadd;
        			sum_of_rows[who] += ntoadd*rowscore(dest,who);
        			break;
        	}
       	
        	break;
         case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
        		case PASS_STATE:
        		case PLAY_STATE:
        			{
        			int who = m.player;
        			int cap = m.captures;
        			DipoleCell dest = getCell(m.to_col,m.to_row);
        			DipoleCell src = getCell(m.from_col,m.from_row);
                  	int distance = Math.max(Math.abs(dest.col-src.col),Math.abs(dest.row-src.row));
                  	sum_of_rows[who] += (rowscore(src,who)-rowscore(dest,who))*distance;
                  	while(distance-- > 0) { src.addChip(dest.removeTop()); }
                  	if(cap>0) 
                  		{ int other = nextPlayer[who];
                  		  DipoleChip opp = playerChip[other];
                  		  chips_on_board[other]+= cap;
                  		  sum_of_rows[other] += cap*rowscore(dest,other);
                  		  while(cap-- > 0) { dest.addChip(opp); }
                  		}
                  	}	  
        			break;
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(m.state);
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }

 int getDestsFrom(DipoleCell c,DipoleCell temp[])
 {	int myPlayer=(c==pickedSource) ? playerIndex(pickedObject) : playerIndex(c.topChip());
 	DipoleChip nextChip = playerChip[nextPlayer[myPlayer]];
 	int lastHeight = c.chipIndex + ((c==pickedSource) ? 1 : 0); 
 	int n=0;
	for(int direction=(FIRST_FORWARD_DIRECTION[myPlayer]),step=0; step<8; step++,direction++)
	 {	DipoleCell dest = c.exitTo(direction);
	 	int span = 1;
		if((dest!=null) && ((step&1)!=0)) { dest=dest.exitTo(direction); span++;  }
		while((dest!=null) && (span<=lastHeight))
		{	DipoleChip dtop = dest.topChip();
			//forward directions must be empty or same color
			if((step<3) && (dtop!=nextChip))
			 { if(temp!=null) { temp[n]=dest; }
			   n++;
			 }
			else if((dtop==nextChip) && (dest.chipIndex<=span))
				{
				  if(temp!=null) { temp[n]=dest; }
				  n++;
				}
		dest = dest.exitTo(direction);
		span++;
		if((dest!=null) && ((step&1)!=0)) { dest=dest.exitTo(direction); span++; }
		}

		}
 	return(n);
 }
 boolean hasMoves()
 {	 // get a list of moves for the robot to consider
	for(int i=0;i<darkCells.length;i++)
	{	DipoleCell c = darkCells[i];
	 	if((c.chipIndex>0)&&(c.topChip()==playerChip[whoseTurn]))
	 	{
	  	int n = getDestsFrom(c,null);
	  	if(n>0) 
	  		{ return(true); 
	  		}
	  	if(distanceToEdge(c)<=c.chipIndex)
	  		{ return(true); 
	  		}
	 	}
	}
	return(false);
 }
 
 // get a hash of moves destinations, used by the viewer
 // to know where to draw dots.
 public Hashtable<DipoleCell,DipoleCell> getMoveDests()
 {	Hashtable<DipoleCell,DipoleCell> dd = new Hashtable<DipoleCell,DipoleCell>();
 	if(droppedDest!=null) 
 	{ dd.put(droppedDest,droppedDest); 
 	  dd.put(pickedSource,pickedSource);
 	}
 	else
 	if((pickedSource!=null)&&(pickedSource.onBoard))
 	{	DipoleCell[] temp = getTempDest();
 		int n = getDestsFrom(pickedSource,temp);
 		for(int i=0;i<n;i++) { dd.put(temp[i],temp[i]); }
 		returnTempDest(temp);
 		
 	}
	 return(dd);
 }
 
 // get a list of moves for the robot to consider
 public CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
 	DipoleCell temp[]=getTempDest();
 	for(int i=0;i<darkCells.length;i++)
 	{	DipoleCell c = darkCells[i];
 		if((c.chipIndex>0)&&(c.topChip()==playerChip[whoseTurn]))
 		{
 	 	int n = getDestsFrom(c,temp);
		for(int j=0;j<n;j++)
 		{	DipoleCell dest = temp[j];
 			all.addElement(new DipoleMovespec(c.col,c.row,dest.col,dest.row,whoseTurn));
 		}
 		if(c.topChip()==playerChip[whoseTurn])
 		{
 			for(int edge = distanceToEdge(c);edge<=c.chipIndex;edge++)
 			{	all.addElement(new DipoleMovespec(c.col,c.row,edge,whoseTurn));
 			}
 		}}
 	}
 	if(all.size()==0) 
 		{ all.push(new DipoleMovespec(RESIGN,whoseTurn));
 
 		}
 	returnTempDest(temp);
   	return(all);
 }
 
}
