// ===========================================================================
// CONTENT  : CLASS DataSourceProxy
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 22/02/2008
// HISTORY  :
//  18/08/2001  duma  CREATED
//  02/12/2001  duma  moved from com.mdcs.db.util
//	22/02/2008	mdu		added	-->	setDriver()		
//
// Copyright (c) 2001-2008, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.db.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.sql.* ;
import java.util.logging.Logger;
import java.io.PrintWriter;
import javax.sql.DataSource ;

/**
 * This class is a datasource wrapper for other datasources or simple
 * connections. It allows to pass simple connection information such
 * as url, userid, password as a datasource to other objects that require
 * a javax.sql.DataSource rather than java.sql.Connection as input.
 * In addition the wrapping of other datasource objects plus their associated
 * userid/password data is supported. That allows to pass around just one object
 * that can be asked to return a connection via getConnection().
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
public class DataSourceProxy implements DataSource
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String dbUrl = null ;
  private String dbUserid = null ;
  private String dbPassword = null ;

  private Connection dbConnection = null ;
  protected Connection getDbConnection() { return dbConnection ; }
  protected void setDbConnection( Connection newValue ) { dbConnection = newValue ; }

  private DataSource dataSource = null ;
  protected DataSource getDataSource() { return dataSource ; }
  protected void setDataSource( DataSource newValue ) { dataSource = newValue ; }

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with a ready-to-use connection.
   */
  public DataSourceProxy( Connection conn )
  {
    super() ;
    this.setDbConnection( conn ) ;
  } // DataSourceProxy() 

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with all necessary connection information.
   */
  public DataSourceProxy( String url, String username, String password )
  {
    super();
    this.setDbUrl( url ) ;
    this.setDbUserid( username ) ;
    this.setDbPassword( password ) ;
  } // DataSourceProxy() 

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with an URL for the database connection.
   */
  public DataSourceProxy( String url )
  {
    this( url, null, null ) ;
  } // DataSourceProxy() 

  // -------------------------------------------------------------------------

  public DataSourceProxy( DataSource dataSource, String username, String password )
  {
    super();
    this.setDataSource( dataSource ) ;
    this.setDbUserid( username ) ;
    this.setDbPassword( password ) ;
  } // DataSourceProxy() 

  // -------------------------------------------------------------------------

  public DataSourceProxy( DataSource dataSource )
  {
    this( dataSource, null, null ) ;
  } // DataSourceProxy() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Attempt to establish a database connection
   */
  public Connection getConnection()
    throws SQLException
  {
    if ( this.hasConnection() )
		{
			return this.getDbConnection() ;
		}

    if ( this.hasDataSource() )
		{
			return this.getDataSourceConnection() ;
		}

    if ( ( this.getDbUserid() == null ) || ( this.getDbPassword() == null ) )
		{
			return DriverManager.getConnection( this.getDbUrl() ) ;
		}
		else
		{
			return DriverManager.getConnection( this.getDbUrl(), this.getDbUserid(), this.getDbPassword() ) ;
		}
  } // getConnection() 

  // -------------------------------------------------------------------------

  /**
   * Attempt to establish a database connection
   */
  public Connection getConnection( String username, String password )
    throws SQLException
  {
    if ( this.hasDataSource() )
		{
			return this.getDataSource().getConnection( username, password ) ;
		}
    return DriverManager.getConnection( this.getDbUrl(), username, password ) ;
  } // getConnection() 

  // -------------------------------------------------------------------------

  public String getDbUrl()
	{
		return dbUrl;
	} // getDbUrl() 
  
  // -------------------------------------------------------------------------
  
	public void setDbUrl( String url )
	{
		dbUrl = url;
	} // setDbUrl() 
	
	// -------------------------------------------------------------------------
	
	public String getDbUserid()
	{
		return dbUserid;
	} // getDbUserid() 
	
	// -------------------------------------------------------------------------
	
	public void setDbUserid( String userId )
	{
		dbUserid = userId;
	} // setDbUserid() 
	
	// -------------------------------------------------------------------------
	
	public String getDbPassword()
	{
		return dbPassword;
	} // getDbPassword() 
	
	// -------------------------------------------------------------------------
	
	public void setDbPassword( String password )
	{
		dbPassword = password;
	} // setDbPassword() 

  // -------------------------------------------------------------------------
  
  /**
   * Returns the maximum time in seconds that this
   * data source can wait while attempting to connect
   * to a database.
   */
  public int getLoginTimeout()
    throws SQLException
  {
    if ( this.hasDataSource() )
		{
			return this.getDataSource().getLoginTimeout() ;
		}
    return DriverManager.getLoginTimeout() ;
  } // getLoginTimeout() 

  // -------------------------------------------------------------------------

  /**
   * Returns the log writer for this data source
   */
  public PrintWriter getLogWriter()
    throws SQLException
  {
    if ( this.hasDataSource() )
		{
			return this.getDataSource().getLogWriter() ;
		}
    return DriverManager.getLogWriter() ;
  } // getLogWriter() 

  // -------------------------------------------------------------------------

  /**
   * Sets the maximum time in seconds that this
   * data source can wait while attempting to connect
   * to a database.
   */
  public void setLoginTimeout( int timeout )
    throws SQLException
  {
    if ( this.hasDataSource() )
		{
			this.getDataSource().setLoginTimeout( timeout ) ;
		}
		else
		{
			DriverManager.setLoginTimeout( timeout ) ;
		}
  } // setLoginTimeout() 

  // -------------------------------------------------------------------------

  /**
   * Sets the log writer for this data source
   */
  public void setLogWriter( PrintWriter writer )
    throws SQLException
  {
    if ( this.hasDataSource() )
		{
			this.getDataSource().setLogWriter( writer ) ;
		}
		else
		{
			DriverManager.setLogWriter( writer ) ;
		}
  } // setLogWriter() 

  // -------------------------------------------------------------------------

  /**
   * Creates an instance of the given class name in order to register the 
   * database driver.
   */
  public void setDriverClassName( String driverClassName )
  {
    try
		{
			Class.forName(driverClassName).getDeclaredConstructor().newInstance() ;
		}
		catch ( Exception e )
		{
			DriverManager.println( e.toString() ) ;
		}
  } // setDriverClassName() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  protected boolean hasDataSource()
  {
    return this.getDataSource() != null ;
  } // hasDataSource() 

  // -------------------------------------------------------------------------

  protected boolean hasConnection()
  {
    return this.getDbConnection() != null ;
  } // hasConnection() 

  // -------------------------------------------------------------------------

  /**
   * Attempt to establish a database connection using the wrapped datasource
   */
  protected Connection getDataSourceConnection()
    throws SQLException
  {
    if ( ( this.getDbUserid() == null ) || ( this.getDbPassword() == null ) )
		{
			return this.getDataSource().getConnection() ;
		}
    return this.getDataSource().getConnection( this.getDbUserid(), this.getDbPassword() ) ;
  } // getDataSourceConnection() 
@Override
public Logger getParentLogger() throws SQLFeatureNotSupportedException {
	return null;
}
@Override
public <T> T unwrap(Class<T> iface) throws SQLException {
	return null;
}
@Override
public boolean isWrapperFor(Class<?> iface) throws SQLException {
	return false;
}

  // -------------------------------------------------------------------------

} // class DataSourceProxy 
