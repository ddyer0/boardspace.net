// ===========================================================================
// CONTENT  : INTERFACE IInitializablePlugin
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 25/02/2007
// HISTORY  :
//  25/02/2007  mdu  CREATED
//
// Copyright (c) 2007, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.plugin ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Properties;

/**
 * Defines the methods needed to initialize a plug-in implementation right
 * after its creation via its public no-argument constructor. 
 *
 * @author M.Duchrow
 * @version 1.0
 */
public interface IInitializablePlugin
{ 
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Initialize the newly created plug-in implementation with the externally
	 * defined ID and specified configuration properties.
	 * 
	 * @param id The externally specified identifier (never null)
	 * @param properties Configuration data (might be empty, but never null)
	 */
	public void initPlugin( String id, Properties properties ) ;
  
	// -------------------------------------------------------------------------
	
} // interface IInitializablePlugin