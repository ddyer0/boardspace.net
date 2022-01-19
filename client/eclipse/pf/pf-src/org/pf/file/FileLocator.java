// ===========================================================================
// CONTENT  : CLASS FileLocator
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.7 - 03/03/2006
// HISTORY  :
//  17/05/2002  duma  CREATED
//	24/05/2002	duma	added		->	toURL(), isFile(), isDirectory(), getAbsolutePath()
//	21/06/2002	duma	added		->	realFile()
//	14/03/2003	duma	added		->	getStandardizedPath(), getStandardizedAbsolutePath()
//	20/12/2003	duma	added		->	logger() / changed all debug statements to use logger
//	03/09/2004	duma	added		->	support for remote files via http://
//	02/10/2004	duma	added		->	support for autho authentication with remote files
//	03/03/2006	mdu		bugfix	->	convertFromURLSyntax() has cut off leading slash on unix
//
// Copyright (c) 2002-2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.file ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.pf.logging.Logger;
import org.pf.security.authentication.AutoAuthenticationManager;
import org.pf.text.StringFilter;
import org.pf.text.StringPatternCollection;
import org.pf.text.StringUtil;

/**
 * This class mainly supports access to files which can be in the normal
 * file directory structure or inside zip archives.
 * The main purpose is to provide methods that transparently treat files 
 * the same way whether they are in the normal directory structure or
 * inside archives.
 * The syntax is simply to allow archive names in a path name at any place
 * where a sub-directory name can be. <br>
 * Examples: <br>
 * <ul>
 *   <li>d:\temp\archive.zip\config\nls.properties</li>
 *	 <li>/usr/java/jdk1.3/src.jar/java/io/File.java</li>
 *	 <li>jar:http://myserver.com/code/src.jar!/settings.properties</li>
 * </ul>
 * @author Manfred Duchrow
 * @version 1.7
 */
public class FileLocator
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	private static final boolean DEBUG = "true".equals( System.getProperty( "org.pf.file.FileLocator.debug" ) ) ;
	private static final String ARCHIVE_INDICATOR				= "!" + File.separator ;
	private static final String JAR_INDICATOR						= "jar:" ;

	private static final String HTTP_INDICATOR 		= "http://" ;
	private static final String HTTPS_INDICATOR 	= "https://" ;
	private static final String JAR_HTTP_INDICATOR 		= "jar:http://" ;
	private static final String JAR_HTTPS_INDICATOR 	= "jar:https://" ;

	private static final String[] SUPPORTED_REMOTE_PROTOCOLS =
		{ HTTP_INDICATOR, HTTPS_INDICATOR, JAR_HTTP_INDICATOR, JAR_HTTPS_INDICATOR } ;
	
	private static final StringFilter JAR_FILE_INDICATORS = 
		new StringPatternCollection( new String[] { "jar:file:/*", "jar:file:\\*" } ) ;
	
	public static final String TEMP_FILE_PREFIX = "FLOC_" ; 
	public static final String TEMP_FILE_SUFFIX = ".xtr" ; 
	
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private FileLocator parentLocator = null ;
  protected FileLocator getParentLocator() { return parentLocator ; }
  protected void setParentLocator( FileLocator newValue ) { parentLocator = newValue ; }

  private File file = null ;
  protected File getFile() { return file ; }
  protected void setFile( File newValue ) { file = newValue ; }
  
  private ZipFile zipFile = null ;
  protected ZipFile getZipFile() { return zipFile ; }
  protected void setZipFile( ZipFile newValue ) { zipFile = newValue ; }  
   
  private Boolean exists = Boolean.TRUE ;
  protected Boolean getExists() { return exists ; }
  protected void setExists( Boolean newValue ) { exists = newValue ; }
      
  private Exception exception = null ;
  protected Exception getException() { return exception ; }
  protected void setException( Exception newValue ) { exception = newValue ; }
      
  private String originalFileName = null ;
  /**
   * Returns the original (unchanged) filename as it was given to the create 
   * method of this locator.
   */
  public String getOriginalFileName() { return originalFileName ; }
  
  private boolean remote = false ;
  protected boolean getRemote() { return remote ; }
  protected void setRemote( boolean newValue ) { remote = newValue ; }  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================
  /**
   * Create a file locator that corresponds to the given file name.
   */
  public static FileLocator create( File file )
  {
  	FileLocator locator = new FileLocator( file.getPath(), false ) ;
		
		return locator.createFrom( file ) ;
  } // create() 

  // -------------------------------------------------------------------------

  /**
   * Create a file locator that corresponds to the given URL.
   * Allowed remote URLs start with either "http://" or "https://"
   * or "jar:http://" or "jar:https://". 
   * 
   * @param url The url that specifies the file
   */
  public static FileLocator create( URL url )
  {
		return create( url.toString() ) ;
  } // create() 

  // -------------------------------------------------------------------------

  /**
   * Create a file locator that corresponds to the given file name.
   * Remote files are also supported for filenames starting with 
   * "http://" or "https://". 
   * 
   * @param filename The name (or URL) that specifies the file
   */
  public static FileLocator create( String filename )
  {
  	if ( usesSupportedRemoteProtocol( filename ) )
  	{
  		return new FileLocator( filename, true ) ;
  	}
  	if ( JAR_FILE_INDICATORS.matches( filename ) )
		{
			return createFromJarFileUrl( filename ) ;
		}
  	return create( new File( filename ) ) ;
  } // create() 
  
  // -------------------------------------------------------------------------
  
  private static FileLocator createFromJarFileUrl( String filename ) 
	{
		String simpleFilename ;
		FileLocator locator ;
		
		simpleFilename = filename.substring( JAR_INDICATOR.length() ) ;
		simpleFilename = str().replaceAll( simpleFilename, ARCHIVE_INDICATOR, "/" ) ;
		locator = create( new File( simpleFilename ) ) ;
		locator.setOriginalFileName( filename ) ;
		return locator ;
	} // createFromJarFileUrl() 
	
	// -------------------------------------------------------------------------
  
  private static FileLocator newWith( FileLocator aParent, String[] pathElements )
		throws Exception
  {
  	FileLocator locator = new FileLocator() ;
		
		return locator.createFrom( aParent, pathElements ) ;
  } // newWith() 

  // -------------------------------------------------------------------------

	protected static boolean usesSupportedRemoteProtocol( String filename ) 
	{
		if ( filename == null )
			return false ;
		
		for (int i = 0; i < SUPPORTED_REMOTE_PROTOCOLS.length; i++ )
		{
			if ( filename.startsWith( SUPPORTED_REMOTE_PROTOCOLS[i] ) )
				return true ;
		}
		return false ;
	} // usesSupportedRemoteProtocol() 

	// -------------------------------------------------------------------------
	
	protected static StringUtil str()
	{
		return StringUtil.current() ;
	} // str() 
	
  // -------------------------------------------------------------------------

	protected static FileUtil fileUtil()
	{
		return FileUtil.current() ;
	} // fileUtil() 
	
  // -------------------------------------------------------------------------

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  FileLocator()
  {
    super() ;
  } // FileLocator() 

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with default values.
   */
  private FileLocator( String filename, boolean remoteFile )
  {
    this() ;
    this.setRemote( remoteFile ) ;
    if ( remoteFile )
		{
			this.setExists( null ) ;
		}
    this.setOriginalFileName( filename ) ;
  } // FileLocator() 

  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

	/**
	 * Returns the file that contains the data the locator points to.
	 * If the locator points to a normal file in a directory, than
	 * this file will be returned.
	 * If the locator points to a file inside an archive, the file
	 * will be unzipped into the <i><b>temp</i></b> directory and this
	 * temp file will be returned.
	 * If the locator points to a remote file it will be downloaded to the
	 * <i><b>temp</i></b> directory and this temp file will be returned. 
	 * If the locator points to a none existing file, this method 
	 * returns false.
	 */
	public File realFile()
	{
		File aFile ;
		
		if ( ! this.exists() ) // Also ensures download of any remote file
			return null ;
		
		try
		{
			aFile = this.fileRef() ;
		}
		catch (Throwable e)
		{
			aFile = null ;
		}
		return aFile ;
	} // realFile() 

  // -------------------------------------------------------------------------

	/**
	 * Returns whether or not the file specified by this locator exists.
	 * If the locator points to a remote file, it will be downloaded when
	 * calling this method.
	 */
	public boolean exists()
	{
		if ( this.isUnlocatedRemoteFile() ) // Not yet checked
		{
			this.copyFromRemote() ;
			if ( this.getExists() == null )
				return false ;
		}
		return this.getExists().booleanValue() ;
	} // exists() 

  // -------------------------------------------------------------------------

	/**
	 * Returns whether or not the name specified by this locator 
	 * points to a file.
	 */
	public boolean isFile()
	{
		try 
		{
			if ( this.exists() )
				return this.isFileElement( this.getFile() ) ;
			else
				return false ;
		} 
		catch(Exception e) 
		{
			return false ;
		}
	} // isFile() 

  // -------------------------------------------------------------------------

	/**
	 * Returns whether or not the name specified by this locator 
	 * points to a directory.
	 */
	public boolean isDirectory()
	{
		try 
		{
			if ( this.exists() )
				return ! this.isFileElement( this.getFile() ) ;
			else
				return false ;
		} 
		catch(Exception e) 
		{
			return false ;
		}
	} // isDirectory() 

  // -------------------------------------------------------------------------

	/**
	 * Returns true if the locator points to a remote file or directory.
	 */
	public boolean isRemote() 
	{
		return this.getRemote() ;
	} // isRemote() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns the size of the file or 0 if it does not exist.
	 */
	public long size()
	{
		ZipEntry entry ;
		
		this.exists() ;
		
		try 
		{
			if ( this.isInArchive() )
			{
				entry = this.archiveEntry() ;
				// if ( DEBUG ) com.mdcs.joi.Inspector.inspectWait( entry ) ;
				return entry.getSize() ;
			}
			else
			{
				return this.getFile().length() ;
			} 
		}
		catch(Exception ex) 
		{
			if ( DEBUG ) this.logger().logException(ex) ;
			return 0L ;
		} 
	} // size() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the timestamp of when the file was last modified 
	 * or 0 in any case of error.
	 */
	public long lastModified()
	{
		ZipEntry entry ;
		
		this.exists() ;
		
		try 
		{
			if ( this.isInArchive() )
			{
				entry = this.archiveEntry() ;
				return entry.getTime() ;
			}
			else
			{
				return this.getFile().lastModified() ;
			} 
		}
		catch(Exception ex) 
		{
			if ( DEBUG ) this.logger().logException(ex) ;
			return 0L ;
		} 
	} // lastModified() 

  // -------------------------------------------------------------------------

	/**
	 * Returns an opened input stream on the file defined by this locator.
	 */
	public InputStream getInputStream()
		throws IOException
	{
		ZipEntry entry ;
		
		if ( ! this.exists() )
		{
			throw new FileNotFoundException( this.getAbsolutePath() ) ;
		}
		
		if ( this.isInArchive() )
		{
			entry = this.archiveEntry() ;
			return this.container().getInputStream( entry ) ;
		}
		else
		{
			return new FileInputStream( this.getFile() ) ;
		} 
	} // getInputStream() 

  // -------------------------------------------------------------------------

  /**
   * Returns the parent of the file represented by this locator
   */
  public FileLocator getParent() 
  { 
  	String path ;
  	
  	path = this.str().cutTail( this.getStandardizedAbsolutePath(), "/" ) ;
  	return FileLocator.create( path ) ;
  } // getParent() 

  // -------------------------------------------------------------------------
	
	/**
	 * Returns whether or not the file specified by this locator 
	 * is inside an archive.
	 */
	public boolean isInArchive()
	{
		return this.getParentLocator() != null ;
	} // isInArchive() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the full pathname.
	 */
	public String getPath()
	{
		return this.fullFilePath( false ).getPath() ;
	} // getPath() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the full absolute pathname.
	 */
	public String getAbsolutePath()
	{
		if ( this.isRemote() )
		{
			return this.getOriginalFileName() ;
		}
		return this.fullFilePath( true ).getPath() ;
	} // getAbsolutePath() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the full pathname in a standardized for.
	 * That is all ".." and "." elements are removed and forward slashes are 
	 * used as separators of the remaining elements.
	 */
	public String getStandardizedPath()
	{
		return this.fileUtil().standardize( this.getPath() ) ;
	} // getStandardizedPath() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the full absolute pathname in a standardized form.
	 * That is all ".." and "." elements are removed and forward slashes are 
	 * used as separators of the remaining elements.
	 */
	public String getStandardizedAbsolutePath()
	{
		return this.fileUtil().standardize( this.getAbsolutePath() ) ;
	} // getStandardizedAbsolutePath() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the last exception that occured while using this locator
	 * or null, if no exception was thrown at all.
	 */
	public Exception exception()
	{
		return this.getException() ;
	} // exception() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the name of the file as an URL.
	 */
	public URL toURL()
		throws MalformedURLException
	{
		StringBuffer buffer = new StringBuffer( 128 ) ;
		
		this.urlPath( buffer ) ;
		return new URL( buffer.toString() ) ;
	} // toURL() 

  // -------------------------------------------------------------------------

	public String toString()
	{
		return this.getStandardizedAbsolutePath() ;
	} // toString()
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

	protected FileLocator createFrom( File filePath )
	{
		FileLocator locator	= null ;
		String[] parts 			= null ;
		File path						= filePath ;
		
		if ( this.fileUtil().isLocalFileURL( path.getPath() ) )
		{
			path = this.convertFromURLSyntax( path ) ;
		}
			
		parts = str().parts( path.getPath(), File.separator ) ;
		try
		{
		 	locator = this.initFromPath( parts, path.getPath().startsWith( File.separator ) ) ;
		}
		catch ( Exception ex )
		{
			this.setException( ex ) ;
			this.doesNotExist( path ) ;
			locator = this ;
		}
		return locator ;
	} // createFrom() 

  // -------------------------------------------------------------------------

  private FileLocator createFrom( FileLocator aParent, String[] pathElements )
		throws Exception
  {
  	this.setParentLocator( aParent ) ;
  	return this.initFromPath( pathElements, false ) ;
  } // createFrom() 

  // -------------------------------------------------------------------------

	protected FileLocator initFromPath( String[] parts, boolean startsFromRoot )
		throws Exception
	{
		FileLocator locator			= this ;
		File pathElement 				= null ;
		String[] rest						= null ;
		boolean elementExists		= false ;
		String originalName ;
		
		if ( startsFromRoot )
			pathElement = new File( File.separator ) ;
		
		for ( int i = 0 ; i < parts.length ; i++ )
		{
			if ( pathElement == null )
				pathElement = new File( parts[i] ) ;
			else
				pathElement = new File( pathElement, parts[i] ) ;

			elementExists = this.doesElementExist( pathElement ) ;
			
			if ( elementExists )
			{	
				this.setFile( pathElement ) ;
				if ( this.isFileElement( pathElement ) )
				{
					if ( DEBUG ) this.logger().logDebug( "Locator(" + pathElement + ")" ) ;
					if ( i < ( parts.length - 1 ) )  // Is not last element ? 
					{
						originalName = this.getOriginalFileName() ;
						this.setOriginalFileName( pathElement.getPath() ) ;
						rest = str().copyFrom( parts, i + 1 ) ;
						locator = FileLocator.newWith( this, rest ) ;
						locator.setOriginalFileName( originalName ) ;
					}
					break ;
				}
			}
			else
			{
				if ( this.isInArchive() )
				{
					if ( i < ( parts.length - 1 ) )  // Is not last element ? 
					{
						// Directories are not always identifiable individually in zip archives.
						// Therefore it must be accepted that they are not found.
						// So in such case no exception will be thrown.
					}
					else
					{
						throw new Exception( "\"" + pathElement.getPath() + "\" does not exist" );
					}
				}
				else
				{
					throw new Exception( "\"" + pathElement.getPath() + "\" does not exist" );
				}
			}
		}
		return locator ;
	} // initFromPath() 

  // -------------------------------------------------------------------------

	protected boolean doesElementExist( File element )
		throws Exception
	{
		if ( this.isInArchive() )
		{
			return doesElementExistInArchive( element.getPath() ) ;			
		}
		else
		{
			return element.exists() ;
		}
	} // doesElementExist() 

  // -------------------------------------------------------------------------

	protected boolean isFileElement( File element )
		throws Exception
	{
		if ( this.isInArchive() )
		{
			return isFileInArchive( element.getPath() ) ;			
		}
		else
		{
			return element.isFile() ;
		}
	} // isFileElement() 

  // -------------------------------------------------------------------------

	protected boolean doesElementExistInArchive( String elementName )
		throws Exception
	{
		ZipEntry entry ;

		entry = this.entryFromArchive( elementName ) ;
				
		return ( entry != null ) ;
	} // doesElementExistInArchive() 

  // -------------------------------------------------------------------------

	protected boolean isFileInArchive( String elementName )
		throws Exception
	{
		ZipEntry entry ;
		entry = this.entryFromArchive( elementName ) ;
		
		// Unfortunately entry.isDirectory() returns false even for
		// pure directory entries inside a zip archive, so it can't be used here.
		// The trick below is problematic, because apart from 
		// directories it will also not recognize files with size 0.
		
		return ( entry != null ) && ( entry.getSize() > 0 ) ;
	} // isFileInArchive() 

  // -------------------------------------------------------------------------

	protected ZipEntry entryFromArchive( String elementName )
		throws IOException
	{
		ZipEntry entry ;
		ZipFile archive ;
		String name ;
		
		name = str().replaceAll( elementName, "\\", "/" ) ;
		archive = this.container() ;
		entry = archive.getEntry( name ) ;

		if (DEBUG)
		{
			StringBuffer buffer = new StringBuffer(100) ;
			// if ( entry == null ) com.mdcs.joi.Inspector.inspect( name ) ;
			buffer.append( archive.getName() + "::" + name + " --- "
								+ ( entry != null ) ) ; 
			if ( entry == null )
			{
				buffer.append( "\n" ) ;				
			}
			else
			{
				buffer.append( " (" + entry.getSize()  + ")" ) ;
				buffer.append( " (T:" + entry.getTime()  + ")" ) ;
				buffer.append( " (" + ( entry.isDirectory() ? "Dir" : "File" ) + ")\n" ) ;
			}
			this.logger().logDebug( buffer.toString() ) ;
		}
		
		return entry ;
	} // entryFromArchive() 

  // -------------------------------------------------------------------------

	protected ZipEntry archiveEntry()
		throws IOException
	{
		return this.entryFromArchive( this.getFile().getPath() ) ;
	} // archiveEntry() 

  // -------------------------------------------------------------------------

	protected void doesNotExist( File aFile )
	{
		this.setExists( Boolean.FALSE ) ;
		this.setFile( aFile ) ;
	} // doesNotExist() 

  // -------------------------------------------------------------------------

	protected File fullFilePath( boolean absolute )
	{
		File full ;
		
		if ( ! this.exists() && this.isRemote() )
			return null ;
		
		if ( this.isInArchive() )
		{
			full = new File( 	this.getParentLocator().fullFilePath( absolute ), 
												this.getFile().getPath() ) ;
		}
		else
		{
			if ( absolute )
				full = this.getFile().getAbsoluteFile() ;
			else
				full = this.getFile() ;
		}
		
		return full ;
	} // fullFilePath() 

  // -------------------------------------------------------------------------

	protected void urlPath( StringBuffer buffer )
	{
		if ( this.isRemote() )
		{
			buffer.append( this.getOriginalFileName() ) ;
		}
		else
		{
			if ( this.isInArchive() )
			{
				this.getParentLocator().urlPath( buffer ) ; 
				buffer.append( ARCHIVE_INDICATOR ) ;
				buffer.append( this.getFile().getPath() ) ;
			}
			else
			{
				buffer.append( this.fileUtil().convertToURLSyntax( this.getFile().getPath() ) ) ;
			}		
		}
	} // urlPath() 

  // -------------------------------------------------------------------------

	protected File fileRef()
		throws IOException
	{
		InputStream archiveStream ;
		ZipEntry entry ;
		File tempFile ;
		
		if ( this.isInArchive() )
		{
			entry = this.archiveEntry() ;
			archiveStream = this.container().getInputStream( entry ) ;
			tempFile = fileUtil().copyToTempFile( archiveStream, 
																				TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX ) ;
			return tempFile ;
		}
		else
		{
			return this.getFile() ;
		}
	} // fileRef() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the file this locator presents as opened zip file or
	 * null in any case of error.
	 */
	protected ZipFile archive()
		throws IOException
	{
		if ( this.getZipFile() == null )
		{
			this.setZipFile( new ZipFile( this.fileRef() ) ) ;
		}
		return this.getZipFile() ;
	} // archive() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the zip file which is presented by the parent container
	 * or null in any case of error.
	 */
	protected ZipFile container()
		throws IOException
	{
		if ( this.isInArchive() )
			return this.getParentLocator().archive() ;
		else
			return null ;
	} // container() 

  // -------------------------------------------------------------------------

	protected File convertFromURLSyntax( File aFile )
	{
		String newStr ;
		
		newStr = this.fileUtil().convertFromURLSyntax( aFile.getPath() ) ;
		newStr = str().replaceAll( newStr, ARCHIVE_INDICATOR, File.separator ) ;
		
		return new File( newStr ) ;
	} // convertFromURLSyntax() 
	
  // -------------------------------------------------------------------------

	protected boolean isUnlocatedRemoteFile() 
	{
		return ( this.getExists() == null ) ;
	} // isUnlocatedRemoteFile() 

	// -------------------------------------------------------------------------
	
	/**
	 * This method must only be called if the original filename contains a
	 * URL starting with one of the supported remote protocols!
	 */
	protected void copyFromRemote() 
	{
		File tempFile ;
		
		if ( this.getOriginalFileName() == null ) // No filename available
			return ;
		
		try
		{
			AutoAuthenticationManager.instance().aboutToAccess( this.getOriginalFileName() ) ;
			tempFile = this.fileUtil().copyToTempFile( this.getOriginalFileName(),
																	TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX, false ) ;
		}
		catch ( IOException e )
		{
			this.setException( e ) ;
			if ( DEBUG ) this.logger().logException(e) ;
			return ;
		}
		if ( tempFile != null )
		{
			this.setExists( tempFile.exists() ? Boolean.TRUE : Boolean.FALSE ) ;
			this.setFile( tempFile ) ;
		}
	} // copyFromRemote() 

	// -------------------------------------------------------------------------
	
  protected void setOriginalFileName( String newValue ) 
  { 
  	originalFileName = this.fileUtil().standardize( newValue ) ; 
  } // setOriginalFileName() 
  
  // -------------------------------------------------------------------------
  
	protected Logger logger()
	{
		return LoggerProvider.getLogger() ;
	} // logger() 

	// -------------------------------------------------------------------------	
		
} // class FileLocator 
