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
import lib.OStack;

class TrainLauncherStack extends OStack<TrainLauncher>
{
	public TrainLauncher[] newComponentArray(int n) { return(new TrainLauncher[n]); }
}

public class TrainLauncher implements OnedayConstants
{
	Line line;
	long lastLaunch;		// last launch, milliseconds into the simulation
	long interval;			// launch interval, milliseconds into the simulation
	int direction;
	TrainLauncher(Line l,int d)
	{
		line = l;
		direction = d;
		interval = (long)(l.runIntervalMinutes*60*1000);
	}
	
	public Train launch(long simtime)	// simtime is milliseconds since start in simulated time
	{	if(lastLaunch==0)
		{	// initial launch
			lastLaunch = (long)(simtime+(line.startOffsetMinutes*60*1000)-(line.runIntervalMinutes*60*1000));
		}
		if((simtime-lastLaunch)>=interval)
		{	lastLaunch = simtime;
			switch(direction)
			{
			default: throw G.Error("not expecting %s",direction);
			case 1:	return(new Train(line,line.getStop(0),1,simtime));
			case -1: return(new Train(line,line.getStop(line.nStops()-1),-1,simtime));
			}
		}
		return(null);
	}
}
