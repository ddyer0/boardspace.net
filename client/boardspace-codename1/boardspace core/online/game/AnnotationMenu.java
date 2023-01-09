package online.game;

import com.codename1.ui.geom.Point;
import com.codename1.ui.geom.Rectangle;
import bridge.Color;

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
 */


import lib.CellId;
import lib.DrawableImage;
import lib.G;
import lib.Graphics;
import lib.HitPoint;
import lib.NetConn;
import lib.PopupManager;
import lib.StackIterator;
import lib.StockArt;
import lib.StringStack;
import lib.TextGlyph;
import lib.Tokenizer;
import online.common.OnlineConstants;

@SuppressWarnings("serial")
public class AnnotationMenu extends Rectangle implements PlayConstants,OnlineConstants
{
	static final int ANNOTATION_TRACKING_OFFSET = 100;
	static final String ANNOTATION_TAG = "A1";
	public enum Annotation
	{	// later. include freehand drawing and ad-hoc arrows
		Triangle(StockArt.Triangle,0),
		Square(StockArt.Square,1),
		Ex(StockArt.Exmark,6),
		Left(StockArt.SolidLeftArrow,2),
		Right(StockArt.SolidRightArrow,3),
		Up(StockArt.SolidUpArrow,4),
		Down(StockArt.SolidDownArrow,5),
		Clear(null,7);
		int index;
		public StockArt chip;
		Annotation(StockArt c,int ind) { chip = c; index = ind; }
		public int index() { return index; }
		static public Annotation find(int n)
		{	for(Annotation a : values()) { if(a.index==n) { return a; }}
			return null;
		}
	}
	private DrawableImage<?> base = null;
	private PopupManager menu = null;
	private commonCanvas drawOn=null;
	private CellId id = null;
	private String text = null;
	private Annotation selected = null;
	public Annotation getSelected() { return selected; }
	private StringStack annotationActions = new StringStack();
	private StringStack annotationActionsHistory = new StringStack();
	
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
	    	if( base.drawChip(gc,drawOn,highlight,id,width,G.centerX(this),G.centerY(this),text))
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
					TextGlyph.create(a.name(),"xxxxx",a.chip,drawOn,2.0,1.0,0,-0.25),
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
				if(selected==Annotation.Clear)
				{	commonMove cm = drawOn.getCurrentMove();
					cm.setAnnotations(null);
					saveAnnotation(cm ,selected,0,0);
					selected = null;
				}
				else
				{
				base = selected.chip;
				menu = null;
				}
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
		if(an.chip!=null) { an.chip.drawChip(gc,drawOn,size,x,y,null); }
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
	public void saveAnnotation(commonMove m,Annotation an,int x,int y)
	{	Point pt = new Point();
		String zone = drawOn.encodeScreenZone(x, y,pt); 
		MoveAnnotation n = new MoveAnnotation(an,zone,G.Left(pt),G.Top(pt));
		m.addAnnotation(n);
		annotationActions.push(G.concat(ANNOTATION_TAG," ",m.playerString()," \"",m.longMoveString(),"\" ",
				"\"",n.toReadableString(),"\""));
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
	public void drawSavedAnimations(Graphics gc, commonMove m,int size) {
		StackIterator<MoveAnnotation> ann = m.getAnnotations();
		if(ann!=null)
		{	
			for(int i=0;i<ann.size();i++)
			{
				MoveAnnotation ta = ann.elementAt(i);
				Point mp = drawOn.decodeScreenZone(ta.zone,ta.xPos,ta.yPos);
		        int xp = G.Left(mp);
		        int yp = G.Top(mp);
				drawAnnotation(gc,ta.annotation,size,xp,yp);
			}
		}		
	}
	
	/**
	 * handle when the player is dragging an annotation around and clicks.
	 * we only expect clicks on the board. others are discarded.
	 * 
	 * @param hp
	 * @param cm
	 * @param ref
	 * @return
	 */
	public boolean saveCurrentAnnotation(HitPoint hp,Rectangle ref,commonMove cm) 
	{
		boolean used = false;
		if(selected!=null)
		{
		saveAnnotation(cm,selected,G.Left(hp),G.Top(hp));
		setSelected(null);
		used = true;
		}
		return used;
		
	}
	
	private Annotation sharedAnnotation = null;
	private String sharedAnnotationString = null;
	private long sharedAnnotationTime = 0;
	/**
	 * retrieve the current annotation tracking message, if any.  The same
	 * messages are used to track annotations being dragged around by some
	 * players' mouse and for just the mouse or the mouse dragging a piece.
	 * 
	 * @return
	 */
	public String mouseMessage() 
	{ 
	 if(annotationActions.size()>0)
		{	// these are permanantly placed annotations
			String aa = annotationActions.remove(0,true);
			annotationActionsHistory.push(aa);
			return (G.concat(NetConn.SEND_GROUP,KEYWORD_SPARE,aa));
		}
	  // this is a floating annotation being dragged around
	  String mm = sharedAnnotationString; sharedAnnotationString = null; 
	  return(mm); 
	}

	/**
	 * record a new tracking message if appropriate. To differentiate our tracking from regular
	 * "picked object" tracking, we encode the annotation icon as a negative, and add ANNOTATION_TAG
	 * to the arguments.   
	 * TRANSITION_PLAN these annotation tracking messages ought to be ignored by out of date clients.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	//
	public boolean trackMouse(int x, int y)
	{	
		if((selected!=null) && drawOn.canTrackMouse())
		{
		long now = G.Date();
		if( (sharedAnnotation!=selected)
				|| (now>sharedAnnotationTime+250))
			{
			sharedAnnotationTime = now;
			sharedAnnotation = selected;
			Point pt = new Point(0,0);
			String zone = drawOn.encodeScreenZone(x, y,pt); // not raw coordinates but normalized
            if (zone != null)
            	{  
               int closestX = G.Left(pt);
               int closestY = G.Top(pt);       
               sharedAnnotationString = G.concat(NetConn.SEND_GROUP,KEYWORD_TRACKMOUSE," ",
            		   zone," ",
            		   closestX , " " ,
            		   closestY, " " ,
            		   -(sharedAnnotation.index()+ANNOTATION_TRACKING_OFFSET)," ",
            		   ANNOTATION_TAG);
            	}
			}
		return true;
	}
	return false;
	}
	/**
	 * this draws the "other player's" annotation that is in the process
	 * of being placed.  These annotations-in-motion are transmitted using
	 * an extension of the player mouse position protocol.  To avoid any
	 * conflict with obsolete clients, the annotation id is encoded as a
	 * negative number.
	 * 
	 * @param g
	 * @param obj
	 * @param xp
	 * @param yp
	 * @param mode
	 * @param size
	 */
	public void drawAnimationSprite(Graphics g, int obj, int xp, int yp, String mode,int size) {
		if(ANNOTATION_TAG.equals(mode) && (Opcodes.NothingMoving!=obj))
		{
			int ann = -obj - ANNOTATION_TRACKING_OFFSET;
			Annotation a = Annotation.find(ann);
			if(a!=null) { drawAnnotation(g,a,size,xp,yp); }
			else if(G.debug()) { G.Error("No annotation for %s",obj); }
		}
	}
	/**
	 * this processes annotation placement messages from other players.  When the player places an annotation,
	 * a "SPAREA1" message is generated to the other players so they will place one too.  Which gets dispatched
	 * to here to be performed.
	 * 
	 * @param cmdStr
	 * @param fullMessage
	 * @return
	 */
	public boolean processSpareMessage(String cmdStr, String fullMessage) {
		if(cmdStr.equals(KEYWORD_SPARE+ANNOTATION_TAG))
		{
		int ind = fullMessage.indexOf(cmdStr);
		Tokenizer tok = new Tokenizer(fullMessage.substring(ind+cmdStr.length()));
		
		// general form is SPAREA1 P0 "move spec" "(readableannotation)"
		String playerString = tok.nextElement();
		String moveStr = tok.nextElement();
		String annStr = tok.nextElement();
		
		int player = commonMove.playerNumberToken(playerString);
		commonMove targetMove = drawOn.ParseMove(moveStr,player);
		commonMove activeMove = drawOn.History.find(targetMove);
		StackIterator<MoveAnnotation> annotation = MoveAnnotation.fromReadableString(annStr);
		if(activeMove!=null && annotation!=null)
		{	MoveAnnotation an = annotation.elementAt(0);
			if(an.annotation==Annotation.Clear) { activeMove.setAnnotations(null); }
			else { activeMove.addAnnotation(an); }
		}
		return true;
		}
		return false;
	}
}
