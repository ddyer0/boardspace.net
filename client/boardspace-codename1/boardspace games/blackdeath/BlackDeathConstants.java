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
package blackdeath;

import lib.CellId;
import lib.Digestable;
import lib.G;
import lib.NameProvider;
import lib.OStack;
import lib.Random;
import online.game.BaseBoard.BoardState;


public interface BlackDeathConstants
{	
	static final String BlackDeathVictoryCondition = "be the most successful disease";
	static final String SetVirulenceDescription = "Set the virulence and lethality of your disease";
	static final String BlackDeathPlayState = "Select The die for Infection attempts";
	static final String BlackDeathPlacementState = "Place 2 initial units";
	static final String BlackDeathFirstInfect = "Infection phase (spread only, no attacks) #1 infection attempts remaining";
	static final String BlackDeathMovementState = "Movement phase, #1 movement points remaining";
	static final String BlackDeathFirstMovement = "Movement phase, (your units only) #1 movement points remaining";
	static final String BlackDeathWesternMovement = "Extra movement, Move a unit toward the west ";
	static final String BlackDeathEasternMovement = "Movement phase, Move a unit toward Jerusalem";
	
	static final String BlackDeathInfectionState = "Infection phase, #1 infection attempts remaining";
	static final String BlackDeathMortalityState = "Mortality phase, click on \"Roll\" to stop the dice";
	static final String BlackDeathRollState = "click on \"Roll\" to stop the dice, or play a card";
	static final String BlackDeathRoll = "click on \"Roll\" to stop the dice";
	static final String BlackDeathKill = "Mortality phase, remove #1 units";
	static final String BlackDeathCatastrophicKill = "Catastrophic! Mortality phase, remove #1 of your units";
	static final String AnyBlackDeathCatastrophicKill = "Catastrophic! Kill a square";
	static final String CloseSomeRegion = "Close one region until your next turn";
	static final String PogromSomeRegion = "Designate one region as the Pogrom Death Zone";
	static final String BlackDeathCure = "Miraculous cure!  remove #1 units";
	static final String ConfirmCardDescription = "Click on Done to confirm playing this card";
	static final String InfectionAttempts = "Infection Attempts";
	static final String Movements = "Unit Movements";
	static final String RollAction = "Roll";
	static final String PerfectRollAction = "Just Win!";
	static final String MutationOrMessage = "For the next turn, alter any virulence or mortality by 1 point";
	static final String MutationAndMessage = "For the next turn, alter any virulence and mortality by 1 point";
	static final String MutationSwapMessage = "For the next turn, alter any virulence and motality by up to 2 and -2 points";
	static final String PerfectRollExplanation = "PerfectRoll";
	static final String NoKillMessage = "NoKillMessage";
	static final String CloseLinkDescription = "Designate #1 links to be closed";
	static final String TradersPlus1Description = "Take 1 Extra Infection Attempt, +1 virulence, at the Eastern Ports";
	static final String TradersPlus2Description = "Take #1 Extra Infection Attempts, at Eastern Ports";
	static final String AlwaysSucceeds = "always suceeds";
	static final String AlwaysFails = "always fails";
	static final String RollDetails = "virulence #1 #2";
	static final String Virulence = "Virulence #1";
	static final String Mortality = "Mortality #1";
	static final String Success = "Succeeds";
	static final String Failure = "Fails";
	static final String Escape = "escape";
	static final String PlayCard = "Playcard";
	
	enum DiseaseMod {
		None,Wet,Cold,Warm,Crowd;
	}
	enum CardEffect {
		SlowTravel,
		HighVirulence,
		Crusade,
		Famine,
		LowVirulence,
		Fire,
		Quaranteen,
		MutationVirulenceOrMortality,
		MutationSwap,
		MutationVirulenceAndMortality,
		Mongols,
		Smugglers,
		TradersPlus1,
		TradersPlus2,
		Pogrom,
		War,;
		public static CardEffect find(String n)
		{
			for(CardEffect v : values()) { if(v.name().equalsIgnoreCase(n)) { return(v); }}
			throw G.Error("Card Effect %s not found",n);
		}
	}
	public enum Dice implements NameProvider
	{	
		d6_1w(1,"white-d6-1",	0.6,0.47,1.392),
		d6_2w(2,"white-d6-2",	0.6,0.47,1.366),
		d6_3w(3,"white-d6-3",	0.6,0.47,1.266),
		d6_4w(4,"white-d6-4",	0.6,0.47,1.363),
		d6_5w(5,"white-d6-5",	0.6,0.47,1.46),
		d6_6w(6,"white-d6-6",	0.6,0.47,1.28);
		int faceValue=0;
		// constructor
		Dice(int v,String str,double x,double y, double s) 
		{ faceValue = v; imageName = str; scale[0]=x; scale[1]=y; scale[2]=s;}
		BlackDeathChip chip = null;
		double scale[] = new double[3];
		String imageName;
		public String getName() { return(imageName); } 
		public static String[] getNames()
				{
				Dice d[] = values();
				String names[] = new String[d.length];
				for(int i=0;i<d.length;i++) { names[i] = d[i].imageName; }
				return(names);
				}
	}
    

 enum BlackDeathVariation
    {
    	blackdeath("blackdeath"),
    	blackdeath_low("blackdeath-low");
    	String name ;
    	// constructor
    	BlackDeathVariation(String n) 
    	{ name = n; 
    	}
    	// match the variation from an input string
    	static BlackDeathVariation findVariation(String n)
    	{
    		for(BlackDeathVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }

    static final String BlackDeath_SGF = "blackdeath"; // sgf game number allocated for blackdeath

    // file names for jpeg images and masks
    // Blackdeath is the first game to have it's images moved (experimentally) into a 
    // dynamically loaded archive.  This gets it out of the codename1 45mb limit.
    // dictionary data code crosswords and wyps is also handled this way.
    //
    static final String ImageDir =
    			G.isCodename1() 
    				? "/appdata/blackdeath/images/"	// loaded at startup time from the web site and cached
    				: "/blackdeath/images/";

class StateStack extends OStack<BlackDeathState>
{
	public BlackDeathState[] newComponentArray(int n) { return(new BlackDeathState[n]); }
}
//
// states of the game
//
public enum BlackDeathState implements BoardState
{
Puzzle(PuzzleStateDescription,false,false),
Resign(ResignStateDescription,true,false),
Gameover(GameOverStateDescription,false,false),
Confirm(ConfirmStateDescription,true,true),
ConfirmCard(ConfirmCardDescription,true,true),
SetVirulence(SetVirulenceDescription,false,false),
SelectInfection(BlackDeathPlayState,false,false),
InitialPlacement(BlackDeathPlacementState,false,false),
FirstInfect(BlackDeathFirstInfect,false,false),
Infection(BlackDeathInfectionState,false,false),
FirstMovement(BlackDeathFirstMovement,true,true),
Movement(BlackDeathMovementState,true,true),
WesternMovement(BlackDeathWesternMovement,false,false),	//  if we're in this state, no escape with "done"
EasternMovement(BlackDeathEasternMovement,false,false),	// if we're in this state, no escape with "done"
Mortality(BlackDeathMortalityState,false,false),
Roll(BlackDeathRollState,false,false),
Kill(BlackDeathKill,false,false),
CatastrophicKill(BlackDeathCatastrophicKill,false,false),
AnyCatastrophicKill(AnyBlackDeathCatastrophicKill,false,false),
CloseRegion(CloseSomeRegion,false,false),
Pogrom(PogromSomeRegion,false,false),
MutationOr(MutationOrMessage,false,false),
MutationAnd(MutationAndMessage,false,false),
MutationSwap(MutationSwapMessage,false,false),
Roll2(BlackDeathRoll,false,false),
Cure(BlackDeathCure,false,false),
CloseLinks(CloseLinkDescription,false,false),
TradersPlus1(TradersPlus1Description,false,false),
TradersPlus2(TradersPlus2Description,false,false),
;
BlackDeathState(String des,boolean done,boolean digest)
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
public boolean simultaneousTurnsAllowed() { return false; }
};


//these next must be unique integers in the BlackDeathMovespec dictionary
//they represent places you can click to pick up or drop a stone
enum BlackDeathId implements CellId
{	
BoardLocation,
EmptyBoard,
PlayerHome,
Cards,
TemporaryCards,
DrawPile,
DiscardPile,
PlayerChips,
PlayerVirulence,
PlayerMortality,
Roll,
PerfectRoll,
Magnify,
MagnifyBoard,
Escape,
Eye,
Link,
;

static public BlackDeathId find(String s)
{	
	for(BlackDeathId v : values()) { if(s.equalsIgnoreCase(v.name())) { return(v); }}
	return(null);
}

}

enum BlackDeathColor implements Digestable
{ Red, Orange, Brown, Yellow, Green, Blue, Purple;
  BlackDeathChip chip = null;
  public long Digest(Random r) {
	return ((ordinal()+1)*r.nextLong());
  	}  
  public static BlackDeathColor get(String n)
  {
	  for(BlackDeathColor c : values()) { if(c.name().equalsIgnoreCase(n)) { return(c); }}
	  throw G.Error("Can't find BlackDeathColor "+n);
  }
}    
	static void putStrings()
	{	/*
		final String BlackDeathStrings[] = 
			{   RollAction,
				RollDetails,
				Success,
				Failure,
				Virulence,
				Escape,
				PlayCard,
				Mortality,
				AlwaysSucceeds,
				BlackDeathRollState,
				AlwaysFails,
				CloseLinkDescription,
				TradersPlus1Description,
				TradersPlus2Description,
				MutationSwapMessage,
				CloseSomeRegion,
				PogromSomeRegion,
				MutationAndMessage,
				BlackDeathEasternMovement,
				BlackDeathWesternMovement,
				AnyBlackDeathCatastrophicKill,
				MutationOrMessage,
				ConfirmCardDescription,
				InfectionAttempts,
				PerfectRollAction,
				Movements,
				BlackDeathCatastrophicKill,
				BlackDeathCure,
				BlackDeathRoll,
				BlackDeathKill,
				BlackDeathPlayState,
				BlackDeathFirstInfect,
				BlackDeathInfectionState,
				BlackDeathMovementState,
				BlackDeathFirstMovement,
				BlackDeathMortalityState,
				SetVirulenceDescription,
				BlackDeathVictoryCondition,
				BlackDeathPlacementState,
				
			};
		final String BlackDeathStringPairs[][] = 
			{   {"BlackDeath","Black Death"},
				{"BlackDeath_variation","Standard Black Death"},
				{"BlackDeath_family","Black Death"},
				{NoKillMessage,"Nothing can be removed\nClick on Done"},
				{PerfectRollExplanation,"#1{##,##Once,##Twice} per game\nyou can guarantee a winning roll"},
			};


		InternationalStrings.put(BlackDeathStrings);
		InternationalStrings.put(BlackDeathStringPairs);
	*/
	}

}