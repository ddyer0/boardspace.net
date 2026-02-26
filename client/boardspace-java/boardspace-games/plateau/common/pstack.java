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
package plateau.common;

import java.awt.Color;
import java.util.*;
import lib.Graphics;
import lib.Drawable;
import lib.DrawingObject;
import lib.G;
import lib.GC;
import lib.Random;
import lib.HitPoint;
import lib.OStack;
import online.game.PlacementProvider;

class CellStack extends OStack<pstack>
{
	public pstack[] newComponentArray(int sz) {
		
		return new pstack[sz];
	}
	
}

class RackIcon implements Drawable
{
	pstack []rack = null;
	public RackIcon(pstack[]from)
	{
		rack = new pstack[from.length];
		for(int i=0;i<from.length;i++)
		{
			rack[i] = new pstack(from[i],null);
		}
	}
	public void draw(Graphics gc, DrawingObject c, int size, int posx, int posy, String msg) {
		int len = rack.length;
		int step = size/len;
		int x = (int)(posx-step*(len/2.0));
		int y = posy;
		GC.frameRect(gc,Color.black,x-step/2,y-step/2,step*len,step);
		for(pstack p : rack)
		{
			p.draw(gc,c,step,x,y,null);
			x += step;
		}
	}

	public int getWidth() {
		return (rack.length-1)*100;
	}

	public int getHeight() {
		return 100;
	}
	
}
// stack of plateau pieces
public class pstack implements PlateauConstants, PlacementProvider, Drawable
{	pstack next = null;
	static final int PARTIAL_COPY = 1001;
	static final int MOUSE_COPY = 1002;
	static final int FULL_COPY = 1000;
    PlateauBoard b = null; // the associated board
    int stackNumber = -1; // index into the stacks array
    int origin = UNKNOWN_ORIGIN; // what kind of stack this is
    int owner = -1; // the player who owns it
    private char col;
    private int row;
    public char col() { return col;}
    public int row() { return row; }
    public int colNum() { return col-'A'; }
    public int rowNum() { return row-1; }
    public int lastPicked = -1;
    public int lastDropped = -1;
    public boolean isCopy = false; 
    Vector<piece> pieces = new Vector<piece>(); // the actual pieces in the stack
    int drawnSize = -1;
    // constructor
    public pstack(pstack from,piece pieces[])
    { // construct an unlinked, spare copy.  This is used to keep track of
      // temporary moves in the user interface.

        int n = from.size();
        setSize(n);
       	b = from.b;
        row = from.row;
        col = from.col;
        owner = from.owner;
        origin = from.origin;
        stackNumber = from.stackNumber;
        isCopy = true;
         for (int i = 0; i < n; i++)
        {	piece fp = from.elementAt(i);
            setElementAt(pieces==null ? new piece(fp) : pieces[fp.piecenumber], i);
        }
    }

    // constructor for temporary stacks used by the mouse sprite
    public pstack(PlateauBoard bd, int or)
    {
        b = bd;
        origin = or;
        stackNumber = MOUSE_COPY;
        isCopy = true;
    }

    public String pickSubset(int ss)
    {	StringBuilder str = new StringBuilder();
    	String comma = "";
    	
        for (int i = 0; i < size(); i++, ss = ss >> 1)
        {
            if ((ss & 1) == 1)
            {
            	int chip = elementAt(i).piecenumber;
            	str.append(comma);
            	str.append(chip);
             	comma = ",";
            }
        }
        return (str.toString());
    }
    public boolean containsStackOfSix(int forplayer)
    {
    	int start = size()-1;
    	int nin = 0;
    	while(start>=0)
    	{	piece pp = elementAt(start);
    		if(pp.owner==forplayer) { nin++; if(nin==6) { return(true); }}
    		else { nin = 0; if(start<5) { return(false); }}
    		start--;
    	}
    	return(false);
    }

    // constructor for stacks used constructing the board
    public pstack(PlateauBoard bd, int or, int nn, int own,char colum,int rown)
    {
        b = bd;
        origin = or;
        stackNumber = nn;
        owner = own;
        col = colum;
        row = rown;
    }

    // constructor used for pieces plucked out of larger stacks,
    // in transit to a new stack.
    public pstack(PlateauBoard bd, int or, int own)
    {
        b = bd;
        origin = or;
        owner = own;
        stackNumber = PARTIAL_COPY;
        isCopy = true;
        
    }

    // methods to encapsulate vector behavior.  We encapsulate
    // rather then extend vector so we can avoid casts in all
    // the consumers, and so we can tweak the behavior of "final"
    // vector methods.
    public int size()
    {
        return (pieces.size());
    }

    public piece elementAt(int i)
    {
        if (i == pieces.size())
        {
            return (null);
        }

        return (pieces.elementAt(i));
    }

    public int indexOf(piece p)
    {	int v = pieces.indexOf(p);
        return (v);
    }

    public void setSize(int n)
    {
        pieces.setSize(n);
    }

    public void setElementAt(piece p, int i)
    {
        pieces.setElementAt(p, i);
    }

    public void addElement(piece p)
    {	//G.Assert(!contains(p),"should be a new piece");
    	//G.Assert(p.myBoard==b,"should be same board");
        pieces.addElement(p);
    }

    public void insertElementAt(piece p, int i)
    {
        pieces.insertElementAt(p, i);
    }
    public piece remove(int i)
    {
    	return pieces.remove(i);
    }
    public boolean removeElement(piece p)
    {
        return (pieces.removeElement(p));
    }

    public String topColorString()
    {
        return (topElement().topColorString());
    }


    /** 
     * @param depth
     * @return a top to bottom string of all the face colors in a stack
     */
    public String allRealColors(int depth)
    {
        String res = "";
        int sz = size();

        for (int i = sz - depth; i < sz; i++)
        {
            piece p = elementAt(i);
            res = p.realColorString() + res;
        }

        return (res);
    }

    /** 
     * this is the test if a stack can be used to pin
     * @param depth
     * @return true if this stack contains a colored top face above height "depth".
     */
    public int containsColor(int depth)
    {
        int sz = size();
        int nc = 0;

        for (int i = sz - depth; i < sz; i++)
        {
            piece p = elementAt(i);

            if (p.realTopColor() != Face.Blank)
            {
                nc++;
            }
        }

        return (nc);
    }
    public boolean containsOwner(int who)
    {
    	for(int sz = size()-1; sz>=0; sz--)
    	{
    		if(elementAt(sz).owner == who) { return true; }
    	}
    	return false;
    }
    // all the colors in a stack, top to bottom
    public String allRealColors()
    {
        return (allRealColors(size()));
    }

    public String pieces()
    {
        String val = "";
        String comma = "";

        for (int i = 0; i < size(); i++)
        {
            val += (comma + elementAt(i).piecenumber);
            comma = ",";
        }

        return (val);
    }

    public String locus()
    {
        switch (origin)
        {
        case BOARD_ORIGIN:
            return ("" + col + row);

        case RACK_ORIGIN:
            return ("R");

        case CAPTIVE_ORIGIN:
            return ("P");

        case TRADE_ORIGIN:
            return ("T");

        default:
        	throw G.Error("no locus for " + this);

        case UNKNOWN_ORIGIN:
            return ("U");
        }
    }
    
    public String stackNumberString()
    {	String msg = isCopy ? "copy of " : "";
    	switch(stackNumber)
    	{
    	default: msg += "#"+stackNumber; break;
    	case MOUSE_COPY: msg = "Mouse"; break;
    	case FULL_COPY: msg += "All of #"+stackNumber; break;
    	case PARTIAL_COPY: msg += "Part of #"+stackNumber; break;
    	}

    	return msg;

    }

    // a pretty string for debugging purposes
    public String toString()
    {	
        String x = "[" + origins[origin] + stackNumberString();

        if (origin == BOARD_ORIGIN)
        {
            x += ("-" + locus());
        }

 
        for (int i = pieces.size() - 1; i >= 0; i--)
        {
            x += pieces.elementAt(i).toString();
        }

        x += " ] ";

        return (x);
    }

    // sum the point values of the contained pieces
    public int pointTotal()
    {
        int sum = 0;

        for (int i = 0; i < size(); i++)
        {
            sum += elementAt(i).pieceType.value;
        }

        return (sum);
    }

    // drop a stack on top of this stack, capture the pieces from it
    public void dropStack(pstack stack)
    {
         while (stack.size() > 0)
        {
            piece m = stack.remove(0);
            m.addToStack(this);
        }
    }

    // sum the point values of a subset of pieces in the stack.  This is used
    // to figure out what totals are achievable for a prisoner exhange.
    public int sumSubset(int ss)
    {
        int sum = 0;

        for (int i = 0; i < size(); i++, ss = ss >> 1)
        {
            if ((ss & 1) == 1)
            {
                sum += elementAt(i).pieceType.value;
            }
        }

        return (sum);
    }

    // make a copy on a temproary stack
    public pstack addCopyStack(pstack st)
    {
        for (int i = 0; i < st.size(); i++)
        {
            addElement(st.elementAt(i));
        }
        return this;
    }
    public Face topFace()
    {
        return (topPiece().visTopColor());
    }
    public boolean stompCapture()
    {
        if(size()==0) { return (false); }
        return(!elementAt(0).unobstructed());
    }
   
    public piece topPiece()
    {
        int sz = size();

        return ((sz > 0) ? elementAt(sz - 1) : null);
    }

    public void revealTop()
    {
        piece p = topPiece();

        if (p != null)
        {
            p.revealTop();
        }
    }
    public boolean contains(piece p)
    {
    	for(int i=size()-1; i>=0; i--) { if(elementAt(i)==p) { return true; }}
    	return false;
    }
    /* 
     * true if the stack contains a colored top before from_h (the bottom) and to_h (the top)
     */
    public boolean containsColor(int from_h,int to_h)
    {
    	for(int i=from_h; i<to_h; i++)
    	{
    		piece p = elementAt(i);
    		if(!p.isMute()) { return(true); }
    	}
    	return(false);
    }
    // true if the stack contains any piece showing color on top
    // This is the test if a stack can be used to pin
    public boolean containsColor()
    {	return(containsColor(0,size()));
    }
    // return the player who owns the stack
    public int topOwner()
    {
        piece p = topPiece();

        return ((p == null) ? (-1) : p.owner);
    }


    // the takeoff height moving from a board piece.  This is not
    // necessarily the same as the stack height or the number of 
    // like-colored pieces in the stack.
    public int takeOffHeight()
    {
        int x = 0;
        int top = size() - 1;

        if (top >= 0)
        {
            piece p = elementAt(top);

            while ((top >= 0) && ((elementAt(top)).owner == p.owner))
            {
                x++;
                top--;
            }

         }

        return (x);
    }

    // get the top piece in a stack, or null for an empty stack
    public piece topElement()
    {
        int sz = size();

        if (sz == 0)
        {
            return (null);
        }

        return (elementAt(sz - 1));
    }

    public Face realTopBottomColor()
    {
        int sz = size();

        if (sz == 0)
        {
            return (Face.Unknown);
        }

        piece p = elementAt(sz - 1);

        return (p.realBottomColor());
    }

    public Face realTopColor()
    {
        int sz = size();

        if (sz == 0)
        {
            return (Face.Unknown);
        }

        piece p = elementAt(sz - 1);

        return (p.realTopColor());
    }

    // drop a stack inside this stack, under one of our pieces
    public void dropStackUnder(piece under, pstack stack)
    {
        int target = indexOf(under);
        dropStackUnder(target, stack);
    }

    public void dropStackUnder(int target, pstack stack)
    {
        while (stack.size() > 0)
        {
            piece m = stack.remove(0);         
            m.addToStack(this, target);
        }
    }

    public int captureDepth()
    {
        int own = topOwner();
        int depth = 1;

        for (int i = size() - 2; (i >= 0) && (elementAt(i).owner == own);
                i--)
        {
            depth++;
        }

        return (depth);
    }
    public int captureDepth(int owner,int max)
    {	int caps= 0;
    	for(int idx=size()-1,step=max; step>0 && idx>=0;idx--,step--)
    	{
    		piece p = elementAt(idx);
    		if(p.owner!=owner) { caps++; }
    	}
    	return caps;
    }
    // drop a stack on top of this stack
    public void dropStack(int level, pstack stack)
    {
        if (level >= size())
        {
            dropStack(stack);
        }
        else
        {
            dropStackUnder(level, stack);
        }
    }

    // true if the other stack is a copy of this stack
    public boolean equals(pstack other)
    {
        int mysize = size();

        if (other.size() == mysize)
        {
            for (int i = 0; i < mysize; i++)
            {
                piece mypiece = elementAt(i);
                piece hispiece = other.elementAt(i);

                if (!mypiece.equals(hispiece))
                {
                    return (false);
                }
            }

            return (true);
        }

        return (false);
    }
    


    // make us into a copy of from stack
    public void copyFrom(pstack from)
    {
        int n = from.size();
        setSize(0);
        row = from.row;
        // note that this copy should NOT include the b link, it causes
        // bad matches in copyFrom
        //b = from.b;
        col = from.col;
        owner = from.owner;
        origin = from.origin;
        lastPicked = from.lastPicked;
        lastDropped = from.lastDropped;
        for (int i = 0; i < n; i++)
        {
            piece hispiece = from.elementAt(i);
            piece mypiece = b.getPiece(hispiece.piecenumber);
            mypiece.addToStack(this);
        }
    }
    // create a digest of this stack, for cheap duplicate detection
    public long Digest(Random r)
    {
        long v = r.nextInt();
        int n = size();

        for (int i = 0; i < n; i++)
        {
            piece p = elementAt(i);
            v ^= p.Digest(r);
        }

        return (v);
    }
    public long Piece_Digest()
    {
        long v = 0;
        int n = size();

        for (int i = 0; i < n; i++)
        {
            piece p = elementAt(i);
            v ^= p.Piece_Digest();
        }

        return (v);
   	
    }
    // draw a representation of this stack.  This is done recursively
    // from the bottom, with special case for the top of the stack.
    public void Draw(Graphics gc, int left, int top, int w, int h,
        int maxheight, HitPoint highlight)
    {
        int realw = (int) (w * 0.95); // use 90% of the cell
        int realh = (int) (h * 0.95);
        int pheight = (int) (realw * PASPECT);
        int pspacing = (int) (pheight * STACKSPACING); // stack pieces this far apart

        if (realh < ((pspacing * (maxheight - 1)) + pheight))
        { // recalculate for a too-short field

            double units = ((maxheight - 1) * STACKSPACING) + 1;
            realw = (int) (realh / (units * PASPECT));
            pheight = (int) (realw * PASPECT);
            pspacing = (int) (pheight * STACKSPACING);
        }
        else
        { // recalculate based on surplus height
            realh = (pspacing * (maxheight - 1)) + pheight;
        }

        int bottom = (top + h) - ((h - realh) / 2) - pheight;
        int stackh = size() - 1;
        int realx = left + ((w - realw) / 2);

        //gc.setColor(Color.white);
        //gc.fillRect(realx,top+(h-realh)/2,realw,realh);
        //gc.setColor(Color.yellow);
        //gc.fillRect(realx,bottom,realw,pheight);
        piece prevchip = null;

        if (stackh < 0)
        {
            boolean inhi = (highlight == null) 
            	? false
                : (highlight.dragging// fatten sentitivity vertically
                      && G.pointInRect(highlight, realx, bottom - ((2 * pheight) / 3),realw, pheight * 2));
           if(inhi)
            {
            boolean hi = b.LegalToHit(this, null, false);
            if (hi)
            {
                if (gc != null)
                {
                    GC.setColor(gc,stack_marker_color);
                    GC.fillOval(gc,realx - 2, bottom - 2, realw + 2, pheight + 2);
                }
                else
                {
                    highlight.hitCode = PlateauId.HitEmptyRack;
                    highlight.hitObject = this;
                }
            }
            else if (gc != null)
            {
                if (b!=null && this == b.takeOffStack())
                {
                    GC.setColor(gc,stack_marker_color);
                    GC.drawArrow(gc, left + (w / 2), bottom + (realh / 4),
                        left + (w / 2), bottom, realh / 5,b.lineStrokeWidth);
                }
            }
            }
        }
        else
        {
            for (int idx = 0; idx <= stackh; idx++)
            {
                piece chip = elementAt(idx);
                piece nextElement = (idx < stackh) ? elementAt(idx + 1) : null;
                boolean gap = b!=null 
                		&& (b.isFloatingPiece(nextElement, false) || b.isFloatingPiece(chip, true));
                chip.DrawTop(gc, realx, bottom, realw, pheight, prevchip,
                    gap || (idx == stackh), highlight, pspacing);
                prevchip = chip;
                bottom -= pspacing;

                if (gap)
                {
                    bottom -= (pspacing * 2);
                    prevchip = null;
                }
            }

            if ((gc != null) && (b!=null) && (this == b.takeOffStack()))
            {
                GC.setColor(gc,stack_marker_color);
                GC.drawArrow(gc, left + (w / 2), bottom + (realh / 4),
                    left + (w / 2), bottom, realh / 5,b.lineStrokeWidth);
            }
        }
    }
    
    public int controlledStackHeight(int forplayer)
    {	int h = size()-1;
    	int n = 0;
    	while(h>=0)
    	{	
    	piece pp = elementAt(h--);
    	if(pp.owner!=forplayer) { return(n); }
    	n++;
    	}
    	return(n);
    }
    public int getLastPlacement(boolean empty) {
		return empty ? lastPicked : lastDropped;
	}
    public void swap(piece out,piece in)
    {	int idx = indexOf(out);
    	pstack ostack = in.mystack;
    	int oindex = ostack.indexOf(in); 
    	setElementAt(in,idx);
    	in.mystack = this;
    	ostack.setElementAt(out,oindex);
    	out.mystack = ostack;
     }
	public void draw(Graphics gc, DrawingObject c, int size, int posx, int posy, String msg)
	{
		Draw(gc,posx-size/2,posy-size/2,size,size,size(),null);		
	}
	public int getWidth() {
		return 100;
	}
	public int getHeight() {
		return 100;
	}
}
