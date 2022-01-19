// ===========================================================================
// CONTENT  : ABSTRACT CLASS KeyValueInspector
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
import java.awt.event.ActionEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.pf.joi.BasicInspector;
import org.pf.joi.InspectionWindowController;
import org.pf.joi.Inspector;
import org.pf.joi.Spy;

/**
 * Abstract superclass for all inspectors that present key/value pairs.
 *
 * @author M.Duchrow
 * @version 1.0
 */
abstract public class KeyValueInspector extends BasicInspector
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final String actKeyInspectWindow		= "actionInspectKeyInWindow" ;
	protected static final String actKeyInspectTab			= "actionInspectKeyInTab" ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public KeyValueInspector()
  {
    super() ;
  } // KeyValueInspector() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Is called whenever a user action event occurred.    <br>
	 * This method is actually performing all actions, triggered by buttons,
	 * keystrokes or menu items.
	 *
	 * @param e The action event holding further information on what happened.
	 */
	public void actionPerformed(ActionEvent e)
	{
		if ( e.getActionCommand().equals( actKeyInspectWindow ) )
		{
			this.inspectCurrentKey( InspectionWindowController.INSPECT_IN_NEW_WINDOW ) ;
		}
		else if ( e.getActionCommand().equals( actKeyInspectTab ) )
		{
			this.inspectCurrentKey( InspectionWindowController.INSPECT_IN_NEW_TAB ) ;
		}
		else
		{
			super.actionPerformed( e ) ;
		}
	} // actionPerformed() 

  // -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected void inspectCurrentKey( int where )
	{
		Spy spy ;
		AssociationSpy associationSpy ;
		
		spy = this.getCurrentElement() ; 
		if ( spy != null )
		{
			try
			{
				if ( spy instanceof AssociationSpy )
				{ 
					associationSpy = (AssociationSpy)spy ;
					if ( where == InspectionWindowController.INSPECT_IN_NEW_WINDOW )
					{
						Inspector.inspect( associationSpy.getKey() ) ;
					}
					else
					{
						Inspector.inspectIn( this.getController(), where, 
																					null, associationSpy.getKey() ) ;						
					}
				}
				else
				{
					if ( where == InspectionWindowController.INSPECT_IN_NEW_WINDOW )
					{
						Inspector.inspect( spy.getValue() ) ;
					}
					else
					{
						Inspector.inspectIn( this.getController(), where, null, spy.getValue() ) ;						
					}
				}
			}
			catch ( Exception ex )
			{
				ex.printStackTrace( errorDevice ) ;
			}
		}
	} // inspectCurrentKey() 

	// -------------------------------------------------------------------------
 
 	protected JPopupMenu createElementPopupMenu()
	{
		JMenuItem menuItem	= null ;
		JPopupMenu popup		= null ;

		popup = super.createElementPopupMenu() ;
		popup.addSeparator() ;
		menuItem = this.createMenuItem(	"Inspect key in new tab", actKeyInspectTab ) ;
		popup.add( menuItem ) ;
		menuItem = this.createMenuItem(	"Inspect key in new window", actKeyInspectWindow ) ;
		popup.add( menuItem ) ;

		return popup ;
	} // createElementPopupMenu() 
  
  // -------------------------------------------------------------------------
	  
} // class KeyValueInspector 
