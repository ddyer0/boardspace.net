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
package lib;


// SortedString is a special purpose sort to put game names into order by date
//instead of the overall games.   For individual games, the end of the name is yyyy-mm-dd-hhmm,
//all digits.  Zip archives have names ending in mmm-d-yyyy where the mmm are alpha and d
//is one of two digits
//
public class SortedString implements Comparable<SortedString>
{	public String str = "";
	String split ;
	public SortedString(String s)
	{ 
		str = (s==null)?"":s;
		split = splitPoint(str);
	}
	String months[] = { "jan","feb","mar","apr","may","jun","jul","aug","sep","oct","nov","dec"};

	String submonthToNumber(String mm)
	{	String m = mm.toLowerCase();
		for(int i=0;i<months.length;i++)
		{
			if(m.startsWith(months[i])) { return(""+(100+i));}	// 3 digits
		}
		return(m);
	}
	
	String splitPoint(String s)
	{	
		int dot = -1;
		int lastdash = -1;
		int l = s.length()-1;
		int n = 4;
		while((l-->0)&&(n>0))
		{	char ch = s.charAt(l);
			switch(ch)
			{
			case '-':
				lastdash = l;
				n--;
				break;
			case '.':
				if(dot==-1) { dot = l; }
				break;
			default:
			}
		}
		if(dot>=0) { s = s.substring(0,dot); }	// discard the extension
		
		switch(n)
		{
		default: 
			return(s);
		case 1:
			{
			// 3 dashes, should be mmm-d-yyyy.zip
			String rest = s.substring(0,lastdash+1);
			String date[] = G.split(s.substring(lastdash+1),'-');
			if(date.length==3)
			{
			String month = date[0];
			String day = date[1];
			String year = date[2];
			if(day.length()==1) { day = "0"+day; }
			month = submonthToNumber(month);
			return(year+"-"+month+"-"+day+"-"+rest);
			}
			return(s);
			}
		case 0:	// we found 4 dashes, must be a plain game name
			return(s.substring(lastdash+1)+s.substring(0,lastdash+1));
		}
		
	}
	public int compareTo(SortedString o) {
		return(split.compareTo(o.split));
	}
}
