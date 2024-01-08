package bridge;

import java.applet.AudioClip;
import java.net.URL;

@SuppressWarnings("deprecation")
public class Cheerpj implements AudioClip
{
	
    public native int getWidth();
    public native int getHeight();
    public native void playSound(String sound);
    /**
     * screen width and height are used to trigger rescaling the windows
     * to remain visible when the user resizes his browser window.
     * @return
     */
    public static int getScreenWidth()
    {	
    	return getInstance().getWidth();
    }
    /**
     * screen width and height are used to trigger rescaling the windows
     * to remain visible when the user resizes his browser window.
     * @return
     */
    public static int getScreenHeight()
    {	
    	return getInstance().getHeight();
 
    }
    private Cheerpj() {}
    private static Cheerpj instance = null;
    public static Cheerpj getInstance()
    {
    	if(instance==null) { instance = new Cheerpj(); }
    	return instance;
    }
	
    String clipName = null;
    /**
     * Cheerpj plays sounds using browser's native clip mechanism.  This is a simplified
     * interface that uses a single folder /java/sound/ to hold all the sounds used by
     * any of the games.  Also, browsers don't understand the .au format that java
     * uses, so .wav files are used instead.  Fortunately we already had .wav versions
     * prepared for the codename1 branch.
     * 
     * @param clip
     */
    public Cheerpj(URL clip)
    {	String nam = clip.getFile();
    	int ind = nam.lastIndexOf('.');
    	int ind2 = nam.lastIndexOf('/');
    	if(ind>0 && ind2>0) {nam = nam.substring(ind2+1,ind); }
    	clipName = "/java/sound/"+nam+".wav";
    }
    public String toString() { return "<cheerpj "+clipName+">"; }
	public void play() {
		
		//G.print("Actual Play "+this);
		getInstance().playSound(clipName);
	}
	public void loop() {}
	public void stop() {}
}