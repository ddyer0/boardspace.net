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
package crosswords;

import lib.InternationalStrings;
import lib.OStack;

import java.awt.Color;

import lib.CellId;
import online.game.BaseBoard.BoardState;


public interface CrosswordsConstants
{	static String InvalidExplanation = "The current letter placement is invalid because: #1";
	static String CrosswordsVictoryCondition = "score the most points";
	static String ResolveBlankState = "Assign a value to the blank tile";
	static String DiscardTilesState = "Click on Done to discard your rack and draw a new one";
	static String CrosswordsPlayState = "Place a word on the board";
	static String FirstPlayState = "Position the starting tile and set word placement options";
	static String DoubleWord = "Double Word Score";
	static String DoubleLetter = "Double Letter Score";
	static String TripleWord = "Triple Word Score";
	static String TripleLetter = "Triple Letter Score";
	static String TilesLeft = "#1{##no tiles, tile, tiles}";
	static String LastTurnMessage = "Last Turn!";
	static String NotWords = "Some crosswords are not words";
	static String NotALine = "Some letters are not connected";
	static String DuplicateWord = "There are duplicate words";
	static String NotNewConnected = "Not all new words are connected";
	static String BackwardMessage = "backwards words";
	static String DiagonalMessage = "diagonal words";
	static String ConnectedMessage = "any connected letters";
	static String NoDuplicateMessage = "duplicate words";
	static String OpenRackMessage = "open racks";
	static String RotateMessage = "rotate the board";
	static String ShowTilesMessage = "Show everyone your tiles";
	static String LockMessage = "Lock auto-rotation of the board";
	static String UnlockMessage = "allow auto-rotation of the board";
	static String HideTilesMessage = "Hide your tiles";
	static String DumpRackMessage = "Drop a tile here to dump your rack and redraw";
	static String ServiceName = "Crossword Rack for #1";
	static String AddWordMessage = "\"#1\" #2{ points, point, points}";
	static String GetDefinitionMessage = "click for defintion of #1";
	static String SelectBlankMessage = "Select the blank letter";
	static String JustWordsMessage = "Words";
	static String JustWordsHelp = "Check for good words";
	static String VocabularyMessage = "Vocabulary";
	static String WordsMessage = "Best Words";
	static String SeeYourTilesMessage = "See Your Tiles";
	static String UseBigFont = "use larger letters on the tiles";
	static String UseSmallFont = "use smaller letters on the tiles";

	static Color[] mouseColors = { Color.red,Color.yellow,Color.blue,Color.green};
	static Color[] dotColors = { Color.white,Color.black,Color.white,Color.white};
	class StateStack extends OStack<CrosswordsState>
	{
		public CrosswordsState[] newComponentArray(int n) { return(new CrosswordsState[n]); }
	}
	//
    // states of the game
    //
	public enum CrosswordsState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Gameover(GameOverStateDescription,false,false),
	Confirm(ConfirmStateDescription,true,true),
	ConfirmFirstPlay(ConfirmStateDescription,true,true),
	Play(CrosswordsPlayState,false,false),
	ResolveBlank(ResolveBlankState,false,false),
	DiscardTiles(DiscardTilesState,true,true),
	FirstPlay(FirstPlayState,true,true);
	CrosswordsState(String des,boolean done,boolean digest)
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
	};
	
    //	these next must be unique integers in the Jumbulayamovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum CrosswordsId implements CellId
	{
    	BoardLocation,
    	Rack,
    	BigfontOn,
    	BigfontOff,
    	RackMap,
    	LocalRack,
    	RemoteRack,
    	DrawPile,
    	EmptyBoard,
    	SetOption,
    	EyeOption,
    	Rotate,
    	Lock,
    	CheckWords,
    	Vocabulary,
    	Definition,
    	Blank, RevealRack;

	}
 enum Option
 {
	 Backwards(BackwardMessage,true),
	 Diagonals(DiagonalMessage,true),
	 Connected(ConnectedMessage,false),
	 NoDuplicate(NoDuplicateMessage,false),
	 OpenRacks(OpenRackMessage,true);
	 String message;
	 boolean allowedForRobot = true;
	 CrosswordsChip onIcon;
	 CrosswordsChip offIcon;
	 Option(String ms,boolean lo) { message = ms; allowedForRobot = lo; }
	 static Option getOrd(int n) { return(values()[n]); }
	 static int optionsAvailable(boolean robotgame)
	 {
		 int n=0;
		 for(Option v : values()) { if(!robotgame || v.allowedForRobot) { n++; }}
		 return(n);
	 }
 }
 enum CrosswordsVariation
    {	Crosswords("crosswords",15),
    	Crosswords17("crosswords-17",17);	
    	String name ;
    	int boardsize;
    	// constructor
    	CrosswordsVariation(String n,int bs) 
    	{ name = n; 
    	  boardsize = bs;
    	}
    	// match the variation from an input string
    	static CrosswordsVariation findVariation(String n)
    	{
    		for(CrosswordsVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }
 	

    static void putStrings()
    {
    	
    	String CrosswordsStrings[] = 
    		{  "Crosswords",
    			SelectBlankMessage,SeeYourTilesMessage,
    			GetDefinitionMessage,
    			WordsMessage,
    			VocabularyMessage,
    			UseBigFont,
    			UseSmallFont,
    			JustWordsMessage,
    			JustWordsHelp,
    			InvalidExplanation,
    			AddWordMessage,
    			LockMessage,
    			RotateMessage,
    			UnlockMessage,
    			ServiceName,
    			ShowTilesMessage,
    			DumpRackMessage,
    			HideTilesMessage,
    			NoDuplicateMessage,
    			ConnectedMessage,
    			DuplicateWord,
    			DiagonalMessage,
    			BackwardMessage,
    			NotNewConnected,
    			NotALine,
    			DiscardTilesState,
    			ResolveBlankState,
    			NotWords,
    			FirstPlayState,
    			CrosswordsPlayState,
    	        CrosswordsVictoryCondition,
    	       DoubleWord,
    	       DoubleLetter,
    	       TripleWord,
    	       TripleLetter,
    	       TilesLeft,

    		};
    		String CrosswordsStringPairs[][] = 
    		{   {"Crosswords_family","Crosswords"},
    				{"Crosswords_variation","Standard Crosswords"},
    				{"Crosswords-17_variation","Big Crosswords"},
    		};

    		InternationalStrings.put(CrosswordsStringPairs);
    		InternationalStrings.put(CrosswordsStrings);
    }
}