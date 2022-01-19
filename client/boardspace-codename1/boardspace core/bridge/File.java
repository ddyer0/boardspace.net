package bridge;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Storage;

import lib.G;

public class File {
	private String path;
	public File(String p) 
		{ path = p;
		  G.Assert(p!=null,"path must not be null");
		}
	public File(File base,String v)
	{	path = base.path+(base.path.endsWith("/")?v:"/"+v);
	}
	public URI toURI() throws URISyntaxException { return new URI(path); }
	
	public String getAbsolutePath() { return(path); }
	public String toString() { return("<file "+path+">"); }
	public void renameTo(File file) {
		G.Error("renameTo not implemented");
		
	}
	public void mkdir() 
	{	//G.print("mkdir "+path);
		FileSystemStorage.getInstance().mkdir(path);
	}
	public void delete()
	{
		FileSystemStorage.getInstance().delete(path); 
	}
	public String getPath() {
		return(path==null ? "" : path);
	}
	public File[] listFiles()
	{	FileSystemStorage storage = FileSystemStorage.getInstance();
		try {
		String []paths = storage.listFiles(path);
		if(paths!=null)
		{
		int len = paths.length;
		File files[] = new File[len];
		for(int i=0;i<len;i++)
		{	String newname = path+'/'+paths[i];
			if(storage.isDirectory(newname)) { newname += "/"; }
			files[i] = new File(newname);
		}
		return(files);
		}}
		catch (IOException e) {}
		return(null);
	}
	public String getName()
	{	
		int last = path.lastIndexOf('/');
		int len = path.length();
		if((last>=0) && (last==len-1))
		{	// endswith /
			int first = path.lastIndexOf('/',last-1);
			return(path.substring(first+1,last));
		}
		else
		{ // doesn't end with /
		return(path.substring(last+1));
		}
	}
	
	public long length() {
		throw G.Error("Length not implemented");
	}
	public boolean isDirectory()
	{
		return(path.endsWith("/"));
	}
	public boolean exists()
	{
		return(Storage.getInstance().exists(path));
	}
	public long lastModified()
	{	return FileSystemStorage.getInstance().getLastModified(path);
	}
	public void setLastModified(long f)
	{	// nonfunctional, apparently unreliable on android
	}
	public String getParent() {
		int ind = path.lastIndexOf('/');
		if(ind>=0) { return(path.substring(0,ind)); }
		return(null);
	}
	
}
