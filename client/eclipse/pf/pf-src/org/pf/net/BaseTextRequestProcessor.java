// ===========================================================================
// CONTENT  : ABSTRACT CLASS BaseTextRequestProcessor
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 05/12/2002
// HISTORY  :
//  05/12/2002  mdu  CREATED
//
// Copyright (c) 2002, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.net ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * The abstract implementation of a text request processor that
 * leaves only the processing of a single request to subclasses.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
abstract public class BaseTextRequestProcessor implements TextRequestProcessor
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private TextSocketConnection connection = null ;
  protected TextSocketConnection connection() { return connection ; }
  protected void connection( TextSocketConnection newValue ) { connection = newValue ; }

  private boolean continueListening = true ;
  protected boolean continueListening() { return continueListening ; }
  protected void continueListening( boolean newValue ) { continueListening = newValue ; }      

  // =========================================================================
  // CLASS METHODS
  // =========================================================================


  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public BaseTextRequestProcessor( TextSocketConnection conn )
  {
    super() ;
    this.connection( conn ) ;
  } // BaseTextRequestProcessor()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Runs the processing of requests coming in from the underlying
   * socket connection.
   */
	public void run()
	{
		String request ;
		
		while ( this.continueListening() )
		{
			if ( this.connection().isOpen() )
			{
				request = this.connection().readLine() ;
				if ( request != null )
				{	
					this.processRequest( request ) ;
				}
			}
			else
			{
				this.continueListening(false) ;
			}
		}
		this.connection().close() ;		
	} // run()

	// -------------------------------------------------------------------------

	/**
	 * Subclasses must implement this method to process the request.
	 * 
	 * @param request The request as text that has been received from the connection
	 */
	abstract public void processRequest( String request ) ;

  // -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  // -------------------------------------------------------------------------

} // class BaseTextRequestProcessor
