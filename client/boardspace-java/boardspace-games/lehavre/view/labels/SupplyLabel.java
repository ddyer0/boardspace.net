package lehavre.view.labels;

import lehavre.main.NetworkInterface;
import lehavre.model.*;
import lehavre.view.*;
import lehavre.view.menus.*;

/**
 *
 *	The <code>SupplyLabel</code> class is a specialized label
 *	to display supply chits in the main window.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class SupplyLabel
extends ImageLabel
{
	static final long serialVersionUID =1L;
	/** The represented supply chit. */
	private Supply supply;

	/** The context menu. */
	private SupplyMenu menu;

	/** True if the supply chit is still hidden. */
	private boolean hidden;

	/** The language version. */
	private String language;

	/**
	 *	Creates a new <code>SupplyLabel</code> instance for the given supply chit.
	 *	@param language the language version
	 *	@param menu the supply chit menu
	 *	@param supply the supply chit
	 */
	public SupplyLabel(NetworkInterface net,String language, SupplyMenu menu, Supply supply) {
		super(net,null, String.format(MainWindow.SYMBOL_PATH, "supply0"), false);
		this.language = language;
		this.supply = supply;
		this.menu = menu;
		hidden = true;
	}

	/**
	 *	Flips over the supply chit and makes it visible.
	 */
	public void turnOver(NetworkInterface net) {
		if(!hidden) return;
		createIcon(net,language, String.format(MainWindow.SUPPLY_PATH, supply), true);
		setComponentPopupMenu(menu);
		hidden = false;
	}
}