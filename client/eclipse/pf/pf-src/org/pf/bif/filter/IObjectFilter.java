// ===========================================================================
// CONTENT  : INTERFACE IObjectFilter
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 24/02/2006
// HISTORY  :
//  24/02/2006  mdu  CREATED
//	15/01/2012	mdu		changed	-->	to generic type
//
// Copyright (c) 2006-2012, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.bif.filter ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Defines just one method useful for generic filtering.
 *
 * @author M.Duchrow
 * @version 1.1
 */
public interface IObjectFilter<T>
{ 
	/**
	 * Returns true if the given object matches the filter.
	 * 
	 * @param object Any object or null
	 */
	public boolean matches(T object);
	
} // interface IObjectFilter