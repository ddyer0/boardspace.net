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
package manhattan;

import lib.Digestable;
import lib.DrawableImage;
import lib.DrawableImageStack;
import lib.G;
import lib.Graphics;
import lib.Image;
import lib.ImageLoader;
import lib.ImageStack;
import lib.OStack;
import lib.Random;
import lib.StockArt;
import lib.exCanvas;
import online.game.chip;
import bridge.SystemImage.ScaleType;
import common.CommonConfig;
class ChipStack extends OStack<ManhattanChip> implements Digestable
{
	public ManhattanChip[] newComponentArray(int n) { return(new ManhattanChip[n]); }

	public long Digest(Random r) {
		long v = 0;
		for(int lim=size()-1; lim>=0; lim--) { v ^= elementAt(lim).Digest(r); }
		return v;
	}
}

/**
 * this is a specialization of {@link chip} to represent the stones used by pushfight;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class ManhattanChip extends chip<ManhattanChip> implements CommonConfig,ManhattanConstants
{

	private static DrawableImageStack otherChips = new DrawableImageStack();
	private static DrawableImageStack myChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public Type type = Type.Other;
	public ManhattanId id;
	public MColor color = null;
	public String ioSpec = "";
	ManhattanChip cardBack = null;
	public String contentsString() { return(id==null ? file : id.name()); }
	static Random r = new Random(63466235);
	static private double defaultScale[] = {0.5,0.5,1.0};
	public WorkerType workerType = WorkerType.N;	// not a worker
	public Cost cost = Cost.None;
	private int loadingCost = 0;
	private int bombValue = 0;
	private int bombTestedValue = 0;
	private boolean isAMine = false;
	public boolean isAMine() { return isAMine; }
	private boolean isAUniversity;
	public boolean isAUniversity() { return isAUniversity; }
	private boolean isAReactor = false;
	public boolean isAReactor() { return isAReactor; }
	private boolean isAnEnricher = false;
	public boolean isAnEnricher() { return isAnEnricher; }
	private boolean bombersAndMoney = false;
	public boolean bombersAndMoney() { return bombersAndMoney; }
	private boolean fightersAndMoney = false;
	public boolean fightersAndMoney() { return fightersAndMoney; }
	
	public ManhattanChip lores = null;
	public ManhattanChip hires = null;
	
	public ManhattanChip(Image im, ManhattanChip ho)
	{
		hires = ho;
		image = im;
		if(ho!=null)
		{
		scale = ho.scale;
		file = ho.file;
		color = ho.color;
		type = ho.type;
		}
		else { randomv = r.nextLong(); }
	}
	public DrawableImage<?> getAltSizeChip(exCanvas canvas,int SQUARESIZE,double xscale,int cx,int cy)
	{	/*
		if(canvas!=null)
		{
			int w = canvas.getWidth();
			int h = canvas.getHeight();
			int px = canvas.getSX();
			int py = canvas.getSY();
			int x = cx-px;
			int y = cy-py;
			if((x+SQUARESIZE*2<0)
					|| x-SQUARESIZE*2>w
					|| y+SQUARESIZE*2<0
					|| y-SQUARESIZE*2>h)
			{	//if(this==ManhattanChip.Yellowcake)
				{ //return ManhattanChip.backgroundReviewTile; 			
				}
			}
		}*/
		if(image!=null
				&& image.isUnloadable() 
				&& SQUARESIZE<image.getWidth()/2)
		{	// this is an ad-hoc heuristic for the manhattan project's artwork.  
			// all the images are unloadable, and are relatively huge.  If asking
			// for a scaled image that's much smaller than the original image, create
			// a new half-scale copy of the big image.  This reduces the image load 
			// by 75% for most cases, but still allows a giant card to be displayed
			// when needed.   The other half of the problem is that when zoomed up,
			// all the images need the hires version.  This is addressed by some 
			// logic in the canvas which skips all the image processing for images
			// that aren't visible.
			int imw = image.getWidth();
			int ratio = Math.max(2,imw/(SQUARESIZE*4));
			int neww = imw/ratio;
			G.Assert(neww>=SQUARESIZE,"bigger");
			if((lores!=null) && lores.getImage().getWidth()<neww)
				{
					//G.print("rescale ",lores);
					lores.getImage().unload();
					lores = null;
				}		
			if(lores==null)
			{
			Image smaller = image.getScaledInstance(image.getWidth()/ratio,image.getHeight()/ratio,ScaleType.defaultScale);
			ManhattanChip newchip = new ManhattanChip(smaller,this);
			image.unload();
			lores = newchip;
			//G.print("scalable ",this," ",image," ",image.isUnloaded()," ",SQUARESIZE," ",smaller);
			}
			
		}
		if((lores!=null) && lores.getImage().getWidth()>=SQUARESIZE)
		{
			return lores;
		}
		return this;
	}
	
	// nWorkersReqired is used when pricing the university costs for india
	int nWorkersRequired = 0;
	// nScientists and nEngineers are used when accessing worker costs for Israel
	int nScientistsRequired = 0;
	int nEngineersRequired = 0;
	public int nWorkersRequired() { return nWorkersRequired; }
	public int nScientistsRequired() { return nScientistsRequired; }
	public int nEngineersRequired() { return nEngineersRequired; }
	
	public boolean isPlutoniumBomb() { return bombTestedValue>bombValue; }
	public int loadingCost() { return loadingCost; }
	public int bombValue() { return bombValue; }
	public int bombTestedValue() { return bombTestedValue; }
	private Cost israeliCost = null;	// only for bombs
	public Cost getIsraeliCost()
	{
		return israeliCost;
	}
	public Benefit benefit = Benefit.None;
	private ManhattanChip(String na)
	{	file = na;
		scale = defaultScale;
		randomv = r.nextLong();
		otherChips.push(this);
	}
	private ManhattanChip(String na,WorkerType ty,MColor co)
	{	file = na;
		color = co;
		workerType = ty;
		type = Type.Worker;
		randomv = r.nextLong();
		myChips.push(this);
	}
	// constructor for all the other random artwork.
	private ManhattanChip(String na,Type t,double[]sc,MColor c )
	{	
		scale=sc;
		type = t;
		randomv = r.nextLong();
		file = na;
		color = c;
		randomv = r.nextLong();
		otherChips.push(this);
	}
	private ManhattanChip(String na,Type t,Cost req,Benefit be)
	{
		randomv = r.nextLong();
		cost = req;
		benefit = be;
		file = na;
		type = t;
		randomv = r.nextLong();

		if(t==Type.Bomb) 
			{ parseBenefits(); 
			  israeliCost = cost.getIsraeliCosts();
			}
		if(t==Type.Bombtest)
		{
			switch(benefit)
			{
			case Points0: bombValue = 0; break;
			case Points2: bombValue = 2; break;
			case Points4: bombValue = 4; break;
			case Points6: bombValue = 6; break;
			case Points8: bombValue = 8; break;
			default: throw G.Error("not expecting %s",benefit);
			}
		}
		setWorkerRequirements();
		setBuildingType();
		myChips.push(this);
		
	}
	private void setBuildingType()
	{
		switch(benefit)
		{
		case Yellowcake:
		case Yellowcake2:
		case Yellowcake3:
		case Yellowcake4:
		case Yellowcake6:
			isAMine = true;
			break;
		case Scientist:
		case Engineer:
		case ScientistOrEngineer:
		case Engineer2OrScientist:
		case Scientist2OrEngineer:
		case Worker4:
		case Engineer3:
		case Scientist2:
		case Scientist3:
			isAUniversity = true;
			break;
		case Plutonium:
		case Plutonium2:
		case Plutonium3:
		case Plutonium4:
			isAReactor = true;
			break;
		case Uranium:
		case Uranium2:
		case Uranium3:
			isAnEnricher = true;
			break;
		case FighterAnd2:
		case Fighter3And3:
			fightersAndMoney = true;
			break;
		case BomberAnd2:
		case Bomber3And3:
			bombersAndMoney = true;
			break;
			
			
		default: break;
			
		}
	}
	private void setWorkerRequirements()
	{
		switch(cost)
		{
		case ScientistAnd3YellowcakeAnd1:
		case ScientistAnd2YellowcakeAnd2:
		case ScientistAnd1YellowcakeAnd3:
		case ScientistAnd4YellowcakeAnd4:
		case ScientistAnd1Uranium:
		case ScientistAnd1Yellowcake:
		case ScientistAnd1UraniumOr2Yellowcake:
		case ScientistAnd3YellowcakeAnd5:
		case ScientistAnd5Yellowcake:
		case ScientistAnd3Uranium:
		case ScientistAndBombDesign:
		case Scientist:
			nScientistsRequired = 1;
			//$FALL-THROUGH$
		case AnyWorkerAndBomb:
		case AnyWorker:	nWorkersRequired = 1; 
			break;
		case Engineer:
			nEngineersRequired = 1;
			nWorkersRequired = 1;
			break;
			
		case ScientistAndEngineerAnd4Uranium:
		case ScientistAndEngineerAnd4Plutonium:
		case ScientistAndEngineerAnd3Uranium:
		case ScientistAndEngineerAndBombDesign:
			nScientistsRequired = 1;
			nEngineersRequired = 1;
			nWorkersRequired = 2;
			break;
			
		case Scientists2And6YellowcakeAnd7:
		case Scientist2And5YellowcakeAnd2:
		case Scientist2And1UraniumOr3Yellowcake:
		case Scientist2And4YellowcakeAnd3:
		case Scientist2And3Yellowcake:
		case Scientist2And6Yellowcake:
		case Scientists2And1UraniumOr7Yellowcake:
		case Scientists2And1UraniumOr4Yellowcake:
		case Scientist2And2YellowcakeAnd5:
		case Scientists2And3YellowcakeAnd4:
			nScientistsRequired = 2;
			nWorkersRequired = 2;
			break;
			
		case Scientist2AndEngineer2And5Uranium:
		case Scientist2AndEngineer2And6Uranium:
		case Scientist2AndEngineer2And5Plutonium:
		case Scientist2AndEngineer2And6Plutonium:
			nScientistsRequired = 2;
			nEngineersRequired = 2;
			nWorkersRequired = 4;
			break;
			
		case ScientistAndEngineer2And4Plutonium:
		case ScientistAndEngineer2And4Uranium:
		case ScientistAndEngineer2And5Uranium:
		case ScientistAndEngineer2And5Plutonium:
			nScientistsRequired = 1;
			nEngineersRequired = 2;
			nWorkersRequired = 3;
			break;

		case Scientist2AndEngineer3And6Plutonium:
		case Scientist2AndEngineer3And7Plutonium:
			nScientistsRequired = 2;
			nEngineersRequired = 3;
			nWorkersRequired = 5;
			break;
			
		case Scientist2AndEngineer4And7Plutonium:
			nScientistsRequired = 2;
			nEngineersRequired = 4;
			nWorkersRequired = 6;
			break;
			
			
		case Engineer2:
			nEngineersRequired = 2;
			//$FALL-THROUGH$
		case Any2WorkersAndRetrieve:	// germany
		case Any2WorkersAndCash:
		case Any2Workers: 
			nWorkersRequired = 2; 
			break;
		
		case Scientists3And8Yellowcake:
			nScientistsRequired = 3;
			nWorkersRequired = 3;
			break;
		case Engineer3:
			nEngineersRequired = 3;
			//$FALL-THROUGH$
		case Any3Workers: 
			nWorkersRequired = 3;
			break;
		
		default: G.Error("Not expecting %s",cost);
			break;
		case None:
			break;
		}
	}
	
	// main value first, tested value only for plutonium bombs, loading cost 
	// PxxTxxLx
	//
	public void parseBenefits()
	{
		G.Assert(type==Type.Bomb,"only for bombs");
		String name = benefit.name();
		for(int i=0,len=name.length(); i<len;i++)
		{
			switch(name.charAt(i))
			{
			case 'P':	
				{
					int cost = (name.charAt(++i)-'0')*10 + (name.charAt(++i)-'0');
					bombValue = bombTestedValue = cost;
				}		
				break;
			case 'T':
				{
				int cost = (name.charAt(++i)-'0')*10 + (name.charAt(++i)-'0');
				bombTestedValue = cost;
				}
				break;
			case 'L':
				{
				int cost = name.charAt(++i)-'0';
				loadingCost = cost;
				}
				break;
			default: throw G.Error("can't parse %s",name);
			}
		}
	}
	private ManhattanChip(String na,Type t)
	{
		randomv = r.nextLong();
		file = na;
		type = t;
	}

	static MColor playerColors[] = { MColor.Red, MColor.Green ,MColor.Blue,  MColor.Yellow, MColor.Purple};
	static ManhattanChip xx = new ManhattanChip(StockArt.Exmark.getImage(), null);
	static ManhattanChip icon = new ManhattanChip("manhattan-icon-nomask");
	static ManhattanChip board = new ManhattanChip("board-nomask");
	static ManhattanChip Yellowcake = new ManhattanChip("yellowcake",Type.Yellowcake,defaultScale,MColor.Gray);
	static ManhattanChip Plutonium = new ManhattanChip("plutonium",Type.Plutonium,defaultScale,MColor.Gray);
	static ManhattanChip Uranium = new ManhattanChip("uranium",Type.Uranium,defaultScale,MColor.Gray);
	static ManhattanChip Damage = new ManhattanChip("damage",Type.Damage,defaultScale,MColor.Gray);
	static ManhattanChip JapanAirstrike = new ManhattanChip("japan-airstrike",Type.JapanAirstrike,defaultScale,MColor.Gray);
	static ManhattanChip Spy = new ManhattanChip("spy-nomask",Type.Other,defaultScale,MColor.Gray);
	static ManhattanChip BomberSale = new ManhattanChip("bombersale-nomask",Type.BomberSale,defaultScale,MColor.Gray);
	static ManhattanChip AirstrikeHelp = new ManhattanChip("airstrike-nomask",Type.Help,defaultScale,MColor.Gray);
	static ManhattanChip BombtestHelp = new ManhattanChip("bombtest-nomask",Type.Help,defaultScale,MColor.Gray);
	static ManhattanChip Question = new ManhattanChip("question",Type.Other,defaultScale,MColor.Gray);
	static ManhattanChip BlankCard = new ManhattanChip("blankbomb",Type.Other,defaultScale,MColor.Gray);
	static ManhattanChip RotateCW = new  ManhattanChip("rotatecw",Type.Other,defaultScale,MColor.Gray);
	static ManhattanChip RotateCCW = new  ManhattanChip("rotateccw",Type.Other,defaultScale,MColor.Gray);
	
	static ManhattanChip playerBoards[] = {
			new ManhattanChip("player-board-red-nomask",Type.Other,defaultScale,MColor.Red),
			new ManhattanChip("player-board-green-nomask",Type.Other,defaultScale,MColor.Green),
			new ManhattanChip("player-board-blue-nomask",Type.Other,defaultScale,MColor.Blue),
			new ManhattanChip("player-board-yellow-nomask",Type.Other,defaultScale,MColor.Yellow),
			new ManhattanChip("player-board-purple-nomask",Type.Other,defaultScale,MColor.Purple),
	};
	
	static ManhattanChip coin_1 =  new ManhattanChip("coin-1",Type.Coin,defaultScale,null);
	static ManhattanChip coin_2 =  new ManhattanChip("coin-2",Type.Coin,defaultScale,null);
	static ManhattanChip coin_5 =  new ManhattanChip("coin-5",Type.Coin,new double[] {0.5,0.5,1.2},null);
	static ManhattanChip built =  new ManhattanChip("built",Type.Bombbuilt,new double[] {0.6,0.4,1.2},null);
		
	static ManhattanChip playerChips[] = {
			new ManhattanChip("red-stone",Type.Marker,defaultScale,MColor.Red),
			new ManhattanChip("green-stone",Type.Marker,defaultScale,MColor.Green),
			new ManhattanChip("blue-stone",Type.Marker,defaultScale,MColor.Blue),
			new ManhattanChip("yellow-stone",Type.Marker,defaultScale,MColor.Yellow),
			new ManhattanChip("purple-stone",Type.Marker,defaultScale,MColor.Purple),
	};

	public int chipNumber() { return(id==null?-1:id.ordinal()); }
	
	static ManhattanChip playerBombers[] = {
			 new ManhattanChip("red-bomber-nomask",Type.Bomber,defaultScale,MColor.Red),
			 new ManhattanChip("green-bomber-nomask",Type.Bomber,defaultScale,MColor.Green),
			 new ManhattanChip("blue-bomber-nomask",Type.Bomber,defaultScale,MColor.Blue),
			 new ManhattanChip("yellow-bomber-nomask",Type.Bomber,defaultScale,MColor.Yellow),
			 new ManhattanChip("purple-bomber-nomask",Type.Bomber,defaultScale,MColor.Purple),
	};
	public static ManhattanChip getBomber(MColor color)
	{
		for(ManhattanChip ch : playerBombers) { if(ch.color==color) { return ch; }}
		return null;
	}
	static ManhattanChip playerFighters[] = {
		new ManhattanChip("red-fighter-nomask",Type.Fighter,defaultScale,MColor.Red),
		new ManhattanChip("green-fighter-nomask",Type.Fighter,defaultScale,MColor.Green),
		new ManhattanChip("blue-fighter-nomask",Type.Fighter,defaultScale,MColor.Blue),
		new ManhattanChip("yellow-fighter-nomask",Type.Fighter,defaultScale,MColor.Yellow),
		new ManhattanChip("purple-fighter-nomask",Type.Fighter,defaultScale,MColor.Purple),
		};
	public static ManhattanChip getFighter(MColor color)
	{
		for(ManhattanChip ch : playerFighters) { if(ch.color==color) { return ch; }}
		return null;
	}

	public static ManhattanChip BuildingBackA = new ManhattanChip("buildingbacka",Type.Building);
	public static ManhattanChip BuildingBackB = new ManhattanChip("buildingbackb",Type.Building);
	public static ManhattanChip BombBack = new ManhattanChip("bombback",Type.Bomb);
	public static ManhattanChip NationsBack = new ManhattanChip("nations-back",Type.Nations);
	public static ManhattanChip Nations2Back = new ManhattanChip("nations2-back",Type.Nations);

	static ManhattanChip otherCards[] = {
			BuildingBackA,
			BuildingBackB,
			BombBack,
			
	};
	static ManhattanChip BombTests[] = { 
			new ManhattanChip("bombtest-0",Type.Bombtest,Cost.None,Benefit.Points0),
			new ManhattanChip("bombtest-2",Type.Bombtest,Cost.None,Benefit.Points2),
			new ManhattanChip("bombtest-4",Type.Bombtest,Cost.None,Benefit.Points4),
			new ManhattanChip("bombtest-6",Type.Bombtest,Cost.None,Benefit.Points6),
			new ManhattanChip("bombtest-8",Type.Bombtest,Cost.None,Benefit.Points8),
		};
	
	static ManhattanChip startingBuildings[] = {
			new ManhattanChip("building01",Type.Building,Cost.AnyWorker,Benefit.FighterOr2),
			new ManhattanChip("building02",Type.Building,Cost.AnyWorker,Benefit.BomberOr2),
			
			new ManhattanChip("building11",Type.Building,Cost.AnyWorker,Benefit.Yellowcake),
			
			new ManhattanChip("building12",Type.Building,Cost.Any2Workers, Benefit.Yellowcake2),
			new ManhattanChip("building21",Type.Building,Cost.AnyWorker, Benefit.Scientist),
			new ManhattanChip("building22",Type.Building,Cost.AnyWorker, Benefit.Engineer),
	};
	static ManhattanChip buildings[] =
		{
		new ManhattanChip("building03",Type.Building,Cost.AnyWorker, Benefit.FighterOrBomber),
		new ManhattanChip("building04",Type.Building,Cost.Any2Workers,Benefit.FighterAnd2),
		new ManhattanChip("building05",Type.Building,Cost.Any2Workers, Benefit.BomberAnd2),
		new ManhattanChip("building06",Type.Building,Cost.Engineer, Benefit.Five),
		new ManhattanChip("building07",Type.Building,Cost.Engineer, Benefit.Fighter2OrBomber2),
		new ManhattanChip("building08",Type.Building,Cost.Engineer2, Benefit.Fighter2AndBomber2),
		new ManhattanChip("building09",Type.Building,Cost.Engineer2, Benefit.Fighter3And3),
		new ManhattanChip("building10",Type.Building,Cost.Engineer2, Benefit.Bomber3And3),
		new ManhattanChip("building13",Type.Building,Cost.Any3Workers, Benefit.Yellowcake3),
		new ManhattanChip("building14",Type.Building,Cost.Any2Workers, Benefit.Yellowcake3),
		new ManhattanChip("building15",Type.Building,Cost.Any3Workers, Benefit.Yellowcake4),
		new ManhattanChip("building16",Type.Building,Cost.Engineer, Benefit.Yellowcake2),
		new ManhattanChip("building17",Type.Building,Cost.Engineer, Benefit.Yellowcake3),
		new ManhattanChip("building18",Type.Building,Cost.Engineer2, Benefit.Yellowcake3),
		new ManhattanChip("building19",Type.Building,Cost.Engineer2, Benefit.Yellowcake4),
		new ManhattanChip("building20",Type.Building,Cost.Engineer3, Benefit.Yellowcake6),
		new ManhattanChip("building23",Type.Building,Cost.AnyWorker, Benefit.ScientistOrEngineer),
		new ManhattanChip("building24",Type.Building,Cost.Any2Workers, Benefit.Engineer2OrScientist),
		new ManhattanChip("building25",Type.Building,Cost.Any2Workers, Benefit.Scientist2OrEngineer),
		new ManhattanChip("building26",Type.Building,Cost.Engineer, Benefit.Worker4),	// 4 regular workers
		new ManhattanChip("building27",Type.Building,Cost.Any2Workers, Benefit.Engineer3),
		new ManhattanChip("building28",Type.Building,Cost.Engineer, Benefit.Engineer3),
		new ManhattanChip("building29",Type.Building,Cost.Scientist,Benefit.Scientist2),
		new ManhattanChip("building30",Type.Building,Cost.Scientist, Benefit.Scientist3),
		new ManhattanChip("building31",Type.Building,Cost.Scientist2And5YellowcakeAnd2, Benefit.Uranium2),
		new ManhattanChip("building32",Type.Building,Cost.Scientists2And6YellowcakeAnd7,Benefit.Uranium3),
		new ManhattanChip("building33",Type.Building,Cost.ScientistAnd3YellowcakeAnd1,Benefit.Uranium),
		new ManhattanChip("building34",Type.Building,Cost.ScientistAnd2YellowcakeAnd2,Benefit.Uranium),
		new ManhattanChip("building35",Type.Building,Cost.ScientistAnd1YellowcakeAnd3,Benefit.Uranium),
		new ManhattanChip("building36",Type.Building,Cost.Scientist2And4YellowcakeAnd3,Benefit.Uranium2),
		new ManhattanChip("building37",Type.Building,Cost.ScientistAnd4YellowcakeAnd4, Benefit.Uranium2),
		new ManhattanChip("building38",Type.Building,Cost.Scientists2And3YellowcakeAnd4,Benefit.Uranium2),
		new ManhattanChip("building39",Type.Building,Cost.ScientistAnd3YellowcakeAnd5, Benefit.Uranium2),
		new ManhattanChip("building40",Type.Building,Cost.Scientist2And2YellowcakeAnd5, Benefit.Uranium2),
	
		new ManhattanChip("building41",Type.Building,Cost.ScientistAnd1Yellowcake,Benefit.Plutonium),
		new ManhattanChip("building42",Type.Building,Cost.ScientistAnd1UraniumOr2Yellowcake,Benefit.Plutonium),
		new ManhattanChip("building43",Type.Building,Cost.ScientistAnd1Uranium,Benefit.Plutonium2),
		new ManhattanChip("building44",Type.Building,Cost.Scientist2And3Yellowcake,Benefit.Plutonium2),
		new ManhattanChip("building45",Type.Building,Cost.Scientists2And1UraniumOr4Yellowcake,Benefit.Plutonium2),
		new ManhattanChip("building46",Type.Building,Cost.ScientistAnd5Yellowcake,Benefit.Plutonium2),
		new ManhattanChip("building47",Type.Building,Cost.Scientist2And1UraniumOr3Yellowcake,Benefit.Plutonium2),
		new ManhattanChip("building48",Type.Building,Cost.Scientist2And6Yellowcake,Benefit.Plutonium3),
		new ManhattanChip("building49",Type.Building,Cost.Scientists2And1UraniumOr7Yellowcake,Benefit.Plutonium3),
		new ManhattanChip("building50",Type.Building,Cost.Scientists3And8Yellowcake,Benefit.Plutonium4),

				
		};
	
	public static ManhattanChip bombs[] = {
			new ManhattanChip("bomb00",Type.Bomb,Cost.ScientistAnd3Uranium,Benefit.P09T09L1),
			new ManhattanChip("bomb01",Type.Bomb,Cost.ScientistAnd3Uranium,Benefit.P10T10L1),	
			new ManhattanChip("bomb02",Type.Bomb,Cost.ScientistAnd3Uranium,Benefit.P11T11L1),	
			new ManhattanChip("bomb03",Type.Bomb,Cost.ScientistAndEngineerAnd3Uranium,Benefit.P12T12L1),	
			new ManhattanChip("bomb04",Type.Bomb,Cost.ScientistAndEngineerAnd4Uranium,Benefit.P13T13L1),
			new ManhattanChip("bomb05",Type.Bomb,Cost.ScientistAndEngineerAnd4Uranium,Benefit.P14T14L2),
			new ManhattanChip("bomb06",Type.Bomb,Cost.ScientistAndEngineer2And4Uranium,Benefit.P15T15L2),
			new ManhattanChip("bomb07",Type.Bomb,Cost.ScientistAndEngineer2And4Uranium,Benefit.P16T16L2),
			new ManhattanChip("bomb08",Type.Bomb,Cost.ScientistAndEngineer2And5Uranium,Benefit.P18T18L2),
			new ManhattanChip("bomb09",Type.Bomb,Cost.ScientistAndEngineer2And5Uranium,Benefit.P20T20L3),
			new ManhattanChip("bomb10",Type.Bomb,Cost.ScientistAndEngineer2And5Uranium,Benefit.P22T22L3),
			new ManhattanChip("bomb11",Type.Bomb,Cost.Scientist2AndEngineer2And5Uranium,Benefit.P24T24L3),
			new ManhattanChip("bomb12",Type.Bomb,Cost.Scientist2AndEngineer2And6Uranium,Benefit.P26T26L3),
			new ManhattanChip("bomb13",Type.Bomb,Cost.Scientist2AndEngineer2And6Uranium,Benefit.P28T28L4),	
			new ManhattanChip("bomb14",Type.Bomb,Cost.Scientist2AndEngineer2And6Uranium,Benefit.P30T30L5),	
			
			new ManhattanChip("bomb15",Type.Bomb,Cost.ScientistAndEngineerAnd4Plutonium,Benefit.P08T13L2),	
			new ManhattanChip("bomb16",Type.Bomb,Cost.ScientistAndEngineerAnd4Plutonium,Benefit.P08T14L2),	
			new ManhattanChip("bomb17",Type.Bomb,Cost.ScientistAndEngineer2And4Plutonium,Benefit.P09T15L2),
			new ManhattanChip("bomb18",Type.Bomb,Cost.Scientist2AndEngineer2And5Plutonium,Benefit.P11T19L3),	
			new ManhattanChip("bomb19",Type.Bomb,Cost.Scientist2AndEngineer2And6Plutonium,Benefit.P11T20L3),	
			new ManhattanChip("bomb20",Type.Bomb,Cost.ScientistAndEngineer2And4Plutonium,Benefit.P09T16L2),
			new ManhattanChip("bomb21",Type.Bomb,Cost.ScientistAndEngineer2And5Plutonium,Benefit.P10T17L2),
			new ManhattanChip("bomb22",Type.Bomb,Cost.ScientistAndEngineer2And5Plutonium,Benefit.P10T18L3),
			new ManhattanChip("bomb23",Type.Bomb,Cost.Scientist2AndEngineer2And6Plutonium,Benefit.P12T22L4),
			new ManhattanChip("bomb24",Type.Bomb,Cost.Scientist2AndEngineer3And6Plutonium,Benefit.P12T24L4),
			new ManhattanChip("bomb25",Type.Bomb,Cost.Scientist2AndEngineer3And6Plutonium,Benefit.P13T26L5),
			new ManhattanChip("bomb26",Type.Bomb,Cost.Scientist2AndEngineer3And7Plutonium,Benefit.P14T28L6),
			new ManhattanChip("bomb27",Type.Bomb,Cost.Scientist2AndEngineer3And7Plutonium,Benefit.P16T30L7),
			new ManhattanChip("bomb28",Type.Bomb,Cost.Scientist2AndEngineer4And7Plutonium,Benefit.P18T32L8),
			new ManhattanChip("bomb29",Type.Bomb,Cost.Scientist2AndEngineer4And7Plutonium,Benefit.P20T34L9),
	};
	static ManhattanChip USA = new ManhattanChip("nations-01",Type.Nations,Cost.AnyWorker,Benefit.Nations_USA);
	static ManhattanChip UK = new ManhattanChip("nations-02",Type.Nations,Cost.AnyWorker,Benefit.Nations_UK);
	static ManhattanChip USSR = new ManhattanChip("nations-03",Type.Nations,Cost.Any2Workers,Benefit.Nations_USSR);
	static ManhattanChip GERMANY = new ManhattanChip("nations-04",Type.Nations,Cost.Any2WorkersAndRetrieve,Benefit.Nations_GERMANY);
	static ManhattanChip JAPAN = new ManhattanChip("nations-05",Type.Nations,Cost.Any2Workers,Benefit.Nations_JAPAN);
	static ManhattanChip CHINA = new ManhattanChip("nations-06",Type.Nations,Cost.AnyWorker,Benefit.Nations_CHINA);
	static ManhattanChip FRANCE = new ManhattanChip("nations-07",Type.Nations,Cost.ScientistAndBombDesign,Benefit.Nations_FRANCE);
	static ManhattanChip SOUTH_AFRICA = new ManhattanChip("nations2-07",Type.Nations,Cost.Engineer,Benefit.Nations_SOUTH_AFRICA);
	static ManhattanChip AUSTRALIA = new ManhattanChip("nations2-01",Type.Nations,Cost.Any3Workers,Benefit.Nations_AUSTRALIA);
	static ManhattanChip BRAZIL = new ManhattanChip("nations2-02",Type.Nations,Cost.AnyWorker,Benefit.Nations_BRAZIL);
	static ManhattanChip INDIA = new ManhattanChip("nations2-03",Type.Nations,Cost.Any2WorkersAndCash,Benefit.Nations_INDIA);
	static ManhattanChip ISRAEL = new ManhattanChip("nations2-04",Type.Nations,Cost.AnyWorker,Benefit.Nations_ISRAEL);
	static ManhattanChip NORTH_KOREA = new ManhattanChip("nations2-05",Type.Nations,Cost.ScientistAnd1Yellowcake,Benefit.Nations_NORTH_KOREA);
	static ManhattanChip PAKISTAN = new ManhattanChip("nations2-06",Type.Nations,Cost.AnyWorkerAndBomb,Benefit.Nations_PAKISTAN);
	static ManhattanChip Nations[] = {
			USA,		// usa
			UK,		// britain
			USSR,	// soviet union
			GERMANY,	// germany
			JAPAN,	// japan
			CHINA,		// china
			FRANCE,		// france
			AUSTRALIA,	// australia
			BRAZIL,	// brazil
			INDIA,	// india
			ISRAEL,	// israel
			NORTH_KOREA, // north korea
			PAKISTAN,	// pakistan
			SOUTH_AFRICA,	// south africa
	};
	
	static ManhattanChip NationsBacks[] = {
			NationsBack,
			Nations2Back,
	};
	static ManhattanChip Oppenheimer = new ManhattanChip("oppenheimer-front",Type.Personalities);
	static ManhattanChip Fuchs = new ManhattanChip("fuchs-front",Type.Personalities);
	static ManhattanChip Groves = new ManhattanChip("groves-front",Type.Personalities);
	static ManhattanChip Lemay = new ManhattanChip("lemay-front",Type.Personalities);
	static ManhattanChip Szilard = new ManhattanChip("szilard-front",Type.Personalities);
	static ManhattanChip Nichols = new ManhattanChip("nichols-front",Type.Personalities);
	static ManhattanChip Landsdale = new ManhattanChip("lansdale-front",Type.Personalities);
	
	static ManhattanChip Personalities[] = {
			Oppenheimer,
			new ManhattanChip("oppenheimer-back",Type.Other),
			Fuchs,
			new ManhattanChip("fuchs-back",Type.Other),
			Groves,
			new ManhattanChip("groves-back",Type.Other),
			Lemay,
			new ManhattanChip("lemay-back",Type.Other),
			Szilard,
			new ManhattanChip("szilard-back",Type.Other),
			Nichols,
			new ManhattanChip("nichols-back",Type.Other),
			Landsdale,
			new ManhattanChip("lansdale-back",Type.Other),
	};
	static public ManhattanChip GrayScientist = new ManhattanChip("worker-xs",WorkerType.S,MColor.Gray);
	
	static ManhattanChip scientists[] = {
			new ManhattanChip("worker-rs",WorkerType.S,MColor.Red),
			new ManhattanChip("worker-gs",WorkerType.S,MColor.Green),
			new ManhattanChip("worker-bs",WorkerType.S,MColor.Blue),
			new ManhattanChip("worker-ys",WorkerType.S,MColor.Yellow),
			new ManhattanChip("worker-ps",WorkerType.S,MColor.Purple),
			GrayScientist
	};
	
	static public ManhattanChip GrayEngineer = new ManhattanChip("worker-xe",WorkerType.E,MColor.Gray);
	
	static ManhattanChip engineers[] = {
			new ManhattanChip("worker-re",WorkerType.E,MColor.Red),
			new ManhattanChip("worker-ge",WorkerType.E,MColor.Green),
			new ManhattanChip("worker-be",WorkerType.E,MColor.Blue),
			new ManhattanChip("worker-ye",WorkerType.E,MColor.Yellow),
			new ManhattanChip("worker-pe",WorkerType.E,MColor.Purple),
			GrayEngineer
			
	};
	static public ManhattanChip GrayWorker = new ManhattanChip("worker-xl",WorkerType.L,MColor.Gray);
	static ManhattanChip laborers[] = {
			new ManhattanChip("worker-rl",WorkerType.L,MColor.Red),
			new ManhattanChip("worker-gl",WorkerType.L,MColor.Green),
			new ManhattanChip("worker-bl",WorkerType.L,MColor.Blue),
			new ManhattanChip("worker-yl",WorkerType.L,MColor.Yellow),
			new ManhattanChip("worker-pl",WorkerType.L,MColor.Purple),	
			GrayWorker,
	};
	
	static public ManhattanChip GreyGuys[] = 
		{
			GrayScientist,GrayEngineer,GrayWorker

		};
	
    /**
     * this is the basic hook to substitute an alternate chip for display.  The canvas getAltChipSet
     * method is called from drawChip, and the result is passed here to substitute a different chip.
     * 
     */
	public ManhattanChip getAltChip(int set)
	{	
		return this;
	}


    /* plain images with no mask can be noted by naming them -nomask */
    static public ManhattanChip backgroundTile = new ManhattanChip("background-tile-nomask");
    static public ManhattanChip backgroundReviewTile = new ManhattanChip("background-review-tile-nomask");
     

	/*
	 * this is a standard trick to display card backs as an alternate to the normal face.
	 * */
	
	public static String BACK = NotHelp+"_back_";	// the | causes it to be passed in rather than used as a tooltip
	

	public void drawChip(Graphics gc,exCanvas canvas,int SQUARESIZE,double xscale,int cx,int cy,String label)
	{
		boolean isBack = BACK.equals(label);
		if(cardBack!=null && isBack)
		{
		 cardBack.drawChip(gc,canvas,SQUARESIZE, xscale, cx, cy,null);
		}
		else
		{ super.drawChip(gc, canvas, SQUARESIZE, xscale, cx, cy, label);
		}
	}


   
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
		//
		// these images will be autoloaded, but more support in drawChip and drawCell is needed 
		// to make sure they aren't loaded unnecessarily
		//
		myChips.autoloadMaskGroup(Dir,"buildingbackb-mask",otherCards);
		myChips.autoloadMaskGroup(Dir,"buildingbackb-mask",startingBuildings);
		myChips.autoloadMaskGroup(Dir,"buildingbackb-mask",buildings);
		myChips.autoloadMaskGroup(Dir,"buildingbackb-mask",bombs);
		
		
		for(ManhattanChip c : startingBuildings) { c.cardBack = BuildingBackA; }
		for(ManhattanChip c : buildings) { c.cardBack = BuildingBackB; }
		for(ManhattanChip c : bombs) { c.cardBack = BombBack; }

		otherChips.autoloadGroup(Dir);	
		
		myChips.autoloadMaskGroup(Dir,"nations-mask",Nations);
		myChips.autoloadMaskGroup(Dir,"nations-mask",NationsBacks);
		double nationScale[] = new double[] {0.5,0.5,0.9};
		for(ManhattanChip ch : Nations) { ch.scale = nationScale;ch.cardBack = NationsBack; }
		
		myChips.autoloadMaskGroup(Dir,"personalities-mask",Personalities);

		for(int i=0;i<Personalities.length;i+=2)
		{
			Personalities[i].cardBack = Personalities[i+1];
			Personalities[i+1].cardBack = Personalities[i];
		}
		
		myChips.autoloadMaskGroup(Dir,"worker-s-mask",scientists);
		myChips.autoloadMaskGroup(Dir,"worker-e-mask",engineers);
		myChips.autoloadMaskGroup(Dir,"worker-l-mask",laborers);
		myChips.autoloadMaskGroup(Dir,"bombtest-mask",BombTests);

	
		imagesLoaded = true;
		Image.registerImages(myChips);
		Image.registerImages(otherChips);
		}
	}   
	   public static double imageSize(ImageStack imstack)
	    {	double sum = otherChips.imageSize(imstack)+myChips.imageSize(imstack);
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
	
}
