// ===========================================================================
// CONTENT  : CLASS LoggerProvider
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 22/12/2003
// HISTORY  :
//  22/12/2003  duma  CREATED
//
// Copyright (c) 2003, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.pax;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.logging.Logger;
import org.pf.logging.NilLogger;
import org.pf.logging.PrintStreamLogger;

/**
 * This is the central access point for the package's logger.
 * Replacing the logger here means that all classes in this package
 * will use the new logger for further output.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class LoggerProvider
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // CLASS VARIABLES
  // =========================================================================
  private static Logger logger = new PrintStreamLogger() ;

  // =========================================================================
  // PUBLIC CLASS METHODS
  // =========================================================================
	/**
	 * Returns the current logger used by this component to report
	 * errors and exceptions. 
	 */
  public static Logger getLogger() 
  { 
  	return logger ; 
  } // getLogger()

  // -------------------------------------------------------------------------

	/**
	 * Replace the logger by another one. A value of null installs
	 * the prg.pf.logging.NilLogger.
	 */
  public static void setLogger( Logger newLogger ) 
  { 
  	if ( newLogger == null )
  		logger = new NilLogger() ;
  	else
  		logger = newLogger ; 
  } // setLogger()

  // -------------------------------------------------------------------------

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  private LoggerProvider()
  {
    super() ;
  } // LoggerProvider()

  // -------------------------------------------------------------------------

} // class LoggerProvider