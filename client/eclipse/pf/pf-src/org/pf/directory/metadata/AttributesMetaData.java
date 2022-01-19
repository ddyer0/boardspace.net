// ===========================================================================
// CONTENT  : CLASS AttributesMetaData
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
import java.util.Map;

import org.pf.util.CaseInsensitiveKeyMap;

/**
 * A container for a collection of AttributeMetaData objects.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class AttributesMetaData
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private Map attrMetaData = new CaseInsensitiveKeyMap(40) ;
  protected Map getAttrMetaData() { return attrMetaData ; }
  protected void setAttrMetaData( Map newValue ) { attrMetaData = newValue ; }
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public AttributesMetaData()
  {
    super() ;
  } // AttributesMetaData() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Adds the given meta data or replaces one with the same name. 
   */
  public void setMetaData( AttributeMetaData metaData ) 
	{
		if ( metaData != null )
		{
			this.getAttrMetaData().put( metaData.getAttrName(), metaData ) ;
		}
	} // setMetaData() 
	
	// -------------------------------------------------------------------------

  /**
   * Returns the meta data for the given attribute name.
   * 
   * @param attrName The (case-insensitive) name of the attribute 
   * @return The found metadata or null
   */
  public AttributeMetaData getMetaData( String attrName  ) 
  {
  	if ( attrName != null )
  	{
  		return (AttributeMetaData)this.getAttrMetaData().get( attrName ) ;
  	}
  	return null ;
  } // getMetaData() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Removes the meta data for the given attribute name.
   * 
   * @param attrName The (case-insensitive) name of the attribute 
   * @return The removed metadata or null
   */
  public AttributeMetaData removeMetaData( String attrName  ) 
  {
  	if ( attrName != null )
  	{
  		return (AttributeMetaData)this.getAttrMetaData().remove( attrName ) ;
  	}
  	return null ;
  } // removeMetaData() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the meta data for the given attribute name. If it already
   * exists that will be returned otherwise a new meta data object with 
   * the given name is created and returned.
   * 
   * @param attrName The (case-insensitive) name of the attribute 
   * @return The found metadata for the given attribute name
   */
  public AttributeMetaData attributeMetaData( String attrName  ) 
  {
  	AttributeMetaData metadata ;
  	
  	metadata = this.getMetaData( attrName ) ;
  	if ( metadata == null )
  	{
  		metadata = new AttributeMetaData( attrName ) ;
  		this.setMetaData( metadata ) ;
  	}
  	return metadata ;
  } // attributeMetaData() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class AttributesMetaData 
