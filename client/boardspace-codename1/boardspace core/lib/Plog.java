package lib;

/**
 * private log object, continuously logs recent events with low overhead.
 * only a fixed number of events are saved on a ring
 * events are time stamped and thread stamped.
 * events can be atomic or constructed in several steps.
 * 
 * @author ddyer
 *
 */
public class Plog {
	String events[] = null;
	int index = 0;
	int totalEvents = 0;
	int startingEvent = 0;
	public boolean verbose = false;
	public Thread logThread = null;
	StringBuilder eventLog = null;
	
	public Plog(int size)
	{
		events = new String[size];
		index = 0;
		totalEvents = 0;
	}
	
	// start a new event, add timestamps and thread stamps
	public synchronized StringBuilder startEvent()
	{
	    finishEvent();
		setLogThread();
		StringBuilder el = eventLog = new StringBuilder();
		long now60 = G.nanoTime()%60000000000l;			// 60 seconds
		int secs = (int)(now60/1000000000l);			//
		int nanos = (int)(now60%1000000000l);
	    if(isLogThread()) {el.append("E "); }
	    else { el.append(Thread.currentThread().getName()); el.append(" "); }
	   
		int micros = (int)((nanos+500)/1000);
		int millis = micros/1000;
		micros=micros%1000;
		if(secs<10) { el.append(' '); }
		el.append(secs);
		el.append('+');
   		el.append(millis);
   		el.append('.');
   		if(micros<10) { el.append("00"); }
   		else if(micros<100) { el.append("0");}
   		el.append(micros);
		el.append(" ");
		return(el);
	}
	/**
	 * start a new event and leave it open for additional data
	 * @param msg
	 */
	public synchronized StringBuilder appendNewLog(String msg)
	   {		
	    StringBuilder ev = startEvent();	    
   		ev.append(msg);
   		return(ev);
  	   }
	/**
	 * clear the event log
	 */
	public synchronized void restartLog()
	   {	finishEvent();
	   		totalEvents = index = 0;
	   		AR.setValue(events,null);
	   }

	/**
	 * add a complete event
	 * 
	 * @param msg
	 */
	public synchronized void addLog(String msg)
	   {
			appendNewLog(msg);
			finishEvent();
	   }
	public synchronized void addLog(String msg,Object ...args)
	{
		appendNewLog(msg);
		if(args!=null)
		{
		for(int i=0; i<args.length;i++) 
			{ Object ai = args[i];
			  appendLog(ai==null ? "null" : ai.toString());
			}
		}
		finishEvent();
	}
	/** 
	 * true of the current thread is the expected thread for this log
	 * 
	 * @return
	 */
	public boolean isLogThread() { return(Thread.currentThread()==logThread); }
	/**
	 * set the expected thread for this log to the current thread
	 */
	private void setLogThread()
	   {
	   	if(logThread==null) {  logThread = Thread.currentThread(); }
	   }
	
	public synchronized void finishEvent()
	   {StringBuilder ev = eventLog;
		if(ev!=null)
		{	eventLog = null;	
			String msg = events[index++] = ev.toString();
			totalEvents++;
			if(index>=events.length) { index = 0;}		
			if(verbose) { G.print(msg); }
		}
	   }
	/**
	 * append to the current event
	 * @param msg
	 */
	public synchronized void appendLog(String msg)
	   {	StringBuilder ev = eventLog;
	   		if(ev!=null) { ev.append(msg); }
	   }
	/**
	 * append to the current event
	 * @param msg
	 */
	public synchronized void appendLog(int msg)
	   {	StringBuilder ev = eventLog;
  			if(ev!=null) { ev.append(msg); }
	   }
	/**
	 * append to the current event
	 * @param msg
	 */
	public synchronized void appendLog(char msg)
	   {StringBuilder ev = eventLog;
  		if(ev!=null) { ev.append(msg); }
	   }
	public synchronized void appendLog(Object s)
	{	StringBuilder ev = eventLog;
		if(ev!=null) { ev.append(s.toString()); };
	}
	/**
	 * append to the current event
	 * @param msg
	 */
	public synchronized void appendLog(double msg)
	   {	StringBuilder ev = eventLog;
  			if(ev!=null) { ev.append(msg); }
	   }
	/**
	 * get the current event log as a single string with line breaks
	 * @return
	 */
	public synchronized String getLog()
	   {	
		return(getLog(0));
	   }
	public synchronized String getUnseen()
	{	int se = startingEvent;
		startingEvent = totalEvents;
		return getLog(se);
	}
	private synchronized String getLog(int from)
	   {	finishEvent();
			int size = events.length;
			int idx = (totalEvents-from)>=size
							? (index+1)%size		// log is full, we lost some events
							: from%size;	// recent events only
			if(idx!=index)
			{
			StringBuilder b = new StringBuilder();
			while(idx!=index)
			{
				b.append(events[idx]);
				b.append('\n');
				idx++;
				if(idx>=size) { idx = 0; }
			}
			return(b.toString());
			}
			return(null);
		}

	/**
	 * close,restart, and return the current log as a single string
	 * optionally just discard the log
	 * @param discard
	 * @return
	 */
	public synchronized String finishLog(boolean discard)
	{
		String m = discard ? null : getLog();
		if(discard) { restartLog(); }
		return(m);	
	}
	/**
	 * close,restart, and return the current log as a single string
	 * @param discard
	 * @return
	 */
	public String finishLog() { return finishLog(false); }
	/** this is intended to be the permanent system level log
	 * of events, which can be attached to bug reports etc.
	 * this is incorporated into most bug reports automatically
	 * by http.getErrorMessage()
	 */
	public static Plog log = new Plog(100);

}
