// ===========================================================================
// CONTENT  : CLASS MatchRuleChars
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.6 - 20/12/2004
// HISTORY  :
//  20/07/2001  duma  CREATED
//  08/01/2002  duma  changed -> Made serializable
//	22/11/2002	duma	added		-> Special attribute name characters
//	27/12/2002	duma	added		-> Characters for equals, less and greater operators
//	04/12/2003	duma	added		-> Support for delimiter enclosed values
//	20/12/2004	duma	added		-> getSingleCharWildcard(), getMultiCharWildcard()
//
// Copyright (c) 2001-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.Serializable ;

/**
 * Contains the set of special characters for MatchRules.
 *
 * @author Manfred Duchrow
 * @version 1.6
 */
public class MatchRuleChars  implements Serializable
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  static final char DEFAULT_AND_CHAR          			= '&' ;
  static final char DEFAULT_OR_CHAR           			= '|' ;
  static final char DEFAULT_NOT_CHAR          			= '!' ;
  static final char DEFAULT_VALUE_SEP_CHAR    			= ',' ;
  static final char DEFAULT_VALUE_START_CHAR  			= '{' ;
  static final char DEFAULT_VALUE_END_CHAR    			= '}' ;
  static final char DEFAULT_GROUP_START_CHAR  			= '(' ;
  static final char DEFAULT_GROUP_END_CHAR   				= ')' ;
  static final char DEFAULT_EQUALS_CHAR   			 		= '=' ;
  static final char DEFAULT_GREATER_CHAR   					= '>' ;
  static final char DEFAULT_LESS_CHAR 	   					= '<' ;
	static final char DEFAULT_VALUE_DELIMITER_CHAR 		= '\'' ;
  static final String DEFAULT_EXTRA_ATTRNAME_CHARS  = "" ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private char andChar ;
  /** Returns the character for AND operations ( DEFAULT = '&' ) */
  public char getAndChar() { return andChar ; }
  /** Sets the character for AND operations */
  public void setAndChar( char newValue ) { andChar = newValue ; }

  private char orChar ;
  /** Returns the character for OR operations ( DEFAULT = '|' ) */
  public char getOrChar() { return orChar ; }
  /** Sets the character for OR operations */
  public void setOrChar( char newValue ) { orChar = newValue ; }

  private char notChar ;
  /** Returns the character for NOT operations ( DEFAULT = '!' ) */
  public char getNotChar() { return notChar ; }
  /** Sets the character for NOT operations */
  public void setNotChar( char newValue ) { notChar = newValue ; }

  private char valueSeparatorChar ;
  /** Returns the character for separation of values ( DEFAULT = ',' ) */
  public char getValueSeparatorChar() { return valueSeparatorChar ; }
  /** Sets the character that separates values in a value list */
  public void setValueSeparatorChar( char newValue ) { valueSeparatorChar = newValue ; }

  private char valueStartChar ;
  /** Returns the character that starts a list of values ( DEFAULT = '{' ) */
  public char getValueStartChar() { return valueStartChar ; }
  /** Sets the character that starts a value list */
  public void setValueStartChar( char newValue ) { valueStartChar = newValue ; }

  private char valueEndChar ;
  /** Returns the character ends a list of values ( DEFAULT = '}' ) */
  public char getValueEndChar() { return valueEndChar ; }
  /** Sets the character that ends a value list */
  public void setValueEndChar( char newValue ) { valueEndChar = newValue ; }

  private char groupStartChar ;
  /** Returns the character that starts a logical group ( DEFAULT = '(' ) */
  public char getGroupStartChar() { return groupStartChar ; }
  /** Sets the character that starts a group */
  public void setGroupStartChar( char newValue ) { groupStartChar = newValue ; }

  private char groupEndChar ;
  /** Returns the character that ends a logical group ( DEFAULT = ')' ) */
  public char getGroupEndChar() { return groupEndChar ; }
  /** Sets the character that ends a group */
  public void setGroupEndChar( char newValue ) { groupEndChar = newValue ; }

  private char equalsChar ;
  /** Returns the character that is used to compare if two values are equal ( DEFAULT = '=' ) */
  public char getEqualsChar() { return equalsChar ; }
  /** Sets the character that is used to compare if two values are equal  */
  public void setEqualsChar( char newValue ) { equalsChar = newValue ; }
  
  private char greaterChar ;
  /** Returns the character that is used to compare if a value is greater than another ( DEFAULT = '>' ) */
  public char getGreaterChar() { return greaterChar ; }
  /** Sets the character that is used to compare if a value is greater than another */
  public void setGreaterChar( char newValue ) { greaterChar = newValue ; }  
  
  private char lessChar ;
  /** Returns the character that is used to compare if a value is less than another ( DEFAULT = '<' ) */
  public char getLessChar() { return lessChar ; }
  /** Sets the character that is used to compare if a value is less than another */
  public void setLessChar( char newValue ) { lessChar = newValue ; }  
  
	private char valueDelimiterChar ;
	/** Returns the character that is used to enclose a value ( DEFAULT = '\'' ) */
	public char getValueDelimiterChar() { return valueDelimiterChar ; }
	/** Sets the character that is used to enclose a value */
	public void setValueDelimiterChar( char newValue ) { valueDelimiterChar = newValue ; }  
  
	// -------------------------------------------------------------------------
  
  private String specialNameCharacters = DEFAULT_EXTRA_ATTRNAME_CHARS ;
  /**
   * Returns all extra charcters, that are allowed in attribute names.
   * <br>
   * By default only [A-Z][a-z][0-9] are allowed.
   */
  public String getSpecialNameCharacters() { return specialNameCharacters ; }
  /**
   * Sets all extra charcters, that are allowed in attribute names.
   * <br>
   * By default only [A-Z][a-z][0-9] are allowed.
   * 
   * @param chars All additional characters that are allowed in attribute names (must not be null)
   */
  public void setSpecialNameCharacters( String chars ) { specialNameCharacters = chars ; }

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public MatchRuleChars()
  {
    super() ;
    this.reset() ;
  } // MatchRuleChars() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Resets all rule characters to their default values.    
   * <p>
   * That is: 
   * <ul>
   * <li>& = AND 
   * <li>| = OR
   * <li>! = NOT
   * <li>{ = start of value(s)
   * <li>, = value list separator 
   * <li>} = end of value(s)
   * <li>( = start of group
   * <li>) = end of group
   * <li>= = equals operator
   * <li>&gt; = greater than operator
   * <li>&lt; = less than operator
   * <li>' = value delimiter
   * </ul>
   * It also resets the special characters in attributes names to none ("").
   */
  public void reset()
  {
    this.setAndChar( DEFAULT_AND_CHAR ) ;
    this.setOrChar( DEFAULT_OR_CHAR ) ;
    this.setNotChar( DEFAULT_NOT_CHAR ) ;
    this.setValueSeparatorChar( DEFAULT_VALUE_SEP_CHAR ) ;
    this.setValueStartChar( DEFAULT_VALUE_START_CHAR ) ;
    this.setValueEndChar( DEFAULT_VALUE_END_CHAR ) ;
    this.setGroupStartChar( DEFAULT_GROUP_START_CHAR ) ;
    this.setGroupEndChar( DEFAULT_GROUP_END_CHAR ) ;
    this.setEqualsChar( DEFAULT_EQUALS_CHAR ) ;
    this.setGreaterChar( DEFAULT_GREATER_CHAR ) ;
    this.setLessChar( DEFAULT_LESS_CHAR ) ;
		this.setValueDelimiterChar( DEFAULT_VALUE_DELIMITER_CHAR ) ;
    this.setSpecialNameCharacters( DEFAULT_EXTRA_ATTRNAME_CHARS ) ;
  } // reset() 

  // -------------------------------------------------------------------------
  
  /**
   * Returns the character that is used as placeholder for multiple characters.
   * Currently that is '*'.
   */
  public char getMultiCharWildcard() 
	{
		return StringPattern.getDefaultMultiCharWildcard();
	} // getMultiCharWildcard() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns the character that is used as placeholder for a single character.
   * Currently that is '?'.
   */
  public char getSingleCharWildcard() 
	{
		return StringPattern.getDefaultSingleCharWildcard();
	} // getSingleCharWildcard() 

	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  // -------------------------------------------------------------------------

} // class MatchRuleChars 
