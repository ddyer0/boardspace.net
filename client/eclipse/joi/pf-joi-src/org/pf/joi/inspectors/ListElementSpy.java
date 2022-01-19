// ===========================================================================
// CONTENT  : CLASS ListElementSpy
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 24/04/2004
// HISTORY  :
//	11/01/2000	duma	CREATED
//	24/04/2004	duma	changed	-->	Support element modification
//
// Copyright (c) 2000-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi.inspectors;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.joi.AbstractObjectSpy;
import org.pf.joi.CollectionElementSpy;

/**
 * Wrapper class for elements in objects that implement the 
 * <i>java.util.List</i> interface.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class ListElementSpy extends CollectionElementSpy
{ 
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private int index = 0 ;
  protected int getIndex() { return index ; }
  protected void setIndex( int newValue ) { index = newValue ; }

  // =========================================================================
  // CLASS METHODS
  // =========================================================================
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public ListElementSpy( AbstractObjectSpy container, int pos, 
  															Object object )
  {
  	super( container, Integer.toString(pos), object ) ; 
  	this.setIndex( pos ) ;
  } // ListElementSpy()  

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected void modifyValue( Object newValue )
		throws Exception
	{
		super.modifyValue( newValue ) ;
		this.setObject( newValue ) ;
	} // modifyValue() 
	
	// --------------------------------------------------------------------------
  
} // class ListElementSpy