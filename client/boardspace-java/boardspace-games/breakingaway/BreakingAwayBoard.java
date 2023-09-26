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
package breakingaway;

import java.awt.Point;
import java.awt.Rectangle;
/* below here should be the same for codename1 and standard java */
import online.game.*;
import lib.*;

import java.util.Hashtable;
import java.util.StringTokenizer;

import breakingaway.BreakingAwayConstants.BreakId;
import breakingaway.BreakingAwayConstants.BreakState;
import breakingaway.BreakingAwayConstants.StateStack;
import breakingaway.BreakingAwayConstants.Variation;

import static breakingaway.BreakingAwayMovespec.*;
/**
 * BreakingAwayBoard knows all about the game BreakingAway, which is played on a
 * circular track with 40 segments.   This board representation is unusual in many
 * respects.  There is no formal "board" with a 1-1 correspondence between squares
 * and piece positions.  The "cycle" array has a cell for each rider, which holds
 * his current position.  
 * <p>The col,row coordinates of these cells are mutable and change to reflect the 
 * riders current position, rather than the usual arrangement where the riders are
 *  deposited into the current cell.
 * <p>The cell's row represents the position along the track, the col represents
 * the position from inner to outer edge of the track.  This is used in display
 * but has no consequences in the race logic.
 * <p>The riders are deposited into the 40 space track modulo their cell's 
 * position, which is not strictly limited to 0-100.   Riders start at -3 to 0
 * and continue past 100 until 8 riders have finished.
 * <p>Screen coordinates are based on radial lines that were mapped with photoshop,
 * cell column is used to interpolate between inner and outer edges of the appropriatge line.
 * <p>The picture used to represent the riders depends on the row they are in. This is used
 * to give the riders apparent rotation as they move around the track.  There are only a few
 * actual rider images - the rest are faked by compressing or expanding the x axis of the image.
 * <p>riders don't move in a predetermined order, everything changes depending on the current player order on the track.
 * <p>there is an initial "adjustment" phase where all players make changes simultaneously. To accomodate standard
 * robot behavior which only make moves when it is the robot's turn, the "current player" is maintained as the first
 * player who hasn't finished making adjustments.  So eventually it will be the robot's turn.
 * 
 * @author ddyer
 * 
 */

class BreakingAwayBoard extends RBoard<BreakingAwayCell> implements BoardProtocol
{ 	
	static final String[] BREAKINGAWAYGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	static final int MINPLAYERS = 3;
	static final int MAXPLAYERS = 6;
	static final int TRACK_ROWS = 40;
	static final int CYCLES_PER_PLAYER = 4;
	static final int SPRINT1_LINE = 33;
	static final int SPRINT2_LINE = 33+TRACK_ROWS;
	static final int FINISH_LINE = 100;
	static final int SPRINTPOINTS[] = {10,8,6,5,4,3,2,1};
	static final int FINISHPOINTS[] = {20,16,12,10,8,6,4,2};
    // initial movement totals for each rider
    static final int MOVEMENTSUM[] = { 30, 25, 20, 16 };
	   
    static int raw_points[] = {
    //row   x        y      x2		y2		man compress   
    0,     538,     146,	573,     267,	0,	0,
    1,     565,     146,	632,     265,   5,	-30,
    2,     581,     143,    697,     252,	5,	-15,
    3,     591,     140,    743,     231,	5,	5,
    4,     599,     136,	770,     200,	5,	40, 
    5,     602,     130,    775,     165,	1,	50,
    6,     603,     124,    765,     135,	1,	30,
    7,     600,     119,    738,     109,	1,	20,
    8,     596,     113,    707,      88, 	1,	10,
    9,     587,     108,	673,      72,	1,	0,
   10,     577,     103,    633,      59,	1,	-35,
   11,     563,     100,    588,      49,	1,	-45,
   12,     548,      99,	543,      45,	2,	15,
   13,     522,      99,    509,      43,	2,	0,
   14,     490,      99,    481,      44,	2,	0,
   15,     459,      99,    456,      43,	2,	0, 
   16,     426,      99,	427,      42,	2,	0, 
   17,     390,     100,    397,      43,	2,	0,
   18,     357,     100,    370,      42,	2,	0,
   19,     326,      99,    345,      43,	2,	0,
   20,     296,     100,    320,      43,	2,	0,
   21,     274,     100,    287,      43,	2,	20,
   22,     259,     100,    245,      48,	3,	-30,
   23,     244,     103,    199,      56,	3,	0,
   24,     232,     109,	158,      69,	3,	20,
   25,     224,     114,    114,      87,	3,	30,
   26,     219,     119,     79,     108,	3,	40,
   27,     216,     125,     49,     134,	3,	50,
   28,     216,     129,     32,     164,	4,	50, 
   29,     219,     135,     30,     193,	4,	25,
   30,     227,     139,	 48,     224,	4,	0,
   31,     238,     143,     95,     250,	4,	-20, 
   32,     252,     145,    165,     266,	4,	-40,
   33,     276,     145,    228,     268,	0,	0, 
   34,     308,     145,	278,     269,	0,	0,
   35,	   344,     145,    321,     269,	0,	0,
   36,     384,     145,    371,     270,	0,	0,
   37,     425,     145,    425,     269,	0,	0,
   38,     463,     144,    475,     267,	0,	0, 
   39,     502,     145,    522,     269,	0,	0
   };


	BreakState unresign;
	BreakState board_state;
	public BreakState getState() {return(board_state); }
	private void setState(BreakState st) 
	{ 	unresign = (st==BreakState.RESIGN_STATE)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public CellStack animationStack = new CellStack();
    static private double inner_points[][] = new double[TRACK_ROWS][2];	// these are the lines where the cyclists line up
    static private double outer_points[][] = new double[TRACK_ROWS][2];	// scaled so 1.0 is full height
    static private int row_cycle_index[] = new int[TRACK_ROWS];		// which picture to use for riders in each row
    static private int row_compress[] = new int[TRACK_ROWS];			// how much to compress or expand the picture X axis
    public int rowCycleIndex(int row) { return(row_cycle_index[Math.max(row+((reverse_x!=reverse_y)?20:0),0)%40]); }  // get the image index for any row
    public int rowCompress(int row) { return(row_compress[Math.max(row+((reverse_x!=reverse_y)?20:0),0)%40]); }		 // get the stretch factor for any row
    public boolean reverse_y=false;
    public boolean reverse_x=false;
    public int ridersInRow[] = new int[FINISH_LINE*2];	// maintained to make static evaluation effecient
    private Rectangle boardCenter = new Rectangle();		// maps the placement of the track within the current board rectangle
    public BreakingAwayCell allRidersByY[] = null;		// riders sorted by Y axis
    public BreakingAwayCell cycleDests[] = new BreakingAwayCell[4];	// temp for display

    public BreakingAwayCell allRidersInOrder[] = null;	// riders in movement order
    public int riderTurnOrder = 0;						// index into allRidersInOrder
    private IStack undoMovement = new IStack();			// keep track of movements for the robot undo
    private CellStack undoSort = new CellStack();				// keep track of player order for the robot undo
    private boolean doneAdjusting[] = null;				// flags for which players have finished adjusting
    boolean doneAdjustingUI[] = null;			// flags for which players have finished adjusting in the UI
    boolean visibleInMainUI[] = null;
    boolean visibleInHiddenUI[] = null;
    public boolean showMovements[] = null;
    public boolean showHiddenMovements[] = null;
    public void SetDrawState() { throw G.Error("Not expected"); }
    private Variation variation = Variation.Standard;
	public BreakingAwayCell cycles[][] = null;		// this is the main root of cycle state
    public int pointsPerPlayer[]= null;
    public int ridersAcrossFinish = 0;
    public int ridersAcrossSprint1 = 0;
    public int ridersAcrossSprint2 = 0;
    public int timeStep = 0;
    public int maxTimeStep = 0;
    private StateStack robotState = new StateStack();
    private IStack robotUndo = new IStack();
    public boolean robotBoard = false;
    
    public Point encodeCellPosition(int x,int y,double cellsize)
    {  	BreakingAwayCell mincell = closestCellB(x,G.Height(boardRect)-y);
     	if(mincell!=null)
     	{	
     		// encode the subcell position in tenths of a cell, chinese remainder
     		// the cell number with the subcell position.  This isn't quite correct
     		// because it should also consider differences in rotation, but we don't
     		// use vastly different rotations, so it's pretty much ok
     		//
     		return(new Point(Math.max(0,(mincell.col-'A')),mincell.row));
     	}
     	return(null);
    }
    /** decode a cell position (encoded by {@link #encodeCellPosition})
     * to an x,y inside the board rectangle
     */
   public Point decodeCellPosition(int x,int y,double cellsize)
   {	Point val = null;
		char col = (char)('A'+x);
   			int row = y;
   			{	int cx = BCtoX(col,row);
   				int cy =(G.Height(boardRect)-BCtoY(col,row));
   				val = new Point(cx,cy);
   			}
   		return(val);
   }
    //
    // award points for crossing the sprint and finish lines
    //
    public void scorePlayer(int pl,int from,int to)
	{	if(to>FINISH_LINE) 
			{
			if(from<=FINISH_LINE) 
				{ if(ridersAcrossFinish<FINISHPOINTS.length)
					{pointsPerPlayer[pl]+= FINISHPOINTS[ridersAcrossFinish];
					}
				  ridersAcrossFinish++;
				}
			}
		else if(to>SPRINT2_LINE) 
		{
		if(from<=SPRINT2_LINE) 
			{ if(ridersAcrossSprint2<SPRINTPOINTS.length)
				{pointsPerPlayer[pl]+= SPRINTPOINTS[ridersAcrossSprint2];
				}
			  ridersAcrossSprint2++;
			}
		}	
		else if(to>SPRINT1_LINE) 
		{
		if(from<=SPRINT1_LINE) 
			{ if(ridersAcrossSprint1<SPRINTPOINTS.length)
				{pointsPerPlayer[pl]+= SPRINTPOINTS[ridersAcrossSprint1];
				}
			  ridersAcrossSprint1++;
			}
		}
	}
    // 
    // undo the scoring, used by the robot to unwind scoring moves.
    //
    public void unScorePlayer(int pl,int from,int to)
	{	if(to>FINISH_LINE) 
			{
			if(from<=FINISH_LINE) 
				{ ridersAcrossFinish--;
				  if(ridersAcrossFinish<FINISHPOINTS.length)
					{pointsPerPlayer[pl]-= FINISHPOINTS[ridersAcrossFinish];
					}
				  
				}
			}
		else if(to>SPRINT2_LINE) 
		{
		if(from<=SPRINT2_LINE) 
			{ ridersAcrossSprint2--;
			  if(ridersAcrossSprint2<SPRINTPOINTS.length)
				{pointsPerPlayer[pl]-= SPRINTPOINTS[ridersAcrossSprint2];
				}
			  
			}
		}	
		else if(to>SPRINT1_LINE) 
		{
		
		if(from<=SPRINT1_LINE) 
			{ ridersAcrossSprint1--;
			  if(ridersAcrossSprint1<SPRINTPOINTS.length)
				{pointsPerPlayer[pl]-= SPRINTPOINTS[ridersAcrossSprint1];
				}
			  
			}
		}
	}
   
     static {
    		// convert the raw points as drawn with photoshop to an array
    		// of inner and outer radius points
		   for(int i=0,j=0;i<raw_points.length;)
		   {i++;
			double ix = raw_points[i++]/800.0;		// 800 by 311 is the size of the image that was measured
		   	double iy = raw_points[i++]/311.0;
		   	inner_points[j][0]=ix;
		   	inner_points[j][1]=iy;
			double ox = raw_points[i++]/800.0;
		   	double oy = raw_points[i++]/311.0;
		   	outer_points[j][0] = ox;
		   	outer_points[j][1] = oy;
		   	
		   	row_cycle_index[j] = raw_points[i++];
		   	row_compress[j] = raw_points[i++];
		   	j++;
		   	            
		   }
	   }
    
    //
    // set up the mapping so BCtoX and BCtoY work for the current display
    //
	public void SetDisplayRectangle(Rectangle tbRect)
	{	       
	      // calculate the rectangle where the image will actually reside.  
	      // This is redundant to what G.CenterImage normally does.
		  // 800x311 is the size of the image we measured
	      double ratio = 800/311.0;
	      G.SetRect(boardRect,G.Left(tbRect),G.Top(tbRect),G.Width(tbRect),G.Height(tbRect));
	      
	      if ((G.Height(tbRect)*ratio)<G.Width(tbRect))
	      {	// too wide, use full height and clip x
	    	 G.SetHeight(boardCenter,G.Height(tbRect));
	    	 G.SetTop(boardCenter,0);
	    	 int neww = (int)(G.Height(tbRect)*ratio);
	    	 int dif = (G.Width(tbRect)-neww)/2;
	    	 G.SetLeft(boardCenter, dif);
	    	 G.SetWidth(boardCenter,neww);
	      }
	      else
	      {	// too tall, use full width and clip y
	    	G.SetWidth(boardCenter,G.Width(tbRect));
	    	G.SetLeft(boardCenter, 0);
	    	int newh = (int)(G.Width(tbRect)/ratio);
	    	int dif = (G.Height(tbRect)-newh)/2;
	    	G.SetTop(boardCenter, dif);
	    	G.SetHeight(boardCenter,newh);
	       }	
	}

	// cribbed from com.sun.javafx.geom.Line2D.java
	
	private float ptSegDistSq(float paramFloat1, float paramFloat2, float paramFloat3, float paramFloat4, float paramFloat5, float paramFloat6)
	{
	  paramFloat3 -= paramFloat1;
	  paramFloat4 -= paramFloat2;
	  
	  paramFloat5 -= paramFloat1;
	  paramFloat6 -= paramFloat2;
	  float f1 = paramFloat5 * paramFloat3 + paramFloat6 * paramFloat4;
	  float f2;
	  if (f1 <= 0.0F)
	  {

	 
	     f2 = 0.0F;
	 

	  }
	  else
	  {
	   paramFloat5 = paramFloat3 - paramFloat5;
	   paramFloat6 = paramFloat4 - paramFloat6;
	   f1 = paramFloat5 * paramFloat3 + paramFloat6 * paramFloat4;
	   if (f1 <= 0.0F)
	      {
	 
	      f2 = 0.0F;
	      }
	      else
	      {
	      f2 = f1 * f1 / (paramFloat3 * paramFloat3 + paramFloat4 * paramFloat4);
	      }
	    }

	   float f3 = paramFloat5 * paramFloat5 + paramFloat6 * paramFloat6 - f2;
	   if (f3 < 0.0F) {
	    f3 = 0.0F;
	    }
	    return f3;
	 }
   
    //
	// used for inverse mapping from x,y to a board cell
	//
	public int closestRow(int x,int y)
	{	int minIndex = -1;
		double minDis = -1;
		int zx1 = BCtoX('Z',0);
		int zx2 = 2*(BCtoX('Z',1)-zx1);
		int dis = zx2*zx2;
		for(int i=0;i<40;i++)
			{
				int x1 = BCtoX('A',i);
				int y1 = BCtoY('A',i);
				int x2 = BCtoX('Z',i);
				int y2 = BCtoY('Z',i);		
				double ds = ptSegDistSq(x1,y1,x2,y2,x,y);
				if((ds<dis) && ((minIndex==-1) || (ds < minDis))) 
					{ minIndex = i; minDis = ds; 
					}
			}
	return(minIndex);
	}
	//
	// used for inverse mapping from x,y to a board cell
	//
	public char closestCol(int x,int y,int row)
	{	int minDis = -1;
		char minCol = '@';
		for(int i=0;i<26;i++)
		{	char thisCol = (char)('A'+i);
			int x1 = BCtoX(thisCol,row);
			int y1 = BCtoY(thisCol,row);
			int ds = G.distanceSQ(x1,y1,x,y);
			//if(y<0) { G.print("Close "+row+thisCol+": "+x1+" "+y1+" "+ds); }
			if((minCol=='@') || (ds<minDis)) 
			{	minCol = thisCol;
				minDis = ds;
			}
		}
		return(minCol);
	}
	//
	// used for inverse mapping from x,y to a board cell
	//
	public BreakingAwayCell closestCell(int xp,int yp0)
	{	return(closestCellB(xp,G.Height( boardCenter)-yp0));
	}
	public BreakingAwayCell closestCellB(int xp,int yp)
	{	int closestRow = closestRow(xp,yp);
		if(closestRow>=0) 
		{
		char closestCol = closestCol(xp,yp,closestRow);
		int pl = (pickedSource!=null) ? pickedSource.player : -1;
		int in = (pickedSource!=null) ? pickedSource.index : -1;
		BreakingAwayCell cc = new BreakingAwayCell(pl,in,closestCol,closestRow,BreakId.BoardLocation);
		return(cc);
		}
		
		return(null);
	}
	//
	//convert col,row to a screen position
	//
	public int BCtoX(char col,int row)
	{	// in our representation, row indicated distance around the track
		// and "A"-"Z" represents distance from the center
		if(reverse_y!=reverse_x) { row += 20; }
		if(row<0) { row+=40; }	// start at slightly negative positions
		row = row%40;			// position around the track
		int adj = Math.min(8,13-Math.max(0,Math.min(12,Math.abs(16-row))));
		double ix = inner_points[row][0];
		double ox = outer_points[row][0];
		double percent = (col-'B'+adj)/25.0;
		return((int)(G.Left(boardCenter)+G.Width(boardCenter)*(ix + percent*(ox-ix))));
	}	
	//
	//convert col,row to a screen position
	//
	public int BCtoY(char col,int row)
	{	// in our representation, row indicated distance around the track
		// and "A"-"Z" represents distance from the center
		if(reverse_y!=reverse_x) { row += 20; }
		if(row<0) { row+=40; }	// start at slightly negative positions
		row = row%40;			// position around the track
		int adj = Math.min(8,13-Math.max(0,Math.min(12,Math.abs(16-row))));
		double iy = inner_points[row][1];
		double oy = outer_points[row][1];
		double percent = (col-'B'+adj)/25.0;
		return((int)(G.Top(boardCenter)+G.Height(boardCenter)*(iy+percent*(oy-iy))));
	}

	// intermediate states in the process of an unconfirmed move should
	// be represented explicitly, so unwinding is easy and reliable.
	public BreakingAwayPiece pickedObject = null;
	private char picked_col = '@';
	int picked_row = 0;
	public BreakingAwayCell pickedSource = null;
	public BreakingAwayCell droppedDest = null;


	public BreakingAwayCell sourceCell() 
	{
		return (pickedSource);
	}

	public BreakingAwayCell destCell() 
	{
		return (droppedDest);
	}
	
	public BreakingAwayBoard(String init, long randomv, int npl,int map[]) // default constructor
	{	setColorMap(map, npl);
		doInit(init, randomv, npl); // do the initialization
	}

	public void doInit(String v,long rv) {
		doInit(v, rv, players_in_game);
	}

	public void doInit(String init, long randomv, int npl)
	{	
		players_in_game = npl;
		randomKey = randomv;
		robotState.clear();
		robotUndo.clear();
		variation = Variation.findVariation(init);
		gametype = init;
		undoMovement.clear();
		doneAdjusting = new boolean[players_in_game];
		doneAdjustingUI = new boolean[players_in_game];
		visibleInMainUI = new boolean[players_in_game];
		boolean oldvis[] = visibleInHiddenUI;
		showMovements = new boolean[players_in_game];
		showHiddenMovements = new boolean[players_in_game];
		AR.setValue(showHiddenMovements,true);
		visibleInHiddenUI = new boolean[players_in_game];
		AR.setValue(visibleInHiddenUI, true);
		if(oldvis!=null)
		{	// preserve the visibility selection over scrolling.
			for(int i=0,lim=Math.min(npl, oldvis.length); i<lim;i++) { visibleInHiddenUI[i]=oldvis[i]; }
		}
		AR.setValue(visibleInHiddenUI, true);
		AR.setValue(ridersInRow,0);
		win = new boolean[npl];
		cycles = new BreakingAwayCell[players_in_game][CYCLES_PER_PLAYER];
		pointsPerPlayer = new int[players_in_game];
		ridersAcrossSprint1 = 0;
		ridersAcrossSprint2 = 0;
		ridersAcrossFinish = 0;
		allRidersByY = new BreakingAwayCell[players_in_game*CYCLES_PER_PLAYER];
		allRidersInOrder = new BreakingAwayCell[players_in_game*CYCLES_PER_PLAYER];
		
		// set up the array of possible cycle destinations
		for(int i=0,lim=cycleDests.length;  i<lim;i++)
		{	cycleDests[i] = new BreakingAwayCell(0,0,'A',0,BreakId.BoardLocation);
		}
		int map[]=getColorMap();
		for (int currentPosition = 0,idx=0; currentPosition < players_in_game; currentPosition++) 
			{
			for(int currentRow=0; currentRow<CYCLES_PER_PLAYER;currentRow++) 
			{
			int currentPlayer = (variation==Variation.Staggered) ? (currentPosition+currentRow)%players_in_game : currentPosition;
			BreakingAwayCell cc = cycles[currentPlayer][currentRow] = 
				new BreakingAwayCell(currentPlayer,currentRow,(char)(currentPosition*3+'C'),-currentRow,BreakId.Rider);
				//new BreakingAwayCell(seq>36?'B':'A',seq++,BoardLocation);
			BreakingAwayPiece cycle = BreakingAwayPiece.getChip(map[currentPlayer]);
			addChip(cc,cycle);
			cc.breakAway = false;
			cc.arrivalOrder = idx;
			cc.nextArrivalOrder = idx;
			allRidersInOrder[idx] = cc;
			allRidersByY[idx++] = cc;
			
			{
			// give the cyclists their initial allocation of movement
			// factors.  This can be modified later by the player's 
			// setup instructions.
			int nmoves = cc.movements.length;
			int summoves = MOVEMENTSUM[currentRow];
			int incamount = 1;
			int baseamount = (summoves-incamount*nmoves)/nmoves-1;
			for(int mm=0;mm<nmoves;mm++)
				{ cc.movements[mm]=baseamount;
				  summoves -=baseamount;
				  baseamount+=incamount;
				}
			cc.movements[nmoves-1] += summoves;	// give leftover to the last
			AR.copy(cc.pendingMovements,cc.movements);
			}
			}
			}

		setState(BreakState.PUZZLE_STATE);

		whoseTurn = FIRST_PLAYER_INDEX;
		droppedDest = null;
		pickedSource = null;
		pickedObject = null;
		moveNumber = 1;
		maxTimeStep = 1;
		timeStep = 1;
		sortRiders();
	}

	public BreakingAwayBoard cloneBoard() {
		BreakingAwayBoard dup = new BreakingAwayBoard(gametype, randomKey, players_in_game,getColorMap());
		dup.copyFrom(this);
		return (dup);
	}
    public void copyFrom(BoardProtocol b) { copyFrom((BreakingAwayBoard)b); }

	public void sameboard(BoardProtocol f) { sameboard((BreakingAwayBoard)f); }
	/**
	 * Robots use this to verify a copy of a board. If the copy method is
	 * implemented correctly, there should never be a problem. This is mainly a
	 * bug trap to see if BOTH the copy and sameboard methods agree.
	 * 
	 * @param from_b
	 */
	public void sameboard(BreakingAwayBoard from_b) {
		super.sameboard(from_b);
		G.Assert(AR.sameArrayContents(pointsPerPlayer,from_b.pointsPerPlayer),"points match");
		G.Assert(AR.sameArrayContents(ridersInRow,from_b.ridersInRow),"riders match");
		G.Assert( (timeStep==from_b.timeStep)
					&& (maxTimeStep == from_b.maxTimeStep),"timeStep matches");
		for(int i=0; i<allRidersInOrder.length;i++)
		{	G.Assert(allRidersInOrder[i].sameCell(from_b.allRidersInOrder[i]),
					"riders in order");
		}
		G.Assert((unresign == from_b.unresign),"unresign mismatch");
		G.Assert((riderTurnOrder == from_b.riderTurnOrder),"riderTurnOrder matches");
		G.Assert((ridersAcrossSprint2 == from_b.ridersAcrossSprint2),"ridersAcrossSprint2 matches");
		G.Assert((ridersAcrossFinish == from_b.ridersAcrossFinish),"ridersAcrossFinish matches");
		G.Assert((ridersAcrossSprint1 == from_b.ridersAcrossSprint1),"ridersAcrossSprint1 matches");
		//G.Assert(Digest()==from_b.Digest(), "Digest Matches");
	}

	/**
	 * this is used in fraud detection to see if the same game is being played
	 * over and over. Each game in the database contains a digest of the final
	 * state of the game. Other site machinery looks for duplicate digests.
	 * 
	 * @return
	 */
	public long Digest() {
		long v = 0;
		// the basic digestion technique is to xor a bunch of random numbers.
		// The key
		// trick is to always generate exactly the same sequence of random
		// numbers, and
		// xor some subset of them. Note that if you tweak this, all the
		// existing
		// digests are invalidated.
		//
		Random r = new Random(122058943);

		if((board_state==BreakState.PLAY_STATE)||(board_state==BreakState.CONFIRM_STATE))
		{
		for(int i=0,lim=allRidersInOrder.length; i<lim; i++)
		{	v ^= allRidersInOrder[i].Digest(r);	// uses the long form of digest
		}}
		v ^= riderTurnOrder*r.nextLong();
		v += ridersAcrossSprint1*r.nextLong();
		v += ridersAcrossSprint2*r.nextLong();
		v += ridersAcrossFinish*r.nextLong();
		for (int i = 0; i < players_in_game; i++) 
		{
			long nv = r.nextLong();
			v ^= ((doneAdjusting[i])?1:0)*nv;
			long pv = r.nextLong();
			v += pointsPerPlayer[i]*pv;
		}
		
		
		for(int i=0,lim=ridersInRow.length; i<lim; i++) { v += ridersInRow[i]*r.nextLong(); }
		{BreakState estate = (board_state==BreakState.DONE_ADJUSTING_STATE)?BreakState.ADJUST_MOVEMENT_STATE:board_state;
		 v ^= ((estate.ordinal()*20)+whoseTurn)*r.nextLong();
		}
		cell.Digest(r,pickedSource);
		chip.Digest(r,pickedObject);
		return (v);
	}

	/*
	 * make a copy of a board. This is used by the robot to get a copy of the
	 * board for it to manupulate and analyze without affecting the board that
	 * is being displayed.
	 */
	public void copyFrom(BreakingAwayBoard from_b) 
	{	
		super.copyFrom(from_b);
		boardRect = from_b.boardRect;
		boardCenter = from_b.boardCenter;
		
		board_state = from_b.board_state;
		unresign = from_b.unresign;
		timeStep = from_b.timeStep;
		maxTimeStep = from_b.maxTimeStep;
		riderTurnOrder = from_b.riderTurnOrder;
		ridersAcrossSprint1 = from_b.ridersAcrossSprint1;
		ridersAcrossSprint2 = from_b.ridersAcrossSprint2;
		ridersAcrossFinish = from_b.ridersAcrossFinish;
		undoMovement.clear();
		AR.copy(ridersInRow,from_b.ridersInRow);
		AR.copy(pointsPerPlayer,from_b.pointsPerPlayer);
		AR.copy(doneAdjusting,from_b.doneAdjusting);
		AR.copy(doneAdjustingUI,from_b.doneAdjustingUI);
		AR.copy(visibleInMainUI,from_b.visibleInMainUI);
		AR.copy(visibleInHiddenUI,from_b.visibleInHiddenUI);
		AR.copy(showMovements,from_b.showMovements);
		for(int i=0,lim=allRidersInOrder.length; i<lim;i++)
		{	BreakingAwayCell fr = from_b.allRidersInOrder[i];
			BreakingAwayCell to = getCell(fr.player,fr.index);
			to.copyAllFrom(fr);		// not copyFrom
			allRidersInOrder[i]=to;
		}
		sameboard(from_b);
	}

	//
	// assign new movements based on pack position.  Each rider should have
	// moved, and the movement factor used will be negative instead of positive
	//
	private void assignMovements()
	{	BreakingAwayCell oldleader = allRidersInOrder[0];
		AR.setValue(ridersInRow,0);
		for(int i=0,lim=allRidersInOrder.length; i<lim;i++)
		{	undoSort.push(allRidersInOrder[i]);
		}
		sortRiders();
		timeStep++;
		BreakingAwayCell newleader = allRidersInOrder[0];
		BreakingAwayCell newsecond = allRidersInOrder[1];
		boolean newBreak = (newleader!=oldleader) && (newleader.row!=newsecond.row); 

		for(int i=0,
				lim=allRidersInOrder.length,
				ridersAhead=0,				// number of riders in rows ahead with no gaps
				nriders=0,				// riders in the current row
				currentRow = 9999; 			// the current row number
			i<lim; 
			i++)
		{	BreakingAwayCell rider = allRidersInOrder[i];
			int row = rider.row;
			
			if(row!=currentRow)			// changing rows 
			{	if((row+1)==currentRow)	// staying in the pack
				{ ridersAhead+=nriders;	// still making a pack of riders 
				}
				else 
				{ ridersAhead = 0;		// a break, we're pushing the wind 
				}
			nriders = 0;			// new row
			currentRow = row;
			}
			nriders++;
		
			int movements[] = rider.movements;
			int usedIdx = -1;
			for(int j=0;j<movements.length;j++)
			{	// locate the movement we used and will replace, which is negative
				if(movements[j]<0) { usedIdx=j; break; }
			}
			G.Assert(usedIdx>=0,"some movement used");

			undoMovement.push(movements[usedIdx]<<3|usedIdx);
			// check for special breaking awaw replenishment
			if((i==0) && newBreak )
			{	movements[usedIdx] = newleader.row-newsecond.row;	// breaking away bonus/penalty
				rider.breakAway = true;
			}
			else { movements[usedIdx] = 3+ridersAhead; rider.breakAway=false; }
			//G.print("Rider " + rider + " = "+movements[usedIdx]);
		}
	}
	//
	// undo the effect of new assignments.  This is used by the robot
	//
	private void unassignMovments()
	{	for(int i=allRidersInOrder.length-1; i>=0;i--)
		{
		BreakingAwayCell rider = allRidersInOrder[i];
		int mval = undoMovement.pop();
		rider.movements[(mval&0x7)]=mval>>3;
		}
		for(int i=allRidersInOrder.length-1; i>=0; i--)
		{	// restore the pre-sort order of the riders
		allRidersInOrder[i]=undoSort.pop();
		allRidersInOrder[i].popPosition();
		}
		timeStep--;
	}
	//
	// change whose turn it is, increment the current move number
	//
	public void setNextPlayer() {
		switch (board_state) {
		case PLAY_STATE:
		default:
			throw G.Error("Move not complete, can't change the current player");
		case PUZZLE_STATE:
			break;
		case ADJUST_MOVEMENT_STATE:
		case DONE_ADJUSTING_STATE:
			whoseTurn = (whoseTurn+1)%players_in_game;
			moveNumber++;
			visibleInMainUI[whoseTurn]=false;
			break;
		case CONFIRM_DROP_STATE:
			break;
		case CONFIRM_STATE:
			moveNumber++;
			riderTurnOrder++;
			if(riderTurnOrder>=allRidersInOrder.length)
			{	
				assignMovements();
			}
			whoseTurn = allRidersInOrder[riderTurnOrder].player;
			visibleInMainUI[whoseTurn]=false;
			break;
			
		case RESIGN_STATE:
			moveNumber++; // the move is complete in these states
			setWhoseTurn((whoseTurn + 1) % players_in_game);
			visibleInMainUI[whoseTurn]=false;
			return;
		}
	}

	/**
	 * this is used to determine if the "Done" button in the UI is live
	 * 
	 * @return
	 */
	public boolean DoneState() {
		
		switch (board_state) {
		case ADJUST_MOVEMENT_STATE:
		case CONFIRM_STATE:
		case CONFIRM_DROP_STATE:
		case RESIGN_STATE:
			return (true);

		default:
			return (false);
		}
	}
	public boolean DigestState()
	{	return(board_state==BreakState.CONFIRM_STATE);
	}
	/**
	 * In our implementation, the letter side(a-k) is black and the number side
	 * (1-11) is white. Either player can be playing either color.
	 * 
	 * @param ind
	 * @return
	 */

	private void setGameOver(boolean draw) {
		for (int i = 0; i < players_in_game; i++) {
			win[i] = !draw && (whoseTurn == i);
		}
		setState(BreakState.GAMEOVER_STATE);
	}

	// true if 8 riders have passed the finish line.
	private boolean GameOverNow()
	{	return(ridersAcrossFinish>=FINISHPOINTS.length);
	}

	//
	// return true if balls[rack][ball] should be selectable, meaning
	// we can pick up a ball or drop a ball there. movingBallColor is
	// the ball we would drop, or -1 if we want to pick up
	//
	private void acceptPlacement() 
	{	if((board_state!=BreakState.PUZZLE_STATE) && (droppedDest!=null))
		{	int destRow = droppedDest.row;
			int distance = destRow - Math.max(0,picked_row);
			int movements[] = droppedDest.movements;
			boolean ok = false;
			scorePlayer(droppedDest.player,picked_row,destRow);
			for(int i=0;!ok && i<movements.length;i++)
			{	if(distance==movements[i])
				{
				movements[i]= - movements[i];
				ok = true;
				}
			}
			G.Assert(ok,"One of the movements was used");
		}
		pickedObject = null;
		droppedDest = null;
		pickedSource = null;

	}
	private void unDropObject()
	{
		if(droppedDest!=null)
		{
		BreakingAwayCell dr = droppedDest;
		pickedObject = removeChip(dr);
		ridersInRow[dr.row]--;

		dr.col = picked_col;
		dr.row = picked_row;
		droppedDest=null;
		
		}
	}

	private BreakingAwayPiece removeChip(BreakingAwayCell c) {
		BreakingAwayPiece top = c.removeTop();
		return (top);
	}

	private void addChip(BreakingAwayCell c, BreakingAwayPiece top) 
	{	
		c.addChip(top);
	}
	private void unPickObject()
	{
		if((pickedObject!=null) && (pickedSource!=null))
		{
			BreakingAwayCell ps = pickedSource;
			pickedSource = null;
			addChip(ps,pickedObject);
			pickedObject = null;
		}
	}


	private BreakingAwayCell getCell(int player,int index)
	{	return(cycles[player][index]);
	}

	//
	// true if col,row is the place where something was dropped and not yet
	// confirmed.
	// this is used to mark the one square where you can pick up a marker.
	//
	public boolean isDest(BreakingAwayCell cell) {
		return (droppedDest == cell);
	}

	// get the index in the image array corresponding to movingObjectChar
	// or HitNoWhere if no moving object. This is used to determine what
	// to draw when tracking the mouse.
	public int movingObjectIndex() 
	{	BreakingAwayPiece c = pickedObject;
		if (c != null) {
			return (c.chipIndex());
		}
		return (NothingMoving);
	}

	// pick something up. Note that when the something is the board,
	// the board location really becomes empty, and we depend on unPickObject
	// to replace the original contents if the pick is cancelled.
	private void pickObject(BreakingAwayCell c)
	{
		switch (c.rackLocation())
	{	case Rider:
		case BoardLocation:
			pickedSource = c;
			pickedObject = removeChip(c);
			break;


		default:
			throw G.Error("not expecting source %s", c);
		}
	}


	private void doDone() 
	{	acceptPlacement();
		if (board_state==BreakState.RESIGN_STATE) {
			for(int i=0;i<players_in_game;i++) { cycles[whoseTurn][i].dropped=true; }
			} 
			{
			boolean win1 = GameOverNow();
			setNextPlayer();
			if (win1) {
				setGameOver(true);
			} else {
				setState(BreakState.PLAY_STATE);
			}
		}
	}


	private void sortRiders()
	{	for(int i=0,lim=allRidersInOrder.length; i<lim;i++)
		{	int nextOrder = allRidersInOrder[i].nextArrivalOrder;
			G.Assert(nextOrder>=0,"order is set");
			allRidersInOrder[i].arrivalOrder = nextOrder;
		}
		Sort.sort(allRidersInOrder,0,allRidersInOrder.length-1,true);
		riderTurnOrder = 0;
		maxTimeStep = Math.max(timeStep,maxTimeStep);
		for(int i=0,lim=allRidersInOrder.length; i<lim;i++)
		{	allRidersInOrder[i].pushPosition();
		}
	}
	// for use by the user interface
	public void doneAdjusting(int pl)
	{	G.Assert(board_state==BreakState.ADJUST_MOVEMENT_STATE,"adjusting");
		doneAdjustingUI[pl]=true;
	}
	
	void doneAdjusting(int pl,int data[])
	{	G.Assert(board_state==BreakState.ADJUST_MOVEMENT_STATE,"adjusting");
		doneAdjustingUI[pl]=true;
		doneAdjusting[pl] = true;
		showMovements[pl] = false;
		int di=0;
		for(int i=0;i<CYCLES_PER_PLAYER;i++)
		{	BreakingAwayCell c = cycles[pl][i];
			int pmovements[] = c.pendingMovements;
			int movements[] = c.movements;
			for(int j=0,lim=movements.length; j<lim; j++) 
			{		
				pmovements[j] = data[di];
				movements[j] = data[di++];
			}
		}
	}
	String readyString(int pl)
	{	String data = "";
		for(int i=0;i<CYCLES_PER_PLAYER;i++)
		{	BreakingAwayCell c = cycles[pl][i];
			int movements[] = c.pendingMovements;
			for(int j=0,lim=movements.length; j<lim; j++) { data += " "+movements[j]; }
		}
		return(data);
	}
	void parseMovements(StringTokenizer msg)
	{
		int pl = G.IntToken(msg);
		for(int i=0;i<CYCLES_PER_PLAYER;i++)
		{	BreakingAwayCell c = cycles[pl][i];
			int movements[] = c.pendingMovements;
			for(int j=0,lim=movements.length; j<lim; j++) 
			{		
				movements[j] = G.IntToken(msg);
			}
		}
	}
	public boolean Execute(commonMove mm,replayMode replay) {
		BreakingAwayMovespec m = (BreakingAwayMovespec) mm;
		//if(replay!=replayMode.Replay) { G.print("E "+m+" "+Digest()); }
		switch (m.op) {
		case MOVE_DONEADJUST:
			doneAdjusting(m.playerNumber,m.moveData);
			break;
		case MOVE_MOVEMENTS:
			{
			BreakingAwayCell cell = getCell(m.playerNumber, m.cycleIndex);
	       	int movements[] = cell.pendingMovements;
	       	int md[] = m.moveData;
	       	for(int lim = Math.min(md.length, movements.length)-1; lim>=0; lim--)
				{
	       		movements[lim] = md[lim];
				}
			}
			break;
		case MOVE_PLUS1:
		case MOVE_MINUS1:
			{
			BreakingAwayCell cell = getCell(m.playerNumber, m.cycleIndex);
           	int delta = (m.op==MOVE_PLUS1)  ? 1 : -1;
	       	int movements[] = cell.pendingMovements;
        	int col = m.from_row;
        	int mcol = col;
        	int next = movements[col]+delta;
         	int mcolnext = mcol-delta;
        	int min = 0;
        	do { mcol = (mcol+1)%movements.length;
        	mcolnext = movements[mcol]-delta;
        		 min = (mcol==3) ? 0 : 1;
        	} while ((mcolnext<min) || (mcolnext> 15));
        	// adjust the movements locally
        	if(col!=mcol) 
        		{movements[col] = next;
        		 movements[mcol] = mcolnext;
        		}
        	}
			break;
		case MOVE_ADJUST:
			setState(BreakState.ADJUST_MOVEMENT_STATE);
			break;

		case MOVE_DONE:
		
			doDone();

			break;
		case MOVE_DROP_RIDER:
			{
			BreakingAwayCell c = getCell(m.playerNumber, m.cycleIndex);
			c.dropped = !c.dropped;
			setState(c.dropped?BreakState.CONFIRM_DROP_STATE:BreakState.PLAY_STATE);
			}
			break;
		case MOVE_READY:
			{
			// the "ready" move carries all the movement factors the user decided to use.
			int idx = 0;
			int data[] = m.moveData;
			for(int i=0;i<CYCLES_PER_PLAYER;i++)
			{	BreakingAwayCell c = cycles[whoseTurn][i];
				int movements[] = c.movements;
				for(int j=0,lim=movements.length; j<lim; j++) 
				{	undoMovement.push(movements[j]);
					movements[j] = data[idx++];
				}
			}
			doneAdjustingUI[whoseTurn] = doneAdjusting[whoseTurn] = true;
			}
			setNextPlayer();
			boolean alldone=true;
			for(int i=0,lim=doneAdjusting.length; alldone && (i<lim); i++) { alldone &= doneAdjusting[i]; }
			if(alldone) { whoseTurn = 0; setState(BreakState.PLAY_STATE); }
			break;
		case MOVE_DROPB: 
			{
			BreakingAwayCell c = getCell(m.playerNumber, m.cycleIndex);
			c.col = m.to_col;
			c.row = m.to_row;
			if((m.to_row==picked_row) && (m.to_col==picked_col)) 
			{ unPickObject(); }
			else
			{
			addChip(c,pickedObject);
			if(replay==replayMode.Single)
			{
				animationStack.push(pickedSource);
				animationStack.push(c);
			}
			ridersInRow[c.row]++;
			c.nextArrivalOrder = riderTurnOrder;
			pickedObject = null;
			if(board_state==BreakState.PUZZLE_STATE) { pickedSource = null; }
			else { droppedDest = c;  setState(BreakState.CONFIRM_STATE); }
			}}
			break;
		case MOVE_MOVE:	// robot move
			{
			BreakingAwayCell c = getCell(m.playerNumber, m.cycleIndex);
			int oldOrder = c.arrivalOrder;
			int oldNext = c.nextArrivalOrder;
			pickedSource = droppedDest = c;
			picked_row = c.row;
			picked_col = c.col;
			G.Assert(picked_row==m.from_row,"stating row mathces");
			pickedObject = null;
			c.col = m.to_col;
			c.row = m.to_row;
			if(c.row>=0) 
				{ ridersInRow[c.row]++;
				  c.nextArrivalOrder = riderTurnOrder;
				}
			
			int dist = c.row-Math.max(0,picked_row);
			int idx = -1;
			for(int i=0;i<c.movements.length;i++) { if(c.movements[i]==dist) { idx = i; break; }}
			G.Assert(idx>=0,"some movement used");
			if(robotBoard)
			{
			robotUndo.push(oldOrder*1000000+riderTurnOrder*1000+dist*10+idx);	// record for the robot
			robotUndo.push((maxTimeStep<<8)|oldNext);
			}
			setState(BreakState.CONFIRM_STATE); 
			}
			break;
		case MOVE_PICKB:
			// come here only where there's something to pick, which must
			switch (board_state) {
			default:
				throw G.Error("Not expecting pickb in state %s", board_state);
			case CONFIRM_STATE:
				if (isDest(getCell(m.playerNumber, m.cycleIndex))) {
					unDropObject();
					if(pickedSource!=null) { setState(BreakState.PLAY_STATE); }
					else { throw G.Error("not expecting state"); }
				} else {
					throw G.Error("Can't pick something else");
				}
				break;
			case PUZZLE_STATE:
				pickObject(getCell(m.playerNumber, m.cycleIndex));
				break;
			case PLAY_STATE:
			{
				BreakingAwayCell c = getCell(m.playerNumber, m.cycleIndex);
				picked_row = c.row;
				picked_col = c.col;
				if(isDest(c))
					{
					unDropObject();
					}
				else 
					{ pickObject(getCell(m.playerNumber, m.cycleIndex)); }
				break;
			}}
			break;
			

		case MOVE_START:
			setWhoseTurn(m.player);
			acceptPlacement();
			unPickObject();

			{
			boolean win1 = GameOverNow();
			if(win1) 
			{	setGameOver(true);
			}
			else 
			{	setGameOver(false);
				sortRiders();
				setState((allRidersInOrder[0].row==0)
						?BreakState.ADJUST_MOVEMENT_STATE
						:BreakState.PLAY_STATE);
			}
			}
			break;

		case MOVE_RESIGN:
			setState((unresign==null)?BreakState.RESIGN_STATE:unresign);
			break;
		case MOVE_EDIT:
			acceptPlacement();
			setState(BreakState.PUZZLE_STATE);
			break;
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(BreakState.GAMEOVER_STATE);
			break;

		default:
			cantExecute(m);
		}
		//if(replay!=replayMode.Replay) {	G.print("X "+m+" "+Digest()); }

		// System.out.println("Ex "+m+" for "+whoseTurn+" "+board_state);
		// System.out.println("Digest "+Digest());
		return (true);
	}



	public boolean LegalToHitBoard(BreakingAwayCell cell) {
		switch (board_state) {
		case PLAY_STATE:
			return (((pickedObject == null)
							? ((getSources().get(cell) != null))
							: (getDests().get(cell) != null)));

		case CONFIRM_STATE:
			return ((cell == droppedDest) || ((pickedObject == null) ? (getSources()
					.get(cell) != null)
					: (getDests().get(cell) != null)));
		case GAMEOVER_STATE:
		case ADJUST_MOVEMENT_STATE:
		case DONE_ADJUSTING_STATE:
		case CONFIRM_DROP_STATE:
		case RESIGN_STATE:
			return (false);

		default:
			throw G.Error("Not expecting state %s", board_state);
		case PUZZLE_STATE:
			return ((pickedObject == null) ? ((cell != null) && (cell.height() > 0)) // something
																						// available
																						// to
																						// pick
																						// up
					: ((cell.height()) == 0));
		}
	}

	/**
	 * assistance for the robot. In addition to executing a move, the robot
	 * requires that you be able to undo the execution. The simplest way to do
	 * this is to record whatever other information is needed before you execute
	 * the move. It's also convenient to automatically supply the "done"
	 * confirmation for any moves that are not completely self executing.
	 */
	public void RobotExecute(BreakingAwayMovespec m) {
		robotBoard = true;
		robotState.push(board_state);
		// to undo state transistions is to simple put the original state back.
		//G.print("E "+m);
		//G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

		if (Execute(m,replayMode.Replay)) {
			if (board_state==BreakState.CONFIRM_STATE)
			{
				doDone();
			}
			else if((m.op==MOVE_READY) || (m.op==MOVE_ADJUST) || (m.op == MOVE_MOVE) || (m.op == MOVE_DONE))
				{
			} else {
				throw G.Error("Robot move should be in a done state");
			}
		}
	}

	//
	// un-execute a move. The move should only be unexecuted
	// in proper sequence. This only needs to handle the moves
	// that the robot might actually make.
	//
	public void UnExecute(BreakingAwayMovespec m) {
		// System.out.println("U "+m+" for "+whoseTurn);
		//G.print("U "+m);

		switch (m.op) {
		default:
			cantUnExecute(m);
        	break;
		case MOVE_READY:
			{	// undo in the revese order 
				for(int i=CYCLES_PER_PLAYER-1;i>=0;i--)
				{	BreakingAwayCell c = cycles[whoseTurn][i];
					int movements[] = c.movements;
					for(int j=movements.length-1; j>=0; j--) 
					{	movements[j]=undoMovement.pop();
					}
				}
				moveNumber--;
				whoseTurn--;
				if(whoseTurn<0) { whoseTurn = players_in_game-1; }

			}
			break;
		case MOVE_MOVE: 
			{
			// used by the robot and by automove
			riderTurnOrder--;
			moveNumber--;
			if(riderTurnOrder<0) 
				{ unassignMovments();  
				  riderTurnOrder = allRidersInOrder.length-1;
				}
			BreakingAwayCell c = getCell(m.playerNumber,m.cycleIndex);
			if(m.to_row>=0){ ridersInRow[m.to_row]--; }
			c.row = m.from_row;
			c.col = m.from_col;
			int undoInfo2 = robotUndo.pop();
			int undoInfo = robotUndo.pop();
			maxTimeStep = undoInfo2>>8;
			int arrive = undoInfo/1000000;
			int uninfo = undoInfo%1000000;
			int rider = uninfo/1000;
			int rem = uninfo%1000;
			int dist = rem/10;
			int index = rem%10;
			c.arrivalOrder = arrive;
			c.movements[index]=dist;
			c.nextArrivalOrder = undoInfo2&0xff; 
			unScorePlayer(c.player,m.from_row,m.to_row);
			G.Assert(rider==riderTurnOrder,"turn matches");
			whoseTurn = allRidersInOrder[rider].player;
			}
			break;
		case MOVE_DONE:
			break;
		case MOVE_RESIGN:
			break;
		}
		droppedDest = null;
		pickedSource = null;
		pickedObject = null;
		setState(robotState.pop());
		
	}

	double rawScoreForPlayer(int pl)
	{		return(pointsPerPlayer[pl]);
	}
	double simpleEvaluation(int pl, boolean print) 
	{ 	double val = pointsPerPlayer[pl]*100;	
		double position = 0.0;
		double potential = 0.0;
		double draft = 0.0;
		double leader = 0.0;
		int leaderpos = riderLeading().row;
		BreakingAwayCell riders[] = cycles[pl];
		for(int i=0;i<riders.length;i++)
		{	BreakingAwayCell c = riders[i];
			int movements[] = c.movements;
			position += c.row;
			leader += (c.row-leaderpos);	// penalty for being at the back
			for(int j=0,lim=movements.length; j<lim; j++)
			{	int mm = movements[j];
				if(mm>0) { potential += mm*1.5; }	// favor potential over movement
				if(mm<0) {
						draft += 6;
						for(int pos=c.row+1; ridersInRow[pos]>0; pos++)
						{	draft += ridersInRow[pos]*2.0;
						}
					 }
			}
			
		}
		return(val+position+potential+draft+leader);
	}



	double maxEvaluation(int pl, boolean print) { // simple evaluation based on
													// piece mobility and
													// importance
		return (simpleEvaluation(pl, print));
	}

	// return true if there are drop moves.
	// if all!=null, it's a list of moves.

	Hashtable<BreakingAwayCell,BreakingAwayCell> getSources() 
	{ 
		Hashtable<BreakingAwayCell,BreakingAwayCell> h = new Hashtable<BreakingAwayCell,BreakingAwayCell>();
		if(board_state!=BreakState.PUZZLE_STATE)
		{	// only one guy can move
			BreakingAwayCell c = allRidersInOrder[riderTurnOrder];
			h.put(c,c);
		}
		return (h);
	}
	
	private boolean isEmptyColumn(char destcol,int destrow,int elbo)
	{	if((destcol<'A') || (destcol>'Z')) 
			{ return(false); }
		for(int i=0,lim=allRidersInOrder.length;i<lim;i++)
		{	BreakingAwayCell c = allRidersInOrder[i];
			if(((c.row%40)==(destrow%40)) && (c.col<=(destcol+elbo)) && (c.col>=(destcol-elbo)) ) 
				{ return(false); }
		}
		return(true);
	}
	// select an appropriate column, with elbow room if possible,
	// toward the edge for lapped riders. Toward the center for other riders
	// and maintaining the same general line.
	public char selectDestinationColumn(char sourceCol,int sourcerow,int destRow)
	{	
		int leaderRow = riderLeading().row;
		// trend toward the center if we're in the main body, trend toward the edge if we're lapped.
		int trend = ((leaderRow-destRow) > 25) ? 3 : -3;	// trend out or in
		int distance = destRow - sourcerow;
		
		for(int elbo=1; elbo>=0; elbo--)
		{ // first pass prefer some elbow room, second pass squeeze in
		int mincol = 'A';
		int midcol = 'O';
		char startcol = (char)Math.max(mincol,Math.min(midcol,(sourceCol+(trend*distance))));
		for(int n=0;n<25;n+=2)
			{
			char destCol1 = (char)(startcol+n);
			if(isEmptyColumn(destCol1,destRow,elbo)) { return(destCol1); }
			}		
		}
		throw G.Error("shouldn 't get here");
	}
	boolean isEmpty(char col,int row)
	{	if((col<'A') || (col>'Z')) { return(false); }
		for(int i=0;i<players_in_game;i++) 
		{	for(int j=0; j<CYCLES_PER_PLAYER;j++)
			{
			BreakingAwayCell c = getCell(i,j);
			if((c.col==col) && (c.row==row)) { return(false); }
			}
		}
		return(true);
	}
	void selectVacantCol(BreakingAwayMovespec m)
	{
		int row = m.to_row;
		char col = m.to_col;
		for(int i=0;i<=players_in_game;i++)
		{
			char dest = (char)(i+col);
			if(isEmpty(dest,row)) { m.to_col = dest; return; }
			char dest2 = (char)(i-col);
			if(isEmpty(dest2,row)) { m.to_col = dest2; return; }
		}
		return;
	}
	Hashtable<BreakingAwayCell,BreakingAwayCell> getDests() 
	{
		Hashtable<BreakingAwayCell,BreakingAwayCell> h = new Hashtable<BreakingAwayCell,BreakingAwayCell>();
		if((pickedSource!=null)&&(droppedDest==null))
		{
		int movements[] = pickedSource.movements;
		h.put(pickedSource,pickedSource);
		for(int i=0;i<movements.length;i++)
		{	int dis = movements[i];
			if(dis>0)
			{
			int src = Math.max(pickedSource.row,0);
			BreakingAwayCell c = cycleDests[i];
			c.row = dis+src;
			c.col = selectDestinationColumn(pickedSource.col,src,src+dis);
			c.player = pickedSource.player;
			c.index = pickedSource.index;
			h.put(c,c);
			}
		}
		//G.Error("not implemented");
		
		}
		return (h);
	}

	String random_adjustment(int pla,int idx)
	{	
		Random r = new Random(randomKey+pla^idx+(pla+1)*(idx+1));
		int movements[] = cycles[pla][idx].movements;
		int lim = movements.length;
		int sum = 0;
		for(int i=0; i<lim; i++) { sum+= movements[i]; }
		
		int seed = 3+Random.nextInt(r,Math.min(sum-lim-3,12));
		String result = " "+seed;
		sum = sum-seed;
		lim--;
		while(lim>0)
		{
		int next = sum/lim;
		G.Assert(next>=1 && next<=15,"good value");
		sum -= next;
		if((next==sum) && (next>1) && (sum<15)) { next--; sum++; }
		result += " "+next;
		lim--;
		}
		return(result);
	}
	public BreakingAwayCell riderLeading()
	{	BreakingAwayCell leader = allRidersInOrder[0];
		int leadingRow = leader.row;
		for(int i=0;i<=riderTurnOrder;i++) 
			{ BreakingAwayCell thisRow = allRidersInOrder[i];
			  int row = thisRow.row;
			  if(row>leadingRow) { leader=thisRow; leadingRow=row; }
			}
		return(leader);
	}
	public BreakingAwayCell riderToMove()
	{	return(allRidersInOrder[riderTurnOrder]);
	}
	public String randomMove()
	{
		BreakingAwayCell source = riderToMove();
		int movements[] = source.movements;
		Random rv = new Random((randomKey*moveNumber)^riderTurnOrder);
		int least = movements[Random.nextInt(rv,movements.length)];
		int destrow = (Math.max(0,source.row)+least);
		return("Move "+source.player+" "+source.index
					+ " "+source.col
					+ " "+source.row
					+ " "+selectDestinationColumn(source.col,source.row,destrow)
					+ " "+destrow
					);
	}
	private CommonMoveStack  GetListOfMoves0(int who) {
		CommonMoveStack all = new CommonMoveStack();
		switch(board_state)
		{
		case CONFIRM_DROP_STATE:
		case RESIGN_STATE:
		case CONFIRM_STATE:
			all.addElement(new BreakingAwayMovespec(who,MOVE_DONE));
			break;
		case DONE_ADJUSTING_STATE:
		case ADJUST_MOVEMENT_STATE:
		{	String data = "Ready";
			for(int i=0;i<CYCLES_PER_PLAYER; i++)
			{
				data += random_adjustment(who,i);
			}
			all.addElement(new BreakingAwayMovespec(data,who));
			}
			break;
		case PLAY_STATE:
			BreakingAwayCell source = allRidersInOrder[riderTurnOrder];
			int movements[] = source.movements;
			for(int i=0;i<movements.length;i++)
			{	int thism = movements[i];
				if(thism>0)
				{
				boolean samem = false;
				for(int j=0;j<i && !samem ;j++) { samem |= (movements[j]==thism); }
				// avoiding duplicate moves at this point is much cheaper than later
				if(!samem)
					{all.addElement(new BreakingAwayMovespec(who,MOVE_MOVE,
						source.player,source.index,
						source.col,source.row,
						source.col,Math.max(source.row,0)+thism));
					}
				}
			}
				break;
		default:
			break;
		}

		return (all);
	}

	public CommonMoveStack  GetListOfMoves() {
		return(GetListOfMoves0(whoseTurn));
	}
	public int cellToX(BreakingAwayCell c) {
		return(BCtoX(c.col,c.row));
	}
	public int cellToY(BreakingAwayCell c) {
		return(BCtoY(c.col,c.row));
	}
	
	
}