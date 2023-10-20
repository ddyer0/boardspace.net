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
import lib.Graphics;

import lib.Random;
import lib.exCanvas;
import lib.CellId;
import lib.G;
import lib.HitPoint;
/**
 * this is the principal subclass of {@link cell} for cells whose contents
 * is just a single object.  The methods here are designed to be compatible
 * with those of {@link stackCell} so the same coding idioms can be used for both.
 * 
 * @see stackCell
 * @author ddyer
 *
 */
public abstract class chipCell
		<FINALTYPE extends chipCell<FINALTYPE,COMPONENTTYPE>,
		 COMPONENTTYPE extends chip<?>> 
		extends cell<FINALTYPE> {
	/**
	 * the contents of the cell.  Normally use the {@link #topChip} method.
	 */
	public COMPONENTTYPE chip=null;					// contents of the cell
	/**
	 * funcion for interop with stackCell
	 */
	public COMPONENTTYPE topChip() { return(chip); }
	/**
	 * get the height of the stack (ie 0 or 1 for this type of cell)
	 */
	public int height() { return((chip!=null)?1:0); }
	/** 
	 * return true if the cell is empty
	 * @return true if the cell is empty
	 */
	public boolean isEmpty() { return(chip==null); }
	/**
	 * constructor for a singleton
	 */
	public chipCell() { super(); }
	/**
	 * constructor for a cell with a rackLocation
	 * @param loc
	 */
	public chipCell(CellId loc) { super(loc); }
	/**
	 * constructor for a cell on the board with a particular geometry
	 * @param geo
	 * @param co
	 * @param ro
	 */
	public chipCell(Geometry geo,char co,int ro) { super(geo,co,ro); }
	
	/**
	 * constructor for a cell on the board with a particular geometry and a rack location
	 * @param geo
	 * @param loc
	 * @param co
	 * @param ro
	 */
	public chipCell(Geometry geo,CellId loc,char co,int ro) { super(geo,loc,co,ro); }
	
	/**
	 * constructor for a singleton
	 */
	public chipCell(Random r) { super(r); }
	/**
	 * constructor for a cell with a rackLocation
	 * @param loc
	 */
	public chipCell(Random r,CellId loc) { super(r,loc); }
	/**
	 * constructor for a cell on the board with a particular geometry
	 * @param geo
	 * @param co
	 * @param ro
	 */
	public chipCell(Random r,Geometry geo,char co,int ro) { super(r,geo,co,ro); }

	
	/**
	 * clear the contents of this cell.  This method is normally wrapped
	 * by subclasses to encapsulate the initialization process.
	 */
	public void reInit() { super.reInit(); chip=null; }
	
/**
 * copy this contents of this cell from the other.  This method is normally
 * wrapped by subclasses to encapsulate the copying process.  Copying should
 * be aware of shared structure, and that "other" may be a cell in the same location
 * on a different board.
 */
	public void copyAllFrom(FINALTYPE other)
	{	super.copyAllFrom(other);
		copyFrom(other);
	}
	public void copyFrom(FINALTYPE other)
	{	super.copyFrom(other);
		chip = other.chip;
	}
	public boolean sameContents(FINALTYPE other) { return(chip==other.chip); }

	/**
	 * return a short description string of the contents
	 */
	public String contentsString() { return(chip==null?"":chip.contentsString()); }
	/**
	 * remove the top chip from this cell, which must be the indicated one.
	 * use this as a consistency check when you "know" what should be about to be removed.
	 */
	public void removeChip(chip<?> oldChip)
	{	G.Assert(chip==oldChip,"removing wrong chip");
		chip = null;
	}
	/** 
	 * remove the top chip of the cell.  It's an error if the cell is 
	 * already empty.
	 * @return the chip removed
	 */
	public COMPONENTTYPE removeTop() 
	{ 	G.Assert(chip!=null,"there is no chip to remove");
		COMPONENTTYPE ch = chip; 
		chip = null;
		return(ch); 
	}
	/**
	 * add a chip to this cell.  It's an error if the cell is not empty.
	 * @param newchip
	 */
	public void addChip(COMPONENTTYPE newchip) 
	{ 	if(onBoard) { G.Assert(chip==null,"Cell is not empty"); }
		chip = newchip;
	}
	/**
	 * generate a digest of the cell identity and contents.  The cell
	 * identity is normally the next random number in sequence.
	 */
	public long Digest(Random r) 
		{ long val= super.Digest(r);
		  if(chip!=null) { val += chip.Digest(r); }
		  return(val);
		}

	/**
	 * draw this chip and it's contents, note if the mouse falls inside.  This uses {@link #stackTopLevel()}
	 * to determine if there is a chip to draw.
	 * @param gc
	 * @param canvas
	 * @param highlight
	 * @param SQUARESIZE
	 * @param e_x
	 * @param e_y
	 * @param thislabel
	 * @return true if this chip was hit
	 */
  	public boolean drawChip(Graphics gc,exCanvas canvas,HitPoint highlight,int SQUARESIZE,int e_x,int e_y,String thislabel)
	{	return(drawChip(gc,canvas,stackTopLevel()>0?chip:null,highlight,SQUARESIZE,e_x,e_y,thislabel));
	}
  	/**
  	 * draw the entire stack (in this case, at most one chip) and note if the
  	 * mouse falls inside.  This method has the same signature as {@link stackCell#drawStack}
  	 * so code can be written to use drawStack for either subclass.
  	 * @param gc
  	 * @param highlight
  	 * @param xpos
  	 * @param ypos
  	 * @param canvas
  	 * @param liftSteps
  	 * @param SQUARESIZE
  	 * @param yscale
  	 * @param label
  	 * @return true if this stack was hit
  	 */
    public boolean drawStack(Graphics gc,HitPoint highlight,int xpos,int ypos,
    		exCanvas canvas,
    		int liftSteps,int SQUARESIZE,double yscale,
    		String label)
    {	return(drawChip(gc,canvas,highlight,SQUARESIZE,xpos,ypos,label));
    }
    
	// method for "DrawableSprite" interface
	public void drawChip(Graphics gc,exCanvas c,int size, int posx,int posy,String msg)
	{
		drawStack(gc,null,posx,posy,c,0,size,1.0,msg);
	}

	// support for StackIterator
	public int size() {
		return 1;
	}
	@SuppressWarnings("unchecked")
	public FINALTYPE elementAt(int n) {
		if(n==0) { return (FINALTYPE)this; }
		throw G.Error("Index out of range %s",n);
	}

	/**
	 * create a bitmap of 1<<direction of the directions for which exit in that direction is null
	 * this is used in some connection games to determine wins, and in some boards to decorate the borders
	 * 
	 * @return
	 */
	public int borderDirectionMask()
	{
		int bd = 0;
        for(int direction=0;direction<geometry.n;direction++)
        {
        	if((exitTo(direction)==null) && (exitTo(direction+1)==null))
        	{	bd |= (1<<direction);
        	}
        }
        return bd;
	}
}
