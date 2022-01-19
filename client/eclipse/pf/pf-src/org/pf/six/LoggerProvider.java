// ===========================================================================
// CONTENT  : CLASS LoggerProvider
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 29/06/2002
// HISTORY  :
//  29/06/2002  duma  CREATED
//
// Copyright (c) 2002, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.six ;

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
  private static boolean isLoggingSupressed = false ;

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
		{
			logger = new NilLogger() ;
		}
		else
		{
			isLoggingSupressed = false ;
			logger = newLogger ;
		} 
  } // setLogger() 

  // -------------------------------------------------------------------------

  /**
   * Supress all logging. That might cause the component to rather throw
   * exceptions than just log some error messages.
   */
  public static void supressLogging() 
	{
		isLoggingSupressed = true ;
		setLogger(null) ;
	} // supressLogging()

	// -------------------------------------------------------------------------
  
  /**
   * Returns true if currently nothing will be logged
   */
  public static boolean isLoggingSupressed() 
	{
		return isLoggingSupressed ;
	} // isLoggingSupressed() 

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
