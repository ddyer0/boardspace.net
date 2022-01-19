// ===========================================================================
// CONTENT  : CLASS StringPatternCollection
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.9 - 21/01/2012
// HISTORY  :
//  29/09/2003  mdu  CREATED
//	06/02/2004	mdu	changed	-->	* matches empty string!
//	11/06/2004	mdu	added		-->	addPatterns()	
//	06/05/2005	mdu	added		-->	implements StringFilter
//	24/02/2006	mdu		changed	-> to extend AStringFilter rather than implementing StringFilter
//	21/01/2007	mdu		added		-> copy()
//	22/02/2008	mdu		added		-> matchesAll(), create(String[])
//	21/01/2012	mdu		added		-> implements IJSONConvertible
//
// Copyright (c) 2003-2012, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.pf.bif.text.IJSONConvertible;

/**
 * Holds a collection of StringPattern objects and allows to check if a
 * given string matches any or none of the patterns. 
 * <p>
 * <b>Attention: Since version 1.1 of this class there is an incompatibility
 * to its predecessors.<br>
 * It now creates StringPattern objects that match '*' to empty string.
 * That is, the pattern "Tom*" now matches string "Tom" which it doesn't 
 * before version 1.1. 
 * </b>
 *
 * @author Manfred Duchrow
 * @version 1.9
 */
public class StringPatternCollection extends AStringFilter implements IJSONConvertible
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final String DEFAULT_PATTERN_SEPARATOR = ";" ;
	
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private Collection<StringPattern> patterns = null ;
	protected Collection<StringPattern> getPatterns() { return patterns ; }
	protected void setPatterns( Collection<StringPattern> newValue ) { patterns = newValue ; }

  private Boolean ignoreCase = null ;
  /**
   * Returns whether or not the pattern matching ignores upper and lower case
   * Might return null if not specified.
   */
  protected Boolean getIgnoreCase() { return ignoreCase ; }  
  /**
   * Sets whether the pattern matching should ignore case or not
   */
  protected void setIgnoreCase( Boolean newValue ) { ignoreCase = newValue ; }  

  // =========================================================================
  // CLASS METHODS
  // =========================================================================
  /**
   * Returns a new instance with the given string patterns and the selection,
   * if case should be ignored when comparing characters.
   * <br>
   * The patterns must be separated by semi-colon ';'.
   *
   * @param patterns The pattern definitions.
   * @param ignoreCase Definition, if case sensitive character comparison or not.
   */
  public static StringPatternCollection create( String patterns, boolean ignoreCase ) 
	{
  	StringPatternCollection spc ;
  	
  	spc = new StringPatternCollection() ;
  	spc.addPatterns( patterns ) ;
  	spc.setIgnoreCase( ignoreCase ) ;
  	return spc ;
	} // create()
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns a new instance with the given string patterns and the selection,
   * if case should be ignored when comparing characters.
   *
   * @param patterns The pattern definitions.
   * @param ignoreCase Definition, if case sensitive character comparison or not.
   */
	public static StringPatternCollection create(String[] patterns, boolean ignoreCase)
	{
		StringPatternCollection spc;

		spc = new StringPatternCollection();
		spc.addPatterns(patterns);
		spc.setIgnoreCase(ignoreCase);
		return spc;
	} // create()

	// -------------------------------------------------------------------------

	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Initialize the new instance with default values.
	 */
	public StringPatternCollection()
	{
		super();
		this.reset();
	} // StringPatternCollection() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with one pattern.
	 */
	public StringPatternCollection(StringPattern pattern)
	{
		this();
		this.add(pattern);
	} // StringPatternCollection() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with a pattern array.
	 */
	public StringPatternCollection(String... strPatterns)
	{
		this();
		this.addPatterns(strPatterns);
	} // StringPatternCollection() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with a collection of patterns.
	 * 
	 * @param strPatterns A collection of String
	 */
	public StringPatternCollection(Collection strPatterns)
	{
		this();
		this.addPatterns(strPatterns);
	} // StringPatternCollection() 

  // -------------------------------------------------------------------------
 
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns true if the given string matches the filter otherwise false.
	 * If the given string is null false will be returned.
	 * 
	 * @param aString Any string or even null
	 */
	public boolean matches( String aString ) 
	{
		return this.matchesAny( aString ) ;
	} // matches() 
  
	// -------------------------------------------------------------------------
	
  /**
   * Returns true, if the given probe matches any of the patterns.
   * 
   * @param probe The string to match against all patterns
   */
  public boolean matchesAny( String probe )
	{
		Iterator iter ;
		StringPattern pattern ;
		
		if ( probe == null )
		{
			return false ;
		}
		
		iter = this.getPatterns().iterator() ;
		while ( iter.hasNext() )
		{
			pattern = (StringPattern)iter.next();
			if ( pattern.matches( probe ) )
			{
				return true ;
			}
		}
		return false ;
	} // matchesAny() 
 
	// -------------------------------------------------------------------------
	
	/**
	 * Returns true, if the given probe matches none of the patterns.
	 * 
	 * @param probe The string to check against all patterns
	 */
	public boolean matchesNone( String probe )
	{
		return ! this.matchesAny( probe ) ;
	} // matchesNone() 
 
	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the given probe matches all patterns in this collection.
	 * 
	 * @param probe The string to check against all patterns
	 */
	public boolean matchesAll( String probe )
	{
		Iterator iter ;
		StringPattern pattern ;
		
		if ( probe == null )
		{
			return false ;
		}		
		iter = this.getPatterns().iterator() ;
		while ( iter.hasNext() )
		{
			pattern = (StringPattern)iter.next();
			if ( !pattern.matches( probe ) )
			{
				return false ;
			}
		}
		return true ;
	} // matchesAll()
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns an array containing all of the given strings that matches any
	 * pattern of this pattern collection.
	 * If the input array is null then the result of this method is also null.
	 * 
	 * @param strings The strings to be matched against this pattern
	 */
	public String[] select( String[] strings )
	{
		return this.selectOrReject( strings, true ) ;
	} // select() 

	// -------------------------------------------------------------------------

	/**
	 * Returns an array containing all of the given strings that do NOT match 
	 * any pattern of this pattern collection.
	 * If the input array is null then the result of this method is also null.
	 * 
	 * @param strings The strings to be matched against this pattern
	 */
	public String[] reject( String[] strings )
	{
		return this.selectOrReject( strings, false ) ;
	} // reject() 

	// -------------------------------------------------------------------------

	/**
	 * Returns an array containing all of the given strings that matches any
	 * pattern of this pattern collection.
	 * If possible, the result collection is of the same type as the input collection.
	 * Otherwise it is an ArrayList.
	 * If the input collection is null then the result of this method is also null.
	 * 
	 * @param strings The strings to be matched against this pattern
	 */
	public Collection select( Collection strings )
	{
		return this.selectOrReject( strings, true ) ;
	} // select() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns a collection containing all of the given strings that do NOT match 
	 * any pattern of this pattern collection.
	 * If possible, the result collection is of the same type as the input collection.
	 * Otherwise it is an ArrayList.
	 * If the input collection is null then the result of this method is also null.
	 * 
	 * @param strings The strings to be matched against this pattern
	 */
	public Collection reject( Collection strings )
	{
		return this.selectOrReject( strings, false ) ;
	} // reject() 
	
	// -------------------------------------------------------------------------
	
  /**
   * Removes all patterns and starts with a new empty collection.
   */
	public void reset()
	{
		this.setPatterns(new ArrayList<StringPattern>()) ;
		this.setIgnoreCase( null ) ;
	} // reset() 
 
	// -------------------------------------------------------------------------

	/**
	 * Adds the given pattern to the collection of patterns, 
	 * if it is not yet there
	 * 
	 * @param aPattern The pattern to add
	 */
	public void add(StringPattern aPattern)
	{
		if (aPattern != null)
		{
			if (!this.getPatterns().contains(aPattern))
			{
				if (this.getIgnoreCase() != null)
				{
					aPattern.setIgnoreCase(this.getIgnoreCase().booleanValue());
				}
				this.getPatterns().add(aPattern);
			}
		}
	} // add() 
 
	// -------------------------------------------------------------------------

  /**
   * Adds all patterns in the given string which must be separated by ';'.
   * 
   * @param patternList A list of patterns separated by ';'
   */
	public void addPatterns(String patternList)
	{
		this.addPatterns(patternList, DEFAULT_PATTERN_SEPARATOR);
	} // addPatterns() 

	// -------------------------------------------------------------------------

  /**
   * Adds all patterns in the given string which must be separated by ';'.
   */
	public void addPatterns( String patternList, String separators )
	{
		String[] strPatterns ;
		
		if ( patternList != null )
		{
			strPatterns = this.str().substrings( patternList, separators ) ;
			this.addPatterns( strPatterns ) ;
		}
	} // addPatterns() 

	// -------------------------------------------------------------------------
	
  /**
   * Adds all patterns strings in the given collection.
   * @param strPatterns A collection of String
   */
	public void addPatterns( Collection<String> strPatterns )
	{
		if ( strPatterns != null )
		{
			this.addPatterns( this.str().asStrings( strPatterns ) ) ;
		}
	} // addPatterns() 

	// -------------------------------------------------------------------------
	
  /**
   * Adds all patterns from the given pattern collection.
   * @param patternCollection A pattern collection like this one
   */
	public void addPatterns( StringPatternCollection patternCollection )
	{
		Iterator<StringPattern> iter ;
		StringPattern element;
		
		if ( patternCollection != null )
		{
			iter = patternCollection.getPatterns().iterator() ;
			while ( iter.hasNext() )
			{
				element = iter.next();
				this.add( element ) ;
			}
		}
	} // addPatterns() 

	// -------------------------------------------------------------------------
	
  /**
   * Adds all patterns in the given array.
   */
	public void addPatterns(String[] patternCollection)
	{
		if (patternCollection != null)
		{
			for (int i = 0; i < patternCollection.length; i++)
			{
				this.add(patternCollection[i]);
			}
		}
	} // addPatterns() 

	// -------------------------------------------------------------------------
	
	/**
	 * Adds the given pattern as pattern to the collection of patterns, 
	 * if it is not yet there.
	 * <p>
	 * Since V1.1 the pattern internally created supports '*' matching empty
	 * strings (e.g. "AC" matches "A*C")! 
	 * 
	 * @param pattern The pattern to add
	 */
	public void add( String pattern )
	{
		if ( pattern != null )
		{
			this.add( this.newPattern( pattern ) ) ;
		}
	} // add() 
 
	// -------------------------------------------------------------------------	
	
	/**
	 * Removes the given pattern from the collection if it exists.
	 * The lookup is done by using the equals() method.
	 * 
	 * @param aPattern The pattern to remove
	 * @return true, if the pattern was found and removed, otherwise false 
	 */
	public boolean remove( StringPattern aPattern )
	{
		boolean removed = false ;
		
		if ( aPattern != null )
		{
			removed = this.getPatterns().remove( aPattern ) ;
		}
			
		return removed ;
	} // remove() 
 
	// -------------------------------------------------------------------------
	
	/**
	 * returns the number of patterns in this collection.
	 */
	public int size()
	{
		return this.getPatterns().size() ;
	} // size() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if no pattern definition is currently in this collection
	 */
	public boolean isEmpty()
	{
		return this.getPatterns().isEmpty() ;
	} // isEmpty() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns an array with all pattern strings contained in this object.
	 * If no pattern is contained an empty array will be returned. 
	 */
	public String[] getPatternStrings() 
	{
		String[] result ;
		StringPattern pattern ;
		Iterator<StringPattern> iter ;
		
		if ( this.isEmpty() )
		{
			return StringUtil.EMPTY_STRING_ARRAY ;
		}
		result = new String[this.size()] ;
		iter = this.getPatterns().iterator() ;
		for (int i = 0; i < result.length; i++ )
		{
			pattern = iter.next();
			result[i] = pattern.getPattern() ;
		}
		return result ;
	} // getPatternStrings() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns a copy of this pattern collection. The copy is indipendent of this
	 * object. That means, it can be modified without impact on this object.
	 */
	public StringPatternCollection copy() 
	{
		StringPatternCollection copy ;
		
		copy = new StringPatternCollection( this.getPatternStrings() ) ;
		copy.setIgnoreCase( this.getIgnoreCase() ) ;
		return copy ;
	} // copy() 
	
	// -------------------------------------------------------------------------
	
  /**
   * Sets whether all the patterns inside this collection should ignore case or not.
   * Be aware that this methods modifies all contained patterns according to
   * the given parameter. 
   * All patterns that are added to this collection afterwards will also be
   * set to the specified ignore case setting.
   * This behavior can only be switched off with calling reset().
   * 
   * @param ignore true if pattern matching should be case insensitive from now on
   */
  public void setIgnoreCase( boolean ignore ) 
  { 
  	Iterator iterator = this.getPatterns().iterator() ;
  	while ( iterator.hasNext() )
		{
			StringPattern pattern = (StringPattern)iterator.next();
			pattern.setIgnoreCase( ignore ) ;
		}
  	this.setIgnoreCase( ignore ? Boolean.TRUE : Boolean.FALSE ) ;
  } // setIgnoreCase() 

  // -------------------------------------------------------------------------
  
	public void appendAsJSONString(StringBuffer buffer)
	{
  	JSONUtil.current().appendJSONArray(buffer, this.getPatterns().toArray());
	} // appendAsJSONString()

	// -------------------------------------------------------------------------

  /**
   * Returns a JSON string representation of this object.
   * @return JSON object: {"string1":"string2"}
   */
  public String toJSON()
  {
  	return JSONUtil.current().convertToJSON(this);
  } // asJSONString()
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * Creates and returns a new pattern object out of the given string.
   * <br>
   * Subclasses may override this method to instantiate their own subclass
   * of StringPattern.
   */
	protected StringPattern newPattern( String pattern )
	{
		StringPattern strPattern ;
		
		strPattern = new StringPattern( pattern ) ;
		strPattern.multiCharWildcardMatchesEmptyString( true ) ;
		return strPattern ;
	} // newPattern() 
 
	// -------------------------------------------------------------------------

	protected String[] selectOrReject( String[] strings, boolean select )
	{
		List result ;
		
		if ( strings == null )
		{
			return null ;
		}
			
		result = new ArrayList( strings.length ) ;
		for (int i = 0; i < strings.length; i++)
		{
			if ( select == this.matchesAny( strings[i] ) )
			{
				result.add( strings[i] ) ;
			}
		}
		return this.str().asStrings( result ) ;	
	} // selectOrReject() 

	// -------------------------------------------------------------------------

	/**
	 * If possible, the result collection is of the same type as the input collection.
	 * Otherwise its an ArrayList.
	 */
	protected Collection selectOrReject( Collection strings, boolean select )
	{
		Collection result ;
		Iterator iter ;
		String str ;
		
		if ( strings == null )
		{
			return null ;
		}
		
		try
		{
			result = (Collection)strings.getClass().getDeclaredConstructor().newInstance() ;
		}
		catch ( Exception e )
		{
			result = new ArrayList(strings.size()) ;
		}
		for (iter = strings.iterator(); iter.hasNext(); )
		{
			str = (String)iter.next();
			if ( select == this.matchesAny( str ) )
			{
				result.add( str ) ;
			}
		}
		return result ;	
	} // selectOrReject() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the default separators for patterns
	 * @see #addPatterns(String)
	 */
	protected String getDefaultPatternSeparator() 
	{
		return DEFAULT_PATTERN_SEPARATOR ;
	} // getDefaultPatternSeparator() 

	// -------------------------------------------------------------------------
	
	protected StringUtil str()
	{
		return StringUtil.current() ;
	} // str() 

	// -------------------------------------------------------------------------
	
} // class StringPatternCollection 
