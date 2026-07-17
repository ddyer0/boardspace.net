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

import bridge.URL;
import bridge.BSClip;
import java.util.Hashtable;

public class SoundManager implements Runnable 
{	private static SoundManager theInstance = null;
	private  boolean exit = false;
    private  int readPtr = 0;
    private  int writePtr = 0;
    static final int QUEUELENGTH = 20;
    private  URL[] sounds = new URL[QUEUELENGTH];
    private  long[] delays = new long[QUEUELENGTH];
    private  long nextDelay=0;
    public  Hashtable<String,BSClip> soundClips = new Hashtable<String,BSClip>();
	public synchronized void showClips(String msg)
	{	
		int rp = readPtr;
		while(rp!=writePtr)
		{
			rp++;
			if (rp == QUEUELENGTH)
            {
                rp = 0;
            }
		}
	}
    private URL extractAClip()
	    {
	    	long now = G.Date();
	    	
	    	try
	        {
	            
	            while (!exit && ((readPtr == writePtr) || (now<delays[readPtr])))
	            {	if(readPtr==writePtr) 
	            		{ 
	            		  synchronized(this) { wait(); }
	            		}
	            		else 
	            		{ synchronized (this) { wait(Math.max(1, delays[readPtr]-now)); }};
	            	now = G.Date();
	            }
	        }
	        catch (InterruptedException err)
	        { // pro forma catch
	        }
	        now = G.Date();
	        if ((readPtr != writePtr) && now>=delays[readPtr])
	        {	//showClips("Remove");
	            URL clipName = sounds[readPtr];
	            readPtr++;
	            if (readPtr == QUEUELENGTH)
	            {
	                readPtr = 0;
	            }
	            return (clipName);
	        }

	        return (null);
	    }

	    private BSClip getAClip()
	    {	URL n = extractAClip();
	    	BSClip cc = GetCachedClip(n);
	    	//G.print("fetch ",n,cc);
	    	return(cc);
	    }

	    private BSClip GetCachedClip(URL clipUrl)
	    {	if (clipUrl != null)
	        {	String name = clipUrl.toExternalForm();
	            BSClip clip = soundClips.get(name);

	            if (clip == null)
	            {	
	                clip = LoadAClip(clipUrl);

	                if (clip != null)
	                {
	                    soundClips.put(name, clip);
	                }
	            }
	            return (clip);
	        }
	        return (null);
	    }


	    public static BSClip LoadAClip(URL name)
	    {
	        BSClip v = BSClip.getAudioClip(name);

	        return (v);
	    }

	    public static BSClip loadASoundClip(String clipName, boolean doc)
	    {	
	       return  getInstance().GetCachedClip(G.getUrl(clipName,doc));
	    }

	    public static BSClip loadASoundClip(String clipName)
	    {
	    	return getInstance().GetCachedClip((G.getUrl(clipName, false)));
	    }
	    public synchronized static void makeNewInstance()
	    {	if(theInstance==null)
	    	{
	    	theInstance = new SoundManager();
            Thread rr = new Thread(theInstance,"RA thread");
            rr.start();
	    	}
	    }
	    public static SoundManager getInstance()
	    {	if(theInstance==null)
	    		{
	    		makeNewInstance();
	    		}
	    	return(theInstance);
	    }

	    public void playASoundClip(String clipName, boolean doc,int delay)
	    { //System.out.println("play "+clipName);
	        if(!exit)
	        {
	        URL u = G.getUrl(clipName,doc);
	        if(GetCachedClip(u)!=null)	// get it loaded now
	        {
	        //G.print("load ",u);
	        playASoundClip(u,delay);
	        }}
	    }
	    public static void playASoundClip(String clipName,int delay )
	    {	
	    	getInstance().playASoundClip(clipName,false,delay);
	    }

	    
	    public synchronized void playASoundClip(URL clip,int delay)
	    {	long now = G.Date();
	        sounds[writePtr] = clip;
	        delays[writePtr] = nextDelay;
	        nextDelay=Math.max(Math.min(now+5000,nextDelay),now)+delay;
	        int nextwp = writePtr;
	        nextwp++;
	        if(nextwp==QUEUELENGTH) { nextwp = 0; }
	        if(nextwp==readPtr)
	        	{ System.out.println("sound queue full playing "+clip); 
	        	}
	        else { writePtr = nextwp; }

	        notify(); 
	    }

	    public static void playASoundClip(String clipName)
	    {
	        getInstance().playASoundClip(clipName, false,0);
	    }
	    public static void playASoundClip(String name,boolean doc)
	    {
	    	getInstance().playASoundClip(name, doc, 0);
	    }
	    public static boolean soundIdle()
	    {
	    	return(getInstance().soundIdleNow()); 
	    }
	    private boolean soundIdleNow()
	    {	return(readPtr==writePtr);
	    }
	    
    public void run()
    {	
        try {
        //System.out.println("run");
        G.setThreadName(Thread.currentThread(),"Sound");
        for (; !exit;)
        {	final BSClip clip = getAClip();
        	if (clip != null)
                {	
                	clip.playAudioClip();
                }
             };
        }
        catch (Throwable err)
        { Http.postError(this,"in round manager run()",err); 
        }
        finally { theInstance=null; }
    }

    public static void stop()
    {	getInstance().stopNow();
    }
    private void stopNow()
    {	exit = true;
    	synchronized (this) { notify(); }
    }
	static public void preloadSounds(String... sounds)
	{	for(String str : sounds) { loadASoundClip(str); }
	}
}
