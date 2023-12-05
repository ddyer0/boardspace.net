package bridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lib.G;
import lib.SocketProxy;

/**
 * this is a dummy class that shouldn't be instantiated by the codename1 branch.
 * It exists only to make the main code appear to be uniform
 */
public class WebSocket implements SocketProxy
{

  public WebSocket(String host,int port)
  {	  throw G.Error("shouldn't be called");
  }
  
  public InputStream getInputStream()
  {		throw G.Error("shouldn't be called");
  }	
  public OutputStream getOutputStream()
  {		
	  throw G.Error("shouldn't be called");
  }

public InetAddress getLocalAddress() {
	return null;
}
public InetAddress getInetAddress() {
	return null;
}
public boolean isConnected() { return false; }

public void close() throws IOException { }

}