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

import com.codename1.ui.geom.Point;
import com.codename1.ui.geom.Rectangle;
import bridge.Color;

/**
 * this provides a menu of annotations such as squares, arrows, and triangles which can be added to the current move of a game.
 * 
 * they will be saved to the game record as additions to the commonMove structure, and printed to sgf files
 * as AN properties.
 * 
 * this is fully integrated as of version 6.99.  These annotations, if seen by earlier versions of the client,
 * will be ignored.
 * 
 */


import lib.CellId;
import lib.Drawable;
import lib.G;
import lib.GC;
import lib.Graphics;
import lib.HitPoint;
import lib.InternationalStrings;
import lib.NetConn;
import lib.PopupManager;
import lib.StackIterator;
import lib.StockArt;
import lib.StringStack;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;
import online.common.OnlineConstants;
import online.game.sgf.sgf_property;

@SuppressWarnings("serial")
/**
 * the main annotation icon extends rectangle so it can be placed and manipulated
 * uniformly with other boxes on the game canvas
 * 
 * @author ddyer
 *
 */
public class AnnotationMenu extends Rectangle implements PlayConstants,OnlineConstants
{
	static final int ANNOTATION_TRACKING_OFFSET = 100;
	static final String ANNOTATION_TAG = "A1";
	public enum Annotation
	{	// later. include freehand drawing and ad-hoc arrows
		Triangle(StockArt.Triangle,StockArt.Triangle,0),
		Square(StockArt.Square,StockArt.Square,1),
		Ex(StockArt.Exmark,StockArt.Exmark,6),
		Left(StockArt.SolidLeftArrow,StockArt.SolidRightArrow,2),
		Right(StockArt.SolidRightArrow,StockArt.SolidLeftArrow,3),
		Up(StockArt.SolidUpArrow,StockArt.SolidDownArrow,4),
		Down(StockArt.SolidDownArrow,StockArt.SolidUpArrow,5),
		Clear(null,null,7);
		int index;
		private Drawable chip;
		private Drawable reverseChip = null;
		Annotation(StockArt c,StockArt rev,int ind)
			{ chip = c; reverseChip=rev; index = ind; 
			}
		public int index() { return index; }
		public Drawable getChip(boolean rev) { return rev ? reverseChip : chip; }
		static public Annotation find(int n)
		{	for(Annotation a : values()) { if(a.index==n) { return a; }}
			return null;
		}
	}
	private Drawable base = null;		// this is the image drawn as the icon on the tool bar
	private PopupManager menu = null;			// the menu used during the selection process
	private commonCanvas drawOn=null;			// the associated game viewer
	private CellId id = null;					// the hit code when the annotation icon is clicked
	private Annotation selected = null;			// the annotation actively selected for placement, if any
	private boolean selectedMulti = false;		// if true, keep selecting
	public boolean getMulti() { return selectedMulti; }
	public Annotation getSelected() { return selected; }
	public void setSelected(Annotation sel) { selected = sel; }
	private StringStack annotationActions = new StringStack();			// annotations queued for transmission to the other players
	private StringStack annotationActionsHistory = new StringStack();	// history of all transmitted annotations
	
	static String helpText = "Add an Annotation";
	static String CancelMessage = "click here to cancel annotation";
	static String StopMessage = "Stop placing annotations";

	static public void putStrings()
	{
		InternationalStrings.put(helpText);
		InternationalStrings.put(CancelMessage);
		InternationalStrings.put(StopMessage);
	}
	/**
	 * create an annotation menu.  Generally, each canvas creates one so it's always there.
	 * everything becomes active when the annotation menu rectangle is placed on the game window.
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
	    	if( base.drawChip(gc,drawOn,width,G.centerX(this),G.centerY(this),highlight,id,null))
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
		boolean Columns2 = true;
		if(menu==null) { menu=new PopupManager(); }
		menu.useSimpleMenu = true;
		menu.newPopupMenu(drawOn,drawOn.deferredEvents);
		double ysize = 1.25;
		double xsize = 1;
		selected = null;
		selectedMulti = false;
		//Font baseFont = drawOn.getFont();
		//Font menuFont = FontManager.getFont(baseFont,baseFont.getSize()*4/4);
		//menu.setFont(menuFont);
		for(Annotation a : Annotation.values())
			{ 
			Drawable chip = a.getChip(drawOn.reverseView());
			if(chip==null)
			{	// put the "clear" item in the right column
				menu.addMenuItem("",a.index);
				menu.addMenuItem(TextChunk.create(a.name()),a.index);
			}
			else
			{
			Text ic = TextGlyph.create(chip,a.name(),menu,ysize,xsize);
			menu.addMenuItem(ic,a.index);
				menu.addMenuItem(
				TextGlyph.join(
						TextGlyph.create(chip,a.name(),menu,ysize,xsize),
						TextChunk.create("++")
					),
				a.index+100);
			}
			}
		if(Columns2) { menu.setNColumns(2); }
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
			{	int selectedIndex = menu.value;
				selected = Annotation.find(selectedIndex%100);
				selectedMulti = selectedIndex>=100;
				if(selected==Annotation.Clear)
				{	// clear is special, we don't drag it around we just do it.
					commonMove cm = drawOn.getCurrentMove();
					cm.setAnnotations(null);
					saveAnnotation(cm ,selected,0,0);
					selected = null;
				}
				else
				{
				base = selected.getChip(drawOn.reverseView());
				menu = null;
				}
				return(true);	// we handled it
				}
			}
			return(false);
		}
	/**
	 * draw an annotation at a particular size and location.  The location
	 * is derived from the standard mouse position encoding, and the size
	 * is expected to be associated with the cell size of the board.
	 * 
	 * @param gc
	 * @param an
	 * @param size
	 * @param x
	 * @param y
	 */
	public void drawAnnotation(Graphics gc,Annotation an,int size,int x,int y)
	{
		if(an.chip!=null) { an.getChip(drawOn.reverseView()).drawChip(gc,drawOn,size,x,y,null); }
	}
	/**
	 * save an annotation on a move. The supplied x,y are absolute, but
	 * they'll be recorded as encoded mouse positions.  This uses the 
	 * same encoding scheme used to transmit the mouse positions during
	 * the game, so for example board positions are presented correctly
	 * given each players choice of viewing mode (ie; upside down, with
	 * perspective, or whatever)
	 * 
	 * @param m		the move in the game history to add the annotation
	 * @param an	the annotation
	 * @param ref	normally the board rectangle
	 * @param x		the absolute x of the annotation
	 * @param y		the absolute x of the annotation
	 */
	public void saveAnnotation(commonMove m,Annotation an,int x,int y)
	{	Point pt = new Point(0,0);
		String zone = drawOn.encodeScreenZone(x, y,pt); 
		MoveAnnotation n = new MoveAnnotation(an,zone,G.Left(pt),G.Top(pt));
		m.addAnnotation(n);	
		if(drawOn.canTrackMouse())
		{
		// this queues the annotation to be sent to the other players and spectators
		// spectators can't place public annotations during a game
		annotationActions.push(G.concat(ANNOTATION_TAG," ",m.playerString(),
				" \"",
				sgf_property.bracketedString(m.longMoveString()),
				"\" ",
				"\"",n.toReadableString(),"\""));
		}
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
	public boolean saveCurrentAnnotation(HitPoint hp,commonMove cm) 
	{
		boolean used = false;
		if(selected!=null)
		{	
		saveAnnotation(cm,selected,G.Left(hp),G.Top(hp));
		if(!selectedMulti) { setSelected(null); }
		used = true;
		}
		return used;
		
	}
	
	public void stopAnnotation()
	{
		selected = null;
		selectedMulti = false;
	}
	
	private Annotation sharedAnnotation = null;
	private String sharedAnnotationString = null;
	private long sharedAnnotationTime = 0;
	/**
	 * retrieve the current annotation tracking message, if any.  The same
	 * messages are used to track annotations being dragged around by some
	 * players' mouse and for just the mouse or the mouse dragging a piece.
	 * 
	 * this is piggybacked onto the same mechanism that transmits the mouse
	 * position among the players.
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
               //
               // this is constructed so the tracking messages for annotations will
               // be ignored by old clients that don't understand the annotations
               //
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
	// if no annotation is being shared, return null so the 
	// default action of just tracking the mouse will occur
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
	public void drawAnimationSprite(Graphics g, int obj, int xp, int yp, String mode,int size) 
	{
		if(ANNOTATION_TAG.equals(mode) 
				&& (Opcodes.NothingMoving!=obj))
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
		commonMove activeMove = drawOn.History.matchesCurrentMove(targetMove);
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
	
	public void showCancellation(Graphics gc,Rectangle r,HitPoint hp,CellId id)
	{	InternationalStrings s = G.getTranslations();
		GC.frameRect(gc,Color.black,r);
		GC.Text(gc,true,r,Color.black,null,s.get(CancelMessage));
		HitPoint.setHelpText(hp,r,id,s.get(StopMessage));
	}
}
