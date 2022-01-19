// =======================================================================
// Title   : Interface CaseSensitivity
// Author  : Manfred Duchrow
// Version : 1.0 - 11/01/2003
// History :
// 	11/01/2003 	duma  created
//
// Copyright (c) 2003, by Manfred Duchrow. All rights reserved.
// =======================================================================
package org.pf.settings;

// ========================================================================
// IMPORT
// ========================================================================

/**
 * Classes that are implementing this interface are able to switch their
 * string comparisons from case sensistive to case insensitive and vice versa.
 * 
 * @author Manfred Duchrow
 * @version 1.0
 */
public interface CaseSensitivity
{

	// ========================================================================
	// PUBLIC INSTANCE METHODS
	// ========================================================================
		
  /**
   * Returns true, if the search for category and key names is case sensitive.
   */
  public boolean getCaseSensitive() ;
  
  // -------------------------------------------------------------------------
  
  /**
   * Sets whether or not the internal search for category names and
   * key names must be case sensitive.
   * 
   * @param isCaseSensitive The new value that determins if names are compared case sensitive or not
   */
  public void setCaseSensitive( boolean isCaseSensitive )  ;
	
	// -------------------------------------------------------------------------
	
} // interface CaseSensitivity