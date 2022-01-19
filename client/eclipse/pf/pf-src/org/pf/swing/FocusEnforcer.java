// ===========================================================================
// CONTENT  : CLASS FocusEnforcer
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 26/01/2008
// HISTORY  :
//  26/01/2008  mdu  CREATED
//
// Copyright (c) 2008, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.swing ;

//===========================================================================
//IMPORTS
//===========================================================================
import javax.swing.JComponent;

/**
 * This subclass of Thread can be used to set the focus of a window 
 * to a specific component. The thread is running until the component
 * has the focus.
 * This is particularly useful with modal dialogs (i.e. JDialog) where 
 * it is not possible in another way to set the focus to a component other than 
 * the first in the dialog. 
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class FocusEnforcer extends Thread
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	private static final long TIMEOUT = 5000 ; // milliseconds

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private JComponent component ; 

  // =========================================================================
  // PUBLIC CLASS METHODS
  // =========================================================================
	/**
	 * Starts a thread to put the focus on the given component.
	 * The thread runs until the given component has gained the focus.
	 * 
	 * @param focusComponent The component that should get the focus.
	 */
	public static void enforceFocusOn( JComponent focusComponent ) 
	{
		FocusEnforcer focusEnforcer ;
		
		focusEnforcer = new FocusEnforcer( focusComponent ) ;
		focusEnforcer.start() ;
	} // enforceFocusOn() 
	
	// -------------------------------------------------------------------------
	
	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
  /**
   * Initialize the new instance the component to put the focus on.
   * 
   * @param focusComponent The component that should get the focus.
   */
  public FocusEnforcer( JComponent focusComponent )
  {
    super() ;
    this.component = focusComponent ;
  } // FocusEnforcer() 
  
  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance the component to put the focus on.
   * 
   * @param focusComponent The component that should get the focus.
   * @param name The name of the thread
   */
  public FocusEnforcer( JComponent focusComponent, String name )
  {
  	super( name ) ;
  	this.component = focusComponent ;
  } // FocusEnforcer() 
  
  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Actually execute the thread's purpose and set the focus on the desired 
   * component. If it isn't possible for any reason then this method gives up
   * after 5 seconds.
   */
	public void run()
	{
		long start ;
		
		if ( component != null )
		{
			start = System.currentTimeMillis() ;
			while ( !component.hasFocus() && ( (System.currentTimeMillis() - start) < TIMEOUT  ) )
			{
				component.requestFocus() ;  				
			}
		}
	} // run() 

	// -------------------------------------------------------------------------
	
} // class FocusEnforcer 
