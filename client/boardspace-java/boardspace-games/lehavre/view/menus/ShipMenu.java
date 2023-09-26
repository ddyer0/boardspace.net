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
package lehavre.view.menus;

import java.awt.event.*;
import javax.swing.*;
import lehavre.main.*;
import lehavre.model.*;
import lehavre.util.*;

/**
 *
 *	The <code>ShipMenu</code> class is the context menu of a ship label.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/26
 */
public final class ShipMenu
extends JPopupMenu
{
	static final long serialVersionUID =1L;
	/** The dynamic menu items. */
	private JMenuItem build, purchase, sale;

	/**
	 *	Creates a new <code>ShipMenu</code> instance for the given ship.
	 *	@param control the control object
	 *	@param ship the ship
	 */
	public ShipMenu(final LeHavre control, final Ship ship) {
		Dictionary dict = control.getDictionary();
		build = new JMenuItem(dict.get("menuBuild"));
		build.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					control.build(ship);
				}
			}
		);
		add(build);
		build.setEnabled(false);
		purchase = new JMenuItem(dict.get("menuBuy"));
		purchase.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if(build.isEnabled()) {
						GameState game = control.getGameState();
						game.remove(ship);
						int n = game.getActivePlayer().getLocation().getShips(control).size();
						game.addShip(ship);
						if(n == 0) {
							control.showError("NoShipBuying");
							return;
						}
					}
					control.buy(ship);
				}
			}
		);
		add(purchase);
		purchase.setEnabled(false);
		sale = new JMenuItem(dict.get("menuSell"));
		sale.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					control.sell(ship);
				}
			}
		);
		add(sale);
		sale.setEnabled(false);
		addSeparator();
		JMenuItem item = new JMenuItem(dict.get("menuInfo"));
		item.addActionListener(
			new ActionListener() {
				private Dictionary dict = control.getDictionary();
				public void actionPerformed(ActionEvent e) {
					control.showInformation(Util.getToolTipText(dict, ship), dict.get("popupShip"));
				}
			}
		);
		add(item);
	}

	/**
	 *	Enables or disables the building option.
	 *	@param enabled provide true to enable the building option
	 */
	public void setBuildingEnabled(boolean enabled) {
		build.setEnabled(enabled);
	}

	/**
	 *	Returns true if building is enabled.
	 *	@return true if building is enabled
	 */
	public boolean isBuildingEnabled() {
		return build.isEnabled();
	}

	/**
	 *	Returns the building menu item.
	 *	@return the building menu item
	 */
	public JMenuItem getBuildOption() {
		return build;
	}

	/**
	 *	Enables or disables the purchase option.
	 *	@param enabled provide true to enable the purchase option
	 */
	public void setPurchaseEnabled(boolean enabled) {
		purchase.setEnabled(enabled);
	}

	/**
	 *	Enables or disables the sale option.
	 *	@param enabled provide true to enable the sale option
	 */
	public void setSaleEnabled(boolean enabled) {
		sale.setEnabled(enabled);
	}
}