package online.game;

import com.codename1.ui.geom.Rectangle;

import bridge.*;
import lib.Image;
import lib.Graphics;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.SimpleObservable;
import lib.SimpleObserver;
import lib.Sort;
import lib.StockArt;
import online.common.*;
import online.search.SimpleRobotProtocol;

/** this class holds information about players.  It's used by both the game controller
 * and the game client.  Rectangles defined in the player object should be laid out
 * by the game's {@link online.common.exCanvas#setLocalBounds} method
 *
 */
public class commonPlayer implements Opcodes,lib.CompareTo<commonPlayer>
{ //the main properties
    public int boardIndex = -1; //index in board data structures
    public int channel = -1; 	//communication channel, or fake channel for robots
    private int order = -1; 		//ordering number, offset by 1000 so it can't be confused with an index
    public int getOrder()
    { 	return(order);
    }
    private String hostUID = "";
    public void setHostUID(String n) { hostUID = n; }
    public String getHostUID()
    {	return(hostUID);
    }
    public void setOrder(int o)
    	{ G.Assert(o>=-1 && o<=6,"bad order %s",o);
    	  order = o; 
    	  if(launchUser!=null) 
    	  	{ // when resuming a game, the order changes and the launchuser
    		  // is used to trigger the sound cues for turn change.
    	  	  launchUser.order = o;
    	  	}
    	}
    public boolean restored = false;
    public int getPosition()
    {	
    	return(order);	// eliminate seat as a consideration
    }
    
    public boolean readyToPlay = false; //tiles have been chosen etc
    public long readyToPlayTime = 0;	// time at which readytoplay became true
    public boolean startedToPlay = false;	// actually running the game loop
    public boolean spectator; //true if we are a spectator	
    public String playerString=null;
    public String playerString() 
    	{ return("P"+boardIndex);
    	}
    //other properties
    public String IP = ""; 		//ip as seen by the server
    public String localIP = ""; //ip as seen locally
    public String uid = null; 	//user unique id.  This identifies the player in the database
    public LaunchUser launchUser = null;	// launch user info

    public long clock = 0; 		//clock skew between local GMT and the server's GMT
    public long ping = -1; 		//ping time
    public String qcode = null; //quit code, default to null
    public void setQcode(String s) { qcode = s; }
    public boolean primary = false; //are we the primary player or one of the others

    //stuff for viewer
    public Image pic = null; //picture
    private boolean fetchingImage=false; 
    public boolean imageComplete; //picture ready
    
    public ExtendedHashtable info = new ExtendedHashtable();//share with viewer
    public String trueName = null; 				//true name of the user
    public String userName = ""; 				//public name initially ""
    public long lastInputTime = 0;				// last input (used for timeouts)
    public int progress = 0;					// progress (used for robots)
    public double qprogress = 0.0;
    public long elapsedTime = 0;				// time used on the user's clock in milliseconds
    public long elapsedTimeWhenInactive = 0;		// time used before the last time it was changed
    public boolean elapsedTimeFrozen = false;	// true of time is on pause
    public boolean timeIsInactive = false;		// true if this time is for an inactive player
    public void setElapsedTimeFrozen(boolean val) 
    { 	elapsedTimeFrozen = val;
    }
    public void setTimeIsInactive(boolean val) 
    { 	if(!timeIsInactive)
    	{ timeIsInactive = val;
    	  elapsedTimeWhenInactive = elapsedTime;
    	}
    }
    public long timeWhenLastInactive()
    {
    	if(timeIsInactive) { return(elapsedTime); }
    	else { return(elapsedTimeWhenInactive); }
    }
    
    public long reviewTime = 0;					// most recent time update in review mode
    public boolean updated = false;
    public int mouseX = -1;
    public int mouseY = -1;
    public int mouseObj = NothingMoving;
    public int drawnMouseX = -1;
    public int drawnMouseY = -1;
    public long drawnMouseTime = 0;
    public String mouseZone = "off";
    public int focuschanged = 0; //count of focus changes

    //stuff for robot player
    public boolean isRobot = false;			// I'm the robot
    public boolean isProxyPlayer = false;	// I'm an extra player on the same console
    private String robotWait = null; //if null, the current robot can move
    public SimpleRobotProtocol robotPlayer = null; //if not null, handle to the robot move generator
    public commonPlayer robotRunner = null;		// player who is running this robot
    public int lastRobotProgress = 0;
    SimpleObservable observer = null;
    
    //
    // rectangles in the user interface
    public Rectangle playerBox = new Rectangle();
    /**
     * this rectangle will display the player's clock time 
     */
    public Rectangle timeRect = new Rectangle();		// clock
    /** this rectangle will display the "live network" spinner for this player */
    public Rectangle animRect = new Rectangle();		// network live animaion
    /** this rectangle will display the player name */
    public Rectangle nameRect = new Rectangle();		// name
    /** this rectangle will display the player's avatar image */
    public Rectangle picRect = new Rectangle();			// avatar
    /** 
     * if initialized by setLocalBounds, this will display the
     * time differential relative to the other player
     */
    public Rectangle extraTimeRect = new Rectangle();	// extra time for tournament mode
    // add rectangles for the "show rectangles" development mode
    public void addRectangles(exCanvas to,int idx)
    {	idx++;
    	to.addRect("timeRect"+idx,timeRect);
    	to.addRect("animRect"+idx,animRect);
    	to.addRect("nameRect"+idx,nameRect);
    	to.addRect("picRect"+idx,picRect);
    	to.addRect("extratimeRect"+idx,extraTimeRect);
    }
    public double displayRotation =  0;

    public commonPlayer(int iidx)
    {  	boardIndex = iidx;
        order = iidx;		// offset so it can't be used as an index
        channel = 50000 + iidx; //channels are fake
  }
    public SimpleObservable addObserver(SimpleObserver o)
    {
        if (observer == null)
        {
            observer = new SimpleObservable();
        }

        observer.addObserver(o);

        return (observer);
    }
    public Rectangle setPlayerBox()
    {	G.SetWidth(playerBox, -1);
    	G.union(playerBox, timeRect,animRect,nameRect,picRect,extraTimeRect);
    	return(playerBox);
    }
    /**
     * set the rotation of the gc, and adjust the coordinates of the hp so view and mouse sensitivity
     * are rotated with the player box
     * @param gc
     * @param hp
     * @param reverse
     */
    public void setRotatedContext(Graphics gc,HitPoint hp,boolean reverse)
    {
    	if(displayRotation!=0)
    	{
    		if(reverse) { GC.unsetRotatedContext(gc, hp); }
    		else { GC.setRotatedContext(gc,playerBox,hp,displayRotation); }
    	}
    }
    public void setRotation(Rectangle r,Graphics gc,HitPoint hp,boolean reverse)
    {
    	if(displayRotation!=0)
    	{
    		int cx = G.centerX(r);
    		int cy = G.centerY(r);
    		double rot = reverse ? -displayRotation : displayRotation;
    		GC.setRotation(gc, rot, cx, cy);
    		G.setRotation(hp, rot, cx, cy);
    	}
    }
    /**
     * rotate x,y around the player's center.  This helps animations hit the 
     * right target for player boxes rotated by 90 degrees.
     * @param c
     * @param x
     * @param y
     */
    public void rotateCurrentCenter(cell<?> c,int x,int y)
    {	if(displayRotation!=0)
    	{	int px = G.centerX(playerBox);
    		int py = G.centerY(playerBox);
    		c.setCurrentCenter( G.rotateX(x, y, displayRotation, px, py),
    							G.rotateY(x, y, displayRotation, px, py));
    		c.setCurrentRotation(displayRotation);
    	}
    	else 
    	{ c.setCurrentCenter(x,y);
    	  c.setCurrentRotation(0);
    	}
    }


    public void deleteObserver(SimpleObserver o)
    {
        if (observer != null)
        {
            observer.removeObserver(o);

            if (observer.countObservers() == 0)
            {
                observer = null;
            }
        }
    }
    /**
     * return true if the point is inside the player box, considering
     * the rotation associated with the player.  This is intended to be
     * used to make sprites appear with the same orientation that the
     * player is using on playtables.
     * @param hp
     * @return
     */
    public boolean inPlayerBox(HitPoint hp)
    {	boolean val = false;
    	val = G.pointInRect(hp, rotatedPlayerBox());
    	return(val);
    }
    private Rectangle rotatedBox =null;
    public Rectangle rotatedPlayerBox()
    {
    	switch(G.rotationQuarterTurns(displayRotation))
    	{
    	case 0:
    	case 2: return(playerBox);
    	default:
    			rotatedBox = G.copy(rotatedBox,playerBox);
    			G.setRotation(rotatedBox, displayRotation);
    			return(rotatedBox);
    	}
    }
    static public commonPlayer findPlayerByIndex(commonPlayer[] players, int ind)
    { for(int i=0;i<players.length;i++) 
    	{ commonPlayer p = players[i];
    		if((p!=null)&&(p.boardIndex==ind)) { return(p); }
    	}
    	return(null);
    }

    public void setChanged()
    {
        updated = true;

        if (observer != null)
        {
            observer.setChanged(this);
        }
    }

    //
    // copy state from p in preparation for taking over playing
    //
    public void bePlayer(commonPlayer p)
    {
        boardIndex = p.boardIndex;		//current index in player array
        channel = p.channel; 	//the 220 message will change our channel
        order = p.order;		//order of play, lowest goes first
        mouseObj = NothingMoving;			// not tracking anything
        elapsedTime = p.elapsedTime;
        elapsedTimeFrozen = p.elapsedTimeFrozen;
        elapsedTimeWhenInactive = p.elapsedTimeWhenInactive;
        timeIsInactive = p.timeIsInactive;
        reviewTime = p.reviewTime;
        //System.out.println( trueName+" is now "+order); 
        spectator = false;
    }

    public synchronized void setRobotWait(String reason, String context)
    {   //G.print("Set "+trueName+" "+reason+" "+context+" "+Thread.currentThread()+" "+this);
       
        robotWait = reason;
    }
    public synchronized String robotWait() { return(robotWait); }

    public void startRobot(SimpleRobotProtocol pl,commonPlayer runner)
    {	
        robotPlayer = pl;
        robotRunner = runner;
        setRobotWait(null,"startRobot");
        qprogress = 0.0;
    }

    public boolean robotStarted()
    {
        return ((robotPlayer != null) && (robotPlayer.Auto()));
    }

    public SimpleRobotProtocol robotRunning()
    {
        return (((robotPlayer != null) 
        		&& (( robotWait()!=null ) || (robotPlayer.Running())))
        		? robotPlayer 
        		: null);
    }
    public SimpleRobotProtocol robotBeingMonitored()
    {
        return (((robotPlayer != null) 
        		&& (( robotWait()!=null ) || (robotPlayer.beingMonitored())))
        		? robotPlayer 
        		: null);
    }

    public void runRobot(boolean run)
    {
        if (robotPlayer != null)
        {
            robotPlayer.StartRobot(run,this);
        }
    }

    public void startRobotTurn()
    {
        if (robotPlayer != null)
        {
            robotPlayer.DoTurnStep(this.boardIndex);
        }
    }
    public boolean pauseRobot()
    {
        if (robotPlayer != null)
        {
            robotPlayer.Pause();

            return (true);
        }

        return (false);
    }
    public boolean resumeRobot()
    {
        if (robotPlayer != null)
        {
            robotPlayer.Resume();

            return (true);
        }

        return (false);
    }
    public boolean notifyRobot(commonMove m)
    {
        if (robotPlayer != null)
        {
            robotPlayer.Notify(m);

            return (true);
        }

        return (false);

    }
    public boolean stopRobot()
    {
        if (robotPlayer != null)
        {
            robotPlayer.Quit();
            robotPlayer = null;
            robotRunner = null;
            // robotWait = null; // don't clear robotWait, it is needed to suppress
            // the last move, in transit for the winning move.
            qprogress = 0.0;

            return (true);
        }

        return (false);
    }
    private long lastAsk = 0;
    public String getRobotMove()
    {  	SimpleRobotProtocol r = robotRunning();
		if(r!=null)
		{ long now = lastInputTime = G.Date();
		  if((now-lastAsk)>500)
		  { UpdateProgress(r.getProgress());
		    commonMove m = r.getResult();
		    if(m!=null) 
		    { 	//G.print("finish "+m+" for "+this);
		    	return(m.moveString()); 
		    	}
		    lastAsk = now;
		  }
		}
		return(null);
	}


    static public commonPlayer findPlayerByChannel(commonPlayer[] players,
        int inInt)
    {
        //get a player (not spectator) index associated with a player or spectator ID
    	if(players==null) { return(null); }
        for (int i = 0; i < players.length; i++)
        {
            commonPlayer p = players[i];

            if ((p != null) && (p.channel == inInt))
            {
                return (p);
            }
        }

        return (null);
    }
    // return the next higher channel number than "lowest"
    static public commonPlayer nextPlayerByChannel(commonPlayer[] players,commonPlayer lowest)
    {	
   		commonPlayer best = null;
   	   	if(lowest==null)
    	{	// we want strictly the lowest channel
    		for(int i=0;i<players.length;i++)
    		{	commonPlayer thisplayer = players[i];
    			if(thisplayer!=null)
    			{	if((best==null) || (thisplayer.channel<best.channel)) 
    					{ best=thisplayer; 
    					}
    			}
    		}
     	}
    	else
    	{	// return next higher than lowest
    		for(int i=0;i<players.length;i++)
    		{	commonPlayer thisplayer=players[i];
    			if((thisplayer!=null) && (thisplayer.channel>lowest.channel))
    			{	if((best==null) || (thisplayer.channel<best.channel))
    				{ best = thisplayer;
    				}
    			}
    		}
    		
    	}
   		return(best);
   }
    

    static public commonPlayer findPlayerByPosition(commonPlayer[] players, int col)
    {
        for (int i = 0; i < players.length; i++)
        {
            commonPlayer p = players[i];

            if ((p != null) && (p.boardIndex == col))
            {
                return (p);
            }
        }

        return (null);
    }

    static public int numberOfPlayers(commonPlayer[] players)
    {
        int n = 0;

        for (int i = 0; i < players.length; i++)
        {
            if (players[i] != null)
            {
                n++;
            }
        }

        return (n);
    }

    static public void initPlayers(commonPlayer[] players,boolean reviewer)
    {
        for (int i = 0; i < players.length; i++)
        {
            players[i] = reviewer ? new commonPlayer(i) : null;
        }
    }


    static public commonPlayer firstPlayer(commonPlayer[] players)
    {
        commonPlayer best = null;

        for (int i = 0; i < players.length; i++)
        {
            commonPlayer p = players[i];

            if (p != null)
            {	G.Assert(p.order>=0,"player order is not set");
                if ((best == null) || (p.order < best.order))
                {
                    best = p;
                }
            }
        }

        return (best);
    }

    static public commonPlayer nextPlayer(commonPlayer[] players,
        commonPlayer first)
    {
        commonPlayer best = null;
        G.Assert(first != null, "first player can't be null");

        for (int i = 0; i < players.length; i++)
        {
            commonPlayer p = players[i];

            if ((p != null) && (p != first))
            {	G.Assert(p.order>=0,"player order is not set");
                if (p.order > first.order)
                {
                    if ((best == null) || (p.order < best.order))
                    {
                        best = p;
                    }
                }
            }
        }

        return (best);
    }

    static public commonPlayer circularNextPlayer(commonPlayer[] players,
        commonPlayer first)
    {
        commonPlayer p = nextPlayer(players, first);

        if (p == null)
        {
            p = firstPlayer(players);
        }

        return (p);
    }

    public boolean sameIP(commonPlayer[] players)
    {
        for (int i = 0; i < players.length; i++)
        {
            commonPlayer p = players[i];

            if ((p != null) && (p != this) && (IP.equals(p.IP)))
            {
                return (true);
            }
        }

        return (false);
    }

    public boolean sameHost(commonPlayer[] players)
    {
        for (int i = 0; i < players.length; i++)
        {
            commonPlayer p = players[i];

            if ((p != null) && (p != this) && (localIP.equals(p.localIP)))
            {
                return (true);
            }
        }

        return (false);
    }


    public boolean sameClock(commonPlayer[] players)
    {
        long start1 = clock;
        long end1 = clock + ping;

        for (int i = 0; i < players.length; i++)
        {
            commonPlayer p = players[i];

            if ((p != null) && (p != this))
            {
                long start2 = p.clock;
                long end2 = start2 + p.ping;

                if (((start2 >= start1) && (start2 <= end1)) ||
                        ((end2 >= start1) && (end2 <= end1)))
                {
                    return (true);
                }
            }
        }

        return (false);
    }

    public void UpdateProgress(double v)
    {
        if (qprogress != v)
        {
            qprogress = v;
            setChanged();
            UpdateLastInputTime();
       }

    }

    void UpdateLastInputTime()
    {
        lastInputTime = G.Date();
        progress++;
    }
    public void ClearLastInputTime()
    {
    	lastInputTime = 0;
    }
    /**
     * set the elapsed time from a string formatted hh:mm:ss mm:ss or ss
     * @param val
     */
    public void setElapsedTime(String val)
    {	if(val!=null)
    	{
    	int hr=0;
    	int min=0;
    	int sec = 0;
    	int num=0;
    	for(int lim=val.length(),idx=0; idx<lim; idx++)
    	{	char ch = val.charAt(idx);
    		if(Character.isDigit(ch)) { num = num*10+(ch-'0'); }
    		if(ch==':') { hr = min; min=sec; sec = num; num=0; }
    	}
    	hr=min; min=sec; sec=num;
    	min += hr*60;
    	sec += min*60;
    	setElapsedTime(sec*1000);
    	}
    }
    public void setElapsedTime(long val)	// value in milliseconds
    {	long et = elapsedTime;
    	 if(!elapsedTimeFrozen) { elapsedTime=val; }
        if ((et/1000) != (val/1000))
        {	// don't make the player object seem to have changed too often
            setChanged();
        }
    }
    public void setReviewTime(long val)
    {
    	long et = reviewTime;
    	reviewTime = val;
    	if((et/1000)!=(val/1000))
    	{
    		setChanged();
    	}
    }

    private synchronized final boolean mouseTrackingInternal(String zone,int inx, int iny,
        int obj)
    {
        if ((mouseX != inx) || (mouseY != iny) || (!mouseZone.equals(zone)) ||
                (mouseObj != obj))
        {
            mouseX = inx;
            mouseY = iny;
            mouseZone = zone;
            mouseObj = obj;
            return (true);
        }

        return (false);
    }

    public void mouseTracking(String zone,int inx, int iny,int obj)
    {
        if (mouseTrackingInternal(zone,inx, iny,obj))
        {
            setChanged();
        }
    }
    public void mouseTrackingObject(int obj) 
    { mouseObj = obj; 
    }
    
    public void setMouseTrack(String zone)
    {
        mouseZone = zone;
    }
    // count the number of players who have lost their connections
    public static int numberOfVacancies(commonPlayer playerConnections[])
    {
        int n = 0;

        for (int i = 0; i < playerConnections.length; i++)
        {
            commonPlayer p = playerConnections[i];

            if ((p==null) || (p.qcode != null) || (p.order<0))
            {
                n++;
            }
        }

        return (n);
    }
    public void setPlayerName(String name, boolean istrue,SimpleObserver o)
    {	if(name!=null)
    {	
        if (("".equals(userName)) || !istrue)
        {
            userName = name;
        }

        if (istrue || (trueName == null))
        {	
            fetchingImage = false;
            pic = null;
            trueName = name;
        }
        if(o!=null) { addObserver(o); }
        imageComplete = false;
        setChanged();
    }
    }

    public void setPlayerInfo(String key, String val)
    {
        info.put(key, val);
        if(exHashtable.TIME.equals(key)) 
        { setElapsedTime(val);
        }
    }

    public String getPlayerInfo(String key)
    {
        String val = (String) info.get(key);
        return (val);
    }

    public String colourString()
    {
        return ((boardIndex < 0) ? "None" : ("Player " + boardIndex));
    }

    public int colourIndex()
    {
        return ((boardIndex < 0) ? 0 : boardIndex);
    }
    public int seatIndex()
    {
    	return(boardIndex);
    }
    public String toString()
    {	String robo = 
    		isRobot 
    			? "R"+ ((robotPlayer==null)?" null ": " T ") + ((robotRunner==null) ? " null" : robotRunner.trueName) 
    			: "";
        return ("Player:" + trueName + ":i" + boardIndex+":o"+order+":#"+channel+robo);
    }
    public String trueName() { return(trueName); }
    public String userName() { return(userName); }
    public String prettyName(String defaultName)
    {
        return ((trueName != null) ? userName : defaultName);
    }
    //
    // ddyer 4/2020
    // observed in sample code, there is a limit to the number of images
    // that can be fetching simultaneously.  What I notices was that when 
    // fetching 6 pictures, the other thread scaling other images would stall.
    //
    static int fetchersActive = 0;
    public Image getPlayerImage()
    {	final String name = userName.toLowerCase();
    	Image p = null;
    	boolean fetchit = false;
    	synchronized (this) 
    	{ p = pic;
    	  if((p==null) 
    	    && (name!=null) 
    	    && !"".equals(name) 
    	    && (fetchersActive<2)
    	    && !fetchingImage) 
    	  	{ fetchingImage = fetchit = true; } 
    	}
    	if(fetchit)
    	{	final commonPlayer me = this;
   			fetchersActive++;
     		new Thread(new Runnable() 
    			{ public void run() 
    				{
				        try
				        {
				            URL picurl = G.getUrl(Config.getPicture + "?pname=" +
				                    name.toLowerCase(), true);
    					 me.pic = Image.getURLImage(picurl);
				            setChanged();
				        }
				   		catch (Throwable err)
				        {
    					G.print("loading player image" + name + " : " + err);
    				}
    				finally { fetchersActive--; }
    				}}
				    				).start();
				    	}
    	if(p==null) { return(StockArt.Player.image); }
    	return (p);
    }

    public boolean drawPlayerImage(Graphics g,Rectangle dest)
    {
        int currentX = G.Left(dest);
        int currentY = G.Top(dest);
        boolean drawn = false;
        Image im = getPlayerImage();

        if (im != null)
        {
            int imwidth = G.Width(dest);
            int w = im.getWidth();
            int h = im.getHeight();

            if ((w > 0) && (h > 0))
            {
                int maxd = Math.max(w, h);
                double scl = (double) maxd / imwidth;
                int nw = (int) (w / scl);
                int nh = (int) (h / scl);

                if (!drawn)
                { //this papers over a bug in IE 5 (and probably other explorer) where

                    // only a partial image displays.  The bug appears to be a stale
                    // cached copy scaled-down images, made when only part of the image
                    // was available.  We draw at half the desired size until the whole
                    // image is available, then redraw at the desired size.
                    drawn =im.drawImage(g,
                            currentX + ((imwidth - nw) / 2) + 1, currentY,
                            nw / 2, nh / 2);
                }

                if (drawn)
                {
                    imageComplete = drawn =im.drawImage(g,currentX, currentY, nw, nh);
                }
                else
                {
                     // g.setColor(GhostRingColor);
 // fillRect(g,currentX,currentY,nw,nh);
                }
            }
            else
            {
                 // g.setColor(GhostRingColor);
 //  fillRect(g,currentX,currentY,dest.width,dest.height);
            }
        }

        return (drawn);
    }

    public boolean drawProgressBar(Graphics inG, Rectangle r)
    {
        double ind = qprogress;

        if (ind > 0.0)
        {	if(inG!=null)
        	{
        	// in non-debug mode, clip to full width
        	double indc = G.debug() ? ind : Math.min(1.0, ind);
            GC.fillRect(inG,Color.red,G.Left(r), G.Top(r), (int) (G.Width(r) * indc), G.Height(r));
            GC.frameRect(inG,Color.black,G.Left(r), G.Top(r), G.Width(r), G.Height(r));
        	}
            return (true);
        }

        return (false);
    }


    public int compareTo(commonPlayer p)
    {	
    	return(order-p.order);
    }
    public int altCompareTo(commonPlayer other)
    {	return(compareTo(other));
    }
    /**
     * reorder the players, ascending by p.order
     * @param players
     */
    static public void reorderPlayers(commonPlayer players[])
    {	// player list must have nulls only at the end.  Sort on "order"
    	if((players!=null) && (players.length>0) && (players[0]!=null))
    		{
    		int ee = players.length-1;
    		while(players[ee]==null) { ee--; }
    		Sort.sort(players,0,ee);
    		for(int i=0;i<=ee; i++) { players[i].boardIndex = i; }
    		}
    }
    
    /**
     *  add to a list of commonPlayer, extending if necessary.  If both P and Replace are null, 
     *  the list is cleared to all nulls. 
     * @param seedlist
     * @param p  		if not null, is added to the list
     * @param replace	if not null, is replaced by p
     * @return a new list, or null if no room and no expansion
     */
    static public commonPlayer[] changePlayerList(commonPlayer[]seedlist,commonPlayer p,commonPlayer replace,boolean expand)
    {	if(seedlist==null) { seedlist=new commonPlayer[0]; }
    	if((p==null)&&(replace==null)) 
    			{ for(int i=0;i<seedlist.length;i++) { seedlist[i]=null; }
    			  return(seedlist);
    			}
       	for(int i=0;i<seedlist.length;i++)
    	{ commonPlayer r = seedlist[i];
    	  if(r==replace) 
    	  	{ seedlist[i]=replace=r=null; 
    	  	}
    	  if(p!=null) 
    	  {	if(r==p) { return(seedlist); }
    	  	if(r==null) { seedlist[i]=p;  return(seedlist); }
    	  }
    	}
    	if(expand) 
    	{	// needs more room
    		if(p!=null)
    			{commonPlayer ns[] = new commonPlayer[seedlist.length+10];
    			int i=0;
    			for(i=0;i<seedlist.length;i++)
    			{	commonPlayer n = seedlist[i];
    				ns[i]=n;
    			}
    			ns[i] = p;
    			return(ns);
    			}
    		return(seedlist);
    	}
    	return(null);	// no room and can't expand
    }
	/**
	 * lay out the rectangles associated with a player as
	 * a compact group.
	 * @param x
	 * @param y
	 * @param unit
	 * @return the bounding rectangle
	 */
    public Rectangle createRectangularPictureGroup(int x,int y,int unit)
    {
       	int C2 = unit/2;
       	int nameW = unit * 5;
       	int ux3 = unit*3;
       	int nameH = unit+C2;
    	//first player name
    	G.SetRect(nameRect,x,y,	nameW-unit/8,nameH);
    	// first player portrait
    	G.SetRect(picRect,x+nameW,y,ux3+C2,ux3+C2);

       	// time display for first player
    	int timey = y+nameH;
    	int timex = x+unit;
    	G.SetRect(timeRect,timex, timey, ux3,unit);
    	G.AlignLeft(extraTimeRect, timey+unit,timeRect);
             
    	// first player "i'm alive" animation ball
    	G.SetRect(animRect,timex+ux3,timey,unit,unit);
    	G.SetRect(playerBox, x, y, G.Right(picRect)-x,G.Bottom(picRect)-y);
    	return(playerBox);
    }
    public Rectangle createSquarePictureGroup(int x,int y,int CELLSIZE)
    {	Rectangle box = playerBox;
    	G.SetHeight(box, -1);
        int cx4 = CELLSIZE*4;
        int cx2 = CELLSIZE*2;
        //player name
        G.SetRect(nameRect, x, y,		cx4-CELLSIZE/8, CELLSIZE);
        //player portrait
        G.SetRect(picRect, x,y+CELLSIZE, cx4,cx4);        
        //display for first player
        G.SetRect(timeRect, x+cx4,y, cx2, CELLSIZE);
        G.AlignLeft(extraTimeRect,y+CELLSIZE, timeRect);
        // first player "i'm alive" animation ball
        G.SetRect(animRect, x+cx4,y+cx2,CELLSIZE, CELLSIZE);       
        G.union(box, picRect,nameRect,timeRect);
        return(box);
    }
    public Rectangle createVerticalPictureGroup(int x,int y,int CELLSIZE)
    {	Rectangle box = playerBox;
    	G.SetHeight(box, -1);
        int cx4 = CELLSIZE*5;
        int cx3 = CELLSIZE*3;
        int cx2 = CELLSIZE*2;
        //player name
        G.SetRect(nameRect, x, y,		cx4, CELLSIZE);
        //display for first player
        G.SetRect(timeRect, x,y+CELLSIZE, cx3, CELLSIZE);
        G.AlignLeft(extraTimeRect,y+cx2, timeRect);
        // first player "i'm alive" animation ball
        G.SetRect(animRect, x+cx3,y+cx2,CELLSIZE, CELLSIZE);       
        //player portrait
        G.SetRect(picRect, x,y+cx3, cx4,cx4);        
        G.union(box, picRect,nameRect,timeRect);
       return(box);
    }
	public double messageRotation()
	{	double rot = displayRotation;
		return((G.rotationQuarterTurns(rot)==2)?rot:0);
	}

}
