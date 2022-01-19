// ===========================================================================
// CONTENT  : INTERFACE CryptoConstants
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 16/11/2013
// HISTORY  :
//  16/11/2013  mdu  CREATED
//
// Copyright (c) 2013, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.security.crypto ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * This interface provides various constants useful in cryptology. 
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public interface CryptoConstants
{ 
	public static final String ENC_MECHANISM_START = "{";
	public static final String ENC_MECHANISM_END = "}";

	// ==== SHA-1 ====
	public static final String ALGORITHM_NAME_SHA = "SHA"; 
	public static final String ALGORITHM_NAME_SHA_1 = "SHA-1"; 
	public static final String SHA_1_NAME = "SHA"; 
	public static final String SHA_1_PREFIX = ENC_MECHANISM_START + SHA_1_NAME + ENC_MECHANISM_END; 

	public static final String SSHA_1_NAME = "S" + SHA_1_NAME; 
	public static final String SSHA_1_PREFIX = ENC_MECHANISM_START + SSHA_1_NAME + ENC_MECHANISM_END; 

	// ==== SHA-2 ====
	public static final String ALGORITHM_NAME_SHA_2_224 = "SHA-224"; 
	public static final String SHA_2_224_NAME = "SHA224"; 
	public static final String SHA_2_224_PREFIX = ENC_MECHANISM_START + SHA_2_224_NAME + ENC_MECHANISM_END; 
	
	public static final String SSHA_2_224_NAME = "S" + SHA_2_224_NAME; 
	public static final String SSHA_2_224_PREFIX = ENC_MECHANISM_START + SSHA_2_224_NAME + ENC_MECHANISM_END; 
	
	public static final String ALGORITHM_NAME_SHA_2_256 = "SHA-256"; 
	public static final String SHA_2_256_NAME = "SHA256"; 
	public static final String SHA_2_256_PREFIX = ENC_MECHANISM_START + SHA_2_256_NAME + ENC_MECHANISM_END; 
	
	public static final String SSHA_2_256_NAME = "S" + SHA_2_256_NAME; 
	public static final String SSHA_2_256_PREFIX = ENC_MECHANISM_START + SSHA_2_256_NAME + ENC_MECHANISM_END; 
	
	public static final String ALGORITHM_NAME_SHA_2_384 = "SHA-384"; 
	public static final String SHA_2_384_NAME = "SHA384"; 
	public static final String SHA_2_384_PREFIX = ENC_MECHANISM_START + SHA_2_384_NAME + ENC_MECHANISM_END; 
	
	public static final String SSHA_2_384_NAME = "S" + SHA_2_384_NAME; 
	public static final String SSHA_2_384_PREFIX = ENC_MECHANISM_START + SSHA_2_384_NAME + ENC_MECHANISM_END; 
	
	public static final String ALGORITHM_NAME_SHA_2_512 = "SHA-512"; 
	public static final String SHA_2_512_NAME = "SHA512"; 
	public static final String SHA_2_512_PREFIX = ENC_MECHANISM_START + SHA_2_512_NAME + ENC_MECHANISM_END; 
	
	public static final String SSHA_2_512_NAME = "S" + SHA_2_512_NAME; 
	public static final String SSHA_2_512_PREFIX = ENC_MECHANISM_START + SSHA_2_512_NAME + ENC_MECHANISM_END; 

	// ==== MD5 ====
	public static final String ALGORITHM_NAME_MD5 = "MD5"; 
	public static final String MD5_NAME = "MD5"; 
	public static final String MD5_PREFIX = ENC_MECHANISM_START + MD5_NAME + ENC_MECHANISM_END; 
	public static final String SMD5_NAME = "S" + MD5_NAME; 
	public static final String SMD5_PREFIX = ENC_MECHANISM_START + SMD5_NAME + ENC_MECHANISM_END; 
	
} // interface CryptoConstants