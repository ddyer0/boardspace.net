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
package frogs;

import lib.Random;
import frogs.FrogConstants.FrogId;
import lib.G;
import lib.OStack;
import online.game.*;
class CellStack extends OStack<FrogCell>
{
	public FrogCell[] newComponentArray(int n) { return(new FrogCell[n]); }
}
//
// specialized cell used for the this game.
//
public class FrogCell extends stackCell<FrogCell,FrogPiece> implements PlacementProvider
{	
	public int sweep_counter=0;		// used checking for valid hives
	public int sweep_connected=0;	// second sweeper used to check connectivity
	
	int lastPicked = -1;
	int lastDropped = -1;
	// support for numberMenu
	public int getLastPlacement(boolean empty) {
		return empty ? lastPicked : lastDropped;
	}
	public void reInit()
	{
		super.reInit();
		lastDropped = -1;
		lastPicked = -1;
	}
	public void copyFrom(FrogCell c)
	{
		super.copyFrom(c);
		lastDropped = c.lastDropped;
		lastPicked = c.lastPicked;
	}
	public FrogPiece[] newComponentArray(int n) { return(new FrogPiece[n]); }
	
	public int animationSize(int s) 
	{ 	int last = lastSize();
		return ((last>3) ? last : s);
	}
	public boolean canChangeConnectivity()
	{	int d = 0;
		for(int dir=0;dir<6;dir++) 
			{ FrogCell adj = exitTo(dir); 
			  d |= (adj.topChip()==null)?0:1;
			  d = d<<1;
			}
		return(connectivity_change_map[d>>1]);
	}
	// table TRUE if this neighborhood could change connectivity if the center is removed.
	static boolean connectivity_change_map[] = {
		false,	//0 = 000000
		false,	//1 = 000001
		false,	//2 = 000010
		false,	//3 = 000011
		false,	//4 = 000100
		true,	//5 = 000101
		false,	//6 = 000110
		false,	//7,= 000111
		false,	//8 = 001000
		true,	//9 = 001001
		true,	//10= 001010
		true,	//11= 001011
		false,	//12= 001100
		true,	//13= 001101
		false,	//14= 001110
		false,	//15= 001111
		false,	//16= 010000
		true,	//17= 010001
		true,	//18= 010010
		true,	//19= 010011
		true,	//20= 010100
		true,	//21= 010101
		true,	//22= 010110
		true,	//23= 010111
		false,	//24= 011000
		true,	//25= 011001
		true,	//26= 011010
		true,	//27= 011011
		false,	//28= 011100
		true,	//29= 011101
		false,	//30= 011110
		false,	//31= 011111
		false,	//32= 100000
		false,	//33= 100001 first and last bits
		true,	//34= 100010
		false,	//35= 100011
		true,	//36= 100100
		true,	//37= 100101
		true,	//38= 100110
		false,	//39= 100111
		true,	//40= 101000
		true,	//41= 101001
		true,	//42= 101010
		true,	//43= 101011
		true,	//44= 101100
		true,	//45= 101101
		true,	//46= 101110
		false,	//47= 101111
		false,	//48= 110000
		false,	//49= 110001
		true,	//50= 110010
		false,	//51= 110011
		true,	//52= 110100
		true,	//53= 110101
		true,	//54= 110110
		false,	//55= 110111
		false,	//56= 111000
		false,	//57= 111001
		true,	//58= 111010
		false,	//59= 111011
		false,	//60= 111100
		false,	//61= 111101
		false,	//62= 111110
		false	//63= 111111
	};
		
	static {
	// a rudimentary check on the above map - require that x and ~x have the same change profile
	for(int i=0;i<32;i++) 
		{ G.Assert(connectivity_change_map[i]==connectivity_change_map[i^0x3f],
				"complementary map matches");
		}
	}

	public FrogCell exitLine(int dir,FrogCell empty) 
	{	FrogCell diskrect = exitTo(dir);
		if((diskrect==empty) || (diskrect.topChip()==null)) { return(diskrect); }
		else { return(diskrect.exitLine(dir,empty)); }
	}
	// constructor for cell on board
	public FrogCell(char c,int r,FrogId loc)
	{	super(cell.Geometry.Hex,c,r);
		rackLocation = loc;
	}
	public FrogCell(Random rv,FrogId loc,int r)
	{	super(rv);
		rackLocation = loc;
		onBoard=false;
		row = r;
		col = '@';
	}
	public FrogCell(Random rv,FrogId loc)
	{	super(rv,loc);
	}
	public FrogId rackLocation() { return((FrogId)rackLocation); }
	public static boolean sameCell(FrogCell a,FrogCell b)
	{
		return((a==null)
				?(b==null)
				:a.sameCell(b));
	}
}
