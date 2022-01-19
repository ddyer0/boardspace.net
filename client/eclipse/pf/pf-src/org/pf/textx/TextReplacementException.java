// ===========================================================================
// CONTENT  : EXCEPTION TextReplacementException
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 26/06/1999
// HISTORY  :
//  26/06/1999 	duma  CREATED
//	25/01/2000	duma	moved		-> from package 'com.mdcs.text'
//
// Copyright (c) 1999, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.textx;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * This exception class is a root exception class for
 * exceptions of text replacement services.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class TextReplacementException extends Exception
{
  // ====================================================================
  // CONSTRUCTORS
  // ====================================================================

  /**
   * Initializes the receiver with the given message text.
   */
  public TextReplacementException( String text )
  {
    super( text ) ;
  } // TextReplacementException()

} // class TextReplacementException 