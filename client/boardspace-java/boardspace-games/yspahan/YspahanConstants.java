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

import lib.CellId;
import lib.G;
import lib.InternationalStrings;
import lib.NameProvider;
import lib.OStack;
import online.game.BaseBoard.BoardState;


public interface YspahanConstants 
{	
	static final String Yspahan_INIT = "yspahan";	//init for standard game
	static final int N_DICE = 9;					// 9 is the standard number of dice
		
  
	//
	// these values must be distinct from standard opcodes and widget ids, which are all negative.
	//
	enum yrack implements CellId
	{	NoWhere("Nowhere"),
		Bag_Neighborhood("Bag"),
		Chest_Neighborhood("Chest"),
		Barrel_Neighborhood("Barrel"),
		Vase_Neighborhood("Vase"),
		Supervisor_Track("Supervisor"),
		Card_Stack("Cards"),
		Discard_Stack("Discards"),
		Time_Track("Time"),
		Building_Track("Buildings"),
		Misc_Track("Misc"),
	    Caravan_Track("Caravan"),
	    Dice_Tower("Dice"),
	    Camel_Pool("Camels"),
	    Gold_Pool("Gold"),
	    Dice_Table("Table"),
		Zoom_Slider("Zoom"),
		HitPayCamelButton("PayCamel"),
		HitSendCubeButton("SendCube"),
		HitShowCardsButton("ShowCards");
		// constructor
		yrack(String nam)
		{	
			name = nam;
		}
		String name;
		public String shortName() { return(name); }
		// convert back from integer values in the moves to the enum items
		static public yrack find(int val) 
		{ for(yrack v : values()) { if(v.ordinal()==val) { return(v); }}
		  return(null); 
		}
	    
	}
	static String camelHelpText = "#1{##no Camels, Camel, Camels}";
	static String goldHelpText = "#1{##no Gold, Gold, Gold}";
	static String cardHelpText = "#1{##no cards, card, cards}";
	static String victoryHelpText = "#1{##no Victory points, Victory Point, Victory Points}";
	static String ServiceName = "Yspahan Player info for #1";
	static String WarnCubeMessage = "Warning: it's not your cube!";

    // this represents the "type of" a chip, and also the "type of" a cell. 
	// most cells have a prescribed type, and can only hold objects of that type.
    public enum yclass
    {	playerCubes("Player Cubes"), 
    	gold(goldHelpText),
    	camels(camelHelpText),
    	supervisor("Supervisor"),
    	timeCubes("Time Cubes"),
    	dice("Dice"),
    	cards(cardHelpText),
    	points(victoryHelpText),
    	nullSet(null);
    	// constructor
    	yclass(String text)
    	{	helpText = text;
    	}
    	String helpText = null;
    }
    
    // cards that do special things
    public enum ycard implements NameProvider
	{
		back("back",null,null),
		card_3_camels("3_camels","Receive 3 camels",ystate.CONFIRM_CARD_STATE),
		card_3_gold("3_gold","Receive 3 gold",ystate.CONFIRM_CARD_STATE),
		card_buy_no_camels("no_camels","Buy a building with no camels",ystate.CONFIRM_CARD_STATE),
		card_buy_no_gold("no_gold","Buy a building with no gold",ystate.CONFIRM_CARD_STATE),
		card_place_caravan("place_caravan","Place a cube on the caravan",ystate.CONFIRM_CARD_STATE),
		
		card_place_board("place_board","Place a cube in any unclaimed souk",ystate.CARD_PLACE_CUBE_SOUK),
		card_score_gold("score_gold","Trade up to 10 gold for 1 victory point each",ystate.CARD_SCORE_GOLD),
		card_score_camels("score_camels","Trade up to 4 camels for 2 victory points each",ystate.CARD_SCORE_CAMELS),
		card_swap_camels_gold("swap_camels_gold","Trade camels for gold",ystate.CARD_TRADE_CAMELS_GOLD);
		// constructor
		ycard(String image,String text,ystate newst)
			{ 	imageName=image; 
				helpText = text;
				newstate = newst;
			}
		String helpText = "";
		yclass type = yclass.cards;
		String imageName = null;
		ystate newstate;
		public String getName() { return(imageName); }
		YspahanChip chip = null;
		static double cardScale[] = { 0.5,0.5,1.0};
	}

    // cubes that represent players
	public enum ycube implements NameProvider
	{	green("green-cube"),
		yellow("yellow-cube"),
		red("red-cube"),
		blue("blue-cube");
		// constructor
		ycube(String im) { imageName=im;}
		yclass type = yclass.playerCubes;
		String imageName;
		public String getName() { return(imageName); } 
		YspahanChip chip = null;
		static double cubeScale[] = { 0.5,0.5,1.0};
	}

	// white and yellow dice
	public enum ydie implements NameProvider
	{	
		d6_1w(false,1,"white-d6-1",	0.6,0.47,1.392),
		d6_2w(false,2,"white-d6-2",	0.6,0.47,1.366),
		d6_3w(false,3,"white-d6-3",	0.6,0.47,1.266),
		d6_4w(false,4,"white-d6-4",	0.6,0.47,1.363),
		d6_5w(false,5,"white-d6-5",	0.6,0.47,1.46),
		d6_6w(false,6,"white-d6-6",	0.6,0.47,1.28),
		d6_1y(true,1,"yellow-d6-1",	0.6,0.47,1.4),
		d6_2y(true,2,"yellow-d6-2",	0.6,0.47,1.453),
		d6_3y(true,3,"yellow-d6-3",	0.6,0.47,1.35),
		d6_4y(true,4,"yellow-d6-4",	0.6,0.47,1.5),
		d6_5y(true,5,"yellow-d6-5",	0.6,0.47,1.56),
		d6_6y(true,6,"yellow-d6-6",	0.6,0.47,1.35);
		int faceValue=0;
		boolean yellow = false;
		// constructor
		ydie(boolean yy,int v,String str,double x,double y, double s) 
		{ faceValue = v; imageName = str; scale[0]=x; scale[1]=y; scale[2]=s; yellow = yy; }
		yclass type = yclass.dice;
		YspahanChip chip = null;
		double scale[] = new double[3];
		String imageName;
		public String getName() { return(imageName); } 
	}

	// other random objects in the game space
	public enum ymisc implements NameProvider
	{	white("white-cube",	yclass.timeCubes,		0.5,0.5,1.0),		// the time markers
		point("black-cube", yclass.points,			0.5,0.5,1.0),		// never seen, but marks the VP cell
		gold("gold", yclass.gold,					0.6,0.5,0.94),		// money money money
		camel("camel", yclass.camels,				0.5,0.5,1.0),		// and camels of course
		supervisor("supervisor", yclass.supervisor,	0.5,0.5,1.0),		// white supervisor pawn
    	firstPlayer("firstPlayer",yclass.nullSet,		0.5,0.5,1.0);	// the "first player" marker, actually a pawn
		// constructor
		ymisc(String str,yclass tt,double x,double y, double s) 
		{ imageName = str; type=tt; scale[0]=x; scale[1]=y; scale[2]=s; }
		yclass type = null;
		YspahanChip chip = null;
		double scale[] = new double[3];
		String imageName;
		public String getName() { return(imageName); } 
	}
	static final String extraCamelCost = "extraCamelBuilding";
	static final String extraGoldCost = "extraGoldBuilding";
	static final String extraMovementCost = "extraMovementBuilding";
	static final String extraCardCost = "extraCardBuilding";
	static final String extraPointsCost = "extraPointsBuilding";
	static final String extraCubeCost = "extraCubeBuilding";
	// building spaces, ints are the index, cost in camels, cost in gold
	public enum ybuild
	{	extra_camel(0,2,0,extraCamelCost),
		extra_gold(1,2,2,extraGoldCost),
		extra_movement(2,2,2,extraMovementCost),
		extra_card(3,3,3,extraCardCost),
		extra_points(4,4,4,extraPointsCost),
		extra_cube(5,4,4,extraCubeCost);
		// constructor
		ybuild(int idx,int cost_camels,int cost_gold,String text)
		{	index = idx;
			helpText = text;
			camels = cost_camels;
			gold = cost_gold;
		}
		String helpText = null;
		static ybuild find(int n) 
		{ for(ybuild b : values())
			{ if(b.index==n) 
				{ return(b); 
				}
			}
		throw G.Error("value %d not found",n);
		}
	
		yclass type = yclass.playerCubes;
		int index;
		int camels;
		int gold;
	}
	
	// second row of the player card, contains gold camels, cards cubes, vps
	public enum ypmisc
	{	camel(0,yclass.camels,"#1 camels"),
		gold(1,yclass.gold,"#1 gold"),
		card(2,yclass.cards,"#1 cards"),
		cubes(3,yclass.playerCubes,"#1 cubes"),
		points(4,yclass.points,"#1 victory points");
		// constructor
		ypmisc(int ind,yclass tt,String text) { index = ind; type=tt; helpText = text;}
		static ypmisc find(int n) { for(ypmisc c : values()) { if(c.index==n) { return(c); }} return(null); }
		yclass type = null;
		String helpText = null;
		int index;
	}
	// dice selections
	public enum ydicetower {
		take_camels(0,"take #1 camels",ystate.THREEWAY_TAKE_CAMEL_STATE,ystate.TAKE_CAMEL_STATE,yrack.Camel_Pool),
		place_bag(1,"place #1 cubes in Bag neighborhood",ystate.THREEWAY_PLACE_BAG_STATE,ystate.PLACE_BAG_STATE,yrack.Bag_Neighborhood),
		place_barrel(2,"place #1 cubes in Barrel neighborhood",ystate.THREEWAY_PLACE_BARREL_STATE,ystate.PLACE_BARREL_STATE,yrack.Barrel_Neighborhood),
		place_chest(3,"place #1 cubes in Chest neighborhood",ystate.THREEWAY_PLACE_CHEST_STATE,ystate.PLACE_CHEST_STATE,yrack.Chest_Neighborhood),
		place_vase(4,"place #1 cubes in Vase neighborhood",ystate.THREEWAY_PLACE_VASE_STATE,ystate.PLACE_VASE_STATE,yrack.Vase_Neighborhood),
		take_gold(5,"take #1 gold",ystate.THREEWAY_TAKE_GOLD_STATE,ystate.TAKE_GOLD_STATE,yrack.Gold_Pool);
		// constructor
		ydicetower(int ind,String text,ystate st,ystate st2,yrack nn)
			{ helpText = text;
			  rackLocation = nn;
			  towerIndex = ind;
			  threewayState = st;
			  onewayState = st2;
			}
		static ydicetower find(int index)
		{	for(ydicetower v : values()) { if(v.towerIndex==index) { return(v); }}
			return(null);
		}
		ystate threewayState;
		ystate onewayState;
		yrack rackLocation;
		yclass type = yclass.dice;
		String helpText = null;
		int towerIndex  = 0;
		
	}
	class StateStack extends OStack <ystate>
	{
		public ystate[] newComponentArray(int n) { return(new ystate[n]); }
	}
	/* states of the board/game.  Because several gestures are needed to complete a move, and
    there are several types of move, we use a undoInfo machine to determine what is legal */
	enum ystate implements BoardState
	{	PUZZLE_STATE(false,PuzzleStateDescription),
		RESIGN_STATE(false,ResignStateDescription),
		GAMEOVER_STATE(false,GameOverStateDescription),
		CONFIRM_STATE(false,ConfirmStateDescription), // move and remove completed, ready to commit to it.
	    ROLL_STATE(false,"Add Extra Dice, or click on \"Roll\""), 	// roll the dice
	    SELECT_STATE(false,"Select a group of dice"),	// select a group of dice
	    THREEWAY_TAKE_CAMEL_STATE(true,"Take #1 camels, take a card, or move the supervisor #2 spaces"),	// three way selection of card, play, or move
	    THREEWAY_TAKE_GOLD_STATE(true,"Take #1 gold, take a card, or move the supervisor #2 spaces"),
	    THREEWAY_PLACE_BAG_STATE(true,"Place #1 cubes in Bag neighborhood, take a card, or move the supervisor #2 spaces"),
	    THREEWAY_PLACE_BARREL_STATE(true,"Place #1 cubes in Barrel neighborhood, take a card, or move the supervisor #2 spaces"),
	    THREEWAY_PLACE_CHEST_STATE(true,"Place #1 cubes in Chest neighborhood, take a card, or move the supervisor #2 spaces"),
	    THREEWAY_PLACE_VASE_STATE(true,"Place #1 cubes in Vase neighborhood, take a card, or move the supervisor #2 spaces"),
	    
	    TAKE_CAMEL_STATE(false,"Take #1 camels"),
	    TAKE_GOLD_STATE(false,"Take #1 gold"),
	    PLACE_BAG_STATE(false,"Place #1 cubes in Bag neighborhood"),
	    PLACE_BARREL_STATE(false,"Place #1 cubes in Barrel neighborhood"),
	    PLACE_CHEST_STATE(false,"Place #1 cubes in Chest neighborhood"),
	    PLACE_VASE_STATE(false,"Place #1 cubes in Vase neighborhood"),
	    TAKE_CARD_STATE(true,"Take a card"),
	    MOVE_SUPERVISOR_STATE(false,"Move the supervisor"),
	    PLAY_CARD_DICE_STATE(false,"Play a card"),
	    PLAY_CARD_EFFECT_STATE(false,"Play a card"),
	    PREBUILD_STATE(false,"Click on Done to begin the building phase"),
	    BUILD_STATE(false,"Build a building, play cards, or click on Done"),
	    
	    
	    CARD_PLACE_CUBE_CARAVAN(false,"Place a cube in the caravan"),
	    CARD_PLACE_CUBE_SOUK(false,"Place a cube in any unclaimed souk"),
	    CARD_TRADE_CAMELS_GOLD(false,"Trade camels for gold"),
	    CARD_SCORE_CAMELS(false,"Trade up to 4 camels for 2 victory points each"),
	    CARD_SCORE_GOLD(false,"Trade up to 10 gold for 1 victory point point each"),
	    CONFIRM_CARD_STATE(false,"Click on Done to confirm this card action"),
	    
	    DESIGNATE_CUBE_STATE(false,"Pick the first cube to send to the caravan"),
	    DESIGNATED_CUBE_STATE(false,"Click on Done to send cubes to the carvan"),
	    PAY_CAMEL_STATE(false,"Pay a camel or click Done to send the cube to the caravan"),
	    PAID_CAMEL_STATE(false,"Click on Done to confirm paying a camel"),
	    PASS_STATE(false,"Click on Done to pass")
		;
		// constructor
		ystate(boolean card,String msg) { stateMsg = msg; canTakeCard=card; }
		boolean canTakeCard = false;
		public boolean canFlingCard() { return(canTakeCard); }

		String stateMsg;
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
		public boolean GameOver() { return(this==GAMEOVER_STATE); }
	};
    static final String rowDesc[] = 
    	{
    	"Camels","Bag","Barrel","Chest","Vase","Gold"
    	};

    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
	static final int MOVE_VIEWCARDS = 211;		// view your card rack
	
	static void putStrings()
	{
	    String YspahanStrings[] = 
	        { "Yspahan",
	      	 ServiceName,
	      	 WarnCubeMessage,
	      	"Roll",
	      	"Receive 2 extra gold when you take gold",
	        	"Move the supervisor up to 3 extra spaces when you move the supervisor",
	        	"Receive a card when you place a cube in the caravan",
	        	"Receive 2 extra points for each completed souk",
	        	"Receive 1 extra cube when you place cubes in a souk",
	        	"This die will be rolled",
	        	"This die will not be rolled",
	        	"#1 Cards",
	        	"do what it takes to win",
	        	"Player Cubes",
	        	"#1 Gold",
	        	"#1 Camels",
	        	"Add Extra Dice, or click Done to Roll",
	        	"Select a group of dice",
	        	"Take #1 camels, take a card, or move the supervisor #2 spaces",
	        	"Take #1 gold, take a card, or move the supervisor #2 spaces",
	        	"Place #1 cubes in Bag neighborhood, take a card, or move the supervisor #2 spaces",
	        	"Place #1 cubes in Barrel neighborhood, take a card, or move the supervisor #2 spaces",
	        	"Place #1 cubes in Chest neighborhood, take a card, or move the supervisor #2 spaces",
	        	"Place #1 cubes in Vase neighborhood, take a card, or move the supervisor #2 spaces",
	         	"#1{##no cards, card, cards}",
	      	"#1{##no Camels, Camel, Camels}",
	      	"#1{##no Gold, Gold, Gold}",
	      	"#1{##no Victory points, Victory Point, Victory Points}", 	
	        	"Take #1 camels",
	        	"Take #1 gold",
	        	"Place #1 cubes in Bag neighborhood",
	        	"Place #1 cubes in Barrel neighborhood",
	        	"Place #1 cubes in Chest neighborhood",
	        	"Place #1 cubes in Vase neighborhood",
	        	"Take a card",
	        	"Move the supervisor",
	        	"Click on Done to begin the building phase",
	        	"Build a building, play cards, or click on Done",
	        	"Pay a camel or click Done to send the cube to the caravan",
	        	"Trade up to 10 gold for 1 victory point each",
	        	"Trade up to 4 camels for 2 victory points each",
	        	"Place a cube in any unclaimed souk",
	        	"Trade camels for gold",
	        	"Place a cube on the caravan",	
	        	"Buy a building with no gold",
	        	"Buy a building with no camels",
	        	"Receive 3 gold",
	        	"Receive 3 camels",
	        	"Play a card",
	        	"Click on Done to confirm this card action",
	        	"Select Row #1",
	        	"Cubes",
	        	"Camels",
	        	"Gold",
	        	"Cards",
	        	"Table",
	        	"Card",
	  		"Buildings",
	  		"Vase",
	  		"Barrel",
	  		"Chest",
	  		"Dice",
	  		"Click on Done to confirm paying a camel",
	  		"Click on Done to send cubes to the carvan",
	  		"Pick the first cube to send to the caravan",
	  		"Traded #1 camels become gold",
	  		"Bag",
	  		"Discards",
	  		"Add die",
	  		"Played",
	  		"Traded: #1 gold become camels",
	  		"Traded: #1 camels for #2 points",
	  		"Traded: #1 gold for #2 points",
	  		"Game Over!  Final scores ",
	  		"Pay Gold",
	  		"Last Turn before Scoring",
	  		"Last Turn",
	  		"Pay Camel",
	  		"Pay Camels",
	  		"Supervisor",
	  		"Hoist",
	  		"Plus2",
	  		"Select #1",
	  		"+1 die",
	  		"Send Cube",
	       };
	  String [][] YspahanStringPairs = {
	  	   	{"Yspahan_family","Yspahan"},
	  	   	{"Yspahan_variation","standard Yspahan"},
	  		{extraCamelCost, "Receive 1 extra camel when you take camels\nCost: 2 Camels"},
	  		{extraGoldCost,"Receive 2 extra gold when you take gold\nCost: 2 Camels + 2 Gold"},
	  		{extraMovementCost,"Move the supervisor up to 3 extra spaces when you move the supervisor\nCost: 2 Camels + 2 Gold"},
	  		{extraCardCost,"Receive a card when you place a cube in the caravan\nCost: 3 Camels + 3 Gold"},
	  		{extraPointsCost,"Receive 2 extra points for each completed souk\nCost: 4 Camels + 4 Gold"},
	  		{extraCubeCost,"Receive 1 extra cube when you place cubes in a souk\nCost: 4 Camels + 4 Gold"},
	  };
	  	InternationalStrings.put(YspahanStrings);
	  	InternationalStrings.put(YspahanStringPairs);
	}
}
