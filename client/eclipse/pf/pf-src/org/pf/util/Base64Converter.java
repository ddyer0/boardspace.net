// ===========================================================================
// CONTENT  : CLASS Base64Converter
// AUTHOR   : M.Duchrow
// VERSION  : 1.2 - 08/03/2009
// HISTORY  :
//  22/05/2004  mdu  CREATED
//	11/06/2005	mdu		changed	-->	Due to changed underlying Base64 class
//	08/03/2009	mdu		bugfix	--> Avoid \n in long results
//
// Copyright (c) 2004-2009, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

import java.io.UnsupportedEncodingException;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * A converter that allows to encode strings, byte arrays and char arrays to
 * BASE64 and vice versa.<p>
 * <b> 
 * Currently it is based on the class Base64 from Robert Harder 
 * (http://iharder.sourceforge.net/base64).
 * Thanks to him that he published his implementation as open source.
 * </b>
 * This class mainly adds some convenience methods for string handling.
 * 
 * @author M.Duchrow
 * @version 1.2
 */
public class Base64Converter
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
	 * Returns a BASE64 encoded version of the given string
	 */
	public static String encode( String unencoded ) 
	{
		byte[] bytes ;
		
		bytes = unencoded.getBytes() ;
		return encodeToString( bytes ) ;
	} // encode() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a BASE64 encoded version of the given string, where the given string
	 * is using the specified character encoding.
	 * @throws UnsupportedEncodingException If the given char encoding i snot supported
	 */
	public static String encode( String unencoded, String charEncoding) throws UnsupportedEncodingException 
	{
		byte[] bytes ;
		
		bytes = unencoded.getBytes(charEncoding) ;
		return encodeToString( bytes ) ;
	} // encode() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns a BASE64 encoded version of the given string with new lines
	 * after all 76 characters (MIME compliant).
	 */
	public static String encodeWithLineBreaks( String unencoded ) 
	{
		byte[] bytes ;
		
		bytes = unencoded.getBytes() ;
		return encodeToStringWithLineBreaks( bytes ) ;
	} // encodeWithLineBreaks() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns a BASE64 encoded version of the given character array
	 */
	public static char[] encode( char[] unencoded ) 
	{
		String str ;
		
		str = new String( unencoded ) ;
		return encode( str.getBytes() ) ;
	} // encode() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a BASE64 encoded version of the given byte array
	 */
	public static char[] encode( byte[] unencoded ) 
	{
		return encodeToString( unencoded ).toCharArray() ;
	} // encode() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a BASE64 encoded version of the given byte array 
	 * with new lines after all 76 bytes (MIME compliant).
	 */
	public static char[] encodeWithLineBreaks( byte[] unencoded ) 
	{
		return encodeToStringWithLineBreaks( unencoded ).toCharArray() ;
	} // encodeWithLineBreaks() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns a BASE64 encoded version of the given byte array as String
	 */
	public static String encodeToString( byte[] unencoded ) 
	{
		return Base64.encodeBytes( unencoded, Base64.DONT_BREAK_LINES ) ;
	} // encodeToString() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a BASE64 encoded version of the given byte array as String
	 * with a new line (\n) after all 76 bytes (MIME compliant).
	 */
	public static String encodeToStringWithLineBreaks( byte[] unencoded ) 
	{
		return Base64.encodeBytes( unencoded ) ;
	} // encodeToStringWithLineBreaks() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns a BASE64 encoded version of the given char array as String
	 */
	public static String encodeToString( char[] unencoded ) 
	{
		char[] chars ;
		
		chars = encode( unencoded ) ;
		return new String(chars) ;
	} // encodeToString() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a byte array decoded from the given BASE64 encoded char array
	 */
	public static byte[] decode( char[] encoded ) 
	{
		return decode( new String(encoded) ) ;
	} // decode() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a byte array decoded from the given BASE64 encoded String
	 */
	public static byte[] decode( String encoded ) 
	{
		return Base64.decode( encoded ) ;
	} // decode() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a string decoded from the given BASE64 encoded String
	 * 
	 * @param encoded The BASE64 encoded string
	 */
	public static String decodeToString( String encoded )
	{
		byte[] bytes ;
		
		bytes = decode( encoded ) ;
		return new String( bytes ) ;
	} // decodeToString() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns a string decoded from the given BASE64 encoded String
	 * 
	 * @param encoded The BASE64 encoded string
	 * @param charEncoding The name of the result string's encoding (e.g. "UTF-8"). 
	 */
	public static String decodeToString(String encoded, String charEncoding) throws UnsupportedEncodingException
	{
		byte[] bytes;

		bytes = decode(encoded);
		return new String(bytes, charEncoding);
	} // decodeToString() 

	// -------------------------------------------------------------------------

	// =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  private Base64Converter()
  {
    super() ;
  } // Base64Converter() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class Base64Converter 
