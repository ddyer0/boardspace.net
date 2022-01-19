// ===========================================================================
// CONTENT  : CLASS FileDirectoryScannerFileHandler
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 20/12/2003
// HISTORY  :
//  16/08/2003  mdu  	CREATED
//	20/12/2003	mdu		changed	-->	Use LoggerProvider in handleException()
//
// Copyright (c) 2003, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.file ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Stack;

/**
 * This is a helper class for FileDirectoryScanner which implements the
 * FileHandler interface and collects the data provided by a FileWalker.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
class FileDirectoryScannerFileHandler implements FileHandler
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private Stack dirStack = null ;
	protected Stack getDirStack() { return dirStack ; }
	protected void setDirStack( Stack newValue ) { dirStack = newValue ; }

	private TableOfContents toc = null ;
	protected TableOfContents toc() { return toc ; }
	protected void toc( TableOfContents newValue ) { toc = newValue ; }
	
	private int baseDirLength = 0 ;
	protected int baseDirLength() { return baseDirLength ; }
	protected void baseDirLength( int newValue ) { baseDirLength = newValue ; }	
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   * @throws IOException If the start directory doesn't exist or is no directory
   */
  protected FileDirectoryScannerFileHandler( String baseDir )
  	throws IOException
  {
    super() ;
    this.initialize( baseDir ) ;
  } // FileDirectoryScannerFileHandler()
    
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
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
		DirectoryContents dirContents ;
		String dirName ;

		dirName = dir.getAbsolutePath() ;
		dirName = this.normalizeDirName( dirName ) ;
		if ( dirName.length() > this.baseDirLength() )
		{
			dirName = dirName.substring( this.baseDirLength() ) ;
		}
		else
		{
			dirName = "" ;
		}

		dirContents = new DirectoryContents( dirName ) ;
		this.toc().add( dirContents ) ;
		this.setCurrentDirContents( dirContents ) ;

		return true ;
	} // directoryStart()
 
	// -------------------------------------------------------------------------

	/**
	 * Remove the completed directory contents object from the stack
	 */
	public boolean directoryEnd( File dir )
	{
		this.removeCurrentDirContents() ;
		return true ;
	} // directoryEnd()
 
	// -------------------------------------------------------------------------

	/**
	 * This method is called for whenever an exception occurs in walking through
	 * the directories.   <br>
	 * The method must return true, if the FileWalker should continue. To stop the
	 * calling FileWalker it can return false.
	 * By default this method forwards the given exception to the 
	 * LoggerProvider.logError( msg, exception ).
	 *
	 * @param ex The exception to handle
	 * @param The file, currently found by the FileWalker instance
	 * @return true to continue, false to terminate processing of files
	 */
	public boolean handleException( Exception ex, File file )
	{
		LoggerProvider.getLogger().logError(
						"Error on walking over file: " + file.getPath(), ex ) ;
		return true ;
	} // handleException()
  
	// -------------------------------------------------------------------------
  
	/**
	 * This method is called for each file, that a FileWalker instance finds.
	 * It must return true, if the FileWalker should continue. To stop the
	 * calling FileWalker it can return false.
	 *
	 * @param The file, currently found by the FileWalker instance
	 * @return true to continue, false to terminate processing of files
	 */
	public boolean handleFile( File file )
	{
		this.registerFileInfo( new FileInfo( file ) ) ;
		return true ;
	} // handleFile()
   
	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected void registerFileInfo( FileInfo fileInfo )
	{
		this.getCurrentDirContents().addFileInfo( fileInfo ) ;
	} // registerFileInfo()
   
	// -------------------------------------------------------------------------

	protected DirectoryContents getCurrentDirContents()
	{
		return (DirectoryContents)this.getDirStack().peek() ;
	} // getCurrentDirContents()
   
	// -------------------------------------------------------------------------

	protected void setCurrentDirContents( DirectoryContents newValue )
	{
		this.getDirStack().push( newValue ) ;
	} // setCurrentDirContents()
  	
	// -------------------------------------------------------------------------
	
	protected DirectoryContents removeCurrentDirContents()
	{
		return (DirectoryContents)this.getDirStack().pop() ;
	} // removeCurrentDirContents()
  	
	// -------------------------------------------------------------------------
	
	protected String normalizeDirName( String dirName )
	{
		String pathName = null ;

		pathName = dirName.replace( '\\', '/' ) ;
		if ( pathName.endsWith( "/" ) )
			pathName = pathName.substring( 0, pathName.length() - 1 ) ;
		return pathName ;
	} // normalizeDirName()
  
	// -------------------------------------------------------------------------

	protected void initialize( String baseDirName )
		throws IOException
	{
		int len ;
		String dirName ;
		File startDir ;

		startDir = new File( baseDirName ) ;
		if ( ! startDir.exists() )
			throw new FileNotFoundException( "Directory '" + baseDirName + "' not found.") ;
					
		if ( ! startDir.isDirectory() )
			throw new IOException( "'" + baseDirName + "' is not a directory.") ;					
					
		dirName = startDir.getAbsolutePath() ;
		dirName = this.normalizeDirName( dirName ) ;
		len = dirName.length() ;
		if ( ! dirName.endsWith("/") )
			len++ ;

		this.setDirStack( new Stack() ) ;
		this.toc( new TableOfContents( baseDirName ) ) ;
		this.baseDirLength( len ) ;
	} // reset()
    
	// -------------------------------------------------------------------------
	
} // class FileDirectoryScannerFileHandler
