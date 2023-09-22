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
package online.common;
/**
 * LaunchUser is a bridge between the lobby's information about players
 * and the game setup engine.
 * @author Ddyer
 *
 */
public class LaunchUser
{	public boolean primaryUser;		// true if this is a primary seat using the gui
	public boolean otherUser;			// true if this is a secondary seat using the gui
	public int seat;			// positional seat in the game
	public int order;			// order of play in the game.  Not related to seat position.
	public String host;			// unique host name, same if playing from the same screen.
	public String ranking;		// ranking for this game
	public User user;			// the full lobby user info table
	
    public String toString()
    {
    	return("<lu "+user.prettyName()+"@"+host+" s "+seat+" o "+order+">");
    }
}
