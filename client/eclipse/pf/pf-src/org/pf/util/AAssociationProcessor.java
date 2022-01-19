// ===========================================================================
// CONTENT  : ABSTRACT CLASS AAssociationProcessor
// AUTHOR   : M.Duchrow
// VERSION  : 2.0 - 27/03/2010
// HISTORY  :
//  03/06/2006  mdu  CREATED
//	27/03/2010	mdu		changed to support generic types
//
// Copyright (c) 2006-2010, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.bif.callback.IObjectProcessor ;

/**
 * Specialized abstract processor for Association objects.
 * Subclasses must override only method.<br/>
 * public boolean processObject( Association association )
 *
 * @author M.Duchrow
 * @version 2.0
 */
abstract public class AAssociationProcessor<K,V> implements IObjectProcessor<Association<K,V>>
{
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public AAssociationProcessor()
  {
    super() ;
  } // AAssociationProcessor()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Will be called for an association to do something with it.
   * 
   * @param association The association to process
   * @return true if processing should continue, otherwise false
   */
  public abstract boolean processObject( Association<K,V> association ) ;
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class AAssociationProcessor
