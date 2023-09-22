/* copyright notice */package lehavre.view;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import lehavre.main.*;
import lehavre.model.*;
import lehavre.util.*;

/**
 *
 *	The <code>BonusWindow</code> class is the dialog window that opens
 *	if you click on the bonus button in the lobby. It displays all
 *	available goods in Le Havre. You may change the quantities a given
 *	player gets of each resource at game start.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/22
 */
public final class BonusWindow
extends ControlledWindow
{
	static final long serialVersionUID =1L;
	/** The text fields to enter the quantities. */
	private final ArrayList<JTextField> textfields;

	/** The player's index. */
	private final int index;

	/** True if any change was made. */
	private boolean changed = false;

	/**
	 *	Creates a new <code>BonusWindow</code> instance.
	 *	@param control the control object
	 *	@param player the player
	 */
	public BonusWindow(LeHavre control, Player player) {
		super(control, "bonus");
		setTitle(get("bonusTitle"));
		textfields = new ArrayList<JTextField>();
		index = player.getIndex();

		/* Create the contents panel */
		JPanel contents = new JPanel(new BorderLayout());
		contents.setBackground(gui.getColor("Window"));
		Insets in = gui.getPadding("Window");
		contents.setBorder(BorderFactory.createEmptyBorder(in.top, in.left, in.bottom, in.right));
		getContentPane().add(contents, BorderLayout.CENTER);

		/* Create the description label */
		JEditorPane descr = new JEditorPane();
		descr.setContentType("text/html");
		descr.setText(String.format(get("bonusDescr"), player.getName()));
		descr.setPreferredSize(gui.getSize("Descr"));
		descr.setEditable(false);
		descr.setOpaque(false);
		contents.add(descr, BorderLayout.NORTH);

		/* Create the inner panel */
		JPanel panel = new JPanel(new GridLayout(2, 9, gui.getInt("LineHGap"), gui.getInt("LineVGap")));
		panel.setOpaque(false);
		contents.add(panel, BorderLayout.CENTER);

		/* Create the good chits */
		int hgap = gui.getInt("EntryHGap");
		int vgap = gui.getInt("EntryVGap");
		JLabel iLabel;
		JPanel item;
		int k = Good.getBasicCount();
		String language = control.getDictionary().getLanguage();
		for(Good good: Good.values()) {
			if(!good.isPhysical()) continue;
			if(k-- == 0) panel.add(new JLabel());
			item = new JPanel(new BorderLayout(hgap, vgap));
			item.setOpaque(false);
			panel.add(item);
			iLabel = new ImageLabel(control.network,language, String.format(MainWindow.GOODS_PATH, good));
			item.add(new JLabel(iLabel.getIcon()), BorderLayout.CENTER);
			item.add(iLabel, BorderLayout.CENTER);
			final JTextField input = new JTextField();
			input.setName(good.toString());
			input.setToolTipText(get("popupInput"));
			input.setHorizontalAlignment(JTextField.CENTER);
			input.addCaretListener(
				new CaretListener() {
					public void caretUpdate(CaretEvent e) {
						if(!input.getText().trim().equals("")) changed = true;
					}
				}
			);
			textfields.add(input);
			item.add(input, BorderLayout.SOUTH);
			iLabel.addMouseListener(DialogWindow.createIconListener(input, 0));
		}

		/* Create the button panel */
		panel = new JPanel(new GridLayout(1, 2));
		getContentPane().add(panel, BorderLayout.SOUTH);

		/* Create the apply button */
		JButton button = new JButton(get("apply"));
		button.setFont(FONT16);
		button.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					apply();
				}
			}
		);
		panel.add(button);

		/* Create the cancel button */
		button = new JButton(get("cancel"));
		button.setFont(FONT16);
		button.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					quit();
				}
			}
		);
		panel.add(button);

		/* Display the window */
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	/** Applies the changes. */
	private void apply() {
		GoodsList list = new GoodsList();
		int value;
		for(JTextField textfield: textfields) {
			value = Util.parseInt(textfield.getText().trim());
			list.add(value, Good.valueOf(textfield.getName()));
		}
		control.setBonus(index, list);
		changed = false;
		quit();
	}

	/** Closes the window. */
	public void quit() {
		if(changed && confirm("SaveChanges")) apply();
		setVisible(false);
		dispose();
	}
}