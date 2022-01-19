// ===========================================================================
// CONTENT  : CLASS MultiValueAttribute
// AUTHOR   : M.Duchrow
// VERSION  : 1.3 - 02/07/2006
// HISTORY  :
//  23/04/2004  mdu		CREATED
//	16/07/2004	mdu		added		-->	copy()
//	26/09/2004	mdu		added		-->	isEmpty(), valueCount(), getFirstValueAsString(), getValuesAsStrings()
//	02/07/2006	mdu		added		-->	setSoleValue()
//
// Copyright (c) 2004-2006, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.directory ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.List;

import org.pf.text.StringUtil;
import org.pf.util.NamedValue;

/**
 * An attribute with multiple values associated
 *
 * @author M.Duchrow
 * @version 1.3
 */
public class MultiValueAttribute extends NamedValue
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private boolean mustBeEncoded = false ;
  /**
   * Returns whether or not the value of this attribute must be encoded if 
   * written to a stream
   */
  public boolean mustBeEncoded() { return mustBeEncoded ; }
  /**
   * Sets whether or not the value of this attribute must be encoded if 
   * written to a stream
   */
  public void mustBeEncoded( boolean newValue ) { mustBeEncoded = newValue ; }
  
  // -------------------------------------------------------------------------
  
  private boolean ignoreCase = false ;
  /**
   * Returns true if this MultiValueAttribute is treated case-insensitive.
   * That is, comparison of string values is done case-insensitive.
   */
  public boolean getIgnoreCase() { return ignoreCase ; }
  /**
   * Sets whether or not this MultiValueAttribute is treated case-insensitive.
   * That is, comparison of string values is done case-insensitive.
   */
  public void setIgnoreCase( boolean newValue ) { ignoreCase = newValue ; }
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with a name.
   * 
   * @param attrName The name of the new attribute
   */
  public MultiValueAttribute( String attrName )
  {
    this( attrName, false ) ;
  } // MultiValueAttribute() 
  
  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with a name.
   * 
   * @param attrName The name of the new attribute
   * @param encoded A marker that indicates if the values of this attribute must be encoded
   */
  public MultiValueAttribute( String attrName, boolean encoded )
  {
    super( attrName, new ArrayList() ) ;
    this.mustBeEncoded( encoded ) ;
  } // MultiValueAttribute() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the name of this attribute
   */
  public String getName() 
	{
		return this.name() ;
	} // getName() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns the values of this attribute
   */
  public List	getValues() 
	{
  	return (List)this.value() ;
	} // getValues() 

	// -------------------------------------------------------------------------

  /**
   * Returns the values of this attribute as string array
   * 
   * @throws ClassCastException If any value of this attribute is not of type String
   */
  public String[] getValuesAsStrings() 
	{
  	return StringUtil.current().asStrings( this.getValues() ) ;
	} // getValuesAsStrings() 

	// -------------------------------------------------------------------------

  /**
   * Returns the first value of this attribute
   */
  public Object getFirstValue() 
	{
		return this.getValues().get(0) ;
	} // getFirstValue() 

	// -------------------------------------------------------------------------

  /**
   * Returns the first value of this attribute as String
   * 
   * @throws NullPointerException if the attribute has no value at all
   */
  public String getFirstValueAsString() 
	{
		return this.getFirstValue().toString() ;
	} // getFirstValueAsString() 

	// -------------------------------------------------------------------------

  /**
   * Add the given value if it not already exists in the attributes values
   */
  public void addValue( Object value ) 
	{
  	if ( ! this.containsValue( value ) )
		{
			this.getValues().add( value ) ;
		}
	} // addValue() 

	// -------------------------------------------------------------------------
  
  /**
   * Adds all given values to this attribute skipping duplicate values.
   */
  public void addValues( Object[] values ) 
	{
		if ( values != null )
		{
			for (int i = 0; i < values.length; i++ )
			{
				this.addValue( values[i] ) ;
			}
		}
	} // addValues() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Adds all given values to this attribute skipping duplicate values.
   */
  public void addValues( List values ) 
  {
  	if ( values != null )
  	{
  		for (int i = 0; i < values.size(); i++ )
  		{
  			this.addValue( values.get(i) ) ;
  		}
  	}
  } // addValues() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns true if the given value is one of the values of this attribute.
   * Comparison of string values are done according to the current ignoreCase state. 
   */
  public boolean containsValue( Object value ) 
	{
  	List currentValues ;
  	Object object ;
  	
  	if ( value == null )
		{
			return false ;
		}
  	currentValues = this.getValues() ;
  	if ( ( value instanceof String ) && this.getIgnoreCase() )
		{
  		String str = (String)value ;
  		int max = this.valueCount() ;
  		for (int i = 0; i < max; i++ )
			{
  			object = currentValues.get( i ) ;
  			if ( object instanceof String )
				{
  				if ( str.equalsIgnoreCase( (String)object ) )
  				{
  					return true ;
  				}
				}
			}
			return false ;
		}
		return currentValues.contains( value ) ;
	} // containsValue() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Sets the given value as the only value of this attribute
   */
  public void setSoleValue( Object value ) 
  {
  	this.getValues().clear() ;
 		this.getValues().add( value ) ;
  } // setSoleValue() 
  
  // -------------------------------------------------------------------------
  
	/**
	 * Returns a copy of this object
	 */
	public MultiValueAttribute copy()
	{
		MultiValueAttribute copy ;
		
		copy = new MultiValueAttribute( this.name(), this.mustBeEncoded() ) ;
		copy.setValue( this.getValue() ) ;
		return copy ;
	} // copy() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the number of values associated with this attribute
	 */
	public int valueCount() 
	{
		return this.getValues().size() ;
	} // valueCount() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the attribute has no values
	 */
	public boolean isEmpty() 
	{
		return this.valueCount() == 0 ;
	} // isEmpty() 

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class MultiValueAttribute 
