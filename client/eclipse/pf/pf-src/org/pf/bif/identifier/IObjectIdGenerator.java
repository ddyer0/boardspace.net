// ===========================================================================
// CONTENT  : INTERFACE IObjectIdGenerator
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 22/02/2008
// HISTORY  :
//  22/02/2008  mdu  CREATED
//
// Copyright (c) 2008, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.bif.identifier ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Specifies the methods that are needed to generate new (unique) object
 * identifiers.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public interface IObjectIdGenerator
{ 
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns a new identifier which usually must be different to the
   * previously returned.
   */
  public String newIdentifier();

  // -------------------------------------------------------------------------
  
} // interface IObjectIdGenerator