// ===========================================================================
// CONTENT  : CLASS StringExaminer
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 01/06/2008
// HISTORY  :
//  29/09/2002  duma  CREATED
//	01/06/2008	mdu		added		-->	findPositionOf(), upToPosition()
//
// Copyright (c) 2002-2008, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * As a subclass of StringScanner this class allows more advanced navigation 
 * over the underlying string.    <br>
 * That includes moving to positions of specific substrings etc.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class StringExaminer extends StringScanner
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================


  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private boolean ignoreCase = false ;
  protected boolean ignoreCase() { return ignoreCase ; }
  protected void ignoreCase( boolean newValue ) { ignoreCase = newValue ; }
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================


  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with the string to examine.   <br>
   * The string will be treated case-sensitive.
   * 
   * @param stringToExamine The string that should be examined
   */
  public StringExaminer( String stringToExamine )
  {
    this( stringToExamine, false ) ;
  } // StringExaminer() 

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with the string to examine.
   * 
   * @param stringToExamine The string that should be examined
   * @param ignoreCase Specified whether or not treating the string case insensitive
   */
  public StringExaminer( String stringToExamine, boolean ignoreCase )
  {
    super( stringToExamine ) ;
    this.ignoreCase( ignoreCase ) ;
  } // StringExaminer() 

  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
 	/**
	 * Increments the position pointer up to the last character that matched
	 * the character sequence in the given matchString.
	 * Returns true, if the matchString was found, otherwise false.
	 * <p>
	 * If the matchString was found, the next invocation of method nextChar()
	 * returns the first character after that matchString.
	 * 
	 * @param matchString The string to look up
	 */
	public boolean skipAfter( String matchString )
	{
		int index = 0 ;
		
		if ( ( matchString == null ) || ( matchString.length() == 0 ) )
		{
			return false ;
		}
	
		index = this.findPositionOf( matchString ) ;
		if ( index < 0 )
		{
			return false ;
		}
		this.setPosition( index + matchString.length() ) ;
		return true;
	} // skipAfter() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the position index of the given match string starting to look
	 * for it from the current position.
	 * If the given string cannot be found -1 will be returned.
	 * This method does not change the position counter.
	 */
	public int findPositionOf( String matchString )
	{
		char ch			= '-' ;
		char matchChar = ' ' ;
		int startPos = -1 ;
		int foundPos = -1 ;
		int index = 0 ;
		int savedPosition ;
		
		if ( matchString == null )
		{
			return -1 ;
		}
		if ( matchString.length() == 0 )
		{
			return this.getPosition() ;
		}
		
		savedPosition = this.getPosition() ;
		try
		{			
			ch = this.nextChar() ;
			while ( ( endNotReached( ch ) ) && ( foundPos < 0 ) )  
			{
				matchChar = matchString.charAt( index ) ;
				if ( this.charsAreEqual( ch, matchChar ) )
				{
					if ( startPos < 0 )
					{
						startPos = this.getPosition() - 1 ;
					}
					index++ ;
					if ( index >= matchString.length() ) // whole matchString checked ?
					{
						foundPos = startPos ;
					}
					else
					{
						ch = this.nextChar() ;
					}
				}
				else
				{
					startPos = -1 ;
					if ( index == 0 )
					{
						ch = this.nextChar() ;
					}
					else
					{
						index = 0 ;	
					}
				}
			}
			return foundPos ;
		}
		finally
		{
			this.setPosition( savedPosition ) ;
		}
	} // findPositionOf() 
	
	// -------------------------------------------------------------------------
	
 	/**
	 * Increments the position pointer up to the first character before
	 * the character sequence in the given matchString.
	 * Returns true, if the matchString was found, otherwise false.
	 * <p>
	 * If the matchString was found, the next invocation of method nextChar()
	 * returns the first character of that matchString from the position where
	 * it was found inside the examined string.
	 * 
	 * @param matchString The string to look up
	 */
	public boolean skipBefore( String matchString )
	{
		boolean found ;
		
		found = this.skipAfter( matchString ) ;
		if ( found )
			this.skip( 0 - matchString.length() ) ;
			
		return found ;
	} // skipBefore() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the a string containing all characters from the current position
	 * up to the end of the examined string.   <br>
	 * The character position of the examiner is not changed by this
	 * method.
	 */
	public String peekUpToEnd()
	{
		return this.upToEnd( true ) ;
	} // peekUpToEnd() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the a string containing all characters from the current position
	 * up to the end of the examined string.   <br>
	 * The character position is put to the end by this method.
	 * That means the next invocation of nextChar() returns END_REACHED.
	 */
	public String upToEnd()
	{
		return this.upToEnd( false ) ;
	} // upToEnd() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the a string containing all characters from the current position
	 * up to the specified position of the examined string.   <br>
	 * The character position of the examiner after calling this method is pos.
	 */
	public String peekUpToPosition( int pos )
	{
		StringBuffer strBuffer ;
		
		strBuffer = new StringBuffer( this.length() - this.getPosition() + 1 ) ;
		this.appendUpToPosition( strBuffer, pos, true ) ;
		return strBuffer.toString() ;
	} // peekUpToPosition() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the a string containing all characters from the current position
	 * up to the specified position of the examined string.   <br>
	 * The character position of the examiner after calling this method is 
	 * unchanged.
	 */
	public String upToPosition( int pos )
	{
		StringBuffer strBuffer ;
		
		strBuffer = new StringBuffer( this.length() - this.getPosition() + 1 ) ;
		this.appendUpToPosition( strBuffer, pos ) ;
		return strBuffer.toString() ;
	} // upToPosition() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Appends all characters from the current position
	 * up to the specified position of the examined string to the given buffer.   
	 * <br>
	 * The character position of the examiner after calling this method is pos.
	 */
	public void appendUpToPosition( final StringBuffer strBuffer, final int pos )
	{
		this.appendUpToPosition( strBuffer, pos, false ) ;
	} // appendUpToPosition() 
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  protected boolean charsAreEqual( char char1, char char2)
	{
		return ( this.ignoreCase() ) 
							? ( Character.toUpperCase(char1) == Character.toUpperCase( char2 ) )
							: ( char1 == char2 ) ;
	} // charsAreEqual() 

  // -------------------------------------------------------------------------
  
	/**
	 * Returns the a string containing all characters from the current position
	 * up to the end of the examined string.   <br>
	 * Depending on the peek flag the character position of the examiner 
	 * is unchanged (true) after calling this method or points behind the strings
	 * last character.
	 */
	protected String upToEnd( boolean peek )
	{
		char result			= '-' ;
		int lastPosition = 0 ;
		StringBuffer tempBuffer = new StringBuffer( 100 ) ;
		
		lastPosition = this.getPosition() ;
		try
		{
			result = this.nextChar() ;
			while ( endNotReached( result ) ) 
			{
				tempBuffer.append( result ) ;
				result = this.nextChar() ;
			}
		}
		finally
		{
			if ( peek )
			{
				this.setPosition( lastPosition ) ;
			}			
		}
		return tempBuffer.toString() ;
	} // upToEnd() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the a string containing all characters from the current position
	 * up to the specified position of the examined string.   <br>
	 * Depending on the peek flag the character position of the examiner 
	 * is unchanged (true) after calling this method or points to pos.
	 */
	protected void appendUpToPosition( final StringBuffer strBuffer, final int pos, final boolean peek )
	{
		char result	;
		int lastPosition ;
		
		if ( pos <= this.getPosition() )
		{
			return ;
		}
		lastPosition = this.getPosition() ;
		try
		{
			do
			{				
				result = this.nextChar() ;
				strBuffer.append( result ) ;
			}
			while ( this.getPosition() < pos );
		}
		finally
		{
			if ( peek )
			{
				this.setPosition( lastPosition ) ;
			}			
		}
	} // appendUpToPosition() 
	
	// -------------------------------------------------------------------------
	
} // class StringExaminer 
