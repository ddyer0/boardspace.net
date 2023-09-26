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
package lehavre.main;

import java.util.*;
import lehavre.model.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>Settings</code> class holds the settings set in
 *	the settings window. Only actual changes will be saved.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/21
 */
public final class Settings
implements java.io.Serializable
{	static final long serialVersionUID =1L;

	/** The special buildings settings. */
	public final Hashtable<Buildings, Boolean> specialAccepted = new Hashtable<Buildings, Boolean>();
	public final Hashtable<Buildings, Integer> specialPositions = new Hashtable<Buildings, Integer>();

	/** The supply chits settings. */
	public final Hashtable<Supply, Boolean> supplyVisible = new Hashtable<Supply, Boolean>();
	public final Hashtable<Supply, Integer> supplyPositions = new Hashtable<Supply, Integer>();

	/** The standard buildings settings. */
	public final Hashtable<Buildings, Boolean> standardAccepted = new Hashtable<Buildings, Boolean>();
	public final Hashtable<Buildings, Integer> standardPositions = new Hashtable<Buildings, Integer>();
	public boolean standardOverride = false;

	/** The goods chits settings. */
	public final Hashtable<Good, Integer> goodAmounts = new Hashtable<Good, Integer>();

	/** The extra game settings. */
	public int soloMarketCapacity;
	public boolean advancedLoans;
	public boolean pointsVisible;
}