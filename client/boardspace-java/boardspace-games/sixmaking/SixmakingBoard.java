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
package sixmaking;

import java.awt.Color;
/* below here should be the same for codename1 and standard java */
import java.util.Hashtable;
import online.game.*;
import lib.*;
import static sixmaking.SixmakingMovespec.*;
/**
 * SixmakingBoard knows all about the game of Sixmaking.
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
 * @author ddyer
 *
 */

class SixmakingBoard extends rectBoard<SixmakingCell> implements BoardProtocol,SixmakingConstants
{	
    public int boardColumns;	// size of the board
    public int boardRows;
    public void SetDrawState() { setState(SixmakingState.Draw); }
    
    public SixmakingCell rack[] = null;	// the pool of chips for each player.  
    public CellStack animationStack = new CellStack();
    //
    // private variables
    //
    private RepeatedPositions repeatedPositions = null;			// shared with the viewer
    private SixmakingState board_state = SixmakingState.Play;	// the current board state
    private SixmakingState unresign = null;						// remembers the previous state when "resign"
    Variation variation = Variation.Sixmaking;
    public SixmakingState getState() { return(board_state); } 
	public void setState(SixmakingState st) 
	{ 	unresign = (st==SixmakingState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public SixmakingCell pickedStack = null;
    private IStack pickedHeight = new IStack();		// the height that was picked, ie 1=single 2=king
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    private StateStack dropState = new StateStack();
    public SixmakingCell lastDest[] = {null,null};				// last spot the opponent dropped, for the UI
    public SixmakingCell lastSource[] = {null,null};			// last spor the opponent picked, for the UI
    int lastHeight[] = {0,0};								// last movement height
    
    public SixmakingCell currentDest = null;
    int lastProgressMove = 0;		// last move where a pawn was advanced
    int lastDrawMove = 0;			// last move where a draw was offered
    int robotDepth = 0;		// current depth of robot search.  This is used to make faster wins look better
    						// than slower wins.  It's part of the board so multiple threads have independent values.
  	private StateStack robotState = new StateStack();
  	private CellStack robotCell = new CellStack();
  	private IStack robotHeight=new IStack();
  	private CellStack robotLast = new CellStack();
   	
	// factory method
	public SixmakingCell newcell(char c,int r)
	{	return(new SixmakingCell(c,r));
	}
    public SixmakingBoard(String init,long rv,int np,RepeatedPositions rep,int map[]) // default constructor
    {   repeatedPositions = rep;
    	setColorMap(map, np);
        doInit(init,rv,np); // do the initialization 
     }


	public void sameboard(BoardProtocol f) { sameboard((SixmakingBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if clone,digest and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(SixmakingBoard from_b)
    {
    	super.sameboard(from_b);	// calls sameCell for each cell, also for inherited class variables.
    	G.Assert(unresign==from_b.unresign,"unresign mismatch");
       	G.Assert(AR.sameArrayContents(win,from_b.win),"win array contents match");
       	G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
       	G.Assert(sameContents(pickedHeight,from_b.pickedHeight),"pickedHeight mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(dropState.sameContents(from_b.dropState),"dropState mismatch");
        G.Assert(sameCells(pickedStack,from_b.pickedStack),"pickedStack doesn't match");
        G.Assert(sameCells(lastDest,from_b.lastDest),"lastDest mismatch");
        G.Assert(sameCells(currentDest,from_b.currentDest),"currentDest mismatch");

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

		v ^= Digest(r,pickedStack);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,rack);
		v ^= Digest(r,pickedHeight);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,currentDest);
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
        return (v);
    }
   public SixmakingBoard cloneBoard() 
	{ SixmakingBoard copy = new SixmakingBoard(gametype,randomKey,players_in_game,repeatedPositions,getColorMap());
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((SixmakingBoard)b); }


    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(SixmakingBoard from_b)
    {	
        super.copyFrom(from_b);			// copies the standard game cells in allCells list
        pickedStack.copyFrom(from_b.pickedStack);	
        copyFrom(rack,from_b.rack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        pickedHeight.copyFrom(from_b.pickedHeight);
        getCell(droppedDestStack,from_b.droppedDestStack);
        dropState.copyFrom(from_b.dropState);
        board_state = from_b.board_state;
        lastProgressMove = from_b.lastProgressMove;
        lastDrawMove = from_b.lastDrawMove;
        getCell(lastDest,from_b.lastDest);
        getCell(lastSource,from_b.lastSource);
        AR.copy(lastHeight,from_b.lastHeight);
        
        currentDest = getCell(from_b.currentDest);
        unresign = from_b.unresign;
        repeatedPositions = from_b.repeatedPositions;
        sameboard(from_b);
    }
    public void doInit(String gtype,long rv)
    {
    	doInit(gtype,rv,players_in_game);
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long rv,int np)
    {  	drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = GRIDSTYLE; //coordinates left and bottom
    	randomKey = rv;
    	players_in_game = np;
 		rack = new SixmakingCell[2];
    	Random r = new Random(67246765);
       	pickedStack = new SixmakingCell(r);
       	int map[]=getColorMap();
     	for(int i=0,pl=FIRST_PLAYER_INDEX;i<2; i++,pl=nextPlayer[pl])
    	{
       	SixmakingCell cell = new SixmakingCell(r);
       	cell.rackLocation=RackLocation[i];
     	rack[map[i]]=cell;
     	}    
     	
     	variation = Variation.findVariation(gtype);
     	switch(variation)
     	{
     	default:  throw G.Error(WrongInitError,gtype);
     	case Sixmaking:
     	case Sixmaking_4:
     		boardColumns = variation.size;
     		boardRows = variation.size;
     		initBoard(boardColumns,boardRows);
     		gametype = gtype;
     		for(int lim = variation.startingChips-1; lim>=0; lim--)
     		{	rack[map[FIRST_PLAYER_INDEX]].addChip(SixmakingChip.white);
     			rack[map[SECOND_PLAYER_INDEX]].addChip(SixmakingChip.black);
     		}
     		break;
     	}

        allCells.setDigestChain(r);
	    setState(SixmakingState.Puzzle);
	    
	    lastProgressMove = 0;
	    lastDrawMove = 0;
	    robotDepth = 0;
	    currentDest = null;
	    AR.setValue(lastDest,null);
	    AR.setValue(lastSource,null);
	    robotState.clear();
	    robotLast.clear();
	    robotHeight.clear();
	    robotCell.clear();
	    whoseTurn = FIRST_PLAYER_INDEX;
		acceptPlacement();
        AR.setValue(win,false);
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }

    public double simpleScore(int who)
    {	// range is 0.0 to 0.8
    	return(0);
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
        case AcceptPending:
        case DeclinePending:
        case DrawPending:
        case Resign:
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
        {case Resign:
         case Confirm:
         case Draw:
         case DrawPending:
         case AcceptPending:
         case DeclinePending:
            return (true);

        default:
            return (false);
        }
    }
    
    //
    // this is used by the UI to decide when to display the OFFERDRAW box
    //
    public boolean drawIsLikely()
    {	switch(board_state)
    	{
    	case DrawPending: return true;
    	case AcceptOrDecline:
    	case Play:
    		return((moveNumber - lastProgressMove)>15);
       	default: return(false);
    	}
    	
    }
    //
    // declare the game over, and the winner and loser
    //
    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(SixmakingState.Gameover);
    }
    public boolean gameOverNow() { return(board_state.GameOver()); }
    public boolean winForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	// we maintain the wins in doDone so no logic is needed here.
    	if(board_state.GameOver()) { return(win[player]); }
    	else {
    		for(SixmakingCell c = allCells; c!=null; c=c.next)
    		{	if(c.height()>=6)
    			{	return(c.topChip()==PlayerChip[player]);
    			}
    		}
    	}
    	return(false);
    }
    // estimate the value of the board position.
    public double ScoreForPlayer(int player,boolean print)
    {  	double finalv=simpleScore(player);
    	
    	return(finalv);
    }


    //
    // finalize all the state changes for this move.
    //
    public void acceptPlacement()
    {	
        pickedStack.reInit();
        droppedDestStack.clear();
        dropState.clear();
        pickedSourceStack.clear();
        pickedHeight.clear();
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    G.Assert(pickedStack.isEmpty(), "nothing should be moving");
    if(droppedDestStack.size()>0)
    	{
    	SixmakingCell dr = droppedDestStack.pop();
    	setState(dropState.pop());
    	currentDest=null;
    	switch(dr.rackLocation())
	    	{
	   		default: throw G.Error("Not expecting rackLocation %s",dr.rackLocation);
			case White_Chip_Pool:	// treat the pools as infinite sources and sinks
			case Black_Chip_Pool:	
			case BoardLocation: 
				moveStack(dr,pickedStack,pickedHeight.top());
				break;
	    	
	    	}
	    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	
    	if(!pickedStack.isEmpty())
    	{
    		SixmakingCell ps = pickedSourceStack.pop();
    		int h = pickedHeight.pop();
    		moveStack(pickedStack,ps,h);

     	}
     }

    // 
    // drop the floating object.
    //
    private void dropObject(SixmakingCell c)
    {   G.Assert(!pickedStack.isEmpty(),"pickedStack should not be null"); 	    		
    	switch(c.rackLocation())
		{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case White_Chip_Pool:
		case Black_Chip_Pool:
		case BoardLocation:
			moveStack(pickedStack,c,pickedStack.height());
			break;
		}
    	dropState.push(board_state);
       	droppedDestStack.push(c);
       	currentDest=c;
    }
    
   private void moveStack(SixmakingCell from,SixmakingCell to,int height)
   {	if(height>0)
   		{	SixmakingChip top = from.removeTop();
   			moveStack(from,to,height-1);
   			to.addChip(top);
   		}
   }
   
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(SixmakingCell c,int height)
    {	G.Assert(pickedStack.isEmpty(),"pickedObject should be null");
    	G.Assert(c.height()>=height,"should have a chip");
    	switch(c.rackLocation())
    	{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case White_Chip_Pool:
		case Black_Chip_Pool:	
		case BoardLocation: 
			moveStack(c,pickedStack,height);
			pickedHeight.push(height);
			break;
	  	
    	}
    	pickedSourceStack.push(c);
   }

    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(SixmakingCell cell)
    {	return(droppedDestStack.top()==cell);
    }
    //
    // get the last dropped dest cell
    //
    public SixmakingCell getDest() 
    { return(droppedDestStack.top()); 
    }
    
    public SixmakingCell getPrevDest()
    {
    	return(lastDest[nextPlayer[whoseTurn]]);
    }
    public SixmakingCell getPrevSource()
    {
    	return(lastSource[nextPlayer[whoseTurn]]);
    }
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  Returns +100 if a king is the moving object.
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	int h =  pickedStack.height();
    	return((h>0)?h :NothingMoving);
    }

    // get a cell from a partucular source
    public SixmakingCell getCell(SixmakingId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	return(getCell(col,row));
        case White_Chip_Pool:
       		return(rack[getColorMap()[White_Chip_Index]]);
        case Black_Chip_Pool:
       		return(rack[getColorMap()[Black_Chip_Index]]);
        }
    }
    //
    // get the local cell which is the same role as c, which might be on
    // another instance of the board
    public SixmakingCell getCell(SixmakingCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
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
 			setState(SixmakingState.Confirm);
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
    public boolean isSource(SixmakingCell c)
    {	return(getSource()==c);
    }
    public SixmakingCell getSource()
    {
    	return((pickedSourceStack.size()>0) ?pickedSourceStack.top() : null);
    }
    
    //
    // we don't need any special state changes for picks.
    //
    private void setNextStateAfterPick()
    {
    }
    
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case Gameover: 
    		break;
        case DrawPending:
        	lastDrawMove = moveNumber;
        	setState(SixmakingState.AcceptOrDecline);
        	break;
    	case AcceptPending:
        case Draw:
        	setGameOver(false,false);
        	break;
    	case Confirm:
       	case DeclinePending:
       	case Puzzle:
    	case Play:
    		setState(SixmakingState.Play);
    		if(!hasLegalMoves()) { setGameOver(false,true); } // no moves, we lose
     		break;
    	}

    }

    private void doDone(replayMode replay)
    {	SixmakingCell dest = currentDest;
    	SixmakingCell src = pickedSourceStack.top();
    	if(src!=null)
    	{	if ((src.onBoard && src.topChip()==null)
    			|| !src.onBoard) 
    		{ lastProgressMove = moveNumber; 	// progress if you move a whole stack or onboard a new piece
    		}
    	}
    	// these are used by the UI and also by Ko move detection
    	lastDest[whoseTurn] = dest;
    	lastSource[whoseTurn]=src;
    	lastHeight[whoseTurn]=pickedHeight.size()>0 ? pickedHeight.top() : 0;
    	
    	currentDest = null;
      	acceptPlacement();
      	if (board_state==SixmakingState.Resign)
        {	setGameOver(false,true);
        }
        else
        {	
         	boolean win1 = winForPlayerNow(whoseTurn);
        	boolean win2 = winForPlayerNow(nextPlayer[whoseTurn]);
        	if(win1 || win2) { setGameOver(win1,win2); }
        	else 
        	{
        	setNextPlayer(); 
        	setNextStateAfterDone(); 
        	}
        }
    }


    public boolean Execute(commonMove mm,replayMode replay)
    {	SixmakingMovespec m = (SixmakingMovespec)mm;
        if(replay.animate) { animationStack.clear(); }
        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone(replay);

            break;
            
        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case Play:
        			G.Assert(pickedStack.isEmpty(),"something is moving");
        			SixmakingCell src = getCell(m.source, m.from_col, m.from_row);
        			SixmakingCell dest = getCell(m.dest,m.to_col,m.to_row);
        			m.from_full_height = src.onBoard ? src.height() : 0;
        			m.from_chip = src.topChip();
        			int moved = m.height;
        			m.to_full_height = dest.height()+moved;
        			pickObject(src,m.height);
        			dropObject(dest); 
        			if(replay.animate)
        			{	for(int i=0;i<moved;i++)
        				{
        				animationStack.push(src);
        				animationStack.push(dest);
        				}
        			}
 				    setNextStateAfterDrop();
        			break;
        	}
        	break;
        case MOVE_DROPB:
			{
			SixmakingCell c = getCell(SixmakingId.BoardLocation, m.to_col, m.to_row);
        	G.Assert(!pickedStack.isEmpty(),"something is moving");
        	int moved = pickedHeight.top();
			m.to_full_height = c.height()+moved;
			m.from_chip = pickedStack.topChip();
			if(isSource(c)) 
            	{ 
            	  unPickObject(); 

            	} 
            	else
            		{
            		dropObject(c);
            		setNextStateAfterDrop();
            		if(replay==replayMode.Single)
            			{
            			SixmakingCell src = getSource();
            			if(src!=null)
            			{// in puzzle mode, there may be no source
            			for(int i=0;i<moved;i++)
            			{
            			animationStack.push(src);
            			animationStack.push(c);
            			}}}
            		}
			}
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	SixmakingCell src = getCell(m.from_col,m.from_row);
        	m.from_full_height = src.height();
        	m.from_chip = src.topChip();
        	if(isDest(src))
        		{ unDropObject(); 
        		}
        	else 
        		{ pickObject(src,m.height);
        		  switch(board_state)
        		  {	default: throw G.Error("Not expecting pickb in state %s",board_state);
        		  	case Play:
         		  	case Puzzle:
        		  		break;
        		  }
         		}
 
            break;

        case MOVE_DROP: // drop on chip pool;
        	{
        	SixmakingCell c = getCell(m.source, m.to_col, m.to_row);
            dropObject(c);
            m.to_full_height = 0;
            m.from_chip = pickedStack.topChip();
            setNextStateAfterDrop();
            SixmakingCell sc = getSource();
            if(sc!=null)
            {
            if(replay==replayMode.Single)
			{
			animationStack.push(sc);
			animationStack.push(c);
			}}}
            break;



        case MOVE_PICK:
        	{
        	SixmakingCell c = getCell(m.source, m.from_col, m.from_row);
        	m.from_full_height = 0;
        	m.from_chip = c.topChip();
            pickObject(c,m.height);
            setNextStateAfterPick();
        	}
            break;


        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(SixmakingState.Puzzle);
            {	boolean win1 = winForPlayerNow(whoseTurn);
            	boolean win2 = winForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;
        case MOVE_OFFER_DRAW:
        	currentDest=null;
        	if(board_state==SixmakingState.DrawPending) { setState(dropState.pop()); }
        	else { dropState.push(board_state);
        			setState(SixmakingState.DrawPending);
        		}
        	break;
        case MOVE_ACCEPT_DRAW:
           	currentDest=null;
           	switch(board_state)
        	{	
        	case AcceptPending: 	// cancel accept and revert to neutral
        		setState(SixmakingState.AcceptOrDecline); 
        		break;
           	case AcceptOrDecline:
           	case DeclinePending:	// accept pending
           		setState(SixmakingState.AcceptPending); 
           		break;
        	default: throw G.Error("Not expecting %s",board_state);
        	}
           	break;
        case MOVE_DECLINE_DRAW:
           	currentDest=null;
        	switch(board_state)
        	{	
        	case DeclinePending:	// cancel decline and revert to neutral
        		setState(SixmakingState.AcceptOrDecline); 
        		break;
        	case AcceptOrDecline:
        	case AcceptPending: setState(SixmakingState.DeclinePending); break;
        	default: throw G.Error("Not expecting %s",board_state);
        	}
        	break;
        case MOVE_RESIGN:
           	currentDest=null;
        	setState(unresign==null?SixmakingState.Resign:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            // standardize "gameover" is not true
            setState(SixmakingState.Puzzle);
 
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
        	throw G.Error("Not expecting state %s", board_state);
        case Play: 
        	return(pickedStack.isEmpty() 
        			? (!rack[player].isEmpty() && (player==whoseTurn))
        			: !getSource().onBoard && (player==whoseTurn));
        case Confirm:
        case Draw:
		case Gameover:
		case Resign:
		case AcceptOrDecline:
		case DeclinePending:
		case DrawPending:
		case AcceptPending:
			return(false);
        case Puzzle:
        	return((pickedStack.isEmpty())?true:true);
        }
    }
  

    public boolean legalToHitBoard(SixmakingCell cell,Hashtable<SixmakingCell,SixmakingMovespec>targets)
    {	
        switch (board_state)
        {
 		case Play:
 			return(isSource(cell)||targets.get(cell)!=null);
 		case Resign:
		case Gameover:
		case AcceptOrDecline:
		case DeclinePending:
		case AcceptPending:
		case DrawPending:
			return(false);
		case Confirm:
		case Draw:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case Puzzle:
        	return(pickedStack.isEmpty()?!cell.isEmpty():true);
        }
    }
  public boolean canDropOn(SixmakingCell cell)
  {		SixmakingCell top = !pickedStack.isEmpty() ? pickedSourceStack.top() : null;
  		return(!pickedStack.isEmpty()				// something moving
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
    public void RobotExecute(SixmakingMovespec m)
    {
        //G.print("R "+m);
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        robotState.push(board_state);
        robotLast.push(currentDest);
        robotCell.push(lastSource[whoseTurn]);
        robotCell.push(lastDest[whoseTurn]);
        robotHeight.push(lastHeight[whoseTurn]);
        robotDepth++;
        if (Execute(m,replayMode.Replay))
        {	
            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {	if((robotDepth<=6) && (repeatedPositions.numberOfRepeatedPositions(Digest())>1))
            		{
            		// this check makes game end by repetition explicitly visible to the robot
            		setGameOver(false,false);
            		}
            else { doDone(replayMode.Replay); }
            }
        }
    }
 

   //
   // un-execute a move.  The move should only be un-executed
   // in proper sequence, and if it was executed by the robot in the first place.
   // If you use monte carlo bots with the "blitz" option this will never be called.
   //
    public void UnExecute(SixmakingMovespec m)
    {
        //G.print("U "+m+" for "+whoseTurn);
    	robotDepth--;
        setState(robotState.pop());
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
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
         		case Play:
         			{
        			G.Assert(pickedStack.isEmpty(),"something is moving");
        			pickObject(getCell(m.dest, m.to_col, m.to_row),m.height);
        			SixmakingCell from = getCell(m.source, m.from_col,m.from_row);
       			    dropObject(from); 
       			    acceptPlacement();
         			}
        			break;
        	}
        	break;

        case MOVE_RESIGN:
        case MOVE_ACCEPT_DRAW:
        case MOVE_DECLINE_DRAW:
        case MOVE_OFFER_DRAW:
            break;
        }
        currentDest = robotLast.pop();
        lastHeight[whoseTurn] = robotHeight.pop();
        lastDest[whoseTurn] = robotCell.pop();
        lastSource[whoseTurn] = robotCell.pop();

        
  }

private void loadHash(CommonMoveStack all,Hashtable<SixmakingCell,SixmakingMovespec>hash,boolean from)
{	
	for(int lim=all.size()-1; lim>=0; lim--)
	{
		SixmakingMovespec m = (SixmakingMovespec)all.elementAt(lim);
		switch(m.op)
		{
		default: break;
		case MOVE_BOARD_BOARD:
			if(from) 
				{ int h = 1<<m.height;
				  SixmakingCell cell = getCell(m.source,m.from_col,m.from_row);
				  SixmakingMovespec prev = hash.get(cell);
				  if(prev!=null) { h |= prev.height; }
				  // make the move height a mask of available heights
				  m.height = h;
				  hash.put(cell,m);
				}
			else 
			{ m.height=-1;	// mark all heights acceptable.
			  hash.put(getCell(m.dest,m.to_col,m.to_row),m);
			}
		}
		}
}
/**
 * getTargets() is called from the user interface to get a hashtable of 
 * cells which the mouse can legally hit.
 * 
 * Sixmaking uses the move generator for most of the logic of where it's legal
 * for the mouse to pick up or drop something.  We start with the list of legal
 * moves, and select either the legal "from" spaces, or the legal "to" spaces.
 * 
 * The advantage of this approach is that the logic for "legal moves" whatever it
 * may be, is needed anyway to drive the robot, and by reusing the move list we
 * avoid having to duplicate that logic.
 * 
 * @return
 */
public Hashtable<SixmakingCell,SixmakingMovespec>getTargets()
{
	Hashtable<SixmakingCell,SixmakingMovespec>hash = new Hashtable<SixmakingCell,SixmakingMovespec>();
	CommonMoveStack all = new CommonMoveStack();

		switch(board_state)
		{
		default: break;
		case Play:
			{	if(pickedStack.isEmpty()) 
					{ addMoves(all,whoseTurn); }
					else { SixmakingCell from = pickedSourceStack.top();
					       addMovesFor(all,from,pickedStack.height()+from.height(),pickedStack.height(),whoseTurn); 
					     }
				loadHash(all,hash,pickedStack.isEmpty());
			}
			break;
		}
	return(hash);
}

private boolean addPawnMoves(CommonMoveStack all,SixmakingCell from,int height,int who)
{	boolean some = false;
	for(int h = height; h>0; h--)
	{	some |= addPawnMovesFor(all,from,h,who);
		if(some && (all==null)) { return(some); }
	}
	return(some);
}

private boolean addPawnMovesFor(CommonMoveStack all,SixmakingCell from,int height,int who)
{	boolean some = false;
	for(int direction = CELL_UP,step=0; step<4; step++,direction+=CELL_QUARTER_TURN)
	{	SixmakingCell next = from.exitTo(direction);
		if(next!=null && !next.isEmpty())
		{	// one in any orthoinal direction.  These are never encumbered by Ko
			if(!next.isEmpty())
			{	some |= true;
				if(all!=null) { all.push(new SixmakingMovespec(from,next,height,who)); }
			}
		}
		if((all==null) && some) { return(some); }
	}
	return(some);
}

// this detects moving back to the previous position, which is forbidden
public boolean isKoMove(SixmakingCell from,SixmakingCell to,int height,int who)
{	int nextp = nextPlayer[who];
	return((lastDest[nextp]==from)
			&& (lastSource[nextp]==to)
			&& (lastHeight[nextp]==height));
}

// add a line of moves "from" somewhere in a given direction, with a given height
public boolean addLineOfMoves(CommonMoveStack all,int direction,SixmakingCell from,int h,int who)
{	SixmakingCell next = from;
	boolean some = false;
	while(!some && (next=next.exitTo(direction))!=null)
	{	if(!next.isEmpty())
		{  if(isKoMove(from,next,h,who)) { break; }	// move to next is illegal, but ends the line of possibilities
			{some |= true;
			if(all!=null) 
				{ all.push(new SixmakingMovespec(from,next,h,who)); }
			}
		}
	}
	return(some);
}

// add rook moves "from" somewhere with a maximum height (which will always be 2)
public boolean addRookMoves(CommonMoveStack all,SixmakingCell from,int height,int who)
{	boolean some = false;
	for(int h = height; h>0; h--)
	{	some |= addRookMovesFor(all,from,h,who);
		if(some && all==null) { return(some); }
	}
	return(some);
}
// add rook moves "from" somewhere with a particular height
private boolean addRookMovesFor(CommonMoveStack all,SixmakingCell from,int height,int who)
{	boolean some = false;
	for(int direction = CELL_UP,step=0; step<4; step++,direction+=CELL_QUARTER_TURN)
	{
	some |= addLineOfMoves(all,direction,from,height,who);
	if(some && (all==null)) { return(some); }
	}
	return(some);
}
public boolean addKnightMoves(CommonMoveStack all,SixmakingCell from,int height,int who)
{	boolean some = false;
	for(int h = height; h>0; h--)
	{	some |= addKnightMovesFor(all,from,h,who);
		if(some && (all==null)) { return(some); }
	}
	return(some);
}
public boolean addKnightMovesFor(CommonMoveStack all,SixmakingCell from,int height,int who)
{	boolean some = false;
		for(int direction = CELL_UP,step=0; step<4; step++,direction+=CELL_QUARTER_TURN)
		{	SixmakingCell next1= from.exitTo(direction);
			if(next1!=null)
			{	// one in any orthgonal direction
				for(int dir2 = direction-1,step2=0; step2<2; step2++,dir2+=CELL_QUARTER_TURN)
				{
				SixmakingCell next = next1.exitTo(dir2);
				if(next!=null && !next.isEmpty() && !isKoMove(from,next,height,who))
				{	some |= true;
					if(all!=null) { all.push(new SixmakingMovespec(from,next,height,who)); }
				}
				}
			}
			if((all==null) && some) { return(some); }
		}
	return(some);
}
public boolean addBishopMoves(CommonMoveStack all,SixmakingCell from,int height,int who)
{	boolean some = false;
	for(int h = height; h>0; h--)
	{	some |= addBishopMovesFor(all,from,h,who);
		if(some && (all==null)) { return(some); }
	}
	return(some);
}

private boolean addBishopMovesFor(CommonMoveStack all,SixmakingCell from,int height,int who)
{	boolean some = false;
		for(int direction = CELL_UP_LEFT,step=0; step<4; step++,direction+=CELL_QUARTER_TURN)
		{
		some |= addLineOfMoves(all,direction,from,height,who);
		if(some && (all==null)) { return(some); }
		}
	return(some);
}

private boolean addQueenMoves(CommonMoveStack all,SixmakingCell from,int height,int who)
{	boolean some = false;
	for(int h = height; h>0; h--)
	{	some |= addQueenMovesFor(all,from,h,who);
		if(some && (all==null)) { return(some); }
	}
	return(some);
}
private boolean addQueenMovesFor(CommonMoveStack all,SixmakingCell from,int height,int who)
{	boolean some = false;
		for(int direction = from.geometry.n-1; direction>=0;direction--)
		{
		some |= addLineOfMoves(all,direction,from,height,who);
		if(some && (all==null)) { return(some); }
		}
	return(some);
}
private boolean addKingMoves(CommonMoveStack all,SixmakingCell from,int height,int who)
{	boolean some = false;
	for(int h = height; h>0; h--)
	{	some |= addKingMovesFor(all,from,h,who);
		if(some && (all==null)) { return(some); }
	}
	return(some);
}
private boolean addKingMovesFor(CommonMoveStack all,SixmakingCell from,int height,int who)
{	boolean some = false;
	for(int direction = from.geometry.n-1; direction>=0;direction--)
	{	SixmakingCell next = from.exitTo(direction);
		if(next!=null && !next.isEmpty())
		{	// one in any orthgonal direction
			if(!next.isEmpty())
			{	some |= true;
				if(all!=null) { all.push(new SixmakingMovespec(from,next,height,who)); }
			}
		}
		if((all==null) && some) { return(some); }
	}
	return(some);
}
 // add normal sixmaking moves
 // "all" can be null
 // return true if there are any.
 public boolean addSimpleMoves(CommonMoveStack all,int who)
 {	boolean some = false;
 	switch(board_state)
 	{
	case Puzzle:
 	case Confirm:
 	case DeclinePending:
 	case AcceptPending:
 		if(all==null) { return(true); }
 		all.push(new SixmakingMovespec(MOVE_DONE,who));
 		break;
 	case AcceptOrDecline:
 		if(all==null) { return(true); }
 		if(drawIsLikely()) { all.push(new SixmakingMovespec(MOVE_ACCEPT_DRAW,who)); }
 		all.push(new SixmakingMovespec(MOVE_DECLINE_DRAW,who));
 		break;
 	case Play:
 	if(rack[who].height()>0)
 	{	some |= addMoves(all,rack[who],1,who);
 	}
 	if(some && (all==null)) { return(some); }
 	
 	for(SixmakingCell c = allCells; c!=null; c=c.next)
 	{	int h = c.height();
 		some |= addMoves(all,c,h,who);
 		if(some && (all==null)) { return(some); }
 	}
  		break;
 	default: G.Error("Not expecting state %s",board_state);
 	}
 	return(some);
 }
 public boolean addMovesFor(CommonMoveStack all,SixmakingCell c,int forH,int movingH,int who)
 {	boolean some = false;
 	if(c.onBoard)
 	{
 	switch(forH)
 		{
 		case 0: break;
 		case 1: some |= addPawnMovesFor(all,c,movingH,who); break;
 		case 2: some |= addRookMovesFor(all,c,movingH,who); break;
 		case 3: some |= addKnightMovesFor(all,c,movingH,who); break;
 		case 4: some |= addBishopMovesFor(all,c,movingH,who); break;
 		case 5: some |= addQueenMovesFor(all,c,movingH,who); break;
 		default: some |= addKingMovesFor(all,c,movingH,who); break;
 		}
 		if(some && (all==null)) { return(some); }
 	}
 	else 
 	{
 		for(SixmakingCell d = allCells; d!=null; d=d.next)
 		{	if(d.isEmpty())
 			{	if(all==null) { return(true); }
 				all.push(new SixmakingMovespec(c,d,1,who));
 			}
 		}
 	}
  	return(some);
 }
 public boolean addMoves(CommonMoveStack all,SixmakingCell c,int h,int who)
 {	boolean some = false;
 	if(c.onBoard)
 	{
 	switch(h)
 		{
 		case 0: break;
 		case 1: some |= addPawnMoves(all,c,h,who); break;
 		case 2: some |= addRookMoves(all,c,h,who); break;
 		case 3: some |= addKnightMoves(all,c,h,who); break;
 		case 4: some |= addBishopMoves(all,c,h,who); break;
 		case 5: some |= addQueenMoves(all,c,h,who); break;
 		default: some |= addKingMoves(all,c,h,who); break;
 		}
 		if(some && (all==null)) { return(some); }
 	}
 	else 
 	{
 		for(SixmakingCell d = allCells; d!=null; d=d.next)
 		{	if(d.isEmpty())
 			{	if(all==null) { return(true); }
 				all.push(new SixmakingMovespec(c,d,1,who));
 			}
 		}
 	}
  	return(some);
 }
 
 public boolean hasLegalMoves()
 {
	 return(addMoves(null,whoseTurn));
 }
 public boolean addMoves(CommonMoveStack all,int who)
 {	
 	switch(variation)
 	{
 	case Sixmaking: 
 	case Sixmaking_4:
 		return(addSimpleMoves(all,who));
 		default: throw G.Error("Not expecting %s",variation); 
 	 }
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	addMoves(all,whoseTurn);
 	return(all);
 }
 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   
 	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
 } 

}
