// ===========================================================================
// CONTENT  : CLASS AssociationSpy
// AUTHOR   : M.Duchrow
// VERSION  : 1.1 - 22/07/2007
// HISTORY  :
//  23/04/2004    CREATED
//	22/07/2007	mdu		changed	-->	Added constructor for Map.Entry
//
// Copyright (c) 2004-2007, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi.inspectors;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Map;

import org.pf.joi.AbstractObjectSpy;
import org.pf.joi.CollectionElementSpy;
import org.pf.util.Association;

/**
 * A spy class for Association objects
 *
 * @author M.Duchrow
 * @version 1.1
 */
public class AssociationSpy extends CollectionElementSpy
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
   * Initialize the new instance with a container spy and an association.
   */
  public AssociationSpy(AbstractObjectSpy container, Association association )
  {
  	super( container, association.key().toString(), association ) ;
  } // AssociationSpy()
  
  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with a container spy and a Map.Entry.
   */
  public AssociationSpy(AbstractObjectSpy container, Map.Entry mapEntry )
  {
  	this( container, new Association( mapEntry.getKey(), mapEntry.getValue() ) ) ;
  } // AssociationSpy()
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns the value of the underlying association.
	 */
	public Object getValue()
	{
		return this.getAssociation().value() ;
	} // getValue()

  // -------------------------------------------------------------------------

	/**
	 * Returns the key of the underlying association.
	 */
	public Object getKey()
	{
		return this.getAssociation().key() ;
	} // getKey()

	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected Association getAssociation()
	{
		return (Association)this.getObject() ;
	} // getAssociation()

	// -------------------------------------------------------------------------

} // class AssociationSpy
