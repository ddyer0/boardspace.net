package lehavre.view.labels;

import java.awt.event.*;

import lehavre.main.NetworkInterface;
import lehavre.model.*;
import lehavre.view.*;
import lehavre.view.menus.*;

/**
 *
 *	The <code>OfferLabel</code> class is a specialized label
 *	to display offered goods in the main window.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/26
 */
public final class OfferLabel
extends ImageLabel
{
	static final long serialVersionUID =1L;
	/** The context menu. */
	private OfferMenu menu;

	/**
	 *	Creates a new <code>GoodLabel</code> instance for the given goods chit.
	 *	@param language the language version
	 *	@param menu the goods chit menu
	 *	@param good the goods chit
	 */
	public OfferLabel(NetworkInterface net,String language, OfferMenu menu, Good good) {
		super(net,language, String.format(MainWindow.GOODS_PATH, good));
		setComponentPopupMenu(menu);
		this.menu = menu;
		addMouseListener(
			new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if(e.getClickCount() > 1) OfferLabel.this.menu.activateOffer();
				}
			}
		);
	}

	/**
	 *	Enables or disables the take option.
	 *	@param enabled provide true to enable the take option
	 */
	public void setOfferEnabled(boolean enabled) {
		menu.setOfferEnabled(enabled);
	}
}