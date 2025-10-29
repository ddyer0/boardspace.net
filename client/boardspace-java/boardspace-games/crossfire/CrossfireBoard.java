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
package crossfire;

import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;

/**
 * CrossfireBoard knows all about the game of Hex, which is played
 * on a hexagonal board. It gets a lot of logistic support from 
 * common.hexBoard, which knows about the coordinate system.  
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

class CrossfireBoard extends hexBoard<CrossfireCell> implements BoardProtocol,CrossfireConstants
{	
    static final String[] CROSSFIREGRIDSTYLE = { null,"A1", "A1" }; // left and bottom numbers
    static final CrossId playerColor[] = {CrossId.White_Chip_Pool,CrossId.Black_Chip_Pool};
    static final CrossId prisonerColor[] = { CrossId.White_Prisoner_Pool,CrossId.Black_Prisoner_Pool};
    static int[] ZfirstInCol = { 6, 3, 0, 1, 0, 1, 0, 3, 6 };
    static int[] ZnInCol =     {1, 4, 7, 6, 7, 6, 7, 4, 1 }; // depth of columns, ie A has 4, B 5 etc.
	CrossfireState unresign;
	CrossfireState board_state;
	public CrossfireState getState() {return(board_state); }
	public void setState(CrossfireState st) 
	{ 	unresign = (st==CrossfireState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	// this is required even if it is meaningless for this game, but possibly important
// in other games.  When a draw by repetition is detected, this function is called.
// the game should have a "draw pending" state and enter it now, pending confirmation
// by the user clicking on done.   If this mechanism is triggered unexpectedly, it
// is probably because the move editing in "editHistory" is not removing last-move
// dithering by the user, or the "Digest()" method is not returning unique results
// other parts of this mechanism: the Viewer ought to have a "repRect" and call
// DrawRepRect to warn the user that repetitions have been seen.
	public void SetDrawState() 
		{ setState(CrossfireState.ILLEGAL_MOVE_STATE); 
		};	
    public RepeatedPositions repeatedPositions = null;

    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public CellStack animationStack = new CellStack();
    public CrossfireChip pickedObject = null;
    public CrossfireChip lastPicked = null;
    public CrossfireCell reserve[] = new CrossfireCell[2];
    public CrossfireCell prisoners[] = new CrossfireCell[2];
    CrossfireCell white_reserve = null;
    CrossfireCell black_reserve = null;
    CrossfireCell white_prisoners = null;
    CrossfireCell black_prisoners = null;
    public CrossfireCell pickedSource = null; 
    public CrossfireCell prevPickedSource = null;
    public CrossfireCell prevDroppedDest = null;
    private CellStack robotStack = new CellStack();
    public int robotDepth = 0;
    public CrossfireCell droppedDest = null;
    private int droppedUndoInfo = 0;
    public CrossfireChip lastDroppedObject = null;	// for image adjustment logic


	// factory method to generate a board cell
	public CrossfireCell newcell(char c,int r)
	{	return(new CrossfireCell(c,r));
	}
    public CrossfireBoard(String init,RepeatedPositions rep,int[]map) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = CROSSFIREGRIDSTYLE;
        repeatedPositions = rep;
        Random r = new Random(6326462);
        white_prisoners = new CrossfireCell(r,FIRST_PLAYER_INDEX,prisonerColor[FIRST_PLAYER_INDEX]);
        black_prisoners = new CrossfireCell(r,SECOND_PLAYER_INDEX,prisonerColor[SECOND_PLAYER_INDEX]);
        white_reserve = new CrossfireCell(r,FIRST_PLAYER_INDEX,playerColor[FIRST_PLAYER_INDEX]);
        black_reserve = new CrossfireCell(r,SECOND_PLAYER_INDEX,playerColor[SECOND_PLAYER_INDEX]);
 
        setColorMap(map, 2);
        doInit(init); // do the initialization 
        autoReverseY();		// reverse_y based on the color map
   }
    public CrossfireBoard cloneBoard() 
	{ CrossfireBoard dup = new CrossfireBoard(gametype,new RepeatedPositions(),getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((CrossfireBoard)b); }

    public CrossfireCell getCell(CrossfireCell c) 
    { 	if(c==null) { return(null);} 
    	if(c.onBoard) { return(getCell(c.col,c.row)); }
    	if(c.rackLocation==CrossId.Black_Chip_Pool) { return(black_reserve); }
    	if(c.rackLocation==CrossId.White_Chip_Pool) { return(white_reserve); }
    	if(c.rackLocation==CrossId.Black_Prisoner_Pool) { return(black_prisoners); }
    	if(c.rackLocation==CrossId.White_Prisoner_Pool) { return(white_prisoners); }
    	throw G.Error("Unexpected cell %s",c);
   }
    static int start[][] = {{'B',1,1},{'B',2,0},{'B',3,0},{'B',4,0},
    	{'C',2,1},{'C',3,1},{'C',4,0},{'C',5,0},{'C',6,1},
    	{'D',1,1},{'D',2,1},{'D',3,1},{'D',4,0},{'D',5,1},{'D',6,1},
    	{'E',1,0},{'E',2,0},{'E',3,0},{'E',5,1},{'E',6,1},{'E',7,1},
    	{'F',1,0},{'F',2,0},{'F',3,1},{'F',4,0},{'F',5,0},{'F',6,0},
    	{'G',2,0},{'G',3,1},{'G',4,1},{'G',5,0},{'G',6,0},
    	{'H',1,1},{'H',2,1},{'H',3,1},{'H',4,0}};
    
    // standared init for Hex.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard(String game)
    {	int[] firstcol = null;
    	int[] ncol = null;
    	if(Crossfire_INIT.equalsIgnoreCase(game)) { firstcol = ZfirstInCol; ncol = ZnInCol; }
    	else { throw G.Error(WrongInitError,game); }
        gametype = game;
        setState(CrossfireState.PUZZLE_STATE);
       
        reInitBoard(firstcol, ncol, null); //this sets up a hexagonal board
        animationStack.clear();
        int []map=getColorMap();
        
        
        for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX; i++)
        {	CrossfireCell p = prisoners[map[i]] = getCell(prisonerColor[i],'@',0);
        	CrossfireCell r = reserve[map[i]] = getCell(playerColor[i],'@',0);
        	p.reInit();
        	r.reInit();
        }
        
        whoseTurn = FIRST_PLAYER_INDEX;
        prevDroppedDest = droppedDest = null;
        droppedUndoInfo = 0;
        prevPickedSource = pickedSource = null;
        lastDroppedObject = null;

        // set the initial contents of the board to all empty cells
		for(CrossfireCell c = allCells; c!=null; c=c.next) { c.reInit(); }
		for (int col=start.length-1;  col>=0;  col--)
		{	int colr[] = start[col];
			char c = (char)colr[0];
			int  row = colr[1];
			CrossfireChip  p = CrossfireChip.getChip(colr[2]);
			getCell(c,row).addChip(p);
		}
    }
    public void sameboard(BoardProtocol f) { sameboard((CrossfireBoard)f); }
    private int playerIndex(CrossfireChip ch) { return(getColorMap()[ch.colorIndex]); }
    public double dumbotEval(int forplayer,boolean print)
    {	if(board_state==CrossfireState.ILLEGAL_MOVE_STATE) { return(-VALUE_OF_WIN/2); }
    	double val = reserve[forplayer].height()-prisoners[forplayer].height();
    	for(CrossfireCell c = allCells; c!=null; c=c.next)
    	{	CrossfireChip top = c.topChip();
    		if(top!=null && playerIndex(top)==forplayer) { val++; }
    	}
    	return(val);
    }
    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(CrossfireBoard from_b)
    {
        super.sameboard(from_b); // hexboard compares the boards
        G.Assert(sameCells(prisoners,from_b.prisoners),"prisoners mismatch");
        G.Assert(sameCells(reserve,from_b.reserve),"reserve mismatch");
        G.Assert(sameCells(pickedSource,from_b.pickedSource),"pickedSource matches");
        G.Assert(sameCells(droppedDest,from_b.droppedDest),"droppedDest matches");
        G.Assert(droppedUndoInfo==from_b.droppedUndoInfo, "undo info matches");
 
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
    public long Digest()
    {
 
        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        Random r = new Random(64 * 1000); // init the random number generator
    	long v = super.Digest(r);

		for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX; i++) 
		{ v ^= reserve[i].height()^r.nextLong(); 
		  v ^= prisoners[i].height()*r.nextLong();
		}
        v ^= chip.Digest(r,pickedObject);
        v ^= cell.Digest(r,pickedSource);
        v ^= cell.Digest(r,droppedDest);
        v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
 		return (v);
    }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(CrossfireBoard from_b)
    {	super.copyFrom(from_b);
        repeatedPositions = from_b.repeatedPositions.copy();

        droppedDest = getCell(from_b.droppedDest);
        droppedUndoInfo = from_b.droppedUndoInfo;
        pickedSource = getCell(from_b.pickedSource);
        pickedObject = from_b.pickedObject;	// immutable object
        lastPicked = null;
        robotDepth = from_b.robotDepth;
		for(int i=0;i<2;i++) 
		{  reserve[i] = getCell(from_b.reserve[i]);
		   prisoners[i] = getCell(from_b.prisoners[i]);
		   reserve[i].copyFrom(from_b.reserve[i]);
		   prisoners[i].copyFrom(from_b.prisoners[i]);
		}
		board_state = from_b.board_state;
		unresign = from_b.unresign;
        sameboard(from_b); 
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long random)
    {	randomKey = random;
       Init_Standard(gtype);
       robotStack.clear();
       robotDepth = 0;
       moveNumber = 1;
       pickedObject = null;
       prevPickedSource = pickedSource = null;
       prevDroppedDest = droppedDest = null;

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
        {
        case RESIGN_STATE:
        case CONFIRM_STATE:
            return (true);

        default:
            return (false);
        }
    }
    // this is the default, so we don't need it explicitly here.
    // but games with complex "rearrange" states might want to be
    // more selective.  This determines if the current board digest is added
    // to the repetition detection machinery.
    public boolean DigestState()
    {	
    	return(DoneState());
    }



    // this method is also called by the robot to get the blobs as a side effect
    public boolean WinForPlayerNow(int player)
    {
     	return(win[player] || !hasLegalMoves(nextPlayer[player]));
    }



    //
    // accept the current placements as permanant
    //
    public void acceptPlacement()
    {	if(droppedDest!=null)
    	{
    	prevPickedSource = pickedSource;
    	prevDroppedDest = droppedDest;
        droppedDest = null;
        pickedSource = null;
        droppedUndoInfo = 0;
    	}
     }
    int prevPicked = -1;
    int prevDropped = -1;
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    	if(droppedDest!=null) 
    	{	
    		if(board_state==CrossfireState.PUZZLE_STATE)
    			{
    			pickedObject = droppedDest.removeTop();
    			}
    			else 
    			{ 
   				restoreExcess(droppedDest,droppedUndoInfo);
    			moveStack(droppedDest,pickedSource,findDistance(droppedDest,pickedSource),replayMode.Replay);
    			pickedObject = pickedSource.removeTop();
    			}
    		droppedDest.lastDropped = prevDropped;
   			droppedUndoInfo = 0;
    		droppedDest = null;
     	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	if(pickedSource!=null) 
    		{ //SetBoard(pickedSource,pickedObject);
    		  pickedSource.addChip(pickedObject);
    		  pickedSource.lastPicked = prevPicked;
    		  pickedSource = null;
    		}
		  pickedObject = null;
  }
    // 
    // drop the floating object.
    //
    private void dropObject(CrossfireMovespec m,replayMode replay)
    {	CrossfireCell dest = null;
        switch (m.source)
        {
        default:
        	throw G.Error("not expecting dest %s", m.source);
        case Black_Prisoner_Pool:
        	dest = prisoners[SECOND_PLAYER_INDEX];
        	break;
        case White_Prisoner_Pool:
        	dest = prisoners[FIRST_PLAYER_INDEX];
        	break;
        case Black_Chip_Pool:
        	dest = reserve[SECOND_PLAYER_INDEX];
        	break;
        case White_Chip_Pool:		// back in the pool, we don't really care where
        	dest = reserve[FIRST_PLAYER_INDEX];
            break;
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        case EmptyBoard:
           	dest = getCell(m.to_col,m.to_row);
             break;
        }
       if(isSource(dest)) { unPickObject(); }
       else
       {
       if(board_state==CrossfireState.PUZZLE_STATE)
       {
    	dest.addChip(pickedObject);
        m.undoInfo = droppedUndoInfo = 0;
       }
       else
       {
        pickedSource.addChip(pickedObject);
        moveStack(pickedSource,dest,findDistance(pickedSource,dest),replay);
        m.undoInfo = droppedUndoInfo = removeExcess(dest,replay);
        }
       lastDroppedObject = pickedObject;
       prevDropped = dest.lastDropped;
       dest.lastDropped = moveNumber;
       droppedDest = dest;
       pickedObject = null;
       setNextStateAfterDrop();
       }
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(CrossfireCell c)
    {	return(droppedDest==c);
    }
    
    public Hashtable<CrossfireCell,CrossfireCell> getDests()
    {	Hashtable<CrossfireCell,CrossfireCell> h = new Hashtable<CrossfireCell,CrossfireCell>();
    	switch(board_state)
    	{
    	default: break;
    	case PLAY_STATE:
    		if((pickedObject!=null)&&pickedSource.onBoard)
    		{
    			CommonMoveStack res = new CommonMoveStack();
    			addMovesFrom(res,pickedSource,pickedObject,whoseTurn);
    			for(int lim=res.size()-1; lim>=0; lim--)
    			{
    				CrossfireMovespec m = (CrossfireMovespec)res.elementAt(lim);
    				CrossfireCell c = getCell(m.to_col,m.to_row);
    				h.put(c,c);
    			}
    		}
    	}
    	return(h);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { CrossfireChip ch = pickedObject;
      if(ch!=null)
    	{	return(ch.chipNumber()); 
    	}
      	return (NothingMoving);
    }

    private void pickObject(CrossfireCell c)
    {
    	boolean wasDest = isDest(c);
    	unDropObject(); 
    	if(!wasDest)
    	{
        pickedSource = c;
        prevPicked = c.lastPicked;
        c.lastPicked = moveNumber;
        lastPicked = pickedObject = c.removeTop();
     	lastDroppedObject = null;
     	droppedDest = null;
    	}

    }

    private CrossfireCell getCell(CrossId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	return(getCell(col,row));
        case Black_Prisoner_Pool:
        	return(black_prisoners);
        case Black_Chip_Pool:
        	return(black_reserve);
        case White_Prisoner_Pool:
            return(white_prisoners);
        case White_Chip_Pool:
            return(white_reserve);
        }
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(CrossfireCell c)
    {	return(c==pickedSource);
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
        	if(droppedDest==null)
        	{setNextStateAfterDone();
        	}
        	break;
        case PLAY_STATE:
			setState(CrossfireState.CONFIRM_STATE);
			break;
        case PUZZLE_STATE:
			acceptPlacement();
            break;
        }
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case GAMEOVER_STATE: break;
     	case CONFIRM_STATE:
    	case PUZZLE_STATE:
    	case PLAY_STATE:
    		setState(CrossfireState.PLAY_STATE);
    		break;
        case ILLEGAL_MOVE_STATE:
       	 	win[whoseTurn]=true;
       	 	setState(CrossfireState.GAMEOVER_STATE);
        	break;

    	}

    }
    private void doDone()
    {
        acceptPlacement();

        if (board_state==CrossfireState.RESIGN_STATE)
        {
            win[nextPlayer[whoseTurn]] = true;
        	setState(CrossfireState.GAMEOVER_STATE);
        }
        else
        {	if(WinForPlayerNow(whoseTurn)) 
        		{ win[whoseTurn]=true;
        		  setState(CrossfireState.GAMEOVER_STATE);
        		}
        	else {setNextPlayer();
        		setNextStateAfterDone();
        	}
        }
    }
    // remove elements from the bottom of a cell until the height is 
    // not greater than the storage capacity.  Add the removed chips
    // to the prisoners or reserve of the owner, depending on whose turn it is.
    // return an undo code so the process can be reversed
    private int removeExcess(CrossfireCell from,replayMode replay)
    {	int undoCode = 0;
    	int cap = from.stackCapacity();
    	int h = from.height();
    	while(h>cap) 
    	{	CrossfireChip ch = from.removeChipAtIndex(0);
    		int player = playerIndex(ch);
    		undoCode = (undoCode<<2) | (player+1);
    		h--;
    		CrossfireCell dest = ((player==whoseTurn)?reserve[player]:prisoners[player]);
    		if(replay.animate)
    		{	animationStack.push(from);
    			animationStack.push(dest);
    		}
    		dest.addChip(ch);
    	}
    	return(undoCode);
    }
    private void restoreExcess(CrossfireCell from,int undoCode)
    {
    		while(undoCode!=0)
    		{
    			int player = (undoCode&3)-1;
    			undoCode = undoCode>>2;
    			CrossfireCell storage = (player==whoseTurn) ? reserve[player] : prisoners[player];
    			from.insertChipAtIndex(0,storage.removeTop());
    		}
    }
    private void moveStack(CrossfireCell from,CrossfireCell to,int height,replayMode replay)
    {
    	CrossfireChip top = from.removeTop();
    	if(height>1)
    		{ moveStack(from,to,height-1,(replay.animate)?replayMode.Single:replay); 
    		}
    	to.addChip(top);
    	if(replay==replayMode.Single)
    	{
    		animationStack.push(from);
    		animationStack.push(to);
    	}
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	CrossfireMovespec m = (CrossfireMovespec)mm;
    	if(replay.animate) { animationStack.clear(); }
        //G.print("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
        case MOVE_DONE:
         	doDone();
            break;

        case MOVE_DROPB:
        	{
            if(pickedObject!=null) { dropObject(m,replay); }
        	}
            break;

        case MOVE_PICK:
            unDropObject();
            unPickObject();
            
			//$FALL-THROUGH$
		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
		{
			CrossfireCell src = getCell(m.source, m.from_col, m.from_row);
			if(src==null)
			{	//some damaged games 
				m.op=MOVE_REJECT;
			}
			else
			{
			pickObject(src);
        	if(board_state!=CrossfireState.PUZZLE_STATE) { setState(CrossfireState.PLAY_STATE); }
			}}
            break;

        case MOVE_DROP: // drop on chip pool;
            dropObject(m,replay);
            if(replay==replayMode.Single)
            {	animationStack.push(pickedSource);
            	animationStack.push(droppedDest);
            }
            break;

        case MOVE_FROM_RESERVE:
        	{
        	CrossfireCell from = pickedSource = reserve[whoseTurn];
        	CrossfireCell to = droppedDest = getCell(m.to_col,m.to_row);
        	moveStack(from,to,1,replay);
        	m.undoInfo = droppedUndoInfo = removeExcess(to,replay);
        	setState(CrossfireState.CONFIRM_STATE);
        	}
        	break;
        case MOVE_FROM_TO:
        	{
        	CrossfireCell from = pickedSource = getCell(m.from_col,m.from_row);
        	CrossfireCell to = droppedDest = getCell(m.to_col,m.to_row); 
        	moveStack(from,to,m.height,replay);
        	m.undoInfo = droppedUndoInfo = removeExcess(to,replay);
        	setState(CrossfireState.CONFIRM_STATE);

        	}
        	break;
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            int nextp = nextPlayer[whoseTurn];
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(CrossfireState.PUZZLE_STATE);	// standardize the current state
            if((win[whoseTurn]=WinForPlayerNow(whoseTurn))
               ||(win[nextp]=WinForPlayerNow(nextp)))
               	{ setState(CrossfireState.GAMEOVER_STATE); 
               	}
            else {  setNextStateAfterDone(); }

            break;
            
       case MOVE_ROBOT_RESIGN:
    	   m.undoInfo = droppedUndoInfo;
    	   setState(unresign==null?CrossfireState.RESIGN_STATE:unresign);
    	   break;
       case MOVE_RESIGN:
            setState(unresign==null?CrossfireState.RESIGN_STATE:unresign);
            m.undoInfo = 0;
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(CrossfireState.PUZZLE_STATE);
 
            break;
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(CrossfireState.GAMEOVER_STATE);
			break;

        default:
        	cantExecute(m);
        }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+board_state);

        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case CONFIRM_STATE:
        case RESIGN_STATE:
        	return(false);
        case PLAY_STATE:
        	// you can pick up a stone in the storage area
        	// but it's really optional
        	return((pickedObject==null)?(player==whoseTurn):false);
        case ILLEGAL_MOVE_STATE:
		case GAMEOVER_STATE:
			return(false);
        case PUZZLE_STATE:
            return ((pickedObject!=null)?(pickedObject==CrossfireChip.getChip(player)):true);
        }
    }

    public boolean LegalToHitBoard(CrossfireCell c)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
		case PLAY_STATE:
			{	if(pickedObject==null)
					{
					CrossfireChip ch = c.topChip();
					return((ch!=null) && (playerIndex(ch)==whoseTurn));
					}
					else if(pickedSource.onBoard)
					{
					Hashtable<CrossfireCell,CrossfireCell> d = getDests();
					return(d.get(c)!=null);
					}
					else
					{
						return(true);
					}
			}
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
		case ILLEGAL_MOVE_STATE:
		case CONFIRM_STATE:
			return(isDest(c));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case PUZZLE_STATE:
            return ((pickedObject!=null) || (c.topChip()!=null));
        }
    }
    
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(CrossfireMovespec m)
    {	robotStack.push(pickedSource);
    	robotStack.push(droppedDest);
    	robotDepth++;
        m.state = board_state; //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        //G.print("R "+m);
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        if (Execute(m,replayMode.Replay))
        {
            if (m.op == MOVE_DONE)
            {
            }
            else if((m.op==MOVE_RESIGN)||(m.op==MOVE_ROBOT_RESIGN))
            { doDone(); }
            else if (DoneState())
            {
            	long dig = Digest();
            	int num = repeatedPositions.addToRepeatedPositions(dig,m);
            	if(num>1) { setState(CrossfireState.ILLEGAL_MOVE_STATE);  }
                else {  doDone(); }
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
    public void UnExecute(CrossfireMovespec m)
    {	int dropundo = 0;
        //G.print("U "+m);
    	int old_whoseTurn = whoseTurn;
    	robotDepth--;
    	setWhoseTurn(m.player);
        switch (m.op)
        {
        case MOVE_START:
        case MOVE_PICK:
        case MOVE_DROP:
        case MOVE_EDIT: // robot never does these
   	    default:
   	    	throw G.Error("Can't un execute %s", m);
        case MOVE_DONE:
            break;

        case MOVE_DROPB:
        	CrossfireCell ch = getCell(m.to_col,m.to_row);
        	ch.removeTop();
        	break;
        case MOVE_FROM_RESERVE:
        	{
        	CrossfireCell from = reserve[whoseTurn];
        	CrossfireCell to = getCell(m.to_col,m.to_row);
        	restoreExcess(to,m.undoInfo);
        	moveStack(to,from,1,replayMode.Replay);
        	repeatedPositions.removeFromRepeatedPositions(m);
         	}
        	break;
        case MOVE_FROM_TO:
        	{
        	CrossfireCell to = getCell(m.to_col,m.to_row);
        	CrossfireCell from = getCell(m.from_col,m.from_row);
        	restoreExcess(to,m.undoInfo);
        	moveStack(to,from,m.height,replayMode.Replay);
        	repeatedPositions.removeFromRepeatedPositions(m);
        	}
        	break;
        case MOVE_ROBOT_RESIGN:
        	dropundo = m.undoInfo;
        	break;
        case MOVE_RESIGN:
        	dropundo = m.undoInfo;
            break;
        }
        droppedUndoInfo = dropundo;
        droppedDest = robotStack.pop();
        pickedSource = robotStack.pop();
        setState(m.state);
        if(whoseTurn!=old_whoseTurn)
        {	moveNumber--;
        }
 }
 public void addDropMoves(CommonMoveStack  v,int who)
 {	CrossfireCell c = reserve[who];
	 if(c.topChip()!=null)
	 {
	 for(CrossfireCell ch = allCells; ch!=null; ch=ch.next)
	 	{
		 v.addElement(new CrossfireMovespec(MOVE_FROM_RESERVE,playerColor[who],ch.col,ch.row,who));
	 	}
	 }
 }
 public int addMovesFrom(CommonMoveStack  v,CrossfireCell ch,CrossfireChip top,int who)
 {	int height = ch.height()+((top==null)?0:1);
 	int nm = 0;
 	for(int dir=0; dir<CELL_FULL_TURN; dir++)
 	{
 	CrossfireCell dest = ch.exitTo(dir);
	for(int step = 0;(step<height) && (dest!=null); step++,dest = dest.exitTo(dir)) 
		{
		if(v!=null) 
			{ v.addElement(new CrossfireMovespec(MOVE_FROM_TO,ch.col,ch.row,dest.col,dest.row,step+1,who));
			}
		nm++;
		}
 	} 
 	return(nm);
 }
 public int addMoveMoves(CommonMoveStack  v,int who)
 {	int map[]=getColorMap();
 	CrossfireChip target = CrossfireChip.getChip(map[who]);
 	int nm = 0;
	 for(CrossfireCell ch = allCells; ch!=null; ch=ch.next)
	 {	CrossfireChip top = ch.topChip();
		if(top==target) { nm+=addMovesFrom(v,ch,null,who); }
	 }
	 return(nm);
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	switch(board_state)
 	{
 	case ILLEGAL_MOVE_STATE:
 		all.addElement(new CrossfireMovespec(MOVE_ROBOT_RESIGN,whoseTurn));
 		break;
 	case CONFIRM_STATE:	
 			all.addElement(new CrossfireMovespec(MOVE_DONE,whoseTurn));
 			break;
 	case PUZZLE_STATE:	throw G.Error("Not expecting puzzle state");
 	case PLAY_STATE:
 		addDropMoves(all,whoseTurn);
 		addMoveMoves(all,whoseTurn);
		break;
	default:
		break;
 	}

 	return(all);
 }
 public boolean hasLegalMoves(int who)
 {
	 if(reserve[who].height()>0) { return(true); }
	 int map[]=getColorMap();
	 CrossfireChip target = CrossfireChip.getChip(map[who]);
	 for(CrossfireCell ch = allCells; ch!=null; ch=ch.next)
	 {	CrossfireChip top = ch.topChip();
		if(top==target) 
			{ int nm =addMovesFrom(null,ch,top,who); 
			  if(nm>0) { return(true); }
			}
	 }
	 return(false);
 }


}
