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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import bridge.Utf8OutputStream;
import common.CommonConfig;
/**
 * Static class implements our own flavor of base64 encoding, which
 * is also implemented in C and perl.
 * 
 * @author ddyer
 *
 */
public class Base64 implements CommonConfig {
	 private final static char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
	 private static int[]  toInt   = new int[128];
	 static {
	        for(int i=0; i< ALPHABET.length; i++){
	            toInt[ALPHABET[i]]= i;
	        }
	    }
   /**
     * Translates the specified Base64 string into a byte array, ignoring whitespace
     * and allowing = or == at the end to indicate no bytes
     *
     * @param s the Base64 string (not null)
     * @return the byte array (not null)
     */
    public static byte[] decode(String s)
    {
    	ByteArrayOutputStream buf = new Utf8OutputStream();
    	decodeStream(buf,s);
    	return(buf.toByteArray());
    }
    public static String decodeString(String s) 
    {	ByteArrayOutputStream raw = new Utf8OutputStream();
    	decodeStream(raw,s);
     	return(raw.toString());
    }
    private static void decodeStream(OutputStream buf,String s)
    {
        int mask = 0xFF;
        int lim = s.length();
        int i=0;
        int maxval = toInt.length;
        try {
        while(i<lim)
        {
        	char ch0 = ' ',ch1 = ' ',ch2=' ',ch3=' ';
            int realchars = 0; 
            // build a buffer of 4 valid characters
        	while(ch0==' ')
        	{	
        		char newch = ' ';
        		while(newch == ' ')
        		{
        			newch = 'A';
        			if(i<lim)
        			{ newch = s.charAt(i++);
        			  if(newch<=' ') { newch = ' '; }	// skip whitespace
        			  else if (newch=='=') { newch='A'; }	// padding chars
        			  else if ((newch<0) || (newch>=maxval)) { newch=' ';}	// out of range chars
        			  else { realchars++; }
        			}}
        		ch0 = ch1;
        		ch1 = ch2;
        		ch2 = ch3;
        		ch3 = newch;
        	}
            int c0 = toInt[ch0];
            int c1 = toInt[ch1];
            if(realchars>0) { buf.write((byte)(((c0 << 2) | (c1 >> 4)) & mask)); }
            int c2 = toInt[ch2];
            if(realchars>2) { buf.write((byte)(((c1 << 4) | (c2 >> 2)) & mask)); }
            int c3 = toInt[ch3];
            if(realchars>3) { buf.write((byte)(((c2 << 6) | c3) & mask)); }
        }
        } 
        catch (IOException err) { G.print("Ioexception "+err); }
    } 
	/**
	  * Translates the specified byte array into Base64 string.
	  * with \n every 100 input characters and = or == at the end to mark incomplete octets
	  * @param buf the byte array (not null)
	  * @return the translated Base64 string (not null)
	  */
   public static String encode(byte[] buf,boolean addlines){
       int size = buf.length;
   	int outsz = ((size + 2) / 3) * 4 + size/99;	
   	StringBuffer str = new StringBuffer(outsz);
       int i=0;
       boolean skip1=false;
       boolean skip2=false;
       while(i < size){
           byte b0 = buf[i++];
           byte b1 = 0;
            if(i < size) { b1 = buf[i++]; } else { skip1=true; };
           byte b2 = 0;
            if(i < size) { b2 = buf[i++]; } else { skip2=true; };

           int mask = 0x3F;
           str.append(ALPHABET[(b0 >> 2) & mask]);
           str.append(ALPHABET[((b0 << 4) | ((b1 & 0xFF) >> 4)) & mask]);
           str.append(skip1?'=':ALPHABET[((b1 << 2) | ((b2 & 0xFF) >> 6)) & mask]);
           str.append(skip2?'=':ALPHABET[b2 & mask]);
           if(addlines && (i%99==0)) { str.append('\n'); }
       }
       return str.toString();
   }
   public static byte[] getUtf8(String b)
   {
	   try {
		return b.getBytes("UTF-8");
	} catch (UnsupportedEncodingException e) {
		e.printStackTrace();
	}
	   return(b.getBytes());
   }
   public static String encode(byte[]b) { return(encode(b,false));}
   /**
    * encode as an extended block, which may include linebreaks
    * 
    * @param b
    * @return a base64 encoded block
    */
   public static String encode(String b) { return(encode(getUtf8(b),true)); }
   /**
    * encode as a single token, without spaces or linebreaks
    * @param b
    * @return a base64 encoded token
   */
   public static String encodeSimple(String b) { return(encode(getUtf8(b),false)); }
   /**
    * get a string from a byte stream known to be encoded with utf8
    * @param data
    * @return a string
    */
   public static String getString(byte []data)
   {	try {
	   		return data==null ? "" : new String(data,"UTF-8");
   		}
   		catch (UnsupportedEncodingException a)
   			{
   				return new String(data);
   			}
   }
   //
   // this is a simple, arbitrary, and totally uncryptograpic checksum
   // that's used to verify that the java web start string hasn't been 
   // tampered with or damaged.  It has to match the corresponding algorithm
   // in login.cgi
   //
   public static int simplecs(String str)
   {
 		int len = str.length();
		int cs = len;
		for(int i=0;i<len;i++)
 		{
 			cs = (((cs+str.charAt(i))*((i&1)==0?13:17))) % (((cs&5)==0)?0x1256235:0x5030322);
 		}
 		return(cs);
   }

}
