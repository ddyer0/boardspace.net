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
package snakes;

import lib.G;
import lib.Random;

class Coverage implements SnakesConstants
{	CellType type = CellType.blank;
	TileType role = null;
	boolean exits[] = new boolean[4];
	// constructors
	Coverage() {}
	public String toString() { return("<cv "+type+" "+(exits[0]?" l":"")+(exits[1]?" u":"")+(exits[2]?" r":"")+(exits[3]?"d":"")+">"); }
	Coverage(CellType typ,TileType ro,boolean x0, boolean x1, boolean x2, boolean x3)
	{	type = typ;
		role = ro;
		exits[0] = x0;
		exits[1] = x1;
		exits[2] = x2;
		exits[3] = x3;
	}
	public void reInit() 
	{	type = CellType.blank;
		role = null;
		exits[0]=exits[1]=exits[2]=exits[3]=false;
	}

	public long Digest(Random r)
	{	return(r.nextLong()*
				((type.ordinal()<<4)
				+(exits[0]?1:0)
				+(exits[1]?2:0)
				+(exits[2]?4:0)
				+(exits[3]?8:0)));
	}
	public void copyFrom(Coverage other)
	{	type = other.type;
		role = other.role;
		boolean ox[] = other.exits;
		exits[0]=ox[0];
		exits[1]=ox[1];
		exits[2]=ox[2];
		exits[3]=ox[3];
	}
	// change the coverage map.  If this cell is already blank, or the new one is blank.
	public void addChip(Coverage other,int rotation)
	{	if(other.type==CellType.blank) {return; }	// no change in state
		G.Assert(type==CellType.blank, "cell coverage is not blank");
		type = other.type;
		role = TileType.getPlacedRole(other.role,rotation);

		boolean ox[] = other.exits;
		exits[0]=ox[(4-rotation)%4];
		exits[1]=ox[(5-rotation)%4];
		exits[2]=ox[(6-rotation)%4];
		exits[3]=ox[(7-rotation)%4];
	}
	public boolean sameContents(Coverage other)
	{	if(type!=other.type) { return(false); }
		boolean ox[] = other.exits;
		return( (exits[0]==ox[0])
				&&(exits[1]==ox[1])
				&&(exits[2]==ox[2])
				&&(exits[3]==ox[3]));
	}

static Coverage coverMap[][] = {
		null,null,	// dummies for the tiles
//
// 16 snakes, 8 "twos" and 8 "threes"
// for uniformity, all are represented as "4's" with four boxes
// clockwise from the lower left.
// each cell is either head, tail, body or blank
// and has state for 4 sides, left top right and bottom, each
// is either an exit or not.
// heads and tails have 1 true exit, body have 2 true exits.
//
			{// snake 0, head and tail exit right
			new Coverage(CellType.head,TileType.vertical,false,false,true,false),
			new Coverage(CellType.tail,TileType.vertical,false,false,true,false),
			new Coverage(CellType.blank,null,false,false,false,false),
			new Coverage(CellType.blank,null,false,false,false,false)
			},
			{	// snake 1 2 body seqments
				new Coverage(CellType.body,TileType.vertical,false,true,true,false),
				new Coverage(CellType.body,TileType.vertical,true,false,false,true),
				new Coverage(CellType.blank,null,false,false,false,false),
				new Coverage(CellType.blank,null,false,false,false,false)				
			},
			{	// snake 2 head at top, body below
				new Coverage(CellType.body,TileType.vertical,false,true,false,true),
				new Coverage(CellType.head,TileType.vertical,false,false,false,true),
				new Coverage(CellType.blank,null,false,false,false,false),
				new Coverage(CellType.blank,null,false,false,false,false)				
			},
			{	// snake 3 tail above head below connected
				new Coverage(CellType.head,TileType.vertical,false,true,false,false),
				new Coverage(CellType.tail,TileType.vertical,false,false,false,true),
				new Coverage(CellType.blank,null,false,false,false,false),
				new Coverage(CellType.blank,null,false,false,false,false)				
			},
			{	// snake 4 u shape pointing left
				new Coverage(CellType.body,TileType.vertical,true,true,false,false),
				new Coverage(CellType.body,TileType.vertical,true,false,false,true),
				new Coverage(CellType.blank,null,false,false,false,false),
				new Coverage(CellType.blank,null,false,false,false,false)				
			},
			{	// snake 5 L shape pointing left
				new Coverage(CellType.body,TileType.vertical,false,true,true,false),
				new Coverage(CellType.body,TileType.vertical,false,true,false,true),
				new Coverage(CellType.blank,null,false,false,false,false),
				new Coverage(CellType.blank,null,false,false,false,false)				
			},
			{	// snake 6 tail descending
				new Coverage(CellType.tail,TileType.vertical,false,true,false,false),
				new Coverage(CellType.body,TileType.vertical,false,true,false,true),
				new Coverage(CellType.blank,null,false,false,false,false),
				new Coverage(CellType.blank,null,false,false,false,false)				
			},
			{	// snake 7 body in back pointing L
				new Coverage(CellType.body,TileType.vertical,true,true,false,false),
				new Coverage(CellType.body,TileType.vertical,false,true,false,true),
				new Coverage(CellType.blank,null,false,false,false,false),
				new Coverage(CellType.blank,null,false,false,false,false)				
			},
			// 3 cell snakes
			{	// snake 8 descending body plus curve
				new Coverage(CellType.body,TileType.edge,false,true,false,true),
				new Coverage(CellType.body,TileType.center,false,true,false,true),
				new Coverage(CellType.body,TileType.edge,false,false,true,true),
				new Coverage(CellType.blank,null,false,false,false,false)				
			},
			{	// snake 9 1 body descending blank then inverted S
				new Coverage(CellType.body,TileType.edge,false,true,false,true),
				new Coverage(CellType.blank,null,false,false,false,false),
				new Coverage(CellType.body,TileType.edge,true,false,false,true),
				new Coverage(CellType.body,TileType.center,false,true,true,false)				
			},
			{	// snake 10 1 curve then tail descending joined
				new Coverage(CellType.body,TileType.edge,false,true,true,false),
				new Coverage(CellType.blank,null,false,false,false,false),
				new Coverage(CellType.tail,TileType.edge,false,false,false,true),
				new Coverage(CellType.body,TileType.center,true,true,false,false)			
			},
			{	// snake 11 ascending body + right turn
				new Coverage(CellType.body,TileType.edge,false,true,false,true),
				new Coverage(CellType.body,TileType.center,false,false,true,true),
				new Coverage(CellType.body,TileType.edge,true,false,true,false),
				new Coverage(CellType.blank,null,true,true,false,false)			
			},
			{	// snake 12 right segment and curve
				new Coverage(CellType.body,TileType.edge,true,false,true,false),
				new Coverage(CellType.blank,null,false,false,false,false),
				new Coverage(CellType.body,TileType.edge,true,true,false,false),
				new Coverage(CellType.head,TileType.center,true,false,false,false)			
			},
			{	// snake 13 sideways U pointing right
				new Coverage(CellType.body,TileType.edge,false,true,true,false),
				new Coverage(CellType.body,TileType.center,false,false,true,true),
				new Coverage(CellType.body,TileType.edge,true,false,true,false),
				new Coverage(CellType.blank,null,false,false,false,false)			
			},
			{	// snake 14 sideways U pointing up
				new Coverage(CellType.body,TileType.edge,false,true,true,false),
				new Coverage(CellType.blank,null,false,false,false,false),			
				new Coverage(CellType.body,TileType.edge,false,true,false,true),
				new Coverage(CellType.body,TileType.center,true,true,false,false)
			},
			{	// snake 15 horizontal snake plus curve
				new Coverage(CellType.body,TileType.edge,false,false,true,true),
				new Coverage(CellType.head,TileType.center,false,false,true,false),			
				new Coverage(CellType.tail,TileType.edge,true,false,false,false),
				new Coverage(CellType.blank,null,false,false,false,false)
			}
	};
}