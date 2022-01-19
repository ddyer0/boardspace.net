// ===========================================================================
// CONTENT  : CLASS ChainedElementFilter
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
import java.util.ArrayList;
import java.util.List;

import org.pf.util.CollectionUtil;

/**
 * An abstract filter that combines multiple filters.
 * Subclasses decide how the y are combined.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
abstract public class ChainedElementFilter extends AElementFilter
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private final List<IElementFilter> filterList = new ArrayList<IElementFilter>();

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  public ChainedElementFilter(IElementFilter... filter)
  {
    super();
    CollectionUtil.current().addAll(this.getFilterList(), filter);
  } // ChainedElementFilter()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected List<IElementFilter> getFilterList()
  {
    return filterList;
  } // getFilterList()
  
  // -------------------------------------------------------------------------
  
} // class ChainedElementFilter
