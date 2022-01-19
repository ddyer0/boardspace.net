// ===========================================================================
// CONTENT  : CLASS HashedString
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.0 - 16/11/2013
// HISTORY  :
//  24/06/2008  mdu  CREATED
//	17/11/2013	mdu  added   -> SHA-2 (224, 256, 383, 512)
//
// Copyright (c) 2008-2013, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.security.crypto;

// ===========================================================================
// IMPORTS
// ===========================================================================
import static org.pf.security.crypto.CryptoConstants.*;

import java.util.Arrays;

import org.pf.text.CheckedCharsetName;
import org.pf.text.StringUtil;
import org.pf.util.Base64Converter;

/**
 * Container for a SHA hashed strings including salt for SSHA.
 * Supports SHA-1 (160 bit) as well as SHA-2 (256, 383, 512 bit).
 * SHA-2 224 bit is not supported before Java 8 or with a crypto provider
 * explicitly added to the classpath.
 *
 * @author Manfred Duchrow
 * @version 2.0
 */
public class HashedString
{
	// =========================================================================
	// CONSTANTS
	// =========================================================================
	protected static final CryptoUtil CRYPTU = CryptoUtil.current();
		
	// =========================================================================
	// INSTANCE VARIABLES
	// =========================================================================
  private byte[] strHash = null ;
  protected byte[] getStrHash() { return strHash ; }
  protected void setStrHash( byte[] newValue ) { strHash = newValue ; }
  
  private byte[] salt = null ;
  protected byte[] getSalt() { return salt ; }
  protected void setSalt( byte[] newValue ) { salt = newValue ; }  
  
  private boolean isHashed = false ;
  protected boolean isHashed() { return isHashed ; }
  protected void setIsHashed( boolean newValue ) { isHashed = newValue ; }  
  
  private HashMechanism hashMechanism = null;
  protected HashMechanism getHashMechanism() { return hashMechanism; }
  protected void setHashMechanism(HashMechanism info) { hashMechanism = info; }
  
	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Initialize the new instance with a hashed string.
	 * 
	 * @param string A base64 encoded hash of a string with a prefix 
	 * (e.g. {SHA} or {SSHA} or {SHA256} )
	 */
	public HashedString(String string)
	{
		super();
		this.init(string);
	} // HashedString() 

	// -------------------------------------------------------------------------

	public HashedString(byte[] content, String hashType)
	{
		this(content, null, hashType);
	} // HashedString() 

	// -------------------------------------------------------------------------

	public HashedString(byte[] content, HashMechanism mechanism)
	{
		this(content, null, mechanism);
	} // HashedString() 
	
	// -------------------------------------------------------------------------
	
	public HashedString(byte[] content, byte[] salt, String hashType)
	{
		this(content, salt, HashMechanism.findHashMechanism(hashType));
	} // HashedString() 
	
	// -------------------------------------------------------------------------	
	
	public HashedString(byte[] content, byte[] salt, HashMechanism mechanism)
	{
		super();
		this.init(content, salt, mechanism);
	} // HashedString() 

	// -------------------------------------------------------------------------	

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Returns true if the value is salted.
	 */
	public boolean isSalted()
	{
		if (this.getHashMechanism() == null)
		{
			return false;
		}
		return this.getHashMechanism().isSalted();
	}
  
  /**
	 * Compares the given clear text string with the hashed value (which was hashed as UTF-8).
	 * Returns true if the hash of the given string equals the hash in this object.
	 */
	public boolean isEqualUTF8(String string)
	{
		return this.isEqual(CheckedCharsetName.UTF_8.getBytes(string));
	} // isEqualUTF8() 

	// -------------------------------------------------------------------------

	/**
	 * Compares the given clear text string with the hashed value.
	 * Returns true if the hash of the given string equals the hash
	 * in this object.
	 */
	public boolean isEqual(String string)
	{
		return this.isEqual(this.stringToBytes(string));
	} // isEqual() 

	// -------------------------------------------------------------------------

	/**
	 * Compares the given plain text string with the hashed string.
	 * Returns true if the hash of the given plain text equals the hash
	 * in this object.
	 */
	public boolean isEqual(byte[] plainText)
	{
		byte[] otherSaltedString;
		byte[] otherHash;

		if (this.isHashed())
		{
			if (this.isSalted())
			{
				otherSaltedString = new byte[plainText.length + this.getSalt().length];
				System.arraycopy(plainText, 0, otherSaltedString, 0, plainText.length);
				System.arraycopy(this.getSalt(), 0, otherSaltedString, plainText.length, this.getSalt().length);
			}
			else
			{
				otherSaltedString = plainText;
			}
			otherHash = this.getHashMechanism().getAlgorithm().computeHash(otherSaltedString);
		}
		else
		{
			otherHash = plainText;
		}
		return Arrays.equals(this.getStrHash(), otherHash);
	} // isEqual() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the contents base64 with a curly bracket prefix that defines
	 * the used hashing mechanism.
	 * <P>  
	 * Example: "{SSHA256}lKv8GwCDbjB6wvYroiAKaGZXUdWYgtYAnpQWLTrWHPUSzuooRrOT5Yp70mbNwfx0IziQ+g=="
	 */
	public String asString()
	{
		byte[] bytes;

		bytes = (this.getSalt() == null) ? this.getStrHash() : CRYPTU.concatArrays(this.getStrHash(), this.getSalt());
		if (!this.isHashed())
		{
			// char encoding does not matter here because it s not hashed - it is plain text
			return new String(bytes);
		}
		return CRYPTU.base64EncodedWithPrefix(bytes, getHashMechanism());
	} // asString() 

	// -------------------------------------------------------------------------

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "(" + this.asString() + ")";
	} // toString()

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	/**
	 * Initialized this HashedString instance from a Base64 encoded hash that is
	 * prefixed with an algorithm identifier.
	 * @param string Base64 encoded hash with prefix.
	 */
	protected void init(final String string)
	{
		String prefix;
		String base64Hash;
		byte[] hash;
		byte[][] hashAndSalt;
		HashMechanism mechanism;

		prefix = this.str().prefix(string, ENC_MECHANISM_END);
		if (prefix == null) // not hashed at all
		{
			this.setStrHash(this.stringToBytes(string));
			this.setIsHashed(false);
			return;
		}

		this.setIsHashed(true);
		prefix = prefix.toUpperCase() + ENC_MECHANISM_END;
		base64Hash = this.str().suffix(string, ENC_MECHANISM_END);
		hash = Base64Converter.decode(base64Hash);

		mechanism = HashMechanism.findUnsaltedHashMechanism(prefix);
		if (mechanism != null)
		{
			this.setHashMechanism(mechanism);
			this.setStrHash(hash);
		}
		else
		{
			mechanism = HashMechanism.findSaltedHashMechanism(prefix);
			if (mechanism != null)
			{
				this.setHashMechanism(mechanism);
				hashAndSalt = this.split(hash, mechanism.getByteLength());
				this.setStrHash(hashAndSalt[0]);
				this.setSalt(hashAndSalt[1]);
			}
		}
	} // init() 

	// -------------------------------------------------------------------------

	protected void init(final byte[] content, final byte[] saltData, final HashMechanism mechanism)
	{
		byte[] bytesToHash;

		this.setHashMechanism(mechanism);
		this.setSalt(saltData);
		if (mechanism == null)
		{
			this.setIsHashed(false);
			this.setStrHash(content);
		}
		else
		{
			this.setIsHashed(true);
			if (this.isSalted())
			{
				if (this.getSalt() == null)
				{
					this.setSalt(CRYPTU.generateSalt20());
				}
				bytesToHash = CRYPTU.concatArrays(content, this.getSalt());
			}
			else
			{
				bytesToHash = content;
			}
			this.setStrHash(CRYPTU.computeHash(bytesToHash, mechanism.getAlgorithmName()));
		}
	} // init() 

	// -------------------------------------------------------------------------

	protected byte[][] split(byte[] src, int n)
	{
		byte[] l, r;
		if (src.length <= n)
		{
			l = src;
			r = new byte[0];
		}
		else
		{
			l = new byte[n];
			r = new byte[src.length - n];
			System.arraycopy(src, 0, l, 0, n);
			System.arraycopy(src, n, r, 0, r.length);
		}
		byte[][] lr = { l, r };
		return lr;
	} // split() 

	// -------------------------------------------------------------------------

	/**
	 * Converts the given string to a byte array.
	 * This default implementation is using the platform's default character encoding.
	 * Subclasses my override this method to use other encoding (e.g. UTF-8).
	 */
	protected byte[] stringToBytes(final String string)
	{
		if (string == null)
		{
			return null;
		}
		return string.getBytes();
	} // stringToBytes() 

	// -------------------------------------------------------------------------

	protected StringUtil str()
	{
		return StringUtil.current();
	} // str() 

	//-------------------------------------------------------------------------

} // class HashedString 
