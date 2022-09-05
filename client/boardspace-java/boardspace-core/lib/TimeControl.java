package lib;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.util.StringTokenizer;

import bridge.Config;
import online.common.exCanvas;
import online.game.commonPlayer;
import online.game.commonCanvas;

public class TimeControl implements Config
{	
	public TimeControl(Kind k)
	{
		kind = k;
		fixedTime = k.defaultFixedTime;
		differentialTime = k.defaultDifTime;
		moveIncrement = k.defaultMoveTime;
	}
	public TimeControl copy() 
	{ 	return new TimeControl(kind).copyFrom(this);
	}
	public TimeControl copyFrom(TimeControl from)
	{	kind = from.kind;
		fixedTime = from.fixedTime;
		differentialTime = from.differentialTime;
		moveIncrement = from.moveIncrement;
		return(this);
	}
	public void set(Kind k,int f,int d,int i)
	{
		kind = k;
		fixedTime = f;
		differentialTime = d;
		moveIncrement = i;
		
	}

	private static String SecondsMessage = "#1{ seconds, second, seconds}";
	private static String MinutesMessage = "#1{##zero minutes, minute, minutes}";
	private static String PrettyFixedTimeMessage = "Time: #1 minutes";
	private static String PlusTimeMessage = "fixed + per move";
	private static String TimeDifMessage = "fixed + more than opponent";
	private static String PrettyDifTimeMessage = "Time: #1 minutes, and #2 more than opponent";
	private static String PrettyPlusTimeMessage = "Time: #1 minutes plus #2 per move";
	private static String FixedTime = "fixed time";
	public static String TimeControlToolTip = "timecontroltip";
	private static String NoTime = "No time control";
	private static String TimeExpiredMessage = "Time has expired for #1";
	private static String EndGameMessage = "end the game with a loss for #1";
	private static String ChangeLimitsMessage = "change the time limits";
	public static String []TimeControlStrings = {
		FixedTime,
		ChangeLimitsMessage,
		EndGameMessage,
		TimeExpiredMessage,
		SecondsMessage,
		MinutesMessage,
		PrettyDifTimeMessage,
		PrettyPlusTimeMessage,
		PrettyFixedTimeMessage,
		NoTime,
		PlusTimeMessage,
		TimeDifMessage,
	};
	public static String [][]TimeControlStringPairs = {
			{TimeControlToolTip,"Time control for this game:\n#1"},
		};
	public String toString() { return("<time "+print()+">"); }
	
	public String prettyPrint()
	{
		InternationalStrings s = G.getTranslations();
		switch(kind)
		{
		default: G.Error("Not expecting kind %s",kind);
		case None: 
			return(s.get(kind.TimeMessage));
		case Fixed: 
			return(s.get(PrettyFixedTimeMessage,G.briefTimeString(fixedTime)));
		case Differential: 
			return(s.get(PrettyDifTimeMessage,G.briefTimeString(fixedTime),G.briefTimeString(differentialTime)));
		case PlusTime: 
			return(s.get(PrettyPlusTimeMessage,G.briefTimeString(fixedTime),G.briefTimeString(moveIncrement)));
		}
	}
	
	public String print()
	{	String times = "";
		switch(kind)
		{
		default: G.Error("Not expecting kind %s",kind);
		case None: break;
		case Fixed: times = " "+fixedTime; break;
		case Differential: times = " "+fixedTime+" "+differentialTime; break;
		case PlusTime: times = " "+fixedTime+" "+moveIncrement; break;
		}
		return(kind.name()+times);
	}
	public static TimeControl parse(String msg)
	{
		StringTokenizer tok = new StringTokenizer(msg);
		return(parse(tok));
	}
	public static TimeControl parse(StringTokenizer tok)
	{
		Kind k = Kind.find(tok.nextToken());
		TimeControl val = null;
		if(k!=null)
		{
		val = new TimeControl(k);
		switch(k)
		{
		default: G.Error("Not expecting kind %s", k);
		case None: break;
		case Fixed: 
			val.fixedTime = G.IntToken(tok);
			break;
		case Differential:
			val.fixedTime = G.IntToken(tok);
			val.differentialTime = G.IntToken(tok);
			break;
		case PlusTime:
			val.fixedTime = G.IntToken(tok);
			val.moveIncrement = G.IntToken(tok);
			break;
		}}
		// always return something, no matter what junk was in the parse tokens
		if(val==null) { val = new TimeControl(Kind.None);}
		return(val);
	}
	public enum Kind {
		
		None(NoTime,0,0,0),
		Fixed(FixedTime,10*60*1000,0,0),
		PlusTime(PlusTimeMessage,10*60*1000,30*1000,0),
		Differential(TimeDifMessage,10*60*1000,0,10*60*1000);
		public String TimeMessage="";
		public int defaultFixedTime = 0;
		public int defaultDifTime = 0;
		public int defaultMoveTime = 0;
		Kind(String name,int f,int m,int d) 
		{ TimeMessage = name;
		  defaultFixedTime = f;	// milliseconds
		  defaultDifTime = d;
		  defaultMoveTime = m;
		}
		public static Kind find(String msg)
		{
			for(Kind k : values()) { if(k.name().equalsIgnoreCase(msg)) { return(k); }}
			return(null);
		}

	}
	public int fixedTime;
	public int differentialTime;
	public int moveIncrement;
	public Kind kind = Kind.None;
	

	public enum TimeId implements CellId
	{
		GameOverOnTime,
		ChangeTimeControl,
		ChangeFutureTimeControl,
		ChangeMode,
		ChangeFixedTime,
		ChangeDifferentialTime,
		ChangeIncrementalTime,
		;
		public String shortName() { return(name()); }
		public static TimeId find(String name)
		{	if(name.charAt(0)=='+') { name = name.substring(1); }
			for(TimeId v : values()) { if(v.name().equals(name)) { return(v); }}
			return(null);
		}
	}
	public int timeOverResolution = 0;
	

	public boolean performStandardActions(HitPoint hp,commonCanvas canvas)
	{	CellId id = hp.hitCode;
		if(id instanceof TimeId)
		{
		TimeId tid = (TimeId)id;
		int left = G.Left(hp);
		int top = G.Top(hp);
		switch(tid)
		{	case ChangeTimeControl:
			case GameOverOnTime:
	  			canvas.performClientMessage("+" + id.shortName(),true,true);
	  			return(true);
	  			
	  		// the rest of these pop up menus
			case ChangeMode:
				changeTimeControlKind(left,top,canvas,canvas);
				return(true);
			case ChangeIncrementalTime:
				changeSeconds(left,top,canvas,canvas);
				return(true);
			case ChangeFixedTime:
				changeMinutes(left,top,canvas,canvas,2);
				return(true);
			case ChangeDifferentialTime:
				changeMinutes2(left,top,canvas,canvas);
				return(true);
			default: G.Error("not handled %s", tid);
		}}
		return(false);
	}
	
	public boolean performMessage(commonCanvas canvas,String op,String rest)
	   {
		   TimeId v = TimeId.find(op);
		   if(v!=null)
		   {
		   switch(v)
		   {
		   case ChangeFutureTimeControl:
			   TimeControl tc = parse(rest);
			   copyFrom(tc);
			   return(true);
		   case GameOverOnTime:
			   canvas.doEndGameOnTime();
			   return(true);
		   case ChangeTimeControl:
			   canvas.adoptNewTimeControl();
			   return(true);
		   default: G.Error("Can't handle %s",v);
		   }}
		   return(false);
	   }
	/**
	 * true if the time has expired.  Times are in milliseconds.
	 * For plustime, dif should be the accumulated bonus time
	 * For differential time, dif should be the time more (or less) than the opponent
	 * 
	 * @param time the fixed time charged to the player (in milliseconds)
	 * @param dif the differential time charged to the player
	 * @return
	 */
	public boolean isExpired(long time, long dif)
	{	
		switch(kind)
		{
		default:
			G.Error("case %s not handled",kind);
		case None:
			return(false);
		case Fixed:
			return(time>fixedTime);
		case Differential:
			return(time>fixedTime && dif>differentialTime);
		case PlusTime:
			return(time>fixedTime+dif);
		}
	}
	/**
	 * return the time remaining, if we have used time+dif
	 * 
	 * @param time
	 * @param dif
	 * @return
	 */
	public int timeRemaining(long time,long dif)
	{
		switch(kind)
		{
		default:
			G.Error("case %s not handled",kind);
		case None:
			return(Integer.MAX_VALUE);
		case Fixed:
			return((int)(fixedTime-time));
		case Differential:
			return((int)(Math.max(fixedTime-time,differentialTime-dif)));
		case PlusTime:
			return((int)((fixedTime+dif)-time));
		}
	
	}
	private Rectangle modeRect = new Rectangle();
	private Rectangle mainRect = new Rectangle();
	private Rectangle extraRect = new Rectangle();
	
	public boolean inModeRect(int x,int y) { return(G.pointInRect(x,y,modeRect)); }
	public boolean inMainRect(int x,int y) { return(G.pointInRect(x, y,mainRect)); }
	public boolean inExtraRect(int x,int y) { return(G.pointInRect(x, y,extraRect)); }
	// subdivides the time control rectangle into kind and one or two time increment rectangles
	private void partitionTimeControlRect(Rectangle main)
	{	int l = G.Left(main);
		int t = G.Top(main);
		int w = G.Width(main);
		int h = G.Height(main);

		G.SetRect(modeRect ,l,  t,  w*11/20, h);
		G.SetRect(mainRect,l+w*3/5, t, w/5, h);
		G.SetRect(extraRect,l+w*4/5, t, w/5, h);

	}
	//
	// draw time control description and one or two associated times
	// this is used from the lobby, and also from the revised time control overlay
	//
	public void drawTimeControl(Graphics inG,exCanvas canvas,boolean writable,HitPoint hitPoint,
			Rectangle timeControlRect)
	  {	
		partitionTimeControlRect(timeControlRect);
		InternationalStrings s = G.getTranslations();
	  	  int h = G.Height(timeControlRect);
	  	  {
	      boolean hit = G.pointInRect(hitPoint,modeRect);
	      if(hit)
	      {	// it may seem unnecessary to copy, but this rectangle is reshaped multiple times
	    	// when drawing the lobby, and very mysterious misbehavior can result.
	    	  hitPoint.spriteRect = G.copy(null,modeRect);
	    	  hitPoint.spriteColor = Color.red;
	    	  hitPoint.hitCode = TimeId.ChangeMode;
	      }
	  	  int wt = GC.Text(inG, false, modeRect,Color.black,null, s.get(kind.TimeMessage)+"   ");
	  	  if(writable)
	  	  { int sz = h*(hit?4:3)/5;
	  		StockArt.Pulldown.drawChip(inG,canvas,sz,
	      			G.Left(modeRect)+wt,
	      			G.centerY(modeRect),"");
	  	  }}
	  	  {
	  	  boolean main = true;
	  	  switch(kind)
	  	  {
	  	  default: G.Error("Not expecting %s",kind);
	  	  case None:
	  		  main = false;
	  		  break;
	  	  case PlusTime:
	  	  	{ boolean hit = G.pointInRect(hitPoint, extraRect);
	  	  	  if(hit)
	  	  	  {	  // it may seem unnecessary to copy, but this rectangle is reshaped multiple times
		  		  // when drawing the lobby, and very mysterious misbehavior can result.
	  	  		  hitPoint.spriteRect = G.copy(null, extraRect);
	  	  		  hitPoint.spriteColor = Color.red;
	  	  		  hitPoint.hitCode = TimeId.ChangeIncrementalTime;
	  	  	  }
	  	  	  int wt = GC.Text(inG, false, extraRect, Color.black, null,
	  				  	G.briefTimeString(moveIncrement)+"   ");
	  	  	if(writable)
	  	  		{int sz = h*(hit?4:3)/5;
	  	  		 StockArt.Pulldown.drawChip(inG,canvas,sz,
	      			G.Left(extraRect)+wt-h/4,
	      			G.centerY(extraRect),"");
	  	  		}
	  	  	}
	  		  break;
	  	  case Differential:
	  	  	{
	  	  	boolean hit = G.pointInRect(hitPoint, extraRect);
	  	  	if(hit)
	  	  	{	// it may seem unnecessary to copy, but this rectangle is reshaped multiple times
	  	  		// when drawing the lobby, and very mysterious misbehavior can result.
	  	  		hitPoint.spriteRect = G.copy(null, extraRect);
	  	  		hitPoint.spriteColor = Color.red;
	  	  		hitPoint.hitCode = TimeId.ChangeDifferentialTime;
	  	  	}
	  	  	int wt = GC.Text(inG, false, extraRect, Color.black, null,
	  				  		G.briefTimeString(differentialTime)+"   ");
	  		if(writable)
	  			{StockArt.Pulldown.drawChip(inG,canvas,h*(hit?4:3)/5,
	      			G.Left(extraRect)+wt-h/4,
	      			G.centerY(extraRect),"");
	  			}
	  	  	}
	  		  break;
	  	  case Fixed:
	  		  break;
	  	  }
	  	  if(main)
	  	  {
	  	  boolean hit = G.pointInRect(hitPoint, mainRect);
	  	  if(hit)
	  	  {	  // it may seem unnecessary to copy, but this rectangle is reshaped multiple times
	  		  // when drawing the lobby, and very mysterious misbehavior can result.
	  		  hitPoint.spriteRect = G.copy(null, mainRect);
	  		  hitPoint.spriteColor = Color.red;
	  		  hitPoint.hitCode = TimeId.ChangeFixedTime;
	  	  }
		  int wt = GC.Text(inG, false, mainRect, Color.black, null,
				  	G.briefTimeString(fixedTime)+"   ");
		  if(writable)
			{StockArt.Pulldown.drawChip(inG,canvas,h*(hit?4:3)/5,
			G.Left(mainRect)+wt-h/4,
			G.centerY(mainRect),"");
			}
	  	  }}
	  }
	/**
	 * this is used within a 2-player game when the time has expired.  It allows
	 * the game to be ended on the current terms, or for the time controls to 
	 * be changed.  This is called for the *winning* player, so no cooperation
	 * with the losing player is required.
	 * 
	 * @param gc
	 * @param hitPoint
	 * @param expired
	 * @param canvas
	 * @param boardRect
	 */
    public void drawGameOverlay(Graphics gc,HitPoint hitPoint0,commonPlayer expired,commonCanvas canvas,Rectangle boardRect)
    {	InternationalStrings s = G.getTranslations();
    	commonPlayer who = canvas.whoseTurn();
    	boolean robo = (who.robotRunner==expired);
    	HitPoint hitPoint = robo||canvas.ourActiveMove() ? hitPoint0 : null;
    	if(hitPoint!=null) { hitPoint.neutralize(); }
    	int left = G.Left(boardRect);
    	int top = G.Top(boardRect);
    	int width = G.Width(boardRect);
    	int height = G.Height(boardRect);
    	int inside = (int)(width*0.05);
    	int l =left+inside;
    	int t = top+inside;
    	int w = width-inside*2;
    	int h = height-inside*2;
    	Rectangle ir = new Rectangle(l,t,w,h);
    StockArt.Scrim.getImage().stretchImage(gc, ir);  
    	GC.setFont(gc,canvas.largeBoldFont());
    	String pname = expired.prettyName("");
    	String banner = s.get(TimeExpiredMessage,pname);
    	String endgame = s.get(EndGameMessage,pname);
    	String changetime = s.get(ChangeLimitsMessage);
    	GC.Text(gc,true,l,t,w,inside*2,Color.black,null,banner);
    	
    	if(GC.handleSquareButton(gc, new Rectangle(l+inside*2,t+inside*3,w-inside*4,inside), hitPoint, endgame,
    			bsBlue,Color.lightGray))
    	{
    		hitPoint.hitCode = TimeId.GameOverOnTime;
    	}
    	if(GC.handleSquareButton(gc, new Rectangle(l+inside*2,t+(int)(inside*4.5),w-inside*4,inside), hitPoint, changetime,
    			bsBlue,Color.lightGray))
    	{
    		hitPoint.hitCode = TimeId.ChangeTimeControl;
    	}
    	Rectangle timeControlRect = new Rectangle(l+w/6,t+inside*6,4*w/6,inside);
    	drawTimeControl(gc,canvas,hitPoint!=null,hitPoint,timeControlRect);
    	
    }
    
	  private PopupManager timeControlMenu;
	  public PopupManager changeTimeControlKind(int ex,int ey,MenuParentInterface parent,ActionListener deferredEvents)
	  {	timeControlMenu = new PopupManager();
	  	timeControlMenu.newPopupMenu(parent,deferredEvents);
	  	InternationalStrings s = G.getTranslations();
	  	for(TimeControl.Kind timer : TimeControl.Kind.values())
	  	{
	  		timeControlMenu.addMenuItem(s.get(timer.TimeMessage),timer);
	  	} 	
	  	timeControlMenu.show(ex,ey);
	  	return(timeControlMenu);
	  }
	  private PopupManager minutesMenu;
	  /** present a menu of minutes choices */
	  public PopupManager changeMinutes(int ex,int ey,MenuParentInterface parent,ActionListener deferredEvents,int minimum)
	  { minutesMenu = new PopupManager();
	  	minutesMenu.newPopupMenu(parent,deferredEvents);
	  	InternationalStrings s = G.getTranslations();
	  	for(int i=minimum;i<=120;i++)
	  	{	if((i<=12) || (i<60 && i%5==0) || (i%10==0))
	  		{
	  		minutesMenu.addMenuItem(s.get(MinutesMessage,i),i);
	  		}
	  	} 	
	  	minutesMenu.show(ex,ey);
	  	return(minutesMenu);
	  }
	  private PopupManager secondsMenu;
	  /** present a menu of seconds choices */
	  public PopupManager changeSeconds(int ex,int ey,MenuParentInterface parent,ActionListener deferredEvents)
	  {	secondsMenu = new PopupManager();
	  	secondsMenu.newPopupMenu(parent,deferredEvents);
	  	InternationalStrings s = G.getTranslations();
	  	secondsMenu.addMenuItem(s.get(TimeControl.SecondsMessage,2),2);
	  	secondsMenu.addMenuItem(s.get(TimeControl.SecondsMessage,5),5);
	  	for(int i=10;i<=120;i+=10)
	  	{	
	  		secondsMenu.addMenuItem(s.get(TimeControl.SecondsMessage,i),i);
	  	} 	
	  	secondsMenu.show(ex,ey);
	  	return(secondsMenu);
	  }

	  private PopupManager minutesMenu2;
	  /** present a second "minutes" menu */
	  public PopupManager changeMinutes2(int ex,int ey,MenuParentInterface parent,ActionListener deferredEvents)
	  {	minutesMenu2 = new PopupManager();
		InternationalStrings s = G.getTranslations();
	  	minutesMenu2.newPopupMenu(parent,deferredEvents);
	  	for(int i=1;i<=15; i++)
	  	{	
	  		minutesMenu2.addMenuItem(s.get(TimeControl.MinutesMessage,i),i);
	  	} 	
	  	minutesMenu2.show(ex,ey);
	  	return(minutesMenu2);
	  }
	
	  /**
	   * this handles actions triggered from the time control menus
	   * @param target
	   * @param command
	   * @return true if we recognised the hit on target
	   */
	  public boolean handleDeferredEvent(commonCanvas canvas,Object target, String command)
		{
			if(timeControlMenu!=null && timeControlMenu.selectMenuTarget(target))
			{
				kind = (Kind)timeControlMenu.rawValue;
				timeControlMenu = null;
				return(true);
			}
			else if(secondsMenu!=null && secondsMenu.selectMenuTarget(target))
			{
				moveIncrement = secondsMenu.value*1000;
				secondsMenu = null;
				return(true);
			}
			else if(minutesMenu!=null && minutesMenu.selectMenuTarget(target))
			{
				fixedTime = minutesMenu.value*1000*60;
				minutesMenu = null;
				return(true);
			}
			else if(minutesMenu2!=null && minutesMenu2.selectMenuTarget(target))
			{
				differentialTime = minutesMenu2.value*1000*60;
				minutesMenu2 = null;
				return(true);
			}
			return(false);
		}
	  
	  public String timeControlMessage() 
	  {
	  	return((kind!=TimeControl.Kind.None)
	  			? prettyPrint()
	  			: null);
	  }
}

