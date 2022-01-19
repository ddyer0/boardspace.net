// ===========================================================================
// CONTENT  : CLASS BinaryRequestServer
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 30/03/2012
// HISTORY  :
//  25/06/2004  mdu  CREATED
//	30/03/2012	mdu		added		--> constructors with port and idle timeout
//
// Copyright (c) 2004-2012, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.net ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.net.Socket;

import org.pf.util.ExceptionHandler;

/**
 * Abstract superclass for servers that handle binary requests
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
abstract public class BinaryRequestServer extends RequestServer
{
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
	/**
	 * Initialize the new instance with default values.
	 */
	public BinaryRequestServer()
	{
		this(null);
	} // BinaryRequestServer() 

	// -----------------------------------------------------------------------

	/**
	 * Initialize the new instance with default values.
	 */
	public BinaryRequestServer(ExceptionHandler exHandler)
	{
		super(exHandler);
	} // BinaryRequestServer() 

	// -----------------------------------------------------------------------

	public BinaryRequestServer(int port, ExceptionHandler exHandler)
	{
		super(port, exHandler);
	} // BinaryRequestServer()

	// -----------------------------------------------------------------------

	public BinaryRequestServer(int port, int idleTimeout, ExceptionHandler exHandler)
	{
		super(port, idleTimeout, exHandler);
	} // BinaryRequestServer()

	// -------------------------------------------------------------------------
	
	public BinaryRequestServer(int port, int idleTimeout)
	{
		super(port, idleTimeout);
	} // BinaryRequestServer()

	// -------------------------------------------------------------------------
	
	public BinaryRequestServer(int port)
	{
		super(port);
	} // BinaryRequestServer()

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================

	// =========================================================================
	// ABSTRACT PROTECTED INSTANCE METHODS
	// =========================================================================
	abstract protected RequestProcessor createRequestProcessor(BinarySocketConnection connection);

	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	protected RequestProcessor createRequestProcessor(SocketConnection connection)
	{
		return this.createRequestProcessor((BinarySocketConnection)connection);
	} // createRequestProcessor() 

	// -------------------------------------------------------------------------

	protected SocketConnection createSocketConnection(Socket socket, ExceptionHandler exHandler)
	{
		return new BinarySocketConnection(socket, this.exceptionHandler());
	} // createSocketConnection() 

	// -------------------------------------------------------------------------

} // class BinaryRequestServer 
