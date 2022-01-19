// ===========================================================================
// CONTENT  : CLASS CheckedCharsetName
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 23/11/2013
// HISTORY  :
//  28/08/2012  mdu  CREATED
//	23/11/2013	mdu	 added		--> getName()
//
// Copyright (c) 2012-2013, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.text ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

/**
 * This is a helper class that represents a character encoding name that has already
 * been checked if it is supported by the platform.
 * <p>
 * It helps to avoid the annoying try-catch code-pollution every time you
 * use a valid encoding, but the API requires the handling of UnsupportedEncodingException
 * even if it cannot occur at all. 
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class CheckedCharsetName implements Serializable
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	static final long serialVersionUID = 7421658387877543L;
	
	/**
	 * The pre-defined constant for Java standard charset US-ASCII.
	 */
	public static final CheckedCharsetName US_ASCII = internalCreate("US-ASCII");
	
	/**
	 * The pre-defined constant for Java standard charset ISO-8859-1.
	 */
	public static final CheckedCharsetName ISO_8859_1 = internalCreate("ISO-8859-1");
	
	/**
	 * The pre-defined constant for Java standard charset UTF-8.
	 */
	public static final CheckedCharsetName UTF_8 = internalCreate("UTF-8");

	/**
	 * The pre-defined constant for Java standard charset UTF-16.
	 */
	public static final CheckedCharsetName UTF_16 = internalCreate("UTF-16");
	
	/**
	 * The pre-defined constant for Java standard charset UTF-16BE.
	 */
	public static final CheckedCharsetName UTF_16BE = internalCreate("UTF-16BE");
	
	/**
	 * The pre-defined constant for Java standard charset UTF-16LE.
	 */
	public static final CheckedCharsetName UTF_16LE = internalCreate("UTF-16LE");
	
	/**
	 * The platform default file encoding.
	 */
	public static final CheckedCharsetName DEFAULT = internalCreate(System.getProperty("file.encoding"));
	
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private String charsetName;

	// =========================================================================
	// CLASS METHODS
	// =========================================================================
	/**
	 * Checks whether or not the given encoding is supported.
	 * 
	 * @param charsetName The name of the encoding to check (e.g. "UTF-16")
	 * @throws UnsupportedEncodingException Will be thrown if the encoding is not supported.
	 */
	public static void checkEncoding(String charsetName) throws UnsupportedEncodingException 
	{
		if (charsetName == null)
		{
			throw new UnsupportedEncodingException("<null> is no valid charset name!");
		}
		URLEncoder.encode("http://www.programmers-friend.org", charsetName);
	} // checkEncoding() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Creates a new instance based on the given encoding name.
	 * @param encodingName The name of the encoding to check (e.g. "UTF-16")
	 * @return Returns a new instance of CheckedCharsetName after checking the given charset name
	 * @throws UnsupportedEncodingException Will be thrown if the encoding is not supported.
	 */
	public static CheckedCharsetName create(String encodingName) throws UnsupportedEncodingException 
	{
		return new CheckedCharsetName(encodingName);
	} // create() 
	
	// -------------------------------------------------------------------------
	
	private static CheckedCharsetName internalCreate(String encodingName) 
	{
		try
		{
			return new CheckedCharsetName(encodingName);
		}
		catch (UnsupportedEncodingException ex)
		{
			ex.printStackTrace();
			return null;
		}
	} // internalCreate() 
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with an encoding name (e.g. "UTF-8").
   */
  public CheckedCharsetName(String encodingName) throws UnsupportedEncodingException
  {
    super() ;
    CheckedCharsetName.checkEncoding(encodingName);
    this.charsetName = encodingName.toUpperCase();
  } // CheckedCharsetName() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the pure upper case encoding name (e.g. "UTF-8").
   */
  public String getName() 
	{
  	return this.charsetName;
	} // getName()
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns the corresponding charset object.
   */
  public Charset getCharset() 
	{
  	return Charset.forName(this.getName());
	} // getCharset()
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns the pure upper case encoding name (e.g. "ISO-8859-1").
   */
  @Override
  public String toString()
  {
  	return this.getName();
  } // toString() 
  
  // -------------------------------------------------------------------------
  
  @Override
  public boolean equals(Object obj)
  {
  	if (obj instanceof CheckedCharsetName)
		{
  		CheckedCharsetName checkedName = (CheckedCharsetName)obj;
  		return this.getName().equals(checkedName.getName());
		}
  	return false;
  } // equals() 
  
  // -------------------------------------------------------------------------
  
  @Override
  public int hashCode()
  {
  	return this.getName().hashCode();
  } // hashCode() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns a new String initialized with the given bytes which are expected 
   * to be encoded according to the character set this instance represents,
   * avoiding the unnecessary exception handling (UnsupportedEncodingException).
   * <p>
   * It is the same like: new String(bytes, checkedCharsetName.toString());
   * 
   * @param bytes The byte to create a string from.
   * @return A new string built from the given bytes that were encoded with the charset of this instance.
   */
  public String newString(byte[] bytes) 
	{
		try
		{
			return new String(bytes, this.charsetName);
		}
		catch (UnsupportedEncodingException ex)
		{
			// Ignore this. Could not happen.
			return null;
		}
	} // newString()
	
	// -------------------------------------------------------------------------

  /**
   * Returns the bytes of the given string as array encoded to the charset 
   * this instance represents, avoiding the unnecessary exception handling
   * (UnsupportedEncodingException).
   * 
   * @param string The string to be converted to a byte array using this instance's charset.
   * @return The encoded byte array.
   */
  public byte[] getBytes(String string) 
	{
		try
		{
			return string.getBytes(this.charsetName);
		}
		catch (UnsupportedEncodingException ex)
		{
			// Ignore this. Could not happen.
			return null;
		}
	} // getBytes()
	
	// -------------------------------------------------------------------------
  
} // class CheckedCharsetName 
