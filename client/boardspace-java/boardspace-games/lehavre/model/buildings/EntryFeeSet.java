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
package lehavre.model.buildings;

import java.util.*;
import lehavre.model.*;
import lehavre.util.*;

/**
 *
 *	The <code>EntryFeeSet</code> class collects all possible
 *	combinations for a given <code>Player</code> to pay the
 *	entry fee of a given <code>Building</code>.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2010/11/28
 */
public final class EntryFeeSet
extends HashSet<GoodsList>
{
	static final long serialVersionUID =1L;
	/**
	 *	Creates a new <code>EntryFeeSet</code> instance.
	 *	@param building the building which provides the entry fee
	 *	@param player the player to pay the entry fee
	 */
	public EntryFeeSet(Building building, Player player) {
		EntryFee fee = building.getEntryFee();
		final double food = fee.getFoodEntry();
		final double franc = fee.getFrancEntry();
		// there is something to pay
		if(food > 0 || franc > 0) {
			// food entry available that the player can pay
			if(food > 0 && player.getFood() >= food) {
				// get player's food
				ArrayList<Good> foodList = new ArrayList<Good>();
				for(Good good: Good.values()) {
					if(good.getFoodValue() > 0) {
						int amount = player.getGood(good);
						if(amount > 0) for(int i = 0; i < amount; i++) foodList.add(good);
					}
				}
				// sort goods by food value
				Good[] goods = foodList.toArray(new Good[]{});
				Arrays.sort(goods, new Comparator<Good>() {
					public int compare(Good a, Good b) {
						double delta = a.getFoodValue() - b.getFoodValue();
						if(delta > 0) return -1;
						if(delta < 0) return 1;
						return 0;
					}
				});
				foodList.clear();
				for(Good good: goods) foodList.add(good);
				// go recursively through all combinations of potential food entry payments
				foodRecursion(this, new ArrayList<Good>(), foodList, food);
			}
			// franc entry available that the player can pay
			if(franc > 0 && player.getMoney() >= franc) {
				// remove any food payment containing at least the franc payment
				Iterator<GoodsList> iterator = iterator();
				while(iterator.hasNext()) {
					GoodsList list = iterator.next();
					for(GoodsPair pair: list) {
						// only check francs
						if(Good.Franc.equals(pair.getGood())) {
							// overpaid with francs
							if(pair.getAmount() >= franc) {
								iterator.remove();
								break;
							}
						}
					}
				}
				// add franc payment
				GoodsList list = new GoodsList();
				list.add(franc, Good.Franc);
				add(list);
			}
		// nothing to pay at all
		} else add(null);
	}

	/**
	 *	Performs one step of the food payment recursion.
	 *	@param payments the overall list of potential payments
	 *	@param tempList the list containing the current, but still incomplete set of goods
	 *	@param foodList the seed list of available food
	 *	@param demand the remaining food demand
	 */
	private static void foodRecursion(HashSet<GoodsList> payments, ArrayList<Good> tempList, ArrayList<Good> foodList, double demand) {
		// stop recursion if no food left or no to pay
		if(foodList.size() == 0 || demand == 0) return;
		// copy tempList for further recursion
		ArrayList<Good> tempListCopy = new ArrayList<Good>();
		tempListCopy.addAll(tempList);
		// otherwise retrieve next good and add it to tempList
		Good current = foodList.remove(0);
		tempList.add(current);
		// copy foodList for further recursion
		ArrayList<Good> foodListCopy = new ArrayList<Good>();
		foodListCopy.addAll(foodList);
		// check if the current good satisfies the food demand
		double value = current.getFoodValue();
		if(value >= demand) {
			GoodsList list = new GoodsList();
			for(Good good: tempList) list.add(1, good);
			list.optimize();
			payments.add(list);
			// check next candidate
			tempList.remove(tempList.size() - 1);
			foodRecursion(payments, tempList, foodListCopy, demand);
		// otherwise take another step
		} else foodRecursion(payments, tempList, foodListCopy, demand - value);
		// continue recursion with next goods
		foodRecursion(payments, tempListCopy, foodListCopy, demand);
	}
}