// ===========================================================================
// CONTENT  : CLASS DBUtil
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 15/10/2011
// HISTORY  :
//  15/10/2011  mdu  CREATED
//
// Copyright (c) 2011, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.db.util;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

/**
 * Helper and convenience methods for Java database handling.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class DBUtil
{
	// =========================================================================
	// CONSTANTS
	// =========================================================================

	// =========================================================================
	// CLASS VARIABLES
	// =========================================================================
	private static DBUtil soleInstance = new DBUtil();

	// =========================================================================
	// INSTANCE VARIABLES
	// =========================================================================

	// =========================================================================
	// CLASS METHODS
	// =========================================================================

	/**
	 * Returns the only instance this class supports (design pattern "Singleton")
	 */
	public static DBUtil current()
	{
		return soleInstance;
	} // instance()

	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	protected DBUtil()
	{
		super();
	} // DBUtil()

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Loads the database driver with the given class name and registers it
	 * at the java.sql.DriverManager.
	 * 
	 * @param driverClassName The class name of the driver to load
	 * @return true if loading was successful, otherwise false
	 */
	public boolean loadAndRegisterDriver(final String driverClassName)
	{
		if (driverClassName == null)
		{
			return false;
		}
		try
		{
			this.getClass().forName(driverClassName);
			return this.isDriverRegistered(driverClassName);
		}
		catch (ClassNotFoundException ex)
		{
			return false;
		}
	} // loadDriver()

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the database driver with the specified class name is
	 * registered at the java.sql.DriverManager.
	 * 
	 * @param driverClassName The class name of the driver to load
	 */
	public boolean isDriverRegistered(final String driverClassName)
	{
		Enumeration<Driver> drivers;
		Driver driver;

		if (driverClassName == null)
		{
			return false;
		}
		drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements())
		{
			driver = (Driver)drivers.nextElement();
			if (driverClassName.equals(driver.getClass().getName()))
			{
				return true;
			}
		}
		return false;
	} // isDriverRegistered()

	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================

} // class DBUtil
