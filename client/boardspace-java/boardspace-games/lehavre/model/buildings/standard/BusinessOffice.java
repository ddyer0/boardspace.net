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
package lehavre.model.buildings.standard;


import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.GoodsDialog;

/**
 *
 *	The <code>BusinessOffice</code> class represents the Business Office (_21).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.10 2009/12/29
 */
public final class BusinessOffice
extends Building
{
	static final long serialVersionUID =1L;
	/** The amount of goods needed to get the steel and other resources. */
	public static final int STEEL_RATIO = 4;
	public static final int OTHER_RATIO = 1;

	/** The instance. */
	private static BusinessOffice instance = null;

	/** Creates a new <code>BusinessOffice</code> instance. */
	private BusinessOffice() {
		super(Buildings.$_21);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static BusinessOffice getInstance() {
		if(instance == null) instance = new BusinessOffice();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		GoodsList input = new GoodsList();
		double amount, sum = 0;
		for(Good good: Good.values()) {
			if(good.isPhysical() && !good.isMoney()) {
				amount = player.getGood(good);
				if(amount > 0) {
					input.add(amount, good);
					sum += amount;
				}
			}
		}
		GoodsList goods = new GoodsList();
		Dictionary dict = control.getDictionary();
		String steel = Util.getColored(dict, Good.Steel);

		/* steel conversion */
		if(sum >= STEEL_RATIO) {
			String msg = String.format(dict.get("popupBusinessOffice1"), steel);
			GoodsList payment = GoodsDialog.showChoiceDialog(control, input, STEEL_RATIO, STEEL_RATIO, true, msg, 20, true);
			if(payment.size() > 0) {
				Good good;
				for(GoodsPair pair: payment) {
					amount = pair.getAmount();
					good = pair.getGood();
					goods.add(-amount, good);
					input.add(-amount, good);
				}
				goods.add(1, Good.Steel);
				input.optimize();
				sum -= STEEL_RATIO;
			}
		}

		/* other conversion */
		if(sum >= OTHER_RATIO) {
			GoodsList output = new GoodsList();
			output.add(1, Good.Charcoal);
			output.add(1, Good.Leather);
			output.add(1, Good.Brick);
			String msg = String.format(dict.get("popupBusinessOffice2"), steel);
			GoodsList payment = GoodsDialog.showChoiceDialog(control, output, 1, 1, false, msg, 20, goods.size() > 0);
			if(payment.size() > 0) {
				goods.addAll(payment);
				msg = String.format(dict.get("popupBusinessOffice3"), Util.getColored(dict, payment));
				payment = GoodsDialog.showChoiceDialog(control, input, OTHER_RATIO, OTHER_RATIO, true, msg, 10);
				for(GoodsPair pair: payment) goods.add(-pair.getAmount(), pair.getGood());
			}
		}
		control.receive(goods);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		final int MIN = Math.min(STEEL_RATIO, OTHER_RATIO);
		int amount = 0;
		for(Good good: Good.values()) {
			if(good.isPhysical() && !good.isMoney()) {
				amount += player.getGood(good);
				if(amount >= MIN) return true;
			}
		}
		return false;
	}
}