/* copyright notice */package lehavre.view.labels;

import lehavre.main.NetworkInterface;
import lehavre.model.*;
import lehavre.view.*;

/**
 *
 *	The <code>CounterLabel</code> class is a specialized label to
 *	display the players' ship counter pieces in the main window.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/2/2
 */
public final class CounterLabel
extends ImageLabel
{
	static final long serialVersionUID =1L;
	/** The path to the ship token image directory. */
	private static final String COUNTER_PATH = "bits/ship%s";

	/**
	 *	Creates a new <code>CounterLabel</code> instance
	 *	for the given player.
	 *	@param player the player
	 */
	public CounterLabel(NetworkInterface net,Player player) {
		super(net,null, String.format(COUNTER_PATH, player.getColor()), false);
		setToolTipText(String.format("<html><b>%s</b></html>", player.getName()));
	}
}