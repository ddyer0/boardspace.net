/**
 * 
 */
package yspahan;

import java.util.ArrayList;
import java.util.Arrays;

import lib.G;


/**
 * @author G�nther Rosenbaum
 * 
 */
public class YspahanPlayStratDumbot extends YspahanPlayStratCore implements
		IYspahanPlayStrat
{

	// {****************************************}
	// Strategy Dumbot: just a mix of Kruener Basar and Karwan Baschi V1
	// For 3/4 players
	// Build all buildings in partly random requence
	// 
	//
	// {****************************************}
	//

	private static final int[] nrOfSteps = { 8, 8, 10, 10, 10, 10, 6 };

	// 1/B 6/B We Cz Cs Gmax Kmax C=Tau Ks Gs}
	// {cz=
	// 1 karte nehmen
	// 10 Supervisor/Supervisornachschub/Karte
	// 100 Supervisor}

	private static final int[][][] stratSteps = {
		{ { 0, 0, 24, 0, 0, 100, 100, -1, 2, 0 },
		{ 0, 0, 16, 0, 1, 100, 100, -1, 2, 0 },
		{ 4, 0, 0, 0, 0, 100, 6, -1, 2, 0 },
		{ 0, 6, 0, 0, 0, 6, 100, -1, 2, 0 },
		{ 0, 0, 15, 0, 0, 100, 100, -1, 2, 0 },
		{ 2, 0, 0, 0, 0, 100, 4, -1, 2, 0 },
		{ 101, 0, 0, 0, 1, 100, 4, -1, 2, 0 },
		{ 0, 0, 0, 1, 0, 100, 100, -1, 2, 0 },
		{ 0, 0, 0, 0, 0, 100, 100, -1, 2, 0 },
		{ 0, 0, 0, 0, 0, 100, 100, -1, 2, 0 } },

		{ { 0, 0, 24, 0, 0, 100, 100, -1, 2, 2 },
		{ 0, 0, 16, 0, 1, 100, 100, -1, 2, 2 },
		{ 4, 0, 0, 0, 0, 100, 6, -1, 2, 2 },
		{ 0, 4, 0, 0, 0, 6, 100, 1, 2, 2 },
		{ 0, 0, 15, 0, 0, 100, 100, -1, 2, 2 },
		{ 2, 0, 0, 0, 0, 100, 100, -1, 2, 2 },
		{ 101, 0, 0, 0, 1, 100, 100, -1, 2, 2 },
		{ 0, 0, 0, 1, 0, 100, 100, -1, 2, 2 },
		{ 0, 0, 0, 0, 0, 100, 100, -1, 2, 2 },
		{ 0, 0, 0, 0, 0, 100, 100, -1, 2, 2 } },

		{ { 0, 0, 16, 0, 1, 100, 100, -1, 4, 4 },
		{ 4, 0, 0, 0, 0, 100, 8, -1, 4, 4 },
		{ 0, 6, 0, 0, 0, 8, 100, -1, 4, 4 },
		{ 0, 0, 15, 0, 0, 100, 100, -1, 4, 4 },
		{ 103, 0, 0, 0, 1, 100, 8, -1, 4, 4 },
		{ 2, 0, 0, 0, 0, 100, 4, -1, 4, 4 },
		{ 0, 4, 0, 0, 0, 8, 100, -1, 4, 4 },
		{ 2, 0, 0, 0, 0, 100, 8, -1, 4, 4 },
		{ 0, 4, 0, 0, 0, 100, 100, 1, 4, 4 },
		{ 0, 0, 0, 1, 0, 100, 100, -1, 4, 4 } },

		{ { 0, 0, 16, 0, 1, 100, 100, -1, 4, 4 },
		{ 4, 0, 0, 0, 0, 100, 8, -1, 4, 4 },
		{ 0, 6, 0, 0, 0, 8, 100, -1, 4, 4 },
		{ 0, 0, 16, 0, 1, 100, 100, -1, 4, 4 },
		{ 0, 0, 11, 0, 0, 100, 100, -1, 4, 4 },
		{ 103, 0, 0, 0, 1, 100, 8, -1, 4, 4 },
		{ 2, 0, 0, 0, 0, 100, 8, -1, 4, 4 },
		{ 0, 4, 0, 0, 0, 8, 100, -1, 4, 4 },
		{ 0, 4, 0, 0, 0, 100, 100, 1, 4, 4 },
		{ 0, 0, 0, 1, 0, 100, 100, -1, 4, 4 } },

		{ { 0, 0, 16, 0, 1, 100, 100, -1, 5, 5 },
		{ 5, 0, 0, 0, 0, 100, 6, -1, 5, 5 },
		{ 104, 0, 0, 0, 1, 100, 6, -1, 5, 5 },
		{ 0, 5, 0, 0, 0, 5, 100, -1, 5, 5 },
		{ 0, 0, 12, 0, 1, 100, 100, -1, 5, 5 },
		{ 0, 0, 11, 0, 0, 100, 100, -1, 5, 5 },
		{ 2, 0, 0, 0, 0, 100, 3, -1, 5, 5 },
		{ 0, 4, 0, 0, 0, 100, 100, 1, 5, 5 },
		{ 0, 0, 8, 0, 0, 100, 100, -1, 5, 5 },
		{ 0, 0, 0, 1, 0, 100, 100, -1, 5, 5 } },
		
		{{ 0 , 0 ,16 , 0 , 1 , 100 , 100 , -1 , 4 , 4},
		{ 4 , 0 , 0 , 0 , 0 , 100 , 4   , -1 , 4 , 4},
		{ 0 , 6 , 0 , 0 , 0 , 100 , 4   , -1 , 4 , 4},
		{ 0 , 0 ,15 , 0 , 0 , 100 , 100 , -1 , 4 , 4},
		{103, 0 , 0 , 0 , 1 , 100 ,   4 , -1 , 4 , 4},
		{ 2 , 0 , 0 , 0 , 0 , 100 ,   4 , -1 , 4 , 4},
		{ 0 , 4 , 0 , 0 , 0 ,   4 , 100 , -1 , 4 , 4},
		{ 0 , 0 , 0 ,100, 0 , 100 , 100 , -1 , 4 , 4},
		{ 0 , 4 , 0 , 0 , 0 , 100 , 100 ,  1 , 4 , 4},
		{ 0 , 0 , 0 , 10, 0 , 100 , 100 , -1 , 4 , 4}},
		
		{{ 0 , 0 , 16, 0 , 1 , 100 , 100 , -1 , 0 , 0},
		{ 3 , 0 , 0 , 0 , 0 , 100 , 1   , -1 , 0 , 0},
		{ 0 , 5 , 0 , 0 , 0 , 3   , 100 , -1 , 0 , 0},
		{ 0 , 0 , 0 ,100, 0 , 100 , 100 , -1 , 0 , 0},
		{ 0 , 0 , 6 , 0 , 1 , 100 , 100 , -1 , 0 , 0},
		{ 0 , 0 , 0 , 10, 0 , 100 , 100 , -1 , 0 , 0},
		{ 0 , 0 , 0 , 0 , 0 , 100 , 100 , -1 , 0 , 0},
		{ 0 , 0 , 0 , 0 , 0 , 100 , 100 , -1 , 0 , 0},
		{ 0 , 0 , 0 , 0 , 0 , 100 , 100 , -1 , 0 , 0},
		{ 0 , 0 , 0 , 0 , 0 , 100 , 100 , -1 , 0 , 0}}
	};

	@SuppressWarnings("unused")
	private boolean supAttack = true;


	public YspahanPlayStratDumbot(int player, int numPlayers, long randomKey)
	{
		super(player, numPlayers,randomKey, stratSteps, nrOfSteps);
		int sel = myRandom(4);
		
		switch (sel)
		{
		case 0:		
			prio3Building = 6;
			prio6Building = 5;			
			break;

		case 1:	
			prio3Building = 5;
			prio6Building = 6;			
			break;

		case 2:	
			prio3Building = 6;
			prio6Building = 5;						
			break;
		
		case 3:
		default:	
			prio3Building = 5;
			prio6Building = 6;			
			break;

		}
		prio1Building = 1;				
		prio2Building = 2;
		prio4Building = 3;
		prio5Building = 4;

		Arrays.fill(rValue, 0);
		setPrioBuilding(); // must always called;
		if (myDebug) {System.out.println(  Arrays.toString(prioBuilding));}
	}


	private void mainAction()
	{
		// Start Strategy 8 main actions !

		boolean res = false;
		int stage = -1;
		int cd = numDice(0);
		int gd = numDice(5);

//		check, whether supervisor can reduce opponents scoring!
		//not for Dumbot
//		if ((data.getActDaysInWeek() >= 7) && (realMaxValue() < 8) && supAttack)
//		{
//			res = supervisorAttackMax();
//			if (res)
//			{
//				return;
//			}
//			; // ===========> EXIT with Supervisor
//		}
//		
//		 // actions against "double supervisor"
//		 res = specialSupervisorDefense();
//		 if (res) { return; }; // ===========> EXIT with CUBE

		 
		// only for 2 players not implemented yet
//		 WR := GetWuerfelReiheGegenSp1(Spieler);
//		 If (SpielData.AnzahlSpieler = 2) AND
//		 (Spieldata.Steuerinfo.ZuegeProRunde > 1)
//		 AND (MaxW < WGebBau) AND (WR > 0)
//		 Then
//		 Begin
//		 Res := false;
//		 Res :=
//		 A_BauePrioGebaeude(Spieler,Prio1Geb,Prio2Geb,Prio3Geb,Prio4Geb,Prio5Geb,Prio6Geb,True
//		 );
//		 If Res
//		 then
//		 Begin
//		 DeleteWuerfelZeile(WR);
//		 EXIT;
//		 End;
//		 end;
		//
		//
		stage = 0;
		if (!data.hasBuilding(buildings.extra_camel))
		{
			res = performAction(stage, rValue);
			if (!res)
			{
				throw G.Error("Strat12, Stage %d: No legal move constructed", stage);
			}
			// A_KarteNutzen_Kamel();
			return;
		}

		stage = 1;
		if (!data.hasBuilding(buildings.extra_gold))
		{
			res = performAction(stage, rValue);
			if (!res)
			{
				throw G.Error("Strat12, Stage %d: No legal move constructed", stage);
			}
			// A_KarteNutzen_Kamel();
			return;
		}

		stage = 2;
		if (!data.hasBuilding(buildings.extra_card))
		{
			a_UseCard_Gold();
			a_UseCard_Camel();
			res = performAction(stage, rValue);
			if (!res)
			{
				throw G.Error("Strat12, Stage %d: No legal move constructed", stage);
			}
			// A_KarteNutzen_Gold();
			// A_KarteNutzen_Kamel();
			return;
		}

		stage = 3;
		if (!data.hasBuilding(buildings.extra_movement))
		{
			a_UseCard_Gold();
			a_UseCard_Camel();
			res = performAction(stage, rValue);
			if (!res)
			{
				throw G.Error("Strat12, Stage %d: No legal move constructed", stage);
			}
			return;
		}

		stage = 4;
		if (!data.hasBuilding(buildings.extra_cube))
		{
			a_UseCard_Gold();
			a_UseCard_Camel();
			res = performAction(stage, rValue);
			if (!res)
			{
				throw G.Error("Strat12, Stage %d: No legal move constructed", stage);
			}
			return;
		}

		stage = 5;
		if (!data.hasBuilding(buildings.extra_points))
		{
			a_UseCard_Gold();
			a_UseCard_Camel();

			if (data.getWeek() >= 3 && data.getActDaysInWeek() >= 7) // last
																		// turn
																		// !!
			{
				int card = getCardAddDiceS8(true, true);

				if (data.getGold() >= 4 && data.getCamels() < 4
						&& (data.getCamels() + cd >= 4))
				{
					a_Take_Camels_Ext(-1);
					return; // =========> EXIT
				}
				if (data.getGold() >= 4 && data.getCamels() < 3
						&& (data.getCamels() + cd + 1 >= 4) && card > 0)
				{
					a_Take_Camels_Ext(card);
					return; // =========> EXIT
				}
				if (data.getCamels() >= 4 && data.getGold() < 4
						&& (data.getGold() + gd >= 4))
				{
					a_Take_Gold_Ext(-1);
					return; // =========> EXIT
				}
				if (data.getCamels() >= 4 && data.getGold() < 3
						&& (data.getGold() + gd + 1 >= 4) && card > 0)
				{
					a_Take_Gold_Ext(card);
					return; // =========> EXIT
				}
			}

			res = performAction(stage, rValue);
			if (!res)
			{
				throw G.Error("Strat12, Stage %d: No legal move constructed", stage);
			}
			return;
		}

		// All buildings are constructed!

		stage = 6;
		if (data.getGold() < 2)
		{
			a_UseCard_Gold();
		}
		;
		if (data.getCamels() < 1)
		{
			a_UseCard_Camel();
		}
		;
		res = performAction(stage, rValue);
		if (!res)
		{
			throw G.Error("Strat12, Stage %d: No legal move constructed", stage);
		}
		return;

	}


	public void cardsAfterMoveMainAction()
	{
		// Start Strategy 12 main actions !

		@SuppressWarnings("unused")
		int stage;
		stage = 0;
		if (!data.hasBuilding(buildings.extra_camel))
		{
			a_UseCard_Camel();
			return;
		}

		stage = 1;
		if (!data.hasBuilding(buildings.extra_gold))
		{
			a_UseCard_Gold();
			a_UseCard_Camel();
			return;
		}

		stage = 2;
		if (!data.hasBuilding(buildings.extra_cube))
		{
			a_UseCard_Gold();
			a_UseCard_Camel();
			return;
		}

		stage = 3;
		if (!data.hasBuilding(buildings.extra_points))
		{
			return;
		}

		stage = 4;
		if (!data.hasBuilding(buildings.extra_movement))
		{
			return;
		}

		stage = 5;
		if (!data.hasBuilding(buildings.extra_card))
		{
			return;
		}

		// All buildings are constructed!

		stage = 6;
		return;

	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see yspahan.IYspahanPlayStrat#ThrowDices()
	 */
	public void throwDices()
	{
		// assume that we are start player!
		int yellowDices = 0;

		if (data.hasBuilding(prioBuilding[3]))
		{
				yellowDices = getYellowDice(1);
		}
		
		if (data.getGold() > 4)
		{
				yellowDices = getYellowDice(Math.min(data.getGold() - 4 , 3));
		}
		
		
		if (data.getMissingBuilding(player)==null)
		{
				yellowDices = getYellowDice(3);
		}
				

		throwDice(yellowDices);
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see yspahan.IYspahanPlayStrat#DoMoveMain()
	 */
	public void doMoveMain()
	{
		// dices are thrown; do normal action, use cards
		rValue[0] = 0;
		rValue[5] = 0;
		for (int i = 1; i < 5; i++)
		{
			if (!data.isRegionUsable(i - 1) || numDice(i) == 0)
			{
				rValue[i] = 0;
			}
			else
			{
				rValue[i] = regionGroupValue[i - 1][realDiceCount(i) - 1];
			}
		}

		// *********************************
		// *********************************
		mainAction();
		// *********************************
		// *********************************

//		moveGenerator.ifDone(); // end of main action !!
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see yspahan.IYspahanPlayStrat#DoMoveBuilding()
	 */
	public void doMoveBuilding()
	{
		// dices are thrown; normal action is done: use cards, construct
		// building
		rValue[0] = 0;
		rValue[5] = 0;
		for (int i = 1; i < 5; i++)
		{
			if (!data.isRegionUsable(i - 1) || numDice(i) == 0)
			{
				rValue[i] = 0;
			}
			else
			{
				rValue[i] = regionGroupValue[i - 1][realDiceCount(i) - 1];
			}
		}

		// use some cards
		if (data.getActDaysInWeek() <= 3
				&& data.hasBuilding(buildings.extra_cube))
		{
			a_UseCard_ShopCubeG(3, 2);
			a_UseCard_ShopCubeG(2, 2);
			a_UseCard_ShopCubeG(1, 3);
			a_UseCard_ShopCubeG(0, 2);
		}
		else
		{
			a_UseCard_ShopCubeOneMissing();
			a_UseCard_ShopCubeG(3, 0);
		}

		// {**************************************************************}
		// {******************** End Of Week ****************************}
		// {**************************************************************}

		if (data.getActDaysInWeek() == 7)
		{
			// use special cards
			for (int i = 0; i < 2; i++)
			{
				a_useCard_Caravan();
				a_UseCard_ShopCubeOneMissing();
			}
		}

		// {**************************************************************}
		// {****************** Construct Building ************************}
		// {**************************************************************}
		//
		// We assume, that number of players in game are > 2 (otherwise Building
		// is a main action)

		@SuppressWarnings("unused")
		boolean res = a_BuildPrioBuilding(true);

		// {**************************************************************}
		// {****************** End Of Game *******************************}
		// {**************************************************************}

		if (data.getWeek() == 3 && data.getActDaysInWeek() == 7)
		{
			a_UseCard_MAX_VPointsCamelsGold();
		}

		// {**************************************************************}
		// {****************** End Of Turn *******************************}
		// {**************************************************************}
		// rules not allow cards after building phase; move the building
		// activity to the end of the move!

		moveGenerator.ifDoneMoveEndBuildingPhase(); // end of complete turn!

	} // end do move


	/*
	 * (non-Javadoc)
	 * 
	 * @see yspahan.IYspahanPlayStrat#SupervisorSendCubeToCaravan(int, int)
	 */
	public boolean supervisorSendCubeToCaravan(YspahanCell cell)
	{
		if (data.getCamels() <= 0)
		{
			moveGenerator.ifCamelPay(true);
			return true;
		}
		else
		{
			moveGenerator.ifCamelPay(false);
			data.modCamels(-1);
			return false;
		}
			

//		if ((data.soukGroupComplete(region, group) 
//				&& data.soukGroupVictoryPoints(region, group) >= 4)
//				|| (data.soukGroupVictoryPoints(region, group) == 12 && data.getActDaysInWeek() < 7)
//				|| (data.soukGroupComplete(region, group) && data.getActDaysInWeek() >= 6)
//				|| (data.getCamels() > 4)
//				|| (data.countBuildings(player) >= 6)
//			)
//		{
//			moveGenerator.ifCamelPay(false);
//			return false;
//		}
//		;
//
//		moveGenerator.ifCamelPay(true);
//		return true;

	}


	/**
	 * Return movelist of generated move-vector.
	 */
	public ArrayList<YspahanMovespec> getMoveList()
	{
		return robotMoveList;
	}


	@Override
	public String toString()
	{
		return "Strategy Dumbot for boardspace; player " + player + " of "
				+ numPlayers;
	}


	public void supervisorDesignateCube()
	{
		supervisorDesignateCubeStd(); //from core
	}

}