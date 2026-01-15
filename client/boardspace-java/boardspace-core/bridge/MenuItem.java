package bridge;

import java.awt.Font;

import lib.NativeMenuInterface;
import lib.NativeMenuItemInterface;

@SuppressWarnings("serial")
public class MenuItem extends java.awt.MenuItem implements NativeMenuItemInterface
{
	public MenuItem(String string) { super(string); }
	public MenuItem(String string,Font f) { this(string); setFont(f==null ? lib.FontManager.menuFont() : f); }
	public Icon getNativeIcon() {	return null; }
	public NativeMenuInterface getSubmenu() { return(null); }
	
	public int getNativeHeight() { return(JMenu.Height(this));	}

	public int getNativeWidth() { return(JMenu.Width(this)); }
	public Font getFont() 
	{ Font f = super.getFont();
	  if(f==null) { f = lib.FontManager.getGlobalDefaultFont(); }
	  return(f);
	}
	public String getText() { return(getLabel()); }
			
}
