// ===========================================================================
// CONTENT  : CLASS StringPair
// AUTHOR   : M.Duchrow
// VERSION  : 1.1 - 21/01/2012
// HISTORY  :
//  24/03/2008  mdu  CREATED
//	21/01/2012	mdu		changed	-->	Added impl IJSONConvertible
//
// Copyright (c) 2008-2012, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.bif.text.IJSONConvertible;
import org.pf.bif.text.IMutableStringPair ;
import org.pf.bif.text.IStringPair;

/**
 * A simple pair of strings.
 *
 * @author M.Duchrow
 * @version 1.1
 */
public class StringPair implements IMutableStringPair, IJSONConvertible
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	private static final JSONUtil JU = JSONUtil.current();
	
	public static final StringPair[] EMPTY_ARRAY = new StringPair[0] ;
	
	/**
	 * Defines the hash code of instances that contain two null strings.
	 */
	public static final int NULL_HASHCODE = Void.TYPE.hashCode() ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String string1 = null ;
  public String getString1() { return string1 ; }
  public void setString1( String newValue ) { string1 = newValue ; }
  
  private String string2 = null ;
  public String getString2() { return string2 ; }
  public void setString2( String newValue ) { string2 = newValue ; }
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public StringPair()
  {
    super() ;
  } // StringPair() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with one string.
   * That is, both strings get initialized with the given string.
   */
  public StringPair( String string )
  {
  	super() ;
  	this.setString1( string ) ;
  	this.setString2( string ) ;
  } // StringPair() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with two strings.
   */
  public StringPair( String s1, String s2 )
  {
  	super() ;
  	this.setString1( s1 ) ;
  	this.setString2( s2 ) ;
  } // StringPair() 
  
  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance from the given string pair.
   */
  public StringPair( IStringPair pair )
  {
  	super() ;
  	this.setString1( pair.getString1() ) ;
  	this.setString2( pair.getString2() ) ;
  } // StringPair() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with two strings in an array.
   * 
   * @throws IllegalArgumentException If the array does not contain exactly 2 elements. 
   */
  public StringPair( String[] strings )
  {
  	super() ;
  	if ( ( strings == null ) || ( strings.length != 2 )  )
		{
			throw new IllegalArgumentException( "The given strings array does not exactly consist of 2 string elements!" ) ;
		}
  	this.setString1( strings[0] ) ;
  	this.setString2( strings[1] ) ;
  } // StringPair() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns true if the given object is an IStringPair and its both strings
   * are equal to the corresponding two strings of this object.
   * That applies also to null values in the string variables.
   */
  public boolean equals( Object obj )
  {
  	IStringPair pair ;
  	
  	if ( obj instanceof IStringPair )
  	{
  		pair = (IStringPair) obj ;
  		return this.isEqual( this.getString1(), pair.getString1() )
  				&& this.isEqual( this.getString2(), pair.getString2() ) ;
  	}
  	return false ;
  } // equals() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns a hash built over the two contained strings.
   */
  public int hashCode() 
  {
  	int hash = NULL_HASHCODE ;
  	
  	hash += 1 ;
  	if ( this.getString1() != null )
  	{
  		hash = hash ^ this.getString1().hashCode() ;
  	}
  	hash += 2 ;
  	if ( this.getString2() != null )
  	{
  		hash = hash ^ this.getString2().hashCode() ;
  	}
  	return hash ;
  } // hashCode() 
  
  // -------------------------------------------------------------------------
  
	/**
	 * Appends the internal state as JSON string representation to the given buffer.
	 * 
	 * @param buffer The buffer to which to append the JSON string (must not be null).
	 */
  public void appendAsJSONString(StringBuffer buffer)
  {
  	buffer.append(JSON_OBJECT_START);
  	JU.appendJSONPair(buffer, this.getString1(), this.getString2());
  	buffer.append(JSON_OBJECT_END);
  } // appendAsJSONString()
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns a JSON string representation of this object.
   * @return JSON object: {"string1":"string2"}
   */
  public String toJSON()
  {
  	return JU.convertToJSON(this);
  } // asJSONString()
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns both strings of this pair in a String array.
   */
  public String[] asArray()
  {
  	String[] strings ;
  	
  	strings = new String[2] ;
  	strings[0] = this.getString1() ;
  	strings[1] = this.getString2() ;
  	return strings;
  } // asArray() 
  
  // -------------------------------------------------------------------------

  /**
   * Returns the two strings as one string separated by the given separator.
   * If the provided separator is null, no separator should be put between
   * the strings.
   * 
   * @param separator A separator to be placed between the two strings.
   */
  public String asString( String separator )
  {
  	StringBuffer buffer ;
  	int len = 0;
  	String s1 ;
  	String s2 ;
  	
  	s1 = this.getString1() ;
  	if ( s1 == null )
		{
  		s1 = StringUtil.EMPTY_STRING ;
		}
		else
		{
			len += s1.length() ;
		}
  	s2 = this.getString2() ;
  	if ( s2 == null )
  	{
  		s2 = StringUtil.EMPTY_STRING ;  		
  	}
		else
		{
			len += s2.length() ;
		}
  	if ( separator == null )
		{
			separator = StringUtil.EMPTY_STRING ;
		}
		else
		{
			len += separator.length() ;
		}
  	buffer = new StringBuffer(len) ;
  	buffer.append(s1) ;
  	buffer.append(separator) ;
  	buffer.append(s2) ;
  	return buffer.toString() ;
  } // asString() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the two strings with the default separator in between.
   */
  public String toString()
  {
  	return this.asString( this.getDefaultSeparator() ) ;
  } // toString() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the default separator (here ":").
   */
  public String getDefaultSeparator() 
	{
		return ":" ;
	} // getDefaultSeparator() 
	
	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected boolean isEqual( String s1, String s2 ) 
	{
  	if ( s1 == null )
		{
			return ( s2 == null ) ;
		}
  	if ( s2 == null )
		{
			return false ;
		}
  	return s1.equals( s2 ) ;
	} // isEqual() 
	
	// -------------------------------------------------------------------------
  
} // class StringPair 
