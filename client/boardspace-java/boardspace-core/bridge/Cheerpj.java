package bridge;

public class Cheerpj {
	
    public native int getWidth();
    public native int getHeight();
    
    public static int getScreenWidth()
    {	
    	return getInstance().getWidth();
    }
    public static int getScreenHeight()
    {	
    	return getInstance().getHeight();
 
    }
    private static Cheerpj instance = null;
    public static Cheerpj getInstance()
    {
    	if(instance==null) { instance = new Cheerpj(); }
    	return instance;
    }
}