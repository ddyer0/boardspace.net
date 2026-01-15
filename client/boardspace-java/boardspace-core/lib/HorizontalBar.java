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

import java.awt.Color;
/**
 * a simple drawable bar, intended to be embedded in a text glyph
 * @author Ddyer
 *
 */
public class HorizontalBar extends DrawnIcon
{
	public double percent;
	Color color;
	public int getWidth() { return((int)w); }
	public int getHeight() { return((int)h); }
	public HorizontalBar(double wid,double hei,double pc,Color c)
		{	super((int)wid,(int)hei,null);
			percent = pc;
			color = c;
		}
		public void drawChip(Graphics gc, DrawingObject c, int size, int posx, int posy, String msg) {
			double scale = (double)size/w;
			int fillW = (int)(percent*size*scale);
			//G.setColor(gc, Color.lightGray);
			//GC.fillRect(gc, posx,posy-ah,size,ah);
			GC.setColor(gc, color);
			GC.fillRect(gc, posx,posy+2-h,fillW,h-4);
			GC.setColor(gc, Color.black);
			GC.frameRect(gc, posx,posy+2-h,fillW,h-4);
		}
	}


