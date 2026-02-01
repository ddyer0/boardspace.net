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
package online.game;

/**
 * replayMode serves as advice to control some details when executing a move which are
 * not technically part of the move.  Examples are saving animation information, recording
 * details of a move path, or logging information about what the move did.
 * 
 * @author ddyer
 *
 */
public enum replayMode
{	/** live indicates the user is manipulating the UI directly, but the robot is using accelerated move specifiers, 
 	so normally the usert's moves are not animated, but the robot's move are. */
	Live(true,false),
	/** Replay means a high speed replay or a robot move search is in progress, so no animations or extraneous things should be done */
	Replay(false,true), 
	/** replay, do not animate, but do record move path information */
	Replay1(false,true),	
   	/** Single means a single step replay is in progress, so both the user and robot's moves should be animated */
	Single(true,false);
	public boolean animate = false;
	public boolean isReplay = false;
	replayMode(boolean v,boolean r)
	{	isReplay = r;
		animate = v;
	}
}
