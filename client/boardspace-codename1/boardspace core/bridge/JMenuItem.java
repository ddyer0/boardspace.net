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
package bridge;

import java.util.Vector;

import lib.AwtComponent;
import lib.GC;
import lib.Graphics;
import lib.NativeMenuInterface;
import lib.NativeMenuItemInterface;
import com.codename1.ui.Font;
import lib.Image;
import com.codename1.ui.geom.Dimension;


public class JMenuItem extends Component implements ActionListener,AwtComponent,NativeMenuItemInterface
{	MouseAdapter mouse = new MouseAdapter(null);
	String text = null;
	Object item = null;
	private Icon icon = null;
	private Font font = null;
	public Icon getIcon() { return(icon); }
	public Icon getNativeIcon() { return(icon); }
	private Image cachedImage = null; 
	private AwtComponent cachedParent = null;
	static final int MenuTextSize = 14;
	static final SystemFont.Style MenuTextStyle = SystemFont.Style.Plain;
	private Color fgcolor = Color.black;
	private Color bgcolor = new Color(0xccccff);
	public Color getBackground() { return(bgcolor); }
	public Color getForeground() { return(fgcolor); }
	public void setBackground(Color c) { bgcolor = c; }
	public void setForeground(Color c) { fgcolor = c; }
	public void setFont(Font d) 
	{ font = d; 
	}

	public Font getFont() 
	{ if(font==null) 
		{ font = lib.FontManager.menuFont();
		}
	  return(font);
	}
	public Dimension getPreferredSize()
	{
		if(icon!=null) { return(new Dimension(icon.getIconWidth(),icon.getIconHeight()));}
		String text = getText();
		if(text==null) { text="xxxx"; }
		Font f = getFont();
		FontMetrics fm = lib.FontManager.getFontMetrics(f);
		int w = fm.stringWidth(text);
		int h = fm.getHeight();
		return(new Dimension(w,h));
	}
	public Image getImage(Component parent)
	{	if(parent!=cachedParent) { cachedParent = null; cachedImage = null; }
		if(cachedImage==null)
		 { cachedParent = parent;
		   cachedImage = (icon!=null) 
			? Image.getIconImage(icon,parent)
			: getTextImage(text,parent.getFont(),getForeground(),getBackground());
		 }
		return(cachedImage);
	}
	int showAtX = 0;
	int showAtY = 0;
	public int getX() { return(showAtX); }
	public int getY() { return(showAtY); }
	
	public boolean isTreeMenu() { return(false); }
	public void setVisible(boolean v) { } 	// dummy method
	public JMenuItem getImplementation() { return(this); } 
	public com.codename1.ui.Container  getShowingOn() { return(null); }
	public void show(com.codename1.ui.Container c,int x,int y) throws AccessControlException {};	// dummy method
	Vector<ItemListener>itemListeners=null;
	Vector<ActionListener>actionListeners = null;
	public JMenuItem() 
	{  setFont(lib.FontManager.menuFont()); 
	};
	public JMenuItem(String item,Font f)
	{
		this(item);
		setFont(f==null ? lib.FontManager.menuFont() : f); 
	}
	public String toString() 
		{ 
		return((text==null) ? super.toString() : text); 
		}
	
	public JMenuItem(Icon m) { super(); icon = m; }
	public JMenuItem(Icon m,Font f)
	{	this(m);
		setFont(f==null ? lib.FontManager.menuFont() : f);
	}
	public JMenuItem(String m) { super(); text = m; }
	
	public void setText(String t) { text = t; cachedImage = null; }

	public void addItemListener(ItemListener listener) 
		{ if(itemListeners==null) 
			{ itemListeners=new Vector<ItemListener>();
			}
		if(!itemListeners.contains(listener)) { itemListeners.addElement(listener); }		
		}
	
	public void addActionListener(ActionListener deferredEvents) 
	{	if(actionListeners==null) { actionListeners = new Vector<ActionListener>();}
		actionListeners.add(deferredEvents);
	}
	public ActionListener[] getActionListeners()
	{	if(actionListeners==null) { return(new ActionListener[0]); }
		int sz = actionListeners.size();
		ActionListener ar[] = new ActionListener[sz];
		for(int i=0;i<sz;i++) { ar[i]=actionListeners.elementAt(i); }
		return(ar);
	}

	public String getText() 
	{ return(text);  
	}
	
	public void handleItemEvent(ItemEvent ev)
	{	if(itemListeners!=null)
		{
		for(int i=0;i<itemListeners.size();i++)
			{	ItemListener listener = itemListeners.elementAt(i);
				listener.itemStateChanged(new ItemEvent(this,ev.getX(),ev.getY()));
			}
		}
	}
	public void handleActionEvent(ActionEvent ev)
	{	if(actionListeners!=null)
		{
		for(int i=0;i<actionListeners.size();i++)
			{	actionListeners.elementAt(i).actionPerformed(ev);
			}
		}
	}

	public void actionPerformed(ActionEvent evt) {}


	public String getActionCommand() { return(getText()); }
	public NativeMenuInterface getSubmenu() { return(null);	}

	public int getNativeHeight() {
		return(JMenu.Height(this));
	}
	public int getNativeWidth() {
		return(JMenu.Width(this));
	}

	public FontMetrics getFontMetrics(Font f) {
		return lib.FontManager.getFontMetrics(f);
	}
	/**
	 * get an image that corresponds to the text .
	 * 
	 * @param text
	 * @param f the font
	 * @param foreground color
	 * @param background color
	 * @return a new Image
	 */
	public Image getTextImage(String text,Font f,Color foreground,Color background)
	{	FontMetrics fm = lib.FontManager.getFontMetrics(f);
		int h = Math.max(1, fm.getHeight());
		int w = Math.max(1, fm.stringWidth(text));
		Image im = Image.createImage(w,h);
		Graphics gr = im.getGraphics();
		GC.fillRect(gr,background,0,0,w,h);
		GC.setColor(gr,foreground);
		GC.setFont(gr,f);
		GC.Text(gr,text,0,fm.getAscent());
		return(im);
	}
	String value = null;
	public JMenuItem(String s,String v)
	{	this(s);
		value = v;
	}
	public String getValue() { return(value); }
	public int getWidth() {
		return icon.getIconWidth();
	}
	public int getHeight() {
		return icon.getIconHeight();
	}
}
