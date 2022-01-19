package vnc;

import com.codename1.ui.geom.Rectangle;

import lib.Image;

/**
 * interface for a window that will be available to be mirrored by vnc
 * 
 * @author ddyer
 *
 */
public interface VncScreenInterface 
{
	public boolean needsRecapture();					// true if the screen is known to have changed
	public Image captureScreen();						// get the backing bitmap from the screen, NOT a stable copy
	public void captureScreen(Image im,int timeout);	// capture a copy of the screen next time it changes
	public Rectangle getScreenBound();					// get the dimensions of the screen
	public void setTransmitter(VNCTransmitter vncTransmitter);
	public VNCTransmitter getTransmitter();
	public void stopService(String reason);
	public boolean isActive();
}