package lehavre.model.buildings.standard;

import lehavre.main.*;
import lehavre.model.buildings.*;

/**
 *
 *	The <code>Wharf1</code> class represents the Wharf (_12).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Wharf1
extends Building
implements ShipsBuilder
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static Wharf1 instance = null;

	/** Creates a new <code>Wharf1</code> instance. */
	private Wharf1() {
		super(Buildings.$_12);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Wharf1 getInstance() {
		if(instance == null) instance = new Wharf1();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		control.getGameState().getActivePlayer().setBuilds(1);
		control.getMainWindow().enableShips(getShips(control));
		return false;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		return (getShips(control).size() > 0);
	}
}