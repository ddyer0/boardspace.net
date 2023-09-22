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

/** provide a very simple lock. Create an instance of SimpleLock
 and call getLock/Unlock.  This is intended to be used
 as a lockout mechanism while loading images into static
 variables. */
public class SimpleLock {
	String lockName = "SimpleLock";
	public String toString() { return("<lock "+lockName+">");}
	public SimpleLock(String n) { lockName = n; }
    private boolean lock=false;
    private Thread lockThread = null;
    public String lockThreadStack()
    {
    	Thread l = lockThread;
    	if(l!=null) 
    		{ return(G.getStackTrace(l)); 
    		} 
    	return("");
    }
    public Thread locker() { return(lockThread); }
    private String waitedName = null;
    private String lockerName = null;
    private int maxWaitTime = 30000;	// 30 seconds
    private Thread waitExceeded = null;
    String waitExceededMessage = null;
    public int seq = 0;
    /** if the lock is available, lock it and return true */
    public synchronized boolean Lock() 
    	{ Thread myThread = Thread.currentThread();
    	  if(lock) 
    	  	{ if(lockThread==myThread) 
    	  		{
    	  		Unlock();
    	  		throw new ErrorX("recursive lock not allowed, current thread="+myThread+" locking thread="+lockThread);
    	  		}
    	  		else return(false); 
    	  	}
    	  lock=true; 
    	  lockThread = myThread;
    	  waitedName = null;
    	  lockerName = myThread.getName();
    	  return(true); 
    	}
    
    public synchronized void breakLock()
    {	Thread w = lockThread;
    	String trace = G.getStackTrace();
    	waitExceededMessage = "wait time exceeded by "+trace;
    	waitExceeded = w;
    	lockThread = null;
    	lock = false;
    }
    public synchronized boolean UnlockIfHeld()
    {
    	if(lockThread==Thread.currentThread())
    	{
    		lockThread=null;
    		lockerName = null;
    		lock=false;
    		return(true);
    	}
    	else { return(false); }
    }
    /** unlock the lock without checking if we really owned it */
    public synchronized void Unlock() 
    { 	String tell = waitedName;
    	lockThread = null;
    	lockerName = null;
    	waitedName = null;
    	lock=false;
    	if(waitExceeded==Thread.currentThread())
    	{	String m = waitExceededMessage;
    		String trace = G.getStackTrace();
    		waitExceededMessage = null;
    		waitExceeded = null;
    		Http.postError(this,"\n"+m+"\nlocker:\n"+trace,null);
    	}
    	if(tell!=null)
    	{
    		//G.print(""+Thread.currentThread()+" delayed "+tell+" for "+this);
    		//G.print(G.getStackTrace());
    	}
   }
    /** wait until it is available and get the lock */
    public void getLock() 
    	{ long somelock = 0;
    	  String locker = lockerName;
    	  long now = G.Date();
    	  waitedName = Thread.currentThread().getName();
    	    while(!Lock())
    		{   if(somelock==0) { somelock = G.Date(); if(locker==null) { locker = lockerName; }}  
	            try {	// delay is milliseconds
	                Thread.sleep(100);
	            }
	            catch (InterruptedException e)
	            {
	            }
	            if((G.Date()-now)>maxWaitTime)
	            {
	            	breakLock();
	            }
    		}
    	    if(somelock!=0)
    	    {	//long now = G.Date()-somelock;
    	        //Thread who = Thread.currentThread();
    	    	//G.print(""+who+" Waited "+now+" for Thread "+locker+" for "+this);
    	    	//G.print(G.getStackTrace());
    	    }
    	}


}
