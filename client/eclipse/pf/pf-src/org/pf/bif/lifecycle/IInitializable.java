// ===========================================================================
// CONTENT  : INTERFACE IInitializable
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 24/02/2006
// HISTORY  :
//  24/02/2006  mdu  CREATED
//
// Copyright (c) 2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.bif.lifecycle ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Generic no argument initialization 
 *
 * @author M.Duchrow
 * @version 1.0
 */
public interface IInitializable
{ 
	/**
	 * Initialize the internal state
	 */
	public void initialize() ;
	
} // interface IInitializable