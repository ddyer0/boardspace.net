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

public class CanvasRotater
{
	
	private int canvasRotation=0;
	public  int getCanvasRotation() { return(canvasRotation); }
	public  void setCanvasRotation(int n) {
		canvasRotation = n&3;
    }
	public CanvasRotater() { }
	private boolean quarterTurn()
	{
		return (getCanvasRotation()&1)!=0;
	}
	public int getRotatedWidth(SizeProvider canvas)
	{
		return (quarterTurn() ? canvas.getHeight() : canvas.getWidth());
	}
	public int getRotatedHeight(SizeProvider canvas)
	{
		return quarterTurn() ? canvas.getWidth() : canvas.getHeight();
	}
	
	// this is only correct if windows are full screen.  If windows are less than full screen
	// it should reflect where the window falls within the screen
	public int getRotatedLeft(SizeProvider c) { return c.getX(); }
	
	// this is only correct if windows are full screen.  If windows are less than full screen
	// it should reflect where the window falls within the screen
	public int getRotatedTop(SizeProvider c) { return c.getY(); }

	public boolean rotateCanvas(Graphics g,SizeProvider canvas)
	{	
		if(g==null) { return false; }
		G.Assert(!g._rotated_,"already rotated");
		
		int rot = getCanvasRotation();
		if(rot==0) { g._rotated_ = false; return false; }
		int w = canvas.getWidth()/2;
		int h = canvas.getHeight()/2;
		double r = rot*Math.PI/2;
		switch(rot)
		{
		case 2: 
			GC.setRotation(g,r);
			GC.translate(g,-w*2,-h*2);
			break;
		case 1:
			{
			int wh = (w-h);
			int dx=  wh/2;
			int cx = h+dx;
			int cy = w-dx;			
			GC.translate(g,cx, cy);
		    GC.setRotation(g,r);
		    GC.translate(g,-cx, -cy-wh);
			}
			break;
		case 3:
			{	
			int wh=  (w-h);
			int dx = wh/2;
			int cx = h+dx;
			int cy = w-dx;
			GC.translate(g,cx,cy);
			GC.setRotation(g,r);
			GC.translate(g,wh-cx,-cy);
			}
			break;
		}
		g._rotated_ = true;
		return true;
	}

	public void unrotateCanvas(Graphics g,SizeProvider canvas)
	{	//System.out.println("un "+client+" "+g);
		if(g==null) { return; }
		G.Assert(g._rotated_,"not the rotated");
		g._rotated_=false;
		int rot = getCanvasRotation();
		if(rot==0) { return; }
		int w = canvas.getWidth()/2;
		int h = canvas.getHeight()/2;
		double r = -rot*Math.PI/2;
		switch(rot)
		{
		case 2: 
			GC.setRotation(g,r,w,h);
			break;
		case 1:
			{
			int wh =  (w-h);
			int dx = wh/2;
			int cx = h+dx;
			int cy = w-dx;
			GC.translate(g,cx,cy+wh);
			GC.setRotation(g,r);
			GC.translate(g,-cx,-cy);
			}
			break;
		case 3:
			{	
			int wh =  (w-h);
			int dx = wh/2;
			int cx = h+dx;
			int cy = w-dx;
			GC.translate(g,cx-wh,cy);
			GC.setRotation(g,r);
			GC.translate(g,-cx,-cy);
			}
			break;
		}
	}

	public int rotateCanvasX(int x, int y,SizeProvider canvas)
	{
		switch(getCanvasRotation())
		{
		default:
		case 0:	return x;
		case 2: return canvas.getWidth()-x;
		case 3: return canvas.getHeight()-y;
		case 1: return y;
		}
	}

	public int rotateCanvasY(int x, int y,SizeProvider canvas) 
	{
		switch(getCanvasRotation())
		{
		default:
		case 0:	return y;
		case 2: return canvas.getHeight()-y;
		case 3: return x;
		case 1: return canvas.getWidth()-x;
		}
	}

	public int unrotateCanvasX(int x, int y,SizeProvider canvas)
	{
		switch(getCanvasRotation())
		{
		default:
		case 0:	return x;
		case 2: return canvas.getWidth()-x;
		case 1: return canvas.getWidth()-y;
		case 3: return y;
		}
	}
	public int unrotateCanvasY(int x, int y,SizeProvider canvas) 
	{
		switch(getCanvasRotation())
		{
		default:
		case 0:	return y;
		case 2: return canvas.getHeight()-y;
		case 1: return x;
		case 3: return canvas.getHeight()-x;
		}
	}

}
