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
package lehavre.util;

import java.util.ArrayList;
import lehavre.model.Good;

/**
 *
 *	The <code>GoodsList</code> class is a regular <code>ArrayList</code>
 *	holding <code>Pair</code>s of goods and integers, i.e. amounts of goods.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/2/4
 */
public final class GoodsList
extends ArrayList<GoodsPair>
{
	static final long serialVersionUID =1L;
	/**
	 *	Adds the given pair of data to the list.
	 *	@param amount the amount of the given good
	 *	@param good the good
	 *	@return true
	 */
	public boolean add(double amount, Good good) {
		return super.add(new GoodsPair(amount, good));
	}

	/**
	 *	Returns true if the given list is equal to given object.
	 *	@param object the object
	 */
	@Override
	public boolean equals(Object object) {
		if(!(object instanceof GoodsList)) return false;
		GoodsList list = new GoodsList();
		list.addAll((GoodsList)object);
		for(GoodsPair pair: this) list.add(pair.negate());
		list.optimize();
		return (list.size() == 0);
	}

	/**
	 *	Optimizes the list.
	 */
	public synchronized void optimize() {
		Good[] values = Good.values();
		double[] goods = new double[values.length];
		for(GoodsPair pair: this) goods[pair.getGood().ordinal()] += pair.getAmount();
		clear();
		double amount;
		for(Good good: values) {
			amount = goods[good.ordinal()];
			if(amount != 0) add(amount, good);
		}
	}
}