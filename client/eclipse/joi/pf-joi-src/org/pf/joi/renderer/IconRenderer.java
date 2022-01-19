// ===========================================================================
// CONTENT  : CLASS IconRenderer
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 09/04/2004
// HISTORY  :
//  09/04/2004  mdu  CREATED
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi.renderer;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JLabel;

import org.pf.joi.ObjectRenderer2;

/**
 * Provides a JLabel containing the icon.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class IconRenderer implements ObjectRenderer2
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
  public IconRenderer()
  {
    super() ;
  } // IconRenderer() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * @see org.pf.joi.ObjectRenderer2#inspectComponent(java.lang.Object)
	 */
	public Component inspectComponent( Object obj )
	{
		if ( obj instanceof Icon )
		{
			return new JLabel( (Icon)obj ) ;
		}
		return null;
	} // inspectComponent()

	// -------------------------------------------------------------------------
	
	/**
	 * @see org.pf.joi.ObjectRenderer#inspectString(java.lang.Object)
	 */
	public String inspectString( Object obj )
	{
		return obj.toString() ;
	} // inspectString()
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class IconRenderer 
