// ===========================================================================
// CONTENT  : CLASS ExtendedFileFilter
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.3 - 25/02/2006
// HISTORY  :
//  24/01/2000  duma  CREATED
//	14/02/2003	duma	added		->	Support for digit wildcard characher
//	21/12/2005	duma	added		->	addPatterns()
//	25/02/2006	mdu		added		->	implements IObjectFilter
//
// Copyright (c) 2000-2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.file;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.pf.bif.filter.IObjectFilter;
import org.pf.text.StringPattern;
import org.pf.text.StringUtil;

/**
 * This filter implements the standard pattern matching on UNIX and Windows
 * platforms. It supports the wildcards '*' and '?' on file names.  <br>
 * It allows to set more than one pattern.
 * Apart from that it allows control over inclusion/exclusion of directories 
 * independently from name patterns.
 *
 * @author Manfred Duchrow
 * @version 1.3
 */
public class ExtendedFileFilter implements FilenameFilter, IObjectFilter
{ 
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	/**
   * The character to be used to separate filename patterns (';') as String.
   */
  public static final String PATTERN_SEPARATOR		  = ";" ;
	
	protected final static int DIR_CHECK_NAME		= 1 ;
	protected final static int DIR_INCLUDE			= 2 ;
	protected final static int DIR_EXCLUDE			= 3 ;
		
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private List stringPatterns = new ArrayList() ;
  protected List getStringPatterns() { return stringPatterns ; }  
  protected void setStringPatterns( List newValue ) { stringPatterns = newValue ; }  
	
  private int dirHandling = DIR_CHECK_NAME ;
  protected int getDirHandling() { return dirHandling ; }  
  protected void setDirHandling( int newValue ) { dirHandling = newValue ; }  
		
	private boolean restrictiveMode = false ;
	protected boolean getRestrictiveMode() { return restrictiveMode ; }
	protected void setRestrictiveMode( boolean newValue ) { restrictiveMode = newValue ; }
			
  private Character digitWildcard = null ;
  protected Character getDigitWildcard() { return digitWildcard ; }
  protected void setDigitWildcard( Character newValue ) { digitWildcard = newValue ; }	
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================

  /**
   * Initialize the new instance with default values.
   * <br>
   * The default for "restrictive" is false! See other constructor for 
   * explanation.
   */
  public ExtendedFileFilter()
  {
  	super() ;
  } // ExtendedFileFilter() 

	// -------------------------------------------------------------------------

  /**
   * Initialize the new instance with patterns.
   * 
   * @param patternList A list of patterns (may be just one)
   * @see #addPatterns(String) 
   */
  public ExtendedFileFilter( String patternList )
  {
  	super() ;
  	this.addPatterns( patternList ) ;
  } // ExtendedFileFilter() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with a wildcard character for digits.
   * <br>
   * The default for "restrictive" is false! See other constructor for 
   * explanation.
   * 
   * @param wildcardForDigits The character that stands for any digit (0-9) in a filename
   */
  public ExtendedFileFilter( char wildcardForDigits )
  {
  	super() ;
  	this.setDigitWildcard( Character.valueOf( wildcardForDigits ) ) ;
  } // ExtendedFileFilter() 
  
  // -------------------------------------------------------------------------
  
	/**
	 * Initialize the new instance with the option of a more restrictive handling
	 * of wildcard matching. If the argument restrictive is true, empty strings
	 * won't match the '*' wildcard character.
	 * <p>
	 * Example:
	 * restrictive=true  => "text.doc" does NOT match the pattern "text*.doc"
	 * <br>
	 * restrictive=false => "text.doc" does match the pattern "text*.doc"
	 * 
	 * @param restrictive To restrict the match of '*' to at least one character
	 */
	public ExtendedFileFilter( boolean restrictive )
	{
		super() ;
		this.setRestrictiveMode( restrictive ) ;
	} // ExtendedFileFilter() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with a wildcard character for digits in 
	 * filenames and the option of a more restrictive handling
	 * of wildcard matching. If the argument restrictive is true, empty strings
	 * won't match the '*' wildcard character.
	 * <p>
	 * See other constructor for examples.
	 * 
	 * @param wildcardForDigits The character that stands for any digit (0-9) in a filename
	 * @param restrictive To restrict the match of '*' to at least one character
	 */
	public ExtendedFileFilter( char wildcardForDigits, boolean restrictive )
	{
		this( wildcardForDigits ) ;
		this.setRestrictiveMode( restrictive ) ;
	} // ExtendedFileFilter() 
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

	/**
	 * Adds a pattern. All filenames match this pattern are acceptable.   <br>
	 * Case sensitivity is switched on !
	 * 
	 * @param pattern The pattern string containing  optional wildcards ( '*', '?' ) 
	 */
	public void addPattern( String pattern )
	{
		StringPattern stringPattern		= null ;
		
		stringPattern = this.createStringPattern( pattern, false ) ;
		this.getStringPatterns().add( stringPattern ) ;
	} // addPattern() 
  
  // -------------------------------------------------------------------------

	/**
	 * Adds a pattern. All filenames match this pattern are acceptable.   <br>
	 * Case sensitivity is switched on !
	 * The second parameter specifies a character that will be recognized in the
	 * pattern as a placeholder for a single digit character.  <p>
	 * A patterb "XX-####.log" with a digitWildcard set to '#' wil match to
	 * "XX-2000.log" and "XX-7376.log" but not to "XX-C363.log" and "XX-dddd.log".
	 * 
	 * @param pattern The pattern string containing  optional wildcards ( '*', '?' ) 
	 * @param digitWildcardChar The character that will be treated as wildcard for digits ('0'-'9')
	 */
	public void addPattern( String pattern, char digitWildcardChar )
	{
		StringPattern stringPattern		= null ;
		
		stringPattern = this.createStringPattern( pattern, false ) ;
		stringPattern.setDigitWildcardChar( digitWildcardChar ) ;
		this.getStringPatterns().add( stringPattern ) ;
	} // addPattern() 
  
  // -------------------------------------------------------------------------

	/**
	 * Adds a pattern. All filenames match this pattern are acceptable.
	 * 
	 * @param pattern The pattern string containing  optional wildcards ( '*', '?' ) 
	 * @param ignoreCase If true, all character comparisons are ignoring uppercase/lowercase
	 */
	public void addPattern( String pattern, boolean ignoreCase )
	{
		StringPattern stringPattern		= null ;
		
		stringPattern = this.createStringPattern( pattern, ignoreCase ) ;
		this.getStringPatterns().add( stringPattern ) ;
	} // addPattern() 
  
  // -------------------------------------------------------------------------

	/**
	 * Adds a pattern. All filenames that match this pattern are acceptable.
	 * Additionally to the standard wildcards '*' and '?' a wildcard for single
	 * digit characters ('0' - '9') can be specified here.
	 * 
	 * @param pattern The pattern string containing  optional wildcards ( '*', '?' ) 
	 * @param ignoreCase If true, all character comparisons are ignoring uppercase/lowercase
	 * @param digitWildcardChar The character that will be treated as wildcard for digits ('0'-'9')
	 */
	public void addPattern( String pattern, boolean ignoreCase, char digitWildcardChar )
	{
		StringPattern stringPattern		= null ;
		
		stringPattern = this.createStringPattern( pattern, ignoreCase ) ;
		stringPattern.setDigitWildcardChar( digitWildcardChar ) ;
		this.getStringPatterns().add( stringPattern ) ;
	} // addPattern() 
  
  // -------------------------------------------------------------------------

	/**
	 * Adds one or more patterns separated by semi-colon (';').
	 * 
	 * @param patternList The list of filename patterns
	 */
	public void addPatterns( String patternList ) 
	{
		String[] parts ;
		
		if ( this.str().isNullOrEmpty( patternList ) )
		{
			return ;
		}
		
		parts = this.extractPatterns( patternList ) ;
		for (int i = 0; i < parts.length; i++)
		{
			if ( this.hasDigitWildcard() )
			{
				this.addPattern( parts[i], true, this.getDigitWildcardChar() ) ;
			}
			else
			{
				this.addPattern( parts[i], true ) ;
			}
		}
	} // addPatterns() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Sets the filter to only accept directories that match a defined pattern.
	 */
	public void checkNameOfDirectories()
	{
		this.setDirHandling( DIR_CHECK_NAME ) ;
	} // checkNameOfDirectories() 
  
  // -------------------------------------------------------------------------

	/**
	 * Sets the filter to always accept directories, even if they don't match
	 * a given pattern.
	 */
	public void alwaysIncludeDirectories()
	{
		this.setDirHandling( DIR_INCLUDE ) ;
	} // alwaysIncludeDirectories() 
  
  // -------------------------------------------------------------------------

	/**
	 * Sets the filter to never accept directories.
	 */
	public void alwaysExcludeDirectories()
	{
		this.setDirHandling( DIR_EXCLUDE ) ;
	} // alwaysExcludeDirectories() 
  
  // -------------------------------------------------------------------------

  /**
   * Tests if a specified file should be included in a file list.
   *
   * @param dir the directory in which the file was found.
   * @param name the name of the file.
   * @return true if and only if the name should be included in the file list, false otherwise.
   */
	public boolean accept( File dir, String name )
	{		
		File fileOrDir ;
		boolean accepted ;
		
		fileOrDir = new File( dir, name ) ;
		if ( fileOrDir.isDirectory() )
		{
			if ( this.mustIncludeDirectories() )
			{
				this.postAcceptCheck( dir, name, fileOrDir, true ) ;
				return true ;
			}
			if ( this.mustExcludeDirectories() )
			{
				this.postAcceptCheck( dir, name, fileOrDir, false ) ;
				return false ;
			}
		}
		accepted = this.checkAgainstPatterns( name ) ;
		this.postAcceptCheck( dir, name, fileOrDir, accepted ) ;
		return accepted ;
	} // accept() 
		
	// -------------------------------------------------------------------------

	/**
	 * Returns if this filter matches the given object.
	 * Currently the supported object types are java.lang.String (i.e. a filename),
	 * java.io.File, org.pf.file.FileLocator (not for remote files), org.pf.file.FileInfo.
	 * 
	 *  @param object Any object
	 *  @return true if the object matches this filter
	 */
	public boolean matches( Object object ) 
	{
		if ( object instanceof String )
		{
			return this.matchesFile( new File( (String)object ) ) ;
		}
		if ( object instanceof File )
		{
			return this.matchesFile( (File)object ) ;
		}
		if ( object instanceof FileInfo )
		{
			return this.matchesFile( new File( ((FileInfo)object ).getFullName() ) ) ;
		}
		if ( object instanceof FileLocator )
		{
			FileLocator locator = (FileLocator)object ;
			if ( ! locator.isRemote() )
			{
				return this.matchesFile( new File( locator.getOriginalFileName() ) ) ;
			}
		}
		return false ;
	} // matches() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the filter always accepts directories, even if they don't match
	 * a given pattern.
	 */
	public boolean mustIncludeDirectories()
	{
		return ( this.getDirHandling() == DIR_INCLUDE ) ;
	} // mustIncludeDirectories() 
  
	// -------------------------------------------------------------------------

	/**
	 * Returns true if the filter never accepts directories.
	 */
	public boolean mustExcludeDirectories()
	{
		return ( this.getDirHandling() == DIR_EXCLUDE ) ;
	} // mustExcludeDirectories() 
  
	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	/**
	 * This method gets called after the accept check had happened.
	 * Here it does nothing, but subclasses may override to do things like
	 * logging or monitoring.
	 * 
	 * @param dir The directory in which the file is located
	 * @param filename The name of the file without path 
	 * @param file The combined directory and filename 
	 * @param accepted The result of the acceptance check
	 */
	protected void postAcceptCheck( File dir, String filename, File file, boolean accepted ) 
	{
		
	} // postAcceptCheck() 
	
	// -------------------------------------------------------------------------
	
	protected boolean checkAgainstPatterns( String name )
	{
		Iterator iterator			= null ;
		StringPattern pattern	= null ;
		
		iterator = this.getStringPatterns().iterator() ;
		while ( iterator.hasNext() )
		{
			pattern = (StringPattern)iterator.next() ;
			if ( pattern.matches( name ) )
				return true ;
		} // while
		
		return false ; // No pattern matched
	} // checkAgainstPatterns() 

  // -------------------------------------------------------------------------

	protected StringPattern createStringPattern( String pattern, boolean ignoreCase )
	{
		StringPattern strPattern ;
		
		strPattern = new StringPattern( pattern, ignoreCase ) ;
		strPattern.multiCharWildcardMatchesEmptyString( ! this.getRestrictiveMode() ) ;
		return strPattern ;
	} // createStringPattern() 

	// -------------------------------------------------------------------------

	protected String[] extractPatterns( String pattern )
	{
		return this.str().parts( pattern, PATTERN_SEPARATOR ) ;
	} // extractPatterns() 

  // -------------------------------------------------------------------------

	protected char getDigitWildcardChar()
	{
		if ( this.hasDigitWildcard() )
			return this.getDigitWildcard().charValue() ;
		else
			return '\0' ;
	} // getDigitWildcardChar() 

	// -------------------------------------------------------------------------
	
	protected boolean hasDigitWildcard()
	{
		return this.getDigitWildcard() != null ;
	} // hasDigitWildcard() 

	// -------------------------------------------------------------------------   
	
	protected boolean matchesFile( File file ) 
	{
		File dir ;
		String filename ;
		
		dir = file.getParentFile() ;
		filename = file.getName() ;
		return this.accept( dir, filename ) ;
	} // matchesFile() 
	
	// -------------------------------------------------------------------------
	
	protected StringUtil str()
	{
		return StringUtil.current() ;
	} // str() 
	
	// -------------------------------------------------------------------------
	
} // class ExtendedFileFilter 
