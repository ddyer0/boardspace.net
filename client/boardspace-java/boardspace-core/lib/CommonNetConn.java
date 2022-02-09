package lib;
import java.net.InetAddress;
import bridge.Socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import bridge.ClientSocket;
import bridge.Config;
import bridge.Utf8OutputStream;
/**
 * this contains network code common to the legacy strings network,
 * and the mixed string/binary packet used by the vnc network
 * There are a small number of abstract methods that have to be
 * implemented by all (ie; both) types of actual network connections.
 * All the methods here are final only to prevent accidents in development.
 * 
 *  It runs a reader and writer process, driven by queues, and
 *  provides simple getInput and sendMessage interfaces.  
 *  
 * @author Ddyer
 *
 * @param <TYPE>
 */
public abstract class CommonNetConn<TYPE> implements Runnable, Config
{
    public static final int MAX_READ_ERRORS = 10;		// limit on the number of reports we send.
    final int DefaultQueueLength = 1024;
    int BufferLength = 4096;		// this number has to be smaller than BUFFSIZE acceptable to the server

    /**
     * set the buffer size, which may be a limitation on the
     * amount of data in a packet.  There's no reason to low ball
     * this number, but infinitely large packets are probably
     * evidence of a runaway.
     *  
     * @param n
     */
    public abstract void setBufSize(int n);
    /**
     * actually output a packet to the output stream
     * 
     * @param os
     * @param theBuffStr
     * @return
     */
    abstract boolean realSendMessage(OutputStream os,TYPE theBuffStr);
    /**
     * input a packet from the input stream, and stuff it into the
     * read queue.
     * 
     * @param s
     */
    abstract void doReadStep(InputStream s);

    ConnectionManager myManager=null;

    PacketQueue<TYPE> inQueue = null;
    PacketQueue<TYPE> outQueue = null;
    String myName = "(netConn)";
    boolean waiting = false;
    Thread reader = null;
    Thread writer = null;
 	Random R = new Random();
	int delays = 0;				// for debugging
    public boolean hadConn = false;			// had a connection at some time
    public final void setInputSemaphore(Object to) { inQueue.inputSemaphore = to; }
    public final boolean healthCheck() { return(inQueue.healthCheck() && outQueue.healthCheck()); }
    public final String stateSummary()
    {
    	return(inQueue.stateSummary("")+" "+outQueue.stateSummary(""));
    }
    public int getStats()
    {
    	return(inQueue.getStats());
    }

    final String ipString(int a,int b,int c,int d,int u)
    {
    	return(""+a+"."+b+"."+c+"."+d+"."+u);
    }
    
	boolean discardInput = false;
	boolean discardOutput =false;
	public final void setDiscardInput(boolean v) { discardInput = v; }
	public final void setDiscardOutput(boolean v) { discardOutput = v; }

    boolean started = false;	// true if we're theoretically in operation
    String errstring = null;	// not null when the streams have gone bad
    boolean exitFlag = false; 	// if true, we want out.  reader and writer clean up and exit
    boolean eofok = false; 		// if true, we're expecting to be cut off (streams closing)

    String machineName;
    int machinePort;
    SocketProxy mySocket = null; //open sockets and streams
    InputStream myInStream = null;
    OutputStream myOutStream = null;

    //stats
    
    public int count_writes = 0;
    public int sum_writes = 0;
    public long last_write = 0;
    public int count_reads = 0;
    public int read_errors = 0;							// count of formatting errors in data received
    public int sum_reads = 0;
    public long last_read = 0;

    public CommonNetConn(ConnectionManager man,String me)
    {
    	myManager = man;
    	myName = me;
    	inQueue = new PacketQueue<TYPE>(me + " reader ", DefaultQueueLength*2);
    	//inQueue.logEvents = true;
    	outQueue = new PacketQueue<TYPE>(me + " writer ",DefaultQueueLength);
    }
    public final synchronized void waitForOutputStream()
    {
 	   try
 	   {	while(!exitFlag && (myOutStream==null)) 
 	   		{ wait(); 
 	   		}
 	   }
 	   catch (InterruptedException e)
 	   {
 	   }
    }
    public final synchronized void waitForInputStream()
    {
 	   try
 	   {	while(!exitFlag && (myInStream==null)) { wait(); }
 	   }
 	   catch (InterruptedException e)
 	   {
 	   }
    }
    public final synchronized String getLocalAddress()
    {
        if (mySocket!=null)
        {	InetAddress rem = mySocket.getLocalAddress();
            if(rem!=null) { return (rem.getHostAddress()); }
        }
        return(G.getLocalIpAddress()); 
    }
    public final synchronized String getRemoteAddress()
    {
        if (mySocket!=null)
        {	InetAddress rem = mySocket.getInetAddress();
            if(rem!=null) { return (rem.getHostAddress()); }
        }
        return(G.getLocalIpAddress()); 
    }
   

    public final void start()
    { //System.out.println("Start "+this + reader + writer);
        reader = null;
        writer = null;
        started = false;
        hadConn = false;
        errstring = null;
        exitFlag = false;
        discardInput = false;
        discardOutput = false;
        eofok = false;
        setBufSize(BufferLength);
        reader = new Thread(this,"reader");
        writer = new Thread(this,"writer");
        inQueue.reset();
        outQueue.reset();
        writer.start();
        reader.start();
        reader.setPriority(reader.getPriority()+1);
        writer.setPriority(writer.getPriority()+2);
        started = true;
    }

    public final synchronized boolean setRequestConn()
    {	//System.out.println("Reader start");
        makeConn(); 
        if(getErrString()==null) { start(); return(true); }
        return(false);
    }

    public final synchronized void setExitFlag()
    {
        exitFlag = true;
        eofok = true;
        discardOutput = false;
        discardInput = false;
        outQueue.dead = true;
        inQueue.dead = true;
        
        synchronized (outQueue) { outQueue.notifyAll(); }
        synchronized (inQueue) { inQueue.notifyAll(); }
        closeConn();
    }
    


    /** make a connection, return true if we were successful */
    public final boolean makeConn()
    {
        int[] ports = { machinePort };
        SocketProxy sock = getSocketConnection(machineName, ports);

        //System.out.println("Sock: "+sock);
        if (sock == null)
        {	String msg = "Can't get socket "+machineName+":"+machinePort;
        	Plog.log.addLog(msg);
        	setExitFlag(msg);
            return (false);
        }
        return connectToSocket(sock);
    }
    
    public final boolean connectToSocket(SocketProxy sock)
    {
        setMySocket(sock);

        try
        {
            setMyInputStream(mySocket.getInputStream());
        }
        catch (IOException e)
        {	hadConn = true;
        	String msg = myName + " IOException on input stream open for " +
                    machineName + ":" + machinePort + " - " + e;
        	Plog.log.addLog(msg);
            setExitFlag(msg);

            return (false);
        }

        try
        {
            setMyOutputStream(mySocket.getOutputStream());
        }
        catch (IOException e)
        {	hadConn = true;
            setExitFlag(myName + " IOException on output stream open.");

            return (false);
        }
        // avoid a race condition by setting hadConn only after both succeed
        hadConn = (myInStream!=null) && (myOutStream!=null);
        return (hadConn);
    }

   
    /** close a connection, if any */
    public final void closeConn()
    {
        discardOutput = false;
        discardInput = false;
        // clear the variables first, then attempt the closings which may block
        InputStream inS = setMyInputStream(null);
        OutputStream outS = setMyOutputStream(null);
        SocketProxy sock = setMySocket(null);
        setBufSize(100);	// help the gc
        
        //G.print(myName + " Closing connections "+inS+" "+outS);
        //if(inS!=null || outS!=null || sock!=null) { G.print("CloseConn "+getErrString()); }
        try
        {
            if (inS != null)
            {	
                inS.close();
            }
        }
        catch (IOException e)
        {
        	//G.print(myName + " IOException on input stream close. "+inS);
        	//G.printStackTrace(e);
        }

        try
        {
            

            if (outS != null)
            {	
                outS.close();
            }
        }
        catch (IOException e)
        {
            //G.print(myName + " IOException on output stream close. "+outS);
        }

        try
        {
            if (sock != null)
            {	
                sock.close();
            }
        }
        catch (IOException e)
        {
        	//G.print(myName + " IOException on socket close. "+sock);
        }
        wakeUp();
    }

    public final void wakeUp()
    {   synchronized (outQueue) { outQueue.notifyAll(); }
        synchronized (inQueue) { inQueue.notifyAll(); }
    }
    public final synchronized void setExitFlag(String msg)
    {   setErrString(msg);
    	setExitFlag();
    }

    public final String getErrString()
    {
        return (errstring);
    }

    public final synchronized void setErrString(String v)
    {	if(errstring==null)
    	{
    		String in = inQueue.getHistory();
    		String out = outQueue.getHistory();
    		errstring = v+"\nIn "+in+"\nOut "+out+"\n";
    	}
    else { errstring += " : " + v; }
  
    }

    public final synchronized boolean eofOk()
    {
        return (eofok);
    }

    public final synchronized void setEofOk()
    {
        eofok = true;
    }

    public final synchronized boolean getExitFlag()
    {
        return (exitFlag);
    }

    public final void setMachineName(String inStr)
    {
        machineName = inStr;
    }

    public final void setMachinePort(int inPort)
    {
        machinePort = inPort;
    }

    public final synchronized SocketProxy setMySocket(SocketProxy s)
    {
        SocketProxy oldsock = mySocket;
        mySocket = s;
        return (oldsock);
    }

    public final synchronized InputStream setMyInputStream(InputStream s)
    {
        InputStream os = myInStream;
        myInStream = s;
        discardInput = false;
        synchronized (inQueue) { inQueue.reset(); inQueue.notifyAll(); }
        //System.out.println("setmyin "+s);
        notifyAll();

        return (os);
    }

    public final synchronized OutputStream setMyOutputStream(OutputStream s)
    {
        OutputStream os = myOutStream;
        myOutStream = s;
        discardOutput = false;
        if(myManager!=null) { myManager.missingItems = 0; }
        synchronized (outQueue) { outQueue.reset(); outQueue.notifyAll(); }
        notifyAll();
        return (os);
    }
   
    public final synchronized OutputStream getMyOutStream()
    {
        if (mySocket != null)
        {
            return (myOutStream);
        }
        return (null);
    }

    public final synchronized InputStream getMyInputStream()
    {
        if (mySocket != null)
        {
            return (myInStream);
        }

        return (null);
    }


    public final boolean haveConn()
    { //	return(sendMessage("999"));
    	OutputStream os = myOutStream;
    	InputStream is = myInStream;
    	SocketProxy myS = mySocket;
    	boolean hasConn = started 
    			&& !exitFlag 
    			&& (myS!=null)
    			&& is!=null 
    			&& os!=null 
    			&& myS.isConnected();
        return (hasConn);
    }

    /** public utility function to open a socket on a particular socket numbers */
    @SuppressWarnings("resource")
	public final SocketProxy getSocketConnection(String server,int sock) throws IOException
    {	 SocketProxy myS = null;
    	//SocketPermission sp = new SocketPermission(serverName,"connect");
    	//if((myManager!=null)
    	//		&& (sock==PROXY_HTTP_PORT)
    	//		&& (myManager.serverID>=16))	// has proxy
    	//{ proxySocket temp = new proxySocket(myManager);
    	//  if(temp.status==proxySocket.status_ok) 
    	//  	{ myS = temp; 
    	//  	}
    	//  else { temp.close(); }
    	//}
        if(myS==null)
        	{ myS = CommonNetConn.makeSocketConnection(server,sock); 
        	}
        
        //setting NoDelay seemed to work, but had no discernable effect
        //on the games.  However, it coincided with a rash of 
        //router crashes, possibly due to fragmented packets.
        //if(myS!=null) { myS.setTcpNoDelay(true); }
        
        //myS.setTcpNoDelay(true);
        //System.out.println("Socket timout="+sock.getSoTimeout()+" delay "+sock.getTcpNoDelay());
        //myS.setTcpNoDelay(true);
        //System.out.println("Timeout "+myS.getSoTimeout());
        //System.out.println("Linger "+myS.getSoLinger());
        //System.out.println("Nodelay "+myS.getTcpNoDelay());
         return(myS);
    }

    /** public utility function to open a socket on the first available
       from a list of socket numbers */

    public final SocketProxy getSocketConnection(String server, int[] socks)
    {
        SocketProxy myS = null;

        for (int i = 0; (myS == null) && (i < socks.length); i++)
        {
            try
            {	myS = getSocketConnection(server,socks[i]);
            }
            catch (Throwable e)
            {
            	G.print("Can't get socket " + socks[i] + ": " + e);
                setErrString("getsocket:" + e);
            }
        }

        return (myS);
    }
 
    public final void inputWake() { synchronized(inQueue) { inQueue.notifyAll();}}
    
    // just enqueue the message without checking of the stream is active.  This is used to stuff
    // the initial connection message before the stream is actually set up
    public final void alwaysSendMessage(TYPE m)
    {
    	 outQueue.putItem(m);
    }
    public final boolean sendMessage(TYPE m)
    {
        if (myOutStream != null)
        {	
            String err = outQueue.putItem(m);

            boolean isError = (err != null);

            if (isError)
            {	Plog.log.addLog("network error: ",err);
                setExitFlag(err);
                Http.postError(this, "network error: "+ err,null);
            }

            return (!isError);
        }

        return (false);
    }

    // write one packet
    final boolean doWriteStep()
    {	OutputStream os = getMyOutStream();
        if(os != null)
        {
            TYPE m = outQueue.getItem();

            if (m != null)
            {
                if(!discardOutput) { realSendMessage(os,m); }
                outQueue.setWaitStart(0);
                return(true);
            }
            else
            {	// wait for someone to stuff a message
            	finishOutput(os);
            }
        }
        else
        {	// wait for the connection to be established
            waitForOutputStream();
        }
        return(false);	// nothing sent
    }
    
    // send any pending data that may be buffered on the wire
    // call this before waiting!
    private final void finishOutput(OutputStream os)
    {
    	try {
			os.flush();
		} catch (IOException e) {
			if (!getExitFlag())
	    	 {
	    		 String msg = "stream flush: "+e;
	    		 setExitFlag(msg);
	    	 }
		}
    }

    public final TYPE peekInputItem()
    {
    	return(inQueue.peekItem());
    }
    public final TYPE getInputItem()
    {	TYPE item = inQueue.getItem();
    	//if(item!=null) { System.out.println("in: "+item); }
        return (item);
    }
    public final TYPE getInputItem(int waitTime)
    {
    	TYPE item = inQueue.getItem();
    	if((item==null) && !exitFlag)
    		{ inQueue.setWaitStart(G.Date());
    		  item = inQueue.getItem(); 
    		  if(item==null && waitTime>=0) 
    		  {   synchronized(inQueue)
    			  {
    			  waiting = true;	
    			  item = inQueue.getItem();
    			  if(item==null) 
    			  	{ //G.addLog("Start wait");
    			  	  G.waitAWhile(inQueue,waitTime); item=inQueue.getItem();
    			  	  //G.addLog("End wait");
    			  	} 
	    		  waiting = false;
    			  }}
    		}
    		if(item!=null) { inQueue.setWaitStart(0); }
    	return(item);
    }
    // decode a byte buffer as UTF8
    final String decodeAsUtf8(byte[]inBuf,int i,int inBufLength)
    {
       	@SuppressWarnings("resource")
		Utf8OutputStream buf = new Utf8OutputStream();
        int escape = 0;
        int hexescape = 0;
        int escapeval = 0;
		for(;i<inBufLength;i++) 
		{ 
		 byte cc=inBuf[i];
		 if(escape==0)
		 {	if(cc=='\\') { escape=3; hexescape=escapeval=0; } else { buf.write(cc); }
		 }
		 else // escape processing
		 { if(cc=='\\') { escape=hexescape=0; buf.write(cc); }
		   else if(cc=='#') 
		   { hexescape=4; 
		   }
		   else if(hexescape>0) 
		   { escapeval = escapeval*16+Character.digit((char)cc,16);
		     hexescape--;
		     if(hexescape==0) 
		     { char ccv = (char)escapeval;
		       buf.write(ccv); 
		       escape=hexescape=0; 
		     }
		   }
		   else 
		   { escapeval=escapeval*10+cc-'0'; escape--;
		   	 if(escape==0) 
		   	 { buf.write((char)escapeval);
		   	 }
		   } 
		}
		}
        String str = buf.toString();
        return(str);
    }
    public final void run()
    {
        if (Thread.currentThread() == reader)
        {
            G.setThreadName(Thread.currentThread(),"Reader");
            runRead();
        }
        else if (Thread.currentThread() == writer)
        {
            G.setThreadName(Thread.currentThread(),"Writer");
            runWrite();

            //System.out.println("Writer exit");
        }
        else
        {
            Http.postError(this, "unknown thread", null);
        }
    }

    
    // this is the real "read" loop.  It loops forever, reading 
    // a line from the net connection and stuffing it into the 
    // input ring.
    final void runRead()
    {
        for (;;)
        {
            if (getExitFlag())
            {
                closeConn();

                break;
            }

            InputStream s = getMyInputStream();

            if(s!=null) 
            	{	
            		doReadStep(s);
            		if(waiting) { G.wake(inQueue); } 
            	}
            else { waitForInputStream(); }
 
        }
        //System.out.println("Reader exit");
        reader = null;
    }

    // run the writer process, which takes data from the
    // output queue and send it on the wire.
    private final void runWrite()
    {
    while (!getExitFlag())
    {	if(delays>0) { G.doDelay(Random.nextInt(R,delays)); }
    	
        boolean some = doWriteStep();
        if(!some) { 
        	long now = G.Date();
        	outQueue.setWaitStart(now);
        	synchronized(outQueue) {
        		if(!outQueue.hasData())
        		{    
        		//G.print(G.shortTime(G.Date())+" Start waiting for output to send");
        		G.waitAWhile(outQueue,0);
         		} 
        	}
        }
    }

    writer = null;
    }
	public static SocketProxy makeSocketConnection(String server,int port) throws IOException
	{	Plog.log.addLog("Open socket ",server,":",port);
		SocketProxy p = (USE_NATIVE_SOCKETS && !G.isIOS()
							? new ClientSocket(server, port)
							: new Socket(server,port));
		Plog.log.addLog("opened socket ",p);
		return(p);
	}
}
