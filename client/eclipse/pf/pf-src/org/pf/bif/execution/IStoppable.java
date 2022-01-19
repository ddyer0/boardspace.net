// ===========================================================================
// CONTENT  : INTERFACE IStoppable
// AUTHOR   : M.Duchrow
// VERSION  : 1.1 - 08/08/2006
// HISTORY  :
//  24/02/2006  mdu  CREATED
//	08/08/2006	mdu		bugfix	-->	Define stop() rather than start()
//
// Copyright (c) 2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.bif.execution ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * To be implemented by classes that provide a service which can be stopped
 *
 * @author M.Duchrow
 * @version 1.1
 */
public interface IStoppable
{ 
	/**
	 * Stop the service the implementing object provides
	 */
	public void stop() ;
  
} // interface IStoppable