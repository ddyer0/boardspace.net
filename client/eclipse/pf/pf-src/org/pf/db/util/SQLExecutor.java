// ===========================================================================
// CONTENT  : CLASS SQLExecutor
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 17/07/2002
// HISTORY  :
//  03/07/2002  duma  CREATED
//	17/07/2002	duma	added		->	prepareWriteStatement(), executeWriteStatement()
//
// Copyright (c) 2002, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.db.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

/**
 * Provides an easy to use interface to execute SQL statements against
 * a database.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class SQLExecutor
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================


  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private DataSource dataSource = null ;
  protected DataSource getDataSource() { return dataSource ; }
  protected void setDataSource( DataSource newValue ) { dataSource = newValue ; }

  private Connection connection = null ;
  protected Connection connection() { return connection ; }
  protected void connection( Connection newValue ) { connection = newValue ; }
      
  // =========================================================================
  // CLASS METHODS
  // =========================================================================


  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   * 
   * @param aDataSource The datasource the executor should connect to (must not be null)
   */
  public SQLExecutor( DataSource aDataSource )
  {
    super() ;
    if ( aDataSource == null )
    	throw new IllegalArgumentException( "Datasource in SQLExecutor constructor is null" ) ;
    	
    this.setDataSource( aDataSource ) ;
  } // SQLExecutor()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  
	/**
	 * Returns a PreparedStatement for the given SQL command.
	 * 
	 * @param sql A valid SQL statement with placeholders (?) (no SELECT allowed here!)
	 * @throws SQLException Any problem that occurs during execution
	 */
	public PreparedStatement prepareWriteStatement( String sql )
		throws SQLException
	{
		return this.getConnection().prepareStatement( sql ) ;
	} // prepareWriteStatement()

  // -------------------------------------------------------------------------

	/**
	 * Executes the given statement and returns the number of affected rows.
	 * 
	 * @param statement A valid statement, created before by this executor
	 * @throws SQLException Any problem that occurs during execution
	 */
	public int executeWriteStatement( PreparedStatement statement )
		throws SQLException
	{
		Connection conn ;
		
		conn = this.getConnection() ;
		if ( conn != null )
		{
			return statement.executeUpdate() ;
		}
		return 0 ;
	} // executeWriteStatement()

  // -------------------------------------------------------------------------
  
	/**
	 * Executes the given SQL command and returns the number of affected rows.
	 * 
	 * @param sql A valid SQL statement (no SELECT allowed here!)
	 * @throws SQLException Any problem that occurs during execution
	 */
	public int execute( String sql )
		throws SQLException
	{
		Statement statement ;
		Connection conn ;
		
		conn = this.getConnection() ;
		if ( conn != null )
		{
			statement = conn.createStatement() ;
			return statement.executeUpdate( sql ) ;
		}
		return 0 ;
	} // execute()

  // -------------------------------------------------------------------------

	/**
	 * Executes the given SQL command and returns true if the execution was 
	 * successful.
	 * 
	 * @param sql A valid SQL statement (no SELECT allowed here!)
	 */
	public boolean executeSQL( String sql )
	{
		try
		{
			this.execute( sql ) ;
			return true ;
		}
		catch (SQLException e)
		{
			return false ;
		}		
	} // execute()

  // -------------------------------------------------------------------------

	/**
	 * Closes all open connections.
	 * Connections will be reopened automatically if necessary.
	 */
	public void close()
	{
		if ( ! isClosed() )
		{
			try
			{
				this.connection().close() ;
			}
			catch (SQLException e)
			{
			}
			this.connection( null ) ;
		}
	} // close()

  // -------------------------------------------------------------------------

	/**
	 * Closes the given statement.
	 */
	public void closeStatement( Statement statement )
	{
		if ( statement != null )
		{
			try
			{
				statement.close() ;
			}
			catch (SQLException e)
			{
			}
		}
	} // closeStatement()

  // -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

	protected Connection getConnection()
		throws SQLException
	{
		if ( this.isClosed() )
		{
			this.connection( this.newConnection() ) ;
		}
		return this.connection() ;
	} // getConnection() 

  // -------------------------------------------------------------------------

	protected Connection newConnection()
		throws SQLException
	{
		return this.getDataSource().getConnection() ;
	} // newConnection() 

  // -------------------------------------------------------------------------

  protected boolean isClosed() 
  { 
  	return ( this.connection() == null ) ; 
  } // isClosed()
  
  // -------------------------------------------------------------------------
  
} // class SQLExecutor