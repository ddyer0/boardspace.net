// ===========================================================================
// CONTENT  : CLASS AbstractAbstractObjectSpy
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 16/04/2006
// HISTORY  :
//  14/11/1999  duma  CREATED
//	09/04/2004	duma	added		-->	setElementValue()
//	16/04/2006	mdu		changed	--> get init modifier state from Preferences
//
// Copyright (c) 1999-2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * An instance of this class is a wrapper for one inspected object.
 * It provides the API an inspector is using, to display internal information
 * about the inspected object.
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
abstract public class AbstractObjectSpy extends Spy
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String name = "this" ;
  public String getName() { return name ; }
  public void setName( String newValue ) { name = newValue ; }

	private Object object = null ;
	public Object getObject() { return object ; }
	protected void setObject( Object newValue ) { object = newValue ; }

	private List elementHolders = null ;
	protected List getElementHolders() { return elementHolders ; }
	protected void setElementHolders( List newValue ) { elementHolders = newValue ; }

  private ElementFilter elementFilter = null ;
  public ElementFilter getElementFilter() { return elementFilter ; }
  public void setElementFilter( ElementFilter aValue ) { elementFilter = aValue ; }

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public AbstractObjectSpy( Object obj )
  	throws SecurityException
  {
  	super() ;
  	this.setElementFilter( Preferences.instance().getInitialElementFilter() ) ;
  	this.setObject( obj ) ;
  	this.initializeElements() ;
  } // AbstractObjectSpy()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  /**
   * Returns the type (class) of the underlying object.
   */
	public Class getType()
	{
		Class type		= null ;

		if ( this.getObject() == null )
		{
			type = Void.TYPE ;
		}
		else
		{
			type = this.getObject().getClass() ;
		}
		return type ;
	} // getType()

	// --------------------------------------------------------------------------

	/**
	 * Returns the value of the underlying object.
	 */
	public Object getValue()
		throws Exception
	{
		return this.getObject() ;
	} // getValue()

	// --------------------------------------------------------------------------

	/**
	 * Returns the bit mask indicating the modifiers of the class declaration.
	 *
	 * @see java.lang.reflect.Modifier
	 */
	public int getModifiers()
	{
		return ( this.getType().getModifiers() ) ;
	} // getModifiers()

	// --------------------------------------------------------------------------

	/**
	 * Returns all elements, i.e. including inherited elements as List.   <br>
	 * This method is using the current filter, to filter out elements
	 * with specific modifiers.
	 */
	public List getElements()
	{
		List result					= null ;
		Iterator iterator		= null ;
		Spy element					= null ;

		result = new Vector() ;
		iterator = this.getElementHolders().iterator() ;
		while ( iterator.hasNext() )
		{
			element = (Spy)iterator.next() ;
			if ( ! ( this.getElementFilter().matchesAny( element.getModifiers() ) ) )
			{
				result.add( element ) ;
			}
		}

		return ( result ) ;
	} // getElements()

	// --------------------------------------------------------------------------

	/**
	 * Returns the number of elements ignoring all filter criteria.
	 */
	public int getFullElementCount()
	{
		return this.getElementHolders().size() ;
	} // getFullElementCount()

	// --------------------------------------------------------------------------

  /**
   * Returns the element with the given name or null if no
   * such element can be found in the receiver.
   */
	public ElementSpy getElementNamed( String elementName )
	{
		ElementSpy element		= null ;
		Iterator iterator			= null ;
		
		iterator = this.getElements().iterator() ;
		while ( iterator.hasNext() )
		{
			element = (ElementSpy)iterator.next() ;
			if ( element.getName().equals( elementName ) )
				return element ;
		}
		
		return null ;
	} // getElementNamed()

	// --------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

	abstract protected void addAllElements() ;

	// --------------------------------------------------------------------------

	protected void initializeElements()
		throws SecurityException
	{
		this.setElementHolders( this.createNewElementList() ) ;
		if ( this.getObject() != null )
		{
			this.addAllElements() ;
		}
	} // initializeElements()

	// --------------------------------------------------------------------------

  protected List createNewElementList()
	{
		return ( new Vector() ) ;
	} // createNewElementList()

	// --------------------------------------------------------------------------

	/**
	 * Returns whether or not the elements
	 * of the underlying object can be sorted.
	 */
	protected boolean canBeSorted()
	{
		return true ;
	} // canBeSorted()	
	
	// --------------------------------------------------------------------------

	/**
	 * Sorts all elements of the underlying object in ascending order.
	 */
	public boolean sortElements()
	{
		Object[] array = null ;

		if ( this.canBeSorted() )
		{
			array = this.getElementHolders().toArray() ;
			Arrays.sort(array) ;
			this.setElementHolders( Arrays.asList(array) ) ;
			return true ;
		}
		else
		{
			return false ;
		}
	} // sortElements()
	
	// --------------------------------------------------------------------------

	/**
	 * Returns true if this object allows the modification of its elements.
	 * Here the methods alwys returns false.
	 * Subclasses may override it to return true.
	 * Such a subclass must override setElementValue() as well.
	 */
	protected boolean allowsElementModification() 
	{
		return false ;
	} // allowsElementModification()

	// -------------------------------------------------------------------------
	
	/**
	 * Sets the value of the specified element to the given value.
	 * This abstract implementation throws a not supported expection.
	 * Subclasses may override to do the modification.
	 * Such a subclass must override allowsElementModification() as well.
	 */
	protected void setElementValue( ElementSpy element, Object value )
		throws Exception
	{
		throw new Exception( "Modification of element '" 
												+ ( element == null ? "" : element.getName() )				
												+ "' not supported!" ) ;
	} // setElementValue()

	// -------------------------------------------------------------------------
	
} // class AbstractObjectSpy