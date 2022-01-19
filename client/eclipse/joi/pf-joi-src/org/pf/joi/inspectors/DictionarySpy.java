// ===========================================================================
// CONTENT  : CLASS DictionarySpy
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 22/07/2007
// HISTORY  :
//  22/07/2007  mdu  CREATED
//
// Copyright (c) 2007, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi.inspectors ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Dictionary;
import java.util.Enumeration;

import org.pf.joi.AbstractObjectSpy;
import org.pf.util.Association;

/**
 * This object spy is responsible to provide the internal data of a 
 * java.util.Dictionary instance.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class DictionarySpy extends AbstractObjectSpy
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
   * Initialize the new instance with the object to look into.
   */
  public DictionarySpy( Object obj )
  {
    super( obj ) ;
  } // DictionarySpy()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns the wrapped map (is doing the type cast).
	 */
	protected Dictionary getDictionary()
	{
		return ( (Dictionary)this.getObject() ) ;
	} // getMap()

	// --------------------------------------------------------------------------

	protected void addAllElements() throws SecurityException
	{
		Enumeration keys ;
		Object key ;
		Object value ;
		Association association ;
		AssociationSpy elementSpy ;

		keys = this.getDictionary().keys() ;
		while ( keys.hasMoreElements() )
		{
			key = keys.nextElement() ;
			value = this.getDictionary().get( key ) ;
			association = new Association( key, value ) ;
			elementSpy = new AssociationSpy( this, association );
			this.getElementHolders().add( elementSpy );
		}
	} // addAllElements()

	// --------------------------------------------------------------------------
	
} // class DictionarySpy
