package lib;


import online.common.commonPanel;

/**
 * this class defines the behavior we expect from the root applet.
 * <p>
 * the many constant keywords are the names expected as applet parameters.  Using these
 * names instead of random strings is recommended because it makes the users easy to locate,
 * and it helps avoid bug-by-typo.
 * <p>
 * In most cases, all the relevant information from the applet parameters
 * will have been transferred into the common sharedInfo object, so you shouldn't
 * routinely use the methods to get applet parameters except in a debugging or
 * development context.
 * @author ddyer
 *
 */
@SuppressWarnings("deprecation")
public interface RootAppletProtocol
{	// names of applet parameters

	public void init();


/** create a new LFrame window - this is used to create game windows.
 * 
 * @param name
 * @param a
 * @return a new LFrame
 */
    public LFrameProtocol NewLFrame(String name, commonPanel a);
 
    public void StartLframe();
    public void runLframe();

}