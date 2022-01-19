package oneday;


import lib.Graphics;
import com.codename1.ui.geom.Rectangle;

import lib.*;

import oneday.OnedayBoard.OnedayLocation;
import online.common.exCanvas;


// platform is a particular direction of travel on a particular line
class Platform implements OnedayLocation
{	
	Stop thisStop;			// always not-null
	Station prevStation;		// can be null for the beginning of the line
	Station nextStation;
	String uid = null;
	OnedayChip prize = null;

	public String getUid() { return(uid); }
	// constructor
	Platform(Stop current,Station prev,Station next,String u)
	{
		thisStop = current;
		prevStation = prev;
		nextStation = next;
		uid = u;
	}
	public String toString() 
	{ String prev = prevStation==null ? "-" : prevStation.getName();
	  String next = nextStation==null ? "-" : nextStation.getName();
	  return("<Platform from "+prev+" at "+getStation().getName()+" to "+next+">"); 
	}

	
	// location is set when the station platforms are actually draw 
	Rectangle visLocation = null;
	int x = 0;
	int y = 0;
	public void setVisLocation(Rectangle r)
		{ visLocation = r; 
		  x = G.centerX(r);
		  y = G.centerY(r);
		}
	public Rectangle getVisLocation() { return(visLocation); }
	public int getScreenX() { return(x); }
	public int getScreenY() { return(y); }
	

	
	public long Digest() 
	{ 	return (thisStop.Digest()*42562
				+ (prevStation==null ? 0 : prevStation.Digest()*6784832));
	}
	public long Digest(Random r) {
		return (thisStop.Digest(r)*42562
				+ (prevStation==null ? 0 : prevStation.Digest(r)*6784832));
	}
	public double getX() { return(thisStop.getX()); };
	public double getY() { return(thisStop.getY()); };

	public Station getStation() { return(thisStop.getStation());	}
	public Stop getStop() { return(thisStop);	}
	public Line getLine() { return(thisStop.getLine()); }
	public Platform getPlatform() { return(this); }
	public Train getTrain() { return(null); }

}
class StopStack extends OStack<Stop>
{
	public Stop[] newComponentArray(int n) { return(new Stop[n]); }
}
/* stops with the same values as in the database, but linked to objects for stations and lines */
class Stop implements Digestable,OnedayLocation
{	static Random stopRandom = new Random(637363);
	static boolean NEXTSTOP_COMPATIBILITY_KLUDGE = false;
	Station station;
	Line line;
	int ordinal;
	long randomv;
	Platform leftPlatform = null;
	Platform rightPlatform = null;
	static StopStack stops = new StopStack();
	public long Digest() { return(randomv); }
	public long Digest(Random r) { return(randomv*r.nextLong()); }
	public static long Digest(Stop s) { return((s==null) ? 0 : s.Digest()); }
	public static long Digest(Random r,Stop s) { return((s==null) ? 0 : s.Digest(r)); }
	
	public void findPlatforms()
	{
		if((rightPlatform==null) && (leftPlatform==null))
		{
			Station prevStop = prevStop();
			Station nextStop = nextStop();
			leftPlatform = new Platform(this,prevStop,nextStop,"L"); 
			rightPlatform = new Platform(this,nextStop,prevStop,"R"); 
		}
	}
	public Platform getPlatform(String id)	// not station name
	{	findPlatforms();
		if((leftPlatform!=null) && id.equals(leftPlatform.getUid())) { return(leftPlatform); }
		if((rightPlatform!=null) && id.equals(rightPlatform.getUid())) { return(rightPlatform); }
		throw G.Error("Platform %s on %s not found",id,this);
	}

	public Platform getPlatformTo(Station to)
	{
		findPlatforms();
		if( (leftPlatform.nextStation==to)) { return(leftPlatform); }
		if( (rightPlatform.nextStation==to)) { return(rightPlatform); }
		throw G.Error("no platform found to %s",to);
	}
	public Platform getPlatformTo(Stop to)
	{
		return(getPlatformTo(to==null?null:to.getStation()));
	}
	public Platform getPrevPlatform()
	{
		findPlatforms();
		return(leftPlatform);
	}
	public Platform getNextPlatform()
	{
		findPlatforms();
		return(rightPlatform);
	}
	public double distanceTo(Stop next)
	{	return(station.distanceTo(next.station));
	}
	public String toString()
	{
		return("<stop "+station.getName()+" on "+line+">");
	}
	public Stop(String station,String line,int num)
	{
		this(Station.getStation(station),Line.getLine(line),num);
	}
	Stop(Station c,Line l,int n)
	{	station = c; 
		line = l;
		ordinal = n;
		stops.push(this);
		randomv = stopRandom.nextLong();
		line.addStop(this);
	}
	Station findStop(int n)
	{	Stop st = line.getStop(n);
		return((st==null)?null:st.station);
	}
	Station prevStop() { return(findStop(NEXTSTOP_COMPATIBILITY_KLUDGE?(ordinal-1):(ordinal-2))); }
	Station nextStop() { return(findStop(NEXTSTOP_COMPATIBILITY_KLUDGE?(ordinal+1):ordinal)); }
	static Stop findStop(Station station,Line line)
	{
		for(int idx=stops.size()-1; idx>=0; idx--)
		{
			Stop id = stops.elementAt(idx);
			if((id.station==station)&&(id.line==line)) { return(id); }
		}
		throw G.Error("Stop %s not found for %s",line,station);
	}
	public void drawLines(Graphics g,Rectangle r,double scl)
		{
		line.drawSegments(g,r,scl);
		}
	public void drawStops(Graphics g,exCanvas forCan,Rectangle r)
	{
		line.drawStops(g,forCan,r);
	}
	//
	// true if you can make a direct connection from a prev to this stop
	// 
	public boolean legalConnection(Stop prev)
	{
		return((prev!=this) 								// can't connect to ourself
				&& ((prev==null)||(prev.line==line)));		// can connect if we're on the same line
	}
	
	// true if you can travel from prev1 through prev2 to this station
	public boolean legalConnection(Stop prev1,Stop prev2)
	{	return( legalConnection(prev2)
				&& ((prev1==null)
						|| ((prev1.line==line)
								// this tests that the 3 stops are in the same direction
								&& (((prev1.ordinal-prev2.ordinal)*(prev2.ordinal-ordinal))>0)
								)
						));
	}
	public Stop firstStop(Stop toward)
	{	G.Assert((toward!=this) && (toward.line==line),"same line");
		return(line.getStop(ordinal-1+(toward.ordinal>ordinal?1:-1)));
	}
	// get the last stop before "toward" on this line
	public Stop lastStop(Stop toward)
	{	
		Stop prev = this;
		Stop next = this;
		while((next= prev.firstStop(toward))!=toward) 
			{ prev = next; }
		return(prev);
	}
	// prev1 and prev2 are previous stops. If both stops are also on
	// the same line as this stop, they must be in order.
	public boolean illegalConnection(Stop prev1,Stop prev2)
	{	Stop p1stop = prev1.station.getStopOnLine(line);
		Stop p2stop = prev2.station.getStopOnLine(line);
		return((p1stop!=null)
			&& (p2stop!=null)
			&& !legalConnection(p1stop,p2stop)
			&& (prev1.lastStop(prev2).station==p2stop.firstStop(p1stop).station)	// uses the same track
			);
	}
    // true if lines prev and st run concurrently
    public boolean isConcurrentLine(Stop prev)
    {
		  if(prev!=null)
		  {	Station prevNext = prev.nextStop();
		    Station prevPrev = prev.prevStop();
		    Station n = nextStop();
		    Station p = prevStop();
			  if( ((prevNext==n) || (prevNext==p) ) 
				  && ((prevPrev==n)||(prevPrev==p)))
			  {	  // G.print("concurrent "+this+prev);
				  return(true);
			  }
			  else { return(false); }
		  }
		  return(false);
    }
	// true if we can travel from previousStop1 to here by changing at previousStop2
	public Stop legalChange(Stop previousStop1,Stop previousStop2)
	{
		if(previousStop2.legalConnection(previousStop1)
				&& !legalConnection(previousStop1)
				// this was intended to prevent station hopping in the same direction
				// but it actually prevented forward hops that should be legal.
				&& (!station.directConnection(previousStop1)
						// added this qualifier which is if the current line isn't common
						// which means we switched out even though the endpoints are directly connected
						|| (previousStop1.station.getStopOnLine(line)==null))
				)
		{
			Stop changes[]=previousStop2.station.stops;
			for(Stop changeto : changes)
			{
				if((changeto.line!=previousStop1.line) 
						&& (changeto.line==line)
						&& !changeto.illegalConnection(previousStop1,previousStop2)
						)
				{	
					return(changeto);
				}
			}
		}
		return(null);
	}
	// methods for OnedayLocation
	public double getX() { return(station.getX()); }
	public double getY() { return(station.getY()); }
	public Station getStation() { return(station); }
	public Stop getStop() { return(this); }
	public Line getLine() { return(line); }
	public Platform getPlatform() { return(null); }
	public Train getTrain() { return(null); }
	
}