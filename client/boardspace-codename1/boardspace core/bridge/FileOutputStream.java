package bridge;

import java.io.IOException;
import java.io.OutputStream;

import com.codename1.io.FileSystemStorage;

public class FileOutputStream extends OutputStream {
	OutputStream stream;
	public FileOutputStream(File file) throws IOException
	{	String p =file.getPath();
		//G.print("open output file "+p);
		stream = FileSystemStorage.getInstance().openOutputStream(p);
	}

	public FileOutputStream(String zipname) throws IOException
	{ 	String p = zipname;
		//G.print("open output string "+p);
		stream = FileSystemStorage.getInstance().openOutputStream(p);
	}

	public void write(int b) throws IOException {
		stream.write(b);
	}
	// subtle point - if close is not supplied, close on the real stream
	// won't be called either.
	public void close() throws IOException { stream.close(); }
	// for efficiency, call write methods of the real stream
	// rather than allowing defaults to call our simple method
	public void write(byte[]b) throws IOException { stream.write(b); }
	public void write(byte[] b, int off, int len) throws IOException { stream.write(b,off,len); }

}
