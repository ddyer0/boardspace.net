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
package palago;

import lib.OStack;

class LineInfoStack extends OStack<lineInfo>
{
	public lineInfo[] newComponentArray(int n) { return(new lineInfo[n]); }
}

public class lineInfo {
	lineInfo() {}
	boolean loop;
	PalagoCell end1;
	PalagoCell end2;
	int entry1;
	PalagoCell next1;
	PalagoCell next2;
	int length=-1;
	int incomplete_count=0;
	int hoop_count = 0;
	int twotip_count = 0;			// count of "two tip" patterns
	int fourtip_count = 0;			// count of "v" shaped four-tip patterns
	int hole_count = 0;				// count of holes with loops ending in them
	int squeeze_count = 0;			// u shape trying to escape
	boolean win_for_blue = false;
	boolean win_for_yellow = false;
	// storage for the scan
	boolean end_scan_now = false;
	boolean win_result = false;
	public boolean isLoop() { return(loop); }
	
	static final int Extend_Op = 0;
	static final int MapLoop_Op = 1;
	static final int BlueWin_Op = 2;
	static final int YellowWin_Op = 3;
	static final int BlueIncomplete_Op=4;
	static final int YellowIncomplete_Op = 5;
	static final int MapLine_Op = 6;
	static final int SetClock_Op = 7;
	//
	// this traces a line for any of several purposes.  It's used to build
	// the initial state of the lineInfo object, and also to mark participants in loops.
	// the back-and-forth to the PalagoBoard class is so we only need one copy of this
	// slightly complex loop traversal algorithm.
	//
	void traceLine(int op,PalagoCell from,int start,PalagoBoard bb)
	{	if(from==null)
			{ from=end1; 
			  start = entry1;
			  }
		PalagoCell current = from;
		int mod = from.geometry.n;
		PalagoChip top = from.topChip();
		int entry = start;
		end_scan_now = false;
		while(!end_scan_now)
		{	
			int exit = top.nextInLine(entry);				// exit direction from this chip
			bb.visitLine(this,op,current,top,entry,exit);
			int nextEntry = (exit+mod/2)%mod; 				// complementary direction for next entry

			PalagoCell next = current.exitTo(exit);			// next cell
			PalagoChip nextTop = next==null ? null : next.topChip();			// top of next cell
			if((nextEntry==start) && (next == from))		// closed the loop
				{ if(end1==null)
					{ 
					end1 = end2 = from;  	// closed a loop
					entry1 = nextEntry;
					loop = true;
				    if(op==Extend_Op) { length++; }
					}
				end_scan_now = true;
				}
			else if(nextTop==null)									// fell off the grid
				{ if(end1==null) { next1 = next; end1 = current; entry1 = exit; }	// an open line
				  else if(end2==null) { next2=next; end2=current;  }	// other end of a line
				  end_scan_now = true;
				}
			else 
				{ // continue along the line		  
				  entry = nextEntry;
				  current = next;
				  top = nextTop; 
				}
		}	
	}
	public int incompleteCount(PalagoBoard bb,int colorIndex)
	{	incomplete_count = 0;
		twotip_count = 0;
		fourtip_count = 0;
		hoop_count = 0;
		hole_count = 0;
		squeeze_count = 0;
		bb.scan_clock++;
		traceLine(SetClock_Op,null,0,bb);
		traceLine((colorIndex==0)?YellowIncomplete_Op:BlueIncomplete_Op,null,0,bb);
		if((next1!=null) && (next1==next2) && next1.isHole())
			{if((incomplete_count>0)||(hoop_count<0))
			{ hole_count++; } 
			}
		return(incomplete_count);
	}
	public boolean isBlueWin(PalagoBoard bb)
	{	win_result = false;
		if(isLoop())
			{	win_result = true;
				traceLine(BlueWin_Op,null,0,bb);
			}
		win_for_blue = win_result;
		return(win_result);
	}
	public boolean isYellowWin(PalagoBoard bb)
	{	win_result = false;
		if(isLoop())
			{	win_result = true;
				traceLine(YellowWin_Op,null,0,bb);
			}
		win_for_yellow = win_result; 
		return(win_result);
	}
	public String toString()
	{	String win = "";
		if(win_for_blue) { win += " BWin "; }
		if(win_for_yellow) { win += " YWin "; }
		if(twotip_count>0) { win += " tt="+twotip_count+" "; }
		if(fourtip_count>0) { win += " ff="+fourtip_count+" "; }
		if(hole_count>0) { win += " h="+hole_count+" ";}
		if(squeeze_count>0) { win += " s="+squeeze_count+" "; }
		if((next1==null)&&(next2==null)&&(end1==end2)&&(end1!=null))
			{ return("<loop "+win+length+" at "+end1.col+end1.row+">"); }
		if((next1!=null)&&(next2!=null)&&(end1!=null)&&(end2!=null))
			{ return("<line "+win+length+" "+next1.col+next1.row+"-"+end1.col+end1.row+"--"
					+ end2.col+end2.row+"-"+next2.col+next2.row+">");
			}
		return("<line "+next1+end1+end2+next2+">");
	}
	
}
