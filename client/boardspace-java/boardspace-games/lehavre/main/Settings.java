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