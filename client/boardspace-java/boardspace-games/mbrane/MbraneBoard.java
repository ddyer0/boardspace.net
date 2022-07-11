package mbrane;

import static mbrane.Mbranemovespec.*;
import java.awt.Color;
import java.util.*;
import lib.*;
import lib.Random;
import online.game.*;

/**
 * MbraneBoard knows all about the game of Hex, which is played
 * on a hexagonal board. It gets a lot of logistic support from 
 * common.hexBoard, which knows about the coordinate system.  
 * 
 * This class doesn't do any graphics or know about anything graphical, 
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

class MbraneBoard extends rectBoard<MbraneCell> implements BoardProtocol,MbraneConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
	
    static final String[] MbraneGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
 	// all placements in all three positions.
	static final int AllSudokuPlacements = (0x1ff | (0x1ff<<9) | (0x1ff<<18));

	MbraneVariation variation = MbraneVariation.Mbrane;
	private MbraneState board_state = MbraneState.Puzzle;	
	private MbraneState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	public MbraneState getState() { return(board_state); }
	public CellStack empty = new CellStack();
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(MbraneState st) 
	{ 	unresign = (st==MbraneState.Resign)?board_state:null;
		board_state = st;
		G.Assert( (st!=MbraneState.Confirm)||(pickedObject==null), "shouldn't be confirm state");
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
    MbraneColor reserveColor = MbraneColor.Red;
    
    public MbraneColor reserveColor()
    {
    	switch(board_state)
    	{
    	case Puzzle:	
    		if(reserveColor!=null) { return(reserveColor); }
			//$FALL-THROUGH$
		default: return(getPlayerColor(whoseTurn));
    	}
    }
    
	// get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public MbraneColor playerColor[] = { MbraneColor.Red,MbraneColor.Black};
	public MbraneColor getPlayerColor(int p) { return(playerColor[getColorMap()[p]]); }
	
	public MbraneChip getPlayerChip(int p) 
	{ MbraneColor color = getPlayerColor(p);
	  return(MbraneChip.getChip(color,0,0)); 
	}
	
	public MbraneChip getCurrentPlayerChip() { return(getPlayerChip(whoseTurn)); }

// this is required even if it is meaningless for this game, but possibly important
// in other games.  When a draw by repetition is detected, this function is called.
// the game should have a "draw pending" state and enter it now, pending confirmation
// by the user clicking on done.   If this mechanism is triggered unexpectedly, it
// is probably because the move editing in "editHistory" is not removing last-move
// dithering by the user, or the "Digest()" method is not returning unique results
// other parts of this mechanism: the Viewer ought to have a "repRect" and call
// DrawRepRect to warn the user that repetitions have been seen.
	public void SetDrawState() {throw G.Error("not expected"); };	
	CellStack animationStack = new CellStack();
    int chips_on_board = 0;			// number of chips currently on the board

    private boolean swapped = false;
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public MbraneChip pickedObject = null;
    public MbraneChip lastPicked = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    MbraneCell reserve[] = new MbraneCell[9];
    private MbraneState resetState = MbraneState.Puzzle; 
    public MbraneChip lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public MbraneCell newcell(char c,int r)
	{	return(new MbraneCell(MbraneId.BoardLocation,c,r));
	}
	
	// constructor 
    public MbraneBoard(String init,int players,long key,int[]map,int rev) // default constructor
    {	setColorMap(map);
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = MbraneGRIDSTYLE;
        Random r = new Random(1342356);
        for(int i=0;i<reserve.length;i++)
        {	
        	reserve[i] = new MbraneCell(r,MbraneId.Reserve_Pool,i);
        }
        doInit(init,key,players,rev); // do the initialization 
    }
    
    public String gameType() { return(gametype+" "+players_in_game+" "+randomKey+" "+revision); }
    

    public void doInit(String gtype,long key)
    {
    	StringTokenizer tok = new StringTokenizer(gtype);
    	String typ = tok.nextToken();
    	int np = tok.hasMoreTokens() ? G.IntToken(tok) : players_in_game;
    	long ran = tok.hasMoreTokens() ? G.IntToken(tok) : key;
    	int rev = tok.hasMoreTokens() ? G.IntToken(tok) : revision;
    	doInit(typ,ran,np,rev);
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int players,int rev)
    {	randomKey = key;
   		adjustRevision(rev);
    	players_in_game = players;
		setState(MbraneState.Puzzle);
		variation = MbraneVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		gametype = gtype;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case MbraneSimple:
		case Mbrane:
			reInitBoard(9,9);
		}
		
		for(int i=0;i<reserve.length;i++)
		{	reserve[i].reInit();
			for(int j=0;j<9;j++) 
				{ reserve[i].addChip(MbraneChip.getChip(MbraneColor.Red,i,j)); 
				}
		}
		
 		
		robotCell.clear();
	    playerColor[0] = MbraneColor.Red;
	    playerColor[1] = MbraneColor.Black;
	    whoseTurn = FIRST_PLAYER_INDEX;
	    reserveColor = MbraneColor.Red;
	    chips_on_board = 0;
	    droppedDestStack.clear();
	    pickedSourceStack.clear();
	    stateStack.clear();
	    AR.setValue(resolved,false);
	    AR.setValue(zoneScore,0);
	    
	    pickedObject = null;
	    resetState = null;
	    lastDroppedObject = null;
	    empty.clear();
	    // set the initial contents of the board to all empty cells
		for(MbraneCell c = allCells; c!=null; c=c.next) { c.reInit(); empty.push(c); }
		    
        animationStack.clear();
        swapped = false;
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }

    /** create a copy of this board */
    public MbraneBoard cloneBoard() 
	{ MbraneBoard dup = new MbraneBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((MbraneBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(MbraneBoard from_b)
    {
        super.copyFrom(from_b);
        chips_on_board = from_b.chips_on_board;
        robotState.copyFrom(from_b.robotState);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        reserveColor = from_b.reserveColor;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        copyFrom(reserve,from_b.reserve);
        AR.copy(playerColor,from_b.playerColor);
        stateStack.copyFrom(from_b.stateStack);
        AR.copy(resolved,from_b.resolved);
        AR.copy(zoneScore,from_b.zoneScore);
        getCell(empty,from_b.empty);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        lastPicked = null; 
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((MbraneBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(MbraneBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(reserveColor==from_b.reserveColor,"reserve color mismatch");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(chips_on_board == from_b.chips_on_board,"chips_on_board mismatch");
        G.Assert(sameCells(reserve,from_b.reserve),"reserve mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedsourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(AR.sameArrayContents(resolved,from_b.resolved),"resolved mismatch");
        G.Assert(AR.sameArrayContents(zoneScore, from_b.zoneScore),"zonescore mismatch");
        G.Assert(AR.sameArrayContents(playerColor, from_b.playerColor),"playercolor mismatch");
        G.Assert(empty.size()==from_b.empty.size(), "empty cells mismatch");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Digest matches");

    }

    /** 
     * Digest produces a 64 bit hash of the game state.  This is used in many different
     * ways to identify "same" board states.  Some are relevant to the ordinary operation
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
        // the basic digestion technique is to xor a bunch of random numbers. 
    	// many object have an associated unique random number, including "chip" and "cell"
    	// derivatives.  If the same object is digested more than once (ie; once as a chip
    	// in play, and once as the chip currently "picked up", then it must be given a
    	// different identity for the second use.
        //
        Random r = new Random(64 * 1000); // init the random number generator
        long v = super.Digest(r);
		// many games will want to digest pickedSource too
		// v ^= cell.Digest(r,pickedSource);
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,revision);
		v ^= Digest(r,reserve);
		v ^= Digest(r,resolved);
		v ^= Digest(r,playerColor[0].ordinal());
		v ^= Digest(r,empty.size());
		v ^= r.nextLong()*(reserveColor.ordinal()*100+board_state.ordinal()*10+whoseTurn);
        return (v);
    }



    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer(replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Move not complete, can't change the current player");
        case Puzzle:
        case Score:
            break;
        case Play:
        case PlayNoResolve:
        case PlayOrSwap:
        	// some damaged games have 2 dones in a row
        	if(replay==replayMode.Live) { throw G.Error("Move not complete, can't change the current player"); }
			//$FALL-THROUGH$
		case ConfirmSwap:
		case ConfirmPlay:
		case ConfirmProposeResolution:
		case ConfirmScore:
        case Confirm:
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
    {	return(board_state.doneState());
    }
    // this is the default, so we don't need it explicitly here.
    // but games with complex "rearrange" states might want to be
    // more selecteive.  This determines if the current board digest is added
    // to the repetition detection machinery.
    public boolean DigestState()
    {	
    	return(board_state.digestState());
    }



    public boolean gameOverNow() { return(board_state.GameOver()); }

    public boolean winForPlayerNow(int player)
    {  	
    	return(win[player]);
    }

    int zoneCenters[][] = {
    		{'B',2},
    		{'E',2},
    		{'H',2},
    		{'B',5},
    		{'E',5},
    		{'H',5},
    		{'B',8},
    		{'E',8},
    		{'H',8},
    };
    
    public double scoreZone(int zonen)
    {	switch(variation)
    	{
    	default:
    	case Mbrane: 
    		return(currentScore(zonen));
     	case MbraneSimple:
    		int v = centerScore(zonen,MbraneColor.Red);
    		return(v);
    	}
    }

    boolean resolved[] = new boolean[9];
    double zoneScore[] = new double[9];
    
    // return true if scoring is complete
    public boolean doScore(int resolveSteps)
    {	boolean allresolved = false;
    	while (resolveSteps>=0 && !(allresolved = scoreStep(resolveSteps>0))) { resolveSteps--; };
    	return(allresolved);
    }
    public double currentScore(int zonen)
    {
    	scoreStep(false);
    	return(zoneScore[zonen]);
    }
    private boolean scoreStep(boolean change)
    {	
    	double maxval=0;
    	int maxidx=-1;
    	for(int lim=zoneScore.length-1; lim>=0;lim--)
    	{
    		if(!resolved[lim]) 
    		{ double val = centerScore(lim,MbraneColor.Red)+edgeScore(lim,MbraneColor.Red);
    		  double aval = Math.abs(val);
    		  zoneScore[lim] = val;
    		  if(maxidx<0 ||(aval)>maxval)
    		  {
    			  maxval = aval;
    			  maxidx = lim;
    		  }
    		}
    	}
    	if(change && maxidx>=0)
    	{for(int lim=zoneScore.length-1; lim>=0;lim--)
    	{
    		if(!resolved[lim] && (Math.abs(zoneScore[lim])==maxval))
    		{
    			resolved[lim]=true;
    			if(zoneScore[lim]!=0)
    				{changeTiles(lim,(zoneScore[lim]>0)?MbraneColor.Red:MbraneColor.Black);
    				}
    		}
    	}}
    	return(maxidx<0);
    }
    
    // change the color of all tiles in a zone
    private void changeTiles(int zonen,MbraneColor color)
    {	int zone[] = zoneCenters[zonen];
    	for(int row=-1;row<=1;row++)
    	{
    		for(int col=-1;col<=1;col++) 
    		{	MbraneCell rowCell = getCell((char)(zone[0]+col),row+zone[1]);
    			MbraneChip top = rowCell.topChip();
    			if(top!=null && top.color!=color) 
    				{ rowCell.removeTop();
    				  rowCell.addChip(MbraneChip.getChip(color,top.value));
    				  robotCell.push(rowCell);
       				}
    		}
    	} 	
    }
    private int centerScore(int zonen,MbraneColor color)
    {	int zone[] = zoneCenters[zonen];
    	return (centerScore((char)zone[0],zone[1],color));
    }
    private int centerScore(char coln,int rown,MbraneColor color)
    {	int sum = 0;
    	for(int row=-1;row<=1;row++)
    	{
    		for(int col=-1;col<=1;col++) 
    		{	MbraneChip top =getCell((char)(col+coln),(row+rown)).topChip();
    			if(top!=null) 
    				{ int v = top.visibleNumber();
    				  if(top.color==color) { sum +=v; } else { sum -=v ; }
    				}
    		}
    	}
    	return(sum);
    }
    private double edgeScore(int zonen,MbraneColor color)
    {	double sum = 0;
    	int zone[] = zoneCenters[zonen];
    	int coln = zone[0];
    	int rown = zone[1];
    	for(int row=-2;row<=2;row++)
    	{	for(int col = -2; col<=2; col+=4)
    		{
    		MbraneCell cc = getCell((char)(coln+col),row+rown);
    		if(cc!=null)
    		{
    			MbraneChip top = cc.topChip();
    			if(top!=null)
    			{
    			double val = top.visibleNumber()/2.0;
    			sum += (top.color==color) ? val : -val;
    			}
    		}}}
    	for(int row=-2;row<=2;row+=4)
    	{	for(int col = -1; col<=1; col++)
    		{
    		MbraneCell cc = getCell((char)(coln+col),row+rown);
    		if(cc!=null)
    		{
    			MbraneChip top = cc.topChip();
    			if(top!=null)
    			{
    			double val = top.visibleNumber()/2.0;
    			sum += (top.color==color) ? val : -val;
    			}
    		}}
    	}
    	return(sum);
    }
    //
    // accept the current placements as permanent
    //
    public void acceptPlacement()
    {	
        droppedDestStack.clear();
        pickedSourceStack.clear();
        stateStack.clear();
        pickedObject = null;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {	MbraneCell rv = droppedDestStack.pop();
    	setState(stateStack.pop());
    	doPick(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	MbraneCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	doDrop(rv);
     }
    
    // 
    // drop the floating object.
    //
    private void dropObject(MbraneCell c)
    {
       droppedDestStack.push(c);
       stateStack.push(board_state);
       doDrop(c);
     }

    private void doDrop(MbraneCell c)
    {
        
        switch (c.rackLocation())
         {
         default:
         	throw G.Error("Not expecting dest %s", c.rackLocation);
         case Reserve_Pool:
         	lastDroppedObject = MbraneChip.getChip(MbraneColor.Red,pickedObject.value);
         	c.addChip(lastDroppedObject);
         	pickedObject = null;
         	break;
         case BoardLocation:	// already filled board slot, which can happen in edit mode
         case EmptyBoard:
             c.addChip(pickedObject);
             empty.remove(c, false);
             chips_on_board++;
             lastDroppedObject = pickedObject;
             pickedObject = null;
             break;
         }
    }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(MbraneCell c)
    {	return(droppedDestStack.top()==c);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { MbraneChip ch = pickedObject;
      if(ch!=null)
    	{	return(ch.chipNumber()); 
    	}
      	return (NothingMoving);
    }
   /**
     * get the cell represented by a source code, and col,row
     * @param source
     * @param col
     * @param row
     * @return
     */
    private MbraneCell getCell(MbraneId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	return(getCell(col,row));
        case Reserve_Pool:
        	return(reserve[row]);
        } 	
    }
    public MbraneCell getCell(MbraneCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(MbraneCell c)
    {	pickedSourceStack.push(c);
    	stateStack.push(board_state);
    	doPick(c);
    }
    private MbraneChip doPick(MbraneCell c)
    {
        switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting rackLocation %s" ,c.rackLocation);
        case Reserve_Pool:
        	MbraneChip ch = c.removeTop();
        	pickedObject = lastPicked = MbraneChip.getChip(reserveColor(),ch.value);
        	lastDroppedObject = null;
        	break;
        case BoardLocation:
        	{
            lastPicked = pickedObject = c.removeTop();
            empty.push(c);
            chips_on_board--;
         	lastDroppedObject = null;
        	}
            break;
        }
        return(pickedObject);
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(MbraneCell c)
    {	return(c==pickedSourceStack.top());
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case Play:
        case PlayNoResolve:
        case PlayOrSwap:
			setState(MbraneState.Confirm);
			break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    private void setWin()
    {
   		switch(variation)
		{
		case MbraneSimple:
			{MbraneColor color = getPlayerColor(0);
			int sectors = 0;
			for(int zone = 0; zone<9; zone++)
				{
					int v = centerScore(zone,color);
					if(v>0) { sectors++; }
					else if(v<0) { sectors--; }
				}
			win[0] = sectors>0;
			win[1] = sectors<0;
			}
			break;
		case Mbrane:
			{
				MbraneColor color = getPlayerColor(0);
				int sectors = 0;
				for(int i=0;i<zoneScore.length;i++)
			{
				double v = zoneScore[i];
				if(v>0) { if(color==MbraneColor.Red) { sectors++; } else { sectors--; }}
				else if(v<0) { if(color==MbraneColor.Red) { sectors--; } else { sectors++; }} 
			}
			win[0] = sectors>0;
			win[1] = sectors<0;
			}
			break;
		default:
			G.Error("Not handled");
		}
    }
    private void setNextStateAfterDone(replayMode replay)
    {	
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state %s",board_state);
    	case Gameover: break;
    	case ConfirmSwap: 
    		setState(MbraneState.Play); 
    		break;
        case ConfirmScore:
        	switch(variation)
        	{
        	case Mbrane: 
        		setState(MbraneState.Score);
        		break;
        	case MbraneSimple:
        		setGameOver();
        		break;
        	default: G.Error("Not expected");
        	}
        	break;
        case ConfirmProposeResolution:
        	setState(MbraneState.ProposeResolution);
        	break;
        case ConfirmPlay:
        	setState(MbraneState.PlayNoResolve);
        	break;
    	case Confirm:
    	case Puzzle:
    	case Play:
    	case PlayNoResolve:
    	case PlayOrSwap:
    		if(!sudokuMovesAvailable()) 
    		{ setState(MbraneState.Gameover); 
    		  setWin();
    		}
    		else {
    		setState(((chips_on_board==1)&&!swapped) 
    				? MbraneState.PlayOrSwap
    				: MbraneState.Play);
    		}
    		break;
    	}
       	resetState = board_state;
    }
    private void setGameOver()
    {
    	setState(MbraneState.Gameover);
		setWin();
    }
    private void doDone(replayMode replay)
    {
        acceptPlacement();
        switch(board_state)
        {
        case Resign:
        	win[nextPlayer[whoseTurn]] = true;
        	setState(MbraneState.Gameover);
    		break;
        case Score:
        	if(variation==MbraneVariation.MbraneSimple)
        	{
        		setGameOver();
        		break;
        	}
			//$FALL-THROUGH$
		default:
    		{
    			
    			if(!sudokuMovesAvailable())
        		{
        		switch(variation)
	        		{ case Mbrane:
	        			setState(MbraneState.Score);
	        		  	break;
	        		case MbraneSimple:
	        			setGameOver();
	        			break;
	        		default: throw G.Error("Not expecting state %s",board_state); 
	        		}
        		}
	        	else 
	        	{	setNextPlayer(replay);
	        		setNextStateAfterDone(replay);
	        	}
    		}
        }
    }
    
void doSwap(replayMode replay)
{	
	MbraneColor c = playerColor[0];
	playerColor[0]=playerColor[1];
	playerColor[1]=c;
	
	swapped=!swapped;
	setState((board_state==MbraneState.PlayOrSwap) ? MbraneState.ConfirmSwap : MbraneState.PlayOrSwap);
	}


    public boolean Execute(commonMove mm,replayMode replay)
    {	Mbranemovespec m = (Mbranemovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }

        //G.print("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
        case MOVE_STARTRESOLUTION:
        	setState(board_state==MbraneState.ConfirmProposeResolution ? MbraneState.Play : MbraneState.ConfirmProposeResolution);
        	break;
        case MOVE_OKRESOLUTION:
        	setState(board_state==MbraneState.ConfirmScore ? MbraneState.ProposeResolution : MbraneState.ConfirmScore);
        	break;
        case MOVE_NORESOLUTION:
        	setState(board_state==MbraneState.ConfirmPlay? MbraneState.ProposeResolution : MbraneState.ConfirmPlay);
        	break;
        case MOVE_SCORESTEP:
        	{
        		boolean alldone = doScore(m.to_row);
        		if(alldone) 
        			{ 
        			 setGameOver();
        			}
        	}
        	break;
        case MOVE_PLACERED:
        	reserveColor = MbraneColor.Red;
        	break;
        case MOVE_PLACEBLACK:
        	reserveColor = MbraneColor.Black;
        	break;
		case MOVE_SWAP:	// swap colors with the other player
			doSwap(replay);
			break;
        case MOVE_DONE:
        	switch(board_state)
        	{
        	case Score:	
        		boolean alldone = doScore(1);
        		if(alldone) 
        			{
        			setGameOver();
        			}
        		break;
        	default:     
        		doDone(replay);
        		break;
        	}
 
            break;

        case MOVE_DROPB:
        	{
			MbraneCell dest =  getCell(m.to_col,m.to_row);
			
			if(isSource(dest)) 
				{ unPickObject(); 
				}
				else 
	            {
					dropObject(dest);
	            }
	            /**
	             * if the user clicked on a board space without picking anything up,
	             * animate a stone moving in from the pool.  For Hex, the "picks" are
	             * removed from the game record, so there are never picked stones in
	             * single step replays.
	             */
	            if(replay==replayMode.Single)
	            	{ animationStack.push(pickedSourceStack.top());
	            	  animationStack.push(dest); 
	            	}
	            setNextStateAfterDrop(replay);
        	}
             break;
        case MOVE_MOVE:
        	{
        	MbraneCell from = getCell(MbraneId.Reserve_Pool,'@',m.from_row);
        	MbraneCell to = getCell(m.to_col,m.to_row);
        	doPick(from);
        	doDrop(to);
            if(replay!=replayMode.Replay)
        	{ animationStack.push(from);
        	  animationStack.push(to); 
        	}
       	setNextStateAfterDrop(replay);
        	}
        	break;
        case MOVE_PICK:
        	{
        	MbraneCell src = getCell(MbraneId.Reserve_Pool,'@',m.to_row);
        	pickObject(src);
        	}
        	break;
 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
 			MbraneCell src = getCell(m.to_col,m.to_row);
 			if(isDest(src)) { unDropObject(); }
 			else
 			{
        	// be a temporary p
        	pickObject(src);
        	switch(board_state)
        	{
        	case Puzzle:
         		break;
        	case Confirm:
        		setState(((chips_on_board==1) && !swapped) ? MbraneState.PlayOrSwap : MbraneState.Play);
        		break;
        	default: ;
        	}}}
            break;

        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
            MbraneCell dest = getCell(MbraneId.Reserve_Pool,'@',m.to_row);
            if(isSource(dest)) { unPickObject(); }
            else { dropObject(dest); }
        	}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(MbraneState.Puzzle);	// standardize the current state
            if(!sudokuMovesAvailable())
               	{ setGameOver();
               	}
            else {  setNextStateAfterDone(replay); }

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?MbraneState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(MbraneState.Puzzle);
 
            break;
       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(MbraneState.Gameover);
    	   break;
       default:
        	cantExecute(m);
        }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }
    

	public boolean legalToHitChips()
	{
		switch(board_state)
		{
		case Play:
		case PlayNoResolve:
		case PlayOrSwap:
		case Puzzle:
			return(true);
		default: return(false);
		}
	}

    public boolean LegalToHitBoard(MbraneCell c)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
		case ConfirmSwap:
		case Gameover:
		case Resign:
		case Score:
		case ProposeResolution:
		case ConfirmPlay:
		case ConfirmProposeResolution:
		case ConfirmScore:
		case Play:
		case PlayNoResolve:
		case PlayOrSwap:
			if (pickedObject==null) { return(false); }
		      	else { return(validSudokuPlacement(c,pickedObject)); }
		case Confirm:
			return(isDest(c));
        default:
        	throw G.Error("Not expecting Hit Board state %s",board_state);

        case Puzzle:
        	if(pickedObject==null) { return(c.topChip()!=null); }
        	else { return(validSudokuPlacement(c,pickedObject)); }
        }
    }

    public CellStack robotCell = new CellStack();	// cells flipped when scoring

 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Mbranemovespec m)
    {
        robotState.push(board_state); //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        //G.print("R "+m);

        if (Execute(m,replayMode.Replay))
        {
        	if (DoneState())
            {
                doDone(replayMode.Replay);
            }
        }
    }
    public commonMove getRandomMove(Random r,int ntries,boolean weighted)
    {	int sz = empty.size();
    	if((sz>0) && ((board_state==MbraneState.Play)||(board_state==MbraneState.PlayNoResolve)))
    	{while(ntries-- > 0)
    		{
    		int idx = r.nextInt(sz);
     		MbraneCell c = empty.elementAt(idx);
       		int didx = 0;
    		if(weighted)
    		{
    		didx = Math.max(0,8-G.numberOfTrailingZeros(r.nextInt(0x1ff)));
    		}
    		else
    		{
    		didx = 1 + r.nextInt(8);		// never randomly choose 0
    		}
    		MbraneCell res = reserve[didx];
    		MbraneChip ch = res.topChip();
    		
    		if(ch!=null)
    			{
    			if(validSudokuPlacement(c,ch))
    				{
    				return(new Mbranemovespec(MOVE_MOVE,res,c,whoseTurn));
    				}
    			}
    		}
    	}
    	return(null);
    }
    private boolean validSudokuPlacement(MbraneCell c,MbraneChip place)
    {	if(c.topChip()!=null) { return(false); }
    	int number = place.visibleNumber();
    	int mask = (1<<number) | (1<<(number+9)) | (1<<(number+18));
    	boolean allowed = (c.validSudokuPlacements&mask)==0;
    	//G.Assert(allowed==validSudokuPlacementSlow(c,place),"maintained state incorrect");
    	return(allowed);
    }
    // not used, but remains for potential future debugging
    boolean validSudokuPlacementSlow(MbraneCell c,MbraneChip place)
    {
    	if(c.topChip()!=null) { return(false); }
    	int number = place.visibleNumber();
    	MbraneCell current = c;
    	while( (current=current.exitTo(CELL_UP))!=null) 
    		{ MbraneChip top = current.topChip(); 
    		  if(top!=null && (top.visibleNumber()==number)) { return(false); }
    		}
    	current = c;
       	while( (current=current.exitTo(CELL_DOWN))!=null) 
		{ MbraneChip top = current.topChip(); 
		  if(top!=null && (top.visibleNumber()==number)) { return(false); }
		}
       	current = c;
       	while( (current=current.exitTo(CELL_LEFT))!=null) 
		{ MbraneChip top = current.topChip(); 
		  if(top!=null && (top.visibleNumber()==number)) { return(false); }
		}
       	current = c;
       	while( (current=current.exitTo(CELL_RIGHT))!=null) 
		{ MbraneChip top = current.topChip(); 
		  if(top!=null && (top.visibleNumber()==number)) { return(false); }
		}
       	char cornerCol = (char)(((c.col-'A')/3)*3+'A');
       	int cornerRow = 1+((c.row-1)/3)*3;
       	current = getCell( cornerCol,cornerRow);
       	for(int row=0;row<3;row++)
       		{
       		MbraneCell rowCell = current;
       		for(int col=0;col<3;col++) 
       		{
       			MbraneChip top = rowCell.topChip();
       			if((top!=null) && (top.visibleNumber()==number)) { return(false); }
       			rowCell = rowCell.exitTo(CELL_RIGHT);
       		}
       		current=current.exitTo(CELL_UP);
       		}
       	return(true);
    }
    public void markValidSudoku()
    {	// maintained incrementally
    	//markValidSudokuSlow();
    }
    // mark all cells with a mask of the used sudoku values
    public void markValidSudokuSlow()
    {	
    	for(MbraneCell c = allCells; c!=null; c=c.next) { c.validSudokuPlacements = 0; }
    	for(MbraneCell c = allCells; c!=null; c=c.next) 
    	{
    		MbraneChip ctop = c.topChip();
    		if(ctop!=null)
    		{
    			int number = ctop.visibleNumber();
    			int rowbit = (1<<number);
    			int colbit = (1<<(number+9));
    			int cellbit = (1<<(number+18));
    			
    	    	MbraneCell current = c;
    	    	c.validSudokuPlacements |= rowbit|colbit|cellbit;
    	    	while( (current=current.exitTo(CELL_UP))!=null) 
    	    		{ current.validSudokuPlacements |= colbit;
    	    		}
    	    	current = c;
    	       	while( (current=current.exitTo(CELL_DOWN))!=null) 
    			{ current.validSudokuPlacements |= colbit;
    			}
    	       	current = c;
    	       	while( (current=current.exitTo(CELL_LEFT))!=null) 
    			{ current.validSudokuPlacements |= rowbit;
    			}
    	       	current = c;
    	       	while( (current=current.exitTo(CELL_RIGHT))!=null) 
    			{ current.validSudokuPlacements |= rowbit;
    			}
    	       	char cornerCol = (char)(((c.col-'A')/3)*3+'A');
    	       	int cornerRow = 1+((c.row-1)/3)*3;
    	       	current = getCell( cornerCol,cornerRow);
    	       	for(int row=0;row<3;row++)
    	       		{
    	       		MbraneCell rowCell = current;
    	       		for(int col=0;col<3;col++) 
    	       		{	rowCell.validSudokuPlacements |= cellbit;
     	       			rowCell = rowCell.exitTo(CELL_RIGHT);
    	       		}
    	       		current=current.exitTo(CELL_UP);
    	       		}
    		}
    	}
    }
    
    /* return a mask of which reserve positions can't be placed */
    public int invalidPlacementMask()
    {	int mask = 0x1ff;
    	for(MbraneCell c = allCells; c!=null; c=c.next) 
    		{  if (c.topChip()==null)
    			{mask &= c.invalidPlacementMask(); 
    			}
    		}
    	// a 1 bit indicates no placements are possible
    	return(mask);
    }
    
    private boolean sudokuMovesAvailable()
    {	markValidSudoku();
    	for(int lim=empty.size()-1; lim>=0; lim--)
    		{ 	MbraneCell c = empty.elementAt(lim);
    			// don't test the 0 bit, if only zero moves are available the game is over
    			// a 1 bit in the placement mask indicates that the corresponding number can't be placed.
    			int invalid = c.invalidPlacementMask();
    			if((invalid & 0x1fe)!=0x1fe) { return(true); }
    		}
    	return(false);
    }
    public int emptyReserve()
    {	
    	return(G.bitCount( invalidPlacementMask()));
    }
   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(Mbranemovespec m)
    {
       //System.out.println("U "+m+" for "+whoseTurn);
    	MbraneState state = robotState.pop();
        setState(state);
        
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
        
        switch (m.op)
        {
   	    default:
   	    	throw G.Error("Can't un execute %s", m);
   	    case MOVE_NORESOLUTION:
   	    case MOVE_OKRESOLUTION:
   	    case MOVE_STARTRESOLUTION:
   	    	break;
   	    case MOVE_SCORESTEP:
   	    	AR.setValue(resolved, false);;
   	    	AR.setValue(zoneScore, 0);
   	    	while(robotCell.size()>0)
   	    	{
   	    		MbraneCell c = robotCell.pop();
   	    		MbraneChip top = c.removeTop();
   	    		top = MbraneChip.getChip(top.color==MbraneColor.Red ? MbraneColor.Black : MbraneColor.Red,top.value);
   	    		c.addChip(top);
   	    	}
   	    	break;
        case MOVE_DONE:
            break;
        case MOVE_MOVE:
        	{
        	MbraneCell c = getCell(m.to_col,m.to_row);
        	MbraneChip ch = doPick(c);
        	MbraneCell d = getCell(MbraneId.Reserve_Pool,'@',ch.visibleNumber());
        	doDrop(d);
        	}
        	break;
        case MOVE_SWAP:
        	doSwap(replayMode.Replay);
        	setState(state);
        	break;
        case MOVE_DROPB:
        	getCell(m.to_col,m.to_row).removeTop();
        	break;
        case MOVE_RESIGN:
            break;
        }
 }
  

 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	
 	switch(board_state)
 	{
 	default: throw G.Error("Not expecting state %s",board_state);
 	case Confirm:
 	case ConfirmSwap:
 	case ConfirmScore:
 	case ConfirmPlay:
 	case ConfirmProposeResolution:
 		all.addElement(new Mbranemovespec(MOVE_DONE,whoseTurn));
 		break;
 	case Score:
 		all.addElement(new Mbranemovespec(MOVE_SCORESTEP,9,whoseTurn));
 		break;
 	case ProposeResolution:
 		all.addElement(new Mbranemovespec(MOVE_NORESOLUTION,whoseTurn));
 		all.addElement(new Mbranemovespec(MOVE_OKRESOLUTION,whoseTurn));
 		break;
 	case PlayOrSwap:
		all.addElement(new Mbranemovespec(SWAP,whoseTurn));
		//$FALL-THROUGH$
	case Play:
	case PlayNoResolve:
 		{
	 	markValidSudoku();
	 	if(chips_on_board==0)
	 	{// first move only, we can check only 1/8 of the cells, and we can ignore 0-3 as possible plays
	 		for(char col = 'A'; col<='E'; col++)
	 		{
	 			for(int row = 1+col-'A'; row<=5;row++)
	 			{
	 				for(int n=4;n<9;n++)
	 				{
	 					all.push(new Mbranemovespec(MOVE_MOVE,reserve[n],getCell(col,row),whoseTurn));
	 				}
	 			}
	 		}
	 		
	 	}
	 	else 
	 	{for(int lim=empty.size()-1; lim>=0; lim--)
	 	{	MbraneCell c = empty.elementAt(lim);
	 		G.Assert(c.topChip()==null,"should be empty");
	 		{//all.addElement(new Mbranemovespec(MOVE_DROPB,c.col,c.row,playerColor[whoseTurn],whoseTurn));
	 		int mask = c.validSudokuPlacements;
	 		int bitMask = 1<<0 | (1<<9) | (1<<18);
	 		int bit = 0;
	 		while(mask!=AllSudokuPlacements)
	 			{
	 			if((mask&bitMask)==0)
	 				{
	 				G.Assert(reserve[bit].topChip()!=null,"there must be some chips available");
	 				all.push(new Mbranemovespec(MOVE_MOVE,reserve[bit],c,whoseTurn));
	 				}
 				mask |= bitMask;
	 			bitMask=bitMask<<1;
	 			bit++;
	 			}
	 		}}}
	 	}}
 	if(all.size()==0)
 	{	//sudokuMovesAvailable();
 		G.print("no moves "+sudokuMovesAvailable());
 	}
 	return(all);
 }
 
 public void initRobotValues()
 {
 }

 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(Character.isDigit(txt.charAt(0)))
	 	{ switch(variation)
	 		{
	 		case MbraneSimple:
	 		case Mbrane:
	 			xpos += cellsize/8;
	 			break;
 			default: G.Error("case %s not handled",variation);
	 		}
	 	}
 		else
 		{ 
 		  ypos -= cellsize/8;
 		}
 	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
 }

}
