/* copyright notice */package lehavre.view.labels;

import lehavre.main.NetworkInterface;
import lehavre.model.*;
import lehavre.view.*;

/**
 *
 *	The <code>WorkerLabel</code> class is a specialized label to
 *	display the players' worker piece in the main window.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2010/1/31
 */
public final class WorkerLabel
extends ImageLabel
{
	static final long serialVersionUID =1L;
	/** The path to the worker token image directory. */
	private static final String WORKER_PATH = "bits/worker%s";

	/**
	 *	Creates a new <code>WorkerLabel</code> instance
	 *	for the given player.
	 *	@param player the player
	 */
	public WorkerLabel(NetworkInterface net,Player player) {
		super(net,null, String.format(WORKER_PATH, player.getColor()), false);
		setToolTipText(String.format("<html><b>%s</b></html>", player.getName()));
	}
}