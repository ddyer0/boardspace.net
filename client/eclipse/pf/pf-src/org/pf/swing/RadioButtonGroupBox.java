// ===========================================================================
// CONTENT  : CLASS RadioButtonGroupBox
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 23/09/2003
// HISTORY  :
//  14/09/2003  mdu  CREATED
//
// Copyright (c) 2003, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.swing ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

/**
 * This is a container component for radio buttons that simplifies usage
 * of a group of radio buttons and the selection change handling.
 * <p>
 * It support change listeners, which get called whenever the selected
 * radio button changes. Then the getSource() of the ChangeEvent contains
 * the RadioButtonGroup object which can be asked for the currently selected
 * value.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class RadioButtonGroupBox extends JPanel implements ActionListener
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	public static final int NEW_BUTTON_SET_ID			= 1 ; 
	public static final String NEW_BUTTON_SET_CMD		= "radio.button.set" ; 
	
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private RadioButtonGroup buttonGroup = null ;
	protected void setButtonGroup( RadioButtonGroup newValue ) { buttonGroup = newValue ; }
	
	private Collection actionListeners = null ;
	protected Collection getActionListeners() { return actionListeners ; }
	protected void setActionListeners( Collection newValue ) { actionListeners = newValue ; }

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
	/**
	 * Initialize the new instance with a default layout direction (BoxLayout.X_AXIS).
	 */
	public RadioButtonGroupBox()
	{
		this( BoxLayout.X_AXIS ) ;
	} // RadioButtonGroupBox()
 	
	// -------------------------------------------------------------------------
          
	/**
	 * Initialize the new instance with a layout direction.
	 * @param axis The layout of the radio buttons (either BoxLayout.X_AXIS or BoxLayout.Y_AXIS)
	 */
	public RadioButtonGroupBox(int axis)
	{
		super();
		this.setLayout( new BoxLayout( this, axis) ) ;
		this.setActionListeners( new HashSet() ) ;
		this.setButtonGroup( new RadioButtonGroup() ) ;
	} // RadioButtonGroupBox()
 	
	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with the given layout.
	 * @param layout The layout for this new component
	 */
	public RadioButtonGroupBox( LayoutManager layout )
	{
		super();
		this.setLayout( layout ) ;
		this.setActionListeners( new HashSet() ) ;
		this.setButtonGroup( new RadioButtonGroup() ) ;
	} // RadioButtonGroupBox()
 	
	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with a layout direction and some radio buttons.
	 * The given buttonData must contain a key/value pair for each radio button
	 * that should automatically be created in this group box.
	 * <br>
	 * The key contains the text label of the button and the value is the
	 * selection value of the button.
	 * 
	 * @param axis The layout of the radio buttons (either BoxLayout.X_AXIS or BoxLayout.Y_AXIS)
	 * @param buttonData The definition of the radio buttons
	 */
/* !!!!!!! NOT YET IMPLEMENTED TO AVOID DEPENDENCY TO PF-Utilities	
	public RadioButtonGroupBox(int axis, NamedValueList buttonData )
	{
		this( axis );
		this.initFromKeyValuePairs( buttonData ) ;
	} // RadioButtonGroupBox()
_____________________________________________________________________________*/ 	
	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with a layout on X_AXIS and some radio buttons.
	 * The given buttonData must contain a key/value pair for each radio button
	 * that should automatically be created in this group box.
	 * <br>
	 * The key contains the text label of the button and the value is the
	 * selection value of the button.
	 * 
	 * @param buttonData The definition of the radio buttons
	 */
	/* !!!!!!! NOT YET IMPLEMENTED TO AVOID DEPENDENCY TO PF-Utilities	
	public RadioButtonGroupBox(NamedValueList buttonData )
	{
		this();
		this.initFromKeyValuePairs( buttonData ) ;
	} // RadioButtonGroupBox()
_____________________________________________________________________________*/ 	
	// -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the button group associated with this button box
   */
	public RadioButtonGroup getButtonGroup() 
	{ 
		return buttonGroup ; 
	} // getButtonGroup()
      
	// -------------------------------------------------------------------------
  
	/**
	 * Adds the given radio button to the box
	 * 
	 * @param radioButton The radio button to add
	 * @param index The position where to add the button
	 * @see java.awt.Container#add(java.awt.Component, int)
	 */
	public Component add(RadioButton radioButton, int index)
	{
		this.registerButton( radioButton ) ;
		return super.add(radioButton, index);
	} // add()
          
	// -------------------------------------------------------------------------
	
	/**
	 * Adds the given radio button to the box
	 * 
	 * @param radioButton The radio button to add
	 * @param index The position where to add the button
	 * @see java.awt.Container#add(java.awt.Component, java.lang.Object, int)
	 */
	public void add(RadioButton radioButton, Object constraints, int index)
	{
		this.registerButton( radioButton ) ;
		super.add(radioButton, constraints, index);
	} // add()
          
	// -------------------------------------------------------------------------
	
	/**
	 * Adds the given radio button to the box
	 * 
	 * @param radioButton The radio button to add
	 * @see java.awt.Container#add(java.awt.Component, java.lang.Object)
	 */
	public void add(RadioButton radioButton, Object constraints)
	{
		this.registerButton( radioButton ) ;
		super.add(radioButton, constraints);
	} // add()
          
	// -------------------------------------------------------------------------
	
	/**
	 * Adds the given radio button to the box
	 * 
	 * @param radioButton The radio button to add
	 * @see java.awt.Container#add(java.awt.Component)
	 */
	public Component add(RadioButton radioButton)
	{
		this.registerButton( radioButton ) ;
		return super.add(radioButton);
	} // add()
          
	// -------------------------------------------------------------------------
	
	/**
	 * Adds the given radio button to the box
	 * 
	 * @param radioButton The radio button to add
	 * @see java.awt.Container#add(java.lang.String, java.awt.Component)
	 */
	public Component add(String name, RadioButton radioButton)
	{
		this.registerButton( radioButton ) ;
		return super.add(name, radioButton);
	} // add()
          
	// -------------------------------------------------------------------------

	/**
	 * Add the given listener to the collection of change listeners
	 */
	public void addActionListener(ActionListener listener)
	{
		if ( listener != null )
			this.getActionListeners().add( listener ) ;
	} // addActionListener()
         
	// -------------------------------------------------------------------------

	/**
	 * Remove the specified radio button from this box
	 * @see java.awt.Container#remove(java.awt.Component)
	 */
	public void remove( RadioButton button)
	{
		this.deregisterButton( button ) ;
		super.remove(button);
	} // remove()
      
	// -------------------------------------------------------------------------

	/**
	 * Removes the given listener from the collection of change listeners
	 */
	public void removeActionListener(ActionListener listener)
	{
		if ( listener != null )
			this.getActionListeners().remove( listener ) ;
	} // removeActionListener()
        
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the value associated with the button currently selected.
	 * If none is selected, null will be returned.
	 */
	public Object getSelectedValue()
	{
		if ( this.getButtonGroup() == null )
			return null ;
		else
			return this.getButtonGroup().getSelectedValue() ;
	} // getSelectedValue()
  
	// -------------------------------------------------------------------------
	
	/**
	 * Just public due to interface implementation
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent event)
	{
		RadioButton button ;
		
		button = (RadioButton)event.getSource() ;
		if ( button.isSelected() )
		{
			this.notifyActionListeners() ;
		}
	} // actionPerformed()
   
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	/* !!!!!!! NOT YET IMPLEMENTED TO AVOID DEPENDENCY TO PF-Utilities	
  protected void initFromKeyValuePairs( NamedValueList buttonData )
	{
		RadioButton radioButton ;
				
		for (int i = 0; i < buttonData.size(); i++)
		{
			radioButton = new RadioButton( buttonData.nameAt(i), buttonData.valueAt(i) ) ;
			this.add( radioButton ) ;
		}
	} // initFromKeyValuePairs()
	_____________________________________________________________________________*/ 	
 
	// -------------------------------------------------------------------------
	
	protected void notifyActionListeners()
	{
		Iterator iter ;
		ActionListener listener ;
		ActionEvent event ;
		
		event = new ActionEvent( this.getButtonGroup(), NEW_BUTTON_SET_ID, NEW_BUTTON_SET_CMD ) ;
		iter = this.getActionListeners().iterator() ;
		while ( iter.hasNext())
		{
			listener = (ActionListener)iter.next();
			listener.actionPerformed( event ) ;
		}
	} // notifyActionListeners()
        
	// -------------------------------------------------------------------------

	protected void registerButton( RadioButton button )
	{
		this.getButtonGroup().add( button ) ;
		button.addActionListener(this) ;
	} // registerButton()
       
	// -------------------------------------------------------------------------
	
	protected void deregisterButton( RadioButton button )
	{
		button.removeActionListener(this) ;
		this.getButtonGroup().remove( button ) ;
	} // deregisterButton()
       
	// -------------------------------------------------------------------------

} // class RadioButtonGroupBox
