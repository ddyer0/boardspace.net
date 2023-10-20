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
package universe;

import java.awt.Rectangle;

import lib.Graphics;
import lib.G;
import lib.HitPoint;
import lib.exCanvas;
import online.game.chip;



//
// basic class to describe construction of polyominoes of any size
// by a drawing algorithm with some number of orthogonal steps
//
public enum OminoStep implements UniverseConstants
{
	L(-1, 0),	// step left
	R(1, 0), 	// step right
	U( 0,-1),	// step down
	D(0, 1), 	// step up
	LU(-1,-1),	// step left and up
	LD(-1, 1), 	// step left and down
	DR(1, 1),	// step down and right
	DL(-1, 1),  // step down and left
	UL(-1,-1), 	// step up and left	
	UR(1, -1), 	// step up and right
	RU(1,-1), 	// step right and up
	RD(1, 1);	// step right and down

	// constructor
	OminoStep(int x,int y)
	{	dx = x;
		dy = y;
		//Dict.put(this.toString(),this);
	}
	
	int dx = 0;
	int dy = 0;


	// get the corresponding step if the poly is rotated 90 degrees clockwise
	OminoStep getRotated()
	{	switch(this)
		{
   		case U: return(R);
		case D: return(L);
		case LU: return(UR);
		case LD: return(UL);
		case RU: return(DR);
		case RD: return(DL);
		case UR: return(RD);
		case UL: return(RU);
		case DL: return(LU);
		case DR: return(LD);
		default: throw G.Error("Not expecting direction %1",this);
		case L: return(U);
		case R: return(D);
		}
	}
	
	// get the corresponding step if the poly is flipped on the y axis
	private OminoStep getFlipped() 
	{	switch(this)
		{
		case U: return(D);
		case D: return(U);
		case LU: return(LD);
		case LD: return(LU);
		case RU: return(RD);
		case RD: return(RU);
		case UR: return(DR);
		case UL: return(DL);
		case DL: return(UL);
		case DR: return(UR);
		default: throw G.Error("Not expecting direction %1",this);
		case L:
		case R: return(this);
		}
	}
	
	// get the appropriate step if the current step were
	// rotated and flipped as specified
	public OminoStep permutedDirection(int rot,boolean flip)
	{	OminoStep dir = this;
		while(rot-- > 0) { dir = dir.getRotated(); }
		if(flip) { dir = dir.getFlipped(); }
		return(dir);
 	}

	//
	// definitions for polyominoes up to size 5
	//
	static OminoStep[] ONE = {};
	static OminoStep[] TWO = {R};
	static OminoStep[] THREE_LINE = {R,R};
	static OminoStep[] THREE_CORNER = {R, U};
	
	static OminoStep[] FOUR_LINE = {R, R, R};
	static OminoStep[] FOUR_ELL = {R,R,U};
	static OminoStep[] FOUR_TEE = {R ,R, LU};
	static OminoStep[] FOUR_ZAG = {R,U,R};
	static OminoStep[] FOUR_SQUARE = {R, U, L};
	
	static OminoStep[] FIVE_LINE = {R,R,R,R};
	static OminoStep[] FIVE_ELL = { R, R, R, U};
	static OminoStep[] FIVE_ANGLE = {R, R, U, U};
	static OminoStep[] FIVE_ZAG = {R, R, U, R};
	static OminoStep[] FIVE_ZIG = {R, U, U, R};
	static OminoStep[] FIVE_U = {R, U, U, L};
	static OminoStep[] FIVE_W = {R, U, R, U};
	static OminoStep[] FIVE_CROSS = {R ,U ,DR, LD};
	static OminoStep[] FIVE_TEE = {R ,R, LU, U};
	static OminoStep[] FIVE_ZIGZAG = {R ,U ,R ,LU};
	static OminoStep[] FIVE_POST = {R, R, R ,LU};
	static OminoStep[] FIVE_BULK = {R ,R ,U ,L};
	static OminoStep FIVE_BULK_EXTRA = D;	// alternate version for drawing
	
	static OminoStep TETROMINOES[][] = 
	{ FOUR_LINE, FOUR_ELL, FOUR_TEE, FOUR_ZAG , FOUR_SQUARE 
	};
	
	// the pieces used by "Domain" 
	static OminoStep PHLIPSET[][] =
	{	TWO,TWO,TWO,
		THREE_LINE,THREE_LINE,THREE_LINE,
		THREE_CORNER,THREE_CORNER,
		FOUR_TEE,
		FOUR_SQUARE,
		FIVE_TEE,
		FIVE_LINE,
		FIVE_CROSS
	};
	// pentominoes only
	static OminoStep PENTOMINOES[][] = 
		{ 
		FIVE_LINE, FIVE_ELL, FIVE_POST, FIVE_BULK,
		FIVE_ZAG, FIVE_ANGLE, FIVE_ZIG, FIVE_U,
		FIVE_W, FIVE_CROSS, FIVE_TEE, 
		FIVE_ZIGZAG};
	
	// the full blokus set
	static OminoStep FULLSET[][] =
	{	ONE, TWO, THREE_LINE, THREE_CORNER,
		FOUR_LINE, FOUR_ELL, FOUR_TEE, FOUR_ZAG , FOUR_SQUARE,
		FIVE_LINE, FIVE_ELL, FIVE_POST, FIVE_BULK,
		FIVE_ZAG, FIVE_ANGLE, FIVE_ZIG, FIVE_U,
		FIVE_W, FIVE_CROSS, FIVE_TEE, 
		FIVE_ZIGZAG};
	
	static OminoStep CLASSIC_PENTOMINO_SOLVER[][] = 
	{	FOUR_SQUARE,
		FIVE_LINE, FIVE_ELL, FIVE_POST, FIVE_BULK,
		FIVE_ZAG, FIVE_ANGLE, FIVE_ZIG, FIVE_U,
		FIVE_W, FIVE_CROSS, FIVE_TEE, 
		FIVE_ZIGZAG
	};
	
	static OminoStep SNAKES[][] = 
	{	TWO,TWO,TWO,TWO,TWO,TWO,TWO,TWO,
		THREE_CORNER,THREE_CORNER,THREE_CORNER,THREE_CORNER,THREE_CORNER,THREE_CORNER,THREE_CORNER,THREE_CORNER
	};
	
	// givens are all single cells, numbered 0-9
	static OminoStep GIVENS[][] = 
	{
		ONE,ONE,ONE,ONE,ONE,ONE,ONE,ONE,ONE,ONE
	};
	
    static public int FIRST_PENTAMINO = OminoStep.FULLSET.length-OminoStep.PENTOMINOES.length;

    // bounding box around a pattern drawn at x,y.  This calculation is also
    // important to the unique rotation/flip algorithm in calculateUid, so don't mess with it.
    // note that when drawing, the cx, cy are the center of a chip, so the visible footprint
    // is offset by SQUARESIZE/2 from the returned rectangle.
    static public Rectangle boundingBox(OminoStep pattern[],int SQUARESIZE,int cx,int cy)
    {	Rectangle r = new Rectangle(cx,cy,SQUARESIZE,SQUARESIZE);
		for( OminoStep step : pattern)
		{	cx += step.dx*SQUARESIZE;
			cy += step.dy*SQUARESIZE;
			G.Add(r,cx+SQUARESIZE,cy+SQUARESIZE);
			G.Add(r,cx,cy);
		}
    	return(r);
    }

    // same as bounding box, but expanded to make a square instead of a rectangle
    static public Rectangle boundingSquare(OminoStep pattern[],int SQUARESIZE,int cx,int cy)
    {	Rectangle r = boundingBox(pattern,SQUARESIZE,cx,cy);
		if(G.Width(r)>G.Height(r)) { G.SetHeight(r,G.Width(r));} else { G.SetWidth(r,G.Height(r)); }
    	return(r);
    }

    
    static public String calculateUidString(OminoStep pattern[],int values[])
    {	
       	int cx = 0;
    	int cy = 0;
    	Rectangle r = boundingBox(pattern,1,cx,cy);
    	int boxSize = G.Width(r)*G.Height(r);
    	StringBuffer buf = new StringBuffer(boxSize);
    	buf.setLength(boxSize);
    	for(int i=0;i<boxSize;i++){ buf.setCharAt(i,'0'); }

   		buf.setCharAt((((cy-G.Top(r))*G.Width(r))+(cx-G.Left(r))),(values==null) ? 'x' : (char)('0'+values[0]));
    	
     	int idx = 1;
    	for( OminoStep step : pattern)
    	{	char pval = (values==null) ? 'x' : (char)('0'+values[idx++]);
    		cx += step.dx;
    		cy += step.dy;
     		int shift = (((cy-G.Top(r))*G.Width(r))+(cx-G.Left(r)));
     		buf.setCharAt(shift,pval);
     	}     		
    	return(""+G.Width(r)+G.Height(r)+buf.toString());
    }
    
    // generate a pattern that will produce the same shape as flipped and rotated
    static public OminoStep[] permutedPattern(OminoStep pattern[],int rot, boolean flip)
    { 	OminoStep pat[] = new OminoStep[pattern.length];													// the pattern for the variation
		for(int i=0;i<pattern.length;i++) { pat[i] = permutedStep(pattern[i],rot,flip); }
		return(pat);
    }
    static public OminoStep permutedStep(OminoStep pat,int rot,boolean flip)
    {
    	return((pat==null) ? null : pat.permutedDirection(rot,flip)); 	// replace with the permuted step	
    }
    static public String toString(OminoStep pat[])
    {	String ss = "{x ";
    	for(OminoStep item : pat) { ss += " "+item.toString(); }
    	ss+="}";
    	return(ss);
    }
    
    static public void drawTween(OminoStep step,chip<?> caller,
    		Graphics gc,
            exCanvas canvas,
            int SQUARESIZE,
            double xscale,
            int cx,
            int cy)
    {	int pcx = cx;
    	int pcy = cy;
    	cx += step.dx*SQUARESIZE;
		cy += step.dy*SQUARESIZE;
		switch(step)
		{
		default: throw G.Error("Not expecting step %s", step);
		case L:
		case R:
		case DL:
		case DR:
		case UR:
		case UL:
			caller.drawChipTween(gc, canvas, SQUARESIZE, 1.0, true,(cx+pcx)/2, cy, null);
			break;
		case U:
		case D:
		case LU:
		case LD:
		case RU:
		case RD:
			caller.drawChipTween(gc, canvas, SQUARESIZE, 1.0, false,cx, (cy+pcy)/2, null);
			break;
		}

    }
    static public void drawChip(OminoStep pattern[],int sudokuValues[],OminoStep extraStep,chip<?> caller,
    		Graphics gc,
            exCanvas canvas,
            int SQUARESIZE,
            double xscale,
            int cx,
            int cy,
            java.lang.String label)
    {	boolean sudoku = (sudokuValues!=null) && (label==null);
    	int square = (UniverseChip.tweenStyle==TweenStyle.barbell)?(int)(SQUARESIZE*0.75):SQUARESIZE;
    	caller.drawChipInternal(gc,canvas,square,xscale,cx,cy,sudoku?("#"+sudokuValues[0]):label);
    	int idx = 1;
    	for( OminoStep step : pattern)
    	{	int pcx = cx;
    		int pcy = cy;
    		cx += step.dx*SQUARESIZE;
    		cy += step.dy*SQUARESIZE;
    		caller.drawChipInternal(gc,canvas,square,1.0,cx,cy,sudoku?("#"+sudokuValues[idx]):label);
    		drawTween(step,caller,gc,canvas,SQUARESIZE,xscale,pcx,pcy);
    		idx++;
    	}
    	if(extraStep!=null)
    	{
    		drawTween(extraStep,caller,gc,canvas,SQUARESIZE,xscale,cx,cy);
    	}
    }
    //
    // this finds the chip highlight for the full footprint of the chip
    // 
    static public boolean findChipHighlight(OminoStep pattern[],HitPoint highlight,
            int squareWidth,
            int squareHeight,
            int cx,
            int cy)
    {	int w2 = squareWidth/2;
    	int h2 = squareHeight/2;
    	cx -= w2;
    	cy -= h2;
    	if(G.pointInRect(highlight,cx,cy,squareWidth,squareHeight)) 
    		{ return(true); }
    	for( OminoStep step : pattern)
    	{	cx += step.dx*squareWidth;
    		cy += step.dy*squareHeight;
    		if(G.pointInRect(highlight,cx,cy,squareWidth,squareHeight)) 
    			{ return(true); }
    	}
    	return(false);
    }

}
    


