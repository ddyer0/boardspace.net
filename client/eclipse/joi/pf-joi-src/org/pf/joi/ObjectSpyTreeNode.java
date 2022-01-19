// ===========================================================================
// CONTENT  : CLASS ObjectSpyTreeNode
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 17/11/1999
// HISTORY  :
//  17/11/1999  duma  CREATED
//
// Copyright (c) 1999, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi;

// ===========================================================================
// CONTENT  : CLASS ObjectSpyTreeNode
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 17/11/1999
// HISTORY  :
//  17/11/1999  duma  CREATED
//
// Copyright (c) 1999, by Manfred Duchrow. All rights reserved.
// ===========================================================================
// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * This tree node is a holder for ObjectSpy instances. It supports the
 * correct handling of such instances in a tree.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class ObjectSpyTreeNode extends SpyTreeNode
{ 
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private AbstractObjectSpy object = null ;
	protected AbstractObjectSpy getObject() { return object ; }
	protected void setObject( AbstractObjectSpy newValue ) { object = newValue ; }

  // =========================================================================
  // CLASS METHODS
  // =========================================================================
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public ObjectSpyTreeNode( AbstractObjectSpy objSpy )
  {
  	super() ;
  	this.setObject( objSpy ) ;
  } // ObjectSpyTreeNode()  

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

	public int getElementCount()
	{
		return ( this.getChildren().size() + 1 ) ;
	} // getElementCount()

	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  // -------------------------------------------------------------------------
  
// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  // -------------------------------------------------------------------------
  
} // class ObjectSpyTreeNode