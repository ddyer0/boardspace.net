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
package yspahan;

import java.util.ArrayList;

public interface IYspahanPlayStrat {

	/***
	 * If you are the start player of a round, you can decide to use additional yellow dices!
	 * 
	 */
	public abstract void throwDices();

	public abstract void doMoveMain();
	
	public abstract void cardsAfterMoveMainAction();

	public abstract void doMoveBuilding(); // end do move
	
	public abstract void copyBoardState(YspahanBoard bd, boolean[] robots); 
	
	public abstract ArrayList<YspahanMovespec> getMoveList();

	public abstract boolean supervisorSendCubeToCaravan(YspahanCell cell);

	public abstract void supervisorDesignateCube();

}