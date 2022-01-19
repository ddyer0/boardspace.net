// ===========================================================================
// CONTENT  : CLASS WrappingRuntimeException
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 07/11/2003
// HISTORY  :
//  07/11/2003  mdu  CREATED
//
// Copyright (c) 2003, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * This runtime exception can be used to wrap a "normal" exception. This
 * allows to catch a "normal" exception, but it into a WrappingRuntimeException
 * and throw the WrappingRuntimeException instead.
 * The benefit is to avoid the annoying an quite often useless throws clauses
 * in the method signature. As consequnce no try/catch blocks must be put
 * around the callers of such a method.
 * FIGHT CODE POLLUTION!
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class WrappingRuntimeException extends RuntimeException
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private Exception exception = null ;
	protected Exception getException() { return exception ; }
	protected void setException( Exception newValue ) { exception = newValue ; }
	
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public WrappingRuntimeException( Exception ex )
  {
    super( ex.getMessage() ) ;
    this.setException( ex ) ;
  } // WrappingRuntimeException()

	// -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the string representation of the wrapped exception
   */
	public String toString()
	{
		return this.getException().toString() ;
	} // toString()

	// -------------------------------------------------------------------------
	
	/**
	 * Print the stack trace of the wrapped exception to stdout
	 */
	public void printStackTrace()
	{
		this.getException().printStackTrace() ;
	} // printStackTrace()

	// -------------------------------------------------------------------------
	
	/**
	 * Print the stack trace of the wrapped exception to the given stream
	 */
	public void printStackTrace( PrintStream stream  )
	{
		this.getException().printStackTrace( stream ) ;
	} // printStackTrace()

	// -------------------------------------------------------------------------

	/**
	 * Print the stack trace of the wrapped exception to the given writer
	 */
	public void printStackTrace( PrintWriter writer  )
	{
		this.getException().printStackTrace( writer ) ;
	} // printStackTrace()

	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class WrappingRuntimeException
