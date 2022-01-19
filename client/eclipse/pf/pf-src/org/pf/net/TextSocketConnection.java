// ===========================================================================
// CONTENT  : CLASS TextSocketConnection
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 25/06/2004
// HISTORY  :
//  15/11/2002  mdu  CREATED
//	25/06/2004	mdu	changed		-->	Moved many methods to new superclass SocketConnection
//
// Copyright (c) 2002-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.net ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.pf.util.*;

/**
 * A connection based on a opened socket. The connection provides convenient
 * methods to read and write lines from/to the underlying socket.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class TextSocketConnection extends SocketConnection
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private BufferedReader reader = null ;
  protected BufferedReader reader() { return reader ; }
  protected void reader( BufferedReader newValue ) { reader = newValue ; }
  
  private PrintWriter writer = null ;
  protected PrintWriter writer() { return writer ; }
  protected void writer( PrintWriter newValue ) { writer = newValue ; }    
    
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
	/**
	 * Initialize the new instance to work on the given socket.
	 */
	public TextSocketConnection(Socket openSocket)
	{
		super(openSocket);
	} // TextSocketConnection()

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance to work on the given socket and use the
	 * specified exception handler to report exceptions to.
	 */
	public TextSocketConnection(Socket openSocket, ExceptionHandler exHandler)
	{
		super(openSocket, exHandler);
	} // TextSocketConnection()

	// -------------------------------------------------------------------------

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================

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

	/**
	 * Reads the next line from the socket's input stream and returns it.
	 * Returns null in case of any error or if no more data is available.
	 */
	public String readLine()
	{
		String line = null;

		if (this.canRead())
		{
			try
			{
				// block until something comes in or SocketTimeoutException
				line = this.getReader().readLine();
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
		return line;
	} // readLine()

	// -------------------------------------------------------------------------

	/**
	 * Writes the given text to the socket's output stream.
	 * Returns true, if everything went well, otherwise false.
	 */
	public boolean write(String text)
	{
		boolean success = false;
		if (this.canWrite())
		{
			this.getWriter().write(text);
			this.getWriter().flush();
			success = true;
		}
		return success;
	} // write()

	// -------------------------------------------------------------------------

	/**
	 * Writes the given string to the socket's output stream and appends a
	 * newline character.
	 * Returns true, if everything went well, otherwise false.
	 */
	public boolean writeLine(String line)
	{
		boolean success = false;
		if (this.canWrite())
		{
			this.getWriter().write(line);
			this.getWriter().write('\n');
			this.getWriter().flush();
			success = true;
		}
		return success;
	} // writeLine()

	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	/**
	 * Returns a reader on the socket's input stream.
	 * Does lazy initialization !
	 */
	protected BufferedReader getReader()
	{
		if (!this.hasReader())
		{
			if (this.hasSocket())
			{
				InputStreamReader isr;
				try
				{
					isr = new InputStreamReader(this.socket().getInputStream());
					BufferedReader r = new BufferedReader(isr);
					this.reader(r);
				}
				catch (IOException e)
				{
					this.exceptionOccurred(e);
				}
			}
		}
		return this.reader();
	} // getReader()

	// -------------------------------------------------------------------------

	/**
	 * Returns a writer on the socket's output stream.
	 * Does lazy initialization !
	 */
	protected PrintWriter getWriter()
	{
		if (!this.hasWriter())
		{
			if (this.hasSocket())
			{
				OutputStreamWriter osw;
				try
				{
					osw = new OutputStreamWriter(this.socket().getOutputStream());
					PrintWriter ps = new PrintWriter(osw);
					this.writer(ps);
				}
				catch (IOException e)
				{
					this.exceptionOccurred(e);
				}
			}
		}
		return this.writer();
	} // getWriter()

	// -------------------------------------------------------------------------

	protected boolean hasReader()
	{
		return this.reader() != null;
	} // hasReader()

	// -------------------------------------------------------------------------

	protected boolean hasWriter()
	{
		return this.writer() != null;
	} // hasWriter()

	// -------------------------------------------------------------------------

	protected boolean canRead()
	{
		return (this.isOpen() && (this.getReader() != null));
	} // canRead()

	// -------------------------------------------------------------------------

	protected boolean canWrite()
	{
		return (this.isOpen() && (this.getWriter() != null));
	} // canWrite()

	// -------------------------------------------------------------------------

	protected void closeReader()
	{
		try
		{
			if (this.hasReader())
			{
				this.reader().close();
				this.reader(null);
			}
		}
		catch (IOException e)
		{
			this.exceptionOccurred(e);
		}
	} // closeReader()

	// -------------------------------------------------------------------------

	protected void closeWriter()
	{
		if (this.hasWriter())
		{
			this.writer().close();
			this.writer(null);
		}
	} // closeWriter()

	// -------------------------------------------------------------------------

} // class TextSocketConnection
