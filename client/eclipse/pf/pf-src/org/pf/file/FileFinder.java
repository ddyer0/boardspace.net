// ===========================================================================
// CONTENT  : CLASS FileFinder
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.7 - 06/06/2006
// HISTORY  :
//  02/12/2001  duma  CREATED
//	23/01/2002	duma	added		-> findFile()
//	14/02/2003	duma	added		-> 3 File[] findFiles() methods
//	06/11/2003	mdu		added		->	DEBUG
//	07/05/2004	mdu		added		->	locateFileOnPath(), locateFileOnClasspath()
//	24/12/2004	mdu		added		->	fineFiles(String), findFiles(String,char)
//	06/06/2006	mdu		added		->	findDirectories(...)
//
// Copyright (c) 2001-2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.file;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.pf.logging.Logger;
import org.pf.text.StringUtil;

/**
 * Helper class with convenient methods to find files.
 *
 * @author Manfred Duchrow
 * @version 1.7
 */
public class FileFinder implements FileHandler
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	private static final boolean DEBUG = "true".equals( System.getProperty("org.pf.file.FileFinder.debug") ) ;
	private static final String DEBUG_PREFIX = "org.pf.file.FileFinder: " ;

	private static final char WIN_PATH_SEP		= ';' ;
	private static final char UNIX_PATH_SEP		= ':' ;

	private static final char NO_DIGIT_WILDCARD		= (char)0 ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private List collectedFiles = null ;
  protected List getCollectedFiles() { return collectedFiles ; }
  protected void setCollectedFiles( List newValue ) { collectedFiles = newValue ; }
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  protected FileFinder()
  {
  	super() ;
  	this.setCollectedFiles( new ArrayList(100) ) ;
  } // FileFinder() 
  
  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC CLASS METHODS
  // =========================================================================
  /**
   * Tries to find the file with the given Name.
   * First it checks if the file exists directly under the given name in the 
   * current work directory.
   * If not, it searches the classpath to find it.
   * If the file was found and really exists, then its URL will be returned.
   * In all other cases null will be returned.
   */
  public static URL locateFile( String filename ) 
  {
  	File aFile ;
  	
  	aFile = new File( filename ) ;
  	if ( fileExists( aFile ) )
  	{
  		try
			{
  			return  new URL( FileUtil.current().convertToURLSyntax( aFile.getAbsolutePath() ) ) ;
				// return aFile.toURL() ;
			}
			catch ( MalformedURLException e )
			{
				// Ignore and try on classpath instead
			}
  	}
  	return locateFileOnClasspath( filename ) ;
  } // locateFile() 
   
  // -------------------------------------------------------------------------

  /**
   * Tries to find the file with the given Name on the classpath.
   * If the file was found its URL will be returned.
   * In all other cases null will be returned.
   * 
   * @param filename The name of the file to look for
   */
  public static URL locateFileOnClasspath( String filename ) 
  {
    ClassLoader cl 							= null ;
    URL url											= null ;

    try
    {
      cl = FileFinder.class.getClassLoader() ;
      if ( cl == null )
      {
      	if ( DEBUG )
				{
					debug( "No classloader found !\n<P>" ) ;
				}
        return null ;
      }
      url = cl.getResource( filename ) ;
      if ( url == null )
      {
      	if ( DEBUG )
      	{
          debug( "File '" + filename + "' not found on CLASSPATH !!!" ) ;
      	}
      	url = locateFileOnPath( filename, FileUtil.current().getClasspath() ) ;
      }
    }
    catch ( Exception ex )
    {
    	if ( DEBUG )
    	{
        logger().logException(ex) ;
    	}
    }
    return url ;
  } // locateFileOnClasspath() 

  // -------------------------------------------------------------------------
  
  /**
   * Tries to find the file with the given Name on the classpath.
   * If the file was found and really exists, then it will be returned.
   * In all other cases null will be returned.
   */
  public static File findFileOnClasspath( String filename ) 
  {
    File file										= null ;
    URL url											= null ;

    url = locateFileOnClasspath( filename ) ;
    if ( url == null )
    {
    	if ( DEBUG )
    	{
        debug( "File '" + filename + "' not found on CLASSPATH !!!" ) ;
    	}
    }
    else
    {
      file = new File( url.getFile() ) ;
      if ( ! fileExists( file ) )
        file = null ;
    }
    return file ;
  } // findFileOnClasspath() 

  // -------------------------------------------------------------------------

	/**
	 * Tries to find the file with the given name in one location of the given
	 * location path. The location path is like the Java classpath a concatenation
	 * of file directories and archives. The separator could be ':' or ';' 
	 * depending on current platform.
	 * 
	 * @param filename The name of the file to look for
	 * @param path The path to look up for the file (e.g. "xxx.jar:test/lib:root.jar")
	 */
	public static URL locateFileOnPath( String filename, String path )
	{
		String searchPath ;
		char separator ;
		Classpath classpath ;
		
		if ( ( filename == null ) || ( path == null ) )
			return null ;
			
		separator = ( File.pathSeparatorChar == WIN_PATH_SEP ) ? UNIX_PATH_SEP : WIN_PATH_SEP ;
		searchPath = path.replace( separator, File.pathSeparatorChar ) ;
		classpath = new Classpath( searchPath ) ;
		return locateFileOnPath( filename, classpath ) ;
	} // locateFileOnPath() 

	// -------------------------------------------------------------------------

	/**
	 * Tries to find the file with the given name in one location of the given
	 * location path. The location path is like the Java classpath a concatenation
	 * of file directories and archives. The separator could be ':' or ';' 
	 * depending on current platform.
	 * 
	 * @param filename The name of the file to look for
	 * @param path The path to look up for the file (e.g. "xxx.jar:test/lib:root.jar")
	 */
	public static URL locateFileOnPath( String filename, Classpath path )
	{
		ClasspathElement cpElement ;
		URL url ;
		
		if ( ( filename == null ) || ( path == null ) )
		{
			return null ;
		}
		
		cpElement = path.firstElementContaining( filename ) ;
		
		if ( cpElement == null )
		{
			return null ;
		}
		url = cpElement.createURL( filename ) ;
		if ( url == null )
		{
			return null ;
		}
		return url ;
	} // locateFileOnPath() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Tries to find the file with the given name in one location of the given
	 * location path. The location path is like the Java classpath a concatenation
	 * of file directories and archives. The separator could be ':' or ';'.
	 */
	public static File findFileOnPath( String filename, String path )
	{
		URL url ;
		String name ;

		url = locateFileOnPath( filename, path ) ;
		if ( url == null )
			return null ;
		
		name = url.getFile() ;
		name = str().cutHead( name, "file:/" ) ;
		if ( name.startsWith( "/") )
			name = str().cutHead( name, "/" ) ;
		
		return new File( name ) ;
	} // findFileOnPath() 

	// -------------------------------------------------------------------------

  /**
   * Tries to find the file with the given Name.
   * First it looks if the file exists directly under the given name.
   * If not, it searches the classpath to find it.
   * If the file was found and really exists, then it will be returned.
   * In all other cases null will be returned.
   */
  public static File findFile( String filename ) 
  {
  	File aFile	= null ;
  	
  	aFile = new File( filename ) ;
  	if ( fileExists( aFile ) )
  		return aFile ;
  	
  	aFile = findFileOnClasspath( filename ) ;
  	
  	return aFile ;
  } // findFile() 
   
  // -------------------------------------------------------------------------

	/**
	 * Return all files that match the given pattern(s) start searching in the
	 * specified dir. Searches in all sub directories as well.
	 * More than one pattern can be specified in parameter <i>pattern</i>.
	 * They have to be separated by ';'.
	 * 
	 * @param dir The directory to start searching (must not be null)
	 * @param pattern The pattern(s) the filenames must match (must not be null )
	 * @return All file found that matched to at least one of the patterns
	 * @throws IllegalArgumentException If <i>dir</i> or <i>pattern</i> is null
	 */
	public static File[] findFiles( String dir, String pattern )
	{
		return findFiles( dir, pattern, true ) ;
	} // findFiles() 

	// -------------------------------------------------------------------------

	/**
	 * Return all files that match the given pattern(s) start searching in the
	 * specified dir. Look into sub directories if <i>recursive</i> is true.
	 * More than one pattern can be specified in parameter <i>pattern</i>.
	 * They have to be separated by ';'.
	 * 
	 * @param dir The directory to start searching (must not be null)
	 * @param pattern The pattern(s) the filenames must match (must not be null )
	 * @param recursive If false, only <i>dir</i> is searched, otherwise all sub directories as well
	 * @return All file found that matched to at least one of the patterns
	 * @throws IllegalArgumentException If <i>dir</i> or <i>pattern</i> is null
	 */
	public static File[] findFiles( String dir, String pattern, boolean recursive )
	{
		return findFiles( dir, pattern, recursive, NO_DIGIT_WILDCARD ) ;
	} // findFiles() 

	// -------------------------------------------------------------------------

	/**
	 * Return all files that match the given pattern(s) start searching in the
	 * specified dir. Look into sub directories if <i>recursive</i> is true.
	 * Use the given digit wildcard in patterns to match single digits in 
	 * filenames.<br>
	 * More than one pattern can be specified in parameter <i>pattern</i>.
	 * They have to be separated by ';'.
	 * 
	 * @param dir The directory to start searching (must not be null)
	 * @param pattern The pattern(s) the filenames must match (must not be null )
	 * @param recursive If false, only <i>dir</i> is searched, otherwise all sub directories as well
	 * @param digitWildcard The wildcard character for digit representation in the pattern(s)
	 * @return All file found that matched to at least one of the patterns
	 * @throws IllegalArgumentException If <i>dir</i> or <i>pattern</i> is null
	 */
	public static File[] findFiles( String dir, String pattern, boolean recursive,
																	char digitWildcard )
	{
		FileFinder finder ;
		Character digitChar = null ;
		
		if ( dir == null )
			throw new IllegalArgumentException( "FileFinder.findFiles(): dir is null" ) ;

		if ( pattern == null )
			throw new IllegalArgumentException( "FileFinder.findFiles(): pattern is null" ) ;
		
		if ( digitWildcard != NO_DIGIT_WILDCARD )
		{
			digitChar = Character.valueOf(digitWildcard) ;
		}
		
		finder = new FileFinder() ;
		return finder.collectFiles( dir, pattern, recursive, digitChar ) ;
	} // findFiles() 

	// -------------------------------------------------------------------------

	/**
	 * Return all files that match the given search pattern.
	 * The pattern may contain '**' as last path element to indicate a recursive
	 * search.
	 * <br>
	 * Example: "/usr/test\/**\/*.gz"
	 * 
	 * @param searchPattern The pattern containing path and filename pattern the files must match (must not be null )
	 * @return All files found that matched the pattern
	 * @throws IllegalArgumentException If <i>searchPattern</i> is null
	 */
	public static File[] findFiles( String searchPattern )
	{
		return findFiles( searchPattern, NO_DIGIT_WILDCARD ) ;
	} // findFiles() 

	// -------------------------------------------------------------------------

	/**
	 * Return all files that match the given search pattern.
	 * The pattern may contain '**' as last path element to indicate a recursive
	 * search.
	 * <br>
	 * Example: "/usr/test\/**\/*.gz"
	 * 
	 * @param searchPattern The pattern containing path and filename pattern the files must match (must not be null )
	 * @param digitWildcard The wildcard character for digit representation in the pattern(s)
	 * @return All files found that matched the pattern
	 * @throws IllegalArgumentException If <i>searchPattern</i> is null
	 */
	public static File[] findFiles( String searchPattern, char digitWildcard )
	{
		FileFinder finder ;
		Character digitChar = null ;
		
		if ( searchPattern == null )
			throw new IllegalArgumentException( "FileFinder.findFiles(): searchPattern is null" ) ;

		if ( digitWildcard != NO_DIGIT_WILDCARD )
		{
			digitChar = Character.valueOf(digitWildcard) ;
		}
		
		finder = new FileFinder() ;
		return finder.collectFiles( searchPattern, digitChar ) ;
	} // findFiles() 

	// -------------------------------------------------------------------------

	/**
	 * Return all directories that match the given pattern(s) start searching in the
	 * specified startDir. Look into each sub directories if <i>recursive</i> is true.
	 * Use the given digit wildcard in patterns to match single digits in 
	 * filenames.<br>
	 * More than one pattern can be specified in parameter <i>pattern</i>.
	 * They have to be separated by ';'.
	 * 
	 * @param startDir The directory to start searching (must not be null)
	 * @param pattern The pattern(s) the directorynames must match (must not be null )
	 * @param recursive If false, only <i>startDir</i> is searched, otherwise all sub directories as well
	 * @param digitWildcard The wildcard character for digit representation in the pattern(s)
	 * @return All directories found that matched to at least one of the patterns
	 * @throws IllegalArgumentException If <i>dir</i> or <i>pattern</i> is null
	 */
	public static File[] findDirectories( String startDir, String pattern, boolean recursive,
																				char digitWildcard )
	{
		FileFinder finder ;
		Character digitChar = null ;
		
		if ( startDir == null )
			throw new IllegalArgumentException( "FileFinder.findDirectories(): dir is null" ) ;

		if ( pattern == null )
			throw new IllegalArgumentException( "FileFinder.findDirectories(): pattern is null" ) ;
		
		if ( digitWildcard != NO_DIGIT_WILDCARD )
		{
			digitChar = Character.valueOf(digitWildcard) ;
		}
		
		finder = new FileFinder() ;
		return finder.collectDirectories( startDir, pattern, recursive, digitChar ) ;
	} // findDirectories() 

	// -------------------------------------------------------------------------

	/**
	 * Return all directories that match the given pattern(s) start searching in the
	 * specified startDir. Look into each sub directories if <i>recursive</i> is true.
	 * More than one pattern can be specified in parameter <i>pattern</i>.
	 * They have to be separated by ';'.
	 * 
	 * @param startDir The directory to start searching (must not be null)
	 * @param pattern The pattern(s) the directorynames must match (must not be null )
	 * @param recursive If false, only <i>startDir</i> is searched, otherwise all sub directories as well
	 * @return All directories found that matched to at least one of the patterns
	 * @throws IllegalArgumentException If <i>dir</i> or <i>pattern</i> is null
	 */
	public static File[] findDirectories( String startDir, String pattern, boolean recursive )
	{
		return findDirectories( startDir, pattern, recursive, NO_DIGIT_WILDCARD ) ;
	} // findDirectories() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Return all directories that match the given pattern(s) start searching in the
	 * specified startDir. This method also searches in each sub directory.
	 * More than one pattern can be specified in parameter <i>pattern</i>.
	 * They have to be separated by ';'.
	 * 
	 * @param startDir The directory to start searching (must not be null)
	 * @param pattern The pattern(s) the directorynames must match (must not be null )
	 * @return All directories found that matched to at least one of the patterns
	 * @throws IllegalArgumentException If <i>dir</i> or <i>pattern</i> is null
	 */
	public static File[] findDirectories( String startDir, String pattern )
	{
		return findDirectories( startDir, pattern, true, NO_DIGIT_WILDCARD ) ;
	} // findDirectories() 
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PRIVATE CLASS METHODS
  // =========================================================================

	private static boolean fileExists( File file )
	{
		boolean success = false ;
		if ( file != null )
		{
			try
			{
				FileLocator locator = FileLocator.create( file ) ;
				success = locator.exists() ;
			}
			catch ( Exception ex )
			{
				// nothing to do here
			}
		}
		return success ;
	} // fileExists() 

  // -------------------------------------------------------------------------

	private static Logger logger()
	{
		return LoggerProvider.getLogger() ;
	} // logger() 

	// -------------------------------------------------------------------------	
		
	private static StringUtil str()
	{
		return StringUtil.current() ;
	} // str() 

	// -------------------------------------------------------------------------

	private static void debug( String text )
	{
		logger().logDebug( DEBUG_PREFIX + text ) ;	
	} // debug() 

	// -------------------------------------------------------------------------
		
  // =========================================================================
  // INTERFACE FileHandler METHODS
  // =========================================================================
	/**
	 * This method is called for each file, that a FileWalker instance finds.
	 * It must return true, if the FileWalker should continue. To stop the
	 * calling FileWalker it can return false.
	 *
	 * @param file The file, currently found by the FileWalker instance
	 * @return true to continue, false to terminate processing of files
	 */
	public boolean handleFile( File file ) 
	{
		this.getCollectedFiles().add( file ) ;			
		return true ;
	} // handleFile() 

  // -------------------------------------------------------------------------

	/**
	 * This method is called for whenever an exception occurs in walking through
	 * the directories.   <br>
	 * The method must return true, if the FileWalker should continue. To stop the
	 * calling FileWalker it can return false.
	 *
	 * @param ex The exception to handle
	 * @param file The file, currently found by the FileWalker instance
	 * @return true to continue, false to terminate processing of files
	 */
	public boolean handleException( Exception ex, File file )
	{
		if ( DEBUG )
		{
			this.debug( "Problem with file '" + file + "'" ) ;
			this.debug( ex.toString() ) ;
		}
		return false ;
	} // handleException() 

  // -------------------------------------------------------------------------
  
	/**
	 * This method is called for each directory, that a FileWalker finished to walk through.
	 * It must return true, if the FileWalker should continue. To stop the
	 * calling FileWalker it can return false.
	 *
	 * @param dir The directory, the FileWalker has finished to walk through
	 * @return true to continue, false to terminate processing of files
	 */
	public boolean directoryEnd( File dir )
	{
		return true ;
	} // directoryEnd() 
  
  // -------------------------------------------------------------------------
  
  /**
	 * This method is called for each directory, that a FileWalker starts to walk through.
	 * It must return true, if the FileWalker should continue. To stop the
	 * calling FileWalker it can return false.
	 *
	 * @param dir The directory, the FileWalker is starting to walk through
	 * @param count The number of files and directories the FileWalker found in the directory
	 * @return true to continue, false to terminate processing of files
	 */
	public boolean directoryStart( File dir, int count )
	{
		return true ;
	} // directoryStart() 

  // -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected File[] collectDirectories( String dir, String pattern, boolean recursive, 
																	Character digitWildcard )
	{
		FileWalker fileWalker ;
		ExtendedFileFilter filter ;
		
		fileWalker = new FileWalker( this ) ;
		if ( digitWildcard != null )
		{
			fileWalker.setDigitWildcardChar( digitWildcard.charValue() ) ;
		}
		filter = new ExtendedFileFilter(pattern) ;
		filter.setRestrictiveMode(false) ;
		fileWalker.walkThroughDirectories( dir, filter, recursive ) ;
		return getCollectedFilesArray() ;
	} // collectDirectories() 

	// -------------------------------------------------------------------------

	protected File[] collectFiles( String dir, String pattern, boolean recursive, 
			Character digitWildcard )
	{
		FileWalker fileWalker ;
		
		fileWalker = new FileWalker( this ) ;
		if ( digitWildcard != null )
		{
			fileWalker.setDigitWildcardChar( digitWildcard.charValue() ) ;
		}
		
		fileWalker.walkThrough( dir, pattern, recursive ) ;
		return getCollectedFilesArray() ;
	} // collectFiles() 
	
	// -------------------------------------------------------------------------
	
	// The following method is nearly the same code as the one above.
	// Do not change it to call the method above anyway, because this method
	// the walkThrough() methodt which determines the recursive option 
	// from the given string (e.g. path/**/file)
	protected File[] collectFiles( String pattern, Character digitWildcard )
	{
		FileWalker fileWalker;

		fileWalker = new FileWalker( this );
		if ( digitWildcard != null )
			fileWalker.setDigitWildcardChar( digitWildcard.charValue() );

		fileWalker.walkThrough( pattern );
		return getCollectedFilesArray() ;
	} // collectFiles() 

	// -------------------------------------------------------------------------

	protected File[] getCollectedFilesArray() 
	{
		List list;
		list = this.getCollectedFiles();
		return (File[]) list.toArray( new File[list.size()] );
	} // getCollectedFilesArray() 

	// -------------------------------------------------------------------------
	
} // class FileFinder 
