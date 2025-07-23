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
package bridge;


import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Storage;

import bridge.FileNameFilter.FilenameFilter;
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
	public File(URI uri) {
		path =uri.toString();
	}
	public URI toURI() throws URISyntaxException { return new URI(path); }
	
	public String getAbsolutePath() { return(path); }
	public String toString() { return("<file "+path+">"); }
	public void renameTo(File file) {
		G.Error("renameTo not implemented");
	}
	public String[] list()
	{
		return normalizedList();
	}
	public String[]normalizedList()
	{
		try {
			String[] fileList = FileSystemStorage.getInstance().listFiles(path);
			return fileList;
		} catch (IOException e) {
			throw G.Error("error getting files from %s",path);
		}
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
	
    public File[] listFiles(FilenameFilter filter) {
        String ss[] = normalizedList();
        if (ss == null) return null;
        ArrayList<File> files = new ArrayList<>();
        for (String s : ss)
            if ((filter == null) || filter.accept(this, s))
                files.add(new File( this, s));
        return files.toArray(new File[files.size()]);
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
