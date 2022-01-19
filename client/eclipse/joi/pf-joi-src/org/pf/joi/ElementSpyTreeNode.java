// ===========================================================================
// CONTENT  : CLASS ElementSpyTreeNode
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 17/11/1999
// HISTORY  :
//  17/11/1999  duma  CREATED
//
// Copyright (c) 1999, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * This is a holder for ElementSpy instances. It supports their correct
 * handling in visual trees.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class ElementSpyTreeNode extends SpyTreeNode
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private ElementSpy object = null ;
	protected ElementSpy getObject() { return object ; }
	protected void setObject( ElementSpy newValue ) { object = newValue ; }

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public ElementSpyTreeNode( ElementSpy elemSpy )
  {
  	super() ;
  	this.setObject( elemSpy ) ;
  } // ElementSpyTreeNode()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns the receiver's underlying model object.
	 */
	public Spy getModel()
	{
		return ( this.getObject() ) ;
	} // getModel()

	// -------------------------------------------------------------------------

	public String toString()
	{
		return ( this.getObject().getName() ) ;
	} // toString()

  // -------------------------------------------------------------------------

} // class ElementSpyTreeNode