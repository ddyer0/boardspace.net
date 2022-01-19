// ===========================================================================
// CONTENT  : CLASS ElementAndFilter
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 19/01/2014
// HISTORY  :
//  19/01/2014  mdu  CREATED
//
// Copyright (c) 2014, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.six;

/**
 * A filter that can be used to combine multiple IElementFilters with the
 * boolean AND operator.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class ElementAndFilter extends ChainedElementFilter
{
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  public ElementAndFilter(IElementFilter... filter)
  {
    super(filter);
  } // ElementAndFilter() 

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  @Override
  protected boolean matchesElement(Element element)
  {
    for (IElementFilter filter : this.getFilterList())
    {
      if (!filter.matches(element))
      {
        return false;
      }
    }
    return true;
  } // matchesElement()
  
  // -------------------------------------------------------------------------
  
} // class ElementAndFilter 
