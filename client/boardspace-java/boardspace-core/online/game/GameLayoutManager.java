package online.game;

import java.awt.FontMetrics;
import java.awt.Rectangle;

import lib.G;
import lib.InternationalStrings;
import lib.RectangleStack;
import online.common.SeatingChart.DefinedSeating;
import online.game.PlayConstants.BoxAlignment;


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
class RectangleManager
{	static boolean allowChips = true;
	static int MinCoord = 0;
	static int MaxCoord = 999999;
	double zoom = 1.0;
	public RectangleManager(double zoomto) { zoom = zoomto; }
	
	// other unused rectangles found between the player boxes.  These
	// are the exact unused area which can be completely filled.
	RectangleStack spareRects = new RectangleStack();
	RectangleStack allocatedRects = new RectangleStack();
	Rectangle mainRectangle = null;
	public int marginSize = 0;
	public void setMainRectangle(Rectangle r) 
	{ mainRectangle = r;
	  if(r!=null) { checkRectangles(r); }
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
		setMainRectangle(null);
		G.insetRect(mr, marginSize); 
		allocatedRects.push(G.copy(null,mr));	// allocatedrects before zoom
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
		G.print(G.format("spare rectangles overlap\n%s\n%s\n%s",rel,r,r.intersection(rel)));
		}
		}
	}
	public void checkRectangles()
	{
		for(int i=spareRects.size()-1; i>=0; i--)
		{
			checkRectangles(spareRects.elementAt(i));
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
			//G.print("Add "+r);
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
				 	  done=true; 
				 	}
				}
				else if((G.Width(rel)==wr)&&(G.Left(rel)==lr))
				{
				 if((G.Top(rel)==br) || (G.Bottom(rel)==tr)) 
				 {
				 	  //G.print("combine horizontal ",rel,r);
				 	  G.union(rel,r);
					 done = true;
				 }
				}
			}
			if(!done) { spareRects.push(r); } 
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
    
    /** place one of two rectangles, whichever is more effecient.  When placing in the main rectangle, 
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
    boolean placeInSpareRectangle(Rectangle targetRect, 
    		int minWX0,int minHX0,int maxWX0,int maxHX0,
    		int minWX1,int minHX1,int maxWX1,int maxHX1,
    		BoxAlignment align,boolean preserveAspectRatio,double preferredAspectRatio)
    {	int m2 = marginSize*2;
    	int minWM0 = minWX0+m2;
    	int minWM1 = minWX1+m2;
    	int maxWM0 = maxWX0+m2;
    	int maxWM1 = maxWX1+m2;
    	
    	int minHM0 = minHX0+m2;
    	int minHM1 = minHX1+m2;
    	int maxHM0 = maxHX0+m2;
    	int maxHM1 = maxHX1+m2;
    	
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
    				if(minWX0>0 && minHX0>0 && placeInSpareScore(r,minWM0,minHM0,preferredAspectRatio)) { bestis1 = false; }
    				if(minWX1>0 && minHX1>0 && placeInSpareScore(r,minWM1,minHM1,preferredAspectRatio)) { bestis1 = true; }
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
    			placeInAboveOrBelowChip(targetRect,placeInSpare_best,
    					preserveAspectRatio ? bestMinW : Math.min(G.Width(placeInSpare_bestAboveOrBelow),Math.min(G.Width(placeInSpare_best),bestMaxW)),
    					bestMinH,
    					placeInSpare_bestAboveOrBelow,align);
    		}
    		else if(placeInSpare_bestLeftOrRight!=null) 
    		{
    			placeInLeftOrRightChip(targetRect,placeInSpare_best,
    					bestMinW,
    					preserveAspectRatio ? bestMinH : Math.min(G.Height(placeInSpare_bestLeftOrRight),Math.min(G.Height(placeInSpare_best), bestMaxH)),
    					placeInSpare_bestLeftOrRight,align);
    		}
    		else     			
    		{
        		int actualW = Math.min(bestMaxW,G.Width(placeInSpare_best));
    			int actualH = Math.min(bestMaxH, G.Height(placeInSpare_best));
    			double aspect = (double)bestMinW/bestMinH;
    			if(preserveAspectRatio)
    			{
    				actualH = Math.max(bestMinH,(int)Math.min(actualH, actualW/aspect));
    				actualW = Math.max(bestMinW,(int)Math.min(actualH*aspect, actualW));
    			}
    			
    			if(placeAboveOrBelow)
    			{	
    			placeInAboveOrBelow(targetRect,placeInSpare_best,actualW,actualH,align);
    			}
    			else {
    			placeInLeftOrRight(targetRect,placeInSpare_best,actualW,actualH,align);    			
    			}
    		}
		return(true);
    	}
    	// failed to find a placement
    	if(G.debug()) { G.print("failed to place ",minWX0,"x",minHX0); }
    	
    	return(false);
    }

    boolean placeInSpareRectangle(Rectangle targetRect, int minW0,int minH0,int maxW0,int maxH0,
    		BoxAlignment align,boolean preserveAspectRatio,double preferredAspectRatio)
    {	return placeInSpareRectangle(targetRect,minW0,minH0,maxW0,maxH0,
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
    //
    private void placeInRectangle(Rectangle targetRect,int actualW,int actualH,Rectangle fromRect,BoxAlignment align1,BoxAlignment align2)
    {
    G.Assert(fromRect!=null, "must be a from rectangle");
	int availableW = G.Width(fromRect);
	int availableH = G.Height(fromRect);
	int fromLeft = G.Left(fromRect);
	int fromRight = G.Right(fromRect);
	int fromTop = G.Top(fromRect);
	int fromBottom = G.Bottom(fromRect);

	G.Assert(actualW<=availableW && actualH<=availableH,"size too large");
	
	boolean exactWidth = false;
	switch(align1)
	{
	case Left:
		G.splitLeft(fromRect,targetRect,actualW);
		exactWidth = true;
		break;
	case Top:
		G.splitTop(fromRect, targetRect, actualH);
		exactWidth = false;
		break;
	case Right:
		G.splitRight(fromRect,targetRect,actualW);
		exactWidth = true;
		break;
	case Bottom:
		G.splitBottom(fromRect, targetRect, actualH);
		exactWidth = false;
		break;
	default:
		G.Error("Not expecting align1 %s", align1);
	}
	if(exactWidth)
	{	// trim the height
		int dtop = 0;
		switch(align2)
		{
		case Center:
			dtop = G.centerY(mainRectangle)-actualH/2;
			break;
		case Top:
			dtop = MinCoord;
			break;
		default:
		case Edge:
			dtop = (G.centerY(fromRect)<G.centerY(mainRectangle)) ? MinCoord : MaxCoord;
			break;
		case Bottom: dtop = MaxCoord;
			break;
		}
		dtop = Math.min(Math.max(dtop,fromTop),fromBottom-actualH);
		splitVertical(targetRect,dtop,actualH);

	}
	else
	{	// tdrim the width
		int dleft = 0;
		switch(align2)
		{
		case Left:
			dleft = MinCoord;
			break;
		default:
		case Edge:
			dleft = G.centerX(fromRect)<G.centerX(mainRectangle) ? MinCoord : MaxCoord;
			break;
		case Right:
			dleft = MaxCoord;
			break;
		case Center:
			dleft = G.centerX(mainRectangle)-actualW/2;
			break;
		}
		dleft = Math.min(fromRight-actualW,Math.max(dleft, fromLeft));
		splitHorizontal(targetRect,dleft,actualW);
	}
	allocatedRects.push(G.copy(null, targetRect));	// before zoom or margin
	G.insetRect(targetRect,marginSize);
	zoomRectangle(targetRect);
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
    public boolean placeInMainRectangle(Rectangle targetRect, int minW,int minH,int maxW,int maxH,
    		BoxAlignment align,boolean preserveAspectRatio,double preferredAspectRatio)
    {		G.SetRect(targetRect, 0, 0, 0, 0);
    		return (  (maxW>0)
    				&& (maxH>0)
    				&& (placeInSpareRectangle(targetRect,minW,minH,maxW,maxH,align,preserveAspectRatio,preferredAspectRatio)));
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
    public boolean placeInMainRectangle(Rectangle targetRect,
    		int minW,int minH,int maxW,int maxH,
    		int minW1,int minH1,int maxW1,int maxH1,
    		BoxAlignment align,boolean preserveAspectRatio,double preferredAspectRatio)
    {	G.SetRect(targetRect, 0, 0, 0, 0);
        return (placeInSpareRectangle(targetRect,
    									minW,minH,maxW,maxH,
    									minW1,minH1,maxW1,maxH1,
    									align,preserveAspectRatio,preferredAspectRatio)
    			);
    }
    private void placeInLeftOrRight(Rectangle targetRect,Rectangle fromRect,int actualW,int actualH,BoxAlignment align)
    {
		BoxAlignment align1 = align;
		int fromLeft = G.Left(fromRect);
		// align1 defines the "first cut" of a chip from a new block
		// the original align defines the final cut.
		switch(align)
		{
		case Left:
		case Right:
			break;
		case Center:
			align1 = (fromLeft>G.centerX(mainRectangle)) ? BoxAlignment.Left : BoxAlignment.Right;
			break;
		case Edge:
		case Top:
		case Bottom:
			align1 = (fromLeft<G.centerX(mainRectangle)) ? BoxAlignment.Left : BoxAlignment.Right;
			break;
		}
		placeInRectangle(targetRect,actualW,actualH,fromRect,align1,align);

    }
    // fromRect is tall enough, but not wide enough for the placement. A chip from
    // chipLeftOrRight will supply the rest of the needed width.
    private void placeInLeftOrRightChip(Rectangle targetRect,Rectangle fromRect,int actualW,int actualH,Rectangle chipLeftOrRight,BoxAlignment align)
    {	int fromLeft = G.Left(fromRect);
    	int fromTop = G.Top(fromRect);
    	int fromBottom = G.Bottom(fromRect);
	    
		if(G.Left(chipLeftOrRight)<fromLeft) { G.splitLeft(fromRect, targetRect, actualW-G.Width(chipLeftOrRight)); }
		else { G.splitRight(fromRect, targetRect, actualW-G.Width(chipLeftOrRight)); }
		// targetRect is now a strip from top to bottom of fromRect. 
		int dtop;
	    int chipTop = G.Top(chipLeftOrRight);
	    int chipBottom = G.Bottom(chipLeftOrRight);
	
		switch(align)
		{
		case Center:
			dtop = G.centerX(mainRectangle)/2-actualH/2;
			break;
		case Top:
		default:
			dtop = G.Top(mainRectangle);
			break;
		case Bottom:
			dtop = G.Bottom(mainRectangle);
			break;
		case Edge:
			dtop = (fromTop+fromBottom)>(G.Top(mainRectangle)+G.Bottom(mainRectangle)) ? MaxCoord : MinCoord;
			break;
		}
		dtop = Math.min(chipBottom-actualH, Math.min(fromBottom-actualH, Math.max(chipTop,Math.max(dtop, fromTop))));
		// reduce the target size first, to help coalesce the final new rectangle
		Rectangle split = splitNewVertical(chipLeftOrRight,dtop,actualH);
		splitVertical(targetRect,dtop,actualH);
		G.union(targetRect, split);
		allocatedRects.push(G.copy(null, targetRect));	// allocated rects before zoom or margin
		G.insetRect(targetRect,marginSize);
		zoomRectangle(targetRect);
    }
    // fromRect is wide enough, but not tall enough for the placement.  
    // A chip from chipAboveOrBelow will supply the rest of the needed height.
    private void placeInAboveOrBelowChip(Rectangle targetRect,Rectangle fromRect,int actualW,int actualH,Rectangle chipAboveOrBelow,BoxAlignment align)
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
				dleft = G.centerX(mainRectangle)-actualW/2;
				break;
			case Left:
			default:
				dleft = G.Left(mainRectangle);
				break;
			case Right:
				dleft = G.Right(mainRectangle);
				break;
			case Edge:
				dleft = ((fromTop+fromBottom)>(G.Top(mainRectangle)+G.Bottom(mainRectangle))) ? MaxCoord : MinCoord;
				break;
			}
			dleft = Math.min(Math.max(dleft, Math.max(chipLeft, G.Left(fromRect))), Math.min(chipRight-actualW,G.Right(fromRect)-actualW));
			// reduce the chip first to help coalesce the final
			Rectangle split = splitNewHorizontal(chipAboveOrBelow,dleft,actualW);
			splitHorizontal(targetRect,dleft,actualW);
			G.union(targetRect,split);
			allocatedRects.push(G.copy(null, targetRect));	// allocated rects before zoom and margin
			G.insetRect(targetRect,marginSize);
			zoomRectangle(targetRect);
    }
    // fromRect can contain the required placement, and the scoring
    // has determined that it's more efficient to take a horizontal
    // stripe.
    private void placeInAboveOrBelow(Rectangle targetRect,Rectangle fromRect,int actualW,int actualH,BoxAlignment align)
    {	int fromTop = G.Top(fromRect);
		BoxAlignment align1 = align;
		switch(align)
		{
		
		case Top:
		case Bottom:
			break;
		case Center:
			align1 = (fromTop<G.centerY(mainRectangle)) ? BoxAlignment.Bottom : BoxAlignment.Top;
			break;
		case Edge:
		case Left:
		case Right:
			align1 = (fromTop>G.centerY(mainRectangle)) ? BoxAlignment.Bottom : BoxAlignment.Top;
			break;
		}
	placeInRectangle(targetRect,actualW,actualH,fromRect,align1,align);

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
 * this is the expert for constructing board layouts.  In this version of the algorithm,
 * the player boxes are placed first, around the periphery of the screen, so as to leave
 * a large unused area for the board.  The contstraints try to provide both a reasonable
 * size for the player box, and as large as practical an area for the board and other
 * controls.   Second, all the auxiliary controls are placed, and finally the main
 * rectangle for the board, all that's left over, is used.
 * 
 *  All the placements have a margin to separate them from other controls.
 *  
 *  Placement of the player boxes is driven by "seating charts" for players around
 *  a playtable, or alternatively generic seating intended for online play which 
 *  accommodate any number of players.
 * 
 * @author Ddyer
 *
 */
public class GameLayoutManager  implements Opcodes
{	
	int nPlayers;
	// if true, consider the space left by the difference between the big rectangle
	// and the board aspect ratio to be all waste.  This should be false for 
	// boardless games like hive.
	public boolean strictBoardsize = true;
	// normally we don't skip placing done in planned seating mode, because each player
	// will have a private done button.  Sometimes the "done" button is used for other 
	// purposes and we want to place it anyway.
	public boolean alwaysPlaceDone = false;		

	public String toString() { return("<GamelayoutManager "+selectedSeating+">"); }

	// left top right and bottom include necessary margins, so new content
	// can exactly reach the given values
	private int left;				// these define the central rectangle that is still unallocated
	private int top;					// these reflect the exact unused area, inside the margins
	private int right;
	private int bottom;
	
	
	private int ycenter;
	private int xcenter;
	
	private int playerWX;	// exact size
	private int playerWM;	// includes margin 
	private int playerHX;	// exact size
	private int playerHM;	// includes margin
	
	// placement for player boxes
	private int xleft;	
	private int ytop;
	private int xright;
	private int ybot;
	private int margin;
	
	// define some common waypoints
	private int xmid;			// not the true mid line, but offset by half a box
	private int ymid;
	private int ymidUp;			// center when there's a player below
	private int xsideLeft;		// so when rotated will center on left
	private int xsideRight;	// so when rotated will center on right
		
	private int xthirdLeft;			// in the tightest fit, space will be zero
	private int xthirdRight;
	
	int positions[][] = null;	// the x,y coordinates of the player boxes
	double rotations[] = null;	// the rotations of the player boxes
	
	// add spare rectangles for skinny margins at left or left and right, from top to bottom
	private void addSkinnyLeft(boolean addSkinnyRight,boolean fromtop,boolean tobottom)
	{	
		int spareY = ycenter-playerWM/2;
		addSkinnyLeftFrom(spareY,addSkinnyRight,fromtop,tobottom);
	}
	

	public void addSkinnyLeftFrom(int spareY,boolean addSkinnyRight,boolean fromtop,boolean tobottom)
	{
		int spareYT = fromtop ? top : top+playerHM;
		int spareYH = spareY-spareYT;
		if(spareYH>0)	// might have been eaten by overlap
		{
		addToSpare(new Rectangle(left,spareYT,playerHM,spareYH));
		int spareYB = spareY+playerWM;	// bottom of the sideways rectangle
		int spareYBot = tobottom ? bottom : ybot-margin;
		int spareYBH = spareYBot-spareYB;
		addToSpare(new Rectangle(left,spareYB,playerHM,spareYBH));
		if(addSkinnyRight)
			{	int spareX = right-playerHM;
				addToSpare(new Rectangle(spareX,spareYT,playerHM,spareYH));
				addToSpare(new Rectangle(spareX,spareYB,playerHM,spareYBH));
			}
		}
	}
	
	// add spare rectangles for skinny margins at left or left and right
	private void addSkinnyRight(boolean addSkinnyRight,boolean tall)
	{
		int spareY = ycenter-playerWM/2;
		int spareYT = tall ? top : top+playerHM;
		int spareYH = spareY-spareYT;
		int spareYB = spareY+playerWM;
		int spareYBH = bottom-spareYB;
		int spareX = right-playerHM;
		addToSpare(new Rectangle(spareX,spareYT,playerHM,spareYH));
		addToSpare(new Rectangle(spareX,spareYB,playerHM,spareYBH));
	}
	// add spare rectangles for skinny margins at left or left and right
	private void addSkinnyOffset(int spareX)
	{
		int spareY = ycenter-playerWM/2-playerHM/2;
		int spareYH = spareY-top;
		int spareYB = spareY+playerWM;
		int spareYBH = ybot-spareYB;
		addToSpare(new Rectangle(spareX,top,playerHM,spareYH));
		addToSpare(new Rectangle(spareX,spareYB,playerHM,spareYBH));
	}
	// add spare rectangles for fat margins at left or left and right, with the side box centered
	// this is used only by FiveAroundEdge and SixAroundEdge
	private void addFatLeftCentered(boolean right,boolean six)
	{	int spareY0 = ytop+playerHX+1;
		int spareY = top+ycenter-playerWM/2;
		int spareY2 = spareY+playerWM;
		addToSpare(new Rectangle(left,spareY0,playerWM,spareY-spareY0));
		addToSpare(new Rectangle(left,spareY2,playerWM,ybot-spareY2));
		int spareX = playerHM;
		addToSpare(new Rectangle(spareX,spareY,playerWM-playerHM,spareY2-spareY));
		// rectangles ok 2/26
		if(right)
		{
		if(six)
		{	// the right-top rectangle starts at the true top.
			addToSpare(new Rectangle(xright,spareY0,playerWM,spareY-spareY0));
		}
		else {
			addToSpare(new Rectangle(xright,ytop,playerWM,spareY-ytop));
		}
			addToSpare(new Rectangle(xright,spareY2,playerWM,ybot-spareY2));
			addToSpare(new Rectangle(xright,spareY,playerWM-playerHM,spareY2-spareY));
		}
	}
	
	// add spare rectangles for fat margins at left or left and right, with the side box top aligned
	private void addFatLeftTop(boolean right)
	{
		
		int spareY = top+playerWM;
		int spareX = left+playerHM;
		int spareXW = playerWM-spareX;
		
		addToSpare(new Rectangle(spareX,top,spareXW,spareY-top));
		addToSpare(new Rectangle(left,spareY,playerWM,ybot-spareY));
		if(right)
		{	// ok 2/26
			spareY = top+playerHM;
			int spareY2 = bottom - playerWM;
			addToSpare(new Rectangle(xright,spareY,playerWM,spareY2-spareY));
			addToSpare(new Rectangle(xright,spareY2,spareXW,bottom-spareY2));
		}	
	}
	// add spare rectangles for .X.X. spacing 
	private void add2XAcross(int ycoord)
	{	
		addToSpare(new Rectangle(left,ycoord,xthirdLeft-left,playerHM));
		int spareX = xthirdRight+playerWX;
		addToSpare(new Rectangle(spareX,ycoord,right-spareX,playerHM));
		spareX = xthirdLeft+playerWX;
		addToSpare(new Rectangle(spareX,ycoord,xthirdRight-spareX,playerHM));
	}
	
	// add spare rectangles for ..X.. spacing
	private void add1XAcross(int ycoord)
	{
		int spareX = xmid+playerWX;
		addToSpare(new Rectangle(spareX,ycoord,right-spareX,playerHM));
		addToSpare(new Rectangle(left,ycoord,xmid-left,playerHM));
	}


	// add spare rectangles for X.X.X spacing
	private void add3XAcross(int ycoord)
	{
		int spareX = left+playerWM;
		addToSpare(new Rectangle(spareX,ycoord,xmid-spareX-1,playerHM));
		spareX = xmid+playerWX;
		addToSpare(new Rectangle(spareX,ycoord,xright-spareX-1,playerHM));
	}
	private void addSidebar(int xcoord)
	{	int spareY = ycenter-playerWX/2;
		addToSpare(new Rectangle(xcoord,top,playerHM,spareY-top));
		int spareYB = ycenter+playerWM/2;
		addToSpare(new Rectangle(xcoord,spareYB,playerHM,bottom-spareYB));
	}
	private void addTopToBottom(int xcoord,boolean topTop)
	{
		int spareY = topTop?0:ytop+playerHX;
		// extra space between the top and bottom on the right side
		addToSpare(new Rectangle(xcoord,spareY,playerWM,ybot-spareY));
	}
	private void addSideToSide(int top)
	{
		addToSpare(new Rectangle(left+playerWM,top,right-left-playerWM*2,playerHM));
	}
	
	// add as horizontal segments for corner-edge arrangements
	private void addSpareVStrip(int spareX,int spareX2)
	{	int stripH = (bottom-playerWM-playerHM)/2;
		addToSpare(new Rectangle(spareX,0,playerWM,stripH));
		addToSpare(new Rectangle(spareX,bottom-playerHM-stripH,playerWM,stripH));
		if(playerWM>playerHM) { addToSpare(new Rectangle(spareX2,stripH,playerWM-playerHM,playerWM)); }
	}
	
	// add as horizontal segments for corner-edge arrangements
	private void addSpareVStripFrom(int stripY,int spareX,int spareX2)
	{	
		addToSpare(new Rectangle(spareX,0,playerWM,stripY));
		int strip2H = (bottom-playerHM)-(stripY+playerWM);
		if(strip2H>0)
		{
		addToSpare(new Rectangle(spareX,stripY+playerWM,playerWM,strip2H));
		}
		if(playerWM>playerHM) { addToSpare(new Rectangle(spareX2,stripY,playerWM-playerHM,playerWM)); }
	}
	static double noRotation[] = {0,0,0,0,0,0};
	static double fourAcrossRotation[] = {0,0,Math.PI,Math.PI,Math.PI};	//extra pi for fivearound
	static double fourAroundRotation[] = {0,Math.PI/2,Math.PI,-Math.PI/2};
	static double fiveAroundRotation[] = { 0,0,Math.PI/2,Math.PI,-Math.PI/2};
	static double sixAroundRotation [] = { 0,0,Math.PI/2,Math.PI,Math.PI,-Math.PI/2};
	public double preferredAspectRatio = 1.0;
	public RectangleManager rects = new RectangleManager(1.0);
	
	/**
	 * assign coordinates based on a seating chart, number of players,
	 * cell size, and board width and height.
	 * 
	 * this leaves positions[][] and rotations[] 
	 * @param seating
	 * @param nP
	 * @param l
	 * @param t
	 * @param w
	 * @param h
	 * @param player
	 */
	public void makeLayout(DefinedSeating seating,int nP,
			int l,int t,int w,int h,Rectangle player,int marginSize)
	{
	//G.print("Make ",seating," ",l," ",t," ",w,"x",h);
	rects.spareRects.clear();
	rects.allocatedRects.clear();
	nPlayers = nP;		
	left = l;
	right = l+w;
	top = t;
	bottom = t+h;
	margin = marginSize;
	positions = null;
	rotations = noRotation;
	playerWX = G.Width(player);	// exact size
	playerWM = playerWX + marginSize;	// includes margin 
	playerHX = G.Height(player);// exact size
	playerHM = playerHX+marginSize;		// includes margin
	
	// placement locations for player boxes
	xleft = left+marginSize;	
	ytop = top+marginSize;
	xright = right-playerWM;
	ybot = bottom-playerHM;

	// define some common waypoints
	xcenter = (left+right)/2;
	ycenter = (top+bottom)/2;
	xmid = xcenter-playerWX/2;			// not the true mid line, but offset by half a box
	ymid = ycenter-playerHX/2;
	ymidUp = ycenter-playerHX;
	xsideLeft = left+playerHM/2-playerWX/2;		// so when rotated will center on left
	xsideRight = right-playerHM/2-playerWX/2;	// so when rotated will center on right
	
	// points for the seated layouts with two per side
	int extra = right-left-playerWM*2;		// extra space with 2 boxes across
	int space = extra/4;					// allocate 1/4 left, 1/2 between the boxes, 1/4 right
	
	xthirdLeft = left + space;			// in the tightest fit, space will be zero
	xthirdRight = right -space - playerWX;
	//G.print("Seating "+seating);
	switch(seating)
	{
	case Undefined:
		throw G.Error("seating chart %s not expected",seating);
	case ThreeAroundLeft: // ok 2/4/2020
	{
	/* top and bottom are flush to the left, leaving a bigger right hand rectangle
	   this is currently used by triad
	
   		 __.........
		 ...........
		 |..........
		 ...........
		 __.........
		 
	 */
		rotations = fourAroundRotation;
	positions = new int[][] { {xleft,ybot}, { xsideLeft,ymid}, { xleft,ytop}};
	// there's a skinny rectangle left between the side rectangle and the main board,
	// ok 2/26
	addFatLeftCentered(false,false);
	left += playerWM;
	}
	break;
	case FaceToFaceLandscapeTop:
		rotations = new double[]{Math.PI,0};
		positions = new int[][]{{xright,ytop},{xright,ybot}};
		addToSpare(new Rectangle(left,top,xright-left,playerHM));
		addToSpare(new Rectangle(left,ybot,xright-left,playerHM));
		top += playerHM;
		bottom -= playerHM;
		break;
	case FaceToFaceLandscapeSide: // ok 2/4/2020
		{
		/* player box top and bottom, trimming from right
		  
		 	......._
		 	........
		 	......._
		 */
		rotations = new double[] {Math.PI,0};
		positions = new int[][]{{xright,ytop},{xright,ybot}};
		// ok 2/26
		addTopToBottom(xright,false);
		right -= playerWM;
		}
		break;
	case FaceToFacePortrait: // ok 2/4/2020
		{
		// player box above and below the board, trimming from top and bottom
		if(w<h)
		{
		rotations = new double[]{Math.PI,0};
		positions = new int[][]{{xright,ytop},{xright,ybot}};
		addToSpare(new Rectangle(left,top,xright-left,playerHM));
		addToSpare(new Rectangle(left,ybot,xright-left,playerHM));
		top += playerHM;
		bottom -= playerHM;
		}
		else
		{	// player box left and right, rotated sideways
			rotations = new double[]{Math.PI/2,-Math.PI/2};
			positions = new int[][]{{xsideLeft,ymid},	// left 
				{xsideRight,ymid},		// right
				};
			// ok 8/2/2021
			addSkinnyLeft(true,true,true);
			left += playerHM;
			right -= playerHM;
		}
			
		}
		break;
	case LeftCornerWide: // ok 2/4/2020
		// left corner, with the chip from left and right, leavinbg
		// the full height available in the center
		rotations = new double[]{ 0,Math.PI/2};
		positions = new int[][] { {xright,ybot},		// bottom ..X..
								  {xsideLeft,ymid}};
		// ok 8/2/2021
		addSkinnyLeft(false,true,true);
		addTopToBottom(xright,true);
		left += playerHM;
		right -= playerWM;
		break;
		
	case RightCornerWide: // ok 2/4/2020
		// right corner with the chop from left and right, leaving 
		// the full height available in the center
		rotations = new double[]{ 0,-Math.PI/2};
		positions = new int[][] { {xleft,ybot},		// bottom ..X..
								  {xsideRight,ymid}};
		// ok 8/2/2021
		addSkinnyRight(false,true);
		addTopToBottom(left,true);
		right -= playerHM;
		left += playerWM;
		break;		
		
	case LeftCorner:
		// left corner, with the chip at left and bottom, leaving
		// the maximum possible width
		rotations = new double[]{ 0,Math.PI/2};
		positions = new int[][] { {xmid,ybot},		// bottom ..X..
									{xsideLeft,ymid-playerHM/2}};
		addSkinnyOffset(left);
		add1XAcross(ybot);
		bottom -= playerHM;
		left += playerHM;
		break;
	case RightCorner:
		// right corner, with the chip at right and bottom, leaving
		// the maximum possible width
		rotations = new double[]{ 0,-Math.PI/2};
		positions = new int[][] {{xmid,ybot},		// bottom ..X..
									{xsideRight,ymid-playerHM/2}};
		addSkinnyOffset(right-playerHM);
		add1XAcross(ybot);
		bottom -= playerHM;
		right -= playerHM;
		break;
		
	case ThreeLeftL:
		rotations = new double[]{ 0,0,Math.PI/2};
		positions = new int[][] { {xthirdRight,ybot},{xthirdLeft,ybot},		// bottom .X.X.
									{xsideLeft,ymid-playerHM/2}};
		addSkinnyOffset(left);
		add2XAcross(ybot);
		bottom -= playerHM;
		left += playerHM;
		break;
	case ThreeRightL:
		rotations = new double[]{ 0,0,-Math.PI/2};
		positions = new int[][] {{xthirdRight,ybot},{xthirdLeft,ybot},		// bottom .X.X.
									{xsideRight,ymid-playerHM/2}};
		addSkinnyOffset(right-playerHM);
		add2XAcross(ybot);
		bottom -= playerHM;
		right -= playerHM;
		break;

	case ThreeLeftLW:
		// .........
		// |........
		// __.....__
		rotations = new double[]{ 0,0,Math.PI/2};
		positions = new int[][] { {xright,ybot}, {xleft,ybot},		// bottom X...X
								  {xsideLeft,ymid-playerHM/2}};
		// ok 8/3/2021
		addSpareVStrip(left,left+playerHM);
		addTopToBottom(xright,true);
		{int ww = Math.max(playerWM,playerHM);
		left += ww;
		right -= ww;
		}
		break;
		
	case ThreeRightLW: // ok 2/4/2020
		// .........
		// ........|
		// __.....__

		rotations = new double[]{ 0,0,-Math.PI/2};
		positions = new int[][] {{xright,ybot}, {xleft,ybot},		// bottom X...X
									{xsideRight,ymid-playerHM/2}};
		// ok 8/3/2021
		addSpareVStrip(xright,xright);
		addTopToBottom(left,true);
		{int ww = Math.max(playerWM,playerHM);
		left += ww;
		right -= ww;
		}
		break;
		
	// player boxes 3 across the bottom of the board, with "spare" rect at the bottom-right
    // this might be used for 3, 5 or 6 players.  
	case Portrait3X: // ok 2/4/2020
		{
		int rows = (nPlayers+2)/3 - 1;
		int start = ybot - playerHM*rows;
		int x2 = xleft+playerWM;
		int x3 = xleft+playerWM*2;
		// spare rects ok 2/26
		positions = new int[][]{{ xleft,start}, {x2,start},{x3,start},
					 { xleft,start+playerHM}, {x2,start+playerHM},{x3,start+playerHM}};
		int spareX = left+playerWM*3;
		int spareH = playerHM*(rows+1);
		bottom -= spareH;
		// rectangles ok 2/26
		addToSpare(new Rectangle(spareX,bottom,right-spareX,spareH));
		int rem = nPlayers%3;
		if(rem!=0)
			{
			addToSpare(new Rectangle(left+rem*playerWM,bottom+playerHX,playerWM*(3-rem),playerHM+marginSize));
			}
		}
	break;
	// player boxes 2 across the bottom of the board, with "spare" rect at the bottom-right
    // this might be used for 2, 3 or 4 players.  
	case Portrait2X: // ok 2/4/2020
		{
		int rows = (nPlayers+1)/2 - 1;
		int start = ybot - playerHM*rows;
		int x2 = xleft+playerWM;
		// spare rects ok 2/26
		positions = new int[][]{{ xleft,start}, {x2,start},
					 { xleft,start+playerHM}, {x2,start+playerHM},
					 { xleft,start+2*playerHM}, {x2,start+playerHM*2}};
		int spareX = left+playerWM*2;
		// rectangles ok 2/26
		int spareH = playerHM*(rows+1);
		bottom -= spareH;
		int spareW = right-spareX;
		if(spareW>10) { addToSpare(new Rectangle(spareX,bottom,spareW,spareH)); }
		if((nPlayers&1)!=0)
			{
			addToSpare(new Rectangle(left+playerWM,bottom+(nPlayers>4?playerHM:0)+playerHX,playerWM,playerHM+marginSize));
			}
		}
		break;
	// players three across at the right, with spare as the remaining vertical space. 
	case ThreeAcross:
		/*
	 	...._....
	 	.........
	 	.._..._..
	  
		 */
		{
		rotations = new double[]{0,0,Math.PI};
		positions = new int[][] { {xthirdRight,ybot}, {xthirdLeft,ybot}, 	
								  {xmid,ytop}};								
		// ok 2/26
		add1XAcross(top);
		add2XAcross(ybot);
		
		top += playerHM;
		bottom -= playerHM;
		}
		break;

	case ThreeAcrossLeft: // ok 2/4/2020
		{
			/*
			 position the players at the left and right, and spare at the left and bottom
			 leaving the upper-right corner as the main rectangle
			  
				 _......_
				 ........
				 _.......
			*/
		rotations = new double[]{0,0,Math.PI};
		positions = new int[][] { {xright,ybot},{xleft,ybot},	
							      {xleft,ytop}};				

		addTopToBottom(left,false);
		addSideToSide(ybot);
		bottom -=playerHM;
		left += playerWM;
		}
		break;
	case ThreeAcrossLeftCenter: // ok 2/4/2020
		{
		/* same physical layout as ThreeAcrossLeft, but attribute
		   the spare space to the left and right
		   leaving the center column as the main rectangle
			 _......_
			 ........
			 _.......
		*/
		rotations = new double[]{0,0,Math.PI};
		positions = new int[][] { {xright,ybot},{xleft,ybot},	
							      {xleft,ytop}};				
	
		addTopToBottom(left,false);
		addTopToBottom(xright,true);
		right -= playerWM;
		left += playerWM;
		}
		break;
	case FourAcross:	// ok 2/4/2020
		{
    		/*
		 	.._..._..
		 	.........
		 	.._..._..
		  
    		 */
		rotations = fourAcrossRotation;
		positions = new int[][] { {xthirdRight,ybot}, {xthirdLeft,ybot}, 	// top 		.X.X.
								{xthirdLeft,ytop}, { xthirdRight,ytop}};	// bottom 	.X.X.
		add2XAcross(ybot);
		add2XAcross(top);

		top += playerHM;
		bottom -= playerHM;
		}
		break;
	case FourAcrossEdge: // ok 2/4/2020
		{	
		/*
		 *  like fouracross, but position the boxes at the left and right edge
		 
		 	_...._
		 	......
		 	_...._
		 
		 */
		rotations = fourAcrossRotation;
		positions = new int[][] { {xright,ybot}, {xleft,ybot},		// bottom X...X
								  {xleft,ytop}, { xright,ytop}};	// top X...X
		// rectangles ok 2/26			  
		addTopToBottom(left,false);
		addTopToBottom(xright,false);

		left += playerWM;
		right -= playerWM;
		}
		break;
	case FiveAcross: // ok 2/4/2020
		{
			/*
			  
			   _........._
			   ...........
			   _...._...._
			 
			 */

   		rotations = fourAcrossRotation;	// there is an extra Pi for this case
		positions = new int[][] { {xthirdRight,ybot}, {xthirdLeft,ybot},		// bottom .X.X.
								  {xleft,ytop}, {xmid,ytop}, { xright,ytop}};	// top    X.X.X
		// rectangles ok 2/26
		add2XAcross(ybot);
		add3XAcross(top);
		
		//spare rects checked 2/16/19
		top += playerHM;
		bottom -= playerHM;
		
		}
		break;
	case FiveAcrossEdge: // ok 2/4/2020
		{	// this carves out a strip across the top
			// for 3 players, and the right and left margins
			// for the other two. This leaves more space in the center bottom
	   		rotations = fourAcrossRotation;	// there is an extra Pi for this case
			positions = new int[][] { {xright,ybot}, {xleft,ybot},				// bottom X...X
									  {xleft,ytop}, {xmid,ytop}, { xright,ytop}};// top   X.X.X
			// ok 2/26		  
			add3XAcross(top);
			addTopToBottom(left,false);
			addTopToBottom(xright,false);
			top += playerHM;
			left += playerWM;
			right -= playerWM;
		}
		break;
	case SixAcross:	// ok 2/4/2020
		{
		/*
		  
		   _...._...._
		   ...........
		   _...._...._
		 
		 */
   		rotations = new double[]{0,0,0,Math.PI,Math.PI,Math.PI};
		positions = new int[][] { {xright,ybot}, {xmid,ybot}, {xleft,ybot},		// bottom X.X.X
								  {xleft,ytop}, {xmid,ytop}, { xright,ytop}};	// top    X.X.X
		// rectangles ok 2/26
		add3XAcross(ybot);
		add3XAcross(top);

		top += playerHM;
		bottom -= playerHM;
		}
		break;

	case ThreeWideLeft:	// ok 2/4/2020
		{
		rotations = new double[]{0,Math.PI/2,-Math.PI/2};
		positions = new int[][] {{xleft,ybot},	// bottom X...
								{xsideLeft,ymid-playerHM/2},	
							      {xsideRight,ymid}};				// top    X....

		// add the space beween the left and right player strips
		// ok 8/3/2021
		addSpareVStrip(left,left+playerHM);
		addSidebar(right-playerHM);
		
		{int ww = Math.max(playerWM,playerHM);
		left += ww;
		right -= playerHM;
		}
		}
		break;
	
	case ThreeWide: // ok 2/4/2020
	{
   		/*
		  _......
		  .......
		  _....._ 
		  
		 */
		rotations = new double[]{0,Math.PI/2,-Math.PI/2};
		positions = new int[][] {{xmid,ybot},	// bottom X...X
								{xsideLeft,ymid},	
							      {xsideRight,ymid}};				// top    X....

		// add the space beween the left and right player strips
		int spareX = left+playerHM;
		addToSpare(new Rectangle(spareX,ybot,xmid-spareX,playerHM));
		int spareX2 = xmid+playerWM;
		addToSpare(new Rectangle(spareX2,ybot,right-playerHM-spareX2,playerHM));

		addSidebar(left);
		addSidebar(right-playerHM);
		bottom -=playerHM;
		left += playerHM;
		right -= playerHM;
	}
	break;
	case FourAround:	// ok 2/4/2020
		/* four around
		
		......_.....
		|..........|
		......_.....
		
	 	*/
	case ThreeAround:	// ok 2/4/2020
		/* three around in a U shape, leaving the right unoccupied
		
		......_.....
		|...........
		......_.....
		
	 	*/
		{
		rotations = fourAroundRotation;
		positions = new int[][] { {xmid,ybot},		// bottom ..X..
								{xsideLeft,ymid},	// left 
								{xmid,ytop},		// top ..X..
								{xsideRight,ymid}};	// right
								
		int spareY = ycenter-playerWM/2;
		int spareX = xmid+playerWX;
		int spareH = Math.min(playerHM,spareY-ytop);
		
		// this is like addx1across, but takes into account that the 
		// sideways recangles at the left and right might eat into it.
		addToSpare(new Rectangle(spareX,top,right-spareX,spareH));	
		addToSpare(new Rectangle(left,top,xmid-left,spareH));
		int yb = bottom-spareH;
		addToSpare(new Rectangle(spareX,yb,right-spareX,spareH));	
		addToSpare(new Rectangle(left,yb,xmid-left,spareH));

		// rectangles ok 8/2/2021
		addSkinnyLeft(seating==DefinedSeating.FourAround,false,false);				
		if(seating==DefinedSeating.FourAround)
		{	
			right -= playerHM;
		}
		left += playerHM;
		top += playerHM;
		bottom -= playerHM;
		}
		break;
	case FourAroundEdgeRect:		// ok 2/4/2020
		{	/* like four around, but place the top and bottom rectangles near the left and right
			   and make the central rectangle more rectangular
					_...........
					|..........|
					..........._  
			*/
			rotations = fourAroundRotation;
			int boxY2 = ybot-(playerWM-playerHM)/2;
			positions = new int[][] { {xleft,ybot}, 	// bottom X....
									{ xsideLeft,ytop+(playerWM-playerHM)/2}, 	// left, aligned to box top
									{ xright,ytop},		// top ....X 
								{xsideRight,boxY2}};	// right, aligned to box bottom
			// in this layout we clip left and right margins only
			// and the spare rectangles are in the new left and right spaces
			// ok 2/26
			addFatLeftTop(false);
			Rectangle re = new Rectangle(playerWM,top,xright-(xleft+playerWX),playerHM);
			addToSpare(re);
			int y2 = ytop+playerHX;
			Rectangle ri = new Rectangle(right-playerHM,y2,playerHM,bottom-playerWM-y2);
			addToSpare(ri);
			left += playerWM;
			right -= playerHM;
			top += playerHM;
		}
		break;

	case FourAroundEdge:	// ok 2/4/2020
		{	/* like four around, but place the top and bottom rectangles near the left and right
					_...........
					|..........|
					..........._  
		
		 	*/
			rotations = fourAroundRotation;
			int boxY2 = ybot-(playerWM-playerHM)/2;
			positions = new int[][] { {xleft,ybot}, 	// bottom X....
									{ xsideLeft,ytop+(playerWM-playerHM)/2}, 	// left, aligned to box top
									{ xright,ytop},		// top ....X 
								{xsideRight,boxY2}};	// right, aligned to box bottom
			// in this layout we clip left and right margins only
			// and the spare rectangles are in the new left and right spaces
			// ok 2/26
			addFatLeftTop(true);
			left += playerWM;
			right -= playerWM;
		}
		break;
		
	case SixAround:	// ok 2/4/2020
		{
		/*
		  ..._..._...
		  |.........|
		  ..._..._...
		 
		 */
		rotations = sixAroundRotation;
		positions = new int[][] { {xthirdRight,ybot}, {xthirdLeft,ybot}, 	// bottom .X.X.
									{ xsideLeft,ymid},						// left 
									{ xthirdLeft,ytop}, {xthirdRight,ytop}, // top .X.X.
									{xsideRight,ymid}};						// right 
		// rectangles ok 2/26
		add2XAcross(top);
		add2XAcross(ybot);
		// ok 8/2/2021
		addSkinnyLeft(true,false,false);

		right -= playerHM;
   		left += playerHM;
		top += playerHM;
		bottom -= playerHM;
		}
		break;
	case FiveAround1EdgeCenter:
	case SixAroundEdge:	// ok 2/4/2020
		{
		/* like six across, but pull the boxes to the left and right edge
		 
		   _......._
		   |.......|
		   _......._
		 */
		rotations = new double[]{ 0,0,Math.PI/2,Math.PI,Math.PI,-Math.PI/2};
		positions = new int[][] { {xright,ybot}, {xleft,ybot},		// bottom X...X
								  { xsideLeft,ymid}, 				// left side
									{ xleft,ytop}, {xright,ytop},	// top X...X
									{xsideRight,ymid}};				// right side
		// spare rects checked 2/26
		// add 3 part rectangles on the left and right edges
		if(seating==DefinedSeating.SixAroundEdge)
		{
		addFatLeftCentered(true,true);
		}
		else {
			addFatLeftCentered(false,true);
			addTopToBottom(xright,false);
		}
		right -= playerWM;
		left += playerWM;
		}
		break;
		
	case FourAroundUW:	// ok 2/4/2020
		{
		/*  four around in a U shape, leaving the top unoccupied
		 
		 	.............
		 	|...........|
		 	__.........__
		  
		  place the left and right centered in the available space
		 */
		int ypos = ymid-playerHM/2;
		int stripH = ymid-playerWM/2;
		int overrun = ((stripH+playerWM)-(bottom-playerHM))/2;
		if(overrun<0) 
			{ ypos += overrun; stripH += overrun; 
			}
		rotations = new double[]{ 0,0,Math.PI/2,-Math.PI/2};
		positions = new int[][] { {xright,ybot}, {xleft,ybot}, 
			{ xsideLeft,ypos},						// left side
			{xsideRight,ypos}};						// right side
			
					
					
					// add the space beween the left and right player strips
		addSpareVStripFrom(stripH,left,left+playerHM);
		addSpareVStripFrom(stripH,xright,xright);

		{int ww = Math.max(playerWM,playerHM);
		left += ww;
		right -= ww;
		}
		}
		break;

	case FourAroundU:	// ok 2/4/2020
		{	/* four around in a U shape, leaving the top unoccupied
		
			............
			|..........|
			..._...._...
			
		 	*/
			int ypos = ymidUp;
			int ytop = ymidUp-playerWM/2+playerHM/2;
			rotations = new double[]{ 0,0,Math.PI/2,-Math.PI/2};
			positions = new int[][] { {xthirdRight,ybot}, {xthirdLeft,ybot}, 	// bottom .x.x.
						{ xsideLeft,ypos},						// left side
						{xsideRight,ypos}};						// right side
			// ok 8/2/2021
			addSkinnyLeftFrom(ytop,true,true,false);				
			add2XAcross(ybot);
			left += playerHM;
			bottom -= playerHM;
			right -= playerHM;
		}
		break;
	case FiveAround:	// ok 2/4/2020
		{
	   		/*
  		  ....._.....
  		  |.... ....|
  		  ..._..._...
  		  
  		  */
 		rotations = fiveAroundRotation;
		positions = new int[][] { {xthirdRight,ybot}, {xthirdLeft,ybot}, 	// bottom .x.x.
									{ xsideLeft,ymid},						// left side
									{xmid,ytop},							// top  ..X..
									{xsideRight,ymid}};						// right side
		// spare rects ok 2/26
		add1XAcross(top);
		add2XAcross(ybot);
		// ok 8/2/2021
		addSkinnyLeft(true,false,false);

		left += playerHM;
		top += playerHM;
		bottom -= playerHM;
		right -= playerHM;
		}
		break;
	case FiveAround1Edge: // ok 2/4/2020
		{
    		/*
		    ..._..._... 
		 	|..........
		 	..._..._...
		*/
		rotations = sixAroundRotation;	// last space will be ignored
		positions = new int[][] { {xthirdRight,ybot}, {xthirdLeft,ybot}, 	// bottom .x.x.
									{ xsideLeft,ymid},						// left side
									{ xthirdLeft,ytop}, {xthirdRight,ytop}, // top .X.X.
									};						// right side
		// spare rects ok 2/26
		add2XAcross(top);
		add2XAcross(ybot);
		// ok 8/3/2021
		addSkinnyLeft(false,false,false);
	
		left += playerHM;
		top += playerHM;
		bottom -= playerHM;
		}
		break;

	case FiveAroundEdge:	// ok 2/4/2020
		{
			/* in this layout we take the left and right margins, not the top and bottom
		
				_........
				|.......|
				_......._
				
			 */
			rotations = fiveAroundRotation;
			positions = new int[][] { {xright,ybot}, {xleft,ybot},	// bottom  X...X
									  {xsideLeft,ymid},				// left side
									  {xleft,ytop}, 				// top     X....
									  {xsideRight,ymid}};			// right side
			addFatLeftCentered(true,false);
			left += playerWM;
			right -= playerWM;
	
		}
		break;

	case Across: // ok 2/4/2020
		{
		positions = new int[nPlayers][2] ;
		bottom -= playerHM;
		for(int i=0;i<nPlayers;i++) { positions[i][0] = xleft+i*playerWM; positions[i][1]=bottom; }
		int spareX = xleft+playerWM*nPlayers;
		int spareW = right-spareX;
			if(spareW>10)
			{
				addToSpare(new Rectangle(spareX,bottom,spareW,playerHM));
			}
		}
		break;

	case Portrait:	// ok 2/4/2020
		{
		positions = new int[nPlayers][2] ;
		bottom -= playerHM*nPlayers;
		for(int i=0;i<nPlayers;i++) { positions[i][0] = xleft; positions[i][1]=bottom+i*playerHM; }
		int spareX = xleft+playerWM;
		addToSpare(new Rectangle(spareX,bottom,right-spareX,playerHM*nPlayers));
		}
		break;
	case Landscape3X:
		{
		positions = new int[][] { {xright-playerWM*2,ytop},{xright-playerWM,ytop},{xright,ytop},
								  {xright-playerWM*2,ytop+playerHM},{xright-playerWM,ytop+playerHM}, { xright, ytop+playerHM}};
		right -= playerWM*3;
		int spareY = top+(playerHM*((nPlayers+2)/3));
		// rectangles ok 2/26
		addToSpare(new Rectangle(right,spareY,playerWM*3,bottom-spareY));
		int rem = (nPlayers%3);
		if(rem!=0)
			{
			addToSpare(new Rectangle(right+rem*playerWM-marginSize,spareY-playerHM,(3-rem)*playerWM+marginSize,playerHM));
			}
		}
		break;
	
	case Landscape2X:
		{
		positions = new int[][] { {xright-playerWM,ytop},{xright,ytop},
			{xright-playerWM,ytop+playerHM}, { xright, ytop+playerHM},
			{xright-playerWM,ytop+playerHM*2}, { xright,ytop+playerHM*2}};
		right -= playerWM*2;
		int spareY = top+(playerHM*((nPlayers+1)/2));
		// rectangles ok 2/26
		addToSpare(new Rectangle(right,spareY,playerWM*2,bottom-spareY));
		if((nPlayers&1)!=0)
		{
			addToSpare(new Rectangle(right+playerWX,spareY-playerHM,playerWM+marginSize,playerHM));
		}
		}
		break;
	case SideBySide:   // ok 2/4/2020
	case Landscape:
		{
		positions = new int[nPlayers][2];
		for(int i=0;i<nPlayers;i++) { positions[i][0] = xright; positions[i][1]=ytop+i*playerHM; }
		int spareY =ytop+playerHM*nPlayers-marginSize+1;	// spare rects ok 2/26
		addToSpare(new Rectangle(xright,spareY,right-xright,bottom-spareY));
		right -= playerWM;
		break;
		}
	default: G.Error("Not expecting %s", seating);
	}
	rects.setMainRectangle(new Rectangle(left,top,right-left,bottom-top));
}
	



	/**
	 * perform the layout just calculated.
	 * 
	 * @param window
	 * @param unitsize
	 */
	public void doLayout(GameLayoutClient window,double zoom,int unitsize,Rectangle full)
	{	
	// place all the players	
		for(int i=0;i<nPlayers;i++)
		{
		int []position = positions[i];
		Rectangle playerRect = window.createPlayerGroup(i,
				(int)(position[0]*zoom+0.5),
				(int)(position[1]*zoom+0.5),
				rotations[i],
				(int)(unitsize*zoom+0.5));
		if(G.debug())
			{
			Rectangle playerRectc = G.copy(null, playerRect);
			G.setRotation(playerRectc, -(window.getPlayerOrTemp(i).displayRotation));
			if(!full.contains(playerRectc)) 
				{G.print("player rectangle runs off screen\n",playerRect,"\n",full);
				}
			}
		}
	}
	
	private DefinedSeating tryThese[] = 
		{   DefinedSeating.Portrait,DefinedSeating.Across,DefinedSeating.Landscape,
			DefinedSeating.Landscape2X,	DefinedSeating.Landscape3X,
			DefinedSeating.Portrait2X, DefinedSeating.Portrait3X};
	
	private DefinedSeating selectedSeating = DefinedSeating.Landscape;
	private int selectedNPlayers = -1;
	private double selectedPercent = -1;
	public DefinedSeating selectedSeating() { return(selectedSeating); }
	private double selectedCellSize=0;
	public double selectedCellSize() { return(selectedCellSize); }
	/**
	 * select a layout for the game, based on the defined seating chart and
	 * a desired share of the space to be occupied by the board.  The driving
	 * forces in this algorithm are the "box" that occupies a player's private data
	 * and the number of players.
	 * The player boxes are laid out according to one of the offline seating
	 * charts, or simple plans for online presentation with one or two
	 * columns of players below or right of the board.
	 * 
	 * The residual will be a central box to be occupied by the board
	 * and whatever other ornaments are needed for the game.  There
	 * may be some other space available depending on the exact geometry.
	 * 
	 * @param client				 the client window
	 * @param nPlayers				 number of players
	 * @param width					 width of the actual window
	 * @param height				 height of the actual window
	 * @param marginSize 			 size of margins between boxes
	 * @param minBoardShare			 the minimum share of the board rectangle vs the player rectangles
	 * @param aspectRatio			 preferred aspect ratio for the board rectangle
	 * @param maxCellSize			 maximum cell size for the player box
	 * @param targetLayoutHysterisis when a seating layout is specified, weight this in favor of alternatives
	 * @return the percent coverage for the board in the selected layout
	 */
	public double selectLayout(GameLayoutClient client,int nPlayers,int width,int height,
					int margin,double minBoardShare,
					double aspectRatio,double maxCellSize, double targetLayoutHysterisis)
	{	
		int minSize = client.standardFontSize();
		return selectLayout(client,nPlayers,width,height,
				margin,minBoardShare,
				aspectRatio,Math.min(maxCellSize,minSize*1.8),maxCellSize,
				targetLayoutHysterisis);
	}
	public double selectLayout0(GameLayoutClient client,int nPlayers,int fullwidth,int fullheight,
			int margin,double minBoardShare,
			double aspectRatio,double minSize,double maxCellSize, double targetLayoutHysterisis)
	{	// playtable has a deep bezil that makes the extreme edge hard to get to
		int extramargin = G.isRealPlaytable()?G.minimumFeatureSize()/2 : 0;
		int width = fullwidth-extramargin;
		int height = fullheight-extramargin;
		if(nPlayers!=selectedNPlayers || !client.isZoomed())
		{
		double bestPercent = 0;
		double bestScore = 0;
		double desiredAspectRatio = aspectRatio;
		double targetPreference = targetLayoutHysterisis;
		DefinedSeating best = null;
		DefinedSeating currentSeating = client.seatingChart();
		DefinedSeating originalSeating = currentSeating;
    	rects.marginSize = margin;
    	preferredAspectRatio = desiredAspectRatio;
	    if(currentSeating!=DefinedSeating.Undefined)
	    {	while(currentSeating!=null)
	    	{
	    	double currentPercent = sizeLayout(client,nPlayers,currentSeating,minBoardShare,desiredAspectRatio,maxCellSize,minSize,width,height,margin);
	    	double currentCellSize = selectedCellSize;
	    	double currentScore = currentPercent*currentCellSize;
	    	//G.print("S "+currentSeating+" "+currentPercent+" "+currentScore);
	    	if(currentCellSize>1 && (best==null || currentScore>bestScore))
	    	{
	    		bestPercent = currentPercent;
	    		bestScore = currentScore;
	    		best = currentSeating;
	    		//sizeLayout(client,nPlayers,currentSeating,minBoardShare,desiredAspectRatio,maxCellSize,minSize,width,height);
		    	}
	    	currentSeating = currentSeating.alternate;
	    	if(currentSeating==originalSeating) { currentSeating=null; }
	    	}
	    }
	    else // if there is no target, there's no preference either.
	    { targetPreference = 1.0; 	// no target to prefer
	      best = currentSeating;
	    }
	    
		for(DefinedSeating s : tryThese)	// also try the generic layouts 
		{
			double v = sizeLayout(client,nPlayers,s,minBoardShare,desiredAspectRatio,maxCellSize,minSize,width,height,margin);
			double score = v*selectedCellSize;
			//G.print(""+s+" board "+v+" cell "+selectedCellSize+" = "+score);
	    	//G.print("S "+s+" "+v+" "+score);
	    	if(selectedCellSize>1 && 
					((best==DefinedSeating.Undefined)
					|| (score*targetPreference>bestScore)))
				{ // select the biggest cell whose board percentage
				  // is in the acceptable range
				  bestPercent = v; 
				  best=s; 
				  bestScore = score;
				  //sizeLayout(client,nPlayers,s,minBoardShare,desiredAspectRatio,maxCellSize,minSize,width,height);
				}
		}
    	//G.print("best "+best+" "+bestPercent+" "+bestScore);

		if(best==DefinedSeating.Undefined)
		{	// the screen has no acceptable layouts (he's making his window tiny?)
			// but give him something...
			if(G.debug()) { G.print("No seating! ",width,"x",height," min ",minSize," players "+nPlayers);}
			best = DefinedSeating.Across;
		}
		//G.print("target "+selectedSeating+" "+selectedCellSize+" best "+bestPercent);
		//setLocalBoundsSize(client,best,boardShare,width,height);
	    selectedSeating = best;
		selectedNPlayers = nPlayers;
		selectedPercent = bestPercent;
	    // this sets selectedCellSize again, but with no minimum.  This covers the 
	    // unusual case where the best layout was one with a substandard cell size
	    sizeLayout(client,nPlayers,selectedSeating,minBoardShare,aspectRatio,maxCellSize,minSize,width,height,margin);
		}
		else
		{
		    // this sets selectedCellSize again, but with no minimum.  This covers the 
		    // unusual case where the best layout was one with a substandard cell size
		    sizeLayout(client,nPlayers,selectedSeating,minBoardShare,aspectRatio,maxCellSize,minSize/2,width,height,margin);

		}
		

		int halfMargin = extramargin/2;
    	makeLayout(selectedSeating,nPlayers,halfMargin,halfMargin,width,height,client.createPlayerGroup(0,0,0,0,(int)selectedCellSize),margin);
        doLayout(client,1.0,(int)selectedCellSize,new Rectangle(halfMargin,halfMargin,width,height));
	    return(selectedPercent);	    
	}

	/**
	 * select a layout for the game, based on the defined seating chart and
	 * a desired share of the space to be occupied by the board.  The driving
	 * forces in this algorithm are the "box" that occupies a player's private data
	 * and the number of players.
	 * The player boxes are laid out according to one of the offline seating
	 * charts, or simple plans for online presentation with one or two
	 * columns of players below or right of the board.
	 * 
	 * The residual will be a central box to be occupied by the board
	 * and whatever other ornaments are needed for the game.  There
	 * may be some other space available depending on the exact geometry.
	 * 
	 * @param client				 the client window
	 * @param nPlayers				 number of players
	 * @param width					 width of the actual window
	 * @param height				 height of the actual window
	 * @param marginSize 			 size of margin between boxes
	 * @param minBoardShare			 the minimum share of the board rectangle vs the player rectangles
	 * @param aspectRatio			 preferred aspect ratio for the board rectangle
	 * @param minSize				 minimum cell size for the player box
	 * @param maxCellSize			 maximum cell size for the player box
	 * @param targetLayoutHysterisis when a seating layout is specified, weight this in favor of alternatives
	 * @return the percent coverage for the board in the selected layout
	 */
	public double selectLayout(GameLayoutClient client,int nPlayers,int fullwidth,int fullheight,
			int margin,double minBoardShare,
			double aspectRatio,double minSize,double maxCellSize, double targetLayoutHysterisis)
		{
		boolean oldversion = false;
		if(oldversion) 
			{ return selectLayout0(client,nPlayers,fullwidth,fullheight,margin,minBoardShare,aspectRatio,
					minSize,maxCellSize,
					targetLayoutHysterisis);	
			}
		return(selectLayout(client,nPlayers,fullwidth,fullheight,
				1.0,margin,minBoardShare,
				aspectRatio,minSize,maxCellSize,targetLayoutHysterisis));
	}

	/**
	 * select a layout for the game, based on the defined seating chart and
	 * a desired share of the space to be occupied by the board.  The driving
	 * forces in this algorithm are the "box" that occupies a player's private data
	 * and the number of players.
	 * The player boxes are laid out according to one of the offline seating
	 * charts, or simple plans for online presentation with one or two
	 * columns of players below or right of the board.
	 * 
	 * The residual will be a central box to be occupied by the board
	 * and whatever other ornaments are needed for the game.  There
	 * may be some other space available depending on the exact geometry.
	 * 
	 * The "old" version of this logic used exact width and height if zoomed
	 * windows, and the exact values of sizing parameters.  This proved to be
	 * hard to stabilize because the base size parameters were derived from
	 * font sizes, and that changed as the window zoomed up.
	 * 
	 * The "new" version uses zoom>1.0, corresponding to the global zoom factor,
	 * and allocates based on width/zoom and height/zoom, which ought to be the
	 * actual (unzoomed) size of the window.   Sized parameters, minsize maxsize 
	 * ought to be based on the the global default font size, or some other
	 * metric which is not affected by the zoom.
	 * 
	 * @param client				 the client window
	 * @param nPlayers				 number of players
	 * @param width					 width of the actual window
	 * @param height				 height of the actual window
	 * @param marginSize 			 size of margin between boxes
	 * @param minBoardShare			 the minimum share of the board rectangle vs the player rectangles
	 * @param aspectRatio			 preferred aspect ratio for the board rectangle
	 * @param minSize				 minimum cell size for the player box
	 * @param maxCellSize			 maximum cell size for the player box
	 * @param targetLayoutHysterisis when a seating layout is specified, weight this in favor of alternatives
	 * @return the percent coverage for the board in the selected layout
	 */
	public double selectLayout(GameLayoutClient client,int nPlayers,int fullwidth,int fullheight,
			double zoom,int margin,double minBoardShare,
			double aspectRatio,double minSize,double maxCellSize, double targetLayoutHysterisis)
	{	// playtable has a deep bezil that makes the extreme edge hard to get to
		int extramargin = G.isRealPlaytable()?G.minimumFeatureSize()/2 : 0;
		int width = (int)(fullwidth/zoom)-extramargin;
		int height = (int)(fullheight/zoom)-extramargin;
		rects.zoom = zoom;
		if((nPlayers!=selectedNPlayers) || ((zoom==1.0) ? !client.isZoomed() : true))
		{
		double bestPercent = 0;
		double bestScore = 0;
		double desiredAspectRatio = aspectRatio;
		double targetPreference = targetLayoutHysterisis;
		DefinedSeating best = null;
		DefinedSeating currentSeating = client.seatingChart();
    	rects.marginSize = margin;
    	preferredAspectRatio = desiredAspectRatio;
	    if(currentSeating!=DefinedSeating.Undefined)
	    {	// seatings for a particular player arrangement can have
	    	// alternatives that make the board area more efficient, for
	    	// example 3 players "around" the table might have the players
	    	// at the center of each side, or 3 players squished to one end
	    	DefinedSeating originalSeating = currentSeating;
	    	while(currentSeating!=null)
	    	{
	    	double currentPercent = sizeLayout(client,nPlayers,currentSeating,minBoardShare,desiredAspectRatio,maxCellSize,minSize,width,height,margin);
	    	double currentCellSize = selectedCellSize;
	    	double currentScore = currentPercent*currentCellSize;
	    	//G.print("S "+currentSeating+" "+currentPercent+" "+currentScore+" "+currentPercent+" "+currentCellSize);
	    	if(currentCellSize>1 && (best==null || currentScore>bestScore))
	    	{
	    		bestPercent = currentPercent;
	    		bestScore = currentScore;
	    		best = currentSeating;
	    		//sizeLayout(client,nPlayers,currentSeating,minBoardShare,desiredAspectRatio,maxCellSize,minSize,width,height);
		    	}
	    	currentSeating = currentSeating.alternate;
	    	if(currentSeating==originalSeating) { currentSeating=null; }
	    	}
	    }
	    else // if there is no target, there's no preference either.
	    { targetPreference = 1.0; 	// no target to prefer
	      best = currentSeating;
	    }
	    
		for(DefinedSeating s : tryThese)	// also try the generic layouts 
		{
			double v = sizeLayout(client,nPlayers,s,minBoardShare,desiredAspectRatio,maxCellSize,minSize,width,height,margin);
			double score = v*selectedCellSize;
			//G.print(""+s+" board "+v+" cell "+selectedCellSize+" = "+score);
	    	//G.print("S "+s+" "+v+" "+score);
	    	if(selectedCellSize>1 && 
					((best==DefinedSeating.Undefined)
					|| (score*targetPreference>bestScore)))
				{ // select the biggest cell whose board percentage
				  // is in the acceptable range
				  bestPercent = v; 
				  best=s; 
				  bestScore = score;
				  //G.print("S+ "+best+" "+bestPercent+" "+bestScore+ " "+v+" "+selectedCellSize);
				  //sizeLayout(client,nPlayers,s,minBoardShare,desiredAspectRatio,maxCellSize,minSize,width,height);
				}
		}
		//G.print("best "+best+" "+bestPercent+" "+bestScore);

		if(best==DefinedSeating.Undefined)
		{	// the screen has no acceptable layouts (he's making his window tiny?)
			// but give him something...
			if(G.debug()) { G.print("No seating! ",width,"x",height," min ",minSize," players "+nPlayers);}
			best = DefinedSeating.Across;
		}
		//G.print("target "+selectedSeating+" "+selectedCellSize+" best "+bestPercent);
		//setLocalBoundsSize(client,best,boardShare,width,height);
	    selectedSeating = best;
		selectedNPlayers = nPlayers;
		selectedPercent = bestPercent;
		}
		
	    sizeLayout(client,nPlayers,selectedSeating,minBoardShare,aspectRatio,maxCellSize,minSize,width,height,margin);

	    if(selectedCellSize<=0)
	    {
	    	// in the rare case that the best layout still didn't produce a valid cell size,
	    	// try once more with a smaller minimum
		    sizeLayout(client,nPlayers,selectedSeating,minBoardShare,aspectRatio,maxCellSize,minSize/2,width,height,margin);
	    }
	    		
		int halfMargin = extramargin/2;
 
		makeLayout(selectedSeating,nPlayers,halfMargin,halfMargin,width,height,client.createPlayerGroup(0,0,0,0,(int)selectedCellSize),margin);
        doLayout(client,zoom,(int)selectedCellSize,new Rectangle(halfMargin,halfMargin,fullwidth-margin,fullheight-margin));
	    return(selectedPercent);	    
	}
	
	/**
	 * this knows the characteristics of the DefinedSeating and does rough
	 * calculations based on the positions of the player boxes and the
	 * actual width and height of the board.
	 * 
	 * The values returned are the percentage of space occupied by the residual
	 * box for the board, and the cell size to be used for the player boxes.  It's
	 * desirable that both large cells and large board are the result, but also 
	 * contradictory.
	 * 
	 * @param client
	 * @param seating
	 * @param minBoardShare
	 * @param width
	 * @param height
	 * @return the percentage of space occupied by the "big box" for the board.
	 */
    private double sizeLayout(GameLayoutClient client,int nPlayers,DefinedSeating seating,
    		double minBoardShare,double desiredAspectRatio,double cellSize,double minSize,
    		double width,double height,int marginSize)
    {	
    	double unit = cellSize;
    	//
    	// createPlayerGroup returns the bounding rectangle for a player group based on "unit" as the size.
    	// It's important that it return the same retangle (proportional to unit) to and unit size.
    	//
    	Rectangle box = client.createPlayerGroup(0,0,0,0,(int)unit);
    	double playerW = Math.ceil((double)(G.Width(box))/unit);		// width of the player box in units
    	double playerH = Math.ceil((double)(G.Height(box))/unit);		// height of the player box in units
    	double unitsX=0;					// this will be the number of horizontal units the layout requires
    	double unitsY=0;					// this will be the number of vertical units the layout requires
    	double edgeUnitsX=0;				// this will be the number of units chipped off the horizontal axis
    	double edgeUnitsY=0;				// this will be the number of units chipped off the vertical axis
    	double fixedW = 0;					// this will be the fixed margin size needed for the layout.  Generally 2 for a standalone box
    	double fixedH = 0;					// or 2*n+1 for a group of n adjacent boxes.  The margin quantum doesn't vary with the unit size.
    	
    	// size the particular layout.  We need to know the size and shape of the hole left after all the
    	// player boxes are placed, and how many margin units need to be added to the boxes.  Then we will
    	// shrink the unit size until the box left is big enough to meet the minBoardShare criteria
    	switch(seating)
    	{  	
     	case Undefined:
    		throw G.Error("Not expecting %s as seating",seating);
    	case ThreeAroundLeft:
    		
    		/* top and bottom are flush to the left, leaving a bigger right hand rectangle
 		   this is currently used by triad
 		
 	   		 __.........
     		 ...........
     		 |..........
     		 ...........
     		 __.........
     		 
    		 */
     		
    		unitsX = playerW;		
    		unitsY = playerH*2+playerW;
	   		fixedH = marginSize*4;
	   		fixedW = marginSize;
	   		edgeUnitsX = playerW;		// the left edge will be chipped off 
    		edgeUnitsY = 0;
    		break;
		case ThreeAcrossLeft:
			/*
			 position the players at the left and right, and spare at the left and bottom
			 leaving the upper-right corner as the main rectangle
			  
				 _......_
				 ........
				 _.......
			*/
			unitsX = playerW;			
	   		unitsY = playerH;			
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*2;
	   		edgeUnitsX = playerW;	
	   		edgeUnitsY = playerH;		
	   		break;
		case ThreeAcrossLeftCenter:
			/* same physical layout as ThreeAcrossLeft, but attribute
			   the spare space to the left and right
			   leaving the center column as the main rectangle
				 _......_
				 ........
				 _.......
			*/
			unitsX = playerW*2;			//  bottom x....x
	   		unitsY = playerH*2;			//  top    x.....
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*2;
	   		edgeUnitsX = playerW*2;
	   		edgeUnitsY = 0;
	   		break;

		case FourAroundUW:		
			/*  four around in a U shape, leaving the top unoccupied
			 
		 	.............
		 	|...........|
		 	_..........._
		  
			*/

			unitsX = playerW*2;
			unitsY = playerW+playerH;
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*3;
			edgeUnitsX = playerW*2;
			edgeUnitsY = 0;
			break;
			
		case FourAroundU:
			/* four around in a U shape, leaving the top unoccupied
			
			............
			|..........|
			..._...._...
			
		 	*/
			unitsX = playerW*2;
    		unitsY = playerH+playerW;
	   		fixedW = marginSize*4;
	   		fixedH = marginSize*2;
    		edgeUnitsX = playerH;
    		edgeUnitsY = playerH*2;
    		break;
		case ThreeAround:
			/* three around in a U shape, leaving the right unoccupied
			
				......_.....
				|...........
				......_.....
			
		 	*/

    		unitsX = playerW;
    		unitsY = playerH*2+playerW;
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*2;
    		edgeUnitsX = playerH;
    		edgeUnitsY = playerH*2;
    		break;


		case FourAroundEdgeRect:
			/* like four around, but place the top and bottom rectangles near the left and right
			   and make the central rectangle more rectangular
					_...........
					|..........|
					..........._  
			*/
       		unitsX = playerW*2;
    		unitsY = playerH+playerW;
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*3;
    		edgeUnitsX = playerW+playerH;
    		edgeUnitsY = playerH;
    		break;
	
		case FourAroundEdge:
			/* like four around, but place the top and bottom rectangles near the left and right
			 
			_...........
			|..........|
			..........._  
			 
			 */
       		unitsX = playerW*2;
    		unitsY = playerH+playerW;
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*3;
    		edgeUnitsX = unitsX;
    		edgeUnitsY = 0;
    		break;
		case FourAcrossEdge:
			/*
			 *  like fouracross, but position the boxes at the left and right edge
			 
			 	_...._
			 	......
			 	_...._
			 
			 */
       		unitsX = playerW*2;
    		unitsY = playerH*2;
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*2;
    		edgeUnitsX = unitsX;
    		edgeUnitsY = 0;
    		break;
    	case ThreeAcross:
    		/*
    		 	...._....
    		 	.........
    		 	.._..._..
    		  
    		 */
    	case FourAcross:
    		/*
		 	.._..._..
		 	.........
		 	.._..._..
		  
    		 */
    		unitsX = playerW*2;
    		unitsY = playerH*2;
	   		fixedW = marginSize*4;
	   		fixedH = marginSize*2;
    		edgeUnitsX = 0;
    		edgeUnitsY = unitsY;
    		break;
    		
    	case ThreeWideLeft:
    		unitsX = playerW+playerH;
    		unitsY = playerW+playerH;
	   		fixedW = marginSize*2;
	   		fixedH = marginSize;

    		edgeUnitsX = playerW+playerH;
    		edgeUnitsY = 0;
    		break;
    		
    	case ThreeWide:
    		/*
    		  _......
    		  .......
    		  _....._ 
    		  
    		 */
    		unitsX = playerH*2;
    		unitsY = playerH;
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*2;

    		edgeUnitsX = playerH*2;
    		edgeUnitsY = playerH;
    		break;
     	case FourAround:
    		/* four around
    		
    		......_.....
    		|..........|
    		......_.....
    		
    	 	*/
    		unitsX = playerW+playerH*2;
    		unitsY = playerH*2;
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*2;
    		edgeUnitsX = playerH*2;
    		edgeUnitsY = playerH*2;
    		break;
		case FiveAround1EdgeCenter:
		case SixAroundEdge:
			/* like six across, but pull the boxes to the left and right edge
			 
			   _......._
			   |.......|
			   _......._
			 */

    		unitsX = playerW*2+playerH;
    		unitsY = playerW+playerH*2;
    		edgeUnitsX = playerW*2;
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*4;
    		edgeUnitsY = 0;
    		break;
    		
		case SixAround:
			/*
			  ..._..._...
			  |.........|
			  ..._..._...
			 
			 */
    		unitsX = playerW*2;
    		unitsY = playerW+playerH*2;
	   		fixedW = marginSize*4;
	   		fixedH = marginSize*2;
    		edgeUnitsX = playerH*2;
    		edgeUnitsY = playerH*2;
    		break;
    	case FiveAround1Edge:
    		/*
    		    ..._..._... 
    		 	|..........
    		 	..._..._...
    		*/
    		unitsX = playerW*2;
    		unitsY = playerW+playerH*2;
	   		fixedW = marginSize*4;
	   		fixedH = marginSize*4;

    		edgeUnitsX = playerH;
    		edgeUnitsY = playerH*2;
    		break;
    	case FiveAround:
    		/*
    		  ....._.....
    		  |.... ....|
    		  ..._..._...
    		  
    		  */
    		unitsX = playerW*2;
    		unitsY = playerW+playerH*2;
	   		fixedW = marginSize*4;
	   		fixedH = marginSize*2;
    		edgeUnitsX = playerH*2;
    		edgeUnitsY = playerH*2;
    		break;
    	case FiveAroundEdge:
			/* in this layout we take the left and right margins, not the top and bottom
    		
			_........
			|.......|
			_......._
			*/
   			unitsX = playerW*2;
    		unitsY = playerH*2+playerW;
	   		fixedW = marginSize*2;
	   		fixedH = marginSize*4;

    		edgeUnitsX = playerW*2;
    		edgeUnitsY = 0;
    		break;
    	case FiveAcrossEdge:
    		/*
    		 	_......._
    		 	.........
    		 	_..._...._
    		  
    		 */
    		unitsX = playerW*3;
    		unitsY = playerH*2;
	   		fixedW = marginSize*4;
	   		fixedH = marginSize*2;

    		edgeUnitsX = playerW*2;
    		edgeUnitsY = playerH;
    		break;
    	case SixAcross:
    		/*
  		  
 		   _...._...._
 		   ...........
 		   _...._...._
 		 
    		 */

    	case FiveAcross:
			/*		  
			   _........._
			   ...........
			   _...._...._
			 
			 */		
    		unitsX = playerW*3;
    		unitsY = playerH*2;
	   		fixedW = marginSize*4;
	   		fixedH = marginSize*2;

    		edgeUnitsX = 0;
    		edgeUnitsY = playerH*2;
    		break;
 		// seating charts not represented in the seating viewer
    	case RightCornerWide:
      	case LeftCornerWide:
       		unitsX = playerW+playerH;
      		unitsY = playerW;
      		fixedW = 2*marginSize;
      		fixedH = 2*marginSize;
      		edgeUnitsX = playerH+playerW;
      		edgeUnitsY = 0;
      		break;
     		
     	case LeftCorner:	// ok 2/4/2020
      	case RightCorner:	// ok 2/4/2020
       		unitsX = playerW;
      		unitsY = playerH+playerW;
      		fixedW = 2*marginSize;
      		fixedH = 2*marginSize;
      		edgeUnitsX = playerH;
      		edgeUnitsY = playerH;
      		break;

      	case ThreeLeftLW:
      	case ThreeRightLW:
       		unitsX = playerW*2;
      		unitsY = playerW+playerH;
	   		fixedW = marginSize*4;
	   		fixedH = marginSize*2;

      		edgeUnitsX = playerW*2;
      		edgeUnitsY = 0;     		
      		break;
      		
      	case ThreeLeftL:
      	case ThreeRightL:
       		unitsX = playerW*2;
      		unitsY = playerW+playerH;
	   		fixedW = marginSize*4;
	   		fixedH = marginSize*2;
     		edgeUnitsX = playerH;
      		edgeUnitsY = playerH;
      		break;
      		

      	case Across:
    		unitsX = playerW*nPlayers;
    		unitsY = playerH;
    		fixedW = nPlayers*marginSize+marginSize;
    		fixedH = marginSize*2;
    		edgeUnitsX = 0;
    		edgeUnitsY = unitsY;
    		break;

      	case FaceToFaceLandscapeSide:	// ok 2/4/2020    	
    		/* player box top and bottom, trimming from right
  		  
		 	......._
		 	........
		 	......._
		 	
    		*/

    
       	case SideBySide:
       	case Landscape:
    		unitsX = playerW;
    		unitsY = playerH*nPlayers;
    		fixedH = nPlayers*marginSize+marginSize;
    		fixedW = marginSize*2;
    		edgeUnitsX = unitsX;
    		edgeUnitsY = 0;
    		break;


      	case FaceToFaceLandscapeTop:
      	case Portrait:
    		unitsX = playerW;
    		unitsY = playerH*nPlayers;
	   		fixedH = marginSize*nPlayers+marginSize;
	   		fixedW = marginSize*2;
    		edgeUnitsX = 0;
    		edgeUnitsY = unitsY;
    		break;
      	case Portrait3X:
    		{unitsX = playerW*3;
    		int nrows = ((nPlayers+2)/3);
    		unitsY = playerH*nrows;
    		fixedW = marginSize*3+marginSize;
    		fixedH = nrows*marginSize+marginSize;
    		edgeUnitsX = 0;
    		edgeUnitsY = unitsY;
    		}
    		if(nPlayers%3==1) { edgeUnitsY+=playerH/2; } 	// lie, make this look unattractive if we should be using portrait2X; } 
      		break;
      		
       	case FaceToFacePortrait:
       		{
       		// means players on the short side, whichever that is,
       		boolean xmajor = width<height;
    		unitsX = xmajor? playerW : playerH*2;
    		unitsY = xmajor? playerH*2 : playerW;
    		fixedW = marginSize*2;
    		fixedH = 2*marginSize;
    		edgeUnitsX = xmajor ? 0 : unitsY;
    		edgeUnitsY = xmajor ? unitsY : 0;
    		}
       		break;
    	case Portrait2X:	// two column portrait
    		{
    		int nrows = ((nPlayers+1)/2);
    		unitsX = playerW*2;
    		unitsY = playerH*nrows+marginSize;
    		fixedW = marginSize*2;
    		fixedH = nrows*marginSize+marginSize;
    		edgeUnitsX = 0;
    		edgeUnitsY = unitsY;
    		}
    		break;
    	case Landscape3X:	// three column landscape
    		{
    		int nrows = ((nPlayers+2)/3);
    		unitsX = playerW*3;
    		unitsY = playerH+nrows;
    		fixedW = 3*marginSize+marginSize;
    		fixedH = nrows*marginSize+marginSize;
    		edgeUnitsX = unitsX;
    		edgeUnitsY = 0;
    		if(nPlayers%3==1) { edgeUnitsX+=playerW/2; } 	// lie, make this look unattractive if we should be using landscape2X; } 
    		}
    		break;
    	case Landscape2X:	// two column landscape
    		{
    		int nrows = ((nPlayers+1)/2);
    		unitsX = playerW*2;
    		unitsY = playerH*nrows;
    		fixedW = 2*marginSize+marginSize;
    		fixedH = nrows*marginSize+marginSize;
    		edgeUnitsX = unitsX;
    		edgeUnitsY = 0;
    		}
    		break;
    	default: G.Error("Not expecting %s", seating);
    	};
    	
		double boardPercent = 0;
		double cell = (int)cellSize+2;
		double acceptedSize = -1;
		int acceptedW = 0;
		int acceptedH = 0;
		boolean sizeok = false;
		if(edgeUnitsY>0) { cell = Math.min(cell, (int)(1+height/edgeUnitsY)); }
		if(edgeUnitsX>0) { cell = Math.min(cell, (int)(1+width/edgeUnitsX)); }
		do 
		{	cell -= 1;
			double boardW = width-(edgeUnitsX*cell);
			double boardH = height-(edgeUnitsY*cell);
			double usedW = unitsX*cell+fixedW;
			double usedH = unitsY*cell+fixedH;
			sizeok = boardW>=0 && boardH>=0 && usedW<width && usedH<height;
			if(sizeok)
			{
		    double nextPercent;
		    if(strictBoardsize)
		    {
			    double sxw = boardW*Math.min(boardW/desiredAspectRatio,boardH);
			    double sxh = boardH*Math.min(boardH*desiredAspectRatio, boardW);
			    nextPercent = Math.min(sxw, sxh)/(width*height);
		    }
		    else
		    {
		    	
			double aspectRatio = boardW/boardH;
			double spareArea = boardW*boardH;
			double mina = Math.min(aspectRatio, desiredAspectRatio);
			double maxa = Math.max(aspectRatio, desiredAspectRatio);
			double plainE = (mina/maxa);
			// this requires some explanation to justify. The desired aspect ratio is intended
			// to prefer that the remainder (after placing the player boxes) is roughly the same
			// shape as the board.  One perverse result was that for hive, which has no preferred
			// shape, on wide rectangular boards, placing the player boxes side-by-side scored
			// as good as placing the boxes over-and-under.  The increased "efficiency" of 
			// making a square hole was exactly balanced by the loss of absolute area.  
			// adding the sqrt factor to efficiency will have the effect of preferring absolute
			// size over the matching the shape of the board, while still giving some preference
			// to the shape.
			double effeciency = Math.sqrt(plainE);
			nextPercent = effeciency*(spareArea/(width*height));
		    }
			if(nextPercent<=boardPercent)
				{ // if the coverage stops improving, we reached a limit 
				  // based on the aspect ratio of the board
				  cell+= 1; 
				  break; 
				}
			boardPercent = nextPercent;
			acceptedSize = cell;
			acceptedW = (int)Math.ceil(playerW*cell);
			acceptedH = (int)Math.ceil(playerH*cell);
			//G.print(""+seating+" "+boardPercent+" "+cell);
			}
		} while((!sizeok && (cell>minSize/2))
				|| ((cell>minSize) &&  (boardPercent<minBoardShare)));
		boolean downsize = false;
		if(acceptedSize>0)
		{
		do { 
			Rectangle finalBox = client.createPlayerGroup(0,0,0,0,(int)acceptedSize);
			int finalW = G.Width(finalBox);
			int finalH = G.Height(finalBox);
			downsize = (finalW>acceptedW || finalH>acceptedH);
			if(downsize)
			{	//if the createPlayerGroup is messing with fractions of the unit size, rounding can cause
				//jitter as we go to smaller sizes.  Rather than forbid it, detect it and step the unit 
				// down by 1 if it happens
				//G.print(G.format("Final box larger than expected, expected %sx%s got %sx%s cell %s min %s",acceptedW,acceptedH,finalW,finalH,acceptedSize,minSize));
				acceptedSize--;
				downsize = true;
			}} while(downsize);
		}
		selectedCellSize = acceptedSize;
    	return(boardPercent);
    }
    
    public Rectangle peekMainRectangle() { return(rects.peekMainRectangle()); }
   /** get the main rectangle, nominally designated to contain the board
     * 
     * @return the big inner rectangle left after placing the player boxes
     */
    public Rectangle getMainRectangle()
    {	return rects.allocateMainRectangle();
    }
    
    public void addToSpare(Rectangle r)
    {	if(G.debug()&& ((G.Width(r)<0 || G.Height(r)<0)))
    		{
    		G.print("Adding negative size rectangle to spare ",r);
    		}
    		rects.addToSpare(r);
    }


    public boolean placeTheVcr(GameLayoutClient client,int minW,int maxW)
    {	Rectangle vcr = new Rectangle();
    	boolean ok = placeRectangle(vcr, minW, minW/2, maxW, maxW/2, BoxAlignment.Edge,true);
    	if(ok)
    	{
    		client.SetupVcrRects(G.Left(vcr),G.Top(vcr),G.Width(vcr),G.Height(vcr));
    	}
    	return(ok);
    }
    public boolean placeTheChat(Rectangle chatRect, int minW,int minH,int maxW,int maxH)
    {	return(rects.placeInMainRectangle(chatRect,minW,minH,maxW,maxH,BoxAlignment.Edge,false,preferredAspectRatio));
    }
    public boolean placeRectangle(Rectangle targetRect,int minW,int minH,int maxW,int maxH,
			BoxAlignment align,boolean preserveAspectRatio)
    {	return (rects.placeInMainRectangle(targetRect,minW,minH,maxW,maxH,align,preserveAspectRatio,preferredAspectRatio));
    }
    public boolean placeRectangle(Rectangle targetRect,int minW,int minH,int maxW,int maxH,
    		int minW1,int minH1,int maxW1,int maxH1,
 			BoxAlignment align,boolean preserveAspectRatio)
     {	return (rects.placeInMainRectangle(targetRect,
    		 minW,minH,maxW,maxH,
    		 minW1,minH1,maxW1,maxH1,
    		 align,preserveAspectRatio,preferredAspectRatio));
     }
  
    public boolean placeRectangle(Rectangle targetRect,int minW,int minH,BoxAlignment align)
     {	return (rects.placeInMainRectangle(targetRect,minW,minH,minW,minH,align,true,preferredAspectRatio));
     }

     /**
     * place the chat and log rectangles.  If there's no chat, just place the log 
     * if there is a chat and the space is big enough for the log too, place both
     * together.  Otherwise just place the log separately.
     * @param chatRect
     * @param minChatW
     * @param minChatH
     * @param maxChatW
     * @param maxChatH
     * @param logRect
     * @param minLogW
     * @param minLogH
     * @param maxLogW
     * @param maxLogH
     * @return true if the rectangle was successfully placed
     */
    public boolean placeTheChatAndLog(Rectangle chatRect,int minChatW,int minChatH,int maxChatW,int maxChatH,Rectangle logRect,
    		int minLogW,int minLogH,int maxLogW,int maxLogH)
    {
    	placeTheChat(chatRect,minChatW,minChatH,maxChatW,maxChatH);
    	int actualChatW = G.Width(chatRect);
    	int actualChatH = G.Height(chatRect);
    	int marginSize = rects.marginSize;
    	int chatY = G.Top(chatRect);
    	int chatX = G.Left(chatRect);
    	if((actualChatH>=minLogH) && (actualChatW-minLogW-marginSize>=minChatW))
          {	 int logX = chatX+actualChatW-minLogW;
          	 G.SetRect(logRect, logX,chatY,minLogW,actualChatH);
          	 G.SetWidth(chatRect,actualChatW-minLogW-marginSize);
          	 return(true);
          }
    	  if(actualChatH-minLogH-marginSize>minChatH)
    	  {	// if we expanded the chat vertically, roll back and use
    		// some of the space for the log
    		int logH = minLogH + (actualChatH - (minLogH+minChatH+marginSize*2))/2;
    		int logY = chatY+actualChatH-logH;
    		// pull back space for the log rectangle, then allocate it normally.
    		// this might result in the log going where we don't expect it.
    		G.SetHeight(chatRect,logY-chatY-marginSize);
    		addToSpare(new Rectangle(chatX-marginSize,logY,actualChatW+marginSize*2,actualChatH+chatY+marginSize-logY));
    	  }
    	  
   	  return(placeRectangle(logRect,minLogW,minLogH,maxLogW,maxLogH,BoxAlignment.Edge,false));  
         
    }
    /** allocate the done,edit and a block of option rectangles as a group, then split it
     * into separate rectangles.  There can be any number of option rectangles, which
     * will be allocated below the "done" and "edit" rectangles. 
     * @param boxW	min width of the done and edit buttons
     * @param maxW  max width of the done and edit buttons
     * @param done	
     * @param edit
     * @param rep
     */
    public void placeUnplannedDoneEditRep(int boxW,int maxW,Rectangle done,Rectangle edit,Rectangle... rep)
    {		int nrep = rep.length;
    		if(nrep==0 || (rep[0]==null)) { placeDoneEdit(boxW,maxW,done,edit); }
    		else
    		{
    		int marginSize = rects.marginSize;
    		boolean hasButton = edit!=null || done!=null;
    		int buttonH1 = hasButton ? boxW/2+marginSize : 0;
    		int buttonH2 = hasButton ? maxW/2+marginSize : 0;
    		Rectangle r = new Rectangle();
    		int szw = 2*boxW+marginSize;
    		int szw2 = 2*maxW+marginSize;
    		int szh = buttonH1+nrep*boxW/3;
    		int szh2 = buttonH2+2*nrep*boxW/3;
    		placeRectangle(r,szw,szh,szw2,szh2,
    				BoxAlignment.Center,true);
    		int l = G.Left(r);
    		int t = G.Top(r);
    		int w = G.Width(r);
    		int h = G.Height(r);
    		int buttonW = (w-marginSize)/2;
    		int buttonH = hasButton ? buttonW/2 : 0;
      		if(done!=null) { G.SetRect(done,l,t,buttonW,buttonH); }
    		if(edit!=null) { G.SetRect(edit,l+w-buttonW,t,buttonW,buttonH); }
    		t += buttonH+marginSize;
    		int reph = (h-buttonH-marginSize)/nrep;
    		for(int i=0;i<nrep;i++)
    			{
    			G.SetRect(rep[i], l, t+i*reph,w,reph);
    			}
    		}
    }
    //
    // place done-edit-rep in a situation where the done rect doesn't need to be
    // placed, as is typically true for offline games.
    //
    public void placeDoneEditRep(int boxW,int maxW,Rectangle done,Rectangle edit,Rectangle... rep)
    {
    	if(plannedSeating()) 
    		{
    		  placeDoneEdit(boxW,maxW,done,edit);
    		  if(rep.length>0)
    		  {
    			  placeUnplannedDoneEditRep(boxW,maxW,null,null,rep);
    		  }
    		}
    	else
    	{
    		placeUnplannedDoneEditRep(boxW,maxW,done,edit,rep);
    	}
    }
    public void placeDoneEdit(int boxW,int maxW,Rectangle done,Rectangle edit)
    {		
    		if((done!=null) && (edit!=null) && plannedSeating() && !alwaysPlaceDone) { 
    			G.SetRect(done, 0,0,0,0);
    			int undoW = boxW*2/3;
    			placeRectangle(edit,undoW,undoW,undoW,undoW,BoxAlignment.Center,true);
    		} 
    		else
    		{
    	    if(done==null && (edit!=null)) { placeRectangle(edit,boxW,boxW/2,maxW,maxW/2,BoxAlignment.Center,true); }
    		if(edit==null && (done!=null)) { placeRectangle(done,boxW,boxW/2,maxW,maxW/2,BoxAlignment.Center,true); }
    		if(done!=null && edit!=null)
    		{
    		int marginSize = rects.marginSize;
    		int szw = boxW;
    		int szw2 = maxW;
    		int szh = boxW+marginSize;
    		int szh2 =maxW+marginSize;
    		placeRectangle(done,szw,szh,szw2,szh2,
    						szw*2+marginSize,szw/2,maxW*2+marginSize,maxW/2,
    						BoxAlignment.Center,true);
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
    		}}}
   }
    public void placeDrawGroup(FontMetrics fm,Rectangle acceptDraw,Rectangle declineDraw)
    {	InternationalStrings s = G.getTranslations();
    	int len1 = fm.stringWidth(s.get(OFFERDRAW));
    	int len2 = fm.stringWidth(s.get(ACCEPTDRAW));
    	int len3 = fm.stringWidth(s.get(DECLINEDRAW));
    	int h = fm.getHeight()*3;
    	int len = h+Math.max(len1, Math.max(len2, len3));
    	placeRectangle(acceptDraw,len,h*2,len,h*2,
    			len*2,h,len*2,h,
    			BoxAlignment.Center,true);
    	if(G.Width(acceptDraw)>len)
    	{
    		G.SetWidth(acceptDraw, len);
    		G.AlignTop(declineDraw, G.Left(acceptDraw)+len,acceptDraw);
    	}
    	else { 
    		G.SetHeight(acceptDraw, h);
    		G.AlignLeft(declineDraw, G.Top(acceptDraw)+h,acceptDraw);
    	}
    }
   
    /**
     * return true if the seating chart provides seats on at least 3 sides
     * @return true if seating is "around the table"
     */
    public boolean seatingAround() { return(selectedSeating.seatingAround()); }
    /**
      * @return true if the current seating chart provides seating across the table 
     */
    public boolean seatingFaceToFace() { return(selectedSeating.seatingFaceToFace()); };
    /**
     * @return true if one of the planned seating arrangements was chosen
     */
    public boolean plannedSeating() { return((seatingFaceToFace()||seatingAround())); }

}
