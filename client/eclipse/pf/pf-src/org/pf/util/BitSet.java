// ===========================================================================
// CONTENT  : CLASS BitSet
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 09/03/2000
// HISTORY  :
//  09/03/2000  duma  CREATED
//
// Copyright (c) 2000, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util;

// ===========================================================================
// CONTENT  : CLASS BitSet
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 09/03/2000
// HISTORY  :
//  09/03/2000  duma  CREATED
//
// Copyright (c) 2000, by Manfred Duchrow. All rights reserved.
// ===========================================================================
// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Represents a set of 8 bits and provides methods for convenient
 * bit manipulation.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class BitSet
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  private static final int LOW_BIT        = 0 ;
  private static final int HIGH_BIT       = 7 ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private int bits = 0 ;
  protected int getBits() { return bits ; }  
  protected void setBits( int newValue ) { bits = newValue ; }  

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initializes the new instance with the default value 0.
   */
  public BitSet()
  {
	this( 0 ) ;
  } // BitSet()  

  // -------------------------------------------------------------------------

  /**
   * Initializes the new instance with the given value.
   */
  public BitSet( int initialValue )
  {
	if ( ( initialValue >= 0 ) && ( initialValue <= 255 ) )
	  this.setBits( initialValue ) ;
  } // BitSet()  

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  /**
   * Returns the byte value, that is the combination of the 8 bits.
   */
  public int getValue()
  {
	return this.getBits() ;
  } // getValue()  

  // -------------------------------------------------------------------------

  /**
   * Returns true, if the bit defined by the argument is set.
   * @param bit The bit to test ( 0 - 7 )
   */
  public boolean isBitSet( int bit )
  {
	int andValue  = 1 << bit ;
	return ( ( this.getBits() & andValue ) > 0 ) ;
  } // isBitSet()  

  // -------------------------------------------------------------------------

  /**
   * Returns true, if the bit defined by the argument is not set.
   * @param bit The bit to test ( 0 - 7 )
   */
  public boolean isBitNotSet( int bit )
  {
	return ( ! this.isBitSet( bit ) ) ;
  } // isBitNotSet()  

  // -------------------------------------------------------------------------

  /**
   * Sets the bit defined by the argument.
   * @param bit The bit to manipulate ( 0 - 7 )
   */
  public void setBit( int bit )
  {
	int orValue  = 1 << bit ;
	this.setBits( this.getBits() | orValue ) ;
  } // setBit()  

  // -------------------------------------------------------------------------

  /**
   * Unsets the bit defined by the argument.
   * @param bit The bit to manipulate ( 0 - 7 )
   */
  public void unsetBit( int bit )
  {
	int i         = 0 ;
	int andValue  = 0 ;

	for ( i = LOW_BIT ; i <= HIGH_BIT ; i++ )
	{
	  if ( i != bit )
		andValue = andValue | ( 1 << i ) ;
	}

	this.setBits( this.getBits() & andValue ) ;
  } // unsetBit()  

  // -------------------------------------------------------------------------

  /**
   * Switches a set bit to unset and a unset bit to set.
   * @param bit The bit to toggle ( 0 - 7 )
   */
  public void toggleBit( int bit )
  {
	if ( this.isBitSet( bit ) )
	  this.unsetBit( bit ) ;
	else
	  this.setBit( bit ) ;
  } // toggleBit()  

  // -------------------------------------------------------------------------

  /**
   * Returns the string representation of the receiver.
   */
  public String toString()
  {
	int i                 = 0 ;
	StringBuffer buffer   = null ;

	buffer = new StringBuffer() ;

	for ( i = HIGH_BIT ; i >= LOW_BIT ; i-- )
	  buffer.append( this.isBitSet(i) ? "1" : "0" ) ;

	return buffer.toString() ;
  } // toString()  

  // -------------------------------------------------------------------------

  /**
   * Returns true, if the given arguments equals the receiver.
   */
  public boolean equals( Object obj )
  {
	if ( obj instanceof BitSet )
	  return ( ((BitSet)obj).getValue() == this.getValue() ) ;

	return false ;
  } // equals()  

  // -------------------------------------------------------------------------

  /**
   * Returns the receiver's hash value.
   */
  public int hashCode()
  {
	return this.getValue() ;
  } // hashCode()  

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

} // class BitSet