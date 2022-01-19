// ===========================================================================
// CONTENT  : CLASS XmlStreamWriter
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.6 - 07/02/2010
// HISTORY  :
//  30/06/2002  duma  CREATED
//	26/03/2004	duma	changed	-->	Writes standalone="yes" if necessary
//	30/07/2004	duma	changed	-->	Moved implementation to instance methods
//	11/10/2004	duma	bugfix	--> standalone after encoding
//	27/05/2005	mdu		added		--> appendStylesheetPI(...)
//	12/01/2008	mdu		added		-->	indentation
//	19/01/2008	mdu		added		-->	appendDocumentType() with schemaName
//	07/02/2010	mdu		added		--> appending DOM elements	
//
// Copyright (c) 2002-2010, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.six ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.pf.logging.Logger;
import org.pf.pax.DOMTreeXMLReader;
import org.pf.pax.SAXConstants;
import org.pf.pax.XMLWriteController;
import org.pf.text.StringUtil;
import org.pf.util.NamedTextList;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * Provides convenience methods to write XML data easily to streams.
 *
 * @author Manfred Duchrow
 * @version 1.6
 */
public class XmlStreamWriter
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	private static final String NEWLINE = System.getProperty("line.separator") ;
	
  // =========================================================================
  // CLASS VARIABLES
  // =========================================================================
	protected static final XmlStreamWriter defaultInstance = n() ;
	
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private boolean usingDoubleQuotes = true ;
  protected boolean getUsingDoubleQuotes() { return usingDoubleQuotes ; }
  protected void setUsingDoubleQuotes( boolean newValue ) { usingDoubleQuotes = newValue ; }	
	
  private boolean withNamespacePrefix = false ;
  /**
   * Returns whether or not the namespace prefix is written as well.
   */
  public boolean withNamespacePrefix() { return withNamespacePrefix ; }
  /**
   * Sets whether or not the namespace prefix is written as well.
   */
  public void withNamespacePrefix( boolean newValue ) { withNamespacePrefix = newValue ; }
  
  private int indentation = 2 ;
  /**
   * Returns the current indentation. That is the number of spaces that are prepended
   * to each new level.
   * If not changed the default is 2.
   */
  public int getIndentation() { return indentation ; }
  /**
   * Sets the current indentation. That is the number of spaces that are prepended
   * to each new level.
   */
  public void setIndentation( int newValue ) { indentation = newValue ; }
  // =========================================================================
  // PUBLIC CLASS METHODS
  // =========================================================================
	/**
	 * Writes the given element tree as a well formed XML stream.
	 * This does not include a document type.
	 * 
	 * @param element The root element of an XML tree.
	 * @param writer The writer that receives the output.
	 * @param encoding A valid encoding name or null to omit the attribute.
	 */
	public static void writeWellFormedXML( Element element, Writer writer, 
																					String encoding )
	{
		defaultInstance.appendWellFormedXML( element, writer, encoding, null ) ;
	} // writeWellFormedXML() 

  // -------------------------------------------------------------------------

	/**
	 * Writes the given element tree as a well formed XML stream.
	 * If the dtdURL is null, no document type will be written.
	 * 
	 * @param element The root element of an XML tree.
	 * @param writer The writer that receives the output.
	 * @param encoding A valid encoding name or null to omit the attribute.
	 * @param dtdURL The url to locate the document's DTD (might be null).
	 */
	public static void writeWellFormedXML( Element element, Writer writer, 
																					String encoding, String dtdURL )
	{
		defaultInstance.appendWellFormedXML( element, writer,	encoding, dtdURL ) ;
	} // writeWellFormedXML() 

  // -------------------------------------------------------------------------

	/**
	 * Writes the given element tree as a well formed XML to the given writer.
	 * If the <tt>dtdURL</tt> is null, no document type (DOCTYPE) will be added.
	 * If <tt>dtdURL</tt> is not null then the document type (DOCTYPE) will be added with
	 * the SYSTEM definition or if also the <tt>schemaName</tt> is not null with
	 * the PUBLIC definition. 
	 * 
	 * @param element The root element of an XML tree (must not be null).
	 * @param writer The writer that receives the output (must not be null).
	 * @param encoding A valid encoding name or null to omit the attribute.
	 * @param schemaName The logical name of the schema for the DOCTYPE PUBLIC definition (might be null).
	 * @param dtdURL The URL to locate the document's DTD in the DOCTYPE SYSTEM or PUBLIC definition (might be null).
	 */
	public static void writeWellFormedXML( Element element, Writer writer, 
			String encoding, String schemaName, String dtdURL )
	{
		defaultInstance.appendWellFormedXML( element, writer,	encoding, schemaName, dtdURL ) ;
	} // writeWellFormedXML() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Writes the start line of an XML document to the given writer.
	 * If the specified encoding is not null it will be written as
	 * encoding="..." attribute of the start line.
	 * 
	 * @param writer The writer that receives the output.
	 * @param encoding A valid encoding name or null to omit the attribute.
	 */
	public static void writeDocumentStart( Writer writer, String encoding )
	{
		defaultInstance.appendDocumentStart( writer, encoding, false ) ;
	} // writeDocumentStart() 

  // -------------------------------------------------------------------------

	/**
	 * Writes the start line of an XML document to the given writer.
	 * If the specified encoding is not null it will be written as
	 * encoding="..." attribute of the start line.
	 * 
	 * @param writer The writer that receives the output
	 * @param encoding A valid encoding name or null to omit the attribute.
	 * @param isStandalone If true the standalone="yes" attribute will be added.
	 */
	public static void writeDocumentStart( Writer writer, String encoding, boolean isStandalone )
	{
		defaultInstance.appendDocumentStart( writer, encoding, isStandalone ) ;
	} // writeDocumentStart() 

  // -------------------------------------------------------------------------

	/**
	 * Writes the document type line to the given writer.
	 * 
	 * @param writer The writer that receives the output (must not be null).
	 * @param rootTagName The name of the document's root tag (must not be null).
	 * @param dtdURL The URL to locate the document's DTD (must not be null). 
	 */
	public static void writeDocumentType( Writer writer, String rootTagName,
																				String dtdURL )
	{
		defaultInstance.appendDocumentType( writer, rootTagName, dtdURL ) ;
	} // writeDocumentType() 

  // -------------------------------------------------------------------------

	/**
	 * Writes the document type line to the given writer.
	 * 
	 * @param writer The writer that receives the output (must not be null).
	 * @param rootTagName The name of the document's root tag (must not be null).
	 * @param schemaName The logical name of the schema (e.g. "-//W3C//DTD XHTML 1.0 Strict//EN").
	 * This parameter may be null or blank which means SYSTEM will be used rather than PUBLIC.
	 * @param dtdURL The URL to locate the document's DTD (must not be null). 
	 */
	public static void writeDocumentType( Writer writer, String rootTagName,
			String schemaName, String dtdURL )
	{
		defaultInstance.appendDocumentType( writer, rootTagName, schemaName, dtdURL ) ;
	} // writeDocumentType() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Write the given element and its children to the specified writer 
	 * as XML representation.
	 * 
	 * @param element Usually the root element of an XML tree
	 * @param writer The writer that will receive the output
	 */
	public static void writeSIXElement( Element element, Writer writer )
	{
		defaultInstance.appendSIXElement( element, writer ) ;
	} // writeSIXElement() 

  // -------------------------------------------------------------------------

	/**
	 * Write the given document to the specified writer as XML representation.
	 * Be aware that only the XML data of the DOM will be written. The start
	 * line of a valid XML stream <b>&lt;?xml version="1.0" ?&gt;</b> is not
	 * written by this method.
	 * 
	 * @param document A valid DOM tree
	 * @param writer The writer that will receive the output
	 */
	public static void writeDOM( Document document, Writer writer )
	{
		defaultInstance.appendDOM( document, writer ) ;
	} // writeDOM() 

  // -------------------------------------------------------------------------

	/**
	 * Returns a new instance of this class
	 */
	public static XmlStreamWriter n() 
	{
		return new XmlStreamWriter() ;
	} // n() 

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED CLASS METHODS
  // =========================================================================

	/**
	 * Returns the current logger used by this component to report
	 * errors and exceptions. 
	 */
  protected static Logger logger() 
  { 
  	return LoggerProvider.getLogger() ; 
  } // logger() 

  // -------------------------------------------------------------------------

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public XmlStreamWriter()
  {
    super() ;
  } // XmlStreamWriter() 

  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Configures this writer to use single quotes (') to enclose 
   * XML attribute values.
   */
  public void useSingleQuotes() 
	{
		this.setUsingDoubleQuotes( false ) ;
	} // useSingleQuotes() 

	// -------------------------------------------------------------------------
  
  /**
   * Configures this writer to use double quotes (") to enclose 
   * XML attribute values.
   */
  public void useDoubleQuotes() 
	{
		this.setUsingDoubleQuotes( true ) ;
	} // useDoubleQuotes() 

	// -------------------------------------------------------------------------
  
	/**
	 * Appends the given element tree as a well formed XML to the given writer.
	 * This does not include a document type.
	 * 
	 * @param element The root element of an XML tree
	 * @param writer The writer that receives the output
	 * @param encoding A valid encoding name or null to omit the attribute
	 */
	public void appendWellFormedXML( Element element, Writer writer, 
																					String encoding )
	{
		this.appendWellFormedXML( element, writer, encoding, null ) ;
	} // appendWellFormedXML() 

  // -------------------------------------------------------------------------

	/**
	 * Appends the given element tree as a well formed XML to the given writer.
	 * If the dtdURL is null, no document type will be written.
	 * 
	 * @param element The root element of an XML tree
	 * @param writer The writer that receives the output
	 * @param encoding A valid encoding name or null to omit the attribute
	 * @param dtdURL The URL to locate the document's DTD (might be null)
	 */
	public void appendWellFormedXML( Element element, Writer writer, 
																					String encoding, String dtdURL )
	{
		this.appendWellFormedXML( element, writer, encoding, null, dtdURL ) ;
	} // appendWellFormedXML() 

  // -------------------------------------------------------------------------

	/**
	 * Appends the given element tree as a well formed XML to the given writer.
	 * If the <tt>dtdURL</tt> is null, no document type (DOCTYPE) will be added.
	 * If <tt>dtdURL</tt> is not null then the document type (DOCTYPE) will be added with
	 * the SYSTEM definition or if also the <tt>schemaName</tt> is not null with
	 * the PUBLIC definition. 
	 * 
	 * @param element The root element of an XML tree (must not be null).
	 * @param writer The writer that receives the output (must not be null).
	 * @param encoding A valid encoding name or null to omit the attribute.
	 * @param schemaName The logical name of the schema for the DOCTYPE PUBLIC definition (might be null).
	 * @param dtdURL The URL to locate the document's DTD in the DOCTYPE SYSTEM or PUBLIC definition (might be null).
	 */
	public void appendWellFormedXML( Element element, Writer writer, 
			String encoding, String schemaName, String dtdURL )
	{
		this.appendDocumentStart( writer, encoding, ( dtdURL == null ) ) ;
		
		if ( dtdURL != null )
		{
			this.appendDocumentType( writer, element.getName(), dtdURL ) ;
		}
		
		this.appendSIXElement( element, writer ) ;
	} // appendWellFormedXML() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Appends the start line of an XML document to the given writer.
	 * If the specified encoding is not null it will be written as
	 * encoding="..." attribute of the start line.
	 * 
	 * @param writer The writer that receives the output
	 * @param encoding A valid encoding name or null to omit the attribute
	 */
	public void appendDocumentStart( Writer writer, String encoding )
	{
		this.appendDocumentStart( writer, encoding, false ) ;
	} // appendDocumentStart() 

  // -------------------------------------------------------------------------

	/**
	 * Appends the start line of an XML document to the given writer.
	 * If the specified encoding is not null it will be written as
	 * encoding="..." attribute of the start line.
	 * 
	 * @param writer The writer that receives the output
	 * @param encoding A valid encoding name or null to omit the attribute
	 * @param isStandalone If true the standalone="yes" attribute will be added
	 */
	public void appendDocumentStart( Writer writer, String encoding, boolean isStandalone )
	{
		try
		{
			writer.write( "<?xml version=" ) ;
			this.writeQuoted( writer, "1.0" ) ;
			
			if ( encoding != null )
			{
				writer.write( " encoding=" ) ;
				this.writeQuoted( writer, encoding ) ;
			}
			
			if ( isStandalone )
			{
				writer.write( " standalone=" ) ;
				this.writeQuoted( writer, "yes" ) ;
			}
			
			writer.write( " ?>" ) ;
			writer.write( NEWLINE ) ;
		}
		catch (IOException e)
		{
			logger().logException( e ) ;
		}
	} // appendDocumentStart() 

  // -------------------------------------------------------------------------

	/**
	 * Appends an XSL stylesheet processing instruction for the given XSL filename
	 * 
	 * @param writer The writer that receives the output
	 * @param xslFilename The name of an XSL file to refer to  
	 */
	public void appendXslStylesheetPI( Writer writer, String xslFilename )
	{
		NamedTextList attrs ;
		
		if ( ( writer == null ) || ( xslFilename == null ) )
		{
			this.logger().logError( "null argument passed to " 
					+ this.getClass().getName() + ". appendXslStylesheetPI(Writer,String)" ) ;
			return ;
		}
		
		attrs = new NamedTextList() ;
		attrs.add( "type", "text/xsl" ) ;
		attrs.add( "href", xslFilename ) ;
		this.appendStylesheetPI( writer, attrs ) ;
	} // appendXslStylesheetPI() 

  // -------------------------------------------------------------------------

	/**
	 * Appends a stylesheet processing instruction with the given attributes.
	 * The attributes are key/value pairs separated by comma.
	 * <br>
	 * Example: "href=mystyle.css,type=text/css,title=Compact"
	 * 
	 * @param writer The writer that receives the output
	 * @param attributes The attributes for this instruction
	 */
	public void appendStylesheetPI( Writer writer, String attributes )
	{
		Map attrMap ;
		
		if ( ( writer == null ) || ( attributes == null ) )
		{
			this.logger().logError( "null argument passed to " 
					+ this.getClass().getName() + ". appendStylesheetPI(Writer,String)" ) ;
			return ;
		}
		
		attrMap = this.str().asMap( attributes, "," ) ;
		this.appendStylesheetPI( writer, attrMap ) ;
	} // appendStylesheetPI() 

  // -------------------------------------------------------------------------

	/**
	 * Appends a stylesheet processing instruction with the given attributes.
	 * The attributes are key/value pairs separated by comma.
	 * 
	 * @param writer The writer that receives the output
	 * @param attributes The attributes for this instruction
	 */
	public void appendStylesheetPI( Writer writer, Map attributes )
	{
		NamedTextList namedTextList ;
		
		if ( ( writer == null ) || ( attributes == null ) )
		{
			this.logger().logError( "null argument passed to " 
					+ this.getClass().getName() + ". appendStylesheetPI(Writer,Map)" ) ;
			return ;
		}
		
		namedTextList = new NamedTextList( attributes );
		this.appendStylesheetPI( writer, namedTextList ) ;
	} // appendStylesheetPI() 

  // -------------------------------------------------------------------------

	/**
	 * Appends a stylesheet processing instruction with the given attributes.
	 * 
	 * @param writer The writer that receives the output
	 * @param attributes The attributes for this instruction
	 */
	public void appendStylesheetPI( Writer writer, NamedTextList attributes )
	{
		if ( ( writer == null ) || ( attributes == null ) )
		{
			this.logger().logError( "null argument passed to " 
					+ this.getClass().getName() + ". appendStylesheetPI(Writer,NamedTextList)" ) ;
			return ;
		}

		try
		{
			writer.write( "<?xml-stylesheet " ) ;
			for (int i = 0; i < attributes.size(); i++ )
			{
				if ( i > 0 )
				{
					writer.write( " " ) ;
				}
				writer.write( attributes.nameAt(i) ) ;
				writer.write( "=" ) ;
				this.writeQuoted( writer, attributes.textAt(i) ) ;
			}
			writer.write( " ?>" ) ;
			writer.write( NEWLINE ) ;
		}
		catch (IOException e)
		{
			logger().logException( e ) ;
		}
	} // appendStylesheetPI() 

  // -------------------------------------------------------------------------

	/**
	 * Appends the document type (DOCTYPE) line to the given writer using the 
	 * SYSTEM parameter to specify the DTD location.
	 * 
	 * @param writer The writer that receives the output (must not be null)
	 * @param rootTagName The name of the document's root tag (must not be null)
	 * @param dtdURL The url to locate the document's DTD (must not be null)
	 */
	public void appendDocumentType( Writer writer, String rootTagName,
																				String dtdURL )
	{
		this.appendDocumentType( writer, rootTagName, null, dtdURL ) ;
	} // appendDocumentType() 

  // -------------------------------------------------------------------------

	/**
	 * Appends the document type (DOCTYPE) line to the given writer using the 
	 * PUBLIC parameter if the given schameName is not null or the SYSTEM parameter 
	 * to just specify the DTD location.
	 * 
	 * @param writer The writer that receives the output (must not be null)
	 * @param rootTagName The name of the document's root tag (must not be null)
	 * @param schemaName The logical name of the schema (e.g. "-//W3C//DTD XHTML 1.0 Strict//EN").
	 * This parameter may be null or blank which means SYSTEM will be used rather than PUBLIC.
	 * @param dtdURL The URL to locate the document's DTD (must not be null)
	 */
	public void appendDocumentType( Writer writer, String rootTagName, String schemaName, String dtdURL )
	{
		try
		{
			writer.write( "<!DOCTYPE " ) ;
			writer.write( rootTagName ) ;
			if ( this.str().isNullOrEmpty( schemaName ) )
			{
				writer.write( " SYSTEM "  ) ;				
			}
			else
			{
				writer.write( " PUBLIC "  ) ;				
				this.writeQuoted( writer, schemaName ) ;				
				writer.write( " "  ) ;				
			}
			this.writeQuoted( writer, dtdURL ) ;
			writer.write( ">" ) ;			
			writer.write( NEWLINE ) ;
		}
		catch (IOException e)
		{
			logger().logException( e ) ;
		}
	} // appendDocumentType() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Appends the given element and its children to the specified writer 
	 * as XML representation.
	 * 
	 * @param element Usually the root element of an XML tree
	 * @param writer The writer that will receive the output
	 */
	public void appendSIXElement( Element element, Writer writer )
	{
		ModelConverter converter ;
		Document document ;
		
		converter = new ModelConverter() ;
		document = converter.toDOM( element ) ;
		this.appendDOM( document, writer ) ;
	} // appendSIXElement() 

  // -------------------------------------------------------------------------

	/**
	 * Appends the given document to the specified writer as XML representation.
	 * Be aware that only the XML data of the DOM will be written. The start
	 * line of a valid XML stream <b>&lt;?xml version="1.0" ?&gt;</b> is not
	 * written by this method.
	 * 
	 * @param document A valid DOM tree
	 * @param writer The writer that will receive the output
	 */
	public void appendDOM( Document document, Writer writer )
	{
    this.appendDOMElement( document.getDocumentElement(), writer );
	} // appendDOM() 

  // -------------------------------------------------------------------------
  
	/**
	 * Appends the given DOM element to the specified writer as XML string representation.
	 * Be aware that only the XML data of the DOM will be written. The start
	 * line of a valid XML stream <b>&lt;?xml version="1.0" ?&gt;</b> is not
	 * written by this method.
	 * 
	 * @param element A valid DOM tree element
	 * @param writer The writer that will receive the output
	 */
	public void appendDOMElement( org.w3c.dom.Element element, Writer writer )
	{
		DOMTreeXMLReader xmlReader ;
		XMLWriteController controller ;
		
		xmlReader = this.newXMLReader() ;
		controller = new XMLWriteController( xmlReader, writer ) ;
		controller.setIndentIncrement( this.getIndentation() ) ;
		if ( this.getUsingDoubleQuotes() )
		{
			controller.useDoubleQuotes() ;
		}
		else
		{
			controller.useSingleQuotes() ;
		}
		try
		{
			xmlReader.parse( element ) ;
		}
		catch ( SAXException ex )
		{
			logger().logException( ex ) ;
		}		
	} // appendDOM() 
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected void writeQuoted( Writer writer, String text )
		throws IOException
	{
		writer.write( this.getQuoteChar() ) ;
		writer.write( text ) ;
		writer.write( this.getQuoteChar() ) ;
	} // writeQuoted() 

	// -------------------------------------------------------------------------
	
	protected char getQuoteChar() 
	{
		return this.getUsingDoubleQuotes() ? '"' : '\'' ;
	} // getQuoteChar() 

	// -------------------------------------------------------------------------

	protected DOMTreeXMLReader newXMLReader() 
	{
		DOMTreeXMLReader xmlReader ;
		
    xmlReader = new DOMTreeXMLReader() ;
    if ( this.withNamespacePrefix() )
		{
			try
			{
				xmlReader.setFeature( SAXConstants.FEATURE_NAMESPACE_PREFIXES, true ) ;
			}
			catch ( SAXNotRecognizedException e )
			{
				logger().logException(e) ;
			}
			catch ( SAXNotSupportedException e )
			{
				logger().logException(e) ;
			}
		}
    return xmlReader ;
	} // newXMLReader() 

	// -------------------------------------------------------------------------
	
	protected StringUtil str()
	{
		return StringUtil.current();
	} // str() 

	//-------------------------------------------------------------------------
	
} // class XmlStreamWriter 
