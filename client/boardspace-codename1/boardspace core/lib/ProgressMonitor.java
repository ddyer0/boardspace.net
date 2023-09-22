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
 * thread progress monitor.  Generates errors if designated threads stop making
 * progress.  The theory here is that we can catch thread lockups.
 */
import java.lang.Thread;

import java.util.Vector;

class ThreadData
{	boolean active = true;
	Thread thread;
	long lastActive = 0;
	private int progressCount = 0;
	private int lastProgress = 0;
	ThreadData(Thread t) { thread = t; }
	synchronized void noteProgress(long now)
	{
		lastActive = now;
		progressCount++;
	}
	synchronized void updateProgress()
	{
		lastProgress = progressCount;
	}
	synchronized boolean checkNoProgress(long now)
	{
		return( active 
				&& (lastProgress==progressCount)
				&& (lastActive<now));
		
	}
}

public class ProgressMonitor extends Thread {
	public int progressInterval = 60000;	// 60 seconds
	public int monitorInterval = 5000;		// 5 seconds
	public boolean exit = false;
	private static Vector<ThreadData> monitored = new Vector<ThreadData>();
	private static ProgressMonitor theInstance = null;
	
	public ProgressMonitor()
	{
		super("ProgressMonitor");
	}
	
	
	private static long doomsDay = 0;
	// this artifially gives the montitor something to detect
	public static void startStall(final Object ob,int when)
	{	long now = G.Date();
		if(0==doomsDay) { doomsDay = now+when; }
		else if(doomsDay<0) {}
		else if(now>doomsDay)
		{
		doomsDay = -1;
		new Thread(new Runnable()
			{ public void run()
				{ synchronized(ob) 
					{
					String other = "not me";
					while(true) { try {
						synchronized(other) { other.wait(100); }
					} catch (InterruptedException e) {
						e.printStackTrace();
					} }
					}
				}
			},"doomsday thread").start();
		}
	}
	
	/** call this periodically to notice progress in a thread that should be making it.
	 * 
	 */
	public synchronized static void noteProgress()
	{
		Thread t = Thread.currentThread();
		long now = G.Date();
		ThreadData found = null;
		for(int n = 0;n<monitored.size() && (found==null); n++)
		{
			ThreadData b = monitored.elementAt(n);
			if(b.thread==t) { found = b; }
		}
		boolean newfound = false;
		if(found==null) { found = new ThreadData(t); newfound = true; }
		found.noteProgress(now);
		if (newfound) { monitored.addElement(found);}
		if(theInstance==null) { theInstance = new ProgressMonitor(); theInstance.start(); }
	}

	
	/**
	 *  stop monitoring this process
	 */
	public synchronized static void stopProgress()
	{
		Thread t = Thread.currentThread();
		ThreadData found = null;
		for(int n = 0;n<monitored.size() && (found==null); n++)
		{
			ThreadData b = monitored.elementAt(n);
			if(b.thread==t) { found = b; }
		}
		if(found!=null) { found.active = false; monitored.remove(found); }
		if((monitored.size()==0)
				&& (found!=null)
				&& (theInstance!=null))
			{ theInstance.exit = true; 
			}
	}
	
	public void run()
	{	G.setThreadName(Thread.currentThread(),"Progress Monitor");
		while(!exit)
		{
			try { 
			// in codename1 the edt thread is special and needs to be monitored
			if(G.isCodename1()) {G.startInEdt(new Runnable() { public void run() { noteProgress(); }});}
			sleep(monitorInterval);
			exit = !checkForProgress();
			}
			catch (InterruptedException err)
			{
				
			}
		}
		String msg = report.toString();
		Http.postError(this, msg, null);
	}
	
	StringBuilder report = new StringBuilder();

	private void report(String msg,ThreadData t)
	{
		report.append(msg+" "+t+" "+t.thread);
		report.append("\n");
	}
	private void dumpThreads(String msg,ThreadData ex)
	{
	}
/*
	private void report(String msg,ThreadData t)
	{
		report.append(msg+" "+t+" "+t.thread.getState());
		report.append("\n");
		report("for "+t.thread.getName(),t.thread.getId(),false);
	}
	
	public void dumpThreads(String msg,ThreadData ex)
	{	ThreadMXBean bean = ManagementFactory.getThreadMXBean();
		ThreadInfo info[] = bean.dumpAllThreads(true,true);
		long except = ex.thread.getId();
		for(int i=0;i<info.length;i++)
		{
			ThreadInfo ii = info[i];
			if(ii.getThreadId()!=except)
			{
				dumpThreadInfo(msg,ii,false);
			}
		}
	}
	*/

/**
	private void report(String msg,long id,boolean recurse)
	{
		ThreadMXBean bean = ManagementFactory.getThreadMXBean();
		ThreadInfo infoArr[] = bean.getThreadInfo(new long[]{id},true,true);
		ThreadInfo info = infoArr[0];
		if(info!=null)
		{	dumpThreadInfo(msg,info,recurse);
		}
	}
	
	private void dumpThreadInfo(String msg,ThreadInfo info,boolean recurse)
		{
		LockInfo lock = info.getLockInfo();
		
		if(lock!=null) 
			{ long lockid = info.getLockOwnerId();
			  if(lockid>0)
				  {String name = info.getLockOwnerName();
				  report.append("Lock "+lock+" owned by "+name);
				  report.append("\n");
				  if(!recurse) { report("locker "+name,lockid,true); }
				  }
			}
		report.append(msg);
		report.append(" Stack trace for ");
		report.append(info.getThreadName());
		report.append("\n");
		
		StackTraceElement trace[] = info.getStackTrace();
		for(StackTraceElement e : trace)
		{
			report.append("E ");
			report.append(e);
			report.append("\n");
		}
		report.append("\n");
		}
	}
	*/
	
	private ThreadData getElementIfExists(int n)
	{	ThreadData t = null;
		synchronized (monitored)
		{
			if(n<monitored.size()) { t = monitored.elementAt(n);}
		}
		return(t);
	}
	
	// check that all the monitored threads are making progress
	private boolean checkForProgress()
	{	long now = G.Date()-progressInterval;
		ThreadData stalled = null;
		for(int n=monitored.size()-1;n>=0;n--)
		{
			ThreadData t = getElementIfExists(n);
			if(t!=null)
			{
			if(t.checkNoProgress(now))
			{	
				stalled = t;
			    report("Stalled thread",t);
			}
			else 
			{
			t.updateProgress();
			}}
		}
		if(stalled!=null)
		{
			if(G.isCodename1())
			{
			for(int n=monitored.size()-1;n>=0;n--)
			{
			ThreadData t = getElementIfExists(n);
			if(t!=null && t!=stalled && t.active)
			{
				report("Not stalled",t);
			}}}
			else 
			{
				dumpThreads("Not stalled",stalled);
			}
		}
		
		return(null==stalled);
	}
}
