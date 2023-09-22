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
package gounki;

import lib.G;
import lib.OStack;
import lib.CellId;

import online.game.BaseBoard.BoardState;

public interface GounkiConstants 
{
	static final String Gounki_INIT = "gounki";	//init for standard game

	static final String GoalMessage = "move a piece to the opposite side of the board";
	static final String MoveOrPlaceDescription = "Move a Stack or Start deploying at Stack";
	static final String DeployOrDoneDescription = "Deploy the stack, or click on Done";
	static final String ContinueDeployDescription = "Continue deploying the stack";
	
    //	these next must be unique integers in the dictionary
	enum GounkiId implements CellId
	{
    	Black_Chip_Pool("B"), // positive numbers are trackable
    	White_Chip_Pool("W"),    
    	BoardLocation(null),
    	ReverseViewButton(null),
    	TwistViewButton(null),;
    	String shortName = name();
    	public String shortName() { return(shortName); }
    	GounkiId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public GounkiId find(String s)
    	{	
    		for(GounkiId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public GounkiId get(String s)
    	{	GounkiId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}
	}
	class StateStack extends OStack<GounkiState> 
	{
		public GounkiState[] newComponentArray(int sz) { return(new GounkiState[sz]); }
	}

    public enum GounkiState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	DRAW_STATE(DrawStateDescription),	// game is a draw, click to confirm
    	PLAY_STATE(MoveOrPlaceDescription), 	// place a marker on the board
    	DEPLOY_STATE(DeployOrDoneDescription),	// optional deploy instead of move
    	DEPLOY2_STATE(ContinueDeployDescription),	// optionsl deploy the third chip in a stack
    	DEPLOY_ONLY_STATE(ContinueDeployDescription);	// mandatory start deployment

    	;
    	String description;
    	GounkiState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }
    
	
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_DEPLOY = 209;	// deploy a stack
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
    static final int MOVE_DEPLOYSTEP = 211;		// second or third step in a deploy
	

 
    
    static void putStrings()
    { /*
    		String GounkiStrings[] = {
    			"Gounki",
    			GoalMessage,
    			MoveOrPlaceDescription,
    			DeployOrDoneDescription,
    			ContinueDeployDescription,
    	};
    		String GounkiStringPairs[][] = {
    			{"Gounki_variation","standard Gounki"},
    			{"Gounki_family","Gounki"},
    	  };
    		InternationalStrings.put(GounkiStrings);
    		InternationalStrings.put(GounkiStringPairs);
	*/
    }

}