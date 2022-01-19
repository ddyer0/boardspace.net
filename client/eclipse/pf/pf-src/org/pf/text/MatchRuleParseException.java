// ===========================================================================
// CONTENT  : CLASS MatchRuleParseException
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 20/12/2004
// HISTORY  :
//  11/07/2001  duma  CREATED
//	14/08/2002	duma	added		-> position
//	20/12/2004	duma	changed	-> superclass from Exception to MatchRuleException
//
// Copyright (c) 2002-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * This exception is used for all parsing errors of MatchRule parser classes.
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
public class MatchRuleParseException extends MatchRuleException
{
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private int position = 0 ;
  private String parseString = null ;
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public MatchRuleParseException()
  {
    super() ;
  } // MatchRuleParseException()

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with with a message.
   */
  public MatchRuleParseException( String message )
  {
    super( message ) ;
  } // MatchRuleParseException()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the position in the string where the exception was caused.
   */
  public int getPosition()
	{
		return position ;
	} // getPosition()

	// -------------------------------------------------------------------------

	/**
	 * Returns the string in which caused the parsing exception.
	 */
  public String getParseString() 
  { 
  	return parseString ; 
  } // getParseString() 
  
	// -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  protected void setPosition( int pos )
	{
		position = pos ;
	} // setPosition()

	// -------------------------------------------------------------------------

  protected void setParseString( String newValue ) 
  { 
  	parseString = newValue ; 
  } // setParseString()

	// -------------------------------------------------------------------------

} // class MatchRuleParseException