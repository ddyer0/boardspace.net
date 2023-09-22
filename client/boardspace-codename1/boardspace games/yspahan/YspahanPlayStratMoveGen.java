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

import yspahan.YspahanConstants.yrack;

/**
 * @author Guenther Rosenbaum
 *
 */
public class YspahanPlayStratMoveGen {
	private int player = -1;
	private ArrayList<YspahanMovespec> robotMoveList;
	private YspahanMovespec storedBuildingMove;
	
	private char [] playerIntExt = {'A','B','C','D'};
	private String [] regionIntExt = {"bag", "barrel", "chest", "vase"};
	private yrack [] regionIntExt1 = {yrack.Bag_Neighborhood, yrack.Barrel_Neighborhood, yrack.Chest_Neighborhood,yrack.Vase_Neighborhood};
	private char [][] groupIntExt = { 
			 {'C','D','B','A'},
			 {'A','C','B','D'},
			 {'B','A','C','X'},
			 {'A','B','C','X'}};
	private int [][][] fieldIntExt = { 
			{{0,1,2,9,9,9,9},{0,1,2,3,9,9,9},{1,0,4,3,2,9,9},{0,1,2,3,4,5,9}},
			{{0,1,9,9,9,9,9},{0,1,2,9,9,9,9},{0,1,2,3,9,9,9},{0,1,2,3,4,9,9}},
			{{1,0,9,9,9,9,9},{2,1,0,9,9,9,9},{0,1,2,3,9,9,9},{9,9,9,9,9,9,9}},
			{{0,9,9,9,9,9,9},{0,1,9,9,9,9,9},{0,1,2,9,9,9,9},{9,9,9,9,9,9,9}}};
	

	public YspahanPlayStratMoveGen(int player, ArrayList<YspahanMovespec> robotMoveList)
	{
	this.player = player;
	this.robotMoveList = robotMoveList;
	this.storedBuildingMove = null;
	}

	public int ifGetRegion(YspahanCell cell)
	{
		for (int i = 0; i < regionIntExt1.length; i++)
		{
			if (regionIntExt1[i] == cell.rackLocation) {return i;}
		}
		return -1;
	}
	
	public int ifGetGroup(char c, int region)
	{
		for (int i = 0; i < groupIntExt[region].length; i++)
		{
			if (groupIntExt[region][i] == c) {return i;}
		}
		return -1;
	}
	
	
	/**
	 * Generate a movespec for dropping a cube to a souk.
	 * @param region
	 * @param group
	 * @param field
	 */
	public void ifSetCube(int region, int group, int field) {
		// Example:   Move misc A 3 bag C 1
		if(fieldIntExt[region][group][field]==9)
			{ error("Robot MoveGenError IfSetCube: "+region+group+field);}

		String mv = "Move misc " 
		            + playerIntExt[player]
		            + " 3 "
		            + regionIntExt[region] + " "
		            + groupIntExt[region][group] + " "
		            + fieldIntExt[region][group][field];
		
		robotMoveList.add(new YspahanMovespec(mv,player));	
		trace(mv);
	}
	
	public void ifThrowDice(int yellowDice)
	{
		String mv ="";
		for (int i = 0; i < yellowDice; i++)
		{
			mv = "Move table B "
				+ i + " "
				+ "table A "
				+ (9+i);
			// remove gold is done automatically
			robotMoveList.add(new YspahanMovespec(mv,player));
			trace(mv);
		}
		ifDone();
	}
	
	public void ifDone()
	{
		String mv ="";
		mv = "Done";
		robotMoveList.add(new YspahanMovespec(mv,player));
		trace(mv);
	}

	public void ifDoneMoveEndBuildingPhase()
	{
		if (storedBuildingMove != null)
		{
			robotMoveList.add(storedBuildingMove);
		}
		storedBuildingMove = null;
		String mv ="";
		mv = "Done";
		robotMoveList.add(new YspahanMovespec(mv,player));
		trace(mv);
	}

	public void ifCamelPay(boolean sendToCaravan)
	{
		String mv ="";
		mv = "Done";
		robotMoveList.add(0,new YspahanMovespec(mv,player));
		trace(mv + "(offer first)");
		if (sendToCaravan) {return;}
		
		mv = "Move misc "
			+ playerIntExt[player]
			+ " 0 camels @ -1";
		robotMoveList.add(0,new YspahanMovespec(mv,player));
		trace(mv + "(offer first)");
	}

	
	private int getFirstIndexAfterCards()
	{
		int ins = -1;
		for (int i = robotMoveList.size()-1; i >= 0; i--)
		{
			if (robotMoveList.get(i).isDone())  //used cards before main action
			{
				ins = i;
				break;
			}
		}
		return ins+1;
	}

	public void ifDeleteGoldAddSup(int gold, int diceRow)
	{
		trace("pay gold for add supervisor steps");

		for (int i = 0; i < gold; i++)
		{
			String mv = "Move misc " + playerIntExt[player] + " 1 "
					+ " dice B " + diceRow;
			trace(mv + "(offer first, but after used cards)");
			robotMoveList.add(getFirstIndexAfterCards(), new YspahanMovespec(mv, player));
		}		
	}

	
	public void ifDeleteCardAddDie(int card, int diceRow)
	{
		trace("delete card for add dice");

		String mv = "Move misc " 
	            + playerIntExt[player]
	            + " 2 "
	            + card
	            + " dice C "
	            + diceRow;
	
		trace(mv + "(offer first, but after used cards)");
		
	    robotMoveList.add(getFirstIndexAfterCards(),new YspahanMovespec(mv,player));		
	}
	

	public void ifModGold(int diffGold)
	{//
		String mv = "Move gold @ -1"
				  + " misc "
				  + playerIntExt[player] 
				  + " 1";						  
		trace(mv);
	    robotMoveList.add(new YspahanMovespec(mv,player));
	}


	public void ifModCamels(int diffCamels)
	{//
			String mv = "Move camels @ -1 "
					+ "misc "
					+ playerIntExt[player]
					+ " 0" ;
			trace(mv);
			robotMoveList.add(new YspahanMovespec(mv, player));
	}

	

	public void ifSelectDiceRow(int dice)
	{
		// this move will be set at the begin of the move list
		String mv = "Pick dice A "
				  + dice;
						  
		trace(mv + "(selected dice row = " + dice + ")(offer first, but after used cards)");
		
	    robotMoveList.add(getFirstIndexAfterCards(),new YspahanMovespec(mv,player));		
	}

	
	public void ifDrawCard() {
		String mv = "Move cards @ -1 misc "
					+ playerIntExt[player]
					+ " " + 2;
		trace(mv);
		robotMoveList.add(new YspahanMovespec(mv, player));
//		IfDone();
	}



	public void ifConstructBuilding(int building) {
		// building 1..6 !
		String mv = "Move misc "
					+ playerIntExt[player]
					+ " 3"
					+ " buildings "
					+ playerIntExt[player]
					+ " " + (building-1);
	trace("Building move stored");
	storedBuildingMove = new YspahanMovespec(mv, player);
	}

	
	public void ifDiscardCard(int card) {
		String mv = "Move misc " 
	            + playerIntExt[player]
	            + " 2 "
	            + card
	            + " discards @ -1";
	
		trace(mv);
		robotMoveList.add(new YspahanMovespec(mv, player));
	}

	
	public void ifTradeResource(int from, int to)
	{
		String mv;
		if (to >= 0) //not pool
		{
		 mv = "Move misc " 
	            + playerIntExt[player] + " "
	            + from
	            + " misc "
	            + playerIntExt[player] + " "
	            + to;
		}
		else // to pool
		{
			 String pool = (from == 0 ? " camels " : " gold ");
			 mv = "Move misc " 
			            + playerIntExt[player] + " "
			            + from
			            + pool
			            + "@ -1";			
		}
	
		trace(mv);
		robotMoveList.add(new YspahanMovespec(mv, player));
	
		
	}

	
	public char ifGetPlayerExt(int player)
	{
		return playerIntExt[player];
	}
	
	private char getSupCol(int street)
	{
		return (street==0 || street==2 ? '@' : 'v');
	}
	
	private int getSupRow(int street,int pos)
	{
		if(street==0) return 10+pos;
		if(street==1) return 22-pos;
		if(street==2) return 10-pos;
		if(street==3) return 22+pos;
		return 10;
	}

	public void ifMoveSupervisor(int fromStreet, int fromPos, int toStreet, int toPos)
	{
		String mv = "Move Supervisor " 
	            + getSupCol(fromStreet) + " "
	            + getSupRow(fromStreet, fromPos)
	            + " Supervisor "
	            + getSupCol(toStreet) + " "
	            + getSupRow(toStreet, toPos);
	
		trace(mv);
		robotMoveList.add(new YspahanMovespec(mv, player));
		
	}

	
	private void error(String message)
	{
	//	G.Error(message;)
		System.out.println("*** ERROR *** " + message);
	}

	
	protected void trace(String message)
	{
	//	if (myDebug)
	//	{
	//		G.print(Http.stackTrace("*** TRACE IF: " + message));
	//	}
	}

	public void ifDesignateCube(int region, int group, int field)
	{
		String mv ="";
		mv = "Pick "
	            + regionIntExt[region] + " "
	            + groupIntExt[region][group] + " "
	            + fieldIntExt[region][group][field];
		robotMoveList.add(0,new YspahanMovespec(mv,player));
		trace(mv + "(offer first - designate cube)");
	}




}
