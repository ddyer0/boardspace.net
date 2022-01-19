// ===========================================================================
// CONTENT  : CLASS AFileProcessor
// AUTHOR   : M.Duchrow
// VERSION  : 2.0 - 12/01/2014
// HISTORY  :
//  09/06/2006  mdu  CREATED
//  12/01/2014  mdu   changed --> to implement IFileProcessor rather than IObjectProcessor
//
// Copyright (c) 2006-2014, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.file;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.File;

/**
 * An abstract class that defines the method proccessFile( File ) to be
 * implemented by any subclass.
 * Such a subclass then can be used with FileWalker iterate recursively over 
 * a file directory and process the found files.
 *
 * @author M.Duchrow
 * @version 2.0
 */
abstract public class AFileProcessor implements IFileProcessor
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
   * Initialize the new instance with default values.
   */
  public AFileProcessor()
  {
    super();
  } // AFileProcessor() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Process the given file or directory in any appropriate way.
   * Subclasses may override this method in order to do something useful.
   * This implementation here is doing nothing at all and returns true.
   * 
   * @param file The file or directory to process
   * @return true if processing of files should continue, otherwise false
   */
  public boolean processFile(File file)
  {
    return true;
  } // processFile() 

  // -------------------------------------------------------------------------

  /**
   * Default implementation of interface IObjectProcessor that casts the 
   * given object to java.io.File if possible.
   * If the given object is no File then it will be ignored.
   */
  public boolean processObject(File file)
  {
    return this.processFile((File)file);
  } // processObject()

  // -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class AFileProcessor 
