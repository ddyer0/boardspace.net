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

/* internal storage for a mouse event */
public class CanvasMouseEvent
{	public CanvasMouseEvent next = null;	// next event after this one
	public int x;							// the x
	public int y;							// the y
	public MouseState event;				// event type
	public int button;						// buttons that were down
	public long date;						// timestamp
	public double amount;					// amount of pinch
	public double twist;					// angle of twist
	public boolean first;
	public String toString() { return("<CanvasMouseEvent "+event+">"); }
	// constructor
	public CanvasMouseEvent(MouseState e,int ax,int ay,int ab,long d,double am,double tw,MouseState prev)
	{
		event = e;
		x = ax;
		y = ay;
		button = ab;
		date = d;
		amount = am;
			twist = tw;
			first = e!=prev;
	}
	// merge this event with a subsequent event of the same type
	public void merge(int ax,int ay,long d,double am,double tw)
	{
		x = ax;
		y = ay;
		date = d;
		amount = am;
			twist = tw;
	}
	}
