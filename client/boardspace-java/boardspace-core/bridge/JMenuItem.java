package bridge;

import lib.NativeMenuInterface;
import lib.NativeMenuItemInterface;

@SuppressWarnings("serial")
public class JMenuItem extends javax.swing.JMenuItem implements NativeMenuItemInterface
{	public Icon getNativeIcon() { return(null); }
	public JMenuItem(Icon m) { super(m);}
	public JMenuItem(String s) { super(s); value=s; }
	public NativeMenuInterface getSubmenu() { return(null);	}
	public int getNativeWidth() { return(JMenu.Width(this)); }
	public int getNativeHeight() { return(JMenu.Height(this)); }	
	String value = null;
	public JMenuItem(String s,String v)
	{	super(s);
		value = v;
	}
	public String getValue() { return(value); }
}
