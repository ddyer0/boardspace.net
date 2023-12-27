package bridge;

import java.applet.AudioClip;
import java.net.URL;

@SuppressWarnings("deprecation")
public class Cheerpj implements AudioClip
{
	
    public native int getWidth();
    public native int getHeight();
    public native void playSound(String sound);
    
    public static int getScreenWidth()
    {	
    	return getInstance().getWidth();
    }
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
	@Override
	public void loop() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}
}