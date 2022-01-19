// ===========================================================================
// CONTENT  : CLASS LDIFWriter
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 22/05/2004
// HISTORY  :
//  22/05/2004  mdu  CREATED
//
// Copyright (c) 2004, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.directory.ldif ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import org.pf.directory.MultiValueAttribute;
import org.pf.directory.ldap.LDAPDirEntry;
import org.pf.directory.ldap.LDAPUtil;

/**
 * Simple writer that is capable of writing internal LDAP directory objects 
 * to an LDIF formatted stream.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class LDIFWriter
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final String COLON		= ":" ;
	
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // CLASS METHODS
  // =========================================================================
	/**
	 * Returns a new instance of this class ;
	 */
	public static LDIFWriter n() 
	{
		return new LDIFWriter() ;
	} // n() 

	// -------------------------------------------------------------------------

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public LDIFWriter()
  {
    super() ;
  } // LDIFWriter()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected void appendVersion( PrintStream stream ) 
	{
		stream.println( "Version: 1" ) ;
		stream.println() ;
	} // appendVersion()

	// -------------------------------------------------------------------------

  protected void appendObject( PrintStream stream, LDAPDirEntry entry ) 
	{
  	String[] attrNames ;
  	MultiValueAttribute attribute ;
  	List values ;
  	Iterator iter ;
  	Object value ;
  	
		stream.print( "dn:" ) ;
		stream.println( entry.getDN() ) ;
		attrNames = entry.getAttributeNames() ;
		for (int i = 0; i < attrNames.length; i++ )
		{
			attribute = entry.getAttribute( attrNames[i] ) ;
			values = attribute.getValues() ;
			iter = values.iterator() ;
			while ( iter.hasNext() )
			{
				stream.print( attribute.getName() ) ;
				stream.print( COLON ) ;
				value = iter.next() ;
				if ( this.lutil().needsEncoding( value ) )
				{
					stream.print( COLON ) ;
					value = this.lutil().encodeToBase64( value ) ;
				}
				stream.println( value.toString() ) ;
			}
		}
		stream.println() ;
	} // appendObject()

	// -------------------------------------------------------------------------
  
  protected LDAPUtil lutil() 
	{
		return LDAPUtil.current() ;
	} // lutil()

	// -------------------------------------------------------------------------
  
} // class LDIFWriter
