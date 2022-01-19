// ===========================================================================
// CONTENT  : CLASS TextRequestServer
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 30/03/2012
// HISTORY  :
//  18/11/2002  mdu 	CREATED
//	26/06/2004	mdu		changed	-->	Superclass to RequestServer
//	30/03/2012	mdu		added		--> constructors with port and idle timeout
//
// Copyright (c) 2002-2012, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.net ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.net.Socket;

import org.pf.util.*;

/**
 * A request server that is specialized to accept requests in text form
 * (e.g. HTTP).
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
abstract public class TextRequestServer extends RequestServer
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

	// =========================================================================
  // CONSTRUCTORS
  // =========================================================================
	/**
	 * Initialize the new instance with default values.
	 */
	public TextRequestServer()
	{
		this(null);
	} // TextRequestServer() 

	// -----------------------------------------------------------------------

	/**
	 * Initialize the new instance with default values.
	 */
	public TextRequestServer(ExceptionHandler exHandler)
	{
		super(exHandler);
	} // TextRequestServer() 

	// -----------------------------------------------------------------------

  public TextRequestServer(int port, ExceptionHandler exHandler)
	{
		super(port, exHandler);
	} // TextRequestServer()

  // -------------------------------------------------------------------------
  
	public TextRequestServer(int port, int idleTimeout, ExceptionHandler exHandler)
	{
		super(port, idleTimeout, exHandler);
	} // TextRequestServer()

	// -------------------------------------------------------------------------
	
	public TextRequestServer(int port, int idleTimeout)
	{
		super(port, idleTimeout);
	} // TextRequestServer()

	// -------------------------------------------------------------------------
	
	public TextRequestServer(int port)
	{
		super(port);
	} // TextRequestServer()

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================

	// =========================================================================
	// ABSTRACT PROTECTED INSTANCE METHODS
	// =========================================================================
	abstract protected TextRequestProcessor createRequestProcessor(TextSocketConnection connection);

	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	protected RequestProcessor createRequestProcessor(SocketConnection connection)
	{
		return this.createRequestProcessor((TextSocketConnection)connection);
	} // createRequestProcessor() 

	// -------------------------------------------------------------------------

	protected SocketConnection createSocketConnection(Socket socket, ExceptionHandler exHandler)
	{
		return new TextSocketConnection(socket, this.exceptionHandler());
	} // createSocketConnection() 

	// -------------------------------------------------------------------------

} // class TextRequestServer 
