// ===========================================================================
// CONTENT  : CLASS FileWalker
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.0 - 12/01/2014
// HISTORY  :
//  21/01/2000  duma  CREATED
//	14/02/2003	duma	added		->	Support for patterns with wildcards for digits
//	04/07/2003	duma	bugfix	->	NullPointerException in walkThrough for protected directories
//	25/10/2003	duma	changed	->	To match '*' to empty strings by default
//	24/12/2004	duma	added		->	walkThrough( String )
//	09/06/2006	mdu		added		->	fileProcessor support
//	09/03/2007	mdu		bugfix	->	avoid NPE in walkThrough( String searchPattern )
//  12/01/2014  mdu   changed ->  from AFileProcessor to IFileProcessor
//
// Copyright (c) 2000-2007, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.file;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.File;
import java.io.FilenameFilter;

import org.pf.util.CollectionUtil;

/**
 * This class provides services to navigate through a file directory and
 * handle files that match a name filter.
 * <p/>
 * It can be used either with a FileHandler or an IFileProcessor.
 *
 * @author Manfred Duchrow
 * @version 2.0
 */
public class FileWalker
{ 
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  private static final CollectionUtil CU = CollectionUtil.current();
  
  /**
   * The character to be used to separate filename patterns (';').
   */
  public static final char PATTERN_SEPARATOR_CHAR		= ';' ;

  /**
   * The character to be used to separate filename patterns (';') as String.
   */
  public static final String PATTERN_SEPARATOR = ExtendedFileFilter.PATTERN_SEPARATOR ;
  
  /**
   * The wildcard pattern that indicates a recursive walk-through in a
   * single string definition (i.e. "**").
   */
  public static final String RECURSIVE_DIR_WILDCARD	= "**" ;
  
  private static final FilenameFilter DIRECTORIES_ONLY_FILTER = new FilenameFilter()
  {
    public boolean accept(File dir, String name)
    {
      File file;
      file = new File(dir, name);
      return file.isDirectory();
    }
  }; 
  
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private FileHandler fileHandler = null ;
  protected FileHandler getFileHandler() { return fileHandler ; }  
  protected void setFileHandler( FileHandler newValue ) { fileHandler = newValue ; }  

  private IFileProcessor fileProcessor = null ;
  protected IFileProcessor getFileProcessor() { return fileProcessor ; }
  protected void setFileProcessor( IFileProcessor newValue ) { fileProcessor = newValue ; }
  
  private boolean goOn = true ;
  protected boolean getGoOn() { return goOn ; }  
  protected void setGoOn( boolean newValue ) { goOn = newValue ; }  
	
  private Character digitWildcard = null ;
  protected Character getDigitWildcard() { return digitWildcard ; }
  protected void setDigitWildcard( Character newValue ) { digitWildcard = newValue ; }	
  
  private boolean handleContentBeforeDirectory = false;
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with a file processor.
   */
  public FileWalker(IFileProcessor processor)
  {
    this.setFileProcessor(processor);
  } // FileWalker() 

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with a file handler.
   */
  public FileWalker(FileHandler handler)
  {
    this.setFileHandler(handler);
  } // FileWalker() 

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with a file handler and a wildcard character
   * for digits.
   * 
   * @param handler The file handler that gets all found files
   * @param digitWildcard A character that is used as wildcard for digits in filname patterns
   */
  public FileWalker(FileHandler handler, char digitWildcard)
  {
    this(handler);
    this.setDigitWildcardChar(digitWildcard);
  } // FileWalker() 

  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  /**
   * This method starts in the given directory to search for all files
   * matching the given pattern(s).   <br>
   * There can be more than one pattern in the pattern parameter. They have 
   * to be separated by the PATTERN_SEPARATOR (';').   <p>
   * If recursive is <b>true</b> it goes down to each subdirectory and doing
   * the same there.<br>
   * For each matching file (non-directory) the defined <i>FileHandler.handle()</i>
   * is called.
   *
   * @param dir The directory where to start
   * @param pattern The file name pattern(s) for filtering out the correct files ( wildcards '*' and '?' )
   * @param recursive If set to true, the file selection is going down to all subdirectories
   * @return the number of found files, that have matched the given pattern.
   */
  public long walkThrough(String dir, String pattern, boolean recursive)
  {
    ExtendedFileFilter filter = null;

    this.setGoOn(true);
    filter = this.createFileFilter();

    filter.addPatterns(pattern);

    if (recursive)
    {
      filter.alwaysIncludeDirectories();
    }
    else
    {
      filter.alwaysExcludeDirectories();
    }

    return this.walkThrough(dir, filter, recursive);
  } // walkThrough() 

  // -------------------------------------------------------------------------

  /**
   * Walks through all directories specified by the given pattern and calls
   * the file handler or file processor for each file that matches the file part 
   * in the given pattern.
   * <br>
   * The pattern may contain '**' in the path to define recursive walk through.
   * The file part of the pattern may contain '*' and '?' and optionally
   * the digit wildcard character for the files to be 
   * passed to the file handler or processor.
   * <p>
   * Examples:
   * <br>"prog/lib\/**\/a*.jar"  ==> Starts in directory prog\lib and walks
   * recursive through all sub-directories to pass all files that match pattern 
   * "a*.jar" to the file handler. 
   * <br>"d:/temp/tmp???.doc"  ==> Passes all files that match pattern 
   * "tmp???.doc" to the file handler.
   * 
   * @param searchPattern The pattern that specifies the path and files to walk through 
   */
  public long walkThrough(String searchPattern)
  {
    boolean recursive = false;
    String dir;
    String fileMatchPattern;
    File file;

    if (searchPattern == null)
    {
      return 0L;
    }

    file = new File(searchPattern);
    fileMatchPattern = file.getName();
    dir = file.getParent();
    if (dir == null)
    {
      dir = ".";
    }
    if (dir.endsWith(RECURSIVE_DIR_WILDCARD))
    {
      recursive = true;
      file = new File(dir);
      dir = file.getParent();
    }
    return this.walkThrough(dir, fileMatchPattern, recursive);
  } // walkThrough() 

  // -------------------------------------------------------------------------

  /**
   * Walks through the given directory and optionally through all its 
   * sub-directories as well.
   * Calls the file handler's handleFile() method or the file processor's processFile() 
   * for each file (not directory) that matches the given filter.
   * 
   * @param dir The directory to start from.
   * @param filter The filter that decides whether or not a file is passed to the FileHandler.
   * @param recursive If true all sub-directories are scanned as well. 
   */
  public long walkThrough(String dir, FilenameFilter filter, boolean recursive)
  {
    long counter = 0;
    File directory = null;
    File file = null;
    File[] files = null;
    int index = 0;

    directory = new File(dir);
    files = directory.listFiles(filter);
    if (files == null) // BUGFIX suggested by Kyle Gossman
    {
      return counter;
    }
    this.setGoOn(this.directoryStart(directory, files.length));
    if (!this.getGoOn())
    {
      return counter;
    }

    for (index = 0; index < files.length; index++)
    {
      file = files[index];

      if (file.isDirectory())
      {
        if (recursive)
        {
          counter += this.walkThrough(file.getPath(), filter, recursive);
        }
      }
      else
      {
        this.setGoOn(this.handleFile(file));
        counter++;
      }
      if (!this.getGoOn())
      {
        return counter;
      }
    } // for
    this.setGoOn(this.directoryEnd(directory));

    return counter;
  } // walkThrough() 

  // -------------------------------------------------------------------------

  /**
   * Walks through the given start directory and optionally through all its 
   * sub-directories as well.
   * Calls the file handler or file processor for each directory (not files) that 
   * match the given filter. 
   * However, it scans all directories, even those that do not match the filter.
   * 
   * @param startDir The directory to start from.
   * @param filter The filter that decides whether or not a directory is passed to the FileHandler.
   * @param recursive If true all sub-directories are scanned as well.
   * @see #walkThroughDirectoriesSubDirsFirst(String, FilenameFilter)
   */
  public long walkThroughDirectories(String startDir, FilenameFilter filter, boolean recursive)
  {
    File directory;

    directory = new File(startDir);
    return this.scanDirectories(directory, filter, recursive, false);
  } // walkThroughDirectories() 

  // -------------------------------------------------------------------------

  /**
   * Walks through the given start directory and recursively through all its sub-directories.
   * Calls the file handler or file processor for each directory (not files) that 
   * matches the given filter. It walks down the hierarchy of sub-directories before
   * it calls the handler/processor. That can be used for directory deletion. See {@link FileDeleteProcessor}. 
   * However, it scans all directories, even those that do not match the filter.
   * 
   * @param startDir The directory to start from.
   * @param filter The filter that decides whether or not a directory is passed to the FileHandler.
   * @see #walkThroughDirectories(String, FilenameFilter, boolean)
   */
  public long walkThroughDirectoriesSubDirsFirst(String startDir, FilenameFilter filter)
  {
    File directory;
    
    directory = new File(startDir);
    return this.scanDirectories(directory, filter, true, true);
  } // walkThroughDirectoriesSubDirsFirst() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Sets the given character as a wildcard character to match
   * digits ('0'-'9') only.   <br>
   * 
   * @param digitWildcard The placeholder character for digits
   */
  public void setDigitWildcardChar(char digitWildcard)
  {
    if (digitWildcard <= 0)
    {
      this.setDigitWildcard(null);
    }
    else
    {
      this.setDigitWildcard(Character.valueOf(digitWildcard));
    }
  } // setDigitWildcardChar() 

  // -------------------------------------------------------------------------

  /**
   * Returns whether or not the contents of a directory should be walked through
   * before the directory is passed to the file processor.
   * <br/>
   * The default is false.
   */
  public boolean getHandleContentBeforeDirectory()
  {
    return handleContentBeforeDirectory;
  } // getHandleContentBeforeDirectory() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Defines whether or not the contents of a directory should be walked through
   * before the directory is passed to the file handler.
   */
  public void setHandleContentBeforeDirectory(boolean newValue)
  {
    this.handleContentBeforeDirectory = newValue;
  } // setHandleContentBeforeDirectory() 

  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected long scanDirectories(final File startDir, final FilenameFilter filter, final boolean recursive, final boolean subDirsFirst)
  {
    long counter = 0;
    File dir;
    File[] directories;

    directories = startDir.listFiles(DIRECTORIES_ONLY_FILTER);
    if (CU.isNullOrEmpty(directories))
    {
      return counter;
    }

    this.setGoOn(this.directoryStart(startDir, directories.length));
    if (!this.getGoOn())
    {
      return counter;
    }

    for (int i = 0; i < directories.length; i++)
    {
      dir = directories[i];

      if (subDirsFirst)
      {
        if (this.getGoOn() && recursive)
        {
          counter += this.scanDirectories(dir, filter, recursive, subDirsFirst);
        }        
      }
      if (filter.accept(startDir, dir.getName()))
      {
        this.setGoOn(this.handleFile(dir));
        counter++;
      }
      if (!subDirsFirst)
      {        
        if (this.getGoOn() && recursive)
        {
          counter += this.scanDirectories(dir, filter, recursive, subDirsFirst);
        }
      }
      if (!this.getGoOn())
      {
        return counter;
      }
    } // for
    this.setGoOn(this.directoryEnd(startDir));

    return counter;
  } // scanDirectories() 

  // -------------------------------------------------------------------------

  protected char getDigitWildcardChar()
  {
    if (this.hasDigitWildcard())
      return this.getDigitWildcard().charValue();
    else
      return '\0';
  } // getDigitWildcardChar() 

  // -------------------------------------------------------------------------

  protected boolean hasDigitWildcard()
  {
    return this.getDigitWildcard() != null;
  } // hasDigitWildcard() 

  // -------------------------------------------------------------------------   

  protected ExtendedFileFilter createFileFilter()
  {
    if (this.hasDigitWildcard())
    {
      return new ExtendedFileFilter(this.getDigitWildcardChar(), this.restrictedWilcardMatch());
    }
    return new ExtendedFileFilter(this.restrictedWilcardMatch());
  } // createFileFilter() 

  // -------------------------------------------------------------------------

  /**
   * Returns true to define that '*' wildcards don't match empty strings.
   * The default result is false.
   * Subclasses may override this method to return true.
   */
  protected boolean restrictedWilcardMatch()
  {
    return false;
  } // restrictedWilcardMatch() 

  // -------------------------------------------------------------------------

  protected boolean handleFile(File file)
  {
    if (this.getFileProcessor() != null)
    {
      return this.getFileProcessor().processFile(file);
    }
    return this.getFileHandler().handleFile(file);
  } // handleFile() 

  // -------------------------------------------------------------------------

  protected boolean directoryEnd(File dir)
  {
    if (this.getFileProcessor() != null)
    {
      return true;
    }
    return this.getFileHandler().directoryEnd(dir);
  } // directoryEnd() 

  // -------------------------------------------------------------------------

  protected boolean directoryStart(File dir, int count)
  {
    if (this.getFileProcessor() != null)
    {
      return true;
    }
    return this.getFileHandler().directoryStart(dir, count);
  } // directoryStart() 

  // -------------------------------------------------------------------------

} // class FileWalker 
