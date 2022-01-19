// ===========================================================================
// CONTENT  : CLASS DynamicPanel
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 23/09/2003
// HISTORY  :
//  23/09/2003  mdu  CREATED
//
// Copyright (c) 2003, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.swing ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.awt.LayoutManager;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

/**
 * This panel can contain one or more DynamicPanelSlots, which can be used to 
 * dynamically replace subcomponents with minimum effort.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class DynamicPanel extends JPanel
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private Map dynamicSlots = null ;
	protected Map dynamicSlots() { return dynamicSlots ; }
	protected void dynamicSlots( Map newValue ) { dynamicSlots = newValue ; }
	
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public DynamicPanel()
  {
    super() ;
  } // DynamicPanel()
 	
	// -------------------------------------------------------------------------
	
	/**
	 * Initialize the new instance with a layout manager and a double buffered flag
	 */
	public DynamicPanel(LayoutManager layout, boolean isDoubleBuffered)
	{
		super(layout, isDoubleBuffered);
	} // DynamicPanel()

	// -------------------------------------------------------------------------
	
	/**
	 * Initialize the new instance with a layout manager
	 */
	public DynamicPanel(LayoutManager layout)
	{
		super(layout);
	} // DynamicPanel()

	// -------------------------------------------------------------------------
	
	/**
	 * Initialize the new instance with a flag to have it double buffered or not
	 */
	public DynamicPanel(boolean isDoubleBuffered)
	{
		super(isDoubleBuffered);
	} // DynamicPanel()

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Adds a new dynamic slot with the given name to the panel.
   * 
   * @param name Unique name of the slot to be able to find it again 
   */
	public DynamicPanelSlot addDynamicSlot( String name )
	{
		return this.addDynamicSlot( name, null ) ;
	} // addDynamicSlot()
 
	// -------------------------------------------------------------------------

	/**
	 * Adds a new dynamic slot with the given name to the panel.
	 * 
	 * @param name Unique name of the slot to be able to find it again 
	 * @param constraints Some layout constraints 
	 */
	public DynamicPanelSlot addDynamicSlot( String name, Object constraints )
	{
		DynamicPanelSlot slot ;
		
		slot = new DynamicPanelSlot() ;
		if ( constraints == null )
		{
			slot.setAlignmentX( this.getAlignmentX() ) ;
			slot.setAlignmentY( this.getAlignmentY() ) ;
			this.add( slot ) ;
		}
		else
		{
			this.add( slot, constraints ) ;
		}	
		this.registerSlot( name, slot ) ;
		return slot ;
	} // addDynamicSlot()
 
	// -------------------------------------------------------------------------

	/**
	 * Returns the dynamic slot with the specified name or null if no dynamic slot
	 * is found with this name.
	 * 
   * @param name Unique name of the slot to be looked up 
	 */
	public DynamicPanelSlot getDynamicSlotNamed( String name )
	{
		return (DynamicPanelSlot)this.getDynamicSlots().get( name ) ;
	} // getDynamicSlotNamed()
 
	// -------------------------------------------------------------------------

	/**
	 * Puts the given panel into the slot with the specified name.
	 * Returns true if the slot was found and the panel put in, otherwise false.
	 * 
	 * @param name The name of the slot
	 * @param panel The panel to be placed into the slot
	 */
	public boolean setSlot( String name, JPanel panel )
	{
		DynamicPanelSlot slot ;
		
		if ( ( name == null ) || ( panel == null ) )
			return false ;
		
		slot = this.getDynamicSlotNamed( name ) ;
		if ( slot != null )
		{
			slot.setPanel( panel ) ;
			return true ;
		}
		return false ;
	} // setSlot()

	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

	protected Map getDynamicSlots()
	{
		if ( this.dynamicSlots() == null )
		{
			this.dynamicSlots( new HashMap() ) ;
		}
		return this.dynamicSlots() ;
	} // getDynamicSlots()
 
	// -------------------------------------------------------------------------

	protected void registerSlot( String name, DynamicPanelSlot slot )
	{
		this.getDynamicSlots().put( name, slot ) ;
	} // registerSlot()
 
	// -------------------------------------------------------------------------

} // class DynamicPanel
