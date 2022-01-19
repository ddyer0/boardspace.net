// ===========================================================================
// CONTENT  : CLASS ElementFilterMenu
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 14/03/2004
// HISTORY  :
//  14/03/2004  mdu  CREATED
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;

/**
 * Holds an Element filter and all menu items and takes care that they are 
 * in sync.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class ElementFilterMenu
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private ElementFilter elementFilter = 
					new ElementFilter(	ElementFilter.STATIC + ElementFilter.FINAL ) ;
	protected ElementFilter getElementFilter() { return elementFilter ; }
	protected void setElementFilter( ElementFilter aValue ) { elementFilter = aValue ; }
	
	private JCheckBoxMenuItem transientMenuItem = null ;
	protected JCheckBoxMenuItem getTransientMenuItem() { return transientMenuItem ; }
	protected void setTransientMenuItem( JCheckBoxMenuItem newValue ) { transientMenuItem = newValue ; }
		
	private JCheckBoxMenuItem staticMenuItem = null ;
	protected JCheckBoxMenuItem getStaticMenuItem() { return staticMenuItem ; }
	protected void setStaticMenuItem( JCheckBoxMenuItem newValue ) { staticMenuItem = newValue ; }
	
	private JCheckBoxMenuItem finalMenuItem = null ;
	protected JCheckBoxMenuItem getFinalMenuItem() { return finalMenuItem ; }
	protected void setFinalMenuItem( JCheckBoxMenuItem newValue ) { finalMenuItem = newValue ; }
	
	private JCheckBoxMenuItem privateMenuItem = null ;
	protected JCheckBoxMenuItem getPrivateMenuItem() { return privateMenuItem ; }
	protected void setPrivateMenuItem( JCheckBoxMenuItem newValue ) { privateMenuItem = newValue ; }
	
	private JCheckBoxMenuItem protectedMenuItem = null ;
	protected JCheckBoxMenuItem getProtectedMenuItem() { return protectedMenuItem ; }
	protected void setProtectedMenuItem( JCheckBoxMenuItem newValue ) { protectedMenuItem = newValue ; }
	
	private JCheckBoxMenuItem packageMenuItem = null ;
	protected JCheckBoxMenuItem getPackageMenuItem() { return packageMenuItem ; }
	protected void setPackageMenuItem( JCheckBoxMenuItem newValue ) { packageMenuItem = newValue ; }
	
	private JCheckBoxMenuItem publicMenuItem = null ;
	protected JCheckBoxMenuItem getPublicMenuItem() { return publicMenuItem ; }
	protected void setPublicMenuItem( JCheckBoxMenuItem newValue ) { publicMenuItem = newValue ; }						
	
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public ElementFilterMenu()
  {
    super() ;
    this.init() ;
  } // ElementFilterMenu() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	public void replaceFilter( ElementFilter filter )
	{
		this.setElementFilter( filter ) ;
		this.updateItems() ;
	} // replaceFilter()

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected void init()
	{
		JCheckBoxMenuItem menuItem ;

		menuItem = this.createCheckMenuItem(	"Static", 
												InspectionWindowController.actToggleStatic, null ) ;
		this.setStaticMenuItem( menuItem ) ;

		menuItem = this.createCheckMenuItem(	"Final", 
												InspectionWindowController.actToggleFinal, null ) ;
		this.setFinalMenuItem( menuItem ) ;

		menuItem = this.createCheckMenuItem(	"Transient", 
												InspectionWindowController.actToggleTransient, null ) ;
		this.setTransientMenuItem( menuItem ) ;

		menuItem = this.createCheckMenuItem(	"Private", 
												InspectionWindowController.actTogglePrivate, 
												ImageProvider.instance().getPrivateIcon() ) ;
		this.setPrivateMenuItem( menuItem ) ;

		menuItem = this.createCheckMenuItem(	"Protected", 
												InspectionWindowController.actToggleProtected, 
												ImageProvider.instance().getProtectedIcon() ) ;
		this.setProtectedMenuItem( menuItem ) ;

		menuItem = this.createCheckMenuItem(	"Public", 
												InspectionWindowController.actTogglePublic, 
												ImageProvider.instance().getPublicIcon() ) ;
		this.setPublicMenuItem( menuItem ) ;

		menuItem = this.createCheckMenuItem(	"Default (Package)", 
												InspectionWindowController.actTogglePackage, 
												ImageProvider.instance().getPackageIcon() ) ;
		this.setPackageMenuItem( menuItem ) ;
		
		this.updateItems() ;
	} // init() 

	// -------------------------------------------------------------------------

	protected void updateItems()
	{
		this.getStaticMenuItem().setState( ! getElementFilter().isStaticSet() ) ;
		this.getStaticMenuItem().repaint() ;
		this.getFinalMenuItem().setState( ! getElementFilter().isFinalSet() ) ;
		this.getFinalMenuItem().repaint() ;
		this.getTransientMenuItem().setState( ! getElementFilter().isTransientSet() ) ;
		this.getPrivateMenuItem().setState( ! getElementFilter().isPrivateSet() ) ;
		this.getPrivateMenuItem().repaint() ;
		this.getProtectedMenuItem().setState( ! getElementFilter().isProtectedSet() ) ;
		this.getPublicMenuItem().setState( ! getElementFilter().isPublicSet() ) ;
		this.getPackageMenuItem().setState( ! getElementFilter().isDefaultSet() ) ;
		this.getPackageMenuItem().repaint() ;
	} // updateItems()

	// -------------------------------------------------------------------------  

	protected JCheckBoxMenuItem createCheckMenuItem( String text, String command, Icon icon )
	{
		JCheckBoxMenuItem menuItem	= null ;

		if ( icon == null )
		{
			menuItem = new JCheckBoxMenuItem( text ) ;			
		}
		else
		{
			menuItem = new JCheckBoxMenuItem( text, icon ) ;
		}
		menuItem.setActionCommand( command ) ;

		return menuItem ;
	} // createCheckMenuItem() 

	// -------------------------------------------------------------------------

} // class ElementFilterMenu 
