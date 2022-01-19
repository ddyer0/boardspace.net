package bridge;

/**
 * this implements server socket "accept", used for the simulator and android
 * 
 * the key to making this work is that it also implements the I/O for the
 * underlying sockets.
 * 
 * @author Ddyer
 *
 */
public interface NativeServerSocket extends com.codename1.system.NativeInterface  
{	// bind the socket
	public int bindSocket(int port);
	// unbind the socket
	public int unBind();
	// listen for a connection on the socket, waiting forever
	public int connect(String host,int port);
	public int listen();
	// get handles for the input and output streams associated
	// with the new connection.  These will be used to construct
	// NativeInputStream and NativeOutputStream
	public int getInputHandle(int handle);
	public int getOutputHandle(int handle);
	// read data stream
	public int read(int handle);
	public int readArray(int handle, byte[] array, int offset, int len);
	public int closeInput(int handle);
	public int closeSocket(int handle);
	// write data stream
	public int write(int handle, int b);
	public int writeArray(int handle,byte[]array,int offset,int len);
	public int closeOutput(int handle);
	public int flush(int handle);
	
	// check for errors
	public String getIOExceptionMessage(int handle);
}
