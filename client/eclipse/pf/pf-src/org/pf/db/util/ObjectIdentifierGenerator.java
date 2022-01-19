// ===========================================================================
// CONTENT  : CLASS ObjectIdentifierGenerator
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.5 - 22/02/2008
// HISTORY  :
//  12/12/2000  duma  CREATED
//  02/12/2001  duma  moved from com.mdcs.db.util
//	28/06/2002	duma	renamed	->	from ObjectIdentifierFactory
//	28/06/2002	duma	added		->	nextIdentifier()
//	02/07/2002	duma	added		->	Constructor with startId
//	26/07/2003	duma	bugfix	->	idLength of 1 was not allowed
//	22/02/2008	mdu		changed	->	to extend ObjectIdGenerator
//
// Copyright (c) 2000-2008, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.db.util;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.text.ObjectIdGenerator;

/**
 * This class provides identifiers by incrementing an internal counter,
 * starting at 1.
 *
 * @author Manfred Duchrow
 * @version 1.5
 */
public class ObjectIdentifierGenerator extends ObjectIdGenerator 
  implements ObjectIdentifierProducer
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

	// =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   * That is an ID length of 10 and a start ID of 1.
   */
  public ObjectIdentifierGenerator()
  {
    super() ;
  } // ObjectIdentifierGenerator()
 
  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with the length for the generated identifiers.
   * 
   * @param idLength The length to which Ids are filled up with leading zeros (must be > 0)
   */
  public ObjectIdentifierGenerator( int idLength )
  {
    this() ;
    this.setLength( idLength );
  } // ObjectIdentifierGenerator()
 
  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with the length for the generated identifiers
   * and the id to start with.
   * 
   * @param startId The first id to be generated
   * @param idLength The length to which Ids are filled up with leading zeros
   */
  public ObjectIdentifierGenerator( long startId, int idLength )
  {
    this( idLength ) ;
    if ( startId >= 0 )
      this.setNextAvailableId( startId );
  } // ObjectIdentifierGenerator()
 
  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class ObjectIdentifierGenerator