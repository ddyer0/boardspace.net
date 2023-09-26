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
package lehavre.view;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import lehavre.main.*;
import lehavre.main.Dictionary;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;

/**
 *
 *	The <code>GoodsDialog</code> class is the super class for
 *	non-modal game dialogs where the player has to pay something.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.10 2009/12/28
 */
public final class GoodsDialog
{
	static final long serialVersionUID =1L;
	/** The GUI helper object. */
	//private static final GUIHelper gui = new GUIHelper("entry");

	/** The control object. */
	//private final LeHavre control;

	/** The list of goods chosen. */
	private final GoodsList goods;

	/**
	 *	Creates a new <code>EntryDialog</code> instance.
	 *	@param control the control object
	 */
	private GoodsDialog(LeHavre control) {
	//	this.control = control;
		goods = new GoodsList();
	}

	/**
	 *	Creates a dialog with the given title and returns it.
	 *	@param control the control object
	 *	@param title the title
	 *	@return the dialog
	 */
	@SuppressWarnings("serial")
	public static JDialog createDialog(final LeHavre control, String title) {
		return new JDialog(control.getMainWindow().getFrame(), title, true) {{
			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			addWindowListener(
				new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						control.showError("DialogClose");
					}
				}
			);
		}};
	}

	/**
	 *	Displays the given dialog and its contents.
	 *	@param dialog the dialog
	 *	@param contents the contents
	 */
	public static void showDialog(JDialog dialog, JPanel contents) {
		dialog.getContentPane().add(contents);
		dialog.pack();
		dialog.setLocationRelativeTo(dialog.getOwner());
		dialog.setVisible(true);
	}

	/**
	 *	Returns the list of goods.
	 *	@return the list of goods
	 */
	private GoodsList getGoods() {
		GoodsList ret = new GoodsList();
		ret.addAll(goods);
		return ret;
	}

	/**
	 *	Creates a new entry dialog for the given building.
	 *	Returns the goods paid as list.
	 *	@param control the control object
	 *	@param building the building
	 *	@return the list of goods
	 */
	public static GoodsList showEntryDialog(LeHavre control, Building building) {
		Player player = control.getGameState().getActivePlayer();
		if(player.owns(building)) return new GoodsList();
		EntryFee fee = building.getEntryFee();
		final double foodEntry = fee.getFoodEntry();
		final double moneyEntry = fee.getFrancEntry();
		if(foodEntry == 0 && moneyEntry == 0) return new GoodsList();
		double food = player.getFood();
		double money = player.getMoney();
		if(foodEntry == 0 && moneyEntry > money) return null;
		if(moneyEntry == 0 && foodEntry > food) return null;
		if(foodEntry > food && moneyEntry > money) return null;
		if(money == 0 || moneyEntry == 0) {
			GoodsList goods = new GoodsList();
			double sum = 0, min = Double.POSITIVE_INFINITY, value;
			int amount;
			for(Good good: Good.values()) {
				if(!good.isPhysical()) continue;
				amount = player.getGood(good);
				value = good.getFoodValue();
				if(amount > 0 && value > 0) {
					sum += amount * value;
					if(value < min) min = value;
					goods.add(amount, good);
				}
			}
			if(goods.size() == 1) {
				Good good = goods.get(0).getGood();
				goods.clear();
				goods.add(Math.ceil(foodEntry / good.getFoodValue()), good);
				return goods;
			} else if(sum >= foodEntry && sum < foodEntry + min) return goods;
		} else if(foodEntry == 0 || food == money) {
			GoodsList goods = new GoodsList();
			goods.add(foodEntry > 0 ? (moneyEntry > 0 ? Math.min(foodEntry, moneyEntry) : foodEntry) : moneyEntry, Good.Franc);
			return goods;
		}
		return createEntryDialog(control, foodEntry, moneyEntry);
	}

	/**
	 *	Creates the entry payment dialog.
	 *	Returns the list of goods paid.
	 *	@param control the control object
	 *	@param foodEntry the food entry fee
	 *	@param moneyEntry the money entry fee
	 *	@return the list of goods
	 */
	private static GoodsList createEntryDialog(final LeHavre control, final double foodEntry, final double moneyEntry) {
		final Player player = control.getGameState().getActivePlayer();
		final Dictionary dict = control.getDictionary();
		GoodsList list = FoodWindow.createFoodList(player);
		GoodsList food = new GoodsList();
		for(GoodsPair pair: list) if(pair.getAmount() > 0) food.add(pair);
		final JPanel inner = DialogWindow.createInnerPanel(control.network,dict, food);
		final GoodsDialog goodsDialog = new GoodsDialog(control);
		final JDialog dialog = createDialog(control, Util.getTitle(dict, "entry", player));
		ActionListener action = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GoodsList payment = new GoodsList();
				int food = 0, money = 0, value;
				JTextField textfield;
				Good good;
				for(Component component: inner.getComponents()) {
					if(!(component instanceof Container)) continue;
					for(Component comp: ((Container)component).getComponents()) {
						if(!(comp instanceof JTextField)) continue;
						textfield = (JTextField)comp;
						good = Good.valueOf(textfield.getName());
						value = Util.parseInt(textfield.getText());
						value = Math.min(value > 0 ? value : 0, player.getGood(good));
						if(value > 0) {
							food += value * good.getFoodValue();
							if(good.equals(Good.Franc)) money = value;
							payment.add(value, good);
						}
					}
				}
				boolean OK = true;
				if(moneyEntry == 0 || money < moneyEntry) {
					if(food < foodEntry) {
						control.showError("Unpaid");
						return;
					}
					double delta = food - foodEntry;
					for(GoodsPair pair: payment) {
						if(pair.getGood().getFoodValue() > delta) continue;
						OK = false;
						break;
					}
				} else if(food > money || money > moneyEntry) OK = false;
				if(!OK) control.showError("Overpaid");
				else {
					goodsDialog.goods.addAll(payment);
					dialog.setVisible(false);
					dialog.dispose();
				}
			}
		};
		StringBuilder msg = new StringBuilder();
		msg.append(Util.getColored(dict, new GoodsPair(foodEntry, Good.Food)));
		if(moneyEntry > 0) {
			msg.append(" ");
			msg.append(dict.get("or"));
			msg.append(" ");
			msg.append(Util.getColored(dict, new GoodsPair(moneyEntry, Good.Franc)));
		}
		String descr = String.format(dict.get("entryDescr"), msg);
		Dimension size = new GUIHelper("entry").getSize("Descr");
		showDialog(dialog, DialogWindow.createContents(dict, descr, size, inner, action));
		return goodsDialog.getGoods();
	}

	/**
	 *	Creates a new energy dialog.
	 *	Returns the goods paid as list.
	 *	@param control the control object
	 *	@param demand the energy to pay
	 *	@return the list of goods
	 */
	public static GoodsList showEnergyDialog(LeHavre control, double demand) {
		Player player = control.getGameState().getActivePlayer();
		double energy = player.getEnergy();
		demand -= (player.getPotentialEnergy() - energy);
		if(demand <= 0) return new GoodsList();
		double delta = energy - demand;
		boolean exact = true;
		GoodsList goods = new GoodsList();
		int amount;
		double value;
		for(Good good: Good.values()) {
			value = good.getEnergyValue();
			if(value == 0) continue;
			amount = player.getGood(good);
			if(amount > 0) {
				goods.add(amount, good);
				if(value <= delta) exact = false;
			}
		}
		if(exact) return goods;
		else if(goods.size() == 1) {
			Good good = goods.get(0).getGood();
			goods.clear();
			goods.add(Math.ceil(demand / good.getEnergyValue()), good);
			return goods;
		} else return createEnergyDialog(control, goods, demand);
	}

	/**
	 *	Creates the energy payment dialog.
	 *	Returns the list of goods paid.
	 *	@param control the control object
	 *	@param goods the player's goods
	 *	@param demand the energy to pay
	 *	@return the list of goods
	 */
	private static GoodsList createEnergyDialog(final LeHavre control, GoodsList goods, final double demand) {
		final Player player = control.getGameState().getActivePlayer();
		Dictionary dict = control.getDictionary();
		JLabel sumLabel = DialogWindow.createSigmaLabel();
		final JPanel inner = DialogWindow.createInnerPanel(control.network,dict, goods);
		final ArrayList<JTextField> textfields = new ArrayList<JTextField>();
		Hashtable<Good, JLabel> labels = new Hashtable<Good, JLabel>();
		LimitStrategy limStrat = new LimitStrategy() {
			public double limit(double amount, Good good) {
				return Math.min(amount > 0 ? amount : 0, player.getGood(good));
			}
		};
		SumStrategy sumStrat = new SumStrategy() {
			public double compute(double amount, Good good) {
				return amount * good.getEnergyValue();
			}
		};
		DialogWindow.addSumFunctionality(inner, sumLabel, limStrat, sumStrat, textfields, labels);
		final GoodsDialog goodsDialog = new GoodsDialog(control);
		final JDialog dialog = createDialog(control, Util.getTitle(dict, "energy", player));
		ActionListener action = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GoodsList payment = new GoodsList();
				int energy = 0, value;
				Good good;
				for(JTextField textfield: textfields) {
					good = Good.valueOf(textfield.getName());
					value = Util.parseInt(textfield.getText());
					value = Math.min(value > 0 ? value : 0, player.getGood(good));
					if(value > 0) {
						energy += value * good.getEnergyValue();
						payment.add(value, good);
					}
				}
				boolean OK = true;
				if(energy < demand) {
					control.showError("Unpaid");
					return;
				} else if(energy > demand) {
					double delta = energy - demand;
					for(GoodsPair pair: payment) {
						if(pair.getGood().getEnergyValue() > delta) continue;
						OK = false;
						break;
					}
				}
				if(!OK) control.showError("Overpaid");
				else {
					goodsDialog.goods.addAll(payment);
					dialog.setVisible(false);
					dialog.dispose();
				}
			}
		};
		String descr = String.format(dict.get("energyDescr"), dict.get("goodEnergy"), Util.getColored(dict, new GoodsPair(demand, Good.Energy)));
		Dimension size = new GUIHelper("energy").getSize("Descr");
		showDialog(dialog, DialogWindow.createContents(dict, descr, size, inner, action));
		return goodsDialog.getGoods();
	}

	/**
	 *	Creates a new food dialog.
	 *	Returns the goods paid as list.
	 *	@param control the control object
	 *	@param demand the energy to pay
	 *	@return the list of goods
	 */
	public static GoodsList showFoodDialog(LeHavre control, double demand) {
		Player player = control.getGameState().getActivePlayer();
		double food = player.getFood();
		double delta = food - demand;
		boolean exact = true;
		GoodsList goods = new GoodsList();
		int amount;
		double value;
		for(Good good: Good.values()) {
			value = good.getFoodValue();
			if(value == 0) continue;
			amount = player.getGood(good);
			if(amount > 0) {
				goods.add(amount, good);
				if(value <= delta) exact = false;
			}
		}
		if(exact) return goods;
		else if(goods.size() == 1) {
			Good good = goods.get(0).getGood();
			goods.clear();
			goods.add(Math.ceil(demand / good.getFoodValue()), good);
			return goods;
		} else return createFoodDialog(control, goods, demand);
	}

	/**
	 *	Creates the food payment dialog.
	 *	Returns the list of goods paid.
	 *	@param control the control object
	 *	@param demand the energy to pay
	 *	@return the list of goods
	 */
	private static GoodsList createFoodDialog(final LeHavre control, GoodsList goods, final double demand) {
		final Player player = control.getGameState().getActivePlayer();
		Dictionary dict = control.getDictionary();
		JLabel sumLabel = DialogWindow.createSigmaLabel();
		final JPanel inner = DialogWindow.createInnerPanel(control.network,dict, goods);
		final ArrayList<JTextField> textfields = new ArrayList<JTextField>();
		Hashtable<Good, JLabel> labels = new Hashtable<Good, JLabel>();
		LimitStrategy limStrat = new LimitStrategy() {
			public double limit(double amount, Good good) {
				return Math.min(amount > 0 ? amount : 0, player.getGood(good));
			}
		};
		SumStrategy sumStrat = new SumStrategy() {
			public double compute(double amount, Good good) {
				return amount * good.getFoodValue();
			}
		};
		DialogWindow.addSumFunctionality(inner, sumLabel, limStrat, sumStrat, textfields, labels);
		final GoodsDialog goodsDialog = new GoodsDialog(control);
		final JDialog dialog = createDialog(control, Util.getTitle(dict, "food", player));
		ActionListener action = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GoodsList payment = new GoodsList();
				int food = 0, value;
				Good good;
				for(JTextField textfield: textfields) {
					good = Good.valueOf(textfield.getName());
					value = Util.parseInt(textfield.getText());
					value = Math.min(value > 0 ? value : 0, player.getGood(good));
					if(value > 0) {
						food += value * good.getFoodValue();
						payment.add(value, good);
					}
				}
				boolean OK = true;
				if(food < demand) {
					double amount = Math.ceil(demand - food);
					int francs = player.getGood(Good.Franc);
					if(francs >= amount && control.confirm("FoodAll")) {
						payment.add(amount, Good.Franc);
					} else {
						control.showError("Unpaid");
						return;
					}
				} else if(food > demand) {
					double delta = food - demand;
					for(GoodsPair pair: payment) {
						if(pair.getGood().getFoodValue() > delta) continue;
						OK = false;
						break;
					}
				}
				if(!OK) control.showError("Overpaid");
				else {
					goodsDialog.goods.addAll(payment);
					dialog.setVisible(false);
					dialog.dispose();
				}
			}
		};
		String descr = String.format(dict.get("energyDescr"), dict.get("goodFood"), Util.getColored(dict, new GoodsPair(demand, Good.Food)));
		Dimension size = new GUIHelper("energy").getSize("Descr");
		showDialog(dialog, DialogWindow.createContents(dict, descr, size, inner, action));
		return goodsDialog.getGoods();
	}

	/**
	 *	Creates a new process dialog.
	 *	Returns the goods paid as list.
	 *	@param control the control object
	 *	@param input the goods input
	 *	@param output the goods output
	 *	@param energyBase the energy to start
	 *	@param energyUnit the energy per unit
	 *	@param max the maximum amount
	 *	@return the list of goods
	 */
	public static GoodsList showProcessDialog(LeHavre control, GoodsList input, GoodsList output, double energyBase, double energyUnit, int max) {
		return showProcessDialog(control, input, output, energyBase, energyUnit, max, false);
	}

	/**
	 *	Creates a new process dialog.
	 *	Returns the goods paid as list.
	 *	@param control the control object
	 *	@param input the goods input
	 *	@param output the goods output
	 *	@param energyBase the energy to start
	 *	@param energyUnit the energy per unit
	 *	@param max the maximum amount
	 *	@param allowNoInput provide true to allow no input
	 *	@return the list of goods
	 */
	public static GoodsList showProcessDialog(LeHavre control, GoodsList input, GoodsList output,
											double energyBase, double energyUnit, int max, boolean allowNoInput) {
		Player player = control.getGameState().getActivePlayer();
		if(max <= 0) max = Integer.MAX_VALUE;
		for(GoodsPair pair: input) max = (int)Math.min(max, player.getGood2(pair.getGood()) / pair.getAmount());
		if(energyUnit > 0) max = (int)Math.min(max, (player.getPotentialEnergy() - energyBase) / energyUnit);
		if(max > 1 || allowNoInput) return createProcessDialog(control, input, output, energyBase, energyUnit, max, allowNoInput);
		GoodsList payment = new GoodsList();
		payment.addAll(showEnergyDialog(control, energyBase + energyUnit));
		payment.addAll(getInput(player, 1, input));
		GoodsList goods = new GoodsList();
		for(GoodsPair pair: payment) goods.add(-Math.ceil(pair.getAmount()), pair.getGood());
		for(GoodsPair pair: output) goods.add(Math.floor(pair.getAmount()), pair.getGood());
		return goods;
	}

	/**
	 *	Creates the process dialog.
	 *	Returns the list of goods paid.
	 *	@param control the control object
	 *	@param input the goods input
	 *	@param output the goods output
	 *	@param energyBase the energy to start
	 *	@param energyUnit the energy per unit
	 *	@param max the maximum amount
	 *	@param allowNoInput provide true to allow no input
	 *	@return the list of goods
	 */
	private static GoodsList createProcessDialog(final LeHavre control, final GoodsList input, final GoodsList output,
												final double energyBase, final double energyUnit, final int max, final boolean allowNoInput) {
		if(allowNoInput && max <= 0) return new GoodsList();
		final Player player = control.getGameState().getActivePlayer();
		Dictionary dict = control.getDictionary();
		//GUIHelper gui = new GUIHelper("dialog");
		final JPanel inner = DialogWindow.createItem(control.network,dict, null, max);
		final JTextField textfield = (JTextField)inner.getComponent(1);
		final GoodsDialog goodsDialog = new GoodsDialog(control);
		final JDialog dialog = createDialog(control, Util.getTitle(dict, "process", player));
		ActionListener action = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int amount = Util.parseInt(textfield.getText());
				amount = Math.min(amount > 0 ? amount : 0, max);
				if(!allowNoInput && amount == 0) {
					if(control.confirm("InputAll")) amount = max;
					else return;
				}
				if(amount > 0) {
					GoodsList payment = showEnergyDialog(control, energyBase + amount * energyUnit);
					payment.addAll(getInput(player, amount, input));
					GoodsList goods = new GoodsList();
					for(GoodsPair pair: payment) goods.add(-pair.getAmount(), pair.getGood());
					for(GoodsPair pair: output) goods.add(Math.floor(amount * pair.getAmount()), pair.getGood());
					goodsDialog.goods.addAll(goods);
				}
				dialog.setVisible(false);
				dialog.dispose();
			}
		};
		String descr = String.format(dict.get("processDescr"), Util.getColored(dict, input), Util.getColored(dict, output));
		Dimension size = new GUIHelper("process").getSize("Descr");
		showDialog(dialog, DialogWindow.createContents(dict, descr, size, inner, action));
		return goodsDialog.getGoods();
	}

	/**
	 *	Returns the list of goods paid as input. Brick will be used instead of clay and
	 *	steel instead of iron if the given player is missing part of these resources.
	 *	@param player the player
	 *	@param amount the amount of units
	 *	@param input the list of goods per unit
	 */
	private static GoodsList getInput(Player player, int amount, GoodsList input) {
		GoodsList payment = new GoodsList();
		int sum, value;
		Good good;
		for(GoodsPair pair: input) {
			good = pair.getGood();
			value = player.getGood(good);
			sum = (int)Math.ceil(amount * pair.getAmount());
			if(value < sum) {
				if(good.equals(Good.Clay)) payment.add(sum - value, Good.Brick);
				else if(good.equals(Good.Iron)) payment.add(sum - value, Good.Steel);
				sum = value;
			}
			if(sum > 0) payment.add(sum, good);
		}
		return payment;
	}

	/**
	 *	Creates a new choice dialog.
	 *	Returns the goods chosen as list.
	 *	@param control the control object
	 *	@param options the goods to choose from
	 *	@param min the minimum amount
	 *	@param max the maximum amount
	 *	@param lose provide true if the chosen goods will be lost
	 *	@param text optional additional text
	 *	@param height the height of the optional text
	 *	@return the list of goods
	 */
	public static GoodsList showChoiceDialog(LeHavre control, GoodsList options, int min, int max, boolean lose, String text, int height) {
		return showChoiceDialog(control, options, min, max, lose, text, height, false);
	}

	/**
	 *	Creates a new choice dialog.
	 *	Returns the goods chosen as list.
	 *	@param control the control object
	 *	@param options the goods to choose from
	 *	@param min the minimum amount
	 *	@param max the maximum amount
	 *	@param lose provide true if the chosen goods will be lost
	 *	@param text optional additional text
	 *	@param height the height of the optional text
	 *	@param allowNoInput provide true to allow no input
	 *	@return the list of goods
	 */
	public static GoodsList showChoiceDialog(LeHavre control, GoodsList options, int min, int max,
											boolean lose, String text, int height, boolean allowNoInput) {
		int amount = 0;
		for(GoodsPair pair: options) amount += (int)pair.getAmount();
		if(allowNoInput || min < amount) return createChoiceDialog(control, options, min, max, lose, text, height, allowNoInput);
		return options;
	}

	/**
	 *	Creates a new choice dialog.
	 *	Returns the goods chosen as list.
	 *	@param control the control object
	 *	@param options the goods to choose from
	 *	@param min the minimum amount
	 *	@param max the maximum amount
	 *	@param lose provide true if the chosen goods will be lost
	 *	@return the list of goods
	 */
	public static GoodsList showChoiceDialog(LeHavre control, GoodsList options, int min, int max, boolean lose) {
		return showChoiceDialog(control, options, min, max, lose, false);
	}

	/**
	 *	Creates a new choice dialog.
	 *	Returns the goods chosen as list.
	 *	@param control the control object
	 *	@param options the goods to choose from
	 *	@param min the minimum amount
	 *	@param max the maximum amount
	 *	@param lose provide true if the chosen goods will be lost
	 *	@param allowNoInput provide true to allow no input
	 *	@return the list of goods
	 */
	public static GoodsList showChoiceDialog(LeHavre control, GoodsList options, int min, int max, boolean lose, boolean allowNoInput) {
		return showChoiceDialog(control, options, min, max, lose, null, 0, allowNoInput);
	}

	/**
	 *	Creates the choice dialog.
	 *	Returns the goods chosen as list.
	 *	@param control the control object
	 *	@param options the goods to choose from
	 *	@param min the minimum amount
	 *	@param max the maximum amount
	 *	@param lose provide true if the chosen goods will be lost
	 *	@param text optional additional text
	 *	@param height the height of the optional text
	 *	@param allowNoInput provide true to allow no input
	 *	@return the list of goods
	 */
	private static GoodsList createChoiceDialog(final LeHavre control, final GoodsList options, final int min, final int max,
												boolean lose, String text, int height, final boolean allowNoInput) {
		final Player player = control.getGameState().getActivePlayer();
		final Dictionary dict = control.getDictionary();
		JLabel sumLabel = DialogWindow.createSigmaLabel();
		final JPanel inner = DialogWindow.createInnerPanel(control.network,dict, options);
		final ArrayList<JTextField> textfields = new ArrayList<JTextField>();
		Hashtable<Good, JLabel> labels = new Hashtable<Good, JLabel>();
		LimitStrategy limStrat = new LimitStrategy() {
			public double limit(double amount, Good good) {
				double max = 0;
				for(GoodsPair pair: options) if(pair.getGood().equals(good)) max = pair.getAmount();
				return Math.min(amount > 0 ? amount : 0, max);
			}
		};
		SumStrategy sumStrat = new SumStrategy() {
			public double compute(double amount, Good good) {
				return amount;
			}
		};
		DialogWindow.addSumFunctionality(inner, sumLabel, limStrat, sumStrat, textfields, labels);
 		final GoodsDialog goodsDialog = new GoodsDialog(control);
		final JDialog dialog = createDialog(control, Util.getTitle(dict, "choice", player));
		ActionListener action = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GoodsList goods = new GoodsList();
				int amount = 0, k = 0, value;
				Good good;
				for(JTextField textfield: textfields) {
					good = Good.valueOf(textfield.getName());
					value = Util.parseInt(textfield.getText());
					value = Math.min(value > 0 ? value : 0, (int)options.get(k++).getAmount());
					if(value > 0) {
						amount += value;
						goods.add(value, good);
					}
				}
				boolean ok = (allowNoInput && (amount == 0));
				if(!ok) {
					if(amount < min) {
						control.showError2(String.format(dict.get("errTooFewChosen"), amount, min));
						return;
					}
					if(amount > max) {
						control.showError2(String.format(dict.get("errTooManyChosen"), amount, max));
						return;
					}
				}
				if(ok || amount == max || control.confirm2(String.format(dict.get("errFewerChosen"), Util.getNumbered(dict, max, "good"), amount))) {
					goodsDialog.goods.addAll(goods);
					dialog.setVisible(false);
					dialog.dispose();
				}
			}
		};
		String descr;
		if(text == null) text = "";
		if(min != max) descr = String.format(dict.get("choiceDescr"), dict.get(lose ? "lose" : "receive"), min, max, text);
		else descr = String.format(dict.get("choiceDescr2"), dict.get(lose ? "lose" : "receive"), max, text);
		Dimension size = new GUIHelper("choice").getSize("Descr");
		size.height += height;
		showDialog(dialog, DialogWindow.createContents(dict, descr, size, inner, action));
		return goodsDialog.getGoods();
	}
}