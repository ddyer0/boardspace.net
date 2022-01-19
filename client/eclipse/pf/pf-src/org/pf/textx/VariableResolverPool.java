// ===========================================================================
// CONTENT  : CLASS VariableResolverPool
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 02/10/1999
// HISTORY  :
//  02/10/1999 duma  CREATED
//	25/01/2000	duma	moved		-> from package 'com.mdcs.text'
//
// Copyright (c) 1999-2000, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.textx;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Instances of this class can pool several <i>VariableResolver</i>.
 * It provides a single entry point to them by implementing the
 * interface <i>VariableResolver</i> itself and redirecting it to one
 * of the pooled variable resolver.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class VariableResolverPool implements VariableResolver
{ 
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private Map pool = null ;
  protected Map getPool() { return pool ; }
  protected void setPool( Map aValue ) { pool = aValue ; }

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
  public VariableResolverPool()
  {
  	this.setPool( this.createNewPool() ) ;
  } // VariableResolverPool()

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with the given map to hold the pooled objects.    <br>
	 * 
	 * @param aMap The instance of a <i>Map</i> interface compliant class.
   */
  public VariableResolverPool( Map aMap )
  {
  	this.setPool( aMap ) ;
  } // VariableResolverPool()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
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
		Object value 								= null ;
		VariableResolver resolver		= null ;
		
		resolver = this.findResolverFor( varName ) ;
		if ( resolver == null )
		{
			throw ( new UnknownVariableException( varName ) ) ;
		}
		else
		{
			value = resolver.getValue( varName ) ;
		}
		return value ;
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
		VariableResolver resolver			= null ;
		
		resolver = this.findResolverFor( varName ) ;
		return ( resolver != null ) ;
	} // isKnownVariable()

  // -------------------------------------------------------------------------

	/**
	 * Returns all variable name the resolver currently knows
	 */
	public Set knownVariableNames() 
	{
		Set names = new HashSet() ;
		VariableResolver resolver			= null ;
		Iterator iterator							= null ;
		
		iterator = this.getPool().values().iterator() ;
		while ( iterator.hasNext() ) 
		{
			resolver = (VariableResolver)iterator.next() ;
			names.addAll( resolver.knownVariableNames() ) ;
		} // while()
		return names ;
	} // knownVariableNames() 

  // -------------------------------------------------------------------------

	/**
	 * Add the given resolver under the given name to the pool.   
	 *
	 * @param resolverName The unique name of the resolver in the pool.
	 * @param resolver The variable resolver to add.
	 */
	public void add( String resolverName, VariableResolver resolver )
	{
		if ( ( resolverName != null ) && ( resolver != null ) )
		{
			this.getPool().put( resolverName, resolver ) ;
		}
	} // add()

  // -------------------------------------------------------------------------

	/**
	 * Replace the resolver under the given name to the pool.   
	 *
	 * @param resolverName The unique name of the resolver in the pool.
	 * @param resolver The replacing variable resolver.
	 */
	public void replace( String resolverName, VariableResolver resolver )
	{
		this.remove( resolverName ) ;
		this.add( resolverName, resolver ) ;
	} // replace()

  // -------------------------------------------------------------------------

	/**
	 * Remove the resolver with the given name from the pool.   <br>
	 * If there's no entry with such a name, the call is ignored.
	 *
	 * @param resolverName The unique name of the resolver in the pool.
	 */
	public void remove( String resolverName )
	{
		if ( resolverName != null )
		{
			this.getPool().remove( resolverName ) ;
		}
	} // remove()

  // -------------------------------------------------------------------------

	/**
	 * Return the resolver that was registered under the specified name 
	 * in this pool.
	 *
	 * @param resolverName The unique name of the resolver in the pool.
	 */
	public VariableResolver resolverNamed( String resolverName )
	{
		return (VariableResolver)this.getPool().get( resolverName ) ;
	} // resolverNamed()

  // -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns the resolver from the pool, that knows the given variable name.   
	 *
	 * @param varName The name of the variable to look for
	 */
	protected VariableResolver findResolverFor( String varName )
	{
		VariableResolver resolver			= null ;
		VariableResolver result 			= null ;
		Iterator iterator							= null ;
		Map.Entry entry								= null ;
		
		iterator = this.getPool().entrySet().iterator() ;
		while ( ( result == null ) && ( iterator.hasNext() ) )
		{
			entry = (Map.Entry)iterator.next() ;
			resolver = (VariableResolver)entry.getValue() ;
			if ( resolver.isKnownVariable( varName ) )
			{
				result = resolver ;
			}
		} // while()
		return result ;
	} // findResolverFor()

  // -------------------------------------------------------------------------

  /**
   * Returns a new instance for the pool.    <br>
   * Subclasses may override this method to support a different mapping class.
   *  
   * @return A new instance of a <i>Map</i> compliant class.
   */
	protected Map createNewPool()
	{
		return ( new Hashtable() ) ;
	} // createNewPool()

  // -------------------------------------------------------------------------
  
} // class VariableResolverPool