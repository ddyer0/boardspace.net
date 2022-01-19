package lehavre.view;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.Util;

/**
 *
 *	The <code>ScoringWindow</code> class is the dialog window that opens
 *	at the end of the game and shows the game end statistics and the winner.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/2/17
 */
public final class ScoringWindow
extends ControlledWindow
{
	static final long serialVersionUID =1L;
	/**
	 *	Creates a new <code>ScoringWindow</code> instance.
	 *	@param control the control object
	 */
	public ScoringWindow(LeHavre control) {
		super(control, "score");
		GameState game = control.getGameState();
		Player[] order = game.getPlayers().toArray(new Player[]{});
		Arrays.sort(order);
		final int n = order.length;
		Integer[] scores = new Integer[n];
		int k = 0, max = 0, value;
		StringBuilder winner = new StringBuilder();
		for(int i = 0; i < n; i++) {
			value = order[i].getPoints();
			if(i == 0) max = value;
			scores[i] = value;
			if(value == max) {
				if(winner.length() > 0) winner.append(", ");
				winner.append(order[i].getName());
				k++;
			}
		}
		value = winner.lastIndexOf(", ");
		if(value >= 0) winner.replace(value, value + 1, " " + get("and"));

		/* Create the bounding panel */
		JPanel panel = new JPanel(new BorderLayout());
		Insets in = gui.getPadding("Window");
		panel.setBorder(BorderFactory.createEmptyBorder(in.top, in.left, in.bottom, in.right));
		panel.setBackground(gui.getColor("Window"));

		/* Create the description */
		JEditorPane descr = new JEditorPane();
		descr.setContentType("text/html");
		descr.setText(String.format(get("scoreDescr"), winner, get(k > 1 ? "win" : "wins"), Util.getNumbered(control.getDictionary(), max, "scorePoint")));
		descr.setPreferredSize(gui.getSize("Descr"));
		descr.setEditable(false);
		descr.setOpaque(false);
		panel.add(descr, BorderLayout.NORTH);

		/* Create the ranking table */
		JPanel outer = new JPanel(new FlowLayout(FlowLayout.CENTER));
		outer.setOpaque(false);
		panel.add(outer, BorderLayout.CENTER);
		JPanel inner = new JPanel(new SpringLayout());
		inner.setOpaque(false);
		outer.add(inner);
		String[] titles = {get("rank"), get("name"), get("money"), get("ships"), get("buildings"), get("debts"), get("score")};
		JLabel label;
		Font font = FONT16;
		Border border = BorderFactory.createCompoundBorder(
			BorderFactory.createLoweredBevelBorder(),
			BorderFactory.createEmptyBorder(2, 7, 3, 7)
		);
		Color color = gui.getColor("Title");
		Color color2 = gui.getColor("Window");
		for(String title: titles) {
			label = new JLabel(title, JLabel.CENTER);
			label.setBackground(color);
			label.setForeground(Color.white);
			label.setOpaque(true);
			label.setBorder(border);
			label.setFont(font);
			inner.add(label);
		}
		int rows = n + 1;
		int cols = titles.length;
		int bonus, bValue, sValue;
		Integer prev = null;
		int rank = 0;
		color = gui.getColor("Cell");
		color2 = gui.getColor("Points");
		StringBuilder msg;
		Player player;
		String text;
		for(int i = 0; i < n; i++) {
			msg = new StringBuilder();
			if(!scores[i].equals(prev)) {
				prev = scores[i];
				rank++;
			}
			player = order[i];
			text = String.format("%d.", rank);
			msg.append(text);
			msg.append(" ");
			label = new JLabel(text, JLabel.CENTER);
			label.setBackground(color);
			label.setOpaque(true);
			label.setBorder(border);
			label.setFont(font);
			inner.add(label);
			text = player.getName();
			msg.append(text);
			msg.append(" (");
			label = new JLabel(text, JLabel.CENTER);
			label.setBackground(player.getColor().toColor());
			label.setOpaque(true);
			label.setBorder(border);
			label.setFont(font);
			inner.add(label);
			text = String.valueOf(player.getMoney());
			msg.append(text);
			msg.append("|");
			label = new JLabel(text, JLabel.CENTER);
			label.setBackground(color);
			label.setOpaque(true);
			label.setBorder(border);
			label.setFont(font);
			inner.add(label);
			bonus = bValue = sValue = 0;
			for(Ship ship: player.getShips()) sValue += ship.getValue();
			for(Building building: player.getBuildings()) {
				if(building.isShip()) sValue += building.getValue();
				else bValue += building.getValue();
				if(building.isBonus()) bonus += building.getBonus(player);
			}
			text = String.valueOf(sValue);
			msg.append(text);
			msg.append("|");
			label = new JLabel(text, JLabel.CENTER);
			label.setBackground(color);
			label.setOpaque(true);
			label.setBorder(border);
			label.setFont(font);
			inner.add(label);
			text = String.format("%d+%d", bValue, bonus);
			msg.append(text);
			msg.append("|");
			label = new JLabel(text, JLabel.CENTER);
			label.setBackground(color);
			label.setOpaque(true);
			label.setBorder(border);
			label.setFont(font);
			inner.add(label);
			text = String.valueOf(-player.getLoans() * GameState.LOAN_PENALTY);
			msg.append(text);
			msg.append(") = ");
			label = new JLabel(text, JLabel.CENTER);
			label.setBackground(color);
			label.setOpaque(true);
			label.setBorder(border);
			label.setFont(font);
			inner.add(label);
			text = prev.toString();
			msg.append(text);
			label = new JLabel(text, JLabel.CENTER);
			label.setBackground(color2);
			label.setOpaque(true);
			label.setBorder(border);
			label.setFont(font);
			inner.add(label);
			log(msg.toString());
		}
		SpringUtilities.makeCompactGrid(inner, rows, cols, 0, 20, 5, 5);

		/* Create the scroll pane */
		JScrollPane scroll = new JScrollPane(panel);
		scroll.getVerticalScrollBar().setUnitIncrement(gui.getInt("Unit"));
		getContentPane().add(scroll, BorderLayout.CENTER);

		/* Create the button panel */
		panel = new JPanel();
		getContentPane().add(panel, BorderLayout.SOUTH);

		/* Create the close button */
		JButton button = new JButton(get("quit"));
		button.setFont(FONT16);
		button.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					quit();
				}
			}
		);
		panel.add(button);

		/* Set window bounds. */
		setSize(gui.getSize("Window"));
		setLocationRelativeTo(null);
		setTitle(get("scoreTitle"));
		setVisible(true);
	}

	/** Closes the window. */
	public void quit() {
		if(confirm("Quit")) control.quit();
	}
}