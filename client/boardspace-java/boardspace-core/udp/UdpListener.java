package udp;
/**
 * this implements a simple broadcast UDP sender/listener service.
 * It's intended only for the limited purpose of peers finding one
 * another on a local network, not for more aggressive packet traffic
 * 
 * There are implementations for javase, android, and ios
 * 
 * @author Ddyer
 *
 */
public interface UdpListener
{	/**
	 * Get a UDP message, wait a maxumum amount of time in milliseconds
	 * -1 = don't wait, 0 = wait forever
	 * return null if the timeout expires or possibly for other reasons
	 * @param wait
	 * @return a string or null
	 */
	public String getMessage(int wait);
	/**
	 * run the receiver process in a separate thread.  This thread loads
	 * a queue which is emptied by getMessage.  If filter is true, filter
	 * out messages we send, so they are not received here.
	 * 
	 * @param broadcastPort
	 * @param filter
	 */
    public void runBroadcastReceiver(int broadcastPort,boolean filter);
    /**
     * send a short message on the specified udp port, if filter 
     * @param msg
     * @param broadcastPort
     */
    public boolean sendMessage(String msg,int broadcastPort);
    /**
     * stop the receiver process
     */
    public void stop();
    /**
     * yes, we support  this service.
     * @return true if this device supports udp
     */
    public boolean isSupported();
 }
