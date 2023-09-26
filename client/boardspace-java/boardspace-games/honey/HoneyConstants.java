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
package honey;

import lib.InternationalStrings;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;


public interface HoneyConstants
{	static String InvalidExplanation = "The current letter placement is invalid because: #1";
	static String NotAdjacentExplanation = "Not a continuous word";
	static String MultiUseExplanation = "Used the same cell twice";
	static String HoneyVictoryCondition = "score the most points";
	static String ResolveBlankState = "Assign a value to the blank tile";
	static String DiscardTilesState = "Click on Done to discard your rack and draw a new one";
	static String HoneyPlayState = "Form a complete crossword grid using all the tiles";
	static String TilesLeft = "#1{##no tiles, tile, tiles}";
	static String LastTurnMessage = "No More Tiles!";
	static String NotWords = "Some are not words";
	static String NotConnected = "Some letters are not connected";
	static String NotAllPlaced = "Not all tiles have been placed";
	static String AddWordMessage = "\"#1\" #2{ points, point, points}";
	static String GetDefinitionMessage = "click for defintion of #1";
	static String SelectBlankMessage = "Select the blank letter";
	static String JustWordsMessage = "Words";
	static String JustWordsHelp = "Check for good words";
	static String VocabularyMessage = "Vocabulary";
	static String WordsMessage = "Best Words";
	static String EndGameMessage = "Time Left";
	static String NextDrawMessage = "Next Draw";
	static String PullAction = "Pull";
	static String EndGameAction = "End Game";
	static String EndGameDescription = "Click on End Game to end the game with the current score";
	static String EndingGameDescription = "Waiting for the other players stop";
	static String ExplainPull = "Pull 2 more tiles from the draw pile";
	static String SwitchExplanation = "Switch to viewing this player";
	static final String[] HoneyGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	static final int MAX_PLAYERS = 12;
	static final int rackSize = 5;		// the number of filled slots in the rack.  Actual rack has some empty slots
	static final int rackSpares = 0;	// no extra spaces
	static final int TileIncrement = 2;	// tiles to take at a time
	static final int InitialDrawTime = 5*60*1000;	// 5 minutes
	static final int FinalDrawTime = 5*60*1000;		// 1 minute
	static final int NextDrawTime =  1*60*1000;	// 1 minute
	static final int EndGameTime = 1*60*1000;	// 1 minute
	static final int WordPenalty = 2;	// negative points per word
	class StateStack extends OStack<HoneyState>
	{
		public HoneyState[] newComponentArray(int n) { return(new HoneyState[n]); }
	}
	//
    // states of the game
    //
	public enum HoneyState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),	
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,false,false),
	Endgame(EndGameDescription,true,true),
	EndingGame(EndingGameDescription,true,true),

	Play(HoneyPlayState,false,false),
	NotAdjacent(NotAdjacentExplanation,false,false),
	MultiUse(MultiUseExplanation,false,false);
	;
	HoneyState(String des,boolean done,boolean digest)
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
	public boolean Puzzle() { return(this==Puzzle); }
	public boolean simultaneousTurnsAllowed() { return(this==HoneyState.Play); }
	};
	
    //	these next must be unique integers in the Jumbulayamovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum HoneyId implements CellId
	{
    	BoardLocation,
     	EmptyBoard,
    	EndGame,
    	EyeOption,
    	Rotate,
    	Lock,
    	Switch,
    	Definition,
    	PullAction,
    	Blank, InvisibleDragBoard, ZoomSlider;
    	public String shortName() { return(name()); }

	}


	// this would be a standard board with 4-per-side
	    static final int[] ZfirstInCol4 = { 3, 2, 1, 0, 1, 2, 3 }; // these are indexes into the first ball in a column, ie B1 has index 2
	    static final int[] ZnInCol4 = { 4, 5, 6, 7, 6, 5, 4 }; // depth of columns, ie A has 4, B 5 etc.

	// this would be a standard board with 5-per-side
	    static final int[] ZfirstInCol5 = { 4, 3, 2, 1, 0, 1, 2, 3, 4 };
	    static final int[] ZnInCol5 =     {5, 6, 7, 8, 9, 8, 7, 6, 5 }; // depth of columns, ie A has 4, B 5 etc.
	  //this would be a standard board with 6-per-side
	    static final int[] ZfirstInCol6 = {  5, 4, 3,  2,   1,  0,  1,  2,  3, 4, 5 };
	    static final int[] ZnInCol6 =     {  6, 7, 8, 9, 10, 11, 10, 9, 8, 7, 6}; // depth of columns, ie A has 4, B 5 etc.
	//
	// this would be a standard board with 7-per-side
	    static final int[] ZfirstInCol7 = { 6, 5, 4, 3,  2,   1,  0,  1,  2,  3, 4, 5 ,6};
		static final int[] ZnInCol7 =     { 7, 8, 9, 10, 11, 12, 13, 12, 11, 10, 9, 8, 7 }; // depth of columns, ie A has 4, B 5 etc.


		
	enum HoneyVariation
    {	HoneyComb("HoneyComb",ZfirstInCol4,ZnInCol4);
    	String name ;
    	int []firstincol;
    	int []nincol;
    	int maxTiles;
    	// constructor
    	HoneyVariation(String na,int f[],int n[])
    	{ name = na; 
    	  firstincol = f;
    	  nincol = n;
    	}
    	// match the variation from an input string
    	static HoneyVariation findVariation(String n)
    	{
    		for(HoneyVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }
 	

    static void putStrings()
    {
    	
    	String HoneyStrings[] = 
    		{  "HoneyComb",
    			SelectBlankMessage,
    			NotAdjacentExplanation,
    			MultiUseExplanation,
    			GetDefinitionMessage,
    			WordsMessage,
    			VocabularyMessage,
    			JustWordsMessage,
    			JustWordsHelp,
    			InvalidExplanation,
    			AddWordMessage,
    			NotConnected,
    			ExplainPull,
    			NotAllPlaced,
    			EndGameAction,
    			DiscardTilesState,
    			ResolveBlankState,
    			EndGameDescription,
    			NotWords,
    			SwitchExplanation,
    			PullAction,
    	    	EndGameMessage,
    	    	NextDrawMessage,
    			HoneyPlayState,
    	        HoneyVictoryCondition,
    	        EndingGameDescription,
    	        TilesLeft,

    		};
    		String HoneyStringPairs[][] = 
    		{   {"HoneyComb_family","HoneyComb"},
    				{"HoneyComb_variation","Standard HoneyComb"},
    		};

    		InternationalStrings.put(HoneyStringPairs);
    		InternationalStrings.put(HoneyStrings);
    }
}