// ===========================================================================
// CONTENT  : CLASS ConnectionPool
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 15/11/2002
// HISTORY  :
//  15/11/2002  mdu  CREATED
//
// Copyright (c) 2002, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.net ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.pf.util.*;

/**
 * This class provides a mechanism to reuse established connections rather
 * than creating new ones every time. This results in a significant 
 * performance increase.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class ConnectionPool extends ExceptionIgnorer
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	private static final int POOLSIZE_MINIMUM = 2 ;
	private static final int POOLSIZE_DEFAULT = 10 ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private volatile boolean creatingConnection = false ;

  private int poolSize = POOLSIZE_DEFAULT ;
  protected int poolSize() { return poolSize ; }
  protected void poolSize( int newValue ) { poolSize = newValue ; }

  private int usedSlots = 0 ;
  protected synchronized int usedSlots() { return usedSlots ; }
  protected void usedSlots( int newValue ) { usedSlots = newValue ; }
  
  private TextSocketConnection[] pool = null ;
  protected TextSocketConnection[] pool() { return pool ; }
  protected void pool( TextSocketConnection[] newValue ) { pool = newValue ; }
  
  private String hostname = null ;
  protected String hostname() { return hostname ; }
  protected void hostname( String newValue ) { hostname = newValue ; }
  
  private int port = 21000 ;
  protected int port() { return port ; }
  protected void port( int newValue ) { port = newValue ; }  
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================


  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with the hostname and the port of the server
   * to connect to.
   * It uses the default pool limit of 10.
   * 
   * @param hostname The name of the server to which the pool should connect to
   * @param port The port on the host on which the server process is listening
   */
  public ConnectionPool( String hostname, int port )
  {
    this( hostname, port, POOLSIZE_DEFAULT ) ;
  } // ConnectionPool()

	// -------------------------------------------------------------------------

  /**
   * Initialize the new instance with the hostname and the port of the server
   * to connect to.
   * It also initializes the pool to a maximum of connections that can be 
   * pooled.
   * 
   * @param hostname The name of the server to which the pool should connect to
   * @param port The port on the host on which the server process is listening
   * @param poolLimit The maximum number of connections to be pooled ( must be > 1 )
   */
  public ConnectionPool( String hostname, int port, int poolLimit )
  {
    this( hostname, port, null) ;
  } // ConnectionPool()

	// -------------------------------------------------------------------------

  /**
   * Initialize the new instance with the hostname and the port of the server
   * to connect to.
   * It uses the default pool limit of 10.
   * 
   * @param hostname The name of the server to which the pool should connect to
   * @param port The port on the host on which the server process is listening
   * @param exHandler An optional handler that gets called for all occuring exceptions
   */
  public ConnectionPool( String hostname, int port, ExceptionHandler exHandler )
  {
    this( hostname, port, POOLSIZE_MINIMUM, null) ;
  } // ConnectionPool()

	// -------------------------------------------------------------------------

  /**
   * Initialize the new instance with the hostname and the port of the server
   * to connect to.
   * 
   * @param hostname The name of the server to which the pool should connect to
   * @param port The port on the host on which the server process is listening
   * @param poolLimit The maximum number of connections to be pooled ( must be > 1 )
   * @param exHandler An optional handler that gets called for all occuring exceptions
   */
  public ConnectionPool( String hostname, int port, int poolLimit,
  												ExceptionHandler exHandler )
  {
    super( exHandler ) ;
    this.hostname( hostname ) ;
    this.port( port ) ;

    this.initPool( Math.max( POOLSIZE_MINIMUM, poolLimit ) ) ;
  } // ConnectionPool()

	// -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns a connection based on the underlying socket.
   * Tries to reuse connections as much as possible.
   * If no connection can be reused, a new connection will be established, 
   * if the limit of the connection pool is not yet reached.
   */
	public TextSocketConnection getConnection()
	{
		TextSocketConnection conn = null ;

		while ( conn == null )
		{
			conn = this.findUnusedConnectionInPool() ;
			if ( conn == null )
			{
				if ( this.wantToCreateNewConnection() )
				{
					conn = this.newConnection() ;
					if ( conn != null )
					{
						conn.inUse( true ) ;
						this.addToPool( conn ) ;
						creatingConnection = false ;
					}
				}
				/* DEBUG
				if ( conn == null )
					System.out.println( "T:" + Thread.currentThread().getName() ) ;
				*/
			}
		}
		return conn ;		
	} // getConnection()

	// -------------------------------------------------------------------------

	public void returnConnection( TextSocketConnection connection )
	{
		connection.inUse( false ) ;
	} // returnConnection()

	// -------------------------------------------------------------------------

	/**
	 * Creates connections ready to be used in the pool.
	 * This method should only be called right after the instantiation of
	 * the connection pool.
	 * <b>It must not be called, when the pool is already in use !</b>
	 */
	public void createConnections( int numberOfConnections )
	{
		int create = Math.min( this.poolSize(), numberOfConnections ) ;
		
		for ( int i = 1 ; i <= create ; i++ )
		{
			this.createConnectionInPool() ;
		}		
	} // createConnections()

	// -------------------------------------------------------------------------

	/**
	 * Returns how many connections are in the pool
	 */
	public int numberOfPooledConnections()
	{
		return this.usedSlots() ;	
	} // numberOfPooledConnections()

	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected TextSocketConnection newConnection()
	{
		Socket socket ;
		TextSocketConnection conn = null ;
		
		socket = this.openSocket() ;
		if ( socket != null )
		{
			conn = new TextSocketConnection( socket, this.exceptionHandler() ) ;
		}
		return conn ;		
	} // newConnection()

	// -------------------------------------------------------------------------

	protected Socket openSocket()
	{
		Socket socket = null ;
		
		try
		{
			socket = new Socket( this.hostname(), this.port() ) ;
		}
		catch (UnknownHostException e)
		{
			this.exceptionOccurred(e) ;
		}
		catch (IOException e)
		{
			this.exceptionOccurred(e) ;
		}
		return socket ;		
	} // openSocket()

	// -------------------------------------------------------------------------

	protected TextSocketConnection findUnusedConnectionInPool()
	{
		TextSocketConnection conn = null ;
		
		for( int i = 0 ; i < poolSize ; i++ )
		{
			conn = pool[i] ;
			if ( conn != null )
			{
				if ( conn.isClosed() )
				{
					pool[i] = null ;
					this.decUsedSlots() ;
				}
				else
				{
					if ( conn.wantToUse() )
					{
						// System.out.println( "Reuse " + i ) ;
						return conn ;
					}
				}
			}
		}
		return null ;
	} // findUnusedConnectionInPool()

	// -------------------------------------------------------------------------

	protected void addToPool( TextSocketConnection conn )
	{
		for (int i = 0 ; i < poolSize ; i++)
		{
			if ( pool[i] == null )
			{
				pool[i] = conn ;
				this.incUsedSlots() ;
				return ;
			}
		}
	} // addToPool()

	// -------------------------------------------------------------------------

	protected synchronized boolean wantToCreateNewConnection()
	{
		if ( creatingConnection )
			return false ;
		
		if ( this.poolIsFull() )
			return false ;
			
		creatingConnection = true ;
		return true ;	
	} // wantToCreateNewConnection()

	// -------------------------------------------------------------------------

	protected boolean poolIsFull()
	{
		return ( this.usedSlots() == this.poolSize() ) ;
	} // poolIsFull()

	// -------------------------------------------------------------------------

	protected void initPool( int size )
	{
		this.poolSize( size ) ;
		this.pool( new TextSocketConnection[size] ) ;
		this.usedSlots( 0 ) ;
	} // initPool()

	// -------------------------------------------------------------------------

	protected void createConnectionInPool()
	{
		TextSocketConnection conn ;
		
		conn = this.newConnection() ;
		conn.getReader() ;
		conn.getWriter() ;
		conn.inUse(false) ;
		this.addToPool(conn) ;
	} // createConnectionInPool()

	// -------------------------------------------------------------------------

	protected synchronized void incUsedSlots()
	{
		usedSlots++ ;
	} // incUsedSlots()

	// -------------------------------------------------------------------------

	protected synchronized void decUsedSlots()
	{
		usedSlots-- ;
	} // decUsedSlots()

	// -------------------------------------------------------------------------

} // class ConnectionPool
