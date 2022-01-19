package lehavre.main;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import lehavre.util.GoodsList;
import lehavre.util.GoodsPair;
import lehavre.util.Util;
import lehavre.view.ChatWindow;
import lehavre.view.GUIHelper;
import lehavre.view.GoodsDialog;
import lehavre.view.InstructionsWindow;
import lehavre.view.LobbyWindow;
import lehavre.view.MainWindow;
import lehavre.view.ScoringWindow;
import lehavre.view.SettingsWindow;
import lehavre.model.Buildable;
import lehavre.model.GameState;
import lehavre.model.GameType;
import lehavre.model.Good;
import lehavre.model.Player;
import lehavre.model.PlayerColor;
import lehavre.model.Setup;
import lehavre.model.Ship;
import lehavre.model.buildings.Building;
import lehavre.model.buildings.Buildings;
import lehavre.model.buildings.special.ConstructionSite;
import lehavre.model.buildings.special.DiveBar;
import lehavre.model.buildings.special.HarbourWatch;
import lehavre.model.buildings.special.LumberMill;
import lehavre.model.buildings.special.LuxuryYacht;
import lehavre.model.buildings.special.MSDagmar;
import lehavre.model.buildings.special.MainStation;
import lehavre.model.buildings.special.MasonsGuild;
import lehavre.model.buildings.special.SouvenirShop;
import lehavre.model.buildings.special.Stockmarket;
import lehavre.model.buildings.special.WoodenCrane;
import lehavre.model.buildings.standard.Bank;
import lehavre.model.buildings.standard.Sawmill;

public class LeHavre implements LeHavreConstants 
{
	/* constructor */
	public NetworkInterface network ;	// the network widget
	
	//================================================================================================= CONSTRUCTOR

	public LeHavre(NetworkInterface net,String language,boolean log) throws IOException
	{	network = net;
		GUIHelper.getGuiData(net);			// read the static GUI description 
		dictionary = new Dictionary(network,language);
		logger = new Logger(log);
		game = new GameState();

	}
	//================================================================================================= CONSTANTS

	/** The dictionary. */
	private final Dictionary dictionary;

	/** The event logger. */
	private final Logger logger;

	/** The game state. */
	private final GameState game;
	public GameState getGame() { return(game); }

	/** The user who owns the user interface index. */
	private int myIndex;

	//================================================================================================= VARIABLES

	/** The main game window. */
	private MainWindow main;

	/** The main game window. */
	private LobbyWindow lobby;

	/** The chat window. */
	private ChatWindow chat;



	//================================================================================================= NETWORKING METHODS

	/**
	 *	Returns the current state, i. e. the list of users.
	 *	@return the current state
	 */
	public GameState getState() {
		return(game);
	}


	/**
	 *	Sets the state to the given new one.
	 *	@param gameState the new state
	 */
	public void setState(GameState gameState) {
			if(gameState != null) {
				String newVersion = GameState.VERSION;
				String oldVersion = GameState.VERSION;
				if(newVersion.equals(oldVersion)) game.restore(gameState, true);
				else showFatalError("VersionMismatch", newVersion, oldVersion);
			} else {
				showFatalError("RestoreFailed");
			}
	}



	/**
	 * Called when a message was received from another peer.
	 * @param src
	 * @param order
	 */
	public void receive(AddressInterface src,Order order) {
		//Address dest = message.getDest();
		//Address src = message.getSrc();
		List<? extends Serializable> args = order.getArguments();
		switch(order.getOrderId()) {
			/* Start next turn */
			case ORDER_SETSTATE:
				setState((GameState)args.get(0));
				break;
			case ORDER_NEXT_TURN:
				if((game.getTurn() % GameState.TURNS_PER_ROUND == 0) && !game.isRoundCompleted()) {
					game.setRoundCompleted();
					main.endRound();
				} else if(game.isEndgame() && game.getTurn() >= (game.getTurnCount() + game.getPlayerCount())) {
					stop();
				} else {
					main.nextTurn();
				}
				return;

			/* Restore previous state */
			case ORDER_RESTORE:
				GameState state = (GameState)args.get(0);
				boolean undo = (Boolean)args.get(1);
				if(state == null) {
					showFatalError("RestoreFailed");
					return;
				}
				synchronized(game) {
					game.restore(state, true);
				}
				updateMainWindow();
				if(undo) showMessage(get("logBack"));
				main.nextTurn();
				return;

			/* Enter a building */
			case ORDER_ENTER:
				Player player = game.getPlayer((Integer)args.get(0));
				String name = player.getName();
				Building building = Building.create((Building)args.get(1));
				int index = player.getIndex();
				// remove worker from old location and reactivate
				Building previous = player.getLocation();
				if(previous != null) {
					previous.removeWorker(index);
					main.activate(previous);
				}
				// add worker to new location
				building.addWorker(index);
				player.setLocation(building);
				showMessage(String.format(get("logBuildingUsed"), name, get("building" + building)));
				GoodsList goods = (GoodsList)args.get(2);
				// entry payment
				index = building.getOwner();
				Player owner = (index < 0 ? null : game.getPlayer(index));
				boolean notTown = (owner != null);
				if(goods != null && goods.size() > 0) {
					if(!isPlayer(player)) player.lose(goods);
					if(notTown) owner.receive(goods);
					String payment = Util.getColored(dictionary, goods);
					if(notTown) showMessage(String.format(get("logEntryPlayer"), name, owner.getName(), payment));
					else showMessage(String.format(get("logEntryTown"), name, payment));
					main.updatePlayer(player);
					if(notTown) main.updatePlayer(owner);
				}
				// Souvenir Shop code
				SouvenirShop souvenirShop = SouvenirShop.getInstance();
				if(notTown && !owner.equals(player) && owner.owns(souvenirShop)) {
					goods = new GoodsList();
					goods.add(souvenirShop.OUTPUT_FRANC, Good.Franc);
					owner.receive(goods);
					main.updatePlayer(owner);
					showMessage(String.format(get("logSouvenirShop"), owner.getName(), Util.getColored(dictionary, goods)));
				}
				return;

			/* Take good offer */
			case ORDER_TAKE_OFFER:
				player = game.getPlayer((Integer)args.get(0));
				Good good = (Good)args.get(1);
				goods = new GoodsList();
				goods.add(game.getOffer(good), good);
				player.receive(goods);
				showMessage(String.format(get("logOfferTaken"), player.getName(), Util.getColored(dictionary, goods)));
				main.takeOffer(good);
				// Stock Market code
				if(good.equals(Good.Franc)) {
					Stockmarket stockmarket = Stockmarket.getInstance();
					owner = null;
					for(Player p: game.getPlayers()) {
						if(p.owns(stockmarket)) {
							owner = p;
							break;
						}
					}
					// Stock Market owned
					if(owner != null && !owner.equals(player)) {
						goods = new GoodsList();
						goods.add(stockmarket.OUTPUT_FRANC, Good.Franc);
						owner.receive(goods);
						showMessage(String.format(get("logStockMarket"), owner.getName(), Util.getColored(dictionary, goods)));
					}
				}
				return;

			/* Receive or lose goods */
			case ORDER_RECEIVE:
				player = game.getPlayer((Integer)args.get(0));
				goods = (GoodsList)args.get(1);
				GoodsList plus = new GoodsList();
				GoodsList minus = new GoodsList();
				int value;
				synchronized(player) {
					for(GoodsPair pair: goods) {
						value = (int)pair.getAmount();
						good = pair.getGood();
						if(value < 0) {
							value = Math.abs(value);
							minus.add(value, good);
							player.lose(value, good);
						} else {
							plus.add(value, good);
							player.receive(value, good);
						}
					}
				}
				name = player.getName();
				if(minus.size() > 0) showMessage(String.format(get("logLose"), name, Util.getColored(dictionary, minus)));
				if(plus.size() > 0) showMessage(String.format(get("logReceive"), name, Util.getColored(dictionary, plus)));
				main.updatePlayer(player);
				return;

			/* Build something */
			case ORDER_BUILD:
				player = game.getPlayer((Integer)args.get(0));
				name = player.getName();
				Object object = args.get(1);
				Building location = Building.create((Building)args.get(2));
				minus = (GoodsList)args.get(3);	// entire payment
				goods = (GoodsList)args.get(4);	// energy payment
				String msg = (minus.size() > 0 ? Util.getColored(dictionary, minus) : get("free"));
				if(object instanceof Building) {
					building = Building.create((Building)object);
					if(building.isShip()) {
						boolean loseIron = building.equals(LuxuryYacht.getInstance());
						boolean loseLuxury = building.equals(MSDagmar.getInstance());
						for(Ship ship: player.getShips()) {
							if((loseIron && ship.isIron()) || (loseLuxury && ship.isLuxury())) {
								player.lose(ship);
								main.destroy(ship);
								break;
							}
						}
						if(loseLuxury) moderniseWharf(player, location);
						game.getTown().lose(building);
						showMessage(String.format(get("logChangeShip"), name, get("building" + building), building.getValue()));
					} else {
						building.setBuilt(true);
						player.lose(minus);
						game.remove(building);
						main.checkFootballStadium();
						showMessage(String.format(get("logItemBuilt"), name, msg, get("building" + building), building.getValue()));
						if(building.equals(Bank.getInstance())) performStockMarket(player);
					}
					player.receive(building);
					main.buy(building, false);
				} else if(object instanceof Ship) {
					Ship ship = (Ship)object;
					if(!ship.isWooden()) moderniseWharf(player, location);
					player.lose(minus);
					game.remove(ship);
					player.receive(ship);
					showMessage(String.format(get("logItemBuilt"), name, msg, get("ship" + ship.getType()), ship.getValue()));
					main.buy(ship, false);
				}
				if(isPlayer(player)) main.enableDone(true);
				if(goods != null) showMessage(String.format(get("logEnergy"), name, Util.getColored(dictionary, goods)));
				return;

			/* Buy something */
			case ORDER_BUY:
				player = game.getPlayer((Integer)args.get(0));
				name = player.getName();
				object = args.get(1);
				boolean isTown = false;
				if(object instanceof Building) {
					building = Building.create((Building)object);
					if(!building.isBuilt()) {
						building.setBuilt(true);
						if(building.equals(Bank.getInstance())) performStockMarket(player);
					}
					if(game.getTown().owns(building)) {
						game.getTown().lose(building);
						isTown = true;
					} else {
						game.remove(building);
						main.checkFootballStadium();
					}
					player.receive(building);
					int n = building.getPrice();
					player.lose(n, Good.Franc);
					showMessage(String.format(
						get("logItemBought"),
						name,
						Util.getColored(dictionary, new GoodsPair(n, Good.Franc)),
						get("building" + building),
						building.getValue()
					));
					freeBuilding(building);	// does nothing if ban not allowed
					main.buy(building, isTown);
				} else if(object instanceof Ship) {
					Ship ship = (Ship)object;
					if(game.getTown().owns(ship)) {
						game.getTown().lose(ship);
						isTown = true;
					} else game.remove(ship);
					player.receive(ship);
					int n = ship.getPrice();
					player.lose(n, Good.Franc);
					showMessage(String.format(
						get("logItemBought"),
						name,
						Util.getColored(dictionary, new GoodsPair(n, Good.Franc)),
						get("ship" + ship.getType()), ship.getValue()
					));
					main.buy(ship, isTown);
				}
				if(isPlayer(player) && isPlayer(game.getActivePlayer())) main.enableBack(true);
				return;

			/* Sell something */
			case ORDER_SELL:
				player = game.getPlayer((Integer)args.get(0));
				name = player.getName();
				object = args.get(1);
				if(object instanceof Building) {
					building = Building.create((Building)object);
					player.lose(building);
					game.getTown().receive(building);
					int n = building.getValue() / 2;
					player.receive(n, Good.Franc);
					showMessage(String.format(
						get("logItemSold"),
						name,
						Util.getColored(dictionary, new GoodsPair(n, Good.Franc)),
						get("building" + building),
						2 * n
					));
					freeBuilding(building);	// does nothing if ban not allowed
					main.sell(building, player);
				} else if(object instanceof Ship) {
					Ship ship = (Ship)object;
					player.lose(ship);
					game.addShip(ship);
					int n = ship.getValue() / 2;
					player.receive(n, Good.Franc);
					showMessage(String.format(
						get("logItemSold"),
						name,
						Util.getColored(dictionary, new GoodsPair(n, Good.Franc)),
						get("ship" + ship.getType()),
						2 * n
					));
					main.sell(ship, player);
				}
				if(isPlayer(player) && isPlayer(game.getActivePlayer())) main.enableBack(true);
				return;

			/* Finalize round */
			case ORDER_ROUND_END:
				synchronized(this) {
					player = game.getPlayer((Integer)args.get(0));
					goods = (GoodsList)args.get(1);
					name = player.getName();
					player.setReady(true);
					if(goods.size() > 0) {
						player.lose(goods);
						takeLoansIfNecessary(player);
						showMessage(String.format(get("logPayment"), name, Util.getColored(dictionary, goods)));
					} else showMessage(String.format(get("logFoodSupplied"), name));
					for(Player p: game.getPlayers()) {
						boolean ready = false;
						ready = p.isReady();
						if(!ready) return;
					}
					main.nextRound();
				}
				return;

			/* Show the food demand */
			case ORDER_TELL_FOOD:
				player = game.getPlayer((Integer)args.get(0));
				value = (Integer)args.get(1);
				showMessage(String.format(get("logFoodNeeded"), player.getName(), Util.getColored(dictionary, new GoodsPair(value, Good.Food))));
				return;

			/* Pay interest */
			case ORDER_INTEREST:
				player = game.getPlayer((Integer)args.get(0));
				synchronized(player) {
					player.setReady(true);
					int amount = player.getLoans();
					if(amount > 0) {
						value = game.getInterest(player);
						player.lose(value, Good.Franc);
						takeLoansIfNecessary(player);
						showMessage(String.format(get("logInterest"), player.getName(), Util.getColored(dictionary, new GoodsPair(value, Good.Franc))));
						main.updatePlayer(player);
					}
					for(Player p: game.getPlayers()) if(!p.isReady()) return;
					main.continueTurn();
				}
				return;

			/* Show the energy payment */
			case ORDER_TELL_ENERGY:
				player = game.getPlayer((Integer)args.get(0));
				String text = (String)args.get(1);
				showMessage(String.format(get("logEnergy"), player.getName(), text));
				return;

			/*  Swaps special buildings */
			case ORDER_SWAP:
				player = game.getPlayer((Integer)args.get(0));
				game.swapSpecials();
				showMessage(String.format(get("logSwapSpecials"), player.getName()));
				return;

			/* Pay back loans */
			case ORDER_PAYBACK_LOANS:
				player = game.getPlayer((Integer)args.get(0));
				value = (Integer)args.get(1);
				boolean free = (Boolean)args.get(2);
				paybackLoans(player, value, free);
				return;

			/* Pay back loans */
			case ORDER_TAKE_LOANS:
				player = game.getPlayer((Integer)args.get(0));
				value = (Integer)args.get(1);
				boolean compensate = (Boolean)args.get(2);
				takeLoans(player, value, compensate);
				return;

			/* Chat message received */
			case ORDER_CHAT:
				player = game.getPlayer((Integer)args.get(0));
				showMessage((String)args.get(1), false, player.getName(), getSelf().equals(src));
				return;

			/* Ban a player from a building */
			case ORDER_BAN:
				owner = game.getPlayer((Integer)args.get(0));
				building = Building.create((Building)args.get(1));
				value = (Integer)args.get(2);
				if(building == null || !building.isOccupied()) {
					showFatalError("BanUnoccupied", building);
					return;
				}
				name = owner.getName();
				ArrayList<Integer> workers = building.getWorkers();
				for(Integer worker: workers) {
					player = game.getPlayer(worker);
					if(value > 0) {
						synchronized(owner) {
							owner.lose(value, Good.Franc);
						}
						synchronized(player) {
							player.receive(value, Good.Franc);
						}
						showMessage(String.format(get("logTransfer"), name, player.getName(), Util.getColored(dictionary, new GoodsPair(value, Good.Franc))));
					}
					showMessage(String.format(get("logBuildingBan"), name, player.getName(), get("building" + building), building.getValue()));
				}
				freeBuilding(building);
				main.activate(building);
				return;

			/* Pay the pirate's duty */
			case ORDER_PIRATE:
				player = game.getPlayer((Integer)args.get(0));
				Player pirate = game.getPlayer((Integer)args.get(1));
				value = (Integer)args.get(2);
				synchronized(player) {
					player.lose(value, Good.Franc);
				}
				synchronized(pirate) {
					pirate.receive(value, Good.Franc);
				}
				showMessage(String.format(get("logTransfer"), player.getName(), pirate.getName(), Util.getColored(dictionary, new GoodsPair(value, Good.Franc))));
				main.updatePlayer(player);
				main.updatePlayer(pirate);
				return;

			/* Choose a special building */
			case ORDER_CHOOSE:
				player = game.getPlayer((Integer)args.get(0));
				building = Building.create((Building)args.get(1));
				building.setBuilt(true);
				game.getTown().receive(building);
				main.moveBuilding(building, null);
				showMessage(String.format(get("logBuildingChoose"), player.getName(), get("building" + building), building.getValue()));
				if(isPlayer(player)) {
					main.enablePurchase(building, true);
					if(building.isUsable(this)) {
						synchronized(player) {
							player.setActions(player.getActions() + 1);
						}
						main.activate(building);
						showMessage(get("errMustEnter"));
						showWarning("MustEnter");
					} else main.enableDone(true);
				}
				for(Player p: game.getPlayers()) p.setHandCards(null);
				remove(ConstructionSite.getInstance());
				main.enableBack(true);
				return;

			/* Removes a building */
			case ORDER_REMOVE:
				remove(Building.create((Building)args.get(0)));
				return;

			/* Updates a building */
			case ORDER_UPDATE:
				building = (Building)args.get(0);
				if(building == null) {
					showFatalError("BuildingUpdate");
					return;
				}
				Building proto = Building.create(building);
				synchronized(proto) {
					proto.restore(building);
				}
				return;

			/* New user logs in */
			case ORDER_LOGIN:
				int id = (Integer)args.get(0);
				name = (String)args.get(1);
				boolean me = (myIndex == id);
				if(me) {
					createChatWindow(name);
					lobby = new LobbyWindow(this);
					lobby.setTitle(Util.getTitle(dictionary, "lobby", name, src));
					lobby.setVisible(true);
					for(Player p: game.getPlayers()) lobby.showPlayer(p);
				}
				player = new Player(src, id, name);
				if(me && isServer()) player.setReady(true);
				synchronized(game) {
					game.addPlayer(player);
				}
				lobby.showPlayer(player);
				showMessage(String.format(get("logUserJoined"), name));
				return;

			/* New color */
			case ORDER_COLOR:
				id = (Integer)args.get(0);
				player = game.getPlayer(id);
				value = (Integer)args.get(1);
				if(player != null) player.setColor(PlayerColor.values()[value]);
				lobby.setColor(id, value);
				return;

			/* New seat */
			case ORDER_SEAT:
				id = (Integer)args.get(0);
				player = game.getPlayer(id);
				value = (Integer)args.get(1);
				if(player != null) player.setSeat(value);
				if(!isServer()) lobby.setSeat(id, value);
				return;

			/* Bonus goods */
			case ORDER_BONUS:
				player = game.getPlayer((Integer)args.get(0));
				goods = (GoodsList)args.get(1);
				GoodsList list = new GoodsList();
				for(GoodsPair pair: goods) {
					good = pair.getGood();
					value = (int)pair.getAmount();
					player.setGood(good, value > 0 ? value : -1);
					if(value >= 0) list.add(value, good);
				}
				showMessage(String.format(get("logBonus"), player.getName(), Util.getColored(dictionary, list)));
				return;

			/* Settings changed */
			case ORDER_SETTINGS:
				game.setChanges((Settings)args.get(0));
				showMessage(get("logSettings"));
				if(!isServer()) {
					SettingsWindow win = new SettingsWindow(this);
					win.read(game.getChanges());
				}
				return;

			/* Ready to start */
			case ORDER_READY:
				player = game.getPlayer((Integer)args.get(0));
				synchronized(player) {
					if(!player.isReady()) player.setReady(true);
				}
				showMessage(String.format(get("logUserReady"), player.getName()));
				return;

			/* Update game state */
			case ORDER_START:
				if(!isServer()) this.game.restore((GameState)args.get(0), true);
				showMessage(String.format(get("logGameStart"), get(game.getGameType().toString())));
				text = (String)args.get(1);
				if(text != null) showMessage(String.format(get("logPlayOrder"), text));
				lobby.setVisible(false);
				lobby.dispose();
				lobby = null;
				createMainWindow();
				if(isServer()) nextTurn();
				return;

			/* User logs in again */
			case ORDER_RELOGIN:
				index = (Integer)args.get(0);
				synchronized(game) {
					player = game.getPlayer(index);
					player.setAddress(src);
					name = player.getName();
				}
				if(src.equals(getSelf())) {
					myIndex = index;
					createChatWindow(name);
					createMainWindow();
					main.updateRoundCard();
					restore(false);
				}
				showMessage(String.format(get("logRelogin"), name));
				return;

			/* Display the game state */
			case ORDER_DUMP:
				player = game.getPlayer((Integer)args.get(0));
				showMessage(String.format(get("logDump"), player.getName()));
				StringBuilder dump = new StringBuilder(game.dump());
				for(Buildings bld: Buildings.values()) {
					dump.append("\n");
					dump.append(Building.create(bld).dump());
				}
				for(Ship ship: Ship.values()) {
					dump.append("\n");
					dump.append(ship.dump());
				}
				dump.append("\n");
				dump.append(main.dump());
				JFrame win = new InstructionsWindow(String.format(get("dumpDescr"), dump));
				win.setTitle(get("dumpTitle"));
				win.setVisible(true);
				return;
		default:
			break;
		}
	}

	/**
	 *	Sends the given message to the given recipient.
	 *	@param recipient the recipient
	 *	@param order the order
	 *	@param args the arguments
	 */
	public void send(AddressInterface recipient, int order, Serializable... args) {
		try {
			network.send(recipient,  new Order(order, Arrays.asList(args)));
		} catch(Exception e) {
			handleException(e);
		}
	}

	/**
	 *	Returns the own address.
	 *	@return the own address
	 */
	private AddressInterface getSelf() {
		return network.getSelf();
	}

	/**
	 *	Returns the creator's address
	 *	@return creator's address
	 */
	private AddressInterface getCreator() {
		return network.getCreator();
	}

	/**
	 *	Returns true if you are the game master.
	 *	@return true if you are the game master
	 */
	public boolean isServer() {
		return getCreator().equals(getSelf());
	}

	/**
	 *	Returns true if you are the given player.
	 *	@param player the player
	 *	@return true if you are the given player
	 */
	public boolean isPlayer(Player player) {
		return (player != null ? (network.isStandaloneGame() || (player.getIndex() == myIndex)) : false);
	}

	/**
	 *	Creates the chat window.
	 *	@param name the player name
	 */
	private void createChatWindow(String name) {
		chat = new ChatWindow(this);
		chat.setTitle(Util.getTitle(dictionary, "chat", name, getSelf()));
		chat.setVisible(true);
		showMessage(get("chatIntro"));
	}

	/**
	 *	Creates the main window.
	 */
	private void createMainWindow() {
		main = new MainWindow(network,this);
		main.setTitle(Util.getTitle(dictionary, "main", getUIPlayer()));
		main.setVisible(true);
	}

	/**
	 *	Updates the main window.
	 */
	private void updateMainWindow() {
		main.updateOffer();
		main.updateBuildingStacks();
		main.updateShipStacks();
		main.updateTown(false);
		for(Player player: game.getPlayers()) {
			main.updateBuildings(player);
			main.updateShips(player);
			main.updatePlayer(player);
		}
	}

	/**
	 *	Sets the color for the player with the given index.
	 *	The color is parametrized by the enum ordinal value.
	 *	@param index the index
	 *	@param color the color
	 */
	public void setColor(int index, int color) {
		send(null, ORDER_COLOR, index, color);
	}

	/**
	 *	Sets the seat for the player with the given index.
	 *	@param index the index
	 *	@param seat the seat
	 */
	public void setSeat(int index, int seat) {
		send(null, ORDER_SEAT, index, seat);
	}

	/**
	 *	Sets the bonus goods for the player with the given index.
	 *	@param index the index
	 *	@param goods the goods list
	 */
	public void setBonus(int index, GoodsList goods) {
		if(game.isRunning()) return;
		send(null, ORDER_BONUS, index, goods);
	}

	/**
	 *	Applies the given settings and notifies all players.
	 *	@param settings the settings
	 */
	public void setChanges(Settings settings) {
		if(!game.isRunning()) send(null, ORDER_SETTINGS, settings);
	}

	/**
	 *	Notifies the server being ready to start the game.
	 *	@param button the 'ready' button
	 */
	public void setReady(JButton button) {
		button.removeActionListener(button.getActionListeners()[0]);
		button.setEnabled(false);
		send(null, ORDER_READY, myIndex);
	}

	/**
	 *	Starts the game if all players are ready and the player
	 *	colors are set correctly (i. e. no duplicates).
	 *	@param button the 'start' button
	 *	@param gameType the type of game
	 */
	public boolean start(JButton button, GameType gameType) {
		ArrayList<Player> players = game.getPlayers();

		/* Check if all players ready */
		boolean OK = true;
		for(Player player: players) {
			if(!player.isReady()) {
				OK = false;
				break;
			}
		}
		if(!OK) {
			showError("NotReady");
			return false;
		}

		/* Check if colors properly set */
		ArrayList<PlayerColor> colors = new ArrayList<PlayerColor>();
		PlayerColor color;
		OK = true;
		for(Player player: players) {
			color = player.getColor();
			if(colors.contains(color)) {
				OK = false;
				break;
			}
			colors.add(color);
		}
		if(!OK) {
			showError("SameColors");
			return false;
		}

		/* Disable button */
		button.removeActionListener(button.getActionListeners()[0]);
		button.setEnabled(false);

		/* Set game type */
		game.setGameType(gameType);

		/* Set the initial goods */
		GoodsList list = Setup.getPlayerGoods(gameType);
		for(Player player: players) {
			for(GoodsPair pair: list) if(player.getGood(pair.getGood()) == 0) player.setGood(pair.getGood(), (int)pair.getAmount());
			for(Good good: Good.values()) if(player.getGood(good) < 0) player.setGood(good, 0);
		}

		/* Determine play order */
		int seat, max = 0;
		ArrayList<Integer> pos = new ArrayList<Integer>();
		for(int i = 0; i < players.size(); i++) pos.add(i + 1);
		Hashtable<Integer, ArrayList<Player>> seats = new Hashtable<Integer, ArrayList<Player>>();
		for(int i = 0; i < players.size();) {
			seat = players.get(i).getSeat();
			if(seat > 0) {
				if(seats.get(seat) == null) seats.put(seat, new ArrayList<Player>());
				seats.get(seat).add(players.remove(i));
				pos.remove(Integer.valueOf(seat));
				if(seat > max) max = seat;
			} else {
				i++;
			}
		}
		if(players.size() > 0) {
			for(Player p: players) {
				seat = pos.remove(Util.random(0, pos.size()));
				seats.put(seat, new ArrayList<Player>());
				seats.get(seat).add(p);
				if(seat > max) max = seat;
			}
		}
		pos.clear();
		seat = 0;
		Player player;
		StringBuilder msg = new StringBuilder();
		for(int i = 1; i <= max; i++) {
			players = seats.get(Integer.valueOf(i));
			if(players == null) continue;
			while(players.size() > 0) {
				player = players.remove(Util.random(0, players.size()));
				player.setSeat(++seat);
				pos.add(player.getIndex());
				if(msg.length() > 0) msg.append(", ");
				msg.append(String.format("%d. %s (%s)", seat, player.getName(), get("color" + player.getColor())));
			}
		}
		game.setOrder(pos);

		/* Set-up the game and send it to all peers */
		if(game.setup()) {
			send(null, ORDER_START, game, pos.size() > 1 ? msg.toString() : null);
			return true;
		}
		return false;
	}

	/**
	 *	The game is finished.
	 *	Displays the scoring window.
	 */
	public void stop() {
		game.setOver();
		log(get("logGameOver"));

		/* Pay back loans automatically */
		int n;
		for(Player player: game.getPlayers()) {
			n = Math.min(player.getLoans(), player.getMoney() / GameState.LOAN_PAYBACK);
			if(n > 0) paybackLoans(player, n, false);
		}

		/* Show scoring window */
		main.freeze();
		new ScoringWindow(this);
	}

	//================================================================================================= GAME METHODS

	/**
	 *	Starts the next turn.
	 */
	public void nextTurn() {
		send(null, ORDER_NEXT_TURN);
	}

	/**
	 *	Restores to the previous game state.
	 *	@param undo	provide true if this was caused by an undo action
	 */
	public void restore(boolean undo) {
		send(null, ORDER_RESTORE, game.getPrevious(), undo);
	}

	/**
	 *	Updates the given building for all players.
	 *	@param building the building to update
	 */
	public void updateBuilding(Building building) {
		send(null, ORDER_UPDATE, building);
	}

	/**
	 *	Pays the interest.
	 */
	public void payInterest() {
		send(null, ORDER_INTEREST, getCurrentPlayer().getIndex());
	}

	/**
	 *	Pays the pirate the given duty.
	 *	@param pirate the pirate's index
	 *	@param duty the duty paid
	 */
	public void payDuty(int pirate, int duty) {
		send(null, ORDER_PIRATE, getCurrentPlayer().getIndex(), pirate, duty);
	}

	/**
	 *	Finishes the current round. Notifies all players
	 *	about the goods paid by this player.
	 *	@param goods the goods list
	 */
	public void endRound(GoodsList goods) {
		if(goods == null) goods = new GoodsList();
		send(null, ORDER_ROUND_END, getCurrentPlayer().getIndex(), goods);
	}

	/**
	 *	Let the player lose all of their food and pay
	 *	the rest of the given food demand with money.
	 *	@param demand the food demand
	 */
	public void loseAllFood(int demand) {
		Player player = getCurrentPlayer();
		GoodsList payment = new GoodsList();
		int amount, food = 0;
		for(Good good: Good.values()) {
			if(good.getFoodValue() > 0 && !good.isMoney()) {
				amount = player.getGood(good);
				if(amount > 0) payment.add(amount, good);
				food += amount * good.getFoodValue();
			}
		}
		amount = demand - food;
		if(amount > 0) payment.add(amount, Good.Franc);
		endRound(payment);
	}

	/**
	 *	Tell all players how many food you need
	 *	to pay at the end of the current round.
	 */
	public void tellFoodDemand(int demand) {
		send(null, ORDER_TELL_FOOD, getCurrentPlayer().getIndex(), demand);
	}

	/**
	 *	Tell all players how you've paid the
	 *	current energy demand.
	 */
	public void tellEnergyPayment(GoodsList payment) {
		send(null, ORDER_TELL_ENERGY, getCurrentPlayer().getIndex(), Util.getColored(dictionary, payment));
	}

	/**
	 *	The active player takes the given good offer.
	 *	@param good the good offer
	 */
	public void takeOffer(Good good) {
		send(null, ORDER_TAKE_OFFER, getCurrentPlayer().getIndex(), good);
	}

	/**
	 *	The active player tries to enter the given building.
	 *	@param building the building
	 */
	public void enter(Building building) {
		if(!building.isEnterable()) {
			showWarning("NotEnterable");
			return;
		}
		if(!building.isUsable(this)) {
			showWarning("NotUsable");
			return;
		}
		Player player = getCurrentPlayer();
		main.switchToPlayer(player);
		Building location = player.getLocation();
		GoodsList payment = null;

		/* Main Station code */
		MainStation mainStation = MainStation.getInstance();
		if(mainStation.equals(location)) {
			synchronized(player) {
				player.setActions(2);
				player.setOfferAllowed(false);
				player.setBuildingAllowed(true);
			}
			mainStation.setActive(true);
			// NOTE: one more line of code in MainWindow.java (continue button)
		}

		/* Pay entry fee */
		HarbourWatch harbourWatch = HarbourWatch.getInstance();
		if(harbourWatch.equals(location) || !mainStation.isActive()) {
			payment = GoodsDialog.showEntryDialog(this, building);
			if(payment == null) {
				showWarning("NotPayable");
				return;
			}
			player.lose(payment);
			if(!building.isUsable(this)) {
				player.receive(payment);
				showWarning("NotUsableAfterEntry");
				return;
			}
		}
		reduceActions();
		send(null, ORDER_ENTER, player.getIndex(), building, payment);
		boolean done = building.use(this);
		main.disableAll();
		if(done) main.enableDone(true);
	}

	/**
	 *	Reduces the amount of main actions for the active player.
	 */
	public void reduceActions() {
		Player player = getCurrentPlayer();
		synchronized(player) {
			player.setActions(player.getActions() - 1);
		}
	}

	/**
	 *	The active player receives the goods in the given list.
	 *	@param goods the goods
	 */
	public void receive(GoodsList goods) {
		receive(game.getActivePlayer(), goods);
	}

	/**
	 *	The given player receives the goods in the given list.
	 *	@param player the player
	 *	@param goods the goods
	 */
	public void receive(Player player, GoodsList goods) {
		send(null, ORDER_RECEIVE, player.getIndex(), goods);
	}

	/**
	 *	The active player bans the current worker from the given building
	 *	and pays the given penalty payment to the banned player.
	 *	@param building the building
	 *	@param payment the payment
	 */
	public void ban(Building building, int payment) {
		send(null, ORDER_BAN, getCurrentPlayer().getIndex(), building, payment);
	}

	/**
	 *	The active player chooses the given building to come into play
	 *	by entering the construction site.
	 *	@param building the building
	 */
	public void choose(Building building) {
		send(null, ORDER_CHOOSE, getCurrentPlayer().getIndex(), building);
	}

	/**
	 *	Lets all peers remove the given building from play.
	 *	@param building the building
	 */
	public void removeBuilding(Building building) {
		send(null, ORDER_REMOVE, building);
	}

	/**
	 *	Removes the given building from play.
	 *	@building the building
	 */
	private void remove(Building building) {
		freeBuilding(building);
		int index = building.getOwner();
		if(index < 0) game.getTown().lose(building);
		else game.getPlayer(index).lose(building);
		main.destroy(building);
		showMessage(String.format(get("logBuildingRemoved"), get("building" + building), building.getValue()));
	}

	/**
	 *	The active player tries to buy the given object.
	 *	@param object the object
	 */
	public void buy(Serializable object) {
		Player player = getCurrentPlayer();
		int price = 0;
		if(object instanceof Building) price = ((Building)object).getPrice();
		else if(object instanceof Ship) price = ((Ship)object).getPrice();
		int money = player.getMoney();
		if(money < price) showError2(String.format(
			get("errTooExpensive"),
			Util.getColored(dictionary, new GoodsPair(price, Good.Franc)),
			Util.getColored(dictionary, new GoodsPair(money, Good.Franc))
		));
		else send(null, ORDER_BUY, getCurrentPlayer().getIndex(), object);
	}

	/**
	 *	The active player sells the given object.
	 *	@param object the object
	 */
	public void sell(Serializable object) {
		send(null, ORDER_SELL, getCurrentPlayer().getIndex(), object);
	}

	/**
	 *	The active player tries to build the given object.
	 *	@param object the object
	 */
	public void build(Buildable object) {
		Player player = game.getActivePlayer();
		player.setBuilds(player.getBuilds() - 1);
		GoodsList costs = object.getCosts();
		GoodsList payment = new GoodsList();
		GoodsList energy = null;

		/* compute payment */
		if(costs != null) {
			/* a building has been built */
			if(object instanceof Building) {
				/* compute payment */
				boolean sawmill = (player.getLocation() instanceof Sawmill);
				boolean sawmillUsed = false;
				boolean lumberMill = player.owns(LumberMill.getInstance());
				boolean lumberMillUsed = false;
				boolean guild = player.owns(MasonsGuild.getInstance());
				boolean clay, iron;
				int amount, value;
				for(GoodsPair pair: costs) {
					Good good = pair.getGood();
					value = (int)pair.getAmount();
					amount = player.getGood(good);
					clay = (good.equals(Good.Clay) || good.equals(Good.Brick));
					iron = (good.equals(Good.Iron) || good.equals(Good.Steel));
					if(good.equals(Good.Wood)) {
						if(sawmill) {
							sawmillUsed = (value > 0);
							value -= Sawmill.REDUCTION;
						}
						if(lumberMill) {
							lumberMillUsed = (value > 0);
							value -= LumberMill.REDUCTION;
						}
					} else if(guild && clay) value -= MasonsGuild.REDUCTION;
					if(value > amount) {
						if(clay) {
							payment.add(value - amount, Good.Brick);
							value = amount;
						} else if(iron) {
							payment.add(value - amount, Good.Steel);
							value = amount;
						}
					}
					if(value > 0) payment.add(value, good);
				}
				payment.optimize();

				/* code for Wooden Crane */
				if(player.owns(WoodenCrane.getInstance())) {
					// check for surplus wood
					player.lose(payment);
					boolean potWood = ((sawmill && !sawmillUsed) || (lumberMill && !lumberMillUsed));
					boolean wood = (potWood || player.getGood(Good.Wood) > 0);
					Good chosenGood = null;

					// check for negatives
					boolean negative = false;
					for(Good good: Good.values()) {
						if(good.isMoney() || !good.isPhysical()) continue;
						amount = player.getGood(good);
						if(amount < 0) {
							if(wood && !negative && amount == -1) chosenGood = good;
							else throw new IllegalStateException(String.format("[Building] Fatal error: can't pay %d %s", amount, good));
							negative = true;
						}
					}
					player.receive(payment);

					// let player choose reduction
					if(wood && !negative) {
						ArrayList<Good> options = new ArrayList<Good>();
						for(GoodsPair pair: costs) {
							Good good = pair.getGood();
							if(!good.equals(Good.Wood)) options.add(good);
						}
						if(options.size() > 0) chosenGood = performWoodenCrane(player, options);
					}

					// adjust payment
					if(chosenGood != null) {
						payment.add(-1, chosenGood);
						if(!potWood) payment.add(1, Good.Wood);
						payment.optimize();
					}
				}

			/* a ship has been built */
			} else if(object instanceof Ship) {
				// get energy payment (quick & dirty!)
				player.lose(costs);
				for(GoodsPair pair: costs) if(pair.getGood().equals(Good.Energy)) energy = GoodsDialog.showEnergyDialog(this, pair.getAmount());
				player.receive(costs);
				payment.addAll(energy);

				// pay remaining resources
				int amount, value;
				Good good;
				for(GoodsPair pair: object.getCosts()) {
					good = pair.getGood();
					if(good.equals(Good.Energy)) continue;	// already paid
					value = (int)pair.getAmount();
					amount = player.getGood(good);
					if(value > amount) {
						if(good.equals(Good.Iron)) {
							payment.add(value - amount, Good.Steel);
							value = amount;
						} else throw new IllegalStateException(String.format("[Ship] Fatal error: %d %s needed, found: %d", value, good, amount));
					}
					payment.add(value, good);
				}
				payment.optimize();
			}
		}
		send(null, ORDER_BUILD, getCurrentPlayer().getIndex(), object, player.getLocation(), payment, energy);
	}

	/**
	 *	Asks the given player to choose one good from the given list.
	 *	This good will be replaced with wood during construction.
	 *	@param player the player
	 *	@param goods the list of goods
	 *	@return the chosen good
	 */
	private Good performWoodenCrane(Player player, ArrayList<Good> goods) {
		int size = goods.size();
		String[] options = new String[size];
		for(int i = 0; i < size; i++) options[i] = get("good" + goods.get(i));
		String choice = (String)JOptionPane.showInputDialog(
			null,
			Util.format(get("popupWoodenCrane")),
			get("building" + player.getLocation()),
			JOptionPane.QUESTION_MESSAGE,
			null,
			options,
			options[0]
		);
		if(choice == null) return null;
		int index = 0;
		for(String option: options) {
			if(choice.equals(option)) break;
			index++;
		}
		return goods.get(index);
	}

	/**
	 *	Once the Bank has been built or bought for the first time,
	 *	this method awards the owner of the Stock Market.
	 *	@param player the player who has triggered the event
	 */
	private void performStockMarket(Player player) {
		Stockmarket stockmarket = Stockmarket.getInstance();
		Player owner = null;
		for(Player p: game.getPlayers()) {
			if(p.owns(stockmarket)) {
				owner = p;
				break;
			}
		}
		// Stock Market owned
		if(owner != null && !owner.equals(player)) {
			GoodsList goods = new GoodsList();
			goods.add(stockmarket.OUTPUT_BANK, Good.Franc);
			owner.receive(goods);
			showMessage(String.format(get("logStockMarket"), owner.getName(), Util.getColored(getDictionary(), goods)));
		}
	}

	/**
	 *	Takes as many loans as needed to pay
	 *	the debts of the given player.
	 *	@param player the player
	 */
	public void takeLoansIfNecessary(Player player) {
		double debt = -player.getMoney();
		if(debt > 0) takeLoans(player, (int)Math.ceil(debt / GameState.LOAN_VALUE), true);
	}

	/**
	 *	The active player takes the given number of loans.
	 *	@param amount the number of loans
	 *	@param compensate provide true if there is compensation
	 *						for taking these loans
	 */
	public void takeLoans(int amount, boolean compensate) {
		send(null, ORDER_TAKE_LOANS, getCurrentPlayer().getIndex(), amount, compensate);
	}

	/**
	 *	The given player pays back the given number of loans.
	 *	@param player the player
	 *	@param amount the number of loans
	 *	@param compensate provide true if there is compensation
	 *						for taking these loans
	 */
	private void takeLoans(Player player, int amount, boolean compensate) {
		synchronized(player) {
			for(int i = 0; i < amount; i++) {
				player.takeLoan();
				if(compensate) player.receive(GameState.LOAN_VALUE, Good.Franc);
			}
		}
		showMessage(String.format(get("logTakeLoan"), player.getName(), Util.getNumbered(dictionary, amount, "loan")));
		main.updatePlayer(player);
	}

	/**
	 *	The active player pays back the given number of loans.
	 *	@param amount the number of loans
	 *	@param free provide true if the payback is free
	 */
	public void paybackLoans(int amount, boolean free) {
		send(null, ORDER_PAYBACK_LOANS, getCurrentPlayer().getIndex(), amount, free);
	}

	/**
	 *	The given player pays back the given number of loans.
	 *	@param player the player
	 *	@param amount the number of loans
	 *	@param free provide true if the payback is free
	 */
	private void paybackLoans(Player player, int amount0, boolean free) {
		int amount = amount0;
		synchronized(player) {
			amount = Math.min(amount, player.getLoans());
			for(int i = 0; i < amount; i++) {
				player.returnLoan();
				if(!free) player.lose(GameState.LOAN_PAYBACK, Good.Franc);
			}
		}
		showMessage(String.format(get("logPayback"), player.getName(), Util.getNumbered(dictionary, amount, "loan")));
		main.updatePlayer(player);
	}

	/**
	 *	Swaps the two topmost special buildings.
	 *	Informs all peers about this change.
	 */
	public void swapSpecials() {
		send(null, ORDER_SWAP, getCurrentPlayer().getIndex());
	}

	/**
	 *	The given player modernises the given wharf.
	 *	Does nothing if the wharf needs not to be or
	 *	is already modernised.
	 *	@param player the player
	 *	@param wharf the wharf
	 */
	private void moderniseWharf(Player player, Building wharf) {
		if(!wharf.isModernised()) {
			GoodsList payment = new GoodsList();
			payment.add(1, Good.Brick);
			player.lose(payment);
			wharf.setModernised(true);
			showMessage(String.format(get("logModernise"), player.getName(), Util.getColored(dictionary, payment)));
		}
	}

	/**
	 *	Sends the worker of the given building
	 *	to the dive bar if it is in play.
	 *	@param building the building
	 */
	private void freeBuilding(Building building) {
		if(!building.isBanAllowed()) return;
		DiveBar diveBar = DiveBar.getInstance();
		Player player;
		for(Integer index: building.getWorkers()) {
			player = game.getPlayer(index);
			if(!building.equals(diveBar)) {
				player.setLocation(null);
				building.removeWorker(index);
			}
			// check for dive bar
			if(diveBar.isBuilt()) {
				int fee = diveBar.getFee(player);
				diveBar.addWorker(index);
				synchronized(player) {
					player.setLocation(diveBar);
					player.lose(fee, Good.Franc);
				}
				showMessage(String.format(get("logDiveBar"), player.getName(), Util.getColored(dictionary, new GoodsPair(fee, Good.Franc))));
				// TODO: let player sell stuff first
				takeLoansIfNecessary(player);
			}
		}
	}

	//================================================================================================= UTILITY METHODS

	/**
	 *	Re-opens the chat window if closed.
	 */
	public void openChat() {
		if(!chat.isVisible()) {
			chat.setVisible(true);
			if(main != null) main.chatChanged();
			else if(lobby != null) lobby.chatChanged();
		}
	}

	/**
	 *	Called when the chat window has been closed.
	 */
	public void chatClosed() {
		if(main != null) main.chatChanged();
		else if(lobby != null) lobby.chatChanged();
	}

	/**
	 *	Returns true if the chat window is open.
	 *	@return true if the chat window is open
	 */
	public boolean isChatOpen() {
		return chat.isVisible();
	}

	/**
	 *	Sends the given message to all players.
	 *	This message will appear in the chat window.
	 *	@param message the message
	 */
	public void sendMessage(String message) {
		send(null, ORDER_CHAT, myIndex, message);	// chat is always with true name as sender
	}

	/**
	 *	Displays the given message in the chat window.
	 *	@param message the message
	 *	@param neutral provide true to send a neutral message
	 *	@param name the sending player's name
	 *	@param self provide true if it was you who has sent the message
	 */
	public void showMessage(String message, boolean neutral, String name, boolean self) {
		if(chat != null) {
			if(neutral) {
				chat.write(message, ChatWindow.SYSTEM_COLOR);
				log(message);
				return;
			}
			if(message.startsWith(":")) {
				message = message.substring(1);
			} else {
				name += ":";
			}
			chat.write(String.format("<b>%s</b> %s", name, message.trim()), self ? ChatWindow.SELF_COLOR : ChatWindow.DEFAULT_COLOR);
		}
	}
	public void login(String cluster, String address, String name, GameState state) 
	{	network.login(cluster,address,name,state);
	}
	/**
	 *	Displays the given neutral message in the chat window.
	 *	@param message the message
	 */
	public void showMessage(String message) {
		showMessage(message, true, null, true);
	}

	/**
	 *	Calls the write method of the logger and logs the given message.
	 *	The message will be formatted by default.
	 *	@param message the message
	 */
	public void log(String message) {
		logger.write(message, true);
	}

	/**
	 *	Calls the write method of the logger and logs the given message.
	 *	@param message the message
	 *	@param format provide true to format the message
	 */
	public void log(String message, boolean format) {
		logger.write(message, format);
	}

	/**
	 *	Displays a confirmation dialog with the given message
	 *	and returns true if the player clicks OK.
	 *	@param message the warning message
	 */
	public boolean confirm(String message) {
		return confirm2(get(String.format("err%s", message)));
	}

	/**
	 *	Displays a confirmation dialog with the given message
	 *	and returns true if the player clicks OK.
	 *	@param message the warning message
	 */
	public boolean confirm2(String message) {
		int option = JOptionPane.showConfirmDialog(null, Util.format(message), get("errWarning"), JOptionPane.YES_NO_OPTION);
		return (option == JOptionPane.YES_OPTION);
	}

	/**
	 *	Displays the given information message.
	 *	@param message the message
	 *	@param title the dialog title
	 */
	public void showInformation(String message, String title) {
		JOptionPane.showMessageDialog(null, Util.format(message), title, JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 *	Displays the given information message
	 *	and resizes the popup to the given size.
	 *	@param message the message
	 *	@param title the dialog title
	 *	@param size the size
	 */
	public void showInformation(String message, String title, int size) {
		JOptionPane.showMessageDialog(null, Util.format(message, size), title, JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 *	Displays the given warning message.
	 *	@param message the message
	 */
	public void showWarning(String message) {
		showWarning2(get(String.format("err%s", message)));
	}

	/**
	 *	Displays the given warning message.
	 *	@param message the message
	 */
	public void showWarning2(String message) {
		JOptionPane.showMessageDialog(null, Util.format(message), get("errWarning"), JOptionPane.WARNING_MESSAGE);
	}

	/**
	 *	Displays the given error message.
	 *	@param message the message
	 */
	public void showError(String message) {
		showError2(get(String.format("err%s", message)));
	}

	/**
	 *	Displays the given error message.
	 *	@param message the message
	 */
	public void showError2(String message) {
		JOptionPane.showMessageDialog(null, Util.format(message), get("errWarning"), JOptionPane.ERROR_MESSAGE);
	}

	/**
	 *	Displays a fatal error that should never happen!
	 *	@param message the main error message
	 *	@param args optional parameters
	 */
	public void showFatalError(String message, Object... args) {
		StringBuilder msg = new StringBuilder();
		msg.append("<html><p>");
		msg.append(get("errImpossible"));
		msg.append("</p><p><b>");

		// begin format message
		message = get(String.format("err%s", message));
		if(args.length > 0) { message = String.format(message, Arrays.asList(args)); }
		// end format message
		msg.append(message);
		msg.append("</b></p><p>");
		msg.append(get("errPleaseReport"));
		msg.append("</p></html>");
		showError2(msg.toString());
	}

	/**
	 *	Quits the game.
	 */
	public void quit() 
		{ 	network.quit(); 
			if(logger != null) logger.close();
			if(chat!=null) { chat.dispose(); }
			if(lobby!=null) { lobby.dispose(); }
			if(main!=null) { main.dispose(); }
		}
	//================================================================================================= GETTERS

	/**
	 *	Returns the dictionary.
	 *	@return the dictionary
	 */
	public Dictionary getDictionary() {
		return dictionary;
	}

	/**
	 *	Returns the logger.
	 *	@return the logger
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 *	Returns the game state.
	 *	@return the game state
	 */
	public GameState getGameState() {
		return game;
	}

	/**
	 *	Returns the game title and version.
	 *	@return the game title and version
	 */
	public static String getTitleAndVersion() {
		StringBuffer ret = new StringBuffer();
		ret.append("Le Havre ");
		ret.append(GameState.VERSION_NUMBER);
		return ret.toString();
	}

	/**
	 *	Returns the main window.
	 *	@return the main window
	 */
	public MainWindow getMainWindow() {
		return main;
	}

	/**
	 *  get the player in control of this user interface
	 *	Returns the own player.
	 *	@return the own player
	 */
	public Player getUIPlayer() {
		return game.getPlayer(myIndex);
	}
	/**
	 * the the player allowed to make moves
	 * 
	 * @return the current player
	 */
	public Player getCurrentPlayer() 
	{	return game.getActivePlayer();
	}

    public void setIndex(int i) { myIndex=i; }

	//================================================================================================= PRIVATE METHODS

	/**
	 *	Calls the get method of the dictionary and retrieves the value
	 *	of the entry with the given key.
	 *	@param key the key
	 */
	private String get(String key) {
		return dictionary.get(key);
	}

	/**
	 *	Handles the given exception. If the connection was closed or the
	 *	peer isn't connected any more to the group, the program aborts.
	 *	@param e the exception
	 */
	private void handleException(Exception e) {
		e.printStackTrace();
		if(!network.isConnected() || !network.isOpen()) {
			showError("ConnectionLost");
			quit();
		} else {
			showError2(String.format(get("errNetwork"), e.getMessage()));
		}
	}

	//================================================================================================= DEBUG METHODS

	/**
	 *	Displays a complete summary of all game variables.
	 *	Prints this information to the console.
	 */
	public void dump() {
		send(null, ORDER_DUMP, getCurrentPlayer().getIndex());
	}
}
