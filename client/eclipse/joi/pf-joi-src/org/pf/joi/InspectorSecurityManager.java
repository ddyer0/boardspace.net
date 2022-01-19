// ===========================================================================
// CONTENT  : CLASS InspectorSecurityManager
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 11/04/2000
// HISTORY  :
//  11/04/2000  duma  CREATED
//
// Copyright (c) 2000, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.security.Permission;
import java.lang.reflect.ReflectPermission;

/**
 * This security manager allows to use the reflection API in all environments.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class InspectorSecurityManager extends SecurityManager
{
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  // -------------------------------------------------------------------------

  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public InspectorSecurityManager()
  {
  	super() ;
  } // InspectorSecurityManager()  

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  public void checkPermission(Permission perm)
  {
	if ( perm.getClass() == ReflectPermission.class )
	  return ;
	super.checkPermission( perm ) ;
  } // checkPermission()  

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  // -------------------------------------------------------------------------

} // class InspectorSecurityManager