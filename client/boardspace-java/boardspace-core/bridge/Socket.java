package bridge;

import java.io.IOException;
import java.net.UnknownHostException;

import lib.SocketProxy;

// this version is used if USE_NATIVE_SOCKETS is false, on windows it's a 
// trivial wrapper on regular java sockets
public class Socket extends java.net.Socket implements SocketProxy
{
	public Socket(String server, int sock) throws UnknownHostException, IOException { super(server,sock); }

}
