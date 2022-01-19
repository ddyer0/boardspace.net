// ===========================================================================
// CONTENT  : CLASS ElementSpy
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 30/04/2004
// HISTORY  :
//  14/11/1999  duma  CREATED
//	11/01/2000	duma	changed	->	support subclassing in a better way by impl. of default behavior
//	30/04/2004	duma	changed	->	support for editing primitive types and String
//
// Copyright (c) 1999-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.lang.reflect.Modifier;

/**
 * Instances of this class are holding information about the elements
 * of an inspected object.
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
abstract public class ElementSpy extends Spy implements Comparable
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	private static final Class[] EDITABLE_CLASSES =
		{ 
			String.class, Integer.class, Long.class, Boolean.class, Character.class,
			Short.class, Byte.class, Float.class, Double.class
		} ;
	
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private AbstractObjectSpy container = null ;
  protected AbstractObjectSpy getContainer() { return container ; }
  protected void setContainer( AbstractObjectSpy aValue ) { container = aValue ; }

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public ElementSpy( AbstractObjectSpy object )
  {
  	this.setContainer( object ) ;
  } // ElementSpy() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns true because this spy is an element spy.
	 */
	public boolean isElementSpy() 
	{
		return true ;
	} // isElementSpy() 

	// -------------------------------------------------------------------------
	
  /**
	 * Returns the modifiers of the elements declaration.    <br>
	 * The default is to simulate simple public fields.
	 * Subclasses may override this method.
	 */
	public int getModifiers()
	{
		return ( Modifier.PUBLIC )  ;
	} // getModifiers() 

	// --------------------------------------------------------------------------

	public int compareTo(Object obj)
	{
		if ( ! ( obj instanceof ElementSpy ) )
			return -1 ;

		ElementSpy eSpy = (ElementSpy)obj ;

		return ( this.getName().compareTo( eSpy.getName() ) ) ;
	} // compareTo() 

  // --------------------------------------------------------------------------

	/**
	 * Sets the given object as the new value of the spy's inspected object
	 */
	public void setValue( Object newValue ) 
		throws Exception
	{
		if ( this.isEditable() )
		{
			if ( this.isCorrectType( newValue ) )
			{
				this.modifyValue( newValue );
			}
			else
			{
				throw new Exception( "Incompatible type: " + newValue.getClass().getName() ) ;
			}
		}
	} // setValue() 

	// -------------------------------------------------------------------------
		
	/**
	 * Currently allows to edit String values and primitive types only
	 */
	public boolean isEditable() 
	{
		Class type ;

		if ( this.getContainer().allowsElementModification() )
		{
			if ( Modifier.isFinal( this.getModifiers() ) )
				return false ;
			
			type = this.getCurrentType() ;
			for (int i = 0; i < EDITABLE_CLASSES.length; i++ )
			{
				if ( type.isAssignableFrom( EDITABLE_CLASSES[i] ) )
					return true ;
			}
	
			if ( type.isPrimitive() )
			{
				return true ;
			}
		}
		return false ;
	} // isEditable() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the current value of this element spy ís of type
	 * int or Integer
	 */
	public boolean is_Integer_or_int() 
	{
		return this.is_int() || ( this.getCurrentType() == Integer.class  ) ; 
	} // is_Integer_or_int() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the current value of this element spy ís of type
	 * long or Long
	 */
	public boolean is_Long_or_long() 
	{
		return this.is_long() || ( this.getCurrentType() == Long.class  ) ; 
	} // is_Long_or_long() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the current value of this element spy ís of type
	 * short or Short
	 */
	public boolean is_Short_or_short() 
	{
		return this.is_short() || ( this.getCurrentType() == Short.class  ) ; 
	} // is_Short_or_short() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the current value of this element spy ís of type
	 * byte or Byte
	 */
	public boolean is_Byte_or_byte() 
	{
		return this.is_byte() || ( this.getCurrentType() == Byte.class  ) ; 
	} // is_Byte_or_byte() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the current value of this element spy ís of type
	 * float or Float
	 */
	public boolean is_Float_or_float() 
	{
		return this.is_float() || ( this.getCurrentType() == Float.class  ) ; 
	} // is_Float_or_float() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the current value of this element spy ís of type
	 * double or Double
	 */
	public boolean is_Double_or_double() 
	{
		return this.is_double() || ( this.getCurrentType() == Double.class  ) ; 
	} // is_Double_or_double() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the current value of this element spy ís of type
	 * char or Character
	 */
	public boolean is_Character_or_char() 
	{
		return this.is_char() || ( this.getCurrentType() == Character.class  ) ; 
	} // is_Character_or_char() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the current value of this element spy ís of type
	 * long or Long
	 */
	public boolean is_Boolean_or_boolean() 
	{
		return this.is_boolean() || ( this.getCurrentType() == Boolean.class  ) ; 
	} // is_Boolean_or_boolean() 

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns the type of this element.
	 * Subclasses may override this to be more specific
	 */
	protected Class getCurrentType() 
	{
		return this.getType() ;
	} // getCurrentType() 

	// -------------------------------------------------------------------------
	
  protected boolean isCorrectType( Object obj ) 
	{
  	Class type ;
  	
  	type = this.getType() ;
  	if ( type.isPrimitive() )
		{
			if ( this.is_int() && ( obj instanceof Integer ) ) return true ;
			if ( this.is_long() && ( obj instanceof Long ) ) return true ;
			if ( this.is_boolean() && ( obj instanceof Boolean ) ) return true ;
			if ( this.is_char() && ( obj instanceof Character ) ) return true ;
			if ( this.is_short() && ( obj instanceof Short ) ) return true ;
			if ( this.is_byte() && ( obj instanceof Byte ) ) return true ;
			if ( this.is_double() && ( obj instanceof Double ) ) return true ;
			if ( this.is_float() && ( obj instanceof Float ) ) return true ;
		}
  	else
  	{
  		if ( obj == null )  // null can be assigned to objects
  			return true ;
  	}
  	return this.getType().isInstance( obj ) ;
	} // isCorrectType() 

	// -------------------------------------------------------------------------
	
	protected void modifyValue( Object newValue )
		throws Exception
	{
		this.getContainer().setElementValue( this, newValue ) ;
	} // modifyValue() 
	
	// --------------------------------------------------------------------------
		
} // class ElementSpy 
