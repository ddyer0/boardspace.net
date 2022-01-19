// ===========================================================================
// CONTENT  : INTERFACE ICondition
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 08/08/2006
// HISTORY  :
//  08/08/2006  mdu  CREATED
//
// Copyright (c) 2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.bif.logic ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Represents a condition independent of its internal complexity
 *
 * @author M.Duchrow
 * @version 1.0
 */
public interface ICondition
{ 
	/**
	 * Returns whether or not the condition is true
	 */
	public boolean isTrue() ;
	
} // interface ICondition