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

import com.codename1.ui.Font;
import com.codename1.ui.geom.Rectangle;
import bridge.*;

//
// this presents a JPopupMenu without being a window in the window hierarchy
// it's used to present menus on offscreen windows, but it could be used in
// other places.
// TODO: add "fling" scrolling for touch screens
//
public class SimpleMenu {
	public int margin = 6;
	public NativeMenuInterface menu=null;
	private int parentX;
	private int parentY;
	private int parentW;
	private int parentH;
	private int nColumns = 1;
	private int columnWidth[] = null;

	private Rectangle menuRectangle = null;
	CanvasRotaterProtocol showOn = null;
	private String subMenuString = " ...";
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
		nColumns = Math.max(1,fromMenu.getNColumns());
		menuRectangle = prepareMenu(fromMenu.getNativeMenu(),x,y);
	}
	private ScrollArea scrollArea = null;
	
	public boolean doMouseWheel(int x,int y,double amount)
	{
		if(scrollArea!=null)
		{
			return scrollArea.doMouseWheel(x,y,amount);
		}
		return false;
	}
	private Rectangle prepareMenu(NativeMenuInterface nativemenu,int x,int y)
	{	menu = nativemenu;
		downSeen = false;
		int menuX = showOn.rotateCanvasX(x,y) ;
		int menuY = showOn.rotateCanvasY(x,y) ;

		Rectangle r = sizeMenu(nativemenu);
		int menuH = G.Height(r);
		int totalH = menuH;
		int menuW = G.Width(r);
		if(menuH>parentH-100)
		{
			// we'll need a scroll bar
			scrollArea = new ScrollArea();
			int scrollW = ScrollArea.getDefaultScrollbarWidth();
			menuW += scrollW;
			menuH = parentH-100;
			menuY = 50;
			G.SetHeight(r,menuH);
			G.SetTop(r,menuY);
		}
		else { scrollArea = null; }
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
		
		if(scrollArea!=null)
		{	scrollArea.alwaysVisible = true;
			scrollArea.InitScrollDimensions(
		  			 menuX+menuW-ScrollArea.getDefaultScrollbarWidth(), 
		  			 r,
		  			 ScrollArea.getDefaultScrollbarWidth(),			// scroll bar width
		  			 totalH-menuH*2/3,
		  			 10,				// small jump size
		             menuH/4);			// bi
		}
		return(r);
	}
	private NativeMenuItemInterface selectedItem = null;
	//
	// this draws the menu on the designated graphics, and looks for mouse hits
	// on the HP. If any are found, it triggers action events and returns false
	// as a signal that the menu should come down.
	//
	public boolean drawMenu(Graphics gc,HitPoint hp,int sx,int sy)
	{	
		int hpX =G.Left(hp);
		int hpY =G.Top(hp);
		downSeen |= hp.down;
		boolean rv = downSeen ? ! hp.isUp : true;
		if(menu!=null)
		{
		if(hpX<0 && hpY<0) { hp = null; }
		NativeMenuInterface nextMenu=null;
		Rectangle r = (Rectangle)G.copy(null,menuRectangle);
		G.SetLeft(r,G.Left(r)+sx);
		G.SetTop(r,G.Top(r)+sy);
		GC.fillRect(gc, Color.white,r);
		GC.frameRect(gc,Color.black, r);
		int nitems = menu.getItemCount();
		int xpos = G.Left(r);
		xpos += margin;
		boolean scroll = scrollArea!=null;
		int ypos = G.Top(r)+margin;
		ypos += margin-maxDescent-(scroll?scrollArea.getScrollPosition() : 0);
		int xpos0 = xpos;
		Rectangle clip = GC.setClip(gc,menuRectangle);
		{
		int thisCol = 0;
		int colHeight = 0;
		
		for(int i=0;i<nitems;i++)
			{
			NativeMenuItemInterface mi = menu.getMenuItem(i);
			int colWidth = columnWidth[thisCol];
			Icon ic = mi.getNativeIcon();
			String str = mi.getText();
			int h = mi.getNativeHeight();
			colHeight = Math.max(h,colHeight);
			if(ic!=null)
			{
				if(gc!=null) 
				{ ic.paintIcon(null,gc,xpos,ypos); 
				  if(selectedItem==mi)
				  { GC.frameRect(gc,Color.blue,xpos,ypos,colWidth,h);
				  }
				}
			}
			else if(str!=null)
			{
				if(gc!=null)
					{ if(mi.getSubmenu()!=null) { str += subMenuString; }
					  GC.setFont(gc,mi.getFont());
					  GC.setColor(gc,selectedItem==mi ? Color.blue :Color.black);
					  GC.Text(gc,str,xpos,ypos+h); 
					  if(selectedItem==mi)
						{ GC.frameRect(gc,xpos,ypos+maxDescent,colWidth,h+2);
						}					  
					}
			}
			
			if(G.pointInRect(hp, xpos,ypos,colWidth,h)
					&& ((ic!=null) || !"".equals(str)))
			{ selectedItem = mi;
			if(downSeen && hp.isUp)
			{	
			ActionListener listen[] = mi.getActionListeners();
				
			for(ActionListener l : listen) 
				{ l.actionPerformed(new ActionEvent(mi,ActionEvent.ACTION_PERFORMED,mi.getActionCommand()));
				}
			nextMenu = mi.getSubmenu();
				rv = false;
				}}
			thisCol++;
			if(thisCol>=nColumns)
			{
				xpos = xpos0;
				ypos += colHeight;
				thisCol = 0;
				colHeight = 0;
			}
			else {
				xpos += colWidth+margin;
			}
			
			}
		if(nextMenu!=null)
			{
			int hx = G.Left(hp);
			int hy = G.Top(hp);
			
			menuRectangle = prepareMenu(nextMenu,showOn.unrotateCanvasX(hx,hy),showOn.unrotateCanvasY(hx,hy));
			rv = true;
			}
		}
		GC.setClip(gc,clip);
		if(scrollArea!=null)
			{
			 if(scrollArea.inScrollBarRect(hpX,hpY))
			 {
			 rv = true;
			 if(hp!=null )
					{ scrollArea.doMouseMotion(hpX,hpY,hp.upCode); 
					}
			 }
			 scrollArea.drawScrollBar(gc); 			 
			}
		}
		return(rv);
	}

	private int maxDescent = 0; //used to adjust the position of menu items within the overall menu
	public Rectangle sizeMenu(NativeMenuInterface menu)
	{
		int szy = 0;
		maxDescent = 0;
		int nCols = Math.max(nColumns,1);
		columnWidth = new int[nCols];
		int columnHeight = 0;
		int thisCol = 0;
		Font lastFont = null;
		int lastItem = menu.getItemCount()-1;
		int subwidth = 0;
		for(int i=0;i<=lastItem;i++)
		{
		NativeMenuItemInterface item = menu.getMenuItem(i);
		boolean hasSubmenu = (item.getSubmenu()!=null);
		Font f = item.getFont();
		if(f!=lastFont)
			{
			lastFont = f;
			FontMetrics desc = lib.FontManager.getFontMetrics(f);
			maxDescent = Math.max(maxDescent,desc.getMaxDescent());
			subwidth = desc.stringWidth(subMenuString);
			}
		
		int w = item.getNativeWidth()+(hasSubmenu?subwidth:0);
			int h = item.getNativeHeight();
		columnWidth[thisCol] = Math.max(columnWidth[thisCol],w);
		columnHeight = Math.max(columnHeight,h);
		thisCol++;
		if(thisCol>=nCols || i==lastItem)
			{
			szy += columnHeight;
			columnHeight = 0;
			thisCol = 0;
			}
		}
		int szx = 0;
		for(int i=0;i<nCols;i++) { szx += columnWidth[i]; }
		
		return(new Rectangle(0,0,szx+margin*(2+nCols-1),szy+margin*2));
	}
}
	

