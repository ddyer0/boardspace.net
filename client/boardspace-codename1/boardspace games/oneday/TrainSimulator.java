package oneday;

import com.codename1.ui.geom.Rectangle;
import lib.G;
import lib.SimpleSprite;
import lib.StockArt;
import lib.SimpleSprite.Movement;
import online.common.exCanvas;

public class TrainSimulator {
	StopStack stops = new StopStack();
	double speed = 1.0;			// percent of full map / second
	double stationTime = 0.01;		// time we spend in a station in seconds
	public void addStop(Stop s)
		{ Stop prev = stops.top();
		  if(prev!=null)
		  {	Line newline = s.line;
		    Stop prevStop = prev.station.getStopOnLine(newline);
		  	do {
		  	// push all the intermediate stations
		    prevStop = newline.getStop(prevStop.ordinal-1+((prevStop.ordinal>s.ordinal) ?-1:1));
		    stops.push(prevStop);
		  	} while(prevStop!=s && prevStop!=null);
		  }
		  else { stops.push(s); } 
		}
	public TrainSimulator(OnedayCell rack[])
	{	StopStack stops = OnedayCell.getStops(rack,0,rack.length);
		if(stops.size()==rack.length)
			{while(stops.size()>0) { addStop(stops.pop()); }
			}
	}
	
	// this runs the endgame simulation which traces the winning route at high speed
	public void runSimulation(exCanvas forCan,Rectangle r)
	{	double startingTime = 0.0;
		while(stops.size()>=2)
		{	
			Station start = stops.remove(0,true).station;
			Station next = stops.elementAt(0).station;
			double dist = start.distanceTo(next);
			double duration =(dist*speed);
			SimpleSprite newSprite = new SimpleSprite(true,StockArt.SmallO,
     				G.Width(r)/6,	// use the same cell size as drawSprite would
     				startingTime,
     				duration,
             		G.Left(r)+(int)(start.xpos*G.Width(r)/100),G.Top(r)+(int)(start.ypos*G.Height(r)/100),
             		G.Left(r)+(int)(next.xpos*G.Width(r)/100),G.Top(r)+(int)(next.ypos*G.Height(r)/100),0);
     		newSprite.movement = Movement.SlowInOut;
     		forCan.addSprite(newSprite);
			startingTime +=duration+stationTime;
		}
	}

}