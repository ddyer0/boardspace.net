// ===========================================================================
// CONTENT  : CLASS StopWatch
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 10/02/2007
// HISTORY  :
//  10/02/2007  mdu  CREATED
//
// Copyright (c) 2007, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * A simple stop watch to measure the time between a start and a stop point.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class StopWatch
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private long startTime 	= 0 ;
	private long stopTime		= 0 ;

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public StopWatch()
  {
    super() ;
  } // StopWatch()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Starts the stop watch. That is, remember current time as start time.
   * @return The start time
   */
  public long start() 
	{
  	stopTime = 0 ;
		startTime = System.currentTimeMillis() ;
		return startTime ;
	} // start()
	
	// -------------------------------------------------------------------------
  
  /**
   * Stops the stop watch. That is, remember current time as stop time.
   * @return The stop time
   */
  public long stop() 
	{
		stopTime = System.currentTimeMillis() ;
		return stopTime ;
	} // stop()
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns the duration between start and stop in milliseconds.
   * If {@link #stop()} has not been called yet it returns the duration from
   * start to now.
   */
  public long getDuration() 
	{
  	if ( startTime <= 0 )
		{
			return 0 ;
		}
  	if ( stopTime <= 0 )
		{
			return System.currentTimeMillis() - startTime ;
		}
		return stopTime - startTime ;
	} // getDuration()
	
	// -------------------------------------------------------------------------
  
  /**
   * Resets the watch to a state before it gets started
   */
  public void reset() 
	{
		startTime = 0 ;
		stopTime = 0 ;
	} // reset()
	
	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class StopWatch
