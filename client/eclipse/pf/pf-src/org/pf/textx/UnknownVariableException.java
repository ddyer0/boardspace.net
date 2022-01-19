// ===========================================================================
// CONTENT  : EXCEPTION UnknownVariableException
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 26/06/1999
// HISTORY  :
//  24/06/1999 	duma  CREATED
//	25/01/2000	duma	moved		-> from package 'com.mdcs.text'
//
// Copyright (c) 1999-2000, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.textx;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * This exception class is used for signaling that a given
 * variable name is not known and can't be dealt with.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class UnknownVariableException extends TextReplacementException
{
  // ====================================================================
  // CONSTRUCTORS
  // ====================================================================

  /**
   * Initializes the receiver with the variable name that caused the exception.
   */
  public UnknownVariableException( String varName )
  {
    super( "Unknown variable named: '" + varName + "'" ) ;
  } // UnknownVariableException()

} // class UnknownVariableException 