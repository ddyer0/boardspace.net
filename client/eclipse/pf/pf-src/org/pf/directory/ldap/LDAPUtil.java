// ===========================================================================
// CONTENT  : CLASS LDAPUtil
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 25/06/2006
// HISTORY  :
//  24/04/2004  mdu  CREATED
//	25/06/2006	mdu		added		-->	createTimestamp() methods
//
// Copyright (c) 2004-2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.directory.ldap ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.text.SimpleDateFormat;
import java.util.Date;

import org.pf.text.StringPattern;
import org.pf.text.StringUtil;
import org.pf.util.Base64Converter;

/**
 * Provides utility and convenience methods for LDAP directory data.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class LDAPUtil
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	/**
	 * The distinguished name identifier (i.e. "dn")
	 */
	public static final String DISTINGUISHED_NAME		= "dn" ;

	/**
	 * The pattern that an encrypted string usually matches (e.g. "{SHA}rFg5TesR3F==") 
	 */
	public static final StringPattern ENCRYPTION_PATTERN = new StringPattern( "{*}*" ) ;
	
	
	protected static final String ATTRIBUTE_ASSIGNMENT = "=";
	protected static final String DN_ELEMENT_SEPARATOR = ",";

	protected static final SimpleDateFormat LDAP_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss") ; 
	
  // =========================================================================
  // CLASS VARIABLES
  // =========================================================================
  private static LDAPUtil soleInstance = new LDAPUtil() ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  /**
   * Returns the only instance this class supports (design pattern "Singleton")
   */
  public static LDAPUtil current()
  {
    return soleInstance ;
  } // current() 

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  protected LDAPUtil()
  {
    super() ;
  } // LDAPUtil() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Removes all unnecessary spaces from the given DN.
   * <p>
   * Example:<br>
   * normalizeDN( "cn= Peter Pan, ou=people, dcx = examples , dc=com " )
   * <br>
   * ==> "cn=Peter Pan,ou=people,dcx=examples,dc=com"
   * 
   * @param dn The dn to remove the spaces from
   */
	public String normalizeDN( String dn ) 
	{
		String[] parts ;
		String[] keyValue ;
		
		parts = this.str().parts( dn, DN_ELEMENT_SEPARATOR ) ;
		for (int i = 0; i < parts.length; i++ )
		{
			keyValue = this.str().splitNameValue( parts[i], ATTRIBUTE_ASSIGNMENT ) ;
			parts[i] = keyValue[0].trim() + ATTRIBUTE_ASSIGNMENT + keyValue[1].trim() ;
		}
		return this.str().asString( parts, DN_ELEMENT_SEPARATOR ) ;
	} // normalizeDN() 

	// -------------------------------------------------------------------------
	
  /**
   * Returns true if the given value must be BASE64 encoding if used in a
   * contex with limited character representation. 
   */
  public boolean needsEncoding( Object value ) 
	{
		if ( value instanceof byte[] )
			return true ;
		
		if ( value instanceof String )
		{
			String str = (String)value ;
			return ENCRYPTION_PATTERN.matches( str ) ;
		}
		return false ;
	} // needsEncoding() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns the given value as a String if it is a byte array containing an
   * encrypted string. Otherwise the value is returned unchanged.
   */
  public Object asEncryptedStringIfApplicable( Object value ) 
	{
  	String str ;
  	
		if ( value instanceof byte[] )
		{
			str = new String( (byte[])value ) ;
			if ( ENCRYPTION_PATTERN.matches( str ) )
			{
				return str ;
			}
		}
		return value ;
	} // asEncryptedStringIfApplicable() 

	// -------------------------------------------------------------------------
  
  /**
   * Encodes the given object to BASE64 encoding
   */
  public String encodeToBase64( Object obj ) 
	{
		byte[] bytes ;
		
		if ( obj instanceof byte[] )
		{
			bytes = (byte[])obj ;
			return Base64Converter.encodeToString( bytes ) ;
		}
		else
		{
			return Base64Converter.encode( obj.toString() ) ;
		}
	} // encodeToBase64() 

	// -------------------------------------------------------------------------
  
  /**
   * Creates an LDAP syntax compliant timestamp for the given date.
   * That is "YYYYMMDDhhmmssZ".
   * Example: June 4, 2003 10:45 pm and 17 seconds is "20030604224517Z"
   * 
   * @param date The date to convert to an LDAP timestamp string
   */
  public String createTimestamp( Date date ) 
	{
  	StringBuffer buffer ;
  	
  	buffer = new StringBuffer( 20 ) ;
		buffer.append( LDAP_TIMESTAMP_FORMAT.format( date ) ) ;
		buffer.append( 'Z' ) ;
		return buffer.toString() ;
	} // createTimestamp() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Creates an LDAP syntax compliant timestamp for the current date.
   * 
   * @see #createTimestamp(Date)
   */
  public String createTimestamp() 
  {
  	return this.createTimestamp( new Date() ) ;
  } // createTimestamp() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected StringUtil str()
	{
		return StringUtil.current();
	} // str() 

	// -------------------------------------------------------------------------
	
} // class LDAPUtil 
