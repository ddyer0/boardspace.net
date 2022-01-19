// ===========================================================================
// CONTENT  : CLASS ObjectSpy
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 09/04/2004
// HISTORY  :
//  14/11/1999  duma  CREATED
//	09/04/2004	duma	added		-->	setElementValue()
//
// Copyright (c) 1999-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.lang.reflect.Field;

/**
 * An instance of this class is a wrapper for one inspected object.
 * It provides the API an inspector is using, to display internal information
 * about the inspected object.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class ObjectSpy extends AbstractObjectSpy
{ 
	// --------------------------------------------------------------------------

  // =========================================================================
  // CONSTANTS
  // =========================================================================
  
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
 
  // =========================================================================
  // CLASS METHODS
  // =========================================================================
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /** 
   * Initialize the new instance with default values.
   */
  public ObjectSpy( Object obj )
  	throws SecurityException
  {
  	super( obj ) ; 
  } // ObjectSpy()  

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

	// --------------------------------------------------------------------------
	  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

	protected void addFields( Field[] fields, boolean inherited )
	{
		AttributeSpy elementSpy	= null ;
		
		for ( int i = 0 ; i < fields.length ; i++ )
		{
			elementSpy = this.wrapField( fields[i] ) ;
			if ( inherited )
			{
				elementSpy.beInherited() ;
			}
			this.getElementHolders().add( elementSpy ) ;
		}
	} // addFields()

	// --------------------------------------------------------------------------

	protected void addInheritedFields( Class aClass )
		throws SecurityException 
	{
		if ( aClass != null )
		{
			this.addInheritedFields( aClass.getSuperclass() ) ;
			this.addFields( aClass.getDeclaredFields(), true ) ;
		}
	} // addInheritedFields()

	// --------------------------------------------------------------------------
 
	protected void addAllElements()
		throws SecurityException 
	{
		Class aClass			= null ;
		
		aClass = this.getObject().getClass() ;
		this.addInheritedFields( aClass.getSuperclass() ) ;
		this.addFields( aClass.getDeclaredFields(), false ) ;
	} // addAllElements()

	// --------------------------------------------------------------------------

  protected AttributeSpy wrapField( java.lang.reflect.Field field )
	{
		AttributeSpy elementSpy 	= null ;
		
		elementSpy = new AttributeSpy( this, field ) ;
		return ( elementSpy ) ;
	} // wrapField()

	// --------------------------------------------------------------------------

	/**
	 * Returns true. That is this object allows the modification of its elements.
	 */
	protected boolean allowsElementModification() 
	{
		return true ;
	} // allowsElementModification()

	// -------------------------------------------------------------------------

	/**
	 * Sets the value of the specified element to the given value.
	 */
	protected void setElementValue( ElementSpy element, Object value )
		throws Exception
	{
		Field field ;
		AttributeSpy attrSpy ;
		boolean saveAccessibility	;
		
		if ( element instanceof AttributeSpy )
			attrSpy = (AttributeSpy)element ;
		else
			throw new Exception( "Invalid element spy for '" + element.getName() + "' found" ) ;
			
		field = attrSpy.getField() ;
		saveAccessibility = field.isAccessible() ;
		field.setAccessible( true ) ;
		try
		{
			field.set( this.getValue(), value ) ;
		}
		finally
		{
			field.setAccessible( saveAccessibility ) ;
		}
	} // setElementValue()

	// -------------------------------------------------------------------------
  
} // class ObjectSpy