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
package kuba;

import java.awt.Color;
import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;

/**
 *  
 *  TODO: clean up MOVE_START so it doesn't depend on correct placement of balls in the trenches.
 *  
 * KubaBoard knows all about the game of Kuba, which is played
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

class KubaBoard extends rectBoard<KubaCell> implements BoardProtocol,KubaConstants
{	
	static final int MAX_MOVES_PER_TURN = 20;	// somewhat arbitrary, but 10 is possible..
	static final int NUMBER_OF_REDS = 15;
	static final int NUMBER_OF_BLACKS = 8;
	static final int DEFAULT_COLUMNS = 7;	// 9x5 board
	static final int DEFAULT_ROWS = 7;	//
    static final String[] KUBAGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	static final String Traboulet_INIT = "traboulet";	//init for standard game

	private KubaState unresign;
	private KubaState board_state;
	public CellStack animationStack = new CellStack();
	public KubaState getState() {return(board_state); }
	public void setState(KubaState st) 
	{ 	unresign = (st==KubaState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
    public int boardColumns = DEFAULT_COLUMNS;	// size of the board
    public int boardRows = DEFAULT_ROWS;
    public void SetDrawState() { setState(KubaState.DRAW_STATE); }
    public KubaCell kChips[]=null;
    public int capturedRed[] = new int[2];
    public int capturedOpp[] = new int[2];
    public KubaCell allGutters[]=new KubaCell[DEFAULT_COLUMNS*4];
    public KubaCell trays[][] = 
    					{new KubaCell[NUMBER_OF_BLACKS],
    					new KubaCell[NUMBER_OF_REDS],
    					new KubaCell[NUMBER_OF_BLACKS],
    					new KubaCell[NUMBER_OF_REDS]};
    public KubaCell gutters[][] =    					
    		{new KubaCell[DEFAULT_COLUMNS],
			new KubaCell[DEFAULT_COLUMNS],
			new KubaCell[DEFAULT_COLUMNS],
			new KubaCell[DEFAULT_COLUMNS]};
    public KubaCell redTrays[][] = { trays[TopIndex],trays[BottomIndex]};
    public KubaCell colorTrays[][] = { trays[LeftIndex],trays[RightIndex]};
    public KubaCell lastPushed=null;
    public int lastPushedDirection=-1;

    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public KubaChip pickedObject = null;	// the object in the air
    public KubaCell pickedSource[] = new KubaCell[MAX_MOVES_PER_TURN];
    public KubaCell droppedDest[]=new KubaCell[MAX_MOVES_PER_TURN];
    public KubaCell lastCaptured[]=new KubaCell[MAX_MOVES_PER_TURN];
    public int undoInfo[]=new int[MAX_MOVES_PER_TURN];
	// encoding of "undoInfo" is a bit ad-hoc so we can fit everything
	//
	// into an integer.  The gutter is represented by a ring and indeces
	// are stored in excess 50 format so there will be no negative numbers
	private static final int GutterZero = 50;
	//
	// an undoInfo integer stores up to 5 small integers, with this radix
	// eeddccbbaa
	// aa is the direction of the move
	// bb is the number of balls pushed
	// cc is the gutter index of the last ball
	// dd is the gutter index of the first (newly dropped) ball
	// ee is the color index of the ball
	private static final int GutterRadix = 100;
    // true if undoInfo c represents a capturing move 
    boolean isACapture(int c)
    {   return(c>=(GutterRadix*GutterRadix));	
    }
    boolean isAFinalCapture(int c)
    {	if(isACapture(c))
    	{	int npushed = ((c/GutterRadix)%GutterRadix);
   		return(npushed==1);
    	}
    	return(false);
    }
    public int stackIndex = 0;
    
    /** get all the cells in the current move stack.  This is used by the
     * viewer to present a move trail
     */
    public Hashtable<KubaCell,String> stackOrigins()
    {	Hashtable<KubaCell,String> h = new Hashtable<KubaCell,String>();
    	for(int i=0;i<=stackIndex;i++)
    		{ KubaCell c = pickedSource[i]; 
    			if(c!=null) 
    			{ h.put(c,""+(i+1)); 
    			}
    		  KubaCell d = droppedDest[i];
    		  	if(d!=null)
    		  	{ h.put(d,""+(i+2));
    		  	}
    		}
    	return(h);
    }

    // temporary list of destination cells allocate as a resource for speed
    private KubaCell[][]tempDestResource = new KubaCell[6][];
    private int tempDestIndex=-1;
    private synchronized KubaCell []getTempDest() 
    	{ if(tempDestIndex>=0) { return(tempDestResource[tempDestIndex--]); }
    	  return(new KubaCell[DEFAULT_ROWS*DEFAULT_COLUMNS]);
    	}
    private synchronized void returnTempDest(KubaCell[]d) { tempDestResource[++tempDestIndex]=d; }

	// factory method
	public KubaCell newcell(char c,int r)
	{	return(new KubaCell(c,r));
	}
    public KubaBoard(String init,int map[]) // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = KUBAGRIDSTYLE; //coordinates left and bottom
    	setColorMap(map, 2);
        Random r = new Random(6454267);
        for(int i=LeftIndex;i<=BottomIndex;i++)
    		{	{
    			KubaCell grow[] = gutters[i];
    			for(int j=0;j<grow.length;j++) { grow[j]=new KubaCell(r,Gutters[i],j);}
    			}
    			{
    			KubaCell trow[] = trays[i];
    			for(int j=0;j<trow.length;j++) { trow[j]=new KubaCell(r,Trays[i],j); }
    			}	
        }

        doInit(init); // do the initialization 
     }

    public KubaBoard cloneBoard() 
	{ KubaBoard dup = new KubaBoard(gametype,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((KubaBoard)b); }

    // add a chip to the board and do associated bookkeeping
    private KubaChip addChip(KubaCell c,KubaChip chip)
    {	c.addChip(chip);	
       	return(chip);
    }
    // add a chip to the board, perform bookkeeping
    private KubaChip removeChip(KubaCell c)
    {	KubaChip chip = c.removeChip();
      	return(chip);
    }  
    private void Init_Standard(String game)
    {	if("kuba".equalsIgnoreCase(game)) { game = Traboulet_INIT; }
    	else if(Traboulet_INIT.equalsIgnoreCase(game)) 
    		{ 
    		}
    	else { throw G.Error(WrongInitError,game); }
        gametype = game;
        setState(KubaState.PUZZLE_STATE);
        reInitBoard(DEFAULT_COLUMNS,DEFAULT_ROWS); //this sets up the board and cross links
        reInit(gutters);
        reInit(trays);
         // set up a clockwise ring of gutter cells, left and top increasing, right and bottom decreasing
       {
       int gindex = 0;
       for(int i=0;i<DEFAULT_COLUMNS;i++)
       {	KubaCell c1 = gutters[LeftIndex][i];
       		KubaCell c2 = gutters[TopIndex][i];
       		c1.myIndex=gindex+GutterZero;
       		c2.myIndex=gindex+GutterZero+DEFAULT_COLUMNS;
       		allGutters[gindex+DEFAULT_COLUMNS]=c2;
       		allGutters[gindex++] = c1;
       }
       gindex+=DEFAULT_COLUMNS;
       for(int i=DEFAULT_COLUMNS-1;i>=0;i--)
       {	KubaCell c1 = gutters[RightIndex][i];
       		KubaCell c2 = gutters[BottomIndex][i];
       		c1.myIndex=gindex+GutterZero;
       		c2.myIndex=gindex+GutterZero+DEFAULT_COLUMNS;
       		allGutters[gindex+DEFAULT_COLUMNS]=c2;
       		allGutters[gindex++] = c1;
       }}
       
       
       for(int i=FIRST_PLAYER_INDEX;i<=SECOND_PLAYER_INDEX; i++)
         {
         capturedRed[i]=0;
         capturedOpp[i]=0;
         }

        // fill the board with the starting position
        for(int col=0;
        	col<boardColumns;
        	col++)
        {  	int init[]=KubaChip.STARTING_POSITION[col];
         for(int row=0;row<boardRows;row++)
        	{ int ini = init[row];
        	  if(ini>=0)
        		  {KubaChip ch = KubaChip.getChip(ini);
        		  KubaCell cell = getCell((char)('A'+col),row+1);
        		  if(ch!=null) { addChip(cell,ch); }
        	}}
        }
        whoseTurn = FIRST_PLAYER_INDEX;

    }
    public void sameboard(BoardProtocol f) { sameboard((KubaBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(KubaBoard from_b)
    {	super.sameboard(from_b);
  		G.Assert(AR.sameArrayContents(capturedOpp,from_b.capturedOpp),"capturedOpp mismatch");
  		G.Assert(AR.sameArrayContents(capturedRed,from_b.capturedRed),"capturedRed mismatch");
     	G.Assert((lastPushedDirection==from_b.lastPushedDirection)
    			&& ((lastPushed==from_b.lastPushed) || ((lastPushed!=null) && lastPushed.sameCell(from_b.lastPushed))),
    			"lastPushed same");
 
     	G.Assert(sameCells(trays,from_b.trays),"tray mismatch");
     	G.Assert(sameCells(gutters,from_b.gutters),"gutter mismatch");
     	
        G.Assert(stackIndex == from_b.stackIndex,"stack Index Matches");
        G.Assert(pickedObject == from_b.pickedObject,"picked matches");
        //removed because the robot doesn't maintain it properly
        //for(int i=0;i<=stackIndex;i++)
        //{	G.Assert( cell.sameCell(pickedSource[i],from_b.pickedSource[i])
        //				&& cell.sameCell(droppedDest[i],from_b.droppedDest[i])
        //				&& (undoInfo[i]==from_b.undoInfo[i]),
        //				"move stack matches");
        //}

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
        v ^= Digest(r,gutters);
        v ^= Digest(r,trays);        
		v ^= chip.Digest(r,pickedObject);
		v ^= cell.Digest(r,getSource());
		v ^= r.nextLong() * (board_state.ordinal()*10+whoseTurn);

     return (v);
    }
    
    public KubaCell getCell(KubaCell c)
    {	if(c==null) { return(null); }
    	switch(c.rackLocation())
    	{
    	case BoardLocation:	 
    		return(getCell(c.col,c.row)); 
    	case Gutter0:
    	case Gutter1:
    	case Gutter2:
    	case Gutter3: return(gutters[c.rackLocation().gutterIndex()][c.row]);
    	case Tray0:
    	case Tray1:
    	case Tray2:
    	case Tray3: return(trays[c.rackLocation().trayIndex()][c.row]);
    	default: 	throw G.Error("Can't rerecognize local copy of %s",c); 
    	}
    }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(KubaBoard from_b)
    {	super.copyFrom(from_b);
        
    	stackIndex = from_b.stackIndex;
        pickedObject = from_b.pickedObject;
        for(int i=0;i<=stackIndex;i++)
        {	pickedSource[i] = getCell(from_b.pickedSource[i]);
        	droppedDest[i] =  getCell(from_b.droppedDest[i]);
        	undoInfo[i] = from_b.undoInfo[i];
        }
        AR.copy(capturedRed, from_b.capturedRed);
        AR.copy(capturedOpp, from_b.capturedOpp);

        copyFrom(trays,from_b.trays);
        copyFrom(gutters,from_b.gutters);

	   lastPushed = getCell(from_b.lastPushed);
	   lastPushedDirection = from_b.lastPushedDirection;
       board_state = from_b.board_state;
       unresign = from_b.unresign;

       sameboard(from_b); 
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {	randomKey = key;
       Init_Standard(gtype);
       moveNumber = 1;
       stackIndex = 0;
       lastPushedDirection=-1;
       lastPushed=null;
       pickedObject = null;
       AR.setValue(pickedSource, null);
       AR.setValue(droppedDest, null);
       AR.setValue(undoInfo, 0);

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
        case PLAY2_STATE:
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
         case PLAY2_STATE:
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
    	setState(KubaState.GAMEOVER_STATE);
    }
 
    // look for a win for player. 
    public boolean WinForPlayerNow(int player)
    {	if(board_state==KubaState.GAMEOVER_STATE) { return(win[player]); }
    	return((capturedRed[player]>=7) 
    			|| (capturedOpp[player]==8)
    			|| hasNoLegalMoves(nextPlayer[player])
    			);
     }


    // look for a win for player.  
    public double ScoreForPlayer(int player,boolean print,boolean dumbot)
    {  	// dumbest possible evaluator, probably a good start for this game
    	// point_strength measures the number of choices available
    	// separation measures distance between our and opponents pieces, we want to close..
    	return(capturedRed[player]+2*capturedOpp[player]);
     }

    public void unCaptureBall(int who,KubaChip ball)
    {
    	switch(ball.colorIndex())
    	{
    	default: throw G.Error("Not expected");
    	case KubaChip.WHITE_BALL_INDEX:
    	case KubaChip.BLACK_BALL_INDEX:	
    		{
        	capturedOpp[who]--;
        	KubaCell ce = colorTrays[who][capturedOpp[who]];
    		ce.removeChip(ball);
    		}
    		break;
    	case KubaChip.RED_BALL_INDEX:
    		{
        	capturedRed[who]--;
    		KubaCell ce = redTrays[who][capturedRed[who]];
    		ce.removeChip(ball);
    		}
    		break;
    	}
    }  
    public KubaCell captureBall(int who,KubaChip ball)
    {
    	switch(ball.colorIndex())
    	{
    	default: throw G.Error("Not expected");
    	case KubaChip.WHITE_BALL_INDEX:
    	case KubaChip.BLACK_BALL_INDEX:	
    		{
    		KubaCell ce = colorTrays[who][capturedOpp[who]];
    		ce.addChip(ball);
    		capturedOpp[who]++;
    		return(ce);
    		}
    		
    	case KubaChip.RED_BALL_INDEX:
    		{
    		KubaCell ce = redTrays[who][capturedRed[who]];
    		ce.addChip(ball);
    		capturedRed[who]++;
    		return(ce);
    		}
    	}
    }
    public void finalizePlacement(replayMode replay)	// finalize placement, no more undo
    {	int firstCap = 1000;
    	int lastCap = -1;
    	lastPushed = null;
    	lastPushedDirection = -1;
    	if(stackIndex>0)
    	{	// record information for the pushback rule
    		int undo = undoInfo[stackIndex-1];
    		if(!((board_state==KubaState.PUZZLE_STATE) || isACapture(undo)))
    		{	
    			KubaCell c = droppedDest[stackIndex-1];
    			int dir = undo%GutterRadix;
    			int ns = undo/GutterRadix;
    			if(ns>0) 
    			{ while(ns-- > 0) { c = c.exitTo(dir); }
    			  lastPushed = c;
    			  lastPushedDirection = dir;
    			} 
    		}
    	}
    	
    	while(stackIndex>=0)
    	{   pickedObject = null;
        	droppedDest[stackIndex]  = null;
        	pickedSource[stackIndex]  = null;
        	int undo = undoInfo[stackIndex];
        	if(isACapture(undo)) 
        		{ int cap = undo/(GutterRadix*GutterRadix);
        		  int fc = (cap/GutterRadix)%GutterRadix;
        		  int lc = cap%GutterRadix;
        		  firstCap = Math.min(firstCap,fc); 
        		  firstCap = Math.min(firstCap,lc);
        		  lastCap = Math.max(lastCap,fc);
        		  lastCap = Math.max(lastCap,lc); 
        		}
        	undoInfo[stackIndex] = 0;
        	stackIndex--;
    	}
    	if(lastCap>=0)
    	{	while(firstCap<=lastCap)
    		{ KubaCell gchip = gutterCell(firstCap);
    		  if(gchip.chip!=null) 
    		  	{ KubaChip gc = gchip.removeChip();
    		  	  KubaCell dest = captureBall(whoseTurn,gc);
    		  	  if((replay.animate) && (dest.centerX()>0))
    		  	  {	// the extra cells beyone a win are never displayed
    		  		  animationStack.push(gchip);
    		  		  animationStack.push(dest);
    		  	  }
    		  	}
    		  firstCap++;
    		}
    	}
    	stackIndex = 0;
    }
    
    // lowest level of uncapture.  "removed" is the most distant cell removed by the capture
    // picked and dropped are the source and destination respectively.  This handles both
    // capture by approach and by withdrawal.
    // call this after the pick-drop has been undone, so "picked" contains the color moving

    private void unCaptureObject(KubaCell c,int info,boolean completed)
    {	int removed = info;
    	int cap = removed/(GutterRadix*GutterRadix);
    	int pushed = removed%(GutterRadix*GutterRadix);
    	int npushed = pushed/GutterRadix;
    	int direction = pushed%GutterRadix;
    	if((npushed>0)||(cap>0))
    	{
    	KubaCell d = c.exitTo(direction);
    	//System.out.println("Undo direction="+direction+" n="+npushed+" cap="+cap);
    	while(npushed>((cap==0)?0:1))
    	{	KubaCell e = d.exitTo(direction);
    		KubaChip top = e.removeChip();
    		d.addChip(top); 
    		d = e;
    		npushed--;
    	}
    	if(cap!=0)
    	{	int removeFrom = cap%GutterRadix;
    		int removeTo = (cap/GutterRadix)%GutterRadix;
    		int removedColor = cap/(GutterRadix*GutterRadix);
       		KubaCell r1 = gutterCell(removeTo);
       	 	if(completed)
    		{	KubaChip ch = KubaChip.getChip(removedColor);
    			unCaptureBall(whoseTurn,ch);
    			r1.addChip(ch);
    		}
    		KubaChip top = r1.removeChip();
    		G.Assert(top.colorIndex()==removedColor,"Correct color");
    		d.addChip(top);
    		if(!completed) { slideRow(d,removeTo,removeFrom,null,replayMode.Replay); }
    	}}
    }


    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    KubaCell dr = droppedDest[stackIndex];
    if(dr!=null)
    	{
    	droppedDest[stackIndex] =  null;
    	pickedObject = removeChip(dr);
    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	KubaChip po = pickedObject;
    	if(po!=null)
    	{
        KubaCell ps = pickedSource[stackIndex];
       	addChip(ps,po);
        unCaptureObject(ps,undoInfo[stackIndex],false);
        pickedSource[stackIndex]=null;
    	pickedObject = null;
  	}
     }
    
    private void dropOnBoard(KubaCell dcell)
    {
    	droppedDest[stackIndex] = dcell;
    	undoInfo[stackIndex]=0;
    	addChip(dcell,pickedObject);
    	pickedObject = null;
    }
    
    // drop a ball and push others out of the way, returns the number of balls pushed
    private int dropAndPush(KubaCell from,KubaCell to,replayMode replay,boolean skip)
    {	// initial "from" address has already been pushed on the animation stack
    	int distance = 0;
       	droppedDest[stackIndex]= to;
       	int dir = findDirection(from.col,from.row,to.col,to.row);
    	distance  = dropAndPush(from,pickedObject,to,dir,replay,skip)*GutterRadix+dir;
       	undoInfo[stackIndex]= distance;
       	pickedObject = null;
       	//System.out.println("DropAndPush "+from+to+" =  "+distance);
       	return(distance);
    }

    // fetch the gutter cell at "index" relative to row, treating
    // the space as a ring of 4 gutters.  We use an artificial
    // offset of GutterZero to shift the range of gutter indeces
    // so we don't have to contend with negative indeces when we
    // encode several indeces into an integer.
    KubaCell gutterCell(int ii)
    {  	int index = ii-GutterZero;
    	if(index<0) { index+=allGutters.length; }
    	if(index>=allGutters.length) { index -= allGutters.length; }
    	return(allGutters[index]);
    }
    // slide a row of balls in the gutter, so the point where we drop
    // will hold the most recently dropped ball.  "start" is the
    // index of an empty cell, and "end" is the index of the cell
    // closest to the board cell we're exiting.
    int slideRow(KubaCell from,int start,int end,KubaChip drop,replayMode replay)
    {	int empty = start;
    	int s = Integer.signum(empty-end);
    	while(empty!=end) 
    	{ KubaCell dest = gutterCell(empty);
    	  KubaCell src = gutterCell(empty-s);
    	  if(replay.animate)
    	  {
    		  animationStack.push(src);
    		  animationStack.push(dest);
    	  }
    	  dest.addChip(src.removeChip()); 
    	  empty -= s; 
    	}
    	KubaCell dest =  gutterCell(end);
    	if(replay.animate)
    	{
    		animationStack.push(from);
    		animationStack.push(dest);
    	}
    	if(drop!=null) { dest.addChip(drop); } 
    	return(start);		// this is the index of the last ball in motion
    }
    
    // drop in the gutter adjacent to "chip", "c" is the gutter adjacent to the edge
    // return the start index combined with the index where the last ball was rolled to,
    // which is the essential information to implement an undo.
    int dropInGutterNear(KubaCell from,KubaChip chip,KubaCell c,replayMode replay)
    {	int center = c.myIndex;
    	for(int i=0;i<allGutters.length;i++)
    	{	if(gutterCell(center+i).topChip()==null)
    			{ return(100*center+slideRow(from,center+i,center,chip,replay)); };
    		if(gutterCell(center-i).topChip()==null) 
    			{ return(100*center+slideRow(from,center-i,center,chip,replay)); };
    	}
    	throw G.Error("Not expecting this exit");
    	
    }
    // return an integer code which indicates which direction we exited, and where the
    // last ball landed.
    int dropInGutterNear(KubaCell from,KubaChip chip,KubaCell to,int direction,replayMode replay)
    {	switch (direction)
    	{
    	default: throw G.Error("Not expecting direction %s",direction);
    	case CELL_UP:	
    		return(dropInGutterNear(from,chip,gutters[TopIndex][to.col-'A'],replay)); 
    	case CELL_RIGHT:
    		return(dropInGutterNear(from,chip,gutters[RightIndex][to.row-1],replay));
    	case CELL_DOWN:
    		return(dropInGutterNear(from,chip,gutters[BottomIndex][to.col-'A'],replay));
     	case CELL_LEFT:
    		return(dropInGutterNear(from,chip,gutters[LeftIndex][to.row-1],replay));
    	}
    }
    int dropAndPush(KubaCell from,KubaChip move,KubaCell to,int dir,replayMode replay,boolean skip)
    {	int distance = 0;
    	KubaChip top = to.topChip();
    	if(replay.animate && !skip)
    	{
    		animationStack.push(from);
    		animationStack.push(to);
    	}
    	if(top!=null)
     	{	
    		KubaCell next = to.exitTo(dir);
			to.removeChip();
			if(next!=null) 
    		{ // continue pushing on the board
				distance = 1 + dropAndPush(to,top,next,dir,replay,false); 
    		}
    		else { // push off the edge
    			distance = 1 + dropInGutterNear(from,top,to,dir,replay)*GutterRadix + top.colorIndex()*(GutterRadix*GutterRadix*GutterRadix);
  		}
     	}
    	to.addChip(move);
    	return(distance);		// we pushed n balls
    }
    // 
    // drop the floating object.
    //
    private void dropObject(KubaId dest, char col, int row)
    {
       G.Assert((pickedObject!=null)&&(droppedDest[stackIndex]==null),"ready to drop");
       switch (dest)
        {
        default: throw G.Error("Not expecting dest %s",dest);
        case Tray0:
        case Tray1:
        case Tray2:
        case Tray3:
        	dropOnBoard(trays[dest.trayIndex()][row]);
        	break;
        case Gutter0:
        case Gutter1:
        case Gutter2:
        case Gutter3:
        	dropOnBoard(gutters[dest.gutterIndex()][row]);
        	break;
        case BoardLocation: // an already filled board slot.
        	dropOnBoard(getCell(col,row));
         	break;
        case Black_Chip:		// back in the pool
        case White_Chip:		// back in the pool
        	finalizePlacement(replayMode.Replay);	// only in puzzle state
            break;
        }
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(KubaCell cell)
    {	return((stackIndex>0)&&(droppedDest[stackIndex-1]==cell));
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    {	KubaChip ch = pickedObject;
    	if((ch!=null)&&(droppedDest[stackIndex]==null))
    		{ return(ch.pieceNumber());
    		}
        return (NothingMoving);
    }
  
    private void pickFromBoard(KubaCell src)
    {  	pickedSource[stackIndex] = src;
    	undoInfo[stackIndex]=0;
    	pickedObject = removeChip(src);
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(KubaId source, char col, int row)
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
        case Gutter0:
        case Gutter1:
        case Gutter2:
        case Gutter3:
        	pickFromBoard(gutters[source.gutterIndex()][row]);
        	break;
        
        case Tray0:
        case Tray1:
        case Tray2:
        case Tray3:
        	pickFromBoard(trays[source.trayIndex()][row]);
        	break;
               	
       }
   }


    //
    // this is called by the robot, which never balks or undoes a move.
    // ..so we must not consider the possibility.
    //
    private void setForwardStateAfterDrop(boolean cap,replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case DRAW_STATE:
        	if(droppedDest!=null) { setNextStateAfterDone(); }
        	break;
        case CONFIRM_STATE:
        	if(replay==replayMode.Live) 
        		{	// allow some damaged games with double push to continue
        		throw G.Error("Not expecting drop in state %s", board_state);
        		}
			//$FALL-THROUGH$
		case PLAY2_STATE:
        case PLAY_STATE:
        	stackIndex++;
        	// if we captured, allow another move
        	setState(cap ? KubaState.PLAY2_STATE : KubaState.CONFIRM_STATE);	// 
        	break;

        case PUZZLE_STATE:
			finalizePlacement(replayMode.Replay);
            break;
        }
	
    }
    
    // choose the next state, which will be "confirm" if no further captures
    // are possible, and "play2" if additional captures are possible.
    private void setNextStateAfterCapture(KubaCell dst)
    {
       	setState(KubaState.PLAY2_STATE);	
    }
    // drop a marble, but decide if the drop is a balk or an undo
    private void setNextStateAfterDrop(boolean cap,replayMode replay)
    {
        KubaCell src = pickedSource[stackIndex];
        KubaCell dst = droppedDest[stackIndex];
        if(src==dst) 
        { unDropObject();
          unPickObject(); 
        }
        else if((stackIndex>0) 
        		&& (dst==pickedSource[stackIndex-1])
        		// the unfortunate case is where the previous move pushed one
        		// ball into the gutter, and the current move slides the pushing
        		// ball back.  In this one case, it's not possible to distingush
        		// between an "undo" and a final non-capture move.  We don't want
        		// it to be treated as an undo
        		&& !isAFinalCapture(undoInfo[stackIndex-1])	// not a capture
        		)
        	{	// single step undo
        		unDropObject();
        		unPickObject();
        		stackIndex--;
        		unDropObject();
        		unPickObject();
        		if(stackIndex>0) { setNextStateAfterCapture(dst); } else { setNextStateAfterDone(); }
        	}
        else
        {
        setForwardStateAfterDrop(cap,replay);
        }
    }
    public KubaCell getSource()
    {
    	return(pickedSource[stackIndex]);
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
   			setState((stackIndex>0) ? KubaState.PLAY2_STATE : KubaState.PLAY_STATE); 
        	break;
        case CONFIRM_STATE:
        case PLAY_STATE:
        case PLAY2_STATE:
        case PUZZLE_STATE:
   		 
            break;
        }
    }
    
    // current player is the new player
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
 		case PLAY2_STATE:
    	case PLAY_STATE:
      	case PUZZLE_STATE:
       		setState(KubaState.PLAY_STATE);
       		break;
    	}

    }
   
    private void doDone(replayMode replay)
    {	
        finalizePlacement(replay);

        if (board_state==KubaState.RESIGN_STATE)
        {	setGameOver(false,true);
        }
        else
        {	boolean win1 = WinForPlayerNow(whoseTurn);
        	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
        	if(win1 || win2)  { setGameOver(win1,win2); }
        	else {setNextPlayer(); setNextStateAfterDone(); }
        }
    }
    // count the number of chips in the row
    private int countAndCompactRow(KubaCell row[])
    {	int count = 0;
    	for(int i=0;i<row.length;i++)
    	{	if(row[i].chip!=null) 
    		{ if(i>count) 
    			{	// compact as well as count
    			row[count].chip = row[i].chip; row[i].chip = null; 
    			}
    		  count++;
    		}
    	}
    	return(count);
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	KubaMovespec m = (KubaMovespec)mm;
        //System.out.println("E "+m+" for "+whoseTurn+" : "+stackIndex);
        switch (m.op)
        {
        case MOVE_DONE:
         	doDone(replay);

            break;
         case MOVE_BOARD_BOARD:
        	 {
             KubaCell from = getCell(m.from_col, m.from_row);
        	 KubaCell to = getCell(m.to_col, m.to_row);
        	 pickFromBoard(from);
        	 int distance = dropAndPush(from,to,replay,false);
        	 m.undoInfo = distance;
        	 // avoid the logic in setnextstateafterdrop that does undo
        	 setForwardStateAfterDrop(distance>=(GutterRadix*GutterRadix),replay);	
        	 }
        	break;
        case MOVE_DROPB:
        	{
        	G.Assert(pickedObject!=null,"something is moving");
        	KubaCell to = getCell(m.to_col, m.to_row);
          	KubaCell from = pickedSource[stackIndex];
          	int distance = 0;
          	if(from==to) 
          	{
          	 dropOnBoard(to);	
          	}
          	else 
          	{
        	distance = dropAndPush(from,to,replay,replay==replayMode.Live);
          	}
          	m.undoInfo = distance;
        	setNextStateAfterDrop(isACapture(distance),replay);
        	}
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary pick
        	switch(board_state)
        	{	
           	case CONFIRM_STATE:
           	default:
        		pickObject(KubaId.BoardLocation, m.from_col, m.from_row);
        	}
        	setNextStateAfterPick();
        	 
            break;
        case MOVE_DROPT:
        case MOVE_DROPG: // drop on chip pool;
            dropObject(m.source, m.to_col, m.to_row);
            setNextStateAfterDrop(false,replay);

            break;
        case MOVE_PICKT:
        case MOVE_PICKG:
            unDropObject();
            unPickObject();
            pickObject(m.source, m.from_col, m.from_row);
            setNextStateAfterPick();
            break;

 
        case MOVE_START:
            setWhoseTurn(m.player);
            finalizePlacement(replayMode.Replay);
            setState(KubaState.PUZZLE_STATE);
            for(int pl = FIRST_PLAYER_INDEX;pl<=SECOND_PLAYER_INDEX;pl++)
            { capturedRed[pl] = countAndCompactRow(redTrays[pl]);
              capturedOpp[pl]=countAndCompactRow(colorTrays[pl]);
            }
                                         
            {	boolean win1 = WinForPlayerNow(whoseTurn);
            	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(); 
            	}
            }
            break;

        case MOVE_RESIGN:
            setState(unresign==null?KubaState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
        	finalizePlacement(replayMode.Replay);
            setState(KubaState.PUZZLE_STATE);

            break;
            
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(KubaState.GAMEOVER_STATE);
			break;

        default:
        	cantExecute(m);
        }


        return (true);
    }
    public boolean canHitGutters()
    {	return(board_state==KubaState.PUZZLE_STATE);
    }
    public boolean LegalToHitCell(KubaCell c)
    {
        switch (board_state)
        {
        default:
 			return(false);
        case PUZZLE_STATE:
        	return((pickedObject==null)==((c.topChip()!=null)));
        }
    }
    int playerIndex(KubaChip ch) 
    {	int ind = ch.colorIndex();
    	return(ind<=1 ? getColorMap()[ind] : ind);
    }
    
    // true if it's legal to drop chip  originating from fromCell on toCell
    public boolean LegalToPickOrDrop(KubaCell fromCell,KubaChip gobblet,KubaCell toCell)
    {	boolean val = false;
    	if(toCell==null)
    	{throw G.Error("not expected");
    	}
    	else if(fromCell!=null)
    	{	KubaCell temp[] = getTempDest();
		KubaChip picked = pickedObject;
		int n= freeDestsFrom(fromCell,playerIndex(picked),temp,0);
		for(int i=0;i<n;i++) { if (temp[i]==toCell) { val = true;  break; }}
		returnTempDest(temp);
    	}
    	else
    	{	KubaChip top = toCell.topChip();
    		if((top!=null) && (playerIndex(top)==whoseTurn))
    		{
    		int n = freeDestsFrom(toCell,playerIndex(top),null,0);
    		val = (n>0);
    		}
    	}
		return(val);
    }
    private boolean reconsidering(KubaCell cell)
    {	return((cell==droppedDest[stackIndex]) 
				|| ((stackIndex>0) && (droppedDest[stackIndex-1]==cell))
				|| ((stackIndex>0) && (pickedObject!=null) && (pickedSource[stackIndex-1]==cell))
				|| (cell==pickedSource[stackIndex]));
    }
    public boolean LegalToHitBoard(KubaCell cell)
    {	
        switch (board_state)
        {
 		case PLAY2_STATE:
		case PLAY_STATE:
			if(reconsidering(cell))	
				{ return(true); }
			return(LegalToPickOrDrop(pickedSource[stackIndex],pickedObject,cell));

		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
		case DRAW_STATE:
			if(reconsidering(cell) || isDest(cell))
			{ return(reconsidering(cell) || isDest(cell));
			};
			return(false);
        default:
        	throw G.Error("Not expecting state %s", board_state);

        case PUZZLE_STATE:
        	return(pickedObject==null?(cell.chip!=null):cell.canDrop(pickedObject));
        }
    }

 
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(KubaMovespec m)
    {	//System.out.println("R "+m+" for "+whoseTurn);
        m.state = board_state ; //record the starting state. The most reliable
        m.undoDirection = (lastPushedDirection+1);
        m.stack = stackIndex;
        m.lastPushed = lastPushed;
                // to undo state transitions is to simple put the original state back.
       G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
       
       //G.print("R "+m);
       if(Execute(m,replayMode.Replay))
       {
    	  switch(board_state)
    	  {	
    	  case DRAW_STATE:
    	  case CONFIRM_STATE:	
     		  doDone(replayMode.Replay); break;
    	  
    	  case PLAY_STATE:
    	  case GAMEOVER_STATE:
    		  break;
    	  case PLAY2_STATE:	
    		  finalizePlacement(replayMode.Replay);	// push captured balls off
    		  break;
    	  
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
    public void UnExecute(KubaMovespec m)
    {
       // G.print("U "+m);

        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;

        case MOVE_DONE:
            break;
        case MOVE_BOARD_BOARD:
       		{ KubaCell src = getCell(m.to_col,m.to_row);
       		  KubaCell dest = getCell(m.from_col,m.from_row);
       		  dest.addChip(src.removeChip());
       		  int info = m.undoInfo;
       		  unCaptureObject(dest,info,true);
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        lastPushedDirection=m.undoDirection-1;
        setState(m.state);
        stackIndex = m.stack;
        lastPushed = m.lastPushed;
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
  private KubaChip captureInDirection(KubaCell c,int direction)
  {	KubaChip top = c.topChip();
  	if(top==null) { return(null); }	// empty cell, nothing captures
  	KubaCell nx = c.exitTo(direction);
  	if(nx==null) 
  		{ return(top); // next is the edge, this cell is what is captured
  		}	
  	return(captureInDirection(nx,direction));	// tail recursion
  }
  private boolean isAPushBack(KubaCell c,int opp)
  {	// lastPushed is the end cell from the last push operation.
	// the condition of stackIndex==0 detects that only the first move in a sequence
	// can be a pushback.
	boolean val = (stackIndex==0) 
	  			&& (lastPushed==c)
	  			&& (lastPushedDirection==(opp%CELL_FULL_TURN));
   	return( val );  
  }
  
  private int freeDestsFrom(KubaCell c,int forPlayer,KubaCell dests[],int idx)
    {	    	
    	for(int direction=1; direction<CELL_FULL_TURN; direction+=CELL_QUARTER_TURN)
    	{
    	KubaCell nx = c.exitTo(direction);
    	if(nx!=null)
    		{
    		int opp = direction+CELL_HALF_TURN;
    		KubaCell px = c.exitTo(opp);	// opposite direction
    		if( ((px==null) ||(px.topChip()==null))
    			&& !isAPushBack(c,opp))
    		{	KubaChip cap = captureInDirection(nx,direction);
    			if((cap==null) || (playerIndex(cap)!=forPlayer))
    			{	// edge of the board, or an empty space, and captures nothing or a different color
    			if(dests!=null) 
    				{ dests[idx] = nx; 
    				}	
    			idx++;
    			}
    		}
    		}
    	}
    	return(idx);
    }

 
 private void freeMovesFor(int player,CommonMoveStack  result)
 {	KubaCell temp[] = getTempDest();
	for(KubaCell c = allCells;
		c!=null;
		c = c.next)
	{
		KubaChip ctop = c.topChip();
		if((ctop!=null) && (playerIndex(ctop)==player))
		{	int idx = freeDestsFrom(c,player,temp,0);
			for(int i=0;i<idx;i++) 
			{	KubaCell dst = temp[i];
				result.addElement(new KubaMovespec(MOVE_BOARD_BOARD,c.col,c.row,dst.col,dst.row,whoseTurn));
			}
		}
	}
	returnTempDest(temp);
}
 
private boolean hasNoLegalMoves(int player)
{
	for(KubaCell c = allCells;
		c!=null;
		c = c.next)
	{
		KubaChip ctop = c.topChip();
		if((ctop!=null) && (playerIndex(ctop)==player))
		{	int idx = freeDestsFrom(c,player,null,0);
			if(idx>0) { return(false); }
		}
	}
	return(true);	
}
 
CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
  	switch(board_state)
 	{
 	case CONFIRM_STATE:
	case DRAW_STATE:
		all.addElement(new KubaMovespec(MOVE_DONE,whoseTurn));	// stopping now is an option
		break;
	case PLAY2_STATE:
	case PLAY_STATE:
		freeMovesFor(whoseTurn,all);
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
 	else { ypos += cellsize/3; }
 	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
 }
}
