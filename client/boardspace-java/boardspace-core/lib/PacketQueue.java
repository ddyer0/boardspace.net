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
package lib;

/**
* this is a simple fixed length queue
*/
public class PacketQueue<TYPE>
{	boolean logEvents = false;
	static int HISTORYBITS = 3;
	static int DISTRESS_TIME = 30000;		// milliseconds 
	private int QueueRead = 0;				// next read pointer
	private int QueueWrite = 0;				// next write pointer
	private Object[] Queue = null;			// the actual queue
	int countItems = 0;						//total items ever put in the queue
	private long []queueTimes = new long[HISTORYBITS+1];
	private long lastqueueGettime = 0; //time when last item was removed from the queue
	private long lastqueuePuttime = 0; //time last item was put in the input queue
	private long lastqueueWaitStart = 0;	// time when last started waiting for activity
	public void setWaitStart(long val) { lastqueueWaitStart = val; }
	private String myName = "queue";
	public Object inputSemaphore=null;		// object to wake on input
	public boolean dead = false;			// true if this queue is no longer active
	public String toString() { return "<q "+myName+">"; }
	// constructor
	public PacketQueue(String myNam, int len)
	{	countItems = 0;
	    myName = myNam;
	    Queue = new Object[len];
	}
	
	synchronized int getStats()
	{	// items sent - items pending
		int ql = Queue.length;
		return(countItems - (ql+QueueWrite-QueueRead)%ql);
	}

	public synchronized int queueLength()
	{
		return(Math.abs(QueueWrite-QueueRead));
	}

	public void reset()
	{
    lastqueueGettime = G.Date();
    	lastqueuePuttime = 0;
	    countItems = 0;
	    QueueRead = 0;
	    QueueWrite = 0;
	}

	public String stateSummary(String msg)
	{
	    long now = G.Date();
	    String wait = (lastqueueWaitStart>0) 
	    					? " Waiting "+(now-lastqueueWaitStart)
	    					: " not waiting";
	    return (" " + msg +"#"+countItems
	    		+ "(" + (now - lastqueuePuttime) 
	    		+ " since put," 
	    		+ (now - lastqueueGettime) 
	    		+ " since take, " 
	    		+ "Queue: " + queueLength()
	    		+ wait);
	}
	public String getHistory()
	{
		int ptr = QueueWrite-HISTORYBITS-1;
		int len = Queue.length;
		if(ptr<0) { ptr+=len; }
		StringBuilder msg = new StringBuilder();
		for(int i=0;i<=HISTORYBITS;i++)
		{
			long time = queueTimes[ptr&HISTORYBITS];
			Object item = Queue[ptr%len];
			msg.append(G.timeString(time));
			msg.append(":");
			msg.append(item);
			msg.append("\n");
			ptr++;
		}
		return(msg.toString());
	}
	
	public boolean healthCheck()
	{
		long now = G.Date();
		if(lastqueueGettime==0) { return(true); }
		long dif1 = now-lastqueuePuttime;
		long dif2 = now-lastqueueGettime;
		return((dif1<DISTRESS_TIME) &&  (dif2<DISTRESS_TIME));
	}
	@SuppressWarnings("unchecked")
	public synchronized TYPE peekItem()
	{
		 if ((QueueRead != QueueWrite) && (Queue[QueueRead] != null))
		 {
			 return((TYPE)(Queue[QueueRead])); 
		 }
		 return(null);
	}
	public synchronized boolean hasData()
	{
		return (dead || ((QueueRead != QueueWrite) && (Queue[QueueRead] != null)));
	}
	/** extract an item from the  queue */
	public synchronized TYPE getItem()
	{
	    if (!dead && hasData())
	    {
	        lastqueueGettime = G.Date();
	
	        @SuppressWarnings("unchecked")
			TYPE tempString =(TYPE)(Queue[QueueRead]);
	        QueueRead += 1;
	        if(logEvents) 
    		{ int ne = QueueWrite-QueueRead;
    		  if (ne<0) { ne += Queue.length; }
    		  //G.addLog("Rem "+ne); 
    		  //if(ne==0) { G.startLog("queue empty"); } 
    		}
	        if (QueueRead == Queue.length)
	        {   QueueRead = 0;
	        }
	        //G.doDelay(1000); //slow down the communucations to simulate slow networks
	        return (tempString);
	    }
	
	    return (null);
	}

	/** add an item to the  queue, return an informative string as an error */
	public synchronized String putItem(TYPE inStr)
	{     
		if(dead) { return(null); }
	    long now = G.Date();
	    int oldQueueWrite = QueueWrite;
	    int newQueueWrite = oldQueueWrite + 1;
	    countItems++;
	    if (newQueueWrite == Queue.length)
	    {
	        newQueueWrite = 0;
	    }
	
	    if (newQueueWrite != QueueRead)
	    {	if(logEvents) 
	    		{ int ne = QueueWrite-QueueRead;
	    		  if (ne<0) { ne += Queue.length; }
	    		  //G.addLog("Add "+(ne+1));
	    		}
	        lastqueuePuttime = now;
	        queueTimes[QueueWrite&HISTORYBITS] = now; 
	        Queue[QueueWrite] = inStr;
	        QueueWrite = newQueueWrite;
	        G.wake(this);
	        Object sem = inputSemaphore;
	        if((sem!=null) && (oldQueueWrite==QueueRead))
	        {	G.wake(sem);
	        }
	        return (null);
	    }
	    // complain if the user tried to overstuff the queue
	    long dif = now - lastqueueGettime;
	    String msg = myName + " Tried to write to full "
	    			+myName+", last taken" 
	    			+(dif / 1000.0) + " seconds ago";
	    return (msg);
	}
}
