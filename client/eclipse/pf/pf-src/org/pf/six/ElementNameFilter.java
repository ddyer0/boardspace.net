// ===========================================================================
// CONTENT  : CLASS ElementNameFilter
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
import org.pf.text.StringFilter;
import org.pf.text.StringPattern;

/**
 * Can be used to find elements that match a particular element name or name pattern.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class ElementNameFilter extends AElementFilter
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private StringFilter tagNameFilter;

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  public ElementNameFilter(StringFilter tagNameFilter)
  {
    super();
    this.tagNameFilter = tagNameFilter;
  } // ElementNameFilter() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Creates a filter for the given name or name pattern (if "?" or "*" used).
   * 
   * @param tagName The name or name pattern (must not be null).
   */
  public ElementNameFilter(String tagName)
  {
    this(new StringPattern(tagName));
  } // ElementNameFilter() 
  
  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected boolean matchesElement(Element element)
  {
    if (!this.getTagNameFilter().matches(element.getName()))
    {
      return false;
    }
    return true;
  } // matchesElement()
  
  // -------------------------------------------------------------------------
  
  protected StringFilter getTagNameFilter()
  {
    return tagNameFilter;
  } // getTagNameFilter()
  
  // -------------------------------------------------------------------------
  
} // class ElementNameFilter 
