// ===========================================================================
// CONTENT  : CLASS PluginHolder
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 25/02/2007
// HISTORY  :
//  25/02/2007  mdu  CREATED
//
// Copyright (c) 2007, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.plugin ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Properties;

import org.pf.reflect.ClassInfo;

/**
 * This is a container that holds all necessary data of a particular plug-in.
 *
 * @author M.Duchrow
 * @version 1.0
 */
class PluginHolder
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String externalId = null ;
  protected String getExternalId() { return externalId ; }
  protected void setExternalId( String newValue ) { externalId = newValue ; }
  
  private ClassInfo classInfo = null ;
  protected ClassInfo getClassInfo() { return classInfo ; }
  protected void setClassInfo( ClassInfo newValue ) { classInfo = newValue ; }
  
  private boolean isSingleton = false ;
  protected boolean isSingleton() { return isSingleton ; }
  protected void isSingleton( boolean newValue ) { isSingleton = newValue ; }
  
  private Properties configuration = new Properties() ;
  protected Properties getConfiguration() { return configuration ; }
  protected void setConfiguration( Properties newValue ) { configuration = newValue ; }
  
  private Object soleInstance = null ;
  protected Object getSoleInstance() { return soleInstance ; }
  protected void setSoleInstance( Object newValue ) { soleInstance = newValue ; }
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  protected PluginHolder( String id, ClassInfo info )
  {
    super() ;
    this.setExternalId( id ) ;
    this.setClassInfo( info ) ;
  } // PluginHolder() 
  
  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns an instance of the underly plug-in. If it is defined to be
   * a singleton, the sole instance will be returned for every call.
   * Otherwise a new instance will be created for each call.
   * 
   * @return An instance of the plug-in or null if instance creation failed.
   */
  public Object getInstance() 
	{
		if ( this.isSingleton() )
		{
			if ( this.getSoleInstance() == null )
			{
				this.setSoleInstance( this.createNewInstance() ) ;
			}
			return this.getSoleInstance() ;
		}
		return this.createNewInstance() ;
	} // getInstance() 
	
	// -------------------------------------------------------------------------

  /**
   * Returns true if the plugin is of the given type. That implies all
   * sub-types, too!
   * 
   * @param pluginType The type to check
   */
	public boolean isPluginType( Class pluginType )
	{
		if ( pluginType == null )
		{
			return false ;
		}
		return this.getClassInfo().isAssignableTo( pluginType );
	} // isPluginType()
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected Object createNewInstance() 
	{
		Object object ;
		IInitializablePlugin initializablePlugin ;
		
		object = this.getClassInfo().createInstance() ;
		if ( object instanceof IInitializablePlugin )
		{
			initializablePlugin = (IInitializablePlugin)object ;
			initializablePlugin.initPlugin( this.getExternalId(), this.getConfiguration() ) ;
		}
		return object ;
	} // createNewInstance() 
	
	// -------------------------------------------------------------------------

} // class PluginHolder 
