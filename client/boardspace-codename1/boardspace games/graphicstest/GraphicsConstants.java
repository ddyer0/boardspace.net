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
package graphicstest;

import lib.CellId;
import lib.InternationalStrings;
import lib.OStack;
import online.game.BaseBoard.BoardState;
import online.game.BaseBoard.StateRole;

/**
 * Constants for the game.  This is set for a 2 player game, change to Play6Constants for a multiplayer game
 * @author Ddyer
 *
 */

public interface GraphicsConstants
{	
//	these next must be unique integers in the GraphicsMovespec dictionary
//  they represent places you can click to pick up or drop a stone
	enum PrototypeId implements CellId
	{

	
	}

class StateStack extends OStack<GraphicsState>
{
	public GraphicsState[] newComponentArray(int n) { return(new GraphicsState[n]); }
}
//
// states of the game
//
public enum GraphicsState implements BoardState,GraphicsConstants
{
	Playing(StateRole.Play,"running the tests",false,false);
	
	GraphicsState(StateRole r,String des,boolean done,boolean digest)
	{	role = r;
		description = des;
		digestState = digest;
		doneState = done;
	}
	boolean doneState;
	boolean digestState;
	String description;
	public String description() { return(description); }
	StateRole role;
	public StateRole getRole() { return role; }

	public boolean doneState() { return(doneState); }
	public boolean digestState() { return(digestState); }
	public boolean simultaneousTurnsAllowed() { return(false); }
};
 
 enum PrototypeVariation
    {
    	prototype("graphicstest");
    	String name ;
    	// constructor
    	PrototypeVariation(String n) 
    	{ name = n; 
    	}
    	// match the variation from an input string
    	static PrototypeVariation findVariation(String n)
    	{
    		for(PrototypeVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
    		return(null);
    	}
     	
    }
	
	static void putStrings()
	{
		String GameStrings[] = 
		{  
		};
		InternationalStrings.put(GameStrings);
		
	}


}