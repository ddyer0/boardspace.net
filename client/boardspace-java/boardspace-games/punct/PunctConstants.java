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
package punct;

import lib.G;
import lib.CellId;

import online.game.BaseBoard.BoardState;


public interface PunctConstants 
{   //	these next must be unique integers in the dictionary
	static final int POOL_LEVEL=-1;		// piece located in the chip pool
	static final int TRANSIT_LEVEL=-2;	// piece being dragged around
	
	// these are "hit codes" in the UI
	enum PunctId implements CellId
	{
    	Black_Chip_Pool("B"), // positive numbers are trackable
    	White_Chip_Pool("W"),	// 
    	Chip_Pool(null),		// generic pool
    	BoardLocation("Board"),
    	EmptyBoard(null),
    	RotatePieceCW(null),
    	RotatePieceCCW(null),
    	HitOtherPiece(null),
    	;
   	String shortName = name();
	public String shortName() { return(shortName); }
   	PunctId(String sn) { if(sn!=null) { shortName = sn; }}
	static public PunctId find(String s)
	{
		for(PunctId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public PunctId get(String s)
	{	PunctId v = find(s);
		G.Assert(v!=null,IdNotFoundError,s);
		return(v);
	}
	}
	public enum PunctColor { White,Black};
	
    public static PunctId[] chipPoolIndex = { PunctId.White_Chip_Pool, PunctId.Black_Chip_Pool };

    static final int MAXHEIGHT = 24;		// max height the stack can reach
    static final int NUMPIECETYPES = 6;		// T E C L M R
    static final int NUMPIECES = 19;		// number of pieces per player, including punct
    static final int NUMREALPIECES=18;		// number excluding the punct
    static final int WINNING_SPAN = 16;		// number of rows etc.
     
    /* characters which identify the contents of a board position */
    static final char White = 'w'; // a white ball
    static final char Black = 'B'; // a black ball
    static final String Punct_init = "punct"; //init for standard game


    /* the "external representation for the board is A1 B2 etc.  This internal representation is X,Y
       where adjacent X's are separated by 2.  This gives the board nice mathematical properties for
       calculating adjacency and connectivity. */
    static int[] ZfirstInCol = { 9,  6,  5,  4,  3,  2,  1,  0,  1,  0, 1, 2, 3, 4, 5, 6, 9  }; // these are indexes into the first ball in a column, ie B1 has index 2
    static int[] ZnInCol =   {   7, 10, 11, 12, 13, 14, 15, 16, 15, 16,15,14,13,12,11,10, 7 }; // depth of columns, ie A has 4, B 5 etc.
    static int[] ZfirstCol = {   1,  0,  0,  0,  0,  0,  0,  0,  1,  1, 2, 3, 4, 5, 6, 7, 9 }; // number of the first visible column in this row, 

    public enum PunctState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	PLAY_STATE("Place a piece on the board, or move a piece already on the board"),
    	DRAW_STATE(DrawStateDescription);
    	String description;
    	PunctState(String des) { description = des; }
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
   static final int MOVE_ROTATE = 209; // rotate a piece that's in the air
	static final int MOVE_MOVE = 210;	// move from board to board
	
    static final String Punct_SGF = "25"; // sgf game number allocated for punct
    static final String[] PUNCTGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

	static double hextile_scale[] = { 0.5,0.40,3.0 };
	static double recycle_scale[] = { 0.5,0.5,1.5};
	
    static final double dot_scale[][][] =	// placing the dot at the rotation center of any piece
    { {{0.5771,0.062857,0.66}},	// white
      {{0.5771,0.09142,0.7}}}; 	// black
    
    static final double punct_scale[][][] =	// the punct piece
    { {{0.57,0.21,1.1}},	// white
        {{0.55,0.16,1.3}}}; 	// black
    
	// these offsets describe where the other dots fall, relative to the punct dot
    // for a piece in rotation 0 as depicted in our tile set
    static final int tri_offset[][][] = 			// triangular piece
    { {{0,-1},{-1,-1},{-1,0},{0,1},{1,1},{1,0}},	//col,row offset to the second dot
      {{-1,-1},{-1,0},{0,1},{1,1},{1,0},{0,-1}}};   //col,row offset to the third dot

    static final double tri_scale[][][] =
    {{{0.67777,		0.1722,		2.488},			// white rotation 0
   	  {0.677551,	0.3183673,	2.4325},		// white rotation 1
   	  {0.685714,	0.587755,	2.488},			// white rotation 2
  	  {0.36190,		0.54285,	2.4325},		// white rotation 3
   	  {0.379591,	0.3877,		2.449230},		// white rotation 4
  	  {0.35714,		0.1238,		2.4325}			// white rotation 5
    	},	
   	{{0.688,	0.18,	2.1208},		// black rotation 0
     {0.704,	0.344,	2.39200},		// black rotation 1
     {0.704,	0.592,	2.1208},		// black rotation 2
     {0.396,	0.544,	2.39200},		// black rotation 3
     {0.3909,	0.40909,2.1208},		// black rotation 4
     {00.375,	0.16666,2.39200}}};		// black rotation 5
    
    static final int straight_offset_c[][][] = 		// straight with dot in the center
    { {{0,-1},{-1,-1},{-1,0},{0,1},{1,1},{1,0}},	//col,row offset to the second dot
      {{0,1},{1,1},{1,0},{0,-1},{-1,-1},{-1,0}}};	//col,row offset to the third dot
     
       static final double straight_scale_c[][][] =
    {{{0.588,	0.38775,	1.576},		// white rotation 0
      {0.5257,	0.37142,	2.9885},	// white rotation 1
      {0.5047,	0.3285,		3.2619},	// white rotation 2
      {0.588,	0.376,		1.576},		// white rotation 3
      {0.5257,	0.37142,	2.9885},	// white rotation 4
      {0.5047,	0.3285,		3.2619}},	// white rotation 5
    {{0.6,		0.37,	1.74400},		// black rotation 0
     {0.536,	0.36,	2.84},		// black rotation 1
     {0.544,	0.376,	2.83},	// black rotation 2
     {0.6,		0.37,	1.74400},		// black rotation 3
     {0.536,	0.36,	2.84},		// black rotation 4
     {0.544,	0.376,	2.83}}};	// black rotation 5 
       
    static final int straight_offset_t[][][] = 	// straight with dot at the top
       { {{0,-1},{-1,-1},{-1,0},{0,1},{1,1},{1,0}},		//col,row positions of second dot
         {{0,-2},{-2,-2},{-2,0},{0,2},{2,2},{2,0}}};	//col,row positions of third dot


    static final double straight_scale_t[][][] =
    {{
     {0.5959,	0.11428,	1.6428},	// white rotation 0
   	 {0.7674,	0.2,		3.1046},	// white rotation 1
     {0.7346,	0.53469,	3.5},		// white rotation 2
     {0.5953,	0.675,		1.6428},	// white rotation 3
     {0.28837,	0.56279,	3.1046},	// white rotation 4
     {0.290909,	0.16363,	3.5}},		// white rotation 5
   	{{0.58,		0.080851,	1.74400},	// black rotation 0
     {0.775,	0.191666,	2.84},		// black rotation 1
     {0.8083,	0.5583,		2.83},		// black rotation 2
     {0.59583,	0.704545,	1.74400},	// black rotation 3
     {0.24186,	0.57674,	2.84},		// black rotation 4
     {0.2625,	0.1916,	2.83}}};		// black rotation 5   
     
     
     
     static final int y_offset_t[][][] = 	// y with dot on the top
     { {{0,-1},{-1,-1},{-1,0},{0,1},{1,1},{1,0}},		//col,row position of the near dot
       {{-1,-2},{-2,-1},{-1,1},{1,2},{2,1},{1,-1}}};	//col,row position of the far dot
    static final double y_scale_t[][][] =
    {{{0.7,		0.0625,		1.98333},	// white rotation 0
   	 {0.8045,	0.26818,	2.75},		// white rotation 1
     {0.7375,	0.6375,		2.204},		// white rotation 2
     {0.43414,	0.64878,	2.6829},	// white rotation 3
     {0.2780,	0.47804,	3.0609},	// white rotation 4
     {0.3463,	0.14146,	2.8902}},	// white rotation 5
   	{{0.72083,	0.1,		2.1},		// black rotation 0
     {0.756521,	0.2608,		3.0978},	// black rotation 1
     {0.7565,	0.62173,	2.05434},	// black rotation 2
     {0.3869,	0.5913,		2.2173},	// black rotation 3
     {0.24347,	0.4043,		2.61956},	// black rotation 4
     {0.3173,	0.1478,		2.2717}}};	// black rotation 5 
    
    static final int y_offset_c[][][] = 			// y with dot in the middle
    { {{0,1},{-1,0},{1,0},{0,-1},{-1,-1},{-1,0}},	//col,row position of the trailing dot
      {{-1,-1},{1,1},{0,1},{1,1},{1,0},{0,-1}}};	//col,row position of the leading dot
     
    static final double y_scale_c[][][] =
    {{{0.7,		0.4375,		2.024},		// white rotation 0
   	 {0.5238,	0.4857,		2.76190},	// white rotation 1
     {0.39047,	0.4809,		2.2604},	// white rotation 2
     {0.4190,	0.33809,	2.6428},	// white rotation 3
     {0.5285,	0.3,		3.0357},	// white rotation 4
     {0.62857,	0.3095,		2.83}},		// white rotation 5
   	{{0.70555,	0.4333,		1.9861},	// black rotation 0
     {0.5,		0.47083,	2.90625},	// black rotation 1
     {0.36818,	0.42727,	1.977},		// black rotation 2
     {0.3836,	0.2979,		2.3367},	// black rotation 3
     {0.506382,	0.174468,	2.6382978},	// black rotation 4
     {0.653333,	0.324444,	2.24444}}};	// black rotation 5
    
    static final int y_offset_b[][][] = 	// y with dot at the bottom
    { {{1,1},{1,0},{0,-1},{-1,-1},{-1,0},{0,1}},		//col,row position of the trailing dot
       {{1,2},{2,1},{1,-1},{-1,-2},{-2,-1},{-1,1}}};   	//col,row position of the far dot

     static final double y_scale_b[][][] =
    {{{0.32,	0.62666,	2.044},				// white rotation 0
   	 {0.25853,	0.26341, 	2.9146},			// white rotation 1
     {0.39166,	0.125,		2.3237},			// white rotation 2
     {0.7063,	0.1617,		2.6489},			// white rotation 3
     {0.78297,	0.4680,		3.053},				// white rotation 4
     {0.63076,	0.610256,	2.71230}},			// white rotation 5
   	{{0.3469,	0.6244,		1.971},				// black rotation 0
     {0.252173,	0.2565,		3.04913},			// black rotation 1
     {0.38,		0.124,		2.09},				// black rotation 2
     {0.728,	0.14,		2.3},				// black rotation 3
     {0.816,	0.416,		2.6900},			// black rotation 4
     {0.643902,	0.658536,	2.43268}}};	// black rotation 5

     // these are use in short move strings presented with the game record
     static final char pieceID[] =
     { 'P', 'T','T','T','T','T','T',
  	   'C','C',
  	   'E','E','E','E',
  	   'L','L',
  	   'M','M',
  	   'R','R',
  	   // same again for black
  	   'P', 'T','T','T','T','T','T',
  	   'C','C',
  	   'E','E','E','E',
  	   'L','L',
  	   'M','M',
  	   'R','R'   
     };
     
    // file names for jpeg images and masks
    static final String ImageDir = "/punct/images/";
    static final int BOARD_INDEX = 0;
    static final int HEXTILE_INDEX = 1;
    static final int PUNCT_INDEX=2;
    static final int TRI_INDEX=4;
    static final int STRAIGHT_INDEX=8;
    static final int TOP_SUBINDEX = 0;
    static final int MID_SUBINDEX = 1;
    static final int BOT_SUBINDEX = 2;
    static final int Y_INDEX=14;
    static final int WHITE_DOT_INDEX=26;
    static final int BLACK_DOT_INDEX=27;
    static final int RECYCLE_INDEX=28;
  
	   
    static final String[] ImageFileNames = 
        {
    		"board","hextile",
            "white-punct","black-punct",
            "white-tri","black-tri",
            "white-tri-r1","black-tri-r1",
            "white-straight-r0","black-straight-r0",
            "white-straight-r1","black-straight-r1",
            "white-straight-r2","black-straight-r2",
            "white-y-r0","black-y-r0",
            "white-y-r1","black-y-r1",
            "white-y-r2","black-y-r2",
            "white-y-r3","black-y-r3",
            "white-y-r4","black-y-r4",
            "white-y-r5","black-y-r5",
            "white-dot","black-dot",
            "rotate-ccw"
        };
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int LIFT_ICON_INDEX = 2;
    static final int ICON_INDEX = 3;
    static final String TextureNames[] =
    	{ "background-tile" ,"background-review-tile","lift-icon",
    			"punct-icon-nomask"};
    static final int dotIndex[]={WHITE_DOT_INDEX,BLACK_DOT_INDEX};
    static final char playerChar[] = {White,Black};
    static final PunctId playerChipCode[] = { PunctId.White_Chip_Pool,PunctId.Black_Chip_Pool};
    
    // connectivity hacks
    static final int CONN3_VALUE = 1;		// points awarded for a 3-space connection
    static final int CONN2_VALUE = 2;		// points awarded for a 2-space connection
    static final int CONN1_VALUE = 3;		// points awarded for a 1-space connection
    static final int CONN_CONNECTED = 20;	// points to be considered virtually connected
    
    // blobits are used in the move generator to designate sets of cells
    // where moves are to be considered.  Killer_blobit=1 is used for opponent
    // winning moves we need to counter. Center_blobit is used to designate the
    // center spaces when racing toward the center.  Each punctBlob gets a unique
    // blobit (there had better never be 30 of them!)
    static final int KILLER_BLOBIT = 1;		// low order bit of bloBits
    static final int CENTER_BLOBIT = 0x80000000;	// high order bit, for center drop

}