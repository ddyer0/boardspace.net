// ===========================================================================
// CONTENT  : CLASS Inspector
// AUTHOR   : Manfred Duchrow
// VERSION  : 3.0 - 17/11/2010
// HISTORY  :
//  29/11/1999  duma  CREATED
//	11/01/2000	duma	RENAMED		->	from AbstractInspector
//	11/01/2000	duma	extended	->	inspectorRegistry
//	11/01/2000	duma	extended	->	Support of different Inspector classes
//	28/01/2000	duma	removed		->	Runnable
//  26/06/2000  duma  added     ->  StringInspector
//  22/07/2001  duma  added     ->  Support of named object inspection
//	26/01/2002	duma	added			->	Icons and model access
//	26/02/2003	duma	changed		->	Registry for inspector and exporter mapping
//	02/05/2004	duma	changed		->	V2.1
//	15/08/2004	duma	changed		->	V2.1.1
//	27/12/2004	mdu		changed		->	V2.2
//	15/06/2006	mdu		changed		->	V2.3
//	22/07/2007	mdu		changed		->	V2.4
//	12/12/2008	mdu		changed		->	V2.5
//	17/11/2010	mdu		changed		->	V3.0
//
// Copyright (c) 1999-2010 by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.awt.event.MouseAdapter;
import java.util.ArrayList;
import java.util.List;

import org.pf.reflect.ClassInfo;
import org.pf.text.StringUtil;

/**
 * This is the default inspector, which can display all normal java objects.
 * Currently it opens a window on the object to inspect and displays all
 * of its fields that are <b>not</b> <i>static</i> and <b>not</b> <i>final</i>,
 * which means no class variables and no constants, but all instance variables
 * (including inherited attributes).<br>
 * For arrays it lists up all elements from 0 to n.<br>
 * For deeper inspection it is possible to open a new inspector on each
 * attribute.<br>
 * <br>
 * Here is an example how to use the inspector:<br>
 * <ul><code>
 * panel = new JPanel() ;<br>
 * Inspector.inspect( panel ) ;
 * </code></ul>
 *
 * @author Manfred Duchrow
 * @version 3.0
 * @since JDK 1.5
 */
abstract public class Inspector extends MouseAdapter
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final String PROG_NAME						= "Java Object Inspector" ;
	protected static final String PROG_ID							= "JOI" ;
	protected static final String PROG_VERSION				= "V3.0" ;
	protected static final String PROG_COPYRIGHT			= "Copyright (c) 1999-2010, by Manfred Duchrow" ;
	protected static final String PROG_SIGNATURE			= PROG_ID + " " + PROG_VERSION ;
	protected static final String PROG_FULL_SIGNATURE	= PROG_NAME + " " + PROG_VERSION ;

	protected static final String ActionSeparator       = "|" ;
	protected static final String ExportPrefix          = "Export" + ActionSeparator ;
	protected static final String ImportPrefix          = "Import" + ActionSeparator ;

  /**
   * The filename that will be looked up in each classpath element to load
   * inspector classes ( subclasses of BasicInspector ) automatically into 
   * the registry of JOI.<br>
   * Filename: <b>"META-INF/joi.inspector"</b>
   */
	public static final String INSPECTOR_MAPPING_FILENAME = "META-INF/joi.inspector" ;
	public static final String INSPECTOR_MAPPING_FILENAME_CLASSLOADER = "/META-INF/joi-cl.inspector" ;
	public static final String INSPECTOR_MAPPING_FILENAME_ALL = "/META-INF/joi-all.inspector" ;

  /**
   * The filename that will be looked up in each classpath element to load
   * exporter classes ( implementors of ExportProvider ) automatically into 
   * the registry of JOI.<br>
   * Filename: <b>"META-INF/joi.exporter"</b>
   */
	public static final String EXPORTER_MAPPING_FILENAME = "META-INF/joi.exporter" ;
	public static final String EXPORTER_MAPPING_FILENAME_CLASSLOADER = "/META-INF/joi-cl.exporter" ;
	public static final String EXPORTER_MAPPING_FILENAME_ALL = "/META-INF/joi-all.exporter" ;

  // =========================================================================
  // CLASS VARIABLES
  // =========================================================================
  private static List controllerRegistry = new ArrayList() ;
  protected static List getControllerRegistry() { return controllerRegistry ; }
  protected static void setControllerRegistry( List newValue ) { controllerRegistry = newValue ; }

  private static ClassAssociations inspectorBinding = null ;
  protected static ClassAssociations getInspectorBinding() { return inspectorBinding ; }
  protected static void setInspectorBinding( ClassAssociations newValue ) { inspectorBinding = newValue ; }
  
  // Registry for plugged-in Import-Export providers
  private static ClassAssociations exportProviderRegistry = null ;
	protected static ClassAssociations getExportProviderRegistry() { return exportProviderRegistry ; }
	protected static void setExportProviderRegistry( ClassAssociations newValue ) { exportProviderRegistry = newValue ; }

  private static boolean haltCurrentProccess = false ;
  protected static boolean getHaltCurrentProccess() { return haltCurrentProccess ; }
  protected static void setHaltCurrentProccess( boolean newValue ) { haltCurrentProccess = newValue ; }
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private AbstractObjectSpy inspectedObject = null ;
	protected AbstractObjectSpy getInspectedObject() { return inspectedObject ; }
	protected void setInspectedObject( AbstractObjectSpy newValue ) { inspectedObject = newValue ; }

  // =========================================================================
  // PUBLIC CLASS METHODS
  // =========================================================================
	/**
	 * Inspect the given object.   <br>
	 * That means to display the internal state of the given object's attributes.
	 * This method is always using the basic inspector.
	 *
	 * @param obj The object to look inside
	 */
	public static Inspector basicInspect( Object obj )
	{
		return basicInspect( null, obj ) ;
	} // basicInspect() 

  // -------------------------------------------------------------------------

	/**
	 * Inspect the given object.   <br>
	 * That means to display the internal state of the given object's attributes.
	 * This method is always using the basic inspector.
	 *
   * @param name The name of the object in its program context
	 * @param obj The object to look inside
	 */
	public static Inspector basicInspect( String name, Object obj )
	{
		return ( launchInspectorOn( new BasicInspector(), name, obj ) ) ;
	} // basicInspect() 

  // -------------------------------------------------------------------------

	/**
	 * Inspect the given object.   <br>
	 * That means to display the internal state of the given object's attributes.
	 * The inspector can be a specialized one for the class of the given object.
	 *
	 * @param obj The object to look inside
	 */
	public static Inspector inspect( Object obj )
	{
		return inspect( null, obj ) ;
	} // inspect() 

  // -------------------------------------------------------------------------

	/**
	 * Inspect the given object.   <br>
	 * That means to display the internal state of the given object's attributes.
	 * The inspector can be a specialized one for the class of the given object.
	 *
	 * @param obj The object to look inside
	 */
	public static Inspector inspect( String name, Object obj )
	{
		return ( launchInspectorOn( getInspectorFor( obj ), name, obj ) ) ;
	} // inspect() 

  // -------------------------------------------------------------------------

	/**
	 * Inspect the given object like in basicInspect().   <br>
	 * But this method doesn't return until the inspector and all its
   * sub-inspectors are closed again.
	 *
	 * @param obj The object to look inside
	 */
	public static void basicInspectWait( Object obj )
	{
		basicInspectWait( null, obj ) ;
	} // basicInspectWait() 

  // -------------------------------------------------------------------------

	/**
	 * Inspect the given object like in basicInspect().   <br>
	 * But this method doesn't return until the inspector and all its
   * sub-inspectors are closed again.
	 *
   * @param name The name of the object in its program context
	 * @param obj The object to look inside
	 */
	public static void basicInspectWait( String name, Object obj )
	{
		basicInspect( name, obj ) ;
		halt() ;
	} // basicInspectWait() 

  // -------------------------------------------------------------------------

	/**
	 * Inspect the given object like in inspect().   <br>
	 * But this method doesn't return until the inspector and all its
   * sub-inspectors are closed again.
	 *
	 * @param obj The object to look inside
	 */
	public static void inspectWait( Object obj )
	{
		inspectWait( null, obj ) ;
	} // inspectWait() 

  // -------------------------------------------------------------------------

	/**
	 * Inspect the given object like in inspect().   <br>
	 * But this method doesn't return until the inspector and all its
   * sub-inspectors are closed again.
	 *
   * @param name The name of the object in its program context
	 * @param obj The object to look inside
	 */
	public static void inspectWait( String name, Object obj )
	{
		inspect( name, obj ) ;
		halt() ;
	} // inspectWait() 

  // -------------------------------------------------------------------------

	/**
	 * Bind a specific class or interface to a special inspector class.   <br>
	 * This can be used to "install" self written specialized inspectors for
	 * specific classes or groups of classes.<br>
	 * If there is for example a special inspector for dates this method could look
	 * like the following:<br>
	 * <br>
	 * Inspector.bindInspector( "java.util.Date", "com.xxx.debug.DateInspector" ) ;<br>
	 * <br>
	 * or if there is a particular inspector for all objects that understand the
	 * BeanInfo interface:
	 * <br>
	 * Inspector.bindInspector( "java.beans.BeanInfo", "org.zzzz.inspect.BeanInfoInspector" ) ;<br>
	 * <br>
	 *
	 * @param className The fully qualified name of the class or interface
	 * @param inspectorName The fully qualified class name of the associated inspector
	 */
	public static void bindInspector( String className, String inspectorName )
	{
		inspectorBinding() ; // enforce initialization
		basicBindInspector( className, inspectorName ) ;
	} // bindInspector() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the object spy wrapper for the given object.   <br>
	 * It will be wrapped by the basic inspector.
   *
	 * @param obj The object to look inside
	 */
	public static AbstractObjectSpy getBasicObjectSpy( Object obj )
	{
		return getBasicObjectSpy( null, obj ) ;
	} // getBasicObjectSpy() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the object spy wrapper for the given object.   <br>
	 * It will be wrapped by the basic inspector.
   *
   * @param name The name of the object in its program context
	 * @param obj The object to look inside
	 */
	public static AbstractObjectSpy getBasicObjectSpy( String name, Object obj )
	{
    Inspector inspector   = null ;
    inspector = new BasicInspector() ;
		return inspector.objectSpyFor( name, obj ) ;
	} // getBasicObjectSpy() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the object spy wrapper for the given object.   <br>
	 * It will be wrapped by the inspector that is registerd for
   * the object's type.
   *
	 * @param obj The object to look inside
	 */
	public static AbstractObjectSpy getObjectSpy( Object obj )
	{
		return getObjectSpy( null, obj ) ;
	} // getObjectSpy() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the object spy wrapper for the given object.   <br>
	 * It will be wrapped by the inspector that is registerd for
   * the object's type.
   *
   * @param name The name the object has in the program context
	 * @param obj The object to look inside
	 */
	public static AbstractObjectSpy getObjectSpy( String name, Object obj )
	{
    Inspector inspector   = null ;
    inspector = getInspectorFor( obj ) ;
		return inspector.objectSpyFor( name, obj ) ;
	} // getObjectSpy() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the component's long name and current version
	 */
	public static String fullIdentification()
	{
		return PROG_FULL_SIGNATURE ;
	} // fullIdentification() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the component's short name and current version
	 */
	public static String shortIdentification()
	{
		return PROG_SIGNATURE ;
	} // shortIdentification() 

  // -------------------------------------------------------------------------

	/**
	 * Continues the current process.
	 */
	public static void deactivateHalt()
	{
		setHaltCurrentProccess(false) ;
	} // deactivateHalt() 
  // -------------------------------------------------------------------------

	/**
	 * Halts the current process.
	 */
	public static void halt()
	{
		setHaltCurrentProccess(true) ;
		waitWhileHaltActive() ;
	} // halt() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the name and current version of this component.
	 */
	public static String getProgSignature()
	{
		return PROG_SIGNATURE ;
	} // getProgSignature() 

	// -------------------------------------------------------------------------

	public static void main( String[] args )
	{
		inspectWait( "System Properties", System.getProperties() ) ;
		System.exit(0);
	} // main()
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED CLASS METHODS
  // =========================================================================
	/**
	 * Opens a new inspector inside the given frame. No new window will be 
	 * opended.
	 *
	 * @param frame The frame the inspector should display its information
	 * @param name The name of the object
	 * @param obj The object to inspect
	 */
	protected static BasicInspector inspectIn( 
							InspectionWindowController controller, int where,  
																						String name, Object obj )
	{
		BasicInspector inspector ;
		
		inspector = getInspectorFor( obj ) ;
		openInspectorIn( controller, where, inspector, name, obj ) ;
		return inspector ;
	} // inspectIn() 

	// -------------------------------------------------------------------------

	/**
	 * Opens a new basic inspector inside the given frame. No new window will be 
	 * opended.
	 *
	 * @param frame The frame the inspector should display its information
	 * @param name The name of the object
	 * @param obj The object to inspect
	 */
	protected static BasicInspector basicInspectIn( 
											InspectionWindowController controller, int where,  
																									String name, Object obj )
	{
		BasicInspector inspector ;
		
		inspector = new BasicInspector() ;
		openInspectorIn( controller, where, inspector, name, obj ) ;
		return inspector ;
	} // basicInspectIn() 

	// -------------------------------------------------------------------------

	/**
	 * Opens a new inspector inside the given frame. No new window will be 
	 * opended.
	 *
	 * @param frame The frame the inspector should display its information
	 * @param name The name of the object
	 * @param obj The object to inspect
	 */
	protected static BasicInspector openInspectorIn( 
					InspectionWindowController controller, int where, 
					BasicInspector inspector,	String name, Object obj )
	{
		inspector.inspectObject( name, obj ) ;
		controller.openNewInspector( inspector, where ) ;
		return inspector ;
	} // openInspectorIn() 

	// -------------------------------------------------------------------------

	/**
	 * Starts the given inspector.   <br>
	 * Before starting an inspector it is registered for overall inspector
	 * management.
	 *
	 * @param inspector The inspector that should be started.
	 */
	protected static Inspector launchInspectorOn( BasicInspector inspector, 
																								String name, Object obj )
	{
		InspectionWindowController controller ;
		
		inspector.inspectObject( name, obj ) ;
		controller = new InspectionWindowController() ; 
		registerController( controller ) ;
		controller.start( inspector ) ;
		return inspector ;
	} // launchInspectorOn() 

  // -------------------------------------------------------------------------

	protected static BasicInspector getInspectorFor( Object obj )
	{
		BasicInspector inspector			= null ;
		ClassInfo inspectorClassInfo	= null ;

		if ( obj != null )
		{
			inspectorClassInfo = findInspectorClassInfoFor( obj ) ;
			if ( inspectorClassInfo != null )
			{
				inspector = (BasicInspector)inspectorClassInfo.createInstance() ;
			}
		}

		/* Set the default inspector */
		if ( inspector == null )
		{
			inspector = new BasicInspector() ;
		}

		return inspector ;
	} // getInspectorFor() 

  // -------------------------------------------------------------------------

  protected static ClassInfo findInspectorClassInfoFor( Object object )
	{
		return inspectorBinding().findForClassOfObject( object.getClass(), object ) ;
	} // findInspectorClassInfoFor() 

  // -------------------------------------------------------------------------

	protected static ClassAssociations inspectorBinding()
	{
		if ( getInspectorBinding() == null )
		{
			initializeInspectorBinding() ;
		}
		return getInspectorBinding() ;
	} // inspectorBinding() 

  // -------------------------------------------------------------------------

  protected static void initializeInspectorBinding()
  {
    setInspectorBinding( new InspectorRegistry() ) ;
    CommonFunctions.loadPluginDefinitions( getInspectorBinding(), INSPECTOR_MAPPING_FILENAME,
    		INSPECTOR_MAPPING_FILENAME_CLASSLOADER, INSPECTOR_MAPPING_FILENAME_ALL);
  } // initializeInspectorBinding() 

	// -------------------------------------------------------------------------

	protected static void basicBindInspector( String className, String inspectorName )
	{
		getInspectorBinding().register( className, inspectorName ) ;
	} // basicBindInspector() 

  // -------------------------------------------------------------------------

	/**
	 * Registers the given controller.   <br>
	 *
	 * @param controller The controller that should be registered.
	 */
	protected static void registerController( InspectionWindowController controller )
	{
		getControllerRegistry().add( controller ) ;
	} // registerController() 

  // -------------------------------------------------------------------------

	/**
	 * Unregisters the given inspector.   <br>
	 *
	 * @param inspector The inspector that should be unregistered.
	 */
	protected static void unregisterController( InspectionWindowController controller )
	{
		getControllerRegistry().remove( controller ) ; 
	} // unregisterController() 

  // -------------------------------------------------------------------------

	/**
	 * Returns whether at least one inspector is currently running.   <br>
	 */
	protected static boolean anyInspectorActive()
	{
		return ( getControllerRegistry().size() > 0 ) ;
	} // anyInspectorActive() 

  // -------------------------------------------------------------------------

	/**
	 * Returns whether or not the current proccess halt is still active
	 */
	protected static boolean isHaltActive()
	{
		return ( getHaltCurrentProccess() && anyInspectorActive() ) ;
	} // isHaltActive() 

  // -------------------------------------------------------------------------

  /**
   * Runs an endless loop until the last inspector is closed
   * or the current should explicitly be continued.
   */
  protected static void waitWhileHaltActive()
  {
	  while ( isHaltActive() )
	  {
	    try
	    {
	      Thread.sleep(20) ;
	    }
	    catch ( Exception ex ) {}
	  }
  } // waitWhileHaltActive() 

  // -------------------------------------------------------------------------

	protected static ClassAssociations exportProviderRegistry()
	{
		if ( getExportProviderRegistry() == null )
		{
			initializeExportProviderRegistry() ;
		}
		return getExportProviderRegistry() ;
	} // exportProviderRegistry() 

  // -------------------------------------------------------------------------

  protected static void initializeExportProviderRegistry()
  {
    setExportProviderRegistry( new ClassAssociations( ExportProvider.class ) ) ;
    CommonFunctions.loadPluginDefinitions(getExportProviderRegistry(), EXPORTER_MAPPING_FILENAME, 
    		EXPORTER_MAPPING_FILENAME_CLASSLOADER, EXPORTER_MAPPING_FILENAME_ALL);
  } // initializeExportProviderRegistry() 

	// -------------------------------------------------------------------------

	protected static void closeAllControllers()
	{
		InspectionWindowController[] controller ;
		int index ;

		controller = new InspectionWindowController[getControllerRegistry().size()] ;
		getControllerRegistry().toArray( controller ) ;
		for ( index = 0 ; index < controller.length ; index++ )
		{
			controller[index].terminate() ;
		}
	} // closeAllControllers() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the JOI about information.
	 */
	protected static String getAboutInfoText()
	{
		String text 	= null ;

		text = PROG_ID + " - " + PROG_NAME + " " + PROG_VERSION + "\n\n" ;
		text = text + PROG_COPYRIGHT + "\n\n" ;
		text = text + "All Rights Reserved.\n" ;
		return text ;
	} // getAboutInfoText() 

	// -------------------------------------------------------------------------

	protected static ExportProvider findExporterNamed( String exporterId )
	{
		ExportProvider exportProvider = null ;
		String key = exporterId ;

		if ( key.startsWith( ExportPrefix ) )
			key = StringUtil.current().suffix( key, ExportPrefix ) ;

		exportProvider = (ExportProvider)exportProviderRegistry().newInstance(key) ;

		return exportProvider ;
	} // findExporterNamed() 

	// -------------------------------------------------------------------------

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  protected Inspector()
  {
  } // Inspector() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

	/**
	 * Inspect the given object.   <br>
	 * That means to display the internal state of the given object's attributes.
	 *
	 * @param obj The object to look inside
	 */
	protected void inspectObject( String name, Object obj )
	{
		AbstractObjectSpy ospy 	= null ;

		ospy = this.objectSpyFor( name, obj ) ;
		this.setInspectedObject( ospy ) ;
	} // inspectObject() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the correct wrapper class (spy) for the given object.   <br>
	 * Subclasses probably must override this method to support their own
	 * spy classes.
	 *
	 * @param obj The object to inspect
	 */
	protected AbstractObjectSpy objectSpyFor( Object obj )
	{
		AbstractObjectSpy ospy 	= null ;

		if ( ( obj != null ) && ( obj.getClass().isArray() ) )
		{
			ospy = new ArraySpy( obj ) ;
		}
		else
		{
			ospy = new ObjectSpy( obj ) ;
		}
		return ( ospy ) ;
	} // objectSpyFor() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the correct wrapper class (spy) for the given object.   <br>
	 * The wrapper then gets the given name.
   *
	 * @param obj The object to inspect
	 */
	protected AbstractObjectSpy objectSpyFor( String name, Object obj )
	{
		AbstractObjectSpy ospy 	= null ;

    ospy = this.objectSpyFor( obj ) ;
    if ( name != null )
      ospy.setName( name ) ;
		return ospy ;
	} // objectSpyFor() 

  // -------------------------------------------------------------------------

} // class Inspector 
