package online.game;


import java.util.Enumeration;
import java.util.Hashtable;

import com.codename1.ui.Font;
import com.codename1.ui.geom.Rectangle;
import static com.codename1.util.MathUtil.atan2;

import bridge.Color;
import lib.CellId;
import lib.DrawableImage;
import lib.G;
import lib.GC;
import lib.Graphics;
import lib.HitPoint;
import lib.IStack;
import lib.InternationalStrings;
import lib.Location;
import lib.LocationProvider;
import lib.NameProvider;
import lib.PopupManager;
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
 * method should maintain the cells bookeeping variables.  Minimally, these will maintain
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
 * @author ddyer
 *
 */
@SuppressWarnings("serial")
public class NumberMenu extends Rectangle {

	DrawableImage<?> base = null;
	CellId id = null;
	String text = "#";
	String helpText = "Show move numbers";
	commonCanvas drawOn=null;
	PopupManager menu = null;
	private NumberingMode selected = NumberingMode.None;
	public NumberingMode selected() { return selected; }
	PlacementProvider selectedProvider = null;
	int startingNumber = 0;
	double arrowOpacity = 0.7;		// opacity for arrows
	
	/**
	 * NumberingMode encapsulates most of the behavior associated
	 * with choosing how move numbers are displayed on stones.  This
	 * enum also serves as a singleton class with a selected value
	 * with the ability to display a menu of choices.
	 */
	public enum NumberingMode implements NameProvider
	{ None, All, Last, Last_2, Last_5, From_Here;
	  public String getName() { return(toString().replace('_',' ')); }
	}
	/**
	 * 
	 * @param on
	 * @param ic
	 * @param i
	 */
	public NumberMenu(commonCanvas on,DrawableImage<?> ic,CellId i) {
		drawOn = on;
		base = ic;
		id = i;
		text = "#";
		selectedProvider = (PlacementProvider)on;
	}
	/**
	 * draw the menu icon
	 * 
	 * @param gc
	 * @param highlight
	 */
	public void draw(Graphics gc,HitPoint highlight)
	    {	int width = G.Width(this);
	    	if(base.drawChip(gc,drawOn,highlight,id,width,G.centerX(this),G.centerY(this),text))
	    	{	highlight.spriteRect = this;
	    		highlight.spriteColor = Color.red;
				highlight.setHelpText(G.getTranslations().get(helpText));
	    	}
	     }  
	/**
	 *  generate a pop-up menu of the choices.  This is normally invoked
	 *  in StopDragging when the menu icon is hit.  The menu itself will
	 *  be serviced in the canvas handleDeferredEvent method
	 */
	public void showMenu()
	{
		if(menu==null) { menu=new PopupManager(); }
		menu.newPopupMenu(drawOn,drawOn.deferredEvents);
		menu.addMenuItem(NumberingMode.values());
		menu.show(G.Left(this),G.Top(this));
	}
	/**
	 * called from the canvas handleDeferredEvent method, retrn true if an item
	 * in the viewing choices has been hit and handled
	 * 
	 * @param target
	 * @param p
	 * @return
	 */
	public boolean selectMenu(Object target,PlacementProvider p)
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
		
	private void doSelection(NumberingMode sel,PlacementProvider p)
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
				startingNumber = p.getLastPlacement(false)-1;
				}
				break;
			}
		}
	public static void putStrings()
	{	
		InternationalStrings.put(G.getNames(NumberingMode.values()));			
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
		{
			switch(selected)
			{
			case None: return -1;
			case From_Here:
			case All:
				return number>=startingNumber ? (number-startingNumber) : -1;
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
	public void recordSequenceNumber(int moveNumber)
	{	int sz = sequenceNumbers.size();
		int last = selectedProvider.getLastPlacement(false);
		while(sz>moveNumber+1) 		// the move number moved backwards, remove excess
			{ sequenceNumbers.pop(); sz--; 
			}
		while(sz<moveNumber) 
			{ sequenceNumbers.push(last-1); sz++; 	// the move number jumped ahead
			}
		if(sz==moveNumber) 							// on target next number
			{ sequenceNumbers.push(last); 
			}
		if(sequenceNumbers.elementAt(moveNumber)>last)	// maybe replace the current with a lower sequence
			{ sequenceNumbers.setElementAt(moveNumber,last);
			}
	}
	/**
	 * 
	 * get the first sequence number corresponding to "back" moves ago
	 * "back" numbers ought to be negative.  This is only going to be called
	 * with a small value of "back", when displaying the last few moves
	 * @param back a small negative integer
	 */
	private int startingSequenceNumber(int back)
	{	int sz = sequenceNumbers.size();
		if(sz>0)
		{int currentMoveNumber = sequenceNumbers.size()+back;
		if(currentMoveNumber<=0) { return 0; }
		else if(currentMoveNumber>=sequenceNumbers.size()) { return sequenceNumbers.top()+1; }
		else return sequenceNumbers.elementAt(currentMoveNumber); 
		}
		else
		{	// 1-1 mapping
			return selectedProvider.getLastPlacement(false)+back;
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
		LocationProvider loc = makeLocation(x,y,c);
		tbl.put(seq,loc);
		return loc;
	}
	/**
	 * make a new location object, can be overridden.
	 * @param x
	 * @param y
	 * @return
	 */
	public LocationProvider makeLocation(int x,int y)
	{
		return makeLocation(x,y,null);
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
	 * the board at large.   
	 * @param gc
	 * @param cellSize	used to scale the numbers and the arrowhead
	 * @param pieceLabelFont the font for drawing the label
	 * @param labelColor the color for drawing the label and arrows
	 */
    public void drawSequenceNumbers(Graphics gc,int cellSize,Font pieceLabelFont,Color labelColor)
    {	  

    	for(Enumeration<Integer>destindex = dests.keys(); destindex.hasMoreElements();)
    	{	int idx = destindex.nextElement();
    		LocationProvider src = sources.get(idx);
    		LocationProvider dest = dests.get(idx);
       		if(dest!=null)
    		{
           	drawNumber(gc,cellSize,dest,pieceLabelFont,labelColor,idx);
  
    		if(src!=null)
    		{
        	drawArrow(gc,src,dest,labelColor,cellSize);   		
    		}}
 
     	} 
    	for(Enumeration<Integer>srcindex = sources.keys(); srcindex.hasMoreElements();)
    	{
    		int idx = srcindex.nextElement();
    		LocationProvider src = sources.get(idx);
    		LocationProvider dest = dests.get(idx);
    		if(dest==null)
    		{
    			drawNumber(gc,cellSize,src,pieceLabelFont,labelColor,idx);
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
 *     
 * @param gc
 * @param cellSize
 * @param dest
 * @param pieceLabelFont
 * @param color
 * @param idx
 */
    public void drawNumber(Graphics gc,int cellSize,LocationProvider dest,Font pieceLabelFont,Color color,int idx)
    {
   		GC.setFont(gc,pieceLabelFont);
     	GC.drawOutlinedText(gc,true,dest.getX()-cellSize/2,dest.getY()-cellSize/2,cellSize,cellSize,color,Color.black,
     			moveNumberString(idx));
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
    {	Color lc = src.getColor();
	    GC.setColor(gc,lc==null ? defaultColor : lc);
		GC.setOpacity(gc,arrowOpacity);
		int x1 = src.getX();
		int y1 = src.getY();
		int x2 = dest.getX();
		int y2 = dest.getY();
		double linew = cellSize/20.0;
		double shorten = cellSize/5.0;
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
	 	GC.drawArrow(gc,(int)(x1-cx+lx),(int)(y1-cy+ly),(int)(x2+cx+lx),(int)(y2+cy+ly),cellSize/4,linew);
	 	GC.setOpacity(gc,1);
    }
}
