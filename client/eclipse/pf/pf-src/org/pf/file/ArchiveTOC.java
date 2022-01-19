// ===========================================================================
// CONTENT  : CLASS ArchiveTOC
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 26/08/2012
// HISTORY  :
//  22/07/2002  mdu  CREATED
//	26/08/2012	mdu	changed	--> Generic types		
//
// Copyright (c) 2002-2012, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.file ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.pf.util.NamedValue;
import org.pf.util.NamedValueList;

/**
 * This is a wrapper class around a zip archive's table of contents.
 * It reads in the table of contents of an archive and provides it as
 * a NamedValueList where the names are the directories and the values
 * are arrays of FileInfo.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class ArchiveTOC
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private NamedValueList<FileInfo[]> directories = null ;
  protected NamedValueList directories() { return directories ; }
  protected void directories( NamedValueList newValue ) { directories = newValue ; }

  private String zipFilename = null ;
  protected String getZipFilename() { return zipFilename ; }
  protected void setZipFilename( String newValue ) { zipFilename = newValue ; }
      
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
	/**
	 * Initialize the new instance with the archive named by the given filename.
	 * Reads in automatically the archive's table of contents.
	 */
	public ArchiveTOC(String filename) throws IOException
	{
		super();
		this.directories(new NamedValueList());
		this.init(filename);
	} // ArchiveTOC() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with the archive identified by the given file.
	 * Reads in automatically the archive's table of contents.
	 */
	public ArchiveTOC(File archive) throws IOException
	{
		this(archive.getAbsolutePath());
	} // ArchiveTOC() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Returns a named value list, where the names are directory names and
	 * the values are FileInfo[] objects containing all files inside the
	 * directories.
	 * 
	 */
	public NamedValueList<FileInfo[]> toc()
	{
		return this.directories();
	} // toc() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns the information about the files of this archive as a TableOfContents
	 * object. 
	 */
	public TableOfContents asTableOfContents() 
	{
		TableOfContents toc;
		NamedValueList<FileInfo[]> namedValueList;
		DirectoryContents dirContents;
		
		toc = new TableOfContents(this.getZipFilename());
		namedValueList = this.toc();
		for (int i = 0; i < namedValueList.size(); i++)
		{
			dirContents = new DirectoryContents(namedValueList.nameAt(i), namedValueList.valueAt(i));
			toc.add(dirContents);
		}
		return toc;
	} // asTableOfContents() 
	
	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================

	protected void init(String filename) throws IOException
	{
		ZipFile zipFile = null;

		zipFile = new ZipFile(filename);
		try
		{
			this.readTOC(zipFile);
		}
		finally
		{
			FileUtil.current().close(zipFile);
		}
		this.setZipFilename(filename);
	} // init() 

	// -------------------------------------------------------------------------

	protected void readTOC(ZipFile zipFile) throws IOException
	{
		Enumeration entries = null;
		ZipEntry entry = null;
		FileInfo fileInfo = null;

		entries = zipFile.entries();
		while (entries.hasMoreElements())
		{
			entry = (ZipEntry)entries.nextElement();
			if (this.isDirectory(entry))
			{
				// IGNORE all directory entries !
			}
			else
			{
				fileInfo = new FileInfo(zipFile.getName(), entry);
				this.add(fileInfo);
			}
		}
		this.convertLists();
	} // readTOC() 

	// -------------------------------------------------------------------------

	protected void convertLists()
	{
		NamedValue[] dirs;
		FileInfo[] infoArray;
		List infos;

		dirs = this.directories().namedValueArray();
		for (int i = 0; i < dirs.length; i++)
		{
			infos = (List)dirs[i].value();
			infoArray = (FileInfo[])infos.toArray(new FileInfo[0]);
			dirs[i].value(infoArray);
		}
	} // convertLists() 

	// -------------------------------------------------------------------------

	protected void add(FileInfo info)
	{
		NamedValue pathEntry;
		List classList;

		pathEntry = this.directories().findNamedValue(info.getPath());
		if (pathEntry == null)
		{
			pathEntry = new NamedValue(info.getPath(), new ArrayList());
			this.directories().add(pathEntry);
		}
		classList = (List)pathEntry.value();
		classList.add(info);
	} // add() 

	// -------------------------------------------------------------------------

	protected boolean isDirectory(ZipEntry entry)
	{
		return entry.isDirectory();
	} // isDirectory() 

	// -------------------------------------------------------------------------

} // class ArchiveTOC 
