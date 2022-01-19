// ===========================================================================
// CONTENT  : CLASS LDAPSearchResult
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 07/08/2004
// HISTORY  :
//  07/08/2004  mdu  CREATED
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.directory.ldap ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.osf.ObjectSearchResult;

/**
 * Apart from returning the found object a search result can contain 
 * information if the size limit has been reached. 
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class LDAPSearchResult extends ObjectSearchResult
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with no size limit.
   */
  public LDAPSearchResult()
  {
    super() ;
  } // LDAPSearchResult() 

  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with an initial size.
   * 
   * @param initialCapacity Initial size reserved for elements
   */
  public LDAPSearchResult( int initialCapacity )
  {
    super( initialCapacity ) ;
  } // LDAPSearchResult() 

  // -------------------------------------------------------------------------
 
  /**
   * Initialize the new instance with an initial size and a maximum size.
   * If the given initial size is greater than the maximum size, then the
   * maximum size will be used as initial size too.
   * 
   * @param initialCapacity Initial size reserved for elements
   */
  public LDAPSearchResult( int initialCapacity, int maximumSize )
  {
    super( initialCapacity, maximumSize ) ;
  } // LDAPSearchResult() 

  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns an array containing all found objects.
   */
  public LDAPDirEntry[] getFoundObjects() 
	{
  	return (LDAPDirEntry[])this.asArray( LDAPDirEntry.class ) ;
	} // getFoundObjects() 

	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  
} // class LDAPSearchResult 
