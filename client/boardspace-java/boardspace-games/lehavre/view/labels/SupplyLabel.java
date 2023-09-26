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