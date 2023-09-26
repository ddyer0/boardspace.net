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
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import lehavre.main.*;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.util.*;

/**
 *
 *	The <code>SettingsWindow</code> class is the dialog window that opens
 *	if you chose the settings option from a lobby menu. It displays all
 *	game components that maybe customized before the game starts.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.10 2009/12/28
 */
public final class SettingsWindow
extends ControlledWindow
{
	static final long serialVersionUID =1L;
	/** The maximum width of a textual tooltip window. */
	private static final int TOOLTIP_MAX_WIDTH = 250;

	/** The gaps between elements in a grid layout. */
	private final int HGAP, VGAP;

	/** The special buildings components. */
	private final ArrayList<JCheckBox> specialCheckBoxes;
	@SuppressWarnings("rawtypes")
	private final ArrayList<JComboBox> specialComboBoxes;

	/** The supply chits components. */
	private final ArrayList<JCheckBox> supplyCheckBoxes;
	@SuppressWarnings("rawtypes")
	private final ArrayList<JComboBox> supplyComboBoxes;

	/** The standard buildings components. */
	private final ArrayList<JCheckBox> standardCheckBoxes;
	@SuppressWarnings("rawtypes")
	private final ArrayList<JComboBox> standardComboBoxes;
	private final JCheckBox standardOverride;

	/** The goods chits components. */
	private final ArrayList<JTextField> goodsTextFields;

	/** The extra components. */
	@SuppressWarnings("rawtypes")
	private final JComboBox extraSoloComboBox;
	private final JCheckBox extraLoansCheckBox;
	private final JCheckBox extraPointsCheckBox;

	/** True if any change was made. */
	private boolean changed = false;

	/**
	 *	Creates a new <code>SettingsWindow</code> instance.
	 *	@param control the control object
	 */
	@SuppressWarnings("rawtypes")
	public SettingsWindow(LeHavre control) {
		super(control, "set");
		specialCheckBoxes = new ArrayList<JCheckBox>();
		specialComboBoxes = new ArrayList<JComboBox>();
		supplyCheckBoxes = new ArrayList<JCheckBox>();
		supplyComboBoxes = new ArrayList<JComboBox>();
		standardCheckBoxes = new ArrayList<JCheckBox>();
		standardComboBoxes = new ArrayList<JComboBox>();
		standardOverride = new JCheckBox();
		goodsTextFields = new ArrayList<JTextField>();
		extraSoloComboBox = new JComboBox();
		extraLoansCheckBox = new JCheckBox();
		extraPointsCheckBox = new JCheckBox();
		HGAP = gui.getInt("LineHGap");
		VGAP = gui.getInt("LineVGap");

		/* Create the tabs */
		JTabbedPane tab = new JTabbedPane();
		createSpecialTab(tab);
		createSupplyTab(tab);
		createStandardTab(tab);
		createGoodsTab(tab);
		createExtrasTab(tab);
		getContentPane().add(tab, BorderLayout.CENTER);
		getContentPane().setBackground(gui.getColor("Window"));
		read(control.getGameState().getChanges());
		changed = false;

		/* Create the button panel */
		JPanel panel = new JPanel(new GridLayout(1, 3));
		getContentPane().add(panel, BorderLayout.SOUTH);

		/* Create the load button */
		JButton button = new JButton(get("load"));
		button.setFont(FONT16);
		if(control.isServer()) {
			button.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						load();
					}
				}
			);
		} else button.setEnabled(false);
		panel.add(button);

		/* Create the save button */
		button = new JButton(get("save"));
		button.setFont(FONT16);
		button.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					save();
				}
			}
		);
		panel.add(button);

		/* Create the apply button */
		button = new JButton(get("apply"));
		button.setFont(FONT16);
		if(control.isServer()) {
			button.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						apply();
					}
				}
			);
		} else button.setEnabled(false);
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

		/* Set window bounds. */
		setSize(gui.getSize("Window"));
		setLocationRelativeTo(null);
		setTitle(get("setTitle"));
		setVisible(true);
	}

	/** Loads settings from file */
	private void load() {
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new SetFileFilter());
		if(fc.showOpenDialog(SettingsWindow.this) == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			try {
				ObjectInputStream stream = new ObjectInputStream(new FileInputStream(file));
				read((Settings)stream.readObject());
				stream.close();
			} catch(Exception ex) {
				ex.printStackTrace();
				control.showError("LoadFailed");
			}
			control.showMessage(get("logLoadSettings"));
			changed = true;
		}
	}

	/**
	 *	Reads the given settings object and applies the changes.
	 *	@param set the settings object
	 */
	@SuppressWarnings("rawtypes")
	public void read(Settings set) {
		if(set == null) return;
		for(JCheckBox checkbox: specialCheckBoxes) checkbox.setSelected(set.specialAccepted.get(Buildings.valueOf(checkbox.getName())));
		for(JComboBox combobox: specialComboBoxes) combobox.setSelectedIndex(set.specialPositions.get(Buildings.valueOf(combobox.getName())));
		for(JCheckBox checkbox: supplyCheckBoxes) checkbox.setSelected(set.supplyVisible.get(Supply.valueOf(checkbox.getName())));
		for(JComboBox combobox: supplyComboBoxes) combobox.setSelectedIndex(set.supplyPositions.get(Supply.valueOf(combobox.getName())));
		for(JCheckBox checkbox: standardCheckBoxes) checkbox.setSelected(set.standardAccepted.get(Buildings.valueOf(checkbox.getName())));
		for(JComboBox combobox: standardComboBoxes) combobox.setSelectedIndex(set.standardPositions.get(Buildings.valueOf(combobox.getName())));
		standardOverride.setSelected(set.standardOverride);
		int value;
		for(JTextField textfield: goodsTextFields) {
			value = set.goodAmounts.get(Good.valueOf(textfield.getName()));
			textfield.setText(value < 0 ? "" : String.valueOf(value));
		}
		value = set.soloMarketCapacity;
		if(value < GameState.MARKET_MIN) value = GameState.MARKET_MIN;
		if(value > GameState.MARKET_MAX) value = GameState.MARKET_MAX;
		extraSoloComboBox.setSelectedItem(Integer.valueOf(value));
		extraLoansCheckBox.setSelected(set.advancedLoans);
		extraPointsCheckBox.setSelected(set.pointsVisible);
	}

	/** Saves and applies the changes. */
	private void save() {
		if(!changed && control.isServer()) {
			control.showWarning("NoChanges");
			return;
		}
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new SetFileFilter());
		if(fc.showSaveDialog(SettingsWindow.this) == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			if(!file.getName().endsWith(SetFileFilter.ACCEPTED_EXTENSION)) file = new File(file.getParentFile(), file.getName() + SetFileFilter.ACCEPTED_EXTENSION);
			try {
				ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(file));
				stream.writeObject(getSettings());
				stream.close();
			} catch(Exception ex) {
				ex.printStackTrace();
				control.showError("SaveFailed");
			}
			control.showMessage(get("logSaveSettings"));
		}
		apply();
	}

	/** Applies the changes and quits. */
	private void apply() {
		if(control.isServer()) control.setChanges(getSettings());
		changed = false;
		quit();
	}

	/**
	 *	Returns the settings object that mirrors the changes.
	 *	@return the settings object
	 */
	@SuppressWarnings("rawtypes")
	private Settings getSettings() {
		Settings set = new Settings();
		for(JCheckBox checkbox: specialCheckBoxes) set.specialAccepted.put(Buildings.valueOf(checkbox.getName()), checkbox.isSelected());
		for(JComboBox combobox: specialComboBoxes) set.specialPositions.put(Buildings.valueOf(combobox.getName()), combobox.getSelectedIndex());
		for(JCheckBox checkbox: supplyCheckBoxes) set.supplyVisible.put(Supply.valueOf(checkbox.getName()), checkbox.isSelected());
		for(JComboBox combobox: supplyComboBoxes) set.supplyPositions.put(Supply.valueOf(combobox.getName()), combobox.getSelectedIndex());
		for(JCheckBox checkbox: standardCheckBoxes) set.standardAccepted.put(Buildings.valueOf(checkbox.getName()), checkbox.isSelected());
		for(JComboBox combobox: standardComboBoxes) set.standardPositions.put(Buildings.valueOf(combobox.getName()), combobox.getSelectedIndex());
		set.standardOverride = standardOverride.isSelected();
		for(JTextField textfield: goodsTextFields) set.goodAmounts.put(Good.valueOf(textfield.getName()), Util.parseInt(textfield.getText()));
		int value = (Integer)extraSoloComboBox.getSelectedItem();
		if(value < GameState.MARKET_MIN) value = GameState.MARKET_MIN;
		if(value > GameState.MARKET_MAX) value = GameState.MARKET_MAX;
		set.soloMarketCapacity = value;
		set.advancedLoans = extraLoansCheckBox.isSelected();
		set.pointsVisible = extraPointsCheckBox.isSelected();
		return set;
	}

	/**
	 *	Creates the basic components of each tab.
	 *	@param tab the tabbed pane
	 *	@param type the content type
	 *	@return the content panel
	 */
	private JPanel createTab(JTabbedPane tab, String type) {
		/* Create the bounding panel */
		JPanel panel = new JPanel(new BorderLayout());
		Insets in = gui.getPadding("Tab");
		panel.setBorder(BorderFactory.createEmptyBorder(in.top, in.left, in.bottom, in.right));
		panel.setBackground(gui.getColor("Tab"));
		panel.setOpaque(true);

		/* Create the description label */
		JEditorPane descr = new JEditorPane();
		descr.setContentType("text/html");
		descr.setText(get(String.format("set%sDescr", type)));
		descr.setPreferredSize(gui.getSize("LargeDescr"));
		descr.setEditable(false);
		descr.setOpaque(false);
		panel.add(descr, BorderLayout.NORTH);

		/* Create the scroll pane */
		JScrollPane scroll = new JScrollPane(panel);
		scroll.getVerticalScrollBar().setUnitIncrement(gui.getInt("ScrollUnit"));
		tab.addTab(get(String.format("set%sTitle", type)), scroll);
		return panel;
	}

	/**
	 *	Creates the tab for the special buildings settings.
	 *	@param tab the tabbed pane
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void createSpecialTab(JTabbedPane tab) {
		JPanel spec = createTab(tab, "Spec");

		/* Create the inner panel */
		ArrayList<Buildings> specials = new ArrayList<Buildings>();
		for(Buildings building: Buildings.values()) if(building.isSpecial()) specials.add(building);
		JPanel panel = new JPanel(new GridLayout(0, 3));
		panel.setOpaque(false);
		spec.add(panel, BorderLayout.CENTER);

		/* The 'Basegame' checkbox */
		JPanel line = new JPanel();
		line.setOpaque(false);
		panel.add(line);
		JCheckBox checkbox = new JCheckBox(get("setSpecBase"), true);
		checkbox.setOpaque(false);
		if(control.isServer()) {
			checkbox.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						boolean checked = ((JCheckBox)e.getSource()).isSelected();
						String name;
						for(JCheckBox checkbox: specialCheckBoxes) {
							name = checkbox.getName().substring(1);
							if(!name.equals("_31")) {
								if(!name.startsWith("0")) continue;
								int index = Integer.parseInt(name);
								if(index < 1 || index > 35) continue;
							}
							checkbox.setSelected(checked);
						}
						changed = true;
					}
				}
			);
		} else checkbox.setEnabled(false);
		line.add(checkbox);

		/* The 'Essen 2008' checkbox */
		line = new JPanel();
		line.setOpaque(false);
		panel.add(line);
		checkbox = new JCheckBox(get("setSpec2008"), true);
		checkbox.setOpaque(false);
		if(control.isServer()) {
			checkbox.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						boolean checked = ((JCheckBox)e.getSource()).isSelected();
						String name;
						for(JCheckBox checkbox: specialCheckBoxes) {
							name = checkbox.getName().substring(1);
							if(!name.equals("_00")) {
								if(!name.startsWith("0")) continue;
								int index = Integer.parseInt(name);
								if(index < 36 || index > 46) continue;
							}
							checkbox.setSelected(checked);
						}
						changed = true;
					}
				}
			);
		} else checkbox.setEnabled(false);
		line.add(checkbox);

		/* The 'Le Grande Hameau' checkbox */
		line = new JPanel();
		line.setOpaque(false);
		panel.add(line);
		checkbox = new JCheckBox(get("setSpecLGH"), true);
		checkbox.setOpaque(false);
		if(control.isServer()) {
			checkbox.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						boolean checked = ((JCheckBox)e.getSource()).isSelected();
						String name;
						for(JCheckBox checkbox: specialCheckBoxes) {
							name = checkbox.getName();
							if(!name.startsWith("$GH")) continue;
							checkbox.setSelected(checked);
						}
						changed = true;
					}
				}
			);
		} else checkbox.setEnabled(false);
		line.add(checkbox);

		/* Create the options */
		panel = new JPanel(new GridLayout(0, 2, HGAP, VGAP));
		panel.setOpaque(false);
		spec.add(panel, BorderLayout.SOUTH);
		int hgap = gui.getInt("EntryHGap");
		int vgap = gui.getInt("EntryVGap");
		String name;
		JLabel label;
		JComboBox list;
		String checkText = Util.format(get("setSpecCheck"));
		String listText = Util.format(get("setSpecList"));
		String[] positions = {"?", "1.", "2.", "3.", "4.", "5.", "6.", get("start")};
		for(Buildings building: specials) {
			line = new JPanel(new BorderLayout(hgap, vgap));
			line.setOpaque(false);
			panel.add(line);

			/* Create the checkbox */
			name = String.format("$%s", building);
			checkbox = new JCheckBox(building.toString(), true);
			checkbox.setName(name);
			checkbox.setToolTipText(checkText);
			checkbox.setOpaque(false);
			if(control.isServer()) {
				checkbox.addActionListener(
					new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							changed = true;
						}
					}
				);
			} else checkbox.setEnabled(false);
			specialCheckBoxes.add(checkbox);
			line.add(checkbox, BorderLayout.WEST);

			/* Create the name label */
			label = new JLabel(get("building" + building));
			label.setFont(SFONT18);
			label.setToolTipText(fitTooltip(Util.getToolTipText(control.getDictionary(), building), TOOLTIP_MAX_WIDTH));
			line.add(label, BorderLayout.CENTER);

			/* Create the positions list */
			list = new JComboBox(positions);
			list.setName(name);
			list.setToolTipText(listText);
			list.setOpaque(false);
			if(control.isServer()) {
				list.addActionListener(
					new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							changed = true;
						}
					}
				);
			} else list.setEnabled(false);
			specialComboBoxes.add(list);
			line.add(list, BorderLayout.EAST);
		}
	}

	/**
	 *	Creates the tab for the supply settings.
	 *	@param tab the tabbed pane
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void createSupplyTab(JTabbedPane tab) {
		JPanel sup = createTab(tab, "Sup");

		/* Create the inner panel */
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		sup.add(panel, BorderLayout.CENTER);
		JPanel upper = new JPanel();
		upper.setOpaque(false);
		panel.add(upper, BorderLayout.NORTH);
		JPanel lower = new JPanel();
		lower.setOpaque(false);
		panel.add(lower, BorderLayout.SOUTH);

		/* Create the options */
		int hgap = gui.getInt("EntryHGap");
		int vgap = gui.getInt("EntryVGap");
		final String[] positions = {"?", "1.", "2.", "3.", "4.", "5.", "6.", "7."};
		JPanel item, line;
		JLabel label;
		String name;
		JCheckBox checkbox;
		JComboBox list;
		int i = 0;
		String checkText = Util.format(get("setSupCheck"));
		String listText = Util.format(get("setSupList"));
		String language = control.getDictionary().getLanguage();
		for(Supply supply: Supply.values()) {
			item = new JPanel(new BorderLayout(hgap, vgap));
			item.setOpaque(false);
			(i++ < 4 ? upper : lower).add(item);

			/* Create the icon */
			label = new ImageLabel(control.network,language, String.format(MainWindow.SUPPLY_PATH, supply + "s"), false);
			label.setToolTipText(Util.getToolTipText(control.getDictionary(), supply));
			line = new JPanel(new FlowLayout(FlowLayout.CENTER));
			line.add(label);
			item.add(line, BorderLayout.CENTER);

			/* Create the bottom panel */
			line = new JPanel();
			line.setOpaque(false);
			item.add(line, BorderLayout.SOUTH);

			/* Create the checkbox */
			name = String.format("$%d", supply.ordinal() + 1);
			checkbox = new JCheckBox(get("setSupOpen"), false);
			checkbox.setName(name);
			checkbox.setToolTipText(checkText);
			checkbox.setOpaque(false);
			if(control.isServer()) {
				checkbox.addActionListener(
					new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							changed = true;
						}
					}
				);
			} else checkbox.setEnabled(false);
			supplyCheckBoxes.add(checkbox);
			line.add(checkbox);

			/* Create the positions list */
			list = new JComboBox(positions);
			list.setName(name);
			list.setToolTipText(listText);
			list.setOpaque(false);
			if(control.isServer()) {
				list.addActionListener(
					new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							changed = true;
						}
					}
				);
			} else list.setEnabled(false);
			supplyComboBoxes.add(list);
			line.add(list);
		}

		/* The 'all' checkbox */
		checkbox = new JCheckBox(get("all"), false);
		checkbox.setOpaque(false);
		if(control.isServer()) {
			checkbox.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						boolean checked = ((JCheckBox)e.getSource()).isSelected();
						for(JCheckBox checkbox: supplyCheckBoxes) checkbox.setSelected(checked);
						changed = true;
					}
				}
			);
		} else checkbox.setEnabled(false);
		lower.add(checkbox);
	}

	/**
	 *	Creates the tab for the standard buildings settings.
	 *	@param tab the tabbed pane
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void createStandardTab(JTabbedPane tab) {
		JPanel std = createTab(tab, "Std");

		/* Create the inner panel */
		ArrayList<Buildings> standards= new ArrayList<Buildings>();
		for(Buildings building: Buildings.values()) if(building.isStandard()) standards.add(building);
		int n = standards.size();
		boolean odd = (n % 2 != 0);
		JPanel panel = new JPanel(new GridLayout((odd ? n + 1 : n) / 2 + 2, 2, HGAP, VGAP));
		panel.setOpaque(false);
		std.add(panel, BorderLayout.CENTER);

		/* Create the options */
		int hgap = gui.getInt("EntryHGap");
		int vgap = gui.getInt("EntryVGap");
		String[] positions = {"?", "1.", "2.", "3.", get("start")};
		JPanel line;
		String name;
		JCheckBox checkbox;
		JLabel label;
		JComboBox list;
		String checkText = Util.format(get("setStdCheck"));
		String listText = Util.format(get("setStdList"));
		for(Buildings building: standards) {
			line = new JPanel(new BorderLayout(hgap, vgap));
			line.setOpaque(false);
			panel.add(line);

			/* Create the checkbox */
			name = String.format("$%s", building);
			checkbox = new JCheckBox(building.toString(), true);
			checkbox.setName(name);
			checkbox.setOpaque(false);
			checkbox.setToolTipText(checkText);
			if(control.isServer()) {
				checkbox.addActionListener(
					new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							changed = true;
						}
					}
				);
			} else checkbox.setEnabled(false);
			standardCheckBoxes.add(checkbox);
			line.add(checkbox, BorderLayout.WEST);

			/* Create the name label */
			label = new JLabel(get("building" + building));
			label.setFont(SFONT18);
			label.setToolTipText(fitTooltip(Util.getToolTipText(control.getDictionary(), building), TOOLTIP_MAX_WIDTH));
			line.add(label, BorderLayout.CENTER);

			/* Create the positions list */
			list = new JComboBox(positions);
			list.setName(name);
			list.setToolTipText(listText);
			list.setOpaque(false);
			if(control.isServer()) {
				list.addActionListener(
					new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							changed = true;
						}
					}
				);
			} else list.setEnabled(false);
			standardComboBoxes.add(list);
			line.add(list, BorderLayout.EAST);
		}

		/* The 'all' button */
		if(odd) panel.add(new JLabel());
		line = new JPanel(new BorderLayout(hgap, vgap));
		line.setOpaque(false);
		panel.add(line);
		checkbox = new JCheckBox(get("all"), true);
		checkbox.setOpaque(false);
		if(control.isServer()) {
			checkbox.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						boolean checked = ((JCheckBox)e.getSource()).isSelected();
						for(JCheckBox checkbox: standardCheckBoxes) checkbox.setSelected(checked);
						changed = true;
					}
				}
			);
		} else checkbox.setEnabled(false);
		line.add(checkbox, BorderLayout.WEST);

		/* The override button */
		line = new JPanel(new BorderLayout(hgap, vgap));
		line.setOpaque(false);
		panel.add(line);
		standardOverride.setText(get("override"));
		standardOverride.setSelected(false);
		standardOverride.setOpaque(false);
		standardOverride.setToolTipText(Util.format(get("setStdOverride")));
		if(control.isServer()) {
			standardOverride.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						changed = true;
					}
				}
			);
		} else standardOverride.setEnabled(false);
		line.add(standardOverride, BorderLayout.WEST);
	}

	/**
	 *	Creates the tab for the goods at start settings.
	 *	@param tab the tabbed pane
	 */
	private void createGoodsTab(JTabbedPane tab) {
		JPanel sup = createTab(tab, "Goods");

		/* Create the inner panel */
		JPanel panel = new JPanel(new GridLayout(2, 4, HGAP, VGAP));
		panel.setOpaque(false);
		sup.add(panel, BorderLayout.CENTER);

		/* Create the options */
		Good[] goods = Setup.getOfferedGoods();
		int hgap = gui.getInt("EntryHGap");
		int vgap = gui.getInt("EntryVGap");
		JPanel item;
		JLabel label;
		int i = 0;
		String language = control.getDictionary().getLanguage();
		for(Good good: goods) {
			if(i++ == 4) panel.add(new JLabel());
			item = new JPanel(new BorderLayout(hgap, vgap));
			item.setOpaque(false);
			panel.add(item);
			label = new ImageLabel(control.network,language, String.format(MainWindow.GOODS_PATH, good), false);
			label = new JLabel(label.getIcon());
			item.add(label, BorderLayout.CENTER);
			final JTextField input = new JTextField();
			input.setName(good.toString());
			input.setToolTipText(get("popupInput"));
			input.setHorizontalAlignment(JTextField.CENTER);
			if(control.isServer()) {
				input.addCaretListener(
					new CaretListener() {
						public void caretUpdate(CaretEvent e) {
							if(!input.getText().trim().equals("")) changed = true;
						}
					}
				);
			} else input.setEditable(false);
			goodsTextFields.add(input);
			item.add(input, BorderLayout.SOUTH);
			label.addMouseListener(DialogWindow.createIconListener(input, 0));
		}
	}

	/**
	 *	Creates the tab for the extra game settings.
	 *	@param tab the tabbed pane
	 */
	@SuppressWarnings("unchecked")
	private void createExtrasTab(JTabbedPane tab) {
		JPanel extra = createTab(tab, "Extra");
		extra.setLayout(new GridLayout(6, 1));
		Dimension size = gui.getSize("SmallDescr");

		/* Create the solo game component */
		JEditorPane descr = (JEditorPane)extra.getComponent(0);
		descr.setPreferredSize(size);
		JPanel inner = new JPanel(new FlowLayout(FlowLayout.CENTER));
		inner.setOpaque(false);
		extra.add(inner);

		/* Fill the combo box */
		Integer item;
		for(int i = GameState.MARKET_MIN; i <= GameState.MARKET_MAX; i++) {
			item = Integer.valueOf(i);
			extraSoloComboBox.addItem(item);
			if(i == GameState.MARKET_SOLO) extraSoloComboBox.setSelectedItem(item);
		}
		extraSoloComboBox.setPreferredSize(gui.getSize("Extra"));
		extraSoloComboBox.setToolTipText(get("setExtraList"));
		extraSoloComboBox.setFont(FONT16);
		extraSoloComboBox.setOpaque(false);
		if(control.isServer()) {
			extraSoloComboBox.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						changed = true;
					}
				}
			);
		} else extraSoloComboBox.setEnabled(false);
		inner.add(extraSoloComboBox);

		/* Create the loan components */
		descr = new JEditorPane();
		descr.setContentType("text/html");
		descr.setText(get("setLoansDescr"));
		descr.setPreferredSize(size);
		descr.setEditable(false);
		descr.setOpaque(false);
		extra.add(descr);
		inner = new JPanel(new FlowLayout(FlowLayout.CENTER));
		inner.setOpaque(false);
		extra.add(inner);
		extraLoansCheckBox.setText(get("setLoansOption"));
		extraLoansCheckBox.setSelected(false);
		extraLoansCheckBox.setOpaque(false);
		if(control.isServer()) {
			extraLoansCheckBox.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						//boolean checked = ((JCheckBox)e.getSource()).isSelected();
						changed = true;
					}
				}
			);
		} else extraLoansCheckBox.setEnabled(false);
		inner.add(extraLoansCheckBox);

		/* Create the points components */
		descr = new JEditorPane();
		descr.setContentType("text/html");
		descr.setText(get("setPointsDescr"));
		descr.setPreferredSize(size);
		descr.setEditable(false);
		descr.setOpaque(false);
		extra.add(descr);
		inner = new JPanel(new FlowLayout(FlowLayout.CENTER));
		inner.setOpaque(false);
		extra.add(inner);
		extraPointsCheckBox.setText(get("setPointsOption"));
		extraPointsCheckBox.setSelected(false);
		extraPointsCheckBox.setOpaque(false);
		if(control.isServer()) {
			extraPointsCheckBox.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						//boolean checked = ((JCheckBox)e.getSource()).isSelected();
						changed = true;
					}
				}
			);
		} else extraPointsCheckBox.setEnabled(false);
		inner.add(extraPointsCheckBox);
	}

	/**
	 *	Fits the given HTML formatted tooltip message into a {@code <div>}
	 *	with the given width.
	 *	@param message the tooltip message
	 *	@param width the width to apply
	 */
	private String fitTooltip(String message, int width) {
		return message.replace("<html>", String.format("<html><div width=\"%d\">", width)).replace("</html>", "</div></html>");
	}

	/** Closes the window. */
	public void quit() {
		if(changed && control.isServer() && confirm("SaveChanges")) save();
		setVisible(false);
		dispose();
	}
}