// ===========================================================================
// CONTENT  : CLASS FileUtil
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.1 - 11/01/2014
// HISTORY  :
//  17/05/2002  duma  CREATED
//	14/03/2003	duma	added		->	standardize(), javaFilename()
//	20/12/2003	duma	added		->	close(zipFile)
//	05/02/2004	duma	added		->	copyFile() methods
//	06/03/2004	duma	added		->	close(reader), close(writer)
//	03/09/2004	duma	added		->	4 copyToTempFile() methods
//	24/02/2006	mdu		added		->	readTextLines(...)
//	03/03/2006	mdu		added		->	convertFromURLSyntax()
//	28/07/2006	mdu		changed	->	performance optimization
//	27/04/2008	mdu		changed	->	calculateClasspath()
//	28/08/2012	mdu		added		->	readTextFrom() and readTextLinesFrom() methods supporting encoding
//	11/01/2014	mdu		added		->	methods with Charset parameter
//
// Copyright (c) 2002-2014, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.file ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;

import org.pf.reflect.Dynamic;
import org.pf.reflect.ReflectUtil;
import org.pf.text.CheckedCharsetName;
import org.pf.text.StringFilter;
import org.pf.text.StringPattern;
import org.pf.text.StringUtil;
import org.pf.textx.BasicVariableContainer;
import org.pf.textx.TextEngine;
import org.pf.textx.TextReplacementException;
import org.pf.textx.VariableResolver;
import org.pf.util.OrderedSet;
import org.pf.util.SysUtil;

/**
 * This class provides helper methods for file and stream handling.
 * It's an add-on to the java.io package.
 * The service is implemented as a singleton, so use the
 * <b>FileUtil.current()</b> method to get the sole instance.
 *
 * @author Manfred Duchrow
 * @version 2.1
 */
public class FileUtil
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  private static final String SYS_PROP_CLASSPATH = "java.class.path";
  private static final String SYS_PROP_EXT_DIRS = "java.ext.dirs" ;
	private static final String JAR_FILE_EXTENSION = "jar";
	/** The lines.separator from the system properties as a constant */
	public static final String LINE_SEPARATOR	= System.getProperty( "line.separator" ) ;
	protected static final int DEFAULT_BUFFER_SIZE	= 1024 ;

	/**
	 * The constant that contains the default prefix for temporary file.
	 * It is "PFFU" (<b>P</b>rogrammer's <b>F</b>riend <b>F</b>ile<b>U</b>til).
	 */
	public static final String DEFAULT_TEMP_FILE_PREFIX = "PFFU" ;

	/**
	 * Contains the default URL string encoding used in this class: "UTF-8"
	 */
	public static final String DEFAULT_URL_STR_ENCODING = "UTF-8" ;

	protected static final String FILE_PROTOCOL_INDICATOR = "file:" ;
	protected static final StringPattern LOCAL_PATTERN = StringPattern.create( "*file:?*" ) ;
	protected static final StringPattern REMOTE_PATTERN = StringPattern.create( "*?://?*" ) ;
	protected static final StringPattern JAR_PATTERN = StringPattern.create( "*jar:*:*!/*" ) ;
	protected static final StringPattern DRIVE_LETTER_PATTERN_1 = StringPattern.create( "?:" ) ;
	protected static final StringPattern DRIVE_LETTER_PATTERN_2 = StringPattern.create( "?:?*" ) ;
	protected static final StringPattern WINDOWS_DRIVE_PATTERN_1 = StringPattern.create( "\\?:*" ) ;
	protected static final StringPattern WINDOWS_DRIVE_PATTERN_2 = StringPattern.create( "/?:*" ) ;

	private static final FileUtil current = new FileUtil() ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private Classpath calculatedClasspath = null ;
  private String classpathString = null ;
  private Classpath systemClasspath = null ;

	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Initialize the new instance with default values.
	 */
	protected FileUtil()
	{
		super();
	} // FileUtil() 

	// =========================================================================
	// PUBLIC CLASS METHODS
	// =========================================================================
	public static FileUtil current()
	{
		return current;
	} // current() 

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================

	/**
	 * Copies all data from the input stream to the output stream using
	 * a buffer with the default size (1024 bytes).
	 * After all data is copied both streams will be closed !
	 */
	public void copyStream(InputStream inStream, OutputStream outStream) throws IOException
	{
		this.copyStream(inStream, outStream, DEFAULT_BUFFER_SIZE);
	} // copyStream() 

	// -------------------------------------------------------------------------

	/**
	 * Copies all data from the iniput stream to the output stream using
	 * a buffer of the given size in bytes.
	 * After all data is copied both streams will be closed !
	 */
	public void copyStream(InputStream inStream, OutputStream outStream, int bufSize) throws IOException
	{
		byte[] buffer = new byte[bufSize];
		int count;

		try
		{
			count = inStream.read(buffer);
			while (count > -1)
			{
				outStream.write(buffer, 0, count);
				count = inStream.read(buffer);
			}
		}
		finally
		{
			this.close(inStream);
			this.close(outStream);
		}
	} // copyStream() 

	// -------------------------------------------------------------------------

	/**
	 * Copies the whole contents of the file specified by sourceFile to the file
	 * specified by destFile.
	 * If destFile already exists, it will be overwritten without warning!
	 * If preservetimestamp is true, the last modified time of the destFile will
	 * be set to that of the sourceFile. Otherwise it will be the current date.
	 *
	 * @param sourceFile The file to copy
	 * @param destFile The destination where to copy the file content
	 * @param preserveTimestamp A flag that indicates if destFile must have the same timestamp as sourceFile
	 * @throws IOException
	 */
	public void copyFile(File sourceFile, File destFile, boolean preserveTimestamp) throws IOException
	{
		FileInputStream inStream = null;
		FileOutputStream outStream = null;

		try
		{
			inStream = new FileInputStream(sourceFile);
			outStream = new FileOutputStream(destFile);
			this.copyStream(inStream, outStream);
		}
		finally
		{
			this.close(inStream);
			this.close(outStream);
		}
		if (preserveTimestamp)
		{
			destFile.setLastModified(sourceFile.lastModified());
		}
	} // copyFile() 

	// -------------------------------------------------------------------------

	/**
	 * Copies the whole contents of the file specified by sourceFile to the file
	 * specified by destFile.
	 * If destFile already exists, it will be overwritten without warning!
	 * If preservetimestamp is true, the last modified time of the destFile will
	 * be set to that of the sourceFile. Otherwise it will be the current date.
	 *
	 * @param sourceFile The file to copy
	 * @param destFile The destination where to copy the file content
	 * @param preserveTimestamp A flag that indicates if destFile must have the same timestamp as sourceFile
	 * @throws IOException
	 */
	public void copyFile(FileLocator sourceFile, File destFile, boolean preserveTimestamp) throws IOException
	{
		InputStream inStream = null;
		FileOutputStream outStream = null;

		try
		{
			inStream = sourceFile.getInputStream();
			outStream = new FileOutputStream(destFile);
			this.copyStream(inStream, outStream);
		}
		finally
		{
			this.close(inStream);
			this.close(outStream);
		}
		if (preserveTimestamp)
		{
			destFile.setLastModified(sourceFile.lastModified());
		}
	} // copyFile() 

	// -------------------------------------------------------------------------

	/**
	 * Copies the whole contents of the file specified by source URL to the file
	 * specified by destFile.
	 * If destFile already exists, it will be overwritten without warning!
	 *
	 * @param source An URL that specifies the source file that must be copied
	 * @param destFile The destination where to copy the file content
	 * @throws IOException
	 */
	public void copyFile(URL source, File destFile) throws IOException
	{
		InputStream inStream = null;
		FileOutputStream outStream = null;

		try
		{
			inStream = source.openStream();
			outStream = new FileOutputStream(destFile);
			this.copyStream(inStream, outStream);
		}
		finally
		{
			this.close(inStream);
			this.close(outStream);
		}
	} // copyFile() 

	// -------------------------------------------------------------------------

	/**
	 * Copies the whole contents of the file specified by sourceFile to the file
	 * specified by destFile.
	 * If destFile already exists, it will be overwritten without warning!
	 * The modification timestamp of destFile will be the current date.
	 *
	 * @param sourceFile The file to copy
	 * @param destFile The destination where to copy the file content
	 * @throws IOException
	 */
	public void copyFile(File sourceFile, File destFile) throws IOException
	{
		this.copyFile(sourceFile, destFile, false);
	} // copyFile() 

	// -------------------------------------------------------------------------

	/**
	 * Copies the whole contents of the file specified by sourceFilename to the
	 * file specified by destFilename.
	 * If destFilename already exists, it will be overwritten without warning!
	 * If preservetimestamp is true, the last modified time of the destFilename will
	 * be set to that of the sourceFilename. Otherwise it will be the current date.
	 *
	 * @param sourceFilename The file to copy
	 * @param destFilename The destinition where to copy the file content
	 * @param preserveTimestamp A flag that indicates if destFile must have the same timestamp as sourceFile
	 * @throws IOException
	 */
	public void copyFile(String sourceFilename, String destFilename, boolean preserveTimestamp) throws IOException
	{
		this.copyFile(new File(sourceFilename), new File(destFilename), preserveTimestamp);
	} // copyFile() 

	// -------------------------------------------------------------------------

	/**
	 * Copies the whole contents of the file specified by sourceFilename to the
	 * file specified by destFilename.
	 * If destFilename already exists, it will be overwritten without warning!
	 * The modification timestamp of destFilename will be the current date.
	 *
	 * @param sourceFilename The file to copy
	 * @param destFilename The destinition where to copy the file content
	 * @throws IOException
	 */
	public void copyFile(String sourceFilename, String destFilename) throws IOException
	{
		this.copyFile(sourceFilename, destFilename, false);
	} // copyFile() 

	// -------------------------------------------------------------------------

	/**
	 * Reads the whole content of the given input stream and returns
	 * it as a string.
	 * The stream will be closed after calling this method. Even if an exception
	 * occurred!
	 * The stream character encoding is the JVM default encoding.
	 *
	 * @param inStream The input stream to read.
	 * @return The text content of the given stream.
	 */
	public String readTextFrom(InputStream inStream) throws IOException
	{
		return this.readTextFrom(inStream, Charset.defaultCharset());
	} // readTextFrom() 

	// -------------------------------------------------------------------------

	/**
	 * Reads the whole content of the given input stream and returns
	 * it as a string.
	 * The stream will be closed after calling this method. Even if an exception
	 * occurred!
	 *
	 * @param inStream The input stream to read (must not be null).
	 * @param charsetName A charset name already checked to be supported (must not be null).
	 * @return The text content of the given stream
	 */
	public String readTextFrom(InputStream inStream, CheckedCharsetName charsetName) throws IOException
	{
		return this.readTextFrom(inStream, charsetName.getCharset());
	} // readTextFrom() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Reads the whole content of the given input stream and returns
	 * it as a string.
	 * The stream will be closed after calling this method. Even if an exception
	 * occurred!
	 *
	 * @param inStream The input stream to read (must not be null).
	 * @param charset A character set defining the encoding of the stream (must not be null).
	 * @return The text content of the given stream.
	 */
	public String readTextFrom(InputStream inStream, Charset charset) throws IOException
	{
		StringWriter writer;
		
		writer = new StringWriter(1024);
		this.copyText(inStream, charset, writer);
		return writer.toString();
	} // readTextFrom() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Reads the whole content of the file with the given name and returns
	 * it as a string.
	 * The file character encoding is the JVM default encoding.
	 *
	 * @param filename The name of the text containing file
	 */
	public String readTextFrom(String filename) throws IOException
	{
		return this.readTextFrom(filename, Charset.defaultCharset());
	} // readTextFrom() 

	// -------------------------------------------------------------------------

	/**
	 * Reads the whole content of the file with the given name and returns
	 * it as a string.
	 *
	 * @param filename The name of the text containing file (must not be null).
	 * @param charset A character set defining the file encoding (must not be null).
	 */
	public String readTextFrom(String filename, Charset charset) throws IOException
	{
		File file;
		
		file = new File(filename);
		return this.readTextFrom(file, charset);
	} // readTextFrom() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Reads the whole content of the file with the given name and returns
	 * it as a string.
	 *
	 * @param filename The name of the text containing file
	 * @param charsetName A charset name already checked to be supported (must not be null).
	 */
	public String readTextFrom(String filename, CheckedCharsetName charsetName) throws IOException
	{
		return this.readTextFrom(filename, charsetName.getCharset());
	} // readTextFrom() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Reads the whole content of the specified file and returns
	 * it as a string.
	 * The file character encoding is the JVM default encoding.
	 */
	public String readTextFrom(File file) throws IOException
	{
		return this.readTextFrom(file, Charset.defaultCharset());
	} // readTextFrom() 

	// -------------------------------------------------------------------------

	/**
	 * Reads the whole content of the specified file and returns
	 * it as a string.
	 * 
	 * @param file The text file from which to read (must not be null).
	 * @param charsetName A charset name already checked to be supported (must not be null).
	 */
	public String readTextFrom(File file, CheckedCharsetName charsetName) throws IOException
	{
		return this.readTextFrom(file, charsetName.getCharset());
	} // readTextFrom() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Reads the whole content of the specified file and returns
	 * it as a string.
	 * 
	 * @param file The text file from which to read (must not be null).
	 * @param charset A character set defining the file encoding (must not be null).
	 */
	public String readTextFrom(File file, Charset charset) throws IOException
	{
		FileInputStream inStream;
		
		inStream = new FileInputStream(file);
		return this.readTextFrom(inStream, charset);
	} // readTextFrom() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Reads the whole content of the specified file and returns
	 * it as a string.
	 * The file character encoding is the JVM default encoding.
	 */
	public String readTextFrom(FileLocator fileLocator) throws IOException
	{
		return this.readTextFrom(fileLocator.getInputStream());
	} // readTextFrom() 

	// -------------------------------------------------------------------------

	/**
	 * Reads the whole content of the specified file and returns
	 * it as a string.
	 * 
	 * @param fileLocator The locator of the text file to read (must not be null).
	 * @param charsetName A charset name already checked to be supported (must not be null).
	 */
	public String readTextFrom(FileLocator fileLocator, CheckedCharsetName charsetName) throws IOException
	{
		return this.readTextFrom(fileLocator, charsetName.getCharset());
	} // readTextFrom() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Reads the whole content of the specified file and returns
	 * it as a string.
	 * 
	 * @param fileLocator The locator of the text file to read (must not be null).
	 * @param charset A character set defining the encoding of the file (must not be null).
	 */
	public String readTextFrom(FileLocator fileLocator, Charset charset) throws IOException
	{
		return this.readTextFrom(fileLocator.getInputStream(), charset);
	} // readTextFrom() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Reads all text lines from the named file into a String array.
	 * The file character encoding is the JVM default encoding.
	 *
	 * @param filename The name of the file from which to read the text lines
	 */
	public String[] readTextLinesFrom(String filename) throws IOException
	{
		return this.readTextLinesFrom(filename, Charset.defaultCharset());
	} // readTextLinesFrom() 

	// -------------------------------------------------------------------------

	/**
	 * Reads all text lines from the named file into a String array.
	 *
	 * @param filename The name of the file from which to read the text lines
	 * @param charsetName A charset name already checked to be supported (must not be null).
	 */
	public String[] readTextLinesFrom(String filename, CheckedCharsetName charsetName) throws IOException
	{
		return this.readTextLinesFrom(filename, charsetName.getCharset());
	} // readTextLinesFrom() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Reads all text lines from the named file into a String array.
	 *
	 * @param filename The name of the file from which to read the text lines (must not be null).
	 * @param charset A character set defining the encoding of the file (must not be null).
	 */
	public String[] readTextLinesFrom(String filename, Charset charset) throws IOException
	{
		File file;
		
		file = new File(filename);
		return this.readTextLinesFrom(file, charset);
	} // readTextLinesFrom() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Reads all text lines from the specified file into a String array.
	 * The file character encoding is the JVM default encoding.
	 *
	 * @param file The file from which to read the text lines.
	 */
	public String[] readTextLinesFrom(File file) throws IOException
	{
		return this.readTextLinesFrom(file, Charset.defaultCharset());
	} // readTextLinesFrom() 

	// -------------------------------------------------------------------------

	/**
	 * Reads all text lines from the specified file into a String array.
	 *
	 * @param file The file from which to read the text lines
	 * @param charsetName A charset name already checked to be supported (must not be null).
	 */
	public String[] readTextLinesFrom(File file, CheckedCharsetName charsetName) throws IOException
	{
		return this.readTextLinesFrom(file, charsetName.getCharset());
	} // readTextLinesFrom() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Reads all text lines from the specified file into a String array.
	 *
	 * @param file The file from which to read the text lines (must not be null).
	 * @param charset A character set defining the encoding of the file (must not be null).
	 */
	public String[] readTextLinesFrom(File file, Charset charset) throws IOException
	{
		FileInputStream inStream;
		
		inStream = new FileInputStream(file);
		return this.readTextLinesFrom(inStream, charset);
	} // readTextLinesFrom() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Reads all text lines from the specified file location into a String array.
	 * The file character encoding is the JVM default encoding.
	 *
	 * @param fileLocator The file from which to read the text lines
	 */
	public String[] readTextLinesFrom(FileLocator fileLocator) throws IOException
	{
		return this.readTextLinesFrom(fileLocator.getInputStream());
	} // readTextLinesFrom() 

	// -------------------------------------------------------------------------

	/**
	 * Reads all text lines from the specified file location into a String array.
	 *
	 * @param fileLocator The file from which to read the text lines
	 * @param charsetName A charset name already checked to be supported (must not be null).
	 */
	public String[] readTextLinesFrom(FileLocator fileLocator, CheckedCharsetName charsetName) throws IOException
	{
		return this.readTextLinesFrom(fileLocator.getInputStream(), charsetName);
	} // readTextLinesFrom() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Reads all text lines from the specified stream into a String array.
	 * After that the stream will be closed. Even if an exception occurs.
	 * The character encoding is the JVM default encoding.
	 *
	 * @param inStream The stream from which to read the text lines
	 */
	public String[] readTextLinesFrom(InputStream inStream) throws IOException
	{
		return this.readTextLinesFrom(new InputStreamReader(inStream));
	} // readTextLinesFrom() 

	// ------------------------------------------------------------------------

	/**
	 * Reads all text lines from the specified stream into a String array.
	 * After that the stream will be closed. Even if an exception occurs.
	 *
	 * @param inStream The stream from which to read the text lines
	 * @param charsetName A charset name already checked to be supported (must not be null).
	 */
	public String[] readTextLinesFrom(InputStream inStream, CheckedCharsetName charsetName) throws IOException
	{
		this.assertArgumentNotNull("charsetName", charsetName);
		return this.readTextLinesFrom(inStream, charsetName.getCharset());
	} // readTextLinesFrom() 
	
	// ------------------------------------------------------------------------
	
	/**
	 * Reads all text lines from the specified stream into a String array.
	 * After that the stream will be closed. Even if an exception occurs.
	 *
	 * @param inStream The stream from which to read the text lines (must not be null).
	 * @param charset A character set defining the encoding of the stream (must not be null).
	 */
	public String[] readTextLinesFrom(InputStream inStream, Charset charset) throws IOException
	{
		InputStreamReader reader;
		
		this.assertArgumentNotNull("inStream", inStream);
		this.assertArgumentNotNull("charset", charset);
		try
		{
			reader = new InputStreamReader(inStream, charset);
			return this.readTextLinesFrom(reader);
		}
		finally
		{
			this.close(inStream);
		}
	} // readTextLinesFrom() 
	
	// ------------------------------------------------------------------------
	
	/**
	 * Reads all text lines from the specified reader into a String array.
	 * After that the reader will be closed. Even if an exception occurs.
	 *
	 * @param reader The reader which provides the text
	 */
	public String[] readTextLinesFrom(Reader reader) throws IOException
	{
		return this.basicReadTextLinesFrom(reader, null);
	} // readTextLinesFrom() 

	// ------------------------------------------------------------------------

	/**
	 * Reads all text lines from the named file into a String array.
	 * The character encoding is the JVM default encoding.
	 *
	 * @param filename The name of the file from which to read the text lines
	 * @param filter A filter that defines which lines to return (may be null -> all lines)
	 */
	public String[] readTextLinesFrom(String filename, StringFilter filter) throws IOException
	{
		return this.readTextLinesFrom(filename, Charset.defaultCharset(), filter);
	} // readTextLinesFrom() 

	// -------------------------------------------------------------------------

	/**
	 * Reads all text lines from the named file into a String array.
	 *
	 * @param filename The name of the file from which to read the text lines
	 * @param charsetName A charset name already checked to be supported (must not be null).
	 * @param filter A filter that defines which lines to return (may be null -> all lines)
	 */
	public String[] readTextLinesFrom(String filename, CheckedCharsetName charsetName, StringFilter filter) throws IOException
	{
		return this.readTextLinesFrom(filename, charsetName.getCharset(), filter);
	} // readTextLinesFrom() 
	
	// -------------------------------------------------------------------------

	/**
	 * Reads all text lines from the named file into a String array.
	 *
	 * @param filename The name of the file from which to read the text lines
	 * @param charset A character set defining the file encoding (must not be null).
	 * @param filter A filter that defines which lines to return (may be null -> all lines)
	 */
	public String[] readTextLinesFrom(String filename, Charset charset, StringFilter filter) throws IOException
	{
		File file;
		
		file = new File(filename);
		return this.readTextLinesFrom(file, charset, filter);
	} // readTextLinesFrom() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Reads all text lines matching the filter from the specified file into a
	 * String array.
	 * The character encoding is the JVM default encoding.
	 *
	 * @param file The file from which to read the text lines
	 * @param filter A filter that defines which lines to return (may be null -> all lines)
	 */
	public String[] readTextLinesFrom(File file, StringFilter filter) throws IOException
	{
		return this.readTextLinesFrom(file, Charset.defaultCharset(), filter);
	} // readTextLinesFrom() 

	// -------------------------------------------------------------------------

	/**
	 * Reads all text lines matching the filter from the specified file into a
	 * String array.
	 *
	 * @param file The file from which to read the text lines
	 * @param charsetName A charset name already checked to be supported (must not be null).
	 * @param filter A filter that defines which lines to return (may be null -> all lines)
	 */
	public String[] readTextLinesFrom(File file, CheckedCharsetName charsetName, StringFilter filter) throws IOException
	{
		return this.readTextLinesFrom(file, charsetName.getCharset(), filter);
	} // readTextLinesFrom() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Reads all text lines matching the filter from the specified file into a
	 * String array.
	 *
	 * @param file The file from which to read the text lines (must not be null).
	 * @param charset A character set defining the encoding of the file (must not be null).
	 * @param filter A filter that defines which lines to return (may be null -> all lines)
	 */
	public String[] readTextLinesFrom(File file, Charset charset, StringFilter filter) throws IOException
	{
		FileInputStream inStream;
		
		inStream = new FileInputStream(file);
		return this.readTextLinesFrom(inStream, charset, filter);
	} // readTextLinesFrom() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Reads all text lines matching the filter from the specified file location
	 * into a String array.
	 * The character encoding is the JVM default encoding.
	 *
	 * @param fileLocator The file from which to read the text lines
	 * @param filter A filter that defines which lines to return (may be null -> all lines)
	 */
	public String[] readTextLinesFrom(FileLocator fileLocator, StringFilter filter) throws IOException
	{
		return this.readTextLinesFrom(fileLocator, Charset.defaultCharset(), filter);
	} // readTextLinesFrom() 

	// -------------------------------------------------------------------------

	/**
	 * Reads all text lines matching the filter from the specified file location
	 * into a String array.
	 *
	 * @param fileLocator The file from which to read the text lines
	 * @param charsetName A charset name already checked to be supported (must not be null).
	 * @param filter A filter that defines which lines to return (may be null -> all lines)
	 */
	public String[] readTextLinesFrom(FileLocator fileLocator, CheckedCharsetName charsetName, StringFilter filter) throws IOException
	{
		return this.readTextLinesFrom(fileLocator, charsetName.getCharset(), filter);
	} // readTextLinesFrom() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Reads all text lines matching the filter from the specified file location
	 * into a String array.
	 *
	 * @param fileLocator The file from which to read the text lines  (must not be null).
	 * @param charset A character set defining the file encoding (must not be null).
	 * @param filter A filter that defines which lines to return (may be null -> all lines)
	 */
	public String[] readTextLinesFrom(FileLocator fileLocator, Charset charset, StringFilter filter) throws IOException
	{
		return this.readTextLinesFrom(fileLocator.getInputStream(), charset, filter);
	} // readTextLinesFrom() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Reads all text lines matching the filter from the specified stream into a
	 * String array.
	 * After that the stream will be closed. Even if an exception occurs.
	 * The character encoding is the JVM default encoding.
	 *
	 * @param inStream The stream from which to read the text lines
	 * @param filter A filter that defines which lines to return (may be null -> all lines)
	 */
	public String[] readTextLinesFrom(InputStream inStream, StringFilter filter) throws IOException
	{
		return this.readTextLinesFrom(inStream, Charset.defaultCharset(), filter);
	} // readTextLinesFrom() 

	// ------------------------------------------------------------------------

	/**
	 * Reads all text lines matching the filter from the specified stream into a
	 * String array using the specified character encoding.
	 * After that the stream will be closed. Even if an exception occurs.
	 *
	 * @param inStream The stream from which to read the text lines.
	 * @param charsetName A charset name already checked to be supported (must not be null).
	 * @param filter A filter that defines which lines to return (may be null -> all lines).
	 */
	public String[] readTextLinesFrom(InputStream inStream, CheckedCharsetName charsetName, StringFilter filter) throws IOException
	{
		this.assertArgumentNotNull("inStream", inStream);
		this.assertArgumentNotNull("charsetName", charsetName);
		return this.readTextLinesFrom(inStream, charsetName.getCharset(), filter);
	} // readTextLinesFrom() 
	
	// ------------------------------------------------------------------------
	
	/**
	 * Reads all text lines matching the filter from the specified stream into a
	 * String array using the specified character encoding.
	 * After that the stream will be closed. Even if an exception occurs.
	 *
	 * @param inStream The stream from which to read the text lines (must not be null).
	 * @param charset A character set defining the encoding of the input stream (must not be null).
	 * @param filter A filter that defines which lines to return (may be null -> all lines).
	 */
	public String[] readTextLinesFrom(InputStream inStream, Charset charset, StringFilter filter) throws IOException
	{
		InputStreamReader reader;
		
		this.assertArgumentNotNull("inStream", inStream);
		this.assertArgumentNotNull("charset", charset);

		try
		{
			reader = new InputStreamReader(inStream, charset);
			return this.readTextLinesFrom(reader, filter);
		}
		finally
		{
			this.close(inStream);
		}
	} // readTextLinesFrom() 
	
	// ------------------------------------------------------------------------
	
	/**
	 * Reads all text lines that match the filter from the specified reader into
	 * a String array. If the filter is null all lines will be returned.
	 * After that the reader will be closed. Even if an exception occurs.
	 *
	 * @param reader The reader which provides the text
	 * @param filter A filter that defines which lines to return (may be null -> all lines)
	 */
	public String[] readTextLinesFrom(Reader reader, StringFilter filter) throws IOException
	{
		return this.basicReadTextLinesFrom(reader, filter);
	} // readTextLinesFrom() 

	// ------------------------------------------------------------------------

	/**
	 * Copies all text lines from the specified reader to the given writer.
	 * After that the reader will be closed. Even if an exception occurs.
	 *
	 * @param reader The reader which provides the text to copy
	 * @param writer The writer to which the text will be copied
	 */
	public void copyText(Reader reader, final StringWriter writer) throws IOException
	{
		BufferedReader bufReader;
		LineProcessor processor;

		bufReader = new BufferedReader(reader);
		try
		{
			processor = new LineProcessor()
			{
				public boolean processLine(String line, int lineNo)
				{
					if (lineNo > 1)
					{
						writer.write(LINE_SEPARATOR);
					}

					writer.write(line);
					return true;
				}
			};
			this.processTextLines(bufReader, processor);
		}
		finally
		{
			bufReader.close();
		}
	} // copyText() 

	// ------------------------------------------------------------------------

	/**
	 * Reads all text lines from the file with the specified name and passes them
	 * one by one to the given line processor.
	 * The processing will be terminated, if the end of the text is reached or
	 * if the processor returns <b>false</b>.<br>
	 *
	 * @param filename The name of the text file to read
	 * @param processor The processor that receives the lines from the text
	 */
	public void processTextLines(String filename, LineProcessor processor) throws IOException
	{
		FileInputStream inStream;

		this.assertArgumentNotNull("filename", filename);
		inStream = new FileInputStream(filename);
		this.processTextLines(inStream, processor);
	} // processTextLines() 

	// -------------------------------------------------------------------------

	/**
	 * Reads all text lines from the file with the specified name and passes them
	 * one by one to the given line processor.
	 * The processing will be terminated, if the end of the text is reached or
	 * if the processor returns <b>false</b>.<br>
	 *
	 * @param filename The name of the text file to read
	 * @param charsetName A charset name already checked to be supported (must not be null).
	 * @param processor The processor that receives the lines from the text
	 */
	public void processTextLines(String filename, CheckedCharsetName charsetName, LineProcessor processor) throws IOException
	{
		FileInputStream inStream;
		
		this.assertArgumentNotNull("filename", filename);
		inStream = new FileInputStream(filename);
		this.processTextLines(inStream, charsetName, processor);
	} // processTextLines() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Reads all text lines from the specified input stream and passes them
	 * one by one to the given line processor.
	 * The processing will be terminated, if the end of the text is reached or
	 * if the processor returns <b>false</b>.<br>
	 * The given input stream will be closed after the execution of this method.
	 * Even if an exception occurred.
	 * The character encoding is the JVM default encoding.
	 *
	 * @param inStream The input stream that contains the text
	 * @param processor The processor that receives the lines from the text
	 */
	public void processTextLines(InputStream inStream, LineProcessor processor) throws IOException
	{
		this.processTextLines(inStream, Charset.defaultCharset(), processor);
	} // processTextLines() 

	// -------------------------------------------------------------------------

	/**
	 * Reads all text lines from the specified input stream and passes them
	 * one by one to the given line processor.
	 * The processing will be terminated, if the end of the text is reached or
	 * if the processor returns <b>false</b>.<br>
	 * The given input stream will be closed after the execution of this method.
	 * Even if an exception occurred.
	 *
	 * @param inStream The input stream that contains the text (must not be null).
	 * @param charsetName A charset name already checked to be supported (must not be null).
	 * @param processor The processor that receives the lines from the text (must not be null).
	 */
	public void processTextLines(InputStream inStream, CheckedCharsetName charsetName, LineProcessor processor) throws IOException
	{
		this.processTextLines(inStream, charsetName.getCharset(), processor);
	} // processTextLines() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Reads all text lines from the specified input stream and passes them
	 * one by one to the given line processor.
	 * The processing will be terminated, if the end of the text is reached or
	 * if the processor returns <b>false</b>.<br>
	 * The given input stream will be closed after the execution of this method.
	 * Even if an exception occurred.
	 *
	 * @param inStream The input stream that contains the text (must not be null).
	 * @param charset A character set defining the encoding of the stream (must not be null).
	 * @param processor The processor that receives the lines from the text (must not be null).
	 */
	public void processTextLines(InputStream inStream, Charset charset, LineProcessor processor) throws IOException
	{
		InputStreamReader reader;
		
		this.assertArgumentNotNull("inStream", inStream);
		this.assertArgumentNotNull("charset", charset);
		this.assertArgumentNotNull("processor", processor);
		try
		{
			reader = new InputStreamReader(inStream, charset);
			this.processTextLines(reader, processor);			
		}
		finally
		{
			this.close(inStream);
		}
	} // processTextLines() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Reads all text lines from the specified reader and passes them one by one
	 * to the given line processor.
	 * The processing will be terminated, if the end of the text is reached or
	 * if the processor returns <b>false</b>.
	 *
	 * @param reader The reader that contains a text stream
	 * @param processor The processor that receives the lines from the text
	 */
	public void processTextLines(Reader reader, LineProcessor processor) throws IOException
	{
		BufferedReader bufReader;
		String line;
		int counter = 0;
		boolean continue_reading = true;

		this.assertArgumentNotNull("reader", reader);
		this.assertArgumentNotNull("processor", processor);

		bufReader = new BufferedReader(reader);
		try
		{
			while (continue_reading && bufReader.ready())
			{
				line = bufReader.readLine();
				if (line == null)
					break;

				counter++;
				continue_reading = processor.processLine(line, counter);
			}
		}
		finally
		{
			this.close(bufReader);
		}
	} // processTextLines() 

	// ------------------------------------------------------------------------

	/**
	 * Close the given stream ignoring any exception.
	 * Returns true, if the stream was closed successfully, false otherwise
	 */
	public boolean close(InputStream stream)
	{
		if (stream == null)
		{
			return false;
		}
		try
		{
			stream.close();
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
	} // close() 

	// -------------------------------------------------------------------------

	/**
	 * Close the given stream ignoring any exception.
	 * Returns true, if the stream was closed successfully, false otherwise
	 */
	public boolean close(OutputStream stream)
	{
		if (stream == null)
		{
			return false;
		}
		try
		{
			stream.close();
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
	} // close() 

	// -------------------------------------------------------------------------

	/**
	 * Close the given reader ignoring any exception.
	 * Returns true, if the reader was closed successfully, false otherwise
	 */
	public boolean close(Reader reader)
	{
		if (reader == null)
		{
			return false;
		}
		try
		{
			reader.close();
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
	} // close() 

	// -------------------------------------------------------------------------

	/**
	 * Close the given writer ignoring any exception.
	 * Returns true, if the writer was closed successfully, false otherwise
	 */
	public boolean close(Writer writer)
	{
		if (writer == null)
		{
			return false;
		}
		try
		{
			writer.close();
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
	} // close() 

	// -------------------------------------------------------------------------

	/**
	 * Close the given ZIP file ignoring any exception.
	 * Returns true, if the ZIP file was closed successfully, false otherwise
	 */
	public boolean close(ZipFile zipFile)
	{
		if (zipFile == null)
		{
			return false;
		}
		try
		{
			zipFile.close();
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
	} // close() 

	// -------------------------------------------------------------------------

	/**
	 * Close the given closeable ignoring any exception and checking for null before closing.
	 * Returns true, if the object was closed successfully, false otherwise.
	 */
	public boolean close(Closeable closeable)
	{
		if (closeable == null)
		{
			return false;
		}
		try
		{
			closeable.close();
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
	} // close() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Convert the filename to a canonical (see java.io.File.getCanonicalPath())
	 * format and replace any backslashes '\' by slashes ('/').
	 * If possible all "." and ".." elements in the path are eliminated.
	 *
	 * @param filename The filename which has to be standardized
	 * @return An absolute filename that uses slashes to separate its elements
	 */
	public String standardize(String filename)
	{
		if (filename == null)
		{
			return null;
		}

		if (this.isRemote(filename))
		{
			return filename;
		}

		return this.standardizeFilename(filename);
	} // standardize() 

	// -------------------------------------------------------------------------

	/**
	 * Convert all the given filenames to a canonical (see java.io.File.getCanonicalPath())
	 * format and replace any backslashes '\' by slashes ('/').
	 * If possible all "." and ".." elements in the path are eliminated.
	 *
	 * @param filenames The file names which have to be standardized
	 * @return An new array containing absolute filenames that uses slashes to separate its elements
	 */
	public String[] standardize(String[] filenames)
	{
		String[] names;

		if (filenames == null)
			return null;

		names = new String[filenames.length];
		for (int i = 0; i < filenames.length; i++)
		{
			names[i] = this.standardize(filenames[i]);
		}
		return names;
	} // standardize() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given filename in the platform independent way that Java
	 * understands. That is all elements are separated by a forward slash rather
	 * than back slashes.
	 *
	 * @param filename The name to be modified
	 */
	public String javaFilename(String filename)
	{
		if (filename == null)
		{
			return null;
		}

		return filename.replace('\\', '/');
	} // javaFilename() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a new array of filenames where all backslashes are
	 * replaced by forward slashes.
	 *
	 * @param filenames The file names to be modified
	 * @return The same file names but with forward slashes only
	 */
	public String[] javaFilenames(String[] filenames)
	{
		String[] newNames;

		if (filenames == null)
		{
			return null;
		}

		newNames = new String[filenames.length];
		for (int i = 0; i < filenames.length; i++)
		{
			newNames[i] = this.javaFilename(filenames[i]);
		}
		return newNames;
	} // javaFilenames() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the current classpath as String.
	 * If this is called from within an Eclipse plug-in the classpath is
	 * created from the plug-in dependencies.
	 * Otherwise the classpath is taken from the system property "java.class.path"
	 * or the manifest file if the application was started with option -jar.
	 */
	public String getClasspath()
	{
		Classpath classpath;

		if (classpathString == null)
		{
			classpath = this.calculateClasspath();
			if (classpath != null)
			{
				classpathString = classpath.toString();
			}
		}
		return classpathString;
	} // getClasspath() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the current classpath.
	 * If this is called from within an Eclipse plug-in the classpath is
	 * created from the plug-in dependencies.
	 * Otherwise the classpath is taken from the system property "java.class.path"
	 * or the manifest file if the application was started with option -jar.
	 *
	 * @see #getClasspath()
	 */
	public Classpath calculateClasspath()
	{
		if (calculatedClasspath == null)
		{
			if (SysUtil.current().isEclipse())
			{
				calculatedClasspath = this.calculateEclipseClasspath();
			}
			if (calculatedClasspath == null)
			{
				calculatedClasspath = new Classpath(this.getClasspathFromSystemProperty());
			}
		}
		return calculatedClasspath;
	} // calculateClasspath() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a class path that is either the value of the specified system
	 * property or the classpath calculated from the current Java environment.
	 * <p>
	 * If the given system property has a lookup path set it will be scanned
	 * for ${xxx} placeholders which will be replaced by values of the corresponding
	 * system properties.
	 *
	 * @param sysPropName A system property name of null
	 */
	public Classpath getLookupPath(String sysPropName)
	{
		String propValue;

		if (!this.str().isNullOrEmpty(sysPropName))
		{
			propValue = System.getProperty(sysPropName);
			if (!this.str().isNullOrEmpty(propValue))
			{
				return this.determineLookupClasspath(sysPropName);
			}
		}
		if (SysUtil.current().isEclipse())
		{
			return this.calculateClasspath();
		}
		return this.determineLookupClasspath(sysPropName);
	} // getLookupPath() 

	// -------------------------------------------------------------------------

	/**
	 * Copies the contents of the given inStream to a temporary file and returns
	 * this temporary file. When the VM terminates the temporary file will
	 * automatically be deleted if it still exists.
	 * The name of the temporary file starts with the default prefix "PFFU"
	 * and ends with the default suffix ".tmp".
	 *
	 * @param inStream The stream to copy into the temporary file
	 * @see FileUtil#copyToTempFile(InputStream, String)
	 * @see FileUtil#copyToTempFile(InputStream, String, String)
	 * @see FileUtil#copyToTempFile(InputStream, String, String, boolean)
	 */
	public File copyToTempFile(InputStream inStream) throws IOException
	{
		return this.copyToTempFile(inStream, DEFAULT_TEMP_FILE_PREFIX);
	} // copyToTempFile() 

	// -------------------------------------------------------------------------

	/**
	 * Copies the contents of the given inStream to a temporary file and returns
	 * this temporary file. When the VM terminates the temporary file will
	 * automatically be deleted if it still exists.
	 * The name of the temporary file starts with the given prefix and ends with
	 * the default ".tmp".
	 *
	 * @param inStream The stream to copy into the temporary file
	 * @param filePrefix The prefix to be used for the temp file name (must not be null)
	 * @see FileUtil#copyToTempFile(InputStream)
	 * @see FileUtil#copyToTempFile(InputStream, String, String)
	 * @see FileUtil#copyToTempFile(InputStream, String, String, boolean)
	 */
	public File copyToTempFile(InputStream inStream, String filePrefix) throws IOException
	{
		return this.copyToTempFile(inStream, filePrefix, null);
	} // copyToTempFile() 

	// -------------------------------------------------------------------------

	/**
	 * Copies the contents of the given inStream to a temporary file and returns
	 * this temporary file. When the VM terminates the temporary file will
	 * automatically be deleted if it still exists.
	 * The name of the temporary file starts with the given prefix and ends with
	 * the given suffix.
	 *
	 * @param inStream The stream to copy into the temporary file
	 * @param filePrefix The prefix to be used for the temp file name (must not be null)
	 * @param fileSuffix The prefix to be used for the temp file name (may be null)
	 * @see File#createTempFile(java.lang.String, java.lang.String)
	 * @see FileUtil#copyToTempFile(InputStream)
	 * @see FileUtil#copyToTempFile(InputStream, String)
	 * @see FileUtil#copyToTempFile(InputStream, String, String, boolean)
	 */
	public File copyToTempFile(InputStream inStream, String filePrefix, String fileSuffix) throws IOException
	{
		return this.copyToTempFile(inStream, filePrefix, fileSuffix, true);
	} // copyToTempFile() 

	// -------------------------------------------------------------------------

	/**
	 * Copies the contents of the given inStream to a temporary file and returns
	 * this temporary file.
	 * The name of the temporary file starts with the given prefix and ends with
	 * the given suffix.
	 *
	 * @param inStream The stream to copy into the temporary file
	 * @param filePrefix The prefix to be used for the temp file name (must not be null)
	 * @param fileSuffix The prefix to be used for the temp file name (may be null)
	 * @param deleteOnExit If true the temporary file will be deleted when the VM terminates
	 * @see File#createTempFile(java.lang.String, java.lang.String)
	 * @see FileUtil#copyToTempFile(InputStream)
	 * @see FileUtil#copyToTempFile(InputStream, String)
	 * @see FileUtil#copyToTempFile(InputStream, String, String)
	 */
	public File copyToTempFile(InputStream inStream, String filePrefix, String fileSuffix, boolean deleteOnExit)
		throws IOException
	{
		File tempFile;
		OutputStream outStream;

		tempFile = File.createTempFile(filePrefix, fileSuffix);
		outStream = new FileOutputStream(tempFile);
		this.copyStream(inStream, outStream);
		if (deleteOnExit)
		{
			tempFile.deleteOnExit();
		}
		return tempFile;
	} // copyToTempFile() 

	// -------------------------------------------------------------------------

	/**
	 * Copies the contents of the file specified by filename to a temporary file
	 * and returns this temporary file.
	 * The name of the temporary file starts with the given prefix and ends with
	 * the given suffix.
	 *
	 * @param filename The name of the file which to copy into the temporary file
	 * @param filePrefix The prefix to be used for the temp file name (must not be null)
	 * @param fileSuffix The prefix to be used for the temp file name (may be null)
	 * @param deleteOnExit If true the temporary file will be deleted when the VM terminates
	 * @return The newly created temporary file or null if the input file (filename) cannot be found
	 * @see File#createTempFile(java.lang.String, java.lang.String)
	 * @throws IOException
	 * @throws IllegalArgumentException If any input parameter is not ok
	 */
	public File copyToTempFile(String filename, String filePrefix, String fileSuffix, boolean deleteOnExit)
		throws IOException
	{
		InputStream inStream;
		URL url;

		if (filename == null)
			throw new IllegalArgumentException("<filename> must not be null!");
		if (filePrefix == null)
			throw new IllegalArgumentException("<filePrefix> must not be null!");

		if (this.isLocal(filename))
		{
			inStream = new FileInputStream(filename);
		}
		else
		{
			url = new URL(filename);
			inStream = url.openStream();
		}

		if (inStream == null)
		{
			return null;
		}
		return this.copyToTempFile(inStream, filePrefix, fileSuffix, deleteOnExit);
	} // copyToTempFile() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the given filename specifies a file (or directory) on
	 * the local file system using the URL syntax such as "file:/usr/bin/run"
	 * or "file:\C:\temp\readme.txt.
	 */
	public boolean isLocalFileURL(String filename)
	{
		if (filename == null)
			return false;

		return LOCAL_PATTERN.matches(filename);
	} // isLocalFileURL() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the given filename specifies a file (or directory) on
	 * the local file system. That is, it doesn't contain a protocol prefix
	 * such as "http:" or "ftp:".
	 */
	public boolean isLocal(String filename)
	{
		if (filename == null)
		{
			return false;
		}
		if (this.isLocalFileURL(filename))
		{
			return true;
		}
		return !this.isRemote(filename);
	} // isLocal() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the given filename specifies a file (or directory) on
	 * a remote system. That is, it contains a protocol prefix such as "http:"
	 * or "ftp:".
	 */
	public boolean isRemote(String filename)
	{
		if (filename == null)
		{
			return false;
		}
		return REMOTE_PATTERN.matches(filename);
	} // isRemote() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the given filename specifies a file (or directory) that
	 * is located inside a JAR file.
	 */
	public boolean isInsideJavaArchive(String filename)
	{
		if (filename == null)
		{
			return false;
		}
		return JAR_PATTERN.matches(filename);
	} // isInsideJavaArchive() 

	// -------------------------------------------------------------------------

	/**
	 * Converts the given filename to URL syntax. That is, it prepends the
	 * scheme "file:" and replaces all illegal URL characters by %xx values.
	 * Be aware that this method uses %20 for replacing spaces and not the
	 * plus ('+') as the URLEncoder class would do.
	 * <p>
	 * Examples:<br>
	 *  "/path/name" -> "file:/path/name"<br>
	 *  "C:\path\name" -> "file:/C:/path/name"<br>
	 *  "path/name" -> "file:path/name"
	 *  "C:\\folder name\\filename{v1}" -> "file:/C:/folder%20name/filename%7bv1%7d"<br>
	 *
	 * @param filename The filename to convert
	 */
	public String convertToURLSyntax(String filename)
	{
		StringBuffer buffer;
		String[] parts;
		String part;

		if ((filename == null) || this.isRemote(filename) || this.isLocalFileURL(filename))
		{
			return filename;
		}
		buffer = new StringBuffer(filename.length() + FILE_PROTOCOL_INDICATOR.length() + 3);
		buffer.append(FILE_PROTOCOL_INDICATOR);
		parts = this.str().allParts(filename, "/\\");
		for (int i = 0; i < parts.length; i++)
		{
			if (i == 0)
			{
				if (DRIVE_LETTER_PATTERN_2.matches(filename))
				{
					buffer.append("/");
					part = parts[i];
				}
				else
				{
					if ((filename.startsWith("/")) || (filename.startsWith("\\")))
					{
						buffer.append("/");
					}
					part = this.urlEncode(parts[i]);
				}
			}
			else
			{
				buffer.append("/");
				part = this.urlEncode(parts[i]);
			}
			buffer.append(part);
		}
		return buffer.toString();
	} // convertToURLSyntax() 

	// -------------------------------------------------------------------------

	/**
	 * Converts the given filename from URL syntax to normal file system syntax.
	 * It removes the scheme and decodes all escaped characters.
	 * <p>
	 * Examples:<br>
	 *  "file:/path/name" -> "/path/name"<br>
	 *  "file:\C:\path\name" -> "C:\path\name"<br>
	 *  "path/name" -> "path/name"
	 *  "file:/C:/Program+Files/settings.ini" -> "C:\Program Files\settings.ini"<br>
	 *
	 *   @param filename The filename to convert
	 */
	public String convertFromURLSyntax(String filename)
	{
		String newStr;

		if (filename == null)
		{
			return null;
		}
		if (LOCAL_PATTERN.matches(filename))
		{
			newStr = filename.substring(FILE_PROTOCOL_INDICATOR.length());
			newStr = this.urlDecode(newStr);
			if (WINDOWS_DRIVE_PATTERN_1.matches(newStr) || WINDOWS_DRIVE_PATTERN_2.matches(newStr))
			{
				newStr = newStr.substring(1); // Remove leading backslash
			}
			return newStr;
		}
		return filename;
	} // convertFromURLSyntax() 

	// -------------------------------------------------------------------------

	/**
	 * Converts the given file from URL syntax to normal file system syntax.
	 * <p>
	 * Examples:<br>
	 *  "file:/path/name" -> "/path/name"<br>
	 *  "file:\C:\path\name" -> "C:\path\name"<br>
	 *  "path/name" -> "path/name"
	 *
	 *   @param file The file to convert
	 */
	public File convertFromURLSyntax(File file)
	{
		return new File(this.convertFromURLSyntax(file.getPath()));
	} // convertFromURLSyntax() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given string with all %xx placeholders decoded using the
	 * specified charset encoding.
	 *
	 * @param str The string to decode.
	 * @param charsetName The name of the charset to be used for decoding. 
	 * @return A string with all %xx placeholders decoded according to the specified charset.
	 */
	public String urlDecode(String str, CheckedCharsetName charsetName)
	{
		if (str == null)
		{
			return null;
		}
		try
		{
			return URLDecoder.decode(str, charsetName.toString());
		}
		catch (UnsupportedEncodingException e)
		{
			// Should never happen because it is always a supported charset (i.e. CheckedCharsetName).
			e.printStackTrace();
		}
		return str;
	} // urlDecode() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the given string with all %xx placeholders decoded.
	 *
	 * @param str The string to decode
	 * @return A string with all %xx placeholders decoded (using UTF-8)
	 */
	public String urlDecode(String str)
	{
		return urlDecode(str, CheckedCharsetName.UTF_8);
	} // urlDecode() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given string with all characters that are not allowed directly
	 * in URLs encoded as %xx placeholders. In particular it replaces spaces by "
	 * %20" rather than '+'! That is reasonable in cases where files in the
	 * local file system contain spaces. Such URLs (staring with "file:") cannot
	 * open a stream if the URL contains a '+' because the '+' would not be
	 * decoded back to a space. That is a bug (or at least an inconsistency) in
	 * the URL class implementation.
	 *
	 * @param str The string to encode.
	 * @param charsetName The name of the charset to be used for encoding. 
	 * @return A string with %xx encodings for all invalid URL characters.
	 */
	public String urlEncode(String str, CheckedCharsetName charsetName)
	{
		String newStr;

		if (str == null)
		{
			return null;
		}
		try
		{
			newStr = URLEncoder.encode(str, charsetName.toString());
			return this.str().replaceAll(newStr, "+", "%20");
		}
		catch (UnsupportedEncodingException e)
		{
			// Cannot happen because a CheckedCharsetName was used.
			e.printStackTrace();
		}
		return str;
	} // urlEncode() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given string with all characters that are not allowed directly
	 * in URLs encoded as %xx placeholders. In particular it replaces spaces by "
	 * %20" rather than '+'! That is reasonable in cases where files in the
	 * local file system contain spaces. Such URLs (staring with "file:") cannot
	 * open a stream if the URL contains a '+' because the '+' would not be
	 * decoded back to a space. That is a bug (or at least an inconsistency) in
	 * the URL class implementation.
	 *
	 * @param str The string to encode.
	 * @return A string with %xx (UTF-8) encodings for all invalid URL characters.
	 */
	public String urlEncode(String str)
	{
		return this.urlEncode(str, CheckedCharsetName.UTF_8);
	} // urlEncode() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Deletes all files and sub-directories in the given directory and the directory itself.
	 * !!! BE CAREFUL WITH THIS METHOD!!!
	 * <br/>
	 * If called with the wrong directory name it can cause data loss.
	 * 
	 * @param dir The directory under which all files must be deleted (must not be null).
	 * @return The number of deleted artifacts.
	 */
	public long removeDirectory(final File dir) 
	{
	  long counter;
	  
    if (!dir.exists() || !dir.isDirectory())
    {
      return 0;
    }
	  counter = this.cleanDirectory(dir);
	  if (dir.delete())
    {
      counter++;
    }
	  return counter;
	} // removeDirectory() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Deletes all files and sub-directories in the given directory.
	 * !!! BE CAREFUL WITH THIS METHOD!!!
	 * <br/>
	 * If called with the wrong directory name it can cause data loss.
	 * 
	 * @param dir The directory under which all files must be deleted (must not be null).
	 * @return The number of deleted artifacts.
	 */
	public long cleanDirectory(final File dir) 
	{
    if (!dir.exists() || !dir.isDirectory())
    {
      return 0;
    }
	  return this.cleanDirectory(dir, false);
	} // cleanDirectory() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Deletes all files (and optionally sub-directories) in the given directory.
	 * !!! BE CAREFUL WITH THIS METHOD!!!
	 * <br/>
	 * If called with the wrong directory name it can cause data loss.
	 * 
	 * @param dir The directory under which all files must be deleted (must not be null).
	 * @param filesOnly If true only files will be deleted, if false sub-directories will be deleted as well.
	 * @return The number of deleted artifacts.
	 */
	public long cleanDirectory(final File dir, final boolean filesOnly) 
  {
    FileWalker fileWalker;
    AFileProcessor fileProcessor;
    long deleteCounter;
    File startDir;
    
    startDir = new File(this.standardize(dir.getAbsolutePath()));
    if (!startDir.exists() || !startDir.isDirectory())
    {
      return 0;
    }
    fileProcessor = new FileDeleteProcessor();
    fileWalker = new FileWalker(fileProcessor);
    // Delete all files
    deleteCounter = fileWalker.walkThrough(startDir.getAbsolutePath(), DefaultFilenameFilter.ALL, true);
    if (filesOnly)
    {
      return deleteCounter;
    }
    // Delete all sub-directories
    deleteCounter += fileWalker.walkThroughDirectoriesSubDirsFirst(dir.getAbsolutePath(), DefaultFilenameFilter.ALL);
    return deleteCounter;    
  } // cleanDirectory() 
  
  // -------------------------------------------------------------------------
	
	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	protected void copyText(InputStream inStream, StringWriter writer) throws IOException
	{
		this.copyText(new InputStreamReader(inStream), writer);
	} // copyText() 

	// ------------------------------------------------------------------------

	protected void copyText(InputStream inStream, CheckedCharsetName charsetName, StringWriter writer) throws IOException
	{		
		this.assertArgumentNotNull("inStream", inStream);
		this.assertArgumentNotNull("charsetName", charsetName);
		this.copyText(inStream, charsetName.getCharset(), writer);
	} // copyText() 
	
	// ------------------------------------------------------------------------
	
	protected void copyText(InputStream inStream, Charset charset, StringWriter writer) throws IOException
	{
		InputStreamReader reader = null;
		
		this.assertArgumentNotNull("inStream", inStream);
		this.assertArgumentNotNull("charset", charset);
		
		try
		{
			reader = new InputStreamReader(inStream, charset);
			this.copyText(reader, writer);			
		}
		finally
		{
			this.close(reader);
			this.close(inStream);
		}
	} // copyText() 
	
	// ------------------------------------------------------------------------
	
	protected String standardizeFilename(String filename)
	{
		String[] nameElements;
		boolean hasDriveLetter;
		boolean startedFromRoot;
		boolean isAbsolute;
		int index;

		filename = this.javaFilename(filename);
		startedFromRoot = filename.startsWith("/");
		nameElements = this.str().parts(filename, "/");
		if (nameElements.length > 0)
		{
			hasDriveLetter = false;
			for (int i = 0; i < nameElements.length; i++)
			{
				if (DRIVE_LETTER_PATTERN_1.matches(nameElements[0]))
				{
					nameElements[0] = nameElements[0].toUpperCase();
					hasDriveLetter = true;
				}
			}
			if (!hasDriveLetter)
			{
				if (startedFromRoot)
				{
					nameElements = this.str().append(new String[] { "" }, nameElements);
				}
			}
			isAbsolute = hasDriveLetter || startedFromRoot;
			for (int i = 0; i < nameElements.length; i++)
			{
				if (".".equals(nameElements[i]))
				{
					nameElements[i] = null;
				}
				else
				{
					if ("..".equals(nameElements[i]))
					{
						index = this.indexOfPreceedingNotNullElement(nameElements, i - 1);
						if (index >= 0)
						{
							if ((index > 0) || (!isAbsolute))
							{
								nameElements[i] = null;
								nameElements[index] = null;
							}
						}
					}
				}
			}
			nameElements = this.str().removeNull(nameElements);
			return this.str().asString(nameElements, "/");
		}
		else
		{
			return "";
		}
	} // standardizeFilename() 

	// -------------------------------------------------------------------------

	protected int indexOfPreceedingNotNullElement(String[] elements, int start)
	{
		for (int i = start; i >= 0; i--)
		{
			if (elements[i] != null)
			{
				if ("..".equals(elements[i])) // This is not a valid not null element
				{
					return -1;
				}
				else
				{
					return i;
				}
			}
		}
		return -1;
	} // indexOfPreceedingNotNullElement() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the classpath definition from the manifest file in the specified
	 * JAR or null if not found.
	 */
	protected String classpathFromManifest(String jarFilename)
	{
		JarFile jarFile;
		Manifest manifest;
		String classpath;
		Attributes attrs;

		try
		{
			jarFile = new JarFile(jarFilename);
		}
		catch (IOException e)
		{
			// Don't care
			return null;
		}
		try
		{
			manifest = jarFile.getManifest();
			if (manifest == null)
				return null;

			attrs = manifest.getMainAttributes();
			if (attrs == null)
				return null;

			classpath = attrs.getValue("Class-Path");
		}
		catch (IOException e1)
		{
			// I hate code polution by unwanted exception handling !!!!!!
			return null;
		}
		finally
		{
			try
			{
				jarFile.close();
			}
			catch (Throwable e)
			{
				// I hate code polution by unwanted exception handling !!!!!!
			}
		}
		return classpath;
	} // classpathFromManifest() 

	// -------------------------------------------------------------------------

	protected String constructClasspathFrom(String jarFilename)
	{
		String classpath;
		String[] elements;

		classpath = this.classpathFromManifest(jarFilename);
		if (classpath != null)
		{
			classpath = jarFilename + " " + classpath;
			elements = this.str().parts(classpath, " ");
			classpath = this.str().asString(elements, File.pathSeparator);
		}
		return classpath;
	} // constructClasspathFrom() 

	// -------------------------------------------------------------------------

	protected Classpath calculateEclipseClasspath()
	{
		OrderedSet elements;
		Object classLoader;
		Object bundleLoader;
		try
		{
			elements = new OrderedSet(50);
			classLoader = this.getClass().getClassLoader();
			bundleLoader = this.reflect().getValueOf(classLoader, "delegate");
			this.appendClasspathEntriesFrom(elements, bundleLoader);
			return new Classpath(elements);
		}
		catch (Throwable e)
		{
			// Don't care
		}
		return null;
	} // calculateEclipseClasspath() 

	// -------------------------------------------------------------------------

	protected void appendClasspathEntriesFrom(Collection elements, Object bundleLoader)
	{
		Object classloader;
		Object bundlefile;
		Object[] classpathEntries;
		Object[] requiredBundles;
		Object bundleLoader2;
		File file;

		try
		{
			classloader = this.reflect().getValueOf(bundleLoader, "classloader");
			if (classloader == null)
			{
				return;
			}
			classpathEntries = (Object[])this.reflect().getValueOf(classloader, "classpathEntries");
			if (classpathEntries != null)
			{
				for (int i = 0; i < classpathEntries.length; i++)
				{
					bundlefile = this.reflect().getValueOf(classpathEntries[i], "bundlefile");
					file = (File)Dynamic.invoke(bundlefile, "getBaseFile");
					elements.add(file.getPath());
				}
			}
			requiredBundles = (Object[])this.reflect().getValueOf(bundleLoader, "requiredBundles");
			if (requiredBundles != null)
			{
				for (int i = 0; i < requiredBundles.length; i++)
				{
					bundleLoader2 = Dynamic.invoke(requiredBundles[i], "getBundleLoader");
					this.appendClasspathEntriesFrom(elements, bundleLoader2);
				}
			}
		}
		catch (Throwable e)
		{
			// Don't care
		}
	} // appendClasspathEntriesFrom() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the classpath this JVM is using. This is either the current
	 * value of the system property "java.class.path" or in case of JVM started
	 * with option -jar the classpath from the manifest file in the startup JAR.
	 */
	protected String getClasspathFromSystemProperty()
	{
		String classpath;
		String[] elements;
		String suffix;

		classpath = System.getProperty(SYS_PROP_CLASSPATH);
		elements = this.str().parts(classpath, File.pathSeparator);
		if (elements.length == 1)
		{
			suffix = this.str().cutHead(elements[0], ".");
			if (JAR_FILE_EXTENSION.equalsIgnoreCase(suffix))
			{
				classpath = this.constructClasspathFrom(elements[0]);
				if (classpath == null)
				{
					classpath = elements[0];
				}
			}
		}

		return classpath;
	} // getClasspathFromSystemProperty() 

	// -------------------------------------------------------------------------

	/**
	 * Reads all text lines that match the filter from the specified reader into
	 * a String array. If the filter is null all lines will be returned.
	 * After that the reader will be closed. Even if an exception occurs.
	 *
	 * @param reader The reader which provides the text
	 * @param filter A filter that defines which lines to return (may be null -> all lines)
	 */
	protected String[] basicReadTextLinesFrom(final Reader reader, final StringFilter filter) throws IOException
	{
		BufferedReader bufReader;
		LineProcessor processor;
		final List lines = new ArrayList(100);

		bufReader = new BufferedReader(reader);
		try
		{
			processor = new LineProcessor()
			{
				public boolean processLine(String line, int lineNo)
				{
					if ((filter == null) || (filter.matches(line)))
					{
						lines.add(line);
					}
					return true;
				}
			};
			this.processTextLines(bufReader, processor);
		}
		finally
		{
			bufReader.close();
		}
		return this.str().asStrings(lines);
	} // basicReadTextLinesFrom() 

	// ------------------------------------------------------------------------

	protected Classpath determineLookupClasspath(String sysPropName)
	{
		String classpath = null;

		if (!this.str().isNullOrEmpty(sysPropName))
		{
			classpath = System.getProperty(sysPropName);
		}
		if (!this.str().isNullOrBlank(classpath))
		{
			classpath = this.createClasspathFrom(classpath);
			if (classpath != null)
			{
				return new Classpath(classpath);
			}
		}
		return this.createSystemClasspath();
	} // determineLookupClasspath() 

	// -------------------------------------------------------------------------

	protected Classpath createSystemClasspath()
	{
		String classpath;

		if (systemClasspath == null)
		{
			classpath = System.getProperty(SYS_PROP_EXT_DIRS, "");
			if (!this.str().isNullOrEmpty(classpath))
			{
				classpath = classpath + File.pathSeparator;
			}
			classpath = classpath + this.getClasspath();
			systemClasspath = new Classpath(classpath);
		}
		return systemClasspath;
	} // createSystemClasspath() 

	// -------------------------------------------------------------------------

	protected String createClasspathFrom(String path)
	{
		TextEngine textEngine;
		VariableResolver varResolver;

		varResolver = new BasicVariableContainer(System.getProperties());
		textEngine = new TextEngine(varResolver);
		textEngine.setVarStartPrefix(Character.valueOf('$'));
		textEngine.setVarStartDelimiter('{');
		textEngine.setVarEndDelimiter('}');
		textEngine.setSpecialNameCharacters(".");
		try
		{
			return textEngine.completeText(path);
		}
		catch (TextReplacementException e)
		{
			return path;
		}
	} // createClasspathFrom() 

	// -------------------------------------------------------------------------

	protected void assertArgumentNotNull(String argName, Object arg) 
	{
		if (arg == null)
		{
			throw new IllegalArgumentException("Parameter " + argName + " must not be null!");
		}
	} // assertArgumentNotNull() 
	
	// -------------------------------------------------------------------------
	
	protected StringUtil str()
	{
		return StringUtil.current();
	} // str() 

	// -------------------------------------------------------------------------

	protected ReflectUtil reflect()
	{
		return ReflectUtil.current();
	} // reflect() 

	// -------------------------------------------------------------------------

} // class FileUtil 
