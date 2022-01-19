package bridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;

import lib.Plog;
import lib.SocketProxy;
/**
 * this is the callback class for ServerSockets.  It has to be a completely independent
 * class so the simple newInstance() interface will work. 
 * @author Ddyer
 *
 */
public class ServerConnection  implements SocketProxy
{	public ServerConnection () {  }
	private InputStream inputStream;
	private OutputStream outputStream;
	public InputStream getInputStream() { return(inputStream); }
	public OutputStream getOutputStream() { return(outputStream); }
	public void close() throws IOException
	{ InputStream i = inputStream;
	  OutputStream o = outputStream;
	  if(i!=null) { i.close(); inputStream=null; }
	  if(o!=null) { o.close(); outputStream = null; }
	}
	public ServerConnection(InputStream is,OutputStream os)
	{
		inputStream = is;
		outputStream = os;
	}
	public void connectionEstablished(ServerSocket sock,InputStream is, OutputStream os) 
	{	Plog.log.addLog("ServerConnection established ",is," ",os);
		inputStream = is;
		outputStream = os;
		sock.addConnection(this);
	}
	public void connectionError(int errorCode, String message) {
		Plog.log.addLog("ServerConnection error: ",errorCode," ",message);
	}
	public boolean isConnected()
	{
		return((inputStream!=null) && (outputStream!=null));
	}
	public InetAddress getLocalAddress() {
		return(null);
	}
	public InetAddress getInetAddress() {
		return(null);
	}
}
