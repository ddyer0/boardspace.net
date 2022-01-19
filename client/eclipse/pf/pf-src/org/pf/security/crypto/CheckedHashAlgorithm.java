// ===========================================================================
// CONTENT  : CLASS CheckedHashAlgorithm
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 23/11/2013
// HISTORY  :
//  23/11/2013  mdu  CREATED
//
// Copyright (c) 2013, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.security.crypto;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.pf.text.CheckedCharsetName;

/**
 * This is a helper class that represents a hash algorithm that has already
 * been checked if it is supported by the platform.
 * <p>
 * It helps to avoid the annoying try-catch code-pollution every time you
 * use a valid algorithm, but the API requires the handling of NoSuchAlgorithmException
 * even if it cannot occur at all. 
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class CheckedHashAlgorithm implements Serializable
{
	// =========================================================================
	// CONSTANTS
	// =========================================================================
	static final long serialVersionUID = 20516573828075112L;
	
	/**
	 * The pre-defined constant for the MD hash algorithm.
	 */
	public static final CheckedHashAlgorithm MD5 = internalCreate(CryptoConstants.ALGORITHM_NAME_MD5);

	/**
	 * The pre-defined constant for the SHA-1 hash algorithm.
	 */
	public static final CheckedHashAlgorithm SHA_1 = internalCreate(CryptoConstants.ALGORITHM_NAME_SHA_1);

	/**
	 * The pre-defined constant for the SHA-256 hash algorithm.
	 */
	public static final CheckedHashAlgorithm SHA_2_256 = internalCreate(CryptoConstants.ALGORITHM_NAME_SHA_2_256);
	
	/**
	 * The pre-defined constant for the SHA-384 hash algorithm.
	 */
	public static final CheckedHashAlgorithm SHA_2_384 = internalCreate(CryptoConstants.ALGORITHM_NAME_SHA_2_384);
	
	/**
	 * The pre-defined constant for the SHA-512 hash algorithm.
	 */
	public static final CheckedHashAlgorithm SHA_2_512 = internalCreate(CryptoConstants.ALGORITHM_NAME_SHA_2_512);
	
	public static final int UNKNOWN_LENGTH = -1;
	
	private static final CheckedHashAlgorithm[] PREDEFINED_ALGORITHMS =
		{ MD5, SHA_1, SHA_2_256, SHA_2_384, SHA_2_512 };
	
	// =========================================================================
	// INSTANCE VARIABLES
	// =========================================================================
	private String algorithmName;
	private int byteLength;

	// =========================================================================
	// CLASS METHODS
	// =========================================================================
	/**
	 * Checks whether or not the given algorithm is supported.
	 * 
	 * @param algorithmName The name of the algorithm to check (e.g. "SHA-1")
	 * @throws NoSuchAlgorithmException Will be thrown if the algorithm is not supported.
	 */
	public static void checkAlgorithm(String algorithmName) throws NoSuchAlgorithmException 
	{
		if (algorithmName == null)
		{
			throw new NoSuchAlgorithmException("<null> is no valid hash algorithm name!");
		}
		MessageDigest.getInstance(algorithmName);
	} // checkAlgorithm() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns whether or not the given algorithm is supported.
	 * 
	 * @param algorithmName The name of the algorithm to check (e.g. "SHA-256")
	 */
	public static boolean isSupportedAlgorithm(String algorithmName) 
	{
		try
		{
			checkAlgorithm(algorithmName);
			return true;
		}
		catch (NoSuchAlgorithmException ex)
		{
			return false;
		}
	} // isSupportedAlgorithm() 
	
	// -------------------------------------------------------------------------

	/**
	 * Creates a new instance based on the given algorithm name.
	 * 
	 * @param algorithmName The name of the algorithm to create (e.g. "UTF-16")
	 * @return Returns a new instance of CheckedHashAlgorithm after checking the given algorithm name
	 * @throws NoSuchAlgorithmException Will be thrown if the algorithm is not supported.
	 */
	public static CheckedHashAlgorithm create(String algorithmName) throws NoSuchAlgorithmException 
	{
		return new CheckedHashAlgorithm(algorithmName);
	} // create() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Looks for a predefined CheckedHashAlgorithm constant with the given 
	 * algorithm name and returns it.
	 * Returns null if not found.
	 * 
	 * @param algorithmName The name of the hash algorithm.
	 */
	public static CheckedHashAlgorithm find(String algorithmName)
	{
		for (CheckedHashAlgorithm algorithm : PREDEFINED_ALGORITHMS)
		{
			if (algorithm.getName().equalsIgnoreCase(algorithmName))
			{
				return algorithm;
			}
		}
		return null;
	} // find() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Looks for a predefined CheckedHashAlgorithm constant with the given 
	 * algorithm name and returns it.
	 * Tries to create a new instance if not found.
	 * 
	 * @param algorithmName The name of the hash algorithm.
	 * @throws NoSuchAlgorithmException If the specified algorithm is not supported.
	 */
	public static CheckedHashAlgorithm findOrCreate(String algorithmName) throws NoSuchAlgorithmException
	{
		CheckedHashAlgorithm algorithm;
		
		algorithm = find(algorithmName);
		if (algorithm == null)
		{
			algorithm = create(algorithmName);
		}
		return algorithm;
	} // findOrCreate() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns an array of all CheckedHashAlgorithm constants defined by this class.
	 */
	public static CheckedHashAlgorithm[] getPredefinedAlgorithms() 
	{
		return PREDEFINED_ALGORITHMS.clone();
	} // getPredefinedAlgorithms() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Tries to extract the bit length part of the given algorithm name and return it
	 * as an integer.
	 * 
	 * @param algorithmName The algorithm name (e.g. "SHA-256").
	 * @return The bit length derived from the name or {@link #UNKNOWN_LENGTH}.
	 */
	public static int detectBitLength(final String algorithmName) 
	{
		int index;
		String lengthPart;
		
		if (algorithmName == null)
		{
			return UNKNOWN_LENGTH;
		}
		if (algorithmName.equals(CryptoConstants.ALGORITHM_NAME_MD5))
		{
			return 128;
		}
		if (algorithmName.equals(CryptoConstants.ALGORITHM_NAME_SHA_1) || algorithmName.equals(CryptoConstants.ALGORITHM_NAME_SHA))
		{
			return 160;
		}
		index = algorithmName.indexOf('-');
		if (index > 0)
		{
			lengthPart = algorithmName.substring(index+1);
			try
			{
				return Integer.parseInt(lengthPart);
			}
			catch (NumberFormatException ex)
			{
				return UNKNOWN_LENGTH;
			}
		}
		return UNKNOWN_LENGTH;
	} // detectBitLength() 
	
	// -------------------------------------------------------------------------
	
	private static CheckedHashAlgorithm internalCreate(String algorithmName) 
	{
		try
		{
			return new CheckedHashAlgorithm(algorithmName);
		}
		catch (NoSuchAlgorithmException ex)
		{
			ex.printStackTrace();
			return null;
		}
	} // internalCreate() 
	
	// -------------------------------------------------------------------------
	
	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Creates successfully a checked algorithm object or throws an exception.
	 * 
	 * @param algorithmName The name of the algorithm (e.g. @see {@link CryptoConstants#ALGORITHM_NAME_SHA_2_512}).
	 * @throws NoSuchAlgorithmException If the specified algorithm is not supported.
	 */
	protected CheckedHashAlgorithm(String algorithmName) throws NoSuchAlgorithmException
	{
		super();
		CheckedHashAlgorithm.checkAlgorithm(algorithmName);
		this.setAlgorithmName(algorithmName.toUpperCase());
		this.setByteLength(this.detectBitLength() / 8);
	} // CheckedHashAlgorithm() 

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Returns the uppercase name of the hashing algorithm.
	 */
	public String getName() 
	{
		return this.algorithmName;
	} // getName() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the algorithm as prefix (i.e. surrounded by curly braces).
	 * <p>
	 * Example: algorithm "SHA-512" returns prefix "{SHA512}"   
	 */
	public String getPrefix() 
	{
		String prefix;
		
		if (this.getName().equals(CryptoConstants.ALGORITHM_NAME_SHA_1))
		{
			return CryptoConstants.SHA_1_PREFIX;
		}
		prefix = this.getName().replace("-", "");
		prefix = CryptoConstants.ENC_MECHANISM_START + prefix + CryptoConstants.ENC_MECHANISM_END;
		return prefix;
	} // getPrefix() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the byte length of this algorithm.
	 * @return The hash length in bytes of this algorithm or {@link #UNKNOWN_LENGTH} 
	 * if it is unknown.
	 */
	public int getByteLength() 
	{
		return this.byteLength;
	} // getByteLength() 
	
	// -------------------------------------------------------------------------

	/**
	 * Returns the bit length of this algorithm.
	 * @return The hash length in bits of this algorithm or {@link #UNKNOWN_LENGTH} 
	 * if it is unknown.
	 */
	public int getBitLength() 
	{
		if (this.getByteLength() > 0)
		{			
			return this.getByteLength() * 8;
		}
		return UNKNOWN_LENGTH;
	} // getBitLength() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Computes a hash value for the given text using the this hashing algorithm
	 * and UTF-8 encoding for the text conversion to bytes.
	 * The returned byte array contains the hash. Its length depends on the algorithm.
	 * 
	 * @param text The text to be hashed (must not be null).
	 * @throws CryptoRuntimeExcption If the anything goes wrong.
	 */
	public byte[] computeUTF8Hash(final String text)
	{
		return this.computeHash(text, CheckedCharsetName.UTF_8);
	} // computeUTF8Hash() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Computes a hash value for the given text using the this hashing algorithm.
	 * The returned byte array contains the hash. Its length depends on the algorithm.
	 * 
	 * @param text The text to be hashed (must not be null).
	 * @param encoding Specifies the character encoding to use for the string when it gets converted to bytes.  
	 * @throws CryptoRuntimeExcption If the anything goes wrong.
	 */
	public byte[] computeHash(final String text, final CheckedCharsetName encoding)
	{
		byte[] bytes;
		
		bytes = encoding.getBytes(text);
		return this.computeHash(bytes);
	} // computeHash() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Computes a hash value for the given input data using the this hashing algorithm.
	 * The returned byte array contains the hash. Its length depends on the algorithm.
	 * 
	 * @param content The content to be hashed (must not be null).
	 * @throws CryptoRuntimeExcption If the anything goes wrong.
	 */
	public byte[] computeHash(final byte[] content)
	{
		MessageDigest digest;
		byte[] hash;

		digest = this.getMessageDigest();
		hash = digest.digest(content);
		return hash;
	} // computeHash() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a MessageDigest for the specified algorithm name.
	 * Since this is an already checked algorithm no exception is expected.
	 * If still an exception occurs it will be caught and a CryptoRuntimeException
	 * thrown instead (should never happen).
	 */
	public MessageDigest getMessageDigest()
	{
		try
		{
			return MessageDigest.getInstance(this.getName());
		}
		catch (NoSuchAlgorithmException ex)
		{
			throw new CryptoRuntimeException(ex);
		}
	} // getMessageDigest() 
	
	// -------------------------------------------------------------------------
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof CheckedHashAlgorithm)
		{
			CheckedHashAlgorithm other = (CheckedHashAlgorithm)obj;
			return this.getName().equals(other.getName());
		}
		return false;
	} // equals() 
	
	// -------------------------------------------------------------------------
	
	@Override
	public int hashCode()
	{
		return this.getName().hashCode();
	} // hashCode() 
	
	// -------------------------------------------------------------------------
	
	@Override
	/**
	 * Returns a debug string containing class name and algorithm name.
	 */
	public String toString()
	{
		return this.getClass().getSimpleName() + "(" + this.getName() + ")";
	} // toString() 
	
	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	protected void setAlgorithmName(String algorithmName)
	{
		this.algorithmName = algorithmName;
	} // setAlgorithmName() 
	
	// -------------------------------------------------------------------------
	
	protected void setByteLength(int byteLength)
	{
		this.byteLength = byteLength;
	} // setByteLength()
	
	// -------------------------------------------------------------------------
	
	/**
	 * Tries to derive the bit length from the algorithm's name.
	 * @return The bit length or {@link #UNKNOWN_LENGTH} if not detectable.
	 */
	protected int detectBitLength() 
	{
		return CheckedHashAlgorithm.detectBitLength(this.getName());
	} // detectBitLength() 
	
	// -------------------------------------------------------------------------
	
} // class CheckedHashAlgorithm 
