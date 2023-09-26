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
package tablut;

import lib.G;
import lib.CellId;

import online.game.BaseBoard.BoardState;


public interface TabConstants 
{   //	these next must be unique integers in the Tabmovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	static String GoalString = "gold: escape the flagship  silver: capture gold flagship";
	static String RearrangeGoldDescription = "Rearrange the gold fleet";
	static String RearrangeSilverDescription = "Rearrange the silver fleet";
	static String MoveFirstDescription = "Make your first move, or swap fleets";
	static String ConfirmFirstDescription = "Click Done to confirm swapping fleets with your opponent";
	static String MoveShipDescription = "Move one ship";
	static String TablutStrings[] = 
	{ 	"Tablut",
		GoalString,
		RearrangeGoldDescription,
        RearrangeSilverDescription,
        MoveFirstDescription,
        ConfirmFirstDescription,
        MoveShipDescription,
        "Flagship must Reach the corner to win",
        "Flagship wins at any edge square",
        "Flagship can participate in captures",
        "Flagship can not capture",
        "Only the flagship can occupy the center square",
        "center square is not special",
        "Flagship must be surrounded on all 4 sides",
        "Flagship is captured normally",
        "Make the first move, or swap and move second",
        "Position the starting rings",

	};
	static String TablutStringPairs[][] =
	{
        {"Tablut_family","Tablut"},
        {"Tablut-7","Tablut 7x7"},
        {"Tablut-9","Tablut 9x9"},
        {"Tablut-11","Tablut-11x11"},
        {"Tablut-7_variation","7x7 board"},
        {"Tablut-9_variation","9x9 board"},
        {"Tablut-11_variation","11x11 board"},	
	};
	
	enum TabId implements CellId
	{
		EmptyBoardLocation(null,null,null),
    	BoardLocation(null,null,null),
    	SilverShipLocation("S",null,null),
    	GoldShipLocation("G",null,null),
    	GoldFlagLocation("F",null,null),
    	CornerWin("corner-win","Flagship must Reach the corner to win","Flagship wins at any edge square"),
    	FlagShipCaptures("flagship-captures","Flagship can participate in captures","Flagship can not capture"),
    	ExclusiveCenter("exclusive-center","Only the flagship can occupy the center square","center square is not special"),
		FourSideCaptures("fourside-capture","Flagship must be surrounded on all 4 sides","Flagship is captured normally"),
		True("true",null,null),
		False("false",null,null);
   	;
   	String shortName = name();
	public String shortName() { return(shortName); }
   	String trueName = null;
   	String falseName = null;
   	TabId(String sn,String tr,String fa) 
   		{ if(sn!=null) { shortName = sn; }
   		  trueName = tr;
   		  falseName = fa;
   		}
	static public TabId find(String s)
	{	
		for(TabId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public TabId get(String s)
	{	TabId v = find(s);
		G.Assert(v!=null,IdNotFoundError,s);
		return(v);
	}

	}
	public TabId optionNames[] = { TabId.CornerWin,TabId.FlagShipCaptures,TabId.ExclusiveCenter,TabId.FourSideCaptures};
	
	// other things you can point at.  Negative numbers are fixed objects such as buttons
    // positive numbers are movable objects you pick up and drag.  There are also some
    // shared object such as HitNoWhere
	static final int OptionsButton = -40;
 
  
    // init strings for variations of the game.
    static final String Tablut_11_INIT = "tablut-11"; //init for standard game
    static final String Tablut_9_INIT = "tablut-9";
    static final String Tablut_7_INIT = "tablut-7";
    static final String Default_Tablut_Game = "tablut-9";
    static final String ENDOPTIONS = ".endoptions.";
    public enum TablutState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	REARRANGE_GOLD_STATE(RearrangeGoldDescription),
    	REARRANGE_SILVER_STATE(RearrangeSilverDescription),
    	PLAY_OR_SWAP_STATE(MoveFirstDescription),
    	CONFIRM_SWAP_STATE(ConfirmFirstDescription),
    	PLAY_STATE(MoveShipDescription),
    	DRAW_STATE(DrawStateDescription),
    	CONFIRM_NOSWAP_STATE(ConfirmStateDescription);
    	String description;
    	TablutState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }


	
	// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
	static final int MOVE_SETOPTION = 209;
	static final int MOVE_MOVE = 210;
	
    static final String Tablut_SGF = "29"; // sgf game name
    static final String[] TABGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

  
    // file names for jpeg images and masks
    static final String ImageDir = "/tablut/images/";


    //to keep the artwork aquisition problem as simple as possible, images
    //are recentered and scaled on the fly before presentation.  These arrays
    //are X,Y,SCALE factors to standardize the size and center of the images
    //in the development environment, use "show aux sliders" to pop up sliders
    //which adjust the position and scale of displayed elements.  Copy the numbers
    //back into these arrays and save.  Due to flakeyness in eclipse, it's not very
    //reliable to save and have the result be replaced in the running applet, but
    //this is only a one time thing in development.
    //
   
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int ICON_INDEX =2;
    static final String TextureNames[] = 
    	{ "background-tile" ,"background-review-tile",
    			"tablut-icon-nomask"};
    
    		

}