// ===========================================================================
// CONTENT  : INTERFACE Inspectable
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 17/11/1999
// HISTORY  :
//  17/11/1999  duma  CREATED
//
// Copyright (c) 1999, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * This interface defines the methods an object must support, if it
 * needs special representation in an inspector.
 * Normally an inspector uses the method <i>toString()</i> to get
 * a string representation of an object.<br>
 * If an objects implements the interface <b>Inspectable</b>, then the
 * inspector is using <i>inspectString()</i> instead.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public interface Inspectable
{
  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

	/**
	 * Returns the receiver's string representation for an inspector.
	 */
	public String inspectString() ;

  // -------------------------------------------------------------------------

} // class Inspectable