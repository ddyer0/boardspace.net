// ===========================================================================
// CONTENT  : INTERFACE IElementFilter
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
import org.pf.bif.filter.IObjectFilter;

/**
 * Filter definition for {@link Element} objects.
 * Can be used with find methods of XmlDocument and Element. 
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public interface IElementFilter extends IObjectFilter<Element>
{
  /**
   * A predefined filter that matches all elements (except null).
   */
  public final IElementFilter ALL = new IElementFilter()
  {
    public boolean matches(Element object)
    {
      return (object instanceof Element);
    }
  };

  /**
   * A predefined filter that matches no elements at all.
   */
  public final IElementFilter NONE = new IElementFilter()
  {
    public boolean matches(Element object)
    {
      return false;
    }
  };

} // interface IElementFilter