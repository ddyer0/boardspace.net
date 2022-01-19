// ===========================================================================
// CONTENT  : INTERFACE MatchRuleVisitor
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 27/12/2001
// HISTORY  :
//  17/08/2001  duma  CREATED
//	27/12/2002	duma	changed	->	Support of different operators in attributes
//
// Copyright (c) 2001-2002, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Implementers of this interface can be used with MatchRule.apply()
 * to navigate through the parsed rule tree and execute specific
 * tasks on the elements.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public interface MatchRuleVisitor
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  /**
   * This method will be called right before the MatchRule walks
   * through its elements.
   */
  public void walkThroughInit() ;

  // -------------------------------------------------------------------------

  /**
   * This method will be called when the MatchRule has finished to walk
   * through its elements.
   */
  public void walkThroughFinished() ;

  // -------------------------------------------------------------------------

  /**
   * This method will be called for each start of a new group.
   *
   * @param andOperator If true it is an AND combination otherwise it is OR
   * @param notOperator Is only true for a NOT operation
   */
  public void startGroup( boolean andOperator, boolean notOperator ) ;

  // -------------------------------------------------------------------------

  /**
   * This method will be called for each group end occurence.
   */
  public void endGroup() ;

  // -------------------------------------------------------------------------

  /**
   * This method will be called for each attribute.
   *
   * @param name The attribute's name
   * @param compareOperator The operator used to compare values
   * @param values All values the attrubute my match (implicit OR combination !)
   * @param andOperator If true it is an AND combination otherwise it is OR
   * @param notOperator Is only true for a NOT operation
   */
  public void attribute( String name, int compareOperator, String[] values, 
                          boolean andOperator, boolean notOperator ) ;

  // -------------------------------------------------------------------------

} // interface MatchRuleVisitor