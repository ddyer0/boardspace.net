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
package lehavre.model;

import java.io.*;
import java.util.*;
import lehavre.main.*;
import lehavre.model.buildings.*;
import lehavre.util.*;

/**
 *
 *	The <code>GameState</code> class is a snapshot of the game.
 *	This is the class doing all the internal game stuff.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.26 2010/4/25
 */
public final class GameState
implements Serializable
{	
	/** The game version. */
	public static final long serialVersionUID = 126201011261934L;	// vvvyyyymmddhhmm
    public static final String VERSION_NUMBER;
    public static final String VERSION_DATE;
    public static final String VERSION_TIME;
    public static final String VERSION;

	static {
		String uid = Long.toString(0x71dea19cb868L);
		int k = uid.length();
		VERSION_NUMBER = String.format("%s.%s", uid.substring(0, k - 14), uid.substring(k - 14, k - 12));
		VERSION_DATE = String.format("%s/%s/%s", uid.substring(k - 12, k - 8), uid.substring(k - 8, k - 6), uid.substring(k - 6, k - 4));
		VERSION_TIME = String.format("%s:%s", uid.substring(k - 4, k - 2), uid.substring(k - 2, k));
		VERSION = String.format("Version %s, %s %s", VERSION_NUMBER, VERSION_DATE, VERSION_TIME);
	}

	/** Some magic in-game numbers. */
	public static final int TURNS_PER_ROUND = 7;
	public static final int STACK_COUNT = 3;
	public static final int OFFER_COUNT = Setup.getOfferedGoods().length;
	public static final int LOAN_VALUE = 4;
	public static final int LOAN_PAYBACK = 5;
	public static final int LOAN_PENALTY = 7;
	public static final int MARKET_MIN = 2;
	public static final int MARKET_MAX = 8;
	public static final int MARKET_SOLO = 4;
	public static final int HARVEST_GRAIN = 1;
	public static final int HARVEST_GRAIN_CONDITION = 1;
	public static final int HARVEST_CATTLE = 1;
	public static final int HARVEST_CATTLE_CONDITION = 2;
	public static final int SHIP_TYPES = Ship.Type.values().length;

	/** Private magic numbers */
	private static final int INTEREST = 1;
	private static final int ADVANCED_INTEREST = 2;

	/** The last valid state. */
	private GameState previous;

	/** The requested changes. */
	private Settings settings;

	/** The type of game: true if long game. */
	private GameType gameType;

	/** The players objects. */
	private final ArrayList<Player> players;

	/** The play order. */
	private final ArrayList<Integer> order;

	/** The round cards. */
	private final ArrayList<Round> rounds;

	/** The town object. */
	private Town town;

	/** The supply chits. */
	private final Supply[] supply;

	/** The visibility of the supply chits. */
	private final boolean[] supplyVisible;

	/** The offer spaces. */
	private final int[] offer;

	/** All buildings. */
	private final ArrayList<Building> buildings;

	/** The special buildings. */
	private final ArrayList<Buildings> specials;

	/** The standard buildings. */
	private final ArrayList<ArrayList<Buildings>> standards;

	/** The ships. */
	private final ArrayList<ArrayList<Ship>> ships;

	/** The current round. */
	private int round;

	/** The current turn. */
	private int turn;

	/** The maximum amount of goods from marketplace. */
	private int marketCapacity;

	/** True if playing with the loan variant. */
	private boolean advancedLoans;

	/** True if points are visible during the game. */
	private boolean pointsVisible;

	/** Some state variables. */
	private boolean completed;	// round completed?
	private boolean running;	// game running?
	private boolean endgame;	// end phase?
	private boolean over;		// game over?

	/**
	 *	Creates a new <code>GameState</code> instance.
	 */
	public GameState() {
		players = new ArrayList<Player>();
		order = new ArrayList<Integer>();
		rounds = new ArrayList<Round>();
		buildings = new ArrayList<Building>();
		for(Buildings building: Buildings.values()) buildings.add(Building.create(building));
		specials = new ArrayList<Buildings>();
		standards = new ArrayList<ArrayList<Buildings>>();
		for(int i = 0; i < STACK_COUNT; i++) standards.add(new ArrayList<Buildings>());
		ships = new ArrayList<ArrayList<Ship>>();
		for(int i = 0; i < SHIP_TYPES; i++) ships.add(new ArrayList<Ship>());
		town = new Town();
		supply = new Supply[Supply.values().length];
		supplyVisible = new boolean[supply.length];
		offer = new int[OFFER_COUNT];
		gameType = GameType.LONG;
		completed = false;
		running = false;
		endgame = false;
		over = false;
		previous = null;
	}

	/**
	 *	Returns a copy of this state.
	 *	@return the copy
     */
	public GameState copy() {
		GameState ret = null;
		try {
			/* Write the object out to a byte array. */
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(bos);
			out.writeObject(this);
			out.flush();
			out.close();

			/* Make an input stream from the byte array and read a copy of the object back in. */
			ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
			ret = (GameState)in.readObject();
		} catch(Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	/**
	 *	Reads in the fields of the given game state.
	 *	@param state the game state
	 *	@param includeBuildings provide true to restore the buildings
	 */
	public synchronized void restore(GameState state, boolean includeBuildings) {
		if(includeBuildings) for(Building building: state.buildings) Building.create(building).restore(building);
		players.clear();
		for(Player player: state.players) players.add(player.clone());
		order.clear();
		order.addAll(state.order);
		rounds.clear();
		rounds.addAll(state.rounds);
		int k = 0, n = players.size();
		for(Round round: rounds) {
			round.setIndex(++k);
			round.init(n);
		}
		for(Ship ship: Ship.values()) ship.init(n);
		specials.clear();
		specials.addAll(state.specials);
		standards.clear();
		ArrayList<Buildings> buildings;
		for(ArrayList<Buildings> stack: state.standards) {
			buildings = new ArrayList<Buildings>();
			buildings.addAll(stack);
			standards.add(buildings);
		}
		ships.clear();
		ArrayList<Ship> list;
		for(ArrayList<Ship> stack: state.ships) {
			list = new ArrayList<Ship>();
			list.addAll(stack);
			ships.add(list);
		}
		for(int i = 0; i < supply.length; i++) supply[i] = state.supply[i];
		for(int i = 0; i < supplyVisible.length; i++) supplyVisible[i] = state.supplyVisible[i];
		for(int i = 0; i < offer.length; i++) offer[i] = state.offer[i];
		gameType = state.gameType;
		town = state.town.clone();
		turn = state.turn;
		round = state.round;
		marketCapacity = state.marketCapacity;
		pointsVisible = state.pointsVisible;
		completed = state.completed;
		running = state.running;
		endgame = state.endgame;
		over = state.over;
		settings = state.settings;
		previous = state.previous;
	}

	/**
	 *	Returns true if the current round is completed.
	 *	@return true if the current round is completed
	 */
	public boolean isRoundCompleted() {
		return completed;
	}

	/**
	 *	Called after all players have paid their food
	 *	at the end of a round.
	 */
	public void setRoundCompleted() {
		completed = true;
	}

	/**
	 *	Returns the special buildings.
	 *	@return the special buildings
	 */
	public ArrayList<Building> getSpecials() {
		ArrayList<Building> ret = new ArrayList<Building>();
		for(Buildings building: specials) ret.add(Building.create(building));
		return ret;
	}

	/**
	 *	Swaps the two topmost special buildings.
	 */
	public void swapSpecials() {
		if(specials.size() > 1) specials.add(0, specials.remove(1));
	}

	/**
	 *	Returns the standard buildings of the given stack.
	 *	@param index the index of the stack
	 *	@return the standard buildings
	 */
	public ArrayList<Building> getStandard(int index) {
		ArrayList<Building> ret = new ArrayList<Building>();
		for(Buildings building: standards.get(index)) ret.add(Building.create(building));
		return ret;
	}

	/**
	 *	Returns the topmost building of each standard buildings stack.
	 *	@return the topmost buildings
	 */
	public ArrayList<Building> getTopmostBuildings() {
		ArrayList<Building> ret = new ArrayList<Building>();
		for(ArrayList<Buildings> stack: standards) if(stack.size() > 0) ret.add(Building.create(stack.get(0)));
		return ret;
	}

	/**
	 *	Returns the next building to be built by the town.
	 *	@return the next building to be built
	 */
	public Building getNextBuilding() {
		ArrayList<Building> buildings = getTopmostBuildings();
		if(buildings.size() == 0) return null;
		Building ret = buildings.get(0);
		for(Building building: buildings) if(building.getIndex() < ret.getIndex()) ret = building;
		return ret;
	}

	/**
	 *	Adds the given building to the given stack.
	 *	@param building the building
	 *	@param stack the stack index
	 */
	public void addStack(Building building, int stack) {
		standards.get(stack).add(0, building.getProto());
	}

	/**
	 *	Adds the given building to an empty stack.
	 *	@param building the building
	 */
	public void addStack(Building building) {
		int empty = -1;
		for(int i = 0; i < STACK_COUNT; i++) {
			if(standards.get(i).size() == 0) {
				empty = i;
				break;
			}
		}
		if(empty >= 0) addStack(building, empty);
	}

	/**
	 *	Returns the stack index for the given building or -1.
	 *	@param building the building
	 *	@return the stack index
	 */
	public int getStack(Building building) {
		for(int i = 0; i < STACK_COUNT; i++) if(standards.get(i).contains(building.getProto())) return i;
		return -1;
	}

	/**
	 *	Removes the given building.
	 *	@param building the building
	 */
	public void remove(Building building) {
		Buildings protot = building.getProto();
		if(building.isSpecial()) specials.remove(protot);
		int index = getStack(building);
		if(index >= 0) standards.get(index).remove(protot);
	}

	/**
	 *	Returns the ships of the given type.
	 *	@param type the type
	 *	@return the ships
	 */
	public ArrayList<Ship> getShips(Ship.Type type) {
		ArrayList<Ship> ret = new ArrayList<Ship>();
		ret.addAll(ships.get(type.ordinal()));
		return ret;
	}

	/**
	 *	Returns the topmost ship of each ship stack.
	 *	@return the topmost ships
	 */
	public ArrayList<Ship> getTopmostShips() {
		ArrayList<Ship> ret = new ArrayList<Ship>();
		for(ArrayList<Ship> stack: ships) if(stack.size() > 0) ret.add(stack.get(0));
		return ret;
	}

	/**
	 *	Adds the given ship.
	 *	@param ship the ship
	 */
	public void addShip(Ship ship) {
		ships.get(ship.getType().ordinal()).add(0, ship);
	}

	/**
	 *	Removes the given ship.
	 *	@param ship the ship
	 */
	public void remove(Ship ship) {
		for(ArrayList<Ship> stack: ships) if(stack.contains(ship)) stack.remove(ship);
	}

	/**
	 *	Returns the town object.
	 *	@return the town object
	 */
	public Town getTown() {
		return town;
	}

	/**
	 *	Returns the current supply chit.
	 *	@return the supply chit
	 */
	public Supply getSupply() {
		return getSupply((turn - 1) % TURNS_PER_ROUND);
	}

	/**
	 *	Returns the supply chit with the given index.
	 *	@param index the index
	 *	@return the supply chit
	 */
	public Supply getSupply(int index) {
		if(index < 0 || index >= supply.length) return null;
		return supply[index];
	}

	/**
	 *	Returns true if the supply chit with the given index
	 *	is visible at the beginning of the game.
	 *	@param index the index
	 *	@return true if the supply chit is visible
	 */
	public boolean isVisible(int index) {
		return supplyVisible[index];
	}

	/**
	 *	Sets the supply chit with the given index to be visible.
	 *	@param index the index
	 */
	public void setVisible(int index) {
		supplyVisible[index] = true;
	}

	/**
	 *	Returns the amount of goods on the given offer space.
	 *	@param index the index of the offer space
	 *	@return the amount of goods
	 */
	public int getOffer(int index) {
		return offer[index];
	}

	/**
	 *	Returns the amount of goods of the given type.
	 *	@param type the good type
	 *	@return the amount of goods
	 */
	public int getOffer(Good type) {
		return offer[getOfferIndex(type)];
	}

	/**
	 *	Adds the new good to the appropriate offer space.
	 *	@param good the good
	 */
	public void fillOffer(Good good) {
		offer[getOfferIndex(good)]++;
	}

	/**
	 *	Removes all goods from the appropriate offer space.
	 *	@param good the good
	 */
	public void clearOffer(Good good) {
		offer[getOfferIndex(good)] = 0;
	}

	/**
	 *	Returns the settings object.
	 *	@return the settings object
	 */
	public Settings getChanges() {
		return settings;
	}

	/**
	 *	Sets the settings to the given object.
	 *	@param settings the new settings
	 */
	public void setChanges(Settings settings) {
		this.settings = settings;
	}

	/**
	 *	Sets the type of game to the given one.
	 *	@param gameType the type of game
	 */
	public void setGameType(GameType gameType) {
		this.gameType = gameType;
	}

	/**
	 *	Returns the type of game.
	 *	@return the type of game
	 */
	public GameType getGameType() {
		return gameType;
	}

	/**
	 *	Returns true if this is a long game.
	 *	@return true if this is a long game
	 */
	public boolean isLongGame() {
		return GameType.LONG.equals(gameType);
	}

	/**
	 *	Returns true if this is a short game.
	 *	@return true if this is a short game
	 */
	public boolean isShortGame() {
		return GameType.SHORT.equals(gameType);
	}

	/**
	 *	Returns true if the game is a solo game.
	 *	@return true if the game is a solo game
	 */
	public boolean isSoloGame() {
		return (getPlayerCount() == 1);
	}

	/**
	 *	Returns true if playing with advanced loans.
	 *	@return true if playing with advanced loans
	 */
	public boolean areLoansAdvanced() {
		return advancedLoans;
	}

	/**
	 *	Returns the proper amount of interest the given
	 *	player needs to pay.
	 *	@param player the player
	 *	@return the amount of interest
	 */
	public int getInterest(Player player) {
		if(!areLoansAdvanced() || isSoloGame() || player.getLoans() < getPlayerCount()) return INTEREST;
		else return ADVANCED_INTEREST;
	}

	/**
	 *	Returns true if the points are visible during the game.
	 *	@return true if the points are visible
	 */
	public boolean arePointsVisible() {
		return pointsVisible;
	}

	/**
	 *	increases the turn.
	 */
	public void nextTurn() {
		turn++;
		completed = false;
	}

	public void nextRound() {
		if(++round > rounds.size()) endgame = true;
		setPrevious(null);
	}

	/**
	 *	Returns the current turn.
	 *	@return the current turn
	 */
	public int getTurn() {
		return turn;
	}

	/**
	 *	Returns the current round card.
	 *	@return the current round card
	 */
	public Round getRound() {
		if(round == 0 || endgame) return null;
		return rounds.get(round - 1);
	}

	/**
	 *	Returns the total number of turns.
	 *	@return the total number of turns
	 */
	public int getTurnCount() {
		return getRoundCount() * TURNS_PER_ROUND;
	}

	/**
	 *	Returns the total number of rounds.
	 *	@return the total number of rounds
	 */
	public int getRoundCount() {
		return rounds.size();
	}

	/**
	 *	Returns the market capacity.
	 *	@return the market capacity
	 */
	public int getMarketCapacity() {
		return marketCapacity;
	}

	/**
	 *	Returns the previous game state.
	 *	@return the previous game state
	 */
	public GameState getPrevious() {
		return previous;
	}

	/**
	 *	Sets the previous game state to the given one.
	 *	@param state the previous game state
	 */
	public void setPrevious(GameState state) {
		previous = state;
	}

	/**
	 *	Adds the given player.
	 *	@param player the player
	 */
	public void addPlayer(Player player) {
		int index = player.getIndex(), k = 0;
		for(Player p: players) {
			if(p.getIndex() > index) break;
			k++;
		}
		players.add(k, player);
	}

	/**
	 *	Returns the player with the given index.
	 *	@param index the index
	 *	@return the player
	 */
	public Player getPlayer(int index) {
		return players.get(index);
	}

	/**
	 *	Returns the active player.
	 *	@return the active player
	 */
	public Player getActivePlayer() {
		return getPlayerBySeat((turn - 1) % getPlayerCount() + 1);
	}

	/**
	 *	Returns the player at the given seat.
	 *	@param seat the seat
	 *	@return the player
	 */
	public Player getPlayerBySeat(int seat) {
		if(seat < 1 || seat > getPlayerCount()) return null;
		return getPlayer(order.get(seat - 1));
	}

	/**
	 *	Returns the players.
	 *	@return the players
	 */
	public ArrayList<Player> getPlayers() {
		ArrayList<Player> ret = new ArrayList<Player>();
		ret.addAll(players);
		return ret;
	}

	/**
	 *	Returns the number of players.
	 *	@return the number of players
	 */
	public int getPlayerCount() {
		return players.size();
	}

	/**
	 *	Sets the play order to the given one.
	 *	@param order the new play order
	 */
	public void setOrder(ArrayList<Integer> order) {
		this.order.addAll(order);
	}

	/**
	 *	Returns true if the game is running.
	 *	@return true if the game is running
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 *	Returns true if the game is ending.
	 *	@return true if the game is ending
	 */
	public boolean isEndgame() {
		return endgame;
	}

	/**
	 *	Sets up the game according to the settings object.
	 *	@return true if the setup was successful
	 */
	public boolean setup() {
		running = true;

		/* Fill settings with default values if nothing set */
		if(settings == null) {
			settings = new Settings();
			for(Buildings building: Buildings.values()) {
				if(building.isSpecial()) {
					settings.specialAccepted.put(building, true);
					settings.specialPositions.put(building, 0);
				} else if(building.isStandard()) {
					settings.standardAccepted.put(building, true);
					settings.standardPositions.put(building, 0);
				}
			}
			for(Supply supply: Supply.values()) {
				settings.supplyVisible.put(supply, false);
				settings.supplyPositions.put(supply, 0);
			}
			for(Good good: Good.values()) {
				if(getOfferIndex(good) < 0) continue;
				settings.goodAmounts.put(good, -1);
			}
			settings.soloMarketCapacity = GameState.MARKET_SOLO;
			settings.advancedLoans = false;
			settings.pointsVisible = false;
		}

		/* Fill the offer spaces */
		int[] start = Setup.getInitialGoods(gameType);
		for(Good good: settings.goodAmounts.keySet()) offer[getOfferIndex(good)] = settings.goodAmounts.get(good);
		for(int i = 0; i < offer.length; i++) if(offer[i] < 0) offer[i] = start[i];

		/* Order the supply chits */
		int index;
		final int k = Supply.values().length;
		ArrayList<Integer> indices = new ArrayList<Integer>();	// free positions
		for(int i = 1; i <= k; i++) indices.add(i);
		Hashtable<Integer, ArrayList<Supply>> buckets = new Hashtable<Integer, ArrayList<Supply>>();
		ArrayList<Supply> chits = new ArrayList<Supply>();
		for(Supply chit: settings.supplyPositions.keySet()) {
			index = settings.supplyPositions.get(chit);
			if(index > 0) {
				if(buckets.get(index) == null) buckets.put(index, new ArrayList<Supply>());
				buckets.get(index).add(chit);
				indices.remove(index);
			} else chits.add(chit);
		}
		if(chits.size() > 0) {
			for(Supply chit: chits) {
				index = indices.remove(Util.random(0, indices.size()));
				buckets.put(index, new ArrayList<Supply>());
				buckets.get(index).add(chit);
			}
		}
		index = 0;
		for(int i = 1; i <= k; i++) {
			chits = buckets.get(i);
			if(chits == null) continue;
			while(chits.size() > 0) supply[index++] = chits.remove(Util.random(0, chits.size()));
		}
		for(int i = 0; i < k; i++) supplyVisible[i] = settings.supplyVisible.get(supply[i]);

		/* Order the round cards */
		final int n = players.size();
		Round[] cards = Setup.getRoundCards(gameType, n);
		for(int i = 0; i < cards.length; i++) {
			rounds.add(cards[i]);
			cards[i].init(n);
			cards[i].setIndex(i + 1);
		}

		/* Initialize the ships */
		for(Ship ship: Ship.values()) ship.init(n);

		/* Set-up the buildings */
		int spCount = 0, stCount = 0;
		indices.clear();
		final int MAX_SPECIALS = (isLongGame() ? 6 : 0);
		for(int i = 1; i <= MAX_SPECIALS; i++) indices.add(i);
		ArrayList<Buildings> specials = new ArrayList<Buildings>();
		ArrayList<Buildings> standards = new ArrayList<Buildings>();
		Hashtable<Integer, ArrayList<Buildings>> spBuckets = new Hashtable<Integer, ArrayList<Buildings>>();
		Building building;
		for(Buildings protot: Buildings.values()) {
			/* Read special buildings indices */
			if(protot.isSpecial()) {
				if(!settings.specialAccepted.get(protot) || (n == 1 && !Setup.isSpecialUsedSolo(protot))) continue;
				index = settings.specialPositions.get(protot);
				if(index <= MAX_SPECIALS) {
					if(isShortGame()) continue;
					if(index > 0) {
						if(spBuckets.get(index) == null) spBuckets.put(index, new ArrayList<Buildings>());
						spBuckets.get(index).add(protot);
						indices.remove(index);
						spCount++;
					} else specials.add(protot);
					continue;
				}

			/* Read standard buildings indices */
			} else if(protot.isStandard()) {
				if(!settings.standardAccepted.get(protot)) continue;
				index = settings.standardPositions.get(protot);
				if(index <= STACK_COUNT && (isLongGame() || settings.standardOverride || !Setup.isBuildingStart(protot, n))) {
					if(!settings.standardOverride && !Setup.isBuildingUsed(protot, n, gameType)) continue;
					stCount++;
					if(index > 0) this.standards.get(index - 1).add(protot);
					else standards.add(protot);
					continue;
				}
			}

			/* Add as start building */
			building = Building.create(protot);
			if(building != null) {
				building.setBuilt(true);
				town.receive(building);
			}
		}

		/* Order the special buildings */
		if(isLongGame()) {
			if(spCount < MAX_SPECIALS) {
				while(spCount < MAX_SPECIALS && specials.size() > 0) {
					index = indices.remove(Util.random(0, indices.size()));
					spBuckets.put(index, new ArrayList<Buildings>());
					spBuckets.get(index).add(specials.remove(Util.random(0, specials.size())));
					spCount++;
				}
			}
			index = 0;
			ArrayList<Buildings> bucket;
			while(this.specials.size() < MAX_SPECIALS && index <= MAX_SPECIALS) {
				bucket = spBuckets.get(index++);
				if(bucket == null) continue;
				while(bucket.size() > 0 && this.specials.size() < MAX_SPECIALS)
					this.specials.add(bucket.remove(Util.random(0, bucket.size())));
			}
			specials.remove(Buildings.$_00);
			specials.remove(Buildings.$_31);
			Collections.shuffle(specials);
			town.setLeftOver(specials);

		/* If 1p/2p game, start with ships. */
		} else if(n < 3) {
			players.get(0).receive(Ship.S01);
			if(n > 1) players.get(1).receive(Ship.S03);
			else addShip(Ship.S03);
		}

		/* Order the standard buildings */
		indices.clear();
		final int MAX_STANDARDS = (int)Math.ceil(stCount / (double)STACK_COUNT);
		ArrayList<Buildings> buildings;
		for(int i = 0; i < STACK_COUNT; i++) {
			buildings = this.standards.get(i);
			while(buildings.size() > MAX_STANDARDS) standards.add(buildings.remove(Util.random(0, buildings.size())));
			if(buildings.size() < MAX_STANDARDS) indices.add(i);
		}
		while(standards.size() > 0) {
			int ri = Util.random(0, indices.size());
			index = indices.get(ri);
			this.standards.get(index).add(standards.remove(Util.random(0, standards.size())));
			if(this.standards.get(index).size() >= MAX_STANDARDS) 
				{ indices.remove(ri);			
				}
		}
		Buildings[] array;
		for(int i = 0; i < STACK_COUNT; i++) {
			buildings = this.standards.get(i);
			array = buildings.toArray(new Buildings[]{});
			Arrays.sort(array);
			buildings.clear();
			for(int j = 0; j < array.length; j++) buildings.add(array[j]);
		}

		/* Read final setting and remove settings object */
		marketCapacity = settings.soloMarketCapacity;
		advancedLoans = settings.advancedLoans;
		pointsVisible = settings.pointsVisible;
		settings = null;
		return true;
	}

	/**
	 *	Returns the index of the offer space for the given good.
	 *	@param good the good
	 *	@return the index
	 */
	public static int getOfferIndex(Good good) {
		switch(good) {
			case Franc: return 0;
			case Fish: return 1;
			case Wood: return 2;
			case Clay: return 3;
			case Iron: return 4;
			case Grain: return 5;
			case Cattle: return 6;
			default: return -1;
		}
	}

	/**
	 *	Returns true if the game is over.
	 *	@return true if the game is over
	 */
	public boolean isOver() {
		return over;
	}

	/**
	 *	Sets the game to be over.
	 */
	public void setOver() {
		over = true;
	}

	/**
	 *	Returns the full description of this object.
	 *	Such a description contains information on all
	 *	its fields and their current values.
	 *	@return the full description
	 */
	public String dump() {
		StringBuilder ret = new StringBuilder();
		ret.append(String.format("%s (%s)%n", getClass().getSimpleName(), VERSION));
		ret.append(String.format("> GameType: %s%n", gameType));
		ret.append(String.format("> SoloGame: %s%n", isSoloGame()));
		ret.append(String.format("> Running: %s%n", running));
		ret.append(String.format("> MarketCapacity: %s%n", marketCapacity));
		ret.append("> Town:\n\t");
		ret.append(town.dump().replace(">", "\t>"));
		ret.append("> Players:\n\t");
		int k = 0;
		for(Player player: players) {
			if(k++ > 0) ret.append("\t");
			ret.append(player.dump().replace(">", "\t>"));
		}
		ret.append(String.format("> ActivePlayer: %s%n", getActivePlayer()));
		ret.append(String.format("> Order: %s%n", order));
		ArrayList<Object> list = new ArrayList<Object>();
		for(Object item: supply) list.add(item);
		ret.append(String.format("> Supply: %s%n", list));
		list.clear();
		for(Object item: supplyVisible) list.add(item);
		ret.append(String.format("> Visible: %s%n", list));
		list.clear();
		for(Object item: offer) list.add(item);
		ret.append(String.format("> Offer: %s%n", list));
		ret.append(String.format("> Specials: %s%n", specials));
		ret.append(String.format("> Standards: %s%n", standards));
		ret.append(String.format("> Topmost: %s%n", getTopmostBuildings()));
		ret.append(String.format("> Ships: %s%n", ships));
		ret.append(String.format("> Topmost: %s%n", getTopmostShips()));
		ret.append("> Rounds:\n\t");
		k = 0;
		for(Round round: rounds) {
			if(k++ > 0) ret.append("\t");
			ret.append(round.dump().replace(">", "\t>"));
		}
		ret.append(String.format("> Round: %s%n", round));
		ret.append(String.format("> Turn: %s%n", turn));
		ret.append(String.format("> TurnCount: %s%n", getTurnCount()));
		ret.append(String.format("> Completed: %s%n", completed));
		ret.append(String.format("> Endgame: %s%n", endgame));
		ret.append(String.format("> Over: %s%n", over));
		return ret.toString();
	}
}