// ===========================================================================
// CONTENT  : CLASS DynamicPanelSlot
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 29/08/2003
// HISTORY  :
//  29/08/2003  mdu  CREATED
//
// Copyright (c) 2003, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.swing;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.awt.Component;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

/**
 * This is a panel that containes one other panel which can be replaced at any 
 * time. It is not intended to contain more than one panel.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class DynamicPanelSlot extends JPanel
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
   * Initialize the new instance with default values.
   */
  public DynamicPanelSlot()
  {
    super() ;
    this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) ) ;
  } // DynamicPanelSlot()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Sets the contained panel. That implies that the formerly contained panel
   * is removed.
   * 
   * @param panel The ne panel to be contained
   */
	public void setPanel( JPanel panel )
	{
		this.removeAll() ;
		this.setVisible(false) ;
		if ( panel != null )
		{
			this.add( panel ) ;
		}
		this.setVisible(true) ;
	} // setPanel()

	// -------------------------------------------------------------------------
	
	/**
	 * Returns the contained panel or null if none is set
	 */
	public JPanel getPanel()
	{
		Component[] components ;
		
		components = this.getComponents() ;
		if ( ( components == null ) || ( components.length == 0 ) )
			return null ;
			
		return (JPanel)components[0] ;
	} // getPanel()

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class DynamicPanelSlot
