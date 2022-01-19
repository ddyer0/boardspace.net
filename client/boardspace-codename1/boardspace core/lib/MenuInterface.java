package lib;

import bridge.ActionListener;

public interface MenuInterface {
	public NativeMenuItemInterface add(String item,ActionListener listener);
	public NativeMenuItemInterface add(Text item,ActionListener listener);
	public NativeMenuInterface getNativeMenu();	
	public void add(MenuInterface item);
	public boolean isVisible();
	public void show(MenuParentInterface parent, int x, int y);
	public void setVisible(boolean b);
	public MenuInterface newSubMenu(String msg);
}
