// ===========================================================================
// CONTENT  : CLASS FileCopyProcessor
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 12/01/2014
// HISTORY  :
//  12/01/2014  mdu  CREATED
//
// Copyright (c) 2014, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.file ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.File;
import java.io.IOException;

/**
 * Copies each file to specific target folder. 
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class FileCopyProcessor extends AFileProcessor
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private File targetFolder;
  private Exception exception = null;

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initializes the new instance with a target folder.
   * 
   * @param targetFolder The folder to which the files must be copied.
   * @throws IllegalArgumentException if the given target folder is no directory
   * or does not exist. 
   */
  public FileCopyProcessor(File targetFolder)
  {
    super() ;
    if (targetFolder == null)
    {
      throw new IllegalArgumentException("The target folder must not be null!");
    }
    if (!targetFolder.exists())
    {
      throw new IllegalArgumentException("The target folder " + targetFolder.getAbsolutePath() + " does not exist!");
    }
    if (!targetFolder.isDirectory())
    {
      throw new IllegalArgumentException("The specified target " + targetFolder.getAbsolutePath() + " is no directory!");
    }
    this.setTargetFolder(targetFolder);
  } // FileCopyProcessor() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  public File getTargetFolder()
  {
    return targetFolder;
  } // getTargetFolder() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the exception that occurred during file copying or null if 
   * all was ok.
   */
  public Exception getException()
  {
    return exception;
  } // getException()
  
  // -------------------------------------------------------------------------
  
  /**
   * Copies the given file to the target folder. If any exception occurs, the
   * method returns false (i.e. terminate processing) and provides the exception
   * via {@link #getException()} method.
   */
  @Override
  public boolean processFile(File file)
  {
    File targetFile;
    
    targetFile = this.construcTargetFile(file);
    try
    {
      fileUtil().copyFile(file, targetFile, true);
    }
    catch (IOException ex)
    {
      this.setException(ex);
      return false;
    }
    return true;
  } // processFile() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected File construcTargetFile(File file) 
  {
    return new File(this.getTargetFolder(), file.getName());
  } // construcTargetFile()
  
  // -------------------------------------------------------------------------
  
  protected void setTargetFolder(File targetFolder)
  {
    this.targetFolder = new File(this.fileUtil().standardize(targetFolder.getAbsolutePath()));
  } // setTargetFolder() 
  
  // -------------------------------------------------------------------------
  
  protected void setException(Exception exception)
  {
    this.exception = exception;
  } // setException()
  
  // -------------------------------------------------------------------------
  
  protected FileUtil fileUtil() 
  {
    return FileUtil.current();
  } // fileUtil() 
  
  // -------------------------------------------------------------------------
  
} // class FileCopyProcessor 
