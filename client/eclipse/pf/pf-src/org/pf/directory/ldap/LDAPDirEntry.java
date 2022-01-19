// ===========================================================================
// CONTENT  : CLASS LDAPDirEntry
// AUTHOR   : M.Duchrow
// VERSION  : 1.1 - 22/08/2006
// HISTORY  :
//  23/04/2004	mdu		CREATED
//	18/06/2006	mdu		changed	-->	collectAll() to be based on IObjectFilter
//	22/08/2006	mdu		added		-->	support EMPTY_DN for dn = "" 
//
// Copyright (c) 2004-2006, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.directory.ldap ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Iterator;

import org.pf.bif.filter.IObjectFilter;
import org.pf.directory.DirectoryObject;
import org.pf.directory.DistinguishedName;
import org.pf.directory.MultiValueAttribute;
import org.pf.directory.MultiValueAttributes;

/**
 * Represents an object from a LDAP directory
 *
 * @author M.Duchrow
 * @version 1.1
 */
public class LDAPDirEntry extends DirectoryObject
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	public static final String ATTRNAME_CREATE_TIMESTAMP		= "createtimestamp" ;
	public static final String ATTRNAME_MODIFY_TIMESTAMP		= "modifytimestamp" ;
	public static final String ATTRNAME_CREATORS_NAME 			= "creatorsname" ;
	public static final String ATTRNAME_MODIFIERS_NAME 			= "modifiersname" ;
	public static final String ATTRNAME_SUBSCHEMA_SUBENTRY	= "subschemasubentry" ;
	public static final String ATTRNAME_NAMING_CONTEXTS			= "namingcontexts" ;
	
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private DistinguishedName distinguishedName = null ;
  /**
   * Returns the distinguished name of this object
   */
  public DistinguishedName getDistinguishedName() { return distinguishedName ; }
  protected void setDistinguishedName( DistinguishedName newValue ) { distinguishedName = newValue ; }
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with its dn.
   * 
   * @param dn The distinguished name of the new object.
   */
  public LDAPDirEntry( String dn )
  {
    this( ( ( dn != null ) && ( dn.length() == 0 ) ) ? DistinguishedName.EMPTY_DN : new DistinguishedName(dn) ) ;
  } // LDAPDirEntry() 
  
  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with its distinguished name.
   * 
   * @param dn The distinguished name of the new object.
   */
  public LDAPDirEntry( DistinguishedName dn )
  {
    super( dn.toString() ) ;
    this.setDistinguishedName( dn ) ;
  } // LDAPDirEntry() 
  
  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with the given entry data.
   * Actually that means creating a copy.
   */
  public LDAPDirEntry( LDAPDirEntry dirObject )
  {
    this( dirObject.getDN() ) ;
    this.setMetaData( dirObject.getMetaData() ) ;
    this.copyAttributesFrom( dirObject ) ;
  } // LDAPDirEntry() 

  // -------------------------------------------------------------------------  
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the creation timestamp of this object
   */
  public String getCreateTimestamp() 
  {
  	try
  	{
  		return this.getAttributeAsString( ATTRNAME_CREATE_TIMESTAMP ) ;
  	}
  	catch ( NoSuchFieldException e )
  	{
  		return null ;
  	}
  } // getCreateTimestamp() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the modification timestamp of this object
   */
  public String getModifyTimestamp() 
  {
  	try
  	{
  		return this.getAttributeAsString( ATTRNAME_MODIFY_TIMESTAMP ) ;
  	}
  	catch ( NoSuchFieldException e )
  	{
  		return null ;
  	}
  } // getModifyTimestamp() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Sets the creation timestamp of this object to the given value.
   * 
   * @param timestamp A valid LDAP timestamp string of format "yyyyMMddHHmmssZ"
   */
  public void setCreateTimestamp( String timestamp ) 
  {
  	this.setAttribute( ATTRNAME_CREATE_TIMESTAMP, timestamp );
  } // setCreateTimestamp() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Sets the modification timestamp of this object to the given value.
   * 
   * @param timestamp A valid LDAP timestamp string of format "yyyyMMddHHmmssZ"
   */
  public void setModifyTimestamp( String timestamp ) 
  {
  	this.setAttribute( ATTRNAME_MODIFY_TIMESTAMP, timestamp );
  } // setModifyTimestamp() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Sets the creation and modification timestamps to the current date/time
   */
  public void updateTimestamps() 
  {
  	String timestamp ;
  	
  	timestamp = LDAPUtil.current().createTimestamp() ;
  	this.setCreateTimestamp( timestamp ) ;
  	this.setModifyTimestamp( timestamp ) ;
  } // updateTimestamps() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Sets the modification timestamp to the current date/time
   */
  public void updateModifyTimestamp() 
  {
  	String timestamp ;
  	
  	timestamp = LDAPUtil.current().createTimestamp() ;
  	this.setModifyTimestamp( timestamp ) ;
  } // updateModifyTimestamp() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Sets the attribute with the given name to the given value.
   * If the attribute already exists its value will be replaced, otherwise
   * the attribute gets created with the given value as initial value.
   * 
   * @param attrName The name of the attribute to set
   * @param attrValue The value to set
   */
  public void setAttribute( String attrName, String attrValue ) 
  {
  	MultiValueAttribute attribute ;
  	
  	if ( this.str().isNullOrEmpty( attrName ) || this.str().isNullOrEmpty( attrValue ) )
  	{
  		return ;
  	}
  	attribute = this.getAttribute( attrName );
  	if ( attribute == null )
  	{
  		attribute = new MultiValueAttribute( attrName ) ;
  		this.addAttribute( attribute ) ;
  	}
  	attribute.setSoleValue( attrValue ) ;
  } // setAttribute() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the distinguished name of this object.
   */
  public String getDN() 
	{
		return this.getIdentifier() ;
	} // getDN() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns true if this entry is a container object that can contain other
   * entries as children.
   * <p>
   * Here it always returns false. Subclasses must override this method to 
   * return true if appropriate.
   */
  public boolean isContainer() 
	{
		return false ;
	} // isContainer() 

	// -------------------------------------------------------------------------
  
  /**
   * Add the given attributes to this LDAP entry
   */
  public void updateFrom( MultiValueAttributes attributes ) 
	{
  	String[] attrNames ;
  	MultiValueAttribute attr ;
  	Iterator values ;
  	
  	attrNames = attributes.getAttributeNames() ;
  	for (int i = 0; i < attrNames.length; i++ )
		{
  		attr = attributes.getAttribute( attrNames[i] ) ;
  		values = attr.getValues().iterator() ;
  		while ( values.hasNext() )
			{
	  		this.addValue( attrNames[i], values.next(), attr.mustBeEncoded() ) ;
			}
		}		
	} // updateFrom() 

	// -------------------------------------------------------------------------

  /**
   * Returns a string representation of this object
   */
  public String toString() 
	{
		return "LDAPDirEntry(" + this.getDN() + ")" ;
	} // toString() 

	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected void copyAttributesFrom( LDAPDirEntry dirObject ) 
	{
		String[] names ;
		MultiValueAttribute attr ;
		
		names = dirObject.getAttributeNames() ;
		for (int i = 0; i < names.length; i++ )
		{
			attr = dirObject.getAttribute( names[i] ) ;
			this.basicAddAttribute( attr ) ;
		}
	} // copyAttributesFrom() 

	// -------------------------------------------------------------------------

  /**
   * Adds this object if it matches the given filter to the 
   * given result container.
   * 
   * @param result The container to put in the found object
   * @param rule The rule the objects must match 
   */
  protected void collectAll( LDAPSearchResult result, IObjectFilter filter ) 
	{  	
  	if ( filter.matches( this ) )
		{
			result.add( this ) ;
		}
	} // collectAll() 

	// -------------------------------------------------------------------------
  
  protected String inspectString() 
	{
		return "LDAPDirEntry(" + this.getDN() + ")" ;
	} // inspectString() 

	// -------------------------------------------------------------------------
  
} // class LDAPDirEntry 
