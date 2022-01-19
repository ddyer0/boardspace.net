package lib;

import java.io.PrintStream;

import bridge.Component;
import bridge.Container;

public interface CanvasProtocol extends DeferredEventHandler
{    /** do any pending tasks (on a time scale of fractions of a second). This will
	* normally include such tasks as redrawing the window, processing mouse gestures,
	* and handling incoming moves.  
	* Note that most realtime events just record their state and exit, leaving this process
	*  to actually do the work.
	*  Using a single process in this way avoids race conditions and deadlocks.
     * This loop automatically traps errors and reports them, which is the primary mechansism to get
     * feedback about bugs in the game implementation
     */
    public void ViewerRun(int waitTime);

    public void setBounds(int x,int y,int w,int h);
	/** set the chat applet 
	 * This service is normally completely covered by the commonCanvas class, which records
	 * the activity for later action by the viewerRun loop
 	 * */
	public void setTheChat(ChatInterface p,boolean framed);   
	 /** initialize from scratch.  H contains a lot of shared state available to the viewer, including
    the specs for the game being created and environment variables such as "s", the language translations,
    and "root" the applet root, and "frame" the frame containing menus.
    */
	public void init(ExtendedHashtable h,LFrameProtocol frame);

	   /** this is a subtrafuge to allow us to know that viewers are components that can be added to AWT containers
	    * This service is normally completely covered by the commonCanvas class, which records
	    * the activity for later action by the viewerRun loop
	    * */
	public void addSelfTo(Container c);	
   /** suicide when the window is being shut down. clean up.  Close any auxiliary
    * windows, kill auxiliary processes, etc.
   */

	/** print debugging info for the error log.  This service is normally completely 
	 * covered by the commonCanvas class
	 */
	public void printDebugInfo(PrintStream s);

   public Component add(Component c);
   public void shutDown();
   public String statsForLog();
   public void wake();
   public ExtendedHashtable getSharedInfo(); 
   public boolean setTouched(boolean v);
 }
