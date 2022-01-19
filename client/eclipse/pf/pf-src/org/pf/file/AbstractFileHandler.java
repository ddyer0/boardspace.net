// ===========================================================================
// CONTENT  : ABSTRACT CLASS AbstractFileHandler
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 21/01/2001
// HISTORY  :
//  21/01/2001  duma  CREATED
//
// Copyright (c) 2001, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.file;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.File;

/**
 * This abstract class implements some of the FileHandler interface methods
 * in a way that allows subclasses to concentrate on the method handleFile().
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
abstract public class AbstractFileHandler implements FileHandler
{
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  /**
   * The default is to do nothing and return true so that the FileWalker
   * can continue
   */
  public boolean directoryStart( File dir, int count )
  {
  	return true ;
  } // directoryStart()

  // -------------------------------------------------------------------------

  /**
   * The default is to do nothing and return true so that the FileWalker
   * can continue
   */
  public boolean directoryEnd( File dir )
  {
  	return true ;
  } // directoryEnd()

} // class AbstractFileHandler