// ===========================================================================
// CONTENT  : CLASS ObjectValuePanel
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 09/04/2004
// HISTORY  :
//  09/04/2004  mdu  CREATED
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.pf.swing.DynamicPanel;
import org.pf.swing.DynamicPanelSlot;
import org.pf.swing.TakeAllLayoutManager;

/**
 * A panel that reuses the panels and text areas for various display elements.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class ObjectValuePanel extends DynamicPanel
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final String BUTTON_LABEL_SAVE = "Apply changes";
	protected static final String BUTTON_LABEL_RESET = "Reset";
	protected static final String BUTTON_LABEL_NULL = "Set to null";
	protected static final String SLOT_OBJECT_VALUE			= "slot.object.value" ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private JTextArea readOnlyTextArea = null ;
  protected JTextArea getReadOnlyTextArea() { return readOnlyTextArea ; }
  protected void setReadOnlyTextArea( JTextArea newValue ) { readOnlyTextArea = newValue ; }
  
  private JTextArea editableTextArea = null ;
  protected JTextArea getEditableTextArea() { return editableTextArea ; }
  protected void setEditableTextArea( JTextArea newValue ) { editableTextArea = newValue ; }  
  
  private DynamicPanelSlot readOnlyTextPanel = null ;
  protected DynamicPanelSlot getReadOnlyTextPanel() { return readOnlyTextPanel ; }
  protected void setReadOnlyTextPanel( DynamicPanelSlot newValue ) { readOnlyTextPanel = newValue ; }  
  
  private DynamicPanelSlot editableTextPanel = null ;
  protected DynamicPanelSlot getEditableTextPanel() { return editableTextPanel ; }
  protected void setEditableTextPanel( DynamicPanelSlot newValue ) { editableTextPanel = newValue ; }  
  
  private DynamicPanelSlot componentPanel = null ;
  protected DynamicPanelSlot getComponentPanel() { return componentPanel ; }
  protected void setComponentPanel( DynamicPanelSlot newValue ) { componentPanel = newValue ; }
  
  private JButton nullButton = null ;
  protected JButton getNullButton() { return nullButton ; }
  protected void setNullButton( JButton newValue ) { nullButton = newValue ; }  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public ObjectValuePanel( BasicInspector inspector )
  {
    super( new TakeAllLayoutManager() ) ;
    this.init( inspector ) ;
  } // ObjectValuePanel() 

  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Sets the given text and shows the view with the corresponding text 
   * component. 
   */
  public void activateText( String text, boolean editable, boolean isPrimitive ) 
	{
  	if ( editable )
		{
  		this.getNullButton().setEnabled( ! isPrimitive ) ;
			this.setEditableText( text ) ;
			this.activateEditableTextView() ;
		}
		else
		{
			this.getReadOnlyTextArea().setText( text ) ;
			this.activateReadOnlyTextView() ;
		}
	} // activateText() 

	// -------------------------------------------------------------------------
  
  /**
   * Sets the editable text field to the given text value
   */
  public void setEditableText( String text ) 
	{
  	this.getEditableTextArea().setText( text ) ;
	} // setEditableText()

	// -------------------------------------------------------------------------
  
  /**
   * Sets the given text and shows the view with the corresponding text 
   * component. 
   */
  public void activateComponent( Component comp ) 
	{
		this.getComponentPanel().removeAll() ;
		this.getComponentPanel().add( comp ) ;
		this.activateComponentView() ;
	} // activateComponent() 

	// -------------------------------------------------------------------------

  /**
   * Returns the text from the editable text area
   */
  public String getEditedText() 
	{
		return this.getEditableTextArea().getText() ;
	} // getEditedText() 

	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected void init( BasicInspector inspector ) 
	{
  	JScrollPane scrollPane ;
  	
		this.addDynamicSlot( SLOT_OBJECT_VALUE ) ;

		this.setReadOnlyTextPanel( new DynamicPanelSlot() ) ;
		this.setReadOnlyTextArea( this.createTextArea(false) ) ;
		scrollPane = this.createScrollPane( this.getReadOnlyTextArea() ) ;
		this.getReadOnlyTextPanel().add( scrollPane ) ;

		this.setEditableTextPanel( new DynamicPanelSlot() ) ;
		this.setEditableTextArea( this.createTextArea(true) ) ;
		this.getEditableTextPanel().setLayout( new BorderLayout() ) ;
		this.getEditableTextPanel().add( this.createButtons( inspector ), BorderLayout.NORTH ) ;		
		scrollPane = this.createScrollPane( this.getEditableTextArea() ) ;
		this.getEditableTextPanel().add( scrollPane, BorderLayout.CENTER ) ;
		
		this.setComponentPanel( new DynamicPanelSlot() ) ;
		
		this.activateReadOnlyTextView() ;
	} // init() 

	// -------------------------------------------------------------------------
  
  protected JPanel createButtons( BasicInspector inspector ) 
	{
  	JPanel panel ;
  	JButton button ;
  	
  	panel = new JPanel( new FlowLayout() ) ;

  	button = new JButton( BUTTON_LABEL_SAVE ) ;
		button.setActionCommand( inspector.actSaveModifiedValue ) ;
		button.addActionListener( inspector ) ;
		panel.add( button ) ;

  	button = new JButton( BUTTON_LABEL_RESET ) ;
		button.setActionCommand( inspector.actResetModifiedValue ) ;
		button.addActionListener( inspector ) ;
		panel.add( button ) ;

  	button = new JButton( BUTTON_LABEL_NULL ) ;
		button.setActionCommand( inspector.actSetValueToNull ) ;
		button.addActionListener( inspector ) ;
		this.setNullButton( button ) ;
		panel.add( button ) ;
		
		return panel ;
	} // createButtons() 

	// -------------------------------------------------------------------------
 
  protected JTextArea createTextArea( boolean editable ) 
	{
		JTextArea textArea			= null ;

		textArea = new JTextArea() ;
		textArea.setEditable( editable ) ;
		textArea.setLineWrap( true ) ;
		textArea.setWrapStyleWord( false ) ;
		return textArea ;
	} // createTextArea() 

	// -------------------------------------------------------------------------
	  
  protected JScrollPane createScrollPane( Component comp ) 
	{
		JScrollPane pane ;
		
		pane = new JScrollPane( comp ) ;
		return pane ;
	} // createScrollPane()
	
	// -------------------------------------------------------------------------
  
	protected void activateReadOnlyTextView() 
	{
		this.setSlot( SLOT_OBJECT_VALUE, this.getReadOnlyTextPanel() ) ;
	} // activateReadOnlyTextView() 

	// -------------------------------------------------------------------------
	
	protected void activateEditableTextView() 
	{
		this.setSlot( SLOT_OBJECT_VALUE, this.getEditableTextPanel() ) ;
	} // activateEditableTextView() 

	// -------------------------------------------------------------------------

	protected void activateComponentView() 
	{
		this.setSlot( SLOT_OBJECT_VALUE, this.getComponentPanel() ) ;
	} // activateComponentView() 

	// -------------------------------------------------------------------------

} // class ObjectValuePanel 
