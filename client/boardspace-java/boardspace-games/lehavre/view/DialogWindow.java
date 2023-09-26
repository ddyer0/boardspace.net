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
import javax.swing.event.*;

import lehavre.main.*;
import lehavre.main.Dictionary;
import lehavre.model.*;
import lehavre.util.*;
import lib.G;

/**
 *
 *	The <code>DialogWindow</code> class is the super class for
 *	non-modal game dialogs where the player has to pay something.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/2/7
 */
public abstract class DialogWindow
extends ControlledWindow
{
	static final long serialVersionUID =1L;
	/** The HTML code for the sigma symbol. */
	private static final String SIGMA_SYMBOL = "<html><b style=\"color:navy;\">&sum; %s</b></html>";

	/**
	 *	Creates a new <code>DialogWindow</code> instance.
	 *	@param control the control object
	 */
	public DialogWindow(LeHavre control) {
		super(control, null);
	}

	/**
	 *	Returns the contents panel.
	 *	@param dictionary the dictionary
	 *	@param description the description
	 *	@param size the description size
	 *	@param inner the inner panel
	 *	@param action the button action
	 *	@return the contents panel
	 */
	public static final JPanel createContents(Dictionary dictionary, String description, Dimension size, JPanel inner, ActionListener action) {
		GUIHelper gui = new GUIHelper("dialog");
		JPanel contents = new JPanel(new BorderLayout());
		contents.setBackground(gui.getColor("Window"));
		Insets in = gui.getPadding("Window");
		contents.setBorder(BorderFactory.createEmptyBorder(in.top, in.left, in.bottom, in.right));

		/* Create the description label */
		JEditorPane descr = new JEditorPane();
		descr.setContentType("text/html");
		descr.setText(description);
		descr.setPreferredSize(size != null ? size : gui.getSize("Descr"));
		descr.setEditable(false);
		descr.setOpaque(false);
		contents.add(descr, BorderLayout.NORTH);

		/* Add the inner panel */
		if(inner != null) contents.add(inner, BorderLayout.CENTER);

		/* Add the button */
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, gui.getInt("ButtonHGap"), gui.getInt("ButtonVGap")));
		panel.setOpaque(false);
		contents.add(panel, BorderLayout.SOUTH);
		JButton button = new JButton(dictionary.get("apply"));
		button.setFont(FONT16);
		button.addActionListener(action);
		panel.add(button);
		return contents;
	}

	/**
	 *	Creates the contents.
	 *	@param player the player object
	 *	@param type the prefix for the GUI helper
	 *	@param description the description text
	 *	@param inner the central panel
	 *	@param action the action listener for the button
	 */
	protected void create(Player player, String type, String description, JPanel inner, ActionListener action) {
		Dictionary dict = control.getDictionary();
		setTitle(Util.getTitle(dict, type, player));
		getContentPane().add(createContents(dict, description, null, inner, action));
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	/**
	 *	Creates a panel that displays the given list of goods and returns it.
	 *	@param dictionary the dictionary
	 *	@param goods the list of goods
	 *	@param sumLabel an optional sum label
	 *	@return the inner panel
	 */
	public static JPanel createInnerPanel(NetworkInterface net,Dictionary dictionary, GoodsList goods, JLabel sumLabel) {
		GUIHelper gui = new GUIHelper("dialog");
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, gui.getInt("LineHGap"), gui.getInt("LineVGap")));
		int n = (sumLabel != null ? 1 : 0);
		n = (int)Math.ceil((goods.size() + n) / (double)gui.getInt("ItemCount"));
		Dimension size = gui.getSize("Line");
		panel.setPreferredSize(new Dimension(G.Width(size), n * G.Height(size)));
		panel.setOpaque(false);

		/* Create the goods panels */
		for(GoodsPair pair: goods) panel.add(createItem(net,dictionary, pair.getGood(), (int)pair.getAmount()));
		if(sumLabel != null) panel.add(sumLabel);
		return panel;
	}

	/**
	 *	Creates a panel that displays the given list of goods and returns it.
	 *	@param dictionary the dictionary
	 *	@param goods the list of goods
	 *	@return the inner panel
	 */
	public static JPanel createInnerPanel(NetworkInterface network,Dictionary dictionary, GoodsList goods) {
		return createInnerPanel(network,dictionary, goods, null);
	}

	/**
	 *	Creates an item for a dialog window. An item consists of an icon,
	 *	a textfield and a maximum amount that may be entered in the textfield.
	 *	@param dictionary the dictionary
	 *	@param good the good to display or null to display the process icon
	 *	@param max the maximum amount
	 *	@return the item
	 */
	public static JPanel createItem(NetworkInterface net,Dictionary dictionary, Good good, int max) {
		JPanel item = new JPanel();
		boolean generic = (good == null || good.equals(Good.Food) || good.equals(Good.Energy));
		GUIHelper gui = new GUIHelper("dialog");
		if(generic) item.setLayout(new FlowLayout(FlowLayout.CENTER, gui.getInt("EntryHGap"), gui.getInt("EntryVGap")));
		else item.setLayout(new BorderLayout(gui.getInt("EntryHGap"), gui.getInt("EntryVGap")));
		item.setOpaque(false);
		ImageLabel iLabel = new ImageLabel(net,
			generic ? null : dictionary.getLanguage(),
			generic ? "symbols/process" : String.format(MainWindow.GOODS_PATH, good),
			!generic
		);
		item.add(iLabel, BorderLayout.WEST);
		JTextField input = new JTextField();
		if(!generic) {
			input.setColumns(gui.getInt("TextWidth"));
			input.setName(good.toString());
		}
		input.setToolTipText(dictionary.get("popupInput"));
		input.setHorizontalAlignment(JTextField.CENTER);
		if(generic) input.setPreferredSize(gui.getSize("Text"));
		item.add(input, BorderLayout.CENTER);
		iLabel.addMouseListener(createIconListener(input, max));
		// show max amount
		JLabel label = new JLabel(String.format("(%s)", Util.format(max)));
		if(!generic) label.setName(good.toString());
		label.setFont(FONT16);
		item.add(label, BorderLayout.EAST);
		return item;
	}

	/**
	 *	Creates the mouse listener for addition or subtraction on mouse click.
	 *	@param textfield the textfield to enter the values
	 *	@param max the maximum amount that may be entered
	 *	@return the mouse listener
	 */
	public static MouseListener createIconListener(final JTextField textfield, final int max) {
		return new MouseAdapter() {
			/* Adds to or subtracts from the value by clicking the icon */
			public void mouseClicked(MouseEvent e) {
				int delta = (e.getButton() == MouseEvent.BUTTON1 ? +1 : -1);
				int mask = MouseEvent.SHIFT_DOWN_MASK;
				int mod = e.getModifiersEx();
				if((mod & mask) == mask) delta *= 5;
				int value;
				try {
					value = Integer.parseInt(textfield.getText());
				} catch(NumberFormatException ex) {
					value = 0;
				}
				value += delta;
				if(value < 0) value = 0;
				if(max > 0 && value > max) value = max;
				textfield.setText(String.valueOf(value));
			}
		};
	}

	/**
	 *	Returns the sigma symbol with the given number attached.
	 *	@param num the number
	 *	@return the sigma symbol
	 */
	private static String getSigmaSymbol(double num) {
		String sum;
		if(num == Math.floor(num)) sum = String.format("%d", Integer.valueOf((int)num));
		else sum = String.format("%1.1f", num);
		return String.format(SIGMA_SYMBOL, sum);
	}

	/**
	 *	Returns a label with the sigma symbol.
	 *	@return a label with the sigma symbol
	 */
	public static JLabel createSigmaLabel() {
		JLabel sumLabel = new JLabel(getSigmaSymbol(0));
		sumLabel.setPreferredSize((new GUIHelper("dialog")).getSize("Sum"));
		sumLabel.setFont(FONT16);
		return sumLabel;
	}

	/**
	 *	Adds the sum functionality to the given panel based on the given
	 *	limitation and summation strategies.
	 *	@param panel the panel
	 *	@param sumLabel the sum label
	 *	@param limStrat the limitation strategy
	 *	@param sumStrat the summation strategy
	 */
	public static void addSumFunctionality(JPanel panel, JLabel sumLabel, LimitStrategy limStrat, SumStrategy sumStrat) {
		addSumFunctionality(panel, sumLabel, limStrat, sumStrat, new ArrayList<JTextField>(), new Hashtable<Good, JLabel>());
	}

	/**
	 *	Adds the sum functionality to the given panel based on the given
	 *	limitation and summation strategies.
	 *	@param panel the panel
	 *	@param sumLabel the sum label
	 *	@param limStrat the limitation strategy
	 *	@param sumStrat the summation strategy
	 *	@param textfields the textfields
	 *	@param labels the good labels
	 */
	public static void addSumFunctionality(JPanel panel, final JLabel sumLabel, final LimitStrategy limStrat, final SumStrategy sumStrat, final ArrayList<JTextField> textfields, final Hashtable<Good, JLabel> labels) {
		// read textfields and labels from panel
		for(Component component: panel.getComponents()) {
			if(!(component instanceof Container)) continue;
			for(Component comp: ((Container)component).getComponents()) {
				if(comp instanceof JTextField) textfields.add((JTextField)comp);
				else if(!(comp instanceof ImageLabel) && (comp instanceof JLabel)) labels.put(Good.valueOf(comp.getName()), (JLabel)comp);
			}
		}
		// add sum functionality
		for(JTextField textfield: textfields) textfield.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent e) {
				double sum = 0;
				for(JTextField textfield: textfields) {
					Good good = Good.valueOf(textfield.getName());
					double value = Util.parseInt(textfield.getText());
					value = limStrat.limit(value, good);
					sum += sumStrat.compute(value, good);
				}
				sumLabel.setText(DialogWindow.getSigmaSymbol(sum));
			}
		});
	}

	/**
	 *	DO NOT close the window.
	 */
	public void quit() {
		control.showError("DialogClose");
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				invalidate();
				repaint();
			}
		});
	}
}