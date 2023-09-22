/* copyright notice */package lehavre.view;

import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import lehavre.main.*;
import lehavre.model.*;
import lehavre.util.*;
import lib.SimpleObservable;
import lib.SimpleObserver;

/**
 *
 *	The <code>FoodWindow</code> class is the dialog displayed at the end
 *	of each round when each player has to pay the food demand.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/2/6
 */
public final class FoodWindow
extends DialogWindow
{
	static final long serialVersionUID =1L;
	/**
	 *	Creates a new <code>FoodWindow</code> instance.
	 *	@param control the control object
	 *	@param player the player object
	 *	@param demand the food demand
	 */
	public FoodWindow(final LeHavre control, final Player player, final int demand) {
		super(control);
		String description = String.format(get("foodDescr"), Util.getColored(control.getDictionary(), new GoodsPair(demand, Good.Food)));
		JLabel sumLabel = createSigmaLabel();
		final JPanel inner = createInnerPanel(control.network,control.getDictionary(), createFoodList(player), sumLabel);
		final ArrayList<JTextField> textfields = new ArrayList<JTextField>();
		final Hashtable<Good, JLabel> labels = new Hashtable<Good, JLabel>();
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
		addSumFunctionality(inner, sumLabel, limStrat, sumStrat, textfields, labels);
		final SimpleObserver observer = new SimpleObserver() {
			public void update(SimpleObservable obs, Object eventType, Object obj) {
				if(obj instanceof String) {
					StringTokenizer tok = new StringTokenizer((String)obj, "=");
					if(!tok.nextToken().equals("good")) return;
					tok = new StringTokenizer(tok.nextToken(), ",");
					Good good = Good.valueOf(tok.nextToken());
					@SuppressWarnings("unused")
					int oldValue = Integer.parseInt(tok.nextToken());
					int newValue = Integer.parseInt(tok.nextToken());
					labels.get(good).setText(String.format("(%d)", newValue));
				}
			}
		};
		player.addObserver(observer);
		ActionListener action = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				double sum = player.getFood();
				int money = player.getMoney();
				double food = sum - money;
				GoodsList payment = new GoodsList();
				if(sum < demand) {
					if(!confirm("FoodLack")) return;
					payment = null;
				} else {
					Good good;
					int paid = 0, value;
					for(JTextField textfield: textfields) {
						good = Good.valueOf(textfield.getName());
						value = Util.parseInt(textfield.getText().trim());
						value = Math.min(value > 0 ? value : 0, player.getGood(good));
						if(value > 0) {
							paid += value * good.getFoodValue();
							payment.add(value, good);
						}
					}
					if(paid < demand) {
						if(food > demand) {
							control.showError("Unpaid");
							return;
						} else {
							if(!confirm("FoodAll")) return;
							payment = null;
						}
					} else if(paid > demand) {
						boolean OK = true;
						int delta = paid - demand;
						for(GoodsPair pair: payment) {
							if(pair.getGood().getFoodValue() > delta) continue;
							OK = false;
							break;
						}
						if(!OK) {
							control.showError("Overpaid");
							return;
						}
					}
				}
				if(payment != null) control.endRound(payment);
				else control.loseAllFood(demand);
				player.removeObserver(observer);
				setVisible(false);
				dispose();
			}
		};

		/* Create the contents */
		create(player, "food", description, inner, action);
	}

	/**
	 *	Creates the list of food for the given player.
	 *	@param player the player
	 */
	public static GoodsList createFoodList(Player player) {
		GoodsList goods = new GoodsList();
		int n;
		for(Good good: Good.values()) {
			if(good.getFoodValue() > 0) {
				n = player.getGood(good);
				if(n > 0 || good.isMoney()) goods.add(n, good);
			}
		}
		return goods;
	}
}