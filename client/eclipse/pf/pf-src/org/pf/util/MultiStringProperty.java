// ===========================================================================
// CONTENT  : CLASS MultiStringProperty
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

/**
 * This is a property with a name and multiple strings as value list.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class MultiStringProperty extends MultiValueProperty<String>
{
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public MultiStringProperty(String name, String... values)
  {
    super(name, values) ;
  } // MultiStringProperty()
  
  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with a name and define whether or not 
   * duplicate values are allowed.
   */
  public MultiStringProperty(String name, boolean allowDuplicateValues, String... values)
  {
  	super(name, allowDuplicateValues, values) ;
  } // MultiStringProperty() 
  
  // -------------------------------------------------------------------------

} // class MultiStringProperty
