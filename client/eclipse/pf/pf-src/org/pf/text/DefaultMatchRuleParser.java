// ===========================================================================
// CONTENT  : CLASS DefaultMatchRuleParser
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.4 - 20/12/2004
// HISTORY  :
//  23/08/2002  duma  CREATED
//	22/11/2002	duma	added		->	Supports special characters in attribute names
//	01/01/2003	duma	added		->	Parsing of <, >, <=, >=
//	24/10/2003	duma	changed	->	parse( String rule ), added createMatchRuleOn()
//	04/12/2003	duma	changed	->	Throw exception if closing parenthesis is missing
//	20/12/2004	duma	added		->	static create(), static create(chars)
//
// Copyright (c) 2002-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This parser translates the match-rule syntax to a MatchRule object.
 * <p>
 * <b>It is recommended to use the static create() methods rather than the 
 * constructors to get a new parser instance.</b>
 * <p>
 * <h4>Example 1</h4>
 * Here is a sample rule:
 * <p>
 * <em>firstName=John & lastName=M*</em>
 * <p>
 * It means that attribute 'firstName' must have the value <i>"John"</i> 
 * and that the attribute 'lastName' must match <i>"M*"</i>, that is all 
 * values starting with a capital 'M' will evaluate to true.
 * <p>
 * A slightly different syntax for the above rule is:
 * <p>
 * <em>firstName{John} & lastName{M*}</em>
 * <p>
 * Using the following dictionary will evaluate the rules from above to false:
 * <p>
 * <pre>
 * Map map = new HashMap() ;
 * map.put( "firstName", "Conner" ) ;
 * map.put( "lastName", "McLeod" ) ;
 * MatchRule mr = DefaultMatchRuleParser.parseRule( "firstName{John} & lastName{M*}" ) ;
 * boolean found = mr.matches(map) ; // returns false
 * </pre>
 * <p>
 * The next dictionary will evaluate the rule to true:
 * <p>
 * <pre>
 * Map map = new HashMap() ;
 * map.put( "firstName", "John" ) ;
 * map.put( "lastName", "Miles" ) ;
 * MatchRule mr = DefaultMatchRuleParser.parseRule( "firstName{John} & lastName{M*}" ) ;
 * boolean found = mr.matches(map) ; // returns true
 * </pre> 
 * <p>
 * The parser generally supports the following comparisons in rules:
 * <table border="1" cellpadding=2">
 *   <tr>
 *     <td><b>Comparison</b></td>
 *     <td><b>Description</b></td>
 *     <td><b>Example(s)</b></td>
 *   </tr>
 *   <tr>
 *     <td>equals</td>
 *     <td>Compares if the value of the attribute is equal to the specified value</td>
 *     <td>name=Roger</td>
 *   </tr>
 *   <tr>
 *     <td>equals any</td>
 *     <td>Compares if the value of the attribute is equal to any of a list of specified values</td>
 *     <td>name{Fred,Peter,John,Mike}</td>
 *   </tr>
 *   <tr>
 *     <td>matches</td>
 *     <td>Compares if the value of the attribute matches the specified pattern</td>
 *     <td>name=J*y</td>
 *   </tr>
 *   <tr>
 *     <td>matches any</td>
 *     <td>Compares if the value of the attribute matches any of a list of specified values</td>
 *     <td>name{Fred,A*,B?n,R*so?,Carl}</td>
 *   </tr>
 *   <tr>
 *     <td>less</td>
 *     <td>Compares if the value of the attribute is less than the specified value</td>
 *     <td>name&lt;Henderson<br>
 *         age&lt;20
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>greater</td>
 *     <td>Compares if the value of the attribute is greater than the specified value</td>
 *     <td>name&gt;Franklin<br>
 *         age&gt;15
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>less or equal</td>
 *     <td>Compares if the value of the attribute is less or equal to the specified value</td>
 *     <td>name&lt;=Anderson<br>
 *         age&lt;=50
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>greater or equal</td>
 *     <td>Compares if the value of the attribute is greater or equal to the specified value</td>
 *     <td>name&gt;=Franklin<br>
 *         age&gt;=3
 *     </td>
 *   </tr>
 * </table>
 * <p>
 * There are some characters with special purpose in a rule. 
 * The table below describes each of them and lists the method that can 
 * be used to change the character.
 * <table border="1" cellpadding=2">
 * <tr>
 *     <td><b>char</b></td> <td><b>Description</b></td> <td><b>Method to change in MatchRuleChars</b></td>
 * </tr>
 * <tr>
 *   <td>&</td> <td>AND operator</td> <td>setAndChar()</td>
 * </tr>
 * <tr>
 *   <td>|</td> <td>OR operator</td> <td>setOrChar( )</td>
 * </tr>
 * <tr>
 *   <td>!</td> <td>NOT operator</td> <td>setNotChar()</td>
 * </tr>
 * <tr>
 *   <td>{</td> <td>Starts a list of values</td> <td>setValueStartChar()</td>
 * </tr>
 * <tr>
 *   <td>,</td> <td>Separator of values in a value list</td> <td>setValueSeparatorChar()</td>
 * </tr>
 * <tr>
 *   <td>}</td> <td>Ends a list of values</td> <td>setValueEndChar()</td>
 * </tr>
 * <tr>
 *   <td>(</td> <td>Starts a group of attribute rules</td> <td>setGroupStartChar()</tr>
 * </tr>
 * <tr>
 *   <td>)</td> <td>Ends a group of attribute rules</td> <td>setGroupEndChar()</td>
 * </tr>
 * <tr>
 *   <td>=</td> <td>Compares equality of the attribute's value</td> <td>setEqualsChar()</td>
 * </tr>
 * <tr>
 *   <td>&lt;</td> <td>Compares the attribute's value to be less than the specified value</td> <td>setLessChar()</td>
 * </tr>
 * <tr>
 *   <td>&gt;</td> <td>Compares the attribute's value to be greater zhan the specified value</td> <td>setGreaterChar()</td>
 * </tr>
 * <tr>
 *   <td>?</td> <td>Wildcard for a single character in a value</td> <td>---</td>
 * </tr>
 * <tr>
 *   <td>*</td> <td>Wildcard for any count of characters in a value</td> <td>---</td>
 * </tr>
 * </table>
 * <p>
 * Any rule must comply to the following restrictions:
 * <ul>
 *   <li>A rule must not contain any carriage-return or line-feed characters!</li>
 *   <li>The NOT operator is only allowed after an AND or an OR operator!</li>
 *   <li>There might be any amount of spaces in front or after operators</li>
 *   <li>There might be any amount of spaces in front or after group parenthesis</li>
 *   <li>Attribute names must only consist of characters ( A-Z, a-z ) and/or digits ( 0-9 )
 *       and any additional characters that are specified in MatchRuleChars.setSpecialNameCharacters()
 *   </li>
 *   <li>Between an attribute name and the value list starting bracket might be any amount of whitespace characters.</li>
 *   <li>Between an attribute name and a compare operator might be any amount of whitespace characters.</li>
 *   <li>Any character inside a value list is treated as part of a value except the VALUE SEPARATOR, the VALUE LIST END CHARACTER and the two WILDCARD characters.</li>
 * </ul>
 * <p>
 * <h4>Example 2:</h4>
 * <p>
 * A more complex rule could look like this:
 * <p>
 * <em>( city{P*,Ch*} & ! city{Paris,Pretoria} ) | ( language{en,de,fr,it,es} & currency{??D} )</em>
 * <p>
 * The dictonary below will evaluate to true if checked against the above rule:
 * <p>
 * <pre>
 * DefaultMatchRuleParser parser = new DefaultMatchRuleParser() ;
 * MatchRule rule = parser.parse("( city{P*,Ch*} & ! city{Paris,Pretoria} ) | " +
 *  ( language{en,de,fr,it,es} & currency{??D} )" ) ;
 * Map map = new HashMap() ; 
 * map.put( "city", "Pittsburg" ) ;
 * map.put( "language", "en" ) ;
 * map.put( "currency", "USD" ) ;
 * boolean ok = rule.matches( map ) ;
 * </pre>
 * <p>
 * Whereas the following values produce a false:
 * <p>
 * <pre>
 * MatchRuleChars chars = new  MatchRuleChars() ;
 * chars.setValueSeparatorChar( ';' ) ;
 * DefaultMatchRuleParser parser = new DefaultMatchRuleParser(chars) ;
 * MatchRule rule = parser.parse( "( city{P*;Ch*} &amp; ! city{Paris;Pretoria} ) | " +
 *   ( language{en;de;fr;it;es} & currency{??D} )" ) ;
 * Map map = new HashMap() ;
 * map.put( "city", "Pretoria" ) ;
 * map.put( "language", "de" ) ;
 * map.put( "currency", "USD" ) ;
 * boolean ok = rule.matches( map ) ;
 * </pre>
 * <p>   
 *
 * @author Manfred Duchrow
 * @version 1.4
 */
public class DefaultMatchRuleParser extends BaseMatchRuleParser
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private MatchRuleChars ruleChars = new MatchRuleChars() ;
  protected MatchRuleChars getRuleChars() { return ruleChars ; }
  protected void setRuleChars( MatchRuleChars newValue ) { ruleChars = newValue ; }

	// -------------------------------------------------------------------------
	
	private boolean ignoreCaseInNames = false ;
	/**
	 * Returns true, if the parser produces MatchRules that treat
	 * attribute names case-insensitive.
	 */
	public boolean getIgnoreCaseInNames() { return ignoreCaseInNames ; }
	/**
	 * Sets whether or not the parser produces MatchRules that treat
	 * attribute names case-insensitive.
	 */
	public void setIgnoreCaseInNames( boolean newValue ) { ignoreCaseInNames = newValue ; }	

	// -------------------------------------------------------------------------

	private boolean ignoreCaseInValues = false ;
	/**
	 * Returns true, if the parser produces MatchRules that are case-insensitive
	 * when comparing values. 
	 */
	public boolean getIgnoreCaseInValues() { return ignoreCaseInValues ; }
	/**
	 * Sets whether or not the parser produces MatchRules that are 
	 * case-insensitive when comparing values. 
	 */
	public void setIgnoreCaseInValues( boolean newValue ) { ignoreCaseInValues = newValue ; }
	
	// -------------------------------------------------------------------------

	private boolean multiCharWildcardMatchesEmptyString = false ;
	/**
	 * Returns true, if this parser creates match rules that allow empty strings 
	 * at the position of the multi character wildcard ('*').
	 * <p>
	 * The default value is false. 
	 */
	public boolean getMultiCharWildcardMatchesEmptyString() { return multiCharWildcardMatchesEmptyString ; }
	/**
	 * Sets whether or not this parser creates match rules that allow empty 
	 * strings at the position of the multi character wildcard ('*').
	 * <p>
	 * The default value is false. 
	 */
	public void setMultiCharWildcardMatchesEmptyString( boolean newValue ) { multiCharWildcardMatchesEmptyString = newValue ; }	
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PUBLIC CLASS METHODS
  // =========================================================================
  /**
   * Parse the given rule string to a MatchRule object that can
   * be used to check attributes in a Map, if they match the rule.
   * This method creates a new parser for each call by invocation of a 
   * constructor.
   * 
   * @param rule The rule in a string compliant to the MatchRule syntax
   * @throws MatchRuleParseException Each syntax error in the given rule causes
   * 																	this exception with a short description
   * 																	of what is wrong
   */
	public static MatchRule parseRule( String rule )
		throws MatchRuleParseException
	{
		DefaultMatchRuleParser parser ;
			
		parser = new DefaultMatchRuleParser() ;
			
		return parser.parse( rule ) ;	
	} // parseRule() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a new parser that generates rules which treat the multi-char
	 * wildcard (i.e. '*') in a way that it matches empty strings.
	 * <br>
	 * Using the constructor is different. In that case a multi-char wildcard
	 * won't match an empty string!
	 */
	public static DefaultMatchRuleParser create() 
	{
		DefaultMatchRuleParser parser ;
		
		parser = new DefaultMatchRuleParser() ;
		parser.setMultiCharWildcardMatchesEmptyString(true) ;
		return parser ;			
	} // create() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns a new parser that generates rules which treat the multi-char
	 * wildcard (i.e. '*') in a way that it matches empty strings.
	 * <br>
	 * Using the constructor is different. In that case a multi-char wildcard
	 * won't match an empty string!
	 * 
	 * @param chars The charscter set that is used for the rules operators etc.
	 */
	public static DefaultMatchRuleParser create( MatchRuleChars chars ) 
	{
		DefaultMatchRuleParser parser ;
		
		parser = new DefaultMatchRuleParser( chars ) ;
		parser.setMultiCharWildcardMatchesEmptyString(true) ;
		return parser ;			
	} // create() 

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public DefaultMatchRuleParser()
  {
    super() ;
  } // DefaultMatchRuleParser() 

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with a set of rule characters.
   */
  public DefaultMatchRuleParser( MatchRuleChars ruleCharacters )
  {
    this() ;
    if ( ruleCharacters != null )
	    this.setRuleChars( ruleCharacters ) ;
  } // DefaultMatchRuleParser() 

  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Parse the given rule string to a MatchRule object that can
   * be used to check attributes in a Map, if they match the rule.
   * 
   * @param rule The rule in a string compliant to the MatchRule syntax
   * @throws MatchRuleParseException Each syntax error in the given rule causes
   * 																	this exception with a short description
   * 																	of what is wrong
   */
	public MatchRule parse( String rule )
		throws MatchRuleParseException
	{
		MatchGroup group ;
			
		group = this.parseToGroup( rule ) ;
		if ( group == null )
			return null ;
			
		return this.createMatchRuleOn( group ) ;	
	} // parse() 

	// -------------------------------------------------------------------------

  /**
   * Parse the given rule string to a MatchRule and apply the given datatypes
   * to it. Such a rule will do attribute comparisons according to the
   * corresponding datatype. 
   * 
   * @param rule The rule in a string compliant to the MatchRule syntax
   * @param datatypes The attributes and their associated datatypes
   * @throws MatchRuleParseException Each syntax error in the given rule causes
   * 																	this exception with a short description
   * 																	of what is wrong
   * @see MatchRule#setDatatypes(Map)
   */
	public MatchRule parseTypedRule( String rule, Map datatypes )
		throws MatchRuleException
	{
		MatchRule matchRule ;

		matchRule = this.parse( rule ) ;
		matchRule.setDatatypes( datatypes ) ;
		return matchRule ;
	} // parseTypedRule() 

	// -------------------------------------------------------------------------

  /** Returns the character for AND operations ( DEFAULT = '&' ) */
  public char getAndChar()
  {
    return this.getRuleChars().getAndChar() ;
  } // getAndChar() 

  // -------------------------------------------------------------------------

  /** Sets the character for AND operations */
  public void setAndChar( char newValue )
  {
    this.getRuleChars().setAndChar( newValue );
  } // setAndChar() 

  // -------------------------------------------------------------------------

  /** Returns the character for OR operations ( DEFAULT = '|' ) */
  public char getOrChar()
  {
    return this.getRuleChars().getOrChar() ;
  } // getOrChar() 

  // -------------------------------------------------------------------------

  /** Sets the character for OR operations */
  public void setOrChar( char newValue )
  {
    this.getRuleChars().setOrChar( newValue );
  } // setOrChar() 

  // -------------------------------------------------------------------------

  /** Returns the character for NOT operations ( DEFAULT = '!' ) */
  public char getNotChar()
  {
    return this.getRuleChars().getNotChar() ;
  } // getNotChar() 

  // -------------------------------------------------------------------------

  /** Sets the character for NOT operations */
  public void setNotChar( char newValue )
  {
    this.getRuleChars().setNotChar( newValue );
  } // setNotChar() 

  // -------------------------------------------------------------------------

  /** Returns the character for separation of values ( DEFAULT = ',' ) */
  public char getValueSeparatorChar()
  {
    return this.getRuleChars().getValueSeparatorChar() ;
  } // getValueSeparatorChar() 

  // -------------------------------------------------------------------------

  /** Sets the character that separates values in a value list */
  public void setValueSeparatorChar( char newValue )
  {
    this.getRuleChars().setValueSeparatorChar( newValue ) ;
  } // setValueSeparatorChar() 

  // -------------------------------------------------------------------------

	/** Returns the character that is used to enclose a value ( DEFAULT = '\'' ) */
	public char getValueDelimiterChar()
	{
		return this.getRuleChars().getValueDelimiterChar() ;
	} // getValueDelimiterChar() 
  
	// -------------------------------------------------------------------------

  /** Returns the character that starts a list of values ( DEFAULT = '{' ) */
  public char getValueStartChar()
  {
    return this.getRuleChars().getValueStartChar() ;
  } // getValueStartChar() 

  // -------------------------------------------------------------------------

  /** Sets the character that starts a value list */
  public void setValueStartChar( char newValue )
  {
    this.getRuleChars().setValueStartChar( newValue );
  } // setValueStartChar() 

  // -------------------------------------------------------------------------

  /** Returns the character ends a list of values ( DEFAULT = '}' ) */
  public char getValueEndChar()
  {
    return this.getRuleChars().getValueEndChar() ;
  } // getValueEndChar() 

  // -------------------------------------------------------------------------

  /** Sets the character that ends a value list */
  public void setValueEndChar( char newValue )
  {
    this.getRuleChars().setValueEndChar( newValue ) ;
  } // setValueEndChar() 

  // -------------------------------------------------------------------------

  /** Returns the character that starts a logical group ( DEFAULT = '(' ) */
  public char getGroupStartChar()
  {
    return this.getRuleChars().getGroupStartChar() ;
  } // getGroupStartChar() 

  // -------------------------------------------------------------------------

  /** Sets the character that starts a group */
  public void setGroupStartChar( char newValue )
  {
    this.getRuleChars().setGroupStartChar( newValue ) ;
  } // setGroupStartChar() 

  // -------------------------------------------------------------------------

  /** Returns the character that ends a logical group ( DEFAULT = ')' ) */
  public char getGroupEndChar()
  {
    return this.getRuleChars().getGroupEndChar() ;
  } // getGroupEndChar() 

  // -------------------------------------------------------------------------

  /** Sets the character that ends a group */
  public void setGroupEndChar( char newValue )
  {
    this.getRuleChars().setGroupEndChar( newValue ) ;
  } // setGroupEndChar() 

  // -------------------------------------------------------------------------

  /** Returns the character for greater than comparisons ( DEFAULT = '>' ) */
  public char getGreaterChar()
  {
    return this.getRuleChars().getGreaterChar() ;
  } // getGreaterChar() 

  // -------------------------------------------------------------------------

	  /** Sets the character that is used to compare if a value is greater than another */
  public void setGreaterChar( char newValue ) 
  { 
  	this.getRuleChars().setGreaterChar( newValue ) ; 
  } // setGreaterChar() 

	// -------------------------------------------------------------------------

  /** Returns the character for less than comparisons ( DEFAULT = '<' ) */
  public char getLessChar()
  {
    return this.getRuleChars().getLessChar() ;
  } // getLessChar() 

  // -------------------------------------------------------------------------

 /** Sets the character that is used to compare if a value is less than another */
  public void setLessChar( char newValue ) 
  { 
  	this.getRuleChars().setLessChar( newValue ) ; 
  } // setLessChar() 

	// -------------------------------------------------------------------------
	
  /** Returns the character for equals comparisons ( DEFAULT = '=' ) */
  public char getEqualsChar()
  {
    return this.getRuleChars().getEqualsChar() ;
  } // getEqualsChar() 

  // -------------------------------------------------------------------------

  /** Sets the character that is used to compare if two values are equal  */
  public void setEqualsChar( char newValue ) 
  { 
  	this.getRuleChars().setEqualsChar( newValue ) ; 
  } // setEqualsChar() 

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  /**
   * Parse the given rule string to a MatchGroup which can be used to
   * create a MatchRule.
   * 
   * @param rule The rule in a string compliant to the MatchRule syntax
   * @throws MatchRuleParseException Each syntax error in the given rule causes
   * 																	this exception with a short description
   * 																	of what is wrong
   */
	protected MatchGroup parseToGroup( String rule )
		throws MatchRuleParseException
	{
		StringBuffer buffer ;
		MatchGroup group ;
		
		if ( rule == null )
			return null ;

    buffer = new StringBuffer( rule.length() + 2 ) ;
    buffer.append( this.getGroupStartChar() ) ;
    buffer.append( rule ) ;
    buffer.append( this.getGroupEndChar() ) ;
			
		this.scanner( new StringScanner( buffer.toString() ) ) ;
		group = this.parseGroup() ;
		this.checkExpectedEnd( this.scanner().peek() ) ;
		//group.optimize() ;
		return group ;
	} // parseToGroup() 

	// -------------------------------------------------------------------------
  
  // ******** GROUP PARSING **************************************************

  protected MatchGroup parseGroup()
    throws MatchRuleParseException
  {
  	MatchGroup group ;
    char ch             = ' ' ;
    boolean enclosed    = false ;

		group = new MatchGroup() ;
    this.readOperators( group ) ;
    ch = this.scanner().nextNoneWhitespaceChar() ;
    if ( ch == this.getGroupStartChar() )
      enclosed = true ;
    else
      this.scanner().skip( -1 ) ;

    this.readElements( group ) ;
    ch = this.scanner().peek() ;
    if ( enclosed )
    {
			this.checkUnexpectedEnd( ch ) ;
      if ( this.isGroupEnd(ch) )
      {
        this.scanner().nextChar() ;
      }
      else
      {
				this.scanner().skip(-1) ;
        this.throwException( "Expected '" + this.getGroupEndChar()
             + "', but found '" + ch + "' at position " 
             + this.scanner().getPosition() ) ;
      }
    }
    else
    {
     	this.checkExpectedEnd(ch) ;
    }
    return group ;
  } // parseGroup() 

  // -------------------------------------------------------------------------

  protected void readElements( MatchGroup group )
    throws MatchRuleParseException
  {
    char ch         = ' ' ;

    ch = this.scanner().peek() ;
    this.checkUnexpectedEnd(ch) ;
    do
    {
      group.addElement( this.readElement() ) ;
      ch = this.scanner().nextNoneWhitespaceChar() ;
			if ( ! this.atEnd( ch ) )
				this.scanner().skip(-1);
    } while (  this.isOperator(ch ) ) ;

  } // readElements() 

  // -------------------------------------------------------------------------

  protected MatchElement readElement()
    throws MatchRuleParseException
  {
    MatchElement element = null ;

    if ( this.nextIsGroupElement() )
    {
      element = this.parseGroup() ;
    }
    else
    {
      element = this.parseAttribute() ;
    }
    return element ;
  } // readElement() 

  // -------------------------------------------------------------------------

  protected boolean nextIsGroupElement()
    throws MatchRuleParseException
  {
    char ch         = ' ' ;
    boolean isGroup = false ;

    this.scanner().markPosition() ;

    ch = this.scanner().nextNoneWhitespaceChar() ;
    while ( ( this.isOperator(ch) ) || ( ch == this.getNotChar() ) )
    {
      ch = this.scanner().nextNoneWhitespaceChar() ;
    }

    if ( ch == this.getGroupStartChar() )
    {
      isGroup = true ;
    }
    this.scanner().restorePosition() ;
    return isGroup ;
  } // nextIsGroupElement() 

  // -------------------------------------------------------------------------
  
  // ******** ATTRIBUTE PARSING **********************************************
  
  protected MatchAttribute parseAttribute()
    throws MatchRuleParseException
  {
  	MatchAttribute attr ;
  	char ch ;
  	
    if ( this.scanner().length() < 3 )  // at least "x=*"
    {
      this.throwException( "Impossible length for attribute at "
                          + "position : " + this.scanner().getPosition() ) ;
    }

    attr = new MatchAttribute() ;
    this.readOperators( attr ) ;
    this.readAttributeName( attr ) ;
    ch = this.readCompareOperator( attr ) ;
    if ( ch == this.getValueStartChar() )
    {
    	this.readMatchValues( attr ) ;
    }
    else
    {
    	this.readMatchValue( attr ) ;
    }  	
    return attr ;
  } // parseAttribute() 

  // -------------------------------------------------------------------------

  protected void readAttributeName( MatchAttribute attribute )
    throws MatchRuleParseException
  {
    StringBuffer strbuf = new StringBuffer(40) ;
    char ch   = this.scanner().nextNoneWhitespaceChar() ;
    
    while ( this.isValidNameCharacter(ch) )
    {
      strbuf.append( ch ) ;
      ch = this.scanner().nextChar() ;
    }
    
    if ( ch == StringUtil.CH_SPACE )
		{
			ch = this.scanner().nextNoneWhitespaceChar() ;
		}
    this.checkUnexpectedEnd(ch) ;

    if ( this.isValidCompareOperator( ch ) )
    {
      this.scanner().skip(-1) ; // To get back to start of operator 
    }
    else
    {
      this.throwException( "Invalid character '" + ch + "' in "
                  + "attribute \"" + this.scanner().toString() + "\"" ) ;
    }
    
    attribute.setAttributeName( strbuf.toString() ) ;
  } // readAttributeName() 

  // -------------------------------------------------------------------------

	protected boolean isValidCompareOperator( char ch )
	{
		if ( ch == this.getValueStartChar() )
			return true ;
			
		if ( ch == this.getEqualsChar() )
			return true ;
			
		if ( ch == this.getGreaterChar() )
			return true ;

		if ( ch == this.getLessChar() )
			return true ;
			
		return false ;			
	} // isValidCompareOperator() 

	// -------------------------------------------------------------------------

	protected char readCompareOperator( MatchAttribute attribute )
    throws MatchRuleParseException
	{
		char ch ;
		
    ch = this.scanner().nextNoneWhitespaceChar() ;
    this.checkUnexpectedEnd(ch) ;

		if ( ( ch == this.getValueStartChar() ) || ( ch == this.getEqualsChar() ) )
		{
			attribute.setEqualsOperator() ;
			return ch ;
		}	
		
		if ( ch == this.getGreaterChar() )
		{
			if ( this.scanner().peek() == this.getEqualsChar() )
			{
				ch = this.scanner().nextChar() ;
				attribute.setGreaterOrEqualOperator() ;
			}
			else
			{
				attribute.setGreaterOperator() ;
			}
			return ch ;
		}

		if ( ch == this.getLessChar() )
		{
			if ( this.scanner().peek() == this.getEqualsChar() )
			{
				ch = this.scanner().nextChar() ;
				attribute.setLessOrEqualOperator() ;
			}
			else
			{
				attribute.setLessOperator() ;
			}
			return ch ;
		}
			
    this.throwException( "Invalid compare operator '" + ch + "' after "
             + "attribute \"" + this.scanner().toString() + "\"" ) ;
    return ch ;
	} // readCompareOperator() 

	// -------------------------------------------------------------------------

	protected void readMatchValues( MatchAttribute attribute )
		throws MatchRuleParseException
	{
		List patterns 							= new ArrayList() ;
		StringPattern[] strPatterns = null ;
		char ch ;
		StringPattern pattern ;
    
		do
		{
			pattern = this.readMatchValue() ;
			patterns.add( pattern ) ;
    	
			ch = this.scanner().nextNoneWhitespaceChar() ;
			this.checkUnexpectedEnd( ch ) ;
    	
			if ( ( ch != this.getValueSeparatorChar() ) 
				&& ( ch != this.getValueEndChar() ) )
			{
				this.scanner().skip(-1) ;
				this.throwException( "Expected '" + this.getValueSeparatorChar() 
									+ "' or '" + this.getValueEndChar() + "' at position "
									+ this.scanner().getPosition() 
									+ " but found '" + ch + "'" ) ;
			}
		} while ( ch != this.getValueEndChar() ) ;

		strPatterns = new StringPattern[patterns.size()] ;
		patterns.toArray( strPatterns ) ;
		attribute.setPatterns( strPatterns ) ;
	} // readMatchValues() 
  
	// -------------------------------------------------------------------------

	protected void readMatchValue( MatchAttribute attribute )
		throws MatchRuleParseException
	{
		StringPattern pattern ;

		pattern = this.readMatchValue() ;					
		attribute.setPattern( pattern ) ;
	} // readMatchValue() 
  
	// -------------------------------------------------------------------------

	protected StringPattern readMatchValue()
		throws MatchRuleParseException
	{
		String value ;
		char ch ;
    
		ch = this.scanner().nextNoneWhitespaceChar() ;
		this.checkUnexpectedEnd(ch) ;
    
		if ( ch == this.getValueDelimiterChar() )
		{
			value = this.readDelimitedMatchValue() ;
		}
		else
		{
			value = this.readUndelimitedMatchValue( ch ) ;
		}		
		return new StringPattern( value ) ;					
	} // readMatchValue() 
  
	// -------------------------------------------------------------------------

	protected String readDelimitedMatchValue()
		throws MatchRuleParseException
	{
		StringBuffer strbuf ;
		char delimiter ;
		char ch ;
		
		delimiter = this.getValueDelimiterChar() ;
		strbuf = new StringBuffer(40) ;
		
		ch = this.scanner().nextChar() ;
		this.checkUnexpectedEnd( ch ) ;
		
		while ( ch != delimiter )
		{	
			strbuf.append( ch ) ;
			ch = this.scanner().nextChar() ;
			this.checkUnexpectedEnd( ch ) ;
		} 
		return strbuf.toString() ;
	} // readDelimitedMatchValue() 
  
	// -------------------------------------------------------------------------

	protected String readUndelimitedMatchValue( char ch )
		throws MatchRuleParseException
	{
		StringBuffer strbuf ;

		strbuf = new StringBuffer(40) ;
		while ( this.isPartOfValue(ch) )
		{
			strbuf.append( ch ) ;
			ch = this.scanner().nextChar() ;
		}

		if ( ! this.atEnd(ch) )
			this.scanner().skip(-1) ;

		return strbuf.toString().trim() ;
	} // readUndelimitedMatchValue() 
  
	// -------------------------------------------------------------------------

	/**
	 * Returns true if the given character can be part of a value.
	 * Returns false if the character is a special character that terminates 
	 * a value.
	 */
	protected boolean isPartOfValue( char ch )
	{
		if ( ch == this.getValueEndChar() )
			return false ;

		if ( ch == this.getValueSeparatorChar() )
			return false ;

		return ! ( this.isOperator(ch) || this.isGroupEnd(ch) || this.atEnd(ch) ) ;
	} // isPartOfValue() 

	// -------------------------------------------------------------------------

  // ******** COMMON METHODS *************************************************

	/**
	 * Return two operator flags depending on the next characters in the 
	 * scanner.
	 * 
	 * The first value defines if the operator is an AND (true) or an OR (false).
	 * The second value defines if NOT is set (true) or not (false).
	 */
  protected void readOperators( MatchElement element )
    throws MatchRuleParseException
  {
    char ch ;
    
    ch = this.scanner().nextNoneWhitespaceChar() ;
    if ( ( ch == this.getAndChar() ) || ( ch == this.getOrChar() ) )
    {
      element.setAnd( ch == this.getAndChar() ) ;
      if ( this.scanner().nextNoneWhitespaceChar() == this.getNotChar() )
      {
        element.setNot( true ) ;
      }
      else
      {
        this.scanner().skip(-1) ;
      }
    }
    else
    {
      if ( ch == this.getNotChar() )
      {
        element.setNot( true ) ;
      }
      else
      {
        this.scanner().skip(-1) ;
      }
    }
  } // readOperators() 

  // -------------------------------------------------------------------------

	protected String readUpTo( char exitChar )
    throws MatchRuleParseException
	{
    StringBuffer strbuf ;
    char ch    ;
    
    strbuf = new StringBuffer(100) ;
    ch = this.scanner().nextChar() ;
    while ( ch != exitChar )
    {
      this.checkUnexpectedEnd(ch) ;
      strbuf.append( ch ) ;
      ch = this.scanner().nextChar() ;
    }
		return strbuf.toString() ;
	} // readUpTo() 

	// -------------------------------------------------------------------------

  protected boolean isOperator( char ch )
  {
    return ( ( ch == this.getAndChar() ) || ( ch == this.getOrChar() ) ) ;
  } // isOperator() 

  // -------------------------------------------------------------------------

	protected boolean isValidNameCharacter( char ch )
	{
		boolean valid ;
		
		valid = Character.isLetterOrDigit(ch) ;
		if ( ! valid )
		{
			valid = this.getSpecialNameCharacters().indexOf(ch) >= 0 ;
		}		
		return valid ;
	} // isValidNameCharacter() 

	// -------------------------------------------------------------------------

	protected boolean isGroupEnd( char ch )
	{
		return ( ch == this.getGroupEndChar() ) ;
	} // isGroupEnd() 

	// -------------------------------------------------------------------------

  /** Returns the special character allowed in attribute names */
  protected String getSpecialNameCharacters()
  {
    return this.getRuleChars().getSpecialNameCharacters() ;
  } // getSpecialNameCharacters() 

  // -------------------------------------------------------------------------
	
	protected MatchRule createMatchRuleOn( MatchGroup group )
	{
		MatchRule matchRule  ;
		
		matchRule = new MatchRule( group ) ;
		
		if ( this.getIgnoreCaseInNames() )
			matchRule.ignoreCaseInNames( true ) ;

		if ( this.getIgnoreCaseInValues() )
			matchRule.ignoreCase( true ) ;
						
		if ( this.getMultiCharWildcardMatchesEmptyString() )
			matchRule.multiCharWildcardMatchesEmptyString( true ) ;				
						
		return matchRule ;
	} // createMatchRuleOn() 

	// -------------------------------------------------------------------------
	
} // class DefaultMatchRuleParser 
