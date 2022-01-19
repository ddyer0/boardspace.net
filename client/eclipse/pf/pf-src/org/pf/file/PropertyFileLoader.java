// ===========================================================================
// CONTENT  : CLASS PropertyFileLoader
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.6 - 20/12/2005
// HISTORY  :
//  02/12/2001  duma  CREATED
//	23/01/2002	duma	changed	-> Use findFile() indstead of findFileOnClassPath()
// 	24/05/2002	duma	added		-> Allow filepath with archive names like sub-directory names
//	04/07/2003	duma	added		-> 3 new loadProperties() methods (1 for filename, 2 for stream)
//	20/12/2003	duma	changed	-> Use logger for exception output
//	17/09/2004	duma	added		-> implements LineProcessor and methods to load PropertiesFileContent
//	20/12/2005	duma	added		-> loadProperties() for FileLocator
//
// Copyright (c) 2001-2005, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.file;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Properties;

/**
 * Provides some static convenience methods to load properties from
 * property files.
 * The files will be searched on the classpath. 
 * They will be properly loaded even from files inside archives.
 *
 * @author Manfred Duchrow
 * @version 1.6
 */
public class PropertyFileLoader implements LineProcessor
{
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private PropertiesFileContent properties = null ;
  protected PropertiesFileContent getProperties() { return properties ; }
  protected void setProperties( PropertiesFileContent newValue ) { properties = newValue ; }
  
  // =========================================================================
  // PUBLIC CLASS METHODS
  // =========================================================================

	/**
	 * Loads all properties from the file with the given name.
	 * Returns the properties or null in any case of exception or
	 * if the file wasn't found.
	 */
	public static Properties loadProperties( String filename )
	{
		return loadProperties( filename, null ) ;
	} // loadProperties() 
  
	// -------------------------------------------------------------------------
  
	/**
	 * Loads all properties from the file with the given name.
	 * Returns the properties or null in any case of exception or
	 * if the file wasn't found.
	 * Places the given default properties into the newly created properties
	 * as defaults. 
	 */
	public static Properties loadProperties( String filename, Properties defaults )
	{
		URL url   = null ;
    
		url = FileFinder.locateFile( filename ) ;
		if ( url == null )
			return null ;
		else
			return loadProperties( url , defaults) ;
	} // loadProperties() 
  
	// -------------------------------------------------------------------------
  
  /**
   * Loads all properties from the given file.
   * Returns the properties or null in any case of exception or
   * if the file wasn't found.
   */
  public static Properties loadProperties( File file )
  {
    return loadProperties( file, null ) ;
  } // loadProperties() 
  
  // -------------------------------------------------------------------------
    
  /**
   * Load all properties from the given file.
   * Returns the properties or null in any case of exception or
   * if the file wasn't found.
   * The given default properties a used as default values, if
   * the corresponding properties are not set in the file.
   */
  public static Properties loadProperties( File file, Properties defaults )
  {
    return loadProperties( FileLocator.create( file ), defaults ) ;
  } // loadProperties() 
  
  // -------------------------------------------------------------------------

  /**
   * Loads all properties from the specified file.
   * Returns the properties or null in any case of exception or
   * if the file wasn't found.
   */
  public static Properties loadProperties( FileLocator locator )
  {
    return loadProperties( locator, null ) ;
  } // loadProperties() 
  
  // -------------------------------------------------------------------------
    
  /**
   * Load all properties from the given file.
   * Returns the properties or null in any case of exception or
   * if the file wasn't found.
   * The given default properties a used as default values, if
   * the corresponding properties are not set in the file.
   */
  public static Properties loadProperties( FileLocator locator, Properties defaults )
  {
  	Properties properties	= null ;
  	InputStream stream		= null ;
  	
  	try
  	{
  		if ( locator.exists() )
  		{
  			stream = locator.getInputStream() ;
  			properties = loadProperties( stream, defaults ) ;
  		}
  	}
  	catch ( Exception ex )
  	{
  		LoggerProvider.getLogger().logException(ex) ;
  	}
  	finally
  	{
  		FileUtil.current().close( stream ) ;
  	}
  	return properties ;
  } // loadProperties() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Load all properties from the file specified by the given URL.
   * Returns the properties or null in any case of exception or
   * if the file wasn't found.
   * The given default properties a used as default values, if
   * the corresponding properties are not set in the file.
   * 
   * @param url The url that specifies the file to read the properties from
   * @param defaults The default properties to be put into the result (may be null)
   */
  public static Properties loadProperties( URL url, Properties defaults )
  {
    Properties properties	= null ;
    InputStream stream		= null ;

    try
    {
       stream = url.openStream() ;
       properties = loadProperties( stream, defaults ) ;
    }
    catch ( Exception ex )
    {
      LoggerProvider.getLogger().logException(ex) ;
    }
    finally
    {
      FileUtil.current().close( stream ) ;
    }
    return properties ;
  } // loadProperties() 
  
  // -------------------------------------------------------------------------

	/**
	 * Loads all properties from the given stream.
	 * Returns the properties.
	 * If the given default properties are not null they are set as defaults
	 * in the newly created properties object.
	 * 
	 * @param stream The stream to read the properties from
	 * @param defaults The default properties (might be null) 
	 */
	public static Properties loadProperties( InputStream stream, Properties defaults )
		throws IOException
	{
		Properties properties	= null ;

		if ( stream != null )
		{ 
			if ( defaults == null )
			{
				properties = new Properties() ;
			}
			else
			{
				properties = new Properties( defaults ) ;
			}

			properties.load( stream ) ;
		}		
		return properties ;
	} // loadProperties() 
  
	// -------------------------------------------------------------------------
    
	/**
	 * Loads all properties from the given stream.
	 * Returns the properties.
	 * 
	 * @param stream The stream to read the properties from
	 */
	public static Properties loadProperties( InputStream stream )
		throws IOException
	{
		return loadProperties( stream, null ) ;
	} // loadProperties() 
  
	// -------------------------------------------------------------------------

  /**
   * Reads all text lines from the given file and returns the result in an
   * PropertiesFileContent object. All comments and blank lines are preserved.
   * The order of the properties will be the same as in the file.
   * Apart from that the backslash is not treated as a special character for
   * escaping character sequences. That means, all single backslashes in 
   * the file will be preserved as well.
   * 
   * @param filename The name of the file to read (can be also a URL)
   */
  public static PropertiesFileContent loadFullPropertiesFile( String filename ) 
  	throws IOException
	{
  	PropertyFileLoader loader ;
  	
  	loader = new PropertyFileLoader() ;
  	return loader.loadFrom( FileLocator.create( filename ) ) ;
	} // loadFullPropertiesFile() 

	// -------------------------------------------------------------------------
  
  /**
   * Reads all text lines from the given stream and returns the result in an
   * PropertiesFileContent object. All comments and blank lines are preserved.
   * The order of the properties will be the same as in the file.
   * Apart from that the backslash is not treated as a special character for
   * escaping character sequences. That means, all single backslashes in 
   * the file will be preserved as well.
   * Using a reader even allows to read properties from sources that have
   * a different character encoding than the default.
   * After the calling this method the given reader will be closed!
   * 
   * @param reader The reader to load the data from
   */
  public static PropertiesFileContent loadFullPropertiesFile( Reader reader ) 
  	throws IOException
	{
  	PropertyFileLoader loader ;
  	
  	loader = new PropertyFileLoader() ;
  	return loader.loadFrom( reader ) ;
	} // loadFullPropertiesFile() 

	// -------------------------------------------------------------------------
  
  /**
   * Reads all text lines from the given stream and returns the result in an
   * PropertiesFileContent object. All comments and blank lines are preserved.
   * The order of the properties will be the same as in the file.
   * Apart from that the backslash is not treated as a special character for
   * escaping character sequences. That means, all single backslashes in 
   * the file will be preserved as well.
   * After the calling this method the given stream will be closed!
   * 
   * @param stream The stream to read the data from
   */
  public static PropertiesFileContent loadFullPropertiesFile( InputStream stream ) 
  throws IOException
  {
  	PropertyFileLoader loader ;
  	
  	loader = new PropertyFileLoader() ;
  	return loader.loadFrom( stream ) ;
  } // loadFullPropertiesFile() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  private PropertyFileLoader()
  {
  	super() ;
  } // PropertyFileLoader() 

  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Processes each line that is read from the file
   */
	public boolean processLine( String line, int lineNo )
	{
		this.getProperties().addLine( line ) ;
		return true;
	} // processLine() 

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * Reads all text lines from the given file and returns the in an
   * PropertiesFileContent object.
   * 
   * @param filename The name of the file to read
   */
  protected PropertiesFileContent loadFrom( FileLocator locator ) 
  	throws IOException
	{
		return this.loadFrom( locator.getInputStream() ) ;
	} // loadFrom() 

	// -------------------------------------------------------------------------
  
  /**
   * Reads all text lines from the given stream and returns the in an
   * PropertiesFileContent object.
   * 
   * @param stream The stream to read the properties from
   */
  protected PropertiesFileContent loadFrom( InputStream stream ) 
  	throws IOException
	{
		return this.loadFrom( new BufferedReader( new InputStreamReader( stream ) ) ) ;
	} // loadFrom() 

	// -------------------------------------------------------------------------
  
  /**
   * Reads all text lines from the given reader and returns the in an
   * PropertiesFileContent object.
   * 
   * @param reader The reader to read the properties from
   */
  protected PropertiesFileContent loadFrom( Reader reader ) 
  	throws IOException
  {
  	this.setProperties( new PropertiesFileContent() ) ;
  	this.fileUtil().processTextLines( reader, this ) ;
  	return this.getProperties() ;
  } // loadFrom() 
  
  // -------------------------------------------------------------------------
  
	protected FileUtil fileUtil() 
	{
		return FileUtil.current() ;
	} // fileUtil() 

	// -------------------------------------------------------------------------	

} // class PropertyFileLoader 
