// ===========================================================================
// CONTENT  : CLASS TableOfContents
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
import java.util.ArrayList;
import java.util.List;

/**
 * An instance of this class contains a list of DirectoryContents objects.
 * The purpose is to hold in one object all directory and file information 
 * from either an archive (zip/jar) or a base directory (e.g. "/usr/apps/conf").
 * <br>
 * To represent both types in a common information structure allows to present
 * or analyze the content information in a common way. 
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class TableOfContents
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private String containerName = null ;
	protected String getContainerName() { return containerName ; }
	protected void setContainerName( String newValue ) { containerName = newValue ; }  
  
	private List dirInfos = null ;
	protected List getDirInfos() { return dirInfos ; }
	protected void setDirInfos( List newValue ) { dirInfos = newValue ; }
		
	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Initialize the new instance with default values.
	 */
	public TableOfContents(String name)
	{
		super();

		if (name == null)
			throw new IllegalArgumentException("name must not be null!");

		this.setContainerName(name);
		this.setDirInfos(new ArrayList());
	} // TableOfContents()

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Returns the name of the container this table of contents belongs to
	 */
	public String getName()
	{
		return this.getContainerName();
	} // getName()

	// -------------------------------------------------------------------------

	/**
	 * Adds the given directory contents holder to the table of contents
	 */
	public void add(DirectoryContents dir)
	{
		if (dir != null)
		{
			this.getDirInfos().add(dir);
		}
	} // add()

	// -------------------------------------------------------------------------

	/**
	 * Returns all contained directories
	 */
	public DirectoryContents[] getDirectories()
	{
		DirectoryContents[] array;

		array = new DirectoryContents[this.getDirInfos().size()];
		this.getDirInfos().toArray(array);
		return array;
	} // getDirectories()

	// -------------------------------------------------------------------------

	/**
	 * Returns the number of directory entries in this table of contents
	 */
	public int size()
	{
		return this.getDirInfos().size();
	} // size()

	// -------------------------------------------------------------------------

	/**
	 * Returns the DirectoryContents entry at the specified index.
	 * 
	 * @throws ArrayIndexOutOfBoundsException if the index is not valid
	 */
	public DirectoryContents dirContentAt(int index)
	{
		return (DirectoryContents)this.getDirInfos().get(index);
	} // dirContentAt()

	// -------------------------------------------------------------------------

	/**
	 * Returns the name of the directory entry at the specified index.
	 * 
	 * @throws ArrayIndexOutOfBoundsException if the index is not valid
	 */
	public String dirNameAt(int index)
	{
		return this.dirContentAt(index).getDirName();
	} // dirNameAt()

	// -------------------------------------------------------------------------

	/**
	 * Returns the file info objects of the directory entry at the specified index.
	 * 
	 * @throws ArrayIndexOutOfBoundsException if the index is not valid
	 */
	public FileInfo[] filesAt(int index)
	{
		return this.dirContentAt(index).getContainedFiles();
	} // filesAt()

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
		return "TableOfContents(\"" + this.getName() + "\")";
	} // inspectString()

	// -------------------------------------------------------------------------

} // class TableOfContents
