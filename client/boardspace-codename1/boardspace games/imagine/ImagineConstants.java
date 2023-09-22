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
package imagine;

import lib.G;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;

public interface ImagineConstants
{	
	static String ImagineVictoryCondition = "share your imagination";
	static String ImaginePlayState = "ImaginePlayState";
	static String ImagineReadyState = "Wait for the other players to select an image";
	static String ImagineVoteState = "Vote for one of the other player's images, and place your bet";
	static String ImagineStoryState = "Select an image, tell a story, and place your stake";
	static String WaitForStory = "WaitForStoryMessage";
	static String StoryTellerMessage = "Architect";
	static String ImagineAppreciateState = "Click on Done when you are ready to continue";
	static String SkipStateDescription = "Skip your turn as Architect";
	static String GetnewStateDescription = "Get new cards; this costs 2 points";
	static String RoundMarker = "round #1 of #2";
	static String ReadyMessage = "Ready";
	static String SkipStory = "Skip turn as Architect";
	static String GetnewStory = "Get new Cards (- 2 points)";
	static String BetAndScored = "#1 bet #2 scores #3";
	static String VoteBestMatch = "Vote for the best match";
	static String OthersVoting = "The other players are voting";
	static String ViewResult = "View the Result";
	static String WaitForArchitect = "Wait for the Architect";
	static String YouMustSelect = "You are the architect";
	static String SelectYourImage = "Select your Image";
	static String OthersAreSelecting = "The other players are selecting";
	
	static String ImagePlayReadyState = "Wait for the other players to select an image";
	static String ImagineAppreciateReadyState = "Wait for the other players to click Done";
	static String ImagineVoteReadyState = "Wait for the other players to vote";
	
	
	class StateStack extends OStack<ImagineState>
	{
		public ImagineState[] newComponentArray(int n) { return(new ImagineState[n]); }
	}
	//
    // states of the game
    //
	public enum ImagineState implements BoardState
	{
	Puzzle(PuzzleStateDescription,null,false,false),
	Resign(ResignStateDescription,null,true,false),
	Gameover(GameOverStateDescription,null,false,false),
	Story(ImagineStoryState,null,true,true),
	Play(ImaginePlayState,ImagePlayReadyState,true,true),
	Appreciate(ImagineAppreciateState,ImagineAppreciateReadyState,true,true),
	Vote(ImagineVoteState,ImagineVoteReadyState,true,true),
	Getnew(GetnewStateDescription,null,true,true),
	Skip(SkipStateDescription,null,true,true);
	ImagineState(String des,String ready,boolean done,boolean digest)
	{
		description = des;
		readyDescription = ready;
		digestState = digest;
		doneState = done;
	}
	boolean doneState;
	boolean digestState;
	String description;
	String readyDescription;
	public boolean GameOver() { return(this==Gameover); }
	public String description() { return(description); }
	public boolean doneState() { return(doneState); }
	public boolean digestState() { return(digestState); }
	public boolean Puzzle() { return(this==Puzzle); }
	public boolean simultaneousTurnsAllowed() 
	{
		switch(this)
		{
		case Appreciate:
		case Play:
		case Vote:
			return(true);
		default: return(false);
		}
	}	};
	
	static int CARDS_PER_PLAYER = 6;
	static int MIN_VOTING_CARDS = 5;
	
	enum Colors { Blue, Green, Orange, Purple, Red, Yellow,Black, White ;
		ImagineChip chip = null;
		ImagineChip checkMark = null;
		static public Colors find(int n) 
		{ for(Colors c : values()) { if(c.ordinal()==n) { return(c); }}
		  return(null);
		}
		static public Colors find(String n) 
		{ 
		  for(Colors c : values()) { if(c.name().equalsIgnoreCase(n)) { return(c); }}
		  return(null);
		}
		static public Colors get(int n) 
		{
			Colors f = find(n);
			G.Assert(f!=null,"color %d not found",n);
			return(f);
		};
	
		static public Colors get(String n) 
		{
			Colors f = find(n);
			G.Assert(f!=null,"color %s not found",n);
			return(f);
		};
	}
	static final int MAX_PLAYERS = Colors.values().length;
	
    //	these next must be unique integers in the Stymiemovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum ImagineId implements CellId
	{	Card("C"),
		Story("S"),
    	HitPlayerChip("W"),
    	SetStory(null),
    	SetReady(null),
    	SetCandidate(null),
    	SetChoice(null),
    	Stake_0(null),
    	Stake_1(null),
    	Stake_2(null),
    	Deck(null),
    	Discards(null),
    	Skip(null),
    	GetNew(null),
    	Eye(null),
    	Presentation(null),	// the presentation of selected cards
    	;
    	String shortName = name();
    	ImagineChip chip;
    	public String shortName() { return(shortName); }
    	ImagineId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public ImagineId find(String s)
    	{	
    		for(ImagineId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public ImagineId get(String s)
    	{	ImagineId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}

	}
    

 enum ImagineVariation
    {
    	Imagine("imagine");
    	String name ;
    	// constructor
    	ImagineVariation(String n) 
    	{ name = n; 
    	}
    	// match the variation from an input string
    	static ImagineVariation findVariation(String n)
    	{
    		for(ImagineVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }


    static void putStrings()
    { /*
    	 String ImagineStrings[] = 
    		{  "Imagine",
    			ViewResult,
    			ImagineAppreciateReadyState,
    			ImagineVoteReadyState,
    			ImagePlayReadyState,
    			OthersAreSelecting,
    			YouMustSelect,
    			OthersVoting,
    			SelectYourImage,
    			WaitForArchitect,
    			BetAndScored,
    			VoteBestMatch,
    			SkipStory,
    			GetnewStory,
    			RoundMarker,
    			SkipStateDescription,
    			GetnewStateDescription,
    			WaitForStory,
    			ReadyMessage,
    			StoryTellerMessage,
    			ImagineStoryState,
    			ImaginePlayState,
    			ImagineVoteState,
    			ImagineVictoryCondition,
    			ImagineAppreciateState,
    
    		};
    		String ImagineStringPairs[][] = 
    		{   {"Imagine_family","Imagine"},
    			{"Imagine_variation","Imagine"},
    			{WaitForStory,"Wait for #1 to tell a story\nabout one of their images"},
    			{ImaginePlayState,"Select one image that best matches the story and place your stake"},
    		};
    		InternationalStrings.put(ImagineStrings);
    		InternationalStrings.put(ImagineStringPairs);
	*/
    }
}