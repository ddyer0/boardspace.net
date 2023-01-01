package online.game;

import java.awt.Color;
import java.awt.Rectangle;

/**
 * this provides a menu of annotations such as squares, arrows, and triangles which can be added to the current move of a game.
 * 
 * As additions to the commonMove structure, they will be saved to the game record.
 * Eventually, they will be shared in real time among the viewers of the game.
 * 
 * they should be asynchronous, some special considerations are needed for the incremental game record maintenance.
 * another special consideration is how they will appear to players in tournament games.  Probably should be censored out.
 * 
 * the annotations are expected to be associated with the board portion of the display,
 * and their location will be recorded as fractions of the board width and height.
 * 
 * TODO: save the annotations to the game record and print them to the sgf files
 * TODO: transmit annotations in real time.
 */


import lib.CellId;
import lib.DrawableImage;
import lib.G;
import lib.Graphics;
import lib.HitPoint;
import lib.PopupManager;
import lib.StackIterator;
import lib.StockArt;
import lib.TextGlyph;

@SuppressWarnings("serial")
public class AnnotationMenu extends Rectangle {

	public enum Annotation
	{	// later. include freehand drawing and ad-hoc arrows
		Triangle(StockArt.Triangle),
		Square(StockArt.Square),
		Left(StockArt.SolidLeftArrow),
		Right(StockArt.SolidRightArrow),
		Up(StockArt.SolidUpArrow),
		Down(StockArt.SolidDownArrow);
		public StockArt chip;
		Annotation(StockArt c) { chip = c; }
	}
	private DrawableImage<?> base = null;
	private PopupManager menu = null;
	private commonCanvas drawOn=null;
	private CellId id = null;
	private String text = null;
	private Annotation selected = null;
	public Annotation getSelected() { return selected; }
	public void setSelected(Annotation sel) { selected = sel; }
	String helpText = "Add an Annotation";
	/**
	 * 
	 * @param on
	 * @param ic
	 * @param i
	 */
	public AnnotationMenu(commonCanvas on,CellId i) 
	{
		drawOn = on;
		base = StockArt.Triangle;
		id = i;
	}
	
	/**
	 * draw the menu icon.  Normally this will be an item on the status bar
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
		for(Annotation a : Annotation.values())
			{ menu.addMenuItem(
					TextGlyph.create("xxxxx",a.name(),a.chip,drawOn,2.0,1.0,0,-0.25),
					a);
			}
		menu.show(G.Left(this),G.Top(this));
	}
		
	/**
	 * called from the canvas handleDeferredEvent method, return true if an item
	 * in the viewing choices has been hit and handled
	 * 
	 * @param target
	 * @param p
	 * @return
	 */	
	public boolean selectMenu(Object target)
		{
		if(menu!=null)
		{	if(menu.selectMenuTarget(target))
			{	selected = (Annotation)menu.rawValue;
				base = selected.chip;
				menu = null;
				return(true);	// we handled it
				}
			}
			return(false);
		}
	/**
	 * draw an annotation at a particular size and location
	 * @param gc
	 * @param an
	 * @param size
	 * @param x
	 * @param y
	 */
	public void drawAnnotation(Graphics gc,Annotation an,int size,int x,int y)
	{
		an.chip.drawChip(gc,drawOn,size,x,y,null);
	}
	/**
	 * save an annotation on a move. The supplied x,y are absolute, but
	 * they'll be recorded as relative to the reference rectangle.
	 * 
	 * @param m		the move in the game history to add the annotation
	 * @param an	the annotation
	 * @param ref	normally the board rectangle
	 * @param x		the absolute x of the annotation
	 * @param y		the absolute x of the annotation
	 */
	public void saveAnnotation(commonMove m,Annotation an,Rectangle ref,int x,int y)
	{
		StackIterator<MoveAnnotation> ann = m.getAnnotations();
		double dx = (double)(x-G.Left(ref))/G.Width(ref);
		double dy = (double)(y-G.Top(ref))/G.Height(ref);
		MoveAnnotation n = new MoveAnnotation(an,dx,dy);
		// annotations are expected to be rare, so we use the StackIterator protocol to treat
		// singletons and lists of annotations uniformly
		if(ann==null) { ann = n; } else { ann = ann.push(n); }
		m.setAnnotations(ann);
	}
	/**
	 * clear the annotations for a move
	 * 
	 * @param m
	 */
	public void clearAnnotations(commonMove m)
	{
		m.setAnnotations(null);
	}
	/**
	 * draw all the annotations associated with this move
	 * 
	 * @param gc
	 * @param m
	 * @param ref
	 * @param size
	 */
	public void drawSavedAnimations(Graphics gc, commonMove m,Rectangle ref,int size) {
		StackIterator<MoveAnnotation> ann = m.getAnnotations();
		if(ann!=null)
		{	int l = G.Left(ref);
			int t = G.Top(ref);
			int w = G.Width(ref);
			int h = G.Height(ref);
			for(int i=0;i<ann.size();i++)
			{
				MoveAnnotation ta = ann.elementAt(i);
				drawAnnotation(gc,ta.annotation,size,(int)(l+ta.xPos*w),(int)(t+ta.yPos*h));
			}
		}		
	}
	public boolean StopDragging(HitPoint hp,commonMove cm,Rectangle ref) {
		boolean used = false;
		if(selected!=null)
		{
		if(G.pointInRect(hp,ref))
		{
			saveAnnotation(cm,selected,ref,G.Left(hp),G.Top(hp));
		}
		setSelected(null);
		used = true;
		}
		return used;
		
	}
	
}
