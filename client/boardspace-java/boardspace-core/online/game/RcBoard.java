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

import java.awt.Point;
import java.awt.Rectangle;

import java.util.Hashtable;
import lib.G;

class DPoint {
	double x;
	double y;
	DPoint(double xx,double yy) { x=xx; y=yy; }
}
/** a board-like class where the cells have random coordinates instead of being in a grid.  The picture
 * for the board should be displayed using {@link lib.G#centerImage} in a rectangle, and the same rectangle should be
 * passed to setDisplayRectangle
 * 
 * @author ddyer
 *
 */
public class RcBoard
{	private Rectangle displayRect = new Rectangle(0,0,1,1);
	private Hashtable<Object,DPoint>coords = new Hashtable<Object,DPoint>();
	private double basic_width = 1.0;
	private double basic_height = 1.0;
	
	// constructors
	/** this is the default constructor where the user supplied coordinates are 0.0-1.0 to indicate
	 * a percentage of the whole board size
	 */
	public RcBoard() {};
	/**
	 * this constructor create a board where the user supplied coordinatges are 0.0-nnn, or effectively
	 * can be pixel points in a real image.
	 * @param w
	 * @param h
	 */
	public RcBoard(double w,double h) { basic_width = w; basic_height = h; }
	
	/**
	 * adjust the specified rectangle to the centered image of the board. 
	 * @param r
	 * @param ww
	 * @param hh
	 * @return
	 */
	private Rectangle makeDisplayRectangle(Rectangle r,double ww,double hh)
	{
	double aspect_ratio = (ww/hh);
	int x_offset = G.Left(r);
	int y_offset = G.Top(r);
	int w = G.Width(r);
	int h = G.Height(r);
	if (((double)G.Width(r)/G.Height(r)) > aspect_ratio)
		{	// extra space at left
		  	x_offset += (int)(G.Width(r)-G.Height(r)*aspect_ratio)/2;
		  	w = (int)(h*aspect_ratio);
		}
		else
		{	// extra space at top
		  y_offset += (int)(G.Height(r)-G.Width(r)/aspect_ratio)/2;
		  h = (int)(w/aspect_ratio);
		}
	return(new Rectangle(x_offset,y_offset,w,h));
	}
	/**
	 * associate an x,y location with some object.  x and y are in whatever native coordinate
	 * system for the board.
	 * @param c
	 * @param xpos
	 * @param ypos
	 */
	public void setLocation(Object c,double xpos,double ypos)
	{	coords.put(c,new DPoint(xpos/basic_width,ypos/basic_height));
	}
	/**
	 * change the displayed location of the board
	 * @param r
	 */
	public void setDisplayRectangle(Rectangle r)
	{	displayRect = makeDisplayRectangle(r,basic_width,basic_height);
	}
	/**
	 * get a Point in the displayed rectangle that corresponds to the object
	 * @param c
	 * @return a point
	 */
    public Point getXY(Object c)
    {	DPoint loc = coords.get(c);
    	return(new Point(G.Left(displayRect) + (int)(G.Width(displayRect)*loc.x),
    		G.Top(displayRect) + (int)(G.Height(displayRect)*loc.y)));
    }
    /**
     * get the integer X coordinate for "c" in the current display rectangle
     * @param c
     * @return an int
     */
    public int getX(Object c)
    {	DPoint loc = coords.get(c);
    	return(G.Left(displayRect) + (int)(G.Width(displayRect)*loc.x));
    }
    /**
     * get the integer Y coordinate for "c" in the current display rectangle
     * @param c
     * @return an int
     */
    public int getY(Object c)
    {	DPoint loc = coords.get(c);
    	return(G.Top(displayRect) + (int)(G.Height(displayRect)*loc.y));
    }
}
