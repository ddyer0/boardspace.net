// ===========================================================================
// CONTENT  : INTERFACE ISystemExitListener
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 01/03/2008
// HISTORY  :
//  01/03/2008  mdu  CREATED
//
// Copyright (c) 2008, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.bif.lifecycle ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * An implementor of this interface can be added to a listener list that   
 *
 * @author M.Duchrow
 * @version 1.0
 */
public interface ISystemExitListener
{ 
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * This method should be called just before the program gets terminated
	 * by a System.exit(rc) call.
	 * 
	 * @param rc The return code for the exit.
	 */
	public void systemAboutToExit(int rc);
	
	// -------------------------------------------------------------------------

} // interface ISystemExitListener