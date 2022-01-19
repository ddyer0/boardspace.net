// ===========================================================================
// CONTENT  : CLASS DirectoryContents
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 16/08/2003
// HISTORY  :
//  16/08/2003  mdu  CREATED
//
// Copyright (c) 2003, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.file ;

import java.util.ArrayList;
import java.util.List;

import org.pf.util.CollectionUtil;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Represents one directory path with all its contained files.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class DirectoryContents
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private String dirName = null ;
	protected String getDirName() { return dirName ; }
	protected void setDirName( String newValue ) { dirName = newValue ; }
	
	private List files = null ;
	protected List getFiles() { return files ; }
	protected void setFiles( List newValue ) { files = newValue ; }	
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with the directory's name.
   */
  public DirectoryContents( String name )
  {
    this( name, null ) ;
  } // DirectoryContents()
	
	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with the directory's name and its content
	 */
	public DirectoryContents( String name, FileInfo[] fileInfos )
	{
		super() ;
    
		if ( name == null )
			throw new IllegalArgumentException( "name must not be null!" ) ;
    
		this.setDirName( name ) ;
		this.setFiles( new ArrayList() ) ;
		if ( fileInfos != null )
			CollectionUtil.current().addAll( this.getFiles(), fileInfos ) ;
	} // DirectoryContents()
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns the name of the directory this object represents
	 */
	public String getDirectoryName()
	{
		return this.getDirName() ;
	} // getDirectoryName()

	// -------------------------------------------------------------------------

	/**
	 * Returns the number of files that are directly exist in this directory
	 */
	public int numberOfContainedFiles()
	{
		return this.getFiles().size() ;
	} // numberOfContainedFiles()

	// -------------------------------------------------------------------------

	/**
	 * Returns an array of information objects on all files contained in this 
	 * directory
	 */
	public FileInfo[] getContainedFiles()
	{
		FileInfo[] array ;
		
		array = new FileInfo[this.numberOfContainedFiles()] ;
		this.getFiles().toArray( array ) ;
		return array ;
	} // getContainedFiles()

	// -------------------------------------------------------------------------

	/**
	 * Adds the given file info to the list of contained file info objects
	 */
	public void addFileInfo( FileInfo info )
	{
		if ( info != null )
			this.getFiles().add( info ) ;
	} // addFileInfo()

	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

	/**
	 * Returns a display string for an inspector.
	 * Active JOI support !
	 */
	protected String inspectString()
	{
		return "DirectoryContents(\"" + this.getDirectoryName() + "\")" ;
	} // inspectString()

	// -------------------------------------------------------------------------

} // class DirectoryContents
