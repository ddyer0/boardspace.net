/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.

    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/.
 */
package lib;


import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
/**
 * This version of a utf8reader converts an incoming stream which "ought to" be encoded
 * using utf8 to a unicode string.  Some streams are not really utf8, because they contain
 * raw values from 0x80 to 0xff which are not encodings of utf8 characters.  Most commonly
 * they are 0xe9 (accent acute) or something like that.  Rather than throw a runtime exception
 * as IOS does, make the best of it and produce a unicode string "as though" the original
 * stream had been encoded properly.   Various combinations of source and client OS do lots
 * of bad things with file names containing unusual characters.
 * 
 * @author Ddyer
 *
 */
public class Utf8Reader extends Reader
{	InputStream stream;
	byte in[] = new byte[1024];
	int offset = 0;
	int end = 0;
	int totalChars;
	int linen = 0;
	int charn = 0;
	int errs = 0;
	int peekByte = -1;
	int startOfLineOffset = -1;
	boolean BomDetected = false;
	boolean readline = true;
	
	// create from a stream, detect and discard Byte Order Marker 
	// at the head of the stream.
	public Utf8Reader(InputStream in)
	{
		stream = in;
		detectBOM();
	}
	//
	// some UTF8 files start with an explicit marker of #0xef 0xbb 0xbf
	// in this case, discard the marker so it doesn't appear in the 
	// input stream
	//
	private void detectBOM()
	{	try {
			if((getByte() == 0xEF)
				&& (getByte() == 0xBB)
				&& (getByte() == 0xBF))
				{	BomDetected = true;
					return;
				}
			offset = 0; 
			}
		catch (IOException e)
		{	// immediate EOF or something.  Errors will persist	
		}	
	}
	public void close() throws IOException {
		stream.close();
	}
	private String errorLoc()
	{
		return(linen>0 ? ("line "+linen+" + "+charn) : ("char "+totalChars)); 
	}
		
	public int getByte() throws IOException
	{	if(peekByte>=0) { int pb = peekByte; peekByte = -1; return(pb); }
		if(offset==end) { 
			offset = 0;
			startOfLineOffset = -1;
			end = stream.read(in,offset,in.length);
		}
		if(offset>=end) { return(-1); }
		totalChars++;
		return(in[offset++]&0xff);
	}
	public boolean isOkUtf8() throws IOException
	{
		peekByte = getByte();
		return(peekByte>=0x80);		
	}
	public int getNByte(int n) throws IOException
	{	int b = getByte();
		if(b<0x80) 
		{ errs++;
		  if(G.debug())
			  {Plog.log.addLog("Illegal UTF8 char #",n," #x",Integer.toHexString(b)," at ",errorLoc()); }
			}
		return(b & 0x7f);
	}
	public int getCharCode() throws IOException
	{	int ch = getByte();
		if(ch<0x80) { return(ch); }
		if(!isOkUtf8()) { 
			errs++;
			if(G.debug()) { Plog.log.addLog("Illegal UTF8 Singleton #x",Integer.toHexString(ch)," at ",errorLoc()); }
			return(ch);
		}
		if(ch<0xC0) { 
			// illegal ascii value for utf0
			errs++;
			if(G.debug()) { Plog.log.addLog("Illegal UTF8 singleton #x",Integer.toHexString(ch)," at ",errorLoc()); }
			offset++;
			return(ch);
		}
		// multi byte encoding
		if(ch<0xE0)
			{
			// 2 byte encoding
			int b1 = (ch-0xC0)<<6;
			int b2 = getNByte(2);
			return( b1|b2 );
			}
		if(ch<0xF0)
		{
			// 3 byte encoding
			int b1 = (ch-0xE0)<<6;
			int b2 = getNByte(2);
			int b3 = getNByte(3);
			return(( (b1|b2)<<6)|b3);
		}
		else {
			// 4 byte encoding
			int b1 = (ch-0xE0)<<6;
			int b2 = getNByte(2);
			int b3 = getNByte(3);
			int b4 = getNByte(4);
			return( (((b1|b2)<<6)|b3)|b4);
			}

		}
	public int read(char[] cbuf, int off, int len) throws IOException 
	{
		for(int i=0; i<len;i++)
		{
			int cc = getCharCode();
			if(cc<0) {  return(i==0 ? -1 : i); }
			cbuf[i] = (char)cc;
		}
		return(len);	
	}
	
	StringBuffer out = null;
	public String readLine() throws IOException
	{	readline = true;
		if(out==null) { out = new StringBuffer(); } else { out.setLength(0); }
		int ch;
		charn = 0;
		errs = 0;
		startOfLineOffset = offset;
		while ( ((ch=getCharCode())>=0) && (ch!='\n') && (ch!='\r'))
		{
			out.append((char)ch);
			charn++;
		}
		if(ch=='\r') { ch = getCharCode(); if(ch!='\n') { peekByte = ch; }}
		if(charn==0) { return(ch<0 ? null : ""); }
		String msg = out.toString();
		if(errs>0 && G.debug()) 
		{ 	if(startOfLineOffset>=0)
			{	// if it's still available, show the raw data
				StringBuffer b = new StringBuffer();
				b.append("Raw Data:\n");
				for(int i=startOfLineOffset; i<=offset;i++)
					{	b.append(G.format(" %2x",in[i]&0xff));
					}
				b.append("\n");
				for(int i=startOfLineOffset;i<=offset;i++)
				{	b.append(G.format("  %s",(char)in[i]));
				}
				Plog.log.addLog(b.toString());
			}
			Plog.log.addLog("L"+linen+" ",msg,"\n"); }
		
		linen++;
		return(msg);
	}
	// 
	// read an ascii file representing utf-8 encoded characters
	// and return the next word as a string
	//
	public String readToWhitespace(boolean lowercase) throws IOException
	{	readline = true;
		if(out==null) { out = new StringBuffer(); } else { out.setLength(0); }
		int ch;
		charn = 0;
		errs = 0;
		startOfLineOffset = offset;
		while (( (ch=getCharCode())>=0) && (ch!=' ') && (ch!='\t') && (ch!='\n') && (ch!='\r'))
		{	if(lowercase && ch>='A' && ch<='Z') { ch=ch+('a'-'A'); }
			out.append((char)ch);
			charn++;
		}
		if(ch=='\r') { ch = getCharCode(); if(ch!='\n') { peekByte = ch; }}
		if(charn==0) { return(null); }
		linen++;
		return(out.toString());
	}	
	//
	// read an ascii file up to the next end of line
	// return the result as an array of bytes.  This assumes
	// that the bytes are utf-8 encoded and does not contain
	// naked eol characters that arise as part of the character
	// encoding
	//
	public ByteOutputStream readBinaryLine(ByteOutputStream bytesout) throws IOException
	{	readline = true;
		if(bytesout==null) {  bytesout = new ByteOutputStream(); } else { bytesout.reset(); }
		int ch;
		charn = 0;
		errs = 0;
		startOfLineOffset = offset;
		while ( ((ch=getByte())>=0) && (ch!='\n') && (ch!='\r'))
		{
			bytesout.write(ch);
			charn++;
		}
		if(ch=='\r') { ch = getByte(); if(ch!='\n') { peekByte = ch; }}
		linen++;
		if(charn==0) { return(null); }
		return(bytesout);
	}


}
