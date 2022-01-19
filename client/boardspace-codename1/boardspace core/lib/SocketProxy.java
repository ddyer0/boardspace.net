package lib;

import bridge.InetAddress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
/*
 * this encapsulates the things out netconns need from sockets.
 * curse codename1 for making server sockets and client sockets
 * not share an ancestor
 */
public interface SocketProxy {
	public InetAddress getLocalAddress();
	public InetAddress getInetAddress();
	public boolean isConnected();
	public InputStream getInputStream() throws IOException;
	public OutputStream getOutputStream() throws IOException;
	public void close() throws IOException;
}
