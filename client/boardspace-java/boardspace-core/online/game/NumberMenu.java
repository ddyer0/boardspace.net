package online.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Enumeration;
import java.util.Hashtable;

import lib.CellId;
import lib.DrawableImage;
import lib.G;
import lib.GC;
import lib.Graphics;
import lib.HitPoint;
import lib.InternationalStrings;
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
 * To use this, both the viewer and the cell should implement the PlacementProvider interface,
 * which returns the highest sequence number allocated to the cell or viewer.
 * The viewer's DrawBoardElements method should call clearSequenceNumbers, then
 * call saveSequenceNumber for each cell that may be the source or destination of
 * a placement or movement.
 * 
 * Finally, the DrawBoardElements method should call drawSequenceNumbers.
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
	NumberingMode selected = NumberingMode.None;
	PlacementProvider selectedProvider = null;
	int startingNumber = 0;
	double arrowOpacity = 0.7;		// opacity for arrows
	
	/**
	 * NumberingMode encapsulates most of the behavior associated
	 * with choosing how move numbers are displayed on stones.  This
	 * enum also serves as a singleton class with a selected value
	 * with the ability to display a menu of choices.
	 */
	enum NumberingMode implements NameProvider
	{ None, All, Last, Last_2, Last_5, From_Here;
	  public String getName() { return(toString().replace('_',' ')); }
	}

	public NumberMenu(commonCanvas on,DrawableImage<?> ic,CellId i) {
		drawOn = on;
		base = ic;
		id = i;
		text = "#";
	}

	public void draw(Graphics gc,HitPoint highlight)
	    {	int width = G.Width(this);
	    	if(base.drawChip(gc,drawOn,highlight,id,width,G.centerX(this),G.centerY(this),"#"))
	    	{	highlight.spriteRect = this;
	    		highlight.spriteColor = Color.red;
				highlight.setHelpText(G.getTranslations().get(helpText));
	    	}
	     }  
	// generate a pop-up menu of the choices
	public void showMenu()
	{
		if(menu==null) { menu=new PopupManager(); }
		menu.newPopupMenu(drawOn,drawOn.deferredEvents);
		menu.addMenuItem(NumberingMode.values());
		menu.show(G.Left(this),G.Top(this));
	}

	// handle the user clicking on one of the choices
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
		
	void doSelection(NumberingMode sel,PlacementProvider p)
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
	 * @return
	 */
	public int getSequenceNumber(PlacementProvider c,boolean empty)
	{	
		int number = c.getLastPlacement(empty);
		if(number>0)
		{
			switch(selected)
			{
			case None: return -1;
			case From_Here:
			case All:
				return number>startingNumber ? (number-startingNumber) : -1;
			case Last:
				startingNumber = selectedProvider.getLastPlacement(false) - 2;
				return number>startingNumber ? number : -1;
				
			case Last_2:
				startingNumber = selectedProvider.getLastPlacement(false) - 3;
				return number>startingNumber ? number : -1;
				
			case Last_5:
				startingNumber = selectedProvider.getLastPlacement(false) - 6;
				return number>startingNumber ? number : -1;
			default:
				G.Error("Not handled");
			}
		}
		
		return -1;
	}

	private Hashtable <Integer,Point> sources = new Hashtable<Integer,Point>();
	private Hashtable <Integer,Point> dests = new Hashtable<Integer,Point>();
	
	/**
	 * call at the beginning of a redisplay pass
	 */
	public void clearSequenceNumbers()
	{	sources.clear();
		dests.clear();
	}
	/**
	 * lower level save of a source or destination position
	 * 
	 * @param seq
	 * @param empty
	 * @param x
	 * @param y
	 */
	public void saveSequenceNumber(int seq,boolean empty,int x,int y)
	{
		Hashtable<Integer,Point> tbl = empty ? sources :dests;
		tbl.put(seq,new Point(x,y));
	}
	/**
	 * call with a cell and its center position, to save it for drawing arrows and sequence numbers
	 * you can call this again if the cell has a stack that offsets the position of the top.
	 * @param cell
	 * @param xpos
	 * @param ypos
	 */
	public void saveSequenceNumber(PlacementProvider cell,int xpos,int ypos)
	{
		int slabel = getSequenceNumber(cell,true);	
		if(slabel>0) 
			{ 
			saveSequenceNumber(slabel,true,xpos,ypos); 
    		}
		int dlabel = getSequenceNumber(cell,false);		
		if(dlabel>0) 
	 	{
	 	  saveSequenceNumber(dlabel,false,xpos,ypos);
	 	}
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
    		Point src = sources.get(idx);
    		Point dest = dests.get(idx);
  
    		if(src!=null)
    		{
    		int sxpos = G.Left(src);
    		int sypos = G.Top(src);
    		if(dest!=null)
    		{
    		int dxp = G.Left(dest);
        	int dyp = G.Top(dest);
    		GC.setColor(gc,labelColor);
    		GC.setOpacity(gc,arrowOpacity);
         	GC.drawArrow(gc,sxpos,sypos,dxp,dyp,cellSize/4,cellSize/20.0);
         	GC.setOpacity(gc,1);
    		}}
    		if(dest!=null)
    		{
    		int dxp = G.Left(dest);
           	int dyp = G.Top(dest);
    		GC.setFont(gc,pieceLabelFont);
         	GC.drawOutlinedText(gc,true,dxp-cellSize/2,dyp-cellSize/2,cellSize,cellSize,labelColor,Color.black,""+idx);
    		} 
    	}  		
    }

}
