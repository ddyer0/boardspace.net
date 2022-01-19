// ===========================================================================
// CONTENT  : CLASS ListSpy
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 11/01/2000
// HISTORY  :
//  11/01/2000  duma  CREATED
//
// Copyright (c) 2000, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi.inspectors;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Iterator;
import java.util.List;

import org.pf.joi.AbstractObjectSpy;
import org.pf.joi.ElementSpy;

/**
 * An instance of this class is a wrapper for one inspected object that implements
 * the <i>java.util.List</i> interface.
 * It provides the API an inspector is using, to display internal information
 * about the inspected array.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class ListSpy extends AbstractObjectSpy
{
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public ListSpy( Object obj )
  	throws SecurityException
  {
  	super( obj ) ;
  } // ListSpy()

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

	/**
	 * Returns the wrapped list (is doing the type cast).
	 */
	protected List getList()
	{
		return ( (List)this.getObject() ) ;
	} // getList()

	// --------------------------------------------------------------------------

	protected void addAllElements()
		throws SecurityException
	{
		ListElementSpy elementSpy	= null ;
		Iterator iterator					= null ;
		int index									= 0 ;

		iterator = this.getList().iterator() ;
		while ( iterator.hasNext() )
		{
			elementSpy = new ListElementSpy( this, index, iterator.next() ) ;
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
		ListElementSpy listElementSpy ;
		
		if ( element instanceof ListElementSpy )
			listElementSpy = (ListElementSpy)element ;
		else
			throw new Exception( "Invalid spy for element '" + element.getName() + "' found" ) ;

		this.getList().set( listElementSpy.getIndex(), value ) ;
	} // setElementValue()

	// -------------------------------------------------------------------------

} // class ListSpy