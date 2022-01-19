// ===========================================================================
// CONTENT  : ABSTRACT CLASS BaseMatchRuleParser
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 04/1272003
// HISTORY  :
//  23/08/2002  duma  CREATED
//	04/12/2003	duma	added		-->	checkExpectedEnd()
//
// Copyright (c) 2002-2003, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Abstract superclass for parsers that produce a MatchRule from any specific
 * syntax. This class provides some common methods and a instance variables
 * that holds a scanner (StringScanner) which most parsers need.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
abstract public class BaseMatchRuleParser
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private StringScanner scanner = null ;
  protected StringScanner scanner() { return scanner ; }
  protected void scanner( StringScanner newValue ) { scanner = newValue ; }

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values
   */
  public BaseMatchRuleParser()
  {
    super() ;
  } // BaseMatchRuleParser()

	// -------------------------------------------------------------------------

  /**
   * Initialize the new instance with the given rule.
   * 
   * @param rule Any rule string the internal scanner should be initialized with
   */
  public BaseMatchRuleParser( String rule )
  {
    this() ;
    this.scanner( new StringScanner(rule) ) ;
  } // BaseMatchRuleParser()

	// -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * Returns true, if the given character indicates the end of a scanned string.
   */
	protected boolean atEnd( char ch )
	{
		return ( ch == StringScanner.END_REACHED ) ;
	} // atEnd()

	// -------------------------------------------------------------------------

	protected void checkUnexpectedEnd( char ch )
		throws MatchRuleParseException
	{
		if ( this.atEnd( ch ) )
			this.throwException( "Unexpected end of string reached" ) ;		
	} // checkUnexpectedEnd()

	// -------------------------------------------------------------------------	

	protected void checkExpectedEnd( char ch )
		throws MatchRuleParseException
	{
		if ( ! this.atEnd(ch) )
		{
			this.throwException( "Nothing more expected "
					 + "at position " + this.scanner().getPosition() 
					 + ", but found '" + ch + "'" ) ;
		}		
	} // checkExpectedEnd()

	// -------------------------------------------------------------------------

	protected void throwException( String msg )
		throws MatchRuleParseException
	{
		MatchRuleParseException exception ;
		
		exception = new MatchRuleParseException( msg ) ;
		exception.setPosition( this.scanner().getPosition() ) ;
		exception.setParseString( this.scanner().toString() ) ;
		throw exception ;
	} // throwException()

	// -------------------------------------------------------------------------
	
	protected StringUtil str()
	{
		return StringUtil.current() ;
	} // str()

	// -------------------------------------------------------------------------
	
} // class BaseMatchRuleParser