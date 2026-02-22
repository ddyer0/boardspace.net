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

import lib.Graphics;
import lib.Image;
import lib.OStack;
import bridge.Color;
import lib.G;
import lib.Random;
import lib.GC;
import lib.HitPoint;

class PieceStack extends OStack<piece>
{

	public piece[] newComponentArray(int sz) {
		return new piece[sz];
	}
	
}
// plateau piece
public class piece implements PlateauConstants
{
    // these image arrays are initialized by the viewer class but used here.
    public static Image[][] top_images;
    public static Image[][] single_images;
    public static Image[][] stack_images;
    int owner; // the player who own the piece
    int piecenumber; // index into the board's piece array
    long randomv ;
    boolean hittop = false; // true if this piece is current hit on top by the mouse
    pstack mystack; // the stack which contains this piece.
    PlateauBoard myBoard = null;
    private Face vis_top_color; // the color showing on top
    private Face vis_bottom_color; // the color (not)showing on bottom
    private Face real_top_color;
    private Face real_bottom_color;
    private int knownMask = 0;
    private static int knownMask(boolean top,int player)
    {
    	return (1<<(player+(top?2:0)));
    }
    int allKnown = knownMask(true,0)|knownMask(true,1)|knownMask(false,0)|knownMask(true,1);
    public void setTopKnown(int p) { knownMask |= knownMask(true,p); }
    public void setBottomKnown(int p) { knownMask |= knownMask(false,p); }
    public boolean allKnown() { return knownMask==allKnown; }
    public boolean topKnown(int pl)
    {
    	return (knownMask & knownMask(true,pl))!=0;
    }
    
    public boolean bottomKnown(int pl)
    {	int mask =  knownMask(false,pl);
    	int v = knownMask & mask;
    	return (v)!=0;
    }
    private void flipKnowns()
    {	// this can be optimized later
    	boolean top0 = topKnown(0);
    	boolean top1 = topKnown(1);
    	boolean bottom0 = bottomKnown(0);
    	boolean bottom1 = bottomKnown(1);
    	knownMask = 0;
    	if(bottom0) { setTopKnown(0); }
    	if(bottom1) { setTopKnown(1); }
    	if(top0) { setBottomKnown(0); }
    	if(top1) { setBottomKnown(1); }
    }
    private boolean flipped;
    PieceType pieceType; // remembers the real piece type of anonymized pieces

	public void copyFrom(piece other) {
		vis_top_color = other.vis_top_color;
		vis_bottom_color = other.vis_bottom_color;
		real_top_color = other.real_top_color;
		real_bottom_color = other.real_bottom_color;
		flipped = other.flipped;
		owner = other.owner;
		knownMask = other.knownMask;
	}
	// create a copy not used on the board, used for game log
	public piece(piece other)
	{
		copyFrom(other);
		mystack = other.mystack;
	}
	
    // constructor for permanent pieces
    public piece(PlateauBoard b,PieceType p,int own, int ind,long rv)
    {	myBoard= b;
        owner = own;
        piecenumber = ind;
        pieceType = p;
        real_top_color = p.topColor;
        real_bottom_color = p.bottomColor;
        randomv = rv;
        revealAll();
    }

    // constructor for temporary pieces
    public piece(PlateauBoard b,int own, Face color)
    {	myBoard = b;
        owner = own;
        real_top_color = vis_top_color = color;
        real_bottom_color = vis_bottom_color = Face.Unknown;
    }


    boolean equals(piece other)
    {
        boolean va = ((piecenumber == other.piecenumber) &&  (flipped == other.flipped));

        if (!va)
        {
        	throw G.Error("Mismatch");
        }

        return (va);
    }

    void anonymize()
    {
        vis_top_color = Face.Unknown;
        vis_bottom_color = Face.Unknown;
        knownMask = 0;
    }

    public String locus()
    {
        return (mystack.locus() + " " + height());
    }

    public String topColorString()
    {
        return vis_top_color.shortName;
    }

    public Face visTopColor()
    {
        return (vis_top_color);
    }

    public Face realTopColor()
    {
        return (real_top_color);
    }

    public Face realBottomColor()
    {
        return (real_bottom_color);
    }
    public Face visBottomColor() 
    {
    	return vis_bottom_color;
    }
    public String bottomColorString()
    {
        return (vis_bottom_color.shortName);
    }

    public String colorString()
    { 	// top first
        return (vis_top_color.shortName +  vis_bottom_color.shortName);
    }

    public String realTopColorString()
    { // top first
        return (real_top_color.shortName);
    }

    public String realBottomColorString()
    { // top first

        return (real_bottom_color.shortName);
    }

    public String realColorString()
    { // top first
        return (real_top_color.shortName + real_bottom_color.shortName);
    }

    void resetColor(String col)
    {
        for (Face f : Face.values())
        {
            if (f.shortName.equals(col))
            {
                vis_top_color = f;
             }
        }
    }

    public void flipup()
    {
        if (flipped)
        {
            flip();
        }
    }

    public void revealAll()
    {
        vis_top_color = real_top_color;
        vis_bottom_color = real_bottom_color;
        setTopKnown(0);
        setTopKnown(1);
        setBottomKnown(0);
        setBottomKnown(1);
    }

    public void revealTop()
    {
    	// reveal just the top
    	vis_top_color = real_top_color;
    	setTopKnown(0);
    	setTopKnown(1);
    }

    // our height in the stack counting from the bottom as 0
    public int height()
    {
        return (mystack.indexOf(this));
    }

    // our depth in the stack counting from the top as 0
    public int depth()
    {
        return (mystack.size() - 1 - mystack.indexOf(this));
    }

    // we are unobstructed if there is no piece of the
    // opposite color on top of us
    public boolean unobstructed()
    {
        int sz = mystack.size();
        int ind = mystack.indexOf(this);

        for (int i = ind + 1; i < sz; i++)
        {
            piece op = mystack.elementAt(i);

            if (op.owner != owner)
            {
                return (false);
            }
        }

        return (true);
    }
  
    // we are unsandwiched if we're at the bottom, or on top of our own, 
    // or not sandwiched between two opposing pieces
    public boolean unsandwiched(boolean ontop,int forPlayer)
    {
        int ind = mystack.indexOf(this);
        if(ontop) { return(owner==forPlayer); }			// on top of our own
        if(ind==0) { return(true); }					// on bottom of anything
        return( (mystack.elementAt(ind-1).owner==forPlayer)	// or between the sheets
						|| (owner==forPlayer));
        				
    }
    // make a new stack containing a single piece, which is sucked
    // out of the old stack which contains it.  This is used when we
    // pick up a piece that has been temporarily dropped inside another
    // stack, but is picked up again
    public pstack makeNewSingleStack()
    {
        pstack newstack = new pstack(mystack.b, mystack.origin, mystack.owner);
        mystack.removeElement(this);
        addToStack(newstack);

        return (newstack);
    }

    // make a new stack and put this piece (and all the pieces on top) in it
    public pstack makeNewStack()
    {
        pstack myoldstack = mystack;
        pstack newstack = new pstack(myoldstack.b, myoldstack.origin,
                myoldstack.owner);
        int index = myoldstack.indexOf(this);
        int stacksize = myoldstack.size();

        while (index < stacksize--)
        {
            piece p = myoldstack.remove(index);
            p.addToStack(newstack);
        }

        return (newstack);
    }


    // put this piece in a stack.  each piece can be in only one stack
    public void addToStack(pstack st)
    {
        st.addElement(this);
        mystack = st;
    }

    // put this piece in a stack.  each piece can be in only one stack
    public void addToStack(pstack st, int index)
    {
        st.insertElementAt(this, index);
        mystack = st;
    }

    public boolean isMute()
    {
        return (real_top_color == Face.Blank);
    }

    public boolean isMonoColor()
    {
        return (real_top_color == real_bottom_color);
    }

    public boolean isFlipped()
    {
        return (flipped);
    }

    public boolean flip()
    {
        Face c = vis_top_color;
        vis_top_color = vis_bottom_color;
        vis_bottom_color = c;
        c = real_top_color;
        real_top_color = real_bottom_color;
        real_bottom_color = c;
        flipped = !flipped;
        flipKnowns();
        return (real_top_color != real_bottom_color);
    }

    // draw a pretty picture of the piece, and notice if the mouse is nearby
    public void DrawTop(Graphics g, int left, int top, int w, int h,
        piece prevchip, boolean ontop, HitPoint highLight, int stepsize)
    { //pixelgrabber

        int h2 = (int) (h * TOP_RATIO);
        int overlap = (h - h2) - stepsize; //overlap between pieces in a stack

        // fatten the bottom sensitivity in the downward direction
        boolean hitbot = (highLight != null) &&
            ((highLight.hitObject == null) || (highLight.hitObject == this)) &&
            G.pointInRect(highLight, left, top + h2, w, h);

        // fatten the top sensitivity in the upward direction
        hittop = ontop && !hitbot && (depth() == 0) // make sure the piece is on top
            // otherwise, floating pieces can be flipped!
             &&(highLight != null)// all this redundant checking is to make sure only one piece
            // of a stack is selected.
             &&
            ((highLight.hitObject == null) || (highLight.hitObject == this)) &&
            G.pointInRect(highLight, left, top - h2, w, h2 * 2);

        boolean legalhit = (hittop || hitbot) &&
            mystack.b.LegalToHit(mystack, this, hittop);
        if (legalhit)
        {
            highLight.hitObject = this;
            highLight.hitCode = PlateauId.HitAChip;
        }

        if (g != null)
        {
            boolean drawn = false;

            if (top_images != null)
            {
                Image im = (prevchip != null)
                    ? (stack_images[owner][(prevchip.owner == 0)
                    ? 0 : 1]) : single_images[owner][0];
                Image topIm = top_images[owner][vis_top_color.ordinal()];

                if (im != null)
                {
                   im.drawImage(g, left, top + h2, w, h - h2); // draw the bottom strip

                    if (hitbot && (highLight.hitObject == this))
                    {
                        GC.frameRect(g, highlight_color, left,
                            top + h2 + overlap, w, h - h2 - overlap);
                        GC.frameRect(g, highlight_color, left - 1,
                            top + h2 + 1 + overlap, w + 2, h - h2 - overlap);
                    }

                    // mark pieces that will be captured
                    if ((g != null) && (mystack.b!=null) && (mystack.b.isCaptured(this)))
                    {
                        GC.setColor(g,Color.red);
                        lib.GC.drawLine(g,left, top + ((2 * h) / 3), left + w, top +
                            h);
                        lib.GC.drawLine(g,left, top + h, left + w, top +
                            ((2 * h) / 3));
                    }

                    // draw the arrows pointing to the dropped piece
                    if ((g != null) && (mystack.b.isOnboarded(this)))
                    {
                        GC.setColor(g,stack_marker_color);
                        GC.drawArrow(g, left - (w / 5), top + h2, left + 2,
                            top + h2, h2 / 2,mystack.b.lineStrokeWidth);
                        GC.drawArrow(g, left + w + (w / 5), top + h2,
                            (left + w) - 2, top + h2, h2 / 2,mystack.b.lineStrokeWidth);
                    }

                    if (ontop)
                    {
                       topIm.drawImage(g, left, top, w, h2); // draw the cap

                        if (legalhit && hittop)
                        {
                            GC.frameRect(g, Color.blue, left, top, w, h2);
                            GC.frameRect(g, Color.blue, left - 1, top - 1,
                                w + 2, h2);
                        }
                    }

                    drawn = true;
                }
            }

            if (!drawn)
            {
                GC.Text(g, true, left, top, w, h, Color.black, null,
                		vis_top_color.shortName);
            }
        }
    }

    // a pretty string for debugging
    public String toString()
    {
        return ("[" + PLAYERCOLORS[owner] 
        		+ "#"+piecenumber + " " 
        		+ topColorString() + " "
        		+ bottomColorString()
        		+ "]");
     }
    long Piece_Digest() { return(randomv); }
    long Face_Digest(Random r, Face color)
    {
    	long v1 = r.nextLong();
    	long v2 = r.nextLong();
    	long v3 = r.nextLong();
    	long v4 = r.nextLong();
    	long v5 = r.nextLong();

        switch (color)
        {
        default:
        	throw G.Error("Illegal face color %s", color);

        case Unknown:
            return (v1);

        case Blank:
            return (v2);

        case Red:
            return (v3);

        case Blue:
            return (v4);

        case Orange:
            return (v5);
        }
    }
    long Digest(Random r)
    {
    	long v = 0;
        long v1 = r.nextLong();

        switch (owner)
        {
        case 0:
            break;

        case 1:
            v ^= v1;

            break;

        default:
        	throw G.Error("Illegal owner color %s", owner);
        }
        v ^= Face_Digest(r, real_top_color);
        v ^= Face_Digest(r, real_bottom_color);
        return (v);
    }

    // true if this particular piece can substitute for another
    // which is partially hidden.
	public boolean compatibleWith(piece unknownPiece,int player)
	{	boolean ktop = unknownPiece.topKnown(player);
		Face top = unknownPiece.realTopColor();
		boolean kbot =	unknownPiece.bottomKnown(player);
		Face bot = unknownPiece.realBottomColor();
		
		boolean match = (!ktop || real_top_color == top)
							&& (!kbot || real_bottom_color==bot);
		if(!match)
		{
			return (!ktop || real_bottom_color == top)
					 && (!kbot || real_top_color == bot);
		}
		return match;
	}

}