package lib;

import java.applet.AudioClip;
import java.net.URL;
import java.util.Hashtable;

@SuppressWarnings("deprecation")
public class SoundManager implements Runnable 
{	private static SoundManager theInstance = null;
	private static Thread RAThread=null;
	private  boolean exit = false;
    private int readPtr = 0;
    private  int writePtr = 0;
    static final int QUEUELENGTH = 20;
    private  URL[] sounds = new URL[QUEUELENGTH];
    private  long[] delays = new long[QUEUELENGTH];
    private  long nextDelay=0;
    private  Hashtable<String,AudioClip> soundClips = new Hashtable<String,AudioClip>();
	public synchronized void showClips(String msg)
	{	long now = G.Date();
		int rp = readPtr;
		G.print("");
		G.print(msg);
		while(rp!=writePtr)
		{
			G.print("Play "+sounds[rp]+" "+(delays[rp]-now));
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

	    private AudioClip getAClip()
	    {	URL n = extractAClip();
	    	return(GetCachedClip(n));
	    }

	    private AudioClip GetCachedClip(URL clipUrl)
	    {	if (clipUrl != null)
	        {	String name = clipUrl.toExternalForm();
	            AudioClip clip = soundClips.get(name);

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


	    public static AudioClip LoadAClip(URL name)
	    {
	        AudioClip v = null;

	        try
	        {	
	        	v = G.getAudioClip(name);
	        }
	        catch (NullPointerException ex)
	        {
	             /* shouldn't happen */
	        }
	        catch (Throwable err)
	        {
	            Http.postError(RAThread, "LoadAClip Failed for " + name, err);
	        }

	        return (v);
	    }

	    public static void loadASoundClip(String clipName, boolean doc)
	    {	
	        getInstance().GetCachedClip(G.getUrl(clipName,doc));
	    }

	    public static void loadASoundClip(String clipName)
	    {
	    	getInstance().GetCachedClip((G.getUrl(clipName, false)));
	    }
	    public synchronized static void makeNewInstance()
	    {	if(theInstance==null)
	    	{
	    	theInstance = new SoundManager();
            RAThread = new Thread(theInstance,"RA thread");
            RAThread.start();
	    	}
	    }
	    public static SoundManager getInstance()
	    {	if(theInstance==null)
	    		{
	    		makeNewInstance();
	    		}
	    	return(theInstance);
	    }

	    public void fixRAThread()
	    {
	        if (!exit)
	        {
	            if ((RAThread == null) || !RAThread.isAlive())
	            {
	                if (RAThread != null)
	                {
	                    Http.postError(RAThread, "RAThread died", null);
	                }
	                exit = true;
	                theInstance=null;
	            }
	        }
	    }

	    public void playASoundClip(String clipName, boolean doc,int delay)
	    { //System.out.println("play "+clipName);
	        fixRAThread();
	        URL u = G.getUrl(clipName,doc);
	        if(GetCachedClip(u)!=null)	// get it loaded now
	        {
	        playASoundClip(u,delay);
	        }
	    }
	    public static void playASoundClip(String clipName,int delay )
	    {
	    	getInstance().playASoundClip(clipName,false,delay);
	    }

	    public static void playASoundClip(URL clip,int delay)
	    {	
	    	getInstance().playAsoundClip(clip, delay);
	    }
	    
	    public synchronized void playAsoundClip(URL clip,int delay)
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
        RAThread = Thread.currentThread();
        G.setThreadName(RAThread,"Root");
        for (; !exit;)
        {	final AudioClip clip = getAClip();
                if (clip != null)
                {	
                	clip.play();
                }
             };
    }
       catch (Throwable err) 
        { Http.postError(this,"in round manager run()",err); 
        }
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