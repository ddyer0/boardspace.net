// ===========================================================================
// CONTENT  : CLASS ReloadableSettings
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.3 - 18/03/2005
// HISTORY  :
//  15/06/2002  duma  CREATED
//	11/12/2002	duma	changed	-> Extends now Observable and notifies listeners 
//																after a reload
//  18/12/2002  duma  added   -> All new methods of ReadOnlySettings
//															 Changed constructor, added another constructor
//															 Introduced mustExist flag
//	06/03/2004	duma	bugfix	-> Removed potential NullPointerException in constructor
//										added		-> createException()
//	18/03/2005	mdu		added		-> startReloading(), stopReloading()
//
// Copyright (c) 2002-2005, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.settings.impl ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.File;
import java.util.Observable;

import org.pf.settings.ReadOnlySettings;
import org.pf.settings.SettingsFileReader;
import org.pf.util.Trigger;
import org.pf.util.TriggerClient;

/**
 * Implements settings that will be automatically reloaded
 * if their underlying file has been modified. The interval for
 * checking the file for changes can be specified.
 *
 * @author Manfred Duchrow
 * @version 1.3
 */
public class ReloadableSettings extends Observable 
																implements ReadOnlySettings, TriggerClient
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	static final boolean DEBUG 				= false ;
	private static int UNKNOWN_FILE_TIMESTAMP	= 0 ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private volatile ReadOnlySettings settings = null ;
  protected ReadOnlySettings getSettings() { return settings ; }
  protected void setSettings( ReadOnlySettings newValue ) { settings = newValue ; }
  
  private Trigger fileCheckTrigger = null ;
  protected Trigger getFileCheckTrigger() { return fileCheckTrigger ; }
  protected void setFileCheckTrigger( Trigger newValue ) { fileCheckTrigger = newValue ; }  
  
  private boolean fileMustExist = true ;
  protected boolean fileMustExist() { return fileMustExist ; }
  protected void fileMustExist( boolean newValue ) { fileMustExist = newValue ; }
  
  private File file = null ;
  protected File getFile() { return file ; }
  protected void setFile( File newValue ) { file = newValue ; }
  
  private SettingsFileReader reader = null ;
  protected SettingsFileReader getReader() { return reader ; }
  protected void setReader( SettingsFileReader newValue ) { reader = newValue ; }

  private long fileTimestamp = UNKNOWN_FILE_TIMESTAMP ;
  protected long getFileTimestamp() { return fileTimestamp ; }
  protected void setFileTimestamp( long newValue ) { fileTimestamp = newValue ; }
        
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with 
   * the interval after which the file has to be checked for changes 
   * and a reader that can read the file structure into a settings
   * object.
   * If the file can't be found an exception will be thrown.
   * 
   * @param checkInterval the interval in <b>seconds</b> after which the file must be checked
   * @param reader The reader that can read the settings file
   */
  public ReloadableSettings( long checkInterval, SettingsFileReader reader )
  	throws ReloadableSettingsException
  {
    this( checkInterval, reader, true ) ;
  } // ReloadableSettings() 

	// -----------------------------------------------------------------------
	
  /**
   * Initialize the new instance with the name of underlying file,
   * the interval after which the file has to be checked for changes 
   * and a reader that can read the file structure into a settings
   * object.
   * If mustExist is set to false and the settings file doesn't exist, the 
   * settings object is still valid and might be just empty. However it
   * keeps checking the file and whenever it exists it will be read in.
   * 
   * @param checkInterval the interval in <b>seconds</b> after which the file must be checked
   * @param reader The reader that can read the settings file
   * @param mustExist If true, the settings file must exist. If it doesn't exist an exception will be thrown
   */
  public ReloadableSettings( long checkInterval, SettingsFileReader reader,
  														boolean mustExist )
  	throws ReloadableSettingsException
  {
	  //super() ; 

		if ( reader == null )
			throw this.invalidParameter( "reader" ) ;

		String filename = reader.getFileName() ;   
    if ( filename == null )
    	throw this.invalidParameter( "reader.getFileName()" ) ;
    
    if ( checkInterval < 1 )
    	throw this.invalidParameter( "checkInterval" ) ;

		this.fileMustExist( mustExist ) ;    
    this.checkAndRememberFile( filename, mustExist ) ;
    this.setReader( reader ) ;
    this.loadSettings() ;
    this.clearChanged() ;
    this.startTrigger( checkInterval ) ;
  } // ReloadableSettings() 

	// -----------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns the name of the settings object as a whole. 
	 * This might be a simple name or a resource locator or a filename.
	 * However, naming a setting is obtional and therefore this method can also
	 * return null.
	 */
  public String getName() 
  {
  	return this.getSettings().getName() ;
  } // getName() 
  
  // -----------------------------------------------------------------------
  
	/**
	 * Sets the name of the settings object as a whole. 
	 * This might be a simple name or a resource locator or a filename.
	 */
  public void setName( String aName ) 
  {
  	this.getSettings().setName( aName ) ;
  } // setName() 
  
  // -------------------------------------------------------------------------
  	
	/**
	 * Returns the value of keyName in the specified category.
	 * 
	 * @param categoryName The name of the category (null means the default category)
	 * @param keyName The name of a key inside the category
	 * @return The associated value or null if either the category or the key could not be found
	 */
	public String getValueOf( String categoryName, String keyName )  
	{
		return this.getSettings().getValueOf( categoryName, keyName ) ;
	} // getValueOf() 

	// ------------------------------------------------------------------------
	
	/**
	 * Returns the value of keyName in the default category.
	 * 
	 * @param keyName The name of a key inside the default category
	 * @return The associated value or null if the key could not be found
	 */
	public String getValueOf( String keyName ) 
	{
		return this.getSettings().getValueOf( keyName ) ;
	} // getValueOf() 

	// ------------------------------------------------------------------------
	
	/**
	 * Returns an array of all currently known categories.
	 * An empty string is the name for the default category!
	 */
	public String[] getCategoryNames() 
	{
		return this.getSettings().getCategoryNames() ;
	} // getCategoryNames() 

	// ------------------------------------------------------------------------
	
	/**
	 * Returns all currently known key names in the category with the
	 * specified name.
	 * If the category name is null or an empty string, the default
	 * category's keys will be returned.
	 */
	public String[] getKeyNamesOf( String categoryName ) 
	{
		return this.getSettings().getKeyNamesOf( categoryName ) ;
	} // getKeyNamesOf() 
	
	// ------------------------------------------------------------------------
	
	/**
	 * Returns all currently known key names in the default category.
	 */
	public String[] getKeyNamesOfDefaultCategory() 
	{
		return this.getSettings().getKeyNamesOfDefaultCategory() ;
	} // getKeyNamesOfDefaultCategory() 
	
	// ------------------------------------------------------------------------
	
	/**
	 * Gets the defaults that are looked up, if a setting can't be found
	 * in the main settings object.
	 * 
	 * @return A settings object with default values or null
	 */
	public ReadOnlySettings getDefaults() 
	{
		return this.getSettings().getDefaults() ;
	} // getDefaults() 

  // -------------------------------------------------------------------------
	
	/**
	 * Sets defaults that must be looked up, if a setting can't be found
	 * in the main settings object.
	 * 
	 * @param defaults A settings object with default values or null
	 */
	public void setDefaults( ReadOnlySettings defaults ) 
	{
		this.getSettings().setDefaults( defaults ) ;
	} // setDefaults() 

  // -------------------------------------------------------------------------

	/**
	 * Returns the name of the name of the settings where the specified
	 * category and key are found.
	 * That is a lookup in this settings object and then, if not found, 
	 * in its defaults and so on.
	 * 
	 * @param categoryName The name of the category (null means the default category)
	 * @param keyName The name of a key inside the category
	 * @return The name of the settings object or null 
	 */
	public String getSettingsNameOf( String categoryName, String keyName ) 
	{
		return this.getSettings().getSettingsNameOf( categoryName, keyName ) ;
	} // getSettingsNameOf() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns the name of the name of the settings where the specified
	 * key is found in the default category.
	 * That is a lookup in this settings object and then, if not found, 
	 * in its defaults and so on.
	 * 
	 * @param keyName The name of a key inside the default category
	 * @return The name of the settings object or null 
	 */
	public String getSettingsNameOf( String keyName ) 
	{
		return this.getSettings().getSettingsNameOf( keyName ) ;
	} // getSettingsNameOf() 

	// -------------------------------------------------------------------------

	/**
	 * Returns whether or not the trigger is allowed to activate the
	 * trigger client's triggeredBy() method.
	 * <br>
	 * This method will be called, whenever the trigger's timing allows the
	 * trigger to execute the trigger client's service method.
	 * So, eventually the trigger client decides itself, if its service
	 * method will be called this time.<br>
	 * 
	 * @param trigger The trigger that calls this method
	 * @return true, if the service method triggeredBy() can be called
	 * @see org.pf.util.Trigger
	 */
	public boolean canBeTriggeredBy( Trigger trigger )
	{
		boolean canTrigger  ;
		
		canTrigger = ( this.currentFileTimestamp() > this.getFileTimestamp() ) ;
		
		if ( DEBUG ) System.out.println( "Can trigger: " + canTrigger ) ;
		
		return canTrigger ;
	} // canBeTriggeredBy() 
	
  // -------------------------------------------------------------------------

	/**
	 * This method will be called by a trigger whenever its timing says so.
	 * It reloads the settings and then notifies all observers that are 
	 * registered.
	 * 
	 * @param trigger The trigger that calls this method
	 * @return true, if the trigger should continue, otherwise false
	 * @see org.pf.util.Trigger
	 */
	public boolean triggeredBy( Trigger trigger ) 
	{
		if ( DEBUG ) System.out.println( "Triggered" ) ;
		try
		{
			if ( this.loadSettings() )
				this.notifyObservers() ;
		}
		catch (ReloadableSettingsException e)
		{
			// Currently just ignored
		}
		return true ;
	} // triggeredBy() 
  
  // -------------------------------------------------------------------------
  
	/**
	 * Stops the automatic reloading of changes in the underlying file.
	 */
	public void stopReloading() 
	{
		if ( this.getFileCheckTrigger() != null )
		{
			this.getFileCheckTrigger().terminate() ;
			this.setFileCheckTrigger( null ) ;
			if ( DEBUG ) System.out.println( "Reloading trigger stopped." ) ;
		}
	} // stopReloading()

	// -------------------------------------------------------------------------
	
	/**
	 * Starts the automatic reloading of changes in the underlying file.
	 * 
	 * @param checkInterval The interval (in milliseconds) for checking the file's modification timestamp
	 */
	public void startReloading( long checkInterval ) 
	{
		if ( this.getFileCheckTrigger() == null )
		{
			this.startTrigger( checkInterval ) ;
			if ( DEBUG ) System.out.println( "Reloading trigger started." ) ;
		}
	} // stopReloading()

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

	/**
	 * Tries to load the settings and returns true if done so.
	 */	
	protected boolean loadSettings()
		throws ReloadableSettingsException
	{
		ReadOnlySettings someSettings = null ;
		String filename ;

		if ( DEBUG ) System.out.println( "Loading..." ) ;

		filename = this.getFile().getAbsolutePath() ;
		if ( this.fileExists() )
		{				
			someSettings = this.getReader().loadSettings() ;
			if ( someSettings == null )
			{
				// Shouldn't ever happen, because the file exists.
				this.throwException( ReloadableSettingsException.LOADING_FAILED,
														"Loading settings from file '" 
														+ filename + "' failed!" ) ;
			}
			else
			{
				if ( DEBUG ) System.out.println( "Loaded" ) ;
				this.setFileTimestamp( this.currentFileTimestamp() ) ;
				this.replaceSettings( someSettings ) ;
				this.setChanged();
				return true ;
			}
		}
		
		// Only come here, if file doesn't exist
		if ( this.fileMustExist() )
		{
			this.throwException( ReloadableSettingsException.LOADING_FAILED,
												"File '" + filename + "' not found!" ) ;
		}
		
		// Not loaded, but that's ok
		this.setFileTimestamp( UNKNOWN_FILE_TIMESTAMP ) ;
		this.replaceSettings( new SettingsImpl( filename ) ) ;
		return false ; 
	} // loadSettings() 

  // -------------------------------------------------------------------------

	protected void checkAndRememberFile( String filename, boolean mustExist )
		throws ReloadableSettingsException
	{
		File aFile ;
		
		aFile = new File( filename ) ;
		this.setFile( aFile ) ;
		if ( ( ! aFile.exists() ) && mustExist )
		{
			this.throwException( ReloadableSettingsException.FILE_NOT_FOUND,
											"File '" + filename + "' not found!" ) ;
		}
	} // checkAndRememberFile() 

  // -------------------------------------------------------------------------

	protected void startTrigger( long intervalInSeconds )
	{
		Trigger trigger ;
		String triggerName ;
		
		triggerName = "Settings://" + this.getFile().getName() ;
		trigger = Trigger.launch( triggerName, this, intervalInSeconds * 1000 ) ;
		this.setFileCheckTrigger( trigger ) ;
	} // startTrigger() 

  // -------------------------------------------------------------------------

	protected long currentFileTimestamp()
	{
		if ( this.fileExists() )
			return this.getFile().lastModified() ;

		return UNKNOWN_FILE_TIMESTAMP ;
	} // currentFileTimestamp() 

  // -------------------------------------------------------------------------

	protected void replaceSettings( ReadOnlySettings newSettings )
	{
		ReadOnlySettings defaults ;
		
		if ( this.getSettings() != null )
		{
			defaults = this.getSettings().getDefaults() ;
			newSettings.setDefaults( defaults ) ;
		}
		this.setSettings( newSettings ) ;
	} // replaceSettings() 

	// -------------------------------------------------------------------------

	protected boolean fileExists()
	{
		return this.getFile().exists() ;
	} // fileExists() 

	// -------------------------------------------------------------------------

	protected ReloadableSettingsException invalidParameter( String paramName )
	{
		return this.createException( ReloadableSettingsException.INVALID_PARAMETER, 
													"Invalid parameter '" + paramName + "'" ) ;
	} // invalidParameter() 

  // -------------------------------------------------------------------------

	protected void throwException( int code, String msg )
		throws ReloadableSettingsException
	{
		throw this.createException( code, msg ) ;
	} // throwException() 

  // -------------------------------------------------------------------------

	protected ReloadableSettingsException createException( int code, String msg )
	{
		return new ReloadableSettingsException( code, msg ) ;
	} // createException() 

	// -------------------------------------------------------------------------

} // class ReloadableSettings 
