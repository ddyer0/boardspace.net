/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.

    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/.
 */
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
public interface UdpListener extends com.codename1.system.NativeInterface
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
