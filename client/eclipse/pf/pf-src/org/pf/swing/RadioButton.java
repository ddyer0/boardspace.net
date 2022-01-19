// ===========================================================================
// CONTENT  : CLASS RadioButton
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 14/09/2003
// HISTORY  :
//  14/09/2003  duma  CREATED
//
// Copyright (c) 2003, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.swing ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JRadioButton;

/**
 * A radio button with some additional convenience nethods
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class RadioButton extends JRadioButton
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private Object value = null ;

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public RadioButton()
  {
    super() ;
  } // RadioButton()
 
	// -------------------------------------------------------------------------

	/**
	 * Constructor for RadioButton.
	 * @param icon
	 */
	public RadioButton(Icon icon)
	{
		super(icon);
	} // RadioButton()

	// -------------------------------------------------------------------------

	/**
	 * Constructor for RadioButton.
	 * @param a
	 */
	public RadioButton(Action a)
	{
		super(a);
	} // RadioButton()

	// -------------------------------------------------------------------------

	/**
	 * Constructor for RadioButton.
	 * @param icon
	 * @param selected
	 */
	public RadioButton(Icon icon, boolean selected)
	{
		super(icon, selected);
	} // RadioButton()

	// -------------------------------------------------------------------------

	/**
	 * Constructor for RadioButton.
	 * @param text
	 */
	public RadioButton(String text)
	{
		super(text);
	} // RadioButton()

	// -------------------------------------------------------------------------

	/**
	 * Constructor for RadioButton.
	 * @param text
	 * @param selected
	 */
	public RadioButton(String text, boolean selected)
	{
		super(text, selected);
	} // RadioButton()

	// -------------------------------------------------------------------------

	/**
	 * Constructor for RadioButton.
	 * @param text
	 * @param icon
	 */
	public RadioButton(String text, Icon icon)
	{
		super(text, icon);
	} // RadioButton()

	// -------------------------------------------------------------------------

	/**
	 * Constructor for RadioButton.
	 * @param text
	 * @param icon
	 * @param selected
	 */
	public RadioButton(String text, Icon icon, boolean selected)
	{
		super(text, icon, selected);
	} // RadioButton()

	// -------------------------------------------------------------------------

	/**
	 * Constructor for RadioButton.
	 * @param text The label of thie new radio button
	 * @param value The value this radio button represents
	 */
	public RadioButton(String text, Object value)
	{
		super(text);
		this.setValue( value ) ;
	} // RadioButton()

	// -------------------------------------------------------------------------

	/**
	 * Constructor for RadioButton.
	 * @param text The label of thie new radio button
	 * @param value The value this radio button represents
	 * @param selected The initial state
	 */
	public RadioButton(String text, Object value, boolean selected)
	{
		super(text, selected);
		this.setValue( value ) ;
	} // RadioButton()

	// -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Return the value this button represents.
   */
  public Object getValue()
  {
  	return value ;
  } // getValue()
 
  // -------------------------------------------------------------------------

  /**
   * Sets the value this button represents.
   */
  public void setValue( Object newValue )
  {
  	value = newValue ;
  } // setValue()
 
	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class RadioButton
