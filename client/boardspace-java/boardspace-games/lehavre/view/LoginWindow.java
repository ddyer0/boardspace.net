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

import javax.swing.*;

import lehavre.main.*;
import lehavre.model.GameState;
import lehavre.util.*;
import lib.G;

/**
 *
 *	The <code>LoginWindow</code> class is the window you see when you start the
 *	game. It allows you to host a new game or join an exisiting one. Provide the
 *	name of the game to create/join and your username to use during the game.
 *
 *	Thanks to JGroups (http://www.jgroups.org/javagroupsnew/docs/index.html)
 *	all participants will be found automatically and joined in the correct games.
 *	This way you can even run multiple games simultaneously.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/7
 */
public final class LoginWindow
extends GameWindow
{
	static final long serialVersionUID =1L;
	/** The path to the login data file. */
	private static final String LOGIN_PATH = "lehavre/config/login.txt";

	/** The input fields. */
	private final JTextField gameField, addrField, nameField;

	/** The game state to load. */
	private GameState state = null;

	/**
	 *	Creates a new <code>LoginWindow</code> instance and
	 *	enters the given default values for the text fields.
	 *	@param game the name of the game
	 *	@param address the IP address
	 *	@param name the name of the player
	 *	@param control the control object
	 */
	public LoginWindow(final LeHavre control, String game, String address, String name) {
		super(control, "login");

		/* Create the language selection menu */
		setJMenuBar(new JMenuBar());
		String title = get("menuLanguage");
		JMenu menu = new JMenu(title);
		menu.setMnemonic(title.charAt(0));
		getJMenuBar().add(menu);

		/* Create the language menu items */
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".txt");
			}
		};
		String[] languages = new File(Dictionary.LANG_PATH).list(filter);
		final String LANGUAGE = control.getDictionary().getLanguage();
		JMenuItem item;
		for(String language: languages) {
			language = language.substring(0, language.indexOf("."));
			title = get(language);
			if(language.equals(LANGUAGE)) title = String.format("%s \u2713", title);
			item = new JMenuItem(title);
			item.setMnemonic(title.charAt(0));
			item.setName(language);
			item.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String language = ((JMenuItem)e.getSource()).getName();
						boolean changed = false;
						try {
							changed = control.getDictionary().load(language,control.network);
						} catch(IOException ex) {
							ex.printStackTrace();
							control.showError("LangChange");
						}
						if(changed) {
							setVisible(false);
							dispose();
							log(String.format(get("logLanguage"), get(language)));
							new LoginWindow(control, getGame(), getAddress(), getPlayer());
						}
					}
				}
			);
			menu.add(item);
		}

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
					showInstructions("loginInstructions", ((JMenuItem)e.getSource()).getText());
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
		Dimension gap = gui.getSize("Gap");
		JPanel contents = new JPanel(new BorderLayout(G.Width(gap), G.Height(gap)));
		contents.setBackground(gui.getColor("Window"));
		Insets in = gui.getPadding("Window");
		contents.setBorder(BorderFactory.createEmptyBorder(in.top, in.left, in.bottom, in.right));
		getContentPane().add(contents);

		/* Get the latest login data */
		boolean noGame = (game == null);
		boolean noAddress = (address == null);
		boolean noName = (name == null);
		if(noGame || noAddress || noName) {
			String savedGame = "";
			String savedAddress = "";
			String savedName = "";
			try {
				BufferedReader reader = new BufferedReader(new FileReader(LOGIN_PATH));
				String line = reader.readLine();
				if(line!=null)
				{
				savedGame = line.trim();
				line = reader.readLine();
				if(line!=null)
				{
				savedAddress = line.trim();
				line = reader.readLine();
				if(line!=null) { savedName = line.trim(); }
				}
				}
				reader.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
			if(noGame) game = savedGame;
			if(noAddress) address = savedAddress;
			if(noName) name = savedName;
		}

		/* Create the logo */
		JLabel label = new ImageLabel(control.network,null, String.format(MainWindow.SYMBOL_PATH, "logo1"), false);
		label.setBackground(gui.getColor("Logo"));
		label.setOpaque(true);
		in = gui.getPadding("Logo");
		label.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createRaisedBevelBorder(),
			BorderFactory.createEmptyBorder(in.top, in.left, in.bottom, in.right)
		));
		contents.add(label, BorderLayout.NORTH);

		/* Create the login area */
		JPanel login = new JPanel(new BorderLayout(gap.width, gap.height));
		login.setBackground(gui.getColor("Login"));
		in = gui.getPadding("Login");
		login.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createRaisedBevelBorder(),
			BorderFactory.createEmptyBorder(in.top, in.left, in.bottom, in.right)
		));
		contents.add(login, BorderLayout.SOUTH);

		/* Create the login panel */
		JPanel panel = new JPanel(new GridLayout(3, 2, gap.width, gap.height));
		panel.setOpaque(false);
		login.add(panel, BorderLayout.CENTER);
		Font font = G.getFont("SansSerif", G.Style.Plain, new GUIHelper("font").getInt("LoginLabels"));
		Font bold = G.getFont(font,G.Style.Bold,0);
		Color text = gui.getColor("Text");

		/* Create the game part */
		label = new JLabel(get("loginGame"));
		label.setForeground(text);
		label.setFont(bold);
		panel.add(label);
		gameField = new JTextField();
		gameField.setText(game);
		gameField.setFont(font);
		gameField.setOpaque(true);
		panel.add(gameField);

		/* Create the address part */
		label = new JLabel(get("loginAddr"));
		label.setForeground(text);
		label.setFont(bold);
		panel.add(label);
		addrField = new JTextField();
		addrField.setText(address);
		addrField.setFont(font);
		addrField.setOpaque(true);
		panel.add(addrField);

		/* Create the name part */
		label = new JLabel(get("loginName"));
		label.setForeground(text);
		label.setFont(bold);
		panel.add(label);
		nameField = new JTextField();
		nameField.setText(name);
		nameField.setFont(font);
		nameField.setOpaque(true);
		panel.add(nameField);

		/* Create the buttons panel */
		panel = new JPanel(new GridLayout(1, 2, gap.width, gap.height));
		panel.setOpaque(false);
		login.add(panel, BorderLayout.SOUTH);

		/* Create the load button */
		JButton button = new JButton(get("loginLoad"));
		button.setFont(FONT16);
		button.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JFileChooser fc = new JFileChooser();
					fc.setFileFilter(new SavFileFilter());
					if(fc.showOpenDialog(LoginWindow.this) == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						try {
							ObjectInputStream stream = new ObjectInputStream(new FileInputStream(file));
							state = (GameState)stream.readObject();
							stream.close();
							login();
						} catch(Exception ex) {
							ex.printStackTrace();
							control.showError("LoadFailed");
						}
						((JButton)e.getSource()).setForeground(Color.red);
					}
				}
			}
		);
		panel.add(button);

		/* Create the login button */
		button = new JButton(get("loginJoin"));
		button.setFont(FONT16);
		button.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					login();
				}
			}
		);
		panel.add(button);

		/* Set window bounds. */
		pack();
		setLocationRelativeTo(null);
		setResizable(false);
		setVisible(true);
	}

	/**
	 *	Creates a new <code>LoginWindow</code> instance.
	 *	@param control the control object
	 */
	public LoginWindow(LeHavre control) {
		this(control, null, null, null);
	}

	/**
	 *	Returns the contents of the given field.
	 *	@param field the field
	 *	@return the contents of the given field
	 */
	private String getContent(JTextField field) {
		return field.getText().trim();
	}

	/**
	 *	Returns the entered game name.
	 *	@return the entered game name
	 */
	private String getGame() {
		return getContent(gameField);
	}

	/**
	 *	Returns the entered player name.
	 *	@return the entered player name
	 */
	private String getPlayer() {
		return getContent(nameField);
	}

	/**
	 *	Returns the entered IP address.
	 *	@return the entered IP address
	 */
	private String getAddress() {
		return getContent(addrField);
	}

	/**
	 *	Performs the login.
	 */
	private void login() {
		String game = getGame();
		String addr = getAddress();
		String name = getPlayer();
		if(!(game + name).matches("^(\\w| |-)+$")) {
			control.showError("IllegalChars");
			return;
		}
		if(Math.max(game.length(), name.length()) > 20) {
			control.showError("NameTooLong");
			return;
		}
		if(addr.length() > 0) {
			String[] parts = addr.split("\\.");
			int approved = 0, value;
			for(String part: parts) {
				value = Util.parseInt(part);
				if(value >= 0 && value <= 255) approved++;
			}
			if(approved != 4) {
				control.showError("BadAddress");
				return;
			}
		}
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(LOGIN_PATH));
			writer.write(game);
			writer.write("\r\n");
			writer.write(addr);
			writer.write("\r\n");
			writer.write(name);
			writer.close();
			log(String.format(get("logLoginDataSaved"), game, name));
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		setVisible(false);
		dispose();
		control.login(game, addr, name, state);
	}
}