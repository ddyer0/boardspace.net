/* copyright notice */package oneday;

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
