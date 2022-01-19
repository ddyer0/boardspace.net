package oneday;


import com.codename1.ui.geom.Rectangle;

import lib.*;
import lib.Random;
import oneday.OnedayBoard.OnedayLocation;
class TrainStack extends OStack<Train>
{
	public Train[] newComponentArray(int n) { return(new Train[n]); }
}

public class Train implements Digestable,OnedayLocation
{
	enum Status { moving, stopped, ended }
	public boolean isStopped() { return(status==Status.stopped); }
	public boolean isEnded() { return(status==Status.ended); }
	Line line;				// line we're moving on
	Stop currentStop;		// were we departed from
	Stop nextStop;			// where we are heading
	Stop prevStop;			// where we last were
	double position;		// fractional transit between current stop and next stop
	long timeAtPosition;	// simulated time we arrived at position
	long currentTime;		// simulated last updated time
	Status status;			// moving, stationary
	int directionOfTravel;	// +1 for from stop 1 to n, -1 for stop n to 1
	public Train() { }
	public String toString() { return("<train on "+line.name+" "+status+">");}
	public void sameTrain(Train other)
	{
		G.Assert(line==other.line,"line mismatch");
		G.Assert(currentStop==other.currentStop,"currentStop mismatch");
		G.Assert(nextStop==other.nextStop,"nextStop mismatch");
		G.Assert(prevStop==other.prevStop,"prevStop mismatch");
		G.Assert(position == other.position,"position mismatch");
		G.Assert(timeAtPosition==other.timeAtPosition,"timeAtPosition mismatch");
		G.Assert(currentTime==other.currentTime,"currentTime mismatch");
		G.Assert(status==other.status,"status mismatch");
		G.Assert(directionOfTravel==other.directionOfTravel,"directionOfTravel mismatch");
	}
	public static void sameTrain(Train from,Train other)
	{
		if(from==null) { G.Assert(other==null,"train mismatch"); }
		else { from.sameTrain(other); }
	}
	public void deactivate()
	{
		status = Status.ended;
	}
	public Train copyFrom() 
	{	Train tr = new Train();
		tr.copyFrom(this);
		return(tr);
	}
	public static Train clone(Train t) { return((t==null) ? null : t.copyFrom()); }
	
	public void copyFrom(Train other)
	{
		line = other.line;
		currentStop = other.currentStop;
		nextStop = other.nextStop;
		position = other.position;
		timeAtPosition = other.timeAtPosition;
		currentTime = other.currentTime;
		status = other.status;
		directionOfTravel = other.directionOfTravel;
		sameTrain(other);
	}
	public long Digest()
	{	long v = 0;
		v ^= Line.Digest(line);
		v ^= Stop.Digest(currentStop);
		v ^= Stop.Digest(nextStop);
		v ^= Stop.Digest(prevStop);
		v ^= (long)(position*2345246);
		v ^= (long)(timeAtPosition*3526725);
		v ^= (long)(currentTime*672342);
		v ^= (status.ordinal()+1)*123461;
		v ^= (directionOfTravel+1)*673463578;
		return(v);
	}
	public long Digest(Random r)
	{
		long v = 0;
		v ^= Line.Digest(line);
		v ^= Stop.Digest(r,currentStop);
		v ^= Stop.Digest(r,nextStop);
		v ^= Stop.Digest(r,nextStop);
		v ^= (long)(position*r.nextLong());
		v ^= (long)(timeAtPosition*r.nextLong());
		v ^= (long)(currentTime*r.nextLong());
		v ^= (status.ordinal()+1)*r.nextLong();
		v ^= (directionOfTravel+1)*r.nextLong();

		return(v);
	}
	public static long Digest(Random r,Train t) { return(t==null ? 0 : t.Digest(r)); }
	
	public Train(Line l,Stop s,int dir,long now)
	{	
		timeAtPosition = now;
		currentTime = now;
		line = l;
		currentStop = s;
		directionOfTravel = dir;
		status = Status.stopped;
		nextStop = line.getStop(currentStop.ordinal+directionOfTravel-1);
		G.Assert(nextStop!=null,"has a next stop");
		Platform plat = getPlatform();
		G.Assert(plat.nextStation==nextStop.getStation(), "direction of travel ok");
	}
	public void start(long simtime)
	{	timeAtPosition = simtime;
		currentTime = simtime;
		status = Status.stopped;
		position = 0.0;
	}
	public void update(long simtime)
	{	
		long timeSinceUpdate = simtime-timeAtPosition;
		currentTime = simtime;
		
		while(timeSinceUpdate>0)
		{
		switch(status)
		{
		case ended:
			timeSinceUpdate = 0;
			break;
		case moving:
			{
			long transitTicks = line.timeToNextStop(directionOfTravel>0?currentStop:nextStop);				// how long this segment takes
			if(timeSinceUpdate<transitTicks)
				{	// still moving
					position = (double)timeSinceUpdate/transitTicks;
					timeSinceUpdate = 0;
				}
				else
				{ // reached the station
					status = Status.stopped;
					prevStop = currentStop;
					currentStop = nextStop;		
					nextStop = line.getStop(currentStop.ordinal+directionOfTravel-1);
					position = 0.0;
					timeSinceUpdate -= transitTicks;
					timeAtPosition += transitTicks;		// possibly iterate into the station pause
				}
			}
			break;
		case stopped:
			{
			long stationaryTicks =line.stationTime();	// how long in milliseconds we remain stopped
			if(stationaryTicks<timeSinceUpdate)					// time to get moving?
				{	// time to get moving
					timeSinceUpdate -= stationaryTicks;
					timeAtPosition += stationaryTicks;
					
					status = (nextStop==null) ? Status.ended : Status.moving;
					position = 0.0;
				}
			else 
				{ // still at the station
				  timeSinceUpdate = 0; 
				  position = 0.0;
				}
			}
			break;
		default:
			break;
		
		}

		}
		
	}
	int positionOnMap_x(Rectangle r)
	{	double curx = getX()/100.0*G.Width(r);
		return((int)curx);
	}

	int positionOnMap_y(Rectangle r)
	{
		double cury = getY()/100.0*G.Height(r);
		return((int)cury);
	}
	
	// methods for OnedayLocation
	public double getY() 
	{
		double lastY = currentStop.station.getY();
		return((nextStop==null) 
				? lastY
				: G.interpolateD(position, lastY, nextStop.station.getY()));
	}
	public double getX()
	{	double lastX = currentStop.station.getX();
		return((nextStop==null)
					?lastX 
					: G.interpolateD(position, lastX, nextStop.station.getX()));
	}

	public Station getStation()
	{
		return((status==Status.stopped) 
				? currentStop.station 
				: (nextStop==null ? null : nextStop.station));
	}
	public Stop getStop() 
		{ if((status==Status.stopped)||(status==Status.ended)) { return(currentStop);}
			// no stop for a moving train
		return(null); 
		}
	public Line getLine() { return(line); }
	public Train getTrain() { return(this); }
	public Platform getPlatform() 
	{ 	if((status==Status.stopped)||(status==Status.ended))
		{
			return(getStop().getPlatformTo(nextStop));
		}
		return(null);
		}
}
