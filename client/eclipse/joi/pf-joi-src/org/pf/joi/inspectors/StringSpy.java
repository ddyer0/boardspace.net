// ===========================================================================
// CONTENT  : CLASS StringSpy
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 10/04/2004
// HISTORY  :
//  26/06/2000  duma  CREATED
//	10/04/2004	duma	changed	-->	addAllElements()
//
// Copyright (c) 2000-20024, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi.inspectors;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.joi.AbstractObjectSpy;
import org.pf.joi.ArrayElementSpy;
import org.pf.joi.ElementSpy;

/**
 * Instances of this class are responsible to provide the internal state
 * of strings for presentation in an inspector.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class StringSpy extends AbstractObjectSpy
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
  public StringSpy( Object obj )
  	throws SecurityException
  {
  	super( obj ) ;
  } // StringSpy()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

	/**
	 * Returns the wrapped String (is doing the type cast).
	 */
	protected String getString()
	{
		return ( (String)this.getObject() ) ;
	} // getString()

	// --------------------------------------------------------------------------

	protected void addAllElements()
		throws SecurityException
	{
		ElementSpy elementSpy	    = null ;
		String str                = null ;
		int index									= 0 ;

		str = this.getString() ;
		while ( index < str.length() )
		{
			elementSpy = new ArrayElementSpy( this, index,
										str.charAt( index ) ,
										Character.TYPE ) ;
			this.getElementHolders().add( elementSpy ) ;
			index++ ;
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

	// --------------------------------------------------------------------------

} // class StringSpy