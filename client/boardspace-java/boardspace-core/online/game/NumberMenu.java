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

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import static java.lang.Math.atan2;
import java.util.Enumeration;
import java.util.Hashtable;
import lib.CellId;
import lib.Drawable;
import lib.DrawableImage;
import lib.EnumMenu;
import lib.G;
import lib.Graphics;
import lib.HitPoint;
import lib.IStack;
import lib.InternationalStrings;
import lib.Location;
import lib.LocationProvider;
import lib.MenuParentInterface;
import lib.NumberMenuHost;
import lib.PopupManager;
import lib.exCanvas;
/**
 * 
 * This class provides a numbering menu, intended to be placed on the stat line
 * or somewhere similar, and machinery to implement several numbering modes.
 * to draw and service the menu, call the "draw" method
 * 
 * The initial consumer of this class was Hive
 * 
 * To use this, 
 * 
 * cell class should implement PlacementProvider, and the cell's reInit and copyFrom
 * method should maintain the cells bookkeeping variables.  Minimally, these will maintain
 * the sequence number when the cell is filled, emptied, or captured.
 * 
 * board class should manipulate the cell's state variables, and a placementSequence.
 * In games with simple move structures, this could be the same as moveNumber.  In games
 * with multiple actions per turn, the sequence numbers will run ahead of move numbers.
 * 
 * the viewer class should allocate a NumberMenu item, and implement PlacementProvider.
 * the setLocalBounds method should position the numberMenu
 * if the move structure is complex, the execute class should call recordSequenceNumber
 * the redrawBoard method should call numberMenu.draw
 * the startDragging menu should call numberMenu.show
 * the handleDeferredEvents method should call numberMenu.selectMenu
 * the drawBoardElementsMethod should call numberMenu.clearSequenceNumbers and numberMenu.drawSequenceNumbers
 * and also for each cell, should call numberMenu.recordSequenceNumbers.
 * 
 * This handles all the major features except captured pieces.  For those, the board and cell classes
 * should remember the sequence and identity of the captured pieces, and the drawBoardElements method
 * should draw a representation of them inline.
 * 
 * 12/7/2025 made some simple changes to allow chains of locations with the same number.  
 * This is used by split/join moves in Truchet
 * 
 * @author ddyer
 *
 */
@SuppressWarnings("serial")
public class NumberMenu extends Rectangle {
	public String numberMarker = "\u26aa";	// hollow circle
		// another likely choice "\u2b24"; // filled circle
	public Drawable numberIcon = null;
	public boolean arrowNumberMarker = false;
	Drawable base = null;
	CellId id = null;
	String text = "#";
	String helpText = "Show move numbers";
	PopupManager menu = null;
	private NumberingMode selected = NumberingMode.None;
	public NumberingMode selected() { return selected; }
	NumberMenuHost selectedProvider = null;
	int startingNumber = 0;
	private boolean mouseIsOn = false;
	public boolean includePartialMoves = true;
	public boolean includeNumbers = true;	// if true, include the actual move number at the end of the arrow
	public double arrowOpacity = 0.7;		// opacity for arrows
    public double lineWidthMultiplier = 0.05;		// cellsize multiplier for line width of the arrow
    public double arrowOffsetMultiplier = 0.2;		// cellsize multiplier for the amount the point falls short of the arrow end target
    public double arrowWidthMultiplier = 0.25;		// cellsize multiplier for the width of the arrowhead
 
	
	/**
	 * NumberingMode encapsulates most of the behavior associated
	 * with choosing how move numbers are displayed on stones.  This
	 * enum also serves as a singleton class with a selected value
	 * with the ability to display a menu of choices.
	 */
	public enum NumberingMode implements EnumMenu
	{ None, All, Last, Last_2, Last_5, From_Here;
	  public String menuItem() { return(name().replace('_',' ')); }
	  public boolean useNumbers()
	  {	  // true if this mode should use explicit numbers 
		  return !(this==None||this==Last);
	  }
	}
	/**
	 * 
	 * @param on
	 * @param ic
	 * @param i
	 */
	public NumberMenu(NumberMenuHost provider,Drawable ic,CellId i) {
		base = ic;
		id = i;
		text = DrawableImage.NotHelpDraw + "#";
		selectedProvider = provider;
	}
	/**
	 * draw the menu icon
	 * 
	 * @param gc
	 * @param highlight
	 */
	public void draw(Graphics gc,exCanvas drawOn,HitPoint highlight)
	    {	int width = G.Width(this);
	    	mouseIsOn = false;
	    	if(base.draw(gc,drawOn,width,G.centerX(this),G.centerY(this),highlight,id,text))
	    	{	highlight.spriteRect = this;
	    		highlight.spriteColor = Color.red;
	    		mouseIsOn = true;
				highlight.setHelpText(G.getTranslations().get(helpText));
	    	}
	     }  
	/**
	 *  generate a pop-up menu of the choices.  This is normally invoked
	 *  in StopDragging when the menu icon is hit.  The menu itself will
	 *  be serviced in the canvas handleDeferredEvent method
	 */
	public void showMenu(MenuParentInterface drawOn,ActionListener listener)
	{
		if(menu==null) { menu=new PopupManager(); }
		menu.newPopupMenu(drawOn,listener);
		menu.show(G.Left(this),G.Top(this),NumberingMode.values());
	}
	/**
	 * called from the canvas handleDeferredEvent method, retrn true if an item
	 * in the viewing choices has been hit and handled
	 * 
	 * @param target
	 * @param p
	 * @return
	 */	
	public boolean selectMenu(Object target,NumberMenuHost p)
		{
		if(menu!=null)
		{	if(menu.selectMenuTarget(target))
			{
				doSelection((NumberingMode)menu.rawValue,p);
				menu = null;
				return(true);	// we handled it
				}
			}
			return(false);
		}
		
	private void doSelection(NumberingMode sel,NumberMenuHost p)
		{
		selected = sel;
		selectedProvider = p;
		switch(selected)
			{
			default: break;
			case None:
			case All:
				startingNumber = 0;
				break;
			case From_Here:	
				{
				startingNumber = p.getLastPlacement()-1;
				}
				break;
			}
		}
	public static void putStrings()
	{	
		InternationalStrings.put(NumberingMode.values());			
	}
	
	/**
	 * get the sequence number for cell c in the current display scheme.
	 * if "empty" is true, get it as the potential origin of a move,
	 * otherwise as a potential destination
	 * @param c
	 * @param empty
	 * @return a positive number which should be seen, or a negative number which should not be
	 */
	public int getSequenceNumber(PlacementProvider c,boolean empty)
	{	
		int number = c.getLastPlacement(empty);	// sequence number associated with c
		int v = getVisibleNumber(number);		// a number which will either be number or -1
		return v;
	}
	/** return the displayed number for this sequence number. Negative or zero probably
	 * is not intended to be displayed.
	 * @param number
	 * @return a positive number which should be seen, or a negative number which should not be
	 */
	public int getVisibleNumber(int number)
	{
		if(number>=0)
		{	NumberingMode choice = selected();
			if(mouseIsOn && choice==NumberingMode.None)
				{ choice = NumberingMode.Last;
				}
			switch(choice)
			{
			case None: return -1;
			case From_Here:
			case All:
				return number>startingNumber ? (number-startingNumber) : -1;
			case Last:
				startingNumber = startingSequenceNumber(-1);
				return number>=startingNumber ? number : -1;
				
			case Last_2:
				startingNumber = startingSequenceNumber(-2);
				return number>=startingNumber ? number : -1;
				
			case Last_5:
				startingNumber = startingSequenceNumber(-5);
				return number>=startingNumber ? number : -1;
			default:
				G.Error("Not handled");
			}
		}
		
		return -1;
	}

	private IStack sequenceNumbers = new IStack();
	private Hashtable <Integer,LocationProvider> sources = new Hashtable<Integer,LocationProvider>();
	private Hashtable <Integer,LocationProvider> dests = new Hashtable<Integer,LocationProvider>();
	private Hashtable <LocationProvider,PlacementProvider>reverse = new Hashtable<LocationProvider,PlacementProvider>();
	
	/** record the first sequence number associated with each move number
	 * index n in sequenceNumbers contains the lowest sequence number associated
	 * with that move number.  This is normally called from the canvas Execute
	 * method, but need not be called at all for games with a simple 1:1 correstondence
	 * between sequence numbers and move numbers
	 * 
	 * @param moveNumber0
	 */
	boolean newMoveNumber = false;
	public void recordSequenceNumber(int moveNumber)
	{	int sz = sequenceNumbers.size();
		int last = selectedProvider.getLastPlacement();
		while(sz>moveNumber+1) 		// the move number moved backwards, remove excess
			{ sequenceNumbers.pop(); sz--; 
			  newMoveNumber = true;
			}
		while(sz<moveNumber) 
			{ sequenceNumbers.push(last-1); sz++; 	// the move number jumped ahead
			  newMoveNumber = true;
			}
		if(sz==moveNumber) 							// on target next number
			{ sequenceNumbers.push(last); 
			  newMoveNumber = false;
			}
		if(sequenceNumbers.elementAt(moveNumber)>last)	// maybe replace the current with a lower sequence
			{ sequenceNumbers.setElementAt(moveNumber,last);
			  newMoveNumber = false;
			}
	}
	public int sequenceOffset = 0;
	public int currentSequenceNumber()
	{
		return Math.max(0,(sequenceNumbers.size()+((newMoveNumber||!includePartialMoves)? 0 : -1)));
	}
	/**
	 * 
	 * get the first sequence number corresponding to "back" moves ago
	 * "back" numbers ought to be negative.  This is only going to be called
	 * with a small value of "back", when displaying the last few moves
	 * @param back a small negative integer
	 */
	private int startingSequenceNumber(int back)
	{	int sz = currentSequenceNumber();
		if(sz>0)
		{int currentMoveNumber = sz+back;
		if(currentMoveNumber<=0) { return 0; }
		else if(currentMoveNumber>=sz) { return sequenceNumbers.top()+1; }
		else return sequenceNumbers.elementAt(currentMoveNumber); 
		}
		else
		{	// 1-1 mapping
			return selectedProvider.getLastPlacement()+back;
		}
	}
	/**
	 * when a sequence number is stored, this records the reverse mapping
	 * back to the PlacementProvider (ie: cell) that has that sequence number
	 * @param from
	 * @return
	 */
	public PlacementProvider getPlacement(LocationProvider from)
	{
		return reverse.get(from);
	}
	/**
	 * call at the beginning of a redisplay pass
	 */
	public void clearSequenceNumbers()
	{	sources.clear();
		dests.clear();
		reverse.clear();
	}
	
	/**
	 * lower level save of a source or destination position
	 * 
	 * @param seq
	 * @param empty
	 * @param x
	 * @param y
	 */
	public LocationProvider saveSequenceNumber(int seq,boolean empty,int x,int y)
	{	return saveSequenceNumber(seq,empty,x,y,null);
	}
	/**
	 * lower level save of a source or destination, and an associated color
	 * @param seq
	 * @param empty
	 * @param x
	 * @param y
	 * @param c
	 * @return
	 */
	public LocationProvider saveSequenceNumber(int seq,boolean empty,int x,int y,Color c)
	{
		Hashtable<Integer,LocationProvider> tbl = empty ? sources :dests;
		LocationProvider prev = tbl.get(seq);
		LocationProvider loc = makeLocation(x,y,c);
		loc.setNext(prev);
		tbl.put(seq,loc);
		return loc;
	}

	/**
	 * make a new location object, can be overridden.
	 * @param x
	 * @param y
	 * @param color
	 * @return
	 */
	public LocationProvider makeLocation(int x,int y,Color c)
	{
		return new Location(x,y,c);
	}
	/**
	 * call with a cell and its center position, to save it for drawing arrows and sequence numbers
	 * you can call this again if the cell has a stack that offsets the position of the top.
	 * @param cell
	 * @param xpos
	 * @param ypos
	 */
	public LocationProvider saveSequenceNumber(PlacementProvider cell,int xpos,int ypos)
	{
		return saveSequenceNumber(cell,xpos,ypos,null);
	}
	/**
	 * call with a cell and its center position, to save it for drawing arrows and sequence numbers
	 * you can call this again if the cell has a stack that offsets the position of the top.
	 * @param cell
	 * @param xpos
	 * @param ypos
	 * @param c
	 * @return
	 */
	public LocationProvider saveSequenceNumber(PlacementProvider cell,int xpos,int ypos,Color c)
	{
		int slabel = getSequenceNumber(cell,true);	
		LocationProvider sloc = null;
		if(slabel>=0) 
			{ 
			sloc = saveSequenceNumber(slabel,true,xpos,ypos,c); 
			reverse.put(sloc,cell);
    		}
		int dlabel = getSequenceNumber(cell,false);		
		if(dlabel>=0) 
	 	{
	 	  LocationProvider dloc = saveSequenceNumber(dlabel,false,xpos,ypos,null);
	 	  reverse.put(dloc,cell);
	 	  return dloc;
	 	}
		return sloc;
	}
	/**
	 * call this to draw the sequence numbers that have been saved in the process of drawing
	 * the board at large.   This version draws the numbers just beyond the tip of the arrow.
	 * 
	 * @param gc
	 * @param cellSize	used to scale the numbers and the arrowhead
	 * @param pieceLabelFont the font for drawing the label
	 * @param labelColor the color for drawing the label and arrows
	 */
    public void drawSequenceNumbers(Graphics gc,int cellSize,Font pieceLabelFont,Color labelColor)
    {
    	drawSequenceNumbers(gc,cellSize,pieceLabelFont,labelColor,0);
    }
    /**
     * call this to draw the sequence numbers that have been saved in the process of drawing
	 * the board at large.   This version draws the numbers just beyond the tip of the arrow
	 * if labelPosition is zero, but slides it down the shaft of the array with small positive
	 * values.
     * @param gc
     * @param cellSize
     * @param pieceLabelFont
     * @param labelColor
     * @param labelPosition
     */
    public void drawSequenceNumbers(Graphics gc,int cellSize,Font pieceLabelFont,Color labelColor,double labelPosition)
    {	  

    	for(Enumeration<Integer>destindex = dests.keys(); destindex.hasMoreElements();)
    	{	int idx = destindex.nextElement();
    		
       		LocationProvider dest0 = dests.get(idx);
       		LocationProvider src0 = sources.get(idx);
       		LocationProvider src = src0;
       		while(src!=null)
     			{
        		LocationProvider dest = dest0;
	    		while(dest!=null)
	    			{
	    			drawArrow(gc,src,dest,labelColor,cellSize);  
	    			dest = dest.getNext();
	    			}
	    		src = src.getNext();
     			}
        	drawNumber(gc,cellSize,src0,dest0,labelPosition,pieceLabelFont,labelColor,idx); 
    		}
    	
     	for(Enumeration<Integer>srcindex = sources.keys(); srcindex.hasMoreElements();)
    	{
    		int idx = srcindex.nextElement();
     		LocationProvider dest = dests.get(idx);
    		if(dest==null)
    		{	
    	   		LocationProvider src = sources.get(idx);
    	   		while(src!=null)
    			{
    			drawNumber(gc,cellSize,src,pieceLabelFont,labelColor,idx);
    			src = src.getNext();
    			}
    		}
    	}
    }
    /**
     * convert a sequence number to the corresponding move number.
     * for simple games where there is only one action per move,
     * this will be trivial.
     * @param number
     * @return
     */
    public String moveNumberString(int number)
    {	int sidx = sequenceNumbers.size()-1;
    	if(sidx>0)
    	{
    	int low = 1;
    	int high = sidx;
    	while(high-low>0)
    	{	// binary search for the boundary
    		int mid = (low+high)/2;
    		int target = sequenceNumbers.elementAt(mid);
    		if(target==number) { low = high = mid;  }
    		else if(target>number) { high = mid; }
     		else if(low==mid)
     			{ 
     			if(sequenceNumbers.elementAt(high)>number) { high=low; } else { low++; }
     			}
     		else {  low = mid;	}
    	}
    	return ""+(low)/*+"("+number+")"*/; 
    	/*
    	for(int idx=0;idx<=sidx;idx++)
    		{
    		if(sequenceNumbers.elementAt(idx)>number)
    		{
    			return ""+(idx-1) /* +("("+number+")") 
    		}
     	return ""+(sidx);
        */
    	}
    	else { return ""+number; }	// no sequence number mapping
    }
  /**
   * draw a number (nominally, a move number) associated with a location
   * this draws the numbers just beyond the point of the arrow.
   * 
   * @param gc
   * @param cellSize
   * @param dest
   * @param pieceLabelFont
   * @param color
   * @param idx
   */
    public void drawNumber(Graphics gc,int cellSize,LocationProvider dest,Font pieceLabelFont,Color color,int idx)
    {	if(includeNumbers)
    	{
    	boolean numbers = selected().useNumbers();
    	if(!numbers && numberIcon!=null)
    	{
        	selectedProvider.drawNumber(gc,null,reverse.get(dest),
        			cellSize,dest.getX(),dest.getY(),
        			pieceLabelFont,color, numberIcon);		
    	}
    	else
    	{
    	String m = numbers ? moveNumberString(idx) : numberMarker;
    	selectedProvider.drawNumber(gc,null,reverse.get(dest),
    			cellSize,dest.getX(),dest.getY(),
    			pieceLabelFont,color, m);
    	}}
     }
   
    /**
     * draw a number (nominally, a move number) associated with a location, somewhere
     * along the line defined by the arrow from src to dest, offset from the dest
     * by a percentage of the cell size.  Zero draws it just beyond the point.
     * 
     * @param gc
     * @param cellSize
     * @param src
     * @param dest
     * @param position
     * @param pieceLabelFont
     * @param color
     * @param idx
     */
    public void drawNumber(Graphics gc,int cellSize,LocationProvider src,LocationProvider dest,double position,Font pieceLabelFont,Color color,int idx)
    {
    			
		int x1 = src==null ? dest.getX() : src.getX();
		int y1 = src==null ? dest.getY() : src.getY();
		int x2 = dest.getX();
		int y2 = dest.getY();
		// a little basic trigonometry.  We want to shorten the 
		// arrow by a fraction of the cell size, both at the beginning
		// and the end, so the arrows don't overlap with the numbers.
		double dx = x1-x2;
		double dy = y1-y2;
		double angle = atan2(dx,dy);
		double sin = Math.sin(angle);
		double cos = Math.cos(angle);
		double sin2 = Math.sin(angle+Math.PI/2);
		double cos2 = Math.cos(angle+Math.PI/2);
		int xpos = (int)(x2+sin*cellSize*position+sin2*cellSize*lineWidthMultiplier);
		int ypos = (int)(y2+cos*cellSize*position+cos2*cellSize*lineWidthMultiplier);
		if(includeNumbers)
		{
		 	  /** call back to the host window for actual drawing.  This allows the 
		  	   * the host to override or embellish the default behavior
		  	   */
		   	boolean numbers = selected().useNumbers();
	    	if(!numbers && numberIcon!=null && src==null)
	    	{
	        	selectedProvider.drawNumber(gc,null,reverse.get(dest),
	        			cellSize,xpos,ypos,
	        			pieceLabelFont,color, numberIcon);		
	    	}
	    	else
	    	{
	    	String m = numbers ? moveNumberString(idx) : src==null ? numberMarker : "";
	    	PlacementProvider d = reverse.get(dest);
			PlacementProvider s = src==null ? null : reverse.get(src);
			selectedProvider.drawNumber(gc,s,d,
						cellSize,xpos,ypos,
						pieceLabelFont,color, m);
	    	}
		}
	
		
    }
    /**
     *  draw the arrow from src to dest 
     * @param gc
     * @param sxpos
     * @param sypos
     * @param dxp
     * @param dyp
     * @param labelColor
     * @param cellSize
     */
    public void drawArrow(Graphics gc,LocationProvider src,LocationProvider dest,Color defaultColor,int cellSize)
    {	
		double linew = cellSize*lineWidthMultiplier;
		double shorten = cellSize*arrowOffsetMultiplier;

		
		int x1 = src.getX();
		int y1 = src.getY();
		int x2 = dest.getX();
		int y2 = dest.getY();
		// a little basic trigonometry.  We want to shorten the 
		// arrow by a fraction of the cell size, both at the beginning
		// and the end, so the arrows don't overlap with the numbers.
		double dx = x1-x2;
		double dy = y1-y2;
		double angle = atan2(dx,dy);
		double sin = Math.sin(angle);
		double cos = Math.cos(angle);
		// we also want to offset the arrow slightly, perpendicular to its direction,
		// so that motion-reverse will display two distinct arrows instead
		// of one double header
		double sin2 = Math.sin(angle+Math.PI/2);
		double cos2 = Math.cos(angle+Math.PI/2);
		double cx = (sin*shorten);
		double cy = (cos*shorten);
		double lx = (linew*sin2);
		double ly = (linew*cos2);
		
		Color lc = src.getColor();
		Color color = lc==null ? defaultColor : lc;
	 	
		PlacementProvider s = reverse.get(src);
		PlacementProvider d = reverse.get(dest);
		if(s!=d)
		{
		/** call back to the host window for actual drawing.  This allows the 
	  	   * the host to override or embellish the default behavior
	  	   */
		selectedProvider.drawArrow(gc,s,d,
				(int)(x1-cx+lx),(int)(y1-cy+ly),(int)(x2+cx+lx),(int)(y2+cy+ly),
				color,arrowOpacity,
				(int)(cellSize*arrowWidthMultiplier),linew);
		}
    }
}
