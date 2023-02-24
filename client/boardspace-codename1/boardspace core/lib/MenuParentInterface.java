package lib;

import bridge.AccessControlException;

public interface MenuParentInterface
{
	public void show(MenuInterface menu,int x,int y) throws AccessControlException;

}
