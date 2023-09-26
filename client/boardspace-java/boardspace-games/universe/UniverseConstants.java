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
package universe;

import lib.CellId;
import lib.OStack;
import online.game.BaseBoard.BoardState;

public interface UniverseConstants 
{	static final String NormalPlayDescription = "Place a piece on the board";
	static final String PlayOrSwapDescription = "Place a piece on the board, or swap colors with your opponent";
	static public String[] PolyominoStrings =
	{        
        "Polyomino Flip",
        "make the last move",
        "own the most blocks",
        NormalPlayDescription,
        PlayOrSwapDescription,
        "Pan-Kai",
        "Universe",
        "Diagonal Blocks Duo",
        "Diagonal Blocks",
        "Phlip",

	};
	static public String[][] PolyominoStringPairs = 
		{
		{"Pan-Kai_family","Pan-Kai"},
        {"Universe_family","Universe"},
        {"Polyomino Flip_variation","Polyomino Flip"},
        {"Phlip_family","Phlip"},
        {"Pan-Kai_variation","2 players"},
        {"Universe_variation","3-4 players"},
        {"Phlip_variation","Standard Phlip"},
		{"Diagonal Blocks_family","Diagonal Blocks"},
		{"Diagonal Blocks Duo_family","Diagonal Blocks Duo"},
        {"Diagonal-Blocks-Duo","Diagonal Blocks Duo"},
        {"Diagonal-Blocks-Duo_variation","2 players"},
        {"Diagonal-Blocks","Diagonal Blocks"},
        {"Diagonal-Blocks_variation","3-4 players"},

		};
	enum FlipStyle { allowed, notallowed };
	enum TweenStyle { none, smudge, barbell };
	enum variation {
		Universe("universe",OminoStep.PENTOMINOES),							// 3-4 player pentomino game
		Pan_Kai("pan-kai",OminoStep.PENTOMINOES),							// 2 player pentomino game
		Pentominoes("pentominoes",OminoStep.PENTOMINOES),					// 1 player pentomino puzzle
		Phlip("phlip",OminoStep.PHLIPSET),									// 2 player reversi-type game
		Diagonal_Blocks("diagonal-blocks",OminoStep.FULLSET),			// 4 player blokus clone
		Diagonal_Blocks_Classic("diagonal-blocks-classic",OminoStep.FULLSET),	// 2 player 4 color diagonal blocks.
		Diagonal_Blocks_Duo("diagonal-blocks-duo",OminoStep.FULLSET),	// 2 player blokus clone
		Blokus_Duo("blokus-duo",OminoStep.FULLSET),	// 2 player blokus clone
		Blokus("blokus",OminoStep.FULLSET),		// regular 4 player blokus
		Blokus_Classic("blokus-classic",OminoStep.FULLSET),	// regular blokus, 2 player variant
		PolySolver_9x9("polysolver9x9",OminoStep.FULLSET),				// polyomino solver using blokus pieces 9x9
		PolySolver_8x8("polysolver8x8",OminoStep.CLASSIC_PENTOMINO_SOLVER),	// polyomino solver pentominoes plus block 4
		PolySolver_6x6("polysolver6x6",OminoStep.FULLSET),				// polyomino solver using blokus pieces 6x6
		Nudoku_6x6("nudoku6x6",OminoStep.FULLSET),					// nudoku for 9x9
		Nudoku_8x8("nudoku8x8",OminoStep.FULLSET),					// nudoku for 8x8
		Nudoku_9x9("nudoku9x9",OminoStep.FULLSET),					// nudoku for 6x6
		Nudoku_12("nudoku-12",OminoStep.SNAKES),			// snakes nudoku puzzle # 12
		Nudoku_11("nudoku-11",OminoStep.SNAKES),			// snakes nudoku puzzle # 11
		Nudoku_10("nudoku-10",OminoStep.SNAKES),			// snakes nudoku puzzle # 10
		Nudoku_9("nudoku-9",OminoStep.SNAKES),				// snakes nudoku puzzle # 9
		Nudoku_8("nudoku-8",OminoStep.SNAKES),				// snakes nudoku puzzle # 8
		Nudoku_7("nudoku-7",OminoStep.SNAKES),				// snakes nudoku puzzle # 7
		Nudoku_6("nudoku-6",OminoStep.SNAKES),				// snakes nudoku puzzle # 6
		Nudoku_5("nudoku-5",OminoStep.SNAKES),				// snakes nudoku puzzle # 5
		Nudoku_4("nudoku-4",OminoStep.SNAKES),				// snakes nudoku puzzle # 4
		Nudoku_3("nudoku-3",OminoStep.SNAKES),				// snakes nudoku puzzle # 3
		Nudoku_2("nudoku-2",OminoStep.SNAKES),				// snakes nudoku puzzle # 2
		Nudoku_1("nudoku-1",OminoStep.SNAKES),				// snakes nudoku puzzle # 1
		Sevens_7("sevens-7",OminoStep.FULLSET),				// sevens puzzle
		Nudoku_1_Box("Nudoku-1-Box",OminoStep.SNAKES),		// nudoku puzzles 1 - 2
		Nudoku_2_Box("Nudoku-2-Box",OminoStep.SNAKES),		// nudoku puzzles 3 - 8
		Nudoku_3_Box("Nudoku-3-Box",OminoStep.SNAKES),		// nudoku puzzles 9-15
		Nudoku_4_Box("Nudoku-4-Box",OminoStep.SNAKES),		// nudoku puzzles 9-15
		Nudoku_5_Box("Nudoku-5-Box",OminoStep.SNAKES),
		Nudoku_6_Box("Nudoku-6-Box",OminoStep.SNAKES);
		String name;
		OminoStep tileSet[][];
		variation(String sn,OminoStep[][]set)
		{ name = sn;
		  tileSet = set;
		}
		public boolean passIsPermanent()
		{
			switch(this)
			{
			case Phlip:
			case Blokus:
			case Blokus_Duo:
			case Diagonal_Blocks:
			case Diagonal_Blocks_Classic:
			case Diagonal_Blocks_Duo:
			case Pan_Kai:
			case Universe: 
					return(true);
			default:
					return(false);
			}
		}
		public int numberOfShapes() { return(tileSet.length); }
		public boolean isNudoku()
		{
			return(name.toLowerCase().startsWith("nudo") || name.startsWith("seven"));
		}
		static variation find(String ss)
		{	
			for(variation v : values())
			{
			if(ss.equalsIgnoreCase(v.name)) { return(v); }
			}
			return(null);
		}
	};
		
	static char GIVENS_COLUMN = 'Y';
	static char TAKENS_COLUMN = 'Z';
	
    enum UniverseId implements CellId
    {
    	ChipRack,
    	BoardLocation,	// 
    	PickedCell,		// the chip "in the air"
    	RotateCW,		// rotate the cell
    	RotateCCW,		// rotate the other way
    	FlipCell,		// flip vertically
    	RotateNothing,	// hit the rotator but missed
    	GeneratePuzzle,	// generate a new puzzle
    	PatternLocation, // choose new pattern
    	GivensRack,		// numbers given in the current puzzle
    	TakensRack,		// polys taken out of the current puzzle
    	GivenOnBoard,;
    	public String shortName() { return(name()); }
   }
    class StateStack extends OStack<UniverseState>
    {
		public UniverseState[] newComponentArray(int sz) {
			return new UniverseState[sz];
		}
    	
    }
    public enum UniverseState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	PLAY_STATE(NormalPlayDescription),
    	PASS_STATE(PassStateDescription),
    	PLAY_OR_SWAP_STATE(PlayOrSwapDescription),
    	CONFIRM_SWAP_STATE(ConfirmSwapDescription);
    	
    	String description;
    	UniverseState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }
    
    // 4 player universe
	static final int universe_n_in_col[] = 		{ 5, 10, 10, 14, 14, 15, 16, 16, 16, 16, 15, 14, 14, 10, 10, 5 };
	static final int universe_first_in_col[] = 	{ 7,  4,  4,  2,  2,  1,  1,  1,  1,  1,  2,  2,  2,  4,  4, 6 };
	// 3 player universe
	static final int universe_3n_in_col[] = 		{ 5, 10, 10, 12, 12, 13, 13, 13, 13, 13, 12, 12, 12};
	static final int universe_3first_in_col[] = 	{ 7,  4,  4,  2,  2,  1,  1,  1,  1,  1,  2,  2,  2};
	
	static final int diagonal_blocks_duo_n_in_col[] = { 10, 12, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 12, 10};
	static final int diagonal_blocks_duo_first_in_col [] = { 3, 2, 1, 1, 1, 1, 1,  1, 1, 1, 1, 1, 2 , 3};

	static final int diagonal_blocks_3_n_in_col[] = { 13, 15, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 15, 13};
	static final int diagonal_blocks_3_first_in_col [] = { 3, 2, 1, 1, 1, 1, 1, 1, 1, 1,  1, 1, 1, 1, 1, 2 , 3};

	static final int diagonal_blocks_4_n_in_col[] = { 14, 16, 18,  20, 20, 20, 20,   20, 20, 20, 20, 20,  20, 20, 20, 20, 20,  18, 16, 14};
	static final int diagonal_blocks_4_first_in_col [] = { 4, 3, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1,  1, 1, 1, 1, 1, 2 , 3, 4};

    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_RACK_BOARD = 209;	// move from rack to board
	static final int MOVE_ROTATE_CW = 211;	// rotate the cell to the next position
	static final int MOVE_ROTATE_CCW = 212;	// rotate the cell to the next position
	static final int MOVE_FLIP = 213;	// rotate the cell to the next position
	static final int MOVE_UNPICK = 214;	// drop it
	static final int MOVE_ASSIGN = 215;	// assign sudoku values
	static final int MOVE_LINK = 216;	// link two cells
	static final int MOVE_PICKGIVEN = 217;
	static final int MOVE_GAMEOVER = 218;
	static final int MOVE_ALLDONE = 219;
    static final String Universe_SGF = "Universe"; // sgf game name
    static final String[] UNIVERSEGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

 
    // file names for jpeg images and masks
    static final String ImageDir = "/universe/images/";
	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int ICON_INDEX = 2;
    public enum diagonalSweepResult 
    { none, start, adjacent, diagonal };
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  "universe-icon-nomask"};
    static double[][] ArtScales = {{0.479,0.479,1.4}};
    static int GRAY_INDEX = 0;
    static int DARKGRAY_INDEX = 1;
    static int DARKERGRAY_INDEX = 2;
    static int LIGHTGRAY_INDEX = 3;
    static int ROTATECW_INDEX = LIGHTGRAY_INDEX+1;
    static int ROTATECCW_INDEX = ROTATECW_INDEX+1;
    static int FLIP_INDEX = ROTATECCW_INDEX+1;
    static final String ArtNames[] = { "gray","darkgray","darkergray","lightgray","rotatecw","rotateccw","flip" };
 

}