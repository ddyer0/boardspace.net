// ===========================================================================
// CONTENT  : INTERFACE ITextProvider
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 25/03/2006
// HISTORY  :
//  25/03/2006  mdu  CREATED
//
// Copyright (c) 2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.nls ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Defines the minimal interface a class must implement in order to provide
 * text for identifying keys.
 * 
 * @author M.Duchrow
 * @version 1.0
 */
public interface ITextProvider
{ 
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the text associated with the given key or null if the key cannot
   * be found.
   * <br>
   * If the specified key is null then null will be returned.
   * 
   * @param key The identifier for the text
   */
  public String getText( String key ) ; 
  
  // -------------------------------------------------------------------------
  
} // interface ITextProvider