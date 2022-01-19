// ===========================================================================
// CONTENT  : CLASS BinarySocketConnection
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 23/06/2006
// HISTORY  :
//  25/06/2004  mdu  CREATED
//	23/06/2006	mdu		changed	-->	getData() to block until data is available
//
// Copyright (c) 2004-2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.net ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.pf.util.ExceptionHandler;


/**
 * Receives binary data from a socket and provides it to a request processor
 * by the getData() method
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class BinarySocketConnection extends SocketConnection
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private BufferedInputStream reader = null ;
  protected BufferedInputStream reader() { return reader ; }
  protected void reader( BufferedInputStream newValue ) { reader = newValue ; }
  
  private BufferedOutputStream writer = null ;
  protected BufferedOutputStream writer() { return writer ; }
  protected void writer( BufferedOutputStream newValue ) { writer = newValue ; }
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
	/**
	 * @param openSocket The socket this connection is based on
	 */
	public BinarySocketConnection(Socket openSocket)
	{
		super(openSocket);
	} // BinarySocketConnection() 

	// -------------------------------------------------------------------------

	/**
	 * @param openSocket The socket this connection is based on
	 * @param exHandler The exception handler for this connection
	 */
	public BinarySocketConnection(Socket openSocket, ExceptionHandler exHandler)
	{
		super(openSocket, exHandler);
	} // BinarySocketConnection() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Tries to read data from the underlying input stream. Blocks until some
	 * data comes in and then returns it as a byte array.
	 */
	public byte[] getData()
	{
		byte[] data = null;
		int b;
		int numberOfBytes = 0;

		if (this.canRead())
		{
			try
			{
				b = this.getReader().read(); // to block until something comes in or SocketTimeoutException
				numberOfBytes = this.getReader().available();
				if (numberOfBytes > 0)
				{
					data = new byte[numberOfBytes + 1];
					data[0] = (byte)b;
					this.getReader().read(data, 1, numberOfBytes);
				}
				else
				{
					data = null;
				}
			}
			catch (SocketException ex)
			{
				this.isOpen(false);
			}
			catch (SocketTimeoutException e)
			{
				this.exceptionOccurred(e);
				this.close();
			}
			catch (IOException e)
			{
				this.exceptionOccurred(e);
			}
		}
		return data;
	} // getData() 

	// -------------------------------------------------------------------------

	/**
	 * Writes the given data to the underlying output stream
	 */
	public boolean writeData(byte[] bytes)
	{
		try
		{
			this.getWriter().write(bytes);
			this.getWriter().flush();
			return true;
		}
		catch (Throwable e)
		{
			this.exceptionOccurred(e);
			return false;
		}
	} // writeData()

	// -------------------------------------------------------------------------

	/**
	 * Close the connection and all release underlying resources.
	 */
	@Override
	public void close()
	{
		super.close();
		this.closeReader();
		this.closeWriter();
	} // close()

	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	protected boolean canRead()
	{
		return (this.isOpen() && (this.getReader() != null));
	} // canRead() 

	// -------------------------------------------------------------------------

	protected boolean hasReader()
	{
		return (this.reader() != null);
	} // hasReader() 

	// -------------------------------------------------------------------------

	protected boolean hasWriter()
	{
		return (this.writer() != null);
	} // hasWriter() 

	// -------------------------------------------------------------------------

	protected BufferedInputStream getReader()
	{
		if (!this.hasReader())
		{
			try
			{
				this.reader(new BufferedInputStream(this.socket().getInputStream()));
			}
			catch (IOException e)
			{
				this.exceptionOccurred(e);
			}
		}
		return this.reader();
	} // getReader() 

	// -------------------------------------------------------------------------

	protected BufferedOutputStream getWriter()
	{
		if (!this.hasWriter())
		{
			try
			{
				this.writer(new BufferedOutputStream(this.socket().getOutputStream()));
			}
			catch (IOException e)
			{
				this.exceptionOccurred(e);
			}
		}
		return this.writer();
	} // getWriter() 

	// -------------------------------------------------------------------------

	protected void closeReader()
	{
		if (this.hasReader())
		{
			try
			{
				this.reader().close();
			}
			catch (IOException e)
			{
				this.exceptionOccurred(e);
			}
		}
		this.reader(null);
	} // closeReader()

	// -------------------------------------------------------------------------

	protected void closeWriter()
	{
		if (this.hasWriter())
		{
			try
			{
				this.writer().close();
			}
			catch (IOException e)
			{
				this.exceptionOccurred(e);
			}
			this.writer(null);
		}
	} // closeWriter()

	// -------------------------------------------------------------------------

} // class BinarySocketConnection 
