/**
 * 
 */
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
package yspahan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lib.Random;
import lib.G;
import lib.Http;
import yspahan.YspahanConstants.ycard;
import yspahan.YspahanPlayData.CubeGroup;

/**
 * @author Günther Rosenbaum The core features and utility methods for all
 *         strategies are implemented here, together with common properties.
 */
public abstract class YspahanPlayStratCore implements YspahanPlayStratConst
{

	YspahanBoard board = null;
	public YspahanPlayData data = null; // public for unit test
	ArrayList<YspahanMovespec> robotMoveList;
	YspahanPlayStratMoveGen moveGenerator;

	protected int player = -1;
	protected int numPlayers = 0;
	protected boolean[] robotPlayers = new boolean[max_nr_of_players];

	private int[][][] steps = null; // priority list of strategy steps
	private int[] nrOfSteps = null; // number of steps in each stage

	class SupMoveDetail
	{
		public boolean playerOK = false;
		public boolean usable = false;
		public int value = -1;
		public int region = -1;
		public boolean good = false;
		public boolean veryGood = false;
		public boolean min1 = false;
		public boolean min2 = false;
		public boolean complete = false;
		public int costs = -1;
	}

	protected SupMoveDetail[][][] actSupMoveDetail;                                   // dice,
																					// direction,step

	protected static final int[][] regionGroupValue = {
			{ 2, 4, 6, 8, 12, 16, 18, 20, 22, 24, 26, 28 },
			{ 3, 6, 8, 12, 16, 18, 20, 22, 24, 25, 28, 30 },
			{ 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48 },
			{ 8, 16, 24, 32, 40, 48, 56, 64, 72, 80, 88, 96 } };

	protected int[] rValue = { 0, 0, 0, 0, 0, 0 };

	protected int prio1Building, prio2Building, prio3Building, prio4Building,
			prio5Building, prio6Building;
	protected int[] prioBuilding;

	protected Random myRand;


	/**
	 * constructor
	 * 
	 * @param player
	 * @param numPlayers
	 * @param randomKey 
	 * @param nrofsteps
	 */
	protected YspahanPlayStratCore(int player, int numPlayers, long randomKey, int[][][] steps,
			int[] nrofsteps)
	{
		super();
		this.player = player;
		this.numPlayers = numPlayers;
		this.steps = steps;
		this.nrOfSteps = nrofsteps;
		this.robotMoveList = new ArrayList<YspahanMovespec>();
		this.moveGenerator = new YspahanPlayStratMoveGen(player, robotMoveList);
		this.actSupMoveDetail = new SupMoveDetail[7][4][11];
		for (int i = 1; i < 7; i++)
		{
			for (int j = 0; j < 4; j++)
			{
				for (int k = 0; k < 11; k++)
				{
					actSupMoveDetail[i][j][k] = new SupMoveDetail();
				}
			}
		}

		prio1Building = 0;
		prio2Building = 0;
		prio3Building = 0;
		prio4Building = 0;
		prio5Building = 0;
		prio6Building = 0;

		myRand = new Random(randomKey);
		for (int j = 0; j < 10		; j++)
		{
			myRand.nextInt(4711);
		}

	}


	protected void setPrioBuilding()
	{
		prioBuilding = new int[7];
		prioBuilding[0] = 0;
		prioBuilding[1] = prio1Building;
		prioBuilding[2] = prio2Building;
		prioBuilding[3] = prio3Building;
		prioBuilding[4] = prio4Building;
		prioBuilding[5] = prio5Building;
		prioBuilding[6] = prio6Building;
	}


	/**
	 * Copy actual state to internal data structures
	 * 
	 * @param bd
	 */
	public void copyBoardState(YspahanBoard bd, boolean[] robots)
	{
		// here we will copy the external representation of the board to the
		// internal representation.
		// this will be done before each move generation.
		// At the moment we will not implement a kind of adapter to separate the
		// adaption ...
		// We create a new data object each turn

		data = new YspahanPlayData(bd, player, numPlayers);
		robotPlayers = robots;
	}


	protected boolean performAction(int stage, int[] zValue)
	{

		boolean result = false;
		int gx = data.getGold();
		int cx = data.getCamels();
		if (findCard(ycard.card_buy_no_camels) >= 0)
		{
			cx += 4;
		}
		;
		if (findCard(ycard.card_buy_no_gold) >= 0)
		{
			gx += 4;
		}
		;
		int cd = numDice(camelRow);
		int gd = numDice(goldRow);
		int realDays = 4;

		int maxValue = -1;
		int maxInx = 0;
		int resultWay[][][] = new int[4][4][2];

		for (int i = 1; i <= 4; i++)
		{
			if (zValue[i] >= maxValue)
			{
				maxValue = zValue[i];
				maxInx = i;
			}
		}

		int maxNumD = 0;
		int maxNumDInx = 0;
		for (int i = 1; i <= 4; i++)
		{
			if (numDice(i) >= maxNumD)
			{
				maxNumD = numDice(i);
				maxNumDInx = i-1; //region index
			}
		}
		;

		int card = -1;
		int zMax = 0;
		int zInx = 0;

		int camelLimit = 100;
		int goldLimit = 100;
		
		// Start loop of actions
		for (int i = 0; i < nrOfSteps[stage]; i++)
		{
			boolean go = false;
			card = -1;
			
			camelLimit = steps[stage][i][6];
			if (camelLimit >= 100)
			{
				if (data.getRounds() == 20)
				{
					camelLimit = 8;
				}
				else if(data.getRounds() == 21)
				{
					camelLimit = 4;
				}
				else
				{
					camelLimit = 10;
				}
			}

			// camels
			if ((steps[stage][i][0] > 0) && (gx < steps[stage][i][5])
					&& (cx < camelLimit))
			{
				if (steps[stage][i][7] == 1)
				{
					go = (findCard(ycard.card_swap_camels_gold) >= 0);
				}
				else
				{
					go = true;
				}
			}

			if (go)
			{
				card = -1;
				go = false;
				if (steps[stage][i][0] > 100) 
				{
					if ((steps[stage][i][8] == cd + cx + 1) && (cd > 0))
					{
						if (steps[stage][i][4] == 1)
						{
							card = getCardAddDiceS8(true,
									(steps[stage][i][8] <= 0)
											&& (steps[stage][i][9] <= 0));
							go = (card >= 0);
						}
						else
						{
							card = -1;
							go = false;
						}
					}
					else
					{
						go = false;
					}
				}
				else
				{
					go = (cd >= steps[stage][i][0]);
				}
			}

			if (go)
			{
				a_Take_Camels_Ext(card);
				if (steps[stage][i][7] == 1)
				{
					if (gx < steps[stage][i][9])
					{
						a_useCard_Trade(0, steps[stage][i][9] - gx);
					}
				}
				result = true;
				return result; // ==> EXIT; ACTION "Get Camels" done!
			}

			// *************************************************************
			// ******************* GOLD *********************************
			// *************************************************************

			go = false;
			
			goldLimit = steps[stage][i][5];
			if (goldLimit >= 100)
			{
				if (data.getRounds() == 20)
				{
					goldLimit = 8;
				}
				else if(data.getRounds() == 21)
				{
					goldLimit = 4;
				}
				else
				{
					goldLimit = 10;
				}
			}

			if ((steps[stage][i][1] > 0) && (gx < goldLimit)
					&& (cx < steps[stage][i][6]))
			{
				if (steps[stage][i][7] == 1)
				{
					go = (findCard(ycard.card_swap_camels_gold) >= 0);
				}
				else
				{
					go = true;
				}
			}

			if (go)
			{
				go = false;
				card = -1;
				if (steps[stage][i][1] > 100)
				{
					if ((steps[stage][i][9] == gd + gx + 1) && (gd > 0))
					{
						if (steps[stage][i][4] == 1)
						{
							card = getCardAddDiceS8(true,
									(steps[stage][i][8] <= 0)
											&& (steps[stage][i][9] <= 0));
							go = (card >= 0);
						}
						else
						{
							card = -1;
							go = false;
						}
					}
					else
					{
						go = false;
					}
				}
				else
				{
					go = (gd >= steps[stage][i][1]);
				}
			}

			if (go)
			{
				a_Take_Gold_Ext(card);
				if (steps[stage][i][7] == 1)
				{
					gx = data.getGold();
					if (cx < steps[stage][i][8] && gx > steps[stage][i][9])
					{
						a_useCard_Trade(gx - steps[stage][i][9], 0);
					}
				}
				result = true;
				return result; // ==> EXIT; ACTION "Get Gold" done!
			}

			// *************************************************************
			// ******************* CUBES ********************************
			// *************************************************************

			go = false;
			if ((steps[stage][i][2] > 0) && (gx < steps[stage][i][5])
					&& (cx < steps[stage][i][6]))
			{
				card = -1;
				if ((maxValue >= steps[stage][i][2])
						|| (data.getActDaysInWeek() >= realDays))
				{
					if (steps[stage][i][4] == 1)
					{
						card = getCardAddDiceS8(true, (steps[stage][i][8] <= 0)
								&& (steps[stage][i][9] <= 0));
					}
					else
					{
						card = -1;
					}
					if (data.getActDaysInWeek() >= realDays)
					{
						computeRealDiceValue(card, resultWay, zValue);
						zMax = 0;
						zInx = -1;
						for (int zrun = 0; zrun < 4; zrun++)
						{
							if (zValue[zrun] >= zMax)
							{
								zMax = zValue[zrun];
								zInx = zrun;
							}
						}
						if (zMax == 0 && card < 0)
						{
							card = getCardAddDiceS8(true,
									(steps[stage][i][8] <= 0)
											&& (steps[stage][i][9] <= 0));
							if (card >= 0)
							{
								computeRealDiceValue(card, resultWay, zValue);
								zMax = 0;
								zInx = -1;
								for (int zrun = 0; zrun < 4; zrun++)
								{
									if (zValue[zrun] >= zMax)
									{
										zMax = zValue[zrun];
										zInx = zrun;
									}
								}
							}
						}
						if (2 * zMax >= steps[stage][i][2])
						{
							boolean res = s_use_small_groups(zInx, card, true,
									resultWay);
							if (res)
							{
								result = true;
								return result;
							} // ====> EXIT with CUBES
						}
					}
					else
					{
						boolean res = s_use_small_groups(maxInx - 1, card,
								false, resultWay);
						if (res)
						{
							result = true;
							return result;
						} // ====> EXIT with CUBES
					}
				}
			}

			if (((steps[stage][i][3] == 10) && (gx < steps[stage][i][5]) && (cx < steps[stage][i][6]))
					&& (!(caravanFactor() == 3) || (data.getWeek() == 3))
					&& (data.getActDaysInWeek() <= 5))
			{
				if (maxNumD >= 3)
				{
					boolean res = s_use_small_groups(maxNumDInx, -1, false,
							resultWay);
					if (res)
					{
						result = true;
						return result;
					} // ====> EXIT with CUBES
				}
			}

			if ((((steps[stage][i][3] == 10) || (steps[stage][i][3] == 100))
					&& (gx < steps[stage][i][5]) && (cx < steps[stage][i][6]))
					&& (!(caravanFactor() == 3) || (data.getWeek() == 3) ||
							(data.hasBuilding(buildings.extra_points) && (data.getCamels() > 1))))
			{
				computeSupervisor(actSupMoveDetail);
				boolean res = supervisorMove(2, 1, actSupMoveDetail);
				if (res)
				{
					result = true;
					return result;
				} // ====> EXIT with SUPERVISOR
			}

			if (((steps[stage][i][3] == 10) && (gx < steps[stage][i][5]) && (cx < steps[stage][i][6]))
					&& (!(caravanFactor() == 3) || (data.getWeek() == 3))
					&& (data.getActDaysInWeek() <= 6))
			{
				if (maxNumD >= 2)
				{
					boolean res = s_use_small_groups(maxNumDInx, -1, false,	resultWay);
					if (res)
					{
						result = true;
						return result;
					} // ====> EXIT with CUBES
				}
			}

			if (((steps[stage][i][3] > 0) && (steps[stage][i][3] < 100))
					&& (gx < steps[stage][i][5]) && (cx < steps[stage][i][6]))
			{ // take card
				int dice = getDiceRowAgainstOpponent();

				boolean res = a_TakeCard(dice);
				if (!res )  
				{
					// no card available !!
					res = doAnyMove();
					if (!res)
					{
						deleteDiceRow(dice);
						G.print("Pass move by robot!");
						//passing!
					}
				}
				result = true;
				return result; // ====> EXIT with CARD or NOTHING
			}

		} // end loop actions

		return false;
	}


	private boolean doAnyMove()
	{
		int resultWay[][][] = new int[4][4][2];

		computeRealDiceValue(-1, resultWay, rValue);

		boolean res = false;
		for (int i = 4; i >=1; i--)
		{
			if (numDice(i) > 0)
			{
				res = s_use_small_groups(i-1, -1, true, resultWay);
				if (res)
				{
					return true;
				} // ====> EXIT with CUBES
			}
		}
		
		if (numDice(camelRow) > 0)
		{
			a_Take_Camels_Ext(-1);
			return true;
		}
		
		if (numDice(goldRow) > 0)
		{
			a_Take_Gold_Ext(-1);
			return true;
		}
		
		// never do a forced supervisor move
		return false;
	}


	protected int caravanFactor()
	{
		return data.theCaravan.factorForPlayer();
	}


	protected boolean s_use_small_groups(int region, int card,
			boolean validWay, int[][][] resValue)
	{

		boolean cardUsed = true;
		boolean result = false;
		int cubes = numDice(region + 1);

		if (card >= 0)
		{
			cubes++;
		}
		if (data.getCubes() < cubes)
		{
			cubes = data.getCubes();
			cardUsed = false;
		}

		if (cubes == 0)
		{
			return false;
		} // =========> EXIT

		boolean res = false;

		if (validWay)
		{
			for (int i = 0; i < 4; i++)
			{
				if (resValue[region][i][0] < 0)
				{
					break;
				}
				if (cubes == 0)
				{
					break;
				}
				res = true;
				while (res)
				{
					res = moveCubeToGroup(region, resValue[region][i][0]);
					if (res)
					{
						cubes--;
						result = true;
					}
					if (cubes == 0)
					{
						res = false;
					}
				}
			}
		}

		//
		boolean running = true;
		int count = 0;

		while (running && (cubes > 0))
		{
			count++;
			if (count > 20)
			{
				error("s_use_small_groups -- count overflow");
				return false;
			}
			int group = data.groupStartedD(player, region, cubes);
			while (group >= 0 && cubes > 0)
			{
				res = moveCubeToGroup(region, group);
				cubes--;
				group = data.groupStartedD(player, region, cubes);
				result = true;
			}
			if (cubes == 0)
			{
				break;
			}

			group = getSmallestEmptyGroup(region);
			if (group < 0)
			{
				break;
			}

			res = moveCubeToGroup(region, group);
			cubes--;
			result = true;
			if (cubes == 0)
			{
				break;
			}
		}

		if (result)
		{
			deleteCard((cubes == 0 && cardUsed && card >= 0) ? card : -1,
					region + 1);
			deleteDiceRow(region + 1);
			return true;
		}

		return false;

	}


	private void deleteCard(int card, int diceRegion)
	{
		if (card >= 0)
		{
			data.deleteCard(card);
			moveGenerator.ifDeleteCardAddDie(card, diceRegion);
		}
	}


	/**
	 * Only for tracing.
	 * 
	 * @param aStein
	 * @param region
	 * @param i
	 */
//	private void playerAction(int action, int region, int nrOfDice)
//	{
//		trace("action: " + action + " in region " + region + " , dices: " + nrOfDice);
//		// only for info
//	}


	private int getSmallestEmptyGroup(int region)
	{
		for (CubeGroup cGroup : data.boardArea[region])
		{
			if (cGroup.Owner < 0 && cGroup.maxCount > 0)
			{
				return cGroup.group;
			}
		}
		return -1;
	}


	protected void computeRealDiceValue(int card, int[][][] resWay, int[] zValue)
	{
		computeRealDiceValue(player, card, resWay, zValue);
	}


	protected void computeRealDiceValue(int thePlayer, int card,
			int[][][] resWay, int[] zValue)
	{
		int[][] empty = new int[4][2];
		int[][] halfFull = new int[4][2];
		int[][] gWay;
		Arrays.fill(zValue, -1);
		for (int[][] is : resWay)
		{
			for (int[] is2 : is)
			{
				Arrays.fill(is2, -1);
			}
		}
		;

		for (int i = 0; i < 4; i++)
		{ // for all regions
			int w = numDice(thePlayer, i + 1);
			if ((w != 0) && data.isRegionUsable(thePlayer, i))
			{
				if (card >= 0)
				{
					w++;
				}
				for (int[] le : empty)
				{
					Arrays.fill(le, -1);
				}
				for (int[] ha : halfFull)
				{
					Arrays.fill(ha, -1);
				}

				gWay = new int[4][2];
				for (int[] gw : gWay)
				{
					Arrays.fill(gw, -1);
				}

				for (int j = 0; j < data.boardArea[i].length; j++)
				{ // for all groups
					if (!data.boardArea[i][j].full)
					{
						if (data.boardArea[i][j].Owner < 0)
						{
							empty[j][0] = data.boardArea[i][j].maxCount;
							empty[j][1] = getGroupValue(thePlayer, i, j);
						}
						else
						{
							if (data.boardArea[i][j].Owner == thePlayer)
							{
								halfFull[j][0] = data.boardArea[i][j].maxCount
										- data.boardArea[i][j].actCount;
								halfFull[j][1] = getGroupValue(thePlayer, i, j);
							}
						}
					}
				}
				int value = compValue(0, w, halfFull, empty, gWay, 0);
				resWay[i] = gWay;
				for (int l = 0; l < 4; l++)
				{
					resWay[i][l][1] = -1;
				}
				zValue[i] = value;
			}
		}
	}


	private void a2copy(int[][] from, int[][] to)
	{
		for (int i = 0; i < from.length; i++)
		{
			for (int j = 0; j < from[i].length; j++)
			{
				to[i][j] = from[i][j];
			}
		}
	}


	private int compValue(int level, int cubes, int[][] halfFull,
			int[][] empty, int[][] gWay, int vpoints)
	{
		// compute the optimal sequence of filling the groups
		// the half full groups must be filled at first - after this the empty
		// ones.
		// [0]: cubes, [1]: vp
		int[][] saveEmpty = new int[4][2];
		int[][] saveHalfFull = new int[4][2];
		int[][] saveGWay = new int[4][2];

		if (cubes == 0)
		{
			for (int i = 0; i < 4; i++)
			{
				gWay[i][0] = -1;
			}
			return vpoints;
		}
		if (level == 5)
		{
			error("comp value: to much levels.");
			return vpoints;
		}

		for (int[] sa : saveGWay)
		{
			Arrays.fill(sa, -1);
		}
		boolean hv_found = false;
		int maxS = vpoints;

		for (int a = 0; a < 4; a++)
		{// search in partly filled groups
			if (halfFull[a][0] > 0)
			{
				hv_found = true;
				if (cubes >= halfFull[a][0])
				{
					a2copy(halfFull, saveHalfFull);
					saveHalfFull[a][0] = -1;
					int sp = vpoints;
					sp += halfFull[a][1];
					int value = compValue(level + 1, cubes - halfFull[a][0],
							saveHalfFull, empty, gWay, sp);
					if (value > maxS)
					{
						maxS = value;
						a2copy(gWay, saveGWay);
						saveGWay[level][0] = a;
					}
				}
			}
		}
		if (hv_found)
		{
			a2copy(saveGWay, gWay);
			return maxS;
		}

		for (int a = 0; a < 4; a++)
		{// search in empty groups
			if (empty[a][0] > 0)
			{
				if (cubes >= empty[a][0])
				{
					a2copy(empty, saveEmpty);
					saveEmpty[a][0] = -1;
					int sp = vpoints;
					sp += empty[a][1];
					int value = compValue(level + 1, cubes - empty[a][0],
							halfFull, saveEmpty, gWay, sp);
					if (value > maxS)
					{
						maxS = value;
						a2copy(gWay, saveGWay);
						saveGWay[level][0] = a;
					}
				}
			}
		}

		a2copy(saveGWay, gWay);
		return maxS;
	}


	protected int getCardAddDiceS8(boolean prio, boolean allBuild)
	{

		if (!data.hasSomeCard())
		{
			return -1;
		}

		ycard[] cardPrio = new ycard[9];

		if (!prio)
		{
			if (allBuild)
			{
				cardPrio[0] = ycard.card_buy_no_camels;
				cardPrio[1] = ycard.card_buy_no_gold;
				cardPrio[2] = ycard.card_swap_camels_gold;
				cardPrio[3] = ycard.card_3_gold;
				cardPrio[4] = ycard.back;
				cardPrio[5] = ycard.card_score_gold;
				cardPrio[6] = ycard.card_score_camels;
				cardPrio[7] = ycard.back;
				cardPrio[8] = ycard.back;
			}
			else
			{
				cardPrio[0] = ycard.card_score_gold;
				cardPrio[1] = ycard.card_score_camels;
				cardPrio[2] = ycard.card_3_gold;
				cardPrio[3] = ycard.card_swap_camels_gold;
				cardPrio[4] = ycard.back;
				cardPrio[5] = ycard.card_place_caravan;
				cardPrio[6] = ycard.card_place_board;
				cardPrio[7] = ycard.card_buy_no_gold;
				cardPrio[8] = ycard.back;
			}
		}

		else
		{ // with priority: always return a card!
			if (allBuild)
			{
				cardPrio[0] = ycard.card_buy_no_camels;
				cardPrio[1] = ycard.card_buy_no_gold;
				cardPrio[2] = ycard.card_swap_camels_gold;
				cardPrio[3] = ycard.card_3_gold;
				cardPrio[4] = ycard.card_3_camels;
				cardPrio[5] = ycard.card_score_gold;
				cardPrio[6] = ycard.card_score_camels;
				cardPrio[7] = ycard.card_place_caravan;
				cardPrio[8] = ycard.card_place_board;
			}
			else
			{
				cardPrio[0] = ycard.card_score_gold;
				cardPrio[1] = ycard.card_score_camels;
				cardPrio[2] = ycard.card_3_gold;
				cardPrio[3] = ycard.card_swap_camels_gold;
				cardPrio[4] = ycard.card_3_camels;
				cardPrio[5] = ycard.card_place_caravan;
				cardPrio[6] = ycard.card_place_board;
				cardPrio[7] = ycard.card_buy_no_gold;
				cardPrio[8] = ycard.card_buy_no_camels;
			}
		}

		for (int i = 0; i < cardPrio.length; i++)
		{
			int cardNum = data.hasCard(cardPrio[i]);
			if (cardNum >= 0)
			{
				return cardNum;
			}
		}

		return -1;
	}


	protected int numDice(int diceRow)
	{
		// i = 0..5
		return numDice(player, diceRow);
	}


	protected int numDice(int thePlayer, int diceRow)
	{
		// i = 0..5
		return data.getDiceCount(thePlayer, diceRow);
	}


	/**
	 * 
	 * @param cardBuyNoCamels
	 * @return -1, if the card is not in players hand; else index of card.
	 */
	protected int findCard(ycard card)
	{
		// -1 or index
		return data.hasCard(card);
	}


	protected int realMaxValue()
	{
		int[] zValue = new int[6];
		int[][][] resWay = new int[4][4][2];

		int card = getCardAddDiceS8(true, false);
		computeRealDiceValue(card, resWay, zValue);

		int max = 0;
		for (int i = 0; i < zValue.length; i++)
		{
			max = Math.max(max, zValue[i]);
		}
		return max;
	}


	protected int myRandom(int i)
	{
		if (myRand == null)
		{
			return 0;
		}
		else
		{
			return myRand.nextInt(i);
		}
	}


	protected void throwDice(int yellowDices)
	{

		int yDice = Math.min(yellowDices, data.getGold());

		moveGenerator.ifThrowDice(yDice);
		// nothing else to do; the random result will be generated by
		// environment.
		// This move always ends with DONE!
	}


	/**
	 * Activities against double supervisor.
	 * 
	 * @return true: activities neccessary.
	 */
	protected boolean checkSpecialSupervisorDefense()
	{

		// Activities against "double" supervisor: use white barrel region in
		// week 2/3

		if ((data.getWeek() > 1)
				&& (data.getActDaysInWeek() <= data.numberOfPlayers)
				&& (specialSupervisorDefenseNeeded())
				&& (data.boardArea[1][0].Owner < 0))
		{
			return true;
		}

		if ((data.getWeek() > 1)
				&& (data.getActDaysInWeek() <= data.numberOfPlayers)
				&& (specialSupervisorDefenseNeeded())
				&& (isHuman(data.boardArea[1][0].Owner))
				&& (data.boardArea[3][0].Owner != data.boardArea[1][0].Owner)
				&& (data.boardArea[0][0].Owner == player))
		{
			return true;
		}

		return false;
	}


	private boolean specialSupervisorDefenseNeeded()
	{
		// Result := Not (Spieldata.Steuerinfo.Varianten.V1 OR
		// Spieldata.Steuerinfo.Varianten.V2
		// OR SpielData.Steuerinfo.AutoSpieler[1] );

		// Might be false only for special variants of the game board, or in an
		// computer only game.
		return true;

	}


	protected int getYellowDice(int count)
	{
		int res = Math.min(count, 3);
		res = Math.min(res, data.getGold());
		data.modGold(-res);
		return res;
	}


	protected boolean a_BuildPrioBuilding(boolean exact)
	{

		// Px = 0 ignorieren; Px=1..6 Prio-Gebäude; Px=11..16 non Prio Gebäude
		boolean trade = false;
		boolean res = false;

		for (int i = 1; i < prioBuilding.length; i++)
		{
			if (prioBuilding[i] > 0 && prioBuilding[i] % 10 <= 6)
			{
				if ((prioBuilding[i] % 10) > 2)
				{
					trade = true;
				}
				else
				{
					trade = (data.getGold() >= 4);
				}
				res = a_Build_Building(prioBuilding[i] % 10, true, trade);// use
																			// cards
				if (res || (exact && !data.hasBuilding(prioBuilding[i] % 10)))
				{
					return res;
				}
			}
		}
		return false;
	}


	/**
	 * 
	 * @param diceRow
	 * @return Number of cubes of this dice row.
	 */
	protected int realDiceCount(int diceRow)
	{
		// diceRow 0..5
		int countDice = numDice(diceRow);
		int countCube = 0;
		int vp = 0;

		for (CubeGroup cGroup : data.boardArea[diceRow - 1])
		{
			if (((cGroup.Owner == player) && (!cGroup.full))
					|| (cGroup.Owner < 0))
			{
				countCube += (cGroup.maxCount - cGroup.actCount);
				vp += cGroup.vPoints;
			}
		}
		int res = Math.min(countCube, countDice);

		if ((countCube < countDice) && (vp >= 8))
		{
			return countDice;
		}

		return res;
	}


	protected boolean moveCubeToGroup(int region, int group)
	{
		CubeGroup cGroup = data.boardArea[region][group];
		if (cGroup.Owner < 0)
		{
			if (data.groupStarted(region, player) > -1)
			{
				error("Not allowed move");
				return false;
			}
		}
		else
		{
			if (cGroup.Owner != player)
			{
				error("Not allowed move");
			}
		}

		for (int i = 0; i < cGroup.maxCount; i++)
		{
			if (!cGroup.used[i])
			{
				setCube(region, group, i);
				data.playerInfo[player].cubes--;
				return true;
			}

		}
		return false;
	}


	private void setCube(int region, int group, int field)
	{
		CubeGroup cGroup = data.boardArea[region][group];
		cGroup.Owner = player;
		cGroup.used[field] = true;
		cGroup.actCount++;
		cGroup.full = (cGroup.actCount >= cGroup.maxCount);
		moveGenerator.ifSetCube(region, group, field);
	}


	public boolean isHuman(int owner)
	{
		if(myDebug)  {return true;}

		if (owner < 0 || owner >= max_nr_of_players)
		{
			return false;
		}

		return !robotPlayers[owner];
	}


	public boolean isRobot(int owner)
	{
		if(myDebug)  {return false;}

		if (owner < 0 || owner >= max_nr_of_players)
		{
			return false;
		}

		return robotPlayers[owner];
	}


	public int getGroupValue(int region, int group)
	{
		return getGroupValue(player, region, group);
	}


	public int getGroupValue(int thePlayer, int region, int group)
	{
		int value = data.boardArea[region][group].vPoints;
		return data.hasBuilding(thePlayer, buildings.extra_points) ? value + 2
				: value;
	}


	protected boolean supervisorMove(int cost2, int cost1,
			SupMoveDetail[][][] actSupBewDetail)
	{
		trace("SupervisorMove");
		int street1 = -1;
		int street2 = -1;
		List<Integer> streetSeq = Arrays.asList(1,2,0,3);
		
		//look for positions with two cubes
		if (data.boardArea[0][0].Owner == player
				&&
				data.boardArea[0][0].used[1]
						&&
						data.boardArea[1][0].Owner == player
						&&
						data.boardArea[1][0].used[0]
								&&
								supervisorAlone(3,1))
		{
			street1=3; street2=0;
		}
		else
		{	
			if (data.boardArea[3][0].Owner == player
					&&
					data.boardArea[3][0].used[0]
							&&
							data.boardArea[1][0].Owner == player
							&&
							data.boardArea[1][0].used[0]
									&&
									supervisorAlone(0,1))
			{
				street1=0; street2=3;
			}
			else
			{
//				if (myRandom(2)==0)
//				{street1 = 3; street2 = 0; }
//				else
//				{ street1 = 0; street2 = 3; }
// bug:  was in comments in source for testing!! code removed here, too.
			};
		};

		boolean res = false;
		if(street1 > -1) // double positions
		{
			for (int costInx = 0; costInx <= cost2; costInx++)
			{
				res = supervisorDouble(street1, costInx, actSupBewDetail);
				if(res) return true;
				res = supervisorDouble(street2, costInx, actSupBewDetail);
				if(res) return true;
			}
		}


		//single positions
		int rowOpp = getDiceRowAgainstOpponent();
		if(rowOpp < 0) rowOpp = 4;
		
		for (int costInx = 0; costInx <= cost1; costInx++)
		{
			int dice = -1;
			
			for (int row = 5; row >=-1; row--)
			{
				dice = ( row==-1 ? 5 : (row==5 ? rowOpp : row));
				if(data.getDiceCount(dice)>0)
				{
					int valueDice = data.getDiceValue(dice);
					for (int st = 0; st < 4	; st++)
					{
						for (int pos = 2; pos < data.supervisor.getConnect()[streetSeq.get(st)].length; pos++)
						{
							if (actSupBewDetail[valueDice][streetSeq.get(st)][pos].veryGood
									&& actSupBewDetail[valueDice][streetSeq.get(st)][pos].costs==costInx
										&& supervisorAlone(streetSeq.get(st), pos) )
							{
								boolean res1 = s_Move_Supervisor(dice, streetSeq.get(st), pos);
								if (res1) return true;							
							}
						}
						//central positions later
						if (actSupBewDetail[valueDice][streetSeq.get(st)][1].veryGood
								&& actSupBewDetail[valueDice][streetSeq.get(st)][1].costs==costInx
									&& supervisorAlone(streetSeq.get(st), 1) )
						{
							boolean res1 = s_Move_Supervisor(dice, streetSeq.get(st), 1);
							if (res1) return true;							
						}

					}
				}
			}
		}

		return false;
	}


	protected boolean supervisorMoveAttack(int playerAttack, int minValue, 	SupMoveDetail[][][] actSupBewDetail)
	{

		//single positions
		int rowOpp = getDiceRowAgainstOpponent();
		if(rowOpp < 0) rowOpp = 4;
		
		for (int value = 14; value >= minValue; value--)
		{
			int dice = -1;
			
			for (int row = 5; row >=-1; row--)
			{
				dice = ( row==-1 ? 5 : (row==5 ? rowOpp : row));
				if(data.getDiceCount(dice)>0)
				{
					int valueDice = data.getDiceValue(dice);
					for (int st = 0; st < 4	; st++)
					{
						for (int pos = 1; pos < data.supervisor.getConnect()[st].length; pos++)
						{
							if (actSupBewDetail[valueDice][st][pos].veryGood
									&& actSupBewDetail[valueDice][st][pos].value == value
									&& isHuman(playerAttack)									
									&&
										((data.getDiceCount(playerAttack, actSupBewDetail[valueDice][st][pos].region+1) <= 0)
										||
										(actSupBewDetail[valueDice][st][pos].region+1 == dice)
										||
										!playerIsActiveOpponentThisRound(playerAttack)
										)
								)
							{
								boolean res1 = s_Move_Supervisor(dice, st, pos);
								if (res1) return true;							
							}
						}
					}
				}
			}
		}

		return false;
	}

	
	
	private boolean supervisorAlone(int street, int pos)
	{
		// if opponents are effected, then -> false
		if (data.supervisor.getConnect()[street][pos]== null) return false;
				
		if  (data.supervisor.getConnect()[street][pos].length <= 1)
		{ // single positions
			int xr = data.supervisor.getConnect()[street][pos][0].region;
			int xg = data.supervisor.getConnect()[street][pos][0].group;
			int xf = data.supervisor.getConnect()[street][pos][0].field;
			
			if (criticalCubeSupervisor(xr,xg,xf))
			{
				return (data.getCamels() > 0 ? true : false);
			}
			else
			{
				return true;
			}
		}
		
		
		//from here on  only double positions

		
		int xr = data.supervisor.getConnect()[street][pos][0].region;
		int xg = data.supervisor.getConnect()[street][pos][0].group;
		int xf = data.supervisor.getConnect()[street][pos][0].field;
		int	yr = data.supervisor.getConnect()[street][pos][1].region;
		int	yg = data.supervisor.getConnect()[street][pos][1].group;
		int	yf = data.supervisor.getConnect()[street][pos][1].field;
		
		
		//one link not used
		if (!data.boardArea[xr][xg].used[xf])
		{
			return (data.getCamels() > 0 ? true : false);			
		}
		
		if (!data.boardArea[yr][yg].used[yf])
		{
			return (data.getCamels() > 0 ? true : false);			
		}
		
		// both links used
		
		if(data.boardArea[xr][xg].Owner == player && data.boardArea[yr][yg].Owner == player)
		{
			return (data.getActDaysInWeek() > 3 ? true : (data.getCamels() > 0 ? true : false));			
		}
		
		if(data.getWeek() == 1) return true;
		
		if(data.getActDaysInWeek() >= 5) return true;
		
		return false;
	}

	@SuppressWarnings("unused")
	private boolean isDoubleField(int region, int group, int field)
	{
		return (region==0 && group==0 && field==1)||(region==1 && group==0 && field==0)||(region==3 && group==0 && field==0);
	}
	
	
	private boolean criticalCubeSupervisor(int region, int group, int field)
	{
		// if(isDoubleField(region, group, field)) return false;
		
		if(!data.boardArea[region][group].used[field]) return false;
		
		if(data.getWeek() == 1) return false;
		
		if(data.getActDaysInWeek() >= 5) return false;

		return true;
	}


	private boolean supervisorDouble(int street, int costs,SupMoveDetail[][][] actSupBewDetail)
	{ //move supervisor to a double field
		int j=0;
		
		for (int row = 4; row >= -1; row--)
		{
			j = (row==-1 ? 5 : row);
			if(data.getDiceCount(j)>0)
			{
				int diceValue = data.getDiceValue(j);
				if(actSupBewDetail[diceValue][street][1].veryGood && actSupBewDetail[diceValue][street][1].costs==costs)
						{
						 	boolean res = s_Move_Supervisor(j,street,1); 
						 	if(res) return true;
						}
			}
		}
		
		return false;
	}

	
	private boolean s_Move_Supervisor(int diceRow, int street, int pos)
	{
		int supP = data.supervisor.getPos();
		int supS = data.supervisor.getStreet();
		int diceValue = data.getDiceValue(diceRow);
		
		if (data.getDiceCount(diceRow) <= 0) return false;
		
		int costs = supCosts(supS, supP, street, pos, diceValue, data.hasBuilding(buildings.extra_movement));
		
		if(data.getGold() < costs) return false;
		
		data.modGold(-costs);
		
		moveGenerator.ifMoveSupervisor(supS,supP,street,pos);
		data.supervisor.setPos(pos);
		data.supervisor.setStreet(street);
		if(costs > 0) {	moveGenerator.ifDeleteGoldAddSup(costs, diceRow);}
		deleteDiceRow(diceRow);
		return true;
	}


	protected boolean specialSupervisorDefense()
	{
		// actions against double supervisor: use white barrel region in week
		// 2+3, first day
		int[][][] ergWert = null;

		if (data.getWeek() > 1
				&& data.getActDaysInWeek() <= data.numberOfPlayers
				&& specialSupervisorDefenseNeeded()
				&& data.boardArea[1][0].Owner < 0 && numDice(2) > 0)
		{
			boolean res = s_use_small_groups_barrel();
			if (res)
			{
				trace("SpezialSupervisorAbwehr");
				return true;
			}
		}

		if (data.getWeek() > 1
				&& data.getActDaysInWeek() <= data.numberOfPlayers
				&& specialSupervisorDefenseNeeded()
				&& isHuman(data.boardArea[1][0].Owner)
				&& data.boardArea[1][0].Owner != data.boardArea[0][0].Owner
				&& data.boardArea[3][0].Owner < 0 && numDice(4) > 0)
		{
			boolean res = s_use_small_groups(3, -1, false, ergWert);
			if (res)
			{
				trace("SpezialSupervisorAbwehr");
				return true;
			}
		}

		// modified
		if (data.getWeek() > 2
				&& data.getActDaysInWeek() <= data.numberOfPlayers
				&& specialSupervisorDefenseNeeded()
				&& isHuman(data.boardArea[1][0].Owner)
				&& data.hasBuilding(data.boardArea[1][0].Owner, buildings.extra_card)
				&& data.hasBuilding(data.boardArea[1][0].Owner, buildings.extra_movement)
				&& data.boardArea[1][0].Owner != data.boardArea[3][0].Owner
				&& data.boardArea[0][0].Owner < 0 && numDice(1) > 0)
		{
			boolean res = s_use_small_groups(0, -1, false, ergWert);
			if (res)
			{
				trace("SpezialSupervisorAbwehr");
				return true;
			}
		}

		return false;
	}


	private boolean s_use_small_groups_barrel()
	{
		int cubes = numDice(2);
		cubes = Math.min(cubes, data.getCubes());
		if (cubes == 0)
		{
			return false;
		}

		boolean res = barrelLoop(cubes);
		if (res)
		{
			deleteDiceRow(2);
			return true;
		}
		return false;
	}


	private boolean barrelLoop(int cubes)
	{
		if (cubes <= 0)
		{
			return false;
		}

		boolean running = true;
		boolean result = false;
		int count = 0;

		while (running)
		{
			count++;
			if (count > 20)
			{
				error("endless loop?");
			}

			int group = groupStartedD(1, cubes);

			while (group >= 0 && cubes > 0)
			{
				moveCubeToGroup(1, group);
				cubes--;
				group = groupStartedD(1, cubes);
				result = true;
			}
			if (cubes <= 0)
			{
				return result;
			}

			group = getSmallestEmptyGroup(1);

			if (group == -1)
			{
				return result;
			}

			if (group == 0)
			{ // ok}
			}
			else
				if (cubes == 1 && isGroupUsable(1, 2))
				{
					group = 2;
				}
				else
				{
					if (cubes == 2 && isGroupUsable(1, 3))
					{
						group = 3;
					}
				}

			moveCubeToGroup(1, group);
			cubes--;
			result = true;
			if (cubes == 0)
			{
				return result;
			}
		}
		return false;
	}


	private boolean isGroupUsable(int region, int group)
	{
		if (data.boardArea[region][group] == null)
		{
			return false;
		}

		return (data.boardArea[region][group].maxCount > 0
				&& !data.boardArea[region][group].full && data.boardArea[region][group].Owner < 0)
				|| data.boardArea[region][group].Owner == player;
	}


	private int groupStartedD(int region, int cubes)
	{
		for (int i = data.boardArea[region].length - 1; i >= 0; i--)
		{
			if (data.boardArea[region][i].Owner == player
					&& !data.boardArea[region][i].full)
			{
				if (data.boardArea[region][i].maxCount
						- data.boardArea[region][i].actCount <= cubes)
				{
					return i;
				}
			}
		}
		return groupStarted(region);
	}


	protected boolean supervisorAttackMax()
	{
		SupMoveDetail [][][]  attackSupMoveDetail = new SupMoveDetail[7][4][11];
		for (int i = 1; i < 7; i++)
		{
			for (int j = 0; j < 4; j++)
			{
				for (int k = 0; k < 11; k++)
				{
					attackSupMoveDetail[i][j][k] = new SupMoveDetail();
				}
			}
		}
		
		for (int thePlayer = 0; thePlayer < data.numberOfPlayers; thePlayer++)
		{
				if (thePlayer != player)
				{
					computeSupervisorAttack(thePlayer, attackSupMoveDetail);
					if (data.getCamels(thePlayer) <= 0)
					{
						boolean res = supervisorMoveAttack(thePlayer, 8,
								attackSupMoveDetail);
						if (res)
						{
							trace("supervisorAttack done");
							return true;
						}
					}
				}
				
				//special attack on caravan will not be done, because we use it ourself.
				//(filling the caravan just before scoring)
		}
		
		return false;
	}


	protected void computeSupervisor(SupMoveDetail[][][] actSupMoveDetail)
	{
		trace("computeSupervisor");
		// dice - street - position
		
		for (int dice = 1; dice <= 6; dice++)
		{
			for (int street = 0; street < 4	; street++)
			{
				for (int pos = 1; pos < data.supervisor.getStreetLength(street); pos++)
				{
					int xr = data.supervisor.getConnect()[street][pos][0].region;
					int xg = data.supervisor.getConnect()[street][pos][0].group;
					int xf = data.supervisor.getConnect()[street][pos][0].field;
					int pl = data.boardArea[xr][xg].Owner;
					actSupMoveDetail[dice][street][pos].playerOK = (player == pl);
					actSupMoveDetail[dice][street][pos].usable = data.boardArea[xr][xg].used[xf];
					actSupMoveDetail[dice][street][pos].value = data.boardArea[xr][xg].vPoints;
					actSupMoveDetail[dice][street][pos].region = xr;
					actSupMoveDetail[dice][street][pos].min1 = (data.boardArea[xr][xg].actCount >= 1);
					actSupMoveDetail[dice][street][pos].min2 = (data.boardArea[xr][xg].actCount >= 2);
					actSupMoveDetail[dice][street][pos].complete = data.boardArea[xr][xg].full;
					actSupMoveDetail[dice][street][pos].costs = supCosts(data.supervisor.getStreet(),data.supervisor.getPos(),
							                                    street,pos,dice,data.hasBuilding(buildings.extra_movement));
					actSupMoveDetail[dice][street][pos].good = actSupMoveDetail[dice][street][pos].playerOK
																&&
																actSupMoveDetail[dice][street][pos].usable
																&&
																!actSupMoveDetail[dice][street][pos].complete
																&&
																(actSupMoveDetail[dice][street][pos].costs <= 2)
																&&
																(actSupMoveDetail[dice][street][pos].costs <= data.getGold());
					actSupMoveDetail[dice][street][pos].veryGood =
															actSupMoveDetail[dice][street][pos].playerOK
															&&
															actSupMoveDetail[dice][street][pos].usable
															&&
															
															(((actSupMoveDetail[dice][street][pos].value <= 4)
															&&
															actSupMoveDetail[dice][street][pos].complete
															&&
															(data.getActDaysInWeek()<6 || data.getCamels()>0 ))
															||
															!actSupMoveDetail[dice][street][pos].complete)
															
															&&
															(actSupMoveDetail[dice][street][pos].costs <= 2)
															&&
															(actSupMoveDetail[dice][street][pos].costs <= data.getGold());
					actSupMoveDetail[dice][street][pos].veryGood =	actSupMoveDetail[dice][street][pos].veryGood
															||
															(actSupMoveDetail[dice][street][pos].playerOK
															&&
															actSupMoveDetail[dice][street][pos].usable
															&&
															actSupMoveDetail[dice][street][pos].complete
															&&
															(data.getCamels()>1)
															&&
															(actSupMoveDetail[dice][street][pos].costs <= 2)
															&&
															(actSupMoveDetail[dice][street][pos].costs <= data.getGold()));														
					
					if (data.supervisor.getConnect()[street][pos].length > 1)
					{
						xr = data.supervisor.getConnect()[street][pos][1].region;
						xg = data.supervisor.getConnect()[street][pos][1].group;
						xf = data.supervisor.getConnect()[street][pos][1].field;
						pl = data.boardArea[xr][xg].Owner;
						if (pl == player &&  data.boardArea[xr][xg].used[xf])
						{
							actSupMoveDetail[dice][street][pos].playerOK = true;
							actSupMoveDetail[dice][street][pos].usable = true;
							actSupMoveDetail[dice][street][pos].value = data.boardArea[xr][xg].vPoints;
							actSupMoveDetail[dice][street][pos].region = xr;
							actSupMoveDetail[dice][street][pos].min1 = (data.boardArea[xr][xg].actCount >= 1);
							actSupMoveDetail[dice][street][pos].min2 = (data.boardArea[xr][xg].actCount >= 2);
							actSupMoveDetail[dice][street][pos].complete = data.boardArea[xr][xg].full;
							if (!actSupMoveDetail[dice][street][pos].good)
							{
								actSupMoveDetail[dice][street][pos].good = actSupMoveDetail[dice][street][pos].playerOK
															&&
															actSupMoveDetail[dice][street][pos].usable
															&&
															!actSupMoveDetail[dice][street][pos].complete
															&&
															(actSupMoveDetail[dice][street][pos].costs <= 2)
															&&
															(actSupMoveDetail[dice][street][pos].costs <= data.getGold());
							}
							if (!actSupMoveDetail[dice][street][pos].veryGood)
							{
								actSupMoveDetail[dice][street][pos].veryGood=
										(actSupMoveDetail[dice][street][pos].playerOK
										&&
										actSupMoveDetail[dice][street][pos].usable
										&&
											((actSupMoveDetail[dice][street][pos].value <= 4)
											||
											!actSupMoveDetail[dice][street][pos].complete))
										&&
										(actSupMoveDetail[dice][street][pos].costs <= 2)
										&&
										(actSupMoveDetail[dice][street][pos].costs <= data.getGold());

							}
						}
					}
					
				}
			}
		}

	}

	
	protected void computeSupervisorAttack(int playerAttacked, SupMoveDetail[][][] actSupMoveDetail)
	{
		trace("computeSupervisorAttack");
		// dice - street - position
		
		for (int dice = 1; dice <= 6; dice++)
		{
			for (int street = 0; street < 4	; street++)
			{
				for (int pos = 1; pos < data.supervisor.getStreetLength(street); pos++)
				{
					int xr = data.supervisor.getConnect()[street][pos][0].region;
					int xg = data.supervisor.getConnect()[street][pos][0].group;
					int xf = data.supervisor.getConnect()[street][pos][0].field;
					int pl = data.boardArea[xr][xg].Owner;
					
					actSupMoveDetail[dice][street][pos].playerOK = (playerAttacked == pl);
					actSupMoveDetail[dice][street][pos].usable = data.boardArea[xr][xg].used[xf];
					actSupMoveDetail[dice][street][pos].value = data.boardArea[xr][xg].vPoints;
					actSupMoveDetail[dice][street][pos].region = xr;
					actSupMoveDetail[dice][street][pos].min1 = (data.boardArea[xr][xg].actCount >= 1);
					actSupMoveDetail[dice][street][pos].min2 = (data.boardArea[xr][xg].actCount >= 2);
					actSupMoveDetail[dice][street][pos].complete = data.boardArea[xr][xg].full;
					actSupMoveDetail[dice][street][pos].costs = supCosts(data.supervisor.getStreet(),data.supervisor.getPos(),
							                                    street,pos,dice,data.hasBuilding(buildings.extra_movement));
					actSupMoveDetail[dice][street][pos].good = false;
					actSupMoveDetail[dice][street][pos].veryGood =
															actSupMoveDetail[dice][street][pos].playerOK
															&&
															actSupMoveDetail[dice][street][pos].usable
															&&
															actSupMoveDetail[dice][street][pos].complete															
															&&
															(actSupMoveDetail[dice][street][pos].costs <= 3)
															&&
															(actSupMoveDetail[dice][street][pos].costs <= data.getGold());								
				}
			}
		}

	}


	private int supCosts(int street, int pos, int newStreet, int newPos, int dice,
			boolean hasBuilding)
	{
		int diff = 0;
		if (street == newStreet)
		{diff = Math.abs(pos-newPos);}
		else
		{diff = pos + newPos;}
		
		diff = Math.abs(dice - diff);
		if (hasBuilding)
		{diff = Math.max(diff-3, 0);}
	
		return diff;
	}


	protected boolean a_UseCard_MAX_VPointsCamelsGold()
	{
		a_UseCard_Gold();
		a_UseCard_Gold();
		a_UseCard_Camel();
		a_UseCard_Camel();

		boolean result = false;
		boolean res = a_UseCard_VPointsCamels(1000, true);
		if (res)
		{
			a_UseCard_VPointsCamels(1000, false);
			result = true;
		}
		res = a_UseCard_VPointsGold(1000, true);
		if (res)
		{
			a_UseCard_VPointsGold(1000, false);
			result = true;
		}
		if (result) trace("a_UseCard_MAX_VPointsCamelsGold");
		return result;
	}


	private boolean a_UseCard_VPointsGold(int countGold, boolean trade)
	{
		int card = data.hasCard(ycard.card_score_gold);
		if (card < 0)
		{
			return false;
		}

		int countCard = data.countCard(ycard.card_score_gold);

		int maxCount = Math.min(countGold, 10 * countCard);
		maxCount = Math.min(maxCount, data.getGold());
		@SuppressWarnings("unused")
		boolean res = false;

		if (maxCount < countCard * 10 && trade && data.getCamels() > 0)
		{
			// trade camels to gold if possible
			res = a_useCard_Trade(0, countCard * 10 - maxCount);
		}

		maxCount = Math.min(countGold, 10); // only one card
		maxCount = Math.min(maxCount, data.getGold());
		if (maxCount <= 0)
		{
			return false;
		}

		// do the action
		trace("a_UseCard_VPointsGold " + maxCount);
		data.modGold(-maxCount); // vp modification not necessary
		data.deleteCard(card);
		moveGenerator.ifDiscardCard(card);
		for (int i = 0; i < maxCount; i++)
		{
			moveGenerator.ifTradeResource(1, -1); // from gold to pool
		}
		moveGenerator.ifDone();

		return true;
	}


	private boolean a_UseCard_VPointsCamels(int countCamel, boolean trade)
	{
		int card = data.hasCard(ycard.card_score_camels);
		if (card < 0)
		{
			return false;
		}

		int countCard = data.countCard(ycard.card_score_camels);

		int maxCount = Math.min(countCamel, 4 * countCard);
		maxCount = Math.min(maxCount, data.getCamels());
		@SuppressWarnings("unused")
		boolean res = false;

		if (maxCount < countCard * 4 && trade && data.getGold() > 0)
		{
			// trade gold to camels if possible
			res = a_useCard_Trade(countCard * 4 - maxCount, 0);
		}

		maxCount = Math.min(countCamel, 4); // only one card
		maxCount = Math.min(maxCount, data.getCamels());
		if (maxCount <= 0)
		{
			return false;
		}

		// do the action
		trace("a_UseCard_VPointsCamels " + maxCount);
		data.modCamels(-maxCount); // vp modification not necessary
		data.deleteCard(card);
		moveGenerator.ifDiscardCard(card);
		for (int i = 0; i < maxCount; i++)
		{
			moveGenerator.ifTradeResource(0, -1); // from camel to pool
		}
		moveGenerator.ifDone();

		return true;
	}


	private boolean a_Build_Building(int building, boolean useBuiltCard,
			boolean trade)
	{

		int cardBuildNoGold;
		int cardBuildNoCamel;
		int cardTrade;

		if (data.hasBuilding(building) || data.getCubes() < 0)
		{
			return false;
		} // nothing to do

		cardBuildNoGold = (!useBuiltCard ? -1
				: (building == buildings.extra_camel.index ? -1 : data
						.hasCard(ycard.card_buy_no_gold)));
		cardBuildNoCamel = (!useBuiltCard ? -1 : data
				.hasCard(ycard.card_buy_no_camels));
		cardTrade = (!trade ? -1 : data.hasCard(ycard.card_swap_camels_gold));

		int costGold = buildings.goldCost(building);
		int costCamels = buildings.camelCost(building);

		if (cardBuildNoGold >= 0)
		{
			costGold = 0;
		}
		if (cardBuildNoCamel >= 0)
		{
			costCamels = 0;
		}

		// check if building can be constructed
		if (costGold <= data.getGold() && costCamels <= data.getCamels())
		{ // OK
		}
		else
		{
			if ((costGold + costCamels <= data.getGold() + data.getCamels())
					&& (cardTrade >= 0))
			{ // trading
				if (costGold > data.getGold())
				{
					a_useCard_Trade(0, costGold - data.getGold());
				}
				else
				{
					a_useCard_Trade(costCamels - data.getCamels(), 0);
				}
			}
			else
			{ // not enough resources
				return false;
			}
		}

		trace("a_Build_Building ; trade = " + trade + " ; NoGold= "
				+ cardBuildNoGold + "; NoCamels= " + cardBuildNoCamel);

		if (cardBuildNoGold >= 0)
		{
			data.deleteCard(cardBuildNoGold);
			moveGenerator.ifDiscardCard(cardBuildNoGold);
			moveGenerator.ifDone();
		}
		if (cardBuildNoCamel >= 0)
		{
			data.deleteCard(cardBuildNoCamel);
			moveGenerator.ifDiscardCard(cardBuildNoCamel);
			moveGenerator.ifDone();
		}

		data.setBuilding(building);
		data.modCamels(-costCamels);
		data.modGold(-costGold);
		moveGenerator.ifConstructBuilding(building);
		return true;
	}


	protected boolean a_UseCard_ShopCubeOneMissing()
	{
		int cardShop = data.hasCard(ycard.card_place_board);
		if (cardShop < 0)
		{
			return false;
		}
		if (data.getCubes() <= 0)
		{
			return false;
		}
		trace("a_UseCard_ShopCubeOneMissing");

		int[] area = new int[3];
		boolean res = findBestPlace(area);
		if (!res)
		{
			return false;
		}

		res = a_UseCard_ShopCube(area[0], area[1], area[2]);
		if (!res)
		{
			error("a_UseCard_ShopCubeOneMissing");
		}

		return true;
	}


	private boolean findBestPlace(int[] area)
	{
		area[0] = 3;
		area[1] = 2;
		if (findArea(area))
		{
			return true;
		}
		area[0] = 2;
		area[1] = 2;
		if (findArea(area))
		{
			return true;
		}
		area[0] = 1;
		area[1] = 3;
		if (findArea(area))
		{
			return true;
		}
		area[0] = 0;
		area[1] = 3;
		if (findArea(area))
		{
			return true;
		}
		area[0] = 3;
		area[1] = 1;
		if (findArea(area))
		{
			return true;
		}
		area[0] = 2;
		area[1] = 1;
		if (findArea(area))
		{
			return true;
		}
		area[0] = 1;
		area[1] = 2;
		if (findArea(area))
		{
			return true;
		}
		area[0] = 0;
		area[1] = 2;
		if (findArea(area))
		{
			return true;
		}
		area[0] = 3;
		area[1] = 0;
		if (findArea(area))
		{
			return true;
		}

		if (data.boardArea[3][0].Owner < 0 && groupStarted(3) < 0)
		{
			area[0] = 3;
			area[1] = 0;
			area[2] = 0;
			return true;
		}

		area[0] = 2;
		area[1] = 0;
		if (findArea(area))
		{
			return true;
		}
		area[0] = 1;
		area[1] = 1;
		if (findArea(area))
		{
			return true;
		}
		area[0] = 0;
		area[1] = 1;
		if (findArea(area))
		{
			return true;
		}
		area[0] = 1;
		area[1] = 0;
		if (findArea(area))
		{
			return true;
		}
		area[0] = 0;
		area[1] = 0;
		if (findArea(area))
		{
			return true;
		}

		return false;
	}


	private boolean findArea(int[] area)
	{
		CubeGroup cg = data.boardArea[area[0]][area[1]];
		if (cg.Owner == player && (cg.maxCount - cg.actCount == 1))
		{
			for (int i = 0; i < cg.maxCount; i++)
			{
				if (!cg.used[i])
				{
					area[2] = i;
					return true;
				}
			}
		}
		return false;
	}


	protected boolean a_useCard_Caravan()
	{
		int card = data.hasCard(ycard.card_place_caravan);
		if (card < 0)
		{
			return false;
		}

		if (data.getCubes() <= 0)
		{
			return false;
		}

		trace("a_useCard_Caravan");
		// use card
		data.deleteCard(card);
		moveGenerator.ifDiscardCard(card);
		moveGenerator.ifDone();
		a_CubeToCaravanFromPool(player);
		return true;

	}


	protected boolean a_CubeToCaravanFromPool(int player)
	{
		data.theCaravan.addPlayer(player);
		// no move generator needed (if card)
		return true;
	}


	protected boolean a_UseCard_ShopCubeG(int region, int group)
	{
		int cardShop = data.hasCard(ycard.card_place_board);
		if (cardShop < 0)
		{
			return false;
		}
		if (data.getCubes() <= 0)
		{
			return false;
		}
		if (data.boardArea[region][group].Owner >= 0)
		{
			return false;
		}

		if (groupStarted(region) >= 0)
		{
			return false;
		}

		trace("a_UseCard_ShopCubeG");

		boolean res = a_UseCard_ShopCube(region, group, 0);
		if (!res)
		{
			error("a_UseCard_ShopCubeG");
		}

		return true;
	}


	private boolean a_UseCard_ShopCube(int region, int group, int field)
	{
		int card = data.hasCard(ycard.card_place_board);
		if (card < 0)
		{
			return false;
		}

		if (data.getCubes() <= 0)
		{
			return false;
		}
		if (data.boardArea[region][group].used[field])
		{
			return false;
		}
		if (!isGroupAllowed(region, group))
		{
			return false;
		}
		if (groupStarted(region) >= 0
				&& data.boardArea[region][group].Owner != player)
		{
			return false;
		}

		// use card
		data.deleteCard(card);
		moveGenerator.ifDiscardCard(card);

		setCube(region, group, field);
		moveGenerator.ifDone();
		trace("A_KarteNutzen_ShopStein " + region + " " + group + " " + field);
		return true;
	}


	private boolean isGroupAllowed(int region, int group)
	{
		CubeGroup c = data.boardArea[region][group];
		return (c.maxCount > 0 && c.Owner < 0) || (c.Owner == player);
	}


	private int groupStarted(int region)
	{
		for (int i = 0; i < data.boardArea[region].length; i++)
		{
			if (data.boardArea[region][i].Owner == player
					&& !data.boardArea[region][i].full)
			{
				return i;
			}
		}
		return -1;
	}


	protected void a_UseCard_Gold()
	{
		int card = data.hasCard(ycard.card_3_gold);
		if (card < 0)
		{
			return;
		}

		// use card
		data.modGold(3);
		data.deleteCard(card);
		moveGenerator.ifDiscardCard(card);
		moveGenerator.ifDone();
		trace("A_KarteNutzen_Gold");
	}


	protected void a_UseCard_Camel()
	{
		int card = data.hasCard(ycard.card_3_camels);
		if (card < 0)
		{
			return;
		}

		// use card
		data.modCamels(3);
		data.deleteCard(card);
		moveGenerator.ifDiscardCard(card);
		moveGenerator.ifDone();
		trace("A_KarteNutzen_Kamel");
	}


	protected void a_Take_Gold_Ext(int card)
	{
		trace("A_Nehme_Gold_Ext; card: " + card);
		boolean res = a_Take_Gold(card >= 0);

		if (!res) return;

		deleteCard(card, goldRow);
		deleteDiceRow(goldRow);
	}


	private boolean a_Take_Gold(boolean addDice)
	{
		int gold = numDice(goldRow); // buildings are included!
		if (gold == 0) return false;

		if (addDice) gold++;
		data.modGold(gold);

		moveGenerator.ifModGold(gold);

		return true;
	}


	protected boolean a_useCard_Trade(int gold, int camels)
	{
		// one parameter is always 0;
		// if a parameter is != 0, it should be traded to the other kind.

		int card = data.hasCard(ycard.card_swap_camels_gold);
		if (card < 0 || (gold == 0 && camels == 0) || (gold > 0 && camels > 0))
		{
			return false;
		}

		trace("a_useCard_Trade; gold: " + gold + " , camels: " + camels);

		if (gold > 0)
		{
			int tradeGold = Math.min(gold, data.getGold());
			if (tradeGold <= 0)
			{
				return false;
			}

			data.modGold(-tradeGold);
			data.modCamels(tradeGold);
			data.deleteCard(card);

			moveGenerator.ifDiscardCard(card);
			for (int i = 0; i < tradeGold; i++)
			{
				moveGenerator.ifTradeResource(1, 0); // from gold to camels
			}
			moveGenerator.ifDone();
			return true;
		}

		if (camels > 0)
		{
			int tradeCamels = Math.min(camels, data.getCamels());
			if (tradeCamels <= 0)
			{
				return false;
			}

			data.modGold(tradeCamels);
			data.modCamels(-tradeCamels);
			data.deleteCard(card);

			moveGenerator.ifDiscardCard(card);
			for (int i = 0; i < tradeCamels; i++)
			{
				moveGenerator.ifTradeResource(0, 1); // from gold to camels
			}
			moveGenerator.ifDone();
			return true;
		}
		return false;
	}


	protected void a_Take_Camels_Ext(int card)
	{
		trace("a_Take_Camels_Ext, card: " + card);
		boolean res = a_Take_Camels(card >= 0);

		if (!res) return;

		deleteCard(card, camelRow);
		deleteDiceRow(camelRow);
	}


	private boolean a_Take_Camels(boolean addDice)
	{
		int camels = numDice(camelRow); // buildings are included!
		if (camels == 0) return false;

		if (addDice) camels++;
		data.modCamels(camels);

		moveGenerator.ifModCamels(camels);

		return true;
	}


	protected void deleteDiceRow(int dice)
	{
		trace("DeleteWuerfelZeile and end of main action");

		if (dice >= 0)
		{
			moveGenerator.ifSelectDiceRow(dice);
		}
		moveGenerator.ifDone(); // end of main action !!
	}


	protected boolean a_TakeCard(int dice)
	{
		trace("A_NehmeKarte, row: " + dice);

		boolean res = !data.cardStackEmpty();

		if (res)
		{
			moveGenerator.ifDrawCard();
		}

		if ( res)  
		{
			deleteDiceRow(dice);
		}

		return res;
	}

	
	private boolean playerIsActiveOpponentThisRound(int thePlayer)
	{
		//opponent, not a robot, who has not moved this round
		boolean [] players = new boolean[max_nr_of_players];
		getActOpponents(players);
		return players[thePlayer];		
	}

	
	private boolean getActOpponents(boolean[] players)
	{
		Arrays.fill(players, false);
		boolean res = false;

		if (data.lastRound() && data.numberOfPlayers==4)
		{
			for (int i = 0; i < data.getPlayerCount(); i++)
			{
				if (i != player
						&& data.getVictoryPoints(i) > data.getVictoryPoints(player) && isHuman(i))
				{
					players[i] = true;
					res = true;
				}
			}
			return res;
		}

//normal sequence of players
		for (int i = 1; i < data.getPlayerCount(); i++)
		{
			int pl1 = (player + i) % data.getPlayerCount();
			if (pl1 == data.startPlayer)
			{
				break;
			}

			if (isHuman(pl1))
			{
				players[pl1] = true;
				res = true;
			}
		}
		return res;

	}


	private int maxInArray(int[] array)
	{
		int max = Integer.MIN_VALUE;
		int index = -1;

		for (int i = 0; i < array.length; i++)
		{
			if (array[i] > max)
			{
				max = array[i];
				index = i;
			}
		}
		return index;
	}


	protected int getDiceRowAgainstOpponent()
	{
		trace("getDiceRowAgainstOpponent");
		boolean[] opp = new boolean[max_nr_of_players];

		if (!getActOpponents(opp)) // nothing to do
		{
			return getHighDiceRow(opp);
		}

		int[] playerValue = new int[max_nr_of_players];
		int[] playerValueInx = new int[max_nr_of_players];
		int resultWay[][][] = new int[4][4][2];
		int[] zValue = new int[4];

		Arrays.fill(playerValue, -1);
		for (int i = 0; i < data.numberOfPlayers; i++)
		{
			if (opp[i])
			{
				int card = (data.hasSomeCard(i) ? 0 : -1);
				computeRealDiceValue(i, card, resultWay, zValue);
				playerValueInx[i] = maxInArray(zValue);
				playerValue[i] = zValue[playerValueInx[i]];
			}
		}

		int highValuePlayer = maxInArray(playerValue);
		int highValue = playerValue[highValuePlayer];
		int highValueInx = playerValueInx[highValuePlayer]; // as region index

		if (highValue >= 6)
		{
			return highValueInx + 1;
		}

		// last building

		if (data.getDiceCount(highValuePlayer, camelRow) >= 3)
		{
			return camelRow;
		}

		if (data.getDiceCount(highValuePlayer, goldRow) >= 5)
		{
			return goldRow;
		}

		for (int thePlayer = 0; thePlayer < data.numberOfPlayers; thePlayer++)
		{
			if (opp[thePlayer])
			{
				if (data.countBuildings(thePlayer) == 5)
				{
					buildings b = data.getMissingBuilding(thePlayer);

					if (data.getCamels(thePlayer) < buildings
							.camelCost(b.index)
							&& data.getCamels(thePlayer)
									+ data.getDiceCount(thePlayer, camelRow) >= buildings
										.camelCost(b.index))
					{
						return camelRow;
					}
					if (data.getGold(thePlayer) < buildings.goldCost(b.index)
							&& data.getGold(thePlayer)
									+ data.getDiceCount(thePlayer, goldRow) >= buildings
										.goldCost(b.index))
					{
						return goldRow;
					}
				}
			}
		}

		if (data.getDiceCount(highValuePlayer, camelRow) >= 2)
		{
			return camelRow;
		}
		
		if (data.getDiceCount(highValuePlayer, goldRow) >= 4 && data.getWeek()==1)
		{
			return goldRow;
		}

		
		if (highValue >= 3)
		{
			return highValueInx + 1;
		}

		if (data.getDiceCount(highValuePlayer, goldRow) >= 4)
		{
			return goldRow;
		}


		return getHighDiceRow(opp);

	}


	@SuppressWarnings("unused")
	private int getLowDiceRow()
	{
		for (int i = 1; i < 6; i++)
		{
			int dice = numDice(i);
			if (dice > 0)
			{
				return i;
			}
		}
		int dice = numDice(0);
		if (dice > 0)
		{
			return 0;
		}
		return -1;
	}


	private int getHighDiceRow(boolean[] opp)
	{
		int dice = -1;
		
		for (int i = 4; i >= 1; i--)
		{
			if (data.isWhiteDiceAvailable(i)
					&& data.isRegionUsable(opp, i-1))
			{
				return i;
			}
		}
		
		dice = numDice(0);
		if (dice > 0)
		{
			return 0;
		}
	
		dice = numDice(5);
		if (dice > 0)
		{
			return 5;
		}
	
	
		for (int i = 4; i >= 1; i--)
		{
			dice = numDice(i);
			if (dice > 0)
			{
				return i;
			}
		}

		return -1;
	}

	
	protected void supervisorDesignateCubeStd()
	{
		int supStreet = data.supervisor.nextStreet;
		int supPos = data.supervisor.nextPos;
		int owner0 = -1;
		int owner1 = -1; //both owners must exist
		int region = -1;
		int group = -1;
		int field = -1;
		int firstLink = -1;
		int ownLink = -1;
		
		if(data.supervisor.getConnect()[supStreet][supPos].length<=1)
		{
			firstLink = 0;
		}
		else
		{
			owner0 = data.getSupervisorOwner(supStreet, supPos, 0);
			owner1 = data.getSupervisorOwner(supStreet, supPos, 1);
			if (owner0 == player) ownLink = 0;
			if (owner1 == player) ownLink = 1;
			if (ownLink > -1)
			{
				if (data.theCaravan.openCount() == 1)
				{
					firstLink = ownLink;
				}
				else
				{
					firstLink = 1-ownLink;
				}
			}
			else
			{
				if (data.theCaravan.cubesOfPlayer(owner0) > data.theCaravan.cubesOfPlayer(owner1))
				{
					firstLink = 0;
				}
				else
				{
					firstLink = 1;
				}
			}
		}
		
			region = data.supervisor.getRegion(supStreet, supPos, firstLink);
			group = data.supervisor.getGroup(supStreet, supPos, firstLink);
			field = data.supervisor.getField(supStreet, supPos, firstLink);
			moveGenerator.ifDesignateCube(region,group, field);
	}
	
	protected boolean isCriticalSupervisorGroup(int region, int group)
	{
		// critical groups for double supervisor moves
		if(region == 0 && group == 0)
		{return true;}
		
		if(region == 1 && group == 0)
		{return true;}

		if(region == 3 && group == 0)
		{return true;}
		
		return false;
	}


	protected void trace(String message)
	{
		if (myDebug)
		{	G.print(Http.stackTrace("*** TRACE: " + message));
		}
	}


	protected void error(String message)
	{
		G.print(Http.stackTrace("*** ERROR: YspahanPlayStratCore - " + message));
	}

} // end class

