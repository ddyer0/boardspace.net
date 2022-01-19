// ===========================================================================
// CONTENT  : CLASS Countdown
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 08/08/2006
// HISTORY  :
//  08/08/2006  mdu  CREATED
//
// Copyright (c) 2006, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.bif.logic.ICondition;

/**
 * Supports loops with a countdown of a defined time.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class Countdown
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // PUBLIC CLASS METHODS
  // =========================================================================
	/**
	 * Runs an empty loop until either the contidion is false or the specified
	 * time in milliseconds is over (more or less).
	 * That particularly makes sense if the condition gets influenced by a
	 * different thread. Therefore this thread sleeps one millisecond per 
	 * loop iteration to allow other threads to do something.
	 * 
	 * @param condition A condition that is used to continue (true) or finish (false) the loop
	 * @param millis The time in milliseconds after which the loop gets terminated independent of the condition
	 */
	public static void whileTrue( ICondition condition, long millis ) 
	{
		doWhile( condition, millis, true ) ;
	} // whileTrue() 
	
	// -------------------------------------------------------------------------

	/**
	 * Runs an empty loop until either the contidion is true or the specified
	 * time in milliseconds is over.
	 * That particularly makes sense if the condition gets influenced by a
	 * different thread. Therefore this thread sleeps one millisecond per 
	 * loop iteration to allow other threads to do something.
	 * 
	 * @param condition A condition that is used to continue (false) or finish (true) the loop
	 * @param millis The time in milliseconds after which the loop gets terminated independent of the condition
	 */
	public static void whileFalse( ICondition condition, long millis ) 
	{
		doWhile( condition, millis, false ) ;
	} // whileFalse() 
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PUBLIC CLASS METHODS
  // =========================================================================
	private static void doWhile( ICondition condition, long millis, boolean whileTrue ) 
	{
		long done ;
		
		if ( condition != null )
		{
			done = System.currentTimeMillis() + millis ;
			while( ( condition.isTrue() == whileTrue ) && ( System.currentTimeMillis() <= done ) ) 
			{
				try
				{
					Thread.currentThread().sleep( 1 ) ;
				}
				catch ( InterruptedException e )
				{
					break ;
				}
			}
		}
	} // doWhile() 
	
	// -------------------------------------------------------------------------

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  private Countdown()
  {
    super() ;
  } // Countdown() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class Countdown 
