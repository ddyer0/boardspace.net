package lehavre.model.buildings.special;

import lehavre.main.*;
import lehavre.model.buildings.*;

/**
 *	The <code>MasonsGuild</code> class represents the Masons' Guild (025).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class MasonsGuild
extends Building
{
	static final long serialVersionUID =1L;
	/** The amount of brick/clay reduced. */
	public static final int REDUCTION = 1;

	/** The instance. */
	private static MasonsGuild instance = null;

	/** Creates a new <code>MasonsGuild</code> instance. */
	private MasonsGuild() {
		super(Buildings.$025);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static MasonsGuild getInstance() {
		if(instance == null) instance = new MasonsGuild();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		control.showFatalError("UseBuilding", this);
		return false;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		return false;
	}
}