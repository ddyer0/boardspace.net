// ===========================================================================
// CONTENT  : CLASS RDNFilter
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 27/06/2006
// HISTORY  :
//  27/06/2006  mdu  CREATED
//
// Copyright (c) 2006, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.directory.ldap ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.directory.DistinguishedNameElement;

/**
 * Special filter that can be used to lookup an LDAP entry by its 
 * Relative Distinguished Name (RDN).
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class RDNFilter extends LDAPDirEntryFilter
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private DistinguishedNameElement rdn = null ;
  protected DistinguishedNameElement getRdn() { return rdn ; }
  protected void setRdn( DistinguishedNameElement newValue ) { rdn = newValue ; }
	
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public RDNFilter( DistinguishedNameElement rdn )
  {
  	super() ;
  	this.setRdn( rdn ) ;
  } // RDNFilter() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns true if the given entry has the same RDN as this filter
   */
  public boolean matches( LDAPDirEntry entry )
  {
  	return this.getRdn().equalsIgnoreCase( entry.getDistinguishedName().getRDN() ) ;
  } // matches()
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class RDNFilter 
