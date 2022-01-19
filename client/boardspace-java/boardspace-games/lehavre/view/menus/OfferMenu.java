package lehavre.view.menus;

import java.awt.event.*;
import javax.swing.*;
import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.Building;
import lehavre.model.buildings.special.Hotel;
import lehavre.util.*;

/**
 *
 *	The <code>OfferMenu</code> class is the context menu of an offer.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/26
 */
public final class OfferMenu
extends JPopupMenu
{
	static final long serialVersionUID =1L;
	/** The dynamic menu item. */
	private JMenuItem offer;

	/**
	 *	Creates a new <code>OfferMenu</code> instance for the given good.
	 *	@param control the control object
	 *	@param good the offered good
	 */
	public OfferMenu(final LeHavre control, final Good good) {
		final Dictionary dict = control.getDictionary();
		offer = new JMenuItem(dict.get("menuTake"));
		offer.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Player player = control.getCurrentPlayer();
					if(!player.isOfferAllowed()) {
						control.showError("OfferTabu");
						return;
					}
					control.reduceActions();
					control.takeOffer(good);
					// Hotel code
					Building location = player.getLocation();
					Hotel hotel = Hotel.getInstance();
					if(location != null && location.equals(hotel)) {
						GoodsList goods = new GoodsList();
						goods.add(hotel.OUTPUT_FRANC, Good.Franc);
						control.receive(player, goods);
					}
				}
			}
		);
		add(offer);
		offer.setEnabled(false);
		addSeparator();
		JMenuItem item = new JMenuItem(dict.get("menuInfo"));
		item.addActionListener(
			new ActionListener() {
				private final String text, title;
				{
					text = String.format(dict.get("popupOffered"), "%s");
					title = dict.get("popupOffer");
				}
				public void actionPerformed(ActionEvent e) {
					String msg = String.format(text, Util.getColored(dict, new GoodsPair(control.getGameState().getOffer(good), good)));
					control.showInformation(msg, title);
				}
			}
		);
		add(item);
	}

	/**
	 *	Enables or disables the take option.
	 *	@param enabled provide true to enable the take option
	 */
	public void setOfferEnabled(boolean enabled) {
		offer.setEnabled(enabled);
	}

	/**
	 *	Activates the menu option.
	 */
	public void activateOffer() {
		if(offer.isEnabled()) offer.doClick();
	}
}