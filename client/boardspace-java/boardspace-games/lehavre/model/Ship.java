package lehavre.model;
import lehavre.util.*;

/**
 *
 *	The <code>Ship</code> enum lists all ships in the game.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/18
 */
public enum Ship
implements Buildable
{
	S01(0, 2),
	S02(0, 2),
	S03(0, 2),
	S04(0, 4),
	S05(0, 4),
	S06(1, 2),
	S07(0, 6),
	S08(1, 4),
	S09(0, 6),
	S10(1, 6),
	S11(1, 8),
	S12(2, 10),
	S13(1, 10),
	S14(2, 16),
	S15(1, 12),
	S16(2, 20),
	S17(2, 24),
	S18(3, 38),
	S19(3, 34),
	S20(3, 30);

	/** Internal enum type: the ship type. */
	public enum Type
	{
		Wooden("3e 5w", 14, new int[]{5, 4, 3, 2, 1}, 2),
		Iron("3e 4i", 20, new int[]{7, 5, 4, 3, 2}, 3),
		Steel("3e 2I", 30, new int[]{10, 7, 6, 5, 3}, 4),
		Luxury("3e 3I", -1, null, 0);

		/** The price and capacity. */
		private final int price, capacity;

		/** The food supply array. */
		private final int[] food;

		/** The costs in goods. */
		private final GoodsList costs;

		/** Initializes a constant. */
		Type(String costs, int price, int[] food, int capacity) {
			this.price = price;
			this.food = food;
			this.capacity = capacity;
			//String[] parts = costs.split(" ");
			this.costs = Util.toList(costs);
		}
	}

	/** The number of players. */
	private int num;

	/** The value. */
	private final int value;

	/** The ship type. */
	private final Type type;

	/** Initializes a constant. */
	Ship(int type, int value) {
		this.value = value;
		this.type = Type.values()[type];
	}

	/**
	 *	Sets the number of players to the given value.
	 *	@param num the number of players
	 */
	public void init(int num) {
		this.num = num - 1;
	}

	/**
	 *	Returns the type.
	 *	@return the type
	 */
	public Type getType() {
		return type;
	}

	/**
	 *	Returns the value.
	 *	@return the value
	 */
	public int getValue() {
		return value;
	}

	/**
	 *	Returns the price.
	 *	@return the price
	 */
	public int getPrice() {
		return type.price;
	}

	/**
	 *	Returns true if the ship can be bought.
	 *	@return true if the ship can be bought
	 */
	public boolean isBuyable() {
		return type.price >= 0;
	}

	/**
	 *	Returns the building costs.
	 *	@return the building costs
	 */
	public GoodsList getCosts() {
		return type.costs;
	}

	/**
	 *	Returns the capacity.
	 *	@return the capacity
	 */
	public int getCapacity() {
		return type.capacity;
	}

	/**
	 *	Returns the food supply.
	 *	@return the food supply
	 */
	public int getFoodSupply() {
		return (type.food != null ? type.food[num] : 0);
	}

	/**
	 *	Returns true if this is a wooden ship.
	 *	@return true if this is a wooden ship
	 */
	public boolean isWooden() {
		return type.equals(Type.Wooden);
	}

	/**
	 *	Returns true if this is an iron ship.
	 *	@return true if this is an iron ship
	 */
	public boolean isIron() {
		return type.equals(Type.Iron);
	}

	/**
	 *	Returns true if this is a steel ship.
	 *	@return true if this is a steel ship
	 */
	public boolean isSteel() {
		return type.equals(Type.Steel);
	}

	/**
	 *	Returns true if this is a luxury liner.
	 *	@return true if this is a luxury liner
	 */
	public boolean isLuxury() {
		return type.equals(Type.Luxury);
	}

	/**
	 *	Returns the full description of this object.
	 *	Such a description contains information on all
	 *	its fields and their current values.
	 *	@return the full description
	 */
	public String dump() {
		StringBuilder ret = new StringBuilder();
		ret.append(String.format("%s %s%n", getClass().getSimpleName(), this));
		ret.append(String.format("> Type: %s%n", getType()));
		ret.append(String.format("> Value: %s%n", getValue()));
		ret.append(String.format("> Price: %s%n", getPrice()));
		ret.append(String.format("> Costs: %s%n", getCosts()));
		ret.append(String.format("> Capacity: %s%n", getCapacity()));
		ret.append(String.format("> FoodSupply: %s%n", getFoodSupply()));
		ret.append(String.format("> isBuyable: %s%n", isBuyable()));
		ret.append(String.format("> isWooden: %s%n", isWooden()));
		ret.append(String.format("> isIron: %s%n", isIron()));
		ret.append(String.format("> isSteel: %s%n", isSteel()));
		ret.append(String.format("> isLuxury: %s%n", isLuxury()));
		return ret.toString();
	}
}