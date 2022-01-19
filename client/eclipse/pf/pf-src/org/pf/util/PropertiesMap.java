// ===========================================================================
// CONTENT  : CLASS PropertiesMap
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 19/01/2014
// HISTORY  :
//  19/01/2014  mdu  CREATED
//
// Copyright (c) 2014, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.util;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * This is a Map wrapper for a properties object so that it can be used 
 * as Map<String, String>, too.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class PropertiesMap implements Map<String, String>
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private final Properties internalProperties;

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  public PropertiesMap()
  {
    this(null);
  } // PropertiesMap() 
  
  // -------------------------------------------------------------------------

  public PropertiesMap(Properties properties)
  {
    super();
    if (properties == null)
    {
      this.internalProperties = new Properties();
    }
    else
    {
      this.internalProperties = properties;
    }
  } // PropertiesMap() 
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the data as Properties object.
   * Modifying the returned properties does have no impact on this object.
   */
  public Properties getProperties()
  {
    return (Properties)this.getInternalProperties().clone();
  } // getProperties() 
  
  // -------------------------------------------------------------------------

  public int size()
  {
    return this.getInternalProperties().size();
  } // size()
  
  // -------------------------------------------------------------------------

  public boolean isEmpty()
  {
    return this.getInternalProperties().isEmpty();
  } // isEmpty()
  
  // -------------------------------------------------------------------------

  public boolean containsKey(Object key)
  {
    if (key instanceof String)
    {      
      return this.getInternalProperties().containsKey(key);
    }
    return false;
  } // containsKey()
  
  // -------------------------------------------------------------------------

  public boolean containsValue(Object value)
  {
    if (value instanceof String)
    {      
      return this.getInternalProperties().containsValue(value);
    }
    return false;
  } // containsValue()
  
  // -------------------------------------------------------------------------

  public String get(Object key)
  {
    if (key instanceof String)
    {      
      return this.getInternalProperties().getProperty((String)key);
    }
    return null;
  } // get()
  
  // -------------------------------------------------------------------------

  public String put(String key, String value)
  {
    String oldValue;
    
    oldValue = this.getInternalProperties().getProperty(key);
    this.getInternalProperties().setProperty(key, value);
    return oldValue;
  } // put()
  
  // -------------------------------------------------------------------------

  public String remove(Object key)
  {
    if (key instanceof String)
    {      
      return (String)this.getInternalProperties().remove(key);
    }
    return null;
  } // remove()

  public void putAll(Map<? extends String, ? extends String> map)
  {
    this.getInternalProperties().putAll(map);
  } // putAll()
  
  // -------------------------------------------------------------------------

  public void clear()
  {
    this.getInternalProperties().clear();
  } // clear()
  
  // -------------------------------------------------------------------------

  public Set<String> keySet()
  {
    Set<String> result;

    result = new HashSet<String>(this.getInternalProperties().size());
    for (Object object : this.getInternalProperties().keySet())
    {
      if (object instanceof String)
      {
        result.add((String)object);
      }
    }
    return result;
  } // keySet()
  
  // -------------------------------------------------------------------------

  public Collection<String> values()
  {
    Collection<String> result;
    
    result = new ArrayList<String>(this.getInternalProperties().size());
    for (Object object : this.getInternalProperties().values())
    {
      if (object instanceof String)
      {
        result.add((String)object);
      }
    }
    return result;
  } // values()
  
  // -------------------------------------------------------------------------

  public Set<Map.Entry<String, String>> entrySet()
  {
    Set<Map.Entry<String, String>> result;
    
    result = new HashSet(this.getInternalProperties().size());
    for (Map.Entry entry : this.getInternalProperties().entrySet())
    {
      if ((entry.getKey() instanceof String) && (entry.getValue() instanceof String))
      {
        result.add(new MapEntry<String, String>((String)entry.getKey(), (String)entry.getValue()));
      }
    }
    return result;
  } // entrySet()
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected Properties getInternalProperties()
  {
    return internalProperties;
  } // getInternalProperties() 

  // -------------------------------------------------------------------------
  
} // class PropertiesMap 
