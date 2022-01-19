// ===========================================================================
// CONTENT  : CLASS LDAPDirEntryFilter
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 18/06/2006
// HISTORY  :
//  18/06/2006  mdu  CREATED
//
// Copyright (c) 2006, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.directory.ldap ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.bif.filter.IObjectFilter;

/**
 * Subclasses must override the method matches(LDAPDirEntry) in order to be useful.
 *
 * @author M.Duchrow
 * @version 1.0
 */
abstract public class LDAPDirEntryFilter implements IObjectFilter
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public LDAPDirEntryFilter()
  {
    super() ;
  } // LDAPDirEntryFilter() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Calls matches(LDAPDirEntry) if the given object is of that type otherwise
   * it returns false.
   */
  public boolean matches( Object object )
  {
  	if ( object instanceof LDAPDirEntry )
		{
			return this.matches( (LDAPDirEntry)object ) ;
		}
  	return false;
  } // matches()
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns true if the given LDAP entry object matches this filter.
   */
  abstract public boolean matches( LDAPDirEntry entry ) ;
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class LDAPDirEntryFilter 
