// ===========================================================================
// CONTENT  : CLASS SocketConnection
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 25/06/2004
// HISTORY  :
//  25/06/2004  mdu  CREATED
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.net ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import org.pf.util.ExceptionHandler;
import org.pf.util.ExceptionIgnorer;

/**
 * Generic superclass for connections on a specific socket (port).
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class SocketConnection extends ExceptionIgnorer
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private Socket socket = null ;
  protected Socket socket() { return socket ; }
  protected void socket( Socket newValue ) { socket = newValue ; }
  
  private AtomicBoolean isOpen = new AtomicBoolean(true);
  /** Returns true, if the socket connection is open */
  public boolean isOpen() { return isOpen.get(); }
  protected void isOpen( boolean newValue ) { isOpen.set(newValue); }  
  
  private AtomicBoolean inUse = new AtomicBoolean(false);
  protected boolean inUse() { return inUse.get(); }
  protected void inUse( boolean newValue ) { inUse.set(newValue); }  
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Initialize the new instance to work on the given socket.
	 */
	public SocketConnection(Socket openSocket)
	{
		this(openSocket, null);
	} // SocketConnection() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance to work on the given socket and use the
	 * specified exception handler to report exceptions to.
	 */
	public SocketConnection(Socket openSocket, ExceptionHandler exHandler)
	{
		super(exHandler);
		this.socket(openSocket);
	} // SocketConnection() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Close the connection and all release underlying resources.
	 */
	public void close()
	{
		this.isOpen(false);
		this.closeSocket();
	} // close()

	// -------------------------------------------------------------------------

	/**
	 * Returns true, if the connection can be used.
	 * In that case it automatically set the connection to be in use.
	 */
	protected synchronized boolean wantToUse()
	{
		if (this.inUse())
		{
			return false;
		}
		if (this.isClosed())
		{
			return false;
		}
		this.inUse(true);
		return true;
	} // wantToUse()

	// -------------------------------------------------------------------------

	/**
	 * Returns true, if the connection is closed and can't be used anymore.
	 */
	public boolean isClosed()
	{
		return !this.isOpen();
	} // isClosed()

	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	protected boolean hasSocket()
	{
		return this.socket() != null;
	} // hasSocket()

	// -------------------------------------------------------------------------

	protected void closeSocket()
	{
		try
		{
			if (this.hasSocket())
			{
				this.socket().close();
				this.socket(null);
			}
		}
		catch (IOException e)
		{
			this.exceptionOccurred(e);
		}
	} // closeSocket()

	// -------------------------------------------------------------------------

} // class SocketConnection 
