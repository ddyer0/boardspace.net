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

/**
 *
 *	The <code>Farm</code> class represents the Farm (004).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Farm
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final double OUTPUT_FISH = getProperty("fout", 2);
	private final double OUTPUT_GRAIN = getProperty("gout", 2);
	private final double OUTPUT_WOOD = getProperty("wout", 2);
	private final double OUTPUT_HIDES = getProperty("hout", 1);
	private final double OUTPUT_CATTLE = getProperty("mout", 1);

	/** The instance. */
	private static Farm instance = null;

	/** Creates a new <code>Farm</code> instance. */
	private Farm() {
		super(Buildings.$004);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Farm getInstance() {
		if(instance == null) instance = new Farm();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList goods = new GoodsList();
		goods.add(OUTPUT_FISH, Good.Fish);
		goods.add(OUTPUT_GRAIN, Good.Grain);
		goods.add(OUTPUT_WOOD, Good.Wood);
		goods.add(OUTPUT_HIDES, Good.Hides);
		goods.add(OUTPUT_CATTLE, Good.Cattle);
		control.receive(goods);
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