package lehavre.model.buildings.standard;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;

/**
 *
 *	The <code>HardwareStore</code> class represents the Hardware Store (_06).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class HardwareStore
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static HardwareStore instance = null;

	/** Creates a new <code>HardwareStore</code> instance. */
	private HardwareStore() {
		super(Buildings.$_06);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static HardwareStore getInstance() {
		if(instance == null) instance = new HardwareStore();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GoodsList goods = new GoodsList();
		goods.add(1, Good.Wood);
		goods.add(1, Good.Brick);
		goods.add(1, Good.Iron);
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