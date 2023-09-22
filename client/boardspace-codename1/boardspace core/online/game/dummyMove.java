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
 * this is a class used to insert messages into the raw move history
 * of a game.
 * @author ddyer
 *
 */
public class dummyMove extends commonMove {
	public String message;
	public dummyMove(String str)
	{	message = str;
	}
	public commonMove Copy(commonMove to) {
		if(to==null) { to=new dummyMove(""); }
		dummyMove meto = (dummyMove)to;
		meto.message = message;
		return(to);
	}

	public boolean Same_Move_P(commonMove other) {
		//  Auto-generated method stub
		dummyMove me = (dummyMove)other;
		return((message==null) ? (me.message==null) : message.equals(me.message));
	}

	public String moveString() {
		//  Auto-generated method stub
		return(message);
	}

	public String shortMoveString() {
		//  Auto-generated method stub
		return(message);
	}

}
