// ===========================================================================
// CONTENT  : CLASS StringObfuscator
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 18/08/2012
// HISTORY  :
//  11/06/2005  mdu  CREATED
//	18/08/2012	mdu	 Bugfix --> modified obfuscate() and plain() to work correct with unicode strings
//
// Copyright (c) 2005-2012, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.textx;

import java.util.Random;

import org.pf.util.Base64Converter;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Simple converter that can convert a string into a obfuscated Base64
 * representation and back to its original. 
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class StringObfuscator
{
	// =========================================================================
	// CONSTANTS
	// =========================================================================
	private final String[] keys = { "TextReplacementException", "AssociationList", "GlobalLocalPlaceholderReplacement",
			"UnknownVariableException", "StringUtil", "ObjectCollectionFilter", "InvalidParameterException",
			"FunctionResolver", "MapWrapper", "SysUtil" };

	// =========================================================================
	// INSTANCE VARIABLES
	// =========================================================================
	private final Random intGenerator = new Random(System.currentTimeMillis());

	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Initialize the new instance with default values.
	 */
	public StringObfuscator()
	{
		super();
	} // StringObfuscator() 

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Obfuscate the given string and return a Base64 encoded representation of it
	 * 
	 * @param aStr The string to obfuscate (must not be null) 
	 */
	public String obfuscate(String aStr)
	{
		byte[] bytes;
		byte[] extendedBytes;
		int keyNum;

		bytes = aStr.getBytes();
		keyNum = intGenerator.nextInt(keys.length);
		xor(bytes, keyNum);
		extendedBytes = new byte[bytes.length+1];
		extendedBytes[0] = (byte)keyNum;
		System.arraycopy(bytes, 0, extendedBytes, 1, bytes.length);
		return Base64Converter.encodeToString(extendedBytes);
	} // obfuscate() 

	// -------------------------------------------------------------------------

	/**
	 * Converts an obfuscated string back to its original plan representation
	 * 
	 * @param aStr The string to convert back to plain text (must not be null)
	 */
	public String plain(String aStr)
	{
		byte[] bytes;
		byte[] extendedBytes;
		int keyNum;
		
		extendedBytes = Base64Converter.decode(aStr);
		keyNum = extendedBytes[0];
		bytes = new byte[extendedBytes.length-1];
		System.arraycopy(extendedBytes, 1, bytes, 0, bytes.length);
		xor(bytes, keyNum);
		return new String(bytes);
	} // plain() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	void xor(byte[] bytes, int keyNum)
	{
		byte[] key;
		int keyLen;
		int k = 0;

		key = keys[keyNum].getBytes();
		keyLen = key.length;
		for (int i = 0; i < bytes.length; i++)
		{
			bytes[i] = (byte)(bytes[i] ^ key[k]);
			k++;
			if (k >= keyLen)
			{
				k = 0;
			}
		}
	} // xor() 

	// -------------------------------------------------------------------------

} // class StringObfuscator 
