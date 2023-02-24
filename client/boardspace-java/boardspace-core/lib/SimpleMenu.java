package lib;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import bridge.Icon;

//
// this presents a JPopupMenu without being a window in the window hierarchy
// it's used to present menus on offscreen windows, but it could be used in
// other places.
// TODO: make SimpleMenu fancier, allow for scrolling and avoid placement at the bottom of the screen
//
public class SimpleMenu {
	public int margin = 2;
	public NativeMenuInterface menu=null;
	private int parentX;
	private int parentY;
	private int parentW;
	private int parentH;
	private Rectangle menuRectangle = null;
	CanvasRotaterProtocol showOn = null;
	public boolean downSeen = false;
	/**
	 * x,y are real screen coordinates.
	 * @param parent
	 * @param fromMenu
	 * @param x
	 * @param y
	 */
	public SimpleMenu(CanvasRotaterProtocol parent,MenuInterface fromMenu,int x,int y)
	{	showOn = parent;
		parentX = parent.getRotatedLeft();
		parentY = parent.getRotatedTop();
		parentW = parent.getRotatedWidth();
		parentH = parent.getRotatedHeight();
		menuRectangle = prepareMenu(fromMenu.getNativeMenu(),x,y);
	}
	private Rectangle prepareMenu(NativeMenuInterface nativemenu,int x,int y)
	{	menu = nativemenu;
		downSeen = false;
		int menuX = showOn.rotateCanvasX(x,y) ;
		int menuY = showOn.rotateCanvasY(x,y) ;

		Rectangle r = sizeMenu(nativemenu);
		int menuH = G.Height(r);
		int menuW = G.Width(r);
		int bottom = (menuY-parentY)+menuH;
		if(bottom>=parentH)
			{
			// need to move it up
			menuY -= (bottom-parentH);
			}
		if(menuY<parentY)
		{
			menuY = parentY;
		}

		int right = (menuX-parentX)+menuW; 
		if(right>=parentW)
			{
			// need to move it left
			menuX -= (right-parentW);
			}
		// not handled yet, if the menu is still partly invisible it has to scroll.
		
		G.SetLeft(r,menuX);
		G.SetTop(r,menuY);
		return(r);
	}
	
	//
	// this draws the menu on the designated graphics, and looks for mouse hits
	// on the HP. If any are found, it triggers action events and returns false
	// as a signal that the menu should come down.
	//
	public boolean drawMenu(Graphics gc,HitPoint hp,int sx,int sy)
	{	
		downSeen |= hp.down;
		boolean rv = downSeen ? !hp.isUp : true;
		if(menu!=null)
		{
		NativeMenuInterface nextMenu=null;
		Rectangle r = (Rectangle)G.copy(null,menuRectangle);
		G.SetLeft(r,G.Left(r)+sx);
		G.SetTop(r,G.Top(r)+sy);
		GC.fillRect(gc, Color.white,r);
		GC.frameRect(gc,Color.black, r);
		int nitems = menu.getItemCount();
		int xpos = G.Left(r);
		int ypos = G.Top(r);
		int w = G.Width(r);
		xpos += margin;
		ypos += margin;
		for(int i=0;i<nitems;i++)
			{
			NativeMenuItemInterface mi = menu.getMenuItem(i);
			Icon ic = mi.getNativeIcon();
			String str = mi.getText();
			int h = mi.getNativeHeight();
			if(ic!=null)
			{
				if(gc!=null) { ic.paintIcon(null,gc,xpos,ypos); }
			}
			else if(str!=null)
			{
				if(gc!=null)
					{ GC.setFont(gc, mi.getFont());
					  GC.Text(gc,str,xpos,ypos+h); 
					}
			}
			if(G.pointInRect(hp, xpos,ypos,w,h))
			{ GC.frameRect(gc,Color.blue,xpos,ypos,w-2,h+2);
			  if(hp.isUp)
				  {	
				  ActionListener listen[] = mi.getActionListeners();
				
				  for(ActionListener l : listen) 
					{ l.actionPerformed(new ActionEvent(mi,ActionEvent.ACTION_PERFORMED,mi.getActionCommand()));
					}
				nextMenu = mi.getSubmenu();
				rv = false;
				}}
			ypos += h;
			}
		if(nextMenu!=null)
			{
			int hx = G.Left(hp);
			int hy = G.Top(hp);
			
			menuRectangle = prepareMenu(nextMenu,showOn.unrotateCanvasX(hx,hy),showOn.unrotateCanvasY(hx,hy));
			rv = true;
			}
		}
		return(rv);
	}
	public Rectangle sizeMenu(NativeMenuInterface menu)
	{
		int szx = 0;
		int szy = 0;
		int nitems = menu.getItemCount();
		for(int i=0;i<nitems;i++)
		{	NativeMenuItemInterface item = menu.getMenuItem(i);
			int w = item.getNativeWidth();
			int h = item.getNativeHeight();
			szy += h;
			szx = Math.max(szx, w);
		}
		return(new Rectangle(0,0,szx+margin*2,szy+margin*2));
	}
}
	

