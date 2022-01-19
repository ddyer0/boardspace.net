// ===========================================================================
// CONTENT  : CLASS LdapFilterParser
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.3 - 23/03/2007
// HISTORY  :
//  14/08/2002  duma  CREATED
//	27/12/2002	duma	changed	-> Added parsing of <= and >= operators
//	24/12/2003	duma	changed	-> parse( String filter )
//										added 	-> createMatchRuleOn( MatchGroup group )
//	23/03/2007	mdu		bugfix	-> throw exception if parsing finished but string end not reached
//
// Copyright (c) 2002-2007, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * An instance of this class can be used to parse an LDAP filter string
 * to a MatchRule.
 *
 * @author Manfred Duchrow
 * @version 1.2.1
 */
public class LdapFilterParser extends BaseMatchRuleParser
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	private static final char ELEMENT_START				= '(' ;
	private static final char ELEMENT_END					= ')' ;
	private static final char OPERATOR_AND				= '&' ;
	private static final char OPERATOR_OR					= '|' ;
	private static final char OPERATOR_NOT				= '!' ;
	private static final char EQUALS_COMPARATOR		= '=' ;
	private static final char GREATER_COMPARATOR	= '>' ;
	private static final char LESS_COMPARATOR			= '<' ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  
  // =========================================================================
  // PUBLIC CLASS METHODS
  // =========================================================================
  /**
   * Parse the given LDAP filter string to a MatchRule object that can
   * be used to check attributes in a Map, if they match the rule (filter).
   * 
   * @param filter The LDAP search filter
   * @throws MatchRuleParseException Each syntax error in the given filter causes
   * 																	this exception with a short description
   * 																	of what is wrong
   */
	public static MatchRule parseFilter( String filter )
		throws MatchRuleParseException
	{
		LdapFilterParser parser = new LdapFilterParser() ;
			
		return parser.parse( filter ) ;
	} // parseFilter()

	// -------------------------------------------------------------------------

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public LdapFilterParser()
  {
    super() ;
  } // LdapFilterParser()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Parse the given LDAP filter string to a MatchRule object that can
   * be used to check attributes in a Map, if they match the rule (filter).
   * 
   * @param filter The LDAP search filter
   * @throws MatchRuleParseException Each syntax error in the given filter causes
   * 																	this exception with a short description
   * 																	of what is wrong
   */
	public MatchRule parse( String filter )
		throws MatchRuleParseException
	{
		MatchGroup group ;
			
		group = this.parseToGroup( filter ) ;
		if ( group == null )
		{
			return null ;
		}
		if ( !this.scanner().atEnd() )
		{
			throw new MatchRuleParseException( "End of filter expected at position " 
					+ this.scanner().getPosition() ) ;
		}				
		return this.createMatchRuleOn( group ) ;	
	} // parse()

	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  /**
   * Parse the given LDAP filter string to a MatchGroup which can be used to
   * create a MatchRule.
   * 
   * @param filter The LDAP search filter
   * @throws MatchRuleParseException Each syntax error in the given filter causes
   * 																	this exception with a short description
   * 																	of what is wrong
   */
	protected MatchGroup parseToGroup( String filter )
		throws MatchRuleParseException
	{
		if ( filter == null )
			return null ;
			
		this.scanner( new StringScanner(filter) ) ;
		return this.parse() ;
	} // parseToGroup()

	// -------------------------------------------------------------------------

	protected MatchGroup parse()
		throws MatchRuleParseException
	{
		char ch ;
		MatchElement element ;
		MatchGroup group ;
		
		ch = this.scanner().nextNoneWhitespaceChar() ;
		this.checkUnexpectedEnd(ch) ;
		
		if ( ch == ELEMENT_START )
		{
			element = this.parseElement() ;
			this.checkNextClosingParenthesis() ;
			group = new MatchGroup() ;
			group.addElement( element ) ;
			element = group ;
		}
		else
		{
			this.scanner().skip(-1) ;
			element = this.parseAttribute() ;
			ch = this.scanner().nextChar() ;
			if ( ! this.atEnd( ch ) )
			{
				this.throwException( "End expected, but found '" 
										+ ch + "'" ) ;
			}
		}
		
		if ( element.isGroup() )
		{
			group = (MatchGroup)element ;
		}
		else
		{
			group = new MatchGroup() ;
			group.addElement( element ) ;
		}
		
		return group ;
	} // parse()

	// -------------------------------------------------------------------------

	protected MatchElement parseElement()
		throws MatchRuleParseException
	{
		char ch ;
		MatchElement element = null ;
		
		ch = this.scanner().nextNoneWhitespaceChar() ;
		this.checkUnexpectedEnd( ch ) ;
		
		if ( this.isOperator( ch ) )
		{
			element = this.parseGroup( ch ) ;			
		}
		else
		{
			if ( this.isNotOperator( ch ) )
			{
				element = this.parseNotElement() ;
			}
			else
			{
				if ( this.isLiteral( ch ) )
				{
					this.scanner().skip( -1 ) ;
					element = this.parseAttribute() ;
				}
				else
				{
					this.throwException( "Unexpected character '" + ch + "' at position "
										+ ( this.scanner().getPosition() - 1 ) ) ;
				}
			}
		}
		
		return element ;
	} // parseElement()

	// -------------------------------------------------------------------------

	protected MatchElement parseNotElement()
		throws MatchRuleParseException
	{
		MatchElement element = null ;

		this.checkNextOpeningParenthesis() ;

		element = this.parseElement() ;

		this.checkNextClosingParenthesis() ;

		element.setNot( true ) ;

		return element ;
	} // parseNotElement()

	// -------------------------------------------------------------------------

	protected MatchGroup parseGroup( char operator )
		throws MatchRuleParseException
	{
		char ch ;
		MatchGroup group = null ;
		MatchElement element = null ;
		boolean andOperator  = true ;

		ch = this.checkNextOpeningParenthesis() ;

		andOperator = ( operator != OPERATOR_OR ) ;

		group = new MatchGroup() ;
		while ( ( ! this.atEnd( ch ) ) && ( ch == ELEMENT_START ) )
		{
			element = this.parseElement() ;
			group.addElement( element ) ;
			if ( group.elementCount() > 1 )
			{
				element.setAnd( andOperator ) ;
			}
			this.checkNextClosingParenthesis() ;
				
			ch = this.scanner().nextNoneWhitespaceChar() ;						
		}

		if ( ch != ELEMENT_END )
			this.parenthesisExpected( "Closing" ) ;

		this.scanner().skip(-1) ;		

		return group ;
	} // parseGroup()

	// -------------------------------------------------------------------------

	protected MatchAttribute parseAttribute()
		throws MatchRuleParseException
	{
		MatchAttribute matchAttr ;
		StringBuffer buffer = new StringBuffer( 30 ) ;
		char ch ;
		
		ch = this.scanner().nextChar() ;
		while ( ( ! this.atEnd( ch ) ) && this.isValidAttributeNameCharacter( ch ) )
		{
			buffer.append( ch ) ;
			ch = this.scanner().nextChar() ;
		}
		
		this.checkUnexpectedEnd( ch ) ;
		
		matchAttr = new MatchAttribute() ;
		matchAttr.setAttributeName( buffer.toString() ) ;

		if ( ch == GREATER_COMPARATOR )
		{
			matchAttr.setGreaterOrEqualOperator() ;
			ch = this.scanner().nextChar() ;
		}
		else
		{
			if ( ch == LESS_COMPARATOR )
			{
				matchAttr.setLessOrEqualOperator() ;
				ch = this.scanner().nextChar() ;
			}
		}

		if ( ( ch != EQUALS_COMPARATOR ) || ( this.scanner().peek() == '~' ) )
		{
			this.throwException( "Unsupported or invalid operator '"
					+ ch + "' at position " + this.scanner().getPosition() ) ;
		}	
				
		buffer = new StringBuffer( 30 ) ;
		ch = this.scanner().nextChar() ;
		while ( ( ! this.atEnd( ch ) ) && ( ch != ELEMENT_END ) )
		{
			buffer.append( ch ) ;
			ch = this.scanner().nextChar() ;
		}
		matchAttr.setPattern( new StringPattern( buffer.toString(), true ) ) ;
		
		if ( ch == ELEMENT_END )
			this.scanner().skip(-1) ;
		
		return matchAttr ;
	} // parseAttribute()

	// -------------------------------------------------------------------------

	protected boolean isNotOperator( char ch )
	{
		return	( ch == OPERATOR_NOT ) ;
	} // isNotOperator()

	// -------------------------------------------------------------------------

	protected boolean isOperator( char ch )
	{
		return	(	( ch == OPERATOR_AND ) ||
							( ch == OPERATOR_OR ) 
						) ;
	} // isOperator()

	// -------------------------------------------------------------------------

	protected boolean isLiteral( char ch )
	{
		return Character.isJavaIdentifierStart( ch ) ;
	} // isLiteral()

	// -------------------------------------------------------------------------

	protected boolean isValidAttributeNameCharacter( char ch )
	{
		return	(	( this.isLiteral( ch ) ) ||
							( Character.isDigit( ch ) ) 
						) ;
	} // isValidAttributeNameCharacter()

	// -----------------------------------------------------------------------
	
	protected void parenthesisExpected( String prefix )
		throws MatchRuleParseException
	{
		this.throwException( prefix + " parenthesis expected at position "
										+ ( this.scanner().getPosition() ) ) ;
	} // parenthesisExpected()

	// -------------------------------------------------------------------------

	protected char checkNextOpeningParenthesis()
		throws MatchRuleParseException
	{
		char ch ;
		
		ch = this.scanner().nextNoneWhitespaceChar() ;
		this.checkUnexpectedEnd(ch) ;
		if ( ch != ELEMENT_START )
			this.parenthesisExpected( "Opening" ) ;	
			
		return ch ;	
	} // checkNextOpeningParenthesis()

	// -------------------------------------------------------------------------

	protected char checkNextClosingParenthesis()
		throws MatchRuleParseException
	{
		char ch ;
		
		ch = this.scanner().nextNoneWhitespaceChar() ;
		this.checkUnexpectedEnd(ch) ;
		if ( ch != ELEMENT_END )
			this.parenthesisExpected( "Closing" ) ;	
			
		return ch ;	
	} // checkNextClosingParenthesis()

	// -------------------------------------------------------------------------

	protected MatchRule createMatchRuleOn( MatchGroup group )
	{
		MatchRule matchRule  ;
		
		matchRule = new MatchRule( group ) ;
		
		matchRule.ignoreCaseInNames( true ) ;
		matchRule.ignoreCase( true ) ;
		matchRule.multiCharWildcardMatchesEmptyString( true ) ;				
						
		return matchRule ;
	} // createMatchRuleOn()
 
	// -------------------------------------------------------------------------

} // class LdapFilterParser