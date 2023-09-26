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

import java.awt.Color;


import lehavre.main.AddressInterface;
import lehavre.main.Dictionary;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.view.GUIHelper;

/**
 *
 *	The <code>Util</code> class offers a lot of useful calculation
 *	methods for the game. It is an overall pure static class.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/18
 */
public final class Util
{
	/** No instances. */
	private Util() {}

	/**
	 *	Returns the given string repeated the given amount of times.
	 *	@param text the string
	 *	@param n the amount of times to repeat
	 *	@return the repeated string
	 */
	public static String repeat(String text, int n) {
		if(n <= 0) return "";
		StringBuilder ret = new StringBuilder();
		for(int i = 0; i < n; i++) ret.append(text);
		return ret.toString();
	}

	/**
	 *	Returns a random value in the given range.
	 *	@param min the lower bound
	 *	@param max the upper bound
	 */
	public static int random(int min, int max) {
		if(max - min <= 1) return min;
		return min + (int)(Math.random() * (max - min));
	}

	/**
	 *	Returns the string representation of the given double value.
	 *	@param value the double value
	 *	@return the string representation
	 */
	public static String format(double value) {
		if(value == (int)value) return String.valueOf((int)value);
		return String.format("%.1f", value);
	}

	/**
	 *	Returns the given text embedded in HTML tags
	 *	and restraint in size to the default value.
	 *	@param text the text
	 *	@return the formatted text
	 */
	public static String format(String text) {
		return format(text, 200);
	}

	/**
	 *	Returns the given text embedded in HTML tags
	 *	and restraint to the given size.
	 *	@param text the text
	 *	@param size the size
	 *	@return the formatted text
	 */
	public static String format(String text, int size) {
		StringBuilder ret = new StringBuilder();
		ret.append("<html><div width=\"");
		ret.append(size);
		ret.append("\">");
		ret.append(text);
		ret.append("</div></html>");
		return ret.toString();
	}

	/**
	 *	Returns a string parsed to an integer or -1 if the string
	 *	does not represent a valid integer value.
	 *	@param string the string to be parsed
	 *	@return the integer
	 */
	public static int parseInt(String string) {
		final int INVALID = -1;
		if(string == null) return INVALID;
		string = string.trim();
		return (string.matches("\\d+") ? Integer.parseInt(string) : INVALID);
	}

	/**
	 *	Decodes a string represented list of goods. The different
	 *	values may be seperated by any of the following delimiters:
	 *	'-' (hyphen), ' ' (whitespace), '+' (plus), ',' (comma),
	 *	'/' (slash), '|' (vertical line), ';' (semi-colon)
	 *	@param goods the list of goods
	 */
	public static GoodsList toList(String goods) {
		if(goods == null) return null;
		GoodsList list = new GoodsList();
		String[] parts = goods.split("[-,;\\s/+|]");
		for(String part: parts) {
			int n = part.length();
			list.add(
				Integer.parseInt(part.substring(0, n - 1)),
				Good.fromCode(part.charAt(n - 1))
			);
		}
		return (list.size() > 0 ? list : null);
	}

	/**
	 *	Returns the numerus for the given amount.
	 *	@param amount the amount
	 *	@return the numerus
	 */
	public static String getNumerus(double amount) {
		if(Math.abs(amount) == 1) return "";
		else if(amount != (int)amount || amount < 1 || amount > 4) return "5";
		else return "2";
	}

	/**
	 *	Returns the given word in the correct numerus.
	 *	@param dictionary the dictionary
	 *	@param amount the amount
	 *	@param word the word
	 *	@return the numerated word
	 */
	public static String getNumbered(Dictionary dictionary, double amount, String word) {
		return String.format("%s %s", format(amount), dictionary.get(word + getNumerus(amount)));
	}

	/**
	 *	Returns the given title in the appropriate language
	 *	and adds information on the given player.
	 *	@param dictionary the dictionary
	 *	@param title the title
	 *	@return the formatted title
	 */
	public static String getTitle(Dictionary dictionary, String title) {
		return getTitle(dictionary, title, null, null);
	}

	/**
	 *	Returns the given title in the appropriate language
	 *	and adds information on the given player.
	 *	@param dictionary the dictionary
	 *	@param title the title
	 *	@param player the player
	 *	@return the formatted title
	 */
	public static String getTitle(Dictionary dictionary, String title, Player player) {
		if(player != null) return getTitle(dictionary, title, player.getName(), player.getAddress());
		else return getTitle(dictionary, title);
	}

	/**
	 *	Returns the given title in the appropriate language
	 *	and adds information on the given player.
	 *	@param dictionary the dictionary
	 *	@param title the title
	 *	@param name the player's name
	 *	@param address the player's network address
	 *	@return the formatted title
	 */
	public static String getTitle(Dictionary dictionary, String title, String name, AddressInterface address) {
		String tav = lehavre.main.LeHavre.getTitleAndVersion();
		String key = title + "Title";
		String dic = dictionary.get(key);
		String fullTitle = String.format(dic==null?key:dic, tav==null?"":tav);
		if(name != null && address != null) return String.format("%s [%s@%s]", fullTitle, name, address);
		else return fullTitle;
	}

	/**
	 *	Returns the color code for the given good.
	 *	@param good the good
	 *	@return the color code
	 */
	public static String getColor(Good good) {
		switch(good) {
			case Fish: case SmokedFish: case Food: return "188e9d";
			case Wood: case Charcoal: return "6b4620";
			case Clay: case Brick: return "c21a2e";
			case Iron: case Steel: return "073284";
			case Grain: case Bread: return "ebbf27";
			case Cattle: case Meat: return "e47c2c";
			case Coal: case Coke: case Energy: return "7b7d7e";
			case Hides: case Leather: return "2b3131";
			case Franc: return "c19321";
			default: return "000000";
		}
	}

	/**
	 *	Returns the given good as a colored string.
	 *	The color will be the real color of the good.
	 *	@param dictionary the dictionary
	 *	@param good the good
	 *	@return the colored string
	 */
	public static String getColored(Dictionary dictionary, Good good) {
		return getColoredForAmount(dictionary, 1, good);
	}

	/**
	 *	Returns the given good as a colored string.
	 *	The color will be the real color of the good.
	 *	@param dictionary the dictionary
	 *	@param amount the amount
	 *	@param good the good
	 *	@return the colored string
	 */
	public static String getColoredForAmount(Dictionary dictionary, double amount, Good good) {
		return String.format("<span style=\"color:#%s;\">%s</span>", getColor(good), dictionary.get("good" + good + getNumerus(amount)));
	}

	/**
	 *	Returns the given good as a colored string.
	 *	The color will be the real color of the good.
	 *	The amount also will be included.
	 *	@param dictionary the dictionary
	 *	@param amount the amount
	 *	@param good the good
	 *	@return the colored string
	 */
	public static String getColoredWithAmount(Dictionary dictionary, double amount, Good good) {
		return String.format("<span style=\"color:#%s;\">%s %s</span>", getColor(good), format(amount), dictionary.get("good" + good + getNumerus(amount)));
	}

	/**
	 *	Returns the given amount of goods as a colored string.
	 *	The color will be the real color of the good.
	 *	@param dictionary the dictionary
	 *	@param pair the value pair
	 *	@return the colored string
	 */
	public static String getColored(Dictionary dictionary, GoodsPair pair) {
		return getColoredWithAmount(dictionary, pair.getAmount(), pair.getGood());
	}

	/**
	 *	Returns the given goods list as a colored string.
	 *	@param dictionary the dictionary
	 *	@param goods the goods list
	 */
	public static String getColored(Dictionary dictionary, GoodsList goods) {
		StringBuilder ret = new StringBuilder();
		for(GoodsPair pair: goods) {
			if(ret.length() > 0) ret.append(", ");
			ret.append(getColored(dictionary, pair));
		}
		int n = ret.lastIndexOf(", ");
		if(n >= 0) ret.replace(n, n + 1, " " + dictionary.get("and"));
		return ret.toString();
	}

	/**
	 *	Returns the tooltip text for the given building.
	 *	The data will be read from the given dictionary.
	 *	@param dictionary the dictionary
	 *	@param building the building
	 *	@return the tooltip text
	 */
	public static String getToolTipText(Dictionary dictionary, Buildings building) {
		/* Name and number */
		StringBuilder ret = new StringBuilder();
		ret.append("<html><p><b style=\"font-size:125%;\">");
		ret.append(building.toString());
		ret.append(" <span style=\"color:#0f3269;\">");
		ret.append(dictionary.get("building" + building));
		ret.append("</span> (");

		/* Value and price */
		int value = building.getValue();
		ret.append(value);
		if(building.isBonus()) ret.append("+");
		int price = building.getPrice();
		if(price != value) {
			ret.append(", <i>");
			if(price < 0) ret.append(dictionary.get("bldNoPurchase"));
			else {
				ret.append(dictionary.get("bldPrice"));
				ret.append(": ");
				ret.append(price);
			}
			ret.append("</i>");
		}
		ret.append(")</b>");

		/* Building type and symbol */
		Buildings.Type type = building.getType();
		int hammer = building.getHammer();
		int fishing = building.getFishing();
		if(type != null || hammer > 0 || fishing > 0) {
			ret.append("<br><span style=\"color:#736f6e;\">");
			boolean comma = false;
			if(type != null) {
				ret.append(dictionary.get("type" + type));
				comma = true;
			}
			if(hammer > 0) {
				if(comma) ret.append(", ");
				ret.append(getNumbered(dictionary, hammer, "bldHammer"));
				comma = true;
			}
			if(fishing > 0) {
				if(comma) ret.append(", ");
				ret.append(getNumbered(dictionary, fishing, "bldFishing"));
			}
			ret.append("</span>");
		}

		/* Building costs */
		ret.append("</p><p style=\"margin:10px 0px;\"><span style=\"color:#736f6e;\">");
		ret.append(dictionary.get("bldCosts"));
		ret.append(":</span> ");
		GoodsList list = building.getCosts();
		if(list != null) ret.append(getColored(dictionary, list));
		else {
			ret.append("<i>");
			ret.append(dictionary.get("bldNoBuild"));
			ret.append("</i>");
		}

		/* Entry costs */
		ret.append("<br><span style=\"color:#736f6e;\">");
		ret.append(dictionary.get("bldEntry"));
		ret.append(":</span> ");
		if(building.isEnterable()) {
			double food = building.getFoodEntry();
			double franc = building.getFrancEntry();
			if(food != 0 || franc != 0) {
				if(food != 0) ret.append(getColored(dictionary, new GoodsPair(food, Good.Food)));
				if(franc != 0) {
					if(food != 0) ret.append(" / ");
					ret.append(getColored(dictionary, new GoodsPair(franc, Good.Franc)));
				}
			} else {
				ret.append("<i>");
				ret.append(dictionary.get("bldFree"));
				ret.append("</i>");
			}
		} else {
			ret.append("<i>");
			ret.append(dictionary.get("bldNoEntry"));
			ret.append("</i>");
		}

		/* Description */
		ret.append("</p><p>");
		ret.append(dictionary.get("description" + building));
		ret.append("</p></html>");
		return ret.toString();
	}

	/**
	 *	Returns the tooltip text for the given ship.
	 *	The data will be read from the given dictionary.
	 *	@param dictionary the dictionary
	 *	@param ship the ship
	 *	@return the tooltip text
	 */
	public static String getToolTipText(Dictionary dictionary, Ship ship) {
		/* Name, number and value */
		StringBuilder ret = new StringBuilder();
		ret.append("<html><p><b style=\"font-size:125%;\">");
		ret.append(ship.toString());
		ret.append(" <span style=\"color:#0f3269;\">");
		ret.append(dictionary.get("ship" + ship.getType()));
		ret.append("</span> (");
		ret.append(ship.getValue());
		ret.append(")</b>");

		/* Building costs */
		ret.append("<br>");
		ret.append(dictionary.get("bldCosts"));
		ret.append(": ");
		GoodsList list = ship.getCosts();
		if(list != null) ret.append(getColored(dictionary, list));
		else {
			ret.append("<i>");
			ret.append(dictionary.get("bldNoBuild"));
			ret.append("</i>");
		}

		/* Description */
		ret.append("</p><p style=\"margin:10px 0px;\">");
		ret.append(dictionary.get("bldPrice"));
		ret.append(": ");
		int price = ship.getPrice();
		ret.append(price < 0 ? dictionary.get("bldNoPurchase") : price);
		ret.append("<br>");
		ret.append(String.format(dictionary.get("mainCapacity"), ship.getCapacity()));
		ret.append("<br>");
		ret.append(String.format(dictionary.get("mainFood"), ship.getFoodSupply()));
		ret.append("</p></html>");
		return ret.toString();
	}

	/**
	 *	Returns the tooltip text for the given overview card.
	 *	The data will be read from the given dictionary.
	 *	@param dictionary the dictionary
	 *	@param card the overview card
	 *	@return the tooltip text
	 */
	public static String getToolTipText(Dictionary dictionary, OverviewCard card) {
		GameType gameType = card.getGameType();
		int playerCount = card.getPlayerCount();
		StringBuilder ret = new StringBuilder();
		ret.append("<html><h2>");
		ret.append(dictionary.get(gameType.toString()));
		ret.append(" (");
		ret.append(getNumbered(dictionary, playerCount, "player"));
		ret.append(")</h2><table width=\"720\" border=\"1\"><tr>");
		Round rounds[] = Setup.getRoundCards(gameType, playerCount);
		int n = rounds.length;
		int max = 4 * (int)Math.ceil((double)n / 4);
		for(int i = 1; i <= max; i++) {
			String text = "&nbsp;";
			if(i <= n) {
				text = getToolTipText(dictionary, rounds[i - 1]);
				text = text.replace("<html>", "");
				text = text.replace("</html>", "");
			}
			ret.append("<td>");
			ret.append(text);
			ret.append("</td>");
			if(i < n && (i % 4 == 0)) ret.append("</tr><tr>");
		}
		ret.append("</td></tr></table></html>");
		return ret.toString();
	}

	/**
	 *	Returns the tooltip text for the given round card.
	 *	The data will be read from the given dictionary.
	 *	@param dictionary the dictionary
	 *	@param round the round card
	 *	@return the tooltip text
	 */
	public static String getToolTipText(Dictionary dictionary, Round round) {
		StringBuilder ret = new StringBuilder();
		ret.append("<html><b style=\"color:navy;\">");
		ret.append(dictionary.get("round"));
		ret.append(" ");
		ret.append(String.valueOf(round.getIndex()));
		ret.append("</b> <span style=\"color:#");
		ret.append(getColor(Good.Cattle));
		ret.append(";\">(");
		if(!round.isHarvest()) ret.append("<s>");
		ret.append(dictionary.get("harvest"));
		if(!round.isHarvest()) ret.append("</s>");
		ret.append(")</span><br><b style=\"color:#");
		ret.append(getColor(Good.Food));
		ret.append(";\">");
		ret.append(dictionary.get("foodDemand"));
		ret.append(":</b> <span style=\"color:black;\">");
		ret.append(String.valueOf(round.getFoodDemand()));
		ret.append("</span><br><b style=\"color:#");
		ret.append(getColor(Good.Brick));
		ret.append(";\">");
		ret.append(dictionary.get("building"));
		ret.append(":</b> ");
		switch(round.getBuildingType()) {
			case Round.NO_BUILDING: ret.append(dictionary.get("bldNoBuilding")); break;
			case Round.STANDARD_BUILDING: ret.append(dictionary.get("bldStandard")); break;
			case Round.SPECIAL_BUILDING: ret.append(dictionary.get("bldSpecial")); break;
		default:
			break;
		}
		ret.append("<br><b style=\"color:#");
		ret.append(getColor(Good.Energy));
		ret.append(";\">");
		ret.append(dictionary.get("ship"));
		ret.append(":</b> <span style=\"color:#");
		Ship.Type type = round.getShip().getType();
		Color color = new GUIHelper("main").getColor("Ship" + type);
		ret.append(String.format("%02x", color.getRed()));
		ret.append(String.format("%02x", color.getGreen()));
		ret.append(String.format("%02x", color.getBlue()));
		ret.append(";\">");
		ret.append(dictionary.get("ship" + type));
		ret.append("</span></html>");
		return ret.toString();
	}

	/**
	 *	Returns the tooltip text for the given supply chit.
	 *	The data will be read from the given dictionary.
	 *	@param dictionary the dictionary
	 *	@param supply the supply chit
	 *	@return the tooltip text
	 */
	public static String getToolTipText(Dictionary dictionary, Supply supply) {
		StringBuilder ret = new StringBuilder();
		ret.append("<html><b>");
		ret.append(dictionary.get("supply"));
		ret.append("</b><br>(");
		ret.append(getColored(dictionary, supply.getFirst()));
		ret.append(", ");
		ret.append(getColored(dictionary, supply.getSecond()));
		if(supply.isInterest()) {
			ret.append(", ");
			ret.append(dictionary.get("interest"));
		}
		ret.append(")</html>");
		return ret.toString();
	}

	/**
	 *	Returns the tooltip text for the given goods chit.
	 *	The data will be read from the given dictionary.
	 *	@param dictionary the dictionary
	 *	@param good the goods chit
	 *	@return the tooltip text
	 */
	public static String getToolTipText(Dictionary dictionary, Good good) {
		StringBuilder ret = new StringBuilder();
		ret.append("<html>");
		ret.append(getColored(dictionary, good));
		ret.append(" (");
		ret.append(format(good.getFrancValue()));
		ret.append(" F.)");
		double food = good.getFoodValue();
		if(food > 0) {
			ret.append("<br>");
			ret.append(String.format(dictionary.get("mainFood"), format(food)));
		}
		double energy = good.getEnergyValue();
		if(energy > 0) {
			ret.append("<br>");
			ret.append(String.format(dictionary.get("mainEnergy"), format(energy)));
		}
		ret.append("</html>");
		return ret.toString();
	}
}