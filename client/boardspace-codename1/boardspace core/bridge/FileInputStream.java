package bridge;

import java.io.IOException;
import java.io.InputStream;

import com.codename1.io.FileSystemStorage;

public class FileInputStream extends InputStream
{
	InputStream stream = null;
	public FileInputStream(String filename) throws IOException {
		FileSystemStorage store = FileSystemStorage.getInstance();
		stream = store.openInputStream(filename);
	}
	public FileInputStream(File f) throws IOException {
		FileSystemStorage store = FileSystemStorage.getInstance();
		String filename = f.getAbsolutePath();
		stream = store.openInputStream(filename);
	}
	public int read() throws IOException {
		return(stream.read());
	}
	// subtle point - if these methods are not supplied, the real stream
	// won't be called either.
	public void close() throws IOException { stream.close(); }
	public int available() throws java.io.IOException{ return stream.available(); }
	public void mark(int readlimit){ stream.mark(readlimit);}
	public boolean markSupported(){ return(stream.markSupported()); }
	public void reset() throws IOException { stream.reset(); } 
	// for efficiency, call read methods of the real stream
	// rather than allowing defaults to call our simple method
	public int read(byte[]b) throws IOException { return stream.read(b); }
	public int read(byte[] b, int off, int len) throws IOException { return stream.read(b,off,len); }
	
}
