// ===========================================================================
// CONTENT  : CLASS AttributeMetaData
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 22/08/2006
// HISTORY  :
//  22/08/2006  mdu  CREATED
//
// Copyright (c) 2006, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.directory.metadata ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.text.StringUtil;

/**
 * Provides meta information about an attribute.
 * The link to the attribute is its (unique) name.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class AttributeMetaData
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private boolean singleValued = false ;

  private String attrName = null ;
  public String getAttrName() { return attrName ; }
  protected void setAttrName( String newValue ) { attrName = newValue ; }
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with its name.
   */
  public AttributeMetaData( String name )
  {
    super() ;
    if ( StringUtil.current().isNullOrBlank( name ) )
		{
			throw new IllegalArgumentException( "name must not be null or blank" ) ;
		}
    this.setAttrName( name ) ;
  } // AttributeMetaData() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Defines that the attribute allows only a single value
   */
  public void beSingleValued() 
	{
		singleValued = true ;
	} // beSingleValued() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Defines that the attribute allows multiple values
   */
  public void beMultiValued() 
	{
		singleValued = false ;
	} // beMultiValued() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns true if the attribute allows only a single value
   */
  public boolean isSingleValued() 
	{
		return singleValued ;
	} // isSingleValued() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns true if the attribute allows multiple values
   */
  public boolean isMultiValues() 
	{
		return !singleValued ;
	} // isMultiValues() 
	
	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class AttributeMetaData 
