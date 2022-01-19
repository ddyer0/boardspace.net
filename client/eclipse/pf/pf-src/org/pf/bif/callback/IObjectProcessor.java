// ===========================================================================
// CONTENT  : INTERFACE IObjectProcessor
// AUTHOR   : M.Duchrow
// VERSION  : 2.0 - 27/03/2010
// HISTORY  :
//  03/06/2006  mdu  CREATED
//	27/03/2010	mdu		changed to support generic types
//
// Copyright (c) 2006-2010, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.bif.callback ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Specifies a simple interface that allows loops or recursive executions
 * to send each object to an implementor of this interface. Additionally
 * it can control whether or not execution must be terminated. 
 *
 * @author M.Duchrow
 * @version 2.0
 */
public interface IObjectProcessor<T>
{ 
	/**
	 * Process the given object and return true if processing should be continued,
	 * otherwise false.
	 * 
	 * @param object The object to be processed (null must be handled)
	 * @return true to continue the processing, false to stop the processing
	 */
	public boolean processObject( T object ) ;
	
} // interface IObjectProcessor