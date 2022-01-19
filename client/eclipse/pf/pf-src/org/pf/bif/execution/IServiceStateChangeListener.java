// ===========================================================================
// CONTENT  : INTERFACE IServiceStateChangeListener
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 08/08/2006
// HISTORY  :
//  08/08/2006  mdu  CREATED
//
// Copyright (c) 2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.bif.execution ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * A listener that gets informed about any state changes of a service. 
 *
 * @author M.Duchrow
 * @version 1.0
 */
public interface IServiceStateChangeListener
{ 
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	/**
	 * Indicates that the service has been started
	 */
	public static final int SERVICE_STARTED				= 1 ;
	/**
	 * Indicates that the service has been stopped
	 */
	public static final int SERVICE_STOPPED				= 2 ;
	/**
	 * Indicates that the service has been suspended
	 */
	public static final int SERVICE_SUSPENDED			= 3 ;
	/**
	 * Indicates that the service has been resumed
	 */
	public static final int SERVICE_RESUMED				= 4 ;

	/**
	 * Any custom state value should be defined as addition of this constant
	 * plus any integer value
	 */
	public static final int FIRST_CUSTOM_STATE		= 50 ;
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Will be called to signal a state change of the given service.
	 * 
	 * @param service The service that has been changed
	 * @param event One of the SERVICE_XXX constants defined by this interface
	 */
  public void	serviceStateChanged( IService service, int event ) ;
  
  // -------------------------------------------------------------------------
  
} // interface IServiceStateChangeListener