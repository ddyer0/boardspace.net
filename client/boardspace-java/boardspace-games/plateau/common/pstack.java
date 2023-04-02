package plateau.common;

import java.util.*;
import lib.Graphics;
import lib.G;
import lib.GC;
import lib.HitPoint;


// stack of plateau pieces
public class pstack implements PlateauConstants
{
    PlateauBoard b = null; // the associated board
    int stacknumber = -1; // index into the stacks array
    int origin = UNKNOWN_ORIGIN; // what kind of stack this is
    int owner = -1; // the player who owns it
    int col_a_d; // the row and column for board stacks
    int row_1_4;
    Vector<piece> pieces = new Vector<piece>(); // the actual pieces in the stack
    int drawnSize = -1;
    // constructor
    public pstack(pstack from)
    { // construct an unlinked, spare copy.  This is used to keep track of
      // temporary moves in the user interface.

        int n = from.size();
        setSize(n);
        row_1_4 = from.row_1_4;
        col_a_d = from.col_a_d;
        owner = from.owner;
        origin = from.origin;

        for (int i = 0; i < n; i++)
        {
            setElementAt(from.elementAt(i), i);
        }
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
    // constructor for temporary stacks used by the mouse sprite
    public pstack(PlateauBoard bd, int or)
    {
        b = bd;
        origin = or;
    }

    // constructor for stacks used constructing the board
    public pstack(PlateauBoard bd, int or, int nn, int own)
    {
        b = bd;
        origin = or;
        stacknumber = nn;
        owner = own;
    }

    // constructor used for pieces plucked out of larger stacks,
    // in transit to a new stack.
    public pstack(PlateauBoard bd, int or, int own)
    {
        b = bd;
        origin = or;
        owner = own;
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
    {
        pieces.addElement(p);
    }

    public void insertElementAt(piece p, int i)
    {
        pieces.insertElementAt(p, i);
    }

    public boolean removeElement(piece p)
    {
        return (pieces.removeElement(p));
    }

    public String topColorString()
    {
        return (topElement().topColorString());
    }

    public piece findPiece(int type)
    {
        for (int i = 0; i < size(); i++)
        {
            piece pp = elementAt(i);

            if (pp.realPieceType == type)
            {
                return (pp);
            }
        }

        return (null);
    }

    /** 
     * @param depth
     * @return a top to bottom string of all the face colors in a stack
     */
    public String allColors(int depth)
    {
        String res = "";
        int sz = size();

        for (int i = sz - depth; i < sz; i++)
        {
            piece p = elementAt(i);
            res = p.colorString() + res;
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

            if (p.realTopColor() != MUTE_INDEX)
            {
                nc++;
            }
        }

        return (nc);
    }

    // all the colors in a stack, top to bottom
    public String allColors()
    {
        return (allColors(size()));
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
            return ("" + ((char) ('A' + col_a_d)) + (b.nrows - row_1_4));

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

    // a pretty string for debugging purposes
    public String toString()
    {
        String x = "[" + origins[origin] + "#" + stacknumber;

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
            sum += elementAt(i).pointvalue;
        }

        return (sum);
    }

    // drop a stack on top of this stack, capture the pieces from it
    public void dropStack(pstack stack)
    {
        piece prev = null;

        while (stack.size() > 0)
        {
            piece m = stack.elementAt(0);

            if (m == prev)
            {
            	throw G.Error("Stack not shrinking: " + stack);
            }

            prev = m;
            m.addToStack(this);
        }
    }

    // sum the pointvalues of a subset of pieces in the stack.  This is used
    // to figure out what totals are achievable for a prisoner exhange.
    public int sumSubset(int ss)
    {
        int sum = 0;

        for (int i = 0; i < size(); i++, ss = ss >> 1)
        {
            if ((ss & 1) == 1)
            {
                sum += elementAt(i).pointvalue;
            }
        }

        return (sum);
    }

    // make a copy on a temproary stack
    public void addCopyStack(pstack st)
    {
        for (int i = 0; i < st.size(); i++)
        {
            addElement(st.elementAt(i));
        }
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

    // return the player who owns the stack
    public int topOwner()
    {
        piece p = topPiece();

        return ((p == null) ? (-1) : p.owner);
    }

    // true if the stack contains any piece showing color on top
    // This is the test if a stack can be used to pin
    public boolean containsColor()
    {
        for (int i = 0; i < size(); i++)
        {
            piece p = elementAt(i);

            if (!p.isMute())
            {
                return (true);
            }
        }

        return (false);
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

    public int realTopBottomColor()
    {
        int sz = size();

        if (sz == 0)
        {
            return (-1);
        }

        piece p = elementAt(sz - 1);

        return (p.realBottomColor());
    }

    public int realTopColor()
    {
        int sz = size();

        if (sz == 0)
        {
            return (-1);
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
            piece m = stack.elementAt(0);
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
    
    public int hashCode()
    {	int mysize = size();
    	int v = 0;
        for (int i = 0; i < mysize; i++)
        {
            piece mypiece = elementAt(i);
            v ^= mypiece.hashCode();
        }
        return(v);
    }
    // true if the other is a stack and is a copy of this stack
    public boolean equals(Object other)
    {
        if (other instanceof pstack)
        {
            return (equals((pstack) other));
        }

        return (false);
    }


    // make us into a copy of from stack
    public void copyFrom(pstack from)
    {
        int n = from.size();
        setSize(0);
        row_1_4 = from.row_1_4;
        col_a_d = from.col_a_d;
        owner = from.owner;
        origin = from.origin;

        for (int i = 0; i < n; i++)
        {
            piece hispiece = from.elementAt(i);
            piece mypiece = b.GetPiece(hispiece.piecenumber);

            if (hispiece.isFlipped() != mypiece.isFlipped())
            {
                mypiece.flip();
            }

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
                if (this == b.takeOffStack())
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
                boolean gap = b.isFloatingPiece(nextElement, false) ||
                    b.isFloatingPiece(chip, true);
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

            if ((gc != null) && (this == b.takeOffStack()))
            {
                GC.setColor(gc,stack_marker_color);
                GC.drawArrow(gc, left + (w / 2), bottom + (realh / 4),
                    left + (w / 2), bottom, realh / 5,b.lineStrokeWidth);
            }
        }
    }
}
