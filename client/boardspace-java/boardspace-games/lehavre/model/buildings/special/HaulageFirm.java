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
package lehavre.model.buildings.special;

import java.util.ArrayList;
import javax.swing.JOptionPane;
import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;

/**
 *
 *	The <code>HaulageFirm</code> class represents the Haulage Firm (029).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class HaulageFirm
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final int FEE = (int)getProperty("Fee", 3);

	/** The instance. */
	private static HaulageFirm instance = null;

	/** Creates a new <code>HaulageFirm</code> instance. */
	private HaulageFirm() {
		super(Buildings.$029);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static HaulageFirm getInstance() {
		if(instance == null) instance = new HaulageFirm();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GameState game = control.getGameState();
		//Player player = game.getActivePlayer();
		GoodsList goodsList = new GoodsList();
		goodsList.add(-FEE, Good.Franc);
		control.receive(goodsList);
		Good[] goods = Setup.getOfferedGoods();
		ArrayList<Pair<GoodsPair, GoodsPair>> pairs = new ArrayList<Pair<GoodsPair, GoodsPair>>();
		GoodsPair pair1 = null, pair2;
		int amount;
		for(int i = 0; i < GameState.OFFER_COUNT; i++) {
			if(goods[i].isMoney()) continue;
			amount = game.getOffer(i);
			if(amount > 0) {
				pair2 = new GoodsPair(amount, goods[i]);
				if(pair1 != null) pairs.add(new Pair<GoodsPair, GoodsPair>(pair1, pair2));
				pair1 = pair2;
			} else pair1 = null;
		}
		int index = 0, size = pairs.size();
		Pair<GoodsPair, GoodsPair> pair;
		if(size > 0) {
			Dictionary dict = control.getDictionary();
			String[] options = new String[size];
			String and = dict.get("and");
			for(int i = 0; i < size; i++) {
				pair = pairs.get(i);
				pair1 = pair.getFirst();
				pair2 = pair.getSecond();
				options[i] = String.format("%s %s %s",
					Util.getNumbered(dict, (int)pair1.getAmount(), "good" + pair1.getGood()), and,
					Util.getNumbered(dict, (int)pair2.getAmount(), "good" + pair2.getGood())
				);
			}
			String choice = null;
			while(true) {
				choice = (String)JOptionPane.showInputDialog(
					null,
					Util.format(dict.get("popupHaulageFirm")),
					dict.get("building" + this),
					JOptionPane.QUESTION_MESSAGE,
					null,
					options,
					options[0]
				);
				if(choice == null) control.showError("NoChoice");
				else break;
			}
			for(String option: options) {
				if(choice.equals(option)) break;
				index++;
			}
		}
		pair = pairs.get(index);
		control.takeOffer(pair.getFirst().getGood());
		control.takeOffer(pair.getSecond().getGood());
		return false;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		GameState game = control.getGameState();
		Player player = game.getActivePlayer();
		if(player.getMoney() < FEE) return false;
		Good[] goods = Setup.getOfferedGoods();
		boolean filled = false;
		for(int i = 0; i < GameState.OFFER_COUNT; i++) {
			if(!goods[i].isMoney() && game.getOffer(i) > 0) {
				if(filled) return true;
				filled = true;
			} else filled = false;
		}
		return false;
	}
}