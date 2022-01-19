package bridge;

import java.lang.Exception;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Hashtable;
import java.net.ServerSocket;
/**
 * this javase version of NativeServerSocket is essentially
 * identical to the JavaSE version, which is in turn essentially
 * identical to the plain java version.
 * 
 * This implements server socket binding and an "accept" factory
 * the key to it's success is that it also implements the raw I/O
 * on the socket streams that the factory produces.
 *  
 * @author Ddyer
 *
 */
public class NativeServerSocketImpl implements bridge.NativeServerSocket
{	//
	// native interfaces are restructed from returning objects, so instead
	// it returns integers which act as handles for the underlying objects.
	//
	private Hashtable<Integer,Object>objectMap = new Hashtable<Integer,Object>();
	private Hashtable<String,Integer>reverseMap = new Hashtable<String,Integer>();
	private int nextKey = 1000;
	private Object find(int key) { return(objectMap.get(key)); }
	private void forget(int key) { objectMap.remove(key); }
	private int remember(Object something)
	{ 	int v = nextKey++;
		objectMap.put(v,something);
		return(v);
	}
	private int errorCode(String something)
	{	if(reverseMap.containsKey(something))
			{ return reverseMap.get(something);
			}
		int v = -remember(something);
		reverseMap.put(something, v); 
		return(v);
	}
	//
	// native interfaces also can't throw errors, so errors are caught
	// and their .toString() values are remembered.  For general compatibility
	// with i/o, values from -1 up are not errors.
	//
	public String getIOExceptionMessage(int handle)
	{	return ( (handle>-1) ? null : (String)(find(-handle)));
	}
	
	// service for NativeOutputStream
    public int write(int handle, int param1) {
    	try { 
        	OutputStream o = (OutputStream)find(handle);
    		o.write(param1); 
        	return(0);
   		} catch (Exception e) { return(errorCode(e.toString())); }
    }
    public int writeArray(int param, byte[] param1, int param2, int param3) 
    {	try {
		OutputStream stream = (OutputStream)find(param);
    	stream.write(param1,param2,param3);
    	return(0);
    	} catch (Exception e) 
    		{ return(errorCode(e.toString())); 
    		}
    }

    /*
     * service for NativeInputStream
     * 
     */
    public int read(int param) {
        try {
            InputStream s = (InputStream)find(param);
            int val = s.read();
        	return(val);
        }
        catch (Exception e) { return(errorCode(e.toString())); }
    }
    public int readArray(int param, byte[] param1, int param2, int param3) 
    {
    	try {
    		InputStream stream = (InputStream)find(param);
        	int v = stream.read(param1,param2,param3);
        	return(v);
        	}catch (Exception e) 
    		{ return(errorCode(e.toString()));    
    		}
    }
    public int closeInput(int param) {
    	try { 
    		InputStream stream = (InputStream)find(param);
    		if(stream!=null) { stream.close(); forget(param); }
    		return(0);
    	} catch (Exception e) { return(errorCode(e.toString())); }		
    }
    public int closeSocket(int param) {
    	try { 
    		Socket sock = (Socket)find(param);
    		if(sock!=null) { sock.close(); forget(param); }
    		return(0);
    	} catch (Exception e) { return(errorCode(e.toString())); }		
    }

    public int closeOutput(int param) {
    	try { 
    		OutputStream stream = (OutputStream)find(param);
    		if(stream!=null) { stream.close(); forget(param); }
    		return(0);
    	} catch (Exception e) { return(errorCode(e.toString())); }		
    }

    public int flush(int param) {
    	try {
    		OutputStream stream = (OutputStream)find(param);
    		if(stream!=null) { stream.flush();  }
    		return(0);
    	} catch (Exception e) { return(errorCode(e.toString())); }
    }

    /*
     * service for bind and accept
     */
    private boolean bound = false;
    private ServerSocket serverSocket = null;
    public int unBind() 
    {	try {
    	if(bound)
    	{
    		bound = false;
    		serverSocket.close();
    		serverSocket=null;
    	}
    	return(0);
    	}catch (Exception e) { return(errorCode(e.toString()));    }
    }

    public int bindSocket(int param) 
    {	bound = false;
        try {
        	serverSocket = new ServerSocket(param);
        	bound = true;
        	return(0);
        }catch (Exception e) { return(errorCode(e.toString()));    }
    }
    public int listen() {
    	if(bound)
    	{
    	try {
    		Socket listenSocket = serverSocket.accept();
    		return(listenSocket==null ? -1 : remember(listenSocket));
    	} catch (Exception e) { return(errorCode(e.toString())); }
    	}
    	else { return(errorCode("socket not bound")); }
    }

    // the listener calls listen, then
    // gets these handles and uses them to create
    // input and output streams
    public int getOutputHandle(int handle) 
    {	try {
         {	Socket listenSocket = (Socket)find(handle);
         	if(listenSocket!=null)
         	{
        	return(remember(listenSocket.getOutputStream()));
         	}
         	else { return(errorCode("invalid socket handle")); }
        }
    	}catch (Exception e) { return(errorCode(e.toString()));    }
     }

    public int getInputHandle(int handle) {
    	try {
    	Socket listenSocket = (Socket)find(handle);
    	if(listenSocket!=null) { return(remember(listenSocket.getInputStream())); }
    	else { return(errorCode("invalid socket handle")); }
    	}
    	catch (Exception e) { return(errorCode(e.toString()));    }
    }


    public boolean isSupported() {
        return true;
    }

    public int connect(String host, int port) {
    	try {
    	@SuppressWarnings("resource")
		Socket listenSocket = new Socket(host, port);
    	if(listenSocket!=null) { return(remember(listenSocket)); }
    	} catch (Exception err)	{ errorCode(err.toString()); 	}    	
    	return(-1);
    }
}
