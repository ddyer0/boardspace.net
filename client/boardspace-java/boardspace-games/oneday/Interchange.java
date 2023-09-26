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
package oneday;

import lib.G;
import oneday.OnedayBoard.OnedayLocation;

public class Interchange {
	Station station;
	Line from;
	Line to;
	double timeInMinutes;
	public Interchange(Station sta,Line fromline,Line toline,double timed)
	{
		station = sta;
		from = fromline;
		to = toline;
		timeInMinutes = timed;
	}
	public Interchange(String sta,String fromLine,String toLine,String timeString)
	{	if("aldgate east".equals(sta)) { sta = "aldgate"; }
		station = Station.getStation(sta);
		from = Line.getLine(fromLine);
		to = Line.getLine(toLine);
		timeInMinutes = G.DoubleToken(timeString);
		station.addInterchange(this);
	}
	
	static public long timeFromTo(OnedayLocation from,OnedayLocation to)
	{
		Station sta = from.getStation();
		Interchange interchange = sta.findInterchange(from.getLine(),to.getLine());
		return((int)(interchange.timeInMinutes*60*1000));
	}
}
