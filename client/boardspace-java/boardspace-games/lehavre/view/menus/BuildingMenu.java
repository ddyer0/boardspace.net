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
import java.util.ArrayList;
import javax.swing.*;
import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.view.GUIHelper;

/**
 *
 *	The <code>BuildingMenu</code> class is the context menu of a building label.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public class BuildingMenu
extends JPopupMenu
{
	static final long serialVersionUID =1L;
	/** The dynamic menu items. */
	protected final JMenuItem entry, ban, build, purchase, sale;

	/** The underlying building. */
	protected final Buildings building;

	/** The control object. */
	protected final LeHavre control;

	/**
	 *	Creates a new <code>BuildingMenu</code> instance for the given building.
	 *	@param control the control object
	 *	@param building the building
	 */
	public BuildingMenu(LeHavre control, Building building) {
		this.control = control;
		this.building = building.getProto();
		Dictionary dict = control.getDictionary();
		entry = new JMenuItem(dict.get("menuEnter"));
		entry.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					LeHavre control = getControl();
					if(!control.getCurrentPlayer().isBuildingAllowed()) {
						control.showError("BuildingTabu");
						return;
					}
					control.enter(getBuilding());
				}
			}
		);
		add(entry);
		entry.setEnabled(false);
		ban = new JMenuItem(dict.get("menuBan"));
		ban.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					getControl().ban(getBuilding(), 0);
				}
			}
		);
		add(ban);
		ban.setEnabled(false);
		addSeparator();
		build = new JMenuItem(dict.get("menuBuild"));
		build.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					getControl().build(getBuilding());
				}
			}
		);
		add(build);
		build.setEnabled(false);
		purchase = new JMenuItem(dict.get("menuBuy"));
		purchase.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Building building = getBuilding();
					LeHavre control = getControl();
					if(build.isEnabled()) {
						GameState game = control.getGameState();
						if(building.isShip()) {
							Town town = game.getTown();
							town.lose(building);
							int n = game.getActivePlayer().getLocation().getShips(control).size();
							town.receive(building);
							if(n == 0) {
								control.showError("NoShipBuying");
								return;
							}
						} else {
							int index = 0;
							for(; index < GameState.STACK_COUNT; index++) if(game.getStandard(index).contains(building)) break;
							game.remove(building);
							int n = game.getActivePlayer().getLocation().getBuildings(control).size();
							game.addStack(building, index);
							if(n == 0) {
								control.showError("NoBuildingBuying");
								return;
							}
						}
					}
					control.buy(building);
				}
			}
		);
		add(purchase);
		purchase.setEnabled(false);
		sale = new JMenuItem(dict.get("menuSell"));
		sale.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					getControl().sell(getBuilding());
				}
			}
		);
		add(sale);
		sale.setEnabled(false);
		addSeparator();
		JMenuItem item = new JMenuItem(dict.get("menuSpecial"));
		item.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					doSpecial();
				}
			}
		);
		add(item);
		item = new JMenuItem(dict.get("menuInfo"));
		item.addActionListener(
			new ActionListener() {
				private final String text, title;
				private final int size;
				{
					Dictionary dict = getControl().getDictionary();
					text = lehavre.util.Util.getToolTipText(dict, getBuilding().getProto());
					title = dict.get("popupBuilding");
					size = new GUIHelper("main").getInt("DetailsWidth");
				}
				public void actionPerformed(ActionEvent e) {
					getControl().showInformation(text, title, size);
				}
			}
		);
		add(item);
	}

	/**
	 *	Enables or disables the entry option.
	 *	@param enabled provide true to enable the entry option
	 */
	public void setEntryEnabled(boolean enabled) {
		entry.setEnabled(enabled);
	}

	/**
	 *	Returns the entry menu item.
	 *	@return the entry menu item
	 */
	public JMenuItem getEntryOption() {
		return entry;
	}

	/**
	 *	Enables or disables the ban option.
	 *	@param enabled provide true to enable the ban option
	 */
	public void setBanEnabled(boolean enabled) {
		ban.setEnabled(enabled && building.isBanAllowed());
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

	/**
	 *	Returns the control object.
	 *	@return the control object
	 */
	public LeHavre getControl() {
		return control;
	}

	/**
	 *	Returns the building.
	 *	@return the building
	 */
	public Building getBuilding() {
		return Building.create(building);
	}

	/**
	 *	Performs the special action.
	 */
	protected void doSpecial() {
		LeHavre control = getControl();
		GameState game = control.getGameState();
		Dictionary dict = control.getDictionary();
		Building building = getBuilding();
		StringBuilder message = new StringBuilder();
		ArrayList<Integer> workers = building.getWorkers();
		if(workers.size() > 0) {
			Player player;
			for(Integer index: workers) {
				player = game.getPlayer(index);
				if(message.length() > 0) message.append(", ");
				message.append(String.format(dict.get("popupPlayer"), player.getName(), dict.get("color" + player.getColor())));
			}
			int n = message.lastIndexOf(", ");
			if(n >= 0) message.replace(n, n + 1, " " + dict.get("and"));
		} else {
			message.append(dict.get("nobody"));
		}
		control.showInformation(String.format(dict.get("popupOccupied"), message), dict.get("building" + building));
	}
}