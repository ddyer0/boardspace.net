// ===========================================================================
// CONTENT  : CLASS LDAPDirEntryContainer
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 22/08/2006
// HISTORY  :
//  11/07/2004  mdu  CREATED
//	18/06/2006	mdu		added		--> constructor LDAPDirEntryContainer( DistinguishedName )
//	22/08/2006	mdu		changed	-->	consider meta data in constructor
//
// Copyright (c) 2004-2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.directory.ldap;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.bif.filter.IObjectFilter;
import org.pf.directory.DistinguishedName;
import org.pf.osf.MatchRuleFilter;
import org.pf.osf.ObjectContainer;
import org.pf.text.LdapFilterParser;
import org.pf.text.MatchRule;
import org.pf.text.MatchRuleParseException;

/**
 * This special LDAP entry object is for containers that contain other
 * LDAP entries.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class LDAPDirEntryContainer extends LDAPDirEntry
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private ObjectContainer children = new ObjectContainer(20) ;
  protected ObjectContainer getChildren() { return children ; }
  protected void setChildren( ObjectContainer newValue ) { children = newValue ; }
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with a DN.
   */
  public LDAPDirEntryContainer( String dn )
  {
    super( dn ) ;
  } // LDAPDirEntryContainer() 

  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with a DN.
   */
  public LDAPDirEntryContainer( DistinguishedName dn )
  {
  	super( dn ) ;
  } // LDAPDirEntryContainer() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with the given entry data.
   * Actually that means creating a container out of a "simple" entry.
   */
  public LDAPDirEntryContainer( LDAPDirEntry dirObject )
  {
    super( dirObject ) ;
  } // LDAPDirEntryContainer() 

  // -------------------------------------------------------------------------  
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Adds the given entry as child of this container
   * 
   * @param entry The new child to add (must not be null)
   */
  public void addChild( LDAPDirEntry entry ) 
	{
  	if ( entry != null )
		{
			this.getChildren().add( entry ) ;
		}
	} // addChild() 

	// -------------------------------------------------------------------------
  
  /**
   * Removes the given entry from this container's children
   * 
   * @param entry The child to remove (must not be null)
   */
  public boolean removeChild( LDAPDirEntry entry ) 
  {
  	if ( entry != null )
  	{
  		return this.getChildren().remove( entry ) ;
  	}
  	return false ;
  } // removeChild() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Removes the child that has the same DN as the given one.
   * 
   * @param dn The dn of the child to remove (must not be null)
   */
  public boolean removeChild( DistinguishedName dn ) 
  {
  	LDAPDirEntry entry ;
  	
  	if ( dn != null )
  	{
  		entry = new LDAPDirEntry( dn ) ;
  		return this.getChildren().removeEqual( entry ) ;
  	}
  	return false ;
  } // removeChild() 
  
  // -------------------------------------------------------------------------

  /**
   * Returns true if this container contains exactly the given object.
   * That is, comparison is done by identity (==) rather than equality.
   * 
   * @param entry The entry to look for
   */
  public boolean contains( LDAPDirEntry entry ) 
	{
  	if ( entry == null )
		{
			return false ;
		}
		return this.getChildren().contains( entry ) ;
	} // contains()
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns true if this container contains an entry with the given 
   * distinguished name.
   * 
   * @param dn The DN to look for
   */
  public boolean contains( DistinguishedName dn ) 
  {
  	LDAPDirEntry entry ;
  	if ( dn == null )
  	{
  		return false ;
  	}
  	entry = new LDAPDirEntry( dn ) ;
  	return this.getChildren().containsEqualObject( entry ) ;
  } // contains()
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the first immediate child that matches the given filter
   * 
   * @param filter A filter that gets called for each child 
   */
  public LDAPDirEntry findChild( LDAPDirEntryFilter filter )
	{
  	return (LDAPDirEntry)this.getChildren().findFirst( filter ) ; 
	} // findChild() 

	// -------------------------------------------------------------------------

  /**
   * Returns the first immediate child that matches the given filter
   * 
   * @param filter A filter complient to RFC 2254 
   */
  public LDAPDirEntry findChild( String filter )
  	throws MatchRuleParseException
  {
  	MatchRule rule ;
  	
  	rule = this.filterToRule( filter ) ;
  	return (LDAPDirEntry)this.getChildren().findFirst( rule ) ; 
  } // findChild() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns all immediate children that match the given filter
   * 
   * @param filter A filter complient to RFC 2254
   * @throws MatchRuleParseException If the given filter cannot be parsed 
   */
  public LDAPSearchResult findChildren( String filter )
  	throws MatchRuleParseException
	{
  	return this.findChildren( filter, LDAPSearchResult.UNLIMITED_SIZE ) ;
	} // findChildren() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns all immediate children that match the given filter
   * 
   * @param filter A filter that defines which objects match 
   */
  public LDAPSearchResult findChildren( LDAPDirEntryFilter filter )
  {
  	return this.findChildren( filter, LDAPSearchResult.UNLIMITED_SIZE ) ;
  } // findChildren() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns all immediate children that match the given filter
   * 
   * @param filter A filter complient to RFC 2254 
   * @param sizeLimit Maximum number of entries to be collected ( 0 = unlimited )
   * @throws MatchRuleParseException If the given filter cannot be parsed 
   */
  public LDAPSearchResult findChildren( String filter, int sizeLimit )
  	throws MatchRuleParseException
	{
  	MatchRule rule ;
  	LDAPSearchResult result ;
  	
  	rule = this.filterToRule( filter ) ;
  	result = new LDAPSearchResult( this.getChildren().size(), sizeLimit ) ;
  	this.getChildren().find( result, rule ) ;
  	return result ;
	} // findChildren() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns all immediate children that match the given filter
   * 
   * @param filter A filter that defines which object matches
   * @param sizeLimit Maximum number of entries to be collected ( 0 = unlimited )
   */
  public LDAPSearchResult findChildren( LDAPDirEntryFilter filter, int sizeLimit )
  {
  	LDAPSearchResult result ;
  	
  	result = new LDAPSearchResult( this.getChildren().size(), sizeLimit ) ;
  	this.getChildren().find( result, filter ) ;
  	return result ;
  } // findChildren() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns all objects down the whole hierarchy starting from this object
   * if they match the given filter.
   * 
   * @param filter A filter complient to RFC 2254 
   * @throws MatchRuleParseException If the given filter cannot be parsed 
   */
  public LDAPSearchResult findAll( String filter ) 
  	throws MatchRuleParseException
	{
  	return this.findAll( filter, LDAPSearchResult.UNLIMITED_SIZE ) ;
	} // findAll() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns all objects down the whole hierarchy starting from this object
   * if they match the given filter.
   * 
   * @param filter A filter that defines which objects match
   */
  public LDAPSearchResult findAll( LDAPDirEntryFilter filter ) 
  {
  	return this.findAll( filter, LDAPSearchResult.UNLIMITED_SIZE ) ;
  } // findAll() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns all objects down the whole hierarchy starting from this object
   * if they match the given filter.
   * 
   * @param filter A filter complient to RFC 2254 
   * @param sizeLimit Maximum number of entries to be collected ( 0 = unlimited )
   * @throws MatchRuleParseException If the given filter cannot be parsed 
   */
  public LDAPSearchResult findAll( String filter, int sizeLimit ) 
  	throws MatchRuleParseException
	{
  	MatchRule rule ;
  	LDAPSearchResult result ;
  	
  	rule = this.filterToRule( filter ) ;
  	result = new LDAPSearchResult( this.getChildCount() + 100, sizeLimit ) ;
  	this.collectAll( result, new MatchRuleFilter(rule) ) ;
  	return result ;
	} // findAll() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns all objects down the whole hierarchy starting from this object
   * if they match the given filter.
   * 
   * @param filter A filter the defines which objects match 
   * @param sizeLimit Maximum number of entries to be collected ( 0 = unlimited )
   */
  public LDAPSearchResult findAll( LDAPDirEntryFilter filter, int sizeLimit ) 
  {
  	LDAPSearchResult result ;
  	
  	result = new LDAPSearchResult( this.getChildCount() + 100, sizeLimit ) ;
  	this.collectAll( result, filter ) ;
  	return result ;
  } // findAll() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns true if this entry is a container object that can contain other
   * entries as children.
   * <p>
   * Here it always returns true.
   */
  public boolean isContainer() 
	{
		return true ;
	} // isContainer() 

	// -------------------------------------------------------------------------

  /**
   * Returns the number of children
   */
  public int getChildCount() 
	{
		return this.getChildren().size() ;
	} // getChildCount() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns a string representation of this object
   */
  public String toString() 
	{
		return "LDAPDirEntryContainer(" + this.getDN() + ")" ;
	} // toString() 

	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected MatchRule filterToRule( String ldapFilter )
  	throws MatchRuleParseException
	{
  	MatchRule rule ;
  	
  	rule = LdapFilterParser.parseFilter( ldapFilter ) ;
  	rule.ignoreCase( true ) ;
  	rule.ignoreCaseInNames( true ) ;
  	return rule ;
	} // filterToRule() 

	// -------------------------------------------------------------------------
  
  /**
   * Addes all objects down the whole hierarchy starting from this object
   * if they match the given filter to the given result container.
   * 
   * @param result The container to put in the found object
   * @param rule The rule the objects must match 
   */
  protected void collectAll( LDAPSearchResult result, IObjectFilter filter ) 
	{ 
  	LDAPDirEntry child ;
  	
  	if ( filter.matches( this ) )
		{
			result.add( this ) ;
		}
  	for (int i = 0; ( i < this.getChildren().size() ) && ( ! result.isSizeLimitExceeded() )	; i++ )
		{
			child = (LDAPDirEntry)this.getChildren().get(i) ;
			child.collectAll( result, filter ) ;
		}
	} // collectAll() 

	// -------------------------------------------------------------------------
  
} // class LDAPDirEntryContainer 
