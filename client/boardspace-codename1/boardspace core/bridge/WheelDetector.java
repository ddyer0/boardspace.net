package bridge;

import lib.G;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
/**
 * this class acts as a system-wide detector for mouse wheel events, which are characterized
 * by very fast down/up sequences with same x and very different y.  The tricky part is allowing
 * long presses to be detected.  This is done by calling releaseLatentPress which will push
 * the delayed mouse press if it has been long enough.
 * 
 * @author ddyer
 *
 */
public class WheelDetector {
	static boolean USE_WHEEL = false;
	static boolean DETECT_WHEEL = G.isSimulator()||G.isRealWindroid();
	static long wheelStartTime = 0;
	static int wheelStartX = 0;
	static int wheelStartY = 0;
	static ActionEvent pendingAction = null;
	static MouseAdapter client = null;
	static void off() { DETECT_WHEEL = false; }
	
	static void startWith(MouseAdapter c,ActionEvent e)
	{
		client = c;
		pendingAction = e;
		wheelStartTime = G.Date();
		wheelStartX = e.getX();
		wheelStartY = e.getY();
	}
	static synchronized boolean start(MouseAdapter c,ActionEvent e)
	{	
		boolean isForSure = USE_WHEEL && Display.getInstance().isScrollWheeling();
		if(isForSure) { off(); startWith(c,e); return true; }
		if(DETECT_WHEEL)
		{
		startWith(c,e);
		return true;
		}
		return false;	
	}
	static private void sendit(ActionEvent pa)
	{	pendingAction = null;
		wheelStartTime = 0;
		client.pressedAction(pa);	
	}
	/**
	 * call this any time to possibly release a pointer press that was delayed
	 * by the test for mouse wheel activity
	 * @return true if we released a pointer move
	 */
	static synchronized public boolean releaseLatentPress()
	{	ActionEvent pa = pendingAction;
		if(pa !=null)
		{	long now = G.Date();
			long waiting = now-wheelStartTime;
			if(waiting>200)
			{	//Plog.log.addLog("release after pending for ",waiting);
				sendit(pa);
				return true;
			}
		}
		return false;
	}
	/**
	 * return the initial action if the caller should initiate a wheel event
	 * @param e
	 * @param release
	 * @return
	 */
	static public synchronized ActionEvent releaseLatentPress(ActionEvent e,boolean release)
	{
		com.codename1.ui.events.ActionEvent pa = pendingAction;
		if(pa!=null)
		{	long now = G.Date();
			if((e.getX()!=wheelStartX)					// x moved
				|| (Math.abs(e.getY()-wheelStartY)<10)	// y didn't move much
				|| ((now-wheelStartTime)>200)		// took a long time
				)
			{
			//Plog.log.addLog("release distance = ",e.getY()-wheelStartY," time = ",(now-wheelStartTime));
			sendit(pa);
			pa = null;
			}
			else 
				{ // the expected sequence is down/{drag,drag}/up
				  wheelStartTime = now; 
				  if(release) { pendingAction = null; }
				}
		}
		//Plog.log.addLog("wheel");
		return pa;
	}
}
