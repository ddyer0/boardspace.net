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

import bridge.Color;

import online.common.exCanvas;
/**
 * a simple drawable bar, intended to be embedded in a text glyph
 * @author Ddyer
 *
 */
public class HorizontalBar implements Drawable
{
	 	public void rotateCurrentCenter(double amount,int x,int y,int cx,int cy) {};
	 	public double activeAnimationRotation() { return(0); }
		double w;
		double h;
		double percent;
		Color color;
		public int getWidth() { return((int)w); }
		public int getHeight() { return((int)h); }
		public HorizontalBar(double wid,double hei,double pc,Color c)
		{
			w = wid;
			h = hei;
			percent = pc;
			color = c;
		}
		public void drawChip(Graphics gc, exCanvas c, int size, int posx, int posy, String msg) {
			double scale = size/w;
			int ah = (int)(h);
			int fillW = (int)(percent*size*scale);
			//G.setColor(gc, Color.lightGray);
			//GC.fillRect(gc, posx,posy-ah,size,ah);
			GC.setColor(gc, color);
			GC.fillRect(gc, posx,posy+2-ah,fillW,ah-4);
			GC.setColor(gc, Color.black);
			GC.frameRect(gc, posx,posy+2-ah,fillW,ah-4);
		}
		public String getName() { return(toString()); }
		public int animationHeight() {
			return 0;
		} 	
	}


