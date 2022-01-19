// ===========================================================================
// CONTENT  : CLASS MarkupWriter
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.3 - 30/07/2004
// HISTORY  :
//  28/07/2000  duma  CREATED
//	30/06/2002	duma	changed	->	Use line.separator rather than "\n"
//	30/06/2002	duma	changed	->	Use " as qoute instead of '
//	22/12/2003	duma	changed	->	Use logger() to report exceptions rather than printStackTrace()
//	30/07/2004	duma	added		->	single quotes
//
// Copyright (c) 2000-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.pax;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.IOException;
import java.io.Writer;

import org.pf.logging.Logger;

/**
 * A class that helps to write out a markup language (e.g. XML) to a stream.
 *
 * @author Manfred Duchrow
 * @version 1.3
 */
public class MarkupWriter
{
	// =========================================================================
	// CONSTANTS
	// =========================================================================
	private static final String NEWLINE = System.getProperty("line.separator") ;


	private static final char SINGLE_QUOTE_CHAR = '\'' ; 
	private static final char DOUBLE_QUOTE_CHAR = '"' ; 
	
	// =========================================================================
	// CLASS VARIABLES
	// =========================================================================
	private static char[] SpecialCharacters = initSpecialCharacters();
	private static String[] Placeholders = initPlaceholders();

	// =========================================================================
	// INSTANCE VARIABLES
	// =========================================================================
	private Writer writer = null;
	protected Writer getWriter() { return writer;	}
	protected void setWriter(Writer newValue) { writer = newValue; }

	// -------------------------------------------------------------------------
	
	private int indentLevel = 0;
	public int getIndentLevel() {	return indentLevel;	}
	protected void setIndentLevel(int newValue)	{	indentLevel = newValue;	}

	// -------------------------------------------------------------------------
	
	private int indentIncrement = 2;
	public int getIndentIncrement() {	return indentIncrement;	}
	public void setIndentIncrement(int newValue) { indentIncrement = newValue; }
	
	// -------------------------------------------------------------------------
	
  private char quoteChar = DOUBLE_QUOTE_CHAR ;
  protected char getQuoteChar() { return quoteChar ; }
  protected void setQuoteChar( char newValue ) { quoteChar = newValue ; }
  
	// =========================================================================
	// CLASS METHODS
	// =========================================================================
	private static char[] initSpecialCharacters()
	{
		char[] characters = { '<', '>', '&' };
		return characters;
	} // initSpecialCharacters() 

	// -------------------------------------------------------------------------

	private static String[] initPlaceholders()
	{
		String[] ph = { "&lt;", "&gt;", "&amp;" };
		return ph;
	} // initPlaceholders() 

	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Initialize the new instance with default values.
	 */
	public MarkupWriter(Writer writer)
	{
		super();
		this.setWriter(writer);
	} // MarkupWriter() 

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	public void writeStartTag(String tagName)
	{
		this.beginStartTag(tagName);
		this.closeTag();
	} // writeStartTag() 

	// -------------------------------------------------------------------------

	public void beginStartTag(String tagName)
	{
		this.beginTag();
		this.write(tagName);
	} // beginStartTag() 

	// -------------------------------------------------------------------------

	public void writeEndTag(String tagName)
	{
		this.beginTag();
		this.write("/");
		this.write(tagName);
		this.closeTag();
	} // writeEndTag() 

	// -------------------------------------------------------------------------

	public void writeEmptyTag(String tagName)
	{
		this.beginTag();
		this.write(tagName);
		this.finishEmptyTag();
	} // writeEmptyTag() 

	// -------------------------------------------------------------------------

	public void finishEmptyTag()
	{
		this.writeSpace();
		this.write("/");
		this.closeTag();
	} // finishEmptyTag() 

	// -------------------------------------------------------------------------

	public void finishTag()
	{
		this.closeTag();
	} // finishTag() 

	// -------------------------------------------------------------------------

	public void writeAttribute(String attrName, String value)
	{
		this.writeSpace();
		this.write(attrName);
		this.writeAssignment();
		this.writeQuote();
		this.writeCharData( value, true);
		this.writeQuote();
	} // writeAttribute() 

	// -------------------------------------------------------------------------

	public void writeText(String text)
	{
		this.writeCharData( text, false );
	} // writeText() 

	// -------------------------------------------------------------------------

	public void newIndentedLine()
	{
		this.write( NEWLINE );
		this.writeIndentation();
	} // newIndentedLine() 

	// -------------------------------------------------------------------------

	public void newIncIndentedLine()
	{
		this.incIndent();
		this.newIndentedLine();
	} // newIncIndentedLine() 

	// -------------------------------------------------------------------------

	public void newDecIndentedLine()
	{
		this.decIndent();
		this.newIndentedLine();
	} // newDecIndentedLine() 

	// -------------------------------------------------------------------------

	public String contents()
	{
		return this.getWriter().toString();
	} // contents() 

	// -------------------------------------------------------------------------

	public void incIndent()
	{
		indentLevel += indentIncrement;
	} // incIndent() 

	// -------------------------------------------------------------------------

	public void decIndent()
	{
		indentLevel -= indentIncrement;
	} // decIndent() 

	// -------------------------------------------------------------------------

	/**
	 * Configures the writer to write single quotes ( ' ) around attribute values.
	 */
	public void useSingleQuotes() 
	{
		this.setQuoteChar( SINGLE_QUOTE_CHAR ) ;
	} // useSingleQuotes() 

	// -------------------------------------------------------------------------
	
	/**
	 * Configures the writer to write double quotes ( " ) around attribute values.
	 */
	public void useDoubleQuotes() 
	{
		this.setQuoteChar( DOUBLE_QUOTE_CHAR ) ;
	} // useDoubleQuotes() 

	// -------------------------------------------------------------------------
	
	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	protected void beginTag()
	{
		this.write("<");
	} // beginTag() 

	// -------------------------------------------------------------------------

	protected void closeTag()
	{
		this.write(">");
	} // closeTag() 

	// -------------------------------------------------------------------------

	protected void writeSpace()
	{
		this.write(" ");
	} // writeSpace() 

	// -------------------------------------------------------------------------

	protected void writeAssignment()
	{
		this.write("=");
	} // writeAssignment() 

	// -------------------------------------------------------------------------

	protected void writeQuote()
	{
		this.write( this.getQuoteChar() );
	} // writeQuote() 

	// -------------------------------------------------------------------------

	protected void writeCharData( String text, boolean asAttrValue )
	{
		// Here we have to translate special characters into their
		// markup language representation. e.g. < is &lt;
		for (int index = 0; index < text.length(); index++)
			this.write( this.translateCharacter(text.charAt(index), asAttrValue) );
	} // writeCharData() 

	// -------------------------------------------------------------------------

	protected void write(String text)
	{
		try
		{
			this.getWriter().write(text);
		}
		catch (IOException ex)
		{
			this.logger().logException(ex) ;
		}
	} // write() 

	// -------------------------------------------------------------------------

	protected void write(char ch)
	{
		try
		{
			this.getWriter().write( ch );
		}
		catch (IOException ex)
		{
			this.logger().logException(ex) ;
		}
	} // write() 

	// -------------------------------------------------------------------------

	protected void flush()
	{
		try
		{
			this.getWriter().flush();
		}
		catch (IOException ex)
		{
			this.logger().logException(ex) ;
		}
	} // flush() 

	// -------------------------------------------------------------------------

	protected void writeIndentation()
	{
		for (int i = 1; i <= indentLevel; i++)
			this.writeSpace();
	} // writeIndentation() 

	// -------------------------------------------------------------------------

	protected int findSpecialChar(char ch)
	{
		for (int i = 0; i < SpecialCharacters.length; i++)
		{
			if (SpecialCharacters[i] == ch)
				return i;
		}
		return -1;
	} // findSpecialChar() 

	// -------------------------------------------------------------------------

	protected String translateCharacter( char ch, boolean transQuote )
	{
		if ( ch == this.getQuoteChar() )
		{
			if ( transQuote )
			{
				switch ( ch )
				{
					case SINGLE_QUOTE_CHAR :
						return "&apos;" ;
					case DOUBLE_QUOTE_CHAR :
						return "&quot;" ;
				} 
			}
		}
		int index = this.findSpecialChar(ch);
		if (index >= 0)
			return Placeholders[index];
		else
			return String.valueOf(ch);
	} // translateCharacter() 

	// -------------------------------------------------------------------------

	protected Logger logger()
	{
		return LoggerProvider.getLogger() ;
	} // logger() 

	// -------------------------------------------------------------------------

} // class MarkupWriter 
