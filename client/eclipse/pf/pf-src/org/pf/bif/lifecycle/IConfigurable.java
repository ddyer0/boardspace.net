// ===========================================================================
// CONTENT  : INTERFACE IConfigurable
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
import java.util.Map;

/**
 * Defines a generic configuration method
 *
 * @author M.Duchrow
 * @version 1.0
 */
public interface IConfigurable<K,V>
{ 
	/**
	 * Configure the receiver with the data provided by the given map 
	 * 
	 * @param configuration Contains the configuration data as key/value pairs
	 */
	public void configure(Map<K, V> configuration);
	
} // interface IConfigurable