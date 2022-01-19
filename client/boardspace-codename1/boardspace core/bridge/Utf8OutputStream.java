package bridge;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
//
// this is a byte stream where the bytes are expected to be utf-8 encoded,
// and the final toString will produce a proper unicode string from them.
//
public class Utf8OutputStream extends ByteArrayOutputStream 
{
	public String toString() 
	{	byte ba[] = toByteArray();
		String ss=null;
		try {
			ss = new String(ba, 0, ba.length, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			ss = super.toString();
		}
		return(ss);
	} 
	public String toString(boolean utf8)
	{
		return(utf8 ? toString() : super.toString());
	}
}
