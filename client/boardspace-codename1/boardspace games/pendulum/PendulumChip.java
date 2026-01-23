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
package pendulum;

import lib.AR;
import lib.CellId;
import lib.DrawableImageStack;
import lib.G;
import lib.Graphics;
import lib.HitPoint;
import lib.Image;
import lib.ImageLoader;
import lib.ImageStack;
import lib.OStack;
import lib.Random;
import lib.exCanvas;
import online.game.chip;
import common.CommonConfig;
class ChipStack extends OStack<PendulumChip>
{
	public PendulumChip[] newComponentArray(int n) { return(new PendulumChip[n]); }
}

/**
 * note that the "idString" field is derived from the order that these chips are
 * created.  ANY change in the number or order will invalidate all existing game
 * records.  Add new items only at the end of the file, and remove items at your
 * peril.
 *  
 * @author ddyer
 *
 */
public class PendulumChip extends chip<PendulumChip> implements CommonConfig,PendulumConstants
{

	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack otherChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public PColor color;
	public PendulumId id;
	public PendulumChip back = null;
	private int chipNumber = 0;
	public boolean councilCardToHand = false;
	private String idString = null;
	public PC pc = PC.None;		// cost for strategy cards
	public PB pb[] = null;		// province benefits for province cards
	public String contentsString() { return(color==null ? file : color.name()); }
	public int resources[] = null;	// initial resources for player boards
	public int vps[] = null;		// initial vps for player boards
	// constructor for the chips on the board, which are the only things that are digestable.

	private PendulumChip(String na,double[]sc,PColor con)
	{	
		scale=sc;
		file = na;
		color = con;
		randomv = r.nextLong();
		chipNumber = otherChips.size();
		idString = "c"+otherChips.size();	// this is used in PendulumMovespec
		otherChips.push(this)

		;
	}
	private PendulumChip(String na,double[]sc,PendulumId pid,PColor con)
	{
		this(na,sc,con);
		id = pid;
	}
	
	private PendulumChip(String na,PColor con,int[]v,PB[]bene,int... res)
	{
		this(na,noscale,con);
		resources = res;
		vps = v;
		pb = bene;
	}
	private PendulumChip(String na,PColor con,int[]v,PC co,PB[]bene,int... res)
	{
		this(na,noscale,con);
		resources = res;
		vps = v;
		pb = bene;
	}
	private PendulumChip(String na,double[]sc,PColor con,PendulumChip ba)
	{
		this(na,sc,con);
		back = ba;
	}
	private PendulumChip(String na,double[]sc,PendulumId con)
	{
		this(na,sc,(PColor)null);
		id = con;
	}
	private PendulumChip(String na,double[]sc,PendulumId con,PendulumChip ba,PB...benefits)
	{
		this(na,sc,con);
		back = ba;
		pb = benefits;
	}
	private PendulumChip(String na,double[]sc,PendulumId con,PendulumChip ba,boolean toHand,PB...benefits)
	{	this(na,sc,con,ba,benefits);
		councilCardToHand = toHand;
	}
	private PendulumChip(String na,double[]sc,PendulumId con,PendulumChip ba,PC cost,PB ...benefits)
	{
		this(na,sc,con);
		back = ba;
		pb = benefits;
		pc = cost;
	}
	private PendulumChip(String na,double[]sc,PendulumId con,PendulumChip ba,boolean toHand,PC cost,PB ...benefits)
	{
		this(na,sc,con,ba,cost,benefits);
		councilCardToHand = true;
		
	}
	// constructor for all the other random artwork.
	private PendulumChip(String na,double[]sc)
	{	this(na,sc,(PColor)null);
		otherChips.push(this);
	}
	static final double noscale[] = {0.5,0.5,1.0};
	
	public int chipNumber() { return(chipNumber); }
	
	public static PendulumChip Board3 = new PendulumChip("board_r12-2",null);
	public static PendulumChip Board5 = new PendulumChip("board_r12-1",null);
	public static PendulumChip councilBoard = new PendulumChip("council-board",null);
	private static PendulumChip boards[] = { Board3,Board5};
	
	public static PendulumChip timerTrack = new PendulumChip("timer",noscale,PendulumId.TimerTrack);
	private static double smallMeepleScale[] = {0.5,0.5,0.8};
	static public PendulumChip chips[] = {		
			new PendulumChip("bits/yellow-meeple",smallMeepleScale,PendulumId.RegularWorker,PColor.Yellow),
			new PendulumChip("bits/white-meeple",smallMeepleScale,PendulumId.RegularWorker,PColor.White),
			new PendulumChip("bits/green-meeple",smallMeepleScale,PendulumId.RegularWorker,PColor.Green),
			new PendulumChip("bits/blue-meeple",smallMeepleScale,PendulumId.RegularWorker,PColor.Blue),
			new PendulumChip("bits/red-meeple",noscale,PendulumId.RegularWorker,PColor.Red),
	};

	public boolean isGrande()
	{
		return AR.indexOf(bigchips,this)>=0;
	}
	static public PendulumChip bigchips[] = {
			new PendulumChip("bits/yellow-grande",noscale,PendulumId.GrandeWorker,PColor.Yellow),
			new PendulumChip("bits/white-grande",noscale,PendulumId.GrandeWorker,PColor.White),
			new PendulumChip("bits/green-grande",noscale,PendulumId.GrandeWorker,PColor.Green),
			new PendulumChip("bits/blue-grande",noscale,PendulumId.GrandeWorker,PColor.Blue),
			new PendulumChip("bits/red-grande",noscale,PendulumId.GrandeWorker,PColor.Red),
	};
	
	static public PendulumChip hexes[] = {		
			new PendulumChip("bits/yellow-hexagon",new double[] {0.5,0.5,0.8},PColor.Yellow),
			new PendulumChip("bits/white-hexagon",new double[] {0.5,0.5,0.75},PColor.White),
			new PendulumChip("bits/green-hexagon",new double[] {0.5,0.5,0.8},PColor.Green),
			new PendulumChip("bits/blue-hexagon",new double[] {0.5,0.5,0.85},PColor.Blue),
			new PendulumChip("bits/red-hexagon",noscale,PColor.Red),
	};

	
	static public PendulumChip cylinders[] = {
			new PendulumChip("bits/yellow-cylinder",new double[] {0.5,0.5,0.9},PColor.Yellow),
			new PendulumChip("bits/white-cylinder",new double[] {0.5,0.5,0.9},PColor.White),
			new PendulumChip("bits/green-cylinder",new double[] {0.55,0.5,0.9},PColor.Green),
			new PendulumChip("bits/blue-cylinder",new double[] {0.5,0.5,0.65},PColor.Blue),
			new PendulumChip("bits/red-cylinder",noscale,PColor.Red),
	};

	static public PendulumChip mats[] = {
			new PendulumChip("playermats/bolk_r5-1-nomask",PColor.Yellow,
					new int[]{0,3,6},
					new PB[] {PB.Pow1, PB.M4, PB.C5, PB.D2 },
					2,0,4,0),
			new PendulumChip("playermats/dhrenkir_r5-1-nomask",PColor.White,new int[]{4,4,1},
					new PB[] { PB.Pow1, PB.M4, PB.C5, PB.D2 },
					0,0,4,1),
			new PendulumChip("playermats/gambal_r6-1-nomask",PColor.Green,
					new int[]{3,6,0},
					new PB[] { PB.Pow1, PB.M4, PB.C5, PB.D2 },
					0,0,6,0),
			new PendulumChip("playermats/licinia_r7-1-nomask",PColor.Blue,
					new int[]{3,3,3},
					new PB[] { PB.Pow1, PB.M4, PB.C5, PB.D2 },
					1,1,4,0),
			new PendulumChip("playermats/mesoat_r3-1-nomask",PColor.Red,
					new int[]{6,0,3},
					new PB[] { PB.Pow1, PB.M4, PB.C5, PB.D2},
					0,2,4,0),
	};
	static public PendulumChip advancedmats[] = {
			new PendulumChip("playermats/bolk_r5-2-nomask",PColor.Yellow,
					new int[]{0,7,2},
					new PB[] { PB.Pow1, PB.M4, PB.M3D2, PB.D2 },
					4,0,2,0),
			new PendulumChip("playermats/dhrenkir_r5-2-nomask",PColor.White,
					new int[]{4,1,3},
					new PB[] { PB.V3, PB.M4, PB.C5, PB.D2 },
					0,0,2,2),
			// TODO: special case for player mat gambal_r6-2 retrieve benefit
			new PendulumChip("playermats/gambal_r6-2-nomask",PColor.Green,
					new int[]{2,5,2},
					new PB[] { PB.Pow1, PB.Retrieve, PB.C5, PB.D2},
					2,0,3,0),
			new PendulumChip("playermats/licinia_r7-2-nomask",PColor.Blue,
					new int[]{2,2,3},
					new PB[] {PB.Pow1, PB.R4, PB.R5, PB.D2 },
					1,1,2,0),
			new PendulumChip("playermats/mesoat_r3-2-nomask",PColor.Red,
					new int[]{7,0,0},
					new PB[] { PB.C2, PB.Province, PB.C5, PB.D2},
					0,0,2,0),
	};
	
	static public PendulumChip Cardback = new PendulumChip("provinces/cards_r8-back",noscale,PendulumId.ProvinceCard);
	static public PendulumChip provinceCards[] = {
		new PendulumChip("provinces/cards_r8-1",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-2",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-3",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-4",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-5",noscale,PendulumId.ProvinceCard,Cardback,PB.M1,PB.M2,PB.Pow1,PB.M3),
		new PendulumChip("provinces/cards_r8-6",noscale,PendulumId.ProvinceCard,Cardback,PB.C1,PB.C2,PB.Pres1,PB.C3),
		new PendulumChip("provinces/cards_r8-7",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.D2,PB.Pop1,PB.D3),
		new PendulumChip("provinces/cards_r8-8",noscale,PendulumId.ProvinceCard,Cardback,PB.V1,PB.V2,PB.Pow1,PB.V3),
		new PendulumChip("provinces/cards_r8-9",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-10",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-11",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-12",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-13",noscale,PendulumId.ProvinceCard,Cardback,PB.M1,PB.M2,PB.Pow1,PB.M3),
		new PendulumChip("provinces/cards_r8-14",noscale,PendulumId.ProvinceCard,Cardback,PB.C1,PB.C2,PB.Pres1,PB.C3),
		new PendulumChip("provinces/cards_r8-15",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.D2,PB.Pop1,PB.D3),
		new PendulumChip("provinces/cards_r8-16",noscale,PendulumId.ProvinceCard,Cardback,PB.V1,PB.V2,PB.Pow1,PB.V3),
		new PendulumChip("provinces/cards_r8-17",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-18",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-19",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-20",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-21",noscale,PendulumId.ProvinceCard,Cardback,PB.M1,PB.M2,PB.Pow1,PB.M3),
		new PendulumChip("provinces/cards_r8-22",noscale,PendulumId.ProvinceCard,Cardback,PB.C1,PB.C2,PB.Pres1,PB.C3),
		new PendulumChip("provinces/cards_r8-23",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.D2,PB.Pop1,PB.D3),
		new PendulumChip("provinces/cards_r8-24",noscale,PendulumId.ProvinceCard,Cardback,PB.V1,PB.V2,PB.Pow1,PB.V3),
		new PendulumChip("provinces/cards_r8-25",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-26",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-27",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-28",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-29",noscale,PendulumId.ProvinceCard,Cardback,PB.M1,PB.M2,PB.Pow1,PB.M3),
		new PendulumChip("provinces/cards_r8-30",noscale,PendulumId.ProvinceCard,Cardback,PB.C1,PB.C2,PB.Pres1,PB.C3),
		new PendulumChip("provinces/cards_r8-31",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.D2,PB.Pop1,PB.D3),
		new PendulumChip("provinces/cards_r8-32",noscale,PendulumId.ProvinceCard,Cardback,PB.V1,PB.V2,PB.Pow1,PB.V3),
		new PendulumChip("provinces/cards_r8-33",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-34",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-35",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-36",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-37",noscale,PendulumId.ProvinceCard,Cardback,PB.M1,PB.M2,PB.Pow1,PB.M3),
		new PendulumChip("provinces/cards_r8-38",noscale,PendulumId.ProvinceCard,Cardback,PB.C1,PB.C2,PB.Pres1,PB.C3),
		new PendulumChip("provinces/cards_r8-39",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.D2,PB.Pop1,PB.D3),
		new PendulumChip("provinces/cards_r8-40",noscale,PendulumId.ProvinceCard,Cardback,PB.V1,PB.V2,PB.Pow1,PB.V3),
		new PendulumChip("provinces/cards_r8-41",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-42",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-43",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-44",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-45",noscale,PendulumId.ProvinceCard,Cardback,PB.M1,PB.Pow1,PB.Pow1,PB.Pow1),
		new PendulumChip("provinces/cards_r8-46",noscale,PendulumId.ProvinceCard,Cardback,PB.C1,PB.Pres1,PB.Pres1,PB.Pres1),
		new PendulumChip("provinces/cards_r8-47",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.Pop1,PB.Pop1,PB.Pop1),
		new PendulumChip("provinces/cards_r8-48",noscale,PendulumId.ProvinceCard,Cardback,PB.V1,PB.V2,PB.Pow1,PB.V3),
		new PendulumChip("provinces/cards_r8-49",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-50",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-51",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-52",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.M2,PB.Pow1,PB.C3),
		new PendulumChip("provinces/cards_r8-53",noscale,PendulumId.ProvinceCard,Cardback,PB.M1,PB.Pow1,PB.Pow1,PB.Pow1),
		new PendulumChip("provinces/cards_r8-54",noscale,PendulumId.ProvinceCard,Cardback,PB.C1,PB.Pres1,PB.Pres1,PB.Pres1),
		new PendulumChip("provinces/cards_r8-55",noscale,PendulumId.ProvinceCard,Cardback,PB.D1,PB.Pop1,PB.Pop1,PB.Pop1),
		new PendulumChip("provinces/cards_r8-56",noscale,PendulumId.ProvinceCard,Cardback,PB.V1,PB.V2,PB.Pow1,PB.V3),
		Cardback
	};
	
	static PendulumChip stratBacks[] = {
			new PendulumChip("stratcards/bolkchamp-5",noscale,PendulumId.PlayerStratCard),
			new PendulumChip("stratcards/bolkwar-5",noscale,PendulumId.PlayerStratCard),
			new PendulumChip("stratcards/dhkinsid-5",noscale,PendulumId.PlayerStratCard),
			new PendulumChip("stratcards/dhkty-5",noscale,PendulumId.PlayerStratCard),
			new PendulumChip("stratcards/gambbriber-5",noscale,PendulumId.PlayerStratCard),
			new PendulumChip("stratcards/gambinsurg-5",noscale,PendulumId.PlayerStratCard),
			new PendulumChip("stratcards/licimp-5",noscale,PendulumId.PlayerStratCard),
			new PendulumChip("stratcards/licalc-5",noscale,PendulumId.PlayerStratCard),
			new PendulumChip("stratcards/mespaci-5",noscale,PendulumId.PlayerStratCard),
			new PendulumChip("stratcards/mesjust-5",noscale,PendulumId.PlayerStratCard)

	};
	
	static public PendulumChip stratcards[] = {
		// these are in the same order as the playermats
		new PendulumChip("stratcards/bolkchamp-1",noscale,PendulumId.PlayerStratCard,stratBacks[0],PC.None,PB.Pres1),
		new PendulumChip("stratcards/bolkchamp-2",noscale,PendulumId.PlayerStratCard,stratBacks[0],PC.None,PB.R1),
		new PendulumChip("stratcards/bolkchamp-3",noscale,PendulumId.PlayerStratCard,stratBacks[0],PC.CanRetrieve,PB.Retrieve),
		new PendulumChip("stratcards/bolkchamp-4",noscale,PendulumId.PlayerStratCard,stratBacks[0],PC.M7Recruit,PB.Recruit),
		
		new PendulumChip("stratcards/bolkwar-1",noscale,PendulumId.PlayerStratCard,stratBacks[1],PC.None,PB.Pres1),
		new PendulumChip("stratcards/bolkwar-2",noscale,PendulumId.PlayerStratCard,stratBacks[1],PC.None,PB.BolkWar_2),
		new PendulumChip("stratcards/bolkwar-3",noscale,PendulumId.PlayerStratCard,stratBacks[1],PC.None,PB.BolkWar_3),
		new PendulumChip("stratcards/bolkwar-4",noscale,PendulumId.PlayerStratCard,stratBacks[1],PC.Pow2Recruit,PB.Recruit),

		new PendulumChip("stratcards/dhkinsid-1",noscale,PendulumId.PlayerStratCard,stratBacks[2],PC.None,PB.Pres1),
		new PendulumChip("stratcards/dhkinsid-2",noscale,PendulumId.PlayerStratCard,stratBacks[2],PC.None,PB.R1),
		new PendulumChip("stratcards/dhkinsid-3",noscale,PendulumId.PlayerStratCard,stratBacks[2],PC.CanRetrieve,PB.Retrieve),
		new PendulumChip("stratcards/dhkinsid-4",noscale,PendulumId.PlayerStratCard,stratBacks[2],PC.V3Recruit,PB.Recruit),

		new PendulumChip("stratcards/dhkty-1",noscale,PendulumId.PlayerStratCard,stratBacks[3],PC.C3,PB.Pres2),
		new PendulumChip("stratcards/dhkty-2",noscale,PendulumId.PlayerStratCard,stratBacks[3],PC.C1,PB.R3),
		new PendulumChip("stratcards/dhkty-3",noscale,PendulumId.PlayerStratCard,stratBacks[3],PC.None,PB.Dhkty_3),
		new PendulumChip("stratcards/dhkty-4",noscale,PendulumId.PlayerStratCard,stratBacks[3],PC.V4Recruit,PB.Recruit),

		new PendulumChip("stratcards/gambbriber-1",noscale,PendulumId.PlayerStratCard,stratBacks[4],PC.None,PB.Pres1),
		new PendulumChip("stratcards/gambbriber-2",noscale,PendulumId.PlayerStratCard,stratBacks[4],PC.None,PB.R1),
		new PendulumChip("stratcards/gambbriber-3",noscale,PendulumId.PlayerStratCard,stratBacks[4],PC.CanRetrieve,PB.Retrieve),
		new PendulumChip("stratcards/gambbriber-4",noscale,PendulumId.PlayerStratCard,stratBacks[4],PC.D7Recruit,PB.Recruit),

		new PendulumChip("stratcards/gambinsurg-1",noscale,PendulumId.PlayerStratCard,stratBacks[5],PC.None,PB.Pres1),
		new PendulumChip("stratcards/gambinsurg-2",noscale,PendulumId.PlayerStratCard,stratBacks[5],PC.None,PB.R1),
		new PendulumChip("stratcards/gambinsurg-3",noscale,PendulumId.PlayerStratCard,stratBacks[5],PC.None,PB.Gambinsurg_3),
		new PendulumChip("stratcards/gambinsurg-4",noscale,PendulumId.PlayerStratCard,stratBacks[5],PC.M4D4Recruit,PB.Recruit),

		new PendulumChip("stratcards/licimp-1",noscale,PendulumId.PlayerStratCard,stratBacks[6],PC.None,PB.Pres1),
		new PendulumChip("stratcards/licimp-2",noscale,PendulumId.PlayerStratCard,stratBacks[6],PC.None,PB.R1),
		new PendulumChip("stratcards/licimp-3",noscale,PendulumId.PlayerStratCard,stratBacks[6],PC.CanRetrieve,PB.Retrieve),
		new PendulumChip("stratcards/licimp-4",noscale,PendulumId.PlayerStratCard,stratBacks[6],PC.R8Recruit,PB.Recruit),


		new PendulumChip("stratcards/licalc-1",noscale,PendulumId.PlayerStratCard,stratBacks[7],PC.R2,PB.P2P2P2),
		new PendulumChip("stratcards/licalc-2",noscale,PendulumId.PlayerStratCard,stratBacks[7],PC.R2,PB.RetrieveStrat),
		new PendulumChip("stratcards/licalc-3",noscale,PendulumId.PlayerStratCard,stratBacks[7],PC.R2Retrieve,PB.Retrieve),
		new PendulumChip("stratcards/licalc-4",noscale,PendulumId.PlayerStratCard,stratBacks[7],PC.R8Recruit,PB.Recruit),

		new PendulumChip("stratcards/mesjust-1",noscale,PendulumId.PlayerStratCard,stratBacks[8],PC.None,PB.Pres1),
		new PendulumChip("stratcards/mesjust-2",noscale,PendulumId.PlayerStratCard,stratBacks[8],PC.None,PB.R1),
		new PendulumChip("stratcards/mesjust-3",noscale,PendulumId.PlayerStratCard,stratBacks[8],PC.CanRetrieve,PB.Retrieve),
		new PendulumChip("stratcards/mesjust-4",noscale,PendulumId.PlayerStratCard,stratBacks[8],PC.C7Recruit,PB.Recruit),

		new PendulumChip("stratcards/mespaci-1",noscale,PendulumId.PlayerStratCard,stratBacks[9],PC.C1,PB.Pres1),
		new PendulumChip("stratcards/mespaci-2",noscale,PendulumId.PlayerStratCard,stratBacks[9],PC.C4V2,PB.Mespaci_2),
		new PendulumChip("stratcards/mespaci-3",noscale,PendulumId.PlayerStratCard,stratBacks[9],PC.MesPaci_3,PB.Mespaci_3),
		new PendulumChip("stratcards/mespaci-4",noscale,PendulumId.PlayerStratCard,stratBacks[9],PC.Pop3Recruit,PB.Recruit),

	};
	static PendulumChip finalBack = new PendulumChip("council/finalrew-back",noscale,PendulumId.RewardDeck);
	static public PendulumChip finalrewardcards[] = {
			new PendulumChip("council/finalrew-1",noscale,PendulumId.RewardDeck,finalBack,PB.Pow1Pres1Pop1),
			new PendulumChip("council/finalrew-2",noscale,PendulumId.RewardDeck,finalBack,PC.D5,PB.Pop3),
			new PendulumChip("council/finalrew-3",noscale,PendulumId.RewardDeck,finalBack,PC.M5,PB.Pow3),
			new PendulumChip("council/finalrew-4",noscale,PendulumId.RewardDeck,finalBack,PC.C5,PB.Pres3),
			new PendulumChip("council/finalrew-5",noscale,PendulumId.RewardDeck,finalBack,PC.R10,PB.Legendary),
			finalBack
	};
	static PendulumChip councilBack = new PendulumChip("council/reward-back",noscale,PendulumId.RewardDeck);
	static public PendulumChip rewardcards[] = {
			new PendulumChip("council/reward-1",noscale,PendulumId.RewardDeck,councilBack,PB.BluePB),
			new PendulumChip("council/reward-2",noscale,PendulumId.RewardDeck,councilBack,PB.RedPB),
			new PendulumChip("council/reward-3",noscale,PendulumId.RewardDeck,councilBack,PB.ProvinceReward),
			new PendulumChip("council/reward-4",noscale,PendulumId.RewardDeck,councilBack,PC.NoMax3,PB.Max3),
			new PendulumChip("council/reward-5",noscale,PendulumId.RewardDeck,councilBack,PC.NoMax3,PB.Max3),
			new PendulumChip("council/reward-6",noscale,PendulumId.RewardDeck,councilBack,PB.Pow1Pres1Pop1),
			new PendulumChip("council/reward-7",noscale,PendulumId.RewardDeck,councilBack,PB.RetrieveAll),
			new PendulumChip("council/reward-8",noscale,PendulumId.RewardDeck,councilBack,PB.M5),
			new PendulumChip("council/reward-9",noscale,PendulumId.RewardDeck,councilBack,PB.C5),
			new PendulumChip("council/reward-10",noscale,PendulumId.RewardDeck,councilBack,PB.D5),
			new PendulumChip("council/reward-11",noscale,PendulumId.RewardDeck,councilBack,true,PB.V1),
			new PendulumChip("council/reward-12",noscale,PendulumId.RewardDeck,councilBack,true,PB.V1),
			new PendulumChip("council/reward-13",noscale,PendulumId.RewardDeck,councilBack,true,PC.Vote,PB.SwapVotes),
			new PendulumChip("council/reward-14",noscale,PendulumId.RewardDeck,councilBack,false,PB.ProvinceReward),
			new PendulumChip("council/reward-15",noscale,PendulumId.RewardDeck,councilBack,true,PC.M2Retrieve,PB.Retrieve),
			new PendulumChip("council/reward-16",noscale,PendulumId.RewardDeck,councilBack,true,PC.C2Retrieve,PB.Retrieve),
			new PendulumChip("council/reward-17",noscale,PendulumId.RewardDeck,councilBack,true,PC.M3,PB.Province),
			new PendulumChip("council/reward-18",noscale,PendulumId.RewardDeck,councilBack,true,PC.M3,PB.Province),
			new PendulumChip("council/reward-19",noscale,PendulumId.RewardDeck,councilBack,true,PB.FreeD2),
			new PendulumChip("council/reward-20",noscale,PendulumId.RewardDeck,councilBack,true,PC.D2,PB.Pop1),
			new PendulumChip("council/reward-21",noscale,PendulumId.RewardDeck,councilBack,true,PC.M2,PB.Pow1),
			new PendulumChip("council/reward-22",noscale,PendulumId.RewardDeck,councilBack,true,PC.C2,PB.Pres1),
			new PendulumChip("council/reward-23",noscale,PendulumId.RewardDeck,councilBack,true,PB.FreeD2),
			new PendulumChip("council/reward-24",noscale,PendulumId.RewardDeck,councilBack,true,PB.D1),
			new PendulumChip("council/reward-25",noscale,PendulumId.RewardDeck,councilBack,true,PC.V2,PB.Province),

			councilBack
	};
	static PendulumChip flipBack = new PendulumChip("council/flip-back",noscale,PendulumId.RewardDeck);
	static PendulumChip flipcard = new PendulumChip("council/flip",noscale,PendulumId.RewardDeck,flipBack,PC.MeepleAndGrande,PB.Grande);
	static PendulumChip defcard =  new PendulumChip("council/default",noscale,PendulumId.RewardDeck,councilBack,PB.P1P1P1);
	static PendulumChip extraCouncilCards[] = {
		flipBack,flipcard,defcard	
	};

	static PendulumChip achievementBack =new PendulumChip("achievement/achievement-back",noscale,PendulumId.AchievementCard,null);
	static public PendulumChip achievementcards[] = {
			new PendulumChip("achievement/achievement-1",noscale,PendulumId.AchievementCard,achievementBack,PC.M6XC2V3,PB.M2C2D2),
			new PendulumChip("achievement/achievement-2",noscale,PendulumId.AchievementCard,achievementBack,PC.M2C6V3,PB.V5),
			new PendulumChip("achievement/achievement-3",noscale,PendulumId.AchievementCard,achievementBack,PC.R12V3,PB.BluePB),
			new PendulumChip("achievement/achievement-4",noscale,PendulumId.AchievementCard,achievementBack,PC.M8V3,PB.D5),
			new PendulumChip("achievement/achievement-5",noscale,PendulumId.AchievementCard,achievementBack,PC.C8V3,PB.Province),
			new PendulumChip("achievement/achievement-6",noscale,PendulumId.AchievementCard,achievementBack,PC.D8V3,PB.RedPB),
			new PendulumChip("achievement/achievement-7",noscale,PendulumId.AchievementCard,achievementBack,PC.D3M3C3V3,PB.Pow1Pres1Pop1),
			new PendulumChip("achievement/achievement-8",noscale,PendulumId.AchievementCard,achievementBack,PC.C4D4V3,PB.Pres1Pop1),
			new PendulumChip("achievement/achievement-9",noscale,PendulumId.AchievementCard,achievementBack,PC.M4C4V3,PB.Pow1Pres1),
			new PendulumChip("achievement/achievement-10",noscale,PendulumId.AchievementCard,achievementBack,PC.M4D4V3,PB.Pow1Pop1),
			achievementBack 
	};
	
	static PendulumChip legendary = new PendulumChip("bits/legendary",new double[] {0.5,0.5,0.7},PendulumId.Legendary);	
	static PendulumChip purpleGlass = new PendulumChip("bits/purpleglass",new double[] {0.5,0.45,0.75},PendulumId.PurpleGlass);
	static PendulumChip grayGlass = new PendulumChip("bits/grayglass",noscale,PendulumId.GrayGlass);
	static PendulumChip vote = new PendulumChip("bits/vote",noscale,PendulumId.Vote);
	static PendulumChip singleChips[] = { 
		legendary, purpleGlass,vote,councilBoard,grayGlass,
	};
	static PendulumChip redCube = new PendulumChip("bits/red-cube",noscale,PendulumId.Cube);
	static PendulumChip blueCube = new PendulumChip("bits/blue-cube",noscale,PendulumId.Cube);
	static PendulumChip yellowCube = new PendulumChip("bits/yellow-cube",noscale,PendulumId.Cube);
	static PendulumChip cubes[] = 
		{
				redCube,blueCube,yellowCube,
		};
	
	static PendulumChip redPost = new PendulumChip("bits/red-post",noscale,PendulumId.Post);
	static PendulumChip bluePost = new PendulumChip("bits/blue-post",noscale,PendulumId.Post);
	static PendulumChip yellowPost = new PendulumChip("bits/yellow-post",noscale,PendulumId.Post);
	static PendulumChip posts[] = 
		{
				redPost,bluePost,yellowPost,
		};
	
	static PendulumChip blackTimer = new PendulumChip("bits/black-timer",noscale,PendulumId.BlackTimer,null);
	static PendulumChip greenTimer = new PendulumChip("bits/green-timer",noscale,PendulumId.GreenTimer,null);
	static PendulumChip purpleTimer = new PendulumChip("bits/purple-timer",new double[] {0.5,0.55,1},PendulumId.PurpleTimer,null);
	static PendulumChip hourglasses[] = {
		blackTimer,greenTimer,purpleTimer	
	};
    // indexes into the balls array, usually called the rack
    static final PendulumChip getChip(int n) { return((PendulumChip)otherChips.elementAt(n)); }
    
    /**
     * this is the basic hook to substitute an alternate chip for display.  The canvas getAltChipSet
     * method is called from drawChip, and the result is passed here to substitute a different chip.
     * 
     */
	public PendulumChip getAltChip(int set)
	{	
		return this;
	}


    /* plain images with no mask can be noted by naming them -nomask */
    static public PendulumChip backgroundTile = new PendulumChip("background-tile-nomask",null);
    static public PendulumChip backgroundReviewTile = new PendulumChip("background-review-tile-nomask",null);
   

    public static PendulumChip Icon = new PendulumChip("hex-icon-nomask",null);

    public static PendulumChip Refill = new PendulumChip("bits/refill-nomask",null);
    
   
    /**
     * this is a fairly standard preloadImages method, called from the
     * game initialization.  It loads the images into the stack of
     * chips we've built
     * @param forcan the canvas for which we are loading the images.
     * @param Dir the directory to find the image files.
     */
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(!imagesLoaded)
		{	
		otherChips.autoloadMaskGroup(Dir,"board_r12-mask",boards);
		otherChips.autoloadGroup(Dir,chips);
		otherChips.autoloadGroup(Dir,bigchips);
		otherChips.autoloadGroup(Dir,hexes);
		otherChips.autoloadGroup(Dir,cylinders);
		otherChips.autoloadMaskGroup(Dir,"provinces/cards_r8-mask",provinceCards);
		otherChips.autoloadMaskGroup(Dir,"cards-mask",stratBacks);
		otherChips.autoloadMaskGroup(Dir,"cards-mask",stratcards);
		otherChips.autoloadMaskGroup(Dir,"cards-mask",rewardcards);
		otherChips.autoloadMaskGroup(Dir,"cards-mask",extraCouncilCards);
		otherChips.autoloadMaskGroup(Dir,"cards-mask",finalrewardcards);
		otherChips.autoloadMaskGroup(Dir,"achievement/achievement-mask",achievementcards);
		otherChips.autoloadMaskGroup(Dir,"bits/digital-timer-mask",hourglasses);
		otherChips.autoloadMaskGroup(Dir,"bits/red-cube-mask",cubes);
		otherChips.autoloadMaskGroup(Dir,"bits/blue-post-mask",posts);
		otherChips.autoloadGroup(Dir,mats);
		otherChips.autoloadGroup(Dir,advancedmats);
		
		otherChips.autoloadGroup(Dir,singleChips);
		
		imagesLoaded = forcan.load_masked_images(Dir,otherChips);
		Image.registerImages(otherChips);
		}
	}   
	/**
	 * this is a debugging interface to provide information about images memory consumption
	 * in the "show images" option.
	 * It's especially useful in applications that are very image intensive.
	 * @param imstack
	 * @return size of images (in megabytes)
	 */
	public static double imageSize(ImageStack imstack)
	    {	double sum = otherChips.imageSize(imstack);
	    	return(sum);
	    }
	   
	/*
	// override for drawChip can draw extra ornaments or replace drawing entirely
	public void drawChip(Graphics gc,
	            exCanvas canvas,
	            int SQUARESIZE,
	            double xscale,
	            int cx,
	            int cy,
	            java.lang.String label)
	    {	super.drawChip(gc, canvas, SQUARESIZE, xscale, cx, cy, label);

	    }
	 */
	/*
	 * this is a standard trick to display card backs as an alternate to the normal face.
	 * there is a second piece in PendulumCell to substitute BACK all the way down, so
	 * the entire card deck is not required to be present.
	 * */
	
	public static String BACK = NotHelp+"_back_";	// the | causes it to be passed in rather than used as a tooltip
	
	public void drawChip(Graphics gc,exCanvas canvas,int SQUARESIZE,double xscale,int cx,int cy,String label)
	{
		boolean isBack = BACK.equals(label);
		if(back!=null && isBack)
		{
		 back.drawChip(gc,canvas,SQUARESIZE, xscale, cx, cy,null);
		}
		else
		{
		super.drawChip(gc, canvas, SQUARESIZE, xscale, cx, cy, label);
		}
	}
	public static PendulumChip find(String nextElement) {
		if(nextElement.charAt(0)=='c')
		{
			int idx = G.IntToken(nextElement.substring(1));
			PendulumChip ch = (PendulumChip)otherChips.elementAt(idx);
			G.Assert(ch!=null,"null!");
			return ch;
		}
		throw G.Error("Not a chip id");
	}
	public String idString() {
		return idString;
	}
	public boolean findChipHighlight(CellId rackLocation,HitPoint highlight,int e_x,int e_y,int squareWidth,int squareHeight,double sscale)
    {
    	return super.findChipHighlight(rackLocation,highlight,e_x,e_y,squareWidth,squareHeight,sscale);
    }

}
