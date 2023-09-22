/* copyright notice */package lehavre.view;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import lehavre.main.*;
import lehavre.model.*;
import lehavre.util.Util;

/**
 *
 *	The <code>LobbyWindow</code> class is the window you see after you've logged
 *	in to the game. You can choose your player colors and set the game options.
 *	If all players agreed upon the settings, the game will start.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/7
 */
public final class LobbyWindow
extends GameWindow
{
	static final long serialVersionUID =1L;
	/** The available indices. '?' represents a random index. */
	private static final String[] indices = {"?", "1.", "2.", "3.", "4.", "5."};

	/** The available player colors. */
	private final Color[] colors;

	/** The names of the available colors. */
	private final String[] names;

	/** The login panel. */
	private JPanel loginPanel;

	/** The player panels. */
	private ArrayList<JPanel> playerPanels = new ArrayList<JPanel>();

	/** The chat menu item. */
	private JMenuItem chatMenuItem;

	/**
	 *	Creates a new <code>LobbyWindow</code> instance.
	 *	@param control the control object
	 */
	public LobbyWindow(LeHavre control) {
		super(control, "lobby");
		setJMenuBar(new JMenuBar());

		/* Fill the constants. */
		int n = PlayerColor.values().length;
		colors = new Color[n];
		names = new String[n];
		for(PlayerColor color: PlayerColor.values()) {
			int i = color.ordinal();
			colors[i] = new Color(color.getRed(), color.getGreen(), color.getBlue());
			names[i] = get("color" + color);
		}

		/* Create the game menu */
		String title = get("menuGame");
		JMenu menu = new JMenu(title);
		menu.setMnemonic(title.charAt(0));
		getJMenuBar().add(menu);

		/* Menu item: open the chat window */
		title = get("itemOpenChat");
		chatMenuItem = new JMenuItem(title);
		chatMenuItem.setMnemonic(title.charAt(0));
		chatMenuItem.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					LobbyWindow.this.control.openChat();
					chatMenuItem.setEnabled(false);
				}
			}
		);
		menu.add(chatMenuItem);
		if(control.isChatOpen()) chatMenuItem.setEnabled(false);

		/* Menu item: game settings */
		title = get("itemSettings");
		JMenuItem item = new JMenuItem(title);
		item.setMnemonic(title.charAt(0));
		item.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					new SettingsWindow(LobbyWindow.this.control);
				}
			}
		);
		menu.add(item);

		/* Create the help menu */
		title = get("menuHelp");
		menu = new JMenu(title);
		menu.setMnemonic(title.charAt(0));
		getJMenuBar().add(menu);

		/* Menu item: instructions */
		title = get("itemInstructions");
		item = new JMenuItem(title);
		item.setMnemonic(title.charAt(0));
		item.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					showInstructions("lobbyInstructions", ((JMenuItem)e.getSource()).getText());
				}
			}
		);
		menu.add(item);

		/* Menu item: about */
		title = get("itemAbout");
		item = new JMenuItem(title);
		item.setMnemonic(title.charAt(0));
		item.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					showInstructions("about", ((JMenuItem)e.getSource()).getText());
				}
			}
		);
		menu.add(item);

		/* Create the contents panel */
		JPanel contents = new JPanel(new BorderLayout());
		contents.setBackground(gui.getColor("Window"));
		Insets in = gui.getPadding("Window");
		contents.setBorder(BorderFactory.createEmptyBorder(in.top, in.left, in.bottom, in.right));
		getContentPane().add(contents);

		/* Create the login panel */
		contents.add(new ImageLabel(control.network,null, String.format(MainWindow.SYMBOL_PATH, "logo2"), false), BorderLayout.NORTH);
		loginPanel = new JPanel(null);
		loginPanel.setOpaque(false);
		loginPanel.setBorder(BorderFactory.createTitledBorder(
			BorderFactory.createEtchedBorder(),
			get("lobbyLoginArea"),
			TitledBorder.DEFAULT_JUSTIFICATION,
			TitledBorder.DEFAULT_POSITION,
			FONT16,
			gui.getColor("LoginTitle")
		));
		contents.add(loginPanel, BorderLayout.CENTER);

		/* Create the button panels */
		JPanel panel = new JPanel();
		panel.setOpaque(false);
		contents.add(panel, BorderLayout.SOUTH);
		if(control.isServer()) {
			/* Create the long game button */
			JButton button = new JButton(get("lobbyStartLong"));
			button.setFont(FONT16);
			button.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						LobbyWindow.this.control.start((JButton)e.getSource(), GameType.LONG);
					}
				}
			);
			panel.add(button);

			/* Create the short game button */
			button = new JButton(get("lobbyStartShort"));
			button.setFont(FONT16);
			button.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						LobbyWindow.this.control.start((JButton)e.getSource(), GameType.SHORT);
					}
				}
			);
			panel.add(button);
		} else {
			/* Create the start button */
			JButton button = new JButton(get("lobbyStart"));
			button.setFont(FONT16);
			button.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						LobbyWindow.this.control.setReady((JButton)e.getSource());
					}
				}
			);
			panel.add(button);
		}

		/* Set window bounds. */
		setSize(gui.getSize("Window"));
		setLocationRelativeTo(null);
	}

	/**
	 *	Called when the chat window has changed.
	 */
	public void chatChanged() {
		chatMenuItem.setEnabled(!control.isChatOpen());
	}

	/**
	 *	Creates a new player panel for the given player.
	 *	@param player the player
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void showPlayer(final Player player) {
		int index = player.getIndex();
		player.setColor(PlayerColor.values()[index]);
		JPanel playerPanel = new JPanel(null);
		Point location = gui.getOffset("Player");
		int dy = gui.getInt("PlayerDY") * index;
		location.translate(0, dy);
		playerPanel.setLocation(location);
		playerPanel.setSize(gui.getSize("Player"));
		playerPanel.setBackground(gui.getColor("Player"));
		playerPanel.setOpaque(true);
		playerPanel.setBorder(BorderFactory.createRaisedBevelBorder());
		playerPanels.add(playerPanel);
		loginPanel.add(playerPanel);

		/* Create the index dropdown list. */
		String format = get("lobbyFormat");
		JComboBox list = new JComboBox(indices);
		list.setSelectedIndex(0);
		list.setEditable(false);
		if(control.isServer()) {
			list.setToolTipText(Util.format(get("lobbyIndexInfo")));
			list.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						control.setSeat(player.getIndex(), ((JComboBox)e.getSource()).getSelectedIndex());
					}
				}
			);
		} else {
			list.setToolTipText(String.format(format, get("lobbyIndex")));
			list.setEnabled(false);
		}
		list.setBounds(gui.getBounds("List"));
		list.setFont(FONT16);
		list.setOpaque(true);
		playerPanel.add(list, 0);

		/* Create the name label. */
		JLabel label = new JLabel(player.getName());
		label.setFont(SFONT24);
		label.setForeground(Color.BLACK);
		label.setBounds(gui.getBounds("Name"));
		playerPanel.add(label);

		/* Create the color label. */
		label = new JLabel();
		label.setBounds(gui.getBounds("Color"));
		label.setBackground(colors[index]);
		label.setName(names[index]);
		label.setOpaque(true);
		label.setBorder(BorderFactory.createLoweredBevelBorder());
		if(control.isServer()) {
			label.setToolTipText(Util.format(get("lobbyColorInfo")));
			label.addMouseListener(
				new MouseAdapter() {
					public void mouseClicked(MouseEvent e) {
						if(e.getClickCount() <= 1) return;
						JLabel self = (JLabel)e.getSource();
						String color = self.getName();
						String choice = (String)JOptionPane.showInputDialog(
							LobbyWindow.this,
							get("lobbyChoiceDescr"),
							get("lobbyChoiceTitle"),
							JOptionPane.QUESTION_MESSAGE,
							null,
							names,
							names[0]
						);
						if(choice == null || choice.equals(color)) return;
						int index = 0;
						while(index < names.length) {
							if(choice.equals(names[index])) break;
							index++;
						}
						control.setColor(player.getIndex(), index);
					}
				}
			);
		} else label.setToolTipText(String.format(format, get("lobbyColor")));
		playerPanel.add(label);

		/* Create the settings button */
		JButton button = new JButton(get("lobbyBonus"));
		button.setFont(FONT16);
		if(control.isServer()) {
			button.setToolTipText(Util.format(get("lobbyBonusInfo")));
			button.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						new BonusWindow(LobbyWindow.this.control, player);
					}
				}
			);
		} else button.setEnabled(false);
		location = gui.getOffset("Bonus");
		location.translate(0, dy);
		button.setLocation(location);
		button.setSize(gui.getSize("Bonus"));
		loginPanel.add(button);
		loginPanel.setVisible(false);
		loginPanel.setVisible(true);
	}

	/**
	 *	Sets the color for the player with the given index.
	 *	The color is parametrized by the enum ordinal value.
	 *	@param index the index
	 *	@param color the color
	 */
	public void setColor(int index, int color) {
		JLabel label = (JLabel)playerPanels.get(index).getComponent(2);
		label.setBackground(colors[color]);
		label.setName(names[index]);
	}

	/**
	 *	Sets the seat for the player with the given index.
	 *	@param index the index
	 *	@param seat the seat
	 */
	public void setSeat(int index, int seat) {
		@SuppressWarnings("rawtypes")
		JComboBox list = (JComboBox)playerPanels.get(index).getComponent(0);
		list.setEnabled(true);
		list.setSelectedIndex(seat);
		list.setEnabled(false);
	}
}