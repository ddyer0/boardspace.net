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
package lehavre.model.buildings.standard;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;
import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;
import lehavre.view.*;
import lehavre.view.labels.BuildingLabel;

/**
 *
 *	The <code>Marketplace</code> class represents the Marketplace (_01).
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/25
 */
public final class Marketplace
extends Building
{
	static final long serialVersionUID =1L;
	/** The instance. */
	private static Marketplace instance = null;

	/** Creates a new <code>Marketplace</code> instance. */
	private Marketplace() {
		super(Buildings.$_01);
	}

	/**
	 *	Creates a new instance or returns the existing one.
	 *	@return the instance
	 */
	public static Marketplace getInstance() {
		if(instance == null) instance = new Marketplace();
		return instance;
	}

	/**
	 *	The active player enters the building and uses its function.
	 *	@param control the control object
	 *	@return true after usage
	 */
	public boolean use(LeHavre control) {
		GameState game = control.getGameState();
		Player player = game.getActivePlayer();
		int amount = 2;
		for(Building building: player.getBuildings()) if(building.isCraft()) amount++;
		if(game.isSoloGame()) amount = Math.min(amount, game.getMarketCapacity());
		amount = Math.min(amount, GameState.MARKET_MAX);
		GoodsList goods = new GoodsList();
		for(Good good: Good.values()) if(good.isBasic()) goods.add(1, good);
		control.receive(GoodsDialog.showChoiceDialog(control, goods, amount, amount, false));
		if(game.isLongGame()) {
			Round round = game.getRound();
			if(round != null) {
				Round[] cards = Setup.getRoundCards(game.getGameType(), game.getPlayerCount());
				int[] rounds = {-1, -1};
				for(int i = round.getIndex() - 1, j = 0; i < cards.length; i++) {
					if(cards[i].getBuildingType() == Round.SPECIAL_BUILDING) {
						rounds[j] = i + 1;
						if(++j >= rounds.length) break;
					}
				}
				if(rounds[0] < 0) control.showWarning("NoSpecials");
				else showSpecialsDialog(control, rounds[0], rounds[1]);
			}
		}
		return true;
	}

	/**
	 *	Creates the dialog to view the special buildings.
	 *	@param control the control object
	 *	@param first the round of the first building to come
	 *	@param second the round of the second building to come
	 */
	@SuppressWarnings("serial")
	private void showSpecialsDialog(final LeHavre control, int first, int second) {
		final GameState game = control.getGameState();
		Player player = game.getActivePlayer();
		ArrayList<Building> specials = game.getSpecials();
		final Dictionary dict = control.getDictionary();
		final JDialog dialog = GoodsDialog.createDialog(control, Util.getTitle(dict, "market", player));
		GUIHelper gui = new GUIHelper("dialog");
		JPanel inner = new JPanel(new FlowLayout(FlowLayout.CENTER, gui.getInt("EntryHGap"), gui.getInt("EntryVGap")));
		inner.setOpaque(false);
		final int n = Math.min(specials.size(), 2);
		BuildingLabel label;
		String language = dict.getLanguage();
		for(int i = 0; i < n; i++) {
			final Building building = specials.get(i);
			label = new BuildingLabel(control.network,language, null, building);
			label.setComponentPopupMenu(
				new JPopupMenu() {{
					JMenuItem item = new JMenuItem(dict.get("menuInfo"));
					item.addActionListener(
						new ActionListener() {
							private final String text, title;
							{
								text = Util.getToolTipText(dict, building.getProto());
								title = dict.get("popupBuilding");
							}
							public void actionPerformed(ActionEvent e) {
								control.showInformation(text, title);
							}
						}
					);
					add(item);
				}}
			);
			label.addMouseListener(
				new MouseAdapter() {
					public void mouseClicked(MouseEvent e) {
						if(e.getClickCount() > 1) {
							if(n > 1 && building.equals(game.getSpecials().get(1))) control.swapSpecials();
							dialog.setVisible(false);
							dialog.dispose();
						}
					}
				}
			);
			inner.add(label);
		}
		ActionListener action = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.setVisible(false);
				dialog.dispose();
			}
		};
		String text1, text2, info1 = "", info2 = "";
		if(!game.isSoloGame()) {
			int count = game.getPlayerCount(), turns = GameState.TURNS_PER_ROUND;
			StringBuilder msg = new StringBuilder(" ");
			Player user = game.getPlayerBySeat((first * turns) % count + 1);
			if(player.equals(user)) msg.append(dict.get("bldYouWill"));
			else msg.append(String.format(dict.get("bldOtherWill"), user.getName()));
			info1 = msg.toString();
			user = game.getPlayerBySeat((second * turns) % count + 1);
			if(user != null) {
				msg = new StringBuilder(" ");
				if(player.equals(user)) msg.append(dict.get("bldYouWill"));
				else msg.append(String.format(dict.get("bldOtherWill"), user.getName()));
				info2 = msg.toString();
			}
		}
		int current = game.getRound().getIndex();
		first -= current;
		if(first > 1) text1 = String.format(dict.get("bldInRounds"), Util.getNumbered(dict, first, "round"));
		else text1 = dict.get(String.format("bld%sRound", first > 0 ? "Next" : "This"));
		second -= current;
		if(second > 1) text2 = String.format(dict.get("bldInRounds"), Util.getNumbered(dict, second, "round"));
		else text2 = dict.get(String.format("bld%s", second > 0 ? "NextRound" : "Never"));
		String descr = String.format(dict.get("marketDescr"), text1, info1, text2, info2);
		Dimension size = new GUIHelper("market").getSize("Descr");
		GoodsDialog.showDialog(dialog, DialogWindow.createContents(dict, descr, size, inner, action));
	}

	/**
	 *	Returns true if the active player is able to use the building.
	 *	@param control the control object
	 */
	public boolean isUsable(LeHavre control) {
		return true;
	}
}