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
package micropul;
import lib.Random;
import micropul.MicropulConstants.MicroId;
import lib.Drawable;
import lib.G;
import lib.OStack;
import online.game.stackCell;
import online.game.PlacementProvider;
import online.game.cell;

class CellStack extends OStack<MicropulCell>
{
	public MicropulCell[] newComponentArray(int n) { return(new MicropulCell[n]); }
}
//
// specialized cell used for the this game.
//

// games commonly add a more complex structue.   
//
public class MicropulCell extends stackCell<MicropulCell,MicropulChip> implements PlacementProvider
{	
    static final char playerColors[]={'R','B'};

	public MicropulChip[] newComponentArray(int n) { return(new MicropulChip[n]); }
	int sweep_counter;		// the sweep counter for which blob is accurate
	int rotation[] = new int[STARTING_CHIP_HEIGHT];
	int jewelStatus[] = new int[4];
	public int player = -1;
	public int lastPicked = -1;
	public int lastDropped = -1;
	MicropulCell nextOccupied = null;
	boolean masked = false;	// true if contents painted as blank
	static final int Jewel_None = 0;
	static final int Jewel_Claimed_1 = 1;
	static final int Jewel_Claimed_2 = 2;
	static final int Jewel_Closed = 4;
	static final int Jewel_Poisoned = 8;
	String owner = "";
	public void clearJewels() { for(int i=0;i<4;i++) { jewelStatus[i]=Jewel_None; }}

	public Drawable animationChip(int depth)
	{
		Drawable ac = super.animationChip(depth);
		if((ac!=null) && (rackLocation()==MicroId.Supply)) { return(MicropulChip.SQUARE); }
		return(ac);
	}
	public int animationSize(int s) 
	{ 	int last = lastSize();
		return ((last>3) ? last : s);
	}
	public void copyFrom(MicropulCell ot)
	{	
		nextOccupied = null;
		super.copyFrom(ot);
		lastPicked = ot.lastPicked;
		lastDropped = ot.lastDropped;
		while(chipIndex>=0){ chipStack[chipIndex--]=null; }
		for(int i=0;i<=ot.chipIndex;i++)
		{	addChip(ot.chipStack[i],ot.rotation[i]);
		}

	}
	public void markJewels(MicropulBoard b,int quad,int jcode,int mtype)
	{	MicropulChip ch = bottomChip();
		if(ch==null) 
			{ if(sweep_counter!=b.sweep_counter) 
				{ // count exit cells
				  b.micropulExtensions++; 
				  sweep_counter = b.sweep_counter; 
				}
			  return; 
			}
		int rot = bottomRotation();
		int pip = ch.getComponent((4+quad-rot)%4);
		int piptype = MicropulChip.micropulType(pip);
		if( (piptype==mtype) && (jewelStatus[quad]&jcode)==0)
		{
		jewelStatus[quad] |= jcode;

		if(pip>=MicropulChip.BIG_CODE) 	// count marked cells 
			{ if(quad==0) {		b.micropulCells++;	}}
			else { b.micropulCells++; }
		
		// mark jewels toward left
		switch(quad)
		{	
		case 0: 
		case 3:	
			MicropulCell nextc = exitTo(MicropulBoard.CELL_LEFT());
			nextc.markJewels(b,(quad==0)?1:2,jcode,mtype);
			break;
		case 1:	markJewels(b,0,jcode,mtype);
			break;
		case 2: markJewels(b,3,jcode,mtype);
			break;
		default:
			break;
		}
		
		// mark jewels up
		switch(quad)
		{	
		case 0: 
		case 1:	
			MicropulCell nextc = exitTo(MicropulBoard.CELL_UP());
			nextc.markJewels(b,(quad==0)?3:2,jcode,mtype);
			break;
		case 2:	markJewels(b,1,jcode,mtype);
			break;
		case 3: markJewels(b,0,jcode,mtype);
			break;
		default:
			break;
		}

		// mark jewels right
		switch(quad)
		{	
		case 1: 
		case 2:	
			MicropulCell nextc = exitTo(MicropulBoard.CELL_RIGHT());
			nextc.markJewels(b,(quad==1)?0:3,jcode,mtype);
			break;
		case 0:	markJewels(b,1,jcode,mtype);
			break;
		case 3: markJewels(b,2,jcode,mtype);
			break;
		default:
			break;
		}
		
		// mark jewels down
		switch(quad)
		{	
		case 2: 
		case 3:	
			MicropulCell nextc = exitTo(MicropulBoard.CELL_DOWN());
			nextc.markJewels(b,(quad==2)?1:0,jcode,mtype);
			break;
		case 0:	markJewels(b,3,jcode,mtype);
			break;
		case 1: markJewels(b,2,jcode,mtype);
			break;
		default:
			break;
		}
		} 
	}
	public boolean tintClosed(int quad)
	{	return((jewelStatus[quad]&Jewel_Closed)!=0);
	}
	public MicropulChip tintCode(int quad)
	{	int status = jewelStatus[quad];
		switch(status)
		{
		default: return(MicropulChip.getTint(2));
		case Jewel_None:	return(null);
		case Jewel_Claimed_1|Jewel_Closed:
		case Jewel_Claimed_1: return(MicropulChip.getTint(0));
		case Jewel_Claimed_2|Jewel_Closed:
		case Jewel_Claimed_2: return(MicropulChip.getTint(1));
		}
	}
	
	// flood fill the adjacent micropul to this cell's jewel
	// count the size and number of open extensions
	public void markJewels(MicropulBoard b)
	{	int height = chipIndex;
		for(int h=1;h<=height;h++)
			{
			int rot = rotation[h];
			MicropulChip top = chipStack[h];
			G.Assert(top.isJewel(),"is a jewel");
			markJewels(b,top,rot);
			}
	}
	public void markJewels(MicropulBoard b,MicropulChip top,int rot)
	{
		MicropulChip ch = bottomChip();
		G.Assert(ch!=null,"has a bottom chip");
		int brot = bottomRotation();
		int pip = ch.getComponent((4+rot-brot)%4);
		int mtype =MicropulChip.micropulType(pip);
		int jowner = b.jewelOwner(top);
		int jcode = (jowner==0) ? Jewel_Claimed_1 : Jewel_Claimed_2;
		int status = jewelStatus[rot];
		if(status!=Jewel_None)
		{ jcode |= Jewel_Poisoned;	// mark as poisoned by multiple jewels
		}
		b.sweep_counter++;
		b.micropulCells=0;
		b.micropulExtensions=0;
		markJewels(b,rot,jcode,mtype);
		if(b.micropulExtensions==0)
		{ if(status==Jewel_None) { b.claimedMicropul[jowner] += b.micropulCells; } 
		  markJewels(b,rot,Jewel_Closed,mtype);
		}
		else if(status==Jewel_None)
		{ b.unclaimedMicropul[jowner] += b.micropulCells;
		  b.claimExtensions[jowner] += b.micropulExtensions;
		}
		if((status!=Jewel_None) && ((status&Jewel_Poisoned)==0))
		{	// if the second time in a poisoned group, subtract the result from the first time
			int first_pass_owner = ((status&Jewel_Claimed_1)!=0) ? 0 : 1;
			if(b.micropulExtensions==0) { b.claimedMicropul[first_pass_owner] -= b.micropulCells; }
			else 
			{ b.unclaimedMicropul[first_pass_owner] -= b.micropulCells;
			  b.claimExtensions[first_pass_owner] -= b.micropulExtensions;
			  if(( b.unclaimedMicropul[first_pass_owner])<0) {  b.unclaimedMicropul[first_pass_owner] = 0; }
			  if((b.claimExtensions[first_pass_owner])<0) { b.claimExtensions[first_pass_owner] = 0; }
			}
		}
		
	}
	public void reInit() 
		{ super.reInit(); 
		  for(int i=0;i<4;i++) { jewelStatus[i]=Jewel_None; }
		  lastPicked = -1;
		  lastDropped = -1;
		  nextOccupied = null;
		  masked = false;
		}
	public MicropulCell(Random r) { super(r); }		// construct a cell not on the board
		
	public MicropulCell(char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Square,c,r);
		rackLocation = MicroId.BoardLocation;
	};
	// constructor a cell not on the board. 
	public MicropulCell(Random r,MicroId loc,int play)
	{	super(r);
		rackLocation = loc;
		player = play;
		owner = Micropulmovespec.D.findUnique(play);
		col = playerColors[play];
		onBoard=false;
	}
	
	public MicropulCell(Random r,MicroId loc,int rr , int play)
	{	super();
		rackLocation = loc;
		player = play;
		owner = Micropulmovespec.D.findUnique(play);
		col = playerColors[play];
		row =  rr;
		onBoard=false;
	}
	public MicroId rackLocation() { return((MicroId)rackLocation); }
	public boolean sameRotations(MicropulCell other)
	{
		for(int i=0;i<chipIndex;i++)
		{ if(rotation[i]!=other.rotation[i]) { return(false); }
		}
		return(true);
	}
	public void addChip(MicropulChip item)
	{	super.addChip(item);		// expand stack if needed
		if(chipIndex>=rotation.length) 
		{	int newrot[] = new int[rotation.length+STARTING_CHIP_HEIGHT];
			for(int i=0;i<rotation.length;i++) { newrot[i]=rotation[i]; }
			rotation = newrot;
		}
	}
	public void addChip(MicropulChip item,int rot)
	{	addChip(item);
		rotation[chipIndex]=rot;
	}
	
	public boolean sameCell(MicropulCell other)
	{	return(super.sameCell(other)
				&& sameRotations(other));
	}
	public long DigestRot()
	{	long val = 0;
		for(int i=0;i<=chipIndex;i++) 
	  	{ if(onBoard) { val += chipStack[i].Digest()*rotation[i]; }
	  		else 
	  		{ // rotation doesn't matter for chips in the racks
	  			val += chipStack[i].Digest(); 
	  		}
	  	}
		return(val);
	}
	public long Digest(Random r) 
	{ return(super.Digest(r) + DigestRot());
	}
	// return the top (actually, only) chip. Cast to the correct type
	// here so no one else needs to.
	public MicropulChip bottomChip() { return(chipAtIndex(0)); }
	public int topRotation() { return((chipIndex>=0)?rotation[chipIndex]:-1); }
	public int bottomRotation() { return((chipIndex>=0)?rotation[0]:-1); }
	public MicropulChip getChipAt(int idx) 
		{ return((chipIndex<idx)?null:(MicropulChip)super.chipAtIndex(idx));
		}
	public int getRotationAt(int idx)
		{ return((chipIndex<idx)?-1:rotation[idx]);
		}

	public int getLastPlacement(boolean empty) {
		return empty ? lastPicked : lastDropped;
	}
}
