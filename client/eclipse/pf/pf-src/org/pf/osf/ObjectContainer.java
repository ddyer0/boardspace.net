// ===========================================================================
// CONTENT  : CLASS ObjectContainer
// AUTHOR   : M.Duchrow
// VERSION  : 1.2 - 24/02/2006
// HISTORY  :
//  21/05/2004  mdu  CREATED
//	28/05/2005	mdu		added		-->	find() methods for ObjectFilter
//	24/02/2006	mdu		changed	-->	Using IObjectFilter rather than ObjectFilter
//
// Copyright (c) 2004-2006, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.osf ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.pf.bif.filter.IObjectFilter;
import org.pf.reflect.AttributeReadAccess;
import org.pf.text.DefaultMatchRuleParser;
import org.pf.text.MatchRule;
import org.pf.text.MatchRuleParseException;
import org.pf.text.StringUtil;

/**
 * A generic container of objects that keeps it objects in the order they were
 * added. It also provides methods to search for object by match rules.
 *
 * @author M.Duchrow
 * @version 1.2
 */
public class ObjectContainer
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final DefaultMatchRuleParser ruleParser = createParser() ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private List objects = null ;
  protected List getObjects() { return objects ; }
  protected void setObjects( List newValue ) { objects = newValue ; }
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================
  /**
   * Creates a properly initialized match rule parser
   */
  static DefaultMatchRuleParser createParser() 
	{
  	DefaultMatchRuleParser p ;
  	
  	p = new DefaultMatchRuleParser() ;
  	p.setMultiCharWildcardMatchesEmptyString( true ) ;
  	return p ;
	} // createParser() 

	// -------------------------------------------------------------------------

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public ObjectContainer()
  {
    this( 20 ) ;
  } // ObjectContainer() 

  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with an initial size.
   * 
   * @param initialCapacity Initial size reserved for elements
   */
  public ObjectContainer( int initialCapacity )
  {
    super() ;
    this.setObjects( this.newList( initialCapacity ) ) ;
  } // ObjectContainer() 

  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Adds the given object to the container
   */
  public void add( AttributeReadAccess object ) 
	{
		if ( object != null )
		{
			this.getObjects().add( object ) ;
		}
	} // add() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns the object at the given index position.
   * 
   * @throws ArrayIndexOutOfBoundsException if the index is outside the elements range
   */
  public AttributeReadAccess get( int index ) 
	{
		return (AttributeReadAccess)this.getObjects().get( index ) ;
	} // get() 

	// -------------------------------------------------------------------------
  
  /**
   * Puts the given object at the specified index and returns the object
   * previously located at that index.
   * 
   * @param index The index of the element to be replaced
   * @param object The object to put into the container
   * @throws ArrayIndexOutOfBoundsException if the index is outside the current range
   */
  public AttributeReadAccess set( int index, AttributeReadAccess object ) 
	{
		if ( object != null )
		{
			return (AttributeReadAccess)this.getObjects().set( index, object ) ;
		}
		return null ;
	} // set() 

	// -------------------------------------------------------------------------
  
  /**
   * Removes all objects from the container
   */
  public void clear() 
	{
		this.getObjects().clear() ;
	} // clear() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns a copy of this object container. The copy contains all (identical)
   * objects that this container.
   */
  public ObjectContainer copy() 
	{
		ObjectContainer copy ;
		
		copy = new ObjectContainer(0) ;
		copy.setObjects( this.asList() ) ;
		return copy ;
	} // copy() 

	// -------------------------------------------------------------------------
  
  /**
   * Removes the given object from the container. Returns true if the object 
   * was in the container, otherwise false. The object is looked up by
   * identity (==) rather than equality (equals()).
   * 
   * @param object The object to remove 
   * @see #removeEqual(AttributeReadAccess)
   */
  public boolean remove( AttributeReadAccess object ) 
	{
  	Iterator iter ;
  	Object element ; 
  	
		if ( object != null )
		{
			iter = this.getObjects().iterator() ;
			while ( iter.hasNext() )
			{
				element = iter.next();
				if ( object == element )
				{
					iter.remove() ;
					return true ;
				}
			}
		}
		return false ;
	} // remove() 

	// -------------------------------------------------------------------------
  
  /**
   * Removes the given object from the container. Returns true if the object 
   * was in the container, otherwise false. The object is looked up by
   * equality (equals()) rather than identity (==).
   * 
   * @param object The object to remove
   * @see #remove(AttributeReadAccess) 
   */
  public boolean removeEqual( AttributeReadAccess object ) 
  {
  	Iterator iter ;
  	Object element ; 
  	
  	if ( object != null )
  	{
  		iter = this.getObjects().iterator() ;
  		while ( iter.hasNext() )
  		{
  			element = iter.next();
  			if ( object.equals(element) )
  			{
  				iter.remove() ;
  				return true ;
  			}
  		}
  	}
  	return false ;
  } // removeEqual() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the number of contained objects.
   */
  public int size() 
	{
		return this.getObjects().size() ;
	} // size() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns true if the container currently contains nothing.
   */
  public boolean isEmpty() 
	{
		return this.getObjects().isEmpty() ;
	} // isEmpty() 

	// -------------------------------------------------------------------------
    
  /**
   * Returns true if the container contains an object equal to the given object.
   */
  public boolean containsEqualObject( AttributeReadAccess object ) 
	{
		if ( object != null )
		{
			return this.getObjects().contains( object ) ;
		}
		return false ;
	} // containsEqualObject() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns true if the given object is already in this container.
   * Comparison is done by identity rather than equality!
   * 
   * @param object The object to looked for 
   */
  public boolean contains( AttributeReadAccess object ) 
	{
  	Iterator iter ;
  	Object element ; 
  	
		if ( object != null )
		{
			iter = this.getObjects().iterator() ;
			while ( iter.hasNext() )
			{
				element = iter.next();
				if ( object == element )
				{
					return true ;
				}
			}
		}
		return false ;
	} // contains() 

	// -------------------------------------------------------------------------
 
  /**
   * Returns the first object in this container that matches the given rule or
   * null if no matching element can be found.
   * The rule must comply to the MatchRule syntax of 
   * org.pf.text.DefaultMatchRuleParser.
   * 
   * @param rule A rule based on the attributes the contained objects must match
   * @see org.pf.text.DefaultMatchRuleParser
   * @throws MatchRuleParseException For any error when parsing the given rule  
   */
  public AttributeReadAccess findFirst( String rule ) 
  	throws MatchRuleParseException
	{
  	ObjectContainer result ;
  	
   	result = this.find( rule, true, false ) ;
   	if ( result.isEmpty() )
		{
			return null ;
		}
		else
		{
			return result.get(0) ;
		}
	} // findFirst() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns the first object in this container that matches the given rule or
   * null if no matching element can be found.
   * For all attribute and values the charachter case is ignored.
   * The rule must comply to the MatchRule syntax of 
   * org.pf.text.DefaultMatchRuleParser.
   * 
   * @param rule A rule based on the attributes the contained objects must match
   * @see org.pf.text.DefaultMatchRuleParser
   * @throws MatchRuleParseException For any error when parsing the given rule  
   */
  public AttributeReadAccess findFirstIgnoreCase( String rule ) 
  	throws MatchRuleParseException
	{
  	ObjectContainer result ;
  	
   	result = this.find( rule, true, true ) ;
   	if ( result.isEmpty() )
		{
			return null ;
		}
		else
		{
			return result.get(0) ;
		}
	} // findFirstIgnoreCase() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns all object in this container that match the given rule.
   * The rule must comply to the MatchRule syntax of 
   * org.pf.text.DefaultMatchRuleParser.
   * 
   * @param rule A rule based on the attributes the contained objects must match 
   * @see org.pf.text.DefaultMatchRuleParser
   * @throws MatchRuleParseException For any error when parsing the given rule  
   */
  public ObjectSearchResult find( String rule ) 
  	throws MatchRuleParseException
	{
  	return this.find( rule, false, false ) ;
	} // find() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns all objects in this container that match the given rule.
   * For all attribute and values the charachter case is ignored.
   * The rule must comply to the MatchRule syntax of 
   * org.pf.text.DefaultMatchRuleParser.
   * 
   * @param rule A rule based on the attributes the contained objects must match 
   * @see org.pf.text.DefaultMatchRuleParser
   * @throws MatchRuleParseException For any error when parsing the given rule  
   */
  public ObjectSearchResult findIgnoreCase( String rule ) 
  	throws MatchRuleParseException
	{
  	return this.find( rule, false, true ) ;
	} // findIgnoreCase() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns all objects in this container that match the given rule.
   * 
   * @param rule A rule based on the attributes the contained objects must match
   */
  public ObjectSearchResult find( MatchRule rule ) 
	{
  	return this.find( rule, false ) ;
	} // find() 

	// -------------------------------------------------------------------------
   
  /**
   * Returns all objects in this container that match the given filter.
   * 
   * @param filter A filter the contained objects must match
   */
  public ObjectSearchResult find( IObjectFilter filter ) 
	{
  	return this.find( filter, false ) ;
	} // find() 

	// -------------------------------------------------------------------------
   
  /**
   * Adds all objects in this container that match the given rule to the given
   * result object container.
   * 
   * @param result The container all found objects must be added to
   * @param rule A rule based on the attributes the contained objects must match
   */
  public void find( ObjectSearchResult result, MatchRule rule ) 
	{
  	this.collect( result, rule, false ) ;
	} // find() 

	// -------------------------------------------------------------------------
   
  /**
   * Adds all objects in this container that match the given filter to the given
   * result object container.
   * 
   * @param result The container all found objects must be added to
   * @param filter A filter the contained objects must match
   */
  public void find( ObjectSearchResult result, IObjectFilter filter ) 
	{
  	this.collect( result, filter, false ) ;
	} // find() 

	// -------------------------------------------------------------------------
   
  /**
   * Adds all objects in this container that match the given rule to the given
   * result object container.
   * 
   * @param result The container all found objects must be added to
   * @param rule A rule based on the attributes the contained objects must match
   * @param ignoreCase defines whether or not the search should be case-insensitive
   * @see org.pf.text.DefaultMatchRuleParser
   * @throws MatchRuleParseException For any error when parsing the given rule  
   */
  public void find( ObjectSearchResult result, String rule, boolean ignoreCase ) 
  	throws MatchRuleParseException
	{
  	MatchRule matchRule ;
  	
  	matchRule = this.parseMatchRule( rule, ignoreCase ) ;
  	this.find( result, matchRule ) ;
	} // find() 

	// -------------------------------------------------------------------------
   
  /**
   * Returns the first object in this container that matches the given rule.
   * 
   * @param rule A rule based on the attributes the contained objects must match
   */
  public AttributeReadAccess findFirst( MatchRule rule ) 
	{
  	ObjectContainer result ;
  	
   	result = this.find( rule, true ) ;
   	if ( result.isEmpty() )
		{
			return null ;
		}
		else
		{
			return result.get(0) ;
		}
	} // findFirst() 

	// -------------------------------------------------------------------------
   
  /**
   * Returns the first object in this container that matches the given filter.
   * 
   * @param filter A filter the contained objects must match
   */
  public AttributeReadAccess findFirst( IObjectFilter filter ) 
	{
  	ObjectContainer result ;
  	
   	result = this.find( filter, true ) ;
   	if ( result.isEmpty() )
		{
			return null ;
		}
		else
		{
			return result.get(0) ;
		}
	} // findFirst() 

	// -------------------------------------------------------------------------
   
  /**
   * Returns all contained objects as List.
   * The returned list is not backed by this object container.
   */
  public List asList() 
	{
		List list ;
		
		list = this.newList( this.size() ) ;
		list.addAll( this.getObjects() ) ;
		return list ;
	} // asList() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns the elements of this container in an array of the specified type
   * 
   * @param elementType The type each element must comply to
   */
  public Object[] asArray( Class elementType ) 
	{
  	Object[] array ;
  	
  	array = (Object[])Array.newInstance( elementType, this.size() ) ;
  	this.getObjects().toArray( array ) ;
		return array ;
	} // asArray() 

	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * Adds all objects in this container that match the given rule to the given
   * result container.
   * 
   * @param result The result container all found objects must be added to
   * @param rule A rule based on the attributes the contained objects must match
   * @param first if true then the result will only contain the first matching object
   */
  protected void collect( ObjectSearchResult result, MatchRule rule, boolean first ) 
	{
  	MatchRuleFilter filter ;
  	
  	filter = new MatchRuleFilter( rule ) ;
  	this.collect(result, filter, first ) ;
	} // collect() 
  
  // -------------------------------------------------------------------------
  	
  /**
   * Adds all objects in this container that match the given filter to the given
   * result container.
   * 
   * @param result The result container all found objects must be added to
   * @param filter The filter that determines which objects to collect
   * @param first if true then the result will only contain the first matching object
   */
  protected void collect( ObjectSearchResult result, IObjectFilter filter, boolean first ) 
	{
  	Iterator iter ;
  	AttributeReadAccess element ;
  	  	
  	iter = this.getObjects().iterator() ;
  	while ( ( ! result.isSizeLimitExceeded() ) && ( iter.hasNext() ) )
		{
			element = (AttributeReadAccess) iter.next();
			if ( filter.matches( element ) ) 
			{
				result.add( element ) ;
				if ( first )
					return ;
			}
		}
	} // collect() 
  
  // -------------------------------------------------------------------------
  	
 	/**
   * Returns all objects in this container that match the given rule.
   * 
   * @param rule A rule based on the attributes the contained objects must match
   * @param first if true then the result will only contain the first matching object
   */
  protected ObjectSearchResult find( MatchRule rule, boolean first ) 
	{
  	ObjectSearchResult result ;
  	  	
  	result = this.newResult( first ? 1 : 50 ) ;
  	this.collect( result, rule, first ) ;
  	return result ;
	} // find() 

	// -------------------------------------------------------------------------
   
 	/**
   * Returns all objects in this container that match the given filter.
   * 
   * @param filter A filter the contained objects must match
   * @param first if true then the result will only contain the first matching object
   */
  protected ObjectSearchResult find( IObjectFilter filter, boolean first ) 
	{
  	ObjectSearchResult result ;
  	  	
  	result = this.newResult( first ? 1 : 50 ) ;
  	this.collect( result, filter, first ) ;
  	return result ;
	} // find() 

	// -------------------------------------------------------------------------
   
  /**
   * Returns all object in this container that match the given rule.
   * The rule must comply to the MatchRule syntax of 
   * org.pf.text.DefaultMatchRuleParser.
   * 
   * @param rule A rule based on the attributes the contained objects must match
   * @param ignoreCase true if string matching should ignore case  
   * @see #org.pf.text.DefaultMatchRuleParser
   * @throws MatchRuleParseException For any error when parsing the given rule  
   */
  protected ObjectSearchResult find( String rule, boolean first, boolean ignoreCase ) 
  	throws MatchRuleParseException
	{
		MatchRule matchRule ;
		
		matchRule = this.parseMatchRule( rule, ignoreCase ) ;
		return this.find( matchRule, first ) ;
	} // find() 

	// -------------------------------------------------------------------------

  protected MatchRule parseMatchRule( String rule, boolean ignoreCase ) 
  	throws MatchRuleParseException
	{
		MatchRule matchRule ;
		
		matchRule = this.getRuleParser().parse( rule ) ;
		matchRule.ignoreCase( ignoreCase ) ;
		matchRule.ignoreCaseInNames( ignoreCase ) ;
		return matchRule ;
	} // parseMatchRule() 

	// -------------------------------------------------------------------------
  
  protected ObjectSearchResult newResult( int initialCapacity ) 
	{
  	if ( initialCapacity <= 1 )
		{
    	return new ObjectSearchResult(1) ;
		}
		else
		{
	  	return new ObjectSearchResult( 40 ) ;
		}
	} // newResult() 

	// -------------------------------------------------------------------------
  
  protected List newList( int initialCapacity ) 
	{
		return new ArrayList( initialCapacity ) ;
	} // newList() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns the parser that parses string rules to MatchRules.
   * 
   * Subclasses may override this method to provide a different parser, that
   * might support different rule syntax.
   */
  protected DefaultMatchRuleParser getRuleParser() 
	{
		return ruleParser ;
	} // getRuleParser() 

	// -------------------------------------------------------------------------
  // --- INNER CLASSES -------------------------------------------------------
  // -------------------------------------------------------------------------
  
  protected class DummyObject implements AttributeReadAccess
	{
		public String[] getAttributeNames()
		{
			return StringUtil.EMPTY_STRING_ARRAY ;
		} // getAttributeNames() 
		
		// -----------------------------------------------------------------------
		
		public Object getAttributeValue( String name ) throws NoSuchFieldException
		{
			return null;
		} // getAttributeValue() 
	} // class ObjectContainer
  
  // -------------------------------------------------------------------------
  
} // class ObjectContainer 
