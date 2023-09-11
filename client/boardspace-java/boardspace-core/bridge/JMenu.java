package bridge;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.MouseEvent;

import lib.G;
import lib.Graphics;
import lib.NativeMenuInterface;
import lib.NativeMenuItemInterface;
import lib.Plog;
import lib.SizeProvider;

@SuppressWarnings("serial")
public class JMenu extends javax.swing.JMenu implements NativeMenuInterface,NativeMenuItemInterface,SizeProvider
{	public JMenu(String m) { super(m); }
	public JMenu() { super(); }
	public void paintIcon(Component c, Graphics g, int x, int y) { G.Error("should be overridden");}
	public void paintIcon(Component c, java.awt.Graphics g, int x, int y)
	{
		paintIcon(c,Graphics.create(g),x,y);
	}
	public Icon getNativeIcon() { return(null); }
	public NativeMenuItemInterface getMenuItem(int n) 
	{
		return((NativeMenuItemInterface)getItem(n));
	}
	public void show(Component window, int x, int y) {
		G.Error("Can't show directly");
	}
	public void hide(Component window)
	{
		G.Error("Can't hide directly");
	}
	public NativeMenuInterface getSubmenu() { return(this); }
	public int getNativeWidth() { return(JMenu.Width(this)); }
	public int getNativeHeight() { return(JMenu.Height(this)); }
	public void paint(java.awt.Graphics g) { paint(Graphics.create(g)); }
	public void paint(Graphics g) { super.paint(g.getGraphics()); }
	public static int Height(NativeMenuItemInterface mi)
	{	Icon ic = mi.getNativeIcon();
		if(ic!=null) 
		{
		return(ic.getIconHeight());	
		}
		else
		{
		String str = mi.getText();
		if(str==null) { str="xxxx"; }
		Font f = mi.getFont();
		FontMetrics fm = G.getFontMetrics(f);
		return(fm.getHeight());
		}
	}
	public static int Width(NativeMenuItemInterface mi)
	{	Icon ic = mi.getNativeIcon();
		if(ic!=null) 
		{
		return(ic.getIconWidth());	
		}
		else
		{
		String str = mi.getText();
		if(str==null) { str="xxxx"; }
		Font f = mi.getFont();
		FontMetrics fm = G.getFontMetrics(f);
		return(fm.stringWidth(str));
		}
	}
	protected void processMouseEvent(MouseEvent e) 
	{	try {
		int id = e.getID();
		if(G.isCheerpj())
		{
		G.print("mouse event "+e+ " "+this);
		G.print(G.getStackTrace());
		}
            switch(id)
            {
            case MouseEvent.MOUSE_ENTERED:	
            	// inhibit mouse entered to avoid auto-selection moving
            	// between items on the jmenubar
            	if(getItemCount()==0) { break; }
            case MouseEvent.MOUSE_PRESSED:
            	//G.infoBox("button",G.getStackTrace());
            default: 
            	super.processMouseEvent(e);
            }
		}
		catch (ArrayIndexOutOfBoundsException err) 
			{ Plog.log.addLog("error in java menu ",err);
			}
        }
	
}
