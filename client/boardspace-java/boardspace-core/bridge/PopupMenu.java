package bridge;

import java.awt.Component;
import java.awt.Font;

import lib.G;
import lib.NativeMenuInterface;
import lib.NativeMenuItemInterface;

@SuppressWarnings("serial")
public class PopupMenu extends java.awt.PopupMenu implements NativeMenuInterface,NativeMenuItemInterface
{	public PopupMenu(String m) { super(m==null?"":m); }
	public NativeMenuItemInterface getMenuItem(int n) { return((NativeMenuItemInterface)getItem(n)); }
	public NativeMenuInterface getSubmenu() { return(this); }
	public Icon getNativeIcon() {	return null;	}
	public String getText() { return(getLabel()); }
	public int getNativeHeight() { return(JMenu.Height(this));	}
	public int getNativeWidth() {	return(JMenu.Width(this)); }
	public void show(Component window,int x,int y)
	{
		window.add(this);
		super.show(window,x,y);
	}
	public void hide(Component window)
	{
		window.remove(this);
	}
	public Font getFont() 
	{
		Font f = super.getFont();
		if(f==null) { f = G.getGlobalDefaultFont(); }
		return(f);
	}
}
