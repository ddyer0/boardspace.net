package bridge;

import java.io.IOException;
import java.io.Reader;

public class StringReader extends Reader  {
	private String string;
	private int index = 0;
	public StringReader(String str) { string = str; index = 0; }
	@Override
	public void close() throws IOException {}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		int lim = string.length();
		int count = 0;
		int dest = off;
		while(len-- > 0 && index<lim) { cbuf[dest++]=string.charAt(index++); count++; }
		return(count==0?-1:count);
	}

}
