package bridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;

import lib.G;
import lib.SocketProxy;
/**
 * this is the class that the "cheerpj" branch uses to connect using websocket.
 * in order for this to work, a bunch of separate tweaks have to be in place.
 * look for calls to of "G.isCheerpj()" for details.
 * 
 *  Briefly,
 *   cgi-bin/include.pl must include a socket assignment $cheerpj_game_server_port = 12345;
 *   login.cgi must look for the "cheerpj" parameter, and use the socket instead of the game server socket.
 *   the caller of login.cgi has to supply cheerpj=true
 *   the C server has to open the appropriate web server port, determined by its .conf file
 *   http/js/ has to include the socket and general cheerpj javascript files
 *   http/login.html has to call for all this to happen.
 *   in the previous strategy which doesn't support wss, the parent page has to be http not https
 *     this can be enforced by .htaccess or by in-page redirect
 *   in the latest strategy which does support wss, the some strategy is needed to supply
 *     the server with current private and public key certificates.  This is currently to
 *     copy them at the point of creation, using a letsencrypt post process hook.
 *   all of the above conspire for WebSocket to be used instead of regular sockets.
 */
public class WebSocket implements SocketProxy
{
  int socket = -1;
  
  public native String read(int sock); // poll this to read
  public native void send(int sock,String message);
  public native boolean isConnected(int socket);   
  public native  int connect(String host,int socket);
  public WebSocket(String host,int port)
  {	 
	  G.print("create websocket for ",host,":",port);
	  socket = connect(host,port);
	  while(!isConnected(socket)) { G.print("waiting for connection"); G.doDelay(500); }
	  G.print("connected websocket ",host,port);
  }
  
  private String pending = null;
  private int pendingIndex = 0;
  class sockInputStream extends InputStream
  {
	public int read(byte buffer[],int offset,int len) throws IOException
	{	
		// get one
		buffer[offset++] = (byte)read();
		int remaining = pending.length();
		int n=1;
		// get as much of the rest as fit
		while(pendingIndex<remaining && n<len)
			{  char ch = pending.charAt(pendingIndex++); 
			   buffer[offset++] = (byte)ch;
			   n++;
			}
		return n;
	}
  	public int read() throws IOException {
  		while(pending==null || pending.length()<=pendingIndex)
  		{
  			pending = WebSocket.this.read(socket);
  			if(pending==null) { G.doDelay(100); }
  			pendingIndex = 0;
  		}
  		char ch = pending.charAt(pendingIndex++);
  		return (byte)ch;
  	}
  }
  class sockOutputStream extends OutputStream
  {
	public void write(int b) throws IOException {
		byte bs[] = new byte[1];
		bs[0] = (byte)b;
		WebSocket.this.send(socket,new String(bs));
	}
  }
  sockInputStream inputStream = null;
  sockOutputStream outputStream = null;
  
  public InputStream getInputStream()
  {		return (inputStream = new sockInputStream());
  }	
  public OutputStream getOutputStream()
  {		
  		return (outputStream=new sockOutputStream());
  }

  
public InetAddress getLocalAddress() {
	return null;
}
public InetAddress getInetAddress() {
	return null;
}
public boolean isConnected() {
	
	return !closed;
}
public boolean closed = false;
public void close() throws IOException {
	closed = true;
	if(inputStream!=null) { inputStream.close(); inputStream = null; }
	if(outputStream!=null) { outputStream.close(); outputStream = null; }
	
}
}