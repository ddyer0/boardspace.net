/* copyright notice */package lehavre.model.buildings.special;

import java.util.ArrayList;
import javax.swing.JOptionPane;
import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;


/**
 *
 *	The <code>FleaMarket</code> class represents the Flea Market (GH13).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/12/27
 */
public final class FleaMarket
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static FleaMarket instance = null;

	/** Creates a new <code>FleaMarket</code> instance. */
	private FleaMarket() {
		super(Buildings.$GH13);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static FleaMarket getInstance() {
		if(instance == null) instance = new FleaMarket();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GameState game = control.getGameState();
		ArrayList<Good> ones = new ArrayList<Good>();
		ArrayList<Good> twos = new ArrayList<Good>();
		for(Good good: Setup.getOfferedGoods()) {
			switch(game.getOffer(good)) {
				case 1:	ones.add(good); break;
				case 2: twos.add(good); break;
				default: continue;
			}
		}
		int index = 0, size = twos.size();
		if(size > 1) {
			Dictionary dict = control.getDictionary();
			String[] options = new String[size];
			for(int i = 0; i < size; i++) options[i] = dict.get("good" + twos.get(i));
			String choice = null;
			while(true) {
				choice = (String)JOptionPane.showInputDialog(
					null,
					Util.format(dict.get("popupFleaMarket")),
					dict.get("building" + this),
					JOptionPane.QUESTION_MESSAGE,
					null,
					options,
					options[0]
				);
				if(choice == null) control.showError("NoChoice");
				else break;
			}
			for(String option: options) {
				if(choice.equals(option)) break;
				index++;
			}
		}
		ones.add(twos.get(index));
		for(Good good: ones) control.takeOffer(good);
		return true;
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		GameState game = control.getGameState();
		for(int i = 0; i < GameState.OFFER_COUNT; i++) if(game.getOffer(i) == 2) return true;
		return false;
	}
}