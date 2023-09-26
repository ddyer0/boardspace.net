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
package lehavre.main;

public interface LeHavreConstants {
	/** The order constants. */
	static final int ORDER_DUMP = -1;
	static final int ORDER_CHAT = 0;
	static final int ORDER_LOGIN = 1;
	static final int ORDER_COLOR = 2;
	static final int ORDER_SEAT = 3;
	static final int ORDER_BONUS = 4;
	static final int ORDER_SETTINGS = 5;
	static final int ORDER_READY = 6;
	static final int ORDER_START = 7;
	static final int ORDER_NEXT_TURN = 8;
	static final int ORDER_ROUND_END = 9;
	static final int ORDER_TAKE_OFFER = 10;
	static final int ORDER_ENTER = 11;
	static final int ORDER_BUY = 12;
	static final int ORDER_SELL = 13;
	static final int ORDER_INTEREST = 14;
	static final int ORDER_PAYBACK_LOANS = 15;
	static final int ORDER_RECEIVE = 16;
	static final int ORDER_BAN = 17;
	static final int ORDER_BUILD = 18;
	static final int ORDER_PIRATE = 19;
	static final int ORDER_SWAP = 20;
	static final int ORDER_CHOOSE = 21;
	static final int ORDER_REMOVE = 22;
	static final int ORDER_RESTORE = 23;
	static final int ORDER_TELL_FOOD = 24;
	static final int ORDER_TELL_ENERGY = 25;
	static final int ORDER_RELOGIN = 26;
	static final int ORDER_UPDATE = 27;
	static final int ORDER_TAKE_LOANS = 28;
	static final int ORDER_SETSTATE = 29;

	/** The maximum amount of players. */
	static final int MAX_PLAYERS = 5;
}
