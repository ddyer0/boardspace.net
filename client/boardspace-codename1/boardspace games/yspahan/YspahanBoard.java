package yspahan;


import com.codename1.ui.geom.Point;
import com.codename1.ui.geom.Rectangle;

import online.game.*;

import java.util.*;

import lib.*;
import lib.Random;


/**
 * YspahanBoard knows all about the game of Yspahan
 * 
 * 2DO: record the deck shuffle in the game records
 * this will disentangle game records from the live random number generator
 * @author ddyer
 * 
 */

public class YspahanBoard extends BaseBoard implements BoardProtocol,
		YspahanConstants 
{	ystate unresign;
	public ystate board_state = ystate.PUZZLE_STATE;
	static final int BUILDING_BONUS[] = { 0, 0, 0, 5, 5, 5, 10};		// vp for the n'th building, index by nowned
	static final int MAX_PLAYERS = 4;
	static final int N_EXTRA_DICE = 3;				// 3 extra dice are available for $1 each

	void setState(ystate newstate) {
		unresign = newstate==ystate.RESIGN_STATE?board_state:null;
		board_state = newstate;
		if(!board_state.GameOver()) 
		{ AR.setValue(win,false); 	// make sure "win" is cleared
		}
	}
	public boolean hasTemporaryCard(int forPlayer)
	{
		return(isDest(playerBoards[forPlayer].pmisc[ypmisc.card.index]));
	}

	public boolean GameOver() {
		return (board_state == ystate.GAMEOVER_STATE);
	}

	public ystate getState() {
		return (board_state);
	}

	/* overlay "pool table" for rolling dice */
	class DiceTable extends RcBoard {
		YspahanCell dice[] = new YspahanCell[N_DICE + N_EXTRA_DICE]; // dice to
																		// be
																		// rolled
		YspahanCell extraDice[] = new YspahanCell[N_EXTRA_DICE]; // extra dice
																	// to not be
																	// rolled

		// constructor
		DiceTable(Random r) {
			super(dboard_width, dboard_height);
			for (int i = 0; i < dice.length; i++) {
				dice[i] = makeDie(r, i, 'A', dboard_points[i]);
			}
			for (int i = 0; i < dboard_xpoints.length; i++) {
				extraDice[i] = makeDie(r, i, 'B', dboard_xpoints[i]);
			}
		}

		private YspahanCell makeDie(Random r, int i, char col, int[] spec) {
			YspahanCell die = new YspahanCell(r, yclass.dice, false,
					yrack.Dice_Table, col, i);
			setLocation(die, spec[0], spec[1]);
			return (die);
		}

		void copyFrom(DiceTable from_c) {
			for (int lim = dice.length - 1; lim >= 0; lim--) {
				dice[lim].copyFrom(from_c.dice[lim]);
			}
			;
			for (int lim = extraDice.length - 1; lim >= 0; lim--) {
				extraDice[lim].copyFrom(from_c.extraDice[lim]);
			}
			;
		}

		void resetExtraDice() {
			int from = N_DICE;
			int to = 0;
			while (from < dice.length) {
				YspahanChip top = dice[from].topChip();
				if (top != null) {
					while (extraDice[to].topChip() != null) {
						to++;
					}
					extraDice[to].addChip(dice[from].removeTop());
				}
				from++;
			}
		}

		void doInit() // move the extra dice back to the extra track
		{
			for (int i = 0; i < dice.length; i++) {
				dice[i].reInit();
				if (i < N_DICE) {
					dice[i].addChip(YspahanChip.getDie(i % 6 + 1, false));
				}
			}
			for (int i = 0; i < extraDice.length; i++) {
				extraDice[i].reInit();
				extraDice[i].addChip(YspahanChip.getDie(i % 6 + 2, true));
			}
		}

		public void setDisplayRectangle(Rectangle r) {
			if (players_in_game == 3) { // this is a bit of a kludge to
										// accomadate the different camel track
										// used in a 3-player game.
				r = G.clone(r);
				G.SetWidth(r,G.Width(r) / 3 * 4);
			}
			super.setDisplayRectangle(r);
		}

		private long Digest(Random r) {
			long v = 0;
			for (YspahanCell c : dice) {
				v += c.Digest(r);
			}
			for (YspahanCell c : extraDice) {
				v += c.Digest(r);
			}
			return (v);
		}

		private void sameDiceTable(DiceTable from_c) {
			for (int lim = dice.length - 1; lim >= 0; lim--) {
				YspahanCell c = dice[lim];
				YspahanCell d = from_c.dice[lim];
				G.Assert(c.sameCell(d),
						"dicetable cells %s and %s do not match", c, d);
			}
			for (int lim = extraDice.length - 1; lim >= 0; lim--) {
				YspahanCell c = extraDice[lim];
				YspahanCell d = from_c.extraDice[lim];
				G.Assert(c.sameCell(d),
						"dicetable extra cells %s and %s do not match", c, d);
			}
		}

		// roll all the dice that should be rolled
		private void doRoll(Random r) {
			for (YspahanCell die : dice) {
				if (die.topChip() != null) {
					die.doRoll(r);
				}
			}
		}

		// to is an array of 6 which receives the stack of dice for each value
		private void distribute(YspahanCell to[]) {
			for (YspahanCell die : to) {
				die.reInit();
			}
			for (int i = 0; i < dice.length; i++) {
				YspahanChip top = dice[i].topChip();
				if (top != null) {
					to[top.getDie().faceValue - 1].addChip(top);
				}
			}
		}

	}

	// caravan is a separate class mainly so we can treat it uniformly
	// as an auxiliary board, like the player boards
	class Caravan extends RcBoard { // constructor
		Caravan(Random r) {
			super(cboard_width, cboard_height);
			for (int i = 0; i < caravan12.length; i++) {
				int spec[] = caravan_points[i];
				YspahanCell c = new YspahanCell(r, yclass.playerCubes, false,
						yrack.Caravan_Track, 'A', spec[0]);
				caravan12[i] = c;
				if (i < 9) {
					caravan9[i] = c;
				}
				setLocation(caravan12[i], spec[1], spec[2]);
				caravan = caravan12;
			}
		}

		private YspahanCell caravan12[] = new YspahanCell[12];
		private YspahanCell caravan9[] = new YspahanCell[9];
		public YspahanCell caravan[] = caravan12;

		void doInit(int numPlayers) {
			for (YspahanCell c : caravan12) {
				c.reInit();
			}
			caravan = (numPlayers == 4) ? caravan12 : caravan9;
		}

		void emptyCaravan(replayMode replay) { // like clear, but return the cubes to the player
								// pool
			for (YspahanCell c : caravan) {
				YspahanChip top = c.topChip();
				if (top != null) {
					c.removeTop();
					YspahanCell dest = playerBoards[playerWithColor(top)].pmisc[ypmisc.cubes.index];
					dest.addChip(top);
					if(replay!=replayMode.Replay)
					{
					animationStack.push(c);
					animationStack.push(dest);
					}
				}
			}
		}

		void copyFrom(Caravan from_c) {
			for (int lim = caravan.length - 1; lim >= 0; lim--) {
				caravan[lim].copyFrom(from_c.caravan[lim]);
			}
			;
		}

		long Digest(Random r) {
			long v = 0;
			for (YspahanCell c : caravan) {
				v += c.Digest(r);
			}
			return (v);
		}

		void sameCaravan(Caravan from_c) {
			for (int lim = caravan.length - 1; lim >= 0; lim--) {
				YspahanCell c = caravan[lim];
				YspahanCell d = from_c.caravan[lim];
				G.Assert(c.sameCell(d), "caravan cells %s and %s do not match",
						c, d);
			}
		}

		YspahanCell lastFilledCell() {
			for (int lim = caravan.length - 1; lim >= 0; lim--) {
				YspahanCell c = caravan[lim];
				if (c.topChip() != null) {
					return (c);
				}
			}
			throw G.Error("Some cell should be filled");
		}

		YspahanCell firstEmptyCell() {
			for (int i = 0, lim = caravan.length; i < lim; i++) {
				YspahanCell c = caravan[i];
				if (c.topChip() == null) {
					return (c);
				}
			}
			throw G.Error("Some cell should be empty");
		}

		int immediateVP(YspahanCell c) {
			int third = caravan.length / 3; // each third is different
			return (2 - c.row / third); // first third, 2 points, second third 1
										// point
		}

		int totalCubesForPlayer(YspahanChip player) {
			int n = 0;
			for (YspahanCell c : caravan) {
				if (c.topChip() == player) {
					n++;
				}
			}
			return (n);
		}

		int highestCubeForPlayer(YspahanChip player) {
			for (int len = caravan.length, lim = len - 1; lim >= 0; lim--) {
				if (caravan[lim].topChip() == player) {
					return (lim / (len / 3) + 1); // which third are we in?
				}
			}
			return (0);
		}

		int score(YspahanChip player) {
			return (highestCubeForPlayer(player) * totalCubesForPlayer(player));
		}

		boolean isFull() {
			return (caravan[caravan.length - 1].topChip() != null);
		}
	}

	// one player board per player
	class PlayerBoard extends RcBoard implements lib.CompareTo<PlayerBoard>
	{
		YspahanCell buildings[] = new YspahanCell[ybuild.values().length]; // top
																			// row,
																			// buildings
		YspahanCell pmisc[] = new YspahanCell[ypmisc.values().length]; // second
																		// row,
																		// other
		public YspahanChip playerChip()									// stuff
		{
				return(color.chip);
		}
		boolean showCards = false;
		boolean showHiddenWindowCards = false;
		boolean hasNewCard = false;
		boolean hasSeenNewCard = false;
		int viewCardCount = 0;
		ycube color; // cube of the owner player
		int buildNoCamels = 0; // count of "build without camels" credits
		int buildNoGold = 0; // count of "build without gold" credits
		boolean paidCamelsByCard = false;
		boolean paidGoldByCard = false;
		long xrand = 0; // random number used to digest building credits
		int myIndex = -1; // index in the normal move order

		void doDone() {
			paidCamelsByCard = false;
			paidGoldByCard = false;
		}

		int startPlayerIndex = -1;

		//
		// add all the buildings we could build to "dests"
		//
		void getBuildingDests(Hashtable<YspahanCell, YspahanCell> dests) {
			for (YspahanCell c : buildings) {
				if (c.topChip() == null) // not owned yet
				{
					ybuild building = c.building;
					if (((buildNoCamels > 0) || (camelCount() >= building.camels))
							&& ((buildNoGold > 0) || (goldCount() >= building.gold))) {
						dests.put(c, c);
					}

				}
			}
		}

		// add a credit to build with no gold
		public void addBuildNoGold(int n) {
			buildNoGold += n;
		}

		// add a credit to build with no camels
		public void addBuildNoCamels(int n) {
			buildNoCamels += n;
		}

		// constructor
		PlayerBoard(Random r, ycube pl, int indx) {
			super(pboard_width, pboard_height);
			color = pl;
			myIndex = indx;
			for (int i = 0; i < subboard_track.length; i++) {
				int spec[] = subboard_track[i];
				YspahanCell c = new YspahanCell(r, yclass.playerCubes, false,
						yrack.Building_Track, (char) ('A' + indx),
						spec[0]);
				ybuild building = ybuild.find(spec[0]);
				setLocation(c, spec[1], spec[2]);
				c.playerIndex = indx;
				int ind = spec[0];
				buildings[ind] = c;
				c.building = building;
				c.helpText = building.helpText; // add the building specific
												// help text
			}
			xrand = r.nextLong();
			ypmisc mvals[] = ypmisc.values();
			for (int i = 0; i < subboard_misc.length; i++) {
				int spec[] = subboard_misc[i];
				int miscIndex = spec[0];
				YspahanCell c = new YspahanCell(r, mvals[miscIndex].type, true,
						yrack.Misc_Track, (char) ('A' + indx),
						miscIndex);
				setLocation(c, spec[1], spec[2]);
				c.playerIndex = indx;
				pmisc[spec[0]] = c;
				// the cube pile doesn't contribute to the digest
				if(spec[0]==ypmisc.cubes.index) { c.digestable = false; }
			}
		}

		// add victory points (stored as the height of the stack)
		public void addVP(int n) {
			YspahanCell c = pmisc[ypmisc.points.index];
			if (n > 0) {
				while (n-- > 0) {
					c.addChip(ymisc.point.chip);
				}
			} else {
				while (n++ < 0) {
					c.removeTop();
				}
			}
		}

		public boolean ownsExtraGoldBuilding() {
			YspahanCell bb = buildings[ybuild.extra_gold.index];
			return (bb.topChip() != null);
		}

		public boolean ownsExtraCamelBuilding() {
			YspahanCell bb = buildings[ybuild.extra_camel.index];
			return (bb.topChip() != null);
		}

		public boolean ownsExtraCubeBuilding() {
			YspahanCell bb = buildings[ybuild.extra_cube.index];
			return (bb.topChip() != null);
		}

		public boolean ownsExtraPointsBuilding() {
			YspahanCell bb = buildings[ybuild.extra_points.index];
			return (bb.topChip() != null);
		}

		public boolean ownsExtraCardBuilding() {
			YspahanCell bb = buildings[ybuild.extra_card.index];
			return (bb.topChip() != null);
		}

		public int getVP() {
			return (pmisc[ypmisc.points.index].height());
		}

		// return true if this user owns the building that enables supervisor
		// mobility
		public boolean ownsSupervisorBuilding() {
			YspahanCell bb = buildings[ybuild.extra_movement.index];
			return (bb.topChip() != null);
		}

		public int goldCount() {
			return (pmisc[ypmisc.gold.index].height());
		}

		public void spendGold(int amount,replayMode replay) {
			YspahanCell c = pmisc[ypmisc.gold.index];
			if (amount > 0) {
				G.Assert(c.height() >= amount, "not enough gold");
				moveFromTo(c,gold,amount,replay);
				} else {
				moveFromTo(gold,c,-amount,replay);
			}
		}

		public int camelCount() {
			return (pmisc[ypmisc.camel.index].height());
		}

		public void payForBuilding(ybuild building, boolean undo,replayMode replay) {
			if (building.camels > 0) {
				if (undo) {
					if (paidCamelsByCard) {
						buildNoCamels++;
						paidCamelsByCard = false;
					} else {
						spendCamels(-building.camels,replay);
					}
				} else {
					if (buildNoCamels > 0) {
						buildNoCamels--;
						paidCamelsByCard = true;
					} else {
						spendCamels(building.camels,replay);
					}
				}
			}
			if (building.gold > 0) {
				if (undo) {
					if (paidGoldByCard) {
						buildNoGold++;
						paidGoldByCard = false;
					} else {
						spendGold(-building.gold,replay);
					}
				} else {
					if (buildNoGold > 0) {
						buildNoGold--;
						paidGoldByCard = true;
					} else {
						spendGold(building.gold,replay);
					}
				}
			}
			int nOwned = nBuildingsOwned();
			addVP(BUILDING_BONUS[nOwned] * (undo ? -1 : 1));
		}

		int nBuildingsOwned() {
			int n = 0;
			for (YspahanCell c : buildings) {
				if (c.topChip() != null) {
					n++;
				}
			}
			return (n);
		}

		public void payGoldByCard() {
			buildNoGold--;
			paidGoldByCard = true;
		}

		public void spendCamels(int amount,replayMode replay) {
			YspahanCell c = pmisc[ypmisc.camel.index];
			if (amount > 0) {
				G.Assert(c.height() >= amount, "not enough camels");
				moveFromTo(c,camels,amount,replay);
				} else 
				{
					moveFromTo(camels,c,-amount,replay);
				}
		}

		public int cardCount() {
			return (pmisc[ypmisc.card.index].height());
		}

		void doInit(ycube myPlayer) {
			for (YspahanCell c : buildings) {
				c.reInit();
			}
			for (YspahanCell c : pmisc) {
				c.reInit();
			}
			color = myPlayer;
			YspahanCell gold = pmisc[ypmisc.gold.index];
			gold.addChip(ymisc.gold.chip); // give initially 2 gold
			gold.addChip(ymisc.gold.chip);
			YspahanCell cube = pmisc[ypmisc.cubes.index];
			for (int i = 0; i < 20; i++) {
				cube.addChip(color.chip);
			}
			buildNoCamels = 0;
			buildNoGold = 0;
			viewCardCount = 0;
			hasNewCard = false;
			hasSeenNewCard = false;
			paidCamelsByCard = false;
			paidGoldByCard = false;
		}
		long Digest(Random r) {
			long v = 0;
			// don't digest the cubes, as the exact state of the pile is not
			// part of the game site. This avoids problems with reset and undo
			// when the stack has been topped up.
			for (YspahanCell c : pmisc) {
					v += c.Digest(r);
			}
			for (YspahanCell c : buildings) {
				v += c.Digest(r);
			}
			v ^= (buildNoGold * 100 + buildNoCamels) * xrand;
			v ^= viewCardCount*1343526;
			return (v);
		}

		void copyFrom(PlayerBoard from) {
			for (int lim = buildings.length - 1; lim >= 0; lim--) {
				buildings[lim].copyFrom(from.buildings[lim]);
			}
			for (int lim = pmisc.length - 1; lim >= 0; lim--) {
				pmisc[lim].copyFrom(from.pmisc[lim]);
			}
			buildNoCamels = from.buildNoCamels;
			buildNoGold = from.buildNoGold;
			hasSeenNewCard = from.hasSeenNewCard;
			hasNewCard = from.hasNewCard;
			viewCardCount = from.viewCardCount;
			showHiddenWindowCards = from.showHiddenWindowCards;
			showCards = from.showCards;

		}

		void sameBoard(PlayerBoard from) {
			for (int lim = buildings.length - 1; lim >= 0; lim--) {
				YspahanCell c = buildings[lim];
				YspahanCell d = from.buildings[lim];
				G.Assert(c.sameCell(d), "cells %s and %s do not match", c, d);
			}

			// don't digest the cubes, as the exact state of the pile is not
			// part of the game site. This avoids problems with reset and undo
			// when the stack has been topped up.
			YspahanCell cubes = pmisc[ypmisc.cubes.index];
			for (int lim = pmisc.length - 1; lim >= 0; lim--) {
				YspahanCell c = pmisc[lim];
				YspahanCell d = from.pmisc[lim];
				if (c != cubes) {
					G.Assert(c.sameCell(d), "cells %s and %s do not match", c,
							d);
				}
			}
			G.Assert(buildNoCamels == from.buildNoCamels,
					"mismatch buildNoCamels");
			G.Assert(buildNoGold == from.buildNoGold, "mismatch buildNoCamels");
			G.Assert(viewCardCount==from.viewCardCount,"view card count wrong");

		}

		public int altCompareTo(PlayerBoard o) {
			return (compareTo(o));
		}

		public int compareTo(PlayerBoard other) {
			// compare player order for last move. lowerst score first, greatest
			// distance from firstplayer second
			int myScore = getVP();
			int hisScore = other.getVP();
			if (myScore == hisScore) {
				int myIndex = startPlayerIndex;
				int hisIndex = other.startPlayerIndex;
				// high index (player with the fewest choices last move) first.
				return (Integer.signum(hisIndex - myIndex));
			} else { // low score first
				return (Integer.signum(myScore - hisScore));
			}
		}
	}

	static final int Direction_Prev = 0; // up to 4 linkages per cell, but the
											// directions
	static final int Direction_Next = 2; // are really nominal. If the linkage
											// is not null,
	static final int Direction_Up = 1; // you have to check what it links to ,
										// it could
	static final int Direction_Down = 3; // be another related cell or a
											// supervisor track cell


	YspahanCell allCells = null; // linked list of all cells on the board
	YspahanCell hub = null; // the center of the supervisor track
	YspahanCell cards = null; // the untaken card pile
	YspahanCell discards = null; // the played card pile
	YspahanCell diceTower[] = new YspahanCell[6];
	YspahanCell diceTowerExtraGold[] = new YspahanCell[6];
	YspahanCell diceTowerExtraCard[] = new YspahanCell[6];

	YspahanCell diceSorter[] = new YspahanCell[6];
	YspahanCell gold = null;
	YspahanCell camels = null;

	Caravan caravan = null; // the camel caravan
	DiceTable diceTable = null; // the pop-up dice table
	public YspahanCell selectedDice = null; // dice selected during selection
											// phase
	public YspahanCell selectedCube = null; // cube selected to go to the
											// caravan first
	public YspahanCell protectedCube = null; // cube we protected by paying a
												// camel
	public YspahanCell protectedCube2 = null; // second protected cube
	private ystate confirmCardUndoState = null;
	private ystate resetState = null;
	private YspahanCell resetDice = null;
	
	int payCamelReturnPlayer = -1; // player to return to after paying camels
	public YspahanCell supervisor = null; // the cell containing the supervisor
	public YspahanCell nextSupervisor = null; // the cell that will containt the
												// supervisor next turn

	YspahanCell tempCardCell = null; // temp for drawing cards with a back card
										// on top
	YspahanCell discardStack = null; // copy of reshuffled discards (for undo)
	public PlayerBoard[] playerBoards = new PlayerBoard[MAX_PLAYERS];
	public PlayerBoard getPlayerBoard(int n)
	{
		PlayerBoard p[] = playerBoards;
		if(p!=null && n<p.length) { return(p[n]); }
		return(null);
	}
	private PlayerBoard lastTurnPlayers[] = null; // special ordering of the
													// players for the last turn
	private int lastTurnIndex = -1; // index into the lastTurnPlayers array

	public int startPlayer = 0; // player who starts the current round
	public int gameDay = 0; // game day corresponds to the roll
	public int cardTradeCount = 0; // count of camels or gold traded for victory
									// points
	public YspahanCell days[] = new YspahanCell[7];
	public YspahanCell weeks[] = new YspahanCell[4]; // one extra week so
														// gameover doesn't
														// cause a problem

	public void SetDrawState() {
		throw G.Error("Shouldn't happen");
	}

	Rectangle dispRect = new Rectangle(0, 0, 1, 1);
	RcBoard coords = new RcBoard(board_width, board_height);

	void SetDisplayRectangle(Rectangle r) { // define the actual rectangle
											// occupied by the board, which is
											// centered in "r"
		coords.setDisplayRectangle(r);
	}

	public Point getXY(YspahanCell c) {
		return (coords.getXY(c));
	}

	public int getY(YspahanCell c) {
		return (coords.getY(c));
	}

	public int getX(YspahanCell c) {
		return (coords.getX(c));
	}

	// intermediate states in the process of an unconfirmed move should
	// be represented explicitly, so unwinding is easy and reliable.
	public YspahanChip pickedObject = null;
	public IStack pickedObjectCount = new IStack(); // number of objects of the
													// same type or the stack
													// depth for a single
													// object.
	public IStack droppedObjectCount = new IStack(); // number of objects of the
														// same type in the dest
														// stack
	
	private CellStack pickedSourceStack = new CellStack();
	private CellStack droppedDestStack = new CellStack();
	private StateStack pickedStateStack = new StateStack(); // game
																				// state
																				// before
																				// the
																				// pick
	private StateStack droppedStateStack = new StateStack(); // game
																					// state
																					// before
																					// the
	
	public CellStack animationStack = new CellStack();																		// drop
	public int goldCount(int pl) {
		return (playerBoards[pl].goldCount());
	}

	public int camelCount(int pl) {
		return (playerBoards[pl].camelCount());
	}

	public int cardCount(int pl) {
		return (playerBoards[pl].cardCount());
	}

	// constructor for cells on the main board
	private YspahanCell makeCell(yclass ty, yrack rack, char col, int row,
			double w, double h) {
		YspahanCell c = new YspahanCell(ty, false, rack, col, row);
		coords.setLocation(c, w, h);
		c.next = allCells;
		allCells = c;
		return (c);
	}

	// find a cell on the main board by rack (neighborhood) col (souk) and row
	// (house)
	private YspahanCell getMainBoardCell(yrack rack, char col, int row) {
		for (YspahanCell c = allCells; c != null; c = c.next) {
			if ((c.rackLocation == rack)
					&& ((rack == yrack.Supervisor_Track) || (c.col == col)) // col
																			// is
																			// not
																			// used
																			// in
																			// supervisor
																			// track
					&& (c.row == row)) {
				return (c);
			}
		}
		throw G.Error("cell %d,%d not found", rack, row);
	}

	// copy a stack of cells
	private void copyFrom(CellStack c, CellStack d) {
		c.clear();
		for (int i = 0, lim = d.size(); i < lim; i++) {
			c.push(getCell(d.elementAt(i)));
		}
	}

	// digest a stack of cells
	private long Digest(Random r, CellStack c) {
		long v = 0;
		for (int i = 0, lim = c.size(); i < lim; i++) {
			v ^= c.elementAt(i).Digest(r);
		}
		return (v);
	}

	// digest a stack of integers
	public long Digest(Random r, IStack c) {
		long v = 0;
		for (int i = 0, lim = c.size(); i < lim; i++) {
			v ^= c.elementAt(i) * r.nextLong();
		}
		return (v);
	}

	//
	// construct each of the neighborhoods of a sector, and link to the
	// supervisor track
	// if necessary. a few special houses are linked to more than one
	// supervisor, so special
	// cases are included.
	//
	// the "prev" and "next" links link houses in a souk, and "up" and "down"
	// links link to a supervisor position.
	//
	private void constructSouk(yclass tp, int[][][] souklist, yrack rack) {
		int ne = 0;
		for (int[][] neighborhood : souklist) {
			YspahanCell prev = null;
			int number = 0;
			for (int[] house : neighborhood) {
				int link = house[0];
				YspahanCell c = makeCell(tp, rack, (char) ('A' + ne), number,
						house[1], house[2]);
				if (rack != yrack.Time_Track) {
					c.soukValue = house[3];
				}
				if (prev != null) {
					c.addLink(Direction_Prev, prev);
					prev.addLink(Direction_Next, c);
				}
				while (link >= 0) {
					int find = link % 100;
					link = link / 100;
					if (link == 0) {
						link = -1;
					}
					YspahanCell s = getMainBoardCell(yrack.Supervisor_Track,
							'@', find);
					c.addLink((s.col == 'v') ? Direction_Down : Direction_Up, s);
					s.addLink(
							((s.col == 'v') ? ((rack == yrack.Barrel_Neighborhood) ? Direction_Prev
									: Direction_Next)
									: ((rack == yrack.Vase_Neighborhood) ? Direction_Up
											: Direction_Down)), c);
				}
				prev = c;
				number++;
			}
			ne++;
		}
	}

	//
	// construct a board of linked locations. The backbone is formed by the
	// horizontal
	// and vertical supervisor tracks, which are linked into one list with cell
	// 10 at
	// the hub. Horizontal prev and next links connect the horizontal
	// supervisor, and
	// up and down links the vertical supervisor. Where houses are adjacent to
	// the supervisor
	// cell, links in the orthogonal direction are used.
	//
	private void constructBoard() {
		allCells = null;
		{
			YspahanCell prev = null;
			// horizonrtal backbone
			for (int i = 0; i < center_track_h.length; i++) {
				int spec[] = center_track_h[i];
				YspahanCell c = makeCell(yclass.supervisor,
						yrack.Supervisor_Track, '@', spec[0], spec[1], spec[2]);
				if (prev != null) {
					c.addLink(Direction_Prev, prev);
					prev.addLink(Direction_Next, c);
				}
				prev = c;
			}
		}
		// vertical backbone, and link to the center of the horizontal backbone
		{
			YspahanCell prev = null;
			for (int i = 0; i < center_track_v.length; i++) {
				int spec[] = center_track_v[i];
				int link = spec[0] / 100;
				YspahanCell c = (link != 0) ? hub = getMainBoardCell(
						yrack.Supervisor_Track, 'h', link) : makeCell(
						yclass.supervisor, yrack.Supervisor_Track, 'v',
						spec[0], spec[1], spec[2]);
				if (prev != null) {
					c.addLink(Direction_Up, prev);
					prev.addLink(Direction_Down, c);
				}
				prev = c;
			}
		}
		constructSouk(yclass.playerCubes, bag_souks, yrack.Bag_Neighborhood);
		constructSouk(yclass.playerCubes, barrel_souks,
				yrack.Barrel_Neighborhood);
		constructSouk(yclass.playerCubes, chest_souks, yrack.Chest_Neighborhood);
		constructSouk(yclass.playerCubes, vase_souks, yrack.Vase_Neighborhood);
		constructSouk(yclass.timeCubes, time_tracks, yrack.Time_Track);

		for (int i = 0; i < days.length; i++) {
			days[i] = getMainBoardCell(yrack.Time_Track, 'A', i);
		}
		for (int i = 0; i < weeks.length; i++) {
			weeks[i] = getMainBoardCell(yrack.Time_Track, 'B', i);
		}
		Random r = new Random(63546);
		//
		// the cells linked with allCells are all displayed simply, they either
		// contain a cube (or the supervisor) or not.
		//
		if (allCells != null) {
			allCells.setDigestChain(r);
		}

		// the dice tower is also part of the main board, but not linked into
		// allCells
		for (int i = 0; i < dice_points.length; i++) {
			int spec[] = dice_points[i];
			int index = spec[0];
			YspahanCell c = new YspahanCell(r, yclass.dice, true,
					yrack.Dice_Tower, 'A', index);
			YspahanCell gold = new YspahanCell(r, yclass.gold, true,
					yrack.Dice_Tower, 'B', index); // stack of gold
			YspahanCell card = new YspahanCell(r, yclass.cards, false,
					yrack.Dice_Tower, 'C', index); // single card
			diceTower[index] = c;
			diceTowerExtraGold[index] = gold;
			diceTowerExtraCard[index] = card;

			// link the tower cells to the ydicetower enum instance
			for (ydicetower db : ydicetower.values()) {
				if (db.towerIndex == spec[0]) {
					c.diceTower = db;
				}
			}
			coords.setLocation(c, spec[1], spec[2]);
			coords.setLocation(gold, spec[1] + TOWER_GOLD_X, spec[2]
					+ TOWER_GOLD_Y);
			coords.setLocation(card, spec[1] + TOWER_CARD_X, spec[2]
					+ TOWER_CARD_Y);
		}

		for (int i = 0; i < diceSorter.length; i++) {
			diceSorter[i] = new YspahanCell(r, yclass.dice, true, yrack.NoWhere);
		}
		cards = new YspahanCell(r, yclass.cards, true, yrack.Card_Stack);
		camels = new YspahanCell(r, yclass.camels, true, yrack.Camel_Pool);
		camels.helpText = camelHelpText;
		camels.digestable = false;		// camels don't contribute to the digest
		gold = new YspahanCell(r, yclass.gold, true, yrack.Gold_Pool);
		gold.helpText = goldHelpText;
		gold.digestable = false;	// gold doesn't contribute to the digest
		tempCardCell = new YspahanCell(r, yclass.nullSet, true,
				yrack.Card_Stack);
		discardStack = new YspahanCell(r, yclass.cards, true,
				yrack.Discard_Stack);
		discardStack.row = 1;
		discards = new YspahanCell(r, yclass.cards, true, yrack.Discard_Stack);
		caravan = new Caravan(r);
		diceTable = new DiceTable(r);
		// construct the player boards
		for (int i=0;i<playerBoards.length;i++) {
			playerBoards[i] = new PlayerBoard(r, ycube.values()[i], i);
		}

	}

	public YspahanBoard(String init, long key,int []map,int players) // default constructor
	{
		setColorMap(map, players);
		constructBoard();
		doInit(init, key, players); // do the initialization
	}

	public void sameboard(BoardProtocol f) {
		sameboard((YspahanBoard) f);
	}

	/**
	 * Robots use this to verify a copy of a board. If the copy method is
	 * implemented correctly, there should never be a problem. This is mainly a
	 * bug trap to see if BOTH the copy and sameboard methods agree.
	 * 
	 * @param from_b
	 */
	public void sameboard(YspahanBoard from_b) {
		super.sameboard(from_b);
				
		for (YspahanCell c = allCells, d = from_b.allCells; c != null; c = c.next, d = d.next) {
			G.Assert(c.sameCell(d), "cells match");
		}
		caravan.sameCaravan(from_b.caravan);
		diceTable.sameDiceTable(from_b.diceTable);
		for (int lim = playerBoards.length - 1; lim >= 0; lim--) {
			playerBoards[lim].sameBoard(from_b.playerBoards[lim]);
		}
		G.Assert(confirmCardUndoState == from_b.confirmCardUndoState,
				"mismatch confirmCardUndoState");
		G.Assert(resetState == from_b.resetState,
				"mismatch resetState");
		G.Assert(resetDice == from_b.resetDice,
				"mismatch resetDice");
		G.Assert(payCamelReturnPlayer == from_b.payCamelReturnPlayer,
				"mismatch payCamelReturnState");
		G.Assert(cards.sameCell(from_b.cards), "card stack should match");
		G.Assert(discards.sameCell(from_b.discards), "discards should match");
		G.Assert((unresign == from_b.unresign),
				"unresign mismatch");
		G.Assert(gameDay == from_b.gameDay, "gameDay matches");
		G.Assert(cardTradeCount == from_b.cardTradeCount,
				"tradeCamelCount matches");
		G.Assert(startPlayer == from_b.startPlayer, "startPlayer matches");
		G.Assert(YspahanCell.sameCell(diceTower, from_b.diceTower),
				"dice tower doesn't match");
		G.Assert(YspahanCell.sameCell(diceTowerExtraGold,
				from_b.diceTowerExtraGold), "dice tower gold doesn't match");
		G.Assert(YspahanCell.sameCell(diceTowerExtraCard,
				from_b.diceTowerExtraCard), "dice tower cards don't match");

		G.Assert(YspahanCell.sameCell(supervisor, from_b.supervisor),
				"mismatch supervisor location");
		G.Assert(YspahanCell.sameCell(nextSupervisor, from_b.nextSupervisor),
				"mismatch nextSupervisor location");
		G.Assert(YspahanCell.sameCell(selectedDice, from_b.selectedDice),
				"mismatch selected dice");

		// do not digest camels or gold, as the exact state of the pile is not
		// part of the game state
		// this avoids problems with "undo" happening after the pile
		// has been topped up.
		// G.Assert(gold.sameCell(from_b.gold),"gold doesn't match");
		// G.Assert(camels.sameCell(from_b.camels),"camels don't match");

		// this is a good overall check that all the copy/check/digest methods
		// are in sync, although if this does fail you'll no doubt be at a loss
		// to explain why.
		
		G.Assert(Digest() == from_b.Digest(), "Digest matches");

	}

	/**
	 * Digest produces a 64 bit hash of the game state. This is used in many
	 * different ways to identify "same" board states. Some are germane to the
	 * ordinary operation of the game, others are for system record keeping use;
	 * so it is important that the game Digest be consistent both within a game
	 * and between games over a long period of time which have the same moves.
	 * (1) Digest is used by the default implementation of EditHistory to remove
	 * moves that have returned the game to a previous undoInfo; ie when you
	 * undo a move or hit the reset button. (2) Digest is used after EditHistory
	 * to verify that replaying the history results in the same game as the user
	 * is looking at. This catches errors in implementing undo, reset, and
	 * EditHistory (3) Digest is used by standard robot search to verify that
	 * move/unmove returns to the same board undoInfo, also that
	 * move/move/unmove/unmove etc. (4) Digests are also used as the game is
	 * played to look for draw by repetition. The undoInfo after most moves is
	 * recorded in a hashtable, and duplicates/triplicates are noted. (5) games
	 * where repetition is forbidden (like xiangqi/arimaa) can also use this
	 * information to detect forbidden loops. (6) Digest is used in fraud
	 * detection to see if the same game is being played over and over. Each
	 * game in the database contains a digest of the final undoInfo of the game,
	 * and a midpoint undoInfo of the game. Other site machinery looks for
	 * duplicate digests. (7) digests are also used in live play to detect
	 * "parroting" by running two games simultaneously and playing one against
	 * the other.
	 */
	public long Digest() {
		Random r = new Random(64 * 1000); // init the random number generator
		long v = 0;

		// the basic digestion technique is to xor a bunch of random numbers.
		// The key
		// trick is to always generate exactly the same sequence of random
		// numbers, and
		// xor some subset of them. Note that if you tweak this, all the
		// existing
		// digests are invalidated.
		//

		for (YspahanCell c = allCells; c != null; c = c.next) {
			v ^= c.Digest(r); // this gets all the cells actually on the main
								// board
		}
		for (PlayerBoard pl : playerBoards) {
			v ^= pl.Digest(r);
		}
		v ^= caravan.Digest(r);
		v ^= diceTable.Digest(r);
		v ^= cards.Digest(r);
		// do not digest camels or gold, as the exact state of the pile is not
		// part of the game state
		// this avoids problems with "undo" happening after the pile
		// has been topped up.
		// v ^= gold.Digest();
		// v ^= camels.Digest();
		v ^= ((confirmCardUndoState == null ? 0 : confirmCardUndoState
				.ordinal()) * 10 + payCamelReturnPlayer) * r.nextLong();
		v ^= resetState.ordinal() * r.nextLong();
		v ^= cell.Digest(r,resetDice);
		v ^= cell.Digest(r, selectedDice);
		v ^= cell.Digest(r, selectedCube);
		v ^= cell.Digest(r, protectedCube);
		v ^= cell.Digest(r, protectedCube2);
		v ^= cell.Digest(r, supervisor);
		v ^= cell.Digest(r, nextSupervisor);
		v ^= discards.Digest(r);
		v ^= discardStack.Digest(r);
		v ^= ((cardTradeCount * 1000) + (startPlayer * 100) + (gameDay + 1))
				* r.nextLong();
		v ^= chip.Digest(r, pickedObject);
		v ^= Digest(r, pickedObjectCount);
		v ^= Digest(r, droppedObjectCount);
		v ^= Digest(r, pickedSourceStack);
		v ^= Digest(r, droppedDestStack);
		v ^= (players_in_game + 1) * r.nextLong();
		v ^= (board_state.ordinal() * 100 + whoseTurn )
				* r.nextLong();
		return (v);
	}

	public YspahanBoard cloneBoard() {
		YspahanBoard copy = new YspahanBoard(gametype, randomKey,getColorMap(),players_in_game);
		copy.copyFrom(this);
		return (copy);
	}
    public void copyFrom(BoardProtocol b) { copyFrom((YspahanBoard)b); }

	/*
	 * make a copy of a board. This is used by the robot to get a copy of the
	 * board for it to manupulate and analyze without affecting the board that
	 * is being displayed.
	 */
	public void copyFrom(YspahanBoard from_board) {
		YspahanBoard from_b = from_board;
		G.Assert(from_b != this, "can clone from myself");
		doInit(from_b.gametype, from_b.randomKey, from_b.players_in_game);
		super.copyFrom(from_board);
		board_state = from_b.board_state;
		unresign = from_b.unresign;
		pickedObject = from_b.pickedObject;
		confirmCardUndoState = from_b.confirmCardUndoState;
		resetState = from_b.resetState;
		resetDice = from_b.resetDice;
		payCamelReturnPlayer = from_b.payCamelReturnPlayer;
		pickedObjectCount.copyFrom(from_b.pickedObjectCount);
		droppedObjectCount.copyFrom(from_b.droppedObjectCount);
		copyFrom(pickedSourceStack, from_b.pickedSourceStack);
		copyFrom(droppedDestStack, from_b.droppedDestStack);
		pickedStateStack.copyFrom(from_b.pickedStateStack);
		droppedStateStack.copyFrom(from_b.droppedStateStack);
		cards.copyFrom(from_b.cards);
		gold.copyFrom(from_b.gold);
		camels.copyFrom(from_b.camels);
		selectedDice = getCell(from_b.selectedDice);
		selectedCube = getCell(from_b.selectedCube);
		protectedCube = getCell(from_b.protectedCube);
		protectedCube2 = getCell(from_b.protectedCube2);
		supervisor = getCell(from_b.supervisor);
		nextSupervisor = getCell(from_b.nextSupervisor);
		nextSupervisor = getCell(from_b.nextSupervisor);
		discards.copyFrom(from_b.discards);
		discardStack.copyFrom(from_b.discardStack);
		gameDay = from_b.gameDay;
		cardTradeCount = from_b.cardTradeCount;
		startPlayer = from_b.startPlayer;
		for (YspahanCell dest = allCells, src = from_board.allCells; dest != null; dest = dest.next, src = src.next) {
			dest.copyFrom(src);
		}
		for (int lim = playerBoards.length - 1; lim >= 0; lim--) {
			playerBoards[lim].copyFrom(from_b.playerBoards[lim]);
		}
		for(int lim = diceSorter.length-1; lim>=0; lim--)
			{	diceSorter[lim].copyFrom(from_b.diceSorter[lim]);		
			}
		caravan.copyFrom(from_b.caravan);
		diceTable.copyFrom(from_b.diceTable);
		YspahanCell.copyFrom(diceTower, from_b.diceTower);
		YspahanCell.copyFrom(diceTowerExtraGold, from_b.diceTowerExtraGold);
		YspahanCell.copyFrom(diceTowerExtraCard, from_b.diceTowerExtraCard);
		sameboard(from_b);
	}

	private void setGameDay(int newday) {
		int week = gameDay / 7;
		int day = gameDay % 7;
		YspahanCell oldDay = days[day];
		YspahanCell oldWeek = weeks[week];
		oldWeek.reInit();
		oldDay.reInit();
		int nweek = newday / 7;
		int nday = newday % 7;
		YspahanCell newDay = days[nday];
		YspahanCell newWeek = weeks[nweek];
		newDay.addChip(ymisc.white.chip);
		newWeek.addChip(ymisc.white.chip);
		gameDay = newday;
	}

	// deduce the current game day from the position of the cubes
	private void deriveGameDay() {
		int day = 0;
		int week = 0;
		for (int i = 0; i < days.length; i++) {
			if (days[i].topChip() != null) {
				day = i;
			}
		}
		for (int i = 0; i < weeks.length; i++) {
			if (weeks[i].topChip() != null) {
				week = i;
			}
		}
		gameDay = (week * 7 + day);
	}

	public void doInit() {
		doInit(gametype, randomKey, players_in_game);
	}
	public void doInit(String typ,long key)
	{
		doInit(typ,key,players_in_game);
	}
	/* initialize a board back to initial empty undoInfo */
	public void doInit(String gtype, long key, int players) {
		randomKey = key; // not used, but for reference in this demo game
		players_in_game = players;
		win = new boolean[players];
		Random gameRandom = new Random(key);

		{
			if (Yspahan_INIT.equalsIgnoreCase(gtype)) {
			} else {
				throw G.Error(WrongInitError, gtype);
			}
			gametype = gtype;
		}
		setState(ystate.PUZZLE_STATE);
		for (YspahanCell c = allCells; c != null; c = c.next) {
			c.reInit();
		}
		whoseTurn = FIRST_PLAYER_INDEX;
		moveNumber = 1;

		hub.addChip(ymisc.supervisor.chip);
		supervisor = hub;
		nextSupervisor = null;
		cards.reInit();
		discards.reInit();
		discardStack.reInit();
		camels.reInit();
		selectedDice = null;
		selectedCube = null;
		protectedCube = null;
		protectedCube2 = null;
		payCamelReturnPlayer = -1;
		confirmCardUndoState = null;
		resetState = ystate.SELECT_STATE;
		resetDice = null;
		lastTurnPlayers = null;
		lastTurnIndex = -1;
		cardTradeCount = 0;
		gold.reInit();
		for (int i = 0; i < 30; i++) {
			gold.addChip(ymisc.gold.chip);
			camels.addChip(ymisc.camel.chip);
		}
		pickedSourceStack.clear();
		droppedDestStack.clear();
		pickedStateStack.clear();
		droppedStateStack.clear();
		YspahanCell.reInit(diceTower);
		YspahanCell.reInit(diceTowerExtraGold);
		YspahanCell.reInit(diceTowerExtraCard);
		setGameDay(0);
		for (ycard card : ycard.values()) {
			if (card != ycard.back) {
				cards.addChip(card.chip);
				cards.addChip(card.chip); // 2 of each card
			}
		}
		cards.shuffle(gameRandom);

		ycube cubes[] = ycube.values();
		for (int i=0;i<players_in_game;i++) {
			playerBoards[i].doInit(cubes[getColorMap()[i]]);
		}
		caravan.doInit(players);
		diceTable.doInit();
		setStartPlayer(whoseTurn);
		// note that firstPlayer is NOT initialized here
	}


	void lastTurnPlayerOrder() {
		int lim = playerBoards.length;
		PlayerBoard players[] = new PlayerBoard[lim];
		for (int i = 0; i < lim; i++) {
			players[i] = playerBoards[i];
		}
		Sort.sort(players);
		setStartPlayer(players[0].myIndex);
		lastTurnPlayers = players;
		lastTurnIndex = 0;
		whoseTurn = startPlayer;
	}

	void setStartPlayer(int n) {
		startPlayer = n;
		for (int i = 0; i < players_in_game; i++) {
			playerBoards[(i + startPlayer) % players_in_game].startPlayerIndex = i;
		}
	}

	//
	// change whose turn it is, increment the current move number
	//
	private void setNextPlayer() {
		switch (board_state) {
		default:
			throw G.Error("Move not complete, can't change the current player");
		case PUZZLE_STATE:
			break;
		case CONFIRM_STATE:
		case BUILD_STATE:
		case RESIGN_STATE:
			moveNumber++; // the move is complete in these states
			if (lastTurnPlayers != null) {
				// special logic for the last turn in 4 player games.
				lastTurnIndex = (lastTurnIndex + 1) % lastTurnPlayers.length;
				whoseTurn = lastTurnPlayers[lastTurnIndex].myIndex;
				if (lastTurnIndex == 0) {
					lastTurnPlayers = null;
				}
			} else {
				whoseTurn = (whoseTurn + 1) % players_in_game;
			}
			if (whoseTurn == startPlayer) {
				setGameDay(gameDay + 1);
				if ((gameDay == 20) && (players_in_game == 4)) {
					lastTurnPlayerOrder(); // special rule for the last turn
				} else {
					setStartPlayer((startPlayer + 1) % players_in_game);
					whoseTurn = startPlayer;
				}
			}

			return;
		}
	}

	/**
	 * this is used to determine if the "Done" button in the UI is live
	 * 
	 * @return true of a Done is acceted in the current board state
	 */
	public boolean DoneState() {
		
		switch (board_state) {
		case CARD_SCORE_CAMELS:
		case CARD_SCORE_GOLD:
		case CARD_TRADE_CAMELS_GOLD:
			return (cardTradeCount != 0);
		case CONFIRM_STATE:
		case CONFIRM_CARD_STATE:
		case ROLL_STATE:
		case BUILD_STATE:
		case PREBUILD_STATE:
		case PAID_CAMEL_STATE:
		case DESIGNATED_CUBE_STATE:
		case PASS_STATE:
		case RESIGN_STATE:
			return (true);

		default:
			return (false);
		}
	}

	public boolean WinForPlayerNow(int player) { // return true if the
													// conditions for a win
													// exist for player right
													// now
		if (board_state == ystate.GAMEOVER_STATE) {
			return (win[player]);
		}
		throw G.Error("not implemented");
	}

	// look for a win for player. This algorithm should work for Gobblet Jr too.
	public double ScoreForPlayer(int player, boolean print)
	{
		throw G.Error("not implemented");
	}

	public int currentScoreForPlayer(int pl) {
		return (playerBoards[pl].getVP());
	}
	
	// get the current scores for use by montebot
	public void getScores(YspahanMovespec m)
	{	for(int i=0;i<players_in_game; i++) { m.playerScores[i] = playerBoards[i].getVP(); }
	}
	//
	// return true if balls[rack][ball] should be selectable, meaning
	// we can pick up a ball or drop a ball there. movingBallColor is
	// the ball we would drop, or -1 if we want to pick up
	//
	public void acceptPlacement() {
		playerBoards[whoseTurn].hasNewCard = hasTemporaryCard(whoseTurn);
		playerBoards[whoseTurn].hasSeenNewCard = false;
		pickedObject = null;
		pickedObjectCount.clear();
		droppedObjectCount.clear();
		if (nextSupervisor != null) {
			supervisor = nextSupervisor;
			nextSupervisor = null;
		}
		pickedSourceStack.clear();
		droppedDestStack.clear();
		pickedStateStack.clear();
		droppedStateStack.clear();
	}

	//
	// undo the drop, restore the moving object to moving status.
	//
	private YspahanCell unDropObject() {
		if (droppedDestStack.size() > 0) {
			YspahanCell dr = droppedDestStack.pop();
			setState(droppedStateStack.pop());
			if (dr != null) {
				doTransfers(dr, true,replayMode.Live);
				selectedCube = null;
				int count = droppedObjectCount.pop();
				if (count > 0) {
					while (count-- > 0) {
						pickedObject = dr.removeTop();
					}
				} else {
					pickedObject = dr.removeTop();
				}
				if (pickedObject == ymisc.supervisor.chip) {
					nextSupervisor = null;
				}
				if (board_state == ystate.PAY_CAMEL_STATE) {
					selectedCube = protectedCube;
					protectedCube = protectedCube2;
					protectedCube2 = null;
				}
				if (board_state == ystate.CARD_TRADE_CAMELS_GOLD) {
					if (pickedObject.type == yclass.gold) {
						pickedObject = ymisc.camel.chip;
					} else if (pickedObject.type == yclass.camels) {
						pickedObject = ymisc.gold.chip;
					}
				}

			}
			// the reshuffle is hidden under the top of the stack
			if (droppedDestStack.top() == discardStack) {
				undoReshuffle();
			}
			return(dr);
		}
		return(null);
	}

	//
	// undo the pick, getting back to base undoInfo for the move
	//
	private void unPickObject(replayMode replay,YspahanCell dropped) {
		YspahanChip po = pickedObject;

		if (po != null) {
			pickedObject = null;
			YspahanCell ps = pickedSourceStack.pop();
			if ((board_state == ystate.CARD_TRADE_CAMELS_GOLD)
					&& (po.type != ps.type)) {
				if (po.type == yclass.camels) {
					po = ymisc.gold.chip;
				} else if (po.type == yclass.gold) {
					po = ymisc.camel.chip;
				}
			}
			
			setState(pickedStateStack.pop());
			if(board_state==ystate.SELECT_STATE) { selectedDice = null; resetDice = null; }
			if(po.isCard()) { confirmCardUndoState = null; }
			if (po == ymisc.supervisor.chip) {
				nextSupervisor = null;
			}
			int count = pickedObjectCount.pop();
			if (count > 0) {
				while (count-- > 0) {
					ps.addChip(po);
					if((replay==replayMode.Single) && dropped!=null)
					{	animationStack.push(dropped);
						animationStack.push(ps);
					}
				}
			} else {
				ps.insertChipAtIndex(-count - 1, po);
				if((replay==replayMode.Single) && dropped!=null)
				{	animationStack.push(dropped);
					animationStack.push(ps);
				}

			}
		}
	}

	void spendGold(int player, int amount,replayMode replay) {
		PlayerBoard p = playerBoards[player];
		p.spendGold(amount,replay);
	}

	void spendCamels(int player, int amount,replayMode replay) {
		PlayerBoard p = playerBoards[player];
		p.spendCamels(amount,replay);
	}

	private YspahanChip moveFromTo(YspahanCell from, YspahanCell to, int n,replayMode replay) {
		YspahanChip v = null;
		while (n-- > 0) {
			to.addChip(v = from.removeTop());
			if(replay!=replayMode.Replay)
			{
			animationStack.push(from);
			animationStack.push(to);
			}		
		}

		return (v);
	}

	private void sendCubeToCaravan(YspahanCell myCubes, boolean undo,boolean usingCard,replayMode replay)
	{	if(!undo) { scoreCaravanIfFull(replay); }
		YspahanCell caravanCubes = undo ? caravan.lastFilledCell() : caravan
				.firstEmptyCell();
		YspahanCell from = undo ? caravanCubes : myCubes;
		YspahanCell to = undo ? myCubes : caravanCubes;
		YspahanChip chip = moveFromTo(from, to, 1,replay);
		int points = caravan.immediateVP(caravanCubes);
		PlayerBoard movingPlayer = playerBoards[playerWithColor(chip)];
		movingPlayer.addVP(undo ? -points : points);
		if (movingPlayer.ownsExtraCardBuilding()&& !usingCard) {
			YspahanCell playercards = movingPlayer.pmisc[ypmisc.card.index];
			//
			// under unusual circumstances, there are no cards to take, so 
			// the player goes without.  SInce we only get here at the "done"
			// stage, there is no need for undo of this action, or to keep track
			// of the fact that we did or didn't get a card.
			//
			G.Assert(!undo,"cant get here when undoing");
			if(cards.height()>0){
				moveFromTo(cards, playercards, 1,replay);
			}
		}

	}

	private void doTransfers(YspahanCell droppedDest, boolean undo,replayMode replay) {
		if (board_state == ystate.PUZZLE_STATE) {
			return;
		}
		if (droppedDest == discards) { // handle card actions
			YspahanChip top = discards.topChip();
			ycard card = top.getCard();
			switch (card) {
			case card_3_camels: {
				YspahanCell mycamels = playerBoards[whoseTurn].pmisc[ypmisc.camel.index];
				YspahanCell from = undo ? mycamels : camels;
				YspahanCell to = undo ? camels : mycamels;
				moveFromTo(from, to, 3,replay);
				if(from==camels)
					{while (from.height() < 5) {
						from.addChip(ymisc.camel.chip);
					}
				}
			}
				break;
			case card_3_gold: {
				YspahanCell mygold = playerBoards[whoseTurn].pmisc[ypmisc.gold.index];
				YspahanCell from = undo ? mygold : gold;
				YspahanCell to = undo ? gold : mygold;
				moveFromTo(from, to, 3,replay);
				if(from==gold)
				{ while (from.height() < 5) {
						from.addChip(ymisc.gold.chip);
					}
				}
			}
				break;
			case card_buy_no_camels:
				playerBoards[whoseTurn].addBuildNoCamels(undo ? -1 : 1);
				break;
			case card_buy_no_gold:
				playerBoards[whoseTurn].addBuildNoGold(undo ? -1 : 1);
				break;
			case card_place_caravan: {
				YspahanCell myCubes = playerBoards[whoseTurn].pmisc[ypmisc.cubes.index];
				sendCubeToCaravan(myCubes, undo,true,replay);
			}
				break;
			case card_swap_camels_gold:
			case card_place_board:
			case card_score_gold:
			case card_score_camels: // these three are not simple transfers
				break;
			default:
				throw G.Error("Card %s not handled", card);
			}
		} else {
			switch (board_state) {
			case PREBUILD_STATE:
				break;
			case BUILD_STATE:
				G.Assert(
						droppedDest.rackLocation == yrack.Building_Track,
						"dropped on a building");
				playerBoards[whoseTurn].payForBuilding(droppedDest.building,undo,replay);
				break;
			case CARD_TRADE_CAMELS_GOLD:
				cardTradeCount += ((droppedDest.type == yclass.gold) != undo) ? 1
						: -1;
				break;
			case CARD_SCORE_GOLD:
					{playerBoards[whoseTurn].addVP(undo ? -1 : 1);
					 cardTradeCount += (undo ? -1 : 1);
					}
					G.Assert((cardTradeCount>=0) && (cardTradeCount<=10),"card trade count out of range");
				break;
			case CARD_SCORE_CAMELS:
					{playerBoards[whoseTurn].addVP(undo ? -2 : 2);
					 cardTradeCount += (undo ? -1 : 1);
					}
					G.Assert((cardTradeCount>=0) && (cardTradeCount<=4),"card trade count out of range");
				break;
			case PAY_CAMEL_STATE:
				if (droppedDest == camels) {
					YspahanCell source = getSource();
					int player = (source.col-'A');
					G.Assert(player == whoseTurn,
							"should be the turn of the player moving");
				}
				break;
			case ROLL_STATE: {
				YspahanCell pickedSource = pickedSourceStack.top();

				if (pickedSource.col != droppedDest.col) { // need to pay to
															// make a die
															// transfer
					int amount = pickedSource.col - droppedDest.col;
					spendGold(whoseTurn, undo ? -amount : amount,replay);
				}
			}
				break;
			default:
			}
		}

	}

	//
	// drop the floating object.
	//
	private void dropObject(YspahanCell c, int depth,replayMode replay) {
		G.Assert(pickedObject != null, "nothing to drop");

		if (cards.height() == 0) {
			reshuffleDiscards(); // sneak in the reshuffle
		}
		droppedDestStack.push(c);
		droppedStateStack.push(board_state);
		if (pickedObject == ymisc.supervisor.chip) {
			nextSupervisor = c;
		}
		int count = pickedObjectCount.top();
		droppedObjectCount.push(count);
		if (count > 0) {
			while (count-- > 0) {
				c.addChip(pickedObject);
				if(replay==replayMode.Single)
				{	animationStack.push(pickedSourceStack.top());
					animationStack.push(c);
				}
			}
		} else {
			c.addChip(pickedObject); // just one
			if(replay==replayMode.Single)
			{	animationStack.push(pickedSourceStack.top());
				animationStack.push(c);
			}
		}
		doTransfers(c, false,replay);
		pickedObject = null;
	}

	//
	// true if col,row is the place where something was dropped and not yet
	// confirmed.
	// this is used to mark the one square where you can pick up a marker.
	//
	public boolean isDest(YspahanCell cell) {
		return ((droppedDestStack.size() > 0) && (droppedDestStack.top() == cell));
	}

	public YspahanCell getSource() {
		return ((pickedSourceStack.size() > 0) ? pickedSourceStack.top() : null);
	}

	public YspahanCell getDest() {
		return ((droppedDestStack.size() > 0) ? droppedDestStack.top() : null);
	}

	public boolean isSource(YspahanCell cell) {
		return ((pickedSourceStack.size() > 0) && (pickedSourceStack.top() == cell));
	}

	// get the index in the image array corresponding to movingObjectChar
	// or HitNoWhere if no moving object. This is used to determine what
	// to draw when tracking the mouse.
	// Caution! This method is called in the mouse process
	public int movingObjectIndex() {
		YspahanChip ch = pickedObject;
		if (ch != null) {
			return (ch.chipNumber() + Math
					.max(0, (pickedObjectCount.topz(0) - 1)) * 100);
		}
		return (NothingMoving);
	}

	// get the local cell designated by source, col, and row.
	public YspahanCell getLocalCell(yrack source, char col, int row) {
		switch (source) {
		case Card_Stack:
			return (cards);
		case Discard_Stack:
			return (row == 1 ? discardStack : discards);
		case Camel_Pool:
			return (camels);
		case Gold_Pool:
			return (gold);
		case Bag_Neighborhood:
		case Chest_Neighborhood:
		case Barrel_Neighborhood:
		case Vase_Neighborhood:
		case Time_Track:
		case Supervisor_Track:
			return (getMainBoardCell(source, col, row));
		case Caravan_Track:
			return (caravan.caravan[row]);
		case Dice_Table:
			return ((col == 'A') ? diceTable.dice[row]
					: diceTable.extraDice[row]);
		case Building_Track: {
			PlayerBoard pl = playerBoards[col - 'A'];
			return (pl.buildings[row]);
		}
		case Misc_Track: {
			PlayerBoard pl = playerBoards[col - 'A'];
			return (pl.pmisc[row]);
		}
		case Dice_Tower:
			switch (col) {
			case 'A':
				return (diceTower[row]); // main stack of dice as rolled
			case 'B':
				return (diceTowerExtraGold[row]); // gold played to get extra
													// dice for supervisor
													// movement
			case 'C':
				return (diceTowerExtraCard[row]); // card played to get an extra
													// cube
			default:
				throw G.Error("Not expecting col=%c", col);
			}
		default:
			throw G.Error("Not expecting source %s", source);
		}
	}

	// get the cell in this board which has the same location as a cell from
	// anothe board
	public YspahanCell getCell(YspahanCell c) {
		return ((c == null) ? null : getLocalCell(c.rackLocation(),c.col, c.row));
	}

	// pick something up. Note that when the something is the board,
	// the board location really becomes empty, and we depend on unPickObject
	// to replace the original contents if the pick is cancelled.
	private void pickObject(YspahanCell c, int depth) {
		G.Assert((pickedObject == null), "something is already picked");
		pickedStateStack.push(board_state);
		pickedSourceStack.push(c);
		pickedObject = (depth < 0) ? c.removeTop() : c.removeChipAtIndex(depth);
		int count = 1;
		switch (pickedObject.type) {
		case gold:
			if (c == gold) {
				count = takeGoldCount();
				// make sure there's enough gold. Strange rolls of the dice can
				// require
				// up to 14 gold in one go. We want to leave a few for aesthetic
				// reasons
				while (((c.height() - 5) < count)) {
					c.addChip(pickedObject);
				}
			}
			break;
		case camels:
			if (c == camels) {
				count = takeCamelCount();
				// make sure there's enough camels. Strange rolls of the dice
				// can require
				// up to 15 camels in one go. We want to leave a few for
				// aesthetic reasons
				while ((c.height() - 5) < count) {
					c.addChip(pickedObject);
				}
			}
			break;
		case playerCubes:
			if (c.rackLocation == yrack.Misc_Track) { // player cubes, we
															// can't run out.
				while ((c.height() - 5) < count) {
					c.addChip(pickedObject);
				}
			}
			break;
		case cards:
			if ((c == cards) || (c == discards)) {
			} else { // players are allowed to pick from their pack
				count = (depth < 0) ? 1 : (-depth - 1);
			}
			break;
		default:
			break;
		}
		pickedObjectCount.push(count);
		if (count > 0) {
			while (count-- > 1) {
				c.removeTop();
			}
		} else {
		}
		;
		if (pickedObject == ymisc.supervisor.chip) {
			nextSupervisor = null;
		}
		if (board_state == ystate.CARD_TRADE_CAMELS_GOLD) {
			if (pickedObject.type == yclass.camels) {
				pickedObject = ymisc.gold.chip;
			} else if (pickedObject.type == yclass.gold) {
				pickedObject = ymisc.camel.chip;
			}
		}

	}

	// undo reshuffling the deck.
	private void undoReshuffle() {
		int h = cards.height();
		G.Assert(droppedDestStack.top() == discardStack,
				"should be undoing reshuffle");
		droppedDestStack.pop();
		droppedObjectCount.pop();
		while (h-- > 0) { //
							// remove the cards from the card stack, but replace
							// them in the discard pile
							// from the discard stack, so they retain their
							// original order, which prevents
							// leaking any informtion about the shuffle by the
							// way the discard pile is reordered.
							//
			cards.removeTop();
			discards.addChip(discardStack.removeTop());
		}
	}

	// reshuffle the deck
	private void reshuffleDiscards() {
		int h = discards.height();
		if (h > 0) {
			while (h-- > 0) {
				YspahanChip disc = discards.removeTop();
				cards.addChip(disc);
				discardStack.addChip(disc);
			}
			// mark the destination stack with the discardStack as a token that
			// it needs to be undone
			droppedDestStack.push(discardStack);
			droppedObjectCount.push(1);

			cards.shuffle(new Random(randomKey + gameDay + 5424));
		}
	}

	// return the number of cubes to be placed, considering the buildings and
	// possible payment of a card
	public int cubePlacementCount() {
		if (selectedDice == null) {
			return (1);
		}
		int count = selectedDice.height();
		int row = selectedDice.row;
		count += diceTowerExtraCard[row].height(); // paid a card
		if (playerBoards[whoseTurn].ownsExtraCubeBuilding()) {
			count++;
		}
		return (count);
	}

	// distance the supervisor can move based on the die selection
	public int supervisorCount() 
	{
		if(selectedDice!=null)
			{ YspahanChip top  = selectedDice.topChip();
			  if(top!=null) { return(top.dieValue()); }
			}
		 return(-1);
	}

	// extra supervisor movement considering building and payment of gold.
	public int extraSupervisorCount() {
		if (selectedDice == null) {
			return (0);
		}
		int row = selectedDice.row;
		int payment = diceTowerExtraGold[row].height();
		if (playerBoards[whoseTurn].ownsSupervisorBuilding()) {
			payment += 3;
		}
		return (payment);
	}

	// return the number of gold to take considering buildings and game state
	public int takeGoldCount() {
		if (selectedDice == null) {
			return (1);
		}
		if (selectedDice.row != ydicetower.take_gold.towerIndex) {
			return (1);
		}
		if (board_state == ystate.CARD_TRADE_CAMELS_GOLD) {
			return (1);
		}
		int count = selectedDice.height();
		if (playerBoards[whoseTurn].ownsExtraGoldBuilding()) {
			count += 2;
		}
		int row = selectedDice.row;
		count += diceTowerExtraCard[row].height(); // paid a card
		return (count);
	}

	// return the number of camels to take considering buildings and game state
	public int takeCamelCount() {
		if (selectedDice == null) {
			return (1);
		}
		if (selectedDice.row != ydicetower.take_camels.towerIndex) {
			return (1);
		}
		if (board_state == ystate.CARD_TRADE_CAMELS_GOLD) {
			return (1);
		}
		int count = selectedDice.height();
		if (playerBoards[whoseTurn].ownsExtraCamelBuilding()) {
			count++;
		}
		int row = selectedDice.row;
		count += diceTowerExtraCard[row].height(); // paid a card
		return (count);
	}

	// return true if there are no more legal placements in the current
	// neighborhood
	public boolean noLegalPlacements() {
		Hashtable<YspahanCell, YspahanCell> dests = new Hashtable<YspahanCell, YspahanCell>();
		getNeighborhoodDests(dests);
		return (dests.size() == 0);
	}

	// count the number of cells adjacent to a supervisor
	// square that contain cubes of any color.
	private int nAdjacentCubes(YspahanCell dest) {
		int n = 0;
		for (int dir = 0; dir < dest.geometry.n; dir++) {
			YspahanCell c = dest.exitTo(dir);
			if ((c != null) && (c != protectedCube) // protected cube doesn't
													// exist
					&& (c != protectedCube2) // protected cube doesn't exist
					&& (c.type == yclass.playerCubes) && (c.height() > 0)) {
				n++;
			}
		}
		return (n);
	}

	// select any adjacent cube, normally called when there
	// is known to be 0 or 1 adjacent
	public YspahanCell selectAdjacentCube(YspahanCell dest) {
		for (int dir = 0; dir < dest.geometry.n; dir++) {
			YspahanCell c = dest.exitTo(dir);
			if ((c != null) && (c != protectedCube) // protected cube doesn't
													// exist
					&& (c != protectedCube2) // protected cube doesn't exist
					&& (c.type == yclass.playerCubes) && (c.height() > 0)) {
				return (c);
			}
		}
		return (null);
	}

	//
	// after moving the supervisor, there are initially 0-2 cubes to be sent
	// to the caravan. As cubes are sent this declines to 1 and 0.
	//
	private void setNextSupervisorMoveState(YspahanCell dest) {
		int nAdj = nAdjacentCubes(dest);
		switch (nAdj) {
		case 0:
			setState(ystate.PREBUILD_STATE);
			break;
		case 1:
			selectedCube = selectAdjacentCube(dest);
			setState(ystate.DESIGNATED_CUBE_STATE);
			break;
		case 2:
			setState(ystate.DESIGNATE_CUBE_STATE);
			break;
		default:
			break;
		}
	}

	//
	// chug through the state machine to the next game state after an object is
	// dropped.
	// this is only for drops in the "forward" logical direction, undos do not
	// get here.
	//
	private void setNextStateAfterDrop() {
		YspahanCell dest = getDest();
		switch (board_state) {
		default:
			throw G.Error("Not expecting drop in state %s", board_state);
		case CARD_SCORE_GOLD:
			if (cardTradeCount == 10) // 10 coins plus the card itself
			{
				setState(ystate.CONFIRM_CARD_STATE);
			}
			break;
		case CARD_SCORE_CAMELS:
			if (cardTradeCount == 4) // max camels to discard is 4, plus the
										// card itself
			{
				setState(ystate.CONFIRM_CARD_STATE);
			}
			break;
		case BUILD_STATE:
			setState(ystate.CONFIRM_STATE);
			break;
		case PLAY_CARD_EFFECT_STATE:
			{
				YspahanChip top = discards.topChip();
				setState(top.getCard().newstate);
			}
			break;
		case PLAY_CARD_DICE_STATE:
			if (dest == discards) {
				YspahanChip top = discards.topChip();
				setState(top.getCard().newstate);
			} else {
				if(selectedDice==null)
					{selectedDice = diceTower[dest.row];
					 ydicetower dv = selectedDice.diceTower;
					 setState(dv.threewayState);
					}
				setState(selectedDice.diceTower.onewayState);
			}
			break;
		case THREEWAY_TAKE_CAMEL_STATE: // three way selection of card, play, or
										// move
		case THREEWAY_TAKE_GOLD_STATE:
		case THREEWAY_PLACE_BAG_STATE:
		case THREEWAY_PLACE_BARREL_STATE:
		case THREEWAY_PLACE_CHEST_STATE:
		case THREEWAY_PLACE_VASE_STATE:
		case PREBUILD_STATE:
			break;
		case PAY_CAMEL_STATE:
			setState(ystate.PAID_CAMEL_STATE);
			protectedCube2 = protectedCube;
			protectedCube = selectedCube;
			break;
		case MOVE_SUPERVISOR_STATE:
			if (dest.type == yclass.gold) {
				if(selectedDice==null) { selectedDice = diceTower[dest.row]; };
				break;
			}
			{
				setNextSupervisorMoveState(dest);
				break;
			}
		case TAKE_CARD_STATE:
		case TAKE_CAMEL_STATE:
		case TAKE_GOLD_STATE:
			setState(ystate.PREBUILD_STATE);
			break;
		case CARD_TRADE_CAMELS_GOLD:
			break;
		case CARD_PLACE_CUBE_SOUK:
			setState(ystate.CONFIRM_CARD_STATE);
			break;
		case PLACE_BAG_STATE:
		case PLACE_VASE_STATE:
		case PLACE_BARREL_STATE:
		case PLACE_CHEST_STATE: {
			if ((cubePlacementCount() == (droppedDestStack.size() - diceTowerExtraCard[selectedDice.row]
					.height())) // remember, if he went for an extra cube using
								// a card, he placed a card on the stack
					|| noLegalPlacements()) {
				setState(ystate.PREBUILD_STATE);
			}
		}
			break;
		case ROLL_STATE:
			break;

		case PUZZLE_STATE:
			acceptPlacement();
			break;
		}
	}

	//
	// roll the dice. Make the rolls deterministic based on the game turn and
	// the game random key
	//
	private void doRoll(YspahanMovespec m) {
		diceTable.doRoll(new Random(randomKey + gameDay * 10 + 1));
		diceTable.distribute(diceSorter);
		int lowest = 0;
		int highest = diceSorter.length - 1;
		while (diceSorter[lowest].topChip() == null) {
			lowest++;
		}
		while (diceSorter[highest].topChip() == null) {
			highest--;
		}


		for (YspahanCell die : diceTower) {
			die.reInit();
		}
		// copy contents only, not cell ids
		diceTower[ydicetower.take_camels.towerIndex]
				.copyFrom(diceSorter[lowest]);
		if (lowest < highest) {
			diceTower[ydicetower.take_gold.towerIndex]
					.copyFrom(diceSorter[highest]);
		}
		lowest++;
		while ((lowest < highest) && (diceSorter[lowest].height() == 0)) {
			lowest++;
		}
		if (lowest < highest) {
			diceTower[ydicetower.place_bag.towerIndex]
					.copyFrom(diceSorter[lowest]);
			lowest++;
		}
		while ((lowest < highest) && (diceSorter[lowest].height() == 0)) {
			lowest++;
		}
		if (lowest < highest) {
			diceTower[ydicetower.place_barrel.towerIndex]
					.copyFrom(diceSorter[lowest]);
			lowest++;
		}
		while ((lowest < highest) && (diceSorter[lowest].height() == 0)) {
			lowest++;
		}
		if (lowest < highest) {
			diceTower[ydicetower.place_chest.towerIndex]
					.copyFrom(diceSorter[lowest]);
			lowest++;
		}
		while ((lowest < highest) && (diceSorter[lowest].height() == 0)) {
			lowest++;
		}
		if (lowest < highest) {
			diceTower[ydicetower.place_vase.towerIndex]
					.copyFrom(diceSorter[lowest]);
			lowest++;
		}
		
		if (m != null) {
			String roll = "Roll: ";
			for (int i = 0; i < diceTower.length; i++) {
				int h = diceTower[i].height();
				if (h > 0) {
					roll += ""+h+" "+ rowDesc[i]+",";
				}
			}
			m.setLineBreak(true);
			m.moveInfo = roll;
		}
	}

	// remove the yellow dice and any cards or gold the user placed in payment.
	private void removeExtraDiceItems() {
		for (YspahanCell dcell : diceTower) {
			YspahanChip top = dcell.topChip();
			while ((top != null) && top.isYellow()) {
				dcell.removeTop();
				top = dcell.topChip();
			}
		}
		for (YspahanCell dcell : diceTowerExtraGold) {
			while (dcell.height() > 0) {
				gold.addChip(dcell.removeTop());
			}
		}
		for (YspahanCell dcell : diceTowerExtraCard) {
			while (dcell.height() > 0) {
				discards.addChip(dcell.removeTop());
			}
		}
	}

	//
	// return true if there are any dice left in the dice tower. We need to know
	// because the player has to take a card if there are no dice left.
	//
	private boolean someDiceRemain() {
		for (int lim = diceTower.length - 1; lim >= 0; lim--) {
			if (diceTower[lim].height() > 0) {
				return (true);
			}
		}
		return (false);
	}

	// a "done" is required when an action is to be committed. Mainly
	// this is between the main action and the building action, but also
	// some special cases like sending cubes to the caravan.
	private void setNextStateAfterDone(YspahanMovespec m,replayMode replay) {
		ystate ccundo = confirmCardUndoState;
		cardTradeCount = 0;
		confirmCardUndoState = null;
		if (cards.height() == 0) {
			reshuffleDiscards();
			acceptPlacement();
		}
		switch (board_state) {
		default:
			throw G.Error("Not expecting Done in state %s", board_state);

		case GAMEOVER_STATE:
			break;

		case PAY_CAMEL_STATE:
		case PAID_CAMEL_STATE:
			YspahanCell cube = playerBoards[whoseTurn].pmisc[ypmisc.cubes.index];
			sendCubeToCaravan((board_state==ystate.PAY_CAMEL_STATE)?selectedCube:cube, false,false,replay);
			selectedCube = null;
			whoseTurn = payCamelReturnPlayer;
			payCamelReturnPlayer = -1;
			setNextSupervisorMoveState(supervisor);
			break;
		case DESIGNATED_CUBE_STATE: {
			int cubePlayer = playerWithColor(selectedCube.topChip());
			if (playerBoards[cubePlayer].camelCount() > 0) {
				payCamelReturnPlayer = whoseTurn;
				whoseTurn = cubePlayer;
				setState(ystate.PAY_CAMEL_STATE);
			} else {
				sendCubeToCaravan(selectedCube, false,false,replay);
				selectedCube = null;
				setNextSupervisorMoveState(supervisor);
			}
		}
			break;
		case ROLL_STATE:
			doRoll(m);
			setState(resetState=ystate.SELECT_STATE);
			resetDice = selectedDice;
			break;
		case CONFIRM_CARD_STATE:
		case CARD_TRADE_CAMELS_GOLD:
		case CARD_SCORE_CAMELS:
		case CARD_SCORE_GOLD:
			scoreCaravanIfFull(replay);
			setState(resetState=ccundo);
			resetDice = selectedDice;
			break;
		case PREBUILD_STATE:
			scoreCaravanIfFull(replay);
			protectedCube = protectedCube2 = null; // no longer protected
			setState(ystate.BUILD_STATE);
			break;
		case PASS_STATE:
			m.moveInfo = PASS;
			setState(ystate.BUILD_STATE);
			break;
		case CONFIRM_STATE:
		case BUILD_STATE:
			scoreCaravanIfFull(replay);
			playerBoards[whoseTurn].doDone();
			if (selectedDice != null) {
				selectedDice.reInit();
				removeExtraDiceItems();
				selectedDice = null;
			}
			setNextPlayer();
			{

				if (whoseTurn == startPlayer) {
					if ((gameDay % 7) == 0) {
						scoreSouksEndOfWeek(replay);
						scoreCaravan();
					}
					if (gameDay >= 21) {
						scoreEndOfGame();
					} else {
						setState(ystate.ROLL_STATE);
						diceTable.resetExtraDice(); // move the yellow dice back
													// to the not-selected state
					}
				} else {
					setState(someDiceRemain() ? ystate.SELECT_STATE
							: ((cards.height() > 0) ? ystate.TAKE_CARD_STATE
									: ystate.PASS_STATE));
					resetState = board_state;
					resetDice = null;
				}
			}
			break;
		case PUZZLE_STATE:
			break;
		}
		
	}

	// score the caravan if we just filled it up.
	private void scoreCaravanIfFull(replayMode replay) { // score a caravan if it's full
		if (caravan.isFull()) {
			scoreCaravan();
			caravan.emptyCaravan(replay);
		}
	}

	// score a caravan, when full or at the end of a week.
	private void scoreCaravan() {
		for (ycube cube : ycube.values()) {
			int pc = playerWithColor(cube.chip,-1);
			if(pc!=-1)
			{
			int sc = caravan.score(cube.chip);
			playerBoards[pc].addVP(sc); }
		}
	}


	private void doDone(YspahanMovespec m,replayMode replay) {
		acceptPlacement();

		if (board_state==ystate.RESIGN_STATE) {
			throw G.Error("resigning is not permitted in a multiplayer game");
		} else {
			setNextStateAfterDone(m,replay);
		}
	}

	//
	// make state changes after something is picked up. Usually this is nothing,
	// but
	// in the main action what you pick up locks you into a particualar action
	// type; ie
	// picking up a card means you will take a card and so on.
	//
	public void setNextStateAfterPick() {
		switch (board_state) {
		case DESIGNATE_CUBE_STATE: // picking a victim to send to the caravan
		case DESIGNATED_CUBE_STATE:
			setState((selectedCube==null) ? ystate.DESIGNATE_CUBE_STATE:ystate.DESIGNATED_CUBE_STATE);
			break;
		case BUILD_STATE: // in building phase, you are allowed to play cards or
							// build a single building
			switch (pickedObject.type) {
			case cards:
				confirmCardUndoState = board_state;
				setState(ystate.PLAY_CARD_EFFECT_STATE);
				break;
			case playerCubes:
				break;
			default:
				throw G.Error("shouldn't have hit " + pickedObject.type);
			}
			break;
		case SELECT_STATE:
		case ROLL_STATE:
		case THREEWAY_TAKE_CAMEL_STATE: // three way selection of card, play, or
										// move
		case THREEWAY_TAKE_GOLD_STATE:
		case THREEWAY_PLACE_BAG_STATE:
		case THREEWAY_PLACE_BARREL_STATE:
		case THREEWAY_PLACE_CHEST_STATE:
		case THREEWAY_PLACE_VASE_STATE:
			switch (pickedObject.type) {
			default:
				throw G.Error("Not expecting pickedobject type %s", pickedObject.type);
			case dice:	break;	// moving extra dice on the table
			case gold: {
				setState((getSource() == gold) ? selectedDice.diceTower.onewayState
						: ystate.MOVE_SUPERVISOR_STATE); // paying gold to move
															// the supervisor
			}
				break;
			case camels: // taking camels
			case playerCubes: // use the main tower function
				setState(selectedDice.diceTower.onewayState);
				break;
			case cards: // can always take a card
			{
				ystate newstate = (getSource() == cards) ? ystate.TAKE_CARD_STATE
						: ystate.PLAY_CARD_DICE_STATE;
				if (newstate == ystate.PLAY_CARD_DICE_STATE) {
					confirmCardUndoState = board_state;
				}
				setState(newstate);
			}
				break;
			case supervisor: // or move the supervisor
				setState(ystate.MOVE_SUPERVISOR_STATE);
				break;
			}
			break;
		default:
			break;
		}
	}

	// this is a very special case. If the user has paid enough gold
	// or has the extra supervisor movement building, he should be able
	// to move the supervisor "zero spaces" and hit the same souks again.
	// unfortunately, this conflicts with the move/unmove paradigm which
	// would consider this case to be a takeback rather than a move forward.
	public boolean movingSupervisorZero(YspahanCell c) {
		switch (board_state) {
		default:
			break;
		case MOVE_SUPERVISOR_STATE:
			return ((c == supervisor) && ((supervisorCount() - extraSupervisorCount()) <= 0));
		}
		return (false);
	}


	public boolean Execute(commonMove mm,replayMode mode)
	{	YspahanMovespec m = (YspahanMovespec)mm;
		// G.print("E "+m+" for "+whoseTurn+" "+startPlayer+" "+board_state+" "+confirmCardUndoState);
		switch (m.op) {
		case MOVE_VIEWCARDS:
			playerBoards[whoseTurn].viewCardCount++;
			break;
		case MOVE_DONE:
			G.Assert(DoneState()||board_state==ystate.PAY_CAMEL_STATE, "%s is not a legal Done state", board_state);
			doDone(m,mode);
			break;

		case MOVE_BOARD_BOARD: { // Ro modified
			YspahanCell src = getLocalCell(m.source, m.from_col, m.from_row);
			YspahanCell dest = getLocalCell(m.dest, m.to_col, m.to_row);
			// for using cards depth is the type of the card - not the position;
			// compute the position
			int cardInx = -1;
			if (m.source == yrack.Misc_Track && m.from_row == (ypmisc.card.index) && m.depth >= 0) { // use
																					// cards
				boolean found = false;
				for (int i = 0; i < src.chipStack.length; i++) {
					ycard card = (src.chipAtIndex(i) == null ? null : src
							.chipAtIndex(i).getCard());
					if (card != null && card.ordinal() == m.depth) {
						cardInx = i;
						found = true;
						break;
					}
				}
				if (!found) {
					throw G.Error("Card does not exist(Board.Execute): " + m);
				} // do nothing
			}
			if((src==droppedDestStack.top()) && (dest==pickedSourceStack.top()))
			{
				YspahanCell dr = unDropObject();
				unPickObject(replayMode.Single,dr);
			}else
			{
			pickObject(src, cardInx);
			setNextStateAfterPick();
			// change the replay so we animate even in direct play
			dropObject(dest, -1,(mode==replayMode.Live?replayMode.Single:mode));
			setNextStateAfterDrop();
			}
		}
			break;

		case MOVE_DROP: // drop on chip pool;
		{
			YspahanCell c = getLocalCell(m.dest, m.to_col, m.to_row);
			if (isSource(c) && (!movingSupervisorZero(c))) {
				unPickObject(mode,null);
			} else {
				dropObject(c, -1,mode);
				setNextStateAfterDrop();
			}
		}
			break;

		case MOVE_PICK: {
			YspahanCell c = getLocalCell(m.source, m.from_col, m.from_row);
			if (isDest(c)) {
				unDropObject();
			} else if ((board_state != ystate.PUZZLE_STATE)
					&& (m.source == yrack.Dice_Tower)
					&& (c.type == yclass.dice)) {
				selectedDice = c;
				ydicetower dv = c.diceTower;
				setState(dv.threewayState);
				break;
			} else if ((board_state == ystate.DESIGNATE_CUBE_STATE)
					|| (board_state == ystate.DESIGNATED_CUBE_STATE)) {
				selectedCube = (selectedCube==c)?null:c;
				setNextStateAfterPick();
			} else {
				pickObject(c, m.depth);
				setNextStateAfterPick();
			}
		}
			break;

		case MOVE_START:
			deriveGameDay();
			selectedCube = protectedCube2 = protectedCube = null;
			setWhoseTurn(m.player);
			setStartPlayer(m.player);
			acceptPlacement();
			unPickObject(replayMode.Replay,null);
			// standardize the gameover undoInfo. Particularly importing if the
			// sequence in a game is resign/start
			selectedDice = null;
			setState(ystate.ROLL_STATE);
			break;

		case MOVE_RESIGN:
			setState(unresign==null?ystate.RESIGN_STATE:unresign);
			break;
		case MOVE_EDIT:
			acceptPlacement();
			// standardize "gameover" is not true
			setState(ystate.PUZZLE_STATE);

			break;
			
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(ystate.GAMEOVER_STATE);
			break;

		default:
			cantExecute(m);
		}


		return (true);
	}

	// recursive step the supervisor, note the legal stopping positions by
	// adding positions
	// to the hash table.
	private void stepSuper(YspahanCell start, int direction,
			Hashtable<YspahanCell, YspahanCell> dests, int min, int max) {
		YspahanCell next = start.exitTo(direction);
		if (next != null) {
			min--;
			max--;
			if (min <= 0) {
				dests.put(next, next);
				min++;
			} // a place we can go
			if (max > 0) {
				if (next == hub) { // if we reach the hub, step all ways except
									// back where we came. Conventionally,
									// back is halfway around in in the
									// direction circle.
					int gates = next.geometry.n;
					int reverse = (direction + gates / 2) % gates;
					for (int nextdir = 0; nextdir < next.geometry.n; nextdir++) {
						if (nextdir != reverse) {
							stepSuper(next, nextdir, dests, min, max);
						}
					}
				} else { // other than at the hub, just keep on the same
							// direction
					stepSuper(next, direction, dests, min, max);
				}
			}
		}
	}
	//
	// this is true in the states where we can place cubes on souks
	//
	boolean canClickOnSouk()
	{
		switch(board_state)
		{
		default: return(false);
		case THREEWAY_PLACE_BAG_STATE:
		case THREEWAY_PLACE_VASE_STATE:
		case THREEWAY_PLACE_BARREL_STATE:
		case THREEWAY_PLACE_CHEST_STATE:
		case PLACE_BAG_STATE:
		case PLACE_VASE_STATE:
		case PLACE_BARREL_STATE:
		case PLACE_CHEST_STATE:
		case CARD_PLACE_CUBE_SOUK:
			return(true);
		}
	}

	//
	// get all destinations for the current state and picked object.
	// ** this isn't logically complete, if only handles the situations where it
	// is currently called **
	//
	Hashtable<YspahanCell, YspahanCell> getDests() {
		Hashtable<YspahanCell, YspahanCell> dests = new Hashtable<YspahanCell, YspahanCell>();
		if(pickedObject==null)
		{  if(canClickOnSouk())
				{
				getNeighborhoodDests(dests);
				}
		}
		else if (board_state != ystate.PUZZLE_STATE) 
		{
			if (pickedObject == ymisc.supervisor.chip) {
				getSupervisorDests(dests);
			}
			if ((board_state != ystate.BUILD_STATE)
					&& (pickedObject.type == yclass.playerCubes)) {
				getNeighborhoodDests(dests);
			}
		}
		return (dests);
	}

	//
	// get the legal destinations for a supervisor being moved.
	//
	Hashtable<YspahanCell, YspahanCell> getSupervisorDests(
			Hashtable<YspahanCell, YspahanCell> dests) {
		G.Assert(selectedDice != null, "no group of dice selected");
		boolean ownsBuilding = playerBoards[whoseTurn].ownsSupervisorBuilding();
		int dieCount = selectedDice.topChip().dieValue();
		int mincount = dieCount;
		int maxcount = dieCount;
		int gold = diceTowerExtraGold[selectedDice.row].height();
		mincount -= gold;
		maxcount += gold;
		if (ownsBuilding) {
			mincount -= 3;
			maxcount += 3;
		}
		mincount = Math.max(0, mincount);

		for (int direction = 0; direction < supervisor.geometry.n; direction++) {
			stepSuper(supervisor, direction, dests, mincount, maxcount);
		}
		return (dests);
	}

	// get the legal destinations for a cube being placed in a souk
	void getBuildingDests(Hashtable<YspahanCell, YspahanCell> dests) {
		playerBoards[whoseTurn].getBuildingDests(dests);
	}

	// return a shift factor used to make a bitmask for any souk
	// this is really ad-hoc, just a bitmask where each bit corresponds to a souk in some neighborhood
	private int nidShift(YspahanCell c) {
		yrack loc = c.rackLocation();
		switch (loc) {
		default:
			throw G.Error("Not expecting racklocation %s", loc);
		case Bag_Neighborhood:
			return (0);
		case Chest_Neighborhood:
			return (5);
		case Vase_Neighborhood:
			return (10);
		case Barrel_Neighborhood:
			return (15);
		}
	}

	// score completed souks for a player. Called separately for each player,
	// does not
	// change the state of the board.
	int scoreSouks(PlayerBoard pb) {
		YspahanChip myChip = pb.color.chip;
		int empty = 0;
		int score = 0;
		int extra = pb.ownsExtraPointsBuilding() ? 2 : 0;
		YspahanCell currentSouk = null;
		for (YspahanCell c = allCells; c != null; c = c.next) { // we depend on
																// the huts in a
																// souk being
																// sequential
			if (c.type == yclass.playerCubes) {
				if ((currentSouk == null)
						|| (currentSouk.rackLocation != c.rackLocation)
						|| (currentSouk.col != c.col)) {
					if ((currentSouk != null) && (empty == 0)
							&& (currentSouk.topChip() == myChip)) {
						score += currentSouk.soukValue + extra;
					}
					currentSouk = c;
					empty = 0;
				}
				empty += (c.topChip() == null) ? 1 : 0;
			}
		}
		// score the last souk
		if ((currentSouk != null) && (empty == 0)
				&& (currentSouk.topChip() == myChip)) {
			score += currentSouk.soukValue + extra;
		}

		return (score);
	}
	int playerWithColor(YspahanChip p)
	{
		for(PlayerBoard pb : playerBoards)
		{
			if(pb.color==p.getCube()) { return(pb.myIndex); } 
		}
		throw G.Error("No player has this cube %s",p);
	}
	int playerWithColor(YspahanChip p,int ifnone)
	{
		for(PlayerBoard pb : playerBoards)
		{
			if(pb.color==p.getCube()) { return(pb.myIndex); } 
		}
		return(ifnone);
	}

	// clear all the souks and put the cubes back in the players' piles
	void clearSouks(replayMode replay) // init, but give the cubes back
	{
		hub.addChip(supervisor.removeTop()); // put the supervisor back
		supervisor = hub; // and note the new sup location
		for (YspahanCell c = allCells; c != null; c = c.next) {
			if ((c.type == yclass.playerCubes) && (c.topChip() != null)) 
			{	YspahanChip top = c.removeTop();
				YspahanCell dest = playerBoards[playerWithColor(top)].pmisc[ypmisc.cubes.index];
				dest.addChip(top);
				if(replay!=replayMode.Replay)
				{
					animationStack.push(c);
					animationStack.push(dest);
				}
			}
		}
	}

	// do end of week scoring, for souks, and clear them.
	void scoreSouksEndOfWeek(replayMode replay)
	{
		for (int i = 0; i < players_in_game; i++) {
			PlayerBoard pb = playerBoards[i];
			int sc = scoreSouks(pb);
			pb.addVP(sc);
		}
		clearSouks(replay);
	}

	void scoreEndOfGame() {
		setState(ystate.GAMEOVER_STATE);
	}

	// accumulate valid souks from a neighborhood
	void accumulateNeighborhoodDests(Hashtable<YspahanCell, YspahanCell> dest,yrack neighborhood) 
	{
		Hashtable<YspahanCell, YspahanCell> dest0 = new Hashtable<YspahanCell, YspahanCell>();
		getNeighborhoodDests(dest0, neighborhood);
		for (Enumeration<YspahanCell> keys = dest0.keys(); keys
				.hasMoreElements();) {
			YspahanCell c = keys.nextElement();
			dest.put(c, c);
		}

	}

	Hashtable<YspahanCell, YspahanCell> getNeighborhoodDests(Hashtable<YspahanCell, YspahanCell> dests) 
	{
		if (board_state == ystate.CARD_PLACE_CUBE_SOUK) {
			// any neighborhood, but must follow the rules for each souk
			accumulateNeighborhoodDests(dests, yrack.Bag_Neighborhood);
			accumulateNeighborhoodDests(dests, yrack.Chest_Neighborhood);
			accumulateNeighborhoodDests(dests, yrack.Vase_Neighborhood);
			accumulateNeighborhoodDests(dests, yrack.Barrel_Neighborhood);
		} else if(selectedDice!=null) 
		{
			G.Assert(selectedDice.diceTower != null,
					"a group of dice is selected");
			getNeighborhoodDests(dests,
					selectedDice.diceTower.rackLocation);
		}
		return (dests);
	}

	// get the cells that are legal placements, considering the dice selection
	// and partially filled souks.
	// this is also used by the "place cube any souk" card.
	private Hashtable<YspahanCell, YspahanCell> getNeighborhoodDests(
			Hashtable<YspahanCell, YspahanCell> dests, yrack neighborhood) {
		int incomplete = 0;
		int available = 0;
		int owned = 0;
		YspahanChip mychip = playerBoards[whoseTurn].color.chip;
		//
		// make a first pass over the neighborhood and collect the occupancy
		// status of each souk
		// the interestging states are (1) owned by someone else (2) owned by no
		// one (3) owned by us and not filled
		for (YspahanCell c = allCells; c != null; c = c.next) {
			if (c.rackLocation == neighborhood) {
				int mask = 1 << ((c.col - 'A') + nidShift(c));
				YspahanChip top = c.topChip();
				if (top == null) {
					available |= mask;
					dests.put(c, c);
				} else if (top == mychip) {
					incomplete |= mask;
				} else // owned by someone else
				{
					owned |= mask;
				}
			}
		}
		// dests contains all the empty cells in the neighborhood
		if ((incomplete & available) != 0) { // partly filled souk, we must
												// continue with the same one
			for (Enumeration<YspahanCell> key = dests.keys(); key
					.hasMoreElements();) {
				YspahanCell c = key.nextElement();
				int mask = 1 << ((c.col - 'A') + nidShift(c));
				if ((mask & incomplete) == 0) {
					dests.remove(c);
				}
			}
		} else { // can start any souk that is not owned
			for (Enumeration<YspahanCell> key = dests.keys(); key
					.hasMoreElements();) {
				YspahanCell c = key.nextElement();
				int mask = 1 << ((c.col - 'A') + nidShift(c));
				if ((mask & owned) != 0) {
					dests.remove(c);
				} // not owned by anyone else
			}
		}
		return (dests);
	}

	// true if this cell is a destination for the sup which is moving
	private boolean isSupervisorDest(YspahanCell c) {
		Hashtable<YspahanCell, YspahanCell> dests = new Hashtable<YspahanCell, YspahanCell>();
		getSupervisorDests(dests);
		return ((dests.get(c) != null));
	}

	// true if this cell is a building that can be built
	private boolean isBuildingDest(YspahanCell c) {
		Hashtable<YspahanCell, YspahanCell> dests = new Hashtable<YspahanCell, YspahanCell>();
		getBuildingDests(dests);
		return ((dests.get(c) != null));
	}

	// true if this cell is a neighborhood cell where the player can play
	private boolean isNeighborhoodDest(YspahanCell c) {
		Hashtable<YspahanCell, YspahanCell> dests = new Hashtable<YspahanCell, YspahanCell>();
		getNeighborhoodDests(dests);
		return ((dests.get(c) != null));
	}
	private boolean legalToHitInRoll(YspahanCell c)
	{
		return ((c.rackLocation == yrack.Dice_Table)
				||((c.rackLocation()==yrack.Misc_Track)
						&&(c.row==ypmisc.card.index)
						&&(c.playerIndex == whoseTurn)));
	}
	//
	// this is the main filter for the user interface which determines what
	// cells
	// can be picked from or dropped to.
	//
	public boolean legalToHitCell(YspahanCell c) {
		if (pickedObject != null) {
			// some object has been picked up and is hovering
			if (pickedObject.type != c.type) {
				return (false);
			} // can never drop on a mismatched cell type
			if (!c.stackable && (c.height() > 0)) {
				return (false);
			} // can never stack 2 on a nonstackable cell
			if (isSource(c)) {
				return (true);
			} // can always drop back whereever we picked from
			switch (board_state) {
			case BUILD_STATE:
				// can drop a cube on the building track if we have the price
				return ((c.rackLocation == yrack.Building_Track)
						&& (c.playerIndex == whoseTurn) 
						&& (isBuildingDest(c)));
			case SELECT_STATE:
				// can play a card before selecting a group of dice
				if (pickedObject.type == yclass.cards) {
					return (c == discards); // play a card
				}
				if(pickedObject.type==yclass.gold)
				{	return(((c.rackLocation == yrack.Dice_Tower)
							&& (diceTower[c.row].height()>0)
							&& (selectedDice==null))	 // play for increase
						|| (c == playerBoards[whoseTurn].pmisc[ypmisc.gold.index]));	
				}
				return (false);
			case THREEWAY_PLACE_BARREL_STATE:
			case THREEWAY_PLACE_BAG_STATE:
			case THREEWAY_PLACE_CHEST_STATE:
			case THREEWAY_PLACE_VASE_STATE:
			case THREEWAY_TAKE_CAMEL_STATE:
			case THREEWAY_TAKE_GOLD_STATE:
				return (false);
			case MOVE_SUPERVISOR_STATE:
				switch (pickedObject.type) {
				case gold:
					// adding more gold before moving the sup
					return ((c == playerBoards[whoseTurn].pmisc[ypmisc.gold.index])
							|| (selectedDice==null)
								? ((c.rackLocation == yrack.Dice_Tower) && (diceTower[c.row].height()>0)) 
								: (c == diceTowerExtraGold[selectedDice.row]) );
				case supervisor:
					return ((c == supervisor) || isSupervisorDest(c));
				default:
					throw G.Error("Not expecting moving object %s", pickedObject.type);
				}
			case TAKE_GOLD_STATE:
				G.Assert(pickedObject.type == yclass.gold,
						"gold should be moving");
				return ((c == gold) || (c == playerBoards[whoseTurn].pmisc[ypmisc.gold.index]));
			case PASS_STATE:
				return (false);
			case TAKE_CARD_STATE:
				G.Assert(pickedObject.type == yclass.cards,
						"a card should be moving");
				return ((c == cards) || (c == playerBoards[whoseTurn].pmisc[ypmisc.card.index]));
			case TAKE_CAMEL_STATE:
				G.Assert(pickedObject.type == yclass.camels,
						"a camel should be moving");
				return ((c == camels) || (c == playerBoards[whoseTurn].pmisc[ypmisc.camel.index]));
			case PLACE_BAG_STATE:
			case PLACE_CHEST_STATE:
			case PLACE_VASE_STATE:
			case PLACE_BARREL_STATE:
			case CARD_PLACE_CUBE_SOUK:
				if (c.type == pickedObject.type) {
					switch (pickedObject.type) {
					case playerCubes:
						return ((c == playerBoards[whoseTurn].pmisc[ypmisc.cubes.index]) || isNeighborhoodDest(c));
					case gold:
						if ((c == playerBoards[whoseTurn].pmisc[ypmisc.gold.index])
								|| ((c.rackLocation == yrack.Dice_Tower) && (c.row == selectedDice.row))) {
							return (true);
						}
						break;
					case cards:
						if ((c == playerBoards[whoseTurn].pmisc[ypmisc.card.index])
								|| ((c.rackLocation == yrack.Dice_Tower) && (c.row == selectedDice.row))) {
							return (true);
						}
						break;
					default:
						throw G.Error("moving object %s is not expected",
								pickedObject.type);
					}
				}
				return (false);
			case ROLL_STATE:
				switch(pickedObject.type)
				{
				case dice:	return(c.rackLocation == yrack.Dice_Table);
				case cards: return(c.rackLocation == yrack.Discard_Stack);
				default: return(false); 	// shouldn't happen
				}

			case CARD_TRADE_CAMELS_GOLD:
				return ((c == playerBoards[whoseTurn].pmisc[ypmisc.camel.index]) || (c == playerBoards[whoseTurn].pmisc[ypmisc.gold.index]));
			case PREBUILD_STATE:
			case PLAY_CARD_EFFECT_STATE:
				return(c==discards);
			case PLAY_CARD_DICE_STATE:
				return ((c == discards) // play for normal card effect
						|| ((c.rackLocation == yrack.Dice_Tower)
							&& (diceTower[c.row].height()>0)
							&& ((selectedDice==null) || (c.row == selectedDice.row)))	 // play for increase
						|| (c == playerBoards[whoseTurn].pmisc[ypmisc.card.index]));
			case CARD_SCORE_GOLD:
				return (c == gold);
			case CARD_SCORE_CAMELS:
			case PAY_CAMEL_STATE:
				return (c == camels);
			case GAMEOVER_STATE: return(false);
			default:
				throw G.Error("Not expecting state %s", board_state);
			case PUZZLE_STATE:
				if(c.type==yclass.dice) 
				{ 
					if(c.height()!=0)
					{
						YspahanChip top = c.topChip();
						if(top!=pickedObject) { return(false); }	// no mixed stacks of dice
					}
				}
				return (true);
			}
		} else { // nothing is currently picked up.
			boolean canHitEmpty = canClickOnSouk();
			if ((c.height() == 0) && ((c.type!=yclass.playerCubes) || !canHitEmpty))
				{
				return (false);
				} // can never pick from an empty cell
			if (isDest(c)) {
				return (true);
			} // can always pick what we just dropped
			switch (board_state) {
			case THREEWAY_PLACE_CHEST_STATE:
			case THREEWAY_PLACE_VASE_STATE:
			case THREEWAY_PLACE_BAG_STATE:
			case THREEWAY_PLACE_BARREL_STATE:
				// can always choose a different set of dice, or a card or the
				// sup
				if ((c.type == yclass.dice) || (c == cards)
						|| (c == supervisor)) {
					return (true);
				} // you can always take a card, or another dice selection, or
					// the supervisor
				// can pay a card or gold to alter the dice, or pick a cube to
				// start adding to a souk
				switch (c.type) {
				case playerCubes:
					if(c.height()==0)
					{
						Hashtable<YspahanCell,YspahanCell> dests = getDests();
						return(dests.get(c)!=null);
					}
					//$FALL-THROUGH$
				case gold:
				case cards: // can pay a card or gold to alter the behavior
					return (c.playerIndex == whoseTurn);
				default:
					break;
				}
				return (false);
			case MOVE_SUPERVISOR_STATE:
				return (((c == supervisor)
						|| (c == playerBoards[whoseTurn].pmisc[ypmisc.gold.index]) || (c == diceTowerExtraGold[selectedDice.row])));
			case THREEWAY_TAKE_GOLD_STATE:
			case THREEWAY_TAKE_CAMEL_STATE:
				if ((c.type == yclass.dice)
						|| (c == cards)
						|| (c == supervisor)
						|| (c == playerBoards[whoseTurn].pmisc[ypmisc.card.index])
						|| (c == playerBoards[whoseTurn].pmisc[ypmisc.gold.index])) {
					return (true);
				} // cards, dice, or supervisor
				return ((c.rackLocation == selectedDice.diceTower.rackLocation));

			case TAKE_CARD_STATE:
				// this is a rare case, when all the dice have been taken the
				// user has to take a card
				return (c == cards);
			case SELECT_STATE:
				return ((c.rackLocation == yrack.Dice_Tower)
						|| (c == playerBoards[whoseTurn].pmisc[ypmisc.card.index])
						||	(c == playerBoards[whoseTurn].pmisc[ypmisc.gold.index])
						);
			case ROLL_STATE:
				return legalToHitInRoll(c);
			case GAMEOVER_STATE:
				return((c.rackLocation()==yrack.Misc_Track)&&(c.row==ypmisc.card.index));
			case PASS_STATE:
				return (false);

			case CARD_PLACE_CUBE_SOUK:
			case PLACE_BAG_STATE:
			case PLACE_CHEST_STATE:
			case PLACE_VASE_STATE:
			case PLACE_BARREL_STATE:
				if(c.height()==0)
				{
					Hashtable<YspahanCell,YspahanCell> dests = getDests();
					return(dests.get(c)!=null);
				}
				return (c == playerBoards[whoseTurn].pmisc[ypmisc.cubes.index]);
			case CARD_TRADE_CAMELS_GOLD:
				return ((c == playerBoards[whoseTurn].pmisc[ypmisc.camel.index]) || (c == playerBoards[whoseTurn].pmisc[ypmisc.gold.index]));
			case BUILD_STATE:
				return (isDest(c)
						|| (c == playerBoards[whoseTurn].pmisc[ypmisc.cubes.index]) || (c == playerBoards[whoseTurn].pmisc[ypmisc.card.index]));
			case CONFIRM_STATE:
			case PREBUILD_STATE:
			case CONFIRM_CARD_STATE:
				return (false);
			case DESIGNATE_CUBE_STATE:
			case DESIGNATED_CUBE_STATE: {
				YspahanCell dest = nextSupervisor == null ? supervisor
						: nextSupervisor;
				for (int dir = 0; dir < dest.geometry.n; dir++) {
					YspahanCell adj = dest.exitTo(dir);
					if ((adj == c) && (adj != protectedCube)
							&& (adj != protectedCube2)
							&& (adj.type == yclass.playerCubes)) {
						return (true);
					}
				}
				return (false);
			}
			case PLAY_CARD_EFFECT_STATE:	// should be impossible - this is only when a card is pickedobject
			case PAID_CAMEL_STATE:
				return (false);
			case PAY_CAMEL_STATE:
				G.Assert(selectedCube != null, "the cube must be selected");
				YspahanChip top = selectedCube.topChip();
				return (c == playerBoards[playerWithColor(top)].pmisc[ypmisc.camel.index]);
			case CARD_SCORE_GOLD:
				return (c == playerBoards[whoseTurn].pmisc[ypmisc.gold.index]);
			case CARD_SCORE_CAMELS:
				return (c == playerBoards[whoseTurn].pmisc[ypmisc.camel.index]);
			default:
				throw G.Error("Not expecting state %s", board_state);
			case TAKE_CAMEL_STATE:
				return (c == camels);
			case TAKE_GOLD_STATE:
				return (c == gold);
			case PUZZLE_STATE:
				return (c.height() > 0);
			}
		}
	}

	/**
	 * assistance for the robot. In addition to executing a move, the robot
	 * requires that you be able to undo the execution. The simplest way to do
	 * this is to record whatever other information is needed before you execute
	 * the move. It's also convenient to automatically supply the "done"
	 * confirmation for any moves that are not completely self executing.
	 */
	public void RobotExecute(YspahanMovespec m) {
		m.state = board_state; // record the starting undoInfo. The most
								// reliable
		// to undo undoInfo transistions is to simple put the original undoInfo
		// back.

		G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

		if (Execute(m,replayMode.Replay)) {
			if (m.op == MOVE_DONE) {
			} else if (DoneState()) {
				doDone(m,replayMode.Replay);
			} else {
				throw G.Error("Robot move should be in a done undoInfo");
			}
		}
	}

	//
	// un-execute a move. The move should only be unexecuted
	// in proper sequence. This only needs to handle the moves
	// that the robot might actually make.
	//
	public void UnExecute(YspahanMovespec m) {
		// System.out.println("U "+m+" for "+whoseTurn);
		switch (m.op) {
		default:
			cantUnExecute(m);
			break;
		case MOVE_DONE:
			break;

		case MOVE_BOARD_BOARD:
			switch (board_state) {
			default:
				throw G.Error("Not expecting robot in undoInfo %s", board_state);
			case GAMEOVER_STATE:
			case ROLL_STATE:
				G.Assert((pickedObject == null), "nothing should be moving");
				pickObject(getLocalCell(m.source, m.to_col, m.to_row), -1);
				dropObject(getLocalCell(m.dest, m.from_col, m.from_row),
						m.depth,replayMode.Replay);
				acceptPlacement();
				break;
			}
			break;
		case MOVE_RESIGN:
			break;
		}
		setState(m.state);
		if (whoseTurn != m.player) {
			moveNumber--;
			setWhoseTurn(m.player);
		}
	}

	CommonMoveStack  GetListOfMoves() {
		throw G.Error("Not implemented");
	}

	/**
	 * data defining the coordinates and linkages of cells on the main board.
	 * the xy were measured on the standard board photo with photoshop
	 */
	static final int board_width = 906; // width of the measured image
	static final int board_height = 456; // height of the measured image
	static final int center_track_h[][] = // horizontal points along the center
											// track
	{ { 0, 56, 237 }, { 1, 101, 221 }, { 2, 133, 217 }, { 3, 169, 208 },
			{ 4, 199, 203 }, { 5, 232, 200 }, { 6, 263, 197 }, { 7, 299, 188 },
			{ 8, 344, 188 },
			{ 9, 380, 182 },
			{ 10, 418, 187 }, // center
			{ 11, 458, 181 }, { 12, 488, 172 }, { 13, 518, 161 },
			{ 14, 545, 153 }, { 15, 576, 139 }, { 16, 604, 131 },
			{ 17, 638, 117 } };
	static final int center_track_v[][] = // vertical points along the center
											// track
	{ { 18, 408, 40 }, { 19, 410, 73 },
			{ 20, 414, 108 },
			{ 21, 417, 149 },
			{ 1022, 419, 186 }, // center, cell 22 is made identical to cell 10
			{ 23, 421, 223 }, { 24, 423, 250 }, { 25, 427, 276 },
			{ 26, 425, 303 }, { 27, 430, 337 }, { 28, 417, 367 },
			{ 29, 422, 401 } };

	// first coordinate is the supervisor track or tracks cell linked to
	static final int souk_bag_0[][] = { { 1, 109, 247, 8 },
			{ -1, 111, 308, 8 }, { -1, 112, 356, 8 }, { -1, 114, 405, 8 },
			{ -1, 56, 341, 8 }, { -1, 54, 283, 8 } };
	static final int souk_bag_1[][] = { { 3, 182, 254, 6 }, { 5, 236, 233, 6 },
			{ -1, 311, 290, 6 }, { -1, 249, 324, 6 }, { -1, 180, 338, 6 } };

	static final int souk_bag_2[][] = { { 07, 311, 222, 3 }, // adjacent to 2
																// tracks
			{ 2309, 387, 211, 3 }, // adjacent to 2 tracks
			{ 24, 387, 248, 3 } };

	static final int souk_bag_3[][] = { { 26, 387, 299, 4 },
			{ 28, 354, 369, 4 }, { -1, 258, 392, 4 }, { -1, 188, 396, 4 } };

	static final int[][][] bag_souks = { souk_bag_0, souk_bag_1, souk_bag_2,
			souk_bag_3 };

	static final int souk_barrel_0[][] = { { 2311, 462, 210, 3 }, // adjacent to
																	// 2 tracks
			{ 12, 505, 196, 3 } };

	static final int souk_barrel_1[][] = { { 14, 563, 177, 6 },
			{ 16, 626, 155, 6 }, { -1, 632, 208, 6 }, { -1, 629, 293, 6 } };

	static final int souk_barrel_2[][] = { { 25, 468, 260, 4 },
			{ -1, 516, 255, 4 }, { -1, 575, 230, 4 } };

	static final int souk_barrel_3[][] = { { 29, 475, 385, 8 },
			{ 27, 493, 319, 8 }, { -1, 569, 320, 8 }, { -1, 597, 380, 8 },
			{ -1, 530, 409, 8 } };

	static final int[][][] barrel_souks = { souk_barrel_0, souk_barrel_1,
			souk_barrel_2, souk_barrel_3 };

	static final int souk_chest_0[][] = { { 0, 50, 179, 6 }, // left most souk
			{ 2, 136, 160, 6 }, { 4, 203, 158, 6 } };

	static final int souk_chest_1[][] = { { 6, 259, 150, 4 },
			{ 8, 346, 142, 4 } };

	static final int souk_chest_2[][] = { { 20, 383, 105, 8 },
			{ 18, 379, 35, 8 }, { -1, 306, 41, 8 }, { -1, 299, 95, 8 } };

	static final int[][][] chest_souks = { souk_chest_0, souk_chest_1,
			souk_chest_2 };

	static final int souk_vase_0[][] = { { 2111, 460, 141, 4 } }; // adjacent to
																	// 2 tracks

	static final int souk_vase_1[][] = { { 19, 447, 54, 6 }, { -1, 504, 62, 6 } };

	static final int souk_vase_2[][] = { { 13, 510, 123, 12 },
			{ 15, 562, 104, 12 }, { 17, 625, 81, 12 } };

	static final int[][][] vase_souks = { souk_vase_0, souk_vase_1, souk_vase_2 };

	static final int TOWER_GOLD_X = 80;
	static final int TOWER_GOLD_Y = -10;
	static final int TOWER_CARD_X = 85;
	static final int TOWER_CARD_Y = 0;
	static final int dice_points[][] = {
			{ ydicetower.take_camels.towerIndex, 800, 410 },
			{ ydicetower.place_bag.towerIndex, 799, 338 },
			{ ydicetower.place_barrel.towerIndex, 801, 261 },
			{ ydicetower.place_chest.towerIndex, 806, 189 },
			{ ydicetower.place_vase.towerIndex, 805, 121 },
			{ ydicetower.take_gold.towerIndex, 805, 41 } };

	static final int[][] day_track = { { -1, 28, 97 }, { -1, 60, 95 },
			{ -1, 89, 95 }, { -1, 121, 96 }, { -1, 150, 95 }, { -1, 180, 95 },
			{ -1, 213, 95 } };
	static final int[][] week_track = { { -1, 46, 72 }, { -1, 75, 71 },
			{ -1, 105, 72 }, { -1, 130, 71 } };
	static final int time_tracks[][][] = { day_track, week_track };

	// positions on the player boards
	static final int pboard_width = 270;
	static final int pboard_height = 86;
	static final int subboard_track[][] = {
			{ ybuild.extra_camel.index, 27, 16 },
			{ ybuild.extra_gold.index, 69, 17 },
			{ ybuild.extra_movement.index, 114, 16 },
			{ ybuild.extra_card.index, 159, 17 },
			{ ybuild.extra_points.index, 201, 15 },
			{ ybuild.extra_cube.index, 248, 16 } };

	static final int subboard_misc[][] = { { ypmisc.camel.index, 17, 57 },
			{ ypmisc.gold.index, 68, 59 }, { ypmisc.card.index, 116, 58 },
			{ ypmisc.cubes.index, 185, 58 }, { ypmisc.points.index, 246, 62 } };

	static final int cboard_height = 55;
	static final int cboard_width = 452;
	static final int caravan_points[][] = { { 0, 16, 33 }, { 1, 59, 35 },
			{ 2, 92, 34 }, { 3, 129, 35 }, { 4, 169, 35 }, { 5, 207, 36 },
			{ 6, 243, 37 }, { 7, 278, 36 }, { 8, 320, 34 }, { 9, 354, 34 },
			{ 10, 391, 34 }, { 11, 427, 34 } };

	static final int dboard_width = 345;
	static final int dboard_height = 649;
	static final int dboard_points[][] = { { 88, 124 }, { 84, 168 },
			{ 84, 211 }, { 82, 255 }, { 82, 298 }, { 79, 340 }, { 78, 379 },
			{ 76, 421 }, { 74, 476 }, { 146, 147 }, // extra slots
			{ 144, 184 }, { 143, 227 } };
	static final int dboard_xpoints[][] = { { 227, 141 }, { 227, 188 },
			{ 225, 228 } };

}
