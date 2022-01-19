// ===========================================================================
// CONTENT  : INTERFACE TextRepresentation
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 03/10/1999
// HISTORY 
//  03/10/1999	duma  CREATED
//	25/01/2000	duma	moved		-> from package 'com.mdcs.text'
//
// Copyright (c) 1999-2000, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.textx;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * This interface defines the method an object must support,
 * if it wants to provide its text representation other than
 * by the method <i>toString()</i>.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public interface TextRepresentation
{
  /**
   * Returns if the textual representation of the receiver.
   *
   * @return The string representation of the receiver..
   */
  public String asText() ;

} // interface TextRepresentation