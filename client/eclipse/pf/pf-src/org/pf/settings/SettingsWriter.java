// =======================================================================
// Title   : INTERFACE SettingsWriter
// Author  : Manfred Duchrow
// Version : 1.0 - 01/10/1999
// History :
// 01/10/1999 duma  created
//
// Copyright (c) 1999-2002 by Manfred Duchrow. All rights reserved.
// =======================================================================
package org.pf.settings;


// ========================================================================
// IMPORT
// ========================================================================

/**
 * Defines the interface a class must implement to be able to store
 * a settings object.
 */
public interface SettingsWriter
{

	// ========================================================================
	// PUBLIC INSTANCE METHODS
	// ========================================================================

	/**
	 * Store the given settings object to whatever datastore the
	 * implementer supports.
	 * 
	 * @param settings The settings data to be stored.
	 * @return true, if the settings have been successfully stored
	 */	
	public boolean storeSettings( Settings settings ) ;
	
} // interface SettingsWriter