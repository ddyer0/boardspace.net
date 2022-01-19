// ===========================================================================
// CONTENT  : INTERFACE IResettable
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
 * To be implemented by objects that can be resetted
 *
 * @author M.Duchrow
 * @version 1.0
 */
public interface IResettable
{ 
	/**
	 * Reset the internal state 
	 */
	public void reset() ;
	
} // interface IResettable