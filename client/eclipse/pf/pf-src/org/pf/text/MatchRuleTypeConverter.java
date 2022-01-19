// ===========================================================================
// CONTENT  : CLASS MatchRuleStringConverter
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 16/08/2007
// HISTORY  :
//  16/08/2007  mdu  CREATED
//
// Copyright (c) 2007, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

/**
 * Supports conversion of strings to integers, long, floats, BigDecimal etc.
 *
 * @author M.Duchrow
 * @version 1.0
 */
class MatchRuleTypeConverter
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public MatchRuleTypeConverter()
  {
    super() ;
  } // MatchRuleTypeConverter() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Converts the given object or object array or collection of object
   * to the specified type.
   * The type can either be a class (e.g. Integer.class, BigDecimal.class)
   * or an instance of SimpleDateFormat.
   * If conversion is not possible the null will be returned.
   * 
   * @param object A String or String[] or Collection<String>
   * @param type The type to convert the given string(s) to
   * @return The converted object or null if conversion is not possible
   * @throws IllegalArgumentException if the given type is not supported
   */
  public Object convertToType( Object object, Object type )
  {
    if ( object instanceof String )
    {
      return this.convertToType( (String)object, type ) ;
    }
    if ( object instanceof Collection )
		{
    	return this.convertToType( (Collection)object, type ) ;			
		}
    if ( object instanceof Object[] )
		{			
    	return this.convertToType( (Object[])object, type ) ;
		}
    if ( ( object instanceof Integer ) && ( type == Long.class ) )
		{
			return this.convertToLong( (Integer)object ) ;
		}
    return object ;
  } // convertToType() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Converts the given string to an object of the specified type.
   * The type can either be a class (e.g. Integer.class, BigDecimal.class)
   * or an instance of SimpleDateFormat to convert the string to a date.
   * 
   * @throws IllegalArgumentException if the type is not supported
   */
  public Object convertToType( String strValue, Object type )
  {
    if ( strValue == null )
    {
      return null ;
    }
  	if ( type == Integer.class )
  	{
  		return this.convertToInteger( strValue );
  	}
  	if ( type == Long.class )
  	{
  		return this.convertToLong( strValue );
  	}
  	if ( type instanceof SimpleDateFormat )
  	{
  		return this.convertToDate( strValue, (SimpleDateFormat) type );
  	}
    if ( type == Float.class )
    {
      return this.convertToFloat( strValue );
    }
    if ( type == Double.class )
    {
      return this.convertToDouble( strValue );
    }
    if ( type == BigDecimal.class )
    {
      return this.convertToBigDecimal( strValue );
    }
    throw new IllegalArgumentException( "Type " + type + " not supported." );
  } // convertToType() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Converts the given collection of strings to a collection of objects 
   * of the specified type.
   * The type can either be a class (e.g. Integer.class, BigDecimal.class)
   * or an instance of SimpleDateFormat to convert the string to a date.
   * 
   * @throws IllegalArgumentException if the type is not supported
   * @return Collection of type or null if conversion is not possible
   */
  public Collection convertToType( Collection collection, Object type )
  {
  	Collection result ;
  	Iterator iter ;
  	Object element ;
  	
  	if ( this.isNullOrEmpty( collection ) )
  	{
  		return null ;
  	}
  	result = this.newCollectionOfType( collection.getClass() ) ;
  	iter = collection.iterator() ;
  	while ( iter.hasNext() )
		{
			element = this.convertToType( iter.next(), type ) ;
			if ( element != null )
			{
				result.add( element ) ;
			}
		}
  	return result ;
  } // convertToType() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Converts the given object array to an array of the specified
   * type containing all string values converted to that type.
   * The type can either be a class (e.g. Integer.class, BigDecimal.class)
   * or an instance of SimpleDateFormat to convert the strings to dates.
   * 
   * @throws IllegalArgumentException if the type is not supported
   */
  public Object[] convertToType( Object[] values, Object type )
  {
  	Object[] result ;
  	
  	if ( values == null )
		{
			return null ;
		}
  	if ( type instanceof SimpleDateFormat )
		{
  		result = this.newArrayOfType( Date.class, values.length ) ;
		}
		else
		{
			result = this.newArrayOfType( (Class)type, values.length ) ;
		}
  	for (int i = 0; i < values.length; i++ )
		{
  		result[i] = this.convertToType( values[i], type ) ;
		}
  	return result ;
  } // convertToType() 

  // -------------------------------------------------------------------------

  public Float convertToFloat( String strValue )
  {
  	return Float.valueOf( strValue );
  } // convertToFloat() 

  // -------------------------------------------------------------------------

  public Double convertToDouble( String strValue )
  {
  	return Double.valueOf( strValue );
  } // convertToDouble() 

  // -------------------------------------------------------------------------

  public BigDecimal convertToBigDecimal( String strValue )
  {
    return new BigDecimal( strValue );
  } // convertToBigDecimal() 

  // -------------------------------------------------------------------------

  public Integer convertToInteger( String str ) 
  {
    return Integer.valueOf( str );
  } // convertToInteger() 

  // -------------------------------------------------------------------------

  public Long convertToLong( String str ) 
  {
    return Long.valueOf( str );
  } // convertToLong() 

  // -------------------------------------------------------------------------

  public Date convertToDate( String strValue, SimpleDateFormat dateFormat )
  {
    try
		{
			return dateFormat.parse( strValue );
		}
		catch ( ParseException e )
		{
			throw new IllegalArgumentException( "Unable to convert '" + strValue + "' to Date with format \""
					+ dateFormat.toPattern() + "\"" );
		}
  } // convertToDate() 

  // -------------------------------------------------------------------------

  public Long[] convertToLong( Integer[] intValues ) 
  {
    Long[] longValues = new Long[intValues.length];

    for (int i = 0; i < intValues.length; i++ )
    {
      longValues[i] = this.convertToLong( intValues[i] );
    }
    return longValues ;
  } // convertToLong() 

  // -------------------------------------------------------------------------

  public Long convertToLong( Integer intValue ) 
  {
  	return Long.valueOf( intValue );
  } // convertToLong() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * Returns a new empty collection of the given type or if such a collection
   * cannot be created an instance of an ArrayList.
   */
  protected Collection newCollectionOfType( Class collectionType ) 
	{
		try
		{
			return (Collection)collectionType.getDeclaredConstructor().newInstance() ;
		}
		catch ( Exception e )
		{
			return new ArrayList() ;
		}
	} // newCollectionOfType() 
	
	// -------------------------------------------------------------------------
  
  protected Object[] newArrayOfType( Class elementType, int length ) 
	{
		return (Object[]) Array.newInstance( elementType, length ) ;
	} // newArrayOfType()
	
	// -------------------------------------------------------------------------
  
  protected boolean isNullOrEmpty( Object[] array ) 
	{
		return ( array == null ) || ( array.length == 0 ) ;
	} // isNullOrEmpty() 
	
	// -------------------------------------------------------------------------
  
  protected boolean isNullOrEmpty( Collection collection ) 
  {
  	return ( collection == null ) || ( collection.isEmpty() ) ;
  } // isNullOrEmpty() 
  
  // -------------------------------------------------------------------------
  
  protected void addAll( Collection collection, Object[] array ) 
	{
		if ( ( collection != null ) && ( array != null ) )
		{
			for (int i = 0; i < array.length; i++ )
			{
				collection.add( array[i] ) ;
			}
		}
	} // addAll() 
	
	// -------------------------------------------------------------------------
  
} // class MatchRuleTypeConverter 
