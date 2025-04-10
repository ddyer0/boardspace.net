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
 *	The <code>ToyFair</code> class represents the Toy Fair (E2010).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2010/11/28
 */
public final class ToyFair
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final int LOANS = (int)getProperty("Loan", 1);
	private final int LIMIT = (int)getProperty("Lim", 6);

	/** The instance. */
	private static ToyFair instance = null;

	/** Creates a new <code>ToyFair</code> instance. */
	private ToyFair() {
		super(Buildings.$E2010);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static ToyFair getInstance() {
		if(instance == null) instance = new ToyFair();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		control.takeLoans(LOANS, false);
		GoodsList goods = new GoodsList();
		for(Good good: Good.values()) if(good.isBasic() || (good.isProcessed() && !good.equals(Good.Steel))) goods.add(1, good);
		control.receive(GoodsDialog.showChoiceDialog(control, goods, LIMIT, LIMIT, false));
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		return true;
	}
}