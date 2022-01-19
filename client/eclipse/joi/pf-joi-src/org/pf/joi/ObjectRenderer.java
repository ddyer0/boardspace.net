// ===========================================================================
// CONTENT  : INTERFACE ObjectRenderer
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 26/06/2000
// HISTORY  :
//  26/06/2000  duma  CREATED
//
// Copyright (c) 2000, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi;
// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * This interface must be implemented by classes that are responsible to
 * convert objects to a string representation which will then be used in
 * an inspector. 
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public interface ObjectRenderer
{ 
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  /**
   * Returns the string representation of the specified object.
   */
  public String inspectString( Object obj ) ;  

  // -------------------------------------------------------------------------

} // interface ObjectRenderer