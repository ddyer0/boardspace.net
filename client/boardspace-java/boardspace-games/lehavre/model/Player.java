package lehavre.model;

import java.util.*;

import lehavre.main.AddressInterface;
import lehavre.model.buildings.*;
import lehavre.model.buildings.special.*;
import lehavre.util.*;
import lib.SimpleObservable;

/**
 *
 *	The <code>Player</code> class represents the physical player.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.10 2009/12/28
 */
public final class Player
extends SimpleObservable
implements Cloneable, Comparable<Player>, java.io.Serializable
{
	static final long serialVersionUID =1L;

	/**
	 *	The user name.
	 *	Players can set their names in the login window.
	 */
	private String name;

	/**
	 *	The login index. Indicates when the player logged in.
	 *	0 = the game creator, etc.
	 */
	private int index;

	/**
	 *	The network address.
	 *	@see AddressInterface
	 */
	private AddressInterface address;

	/**
	 *	The position within the play order.
	 *	0 = the start player, etc.
	 */
	private int seat;

	/**
	 *	The color of their game pieces.
	 *	<code>PlayerColor</code> is an enumeration
	 *	holding the available values.
	 */
	private PlayerColor color;

	/**
	 *	True if the player is ready (with whatever).
	 *	Used in the network communication to indicate
	 *	the player is done with the current action,
	 *	e. g. paying food at the end of the round.
	 *	Will be set in the game control class and
	 *	unset in the main window automatically.
	 */
	private boolean ready;

	/**
	 *	The available goods.
	 *	The goods indication is defined in the
	 *	<code>Good</code> enumeration.
	 */
	private final int[] goods;

	/**
	 *	The buildings owned by the player.
	 *	There is no particular order.
	 */
	private final ArrayList<Buildings> buildings;

	/**
	 *	The ships owned by the player.
	 *	There is no particular order.
	 */
	private final ArrayList<Ship> ships;

	/**
	 *	The number of loan cards.
	 *	@see takeLoan
	 *	@see returnLoan
	 */
	private int loans;

	/**
	 *	The location of the worker disc.
	 *	The <code>Building</code> class has a corresponding
	 *	field that saves the current player in the building.
	 */
	private Buildings location;

	/**
	 *	Number of building actions left.
	 *	Used to handle the Construction Firm.
	 */
	private transient int builds;

	/**
	 *	Number of main actions left.
	 *	Used to handle the action phase.
	 */
	private transient int actions;

	/**
	 *	True if player is allowed to take an Offer or
	 *	visit a building as an action, respectively.
	 */
	private transient boolean offerAllowed, buildingAllowed;

	/**
	 *	The special building cards on the player's hand.
	 *	You get hand cards on the construction site.
	 */
	private ArrayList<Buildings> handcards = null;

	/**
	 *	Creates a new <code>Player</code> instance for the
	 *	player with the given address, index and name.
	 *	@param address the address
	 *	@param index the index
	 *	@param name the name
	 */
	public Player(AddressInterface address, int index, String name) {
		this.address = address;
		this.index = index;
		this.name = name;
		seat = -1;
		color = null;
		ready = false;
		goods = new int[Good.values().length];
		buildings = new ArrayList<Buildings>();
		ships = new ArrayList<Ship>();
		loans = 0;
		location = null;
		builds = 0;
		actions = 0;
		offerAllowed = false;
		buildingAllowed = false;
	}

	/**
	 *	Clones the object and all its fields.
	 *	Returns the copy of this object.
	 *	@return the copy
	 */
	@Override
	public synchronized Player clone() {
		Player player = new Player(address, index, name);
		player.color = color;
		player.seat = seat;
		player.ready = ready;
		player.loans = loans;
		player.location = location;
		player.buildings.addAll(buildings);
		player.ships.addAll(ships);
		for(int i = 0; i < goods.length; i++) player.goods[i] = goods[i];
		if(handcards != null) {
			player.handcards = new ArrayList<Buildings>();
			player.handcards.addAll(handcards);
		}
		return player;
	}

	/**
	 *	Returns true if this player is equal to the given object.
	 *	Two players are considered equal if they concur in their
	 *	address, user name and login index.
	 *	@param o the object to compare
	 *	@return true if this player is equal to the given object
	 */
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Player)) return false;
		Player p = (Player)o;
		return address.equals(p.address) && name.equals(p.name) && (index == p.index);
	}
	public int hashCode()
	{
		return(address.hashCode()^name.hashCode()^index);
	}

	/**
	 *	Compares this player to the given one. Returns a negative
	 *	integer, zero or a positive integer if this player's rank
	 *	is smaller than, equal to or greater than the given one.
	 *	@param player the player to compare
	 *	@return -1, 0 or +1 if this player's rank is smaller than,
	 *			equal to or greater than the given one
	 */
	public int compareTo(Player player) {
		return (player.getPoints() - getPoints());
	}

	/**
	 *	Returns the network address.
	 *	@return the network address
	 */
	public AddressInterface getAddress() {
		return address;
	}

	/**
	 *	Sets the network address to the given value.
	 *	@param address the new network address
	 */
	public void setAddress(AddressInterface address) {
		this.address = address;
	}

	/**
	 *	Returns the login index.
	 *	@return the login index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 *	Returns the user name.
	 *	@return the user name
	 */
	public String getName() {
		return name;
	}

	/**
	 *	Returns the player color.
	 *	@return the player color
	 */
	public PlayerColor getColor() {
		return color;
	}

	/**
	 *	Sets the color to the given value.
	 *	@param color the new color
	 */
	public void setColor(PlayerColor color) {
		this.color = color;
	}

	/**
	 *	Returns the player's seat.
	 *	@return the seat
	 */
	public int getSeat() {
		return seat;
	}

	/**
	 *	Sets the seat to the given value.
	 *	@param seat the new seat
	 */
	public void setSeat(int seat) {
		this.seat = seat;
	}

	/**
	 *	Receives the given goods.
	 *	@param goods the goods
	 */
	public void receive(GoodsList goods) {
		for(GoodsPair pair: goods) receive((int)pair.getAmount(), pair.getGood());
	}

	/**
	 *	Loses the given goods.
	 *	@param goods the goods
	 */
	public void lose(GoodsList goods) {
		for(GoodsPair pair: goods) lose((int)pair.getAmount(), pair.getGood());
	}

	/**
	 *	Receives the given amount of goods of the given type.
	 *	@param amount the amount received
	 *	@param type the good type
	 */
	public void receive(int amount, Good type) {
		setGood(type, getGood(type) + amount);
	}

	/**
	 *	Loses the given amount of goods of the given type.
	 *	@param amount the amount received
	 *	@param type the good type
	 */
	public void lose(int amount, Good type) {
		setGood(type, getGood(type) - amount);
	}

	/**
	 *	Returns the amount of goods of the given type.
	 *	@param type the type
	 *	@return the amount of goods
	 */
	public int getGood(Good type) {
		return goods[type.ordinal()];
	}

	/**
	 *	Returns the amount of goods of the given type,
	 *	including corresponding goods.
	 *	@param type the type
	 *	@return the amount of goods
	 */
	public double getGood2(Good type) {
		double ret = getGood(type);
		if(type.equals(Good.Clay)) ret += getGood(Good.Brick);
		else if(type.equals(Good.Iron)) ret += getGood(Good.Steel);
		else if(type.equals(Good.Energy)) ret = getPotentialEnergy();
		else if(type.equals(Good.Food)) ret = getFood();
		return ret;
	}

	/**
	 *	Sets the amount of goods of the given type to the given value.
	 *	@param type the type
	 *	@param amount the new value
	 */
	public void setGood(Good type, int amount) {
		int i = type.ordinal();
		int prev = goods[i];
		goods[i] = amount;
		setChanged(String.format("good=%s,%d,%d", type, prev, amount));
	}

	/**
	 *	Returns the amount of money the player has.
	 *	@return the amount of money
	 */
	public int getMoney() {
		return getGood(Good.Franc);
	}

	/**
	 *	Returns the amount of food the player has.
	 *	Money is already included in this value.
	 *	@return the amount of food
	 */
	public double getFood() {
		double n = 0;
		for(Good good: Good.values()) n += good.getFoodValue() * goods[good.ordinal()];
		return n;
	}

	/**
	 *	Returns the amount of food from the ships.
	 *	@return the amount of food
	 */
	public int getFoodSupply() {
		int n = 0;
		for(Ship ship: ships) n += ship.getFoodSupply();
		SoupKitchen kitchen = SoupKitchen.getInstance();
		if(owns(kitchen)) n += kitchen.getFoodSupply();
		return n;
	}

	/**
	 *	Returns the amount of energy the player has.
	 *	@return the amount of energy
	 */
	public double getEnergy() {
		double n = 0;
		for(Good good: Good.values()) n += good.getEnergyValue() * goods[good.ordinal()];
		return n;
	}

	/**
	 *	Returns the amount of loans.
	 *	@return the amount of loans
	 */
	public int getLoans() {
		return loans;
	}

	/**
	 *	Receives the given building.
	 *	@param building the building
	 */
	public void receive(Building building) {
		buildings.add(building.getProto());
		building.setOwner(index);
	}

	/**
	 *	Loses the given building.
	 *	@param building the building
	 */
	public void lose(Building building) {
		buildings.remove(building.getProto());
		building.setOwner(-1);
	}

	/**
	 *	Returns true if the player owns the given building.
	 *	@param building the building
	 *	@return true if the player owns the given building
	 */
	public boolean owns(Building building) {
		return buildings.contains(building.getProto());
	}

	/**
	 *	Receives the given ship.
	 *	@param ship the ship
	 */
	public void receive(Ship ship) {
		ships.add(ship);
	}

	/**
	 *	Loses the given ship.
	 *	@param ship the ship
	 */
	public void lose(Ship ship) {
		ships.remove(ship);
	}

	/**
	 *	Returns true if the player owns a ship of the given type.
	 *	@param type the type
	 *	@return true if the player owns a ship of the given type
	 */
	public boolean owns(Ship.Type type) {
		for(Ship ship: ships) if(ship.getType().equals(type)) return true;
		return false;
	}

	/**
	 *	Returns the list of buildings.
	 *	@return the list of buildings
	 */
	public ArrayList<Building> getBuildings() {
		ArrayList<Building> ret = new ArrayList<Building>();
		for(Buildings building: buildings) ret.add(Building.create(building));
		return ret;
	}

	/**
	 *	Returns the list of ships.
	 *	@return the list of ships
	 */
	public ArrayList<Ship> getShips() {
		ArrayList<Ship> ret = new ArrayList<Ship>();
		ret.addAll(ships);
		return ret;
	}

	/**
	 *	Takes one loan.
	 */
	public void takeLoan() {
		loans++;
	}

	/**
	 *	Returns one loan.
	 */
	public void returnLoan() {
		loans--;
	}

	/**
	 *	Returns the worker's location.
	 *	@return the worker's location
	 */
	public Building getLocation() {
		return (location != null ? Building.create(location) : null);
	}

	/**
	 *	Moves the worker to the given location.
	 *	@param location the new location
	 */
	public void setLocation(Building location) {
		this.location = (location != null ? location.getProto() : null);
	}

	/**
	 *	Sets the player to be ready.
	 *	@param ready provide true if the player is ready
	 */
	public void setReady(boolean ready) {
		this.ready = ready;
	}

	/**
	 *	Returns true if the player is ready.
	 *	@return true if the player is ready
	 */
	public boolean isReady() {
		return ready;
	}

	/**
	 *	Sets the number of building actions to the given value.
	 *	@param builds the new number of building actions
	 */
	public synchronized void setBuilds(int builds) {
		this.builds = builds;
	}

	/**
	 *	Returns the number of building actions left.
	 *	@return the number of building actions
	 */
	public synchronized int getBuilds() {
		return builds;
	}

	/**
	 *	Sets the number of main actions to the given value.
	 *	@param actions the new number of main actions
	 */
	public synchronized void setActions(int actions) {
		this.actions = actions;
		setChanged(String.format("actions=%d", actions));
	}

	/**
	 *	Returns the number of main actions left.
	 *	@return the number of main actions
	 */
	public synchronized int getActions() {
		return actions;
	}

	/**
	 *	Sets whether taking an Offer is allowed.
	 *	@param allowed provide true to allow it
	 */
	public void setOfferAllowed(boolean allowed) {
		this.offerAllowed = allowed;
	}

	/**
	 *	Returns true if taking an Offer is allowed.
	 *	@return true if taking an Offer is allowed
	 */
	public boolean isOfferAllowed() {
		return offerAllowed;
	}

	/**
	 *	Sets whether visting a building is allowed.
	 *	@param allowed provide true to allow it
	 */
	public void setBuildingAllowed(boolean allowed) {
		this.buildingAllowed = allowed;
	}

	/**
	 *	Returns true if visting a building is allowed.
	 *	@return true if visting a building is allowed
	 */
	public boolean isBuildingAllowed() {
		return buildingAllowed;
	}

	/**
	 *	Returns the amount of food the player could have
	 *	by selling all of their buildings and ships.
	 *	@return the potential amount of food
	 */
	public double getPotentialFood() {
		return getFood() + getPotentialMoney() - getMoney();
	}

	/**
	 *	Returns the amount of energy the player has,
	 *	counting the Wind Farm bonus if owned.
	 *	@return the potential amount of energy
	 */
	public double getPotentialEnergy() {
		WindFarm windFarm = WindFarm.getInstance();
		return getEnergy() + (owns(windFarm) ? windFarm.OUTPUT_ENERGY : 0);
	}

	/**
	 *	Returns the amount of money the player could have
	 *	by selling all of their buildings and ships.
	 *	@return the potential amount of money
	 */
	public int getPotentialMoney() {
		int n = getMoney();
		for(Buildings building: buildings) n += building.getValue() / 2;
		for(Ship ship: ships) n += ship.getValue() / 2;
		return n;
	}

	/**
	 *	Returns the current number of victory points.
	 *	Loans are included as well as any bonus points through
	 *	buildings in the player's possession. Loans reduce the
	 *	overalln value by their penalty value (7 francs).
	 *	@return the current number of victory points
	 */
	public int getPoints() {
		int n = getMoney();
		for(Buildings building: buildings) {
			n += building.getValue();
			if(building.isBonus()) n += Building.create(building).getBonus(this);
		}
		for(Ship ship: ships) n += ship.getValue();
		n -= GameState.LOAN_PENALTY * loans;
		return n;
	}

	/**
	 *	Receives the given special buildings as hand cards.
	 *	@param buildings the special buildings
	 */
	public void setHandCards(ArrayList<Buildings> buildings) {
		if(buildings != null) {
			handcards = new ArrayList<Buildings>();
			handcards.addAll(buildings);
		} else handcards = null;
	}

	/**
	 *	Returns the hand cards.
	 *	@return the hand cards
	 */
	public ArrayList<Buildings> getHandCards() {
		ArrayList<Buildings> ret = new ArrayList<Buildings>();
		ret.addAll(handcards);
		return ret;
	}

	/**
	 *	Returns the string representation of the object.
	 *	@return the string representation
	 */
	@Override
	public String toString() {
		return String.format("%d|%s (%s, %d.)", index, name, color, seat);
	}

	/**
	 *	Returns the full description of this object.
	 *	Such a description contains information on all
	 *	its fields and their current values.
	 *	@return the full description
	 */
	public String dump() {
		StringBuilder ret = new StringBuilder();
		ret.append(String.format("%s %d%n", getClass().getSimpleName(), index));
		ret.append(String.format("> Name: %s%n", name));
		ret.append(String.format("> Address: %s%n", address));
		ret.append(String.format("> Color: %s%n", color));
		ret.append(String.format("> Seat: %s%n", seat));
		ret.append(String.format("> Location: %s%n", location));
		ret.append(String.format("> Buildings: %s%n", buildings));
		ret.append(String.format("> Ships: %s%n", ships));
		StringBuilder msg = new StringBuilder();
		for(Good good: Good.values()) {
			if(!good.isPhysical()) continue;
			if(msg.length() > 0) msg.append(", ");
			msg.append(getGood(good));
		}
		ret.append(String.format("> Goods: [%s]%n", msg));
		ret.append(String.format("> Energy: %s%n", getEnergy()));
		ret.append(String.format("> PotEnergy: %s%n", getPotentialEnergy()));
		ret.append(String.format("> Food: %s%n", getFood()));
		ret.append(String.format("> PotFood: %s%n", getPotentialFood()));
		ret.append(String.format("> FoodSupply: %s%n", getFoodSupply()));
		ret.append(String.format("> Money: %s%n", getMoney()));
		ret.append(String.format("> PotMoney: %s%n", getPotentialMoney()));
		ret.append(String.format("> Points: %s%n", getPoints()));
		ret.append(String.format("> Ready: %s%n", ready));
		ret.append(String.format("> Loans: %s%n", loans));
		ret.append(String.format("> Builds: %s%n", builds));
		ret.append(String.format("> Actions: %s%n", actions));
		ret.append(String.format("> OfferAllowed: %s%n", offerAllowed));
		ret.append(String.format("> BuildingAllowed: %s%n", buildingAllowed));
		ret.append(String.format("> Handcards: %s%n", handcards));
		ret.append(String.format("> Observers: %s%n", countObservers()));
		return ret.toString();
	}
}