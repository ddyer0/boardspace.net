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
package tzaar;

import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;
import tzaar.TzaarChip.ChipColor;


/**
 * TzaarBoard knows all about the game of Dvonn, which is played
 * on a rectangular hexagonal board. It gets a lot of logistic 
 * support from hexboard, which know about the coordinate system.  
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

class TzaarBoard extends hexBoard<TzaarCell> implements BoardProtocol,TzaarConstants
{	// with some care to use cell.geometry instead of 8 or 6, all the logic
	// for the game works the same.  The 
	private TzaarState unresign;
	private TzaarState board_state;
	public TzaarState getState() {return(board_state); }
	public void setState(TzaarState st) 
	{ 	unresign = (st==TzaarState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public boolean gameOver[] = new boolean[NUMPLAYERS]; 
    public CellStack animationStack = new CellStack();
    public boolean placing = true;							// initially true while we are placing chips
    public TzaarCell rack[][] = new TzaarCell[NUMPLAYERS][NTYPES];		// unplaced chips
    public TzaarCell captures[][] = new TzaarCell[NUMPLAYERS][NTYPES];	// captured chips
    ChipColor playerColor[] = new ChipColor[NUMPLAYERS];
    public String standardPosition[][]=	// for black pieces
    	{{"A1","A2","A3","A4",  "D4","E6","E9","F1", "F4","F8","G1","G7", "H1","H6","I1"},
    	 {"B2","B3","B4", "E8","F2","F7", "G2","G6","H2"},
    	 {"C3","C4","E7","F3","F6","G3"}};

	public TzaarCell newcell(char c,int r)
	{	return(new TzaarCell(c,r,cell.Geometry.Hex));
	}
    int prevLastPicked = -1;
    int prevLastDropped = -1;
    int dropState = 0;
    
	static double sumweight = 5.0;
	static double totweight = 0.10;
	static double maxweight = 0.5;
	//
	// the score is based on the number, height, and maximum height of the controlled stacks.
	//
    public double ScoreForPlayer(int who,boolean print,boolean dumbot)
    {  	int tallest[] = new int[NTYPES];	// square of maximum height for each type
    	int power[] = new int[NTYPES];		// total height of all stacks with each type
    	int sum[] = new int[NTYPES];		// number of stacks with each type.
    	
    	for(TzaarCell c = allCells;
	    	c != null;
	    	c = c.next)
    	{	TzaarChip ch = c.topChip();
    		if((ch!=null) && (ch.color==playerColor[who]))
    		{	int typ = ch.typeIndex;
    			int height = c.chipIndex+1;
    			sum[typ]++;
    			power[typ]+=height;
    			if((height*height)>tallest[typ]) { tallest[typ] = height*height; }
    		}
    	}
    	int lowsum =999; // weakest type measured by the total chips under the type.
    	for(int i=0;i<NTYPES;i++) { if(sum[i]<lowsum) { lowsum = sum[i];  }}	
    	double sc = sumweight * lowsum*lowsum;
    	if(print) 
    		{ System.out.println(" minsum "+sc);
    		}
    	for(int i=0;i<NTYPES;i++)
    	{	double tscore = maxweight*tallest[i]*tallest[i];	// fourth power of tallest height
    		double pscore = totweight*power[i]*sum[i];			// total height * number of stacks
    		int ts = sum[i];
    		int ls = ts-lowsum;
    		double sscore = (sumweight*ls)/(ts+1);	// score stacks only for excess over weakest
    		sc += tscore + pscore + sscore;
    		if(print)
    		{	System.out.println("tallest "+tscore+" quant "+sscore+" power "+pscore);
    		}
    	}
    	// score is greater with taller stacks and more chips under control and more stacks.
     	return(sc);
    }

    public void SetDrawState() { throw G.Error("shouldn't happen"); }
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public TzaarChip pickedObject = null;			// the object in the air
    private TzaarCell pickedSourceStack[]=new TzaarCell[3];
    private TzaarCell droppedDestStack[]=new TzaarCell[3];
    private int pickedStackHeight[]=new int[3];
    private int droppedStackHeight[]=new int[3];
    private TzaarCell allCaptures = null;
    private int stackIndex = 0;

    public boolean isSource(TzaarCell c)
    {	return((pickedObject!=null) && (c==pickedSourceStack[stackIndex]));
    }
    public int movingObjectIndex()
    {	TzaarChip ch = pickedObject;
    	if((ch!=null)&&(droppedDestStack[stackIndex]==null))
    		{ return(ch.pieceNumber());
    		}
        return (NothingMoving);
    }
    public ChipColor movingObjectColor()
    {	if((pickedObject!=null)&&(droppedDestStack[stackIndex]==null))
    		{ return(pickedObject.color);
    		}
        return (null);
    }
    public int movingObjectType()
    {	if((pickedObject!=null)&&(droppedDestStack[stackIndex]==null))
    		{ return(pickedObject.typeIndex);
    		}
        return (NothingMoving);
    }
    private void finalizePlacement()	// finalize placement, no more undo
    { 	if(stackIndex>=pickedSourceStack.length) { stackIndex--; }
    	while(stackIndex>=0) 
    		{if(stackIndex<pickedSourceStack.length)
    			{ pickedSourceStack[stackIndex]=droppedDestStack[stackIndex] = null;
    			  pickedStackHeight[stackIndex]=droppedStackHeight[stackIndex]=0;
    			}
    		 stackIndex--;
    		}
    	stackIndex = 0;
     }   


    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    	TzaarCell dr = droppedDestStack[stackIndex];
    	if(dr!=null)
    	{
    	pickedObject = dr.removeTop();
    	dr.lastDropped = prevLastDropped;
    	prevLastDropped = -1;
    	dropState--;
    	TzaarCell src = pickedSourceStack[stackIndex];
    	moveStack(dr,
    	          src,
    	          pickedStackHeight[stackIndex]-1);
    	undoCaptures(droppedStackHeight[stackIndex],whoseTurn,dr);
    	droppedDestStack[stackIndex] =  null;
    	}
    }
    // picked and dropped are the source and destination respectively.  This handles both
    // capture by approach and by withdrawal.

    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	TzaarChip po = pickedObject;
    	if(po!=null)
    	{
    	TzaarCell ps = pickedSourceStack[stackIndex];
    	ps.addChip(po);
    	ps.lastPicked = prevLastPicked;
    	prevLastPicked = -1;
        pickedSourceStack[stackIndex]=null;
    	pickedObject = null;
    	}
     }
    public TzaarCell getSource()
    { 	if(pickedObject!=null) { return(pickedSourceStack[stackIndex]); }
    	return(null);
    }
    private void dropOnBoard(TzaarCell dcell,int who,boolean move,replayMode replay)
    {
    	droppedDestStack[stackIndex] = dcell;
    	TzaarChip dp = dcell.topChip();
    	droppedStackHeight[stackIndex] = 0;
    	if((dp!=null) && (dp.color!=pickedObject.color))
    	{  	droppedStackHeight[stackIndex] = dcell.chipIndex+1;
    		removeStack(dcell,who,replay);
    	}
    	TzaarCell from = pickedSourceStack[stackIndex];
    	if(move) { moveStack(from,dcell,999); }
    	dcell.addChip(pickedObject);
    	pickedObject=null;
     }
    // 
    // drop the floating object.
    //
    private void dropObject(TzaarId dest, int who,char col, int row,replayMode replay)
    {
       G.Assert((pickedObject!=null)&&(droppedDestStack[stackIndex]==null),"ready to drop");
       switch (dest)
        {
        default: throw G.Error("Not expecting dest %s",dest);
        case First_Player_Rack:
        	(placing ? rack : captures)[who][row].addChip(pickedObject);
        	pickedObject = null;
        	break;
        case Second_Player_Rack:
        	(placing ? rack : captures)[who][row].addChip(pickedObject);
        	pickedObject = null;
        	break;
        }
    }
    private void pickFromCell(TzaarCell src)
    {  	//G.print("st "+stackIndex);
    	pickedSourceStack[stackIndex] = src;
        pickedStackHeight[stackIndex] = src.chipIndex+1;
    	pickedObject = src.removeTop();
    	prevLastPicked = src.lastPicked;
    	src.lastPicked = moveNumber;
    }

    private void pickObject(TzaarCell from)
    {
    	G.Assert((pickedObject==null)&&(pickedSourceStack[stackIndex]==null),"ready to pick");
    	pickFromCell(from);
    	
    }
    private TzaarCell getCell(TzaarId source, char col, int row)
    {	
    
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case Second_Player_Rack:
        	return((placing?rack:captures)[SECOND_PLAYER_INDEX][row]);
        case First_Player_Rack:
        	return((placing?rack:captures)[FIRST_PLAYER_INDEX][row]);
        case BoardLocation:
         	{
         	return(getCell(col,row));
         	}
       }
   }

   
    private TzaarCell[][]tempDestResource = new TzaarCell[4][];
    private int tempDestIndex=-1;
    public synchronized TzaarCell []getTempDest() 
    	{ if(tempDestIndex>=0) { return(tempDestResource[tempDestIndex--]); }
    	  return(new TzaarCell[MAXDESTS]);
    	}
    public synchronized void returnTempDest(TzaarCell[]d) { tempDestResource[++tempDestIndex]=d; }

    public TzaarBoard(long random,String init,int map[]) // default constructor
    {  	drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_NOTHING or STYLE_CELL or STYLE_LINES
    	Grid_Style = TZAARGRIDSTYLE; //coordinates left and bottom
    	int ntypes = TzaarChip.NTYPES;
    	Random r = new Random(143006);
    	setColorMap(map, 2);
    	
    	for(int p=FIRST_PLAYER_INDEX; p<=SECOND_PLAYER_INDEX; p++)
    		{ TzaarCell row[]=new TzaarCell[ntypes];
    		  TzaarCell caps[]= new TzaarCell[ntypes];
    		  rack[p] = row;
    		  captures[p] = caps;
    		  for(int i=0;i<ntypes;i++) 
    		  { row[i] = new TzaarCell(r,'@',i);
    		  	row[i].rackLocation = Rack_Cell[p];
    		    caps[i] = new TzaarCell(r,'-',i);
    		    caps[i].rackLocation = Capture_Cell[p];
    		  }
    		}
       	initBoard(ZfirstInCol, ZnInCol, null);
        unlinkCell(getCell('E',5));
        allCaptures = new TzaarCell(r,'&',0);
    	doInit(init,random); // do the initialization 
    	allCells.setDigestChain(r);
    }
    public TzaarBoard cloneBoard() 
	{ TzaarBoard dup = new TzaarBoard(randomKey,gametype,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}    // overridable method.  xpos ypos is the desired centerpoint for the text
    // 
    public void copyFrom(BoardProtocol b) { copyFrom((TzaarBoard)b); }

    private void init_racks()
    {	
    	allCaptures.reInit();
    	reInit(rack);
    	reInit(captures);
        for(int pl = FIRST_PLAYER_INDEX; pl<=SECOND_PLAYER_INDEX;pl++)
        {	TzaarCell row[] = rack[pl];
        	for(int i=0;i<row.length;i++)
        		{ TzaarCell c = row[i];
     		      for(int j=0; j<TzaarChip.Initial_Count[i];j++)
    		      {	c.addChip(TzaarChip.getChip(i,pl));
    		      }
        		}
        }
    }
    

    private void Init_Standard(String game,long key)
    {  
        gametype = game;
        randomKey = key;
        setState(TzaarState.PUZZLE_STATE);
    	int map[] = getColorMap();
    	playerColor[map[FIRST_PLAYER_INDEX]] = ChipColor.White;
    	playerColor[map[SECOND_PLAYER_INDEX]] = ChipColor.Black;

        for(TzaarCell c=allCells;
            c!=null;
            c=c.next)
        {	c.reInit();
        }

        init_racks();		// stack everything in the racks
        placing = true;
        allCaptures.reInit();
        if(Tzaar_Random_Init.equalsIgnoreCase(game))
        {
        placing = false;
        // stack all the chips on the center, then randomize and redistribute
        for(int pl=0;pl<rack.length;pl++)
        	{ TzaarCell row[] = rack[pl];
        	  for(int i=0;i<row.length;i++)
        	  { 
        		  TzaarCell c = row[i];
        		  while(c.chipIndex>=0) { allCaptures.addChip(c.removeTop()); }
        	  }
        	}
        allCaptures.randomize(key);
        
        for(TzaarCell c = allCells;
        	  c!=null;
        	  c = c.next)
        	{	if(allCaptures.chipIndex>=0) { c.addChip(allCaptures.removeTop()); }
        	}
        G.Assert(allCaptures.chipIndex==-1,"all chips placed");
        }
        else if (Tzaar_Custom_Init.equalsIgnoreCase(game)) 
        { 
          placing = true;
        }
        else if (Tzaar_Standard_Init.equalsIgnoreCase(game)) 
        {	placing=false;
        	for(int pl = FIRST_PLAYER_INDEX; pl<=SECOND_PLAYER_INDEX;pl++)
        	{	TzaarCell row[]=rack[pl];
        		for(int idx = 0;idx<row.length;idx++)
        		{	TzaarCell c = row[idx];
        			String pos[]=standardPosition[idx];
        			// don't mess with this - doing so will invalidate historical game records
        			while(c.chipIndex>=0)
        			{	
        				String position = pos[c.chipIndex];
        				TzaarChip p = c.removeTop();
        				char col = position.charAt(0);
        				int rown = position.charAt(1)-'0';
        				if(pl==FIRST_PLAYER_INDEX)
        				{	int coln = col-'A';
        					col = (char)('I'-coln);
        					rown = 1+ nInCol[coln]- (rown-firstRowInCol[coln]);
        				}
        				getCell(col,rown).addChip(p);
        			}
        			
        		}
        	}
        }
        else { throw G.Error(WrongInitError,game); }
        
      whoseTurn = FIRST_PLAYER_INDEX;
    }
    public void sameboard(BoardProtocol f) { sameboard((TzaarBoard)f); }
    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(TzaarBoard from_b)
    {	super.sameboard(from_b);	// check the actual board
    	G.Assert(placing==from_b.placing,"in same place phase");
    	G.Assert(pickedObject==from_b.pickedObject,"pickedObject matches");
    	G.Assert(allCaptures.sameCell(from_b.allCaptures),"same capture stack");
    	G.Assert(sameCells(rack,from_b.rack),"rack mismatch");
    	G.Assert(sameCells(captures,from_b.captures),"captures mismatch");
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

        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        
		v ^= chip.Digest(r,pickedObject);
		v ^= cell.Digest(r,getSource());
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);

       return (v);
    }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(TzaarBoard from_b)
    {	super.copyFrom(from_b);
        pickedObject = from_b.pickedObject;
        placing = from_b.placing;
        copyFrom(rack,from_b.rack);
        copyFrom(captures,from_b.captures);
 
		allCaptures.copyFrom(from_b.allCaptures);
        stackIndex = from_b.stackIndex;
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        AR.copy(pickedStackHeight,from_b.pickedStackHeight);
        AR.copy(droppedStackHeight,from_b.droppedStackHeight);
        sameboard(from_b);
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {
       Init_Standard(gtype,key);
       animationStack.clear();
       pickedObject = null;
       AR.setValue(pickedSourceStack,null);
       AR.setValue(droppedDestStack,null);
       AR.setValue(pickedStackHeight,0);
       AR.setValue(droppedStackHeight,0);
       moveNumber = 1;
       prevLastDropped = -1;
       prevLastPicked = -1;
       dropState = 0;

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
        case CONFIRM_PLACE_STATE:
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
         case CONFIRM_PLACE_STATE:
            return (true);

        default:
            return (false);
        }
    }
    public boolean DigestState()
    {	if((pickedObject!=null)) { return(false); }
    	switch(board_state)
    	{
    	default: return(DoneState());
    	}
    }


    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(TzaarState.GAMEOVER_STATE);
    }
    public boolean hasAllTypes(int pl)
    {	int mask = 0;
    	for(TzaarCell c = allCells;
     	    c != null;
     	    c = c.next)
    	{	TzaarChip ch = c.topChip();
    		if((ch!=null) && (ch.color==playerColor[pl]))
    		{	mask |= ch.typeMask;
    			if(mask==TzaarChip.ALLTYPES_MASK) 
    				{ return(true); }
    		}
    	}
    	return(false);
    }



    //
    // in the actual game, picks are optional; allowed but redundant.
    //
    private void setNextStateAfterDrop()
    {
     	switch(board_state)
    	{
    	default: throw G.Error("State %s Not expected",board_state);
    	case CAPTURE_STATE:
    		// first move of the game, white gets one capture only
      		{
      		boolean heHasTypes = hasAllTypes(nextPlayer[whoseTurn]);
    		setState((!heHasTypes||(allCaptures.chipIndex==0))?TzaarState.CONFIRM_STATE:TzaarState.PLAY_STATE); 
      		}
     		break;
    	case PLACE_STATE:
    		setState(TzaarState.CONFIRM_PLACE_STATE);
    		break;
    	case PLAY_STATE: 
    	{ 	setState(TzaarState.CONFIRM_STATE);
    	}
    	break;
    	}
    }
    private void setNextStateAfterPick()
    {        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting pick in state %s", board_state);
        case CONFIRM_PLACE_STATE:
        	setState(TzaarState.PLACE_STATE);
        	break;
        case GAMEOVER_STATE:
        case CONFIRM_STATE:
        	// on the first move there's only a capture phase.
        	// if the unpicked move was a win on the first move, we reverty to capture state
        	//
        	setState(((allCaptures.chipIndex==-1)||(stackIndex==0))?TzaarState.CAPTURE_STATE:TzaarState.PLAY_STATE);
        	break;
        case PLAY_STATE:
        case PLACE_STATE:
        case CAPTURE_STATE:
			break;
        case PUZZLE_STATE:
            break;
        }
    }

    public boolean allPlaced()
    {	if(placing)
    	{for(int pl=FIRST_PLAYER_INDEX;
    		pl<=SECOND_PLAYER_INDEX;
    		pl++)
    	{	TzaarCell row[] = rack[pl];
    		for(int i=0;i<row.length;i++) 
    		{ if(row[i].chipIndex>=0) { return(false); } 
    		}
    	}
    	}
    	return(true);
    }
    private void doEndGame()
    {	if(placing) { setState(TzaarState.PLACE_STATE); }
    	else
    {
		boolean hasTypes = hasAllTypes(whoseTurn);	// all of one type captured?
		boolean otherTypes = hasAllTypes(nextPlayer[whoseTurn]);
		setState(TzaarState.CAPTURE_STATE);	// default, and makes hasLegalMoves work 
		if(!otherTypes) { setGameOver(true,false); }
		else if(hasTypes && hasLegalMoves(whoseTurn)) {  ;  }
		else	// no one has moves, gameover
			{ 
			  setGameOver(false,true);
			}
    	}
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state.getDescription());
    	
    	case GAMEOVER_STATE: 
    		break;
    	case CONFIRM_STATE:
    		doEndGame();
     		break;
    	case PUZZLE_STATE:
    		break;
    	case CONFIRM_PLACE_STATE:
    		if(allPlaced())
    		{
    			placing = false;
    			setState(TzaarState.CAPTURE_STATE);
    		}
    		else
    		{
    		setState(TzaarState.PLACE_STATE);
    		}
    		break;
    	}

    }
     
    private void doDone()
    {	
    	if(board_state==TzaarState.RESIGN_STATE) 
    	{ setGameOver(false,true);
    	}
    	else
    	{
    	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case PUZZLE_STATE: break;
    	case CONFIRM_STATE:  
    	case CONFIRM_PLACE_STATE:
    		finalizePlacement();
       	 	setNextPlayer(); 
     		setNextStateAfterDone();
     		dropState++;
     		break;
    	}}
    }
    
    // move a stack of chips preserving the stack order, if matching is not null move
    // only chips matching the color
    public void moveStack(TzaarCell from,TzaarCell to,int depth)
    {	if((from.chipIndex>=0) && (depth>0))
    		{	TzaarChip fromp = from.removeTop();
    			moveStack(from,to,depth-1);
    			to.addChip(fromp);
    		}
   }

    // remove a stack of chips, place the removed in the captured stack
    public void removeStack(TzaarCell c,int who,replayMode replay)
    {	
    	while(c.chipIndex>=0)
    	{
    	TzaarChip ch = c.removeTop();
    	TzaarCell dest = captures[who][ch.typeIndex];
    	if(replay!=replayMode.Replay) 
    		{  animationStack.push(c); 
    		   animationStack.push(dest); 
    		}
    	allCaptures.addChip(ch);
    	dest.addChip(ch);
    	}
    }
    // remove the top N pieces from the captured stack and place them on dest
    public void undoCaptures(int n,int who,TzaarCell dest)
    {
    	while(n-->0)
    	{	TzaarChip ch = allCaptures.removeTop();
    		captures[who][ch.typeIndex].removeTop();
    		dest.addChip(ch);
    	}
    }
    public boolean isPassed()
    {	int sm1 = stackIndex-1;
    	return((sm1>0)
    			&& (pickedSourceStack[sm1]==droppedDestStack[sm1])
    			&& (droppedStackHeight[sm1]==0));
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	TzaarMovespec m = (TzaarMovespec)mm;
    	if(replay!=replayMode.Replay) { animationStack.clear(); }
       //G.print("E "+m+" for "+whoseTurn+" : "+stackIndex);
        switch (m.op)
        {
        case MOVE_DONE:
         	doDone();

            break;
        case MOVE_RACK_BOARD:
        	{
            TzaarCell from = getCell(m.source, m.from_col, m.from_row);
            TzaarCell to = getCell(m.to_col,m.to_row);
            m.top = from.topChip();
            m.bottom = to.topChip();
            pickFromCell(from);
			dropOnBoard(to,m.player,false,replay);
     	   	//undoInfo = m.undoInfo = sweepForDvonnContact();
            stackIndex++;
            setNextStateAfterDrop();
            if(replay!=replayMode.Replay)
            	{
            	animationStack.push(from);
       	     	animationStack.push(to);
            	}
        	}
        	break;
        case CAPTURE_BOARD_BOARD:
        case MOVE_BOARD_BOARD:
        	{
        	TzaarCell from = getCell(m.from_col, m.from_row);
        	TzaarCell to = getCell(m.to_col, m.to_row); 
			TzaarChip dp = m.bottom = to.topChip();
			TzaarChip fp = m.top = from.topChip();
			
            if(replay!=replayMode.Replay)
        	{ for(int lim=from.height()-1; lim>=0; lim--)
        		{ animationStack.push(from);
        	      animationStack.push(to);
        		}
        	}
			
			if((dp!=null) && (dp.color!=fp.color))
				{	m.undoInfo = (from.chipIndex+1)*100+to.chipIndex+1;
					removeStack(to,m.player,replay);	// capture
				}
			else
			{	m.undoInfo = (from.chipIndex+1)*100;	// no capture
				G.Assert(board_state!=TzaarState.CAPTURE_STATE,"must be a capture");
			}
        	moveStack(from,to,999);
        	prevLastPicked = from.lastPicked;
        	prevLastDropped = to.lastDropped;
        	from.lastPicked = dropState;
        	to.lastDropped = dropState;
        	dropState++;
            setNextStateAfterDrop();
        	}
        	break;
        case MOVE_DROPB:
        case MOVE_DROPCAP:
        	{
        	G.Assert(pickedObject!=null,"something is moving");
        	TzaarCell from = pickedSourceStack[stackIndex];
        	TzaarCell target =  getCell(m.to_col, m.to_row);
        	if(from==target) { unPickObject(); }
        	else
        	{
        	m.bottom = target.topChip();
            if(replay==replayMode.Single) 
            	{ for(int lim=from.height(); lim>=0; lim--)
            		{animationStack.push(from);
            	     animationStack.push(target);
            		}
            	}
            switch(board_state)
            {
            default:
            	throw G.Error("Not expecting state %s",board_state);
            case PUZZLE_STATE:
                dropOnBoard(target,m.player,false,replay);
                finalizePlacement();
            	break;
			case PLACE_STATE:
				dropOnBoard(target,m.player,false,replay);
				prevLastDropped = target.lastDropped;
				target.lastDropped = dropState;
				dropState++;
         	   	//undoInfo = m.undoInfo = sweepForDvonnContact();
                stackIndex++;
                setNextStateAfterDrop();
                break;
            case CAPTURE_STATE:
            	{	TzaarChip ttop = target.topChip();
            		G.Assert((ttop!=null)&&(ttop.color!=pickedObject.color),"captures");
            	}
  				//$FALL-THROUGH$
			case PLAY_STATE:
               dropOnBoard(target,m.player,true,replay);
               prevLastDropped = target.lastDropped;
               target.lastDropped = dropState;
               dropState++;
         	   	//undoInfo = m.undoInfo = sweepForDvonnContact();
                stackIndex++;
                setNextStateAfterDrop();
               break;
            }}
        	}
            break;
        case MOVE_PASS:
        	if(isPassed())
        	{ 	//changed his mind about passing.
        		stackIndex--;
        		unDropObject();
        		unPickObject();
        		setState(TzaarState.PLAY_STATE);
        	}
        	else
        	{
        	setNextStateAfterDrop();
        	if(stackIndex>0)
        	{
        	TzaarCell dest = droppedDestStack[stackIndex-1];
        	pickedSourceStack[stackIndex] = droppedDestStack[stackIndex] = dest;
            pickedStackHeight[stackIndex] = dest.chipIndex+1;
            droppedStackHeight[stackIndex]= 0;
           	stackIndex++;
        	}}
            break;
 
        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary pick
        	switch(board_state)
        	{	
           	case CONFIRM_STATE:
           	case CONFIRM_PLACE_STATE:
           		if(stackIndex>0) { stackIndex--; }
           		unDropObject();
           		setNextStateAfterPick();
            	break;
           	default:
           		{
           		TzaarCell target = getCell(m.from_col, m.from_row);
           		  m.top = target.topChip();
           		  pickFromCell(target);
           		prevLastPicked = target.lastPicked;
           		target.lastPicked = dropState;
                  setNextStateAfterPick();
           		}
        	}
            break;
         case MOVE_DROP: // drop on chip pool;
            dropObject(m.source, m.player,m.to_col, m.to_row,replay);
            finalizePlacement();
            break;

        case MOVE_PICK:
            unDropObject();
            unPickObject();
            TzaarCell c = getCell(m.source, m.from_col, m.from_row);
            m.top = c.topChip();
            pickObject(c);
            setNextStateAfterPick();
            break;

        	
        case MOVE_START:
            setWhoseTurn(m.player);
            finalizePlacement();
            setState(TzaarState.PUZZLE_STATE);
            doEndGame();
            break;

        case MOVE_RESIGN:
            setState(unresign==null?TzaarState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
        	finalizePlacement();
            setState(TzaarState.PUZZLE_STATE);

            break;
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;

        default:
        	cantExecute(m);
        }

 
        return (true);
    }
 
    // legal to hit the chip reserve area
    public boolean LegalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
         case CONFIRM_STATE:
         case CONFIRM_PLACE_STATE:
         case PLAY_STATE:
         case RESIGN_STATE:
         case CAPTURE_STATE:
        	return(false);

		case GAMEOVER_STATE:
			return(false);
		case PLACE_STATE:
			return(whoseTurn==player);
        case PUZZLE_STATE:
        	return(true);
        }
    }
  
    // true if it's legal to drop gobblet  originating from fromCell on toCell
    public boolean LegalToDropOnBoard(TzaarCell fromCell,TzaarCell toCell)
    {	if(fromCell==toCell) { return(true); }
      	TzaarCell temp[] = getTempDest();
    	int n = getDestsFrom(fromCell,whoseTurn,temp,0,fromCell==pickedSourceStack[stackIndex]);
    	boolean ok = false;
    	for(int i=0;(i<n) && !ok;i++) 
    		{ if(temp[i]==toCell) 
    		{ ok = true; } 
    		}
    	returnTempDest(temp);
    	return(ok);
    }

    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    private boolean isDest(TzaarCell cell)
    {	return((stackIndex>0) && (droppedDestStack[stackIndex-1]==cell));
    }

    public boolean LegalToHitBoard(TzaarCell cell)
    {	
        switch (board_state)
        {
		case PLACE_STATE:
			if(pickedObject!=null)
 			{	return(LegalToDropOnBoard(pickedSourceStack[stackIndex],cell));
 			}
			return false;
			
        case CAPTURE_STATE:
 		case PLAY_STATE:
 			if(pickedObject!=null)
 			{	return(LegalToDropOnBoard(pickedSourceStack[stackIndex],cell));
 			}
 			
 			return((cell.chipIndex>=0)
 					&& (cell.topChip().color==playerColor[whoseTurn])
 					&& (getDestsFrom(cell,whoseTurn,null,0,false)>0));
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
		case CONFIRM_PLACE_STATE:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);

        case PUZZLE_STATE:
        	return((pickedObject==null)
        			?(cell.chipIndex>=0)	// pick from a nonempty cell
        			: true);				// drop on any cell
        }
    }

 
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(TzaarMovespec m)
    {
        m.state = board_state; //record the starting state. The most reliable
        // to undo state transitions is to simple put the original state back.
        
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        //System.out.println("R "+m+" for "+m.player);
        if (Execute(m,replayMode.Replay))
        {	if((m.op==CAPTURE_BOARD_BOARD)||(m.op==MOVE_BOARD_BOARD)||(m.op==MOVE_PASS))
        	{	if(DoneState()) 
        			{  doDone(); 
        			}
        	}
        	else if (m.op == MOVE_DONE)
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
    public void UnExecute(TzaarMovespec m)
    {	int who = whoseTurn;
    	setWhoseTurn(m.player);
        //System.out.println("U "+m+" for "+whoseTurn);

        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;
   	    case MOVE_DROPCAP:
   	    case MOVE_DROPB:
   	    	{ unDropObject();
   	    	}
	    	break;
	    	  
        case MOVE_PASS:
        case MOVE_DONE:
            break;
        case MOVE_RACK_BOARD:
        	{
			TzaarCell dest = getCell(m.to_col,m.to_row);
			TzaarCell src = getCell(m.source,m.from_col,m.from_row);
			src.addChip(dest.removeTop());
        	}
        	break;
        case MOVE_BOARD_BOARD:
        case CAPTURE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
        		case CAPTURE_STATE:
        		case PLAY_STATE:
        			int undo = m.undoInfo;
        			int stak = undo/100;
        			int cap = undo%100;
        			TzaarCell dest = getCell(m.to_col,m.to_row);
        			TzaarCell src = getCell(m.from_col,m.from_row);
        			moveStack(dest,src,stak);
        			if(cap>0) { undoCaptures(cap,m.player,dest); }
        			break;
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(m.state);
        if(who!=m.player)
        	{
        	moveNumber--;
        	}
 }

// get the legal dests from "from" for player "forplayer"
// temp can be null, in which case only the number of dests is returned
 int getDestsFrom(TzaarCell from,int forplayer,TzaarCell temp[],int idx0,boolean picked)
 {	int n=idx0;
 	switch(board_state)
 	{
 	default: throw G.Error("Not implemented");
 	case PLACE_STATE:
 		for(TzaarCell c = allCells;
 			c != null;
 			c = c.next)
 		{	if(c.topChip()==null)
 			{
 			if(temp!=null) { temp[n] = c; }
			n++;
		}
 		}
 		break;
 	case CAPTURE_STATE:
 	case PLAY_STATE:
 		if(from.onBoard)
 		{	int height = from.chipIndex+(picked?2:1);
 			if(height>0)
 			{
 			TzaarChip top = picked ? pickedObject : from.topChip();
 			if(top.color==playerColor[forplayer])
 			{
 			for(int direction = 0; direction<CELL_FULL_TURN; direction++)
 			{
 				TzaarCell dest = from.exitTo(direction);
 				while(dest!=null)
 				{
 					TzaarChip dp = dest.topChip();
 					if(dp!=null)
 					{
 					switch(board_state)
 					{	default: throw G.Error("not expecting this");
 						case PLAY_STATE: if(dp.color==playerColor[forplayer]) 	// stacking in play state
 							{	if(temp!=null) { temp[n] = dest; }
 								n++;
 								break;
 							}
 						//$FALL-THROUGH$
					case CAPTURE_STATE:	
 							if((dp.color!=playerColor[forplayer])&&(dest.chipIndex<height))
							{ if(temp!=null) {temp[n] = dest; }
							  n++;
							}
							break;
 						}
 					break;
 					}
 					dest = dest.exitTo(direction);
 					}
 				}
 			}
 			}}
 
 		break;
 	case CONFIRM_STATE:
 	case PUZZLE_STATE: 
 		break;
 	}
 	return(n);
 }
 // true of either player has a legal move
 boolean hasLegalMoves(int forp)
 {
	 for(TzaarCell c = allCells;
	     c!=null;
	     c = c.next)
	 {	if(getDestsFrom(c,forp,null,0,false)>0) { return(true); }
	 }
	 return(false);
 }
 // get a hash of moves destinations, used by the viewer
 // to know where to draw dots and also to determine what
 // is a legal move
 public Hashtable<TzaarCell,TzaarCell> getMoveDests()
 {	Hashtable<TzaarCell,TzaarCell> dd = new Hashtable<TzaarCell,TzaarCell>();
 	if(pickedObject!=null)
 	{
 	TzaarCell src = pickedSourceStack[stackIndex];
 	if(src!=null)
 	{	TzaarCell temp[] = getTempDest();
 		int n = getDestsFrom(src,whoseTurn,temp,0,true);
 		for(int i=0;i<n;i++)
 		{	TzaarCell c = temp[i];
 			dd.put(c,c);
 		}
 		returnTempDest(temp);
 	}}
	return(dd);
 }
 
 CommonMoveStack  GetListOfMoves(CommonMoveStack  all,boolean erupting,boolean nonerupting)
 {	
	switch(board_state)
	{
	default:
		throw G.Error("Not implemented");
	case PLACE_STATE:
		{
		TzaarCell from[] = rack[whoseTurn];
		for(TzaarCell c = allCells; c!=null; c=c.next)
		{
			if(c.isEmpty())
			{
				for(TzaarCell fr : from)
				{
					if(!fr.isEmpty())
					{
						all.addElement(new TzaarMovespec(MOVE_RACK_BOARD,fr,c,whoseTurn));
					}
				}
			}
		}}
		break;
	case PLAY_STATE:
		all.addElement(new TzaarMovespec(MOVE_PASS,whoseTurn));
		//$FALL-THROUGH$
	case CAPTURE_STATE:
		{
		TzaarCell temp[] = getTempDest();
		for(TzaarCell c =allCells;
		c !=null;
		c=c.next)
		{	TzaarChip cp = c.topChip();
			if((cp!=null) && (cp.color==playerColor[whoseTurn]))
			{ 	int n = getDestsFrom(c,whoseTurn,temp,0,false);
				for(int i=0;i<n;i++)
				{	TzaarCell dest = temp[i];
					int op = (dest.topChip().color!=playerColor[whoseTurn]) ? CAPTURE_BOARD_BOARD : MOVE_BOARD_BOARD;
					all.addElement(new TzaarMovespec(op,c.col,c.row,dest.col,dest.row,whoseTurn));
					
				}
			}
		}
		if(board_state==TzaarState.PLAY_STATE)
		{ all.addElement(new TzaarMovespec(MOVE_PASS,whoseTurn));
		}
		returnTempDest(temp);
		}
		break;
	}
  	return(all);
 }
 
}