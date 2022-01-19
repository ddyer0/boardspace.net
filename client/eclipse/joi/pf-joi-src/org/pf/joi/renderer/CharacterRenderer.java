// ===========================================================================
// CONTENT  : CLASS CharacterRenderer
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 26/06/2000
// HISTORY  :
//  26/06/2000  duma  CREATED
//
// Copyright (c) 2000, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi.renderer;

import org.pf.joi.ObjectRenderer;

// ===========================================================================
// CONTENT  : CLASS CharacterRenderer
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 26/06/2000
// HISTORY  :
//  26/06/2000  duma  CREATED
//
// Copyright (c) 2000, by Manfred Duchrow. All rights reserved.
// ===========================================================================
// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * This class is responsible to convert Character instances to their
 * String representation in inspectors.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class CharacterRenderer implements ObjectRenderer
{
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public CharacterRenderer()
  {
  	super() ;
  } // CharacterRenderer()  

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  /**
   * Returns the string representation of the specified object.
   *
   * @param obj Must be a Character !
   */
  public String inspectString( Object obj )
  {
		String str  = null ;
		int charVal = 0 ;
	
		charVal = (int)( ((Character)obj).charValue() ) ;
		str = " " + obj.toString() + "        dec: " + Integer.toString( charVal )
				  + "        hex: " + Integer.toHexString( charVal ).toUpperCase() ;
		return str ;
  } // inspectString()  

} // class CharacterRenderer