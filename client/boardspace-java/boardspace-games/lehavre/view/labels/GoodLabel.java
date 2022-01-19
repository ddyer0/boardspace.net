package lehavre.view.labels;

import lehavre.main.NetworkInterface;
import lehavre.model.*;
import lehavre.view.*;
import lehavre.view.menus.*;

/**
 *
 *	The <code>GoodLabel</code> class is a specialized label
 *	to display goods chits in the main window.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/26
 */
public final class GoodLabel
extends ImageLabel
{
	static final long serialVersionUID =1L;
	/**
	 *	Creates a new <code>GoodLabel</code> instance for the given goods chit.
	 *	@param language the language version
	 *	@param menu the goods chit menu
	 *	@param good the goods chit
	 */
	public GoodLabel(NetworkInterface network,String language, GoodMenu menu, Good good) {
		super(network,language, String.format(MainWindow.GOODS_PATH, good));
		setComponentPopupMenu(menu);
	}
}