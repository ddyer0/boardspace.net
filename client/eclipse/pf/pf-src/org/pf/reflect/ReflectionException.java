// ===========================================================================
// CONTENT  : CLASS ReflectionException
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 03/06/2006
// HISTORY  :
//  03/06/2006  mdu  CREATED
//
// Copyright (c) 2006, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.reflect ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Special runtime exception to wrap all exceptions that might occur due
 * to reflective access to objects and classes.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class ReflectionException extends RuntimeException
{
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public ReflectionException( Throwable rootCause )
  {
    super( rootCause ) ;
  } // ReflectionException()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class ReflectionException
