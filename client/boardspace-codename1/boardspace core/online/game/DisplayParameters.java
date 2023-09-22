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
package online.game;

import com.codename1.ui.geom.Rectangle;

import lib.G;
/**
 * this is a captive class for use by various board types.  Each game has one "real" board,
 * but there can be many copies of the board.  One instance of DisplayParameters is shared
 * by all copies of the board, so whichever board copy is being displayed, the same parameters
 * will apply.
 * 
 * @author Ddyer
 *
 */
public class DisplayParameters
{	public double CELLSIZE;
	 public double YCELLSIZE;
	 public double GRIDSIZE;
	 public double rotation = 0; 	//rotation in degrees clockwise

	 private double sinrot = 0.0;
	 private double cosrot = 1.0;
	 public int XOFFSET = 0;				// y offset
	 public int YOFFSET = 0;				// x offset
	 double XPERSPECTIVE = 0.0;		// x shrinkage as a function of y
	 double YPERSPECTIVE = 0.0;		// y shrinkage as a function of x
	 double XSKEW = 0.0;			// x as a function of y.  Zero is no effect
	 boolean inited = false;
	 public int HEIGHT = 0;
	 public int WIDTH = 0; 
	 double YSCALE = 1.0;
	 double SCALE = 1.0;
	 private double RAW_XOFF = 0.0;
	 private double RAW_YOFF = 0.0;
	 private boolean REGULAR=true;
	 
	 boolean INTERPOLATE = false;	// another way to define the board's coordinate system is by defining 4 corners
	 double LLCOORD[] = null;
	 double LRCOORD[] = null;
	 double ULCOORD[] = null;
	 double URCOORD[] = null;

	   
	    // setup for torroidal board display.  This is used in conjunction with emptyColumn
	    public int yspandist;
	    public int xyspan;
	    public int xspandist;
	
	public boolean reverse_y = false;
	public boolean reverse_x = false;
	public boolean swapXY = false;			// swapXY used when rotating the board, only use for square boards!
	
	boolean autoReverseY = false;
	boolean autoReverseYNormal = false;


	 //
	 // add a perspective term to X position
	 //
	 double XtoXP(double xpos0, double ypos0)
	 {
	     double dcx = (((WIDTH / 2) - xpos0) * ((XPERSPECTIVE * ypos0) / HEIGHT));

	     return (xpos0 + dcx);
	 }

	 /**
	  * define the board coordinate system by "something like" a transformation matrix.
	  * not really, and hard to use, but is used when matching a real photo with perspective
	  * to the board's coordinate system.
	  * 
	  * @param scale
	  * @param yscale
	  * @param xoff
	  * @param yoff
	  * @param rot
	  * @param xperspec
	  * @param yperspec
	  * @param skew
	  */
	 public void SetDisplayParameters(double scale, double yscale, double xoff,double yoff,double rot,
	         double xperspec, double yperspec,double skew)
	 {
	         XPERSPECTIVE = xperspec;
	         YPERSPECTIVE = yperspec;
	         rotation = rot;
	         YSCALE = yscale;
	         SCALE = scale;
	         RAW_XOFF = -xoff;
	         RAW_YOFF = -yoff;
	         XSKEW = skew;
	         REGULAR = ((rot==0.0) &&  (yscale==1.0) && (xperspec==0.0) && (yperspec==0.0));
	         INTERPOLATE = false;
	         inited = true;
	     }
	 /**
	  * set up an interpolated coordinate system defined by 4 corners.
	  * 
	  * @param LL
	  * @param LR
	  * @param UL
	  * @param UR
	  */
	 public void SetDisplayParameters(double LL[],double LR[],double UL[],double UR[])
	 {	LLCOORD = LL;
	 	LRCOORD = LR;
	 	ULCOORD = UL;
	 	URCOORD = UR;
	 	REGULAR = false;
	 	INTERPOLATE = true;
	 	inited = true;
	 }	 
	 
	 /**
	  * set up a scaled coordinate system bounded by a rectangle.  This encapsulates
	  * one of the other coordinate systems so it fits inside.
	  * 
	  * @param r
	  * @param yCellRatio
	  * @param yGridRatio
	  * @param ncols
	  */
	 protected void setDisplayRectangle0(Rectangle r,double yCellRatio,double yGridRatio,int ncols)
	 {
	     double dang = (rotation * 2 * Math.PI) / 360; // convert to radians
	     sinrot = Math.sin(dang);
	     cosrot = Math.cos(dang);
	     XOFFSET = YOFFSET = 0;
	     HEIGHT = G.Height(r);
	     WIDTH = G.Width(r);
	     CELLSIZE = (double) G.Width(r)/ ncols;				// CELLSIZE is the x differential between a cell and it's adjacent cell
	     YCELLSIZE = CELLSIZE * yCellRatio;	// YCELLSIZE is the y differential between a cell and the cell immediately above it
	     GRIDSIZE = CELLSIZE / yGridRatio;		// gridsize is the Y differential between two adjacent columns
	 }
	 protected void setDisplayRectangle1(Rectangle r,Bbox br,double yCellRatio,double yGridRatio )
	 {	int w = G.Width(r);
	 	int h = G.Height(r);
	     double wrat = SCALE / ((double) (br.right - br.left) / w);
	     double hrat = SCALE / ((double) (br.bottom - br.top) / h);
	     double rat =  Math.min(wrat, hrat);
	     // cellsize was set by setdisplayrectangle0
	     CELLSIZE = (CELLSIZE * rat);
	     YCELLSIZE = (CELLSIZE * yCellRatio);

	     if(REGULAR) { CELLSIZE=(int)CELLSIZE; YCELLSIZE=(int)YCELLSIZE; }
	    
	     GRIDSIZE = ( CELLSIZE / yGridRatio);
	     double xdif = rat * ((br.left + br.right) / 2);
	     double ydif = rat * ((br.top + br.bottom) / 2);
	     XOFFSET = (w / 2) - (int) (RAW_XOFF*CELLSIZE + xdif);
	     YOFFSET = (h / 2) - (int) (RAW_YOFF*CELLSIZE + ydif);
	 } 
	 
	 public double rotateX(double x00, double y00)
	 {
	     if (rotation != 0)
	     {
	         double x0 = x00;
	         double y0 = y00;

	         // Xrotated = X * COS(angle) - Y * SIN(angle)        
	         // Yrotated = X * SIN(angle) + Y * COS(angle)   
	         return ((x0 * cosrot) - (y0 * sinrot));
	     }

	     return (x00);
	 }
	 public double rotateY(double x00, double y00)
	 {
	     if (rotation != 0)
	     {	 
	         double x0 = x00;
	         double y0 = y00;

	         // Xrotated = X * COS(angle) - Y * SIN(angle)        
	         // Yrotated = X * SIN(angle) + Y * COS(angle)   
	         return ((x0 * sinrot) + (y0 * cosrot)+XSKEW*x00);
	     }

	     return (y00);
	 }
	 

	 // add a y perspective term 
	 double YtoYP(double ypos00)
	 {
	     return (ypos00 - ((ypos00 * ypos00 * YPERSPECTIVE) / HEIGHT));
	 }



}