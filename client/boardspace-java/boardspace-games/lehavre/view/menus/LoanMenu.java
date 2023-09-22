/* copyright notice */package lehavre.view.menus;

import java.awt.event.*;
import javax.swing.*;
import lehavre.main.*;
import lehavre.model.*;
import lehavre.util.*;

/**
 *
 *	The <code>LoanMenu</code> class is the context menu of a loan label.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/2/7
 */
public final class LoanMenu
extends JPopupMenu
{
	static final long serialVersionUID =1L;
	/** The dynamic menu item. */
	private JMenuItem payback;

	/**
	 *	Creates a new <code>LoanMenu</code> instance
	 *	for player with the given index.
	 *	@param control the control object
	 *	@param index the index
	 */
	public LoanMenu(final LeHavre control, final int index) {
		Dictionary dict = control.getDictionary();
		payback = new JMenuItem(dict.get("menuPayback"));
		payback.addActionListener(
			new ActionListener() {
				private final Dictionary dict = control.getDictionary();
				public void actionPerformed(ActionEvent e) {
					Player player = control.getGameState().getPlayer(index);
					int money = player.getMoney();
					int payback = GameState.LOAN_PAYBACK;
					if(money < payback) {
						control.showError2(String.format(
							dict.get("errPayback"),
							Util.getColored(dict, new GoodsPair(payback, Good.Franc)),
							Util.getColored(dict, new GoodsPair(money, Good.Franc))
						));
						return;
					}
					int n = Math.min(money / payback, player.getLoans());
					if(n > 1) {
						String input = JOptionPane.showInputDialog(
							LoanMenu.this,
							Util.format(String.format(
								dict.get("popupPaybackInfo"),
								Util.getColored(dict, new GoodsPair(money, Good.Franc)),
								Util.getNumbered(dict, n, "loan")
							)),
							dict.get("popupPayback"),
							JOptionPane.QUESTION_MESSAGE
						);
						int value = Util.parseInt(input);
						if(value < 0) {
							control.showError2(String.format(dict.get("errNaN"), input));
							return;
						}
						n = Math.min(n, value);
					}
					control.paybackLoans(n, false);
				}
			}
		);
		add(payback);
		addSeparator();
		JMenuItem item = new JMenuItem(dict.get("menuInfo"));
		item.addActionListener(
			new ActionListener() {
				private final Dictionary dict;
				private final String title;
				{
					dict = control.getDictionary();
					title = dict.get("popupLoan");
				}
				public void actionPerformed(ActionEvent e) {
					Player player = control.getGameState().getPlayer(index);
					String text = String.format(
						dict.get("popupLoanInfo"),
						player.getName(),
						Util.getNumbered(dict, player.getLoans(), "loan")
					);
					control.showInformation(text, title);
				}
			}
		);
		add(item);
	}

	/**
	 *	Enables or disables the payback option.
	 *	@param enabled provide true to enable the payback option
	 */
	public void setPaybackEnabled(boolean enabled) {
		payback.setEnabled(enabled);
	}
}