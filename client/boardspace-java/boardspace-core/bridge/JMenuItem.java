package bridge;

import java.awt.Font;

import lib.NativeMenuInterface;
import lib.NativeMenuItemInterface;

@SuppressWarnings("serial")
public class JMenuItem extends javax.swing.JMenuItem implements NativeMenuItemInterface
{	Icon ic = null;
	public Icon getNativeIcon() { return(ic); }
	public JMenuItem(Icon m) { super(m); ic = m; }
	public void setItemFont(Font f) { setFont(f==null ? lib.FontManager.menuFont() : f);}
	public JMenuItem(Icon m,Font f) { super(m); ic = m; setItemFont(f);}
	public JMenuItem(String s) { super(s); value=s; }
	public JMenuItem(String s,Font f) { super(s); setItemFont(f); }
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
