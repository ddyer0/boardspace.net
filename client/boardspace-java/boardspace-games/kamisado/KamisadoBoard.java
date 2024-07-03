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
package kamisado;

import java.awt.Color;
import java.util.*;
import lib.*;
import lib.Random;
import online.game.*;

/**
 * CarnacBoard knows all about the game of Kamisado, which is played
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

class KamisadoBoard extends rectBoard<KamisadoCell> implements BoardProtocol,KamisadoConstants
{
	static final int DEFAULT_COLUMNS = 8;	// 8x6 board
	static final int DEFAULT_ROWS = 8;
    static final String[] KAMISADOGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

	private KamisadoState unresign;
	private KamisadoState board_state;
	public KamisadoState getState() {return(board_state); }
	public void setState(KamisadoState st) 
	{ 	unresign = (st==KamisadoState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public int boardColumns = DEFAULT_COLUMNS;	// size of the board
    public int boardRows = DEFAULT_ROWS;
    public void SetDrawState() { setState(KamisadoState.DRAW_STATE); }

    public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
    {  char ch = txt.charAt(0);
    	if(!Character.isDigit(ch) )
    	{ ypos +=cellsize/4; }
    	GC.Text(gc, true, xpos, ypos, -1, 0,clt, null, txt);
    }
    //
    // private variables
    //
    private static int playerDirections[][] = {{CELL_UP_LEFT,CELL_UP,CELL_UP_RIGHT},
    		{CELL_DOWN_RIGHT,CELL_DOWN,CELL_DOWN_LEFT}};
 	private static int destinationRow[] = { 8,1};
    private StateStack robotState = new StateStack();
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public KamisadoChip pickedObject = null;
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    public CellStack lastDestStack = new CellStack();
    public CellStack lastSourceStack = new CellStack();
    public CellStack animationStack = new CellStack();
    
	// factory method
	public KamisadoCell newcell(char c,int r)
	{	return(new KamisadoCell(c,r));
	}
    public KamisadoBoard(String init,long key) // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = KAMISADOGRIDSTYLE; //coordinates left and bottom
        doInit(init,key); // do the initialization 
     }


	public void sameboard(BoardProtocol f) { sameboard((KamisadoBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(KamisadoBoard from_b)
    {
    	super.sameboard(from_b);	// calls sameCell for each cell
    	
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(sameCells(lastDestStack,from_b.lastDestStack),"lastDest matches");
        G.Assert(sameCells(lastSourceStack,from_b.lastSourceStack),"lastSource matches");
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject doesn't match");
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
		v ^= Digest(r,lastDestStack);
		v ^= Digest(r,lastSourceStack);
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
        return (v);
    }
   public KamisadoBoard cloneBoard() 
	{ KamisadoBoard copy = new KamisadoBoard(gametype,randomKey);
	  copy.copyFrom(this); 
	  return(copy);
	}

   public void copyFrom(BoardProtocol b) { copyFrom((KamisadoBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(KamisadoBoard from_b)
    {	super.copyFrom(from_b);
        pickedObject = from_b.pickedObject;	
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(lastDestStack,from_b.lastDestStack);
        getCell(lastSourceStack,from_b.lastSourceStack);
        board_state = from_b.board_state;
        unresign = from_b.unresign;

		sameboard(from_b);
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {	randomKey = key;	// not used, but for reference in this demo game
    	Random r = new Random(67246765);
   
     	{
     	if(Kamisado_INIT.equalsIgnoreCase(gtype)) 
     		{ boardColumns=DEFAULT_COLUMNS; 
     		boardRows = DEFAULT_ROWS;
     		}
     	else { throw G.Error(WrongInitError,gtype); }
     	gametype = gtype;
     	}
	    setState(KamisadoState.PUZZLE_STATE);
	    initBoard(boardColumns,boardRows); //this sets up the board and cross links
	    
	    for(int i=0;i<boardColumns;i++)
	    {
	    	KamisadoCell c = getCell((char)('A'+i),1);
	    	c.addChip(KamisadoChip.getChip(0,boardColumns-i-1));
	    	KamisadoCell d = getCell((char)('A'+i),boardRows);
	    	d.addChip(KamisadoChip.getChip(1,i));
	    }
	    whoseTurn = FIRST_PLAYER_INDEX;
	    lastDestStack.clear();
	    lastSourceStack.clear();
		pickedSourceStack.clear();
		droppedDestStack.clear();
		pickedObject = null;
        allCells.setDigestChain(r);
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }
	
    public boolean legalToHitBoard()
    {
    	switch(board_state)
    	{
    	case PASS_STATE:
    	case GAMEOVER_STATE:	return(false);
    	default: return(true);
    	}
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
    	setState(KamisadoState.GAMEOVER_STATE);
    }
    
    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	if(board_state==KamisadoState.GAMEOVER_STATE) { return(win[player]); }
    	return(false);
    }
    // estimate the value of the board position.
    public double ScoreForPlayer(int player,boolean print)
    {  	
    	throw G.Error("not implemented");
    }


    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement()
    {	pickedObject = null;
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
    	KamisadoCell dr = droppedDestStack.pop();
 
		pickedObject = dr.removeTop(); 

    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	KamisadoChip po = pickedObject;
    	if(po!=null)
    	{
    		KamisadoCell ps = pickedSourceStack.pop();
    		ps.addChip(po);
    		pickedObject = null;
     	}
     }

    // 
    // drop the floating object.
    //
    private void dropObject(KamisadoCell c)
    {   G.Assert(pickedObject!=null,"pickedObject should not be null"); 	    		
		c.addChip(pickedObject);
		pickedObject = null;
       	droppedDestStack.push(c);
    }
    public boolean isSource(KamisadoCell c) { return(getSource()==c); } 
    public KamisadoCell getSource()
    {
    	return((pickedSourceStack.size()>0) ? pickedSourceStack.top() : null);
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(KamisadoCell cell)
    {	return(getDest()==cell);
    }
    
    public KamisadoCell getDest()
    {
    	return((droppedDestStack.size()>0) ? droppedDestStack.top() : null);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	KamisadoChip ch = pickedObject;
    	if(ch!=null)
    		{ return(ch.chipNumber());
    		}
        return (NothingMoving);
    }
   

    public KamisadoCell getCell(KamisadoCell c)
    {
    	return((c==null)?null:getCell(c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(KamisadoCell c)
    {	G.Assert(pickedObject==null,"pickedObject should be null");
    	pickedObject = c.removeTop(); 
    	pickedSourceStack.push(c);
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
        	setNextStateAfterDone(); 
        	break;
        case PLAY_STATE:
			setState(KamisadoState.CONFIRM_STATE);
			break;

        case PUZZLE_STATE:
			acceptPlacement();
            break;
        }
    }
    private boolean isDeadlock(int pl,int depth)
    {	// check for repetition.  A true check for a loop is a little complicated, 
    	// but since there are only 16 pieces, if we reach that depth without finding
    	// a legal move, we must be repeating ourselves
    	if(depth>16) 
    		{ return(true); 	// must be a deadlock
    		}
    	KamisadoCell next = findPassLocation(pl);
		int nextP = nextPlayer[pl];
		lastDestStack.push(next);
		boolean legal = hasLegalMoves(nextP);
		if(!legal)
			{ legal = !isDeadlock(nextP,depth+1); 
			}
		lastDestStack.pop();
		return(!legal);
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
    	case PASS_STATE:
    	case CONFIRM_STATE:
    		if(!hasLegalMoves(whoseTurn)) 
    			{ 
    			if(isDeadlock(whoseTurn,0)) { setGameOver(true,false);} 	// you lose if you causea deadlock}
    			else {	setState(KamisadoState.PASS_STATE); }
    			break;
    			}
			//$FALL-THROUGH$
		case PLAY_STATE:
    	case PUZZLE_STATE:
    		setState(KamisadoState.PLAY_STATE);
    		break;
    	}

    }
   

    
    private void doDone()
    {	KamisadoCell dest = getDest();
    	KamisadoCell src = getSource();
    	
        if(board_state==KamisadoState.PASS_STATE)
        {	G.Assert(dest==null,"should be null here");
        	dest = src = findPassLocation(whoseTurn);
        }

        acceptPlacement();

        if (board_state==KamisadoState.RESIGN_STATE)
        {	setGameOver(false,true);
        }
        else
        {	if(dest!=null)
        	{
        	lastDestStack.push(dest);
        	lastSourceStack.push(src);
        	KamisadoChip top = dest.topChip();
        	if(top!=null)
        		{	int movingPlayer = top.getPlayer();
        			if(dest.row==destinationRow[movingPlayer])
        			{
        			win[movingPlayer] = true;
        			setGameOver(whoseTurn==movingPlayer,whoseTurn==nextPlayer[movingPlayer]);
        			}
        		}
        	}
        	if(board_state!=KamisadoState.GAMEOVER_STATE) { setNextPlayer(); setNextStateAfterDone(); }
        }
    }

    public KColor colorToMove()
    {	KamisadoCell c = lastDestStack.top();
    	return((c==null)?null:c.getColor());
    }
    public KamisadoCell findPassLocation(int who)
    {
    	KColor dest = lastDestStack.top().getColor();	//get the piece he couldn't move
    	for(KamisadoCell c = allCells; c!=null; c=c.next)
    	{
    		KamisadoChip top = c.topChip();
    		if((top!=null)&&(top.getPlayer()==who)&&(top.getColor()==dest))
    		{
    		return(c);
    		}
    	}
    	throw G.Error("No passing move");
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	KamisadoMovespec m = (KamisadoMovespec)mm;

        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone();

            break;
        case MOVE_PASS:
        	doDone();
        	break;
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case PLAY_STATE:
        			G.Assert(pickedObject==null,"something is moving");
        			KamisadoCell src = getCell(m.from_col, m.from_row);
        			KamisadoCell dest = getCell(m.to_col,m.to_row); 
        			pickObject(src);
        			m.chip = pickedObject;
        			dropObject(dest); 
 				    setNextStateAfterDrop();
 				    if(replay.animate)
 				    {
 				    	animationStack.push(src);
 				    	animationStack.push(dest);
 				    }
        			break;
        	}
        	break;
        case MOVE_DROPB:
			{
			KamisadoCell c = getCell(m.to_col, m.to_row);
        	G.Assert(pickedObject!=null,"something is moving");
			
            if(isSource(c)) 
            	{ 
            	  unPickObject(); 

            	} 
            	else
            		{
            		dropObject(c);
            		if(replay==replayMode.Single)
 				    {	KamisadoCell src = getSource();
 				    	animationStack.push(src);
 				    	animationStack.push(c);
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
        		  setState(KamisadoState.PLAY_STATE);
        		}
        	else 
        		{ pickObject(getCell( m.from_col, m.from_row));
        		  m.chip = pickedObject;
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




        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(KamisadoState.PUZZLE_STATE);
            {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;

        case MOVE_RESIGN:
            setState(unresign==null?KamisadoState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            // standardize "gameover" is not true
            setState(KamisadoState.PUZZLE_STATE);
 
            break;

		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;
       default:
        	cantExecute(m);
        }


        return (true);
    }


  public boolean canDropOn(KamisadoCell cell)
  {		KamisadoCell top = (pickedObject!=null) ? pickedSourceStack.top() : null;
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
    public void RobotExecute(KamisadoMovespec m)
    {	robotState.push(board_state);
        // to undo state transistions is to simple put the original state back.
        
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        //G.print("r "+m);
        if (Execute(m,replayMode.Replay))
        {
            if ((m.op == MOVE_DONE)||(m.op==MOVE_PASS))
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
    public void UnExecute(KamisadoMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	// G.print("u "+m);
        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;

        case MOVE_PASS:
        	lastDestStack.pop();
        	lastSourceStack.pop();
        	break;
        case MOVE_DONE:
            break;

        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
        		case PLAY_STATE:
        		case PASS_STATE:
        			{
        			G.Assert(pickedObject==null,"something is moving");
        			KamisadoCell src = getCell( m.to_col, m.to_row);
        			KamisadoCell dest = getCell( m.from_col,m.from_row);
        			pickObject(src);
       			    dropObject(dest); 
       			    G.Assert(lastDestStack.pop()==src,"unmove");
       			    G.Assert(lastSourceStack.pop()==dest,"source matches");
       			    acceptPlacement();
        			}
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
 boolean getListOfMoves(CommonMoveStack  all,KamisadoCell from,int who)
 {
	 for(int direction : playerDirections[who])
	 {	
		for(KamisadoCell next = from.exitTo(direction); next!=null && next.topChip()==null; next = next.exitTo(direction))
			{
		 	if(all==null) { return(true); }
		 	all.push(new KamisadoMovespec(from.col,from.row,next.col,next.row,who));
			}
	 }
	 return((all==null)?false:all.size()>0);
 }
 public KColor getTargetColor()
 {
	 KamisadoCell c = lastDestStack.top();
	 if(c!=null) { return(c.getColor()); }
	 return(null);
 }
 KamisadoCell getNextSourceCell(int who)
 {
	 KColor targetColor = getTargetColor();
	 for(KamisadoCell c = allCells; c!=null; c=c.next)
	 	{
		 KamisadoChip top = c.topChip();
		 if((top!=null)&&(top.getPlayer()==who)&& (top.getColor()==targetColor)) { return(c); }
	 	}
	 throw G.Error("No source found");
 }
 boolean getListOfMoves(CommonMoveStack  all,int who)
 {	KColor targetColor = getTargetColor();
 	for(KamisadoCell c = allCells; c!=null; c=c.next)
 	{
	 KamisadoChip top = c.topChip();
	 if((top!=null)&&(top.getPlayer()==who))
			 {
		 	 if((targetColor==null) || (top.getColor()==targetColor))
		 	 	{
		 		boolean val = getListOfMoves(all,c,who);
		 		if(val && (all==null)) { return(true); }
		 	 	}
			 }
 	}
 	return((all==null)?false:all.size()>0);
 }

 public Hashtable <KamisadoCell,KamisadoCell> getSources()
 {	Hashtable <KamisadoCell,KamisadoCell> sources = new Hashtable<KamisadoCell,KamisadoCell>();
 	CommonMoveStack moves = new CommonMoveStack();
 	switch(board_state)
 	{
 	case CONFIRM_STATE:
 		{
 		KamisadoCell src = getDest();
 		if(src!=null) { sources.put(src,src); }
 		}
 		break;
 	case PASS_STATE:
 		{
 		KamisadoCell dest = getNextSourceCell(whoseTurn);
 		sources.put(dest,dest);
 		}
 		break;
 	case PUZZLE_STATE:
 		if(pickedObject==null)
 		{
 			for(KamisadoCell c = allCells; c!=null; c=c.next) { if(c.topChip()!=null) { sources.put(c,c); }}
 		}
 		break;
 	case PLAY_STATE:
 		if(pickedObject!=null) { getListOfMoves(moves,getSource(),whoseTurn); }
	else { getListOfMoves(moves,whoseTurn); }
		while(moves.size()>0)
 		{
 			KamisadoMovespec m = (KamisadoMovespec)moves.pop();
 			if(m.op==MOVE_BOARD_BOARD) 
 			{
 				KamisadoCell d = getCell(m.from_col,m.from_row);
 				sources.put(d,d);
 			}
 		}
		break;
	default:
		break;
	}
	return(sources);
 }
 public Hashtable<KamisadoCell,KamisadoCell> getDests()
 {	Hashtable <KamisadoCell,KamisadoCell> dests = new Hashtable<KamisadoCell,KamisadoCell>();
 	switch(board_state)
 	{
 	case PUZZLE_STATE:
 		for(KamisadoCell c = allCells; c!=null; c=c.next) { if(c.topChip()==null) { dests.put(c,c); }}
 		break;
 	default:
 	
 	if(pickedObject!=null)
 	{	KamisadoCell src = getSource();
 		CommonMoveStack moves = new CommonMoveStack();
 		getListOfMoves(moves,src,whoseTurn);
 		while(moves.size()>0)
 		{
 			KamisadoMovespec m = (KamisadoMovespec)moves.pop();
 			if(m.op==MOVE_BOARD_BOARD) 
 			{
 				KamisadoCell d = getCell(m.to_col,m.to_row);
 				dests.put(d,d);
 			}
 		}
 	}}
 	return(dests);
 }
 private boolean hasLegalMoves(int who)
 {
	 return(getListOfMoves(null,who));
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	getListOfMoves(all,whoseTurn);
 	if(all.size()==0) 
 		{ all.push(new KamisadoMovespec(MOVE_PASS,whoseTurn)); 
 		}
  	return(all);
 }
 
}
