// ===========================================================================
// CONTENT  : CLASS PropertiesFileContent
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 17/09/2004
// HISTORY  :
//  23/07/2004  mdu  CREATED
//	17/09/2004	mdu		changed	-->	superclass from Object to OrderedProperties
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.file;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.pf.text.StringPattern;
import org.pf.util.NamedText;
import org.pf.util.OrderedProperties;

/**
 * Represents a properties collection with empty lines and comments.
 * The order will be preserved.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class PropertiesFileContent extends OrderedProperties
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private List commentIndex = null ;
  protected List getCommentIndex() { return commentIndex ; }
  protected void setCommentIndex( List newValue ) { commentIndex = newValue ; }
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public PropertiesFileContent()
  {
    super() ;
    this.setElements( new ArrayList(INITIAL_CAPACITY) ) ;
    this.setPropertyIndex( new HashMap(INITIAL_CAPACITY) ) ;
    this.setCommentIndex( new ArrayList(INITIAL_CAPACITY) ) ;
  } // PropertiesFileContent() 

  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with an initial capacity.
   */
  public PropertiesFileContent(  int initialCapacity )
  {
    super( initialCapacity ) ;
    this.setCommentIndex( new ArrayList( initialCapacity ) ) ;
  } // PropertiesFileContent() 

  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Adds the given line at the end of all contained elements
   */
  public void addLine( String line ) 
	{
		PropertiesFileElement element ;
		
		if ( line != null )
		{
			element = new PropertiesFileElement( line ) ;
			this.addElement( element ) ;
		}
	} // addLine() 

	// -------------------------------------------------------------------------
  
  /**
   * Modifies the value or adds the property specified by the given name.
   * Returns true if the property was found and modified or added, 
   * otherwise false.
   * 
   * @param name The name of the property to set
   * @param value The new value to be set
   */
  public Object setProperty( String name, String value ) 
	{
		PropertiesFileElement property ;
		String oldValue ;
		
		property = this.findPropertyElement( name ) ;
		if ( property == null )
		{
			this.appendProperty( name, value ) ;
			return null ;
		}
		oldValue = property.text() ;
		property.changeValue( value ) ;
		return oldValue ;
	} // setProperty() 

	// -------------------------------------------------------------------------
  
  /**
   * Modifies the value of the property specified by the given name.
   * Returns true if the property was found and modified, otherwise false.
   * 
   * @param name The name of the property to modify
   * @param newValue The new value to be set
   */
  public boolean modifyProperty( String name, String newValue ) 
	{
		PropertiesFileElement property ;
		
		property = this.findPropertyElement( name ) ;
		if ( this.isNullOrDeleted( property ) )
		{
			return false ;
		}
		property.changeValue( newValue ) ;
		return true ;
	} // modifyProperty() 

	// -------------------------------------------------------------------------
  
	/**
   * Adds the property with the specified name and value.
   * Returns true if the property was added, otherwise false.
   * In particular it will not be added if a property with the same name
   * already exists (returns false then). 
   * 
   * @param name The name of the property to add
   * @param value The value to be set
   */
  public boolean addProperty( String name, String value ) 
	{
		PropertiesFileElement property ;
		
		property = this.findPropertyElement( name ) ;
		if ( ! this.isNullOrDeleted( property ) )
		{
			return false ;
		}
		return this.appendProperty( name, value ) ;
	} // addProperty() 

	// -------------------------------------------------------------------------
    
  /**
   * Turns the property specified by the given name to a comment.
   * Returns true if the property was found and changed to a comment, 
   * otherwise false.
   * 
   * @param name The name of the property to change to be a comment
   */
  public boolean commentProperty( String name ) 
	{
		PropertiesFileElement property ;
		Integer index ;
		boolean success ;
		
		property = this.findPropertyElement( name ) ;
		if ( this.isNullOrDeleted( property ) )
		{
			return false ;
		}
		index = this.indexOfProperty( name ) ;
		this.getPropertyIndex().remove( name ) ;
		success = property.becomeComment() ;
		this.registerComment( index ) ;
		return success ;
	} // commentProperty() 

	// -------------------------------------------------------------------------
  
  /**
   * Changes a comment containing the property specified by the given name 
   * to a real property setting, by removing the leading comment indicator '#'.
   * Returns true if the property was found in a comment and changed to 
   * a non-comment, otherwise false.
   * 
   * @param name The name of the property to change from a comment to a property
   */
  public boolean uncommentProperty( String name ) 
	{
  	Integer[] index ;
  	StringPattern pattern ;
  	PropertiesFileElement comment ;
  	boolean success ;
  	
  	// Sample pattern: "#*property.name*=*"
  	pattern = StringPattern.create( PropertiesFileElement.COMMENT_INDICATOR + "*"
  			 							+ name + "*" + PropertiesFileElement.ASSIGNMENT_CHAR + "*" ) ;
  	index = (Integer[])this.getCommentIndex().toArray(new Integer[0]) ;
  	for (int i = 0; i < index.length; i++ )
		{
  		comment = this.elementAt( index[i] ) ;
  		if ( pattern.matches( comment.getLine() ) )
			{
  			this.getCommentIndex().remove( index[i] ) ;
				success = comment.becomeProperty() ; 
				this.registerProperty( comment, index[i] ) ;
				return success ;
			}
		}
		return false ;
	} // uncommentProperty() 

	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * Create a new property object with the given name and value. Subclasses
   * may override this to implement their own property class.
   */
  protected NamedText newProperty( String name, String value )
	{
		return new PropertiesFileElement( name, value );
	} // newProperty() 
  
  // -------------------------------------------------------------------------
  
  protected void addElement( NamedText element ) 
	{
  	Integer index ;
  	PropertiesFileElement propertyElement ;
  	
  	propertyElement = (PropertiesFileElement)element ;
  	index = Integer.valueOf( this.size() ) ;
		this.getElements().add( propertyElement ) ;
		if ( propertyElement.isProperty() )
		{
			this.registerProperty( propertyElement, index );
		} 
		else if ( propertyElement.isComment() )
		{
			this.registerComment( index ) ;
		}
	} // addElement() 

	// -------------------------------------------------------------------------
  
  protected PropertiesFileElement findPropertyElement( String key ) 
	{
		return (PropertiesFileElement)this.findProperty( key ) ;
	} // findPropertyElement() 

	// -------------------------------------------------------------------------
  
  protected PropertiesFileElement elementAt( Integer index ) 
	{
		return this.elementAt( index.intValue() ) ;
	} // elementAt() 

	// -------------------------------------------------------------------------
  
  protected PropertiesFileElement elementAt( int index ) 
	{
		return (PropertiesFileElement)this.propertyAt( index ) ;
	} // elementAt() 

	// -------------------------------------------------------------------------
  
	protected void registerComment( Integer index ) 
	{
  	this.getCommentIndex().add( index ) ;
	} // registerComment() 

	// -------------------------------------------------------------------------
  
	protected boolean isValidProperty( NamedText property ) 
	{
		if ( property instanceof PropertiesFileElement )
		{
			PropertiesFileElement prop ;
			prop = (PropertiesFileElement)property ;
			return prop.isProperty() ;
		}
		return super.isValidProperty( property ) ;
	} // isValidProperty() 

	// -------------------------------------------------------------------------
	
} // class PropertiesFileContent 
