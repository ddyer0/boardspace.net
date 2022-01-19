// ===========================================================================
// CONTENT  : INTERFACE TriggerClient
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 26/05/2002
// HISTORY  :
//  26/05/2002  duma  CREATED
//
// Copyright (c) 2002, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * An object that wants to work with a trigger (see {@link Trigger}) is
 * a TriggerClient and must therefore implement this interface.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public interface TriggerClient
{ 
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

	/**
	 * Returns whether or not the trigger is allowed to activate the
	 * trigger client's triggeredBy() method.
	 * <br>
	 * This method will be called, whenever the trigger's timing allows the
	 * trigger to execute the trigger client's service method.
	 * So, eventually the trigger client decides itself, if its service
	 * method will be called this time.<br>
	 * Since the calling trigger is passed to this method, the client is able
	 * to react to more than one trigger. It can distinguish the calling
	 * triggers by name and dispatch internally to the appropriate method. 
	 * <br>
	 * <b>
	 * The implementer should be aware that this method will be called from
	 * a different thread than the one the trigger client is running in.
	 * Therefore it might be neccessary to do some <i>synchronization</i> here.
	 * </b>
	 * 
	 * @param trigger The trigger that calls this method
	 * @return true, if the service method triggeredBy() can be called
	 * @see Trigger
	 */
	public boolean canBeTriggeredBy( Trigger trigger ) ;
	
  // -------------------------------------------------------------------------

	/**
	 * This method will be called by a trigger whenever its timing says so.
	 * <br>
	 * Since the calling trigger is passed to this method, the client is able
	 * to react to more than one trigger. It can distinguish the calling
	 * triggers by name and dispatch internally to the appropriate method. 
	 * <br>
	 * <b>
	 * The implementer should be aware that this method will be called from
	 * a different thread than the one the trigger client is running in.
	 * Therefore it might be neccessary to do some <i>synchronization</i> here.
	 * </b>
	 * 
	 * @param trigger The trigger that calls this method
	 * @return true, if the trigger should continue, otherwise false
	 * @see Trigger
	 */
	public boolean triggeredBy( Trigger trigger ) ;
  
  // -------------------------------------------------------------------------
  
} // interface TriggerClient