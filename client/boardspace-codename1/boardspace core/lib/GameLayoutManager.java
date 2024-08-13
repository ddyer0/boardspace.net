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
package lib;



import com.codename1.ui.geom.Rectangle;

import bridge.FontMetrics;
import lib.GameLayoutClient.BoxAlignment;
import lib.GameLayoutClient.Purpose;
import lib.SeatingChart.DefinedSeating;


/**
 * this is the expert for constructing board layouts.  In this version of the algorithm,
 * the player boxes are placed first, around the periphery of the screen, so as to leave
 * a large unused area for the board.  The constraints try to provide both a reasonable
 * size for the player box, and as large as practical an area for the board and other
 * controls.   Second, all the auxiliary controls are placed, and finally the main
 * rectangle for the board, all that's left over, is used.
 * 
 *  All the placements have a margin to separate them from other controls.
 *  
 *  Placement of the player boxes is driven by "seating charts" for players around
 *  a playtable, or alternatively generic seating intended for online play which 
 *  accommodate any number of players.
 * 
 * The intended entry points for this are the variations of  "doLayout" "placeRectangle" and "getMainRetangle"
 *  
 * @author Ddyer
 *
 */
public class GameLayoutManager implements UniversalConstants
{	
	/** these purpose codes are used to customized the optimization phase */

	public GameLayoutManager(boolean max) { boardMax = max; }
	GameLayoutClient client = null;
	int nPlayers;
	// if true, consider the space left by the difference between the big rectangle
	// and the board aspect ratio to be all waste.  This should be false for 
	// boardless games like hive.
	public boolean strictBoardsize = true;
	// normally we don't skip placing done in planned seating mode, because each player
	// will have a private done button.  Sometimes the "done" button is used for other 
	// purposes and we want to place it anyway.
	public boolean alwaysPlaceDone = false;		

	public String toString() { return("<GamelayoutManager "+selectedSeating+">"); }
	public boolean boardMax = false;
	
	// left top right and bottom include necessary margins, so new content
	// can exactly reach the given values
	private int left;				// these define the central rectangle that is still unallocated
	private int top;					// these reflect the exact unused area, inside the margins
	private int right;
	private int bottom;
	
	private int ycenter;
	private int xcenter;
	
	private int playerWX;	// exact size
	private int playerWM;	// includes margin 
	private int playerHX;	// exact size
	private int playerHM;	// includes margin
	
	// placement for player boxes
	private int xleft;	
	private int ytop;
	private int xright;
	private int ybot;
	private int margin;
	
	// define some common waypoints
	private int xmid;			// not the true mid line, but offset by half a box
	private int ymid;
	private int ymidUp;			// center when there's a player below
	private int xsideLeft;		// so when rotated will center on left
	private int xsideRight;	// so when rotated will center on right
		
	private int xthirdLeft;			// in the tightest fit, space will be zero
	private int xthirdRight;
	public int fails = 0;				// counts allocation failures on this cycle
	int positions[][] = null;	// the x,y coordinates of the player boxes
	double rotations[] = null;	// the rotations of the player boxes
	
	/*
	 * player boxes are placed around the edges of the window, and these
	 * "addskinny" functions return the bits and pieces inbetween the player
	 * areas to the pool of unallocated space.   Some layouts are functionally
	 * the same but partition the returned space into different shapes, leaving
	 * a different shaped main rectangle etc.
	 */
	
	// add spare rectangles for skinny margins at left or left and right, from top to bottom
	private void addSkinnyLeft(boolean addSkinnyRight,boolean fromtop,boolean tobottom)
	{	
		int spareY = ycenter-playerWM/2;
		addSkinnyLeftFrom(true,spareY,addSkinnyRight,fromtop,tobottom);
	}
	
	private void addSkinnyRightOnly(boolean fromtop,boolean tobottom)
	{	
		int spareY = ycenter-playerWM/2;
		addSkinnyLeftFrom(false,spareY,true,fromtop,tobottom);
	}

	private void addSkinnyLeftFrom(boolean addSkinnyLeft,int spareY,boolean addSkinnyRight,boolean fromtop,boolean tobottom)
	{
		int spareYT = fromtop ? top : top+playerHM;
		int spareYH = spareY-spareYT;
		int spareYB = spareY+playerWM;	// bottom of the sideways rectangle
		int spareYBot = tobottom ? bottom : ybot-margin;
		int spareYBH = spareYBot-spareYB;
		if(spareYH>0)	// might have been eaten by overlap
		{
		if(addSkinnyLeft)
			{
			addToSpare(left,spareYT,playerHM,spareYH);
			addToSpare(left,spareYB,playerHM,spareYBH);
			}
		if(addSkinnyRight)
			{	int spareX = right-playerHM;
				addToSpare(spareX,spareYT,playerHM,spareYH);
				addToSpare(spareX,spareYB,playerHM,spareYBH);
			}
		}
	}
	

	// add spare rectangles for skinny margins at left or left and right
	private void addSkinnyOffset(int spareX)
	{
		int spareY = ycenter-playerWM/2-playerHM/2;
		int spareYH = spareY-top;
		int spareYB = spareY+playerWM;
		int spareYBH = ybot-spareYB;
		addToSpare(spareX,top,playerHM,spareYH);
		addToSpare(spareX,spareYB,playerHM,spareYBH);
	}
	// add spare rectangles for fat margins at left or left and right, with the side box centered
	// this is used only by FiveAroundEdge and SixAroundEdge
	private void addFatLeftCentered(boolean addLeft,boolean addRight,boolean six)
	{	int spareY0 = ytop+playerHX+1;
		int spareY = top+ycenter-playerWM/2;
		int spareY2 = spareY+playerWM;
		if(addLeft)
		{
		addToSpare(left,spareY0,playerWM,spareY-spareY0);
		addToSpare(left,spareY2,playerWM,ybot-spareY2);
		int spareX = playerHM;
		addToSpare(spareX,spareY,playerWM-playerHM,spareY2-spareY);
		}
		// rectangles ok 2/26
		if(addRight)
		{
		if(six)
		{	// the right-top rectangle starts at the true top.
			addToSpare(xright,spareY0,playerWM,spareY-spareY0);
		}
		else {
			addToSpare(xright,ytop,playerWM,spareY-ytop);
		}
			addToSpare(xright,spareY2,playerWM,ybot-spareY2);
			addToSpare(xright,spareY,playerWM-playerHM,spareY2-spareY);
		}
	}


	// add spare rectangles for .X.X. spacing 
	private void add2XAcross(int ycoord)
	{	
		addToSpare(left,ycoord,xthirdLeft-left,playerHM);
		int spareX = xthirdRight+playerWX;
		addToSpare(spareX,ycoord,right-spareX,playerHM);
		spareX = xthirdLeft+playerWX;
		addToSpare(spareX,ycoord,xthirdRight-spareX,playerHM);
	}
	
	// add spare rectangles for ..X.. spacing
	private void add1XAcross(int ycoord)
	{
		int spareX = xmid+playerWX;
		addToSpare(spareX,ycoord,right-spareX,playerHM);
		addToSpare(left,ycoord,xmid-left,playerHM);
	}


	// add spare rectangles for X.X.X spacing
	private void add3XAcross(int ycoord)
	{
		int spareX = left+playerWM;
		addToSpare(spareX,ycoord,xmid-spareX-1,playerHM);
		spareX = xmid+playerWX;
		addToSpare(spareX,ycoord,xright-spareX-1,playerHM);
	}
	private void addSidebar(int xcoord)
	{	int spareY = ycenter-playerWX/2;
		addToSpare(xcoord,top,playerHM,spareY-top);
		int spareYB = ycenter+playerWM/2;
		addToSpare(xcoord,spareYB,playerHM,bottom-spareYB);
	}
	private void addTopToBottom(int xcoord,boolean topTop)
	{
		int spareY = topTop?0:ytop+playerHX;
		// extra space between the top and bottom on the right side
		addToSpare(xcoord,spareY,playerWM,ybot-spareY);
	}
	private void addSideToSide(int top)
	{
		addToSpare(left+playerWM,top,right-left-playerWM*2,playerHM);
	}
	
	// add as horizontal segments for corner-edge arrangements
	private void addSpareVStrip(int spareX,int spareX2)
	{	int stripH = (bottom-playerWM-playerHM)/2;
		addToSpare(spareX,0,playerWM,stripH);
		addToSpare(spareX,bottom-playerHM-stripH,playerWM,stripH);
		if(playerWM>playerHM) { addToSpare(spareX2,stripH,playerWM-playerHM,playerWM); }
	}
	
	// add as horizontal segments for corner-edge arrangements
	private void addSpareVStripFrom(int stripY,int spareX,int spareX2)
	{	
		addToSpare(spareX,0,playerWM,stripY);
		int strip2H = (bottom-playerHM)-(stripY+playerWM);
		if(strip2H>0)
		{
		addToSpare(spareX,stripY+playerWM,playerWM,strip2H);
		}
		if(playerWM>playerHM) { addToSpare(spareX2,stripY,playerWM-playerHM,playerWM); }
	}
	static double fourAcrossRotation[] = {0,0,Math.PI,Math.PI,Math.PI};	//extra pi for fivearound
	static double fourAroundRotation[] = {0,Math.PI/2,Math.PI,-Math.PI/2};
	static double fiveAroundRotation[] = { 0,0,Math.PI/2,Math.PI,-Math.PI/2};
	static double sixAroundRotation [] = { 0,0,Math.PI/2,Math.PI,Math.PI,-Math.PI/2};
	public double preferredAspectRatio = 1.0;
	public RectangleManager rects = new RectangleManager(1.0);
	
	private int[][] stackedPositions(int players,int ystart,int ystep,int...xes)
	{	int cols = xes.length;
		int ypos = ystart;
		int result[][] = new int[players][];
		for(int ind = 0;ind<players;)
		{
			for(int col = 0;col<cols && ind<players;col++)
			{
				int rr[] = new int[2];
				rr[0] = xes[col];
				rr[1] = ypos;
				result[ind++] = rr;
			}
			ypos += ystep;
		}
		return result;
	}
	/**
	 * assign coordinates based on a seating chart, number of players,
	 * cell size, and board width and height.
	 * 
	 * this leaves positions[][] and rotations[] 
	 * @param seating
	 * @param nP
	 * @param l
	 * @param t
	 * @param w
	 * @param h
	 * @param player
	 */
	public void makeLayout(DefinedSeating seating,int nP,
			int l,int t,int w,int h,Rectangle player,int marginSize)
	{
	//G.print("Make ",seating," ",l," ",t," ",w,"x",h);
	rects.init(0,0,w,h);
	nPlayers = nP;		
	left = l;
	right = l+w;
	top = t;
	bottom = t+h;
	margin = marginSize;
	positions = null;
	rotations = new double[nPlayers];
	playerWX = G.Width(player);	// exact size
	playerWM = playerWX + marginSize;	// includes margin 
	playerHX = G.Height(player);// exact size
	playerHM = playerHX+marginSize;		// includes margin
	
	// placement locations for player boxes
	xleft = left+marginSize;	
	ytop = top+marginSize;
	xright = right-playerWM;
	ybot = bottom-playerHM;

	// define some common waypoints
	xcenter = (left+right)/2;
	ycenter = (top+bottom)/2;
	xmid = xcenter-playerWX/2;			// not the true mid line, but offset by half a box
	ymid = ycenter-playerHX/2;
	ymidUp = ycenter-playerHX;
	int rotationYoffset = (playerWX-playerHX)/2;
	
	xsideLeft = left+(playerHM-playerWX)/2;		// so when rotated will center on left
	xsideRight = right-(playerHM+playerWM)/2;//right-rotationXoffset;	// so when rotated will center on right
	
	// points for the seated layouts with two per side
	int extra = right-left-playerWM*2;		// extra space with 2 boxes across
	int space = extra/4;					// allocate 1/4 left, 1/2 between the boxes, 1/4 right
	
	xthirdLeft = left + space;			// in the tightest fit, space will be zero
	xthirdRight = right -space - playerWM;
	//G.print("Seating "+seating);
	switch(seating)
	{
	default:
	case Undefined:
		throw G.Error("seating chart %s not expected",seating);
	case ThreeAroundLeft: // ok 2/4/2020
	{
	/* top and bottom are flush to the left, leaving a bigger right hand rectangle
	   this is currently used by triad
	
   		 __.........
		 ...........
		 |..........
		 ...........
		 __.........
		 
	 */
		rotations = fourAroundRotation;
	positions = new int[][] { {xleft,ybot}, { xsideLeft,ymid}, { xleft,ytop}};
	// there's a skinny rectangle left between the side rectangle and the main board,
	// ok 2/26
	addFatLeftCentered(true,false,false);
	left += playerWM;
	}
	break;
	case ThreeAroundRight: // ok 2/4/2020
	{
	/* top and bottom are flush to the left, leaving a bigger right hand rectangle
	   this is currently used by triad
	
   		 .........__
		 ...........
		 .........|
		 ...........
		 .........__
		 
	 */
		rotations = new double[] {0,-Math.PI/2,Math.PI};
		positions = new int[][] { {xright,ybot}, { xsideRight,ymid}, { xright,ytop}};
		// there's a skinny rectangle left between the side rectangle and the main board,
		// ok 10/5/2022
		addFatLeftCentered(false,true,true);
		right -= playerWM;
	}
	break;

	case FaceToFaceLandscapeTop:
		rotations = new double[]{Math.PI,0};
		positions = new int[][]{{xright,ytop},{xright,ybot}};
		addToSpare(left,top,xright-left,playerHM);
		addToSpare(left,ybot,xright-left,playerHM);
		top += playerHM;
		bottom -= playerHM;
		break;
	case FaceToFaceLandscapeSide: // ok 2/4/2020
		{
		/* player box top and bottom, trimming from right
		  
		 	......._
		 	........
		 	......._
		 */
		rotations = new double[] {Math.PI,0};
		positions = new int[][]{{xright,ytop},{xright,ybot}};
		// ok 2/26
		addTopToBottom(xright,false);
		right -= playerWM;
		}
		break;
	case FaceToFacePortraitSide:
		{
		/*
		 * face to face players with the board below them
		 * 
		 * |ssss|
		 * ......
		 * ......
		 * ......
		 */
			rotations = new double[]{Math.PI/2,-Math.PI/2};
			positions = new int[][]{{xsideLeft,top+rotationYoffset},	// left 
				{xsideRight,top+rotationYoffset},		// right
				};

		addToSpare(left+playerHM,top,w-playerHM*2-left,playerWM);
		top+= playerWM;
		}
		break;
	case FaceToFacePortrait: // ok 2/4/2020
		{	// player box left and right, rotated sideways
			rotations = new double[]{Math.PI/2,-Math.PI/2};
			positions = new int[][]{{xsideLeft,ymid},	// left 
				{xsideRight,ymid},		// right
				};
			// ok 8/2/2021
			addSkinnyLeft(true,true,true);
			left += playerHM;
			right -= playerHM;			
		}
		break;
	case LeftCornerWide: // ok 2/4/2020
		{
		// left corner, with the chip from left and right, leavinbg
		// the full height available in the center
		rotations = new double[]{ 0,Math.PI/2};
		positions = new int[][] { {xleft,ybot},		// bottom ..X..
								  {xsideLeft,top+rotationYoffset}};
		if(playerWM>playerHM)
		{
		addToSpare(left+playerHM,top,playerWM-playerHM,playerWM);
		addToSpare(left,top+playerWM,playerWM,ybot-playerWM-top);
		}
		else
		{
		addToSpare(left,top+playerWM,playerHM,bottom-top-playerWM-playerHM);
		addToSpare(left+playerWM,bottom-playerHM,playerHM-playerWM,playerHM);
		}
		// ok 8/2/2021
		//addSkinnyLeft(false,true,true);
		//addTopToBottom(xright,true);
		left += Math.max(playerWM,playerHM);
		}
		break;
		
	case LeftCornerTall: //
		// left corner, with the chip from left and right, leavinbg
		// the full height available in the center
		{
		rotations = new double[]{ 0,Math.PI/2};
		positions = new int[][] { 		// bottom ..X..
								  {right-playerWM,ybot},
								  {xsideLeft,ybot-rotationYoffset}};
		// ok 8/2/2021
		if(playerWM>playerHM)
		{
		addToSpare(left+playerHM,bottom-playerWM,right-left-playerHM,playerWM-playerHM);
		addToSpare(left+playerHM,bottom-playerHM,right-left-playerWM-playerHM,playerHM);
		}
		else
		{
		addToSpare(left,ybot,right-left-playerWM,playerHM-playerWM);	
		addToSpare(left+playerHM,bottom-playerWM,right-left-playerWM-playerHM,playerWM);
		}
		bottom -= Math.max(playerHM,playerWM);
		}
		break;
	case RightCornerTall: //
		// right corner, with the chip from left and right, leavinbg
		// the full height available in the center
		{
		rotations = new double[]{ 0,-Math.PI/2};
		positions = new int[][] { 		// bottom ..X..
								  {left,ybot},
								  {xsideRight,ybot-rotationYoffset}};
		// ok 8/2/2021
		if(playerWM>playerHM)
		{
		addToSpare(left,bottom-playerWM,right-left-playerHM,playerWM-playerHM);
		addToSpare(left+playerWM,bottom-playerHM,right-left-playerWM-playerHM,playerHM);
		}
		else
		{
		addToSpare(left+playerWM,ybot,right-left-playerWM,playerHM-playerWM);	
		addToSpare(left+playerWM,ybot+playerHM-playerWM,right-playerHM-playerWM,playerWM);
		}
		bottom -= Math.max(playerHM,playerWM);
		}
		break;
	
	case RightCornerWide: // ok 2/4/2020
		{
		// right corner with the chop from left and right, leaving 
		// the full height available in the center
		rotations = new double[]{ 0,-Math.PI/2};
		positions = new int[][] { {xright,ybot},		// bottom ..X..
								  {xsideRight,top+rotationYoffset}};
		if(playerWM>playerHM)
		{
		addToSpare(xright,playerWM,playerWM,bottom-top-playerWM-playerHM);
		addToSpare(xright,top,right-xright-playerHM,playerWM);
		}
		else {
		addToSpare(right-playerHM,ybot,playerHM-playerWM,playerHM);	
		addToSpare(right-playerHM,top+playerWM,playerHM,ybot-playerWM-top);
		}
		//addSkinnyRight(false,true);
		//addTopToBottom(left,true);
		right -= Math.max(playerHM,playerWM);
		}
		break;		
		
	case LeftCorner:
		// left corner, with the chip at left and bottom, leaving
		// the maximum possible width
		rotations = new double[]{ 0,Math.PI/2};
		positions = new int[][] { {xmid,ybot},		// bottom ..X..
									{xsideLeft,ymid-playerHM/2}};
		addSkinnyOffset(left);
		add1XAcross(ybot);
		bottom -= playerHM;
		left += playerHM;
		break;
		
	case RightEnd:
		rotations = new double[] {-Math.PI/2};
		positions = new int[][] {{xsideRight,ymid}};
		addSkinnyOffset(right-playerHM);		
		right -= playerHM;
		break;
		
	case RightCorner:
		// right corner, with the chip at right and bottom, leaving
		// the maximum possible width
		rotations = new double[]{ 0,-Math.PI/2};
		positions = new int[][] {{xmid,ybot},		// bottom ..X..
									{xsideRight,ymid-playerHM/2}};
		addSkinnyOffset(right-playerHM);
		add1XAcross(ybot);
		bottom -= playerHM;
		right -= playerHM;
		break;
		
	case ThreeLeftL:
		rotations = new double[]{ 0,0,Math.PI/2};
		positions = new int[][] { {xthirdRight,ybot},{xthirdLeft,ybot},		// bottom .X.X.
									{xsideLeft,ymid-playerHM/2}};
		addSkinnyOffset(left);
		add2XAcross(ybot);
		bottom -= playerHM;
		left += playerHM;
		break;
	case ThreeRightL:
		rotations = new double[]{ 0,0,-Math.PI/2};
		positions = new int[][] {{xthirdRight,ybot},{xthirdLeft,ybot},		// bottom .X.X.
									{xsideRight,ymid-playerHM/2}};
		addSkinnyOffset(right-playerHM);
		add2XAcross(ybot);
		bottom -= playerHM;
		right -= playerHM;
		break;

	case ThreeLeftLW:
		// .........
		// |........
		// __.....__
		rotations = new double[]{ 0,0,Math.PI/2};
		positions = new int[][] { {xright,ybot}, {xleft,ybot},		// bottom X...X
								  {xsideLeft,ymid-playerHM/2}};
		// ok 8/3/2021
		addSpareVStrip(left,left+playerHM);
		addTopToBottom(xright,true);
		{int ww = Math.max(playerWM,playerHM);
		left += ww;
		right -= ww;
		}
		break;
		
	case ThreeRightLW: // ok 2/4/2020
		// .........
		// ........|
		// __.....__

		rotations = new double[]{ 0,0,-Math.PI/2};
		positions = new int[][] {{xright,ybot}, {xleft,ybot},		// bottom X...X
									{xsideRight,ymid-playerHM/2}};
		// ok 8/3/2021
		addSpareVStrip(xright,xright);
		addTopToBottom(left,true);
		{int ww = Math.max(playerWM,playerHM);
		left += ww;
		right -= ww;
		}
		break;
		
	// player boxes 3 across the bottom of the board, with "spare" rect at the bottom-right
    // this might be used for 3, 5 or 6 players.  
	case Portrait3X: // ok 2/4/2020
		{
		int rows = (nPlayers+2)/3 - 1;
		int start = ybot - playerHM*rows;
		int x2 = xleft+playerWM;
		int x3 = xleft+playerWM*2;
		// spare rects ok 2/26
		positions = stackedPositions(nPlayers,start,playerHM,xleft,x2,x3);
		int spareX = left+playerWM*3;
		int spareH = playerHM*(rows+1);
		bottom -= spareH;
		// rectangles ok 2/26
		addToSpare(spareX,bottom,right-spareX,spareH);
		int rem = nPlayers%3;
		if(rem!=0)
			{
			addToSpare(left+rem*playerWM,bottom+playerHX,playerWM*(3-rem),playerHM+marginSize);
			}
		}
	break;
	// player boxes 2 across the bottom of the board, with "spare" rect at the bottom-right
    // this might be used for 2, 3 or 4 players.  
	case Portrait2X: // ok 2/4/2020
		{
		int rows = (nPlayers+1)/2 - 1;
		int start = ybot - playerHM*rows;
		int x2 = xleft+playerWM;
		// spare rects ok 2/26
		positions = stackedPositions(nPlayers,start,playerHM,xleft,x2);
		int spareX = left+playerWM*2;
		// rectangles ok 2/26
		int spareH = playerHM*(rows+1);
		bottom -= spareH;
		int spareW = right-spareX;
		if(spareW>10) { addToSpare(spareX,bottom,spareW,spareH); }
		if((nPlayers&1)!=0)
			{
			addToSpare(left+playerWM,bottom+(nPlayers>4?playerHM:0)+playerHX,playerWM,playerHM+marginSize);
			}
		}
		break;
	// players three across at the right, with spare as the remaining vertical space. 
	case ThreeAcross:
		/*
	 	...._....
	 	.........
	 	.._..._..
	  
		 */
		{
		rotations = new double[]{0,0,Math.PI};
		positions = new int[][] { {xthirdRight,ybot}, {xthirdLeft,ybot}, 	
								  {xmid,ytop}};								
		// ok 2/26
		add1XAcross(top);
		add2XAcross(ybot);
		
		top += playerHM;
		bottom -= playerHM;
		}
		break;

	case ThreeAcrossLeft: // ok 2/4/2020
		{
			/*
			 position the players at the left and right, and spare at the left and bottom
			 leaving the upper-right corner as the main rectangle
			  
				 _......_
				 ........
				 _.......
			*/
		rotations = new double[]{0,0,Math.PI};
		positions = new int[][] { {xright,ybot},{xleft,ybot},	
							      {xleft,ytop}};				

		addTopToBottom(left,false);
		addSideToSide(ybot);
		bottom -=playerHM;
		left += playerWM;
		}
		break;
	case ThreeAcrossLeftCenter: // ok 2/4/2020
		{
		/* same physical layout as ThreeAcrossLeft, but attribute
		   the spare space to the left and right
		   leaving the center column as the main rectangle
			 _......_
			 ........
			 _.......
		*/
		rotations = new double[]{0,0,Math.PI};
		positions = new int[][] { {xright,ybot},{xleft,ybot},	
							      {xleft,ytop}};				
	
		addTopToBottom(left,false);
		addTopToBottom(xright,true);
		right -= playerWM;
		left += playerWM;
		}
		break;
	case FourAcross:	// ok 2/4/2020
		{
    		/*
		 	.._..._..
		 	.........
		 	.._..._..
		  
    		 */
		rotations = fourAcrossRotation;
		positions = new int[][] { {xthirdRight,ybot}, {xthirdLeft,ybot}, 	// top 		.X.X.
								{xthirdLeft,ytop}, { xthirdRight,ytop}};	// bottom 	.X.X.
		add2XAcross(ybot);
		add2XAcross(top);

		top += playerHM;
		bottom -= playerHM;
		}
		break;
	case FourAcrossEdge: // ok 2/4/2020
		{	
		/*
		 *  like fouracross, but position the boxes at the left and right edge
		 
		 	_...._
		 	......
		 	_...._
		 
		 */
		rotations = fourAcrossRotation;
		positions = new int[][] { {xright,ybot}, {xleft,ybot},		// bottom X...X
								  {xleft,ytop}, { xright,ytop}};	// top X...X
		// rectangles ok 2/26			  
		addTopToBottom(left,false);
		addTopToBottom(xright,false);

		left += playerWM;
		right -= playerWM;
		}
		break;
	case FiveAcross: // ok 2/4/2020
		{
			/*
			  
			   _........._
			   ...........
			   _...._...._
			 
			 */

   		rotations = fourAcrossRotation;	// there is an extra Pi for this case
		positions = new int[][] { {xthirdRight,ybot}, {xthirdLeft,ybot},		// bottom .X.X.
								  {xleft,ytop}, {xmid,ytop}, { xright,ytop}};	// top    X.X.X
		// rectangles ok 2/26
		add2XAcross(ybot);
		add3XAcross(top);
		
		//spare rects checked 2/16/19
		top += playerHM;
		bottom -= playerHM;
		
		}
		break;
	case FiveAcrossEdge: // ok 2/4/2020
		{	// this carves out a strip across the top
			// for 3 players, and the right and left margins
			// for the other two. This leaves more space in the center bottom
	   		rotations = fourAcrossRotation;	// there is an extra Pi for this case
			positions = new int[][] { {xright,ybot}, {xleft,ybot},				// bottom X...X
									  {xleft,ytop}, {xmid,ytop}, { xright,ytop}};// top   X.X.X
			// ok 2/26		  
			add3XAcross(top);
			addTopToBottom(left,false);
			addTopToBottom(xright,false);
			top += playerHM;
			left += playerWM;
			right -= playerWM;
		}
		break;
	case SixAcross:	// ok 2/4/2020
		{
		/*
		  
		   _...._...._
		   ...........
		   _...._...._
		 
		 */
   		rotations = new double[]{0,0,0,Math.PI,Math.PI,Math.PI};
		positions = new int[][] { {xright,ybot}, {xmid,ybot}, {xleft,ybot},		// bottom X.X.X
								  {xleft,ytop}, {xmid,ytop}, { xright,ytop}};	// top    X.X.X
		// rectangles ok 2/26
		add3XAcross(ybot);
		add3XAcross(top);

		top += playerHM;
		bottom -= playerHM;
		}
		break;

	case ThreeWideLeft:	// ok 2/4/2020
		{
		rotations = new double[]{0,Math.PI/2,-Math.PI/2};
		positions = new int[][] {{xleft,ybot},	// bottom X...
								{xsideLeft,ymid-playerHM/2},	
							      {xsideRight,ymid}};				// top    X....

		// add the space beween the left and right player strips
		// ok 8/3/2021
		addSpareVStrip(left,left+playerHM);
		addSidebar(right-playerHM);
		
		{int ww = Math.max(playerWM,playerHM);
		left += ww;
		right -= playerHM;
		}
		}
		break;
	
	case ThreeWide: // ok 2/4/2020
	{
   		/*
		  _......
		  .......
		  _....._ 
		  
		 */
		rotations = new double[]{0,Math.PI/2,-Math.PI/2};
		positions = new int[][] {{xmid,ybot},	// bottom X...X
								{xsideLeft,ymid},	
							      {xsideRight,ymid}};				// top    X....

		// add the space beween the left and right player strips
		int spareX = left+playerHM;
		addToSpare(spareX,ybot,xmid-spareX,playerHM);
		int spareX2 = xmid+playerWM;
		addToSpare(spareX2,ybot,right-playerHM-spareX2,playerHM);

		addSidebar(left);
		addSidebar(right-playerHM);
		bottom -=playerHM;
		left += playerHM;
		right -= playerHM;
	}
	break;
	case FourAround:	// ok 2/4/2020
		/* four around
		
		......_.....
		|..........|
		......_.....
		
	 	*/
		
	case ThreeAroundL:	// ok 2/4/2020
		/* three around in a U shape, leaving the right unoccupied
		 * clip from top and bottom first
		......_.....
		|...........
		......_.....
		
	 	*/
		{
		rotations = fourAroundRotation;
		positions = new int[][] { {xmid,ybot},		// bottom ..X..
								{xsideLeft,ymid},	// left 
								{xmid,ytop},		// top ..X..
								{xsideRight,ymid}};	// right
								
		int spareY = ycenter-playerWM/2;
		int spareX = xmid+playerWX;
		int spareH = Math.min(playerHM,spareY-ytop);
		
		// this is like addx1across, but takes into account that the 
		// sideways recangles at the left and right might eat into it.
		if(spareH>0)
			{
			addToSpare(spareX,top,right-spareX,spareH);	
			addToSpare(left,top,xmid-left,spareH);
			int yb = bottom-spareH;
			addToSpare(spareX,yb,right-spareX,spareH);	
			addToSpare(left,yb,xmid-left,spareH);
			}
		// rectangles ok 8/2/2021
		addSkinnyLeft(seating==DefinedSeating.FourAround,false,false);				
		if(seating==DefinedSeating.FourAround)
		{	
			right -= playerHM;
		}
		left += playerHM;
		top += playerHM;
		bottom -= playerHM;
		}
		break;
	case ThreeAroundLH:	// ok 2/4/2020
		/* three around in a U shape, leaving the right unoccupied
		 * clip from top and bottom first
		......_.....
		|...........
		......_.....
		
	 	*/
		{
		int xpos = Math.max(xmid,left+playerHM);
		rotations = fourAroundRotation;
		positions = new int[][] { {xpos,ybot},		// bottom ..X..
								{xsideLeft,ymid},	// left 
								{xpos,ytop}};	// right
		addToSpare(left,top,playerHM,ymid-ytop);
		addToSpare(left,ymid+playerWM,playerHM,bottom-ymid-playerWM);
		int leftW = xpos-left-playerHM;
		addToSpare(left+playerHM,top,leftW,playerHM);
	    addToSpare(left+playerHM,bottom-playerHM,leftW,playerWM);
	    int rightx = xpos+playerWM;
	    int rightw = right-xpos-playerWM;
	    addToSpare(rightx,top,rightw,playerHM);
	    addToSpare(rightx,bottom-playerHM,rightw,playerHM);

		left += playerHM;
		top += playerHM;
		bottom -= playerHM;
		}
		break;

	case FourAroundW:
		/* four around, but clip top and bottom first	
		......_.....
		|..........|
		......_.....
		
	 	*/
		{
		rotations = fourAroundRotation;
		positions = new int[][] { {xmid,ybot},		// bottom ..X..
								{xsideLeft,ymid},	// left 
								{xmid,ytop},		// top ..X..
								{xsideRight,ymid}};	// right
		{
		int wleft = xmid-left;
		int wright = right-xmid-playerWM;
		int xright = xmid+playerWM;
	   addToSpare(left,top,wleft,playerHM);
	   addToSpare(xright,top,wright,playerHM);
	   addToSpare(left,ybot,wleft,playerHM);
	   addToSpare(xright,ybot,wright,playerHM);
		}
		{
		int topabove = top+playerHM;
		int heightabove = ymid-top-playerHM;
		int heightbelow = ybot-ymid-playerWM;
		int topbelow = ymid+playerWM;
		int xright = right-playerHM;
	   addToSpare(left,topabove,playerHM,heightabove);
	   addToSpare(left,topbelow,playerHM,heightbelow);
	   addToSpare(xright,topabove,playerHM,heightabove);
	   addToSpare(xright,topbelow,playerHM,heightbelow);
		}
	
		right -= playerHM;
		left += playerHM;
		top += playerHM;
		bottom -= playerHM;
		}
		break;

	case ThreeAroundR:	// ok 10/5/2022
		/* three around in a U shape, leaving the right unoccupied
		
		......_.....
		|...........
		......_.....
		
	 	*/
		{
		rotations = new double[] {0,-Math.PI/2,Math.PI};
		positions = new int[][] { {xmid,ybot},		// bottom ..X..
								  {xsideRight,ymid},
								  {xmid,ytop}};		// top ..X..};	// right
								
		int spareY = ycenter-playerWM/2;
		int spareX = xmid+playerWX;
		int spareH = Math.min(playerHM,spareY-ytop);
		
		// this is like addx1across, but takes into account that the 
		// sideways recangles at the left and right might eat into it.
		addToSpare(spareX,top,right-spareX,spareH);	
		addToSpare(left,top,xmid-left,spareH);
		int yb = bottom-spareH;
		addToSpare(spareX,yb,right-spareX,spareH);	
		addToSpare(left,yb,xmid-left,spareH);

		// rectangles ok 8/2/2021
		addSkinnyRightOnly(false,false);				
		right -= playerHM;
		top += playerHM;
		bottom -= playerHM;
		}
		break;
		
	case ThreeAroundRH:	// ok 10/5/2022
		/* three around in a U shape, leaving the right unoccupied
		
		......_.....
		|...........
		......_.....
		
	 	*/
		{
		int xpos = Math.min(xmid,right-playerHM-playerWM);
		rotations = new double[] {0,-Math.PI/2,Math.PI};
		positions = new int[][] { {xpos,ybot},		// bottom ..X..
								  {xsideRight,ymid},
								  {xpos,ytop}};		// top ..X..};	// right
	
		addToSpare(left,top,xpos,playerHM);
		addToSpare(left,bottom-playerHM,xpos,playerHM);
		int xright = xpos+playerWM;
		int rightw = right-xpos-playerWM;
		int rightw2 = right-xright-playerHM;
		addToSpare(xright,top,rightw,ymid-top);
		addToSpare(xright,ymid+playerWM,rightw,bottom-ymid-playerWM);
		addToSpare(xright,ymid,rightw2,playerHM-ymid);
		addToSpare(xright,bottom-playerHM,rightw2,ymid+playerWM-(bottom-playerHM));

		right -= playerHM;
		top += playerHM;
		bottom -= playerHM;
		}
		break;
		

	case FourAroundEdgeRect:		// ok 2/4/2020
		{	/* like four around, but place the top and bottom rectangles near the left and right
			   and make the central rectangle more rectangular
					_...........
					|..........|
					..........._  
			*/
			rotations = fourAroundRotation;
			{
			int boxY2 = ybot-(playerWM-playerHM)/2;
			positions = new int[][] { {xleft,ybot}, 	// bottom X....
									{ xsideLeft,ytop+(playerWM-playerHM)/2}, 	// left, aligned to box top
									{ xright,ytop},		// top ....X 
								{xsideRight,boxY2}};	// right, aligned to box bottom
			// ok 3/27/2023
			if(w>h)
				{
				{int spareY = top+playerWM;
				int spareX = left+playerHM;
				int spareXW = playerWM-spareX;		
				addToSpare(spareX,top,spareXW,spareY-top);
				addToSpare(left,spareY,playerWM,ybot-spareY);
				}
					
				right -= playerWM;
				left += playerWM;
				
				{
				int spareY = top+playerHM;
				int spareH = bottom-playerWM-spareY;
				addToSpare(right,spareY,playerWM,spareH);
				addToSpare(right,spareY+spareH,playerWM-playerHM,playerWM);
				}
				
				}
				else {
				addToSpare(left,bottom-playerWM,right-left-playerHM,playerWM-playerHM);
				addToSpare(left+playerHM,top+playerHM,right-left-playerHM,playerWM-playerHM);
				addToSpare(left+playerWM,bottom-playerHM,right-left-playerWM-playerHM,playerHM);
				addToSpare(left+playerHM,top,right-left-playerWM-playerHM,playerHM);
				top += playerWM;
				bottom -= playerWM;
				}
		
			}

		}
		break;
		
	case SixAround:	// ok 2/4/2020
		{
		/*
		  ..._..._...
		  |.........|
		  ..._..._...
		 
		 */
		rotations = sixAroundRotation;
		positions = new int[][] { {xthirdRight,ybot}, {xthirdLeft,ybot}, 	// bottom .X.X.
									{ xsideLeft,ymid},						// left 
									{ xthirdLeft,ytop}, {xthirdRight,ytop}, // top .X.X.
									{xsideRight,ymid}};						// right 
		// rectangles ok 2/26
		add2XAcross(top);
		add2XAcross(ybot);
		// ok 8/2/2021
		addSkinnyLeft(true,false,false);

		right -= playerHM;
   		left += playerHM;
		top += playerHM;
		bottom -= playerHM;
		}
		break;
	case FiveAround1EdgeCenter:
	case SixAroundEdge:	// ok 2/4/2020
		{
		/* like six across, but pull the boxes to the left and right edge
		 
		   _......._
		   |.......|
		   _......._
		 */
		rotations = new double[]{ 0,0,Math.PI/2,Math.PI,Math.PI,-Math.PI/2};
		positions = new int[][] { {xright,ybot}, {xleft,ybot},		// bottom X...X
								  { xsideLeft,ymid}, 				// left side
									{ xleft,ytop}, {xright,ytop},	// top X...X
									{xsideRight,ymid}};				// right side
		// spare rects checked 2/26
		// add 3 part rectangles on the left and right edges
		if(seating==DefinedSeating.SixAroundEdge)
		{
		addFatLeftCentered(true,true,true);
		}
		else {
			addFatLeftCentered(true,false,true);
			addTopToBottom(xright,false);
		}
		right -= playerWM;
		left += playerWM;
		}
		break;
		
	case FourAroundUW:	// ok 2/4/2020
		{
		/*  four around in a U shape, leaving the top unoccupied
		 
		 	.............
		 	|...........|
		 	__.........__
		  
		  place the left and right centered in the available space
		 */
		int ypos = ymid-playerHM/2;
		int stripH = ymid-playerWM/2;
		int overrun = ((stripH+playerWM)-(bottom-playerHM))/2;
		if(overrun<0) 
			{ ypos += overrun; stripH += overrun; 
			}
		rotations = new double[]{ 0,0,Math.PI/2,-Math.PI/2};
		positions = new int[][] { {xright,ybot}, {xleft,ybot}, 
			{ xsideLeft,ypos},						// left side
			{xsideRight,ypos}};						// right side
			
					
					
					// add the space beween the left and right player strips
		addSpareVStripFrom(stripH,left,left+playerHM);
		addSpareVStripFrom(stripH,xright,xright);

		{int ww = Math.max(playerWM,playerHM);
		left += ww;
		right -= ww;
		}
		}
		break;

	case FourAroundU:	// ok 2/4/2020
		{	/* four around in a U shape, leaving the top unoccupied
		
			............
			|..........|
			..._...._...
			
		 	*/
			int ypos = ymidUp;
			int ytop = ymidUp-playerWM/2+playerHM/2;
			rotations = new double[]{ 0,0,Math.PI/2,-Math.PI/2};
			positions = new int[][] { {xthirdRight,ybot}, {xthirdLeft,ybot}, 	// bottom .x.x.
						{ xsideLeft,ypos},						// left side
						{xsideRight,ypos}};						// right side
			// ok 8/2/2021
			addSkinnyLeftFrom(true,ytop,true,true,false);				
			add2XAcross(ybot);
			left += playerHM;
			bottom -= playerHM;
			right -= playerHM;
		}
		break;
	case FiveAround:	// ok 2/4/2020
		{
	   		/*
	   		 * five seated around the table chip from top and bottom first
	   		 * 
  		  ....._.....
  		  |.... ....|
  		  ..._..._...
  		  
  		  */
 		rotations = fiveAroundRotation;
		positions = new int[][] { {xthirdRight,ybot}, {xthirdLeft,ybot}, 	// bottom .x.x.
									{ xsideLeft,ymid},						// left side
									{xmid,ytop},							// top  ..X..
									{xsideRight,ymid}};						// right side
		// spare rects ok 2/26
		add1XAcross(top);
		add2XAcross(ybot);
		// ok 8/2/2021
		addSkinnyLeft(true,false,false);

		left += playerHM;
		top += playerHM;
		bottom -= playerHM;
		right -= playerHM;
		}
		break;
	case FiveAroundEdgeFirst:
		{	// take the left and right edges first
	   		/*
			  ....._.....
			  |.... ....|
			  ..._..._...
			  
			  */
		rotations = fiveAroundRotation;
		

		addSkinnyLeft(true,true,true);
		left += playerHM;
		right -= playerHM;

		// spare rects ok 2/26
		add1XAcross(top);
		int spare = right-left-playerWM*2;
		int spareX = spare/3;
		int bx1 = left+spareX;
		int bx2 = bx1 + playerWM + spareX;
		addToSpare(left,ybot,spareX,playerHM);
		addToSpare(bx1+playerWM,ybot,spareX,playerHM);
		addToSpare(bx2+playerWM,ybot,spare-spareX*2,playerHM);
		
		top += playerHM;
		bottom -= playerHM;
	
		positions = new int[][] { {bx2,ybot}, {bx1,ybot}, 	// bottom .x.x.
			{ xsideLeft,ymid},						// left side
			{xmid,ytop},							// top  ..X..
			{xsideRight,ymid}};						// right side
	
		}

		break;
	case FiveAround1Edge: // ok 2/4/2020
		{
    		/*
		    ..._..._... 
		 	|..........
		 	..._..._...
		*/
		rotations = sixAroundRotation;	// last space will be ignored
		positions = new int[][] { {xthirdRight,ybot}, {xthirdLeft,ybot}, 	// bottom .x.x.
									{ xsideLeft,ymid},						// left side
									{ xthirdLeft,ytop}, {xthirdRight,ytop}, // top .X.X.
									};						// right side
		// spare rects ok 2/26
		add2XAcross(top);
		add2XAcross(ybot);
		// ok 8/3/2021
		addSkinnyLeft(false,false,false);
	
		left += playerHM;
		top += playerHM;
		bottom -= playerHM;
		}
		break;

	case FiveAroundEdge:	// ok 2/4/2020
		{
			/* in this layout we take the left and right margins, not the top and bottom
		
				_........
				|.......|
				_......._
				
			 */
			rotations = fiveAroundRotation;
			positions = new int[][] { {xright,ybot}, {xleft,ybot},	// bottom  X...X
									  {xsideLeft,ymid},				// left side
									  {xleft,ytop}, 				// top     X....
									  {xsideRight,ymid}};			// right side
			addFatLeftCentered(true,true,false);
			left += playerWM;
			right -= playerWM;
	
		}
		break;

	case Across: // ok 2/4/2020
		{
		positions = new int[nPlayers][2] ;
		bottom -= playerHM;
		for(int i=0;i<nPlayers;i++) { positions[i][0] = xleft+i*playerWM; positions[i][1]=bottom; }
		int spareX = xleft+playerWM*nPlayers;
		int spareW = right-spareX;
			if(spareW>10)
			{
				addToSpare(spareX,bottom,spareW,playerHM);
			}
		}
		break;

	case Portrait:	// ok 2/4/2020
		{
		bottom -= playerHM*nPlayers;
		positions = stackedPositions(nPlayers,bottom,playerHM,xleft) ;
		int spareX = xleft+playerWM;
		addToSpare(spareX,bottom,right-spareX,playerHM*nPlayers);
		}
		break;
	case Landscape3X:
		{
		positions = stackedPositions(nPlayers,ytop,playerHM,xright-playerWM*2,xright-playerWM,xright);
		
		right -= playerWM*3;
		int spareY = top+(playerHM*((nPlayers+2)/3));
		// rectangles ok 2/26
		addToSpare(right,spareY,playerWM*3,bottom-spareY);
		int rem = (nPlayers%3);
		if(rem!=0)
			{
			addToSpare(right+rem*playerWM-marginSize,spareY-playerHM,(3-rem)*playerWM+marginSize,playerHM);
			}
		}
		break;
	
	case Landscape2X:
		{
		positions = stackedPositions(nPlayers,ytop,playerHM,xright-playerWM,xright);
		right -= playerWM*2;
		int spareY = top+(playerHM*((nPlayers+1)/2));
		// rectangles ok 2/26
		addToSpare(right,spareY,playerWM*2,bottom-spareY);
		if((nPlayers&1)!=0)
		{
			addToSpare(right+playerWX,spareY-playerHM,playerWM+marginSize,playerHM);
		}
		}
		break;
	case SideBySide:   // ok 2/4/2020
	case Landscape:
		{
		positions = new int[nPlayers][2];
		for(int i=0;i<nPlayers;i++) { positions[i][0] = xright; positions[i][1]=ytop+i*playerHM; }
		int spareY =ytop+playerHM*nPlayers-marginSize+1;	// spare rects ok 2/26
		addToSpare(xright,spareY,right-xright,bottom-spareY);
		right -= playerWM;
		break;
		}
	}
	rects.setMainRectangle(new Rectangle(left,top,right-left,bottom-top));
}
	

	Rectangle fullRect;

	/**
	 * perform the layout just calculated.
	 * 
	 * @param window
	 * @param unitsize
	 */
	public void doLayout(GameLayoutClient window,double zoom,int unitsize,Rectangle full)
	{	
	// place all the players	
		fullRect = full;
		client = window;
		for(int i=0;i<nPlayers;i++)
		{
		int []position = positions[i];
		Rectangle playerRect = window.createPlayerGroup(i,
				(int)(position[0]*zoom+0.5),
				(int)(position[1]*zoom+0.5),
				rotations[i],
				(int)(unitsize*zoom+0.5));
		if(G.debug())
			{
			Rectangle playerRectc = G.copy(null, playerRect);
		//	G.setRotation(playerRectc, -(window.getPlayerOrTemp(i).displayRotation));
			if(!full.contains(playerRectc)) 
				{//G.print("player rectangle runs off screen\n",playerRectc,"\n",full);
				}
			}
		}
	}
	
	private DefinedSeating tryThese[] = 
		{   DefinedSeating.Portrait,DefinedSeating.Across,DefinedSeating.Landscape,
			DefinedSeating.Landscape2X,	DefinedSeating.Landscape3X,
			DefinedSeating.Portrait2X, DefinedSeating.Portrait3X};
	
	private DefinedSeating selectedSeating = DefinedSeating.Landscape;
	private int selectedNPlayers = -1;
	private double selectedPercent = -1;
	public DefinedSeating selectedSeating() { return(selectedSeating); }
	private double selectedCellSize=0;
	public double selectedCellSize() { return(selectedCellSize); }
	/**
	 * select a layout for the game, based on the defined seating chart and
	 * a desired share of the space to be occupied by the board.  The driving
	 * forces in this algorithm are the "box" that occupies a player's private data
	 * and the number of players.
	 * The player boxes are laid out according to one of the offline seating
	 * charts, or simple plans for online presentation with one or two
	 * columns of players below or right of the board.
	 * 
	 * The residual will be a central box to be occupied by the board
	 * and whatever other ornaments are needed for the game.  There
	 * may be some other space available depending on the exact geometry.
	 * 
	 * @param client				 the client window
	 * @param nPlayers				 number of players
	 * @param width					 width of the actual window
	 * @param height				 height of the actual window
	 * @param marginSize 			 size of margins between boxes
	 * @param minBoardShare			 the minimum share of the board rectangle vs the player rectangles
	 * @param aspectRatio			 preferred aspect ratio for the board rectangle
	 * @param maxCellSize			 maximum cell size for the player box
	 * @param targetLayoutHysterisis when a seating layout is specified, weight this in favor of alternatives
	 * @return the percent coverage for the board in the selected layout
	 */
	public double selectLayout(GameLayoutClient client0,int nPlayers,int width,int height,
					int margin,double minBoardShare,
					double aspectRatio,double maxCellSize, double targetLayoutHysterisis)
	{	client = client0;
		int minSize = client.standardFontSize();
		double v = selectLayout(client,nPlayers,width,height,
				margin,minBoardShare,
				aspectRatio,Math.min(maxCellSize,minSize*1.8),maxCellSize,
				targetLayoutHysterisis);
		if(rects.failedPlacements>0)
		{
			G.print(rects.messages.getLog());
		}
		//G.print("Select ",width,"x",height,"=",selectedSeating()," ",selectedCellSize());
		fails = 0;
		return v;
	}

	/**
	 * select a layout for the game, based on the defined seating chart and
	 * a desired share of the space to be occupied by the board.  The driving
	 * forces in this algorithm are the "box" that occupies a player's private data
	 * and the number of players.
	 * The player boxes are laid out according to one of the offline seating
	 * charts, or simple plans for online presentation with one or two
	 * columns of players below or right of the board.
	 * 
	 * The residual will be a central box to be occupied by the board
	 * and whatever other ornaments are needed for the game.  There
	 * may be some other space available depending on the exact geometry.
	 * 
	 * @param client				 the client window
	 * @param nPlayers				 number of players
	 * @param width					 width of the actual window
	 * @param height				 height of the actual window
	 * @param marginSize 			 size of margin between boxes
	 * @param minBoardShare			 the minimum share of the board rectangle vs the player rectangles
	 * @param aspectRatio			 preferred aspect ratio for the board rectangle
	 * @param minSize				 minimum cell size for the player box
	 * @param maxCellSize			 maximum cell size for the player box
	 * @param targetLayoutHysterisis when a seating layout is specified, weight this in favor of alternatives
	 * @return the percent coverage for the board in the selected layout
	 */
	public double selectLayout(GameLayoutClient client,int nPlayers,int fullwidth,int fullheight,
			int margin,double minBoardShare,
			double aspectRatio,double minSize,double maxCellSize, double targetLayoutHysterisis)
		{
		double v = selectLayout(client,nPlayers,fullwidth,fullheight,
				1.0,margin,minBoardShare,
				aspectRatio,minSize,maxCellSize,targetLayoutHysterisis);

		if(rects.failedPlacements>0)
		{
			G.print(rects.messages.getLog());
		}
		//G.print("Select ",fullwidth,"x",fullheight,"=",selectedSeating()," ",selectedCellSize());
		fails = 0;
		return v;
	}

	/**
	 * select a layout for the game, based on the defined seating chart and
	 * a desired share of the space to be occupied by the board.  The driving
	 * forces in this algorithm are the "box" that occupies a player's private data
	 * and the number of players.
	 * The player boxes are laid out according to one of the offline seating
	 * charts, or simple plans for online presentation with one or two
	 * columns of players below or right of the board.
	 * 
	 * The residual will be a central box to be occupied by the board
	 * and whatever other ornaments are needed for the game.  There
	 * may be some other space available depending on the exact geometry.
	 * 
	 * The "old" version of this logic used exact width and height if zoomed
	 * windows, and the exact values of sizing parameters.  This proved to be
	 * hard to stabilize because the base size parameters were derived from
	 * font sizes, and that changed as the window zoomed up.  The interim
	 * heuristic is to force the layout manager to use the same layout when
	 * zoomed as was already used when not zoomed.  This gives the windows
	 * the same basic appearance, but some of the boxes might still wander.
	 * 
	 * The "future" version uses zoom>1.0, corresponding to the global zoom factor,
	 * and allocates based on width/zoom and height/zoom, which ought to be the
	 * actual (unzoomed) size of the window.   Sized parameters, minsize maxsize 
	 * ought to be based on the the global default font size, or some other
	 * metric which is not affected by the zoom.  In this model the client thinks
	 * it is allocating in "zoomed" coordinates, but rectangle manager is actually
	 * working with unzoomed values, all the logic is in the rectangle manager.
	 * 
	 * @param client				 the client window
	 * @param nPlayers				 number of players
	 * @param width					 width of the actual window
	 * @param height				 height of the actual window
	 * @param marginSize 			 size of margin between boxes
	 * @param minBoardShare			 the minimum share of the board rectangle vs the player rectangles
	 * @param aspectRatio			 preferred aspect ratio for the board rectangle
	 * @param minSize				 minimum cell size for the player box
	 * @param maxCellSize			 maximum cell size for the player box
	 * @param targetLayoutHysterisis when a seating layout is specified, weight this in favor of alternatives
	 * @return the percent coverage for the board in the selected layout
	 */
	public double selectLayout(GameLayoutClient client0,int nPlayers,int fullwidth,int fullheight,
			double zoom,int margin,double minBoardShare,
			double aspectRatio,double minSize,double maxCellSize, double targetLayoutHysterisis)
	{	// playtable has a deep bezil that makes the extreme edge hard to get to
		//G.print("\nselect ",minSize,maxCellSize);
		client = client0;
		// the idea of zoom is that we always allocate windows based on the canonical
		// size of the window, rather than the zoomed multiple, with no other changes
		// on the client size.  The math for this doesn't quite work out yet. so we nuke
		// the option for now.
		zoom = 1.0;
		rects.zoom = zoom;
		boolean portraitMode = fullwidth*1.5<fullheight;
		boolean landscapeMode = fullwidth>fullheight*1.5;
		fullRect = new Rectangle(0,0,fullwidth,fullheight);
		int extramargin = G.isRealLastGameBoard()||G.isRealPlaytable()?G.minimumFeatureSize()/2 : 0;
		int width = (int)(fullwidth/zoom)-extramargin;
		int height = (int)(fullheight/zoom)-extramargin;
		boolean recalc = (nPlayers!=selectedNPlayers) || ((zoom==1.0) ? !client.isZoomed() : true);
		if(recalc)
		{
		double bestPercent = 0;
		double bestScore = 0;
		double desiredAspectRatio = aspectRatio;
		double targetPreference = targetLayoutHysterisis;
		DefinedSeating best = null;
		DefinedSeating currentSeating = client.seatingChart();
		if(G.debug()) { G.print("initial ",currentSeating); }
		margin = (int)((margin+0.49)/zoom);
    	rects.marginSize = margin;
    	preferredAspectRatio = desiredAspectRatio;
	    if(currentSeating!=DefinedSeating.Undefined)
	    {	// seatings for a particular player arrangement can have
	    	// alternatives that make the board area more efficient, for
	    	// example 3 players "around" the table might have the players
	    	// at the center of each side, or 3 players squished to one end
	    	DefinedSeating originalSeating = currentSeating;
	    	while(currentSeating!=null)
	    	{
	    	//if(currentSeating==DefinedSeating.ThreeLeftL)	// coerce to one config
	    	{
	    	double currentPercent = sizeLayout(client,nPlayers,currentSeating,minBoardShare,desiredAspectRatio,maxCellSize,minSize,width,height,margin);
	    	double currentCellSize = selectedCellSize;
	    	double currentScore = (minBoardShare>0 ? currentPercent : 1-currentPercent ) *currentCellSize;
	    	//G.print("S "+currentSeating+" "+currentPercent+" "+currentScore+" "+currentPercent+" "+currentCellSize);
	    	if(currentCellSize>1 && (best==null || currentScore>bestScore))
	    	{	//G.print("Better "+currentPercent+" score ",currentScore," ",currentSeating);
	    		bestPercent = currentPercent;
	    		bestScore = currentScore;
	    		best = currentSeating;
	    		//sizeLayout(client,nPlayers,currentSeating,minBoardShare,desiredAspectRatio,maxCellSize,minSize,width,height);
		    	}}
	    	do { currentSeating = currentSeating.alternate; }
	    	while (currentSeating!=null 
	    			&& (portraitMode 
	    					? currentSeating.landscapeOnly
	    					: landscapeMode ? currentSeating.portraitOnly : false));
	    		
	    	if(currentSeating==originalSeating) { currentSeating=null; }
	    	}
	    }
	    else // if there is no target, there's no preference either.
	    { targetPreference = 1.0; 	// no target to prefer
	      best = currentSeating;
	    }
	    if(G.debug() ) { G.print("after primary ",best," ",bestScore," ",bestPercent,"% ",selectedCellSize);}
		for(DefinedSeating s : tryThese)	// also try the generic layouts 
		{
			double v = sizeLayout(client,nPlayers,s,minBoardShare,desiredAspectRatio,maxCellSize,minSize,width,height,margin);
			double score = (minBoardShare>0 ? v : 1-v)*selectedCellSize;
			//G.print(""+s+" board "+v+" cell "+selectedCellSize+" = "+score);
	    	//G.print("S "+s+" "+v+" "+score);
	    	if(selectedCellSize>1 && 
					((best==DefinedSeating.Undefined)
					|| (score*targetPreference>bestScore)))
				{ // select the biggest cell whose board percentage
				  // is in the acceptable range
				  bestPercent = v; 
				  best=s; 
				  bestScore = score;
				  //G.print("S+ "+best+" "+bestPercent+" "+bestScore+ " "+v+" "+selectedCellSize);
				  //sizeLayout(client,nPlayers,s,minBoardShare,desiredAspectRatio,maxCellSize,minSize,width,height);
				}
		}
		//G.print("best "+best+" "+bestPercent+" "+bestScore);

		if(best==null || best==DefinedSeating.Undefined)
		{	// the screen has no acceptable layouts (he's making his window tiny?)
			// but give him something...
			if(G.debug()) { G.print("No seating! ",width,"x",height," min ",minSize," players "+nPlayers);}
			best = DefinedSeating.Across;
		}
		//G.print("target "+selectedSeating+" "+selectedCellSize+" best "+bestPercent);
		//setLocalBoundsSize(client,best,boardShare,width,height);
	    selectedSeating = best;
		selectedNPlayers = nPlayers;
		selectedPercent = bestPercent;
		}
		//G.print("Resize ");
		// if we're not recalculating the layout, allow the minumum cell size to 
		// slip to compensate for breakage due to differing margins and font size.
		//
	    double finalSize = sizeLayout(client,nPlayers,selectedSeating,minBoardShare,aspectRatio,maxCellSize,recalc ? minSize : minSize/2,width,height,margin);

	    if(selectedCellSize<=0 || finalSize<=0)
	    {
	    	// in the rare case that the best layout still didn't produce a valid cell size,
	    	// try once more with no minimum
	    	if(G.debug()) { G.print("Emergency resize"); }
		    finalSize = sizeLayout(client,nPlayers,selectedSeating,0.2,aspectRatio,maxCellSize,1,width,height,margin);
	    }
	    if(finalSize*2 < minBoardShare)
	    {
	    	if(G.debug()) { G.print("bad layout, board share "+finalSize+" wanted "+minBoardShare); }
	    }
		int halfMargin = extramargin/2;
	    if(G.debug() ) { G.print("final ",selectedSeating," ",selectedPercent,"% ",selectedCellSize);}
		makeLayout(selectedSeating,nPlayers,halfMargin,halfMargin,width,height,client.createPlayerGroup(0,0,0,0,(int)selectedCellSize),margin);
    	//if(G.debug())
    	//{
    	//	G.print("Cell Min ",minSize," max ",maxCellSize," actual ",selectedCellSize);
    	//}
        doLayout(client,zoom,(int)selectedCellSize,new Rectangle(halfMargin,halfMargin,fullwidth-margin,fullheight-margin));
        rects.specs.clear();
        fails = 0;
	    return(selectedPercent);	    
	}
	
	/**
	 * this knows the characteristics of the DefinedSeating and does rough
	 * calculations based on the positions of the player boxes and the
	 * actual width and height of the board.
	 * 
	 * The values returned are the percentage of space occupied by the residual
	 * box for the board, and the cell size to be used for the player boxes.  It's
	 * desirable that both large cells and large board are the result, but also 
	 * contradictory.
	 * 
	 * @param client
	 * @param seating
	 * @param minBoardShare
	 * @param width
	 * @param height
	 * @return the percentage of space occupied by the "big box" for the board.
	 */
    private double sizeLayout(GameLayoutClient client,int nPlayers,DefinedSeating seating,
    		double minBoardShare,double desiredAspectRatio,double cellSize,double minSize,
    		double width,double height,int marginSize)
    {	
    	double unit = cellSize;
    	//
    	// createPlayerGroup returns the bounding rectangle for a player group based on "unit" as the size.
    	// It's important that it return the same retangle (proportional to unit) to and unit size.
    	//
    	Rectangle box = client.createPlayerGroup(0,0,0,0,(int)unit);
    	double playerW = Math.ceil((double)(G.Width(box))/unit);		// width of the player box in units
    	double playerH = Math.ceil((double)(G.Height(box))/unit);		// height of the player box in units
    	double unitsX=0;					// this will be the number of horizontal units the layout requires
    	double unitsY=0;					// this will be the number of vertical units the layout requires
    	double edgeUnitsX=0;				// this will be the number of units chipped off the horizontal axis
    	double edgeUnitsY=0;				// this will be the number of units chipped off the vertical axis
    	double fixedW = 0;					// this will be the fixed margin size needed for the layout.  Generally 2 for a standalone box
    	double fixedH = 0;					// or 2*n+1 for a group of n adjacent boxes.  The margin quantum doesn't vary with the unit size.
    	
    	// size the particular layout.  We need to know the size and shape of the hole left after all the
    	// player boxes are placed, and how many margin units need to be added to the boxes.  Then we will
    	// shrink the unit size until the box left is big enough to meet the minBoardShare criteria
    	switch(seating)
    	{  	
       	default:
     	case Undefined:
    		throw G.Error("Not expecting %s as seating",seating);
   	case ThreeAroundRight:
    		
    		/* top and bottom are flush to the left, leaving a bigger right hand rectangle
 		   this is currently used by triad
 		
 	   		 .........__
     		 ...........
     		 ..........|
     		 ...........
 	   		 .........__
    		 
    		 */
   	case ThreeAroundLeft:
    		
    		/* top and bottom are flush to the left, leaving a bigger right hand rectangle
 		   this is currently used by triad
 		
 	   		 __.........
     		 ...........
     		 |..........
     		 ...........
     		 __.........
     		 
    		 */
     		
    		unitsX = playerW;		
    		unitsY = playerH*2+playerW;
	   		fixedH = marginSize*4;
	   		fixedW = marginSize;
	   		edgeUnitsX = playerW;		// the left edge will be chipped off 
    		edgeUnitsY = 0;
    		break;
		case ThreeAcrossLeft:
			/*
			 position the players at the left and right, and spare at the left and bottom
			 leaving the upper-right corner as the main rectangle
			  
				 _......_
				 ........
				 _.......
			*/
			unitsX = playerW*2;			
	   		unitsY = playerH;			
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*2;
	   		edgeUnitsX = playerW;	
	   		edgeUnitsY = playerH;		
	   		break;
		case ThreeAcrossLeftCenter:
			/* same physical layout as ThreeAcrossLeft, but attribute
			   the spare space to the left and right
			   leaving the center column as the main rectangle
				 _......_
				 ........
				 _.......
			*/
			unitsX = playerW*2;			//  bottom x....x
	   		unitsY = playerH*2;			//  top    x.....
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*2;
	   		edgeUnitsX = playerW*2;
	   		edgeUnitsY = 0;
	   		break;

		case FourAroundUW:		
			/*  four around in a U shape, leaving the top unoccupied
			 
		 	.............
		 	|...........|
		 	_..........._
		  
			*/

			unitsX = playerW*2;
			unitsY = playerW+playerH;
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*3;
			edgeUnitsX = playerW*2;
			edgeUnitsY = 0;
			break;
			
		case FourAroundU:
			/* four around in a U shape, leaving the top unoccupied
			
			............
			|..........|
			..._...._...
			
		 	*/
			unitsX = playerW*2;
    		unitsY = playerH+playerW;
	   		fixedW = marginSize*4;
	   		fixedH = marginSize*2;
    		edgeUnitsX = playerH;
    		edgeUnitsY = playerH*2;
    		break;
		case ThreeAroundR:
			/* three around in a U shape, leaving the left unoccupied
			
				......_.....
				...........|
				......_.....
			
		 	*/
		case ThreeAroundL:
			/* three around in a U shape, leaving the right unoccupied
			
				......_.....
				|...........
				......_.....
			
		 	*/

    		unitsX = playerH+playerW;
    		unitsY = playerW+playerH*2;
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*2;
    		edgeUnitsX = playerH;
    		edgeUnitsY = playerH*2;
    		break;

		case ThreeAroundRH:
			/* three around in a U shape, leaving the left unoccupied
			
				......_.....
				...........|
				......_.....
			
		 	*/
		case ThreeAroundLH:
			/* three around in a U shape, leaving the right unoccupied
			
				......_.....
				|...........
				......_.....
			
		 	*/

    		unitsX = playerH+playerW;
    		unitsY = Math.max(playerH*2,playerW);
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*2;
    		edgeUnitsX = playerH;
    		edgeUnitsY = playerH*2;
    		break;


		case FourAroundEdgeRect:
			/* like four around, but place the top and bottom rectangles near the left and right
			   and make the central rectangle more rectangular
					_...........
					|..........|
					..........._  
			*/
			if(width>height)
			{
       		unitsX = playerW*2;
    		unitsY = playerH+playerW;
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*3;
    		edgeUnitsX = playerW*2;
    		edgeUnitsY = 0;
			}
			else
			{
	      		unitsY = playerW*2;
	    		unitsX = playerH+playerW;
		   		fixedW = marginSize*2;
		   		fixedH = marginSize*3;
	    		edgeUnitsY = playerW*2;
	    		edgeUnitsX = 0;
			}
    		break;
		case FourAcrossEdge:
			/*
			 *  like fouracross, but position the boxes at the left and right edge
			 
			 	_...._
			 	......
			 	_...._
			 
			 */
       		unitsX = playerW*2;
    		unitsY = playerH*2;
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*2;
    		edgeUnitsX = unitsX;
    		edgeUnitsY = 0;
    		break;
    	case ThreeAcross:
    		/*
    		 	...._....
    		 	.........
    		 	.._..._..
    		  
    		 */
    	case FourAcross:
    		/*
		 	.._..._..
		 	.........
		 	.._..._..
		  
    		 */
    		unitsX = playerW*2;
    		unitsY = playerH*2;
	   		fixedW = marginSize*4;
	   		fixedH = marginSize*2;
    		edgeUnitsX = 0;
    		edgeUnitsY = unitsY;
    		break;
    		
    	case ThreeWideLeft:
    		unitsX = playerW+playerH;
    		unitsY = playerH+playerW;
	   		fixedW = marginSize*2;
	   		fixedH = marginSize;

    		edgeUnitsX = playerW+playerH;
    		edgeUnitsY = 0;
    		break;
    		
    	case ThreeWide:
    		/*
    		  _......
    		  .......
    		  _....._ 
    		  
    		 */
    		unitsX = playerW*2+playerH;
    		unitsY = playerW;
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*2;

    		edgeUnitsX = playerH*2;
    		edgeUnitsY = playerH;
    		break;
     	case FourAround:
    		/* four around
    		
    		......_.....
    		|..........|
    		......_.....
    		
    	 	*/
    		unitsX = playerW+playerH*2;
    		unitsY = Math.max(playerW,playerH*2);
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*2;
    		edgeUnitsX = playerH*2;
    		edgeUnitsY = playerH*2;
    		break;
    	case FourAroundW:
    		/* four around
    		
    		......_.....
    		|..........|
    		......_.....
    		
    	 	*/
    		unitsX = Math.max(playerW,playerH*2);
    		unitsY = playerW+playerH*2;
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*2;
    		edgeUnitsX = playerH*2;
    		edgeUnitsY = playerH*2;
    		break;
		case FiveAround1EdgeCenter:
		case SixAroundEdge:
			/* like six across, but pull the boxes to the left and right edge
			 
			   _......._
			   |.......|
			   _......._
			 */

    		unitsX = playerW*2+playerH;
    		unitsY = playerW+playerH*2;
    		edgeUnitsX = playerW*2;
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*4;
    		edgeUnitsY = 0;
    		break;
    		
		case SixAround:
			/*
			  ..._..._...
			  |.........|
			  ..._..._...
			 
			 */
    		unitsX = playerW*2;
    		unitsY = playerW+playerH*2;
	   		fixedW = marginSize*4;
	   		fixedH = marginSize*2;
    		edgeUnitsX = playerH*2;
    		edgeUnitsY = playerH*2;
    		break;
    	case FiveAround1Edge:
    		/*
    		    ..._..._... 
    		 	|..........
    		 	..._..._...
    		*/
    		unitsX = playerW*2;
    		unitsY = playerW+playerH*2;
	   		fixedW = marginSize*4;
	   		fixedH = marginSize*4;

    		edgeUnitsX = playerH;
    		edgeUnitsY = playerH*2;
    		break;
    	case FiveAround:
    		/*
    		  ....._.....
    		  |.... ....|
    		  ..._..._...
    		  
    		  */
    		unitsX = playerW*2;
    		unitsY = playerW+playerH*2;
	   		fixedW = marginSize*4;
	   		fixedH = marginSize*2;
    		edgeUnitsX = playerH*2;
    		edgeUnitsY = playerH*2;
    		break;
    	case FiveAroundEdge:
			/* in this layout we take the left and right margins, not the top and bottom
			 * 
    		
			_........
			|.......|
			_......._
			*/
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*4;
   			unitsX = playerW*2;
    		unitsY = playerH*2+playerW;
    		edgeUnitsX = playerW*2;
    		edgeUnitsY = 0;
      		break;
    	case FiveAroundEdgeFirst:
			/* in this layout we take the left and right edges first, then top and bottom   		
			_........
			|.......|
			_......._
			*/
   			unitsX = playerH*2+playerW;
    		unitsY = playerH*2;
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*4;

    		edgeUnitsX = playerH*2;
    		edgeUnitsY = playerH*2;
    		break;
 
    	case FiveAcrossEdge:
    		/*
    		 	_......._
    		 	.........
    		 	_..._...._
    		  
    		 */
    		unitsX = playerW*3;
    		unitsY = playerH*2;
	   		fixedW = marginSize*4;
	   		fixedH = marginSize*2;

    		edgeUnitsX = playerW*2;
    		edgeUnitsY = playerH;
    		break;
    	case SixAcross:
    		/*
  		  
 		   _...._...._
 		   ...........
 		   _...._...._
 		 
    		 */

    	case FiveAcross:
			/*		  
			   _........._
			   ...........
			   _...._...._
			 
			 */		
    		unitsX = playerW*3;
    		unitsY = playerH*2;
	   		fixedW = marginSize*4;
	   		fixedH = marginSize*2;

    		edgeUnitsX = 0;
    		edgeUnitsY = playerH*2;
    		break;
    	
    	case RightCornerTall:
    	case LeftCornerTall:
      		unitsX = playerW+playerH;
      		unitsY = Math.max(playerH,playerW);
      		fixedW = 2*marginSize;
      		fixedH = marginSize;
      		edgeUnitsX = 0;
      		edgeUnitsY = Math.max(playerW,playerW);
      		break;
     		 
     	
     	case RightCornerWide:
      	case LeftCornerWide:
       		unitsY = playerW+playerH;
      		unitsX = Math.max(playerH,playerW);
      		fixedW = 2*marginSize;
      		fixedH = 2*marginSize;
      		edgeUnitsX = Math.max(playerW,playerH);
      		edgeUnitsY = 0;
      		break;
     		
     	case LeftCorner:	// ok 2/4/2020
      	case RightCorner:	// ok 2/4/2020
       		unitsX = playerW;
      		unitsY = playerH+playerW;
      		fixedW = 2*marginSize;
      		fixedH = 2*marginSize;
      		edgeUnitsX = playerH;
      		edgeUnitsY = playerH;
      		break;

      	case ThreeLeftLW:
      	case ThreeRightLW:
       		unitsX = playerW*2;
      		unitsY = playerW+playerH;
	   		fixedW = marginSize*4;
	   		fixedH = marginSize*2;

      		edgeUnitsX = playerW*2;
      		edgeUnitsY = 0;     		
      		break;
      		
      	case ThreeLeftL:
      	case ThreeRightL:
       		unitsX = playerW*2;
      		unitsY = playerW+playerH;
	   		fixedW = marginSize*4;
	   		fixedH = marginSize*2;
     		edgeUnitsX = playerH;
      		edgeUnitsY = playerH;
      		break;
      		

      	case Across:
    		unitsX = playerW*nPlayers;
    		unitsY = playerH;
    		fixedW = nPlayers*marginSize+marginSize;
    		fixedH = marginSize*2;
    		edgeUnitsX = 0;
    		edgeUnitsY = unitsY;
    		break;

      	case FaceToFaceLandscapeSide:	// ok 2/4/2020    	
    		/* player box top and bottom, trimming from right
  		  
		 	......._
		 	........
		 	......._
		 	
    		*/

    
       	case SideBySide:
       	case Landscape:
    		unitsX = playerW;
    		unitsY = playerH*nPlayers;
    		fixedH = nPlayers*marginSize+marginSize;
    		fixedW = marginSize*2;
    		edgeUnitsX = unitsX;
    		edgeUnitsY = 0;
    		break;


      	case FaceToFaceLandscapeTop:
    		unitsX = playerW;
      		unitsY = playerH*2;
      		fixedH = marginSize*2;
      		fixedW = marginSize*2;
      		edgeUnitsX = 0;
      		edgeUnitsY = playerH*2;
      		break;
      	
      	case RightEnd:	// rotated and placed at the right
      		unitsX = playerH;
      		unitsY = playerW;
      		fixedH = marginSize*2;
      		fixedW = marginSize*2;
      		edgeUnitsX = unitsX;
      		edgeUnitsY = 0;
      		break;
      	case Portrait:
    		unitsX = playerW;
    		unitsY = playerH*nPlayers;
	   		fixedH = marginSize*nPlayers+marginSize;
	   		fixedW = marginSize*2;
    		edgeUnitsX = 0;
    		edgeUnitsY = unitsY;
    		break;
      	case Portrait3X:
    		{unitsX = playerW*3;
    		int nrows = ((nPlayers+2)/3);
    		unitsY = playerH*nrows;
    		fixedW = marginSize*3+marginSize;
    		fixedH = nrows*marginSize+marginSize;
    		edgeUnitsX = 0;
    		edgeUnitsY = unitsY;
    		}
    		if(nPlayers%3==1) { edgeUnitsY+=playerH/2; } 	// lie, make this look unattractive if we should be using portrait2X; } 
      		break;
      		
      	case FaceToFacePortraitSide:
	   		{
	   		// means players on the short side, whichever that is,
			unitsX = playerH*2;
			unitsY = playerW;
			fixedW = marginSize*2;
			fixedH = 2*marginSize;
			edgeUnitsX = 0;
			edgeUnitsY = playerW;
			}
	   		break;
	     		
       	case FaceToFacePortrait:
       		{
       		// means players on the short side, whichever that is,
    		unitsX = playerH*2;
    		unitsY = playerW;
    		fixedW = marginSize*2;
    		fixedH = 2*marginSize;
    		edgeUnitsX = unitsY;
    		edgeUnitsY = 0;
    		}
       		break;
    	case Portrait2X:	// two column portrait
    		{
    		int nrows = ((nPlayers+1)/2);
    		unitsX = playerW*2;
    		unitsY = playerH*nrows+marginSize;
    		fixedW = marginSize*2;
    		fixedH = nrows*marginSize+marginSize;
    		edgeUnitsX = 0;
    		edgeUnitsY = unitsY;
    		}
    		break;
    	case Landscape3X:	// three column landscape
    		{
    		int nrows = ((nPlayers+2)/3);
    		unitsX = playerW*3;
    		unitsY = playerH+nrows;
    		fixedW = 3*marginSize+marginSize;
    		fixedH = nrows*marginSize+marginSize;
    		edgeUnitsX = unitsX;
    		edgeUnitsY = 0;
    		if(nPlayers%3==1) { edgeUnitsX+=playerW/2; } 	// lie, make this look unattractive if we should be using landscape2X; } 
    		}
    		break;
    	case Landscape2X:	// two column landscape
    		{
    		int nrows = ((nPlayers+1)/2);
    		unitsX = playerW*2;
    		unitsY = playerH*nrows;
    		fixedW = 2*marginSize+marginSize;
    		fixedH = nrows*marginSize+marginSize;
    		edgeUnitsX = unitsX;
    		edgeUnitsY = 0;
    		}
    		break;
     	};
    	
		double boardPercent = 0;
		double cell = (int)cellSize+2;
		double acceptedSize = -1;
		int acceptedW = 0;
		int acceptedH = 0;
		boolean sizeok = false;
		if(edgeUnitsY>0) { cell = Math.min(cell, (int)(1+height/edgeUnitsY)); }
		if(edgeUnitsX>0) { cell = Math.min(cell, (int)(1+width/edgeUnitsX)); }
		do 
		{	cell -= 1;
			double boardW = width-(edgeUnitsX*cell);
			double boardH = height-(edgeUnitsY*cell);
			double usedW = unitsX*cell+fixedW;
			double usedH = unitsY*cell+fixedH;
			sizeok = boardW>=0 && boardH>=0 && usedW<width && usedH<height;
			if(sizeok)
			{
		    double nextPercent;
		    if(strictBoardsize)
		    {
			    double sxw = boardW*Math.min(boardW/desiredAspectRatio,boardH);
			    double sxh = boardH*Math.min(boardH*desiredAspectRatio, boardW);
			    nextPercent = Math.min(sxw, sxh)/(width*height);
		    }
		    else
		    {
		    	
			double aspectRatio = boardW/boardH;
			double spareArea = boardW*boardH;
			double mina = Math.min(aspectRatio, desiredAspectRatio);
			double maxa = Math.max(aspectRatio, desiredAspectRatio);
			double plainE = (mina/maxa);
			// this requires some explanation to justify. The desired aspect ratio is intended
			// to prefer that the remainder (after placing the player boxes) is roughly the same
			// shape as the board.  One perverse result was that for hive, which has no preferred
			// shape, on wide rectangular boards, placing the player boxes side-by-side scored
			// as good as placing the boxes over-and-under.  The increased "efficiency" of 
			// making a square hole was exactly balanced by the loss of absolute area.  
			// adding the sqrt factor to efficiency will have the effect of preferring absolute
			// size over the matching the shape of the board, while still giving some preference
			// to the shape.
			double effeciency = Math.sqrt(plainE);
			nextPercent = effeciency*(spareArea/(width*height));
		    }
			if(nextPercent<=boardPercent)
				{ // if the coverage stops improving, we reached a limit 
				  // based on the aspect ratio of the board
				  cell+= 1; 
				  break; 
				}
			boardPercent = nextPercent;
			acceptedSize = cell;
			acceptedW = (int)Math.ceil(playerW*cell);
			acceptedH = (int)Math.ceil(playerH*cell);
			//G.print(""+seating+" "+boardPercent+" "+cell);
			}
		} while((!sizeok && (cell>minSize/2))
				|| ((cell>minSize) &&  (minBoardShare>0 && (boardPercent<minBoardShare))));
		boolean downsize = false;
		//G.print("accepted size "+seating+" "+cellSize+" "+minSize+" = "+acceptedSize);
		 
		if(acceptedSize>0)
		{
		do { 
			Rectangle finalBox = client.createPlayerGroup(0,0,0,0,(int)acceptedSize);
			int finalW = G.Width(finalBox);
			int finalH = G.Height(finalBox);
			downsize = (finalW>acceptedW || finalH>acceptedH);
			if(downsize)
			{	//if the createPlayerGroup is messing with fractions of the unit size, rounding can cause
				//jitter as we go to smaller sizes.  Rather than forbid it, detect it and step the unit 
				// down by 1 if it happens
				//G.print(G.format("Final box larger than expected, expected %sx%s got %sx%s cell %s min %s",acceptedW,acceptedH,finalW,finalH,acceptedSize,minSize));
				acceptedSize--;
				//G.print("downsize "+acceptedSize);
				downsize = true;
			}} while(downsize);
		}
		selectedCellSize = Math.max(1,acceptedSize);
		//G.print("final size "+seating+cellSize+" "+minSize+" = "+selectedCellSize);
    	return(boardPercent);
    }
    /**
     * get the main rectangle "as of now".  This is intended to be used
     * as a hint to decide how to place other boxes - for example if 
     * some other box should be horizontal or vertically oriented.
     *  
     * @return
     */
    public Rectangle peekMainRectangle() { return(rects.peekMainRectangle()); }
   /** get the main rectangle, nominally designated to contain the board
     * 
     * @return the big inner rectangle left after placing the player boxes
     */
    public Rectangle getMainRectangle()
    {	return rects.allocateMainRectangle();
    }

    private void addToSpare(Rectangle r)
    {	if(G.debug())
    		{
    		if ((G.Width(r)<0 || G.Height(r)<0))
    			{ G.print("Adding negative size rectangle to spare ",r);    			
    			}
    		if(!fullRect.contains(r)) 
				{G.print("spare rectangle runs off screen\n",r,"\n",fullRect);
				}
    		}
    		rects.addToSpare(r);
    }
    private void addToSpare(int left,int top,int w,int h)
    {
    	addToSpare(new Rectangle(left,top,w,h));
    }
/**
 * place the standard VCR group of rectangles
 * @param client
 * @param minW
 * @param maxW
 * @return
 */
    public boolean placeTheVcr(GameLayoutClient client,int minW,int maxW)
    {	Rectangle vcr = new Rectangle();
        if(boardMax) { G.SetRect(vcr,0,0,0,0); client.SetupVcrRects(0,0,0,0);  return true; }
        else
        {
    	RectangleSpec spec = placeRectangle(Purpose.Vcr,vcr, minW, minW/2, maxW, maxW/2, BoxAlignment.Edge,true);
    	if(spec!=null)
    	{	spec.client = client;
    		rects.finishVcr(spec);
    		return true;
    	}
    	return(false);
        }
    }
    /**
     * place the chat within the desired range of sizes.  
     * @param chatRect
     * @param minW
     * @param minH
     * @param maxW
     * @param maxH
     * @return true if successfully placed
     */
    public boolean placeTheChat(Rectangle chatRect, int minW,int minH,int maxW,int maxH)
    {	
    	RectangleSpec ok = rects.placeInMainRectangle(Purpose.Chat,chatRect,minW,minH,maxW,maxH,BoxAlignment.Edge,false,preferredAspectRatio);
    	if(ok==null)
    	{// try extra hard to present a chat
    	 Rectangle r = peekMainRectangle();
    	 ok = rects.placeInMainRectangle(Purpose.Chat,chatRect,
    			 Math.min(G.Width(r)-rects.marginSize*2,minW),
    			 Math.min(G.Height(r)-rects.marginSize*2,minH),
    			 maxW,maxH,BoxAlignment.Edge,false,preferredAspectRatio);
    	}
    	if(ok!=null)
    	{
    		ok.client = client;
    	}
    	else if(minW>0 && minH>0)
    	{ fails++; 
    	}
    	return ok!=null;
    	
    }
    private RectangleSpec placeRectangle(Purpose purpose,Rectangle targetRect,int minW,int minH,int maxW,int maxH,
			BoxAlignment align,boolean preserveAspectRatio)
    {	
    	RectangleSpec spec = rects.placeInMainRectangle(purpose,targetRect,minW,minH,maxW,maxH,align,preserveAspectRatio,
    		preferredAspectRatio);
    	if(spec==null && minW>0 && minH>0)
    		{ fails++; }
    	return spec;
    	
    }
    /** 
     * place a rectangle within the desired range of sizes.  If "preserve" is true
     * then keep the asptect ratio of minw/minh.
     * @param targetRect
     * @param minW
     * @param minH
     * @param maxW
     * @param maxH
     * @param align
     * @param preserveAspectRatio
     * @return true if successfully placed
     */
    public boolean placeRectangle(Rectangle targetRect,int minW,int minH,int maxW,int maxH,
			BoxAlignment align,boolean preserveAspectRatio)
    {	return (placeRectangle(Purpose.Other,targetRect,minW,minH,maxW,maxH,align,preserveAspectRatio)!=null);
    }

    private RectangleSpec placeRectangle(Purpose purpose,Rectangle targetRect,int minW,int minH,int maxW,int maxH,
    		int minW1,int minH1,int maxW1,int maxH1,
 			BoxAlignment align,boolean preserveAspectRatio)
     {	return (rects.placeInMainRectangle(purpose,targetRect,
    		 minW,minH,maxW,maxH,
    		 minW1,minH1,maxW1,maxH1,
    		 align,preserveAspectRatio,preferredAspectRatio));
     }
    /**
     * place the rectangle with one or the other set of constraints.  Normally the two sets
     * would be a horizontal format and a vertical format shape.  It's up to the caller to
     * work correctly with whatever dimensions were used.
     * 
     * @param targetRect
     * @param minW
     * @param minH
     * @param maxW
     * @param maxH
     * @param minW1
     * @param minH1
     * @param maxW1
     * @param maxH1
     * @param align
     * @param preserveAspectRatio
     * @return true if the rectangle was successfully placed.
     */
    public boolean placeRectangle(Rectangle targetRect,int minW,int minH,int maxW,int maxH,
    		int minW1,int minH1,int maxW1,int maxH1,
 			BoxAlignment align,boolean preserveAspectRatio)
     {	RectangleSpec spec = placeRectangle(Purpose.Other,targetRect,
    		 minW,minH,maxW,maxH,
    		 minW1,minH1,maxW1,maxH1,
    		 align,preserveAspectRatio);
     	if(spec==null && minW>0 && minH>0) 
     		{ fails++; }
     	return spec!=null;
 
     }

    public boolean placeRectangle(Purpose purpose,Rectangle targetRect,int minW,int minH,BoxAlignment align)
     {	return (rects.placeInMainRectangle(purpose,targetRect,minW,minH,minW,minH,align,true,preferredAspectRatio)!=null);
     }
    public boolean placeRectangle(Rectangle targetRect,int minW,int minH,BoxAlignment align)
    {	return (placeRectangle(Purpose.Other,targetRect,minW,minH,minW,minH,align,true)!=null);
    }

     /**
     * place the chat and log rectangles.  If there's no chat, just place the log 
     * if there is a chat and the space is big enough for the log too, place both
     * together.  Otherwise just place the log separately.
     * @param chatRect
     * @param minChatW
     * @param minChatH
     * @param maxChatW
     * @param maxChatH
     * @param logRect
     * @param minLogW
     * @param minLogH
     * @param maxLogW
     * @param maxLogH
     * @return true if the rectangle was successfully placed
     */
    public boolean placeTheChatAndLog(Rectangle chatRect,int minChatW,int minChatH,int maxChatW,int maxChatH,Rectangle logRect,
    		int minLogW,int minLogH,int maxLogW,int maxLogH)
    {
    	placeTheChat(chatRect,minChatW,minChatH,maxChatW,maxChatH);
    	int actualChatW = G.Width(chatRect);
    	int actualChatH = G.Height(chatRect);
    	int marginSize = rects.marginSize;
    	int chatY = G.Top(chatRect);
    	int chatX = G.Left(chatRect);
    	if((actualChatH>=minLogH) && (actualChatW-minLogW-marginSize>=minChatW))
          {	 int logX = chatX+actualChatW-minLogW;
          	 G.SetRect(logRect, logX,chatY,minLogW,actualChatH);
          	 G.SetWidth(chatRect,actualChatW-minLogW-marginSize);
          	 return(true);
          }
    	  if(actualChatH-minLogH-marginSize>minChatH)
    	  {	// if we expanded the chat vertically, roll back and use
    		// some of the space for the log
    		int logH = minLogH + (actualChatH - (minLogH+minChatH+marginSize*2))/2;
    		int logY = chatY+actualChatH-logH;
    		// pull back space for the log rectangle, then allocate it normally.
    		// this might result in the log going where we don't expect it.
    		G.SetHeight(chatRect,logY-chatY-marginSize);
    		addToSpare(chatX-marginSize,logY,actualChatW+marginSize*2,actualChatH+chatY+marginSize-logY);
    	  }
      if(boardMax) { G.SetRect(logRect,0,0,0,0); return true; }
   	  return(placeRectangle(Purpose.Log,logRect,minLogW,minLogH,maxLogW,maxLogH,BoxAlignment.Edge,false)!=null);  
         
    }
    /** allocate the done,edit and a block of option rectangles as a group, then split it
     * into separate rectangles.  There can be any number of option rectangles, which
     * will be allocated below the "done" and "edit" rectangles. 
     * @param boxW	min width of the done and edit buttons
     * @param maxW  max width of the done and edit buttons
     * @param done	
     * @param edit
     * @param rep
     */
    public void placeUnplannedDoneEditRep(int boxW,int maxW,Rectangle done,Rectangle edit,Rectangle... rep)
    {		int nrep = rep.length;
     		if(nrep==0 || (rep[0]==null)) { placeDoneEdit(boxW,maxW,done,edit); }
    		else
    		{
    		boolean canUseDone = client.canUseDone();
    		int marginSize = rects.marginSize;
    		boolean hasButton = (canUseDone || alwaysPlaceDone) && (edit!=null || done!=null);
    		int buttonH1 = hasButton ? boxW/2+marginSize : 0;
    		int buttonH2 = hasButton ? maxW/2+marginSize : 0;
    		Rectangle r = new Rectangle();
    		int szw = 2*boxW+marginSize;
    		int szw2 = 2*maxW+marginSize;
    		int szh = buttonH1+nrep*boxW/3;
    		int szh2 = buttonH2+nrep*boxW/3;
    		
    		int hszw = szw * (nrep+1);
    		int hszw2 = szw2 * (nrep+1);
    		int hszh = buttonH1;
    		int hszh2 = buttonH2;
    		RectangleSpec spec = placeRectangle(Purpose.DoneEditRep,r,
    					szw,szh,szw2,szh2,		// vertical format with done/edit side by side
    					hszw,hszh,hszw2,hszh2,	// horizontal format with everything flat
    				BoxAlignment.Center,true);
    		if(spec!=null)
    		{
    		if(hasButton)
    		{
    		spec.rect2 = done ;
    		spec.rect3 = edit ;
    		}
    		else
    		{	// if we're a spectator, done and edit/undo are not needed
    			spec.rect2 = spec.rect3 = null;
    			if(done!=null) { G.SetRect(done,0,0,0,0); }
    			if(edit!=null) { G.SetRect(edit,0,0,0,0); }
    		}
    		spec.rectList = rep;
    		rects.splitDoneEditRep(spec);  		
    		}}
    }
    
   
    /**
     place done-edit-rep in a situation where the done rect doesn't need to be
     placed, as is typically true for offline games.
     additional rectangles the same size as the repetition rectangle can be added.
     * 
     * @param boxW
     * @param maxW
     * @param done
     * @param edit
     * @param rep
     */
    public void placeDoneEditRep(int boxW,int maxW,Rectangle done,Rectangle edit,Rectangle... rep)
    {
    	if(plannedSeating()) 
    		{
    		  placeDoneEdit(boxW,maxW,done,edit);
    		  if(rep.length>0)
    		  {
    			  placeUnplannedDoneEditRep(boxW,maxW,null,null,rep);
    		  }
    		}
    	else
    	{
    		placeUnplannedDoneEditRep(boxW,maxW,done,edit,rep);
    	}
    }
    /**
     * place two boxes indended as the "done" and "edit" button as either left-right or over-under pair.
     * special considerations apply - the "done" button is not placed in planned seating configurations,
     * where each user's info block is expected to have its own "done" button.  
     */
    public void placeDoneEdit(int boxW,int maxW,Rectangle done,Rectangle edit)
    {		boolean canUseDone =  client.canUseDone();
    		if((done!=null)
    				&& (edit!=null) 
    				&& (plannedSeating() || !canUseDone)
    				&& !alwaysPlaceDone) 
    		{ 
    			G.SetRect(done, 0,0,0,0);
    			// spectators don't need an edit (or undo) button
    			if(!canUseDone) { G.SetRect(edit,0,0,0,0); }
    			else {
    	   			int undoW = boxW*2/3;
    	   			placeRectangle(Purpose.Edit,edit,undoW,undoW,undoW,undoW,BoxAlignment.Center,true);
    			}
    		}
    		else
    		{
    	    if(done==null && (edit!=null)) 
    	    	{ placeRectangle(Purpose.Edit,edit,boxW,boxW/2,maxW,maxW/2,BoxAlignment.Center,true); 
    	    	}
    		if(edit==null && (done!=null)) 
    			{ 	if(plannedSeating()&& !client.canUseDone()&&!alwaysPlaceDone) { G.SetRect(done,0,0,0,0); }
	    			else
	    			{
	    				placeRectangle(Purpose.Done,done,boxW,boxW/2,maxW,maxW/2,BoxAlignment.Center,true);
	    			}}
    		if(done!=null && edit!=null)
    		{
    		int szw = boxW;
    		int szw2 = maxW;
    		int marginSize = rects.marginSize;
    		int szh = boxW+marginSize;
    		int szh2 =maxW+marginSize;
    		RectangleSpec spec = placeRectangle(Purpose.DoneEdit,done,szw,szh,szw2,szh2,
    						szw*2+marginSize,szw/2,maxW*2+marginSize,maxW/2,
    						BoxAlignment.Center,true);  
    		if(spec!=null)
    		{
    		spec.rect2 = edit;
    		rects.split2(spec);
    		}}}
   }
    /** place a group intended to be the offer/accept/decline draw group.  The
     * buttons are sized to the translated size of the standard offerdraw/acceptdraw/declinedraw
     * phrases.
     * 
     * @param fm
     * @param acceptDraw
     * @param declineDraw
     */
    public void placeDrawGroup(FontMetrics fm,Rectangle acceptDraw,Rectangle declineDraw)
    {	InternationalStrings s = G.getTranslations();
    	int len1 = fm.stringWidth(s.get(OFFERDRAW));
    	int len2 = fm.stringWidth(s.get(ACCEPTDRAW));
    	int len3 = fm.stringWidth(s.get(DECLINEDRAW));
    	int h = fm.getHeight()*3;
    	int len = h+Math.max(len1, Math.max(len2, len3));
    	placeRectangle(Purpose.Draw,acceptDraw,len,h*2,len,h*2,
    			len*2,h,len*2,h,
    			BoxAlignment.Center,true);
    	if(G.Width(acceptDraw)>len)
    	{
    		G.SetWidth(acceptDraw, len);
    		G.AlignTop(declineDraw, G.Left(acceptDraw)+len,acceptDraw);
    	}
    	else { 
    		G.SetHeight(acceptDraw, h);
    		G.AlignLeft(declineDraw, G.Top(acceptDraw)+h,acceptDraw);
    	}
    }
   
    /**
     * return true if the seating chart provides seats on at least 3 sides
     * @return true if seating is "around the table"
     */
    public boolean seatingAround() { return(selectedSeating.seatingAround()); }
    /**
      * @return true if the current seating chart provides seating across the table 
     */
    public boolean seatingFaceToFace() { return(selectedSeating.seatingFaceToFace()); };
    /**
     * @return true if one of the planned seating arrangements was chosen
     */
    public boolean plannedSeating() { return((seatingFaceToFace()||seatingAround())); }

    /**
     * optimize the allocated rectangles by expanding them into the unused spaces
     * that are adjacent and still unallocated.  This allows some expandable boxes
     * to be enlarged if they were initially allocated at their minimum size.
     */
    public void optimize()
    {
    	rects.optimize();
    }
    /**
     * return an allocated rectangle to the pool of available space.
     * 
     * @param r
     */
    public void deallocate(Rectangle r)
    {
    	rects.deallocate(r);
    }
    /**
     * return space from the main rectangle to the pool, which will be
     * available in the optimization phase to expand adjacent rectangles
     * 
     * @param extraW
     * @param extraH
     */
    public void returnFromMain(int extraW,int extraH)
    {	Rectangle main = rects.centerRectangle;
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
        if(extraW>0)
        {
        	deallocate(new Rectangle(mainX,mainY,extraW,mainH));
        	deallocate(new Rectangle(mainX+mainW-extraW,mainY,extraW,mainH));
        }
        if(extraH>0)
        {
        	deallocate(new Rectangle(mainX,mainY,mainW,extraH));
        	deallocate(new Rectangle(mainX,mainY+mainH-extraH,mainW,extraH));
        }
    }
}
