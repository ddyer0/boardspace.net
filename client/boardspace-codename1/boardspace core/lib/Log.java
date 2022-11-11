package lib;
/**
 * this log package is intended for debugging.  It records one global log triggered
 * by startlog and ending with finishlog.  It's structured so that addlog events
 * are harmless and have minimal overhead in the absence of an active log.
 * 
 * @author ddyer
 *
 */
public class Log {

	static StringBuilder eventLog = new StringBuilder();
	static long startTimeNanos = 0;
	static long startEventTimeNanos = 0;
	static void initializeLog(String msg)
	   {   synchronized (eventLog)
		   {
		   Log.setLogThread();
	   	    startTimeNanos = G.nanoTime();
	   	    StringBuilder e = new StringBuilder();
	   	    e.append("Start: E ");
	   	    e.append(msg);
	   	    e.append('\n');
	   	    eventLog = e;
		   }
	   }
	public static void startLog(String msg)
	   {	
		    Log.finishLog();
	   		initializeLog(msg+"@"+G.timeString(G.nanoTime()));
	   }
	public static void restartLog()
	   {	synchronized(eventLog)
		   	{
		   if(startTimeNanos==0) { startEventTimeNanos = startTimeNanos = G.nanoTime(); }
		   	}
	   }
	public static void addLog(String msg)
	{
	   	if(startTimeNanos>0)
	    {synchronized(eventLog)
		   	{
		    Log.appendNewLog(msg);
		   	}
	   	}
	}
	
	 public static void addLog(Object... messages)
	 {	if(messages!=null)
	 	{
		 int n = messages.length;
		 if(messages.length>=1)
		 {
			Log.appendNewLog(""+messages[0]);
			for(int i=1;i<n;i++)
			{
				Log.appendLog(""+messages[i]);
			}
		 }
	 	}
	 }
	public static Thread logThread = null;
	public static boolean isLogThread() { return(Thread.currentThread()==logThread); }
	static void setLogThread()
	   {
	   	logThread = Thread.currentThread();
	   }
	public static void finishEvent(long nano)
	   {
		  if(startEventTimeNanos>0) 
		  {	  long interval = (nano-startEventTimeNanos);
		  	  synchronized(eventLog)
		  	  {
		  	  startEventTimeNanos = 0;
		  	  eventLog.append(" ");
		  	  eventLog.append(interval/1000);	// microseconds
			  eventLog.append("uS\n");
		  	  }
		  }
	   }
	public static void finishEvent()
	   {
		   finishEvent(G.nanoTime());
	   }
	public static void appendNewLog(String msg)
	   {	if(startTimeNanos>0)
	   {	synchronized (eventLog)
			   	{ 
		    long now = G.nanoTime();
		    finishEvent(now);
		    startEventTimeNanos = now;
		    long interval = (int)(now-startTimeNanos);
			    if(isLogThread()) {eventLog.append("E "); }
		    else { eventLog.append(Thread.currentThread().getName()); eventLog.append(" "); }
				eventLog.append(msg);
				eventLog.append(" @+");
			int micros = (int)((interval+500)/1000);
			int millis = micros/1000;
			micros=micros%1000;
	   		eventLog.append(millis);	//microseconds
	   		eventLog.append('.');
	   		if(micros<10) { eventLog.append("00"); }
	   		else if(micros<100) { eventLog.append("0");}
	   		eventLog.append(micros);
				eventLog.append(" ");
		   	}}
	   }
	public static void appendLog(String msg)
	   {	if(startTimeNanos>0) 
	   		{synchronized (eventLog)
		   	{		   eventLog.append(msg); }
		    }
	   }
	public static void appendLog(int msg)
	   {		if(startTimeNanos>0) { synchronized (eventLog)
		   {	eventLog.append(msg); }
		    }
	   }
	public static void appendLog(char msg)
	   {
		   if(startTimeNanos>0) {synchronized (eventLog)
			    { eventLog.append(msg); }
		    }
	   }
	public static void appendLog(double msg)
		    {
		   if(startTimeNanos>0) {synchronized (eventLog)
			   { eventLog.append(msg); }
		    }
	   }
	public static void finishLog(boolean discard)
	   {	String str = null;
	   			if(startTimeNanos>0)   		
		   		{
	   			synchronized (eventLog) {
	   				finishEvent();
	   			str = discard ? null : eventLog.toString();
		  		   eventLog.setLength(0);
	   		startTimeNanos = 0;
		  		}}
	   		if(str!=null) { if(G.isSimulator()) { System.out.println(str); } else { G.print(str); }}
	   }
	public static void finishLog() { finishLog(false); }
	public static boolean logActive() { return(startTimeNanos>0); }
	

}
