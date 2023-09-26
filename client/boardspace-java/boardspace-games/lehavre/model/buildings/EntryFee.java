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

import lehavre.model.goods.*;
import lehavre.util.*;

/**
 *
 *	The <code>EntryFee</code> class represents the entry fee of a building.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2010/11/28
 */
public final class EntryFee
{
	/** Thee food and franc entry fee. */
	private final double food, franc;

	/**
	 *	Creates a new <code>EntryFee</code> instance.
	 *	@param food the food entry fee
	 *	@param franc the franc entry fee
	 */
	public EntryFee(Quantity<Food> food, Quantity<Franc> franc) {
		this.food = food.doubleValue();
		this.franc = franc.doubleValue();
	}

	/**
	 *	Returns the amount of food to pay for entry.
	 *	@return the amount of food to pay for entry
	 */
	public double getFoodEntry() {
		return food;
	}

	/**
	 *	Returns the amount of francs to pay for entry.
	 *	@return the amount of francs to pay for entry
	 */
	public double getFrancEntry() {
		return franc;
	}

	/**
	 *	Returns the string representation.
	 *	@return the string representation.
	 */
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append(Util.format(food));
		ret.append(" Food / ");
		ret.append(Util.format(franc));
		ret.append(" Franc");
		return ret.toString();
	}
}