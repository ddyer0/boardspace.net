// ===========================================================================
// CONTENT  : CLASS RequestServer
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 30/03/2012
// HISTORY  :
//  25/06/2004  mdu  CREATED
//	23/08/2006	mdu		changed	-->	Better detection of already used port
//	30/03/2012	mdu		changed	-->	added idle timeout
//
// Copyright (c) 2004-2012, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.net ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicInteger;

import org.pf.util.ExceptionHandler;
import org.pf.util.ExceptionIgnorer;

/**
 * A generic server that listens to a port and starts processing threads
 * on the incoming requests.
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
abstract public class RequestServer extends ExceptionIgnorer implements Runnable
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private PortListener portListener = null ;
  protected PortListener portListener() { return portListener ; }
  protected void portListener( PortListener newValue ) { portListener = newValue ; }
    
  private int port = 0 ;
  protected int port() { return port ; }
  protected void port( int newValue ) { port = newValue ; }    
    
  private boolean done = false ;
  protected boolean done() { return done ; }
  protected void done( boolean newValue ) { done = newValue ; }

  private AtomicInteger connectionIdleTimeout = new AtomicInteger(0);
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
	/**
	 * Initialize the new instance with default values.
	 */
	public RequestServer()
	{
		super();
	} // RequestServer() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with an exception handler.
	 */
	public RequestServer(ExceptionHandler exHandler)
	{
		super(exHandler);
	} // RequestServer() 

	// -----------------------------------------------------------------------

	/**
	 * Initialize the new instance with a port number.
	 */
	public RequestServer(int port)
	{
		this(port, null);
	} // RequestServer() 

	// -----------------------------------------------------------------------

	/**
	 * Initialize the new instance with a port number and an idle timeout.
	 */
	public RequestServer(int port, int idleTimeout)
	{
		this(port, idleTimeout, null);
	} // RequestServer() 
	
	// -----------------------------------------------------------------------
	
	/**
	 * Initialize the new instance with a port number and an exception handler.
	 */
	public RequestServer(int port, ExceptionHandler exHandler)
	{
		this(exHandler);
		this.port(port);
	} // RequestServer() 

	// -----------------------------------------------------------------------

	/**
	 * Initialize the new instance with a port number, an idle timeout and an exception handler.
	 */
	public RequestServer(int port, int idleTimeout, ExceptionHandler exHandler)
	{
		this(exHandler);
		this.port(port);
		this.setConnectionIdleTimeout(idleTimeout);
	} // RequestServer() 
	
	// -----------------------------------------------------------------------
	
	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Opens the server socket on the configured port then runs a loop to
	 * listen on this socket until done is set to true and then closes the
	 * socket. The socket will also be closed in case of any runtime exception.
	 */
	public void run()
	{
		if (this.initialize())
		{
			try
			{
				this.listen();
			}
			finally
			{
				this.close();
			}
		}
	} // run() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the idle timeout in milliseconds after which an unused connection
	 * will be closed automatically.
	 * <br>
	 * The default implementation returns 0, that is no idle timeout.
	 * The method must return a value greater than zero to establish an idle timeout.
	 */
	public int getConnectionIdleTimeout()
	{
		return connectionIdleTimeout.get();
	} // getConnectionIdleTimeout()
	
	// -------------------------------------------------------------------------
	
	/**
	 * Sets the idle timeout in milliseconds after which an unused connection
	 * will be closed automatically.
	 * <br>
	 * A zero or negative value will disable the idle timeout.
	 */
	public void setConnectionIdleTimeout(int timeout)
	{
		connectionIdleTimeout.set(timeout);
	} // setConnectionIdleTimeout()
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the port on which this server is listening for requests.
	 */
	public int getPort() 
	{
		return this.port();
	} // getPort()
	
	// -------------------------------------------------------------------------
	
	// =========================================================================
	// ABSTRACT PROTECTED INSTANCE METHODS
	// =========================================================================
	/**
	 * Creates a new request processor on the given connection.
	 * 
	 * @param connection The connection that provides the data of the incoming request
	 */
	abstract protected RequestProcessor createRequestProcessor(SocketConnection connection);

	// -------------------------------------------------------------------------

	/**
	 * Creates a new socket connection on the given socket on the given connection.
	 * 
	 * @param socket The socket on which the connection is based
	 * @param exHandler The exception handler to report exceptions (might be null)
	 */
	abstract protected SocketConnection createSocketConnection(Socket socket, ExceptionHandler exHandler);

	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	protected Thread createProcessingThread(RequestProcessor requestProcessor)
	{
		return new Thread(requestProcessor);
	} // createProcessingThread() 

	// -------------------------------------------------------------------------

	protected SocketConnection waitForRequestConnection()
	{
		Socket socket;
		socket = this.portListener().waitForRequest();
		if (socket == null)
		{
			return null;
		}
		if (this.getConnectionIdleTimeout() > 0)
		{
			try
			{
				socket.setSoTimeout(this.getConnectionIdleTimeout());
			}
			catch (SocketException ex)
			{
				this.exceptionOccurred(ex);
			}
		}
		return this.createSocketConnection(socket, this.exceptionHandler());
	} // waitForRequestConnection() 

	// -------------------------------------------------------------------------

	protected Socket waitForRequest()
	{
		return this.portListener().waitForRequest();
	} // waitForRequest() 

	// -------------------------------------------------------------------------

	/**
	 * Opens the server socket on the configured port and returns true if it
	 * was successful and can be used for further processing.
	 */
	protected boolean initialize()
	{
		PortListener listener;

		listener = new PortListener(this.port(), this.exceptionHandler());
		this.portListener(listener);
		if (!this.portListener().isOpen())
		{
			this.done(true);
		}
		return !this.done();
	} // initialize() 

	// -------------------------------------------------------------------------

	protected void listen()
	{
		SocketConnection connection;
		RequestProcessor requestProcessor;
		Thread processingThread;

		while (!this.done())
		{
			connection = this.waitForRequestConnection();
			if (connection == null)
			{
				this.done(true);
			}
			else
			{
				requestProcessor = this.createRequestProcessor(connection);
				processingThread = this.createProcessingThread(requestProcessor);
				processingThread.start();
			}
		}
	} // listen() 

	// -------------------------------------------------------------------------

	protected void close()
	{
		if (this.portListener() != null)
		{
			this.portListener().close();
			this.portListener(null);
		}
	} // close() 

	// -------------------------------------------------------------------------
	
} // class RequestServer 
