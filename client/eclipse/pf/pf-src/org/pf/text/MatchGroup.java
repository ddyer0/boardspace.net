// ===========================================================================
// CONTENT  : CLASS MatchGroup
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.7 - 20/12/2004
// HISTORY  :
//  11/07/2001  duma  CREATED
//  09/10/2001  duma  changed -> Made class and constructor public
//  08/01/2002  duma  changed -> Made serializable
//	14/08/2002	duma	changed	-> New constructor with no arguments
//	23/08/2002	duma	re-designed	-> Moved parsing and printing to other classes
//	24/10/2002	duma	bugfix  -> Removed short circuit in doMatch()
//	24/10/2003	duma	added		-> multiCharWildcardMatchesEmptyString()
//	04/12/2003	duma	added		-> optimize(), optimizeAttribute()
//	20/12/2004	duma	added		-> applyDatatypes()
//
// Copyright (c) 2001-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Represents a group of MatchAttributes and/or MatchGroups
 *
 * @author Manfred Duchrow
 * @version 1.7
 */
public class MatchGroup extends MatchElement implements Serializable
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private List elements = null ;
  protected List getElements() { return elements ; }
  protected void setElements( List newValue ) { elements = newValue ; }

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public MatchGroup()
  {
    super() ;
    this.setElements( this.newElementList() );
  } // MatchGroup() 
 
  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Add the specified element to the group.
	 * The element will be added at the end of the element list.
	 */
  public void addElement( MatchElement element )
  {
    this.getElements().add( element ) ;
  } // addElement() 
 
  // -------------------------------------------------------------------------

	/**
	 * Returns always true, because this is a group.
	 */
  public boolean isGroup()
  {
    return true ;
  } // isGroup() 
 
  // -------------------------------------------------------------------------

	/**
	 * Returns the current number of elements in this group.
	 */
	public int elementCount()
	{
		return this.getElements().size() ;
	} // elementCount() 
 
	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  protected MatchElement getElement( int index )
  {
    return (MatchElement)this.getElements().get( index ) ;
  } // getElement() 
 
  // -------------------------------------------------------------------------

  protected boolean doMatch( Map dictionary )
  {
    Iterator iterator     = null ;
    MatchElement element  = null ;
    boolean matched       = true ;
    boolean isFirst       = true ;

    iterator = this.getElements().iterator() ;
    while ( iterator.hasNext() )
    {
      element = (MatchElement)iterator.next() ;
      if ( isFirst )
      {
        isFirst = false ;
        matched = element.matches( dictionary ) ;
      }
      else
      {
        if ( element.getAnd() )
        {
          matched = matched && element.matches( dictionary ) ;
        }
        else
        {
          matched = matched || element.matches( dictionary ) ;
        }
      }
    }
    return matched ;
  } // doMatch() 
 
  // -------------------------------------------------------------------------

  protected void ignoreCase( boolean ignoreIt )
  {
    for ( int i = 0 ; i < this.getElements().size() ; i++ )
      this.getElement(i).ignoreCase( ignoreIt ) ;
  } // ignoreCase() 
 
  // -------------------------------------------------------------------------

	/**
	 * Defines whether or not the case of characters in attribute names
	 * must be ignored.
	 */
	public void ignoreCaseInName( boolean ignoreIt )
	{
		for ( int i = 0 ; i < this.getElements().size() ; i++ )
			this.getElement(i).ignoreCaseInName( ignoreIt ) ;
	} // ignoreCaseInName() 
   
	// -------------------------------------------------------------------------

	protected void multiCharWildcardMatchesEmptyString( boolean yesOrNo )
	{
		for ( int i = 0 ; i < this.getElements().size() ; i++ )
			this.getElement(i).multiCharWildcardMatchesEmptyString( yesOrNo ) ;
	} // multiCharWildcardMatchesEmptyString() 
   
	// -------------------------------------------------------------------------

  protected void apply( MatchRuleVisitor visitor )
  {
    visitor.startGroup( this.getAnd(), this.getNot()  ) ;

    for ( int i = 0 ; i < this.getElements().size() ; i++ )
      this.getElement(i).apply( visitor ) ;

    visitor.endGroup() ;
  } // apply() 
 
  // -------------------------------------------------------------------------

  protected void applyDatatypes( Map datatypes )
		throws MatchRuleException
	{
    for ( int i = 0 ; i < this.getElements().size() ; i++ )
      this.getElement(i).applyDatatypes( datatypes ) ;
	} // applyDatatypes() 
	
	// -------------------------------------------------------------------------
	  
	protected void optimize()
	{
		MatchGroup group ;
		
		for (int i = 0; i < this.elementCount(); i++)
		{
			if ( this.getElement(i).isAttribute() )
			{
				this.optimizeAttribute( i ) ;
			}
			else
			{
				group = (MatchGroup)this.getElement(i) ;
				group.optimize() ;
			}
		}		
	} // optimize() 
 
	// -------------------------------------------------------------------------

	protected void optimizeAttribute( int index )
	{
		MatchAttribute attr ;
		MatchAttribute otherAttr ;
		Collection patterns = null ;
		boolean optimized = false ;
		boolean done = false ;
		int i ;
		
		if ( this.getElement(index).isAttribute() )
			attr = (MatchAttribute)this.getElement(index) ;
		else
			return ;
		
		i = index + 1 ;
		while ( !done && ( i < this.elementCount() ) )
		{
			done = true ;
			if ( this.getElement(i).isAttribute() )
			{
				otherAttr = (MatchAttribute)this.getElement(i) ;
				if ( !( otherAttr.getNot() || otherAttr.getAnd() ) )
				{
					if ( attr.getAttributeName().equals(otherAttr.getAttributeName() ) )
					{	
						done = false ;
						if ( !optimized )
						{
							optimized = true ;
							patterns = new ArrayList( attr.getPatterns().length + otherAttr.getPatterns().length ) ;
							this.addAll( patterns, attr.getPatterns() ) ;
						}
						this.addAll( patterns, otherAttr.getPatterns() ) ;
					}
				}
			}
			i++ ;
		}
		if ( optimized )
		{
			attr.setPatterns( (StringPattern[])patterns.toArray(new StringPattern[patterns.size()]) ) ;
			this.removeElements( index + 1, i - 1 ) ;
		}
	} // optimizeAttribute() 
 
	// -------------------------------------------------------------------------

	protected void addAll( Collection coll, StringPattern[] patternArray )
	{
		for (int i = 0; i < patternArray.length; i++)
		{
			coll.add( patternArray[i] ) ;
		}
	} // addAll() 
 
	// -------------------------------------------------------------------------

	protected void removeElements( int from, int to )
	{
		List list ;
		
		list = this.newElementList() ;
		for (int i = 0; i < this.elementCount(); i++)
		{
			if ( ( i < from ) || ( i > to ) )
				list.add( this.getElement(i) ) ;
		}
		this.setElements( list ) ;
	} // removeElements() 
 
	// -------------------------------------------------------------------------

	protected List newElementList()
	{
		return new ArrayList() ;
	} // newElementList() 
 
	// -------------------------------------------------------------------------

} // class MatchGroup 
