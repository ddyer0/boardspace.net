// ===========================================================================
// CONTENT  : INTERFACE IStartable
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
 * To be implemented by classes that provide a service which can be started
 *
 * @author M.Duchrow
 * @version 1.0
 */
public interface IStartable
{ 
	/**
	 * Start the service the implementing object provides
	 */
	public void start() ;
	
} // interface IStartable