// ===========================================================================
// CONTENT  : CLASS HashMechanism
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
import static org.pf.security.crypto.CryptoConstants.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains all information about a hashing algorithm.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class HashMechanism implements Serializable
{
	// =========================================================================
	// CONSTANTS
	// =========================================================================
	static final long serialVersionUID = 13540657623766355L;
	
	public static final HashMechanism MD5 = HashMechanism.create(CheckedHashAlgorithm.MD5);
	public static final HashMechanism SHA_1 = HashMechanism.create(CheckedHashAlgorithm.SHA_1);
	public static final HashMechanism SHA_2_256 = HashMechanism.create(CheckedHashAlgorithm.SHA_2_256);
	public static final HashMechanism SHA_2_384 = HashMechanism.create(CheckedHashAlgorithm.SHA_2_384);
	public static final HashMechanism SHA_2_512 = HashMechanism.create(CheckedHashAlgorithm.SHA_2_512);

	public static final HashMechanism SALTED_MD5 = HashMechanism.create(CheckedHashAlgorithm.MD5, true);
	public static final HashMechanism SALTED_SHA_1 = HashMechanism.create(CheckedHashAlgorithm.SHA_1, true);
	public static final HashMechanism SALTED_SHA_2_256 = HashMechanism.create(CheckedHashAlgorithm.SHA_2_256, true);
	public static final HashMechanism SALTED_SHA_2_384 = HashMechanism.create(CheckedHashAlgorithm.SHA_2_384, true);
	public static final HashMechanism SALTED_SHA_2_512 = HashMechanism.create(CheckedHashAlgorithm.SHA_2_512, true);

	private static final Map<String, HashMechanism> UNSALTED_MECHANISMS = new HashMap<String, HashMechanism>();
	private static final Map<String, HashMechanism> SALTED_MECHANISMS = new HashMap<String, HashMechanism>();
	static
	{
		UNSALTED_MECHANISMS.put(MD5.getPrefix(), MD5);
		UNSALTED_MECHANISMS.put(SHA_1.getPrefix(), SHA_1);
		UNSALTED_MECHANISMS.put(SHA_2_256.getPrefix(), SHA_2_256);
		UNSALTED_MECHANISMS.put(SHA_2_384.getPrefix(), SHA_2_384);
		UNSALTED_MECHANISMS.put(SHA_2_512.getPrefix(), SHA_2_512);

		SALTED_MECHANISMS.put(SALTED_MD5.getPrefix(), SALTED_MD5);
		SALTED_MECHANISMS.put(SALTED_SHA_1.getPrefix(), SALTED_SHA_1);
		SALTED_MECHANISMS.put(SALTED_SHA_2_256.getPrefix(), SALTED_SHA_2_256);
		SALTED_MECHANISMS.put(SALTED_SHA_2_384.getPrefix(), SALTED_SHA_2_384);
		SALTED_MECHANISMS.put(SALTED_SHA_2_512.getPrefix(), SALTED_SHA_2_512);
	}
	
	// =========================================================================
	// INSTANCE VARIABLES
	// =========================================================================
	private String hashMechanismName = null;
	private CheckedHashAlgorithm algorithm = null;
	private boolean isSalted = false;
	private String prefix = null;

	// =========================================================================
	// CLASS METHODS
	// =========================================================================
	/**
	 * Returns a prefix for the provided hashing mechanism name.
	 * @param hashMechanismName The name to be used in the prefix (must not be null).
	 */
	public static String createPrefix(String hashMechanismName) 
	{
		return ENC_MECHANISM_START + hashMechanismName.toUpperCase() + ENC_MECHANISM_END;
	} // createPrefix() 
	
	// -------------------------------------------------------------------------

	/**
	 * Returns a name for the hashing mechanism that is defined
	 * by the given parameters.
	 * <p>
	 * Example: algorithm is "SHA-256" and isSalted = true. 
	 * That returns "SSHA256".
	 * 
	 * @param algorithm Defines the underlying hashing algorithm.
	 * @param isSalted Defines whether or not the mechanism is salted.
	 */
	public static String createHashMechanismName(CheckedHashAlgorithm algorithm, boolean isSalted) 
	{
		String name;

		name = algorithm.getName();
		if (name.equals(CryptoConstants.ALGORITHM_NAME_SHA_1))
		{
			name = CryptoConstants.SHA_1_NAME;
		}
		name = name.replace("-", "");
		if (isSalted)
		{
			name = "S" + name;
		}
		return name;
	} // createHashMechanismName() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Creates a new instance that represents a particular unsalted hashing mechanism.
	 * 
	 * @param algorithm The underlying hashing algorithm.
	 */
	public static HashMechanism create(CheckedHashAlgorithm algorithm) 
	{
		return create(algorithm, false);
	} // create() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Creates a new instance that represents a particular hashing mechanism.
	 * 
	 * @param algorithm The underlying hashing algorithm.
	 * @param isSalted Defines whether or not the mechanism is salted.
	 */
	public static HashMechanism create(CheckedHashAlgorithm algorithm, boolean isSalted) 
	{
		return create(HashMechanism.createHashMechanismName(algorithm, isSalted), algorithm, isSalted);
	} // create() 
	
	// -------------------------------------------------------------------------

	/**
	 * Creates a new instance that represents a particular hashing mechanism.
	 * 
	 * @param hashMechanismName The name of the mechanism to be used in the prefix.
	 * @param algorithm The underlying hashing algorithm.
	 * @param isSalted Defines whether or not the mechanism is salted.
	 */
	public static HashMechanism create(String hashMechanismName, CheckedHashAlgorithm algorithm, boolean isSalted)
	{
		return new HashMechanism(hashMechanismName, algorithm, isSalted);
	} // create() 
	
	// -------------------------------------------------------------------------
	
	public static HashMechanism findUnsaltedHashMechanism(String hashType)
	{
		return findHashMechanismIn(hashType, UNSALTED_MECHANISMS);
	} // findUnsaltedHashMechanism() 
	
	// -------------------------------------------------------------------------

	public static HashMechanism findSaltedHashMechanism(String hashType)
	{
		return findHashMechanismIn(hashType, SALTED_MECHANISMS);
	} // findSaltedHashMechanism() 
	
	// -------------------------------------------------------------------------

	/**
	 * Tries to find a known (i.e. registered constant) hash mechanism for the given
	 * type name.
	 * 
	 * @param hashType The name of an hashing algorithm ("SHA-1") or the prefix (e.g. "{SSHA384}").
	 * @return The found mechanism or null if not found.
	 */
	public static HashMechanism findHashMechanism(String hashType)
	{
		HashMechanism result;

		result = findUnsaltedHashMechanism(hashType);
		if (result == null)
		{
			result = findSaltedHashMechanism(hashType);
		}
		return result;
	} // findHashMechanism() 

	// -------------------------------------------------------------------------

	private static HashMechanism findHashMechanismIn(String hashType, Map<String, HashMechanism> infoMap)
	{
		HashMechanism result;
		String prefix;

		if (hashType == null)
		{
			return null;
		}
		if (isPrefix(hashType))
		{
			prefix = hashType.toUpperCase();
		}
		else
		{
			prefix = createPrefix(hashType);
		}
		result = infoMap.get(prefix);
		if (result == null)
		{
			for (HashMechanism info : infoMap.values())
			{
				if (info.getAlgorithmName().equals(hashType))
				{
					result = info;
					break;
				}
			}
		}
		return result;
	} // findHashMechanismIn() 
	
	// -------------------------------------------------------------------------

	private static boolean isPrefix(String str)
	{
		return (str.startsWith(ENC_MECHANISM_START) && str.endsWith(ENC_MECHANISM_END));
	} // isPrefix() 

	// -------------------------------------------------------------------------
	
	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Creates a new instance that represents a particular hashing mechanism.
	 * 
	 * @param hashMechanismName The name of the mechanism to be used in the prefix.
	 * @param algorithm The underlying hashing algorithm.
	 * @param isSalted Defines whether or not the mechanism is salted.
	 */
	public HashMechanism(String hashMechanismName, CheckedHashAlgorithm algorithm, boolean isSalted)
	{
		super();
		this.setHashMechanismName(hashMechanismName);
		this.setAlgorithm(algorithm);
		this.setIsSalted(isSalted);
		this.setPrefix(HashMechanism.createPrefix(hashMechanismName));
	} // HashMechanism() 

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Returns the name of this mechanism.
	 */
	public String getName() 
	{
		return this.getHashMechanismName();
	} // getName() 
	
	// -------------------------------------------------------------------------
	
	public CheckedHashAlgorithm getAlgorithm()
	{
		return algorithm;
	} // getAlgorithm() 
	
	// -------------------------------------------------------------------------
	
	public int getByteLength()
	{
		return this.getAlgorithm().getByteLength();
	} // getByteLength() 
	
	// -------------------------------------------------------------------------

	public String getPrefix()
	{
		return prefix;
	} // getPrefix() 
	
	// -------------------------------------------------------------------------

	public boolean isSalted()
	{
		return isSalted;
	} // getIsSalted() 
	
	// -------------------------------------------------------------------------

	public String getAlgorithmName()
	{
		return this.getAlgorithm().getName();
	} // getAlgorithmName() 

	// -------------------------------------------------------------------------
	
	public int getBitLength() 
	{
		return this.getAlgorithm().getBitLength();
	} // getBitLength() 
	
	// -------------------------------------------------------------------------

	@Override
	/**
	 * Returns a debug string containing the class name and the hash mechanism prefix.
	 */
	public String toString()
	{
		return HashMechanism.class.getSimpleName() + "(" + this.getHashMechanismName() + ")";
	} // toString() 

	// -------------------------------------------------------------------------

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof HashMechanism)
		{
			HashMechanism other = (HashMechanism)obj;
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
	
	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	protected String getHashMechanismName()
	{
		return hashMechanismName;
	} // getHashMechanismName() 
	
	// -------------------------------------------------------------------------
	
	protected void setAlgorithm(CheckedHashAlgorithm newValue)
	{
		algorithm = newValue;
	} // setAlgorithm() 
	
	// -------------------------------------------------------------------------
	
	protected void setHashMechanismName(String newValue)
	{
		hashMechanismName = newValue;
	} // setHashMechanismName() 
	
	// -------------------------------------------------------------------------
	
	protected void setPrefix(String newValue)
	{
		prefix = newValue;
	} // setPrefix() 
	
	// -------------------------------------------------------------------------
	
	protected void setIsSalted(boolean newValue)
	{
		isSalted = newValue;
	} // setIsSalted() 

} // class HashMechanism 
