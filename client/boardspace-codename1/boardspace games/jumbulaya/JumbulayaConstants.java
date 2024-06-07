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
package jumbulaya;

import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;


public interface JumbulayaConstants 
{	static String InvalidExplanation = "The current letter placement is invalid because: #1";
	static String JumbulayaVictoryCondition = "score the most points";
	static String DiscardTilesState = "Discard tiles and draw a new ones";
	static String JumbulayaPlayState = "Place a word on the board";
	static String TilesLeft = "#1{##no tiles, tile, tiles}";
	static String OpenRackMessage = "open racks";
	static String RotateMessage = "rotate the board";
	static String ShowTilesMessage = "Show everyone your tiles";
	static String LockMessage = "Lock auto-rotation of the board";
	static String UnlockMessage = "allow auto-rotation of the board";
	static String HideTilesMessage = "Hide your tiles";
	static String DumpRackMessage = "Drop a tile here to discard and redraw";
	static String ServiceName = "Jumbulaya Rack for #1";
	static String GetDefinitionMessage = "click for defintion of #1";
	static String SelectBlankMessage = "Select the blank letter";
	static String CanJumbulayaDescription = "Click on Done or Start a Jumbulaya";
	static String ConfirmJDescription = "Click on Done to end the game with a Jumbulaya";
	static String JumbulayaStateDescription = "Select the tiles that make a Jumbulaya";
	static String InvalidWord = "Not a valid word";
	static String TooManyRack = "Too many letters from your rack";
	static String FewerTiles = "Fewer tiles in this word";
	static String DuplicateWord = "Duplicate word on this line";
	static String PlayJumbulaya = "Jumbulaya : #1";
	static String PlayWord = "Word : #1";
	static String StartJumbulaya = "Start a Jumbulaya";
	static String EndJumbulaya = "Abandon Jumbulaya";
	static String PrevWordMessage = "Most recent word \"#1\" : #2";
	static String PendingWordMessage = "New word \"#1\" : #2";
	static String JumbulayaMessage = "Jumbulaya Word \"#1\" : #2";
	static String SkipTurnMessage = "#1 Skipped turn (penalty)";
	static String ResignedMessage = "#1 left the game";
	static String PossibleMessage = "Possible Jumbulayas";
	static String CheckJumbulayaHelp = "Check for Jumbulayas";
	static String WordsMessage = "Best Words";
	static String VocabularyMessage = "Vocabulary";
	static String JumbulayasMessage = "Jumbulayas";
	static String JustWordsMessage = "Words";
	static String JustWordsHelp = "Check for good words";
	static String AbandonExplanation = "Abandon the Jumbulaya, and accept the penalty";
	static String EndExplanation = "End the game with a Jumbulaya";
	static String SeeYourTilesMessage = "See Your Tiles";

	class StateStack extends OStack<JumbulayaState>
	{
		public JumbulayaState[] newComponentArray(int n) { return(new JumbulayaState[n]); }
	}
	//
    // states of the game
    //
	public enum JumbulayaState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	ConfirmJumbulaya(ConfirmJDescription,true,true),
	Jumbulaya(JumbulayaStateDescription,false,false),
	CanJumbulaya(CanJumbulayaDescription,true,true),
	Play(JumbulayaPlayState,false,false),
	DiscardTiles(DiscardTilesState,true,true);
	JumbulayaState(String des,boolean done,boolean digest)
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
	
    //	these next must be unique integers in the Jumbulayamovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum JumbulayaId implements CellId
	{
    	BoardLocation,
    	Claimed,
    	PlayerCell,
    	Rack,			// rack on the main board
    	RackMap,		// rack for use in the display
    	LocalRack,		// rack on a hidden window
    	RemoteRack,		// rack on a remote client
    	DrawPile,
    	EmptyBoard,
    	SetOption,
    	EyeOption,
    	Rotate,
    	Lock,
    	CheckWords,
    	CheckJumbulayas,
    	Vocabulary,
    	Definition,
    	StartJumbulaya,
    	EndJumbulaya,
    	Blank, RevealRack;

	}
 enum Option
 {
	 OpenRacks(OpenRackMessage,true);
	 String message;
	 boolean allowedForRobot = true;
	 JumbulayaChip onIcon;
	 JumbulayaChip offIcon;
	 Option(String ms,boolean lo) { message = ms; allowedForRobot = lo; }
	 static Option getOrd(int n) { return(values()[n]); }
	 static int optionsAvailable(boolean robotgame)
	 {
		 int n=0;
		 for(Option v : values()) { if(!robotgame || v.allowedForRobot) { n++; }}
		 return(n);
	 }
 }
 enum JumbulayaVariation
    {	Jumbulaya("jumbulaya");
    	String name ;
    	// constructor
    	JumbulayaVariation(String n) 
    	{ name = n; 
    	}
    	// match the variation from an input string
    	static JumbulayaVariation findVariation(String n)
    	{
    		for(JumbulayaVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }
 	
    static void putStrings()
    {/*
    	 String JumbulayaStrings[] = 
    		{  "Jumbulaya",
    			WordsMessage,
    			ResignedMessage,SeeYourTilesMessage,
    			AbandonExplanation,
    			EndExplanation,
    			FewerTiles,
    			JustWordsHelp,
    			JustWordsMessage,
    			JumbulayasMessage,
    			VocabularyMessage,
    			PlayJumbulaya,
    			WordsMessage,
    			CheckJumbulayaHelp,
    			PossibleMessage,
    			CanJumbulayaDescription,
    			JumbulayaMessage,
    			SkipTurnMessage,
    			PendingWordMessage,
    			StartJumbulaya,
    			EndJumbulaya,
    			PrevWordMessage,
    			PlayWord,
    			InvalidWord,
    			DuplicateWord,
    			TooManyRack,
    			ConfirmJDescription,
    			JumbulayaStateDescription,
    			SelectBlankMessage,
    			GetDefinitionMessage,
    			InvalidExplanation,
    			LockMessage,
    			RotateMessage,
    			UnlockMessage,
    			ServiceName,
    			ShowTilesMessage,
    			DumpRackMessage,
    			HideTilesMessage,
    			DiscardTilesState,
    			JumbulayaPlayState,
    			JumbulayaVictoryCondition,
    	       TilesLeft,

    		};
    		

    		 String JumbulayaStringPairs[][] = 
    		{   {"Jumbulaya_family","Jumbulaya"},
    			{"Jumbulaya_variation","Standard Jumbulaya"},
    		};
     		InternationalStrings.put(JumbulayaStrings);
    		InternationalStrings.put(JumbulayaStringPairs);
    */		
    }
}