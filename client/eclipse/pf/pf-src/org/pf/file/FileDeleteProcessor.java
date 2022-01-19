// ===========================================================================
// CONTENT  : CLASS FileDeleteProcessor
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 12/01/2014
// HISTORY  :
//  12/01/2014  mdu  CREATED
//
// Copyright (c) 2014, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.file;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.File;

/**
 * In conjunction with class {@link FileWalker} this file processor can be used
 * to execute recursive and selective file deletion. That is, this processor
 * deletes each file and directory it receives from a FileWalker. 
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class FileDeleteProcessor extends AFileProcessor
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Creates an instance.
   */
  public FileDeleteProcessor()
  {
    super();
  } // FileDeleteProcessor() 
  
  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  @Override
  public boolean processFile(File file)
  {
    file.delete();
    return true;
  } // processFile()
  
  // -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  
} // class FileDeleteProcessor 
