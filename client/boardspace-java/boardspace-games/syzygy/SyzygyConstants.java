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
package syzygy;

import lib.CellId;
import lib.InternationalStrings;
import lib.OStack;

import online.game.BaseBoard.BoardState;


public interface SyzygyConstants 
{   enum SyzId implements CellId
    {
    	BoardLocation ,
    	InvisibleDragBoard,
    	ZoomSlider,
    	ChipPool, // positive numbers are trackable plus n for chip
    	PlayerChip,
    	CrawlCell,
    	CHIP_OFFSET,;
  }
    // init strings for variations of the game.
     /* the "external representation for the board is A1 B2 etc.  This internal representation is X,Y
       where adjacent X's are separated by 2.  This gives the board nice mathematical properties for
       calculating adjacency and connectivity. */
    static int[] ZfirstInCol19 = { 18, 17, 16, 15, 14,13,12,11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 }; // these are indexes into the first ball in a column, ie B1 has index 2
    static int[] ZnInCol19 =     { 19,19,19,19,19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19 }; // depth of columns, ie A has 4, B 5 etc.

    class StateStack extends OStack<SyzygyState>
    {
		public SyzygyState[] newComponentArray(int sz) {
			return new SyzygyState[sz];
		}
    	
    }
    public enum SyzygyState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
        PLAY_STATE("Move a Planet"),
        DRAW_STATE(DrawStateDescription);
    	String description;
    	SyzygyState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }

	// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_FROM_TO = 208;	// move a chip
 		
    //
    // basic image strategy is to use jpg format because it is compact.
    // .. but since jpg doesn't support transparency, we have to create
    // composite images wiht transparency from two matching images.
    // the masks are used to give images soft edges and shadows
    //
    
    //to keep the artwork aquisition problem as simple as possible, images
    //are recentered and scaled on the fly before presentation.  These arrays
    //are X,Y,SCALE factors to standardize the size and center of the images
    //in the development environment, use "show aux sliders" to pop up sliders
    //which adjust the position and scale of displayed elements.  Copy the numbers
    //back into these arrays and save.  Due to flakeyness in eclipse, it's not very
    //reliable to save and have the result be replaced in the running applet, but
    //this is only a one time thing in development.
    //
    //ad hoc scale factors to fit the stones to the board
    static final String GoalString = "Form a Syzygy - three planets in a row";
    static void putStrings()
    {
    	 	 String[] SyzygyStrings = {
	    		"Syzygy",
	    		"Move a Planet",
	    		GoalString,	    		
	    	 	 };
    	 	 String[][] SyzygyStringPairs = {
		    	{"Syzygy_variation","Standard Syzygy"},
		    	{"Syzygy_variation","Standard Syzygy"},
		    	{"Syzygy_family","Syzygy"},
		    };
    	 	 InternationalStrings.put(SyzygyStrings);
    	 	 InternationalStrings.put(SyzygyStringPairs);
    }

}