// ===========================================================================
// CONTENT  : CLASS MultiValueProperty
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 11/03/2012
// HISTORY  :
//  11/03/2012  mdu  CREATED
//
// Copyright (c) 2012, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.List;

/**
 * This represents a single property with a name and a list of multiple values.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class MultiValueProperty<ValueType>
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private NamedValue<List<ValueType>> namedValues;
  protected NamedValue<List<ValueType>> getNamedValues() { return namedValues ; }
  protected void setNamedValues( NamedValue<List<ValueType>> newValue ) { namedValues = newValue ; }
  
  private boolean allowDuplicates = false ;
  public boolean getAllowDuplicates() { return allowDuplicates ; }
  public void setAllowDuplicates( boolean newValue ) { allowDuplicates = newValue ; }
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with a name.
   */
  public MultiValueProperty(String name, ValueType... values)
  {
    super() ;
    if (name == null)
		{
			throw new IllegalArgumentException("The name must not be null for a MultiValueProperty.");
		}
    this.setNamedValues(new NamedValue<List<ValueType>>(name, new ArrayList<ValueType>()));
    this.addValues(values);
  } // MultiValueProperty() 
  
  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with a name and define whether or not 
   * duplicate values are allowed.
   */
  public MultiValueProperty(String name, boolean allowDuplicateValues, ValueType... values)
  {
  	this(name, values) ;
  	this.setAllowDuplicates(allowDuplicateValues);
  } // MultiValueProperty() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the name of this property.
   */
  public String getName() 
	{
		return this.getNamedValues().name();
	} // getName() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Add the given values to the list of values. 
   * 
   * @param values The values to add.
   * @return <tt>true</tt> if at least one value was added.
   */
  public boolean addValues(ValueType... values) 
	{
  	boolean addedSomething = false;
  	
  	for (ValueType value : values)
		{
  		addedSomething = this.addValue(value) || addedSomething;
		}
  	return addedSomething;
	} // addValues() 
	
	// -------------------------------------------------------------------------

  /**
   * Remove the given values from the list of values.
   * @param values The values to remove.
   * @return <tt>true</tt> if at least one value was in the list and has been removed.
   */
  public boolean removeValues(ValueType... values) 
	{
  	boolean removedSomething = false;
  	
  	for (ValueType value : values)
		{
  		removedSomething = this.removeValue(value) || removedSomething;
		}
  	return removedSomething;
	} // removeValues() 
	
	// -------------------------------------------------------------------------

  /**
   * Returns true if the given value is already in the list of values.
   */
  public boolean containsValue(ValueType value) 
	{
  	if (value == null)
		{
			return false;
		}
		return this.getNamedValues().value().contains(value);
	} // containsValue() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns a list of the values.
   * This is a copy of the internal list, so it might be modified without
   * side-effects to this property.
   */
  public List<ValueType> getValues() 
	{
		List<ValueType> copy;
		
		copy = new ArrayList<ValueType>(this.getNamedValues().value());
		return copy;
	} // getValues() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns the first value of the internal list of values or null if no values
   * are available.
   */
  public ValueType getFirstValue() 
  {
  	if (this.hasValues())
		{
			return this.getNamedValues().value().get(0);
		} 
  	return null;
  } // getFirstValue() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Remove all values;
   */
  public void clearValues() 
	{
		this.getNamedValues().value().clear();
	} // clearValues() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns true if no value is in the list of values.
   */
  public boolean isEmpty() 
  {
  	return this.getNamedValues().value().isEmpty();
  } // isEmpty() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns true if at least one value is in the list of values.
   */
  public boolean hasValues() 
	{
		return !this.isEmpty();
	} // hasValues() 
	
	// -------------------------------------------------------------------------

  /**
   * Returns the number of values in the list of values,
   */
  public int numberOfValues() 
	{
  	return this.getNamedValues().value().size();		
	} // numberOfValues() 
	
	// -------------------------------------------------------------------------

  /**
   * Returns true if the given object is a MultiValueProperty with the same
   * name (case-sensitive) as this MultiValueProperty instance.
   */
  @Override
  public boolean equals(Object obj) 
	{
		if (obj instanceof MultiValueProperty)
		{
			MultiValueProperty prop = (MultiValueProperty)obj;
			return this.getName().equals(prop.getName());
		}
		return false;
	} // equals() 
	
	// -------------------------------------------------------------------------
  
  @Override
  public int hashCode() 
	{
		return this.getClass().getName().hashCode() ^ this.getName().hashCode();
	} // hashCode() 
	
	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * Add the given value to the list of values. 
   * 
   * @param value The value to add.
   * @return <tt>true</tt> if the value was added.
   */
  protected boolean addValue(ValueType value) 
	{
  	if (value == null)
		{
			return false;
		}
  	if (!this.getAllowDuplicates())
		{
			if (this.containsValue(value))
			{
				return false;
			}
		}
		return this.getNamedValues().value().add(value);
	} // addValue() 
	
	// -------------------------------------------------------------------------

  /**
   * Remove the given value from the list of values.
   * @param value The value to remove.
   * @return <tt>true</tt> if the value was in the list and has been removed.
   */
  protected boolean removeValue(ValueType value) 
	{
  	List<ValueType> values;
  	
  	if (value == null)
		{
			return false;
		}
  	values = this.getNamedValues().value();
  	return values.remove(value);
	} // removeValue() 
	
	// -------------------------------------------------------------------------
  
} // class MultiValueProperty 
