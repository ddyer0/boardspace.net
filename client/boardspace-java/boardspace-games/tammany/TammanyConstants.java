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
package tammany;

import lib.G;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;


public interface TammanyConstants 
{	static String TammanyVictoryCondition = "Score the most points by winning votes";
	static String TammanyPlayState = "Place Your first Boss Token";
	static String TammanyPlay2State = "Place Your second Boss Token";
	static String TammanyPlayOptionState = "Place a second boss token, or a cube from Castle Cardens";
	static String TammanyVote = "Vote!";
	static String NewMayorMessage = "#1 is the new mayor";
	static String TammanyDiscState = "Take an influence disc, or click on Done";
	static String TammanySlanderState = "Slander a ward boss, or click on done";
	static String TammanyLockState = "Place lock a district, or click on Done";
	static String TammanyArrestState = "Remove a cube, or click on Done";
	static String TammanyMoveState = "Move a cube to an adjacent district, or click on Done";
	static String TammanyElectionState = "Place your votes for ward #1";
	static String TammanyCubeMessage = "Place a cube of your choice";
	static String TammanyUseRoleMessage = "Click on Done to confirm using your role";
	static String TammanyDiscMessage = "Take a disc of your choice";
	static String VacantMessage = "Ward #1 is vacant";
	static String TammanyDistributeState = "Distribute your opponents to their new roles";
	static String TammanyDistribute = "Distribute roles";
	static String ChipsNone = "no chips";
	static String ChipsOne = "1 chip";
	static String ChipsMany = "#1 chips";
	static String WinnerMessage = "Ward #1 won by #2";
	static String TieMessage = "Ward #1 is a tie";
	static String EmptyMessage = "Ward #1 is unclaimed";
	static String ConfirmPlacementDescription = "Click on Done to finish placement";
	static String ConfirmPlacementOrSlanderDescription = "Click on Done to finish placement, or Place a slander chip";
	static String TammanySlanderPaymentState = "Remove a ward boss, and pay an influence chip";
	static String TammanySlanderPayment2State = "Remove a ward boss, and pay 2 influence chips";
	static String TammanyConfirmSlanderState = "click on Done to finish the slander";
	static String TammanyDoubleSlanderState = "Slander a second boss, or click on Done";
	static String NoNewMayor = "There is no new mayor";
	static String UnusedSlander = "#1 gets #2{ points, point, point} for unused slander";
	static String PointsForEthnicControl = "#1 gets 2 points for #2 influence";
	static String WaitMessage = "Wait for the other players to vote";
	static String Election = "Election";
	static String Year = "Year #1";
	static String Ward = "Ward #1";
	static String NextElection = "Election Next Year";
	static String YouVotedMessage = "youvoted";
	static String VoteNowMessage = "setvotes"; 
	static String EnglishToken = "C_English";	// capitalized
	static String ItalianToken = "C_Italian";
	static String IrishToken = "C_Irish";
	static String GermanToken = "C_German";
	static String SlanderToken = "slander";
	static String FreezeToken = "Freeze";
	static String IrishInfluence = "Irish influence";
	static String GermanInfluence = "German influence";
	static String EnglishInfluence = "English influence";
	static String ItalianInfluence = "Italian influence";
	static String ServiceName = "Info for #1";
	static String TammanyStrings[] = 
	{  "Tammany Hall",
		ServiceName,
		IrishInfluence,
		EnglishInfluence,
		ItalianInfluence,
		GermanInfluence,
		FreezeToken,
		SlanderToken,
		TammanyVote,
		WaitMessage,
		Election,
		Year,
		TammanyDistribute,
		Ward,
		NextElection,
		TammanyUseRoleMessage,
		NewMayorMessage,
		ConfirmPlacementOrSlanderDescription,
		UnusedSlander,
		NoNewMayor,
		PointsForEthnicControl,
		EmptyMessage,
		TammanyDoubleSlanderState,
		TammanySlanderPaymentState,
		TammanySlanderPayment2State,
		TammanyConfirmSlanderState,
		ConfirmPlacementDescription,
		TammanySlanderState,
		VacantMessage,
		WinnerMessage,
		TieMessage,
		ChipsNone,
		ChipsOne,
		ChipsMany,
		TammanyCubeMessage,
		TammanyDiscMessage,
		TammanyElectionState,
       TammanyPlayState,
       TammanyPlay2State,
       TammanyPlayOptionState,
       TammanyDiscState,
       TammanyLockState,
       TammanyArrestState,
       TammanyMoveState,
       TammanyDistributeState,
	   TammanyVictoryCondition
	};
	static String TammanyStringPairs[][] = 
	{   {"Tammany","Tammany Hall"},
		{"Tammany Hall_family","Tammany Hall"},
		{"Tammany_variation","Standard Tammany Hall"},
		{EnglishToken,"English"},
		{ItalianToken,"Italian"},
		{IrishToken,"Irish"},
		{GermanToken,"German"},
		{YouVotedMessage,"you have\nvoted"},
		{VoteNowMessage,"set your\nvotes"}, 

	};
	// ward bosses/player colors
	enum Boss { Red,Brown,Black,Purple,Yellow;
		public static Boss find(int n)
			{ for(Boss v : values()) { if(v.ordinal()==n) { return(v); }}
				throw G.Error("No boss found for "+n);
			}
	};
	
	enum Zone { Zone1,Zone2,Zone3 };
	
	// ethnic groups
	enum Ethnic {Irish("Green"),English("White"),German("Orange"),Italian("Blue");
		String chipColor;
		Ethnic(String ch) { chipColor = ch; }
	};
	
	// player roles
	enum Role 
	{ 	Mayor(TammanyId.Mayor),
		DeputyMayor(TammanyId.DeputyMayor),
		CouncilPresident(TammanyId.CouncilPresident),
		ChiefOfPolice(TammanyId.ChiefOfPolice),
		PrecinctChairman(TammanyId.PrecinctChairman);
		TammanyId id;
		Role(TammanyId i)
		{
			id = i;
		}
		public static Role getRole(TammanyId i)
		{
			for(Role r : values())
			{ if(r.id==i) { return(r); }
			}
			throw G.Error("No role matches "+i);
		}
	};
	class StateStack extends OStack <TammanyState>
	{
		public TammanyState[] newComponentArray(int n) { return(new TammanyState[n]); }
	}
    //
    // states of the game
    //
	public enum TammanyState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false,false),
	Resign(ResignStateDescription,true,false,false),
	Gameover(GameOverStateDescription,true,false,false),
	ConfirmNewRoles(ConfirmNewRolesDescription,true,true,false),		// confirm assignment of roles
	
	Disc(TammanyDiscState,true,true,false),				// optionally take a disc as a perq
	Move(TammanyMoveState,true,true,false),				// optionally move a cube as a perq
	Arrest(TammanyArrestState,true,true,false),			// optionally remove a cube as a perq
	Lock(TammanyLockState,true,true,false),				// optionally lock a ward as a perq
	ConfirmUseRole(TammanyUseRoleMessage,true,true,false),		// confirm role-based actions after placement
	
	Play(TammanyPlayState,true,false,false),				// play a boss
	Play2(TammanyPlay2State,true,false,false),			// play a second boss
	PlayOption(TammanyPlayOptionState,true,false,false),	// play a cube or another boss
	ConfirmPlacementOrSlander(ConfirmPlacementOrSlanderDescription,true,true,true),	// confirm standard placement moves
	ConfirmPlacement(ConfirmPlacementDescription,true,true,false),
	
	SlanderPayment(TammanySlanderPaymentState,true,false,false),	// slander with return to perq states
	SlanderPayment2(TammanySlanderPayment2State,true,false,false),
	ConfirmSlander(TammanyConfirmSlanderState,true,true,false),
	DoubleSlander(TammanyDoubleSlanderState,true,true,false),
	PlaceCube(TammanyCubeMessage,false,false,false),		// place a cube after winning an election
	TakeDisc(TammanyDiscMessage,false,false,false),		// take a disc after winning an election
	SerialElection(TammanyElectionState,false,true,false),// election in review mode
	Election(TammanyElectionState,false,false,false),
	DistributeRoles(TammanyDistributeState,false,false,false),	// distribute roles
	SimultaneousElection(TammanyElectionState,false,true,false);

	TammanyState(String des,boolean undo,boolean done,boolean digest)
	{	allowUndo = undo;
		description = des;
		digestState = digest;
		doneState = done;
	}
	boolean doneState;
	boolean digestState;
	String description;
	boolean allowUndo;
	public boolean GameOver() { return(this==Gameover); }
	public String description() { return(description); }
	public boolean doneState() { return(doneState); }
	public boolean digestState() { return(digestState); }
	public boolean Puzzle() { return(this==Puzzle); } 
	public boolean isElection() { return((this==SimultaneousElection)||(this==SerialElection));}
	public boolean simultaneousTurnsAllowed() { return(this==SimultaneousElection); }
	};
	
    //	these next must be unique integers in the TammanyMovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum TammanyId implements CellId
	{	IrishLeader("Irish"),			// ethnic controls
		EnglishLeader("English"),
		GermanLeader("German"),
		ItalianLeader("Italian"),
		CastleGardens("Castle"),		// cubes available to be placed
		// player rolws
		Mayor("Mayor"),
		DeputyMayor("Deputy"),
		CouncilPresident("President"),
		PrecinctChairman("Chairman"),
		ChiefOfPolice("Chief"),
		
		PlayerSlander("Slander"),		// player slander reservoir
		PlayerInfluence("Influence"),	// player influence reservoir
     	Zone1Init("Zone1"),
     	Zone2Init("Zone2"),
     	Zone3Init("Zone3"),
     	ScoringTrack("Score"),
     	YearIndicator("Year"),
     	WardCube("WardCube"),
    	WardBoss("WardBoss"),
    	SlanderPlacement("SlanderPlacement"),
    	InfluencePlacement("InfluencePlacement"),
    	LockPlacement("LockPlacement"),
    	Trash("Trash"),
     	RoleCard("RoleCard"),
     	BossPlacement("BossPlacement"),
     	ElectionBoss("ElectionBoss"),
     	ElectionDisc("ElectionDisc"),
     	ElectionBox("ElectionBox"),
     	Vote("Vote"),
		Bag("Bag"),
		ShowVotes("Show");
    	String shortName = name();
    	public String shortName() { return(shortName); }
    	TammanyId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public TammanyId find(String s)
    	{	
    		for(TammanyId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public TammanyId get(String s)
    	{	TammanyId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}

    	String prettyName()
    	{	switch(this)
    		{
    		case ChiefOfPolice: return("Chief of Police");
    		case CouncilPresident: return("Council President");
    		case DeputyMayor: return("Deputy Mayor");
    		case PrecinctChairman: return("Precinct Chairman");
    		default: return(shortName);
    		}
    	}
	}


 
 enum TammanyVariation
    {
    	tammany("tammany");
    	String name ;
    	// constructor
    	TammanyVariation(String n) 
    	{ name = n; 
    	}
    	// match the variation from an input string
    	static TammanyVariation findVariation(String n)
    	{
    		for(TammanyVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }
	
	// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
 	
    static final String Tammany_SGF = "tammany"; // sgf game number allocated for tammany

 
    // file names for jpeg images and masks
    static final String ImageDir = G.isCodename1() 
			? "/appdata/tammany/images/"	:"/tammany/images/";



}