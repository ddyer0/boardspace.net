// ===========================================================================
// CONTENT  : CLASS PropertiesFileElement
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 17/09/2004
// HISTORY  :
//  23/07/2004  mdu  CREATED
//	17/09/2004	mdu		changed	-->	superclass from Object to NamedText
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.file;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.text.StringUtil;
import org.pf.util.NamedText;

/**
 * Represents one element in a properties file. That can be
 * <ul>
 * <li>An empty line
 * <li>A comment line starting with #
 * <li>A property setting x=y
 * </ul>
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
class PropertiesFileElement extends NamedText
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final String COMMENT_INDICATOR	= "#" ;
	protected static final String ASSIGNMENT_CHAR		= "=" ;

	protected static final int TYPE_UNKNOWN			= -1 ; 
	protected static final int TYPE_EMPTY				= 0 ; 
	protected static final int TYPE_COMMENT			= 1 ; 
	protected static final int TYPE_PROPERTY		= 2 ; 
	
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String line = null ;
  protected String getLine() { return line ; }
  protected void setLine( String newValue ) { line = newValue ; }
  
  private int type = TYPE_UNKNOWN ;
  protected int getType() { return type ; }
  protected void setType( int newValue ) { type = newValue ; }
      
  private boolean deleted = false ;
  protected boolean deleted() { return deleted ; }
  protected void deleted( boolean newValue ) { deleted = newValue ; }
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance from a single line.
   */
  protected PropertiesFileElement( String line )
  {
    super( null, null ) ;
    this.setLine( line ) ;
    this.init() ;
  } // PropertiesFileElement() 
  
  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance from a name/value pair.
   */
  protected PropertiesFileElement( String name, String value )
  {
    this( name.trim() + ASSIGNMENT_CHAR + value.trim() ) ;
  } // PropertiesFileElement() 
  
  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Replaces the old value by the given new one
   */
  public void changeValue( String newValue ) 
	{
  	String newLine ;
  	
		if ( this.isProperty() && ( newValue != null ) )
		{
			newLine = this.str().replaceAll( this.getLine(), this.text(), newValue ) ;
			this.setLine( newLine ) ;
			this.text( newValue ) ;
		}
	} // changeValue() 

	// -------------------------------------------------------------------------

  /**
   * Changes this property to a comment
   */
  public boolean becomeComment() 
	{
  	String newLine ;
  	
		if ( this.isProperty() )
		{
			newLine = COMMENT_INDICATOR + this.getLine() ;
			this.setLine( newLine ) ;
			this.init() ;
			return this.isComment() ;
		}
		return false ;
	} // becomeComment() 

	// -------------------------------------------------------------------------

  /**
   * Changes this comment to a property
   */
  public boolean becomeProperty() 
	{
  	String newLine ;
  	
		if ( this.isComment() )
		{
			newLine = this.str().suffix( this.getLine(), COMMENT_INDICATOR ) ;
			this.setLine( newLine ) ;
			this.init() ;
			return this.isProperty() ;
		}
		return false ;
	} // becomeProperty() 

	// -------------------------------------------------------------------------

  /**
   * Returns true if this is a property element
   */
  public boolean isProperty() 
	{
		return this.getType() == TYPE_PROPERTY ;
	} // isProperty() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns true if this is an empty element
   */
  public boolean isEmptyLine() 
	{
		return this.getType() == TYPE_EMPTY ;
	} // isEmptyLine() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns true if this is a comment element
   */
  public boolean isComment() 
	{
		return this.getType() == TYPE_COMMENT ;
	} // isComment() 

	// -------------------------------------------------------------------------
    
  /**
   * Marks this object as undeleted
   */
  public void undelete() 
	{
		this.deleted(false) ;
	} // undelete() 

	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected void init() 
	{
		String trimmedLine ;
		String[] keyValue ;
		
		trimmedLine = line.trim() ;
		if ( trimmedLine.length() == 0 )
		{
			this.setType( TYPE_EMPTY ) ;
			return ;
		}
		if ( trimmedLine.startsWith( COMMENT_INDICATOR ) )
		{
			this.setType( TYPE_COMMENT ) ;
			return ;
		}
		if ( trimmedLine.indexOf( ASSIGNMENT_CHAR ) < 0 )
		{
			this.setType( TYPE_UNKNOWN ) ;
			return ;
		}
		keyValue = this.str().splitNameValue( trimmedLine, ASSIGNMENT_CHAR ) ;
		this.name( keyValue[0].trim() ) ;
		this.text( keyValue[1].trim() ) ;
		this.setType( TYPE_PROPERTY ) ;
	} // init() 

	// -------------------------------------------------------------------------
  
  protected String inspectString() 
	{
		return this.getLine() ;
	} // inspectString() 

	// -------------------------------------------------------------------------
  
  protected StringUtil str()
	{
		return StringUtil.current();
	} // str() 

	//-------------------------------------------------------------------------
  
} // class PropertiesFileElement 
