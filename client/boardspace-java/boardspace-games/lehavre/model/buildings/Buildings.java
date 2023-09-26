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


import lehavre.util.*;

/**
 *
 *	The <code>Buildings</code> enum lists all buildings in the game.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.10 2009/12/28
 */
public enum Buildings
{
	//type, hammer, fishing, costs, price, value, enterable, food, franc, bonus

	/* The start buildings. */
	$B1(1, 1, 0, null, 4, 4, true, 0, 0, false),
	$B2(1, 1, 0, null, 6, 6, true, 1, 0, false),
	$B3(3, 1, 0, null, 8, 8, true, 2, 0, false),

	/* The standard buildings. */
	$_01(0, 0, 0, "2w", 6, 6, true, 2, 1, false),
	$_02(3, 0, 0, "1c 1i", 14, 14, true, 0, 0, false),
	$_03(1, 0, 1, "1w 1c", 10, 10, true, 0, 0, false),
	$_04(1, 1, 0, "3w", 8, 8, true, 1, 0, false),
	$_05(1, 0, 0, "2c", 8, 8, true, 1, 0, false),
	$_06(2, 1, 1, "3w 1c", 8, 8, true, 1, 0, false),
	$_07(1, 0, 0, "1c", 8,  8, true, 0, 0, false),
	$_08(1, 0, 1, "2w 1c", 6, 6, true, 2, 1, false),
	$_09(1, 0, 0, "1w 1c 1i", 8, 8, true, 0, 2, false),
	$_10(0, 0, 0, null, 2, 2, true, 1, 0, false),
	$_11(4, 0, 1, "1w 2c", 10, 10, true, 1, 0, false),
	$_12(3, 0, 0, "2w 2c 2i", 14, 14, true, 2, 0, false),
	$_13(0, 0, 0, null, 2, 2, true, 1, 0, false),
	$_14(3, 0, 0, "2w 1c 1i", 14, 14, true, 1, 0, false),
	$_15(4, 0, 0, "3w 2c", 16, 16, true, 0, 0, false),
	$_16(3, 0, 0, "1w 3c", 10, 10, true, 2, 0, false),
	$_17(3, 0, 0, "2w 2c 2i", 14, 14, true, 2, 0, false),
	$_18(2, 0, 1, "2w 3C", 10, 10, true, 2, 0, false),
	$_19(2, 0, 0, "1w 1C", 10, 10, true, 0, 1, false),
	$_20(1, 0, 0, "1w 1C", 12, 12, true, 0, 0, false),
	$_21(2, 1, 1, "4w 1c", 12, 12, true, 0, 1, false),
	$_22(3, 1, 0, "3w 2C", 12, 12, true, 3, 1, false),
	$_23(3, 0, 0, "4C 2i", 22, 22, true, 0, 2, false),
	$_24(2, 1, 0, "2w 2C", 10, 4, false, 0, 0, true),
	$_25(3, 0, 0, "2C 2i", 18, 18, true, 0, 1, false),
	$_26(3, 0, 0, "1w 2C 2i", 24, 10, false, 0, 0, true),
	$_27(0, 0, 0, "3i", 16, 16, true, 0, 2, false),
	$_28(4, 0, 0, "4w 3C", 30, 6, false, 0, 0, true),
	$_29(2, 0, 0, "4C 1I", 40, 16, false, 0, 0, true),
	$_30(4, 0, 0, "5w 3C 1i", -1, 26, true, 0, 0, false),

	/* The special buildings. */
	$_00(4, 0, 0, "1C", 14, 14, false, 0, 0, false),
	$_31(4, 0, 0, "1w 2C 2i", 24, 24, false, 0, 0, false),
	$001(4, 0, 1, null, 6, 6, true, 0, 0, false),
	$002(2, 0, 0, null, 6, 6, true, 1, 0, false),
	$003(2, 0, 0, null, 4, 4, true, 1, 0, false),
	$004(2, 0, 1, null, 8, 8, true, 0, 1, false),
	$005(3, 0, 0, null, 8, 8, true, 2, 1, false),
	$006(0, 1, 0, null, 6, 6, true, 1, 0, false),
	$007(2, 0, 1, null, 4, 4, true, 1, 0, false),
	$008(2, 0, 1, null, 6, 6, true, 1, 0, false),
	$009(0, 0, 1, null, 4, 4, true, 1, 0, false),
	$010(2, 0, 1, null, 4, 4, true, 1, 0, false),
	$011(1, 1, 0, null, 6, 6, true, 1, 0, false),
	$012(0, 1, 0, null, 12, 10, false, 0, 0, true),
	$013(2, 1, 1, null, 8, 4, false, 0, 0, true),
	$014(4, 0, 0, null, 6, 6, true, 1, 0, false),
	$015(3, 0, 0, null, 10, 10, true, 0, 2, false),
	$016(2, 0, 1, null, 6, 6, true, 1, 0, false),
	$017(1, 1, 2, null, 6, 6, true, 1, 0, false),
	$018(2, 0, 0, null, 4, 4, true, 1, 0, false),
	$019(2, 0, 0, null, 6, 6, true, 1, 0, false),
	$020(1, 0, 0, null, 6, 6, true, 1, 0, false),
	$021(3, 0, 0, null, 8, 8, true, 2, 0, false),
	$022(1, 0, 0, null, 6, 6, true, 1, 0, false),
	$023(5, 0, 1, null, 20, 20, false, 0, 0, false),
	$024(2, 0, 0, null, 8, 6, false, 0, 0, false),
	$025(1, 1, 0, null, 10, 8, false, 0, 0, false),
	$026(3, 1, 0, null, 8, 8, true, 2, 0, false),
	$027(0, 0, 0, null, 6, 6, true, 0, 1, false),
	$028(2, 0, 1, null, 4, 4, true, 0, 0, false),
	$029(2, 0, 0, null, 6, 6, true, 1, 0, false),
	$030(1, 0, 0, null, 6, 6, true, 1, 0, false),
	$031(3, 1, 0, null, 8, 8, true, 2, 1, false),
	$032(2, 0, 0, null, 6, 6, true, 1, 0, false),
	$033(0, 0, 0, null, 12, 8, false, 0, 0, false),
	$034(3, 1, 0, null, 8, 8, true, 2, 0, false),
	$035(4, 0, 1, null, 8, 8, true, 0, 1, false),
	$036(4, 0, 0, null, 12, 12, false, 0, 0, false),
	$037(0, 0, 0, null, -1, 0, true, 2, 0, false),
	$038(0, 1, 1, null, 6, 6, true, 0, 0, false),
	$039(5, 0, 0, null, 50, 50, false, 0, 0, false),
	$040(2, 0, 0, null, 4, 4, true, 1, 0, false),
	$041(0, 0, 0, null, 4, 0, false, 0, 0, false),
	$042(2, 1, 0, null, 4, 4, true, 2, 1, false),
	$043(2, 0, 0, null, 4, 4, false, 0, 0, false),
	$044(4, 0, 0, null, 2, 2, true, 0, 1, false),
	$045(4, 0, 0, null, 6, 6, false, 0, 0, false),
	$046(0, 0, 0, null, 4, 4, true, 1, 0, false),

	/* Le Grand Hameau */
	$GH01(1, 1, 0, null, 6, 4, false, 0, 0, false),
	$GH02(1, 1, 0, null, 8, 6, false, 0, 0, false),
	$GH03(4, 0, 0, null, 8, 8, true, 1, 0, false),
	$GH04(2, 0, 0, null, 8, 8, true, 0, 1, false),
	$GH05(1, 0, 0, null, 6, 6, true, 1, 0, false),
	$GH06(1, 0, 0, null, 6, 6, true, 1, 0, false),
	$GH07(1, 0, 1, null, 6, 4, true, 0, 0, false),
	$GH08(1, 0, 1, null, 10, 2, false, 0, 0, false),
	$GH09(4, 0, 0, null, 8, 8, true, 2, 0, false),
	$GH10(2, 0, 0, null, 6, 6, true, 1, 0, false),
	$GH11(1, 0, 0, null, 6, 6, true, 1, 0, false),
	$GH12(3, 0, 0, null, 4, 4, true, 1, 0, false),
	$GH13(0, 1, 1, null, 2, 2, true, 0, 0, false),
	$GH14(2, 0, 0, null, 4, 4, true, 2, 1, false),
	$GH15(3, 0, 0, null, 8, 8, true, 1, 0, false),
	$GH16(2, 0, 0, null, 6, 6, true, 1, 0, false),
	$GH17(3, 0, 0, null, 8, 8, true, 2, 0, false),
	$GH18(4, 0, 0, null, 6, 6, true, 0, 1, false),
	$GH19(2, 0, 0, null, 6, 6, true, 1, 0, false),
	$GH20(2, 0, 0, null, 12, 6, false, 0, 0, false),
	$GH21(2, 0, 0, null, 10, 8, false, 0, 0, false),
	$GH22(3, 0, 0, null, 6, 6, true, 0, 1, false),
	$GH23(3, 0, 0, null, 10, 10, true, 3, 2, false),
	$GH24(3, 1, 0, null, 8, 8, false, 0, 0, false),
	$GH25(2, 0, 0, null, 10, 6, false, 0, 0, true),
	$GH26(2, 0, 0, null, 6, 6, true, 1, 0, false),
	$GH27(0, 0, 0, null, 4, 4, true, 0, 0, false),
	$GH28(2, 0, 0, null, 20, 20, true, 0, 3, false),
	$GH29(2, 0, 0, null, 6, 6, true, 0, 1, false),
	$GH30(1, 0, 0, null, 6, 6, true, 1, 0, false),
	$GH31(4, 0, 0, null, -1, -2, false, 0, 0, false),
	$GH32(2, 0, 0, null, 8, 8, true, 1, 0, false),
	$E2010(0, 0, 0, null, 4, 4, true, 0, 1, false);

	/** Internal enum type: the ship  */
	public enum Type
	{
		Craft, Economic, Industrial, Public, Ship;
	}

	/** Various properties of a building. */
	private final int hammer, fishing, price, value;
	private final double food, franc;

	/** Building usable? Gives bonus points? */
	private final boolean enterable, bonus;

	/** The building costs. */
	private final GoodsList costs;

	/** The building type. */
	private final Type type;

	/** Initializes a constant. */
	Buildings(int type, int hammer, int fishing, String costs, int price, int value, boolean enterable, double food, double franc, boolean bonus) {
		this.type = (type > 0 ? Type.values()[type - 1] : null);
		this.hammer = hammer;
		this.fishing = fishing;
		this.price = price;
		this.value = value;
		this.enterable = enterable;
		this.food = food;
		this.franc = franc;
		this.bonus = bonus;
		this.costs = Util.toList(costs);
	}

	/**
	 *	Returns the building type.
	 *	@return the building type
	 */
	public Buildings.Type getType() {
		return type;
	}

	/**
	 *	Returns true if the building is a bonus building.
	 *	@return true if the building is a bonus building
	 */
	public boolean isBonus() {
		return bonus;
	}

	/**
	 *	Returns true if the building is a start building.
	 *	@return true if the building is a start building
	 */
	public boolean isStart() {
		return ordinal() < 3;
	}

	/**
	 *	Returns true if the building is a standard building.
	 *	@return true if the building is a standard building
	 */
	public boolean isStandard() {
		return !isStart() && ordinal() < 33;
	}

	/**
	 *	Returns true if the building is a special building.
	 *	@return true if the building is a special building
	 */
	public boolean isSpecial() {
		return !isStart() && !isStandard();
	}

	/**
	 *	Returns true if the building is enterable.
	 *	@return true if the building is enterable
	 */
	public boolean isEnterable() {
		return enterable;
	}

	/**
	 *	Returns true if the building is a craft building.
	 *	@return true if the building is a craft building
	 */
	public boolean isCraft() {
		return Type.Craft.equals(type);
	}

	/**
	 *	Returns true if the building is an economic building.
	 *	@return true if the building is an economic building
	 */
	public boolean isEconomic() {
		return Type.Economic.equals(type);
	}

	/**
	 *	Returns true if the building is an industrial building.
	 *	@return true if the building is an industrial building
	 */
	public boolean isIndustrial() {
		return Type.Industrial.equals(type);
	}

	/**
	 *	Returns true if the building is a public building.
	 *	@return true if the building is a public building
	 */
	public boolean isPublic() {
		return Type.Public.equals(type);
	}

	/**
	 *	Returns true if the building is a ship.
	 *	@return true if the building is a ship
	 */
	public boolean isShip() {
		return Type.Ship.equals(type);
	}

	/**
	 *	Returns true if a player can be banned from this building.
	 *	@return true if a player can be banned from this building
	 */
	public boolean isBanAllowed() {
		return !equals($GH03) && !equals($GH04);
	}

	/**
	 *	Returns the index for standard buildings or -1.
	 *	@return the index for standard buildings
	 */
	public int getIndex() {
		String index = toString();
		if(!index.startsWith("_")) return -1;
		return Integer.parseInt(index.substring(1));
	}

	/**
	 *	Returns the amount of hammer symbols.
	 *	@return the amount of hammer symbols
	 */
	public int getHammer() {
		return hammer;
	}

	/**
	 *	Returns the amount of fishing symbols.
	 *	@return the amount of fishing symbols
	 */
	public int getFishing() {
		return fishing;
	}

	/**
	 *	Returns the franc price.
	 *	@return the franc price
	 */
	public int getPrice() {
		return price;
	}

	/**
	 *	Returns the franc value.
	 *	@return the franc value
	 */
	public int getValue() {
		return value;
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
	 *	Returns true if the building can be bought.
	 *	@return true if the building can be bought
	 */
	public boolean isBuyable() {
		return price >= 0;
	}

	/**
	 *	Returns true if the building can be sold.
	 *	@return true if the building can be sold
	 */
	public boolean isSellable() {
		return value >= 0;
	}

	/**
	 *	Returns true if the building can be built.
	 *	@return true if the building can be built
	 */
	public boolean isBuildable() {
		return (costs != null || isShip());
	}

	/**
	 *	Returns the building costs.
	 *	@return the building costs
	 */
	public GoodsList getCosts() {
		if(!isBuildable() || costs == null) return null;
		GoodsList ret = new GoodsList();
		for(GoodsPair pair: costs) ret.add(pair.getAmount(), pair.getGood());
		return ret;
	}

	/**
	 *	Returns the name of the building.
	 *	@return the name
	 */
	public String toString() {
		return super.toString().substring(1);
	}
}