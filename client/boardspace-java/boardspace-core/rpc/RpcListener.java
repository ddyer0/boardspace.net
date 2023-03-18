package rpc;

import java.io.IOException;

import bridge.Config;
import bridge.JavaServerSocket;
import bridge.ServerSocket;
import lib.G;
import lib.Http;
import lib.InternationalStrings;
import lib.ServerSocketProxy;
import lib.SocketProxy;

/*
 * client side interface for rpc
 * 
 */
public class RpcListener implements Runnable,Config
{
    private int port;
    private boolean exitRequest = false;
    ServerSocketProxy listening;
    int sessions=0;
 
     static String BindExplanation = "Binding socket #1 failed, probably another copy of Boardspace is running";
    
    public static void putStrings()
    {
    	String strs[] = { BindExplanation };
    	InternationalStrings.put(strs);
    }
    /**
     * listen for connections on the specified port, invoke
     * the service provider for each new connection
     * 
     * @param inport
     * @param p
     */
	public RpcListener(int inport) {
		port = inport;
	}
	
 	public static ServerSocketProxy makeServerSocket(int port) throws IOException
 	{
 		return(USE_NATIVE_SOCKETS && !G.isIOS()
 				? new ServerSocket(port)
 				: new JavaServerSocket(port));
 	}
	public static boolean isServerSocketSupported() {
		return( (USE_NATIVE_SOCKETS && !G.isIOS())
				 ? ServerSocket.isServerSocketSupported()
				 : JavaServerSocket.isServerSocketSupported());
	}
	
	public static boolean isSupported() { return isServerSocketSupported(); }
	public void start()
	{	if(isSupported())
		{	
		new Thread(this,"Rpc Listener on "+port).start();
		}
	else { G.print("Server sockets are not supported here");
		}
	}
	public void stop()
	{	ServerSocketProxy l = listening;
		listening = null;
		exitRequest = true;
		RpcService.services.dead = true;
		if(l!=null) 
		{ l.closeQuietly();
		}
	}

	public void run()
	{	try {
		int tries = 0;
		while(!exitRequest && (tries++<5))
		{
		if(listening==null) 
		{ G.print("Starting server on port "+port);
		  listening = makeServerSocket(port); 
		}
		
		SocketProxy p;
			p = listening.acceptProxy();
			if(p!=null) 
			{	sessions++;
				tries=0;
				new RpcTransmitter(p,port,"Sess-"+sessions).start();
			}
		}}
		catch (IOException e) 
			{
			if(listening==null)
			{	InternationalStrings s = G.getTranslations();
				G.infoBox(e.toString(),s.get(BindExplanation,port));
				exitRequest = true;
			}

			if(!exitRequest)
				{
				e.printStackTrace();
				 G.print("Server socket closed, reopening");
				 if(listening!=null) { listening.closeQuietly(); }
				 listening = null;	// bind a new socket
				}
			
			}
		catch (Throwable err)
		{	
			stop();
			Http.postError(this, "in RpcListener",err);
		}
	}
}
