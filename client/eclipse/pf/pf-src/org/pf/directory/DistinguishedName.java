// ===========================================================================
// CONTENT  : CLASS DistinguishedName
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.3 - 22/08/2006
// HISTORY  :
//  28/05/2005  mdu  CREATED
//	25/02/2006	mdu		added		-->	isDistinguishedName(), looksLikeDistinguishedName()
//	19/06/2006	mdu		added		-->	getPart(), getPartsWithout(), partsCount(), isParentOf(), isDirectParentOf()
//	27/06/2006	mdu		changed	--> elements from String[] to DistinguishedNameElement[]
//	22/08/2006	mdu		changed	--> Added EMPTY_DN
//
// Copyright (c) 2005-2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.directory ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.text.StringPattern;
import org.pf.text.StringUtil;
import org.pf.util.CollectionUtil;

/**
 * Contains a normalized form of a distinguished name that allows comparison.
 *
 * @author Manfred Duchrow
 * @version 1.3
 */
public class DistinguishedName
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	/**
	 * This is a special DN with an empty string as name and no elements at all
	 */
	public static final DistinguishedName EMPTY_DN = new DistinguishedName() ;
	
	protected static final String ELEMENT_SEPARATOR = "," ;
	
	private static final StringPattern DN_ELEMENT_PATTERN = StringPattern.create( "*?=*?", true ) ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String normalizedDN = null ;
  protected String getNormalizedDN() { return normalizedDN ; }
  protected void setNormalizedDN( String newValue ) { normalizedDN = newValue ; }
  
  private DistinguishedNameElement[] elements = null ;
  protected DistinguishedNameElement[] getElements() { return elements ; }
  protected void setElements( DistinguishedNameElement[] newValue ) { elements = newValue ; }

  // =========================================================================
  // CLASS METHODS
  // =========================================================================
  /**
   * Returns true if the given name could be a distinguished (rather than a
   * uid or a common name). This method is much faster than isDistinguishedName()
   * because it just checks if the given name contains an equals character ('=').
   */
  public static boolean looksLikeDistinguishedName( String name ) 
	{
		return ( name != null ) && ( name.indexOf( '=' ) > 0 ) ;
	} // looksLikeDistinguishedName() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns true if the given name is a valid distinguished name.
   * That is, it contains elements separated by ',' and matching the 
   * pattern: "*?=*?".
   */
  public static boolean isDistinguishedName( String name ) 
	{
  	DistinguishedName dn ;
  	DistinguishedNameElement[] parts ;
  	
  	try
		{
			dn = new DistinguishedName( name ) ;
			parts = dn.getElements() ;
			for (int i = 0; i < parts.length; i++ )
			{
				if ( ! DN_ELEMENT_PATTERN.matches( parts[i].toString() ) )
				{
					return false ;
				}
			}
			return true ;
		}
		catch ( Exception e )
		{
			return false ;
		}
	} // isDistinguishedName() 
	
	// -------------------------------------------------------------------------
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with empty values.
   */
  protected DistinguishedName()
  {
    super() ;
    this.setNormalizedDN( "" ) ;
    this.setElements( new DistinguishedNameElement[0] ) ;
  } // DistinguishedName() 
  
  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with a DN.
   * 
   * @param dn The DN that is internally used by the new instance (must not be null)
   * @throws IllegalArgumentException If the given DN is null or blank
   */
  public DistinguishedName( String dn )
  {
  	super() ;
  	if ( this.str().isNullOrBlank( dn ) )
  	{
  		throw new IllegalArgumentException( "dn must not be null or empty") ;
  	}
  	this.init( dn ) ;
  } // DistinguishedName() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with already normalized parts.
   * 
   * @param parts The parts that are internally used to initialize the new instance (must not be null)
   * @throws IllegalArgumentException If the given parts are null or empty
   */
  protected DistinguishedName( DistinguishedNameElement[] parts )
  {
  	super() ;
  	if ( this.coll().isNullOrEmpty( parts ) )
  	{
  		throw new IllegalArgumentException( "normalized parts must not be null or empty") ;
  	}
  	this.initFromNormalizedParts( parts ) ;
  } // DistinguishedName() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the DN as string where all elements are separated by comma (',')
   * without leading or trailing blanks.
   */
  public String toString() 
	{
		return this.getNormalizedDN() ;
	} // toString() 

	// -------------------------------------------------------------------------

	public boolean equals( Object obj )
	{
		DistinguishedName dn ;
		
		if ( obj instanceof DistinguishedName )
		{
			dn = (DistinguishedName)obj ;
			return this.getNormalizedDN().equalsIgnoreCase( dn.getNormalizedDN() );
		}
		return false ;
	} // equals() 
	
	// -------------------------------------------------------------------------
	
	public int hashCode()
	{
		return this.getNormalizedDN().toLowerCase().hashCode();
	} // hashCode() 

	// -------------------------------------------------------------------------
  
	/**
	 * Returns true if the given dn is equal to this distinguished name after
	 * normalization
	 */
	public boolean isEqual( String dn ) 
	{
		DistinguishedName distinguishedName ;
		
		if ( dn == null )
			return false ;
		
		distinguishedName = new DistinguishedName( dn ) ;
		return this.equals( distinguishedName ) ;
	} // isEqual() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if this object is the empty DN (i.e. the object specified 
	 * by constant EMPTY_DN). 
	 */
	public boolean isEmptyDN() 
	{
		return ( this.partCount() == 0 ) && ( this.getNormalizedDN().length() == 0 ) ;
	} // isEmptyDN()
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the this distinguished name represents an object that
	 * is logically a parent of the given dn in the DIT.
	 * This is not necessarily the direct parent of the given DN. It might be
	 * any parent up the DIT hierarchy.
	 * <p>
	 * Be aware that the EMPTY_DN is NOT parent of any other DN and therefore 
	 * always returns false here.
	 * 
	 * @param dn The DN to be checked.
	 * @see #isDirectParentOf(DistinguishedName)
	 */
	public boolean isParentOf( DistinguishedName dn ) 
	{
		DistinguishedNameElement[] parts ;
		int j ;
		
		if ( ( dn == null ) || ( dn.getElements() == null ) )
		{
			return false ;
		}
		parts = this.getElements() ;
		if ( parts.length < dn.getElements().length )
		{
			j = dn.getElements().length - 1 ;
			for (int i = parts.length-1; i >= 0; i-- )
			{
				if ( ! parts[i].equalsIgnoreCase( dn.getElements()[j] ) )
				{
					return false ;
				}
				j--;
			}
			return true ;
		}
		return false ;
	} // isParentOf() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the this distinguished name represents an object that
	 * is the direct parent of the given dn in the DIT.
	 * 
	 * @param dn The DN to be checked.
	 */
	public boolean isDirectParentOf( DistinguishedName dn ) 
	{
		if ( ( dn == null ) || ( dn.getElements() == null ) )
		{
			return false ;
		}
		if ( this.getElements().length == ( dn.getElements().length -1 ) )
		{
			return this.isParentOf( dn ) ;
		}
		return false ;
	} // isDirectParentOf() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns a copy of the parts that build this DN.
	 */
	public DistinguishedNameElement[] getParts() 
	{
		return (DistinguishedNameElement[])this.coll().copy( this.getElements() ) ;
	} // getParts() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns a copy of the parts that build this DN without the number of 
	 * elements given by the parameter. 
	 * 
	 * @param skip defines how many parts from the right not to return
	 */
	public DistinguishedNameElement[] getPartsWithout( int skip ) 
	{
		DistinguishedNameElement[] parts ;
		
		parts = new DistinguishedNameElement[this.partCount()-skip] ;
		System.arraycopy( this.getElements(), 0, parts, 0, parts.length ) ;
		return parts ;
	} // getPartsWithout() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the relative distinguished name (i.e. the left most element)
	 * of this distinguished name.
	 */
	public DistinguishedNameElement getRDN() 
	{
		if ( this.partCount() > 0 )
		{
			return this.getElements()[0] ;
		}
		return null ;
	} // getRDN() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns how many parts this DN consists of
	 */
	public int partCount() 
	{
		return this.getElements().length ;
	} // partCount() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the parent DN of this DN or EMPTY_DN if this DN has no parent.
	 */
	public DistinguishedName getParent() 
	{
		DistinguishedNameElement[] parts ;
		
		if ( this.partCount() <= 1 )
		{
			return EMPTY_DN ;
		}
		parts = new DistinguishedNameElement[this.getElements().length-1] ;
		System.arraycopy( this.getElements(), 1, parts, 0, parts.length ) ;
		return new DistinguishedName( parts ) ;
	} // getParent() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the new distinguished name that represents a child of this DN.
	 * 
	 * @param rdn The relative distinguished name to be added as child element
	 */
	public DistinguishedName makeChild( DistinguishedNameElement rdn ) 
	{
		DistinguishedNameElement[] parts ;
		
		parts = new DistinguishedNameElement[this.getElements().length+1] ;
		System.arraycopy( this.getElements(), 0, parts, 1, this.getElements().length ) ;
		parts[0] = rdn ;
		return new DistinguishedName( parts ) ;
	} // makeChild() 
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected void init( String dn ) 
	{
  	String[] parts ;
		String[] keyValue ;
		DistinguishedNameElement[] dnElements ;
		
		parts = this.str().parts( dn, ELEMENT_SEPARATOR ) ;
		dnElements = new DistinguishedNameElement[parts.length] ;
		for (int i = 0; i < parts.length; i++ )
		{
			keyValue = this.str().splitNameValue( parts[i].trim(), DistinguishedNameElement.ASSIGN_SEPARATOR ) ;
			dnElements[i] = new DistinguishedNameElement( keyValue[0].trim(), keyValue[1].trim() ) ;
		}
		this.initFromNormalizedParts( dnElements ) ;
	} // init() 

	// -------------------------------------------------------------------------

  protected void initFromNormalizedParts( DistinguishedNameElement[] parts ) 
	{
		this.setNormalizedDN( this.asString( parts, ELEMENT_SEPARATOR ) ) ;
		this.setElements( parts ) ;
	} // initFromNormalizedParts() 
	
	// -------------------------------------------------------------------------
  
  protected String asString( DistinguishedNameElement[] parts, String separator ) 
	{
		StringBuffer buffer ;
		
		buffer = new StringBuffer(80) ;
		for (int i = 0; i < parts.length; i++ )
		{
			if ( i > 0 )
			{
				buffer.append( separator ) ;
			}
			buffer.append( parts[i].toString() ) ;
		}
		return buffer.toString() ;
	} // asString() 
	
	// -------------------------------------------------------------------------
  
  protected CollectionUtil coll() 
	{
		return CollectionUtil.current() ;
	} // coll() 
	
	// -------------------------------------------------------------------------
  
  protected StringUtil str() 
	{
		return StringUtil.current() ;
	} // str() 

	// -------------------------------------------------------------------------

} // class DistinguishedName 
