package lib;

import bridge.ObjectInputStream;
import bridge.ObjectOutputStream;
import bridge.Utf8OutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;


public class G2 {

	/**
	 * make a string representing a serializable object.  The string is encoded
	 * with a base64 encoding and padded with whitespace to make it easy to handle.
	 * @param ob
	 * @return a string
	 */
	public static String makeString(Serializable ob)			// make an encoded string from a serializable object
	{	ByteArrayOutputStream stream = new Utf8OutputStream();
		try {
		ObjectOutputStream os = new ObjectOutputStream(stream);
		os.writeObject(ob);
		os.flush();
		os.close();
		} catch (IOException err) {throw G.Error(err.toString());}
		return(Base64.encode(stream.toByteArray(),true));
	}

	/**
	 * read an object from a string that was encoded by {@link makeString}
	 * @param bytes a string encoded
	 * @return an Object
	 */
	public static Object readString(String bytes)				// read an object from an encoded String
	{	byte[] dec = Base64.decode(bytes);
		try {
			ObjectInputStream ins = new ObjectInputStream(new ByteArrayInputStream(dec));
			return(ins.readObject());
		} 
		catch (ClassNotFoundException err) { throw G.Error(err.toString()); }
		catch(IOException err) {throw G.Error(err.toString());}
	}

	/**
	 * decode a string containing unicode characters encoded using &#nnnn;
	 * @param ss
	 * @return the decoded string
	 */
	public static String unicodeDecode(String ss)
	{	
	if(ss!=null)
	{
	StringBuffer sb = new StringBuffer();
	for(int i=0,sslen=ss.length();i<sslen;i++)
	{	char ch = ss.charAt(i);
		if( (ch=='&')
			&&((i+3)<sslen)
			&&(ss.charAt(i+1)=='#'))
			{	int number = 0;
				for(int idx = i+2;idx<sslen;idx++)
				{
				char num = ss.charAt(idx);
				if(num==';') 	// end of sequence
					{ ch = (char)number; 	// convert number into a unicode char
					  i=idx;				// resume the main loop at new index
					  break; 
					}	// end of sequence
				else if(Character.isDigit(num))
					{ number = number*10+(num-'0');	// continue accumulating a number
					}
				else { break; }	// bad format?
				}
			}
		sb.append(ch); 
		}
		ss = sb.toString();
	}
	return(ss);
	}

}
