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
package rithmomachy;

import lib.CompareTo;
import lib.IStack;
import lib.InternationalStrings;
import lib.OStack;
import rithmomachy.RithmomachyConstants.Wintype;

class WinStack extends OStack<WinDescription>
{
	public WinDescription[] newComponentArray(int n) { return(new WinDescription[n]); }
}
/**
 * RithmomachyBoard knows all about the game of Rithmomachy, which is played
 * on a 7x7 board. It gets a lot of logistic support from 
 * common.rectBoard, which knows about the coordinate system.  
 * 
 * This class doesn't do any graphics or know about anything graphical, 
 * but it does know about states of the game that should be reflected 
 * in the graphics.
 * 
 *  The principle interface with the game viewer is the "Execute" method
 *  which processes moves.  Note that this
 *  
 *  In general, the state of the game is represented by the contents of the board,
 *  whose turn it is, and an explicit state variable.  All the transitions specified
 *  by moves are mediated by the state.  In general, my philosophy is to be extremely
 *  restrictive about what to allow in each state, and have a lot of tripwires to
 *  catch unexpected transitions.   We expect to be fed only legal moves, but mistakes
 *  will be made and it's good to have the maximum opportunity to catch the unexpected.
 *  
 * Note that none of this class shows through to the game controller.  It's purely
 * a private entity used by the viewer and the robot.
 * 
 * @author ddyer
 *
 */

public class WinDescription implements CompareTo<WinDescription>
{	int anchor=0;
	int pivot=0;
	int end=0;
	RithmomachyCell anchorCell;
	RithmomachyCell pivotCell;
	RithmomachyCell endCell;
	String annotation=null;
	Wintype type=Wintype.harmonic; 
	int key() { return(anchor*1000*1000+pivot*1000+end); }
	public WinDescription(int a,int b, int c, String des)
	{
		anchor=a;
		pivot = b;
		end = c;
		annotation = des;
	}
	WinDescription copy() 
	{ 	WinDescription n = new WinDescription(anchor,pivot,end,annotation);
		n.pivotCell = pivotCell;
		n.endCell = endCell;
		n.anchorCell = anchorCell;
		n.type = type;
		return(n);
	}
	public WinDescription() {};
	
	public int compareTo(WinDescription other)
	{	return(Integer.signum(key()-other.key()));
	}
	public int altCompareTo(WinDescription other)
	{	return(Integer.signum(other.key()-key()));
	}
	public String dString()
	{	return(""+anchor+" "+pivot+" "+end+((annotation==null)?"":annotation));
	}
	private String iString(IStack standardNumbers,int n)
	{
		return(standardNumbers.contains(n)?""+n:("("+n+")"));
	}
	public String dString(IStack standardNumbers)
	{
		return(iString(standardNumbers,anchor)+" "+iString(standardNumbers,pivot)+" "+iString(standardNumbers,end));
	}
	public String toString()
	{
		return(""+type+" "+dString());
	}

	String getDescription(InternationalStrings s)
	{	return(s.get(type.prettyName()) + " : " + anchor + " "+pivot + " "+end); 
	}
}