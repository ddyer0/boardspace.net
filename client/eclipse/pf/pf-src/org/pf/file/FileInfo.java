// ===========================================================================
// CONTENT  : CLASS FileInfo
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.3 - 27/04/2008
// HISTORY  :
//  22/07/2002  duma  CREATED
//	14/03/2003	duma	changed	->	Use FileUtil now
//	20/12/2005	duma	added		->	asFileLocator()
//	27/04/2008	mdu		added		->	asFile()
//
// Copyright (c) 2002-2008, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.file ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;

/**
 * This data object holds the name, size and modification date of a file.
 * The file can either be a normal file or one inside a zip archive.
 *
 * @author Manfred Duchrow
 * @version 1.3
 */
public class FileInfo
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	private static final String PATH_SEPARATOR = "/" ;
	private static final String ARCHIVE_INDICATOR = "!" ;
	
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String archiveName = null ;
  private String path = null ;
  private String name = null ;   
  private long size = 0L ;    
  private long lastModified = 0L ;
    
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
	/**
	 * Initialize the new instance from the given file.
	 */
	public FileInfo(File file)
	{
		super();
		this.setPathAndName(file);
		this.setSize(file.length());
		this.lastModified(file.lastModified());
	} // FileInfo() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance from the given zip file entry.
	 */
	public FileInfo(String archiveName, ZipEntry zipEntry)
	{
		super();
		this.setArchiveName(archiveName);
		this.setPathAndName(new File(zipEntry.getName()));
		this.setSize(zipEntry.getSize());
		this.lastModified(zipEntry.getTime());
	} // FileInfo() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Returns the name of the archive, the file is included in or null if the
	 * file is located in the normal directory structure.
	 */
	public boolean isInArchive()
	{
		return this.getArchiveName() != null;
	} // isInArchive() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the name of the archive, the file is included in or null if the
	 * file is located in the normal directory structure.
	 */
	public String getArchiveName()
	{
		return archiveName;
	} // getArchiveName() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the full location name, that is in case of a normal file the
	 * path and filename combined.
	 * In case of an archived file it is the archive name + path + filename.
	 * <br>
	 * The path separator is a forward slash ('/') on any platform !
	 */
	public String getLocation()
	{
		if (this.isInArchive())
		{
			return this.getArchiveName() + PATH_SEPARATOR + this.getFullName();
		}
		return this.getFullName();
	} // getLocation() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the path + name of the file.
	 * <br>
	 * The path separator is a forward slash ('/') on any platform !
	 */
	public String getFullName()
	{
		return this.getPath() + PATH_SEPARATOR + this.getName();
	} // getFullName() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the path (without filename) to the file.
	 * <br>
	 * The path separator is a forward slash ('/') on any platform !
	 */
	public String getPath()
	{
		return path;
	} // getPath() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the name of the file (without any path).
	 */
	public String getName()
	{
		return name;
	} // getName() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the size of the file in bytes
	 */
	public long getSize()
	{
		return size;
	} // getSize() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the timestamp of the file's last modification
	 */
	public long lastModified()
	{
		return lastModified;
	} // lastModified() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a FileLocator that corresponds to the file specified by this
	 * FileInfo object.
	 */
	public FileLocator asFileLocator()
	{
		return FileLocator.create(this.getLocation());
	} // asFileLocator() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the file this info objects describes or null if it is inside
	 * an archive.
	 */
	public File asFile()
	{
		if (this.isInArchive())
		{
			return null;
		}
		return new File(this.getPath(), this.getName());
	} // asFile() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the URL of the file this info objects describes.
	 * 
	 * @throws MalformedURLException if no appropriate URL can be created. 
	 */
	public URL asURL() throws MalformedURLException
	{
		File file;
		String fileStr;
		String urlStr;
		
		if (this.isInArchive())
		{
			file = new File(this.getArchiveName());
			fileStr = file.getAbsolutePath() + ARCHIVE_INDICATOR + PATH_SEPARATOR + this.getFullName();
		}
		else
		{
			fileStr = this.asFile().getAbsolutePath();
		}
		urlStr = "jar:file:/" + FileUtil.current().javaFilename(fileStr);
		return new URL(urlStr);
	} // asFile() 
	
	// -------------------------------------------------------------------------
	
	@Override
	public String toString()
	{
		return "FileInfo(" + this.getName() + ")";
	} // toString()

	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	protected void setArchiveName(String newValue)
	{
		archiveName = newValue;
	} // setArchiveName() 

	// -------------------------------------------------------------------------

	protected void setPath(String newValue)
	{
		path = newValue;
	} // setPath() 

	// -------------------------------------------------------------------------

	protected void setName(String newValue)
	{
		name = newValue;
	} // setName() 

	// -------------------------------------------------------------------------

	protected void setSize(long newValue)
	{
		size = newValue;
	} // setSize() 

	// -------------------------------------------------------------------------

	protected void lastModified(long newValue)
	{
		lastModified = newValue;
	} // lastModified() 

	// -------------------------------------------------------------------------

	protected void setPathAndName(File file)
	{
		this.setPath(this.util().javaFilename(file.getParent()));
		this.setName(file.getName());
	} // setPathAndName() 

	// -------------------------------------------------------------------------

	protected FileUtil util()
	{
		return FileUtil.current();
	} // util() 

	// -------------------------------------------------------------------------

} // class FileInfo 
