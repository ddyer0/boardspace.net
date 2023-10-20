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
import java.awt.Font;
import java.awt.Rectangle;

import lib.Graphics;
import lib.Image;
import lib.ImageLoader;
import lib.G;
import lib.GC;
import lib.OStack;
import lib.Random;
import lib.StockArt;
import lib.exCanvas;
import oneday.OnedayBoard.OnedayLocation;

class StationStack extends OStack<Station>
{
	public Station[] newComponentArray(int n) { return(new Station[n]); }
}
class ChipStack extends OStack<OnedayChip>
{
	public OnedayChip[] newComponentArray(int n) { return(new OnedayChip[n]); }
}
class InterchangeStack extends OStack<Interchange>
{
	public Interchange[] newComponentArray(int n) { return(new Interchange[n]); }
}
/**
 * This generates a deck of cards for "one day in london" optimized for 
 * printing as gamecrafter micro-cards.  The size and margins would need
 * to be different for a different target.
 *  
 * @author ddyer
 *
 */
/* station with the same data as in the database, but linked to objects 
 * Constructor remembers all the stations constructed, and the class
 * provides a "find" operation.
 * 
 * The order of lines on the cards is determined by the "line" field of the station
 * the lineset field, which should contain the same information, is not used.  This
 * becomes important when the + symbols are being generated because the lines that
 * are actually concurrent have to be adjacent on the card.
 * 
 * Notes on the real world vs. the game.
 * 
 * We eliminated the circle line because it made the game too easy.  Consequently, if you're
 * really traveling in London, you might find a train labeled as Circle line which is going
 * in the same direction, but which has a final destination you can't reach in the game
 * without changing trains. 
 * The only real link that is "just missing" from the game is circle line between High Street Kensington
 * and Gloucester Road.  
 * The only other abnormality is that we combined Aldgate and Aldgate East stations.  In reality
 * they're a few blocks apart.
 * */
class Station extends OnedayChip implements OnedayConstants,OnedayLocation
{	static final String HOTFRAME = "HotFrame";
	static Random rv = new Random(16246357);
	public boolean isStation() { return(true); }
	// avoid bugfinder complaints about ==string
	Object getHotFrame() { return(HOTFRAME); }
	boolean isHotFrame(Object f)
	{	Object h = getHotFrame();
		return(f==h);
	}
	public Platform randomPlatform(Random r)
	{	
		Stop startingStop = stops[Random.nextInt(r, stops.length)];
		Station prev = startingStop.prevStop();
		boolean next = Random.nextInt(r, 2)==0;
		if(prev==null || next)  { prev = startingStop.nextStop(); }
		return startingStop.getPlatformTo(prev);
	}
	static boolean NOTE_ADJACENT = false;
	String station;		// name of the station
	String lines;		// list of lines running through the station
	String pic;			// the image name for the representative picture
	Stop [] stops;		// one stop per line running through the station 
	InterchangeStack interchanges = new InterchangeStack();
	public void addInterchange(Interchange d) { interchanges.push(d); }
	public String description;	// description for the second line
	public String pronounciation;
	public String activity;
	double xpos;		// x position on the map
	double ypos;		// y position on the map
	int number;			// index in the stations or cards stack
	boolean imageOnly = false;	// if true, the only the image is relevant (card back)
	boolean isCard = false;		// true for cards vs. stations
	Station parent=null;		// for cards, points to the parent station
	int copyNumber;				// for cards, the copy number among nCopies()
	String uid = null;
	public String getUid() { return(uid); }
	
	public double distanceTo(Station next)
	{
		return(G.distance(xpos,ypos,next.xpos,next.ypos)/100.0);
	}
	public int positionOnMap_x(Rectangle r)
	{
		return((int)(G.Left(r)+xpos*G.Width(r)/100.0));
	}
	public int positionOnMap_y(Rectangle r)
	{
		return((int)(G.Top(r)+ypos*G.Height(r)/100.0));
	}
	//
	// cards and stations are really the same, except cards are moved around
	// and stations are fixed, and there can be multiple cards that are identical
	// except for their card number.
	//
	static StationStack stations = new StationStack();	// all the stations
	static StationStack cards = new StationStack();		// all the stations
	public static int nStations() { return(stations.size()); }
	public static int nCards() { return(cards.size()); }
	
	// find the stop at this station on Line l
	public Stop getStopOnLine(Line l)
	{	for(Stop s : stops) { if(s.line==l) { return(s); }}
		return(null);
	}
	// get a station by number
	public static Station getStation(int ord)								// get the n'th station
	{	if(ord<stations.size())
		{
		return(stations.elementAt(ord));
		}
		return(null);
	}
	// get a card by number
	public static Station getCard(int ord)								// get the n'th station
	{	if(ord<cards.size())
		{
		return(cards.elementAt(ord));
		}
		return(null);
	}
	
	public String getName() { return(station); }
	
	// get a station by name
	public static Station getStation(String name)			// get a station by name
	  {	
	    for(int i=stations.size()-1; i>=0; i--)
		  {	Station l = stations.elementAt(i);
			  if(l.station.equalsIgnoreCase(name)) { return(l); }
			  if(l.getUid().equalsIgnoreCase(name)) { return(l); }
		  }
	    throw G.Error("Station %s not found",name);
	  }
	
	// create a station/card that is not in any stack, used for the 
	// fake "card back" card.
	Station(Image im,String na)
	{	file = na;
		image = im;
		imageOnly = true;
		randomv = rv.nextLong();
	}
	public String toString() { return("<"+station+(isCard?("#"+copyNumber):"")+">"); }
	public int getNumber() { return(number); }
	
	Station(String s,String l,String p,double xp,double yp,String des,String pro,String act)	// construct a station and remember it
	{	this(s,l,p,xp,yp,des,mapImage.image,false,pro,act);
	}
	Station(String s,String l,String p,Image im,double xp,double yp,String des,String pro,String act)	// construct a station and remember it
	{	this(s,l,p,xp,yp,des,im,false,pro,act);
	}
	
	Station(String s,String l,String p,double xp,double yp,String des,Image im,boolean isca,String pro,String act)	// construct a station and remember it
	{	file = s;
		activity = act;
		pronounciation = pro;
		image = im;
		description=des;
		station = s;
		lines = l;
		pic = p;
		xpos = xp;
		ypos = yp;
		isCard = isca;
		parent = this;		// I'm also my own grandpa
		uid = s.toLowerCase().replace(' ','_').replace('\'',(char)0);
		randomv = rv.nextLong();
		if(isCard) { number = cards.size(); cards.push(this); }
		else 
			{	 number = stations.size(); stations.push(this); }
	}
	// make the cards that are copies of this station
	private void makeCards(Random r)
	{	int cop = nCopies();
		G.Assert(!isCard,"already a card");

		for(int i=0;i<cop;i++)
		{
			Station card = new Station(file,lines,pic,xpos,ypos,description,image,true,pronounciation,activity);
			card.parent = this;
			card.copyNumber = i;
			card.stops = this.stops;
			card.randomv = r.nextLong();
		}
	}

	//
	// parse the "lines" list and find each of them in the list of all stops.
	// convert this to an array of stops for this station.
	//
	void findStops(Random r)
	{	StopStack mystops = new StopStack();
		String lin = lines;
		int ind;
		// this lines list has names of lines separated by commas
		while((ind=lin.indexOf(','))>=0)
		{	Line stop = Line.getLine(lin.substring(0,ind));
			if(stop.included) { mystops.push(Stop.findStop(this,stop)); }
			lin = lin.substring(ind+1);
		}
		Line stop = Line.getLine(lin);
		if(stop.included) { mystops.push(Stop.findStop(this,stop)); }
		stops = mystops.toArray();
		if(!isCard) { makeCards(r); }
	}
	
	public Interchange findInterchange(Line from,Line to)
	{
		for(int lim=interchanges.size()-1; lim>=0; lim--)
		{
			Interchange el = interchanges.elementAt(lim);
			if(el.from==from && el.to==to) { return(el); }
			if(el.to==from && el.from==to) { return(el); }
		}
		if(from==to) 
			{ // add the interchange from one side of the platform to the other
			  Interchange n = new Interchange(this,from,to,2.0);
			  addInterchange(n);
			  return(n);
			}
		return(null);
	}
	public void findInterchanges()
	{	int found = 0;
		int missed = 0;
		for(int from = 0; from<stops.length; from++)
		{	Stop fromStop = stops[from];
			for(int to = from; to<stops.length; to++)
			{
				Stop toStop = stops[to];
				Interchange change = findInterchange(fromStop.line,toStop.line);
				if(change==null) 
				{ missed++;
				  G.print("Missing interchange "+this+" "+fromStop+" "+toStop); 
				}
				else 
				{ found++;
				  //G.print("change "+this+" "+fromStop+" "+toStop+" "+change.time);
				}
			}
		}
		if(missed>0) { G.print(""+found+" found   "+missed+" missed"); }
	}
	
	
	// this is the final construction step, which matches
	// the station with the lines running through the station.
	// also makes the card deck corresponding to the station list
	static void findAllStops()
	{	Random r = new Random(120240250);
		cards.clear();
		for(int idx = stations.size()-1; idx>=0; idx--)
		{
		Station st = stations.elementAt(idx);
		st.randomv = r.nextLong();	// set the id of the station too
		st.findStops(r);
		st.findInterchanges();
		}
		Line.registerAllSegments();
	}
	//
	// return the number of copies of this station we will have in the
	// game.  At the moment every station gets at least 1, but stations
	// with N lines get N-1 cards.
	//
	public int nCopies()
	{
		return(Math.max(1,nIntersections()));
	}
	//
	// count the stops, ideally we get all of them.
	//
	static int nStops()
	{	int n=0;
		for(int idx=stations.size()-1; idx>=0; idx--)
		{
			n+= stations.elementAt(idx).stops.length;
		}
		return(n);
	}

	
	//
	// elastic parameters for drawing station cards
	//
	int CARD_MARGIN_PERCENT=10;
	Color lightGray = new Color(0.8f,0.8f,0.8f);
	Font titleFont = G.getFont("sansserif",G.Style.Bold,20);
	Font cardFont = G.getFont(titleFont, G.Style.Bold, 15);
	Font smallCardFont = G.getFont(titleFont, G.Style.Bold, 15);

	public void drawChip(Graphics gc,exCanvas canvas,int SQUARESIZE,double xscale,int cx,int cy,String label)
	{	boolean hot = isHotFrame(label);
		if(hot) { label = null; }
		double ratio = (double)blank.image.getHeight()/blank.image.getWidth();
		int h = (int)(SQUARESIZE*ratio);
		if(imageOnly)
		{drawChipInternal(gc,canvas,SQUARESIZE,xscale,cx,cy,label);
		 if(hot) { hotframe.drawChipInternal(gc,canvas,SQUARESIZE,xscale,cx,cy,label); }
		}
		else
		{
		blank.drawChipInternal(gc,canvas,SQUARESIZE,xscale,cx,cy,null);
		Rectangle r = new Rectangle(cx-SQUARESIZE/2,cy-h/2,SQUARESIZE,h);
		drawCard(gc,canvas,r,"");
		(hot?hotframe:frame).drawChipInternal(gc,canvas,SQUARESIZE,xscale,cx,cy,label);
		}
	}

    public void drawLines(Graphics g,Rectangle r,double scl)
    {
    	for(Stop stop : stops)
    	{
    		stop.drawLines(g,r,scl);
    	}
    }
    public void drawStop(Graphics g,exCanvas forCan,Rectangle r,double scl)
    {
		  dot.drawChip(g,forCan,Math.max(8,(int)(scl*G.Width(r)/40)),
  				  (int)(G.Left(r)+xpos*G.Width(r)/100),
  				  (int)(G.Top(r)+G.Height(r)*ypos/100),
  				  null);

    }
    static public void drawAllStops(Graphics g,exCanvas forCan,Rectangle r,double scl)
    {
    	for(int i=0;i<stations.size();i++) { stations.elementAt(i).drawStop(g,forCan,r,scl); }
    }
    public void drawStops(Graphics g,OnedayViewer can,Rectangle r)
    {
    	for(Stop stop : stops)
    	{
    		stop.drawStops(g,can,r);
    	}
    	drawStop(g,can,r,2.0);
    }

    public int nIntersections()
    {	int sz = stops.length;
    	int nn = stops.length-1;
    	if(sz>1)
    	{
    	for(int i=1;i<sz;i++)
    		{
    		// we know the concurrent lines are also adjacent in the stop list
    		if(stops[i].isConcurrentLine(stops[i-1])) 
    			{ nn--; }
    		}
    	}
    	return(nn);
    	
    }
    
    // true if there is a direct connection between stop previousStop
    // and this station, ie; one of our lines matches
	public boolean directConnection(Stop previousStop1)
	{	for(Stop s : stops)
		{	if(s.line==previousStop1.line) { return(true); }
		}
		return(false);
	}
	

    public void drawCard(Graphics g,exCanvas forCan,Rectangle r,String cardId)
	  {	  
    	  int smargin = (G.Width(r)*CARD_MARGIN_PERCENT)/100;
	  	  int tmargin = smargin*2/3;
	  	  int im_w = image.getWidth();
	  	  int im_h = image.getHeight();
		  int w = G.Width(r)-smargin*2;
		  int h =w*im_h/im_w;
	  	  int TEXTHEIGHT = (G.Height(r)-tmargin*2)/10;
	  	  int tbot =TEXTHEIGHT;
	  	  Rectangle title = new Rectangle(G.Left(r)+smargin,G.Top(r)+tmargin,w,TEXTHEIGHT);
	  	  int image_y = G.Top(r)+tmargin+TEXTHEIGHT;
	  	  int image_x = G.Left(r)+smargin;
	  	  // draw the main image for the card
	  	  forCan.drawImage(g,image,image_x,image_y-TEXTHEIGHT/3,w,h);
	  	  Rectangle imrect = new Rectangle(image_x,image_y-TEXTHEIGHT/3,w,h);
  		  drawLines(g,imrect,1.0);
  		  drawStop(g,forCan,imrect,1.0);
  		  //dot.drawChip(g,forCan,TEXTHEIGHT/2,
  			//	  (int)(image_x+xpos*w/100),
  			//	  (int)(image_y+h*ypos/100),
  			//	  null);
		  
	  	  if((w>30) && (tbot>6))
	  		  {// big enough for the text to show up
	  		  GC.setFont(g,titleFont);
	  		  GC.Text(g,true,title,Color.black,Color.white,station);
	  		  GC.setFont(g,G.getFont(cardFont, tbot-2));
	  		  }
		  int picBottom = image_y+h-tbot/2;
		  boolean allConcurrent = true;
		  for(int idx=0;idx<stops.length;idx++)
		  {   Stop prev = (idx>0) ? stops[idx-1] : null;
			  Stop st = stops[idx];
			  Line l = st.line;
			  String name = l.name;
			  if(name.endsWith(" & City")) { name = name.substring(0,name.length()-7); }
			  Rectangle stopRect = new Rectangle(G.Left(r)+smargin+tbot,picBottom,w-tbot*2,tbot);
			  Rectangle stopRectRight = new Rectangle(G.Left(r)+smargin+w-tbot,picBottom,tbot,tbot);
			  Rectangle stopRectLeft = new Rectangle(G.Left(r)+smargin,picBottom,tbot,tbot);
			  picBottom += tbot;
			  if(tbot>6)
			  {
			  GC.setFont(g,G.getFont(cardFont, tbot-2));
			  GC.Text(g,true,stopRect,l.textColor,l.color,(w>30)?name:"");
			  }
			  // 
			  // generate the + symbols for lines which run concurrently.
			  // this depends on the lines that are concurrent being next
			  // to each other
			  //
			  if(st.isConcurrentLine(prev))
			  {
				  G.SetTop(stopRect,G.Top(stopRect)- G.Height(stopRect)/2); 
				  GC.Text(g,true,stopRect,l.textColor,null,"+");
			  }
			  else { allConcurrent=false; }
			  if(tbot>8)
			  {
			  //
			  // draw the station number at the right
			  //
			  GC.setFont(g,G.getFont(smallCardFont, tbot-4));
			  GC.Text(g,true,stopRectLeft,l.textColor,l.color,""+(st.ordinal-1));
			  GC.Text(g,true,stopRectRight,l.textColor,l.color,""+(st.line.nStops()-st.ordinal));
			  }
		  }
		  //
		  // if this is a non-intersection station, note the previous and next stations
		  //
		  if(NOTE_ADJACENT && (stops.length==1 || allConcurrent))
		  {	  Stop st = stops[stops.length-1];
		  	  Rectangle stopRect = new Rectangle(G.Left(r)+smargin,picBottom-tbot,w,tbot);
	  	      picBottom += tbot;
			  Station prevStation = st.prevStop();
			  Station nextStation = st.nextStop();
			  G.SetTop(stopRect, G.Top(stopRect) + G.Height(stopRect));
			  String msg = ((prevStation==null) ? "" : prevStation.station) + "  --  "+ ((nextStation==null)?"":nextStation.station);
			  GC.Text(g,true,stopRect,Color.black,lightGray,msg);
		  }
		  if((cardId!=null)&&(tbot>6))
		  {	Rectangle bot = new Rectangle(G.Left(r)+smargin+smargin/4,G.Bottom(r)-tmargin-tbot,w,tbot);
			GC.Text(g,false,bot,Color.black,null,cardId);
		  }
	  }
    
	static StockArt dot = null;		// the dot used to mark stations on cards
	static StockArt mapImage = null;// the map image inset on each card
	static Station back = null;		// the card back 
	static Station blank = null;	// the blank front
	static StockArt frame = null;	// the frame for drawn cards
	static StockArt hotframe = null;	// reddish frame for drawn cards
	static Station back2 = null;
	static double scale[][] = {{0.5,0.5,1.0},{0.5,0.5,1.0},{0.5,0.5,1.0},
			{0.5,0.5,1.0},{0.5,0.5,1.0},{0.5,0.5,1.0}
			,{0.5,0.5,0.5}	// brown
			,{0.5,0.5,0.5}	// red
			,{0.5,0.5,0.5}	// yellow
			,{0.5,0.5,0.5}	// green
			,{0.5,0.5,0.5}	// pink
			,{0.5,0.5,0.5}	// gray
			,{0.5,0.5,0.5}	// purple
			,{0.5,0.5,0.5}	// black
			,{0.5,0.5,0.5}	// blue
			,{0.5,0.5,0.5}	// cerulian
			,{0.5,0.5,0.5}	// aqua
			};
	
	static final int brownIndex = 0;
	static final int redIndex = 1;
	static final int yellowIndex = 2;
	static final int greenIndex = 3;
	static final int pinkIndex = 4;
	static final int grayIndex = 5;
	static final int purpleIndex = 6;
	static final int blackIndex = 7;
	static final int blueIndex = 8;
	static final int cerulianIndex = 9;
	static final int aquaIndex = 10;
	static final int nStations = 11;
	static StockArt StationColors[] = new StockArt[nStations];
	static final StockArt getLineDot(Line l)
	{
		return(StationColors[l.lineDotIndex]);
	}
    /**
     * this is a fairly standard preloadImages method, called from the
     * game initialization.  It loads the images (all two of them) into
     * a static array of Chip which are used by all instances of the
     * game.
     * @param forcan the canvas for which we are loading the images.
     * @param Dir the directory to find the image files.
     */
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(dot==null)
		{
		StockArt[] stockImages = StockArt.preLoadArt(forcan,Dir,cardImageNames, scale);
		blank = new Station(stockImages[1].image,"blank");
		back = new Station(stockImages[2].image,"back");
		frame = stockImages[3];
		back2 = new Station(stockImages[4].image,"back2");
		hotframe = stockImages[5];
		
		for(int i=0;i<nStations;i++) { StationColors[i]=stockImages[6+i]; }
		}
		if(mapImage==null)
		{
		StockArt[] stockImages = StockArt.preLoadArt(forcan,Dir,mapImageNames, false, scale);
		mapImage = stockImages[0];
		LondonData.loadData();
		findAllStops();
        check_digests(stations.toArray());	// verify that the chips have different digests
        check_digests(cards.toArray());
		dot = stockImages[0];
		}
	}
	
	public Platform getPlatform(Line line,String platid)
	{	
		for(Stop stop : stops) 
		{
			if(stop.getLine()==line)
			{
				return(stop.getPlatform(platid));
			}
		}
		throw G.Error("Platform %s on %s not found",platid,line);
	}
	// methods for OnedayLocation
	public double getX() { return(xpos); }
	public double getY() { return(ypos); }
	public Station getStation() { return(this); }
	public Stop getStop() { return(null); }
	public Line getLine() { return(null); }
	public Platform getPlatform() { return(null); }
	public Train getTrain() { return(null); }

}