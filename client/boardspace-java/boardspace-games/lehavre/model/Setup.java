/* copyright notice */package lehavre.model;

import lehavre.model.buildings.*;
import lehavre.util.*;

/**
 *
 *	The <code>Setup</code> class holds the constant setup settings.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/23
 */
public final class Setup
{
	/** No instances. */
	private Setup() {}

	/**
	 *	Returns the initial number of goods on the offer spaces
	 *	in a game of the given type.
	 *	@param gameType the type
	 *	@return the numbers
	 */
	public static int[] getInitialGoods(GameType gameType) {
		switch(gameType) {
			case LONG: return new int[]{2, 2, 2, 1, 0, 0, 0};
			case SHORT: return new int[]{3, 3, 3, 2, 1, 1, 1};
			default: throw new IllegalArgumentException("Wrong game type: " + gameType);

		}
	}

	/**
	 *	Returns the order of the offer spaces.
	 *	@return the order of the offer spaces
	 */
	public static Good[] getOfferedGoods() {
		return new Good[]{Good.Franc, Good.Fish, Good.Wood, Good.Clay, Good.Iron, Good.Grain, Good.Cattle};
	}

	/**
	 *	Returns a list of goods to give each player at the
	 *	beginning of a game of the given type.
	 *	@param gameType the type
	 *	@return the list of goods
	 */
	public static GoodsList getPlayerGoods(GameType gameType) {
		GoodsList ret = new GoodsList();
		switch(gameType) {
			case LONG:
				ret.add(5, Good.Franc);
				ret.add(1, Good.Coal);
				break;
			case SHORT:
				ret.add(5, Good.Franc);
				ret.add(2, Good.Fish);
				ret.add(2, Good.Wood);
				ret.add(2, Good.Clay);
				ret.add(2, Good.Iron);
				ret.add(1, Good.Cattle);
				ret.add(2, Good.Coal);
				ret.add(2, Good.Hides);
				break;
			default: throw new IllegalArgumentException("Wrong game type: " + gameType);
		}
		return ret;
	}

	/**
	 *	Returns the array of round cards used with the given
	 *	number of players in the given type of game.
	 *	@param gameType the type
	 *	@param num the number of players
	 *	@return the array of round cards
	 */
	public static Round[] getRoundCards(GameType gameType, int num) {
		switch(gameType) {
			case LONG:
				switch(num) {
					case 1: return new Round[]{Round.R01, Round.R04, Round.R10, Round.R13, Round.R14, Round.R16, Round.R20};
					case 2: return new Round[]{Round.R01, Round.R02, Round.R04, Round.R05, Round.R07, Round.R08,
							Round.R10, Round.R11, Round.R13, Round.R14, Round.R16, Round.R17, Round.R19, Round.R20};
					case 3: return new Round[]{Round.R03, Round.R01, Round.R02,Round.R04, Round.R05, Round.R06, Round.R07, Round.R08, 
							Round.R10, Round.R11, Round.R12, Round.R13, Round.R14, Round.R16, Round.R17, Round.R18, Round.R19, Round.R20};
					case 4: case 5: return Round.values();
				default:
					break;
				}
				break;
			case SHORT:
				switch(num) {
					case 1: return new Round[]{Round.R04, Round.R13, Round.R16, Round.R20};
					case 2: return new Round[]{Round.R02, Round.R05, Round.R07, Round.R10, Round.R11, Round.R14, Round.R16, Round.R20};
					case 3: return new Round[]{Round.R03, Round.R01, Round.R02, Round.R05, Round.R06, Round.R07, Round.R10, 
							Round.R11, Round.R12, Round.R14, Round.R18, Round.R20};
					case 4: return new Round[]{Round.R01, Round.R02, Round.R04, Round.R06, Round.R07, Round.R09, 
							Round.R10, Round.R13, Round.R15, Round.R16, Round.R19, Round.R20};
					case 5: return new Round[]{Round.R01, Round.R02, Round.R03, Round.R04, Round.R06, Round.R07, Round.R09,
							Round.R10, Round.R12, Round.R13, Round.R15, Round.R16, Round.R18, Round.R19, Round.R20};
				default:
					break;
				}
				break;
		default:
			break;
		}
		throw new IllegalArgumentException("Wrong game type: " + gameType);
	}

	/**
	 *	Returns true if the given standard building is used as a start
	 *	building in the short game with the given number of players.
	 *	@param building the building
	 *	@param num the number of players
	 *	@return true if the building is used
	 */
	public static boolean isBuildingStart(Buildings building, int num) {
		switch(building) {
			case $_01: return num < 3;
			case $_02: return num != 2 && num != 3;
			case $_13: return num == 1;
			default: return false;
		}
	}

	/**
	 *	Returns true if the given standard building is used in the
	 *	building stacks with the given number of players in the
	 *	given type of game.
	 *	@param building the building
	 *	@param num the number of players
	 *	@param gameType the type of game
	 *	@return true if the building is used
	 */
	public static boolean isBuildingUsed(Buildings building, int num, GameType gameType) {
		boolean longGame = GameType.LONG.equals(gameType);
		switch(building) {
			case $_03: case $_05: case $_08: case $_09: case $_10: case $_12:
			case $_14: case $_16: case $_18: case $_20: case $_22: case $_23:
				return true;
			case $_29:
				return num > 1;
			case $_07: case $_25:
				return longGame || num > 1;
			case $_04: case $_06: case $_17: case $_21: case $_27:
				return num > 2;
			case $_01:
				return longGame || num > 2;
			case $_02: case $_13:
				return longGame && num > 2;
			case $_11:
				return longGame && num > 3;
			case $_28: case $_30:
				return num > (longGame ? 1 : 4);
			case $_15: case $_19:
				return num > (longGame ? 2 : 4);
			case $_24: case $_26:
				return num > (longGame ? 3 : 4);
			default:
				return false;
		}
	}

	/**
	 *	Returns true if the given special building is used in solo games.
	 *	@return true if the given special building is used in solo games
	 */
	public static boolean isSpecialUsedSolo(Buildings building) {
		switch(building) {
			case $014: case $041: case $043: case $044: case $045: case $046:
			case $GH20: case $GH21:
				return false;
			default:
				return true;
		}
	}
}