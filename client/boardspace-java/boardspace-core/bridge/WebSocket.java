package bridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;

import lib.G;
import lib.SocketProxy;

public class WebSocket implements SocketProxy
{
  int socket = -1;
  
  public native String read(int sock); // poll this to read
  public native void send(int sock,String message);
  public native boolean isConnected(int socket);   
  public native  int connect(String host,int socket);

  public WebSocket(String host,int port)
  {	  G.print("create websocket",host,port);
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