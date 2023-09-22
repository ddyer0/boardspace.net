/* copyright notice */package lehavre.model.buildings.special;

import javax.swing.*;
import java.util.*;
import lehavre.main.*;
import lehavre.main.Dictionary;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;

/**
 *
 *	The <code>HarbourWatch</code> class represents the Harbour Watch (014).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class HarbourWatch
extends Building
{
	static final long serialVersionUID =1L;
	/** The building properties. */
	private final int FEE = (int)getProperty("Fee", 1);

	/** The instance. */
	private static HarbourWatch instance = null;

	/** Creates a new <code>HarbourWatch</code> instance. */
	private HarbourWatch() {
		super(Buildings.$014);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public synchronized static HarbourWatch getInstance() {
		if(instance == null) instance = new HarbourWatch();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GameState game = control.getGameState();
		Player active = game.getActivePlayer();

		/* Find out which players can be banned */
		int index = active.getIndex();
		int money = active.getMoney();
		ArrayList<Player> players = new ArrayList<Player>();
		for(Player player: game.getPlayers()) {
			if(player.equals(active)) continue;
			Building building = player.getLocation();
			if(building != null && building.isBanAllowed() && !building.isWorker(index)) {
				int fee = building.getWorkerCount() * FEE;
				if(money >= fee) {
					player.lose(fee, Good.Franc);
					EntryFeeSet sets = new EntryFeeSet(building, active);
					Iterator<GoodsList> iterator = sets.iterator();
					while(iterator != null && iterator.hasNext()) {
						GoodsList set = iterator.next();
						if(set != null) player.lose(set);
						if(building.isUsable(control)) {
							players.add(player);
							iterator = null;
						}
						if(set != null) player.receive(set);
					}
					player.receive(fee, Good.Franc);
				}
			}
		}

		/* Let active player choose whom to ban */
		Dictionary dict = control.getDictionary();
		String[] options = new String[players.size()];
		int i = 0;
		for(Player player: players) options[i++] = String.format("%s (%s)", dict.get("building" + player.getLocation()), player.getName());
		String choice = null;
		while(true) {
			choice = (String)JOptionPane.showInputDialog(
				null,
				Util.format(dict.get("popupHarbourWatch")),
				dict.get("building" + this),
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				options[0]
			);
			if(choice == null) control.showError("NoChoice");
			else break;
		}
		i = 0;
		for(String option: options) {
			if(choice.equals(option)) break;
			i++;
		}

		/* Ban the chosen player and enter the building */
		Building building = players.get(i).getLocation();
		control.ban(building, FEE);
		synchronized(active) {
			active.setActions(active.getActions() + 1);
		}
		control.enter(building);
		return false;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		GameState game = control.getGameState();
		Player active = game.getActivePlayer();
		int index = active.getIndex();
		int money = active.getMoney();
		boolean ret = false;
		for(Player player: game.getPlayers()) {
			if(player.equals(active)) continue;
			Building building = player.getLocation();
			if(building != null && building.isBanAllowed() && !building.isWorker(index)) {
				int fee = building.getWorkerCount() * FEE;
				if(money >= fee) {
					player.lose(fee, Good.Franc);
					EntryFeeSet sets = new EntryFeeSet(building, active);
					Iterator<GoodsList> iterator = sets.iterator();
					while(iterator != null && iterator.hasNext()) {
						GoodsList set = iterator.next();
						if(set != null) player.lose(set);
						if(building.isUsable(control)) {
							ret = true;
							iterator = null;
						}
						if(set != null) player.receive(set);
					}
					player.receive(fee, Good.Franc);
					if(ret) break;
				}
			}
		}
		return ret;
	}
}