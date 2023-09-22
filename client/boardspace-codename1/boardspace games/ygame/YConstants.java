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
package ygame;

import lib.G;
import lib.OStack;
import lib.CellId;

import online.game.BaseBoard.BoardState;


public interface YConstants 
{	
	static String YVictoryCondition = "connect three sides with a chain of stones";
	static String YPlayState = "Place a stone on any empty cell";
	static String YPlayOrSwapState = "Place a stone on any empty cell, or Swap Colors";
	static String SwitchMessage = "Switch sides, and play white using the current position";
	static String YStrings[] = 
	{  "Y",
		YPlayState,
       SwitchMessage,
       YPlayOrSwapState,
       YVictoryCondition
		
	};
	static String YStringPairs[][] = 
	{   {"Y_family","Y"},
		{"Y_variation","game of Y"},
	};

	class StateStack extends OStack<YState>
	{
		public YState[] newComponentArray(int n) { return(new YState[n]); }
	}
	//
    // states of the game
    //
	public enum YState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	ConfirmSwap(ConfirmSwapDescription,true,false),
	PlayOrSwap(YPlayOrSwapState,false,false),
	Play(YPlayState,false,false);
	YState(String des,boolean done,boolean digest)
	{
		description = des;
		digestState = digest;
		doneState = done;
	}
	boolean doneState;
	boolean digestState;
	String description;
	public boolean GameOver() { return(this==Gameover); }
	public String description() { return(description); }
	public boolean doneState() { return(doneState); }
	public boolean digestState() { return(digestState); }
		public boolean Puzzle() { return(this==Puzzle); } public boolean simultaneousTurnsAllowed() { return(false); }
	};
	
    //	these next must be unique integers in the Ymovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum YId implements CellId
	{
    	Black_Chip_Pool("B"), // positive numbers are trackable
    	White_Chip_Pool("W"),
    	BoardLocation(null),
    	EmptyBoard(null),;
    	String shortName = name();
    	YChip chip;
    	public String shortName() { return(shortName); }
    	YId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public YId find(String s)
    	{	
    		for(YId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public YId get(String s)
    	{	YId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}

	}
    /* the "external representation for the board is A1 B2 etc.  This internal representation is X,Y
    where adjacent X's are separated by 2.  This gives the board nice mathematical properties for
    calculating adjacency and connectivity. */
 static int[] YnInCol =     { 9, 10, 11, 12, 13, 12, 11, 10, 5 }; // depth of columns, ie A has 4, B 5 etc.
 
 static int YNeighbors[][] = 

	 {{9,10,1,}, // A1
			 {2,0,11,10,}, // B1
			 {3,1,12,11,}, // C1
			 {2,4,12,13,}, // D1
			 {14,13,5,3,}, // E1
			 {6,4,15,14,}, // F1
			 {7,5,16,15,}, // G1
			 {6,8,16,17,}, // H1
			 {18,17,7,}, // I1
			 {0,19,20,10,}, // A2
			 {21,0,11,20,9,1,}, // B2
			 {21,2,22,10,1,12,}, // C2
			 {2,23,22,13,3,11,}, // D2
			 {23,4,12,24,14,3,}, // E2
			 {4,25,15,24,5,13,}, // F2
			 {25,6,26,14,5,16,}, // G2
			 {6,27,26,17,7,15,}, // H2
			 {27,8,16,28,18,7,}, // I2
			 {8,29,28,17,}, // J2
			 {30,9,20,31,}, // A3
			 {19,21,31,9,10,32,}, // B3
			 {33,10,32,11,20,22,}, // C3
			 {23,21,34,12,33,11,}, // D3
			 {13,34,24,12,35,22,}, // E3
			 {23,25,36,14,13,35,}, // F3
			 {37,14,15,24,26,36,}, // G3
			 {27,25,38,16,37,15,}, // H3
			 {17,38,28,16,39,26,}, // I3
			 {27,29,40,18,17,39,}, // J3
			 {41,18,28,40,}, // K3
			 {19,42,31,43,}, // A4
			 {44,19,43,32,30,20,}, // B4
			 {21,44,31,45,33,20,}, // C4
			 {21,46,32,22,45,34,}, // D4
			 {23,46,22,35,47,33,}, // E4
			 {48,23,36,47,34,24,}, // F4
			 {25,48,37,24,49,35,}, // G4
			 {25,50,36,26,38,49,}, // H4
			 {27,50,26,39,51,37,}, // I4
			 {52,27,40,51,38,28,}, // J4
			 {29,52,41,28,53,39,}, // K4
			 {54,29,40,53,}, // L4
			 {55,56,43,30,}, // A5
			 {44,42,31,57,56,30,}, // B5
			 {43,45,58,32,57,31,}, // C5
			 {46,44,58,32,59,33,}, // D5
			 {45,59,47,34,33,}, // E5
			 {48,46,35,60,59,34,}, // F5
			 {49,47,61,35,60,36,}, // G5
			 {48,50,61,36,62,37,}, // H5
			 {62,51,38,37,49,}, // I5
			 {52,50,39,63,62,38,}, // J5
			 {53,51,64,39,63,40,}, // K5
			 {52,54,64,40,65,41,}, // L5
			 {66,65,41,53,}, // M5
			 {42,67,68,56,}, // A6
			 {69,42,57,68,55,43,}, // B6
			 {69,44,56,70,43,58,}, // C6
			 {44,71,57,45,59,70,}, // D6
			 {71,46,60,58,45,47,}, // E6
			 {71,48,72,59,47,61,}, // F6
			 {48,73,72,62,49,60,}, // G6
			 {50,73,63,61,51,49,}, // H6
			 {73,52,74,62,51,64,}, // I6
			 {52,75,74,65,53,63,}, // J6
			 {75,54,64,76,66,53,}, // K6
			 {54,77,76,65,}, // L6
			 {78,55,68,79,}, // A7
			 {67,69,79,55,56,80,}, // B7
			 {81,56,80,70,68,57,}, // C7
			 {71,69,57,81,82,58,}, // D7
			 {82,59,70,60,58,72,}, // E7
			 {73,71,83,61,82,60,}, // F7
			 {83,62,63,61,74,72,}, // G7
			 {75,73,84,64,83,63,}, // H7
			 {65,84,76,64,85,74,}, // I7
			 {75,77,86,66,65,85,}, // J7
			 {87,66,76,86,}, // K7
			 {67,88,79,89,}, // A8
			 {90,67,89,80,78,68,}, // B8
			 {69,90,79,91,81,68,}, // C8
			 {69,92,70,80,91,82,}, // D8
			 {71,92,70,72,81,83,}, // E8
			 {73,92,72,74,84,82,}, // F8
			 {75,92,74,85,91,83,}, // G8
			 {90,75,86,91,84,76,}, // H8
			 {77,90,87,76,89,85,}, // I8
			 {88,77,86,89,}, // J8
			 {87,89,78,}, // A9
			 {90,88,79,86,87,78,}, // B9
			 {89,91,85,80,86,79,}, // C9
			 {92,90,85,80,84,81,}, // D9
			 {91,84,81,83,82,}, // E9
			 };


 // derived for our picture board
 static public double[][]YCoords =
 {{0.014172839506172874, 0.17839672131147544}, /* A1 */
	 {0.12790950226244346, 0.11816380090497741}, /* B1 */
	 {0.2455565610859729, 0.08196470588235294}, /* C1 */
	 {0.3586787330316742, 0.05934027149321269}, /* D1 */
	 {0.49173288590604036, 0.05033557046979864}, /* E1 */
	 {0.6259610738255033, 0.057046979865771785}, /* F1 */
	 {0.740055033557047, 0.07718120805369133}, /* G1 */
	 {0.8541489932885906, 0.1174496644295302}, /* H1 */
	 {0.9749543624161074, 0.1778523489932886}, /* I1 */

	 {0.01972732329645077, 0.3120805369127517}, /* A2 */
	 {0.10056543209876545, 0.2222222222222222}, /* B2 */
	 {0.20550370370370374, 0.16666666666666663}, /* C2 */
	 {0.31044197530864204, 0.13580246913580252}, /* D2 */
	 {0.42772592592592595, 0.11728395061728392}, /* E2 */
	 {0.5555555555555556, 0.1169382716049383}, /* F2 */
	 {0.6746395061728396, 0.13580246913580252}, /* G2 */
	 {0.7795777777777779, 0.16666666666666663}, /* H2 */
	 {0.8906888888888889, 0.2222222222222222}, /* I2 */
	 {0.964762962962963, 0.308641975308642}, /* J2 */

	 {0.04500987654320987, 0.42592592592592593}, /* A3 */
	 {0.10056543209876545, 0.345679012345679}, /* B3 */
	 {0.17463950617283952, 0.2716049382716049}, /* C3 */
	 {0.27340493827160495, 0.2222222222222222}, /* D3 */
	 {0.37834320987654324, 0.19753086419753085}, /* E3 */
	 {0.49562716049382716, 0.18518518518518523}, /* F3 */
	 {0.6067382716049383, 0.19753086419753085}, /* G3 */
	 {0.7178493827160493, 0.2222222222222222}, /* H3 */
	 {0.8166148148148147, 0.26543209876543206}, /* I3 */
	 {0.8845160493827162, 0.345679012345679}, /* J3 */
	 {0.9400716049382716, 0.42592592592592593}, /* K3 */

	 {0.08204691358024696, 0.5370370370370371}, /* A4 */
	 {0.1252567901234568, 0.4567901234567901}, /* B4 */
	 {0.1808123456790124, 0.3765432098765432}, /* C4 */
	 {0.2487135802469136, 0.30864197530864196}, /* D4 */
	 {0.347479012345679, 0.2777777777777778}, /* E4 */
	 {0.44624444444444444, 0.2654320987654321}, /* F4 */
	 {0.538837037037037, 0.2654320987654321}, /* G4 */
	 {0.6376024691358024, 0.2777777777777778}, /* H4 */
	 {0.7363679012345679, 0.3148148148148148}, /* I4 */
	 {0.8042691358024692, 0.3765432098765432}, /* J4 */
	 {0.8598246913580248, 0.4567901234567901}, /* K4 */
	 {0.9030345679012346, 0.5370370370370371}, /* L4 */
	 {0.1437753086419753, 0.654320987654321}, /* A5 */
	 {0.16846666666666665, 0.5679012345679012}, /* B5 */
	 {0.2116765432098766, 0.4876543209876543}, /* C5 */
	 {0.2672320987654322, 0.41358024691358025}, /* D5 */
	 {0.32278765432098766, 0.35802469135802467}, /* E5 */
	 {0.40920740740740746, 0.345679012345679}, /* F5 */
	 {0.49562716049382716, 0.3395061728395062}, /* G5 */
	 {0.5820469135802471, 0.345679012345679}, /* H5 */
	 {0.6622938271604939, 0.35802469135802467}, /* I5 */
	 {0.7240222222222221, 0.41358024691358025}, /* J5 */
	 {0.7734049382716051, 0.48148148148148145}, /* K5 */
	 {0.8166148148148147, 0.5679012345679012}, /* L5 */
	 {0.847479012345679, 0.654320987654321}, /* M5 */

	 {0.21784938271604937, 0.7654320987654321}, /* A6 */
	 {0.23636790123456786, 0.6790123456790124}, /* B6 */
	 {0.2610592592592593, 0.5864197530864198}, /* C6 */
	 {0.31044197530864204, 0.5061728395061729}, /* D6 */
	 {0.35982469135802475, 0.4382716049382716}, /* E6 */
	 {0.4524172839506173, 0.42592592592592593}, /* F6 */
	 {0.538837037037037, 0.42592592592592593}, /* G6 */
	 {0.6314296296296296, 0.43209876543209874}, /* H6 */
	 {0.6808123456790125, 0.5061728395061729}, /* I6 */
	 {0.730195061728395, 0.5864197530864198}, /* J6 */
	 {0.7548864197530865, 0.6728395061728395}, /* K6 */
	 {0.7734049382716051, 0.7654320987654321}, /* L6 */

	 {0.2980962962962963, 0.8518518518518519}, /* A7 */
	 {0.31044197530864204, 0.7716049382716049}, /* B7 */
	 {0.3289604938271605, 0.6790123456790124}, /* C7 */
	 {0.3536518518518519, 0.5864197530864198}, /* D7 */
	 {0.39686172839506173, 0.5061728395061729}, /* E7 */
	 {0.49562716049382716, 0.5}, /* F7 */
	 {0.5943925925925926, 0.5061728395061729}, /* G7 */
	 {0.6314296296296296, 0.5864197530864198}, /* H7 */
	 {0.6622938271604939, 0.6790123456790124}, /* I7 */
	 {0.6869851851851854, 0.7654320987654321}, /* J7 */
	 {0.6993308641975309, 0.8518518518518519}, /* K7 */

	 {0.384516049382716, 0.9320987654320988}, /* A8 */
	 {0.39068888888888886, 0.8395061728395061}, /* B8 */
	 {0.4030345679012346, 0.7592592592592593}, /* C8 */
	 {0.41538024691358033, 0.6666666666666666}, /* D8 */
	 {0.44054429530201333, 0.5814}, /* E8 */
	 {0.5479268456375839, 0.5780442953020134}, /* F8 */
	 {0.5747724832214764, 0.6652926174496644}, /* G8 */
	 {0.5881953020134227, 0.7592523489932885}, /* H8 */
	 {0.601618120805369, 0.8397892617449664}, /* I8 */
	 {0.6083295302013423, 0.9270375838926174}, /* J8 */


	 {0.4942355704697986, 1.0008630872483222}, /* A9 */
	 {0.4942355704697986, 0.9069033557046979}, /* B9 */
	 {0.4942355704697986, 0.8196550335570469}, /* C9 */
	 {0.4942355704697986, 0.7324067114093958}, /* D9 */
	 {0.4942355704697986, 0.6451583892617448}, /* E9 */
	 };
 //
 // this is an ad-hoc permuitation of the neighbor and coordinate lists, above and below,
 // that transforms the numbering in Hexwiki diagrams to the row/column format used
 // by boardspace.  Read this as the lower left position, A1, corresponds to the
 // 17'th row of the coordinate/neighbor table
 //
 static int YPerm[] = 
	 {
	  17,16,15,14,13,12,11,10, 9,
	  18,39,38,37,36,35,34,33,32, 8,
	  19,40,58,57,56,55,54,53,52,31, 7,
	  20,41,59,74,73,72,71,70,69,51,30, 6,
	  21,42,60,75,87,86,85,84,83,68,50,29, 5,
	  22,43,61,76,88,93,92,82,67,49,28, 4,
	  23,44,62,77,89,91,81,66,48,27, 3,
	  24,45,63,78,90,80,65,47,26, 2,
	   1,25,46,64,79
	 };

 enum YVariation
    {
    	Y("Y");
    	String name ;
    	// constructor
    	YVariation(String nn) 
    	{ name = nn; 
     	}
    	// match the variation from an input string
    	static YVariation findVariation(String n)
    	{
    		for(YVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }
	// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
 	
    static final String Y_SGF = "Y"; // sgf game name
    static final String[] YGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

    // file names for jpeg images and masks
    static final String ImageDir = "/ygame/images/";

}