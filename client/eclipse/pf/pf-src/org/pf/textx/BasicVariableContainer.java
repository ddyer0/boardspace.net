// ===========================================================================
// CONTENT  : CLASS BasicVariableContainer
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.3 - 06/02/2004
// HISTORY  :
//  31/07/1999 	duma  CREATED
//	25/01/2000	duma	moved		-> 	from package 'com.mdcs.text'
//	25/01/2000	duma	added		->	setValueFor( String, boolean )
//  07/02/2000  duma  chnaged ->  text for UnknownVariableException
//	06/02/2004	duma	added		->	addFrom()
//
// Copyright (c) 1999-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.textx;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * This is a basic implementation of the interface <i>VariableResolver</i>.
 * It provides a container for a simple mapping of variable name to their
 * corresponding values.
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
public class BasicVariableContainer implements VariableContainer
{ 
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private Map variableMapping = null ;
  protected Map getVariableMapping() { return variableMapping ; }
  protected void setVariableMapping( Map aValue ) { variableMapping = aValue ; }

  // =========================================================================
  // CLASS METHODS
  // =========================================================================
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.    <br>
   * Subclasses that override this constructor must call <i>super()</i> first, 
   * before doing anything else.
   */
  public BasicVariableContainer()
  {
  	this.setVariableMapping( this.createNewMapping() ) ;
  } // BasicVariableContainer() 

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with the given mapping.    <br>
	 * 
	 * @param mapping The instance of a <i>Map</i> interface compliant class.
   */
  public BasicVariableContainer( Map mapping )
  {
  	this.setVariableMapping( mapping ) ;
  } // BasicVariableContainer() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

	/**
	 * Returns all variable name the resolver currently knows
	 */
	public Set knownVariableNames() 
	{
		return this.getVariableMapping().keySet() ;
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
		Object value = null ;

		value = this.getVariableMapping().get( varName ) ;
		if ( value == null )
		{
			throw ( new UnknownVariableException( varName ) ) ;
		}
		return value ;
	} // getValue() 
  
  // -------------------------------------------------------------------------

  /**
   * Returns if the variable with the given name can be resolved by the receiver.
   *
   * @param varName The case sensitive name of the variable.
   * @return Whether the variable with the given name is known or not.
   */
  public boolean isKnownVariable( String varName )
	{
		Object value = null ;
		
		value = this.getVariableMapping().get( varName ) ;
		return ( value != null ) ;
	} // isKnownVariable() 

  // -------------------------------------------------------------------------

  /**
   * Sets the value of the variable with the given name.
   *
   * @param varName The case sensitive name of the variable. Must not be null !
   * @param value The new value of the variable. Must not be null !
   */
  public void setValueFor( String varName, Object value )
	{
		if ( ( varName != null ) && ( value != null ) )
		{
			this.getVariableMapping().put( varName, value ) ;
		}
	} // setValueFor() 

  // -------------------------------------------------------------------------

  /**
   * Sets the boolean value of the variable with the given name.
   *
   * @param varName The case sensitive name of the variable. Must not be null !
   * @param value The boolean value
   */
  public void setValueFor( String varName, boolean value )
	{
		if ( varName != null )
		{
			this.getVariableMapping().put( varName, value ? Boolean.TRUE : Boolean.FALSE ) ;
		}
	} // setValueFor() 

  // -------------------------------------------------------------------------
  
  /**
   * Sets the value of the variable with the given name.
   *
   * @param varName The case sensitive name of the variable. Must not be null !
   * @param value The new value of the variable. Must not be null !
   */
  public void setValue( String varName, String value )
	{
		if ( ( varName != null ) && ( value != null ) )
		{
			this.getVariableMapping().put( varName, value ) ;
		}
	} // setValue() 

  // -------------------------------------------------------------------------

	/**
	 * Remove the variable with the specified name.
	 * 
	 * @param varName The name of the variable to be removed
	 */
	public void removeVariable( String varName )
	{
		if ( varName != null )
		{
			this.getVariableMapping().remove( varName ) ;
		}		
	} // removeVariable() 

  // -------------------------------------------------------------------------
  
  /**
   * Adds all key value pairs from the given map to the variables, if the
   * key is a String.
   */
	public void addFrom( Map vars )
	{
		Iterator entries ;
		Map.Entry entry ;

		if ( vars == null )
		{
			return ;
		}
		entries = vars.entrySet().iterator() ;
		while ( entries.hasNext() )
		{
			entry = (Entry) entries.next() ;
			if ( entry.getKey() instanceof String )
			{
				this.setValueFor( (String)entry.getKey(), entry.getValue() ) ;
			}
		}
	} // addFrom() 

	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * Returns a new instance for the mapping.    <br>
   * Subclasses may override this method to support a different mapping class.
   *  
   * @return A new instance of a <i>Map</i> compliant class.
   */
	protected Map createNewMapping()
	{
		return ( new HashMap() ) ;
	} // createNewMapping() 

  // -------------------------------------------------------------------------
  
} // class BasicVariableContainer 
