package bridge;
/**
 * this is a socket adapter so codename1 applications, particularly lib.NetConn
 * can run unaltered, rather than implementing codename1's flavor of sockets.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import lib.G;
import lib.SocketProxy;

import com.codename1.io.SocketConnection;

public class Socket extends SocketConnection  implements SocketProxy// SocketConnection adds callback when the connection succeeds or fails 
{	
	InputStream input = null;	// the live input stream
	public InputStream getInputStream()throws IOException { return(input); }

	OutputStream output = null;	// the live output stream
	public OutputStream getOutputStream() throws IOException { return(output); }
	
	int errorCode = 0;		// on failure, the code and a message 
	String message = null;
	private boolean socketPending = false;	// true while we're waiting
	String server = null;		// the server and socket we're connecting to
	int socketNumber = 0;
	
	public Socket() throws IOException { super(); }
	
	// this is the API that standard java provides
	public Socket(String s,int n) throws IOException
	{	connect(s,n);
		if((input==null) || (output==null)) 
			{ throw new IOException("Connect to "+s+":"+n+" got error "+errorCode+" - "+message);
			}
	}
	
	// do the connection and wait for the callback
	private synchronized void connect(String s,int n)
	{
		server = s;
		socketNumber = n;
		input = null;
		output = null;
		socketPending = true;
		errorCode = 0;
		com.codename1.io.Socket.connect(s,n,this);
		try {
			while (socketPending) { wait(); }
		}
		catch (InterruptedException err) {}
	}
	
	// on failure
	public synchronized void connectionError(int err, String errm) 
	{	G.print("not connected callback, err="+err+":"+errm);
		errorCode = err;
		message = errm;
		socketPending = false;
		notifyAll();
	}

	// on success
	public synchronized void connectionEstablished(InputStream is, OutputStream os) 
	{	
		input = is;
		output = os;
		socketPending = false;
		notifyAll();
	}
	// not the usual definition, but only needed in transition to getHostAddress
	public InetAddress getLocalAddress() { return(new InetAddress()); }
	public InetAddress getInetAddress() {return(new InetAddress());	}
	// probably not needed
	public void close() throws IOException
	{	
		if(input!=null) { input.close(); input = null; }
		if(output!=null) { output.close(); output=null; }
	}

}
