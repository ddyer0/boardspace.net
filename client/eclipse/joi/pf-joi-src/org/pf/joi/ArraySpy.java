// ===========================================================================
// CONTENT  : CLASS ArraySpy
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 24/11/1999
// HISTORY  :
//  24/11/1999  duma  CREATED
//
// Copyright (c) 1999, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.lang.reflect.Array;

/**
 * An instance of this class is a wrapper for one inspected array.
 * It provides the API an inspector is using, to display internal information
 * about the inspected array.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class ArraySpy extends AbstractObjectSpy
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
  public ArraySpy( Object obj )
  	throws SecurityException
  {
  	super( obj ) ;
  } // ArraySpy()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

	// --------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

	protected void addAllElements()
		throws SecurityException
	{
		ArrayElementSpy elementSpy		= null ;
		Object obj										= null ;
		int length										= 0 ;
		int index											= 0 ;

		length = Array.getLength( this.getObject() ) ;
		for ( index = 0 ; index < length ; index++ )
		{
			obj = Array.get( this.getObject(), index ) ;
			elementSpy = new ArrayElementSpy( this,	index, obj,
																				this.getType().getComponentType() ) ;
			this.getElementHolders().add( elementSpy ) ;
		}
	} // addAllElements()

	// --------------------------------------------------------------------------

	/**
	 * Returns whether or not the elements
	 * of the underlying object can be sorted.
	 */
	protected boolean canBeSorted()
	{
		return false ;
	} // canBeSorted()
	
	// -------------------------------------------------------------------------
	
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
		ArrayElementSpy arrayElementSpy ;
		
		if ( element instanceof ArrayElementSpy )
			arrayElementSpy = (ArrayElementSpy)element ;
		else
			throw new Exception( "Invalid spy for element '" + element.getName() + "' found" ) ;

		Array.set( this.getObject(), arrayElementSpy.getIndex(), value ) ;
	} // setElementValue()

	// -------------------------------------------------------------------------

} // class ArraySpy