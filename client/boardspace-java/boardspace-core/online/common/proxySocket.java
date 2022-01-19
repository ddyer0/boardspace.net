package online.common;

import java.net.*;

import lib.ConnectionManager;



public class proxySocket extends Socket 
{	ConnectionManager myNetConn=null;
	proxySocket(ConnectionManager cm)
	{	myNetConn = cm;
		status = status_wait;
		//cm.proxyWait(this);
	}

	public static final int status_wait = 0;
	public static final int status_ok = 1;
	public static final int status_failed = 2;
	public int channel = 0;
	public int status = status_wait;
}
