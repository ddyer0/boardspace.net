// ===========================================================================
// CONTENT  : CLASS ExceptionIgnorer
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 28/02/2003
// HISTORY  :
//  18/11/2002  mdu CREATED
//	28/02/2003	mdu	moved		->	from org.pf.net, made all methods public
//
// Copyright (c) 2002-2003, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util;

// ===========================================================================
// IMPORTS
// ===========================================================================


/**
 * An abstract superclass that provides an instance variable to hold an
 * optional ExceptionHandler. It also implements a method handleException()
 * that uses the exception handler, if it is not null.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class ExceptionIgnorer
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================


  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private ExceptionHandler exceptionHandler = null ;
  protected ExceptionHandler exceptionHandler() { return exceptionHandler ; }
  protected void exceptionHandler( ExceptionHandler newValue ) { exceptionHandler = newValue ; }

  // =========================================================================
  // CLASS METHODS
  // =========================================================================


  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public ExceptionIgnorer()
  {
    super() ;
  } // ExceptionIgnorer()

	// -------------------------------------------------------------------------

  /**
   * Initialize the new instance with an exception handler.
   */
  public ExceptionIgnorer( ExceptionHandler handler )
  {
    super() ;
  } // ExceptionIgnorer()

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the exception handler currently in use. Might be null.
   */
  public ExceptionHandler getExceptionHandler() 
  { 
  	return exceptionHandler ; 
  } // getExceptionHandler() 
  
  // -------------------------------------------------------------------------

	/**
	 * Sets the exception handler.
	 * 
	 * @param exHandler The new exception handler (might be null)
	 */  
  public void setExceptionHandler( ExceptionHandler exHandler ) 
  { 
  	exceptionHandler = exHandler ;
  } // setExceptionHandler()

	// -------------------------------------------------------------------------
	
	/**
	 * Can be called for all exceptions that should be handled in the same way.
	 * If an exception handler is available the exception will be passed to it.
	 * 
	 * @param exception The occurred exception to be handled somehow
	 */
	public void exceptionOccurred( Throwable exception )
	{
		if ( this.exceptionHandler() != null )
		{
			this.exceptionHandler().handleException( exception ) ;
		}			
	} // exceptionOccured()

	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class ExceptionIgnorer
