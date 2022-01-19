// ===========================================================================
// CONTENT  : ABSTRACT CLASS MatchElement
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.5 - 20/12/2004
// HISTORY  :
//  11/07/2001  duma  CREATED
//  09/10/2001  duma  changed -> Made class public
//  08/01/2002  duma  changed -> Made serializable
//	14/08/2002	duma	changed	-> New constructor with no arguments
//	24/10/2003	duma	added		-> multiCharWildcardMatchesEmptyString(), ignoreCaseInName()
//	20/12/2004	duma	added		-> applyDatatypes()
//
// Copyright (c) 2001-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Map;
import java.io.Serializable ;

/**
 * Implements all common state and behaviour of elements in a match rule.
 *
 * @author Manfred Duchrow
 * @version 1.5
 */
abstract public class MatchElement implements Serializable
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private boolean and = true ;
  public boolean getAnd() { return and ; }
  public void setAnd( boolean newValue ) { and = newValue ; }

  private boolean not = false ;
  public boolean getNot() { return not ; }
  public void setNot( boolean newValue ) { not = newValue ; }

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public MatchElement()
  {
    super() ;
  } // MatchElement()

  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns true, if the attributes and their values in the given
   * dictionary comply to the rules of the receiver.
   *
   * @param dictionary The attribute-value pairs that have to be checked against the rules
   */
  public boolean matches( Map dictionary )
  {
    boolean ok  = this.doMatch( dictionary ) ;
    if ( this.getNot() )
      return ( ! ok ) ;
    else
      return ok ;
  } // matches()

  // -------------------------------------------------------------------------

  public boolean isGroup()
  {
    return false ;
  } // isGroup()

  // -------------------------------------------------------------------------

  public boolean isAttribute()
  {
    return false ;
  } // isAttribute()

  // -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

	abstract protected void multiCharWildcardMatchesEmptyString( boolean yesOrNo );
	
	// -------------------------------------------------------------------------

  abstract protected boolean doMatch( Map dictionary ) ;

  // -------------------------------------------------------------------------

	abstract protected void ignoreCaseInName( boolean newValue ) ;

	// -------------------------------------------------------------------------
	
  abstract protected void ignoreCase( boolean ignoreIt ) ;

  // -------------------------------------------------------------------------

  abstract protected void apply( MatchRuleVisitor visitor ) ;

  // -------------------------------------------------------------------------

  abstract protected void applyDatatypes( Map datatypes ) throws MatchRuleException;
  
  // -------------------------------------------------------------------------
  
} // class MatchElement