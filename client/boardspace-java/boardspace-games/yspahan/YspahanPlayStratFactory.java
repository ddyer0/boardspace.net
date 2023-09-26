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

public class YspahanPlayStratFactory implements YspahanPlayStratConst
{
	public static IYspahanPlayStrat getStrategy(int level, int player, int numPlayer, long randomKey)
	{
		IYspahanPlayStrat myStrat = null;
		switch (level)
		{
		case 0:
			myStrat = new YspahanPlayStratDumbot(player, numPlayer,randomKey+numPlayer+player );
			break;

		case 1:
			myStrat = new YspahanPlayStrat8(player, numPlayer,randomKey+numPlayer+player);			
			break;

		case 2:
			myStrat = new YspahanPlayStrat12(player, numPlayer,randomKey+numPlayer+player);			
			break;

		default:
			myStrat = new YspahanPlayStratDumbot(player, numPlayer,randomKey+numPlayer+player);
			break;
		}
		//if (myDebug)
		//{
		//	System.out.println(myStrat);
		//}
		return myStrat;
	}

}
