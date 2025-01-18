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
package che;

import lib.OStack;
import lib.CellId;
import lib.InternationalStrings;
import online.game.BaseBoard.BoardState;


public interface CheConstants 
{	
    //	these next must be unique integers in the dictionary
	//  they represent places you can click to pick up or drop a stone
	static final String GoalMessage = "Form a closed shape in your color";
	static final String FirstMoveMessage = "Place a tile on any empty cell";
	static final String SecondMoveMessage = "Place a tile adjacent to those already on the board";
	static final String ThirdMoveMessage = "Place a second tile adjacent to those already on the board";


	enum CheId implements CellId
	{
    	ChipPool0, // positive numbers are trackable
    	ChipPool1,
    	ChipPool2,
    	ChipPool3,
    	BoardLocation,
    	EmptyBoard,
    	InvisibleDragBoard,
    	ZoomSlider,
    	RotateTile;
	}
	
    static CheId ChipPool[] = { CheId.ChipPool0,CheId.ChipPool1,CheId.ChipPool2,CheId.ChipPool3};
    // init strings for variations of the game.
    static final String Che_INIT = "che"; //init for standard game
 
    /* the "external representation for the board is A1 B2 etc.  This internal representation is X,Y
       where adjacent X's are separated by 2.  This gives the board nice mathematical properties for
       calculating adjacency and connectivity. */
	class StateStack extends OStack<CheState>
	{
		public CheState[] newComponentArray(int n) { return(new CheState[n]); }
	}

 
    public enum CheState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
        FIRST_PLAY_STATE(FirstMoveMessage),	// the first move
        PLAY_STATE(SecondMoveMessage), // place a marker on the board
        PLAY2_STATE(ThirdMoveMessage);	// place a second piece;
    	String description;
    	CheState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } 
    }	
    // move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_ROTATE = 208;	// rotate a tile in place
 		
	static final int CHIPS_IN_GAME = 64;

 
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
 
    
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int BACKGROUND_TABLE_INDEX = 2;
    static final int LIGHT_SAMPLE_INDEX = 3;
    static final int DARK_SAMPLE_INDEX = 4;
    static final int PlayerSamples[] = {LIGHT_SAMPLE_INDEX ,  DARK_SAMPLE_INDEX};
    static final String TextureNames[] = 	// these are simple square images, with no mask
    { "background-tile" ,
    	"background-review-tile", 
    	"green-felt-tile",
    	"light-sample",
    	"dark-sample"};
    
    
    static void putStrings()
    {
    	String CheStrings[] = {
    			"Che",
    			GoalMessage,
    			FirstMoveMessage,
    			SecondMoveMessage,
    			ThirdMoveMessage,			
    	};
    	String CheStringPairs[][] = {
    			{"Che_family","Che"},
    			{"Che_variation","standard Che"},       
    	};
    	InternationalStrings.put(CheStrings);
    	InternationalStrings.put(CheStringPairs);
    }


}