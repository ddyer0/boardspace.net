// ===========================================================================
// CONTENT  : CLASS GlobalLocalVariables
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.4 - 11/07/2002
// HISTORY  :
//  24/03/2002  duma  CREATED
//	21/06/2002	duma	changed	->	Ensured that local variables are taken before global
//	01/07/2002	duma	added		->	Allow to set a function resolver
//	11/07/2002	duma	added		->	setValueFor(), setLocalObject(),getLocalObject()
//
// Copyright (c) 2002, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.textx;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Contains global and local variable settings.
 * It insures that local variables are used before global variables if they
 * have the same name.
 *
 * @author Manfred Duchrow
 * @version 1.4
 */
public class GlobalLocalVariables implements VariableContainer
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // ========================================================================= 
  private VariableContainer globalVars = null ;
  protected VariableContainer getGlobalVars() { return globalVars ; }
  protected void setGlobalVars( VariableContainer newValue ) { globalVars = newValue ; }
  
  private VariableContainer localVars = null ;
  protected VariableContainer getLocalVars() { return localVars ; }
  protected void setLocalVars( VariableContainer newValue ) { localVars = newValue ; }
      
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public GlobalLocalVariables()
  {
    super() ;
    this.setGlobalVars( new BasicVariableContainer() ) ;
    this.setLocalVars( new BasicVariableContainer() ) ;
  } // GlobalLocalVariables()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

	/**
	 * Initialize the local variable container with an new container.
	 * 
	 * @param varContainer The new container (must not be null)
	 */
	public void newLocalContext( VariableContainer varContainer )
	{
		if ( varContainer != null )
			this.setLocalVars( varContainer ) ;
	} // newLocalContext()

  // -------------------------------------------------------------------------

	/**
	 * Initialize the global variable container with an new container.
	 * 
	 * @param varContainer The new container (must not be null)
	 */
	public void newGlobalContext( VariableContainer varContainer )
	{
		if ( varContainer != null )
			this.setGlobalVars( varContainer ) ;
	} // newGlobalContext()

  // -------------------------------------------------------------------------

	/**
	 * Set the variable with the given name to the specified value.   <br>
	 * The programmer must take care that there are no name conflicts
	 * between global and local variables.
	 * If the variable already exists, the value will be changed, otherwise
	 * the variable will be created.
	 * 
	 * @param varName The name of the variable
	 * @param value The value to be assigned to the variable
	 * @param global Defines whether or not the variable is global
	 */
	public void set( String varName, String value, boolean global )
	{
		if ( global )
			this.setGlobal( varName, value ) ;
		else
			this.setLocal( varName, value ) ;
	} // set()

  // -------------------------------------------------------------------------

	/**
	 * Set the local variable with the given name to the specified value.   <br>
	 * The programmer must take care that there are no name conflicts
	 * between global and local variables.
	 * If the local variable already exists, the value will be changed, otherwise
	 * the variable will be created.
	 * 
	 * @param varName The name of a local variable
	 * @param value The value to be assigned to the variable
	 */
	public void setLocal( String varName, String value )
	{
		this.localVariables().setValue( varName, value) ;
	} // setLocal()

  // -------------------------------------------------------------------------

	/**
	 * Set the local variable with the given name to the specified value.   <br>
	 * If the local variable already exists, the value will be changed, otherwise
	 * the variable will be created.
	 * 
	 * @param varName The name of a local variable
	 * @param value The value to be assigned to the variable
	 */
	public void setLocalObject( String varName, Object value )
	{
		this.localVariables().setValueFor( varName, value) ;
	} // setLocalObject()

  // -------------------------------------------------------------------------

	/**
	 * Set the global variable with the given name to the specified value.   <br>
	 * The programmer must take care that there are no name conflicts
	 * between global and local variables.
	 * If the global variable already exists, the value will be changed, otherwise
	 * the variable will be created.
	 * 
	 * @param varName The name of a local variable
	 * @param value The value to be assigned to the variable
	 */
	public void setGlobal( String varName, String value )
	{
		this.globalVariables().setValue( varName, value) ;
	} // setGlobal()

  // -------------------------------------------------------------------------

	/**
	 * Set the global variable with the given name to the specified value.   <br>
	 * If the global variable already exists, the value will be changed, otherwise
	 * the variable will be created.
	 * 
	 * @param varName The name of a local variable
	 * @param value The value to be assigned to the variable
	 */
	public void setGlobalObject( String varName, Object value )
	{
		this.globalVariables().setValueFor( varName, value) ;
	} // setGlobalObject()

  // -------------------------------------------------------------------------

	/**
	 * Adds all given name/value pairs from the given properties
	 * to the global variables.
	 */
	public void setGlobalFrom( Properties vars )
	{
		this.setVariablesFrom( vars, true ) ;
	} // setGlobalFrom()

  // -------------------------------------------------------------------------

	/**
	 * Adds all given name/value pairs from the given properties
	 * to the local variables.
	 */
	public void setLocalFrom( Properties vars )
	{
		this.setVariablesFrom( vars, false ) ;
	} // setLocalFrom()

  // -------------------------------------------------------------------------

  // -------------------------------------------------------------------------
  // VariableContainer Interface Implementation
  // -------------------------------------------------------------------------

	/**
	 * Set the value of the specified variable.
	 * Here it will always set a <b>local</b> variable !
	 * 
	 * @param varName The name under which the value has to be stored
	 * @param value The value to store
	 */
	public void setValue( String varName, String value ) 
	{
		this.setLocal( varName, value ) ;
	} // setValue()

  // -------------------------------------------------------------------------

  /**
   * Sets the value of the variable with the given name.
	 * Here it will always set a <b>local</b> variable !
   *
   * @param varName The case sensitive name of the variable. Must not be null !
   * @param value The new value of the variable. Must not be null !
   */
  public void setValueFor( String varName, Object value ) 
  {
  	this.setLocalObject( varName, value ) ;
  } // setValueFor()

  // -------------------------------------------------------------------------
  
	/**
	 * Remove the variable with the specified name.
	 * Will be removed from global and local variables if existing there.
	 * 
	 * @param varName The name of the variable to be removed
	 */
	public void removeVariable( String varName ) 
	{
		this.getLocalVars().removeVariable( varName ) ;
		this.getGlobalVars().removeVariable( varName ) ;
	} // removeVariable() 

  // -------------------------------------------------------------------------
  
	/**
	 * Returns all variable name the resolver currently knows
	 */
	public Set knownVariableNames() 
	{
		Set names = new HashSet() ;

		names.addAll( this.getGlobalVars().knownVariableNames() ) ;
		names.addAll( this.getLocalVars().knownVariableNames() ) ;
		return names ;
	} // knownVariableNames() 

  // -------------------------------------------------------------------------

  /**
   * Returns the value for the variable with the given name.
   *
   * @param varName The case sensitive name of the variable.
   * @return The value associated to the given variable (even null is a valid value)
   * @throws UnknownVariableException The receiver is not knowing the variable. 
   */
  public Object getValue( String varName )
                throws UnknownVariableException
  {
  	if ( this.localVariables().isKnownVariable( varName ) )
  		return this.localVariables().getValue( varName ) ;

  	return this.globalVariables().getValue( varName ) ;
  } // getValueFor()
  
  // -------------------------------------------------------------------------

  /**
   * Returns if the variable with the given name can be resolved by the receiver.
   *
   * @param varName The case sensitive name of the variable.
   * @return Whether the variable with the given name is known or not.
   */
  public boolean isKnownVariable( String varName ) 
  {
  	if ( this.localVariables().isKnownVariable( varName ) )
  		return true ;

  	if ( this.globalVariables().isKnownVariable( varName ) )
  		return true ;

		return false ;
  } // isKnownVariable()
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

	/**
	 * Adds all given name/value pairs from the given properties
	 * to the global or local variables as specified.
	 */
	protected void setVariablesFrom( Properties vars, boolean global )
	{
		Enumeration keys ;
		String key ;

		keys = vars.keys() ;
		while ( keys.hasMoreElements() )
		{
			key = (String)keys.nextElement() ;
			this.set( key, vars.getProperty( key ), global ) ;
		}
	} // setVariablesFrom()

  // -------------------------------------------------------------------------

	protected VariableContainer localVariables()
	{
		return this.getLocalVars() ;
	} // localVariables()

  // -------------------------------------------------------------------------

	protected VariableContainer globalVariables()
	{
		return this.getGlobalVars() ;
	} // globalVariables()

  // -------------------------------------------------------------------------

} // class GlobalLocalVariables