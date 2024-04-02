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
package gametimer;

import lib.CellId;
import lib.InternationalStrings;
import online.game.BaseBoard.BoardState;

/**
 * Constants for the game.  This is set for a 2 player game, change to Play6Constants for a multiplayer game
 * @author Ddyer
 *
 */

public interface GameTimerConstants
{	
//	these next must be unique integers in the GameTimerMovespec dictionary
//  they represent places you can click to pick up or drop a stone
	enum GameTimerId implements CellId
	{	Pause,
		Resume, 
		SetPlayer,
		Endgame
		;
	}

//
// states of the game
//
public enum GameTimerState implements BoardState,GameTimerConstants
{
	Puzzle(PuzzleStateDescription,false,false),
	Gameover(GameOverStateDescription,false,false),
	Resign(ResignStateDescription,true,false),
	Confirm(ConfirmStateDescription,true,true),
	Paused(PausedStateDescription,true,true),
	Running(RunningStateDescription,true,true);
	
	GameTimerState(String des,boolean done,boolean digest)
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
	public boolean simultaneousTurnsAllowed() { return(false); }
};

	static final String EndGameMessage = "End Game";
	static final String RunningStateDescription = "game in progress";
	static final String PausedStateDescription = "game paused";
	static final String PauseMessage = "Pause";
	static final String ResumeMessage = "Resume";
	static void putStrings()
	{
		String GameStrings[] = 
		{  "GameTimer",
			RunningStateDescription,
			PauseMessage,
			ResumeMessage,

		};
		String GameStringPairs[][] = 
		{   
		};
		InternationalStrings.put(GameStrings);
		InternationalStrings.put(GameStringPairs);
		
	}


}