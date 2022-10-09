package lib;

import com.codename1.ui.geom.Rectangle;

import bridge.*;
//
// this presents a JPopupMenu without being a window in the window hierarchy
// it's used to present menus on offscreen windows, but it could be used in
// other places.
// TODO: make SimpleMenu fancier, allow for scrolling and avoid placement at the bottom of the screen
//
public class SimpleMenu {
	public int margin = 2;
	public NativeMenuInterface menu=null;
	private int menuX=0;
	private int menuY=0;
	private int parentX;
	private int parentY;
	private int parentW;
	private int parentH;
	private Rectangle menuRectangle = null;
	
	public SimpleMenu(SizeProvider parent,MenuInterface fromMenu,int x,int y)
	{	Rectangle bounds = parent.getRotatedBounds();
		parentX = G.Left(bounds);
		parentY = G.Top(bounds);
		parentW = G.Width(bounds);
		parentH = G.Height(bounds);
		menuRectangle = prepareMenu(fromMenu.getNativeMenu(),x,y);
	}
	private Rectangle prepareMenu(NativeMenuInterface nativemenu,int x,int y)
	{	menu = nativemenu;
		menuX = x;
		menuY = y;
		Rectangle r = sizeMenu(nativemenu);
		int menuH = G.Height(r);
		int menuW = G.Width(r);
		int bottom = (menuY-parentY)+menuH;
		if(bottom>=parentH)
			{
			// need to move it up
			menuY -= (bottom-parentH);
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
	public boolean drawMenu(Graphics gc,HitPoint hp)
	{	boolean rv = !hp.isUp;
		if(menu!=null)
		{
		NativeMenuInterface nextMenu=null;
		Rectangle r =  menuRectangle;
		GC.fillRect(gc, Color.white,r);
		GC.frameRect(gc,Color.black, r);
		int nitems = menu.getItemCount();
		int xpos = menuX;
		int ypos = menuY;
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
			if(G.pointInRect(hp, xpos,ypos,w,h) && hp.isUp)
			{	
			ActionListener listen[] = mi.getActionListeners();
			for(ActionListener l : listen) 
				{ l.actionPerformed(new ActionEvent(mi,ActionEvent.ACTION_PERFORMED,mi.getActionCommand()));
				}
			nextMenu = mi.getSubmenu();
			}
			ypos += h;
			}
		if(nextMenu!=null)
			{
			menuRectangle = prepareMenu(nextMenu,G.Left(hp),G.Top(hp));
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
	

