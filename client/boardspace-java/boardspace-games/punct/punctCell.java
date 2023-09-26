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
package punct;
import lib.Drawable;
import lib.G;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<punctCell> {
	public punctCell[] newComponentArray(int sz) {
		return(new punctCell[sz]);
	}} 
// 
// specialized cell used for punct
// TODO: modify to use stackCell as the base class
public class punctCell extends cell<punctCell> implements PunctConstants
{ 	
	// constsants once the cell is created
	boolean centerArea = false;		// true for the center 5x5 hex
	// variable state
	public PunctId rackLocation() { return((PunctId)rackLocation); }
	
	public Drawable animationChip(int depth) { return(pieceAt(height-depth)); }

	public punctCell(PunctId id,int idx)
	{
		super(id);
		row = idx;
	}
	PunctPiece pieces[] = new PunctPiece[MAXHEIGHT];
	int height=-1;					// the height of the highest piece, -1 if empty
	int plines[]= new int[2];		// number of movable puncts to this point
	// ephemeral state - no need to copy
	public int sweep_counter=0;
	public int bloBits = 0;	// inclusive or of all blob.bloBits
	public int adjacent2_sweep_counter=0;
	public int blobit_sweep_counter=0;
	public punctBlob blob=null;					// blob containing this cell
	public punctBlob adjacent1=null;			// cells adjacent to the blob are linked
	public punctBlob adjacent2=null;			// cells 2 spaces away are linked
	public void copyAllFrom(punctCell o){ throw G.Error("shouldn't be called");}
	public void copyFrom(punctCell o){ /* this deliberately does nothing */ }
	public boolean sameContents(punctCell other)
	{	//if(height!=other.height) { return(false); }
		//if(plines[0]!=other.plines[0]) { return(false); }
		//if(plines[1]!=other.plines[1]) { return(false); }
		//if(bloBits != other.bloBits) { return(false); }
		//for(int i=0;i<height;i++) { if(pieces[i]!=other.pieces[i]) { return(false); }}
		return(true);
	}
	void copyFrom(punctCell other,PunctPiece allPieces[])
	{	super.copyFrom(other);
		height = other.height;
		plines[0]=other.plines[0];
		plines[1]=other.plines[1];
		bloBits = other.bloBits;
		for(int i=0;i<=height;i++) 
		{ PunctPiece otherpiece = other.pieces[i];
		  if(otherpiece!=null) { pieces[i]=allPieces[otherpiece.id]; }
		}
	}
	// constructor
	punctCell(char tcol,int trow)
	{	super(cell.Geometry.Hex,tcol,trow);
	
		// somewhat ad-hoc marking of the 5x5 center area
		if((col>='G')&&(col<='K'))
		{
			if((row>=7)&&(row<=11))
			{ if(  ((col=='J')&&(row==7)) 
				|| ((col=='K')&&((row==8)||(row==7)))
				|| ((col=='H')&&(row==11))
				|| ((col=='G')&&((row==11)||(row==10))))
			{}
			else { centerArea=true; }
			}
		}
	}

	// reset back to the initial state
	public void reInit() 
		{ height=-1;
		  plines[0]=plines[1]=0;
	      bloBits = 0;
		  blobit_sweep_counter = 0;
		  sweep_counter = 0;
		  adjacent2_sweep_counter=0;
		  for(int i=0;i<MAXHEIGHT;i++) { pieces[i]=null; }
		}
	// true if this is a center hex
	boolean isCenterArea()
	{	return(centerArea);
	}	
	// return the next level available for placement
	int level() { return(height); }
	int level(PunctPiece p) 
	{	if((height>=0) && (pieces[height]==p)) 
			{ int h = height-1;
			  //look down past possible bridges 
			  while((h>=0) && (pieces[h]==null)) { h--; }
			  return(h);
			}
		return(height);
	}
	int nextLevel() 
	{ return(height+1);
	}
	// return the piece at a particular level
	PunctPiece pieceAt(int lvl) { return((lvl>=0)?pieces[lvl]:null); }
	
	// return the top piece, or null
	PunctPiece topPiece() 
	{	if(height>=0)
		{ G.Assert(pieces[height]!=null,"there has to be a piece");
		  return(pieces[height]);
		}
		return(null);
	}
	public chip<?> topChip() 
	{ throw G.Error("Not implemented, need to use standard chip and cell"); 
	}
	PunctPiece topPiece(PunctPiece p) 
	{	if(height>=0)
		{ PunctPiece pp = pieces[height];
		  G.Assert(pieces[height]!=null,"there has to be a piece");
		  if(pp==p) 
		  {	// if the designated piece is the top piece, get the next one down.  This is 
			// so we can test move legality without actually picking up the moving piece.
			int h = height;
		  	while(--h>=0) { pp= pieces[h]; if(pp!=null) { return(pp); }}
		  	return(null);
		  }
		  return(pp);
		}
		return(null);
	}
	// add a piece to the stack at a new top level. 
	void addStack(int level,PunctPiece p)
	{	G.Assert(height<level,"level ok");
		G.Assert(pieces[level]==null,"slot empty");
		pieces[level] = p;
		height=Math.max(height,level);
	}
	// remove a piece from the top of the stack and adjust the height
	void removeStack(int level)
	{	G.Assert(level==height,"remove from the top");
		G.Assert(pieces[height]!=null,"something to remove");
		pieces[height]=null;
		while((--height>=0) && (pieces[height]==null)) {  ; }
	}

	// this is used to maintain the counts of "puncts which could move here"
    // for each cell. 
     void countPcells(int player,int inc)
    {	for(int dir=0;dir<6;dir++) 
    	{ punctCell c = exitTo(dir);
    	  while(c!=null) { c.plines[player] += inc; c=c.exitTo(dir); }
    	}
    }
}
