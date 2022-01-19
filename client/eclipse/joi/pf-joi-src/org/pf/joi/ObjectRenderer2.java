// ===========================================================================
// CONTENT  : INTERFACE ObjectRenderer2
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 09/04/2004
// HISTORY  :
//  09/04/2004  duma  CREATED
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.awt.Component;

/**
 * This interface must be implemented by classes that are responsible to
 * convert objects to a visual (java.awt.Component) representation which will 
 * then be used in an inspector. 
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public interface ObjectRenderer2 extends ObjectRenderer
{ 
  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  /**
   * Returns the visual representation of the given object.
   */
  public Component inspectComponent( Object obj ) ;  

  // -------------------------------------------------------------------------

} // interface ObjectRenderer2