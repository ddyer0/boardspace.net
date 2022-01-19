// ===========================================================================
// CONTENT  : INTERFACE StringFilter
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 06/05/2005
// HISTORY  :
//  06/05/2005  mdu  CREATED
//
// Copyright (c) 2005, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * A simple interface for abstraction of string pattern matching
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public interface StringFilter
{ 
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns true if the given string matches the filter otherwise false.
	 * 
	 * @param aString Any string or even null
	 */
	public boolean matches( String aString ) ;
	
} // interface StringFilter