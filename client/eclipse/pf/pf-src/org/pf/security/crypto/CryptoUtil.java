// ===========================================================================
// CONTENT  : CLASS CryptoUtil
// AUTHOR   : M.Duchrow
// VERSION  : 2.0 - 23/11/2013
// HISTORY  :
//  22/06/2008  mdu  CREATED
//	23/11/2013	mdu  changed -> added support for SHA-2 and refactored.
//
// Copyright (c) 2008-2013, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.security.crypto;

// ===========================================================================
// IMPORTS
// ===========================================================================
import static org.pf.security.crypto.CryptoConstants.*;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.pf.text.CheckedCharsetName;
import org.pf.text.StringUtil;
import org.pf.util.Base64Converter;

/**
 * This utility class simplifies hashing passwords and particularly comparison
 * of passwords.
 *
 * @author M.Duchrow
 * @version 2.0
 */
public class CryptoUtil
{
	// =========================================================================
	// CONSTANTS
	// =========================================================================
	static final String ENC_MECHANISM_XOR1 = "XOR1";

	static final String ENC_PREFIX_XOR1 = ENC_MECHANISM_START + ENC_MECHANISM_XOR1 + ENC_MECHANISM_END;

	private static final String[] HASH_MECHANISMS = { SHA_1_NAME, SSHA_1_NAME, SHA_2_224_NAME, SSHA_2_224_NAME,
			SHA_2_256_NAME, SSHA_2_256_NAME, SHA_2_384_NAME, SSHA_2_384_NAME, SHA_2_512_NAME, SSHA_2_512_NAME, 
			MD5_NAME, SMD5_NAME };

	private static final String[] NAMES1 = { "IServiceStateChangeListener", "isValidAttributeNameCharacter",
			"GlobalLocalPlaceholderReplacement", "AAssociationProcessor", "getFunctionResolver",
			"2sGajdgda67sgAoe9zhaBdhjtsffsd5da31kMpp5", "ObjectCollectionFilter", "EnumerationIterator",
			"org.pf.text.MatchRuleTypeConverter.java", "AttributeReadWriteAccess" };

	// =========================================================================
	// CLASS VARIABLES
	// =========================================================================
	private static CryptoUtil soleInstance = new CryptoUtil();
	private static final SecureRandom intGenerator = new SecureRandom();

	// =========================================================================
	// INSTANCE VARIABLES
	// =========================================================================

	// =========================================================================
	// CLASS METHODS
	// =========================================================================
	/**
	 * Returns the only instance this class supports (design pattern "Singleton")
	 */
	public static CryptoUtil current()
	{
		return soleInstance;
	} // current() 

	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Initialize the new instance with default values.
	 */
	protected CryptoUtil()
	{
		super();
	} // CryptoUtil() 

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Returns the given string as it is if it does not start with a prefix in 
	 * curly brackets (e.g. {xor}). 
	 * If it starts with a prefix the method returns the string
	 * decrypted according to the algorithm named by the prefix.
	 * 
	 * @param aString The string to return or to convert into plain text
	 * @return The input string unchanged or converted to plain text if it was encrypted and could be decrypted 
	 */
	public String asPlainText(final String aString)
	{
		if (aString == null)
		{
			return null;
		}
		if (aString.startsWith(ENC_MECHANISM_START))
		{
			return this.decryptString(aString);
		}
		return aString;
	} // asPlainText() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given string SSHA encrypted and base64 encoded with
	 * a prefix "{SSHA}".
	 * 
	 * @param aString The string to be encrypted
	 */
	public String sshaEncrypted(final String aString)
	{
		String encrypted;
		byte[] hash;

		if (aString == null)
		{
			return null;
		}
		hash = this.createSSHAhash(aString);
		encrypted = Base64Converter.encodeToString(hash);
		return SSHA_1_PREFIX + encrypted;
	} // sshaEncrypted() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given string SSHA encrypted and base64 encoded with
	 * a prefix "{SSHA}".
	 * 
	 * @param aString The string to be encrypted
	 * @param salt The salt value
	 */
	public String sshaEncrypted(final String aString, final byte[] salt)
	{
		String encrypted;
		byte[] hash;

		if (aString == null)
		{
			return null;
		}
		hash = this.createSSHAhash(aString, salt);
		encrypted = Base64Converter.encodeToString(hash);
		return SSHA_1_PREFIX + encrypted;
	} // sshaEncrypted() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given string SHA encrypted and base64 encoded with
	 * a prefix "{SHA}".
	 * 
	 * @param aString The string to be encrypted
	 */
	public String shaEncrypted(final String aString)
	{
		String encrypted;
		byte[] hash;

		if (aString == null)
		{
			return null;
		}
		hash = this.createSHAhash(aString);
		encrypted = Base64Converter.encodeToString(hash);
		return SHA_1_PREFIX + encrypted;
	} // shaEncrypted() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given string UTF-8 char encoded, hashed with the specified hashing
	 * mechanism (e.g. {@link CryptoConstants#SHA_2_384_NAME})
	 * and base64 encoded with a prefix (e.g. "{SHA256}) for the used mechanism".
	 * Finally the whole string gets base64 encoded and returned.
	 * 
	 * @param aString The string to be hashed.
	 * @param hashMechanism The hashing mechanism to be used (Not algorithm names!). 
	 * See {@link CryptoConstants#SHA_1_NAME}.
	 * @throws CryptoRuntimeException If the specified mechanism is not supported.
	 */
	public String base64HashedUTF8(final String aString, final String hashMechanism)
	{
		String hashed;
		
		hashed = this.hashedUTF8(aString, hashMechanism);
		return Base64Converter.encode(hashed);
	} // base64HashedUTF8() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the given string UTF-8 char encoded, hashed with the specified hashing
	 * mechanism (e.g. {@link CryptoConstants#SHA_2_512_NAME})
	 * and base64 encoded with a prefix (e.g. "{SHA256}) for the used mechanism".
	 * 
	 * @param aString The string to be hashed.
	 * @param hashMechanismName The hashing mechanism to be used (Not algorithm names!). See {@link CryptoConstants}.
	 * @throws CryptoRuntimeException If the specified mechanism is not supported.
	 */
	public String hashedUTF8(final String aString, final String hashMechanismName)
	{
		HashMechanism hashMechanism;

		if (aString == null)
		{
			return null;
		}
		hashMechanism = HashMechanism.findHashMechanism(hashMechanismName);
		return this.hashedUTF8(aString, hashMechanism);
	} // hashedUTF8() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given string UTF-8 char encoded, hashed with the specified hashing
	 * mechanism (e.g. {@link HashMechanism#SALTED_SHA_2_256})
	 * and base64 encoded with a prefix (e.g. "{SSHA256}") for the used mechanism.
	 * 
	 * @param aString The string to be hashed.
	 * @param hashMechanism The hashing mechanism to be used.
	 * @throws CryptoRuntimeException If the specified mechanism is not supported.
	 */
	public String hashedUTF8(final String aString, final HashMechanism hashMechanism)
	{
		HashedString hashedString;
		byte[] bytes;
		byte[] salt = null;
		
		if ((aString == null) || (hashMechanism == null))
		{
			return null;
		}
		if (hashMechanism.isSalted())
		{
			salt = this.generateSalt20();
		}
		bytes = CheckedCharsetName.UTF_8.getBytes(aString);
		hashedString = new HashedString(bytes, salt, hashMechanism);
		return hashedString.asString();
	} // hashedUTF8() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the given data hashed with the specified hashing
	 * mechanism (e.g. {@link HashMechanism#SALTED_SHA_2_512})
	 * and base64 encoded with a prefix (e.g. "{SSHA512}") for the used mechanism.
	 * 
	 * @param data The data to be hashed.
	 * @param hashMechanism The hashing mechanism to be used.
	 * @throws CryptoRuntimeException If the specified mechanism is not supported.
	 */
	public String hashed(final byte[] data, final HashMechanism hashMechanism)
	{
		byte[] hash;
		
		if ((data == null) || (hashMechanism == null))
		{
			return null;
		}
		hash = this.computeHash(data, hashMechanism.getAlgorithm());
		return this.base64EncodedWithPrefix(hash, hashMechanism);
	} // hashed() 
	
	// -------------------------------------------------------------------------

	public String base64EncodedWithPrefix(final byte[] hash, final HashMechanism hashMechanism) 
	{
		String result;
		
		result = Base64Converter.encodeToString(hash);
		result = hashMechanism.getPrefix() + result;
		return result;
	} // base64EncodedWithPrefix() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the given string XOR1 encrypted and base64 encoded with
	 * a prefix "{XOR1}".
	 * 
	 * @param aString The string to be encrypted
	 */
	public String xor1Encrypted(final String aString)
	{
		String encrypted;

		if (this.str().isNullOrBlank(aString))
		{
			return aString;
		}
		encrypted = Base64Converter.encodeToString(this.xor1(aString));
		return ENC_PREFIX_XOR1 + encrypted;
	} // xor1Encrypted() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given string encrypted with the default algorithm and base64 
	 * encoded with a prefix naming the algorithm in curly brackets (e.g. {xor}).
	 * <p>
	 * Currently it uses the XOR encryption.
	 */
	public String defaultEncrypted(final String aString)
	{
		return this.xor1Encrypted(aString);
	} // defaultEncrypted() 

	// -------------------------------------------------------------------------

	/**
	 * Compares whether or not the two given strings are equal.
	 * If one or both keys are encrypted then they get decrypted before being
	 * compared. That allows to compare transparently a plain text string 
	 * against an XOR encrypted string or even an XOR encrypted string against
	 * a SHA hashed value. In the latter case the plain text gets hashed before
	 * the comparison.
	 * <p>
	 * The sting character encoding used here is the platform's default encoding. 
	 */
	public boolean equals(final String str1, final String str2)
	{
		return this.equals(str1, str2, null);
	} // equals() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Compares whether or not the two given strings are equal.
	 * If one or both keys are encrypted then they get decrypted before being
	 * compared. That allows to compare transparently a plain text string 
	 * against an XOR encrypted string or even an XOR encrypted string against
	 * a SHA hashed value. In the latter case the plain text gets hashed before
	 * the comparison.
	 * <p>
	 * The sting character encoding used here is the platform's default encoding. 
	 */
	public boolean equalsUTF8(final String str1, final String str2)
	{
		return this.equals(str1, str2, CheckedCharsetName.UTF_8);
	} // equalsUTF8() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Compares whether or not the two given strings are equal.
	 * If one or both keys are encrypted then they get decrypted before being
	 * compared. That allows to compare transparently a plain text string 
	 * against an XOR encrypted string or even an XOR encrypted string against
	 * a SHA hashed value. In the latter case the plain text gets hashed before
	 * the comparison.
	 * @param str1 First string to compare with second.
	 * @param str2 Second string to compare with first.
	 * @param charEncoding The character encoding used to convert strings to byte arrays. 
	 */
	public boolean equals(final String str1, final String str2, CheckedCharsetName charEncoding)
	{
		if (str1 == str2)
		{
			return true;
		}
		if ((str1 == null) || (str2 == null))
		{
			return false;
		}
		String unencoded1;
		String unencoded2;
		String plainText1;
		String plainText2;

		unencoded1 = this.unencoded(str1);
		unencoded2 = this.unencoded(str2);
		if (this.isHashed(unencoded1))
		{
			if (this.isHashed(unencoded2))
			{
				return unencoded1.equals(unencoded2);
			}
			else
			{
				return this.equalsHashedAgainstEncoded(unencoded1, unencoded2, charEncoding);
			}
		}
		else
		{
			if (this.isHashed(unencoded2))
			{
				return this.equalsHashedAgainstEncoded(unencoded2, unencoded1, charEncoding);
			}
			else
			{
				plainText1 = this.asPlainText(unencoded1);
				plainText2 = this.asPlainText(unencoded2);
				return plainText1.equals(plainText2);
			}
		}
	} // equals() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the byte array containing the xor encoded representation
	 * of the given input string.
	 */
	public byte[] xor1(final String string)
	{
		if (string == null)
		{
			return null;
		}
		return this.obfuscate1(string);
	} // xor1() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the SSHA hash representation of the given string
	 */
	public byte[] createSSHAhash(final String aString)
	{
		return createSSHAhash(aString, this.generateSalt20());
	} // createSSHAhash() 

	// -------------------------------------------------------------------------
	/**
	 * Returns the SSHA hash representation (SHA-1 based) of the given string.
	 * 
	 * @param aString the string to be hashed.
	 * @param salt the salt value for SSHA.
	 */
	public byte[] createSSHAhash(final String aString, final byte[] salt)
	{
		byte[] saltedContent;
		byte[] contentBytes;
		byte[] hash;
		byte[] result;

		contentBytes = aString.getBytes();
		saltedContent = this.concatArrays(contentBytes, salt);
		hash = this.computeHash(saltedContent, CheckedHashAlgorithm.SHA_1);
		result = this.concatArrays(hash, salt);
		return result;
	} // createSSHAhash() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the SHA-1 hash representation of the given string in the current
	 * platform's character encoding.
	 */
	public byte[] createSHAhash(final String aString)
	{
		try
		{
			return this.computeHash(aString.getBytes(), CheckedHashAlgorithm.SHA_1);
		}
		catch (CryptoRuntimeException e)
		{
			// deliberately ignored, because here we cannot use logging
			return null;
		}
	} // createSHAhash() 

	// -------------------------------------------------------------------------

	/**
	 * Computes a hash value for the given input data using the specified hashing algorithm.
	 * The returned byte array contains the hash. Its length depends on the algorithm.
	 * If the given algorithm is not supported a CryptoRuntimeExcption will be thrown.
	 * 
	 * @param content The content to be hashed (must not be null).
	 * @param algorithm The name of the has algorithm (see {@link CryptoConstants}). Must not be null.
	 * @throws CryptoRuntimeExcption If the given algorithm is not supported.
	 */
	public byte[] computeHash(final byte[] content, final String algorithm)
	{
		CheckedHashAlgorithm hashAlgorithm;

		try
		{
			hashAlgorithm = CheckedHashAlgorithm.create(algorithm);
		}
		catch (NoSuchAlgorithmException ex)
		{
			throw new CryptoRuntimeException(ex);
		}
		return this.computeHash(content, hashAlgorithm);
	} // computeHash() 

	// -------------------------------------------------------------------------

	/**
	 * Computes a hash value for the given input data using the specified hashing algorithm.
	 * The returned byte array contains the hash. Its length depends on the algorithm.
	 * If the given algorithm is not supported a CryptoRuntimeExcption will be thrown.
	 * 
	 * @param content The content to be hashed (must not be null).
	 * @param hashAlgorithm The hash mechanism to use. Must not be null.
	 * @throws CryptoRuntimeExcption If the given algorithm is not supported.
	 */
	public byte[] computeHash(final byte[] content, final CheckedHashAlgorithm hashAlgorithm)
	{
		return hashAlgorithm.computeHash(content);
	} // computeHash() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Computes a hash value for the given text using the specified hashing algorithm.
	 * The returned byte array contains the hash. Its length depends on the algorithm.
	 * If the given algorithm is not supported a CryptoRuntimeExcption will be thrown.
	 * 
	 * @param text The content to be hashed (must not be null).
	 * @param charEncoding The character encoding to be used when converting the given text to bytes.
	 * @param algorithm The name of the has algorithm (see {@link CryptoConstants}). Must not be null.
	 * @throws CryptoRuntimeExcption If the given charEncoding or algorithm is not supported.
	 */
	public byte[] computeHash(final String text, final String charEncoding, final String algorithm)
	{
		byte[] bytes;

		try
		{
			bytes = text.getBytes(charEncoding);
		}
		catch (UnsupportedEncodingException ex)
		{
			throw new CryptoRuntimeException(ex);
		}
		return this.computeHash(bytes, algorithm);
	} // computeHash() 

	// -------------------------------------------------------------------------

	/**
	 * Computes a hash value for the given text using the specified hashing algorithm.
	 * The returned byte array contains the hash. Its length depends on the algorithm.
	 * If the given algorithm is not supported a CryptoRuntimeExcption will be thrown.
	 * 
	 * @param text The content to be hashed (must not be null).
	 * @param charEncoding The character encoding to be used when converting the given text to bytes.
	 * @param algorithm The name of the has algorithm (see {@link CryptoConstants}). Must not be null.
	 * @throws CryptoRuntimeExcption If the given charEncoding or algorithm is not supported.
	 */
	public byte[] computeHash(final String text, final CheckedCharsetName charEncoding, final String algorithm)
	{
		return this.computeHash(charEncoding.getBytes(text), algorithm);
	} // computeHash() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Computes a hash value for the given text using UTF-8 character encoding an the 
	 * specified hashing algorithm.
	 * The returned byte array contains the hash. Its length depends on the algorithm.
	 * If the given algorithm is not supported a CryptoRuntimeExcption will be thrown.
	 * 
	 * @param text The content to be hashed (must not be null).
	 * @param algorithmName The name of the has algorithm (see {@link CryptoConstants}). Must not be null.
	 * @throws CryptoRuntimeExcption If the given charEncoding or algorithm is not supported.
	 */
	public byte[] computeUTF8Hash(final String text, final String algorithmName)
	{
		CheckedHashAlgorithm algorithm;

		try
		{
			algorithm = CheckedHashAlgorithm.findOrCreate(algorithmName);
		}
		catch (NoSuchAlgorithmException ex)
		{
			throw new CryptoRuntimeException(ex);
		}
		return algorithm.computeUTF8Hash(text);
	} // computeUTF8Hash() 

	// -------------------------------------------------------------------------

	/**
	 * Generates a random salt with the specified length.
	 * 
	 * @param length The length of the salt to be generated.
	 * @return The salt as byte array with the specified length.
	 */
	public byte[] generateSalt(final int length)
	{
		byte[] salt;

		salt = new byte[length];
		intGenerator.nextBytes(salt);
		return salt;
	} // generateSalt() 

	// -------------------------------------------------------------------------

	/**
	 * Generates a random salt with the default length of 20.
	 * 
	 * @return The salt as byte array with a length of 20.
	 */
	public byte[] generateSalt20()
	{
		return this.generateSalt(20);
	} // generateSalt20() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	protected String decryptString(final String aString)
	{
		String mechanism;
		String text;

		mechanism = this.str().getDelimitedSubstring(aString, ENC_MECHANISM_START, ENC_MECHANISM_END);
		text = this.str().suffix(aString, ENC_MECHANISM_END);
		if (ENC_MECHANISM_XOR1.equalsIgnoreCase(mechanism))
		{
			return this.plainText1(text);
		}
		return aString;
	} // decryptString() 

	// -------------------------------------------------------------------------

	protected boolean equalsHashedAgainstEncoded(final String hashed, final String maybeEncoded, CheckedCharsetName charEncoding)
	{
		String plainText;
		HashedString hashedString;

		plainText = this.asPlainText(maybeEncoded);
		hashedString = new HashedString(hashed);
		if (charEncoding == null)
		{
			if (hashedString.isEqual(plainText))
			{
				return true;
			}
			return hashedString.isEqualUTF8(plainText);			
		}
		return hashedString.isEqual(charEncoding.getBytes(plainText));			
	} // equalsHashedAgainstEncoded() 

	// -------------------------------------------------------------------------

	/**
	 * Tries to Base64 decode the given string. Returns the string unchanged if 
	 * it was not Base64 encoded otherwise returns the decoded string. 
	 */
	protected String unencoded(final String str)
	{
		String decoded;

		if (str.startsWith(ENC_MECHANISM_START))
		{
			return str;
		}
		try
		{
			decoded = Base64Converter.decodeToString(str);
			if (this.isEncrypted(decoded))
			{
				return decoded;
			}
			return str;
		}
		catch (Throwable e)
		{
			return str;
		}
	} // unencoded() 

	// -------------------------------------------------------------------------

	protected boolean isEncrypted(String str)
	{
		return (str.startsWith(ENC_MECHANISM_START) && (str.indexOf(ENC_MECHANISM_END) > 1));
	} // isEncrypted() 

	// -------------------------------------------------------------------------

	protected boolean isHashed(final String str)
	{
		String mechanism;

		mechanism = this.str().getDelimitedSubstring(str, ENC_MECHANISM_START, ENC_MECHANISM_END);
		if (this.str().containsIgnoreCase(HASH_MECHANISMS, mechanism))
		{
			return true;
		}
		return false;
	} // isHashed() 

	// -------------------------------------------------------------------------

	/**
	 * Encrypts the given string according to IAP1 algorithm
	 */
	protected byte[] obfuscate1(final String aStr)
	{
		byte[] bytes;
		int keyNum;
		String result;

		bytes = aStr.getBytes();
		keyNum = intGenerator.nextInt(NAMES1.length);
		this.transform1(bytes, keyNum);
		result = Integer.toString(keyNum) + new String(bytes);
		return result.getBytes();
	} // obfuscate1() 

	// -------------------------------------------------------------------------

	/**
	 * Decrypts the given string which must be IAP1 obfuscated 
	 */
	protected String plainText1(final String string)
	{
		byte[] bytes;
		int keyNum;
		String aStr;

		aStr = Base64Converter.decodeToString(string);
		try
		{
			keyNum = Integer.parseInt(aStr.substring(0, 1));
		}
		catch (NumberFormatException e)
		{
			keyNum = 0;
		}
		bytes = aStr.substring(1).getBytes();
		this.transform1(bytes, keyNum);
		return new String(bytes);
	} // plainText1() 

	// -------------------------------------------------------------------------

	/**
	 * Does a XOR transformation of each byte in the given byte array using the
	 * key specified by the given keyNum. 
	 */
	protected void transform1(final byte[] bytes, final int keyNum)
	{
		byte[] key;
		int keyLen;
		int k = 0;

		key = NAMES1[keyNum].getBytes();
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
	} // transform1() 

	// -------------------------------------------------------------------------

	protected byte[] concatArrays(byte[] bytes1, byte[] bytes2)
	{
		byte[] result;

		result = new byte[bytes1.length + bytes2.length];
		System.arraycopy(bytes1, 0, result, 0, bytes1.length);
		System.arraycopy(bytes2, 0, result, bytes1.length, bytes2.length);
		return result;
	} // concatArrays() 

	// -------------------------------------------------------------------------

	protected StringUtil str()
	{
		return StringUtil.current();
	} // str() 

	// -------------------------------------------------------------------------

} // class CryptoUtil 
