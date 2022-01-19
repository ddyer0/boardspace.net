// ===========================================================================
// CONTENT  : INTERFACE SettingsFileReader
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 18/12/2002
// HISTORY  :
//  18/12/2002  duma  CREATED
//
// Copyright (c) 2002, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.settings ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Specializes the settings reader interface for files.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public interface SettingsFileReader extends SettingsReader
{ 
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  /**
   * Returns the name of the file from which the setting should be read.
   * written/read.
   */
  public String getFileName() ;

	// -----------------------------------------------------------------------
	  
} // interface SettingsFileReader