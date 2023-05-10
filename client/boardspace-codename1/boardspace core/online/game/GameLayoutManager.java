package online.game;

import com.codename1.ui.geom.Rectangle;

import bridge.FontMetrics;
import lib.G;
import lib.InternationalStrings;
import online.common.SeatingChart.DefinedSeating;
import online.game.PlayConstants.BoxAlignment;


/**
 * this is the expert for constructing board layouts.  In this version of the algorithm,
 * the player boxes are placed first, around the periphery of the screen, so as to leave
 * a large unused area for the board.  The contstraints try to provide both a reasonable
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
 * @author Ddyer
 *
 */
public class GameLayoutManager  implements Opcodes
{	
	public enum Purpose
	{
		Chat,Done,DoneEdit,DoneEditRep,Edit,
		Log,Banner,Vcr,Draw,
		Other;
	}
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
	
	int positions[][] = null;	// the x,y coordinates of the player boxes
	double rotations[] = null;	// the rotations of the player boxes
	
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

	public void addSkinnyLeftFrom(boolean addSkinnyLeft,int spareY,boolean addSkinnyRight,boolean fromtop,boolean tobottom)
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
			addToSpare(new Rectangle(left,spareYT,playerHM,spareYH));
			addToSpare(new Rectangle(left,spareYB,playerHM,spareYBH));
			}
		if(addSkinnyRight)
			{	int spareX = right-playerHM;
				addToSpare(new Rectangle(spareX,spareYT,playerHM,spareYH));
				addToSpare(new Rectangle(spareX,spareYB,playerHM,spareYBH));
			}
		}
	}
	
	// add spare rectangles for skinny margins at left or left and right
	private void addSkinnyRight(boolean addSkinnyRight,boolean tall)
	{
		int spareY = ycenter-playerWM/2;
		int spareYT = tall ? top : top+playerHM;
		int spareYH = spareY-spareYT;
		int spareYB = spareY+playerWM;
		int spareYBH = bottom-spareYB;
		int spareX = right-playerHM;
		addToSpare(new Rectangle(spareX,spareYT,playerHM,spareYH));
		addToSpare(new Rectangle(spareX,spareYB,playerHM,spareYBH));
	}
	// add spare rectangles for skinny margins at left or left and right
	private void addSkinnyOffset(int spareX)
	{
		int spareY = ycenter-playerWM/2-playerHM/2;
		int spareYH = spareY-top;
		int spareYB = spareY+playerWM;
		int spareYBH = ybot-spareYB;
		addToSpare(new Rectangle(spareX,top,playerHM,spareYH));
		addToSpare(new Rectangle(spareX,spareYB,playerHM,spareYBH));
	}
	// add spare rectangles for fat margins at left or left and right, with the side box centered
	// this is used only by FiveAroundEdge and SixAroundEdge
	private void addFatLeftCentered(boolean addLeft,boolean addRight,boolean six)
	{	int spareY0 = ytop+playerHX+1;
		int spareY = top+ycenter-playerWM/2;
		int spareY2 = spareY+playerWM;
		if(addLeft)
		{
		addToSpare(new Rectangle(left,spareY0,playerWM,spareY-spareY0));
		addToSpare(new Rectangle(left,spareY2,playerWM,ybot-spareY2));
		int spareX = playerHM;
		addToSpare(new Rectangle(spareX,spareY,playerWM-playerHM,spareY2-spareY));
		}
		// rectangles ok 2/26
		if(addRight)
		{
		if(six)
		{	// the right-top rectangle starts at the true top.
			addToSpare(new Rectangle(xright,spareY0,playerWM,spareY-spareY0));
		}
		else {
			addToSpare(new Rectangle(xright,ytop,playerWM,spareY-ytop));
		}
			addToSpare(new Rectangle(xright,spareY2,playerWM,ybot-spareY2));
			addToSpare(new Rectangle(xright,spareY,playerWM-playerHM,spareY2-spareY));
		}
	}


	// add spare rectangles for .X.X. spacing 
	private void add2XAcross(int ycoord)
	{	
		addToSpare(new Rectangle(left,ycoord,xthirdLeft-left,playerHM));
		int spareX = xthirdRight+playerWX;
		addToSpare(new Rectangle(spareX,ycoord,right-spareX,playerHM));
		spareX = xthirdLeft+playerWX;
		addToSpare(new Rectangle(spareX,ycoord,xthirdRight-spareX,playerHM));
	}
	
	// add spare rectangles for ..X.. spacing
	private void add1XAcross(int ycoord)
	{
		int spareX = xmid+playerWX;
		addToSpare(new Rectangle(spareX,ycoord,right-spareX,playerHM));
		addToSpare(new Rectangle(left,ycoord,xmid-left,playerHM));
	}


	// add spare rectangles for X.X.X spacing
	private void add3XAcross(int ycoord)
	{
		int spareX = left+playerWM;
		addToSpare(new Rectangle(spareX,ycoord,xmid-spareX-1,playerHM));
		spareX = xmid+playerWX;
		addToSpare(new Rectangle(spareX,ycoord,xright-spareX-1,playerHM));
	}
	private void addSidebar(int xcoord)
	{	int spareY = ycenter-playerWX/2;
		addToSpare(new Rectangle(xcoord,top,playerHM,spareY-top));
		int spareYB = ycenter+playerWM/2;
		addToSpare(new Rectangle(xcoord,spareYB,playerHM,bottom-spareYB));
	}
	private void addTopToBottom(int xcoord,boolean topTop)
	{
		int spareY = topTop?0:ytop+playerHX;
		// extra space between the top and bottom on the right side
		addToSpare(new Rectangle(xcoord,spareY,playerWM,ybot-spareY));
	}
	private void addSideToSide(int top)
	{
		addToSpare(new Rectangle(left+playerWM,top,right-left-playerWM*2,playerHM));
	}
	
	// add as horizontal segments for corner-edge arrangements
	private void addSpareVStrip(int spareX,int spareX2)
	{	int stripH = (bottom-playerWM-playerHM)/2;
		addToSpare(new Rectangle(spareX,0,playerWM,stripH));
		addToSpare(new Rectangle(spareX,bottom-playerHM-stripH,playerWM,stripH));
		if(playerWM>playerHM) { addToSpare(new Rectangle(spareX2,stripH,playerWM-playerHM,playerWM)); }
	}
	
	// add as horizontal segments for corner-edge arrangements
	private void addSpareVStripFrom(int stripY,int spareX,int spareX2)
	{	
		addToSpare(new Rectangle(spareX,0,playerWM,stripY));
		int strip2H = (bottom-playerHM)-(stripY+playerWM);
		if(strip2H>0)
		{
		addToSpare(new Rectangle(spareX,stripY+playerWM,playerWM,strip2H));
		}
		if(playerWM>playerHM) { addToSpare(new Rectangle(spareX2,stripY,playerWM-playerHM,playerWM)); }
	}
	static double noRotation[] = {0,0,0,0,0,0};
	static double fourAcrossRotation[] = {0,0,Math.PI,Math.PI,Math.PI};	//extra pi for fivearound
	static double fourAroundRotation[] = {0,Math.PI/2,Math.PI,-Math.PI/2};
	static double fiveAroundRotation[] = { 0,0,Math.PI/2,Math.PI,-Math.PI/2};
	static double sixAroundRotation [] = { 0,0,Math.PI/2,Math.PI,Math.PI,-Math.PI/2};
	public double preferredAspectRatio = 1.0;
	public RectangleManager rects = new RectangleManager(1.0);
	
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
	rotations = noRotation;
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
	xsideLeft = left+playerHM/2-playerWX/2;		// so when rotated will center on left
	xsideRight = right-(playerHM+playerWM)/2;	// so when rotated will center on right
	
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
		addToSpare(new Rectangle(left,top,xright-left,playerHM));
		addToSpare(new Rectangle(left,ybot,xright-left,playerHM));
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
		 * |....|
		 * ......
		 * ......
		 * ......
		 */
			int yt = margin+(playerWM-playerHM)/2;
			rotations = new double[]{Math.PI/2,-Math.PI/2};
			positions = new int[][]{{xsideLeft,yt},	// left 
				{xsideRight,yt},		// right
				};

		addToSpare(new Rectangle(left,playerWM,playerHM,bottom-playerWM));
		addToSpare(new Rectangle(right-playerHM,playerWM,playerHM,bottom-playerWM));
		left += playerHM;
		right -= playerHM;
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
		// left corner, with the chip from left and right, leavinbg
		// the full height available in the center
		rotations = new double[]{ 0,Math.PI/2};
		positions = new int[][] { {xright,ybot},		// bottom ..X..
								  {xsideLeft,ymid}};
		// ok 8/2/2021
		addSkinnyLeft(false,true,true);
		addTopToBottom(xright,true);
		left += playerHM;
		right -= playerWM;
		break;
		
	case RightCornerWide: // ok 2/4/2020
		// right corner with the chop from left and right, leaving 
		// the full height available in the center
		rotations = new double[]{ 0,-Math.PI/2};
		positions = new int[][] { {xleft,ybot},		// bottom ..X..
								  {xsideRight,ymid}};
		// ok 8/2/2021
		addSkinnyRight(false,true);
		addTopToBottom(left,true);
		right -= playerHM;
		left += playerWM;
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
		positions = new int[][]{{ xleft,start}, {x2,start},{x3,start},
					 { xleft,start+playerHM}, {x2,start+playerHM},{x3,start+playerHM}};
		int spareX = left+playerWM*3;
		int spareH = playerHM*(rows+1);
		bottom -= spareH;
		// rectangles ok 2/26
		addToSpare(new Rectangle(spareX,bottom,right-spareX,spareH));
		int rem = nPlayers%3;
		if(rem!=0)
			{
			addToSpare(new Rectangle(left+rem*playerWM,bottom+playerHX,playerWM*(3-rem),playerHM+marginSize));
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
		positions = new int[][]{{ xleft,start}, {x2,start},
					 { xleft,start+playerHM}, {x2,start+playerHM},
					 { xleft,start+2*playerHM}, {x2,start+playerHM*2}};
		int spareX = left+playerWM*2;
		// rectangles ok 2/26
		int spareH = playerHM*(rows+1);
		bottom -= spareH;
		int spareW = right-spareX;
		if(spareW>10) { addToSpare(new Rectangle(spareX,bottom,spareW,spareH)); }
		if((nPlayers&1)!=0)
			{
			addToSpare(new Rectangle(left+playerWM,bottom+(nPlayers>4?playerHM:0)+playerHX,playerWM,playerHM+marginSize));
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
		addToSpare(new Rectangle(spareX,ybot,xmid-spareX,playerHM));
		int spareX2 = xmid+playerWM;
		addToSpare(new Rectangle(spareX2,ybot,right-playerHM-spareX2,playerHM));

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
			addToSpare(new Rectangle(spareX,top,right-spareX,spareH));	
			addToSpare(new Rectangle(left,top,xmid-left,spareH));
			int yb = bottom-spareH;
			addToSpare(new Rectangle(spareX,yb,right-spareX,spareH));	
			addToSpare(new Rectangle(left,yb,xmid-left,spareH));
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
		
	case ThreeAroundR:	// ok 10/5/2022
		/* three around in a U shape, leaving the left unoccupied
		
		......_.....
		|...........
		......_.....
		
	 	*/
		{
		rotations = new double[] {0,Math.PI/2,Math.PI};
		positions = new int[][] { {xmid,ybot},		// bottom ..X..
								  {xsideRight,ymid},
								  {xmid,ytop}};		// top ..X..};	// right
								
		int spareY = ycenter-playerWM/2;
		int spareX = xmid+playerWX;
		int spareH = Math.min(playerHM,spareY-ytop);
		
		// this is like addx1across, but takes into account that the 
		// sideways recangles at the left and right might eat into it.
		addToSpare(new Rectangle(spareX,top,right-spareX,spareH));	
		addToSpare(new Rectangle(left,top,xmid-left,spareH));
		int yb = bottom-spareH;
		addToSpare(new Rectangle(spareX,yb,right-spareX,spareH));	
		addToSpare(new Rectangle(left,yb,xmid-left,spareH));

		// rectangles ok 8/2/2021
		addSkinnyRightOnly(false,false);				
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
			int boxY2 = ybot-(playerWM-playerHM)/2;
			positions = new int[][] { {xleft,ybot}, 	// bottom X....
									{ xsideLeft,ytop+(playerWM-playerHM)/2}, 	// left, aligned to box top
									{ xright,ytop},		// top ....X 
								{xsideRight,boxY2}};	// right, aligned to box bottom
			// ok 3/27/2023
			{				
			int spareY = top+playerWM;
			int spareX = left+playerHM;
			int spareXW = playerWM-spareX;		
			addToSpare(new Rectangle(spareX,top,spareXW,spareY-top));
			addToSpare(new Rectangle(left,spareY,playerWM,ybot-spareY));
			}	
			right -= playerWM;
			left += playerWM;

			{
			int spareY = top+playerHM;
			int spareH = bottom-playerWM-spareY;
			addToSpare(new Rectangle(right,spareY,playerWM,spareH));
			addToSpare(new Rectangle(right,spareY+spareH,playerWM-playerHM,playerWM));
			}
		}
		break;

	case FourAroundEdge:	// ok 2/4/2020
		{	/* like four around, but place the top and bottom rectangles near the left and right
					_...........
					|..........|
					..........._  
		
		 	*/
			rotations = fourAroundRotation;
			int boxY2 = ybot-(playerWM-playerHM+2)/2;
			positions = new int[][] { {xleft,ybot}, 	// bottom X....
									{ xsideLeft,ytop+(playerWM-playerHM)/2}, 	// left, aligned to box top
									{ xright,ytop},		// top ....X 
								{xsideRight,boxY2}};	// right, aligned to box bottom
			// in this layout we clip left and right margins only
			// and the spare rectangles are in the new left and right spaces
			// ok 2/26

			int spareY = top+playerWM;
									
			// between horizontal bottom and vertical right
			addToSpare(new Rectangle(playerWM+margin,bottom-playerHM,right-playerHM-playerWM-margin*2,playerHM));
			
			// between vertical left and horizontal bottom
			addToSpare(new Rectangle(left,spareY,playerHM,ybot-spareY));
			spareY = top+playerHM;
			int spareY2 = bottom - playerWM;
			// between horizontal top and vertical right
			addToSpare(new Rectangle(right-playerHM,spareY,playerHM,spareY2-spareY));
			// between left vertical and top horizontal
			addToSpare(new Rectangle(playerHM,top,right-playerWM-playerHM-margin,playerHM));

			left += playerHM;
			right -= playerHM;
			top += playerHM;
			bottom -= playerHM;
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
				addToSpare(new Rectangle(spareX,bottom,spareW,playerHM));
			}
		}
		break;

	case Portrait:	// ok 2/4/2020
		{
		positions = new int[nPlayers][2] ;
		bottom -= playerHM*nPlayers;
		for(int i=0;i<nPlayers;i++) { positions[i][0] = xleft; positions[i][1]=bottom+i*playerHM; }
		int spareX = xleft+playerWM;
		addToSpare(new Rectangle(spareX,bottom,right-spareX,playerHM*nPlayers));
		}
		break;
	case Landscape3X:
		{
		positions = new int[][] { {xright-playerWM*2,ytop},{xright-playerWM,ytop},{xright,ytop},
								  {xright-playerWM*2,ytop+playerHM},{xright-playerWM,ytop+playerHM}, { xright, ytop+playerHM}};
		right -= playerWM*3;
		int spareY = top+(playerHM*((nPlayers+2)/3));
		// rectangles ok 2/26
		addToSpare(new Rectangle(right,spareY,playerWM*3,bottom-spareY));
		int rem = (nPlayers%3);
		if(rem!=0)
			{
			addToSpare(new Rectangle(right+rem*playerWM-marginSize,spareY-playerHM,(3-rem)*playerWM+marginSize,playerHM));
			}
		}
		break;
	
	case Landscape2X:
		{
		positions = new int[][] { {xright-playerWM,ytop},{xright,ytop},
			{xright-playerWM,ytop+playerHM}, { xright, ytop+playerHM},
			{xright-playerWM,ytop+playerHM*2}, { xright,ytop+playerHM*2}};
		right -= playerWM*2;
		int spareY = top+(playerHM*((nPlayers+1)/2));
		// rectangles ok 2/26
		addToSpare(new Rectangle(right,spareY,playerWM*2,bottom-spareY));
		if((nPlayers&1)!=0)
		{
			addToSpare(new Rectangle(right+playerWX,spareY-playerHM,playerWM+marginSize,playerHM));
		}
		}
		break;
	case SideBySide:   // ok 2/4/2020
	case Landscape:
		{
		positions = new int[nPlayers][2];
		for(int i=0;i<nPlayers;i++) { positions[i][0] = xright; positions[i][1]=ytop+i*playerHM; }
		int spareY =ytop+playerHM*nPlayers-marginSize+1;	// spare rects ok 2/26
		addToSpare(new Rectangle(xright,spareY,right-xright,bottom-spareY));
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
			G.setRotation(playerRectc, -(window.getPlayerOrTemp(i).displayRotation));
			if(!full.contains(playerRectc)) 
				{G.print("player rectangle runs off screen\n",playerRectc,"\n",full);
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
	 * font sizes, and that changed as the window zoomed up.
	 * 
	 * The "new" version uses zoom>1.0, corresponding to the global zoom factor,
	 * and allocates based on width/zoom and height/zoom, which ought to be the
	 * actual (unzoomed) size of the window.   Sized parameters, minsize maxsize 
	 * ought to be based on the the global default font size, or some other
	 * metric which is not affected by the zoom.
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
		fullRect = new Rectangle(0,0,fullwidth,fullheight);
		int extramargin = G.isRealLastGameBoard()||G.isRealPlaytable()?G.minimumFeatureSize()/2 : 0;
		int width = (int)(fullwidth/zoom)-extramargin;
		int height = (int)(fullheight/zoom)-extramargin;
		rects.zoom = zoom;
		if((nPlayers!=selectedNPlayers) || ((zoom==1.0) ? !client.isZoomed() : true))
		{
		double bestPercent = 0;
		double bestScore = 0;
		double desiredAspectRatio = aspectRatio;
		double targetPreference = targetLayoutHysterisis;
		DefinedSeating best = null;
		DefinedSeating currentSeating = client.seatingChart();
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
	    	double currentScore = currentPercent*currentCellSize;
	    	//G.print("S "+currentSeating+" "+currentPercent+" "+currentScore+" "+currentPercent+" "+currentCellSize);
	    	if(currentCellSize>1 && (best==null || currentScore>bestScore))
	    	{	//G.print("Better "+currentPercent+" score ",currentScore," ",currentSeating);
	    		bestPercent = currentPercent;
	    		bestScore = currentScore;
	    		best = currentSeating;
	    		//sizeLayout(client,nPlayers,currentSeating,minBoardShare,desiredAspectRatio,maxCellSize,minSize,width,height);
		    	}}
	    	currentSeating = currentSeating.alternate;
	    	if(currentSeating==originalSeating) { currentSeating=null; }
	    	}
	    }
	    else // if there is no target, there's no preference either.
	    { targetPreference = 1.0; 	// no target to prefer
	      best = currentSeating;
	    }
	    
		for(DefinedSeating s : tryThese)	// also try the generic layouts 
		{
			double v = sizeLayout(client,nPlayers,s,minBoardShare,desiredAspectRatio,maxCellSize,minSize,width,height,margin);
			double score = v*selectedCellSize;
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
	    sizeLayout(client,nPlayers,selectedSeating,minBoardShare,aspectRatio,maxCellSize,minSize,width,height,margin);

	    if(selectedCellSize<=0)
	    {
	    	// in the rare case that the best layout still didn't produce a valid cell size,
	    	// try once more with no minimum
	    	if(G.debug()) { G.print("Emergency resize"); }
		    sizeLayout(client,nPlayers,selectedSeating,0.2,aspectRatio,maxCellSize,1,width,height,margin);
	    }
	    		
		int halfMargin = extramargin/2;
		//G.print("Make ",selectedSeating);
		makeLayout(selectedSeating,nPlayers,halfMargin,halfMargin,width,height,client.createPlayerGroup(0,0,0,0,(int)selectedCellSize),margin);
    	//if(G.debug())
    	//{
    	//	G.print("Cell Min ",minSize," max ",maxCellSize," actual ",selectedCellSize);
    	//}
        doLayout(client,zoom,(int)selectedCellSize,new Rectangle(halfMargin,halfMargin,fullwidth-margin,fullheight-margin));
        rects.specs.clear();
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

    		unitsX = playerW;
    		unitsY = playerH*2+playerW;
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
       		unitsX = playerW*2;
    		unitsY = playerH+playerW;
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*3;
    		edgeUnitsX = playerW*2;
    		edgeUnitsY = 0;
    		break;
	
		case FourAroundEdge:
			/* like four around, but place the top and bottom rectangles near the left and right
			 
			_...........
			|..........|
			..........._  
			 
			 */
       		unitsX = playerW*2;
    		unitsY = playerH;
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*3;
    		edgeUnitsX = playerW*2;
    		edgeUnitsY = playerH;
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
    		unitsY = playerW*2;
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
    		
			_........
			|.......|
			_......._
			*/
   			unitsX = playerW*2;
    		unitsY = playerH*2+playerW;
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*4;

    		edgeUnitsX = playerW*2;
    		edgeUnitsY = 0;
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
 		// seating charts not represented in the seating viewer
    	case RightCornerWide:
      	case LeftCornerWide:
       		unitsX = playerW+playerH;
      		unitsY = playerW;
      		fixedW = 2*marginSize;
      		fixedH = 2*marginSize;
      		edgeUnitsX = playerH+playerW;
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
			edgeUnitsX = playerH*2;
			edgeUnitsY = 0;
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
				|| ((cell>minSize) &&  (boardPercent<minBoardShare)));
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
    
    public Rectangle peekMainRectangle() { return(rects.peekMainRectangle()); }
   /** get the main rectangle, nominally designated to contain the board
     * 
     * @return the big inner rectangle left after placing the player boxes
     */
    public Rectangle getMainRectangle()
    {	return rects.allocateMainRectangle();
    }

    public void addToSpare(Rectangle r)
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


    public boolean placeTheVcr(GameLayoutClient client,int minW,int maxW)
    {	Rectangle vcr = new Rectangle();
    	RectangleSpec spec = placeRectangle(Purpose.Vcr,vcr, minW, minW/2, maxW, maxW/2, BoxAlignment.Edge,true);
    	if(spec!=null)
    	{	spec.client = client;
    		rects.finishVcr(spec);
    		return true;
    	}
    	return(false);
    }
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
    	return ok!=null;
    }
    private RectangleSpec placeRectangle(Purpose purpose,Rectangle targetRect,int minW,int minH,int maxW,int maxH,
			BoxAlignment align,boolean preserveAspectRatio)
    {	RectangleSpec spec = rects.placeInMainRectangle(purpose,targetRect,minW,minH,maxW,maxH,align,preserveAspectRatio,
    		preferredAspectRatio);
    	return spec;
    }
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
    public boolean placeRectangle(Rectangle targetRect,int minW,int minH,int maxW,int maxH,
    		int minW1,int minH1,int maxW1,int maxH1,
 			BoxAlignment align,boolean preserveAspectRatio)
     {	RectangleSpec spec = placeRectangle(Purpose.Other,targetRect,
    		 minW,minH,maxW,maxH,
    		 minW1,minH1,maxW1,maxH1,
    		 align,preserveAspectRatio);
     	return spec!=null;
     }

    public RectangleSpec placeRectangle(Purpose purpose,Rectangle targetRect,int minW,int minH,BoxAlignment align)
     {	return (rects.placeInMainRectangle(purpose,targetRect,minW,minH,minW,minH,align,true,preferredAspectRatio));
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
    		addToSpare(new Rectangle(chatX-marginSize,logY,actualChatW+marginSize*2,actualChatH+chatY+marginSize-logY));
    	  }
    	  
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
    		int marginSize = rects.marginSize;
    		boolean hasButton = edit!=null || done!=null;
    		int buttonH1 = hasButton ? boxW/2+marginSize : 0;
    		int buttonH2 = hasButton ? maxW/2+marginSize : 0;
    		Rectangle r = new Rectangle();
    		int szw = 2*boxW+marginSize;
    		int szw2 = 2*maxW+marginSize;
    		int szh = buttonH1+nrep*boxW/3;
    		int szh2 = buttonH2+nrep*boxW/3;
    		RectangleSpec spec = placeRectangle(Purpose.DoneEditRep,r,szw,szh,szw2,szh2,
    				BoxAlignment.Center,true);
    		if(spec!=null)
    		{
    		spec.rect2 = done;
    		spec.rect3 = edit;
    		spec.rectList = rep;
    		rects.splitDoneEditRep(spec);  		
    		}}
    }
    
   
    //
    // place done-edit-rep in a situation where the done rect doesn't need to be
    // placed, as is typically true for offline games.
    //
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
    public void placeDoneEdit(int boxW,int maxW,Rectangle done,Rectangle edit)
    {		
    		if((done!=null) && (edit!=null) && plannedSeating() && !alwaysPlaceDone) { 
    			G.SetRect(done, 0,0,0,0);
    			int undoW = boxW*2/3;
    			placeRectangle(Purpose.Edit,edit,undoW,undoW,undoW,undoW,BoxAlignment.Center,true);
    		} 
    		else
    		{
    	    if(done==null && (edit!=null)) { placeRectangle(Purpose.Edit,edit,boxW,boxW/2,maxW,maxW/2,BoxAlignment.Center,true); }
    		if(edit==null && (done!=null)) 
    			{ 	if(plannedSeating()&&!alwaysPlaceDone) { G.SetRect(done,0,0,0,0); }
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

    public void optimize()
    {
    	rects.optimize();
    }
    public void deallocate(Rectangle r)
    {
    	rects.deallocate(r);
    }
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
