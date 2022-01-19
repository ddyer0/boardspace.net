package lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import jzlib.ZipEntry;
import jzlib.ZipInputStream;

public class FileMapper 
{
	private String getType(String name)
	{
		int ind = name.lastIndexOf('.');
		int sep = name.lastIndexOf('/');
		if((ind>=0) && (ind>=sep)) 
		{ 
		  return(name.substring(ind+1));
		}
		return("");
	}
	public void visitZip(File dir,FileVisitor visitor)
	{	
        InputStream file = null; 
        ZipInputStream zip = null;
        try {
       	file = new FileInputStream(dir);
        ZipEntry z;
        String name;
        zip = new ZipInputStream(file);
        
        while (((z = zip.getNextEntry()) != null) &&
                ((name = z.getName()) != null))
        {	boolean isDir = false;//z.isDirectory();
        	if(!isDir)// ignore zip directories
        	{
        		if(visitor.filter(name,isDir, dir))
        		{
        			visitor.visitZip(dir,z,zip);
        		}
        	}
		}}
        catch (FileNotFoundException e) {
			G.Error("file not found",e);
		} catch (IOException e) {
			G.Error("io exception", e);
		}
        finally {
        try {
        	if(zip!=null) { zip.close(); } 
        	if(file!=null) { file.close();} 
        	}
        	catch (IOException e) {
			// don't care here
        	}
        }//finally 
	}
	public void visitDirectory(File dirFile,FileVisitor visitor)
	{
		File[] files = dirFile.listFiles();
		if(files!=null)
		{
		for(File f : files)
		{
			visit(f,visitor);
		}}
	}
	public void visit(File file,FileVisitor visitor)
	{	
		String name = file.getName();
		String type = getType(name);
		boolean isZip =  "zip".equalsIgnoreCase(type);
		boolean isDir = file.isDirectory() || isZip;
		if(visitor.filter(name,isDir,file))
		{
			if(isZip)
			{
				visitZip(file,visitor);
			}
			if(isDir)
			{
				visitDirectory(file,visitor);
			}
			else 
			{
				visitor.visit(file); 
			}
		}
	}
	
}
