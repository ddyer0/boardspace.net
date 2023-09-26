/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.
    
    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/. 
 */
package lehavre.view.labels;

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