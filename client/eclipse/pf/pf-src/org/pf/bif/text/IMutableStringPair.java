// ===========================================================================
// CONTENT  : INTERFACE IMutableStringPair
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 24/03/2008
// HISTORY  :
//  24/03/2008  mdu  CREATED
//
// Copyright (c) 2008, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.bif.text ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Specifies additional methods for IStringPair objects that allow to modify
 * them. 
 *
 * @author M.Duchrow
 * @version 1.0
 */
public interface IMutableStringPair extends IStringPair
{ 
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Sets the first string to the given value.
	 * 
	 * @param string The new value (may be null).
	 */
	public void	setString1( String string ) ;
	
	// -------------------------------------------------------------------------
	
	/**
	 * Sets the second string to the given value.
	 * 
	 * @param string The new value (may be null).
	 */
	public void	setString2( String string ) ;
	
	// -------------------------------------------------------------------------
	
} // interface IMutableStringPair