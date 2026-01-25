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
package volcano;

import lib.Graphics;

import online.game.*;
import lib.HitPoint;
import lib.OStack;
import lib.Random;
import lib.exCanvas;

class CellStack extends OStack<VolcanoCell>
{
	public VolcanoCell[] newComponentArray(int n) { return(new VolcanoCell[n]); }
}
public class VolcanoCell extends stackCell<VolcanoCell,Pyramid> 
	implements VolcanoConstants,PlacementProvider
{	public Pyramid[] newComponentArray(int n) { return(new Pyramid[n]); }
	public int lastPicked = -1;
	public int lastDropped = -1;
	// constructor
	public VolcanoCell(char c,int r,Geometry geom) 
	{	super(geom,c,r);
		rackLocation = VolcanoId.BoardLocation;
	}
	// constructor
	public VolcanoCell(Random r,int i,VolcanoId code)
	{	super(Geometry.Standalone,'@',i);
		rackLocation = code;
		onBoard=false;
	}
	public VolcanoId rackLocation() { return((VolcanoId)rackLocation); }

	// get the top Pyramid or null
	public void addPyramidInPosition(Pyramid p)
	{	addChip(p);
		for(int i=chipIndex;i>=1;i--)
		{	Pyramid left =chipStack[i];
			Pyramid right = chipStack[i-1];
			if(left.sizeIndex<right.sizeIndex)
			{	
				chipStack[i]=right;
				chipStack[i-1]=left;
			}
		}
	}
	public static boolean sameCell(VolcanoCell c1,VolcanoCell c2)
	{	return((c1==c2) || ((c1!=null)&&(c1.sameCell(c2))));
	}
	public void swapContents(VolcanoCell c2)
	{	
		for(int dep=0;dep<Pyramid.nSizes;dep++)
		{	Pyramid p1 = chipAtIndex(dep);
			setChipAtIndex(dep,c2.chipAtIndex(dep));
			c2.setChipAtIndex(dep,p1);
		}
		// move the caps
		if(chipIndex>c2.chipIndex) { c2.addChip(removeTop()); }
		else if(c2.chipIndex>chipIndex) { addChip(c2.removeTop()); }
	
	}

	public boolean sameTopSize()
	{	// return true if the two top pieces are the same size
		return((chipIndex>=1) 
				&& (chipAtIndex(chipIndex).sizeIndex==chipAtIndex(chipIndex-1).sizeIndex));
	}

	//
	// this supercedes the standard drawStack so the vertical spacing for stacked
	// icehouse pieces an be manipulated precisely.
	//
    public boolean drawStack(Graphics gc,HitPoint highlight,int xpos,int ypos,
    		exCanvas canvas,
    		int liftSteps,int SQUARESIZE,
    		double yscale,		// yscale is not used in this version of drawstack
    		String label)
    {	
      	boolean val = false;
    	int prevSize = 0;
    	int prevBase = 0;
    	boolean altChip = exCanvas.getAltChipset(canvas)!=0;
    	int liftdiv = 40;
    	int lastIndex = stackTopLevel()-1;
    	for(int cindex = -1; cindex<=lastIndex; cindex++)    
        {   Pyramid cup = chipAtIndex(cindex);
        	int step = ((liftSteps>0)?(int)((liftSteps*SQUARESIZE)/(1.5*liftdiv))*cindex : 0);
            int liftYval = altChip ? 0 : step;
            int liftXval =  altChip ? -step : 0;
            int e_x = xpos+(altChip ? 0 : liftXval);
            int e_y = ypos+(altChip ? liftYval : 0); 
            int thisSize = cup==null?-1:cup.sizeIndex;
           	int esize = (cindex==0)?thisSize:prevSize;
           	int lift = prevBase+Math.max(0,((cindex<=0)?0:1)+(thisSize-esize));
           	// this is where "yscale" would normally be used instead of 1/15, but
           	// 1/15 is tuned to the relative sizes of the pyramids and it's the only
           	// value that gives the desired effect of the pyrimids nesting/resting realistically
           	int ylift = (lift*SQUARESIZE)/15;
            String thislabel = (cindex==lastIndex) ? label	: null;
            e_y = altChip ? e_y : ypos-liftYval-ylift;
            e_x = altChip ? xpos-liftXval+ylift : e_x;
            val |= drawChip(gc,canvas,cup,highlight,SQUARESIZE,e_x,e_y,thislabel);
            prevSize = thisSize;
            prevBase = lift;
         }	
  		return(val);
    }
    public void reInit()
    {
    	super.reInit();
    	lastPicked = -1;
    	lastDropped = -1;
    }
    public void copyFrom(VolcanoCell ot)
    {
    	super.copyFrom(ot);
    	lastPicked = ot.lastPicked;
    	lastDropped = ot.lastDropped;
    }
	public int getLastPlacement(boolean empty) {
		
		return empty? lastPicked : lastDropped;
	}	

}
