package lib;

import java.awt.Font;
import java.awt.event.ActionListener;
import bridge.Icon;

public interface NativeMenuItemInterface 
{
	public Icon getNativeIcon();
	public String getText();
	public NativeMenuInterface getSubmenu();
	public int getNativeHeight();	// this has a unique name to avoid overriding getHeight
	public int getNativeWidth();	// this has a unique name to avoid overriding getWidth
	public Font getFont();
	public ActionListener[]getActionListeners();
	public void addActionListener(ActionListener d);
	public String getActionCommand();
	
}
