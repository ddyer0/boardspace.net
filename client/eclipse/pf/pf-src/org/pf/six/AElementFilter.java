// ===========================================================================
// CONTENT  : ABSTRACT CLASS AElementFilter
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

/**
 * Abstract base class for IElementFilter implementors. 
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
abstract public class AElementFilter implements IElementFilter
{
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  public AElementFilter()
  {
    super();
  } // AElementFilter()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  public boolean matches(Element object)
  {
    if (object instanceof Element)
    {
      return this.matchesElement(object);
    }
    return false;
  } // matches() 

  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * Returns whether or not the given element matches this filter.
   *   
   * @param element The element to check against the filter.
   */
  abstract protected boolean matchesElement(Element element);
  
} // class AElementFilter
