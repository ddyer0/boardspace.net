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

import java.awt.Rectangle;

import lib.GameLayoutClient.BoxAlignment;
import lib.GameLayoutClient.Purpose;

/**
 * this manages the actual splitting and placement of rectangles.  The key concepts
 * are that first, player boxes are placed around the periphery of the window, leaving
 * a main rectangle, which will be the residual space used for the board, and a number
 * of "spare" rectangles, used to place auxiliary controls.
 * 
 * Placing is based on a score, which considers the size and shape of the space wasted
 * by the placement.  Generally, there are three priorities.
 * 1) placing inside a spare rectangle.
 * 2) placing by combining a horizontal or vertical "chip" from a rectangle with a spare rectangle.
 * 3) placing inside the main rectangle.  This is done only as a last resort.
 * 
 * @author Ddyer
 *
 */


public class RectangleManager
{	static boolean allowChips = true;
	static int MinCoord = 0;
	static int MaxCoord = 999999;
	public int failedPlacements = 0;
	// the use of "zoom" is not supported as coding is incomplete.
	// the idea is to do the layout exactly the same, then expand the boxes to match the zoom factor
	// but there's a log of tricky zoom/unzoom to work out.
	public double zoom = 1.0;
	public RectangleManager(double zoomto) { zoom = zoomto; }
	
	// other unused rectangles found between the player boxes.  These
	// are the exact unused area which can be completely filled.
	public RectangleStack spareRects = new RectangleStack();
	RectangleStack allocatedRects = new RectangleStack();
	public RectangleSpecStack specs = new RectangleSpecStack();
	Rectangle mainRectangle = null;
	public Rectangle centerRectangle = null;
	Rectangle allocatedMainRectangle = null;
	Rectangle fullRect = null;
	public int marginSize = 0;
	public Plog messages = new Plog(20);
	public void setMainRectangle(Rectangle r) 
	{   
	  if(r==null) 
	  	{ //when we claim the main rectangle, as reduced by other allocations
		  G.Assert(allocatedMainRectangle==null,"only do this once");
		  allocatedMainRectangle = new Rectangle(G.Left(mainRectangle)-marginSize,G.Top(mainRectangle)-marginSize,
	  			G.Width(mainRectangle)+marginSize,G.Height(mainRectangle)+marginSize);
	  	  checkRectangles();
	  	}
	  else { centerRectangle = r; allocatedMainRectangle=null; }
	  mainRectangle = r;
	}
	private void zoomRectangle(Rectangle mr)
	{
		if(zoom!=1.0)
		{	int l = G.Left(mr);
			int t = G.Top(mr);
			int w = G.Width(mr);
			int h = G.Height(mr);
			G.SetRect(mr, (int)(l*zoom+0.5), (int)(t*zoom+0.5), (int)(w*zoom+0.5), (int)(h*zoom+0.5));
	
		}
	}
	//
	// get the main rectangle.  This should always be the last allocatation.
	//
	public Rectangle allocateMainRectangle()
	{
		Rectangle mr = mainRectangle;
		allocatedRects.push(G.copy(null,mr));	// allocatedrects before trim and zoom
		setMainRectangle(null);
		centerRectangle = G.copy(null,mr);
		G.insetRect(mr, marginSize); 
		zoomRectangle(mr);
		return(mr);
	}
	public Rectangle peekMainRectangle()
	{	Rectangle mr = mainRectangle;
		if(mr!=null)
			{mr = G.copy(null,mr);
			zoomRectangle(mr);
			}
		return(mr);
	}
	public void checkRectangles(Rectangle r)
	{
    	for(int lim = spareRects.size()-1; lim>=0; lim--)
		{
		Rectangle rel = spareRects.elementAt(lim);
		if(rel!=r && G.debug() && r.intersects(rel))
		{
		failedPlacements++;
		String msg = G.format("spare rectangles overlap\n%s\n%s\n%s",rel,r,r.intersection(rel));
		messages.addLog(msg);
		if(G.debug())
			{
			G.Error(msg);
			}
		}
		}
    	if(!fullRect.contains(r) && G.Width(r)>0 && G.Height(r)>0) 
		{ 
		failedPlacements++;
		String msg = G.format("spare rectangle overlaps main\n%s\n%s",
				r,fullRect);
		messages.addLog(msg);
		if(G.debug())
			{ G.Error(msg); }
		}
	}
	public void checkRectangles()
	{
		for(int i=spareRects.size()-1; i>=0; i--)
		{	Rectangle r = spareRects.elementAt(i);
			checkRectangles(r);
		}
	}
	
	//
	// add a new spare rectangle to the pool.  Normally this is
	// a consequence of allocating something that uses only part
	// of another rectangle.
	public void addToSpare(Rectangle r)
	{	
		int hr = G.Height(r);
		int wr = G.Width(r);
		if(wr>0 && hr>0) 
			{ 
			if(G.debug())
	    	{
			checkRectangles(r);
	    	}
			int lr = G.Left(r);
			int rr = G.Right(r);
			int tr = G.Top(r);
			int br = G.Bottom(r);
			boolean done = false;
			messages.addLog("Add "+r);
			// try to coalesce the new rectangle with an existing rectangle
			for(int lim = spareRects.size()-1; lim>=0 && !done; lim--)
			{
				Rectangle rel = spareRects.elementAt(lim);
				//G.print("Check "+rel);
				if((G.Height(rel)==hr) && (G.Top(rel)==tr))
				{
				 if((G.Left(rel)==rr)||(G.Right(rel)==lr))  
				 	{ 
				 	  //G.print("combine horizontal ",rel,r);
				 	  G.union(rel,r); 
				 	 if(G.debug())
					  {
						  checkRectangles();
					  }
				 	  done=true; 
				 	}
				}
				else if((G.Width(rel)==wr)&&(G.Left(rel)==lr))
				{
				 if((G.Top(rel)==br) || (G.Bottom(rel)==tr)) 
				 {
				 	  //G.print("combine horizontal ",rel,r);
				 	  G.union(rel,r);
				 	 if(G.debug())
					  {
						  checkRectangles();
					  }
					 done = true;
				 }
				}
			}
			if(!done)
				{ spareRects.push(r);
				  // we already checked r against the rectangles
				}

			}
	}
	//
	// find a left or right adjacent rectangle which can be combined to make
	// a specified width and height.  We know that fromRect is tall enough
	// but not wide enough on it's own.
	//
    private Rectangle findLeftOrRightChip(Rectangle fromRect,int minWM,int minHM)
    {   	
    	Rectangle chipDown = null;
     	// look for combining this with an adjacent as an alternative to chipping the whole thing from here.
    	if(allowChips)
    	{
    	// chipping is a partial solution to the inefficiency when removing a big rectangle could
    	// instead be done by taking part of a surrounding rectangle and a much smaller piece of 
    	// the big rectangle.
    	int chipW = minWM-G.Width(fromRect);
    	int fromTop = G.Top(fromRect);
    	int fromBottom = G.Bottom(fromRect);
    	int fromLeft = G.Left(fromRect);
    	int fromRight = G.Right(fromRect);
    	for(int lim = spareRects.size(),idx=0; idx<=lim; idx++)
    	{	
    		Rectangle spare = (idx==lim) ? mainRectangle : spareRects.elementAt(idx);
    		int spareW = G.Width(spare);
    		if(spareW>0)
    		{
    		int spareH = G.Height(spare);
    		int spareL = G.Left(spare);
    		int spareR = spareL+spareW;
    		int spareT = G.Top(spare);
    		if((spareH>=minHM)
    				&& (spareW<minWM)		// if it would take the whole thing, it can't be a chip
    				&& (spareT>=fromTop)
    				&& ((spareT+minHM)<=fromBottom))
    			{
    			if(spareR==fromLeft)
    				{
    				if(spareW >= chipW) { chipDown = spare;chipW = spareW; }
    				}
    			else if(spareL==fromRight)
    				{
     				if(spareW>=chipW) { chipDown = spare; chipW = spareW; }
    				}
    			}
    		}
    	}}
    	return(chipDown);
    }
	//
	// find an above or below adjacent rectangle which can be combined to make
	// a specified width and height.  We know that fromRect is wide enough but
    // not tall enough on it's own.
	//
    private Rectangle findAboveOrBelowChip(Rectangle fromRect,int minWM,int minHM)
    {
    	    	
    	Rectangle chipAcross = null;
    	// look for combining this with an adjacent as an alternative to chipping the whole thing from here.
    	if(allowChips)
    	{
    	int chipH = minHM-G.Height(fromRect);
    	int fromTop = G.Top(fromRect);
    	int fromBottom = G.Bottom(fromRect);
    	int fromLeft = G.Left(fromRect);
    	int fromRight = G.Right(fromRect);
    	// chipping is a partial solution to the inefficiency when removing a big rectangle could
    	// instead be done by taking part of a surrounding rectangle and a much smaller piece of 
    	// the big rectangle.
    	for(int idx = 0,lim = spareRects.size(); idx<=lim; idx++)
    	{
    		Rectangle spare = (idx==lim) ? mainRectangle : spareRects.elementAt(idx);
     		int spareH = G.Height(spare);
    		if(spareH>0)
    		{
    		int spareW = G.Width(spare);
    		int spareL = G.Left(spare);
    		int spareT = G.Top(spare);
    		int spareB = spareT+spareH;
    		if((spareW>=minWM)
    				&& (spareH<minHM)			// if it could take the whole thing, it can't be a chip
    				&& (spareL>=fromLeft)
    				&& (spareL+minWM<=fromRight))
    		{
    			if(spareB==fromTop)
    			{	if(spareH>=chipH) { chipAcross = spare; chipH = spareH; }
    			}
    			else if(spareT==fromBottom)
    			{	if(spareH>=chipH) { chipAcross = spare; chipH = spareH; }
    			}	
    		}}
    	}}
    	return(chipAcross);
    }
    
    // these are "result" variables for placeInSpareScore
	private Rectangle placeInSpare_best = null;
	private double placeInSpare_score = 0;
	private Rectangle placeInSpare_bestAboveOrBelow = null;
	private Rectangle placeInSpare_bestLeftOrRight = null;
	private boolean placeAboveOrBelow = false;
	
	//
	// look for score a single rectangle as a place for the desired allocation
	// either alone or combined with an adjacent rectangle.
	// The variables above detail the best candidate found.
	//
	private boolean placeInSpareScore(Rectangle r,int minW0,int minH0,double preferredAspectRatio)
	{
   		int w = G.Width(r);
		int h = G.Height(r);
		int minWM0 = minW0;
		int minHM0 = minH0;
		boolean newBest = false;
		if(h>=minHM0)
		{
			Rectangle chipLeftOrRight = findLeftOrRightChip(r,minWM0,minHM0);
			if(chipLeftOrRight!=null) 
				{ double myRemainderW;
				  if(r==mainRectangle)
				  {
				  double neww = w-minWM0+G.Width(chipLeftOrRight);
				  double aspectW = neww/h;
				  double effeciencyW = Math.sqrt(Math.min(preferredAspectRatio, aspectW)/Math.max(preferredAspectRatio,aspectW));
				  myRemainderW = neww*h*effeciencyW;
				  }
				  else {
				  myRemainderW =  - (minWM0-G.Width(chipLeftOrRight))*(h-minHM0);
				  }
				  if(placeInSpare_best==null || myRemainderW>placeInSpare_score)
				  {	newBest = true;
				  	placeInSpare_best = r;
				  	placeInSpare_bestLeftOrRight = chipLeftOrRight;
				  	placeInSpare_bestAboveOrBelow = null;
				  	placeAboveOrBelow = false;
				  	placeInSpare_score = myRemainderW;
				  }
				}
 		}
		if(w>=minWM0)
		{
			Rectangle chipAboveOrBelow = findAboveOrBelowChip(r,minWM0,minHM0);
			if(chipAboveOrBelow!=null) 
				{ double myRemainderH ;
				  if(r==mainRectangle)
				  {
 				  double newh = h-minHM0+G.Height(chipAboveOrBelow);
				  double aspectH = w/newh;
				  double effeciencyH = Math.sqrt(Math.min(preferredAspectRatio, aspectH)/Math.max(preferredAspectRatio,aspectH));
				  myRemainderH = newh*w*effeciencyH;
				  }
				  else
				  {
					 myRemainderH = - (minHM0-G.Height(chipAboveOrBelow))*(w-minWM0); 
				  }

				  if(placeInSpare_best==null || myRemainderH>placeInSpare_score)
				  {
					  placeInSpare_best = r;
					  placeInSpare_bestAboveOrBelow = chipAboveOrBelow;
					  placeInSpare_bestLeftOrRight = null;
					  newBest = true;
					  placeAboveOrBelow = true;
					  placeInSpare_score = myRemainderH;    					  
				  }
				}
		}
		if((w>=minWM0) && (h>=minHM0))
		{	double myRemainder;
			boolean betterRemoveH=false;
			if(r==mainRectangle)
			  {
			  double neww = w-minWM0;
			  double aspectW = neww/h;
			  double effeciencyW = Math.sqrt(Math.min(preferredAspectRatio, aspectW)/Math.max(preferredAspectRatio,aspectW));
			  double myremainderW = neww*h*effeciencyW;

			  double newh = h-minHM0;
			  double aspectH = w/newh;
			  double effeciencyH = Math.sqrt(Math.min(preferredAspectRatio, aspectH)/Math.max(preferredAspectRatio,aspectH));
			  double myremainderH = newh*w*effeciencyH;
			  myRemainder = Math.max(myremainderH, myremainderW);
			  betterRemoveH = myremainderW<myremainderH;
			  //G.print("new best "+betterRemoveH+" "+minW0+"x"+minH0+" "+myremainderW+" "+myremainderH);

			  }
			  else {
				  int chipH = minHM0*(w-minWM0);	// waste if removing a horizontal
				  int chipW = minWM0*(h-minHM0);	// waste if removing a vertical 
				  myRemainder = - Math.min(chipW,chipH);
				  betterRemoveH = chipH<chipW;		// for spare rectangles, rate placement by the amount of waste it creates
			  }
			if((placeInSpare_best==null) || (myRemainder>placeInSpare_score))
			{
				placeInSpare_best = r;
				newBest = true;
				placeInSpare_bestAboveOrBelow = null;
				placeInSpare_bestLeftOrRight = null;;
				placeInSpare_score = myRemainder;
				placeAboveOrBelow = betterRemoveH;
			}
		} 
		return(newBest);
	}
    private int unzoom(int w)
    {
    	return (int)((w+0.49)/zoom);
    }
    /** place one of two rectangles, whichever is more efficient.  When placing in the main rectangle, 
     * the efficiency of the remaining main rectangle is the criterion.  When placing in the existing
     * smaller rectangles, the amount of waste (smaller rectangles) generated is the criterion.
     *  
     * if there are two rectangles, nominally they are being used to choose between a horizontal
     * or a vertical orientation for an item of group of items.
     * 
     * @param targetRect
     * @param minW0
     * @param minH0
     * @param maxW0
     * @param maxH0
     * @param minW1
     * @param minH1
     * @param maxW1
     * @param maxH1
     * @param align
     * @param preserveAspectRatio
     * @return
     */
    private RectangleSpec placeInSpareRectangle(Purpose purpose,Rectangle targetRect, 
    		int minWX0,int minHX0,int maxWX0,int maxHX0,
    		int minWX1,int minHX1,int maxWX1,int maxHX1,
    		BoxAlignment align,boolean preserveAspectRatio,double preferredAspectRatio)
    {	int m2 = marginSize*2;
    	int minWM0 = unzoom(minWX0+m2);
    	int minWM1 = unzoom(minWX1+m2);
    	int maxWM0 = unzoom(maxWX0+m2);
    	int maxWM1 = unzoom(maxWX1+m2);
    	
    	int minHM0 = unzoom(minHX0+m2);
    	int minHM1 = unzoom(minHX1+m2);
    	int maxHM0 = unzoom(maxHX0+m2);
    	int maxHM1 = unzoom(maxHX1+m2);
    	Rectangle alloc = null;		// allocated new rectangle
    	placeInSpare_best = null;
    	placeInSpare_score = 0;
    	placeInSpare_bestAboveOrBelow = null;
    	placeInSpare_bestLeftOrRight = null;
    	placeAboveOrBelow = false;
    	boolean bestis1 = false;
    	for(int idx=0,lim=spareRects.size(); idx<=lim; idx++)
    	{	Rectangle r = idx==lim ? mainRectangle : spareRects.elementAt(idx);
    		if((r!=mainRectangle) || (placeInSpare_best==null))
    				{
    				// chip from the main rectangle only if there is no choice.
    				if(minWX0>0 
    						&& minHX0>0
    						&& placeInSpareScore(r,minWM0,minHM0,(r==mainRectangle) ? preferredAspectRatio : (double)minWM0/minHM0)) 
    							{ bestis1 = false; 
    							}
    				if(minWX1>0
    						&& minHX1>0 
    						&& placeInSpareScore(r,minWM1,minHM1,(r==mainRectangle) ? preferredAspectRatio : (double)minWX1/minHX1)) 
    							{ bestis1 = true; 
    							}
    				}
    	}
    	int bestMinW = bestis1 ? minWM1 : minWM0;
		int bestMaxW = bestis1 ? maxWM1 : maxWM0;
		int bestMinH = bestis1 ? minHM1 : minHM0;
		int bestMaxH = bestis1 ? maxHM1 : maxHM0;
    	if(placeInSpare_best!=null)
    	{	
     		if(placeInSpare_best==mainRectangle) { bestMaxH = bestMinH; bestMaxW = bestMinW; }
    		
    		if(placeInSpare_bestAboveOrBelow!=null)
    		{
    			alloc = placeInAboveOrBelowChip(targetRect,placeInSpare_best,
    					preserveAspectRatio ? bestMinW : Math.min(G.Width(placeInSpare_bestAboveOrBelow),Math.min(G.Width(placeInSpare_best),bestMaxW)),
    					bestMinH,
    					placeInSpare_bestAboveOrBelow,align);
    		}
    		else if(placeInSpare_bestLeftOrRight!=null) 
    		{
    			alloc = placeInLeftOrRightChip(targetRect,placeInSpare_best,
    					bestMinW,
    					preserveAspectRatio 
    						? bestMinH 
    						: Math.min(G.Height(placeInSpare_bestLeftOrRight),Math.min(G.Height(placeInSpare_best), bestMaxH)),
    					placeInSpare_bestLeftOrRight,align);
    		}
    		else     			
    		{	alloc = placeInSpecificRectangle(targetRect,placeAboveOrBelow,placeInSpare_best,
    						bestMinW,bestMinH,bestMaxW,bestMaxH,
    						preserveAspectRatio,align);
    		}
    	RectangleSpec spec = new RectangleSpec(
    			purpose,targetRect,alloc,!bestis1,
    			bestis1 ? minWX1 : minWX0,
    			bestis1 ? maxWX1 : maxWX0,
    			bestis1 ? minHX1 : minHX0,
    			bestis1 ? maxHX1 : maxHX0,
    			align,preserveAspectRatio,preferredAspectRatio);
    	specs.push(spec);
		return(spec);
    	}
    	// failed to find a placement
    	failedPlacements++;
    	if(G.debug()) 
    		{ messages.addLog("failed to place ",minWX0,"x",minHX0); 
    		}
    	
    	return(null);
    }
    private Rectangle placeInSpecificRectangle(Rectangle targetRect,boolean aboveOrBelow,Rectangle fromRect,
    		int minW,int minH,int maxW,int maxH,
    		boolean preserveAspectRatio,BoxAlignment align)
    {	int actualW = Math.min(maxW,G.Width(fromRect));
    	int actualH =  Math.min(maxH,G.Height(fromRect));
    	
		if(preserveAspectRatio)
		{	double aspect = (double)minW/minH;
			int actualH0 = Math.max(minH,(int)Math.min(actualH, actualW/aspect));
			int actualW0 = Math.max(minW,(int)Math.min(actualH*aspect, actualW));
			actualW = actualW0;
			actualH = actualH0;
		}
	
		if(aboveOrBelow)
		{	
		return placeInAboveOrBelow(targetRect,fromRect,actualW,actualH,align);
		}
		else {
		return placeInLeftOrRight(targetRect,fromRect,actualW,actualH,align);   
		}
    }
    
    private RectangleSpec placeInSpareRectangle(Purpose purpose,Rectangle targetRect, int minW0,int minH0,int maxW0,int maxH0,
    		BoxAlignment align,boolean preserveAspectRatio,double preferredAspectRatio)
    {	return placeInSpareRectangle(purpose,targetRect,minW0,minH0,maxW0,maxH0,
    		0,0,0,0,
    		align,preserveAspectRatio,preferredAspectRatio);
    }
    
    // split target rect into 3 pieces, add 2 new pieces to the spare list
    private void splitVertical(Rectangle targetRect,int dtop,int actualH)
    {	Rectangle newtop = G.splitTop(targetRect,null,dtop-G.Top(targetRect));
		Rectangle newbottom = G.splitBottom(targetRect,null,G.Height(targetRect)-actualH); 
		addToSpare(newtop);
		addToSpare(newbottom);
    }
    // split target rect into 3 pieces, returning the center piece as a new rectangle
    private Rectangle splitNewVertical(Rectangle targetRect,int dtop,int actualH)
    {	Rectangle split = G.splitTop(targetRect,null,dtop-G.Top(targetRect));
    	Rectangle splittop = G.splitTop(targetRect,null,actualH);
		addToSpare(split);
		return splittop;
    }

    // split target rect into 3 pieces, add 2 new pieces to the spare list
   private void splitHorizontal(Rectangle targetRect,int dleft,int actualW)
    {	
    	Rectangle split = G.splitLeft(targetRect, null,dleft-G.Left(targetRect));
    	Rectangle split2 = G.splitRight(targetRect,null,G.Width(targetRect)-actualW);
		addToSpare(split);
		addToSpare(split2);
    }
   // split target rect into 3 pieces, returning the center piece as a new rectangle
   private Rectangle splitNewHorizontal(Rectangle targetRect,int dleft,int actualW)
    {	
	    Rectangle split = G.splitLeft(targetRect, null,dleft-G.Left(targetRect));
    	Rectangle splitleft = G.splitLeft(targetRect,null,actualW);
		addToSpare(split);
		return splitleft;
    }
    
   	//
    // place inside a single rectangle, add new waste to the spare list.
    // align is the preferred alignment relative to the main rectangle
    // return the allocated rectangle
    //
    private Rectangle placeInRectangle(Rectangle targetRect,int actualW,int actualH,Rectangle fromRect,BoxAlignment align1,BoxAlignment align2)
    {
    G.Assert(fromRect!=null, "must be a from rectangle");
	int availableW = G.Width(fromRect);
	int availableH = G.Height(fromRect);

	G.Assert(actualW<=availableW && actualH<=availableH,"size too large");
	
	switch(align1)
	{
	case Left:
		G.splitLeft(fromRect,targetRect,actualW);
		if(availableW==actualW) 
			{ spareRects.remove(fromRect); }
		break;
	case Top:
		G.splitTop(fromRect, targetRect, actualH);
		if(actualH==availableH) 
			{ spareRects.remove(fromRect); }
		break;
	case Right:
		G.splitRight(fromRect,targetRect,actualW);
		if(availableW==actualW) 
			{ spareRects.remove(fromRect); }
		break;
	case Bottom:
		G.splitBottom(fromRect, targetRect, actualH);
		if(actualH==availableH) 
			{ spareRects.remove(fromRect); }
		break;
	default:
		G.Error("Not expecting align1 %s", align1);
	}
	// targetrect is now a (possibly oversized) allocated rectangle
	return placeInRectangle(targetRect,actualW,actualH,align2);
    }
    //
    // split from a rectangle and return pieces to the spare rectangle pool
    //
    private Rectangle placeInRectangle(Rectangle targetRect,int actualW,int actualH,BoxAlignment align)
    {
		if(G.Width(targetRect)==actualW)
		{	// trim the height
			int dtop = 0;
			switch(align)
			{
			case Center:
				dtop = G.centerY(centerRectangle)-actualH/2;
				break;
			case Top:
				dtop = MinCoord;
				break;
			default:
			case Edge:
				int tp = G.centerY(centerRectangle)-G.Top(targetRect);
				int tb = G.Bottom(targetRect)-G.centerY(centerRectangle);
				dtop = tp<=tb ? MinCoord : MaxCoord;
				break;
			case Bottom: dtop = MaxCoord;
				break;
			}
			dtop = Math.min(Math.max(dtop,G.Top(targetRect)),G.Bottom(targetRect)-actualH);
			splitVertical(targetRect,dtop,actualH);
		}
		else if(G.Height(targetRect)==actualH)
		{	// tdrim the width
			int dleft = 0;
			switch(align)
			{
			case Left:
				dleft = MinCoord;
				break;
			default:
			case Edge:
				int dl = G.centerX(centerRectangle)-G.Left(targetRect);
				int dr = G.Right(targetRect)-G.centerY(centerRectangle);
				dleft = dl<=dr ? MinCoord : MaxCoord;
				break;
			case Right:
				dleft = MaxCoord;
				break;
			case Center:
				dleft = G.centerX(targetRect)-actualW/2;
				break;
			}
			dleft = Math.min(G.Right(targetRect)-actualW,Math.max(dleft, G.Left(targetRect)));
			splitHorizontal(targetRect,dleft,actualW);
		}
		else { G.Error("Either width or height should already match"); }
	Rectangle alloc = G.copy(null, targetRect);
	allocatedRects.push(alloc);	// before zoom or margin
	G.insetRect(targetRect,marginSize);
	zoomRectangle(targetRect);
	return alloc;
}

    /**
     * place a rectangle with min and max size
     * 
     * @param targetRect
     * @param minW
     * @param minH
     * @param maxW
     * @param maxH
     * @param align
     * @param preserveAspectRatio
     * @param preferredAspectRatio
     * @return
     */
    RectangleSpec placeInMainRectangle(Purpose purpose,Rectangle targetRect, int minW,int minH,int maxW,int maxH,
    		BoxAlignment align,boolean preserveAspectRatio,double preferredAspectRatio)
    {		G.SetRect(targetRect, 0, 0, 0, 0);
    		if (  (maxW>0)
    				&& (maxH>0))
    		{ return placeInSpareRectangle(purpose,targetRect,minW,minH,maxW,maxH,align,preserveAspectRatio,preferredAspectRatio);
    		
    		}
    		return null;
    }
    /**
     * place one of two rectangles, whichever is more efficient.  This is generally used to place
     * a tall/this or short/wide box intended to lie along the board edge, where either layout
     * is acceptable.
     * 
     * @param targetRect
     * @param minW
     * @param minH
     * @param maxW
     * @param maxH
     * @param minW1
     * @param minH1
     * @param maxW1
     * @param maxH1
     * @param align
     * @param preserveAspectRatio
     * @param preferredAspectRatio
     * @return
     */
    public RectangleSpec placeInMainRectangle(Purpose purpose,Rectangle targetRect,
    		int minW,int minH,int maxW,int maxH,
    		int minW1,int minH1,int maxW1,int maxH1,
    		BoxAlignment align,boolean preserveAspectRatio,double preferredAspectRatio)
    {	G.SetRect(targetRect, 0, 0, 0, 0);
        return (placeInSpareRectangle(purpose,targetRect,
    									minW,minH,maxW,maxH,
    									minW1,minH1,maxW1,maxH1,
    									align,preserveAspectRatio,preferredAspectRatio)
    			);
    }
    private Rectangle placeInLeftOrRight(Rectangle targetRect,Rectangle fromRect,int actualW,int actualH,BoxAlignment align)
    {
		BoxAlignment align1 = align;
		// align1 defines the "first cut" of a chip from a new block
		// the original align defines the final cut.
		switch(align)
		{
		case Top:
		case Bottom:
		case Left:
		case Right:
			break;
		case Center:
			align1 = (G.centerX(fromRect)<G.centerX(centerRectangle)) ? BoxAlignment.Right : BoxAlignment.Left;
			break;
		case Edge:
			align1 = (G.centerX(fromRect)<G.centerX(centerRectangle)) ? BoxAlignment.Left : BoxAlignment.Right;
			break;
		}
		return placeInRectangle(targetRect,actualW,actualH,fromRect,align1,align);

    }
    // fromRect is tall enough, but not wide enough for the placement. A chip from
    // chipLeftOrRight will supply the rest of the needed width.
    // return the allocated rectangle
    private Rectangle placeInLeftOrRightChip(Rectangle targetRect,Rectangle fromRect,int actualW,int actualH,Rectangle chipLeftOrRight,BoxAlignment align)
    {	int fromLeft = G.Left(fromRect);
    	int fromTop = G.Top(fromRect);
    	int fromBottom = G.Bottom(fromRect);
	    //int fromRight = G.Right(fromRect);
	    int chipLeft = G.Left(chipLeftOrRight);
	    //int chipRight = G.Right(chipLeftOrRight);
		if(chipLeft<fromLeft) { G.splitLeft(fromRect, targetRect, actualW-G.Width(chipLeftOrRight)); }
		else { G.splitRight(fromRect, targetRect, actualW-G.Width(chipLeftOrRight)); }
		// targetRect is now a strip from top to bottom of fromRect. 
		int dtop;
	    int chipTop = G.Top(chipLeftOrRight);
	    int chipBottom = G.Bottom(chipLeftOrRight);
	
		switch(align)
		{
		case Center:
			dtop = G.centerY(centerRectangle)-actualH/2;
			break;
		case Top:
		default:
			dtop = G.Top(centerRectangle);
			break;
		case Bottom:
			dtop = G.Bottom(centerRectangle);
			break;
		case Edge:
			dtop = (fromTop+fromBottom)>(G.Top(centerRectangle)+G.Bottom(centerRectangle)) ? MaxCoord : MinCoord;
			break;
		}
		dtop = Math.min(chipBottom-actualH, Math.min(fromBottom-actualH, Math.max(chipTop,Math.max(dtop, fromTop))));
		// reduce the target size first, to help coalesce the final new rectangle
		Rectangle split = splitNewVertical(chipLeftOrRight,dtop,actualH);
		splitVertical(targetRect,dtop,actualH);
		G.union(targetRect, split);
		Rectangle alloc = G.copy(null, targetRect);
		allocatedRects.push(alloc);	// allocated rects before zoom or margin
		G.insetRect(targetRect,marginSize);
		zoomRectangle(targetRect);
		return alloc;
    }
    // fromRect is wide enough, but not tall enough for the placement.  
    // A chip from chipAboveOrBelow will supply the rest of the needed height.
    // return the allocated rectangle
    private Rectangle placeInAboveOrBelowChip(Rectangle targetRect,Rectangle fromRect,int actualW,int actualH,Rectangle chipAboveOrBelow,BoxAlignment align)
    {	int fromTop = G.Top(fromRect);
    	int fromBottom = G.Bottom(fromRect);
    	// remove a strip at the bottom, from left to right
    	if(G.Top(chipAboveOrBelow)<fromTop) { G.splitTop(fromRect,targetRect,actualH-G.Height(chipAboveOrBelow));  }
			else { G.splitBottom(fromRect, targetRect,actualH-G.Height(chipAboveOrBelow)); }
			// targetRect is now a strip from left to right
						
			int dleft ;
			int chipLeft = G.Left(chipAboveOrBelow);
			int chipRight = G.Right(chipAboveOrBelow);
			switch(align)
			{
			case Center: 
				dleft = G.centerX(centerRectangle)-actualW/2;
				break;
			case Left:
			default:
				dleft = G.Left(centerRectangle);
				break;
			case Right:
				dleft = G.Right(centerRectangle);
				break;
			case Edge:
				dleft = ((fromTop+fromBottom)>(G.Top(centerRectangle)+G.Bottom(centerRectangle))) ? MaxCoord : MinCoord;
				break;
			}
			dleft = Math.min(Math.max(dleft, Math.max(chipLeft, G.Left(fromRect))), Math.min(chipRight-actualW,G.Right(fromRect)-actualW));
			// reduce the chip first to help coalesce the final
			Rectangle split = splitNewHorizontal(chipAboveOrBelow,dleft,actualW);
			splitHorizontal(targetRect,dleft,actualW);
			G.union(targetRect,split);
			Rectangle alloc = G.copy(null, targetRect);
			allocatedRects.push(alloc);	// allocated rects before zoom and margin
			G.insetRect(targetRect,marginSize);
			zoomRectangle(targetRect);
			return alloc;
    }
    // fromRect can contain the required placement, and the scoring
    // has determined that it's more efficient to take a horizontal
    // stripe.
    private Rectangle placeInAboveOrBelow(Rectangle targetRect,Rectangle fromRect,int actualW,int actualH,BoxAlignment align)
    {	
		BoxAlignment align1 = align;
		switch(align)
		{
		
		case Left:
		case Right:
		case Top:
		case Bottom:
			break;
		case Center:
			align1 = (G.centerY(fromRect)>G.centerY(centerRectangle)) ? BoxAlignment.Top : BoxAlignment.Bottom;
			break;
		case Edge:
			align1 = (G.centerY(fromRect)>G.centerY(centerRectangle)) ? BoxAlignment.Bottom : BoxAlignment.Top;
			break;
		}
	return placeInRectangle(targetRect,actualW,actualH,fromRect,align1,align);

    }
	public void init(int i, int j, int w, int h) {
		spareRects.clear();
		fullRect = new Rectangle(0,0,w,h);
		allocatedRects.clear();
		failedPlacements = 0;
	}

	/** new stuff to support optimization */
	//
	// left,top is the corner of a rectangle we want to expand
	// scan left for spare rectangles whose right edge is our left edge
	// recurse if we get only part of the way down
	// return the new left edge we can achieve
	private int expandLeft(int left,int top,int height)
	{	
		for(int lim = spareRects.size()-1; lim>=0; lim--)
		{
			Rectangle spare = spareRects.elementAt(lim);
			if(G.Right(spare)==left)
			{	// rectangle whose right edge is our left edge
				int bot = G.Bottom(spare);
				if((bot>top)
					&& (G.Top(spare)<=top))
				{	// rectangle contains the top corner
					int remh = height-(bot-top);
					int leftspare = G.Left(spare);
					if(remh>0) 
						{ return Math.max(leftspare,expandLeft(left,bot,remh)); 
						}
					return leftspare;
				}
					
			}
		}
		return left;
	}
	//
	// right,top is the corner of a rectangle we want to expand
	// scan right for spare rectangles whose left edge is our right edge
	// recurse if we get only part of the way down
	// return the new right edge we can achieve
	private int expandRight(int right,int top,int height)
	{	
		for(int lim = spareRects.size()-1; lim>=0; lim--)
		{	
			Rectangle spare = spareRects.elementAt(lim);
			if(G.Left(spare)==right)
			{	// rectangle whose left edge is our right edge
				int bot = G.Bottom(spare);
				if((bot>top)
					&& (G.Top(spare)<=top))
				{	// rectangle contains the top corner
					int remh = height-(bot-top);
					int rightspare = G.Right(spare);
					if(remh>0) 
						{ return Math.min(rightspare,expandRight(right,bot,remh)); 
						}
					return rightspare;
				}
			}
		}
		return right;
	}
	//
	// left,top is the corner of a rectangle we want to expand
	// scan up for spare rectangles whose bottom edge is our top edge
	// recurse if we get only part of the way up
	// return the new top edge we can achieve
	private int expandUp(int left,int top,int width)
	{	
		for(int lim = spareRects.size()-1; lim>=0; lim--)
		{	// rectangle whose bottom edge is our top edge
			Rectangle spare = spareRects.elementAt(lim);
			if(G.Bottom(spare)==top)
			{	int right = G.Right(spare);
				if((right>left)
					&& (G.Left(spare)<=left))
				{	// rectangle contains the left corner
					int remw = width-(right-left);
					int topspare = G.Top(spare);
					if(remw>0) 
						{ return Math.max(topspare,expandUp(right,top,remw)); 
						}
					return topspare;
				}
					
			}
		}
		return top;
	}
	//
	// left,bottom is the corner of a rectangle we want to expand
	// scan down for spare rectangles whose top edge is our bottom edge
	// recurse if we get only part of the way down
	// return the new bottom edge we can achieve
	private int expandDown(int left,int bottom,int width)
	{	
		for(int lim = spareRects.size()-1; lim>=0; lim--)
		{	
			Rectangle spare = spareRects.elementAt(lim);
			if(G.Top(spare)==bottom)
			{	// rectangle whose top edge is our bottom
				int right = G.Right(spare);
				if((right>left)
					&& (G.Left(spare)<=left))
				{	// rectangle contains the left corner
					int remw = width-(right-left);
					int bottomspare = G.Bottom(spare);
					if(remw>0) 
						{ return Math.min(bottomspare,expandDown(right,bottom,remw)); 
						}
					return bottomspare;
				}
					
			}
		}
		return bottom;
	}

	private Rectangle expandRectangleV(RectangleSpec spec,Rectangle alloc)
	{
		boolean expanded = false;
		int origT = G.Top(alloc);
		int origH = G.Height(alloc);
		int origW = G.Width(alloc);
		int origL = G.Left(alloc);

		// expand up and down
		if(origH<spec.maxh)
		{	int expT0 = origT;
			int expT = expT0;
			// drill left as far as we can
			while ((expT = expandUp(origL,expT0,origW))<expT0) { expT0 = expT; }
			if(expT<origT)
			{	//G.print("Expanded up ",origT-expT);
				origH += (origT-expT);
				origT = expT;
				expanded = true;				
			}
			int allocB = origT+origH;
			int expB0 = allocB;
			int expB = expB0;
			// drill right as far as we can
			while ((expB = expandDown(origL,expB0,origW))>expB0) { expB0 = expB; }
			if(expB>allocB)
			{	//G.print("Expanded down ",expB-allocB);
				origH += expB-allocB;
				expanded = true;
				
			}	
		}
		if(expanded) { return new Rectangle(origL,origT,origW,origH); }
		return null;
	}
	
	private Rectangle expandRectangleH(RectangleSpec spec,Rectangle alloc)
	{
		boolean expanded = false;
		int origT = G.Top(alloc);
		int origH = G.Height(alloc);
		int origW = G.Width(alloc);
		int origL = G.Left(alloc);
		if(origW<spec.maxw)
		{	int expL0 = origL;
			int expL = expL0;
			// drill left as far as we can
			while ((expL = expandLeft(expL0,origT,origH))<expL0) { expL0 = expL; }
			if(expL<origL)
			{	//G.print("Expanded left ",origL-expL);
				origW += (origL-expL);
				origL = expL;
				expanded = true;				
			}
			int allocR = origL+origW;
			int expR0 = allocR;
			int expR = expR0;
			// drill right as far as we can
			while ((expR = expandRight(expR0,origT,origH))>expR0) { expR0 = expR; }
			if(expR>allocR)
			{	//G.print("Expanded right ",expR-allocR);
				origW += expR-allocR;
				expanded = true;
				
			}	
		}
		if(expanded) { return new Rectangle(origL,origT,origW,origH); }
		return null;
	}
	private Rectangle larger(Rectangle r1,Rectangle r2)
	{
		return G.Width(r1)*G.Height(r1)>G.Width(r2)*G.Height(r2)
				? r1
				: r2;
	}
	private Rectangle expandRectangle(RectangleSpec spec,Rectangle alloc)
	{	if((spec.maxw>G.Width(alloc))||(spec.maxh>G.Height(alloc)))
		{
		Rectangle exH = expandRectangleH(spec,alloc);
		Rectangle exV = expandRectangleV(spec,alloc);
		if(exH!=null && exV!=null)
		{	// if both horizontal and vertical expansions are possible, try both orders
			Rectangle exHV = expandRectangleV(spec,exH);
			Rectangle exVH = expandRectangleH(spec,exV);
			if(exHV!=null && exVH!=null)
			{ 	return larger(exHV,exVH);
			}
			else if (exHV!=null) { return larger(exHV,exV); }
			else if (exVH!=null) { return larger(exVH,exH); }
			return larger(exH,exV);
		}
		else if(exH!=null) { return exH; }
		return exV;
		}
		return null;
	}
	//
	// carve this target rectangle out of the spare rectangles
	// the target was derived by expanding an allocated rectangle
	// into adjacent unallocated space/
	//
	private void carveRectangle(Rectangle target)
	{	int target_left = G.Left(target);
		int target_right = G.Right(target);
		int target_top = G.Top(target);
		int target_bottom = G.Bottom(target);
		for(int lim = spareRects.size()-1; lim>=0; lim--)
		{
			Rectangle chip = spareRects.elementAt(lim);
			if(chip.intersects(target))
			{	dissect(target_left,target_top,target_right,target_bottom,chip);
			}
		}
	}
	//
	// dissect the target rectangle out of the chip
	// 
	private void dissect(int target_left,int target_top,int target_right,int target_bottom,Rectangle chip)
	{
		int chip_left = G.Left(chip);
		int chip_right = G.Right(chip);
		int chip_top = G.Top(chip);
		int chip_bottom = G.Bottom(chip);

		if((target_left<=chip_left) && (target_right>=chip_right))
		{
			// target absorbs the entire width of the chip
			if(target_top<=chip_top)
			{	// top absorbed
				if(target_bottom>=chip_bottom) { spareRects.remove(chip); }	// all gone
				else { G.SetTop(chip,target_bottom); G.SetHeight(chip,chip_bottom-target_bottom); }
			}
			else if(target_bottom>=chip_bottom)
			{	// bottom absorbed
				G.SetHeight(chip,target_top-chip_top);
			}
			else 
			{	// through the middle
				Rectangle bottom = G.splitBottom(chip,null,chip_bottom-target_top);
				G.SetTop(bottom,target_bottom);
				G.SetHeight(bottom,chip_bottom-target_bottom);
				spareRects.push(bottom);
			}
		}
		else if((target_top<=chip_top) && (target_bottom>=chip_bottom))
		{	// target absorbs the entire height of the chip
			if((target_left<=chip_left) && (target_right>=chip_left))
			{	// left edge absorbed
				if(target_right>=chip_right) { spareRects.remove(chip); }	// completely absorbed
				else { G.SetLeft(chip,target_right); G.SetWidth(chip,chip_right-target_right); }
			}
			else if(target_right>=chip_right)
			{	// right edge absorbed
				G.SetWidth(chip,target_left-chip_left);
			}
			else 
			{		// through the middle vertically
				Rectangle left = G.splitLeft(chip,null,target_left-chip_left);
				G.SetLeft(chip,target_right);
				G.SetWidth(chip,chip_right-target_right);
				spareRects.push(left);
			}			
		}
		else if(target_left<=chip_left)
		{	// chip is partially left of the target
			Rectangle left = G.splitLeft(chip,null,target_right-chip_left);
			spareRects.push(left);
			dissect(target_left,target_top,target_right,target_bottom,left);
		}
		else if(target_right>=chip_right)
		{	// chip is partially right of the target
			Rectangle right = G.splitRight(chip,null,chip_right-target_left);
			spareRects.push(right);
			dissect(target_left,target_top,target_right,target_bottom,right);
		}
		else if(target_top<chip_bottom)
		{	// chip is partially below the target
			Rectangle bottom = G.splitBottom(chip,null,chip_bottom-target_top);
			spareRects.push(bottom);
			dissect(target_left,target_top,target_right,target_bottom,bottom);
		}
		else if(target_bottom>chip_top)
		{	// chip is partially above the target
			Rectangle top = G.splitTop(chip,null,chip_top-target_bottom);
			spareRects.push(top);
			dissect(target_left,target_top,target_right,target_bottom,top);
		}
		else { G.Error("shouldn't get here"); }
	}

	public void reallocate(RectangleSpec spec,Rectangle to)
	{	
		Rectangle alloc = placeInSpecificRectangle(spec.actual,false,to,
				spec.minw+marginSize*2,spec.minh+marginSize*2,
				spec.maxw+marginSize*2,spec.maxh+marginSize*2,
				spec.preserveAspectRatio,spec.align);
		spec.allocated = alloc;
		G.copy(spec.actual,alloc);
		zoomRectangle(spec.actual);
		G.insetRect(spec.actual,marginSize);
		//G.print("Reallocated "+spec);
		switch(spec.name)
		{
		case Draw:
			// the draw group is sized by the language-specific
			// text strings, and is never a variable size so 
			// won't be optimized
		default: G.print("can't realloc ",spec.name);
			break;
		case Other:
		case Banner:	
		case Log:
		case Done:
		case Edit:
			// simple cases where we allocated a simple box 
			// and didn't carve it up
			break;
		case Chat:
			spec.client.positionTheChat(spec.actual,null,null);
			break;
		case Vcr:
			finishVcr(spec);
			break;
		case DoneEdit:		
			split2(spec);
			break;
			
		case DoneEditRep:
			splitDoneEditRep(spec);
			break;
		
		}
	}
	void finishVcr(RectangleSpec spec)
	{	Rectangle vcr = spec.actual;
		spec.client.SetupVcrRects(G.Left(vcr),G.Top(vcr),G.Width(vcr),G.Height(vcr));
	}
	
	// split one allocated rectangle into two over-under or left-right
	// this is the continuation of done-edit 
	void split2(RectangleSpec spec)
    {
   		Rectangle done = spec.actual;
   		Rectangle edit = spec.rect2;
		int l = G.Left(done);
		int t = G.Top(done);
		int w = G.Width(done);
		int h = G.Height(done);
		if(w>h)
		{
		int buttonW = (w-marginSize)/2;
		G.SetWidth(done,buttonW); 
		if(edit!=null) { G.AlignTop(edit,l+buttonW+marginSize,done); }
		}
		else 
		{
		int buttonH = (h-marginSize)/2;
		G.SetHeight(done, buttonH);
		if(edit!=null) { G.AlignLeft(edit, t+buttonH+marginSize,done); }
		}
    }
	public void optimize() {
		if(allocatedMainRectangle!=null)
		{
		//G.print("\nMain ",G.Width(allocatedMainRectangle),"x",G.Height(allocatedMainRectangle));
		checkRectangles();
		for(int i=0;i<specs.size();i++)
		{	RectangleSpec spec = specs.elementAt(i);
			switch(spec.name)
			{
			default:
			case DoneEdit:
			case Done:
			case Edit:
			case Log:
			case Chat:
			case Vcr:
			//G.print(spec);
			Rectangle alloc = spec.allocated;
			Rectangle expanded = expandRectangle(spec,alloc);
			if(expanded!=null)
			{	
				carveRectangle(expanded);
				checkRectangles();
				allocatedRects.remove(spec.allocated);
				spareRects.push(expanded);
				spec.allocated = expanded;
				reallocate(spec,expanded);
				checkRectangles();
			}}
			
		}
		}
	}
	public void deallocate(Rectangle r) {
		spareRects.addElement(r);
		checkRectangles();
	}
	// continuation of DoneEditRep;
	public void splitDoneEditRep(RectangleSpec spec) 
	{	Rectangle r = spec.actual;
		int l = G.Left(r);
		int t = G.Top(r);
		int w = G.Width(r);
		int h = G.Height(r);
		Rectangle done = spec.rect2;
		Rectangle edit = spec.rect3;
		Rectangle rep[] = spec.rectList;
   		boolean hasButton = edit!=null || done!=null;
   		int nrep = rep.length;
   		
   		if(spec.firstAlternate)
   		{
		int buttonW = (w-marginSize)/2;
		int buttonH = hasButton ? buttonW/2 : 0;
  		if(done!=null) { G.SetRect(done,l,t,buttonW,buttonH); }
		if(edit!=null) { G.SetRect(edit,l+w-buttonW,t,buttonW,buttonH); }
		t += buttonH+marginSize/2;
		int reph = (h-buttonH-marginSize/2)/nrep;
		for(int i=0;i<nrep;i++)
			{
			G.SetRect(rep[i], l, t+i*reph,w,reph);
			}	
   		}
   		else
   		{	// horizontal format
   			int buttonH = h;
   			int buttonW = w/((nrep+1)*2);
   			if(done!=null)
   			{
   				G.SetRect(done,l,t,buttonW-marginSize,buttonH);
   				l += buttonW;
   			}
   			if(edit!=null)
   			{
   				G.SetRect(edit,l,t,buttonW-marginSize,buttonH);
   				l += buttonW;	
   			}
   			for(int i=0;i<nrep;i++)
			{
			G.SetRect(rep[i], l, t,buttonW*2-marginSize,buttonH);
			l += buttonW*2;
			}	
   			
   		}
	}

}
class RequiredRectangle
{
	Rectangle targetRect;
	int minWX0;
	int minHX0;
	int maxWX0;
	int maxHX0;
	int minWX1;
	int minHX1;
	int maxWX1;
	int maxHX1;
	BoxAlignment align;
	boolean preserveAspectRatio;
	double preferredAspectRatio;
}

/**
 * this records all the specs for a rectangle, and the items it needs
 * to complete the formatting and subdivision after the primary allocation.
 * 
 * @author ddyer
 *
 */
class RectangleSpec
{
	Rectangle actual;
	Rectangle allocated;
	int minw;
	int maxw;
	int minh;
	int maxh;
	boolean firstAlternate = false;
	BoxAlignment align;
	boolean preserveAspectRatio;
	double preferredAspectRatio;
	Purpose name=Purpose.Other;
	Rectangle rect2;
	Rectangle rect3;
	Rectangle rectList[] = null;
	GameLayoutClient client;
	
	public String toString() 
	{ 	StringBuilder b = new StringBuilder("<spec ");
		int aw = G.Width(allocated);
		int ah = G.Height(allocated);
		G.append(b,name," ",aw,"x",ah);
		if(aw<maxw || ah<maxh) 
		{
			G.append(b," max ",maxw,"x",maxh);
		}
		G.append(b,">");
		return b.toString();
	} 
	public RectangleSpec(Purpose note,Rectangle act,Rectangle alloc,boolean first,int min_width,int max_width,int min_height,int max_height,BoxAlignment alignment,boolean aspect,double ratio)
	{
		name = note;
		firstAlternate = first;
		actual = act;
		allocated = alloc;
		minw = min_width;
		maxw = max_width;
		minh = min_height;
		maxh = max_height;
		align = alignment;
		preserveAspectRatio = aspect;
		preferredAspectRatio = ratio;
	}
}

class RectangleSpecStack extends OStack<RectangleSpec>
{

	public RectangleSpec[] newComponentArray(int sz) {
		return new RectangleSpec[sz];
	}
	
}
