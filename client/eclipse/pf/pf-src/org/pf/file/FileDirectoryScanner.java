// ===========================================================================
// CONTENT  : CLASS FileDirectoryScanner
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 16/08/2003
// HISTORY  :
//  16/08/2003  mdu  CREATED
//
// Copyright (c) 2003, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.file ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.IOException;

/**
 * This scanner starts at a specified base directory and collects information
 * about all contained files and sub directories and their contained files and 
 * so on. <br>
 * As a result it returns a TableOfContents object that contains all the 
 * collected information.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class FileDirectoryScanner
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

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
  public FileDirectoryScanner()
  {
    super() ;
  } // FileDirectoryScanner()
   
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Scan the specified directory and return the table of contents that holds
   * information about all found sub directories and files.
   * All found directories are relative to the given start directory.
   * 
   * @param dir The directory from which to start the scan
   * @throws IOException If the start directory doesn't exist or is no directory
   */
  public TableOfContents scanDirectory( String dir )
  	throws IOException
	{
		return this.scanDirectory( dir, "*" ) ;
	} // scanDirectory()
 
	// -------------------------------------------------------------------------

	/**
	 * Scan the specified directory and return the table of contents that holds
	 * information about all found sub directories and all files that match
	 * the specified pattern.
	 * All found directories are relative to the given start directory.
	 * 
	 * @param dir The directory from which to start the scan
	 * @param filePattern A pattern for the files to include into the TOC 
   * @throws IOException If the start directory doesn't exist or is no directory
	 */
	public TableOfContents scanDirectory( String dir, String filePattern )
		throws IOException
	{
		String baseDir ;
		FileDirectoryScannerFileHandler handler ; 
		String pattern ;
		
		handler = new FileDirectoryScannerFileHandler( dir ) ;
		baseDir = handler.normalizeDirName( dir ) ;
		pattern = ( filePattern == null ) ? "*" : filePattern ;
		this.start( baseDir, pattern, handler ) ;
		return handler.toc() ;
	} // scanDirectory()
 
	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected void start( String dirName, String pattern, FileHandler handler )
	{
		FileWalker fileWalker	= null ;

		fileWalker = new FileWalker( handler ) ;
		fileWalker.walkThrough( dirName, pattern, true ) ;
	} // start()

	// -------------------------------------------------------------------------

} // class FileDirectoryScanner
