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
import lehavre.view.menus.*;

/**
 *
 *	The <code>OverviewCardLabel</code> class is a specialized label
 *	to display the overview card in the main window.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2010/11/26
 */
public final class OverviewCardLabel
extends ImageLabel
{
	static final long serialVersionUID =1L;
	/** The path to the overview card image files. */
	//private static final String OVERVIEW_PATH = "cards/overview/%s%d";

	/**
	 *	Creates a new <code>OverviewCardLabel</code> instance.
	 *	@param language the language version
	 *	@param menu the overview card menu
	 *	@param card the overview card object
	 */
	public OverviewCardLabel(NetworkInterface net,String language, OverviewCardMenu menu, OverviewCard card) {
		super(net,language, String.format("cards/overview/%s%d", card.getGameType(), card.getPlayerCount()));
		setComponentPopupMenu(menu);
	}
}