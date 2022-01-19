// ===========================================================================
// CONTENT  : CLASS PortListener
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 23/08/2006
// HISTORY  :
//  18/11/2002  mdu  CREATED
//	23/08/2006	mdu		added		-->	isOpen(), getPort()
//
// Copyright (c) 2002-2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.net ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.pf.util.*;

/**
 * Encapsulates the functionality to open a port (i.e. server socket) and
 * start listening on it.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class PortListener extends ExceptionIgnorer
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private ServerSocket serverSocket = null ;
  protected ServerSocket serverSocket() { return serverSocket ; }
  protected void serverSocket( ServerSocket newValue ) { serverSocket = newValue ; }	

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with the given port.
   */
  public PortListener( int port )
  {
    this( port, null) ;
  } // PortListener()

	// -----------------------------------------------------------------------

  /**
   * Initialize the new instance with the given port and exception handler.
   */
  public PortListener( int port, ExceptionHandler handler )
  {
    super( handler ) ;
    this.initialize( port ) ;
  } // PortListener()

	// -----------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Listen to the port until a request comes in and return the newly
   * created socket that can be used to handle the request.
   * <p>
   * If the socket is not opened yet then null will be returned.
   */
	public Socket waitForRequest()
	{
		Socket client = null ;
		
		if ( this.isOpen() )
		{
			try
			{
				client = this.serverSocket().accept() ;
			}
			catch (IOException e)
			{
				this.exceptionOccurred(e) ;
			}
		}
		return client ;
	} // waitForRequest()

	// -------------------------------------------------------------------------

	/**
	 * Close the underlying socket.
	 */
	public void close()
	{
		if ( this.isOpen() )
		{
			try
			{
				this.serverSocket().close() ;
			}
			catch (IOException e)
			{
				this.exceptionOccurred(e) ;
			}
			this.serverSocket(null) ;
		}
	} // close()

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the server socket is open and can be used.
	 */
	public boolean isOpen() 
	{
		return ( this.serverSocket() != null ) ;
	} // isOpen()
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the port number this port listener is listening on.
	 * If not opened it returns -1.
	 */
	public int getPort() 
	{
		if ( this.isOpen() )
		{
			return this.serverSocket().getLocalPort() ;
		}
		return -1 ;
	} // getPort()
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected void initialize( int port )
	{
		try
		{
			this.serverSocket( new ServerSocket(port) ) ;
		}
		catch (IOException e)
		{
			this.exceptionOccurred(e) ;
		}	
	} // initialize()

	// -------------------------------------------------------------------------

} // class PortListener
