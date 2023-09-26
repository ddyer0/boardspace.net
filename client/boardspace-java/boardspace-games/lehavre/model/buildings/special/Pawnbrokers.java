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
package lehavre.model.buildings.special;

import java.util.*;
import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.*;

/**
 *
 *	The <code>Pawnbrokers</code> class represents the Pawnbroker's (040).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Pawnbrokers
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double OUTPUT_FRANC = getProperty("$out", 2);

	/** The instance. */
	private static Pawnbrokers instance = null;

	/** The list of sold goods per player. */
	private final Hashtable<Integer, ArrayList<Good>> list;

	/** Creates a new <code>Pawnbrokers</code> instance. */
	private Pawnbrokers() {
		super(Buildings.$040);
		list = new Hashtable<Integer, ArrayList<Good>>();
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Pawnbrokers getInstance() {
		if(instance == null) instance = new Pawnbrokers();
		return instance;
	}

	/**
	 *	Reads in the fields of the given building.
	 *	@param building the game building
	 */
	@Override
	public void restore(Building building) {
		super.restore(building);
		Pawnbrokers pawnbrokers = (Pawnbrokers)building;
		list.clear();
		list.putAll(pawnbrokers.list);
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		int index = player.getIndex();
		ArrayList<Good> goods = list.get(index);
		GoodsList options = new GoodsList();
		for(Good good: Good.values()) {
			if(!good.isPhysical() || good.isMoney() || player.getGood(good) == 0) continue;
			if(goods == null || !goods.contains(good)) options.add(1, good);
		}
		options = GoodsDialog.showChoiceDialog(control, options, 1, options.size(), true);
		if(goods == null) {
			goods = new ArrayList<Good>();
			list.put(index, goods);
		}
		GoodsList payment = new GoodsList();
		Good good;
		for(GoodsPair pair: options) {
			good = pair.getGood();
			payment.add(-pair.getAmount(), good);
			goods.add(good);
		}
		control.updateBuilding(this);
		payment.add(options.size() * OUTPUT_FRANC, Good.Franc);
		control.receive(payment);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		ArrayList<Good> goods = list.get(player.getIndex());
		for(Good good: Good.values()) {
			if(!good.isPhysical() || good.isMoney() || player.getGood(good) == 0) continue;
			if(goods == null || !goods.contains(good)) return true;
		}
		return false;
	}

	/**
	 *	Creates the dialog to view the sold goods.
	 *	The player may buy any of them back if wished.
	 *	@param control the control object
	 */
	public void showGoods(final LeHavre control) {
		Player player = control.getCurrentPlayer();
		ArrayList<Good> goods = list.get(player.getIndex());
		if(goods == null) {
			control.showWarning("Pawnbrokers");
			return;
		}
		GoodsList options = new GoodsList();
		for(Good good: goods) options.add(1, good);
		int max = (int)Math.min(options.size(), player.getMoney() / OUTPUT_FRANC);
		options = GoodsDialog.showChoiceDialog(control, options, 0, max, false);
		if(options.size() == 0) return;
		GoodsList payment = new GoodsList();
		Good good;
		for(GoodsPair pair: options) {
			good = pair.getGood();
			payment.add(pair.getAmount(), good);
			goods.remove(good);
		}
		payment.add(-options.size() * OUTPUT_FRANC, Good.Franc);
		control.receive(payment);
	}

	/**
	 *	Returns the full description of this object.
	 *	Such a description contains information on all
	 *	its fields and their current values.
	 *	@return the full description
	 */
	@Override
	public String dump() {
		StringBuilder ret = new StringBuilder();
		ret.append(super.dump());
		ret.append(String.format("> List: %s%n", list));
		return ret.toString();
	}
}