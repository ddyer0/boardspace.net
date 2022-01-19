package lehavre.view;

import java.awt.event.*;
import lehavre.main.*;
import lehavre.model.*;
import lehavre.util.*;

/**
 *
 *	The <code>MoneyWindow</code> class is the dialog displayed
 *	when a player has not enough money to pay the interest.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/2/7
 */
public final class MoneyWindow
extends DialogWindow
{
	static final long serialVersionUID =1L;
	/**
	 *	Creates a new <code>MoneyWindow</code> instance.
	 *	@param control the control object
	 *	@param player the player object
	 */
	public MoneyWindow(final LeHavre control, final Player player) {
		super(control);
		GameState game = control.getGameState();
		final int demand = game.getInterest(player);

		/* Create the action listener */
		ActionListener action = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(player.getLoans() > 0 && player.getMoney() < demand && !confirm("TakeLoans")) return;
				control.payInterest();
				setVisible(false);
				dispose();
			}
		};

		/* Create the contents */
		Dictionary dict = control.getDictionary();
		String descr = String.format(
			get("moneyDescr"),
			Util.getNumbered(dict, player.getLoans(), "loan"),
			Util.getColoredWithAmount(dict, demand, Good.Franc)
		);
		create(player, "money", descr, null, action);
	}
}