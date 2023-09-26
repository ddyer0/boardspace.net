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

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>CoalTrader</code> class represents the Coal Trader (018).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class CoalTrader
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_FOOD_CHARCOAL = getProperty("WNin", 1);
	private final double INPUT_FOOD_COAL = getProperty("kNin", 2);
	private final double OUTPUT_CHARCOAL = getProperty("Wout", 1);
	private final double OUTPUT_COAL = getProperty("kout", 1);
	private final int LIMIT_CHARCOAL = (int)getProperty("WLim", 1);
	private final int LIMIT_COAL = (int)getProperty("kLim", 5);

	/** The instance. */
	private static CoalTrader instance = null;

	/** Creates a new <code>CoalTrader</code> instance. */
	private CoalTrader() {
		super(Buildings.$018);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static CoalTrader getInstance() {
		if(instance == null) instance = new CoalTrader();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		int charcoal = (int)(player.getFood() / INPUT_FOOD_CHARCOAL);
		int coal = (int)(player.getFood() / INPUT_FOOD_COAL);
		boolean single = (charcoal == 0 || coal == 0);

		/* food --> charcoal */
		GoodsList input1 = new GoodsList();
		input1.add(INPUT_FOOD_CHARCOAL, Good.Food);
		GoodsList output1 = new GoodsList();
		output1.add(OUTPUT_CHARCOAL, Good.Charcoal);

		/* food --> coal */
		GoodsList input2 = new GoodsList();
		input2.add(INPUT_FOOD_COAL, Good.Food);
		GoodsList output2 = new GoodsList();
		output2.add(OUTPUT_COAL, Good.Coal);

		/* get input */
		GoodsList goods = new GoodsList();
		while(true) {
			if(charcoal > 0) {
				int max = Math.min(LIMIT_CHARCOAL != 0 ? LIMIT_CHARCOAL : Integer.MAX_VALUE, charcoal);
				goods.addAll(GoodsDialog.showProcessDialog(control, input1, output1, 0, 0, max, !single));
			}
			if(coal > 0) {
				int max = (LIMIT_COAL != 0 ? LIMIT_COAL : Integer.MAX_VALUE);
				if(goods.size() > 0) {
					double food = player.getFood() - goods.get(0).getAmount() * INPUT_FOOD_CHARCOAL;
					max = (int)Math.min(max, food / INPUT_FOOD_COAL);
				}
				if(max > 0) goods.addAll(GoodsDialog.showProcessDialog(control, input2, output2, 0, 0, max, !single));
			}
			if(goods.size() > 0) break;
			control.showError("InputNone");
		}

		/* pay food */
		double amount = 0;
		for(GoodsPair pair: goods) amount += (pair.getAmount() * (pair.getGood().equals(Good.Coal) ? INPUT_FOOD_COAL : INPUT_FOOD_CHARCOAL));
		GoodsList list = GoodsDialog.showFoodDialog(control, amount);
		for(GoodsPair pair: list) goods.add(-pair.getAmount(), pair.getGood());
		control.receive(goods);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		return (player.getFood() >= Math.min(INPUT_FOOD_CHARCOAL, INPUT_FOOD_COAL));
	}
}