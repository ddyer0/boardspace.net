package bridge;

import java.io.IOException;
import java.util.Vector;

import lib.G;
import lib.Http;
import lib.SocketProxy;
import lib.ServerSocketProxy;
//
// this version is used if USE_NATIVE_SOCKETS is true
//
public class ServerSocket implements ServerSocketProxy
{
	private int port;
	private NativeServerSocket sockImpl = null;
	boolean bound = false;
	private boolean listening = false;
	String socketErrorMessage = null;		// error from the listening loop

	// this isn't nice - the static variable is global and not associated
	// with the particular request
	private Vector<SocketProxy> connections = new Vector<SocketProxy>();

	//
	// constructor, create a server socket and bind to a port
	//
	public ServerSocket(int portToListen) throws IOException
	{
		port = portToListen;
    	sockImpl = (NativeServerSocket)G.MakeNative(NativeServerSocket.class);
    	if(sockImpl==null) { G.Error("NativeServerSocket class not found"); }
    	else if(sockImpl.isSupported())
    		{ int bval = sockImpl.bindSocket(port);
    		  if(bval<0) { throw new IOException(sockImpl.getIOExceptionMessage(bval)); }
    		  bound = true;
    		}
	}

	public synchronized void addConnection(SocketProxy c)
	{	connections.addElement(c);
		G.wake(this);
	}
	public synchronized void close() throws IOException
	{ 	listening = false;
		if(bound) { sockImpl.unBind(); bound = false; }
		  G.wake(this);
		}
	public void closeQuietly() { try { close(); } catch (IOException e) {}}

	private void listen()
	{	// start the listening process if it's not already running
		if(bound && !listening) { startListenLoop(port,ServerConnection.class); listening = true; }
	}
	
	public SocketProxy accept() throws IOException 
	{	listen();
		while(listening)
			{	synchronized(this)
				{
				if(connections.size()>0) { return(connections.remove(0)); }
				if(socketErrorMessage!=null) { throw new IOException(socketErrorMessage); }
				G.waitAWhile(this, 0);
				}
			}
		return(null);
	}
	
	public SocketProxy acceptProxy() throws IOException
	{ return(accept()); 
	}

	/**
	 * Listen to incoming connections on port
	 * @param port the device port
	 * @param scClass class of callback for when the connection is established or fails, this class
	 * will be instantiated for every incoming connection and must have a public no argument constructor.
	 * @return StopListening instance that allows the the caller to stop listening on a server socket
	 */
	private boolean startListenLoop( final int port,  final Class<? extends ServerConnection> scClass) 
	{	final ServerSocket sock = this;
	    class Listener implements Runnable {
	        
	        public void run() {
	            try {
	            	while(listening) 
	            	{	
	                	final int connection = sockImpl.listen();
	            		final ServerConnection sc = (ServerConnection)scClass.newInstance();
	            		if(connection>=0) {
	                        //sc.setConnected(true);
	                    	final NativeInputStream insock = new NativeInputStream(sockImpl,connection);
	                    	final NativeOutputStream outsock = new NativeOutputStream(sockImpl,connection);
	                                sc.connectionEstablished(
	                                		sock,insock,outsock);
	                    } else {
	                    	if(listening)
	                    	{
	                    	listening = false;
	                    	bound = false;
	                    	int bval = sockImpl.unBind();
	                    	if(bval<0)
	                    	{
	                    	String err = socketErrorMessage = sockImpl.getIOExceptionMessage(bval);
	                        sc.connectionError(bval, err);
	                    	}
	                    	}
	                        G.wake(this);
	                    }
	                }
	            	bound = false;
	                sockImpl.unBind();
	            }
	            catch(Throwable err) {
	               Http.postError(this, "server listen", err);
	            }
	            G.wake(this);
	        }

	    }
	    Listener l = new Listener();
	    new Thread(l, "Listening on " + port).start();
	    return true;
	}
	public static boolean isServerSocketSupported() { return(!G.isIOS()); }

}
