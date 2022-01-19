// ===========================================================================
// CONTENT  : CLASS CryptoRuntimeException
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 16/11/2013
// HISTORY  :
//  16/11/2013  mdu  CREATED
//
// Copyright (c) 2013, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.security.crypto;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.lang.RuntimeException;

/**
 * A simple runtime exception class that is used to encapsulate checked
 * exceptions in cases where catching exceptions makes no sense or is 
 * polluting the code.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class CryptoRuntimeException extends RuntimeException
{
	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	public CryptoRuntimeException()
	{
		super();
	} // CryptoRuntimeException()

	public CryptoRuntimeException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public CryptoRuntimeException(String message)
	{
		super(message);
	}

	public CryptoRuntimeException(Throwable cause)
	{
		super(cause);
	}

} // class CryptoRuntimeException
