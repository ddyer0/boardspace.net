// =======================================================================
// Title   : INTERFACE SettingsReader
// Author  : Manfred Duchrow
// Version : 1.0 - 01/10/1999
// History :
// 01/10/1999 duma  created
//
// Copyright (c) 1999-2002, by Manfred Duchrow. All rights reserved.
// =======================================================================
package org.pf.settings;

// ========================================================================
// IMPORT
// ========================================================================

/**
 * Defines the interface a class must implement to be a reader for 
 * settings.
 */
public interface SettingsReader
{

	// ========================================================================
	// PUBLIC INSTANCE METHODS
	// ========================================================================
	
	/**
	 * Returns a newly created Settings object filled with the data
	 * from the datastore the implementer supports.
	 * 
	 * @return New settings filled with the data from the datastore
	 */	
	public Settings loadSettings() ;

	// ------------------------------------------------------------------------

	/**
	 * Returns a newly created Settings object filled with the data
	 * from the datastore the implementer supports.
	 * 
	 * @param settingsClass The class of which an instance should be created and returned
	 * @return An instance of the given class filled with the data from the datastore
	 */	
	public Settings loadSettings( Class settingsClass ) ;
	
		
} // interface SettingsReader