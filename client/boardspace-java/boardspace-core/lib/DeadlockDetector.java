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


import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

  /**
 * @author ATracer
   */
public class DeadlockDetector implements Runnable
{

  private int          checkInterval  = 0;
  private static String    INDENT      = "    ";
  private StringBuilder    sb        = null;
  private static boolean exit = false;
  
  public DeadlockDetector(int checkInterval)
  {
    this.checkInterval = checkInterval * 1000;
  }
  private static DeadlockDetector instance = null;
  private static Thread detectorThread = null;
  public static DeadlockDetector getInstance() 
  {	if(instance==null)
  		{ instance = new DeadlockDetector(1);
  		}
  	return(instance);
      }
  public static Thread startDeadlockDetector()
  {
	  if(detectorThread==null) 
	  	{ detectorThread = new Thread(getInstance(),"deadlock detector");
	  	  detectorThread.start();
	  	}
	  return(detectorThread);
  }
  public static void stopInstance()
  {
	  exit = true;
  }
  @Override
  public void run()
  { 
    boolean noDeadLocks = true;

    while (noDeadLocks && !exit)
    { //System.out.println("ok "+steps++);
      noDeadLocks = checkOnce();
      try { if(noDeadLocks) { Thread.sleep(checkInterval); } }
      catch(InterruptedException e) {};
      }
    }
  
  public boolean checkOnce()
  {	boolean noDeadLocks  = true;
  	ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    long[] deadlockThreadIds = bean.findDeadlockedThreads();
    long[] monitorThreadIds = bean.findMonitorDeadlockedThreads();

    if(deadlockThreadIds!=null ) { noDeadLocks=false; report("deadlock",deadlockThreadIds); } 
    if(monitorThreadIds!=null) { noDeadLocks = false; report("stall",monitorThreadIds); }
    return(noDeadLocks);
  }
  public void report(String msg)
  {	ThreadMXBean bean = ManagementFactory.getThreadMXBean();
  	report(msg,bean.getAllThreadIds());
  }
  public void report(String msg,long threadIds[])
  {
	  System.out.println("Deadlock detected!");
	  sb = new StringBuilder();
	  ThreadMXBean bean = ManagementFactory.getThreadMXBean();
	  ThreadInfo[] infos = bean.getThreadInfo(threadIds);
	  sb.append("\nTHREAD LOCK INFO: \n");
	  for (ThreadInfo threadInfo : infos)
          {
            printThreadInfo(threadInfo);
            LockInfo[] lockInfos = threadInfo.getLockedSynchronizers();
            MonitorInfo[] monitorInfos = threadInfo.getLockedMonitors();

            printLockInfo(lockInfos);
            printMonitorInfo(threadInfo, monitorInfos);
          }

          sb.append("\nTHREAD DUMPS: \n");
          for (ThreadInfo ti : bean.dumpAllThreads(true, true))
          {
            printThreadInfo(ti);
          }
          System.out.println(sb.toString());
  
  }

  private void printThreadInfo(ThreadInfo threadInfo)
  {
    printThread(threadInfo);
    sb.append(INDENT + threadInfo.toString() + "\n");
    StackTraceElement[] stacktrace = threadInfo.getStackTrace();
    MonitorInfo[] monitors = threadInfo.getLockedMonitors();

    for (int i = 0; i < stacktrace.length; i++)
    {
      StackTraceElement ste = stacktrace[i];
      sb.append(INDENT + "at " + ste.toString() + "\n");
      for (MonitorInfo mi : monitors)
      {
        if (mi.getLockedStackDepth() == i)
        {
          sb.append(INDENT + "  - locked " + mi + "\n");
        }
      }
    }
  }

  private void printThread(ThreadInfo ti)
  {
    sb.append("\nPrintThread\n");
    sb.append("\"" + ti.getThreadName() + "\"" + " Id=" + ti.getThreadId() + " in " + ti.getThreadState() + "\n");
    if (ti.getLockName() != null)
    {
      sb.append(" on lock=" + ti.getLockName() + "\n");
    }
    if (ti.isSuspended())
    {
      sb.append(" (suspended)" + "\n");
    }
    if (ti.isInNative())
    {
      sb.append(" (running in native)" + "\n");
    }
    if (ti.getLockOwnerName() != null)
    {
      sb.append(INDENT + " owned by " + ti.getLockOwnerName() + " Id=" + ti.getLockOwnerId() + "\n");
    }
  }

  private void printMonitorInfo(ThreadInfo threadInfo, MonitorInfo[] monitorInfos)
  {
    sb.append(INDENT + "Locked monitors: count = " + monitorInfos.length + "\n");
    for (MonitorInfo monitorInfo : monitorInfos)
    {
      sb.append(INDENT + "  - " + monitorInfo + " locked at " + "\n");
      sb.append(INDENT + "      " + monitorInfo.getLockedStackDepth() + " " + monitorInfo.getLockedStackFrame() + "\n");
    }
  }

  private void printLockInfo(LockInfo[] lockInfos)
  {
    sb.append(INDENT + "Locked synchronizers: count = " + lockInfos.length + "\n");
    for (LockInfo lockInfo : lockInfos)
    {
      sb.append(INDENT + "  - " + lockInfo + "\n");
    }
  }
  
	public static long doomsDay = 0;
	public static void finishStall(final Object ob2,final Object ob1)
	{
		synchronized(ob2) { synchronized(ob1) { try {
			ob1.wait(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} }}
	}
	public static void startStall(final Object ob1,final Object ob2,int when)
	{	long now = G.Date();
		if(0==doomsDay) { doomsDay = now+when; }
		else if(doomsDay<0) {}
		else if(now>doomsDay)
		{
		doomsDay = -1;
		new Thread(new Runnable()
			{ public void run()
				{ 	while(true)
					{ synchronized(ob1) 
						{synchronized (ob2) 
							{ try {
								ob2.wait(100);
							} catch (InterruptedException e) {
								e.printStackTrace();
							} 
						}
						}
					}
				}
			},"doomsday thread").start();
		}
		finishStall(ob1,ob2);
	}
}

   
  