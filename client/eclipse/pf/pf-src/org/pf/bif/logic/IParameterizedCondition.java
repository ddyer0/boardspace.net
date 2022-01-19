// ===========================================================================
// CONTENT  : INTERFACE IParameterizedCondition
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
 * A condition that evaluates to true or false depending on an external
 * parameter. 
 *
 * @author M.Duchrow
 * @version 1.0
 */
public interface IParameterizedCondition<T>
{ 
	/**
	 * Returns true if the condition evaluates to true for the given parameter.
	 * 
	 * @param parameter Any object that might be useful
	 */
	public boolean isTrue(T parameter);
	
} // interface IParameterizedCondition