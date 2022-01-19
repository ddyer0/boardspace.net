// ===========================================================================
// CONTENT  : CLASS SpyTreeNode
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 17/11/1999
// HISTORY  :
//  17/11/1999  duma  CREATED
//
// Copyright (c) 1999, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.tree.TreeNode;

/**
 * This is the abstract superclass for all types of tree nodes in a JavaSpy.
 *
 * @author Manfred Duchrow
 * @version 1.0
 * @since JDK 1.2
 */
abstract public class SpyTreeNode implements TreeNode
{ 
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private TreeNode parent = null ;
	public TreeNode getParent() { return parent ; }
	public void setParent( TreeNode newValue ) { parent = newValue ; }
	
	private Vector children = null ;
	public Vector getChildren() { return children ; }
	protected void setChildren( Vector newValue ) { children = newValue ; }    

  // =========================================================================
  // CLASS METHODS
  // =========================================================================
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public SpyTreeNode()
  {
  	this.setChildren( new Vector() ) ;
  } // SpyTreeNode()  

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns the receiver's underlying model object.
	 */
	abstract public Spy getModel() ;

	// -------------------------------------------------------------------------
	
	/**
	 * Adds the given tree node to the receiver's children.
	 */
	public void add( SpyTreeNode node )
	{
		this.getChildren().add( node ) ;
		node.setParent( this ) ;
	} // add()

	// -------------------------------------------------------------------------
	
	/**
	 * Returns the children of the receiver as an enumeration.
	 */
	public Enumeration children()
	{
		return ( this.getChildren().elements() ) ;
	} // children()

	// -------------------------------------------------------------------------

	/**
	 * Returns the number of children TreeNodes the receiver contains.
	 */
	public int getChildCount()
	{
		return ( this.getChildren().size() ) ;
	} // getChildCount()

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the receiver allows children.
	 */
	public boolean getAllowsChildren()
	{
		return true ;
	} // getAllowsChildren()

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the receiver is a leaf.
	 */
	public boolean isLeaf()
	{
		return ( this.getChildCount() == 0 ) ;
	} // isLeaf()

	// -------------------------------------------------------------------------

	/**
	 * Returns the child TreeNode at index childIndex.
	 */
	public TreeNode getChildAt( int childIndex )
	{
		if ( ( childIndex >= 0 ) && ( childIndex < this.getChildCount() ) )
			return ( (TreeNode)this.getChildren().get( childIndex ) ) ;		
		else
			return ( null ) ;
	} // getChildAt()

	// -------------------------------------------------------------------------

	/**
	 * Returns the index of node in the receivers children.  <br>
	 * If the receiver does not contain node, -1 will be returned.
	 */
	public int getIndex( TreeNode node )
	{
		return ( this.getChildren().indexOf( node ) ) ;		
	} // getIndex()

	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  // -------------------------------------------------------------------------
  
// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  // -------------------------------------------------------------------------
  
} // class SpyTreeNode