// ===========================================================================
// CONTENT  : CLASS ClasspathElement
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.6.1 - 10/09/2006
// HISTORY  :
//  02/02/2003  mdu		CREATED
//	07/11/2003	mdu		added		-->	javadoc for open(), new method createURL(), DEBUG
//	20/12/2003	mdu		changed	-->	Using LoggerProvider
//	08/05/2004	mdu		changed	-->	createURL() now returns jar:file:/ as protocol for URLs that point into JAR files
//	25/12/2004	mdu		added		-->	toString(), equals(), hashCode()
//	26/05/2006	mdu		changed	-->	cache zipFile for re-use and performance optimization
//	01/09/2006	mdu		bugfix	--> Added isOpen() and fixed close()
//	10/09/2006	mdu		bugfix	--> Use "file:/" rather than "file://" in createURL()
//
// Copyright (c) 2003-2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.file ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.pf.logging.Logger;
import org.pf.util.SysUtil;

/**
 * Represents one entry in a classpath. It can be asked if it s a directory
 * or an archive. It also can be asked if it exists and it is valid (really
 * a zip archive).
 *
 * @author Manfred Duchrow
 * @version 1.6.1
 */
public class ClasspathElement
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	private static final String FILE_PROTOCOL_INDICATOR			= "file:/" ;
	private static final String JAR_FILE_PROTOCOL_INDICATOR	= "jar:file:/" ;
	private static final String ARCHIVE_INDICATOR						= "!/" ;

	private static final boolean DEBUG = "true".equals( System.getProperty("org.pf.file.ClasspathElement.debug") ) ;
	private static final String DEBUG_PREFIX = "org.pf.file.ClasspathElement: " ;
	
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String name = null ;
  /** Returns the name of this classpath element */
  public String getName() { return name ; }
  protected void setName( String newValue ) { name = newValue ; }
  
  private File file = null ;
  protected File getFile() { return file ; }
  protected void setFile( File newValue ) { file = newValue ; }
  
  private boolean valid = false ;
  protected boolean getValid() { return valid ; }
  protected void setValid( boolean newValue ) { valid = newValue ; }  
  
  private String id = null ;
  protected String getId() { return id ; }
  protected void setId( String newValue ) { id = newValue ; }  
  
  private boolean leaveArchiveOpen = false ;
  /**
   * Returns true, if an opened archive stays open until method close()
   * gets called. Returns true if the archive gets closed after each access.
   * The default is false.
   * @see #close()
   */
  public boolean leaveArchiveOpen() { return leaveArchiveOpen ; }
  /**
   * Sets whether an opened archive stays open until method close()
   * gets called. If set to false the archive gets closed after each access.
   * @see #close()
   */
  public void leaveArchiveOpen( boolean newValue ) { leaveArchiveOpen = newValue ; }
  
  private ZipFile zipFile = null ;
  protected void setZipFile( ZipFile newValue ) { zipFile = newValue ; }
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with a file representing either a directory
   * or an archive file.
   * 
   * @param aFile A file representing an element in a classpath
   */
  public ClasspathElement( File aFile )
  {
    super() ;
    if ( aFile == null )
    	throw new IllegalArgumentException( "ClasspathElement: File parameter must not be null" ) ;
    this.init( aFile ) ;
  } // ClasspathElement() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with default values.
   */
  public ClasspathElement( String elementName )
  {
    super() ;
    if ( elementName == null )
    	throw new IllegalArgumentException( "ClasspathElement: elementName parameter must not be null" ) ;
	    	
  	this.init( new File( elementName ) ) ;  
  } // ClasspathElement() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns whether or not this classpath element exists.
   */
  public boolean exists()
	{
		return this.getFile().exists() ;
	} // exists() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true, if this element is a file.
	 * Actually it only determines, if this element is a file. 
	 * This doesn't ensure that it is really an archive file.
	 */
	public boolean isFile()
	{
		return this.getFile().isFile() ;		
	} // isFile() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true, if this element is an archive file.
	 */
	public boolean isArchive()
	{
		return ( this.isFile() && this.isValid() ) ;		
	} // isArchive() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns true, if this element is a directory
	 */
	public boolean isDirectory()
	{
		return this.getFile().isDirectory() ;
	} // isDirectory() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true, if this element is either a valid directory or really an
	 * archive (zip file).
	 */
	public boolean isValid()
	{
		return this.getValid() ;
	} // isValid() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true, if this element represents an archive (zip file) and that
	 * archive is currently open.
	 * If it represents a directory this method returns false
	 */
	public boolean isOpen()
	{
		return zipFile != null ;
	} // isOpen() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns whether or not this element contains a file with the given name.
	 * 
	 * @param filename The name of the file to be looked for
	 */
	public boolean contains( String filename )
	{
		if ( this.isValid() )
		{
			if ( this.isDirectory() )
			{
				return this.dirContainsFile( filename ) ;
			}
			else
			{
				return this.archiveContainsFile( filename ) ;					
			}
		}
		return false ;
	} // contains() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a file info object for the given filename which is expected
	 * to be inside the directory or archive represented by this classpath element.
	 * 
	 * @param filename The name of the file to be looked for
	 * @return A file info object or null if the specified file does not exist
	 */
	public FileInfo getFileInfo( String filename )
	{
		File aFile ;
		ZipEntry zipEntry ;
		
		if ( this.isValid() )
		{
			if ( this.isDirectory() )
			{
				aFile = this.getFileFromDirectory( filename ) ;
				if ( ( aFile != null ) && ( aFile.exists() ) )
				{
					return new FileInfo( aFile ) ;
				}
			}
			else
			{
				zipEntry = this.getFileFromArchive( filename ) ;
				if ( zipEntry != null )
				{
					return new FileInfo( this.getName(), zipEntry ) ;
				}
			}
		}
		return null ;
	} // getFileInfo() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Opens the file with the given name in this classpath element and returns 
	 * the input stream. If the file doesn't exist or this classpath element is
	 * not valid, null will be returned.
	 * 
	 * @param filename The name of the file in this container to be opened
	 */
	public InputStream open( String filename )
		throws IOException
	{
		InputStream stream = null ;
		File aFile ;
		ZipFile aZipFile ;
		ZipEntry zipEntry ;
		
		if ( this.isValid() )
		{
			if ( this.isArchive() )
			{
				aZipFile = this.getZipFile() ;
				zipEntry = aZipFile.getEntry( filename ) ;
				if ( zipEntry != null )
				{
					stream = aZipFile.getInputStream( zipEntry ) ;
				}
				else
				{
					// System.out.println( filename ) ;
				}
			}
			else
			{
				aFile = new File( this.getFile(), filename ) ;
				stream = new FileInputStream( aFile ) ;
			}	
		}
		else
		{
			throw new IOException( "Invalid classpath element can't contain '" + filename + "'" ) ;
		}
		return stream ;
	} // open() 

	// -------------------------------------------------------------------------

	/**
	 * Close any underlying (maybe) opened archive 
	 */
	public synchronized void close() 
	{
		if ( zipFile != null )
		{
			try
			{
				zipFile.close() ;
			}
			catch ( IOException e )
			{
				// Don't care
			}
			this.setZipFile(null) ;
		}
	} // close() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns a URL that points to this classpath element.
	 */
	public URL getURL() 
	{
		return this.createURL( null ) ;   
	} // getURL() 

	// -------------------------------------------------------------------------
	
	/**
	 * Creates a URL for the file with the given name that is in this file
	 * container. It is not checked, if the file really exists.
	 * If this classpath element is invalid null will be returned. 
	 */
	public URL createURL( String filename )
	{
		StringBuffer urlStr ;
		URL url ;
		
		if ( ! this.isValid() )
		  return null ;
		  
		urlStr = new StringBuffer( 200 ) ;
		if ( this.isArchive() && ( filename != null ) )
		{
			urlStr.append( JAR_FILE_PROTOCOL_INDICATOR ) ;
		}
		else
		{
			urlStr.append( FILE_PROTOCOL_INDICATOR ) ;
		} 
		urlStr.append( this.futil().standardize( this.getFile().getAbsolutePath() ) ) ;
				  
		if ( filename != null )
		{
			urlStr.append( this.isArchive() ? ARCHIVE_INDICATOR : "/" ) ;
			urlStr.append( filename ) ;
		}
		try
		{
			url = new URL( urlStr.toString() ) ;
		}
		catch (MalformedURLException e)
		{
			if ( DEBUG )
			{
				this.logger().logException(e) ;
			}
			return null ;
		}
		return url ;   
	} // createURL() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns the element's string representation.
	 * The string always contains slashes, never backslashes.
	 */
	public String toString() 
	{
		return this.getName() ;
	} // toString() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns the hash value of this object
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		return this.getId().hashCode() ;
	} // hashCode() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the given object is equal to this object
	 */
	public boolean equals( Object object ) 
	{
		if ( object instanceof ClasspathElement )
		{
			ClasspathElement otherElement = (ClasspathElement)object ;
			return this.getId().equals( otherElement.getId() ) ;
		}
		return false ;
	} // equals() 

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected void init( File aFile )
	{
		this.setFile( aFile ) ;
		this.setName( this.futil().javaFilename( aFile.getPath() ) ) ;
		this.setId( this.futil().standardize( aFile.getAbsolutePath() ) ) ;
		if ( this.sys().isWindows() )
		{
			this.setId( this.getId().toLowerCase() ) ;
		}
		if ( this.exists() )
		{
			if ( this.isDirectory() )
			{
				this.setValid(true) ;
			}
			else
			{
				if ( this.isFile() )
				{
					this.setValid( this.checkArchive() ) ;
				}
			}
		}
	} // init() 

  // -------------------------------------------------------------------------

	protected boolean checkArchive()
	{
		try
		{
			this.getZipFile() ;
			if ( ! this.leaveArchiveOpen() )
			{
				this.close();
			}
			return true ;
		}
		catch (ZipException e)
		{
		}
		catch (IOException e)
		{
		}
		return false ;
	} // checkArchive() 

	// -------------------------------------------------------------------------

	protected boolean dirContainsFile( String filename )
	{
		File aFile ;
		
		aFile = this.getFileFromDirectory( filename ) ;
		if ( aFile == null )
		{
			return false ;
		}
		return aFile.exists() ;
	} // dirContainsFile() 

	// -------------------------------------------------------------------------	

	protected File getFileFromDirectory( String filename )
	{
		File aFile ;
		
		aFile = new File( this.getFile(), filename ) ;
		return aFile ;
	} // getFileFromDirectory() 
	
	// -------------------------------------------------------------------------	
	
	protected boolean archiveContainsFile( String filename )
	{
		return this.getFileFromArchive( filename ) != null ;
	} // archiveContainsFile() 

	// -------------------------------------------------------------------------
	
	protected ZipEntry getFileFromArchive( String filename )
	{
		ZipFile aZipFile ;
		ZipEntry entry ;
		
		try
		{
			aZipFile = this.getZipFile() ;
		}
		catch (Throwable e)
		{
			return null ;
		}
		try
		{
			entry = aZipFile.getEntry( filename ) ;
			return entry ;
		}
		catch (Throwable e)
		{
		}
		finally
		{
			if ( ! this.leaveArchiveOpen() )
			{
				this.close();
			}
		}
		return null ;
	} // getFileFromArchive() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Opens and returns the zip file. The zip file stays open until close()
	 * is called explicitely.
	 * @see #close()
	 */
  protected synchronized ZipFile getZipFile() throws ZipException, IOException
	{
  	if ( zipFile == null )
		{
			zipFile = new ZipFile( this.getFile() ) ;
		}
		return zipFile;
	} // getZipFile() 

  // -------------------------------------------------------------------------
	
	protected void debug( String text )
	{
		this.logger().logDebug( DEBUG_PREFIX + text ) ;	
	} // debug() 

	// -------------------------------------------------------------------------
		
	protected Logger logger()
	{
		return LoggerProvider.getLogger() ;
	} // logger() 

	// -------------------------------------------------------------------------	
		
	protected FileUtil futil() 
	{
		return FileUtil.current() ;
	} // futil() 

	// -------------------------------------------------------------------------
	
	protected SysUtil sys() 
	{
		return SysUtil.current() ;
	} // sys() 

	// -------------------------------------------------------------------------
	
} // class ClasspathElement 
