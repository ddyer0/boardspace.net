package bridge;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lib.G;

import com.codename1.media.Media;
import com.codename1.media.MediaManager;

public class AudioClip {
	String name;
	byte[] data;
	File clipFile = null;
	static boolean useFiles = false;
	
	String format = "audio/wav"; //"audio/wave";
	public String toString() { return(G.concat("<clip ",name," ",data==null?0:data.length,">")); }
	File tempFile = null;
	public AudioClip(URL url)
	{
		name = url.getFile();
		int idx = name.lastIndexOf('/');
		if(idx>=0) { name = name.substring(idx+1); }
		clipFile = new File(clipDirectory()+name);
		InputStream s = null;	
		OutputStream o = null;
		byte buf[] = new byte[1024];
		int len = 0;
		try {
			s = url.openStream();
			o = new FileOutputStream(clipFile);
			if(s!=null)
			{
			do {
				len = s.read(buf,0,1024);
				if(len>0) { o.write(buf,0,len); }
			} while(len>0);
			}
		}
		catch (IOException e) { G.print("IO exception "+e); }
		try { 
			if(o!=null) { o.close(); }
			if(s!=null) { s.close(); }
		}
		catch (IOException e) {};
		useFiles = true;
	}
	public AudioClip(String n,InputStream d)
	{	name = n;
		try {
			// get a copy of the data
			int len = d.available();
			byte bytes[] = new byte[len];
			int got = d.read(bytes,0,len);
			if(got==len) {	data = bytes; useFiles = false; }
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String clipDirectory()
		{	
		String base = G.documentBaseDir();
		String dir = base+"soundclips/";
		new File(dir).mkdir();
		return(dir);
	}
	boolean completed = false;
	Media media = null;
	InputStream stream = null;
	double starttime = 0;
	AudioClip clip = this;
	Runnable complete = new Runnable() 
	{ public void run() 
		{ completed = true; media = null; 
		  long later = G.Date();
		  G.print("clip "+clip+" took "+(later-starttime));
		  try { InputStream s = stream;
		  	    if(s!=null) { s.close(); }
		  	    }
		  catch (IOException e) {}
		}};
	
	Runnable play = new Runnable() 
		{ public void run() { 
			try {	
			final InputStream instream = data==null 
					? new FileInputStream(clipFile)
					: new ByteArrayInputStream(data);
			stream = instream;

			completed = false;	
			media = MediaManager.createMedia(instream, format ,complete);
                if(media!=null) 
				{ starttime = G.Date();
				  media.play() ; 
                	}                
                }
			catch (Throwable ex) 
				{ 
			G.print("Trouble playing "+clip+" : "+ex);
				}
		}};
		
	public void play() {
		if(useFiles ? clipFile!=null : data!=null)
		{
			// this shouldn't require edt, but current superstition
        	// is that it does.  without this IOS hangs when sounds
        	// are used heavily, as when doing timed replays.
        	// 1/15/2020
		  if(G.isIOS()) { G.startInEdt(play); }
				else { play.run(); }
	}}
}
