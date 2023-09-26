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

import lib.G;
import lib.Http;


/**
 * @author Günther Rosenbaum
 * 
 */
public class YspahanPlayData implements YspahanPlayStratConst, YspahanConstants
{

	public class DiceRow
	{
		public int numMyDice = 0;
		public int value = 0;
		public int numWhite = 0;
		public int numYellow = 0;
		public int Row = 0;


		public DiceRow(int row)
		{
			this.Row = row;
		}
	}

	public class CubeGroup
	{
		public int region = -1;
		public int group = -1;
		public int maxCount = 0;
		public int actCount = 0;
		public int vPoints = 0;
		public int Owner = -1;
		public boolean full = false;
		public boolean[] used;


		public CubeGroup(int region, int group, int nrCubes, int vpoints)
		{
			this.region = region;
			this.group = group;
			this.vPoints = vpoints;
			used = new boolean[max_nr_of_souks];
			for (int i = 0; i < used.length; i++)
			{
				used[i] = false;
			}
			this.maxCount = nrCubes;
		}
	}

	public class CPlayerinfo
	{
		private int gold = 0;
		public int camels = 0;
		public int cubes = 0;
		public int victoryPoints = 0;
		public ArrayList<ycard> pCards;
		public boolean[] buildings;


		public CPlayerinfo()
		{
			pCards = new ArrayList<YspahanConstants.ycard>();
			buildings = new boolean[max_nr_of_buildings];
			for (int i = 0; i < buildings.length; i++)
			{
				buildings[i] = false;
			}
			;
		}
	}

	public class TheCaravan
	{
		private int[] cubes;
		private int actcount;
		private int maxcount;
		private int numPlayers;
		private int player; // actual player


		public TheCaravan(int numPlayers, int actPlayer)
		{
			this.player = actPlayer;
			this.numPlayers = numPlayers;
			cubes = new int[3 * numPlayers];
			maxcount = 3 * numPlayers;
			actcount = 0;
			Arrays.fill(cubes, -1);
		}


		public int maxCount()
		{
			return maxcount;
		}


		public int actCount()
		{
			return actcount;
		}

		public int openCount()
		{
			return maxcount-actcount;
		}

		public void setCube(int field, int pl)
		{
			if (field >= cubes.length)
			{
				error("wrong index");
				return;
			}
			cubes[field] = pl;
			actcount = count();
		}


		public int count()
		{
			int cnt = 0;
			for (int i = 0; i < cubes.length; i++)
			{
				cnt += (cubes[i] >= 0 ? 1 : 0);
			}
			return cnt;
		}


		public void addPlayer(int pl)
		{
			setCube(actcount, pl);
		}

		public int cubesOfPlayer(int thePlayer)
		{
			int count = 0;
			for (int i = 0; i < cubes.length; i++)
			{
				int plyr = cubes[i];
				if (plyr == thePlayer)
				{
					count++;
				}
			}
			return count;
			
		}

		public int factorForPlayer(int pl)
		{
			int level = 0;
			for (int i = 0; i < cubes.length; i++)
			{
				int plyr = cubes[i];
				if (plyr == pl)
				{
					level = (i + numPlayers) / numPlayers;
				}
			}
			return level;
		}

		public int factorForPlayer()
		{
			return factorForPlayer(this.player);
		}		
	}

	public class Position
	{
		int region;
		int group;
		int field;	
		
		public Position(int region,int group,int field)
		{
			this.region = region;
			this.group = group;
			this.field = field;
		}
		public Position()
		{
			this(0,0,0);
		}
	}
		
	
	public class Supervisor
	{
		public int getStreet()
		{
			return street;
		}

		public void setStreet(int street)
		{
			this.street = street;
		}

		public int getPos()
		{
			return pos;
		}

		public void setPos(int pos)
		{
			this.pos = pos;
		}

		public Position[][][] getConnect()
		{
			return connect;
		}
		
		public int getStreetLength(int street)
		{
			return connect[street].length;
		}
		
		public int getRegion(int street, int pos, int link)
		{
			if (connect[street][pos][link] != null)
			{
				return connect[street][pos][link].region;
			}
			return -1;
		}

		
		public int getGroup(int street, int pos, int link)
		{
			if (connect[street][pos][link] != null)
			{
				return connect[street][pos][link].group;
			}
			return -1;
		}

		public int getField(int street, int pos, int link)
		{
			if (connect[street][pos][link] != null)
			{
				return connect[street][pos][link].field;
			}
			return -1;
		}

		private int street;
		private int pos;
		private Position [][][]   connect;  //street, position, Link1/2	
		public int nextStreet;
		public int nextPos;
		
		public Supervisor()
		{
			street = 0;
			pos = 0;
			nextStreet = 0;
			nextPos = 0;
			
			connect = new Position [4][][];
			connect[0] = new Position [8][];
			connect[1] = new Position [5][];
			connect[2] = new Position [11][];
			connect[3] = new Position [8][];
			for (int dir = 0; dir < connect.length; dir++)
			{
				for (int pos = 0; pos < connect[dir].length; pos++)
				{
					connect[dir][pos] = new Position[2];
				}					
			}
			connect[0][0] = null;

			connect[0][1] =  new Position[2];
			connect[0][1][0] = new Position(3,0,0);
			connect[0][1][1] = new Position(1,0,0);
			
			connect[0][2] =  new Position[1];
			connect[0][2][0] = new Position(1,0,1);
			
			connect[0][3] =  new Position[1];
			connect[0][3][0] = new Position(3,2,0);
			
			connect[0][4] =  new Position[1];
			connect[0][4][0] = new Position(1,2,0);
			
			connect[0][5] =  new Position[1];
			connect[0][5][0] = new Position(3,2,1);
			
			connect[0][6] =  new Position[1];
			connect[0][6][0] = new Position(1,2,1);
			
			connect[0][7] =  new Position[1];
			connect[0][7][0] = new Position(3,2,2);
			
			
			connect[1][0] = null;

			connect[1][1] =  new Position[1];
			connect[1][1][0] = new Position(3,0,0);
			
			connect[1][2] =  new Position[1];
			connect[1][2][0] = new Position(2,2,0);
			
			connect[1][3] =  new Position[1];
			connect[1][3][0] = new Position(3,1,0);
			
			connect[1][4] =  new Position[1];
			connect[1][4][0] = new Position(2,2,1);
			

			connect[2][0] = null;

			connect[2][1] =  new Position[1];
			connect[2][1][0] = new Position(0,0,1);
			
			connect[2][2] =  new Position[1];
			connect[2][2][0] = new Position(2,0,0);
		
			connect[2][3] =  new Position[1];
			connect[2][3][0] = new Position(0,0,0);
			
			connect[2][4] =  new Position[1];
			connect[2][4][0] = new Position(2,0,1);
			
			connect[2][5] =  new Position[1];
			connect[2][5][0] = new Position(0,2,0);
			
			connect[2][6] =  new Position[1];
			connect[2][6][0] = new Position(2,1,0);
			
			connect[2][7] =  new Position[1];
			connect[2][7][0] = new Position(0,2,1);
			
			connect[2][8] =  new Position[1];
			connect[2][8][0] = new Position(2,1,1);
			
			connect[2][9] =  new Position[1];
			connect[2][9][0] = new Position(0,3,0);
			
			connect[2][10] =  new Position[1];
			connect[2][10][0] = new Position(2,1,2);
			

			connect[3][0] = null;

			connect[3][1] =  new Position[2];
			connect[3][1][0] = new Position(1,0,0);
			connect[3][1][1] = new Position(0,0,1);
			
			connect[3][2] =  new Position[1];
			connect[3][2][0] = new Position(0,0,2);
			
			connect[3][3] =  new Position[1];
			connect[3][3][0] = new Position(1,1,0);
			
			connect[3][4] =  new Position[1];
			connect[3][4][0] = new Position(0,1,0);
			
			connect[3][5] =  new Position[1];
			connect[3][5][0] = new Position(1,3,1);
			
			connect[3][6] =  new Position[1];
			connect[3][6][0] = new Position(0,1,1);
			
			connect[3][7] =  new Position[1];
			connect[3][7][0] = new Position(1,3,0);

		}
		
	}
	

	public int numberOfPlayers = 0;
	public int startPlayer = 0;
	private int player = -1;
	boolean cardStackEmpty = false;

	public CPlayerinfo[] playerInfo;
	private DiceRow[] diceTower;
	private int numWeeks = 0;
	private int numDays = 0;

	public CubeGroup[][] boardArea;
	public Supervisor supervisor;

	public TheCaravan theCaravan;

	// Dice

	public int getPlayer()
	{
		return player;
	}


	public void checkCubeGroups()
	{

		for (CubeGroup[] area : boardArea)
		{
			for (CubeGroup cubeGroup : area)
			{
				cubeGroup.actCount = 0;
				for (boolean bes : cubeGroup.used)
				{
					if (bes)
					{
						cubeGroup.actCount++;
					}
				}
				cubeGroup.full = (cubeGroup.actCount == cubeGroup.maxCount);
			}
		}
	}


	public YspahanPlayData(int player, int anzplayer)
	{
		if (player < 0 || player > max_nr_of_players - 1 || anzplayer < 3
				|| anzplayer > 4)
		{
			error("YspahanPlayData ");
		}

		playerInfo = new CPlayerinfo[max_nr_of_players];
		for (int i = 0; i < playerInfo.length; i++)
		{
			playerInfo[i] = new CPlayerinfo();
			playerInfo[i].pCards = new ArrayList<YspahanConstants.ycard>();
		}
		;
		diceTower = new DiceRow[6];
		for (int i = 0; i < diceTower.length; i++)
		{
			diceTower[i] = new DiceRow(i);
		}

		boardArea = new CubeGroup[nr_of_regions][];
		boardArea[0] = new CubeGroup[4];
		boardArea[0][0] = new CubeGroup(0, 0, 3, 3);
		boardArea[0][1] = new CubeGroup(0, 1, 4, 4);
		boardArea[0][2] = new CubeGroup(0, 2, 5, 6);
		boardArea[0][3] = new CubeGroup(0, 3, 6, 8);

		boardArea[1] = new CubeGroup[4];
		boardArea[1][0] = new CubeGroup(1, 0, 2, 3);
		boardArea[1][1] = new CubeGroup(1, 1, 3, 4);
		boardArea[1][2] = new CubeGroup(1, 2, 4, 6);
		boardArea[1][3] = new CubeGroup(1, 3, 5, 8);

		boardArea[2] = new CubeGroup[3];
		boardArea[2][0] = new CubeGroup(2, 0, 2, 4);
		boardArea[2][1] = new CubeGroup(2, 1, 3, 6);
		boardArea[2][2] = new CubeGroup(2, 2, 4, 8);

		boardArea[3] = new CubeGroup[3];
		boardArea[3][0] = new CubeGroup(3, 0, 1, 4);
		boardArea[3][1] = new CubeGroup(3, 1, 2, 6);
		boardArea[3][2] = new CubeGroup(3, 2, 3, 12);


		this.supervisor = new Supervisor();
		this.player = player;
		this.numberOfPlayers = anzplayer;

		this.theCaravan = new TheCaravan(anzplayer, player);
		this.supervisor = new Supervisor();

		for (int i = 0; i < numberOfPlayers; i++)
		{
			CPlayerinfo spInfo = playerInfo[i];
			for (int j = 0; j < spInfo.buildings.length; j++)
			{
				spInfo.buildings[j] = false;
			}
			spInfo.gold = 0;
			spInfo.camels = 0;
			spInfo.victoryPoints = 0;
		}

		// dice
		for (int i = 0; i < diceTower.length; i++)
		{
			diceTower[i].value = 0;
			diceTower[i].numYellow = 0;
			diceTower[i].numWhite = 0;
			diceTower[i].numMyDice = 0;
		}
		
	}


	public YspahanPlayData(YspahanBoard bd, int player, int anzplayer)
	{
		this(player, anzplayer);

		if (bd == null)
		{
			return;
		} // for unit test the board will not be used

		// move data from board to internal data structure:

		this.startPlayer = bd.startPlayer;
		numDays = (bd.gameDay % 7) + 1;
		numWeeks = (bd.gameDay / 7) + 1;
		
		
		YspahanCell cell = null;
		for (int i = 0; i < numberOfPlayers; i++)
		{
			CPlayerinfo spInfo = playerInfo[i];
			for (int j = 0; j < spInfo.buildings.length; j++)
			{
				YspahanCell bb = bd.playerBoards[i].buildings[j];
				spInfo.buildings[j] = (bb.topChip() != null);
			}
			spInfo.gold = bd.goldCount(i);
			spInfo.camels = bd.camelCount(i);
			spInfo.victoryPoints = bd.currentScoreForPlayer(i);
			cardStackEmpty = (bd.cards.topChip() == null);
			spInfo.cubes = 100; // cubes are unlimited here

			// cards
			cell = bd.getLocalCell(yrack.Misc_Track, (char) ('A' + i), 2);
			for (YspahanChip chip : cell.chipStack)
			{
				if (chip != null)
				{
					spInfo.pCards.add(chip.getCard());
				}
			}

			cell = bd.getLocalCell(yrack.Misc_Track, (char) ('A' + i), 4);
		}

		// caravan
		for (int i = 0; i < bd.caravan.caravan.length; i++)
		{
			YspahanChip chip = bd.caravan.caravan[i].topChip();
			if (chip != null)
			{
				this.theCaravan.setCube(i, bd.playerWithColor(chip));
			}
		}

		// supervisor
		int sup = bd.supervisor.row;
		supervisor.street = getSupStreet(sup);
		supervisor.pos = getSupPos(sup);
		if(bd.nextSupervisor != null)
		{
			sup = bd.nextSupervisor.row;
			supervisor.nextStreet = getSupStreet(sup);
			supervisor.nextPos = getSupPos(sup);
		}				
		
		setBoardArea(bd, 0, 0, 0, yrack.Bag_Neighborhood, 'C', 0);
		setBoardArea(bd, 0, 0, 1, yrack.Bag_Neighborhood, 'C', 1);
		setBoardArea(bd, 0, 0, 2, yrack.Bag_Neighborhood, 'C', 2);

		setBoardArea(bd, 0, 1, 0, yrack.Bag_Neighborhood, 'D', 0);
		setBoardArea(bd, 0, 1, 1, yrack.Bag_Neighborhood, 'D', 1);
		setBoardArea(bd, 0, 1, 2, yrack.Bag_Neighborhood, 'D', 2);
		setBoardArea(bd, 0, 1, 3, yrack.Bag_Neighborhood, 'D', 3);

		setBoardArea(bd, 0, 2, 0, yrack.Bag_Neighborhood, 'B', 1);
		setBoardArea(bd, 0, 2, 1, yrack.Bag_Neighborhood, 'B', 0);
		setBoardArea(bd, 0, 2, 2, yrack.Bag_Neighborhood, 'B', 4);
		setBoardArea(bd, 0, 2, 3, yrack.Bag_Neighborhood, 'B', 3);
		setBoardArea(bd, 0, 2, 4, yrack.Bag_Neighborhood, 'B', 2);

		setBoardArea(bd, 0, 3, 0, yrack.Bag_Neighborhood, 'A', 0);
		setBoardArea(bd, 0, 3, 1, yrack.Bag_Neighborhood, 'A', 1);
		setBoardArea(bd, 0, 3, 2, yrack.Bag_Neighborhood, 'A', 2);
		setBoardArea(bd, 0, 3, 3, yrack.Bag_Neighborhood, 'A', 3);
		setBoardArea(bd, 0, 3, 4, yrack.Bag_Neighborhood, 'A', 4);
		setBoardArea(bd, 0, 3, 5, yrack.Bag_Neighborhood, 'A', 5);

		setBoardArea(bd, 1, 0, 0, yrack.Barrel_Neighborhood, 'A', 0);
		setBoardArea(bd, 1, 0, 1, yrack.Barrel_Neighborhood, 'A', 1);

		setBoardArea(bd, 1, 1, 0, yrack.Barrel_Neighborhood, 'C', 0);
		setBoardArea(bd, 1, 1, 1, yrack.Barrel_Neighborhood, 'C', 1);
		setBoardArea(bd, 1, 1, 2, yrack.Barrel_Neighborhood, 'C', 2);

		setBoardArea(bd, 1, 2, 0, yrack.Barrel_Neighborhood, 'B', 0);
		setBoardArea(bd, 1, 2, 1, yrack.Barrel_Neighborhood, 'B', 1);
		setBoardArea(bd, 1, 2, 2, yrack.Barrel_Neighborhood, 'B', 2);
		setBoardArea(bd, 1, 2, 3, yrack.Barrel_Neighborhood, 'B', 3);

		setBoardArea(bd, 1, 3, 0, yrack.Barrel_Neighborhood, 'D', 0);
		setBoardArea(bd, 1, 3, 1, yrack.Barrel_Neighborhood, 'D', 1);
		setBoardArea(bd, 1, 3, 2, yrack.Barrel_Neighborhood, 'D', 2);
		setBoardArea(bd, 1, 3, 3, yrack.Barrel_Neighborhood, 'D', 3);
		setBoardArea(bd, 1, 3, 4, yrack.Barrel_Neighborhood, 'D', 4);

		setBoardArea(bd, 2, 0, 0, yrack.Chest_Neighborhood, 'B', 1);
		setBoardArea(bd, 2, 0, 1, yrack.Chest_Neighborhood, 'B', 0);

		setBoardArea(bd, 2, 1, 0, yrack.Chest_Neighborhood, 'A', 2);
		setBoardArea(bd, 2, 1, 1, yrack.Chest_Neighborhood, 'A', 1);
		setBoardArea(bd, 2, 1, 2, yrack.Chest_Neighborhood, 'A', 0);

		setBoardArea(bd, 2, 2, 0, yrack.Chest_Neighborhood, 'C', 0);
		setBoardArea(bd, 2, 2, 1, yrack.Chest_Neighborhood, 'C', 1);
		setBoardArea(bd, 2, 2, 2, yrack.Chest_Neighborhood, 'C', 2);
		setBoardArea(bd, 2, 2, 3, yrack.Chest_Neighborhood, 'C', 3);

		setBoardArea(bd, 3, 0, 0, yrack.Vase_Neighborhood, 'A', 0);

		setBoardArea(bd, 3, 1, 0, yrack.Vase_Neighborhood, 'B', 0);
		setBoardArea(bd, 3, 1, 1, yrack.Vase_Neighborhood, 'B', 1);

		setBoardArea(bd, 3, 2, 0, yrack.Vase_Neighborhood, 'C', 0);
		setBoardArea(bd, 3, 2, 1, yrack.Vase_Neighborhood, 'C', 1);
		setBoardArea(bd, 3, 2, 2, yrack.Vase_Neighborhood, 'C', 2);

		checkCubeGroups();

		// dice
		for (int i = 0; i < diceTower.length; i++)
		{
			YspahanCell dc = bd.diceTower[i];
			if (dc.topChip() != null)
			{
				diceTower[i].value = dc.topChip().dieValue();
				for (YspahanChip yc : dc.chipStack)
				{
					if (yc != null)
					{
						if (yc.getDie().yellow)
						{
							diceTower[i].numYellow++;
						}
						else
						{
							diceTower[i].numWhite++;
						}
					}
				}
			}
			checkDiceRows();
		}

//		System.out.println("End constructor YspahanPlayData");
	}

	
	
	public int getSupStreet(int sup)
	{
		if (sup < 10)
		{ return 2;}
		else
			if(sup < 18)
			{ return 0; }
			else
			if (sup < 22)
			{ return 1; }
			else
				if (sup == 22)
				{ return 0;}
				else
				{ return 3; }
	}


	public int getSupPos(int sup)
	{
		if (sup < 10)
		{ return 10-sup;}
		else
			if(sup < 18)
			{ return sup-10;}
			else
			if (sup < 22)
			{ return 22-sup;}
			else
				if (sup == 22)
				{ return 0;}
				else
				{ return sup-22;}
	}

	
	public void checkDiceRows()
	{
		for (int i = 0; i < diceTower.length; i++)
		{
			if (this.player == this.startPlayer)
			{
				diceTower[i].numMyDice = diceTower[i].numWhite
						+ diceTower[i].numYellow;
			}
			else
			{
				diceTower[i].numMyDice = diceTower[i].numWhite;
			}
		}
	}


	private void setBoardArea(YspahanBoard bd, int region, int group, int field,
			yrack area, char extgroup, int extfield)
	{
		YspahanChip c = bd.getLocalCell(area, extgroup, extfield).topChip();
		if (c != null)
		{
			boardArea[region][group].used[field] = true;
			boardArea[region][group].Owner = bd.playerWithColor(c);
		}
	}


	public boolean cardStackEmpty()
	{
		return cardStackEmpty;
	}


	public int getPlayerCount()
	{
		return numberOfPlayers;
	}


	public int getSupervisorOwner(int street, int pos, int link)
	{
		if (supervisor.connect[street][pos].length<link+1) return -1;
		return boardArea[supervisor.getRegion(street, pos, link)][supervisor.getGroup(street, pos, link)].Owner;
	}
	
	/**
	 * 
	 * @param player
	 * @return Amount of gold
	 */
	public int getGold(int player)
	{
		return playerInfo[player].gold;
	}


	public void setGold(int player, int gold)
	{
		if (player < 0 || player > max_nr_of_players - 1 || gold < 0
				|| gold > 1000)
		{
			error("setGold ");
		}

		playerInfo[player].gold = gold;
	}


	public void setGold(int gold)
	{
		setGold(this.player, gold);
	}


	public void modGold(int gold)
	{
		setGold(getGold() + gold);
	}


	/**
	 * 
	 * @return Amount of gold of actual player
	 */
	public int getGold()
	{
		return getGold(this.player);
	}


	public int getVictoryPoints()
	{
		return getVictoryPoints(player);
	}
	
	
	public int getVictoryPoints(int thePlayer)
	{
		return playerInfo[thePlayer].victoryPoints;
	}
	
	public int getCubes()
	{
		return playerInfo[player].cubes;
	}


	public void setCubes(int cubes)
	{
		playerInfo[player].cubes = cubes;
	}


	public void setCamels(int player, int camels)
	{
		if (player < 0 || player > max_nr_of_players - 1 || camels < 0
				|| camels > 1000)
		{
			error("setCamels ");
		}

		playerInfo[player].camels = camels;
	}


	public void setCamels(int camels)
	{
		setCamels(this.player, camels);
	}


	/**
	 * 
	 * @param player
	 * @return Amount of camels
	 */
	public int getCamels(int player)
	{
		return playerInfo[player].camels;
	}


	/**
	 * 
	 * @return Amount of camels of actual player
	 */
	public int getCamels()
	{
		return playerInfo[player].camels;
	}


	public void modCamels(int camels)
	{
		setCamels(getCamels() + camels);
	}


	/**
	 * 
	 * @return get nr of day in week (1..7)
	 */
	public int getActDaysInWeek()
	{
		return numDays;
	}


	/**
	 * 
	 * @return nr of actual week (1..3)
	 */
	public int getWeek()
	{
		return numWeeks;
	}


	/**
	 * 
	 * @return Absolute number of rounds in this game
	 */
	public int getRounds()
	{
		return (getWeek()-1) * 7 + getActDaysInWeek();
	}


	public boolean lastRound()
	{
		return getRounds()>=21;
	}
	
	public void setBuilding(int building)
	{
		// building 1..6
		if (player < 0 || player > max_nr_of_players - 1 || building < 1
				|| building > 6)
		{
			error("hasBuilding ");
		}

		playerInfo[player].buildings[building - 1] = true;
	}


	public void setBuilding(int player, int building)
	{
		// building 1..6
		if (player < 0 || player > max_nr_of_players - 1 || building < 1
				|| building > 6)
		{
			error("hasBuilding ");
		}

		playerInfo[player].buildings[building - 1] = true;
	}


	public int countBuildings(int thePlayer)
	{
		int count = 0;
		for (int building = 1; building <= 6; building++)
		{
			count = count + (hasBuilding(thePlayer, building) ? 1 : 0);
		}
		return count;
	}

	
	public buildings getMissingBuilding(int thePlayer)
	{
		for (int building = 1; building <= 6; building++)
		{
			if(!hasBuilding(thePlayer, building)) { return buildings.find(building); }
		}
		return null;
	}

	
	/**
	 * 
	 * @param player
	 * @param building
	 * @return true, if the building is constructed.
	 */
	public boolean hasBuilding(int player, int building)
	{
		// building 1..6
		if (player < 0 || player > max_nr_of_players - 1 || building < 1
				|| building > 6)
		{
			error("hasBuilding ");
		}

		return playerInfo[player].buildings[building - 1];
	}


	public boolean hasBuilding(int building)
	{
		return hasBuilding(player, building);
	}


	/**
	 * 
	 * @param building
	 * @return true, if the building is constructed.
	 */
	public boolean hasBuilding(buildings building)
	{
		// building 1..6
		if (building.index < 1 || building.index > 6)
		{
			error("hasBuilding ");
		}

		return playerInfo[this.player].buildings[building.index - 1];
	}


	public boolean hasBuilding(int thePlayer, buildings building)
	{
		// building 1..6
		if (building.index < 1 || building.index > 6)
		{
			error("hasBuilding ");
		}

		return playerInfo[thePlayer].buildings[building.index - 1];
	}

	
	/**
	 * 
	 * @param region
	 * @param group
	 * @return check, whether the souk group is complete or not
	 */
	public boolean soukGroupComplete(int region, int group)
	{
		if (region < 0 || region > 3 || group < 0 || group > 3)
		{
			error("soukGroupComplete ");
		}
		try
		{
			return boardArea[region][group].full;
		}
		catch (Throwable e)
		{
			return false;
		}
	}


	/**
	 * 
	 * @param region
	 * @param group
	 * @return Victory points of this souk group
	 */
	public int soukGroupVictoryPoints(int region, int group)
	{
		try
		{
			if (region < 0 || region > 3 || group < 0 || group > 3)
			{
				error("soukGroupVictoryPoints ");
			}

			return boardArea[region][group].vPoints;
		}
		catch (Throwable e)
		{
			return 0;
		}
	}


	/**
	 * 
	 * @param region
	 * @param player
	 * @return index of the smallest group, if there is a started and not
	 *         completed group of that player; otherwise -1.
	 */
	public int groupStarted(int region, int player)
	{
		if (player < 0 || player > max_nr_of_players - 1 || region < 0
				|| region > 3)
		{
			error("groupStarted ");
		}

		for (int i = 0; i < boardArea[region].length; i++)
		{
			CubeGroup cGroup = boardArea[region][i];
			if (cGroup.Owner == player && !cGroup.full)
			{
				return i;
			}
			;
		}
		return -1;
	}


	/**
	 * 
	 * @param region
	 * @return true, if there is an empty group in this region.
	 */
	public boolean isEmptyGroupInRegion(int region)
	{
		if (region < 0 || region > 3)
		{
			error("isEmptyGroupInRegion ");
		}
		for (int i = 0; i < boardArea[region].length; i++)
		{
			CubeGroup cGroup = boardArea[region][i];
			if (cGroup.Owner == -1 && cGroup.maxCount > 0)
			{
				return true;
			}
			;
		}
		return false;
	}


	/***
	 * 
	 * @param region
	 *            0..3
	 * @return true, if there is place for a cube of the actual player.
	 */
	public boolean isRegionUsable(int region)
	{
		// 0..3 region
		return isRegionUsable(player, region);
	}

	
	public boolean isRegionUsable(int thePlayer, int region)
	{
		// 0..3 region
		if (region < 0 || region > 3)
		{
			error("isRegionUsable ");
		}
		return isEmptyGroupInRegion(region)
				|| (groupStarted(region,thePlayer) != -1);
	}
	
	public boolean isRegionUsable(boolean[] opp, int region)
	{
		// 0..3 region
		if (region < 0 || region > 3)
		{
			error("isRegionUsable ");
		}
		
		boolean res = false;
		
		for (int thePlayer = 0; thePlayer < getPlayerCount(); thePlayer++)
		{
			if (opp[thePlayer])
			{
				res = res || isEmptyGroupInRegion(region) 	|| (groupStarted(region, thePlayer) != -1);
			}
		}
		return res;
	}

	
	/**
	 * 
	 * @param region
	 * @return return index of the smallest empty group (-1, if there is no
	 *         empty group)
	 */
	public int getSmallestEmptyGroup(int region)
	{
		if (region < 0 || region > 3)
		{
			error("getSmallestEmptyGroup ");
		}

		// the size of the groups is increasing from group 0 ... 4
		for (int i = 0; i < boardArea[region].length; i++)
		{
			CubeGroup cGroup = boardArea[region][i];
			if (cGroup.Owner == -1 && cGroup.maxCount > 0)
			{
				return i;
			}
			;
		}
		return -1;
	}


	/**
	 * 
	 * @param region
	 * @return return index of the largest empty group (-1, if there is no empty
	 *         group)
	 */
	public int getLargestEmptyGroup(int region)
	{
		if (region < 0 || region > 3)
		{
			error("getLargestEmptyGroup ");
		}
		// the size of the groups is increasing from group 0 ... 4
		for (int i = boardArea[region].length - 1; i >= 0; i--)
		{
			CubeGroup cGroup = boardArea[region][i];
			if (cGroup.Owner == -1 && cGroup.maxCount > 0)
			{
				return i;
			}
			;
		}
		return -1;
	}


	/**
	 * 
	 * @param region
	 * @param group
	 * @param player
	 * @return true, if the player can place a cube in this group.
	 */
	public boolean isGroupUsable(int region, int group, int player)
	{
		if (player < 0 || player > max_nr_of_players - 1 || region < 0
				|| region > 3 || group < 0 || group > 3)
		{
			error("isGroupUsable ");
		}
		try
		{
			CubeGroup cGroup = boardArea[region][group];
			if (cGroup.full)
			{
				return false;
			}
			;
			if (cGroup.Owner == -1)
			{
				return true;
			}
			;
			if (cGroup.Owner == player)
			{
				return true;
			}
			;
			return false;
		}
		catch (Throwable e)
		{
			return false;
		}
	}


	/**
	 * 
	 * @param region
	 * @param group
	 * @param player
	 * @return true, if group is empty or owned by player.
	 */
	public boolean isGroupAllowed(int region, int group, int player)
	{ // unclear ?? "istGruppeNutzbar"
		if (player < 0 || player > max_nr_of_players - 1 || region < 0
				|| region > 3 || group < 0 || group > 3)
		{
			error("isGroupAllowed ");
		}
		try
		{
			CubeGroup cGroup = boardArea[region][group];
			if (cGroup.Owner == -1)
			{
				return true;
			}
			;
			if (cGroup.Owner == player)
			{
				return true;
			}
			;
			return false;
		}
		catch (Throwable e)
		{
			return false;
		}
	}


	/**
	 * 
	 * @param player
	 * @param region
	 * @param anzahlW
	 *            Number of cubes to place in region.
	 * @return Largest group, which could be completed(because of supervisor,
	 *         there might be more than one incomplete group).
	 */
	public int groupStartedD(int player, int region, int anzahlW)
	{ //
		if (player < 0 || player > max_nr_of_players - 1 || region < 0
				|| region > 3 || anzahlW < 0)
		{
			error("groupStartedD ");
		}

		for (int i = boardArea[region].length - 1; i >= 0; i--)
		{
			CubeGroup cGroup = boardArea[region][i];
			if (cGroup.Owner == player && !cGroup.full)
			{
				if (cGroup.maxCount - cGroup.actCount <= anzahlW)
				{
					return i;
				}
				;
			}
			;
		}
		return groupStarted(region, player);
	}


	public void deleteCard(int card)
	{
		if (card < 0)
		{
			error("deleteCard ");
		}

		for (int i = 0; i < playerInfo[player].pCards.size(); i++)
		{
			ycard c = playerInfo[player].pCards.get(i);
			if (c.ordinal() == card)
			{
				playerInfo[player].pCards.remove(i);
			}
		}
	}


	public int hasCard(ycard card)
	{
		int inx = playerInfo[player].pCards.indexOf(card);
		return (inx == -1 ? -1 : playerInfo[player].pCards.get(inx).ordinal());
	}


	public int countCard(ycard card)
	{
		int count = 0;
		for (int i = 0; i < playerInfo[player].pCards.size(); i++)
		{
			ycard playerCard = playerInfo[player].pCards.get(i);
			if (playerCard != null)
			{
				count = count + (playerCard.ordinal() == card.ordinal() ? 1 : 0);
			}			
		}
		return count;
	}

	
	public void addCard(ycard card)
	{
		playerInfo[player].pCards.add(card);
	}


	public boolean hasSomeCard()
	{
		return hasSomeCard(player);
	}


	public boolean hasSomeCard(int thePlayer)
	{
		return !playerInfo[thePlayer].pCards.isEmpty();
	}

	
	public void setDiceCount(int diceRow, int whiteDice, int yellowDice,
			int valueDice)
	{
		if (diceRow < 0 || diceRow > 5)
		{
			error("setDiceCount ");
		}

		diceTower[diceRow].numMyDice = whiteDice;
		diceTower[diceRow].numWhite = whiteDice;
		diceTower[diceRow].numYellow = yellowDice;
		diceTower[diceRow].value = valueDice;
	}

	public int getDiceCount(int diceRow)
	{
		return getDiceCount(player, diceRow);
	}

	public int getDiceValue(int diceRow)
	{
		return diceTower[diceRow].value; // Independent of player
	}
	
	public boolean isWhiteDiceAvailable(int diceRow)
	{
		if (diceRow < 0 || diceRow > 5)
		{
			error("isWhiteDiceAvailable " + diceRow);
		}

		return diceTower[diceRow].numWhite > 0;
	}
	
	
	public int getDiceCount(int thePlayer, int diceRow)
	{
		if (diceRow < 0 || diceRow > 5)
		{
			error("getDiceCount " + diceRow);
			return 0;
		}

		int num; // yellow dice are added during copy board
		if (thePlayer == player)
		{ num = diceTower[diceRow].numMyDice;}
		else
		{ num = diceTower[diceRow].numWhite;}
		
		if (num == 0)
		{
			return num;
		}

		switch (diceRow)
		{
		case camelRow:
			if (hasBuilding(thePlayer,buildings.extra_camel))
			{
				num++;
			}
			break;

		case 1:
		case 2:
		case 3:
		case 4:
			if (hasBuilding(thePlayer,buildings.extra_cube))
			{
				num++;
			}
			break;

		case goldRow:
			if (hasBuilding(thePlayer,buildings.extra_gold))
			{
				num += 2;
			}
			break;

		default:
			break;
		}
		if (num < 0)
		{
			error("getDiceCount ");
		}

		return num;
	}


	public int getStartPlayer()
	{
		return startPlayer;
	}


	public void setStartPlayer(int startplayer)
	{
		if (startplayer < 0 || startplayer > max_nr_of_players - 1)
		{
			error("setStartPlayer ");
		}
		startPlayer = startplayer;
	}


	public void setNumberOfWeeks(int anzWeeks)
	{
		if (anzWeeks < 1 || anzWeeks > 3)
		{
			error("setNumberOfWeeks ");
		}

		numWeeks = anzWeeks;
	}


	public void setNumberOfDays(int numdays)
	{
		if (numdays < 1 || numdays > 7)
		{
			error("setNumberOfDays ");
		}
		numDays = numdays;
	}


	public void setPlayer(int player)
	{
		if (player < 0 || player > 4)
		{
			error("Set player ");
		}
		this.player = player;
	}


	protected static void error(String message)
	{
		G.print(Http.stackTrace("*** ERROR: YspahanPlayData - " + message));
	}

}
