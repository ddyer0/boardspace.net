// ===========================================================================
// CONTENT  : CLASS AssociationListSpy
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 24/04/2004
// HISTORY  :
//  24/04/2004  mdu  CREATED
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi.inspectors ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.joi.AbstractObjectSpy;
import org.pf.util.Association;
import org.pf.util.AssociationList;

/**
 * The spy that looks inside AssociationList objects
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class AssociationListSpy extends AbstractObjectSpy
{
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
  public AssociationListSpy( Object obj )
  {
    super(obj) ;
  } // AssociationListSpy() 

  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected void addAllElements()
		throws SecurityException 
	{
		AssociationList list ;
		AssociationSpy elementSpy ;
		Association assoc ;

		list = this.getList() ;
		for (int i = 0; i < list.size() ; i++ )
		{
			assoc = list.associationAt(i) ;
			elementSpy = new AssociationSpy( this, assoc ) ;
			this.getElementHolders().add( elementSpy ) ;
		}
	} // addAllElements() 
	
	// --------------------------------------------------------------------------

	protected AssociationList getList() 
	{
		return (AssociationList)this.getObject() ;
	} // getList() 

	// -------------------------------------------------------------------------
	
} // class AssociationListSpy 
