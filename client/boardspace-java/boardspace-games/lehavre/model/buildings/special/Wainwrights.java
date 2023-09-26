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
 *	The <code>Wainwrights</code> class represents the Wainwright's (GH06).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Wainwrights
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double INPUT_WOOD = getProperty("win", 2);
	private final double OUTPUT_FRANC = getProperty("$out", 5);
	private final int LIMIT = (int)getProperty("Lim", 1);

	/** The instance. */
	private static Wainwrights instance = null;

	/** Creates a new <code>Wainwrights</code> instance. */
	private Wainwrights() {
		super(Buildings.$GH06);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Wainwrights getInstance() {
		if(instance == null) instance = new Wainwrights();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList input = new GoodsList();
		input.add(INPUT_WOOD, Good.Wood);
		GoodsList output = new GoodsList();
		output.add(OUTPUT_FRANC, Good.Franc);
		Player player = control.getGameState().getActivePlayer();
		int max = (int)player.getGood2(Good.Iron) * LIMIT;
		control.receive(GoodsDialog.showProcessDialog(control, input, output, 0, 0, max));
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		return (player.getGood2(Good.Iron) > 0 && player.getGood(Good.Wood) >= INPUT_WOOD);
	}
}