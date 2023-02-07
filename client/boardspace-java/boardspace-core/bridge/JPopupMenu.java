package bridge;

import java.awt.Component;
import java.awt.Container;

import lib.G;
import lib.NativeMenuInterface;
import lib.NativeMenuItemInterface;
import lib.Plog;

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
	public void show(Component invoker, int x, int y) 
	{	// this is an attempt to paper over some glitches with JPopupMenu
		// which seems to be not entirely thread safe.  The crashes occur
		// when setSelectedPath is called as part of setVisible.  This
		// attempts to prevent the crash by doing it in advance [ddyer 1/2023 ]
		try {
		super.show(invoker,x,y);
		}
		catch (Exception e)
		{	// threading issues in swing menus cause random errors
			Plog.log.addLog("Error showing menu ",e);
			G.doDelay(100);
			super.show(invoker,x,y);			
		}
	}
}

