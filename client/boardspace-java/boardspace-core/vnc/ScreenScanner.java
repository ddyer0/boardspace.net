package vnc;

import lib.Image;
import java.awt.Rectangle;

import lib.G;
import lib.Http;

/**
 *
 * @author heic
 */
public class ScreenScanner implements Runnable {
    
    private Image myCopy;
    private int myWidth;
    private int myHeight;
    private VncScreenInterface kvmman;
    private TileManager tileman;
    private VNCTransmitter transmitter;
    private Thread runner;
    boolean exitRequest = false;
    boolean running = false;
    boolean started = false;
    Object semaphore = new Object();
    int errors = 0;
    boolean dirty = false;
    
    /** Creates a new instance of ScreenScanner */
    public ScreenScanner(VncScreenInterface kvmman_, TileManager tileman_,VNCTransmitter mas) {
        kvmman = kvmman_;
        tileman = tileman_;
        transmitter = mas;
    }
    private void reinitialize()
    {	Rectangle dims = kvmman.getScreenBound();    	
    	int w = G.Width(dims);
    	int h = G.Height(dims);
    	if((w!=myWidth)||(h!=myHeight))
    	{
    	myHeight = h;
    	myWidth = w;
    	myCopy = Image.createTransparentImage(w,h);
    	tileman.setSize(w,h);	
    	}
    }

    public void waitForStarted() { while(!started) { G.waitAWhile(this, 0); }}
    public void setStarted() { started = true; G.wake(this); }
    public synchronized boolean isDirty() { return(dirty); }
    public synchronized void setDirty(boolean v) { dirty=v; }
    
    public void run() {
        int waitTime = -1;
        running = true;
        exitRequest = false;
        setStarted();
        while(!exitRequest) {
            try {
            	reinitialize();
                kvmman.captureScreen(myCopy,waitTime);
                //G.addLog("Screen scanner wake");
                if(tileman.processImage(myCopy) || (waitTime<0))
                	{ synchronized(transmitter)
                		{
                		setDirty(true);
                		transmitter.inputWake(true);
                		}
                	}
                	else 
                	{ transmitter.inputWake(false);	// no new screen, but wake it anyway so it can look around 
                	}
                waitTime = 0;

                }
            catch (Throwable e) {
            	errors++;
                Http.postError(this,"in screen scanner",e);
                if(errors>10) { exitRequest = true; }
            }
        }
        G.wake(kvmman);
        G.wake(transmitter);
        running = false;
    }
      
    public void startScreenScanning()
    {
        if (runner == null) {
            runner = new Thread(this,"Bitmap scanner");
            runner.start();
        }
    }
    String stopReason = null;
    public void stopScreenScanning(String reason)
    {
    	exitRequest = true;
    	stopReason = reason;
    	G.wake(kvmman);
    }

 
}