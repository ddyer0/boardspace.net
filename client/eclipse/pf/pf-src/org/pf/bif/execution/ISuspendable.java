// ===========================================================================
// CONTENT  : INTERFACE ISuspendable
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 24/02/2006
// HISTORY  :
//  24/02/2006  mdu  CREATED
//
// Copyright (c) 2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.bif.execution ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Defines method suspend()
 *
 * @author M.Duchrow
 * @version 1.0
 */
public interface ISuspendable
{ 
	/**
	 * Suspends the execution of the receiver
	 */
	public void suspend() ;
	
} // interface ISuspendable