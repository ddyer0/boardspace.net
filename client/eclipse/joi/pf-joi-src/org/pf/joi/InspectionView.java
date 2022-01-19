// ===========================================================================
// CONTENT  : CLASS InspectionView
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 13/03/2004
// HISTORY  :
//  13/03/2004  mdu  CREATED
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import javax.swing.JPanel;

import org.pf.swing.TakeAllLayoutManager;

/**
 * Represents the view on one object that gets analyzed (inspected).
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class InspectionView extends JPanel
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private transient BasicInspector controller = null ;
	protected BasicInspector getController() { return controller ; }
	protected void setController( BasicInspector newValue ) { controller = newValue ; }
	
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public InspectionView( BasicInspector inspector )
  {
    super( new TakeAllLayoutManager() ) ;
    this.setController( inspector ) ;
  } // InspectionView()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	public String getDisplayName()
	{
		return this.getController().getObjectDisplayName() ;	
	} // getDisplayName()

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class InspectionView
