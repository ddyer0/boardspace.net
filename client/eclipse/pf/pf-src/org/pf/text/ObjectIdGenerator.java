// ===========================================================================
// CONTENT  : CLASS ObjectIdGenerator
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 22/02/2008
// HISTORY  :
//  22/02/2008  mdu  CREATED
//
// Copyright (c) 2008, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.bif.identifier.IObjectIdGenerator;

/**
 * Generates numeric IDs that are left padded with zeros.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class ObjectIdGenerator implements IObjectIdGenerator
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  protected final static char DEFAULT_PAD_CHAR 			= '0' ;
  protected final static long DEFAULT_START_ID	  	= 1 ;
  protected final static int DEFAULT_LENGTH				  = 10 ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private int length = DEFAULT_LENGTH ;
  private char padChar = DEFAULT_PAD_CHAR ;
  private String prefix = null ;

  private long nextAvailableId = DEFAULT_START_ID ;
  protected long getNextAvailableId() { return nextAvailableId ; }
  protected void setNextAvailableId( long newValue ) { nextAvailableId = newValue ; }

  protected int bufferLen = DEFAULT_LENGTH ;
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   * That is an ID length of 10 and a start ID of 1 with fill character '0'.
   */
  public ObjectIdGenerator()
  {
    super() ;
    this.setLength( this.getDefaultLength() ) ;
    this.setNextAvailableId( this.getDefaultStartId() ) ;
    this.setPadChar( this.getDefaultPadChar() ) ;
  } // ObjectIdGenerator() 

  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with the length for the generated identifiers.
   * 
   * @param idLength The length to which Ids are filled up with leading zeros (must be > 0)
   */
  public ObjectIdGenerator( int idLength )
  {
    this() ;
    this.setLength( idLength );
  } // ObjectIdGenerator() 
 
  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with the length for the generated identifiers
   * and the id to start with.
   * 
   * @param startId The first id to be generated
   * @param idLength The length to which Ids are filled up with leading zeros
   */
  public ObjectIdGenerator( long startId, int idLength )
  {
    this( idLength ) ;
		this.setNextAvailableId( startId );
  } // ObjectIdGenerator() 
 
  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns a new unique identifier as string.
   */
  public synchronized String newIdentifier()
  {
  	return ( this.leftPad( this.nextIdentifier() ) ) ;
  } // newIdentifier() 
 
  // -------------------------------------------------------------------------

  /**
   * Returns a new identifier which is different to the last one.
   */
  public long nextIdentifier()
  {
  	long id = this.getNextId() ;
    this.setNextId( id + 1 ) ;
    return id ;
  } // nextIdentifier() 
 
  // -------------------------------------------------------------------------

	/** 
	 * Returns the length to which the IDs are filled up
	 */
	public int getLength() 
	{ 
		return length ; 
	} // getLength() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Set the length to which the IDs must be filled up with leading zeroes.
	 * Since all IDs are based on data type long a value greater than 20 is
	 * not really reasonable, however it is allowed here. 
	 * 
	 * @param newValue The length (must be > 0)
	 */
	public void setLength( int newValue ) 
	{ 
		if ( newValue > 0 )
		{
			length = newValue ;
			this.calcBufferLength() ;
		} 
	} // setLength() 

	// -------------------------------------------------------------------------
	
	/**
	 * Set the character that is used to fill up the IDs to the full length.
	 * 
	 * @param fillChar The fill character
	 */
	public void setPadChar( char fillChar ) 
	{
		padChar = fillChar ;
	} // setPadChar() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Return the character that is used to fill up the IDs to the full length.
	 */
	public char getPadChar() 
	{
		return padChar ;
	} // getPadChar() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the prefix that will be prepended to every generated ID.
	 */
  public String getPrefix()
	{
		return prefix;
	} // getPrefix()
  
  // -------------------------------------------------------------------------
  
	/**
	 * Set the prefix that will be prepended to every generated ID.
	 */
	public void setPrefix( String newValue )
	{
		prefix = newValue;
		this.calcBufferLength() ;
	} // setPrefix()
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  protected String leftPad( long id )
  {
  	StringBuffer buffer ;
  	
  	buffer = new StringBuffer( bufferLen ) ;
  	if ( prefix != null )
		{
			buffer.append(  prefix ) ;
		}
  	this.str().leftPadCh( buffer, id, this.getLength(), this.getPadChar() ) ;
  	return buffer.toString() ;
  } // leftPad() 
 
  // -------------------------------------------------------------------------

  protected long getNextId()
  {
  	return this.getNextAvailableId() ;
  } // getNextId() 
 
  // -------------------------------------------------------------------------

  protected void setNextId( long id )
  {
  	this.setNextAvailableId( id ) ;
  } // setNextId() 
  
  // -------------------------------------------------------------------------

  protected void calcBufferLength() 
	{
		if ( prefix == null )
		{
			bufferLen = this.getLength() ;
		}
		else
		{
			bufferLen = this.getLength() + prefix.length() ;
		}
	} // calcBufferLength()
	
	// -------------------------------------------------------------------------
  
  protected long getDefaultStartId() 
	{
		return DEFAULT_START_ID ;
	} // getDefaultStartId() 
	
	// -------------------------------------------------------------------------
  
  protected int getDefaultLength() 
	{
		return DEFAULT_LENGTH ;
	} // getDefaultLength() 
	
	// -------------------------------------------------------------------------
  
  protected char getDefaultPadChar() 
  {
  	return DEFAULT_PAD_CHAR ;
  } // getDefaultPadChar() 
  
  // -------------------------------------------------------------------------
  
  protected StringUtil str()
	{
		return StringUtil.current();
	} // str() 

	// -------------------------------------------------------------------------
  
} // class ObjectIdGenerator 
