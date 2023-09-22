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

import java.io.IOException;
import java.io.InputStream;

import bridge.File;
import bridge.FileInputStream;
import bridge.FileNotFoundException;
import net.sf.jazzlib.ZipEntry;
import net.sf.jazzlib.ZipInputStream;



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
			G.Error("file not found %s",e);
		} catch (IOException e) {
			G.Error("io exception %s", e);
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
