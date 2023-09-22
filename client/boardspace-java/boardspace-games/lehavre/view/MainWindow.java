/* copyright notice */package lehavre.view;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;

import online.common.exCanvas;
import lehavre.main.*;
import lehavre.main.Dictionary;
import lehavre.model.*;
import lehavre.model.buildings.*;
import lehavre.model.buildings.special.*;
import lehavre.util.*;
import lehavre.view.labels.*;
import lehavre.view.menus.*;
import lib.Graphics;
import lib.G;
import lib.HitPoint;
import lib.MouseState;
import lib.SimpleObservable;
import lib.SimpleObserver;

/**
 *
 *	The <code>MainWindow</code> class shows the board and components,
 *	thus the game itself. This is where the players will do their stuff.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.2 2010/4/25
 */
public final class MainWindow
extends exCanvas
{
	LeHavre control;
	static private Font FONT14,FONT15,FONT16,FONT20;
	static GUIHelper gui;
	static {
		/* Change the look and feel. */
		try {
			UIManager.setLookAndFeel(new MetalLookAndFeel());
		} catch(Exception e) {
			e.printStackTrace();
		}

		/* Load the 1st special font. */
		Font font = GUIHelper.getBoldFont();

		Font boldFont = (font != null ? font : G.getFont("SansSerif", G.Style.Plain, 1));
		gui = new GUIHelper("font");
		FONT14 = boldFont.deriveFont((float)gui.getInt("SmallButtons"));
		FONT15 = boldFont.deriveFont((float)gui.getInt("GoodLabels"));
		FONT16 = boldFont.deriveFont((float)gui.getInt("LargeButtons"));
		FONT20 = boldFont.deriveFont((float)gui.getInt("SymbolLabels"));
		gui = new GUIHelper("main");
	}
	protected void showInstructions(String name, String title) {
		JFrame win = new InstructionsWindow(get(name));
		win.setTitle(title);
		win.setVisible(true);
	}
	public JFrame getFrame()
	{	Component c = this;
		while((c!=null) && ! (c instanceof JFrame)) { c = c.getParent(); }
		return((JFrame)c);
	}
	public void setTitle(String str)
	{	JFrame myFrame = getFrame();
		if(myFrame!=null) { myFrame.setTitle(str); }
	}
	public void dispose() 
	{	JFrame myFrame = getFrame();
		if(myFrame!=null) { myFrame.dispose(); }
	}
	private Container getContentPane()
	{ JFrame myFrame = getFrame();
	  if(myFrame!=null) 
		{ return(myFrame.getContentPane());
		}
	return(null); 
	}

	private void log(String message) {
		control.log(message);
	}
	private void log(String message, boolean format) {
		control.log(message, format);
	}

	private void setLocationRelativeTo(Component cc)
	{	JFrame myFrame = getFrame();
		if(myFrame!=null) { myFrame.setLocationRelativeTo(cc); }	
	}


	private String get(String name)
	{	return(control.getDictionary().get(name));
	}

	
	
	static final long serialVersionUID =1L;
	/** The path to the buttery card image file. */
	public static final String BUTTERY_PATH = "cards/butteries/buttery%s";

	/** The path to the special building card image file. */
	public static final String SPECIAL_PATH = "cards/special";

	/** The path to the overview card image file. */
	public static final String SYMBOL_PATH = "symbols/%s";

	/** The path to the supply chits image files. */
	public static final String SUPPLY_PATH = "chits/supply/%s";

	/** The path to the goods chits image files. */
	public static final String GOODS_PATH = "chits/goods/good%s";

	/** Other constants or magic numbers. */
	private static final int LABELS_PER_COLUMN = 5;
	private static final int GOODS_PER_ROW = 8;
	private static final int GOODS_ROWS = 4;

	/** The absolute position constants */
	private final Point BOARD;
	private final Point COUNTER;
	private final Point SHIP;
	private final Point STANDARD;
	private final Point STACK;
	private final Point BUILDING;
	private final Point ROUND;

	/** The window and tab dimensions. */
	private final Dimension TAB_SIZE;
	private final Dimension WIN_SIZE;
	
	/** The content pane. */
	private final Container contentPane;

	/** All buildings in game. */
	private final Hashtable<Building, BuildingLabel> buildings = new Hashtable<Building, BuildingLabel>();

	/** All ships in game. */
	private final Hashtable<Ship, ShipLabel> ships = new Hashtable<Ship, ShipLabel>();

	/** The supply chit labels. */
	private final ArrayList<SupplyLabel> supplyLabels = new ArrayList<SupplyLabel>();

	/** The offer labels. */
	private final ArrayList<OfferLabel> offers = new ArrayList<OfferLabel>();

	/** The offer text labels. */
	private final ArrayList<JLabel> offerLabels = new ArrayList<JLabel>();

	/** The players' info labels. */
	private final ArrayList<ArrayList<JLabel>> infoLabels = new ArrayList<ArrayList<JLabel>>();

	/** The town's building panel. */
	private final JPanel townPanel = new JPanel();

	/** The players' building panels. */
	private final ArrayList<JPanel> buildingPanels = new ArrayList<JPanel>();

	/** The players' ship panels. */
	private final ArrayList<JPanel> shipPanels = new ArrayList<JPanel>();

	/** The player's ship pieces. */
	private final ArrayList<JLabel> counters = new ArrayList<JLabel>();

	/** The round card label. */
	private RoundLabel roundLabel = null;

	/** The info labels. */
	private final JLabel roundInfoLabel;
	private final JLabel turnInfoLabel;
	private final JLabel buildingInfoLabel;

	/** The action observer for the current player. */
	private SimpleObserver actionObserver = null;

	/** The special buildings label. */
	private final JLabel specialLabel;

	/** The tabbed pane. */
	private final JTabbedPane tabbedPane;

	/** Some frequently accessed buttons. */
	private final JButton backButton, continueButton, chatButton;

	/** True if the points are visible. */
	private final boolean pointsVisible;

	/**
	 *	Creates a new <code>MainWindow</code> instance.
	 * @param net
	 *	@param cont the control object
	 */
	@SuppressWarnings("serial")
	public MainWindow(NetworkInterface net,final LeHavre cont) 
	{
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		JFrame frame = net.getFrame();
		Insets ins = frame.getInsets();
		WIN_SIZE = gui.getSize("Window");
		int w = Math.min(G.Width(screen), WIN_SIZE.width+ins.left+ins.right);
		int h =  Math.min(G.Height(screen), WIN_SIZE.height+ins.top+ins.bottom);

		control = cont;
		contentPane = new JPanel(null);
		contentPane.setPreferredSize(gui.getSize("Content"));
		contentPane.setBackground(gui.getColor("Window"));
		contentPane.add(this);
		JScrollPane scroll = new JScrollPane(contentPane);
		scroll.getHorizontalScrollBar().setUnitIncrement(gui.getInt("ScrollUnit"));
		scroll.getVerticalScrollBar().setUnitIncrement(gui.getInt("ScrollUnit"));
		frame.setContentPane(scroll);
		{ //Component c = this;
		  //System.out.println("Scroll "+scroll);
		  //System.out.println("Content "+contentPane);
		  // show window hierarchy
		  // while(c!=null) { System.out.println("C "+c); c=c.getParent(); }
		}
		//net.getFrame().setSize(Math.min(screen.width, WIN_SIZE.width), Math.min(screen.height, WIN_SIZE.height));
		//setSize(w,h);
		setLocationRelativeTo(null);

		/* Tooltip settings */
		ToolTipManager ttm = ToolTipManager.sharedInstance();
		ttm.setDismissDelay(gui.getInt("DismissDelay"));
		ttm.setInitialDelay(gui.getInt("InitialDelay"));
		ttm.setReshowDelay(gui.getInt("ReshowDelay"));

		/* Draw the logo */
		JLabel label = new ImageLabel(control.network,null, String.format(SYMBOL_PATH, "logo2"), false);
		label.setLocation(gui.getOffset("Logo"));
		label.setComponentPopupMenu(
			new JPopupMenu() {{
				JMenuItem item = new JMenuItem(get("itemSaveState"));
				item.addActionListener(
					new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							JFileChooser fc = new JFileChooser();
							fc.setFileFilter(new SavFileFilter());
							if(fc.showSaveDialog(MainWindow.this) == JFileChooser.APPROVE_OPTION) {
								File file = fc.getSelectedFile();
								if(!file.getName().endsWith(SavFileFilter.ACCEPTED_EXTENSION)) {
									file = new File(file.getParentFile(), file.getName() + SavFileFilter.ACCEPTED_EXTENSION);
								}
								try {
									ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(file));
									stream.writeObject(control.getGameState());
									stream.close();
								} catch(Exception ex) {
									ex.printStackTrace();
									control.showError("SaveFailed");
								}
								control.showMessage(get("logSaveGameState"));
							}
						}
					}
				);
				add(item);
				item = new JMenuItem(get("itemDumpState"));
				item.addActionListener(
					new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							control.dump();
						}
					}
				);
				add(item);
				item = new JMenuItem(get("itemGlobalUndo"));
				item.addActionListener(
					new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							if(control.confirm("Refresh")) {
								enableDone(false);
								enableBack(false);
								control.restore(false);
							}
						}
					}
				);
				add(item);
				item = new JMenuItem(get("itemAbout"));
				item.addActionListener(
					new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							showInstructions("about", ((JMenuItem)e.getSource()).getText());
						}
					}
				);
				add(item);
			}}
		);
		contentPane.add(label);

		/* Draw info about type of game */
		GameState game = control.getGameState();
		label = new JLabel(get(game.getGameType().toString()));
		label.setBounds(gui.getBounds("Type"));
		label.setFont(FONT16);
		contentPane.add(label);

		/* Draw the game board */
		String language = control.getDictionary().getLanguage();
		label = new ImageLabel(control.network,language, "board", false);
		BOARD = gui.getOffset("Board");
		label.setLocation(BOARD.x, BOARD.y);
		contentPane.add(label);
		SHIP = gui.getOffset("Ship");
		pointsVisible = game.arePointsVisible();
		roundInfoLabel = new JLabel();
		turnInfoLabel = new JLabel();
		buildingInfoLabel = new ImageLabel(control.network,null, String.format(SYMBOL_PATH, "buildwarn"), false);

		/* Draw the supply chits */
		Supply supply;
		int[] dx = gui.getIntArray("SupplyX");
		int dy = BOARD.y + gui.getInt("SupplyY");
		for(int i = 0; i < Supply.values().length; i++) {
			supply = game.getSupply(i);
			label = new SupplyLabel(control.network,language, new SupplyMenu(control, supply), supply);
			label.setLocation(dx[i] += BOARD.x, dy);
			if(game.isVisible(i)) ((SupplyLabel)label).turnOver(control.network);
			contentPane.add(label, 0);
			supplyLabels.add((SupplyLabel)label);
		}
		for(Player player: game.getPlayers()) {
			label = new CounterLabel(control.network,player);
			label.setVisible(false);
			contentPane.add(label, 0);
			counters.add(label);
		}

		/* Save some constants */
		COUNTER = gui.getOffset("Counter");
		STANDARD = gui.getOffset("Standard");
		STANDARD.translate(BOARD.x, BOARD.y);
		STACK = gui.getOffset("Stack");
		BUILDING = gui.getOffset("BuildWarn");
		BUILDING.translate(STANDARD.x, STANDARD.y);
		ROUND = gui.getOffset("Round");
		ROUND.translate(BOARD.x, BOARD.y);
		Point p;

		/* Draw buildings */
		if(game.isLongGame()) {
			specialLabel = new ImageLabel(control.network,language, SPECIAL_PATH, false);
			p = gui.getOffset("Special");
			p.translate(BOARD.x, BOARD.y);
			specialLabel.setLocation(p);
			contentPane.add(specialLabel, 0);
		} else specialLabel = null;
		updateBuildingStacks();
		updateShipStacks();

		/* Create the initial offer */
		p = gui.getOffset("Offer");
		Rectangle r = gui.getBounds("Offer");
		r.translate(p.x, p.y);
		Good[] offered = {Good.Franc, Good.Fish, Good.Wood, Good.Clay, Good.Iron, Good.Grain, Good.Cattle};
		for(int i = 0; i < offered.length; i++) {
			label = new OfferLabel(control.network,language, new OfferMenu(control, offered[i]), offered[i]);
			label.setLocation(p.x + dx[i], p.y + dy);
			label.setEnabled(false);
			contentPane.add(label, 0);
			offers.add((OfferLabel)label);
			label = new JLabel();
			label.setFont(FONT15);
			label.setHorizontalAlignment(JLabel.CENTER);
			label.setLocation(r.x + dx[i], r.y + dy);
			label.setSize(r.getSize());
			label.setEnabled(false);
			label.setOpaque(true);
			contentPane.add(label, 0);
			offerLabels.add(label);
		}

		/* Draw the tabs */
		tabbedPane = new JTabbedPane();
		r = gui.getBounds("Tab");
		TAB_SIZE = r.getSize();
		tabbedPane.setBounds(r);
		contentPane.add(tabbedPane, 0);
		addComponentListener(
			new ComponentAdapter() {
				// resize player tabs if window resized
				@Override public void componentResized(ComponentEvent e) {
					final JFrame win = (JFrame)e.getSource();
					Dimension size = win.getSize();
					int dw = size.width - WIN_SIZE.width;
					int dh = size.height - WIN_SIZE.height;
					final int width = TAB_SIZE.width + (dw > 0 ? dw : 0) - (dh < 0 ? 15 : 0);
					final int height = TAB_SIZE.height + (dh > 0 ? dh : 0);
					SwingUtilities.invokeLater(
						new Runnable() {
							public void run() {
								tabbedPane.setSize(width, height);
								win.invalidate();
								win.repaint();
							}
						}
					);
				}
			}
		);
		createTownTab();
		for(int i = 1; i <= game.getPlayerCount(); i++) createPlayerTab(game.getPlayerBySeat(i));
		try {
			SwingUtilities.invokeAndWait(
				new Runnable() {
					public void run() {
						tabbedPane.setBackgroundAt(0, Color.black);
						tabbedPane.setForegroundAt(0, Color.white);
						for(Player player: control.getGameState().getPlayers()) tabbedPane.setBackgroundAt(player.getSeat(), player.getColor().toColor());
					}
				}
			);
		} catch(Exception e) {
			e.printStackTrace();
		}

		/* Create the button and info panel */
		JPanel panel = new JPanel(new GridLayout(3, 2, gui.getInt("ButtonHGap"), gui.getInt("ButtonVGap")));
		panel.setOpaque(false);
		panel.setBounds(gui.getBounds("Button"));
		contentPane.add(panel, 0);

		/* Draw the round and turn info labels */
		turnInfoLabel.setOpaque(true);
		turnInfoLabel.setFont(FONT14);
		turnInfoLabel.setHorizontalAlignment(JLabel.CENTER);
		turnInfoLabel.setBorder(BorderFactory.createRaisedBevelBorder());
		panel.add(turnInfoLabel);
		roundInfoLabel.setOpaque(true);
		roundInfoLabel.setFont(FONT14);
		roundInfoLabel.setBackground(gui.getColor("Tab"));
		roundInfoLabel.setHorizontalAlignment(JLabel.CENTER);
		roundInfoLabel.setBorder(BorderFactory.createRaisedBevelBorder());
		panel.add(roundInfoLabel);

		/* Draw the chat button */
		chatButton = new JButton(get("mainChat"));
		chatButton.setFont(FONT14);
		chatButton.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					control.openChat();
					chatButton.setEnabled(false);
				}
			}
		);
		panel.add(chatButton);
		if(control.isChatOpen()) chatButton.setEnabled(false);

		/* Draw the help button */
		JButton button = new JButton(get("mainHelp"));
		button.setFont(FONT14);
		button.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					showInstructions("mainInstructions", ((JButton)e.getSource()).getText());
				}
			}
		);
		panel.add(button);

		/* Draw the building button */
		button = new JButton(get("mainStacks"));
		button.setFont(FONT14);
		button.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					GameState game = control.getGameState();
					StringBuilder msg = new StringBuilder();
					String head = get("popupStackHead");
					String line = get("popupStackLine");
					ArrayList<Building> stack;
					msg.append("<html>");
					msg.append("<table border=\"0\">");
					msg.append("<colgroup span=\"3\"></colgroup>");
					msg.append("<tr>");
					GoodsList costs, goods = new GoodsList();
					int price = 0;
					for(int i = 0; i < GameState.STACK_COUNT; i++) {
						msg.append("<td>");
						msg.append(String.format(head, i + 1));
						stack = game.getStandard(i);
						if(stack.size() > 0) {
							for(Building building: stack) {
								msg.append(String.format(line, building.getIndex(), get("building" + building), building.getValue()));
								costs = building.getCosts();
								if(costs != null) goods.addAll(costs);
								price += building.getPrice();
							}
						} else msg.append(String.format("<p><i>&bull; %s</i></p>", get("empty")));
						msg.append("</td>");
					}
					msg.append("</tr>");
					msg.append("</table>");
					msg.append("<p>");
					Dictionary dict = control.getDictionary();
					if(goods.size() > 0) {
						goods.optimize();
						msg.append(String.format(get("popupBuildAll"), Util.getColored(dict, goods)));
					}
					if(price > 0) msg.append(String.format(get("popupPriceAll"), Util.getColored(dict, new GoodsPair(price, Good.Franc))));
					msg.append("</p>");
					msg.append("</html>");
					control.showInformation(msg.toString(), get("popupStacks"));
				}
			}
		);
		panel.add(button);

		/* Draw the ship button */
		button = new JButton(get("mainShips"));
		button.setFont(FONT14);
		button.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					GameState game = control.getGameState();
					StringBuilder msg = new StringBuilder();
					String suffix = get("mainSuffix");
					ArrayList<Ship> ships;
					msg.append("<html>");
					int j = 0, k;
					for(Ship.Type type: Ship.Type.values()) {
						if(j++ > 0) msg.append("<br>");
						ships = game.getShips(type);
						msg.append(String.format("<b>%s:</b> ", get("ships" + type)));
						k = 0;
						for(Ship ship: ships) {
							if(k++ > 0) msg.append(", ");
							msg.append(String.valueOf(ship.getValue()));
							msg.append(suffix);
						}
						if(k == 0) msg.append(String.format("<i>%s</i>", get("none")));
					}
					msg.append("</html>");
					control.showInformation(msg.toString(), get("popupShips"));
				}
			}
		);
		panel.add(button);

		/* Draw the continue button */
		continueButton = new JButton(get("mainContinue")) {
			private final Color active = gui.getColor("Active");
			private final Color inactive = gui.getColor("Inactive");
			public void setEnabled(boolean enabled) {
				super.setEnabled(enabled);
				setBackground(enabled ? active : inactive);
			}
		};
		continueButton.setFont(FONT16);
		r = gui.getBounds("Continue");
		r.translate(BOARD.x, BOARD.y);
		continueButton.setBounds(r);
		continueButton.setEnabled(false);
		continueButton.setToolTipText(Util.format(get("mainContinueInfo")));
		continueButton.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// check data integrity
					Player player = control.getCurrentPlayer();
					synchronized(player) {
						boolean fine = true;
						for(Good good: Good.values()) {
							if(player.getGood(good) < 0) {
								fine = false;
								break;
							}
						}
						if(!fine) {
							control.showError("Corrupt");
							control.restore(true);
							return;
						}
					}
					// disable stuff
					enableBack(false);
					enableDone(false);
					BuildingLabel buildingLabel;
					for(Building building: buildings.keySet()) {
						buildingLabel = buildings.get(building);
						buildingLabel.setBuildingEnabled(control.network,false);
						buildingLabel.setBanEnabled(false);
						enablePurchase(building, false);
					}
					ShipLabel shipLabel;
					for(Ship ship: ships.keySet()) {
						shipLabel = ships.get(ship);
						shipLabel.setBuildingEnabled(control.network,false);
						enablePurchase(ship, false);
					}
					synchronized(player) {
						player.setActions(0);
						player.setBuilds(0);
						player.setOfferAllowed(false);
						player.setBuildingAllowed(false);
						player.removeObserver(actionObserver);
						actionObserver = null;
					}
					// deactivate main station
					MainStation.getInstance().setActive(false);
					// next turn
					control.nextTurn();
				}
			}
		);
		contentPane.add(continueButton, 0);

		/* Draw the back button */
		backButton = new JButton(get("mainBack")) {
			private final Color active = gui.getColor("Active");
			private final Color inactive = gui.getColor("Inactive");
			public void setEnabled(boolean enabled) {
				super.setEnabled(enabled);
				setBackground(enabled ? active : inactive);
			}
		};
		backButton.setFont(FONT16);
		r = gui.getBounds("Back");
		r.translate(BOARD.x, BOARD.y);
		backButton.setBounds(r);
		backButton.setEnabled(false);
		backButton.setToolTipText(Util.format(get("mainBackInfo")));
		backButton.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					enableDone(false);
					enableBack(false);
					control.restore(true);
				}
			}
		);
		contentPane.add(backButton, 0);
		contentPane.setVisible(true);
		frame.setSize(w,h);
		log(get("logSetupComplete"));
	}

	/**
	 *	Called when the chat window has changed.
	 */
	public void chatChanged() {
		chatButton.setEnabled(!control.isChatOpen());
	}

	/**
	 *	Freezes the window.
	 *	Disables all components.
	 */
	public void freeze() {
		for(Building building: buildings.keySet()) buildings.get(building).setSaleEnabled(false);
		for(Ship ship: ships.keySet()) ships.get(ship).setSaleEnabled(false);
	}

	/**
	 *	Players need to pay food at the end of each round.
	 *	This method opens a dialog window to ask people
	 *	how they wish to pay. It checks for illegal over-
	 *	payments.
	 */
	public void endRound() {
		if(roundLabel != null) {
			Round round = control.getGameState().getRound();
			turnInfoLabel.setText(get("mainRoundEnd"));
			turnInfoLabel.setBackground(Color.black);
			turnInfoLabel.setForeground(Color.white);
			control.showMessage(String.format(get("logRoundEnd"), round.getIndex()));
			int demand = round.getFoodDemand();
			String text = Util.getColored(control.getDictionary(), new GoodsPair(demand, Good.Food));
			control.showMessage(String.format(get("logFoodDemand"), text));
			Player player = control.getCurrentPlayer();
			FishermansHut fishermansHut = FishermansHut.getInstance();
			if(player.owns(fishermansHut)) {
				int n = fishermansHut.getFishCount(control.getGameState().getPlayerCount());
				GoodsList goods = new GoodsList();
				goods.add(n, Good.Fish);
				control.receive(player, goods);
			}
			demand -= player.getFoodSupply();
			if(demand > 0) {
				control.tellFoodDemand(demand);
				double food = player.getFood();
				if(player.getPotentialFood() > food || food >= demand) {
					new FoodWindow(control, player, demand);
					return;
				} else {
					control.showWarning2(String.format(get("popupBankrupt"), text));
					control.loseAllFood(demand);
					return;
				}
			} else {
				control.endRound(null);
				return;
			}
		}
		nextRound();
	}

	/**
	 *	Draws the new round card. If there already was a round card, it
	 *	will be flipped over and laid out on the appropriate ship stack.
	 */
	public void nextRound() {
		GameState game = control.getGameState();
		boolean harvest = false;

		/* Process previous round card */
		if(roundLabel != null) {
			/* Create new ship */
			Round prev = game.getRound();
			Ship ship = prev.getShip();
			game.addShip(ship);
			createShip(ship);
			control.showMessage(String.format(get("logNewShip"), get("ship" + ship.getType()), ship.getValue()));

			/* Check specials */
			harvest = prev.isHarvest();
			switch(prev.getBuildingType()) {
				case Round.STANDARD_BUILDING:
					Building next = game.getNextBuilding();
					if(next == null) break;
					game.remove(next);
					game.getTown().receive(next);
					moveBuilding(next, null);
					next.setBuilt(true);
					control.showMessage(String.format(get("logNewStandard"), get("building" + next), next.getValue()));
					break;

				case Round.SPECIAL_BUILDING:
					ArrayList<Building> buildings = game.getSpecials();
					if(buildings.size() == 0) break;
					Building special = buildings.get(0);
					game.remove(special);
					checkSpecial(special);
					break;
			default:
				break;
			}
			roundLabel.getParent().remove(roundLabel);
		}

		/* Update players and harvest */
		StringBuilder msg;
		for(Player player: game.getPlayers()) {
			if(harvest) {
				msg = new StringBuilder();
				if(player.getGood(Good.Grain) >= GameState.HARVEST_GRAIN_CONDITION) {
					msg.append(Util.getColored(control.getDictionary(), new GoodsPair(GameState.HARVEST_GRAIN, Good.Grain)));
					player.receive(GameState.HARVEST_GRAIN, Good.Grain);
				}
				int cattle = player.getGood(Good.Cattle);
				if(cattle >= GameState.HARVEST_CATTLE_CONDITION) {
					if(msg.length() > 0) {
						msg.append(" ");
						msg.append(get("and"));
						msg.append(" ");
					}
					int amount = GameState.HARVEST_CATTLE;
					Feedlot feedlot = Feedlot.getInstance();
					if(player.owns(feedlot) && cattle >= feedlot.CATTLE_MIN && cattle <= feedlot.CATTLE_MAX) amount = feedlot.OUTPUT_CATTLE;
					msg.append(Util.getColored(control.getDictionary(), new GoodsPair(amount, Good.Cattle)));
					player.receive(amount, Good.Cattle);
				}
				if(msg.length() > 0) control.showMessage(String.format(get("logHarvest"), player.getName(), msg));
			}
			updatePlayer(player);
		}

		/* Create new round card */
		log(null);	// new line in log file
		game.nextRound();
		updateRoundCard();
		updateBuildingInfo();
		if(control.isServer()) control.nextTurn();
	}

	/**
	 *	Prepares the next turn.
	 */
	public void nextTurn() {
		GameState game = control.getGameState();
		game.setPrevious(game.copy());
		for(Player player: game.getPlayers()) {
			synchronized(player) {
				player.setReady(false);
			}
		}
		if(game.getTurn() > 0) log(null); // new line in log file
		game.nextTurn();
		final Player player = game.getActivePlayer();
		// Picket Line code
		PicketLine picketLine = PicketLine.getInstance();
		picketLine.next(control);
		// display player's name
		final String name = player.getName();
		turnInfoLabel.setText(name);
		turnInfoLabel.setBackground(player.getColor().toColor());
		turnInfoLabel.setForeground(Color.black);
		// activate stuff if current player
		if(control.isPlayer(player) || control.network.isStandaloneGame())
		{
			// add action observer
			actionObserver = new SimpleObserver() {
				public void update(SimpleObservable obs, Object eventType, Object obj) {
					if(obj instanceof String) {
						StringTokenizer tok = new StringTokenizer((String)obj, "=");
						if(!tok.nextToken().equals("actions")) return;
						int actions = Integer.parseInt(tok.nextToken());
						turnInfoLabel.setText(String.format("%s (%d)", name, actions));
					}
				}
			};
			player.addObserver(actionObserver);
			// set main actions
			if(game.isEndgame() && player.owns(WorkersCottages.getInstance())) {
				player.setActions(2);
				control.showInformation(get("popupWorkersCottages"), get("popupTurn"));
			} else player.setActions(1);
			player.setOfferAllowed(true);
			// Picket Line code
			boolean active = picketLine.isActive();
			if(active) control.showError("PicketLine");
			player.setBuildingAllowed(!active);
		}
		// update player tab and switch to it
		updatePlayer(player);
		switchToPlayer(player);
		if(!game.isSoloGame() && !control.network.isStandaloneGame())
		{
			control.showMessage(String.format(get("logGameTurn"), game.getActivePlayer().getName()));
			if(control.isPlayer(player)) control.showInformation(String.format(get("popupYourTurn"), player.getName()), get("popupTurn"));
		}
		boolean interest = false;
		int k = (game.getTurn() - 1) % GameState.TURNS_PER_ROUND;
		// next supply chit
		if(!game.isEndgame()) {
			Supply supply = game.getSupply(k);
			Good first = supply.getFirst();
			Good second = supply.getSecond();
			String text = String.format("%s, %s", Util.getColored(control.getDictionary(), first), Util.getColored(control.getDictionary(), second));
			supplyLabels.get(k).turnOver(control.network);
			if(!game.isVisible(k)) {
				game.setVisible(k);
				StringBuilder msg = new StringBuilder(text);
				if(supply.isInterest()) {
					msg.append(", ");
					msg.append(get("interest"));
				}
				control.showMessage(String.format(get("logSupplyChit"), msg));
			}
			game.fillOffer(first);
			game.fillOffer(second);
			updateOffer();
			control.showMessage(String.format(get("logOffer"), text));
			if(supply.isInterest()) {
				for(Player p: game.getPlayers()) {
					if(p.getLoans() > 0) {
						// Stock Market code
						if(p.owns(Stockmarket.getInstance())) {
							control.showMessage(String.format(get("logNoInterest"), p.getName()));
							continue;
						}
						// regular interest payment
						interest = true;
						if(control.isPlayer(p)) new MoneyWindow(control, p);
					}
				}
			}
		}
		Point location = supplyLabels.get(k).getLocation();
		JLabel label = counters.get(player.getIndex());
		label.setLocation(location.x + COUNTER.x, location.y + COUNTER.y);
		if(!label.isVisible()) label.setVisible(true);
		if(!interest) continueTurn();
	}

	/**
	 *	Continues with the turn (after paying interest).
	 */
	public void continueTurn() {
		GameState game = control.getGameState();
		for(Player player: game.getPlayers()) player.setReady(false);
		Player player = game.getActivePlayer();
		if(control.isPlayer(player)) {
			/* Enable undo option */
			Supply supply = game.getSupply();
			if(supply != null && supply.isInterest()) enableBack(true);

			/* Activate buildings */
			ArrayList<?> top = game.getTopmostBuildings();
			for(Building building: buildings.keySet()) {
				//BuildingLabel label = buildings.get(building);
				activate(building);
				if(!game.isEndgame() && (game.getTown().owns(building) || top.contains(building))) enablePurchase(building, true);
				else checkTobaccoFactory(player, building);
			}

			/* Activate ships */
			top = game.getTopmostShips();
			for(Ship ship: ships.keySet()) {
				//ShipLabel label = ships.get(ship);
				if(!game.isEndgame() && (game.getTown().owns(ship) || top.contains(ship))) enablePurchase(ship, true);
			}

			/* Activate offers */
			for(int i = 0; i < GameState.OFFER_COUNT; i++) if(game.getOffer(i) > 0) offers.get(i).setOfferEnabled(true);
		}
	}

	/**
	 *	The active player takes the given good offer.
	 *	@param good the good offer
	 */
	public void takeOffer(Good good) {
		GameState game = control.getGameState();
		Player player = game.getActivePlayer();
		game.clearOffer(good);
		updateOffer();
		updatePlayer(player);
		if(control.isPlayer(player)) {
			disableAll();
			enableDone(true);
		}
	}

	/**
	 *	The active player buys the given building.
	 *	@param building the building
	 *	@param town provide true if the building belonged to the town
	 */
	public void buy(Building building, boolean town) {
		GameState game = control.getGameState();
		Player player = game.getActivePlayer();
		BuildingLabel label = buildings.get(building);
		if(control.isPlayer(player)) {
			activate(building);
			enablePurchase(building, false);
			label.setSaleEnabled(true);
			label.setBuildingEnabled(control.network,false);
			if(!town) {
				ArrayList<Building> topmost = game.getTopmostBuildings();
				Building location = player.getLocation();
				boolean buildable = ((location != null) && (location instanceof BuildingsBuilder) && (player.getBuilds() > 0));
				ArrayList<Building> possible = null;
				if(buildable) possible = location.getBuildings(control);
				for(Building top: topmost) {
					label = buildings.get(top);
					enablePurchase(top, true);
					label.setBuildingEnabled(control.network,buildable && possible.contains(top));
				}
			}
		}
		if(building.equals(TobaccoFactory.getInstance())) for(Building bld: buildings.keySet()) checkTobaccoFactory(player, bld);
		updateBuildingInfo();
		moveBuilding(building, player);
		updatePlayer(player);
	}

	/**
	 *	The given player sells the given building.
	 *	@param building the sold building
	 *	@param player the selling player
	 */
	public void sell(Building building, Player player) {
		GameState game = control.getGameState();
		BuildingLabel label = buildings.get(building);
		boolean active = control.isPlayer(game.getActivePlayer());
		if(control.isPlayer(player)) {
			label.setSaleEnabled(false);
			if(active && building.equals(TobaccoFactory.getInstance())) for(Building bld: buildings.keySet()) buildings.get(bld).setBanEnabled(false);
		}
		if(active) activate(building);
		moveBuilding(building, null);
		updatePlayer(player);
	}

	/**
	 *	Activates the given building. If the active player
	 *	hasn't performed their main action, yet, they may
	 *	enter this building.
	 *	@param building the building
	 */
	public void activate(Building building) {
		GameState game = control.getGameState();
		Player player = game.getActivePlayer();
		if(control.isPlayer(player) && player.getActions() > 0 && player.isBuildingAllowed()) {
			BuildingLabel label = buildings.get(building);
			if(building.isEnterable() && !building.isWorker(player.getIndex())
				&& (!building.isOccupied() || game.isEndgame())) label.setEntryEnabled(true);
			label.setBanEnabled(false);
		}
	}

	/**
	 *	Enables the ban option for the given building if the given
	 *	player owns the Tobacco Factory.
	 *	@param player the player
	 *	@param building the building
	 */
	private void checkTobaccoFactory(Player player, Building building) {
		if(player.owns(TobaccoFactory.getInstance()) && player.owns(building) && building.isBanAllowed() && building.isOccupied()) {
			int workers = building.getWorkerCount();
			if(building.isWorker(player.getIndex())) workers--;
			if(workers > 0) buildings.get(building).setBanEnabled(true);
		}
	}

	/**
	 *	The active player buys the given ship.
	 *	@param ship the ship
	 *	@param town provide true if the ship belonged to the town
	 */
	public void buy(Ship ship, boolean town) {
		GameState game = control.getGameState();
		Player player = game.getActivePlayer();
		ShipLabel label = ships.get(ship);
		if(control.isPlayer(player)) {
			label.setBuildingEnabled(control.network,false);
			enablePurchase(ship, false);
			label.setSaleEnabled(true);
			if(!town) {
				ArrayList<Ship> topmost = game.getTopmostShips();
				Building location = player.getLocation();
				boolean buildable = ((location != null) && (location instanceof ShipsBuilder) && (player.getBuilds() > 0));
				ArrayList<Buildable> possible = null;
				if(buildable) possible = location.getShips(control);
				for(Ship top: topmost) {
					label = ships.get(top);
					enablePurchase(top, true);
					label.setBuildingEnabled(control.network,buildable && possible.contains(top));
				}
			}
		}
		moveShip(ship, player, player.getShips().size() - 1);
		updatePlayer(player);
	}

	/**
	 *	The given player sells the given ship.
	 *	@param ship the sold ship
	 *	@param player the selling player
	 */
	public void sell(Ship ship, Player player) {
		ShipLabel label = ships.get(ship);
		if(control.isPlayer(player)) label.setSaleEnabled(false);
		updateShips(player);
		updatePlayer(player);
		updateShipStacks();
	}

	/**
	 *	Checks for special buildings with extra rules, e. g.
	 *	the football stadium, dunny or construction site.
	 *	@param special the special building to check
	 */
	public void checkSpecial(Building special) {
		final GameState game = control.getGameState();
		String name = get("building" + special);
		int value = special.getValue();
		ArrayList<Building> buildings;
		Building standard;
		// Dunny: move it to the Building proposals
		if(special.equals(Dunny.getInstance())) {
			buildings = game.getTopmostBuildings();
			int stack = 0;
			if(buildings.size() > 0) {
				standard = buildings.get(0);
				for(Building building: buildings) if(building.getIndex() > standard.getIndex()) standard = building;
				stack = game.getStack(standard);
			}
			game.addStack(special, stack);
			updateBuildingStacks();
			control.showMessage(String.format(get("logNewStack"), name, value));
		// any other special building
		} else {
			Town town = game.getTown();
			if(!town.owns(special)) town.receive(special);
			moveBuilding(special, null);
			// Construction Site: deal hand cards
			if(special.equals(ConstructionSite.getInstance())) {
				for(Player player: game.getPlayers()) {
					player.setHandCards(town.getLeftOver());
					if(control.isPlayer(player)) ConstructionSite.getInstance().showHandCards(control, false);
				}
			// Rattletrap Car: create observer for taking iron
			} else if(special.equals(RattletrapCar.getInstance())) {
				SimpleObserver obs = new SimpleObserver() {
					public void update(SimpleObservable obs, Object eventType, Object obj) {
						if(obj instanceof String) {
							RattletrapCar rc = RattletrapCar.getInstance();
							if(rc.getOwner() >= 0) return;
							StringTokenizer tok = new StringTokenizer((String)obj, "=");
							if(!tok.nextToken().equals("good")) return;
							tok = new StringTokenizer(tok.nextToken(), ",");
							Good good = Good.valueOf(tok.nextToken());
							if(!good.equals(Good.Iron)) return;
							int oldValue = Integer.parseInt(tok.nextToken());
							int newValue = Integer.parseInt(tok.nextToken());
							if(oldValue >= newValue) return;
							// player got iron
							Player player = (Player)obs;
							player.receive(rc);
							moveBuilding(rc, player);
							updatePlayer(player);
							control.showMessage(String.format(get("logRattletrapCar"), player.getName()));
						}
					}
				};
				for(Player player: game.getPlayers()) player.addObserver(obs);
			}
			// Football Stadium: check condition
			if(special.equals(FootballStadium.getInstance())) checkFootballStadium();
			// otherwise build the special building
			else special.setBuilt(true);
			control.showMessage(String.format(get("logNewSpecial"), name, value));
		}
	}

	/**
	 *	Checks if the town owns the football stadium.
	 *	If the football stadium isn't been built, yet,
	 *	it'll be added to the first empty building
	 *	stack if available.
	 */
	public void checkFootballStadium() {
		GameState game = control.getGameState();
		Building building = FootballStadium.getInstance();
		Town town = game.getTown();
		if(town.owns(building) && !building.isBuilt() && game.getTopmostBuildings().size() < GameState.STACK_COUNT) {
			BuildingLabel label = buildings.get(building);
			town.lose(building);
			townPanel.remove(label);
			game.addStack(building);
			contentPane.add(label, 0);
			updateBuildingStacks();
			control.showMessage(String.format(get("logNewStack"), get("building" + building), building.getValue()));
		}
	}

	/**
	 *	Enables the building option of the given buildings.
	 *	@param buildings the buildings
	 */
	public void enableBuildings(ArrayList<Building> buildings) {
		for(Building building: buildings) this.buildings.get(building).setBuildingEnabled(control.network,true);
	}

	/**
	 *	Enables the building option of the given ships.
	 *	@param ships the ships
	 */
	public void enableShips(ArrayList<Buildable> ships) {
		for(Buildable ship: ships) {
			if(ship instanceof Ship) this.ships.get(ship).setBuildingEnabled(control.network,true);
			else if(ship instanceof Building) buildings.get(ship).setBuildingEnabled(control.network,true);
		}
	}

	/**
	 *	Disables all possible main actions if no actions left.
	 *	Activates the back & continue buttons if wished.
	 *	Returns true if no actions are left.
	 */
	public void disableAll() {
		if(control.getCurrentPlayer().getActions() > 0) return;
		for(Building building: buildings.keySet()) if(building.isBuilt()) buildings.get(building).setEntryEnabled(false);
		for(int i = 0; i < GameState.OFFER_COUNT; i++) offers.get(i).setOfferEnabled(false);
	}

	/**
	 *	Enables or disables the purchase of the given building.
	 *	@param building the building
	 *	@param enabled provide true to enable purchase
	 */
	public void enablePurchase(Building building, boolean enabled) {
		buildings.get(building).setPurchaseEnabled(enabled);
	}

	/**
	 *	Enables or disables the purchase of the given ship.
	 *	@param ship the ship
	 *	@param enabled provide true to enable purchase
	 */
	public void enablePurchase(Ship ship, boolean enabled) {
		ships.get(ship).setPurchaseEnabled(enabled);
	}

	/**
	 *	Enables or disables the back button.
	 *	@param enabled provide true to enable the button
	 */
	public void enableBack(boolean enabled) {
		backButton.setEnabled(enabled);
	}

	/**
	 *	Enables or disables the continue button.
	 *	@param enabled provide true to enable the button
	 */
	public void enableDone(boolean enabled) {
		if(enabled) enableBack(true);
		if(control.getCurrentPlayer().getActions() == 0) continueButton.setEnabled(enabled);
	}

	/**
	 *	Removes the given object from the game.
	 *	@param object the object
	 */
	public void destroy(Buildable object) {
		JLabel label = null;
		if(object instanceof Building) label = buildings.get(object);
		else if(object instanceof Ship) label = ships.get(object);
		if(label!=null)
		{
		final Container parent = label.getParent();
		parent.remove(label);
		if(object instanceof Ship) updateShips(control.getGameState().getActivePlayer());
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				parent.invalidate();
				parent.repaint();
			}
		});
		}
	}

	/**
	 *	Creates the town's tab.
	 *	@param tab the tabbed pane
	 */
	private void createTownTab() {
		JPanel panel = new JPanel(null);
		panel.setBackground(gui.getColor("Tab"));
		panel.setOpaque(true);
		panel.setPreferredSize(gui.getSize("Scroll"));

		/* Draw start buildings */
		Dimension gap = gui.getSize("Gap");
		townPanel.setLayout(new FlowLayout(FlowLayout.LEADING, G.Width(gap),G.Height(gap)));
		townPanel.setOpaque(false);
		townPanel.setBounds(gui.getBounds("Town"));
		panel.add(townPanel);
		GameState game = control.getGameState();
		OverviewCard card = new OverviewCard(game.getGameType(), game.getPlayerCount());
		townPanel.add(new OverviewCardLabel(control.network,control.getDictionary().getLanguage(), new OverviewCardMenu(control, card), card));
		updateTown(true);

		/* Draw scroll pane. */
		JScrollPane scroll = new JScrollPane(panel);
		scroll.getVerticalScrollBar().setUnitIncrement(gui.getInt("ScrollUnit"));
		tabbedPane.addTab(get("town"), scroll);
	}

	/**
	 *	Moves the given building to the panel of the given player.
	 *	If the player is null, the town gets the building.
	 *	@param building the building
	 *	@param player the player
	 */
	@SuppressWarnings("serial")
	public void moveBuilding(Building building, Player player) {
		BuildingLabel label = buildings.get(building);
		if(label != null) {
			Container parent = label.getParent();
			if(parent != null) parent.remove(label);
		} else {
			BuildingMenu menu;
			if(building.equals(ConstructionSite.getInstance())) {
				menu = new BuildingMenu(control, building) {
					protected void doSpecial() {
						if(control.getGameState().isOver()) return;
						if(control.getCurrentPlayer().getHandCards() == null) super.doSpecial();
						else ConstructionSite.getInstance().showHandCards(control, false);
					}
				};
			} else if(building.equals(Pawnbrokers.getInstance())) {
				menu = new BuildingMenu(control, building) {
					protected void doSpecial() {
						if(control.getGameState().isOver()) return;
						Pawnbrokers.getInstance().showGoods(control);
					}
				};
			} else menu = new BuildingMenu(control, building);
			label = new BuildingLabel(control.network,control.getDictionary().getLanguage(), menu, building);
			buildings.put(building, label);
		}
		if(player != null) {
			JPanel panel = buildingPanels.get(player.getSeat() - 1);
			int k = 0;
			for(Component comp: panel.getComponents()) {
				if(building.compareTo(((BuildingLabel)comp).getBuilding()) < 0) k++;
				else break;
			}
			panel.add(label, k);
			if(control.isPlayer(player)) label.setSaleEnabled(true);
		} else {
			townPanel.add(label);
			label.setSaleEnabled(false);
		}
		label.updateView(control.network);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				invalidate();
				repaint(20);
			}
		});
	}

	/**
	 *	Moves the given ship to the panel of the given player.
	 *	If the player is null, the town gets the ship.
	 *	@param ship the ship
	 *	@param player the player
	 *	@param position the position
	 */
	public void moveShip(Ship ship, Player player, int position) {
		ShipLabel label = ships.get(ship);
		if(label != null) label.getParent().remove(label);
		else {
			label = new ShipLabel(control.network,control.getDictionary().getLanguage(), new ShipMenu(control, ship), ship);
			ships.put(ship, label);
		}
		Container parent = townPanel;
		if(player != null) {
			label.setLocation(position * SHIP.x, position * SHIP.y);
			label.setSaleEnabled(true);
			parent = shipPanels.get(player.getSeat() - 1);
		}
		parent.add(label, 0);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				invalidate();
				repaint(20);
			}
		});
	}

	/**
	 *	Creates the label for the given ship.
	 *	@param ship the ship
	 */
	private ShipLabel createShip(Ship ship) {
		ShipLabel label = ships.get(ship);
		if(label == null) {
			label = new ShipLabel(control.network,control.getDictionary().getLanguage(), new ShipMenu(control, ship), ship);
			Point location = gui.getOffset(ship.getType().toString());
			location.translate(BOARD.x, BOARD.y);
			label.setLocation(location);
			contentPane.add(label, 0);
			ships.put(ship, label);
		}
		return label;
	}

	/**
	 *	Creates an info label with the given font color for
	 *	the given player and adds it to the given panel.
	 *	@param player the player
	 *	@param panel the panel
	 *	@param labels the list of labels
	 *	@param color the color
	 */
	private void createInfoLabel(Player player, JPanel panel, ArrayList<JLabel> labels, Color color, String tooltip) {
		int k = labels.size();
		JLabel label = new JLabel();
		label.setFont(FONT16);
		Point p = gui.getOffset("Info");
		p.translate(gui.getInt("InfoDX") * (k / LABELS_PER_COLUMN), gui.getInt("InfoDY") * (k % LABELS_PER_COLUMN));
		label.setLocation(p);
		label.setSize(gui.getSize("Info"));
		label.setForeground(color);
		label.setToolTipText(tooltip != null ? Util.format(tooltip) : null);
		labels.add(label);
		panel.add(label, 0);
	}

	/**
	 *	Updates the building stacks.
	 */
	public void updateBuildingStacks() {
		GameState game = control.getGameState();
		ArrayList<Building> stack;
		Building building;
		BuildingLabel label;
		Container parent;
		String language = control.getDictionary().getLanguage();
		for(int i = 0, n; i < GameState.STACK_COUNT; i++) {
			stack = game.getStandard(i);
			n = stack.size();
			for(int j = 1; j <= n; j++) {
				building = stack.get(n - j);
				label = buildings.get(building);
				if(label == null) {
					label = new BuildingLabel(control.network,language, new BuildingMenu(control, building), building);
					buildings.put(building, label);
				}
				label.setBanEnabled(false);
				label.setBuildingEnabled(control.network,false);
				label.setEntryEnabled(false);
				label.setPurchaseEnabled(false);
				label.setSaleEnabled(false);
				label.updateView(control.network);	// for future purposes
				parent = label.getParent();
				if(parent != null) parent.remove(label);
				label.setLocation(STANDARD.x + STACK.x * i, STANDARD.y + STACK.y * j);
				contentPane.add(label, 0);
			}
		}
		updateBuildingInfo();
	}

	/**
	 *	Updates the ship stacks.
	 */
	public void updateShipStacks() {
		GameState game = control.getGameState();
		ShipLabel label;
		Point location;
		ArrayList<Ship> ships;
		for(Ship.Type type: Ship.Type.values()) {
			ships = game.getShips(type);
			for(int i = ships.size() - 1; i >= 0; i--) {
				label = createShip(ships.get(i));
				label.setBuildingEnabled(control.network,false);
				label.setPurchaseEnabled(false);
				label.setSaleEnabled(false);
				label.getParent().remove(label);
				location = gui.getOffset(type.toString());
				location.translate(BOARD.x, BOARD.y);
				label.setLocation(location);
				contentPane.add(label, 0);
			}
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				invalidate();
				repaint(20);
			}
		});
	}

	/**
	 *	Updates the information on when the next standard
	 *	building will be built by the town.
	 */
	public void updateBuildingInfo() {
		Container parent = buildingInfoLabel.getParent();
		if(parent != null) parent.remove(buildingInfoLabel);
		GameState game = control.getGameState();
		Building next = game.getNextBuilding();
		Round round = game.getRound();
		if(next == null || round == null) return;
		int current = round.getIndex(), index = 0;
		Round[] cards = Setup.getRoundCards(game.getGameType(), game.getPlayerCount());
		for(int i = current - 1; i < cards.length; i++) {
			if(cards[i].getBuildingType() == Round.STANDARD_BUILDING) {
				index = i + 1;
				break;
			}
		}
		if(index == 0) return;
		Player player = game.getPlayerBySeat((index * GameState.TURNS_PER_ROUND) % game.getPlayerCount() + 1);
		index -= current;
		int stack = game.getStack(next);
		int count = game.getStandard(stack).size();
		buildingInfoLabel.setLocation(BUILDING.x + STACK.x * stack, BUILDING.y + STACK.y * count);
		String text, info = "";
		if(index > 1) text = String.format(get("bldInRounds"), Util.getNumbered(control.getDictionary(), index, "round"));
		else text = get(String.format("bld%sRound", index > 0 ? "Next" : "This"));
		if(!game.isSoloGame()) {
			StringBuilder msg = new StringBuilder("<br>");
			if(control.isPlayer(player)) msg.append(get("bldYouWill"));
			else msg.append(String.format(get("bldOtherWill"), player.getName()));
			info = msg.toString();
		}
		buildingInfoLabel.setToolTipText(Util.format(String.format(get("mainBuildingInfo"), text, info)));
		contentPane.add(buildingInfoLabel, 0);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				contentPane.invalidate();
				contentPane.repaint();
			}
		});
	}

	/**
	 *	Updates the buildings of the given player.
	 *	@param player the player
	 */
	public void updateBuildings(Player player) {
		for(Building building: player.getBuildings()) moveBuilding(building, player);
		JPanel panel = buildingPanels.get(player.getSeat() - 1);
		Building building;
		for(Component component: panel.getComponents()) {
			if(component instanceof BuildingLabel) {
				building = ((BuildingLabel)component).getBuilding();
				if(!player.owns(building)) panel.remove(component);
			}
		}
	}

	/**
	 *	Updates the ships of the given player.
	 *	@param player the player
	 */
	public void updateShips(Player player) {
		ArrayList<Ship> ships = player.getShips();
		for(int i = 0; i < ships.size(); i++) moveShip(ships.get(i), player, i);
	}

	/**
	 *	Updates the text labels of the offer spaces.
	 */
	public void updateOffer() {
		GameState game = control.getGameState();
		JLabel label;
		for(int i = 0, n; i < GameState.OFFER_COUNT; i++) {
			n = game.getOffer(i);
			offers.get(i).setEnabled(n > 0);
			label = offerLabels.get(i);
			label.setText(String.valueOf(n));
			label.setEnabled(n > 0);
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				contentPane.invalidate();
				contentPane.repaint();
			}
		});
	}

	/**
	 *	Updates the town tab.
	 */
	public void updateTown(boolean first) {
		Town town = control.getGameState().getTown();
		for(Building building: town.getBuildings()) {
			if(first && building.isSpecial()) checkSpecial(building);
			else moveBuilding(building, null);
		}
		Building building;
		for(Component component: townPanel.getComponents()) {
			if(component instanceof BuildingLabel) {
				building = ((BuildingLabel)component).getBuilding();
				if(!town.owns(building)) townPanel.remove(component);
			}
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				townPanel.invalidate();
				townPanel.repaint();
			}
		});
	}

	/**
	 *	Updates the round card.
	 */
	public void updateRoundCard() {
		GameState game = control.getGameState();
		Round round = game.getRound();

		/* Create new round card */
		if(!game.isEndgame()) {
			roundLabel = new RoundLabel(control.network,control.getDictionary().getLanguage(), new RoundMenu(control, round), round);
			roundLabel.setLocation(ROUND);
			contentPane.add(roundLabel, 0);
			int roundNumber = round.getIndex();
			roundInfoLabel.setText(String.format(get("mainRound"), roundNumber, game.getRoundCount()));
			log(String.format(get("logNewRound"), roundNumber), false);
			log(null);	// new line in log file
		} else {
			log(get("logFinalPhase"), false);
			log(null);	// new line in log file
			control.showMessage(get("logEndgame"));
			control.showWarning2(get("popupEndgame"));
			roundLabel = null;
			roundInfoLabel.setText(get("mainFinalPhase"));
		}

		/* Update special building info */
		if(specialLabel != null) {
			String text, info = "";
			int index = -1;
			if(round != null) {
				Round[] cards = Setup.getRoundCards(game.getGameType(), game.getPlayerCount());
				int current = round.getIndex();
				for(int i = current - 1; i < cards.length; i++) {
					if(cards[i].getBuildingType() == Round.SPECIAL_BUILDING) {
						index = i + 1;
						break;
					}
				}
				if(!game.isSoloGame() && index > 0) {
					StringBuilder msg = new StringBuilder(" ");
					Player player = game.getPlayerBySeat((index * GameState.TURNS_PER_ROUND) % game.getPlayerCount() + 1);
					if(player != null) {
						if(control.isPlayer(player)) msg.append(get("bldYouWill"));
						else msg.append(String.format(get("bldOtherWill"), player.getName()));
						info = msg.toString();
					}
				}
				index -= current;
			}
			if(index < 0) text = get("bldNever");
			else if(index > 1) text = String.format(get("bldInRounds"), Util.getNumbered(control.getDictionary(), index, "round"));
			else text = get(String.format("bld%sRound", index > 0 ? "Next" : "This"));
			specialLabel.setToolTipText(Util.format(String.format(get("mainSpecial"), game.getSpecials().size(), text, info)));
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				contentPane.invalidate();
				contentPane.repaint();
			}
		});
	}

	/**
	 *	Updates the text labels of the given player.
	 *	@param player the player
	 */
	public void updatePlayer(Player player) {
		ArrayList<JLabel> labels = infoLabels.get(player.getSeat() - 1);
		GameState game = control.getGameState();
		Dictionary dict = control.getDictionary();

		/* Update ships info */
		Ship.Type[] types = Ship.Type.values();
		int[] ships = new int[types.length];
		int k = 0, value = 0;
		for(Ship ship: player.getShips()) {
			ships[ship.getType().ordinal()]++;
			value += ship.getCapacity();
		}
		if(player.owns(LuxuryYacht.getInstance())) ships[Ship.Type.Iron.ordinal()]++;
		else if(player.owns(MSDagmar.getInstance())) ships[Ship.Type.Luxury.ordinal()]++;
		for(Ship.Type ship: types) labels.get(k).setText(String.format("%s: %d", get("ships" + ship), ships[k++]));
		labels.get(k++).setText(String.format(get("mainCapacity"), value));

		/* Update general info */
		value = player.getMoney();
		labels.get(k++).setText(String.format(get("mainMoney"), value));
		double food = player.getFood();
		labels.get(k++).setText(String.format(get("mainFood"), Util.format(food - value)));
		labels.get(k++).setText(String.format(get("mainEnergy"), Util.format(player.getEnergy())));
		JLabel label = labels.get(k++);
		if(pointsVisible) label.setText(String.format(get("mainPoints"), player.getPoints()));
		else if(!label.isVisible()) label.setVisible(false);
		value = (game.isEndgame() ? 0 : (game.getTurnCount() - game.getTurn() + player.getSeat() - 1) / game.getPlayerCount() + 1);
		labels.get(k++).setText(String.format(get("mainTurns"), value));
		int craft = 0, fishing = 0, hammer = 0;
		for(Building building: player.getBuildings()){
			if(building.isCraft()) craft++;
			fishing += building.getFishing();
			hammer += building.getHammer();
		}
		labels.get(k++).setText(String.format("%d\u00D7", craft));
		labels.get(k++).setText(String.format("%d\u00D7", hammer));
		labels.get(k++).setText(String.format("%d\u00D7", fishing));
		// loans
		label = labels.get(k++);
		value = player.getLoans();
		boolean loansEnabled = control.isPlayer(player) && (value > 0);
		label.setEnabled(loansEnabled);
		if(loansEnabled) label.setToolTipText(Util.format(String.format(
			get("mainLoans"),
			Util.getNumbered(dict, value, "loan"),
			Util.getColored(dict, new GoodsPair(GameState.LOAN_PAYBACK, Good.Franc)),
			Util.getColored(dict, new GoodsPair(GameState.LOAN_PENALTY, Good.Franc))
		)));
		else label.setToolTipText(null);
		label = labels.get(k++);
		label.setText(String.format("\u00D7%d", value));
		label.setEnabled(value > 0);
		int supply = player.getFoodSupply();
		labels.get(k++).setToolTipText(supply > 0 ? Util.format(String.format(get("mainFoodSupply"), Util.getColored(dict, new GoodsPair(supply, Good.Food)))) : null);
		labels.get(k++).setText(String.valueOf(supply));
		food += supply;
		Round round = game.getRound();
		value = (round != null ? round.getFoodDemand() : 0);
		boolean deficient = (!game.isEndgame() && value > food);
		label = labels.get(k++);
		label.setVisible(deficient);
		if(deficient) label.setToolTipText(Util.format(String.format(
			get("mainFoodDeficiency"),
			Util.getColored(dict, new GoodsPair(value, Good.Food)),
			Util.getColored(dict, new GoodsPair(food, Good.Food))
		)));

		/* Update goods info */
		for(Good good: Good.values()) {
			if(!good.isBasic()) continue;
			value = player.getGood(good);
			labels.get(k).setEnabled(value > 0);
			label = labels.get(k + GOODS_PER_ROW);
			label.setText(String.valueOf(value));
			label.setEnabled(value > 0);
			k++;
		}
		k += GOODS_PER_ROW;
		for(Good good: Good.values()) {
			if(!good.isProcessed()) continue;
			value = player.getGood(good);
			labels.get(k).setEnabled(value > 0);
			label = labels.get(k + GOODS_PER_ROW);
			label.setText(String.valueOf(value));
			label.setEnabled(value > 0);
			k++;
		}
		k += GOODS_PER_ROW;
	}

	/**
	 *	Creates the tab for the given player.
	 *	@param tabs the tabbed pane
	 *	@param player the player
	 */
	private void createPlayerTab(Player player) {
		JPanel panel = new JPanel(null);
		panel.setBackground(player.getColor().toColor());
		panel.setOpaque(true);
		panel.setPreferredSize(gui.getSize("Scroll"));

		/* Create the info labels */
		JPanel back = new JPanel(null);
		back.setBackground(gui.getColor("Tab"));
		back.setBorder(BorderFactory.createLoweredBevelBorder());
		back.setBounds(gui.getBounds("Info"));
		panel.add(back);
		ArrayList<JLabel> labels = new ArrayList<JLabel>();
		infoLabels.add(labels);
		Color[] colors = {
			gui.getColor("ShipWooden"),	// Wooden Ships
			gui.getColor("ShipIron"),	// Iron Ships
			gui.getColor("ShipSteel"),	// Steel Ships
			gui.getColor("ShipLuxury"),	// Luxury Liners
			null,						// Capacity
			gui.getColor("Franc"),		// Total Francs
			gui.getColor("Food"),		// Total Food
			gui.getColor("Energy"),		// Total Energy
			gui.getColor("Points"),		// Total Points
			null						// Turns Left
		};
		String[] tooltips = {
			null,						// Wooden Ships
			null,						// Iron Ships
			null,						// Steel Ships
			null,						// Luxury Liners
			get("mainCapacityInfo"),	// Capacity
			null,						// Total Francs
			get("mainFoodInfo"),		// Total Food
			get("mainEnergyInfo"),		// Total Energy
			pointsVisible ? get("mainPointsInfo") : null,
			get("mainTurnsInfo")		// Turns Left
		};
		for(int i = 0; i < colors.length; i++) createInfoLabel(player, panel, labels, colors[i], tooltips[i]);

		/* Craft symbol */
		JLabel label = new JLabel();
		label.setHorizontalAlignment(JLabel.RIGHT);
		label.setFont(FONT20);
		label.setBounds(gui.getBounds("Craft"));
		labels.add(label);
		panel.add(label, 0);
		label = new ImageLabel(control.network,null, String.format(SYMBOL_PATH, "craft"), false);
		label.setLocation(gui.getOffset("Craft"));
		panel.add(label, 0);

		/* Hammer symbol */
		label = new JLabel();
		label.setHorizontalAlignment(JLabel.RIGHT);
		label.setFont(FONT20);
		label.setBounds(gui.getBounds("Hammer"));
		labels.add(label);
		panel.add(label, 0);
		label = new ImageLabel(control.network,null, String.format(SYMBOL_PATH, "hammer"), false);
		label.setLocation(gui.getOffset("Hammer"));
		panel.add(label, 0);

		/* Fishing symbol */
		label = new JLabel();
		label.setHorizontalAlignment(JLabel.RIGHT);
		label.setFont(FONT20);
		label.setBounds(gui.getBounds("Fishing"));
		labels.add(label);
		panel.add(label, 0);
		label = new ImageLabel(control.network,null, String.format(SYMBOL_PATH, "fishing"), false);
		label.setLocation(gui.getOffset("Fishing"));
		panel.add(label, 0);

		/* Create the loans label */
		String language = control.getDictionary().getLanguage();
		label = new LoanLabel(control.network,language, new LoanMenu(control, player.getIndex()));
		label.setLocation(gui.getOffset("Loan"));
		labels.add(label);
		panel.add(label);
		label = new JLabel();
		label.setHorizontalAlignment(JLabel.RIGHT);
		label.setFont(FONT20);
		label.setBounds(gui.getBounds("Loan"));
		labels.add(label);
		panel.add(label, 0);

		/* Create the buttery */
		Point p = gui.getOffset("Buttery");
		label = new ImageLabel(control.network,language, String.format(BUTTERY_PATH, player.getColor()), false);
		label.setLocation(p);
		panel.add(label);

		/* Create the food token */
		label = new ImageLabel(control.network,null, String.format(SYMBOL_PATH, "food"), false);
		Point q = gui.getOffset("Food");
		q.translate(p.x, p.y);
		label.setLocation(q.x, q.y);
		labels.add(label);
		panel.add(label, 0);

		/* Create the food label */
		label = new JLabel();
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setFont(FONT14);
		label.setForeground(Color.white);
		Rectangle r = gui.getBounds("Food");
		r.translate(p.x, p.y);
		label.setBounds(r);
		labels.add(label);
		panel.add(label, 0);

		/* Create the warning label */
		label = new ImageLabel(control.network,null, String.format(SYMBOL_PATH, "foodwarn"), false);
		q = gui.getOffset("Warn");
		q.translate(p.x, p.y);
		label.setLocation(q);
		labels.add(label);
		panel.add(label, 0);

		/* Create the ships panel */
		JPanel shipPanel = new JPanel(null);
		shipPanel.setOpaque(false);
		shipPanel.setBounds(gui.getBounds("Ship"));
		shipPanels.add(shipPanel);
		panel.add(shipPanel);
		updateShips(player);

		/* Create the goods labels */
		Dimension gap = gui.getSize("GoodGap");
		JPanel goodsPanel = new JPanel(new GridLayout(GOODS_ROWS, GOODS_PER_ROW,G.Width(gap), G.Height(gap)));
		for(Good good: Good.values()) {
			if(!good.isBasic()) continue;
			label = new GoodLabel(control.network,language, new GoodMenu(control, good), good);
			labels.add(label);
			goodsPanel.add(label);
		}
		for(Good good: Good.values()) {
			if(!good.isBasic()) continue;
			label = new JLabel();
			label.setHorizontalAlignment(JLabel.CENTER);
			label.setFont(FONT15);
			labels.add(label);
			goodsPanel.add(label);
		}
		for(Good good: Good.values()) {
			if(!good.isProcessed()) continue;
			label = new GoodLabel(control.network,language, new GoodMenu(control, good), good);
			labels.add(label);
			goodsPanel.add(label);
		}
		for(Good good: Good.values()) {
			if(!good.isProcessed()) continue;
			label = new JLabel();
			label.setHorizontalAlignment(JLabel.CENTER);
			label.setFont(FONT15);
			labels.add(label);
			goodsPanel.add(label);
		}
		goodsPanel.setBounds(gui.getBounds("Good"));
		Insets in = gui.getPadding("Good");
		goodsPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLoweredBevelBorder(),
			BorderFactory.createEmptyBorder(in.top, in.left, in.bottom, in.right)
		));
		goodsPanel.setBackground(gui.getColor("Tab"));
		panel.add(goodsPanel);

		/* Draw buildings */
		gap = gui.getSize("Gap");
		JPanel buildingPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, gap.width, gap.height));
		buildingPanel.setOpaque(false);
		buildingPanel.setBounds(gui.getBounds("Building"));
		buildingPanels.add(buildingPanel);
		panel.add(buildingPanel);

		/* Create the scroll pane */
		JScrollPane scroll = new JScrollPane(panel);
		scroll.getVerticalScrollBar().setUnitIncrement(gui.getInt("ScrollUnit"));
		tabbedPane.addTab(player.getName(), scroll);
	}

	/**
	 *	Makes the tab of the given player visible.
	 *	Provide null to make the town tab visible.
	 *	@param player the player
	 */
	public void switchToPlayer(Player player) {
		int index = (player != null ? player.getSeat() : 0);
		tabbedPane.setSelectedIndex(index);
		((JScrollPane)tabbedPane.getComponent(index)).getViewport().setViewPosition(new Point(0,0));
	}

	/**
	 *	Dumps all graphic components.
	 *	@return the dump string
	 */
	public String dump() {
		StringBuilder ret = new StringBuilder();
		ret.append(getClass().getSimpleName());
		dump(getContentPane(), ret, 0);
		return ret.toString();
	}

	/**
	 *	Dumps the contents of the given component and appends them to the given string.
	 *	Expects an indent level as second parameter to dump inner components.
	 *	@param component the component
	 *	@param output the output string
	 *	@param indent the indent
	 */
	private static void dump(Component component, StringBuilder output, int indent) {
		output.append("\n");
		output.append(Util.repeat("\t", indent));
		output.append("> ");
		output.append(component);
		if(component instanceof Container) for(Component comp: ((Container)component).getComponents()) dump(comp, output, indent + 1);
	}
	@Override
	public HitPoint MouseMotion(int arg0, int arg1, MouseState arg2) {
		return null;
	}
	@Override
	public void StartDragging(HitPoint hp) {
		
	}
	@Override
	public void StopDragging(HitPoint hp) {
		
	}
	@Override
	public void setLocalBounds(int l, int t, int w, int h) {
		System.out.println("Resize "+l+" "+t+" "+w+" "+h);
		
	}
	@Override
	public void Pinch(int x, int y, double amount,double twist) {
		
	}
	@Override
	public void Wheel(int x, int y, int button,double amount) {
		
	}
 
    /** this is the place where the canvas is actually repainted.  We get here
     * from the event loop, not from the normal canvas repaint request.
     */
    public void drawCanvas(Graphics offGC, boolean complete,HitPoint hp)
    {
     	drawFixedElements(offGC,complete);
     	G.Error("not implemented");
    	// draw the board contents and changing elements.
        //redrawBoard(offGC,hp);
        //      draw clocks, sprites, and other ephemera
        //drawClocksAndMice(offGC, null);
       
        DrawArrow(offGC,hp);

  
    }
	@Override
	public void drawCanvasSprites(Graphics gc, HitPoint pt) {
		
	}
}