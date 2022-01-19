// ===========================================================================
// CONTENT  : CLASS AttributeSpy
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 09/04/2004
// HISTORY  :
//  14/11/1999  duma  CREATED
//	09/04/2004	duma	added		-->	isEditable()		
//
// Copyright (c) 1999-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.lang.reflect.Field;

/**
 * Instances of this class are holding information about the attributes
 * of an inspected object.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class AttributeSpy extends ElementSpy
{ 
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private Field field = null ;
	protected Field getField() { return field ; }
	protected void setField( Field newValue ) { field = newValue ; }
	  
	private boolean inheritedFlag = false ;
	protected boolean getInheritedFlag() { return inheritedFlag ; }
	protected void setInheritedFlag( boolean newValue ) { inheritedFlag = newValue ; }
	  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public AttributeSpy( AbstractObjectSpy object, Field field )
  {
  	super( object ) ;
  	this.setField( field ) ;
  } // AttributeSpy() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	public Class getType()
	{
		return ( this.getField().getType() ) ;
	} // getType() 

	// --------------------------------------------------------------------------
	
	public String getName()
	{
		return ( this.getField().getName() ) ;
	} // getName() 

	// --------------------------------------------------------------------------

	public int getModifiers()
	{
		return ( this.getField().getModifiers() ) ;
	} // getModifiers() 

	// --------------------------------------------------------------------------

	public Object getValue()
		throws Exception
	{
		Object value 		= null ;
		boolean saveAccessibility	= false ;
		
		saveAccessibility = this.getField().isAccessible() ;
		this.getField().setAccessible( true ) ;
		try
		{
			value = this.getField().get( this.getContainer().getObject() ) ;
		}
		catch ( NullPointerException ex )
		{
			// Ignore this, because null values are allowed !
		}
		this.getField().setAccessible( saveAccessibility ) ;
		
		return value ;
	} // getValue() 

	// --------------------------------------------------------------------------

	public boolean isInherited()
	{
		return this.getInheritedFlag() ;
	} // isInherited() 
 
	// --------------------------------------------------------------------------

	public void beInherited() 
	{
		this.setInheritedFlag( true ) ;
	} // beInherited() 
	
	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns the type of the contained element.
	 */
	protected Class getCurrentType()
	{
		Object value ;
		
		try
		{
			value = this.getValue() ;
			
			if ( value != null )
			{
				return this.getValue().getClass();
			}
		}
		catch ( Exception e )
		{
			// Ignore
		}
		return this.getType();
	} // getCurrentType() 

	// --------------------------------------------------------------------------

} // class AttributeSpy 
