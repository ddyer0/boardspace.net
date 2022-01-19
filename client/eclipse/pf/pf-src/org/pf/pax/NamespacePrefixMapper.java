// ===========================================================================
// CONTENT  : CLASS NamespacePrefixMapper
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 30/01/2011
// HISTORY  :
//  30/01/2011  mdu  CREATED
//
// Copyright (c) 2011, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.pax;

//===========================================================================
//IMPORTS
//===========================================================================
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

/**
* An implementation that handles namespace prefix to URI and URI to prefix
* mapping.
*
* @author M.Duchrow
* @version 1.0
*/
public class NamespacePrefixMapper implements NamespaceContext
{
	// =========================================================================
	// CONSTANTS
	// =========================================================================

	// =========================================================================
	// INSTANCE VARIABLES
	// =========================================================================
	private Map<String, String> mappings;

	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
  /**
   * Initialize the new instance with the given mappings.
   */
  public NamespacePrefixMapper( Map<String,String> prefixToURIMappings )
  {
    super() ;
    this.mappings = prefixToURIMappings ;
  } // NamespacePrefixMapper() 

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with an empty mapping.
   */
  public NamespacePrefixMapper()
  {
    this( new HashMap<String,String>() ) ;
  } // NamespacePrefixMapper()
  
  // -------------------------------------------------------------------------
  
	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	public String getNamespaceURI(String prefix)
	{
		return this.mappings.get(prefix);
	} // getNamespaceURI() 

	// -------------------------------------------------------------------------

	public String getPrefix(String namespaceURI)
	{
		for (Map.Entry<String, String> mapping : this.mappings.entrySet() )
		{
			if ( mapping.getValue().equals(namespaceURI) )
			{
				return mapping.getKey();
			}
		}
		return null;
	} // getPrefix() 

	// -------------------------------------------------------------------------

	public Iterator getPrefixes(String namespaceURI)
	{
		List<String> result;

		result = new ArrayList<String>();
		for (Map.Entry<String, String> mapping : this.mappings.entrySet() )
		{
			if ( mapping.getValue().equals(namespaceURI) )
			{
				result.add(mapping.getKey());
			}
		}
		return result.iterator();
	} // getPrefixes() 

	// -------------------------------------------------------------------------

	/**
	 * Add the prefix and URI to the mappings.
	 */
	public void addMapping(String nsPrefix, String nsURI)
	{
		if ( (nsPrefix != null) && (nsURI != null) )
		{			
			this.mappings.put(nsPrefix, nsURI);
		}
	} // addMapping() 
	
	// -------------------------------------------------------------------------
	
	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
} // class NamespacePrefixMapper
