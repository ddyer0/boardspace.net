// ===========================================================================
// CONTENT  : CLASS RadioButtonGroup
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 14/09/2003
// HISTORY  :
//  14/09/2003  mdu  CREATED
//
// Copyright (c) 2003, by Manfred Duc^hrow. All rights reserved.
// ===========================================================================
package org.pf.swing ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;

/**
 * A radio button group with some additional convenience methods
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class RadioButtonGroup extends ButtonGroup
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
  public RadioButtonGroup()
  {
    super() ;
  } // RadioButtonGroup()
 
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * @see javax.swing.ButtonGroup#add(AbstractButton)
	 * Allows only RadioButton instances to be added
	 *
	 * @throws IllegalArgumentException If the given button is not an instance of RadioButton
	 */
	public void add(AbstractButton button)
	{
		if ( ! ( button instanceof RadioButton ) )
			throw new IllegalArgumentException( "RadioButton expected" ) ;

		super.add(button);
	} // add()
 
	// -------------------------------------------------------------------------

	/**
	 * Changes the selection to the radio button which has the same value
	 * as the given parameter.
	 *
	 * @param valueOfButton The value of the button to select
	 * @throws IllegalArgumentException If the given value is null
	 */
	public void selectByValue( Object valueOfButton )
	{
		Enumeration enumeration ;
		RadioButton button ;

		enumeration = this.getElements() ;
		while ( enumeration.hasMoreElements() )
		{
			button = (RadioButton)enumeration.nextElement();
			if ( valueOfButton.equals( button.getValue() ) )
			{
				button.setSelected(true) ;
				break ;
			}
		}
	} // selectByValue()
 
	// -------------------------------------------------------------------------

	/**
	 * Returns the value associated with the button currently selected.
	 * If none is selected, null will be returned.
	 */
	public Object getSelectedValue()
	{
		Enumeration enumeration ;
		RadioButton button ;

		enumeration = this.getElements() ;
		while ( enumeration.hasMoreElements() )
		{
			button = (RadioButton)enumeration.nextElement();
			if ( button.isSelected() )
			{
				return button.getValue() ;
			}
		}
		return null ;
	} // getSelectedValue()
 
	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class RadioButtonGroup
