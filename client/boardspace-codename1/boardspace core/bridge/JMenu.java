package bridge;

import com.codename1.ui.Font;

import lib.G;
import lib.NativeMenuItemInterface;

public class JMenu extends Menu
{
	public JMenu() { }
	public JMenu(String msg) { super(msg);  }
	public void add(Menu jsubmenu) { super.add(jsubmenu); }
	public boolean isVisible() { return(false); }
	public void setSelected(boolean v) {}
	public boolean isSelected() { return(false); }
	public void addMouseListener(MouseListener l) {}
	public void addMouseMotionListener(MouseMotionListener l) {}
	public void repaint() { }
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

}
