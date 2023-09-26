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

/**********************************************************\
|                                                          |
| XXTEA.java                                               |
|                                                          |
| XXTEA encryption algorithm library for Java.             |
|                                                          |
| Encryption Algorithm Authors:                            |
|      David J. Wheeler                                    |
|      Roger M. Needham                                    |
|                                                          |
| Code Authors: Ma Bingyao <mabingyao@gmail.com>           |
| LastModified: Mar 10, 2015                               |
|                                                          |
\**********************************************************/


import java.io.UnsupportedEncodingException;

import common.CommonConfig;
import common.SaltInterface;

public final class XXTEA implements CommonConfig{

    private static final int DELTA = 0x9E3779B9;

    private static int MX(int sum, int y, int z, int p, int e, int[] k) {
        return (z >>> 5 ^ y << 2) + (y >>> 3 ^ z << 4) ^ (sum ^ y) + (k[p & 3 ^ e] ^ z);
    }

    private XXTEA() {}

    public static final byte[] encrypt(byte[] data, byte[] key) {
        if (data.length == 0) {
            return data;
        }
        int []ik = toIntArray(key, false);
        return toByteArray(
                encrypt(toIntArray(data, true), ik), false);
    }
    public static final byte[] encrypt(String data, byte[] key) {
        try {
            return encrypt(data.getBytes("UTF-8"), key);
        }
        catch (UnsupportedEncodingException e) {
            return null;
        }
    }
    public static final byte[] encrypt(byte[] data, String key) {
            return encrypt(data, fixKey(key));
     }
    public static final byte[] encrypt(String data, String key) {
        try {
            return encrypt(data.getBytes("UTF-8"),fixKey(key));
        }
        catch (UnsupportedEncodingException e) {
            return null;
        }
    }
    public static final String encryptToBase64String(byte[] data, byte[] key) {
        byte[] bytes = encrypt(data, key);
        if (bytes == null) return null;
        return Base64.encode(bytes);
    }
    public static final String encryptToBase64String(String data, byte[] key) {
        byte[] bytes = encrypt(data, key);
        if (bytes == null) return null;
        return Base64.encode(bytes);
    }
    public static final String encryptToBase64String(byte[] data, String key) {
        byte[] bytes = encrypt(data, key);
        if (bytes == null) return null;
        return Base64.encode(bytes);
    }
    public static final String encryptToBase64String(String data, String key) {
        byte[] bytes = encrypt(data, key);
        if (bytes == null) return null;
        return Base64.encode(bytes);
    }
    public static final byte[] decrypt(byte[] data, byte[] key) {
        if (data.length == 0) {
            return data;
        }
        int []ik = toIntArray(key, false);
        return toByteArray(
                decrypt(toIntArray(data, false),ik) , true);
    }
    public static final byte[] decrypt(byte[] data, String key) {
 
    	return decrypt(data, fixKey(key));

    }
    public static final byte[] decryptBase64String(String data, byte[] key) {
        return decrypt(Base64.decode(data), key);
    }
    public static final byte[] decryptBase64String(String data, String key) {
        return decrypt(Base64.decode(data), key);
    }
    public static final String decryptToString(byte[] data, byte[] key) {
        try {
            byte[] bytes = decrypt(data, key);
            if (bytes == null) return null;
            return new String(bytes, "UTF-8");
        }
        catch (UnsupportedEncodingException ex) {
            return null;
        }
    }
    public static final String decryptToString(byte[] data, String key) {
        try {
            byte[] bytes = decrypt(data, key);
            if (bytes == null) return null;
            return new String(bytes, "UTF-8");
        }
        catch (UnsupportedEncodingException ex) {
            return null;
        }
    }
    public static final String decryptBase64StringToString(String data, byte[] key) {
        try {
            byte[] bytes = decrypt(Base64.decode(data), key);
            if (bytes == null) return null;
            return new String(bytes, "UTF-8");
        }
        catch (UnsupportedEncodingException ex) {
            return null;
        }
    }
    public static final String decryptBase64StringToString(String data, String key) {
        try {
            byte[] bytes = decrypt(Base64.decode(data), key);
            if (bytes == null) return null;
            String s1 = new String(bytes, "UTF-8");
            return(s1);
        }
        catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

    private static int[] encrypt(int[] v, int[] k) {
        int n = v.length - 1;
        if (n < 1) {
            return v;
        }
        int p, q = 6 + 52 / (n + 1);
        int z = v[n], y, sum = 0, e;

        while (q-- > 0) {
            sum = sum + DELTA;
            e = sum >>> 2 & 3;
            for (p = 0; p < n; p++) {
                y = v[p + 1];
                z = v[p] += MX(sum, y, z, p, e, k);
            }
            y = v[0];
            z = v[n] += MX(sum, y, z, p, e, k);
        }
        return v;
    }

    private static int[] decrypt(int[] v, int[] k) {
        int n = v.length - 1;

        if (n < 1) {
            return v;
        }
        int p, q = 6 + 52 / (n + 1);
        int z, y = v[0], sum = q * DELTA, e;

        while (sum != 0) {
            e = sum >>> 2 & 3;
            for (p = n; p > 0; p--) {
                z = v[p - 1];
                y = v[p] -= MX(sum, y, z, p, e, k);
            }
            z = v[n];
            y = v[0] -= MX(sum, y, z, p, e, k);
            sum = sum - DELTA;
        }
        return v;
    }

    // this is a bit of customization, which makes this xxtea not quite 
    // standard as delivered.  Convert the string encryption key to the
    // required 16 bytes using md5 and our special tea sugar.
    private static byte[] fixKey(String key) {
        byte [] val = MD5.computeMD5("teasugar"+key);
        return val;
    }

    private static int[] toIntArray(byte[] data, boolean includeLength) {
        int n = (((data.length & 3) == 0)
                ? (data.length >>> 2)
                : ((data.length >>> 2) + 1));
        int[] result;
        if (includeLength) {
            result = new int[n + 1];
            result[n] = data.length;
        }
        else {
            result = new int[n];
        }
        n = data.length;
        for (int i = 0; i < n; ++i) {
            result[i >>> 2] |= (0x000000ff & data[i]) << ((i & 3) << 3);
        }
        return result;
    }

    private static byte[] toByteArray(int[] data, boolean includeLength) {
        int n = data.length << 2;

        if (includeLength) {
            int m = data[data.length - 1];
            n -= 4;
            if ((m < n - 3) || (m > n)) {
                return null;
            }
            n = m;
        }
        byte[] result = new byte[n];

        for (int i = 0; i < n; ++i) {
            result[i] = (byte) (data[i >>> 2] >>> ((i & 3) << 3));
        }
        return result;
    }
    /**
     * encode msg with key and armor the result with base64.
     * @param msg
     * @param key
     * @return
     */
    public static String Encode(String msg,String key)
    {
    	String cyper = encryptToBase64String(msg, key);
    	//String dec = Decode(cyper,key);
    	//G.Assert(msg.equals(dec),"decode failed");
    	return(cyper);
    }
    /**
     * decode msg with key.   msg is typically something received from the web.
     * if msg is null, result will be null
     * @param msg
     * @param key
     * @return
     */
    public static String Decode(String msg,String key)
    {
    	String plain = msg==null ? null : decryptBase64StringToString(msg, key);
    	return(plain);
    }
    /**
     * validate that str has the standard packaging, which starts with a length
     * and checksum of the payload.  This assures that the payload is complete
     * and unaltered.
     * @param str
     * @return
     */
    public static String validate(String str)
    {	if(str!=null)
    	{
    	int ind = str.indexOf("calc=");
    	if(ind>0)
    	{	int lastIdx = str.indexOf('\n',ind);
    		String mainPart = str.substring(lastIdx+1);
    
        	int lenidx = str.indexOf("len=");
        	int lenidend = str.indexOf('\n',lenidx);
        	if(lenidx>=0 && lenidend>=0)
        	{
        	String lenstr = str.substring(lenidx+4,lenidend-lenidx);
        	int mainlen = mainPart.length();
        	int len = G.IntToken(lenstr);
        	if(len!=mainlen) { return(null); }
        	}

    		String checksumPart = str.substring(ind+5,lastIdx);
    		int desiredcs = G.IntToken(checksumPart);
    		int actualcs = Base64.simplecs(mainPart);
    		
     		return(desiredcs==actualcs ? mainPart : null);
    	}}
    	return(null);
    }
    public static SaltInterface theSalt = null;
    public static boolean saltInterfaceLoaded = false;
    public static void loadSalt()
    {
    	if(!saltInterfaceLoaded)
    	{
    		saltInterfaceLoaded = true;
    		try {
    			theSalt = (SaltInterface)G.MakeInstance("common.Salt");
    		} 
    		catch (Throwable e) {}
    	}
    }
    public static String getSalt()
    {	loadSalt();
    	return theSalt==null ? "" : theSalt.getSalt(); 	
    }
    public static int checksumVersion()
    {
    	loadSalt();
    	return theSalt==null ? 0 : theSalt.checksumVersion();
    }
    public static void loadChecksum(int n)
    {
    	loadSalt();
    	if(theSalt!=null) { theSalt.loadChecksum( n); }
    }
    public static String getTeaKey()
    {
    	loadSalt();
    	return ((theSalt!=null) ? theSalt.getTeaKey() : "dummy encryption key");
    }
    /**
     * package the urlstr in the manner expected by {@link validate}, with 
     * length and checksum, then encrypt it with "key" and armor the result
     * in base64.
     * @param urlStr
     * @param key
     * @return
     */
    // this does it all - pack and encrypt the url str
    public static String combineParams(String urlStr0,String key)
    {	//int salt = 
    	//String urlStr = "checksumversion="+Salt.ChecksumVersion+"\n";
    	//int check = 
    	int vv = checksumVersion();
    	String urlStr = urlStr0;
    	int check = 0;
    	//
    	// this "checksum version" hack is a defence against damaging the live site boardspace
    	// from experiments using sources cloned from github.   The general scheme is that the
    	// live site specifies a "checksum version" as one of the login parameters, which triggers
    	// the use of a shared secret salt to the checksum on login and scoring http transactions.
    	// the github repository, missing this shared secret, can't connect.
    	// this scheme is operational in version 7.55 and later
    	if(vv==0)
    	{
    		check = Base64.simplecs(urlStr);
    	}
    	else
    	{ urlStr = "&checksumversion="+vv+urlStr0;
    	  check = Base64.simplecs(getSalt()+urlStr);
    	}
    
		int len = urlStr.length();		
		// new scheme to make parameters harder to hack, combine and them all and checksum
		// send only the combined for the moment the site still accepts the old format
		// this forgiveness can be eliminated when client 4.82 is obsolete
		
		String ss = "len="+len+"\ncalc="+check+"\n"+urlStr;
		String enc = Encode(ss,key);
		//if(G.debug()) { G.print("params ",enc); }
		return(enc);            
    }
    
    public static void teatest()
    {
    	String text = "The quick brown fox jumps over the lazy dog's back.";
    	String cyper = Encode(text, getTeaKey());
    	String decode = Decode(cyper,getTeaKey());
  
    	G.print("in ",text,"\nout ",cyper,"\nre ",decode);
    }
}
