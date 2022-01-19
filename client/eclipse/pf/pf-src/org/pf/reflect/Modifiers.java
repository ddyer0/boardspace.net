// ===========================================================================
// CONTENT  : CLASS Modifiers
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 13/01/2008
// HISTORY  :
//  13/01/2008  mdu  CREATED
//
// Copyright (c) 2008, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.reflect ;

import java.lang.reflect.Modifier;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * This class provides methods to conveniently set/unset modifier bits
 * without having to fiddle around with bit logic.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class Modifiers
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	/**
	 * String constant for "public".
	 */
	public static final String VIS_PUBLIC = "public" ;
	/**
	 * String constant for "protected".
	 */
	public static final String VIS_PROTECTED = "protected" ;
	/**
	 * String constant for "private".
	 */
	public static final String VIS_PRIVATE = "private" ;
	/**
	 * String constant for "".
	 */
	public static final String VIS_DEFAULT = "" ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private int bits = 0 ;
  /**
   * Return the bits as they are currently set.
   */
  public int getBits() { return bits ; }
  protected void setBits( int newValue ) { bits = newValue ; }

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default value 0.
   */
  public Modifiers()
  {
    super() ;
  } // Modifiers() 

  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with given value.
   */
  public Modifiers( int initialValue )
  {
  	super() ;
  	this.setBits( initialValue ) ;
  } // Modifiers() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Resets the modifier to 0.
   */
  public void reset() 
	{
		this.setBits( 0 ) ;
	} // reset() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Return <tt>true</tt> if the modifiers includes the <tt>abstract</tt> modifier, 
   * false otherwise.
   */
  public boolean isAbstract() 
	{
		return Modifier.isAbstract( this.getBits() ) ;
	} // isAbstract() 
	
	// -------------------------------------------------------------------------

  /**
   * Return <tt>true</tt> if the modifiers includes the <tt>final</tt> modifier, 
   * false otherwise.
   */
  public boolean isFinal() 
  {
  	return Modifier.isFinal( this.getBits() ) ;
  } // isFinal() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Return <tt>true</tt> if the modifiers includes the <tt>interface</tt> modifier, 
   * false otherwise.
   */
  public boolean isInterface() 
  {
  	return Modifier.isInterface( this.getBits() ) ;
  } // isInterface() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Return <tt>true</tt> if the modifiers includes the <tt>native</tt> modifier, 
   * false otherwise.
   */
  public boolean isNative() 
  {
  	return Modifier.isNative( this.getBits() ) ;
  } // isNative() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Return <tt>true</tt> if the modifiers includes the <tt>private</tt> modifier, 
   * false otherwise.
   */
  public boolean isPrivate() 
  {
  	return Modifier.isPrivate( this.getBits() ) ;
  } // isPrivate() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Return <tt>true</tt> if the modifiers includes the <tt>protected</tt> modifier, 
   * false otherwise.
   */
  public boolean isProtected() 
  {
  	return Modifier.isProtected( this.getBits() ) ;
  } // isProtected() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Return <tt>true</tt> if the modifiers includes the <tt>public</tt> modifier, 
   * false otherwise.
   */
  public boolean isPublic() 
  {
  	return Modifier.isPublic( this.getBits() ) ;
  } // isPublic() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Return <tt>true</tt> if the modifiers includes the <tt>static</tt> modifier, 
   * false otherwise.
   */
  public boolean isStatic() 
  {
  	return Modifier.isStatic( this.getBits() ) ;
  } // isStatic() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Return <tt>true</tt> if the modifiers includes the <tt>strict</tt> modifier, 
   * false otherwise.
   */
  public boolean isStrict() 
  {
  	return Modifier.isStrict( this.getBits() ) ;
  } // isStrict() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Return <tt>true</tt> if the modifiers includes the <tt>synchronized</tt> modifier, 
   * false otherwise.
   */
  public boolean isSynchronized() 
  {
  	return Modifier.isSynchronized( this.getBits() ) ;
  } // isSynchronized() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Return <tt>true</tt> if the modifiers includes the <tt>transient</tt> modifier, 
   * false otherwise.
   */
  public boolean isTransient() 
  {
  	return Modifier.isTransient( this.getBits() ) ;
  } // isTransient() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Return <tt>true</tt> if the modifiers includes the <tt>volatile</tt> modifier, 
   * false otherwise.
   */
  public boolean isVolatile() 
  {
  	return Modifier.isVolatile( this.getBits() ) ;
  } // isVolatile() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Return <tt>true</tt> if the modifier bits does not include any of the 
   * visibility modifiers <tt>public</tt>, <tt>protected</tt> <tt>private</tt>, 
   * false otherwise.
   */
  public boolean isDefaultVisibility() 
  {
  	return ! ( this.isPublic() || this.isProtected() || this.isPrivate() ) ;
  } // isDefaultVisibility() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Return a string describing the access modifier flags in the specified modifier.
   */
  public String toString()
  {
  	return Modifier.toString( this.getBits() ) ;
  } // toString() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Indicates whether some other object is "equal to" this one.
   */
  public boolean equals( Object object ) 
	{
		if ( object instanceof Modifiers )
		{
			Modifiers mod = (Modifiers)object;
			return this.getBits() == mod.getBits() ;
		}
		return false ;
	} // equals() 
	
	// -------------------------------------------------------------------------

  /**
   * Returns a hash code value for the object.
   */
  public int hashCode() 
	{
		return this.getBits() ;
	} // hashCode() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Sets the <tt>ABSTRACT</tt> bit.
   */
  public void setAbstract() 
	{
		this.setModifier( Modifier.ABSTRACT ) ;
	} // setAbstract() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Sets the <tt>FINAL</tt> bit.
   */
  public void setFinal() 
  {
  	this.setModifier( Modifier.FINAL ) ;
  } // setFinal() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Sets the <tt>INTERFACE</tt> bit.
   */
  public void setInterface() 
  {
  	this.setModifier( Modifier.INTERFACE ) ;
  } // setInterface() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Sets the <tt>NATIVE</tt> bit.
   */
  public void setNative() 
  {
  	this.setModifier( Modifier.NATIVE ) ;
  } // setNative() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Sets the <tt>PRIVATE</tt> bit.
   * This automatically unsets the <tt>PUBLIC</tt> and <tt>PROTECTED</tt> bits. 
   */
  public void setPrivate() 
  {
  	this.setDefaultVisibility() ;
  	this.setModifier( Modifier.PRIVATE ) ;
  } // setPrivate() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Sets the <tt>PROTECTED</tt> bit.
   * This automatically unsets the <tt>PUBLIC</tt> and <tt>PRIVATE</tt> bits. 
   */
  public void setProtected() 
  {
  	this.setDefaultVisibility() ;
  	this.setModifier( Modifier.PROTECTED ) ;
  } // setProtected() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Sets the <tt>PUBLIC</tt> bit.
   * This automatically unsets the <tt>PROTECTED</tt> and <tt>PRIVATE</tt> bits. 
   */
  public void setPublic() 
  {
  	this.setDefaultVisibility() ;
  	this.setModifier( Modifier.PUBLIC ) ;
  } // setPublic() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Sets the <tt>STATIC</tt> bit.
   */
  public void setStatic() 
  {
  	this.setModifier( Modifier.STATIC ) ;
  } // setStatic() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Sets the <tt>STRICT</tt> bit.
   */
  public void setStrict() 
  {
  	this.setModifier( Modifier.STRICT ) ;
  } // setStrict() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Sets the <tt>SYNCHRONIZED</tt> bit.
   */
  public void setSynchronized() 
  {
  	this.setModifier( Modifier.SYNCHRONIZED ) ;
  } // setSynchronized() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Sets the <tt>TRANSIENT</tt> bit.
   */
  public void setTransient() 
  {
  	this.setModifier( Modifier.TRANSIENT ) ;
  } // setTransient() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Sets the <tt>VOLATILE</tt> bit.
   */
  public void setVolatile() 
  {
  	this.setModifier( Modifier.VOLATILE ) ;
  } // setVolatile() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Sets the visibility to default that means unset
   * <tt>PUBLIC</tt>, <tt>PROTECTED</tt>, <tt>PRIVATE</tt> bits.
   */
  public void setDefaultVisibility() 
  {
  	this.unsetModifier( Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE ) ;
  } // setDefaultVisibility() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Sets the visibility from the given string. If the given string is not
   * one of "public", "protected", "private" the the default visibility is set.
   * 
   *  @param visibility One of the visibility strings
   */
  public void setVisibility( String visibility ) 
  {
  	if ( VIS_PUBLIC.equals( visibility ) )
		{
			this.setPublic() ;
			return ;
		}
  	if ( VIS_PROTECTED.equals( visibility ) )
  	{
  		this.setProtected() ;
  		return ;
  	}
  	if ( VIS_PRIVATE.equals( visibility ) )
  	{
  		this.setPrivate() ;
  		return ;
  	}
  	this.setDefaultVisibility() ;  	
  } // setVisibility() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Unsets the <tt>ABSTRACT</tt> bit.
   */
  public void unsetAbstract() 
	{
		this.unsetModifier( Modifier.ABSTRACT ) ;
	} // unsetAbstract() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Unsets the <tt>FINAL</tt> bit.
   */
  public void unsetFinal() 
  {
  	this.unsetModifier( Modifier.FINAL ) ;
  } // unsetFinal() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Unsets the <tt>INTERFACE</tt> bit.
   */
  public void unsetInterface() 
  {
  	this.unsetModifier( Modifier.INTERFACE ) ;
  } // unsetInterface() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Unsets the <tt>NATIVE</tt> bit.
   */
  public void unsetNative() 
  {
  	this.unsetModifier( Modifier.NATIVE ) ;
  } // unsetNative() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Unsets the <tt>PRIVATE</tt> bit.
   */
  public void unsetPrivate() 
  {
  	this.unsetModifier( Modifier.PRIVATE ) ;
  } // unsetPrivate() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Unsets the <tt>PROTECTED</tt> bit.
   */
  public void unsetProtected() 
  {
  	this.unsetModifier( Modifier.PROTECTED ) ;
  } // unsetProtected() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Unsets the <tt>PUBLIC</tt> bit.
   */
  public void unsetPublic() 
  {
  	this.unsetModifier( Modifier.PUBLIC ) ;
  } // unsetPublic() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Unsets the <tt>STATIC</tt> bit.
   */
  public void unsetStatic() 
  {
  	this.unsetModifier( Modifier.STATIC ) ;
  } // unsetStatic() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Unsets the <tt>STRICT</tt> bit.
   */
  public void unsetStrict() 
  {
  	this.unsetModifier( Modifier.STRICT ) ;
  } // unsetStrict() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Unsets the <tt>SYNCHRONIZED</tt> bit.
   */
  public void unsetSynchronized() 
  {
  	this.unsetModifier( Modifier.SYNCHRONIZED ) ;
  } // unsetSynchronized() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Unsets the <tt>TRANSIENT</tt> bit.
   */
  public void unsetTransient() 
  {
  	this.unsetModifier( Modifier.TRANSIENT ) ;
  } // unsetTransient() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Unsets the <tt>VOLATILE</tt> bit.
   */
  public void unsetVolatile() 
  {
  	this.unsetModifier( Modifier.VOLATILE ) ;
  } // unsetVolatile() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * Sets the bits in the underlying int that are specified by mod.
   */
  protected void setModifier( int mod ) 
	{
  	this.setBits( this.getBits() | mod ) ;
	} // setModifier() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Unsets the bits in the underlying int that are specified by mod.
   */
  protected void unsetModifier( int mod ) 
  {
  	this.setBits( this.getBits() & ~mod ) ;
  } // unsetModifier() 
  
  // -------------------------------------------------------------------------
  
} // class Modifiers 
