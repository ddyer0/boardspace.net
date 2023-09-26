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
package lehavre.model;

/**
 *
 *	The <code>OverviewCard</code> class represents the overview card.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.10 2010/11/26
 */
public final class OverviewCard
{
	/** The type of game. */
	private final GameType gameType;

	/** The number of players. */
	private final int playerCount;

	/**
	 *	Creates a new <code>OverviewCard</code> instance
	 *	for the given type of game and number of players.
	 *	@param type the type of game
	 *	@param count the number of players
	 */
	public OverviewCard(GameType type, int count) {
		gameType = type;
		playerCount = count;
	}

	/**
	 *	Returns the type of game.
	 *	@return the type of game
	 */
	public GameType getGameType() {
		return gameType;
	}

	/**
	 *	Returns the number of players.
	 *	@return the number of players
	 */
	public int getPlayerCount() {
		return playerCount;
	}
}