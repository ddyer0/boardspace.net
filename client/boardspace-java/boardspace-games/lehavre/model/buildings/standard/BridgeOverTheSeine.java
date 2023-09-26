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
 *	The <code>BridgeOverTheSeine</code> class represents the Bridge over the Seine (_27).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class BridgeOverTheSeine
extends Building
{
	static final long serialVersionUID =1L;
	/** The minimum amounts to get one franc. */
	public static final int MIN_PROCESSED = 1;
	public static final int MIN_BASIC = 3;

	/** The instance. */
	private static BridgeOverTheSeine instance = null;

	/** Creates a new <code>BridgeOverTheSeine</code> instance. */
	private BridgeOverTheSeine() {
		super(Buildings.$_27);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static BridgeOverTheSeine getInstance() {
		if(instance == null) instance = new BridgeOverTheSeine();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		GoodsList pList = new GoodsList();
		GoodsList bList = new GoodsList();
		int pAmount = 0, bAmount = 0, amount;
		for(Good good: Good.values()) {
			if(good.isProcessed()) {
				amount = player.getGood(good);
				if(amount > 0) {
					pAmount += amount;
					pList.add(amount, good);
				}
				continue;
			}
			if(good.isBasic()) {
				amount = player.getGood(good);
				if(amount > 0) {
					bAmount += amount;
					bList.add(amount, good);
				}
				continue;
			}
		}
		GoodsList payment = new GoodsList();
		amount = 0;
		while(true) {
			if(amount != 0) break;
			if(pAmount >= MIN_PROCESSED) {
				pList = GoodsDialog.showChoiceDialog(control, pList, 0, pAmount, true);
				pAmount = 0;
				for(GoodsPair pair: pList) pAmount += (int)pair.getAmount();
				if(pAmount >= MIN_PROCESSED) {
					amount += pAmount / MIN_PROCESSED;
					payment.addAll(pList);
				}
			}
			if(bAmount >= MIN_BASIC) {
				bList = GoodsDialog.showChoiceDialog(control, bList, 0, bAmount, true);
				bAmount = 0;
				for(GoodsPair pair: bList) bAmount += (int)pair.getAmount();
				if(bAmount >= MIN_BASIC) {
					amount += bAmount / MIN_BASIC;
					payment.addAll(bList);
				}
			}
			if(amount == 0) control.showError("InputNone");
		}
		GoodsList goods = new GoodsList();
		goods.add(amount, Good.Franc);
		for(GoodsPair pair: payment) goods.add(-pair.getAmount(), pair.getGood());
		control.receive(goods);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		int basic = 0, processed = 0;
		for(Good good: Good.values()) {
			if(good.isProcessed()) {
				processed += player.getGood(good);
				if(processed >= MIN_PROCESSED) return true;
			}
			if(good.isBasic()) {
				basic += player.getGood(good);
				if(basic >= MIN_BASIC) return true;
			}
		}
		return false;
	}
}