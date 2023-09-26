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

import java.awt.Color;
import lib.Graphics;
import java.awt.Rectangle;

import java.util.StringTokenizer;

import online.common.exCanvas;
import lib.G;
import lib.GC;
import lib.OStack;
import lib.Random;
import lib.StockArt;

class LineStack extends OStack<Line>
{
	public Line[] newComponentArray(int n) { return(new Line[n]); }
}

/* lines with the same fields as in the database */
class Line implements OnedayConstants
{	static Random lineRandom = new Random(453467);
	String name;
	String comment;
	double runtimeMinutes;			// start to finish time for a run, in minutes
	double runIntervalMinutes;		// interval between trains, in minutes
	double startOffsetMinutes;		// delay to first start, in minutes
	double stationTimeMinutes; 		// delay in each station, minutes
	Color color;
	Color textColor;
	String uid;
	public String getName() { return(name); }
	public String getUid() { return(uid); }
	public Color getColor() { return(color); }
	public StockArt getLineDot() { return(Station.getLineDot(this)); }
	
	public Color getDesatColor() 
		{ int r = color.getRed();
		  int g = color.getGreen();
		  int b = color.getBlue();
		  return(new Color(r+2*(255-r)/3,g+2*(255-g)/3,b+2*(255-b)/3));
		}
	boolean circular=false;
	boolean included = true;
	int lineDotIndex;
	long randomv;
	
	public long Digest(Random r) { return(r.nextLong()*randomv); };
	public static long Digest(Random r,Line l) { return((l!=null)?l.Digest(r) : 0); }
	
	public int nStops() { return(stops.size()); }
	public StopStack stops = new StopStack();
	public String toString()
	{	return("<line "+name+" "+comment+">");
	}
	public void addStop(Stop s)
	{
		int nstops = stops.size()-1;
		while(nstops>=0) 
			{ if(stops.elementAt(nstops).ordinal<s.ordinal)
				{
				stops.insertElementAt(s,nstops+1);
				return;
				}
				nstops--;
			}
		stops.insertElementAt(s,0);
	}
	public Stop getStop(int n)
	{	if(circular) 
		{
			if(n==-1) { n = stops.size()-1; }
			if(n==stops.size()) { n = 0; }
		}
		return((n>=0 && n<stops.size())?stops.elementAt(n):null);
	}
	Line(String n,String c,int r,int g,int b,int ba,boolean inc,int run,int in,int off,int ind)
	{
		this(n,c,new Color(r,g,b),new Color(ba,ba,ba),inc,run,in,off,ind);
	}

	Line(String n,String c,Color v,Color tc,boolean inc,int run,int in,int off,int ind)
	{	runtimeMinutes = run;
		runIntervalMinutes = in;
		lineDotIndex = ind;
		startOffsetMinutes = off;
		stationTimeMinutes = Default_Station_Time/60.0;
		name = n;
		included = inc;
		comment = c;
		color = v;			// line's traditional color from the maps
		textColor = tc;		// guess at a black or white text appropriate for this color
		circular = name.equalsIgnoreCase("circle");
		uid = new StringTokenizer(name).nextToken();
		randomv = lineRandom.nextLong();
		lines.push(this);
	}
	public double totalDistance()
	{	double tot = 0;
		for(int nStops=stops.size()-1,stopn=0; stopn<nStops;stopn++)
		{
			Stop s0 = getStop(stopn);
			Stop s1 = getStop(stopn+1);
			tot += s0.distanceTo(s1);
		}
		return(tot);
	}
	public long stationTime()	// station time in milliseconds
	{
		return((long)(1000*60*stationTimeMinutes));
	}
	// milliseconds 
	public long timeToNextStop(Stop target)
	{
		double tot = 0.0;
		double segmentDistance = 0;
		boolean found = false;
		boolean endOfLine = false;
		int nstops = 0;
		for(int nStops=stops.size()-1,stopn=0; stopn<nStops;stopn++)
		{
			Stop s0 = getStop(stopn);
			Stop s1 = getStop(stopn+1);
			double dist = s0.distanceTo(s1);
			tot += dist;
			nstops++;
			if(s0==target) { segmentDistance = dist; found = true; }
			if(s1==target) { endOfLine = true; }
		}
		G.Assert(found || endOfLine,"station not found "+target);
		long timeforsegment = (long)(1000*60*segmentDistance/tot*(runtimeMinutes-nstops*stationTimeMinutes)); 
		return(timeforsegment);
	}
	static LineStack lines = new LineStack();
	static public Line getLine(String name)
	  {	
	    for(int i=lines.size()-1; i>=0; i--)
		  {	Line l = lines.elementAt(i);
			  if(l.name.equalsIgnoreCase(name)) { return(l); }
			  if(l.uid.equalsIgnoreCase(name)) { return(l); }
		  }
	    throw G.Error("Line %s not found",name);
	  }
	static public Line getLine(int n)
	{
		return(lines.elementAt(n));
	}
	static public int nLines() { return(lines.size()); }
	public void registerSegments()
	{
		for(int i=0,nst = nStops(),lim=nst+(circular?0:-1);
		i<lim;
		i++)
		{
		Station from = getStop(i).station;
		Station to = getStop(i+1).station;
		if((from!=null) && (to!=null))
		{Segment s = new Segment(from,to,this);
		Segment.register(s);
		}
		}
	}
	static public void registerAllSegments()
	{
		for(int i=0;i<lines.size();i++) { lines.elementAt(i).registerSegments(); }
	}
	public void drawStops(Graphics g,exCanvas can,Rectangle r)
	{
		for(int i=0,nst = nStops();i<nst;i++)
		{	getStop(i).station.drawStop(g,can,r,1.0);
			
		}
				
	}
	public void drawSegments(Graphics g,Rectangle r,double scl)
	{	
		double strokeWidth = Math.max(2,(scl*G.Width(r)/120));
		for(int i=0,nst = nStops(),lim=nst+(circular?0:-1);
			i<lim;
			i++)
		{	Station from = getStop(i).station;
			Station to = getStop(i+1).station;
			if((from!=null) && (to!=null))
			{	Segment s = new Segment(from,to,this);
				Segment d = s.checkForDuplicate();

				int fx = G.Left(r)+(int)(from.xpos*G.Width(r)/100);
				int fy = G.Top(r)+(int)(from.ypos*G.Height(r)/100);
				int tx = G.Left(r)+(int)(to.xpos*G.Width(r)/100);
				int ty = G.Top(r)+(int)(to.ypos*G.Height(r)/100);
				if(s.line!=d.line)
				{
					if(Math.abs(fx-tx)>Math.abs(fy-ty))
					{
						fy+=strokeWidth;
						ty+= strokeWidth;
					}
					else { fx+=strokeWidth;
						tx+= strokeWidth;
					}
				}
				GC.setColor(g,color);
				GC.drawFatLine(g,fx,fy,tx,ty,strokeWidth);
			}
		}
	}
	
	public static void drawAllLines(Graphics gc,Rectangle r,double scl)
	{	for(int i=0;i<lines.size();i++) { lines.elementAt(i).drawSegments(gc,r,scl); }
	}
}