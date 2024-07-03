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
package goban;


import bridge.Color;
import lib.Graphics;

import bridge.Utf8OutputStream;
import bridge.Utf8Printer;
import goban.shape.shape.Globals;

import static goban.GoMovespec.*;

import java.io.PrintStream;
import java.util.Hashtable;

import lib.AR;
import lib.G;
import lib.GC;
import lib.IStack;
import lib.Random;
import online.game.BoardProtocol;
import online.game.CommonMoveStack;
import online.game.chip;
import online.game.commonMove;
import online.game.replayMode;
import online.game.squareBoard;
/**
 * GoBoard knows all about the game of Go.
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

public class GoBoard extends squareBoard<GoCell> implements BoardProtocol,GoConstants,Globals
{	// TODO: fix seki detection in GO-bobc-ddyer-2020-10-20-0022
	// the upper-left corner, black looks like it has 2 eyes and so kills white
	// this isn't true, but if tweaked to look like black has 1 eye, then it looks
	// dead instead of white. 
    static final int White_Chip_Index = 1;
    static final int Black_Chip_Index = 0;
    static final GoId RackLocation[] = { GoId.Black_Chip_Pool,GoId.White_Chip_Pool};
    static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
   
    public int boardColumns;	// size of the board
    public int boardRows;
    public void SetDrawState() { setState(GoState.Draw); }
    public double finalTerritory[] = new double[] {0,0};
    public CellStack emptyCells = new CellStack(); 
    public GoCell rack[] = new GoCell[2];
    public GoCell rawRack[] = new GoCell[2];
    public CellStack animationStack = new CellStack();
    public int changeClock = 0;
    public Hashtable<String,String>properties = new Hashtable<String,String>();
    public void putProp(String key,String val)
    {
    	properties.put(key,val);
    }
    public String getProp(String key)
    {
    	return(properties.get(key));
    }
    public void  clearProps() { properties.clear(); }
    //
    // private variables
    //
    private GoState board_state = GoState.Play;	// the current board state
    private GoState unresign = null;					// remembers the previous state when "resign"
    private GoCell koLocation = null;
    private double komi = 0.0;
    public boolean komiIsSet = false;
    public double getKomi() { return(komi); }
    public void setKomi(double v)
    { komi = v; komiIsSet = true; }
    Variation variation = null;
    GoChip lastChip = null; 
    public GoCell lastHit = null;
    public GoState getState() { return(board_state); } 
	public void setState(GoState st) 
	{ 	unresign = (st==GoState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	GoChip playerChip[] = { GoChip.black,GoChip.white };
    
   public int chips_on_board = 0;			// number of chips currently on the board
	
   	public boolean isFirstMove() { return(0==chips_on_board); }
   	
   	public int[][] getHandicapValues()
   	{
   		return(variation.getHandicapValues());
   	}
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public GoChip pickedObject = null;
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    CellStack captureStack[] = null;
    private boolean robotBoard = false;
    private IStack robotIStack = new IStack();
    private CellStack robotOStack = new CellStack();
    int ghostLevel[] = {0,0};
	// factory method
	public GoCell newcell(char c,int r)
	{	return(new GoCell(c,r,this));
	}

	public GoBoard(String init,long rv,int np,int map[]) // default constructor
    {   
		captureStack = new CellStack[2];
		captureStack[0] = new CellStack();
		captureStack[1] = new CellStack();

        setColorMap(map, np);
        doInit(init,rv,np); // do the initialization 
     }
    
    public void setBoardSize(int n)
    {
    	Variation v = Variation.findBySize(n);
    	if(v!=null)
    	{
    		doInit(v,randomKey,players_in_game);
    	}
    }
	public void sameboard(BoardProtocol f) { sameboard((GoBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if clone,digest and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(GoBoard from_b)
    {
    	super.sameboard(from_b);	// calls sameCell for each cell, also for inherited class variables.
    	G.Assert(unresign==from_b.unresign,"unresign mismatch");
    	G.Assert(sameCells(koLocation,from_b.koLocation),"ko mismatch");
       	G.Assert(AR.sameArrayContents(win,from_b.win),"win array contents match");
       	G.Assert(AR.sameArrayContents(playerChip,from_b.playerChip),"playerChip contents match");
       	G.Assert(chips_on_board==from_b.chips_on_board,"chips_on_board contents match");
       	G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject doesn't match");
        G.Assert(sameCells(captureStack,from_b.captureStack),"capture stack mismatch");
        G.Assert(emptyCells.size()==from_b.emptyCells.size(),"emptycells mismatch");
        G.Assert(komi==from_b.komi,"komi mismatch");
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
		v ^= Digest(r,captureStack);
		v ^= Digest(r,koLocation);
		v ^= Digest(r,(int)(komi*2));
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
        return (v);
    }
   public GoBoard cloneBoard() 
	{ GoBoard copy = new GoBoard(gametype,randomKey,players_in_game,getColorMap());
	  copy.copyFrom(this); 
	  return(copy);
	}


    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
   public void copyFrom(BoardProtocol from) {
   	copyFrom((GoBoard)from);   	
   }
    public void copyFrom(GoBoard from_b)
    {	
        super.copyFrom(from_b);			// copies the standard game cells in allCells list
        AR.copy(finalTerritory,from_b.finalTerritory);
        pickedObject = from_b.pickedObject;	
        komi = from_b.komi;
        komiIsSet = from_b.komiIsSet;
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        AR.copy(playerChip,from_b.playerChip);
        chips_on_board = from_b.chips_on_board;
        getCell(captureStack,from_b.captureStack);
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        lastHit = getCell(from_b.lastHit);
        koLocation = getCell(from_b.koLocation);
        getCell(emptyCells,from_b.emptyCells);
        sameboard(from_b);
    }
    public void doInit(String gtype,long rv)
    {
    	doInit(gtype,rv,players_in_game);
    }
    public void constructBoard(Variation v)
    {	
    	if(variation!=v)
    	{	// do the expensive version of board construction.  Things done here
    		// should be immutable or manually reinitialized in doInit
        	Random r = new Random(67246765);
        	variation = v;
      	  	Grid_Style = GRIDSTYLE; //coordinates left and bottom
      	  	drawing_style = DrawingStyle.STYLE_LINES ; // STYLE_CELL or STYLE_LINES
         	for(int i=0,pl=0;i<2; i++,pl=nextPlayer[pl])
        	{
           	GoCell cell = new GoCell(r,RackLocation[i]);
           	cell.addChip(GoChip.chips[i]);
        	rack[i]=rawRack[i]=cell;
         	}
         	
         	switch(variation)
         	{
         	default:  throw G.Error(WrongInitError,variation);
         	case Go_19:
         	case Go_13:
         	case Go_11:
         	case Go_9:
         		boardColumns = variation.size;
         		boardRows = variation.size;
         		initBoard(boardColumns,boardRows);
         		break;
         	}
            allCells.setDigestChain(r);
            changeClock++;
    	}
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long rv,int np)
    {  	
    	Variation v = Variation.findVariation(gtype);
    	G.Assert(v!=null,"No variation named ",gtype);
 		doInit(v,rv,np);
    }
    public void doInit(Variation v,long rv,int np)
    {  	randomKey = rv;
		gametype = v.name;
		players_in_game = np;
 		nextMoves = null;
 		komi = 0.0;
 		komiIsSet=false;
        String komiProp = getProp("KM");
        if(komiProp!=null) { komi = G.DoubleToken(komiProp); komiIsSet=true; } 
        constructBoard(v);
    	emptyCells.clear();
    	for(GoCell c = allCells; c!=null; c=c.next) { c.reInit(); emptyCells.push(c); }
    	int map[]=getColorMap();
    	for(int i=0;i<2;i++) 
    		{ rack[map[i]] = rawRack[i];
    		  playerChip[map[i]]=GoChip.chips[i]; 
    		}
    	robotIStack.clear();
    	robotOStack.clear();
    	lastChip = null;
    	koLocation = null;
  	    setState(GoState.Play);
	    whoseTurn = FIRST_PLAYER_INDEX;
		pickedSourceStack.clear();
		droppedDestStack.clear();
		captureStack[0].clear();
		captureStack[1].clear();
		pickedObject = null;
        AR.setValue(win,false);
        chips_on_board=0;
        AR.setValue(ghostLevel,0);
        AR.setValue(finalTerritory,0);
        moveNumber = 1;
        changeClock++;
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
        case Play:
        case Draw:
        case Play1:
        case Resign:
        case ConfirmHandicapState:
            moveNumber++; //the move is complete in these states
			//$FALL-THROUGH$
		case Score:
            setWhoseTurn(nextPlayer[whoseTurn]);
            return;
        }
    }

    /** this is used to determine if the "Done" button in the UI is live
     *
     * @return a boolean
     */
    public boolean DoneState()
    {	
        switch (board_state)
        {case Resign:
         case Confirm:
         case ConfirmHandicapState:
         case Score:
         case Score2:
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
    	setState(GoState.Gameover);
    }
    
    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	if(board_state==GoState.Gameover) { return(win[player]); }
    	classifyBoard();
    	double t1 = finalTerritory[player] = territoryForPlayer(player);
    	double t2 = finalTerritory[nextPlayer[player]] = territoryForPlayer(nextPlayer[player]);
    	return(t1>t2);
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
    	GoCell dr = droppedDestStack.pop();
    	switch(dr.rackLocation())
	    	{
	   		default: throw G.Error("Not expecting rackLocation %s",dr.rackLocation);
			case BoardLocation: 
				pickedObject = removeChip(dr); 
				break;
			case White_Chip_Pool:	// treat the pools as infinite sources and sinks
			case Black_Chip_Pool:	
				pickedObject = dr.topChip();
				break;	// don't add back to the pool
	    	
	    	}
	    	}
    }
    public void addChip(GoCell c,GoChip ch)
    	{ c.addChip(ch); 
    	  chips_on_board++;
    	  emptyCells.remove(c,false);
    	  changeClock++;
        }
    public GoChip removeChip(GoCell c) 
    	{ GoChip ch = c.removeTop();
    	  chips_on_board--;
    	  emptyCells.push(c);
    	  changeClock++;
    	  return(ch);
    	}
    
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	GoChip po = pickedObject;
    	if(po!=null)
    	{
    		GoCell ps = pickedSourceStack.pop();
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
    // undo the pick, getting back to base state for the move
    //
    private void discardPickedObject()
    {	
    	if(pickedObject!=null)
    	{
    		pickedSourceStack.pop();
    		pickedObject = null;
     	}
     }
    // 
    // drop the floating object.
    //
    private void dropObject(GoCell c)
    {   G.Assert(pickedObject!=null,"pickedObject should not be null"); 	    		
    	switch(c.rackLocation())
		{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case BoardLocation: addChip(c,pickedObject); break;
		case White_Chip_Pool:
		case Black_Chip_Pool:	break;	// don't add back to the pool
		}
       	droppedDestStack.push(c);
       	pickedObject = null;
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(GoCell cell)
    {	return((droppedDestStack.size()>0) && (droppedDestStack.top()==cell));
    }
    public GoCell getDest() { return(droppedDestStack.top()); }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	GoChip ch = pickedObject;
    	if(ch!=null)
    		{ return(ch.chipNumber());
    		}
        return (NothingMoving);
    }
    
    public GoCell getCell(GoId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	return(getCell(col,row));
        case White_Chip_Pool:
       		return(rawRack[White_Chip_Index]);
        case Black_Chip_Pool:
       		return(rawRack[Black_Chip_Index]);
        }
    }
    public GoCell getCell(char col,int row)
    {
    	
    	return(super.getCell(col,(row<0) ? (boardRows+row+1) : row));
    }
    public GoCell getCell(GoCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(GoCell c)
    {	
    	switch(c.rackLocation())
    	{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case BoardLocation: 
			pickedObject = removeChip(c); 
			break;
		case White_Chip_Pool:
		case Black_Chip_Pool:	
			pickedObject = c.topChip();
			break;	// don't add back to the pool
    	
    	}
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
        case Confirm:
        case Draw:
        	setNextStateAfterDone(getDest()); 
        	break;
        case Play:
        case Play1:
			setState(GoState.Confirm);
			break;

        case Puzzle:
        case Gameover:
			acceptPlacement();
            break;
        }
    }
    //	
    //true if col,row is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(GoCell c)
    {	return(getSource()==c);
    }
    public GoCell getSource()
    {
    	return((pickedSourceStack.size()>0) ?pickedSourceStack.top() : null);
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
        case Confirm:
        case Draw:
        	setState(GoState.Play);
        	break;
        case Play:
        case Play1:
        case Score:
        case Score2:
        case Puzzle:
        case Gameover:
            break;
        }
    }
    private void setNextStateAfterDone(GoCell dest)
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case Gameover: 
    		break;

        case Draw:
        	setGameOver(false,false);
        	break;
    	case Play1:
    	case Confirm:
    	case ConfirmHandicapState:
    	case Puzzle:
    	case Play:
    		setState((dest==null)?GoState.Play1:GoState.Play);
    		break;
    	}

    }
   

    
    private void doDone()
    {	GoCell dest = getDest();
    	if(dest!=null) { doCaptures(dest); }
        acceptPlacement();

        switch(board_state)
        {
        case Resign:
        	setGameOver(false,true);
        	break;
        case Score2:
        	{
        	// second "done" while scoring, makes the game over.
        	boolean win1 = WinForPlayerNow(whoseTurn);
        	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
        	setGameOver(win1,win2);
        	}
        	break;
        case Score:
        	setNextPlayer();
        	setState(GoState.Score2);
        	break;
        default:
        	setNextPlayer();
        	setNextStateAfterDone(dest);
        }
    }

    
    //
    // put a stack of chips back on the board.  This is used
    // when undoing a "mark dead" move.
    //
    public void restoreChips(CellStack stack,GoChip chip)
    {	CellStack unremove = (chip==GoChip.black) ? captureStack[Black_Chip_Index] : captureStack[White_Chip_Index];
    	for(int lim=stack.size()-1; lim>=0; lim--)
    	{
    		GoCell c = getCell(stack.elementAt(lim));
    		G.Assert(c.topChip()==null,"should be empty");
    		G.Assert(unremove.remove(c,true)!=null,"should be there");
    		addChip(c,chip);
    	}
    }
    private void clearBoard()
    	{ for(GoCell c = allCells; c!=null; c=c.next) 
    		{ if(c.topChip()!=null) {  removeChip(c); } 
    		}
    	}
    
    public boolean Execute(commonMove mm,replayMode replay)
    {	GoMovespec m = (GoMovespec)mm;
        if(replay.animate) { animationStack.clear(); }
        switch (m.op)
        {
        case MOVE_HANDICAP:
        		{	boolean set=false;
        			clearBoard();
        			for( int h[] : getHandicapValues())
        			{
        			if(h[0]==m.to_row)
        			{
        			set=true;
        			int n = 1;
        				int len = h.length;
        				while(n<len)
        				{
        				GoCell dest = getCell(GoId.BoardLocation,(char)h[n],h[n+1]);
        				pickObject(getCell(GoId.Black_Chip_Pool,m.from_col,m.from_row));
        				dropObject(dest);
        				n += 2;
        				}
        			}}
        		acceptPlacement();
        		setState(set ? GoState.ConfirmHandicapState : GoState.Play);
        		}
        break;
        case MOVE_RESUMESCORING:
        	setState(GoState.Score);
        	break;
        case MOVE_SAFE:
        	{
        	GoCell dest = getCell(GoId.BoardLocation,m.to_col,m.to_row);
        	dest.safeOverride = !dest.safeOverride;
        	break;
        	}
        case MOVE_DEAD:
        	{
        	GoCell dest = getCell(GoId.BoardLocation,m.to_col,m.to_row);
        	dest.safeOverride = false;
        	GoChip top = dest.topChip();
        	CellStack remember = new CellStack();
        	doCaptures(dest);
        	m.source = top==GoChip.white ? GoId.White_Chip_Pool : GoId.Black_Chip_Pool;
        	int who = AR.indexOf(playerChip,top);
        	m.viewerInfo = remember;
        	captureGroup(dest,who,remember);
        	}
        	break;
        case MOVE_PASS:
        	switch(board_state)
        	{
        	case Confirm: setState(GoState.Play); break;
        	case Play1:
        			ghostLevel[0]=captureStack[0].size();
        			ghostLevel[1]=captureStack[1].size();
        			if(robotBoard)
        			{setNextPlayer();
        			 setState(GoState.Gameover);
        			}else
        			{
        			setState(GoState.Score);
        			}
        			break;
        	case Play: 
        		setState(GoState.Confirm); 
        		doDone();
        		break;
        	default: G.Error("Not expecting state %s",board_state);
        	}
        	break;
        case MOVE_NEXTPLAYER:
			// some old damaged games have "nextplayer" embedded
			// in their game records.
        	if(replay==replayMode.Live) { setNextPlayer(); }
        	break;
        case MOVE_DONESCORING:
        case MOVE_DONE:
         	doDone();

            break;
        case MOVE_RACK_BOARD:
           	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case Play:
        		case Play1:
        			G.Assert(pickedObject==null,"something is moving");
                    pickObject(getCell(m.source, m.from_col, m.from_row));
                    dropObject(getCell(GoId.BoardLocation,m.to_col,m.to_row)); 
                    setNextStateAfterDrop();
                    break;
        	}
        	break;
        case MOVE_ADD_BLACK:
	    	{	
	    	GoCell c = getCell(GoId.BoardLocation,m.to_col,m.to_row);
	    	if(c!=null)
	    	{
	    	discardPickedObject();
	    	pickObject(getCell(GoId.Black_Chip_Pool,m.from_col,m.from_row));
        	lastChip = pickedObject;
	    	dropObject(c);
	    	lastHit = c;
	    	}}
	    	break;
        case MOVE_ADD_WHITE:
	    	{	
	        GoCell c = getCell(GoId.BoardLocation,m.to_col,m.to_row);
	        if(c!=null)
	        {
           	G.Assert(c.topChip()==null,"not empty");
           	discardPickedObject();
	    	pickObject(getCell(GoId.White_Chip_Pool,m.from_col,m.from_row));
        	lastChip = pickedObject;
	    	dropObject(c);
	    	lastHit = c;
	        }}
	    	break;

        case MOVE_DROP_BLACK:
        	{	
        	GoCell c = getCell(GoId.BoardLocation,m.to_col,m.to_row);
        	if(c!=null)
        	{
        	GoChip top = c.topChip();
        	if(top!=null) { if(top!=GoChip.black) { G.print("Swap colors at ",c); }; c.removeTop(); }
        	
        	discardPickedObject();
        	pickObject(getCell(GoId.Black_Chip_Pool,m.from_col,m.from_row));
        	lastChip = pickedObject;
        	dropObject(c);
        	lastHit = c;
        	if(board_state!=GoState.Puzzle) { doDone(); }
        	}
        	}
        	break;
        case MOVE_DROP_WHITE:
	    	{	
	    	GoCell c = getCell(GoId.BoardLocation,m.to_col,m.to_row);
        	if(c!=null)
        	{
            GoChip top = c.topChip();
            if(top!=null) { if(top!=GoChip.white) { G.print("Swap colors at ",c); }; c.removeTop(); }

   	    	discardPickedObject();
	    	pickObject(getCell(GoId.White_Chip_Pool,m.from_col,m.from_row));
        	lastChip = pickedObject;
 	    	dropObject(c);
	    	lastHit = c;
        	if(board_state!=GoState.Puzzle) { doDone(); }
	    	}}
	    	break;
        case MOVE_ANNOTATE:
        	{
        		GoCell c = getCell(GoId.BoardLocation, m.to_col, m.to_row);
        		if(c.annotation==pickedObject)
        		{	c.annotation = null; 
        			c.annotationStep = 0;
        		}
        		else
        		{
        		c.annotation = pickedObject;
        		c.annotationStep = moveNumber;
        		}
        		acceptPlacement();
        	}
        	break;
        case MOVE_DROPB:
			{
			GoCell c = getCell(GoId.BoardLocation, m.to_col, m.to_row);
			
			if(pickedObject==null)
			{
				switch(board_state)
				{
				case Puzzle:
					
					if(lastChip==null) { lastChip = GoChip.chips[FIRST_PLAYER_INDEX]; }
					break;
				default:
					pickObject(rack[whoseTurn]);
					break;
				}
			}
        	G.Assert(pickedObject!=null,"something is moving");
			lastChip = pickedObject;
            if(isSource(c)) 
            	{ 
            	  unPickObject(); 

            	} 
            	else
            		{
            		dropObject(c);
             		lastHit = c;
            		setNextStateAfterDrop();
            		}
			}
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	if(isDest(getCell(m.from_col,m.from_row)))
        		{ unDropObject(); 
        		  setState(GoState.Play);
        		}
        	else 
        		{ pickObject(getCell(GoId.BoardLocation, m.from_col, m.from_row));
        		  switch(board_state)
        		  {	default: throw G.Error("Not expecting pickb in state %s",board_state);
        		  	case Play:
         		  	case Puzzle:
        		  		break;
        		  }
         		}
 
            break;
        case MOVE_KOMI:
        	setKomi(m.to_row/2.0);
        	break;
        case MOVE_DROP: // drop on chip pool;
            dropObject(getCell(m.source, m.to_col, m.to_row));
            setNextStateAfterDrop();

            break;

        case MOVE_PICK:
        	{
        	GoCell c = getCell(m.source, m.from_col, m.from_row);
            pickObject(c);
            setNextStateAfterPick();
        	}
            break;


        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(GoState.Play);
            break;

        case MOVE_RESIGN:
        	setState(unresign==null?GoState.Resign:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            // standardize "gameover" is not true
            setState(GoState.Puzzle);
 
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
    public boolean LegalToHitChips(GoCell cell)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
         case Confirm:
         case Draw:
         case Play: 
         case Play1:
        	return((pickedObject==null)
        			?(cell==rack[whoseTurn])
        			:isSource(cell));


		case Gameover:
		case Resign:
		case Score:
		case ConfirmHandicapState:
		case Score2:
			return(false);
        case Puzzle:
        	return(true);
        }
    }
  
    // true if it's legal to drop something  originating from fromCell on toCell
    public boolean LegalToDropOnBoard(GoCell fromCell,GoChip chip,GoCell toCell)
    {	if(toCell.topChip()==null)
    	{	if(toCell!=koLocation) {return(true);}
    		return(testCapturesForKo(toCell,GoChip.getChip(whoseTurn)));
    	}
    	return(false);
    }
    public boolean legalToHitBoard(GoCell cell,Hashtable<GoCell,GoChip>ghostChips)
    {	
        switch (board_state)
        {
 		case Play:
 		case Play1:
			return(LegalToDropOnBoard(pickedSourceStack.top(),pickedObject,cell));
 		case Score2:
 		case Score:
 			return(cell.topChip()!=null || ghostChips.get(cell)!=null);
		case Resign:
		case ConfirmHandicapState:
			return(false);
		case Confirm:
		case Draw:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);
		case Gameover:
			// pickedobject in this case should be an annotation
			return(pickedObject!=null);
        case Puzzle:
        	return((pickedObject==null)!=(cell.isEmpty()));
        }
    }
  public boolean canDropOn(GoCell cell)
  {		GoCell top = (pickedObject!=null) ? pickedSourceStack.top() : null;
  		return((pickedObject!=null)				// something moving
  			&&(top.onBoard 			// on the main board
  					? (cell!=top)	// dropping on the board, must be to a different cell 
  					: (cell==top))	// dropping in the rack, must be to the same cell
  				);
  }
  private int sweepCounter =0;
  public int incrementSweepCounter() { return(++sweepCounter); }


  private void captureGroup(GoCell c,int who,CellStack remember)
  {	removeChip(c);
  	if(robotBoard) { c.setKind(Kind.Dame,"was captured"); } 
  	captureStack[who].push(c);
  	if(remember!=null) { remember.push(c); }
	for(int direction = 0;direction<c.geometry.n; direction++)
	  {
		  GoCell next = c.exitTo(direction);
		  if(next!=null)
		  {	if((next.topChip()!=null) && (next.getSweepCounter()==c.getSweepCounter()))
		  	{ captureGroup(next,who,remember);
		  	}
		  }
	  }
  }
  private boolean doCaptures(GoCell c,GoChip top,int sweep)
  {	boolean captureOrEmpty = false;
  	if(c.getSweepCounter()!=sweep)
  	{
	c.setSweepCounter(sweep);
	for(int direction=0;direction<c.geometry.n;direction++)
	{
		GoCell next = c.exitTo(direction);
		if(next!=null)
		{
			GoChip ntop = next.topChip();
			if(ntop == top) 
			{
				captureOrEmpty |= doCaptures(next,ntop,sweep);
			}
			else if(ntop!=null)
			{
				if(!next.hasLiberties()) 
					{ captureGroup(next,AR.indexOf(playerChip, ntop),null); captureOrEmpty = true; }
			}
			else { captureOrEmpty = true; }
		}
	}
	}
  	return(captureOrEmpty);
  }
  private boolean testCapturesForKo(GoCell c,GoChip chip)
  {	int sz = 0;
  	for(int direction = c.geometry.n-1; direction>=0; direction--)
  		{
	  	GoCell adj = c.exitTo(direction);
	  	if(adj!=null)
	  		{ GoChip top = adj.topChip();
	  		  if((top!=null) && (top!=chip))
	  		  {	if(adj.countLiberties()<=1)
	  		  	{
	  			  sz += adj.groupSize();
	  			  if(sz>1) { return(true); }
	  		  	}
	  		}
	  		}
  		}
  	return(sz!=1);
  }

  private void doCaptures(GoCell c)
  {	sweepCounter++;
  	int next = nextPlayer[whoseTurn];
  	CellStack cap = captureStack[next];
  	int sz = cap.size();
  	GoChip top = c.topChip();
    if(!doCaptures(c,top,sweepCounter))
    	{ captureGroup(c,AR.indexOf(playerChip,top),null); 
    	}
    if(cap.size()-1==sz) 
    {
    	koLocation = cap.top();
    }
    else { koLocation = null; }
  }
  
  enum GroupStatus { None, Single, Multiple };
  
  public void addGhosts(Hashtable<GoCell,GoChip>g,int lvl,CellStack stack,GoChip chip)
  {	for(int i=lvl,lim=stack.size();  i<lim; i++)
  	{
	  g.put(stack.elementAt(i),chip);
  	}
	  
  }
  public Hashtable<GoCell,GoChip>getGhostCells()
  {
	  Hashtable<GoCell,GoChip>g = new Hashtable<GoCell,GoChip>();
	  addGhosts(g,ghostLevel[0],captureStack[0],GoChip.ghostChips[0]);
	  addGhosts(g,ghostLevel[1],captureStack[1],GoChip.ghostChips[1]);
	  return(g);
 }
 public void markMiai()
 {	// this complicated structure is an attempt to assign the miai in the best order,
	// it helps, but doesn't fix the whole problem.
	//
	for(int lim=emptyCells.size()-1; lim>=0;lim--) { emptyCells.elementAt(lim).miaiCounter = 0; }
    for(int thr=5;thr>=0;thr--)
    {	//G.print("Thr "+thr);
    	for(int lim = coloredGroups.size()-1; lim>=0; lim--)
    	{	 coloredGroups.elementAt(lim).assignMiai(thr); 
    	}
    }
 }
 // auto-fill spaces that have only 1 territory adjacent
 public GroupStack markEndgameAtari()
 {	boolean someChange = false;
 	boolean cutPhase = false;
 	for(int lim = coloredGroups.size()-1; lim>=0; lim--)
	  {	 
 	  SimpleGroup g = coloredGroups.elementAt(lim);
 	  g.atariFill = null; 
	  }
    GroupStack simple = null;
      do
      {
 	  do
 	  {
      someChange = false;
	  for(int lim = coloredGroups.size()-1; lim>=0; lim--)
	  {
		  SimpleGroup group = coloredGroups.elementAt(lim);
		  Kind kind = group.getKind();
		  switch(kind)
		  {
		  default: throw G.Error("not expecting %s",kind);
		  case SekiBlack:
		  case SekiWhite:
		  case DeadWhite:
		  case DeadBlack:
		  case RemovedBlack:
		  case RemovedWhite:
		  case ReservedForWhite:
		  case ReservedForBlack:
		  case FalseEye:
		  case FillBlack:		// rare, if we capture then fill
		  case FillWhite:		// rare, if we capture then fill
		  case WhiteTerritory:	// rare, if we capture then fill
		  case BlackTerritory:	// rare, if we capture then fill
			  break;
		  case SafeWhite:
		  case SafeBlack:
		  case White:
		  case Black:
			  if(group.atariFill==null)
			  {
			  GoCell atari = cutPhase ? group.endgameCut() : group.endgameAtari();
			  Kind fillKind = kind.getFillKind();
			  if(atari!=null)
				  { // note that under rare conditions atari may be a cell that is not empty.
				  	// this occurs in 005_016 where black has to capture and then fill where it
				    // captured.
			  		int kills = atari.killIfIsCapture(kind.chip,kind.getOppositeKind().getRemovedKind());	// do this before chipping
				    SimpleGroup g = chipFromGroup(atari,fillKind,
				    			cutPhase ? "avoid cut and kill"
				    					: "endgame atari fill");
				    group.atariFill = atari;
				    g.killedLiberties += kills; 
				    
			  		if(simple==null) { simple=new GroupStack(); }
			  		simple.addElement(g);
			  		someChange = true;
				  }
			  //else { if(killIfIsDead(group,null)) {  someChange = true; }}
			  }
		  }
	  }} while(someChange);
 	  cutPhase = !cutPhase;
      }
 	  while(cutPhase);
 	  return(simple);
 	 }

 public void unChipFromGroup(SimpleGroup g)
 {	SimpleGroup from = g.chippedFrom;
 	G.Assert(g.chippedFromKind!=null,"must be set");
 	g.setKind(g.chippedFromKind,"unchipped");
 	if(from!=null)
	  {	
		  for(int lim=g.size()-1; lim>=0; lim--)
			{	GoCell c = g.elementAt(lim);
				c.setGroup(from);
				from.addElement(c);
			}
	  }
 	else { emptyGroups.addElement(g); }
 }
  public SimpleGroup chipFromGroup(GoCell atari,Kind fillKind,String reason)
  {
	  SimpleGroup g = atari.getGroup();
	  	if(g.size()==1) 
	  	{ // transfer the group from the empty list to the atari list
	  	  emptyGroups.remove(g,false); 
	  	  g.chippedFromKind = g.getKind();
	  	  g.setKind(fillKind,reason);
	  	}
	  	else 
	  	{ // remove an element from the group and create a new single element group
	  	  g.remove(atari,false);
	  	  SimpleGroup oldGroup = g;

	  	  g = new SimpleGroup(this,fillKind,reason);
	  	  g.setChipped(oldGroup);
	  	  g.addElement(atari);
	  	  atari.setGroup(g);
	  	}

	  	return(g);
  }
  public void repairInitialKinds()
  {
	  
  	for(int lim=emptyGroups.size()-1; lim>=0; lim--)
        //for(int lim=0;lim<emptyGroups.size();lim++)
  	  {	SimpleGroup emptyGroup = emptyGroups.elementAt(lim);
  	    Kind kind = emptyGroup.getKind();

  	  switch(kind)
  	  {	default: break;
		case BlackTerritory:
			if((emptyGroup.size()<=2) 
					&& (canReachTerritory(emptyGroup,Kind.BlackTerritory)==null)
					&& canMakeTerritory(emptyGroup,Kind.WhiteTerritory)
						)
			{	// this tries to reclassify tiny "eyes" that can't be territory 
				// if they could plausibly be enemy territory and don't have friends.
				// one game where this happened is PUBLISHED-SUJI_04
				emptyGroup.setKind(Kind.Dame,"reclassify not territory");
			}
			break;
		case WhiteTerritory:
			if((emptyGroup.size()<=2)
					&& (canReachTerritory(emptyGroup,Kind.WhiteTerritory)==null)
					&& canMakeTerritory(emptyGroup,Kind.BlackTerritory)
					) 
			{	// this tries to reclassify tiny "eyes" that can't be territory 
				// if they could plausibly be enemy territory and don't have friends.
				// one game where this happened is PUBLISHED-SUJI_04
				emptyGroup.setKind(Kind.Dame,"reclassify not territory");
			}
  	  }}
  }
  // now try to classify some of the Dame groups as territory with dead stones inside
  public boolean markAsTerritory()
 	 {
	  boolean anyChange = false;
  	  boolean someChange;
  	  repairInitialKinds();
  	  do {
      someChange = false;
	  for(int lim=emptyGroups.size()-1; lim>=0; lim--)
      //for(int lim=0;lim<emptyGroups.size();lim++)
	  {	SimpleGroup emptyGroup = emptyGroups.elementAt(lim);
	    Kind kind = emptyGroup.getKind();

		switch(kind)
			{
			default:throw G.Error("Not expecting kind %s",kind);
			case BlackTerritory:
			case WhiteTerritory:
			case FalseEye:
				break;
			case WhiteSnapbackTerritory:
			case BlackSnapbackTerritory:
			case OutsideDame:
			case FillWhite:
			case FillBlack:
			case ReservedForWhite:
			case ReservedForBlack:
				break;
			case Empty:	// not really expected
				G.Error("not expecting %s",kind);
				break;
			case Dame:
			case BlackDame:
			case WhiteDame:
				{
				// this is where life and death comes in. A live black territory
				// may contain white stones, but is bounded by black stones. A white
				// territory may contain black stones, but is bounded by white stones
				SimpleGroup blackT = canReachTerritory(emptyGroup,Kind.BlackTerritory);	// white+empty expanse
				SimpleGroup whiteT = canReachTerritory(emptyGroup,Kind.WhiteTerritory);	// black+empty expanse
				if(blackT!=null && whiteT!=null)
					{	// appears to be adjacent to both safe white and safe black stones
					emptyGroup.setKind(Kind.OutsideDame,"black and white");
					}
				else if(null!=blackT && (null==whiteT)) 
				{	sweepCounter++;
					// appears to be adjacent to live black stones but not live white.
					// canMakeTerritory is partially heuristic
					if(canMakeTerritory(emptyGroup,Kind.BlackTerritory))
					{
					
					if(emptyGroup.killEmbeddedGroups(Kind.Black)) { someChange = anyChange = true; }
					}
					else { emptyGroup.setKind(Kind.BlackDame,"Black can fill"); }
				}
				else if((null!=whiteT) && (null==blackT))
				{	sweepCounter++;
					// appears to be adjacent to live black stones but not live white.
					// canMakeTerritory is partially heuristic
					if(canMakeTerritory(emptyGroup,Kind.WhiteTerritory))
						{
						if(emptyGroup.killEmbeddedGroups(Kind.White)) { someChange = anyChange = true; }
						}
					else { emptyGroup.setKind(Kind.WhiteDame,"White can fill"); }
				}
				else if((whiteT==null) && (blackT==null))
				{	// two groups locked in a race to capture
					boolean canBeWhite = canMakeTerritory(emptyGroup,Kind.WhiteTerritory);
					boolean canBeBlack = canMakeTerritory(emptyGroup,Kind.BlackTerritory);
					if(canBeWhite && !canBeBlack)
					{
						
						if(emptyGroup.killEmbeddedGroups(Kind.White)) { someChange = anyChange = true; }
					}
					else if(canBeBlack && !canBeWhite)
					{	
						if(emptyGroup.killEmbeddedGroups(Kind.Black)) { someChange = anyChange = true; }
					}
					// otherwise leave as plain dame
				}
				}
			}
	  	}
	  }  while(someChange);
  	  return(anyChange);
 	 }
  
  GroupStack coloredGroups = new GroupStack();
  GroupStack emptyGroups = new GroupStack();
  GroupStack whiteTerritoryGroups = new GroupStack();
  GroupStack blackTerritoryGroups = new GroupStack();
  
  public void placeStone(GoCell c,Kind source)
  {	
      c.addChip(source.fillChip);
      doCaptures(c);
   }
  

  /* classify all the empty cells as black or white territory
   * 
   */
  // 043_199 has a good snapback
  // ** situations where a playout will be necessary **
  // 004_015 black has a tricky endgame fill on the right
  //  005_016 black has to fill 2 while closing the lower right
  //  007_025 black has to fill 2 while closing the bottom center
  //  022_101 white has to connect inside the right hand group
  // 022_101 white has to connect at Q7
  // 022_101 
  // 14TH-GOSEI-CHALLNGER-SEMI-FINAL-2 black gets an extra point because there
  //  are two ways to compromise the eye at p11
  // 14TH-GOSEI-TOURMENT-RD-3-No.1 two missing points?
  //
  // 1986-MEIJIN-GAME-3 black has to disconnect at D1 as outside liberties are filled.
  //   also, white makes a seki if he connects late enough
  // 012_050 white has to capture at k9
  // 024_097 massively wrong life and death top-center white group
  // 14TH-GOSEI-CHALLENGER-FINAL big dead white group in lower right
  // 15TH-KISEI-(TOP)-1ST-RD-GAME-4 has a snapback
  // 14TH-MEIJIN-LEAGUE-GAME-15 has a good forced fill
  // 14TH-MEIJIN-LEAGUE-GAME-28 gets life wrong, could benefit from shape library
  // 14TH-GOSEI-TOURMENT-RD-3-No.4 black has to connect at N10
  // 15TH-KISEI-TITLE-MATCH-GAME-6 has a seki, needs the shape library
  // 14TH-GOSEI-TOURMENT-RD-1-No.12 white has to fill at A13
  // 15TH-GOSEI-CHALLNGER-SEMI-FINAL-1 has a chain of connections that should 
  //   make two connecting moves unnecessary.  Similarly 15TH-GOSEI-TOURMENT-RD-1-No.6
  //   has an eye at H13 that should be protected
  // 15TH-KISEI-(TOP)-SEMI-FINAL-1 black has to capture at G5

  public synchronized void classifyBoard()
  {	// this guarantees that the classification starts over from scratch.
	// it's necessary because the latter steps of the classification reclassify
	// and subdivide empty groups
	changeClock++;
	coloredGroups.clear();
	emptyGroups.clear();
	whiteTerritoryGroups.clear();
	blackTerritoryGroups.clear();
	exceptions = null;
	for(GoCell c=allCells; c!=null; c=c.next)
	{	GoChip top = c.topChip();
		SimpleGroup main = c.getGroup();
		if(top==null)
		{
		emptyGroups.pushNew(main);
		whiteTerritoryGroups.pushNew(c.getWhiteTerritory());
		blackTerritoryGroups.pushNew(c.getBlackTerritory());
		}
		else { coloredGroups.pushNew(main); }
	}
	coloredGroups.sort(false);
	// find the white and black groups, designate some as dead if it's really obvious
 	// also get the liberty size of each group
 	for(int lim=coloredGroups.size()-1; lim>=0; lim--)
 		{	SimpleGroup group = coloredGroups.elementAt(lim);
			  // group size > 6 because it's customary to leave 
			  // even numbers of single stone Kos on the board,
			  // and internal stones that kill a group should be
			  // consider as alive
 			  group.getConnections();	// find and cache connections;
			  if((group.countLiberties()<2)	// as a side effect, caches the liberties count
					  && (group.size()>6))
			  {
				  group.setKind(group.getKind()==Kind.Black?Kind.DeadBlack:Kind.DeadWhite,"atari");
			  }
		  }
  	
 	  // classify the empty spaces as black, white, or dame
	  for(int lim=emptyGroups.size()-1; lim>=0; lim--)
	  {
	    SimpleGroup gr = emptyGroups.elementAt(lim);
	    Kind kind = gr.classifyEmptyGroup();   	
	    gr.setKind(kind,"original"); 
	  }
 	 
	 //addTerritories();

 	 //
 	 // mark Dame groups as territory if possible.
 	 // this is partially a heuristic process
 	 // also sub classify dame as FillBlack, BlackDame and so on
 	 markAsTerritory();
 	 
 	 //
 	 // mark dame which can only be filled by one player because of self atari
 	 //
 	 markSelfAtari();
 	 //
 	 // assign outside connections. This hasn't worked out.
 	 //
 	 // markMiai();
 	 //
 	 // mark some endgame forced fills.  This eliminates the fake eyes.
 	 //
 	 markEndgameAtari();
 	 //
 	 // count eyes and designate safe stones.
 	 //
 	 addTerritories();
 	 //
 	 // game GO-bobc-ddyer-2020-10-20-0022 contains a seki in the upper-left
 	 // that can't be resolved correctly by the current algorithms. Since we
 	 // can't fix it, we had to have a way to mark the seki and score it correctly.
 	 // this lets you toggle from "marked dead" to "dead (removed)" then to "alive override"
 	 //
 	 for(GoCell c = allCells; c!=null; c=c.next) {
 		 if(c.safeOverride && (c.topChip()!=null))
 		 {
 			 SimpleGroup gr = c.getGroup();
 		 	 if(gr!=null) { 
 		 		 gr.setKind(gr.getKind().getSafeKind(),"Safe Override");
 		 	 }
 		 }
 	 }
 	 
 	 markSafeTerritories();
 	 //
 	 // mark things that don't have two eyes as dead
 	 //
 	 killOneEyed();
 	 //
 	 // second pass now that some dead are known
 	 //
 	 markAsTerritory();
 	 
 	 // 
 	 // after all else is filled, there can be double atari hiding
 	 //
 	 markDoubleAtari();
 	 
 	 // second pass
 	 markEndgameAtari();
 	 
 	 //
 	 // remove multiple captures that endgameatari allowed to escape.
 	 //
 	 markEndgameKills();

 	 //
 	 // remove some supfluous fill moves
 	 //
 	 removeOptionalFill();
  }
  
  //
  // in a few cases, multiple captures are present in "complete" games,
  // and everyone understands that they are inevitable.  The "endgame atari"
  // mechanism lets them escape, so we kill them at the end.
  //
  public void markEndgameKills()
  {	for(int lim=emptyCells.size()-1; lim>=0; lim--)
  	{	GoCell empty = emptyCells.elementAt(lim);
  		SimpleGroup emptyGroup = empty.getGroup();
  		if(emptyGroup.size()==1)
  		{	Kind emptyKind = emptyGroup.getKind();
  			switch(emptyKind)
  			{
  			case FillWhite:
  			case FillBlack:
  				GroupStack enemy = emptyGroup.adjacentGroups();
  				SimpleGroup cap1 = null;
  				SimpleGroup cap2 = null;
  				SimpleGroup cap3 = null;
  				int capsize = 0;
  				for(int elim = enemy.size()-1; elim>=0; elim--)
  				{
  					SimpleGroup adj = enemy.elementAt(elim);
  					Kind adjKind = adj.getKind();
  					switch(adjKind)
  					{
  					default: break;
  					case Black:
  					case White:
  					case SafeWhite:
  					case SafeBlack:
  					case SekiWhite:
  					case SekiBlack:
  						if( (adjKind.chip==emptyKind.fillChip) 
  								&&	adj.countLiberties()==1)
  						{	cap3 = cap2;
  							cap2 = cap1;
  							cap1 = adj;
  							capsize += adj.size();
  							
  						}
  					}
  					if(capsize>1)
  					{
  					if(cap1!=null) { cap1.setKind(adjKind.getRemovedKind(),"endgame capture"); }
  					if(cap2!=null) { cap2.setKind(adjKind.getRemovedKind(),"endgame capture"); }
  					if(cap3!=null) { cap3.setKind(adjKind.getRemovedKind(),"endgame capture"); }
					empty.setKind(adjKind.getOppositeKind(),"opposite fill");
  					}
  				}

  				break;
  			default: break;
  			}
  		}
  		
  	}
  }

  public void removeOptionalFill()
  {
	  for(int lim=emptyCells.size()-1; lim>=0; lim--)
	  {
		  GoCell e = emptyCells.elementAt(lim);
		  SimpleGroup gr = e.getGroup();
		  Kind ekind = gr.getKind();
		  switch(ekind)
		  {
		  default: break;
		  case FillBlack:
		  case FillWhite:
		  	{
		  		SimpleGroup chipped = gr.chippedFrom;
		  		if(chipped!=null)
		  		{
		  		Kind chippedKind = chipped.getKind();
		  		switch(chippedKind)
		  		{
		  		default: break;
		  		case WhiteTerritory:
		  		case BlackTerritory:
		  			if(chippedKind.fillChip!=ekind.fillChip)
		  			{	// pointless chip, rejoin
		  				gr.setKind(chippedKind,"reconnected");
		  				for(int direction=e.geometry.n-1; direction>=0; direction--)
		  				{
		  					GoCell next = e.exitTo(direction);
		  					if(next!=null)
		  					{
		  						Kind k = next.getKind();
		  						switch(k)
		  						{
		  						default: break;
		  						case BlackTerritory:
		  						case WhiteTerritory:
		  							if(next.topChip()==null) { break; }
									//$FALL-THROUGH$
		  						case RemovedBlack:
		  						case RemovedWhite:
		  							if(k.fillChip==chippedKind.fillChip)
		  							{	next.setKind(chippedKind.getSafeKind(),"resurrected");
		  							}
		  						}
		  					}
		  				}
		  			}
		  		}
		  		}
		  	}
		  	break;
		  case ReservedForBlack:
		  case ReservedForWhite:
			  SimpleGroup chipped = gr.chippedFrom;
			  if(chipped!=null) { gr.setKind(chipped.getKind(),"reconnected");}
		  }
	  }
  }
  public String checkForInconsistency(SimpleGroup trouble)
  {	  String messages = "";
	  for(int lim=coloredGroups.size()-1; lim>=0; lim--)
	  {	SimpleGroup gr = coloredGroups.elementAt(lim);
	  	Kind k = gr.getKind();
	  	switch(k)
	  	{
		case RemovedWhite:
		case RemovedBlack:
	  	case DeadWhite:
	  	case DeadBlack:
	  		{	
	  		GroupStack enemies = gr.adjacentGroups();
	  		for(int elim=enemies.size()-1; elim>=0; elim--)
	  		{
	  		SimpleGroup enemy = enemies.elementAt(elim);
	  		Kind ekind = enemy.getKind();
	  		switch(ekind)
	  			{
	  			case RemovedWhite:
	  			case RemovedBlack:
	  			case DeadWhite:
	  			case DeadBlack:
	  				messages += "both dead: " +gr+" "+ enemy +"\n";
	  				trouble.union(gr);
	  				trouble.union(enemy);
	  				break;
	  			case SekiWhite:
	  			case SekiBlack:
	  				if(gr.size()>=3)
	  				{
	  				messages += "dead adjacent to seki: " +gr+" "+ enemy +"\n";
	  				trouble.union(gr);
	  				trouble.union(enemy);
	  				}
	  				break;
	  			case SafeWhite:
	  			case SafeBlack:
	  			case White:
	  			case Black:
	  			case WhiteTerritory:
	  			case BlackTerritory:
	  			case FillWhite:
	  			case FillBlack:
	  				break;
	  			default: G.Error("not expecting %s",ekind);
	  			}
	  		}
	  		}
			{
			GroupStack empty = gr.adjacentEmptyGroups();
			for(int elim=empty.size()-1; elim>=0; elim--)
				{
				SimpleGroup egroup = empty.elementAt(elim);
				Kind ekind = egroup.getKind();
				switch(ekind)
				{
				default: throw G.Error("not expecting %s",ekind);
				case SafeWhite:
				case SafeBlack:
				case DeadWhite:
				case DeadBlack:
				case SekiWhite:
				case SekiBlack:
					break;
				case BlackDame:		// added in classify split
				case WhiteDame:		// added in classify split
				case OutsideDame:
				case Dame:
					break;
				case WhiteTerritory:
				case BlackTerritory:
				case FalseEye:
				case BlackSnapbackTerritory:
				case WhiteSnapbackTerritory:
				case FillWhite:
				case FillBlack:
				case ReservedForWhite:
				case ReservedForBlack:
					if(k.chip==ekind.fillChip) 
					{
						messages += "territory not settled "+gr+ " "+egroup + "\n";
		  				trouble.union(gr);
		  				trouble.union(egroup);
					}
				}
				}
			
			}
	  		break;
	  	case SafeWhite:
		case SafeBlack:
		case SekiWhite:
		case SekiBlack:
			{
			GroupStack empty = gr.adjacentEmptyGroups();
			for(int elim=empty.size()-1; elim>=0; elim--)
				{
				SimpleGroup egroup = empty.elementAt(elim);
				Kind ekind = egroup.getKind();
				switch(ekind)
				{
				default: throw G.Error("not expecting %s",ekind);
				case FillBlack:
				case FillWhite:
				case DeadWhite:
				case DeadBlack:
				case SekiBlack:
				case SekiWhite:
					break;
				case ReservedForWhite:
				case ReservedForBlack:
				case OutsideDame:
				case Dame:
				case WhiteDame:
				case BlackDame:
				case SafeBlack:
				case SafeWhite:
					break;
				case BlackSnapbackTerritory:
				case WhiteSnapbackTerritory:
				case WhiteTerritory:
				case BlackTerritory:
				case FalseEye:
					if(k.chip!=ekind.fillChip) 
					{
						messages += "territory not settled "+gr+ " "+egroup + "\n";
		  				trouble.union(gr);
		  				trouble.union(egroup);
					}
				}
				}
			
			}
			break;
		case White:
		case Black:
		case FillWhite:
		case FillBlack:
		case WhiteTerritory:
		case BlackTerritory:
			break;
	  	default: G.Error("not expecting %s",k);
	  	}
	  }
	  return(messages);
  }
  public void addTerritories()
  {
	  for(int lim=emptyGroups.size()-1; lim>=0; lim--)
	  {
		  SimpleGroup empty = emptyGroups.elementAt(lim);
		  Kind k = empty.getKind();
		  switch(k)
		  {
			  default: throw G.Error("not expecting %s",k);
				  
			  case WhiteSnapbackTerritory:
			  case WhiteTerritory:
			  {	GoCell c = empty.top();
			  SimpleGroup ter = c.getWhiteTerritory();
			  ter.addTerritories();
			  if (ter.size()<4)
			  { SimpleGroup ter2 = c.getBlackTerritory();
			    if(ter2.size()<7)
			    {	// 45TH-HONINBO-LEAGUE-GAME-5 
			    	// This is intended to classify small groups with one eye
			    	// as territories of the surrounding group
			    	ter2.addTerritories();
			    }
			  }
		  
		  }
		
				  break;
			  case BlackSnapbackTerritory:
			  case BlackTerritory:
			  {	GoCell c = empty.top();
				  SimpleGroup ter = c.getBlackTerritory();
				  ter.addTerritories();
				  if (ter.size()<4)
				  { SimpleGroup ter2 = c.getWhiteTerritory();
				    if(ter2.size()<7)
				    {
				    	ter2.addTerritories();
				    }
				  }
			  
			  }
			
				  break;
			  case BlackDame:
			  case WhiteDame:
			  case Dame:
			  case ReservedForWhite:
			  case ReservedForBlack:
			  		{
			  		// if an empty space was classified as dame, make a guess if it's black or white now.
			  		SimpleGroup white = empty.top().getWhiteTerritory();
			  		SimpleGroup black = empty.top().getBlackTerritory();
			  		int whiteSz = white.size();
			  		int blackSz = black.size();
			  		
			  		if(whiteSz<blackSz) 
			  			{ white.addTerritories(); }
			  		else if(blackSz<whiteSz)
			  			{ black.addTerritories(); }
			  		else if(whiteSz<=4)	// too small to be a seki
			  		{	
			  			white.addTerritories();
			  		}
			  		else if(blackSz<=4)
			  		{
			  			black.addTerritories();
			  		}}
			  		break;
			  case FalseEye:
			  case OutsideDame:
			  case FillBlack:
			  case FillWhite:
				  break;
		  }}
	  for(int lim=coloredGroups.size()-1; lim>=0; lim--)
		  {
		  SimpleGroup gr = coloredGroups.elementAt(lim);
		  Kind k = gr.getKind();
		  switch(k)
		  {
		  case RemovedWhite:
		  case RemovedBlack:
			  gr.addTerritories();
			  break;
		  default: break;
		  }
		  }
  }
  public void markSafeTerritories()
  {

	  // now that we've collected the maximal set of territories, remove those territories
	  // that contain other territories, that is, live group inside
	  for(int lim=coloredGroups.size()-1; lim>=0; lim--)
	  {
		  coloredGroups.elementAt(lim).removeComplexTerritories();
	  }
	  for(int lim=coloredGroups.size()-1; lim>=0; lim--)
	  {
		  SimpleGroup gr = coloredGroups.elementAt(lim);
		  Kind kind = gr.getKind();
		  switch(kind)
		  {
		  default: throw G.Error("not expecting %s",kind);
		  case SekiBlack:
		  case SekiWhite:
		  case SafeWhite:
		  case SafeBlack:
		  case DeadWhite:
		  case DeadBlack:
		  case RemovedWhite:
		  case RemovedBlack:
		  case ReservedForWhite:
		  case ReservedForBlack:
		  case FillBlack:
		  case FillWhite:
			  break;
		  case White:
		  case Black:
		  {	Kind k = gr.classifySafety();
	  	    gr.reClassifyAsSafe(k);
		  }
		  }}
	  
		for(int lim=coloredGroups.size()-1; lim>=0; lim--)
		  {
			  SimpleGroup gr = coloredGroups.elementAt(lim);
			  Kind kind = gr.getKind();
			  switch(kind)
			  {
			  default: throw G.Error("not expecting %s",kind);
			  case SafeWhite:
			  case SafeBlack:
			  case DeadWhite:
			  case DeadBlack:
			  case RemovedWhite:
			  case RemovedBlack:
			  case SekiWhite:
			  case SekiBlack:
			  case ReservedForWhite:
			  case ReservedForBlack:
			  case FillBlack:
			  case FillWhite:
				  break;
			  case White:
			  case Black:
				  sweepCounter++;
				  SimpleGroup sa = gr.canReachSafeGroup(sweepCounter);
				  if(sa!=null)
				  {
					  gr.setKind(sa.getKind(),"Connect to safe");
				  }
			  }
			  
		  }
  }
  public void killOneEyed()
  {
	  for(int lim=coloredGroups.size()-1; lim>=0; lim--)
	  {
		  SimpleGroup gr = coloredGroups.elementAt(lim);
		  Kind kind = gr.getKind();
		  GoCell seed = gr.top();
		  switch(kind)
		  {
		  default: throw G.Error("not expecting %s",kind);
		  case DeadWhite:
		  case DeadBlack:
		  case RemovedWhite:
		  case RemovedBlack:
		  case SafeWhite:
		  case SafeBlack:
		  case SekiBlack:
		  case SekiWhite:
		  case FillBlack:
		  case FillWhite:
		  case ReservedForWhite:
		  case ReservedForBlack:
		  case WhiteTerritory:	// unnecessary fill becomes territory
		  case BlackTerritory:
			  break;
		  case Black:
			  {
				 SimpleGroup terr = seed.getWhiteTerritory();
				 if(terr.size()>6)
				 	{
					  terr.killEmbeddedGroups();
				 	}
				 }
			  break;
		  case White:
			  {
				  SimpleGroup terr = seed.getBlackTerritory();
				  if(terr.size()>6)
				  {	//gr.setKind(Kind.DeadWhite,"endgame no safety");
					  terr.killEmbeddedGroups();
				  }
			  }
			  break;
		  }
	  }
  }
  public String classificationString()
  {	  classifyBoard();
  	  Utf8OutputStream stream = new Utf8OutputStream();
  	  PrintStream out = Utf8Printer.getPrinter(stream);
	  
		  for(int row=boardRows;row>=1;row--)
		  { 
			  for(int col=0;col<boardColumns;col++)
			  {
			  out.write(getCell((char)('A'+col),row).getKind().ccode);
		  }
		  out.write('\n');
	  }
	  out.flush();
	  String str = stream.toString();
	  out.close();
	  return(str);
  }
  SimpleGroup exceptions = null;
  public String classificationExceptions()
  {	  exceptions = new SimpleGroup(this,Kind.Empty,"any exceptions");
	  return(checkForInconsistency(exceptions));
  }
  public String compareCS(String oldCS,String newCS)
  {	int diffs = 0;
  	Utf8OutputStream stream = new Utf8OutputStream();
  	PrintStream out = Utf8Printer.getPrinter(stream);
	  if(oldCS!=null)
	  {	int idx = 0;
		  for(int row=boardRows;row>=1;row--)
			{ for(int col=0;col<boardColumns;col++)
			  {
				char oldCode = oldCS.charAt(idx);
			  	char newCode = newCS.charAt(idx);
			  	idx++;
			  	if(!(oldCode==newCode))
			  	{	diffs++;
			  		out.print((char)('A'+col));
			  		out.print(""+row);
			  		out.print(" was ");
			  		out.print(Kind.getKind(oldCode).toString());
			  		out.print(" is ");
			  		out.print(Kind.getKind(newCode).toString());
			  		out.print('\n');
			  	}
			  }
			  idx++;
		  }
	  if(diffs==0) { out.print("CS Compared ok\n"); } 
	  }
	  out.flush();
	  String str = stream.toString();
	  out.close();
	  return(str);
  }

  public void markSelfAtari()
  {	  for(int lim=emptyCells.size()-1; lim>=0; lim--)
  		{	GoCell c = emptyCells.elementAt(lim);
	  		SimpleGroup g = c.getGroup();
	  		switch(g.getKind())
	  		{
	  		case OutsideDame:
	  		{	//Guarded cells are outside dame where a pitched
	  			//stone would be atari
	  			Kind gkind = c.isGuardedCell(null);
	  			switch(gkind)
	  			{
	  			case Empty:
	  				break;
	  				
				case ReservedForBlack:
	  				if(c.killIfMultipleCapture(GoChip.black,Kind.RemovedWhite)) { gkind = Kind.FillBlack; }
	  				chipFromGroup(c,gkind,"white would self atari");
	  				break;
				case FillBlack:
	  				c.killIfMultipleCapture(GoChip.black,Kind.RemovedWhite);
	  				chipFromGroup(c,gkind,"white would self atari");
	  				break;
	  			case ReservedForWhite:
	  				if(c.killIfMultipleCapture(GoChip.white,Kind.RemovedBlack))	{	gkind = Kind.FillWhite;		}
	  				chipFromGroup(c,gkind,"black would self atari");
	  				break;
	  			case FillWhite:
	  				c.killIfMultipleCapture(GoChip.white,Kind.RemovedBlack);
	  				chipFromGroup(c,gkind,"black would self atari");
	  				break;
	  			default: G.Error("not expecting %s",gkind);
	  			}
	  			//
	  			// this kills stones that are atari but not enclosed, as happens 
	  			// sometimes in real games.
	  			// 14TH-GOSEI-TOURMENT-RD-2-No.5 white gains 2 points from an unusual capture
	  			//  likewise 15TH-MEIJIN-LEAGUE-GAME-16 for black in center
	  			//
	  			for(int direction=c.geometry.n-1; direction>=0; direction--)
	  			{
	  				GoCell adj = c.exitTo(direction);
	  				if(adj!=null)
	  				{	GoChip top = adj.topChip();
	  					if(top!=null)
	  					{
	  						SimpleGroup adjGroup = adj.getGroup();
	  						Kind k = adjGroup.getKind();
	  						switch(k)
	  						{
	  						case Black:
	  						case White:
		  						if(adjGroup.countLiberties()<=1)
		  								
		  						{
		  							adjGroup.setKind(top==GoChip.black 
		  										? Kind.DeadBlack
		  										: Kind.DeadWhite,
		  									"atari at endgame");
		  						}
		  						break;
	  						case DeadWhite:
	  						case DeadBlack:
	  						case RemovedBlack:
	  						case RemovedWhite:
	  						case SekiBlack:
	  						case SekiWhite:
	  						case SafeWhite:
	  						case SafeBlack:
	  							break;
	  							default: G.Error("not expecting %s",k);
	  						}
	  					}
	  				}
	  			}
	  		}
	  			break;
	  		default: break;
	  		}
	  		
  		}
	for(int lim=emptyGroups.size()-1; lim>=0; lim--)
	  {
		  emptyGroups.elementAt(lim).markSelfAtari();
	  }

  }
  
  //
  // this is officially not ready, and maybe never will be.
  // there are too many ways to fill the dame and exactly 
  // which stones are prone to double atari after all the 
  // filling is hard to guarantee.  This code produces a
  // bunch of false positives.
  //
  public void markDoubleAtari()
  {
	  for(int lim=emptyCells.size()-1; lim>=0; lim-- )
	  {
		  GoCell c = emptyCells.elementAt(lim);
		  if(markDoubleAtari(c,Kind.BlackTerritory,GoChip.white))
		  {
			  chipFromGroup(c,Kind.FillBlack,"prevent double atari");
		  }
		  else if(markDoubleAtari(c,Kind.WhiteTerritory,GoChip.black))
		  {
			  chipFromGroup(c,Kind.FillWhite,"prevent double atari");
		  }

	  }
  }
	
  public boolean markDoubleAtari(GoCell c,Kind k,GoChip color)
  {		int libs = 0;
		int ndanger = 0;
		SimpleGroup danger = null;
		SimpleGroup cgr= c.getGroup();
		Kind cKind = cgr.getKind();
		if(cKind==k)
		{	if(c.countAvailableLiberties(cKind.getOppositeKind())<2) 
				{ return(false); 
				}
			for(int direction=c.geometry.n-1; direction>=0; direction--)
			{
			GoCell next = c.exitTo(direction);
			if(next!=null)
				{	
				SimpleGroup gr = next.getGroup();
				
				GoChip top = next.topChip();
				if(top==null) { libs++; }
				else if(top==color) { libs+= gr.countLiberties()-1; }
				else if((gr!=danger) && gr.countAdjacentSafeLiberties(incrementSweepCounter(),c,ConnectCode.Double)<2)
				{	ndanger++;
					danger = gr;
				}
				}
			}
			return((libs>=2) && (ndanger>=2));
		}
		return(false);
	}
  double scoreEstimateForPlayer(int pl)
  {
	  if(board_state==GoState.Gameover) {
		  return finalTerritory[pl];
	  }
	  return territoryForPlayer(pl);
  }
  double territoryForPlayer(int pl)
  {		int p = getColorMap()[pl];
  		double n = (p==0) ? 0 : komi;
  		Kind target = (p==0) ? Kind.BlackTerritory : Kind.WhiteTerritory;
  		Kind fillKind = (p==0) ? Kind.FillBlack : Kind.FillWhite;
  		Kind deadKind = (p==0) ? Kind.DeadBlack : Kind.DeadWhite;
  		Kind removedKind = (p==0) ? Kind.RemovedBlack : Kind.RemovedWhite;
  		for(GoCell c = allCells; c!=null; c=c.next)
  		{	 			
			Kind kind = c.getKind();
			switch(kind)
			{
			default: throw G.Error("not expecting %s",kind);
			case ReservedForWhite:
			case ReservedForBlack:
			case FillWhite:
			case FillBlack:
				// in rare circumstances, a fill space is already occupied.
				// this means it was captured and then filled in endgame.
				// see game 005_016
				if(c.topChip()!=null)
				{
					n += (kind==fillKind) ? 1 : 0 ;// one point for the captive, but no points for the space
				}
				break;
			case Empty:
			case Dame:
			case OutsideDame:
			case BlackDame:
			case WhiteDame:
			case White:
			case Black:
			case SafeWhite:
			case SafeBlack:
			case SekiBlack:
			case SekiWhite:
			case FalseEye:
				break;
			case RemovedBlack:
			case RemovedWhite:
				n += (kind==removedKind) ? -1 : +1 ;// point for the space plus the captive
				break;
			case DeadBlack:
			case DeadWhite:
				n += (kind==deadKind) ? -1 : +1 ;// point for the space plus the captive
				break;
			case WhiteSnapbackTerritory:
				if(Kind.WhiteTerritory==target) 
				{ n++;
				}
				break;
			case BlackSnapbackTerritory:
				if(Kind.BlackTerritory==target) 
				{ n++;
				}
				break;
			case WhiteTerritory:
			case BlackTerritory:
				if(kind==target) 
				{ n++;
				}
				break;
			}
  				 			
  		}
  		return(n-captureStack[p].size());
  }
  
 
  //
  // sweep and return true if territory kind is encountered
  //
  private SimpleGroup canReachTerritory(SimpleGroup emptyGroup,Kind kind)
  {	  sweepCounter++;
  	  SimpleGroup bound = emptyGroup.canReachTerritory(sweepCounter,kind,null);
  	  return(bound);
  }


  private boolean canMakeTerritory(SimpleGroup emptyGroup,Kind kind)
  {	sweepCounter++;
  	// if any of the boundaries seem to be insecure, this won't be a reliable territory
  	G.Assert(kind.isSafeTerritory(),"unsupported kind");
 
  	Kind mixedKind = (kind==Kind.BlackTerritory) ? Kind.WhiteAndEmpty : Kind.BlackAndEmpty;

	SimpleGroup mixedGroup = mixedKind==Kind.WhiteAndEmpty
				? emptyGroup.top().getBlackTerritory()
				: emptyGroup.top().getWhiteTerritory();
	return(mixedGroup.hCanMakeTerritory(emptyGroup));

  }


  
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(GoMovespec m)
    {	int mySize = captureStack[whoseTurn].size();
    	int next = nextPlayer[whoseTurn];
    	int me = whoseTurn;
    	int hisSize = captureStack[next].size();
    	int who=whoseTurn;
    	robotBoard = true;
		robotIStack.push(mySize);
    	robotIStack.push(hisSize);
    	robotOStack.push(koLocation);
    	//G.print("R "+m);
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        if (Execute(m,replayMode.Replay))
        {	if(captureStack[next].size()-10>hisSize)
        		{ // capturing more than 10 ends the game
        		setGameOver(false,true); 
        		}
        	else if((captureStack[me].size()-mySize)>0)
        		{ // suicide ends the game not in our favor
        			setGameOver(true,false); 
        		}
            if (DoneState())
            	{
                doDone();
            	}
            //nextMoves = null;
            //nextMoves = GetListOfMoves();
            //if(nextMoves.size()==0)
            //    {
            //   	setGameOver(WinForPlayerNow(whoseTurn),WinForPlayerNow(nextPlayer[whoseTurn]));               	
            //    }
         
        }
        else
        {
        	G.Error("Execute failed for %s",m);
        }
        if(whoseTurn==who) 
        	{ G.Error("Whoseturn didn't change %s",m); 
        	}
    }
    
   private CommonMoveStack  nextMoves = null;
   
   private void restoreCaptures(int lvl,CellStack from,GoChip ch)
   {
	   while(from.size()>lvl)
	   {
		   GoCell c = from.pop();
		   addChip(c,ch);
	   }
   }
   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(GoMovespec m)
    {	//G.print("U "+m);
    	koLocation = robotOStack.pop();
    	int hissz = robotIStack.pop();
    	int mysiz = robotIStack.pop();
    	int him = whoseTurn;
    	int me = nextPlayer[whoseTurn];
    	if(hissz<captureStack[him].size()) { restoreCaptures(hissz,captureStack[him],GoChip.chips[him]); }
    	if(mysiz<captureStack[me].size()) { restoreCaptures(mysiz,captureStack[me],GoChip.chips[me]); }

    	switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;
        case MOVE_DROP_BLACK:
        case MOVE_DROP_WHITE:
           	{
        	GoCell c = getCell(m.to_col,m.to_row);
        	if(c.topChip()!=null) { removeChip(c); }
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(GoState.Play);
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
 CellStack endgameMoves(GoChip who)
 {	CellStack moves = new CellStack();
	 for(int lim=emptyCells.size()-1; lim>=0; lim--)
	 {
		 GoCell c = emptyCells.elementAt(lim);
		 switch(c.getKind())
		 {
		 case OutsideDame:
			 moves.push(c);
			 break;
		 case ReservedForWhite:
		 case FillWhite:	
			 if(who==GoChip.white) { moves.push(c); }
			 break;
		 case ReservedForBlack:
		 case FillBlack:
			 if(who==GoChip.black) { moves.push(c); }
			 break;
		 default: break;
		 }
	 }
	 return(moves);
 }
 CommonMoveStack  GetListOfMoves()
 {	
 	CommonMoveStack all = nextMoves;
 	if(all!=null) { nextMoves=null; return(all); }
 	all = new CommonMoveStack(); 
 	for(int lim = emptyCells.size()-1; lim>=0; lim--)
	{
 	GoCell c = emptyCells.elementAt(lim);
	if(c!=koLocation)
		{
 		switch(c.getKind())
		{

		default:
			break;
		case BlackTerritory:
		case WhiteTerritory:
		case WhiteSnapbackTerritory:
		case BlackSnapbackTerritory:

			break;
		case Dame:
		case OutsideDame:
		case BlackDame:
		case WhiteDame:
			GoMovespec mp = new GoMovespec(PlainMove[whoseTurn],c.col,c.row-boardRows-1,whoseTurn);
			GoCell nc = getCell(mp.to_col,c.row);
			G.Assert(nc==c,"same place");
			all.push(mp);
		}
		}
 	}
 	all.push(new GoMovespec(MOVE_PASS,whoseTurn));
 	return(all);
 }
 
 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(Character.isDigit(txt.charAt(0)))
	 	{ 	if(txt.length()>1) { xpos -= cellsize/4; }
	 		xpos -= displayParameters.CELLSIZE/4;
	 	}
 	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
 }

}

/**
bad komi : <sgf_game j933>
territory not settled <group 1 <goban.GoCell R11=W> DeadWhite atari at endgame> <group 1 <goban.GoCell R12=> OptionalFillWhite black self atari>

new-stars-Hayashi-Yutaro-9-dan-19th-N-Kiin-Championship has a black+empty group that incorporates a lot of live black stones.
If split, it would be fine.


*/
