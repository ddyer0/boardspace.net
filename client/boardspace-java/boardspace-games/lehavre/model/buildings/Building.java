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

import java.io.*;
import java.util.*;
import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.special.*;
import lehavre.model.buildings.standard.*;
import lehavre.model.buildings.start.*;
import lehavre.model.goods.Food;
import lehavre.model.goods.Franc;
import lehavre.util.*;
import lib.SimpleObservable;

/**
 *
 *	The <code>Building</code> class is the abstract super-class
 *	of all the buildings in the game.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.10 2009/12/28
 */
public abstract class Building
extends SimpleObservable
implements Buildable, Comparable<Building>
{
	static final long serialVersionUID =1L;
	/** The path to the file containing the building data. */
	private static final String CONFIG_PATH = "lehavre/config/buildings.txt";

	/** The building properties. */
	private static final Hashtable<String, Double> PROPERTIES = new Hashtable<String, Double>();

	/**
	 *	Returns the property for the given key or the default value
	 *	if no value is assigned to that key.
	 *	@param key the key
	 *	@param def the default value
	 */
	protected final double getProperty(String key, double def) {
		Double value = PROPERTIES.get(String.format("%s_%s", proto, key));
		return (value != null ? value : def);
	}

	static {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(CONFIG_PATH));
			String line;
			while((line = reader.readLine()) != null) {
				if(line.startsWith("#") || line.trim().length() == 0) continue;
				String[] pair = line.split("\\s+");
				PROPERTIES.put(pair[0], Double.parseDouble(pair[1]));
			}
			reader.close();
		} catch(Exception e) {}
	}

	/** The <code>Observable</code> constants. */
	public static final int OCCUPIED = 0;
	public static final int MODERNISED = 1;

	/** The building Kulami. */
	private final Buildings proto;

	/** The owner's index. */
	private int owner;

	/** The worker's indices. */
	private ArrayList<Integer> workers;

	/** True if already built. */
	private boolean built;

	/** True if already modernized. */
	private boolean modernised;

	/**
	 *	Creates a new <code>Building</code> instance to match
	 *	the given Kulami.
	 *	@param Kulami the Kulami
	 */
	protected Building(Buildings p) {
		this.proto = p;
		owner = -1;
		workers = new ArrayList<Integer>();
		built = false;
	}

	/**
	 *	Reads in the fields of the given building.
	 *	@param building the building
	 */
	public void restore(Building building) {
		owner = building.owner;
		workers.clear();
		workers.addAll(building.getWorkers());
		built = building.built;
		modernised = building.modernised;
	}

	/**
	 *	Factory method to create a new building object.
	 *	Returns a new building from the one.
	 *	@param building the building
	 *	@return the new building
	 */
	public static Building create(Building building) {
		return (building != null ? create(building.proto) : null);
	}

	/**
	 *	Factory method to create a new building object.
	 *	Returns a new building from the given Kulami.
	 *	@param Kulami the Kulami
	 *	@return the new building
	 */
	public static Building create(Buildings proto) {
		switch(proto) {
			case $B1: return BuildingFirm1.getInstance();
			case $B2: return BuildingFirm2.getInstance();
			case $B3: return ConstructionFirm.getInstance();
			case $_00: return Dunny.getInstance();
			case $_01: return Marketplace.getInstance();
			case $_02: return Sawmill.getInstance();
			case $_03: return Fishery.getInstance();
			case $_04: return Joinery.getInstance();
			case $_05: return Bakehouse.getInstance();
			case $_06: return HardwareStore.getInstance();
			case $_07: return CharcoalKiln.getInstance();
			case $_08: return Smokehouse.getInstance();
			case $_09: return Abattoir.getInstance();
			case $_10: return ClayMound.getInstance();
			case $_11: return ArtsCentre.getInstance();
			case $_12: return Wharf1.getInstance();
			case $_13: return BlackMarket.getInstance();
			case $_14: return Brickworks.getInstance();
			case $_15: return LocalCourt.getInstance();
			case $_16: return Colliery.getInstance();
			case $_17: return Wharf2.getInstance();
			case $_18: return ShippingLine.getInstance();
			case $_19: return GroceryMarket.getInstance();
			case $_20: return Tannery.getInstance();
			case $_21: return BusinessOffice.getInstance();
			case $_22: return Ironworks.getInstance();
			case $_23: return SteelMill.getInstance();
			case $_24: return Storehouse.getInstance();
			case $_25: return Cokery.getInstance();
			case $_26: return Dock.getInstance();
			case $_27: return BridgeOverTheSeine.getInstance();
			case $_28: return TownHall.getInstance();
			case $_29: return Bank.getInstance();
			case $_30: return Church.getInstance();
			case $_31: return FootballStadium.getInstance();
			case $001: return LabourExchange.getInstance();
			case $002: return Bakery.getInstance();
			case $003: return BaguetteShop.getInstance();
			case $004: return Farm.getInstance();
			case $005: return ClothingIndustry.getInstance();
			case $006: return IronMineAndCoalSeam.getInstance();
			case $007: return FishMarket.getInstance();
			case $008: return FishRestaurant.getInstance();
			case $009: return FishpondAndWood.getInstance();
			case $010: return ForestHut.getInstance();
			case $011: return PlantNursery.getInstance();
			case $012: return BusinessPark.getInstance();
			case $013: return Guildhouse.getInstance();
			case $014: return HarbourWatch.getInstance();
			case $015: return Smelter.getInstance();
			case $016: return Diner.getInstance();
			case $017: return HuntingLodge.getInstance();
			case $018: return CoalTrader.getInstance();
			case $019: return Patisserie.getInstance();
			case $020: return Furriery.getInstance();
			case $021: return LeatherIndustry.getInstance();
			case $022: return Kiln.getInstance();
			case $023: return LuxuryYacht.getInstance();
			case $024: return Feedlot.getInstance();
			case $025: return MasonsGuild.getInstance();
			case $026: return FurnitureFactory.getInstance();
			case $027: return TownSquare.getInstance();
			case $028: return Tavern.getInstance();
			case $029: return HaulageFirm.getInstance();
			case $030: return SchnapsDistillery.getInstance();
			case $031: return Steelworks.getInstance();
			case $032: return Steakhouse.getInstance();
			case $033: return WindFarm.getInstance();
			case $034: return BrickManufacturer.getInstance();
			case $035: return Zoo.getInstance();
			case $036: return WorkersCottages.getInstance();
			case $037: return ConstructionSite.getInstance();
			case $038: return LoggingCamp.getInstance();
			case $039: return MSDagmar.getInstance();
			case $040: return Pawnbrokers.getInstance();
			case $041: return PiratesLair.getInstance();
			case $042: return Junkyard.getInstance();
			case $043: return DiveBar.getInstance();
			case $044: return PicketLine.getInstance();
			case $045: return TobaccoFactory.getInstance();
			case $046: return CattleDrive.getInstance();
			case $GH01: return WoodenCrane.getInstance();
			case $GH02: return LumberMill.getInstance();
			case $GH03: return MainStation.getInstance();
			case $GH04: return Hotel.getInstance();
			case $GH05: return FurFactory.getInstance();
			case $GH06: return Wainwrights.getInstance();
			case $GH07: return FishermansHut.getInstance();
			case $GH08: return SoupKitchen.getInstance();
			case $GH09: return CattleMarket.getInstance();
			case $GH10: return BrickTrader.getInstance();
			case $GH11: return ClayCompany.getInstance();
			case $GH12: return SmeltingFurnace.getInstance();
			case $GH13: return FleaMarket.getInstance();
			case $GH14: return Inn.getInstance();
			case $GH15: return CanneryRow.getInstance();
			case $GH16: return HidesTrader.getInstance();
			case $GH17: return FurIndustry.getInstance();
			case $GH18: return DepartmentOfAgriculture.getInstance();
			case $GH19: return DepartmentOfEconomics.getInstance();
			case $GH20: return Stockmarket.getInstance();
			case $GH21: return SouvenirShop.getInstance();
			case $GH22: return Wreckers.getInstance();
			case $GH23: return BlastFurnace.getInstance();
			case $GH24: return BiofuelFacility.getInstance();
			case $GH25: return Ranch.getInstance();
			case $GH26: return TradingFirm.getInstance();
			case $GH27: return TraffickingSpot.getInstance();
			case $GH28: return MajorCorporation.getInstance();
			case $GH29: return Emporium.getInstance();
			case $GH30: return Cobblers.getInstance();
			case $GH31: return RattletrapCar.getInstance();
			case $GH32: return WholesaleBakery.getInstance();
			case $E2010: return ToyFair.getInstance();
			default: return null;
		}
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 */
	public abstract boolean use(LeHavre control);

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public abstract boolean isUsable(LeHavre control);

	/**
	 *	Returns the buildings the active player can build.
	 *	@param control the control object
	 */
	public ArrayList<Building> getBuildings(LeHavre control) {
		GameState game = control.getGameState();
		Player player = game.getActivePlayer();
		ArrayList<Building> ret = new ArrayList<Building>();
		boolean sawmill = (this instanceof Sawmill);
		boolean lumberMill = player.owns(LumberMill.getInstance());
		boolean crane = player.owns(WoodenCrane.getInstance());
		boolean guild = player.owns(MasonsGuild.getInstance());
		GoodsList costs;
		boolean payable, craneNeeded, woodNeeded;
		double amount, goodCount, wood;
		Good good;
		for(Building building: game.getTopmostBuildings()) {
			if(!building.isBuildable()) continue;
			costs = building.getCosts();
			payable = true;
			craneNeeded = woodNeeded = false;
			wood = 0;
			for(GoodsPair pair: costs) {
				amount = pair.getAmount();
				good = pair.getGood();
				if(good.equals(Good.Wood)) {
					if(sawmill) amount -= Sawmill.REDUCTION;
					if(lumberMill) amount -= LumberMill.REDUCTION;
					wood = amount;
					woodNeeded = true;
				} else if(guild && (good.equals(Good.Clay) || good.equals(Good.Brick))) amount -= MasonsGuild.REDUCTION;
				goodCount = player.getGood2(good);
				if(goodCount < amount) {
					if(crane && !craneNeeded && !good.equals(Good.Wood) && (amount - goodCount) == 1) {
						craneNeeded = true;
						continue;
					}
					payable = false;
					break;
				}
			}
			if(sawmill && !woodNeeded) continue;
			if(craneNeeded) {
				goodCount = player.getGood(Good.Wood);
				if(woodNeeded && (goodCount - wood) <= 0) continue;
				if(!woodNeeded && !sawmill && !lumberMill  && goodCount == 0) continue;
			}
			if(payable) ret.add(building);
		}
		return ret;
	}

	/**
	 *	Returns the ships the active player can build.
	 *	@param control the control object
	 */
	public ArrayList<Buildable> getShips(LeHavre control) {
		GameState game = control.getGameState();
		Player player = game.getActivePlayer();
		ArrayList<Buildable> ret = new ArrayList<Buildable>();
		GoodsList costs;
		boolean payable;
		Good good;
		double amount, energy;
		boolean canModernise = (modernised || player.getGood(Good.Brick) > 0);
		for(Ship ship: game.getTopmostShips()) {
			if(!canModernise && !ship.isWooden()) continue;
			costs = ship.getCosts();
			payable = true;
			energy = 0;
			for(GoodsPair pair: costs) {
				amount = pair.getAmount();
				good = pair.getGood();
				if(player.getGood2(good) < amount) {
					payable = false;
					break;
				}
				if(good.equals(Good.Energy)) energy = amount;
			}
			if(energy > 0) {
				player.lose(costs);
				if(player.getPotentialEnergy() < energy) payable = false;
				player.receive(costs);
			}
			if(payable) ret.add(ship);
		}
		Town town = game.getTown();
		if(player.owns(Ship.Type.Iron) && town.owns(LuxuryYacht.getInstance())) ret.add(LuxuryYacht.getInstance());
		else if(player.owns(Ship.Type.Luxury) && town.owns(MSDagmar.getInstance()) && canModernise) ret.add(MSDagmar.getInstance());
		return ret;
	}

	/**
	 *	Compares this building to the given one. Returns a negative
	 *	integer, zero, or a positive integer as this building is less
	 *	than, equal to, or greater than the specified building.
	 *	@param building the building to be compared
	 *	@return a negative integer, zero, or a positive integer as
	 *			this building is less than, equal to, or greater than
	 *			the specified building.
	 */
	public int compareTo(Building building) {
		final int GREATER = 1, SMALLER = -1;
		int diff = proto.ordinal() - building.proto.ordinal();
		if(isStart()) {
			if(building.isStart()) return diff;
			else return GREATER;
		} else if(building.isStart()) return SMALLER;
		else if(!isEnterable()) {
			if(!building.isEnterable()) return diff;
			else return SMALLER;
		} else if(!building.isEnterable()) return GREATER;
		else if(isSpecial()) {
			if(building.isSpecial()) return diff;
			else return GREATER;
		} else if(building.isSpecial()) return SMALLER;
		else return diff;
	}

	/**
	 *	Returns true if this building is equal to the given object.
	 *	@param object the object to compare
	 *	@return true if this building is equal to the given object
	 */
	@Override
	public boolean equals(Object object) {
		if(!(object instanceof Building)) return false;
		else return proto.equals(((Building)object).proto);
	}
	public int hashCode()
	{
		return super.hashCode();
	}

	/**
	 *	Returns the Kulami.
	 *	@return the Kulami
	 */
	public Buildings getProto() {
		return proto;
	}

	/**
	 *	Returns the index for standard buildings or -1.
	 *	@return the index for standard buildings
	 */
	public int getIndex() {
		return proto.getIndex();
	}

	/**
	 *	Returns true if the player with the given index occupies the building.
	 *	@param index the player's index
	 *	@return true if the player occupies the building
	 */
	public boolean isWorker(int index) {
		for(Integer workerIndex: workers) if(workerIndex == index) return true;
		return false;
	}

	/**
	 *	Adds the player with the given index to the list of workers.
	 *	@param index the player's index
	 */
	public void addWorker(int index) {
		workers.add(index);
		setChanged(OCCUPIED);
	}

	/**
	 *	Removes the player with the given index from the list of workers.
	 *	@param index the player's index
	 */
	public void removeWorker(int index) {
		workers.remove(Integer.valueOf(index));
		setChanged(OCCUPIED);
	}

	/**
	 *	Returns the list of worker indices
	 *	@return the list of worker indices
	 */
	public ArrayList<Integer> getWorkers() {
		ArrayList<Integer> ret = new ArrayList<Integer>();
		ret.addAll(workers);
		return ret;
	}

	/**
	 *	Returns the number of workers in the building.
	 *	@return the number of workers
	 */
	public int getWorkerCount() {
		return workers.size();
	}

	/**
	 *	Returns the latest worker or -1.
	 *	@return the latest worker
	 */
	public int getLatestWorker() {
		int size = workers.size();
		return (size > 0 ? workers.get(size - 1) : -1);
	}

	/**
	 *	Returns true if the building is occupied.
	 *	@return true if the building is occupied
	 */
	public boolean isOccupied() {
		return (getWorkerCount() > 0);
	}

	/**
	 *	Sets the owner's index to the given one.
	 *	@param index the new index
	 */
	public void setOwner(int index) {
		owner = index;
	}

	/**
	 *	Returns the owner's index.
	 *	@return the owner's index
	 */
	public int getOwner() {
		return owner;
	}

	/**
	 *	Sets the building to be built or not.
	 *	@param built provide true if the building is built
	 */
	public void setBuilt(boolean built) {
		this.built = built;
	}

	/**
	 *	Returns true if the building has been already built.
	 *	@return true if the building is built
	 */
	public boolean isBuilt() {
		return built;
	}

	/**
	 *	Sets the building to be modernised or not.
	 *	@param modernised provide true if the building is modernised
	 */
	public void setModernised(boolean modernised) {
		this.modernised = modernised;
		setChanged(MODERNISED);
	}

	/**
	 *	Returns true if the building has been already modernized.
	 *	@return true if the building is modernized
	 */
	public boolean isModernised() {
		return modernised;
	}

	/**
	 *	Returns the building type.
	 *	@return the building type
	 */
	public Buildings.Type getType() {
		return proto.getType();
	}

	/**
	 *	Returns true if the building is a bonus building.
	 *	@return true if the building is a bonus building
	 */
	public boolean isBonus() {
		return proto.isBonus();
	}

	/**
	 *	Returns true if a player can be banned from this building.
	 *	@return true if a player can be banned from this building
	 */
	public boolean isBanAllowed() {
		return proto.isBanAllowed();
	}

	/**
	 *	Returns true if the building is a start building.
	 *	@return true if the building is a start building
	 */
	public boolean isStart() {
		return proto.isStart();
	}

	/**
	 *	Returns true if the building is a standard building.
	 *	@return true if the building is a standard building
	 */
	public boolean isStandard() {
		return proto.isStandard();
	}

	/**
	 *	Returns true if the building is a special building.
	 *	@return true if the building is a special building
	 */
	public boolean isSpecial() {
		return proto.isSpecial();
	}

	/**
	 *	Returns true if the building is enterable.
	 *	@return true if the building is enterable
	 */
	public boolean isEnterable() {
		return (built && proto.isEnterable());
	}

	/**
	 *	Returns true if the building is a craft building.
	 *	@return true if the building is a craft building
	 */
	public boolean isCraft() {
		return proto.isCraft();
	}

	/**
	 *	Returns true if the building is an economic building.
	 *	@return true if the building is an economic building
	 */
	public boolean isEconomic() {
		return proto.isEconomic();
	}

	/**
	 *	Returns true if the building is an industrial building.
	 *	@return true if the building is an industrial building
	 */
	public boolean isIndustrial() {
		return proto.isIndustrial();
	}

	/**
	 *	Returns true if the building is a public building.
	 *	@return true if the building is a public building
	 */
	public boolean isPublic() {
		return proto.isPublic();
	}

	/**
	 *	Returns true if the building is a ship.
	 *	@return true if the building is a ship
	 */
	public boolean isShip() {
		return proto.isShip();
	}

	/**
	 *	Returns the amount of hammer symbols.
	 *	@return the amount of hammer symbols
	 */
	public int getHammer() {
		return proto.getHammer();
	}

	/**
	 *	Returns the amount of fishing symbols.
	 *	@return the amount of fishing symbols
	 */
	public int getFishing() {
		return proto.getFishing();
	}

	/**
	 *	Returns the franc price.
	 *	@return the franc price
	 */
	public int getPrice() {
		return proto.getPrice();
	}

	/**
	 *	Returns the franc value.
	 *	@return the franc value
	 */
	public int getValue() {
		return proto.getValue();
	}

	/**
	 *	Returns the bonus points for the given player.
	 *	@param player the player
	 *	@return the bonus points
	 */
	public int getBonus(Player player) {
		return 0;
	}

	/**
	 *	Returns the entry fee.
	 *	@return the entry fee
	 */
	public EntryFee getEntryFee() {
		return new EntryFee(
			new Quantity<Food>(proto.getFoodEntry()),
			new Quantity<Franc>(proto.getFrancEntry())
		);
	}

	/**
	 *	Returns true if the building can be bought.
	 *	@return true if the building can be bought
	 */
	public boolean isBuyable() {
		return proto.isBuyable();
	}

	/**
	 *	Returns true if the building can be sold.
	 *	@return true if the building can be sold
	 */
	public boolean isSellable() {
		return proto.isSellable();
	}

	/**
	 *	Returns true if the building can be built.
	 *	@return true if the building can be built
	 */
	public boolean isBuildable() {
		return proto.isBuildable();
	}

	/**
	 *	Returns the building costs.
	 *	@return the building costs
	 */
	public GoodsList getCosts() {
		return proto.getCosts();
	}

	/**
	 *	Returns the name of the building.
	 *	@return the name
	 */
	public String toString() {
		return proto.toString();
	}

	/**
	 *	Returns the full description of this object.
	 *	Such a description contains information on all
	 *	its fields and their current values.
	 *	@return the full description
	 */
	public String dump() {
		StringBuilder ret = new StringBuilder();
		ret.append(String.format("%s %s%n", getClass().getSimpleName(), getProto()));
		ret.append(String.format("> Index: %s%n", getIndex()));
		ret.append(String.format("> Type: %s%n", getType()));
		ret.append(String.format("> Costs: %s%n", getCosts()));
		ret.append(String.format("> Price: %s%n", getPrice()));
		ret.append(String.format("> Value: %s%n", getValue()));
		ret.append(String.format("> Fishing: %s%n", getFishing()));
		ret.append(String.format("> Hammer: %s%n", getHammer()));
		ret.append(String.format("> EntryFee: %s%n", getEntryFee()));
		ret.append(String.format("> Owner: %s%n", getOwner()));
		ret.append(String.format("> Workers: %s%n", getWorkers()));
		ret.append(String.format("> isOccupied(): %s%n", isOccupied()));
		ret.append(String.format("> isBonus(): %s%n", isBonus()));
		ret.append(String.format("> isBuildable(): %s%n", isBuildable()));
		ret.append(String.format("> isEnterable(): %s%n", isEnterable()));
		ret.append(String.format("> isBuilt(): %s%n", isBuilt()));
		ret.append(String.format("> isModernized(): %s%n", isModernised()));
		ret.append(String.format("> isBuyable(): %s%n", isBuyable()));
		ret.append(String.format("> isCraft(): %s%n", isCraft()));
		ret.append(String.format("> isEconomic(): %s%n", isEconomic()));
		ret.append(String.format("> isIndustrial(): %s%n", isIndustrial()));
		ret.append(String.format("> isPublic(): %s%n", isPublic()));
		ret.append(String.format("> isShip(): %s%n", isShip()));
		ret.append(String.format("> isSpecial(): %s%n", isSpecial()));
		ret.append(String.format("> isStandard(): %s%n", isStandard()));
		ret.append(String.format("> isStart(): %s%n", isStart()));
		return ret.toString();
	}
}