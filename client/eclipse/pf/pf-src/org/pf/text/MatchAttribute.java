// ===========================================================================
// CONTENT  : CLASS MatchAttribute
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.0 - 16/08/2007
// HISTORY  :
//  11/07/2001  duma  CREATED
//  09/10/2001  duma  changed -> Made class and constructor public and added a constructor
//  24/11/2001  duma  changed -> Supports now String[] and List objects as values of an attribute
//  08/01/2002  duma  changed -> Made serializable
//	14/08/2002	duma	changed	-> New constructor with no arguments
//	23/08/2002	duma	re-designed	-> Moved parsing and printing to other classes
//	26/12/2002	duma	added		-> Operators to support =,<,>,<= and >=
//	21/03/2003	duma	changed	-> Supports Integer values in value map
//	24/10/2003	duma	added		-> multiCharWildcardMatchesEmptyString()
//										changed	-> matchValue()
//	20/12/2004	duma	added		-> support for various datatypes (Float,Double,BigDecimal,Integer,Long,Date)
//	16/08/2007	mdu		changed	-> moved conversion to MatchRuleStringConverter
//
// Copyright (c) 2001-2007, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An instance of this class holds the name and the pattern values
 * for one attribute.
 * With the matches() method it can be checked against a Map of attributes.
 *
 * @author Manfred Duchrow
 * @version 2.0
 */
public class MatchAttribute extends MatchElement implements Serializable
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  /** The operator value for EQUALS */
	public static final int OPERATOR_EQUALS							= 1 ;
  /** The operator value for GREATER */
	public static final int OPERATOR_GREATER						= 2 ;
  /** The operator value for LESS */
	public static final int OPERATOR_LESS								= 3 ;
  /** The operator value for GREATER OR EQUAL */
	public static final int OPERATOR_GREATER_OR_EQUAL		= 4 ;
  /** The operator value for LESS OR EQUAL */
	public static final int OPERATOR_LESS_OR_EQUAL			= 5 ;

	private static MatchRuleTypeConverter TYPE_CONVERTER = new MatchRuleTypeConverter() ;
	
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private int operator = OPERATOR_EQUALS ;
  protected int operator() { return operator ; }
  protected void operator( int newValue ) { operator = newValue ; }  
  
  private String attributeName = null ;
  /**
   * Returns the name of the attribute that will be checked by this rule element
   */
  public String getAttributeName() { return attributeName ; }
  /**
   * Sets the name of the attribute that will be checked by this rule element
   */
  public void setAttributeName( String newValue ) { attributeName = newValue ; }

	// -------------------------------------------------------------------------

  private StringPattern[] patterns = null ;
  /**
   * Returns the value pattern(s) against that will be matched
   */
  public StringPattern[] getPatterns() { return patterns ; }
  /**
   * Sets the value pattern(s) against that will be matched
   */
  public void setPatterns( StringPattern[] newValue ) { patterns = newValue ; }

	// -------------------------------------------------------------------------

  private boolean ignoreCaseInName = false ;
  /**
   * Returns true, if the attribute name should be treated not case-sensitive.
   */
  public boolean ignoreCaseInName() { return ignoreCaseInName ; }
  /**
   * Sets whether the attribute name should be treated not case-sensitive.
   */
  protected void ignoreCaseInName( boolean newValue ) { ignoreCaseInName = newValue ; }
  
  // -------------------------------------------------------------------------
  
  private Object valueType = null ; // null means String!
  protected Object getValueType() { return valueType ; }
  protected void setValueType( Object newValue ) { valueType = newValue ; }
  
  // -------------------------------------------------------------------------
  
  private Float[] floatValues 					= null ;
  private Double[] doubleValues 				= null ;
  private BigDecimal[] bigDecimalValues = null ;
  private Integer[] integerValues 			= null ;
  private Long[] longValues 						= null ;
  private Date[] dateValues 						= null ;
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public MatchAttribute()
  {
    super() ;
  } // MatchAttribute() 

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with a name.
   * @param name The name of the attribute
   */
  public MatchAttribute( String name )
  {
    super() ;
    this.setAttributeName( name ) ;
  } // MatchAttribute() 

  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

	/**
	 * Returns true, if the element is an attribute element.
	 * <br>
	 * Here this method always returns true.
	 */
  public boolean isAttribute()
  {
    return true ;
  } // isAttribute() 

  // -------------------------------------------------------------------------

	/**
	 * Sets the specified pattern as the sole pattern to be checked when
	 * matching this attribute against a map.
	 */
	public void setPattern( StringPattern aPattern )
	{
		StringPattern[] p = new StringPattern[1] ;
		p[0] = aPattern ;
		this.setPatterns( p ) ;
	} // setPattern() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a string containing the attribute name, the operator and the
	 * value(s) set in this part of a match rule.
	 */
  public String toString()
  {
    StringBuffer str  		= new StringBuffer(40) ;
    boolean hasValueList  = false ;

    if ( this.getNot() )
      str.append( MatchRuleChars.DEFAULT_NOT_CHAR ) ;
      
    str.append( this.getAttributeName() ) ;
    hasValueList = this.getPatterns().length > 1 ;
    if ( hasValueList )
		{
	    str.append( MatchRuleChars.DEFAULT_VALUE_START_CHAR ) ;		
	    for ( int i = 0 ; i < this.getPatterns().length ; i++ )
	    {
	      if ( i > 0 )
	        str.append( MatchRuleChars.DEFAULT_VALUE_SEP_CHAR ) ;
	
	      str.append( ((StringPattern)this.getPatterns()[i]).getPattern() ) ;
	    }
    	str.append( MatchRuleChars.DEFAULT_VALUE_END_CHAR ) ;
		}
		else
		{
			switch ( this.operator() )
			{
				case OPERATOR_EQUALS :
					str.append( MatchRuleChars.DEFAULT_EQUALS_CHAR ) ;
					break ;
				case OPERATOR_GREATER :
					str.append( MatchRuleChars.DEFAULT_GREATER_CHAR ) ;
					break ;
				case OPERATOR_GREATER_OR_EQUAL :
					str.append( MatchRuleChars.DEFAULT_GREATER_CHAR ) ;
					str.append( MatchRuleChars.DEFAULT_EQUALS_CHAR ) ;
					break ;
				case OPERATOR_LESS :
					str.append( MatchRuleChars.DEFAULT_LESS_CHAR ) ;
					break ;
				case OPERATOR_LESS_OR_EQUAL :
					str.append( MatchRuleChars.DEFAULT_LESS_CHAR ) ;
					str.append( MatchRuleChars.DEFAULT_EQUALS_CHAR ) ;
					break ;
			}
			str.append( this.getPatterns()[0].toString() ) ;
		}
    	
    return str.toString() ;
  } // toString() 

  // -------------------------------------------------------------------------

	/**
	 * Sets the operator for value comparisons of this attribute to EQUALS.
	 */
	public void setEqualsOperator()
	{
		this.operator( OPERATOR_EQUALS ) ;
	} // setEqualsOperator() 

	// -------------------------------------------------------------------------

	/**
	 * Sets the operator for value comparisons of this attribute to GREATER.
	 */
	public void setGreaterOperator()
	{
		this.operator( OPERATOR_GREATER ) ;
	} // setGreaterOperator() 

	// -------------------------------------------------------------------------

	/**
	 * Sets the operator for value comparisons of this attribute to LESS.
	 */
	public void setLessOperator()
	{
		this.operator( OPERATOR_LESS ) ;
	} // setLessOperator() 

	// -------------------------------------------------------------------------

	/**
	 * Sets the operator for value comparisons of this attribute to GREATER OR EQUAL.
	 */
	public void setGreaterOrEqualOperator()
	{
		this.operator( OPERATOR_GREATER_OR_EQUAL ) ;
	} // setGreaterOrEqualOperator() 

	// -------------------------------------------------------------------------

	/**
	 * Sets the operator for value comparisons of this attribute to LESS OR EQUAL.
	 */
	public void setLessOrEqualOperator()
	{
		this.operator( OPERATOR_LESS_OR_EQUAL ) ;
	} // setLessOrEqualOperator() 

	// -------------------------------------------------------------------------

	/**
	 * Sets the datatype this attribute's value must have. 
	 * Implicitly the current value (pattern) gets converted to that datatype.
	 * <p>
	 * Currently supported datatypes are:
	 * <ul>
	 * <li>Float.class
	 * <li>Double.class
	 * <li>BigDecimal.class
	 * <li>Integer.class
	 * <li>Long.class
	 * <li>String.class
	 * <li>SimpleDateFormat
	 * </ul>
	 * 
	 * @param type The type of the attribute's value
	 * @throws MatchRuleException if the current value (pattern) cannot be converted to the specified datatype
	 */
	public void setDatatype( Object type )
		throws MatchRuleException
	{
		if ( ( type == null ) || ( type == String.class ) )
		{
			this.setValueType( null ) ;
			return ;
		}
		this.convertToType( type ) ;
		this.setValueType( type ) ;
	} // setDatatype() 

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  protected boolean doMatch( Map dictionary )
  {
    Object value        = null ;

    value = this.valueInMap( dictionary ) ;
    if ( value == null )
      return false ;
    
    if ( this.isTyped() )
		{
    	return this.doTypedMatch( value ) ;
		}
    
    if ( value instanceof String )
		{
			return this.matchValue( value ) ;
		}
		else if ( value instanceof String[] )
		{
			return this.matchValueArray( (Object[])value ) ;
		}
		else if ( value instanceof java.util.Collection )
		{
			return this.matchValueCollection( (Collection)value ) ;
		}
		else if ( value instanceof Integer )
		{
			return this.matchValue( value ) ;
		}
		else if ( value instanceof Integer[] )
		{
			return this.matchValueArray( (Object[])value ) ;
		}

    return false ;
  } // doMatch() 

  // -------------------------------------------------------------------------

  protected boolean doTypedMatch( final Object object ) 
	{
  	Object value = object ;
  	
  	if ( !this.isCorrectType( value ) )
    {
      value = TYPE_CONVERTER.convertToType( object, this.getValueType() ) ;
    }

		if ( this.isCorrectType( value ) )
		{
			try
			{
				if ( this.getValueType() == Float.class )
				{
					return this.doFloatMatch( value ) ;
				}
				if ( this.getValueType() == Double.class )
				{
					return this.doDoubleMatch( value ) ;
				}
				if ( this.getValueType() == BigDecimal.class )
				{
					return this.doBigDecimalMatch( value ) ;
				}
				if ( this.getValueType() == Integer.class )
				{
					return this.doIntegerMatch( value ) ;
				}
				if ( this.getValueType() == Long.class )
				{
					return this.doLongMatch( value ) ;
				}
				if ( this.getValueType() instanceof SimpleDateFormat )
				{
					return this.doDateMatch( value ) ;
				}
			}
			catch ( RuntimeException e )
			{
				return false ; // Class cast exception - should not occur
			}
		}
		return false ; // Type mismatch
	} // doTypedMatch() 

	// -------------------------------------------------------------------------
  
  protected boolean isCorrectType( Object value )
	{
  	if ( ( this.getValueType() instanceof SimpleDateFormat ) 
  			&& ( value.getClass() == Date.class ) )
		{
			return true ;
		}
		return this.getValueType() == this.getTypeOf( value );
	} // isCorrectType() 
  
  // -------------------------------------------------------------------------
  
	protected boolean doFloatMatch( Object value ) 
	{
		Float[] dataValues ;
		
		dataValues = (Float[])this.toArray( value, Float.class ) ;
		for (int i = 0; i < dataValues.length; i++ )
		{
			if ( this.matchValueAgainstValues( dataValues[i], floatValues ) )
				return true ;
		}
		return false ;
	} // doFloatMatch() 

	// -------------------------------------------------------------------------
  
  protected boolean doDoubleMatch( Object value ) 
	{
		Double[] dataValues ;
		
		dataValues = (Double[])this.toArray( value, Double.class ) ;
		for (int i = 0; i < dataValues.length; i++ )
		{
			if ( this.matchValueAgainstValues( dataValues[i], doubleValues ) )
				return true ;
		}
		return false ;
	} // doDoubleMatch() 

	// -------------------------------------------------------------------------
  
  protected boolean doBigDecimalMatch( Object value ) 
	{
  	BigDecimal[] dataValues ;
		
		dataValues = (BigDecimal[])this.toArray( value, BigDecimal.class ) ;
		for (int i = 0; i < dataValues.length; i++ )
		{
			if ( this.matchValueAgainstValues( dataValues[i], bigDecimalValues ) )
				return true ;
		}
		return false ;
	} // doBigDecimalMatch() 

	// -------------------------------------------------------------------------
  
  protected boolean doIntegerMatch( Object value ) 
	{
		Integer[] dataValues ;
		
		dataValues = (Integer[])this.toArray( value, Integer.class ) ;
		for (int i = 0; i < dataValues.length; i++ )
		{
			if ( this.matchValueAgainstValues( dataValues[i], integerValues ) )
			{
				return true ;
			}
		}
		return false ;
	} // doIntegerMatch() 

	// -------------------------------------------------------------------------
  
  protected boolean doLongMatch( Object value ) 
	{
  	Long[] dataValues ;
		
		dataValues = (Long[])this.toArray( value, Long.class ) ;
		for (int i = 0; i < dataValues.length; i++ )
		{
			if ( this.matchValueAgainstValues( dataValues[i], longValues ) )
				return true ;
		}
		return false ;
	} // doLongMatch() 

	// -------------------------------------------------------------------------
  
  protected boolean doDateMatch( Object value ) 
	{
  	Date[] dataValues ;
		
		dataValues = (Date[])this.toArray( value, Date.class ) ;
		for (int i = 0; i < dataValues.length; i++ )
		{
			if ( this.matchValueAgainstValues( dataValues[i], dateValues ) )
				return true ;
		}
		return false ;
	} // doDateMatch() 

	// -------------------------------------------------------------------------
  
  protected boolean matchValueArray( Object[] values )
  {
    for ( int i = 0 ; i < values.length ; i++ )
    {
      if ( this.matchValue( values[i] ) )
			{
				return true ;
			}
    }
    return false ;
  } // matchValueArray() 

  // -------------------------------------------------------------------------

  protected boolean matchValueCollection( Collection values )
  {
    Iterator iterator   = values.iterator() ;
    String value        = null ;
    
    while ( iterator.hasNext() )
    {
      try
      {
        value = (String)iterator.next() ;
        if ( this.matchValue( value ) )
				{
					return true ;
				}
      }
      catch ( Throwable t )
      {
        // Just ignore that value
      }
    }
    return false ;
  } // matchValueCollection() 

  // -------------------------------------------------------------------------

  protected boolean matchValue( Object value )
  {
    if ( value == null )
		{
			return false ;
		}
   
   StringPattern pattern ;
   String strValue ;
    
    for ( int i = 0 ; i < this.getPatterns().length ; i++ )
    {
    	pattern = this.getPatterns()[i] ;
    	if ( ( this.operator() == OPERATOR_EQUALS ) && ( pattern.hasWildcard() ) )
    	{
    		strValue = value.toString() ;
	      if ( pattern.matches( strValue ) )
	        return true ;
    	}
    	else
    	{
    		if ( value instanceof String )
    		{
    			if ( this.compare( (String)value, pattern.toString(), pattern.getIgnoreCase() ) )
					{
						return true ;
					}
    		}
    		else if ( value instanceof Integer )
    		{
    			if ( this.compare( (Integer)value, pattern.toString() ) )
					{
						return true ;
					}    			
    		}
    	}
    }

    return false ;
  } // matchValue() 

  // -------------------------------------------------------------------------

  protected boolean matchValueAgainstValues( Comparable value, Object[] values )
  {
  	int result ;
  	
    if ( value == null )
      return false ;
   
    for ( int i = 0 ; i < values.length ; i++ )
    {
    	result = value.compareTo( values[i] ) ;
 			if ( this.compareIntegers( result, 0 ) )
			{
				return true ;
			}    			
    }
    return false ;
  } // matchValueAgainstValues() 

  // -------------------------------------------------------------------------
    
	/**
	 * Returns true if the given value compared by using the current operator
	 * to the rule value evaluates to true.
	 */
	protected boolean compare( String value, String ruleValue, boolean ignoreCase)
	{
		int result ;
	
		if ( ignoreCase )
			result = value.compareToIgnoreCase( ruleValue ) ;
		else
			result = value.compareTo( ruleValue ) ;
			
		return this.compareIntegers( result, 0 ) ;
	} // compare() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the given value compared by using the current operator
	 * to the rule value evaluates to true.
	 */
	protected boolean compare( Integer value, String ruleValue )
	{
    int ruleIntValue ;
    
    try
		{
			ruleIntValue = Integer.parseInt( ruleValue ) ;
		}
		catch (RuntimeException e)
		{
			return false ;
		}
		return this.compareIntegers( value.intValue(), ruleIntValue ) ;
	} // compare() 

	// -------------------------------------------------------------------------

	protected boolean compareIntegers( int a, int b )
	{
		switch ( this.operator() )
		{
			case OPERATOR_EQUALS :
				return ( a == b ) ;
			case OPERATOR_GREATER_OR_EQUAL :
				return ( a >= b ) ;
			case OPERATOR_LESS_OR_EQUAL :			
				return a <= b ;
			case OPERATOR_GREATER :
				return ( a > b ) ;
			case OPERATOR_LESS :			
				return a < b ;
		} // switch
		return false ;		
	} // compareIntegers() 

	// -------------------------------------------------------------------------

  protected void ignoreCase( boolean ignoreIt )
  {
    for ( int i = 0 ; i < this.getPatterns().length ; i++ )
      this.getPatterns()[i].setIgnoreCase( ignoreIt ) ;
  } // ignoreCase() 

  // -------------------------------------------------------------------------

	protected void multiCharWildcardMatchesEmptyString( boolean yesOrNo )
	{
		for ( int i = 0 ; i < this.getPatterns().length ; i++ )
			this.getPatterns()[i].multiCharWildcardMatchesEmptyString( yesOrNo ) ;
	} // multiCharWildcardMatchesEmptyString() 
 
	// -------------------------------------------------------------------------

  protected void apply( MatchRuleVisitor visitor )
  {
    String[] values   = new String[this.getPatterns().length] ;

    for ( int i = 0 ; i < this.getPatterns().length ; i++ )
      values[i] = this.getPatterns()[i].getPattern() ;

    visitor.attribute( this.getAttributeName(), this.operator(), values,
                       this.getAnd(), this.getNot()  ) ;
  } // apply() 

  // -------------------------------------------------------------------------

	protected Object valueInMap( Map map )
	{
		String attrName ;
		
		attrName = this.nameOfAttribute( map ) ;
		if ( attrName == null )
			return null ;
			
		return map.get( attrName ) ;
	} // valueInMap() 

	// -------------------------------------------------------------------------
  
  protected String nameOfAttribute( Map map )
	{
		String name ;
		Set keyNames ;
		String key ;
		Iterator iterator ;

		name = this.getAttributeName() ;
		if ( this.ignoreCaseInName() )
		{
			keyNames = map.keySet() ;
			iterator = keyNames.iterator() ;
			while ( iterator.hasNext() )
			{
				key = (String)iterator.next() ;
				if ( name.equalsIgnoreCase( key ) )
					return key ;
			}
			name = null ;
		}
		return name ;
	} // nameOfAttribute() 

	// -------------------------------------------------------------------------

  protected void convertToType( Object type )
  	throws MatchRuleException
	{
  	String[] strValues ;
  	
  	strValues = new String[this.getPatterns().length] ;
    for ( int i = 0 ; i < this.getPatterns().length ; i++ )
    {
      strValues[i] = this.getPatterns()[i].getPattern() ;
    }

    if ( type == Float.class )
		{
    	this.convertToFloat( strValues );
			return;
		}
    if ( type == Double.class )
		{
    	this.convertToDouble( strValues );
			return;
		}
    if ( type == BigDecimal.class )
		{
    	this.convertToBigDecimal( strValues );
			return;
		}
    if ( type == Integer.class )
		{
    	this.convertToInteger( strValues );
			return;
		}
    if ( type == Long.class )
		{
    	this.convertToLong( strValues );
			return;
		}
    if ( type instanceof SimpleDateFormat )
		{
    	this.convertToDate( strValues, (SimpleDateFormat)type );
			return;
		}
		throw new MatchRuleException( "Type " + type + " not supported." ) ;
	} // convertToType() 

	// -------------------------------------------------------------------------
  
  protected void convertToFloat( String[] strValues ) 
  	throws MatchRuleException
	{
		floatValues = new Float[strValues.length] ;
		
		for (int i = 0; i < strValues.length; i++ )
		{
			try
			{
				floatValues[i] = Float.valueOf( strValues[i] ) ;
			}
			catch ( NumberFormatException e )
			{
				throw this.createTypeConversionException( strValues[i], Float.class ) ;
			}    		
		}
	} // convertToFloat() 
  
  // -------------------------------------------------------------------------
  
  protected void convertToDouble( String[] strValues ) 
		throws MatchRuleException
	{
		doubleValues = new Double[strValues.length] ;
		
		for (int i = 0; i < strValues.length; i++ )
		{
			try
			{
				doubleValues[i] = Double.valueOf( strValues[i] ) ;
			}
			catch ( NumberFormatException e )
			{
				throw this.createTypeConversionException( strValues[i], Double.class ) ;
			}    		
		}
	} // convertToDouble() 
	
	// -------------------------------------------------------------------------

  protected void convertToBigDecimal( String[] strValues ) 
		throws MatchRuleException
	{
		bigDecimalValues = new BigDecimal[strValues.length] ;
		
		for (int i = 0; i < strValues.length; i++ )
		{
			try
			{
				bigDecimalValues[i] = new BigDecimal( strValues[i] ) ;
			}
			catch ( NumberFormatException e )
			{
				throw this.createTypeConversionException( strValues[i], BigDecimal.class ) ;
			}    		
		}
	} // convertToBigDecimal() 
	
	// -------------------------------------------------------------------------

  protected void convertToInteger( String[] strValues ) 
		throws MatchRuleException
	{
		integerValues = new Integer[strValues.length] ;
		
		for (int i = 0; i < strValues.length; i++ )
		{
			try
			{
				integerValues[i] = Integer.valueOf( strValues[i] ) ;
			}
			catch ( NumberFormatException e )
			{
				throw this.createTypeConversionException( strValues[i], Integer.class ) ;
			}    		
		}
	} // convertToInteger() 
	
	// -------------------------------------------------------------------------
	
  protected void convertToLong( String[] strValues ) 
		throws MatchRuleException
	{
		longValues = new Long[strValues.length] ;
		
		for (int i = 0; i < strValues.length; i++ )
		{
			try
			{
				longValues[i] = Long.valueOf( strValues[i] ) ;
			}
			catch ( NumberFormatException e )
			{
				throw this.createTypeConversionException( strValues[i], Long.class ) ;
			}    		
		}
	} // convertToLong() 
	
	// -------------------------------------------------------------------------
	
  protected void convertToDate( String[] strValues, SimpleDateFormat dateFormat ) 
		throws MatchRuleException
	{
		dateValues = new Date[strValues.length] ;
		
		for (int i = 0; i < strValues.length; i++ )
		{
			try
			{
				dateValues[i] = dateFormat.parse( strValues[i] ) ;
			}
			catch ( ParseException e )
			{
				throw new MatchRuleException( "Unable to convert '" + strValues[i] 
				    + "' to Date with format \"" + dateFormat.toPattern() 
						+ "\" for attribute <" + this.getAttributeName() + ">" ) ;
			}    		
		}
	} // convertToDate() 
	
	// -------------------------------------------------------------------------

	protected MatchRuleException createTypeConversionException( String value, Class type ) 
	{
		return new MatchRuleException( "Unable to convert '" + value + "' to " 
				+ type + " for attribute <" + this.getAttributeName() + ">" ) ;
	} // createTypeConversionException() 

	// -------------------------------------------------------------------------
  
  protected boolean isTyped() 
	{
		return this.getValueType() != null ;
	} // isTyped() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns the type of the given object or if it is an array or a list the
   * type of its first element.
   */
  protected Class getTypeOf( Object object ) 
	{
		try
		{
			if ( object instanceof List )
			{
				return ((List)object).get(0).getClass() ;
			}
			if ( object instanceof Collection )
			{
				return ((Collection)object).iterator().next().getClass() ;
			}
			if ( object.getClass().isArray() )
			{
				return object.getClass().getComponentType() ;
			}
			return object.getClass() ;
		}
		catch ( RuntimeException e )
		{
			return null ;
		}
	} // getTypeOf() 

	// -------------------------------------------------------------------------
  
  protected Object[] toArray( Object object, Class type ) 
	{
  	Object[] array ;
  	Collection coll ;
  	
		if ( object instanceof Collection )
		{
			coll = (Collection)object ;
			array = (Object[])Array.newInstance( type, coll.size() ) ;
			return coll.toArray(array) ; 
		}
		if ( object.getClass().isArray() )
		{
			return (Object[])object ;
		}
		array = (Object[])Array.newInstance( type, 1 ) ;
		array[0] = object ;
		return array ;
	} // toArray() 

	// -------------------------------------------------------------------------
  
  protected void applyDatatypes( Map datatypes )
  	throws MatchRuleException 
	{
  	this.setDatatype( this.valueInMap( datatypes ) ) ;
	} // applyDatatypes() 

	// -------------------------------------------------------------------------
  
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
  
} // class MatchAttribute 
