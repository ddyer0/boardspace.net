package bridge;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
/**
 * this is an output stream where the input is expected to be UTF-8 bytes,
 * and the final .toString() will re-encode the utf-8 bytes as a 
 * unicode string.
 * @author Ddyer
 *
 */
public class Utf8OutputStream extends ByteArrayOutputStream 
{
	public String toString() 
	{	 
		try {
			return(toString("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			return(toString());
		} 
	} 
	public String toString(boolean utf8)
	{
		return(utf8 ? toString() : super.toString());
	}
}
