// ===========================================================================
// CONTENT  : CLASS InspectionWindowController
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 15/04/2006
// HISTORY  :
//  14/03/2004  mdu  CREATED
//	15/04/2006	mdu		changed	-->	showing system properties
//
// Copyright (c) 2004-2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.URL;
import java.util.Map;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.pf.file.FileFinder;
import org.pf.file.FileUtil;

/**
 * Controls one inspection window with one ore more inspectors inside.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class InspectionWindowController implements ActionListener,WindowListener
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	/**
	 * The action code that tells an inspector to open the inspection view
	 * in the current window or tab.
	 */
	public static final int INSPECT_IN_CURRENT_PLACE		= 1 ;
	/**
	 * The action code that tells an inspector to open the inspection view
	 * in a new tab.
	 */
	public static final int INSPECT_IN_NEW_TAB					= 2 ;
	/**
	 * The action code that tells an inspector to open the inspection view
	 * in a new window.
	 */
	public static final int INSPECT_IN_NEW_WINDOW			= 3 ;

	protected static final String TXT_ACTIONS = "Actions";
	protected static final String TXT_CONTINUE_THREAD = "Continue Thread";
	protected static final String TXT_SORT = "Sort";
	protected static final String TXT_BASIC_INSPECT_IN_NEW_WINDOW = "Basic Inspect in new window";
	protected static final String TXT_BASIC_INSPECT_IN_NEW_TAB = "Basic Inspect in new tab";
	protected static final String TXT_BASIC_INSPECT_HERE = "Basic Inspect in this tab/window";
	protected static final String TXT_INSPECT_IN_NEW_WINDOW = "Inspect in new window";
	protected static final String TXT_INSPECT_IN_NEW_TAB = "Inspect in new tab";
	protected static final String TXT_INSPECT_HERE = "Inspect in this tab/window";
	
	protected static final String actCloseWindow				= "actionCloseWindow" ;
	protected static final String actCloseAll						= "actionCloseAll" ;
	protected static final String actCloseTab						= "actionCloseTab" ;
	protected static final String actInspectWindow			= "actionInspectInWindow" ;
	protected static final String actInspectTab					= "actionInspectInTab" ;
	protected static final String actInspectHere				= "actionInspectHere" ;
	protected static final String actBasicInspectWindow	= "actionBasicInspectInWindow" ;
	protected static final String actBasicInspectTab		= "actionBasicInspectInTab" ;
	protected static final String actBasicInspectHere		= "actionBasicInspectHere" ;
	protected static final String actSort								= "actionSort" ;
	protected static final String actContinue						= "actionContinue" ;
	protected static final String actAbout							= "actionAbout" ;
	protected static final String actToggleStatic				= "ToggleStatic" ;
	protected static final String actToggleFinal				= "ToggleFinal" ;
	protected static final String actToggleTransient		= "ToggleTransient" ;
	protected static final String actTogglePackage			= "TogglePackage" ;
	protected static final String actTogglePrivate			= "TogglePrivate" ;
	protected static final String actToggleProtected		= "ToggleProtected" ;
	protected static final String actTogglePublic				= "TogglePublic" ;
	protected static final String actShowInspectorMapping	= "ShowInspectorMapping" ;
	protected static final String actShowRendererMapping	= "ShowRendererMapping" ;
	protected static final String actShowExporterMapping	= "ShowExporterMapping" ;
	protected static final String actShowSystemProperties	= "ShowSystemProperties" ;
	protected static final String actShowLicenseText		= "ShowLicenseText" ;

	protected static final int PLUGIN_INSPECTOR					= 1 ;
	protected static final int PLUGIN_RENDERER					= 2 ;
	protected static final int PLUGIN_EXPORTER					= 3 ;
	
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private InspectionFrame mainFrame = null ;
	protected InspectionFrame getMainFrame() { return mainFrame ; }
	protected void setMainFrame( InspectionFrame newValue ) { mainFrame = newValue ; }

	private BasicInspector currentInspector = null ;
	protected BasicInspector getCurrentInspector() { return currentInspector ; }
	protected void setCurrentInspector( BasicInspector newValue ) { currentInspector = newValue ; }
	
	private ElementFilterMenu filterMenu = null ;
	protected ElementFilterMenu getFilterMenu() { return filterMenu ; }
	protected void setFilterMenu( ElementFilterMenu newValue ) { filterMenu = newValue ; }
	
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance.
   */
  public InspectionWindowController()
  {
    super() ;
    this.setFilterMenu( new ElementFilterMenu() ) ;
  } // InspectionWindowController() 

	// -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Is called whenever a user action event occured.    <br>
	 * This method is actually performing all actions, triggered by buttons,
	 * keystrokes or menu items.
	 *
	 * @param e The action event holding further information on what happened.
	 */
	public void actionPerformed(ActionEvent e)
	{
		if ( e.getActionCommand().equals( actCloseWindow ) )
			this.terminate() ;
		else if ( e.getActionCommand().equals( actBasicInspectWindow ) )
			this.currentInspector().basicInspectCurrentElement( INSPECT_IN_NEW_WINDOW ) ;
		else if ( e.getActionCommand().equals( actBasicInspectTab ) )
			this.currentInspector().basicInspectCurrentElement( INSPECT_IN_NEW_TAB ) ;
		else if ( e.getActionCommand().equals( actBasicInspectHere ) )
			this.currentInspector().basicInspectCurrentElement( INSPECT_IN_CURRENT_PLACE ) ;
		else if ( e.getActionCommand().equals( actInspectWindow ) )
			this.currentInspector().inspectCurrentElement( INSPECT_IN_NEW_WINDOW ) ;
		else if ( e.getActionCommand().equals( actInspectTab ) )
			this.currentInspector().inspectCurrentElement( INSPECT_IN_NEW_TAB ) ;
		else if ( e.getActionCommand().equals( actInspectHere ) )
			this.currentInspector().inspectCurrentElement( INSPECT_IN_CURRENT_PLACE ) ;
		else if ( e.getActionCommand().equals( actCloseAll ) )
			this.closeAllControllers() ;
		else if ( e.getActionCommand().equals( actSort ) )
			this.currentInspector().sortElements() ;
		else if ( e.getActionCommand().equals( actContinue ) )
			this.continueProcess() ;
		else if ( e.getActionCommand().equals( actToggleStatic ) )      
			this.toggleFilter( ElementFilter.STATIC ) ;
		else if ( e.getActionCommand().equals( actToggleFinal ) )      
			this.toggleFilter( ElementFilter.FINAL ) ;
		else if ( e.getActionCommand().equals( actToggleTransient ) )      
			this.toggleFilter( ElementFilter.TRANSIENT ) ;
		else if ( e.getActionCommand().equals( actTogglePrivate ) )      
			this.toggleFilter( ElementFilter.PRIVATE ) ;
		else if ( e.getActionCommand().equals( actToggleProtected ) )      
			this.toggleFilter( ElementFilter.PROTECTED ) ;
		else if ( e.getActionCommand().equals( actTogglePublic ) )      
			this.toggleFilter( ElementFilter.PUBLIC ) ;
		else if ( e.getActionCommand().equals( actTogglePackage ) )      
			this.toggleFilter( ElementFilter.DEFAULT ) ;
		else if ( e.getActionCommand().equals( actShowInspectorMapping ) )
			this.showPluginMapping( PLUGIN_INSPECTOR ) ;
		else if ( e.getActionCommand().equals( actShowRendererMapping ) )
			this.showPluginMapping( PLUGIN_RENDERER ) ;
		else if ( e.getActionCommand().equals( actShowExporterMapping ) )
			this.showPluginMapping( PLUGIN_EXPORTER ) ;
		else if ( e.getActionCommand().equals( actCloseTab ) )
			this.closeInspectionTab( e ) ;
		else if ( e.getActionCommand().equals( actShowSystemProperties ) )
			this.showSystemProperties() ;
		else if ( e.getActionCommand().equals( actShowLicenseText ) )
			this.showLicenseText() ;
		else if ( e.getActionCommand().equals( actAbout ) )
			this.displayAboutInfo() ;
		else if ( e.getActionCommand().startsWith( Inspector.ExportPrefix ) )
			this.exportObject( e.getActionCommand().substring( Inspector.ExportPrefix.length()) ) ;
	} // actionPerformed() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// PUBLIC INTERFACE IMPLEMENTATION WindowListener
	// =========================================================================
	/**
	 * Noop implementation to be compliant to WindowListener interface.
	 * @see java.awt.event.WindowListener#windowOpened(WindowEvent)
	 */
	public void windowOpened(WindowEvent e)
	{
	} // windowOpened() 

	// -------------------------------------------------------------------------

	/**
	 * Noop implementation to be compliant to WindowListener interface.
	 * @see java.awt.event.WindowListener#windowClosed(WindowEvent)
	 */
	public void windowClosed(WindowEvent e)
	{
	} // windowClosed() 

	// -------------------------------------------------------------------------

	/**
	 * Check if it's ok to close the window and if yes then do it.
	 * @see java.awt.event.WindowListener#windowClosing(WindowEvent)
	 */
	public void windowClosing(WindowEvent e)
	{
		this.terminate() ;
	} // windowClosing() 

	// -------------------------------------------------------------------------

	/**
	 * Noop implementation to be compliant to WindowListener interface.
	 * @see java.awt.event.WindowListener#windowDeactivated(WindowEvent)
	 */
	public void windowDeactivated(WindowEvent e)
	{
	} // windowDeactivated() 

	// -------------------------------------------------------------------------

	/**
	 * Noop implementation to be compliant to WindowListener interface.
	 * @see java.awt.event.WindowListener#windowDeiconified(WindowEvent)
	 */
	public void windowDeiconified(WindowEvent e)
	{
	} // windowDeiconified() 

	// -------------------------------------------------------------------------

	/**
	 * Noop implementation to be compliant to WindowListener interface.
	 * @see java.awt.event.WindowListener#windowIconified(WindowEvent)
	 */
	public void windowIconified(WindowEvent e)
	{
	} // windowIconified() 

	// -------------------------------------------------------------------------

	/**
	 * Noop implementation to be compliant to WindowListener interface.
	 * @see java.awt.event.WindowListener#windowActivated(WindowEvent)
	 */
	public void windowActivated(WindowEvent e)
	{
	} // windowActivated() 

	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the currently selected inspector
   */
  protected BasicInspector currentInspector()
	{
		return this.getCurrentInspector() ;
	} // currentInspector() 

	// -------------------------------------------------------------------------

	protected void replaceCurrentInspector( BasicInspector inspector )
	{
		this.setCurrentInspector( inspector ) ;
		this.getFilterMenu().replaceFilter( this.getCurrentInspector().elementFilter() ) ;
	} // replaceCurrentInspector() 

	// -------------------------------------------------------------------------

	/**
	 * Must be called, if another view was selected
	 */
	protected void viewSelected( InspectionView view )
	{
		this.replaceCurrentInspector( view.getController() ) ;
	} // viewSelected() 

	// -------------------------------------------------------------------------

	/**
	 * This method terminates execution of the inspector.
	 * It also removes the inspector from the inspector registry.
	 * Subclasses normally override this method.
	 * They must call super.terminate() !
	 */
	protected void terminate()
	{
		Inspector.unregisterController( this ) ;
		this.close() ;
	} // terminate() 

	// -------------------------------------------------------------------------
	
	/**
	 * Ends the wait loop to let the calling process
	 * continue its work.
	 */
	protected void continueProcess()
	{
		Inspector.deactivateHalt() ;
	} // continueProcess() 

	// -------------------------------------------------------------------------
    
	protected void start( BasicInspector inspector )
	{
		this.addInspector( inspector ) ;
		this.replaceCurrentInspector( inspector ) ;
		this.buildUI() ;
		this.getMainFrame().addInspection( inspector.getInspectionView() ) ;			
		this.openUI() ;
	} // start() 

	// -------------------------------------------------------------------------

	/**
	 * Close the controller.
	 */
	protected void close()
	{
		this.getMainFrame().setVisible( false ) ;
	} // close() 

	// -------------------------------------------------------------------------

	protected void closeAllControllers()
	{
		Inspector.closeAllControllers() ;
	} // closeAllControllers() 
	
	// -------------------------------------------------------------------------

	/**
	 * Toggle the filter flag specified by the given value
	 */
	protected void toggleFilter( int filterFlag )
	{
		this.getElementFilter().toggleSwitch(filterFlag) ;
		this.currentInspector().updateDisplay() ;	
	} // toggleFilter() 

	// -------------------------------------------------------------------------

	protected void showPluginMapping( int pluginType )
	{
		Map mapping = null ;
		String name = "" ;
		
		switch ( pluginType )
		{
			case ( PLUGIN_INSPECTOR ) :
				mapping = Inspector.inspectorBinding().getMapping() ;
				name = "Inspector mapping" ;
				break ;
			case ( PLUGIN_EXPORTER ) :
				mapping = Inspector.exportProviderRegistry().getMapping() ;
				name = "Exporter mapping" ;
				break ;
			case ( PLUGIN_RENDERER ) :
				mapping = Spy.getRendererRegistry().getMapping() ;
				name = "Renderer mapping" ;
				break ;
		}
		
		if ( mapping != null )
		{
			Inspector.inspect( name, mapping ) ;
		}
	} // showPluginMapping() 

	// -------------------------------------------------------------------------

	protected void exportObject( String exportName )
	{
		ExportProvider exportProvider   = null ;

		exportProvider = Inspector.findExporterNamed( exportName ) ;
		if ( exportProvider != null )
		{
			try
			{
				exportProvider.export( this.getInspectedObject(), this.getMainFrame() ) ;
			}
			catch ( Exception ex )
			{
				ex.printStackTrace() ;
			}
		}
	} // exportObject() 

	// -------------------------------------------------------------------------

	protected void closeInspectionTab( ActionEvent event )
	{
		InspectionView view ;
		
		view = this.getMainFrame().getSelectedView() ;
		if ( view != null )
		{
			this.removeInspector( view.getController() ) ;
			this.getMainFrame().removeInspection( view ) ;
		}
	} // closeInspectionTab() 

	// -------------------------------------------------------------------------

	/**
	 * Display information about JOI.
	 */
	protected void displayAboutInfo()
	{
		JOptionPane.showMessageDialog( 	this.getMainFrame().getContentPane(),
																						Inspector.getAboutInfoText(),
																						"About " + Inspector.PROG_ID,
																						JOptionPane.INFORMATION_MESSAGE ) ;
	} // displayAboutInfo() 

	// -------------------------------------------------------------------------

	protected void showSystemProperties() 
	{
		Inspector.inspect( "System Properties", System.getProperties() ) ;
	} // showSystemProperties() 
	
	// -------------------------------------------------------------------------
	
	protected void showLicenseText() 
	{
		String text ;
		URL url ;
		
		url = FileFinder.locateFileOnClasspath( "cpl-v1.0.txt" ) ;
		if ( url != null )
		{
			try
			{
				text = this.fileUtil().readTextFrom( url.openStream() ) ;
			}
			catch ( Exception e )
			{
				text = e.toString() ;
			}
			Inspector.inspect( "Common Public License 1.0", text ) ;
		}
	} // showLicenseText() 
	
	// -------------------------------------------------------------------------
	
	protected void addInspector( BasicInspector inspector )
	{
		// TODO Add inspector to InspectorContainer
		// this.getInspectors().add( inspector ) ;
		inspector.setController( this ) ;		
	} // addInspector() 

	// -------------------------------------------------------------------------

	protected void removeInspector( BasicInspector inspector )
	{
		// this.getInspectors().remove( inspector ) ;
		inspector.setController( null ) ;		
	} // removeInspector() 

	// -------------------------------------------------------------------------

	protected String getInspectorId()
	{
		return this.currentInspector().getInspectorId() ;
	} // getInspectorId() 

	// -------------------------------------------------------------------------

	protected AbstractObjectSpy getInspectedObject()
	{
		return this.currentInspector().getInspectedObject() ;
	} // getInspectedObject() 

	// -------------------------------------------------------------------------

	protected String getInspectedObjectTypeString()
	{
		return this.currentInspector().getInspectedObject().getTypeString() ;
	} // getInspectedObjectTypeString() 

	// -------------------------------------------------------------------------

	protected ElementFilter getElementFilter()
	{
		return this.getFilterMenu().getElementFilter() ;
	} // getElementFilter() 

	// -------------------------------------------------------------------------

	protected void openUI()
	{
		this.getMainFrame().validate() ;
		this.getMainFrame().setVisible( true ) ;
	} // openUI() 

	// -------------------------------------------------------------------------
	
	protected void openNewInspector( BasicInspector inspector, int where )
	{
		this.addInspector( inspector ) ;
		switch ( where )
		{
			case INSPECT_IN_NEW_TAB :
				this.getMainFrame().addInspection( inspector.getInspectionView() ) ;		
				break;

			case INSPECT_IN_CURRENT_PLACE :
				this.getMainFrame().replaceInspection( inspector.getInspectionView() ) ;
				break;
			
			default:
			  this.removeInspector( inspector ) ;
				return ;
		}		
		inspector.setFilter( this.getElementFilter().copy() ) ;
		this.replaceCurrentInspector( inspector ) ;
	} // openNewInspector() 
	
	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------
	
	protected void buildUI()
	{
		InspectionFrame frame ;

		frame = new InspectionFrame( this ) ;
		this.setMainFrame( frame ) ;
		frame.setJMenuBar( this.createMenuBar() ) ;
	} // buildUI() 

	// -------------------------------------------------------------------------

	protected JMenu createMenu( String text )
	{
		return ( new JMenu( text ) ) ;
	} // createMenu() 

	// -------------------------------------------------------------------------

	protected JMenuBar createMenuBar()
	{
		JMenuBar menuBar		= null ;

		menuBar = new JMenuBar() ;
		// --- Window ---
		menuBar.add( this.createSubmenuWindow() ) ;
		// --- Actions ---
		menuBar.add( this.createSubmenuActions() ) ;
		// --- Show ---
		menuBar.add( this.createSubmenuShow() ) ;
		// --- Plugins ---
		menuBar.add( this.createSubmenuPlugins() ) ;
		// --- Help ---
		menuBar.add( this.createSubmenuHelp() ) ;

		return menuBar ;
	} // createMenuBar() 

	// -------------------------------------------------------------------------

	protected JMenu createSubmenuActions()
	{
		JMenu menu 					= null ;
		JMenuItem menuItem	= null ;

		// --- Actions ---
		menu = this.createMenu( TXT_ACTIONS ) ;

		menuItem = this.createMenuItem(	TXT_INSPECT_HERE, actInspectHere ) ;
		menu.add( menuItem ) ;
		menuItem = this.createMenuItem(	TXT_INSPECT_IN_NEW_TAB, actInspectTab ) ;
		menu.add( menuItem ) ;
		menuItem = this.createMenuItem(	TXT_INSPECT_IN_NEW_WINDOW, actInspectWindow ) ;
		menu.add( menuItem ) ;

		menu.addSeparator() ;

		menuItem = this.createMenuItem(	TXT_BASIC_INSPECT_HERE, actBasicInspectHere ) ;
		menu.add( menuItem ) ;
		menuItem = this.createMenuItem(	TXT_BASIC_INSPECT_IN_NEW_TAB, actBasicInspectTab ) ;
		menu.add( menuItem ) ;
		menuItem = this.createMenuItem(	TXT_BASIC_INSPECT_IN_NEW_WINDOW, actBasicInspectWindow ) ;
		menu.add( menuItem ) ;
		
		menu.addSeparator() ;
		
		menuItem = this.createMenuItem(	TXT_SORT, actSort ) ;
		menu.add( menuItem ) ;
		menu.addSeparator() ;
		menuItem = this.createMenuItem(	TXT_CONTINUE_THREAD, actContinue ) ;
		menu.add( menuItem ) ;

		return menu ;
	} // createSubmenuActions() 

	// -------------------------------------------------------------------------

	protected JMenu createSubmenuWindow()
	{
		JMenu menu 					          = null ;
		JMenuItem menuItem	          = null ;
		ExportProvider exportProvider = null ;
		String[] keys ;

		menu = this.createMenu( "Window" ) ;

		keys = Inspector.exportProviderRegistry().keys() ;
		if ( keys.length > 0 )
		{
			for ( int i = 0 ; i < keys.length ; i++ )
			{
				exportProvider = Inspector.findExporterNamed( keys[i] ) ;
				if ( exportProvider != null )
				{
					String text = exportProvider.exportLabel() ;
					if ( text != null )
					{
						menuItem = this.createMenuItem(	text, Inspector.ExportPrefix + keys[i] ) ;
						menu.add( menuItem ) ;
					}
				}
			}
		 menu.addSeparator() ; 
		}
		// menuItem = this.createMenuItem(	"Close Tab", actCloseTab ) ;
		// menu.add( menuItem ) ;
		menuItem = this.createMenuItem(	"Close Window", actCloseWindow ) ;
		menu.add( menuItem ) ;
		menuItem = this.createMenuItem(	"Close All Windows", actCloseAll ) ;
		menu.add( menuItem ) ;

		return menu ;
	} // createSubmenuWindow() 

	// -------------------------------------------------------------------------

	protected JMenu createSubmenuShow()
	{
		JMenu menu 									= null ;
		JCheckBoxMenuItem menuItem	= null ;

		// --- Filter ---
		menu = this.createMenu( "Show" ) ;

		menuItem = this.getFilterMenu().getStaticMenuItem() ;
		menuItem.addActionListener( this ) ;
		menu.add( menuItem ) ;

		menuItem = this.getFilterMenu().getFinalMenuItem() ;
		menuItem.addActionListener( this ) ;
		menu.add( menuItem ) ;

		menuItem = this.getFilterMenu().getTransientMenuItem() ;
		menuItem.addActionListener( this ) ;
		menu.add( menuItem ) ;

		menu.addSeparator() ;

		menuItem = this.getFilterMenu().getPrivateMenuItem() ;
		menuItem.addActionListener( this ) ;
		menu.add( menuItem ) ;

		menuItem = this.getFilterMenu().getProtectedMenuItem() ;
		menuItem.addActionListener( this ) ;
		menu.add( menuItem ) ;

		menuItem = this.getFilterMenu().getPublicMenuItem() ;
		menuItem.addActionListener( this ) ;
		menu.add( menuItem ) ;

		menuItem = this.getFilterMenu().getPackageMenuItem() ;
		menuItem.addActionListener( this ) ;
		menu.add( menuItem ) ;
		
		return menu ;
	} // createSubmenuShow() 

	// -------------------------------------------------------------------------

	protected JMenu createSubmenuPlugins()
	{
		JMenu menu 					= null ;
		JMenuItem menuItem	= null ;

		// --- Plugins ---
		menu = this.createMenu( "Plugins" ) ;
		menuItem = this.createMenuItem(	"Inspector", actShowInspectorMapping ) ;
		menu.add( menuItem ) ;
		menuItem = this.createMenuItem(	"Renderer", actShowRendererMapping ) ;
		menu.add( menuItem ) ;
		menuItem = this.createMenuItem(	"Exporter", actShowExporterMapping ) ;
		menu.add( menuItem ) ;

		return menu ;
	} // createSubmenuPlugins() 

	// -------------------------------------------------------------------------

	protected JMenu createSubmenuHelp()
	{
		JMenu menu 					= null ;
		JMenuItem menuItem	= null ;

		// --- Help ---
		menu = this.createMenu( "Help" ) ;
		
		menuItem = this.createMenuItem(	"Show System Properties", actShowSystemProperties) ;
		menu.add( menuItem ) ;
		
		menu.addSeparator() ;
		
		menuItem = this.createMenuItem(	"License", actShowLicenseText ) ;
		menu.add( menuItem ) ;
		
		menuItem = this.createMenuItem(	"About " + Inspector.PROG_ID, actAbout ) ;
		menu.add( menuItem ) ;

		return menu ;
	} // createSubmenuHelp() 

	// -------------------------------------------------------------------------

	protected JMenuItem createMenuItem( String text, String command )
	{
		JMenuItem menuItem	= null ;

		menuItem = new JMenuItem( text ) ;
		menuItem.setActionCommand( command ) ;
		menuItem.addActionListener( this ) ;

		return menuItem ;
	} // createMenuItem() 

	// -------------------------------------------------------------------------

	protected FileUtil fileUtil() 
	{
		return FileUtil.current() ;
	} // fileUtil()
	
	// ------------------------------------------------------------------------- 
	
} // class InspectionWindowController 
