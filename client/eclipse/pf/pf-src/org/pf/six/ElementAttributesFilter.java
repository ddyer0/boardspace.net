// ===========================================================================
// CONTENT  : CLASS ElementAttributesFilter
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 19/01/2014
// HISTORY  :
//  19/01/2014  mdu  CREATED
//
// Copyright (c) 2014, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.six;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.HashMap;
import java.util.Map;

import org.pf.util.CollectionUtil;

/**
 * A filter implementation for Element searches that must match a defined
 * set of attributes.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class ElementAttributesFilter extends AElementFilter
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private final Map<String, String> attributes;

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Creates a new filter with no attributes defined.
   */
  public ElementAttributesFilter()
  {
    this((Map<String, String>)null);
  } // ElementAttributesFilter() 

  // -------------------------------------------------------------------------

  /**
   * Creates a new filter that matches all elements that have all
   * the given attributes with the same value.
   * 
   * @param attributes The attributes an element must match.
   */
  public ElementAttributesFilter(Map<String, String> attributes)
  {
    super();
    if (attributes == null)
    {
      this.attributes = new HashMap();
    }
    else
    {
      this.attributes = attributes;
    }
  } // ElementAttributesFilter() 

  // -------------------------------------------------------------------------
  
  /**
   * Creates a new filter that matches all elements that have all
   * the given attributes with the same value.
   * 
   * @param keyValuePairs The attributes an element must match.
   *   Each string must contain a key and value separated by ":" or "=".
   *   Example: new ElementAttributesFilter("a=1", "b=2", "c:3")
   */
  public ElementAttributesFilter(String... keyValuePairs)
  {
    this();
    CollectionUtil.current().addAll(this.getAttributes(), keyValuePairs);
  } // ElementAttributesFilter() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected Map<String, String> getAttributes()
  {
    return attributes;
  } // getAttributes() 

  // -------------------------------------------------------------------------
  
  protected boolean matchesElement(Element element)
  {
    String key;
    String value;
    String expectedValue;

    for (Map.Entry<String, String> entry : this.getAttributes().entrySet())
    {
      key = entry.getKey();
      value = element.getAttribute(key);
      if (value == null)
      {
        return false;
      }
      expectedValue = entry.getValue();
      if (!value.equals(expectedValue))
      {
        return false;
      }
    }
    return true;
  } // elementMatchesAttributes() 

  // -------------------------------------------------------------------------

} // class ElementAttributesFilter 
