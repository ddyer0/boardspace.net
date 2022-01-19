// ===========================================================================
// CONTENT  : CLASS Preferences
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 16/04/2006
// HISTORY  :
//  13/03/2004  mdu  CREATED
//	16/04/2006	mdu		added		-->	initialModifiers
//
// Copyright (c) 2004-2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Properties;

import org.pf.file.PropertyFileLoader;
import org.pf.util.Bool;

/**
 * This singleton contains the general settings of JOI.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class Preferences
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  protected static final String CONFIG_FILENAME					= "joi.properties" ;
  protected static final String DEFAULT_CONFIG_FILENAME	= "joi_default.properties" ;
  
	protected static final int DEFAULT_MAIN_MARGIN							= 2 ;
	protected static final int DEFAULT_WINDOW_WIDTH							= 700 ;
	protected static final int DEFAULT_WINDOW_HEIGHT						= 500 ;
	protected static final int DEFAULT_TREE_WIDTH								= 200 ;
	protected static final int DEFAULT_DOUBLE_CLICK_MODE				=
														InspectionWindowController.INSPECT_IN_NEW_WINDOW ;
	protected static final int DEFAULT_MIDDLE_BUTTON_MODE				=
														InspectionWindowController.INSPECT_IN_NEW_TAB ;
	protected static final  boolean DEFAULT_AUTO_SORT 					= false ;
	protected static final  boolean DEFAULT_ALLOW_MODIFY				= true ;
	
	protected static final String OPT_DOUBLE_CLICK_ACTION	= "action.doubleclick" ;
	protected static final String OPT_MIDDLE_BUTTON				= "action.middle.button" ;
	protected static final String OPT_WINDOW_WIDTH				= "window.width" ;
	protected static final String OPT_WINDOW_HEIGHT				= "window.height" ;
	protected static final String OPT_TREE_WIDTH					= "tree.width" ;
	protected static final String OPT_AUTO_SORT						= "auto.sort" ;
	protected static final String OPT_ALLOW_MODIFY				= "allow.modification" ;
	protected static final String OPT_QUOTE_STRINGS				= "quote.strings" ;
	protected static final String OPT_STATIC_DEFAULT			= "modifier.static.default" ;
	protected static final String OPT_FINAL_DEFAULT				= "modifier.final.default" ;
	protected static final String OPT_TRANSIENT_DEFAULT		= "modifier.transient.default" ;
	protected static final String OPT_PUBLIC_DEFAULT			= "modifier.public.default" ;
	protected static final String OPT_PRIVATE_DEFAULT			= "modifier.private.default" ;
	protected static final String OPT_PROTECTED_DEFAULT		= "modifier.protected.default" ;
	protected static final String OPT_PACKAGE_DEFAULT			= "modifier.package.default" ;
	
	protected static final String DOUBLE_CLICK_OPEN_WINDOW	= "openNewWindow" ;
	protected static final String DOUBLE_CLICK_OPEN_TAB			= "openNewTab" ;
	protected static final String DOUBLE_CLICK_REPLACE			= "openReplaceCurrent" ;

	static final String[] MODIFIER_OPTIONS = 
	{
		OPT_STATIC_DEFAULT, OPT_FINAL_DEFAULT, OPT_TRANSIENT_DEFAULT, OPT_PRIVATE_DEFAULT,
		OPT_PROTECTED_DEFAULT, OPT_PUBLIC_DEFAULT, OPT_PACKAGE_DEFAULT
	} ;
	
  // =========================================================================
  // CLASS VARIABLES
  // =========================================================================
  private static Preferences soleInstance = new Preferences() ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private int doubleClickMode = DEFAULT_DOUBLE_CLICK_MODE ;
  protected void setDoubleClickMode( int newValue ) { doubleClickMode = newValue ; }

  private int middleButtonMode = DEFAULT_MIDDLE_BUTTON_MODE ;
  protected void setMiddleButtonMode( int newValue ) { middleButtonMode = newValue ; }
  
  private int windowWidth = DEFAULT_WINDOW_WIDTH ;
  protected void setWindowWidth( int newValue ) { windowWidth = newValue ; }  
  
  private int windowHeight = DEFAULT_WINDOW_HEIGHT ;
  protected void setWindowHeight( int newValue ) { windowHeight = newValue ; }
  
  private int treeWidth = DEFAULT_TREE_WIDTH ;
  protected void setTreeWidth( int newValue ) { treeWidth = newValue ; }    
  
  private boolean autoSort = DEFAULT_AUTO_SORT ;
  protected boolean getAutoSort() { return autoSort ; }
  protected void setAutoSort( boolean newValue ) { autoSort = newValue ; }  
  
  private boolean isEditingSupported = DEFAULT_ALLOW_MODIFY ;
  protected void setIsEditingSupported( boolean newValue ) { isEditingSupported = newValue ; }
  
  private boolean quoteStrings = true ;
  protected void setQuoteStrings( boolean newValue ) { quoteStrings = newValue ; }  
  
  private ElementFilter initialElementFilter = null ;
  protected void setInitialElementFilter( ElementFilter newValue ) { initialElementFilter = newValue ; }
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  /**
   * Returns the only instance this class supports (design pattern "Singleton")
   */
  public static Preferences instance()
  {
    return soleInstance ;
  } // instance() 

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  private Preferences()
  {
    super() ;
    this.initialize() ;
  } // Preferences() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the initial width for a window
   */
	public int getWindowWidth()
	{
		return windowWidth ;	
	} // getWindowWidth() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns the initial width for a window
	 */
	public int getWindowHeight()
	{
		return windowHeight ;	
	} // getWindowHeight() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns the margin to be used inside the main window
	 */
	public int getMainMargin()
	{
		return DEFAULT_MAIN_MARGIN ;
	} // getMainMargin() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the the width of the tree view
	 */
  public int getTreeWidth()
	{
		return treeWidth;
	} // getTreeWidth() 

	// -------------------------------------------------------------------------
		
	/**
	 * Returns a open mode that defines where to inspect the selected element
	 * with a double-click.
	 * <ol>
	 * <li>In the current place
	 * <li>In a new tab
	 * <li>In a new window
	 * </ol>
	 */
	public int getDoubleClickMode() 
	{
		return doubleClickMode ;
	} // getDoubleClickMode() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns a open mode that defines where to inspect the selected element
	 * with pressing the middle mouse button.
	 * <ol>
	 * <li>In the current place
	 * <li>In a new tab
	 * <li>In a new window
	 * </ol>
	 */
  public int getMiddleButtonMode()
	{
		return middleButtonMode;
	} // getMiddleButtonMode() 

  // -------------------------------------------------------------------------
  
  /**
   * Returns true if automatic sorting of elements is desired
   */
  public boolean isAutoSortOn() 
	{
		return this.getAutoSort() ;
	} // isAutoSortOn() 

	// -------------------------------------------------------------------------

  /**
   * Returns true if editing og object values is supported
   */
  public boolean isEditingSupported()
	{
		return isEditingSupported;
	} // isEditingSupported() 

 	// -------------------------------------------------------------------------

  /**
   * Returns whether or not strings should be enclosed by quotes when shown
   * in an inspector.
   */
  public boolean getQuoteStrings() 
  { 
  	return quoteStrings ; 
  } // getQuoteStrings() 

  // -------------------------------------------------------------------------

  /**
   * Returns the a copy of the initial element filter
   */
  public ElementFilter getInitialElementFilter()
	{
		return initialElementFilter.copy();
	} // getInitialElementFilter() 

  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected void initialize() 
	{
		Properties config =null;
		Properties defaultConfig =null;
		try {
		defaultConfig = PropertyFileLoader.loadProperties( DEFAULT_CONFIG_FILENAME ) ;
		config = PropertyFileLoader.loadProperties( CONFIG_FILENAME, defaultConfig ) ;
		} catch (ExceptionInInitializerError err) 
		{
			System.out.println("Using default config");
		}
		if ( config == null )
		{
			config = defaultConfig ;
		}
		if ( config != null )
		{
			this.initialize( config ) ;
		}
	} // initialize() 

	// -------------------------------------------------------------------------
	
	protected void initialize( Properties config ) 
	{
		this.initDoubleClickAction( config.getProperty( OPT_DOUBLE_CLICK_ACTION ) ) ;
		this.initMiddleButtonAction( config.getProperty( OPT_MIDDLE_BUTTON ) ) ;
		this.initWindowWidth( config.getProperty( OPT_WINDOW_WIDTH ) ) ;
		this.initWindowHeight( config.getProperty( OPT_WINDOW_HEIGHT ) ) ;
		this.initTreeWidth( config.getProperty( OPT_TREE_WIDTH ) ) ;
		this.initAutoSort( config.getProperty( OPT_AUTO_SORT ) ) ;
		this.initAllowModify( config.getProperty( OPT_ALLOW_MODIFY ) ) ;
		this.initQuoteStrings( config.getProperty( OPT_QUOTE_STRINGS ) ) ;
		this.initInitialElementFilter( config ) ;
	} // initialize() 

	// -------------------------------------------------------------------------
	
	protected void initDoubleClickAction( String option ) 
	{
		int action ;
		
		action = this.detectOpenAction( option ) ;
		if ( action >= 0 )
		{
			this.setDoubleClickMode( action ) ;
		}
	} // initDoubleClickAction() 

	// -------------------------------------------------------------------------

	protected void initMiddleButtonAction( String option ) 
	{
		int action ;
		
		action = this.detectOpenAction( option ) ;
		if ( action >= 0 )
		{
			this.setMiddleButtonMode( action ) ;
		}
	} // initMiddleButtonAction() 

	// -------------------------------------------------------------------------

	protected void initWindowWidth( String option ) 
	{
		int value ;
		
		value = this.positiveInt( option ) ;
		if ( value >= 300 )
		{
			this.setWindowWidth( value ) ;
		}
	} // initWindowWidth() 

	// -------------------------------------------------------------------------
	
	protected void initWindowHeight( String option ) 
	{
		int value ;
		
		value = this.positiveInt( option ) ;
		if ( value >= 200 )
		{
			this.setWindowHeight( value ) ;
		}
	} // initWindowHeight() 

	// -------------------------------------------------------------------------
	
	protected void initTreeWidth( String option ) 
	{
		int value ;
		
		value = this.positiveInt( option ) ;
		if ( value >= 100 )
		{
			this.setTreeWidth( value ) ;
		}
	} // initTreeWidth() 

	// -------------------------------------------------------------------------
	
	protected void initAutoSort( String option ) 
	{
		if ( option != null )
		{
			this.setAutoSort( Bool.isTrue( option ) ) ;
		}
	} // initAutoSort() 

	// -------------------------------------------------------------------------
	
	protected void initAllowModify( String option ) 
	{
		if ( option != null )
		{
			this.setIsEditingSupported( Bool.isTrue( option ) ) ;
		}
	} // initAllowModify() 

	// -------------------------------------------------------------------------
	
	protected void initQuoteStrings( String option ) 
	{
		if ( option != null )
		{
			this.setQuoteStrings( Bool.isTrue( option ) ) ;
		}
	} // initQuoteStrings() 

	// -------------------------------------------------------------------------
	
	protected int positiveInt( String str ) 
	{
		int value = -1 ;
		
		if ( str != null )
		{
			try
			{
				value = Integer.parseInt( str.trim() ) ;
			}
			catch ( Exception e )
			{
				// Ignore it, value will be -1
			}
		}
		return value ;
	} // positiveInt() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns the action code or a negative value if nothing (correct) was 
	 * specified in the given option.
	 */
	protected int detectOpenAction( String option ) 
	{
		if ( option != null )
		{
			if ( DOUBLE_CLICK_OPEN_TAB.equalsIgnoreCase( option ) )
				return InspectionWindowController.INSPECT_IN_NEW_TAB ; 
			else if ( DOUBLE_CLICK_OPEN_WINDOW.equalsIgnoreCase( option ) )
				return InspectionWindowController.INSPECT_IN_NEW_WINDOW ;
			else if ( DOUBLE_CLICK_REPLACE.equalsIgnoreCase( option ) )
				return InspectionWindowController.INSPECT_IN_CURRENT_PLACE ;
		}
		return -1 ;
	} // detectOpenAction() 

	// -------------------------------------------------------------------------

	protected void initInitialElementFilter( Properties config ) 
	{
		String value ;
		int modifier = 0 ;
		ElementFilter filter ;
		
		filter = new ElementFilter(0,false) ;
		this.setInitialElementFilter( filter ) ;
		for (int i = 0; i < MODIFIER_OPTIONS.length; i++ )
		{
			value = config.getProperty( MODIFIER_OPTIONS[i], "true" ) ;
			if ( Bool.isFalse( value ) )
			{
				if ( OPT_STATIC_DEFAULT.equals( MODIFIER_OPTIONS[i] ) )
				{
					modifier = ElementFilter.STATIC ;
				}
				else if( OPT_FINAL_DEFAULT.equals( MODIFIER_OPTIONS[i] ) )
				{
					modifier = ElementFilter.FINAL ;
				} 
				else if( OPT_TRANSIENT_DEFAULT.equals( MODIFIER_OPTIONS[i] ) )
				{
					modifier = ElementFilter.TRANSIENT ;
				} 
				else if( OPT_PUBLIC_DEFAULT.equals( MODIFIER_OPTIONS[i] ) )
				{
					modifier = ElementFilter.PUBLIC ;
				} 
				else if( OPT_PRIVATE_DEFAULT.equals( MODIFIER_OPTIONS[i] ) )
				{
					modifier = ElementFilter.PRIVATE ;
				} 
				else if( OPT_PROTECTED_DEFAULT.equals( MODIFIER_OPTIONS[i] ) )
				{
					modifier = ElementFilter.PROTECTED ;
				} 
				else if( OPT_PACKAGE_DEFAULT.equals( MODIFIER_OPTIONS[i] ) )
				{
					modifier = ElementFilter.DEFAULT ;
				} 
				filter.toggleSwitch( modifier ) ;
			}
		}
	} // initInitialElementFilter() 
	
	// -------------------------------------------------------------------------
	
} // class Preferences 
