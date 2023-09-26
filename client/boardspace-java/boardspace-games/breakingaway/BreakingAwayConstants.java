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
package breakingaway;

import lib.CellId;
import lib.InternationalStrings;
import lib.OStack;
import online.game.BaseBoard.BoardState;


public interface BreakingAwayConstants
{	
	static final String PlayerDoneAdjusting = "Done Adjusting";
	static final String AnimateHelp = "Smoothly animate the game";
	static final String AnimateMessage = "Animate";
	static final String DropMessage = "Drop this rider from the Race";
	static final String AdjustMessage = "Adjust";
	static final String GoalMessage = "Score the most points at the sprint and finish lines";
	static final String RiderSize = "Rider Size";
	static final String FinishMessage = "#1 riders have finished";
	static final String FinishBonusMessage = "The next finish bonus is #1";
	static final String SprintBonusMessage = "The next sprint bonus is #1";
	static final String SecondSprintMessage = "#1 riders have crossed the second sprint line.";
	static final String FirstSprintMessage = "#1 riders have crossed the first sprint line.";
	static final String MoveMessage = "Move a Cyclist";
	static final String AdjustStateMessage = "Adjust your starting movements";
	static final String WaitStateMessage = "Wait for other players to adjust";
	static final String ConfirmDropMessage = "Confirm Dropping this rider";
    //	these next must be unique integers in the dictionary

	static final String ServiceName = "Rider movement for #1";

    enum Variation
    {	
    	Standard("breakingaway"),
    	Staggered("breakingaway-ss");	// stagger start
    	String name;
    	Variation(String n) { name=n; }
       	static Variation findVariation(String n)
    	{	
    		for(Variation v : values()) { if(v.name.equalsIgnoreCase(n)) { return(v); }}
    		return(null);
    	}
    }

    
class StateStack extends OStack<BreakState>
{
	public BreakState[] newComponentArray(int n) { return(new BreakState[n]); }
} 
public enum BreakState implements BoardState
{	PUZZLE_STATE(PuzzleStateDescription),
	RESIGN_STATE(ResignStateDescription),
	GAMEOVER_STATE(GameOverStateDescription),
	CONFIRM_STATE(ConfirmStateDescription),
	PLAY_STATE(MoveMessage),
	ADJUST_MOVEMENT_STATE(AdjustStateMessage),
	DONE_ADJUSTING_STATE(WaitStateMessage),
	CONFIRM_DROP_STATE(ConfirmDropMessage);

	String description;
	BreakState(String des) { description = des; }
	public String getDescription() { return(description); }
	public boolean GameOver() { return(this==GAMEOVER_STATE); }
	public boolean Puzzle() { return(this==PUZZLE_STATE); }
	public boolean simultaneousTurnsAllowed() { return(this==ADJUST_MOVEMENT_STATE); }
}

 

public enum BreakId implements CellId
{	
	BoardLocation,
	Rider,
	ZoomSlider,
	PlusOne,
	MinusOne,
	HitAdjustButton,
	SelectRider,
	DropRider,
	Animate,
	BackwardTime,
	ForwardTime,
	HitBubble,
	ReverseViewButton,
	HideInfoButton,
	HidePlayerInfoButton,
  	PlayerDone,
       	;
	public String shortName() { return(name()); }
}
 
     static void putStrings()
     {
    	     String BreakingAwayStrings[] = {
    	        	AnimateHelp,
    	        	AnimateMessage,
    	        	DropMessage,
    	            AdjustMessage,
    	            GoalMessage,
    	            RiderSize,
    	            FinishMessage,
    	            FinishBonusMessage,
    	            SprintBonusMessage,
    	            SecondSprintMessage,
    	            FirstSprintMessage,
    	            MoveMessage,
    	            AdjustStateMessage,
    	            WaitStateMessage,
    	            ConfirmDropMessage,
    	            PlayerDoneAdjusting,
    	            
    	            ServiceName,
    	     
    	        };
    	         String BreakingAwayStringPairs[][] = {
    	        		{"BreakingAway","Breaking Away"},
    	        		{"BreakingAway_variation","linear start"},
    	        		{"BreakingAway_family","Breaking Away"},
    	        		{"BreakingAway-ss","Breaking Away-S"},
    	        		{"BreakingAway-ss_variation","stagger start"},
    	        };
    	        InternationalStrings.put(BreakingAwayStringPairs);
    	        InternationalStrings.put(BreakingAwayStrings);
     }
    
}