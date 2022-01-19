package bridge;

import java.awt.Component;
import java.awt.Container;

import lib.NativeMenuInterface;
import lib.NativeMenuItemInterface;

@SuppressWarnings("serial")
public class JPopupMenu extends javax.swing.JPopupMenu implements NativeMenuInterface
{	public JPopupMenu() { super(); }
	public JPopupMenu(String msg) { super(msg); } 
	public int getItemCount() { return(getComponentCount()); }
	public NativeMenuItemInterface getMenuItem(int n) 
	{ Component c = getComponent(n);
	  return((NativeMenuItemInterface)c);
	}
	public void hide(Component window)
	{
		if(window instanceof Container) { ((Container)window).remove(this); }
	}
}

