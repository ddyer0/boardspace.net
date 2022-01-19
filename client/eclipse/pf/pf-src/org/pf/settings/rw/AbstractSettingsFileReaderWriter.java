// ===========================================================================
// CONTENT  : CLASS AbstractSettingsFileReaderWriter
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.5 - 08/01/2009
// HISTORY  :
//  30/05/2002  duma  CREATED
//	18/12/2002	duma	changed	->	Implementing now SettingsFileReader
//										added		->	New constructor that gets a filename
//	04/07/2003	duma	changed	->	Refactorings
//	18/03/2005	mdu		changed	->	Added FileLocator support
//	24/03/2006	mdu		added		->	encoding
//	08/01/2009	mdu		added		->	support strings as input
//
// Copyright (c) 2002-2009, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.settings.rw ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.pf.file.ClasspathElement;
import org.pf.file.FileLocator;
import org.pf.settings.Settings;
import org.pf.settings.SettingsFileReader;

/**
 * This class provides a common implementation for SettingsReader and
 * SettingsWriter that use a file as datastore.
 *
 * @author Manfred Duchrow
 * @version 1.5
 */
abstract public class AbstractSettingsFileReaderWriter 
								extends AbstractSettingsReaderWriter
								implements SettingsFileReader
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final String NEWLINE = System.getProperty("line.separator") ;
	
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String fileName = null ;
  /**
   * Returns the name of the file from which or to which the setting should be
   * written/read.
   */
  public String getFileName() { return fileName ; }
  /**
   * Sets the name of the file from which or to which the setting should be
   * written/read.
   */
  public void setFileName( String aValue ) { fileName = aValue ; }

  // -------------------------------------------------------------------------
  
  private String encoding = null ;
  /**
   * Returns the encoding to be used for reading an writing. If encoding is
   * null then the default encoding will be used.
   */
  /**
   * Set the encoding to be used for reading an writing. If encoding is
   * null then the default encoding will be used.
   */
  public String getEncoding() { return encoding ; }
  public void setEncoding( String newValue ) { encoding = newValue ; }
  
  // -------------------------------------------------------------------------
  
  private FileLocator fileLocator = null ;
  protected FileLocator getFileLocator() { return fileLocator ; }
  protected void setFileLocator( FileLocator newValue ) { fileLocator = newValue ; }
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public AbstractSettingsFileReaderWriter()
  {
    super() ;
  } // AbstractSettingsFileReaderWriter() 

	// -----------------------------------------------------------------------
	
  /**
   * Initialize the new instance with a filename.
   * 
   * @param filename The name of the file the settings must be read from
   */
  public AbstractSettingsFileReaderWriter( String filename )
  {
    this() ;
    this.setFileName( filename ) ;
  } // AbstractSettingsFileReaderWriter() 

	// -----------------------------------------------------------------------	

  /**
   * Initialize the new instance with a file locator.
   * 
   * @param fileLocator The locator for the file the settings must be read from
   */
  public AbstractSettingsFileReaderWriter( FileLocator fileLocator )
  {
    this() ;
    this.setFileName( fileLocator.getOriginalFileName() ) ;
    this.setFileLocator( fileLocator ) ;
  } // AbstractSettingsFileReaderWriter() 

	// -----------------------------------------------------------------------	

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns a newly created Settings object filled with the data
	 * from the datastore the implementer supports.
	 * 
	 * @param settingsClass The class of which an instance should be created and returned
	 * @return An instance of the given class filled with the data from the datastore
	 */	
	public Settings loadSettings( Class settingsClass ) 
	{
		if ( this.getFileLocator() != null )
		{
			return this.readSettings( this.getFileLocator(), settingsClass ) ;
		}
		return this.readSettings( this.getFileName(), settingsClass ) ;
	} // loadSettings() 

	// ------------------------------------------------------------------------

	/**
	 * Returns a newly created Settings object filled with the data
	 * from the file the implementer supports. The file is looked up in the given
	 * classpath element.
	 * 
	 * @param classpathElement The classpath element from wich to load the settings file
	 * @return An new instance of a Settings class filled with the data from the file
	 * @throws IOException If the file cannot be found or opened
	 */	
	public Settings loadSettingsFrom( ClasspathElement classpathElement )
		throws IOException
	{
		return this.loadSettingsFrom( classpathElement, null ) ;
	} // loadSettingsFrom() 
	
	// ------------------------------------------------------------------------
	
	/**
	 * Returns a newly created Settings object filled with the data
	 * from the file the implementer supports. The file is looked up in the given
	 * classpath element.
	 * 
	 * @param classpathElement The classpath element from wich to load the settings file
	 * @param settingsClass The class of which an instance should be created and returned
	 * @return An instance of the given class filled with the data from the datastore
	 * @throws IOException If the file cannot be found or opened
	 */	
	public Settings loadSettingsFrom( ClasspathElement classpathElement, Class settingsClass )
	throws IOException
	{
		InputStream stream ;
		Settings settings ;
		
		stream = classpathElement.open( this.getFileName() ) ;
		settings = this.readSettings( stream, settingsClass ) ;
		if ( settings != null )
		{
			settings.setName( classpathElement.getName() + "/" + this.getFileName() ) ;
		}
		return settings ;
	} // loadSettingsFrom() 
	
	// ------------------------------------------------------------------------
	
	/**
	 * Uses the given string as content to parse the settings from.
	 */
	public Settings loadSettingsFrom( String content ) 
	{
		return this.loadSettingsFrom( content, null ) ;
	} // loadSettingsFrom()
	
	// -------------------------------------------------------------------------
	
	/**
	 * Uses the given string as content to parse the settings from.
	 */
	public Settings loadSettingsFrom( String content, Class settingsClass ) 
	{
		ByteArrayInputStream stream ;
		
		if ( content == null )
		{
			return null ;
		}
		stream = new ByteArrayInputStream( content.getBytes() ) ;
		return this.readSettings( stream, settingsClass ) ;
	} // loadSettingsFrom()
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // ABSTRACT PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * Reads the content of a file in any intermediate object. That also can
   * already be a valid Settings object.
   */
	abstract protected Object readFromStream( InputStream stream, Class settingsClass ) 
		throws IOException ;
	
	// -------------------------------------------------------------------------

	/**
	 * Converts the intermediate object read with 'readFromStream()' to a Settings
	 * object.
	 */
	abstract protected Settings convertToSettings( Object result, Class settingsClass ) ;

	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	/**
	 * Returns a newly created Settings object filled with the data
	 * from the file specified by filename.
	 * 
	 * @param filename The name of the file to read
	 * @param settingsClass The class of which an instance should be created and returned
	 * @return An instance of the given class filled with the data from the datastore
	 */	
	protected Settings readSettings( String filename, Class settingsClass )
	{
		FileLocator locator = null ;

		locator = FileLocator.create( filename ) ;
		return this.readSettings( locator, settingsClass ) ;
	} // readSettings() 
 
	// ------------------------------------------------------------------------

	/**
	 * Returns a newly created Settings object filled with the data
	 * from the file specified by filename.
	 * 
	 * @param locator The locator of the file to read
	 * @param settingsClass The class of which an instance should be created and returned
	 * @return An instance of the given class filled with the data from the datastore
	 */	
	protected Settings readSettings( FileLocator locator, Class settingsClass )
	{
		Settings settings ;
		InputStream stream ;
		
		try
		{
			stream = locator.getInputStream() ;
			settings = this.readSettings( stream, settingsClass ) ;
			if ( settings != null )
			{				
				settings.setName( locator.getOriginalFileName() ) ;
			}
		}
		catch ( IOException e )
		{
			settings = null;
		}		
		return settings ;
	} // readSettings() 
 
	// ------------------------------------------------------------------------

	/**
	 * Returns a newly created Settings object filled with the data
	 * from the given stream.
	 * After calling this method the given stream will be closed!
	 * 
	 * @param stream The input stream from which to read
	 * @param settingsClass The class of which an instance should be created and returned
	 * @return An instance of the given class filled with the data from the stream
	 */	
	protected Settings readSettings( InputStream stream, Class settingsClass )
	{
		Settings settings 	= null ;
		Object result ;
		
		try
		{
			result = this.readFromStream( stream, settingsClass ) ;
			settings = this.convertToSettings( result, settingsClass ) ;
		}
		catch ( IOException ex )
		{
			settings = null ;
		}
		finally
		{
			this.fileUtil().close( stream ) ;
		}
		
		return settings ;
	} // readSettings() 
	
	// ------------------------------------------------------------------------
	
	protected Reader createReader( InputStream stream ) 
	{
		Reader reader ;
		
		if ( this.getEncoding() == null )
		{
			reader = new InputStreamReader( stream ) ;
		}
		else
		{
			try
			{
				reader = new InputStreamReader( stream, this.getEncoding() ) ;
			}
			catch ( UnsupportedEncodingException e )
			{
				e.printStackTrace();
				reader = new InputStreamReader( stream ) ;
			}
		}
		return reader ;
	} // createReader() 
	
	// -------------------------------------------------------------------------
	
	protected Writer createWriter( OutputStream stream ) 
	{
		Writer writer ;
		
  	if ( this.getEncoding() == null )
		{
  		writer = new OutputStreamWriter( stream ) ;
		}
		else
		{
			try
			{
				writer = new OutputStreamWriter( stream, this.getEncoding() ) ;
			}
			catch ( UnsupportedEncodingException e )
			{
				e.printStackTrace();
				writer = new OutputStreamWriter( stream ) ;
			}
		}
  	return new BufferedWriter( writer ) ;
	} // createWriter() 
	
	// -------------------------------------------------------------------------
	
} // class AbstractSettingsFileReaderWriter 
