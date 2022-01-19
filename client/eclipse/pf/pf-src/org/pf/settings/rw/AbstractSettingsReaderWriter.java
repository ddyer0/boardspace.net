// ===========================================================================
// CONTENT  : ABSTRACT CLASS AbstractSettingsReaderWriter
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 04/07/2003
// HISTORY  :
//  26/05/2002  duma  CREATED
//	04/07/2003	duma	added		->	fileUtil()
//
// Copyright (c) 2002-2003, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.settings.rw ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.file.FileUtil;
import org.pf.settings.Settings;
import org.pf.settings.SettingsReader;
import org.pf.settings.SettingsWriter;
import org.pf.text.StringUtil;

/**
 * Provides common convenience methods for settings readers and writers
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
abstract public class AbstractSettingsReaderWriter implements SettingsReader, SettingsWriter
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final Class DEFAULT_SETTINGS_CLASS = org.pf.settings.impl.SettingsImpl.class ;
	
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public AbstractSettingsReaderWriter()
  {
    super() ;
  } // AbstractSettingsReaderWriter()
 
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns a newly created Settings object filled with the data
	 * from the datastore the implementer supports.
	 * 
	 * @return An instance of SettingsImpl filled with the data from the datastore
	 */	
	public Settings loadSettings() 
	{
		return this.loadSettings( null ) ;
	} // loadSettings()
 
	// ------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	protected Class getDefaultSettingsClass()
	{
		return DEFAULT_SETTINGS_CLASS ;
	} // getDefaultSettingsClass()

	// -------------------------------------------------------------------------
	
	protected Settings newSettings( Class settingsClass )
	{
		Settings settings = null ;
    
    if ( settingsClass == null )
		{
			settingsClass = this.defaultSettingsClass() ;
		}
    
		if ( settingsClass != null )
		{
			try
			{
				settings = (Settings)settingsClass.getDeclaredConstructor().newInstance() ;
			}
			catch ( Exception ex )
			{
				// don't care, instantiate default implementation later
			} 
		}
		if ( settings == null )
		{
			settings = new org.pf.settings.impl.SettingsImpl() ;
		}    
		return settings ;    
	} // newSettings()
 
	// ------------------------------------------------------------------------

	protected FileUtil fileUtil()
	{
		return FileUtil.current() ;
	} // fileUtil()
 
	// ------------------------------------------------------------------------

	protected StringUtil str()
	{
		return StringUtil.current() ;
	} // str()
 
	// ------------------------------------------------------------------------

	// =========================================================================
	// PRIVATE INSTANCE METHODS
	// =========================================================================
	private Class defaultSettingsClass()
	{
		Class aClass ;
		
		aClass = this.getDefaultSettingsClass() ;
		if ( ( aClass != null ) && ( Settings.class.isAssignableFrom(aClass) ) )
			return aClass ;
		else
			return DEFAULT_SETTINGS_CLASS ;
	} // defaultSettingsClass()

	// -------------------------------------------------------------------------

} // class AbstractSettingsReaderWriter