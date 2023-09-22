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
package knockabout;

import lib.CellId;

import online.game.BaseBoard.BoardState;
public interface KnockaboutConstants 
{	static final int NPIECES = 9;
	static final int MAXDESTS = (NPIECES*6+1);
	static final String Knockabout_Standard_Init = "knockabout";	// standardized init
    
	static final String GoalMessage = "Knock 5 of your opponent's dice into the gutter";
	static final String KnockaboutStrings[] = {
			"Knockabout",
			GoalMessage,
			"Move a piece",
	};
	static final String KnockaboutStringPairs[][] = {
			{"Knockabout_family","Knockabout"},
			{"Knockabout_variation","standard Knockabout"},
	};
	
	enum KnockId implements CellId
	{
    	BoardLocation,
    	HitPlusCode,
    	WhiteDie,BlackDie,
    	HitMinusCode;
		public String shortName() { return(name()); }
	}
    // indexes into the balls array, usually called the rack
    static final int NUMPLAYERS = 2;				// maybe someday...
 
   
     /* the "external representation for the board is A1 B2 etc.  
      * these magic numbers define a simple hexagonal board with 7 cells per side
      */
    static int[] ZfirstInCol = { 6, 5, 4, 3, 2, 1, 0, 1, 2, 3, 4, 5, 6 }; // these are indexes into the first ball in a column, ie B1 has index 2
    static int[] ZnInCol = {     7, 8, 9,10,11,12,13,12,11,10, 9, 8, 7 }; // depth of columns, ie A has 4, B 5 etc.
 

                                          
    public enum KnockaboutState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	DRAW_STATE(DrawStateDescription),
    	PLAY_STATE("Move a piece");
    	String description;
    	KnockaboutState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }
     

	
	// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_BOARD_BOARD = 209;	// move board to board
    static final int MOVE_ROLL = 210;	// capturing move
	
    static final String Knockabout_SGF = "Knockabout"; // sgf game name
    static final String[] KNOXKABOUTGRIDSTYLE = { null, "A1", "A1" }; // top and bottom numbers
    // file names for jpeg images and masks
    static final String ImageDir = "/knockabout/images/";

    static final int BOARD_INDEX = 0;
    static final int PLUS1_INDEX = 1;
    static final int MINUS1_INDEX = 2;
    static final String[] ImageFileNames = 
        {
    	"board",
    	"plusone",
    	"minusone"
        };
                 
	double SCALES[][] = 
	{{0.5,0.5,0.98},			// square board
	{0.5,0.5,1.0},
	{0.5,0.5,1.0}
	
	};
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile"};
}