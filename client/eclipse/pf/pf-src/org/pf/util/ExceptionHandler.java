// ===========================================================================
// CONTENT  : INTERFACE ExceptionHandler
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 28/02/2003
// HISTORY  :
//  15/11/2002  duma  CREATED
//	28/02/2003	duma	moved		->	From org.pf.net
//
// Copyright (c) 2002-2003, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Whenever a component doesn't want to throw every exception and declare them
 * in their methods it is useful to give the exceptions to an exception handler.
 * This handler can log or ignore the exception, or even throw a runtime
 * exception or exit the program.  <p>
 * Some Programmer's Friend components are supporting this mechanism. They all
 * pass their exceptions to an exception handler if one was set. Otherwise the
 * they just swallow the exceptions.
 * This interface must be implemented by every handler that's intended to be 
 * used with a PF component.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public interface ExceptionHandler
{ 
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

	/**
	 * Handle the given exception in an apropriate way.
	 * 
	 * @param throwable The exception that occured somewhere
	 */
	public void handleException( Throwable throwable ) ;

  // -------------------------------------------------------------------------
  
} // interface ExceptionHandler