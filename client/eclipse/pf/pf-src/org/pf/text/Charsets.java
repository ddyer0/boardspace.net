// ===========================================================================
// CONTENT  : INTERFACE Charsets
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 11/01/2014
// HISTORY  :
//  11/01/2014  mdu  CREATED
//
// Copyright (c) 2014, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.text ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.nio.charset.Charset;

/**
 * Provides constants for the standard character sets that are available in every JRE.
 * They are useful to avoid the annoying try-catch blocks for UnsupportedEncodingException
 * when used with String.getBytes(Charset) or String(byte[], Charset). These methods are
 * available since Java 6. 
 * Can also be used with InputStreamReader(InputStream, Charset) and
 * OutputStreamWriter(OutputStream, Charset) which are available since Java 1.4.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public interface Charsets
{ 
	public static final Charset US_ASCII = Charset.forName("US-ASCII");

	public static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

	public static final Charset UTF_8 = Charset.forName("UTF-8");
	
	public static final Charset UTF_16 = Charset.forName("UTF-16");
	
	public static final Charset UTF_16LE = Charset.forName("UTF-16LE");

	public static final Charset UTF_16BE = Charset.forName("UTF-16BE");
	
	public static final Charset JVM_DEFAULT = Charset.defaultCharset();
} // interface Charsets