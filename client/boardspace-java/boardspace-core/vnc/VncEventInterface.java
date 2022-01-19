package vnc;

public interface VncEventInterface
{
	public void keyStroke(int keycode);					// send a key down to the screen
	public void keyRelease(int keycode);				// send a key up to the screen
	public void mouseMove(int x, int y);				// send the mouse position to the screen
	public void mouseDrag(int x, int y,int buttons);				// send the mouse position to the screen
	public void mousePress(int x,int y,int buttons);				// send mouse buttons down
	public void mouseRelease(int x,int y,int buttons);				// send mouse buttons up
	public void mouseStroke(int x,int y,int buttons) ;				// send mouse click
	public void setTransmitter(VNCTransmitter vncTransmitter);
	public void stopService(String reason);
	public void notifyActive();
	public void notifyFinished();
}