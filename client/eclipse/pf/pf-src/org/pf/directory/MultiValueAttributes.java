// ===========================================================================
// CONTENT  : CLASS MultiValueAttributes
// AUTHOR   : M.Duchrow
// VERSION  : 1.2 - 22/08/2006
// HISTORY  :
//  23/04/2004  mdu		CREATED
//	26/09/2004	mdu		added		-->	getAttributeNameList()
//	25/06/2007	mdu		changed	-->	getCopyOfAttributes() to handle "*" in attrNames
//	22/08/2006	mdu		added		-->	metaData
//
// Copyright (c) 2004-2006, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.directory ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.List;

import org.pf.directory.ldap.LDAPUtil;
import org.pf.directory.metadata.AttributeMetaData;
import org.pf.directory.metadata.AttributesMetaData;
import org.pf.reflect.AttributeReadAccess;
import org.pf.text.StringUtil;
import org.pf.util.NamedValueList;

/**
 * Holds many attribute names with associated multiple values under.
 * Access to attributes by their name is case-insensitive.
 *
 * @author M.Duchrow
 * @version 1.2
 */
public class MultiValueAttributes implements AttributeReadAccess
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private NamedValueList keyedAttributes = null ;
  protected NamedValueList getKeyedAttributes() { return keyedAttributes ; }
  protected void setKeyedAttributes( NamedValueList newValue ) { keyedAttributes = newValue ; }
  
  private AttributesMetaData metaData = null ;
  public AttributesMetaData getMetaData() { return metaData ; }
  public void setMetaData( AttributesMetaData newValue ) { metaData = newValue ; }
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public MultiValueAttributes()
  {
    super() ;
    this.reset() ;
  } // MultiValueAttributes() 

  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  
  /**
   * Returns the first value of the attribute with the given name.
   * If the attribute is not known it returns null.
   * 
   * @param attrName The name of the attribute of which the first associated value should be returned
   */
  public Object getValue( String attrName ) 
	{
  	MultiValueAttribute attr ;
  	
  	attr = this.getAttribute( attrName ) ;
  	if ( attr == null )
  		return null ;
  	
		return attr.getFirstValue() ; 
	} // getValue() 

	// -------------------------------------------------------------------------

  /**
   * Returns the values of the attribute with the given name.
   * If the attribute is not known it returns null.
   * 
   * @param attrName The name of the attribute of which the associated values should be returned
   */
  public List getValues( String attrName ) 
	{
		return this.basicGetValues( attrName ) ;
	} // getValues() 

	// -------------------------------------------------------------------------

  /**
   * Adds the given value to the attribute with the given name.
   * If the attribute not yet exists it will be automatically added as well.
   * 
   * @param attrName The name of the attribute
   * @param value The value to be added to the attribute's values
   */
  public void addValue( String attrName, Object value ) 
	{
		this.putValue( attrName, value, false ) ;
	} // addValue() 

	// -------------------------------------------------------------------------
  
  /**
   * Adds the given value to the attribute with the given name.
   * If the attribute not yet exists it will be automatically added as well.
   * 
   * @param attrName The name of the attribute
   * @param value The value to be added to the attribute's values
   * @param encoded An indicator that defines whether the values of this attribute have to be encoded
   */
  public void addValue( String attrName, Object value, boolean encoded ) 
	{
		this.putValue( attrName, value, encoded ) ;
	} // addValue() 

	// -------------------------------------------------------------------------

  /**
   * Adds the given attribute. If an attribute with the same name already 
   * exists then it will be replaced.
   * 
   * @param attribute The attribute to add
   */
  public void addAttribute( MultiValueAttribute attribute ) 
	{
  	if ( attribute != null )
  	{
	  	if ( this.hasAttribute( attribute.getName() ) )
			{
	  		this.removeAttribute(attribute.getName() ) ;
			}
			this.basicAddAttribute( attribute ) ;
		}
	} // addAttribute() 

	// -------------------------------------------------------------------------

  /**
   * Removes the attribute with the given name and of course also all its 
   * associated values.
   * 
   * @param attrName The name of the attribute to remove 
   */
  public void removeAttribute( String attrName ) 
	{
		this.basicRemoveAttribute( attrName ) ;
	} // removeAttribute() 

	// -------------------------------------------------------------------------
  
  /**
   * Removes the given value from the specified attribute. If the value is not
   * yet associated with the attribute nothing happens.
   * If the attribute has no values after removing this one, the attribute 
   * will be removed as well.
   * 
   * @param attrName The name of the attribute
   * @param value The value to remove from the attribute
   */
  public void removeValue( String attrName, Object value ) 
	{
		List values ;
		
		if ( attrName == null )
			return ;
		if ( value == null )
			return ;
		
		values = this.basicGetValues( attrName ) ;
		if ( values != null )
		{
			values.remove( value ) ;
			if ( values.isEmpty() )
			{
				this.removeAttribute( attrName ) ;
			}
		}
	} // removeValue() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns the names of all attributes.
   */
  public String[] getAttributeNames() 
	{
		return this.str().asStrings( this.getAttributeNameList() ) ;
	} // getAttributeNames() 

	// -------------------------------------------------------------------------
  
	/**
	 * Returns the current value of the attribute with the given name.
	 *
	 * @param attrName The attribute's name ( case sensitive )
	 * @throws NoSuchFieldException If there is no attribute with the given name
	 */
  public Object getAttributeValue( String attrName )
  	throws NoSuchFieldException
	{
		if ( this.hasAttribute( attrName ) )
		{
			return this.basicGetValues( attrName ) ;
		}
		else
		{
			throw new NoSuchFieldException( "Attribute '" + attrName + "' not found" ) ;
		}
	} // getAttributeValue() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns the attribute with the specified name or null if not found
   * 
   * @param attrName The name of the attribute
   */
  public MultiValueAttribute getAttribute( String attrName ) 
	{
		return this.basicGetAttribute( attrName ) ; 
	} // getAttribute() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns if this object contains an attribute with the specified name.
   */
  public boolean hasAttribute( String attrName ) 
	{
		return ( this.basicGetAttribute( attrName ) != null ) ;
	} // hasAttribute() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns the current number of attributes in this object.
   */
  public int size() 
	{
		return this.getKeyedAttributes().size() ;
	} // size() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns true if this object currently contains no attribute at all
   */
  public boolean isEmpty() 
	{
		return ( this.size() == 0 ) ;
	} // isEmpty() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns a copy of all those attributes that are specified by the given 
   * name array and that exist in this object.
   * If the name array is null or empty all attributes will be copied.
   * 
   * @param attrNames The names of all attributes to copy (null means to copy all attributes) 
   */
  public MultiValueAttributes getCopyOfAttributes( String[] attrNames ) 
	{
  	MultiValueAttributes copy ;
  	MultiValueAttribute attr ;
  	String[] allAttrNames ;
  	boolean copyAll ;
  	
  	copyAll = this.str().isNullOrEmpty( attrNames ) || this.str().contains( attrNames, "*" ) ;
  	copy = this.newInstance() ;
  	allAttrNames = this.getAttributeNames() ;
  	for (int i = 0; i < allAttrNames.length; i++ )
		{
			if ( ( copyAll ) || 
					( this.str().containsIgnoreCase( attrNames, allAttrNames[i] ) ) )
			{
				attr = this.getAttribute( allAttrNames[i] ) ;
				copy.addAttribute( attr.copy() ) ;
			}
		}
  	
  	return copy ;
	} // getCopyOfAttributes() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns the first value of the specified attribute as String.
   * 
   * @param attrName The name of the attribute
   * @throws NoSuchFieldException If no attribute with the specified name can be found
   * @return null if the attribute was found but has no value
   */
  public String getAttributeAsString( String attrName )
		throws NoSuchFieldException
	{
		MultiValueAttribute attr ;
		
		attr = this.getAttribute( attrName ) ;
		if ( attr == null )
		{
			throw new NoSuchFieldException( attrName ) ;
		}
		if ( attr.isEmpty() )
			return null ;

		return attr.getFirstValue().toString() ;
	} // getAttributeAsString() 
	
	// -------------------------------------------------------------------------
	
  /**
   * Returns the values of the specified attribute as String array.
   * 
   * @param attrName The name of the attribute
   * @throws NoSuchFieldException If no attribute with the specified name can be found
   * @throws ClassCastException If any value of the attribute is not of type String
   * @return An empty array if the attribute was found but has no values
   */
  public String[] getAttributeAsStringArray( String attrName )
		throws NoSuchFieldException
	{
		MultiValueAttribute attr ;
		
		attr = this.getAttribute( attrName ) ;
		if ( attr == null )
		{
			throw new NoSuchFieldException( attrName ) ;
		}
		if ( attr.isEmpty() )
			return StringUtil.EMPTY_STRING_ARRAY ;

		return attr.getValuesAsStrings() ;
	} // getAttributeAsStringArray() 
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * Subclasses may override this method to provide an instance of their own
   * class.
   */
  protected MultiValueAttributes newInstance() 
	{
		return new MultiValueAttributes() ;
	} // newInstance() 

	// -------------------------------------------------------------------------
  
  protected void putValue( String name, Object value, boolean encoded ) 
	{
  	MultiValueAttribute attr ;
  	Object valueToStore ;
		
		if ( name == null )
			return ;
		if ( value == null )
			return ;
		
		valueToStore = this.lutil().asEncryptedStringIfApplicable( value ) ;
		
		attr = this.basicGetAttribute( name ) ;
		if ( attr == null )
		{
			this.createAttribute( name, valueToStore, encoded ) ;
		}
		else
		{
			if ( this.isSingleValueAttribute( attr.getName() ) )
			{				
				attr.setSoleValue( valueToStore ) ;
			}
			else
			{
				attr.addValue( valueToStore ) ;
			}
		}
	} // putValue() 

	// -------------------------------------------------------------------------
  
  protected List basicGetValues( String name ) 
	{
  	MultiValueAttribute attr ;
  	
  	attr = this.basicGetAttribute( name ) ;
  	if ( attr == null )
  		return null ;
  	else
  		return attr.getValues() ;
	} // basicGetValues() 

	// -------------------------------------------------------------------------

  protected MultiValueAttribute createAttribute( String name, Object value, boolean encoded ) 
	{
  	MultiValueAttribute attr ;
  	
		attr = new MultiValueAttribute( name, encoded ) ;
		attr.addValue( value ) ;
		this.basicAddAttribute( attr ) ;
  	return attr ;
	} // createAttribute() 

	// -------------------------------------------------------------------------

  protected MultiValueAttribute basicGetAttribute( String name ) 
	{
  	String normalizedName ;
  	
  	normalizedName = this.normalizeName( name ) ;
		return (MultiValueAttribute)this.getKeyedAttributes().valueAt( normalizedName ) ;
	} // basicGetAttribute() 

	// -------------------------------------------------------------------------

  protected void basicAddAttribute( MultiValueAttribute attr ) 
	{
  	String normalizedName ;
  	
  	normalizedName = this.normalizeName( attr.name() ) ;
		this.getKeyedAttributes().add( normalizedName, attr ) ;
	} // basicAddAttribute() 

	// -------------------------------------------------------------------------

  protected void basicRemoveAttribute( String name ) 
	{
  	String normalizedName ;
  	
  	normalizedName = this.normalizeName( name ) ;
  	this.getKeyedAttributes().remove( normalizedName ) ;
	} // basicRemoveAttribute() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns a list containing all the names of all attributes.
   * Subclasses may override this method to add more names if applicable.
   * However, it is recommended to always call super.getAttributeNameList().
   */
  protected List getAttributeNameList() 
	{
  	List nameList ;
  	
		nameList = new ArrayList( this.getKeyedAttributes().size() ) ;
		this.appendAttributeNames(  nameList ) ;
		return nameList ;
	} // getAttributeNameList() 

	// -------------------------------------------------------------------------  
  
  /**
   * Appends attribute names to the given list
   * Subclasses may override this method to add more names if applicable.
   * However, it is recommended to always call super.appendAttributeNames().
   */
  protected void appendAttributeNames( List nameList ) 
	{
  	MultiValueAttribute attr ;
  	List nameValuePairs ;
  	
		nameValuePairs = this.getKeyedAttributes().values() ;
		for (int i = 0; i < nameValuePairs.size(); i++ )
		{
			attr = (MultiValueAttribute)nameValuePairs.get(i) ;
			nameList.add( attr.name() ) ;
		}
	} // appendAttributeNames() 

	// -------------------------------------------------------------------------  
  
  protected boolean isSingleValueAttribute( String attrName ) 
	{
		AttributeMetaData attrMetaData ;
		
		if ( this.getMetaData() != null )
		{
			attrMetaData = this.getMetaData().getMetaData( attrName ) ;
			if ( attrMetaData != null )
			{
				return attrMetaData.isSingleValued() ;
			}
		}
		return false ;
	} // isSingleValueAttribute()
	
	// -------------------------------------------------------------------------
  
  protected String normalizeName( String name ) 
	{
		return name.toLowerCase() ;
	} // normalizeName() 

	// -------------------------------------------------------------------------
  
  protected void reset() 
	{
		this.setKeyedAttributes( new NamedValueList() ) ;
	} // reset() 

	// -------------------------------------------------------------------------
  
  protected List newValueList() 
	{
		return new ArrayList() ;
	} // newValueList() 

	// -------------------------------------------------------------------------
  
  protected LDAPUtil lutil() 
	{
		return LDAPUtil.current() ;
	} // lutil() 

	// -------------------------------------------------------------------------
  
  protected StringUtil str()
	{
		return StringUtil.current();
	} // str() 

	//-------------------------------------------------------------------------
  
} // class MultiValueAttributes 
