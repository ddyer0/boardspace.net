// ===========================================================================
// CONTENT  : CLASS CaseInsensitiveMultiValueProperties
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A map which holds named multi-value properties and allows case-insensitive
 * access to them by their names.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class CaseInsensitiveMultiValueProperties<ValueType>
{
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private Map<String, MultiValueProperty<ValueType>> properties = null ;
  protected Map<String, MultiValueProperty<ValueType>> getProperties() { return properties ; }
  protected void setProperties( Map<String, MultiValueProperty<ValueType>> newValue ) { properties = newValue ; }
  
  private boolean allowDuplicates = false ;
  public boolean getAllowDuplicates() { return allowDuplicates ; }
  protected void setAllowDuplicates( boolean newValue ) { allowDuplicates = newValue ; }
	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
  /**
   * Initialize the new instance with default values (allowDuplicateValues=false).
   */
  public CaseInsensitiveMultiValueProperties()
  {
    super() ;
    this.setProperties(this.createEmptyMap());
  } // CaseInsensitiveMultiValueProperties() 

  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with a flag that specifies whether or not to
   * allow duplicate values. If set to false duplicate values will be silently
   * skipped when tried to be added.
   */
  public CaseInsensitiveMultiValueProperties(boolean allowDuplicateValues)
  {
  	this() ;
  	this.setAllowDuplicates(allowDuplicateValues);
  	this.setProperties(this.createEmptyMap());
  } // CaseInsensitiveMultiValueProperties() 
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the names of all properties held in this container. 
   */
  public Set<String> getNames() 
	{
		return this.getProperties().keySet();
	} // getNames()
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns the property with the given name or null if not found.
   */
  public MultiValueProperty<ValueType> getProperty(String name) 
	{
		return this.getProperties().get(name);
	} // getProperty() 
	
	// -------------------------------------------------------------------------

  /**
   * Returns the values of the specified property or null if not found.
   * @param name The propertie's name (must not be null).
   */
  public List<ValueType> getValues(String name) 
	{
  	MultiValueProperty<ValueType> property;
  	
		property = this.getProperty(name);
		if (property == null)
		{
			return null;
		}
  	return property.getValues();		
	} // getValues() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Add one or more values to the property with the given name.
   * Not yet existing properties will be created automatically. This can also be
   * used to create a new property with an empty value list (e.g. addValues("prop")).
   * 
   * @param name The name of the property (must not be null)
   * @param values The values to be added - may be omitted completely (must not be null)
   */
  public void addValues(String name, ValueType... values) 
	{
  	MultiValueProperty<ValueType> property;
  	
  	property = this.getOrCreate(name);
  	property.addValues(values);
	} // addValues() 
	
	// -------------------------------------------------------------------------
  
  public boolean contains(String name) 
	{
		return this.getProperties().containsKey(name);
	} // contains() 
	
	// -------------------------------------------------------------------------
  
  public void remove(String... names) 
	{
  	for (String name : names)
		{
  		this.getProperties().remove(name);
		}
	} // remove() 
	
	// -------------------------------------------------------------------------
  
  public void removeValues(String name, ValueType... values) 
  {
  	MultiValueProperty<ValueType> property;
  	
  	property = this.getProperty(name);
  	if (property != null)
  	{
  		property.removeValues(values);
  	}
  } // removeValues() 
  
  // -------------------------------------------------------------------------
  
  public void clearValues(String name) 
	{
  	MultiValueProperty<ValueType> property;
  	
  	property = this.getProperty(name);
  	if (property != null)
  	{
  		property.clearValues();
  	}
	} // clearValues() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Removes all hold data.
   */
  public void clear() 
	{
		this.getProperties().clear();
	} // clear() 
	
	// -------------------------------------------------------------------------
  
  public boolean isEmpty() 
	{
		return this.getProperties().isEmpty();
	} // isEmpty() 
	
	// -------------------------------------------------------------------------
  
  public int size() 
	{
		return this.getProperties().size();
	} // size() 
	
	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected MultiValueProperty<ValueType> getOrCreate(String name) 
	{
  	MultiValueProperty<ValueType> property;
  	
  	property = this.getProperty(name);
  	if (property == null)
  	{
  		property = new MultiValueProperty<ValueType>(name, this.getAllowDuplicates());
  		this.getProperties().put(name, property);
  	}
  	return property;
	} // getOrCreate() 
	
	// -------------------------------------------------------------------------
  
  protected Map<String, MultiValueProperty<ValueType>> createEmptyMap()
  {
  	return new TreeMap<String, MultiValueProperty<ValueType>>(String.CASE_INSENSITIVE_ORDER);
  } // createEmptyMap() 
  
  // -------------------------------------------------------------------------
  
} // class CaseInsensitiveMultiValueProperties 
