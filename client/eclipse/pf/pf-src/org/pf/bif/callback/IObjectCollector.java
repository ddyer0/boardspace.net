// ===========================================================================
// CONTENT  : INTERFACE IObjectCollector
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 03/06/2006
// HISTORY  :
//  03/06/2006  mdu  CREATED
//
// Copyright (c) 2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.bif.callback ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Collection;

/**
 * Implementors will be called by for elements in a loop or a recursive
 * processing. They can be used to add the processed objects to a given collection.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public interface IObjectCollector<T>
{ 
	/**
	 * Will be called with an object that maybe must be added to the given 
	 * collection.
	 * The decision is up to the implementor. The implementor actually must
	 * do the adding too. 
	 * 
	 * @param collection The collection to add objects (must not be null)
	 * @param object The object to be potentially added (must not be null)
	 * @return true to continue the processing, false to stop the processing
	 */
	public boolean collectObject(Collection<T> collection, T object);
	
} // interface IObjectCollector