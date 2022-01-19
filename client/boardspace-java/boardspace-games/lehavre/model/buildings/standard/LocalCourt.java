package lehavre.model.buildings.standard;

import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.GoodsList;

/**
 *
 *	The <code>LocalCourt</code> class represents the Local Court (_15).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class LocalCourt
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static LocalCourt instance = null;

	/** Creates a new <code>LocalCourt</code> instance. */
	private LocalCourt() {
		super(Buildings.$_15);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static LocalCourt getInstance() {
		if(instance == null) instance = new LocalCourt();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		int loans = player.getLoans();
		control.paybackLoans(loans < 3 ? 1 : 2, true);
		if(loans == 2) {
			GoodsList money = new GoodsList();
			money.add(2, Good.Franc);
			control.receive(money);
		}
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		Player player = control.getGameState().getActivePlayer();
		return (player.getLoans() > 0);
	}
}