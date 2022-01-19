// ===========================================================================
// CONTENT  : CLASS ManifestReaderWriter
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.4 - 18/03/2005
// HISTORY  :
//  24/05/2002  duma  CREATED
//	30/05/2002	duma	changed	2->	New superclass
//	18/12/2002	duma	added		->	Set the filename as the name of the read settings
//	04/07/2003	duma	changed	->	Moved all implementation from class to instance
//	18/03/2005	mdu		changed	->	Added FileLocator support
//
// Copyright (c) 2002-2005, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.settings.rw ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.pf.file.FileFinder;
import org.pf.file.FileLocator;
import org.pf.file.FileUtil;
import org.pf.settings.Settings;

/**
 * Implements functionality to read manifest files into a settings object
 * and write a settings object to a manifest file.
 *
 * @author Manfred Duchrow
 * @version 1.4
 */
public class ManifestReaderWriter extends AbstractSettingsFileReaderWriter
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	/**
	 * Specifies the relative path (including file name) of the manifest file.
	 * "META-INF/MANIFEST.MF"
	 */
	public static final String RELATIVE_MANIFEST_PATH = "META-INF/MANIFEST.MF" ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // PUBLIC CLASS METHODS
  // =========================================================================
	/**
	 * Converts the given properties object to a settings object and
	 * returns it.
	 */
	public static Settings manifestToSettings( Manifest manifest, Class settingsClass )
	{
		ManifestReaderWriter inst ;
		
		inst = new ManifestReaderWriter() ;
		return inst.convertManifestToSettings( manifest, settingsClass ) ;		
	} // manifestToSettings()
   
	// ------------------------------------------------------------------------

	/**
	 * Loads the manifest data from the specified file and returns it as
	 * settings.
	 * The file will be automatically searched for in the whole classpath.
	 * 
	 * @param filename The name of the manifest file.
	 */
	public static Settings loadSettings( String filename, Class settingsClass ) 
	{
		ManifestReaderWriter inst ;
		
		inst = new ManifestReaderWriter() ;
		return inst.readSettings( filename, settingsClass ) ;
	} // loadSettings()
   
	// ------------------------------------------------------------------------

	/**
	 * Reads in a manifest file from the file with the specified name.
	 * Returns the manifest object or null in any case of error.
	 * The file will be automatically searched for in the whole classpath.
	 * 
	 * @param filename The name of the file to read
	 */
	public static Manifest readManifestFrom( String filename )
	{
		ManifestReaderWriter inst ;
		URL url ;
		
		inst = new ManifestReaderWriter() ;

		url = FileFinder.locateFile( filename ) ;
		if ( url == null )
			return null ;
		
		try
		{
			return inst.readManifestFrom( url.openStream() ) ;
		}
		catch ( IOException e )
		{
			return null ;
		}
	} // readManifestFrom()
   
  // -------------------------------------------------------------------------

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with defaults.
   */
  public ManifestReaderWriter()
  {
    super() ;
  } // ManifestReaderWriter()
  
	// -------------------------------------------------------------------------
	 
	/**
	 * Initialize the new instance with the given filename.
	 */
	public ManifestReaderWriter( String aFilename )
	{
		super( aFilename ) ;
	} // ManifestReaderWriter()
  
	// -------------------------------------------------------------------------
	 
	/**
	 * Initialize the new instance with the given file locator.
	 */
	public ManifestReaderWriter( FileLocator locator )
	{
		super( locator ) ;
	} // ManifestReaderWriter()
  
	// -------------------------------------------------------------------------
	 
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Stores the given settings to the file with this object's filename
	 * in the format of Java manifest files. 
	 * <H2>NOT YET IMPLEMENTED</H2>
	 * @param settings The settings to store
	 * @return true, if the settings have been successfully stored. Otherwise false.
	 */
	public boolean storeSettings( Settings settings ) 
	{
		return false ;
	} // storeSettings()
   
	// ------------------------------------------------------------------------

	/**
	 * Reads in manifest data from the specified stream.
	 * Returns the manifest object or null in any case of error.
	 * The stream will be closed after calling this method!
	 * 
	 * @param stream The stream to read the manifest data from
	 */
	public Manifest readManifestFrom( InputStream stream )
	{
		ManifestReaderWriter inst ;
		Manifest manifest = null ;
		
		inst = new ManifestReaderWriter() ;
		try
		{
			manifest = (Manifest)inst.readFromStream( stream, null ) ;
		}
		catch (IOException e)
		{
			// Ignore
		}
		finally
		{
			FileUtil.current().close( stream ) ;
		}
		return manifest ; 
	} // readManifestFrom()
   
  // -------------------------------------------------------------------------
	
	// ========================================================================
	// PROTECTED INSTANCE METHODS
	// ========================================================================
	/**
	 * Reads in a manifest file from the file with the specified name.
	 * Returns the manifest object or null in any case of error.
	 * The file will be automatically searched for in the whole classpath.
	 * 
	 * @param filename The name of the file to read
	 */
	protected Object readFromStream( InputStream stream, Class settingsClass ) 
		throws IOException
	{
		return new Manifest( stream ) ;
	} // readFromStream()
   
	// -------------------------------------------------------------------------

	protected Settings convertToSettings( Object result, Class settingsClass ) 
	{
		return this.convertManifestToSettings( (Manifest)result, settingsClass ) ;
	} // convertToSettings()
 
	// -------------------------------------------------------------------------

	protected Settings convertManifestToSettings( Manifest manifest, Class settingsClass )
	{
		Map entries 					= null ;
		Iterator iterator			= null ;
		Settings settings			= null ; 
		Attributes attributes	= null ;
		String categoryName		= null ;
		
		settings = this.newSettings( settingsClass ) ;
		this.addAttributesToSettings( null, manifest.getMainAttributes(), settings ) ;
		entries = manifest.getEntries() ;
		iterator = entries.keySet().iterator() ;
		while ( iterator.hasNext() )
		{
			categoryName = (String)iterator.next() ;
			attributes = (Attributes)entries.get( categoryName ) ;
			this.addAttributesToSettings( categoryName, attributes, settings ) ;
		}
		return settings ;
	} // convertManifestToSettings()
   
	// ------------------------------------------------------------------------

	protected void addAttributesToSettings( String categoryName,
																							Attributes attributes,
																							Settings settings )
	{
		Iterator iterator			= null ;
		Attributes.Name key		= null ;
		String value					= null ;
		
		iterator = attributes.keySet().iterator() ;
		while ( iterator.hasNext() )
		{
			key = (Attributes.Name)iterator.next() ;
			value = attributes.getValue( key ) ;
			settings.setValueOf( categoryName, key.toString(), value ) ;
		}
	} // addAttributesToSettings()
   
	// -------------------------------------------------------------------------

} // class ManifestReaderWriter