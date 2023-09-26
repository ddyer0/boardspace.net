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
package lehavre.model.buildings.standard;

import java.util.*;
import lehavre.main.*;
import lehavre.main.Dictionary;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.model.buildings.special.*;
import lehavre.util.*;
import lehavre.view.*;

/**
 *
 *	The <code>ShippingLine</code> class represents the Shipping Line (_18).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class ShippingLine
extends Building
{
	static final long serialVersionUID =1L;
	/** The energy needed per ship. */
	public static final int ENERGY_NEEDED = 3;

	/** The instance. */
	private static ShippingLine instance = null;

	/** Creates a new <code>ShippingLine</code> instance. */
	private ShippingLine() {
		super(Buildings.$_18);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public synchronized static ShippingLine getInstance() {
		if(instance == null) instance = new ShippingLine();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		Dictionary dict = control.getDictionary();
		activateBiofuelFacility(player);

		// get available ships
		ArrayList<Integer> shipsList = new ArrayList<Integer>();
		for(Ship ship: player.getShips()) {
			int amount = ship.getCapacity();
			if(amount > 0) shipsList.add(amount);
		}

		// get possible shipping sizes
		Integer[] values = shipsList.toArray(new Integer[]{});
		Arrays.sort(values);
		shipsList.clear();
		// number of ships player can power
		final int NUM_SHIPS = Math.min(values.length, (int)Math.floor(player.getPotentialEnergy() / ENERGY_NEEDED));
		StringBuilder lines = new StringBuilder();
		String text = dict.get("popupShipping");
		// tell player what shipping options they have
		int k, MAX = 0;
		for(int i = 1; i <= NUM_SHIPS; i++) {
			k = values[values.length - i];
			shipsList.add(k);
			MAX += k; // maximum number of goods that can be shipped
			if(lines.length() > 0) lines.append("<br>");
			lines.append(String.format(text, Util.getNumbered(dict, MAX, "good"), Util.getColored(dict, new GoodsPair(i * ENERGY_NEEDED, Good.Energy))));
		}
		text = String.format("<p>%s</p>", lines);

		// retrieve available goods for sale
		GoodsList goods = new GoodsList();
		for(Good good: Good.values()) {
			if(!good.isPhysical() || good.isMoney()) continue;
			int amount = player.getGood(good);
			if(amount > 0) goods.add(amount, good);
		}

		// get payment
		GoodsList payment = null;
		boolean done = false;
		int shipCount = 0;
		final int SIZE = NUM_SHIPS * DialogWindow.FONT16.getSize();
		do {
			payment = GoodsDialog.showChoiceDialog(control, goods, 1, MAX, true, text, SIZE);
			int amount = 0; // number of goods to be shipped
			for(GoodsPair pair: payment) amount += (int)pair.getAmount();
			shipCount = 0; // number of ships needed
			for(Integer capacity: shipsList) {
				shipCount++;
				amount -= capacity;
				if(amount <= 0) break;
			}
			// check if they can pay the energy cost
			player.lose(payment);
			if(player.getPotentialEnergy() >= shipCount * ENERGY_NEEDED) done = true;
			player.receive(payment);
			// tell them if not
			if(!done) control.showError("Shipping");
		} while(!done);

		// get revenue
		double money = 0;
		for(GoodsPair pair: payment) money += pair.getAmount() * pair.getGood().getFrancValue();

		// pay energy
		player.lose(payment);
		int energy = shipCount * ENERGY_NEEDED;
		GoodsList energyPayment = new GoodsList();
		if(energy > 0) energyPayment.addAll(GoodsDialog.showEnergyDialog(control, energy));
		player.receive(payment);

		// add energy payment
		payment.addAll(energyPayment);
		payment.optimize();

		// finalize transaction
		GoodsList ret = new GoodsList();
		ret.add(money, Good.Franc);
		for(GoodsPair pair: payment) ret.add(-pair.getAmount(), pair.getGood());
		control.receive(ret);
		control.tellEnergyPayment(energyPayment);
		PiratesLair piratesLair = PiratesLair.getInstance();
		int index = piratesLair.getOwner();
		if(index >= 0 && index != player.getIndex()) control.payDuty(index, shipCount * piratesLair.FEE);
		deactivateBiofuelFacility();
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		activateBiofuelFacility(player);
		boolean ships = false;
		for(Ship ship: player.getShips()) {
			if(ship.getCapacity() > 0) {
				ships = true;
				break;
			}
		}
		boolean ret = false;
		if(ships && player.getPotentialEnergy() >= ENERGY_NEEDED) {
			ArrayList<Double> list = new ArrayList<Double>();
			for(Good good: Good.values()) {
				if(!good.isPhysical() || good.isMoney()) continue;
				int amount = player.getGood(good);
				if(amount == 0) continue;
				double energy = good.getEnergyValue();
				// found good that is not energy, i. e. that can be sold
				if(energy == 0) {
					ret = true;
					break;
				}
				// otherwise save energy value
				for(int i = 0; i < amount; i++) list.add(energy);
			}
			if(!ret) {
				// sort energy values in descending order
				Double[] values = list.toArray(new Double[]{});
				Arrays.sort(values);
				list.clear();
				for(int i = values.length - 1; i >= 0; i--) list.add(values[i]);
				double energy = 0;
				// now check whether there will be goods left for sale
				double delta = ENERGY_NEEDED - (player.getPotentialEnergy() - player.getEnergy());
				while(list.size() > 0) {
					energy += list.remove(0);
					// at least one good left and energy goal achieved
					if(list.size() > 0 && energy >= delta) {
						ret = true;
						break;
					}
				}
			}
		}
		deactivateBiofuelFacility();
		return ret;
	}

	/**
	 *	Changes the energy value of Grain if the given player
	 *	owns the Biofuel Facility.
	 *	@param player the player
	 */
	private void activateBiofuelFacility(Player player) {
		synchronized(Good.Grain) {
			BiofuelFacility bf = BiofuelFacility.getInstance();
			if(player.owns(bf)) Good.Grain.setEnergyValue(bf.ENERGY_PER_GRAIN);
		}
	}

	/**
	 *	Restores the energy value of Grain if necessary.
	 */
	private void deactivateBiofuelFacility() {
		synchronized(Good.Grain) {
			Good.Grain.restoreEnergyValue();
		}
	}
}