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