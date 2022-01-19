// ===========================================================================
// CONTENT  : CLASS StringPattern
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.7 - 21/01/2012
// HISTORY  :
//  24/01/2000  duma  CREATED
//  08/01/2002  duma  bugfix  -> Handle *xxx (equal characters after star) correctly
//  16/01/2002  duma  changed -> Implements Serializable
//	06/07/2002	duma	bugfix	-> Couldn't match "London" on "L*n"
//	19/09/2002	duma	bugfix	-> Couldn't match "MA_DR_HRBLUB" on "*_HR*"
//	19/09/2002	duma	changed	-> Using now StringExaminer instead of CharacterIterator
//	29/09/2002	duma	changed	-> Refactored: Using StringExaminer instead of StringScanner
//	26/12/2002	duma	changed	-> Comment of matches() was wrong / new hasWildcard()
//	13/02/2003	duma	added		-> setDigitWildcardChar()
//	29/09/2003	duma	added		-> equals(), hashCode(), inspectString()
//	24/10/2003	duma	changed	-> Supports that '*' matches empty strings
//	06/04/2004	duma	added		-> reject(), select()
//	19/06/2004	duma	added		-> create() methods
//	20/12/2004	duma	added		-> getDefaultSingleCharWildcard(), getDefaultMultiCharWildcard()
//	24/12/2004	duma	added		-> containsWildcard()
// 	06/05/2005  duma
//	22/12/2005	duma	added		-> allow changing of wildcard characters
//	24/02/2006	mdu		changed	-> to extend AStringFilter rather than implementing StringFilter
//	21/01/2007	mdu		added		-> copy()
//	21/01/2012	mdu		added		-> implements IJSONConvertible
//
// Copyright (c) 2000-2012, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.pf.bif.text.IJSONConvertible;

/** 
 * This class provides services for checking strings against string-patterns.
 * Currently it supports the wildcards<br>
 * '*' for any number of any character and <br>
 * '?' for any single character.
 * <p>
 * The API is very simple:<br>
 * <br>
 * There are two class methods <i>match()</i> and <i>matchIgnoreCase()</i>.
 * <br>
 * Example:
 * <br>
 * StringPattern.match( 'Hello World", "H* W*" ) ;  --> evaluates to true  <br>
 * StringPattern.matchIgnoreCase( 'StringPattern", "str???pat*" ) ;  --> evaluates to true  <br>
 * <p>
 * To be compatible with most pattern engines this class also supports that the
 * multi-char wildcard '*' matches empty string. By default it doesn't !
 * <b>
 * To switch on this behaviour call 
 * <br>pattern.multiCharWildcardMatchesEmptyString(true);
 * <br>
 * If this option is set to true, the following example returns true,
 * otherwise it would return false:
 * <pre>
 * StringPattern pattern = new StringPattern( "Fred*" ) ;
 * pattern.multiCharWildcardMatchesEmptyString(true);
 * return pattern.matches( "Fred" ) ; // <== returns true
 * </pre>
 * <p>
 * It is also possible to instantiate new pattern object by using the static 
 * <em>create()</em> methods rather than the constructors. These methods
 * will create new patterns that have the multiCharWildcardMatchesEmptyString
 * option initialized to true.
 * 
 * @author Manfred Duchrow
 * @version 2.7
 */
public class StringPattern extends AStringFilter implements Serializable, IJSONConvertible
{ 
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  protected final static char DEFAULT_MULTICHAR_WILDCARD 		= '*' ;
  protected final static char DEFAULT_SINGLECHAR_WILDCARD 	= '?' ;
  
	protected final static String SWITCH_ON						= "+" ; 
	protected final static String SWITCH_OFF					= "-" ; 

	private static final String INSPECT_PREFIX				= "StringPattern(" ;
	private static final String INSPECT_SEPARATOR			= ",\"" ; 
	private static final String INSPECT_SUFFIX				= "\")" ;
		
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private boolean ignoreCase = false ;
  /**
   * Returns whether or not the pattern matching ignores upper and lower case
   */
  public boolean getIgnoreCase() { return ignoreCase ; }  
  /**
   * Sets whether the pattern matching should ignore case or not
   */
  public void setIgnoreCase( boolean newValue ) { ignoreCase = newValue ; }  
  
  private String pattern = null ;
  /**
   * Returns the pattern as string.
   */
  public String getPattern() { return pattern ; } 
  /**
   * Sets the pattern to a new value
   */ 
  public void setPattern( String newValue ) { pattern = newValue ; }  

	// -------------------------------------------------------------------------
	
  private Character digitWildcard = null ;
  protected Character digitWildcard() { return digitWildcard ; }
  protected void digitWildcard( Character newValue ) { digitWildcard = newValue ; }
  
  // -------------------------------------------------------------------------
  
  private char singleCharWildcard = DEFAULT_SINGLECHAR_WILDCARD ;
  /**
   * Returns the wildcard character that is used as placeholder for a single
   * occurance of any character.
   */
  public char getSingleCharWildcard() { return singleCharWildcard ; }
  /**
   * Sets the wildcard character that is used as placeholder for a single
   * occurance of any character.
   */
  public void setSingleCharWildcard( char newValue ) { singleCharWildcard = newValue ; }
  
  // -------------------------------------------------------------------------
  
  private char multiCharWildcard = DEFAULT_MULTICHAR_WILDCARD ;
  /**
   * Returns the wildcard character that is used as placeholder for zero to many
   * occurances of any character(s).
   */
  public char getMultiCharWildcard() { return multiCharWildcard ; }
  /**
   * Sets the wildcard character that is used as placeholder for zero to many
   * occurances of any character(s).
   */
  public void setMultiCharWildcard( char newValue ) { multiCharWildcard = newValue ; }
  
	// -------------------------------------------------------------------------
	  
	private boolean multiCharWildcardMatchesEmptyString = false ;
	/**
	 * Returns true, if this StringPattern allows empty strings at the position
	 * of the multi character wildcard ('*').
	 * <p>
	 * The default value is false. 
	 */
	public boolean multiCharWildcardMatchesEmptyString() { return multiCharWildcardMatchesEmptyString ; }
	/**
	 * sets whether or not this StringPattern allows empty strings at the 
	 * position of the multi character wildcard ('*').
	 * <p>
	 * The default value is false. 
	 */
	public void multiCharWildcardMatchesEmptyString( boolean newValue ) { multiCharWildcardMatchesEmptyString = newValue ; }
	  
  // =========================================================================
  // PUBLIC CLASS METHODS
  // =========================================================================
  /**
   * Returns a new instance with the string pattern.
   * The default is case sensitive checking.
   * <br>
   * The option multiCharWildcardMatchesEmptyString will be initialized to true.
   *
   * @param pattern The pattern to check against ( May contain '*' and '?' wildcards )
   */
  public static StringPattern create( String pattern )
  {
  	return create( pattern, false) ;
  } // create() 

	// -------------------------------------------------------------------------

  /**
   * Returns a new instance with the string pattern and the selection,
   * if case should be ignored when comparing characters.
   * <br>
   * The option multiCharWildcardMatchesEmptyString will be initialized to true.
   *
   * @param pattern The pattern to check against ( May contain '*' and '?' wildcards )
   * @param ignoreCase Definition, if case sensitive character comparison or not.
   */
  public static StringPattern create( String pattern, boolean ignoreCase )
  {
  	StringPattern thePattern ;
  	
  	thePattern = new StringPattern( pattern, ignoreCase ) ; 
  	thePattern.multiCharWildcardMatchesEmptyString(true) ;
  	return thePattern ;
  } // create() 

  // -------------------------------------------------------------------------

  /**
   * Returns anew instance with the string pattern and a digit wildcard 
   * character.
   * The default is case sensitive checking.
   * <br>
   * The option multiCharWildcardMatchesEmptyString will be initialized to true.
   *
   * @param pattern The pattern to check against ( May contain '*', '?' wildcards and the digit wildcard )
   * @param digitWildcard A wildcard character that stands as placeholder for digits
   */
  public static StringPattern create( String pattern, char digitWildcard )
  {
  	return create( pattern, false, digitWildcard ) ;
  } // create() 

	// -------------------------------------------------------------------------

  /**
   * Returns a new instance with the string pattern and the selection,
   * if case should be ignored when comparing characters plus a wildcard 
   * character for digits.
   * <br>
   * It is also configured to match the wildcard '*' to empty strings.
   * <p>
   * Example:
   * <br>
   * StringPattern.create( "*London*Eye#", true, '#' ).matches( "londonEYE8" )  ==> true 
   *
   * @param pattern The pattern to check against ( May contain '*' and '?' wildcards )
   * @param ignoreCase Definition, if case sensitive character comparison or not.
   * @param digitWildcard A wildcard character that stands as placeholder for digits
   */
  public static StringPattern create( String pattern, boolean ignoreCase, char digitWildcard )
  {
  	StringPattern thePattern ;
  	
  	thePattern = new StringPattern( pattern, ignoreCase, digitWildcard ) ; 
  	thePattern.multiCharWildcardMatchesEmptyString(true) ;
  	return thePattern ;
  } // create() 

  // -------------------------------------------------------------------------

  /**
   * Returns true, if the given probe string matches the given pattern.  <br>
   * The character comparison is done case sensitive.
   *
   * @param probe The string to check against the pattern.
   * @param pattern The patter, that probably contains wildcards ( '*' or '?' )
   */
  public static boolean match( String probe, String pattern )
	{
		StringPattern stringPattern = new StringPattern( pattern, false ) ;
		return ( stringPattern.matches( probe ) ) ;
	} // match() 

  // -------------------------------------------------------------------------

  /**
   * Returns true, if the given probe string matches the given pattern.  <br>
   * The character comparison is done ignoring upper/lower-case.
   *
   * @param probe The string to check against the pattern.
   * @param pattern The patter, that probably contains wildcards ( '*' or '?' )
   */
  public static boolean matchIgnoreCase( String probe, String pattern )
	{
		StringPattern stringPattern = new StringPattern( pattern, true ) ;
		return ( stringPattern.matches( probe ) ) ;
	} // matchIgnoreCase() 

	// -------------------------------------------------------------------------
  
	/**
	 * Returns the character that is used to specify any number of any character.
	 * The default is '*'.
	 */
	public static char getDefaultMultiCharWildcard() 
	{
		return DEFAULT_MULTICHAR_WILDCARD ;
	} // getDefaultMultiCharWildcard() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns the character that is used to specify any single character.
	 * The default is '?'.
	 */
	public static char getDefaultSingleCharWildcard() 
	{
		return DEFAULT_SINGLECHAR_WILDCARD ;
	} // getDefaultSingleCharWildcard() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the given string contains a single or multi character
	 * wildcard. Of course this checks only the default wildcard characters.
	 * 
	 * @param aString The string to check for wildcard characters
	 */
	public static boolean containsWildcard( String aString ) 
	{
		return ( aString != null ) 
				&&	(	( aString.indexOf( DEFAULT_MULTICHAR_WILDCARD ) >= 0 ) 
							||	( aString.indexOf( DEFAULT_SINGLECHAR_WILDCARD ) >= 0 ) 
						);		
	} // containsWildcard() 

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initializes the new instance with the string pattern and the selection,
   * if case should be ignored when comparing characters.
   *
   * @param pattern The pattern to check against ( May contain '*' and '?' wildcards )
   * @param ignoreCase Definition, if case sensitive character comparison or not.
   * @see #create(String, boolean)
   */
  public StringPattern( String pattern, boolean ignoreCase )
  {
  	this.setPattern( pattern ) ;
  	this.setIgnoreCase( ignoreCase ) ;  	
  } // StringPattern() 

  // -------------------------------------------------------------------------

  /**
   * Initializes the new instance with the string pattern.
   * The default is case sensitive checking.
   * <p>
   * As an alternative see also StringPattern.create() method.
   *
   * @param pattern The pattern to check against ( May contain '*' and '?' wildcards )
   * @see #create(String)
   */
  public StringPattern( String pattern )
  {
  	this( pattern, false) ;
  } // StringPattern() 

	// -------------------------------------------------------------------------

  /**
   * Initializes the new instance with the string pattern and a digit wildcard 
   * character.
   * The default is case sensitive checking.
   *
   * @param pattern The pattern to check against ( May contain '*', '?' wildcards and the digit wildcard )
   * @param digitWildcard A wildcard character that stands as placeholder for digits
   * @see #create(String, char)
   */
  public StringPattern( String pattern, char digitWildcard )
  {
  	this( pattern, false, digitWildcard ) ;
  } // StringPattern() 

	// -------------------------------------------------------------------------

  /**
   * Initializes the new instance with the string pattern and the selection,
   * if case should be ignored when comparing characters plus a wildcard 
   * character for digits.
   *
   * @param pattern The pattern to check against ( May contain '*' and '?' wildcards )
   * @param ignoreCase Definition, if case sensitive character comparison or not.
   * @param digitWildcard A wildcard character that stands as placeholder for digits
   * @see #create(String, boolean, char)
   */
  public StringPattern( String pattern, boolean ignoreCase, char digitWildcard )
  {
  	this.setPattern( pattern ) ;
  	this.setIgnoreCase( ignoreCase ) ;  	
  	this.setDigitWildcardChar( digitWildcard ) ;
  } // StringPattern() 

  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  /**
   * Tests if a specified string matches the pattern.
   *
   * @param probe The string to compare to the pattern
   * @return true if and only if the probe matches the pattern, false otherwise.
   */
	public boolean matches( String probe )
	{
		StringExaminer patternIterator	= null ;
		StringExaminer probeIterator		= null ;
		char patternCh	= '-' ;
		char probeCh	= '-' ;
		String newPattern = null ;
		String subPattern = null ;
		int charIndex = 0 ;
				
		if ( probe == null ) return false ;
		if ( ! this.multiCharWildcardMatchesEmptyString() )
		{
			if ( probe.length() == 0 )
			{
				return ( this.getPattern().length() == 0 ) ; // Bugfix 5.2.1
			}
		}
		
		patternIterator = this.newExaminer( this.getPattern() ) ;
		probeIterator = this.newExaminer( probe ) ;
		
		probeCh = probeIterator.nextChar() ;
		patternCh = this.getPatternChar( patternIterator, probeCh ) ;
		
		while ( ( this.endNotReached( patternCh ) ) &&
					  ( this.endNotReached( probeCh ) ) )
		{
						
			if ( patternCh == this.getMultiCharWildcard() ) 
			{
				patternCh = this.skipWildcards( patternIterator ) ;
				if ( this.endReached( patternCh ) )
				{
					return true ; // No more characters after multi wildcard - So everything matches
				}
				else
				{
					patternIterator.skip(-1) ; // Position to first non-wildcard character
					newPattern = this.upToEnd( patternIterator ) ;
					charIndex = newPattern.indexOf( this.getMultiCharWildcard() ) ;
					if ( charIndex >= 0 ) // Pattern contains another multi-char wildcard ? 
					{
						subPattern = newPattern.substring( 0, charIndex ) ;

						if ( this.multiCharWildcardMatchesEmptyString() )
							probeIterator.skip(-1) ; 
						
						if ( this.skipAfter( probeIterator, subPattern ) )
						{
							patternIterator = this.newExaminer( newPattern.substring( charIndex ) ) ;
							patternCh = probeCh ; // To get it through the comparison at the end of this method
						}
						else 
						{
							// The substring up to the multi-value wildcard doesn't match
							// the next characters in the probe 
							return false ;
						}
					}
					else // No more multi-char wildcard in pattern
					{
						probeIterator.skip(-1) ;
						return this.matchReverse( newPattern, probeIterator ) ;
					}
				}
			}
				
			if ( this.charsAreEqual( probeCh, patternCh ) )
			{
				if ( this.endNotReached(patternCh) )
				{
					probeCh = probeIterator.nextChar() ;
					patternCh = this.getPatternChar( patternIterator, probeCh ) ;
				}
			}
			else
			{
        if ( patternCh != this.getMultiCharWildcard() )
				{
					return false ;   // character is not matching - return immediately
				}
			}
		} // while() 
		
		return this.eventuallyMatched( probeCh, patternCh, patternIterator ) ;
	} // matches() 
		
  // -------------------------------------------------------------------------

	/**
	 * Returns an array containing all of the given strings that match this
	 * pattern.
	 * 
	 * @param strings The strings to be matched against this pattern
	 */
	public String[] select( String[] strings )
	{
		return this.selectOrReject( strings, true ) ;
	} // select() 

	// -------------------------------------------------------------------------

	/**
	 * Returns an array containing all of the given strings that do NOT match 
	 * this pattern.
	 * 
	 * @param strings The strings to be matched against this pattern
	 */
	public String[] reject( String[] strings )
	{
		return this.selectOrReject( strings, false ) ;
	} // reject() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the pattern string.
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		if ( this.getPattern() == null )
			return super.toString() ;
		else
			return this.getPattern() ;
	} // toString() 

  // -------------------------------------------------------------------------

	/**
	 * Returns true if the pattern contains any '*' or '?' or
	 * digit wildcard wildcard character.
	 */
	public boolean hasWildcard()
	{
		if ( this.getPattern() == null )
			return false ;

		if ( this.hasDigitWildcard() )
		{
			if ( this.getPattern().indexOf( this.digitWildcardChar() ) >= 0 )
				return true ;
		}

		return StringPattern.containsWildcard( this.getPattern() ) ;		
	} // hasWildcard() 

	// -------------------------------------------------------------------------

	/**
	 * Sets the given character as a wildcard character in this pattern to
	 * match only digits ('0'-'9').   <br>
	 * 
	 * @param digitWildcard The placeholder character for digits
	 */
	public void setDigitWildcardChar( char digitWildcard )
	{
		if ( digitWildcard <= 0 )
		{
			this.digitWildcard( null ) ;
		}
		else
		{
			this.digitWildcard( Character.valueOf( digitWildcard ) ) ;
		}
	} // setDigitWildcardChar() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a copy of this pattern. The values of the copy are independent
	 * of the origin. That means, changes to the copy will have no impact on 
	 * this string pattern.
	 */
	public StringPattern copy() 
	{
		StringPattern copy ;
		
		copy = new StringPattern( this.getPattern(), this.getIgnoreCase(), this.digitWildcardChar() ) ;
		copy.setSingleCharWildcard( this.getSingleCharWildcard() ) ;
		copy.setMultiCharWildcard( this.getMultiCharWildcard() ) ;
		copy.multiCharWildcardMatchesEmptyString( this.multiCharWildcardMatchesEmptyString() ) ;
		return copy ;
	} // copy() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the given object is equal to the receiver.
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj)
	{
		if ( obj instanceof StringPattern )
		{
			StringPattern otherPattern = (StringPattern)obj ;
			if ( this.getIgnoreCase() != otherPattern.getIgnoreCase() )
			{
				return false ;
			}
			return this.getPattern().equals( otherPattern.getPattern() ) ;
		}
		return false ;
	} // equals() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a hash code value for the object.
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		String temp ;
		temp = this.ignoreCaseAsString() + this.getPattern() ;
		return temp.hashCode() ;
	} // hashCode() 

	// -------------------------------------------------------------------------

	public void appendAsJSONString(StringBuffer buffer)
	{
  	JSONUtil.current().appendJSONString(buffer, this.getPattern());
	} // appendAsJSONString()

	// -------------------------------------------------------------------------

  /**
   * Returns a JSON string representation of this object.
   * @return JSON object: {"string1":"string2"}
   */
  public String toJSON()
  {
  	return JSONUtil.current().convertToJSON(this);
  } // asJSONString()
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected boolean eventuallyMatched( char probeCh, char patternCh, StringExaminer patternIterator )
	{
		char ch ;
		
		if ( ! this.endReached( probeCh ) )
		{
			return false ;
		}
		
		if ( this.endReached( patternCh ) )
		{
			return true ;
		}

		if ( this.multiCharWildcardMatchesEmptyString() )
		{
			if ( patternCh == this.getMultiCharWildcard() ) 
			{
				while( ! patternIterator.atEnd() )
				{
					ch = patternIterator.nextChar() ;
					if ( ch != this.getMultiCharWildcard() ) 
					{
						return false ;
					}
				}
				return true ;
			}
		}
		return false ;		
	} // eventuallyMatched() 

	// -------------------------------------------------------------------------
  
  protected boolean hasDigitWildcard()
	{
		return this.digitWildcard() != null ;
	} // hasDigitWildcard() 

	// -------------------------------------------------------------------------

	protected char digitWildcardChar()
	{
		if ( this.hasDigitWildcard() )
			return this.digitWildcard().charValue() ;
		else
			return '\0' ;
	} // digitWildcardChar() 

	// -------------------------------------------------------------------------

	/**
	 * Moves the iterator position to the next character that is no wildcard.
	 * Doesn't skip digit wildcards and single-char wildcards !
	 */  
	protected char skipWildcards( StringExaminer iterator )
	{
		char result			= '-' ;
		
		do
		{
			result = iterator.nextChar() ;
		}
		while ( result == this.getMultiCharWildcard() ) ;
		return result ;
	} // skipWildcards() 

  // -------------------------------------------------------------------------

 	/**
	 * Increments the given iterator up to the last character that matched
	 * the character sequence in the given matchString.
	 * Returns true, if the matchString was found, otherwise false.
	 * 
	 * @param matchString The string to be found (must not contain *)
	 */
	protected boolean skipAfter( StringExaminer examiner, String matchString )
	{
		// Do not use the method of StringExaminer anymore, because digit wildcard
		// support is in the charsAreEqual() method which is unknown to the examiner.
		// return examiner.skipAfter( matchString ) ;
		
		char ch			= '-' ;
		char matchChar = ' ' ;
		boolean found = false ;
		int index = 0 ;
		
		if ( ( matchString == null ) || ( matchString.length() == 0 ) )
			return false ;
		
		ch = examiner.nextChar() ;
		while ( ( examiner.endNotReached( ch ) ) && ( ! found ) )  
		{
			matchChar = matchString.charAt( index ) ;
			if ( this.charsAreEqual( ch, matchChar ) )
			{
				index++ ;
				if ( index >= matchString.length() ) // whole matchString checked ?
				{
					found = true ;
				}
				else
				{
					ch = examiner.nextChar() ;
				}
			}
			else
			{
				if ( index == 0 )
				{
					ch = examiner.nextChar() ;
				}
				else
				{
					index = 0 ;	
				}
			}
		}
		return found ;
	} // skipAfter() 

  // -------------------------------------------------------------------------

	protected String upToEnd( StringExaminer iterator )
	{
		return iterator.upToEnd() ;
	} // upToEnd() 

  // -------------------------------------------------------------------------

	protected boolean matchReverse( String patternStr, 
																	StringExaminer probeIterator )
	{
		String newPattern ;
		String newProbe ;
		StringPattern newMatcher ;
		
		newPattern = String.valueOf( this.getMultiCharWildcard() ) + patternStr ;
		newProbe = this.upToEnd( probeIterator ) ;
		newPattern = this.strUtil().reverse( newPattern ) ;
		newProbe = this.strUtil().reverse( newProbe ) ;
		newMatcher = new StringPattern( newPattern, this.getIgnoreCase() ) ;
		newMatcher.setSingleCharWildcard( this.getSingleCharWildcard() ) ;
		newMatcher.setMultiCharWildcard( this.getMultiCharWildcard() ) ;
		newMatcher.multiCharWildcardMatchesEmptyString( 
																	this.multiCharWildcardMatchesEmptyString() ) ;
		if ( this.hasDigitWildcard() )
			newMatcher.setDigitWildcardChar( this.digitWildcardChar() ) ;
			
		return newMatcher.matches( newProbe ) ;		
	} // matchReverse() 

	// -------------------------------------------------------------------------

  protected boolean charsAreEqual( char probeChar, char patternChar )
	{
		if ( patternChar == this.getSingleCharWildcard() )
					return true ;
					
		if ( this.hasDigitWildcard() )
		{
			if ( patternChar == this.digitWildcardChar() )
			{
				return Character.isDigit( probeChar ) ; 
			}
		}
		
		if ( this.getIgnoreCase() )
		{
			return ( Character.toUpperCase(probeChar) == Character.toUpperCase( patternChar ) ) ;
		}
		else
		{
			return ( probeChar == patternChar ) ;
		}
	} // charsAreEqual() 

  // -------------------------------------------------------------------------
  
  protected boolean endReached( char character )
	{
		return ( character == StringExaminer.END_REACHED ) ;
	} // endReached() 

  // -------------------------------------------------------------------------
  
  protected boolean endNotReached( char character )
	{
		return ( ! endReached( character ) ) ;
	} // endNotReached() 

  // -------------------------------------------------------------------------
	
	protected char getPatternChar( StringExaminer patternIterator , char probeCh )
	{
		char patternCh ;
	
		patternCh = patternIterator.nextChar() ;	
			
		return ( ( patternCh == this.getSingleCharWildcard() ) ? probeCh : patternCh ) ;
	} // getPatternChar() 
  
  // -------------------------------------------------------------------------

	protected StringExaminer newExaminer( String str )
	{
		return new StringExaminer( str, this.getIgnoreCase() ) ;
	} // newExaminer() 

	// -------------------------------------------------------------------------
	
	protected String ignoreCaseAsString()
	{
		return ( this.getIgnoreCase() ? SWITCH_ON : SWITCH_OFF ) ;
	} // ignoreCaseAsString() 

	// -------------------------------------------------------------------------
	
	protected String[] selectOrReject( String[] strings, boolean select )
	{
		List result ;
		
		if ( strings == null )
			return null ;
			
		result = new ArrayList( strings.length ) ;
		for (int i = 0; i < strings.length; i++)
		{
			if ( select == this.matches( strings[i] ) )
				result.add( strings[i] ) ;
		}
		return this.strUtil().asStrings( result ) ;	
	} // selectOrReject() 

	// -------------------------------------------------------------------------
	
	protected String inspectString()
	{
		StringBuffer buffer = new StringBuffer(50) ;
		
		buffer.append( INSPECT_PREFIX ) ;
		buffer.append( this.ignoreCaseAsString() ) ;
		buffer.append( INSPECT_SEPARATOR ) ;
		buffer.append( this.getPattern() ) ;
		buffer.append( INSPECT_SUFFIX ) ;
		
		return buffer.toString() ;
	} // inspectString() 

	// -------------------------------------------------------------------------
	
	protected StringUtil strUtil()
	{
		return StringUtil.current() ;
	} // strUtil() 

	// -------------------------------------------------------------------------
	
} // class StringPattern 
