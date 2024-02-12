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
	
	static String NonWordsMessage = "#1 Non Words";
	static String SharedWordsMessage = "#1 Shared";
	static String ShowSummaryMessage = "Show all the words found";
	
	static String NotAdjacentExplanation = "Not a continuous word";
	static String HoneyVictoryCondition = "score the most points";
	static String HoneyPlayState = "Find the most value in Words";
	static String GetDefinitionMessage = "click for defintion of #1";
	static String JustWordsMessage = "#1 Words";
	static String AvailableWordsMessage = "FindableMessage";
	static String FoundWordsMessage = "FoundMessage";
	static String VocabularyMessage = "Vocabulary";
	static String EndGameMessage = "Time Left";
	static String EndGameAction = "End Game";
	static String EndGameDescription = "Click on End Game to end the game with the current score";
	static String EndingGameDescription = "Waiting for the other players stop";
	static String SwitchExplanation = "Switch to viewing this player";
	static final String[] HoneyGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	static final int MAX_PLAYERS = 12;
	static final int InitialDrawTime = (3*60+5)*1000;	// 3 minutes plus 5 second countdown
	static final int EndGameTime = 1*60*1000;	// 1 minute
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
	NotAdjacent(NotAdjacentExplanation,false,false);
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
    	EndGame,
    	Switch,
    	Definition,
    	ShowSummary,
    	ZoomSlider, Vocabulary;
    	public String shortName() { return(name()); }

	}


	// this would be a standard board with 4-per-side
	    static final int[] ZfirstInCol4 = { 3, 2, 1, 0, 1, 2, 3 }; // these are indexes into the first ball in a column, ie B1 has index 2
	    static final int[] ZnInCol4 = { 4, 5, 6, 7, 6, 5, 4 }; // depth of columns, ie A has 4, B 5 etc.

		
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
    			NotAdjacentExplanation,
    			GetDefinitionMessage,
    			NonWordsMessage,
    			SharedWordsMessage,
    			VocabularyMessage,
    			JustWordsMessage,
    			InvalidExplanation,
    			EndGameAction,
    			EndGameDescription,
    			ShowSummaryMessage,
    			SwitchExplanation,
    	    	EndGameMessage,
    			HoneyPlayState,
    	        HoneyVictoryCondition,
    	        EndingGameDescription,
 
    		};
    		String HoneyStringPairs[][] = 
    		{   {"HoneyComb_family","HoneyComb"},
    			{"HoneyComb_variation","Standard HoneyComb"},
    			{FoundWordsMessage,"#1 Found\nBy Any Player"},
    			{AvailableWordsMessage,"#1 Findable\nin #2 words"},
    		};

    		InternationalStrings.put(HoneyStringPairs);
    		InternationalStrings.put(HoneyStrings);
    }
}