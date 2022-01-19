package lib;

import bridge.AccessControlException;
import bridge.Component;

public interface NativeMenuInterface {
	public int getItemCount();
	public NativeMenuItemInterface getMenuItem(int n);
	public void show(Component window, int x, int y) throws AccessControlException;
	public void hide(Component window);
}
