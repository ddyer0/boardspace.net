// ===========================================================================
// CONTENT  : INTERFACE IFileProcessor
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

import org.pf.bif.callback.IObjectProcessor;

/**
 * A callback interface for file processing.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public interface IFileProcessor extends IObjectProcessor<File>
{ 
  /**
   * Process the given file or directory in any appropriate way.
   * Subclasses may override this method in order to do something useful.
   * This implementation here is doing nothing at all and returns true.
   * 
   * @param file The file or directory to process
   * @return true if processing of files should continue, otherwise false
   */
  public boolean processFile(File file);

} // interface IFileProcessor