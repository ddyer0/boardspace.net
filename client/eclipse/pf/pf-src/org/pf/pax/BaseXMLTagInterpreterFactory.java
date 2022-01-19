// ===========================================================================
// CONTENT  : CLASS BaseXMLTagInterpreterFactory
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.0 - 28/02/2002
// HISTORY  :
//  11/07/1999 	duma  CREATED
//	28/02/2002	duma	changed	-> Support for SAX 2.0
//
// Copyright (c) 1999-2002, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.pax;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Hashtable;
import java.util.Map;

import org.xml.sax.SAXException;

/**
 * This is the basic implemenatation of a registry of tag interpreter classes
 * an their associated tag names.
 *
 * @author Manfred Duchrow
 * @version 2.0
 */
public abstract class BaseXMLTagInterpreterFactory
		implements XMLTagInterpreterFactory
{
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private Map classNameRegistry = new Hashtable() ;
  /** Returns the internal mapping of tag names to their corresponding
   * interpreter classes.    <br>
   * key : tag name (String)  <br>
   * value : tag interpreter class (Class)
   */
  protected Map getClassNameRegistry() { return classNameRegistry ; }  
  /** Sets the internal mapping of tag names to their corresponding
   * interpreter classes.   <br>
   * key : tag name (String)  <br>
   * value : tag interpreter class (Class)
   */
  protected void setClassNameRegistry( Map aValue ) { classNameRegistry = aValue ; }  

  private Map classRegistry = new Hashtable() ;
  /** Returns the internal mapping of tag names to their corresponding
   * interpreter class names.    <br>
   * key : tag name (String)  <br>
   * value : tag interpreter class name (String)
   */
  protected Map getClassRegistry() { return classRegistry ; }  
  /** Sets the internal mapping of tag names to their corresponding
   * interpreter class names.    <br>
   * key : tag name (String)  <br>
   * value : tag interpreter class name (String)
   */
  protected void setClassRegistry( Map aValue ) { classRegistry = aValue ; }  

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns an instance of the tag interpreter class associated that
   * is associated to the specified tag name.
   *
   * @param tagName The name of the tag, the interpreter is looked up for.
   * @return The tag interpreter instance or null, if the tag isn't supported.
   */
  public XMLTagInterpreter getInterpreterFor( String tagName )
  		throws SAXException
  {
	Class interpreterClass        = null ;
	XMLTagInterpreter interpreter = null ;

	interpreterClass = this.getInterpreterClassFor( tagName ) ;
	if ( interpreterClass != null )
	{
	  try
	  {
		interpreter = (XMLTagInterpreter)interpreterClass.getDeclaredConstructor().newInstance() ;
	  }
	  catch ( Exception ex )
	  {
	  	String msg = "Could not create new instance of interpreter class '" ;
	  	msg = msg + interpreterClass.getName() + "'\n" ;
	  	msg = msg + ex.toString() ;
		throw ( new SAXException( msg ) ) ;
	  }
	}
	return interpreter ;
  } // getInterpreterFor()  

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * Register a tag interpreter class name for a particular tag name.   <br>
   *
   * @param tagName The name of the tag, the interpreter class is responsible for.
   * @param className The full class name (with package !) for the interpreter class.
   */
  protected void registerTagInterpreter( String tagName, String className )
  {
	this.getClassNameRegistry().put( tagName, className ) ;
  } // registerTagInterpreter()  

  // -------------------------------------------------------------------------

  /**
   * Returns a tag interpreter class associated with the given tag name.
   *
   * @param tagName The name of the tag, the interpreter is searched for.
   * @return The tag interpreter instance or null, if the tag isn't supported.
   */
  protected Class getInterpreterClassFor( String tagName )
  			throws SAXException
  {
	String className       = null ;
	Class interpreterClass = null ;

	interpreterClass = (Class)this.getClassRegistry().get( tagName ) ;
	if ( interpreterClass == null )
	{
	  className = (String)this.getClassNameRegistry().get( tagName ) ;
	  if ( className != null )
	  {
		try
		{
		  interpreterClass = Class.forName( className ) ;
		  this.getClassRegistry().put( tagName, interpreterClass ) ;
		}
		catch ( Exception ex )
		{
			throw ( new SAXException( "Interpreter class '" + className + "' not found !" ) ) ;
		}
	  }
	}
	return interpreterClass ;
  } // getInterpreterClassFor()  

  // -------------------------------------------------------------------------

// -------------------------------------------------------------------------

} // class BaseXMLTagInterpreterFactory