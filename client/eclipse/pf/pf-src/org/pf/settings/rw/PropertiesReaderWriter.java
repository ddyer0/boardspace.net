// =======================================================================
// Title   : CLASS PropertiesReaderWriter
// Author  : Manfred Duchrow
// Version : 1.7 - 24/03/2006
// History :
// 	01/10/1999 	duma  created
//	24/05/2002	duma	changed	->	Provide static methods and new superclass
//	30/05/2002	duma	changed	->	New superclass
//	18/12/2002	duma	changed	->	New constructor. Set filename as settings name
//	04/07/2003	duma	changed	->	Moved all implementation from class to instance
//	04/06/2004	duma	changed	->	Implemented storeSettings()
//	18/03/2005	mdu		changed	->	Added FileLocator support
//	24/03/2006	mdu		changed	->	Added support for other character encoding
//
// Copyright (c) 1999-2006, by Manfred Duchrow. All rights reserved.
// =======================================================================
package org.pf.settings.rw;

// ========================================================================
// IMPORT
// ========================================================================
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Properties;

import org.pf.file.FileLocator;
import org.pf.file.PropertiesFileContent;
import org.pf.file.PropertyFileLoader;
import org.pf.settings.Settings;

/**
 * A reader that can load a properties file into a settings object.
 * 
 * @author Manfred Duchrow
 * @version 1.7
 */
public class PropertiesReaderWriter extends AbstractSettingsFileReaderWriter
{
	// ========================================================================
	// CONSTANTS
	// ========================================================================
	
	// ========================================================================
	// INSTANCE VARIABLES
	// ========================================================================
  private boolean backslashIsEscapeChar = true ;
  /**
   * Sets whether or not backslashes should be treated as escape character.
   */
  public boolean getBackslashIsEscapeChar() { return backslashIsEscapeChar ; }
  /**
   * Returns whether or not backslashes should be treated as escape character.
   */
  public void setBackslashIsEscapeChar( boolean newValue ) { backslashIsEscapeChar = newValue ; }
  
	// ========================================================================
	// PUBLIC CLASS METHODS
	// ========================================================================

	/**
	 * Converts the given properties object to a settings object and
	 * returns it.
	 */
	public static Settings propertiesToSettings(Properties properties, Class settingsClass)
	{
		PropertiesReaderWriter inst ;
		
		inst = new PropertiesReaderWriter() ;
		return inst.convertPropertiesToSettings( properties, settingsClass ) ;
	} // propertiesToSettings() 
  
	// ------------------------------------------------------------------------

	/**
	 * Loads the properties from the specified file and returns them as
	 * settings.
	 * 
	 * @param filename The name of the properties file.
	 * @param settingsClass The class that implements the Settings interface of which an instance will be created
	 */
	public static Settings loadSettings(String filename, Class settingsClass)
	{
		PropertiesReaderWriter inst ;
		
		inst = new PropertiesReaderWriter() ;
		return inst.readSettings( filename, settingsClass ) ;
	} // loadSettings() 
  
	// ------------------------------------------------------------------------

	/**
	 * Loads the properties from the specified file and returns them as
	 * settings.
	 * 
	 * @param locator The locator that points to the properties file.
	 * @param settingsClass The class that implements the Settings interface of which an instance will be created
	 */
	public static Settings loadSettings(FileLocator locator, Class settingsClass)
	{
		PropertiesReaderWriter inst ;
		
		inst = new PropertiesReaderWriter() ;
		return inst.readSettings( locator, settingsClass ) ;
	} // loadSettings() 
  
	// ------------------------------------------------------------------------

	// ========================================================================
	// CONSTRUCTORS
	// ========================================================================
	/**
	 * Create a new reader for properties file.
	 */
	public PropertiesReaderWriter()
	{
		super();
	} // PropertiesReaderWriter() 
  
	// -----------------------------------------------------------------------
	
	/**
	 * Create a new reader for properties file that loads from a file with
	 * the given name.
	 * 
	 * @param filename The name of the file to read from
	 */
	public PropertiesReaderWriter(String filename)
	{
		this( filename, null );
	} // PropertiesReaderWriter() 
  
	// -----------------------------------------------------------------------
	
	/**
	 * Create a new reader for properties file that loads from a file with
	 * the given name using the specified character encoding.
	 * 
	 * @param filename The name of the file to read from
	 * @param charsetName The character encoding (e.g. "UTF-8") or null
	 */
	public PropertiesReaderWriter( String filename, String charsetName )
	{
		super(filename);
		this.setEncoding( charsetName ) ;
	} // PropertiesReaderWriter() 
	
	// -----------------------------------------------------------------------
	
	/**
	 * Create a new reader for properties file that loads from the file
	 * specified by the given locator.
	 * 
	 * @param locator The locator that points to the file to read from
	 */
	public PropertiesReaderWriter(FileLocator locator)
	{
		this( locator, null );
	} // PropertiesReaderWriter() 
  
	// -----------------------------------------------------------------------
	
	/**
	 * Create a new reader for properties file that loads from the file
	 * specified by the given locator using the specified character encoding.
	 * 
	 * @param locator The locator that points to the file to read from
	 * @param charsetName The character encoding (e.g. "UTF-8") or null
	 */
	public PropertiesReaderWriter(FileLocator locator, String charsetName )
	{
		super(locator);
		this.setEncoding( charsetName ) ;
	} // PropertiesReaderWriter() 
	
	// -----------------------------------------------------------------------
	
	// ========================================================================
	// PUBLIC INSTANCE METHODS
	// ========================================================================

	/**
	 * Stores the given settings to the file with this object's filename
	 * in the format of Java properties files. 
	 * 
	 * @param settings The settings to store
	 * @return true, if the settings have been successfully stored. Otherwise false.
	 */
	public boolean storeSettings(Settings settings)
	{
		if ( this.getFileName() == null )
			return false;
		
		return this.storeSettings( this.getFileName(), settings ) ;
	} // storeSettings() 
  
	// ------------------------------------------------------------------------

	// ========================================================================
	// PROTECTED INSTANCE METHODS
	// ========================================================================
	protected Object readFromStream( InputStream stream, Class settingsClass ) 
		throws IOException
	{
		Reader reader ;

		if ( this.mustUseReader() )
		{
			reader = this.createReader( stream ) ;
			return this.readFromReader( reader ) ;
		}
		return PropertyFileLoader.loadProperties(stream);
	} // readFromStream() 
	
	// -------------------------------------------------------------------------

	protected PropertiesFileContent readFromReader( Reader reader ) 
		throws IOException
	{
		return PropertyFileLoader.loadFullPropertiesFile( reader );
	} // readFromReader() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the properties cannot be loaded by the standatd Java
	 * mechanism. That is, either if backslashes should be preserved or if
	 * an explicit character encoding was specified.
	 */
	protected boolean mustUseReader() 
	{
		return ( ! this.getBackslashIsEscapeChar() ) || ( this.getEncoding() != null ) ;
	} // mustUseReader() 
	
	// -------------------------------------------------------------------------
	
	protected Settings convertToSettings( Object result, Class settingsClass ) 
	{
		if ( ! ( result instanceof Properties ) )
			return null ;
			
		return this.convertPropertiesToSettings( (Properties)result, settingsClass ) ;
	} // convertToSettings() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Converts the given properties object to a settings object and
	 * returns it.
	 */
	protected Settings convertPropertiesToSettings(Properties properties, Class settingsClass)
	{
		Enumeration propNames = null;
		String name = null;
		Settings settings = null;

		settings = this.newSettings(settingsClass);
		propNames = properties.propertyNames();
		while (propNames.hasMoreElements())
		{
			name = (String) propNames.nextElement();
			settings.setValueOf(name, properties.getProperty(name));
		}
		return settings;
	} // convertPropertiesToSettings() 
  
	// ------------------------------------------------------------------------

	protected boolean storeSettings( String filename, Settings settings)
  {
	  Writer writer ;
	  
	  try
		{
  		writer = this.createWriter( new FileOutputStream( filename ) ) ;
		}
		catch ( IOException e )
		{
			return false ;
		}
		
	  try
		{
			this.writeSettings( writer, settings ) ;
		}
		catch ( IOException e )
		{
			return false ;
		}
		finally
		{
			fileUtil().close( writer ) ;
		}
		return true ;
  } // storeSettings() 

  // -------------------------------------------------------------------------
	
	protected void writeSettings( Writer writer, Settings settings )
		throws IOException
  {
		String[] keys ;
		
	  keys = settings.getKeyNamesOfDefaultCategory() ;
	  for (int i = 0; i < keys.length; i++ )
		{
			writer.write( keys[i] ) ;
			writer.write( "=" ) ;
			writer.write( settings.getValueOf( keys[i] ) ) ; 
			writer.write( NEWLINE ) ;
		}
	  writer.flush() ;
  } // writeSettings() 

  // -------------------------------------------------------------------------
	
} // class PropertiesReaderWriter 
