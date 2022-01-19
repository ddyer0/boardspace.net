// ===========================================================================
// CONTENT  : INTERFACE XmlItem
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 17/01/2014
// HISTORY  :
//  17/01/2014  mdu  CREATED
//
// Copyright (c) 2014, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.six ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Defines a minimal common interface each object in an XML tree must support.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public interface XmlItem
{ 
  public enum Type { ELEMENT, PI }
  
  /**
   * Return the name of the item.
   */
  public String getName();
  
  /**
   * Returns the type of the item.
   */
  public Type getItemType();
  
} // interface XmlItem