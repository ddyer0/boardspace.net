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
package colorito;

import online.game.*;

import java.util.*;

import colorito.ColoritoChip.ChipColor;
import lib.*;
import lib.Random;
import static colorito.ColoritoMovespec.*;
/**
 * ColoritoBoard knows all about the game of Colorito.
 * It gets a lot of logistic support from game.rectBoard, 
 * which knows about the coordinate system.  
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
 *  restrictive about what to allow in each state, and have a lot of trip wires to
 *  catch unexpected transitions.   We expect to be fed only legal moves, but mistakes
 *  will be made and it's good to have the maximum opportunity to catch the unexpected.
 *  
 * Note that none of this class shows through to the game controller.  It's purely
 * a private entity used by the viewer and the robot.
 * 
 * special problems for Colorito
 * http://programmers.stackexchange.com/questions/237529/ai-development-variation-on-the-horizon-effect
 * the current strategy to deal with this to to prevent the top-level moves that would
 * lead to repetition, effectively forcing the bot to try something else.
 * 
 * @author ddyer
 *
 */

class ColoritoBoard extends rectBoard<ColoritoCell> implements BoardProtocol,ColoritoConstants
{	
    static final int White_Chip_Index = 0;
    static final int Black_Chip_Index = 1;
    static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	static final ColoritoId RackLocation[] = { ColoritoId.White_Chip_Pool,ColoritoId.Black_Chip_Pool};
	
	int dropState = 0;
	int prevLastPicked = -1;
	int prevLastDropped = -1;
	
	public ColoritoCell rack[] = null;						// holds the sample chips for the viewer
	
    public int boardColumns = Variation.Colorito_10.cols;	// size of the board
    public int boardRows = Variation.Colorito_10.rows;
    public void SetDrawState() { setState(ColoritoState.Draw); }
    public CellStack animationStack = new CellStack();
    public CellStack occupiedCells = new CellStack();
    public Hashtable<ColoritoChip,ColoritoCell> chipHomes = new Hashtable<ColoritoChip,ColoritoCell>();
    //
    // private variables
    //
	private RepeatedPositions repeatedPositions = null;
	private int passedMoves = 0;
    private int sweep_counter=0;
    private boolean firstMove[]=new boolean[2];
    private Variation variation = Variation.Colorito_10;
    private ColoritoState board_state = ColoritoState.Play;	// the current board state
    private ColoritoState unresign = null;					// remembers the previous state when "resign"
    private StateStack robotState = new StateStack();
    public int robotDepth = 0;
    public ColoritoState getState() { return(board_state); } 
	public void setState(ColoritoState st) 
	{ 	unresign = (st==ColoritoState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
    private ColoritoId playerColor[]={ColoritoId.White_Chip_Pool,ColoritoId.Black_Chip_Pool};
 	public ColoritoId getPlayerColor(int p) { return(playerColor[p]); }
	
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public ColoritoChip pickedObject = null;
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    
	// factory method
	public ColoritoCell newcell(char c,int r)
	{	return(new ColoritoCell(c,r));
	}
    public ColoritoBoard(String init,long rv,int np,RepeatedPositions pos,int map[]) // default constructor
    {   repeatedPositions = pos;
    	setColorMap(map, np);
        doInit(init,rv,np); // do the initialization 
        autoReverseY();		// reverse_y based on the color map
    }


	public void sameboard(BoardProtocol f) { sameboard((ColoritoBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if clone,digest and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(ColoritoBoard from_b)
    {
    	super.sameboard(from_b);	// calls sameCell for each cell, also for inherited class variables.
    	G.Assert(unresign==from_b.unresign,"unresign mismatch");
    	G.Assert(passedMoves==from_b.passedMoves,"passedMoves mismatch");
       	G.Assert(AR.sameArrayContents(win,from_b.win),"win array contents match");
       	G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor contents match");
       	G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject doesn't match");
        G.Assert(AR.sameArrayContents(firstMove,from_b.firstMove),"firstMove mismatch");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");

    }

    /** 
     * Digest produces a 64 bit hash of the game state.  This is used in many different
     * ways to identify "same" board states.  Some are germane to the ordinary operation
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

        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        Random r = new Random(64 * 1000); // init the random number generator
        long v = super.Digest(r);

		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,firstMove);
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
        return (v);
    }
   public ColoritoBoard cloneBoard() 
	{ ColoritoBoard copy = new ColoritoBoard(gametype,randomKey,players_in_game,null,getColorMap());
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((ColoritoBoard)b); }


    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(ColoritoBoard from_b)
    {	
        super.copyFrom(from_b);			// copies the standard game cells in allCells list
        pickedObject = from_b.pickedObject;	
        repeatedPositions = from_b.repeatedPositions==null ? null : from_b.repeatedPositions.copy();
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(occupiedCells,from_b.occupiedCells);
         AR.copy(playerColor,from_b.playerColor);
         AR.copy(firstMove,from_b.firstMove);
        passedMoves = from_b.passedMoves;
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        robotDepth = from_b.robotDepth;
        sameboard(from_b);
    }
    public void doInit(String gtype,long rv)
    {
    	doInit(gtype,rv,players_in_game);
    }
    private void addChip(ColoritoCell c,ColoritoChip ch)
    {	occupiedCells.push(c);
    	c.addChip(ch);
    }
    private ColoritoChip removeChip(ColoritoCell c)
    {	occupiedCells.remove(c, false);
    	return(c.removeTop());
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long rv,int np)
    {  	drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = GRIDSTYLE; //coordinates left and bottom
    	randomKey = rv;
    	dropState = 0;
    	prevLastDropped = -1;
    	prevLastPicked = -1;
    	players_in_game = np;
    	sweep_counter = 0;
    	passedMoves = 0;
    	robotState.clear();
    	robotDepth = 0;
		occupiedCells.clear();
		rack = new ColoritoCell[2];
    	Random r = new Random(67246765);
    	int map[] = getColorMap();
     	for(int i=0;i<2; i++)
    	{
       	ColoritoCell cell = new ColoritoCell(r);
       	cell.rackLocation=RackLocation[i];
       	// rack cells for display only
       	cell.addChip(ColoritoChip.getChip((1-i)*2));
      	cell.addChip(ColoritoChip.getChip((1-i)*2+1));
     	rack[map[i]]=cell;
     	}    
     	{
     	variation = Variation.findVariation(gtype);
     	switch(variation)
     	{	default: throw G.Error(WrongInitError,gtype);
     		case Colorito_10:
     		case Colorito_8:
     		case Colorito_7:
     		case Colorito_6:
     		case Colorito_6_10:
      		boardColumns = variation.cols; 
     		boardRows = variation.rows;
     		gametype = gtype;
     		reInitBoard(boardColumns,boardRows); //this sets up the board and cross links
     	}
     	
     	}
	    setState(ColoritoState.Puzzle);
	    
	    // fill the board with the background tiles
	    for(int row=1;row<=2;row++)
	    {	for(int col=0;col<boardColumns;col++)
	    	{ ColoritoCell c = getCell((char)('A'+col),row);
	    	  int number = col+1+(row-1)*boardColumns;
	    	  ColoritoChip cchip = ColoritoChip.getChip(row-1,number);
	    	  c.number = number;	 
	    	  c.addChip(ColoritoChip.getTile(4));
	    	  addChip(c,cchip);
	    	  
	    	  ColoritoCell d = getCell((char)('A'+boardColumns-col-1),boardRows-row+1);
	    	  ColoritoChip dchip = ColoritoChip.getChip((4-row),number);
	    	  d.number = number;
	    	  d.addChip(ColoritoChip.getTile(4));
	    	  addChip(d,dchip);
	    	  chipHomes.put(cchip,d);
	    	  chipHomes.put(dchip,c);
	    	}
	    }
	    for(int row=3;row<=boardRows-2;row++)
	    {	for(int col=0;col<boardColumns;col++)
	    	{ ColoritoCell c = getCell((char)('A'+col),row);
	    	  c.addChip(ColoritoChip.getTile(((row&1)*2+col)%4));
	    	  c.number = 0;
	    	}
	    }

	    
	    whoseTurn = FIRST_PLAYER_INDEX;
		playerColor[FIRST_PLAYER_INDEX]=ColoritoId.White_Chip_Pool;
		playerColor[SECOND_PLAYER_INDEX]=ColoritoId.Black_Chip_Pool;
		pickedSourceStack.clear();
		droppedDestStack.clear();
		AR.setValue(firstMove,true);
		pickedObject = null;
        AR.setValue(win,false);
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
        case Puzzle:
            break;
        case Confirm:
        case Draw:
        case Resign:
            moveNumber++; //the move is complete in these states
            firstMove[whoseTurn]=false;
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
        {case Resign:
         case Confirm:
         case Draw:
            return (true);

        default:
            return (false);
        }
    }


    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(ColoritoState.Gameover);
    }
    
    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	if(board_state==ColoritoState.Gameover) { return(win[player]); }
    	boolean mismatch = false;
    	boolean oppmatch = false;
    	boolean allOppmatch = !firstMove[player];
    	boolean match = false;
    	int []map = getColorMap();
    	for(int row = 1; row<=2; row++)
    	{	ColoritoCell c = getCell('A', (map[player]==0)?row:boardRows-row+1);
    		while(c!=null)
    		{	if(c.isEmpty()) 
    				{ return(false); }
    			ColoritoChip top = c.topChip();
    			
    			if(playerIndex(top)==player)
    			{	ColoritoCell home = chipHomes.get(top);
    				mismatch |= (c!=home);
    				match = true;
    				allOppmatch = false;
    				
    			}
    			else
    			{	oppmatch = true;
    			}
    			c = c.exitTo(CELL_RIGHT);
    		}
    	}
    	return( allOppmatch ? true : oppmatch ? match : !mismatch);
    }
    // estimate the value of the board position.
    public double ScoreForPlayer(int player,boolean print,double cup_weight,double ml_weight,boolean dumbot)
    {  	double finalv=passedMoves*0.5;				// make passes look slightly good
    	double ORPHAN_PENALTY = 900.0;			// ought to be less than the originrow penalty
    	double ORIGINROW_PENALTY = 1000.0;
    	String msg = "";
		for(int lim=occupiedCells.size()-1; lim>=0; lim--)
		{	ColoritoCell c = occupiedCells.elementAt(lim);
			ColoritoChip ch = c.topChip();
			boolean inOriginRow = (player==0) ? (c.row>=boardRows-1) : (c.row<=2);
			if(playerIndex(ch)==player)
			{	ColoritoCell home = chipHomes.get(ch);
				int xdis = (home.row-c.row);
				int ydis = (home.col-c.col);
				if((xdis==0)&&(ydis==0))
				{}
				else
				{
				boolean orphan = !hasLegalMoves(c,ch,player,true);
				finalv += xdis*xdis+ydis*ydis;
				if(orphan) 
					{ finalv += ORPHAN_PENALTY; 
					  if(print)
					  	{ msg += "Orphan="+c.col+c.row+" "+ORPHAN_PENALTY; 
					  	}
					}
				if(inOriginRow) { finalv += ORIGINROW_PENALTY; if(print) { msg+= "Origin="+c.col+c.row+" "+ORIGINROW_PENALTY;  }}
				}
			}
		}
		if(print && !"".equals(msg)) { G.print(msg); } 
	    return(-finalv);
    }


    //
    // finalize all the state changes for this move.
    //
    public void acceptPlacement()
    {	
        pickedObject = null;
        droppedDestStack.clear();
        pickedSourceStack.clear();
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    G.Assert(pickedObject==null, "nothing should be moving");
    if(droppedDestStack.size()>0)
    	{
    	ColoritoCell dr = droppedDestStack.pop();
    	switch(dr.rackLocation())
	    	{
	   		default: throw G.Error("Not expecting rackLocation %s",dr.rackLocation);
			case BoardLocation: 
				pickedObject = removeChip(dr); 
				dr.lastDropped = prevLastDropped;
				prevLastDropped = -1;
				break;
	    	
	    	}
	    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	ColoritoChip po = pickedObject;
    	if(po!=null)
    	{
    		ColoritoCell ps = pickedSourceStack.pop();
    		ps.lastPicked = prevLastPicked;
    		prevLastPicked = -1;
    		switch(ps.rackLocation())
    		{
    		default: throw G.Error("Not expecting rackLocation %s",ps.rackLocation);
    		case BoardLocation: addChip(ps,po); break;
    		case White_Chip_Pool:
    		case Black_Chip_Pool:	break;	// don't add back to the pool
    		}
    		pickedObject = null;
     	}
     }

    // 
    // drop the floating object.
    //
    private void dropObject(ColoritoCell c)
    {   G.Assert(pickedObject!=null,"pickedObject should not be null"); 	    		
    	switch(c.rackLocation())
		{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case BoardLocation: addChip(c,pickedObject); pickedObject = null; 
			prevLastDropped = c.lastDropped;
			c.lastDropped = dropState;
			dropState++;
			break;
		case White_Chip_Pool:
		case Black_Chip_Pool:	break;	// don't add back to the pool
		}
       	droppedDestStack.push(c);
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(ColoritoCell cell)
    {	return((droppedDestStack.size()>0) && (droppedDestStack.top()==cell));
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	ColoritoChip ch = pickedObject;
    	if(ch!=null)
    		{ return(1);
    		}
        return (NothingMoving);
    }
  
    public ColoritoCell getCell(ColoritoId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	return(getCell(col,row));
        }
    }
    public ColoritoCell getCell(ColoritoCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private ColoritoChip pickObject(ColoritoCell c)
    {	G.Assert(pickedObject==null,"pickedObject should be null");
    	switch(c.rackLocation())
    	{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case BoardLocation: 
			pickedObject = removeChip(c); 
			prevLastPicked = c.lastPicked;
			c.lastPicked = dropState;
			break;
    	
    	}
    	pickedSourceStack.push(c);
    	return(pickedObject);
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
        case Confirm:
        case Draw:
        	setNextStateAfterDone(); 
        	break;
        case Play:
			setState(ColoritoState.Confirm);
			break;

        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    //	
    //true if col,row is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(ColoritoCell c)
    {	return(getSource()==c);
    }
    public ColoritoCell getSource()
    {
    	return((pickedSourceStack.size()>0) ?pickedSourceStack.top() : null);
    }

    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case Gameover: 
    		break;

        case Draw:
        	setGameOver(false,false);
        	break;
    	case Confirm:
    	case Puzzle:
    	case Play:
    		setState(ColoritoState.Play);
    		break;
    	}

    }
   

    
    private void doDone()
    {	
        acceptPlacement();
        dropState++;
        if (board_state==ColoritoState.Resign)
        {	setGameOver(false,true);
        }
        else
        {	boolean win1 = WinForPlayerNow(whoseTurn);
        	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
        	if(win1 || win2)  { setGameOver(win1,win2); }
        	else {setNextPlayer(); setNextStateAfterDone(); }
        }
    }

    private void animatePath(ColoritoCell src,ColoritoCell dest)
    {
    	CellStack path = getMovePath(src,dest);
		if(path!=null)
		{
		ColoritoCell prev = path.pop();
		while(path.top()!=null)
		{	animationStack.push(prev); 
			prev.lastPicked = dropState;
			prev = path.pop();
			animationStack.push(prev);
			prev.lastDropped = dropState;
			dropState++;
		}}
    	
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	ColoritoMovespec m = (ColoritoMovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }
        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone();

            break;
        case MOVE_PASS:
        	// this is used by the robot to make a sort of trailing null move possible.
        	// when the next move in a search would have to be a "bad" move that starts
        	// a better sequence the current search won't see.
        	passedMoves++;
        	setState(ColoritoState.Confirm);
        	break;
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case Play:
        			G.Assert(pickedObject==null,"something is moving");
        			ColoritoCell src = getCell(ColoritoId.BoardLocation, m.from_col, m.from_row);
        			ColoritoCell dest = getCell(ColoritoId.BoardLocation,m.to_col,m.to_row);
        			pickObject(src);
        			m.chip = pickedObject;
        			dropObject(dest); 
        			if(replay!=replayMode.Replay)
        			{	animatePath(src,dest);
        			}
 				    setNextStateAfterDrop();
        			break;
        	}
        	break;
        case MOVE_DROPB:
			{
			ColoritoCell c = getCell(ColoritoId.BoardLocation, m.to_col, m.to_row);
        	G.Assert(pickedObject!=null,"something is moving");
			
            if(isSource(c)) 
            	{ 
            	  unPickObject(); 

            	} 
            	else
            		{
            		dropObject(c);
            		if(replay==replayMode.Single)
            			{
            			animatePath(getSource(),c);
            			}
            		setNextStateAfterDrop();
            		}
			}
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	if(isDest(getCell(m.from_col,m.from_row)))
        		{ unDropObject(); 
        		  setState(ColoritoState.Play);
        		}
        	else 
        		{
        		pickObject(getCell(ColoritoId.BoardLocation, m.from_col, m.from_row));
        		m.chip = pickedObject;
         		}
 
            break;

        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(ColoritoState.Puzzle);
            {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;

        case MOVE_RESIGN:
        	setState(unresign==null?ColoritoState.Resign:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            // standardize "gameover" is not true
            setState(ColoritoState.Puzzle);
 
            break;

		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;

		default:
        	cantExecute(m);
        }

 
        return (true);
    }


    public boolean LegalToHitBoard(ColoritoCell cell,Hashtable<ColoritoCell,ColoritoCell>sources,Hashtable<ColoritoCell,ColoritoCell>dests)
    {	
        switch (board_state)
        {
 		case Play:
			return(pickedObject==null ? (sources.get(cell)!=null) : (cell==getSource() || (dests.get(cell)!=null)));

		case Gameover:
		case Resign:
			return(false);
		case Confirm:
		case Draw:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case Puzzle:
        	return((pickedObject==null)!=cell.isEmpty());
        }
    }
  public boolean canDropOn(ColoritoCell cell)
  {		ColoritoCell top = (pickedObject!=null) ? pickedSourceStack.top() : null;
  		return((pickedObject!=null)				// something moving
  			&&(top.onBoard 			// on the main board
  					? (cell!=top)	// dropping on the board, must be to a different cell 
  					: (cell==top))	// dropping in the rack, must be to the same cell
  				);
  }
 
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(ColoritoMovespec m,boolean digest)
    {
        robotState.push(board_state); //record the starting state. The most reliable
        robotDepth++;
        // to undo state transistions is to simple put the original state back.
        // System.out.println("E "+m+" for "+whoseTurn);

        if (Execute(m,replayMode.Replay))
        {
            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {	long dig = digest ? Digest() : 0;
            	doDone();
            	if(digest)
            		{
            		if(repeatedPositions.numberOfRepeatedPositions(dig)>0)
            		{	// make this an artificial loss if it's a duplicate
            			// this effectively prevents the robot from playing
            			// infinitely repeated moves.
            		setGameOver(true,false);
            		}}
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
    public void UnExecute(ColoritoMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	robotDepth--;
        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;
   	    case MOVE_PASS:
   	    	passedMoves--;
   	    	break;
        case MOVE_DONE:
            break;
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default:throw G.Error("Not expecting robot in state %s",board_state);
        		case Gameover:
        		case Play:
        			G.Assert(pickedObject==null,"something is moving");
        			pickObject(getCell(ColoritoId.BoardLocation, m.to_col, m.to_row));
       			    dropObject(getCell(ColoritoId.BoardLocation, m.from_col,m.from_row)); 
       			    acceptPlacement();
        			break;
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(robotState.pop());
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
 public Hashtable<ColoritoCell,ColoritoCell> getDests()
 {	return(getDests(pickedSourceStack.top(),pickedObject));
 }
 public boolean hasLegalMoves(ColoritoCell c,ColoritoChip ch,int pl,boolean nonorigin)
 {	return(addMoves(null,c,ch,pl,nonorigin));
 }
 public Hashtable<ColoritoCell,ColoritoCell> getSources()
 {	Hashtable <ColoritoCell,ColoritoCell>sources = new Hashtable<ColoritoCell,ColoritoCell>();
 	if(pickedObject!=null)
 		{	ColoritoCell c = pickedSourceStack.top();
 			sources.put(c,c);
 		}
 		else
 			for(int lim=occupiedCells.size()-1; lim>=0; lim--)
 			{
 				ColoritoCell c = occupiedCells.elementAt(lim);
 				ColoritoChip top = c.topChip();
 				int pl = playerIndex(top);
 				if((pl==whoseTurn) || (board_state==ColoritoState.Puzzle))
 				{	if(addMoves(null,c,top,pl,false)) { sources.put(c,c); }
 				}
 			}
 	return(sources);
 }
 public Hashtable<ColoritoCell,ColoritoCell> getDests(ColoritoCell from,ColoritoChip ch)
 {	CommonMoveStack all = new CommonMoveStack();
 	Hashtable <ColoritoCell,ColoritoCell>dests = new Hashtable<ColoritoCell,ColoritoCell>();
 	if((from!=null) && (ch!=null))
 	{
 	addMoves(all,from,ch,playerIndex(ch),false);
 	while(all.size()>0)
 	{	ColoritoMovespec m = (ColoritoMovespec)all.pop();
 		ColoritoCell c = getCell(m.to_col,m.to_row);
 		dests.put(c,c);
 	}}
 	return(dests);
 }
 
 // return a movespec with the shortest path to "to"
 // path is the best path so far, we return null or a new path that's better than path which is still being built.
 private int pathDepth = 0;
 public CellStack getJumpMovePath(CellStack path,ColoritoCell jump,ColoritoCell to,int depth)
 {	CellStack newpath = null;
 	for(int dir = 0;dir<jump.geometry.n; dir++)
 	{	ColoritoCell j1 = jump.exitTo(dir);
 		if((j1!=null) && !j1.isEmpty())
 		{	ColoritoCell j2 = j1.exitTo(dir);
 			// take 2 steps in the same direction, jump over a filled cell to an empty cell
			if(j2==to)
			{	// the place to stop
				if((path==null) || (depth<pathDepth))
				{
				// if we're on a shorter path, replace path with a new one.
				newpath = new CellStack();
				newpath.push(to);
				pathDepth = depth;
				return(newpath);
				}
			}
			else if((j2!=null)
					&& j2.isEmpty() 
					&& ((j2.sweep_counter<sweep_counter)						// not seen on this sweep
							|| (j2.sweep_counter>(sweep_counter+depth)))			// seen on a longer sweep path
 						 )
 					{	// mark the sweep counter of this cell so we will only return
 						// if we're on a shorter path.
						j2.sweep_counter = sweep_counter+depth;
  						CellStack tpath = getJumpMovePath(path,j2,to,depth+1);
						if(tpath!=null) 
 						  	{ tpath.push(j2);
 						  	  newpath = tpath;
 						  	}
 					}
 			
 		}
 	}
	return(newpath);
 }
 //
 // get the shortest path from from to to
 //
 public CellStack getMovePath(ColoritoCell from,ColoritoCell to)
 {	
  	sweep_counter++;
 	for(ColoritoCell c = allCells; c!=null; c=c.next) { c.sweep_counter = 0; }
 	from.sweep_counter = sweep_counter;
	// add slide moves
 	for(int dir = 0; dir<from.geometry.n; dir++)
 	{	ColoritoCell dest = from.exitTo(dir);
 		if(to==dest) 
 			{	if(dest.sweep_counter!=sweep_counter)
 				{	dest.sweep_counter = sweep_counter;
  					CellStack path = new CellStack();
 					path.push(to);
 					path.push(from);
 					return(path);
 				}
 			}
 	}
 	// direct jump
 	CellStack best = getJumpMovePath(null,from,to,0);
 	CellStack direct = best;
 	// slide and jump
 	for(int dir = 0; dir<from.geometry.n; dir++)
 	{	ColoritoCell dest = from.exitTo(dir);
 		if((dest!=null) && dest.isEmpty())
 		{	if(to!=dest)
 			{
  			// slide and jump
			CellStack newbest = getJumpMovePath(best,dest,to,1);
			if((newbest!=null) && (newbest!=direct)) { newbest.push(dest); best = newbest; }
  			}
 		}
 	}
	if(best!=null)
		{	
		
		best.push(from);
		}
	return(best);
 }
 
 // return true if there are any jump moves starting from "jump"
 public boolean addJumpMoves(CommonMoveStack all,ColoritoCell from,ColoritoCell jump,ColoritoChip top,int who,boolean nonorigin)
 {	boolean some = false;
 
 	for(int dir = 0;dir<from.geometry.n; dir++)
 	{	ColoritoCell j1 = jump.exitTo(dir);
 		if((j1!=null) && !j1.isEmpty())
 		{	ColoritoCell j2 = j1.exitTo(dir);
 			if((j2!=null)
 					&& j2.isEmpty() 
 					&& (j2.sweep_counter != sweep_counter)
 					&& (!nonorigin || ((who==0) ? (j2.row<boardRows-1) : (j2.row>2) )))
 				
 					{	ChipColor jumpColor = j2.chipAtIndex(0).color;
 						j2.sweep_counter = sweep_counter;
 						
 						if((jumpColor==ChipColor.neutral) || (jumpColor==top.color))
 						{	// a place to stop
 							some = true;
 							if(all!=null)
 							{
 							all.push(new ColoritoMovespec(ColoritoMovespec.MOVE_BOARD_BOARD,from.col,from.row,j2.col,j2.row,who));
 							}
 							else { return(some); }
 						}
 						some |= addJumpMoves(all,from,j2,top,who,nonorigin);
 						if(some && (all==null)) { return(some); }
 					}
 			
 		}
 	}
	 return(some);
 }
 public boolean addMoves(CommonMoveStack all,ColoritoCell from,ColoritoChip top,int who,boolean nonorigin)
 {	boolean some = false;
 	ChipColor topColor = top.color;
 	sweep_counter++;
 	from.sweep_counter = sweep_counter;
	// add slide moves
 	for(int dir = 0; dir<from.geometry.n; dir++)
 	{	ColoritoCell to = from.exitTo(dir);
 		if((to!=null) && to.isEmpty())
 		{	ChipColor toColor = to.chipAtIndex(0).color;
  			if( ((toColor==ChipColor.neutral)||(toColor==topColor))
  					&& (!nonorigin || ((who==0) ? (to.row<boardRows-1) : (to.row>2) ))) 
 			{	if(all==null) { return(true); }
 				if(to.sweep_counter!=sweep_counter)
 				{	to.sweep_counter = sweep_counter;
 					some = true;
 					all.push(new ColoritoMovespec(ColoritoMovespec.MOVE_BOARD_BOARD,from.col,from.row,to.col,to.row,who));
 				}
 			}
  			// slide and jump
			some |= addJumpMoves(all,from,to,top,who,nonorigin);
 			if(some && (all==null)) { return(some); }

 		}
 	}
	some |= addJumpMoves(all,from,from,top,who,nonorigin);

	return(some);
 }
 private int playerIndex(ColoritoChip ch) { return(getColorMap()[ch.colorIndex]); }
 
 public boolean addMoves(CommonMoveStack all,int who)
 {	boolean some = false;
	for(int lim = occupiedCells.size()-1; lim>=0; lim--)
	{	ColoritoCell c = occupiedCells.elementAt(lim);
		ColoritoChip ch = c.topChip();
		if(playerIndex(ch)==who)
		{	some |= addMoves(all,c,ch,who,false);
			if(some && (all==null)) { return(some); }
		}
	}		
 	return(some);
 }
 
 CommonMoveStack  GetListOfMoves(boolean includepass)
 {	CommonMoveStack all = new CommonMoveStack();
 	switch(board_state)
 	{
 	default: throw G.Error("Not expecting %s",board_state);
 	case Play:
  		addMoves(all,whoseTurn);
  		if(includepass) { all.push(new ColoritoMovespec(MOVE_PASS,whoseTurn)); }
 		break;
 	}
  	return(all);
 }
 
}
