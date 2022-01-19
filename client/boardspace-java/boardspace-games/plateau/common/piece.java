package plateau.common;


import java.awt.*;


import bridge.ImageUpdateProxy;

import java.util.*;
import lib.Graphics;
import lib.Image;
import lib.G;
import lib.GC;
import lib.HitPoint;

// plateau piece
public class piece extends ImageUpdateProxy implements PlateauConstants
{
    // these image arrays are initialized by the viewer class but used here.
    public static Image[][] top_images;
    public static Image[][] single_images;
    public static Image[][] stack_images;
    int owner; // the player who own the piece
    int piecenumber; // index into the board's piece array
    long randomv ;
    int pointvalue; // point value for this piece in exchanges
    int pieceType; // the piece type, mainly used to place the piece positionally on a rack
    boolean hittop = false; // true if this piece is current hit on top by the mouse
    pstack mystack; // the stack which contains this piece.
    private int vis_top_color; // the color showing on top
    private int vis_bottom_color; // the color (not)showing on bottom
    private int real_top_color;
    private int real_bottom_color;
    private boolean flipped;
    public int placedPosition;	// initial position when placed onboard
    int realPieceType; // remembers the real piece type of anonymized pieces
    // represent imperfect knowledge of opponent colors
    int possibleid;

    // record the state of visible faces and deduced identity
    final int faceColorCode(int n)
    {	G.Assert((n>=FACE_COLOR_OFFSET)&&(n<=(FACE_COLOR_OFFSET+15)),"code ok");
    	return(n-FACE_COLOR_OFFSET);
    }
    public int getState() 
    	{ return((possibleid<<9) 
    			| ((flipped?(1<<8):0)
    			| (faceColorCode(vis_top_color)<<4)
    			| (faceColorCode(vis_bottom_color)))); 
    	}
    public void setState(int state)
    	{	possibleid = (state>>9);
    		boolean newflipped = ((state>>8)&1)==1;
    		if(newflipped!=flipped) { flip(); }
    		vis_top_color = FACE_COLOR_OFFSET+((state>>4)&0xf);
    		vis_bottom_color = FACE_COLOR_OFFSET+(state&0xf);
    		G.Assert(getState()==state,"got the desired state");
    	}
    // constructor for permanent pieces
    public piece(int own, int ind, int type,long rv)
    {
        owner = own;
        piecenumber = ind;
        realPieceType = type;
        real_top_color = topColor[type];
        real_bottom_color = bottomColor[type];
        randomv = rv;
        revealAll();
    }

    // constructor for temporary pieces
    public piece(int own, int color)
    {
        owner = own;
        real_top_color = vis_top_color = color;
        real_bottom_color = vis_bottom_color = UNKNOWN_FACE;
    }


    boolean equals(piece other)
    {
        boolean va = ((piecenumber == other.piecenumber) &&
            (flipped == other.flipped));

        if (!va)
        {
        	throw G.Error("Mismatch");
        }

        return (va);
    }

    void anonymize()
    {
        pointvalue = 0;
        pieceType = -1;
        vis_top_color = UNKNOWN_FACE;
        vis_bottom_color = UNKNOWN_FACE;
        possibleid = MAYCONTAINMUTE | MAYCONTAINBLUE | MAYCONTAINRED |
            MAYCONTAINORANGE;
    }

    public String locus()
    {
        return (mystack.locus() + " " + height());
    }

    public String topColorString()
    {
        return (ColorChars[vis_top_color - FACE_COLOR_OFFSET]);
    }

    public int visTopColor()
    {
        return (vis_top_color);
    }

    public int realTopColor()
    {
        return (real_top_color);
    }

    public int realBottomColor()
    {
        return (real_bottom_color);
    }

    public String bottomColorString()
    {
        return (ColorChars[vis_bottom_color - FACE_COLOR_OFFSET]);
    }

    public String colorString()
    { // top first

        return (ColorChars[vis_top_color - FACE_COLOR_OFFSET] +
        ColorChars[vis_bottom_color - FACE_COLOR_OFFSET]);
    }

    public String realTopColorString()
    { // top first

        return (ColorChars[real_top_color - FACE_COLOR_OFFSET]);
    }

    public String realBottomColorString()
    { // top first

        return (ColorChars[real_bottom_color - FACE_COLOR_OFFSET]);
    }

    public String realColorString()
    { // top first

        return (ColorChars[real_top_color - FACE_COLOR_OFFSET] +
        ColorChars[real_bottom_color - FACE_COLOR_OFFSET]);
    }

    void resetColor(String col)
    {
        for (int i = 0; i < ColorChars.length; i++)
        {
            if (ColorChars[i].equals(col))
            {
                vis_top_color = FACE_COLOR_OFFSET + i;
                possibleid |= ColorKnown[i];
                possibleid &= ColorUnknown[i];
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
        pieceType = realPieceType;
        pointvalue = pointValue[realPieceType];
        vis_top_color = real_top_color;
        vis_bottom_color = real_bottom_color;
        possibleid = ColorKnown[vis_top_color - FACE_COLOR_OFFSET] |
            ColorKnown[vis_bottom_color - FACE_COLOR_OFFSET];
    }

    public void revealTop()
    {
        if (vis_bottom_color == real_bottom_color)
        { // the bottom is already know, so no secrets left
            revealAll();
        }
        else
        {
            // reveal just the top
            vis_top_color = real_top_color;
            possibleid |= ColorKnown[vis_top_color - FACE_COLOR_OFFSET];
            possibleid &= ColorUnknown[vis_top_color - FACE_COLOR_OFFSET];
        }
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
            piece p = myoldstack.elementAt(index);
            p.addToStack(newstack);
        }

        return (newstack);
    }

    // remove this piece from it's stack.
    public boolean removeFromStack()
    {
        pstack ms = mystack;
        mystack = null;

        if (ms != null)
        {
            return (ms.removeElement(this));
        }

        return (false);
    }

    // put this piece in a stack.  each piece can be in only one stack
    public void addToStack(pstack st)
    {
        removeFromStack();
        st.addElement(this);
        mystack = st;
    }

    // put this piece in a stack.  each piece can be in only one stack
    public void addToStack(pstack st, int index)
    {
        removeFromStack();
        st.insertElementAt(this, index);
        mystack = st;
    }

    public boolean isMute()
    {
        return (real_top_color == BLANK_FACE);
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
        int c = vis_top_color;
        vis_top_color = vis_bottom_color;
        vis_bottom_color = c;
        c = real_top_color;
        real_top_color = real_bottom_color;
        real_bottom_color = c;

        flipped = !flipped;

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
                Image topIm = top_images[owner][vis_top_color -
                    FACE_COLOR_OFFSET];

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
                    if ((g != null) && (mystack.b.isCaptured(this)))
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
                    ColorNames[vis_top_color - FACE_COLOR_OFFSET]);
            }
        }
    }

    // a pretty string for debugging
    public String toString()
    {
        return ("[" + PLAYERCOLORS[owner] + "#"+piecenumber + " " +
        ColorNames[vis_top_color - FACE_COLOR_OFFSET] + "/" +
        ColorNames[vis_bottom_color - FACE_COLOR_OFFSET] + "]");
    }
    long Piece_Digest() { return(randomv); }
    long Face_Digest(Random r, int color)
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

        case UNKNOWN_FACE:
            return (v1);

        case BLANK_FACE:
            return (v2);

        case RED_FACE:
            return (v3);

        case BLUE_FACE:
            return (v4);

        case ORANGE_FACE:
            return (v5);
        }
    }
    long Digest(Random r)
    {
    	long v = placedPosition*123;
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
}
