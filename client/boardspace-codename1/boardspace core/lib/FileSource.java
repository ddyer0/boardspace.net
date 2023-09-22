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

import bridge.MalformedURLException;
import bridge.URL;

import java.util.Hashtable;

public class FileSource 
{
	String name;
	boolean readOnly = true;
	boolean openZips = true;
	boolean modal = false;
	
    public URL filePaneDir; //the currently selected directory
    public URL dirPaneDir;	// the current directory seen in the directory pane.
    public URL currentZip; //the currently selected zip file

    public URL rootdir; //the root we remove from displays of directory names
    public boolean dirlistlocked = false;
    
    private String prefixdir; //the parent of the currently selected directory
    private String basedir;	
    public String baseDir() { return(basedir); }
    private URL currentDir;
    public URL currentDir() { return(currentDir); }
    
    public FileSource(){};
    // constructor
    public FileSource(String id,URL root,URL current,boolean zips,boolean read,boolean wait,boolean lock)
    {	name = id;
    	rootdir = root;
    	setPrefixDir(current);
    	currentDir = lastBaseDir(current);
    	basedir = "";
    	openZips = zips;
    	readOnly = read;
    	modal = wait;
    	dirlistlocked = lock;
     }
    public URL rootDir() { return(rootdir); }
    public void setRootDir(URL ss)
    {
    	rootdir = ss;
    }
    public void setPrefixDir(URL ss)
    {
    	if(ss!=null)
    		{prefixdir = ss.toExternalForm(); //remove it leaving just the directory
    		if(!prefixdir.endsWith("/")) { prefixdir+='/';}
    		basedir = "";
    		}
    }
    public URL selectedUrl(String fileName)
    {	boolean isdir = fileName.endsWith("/");
     	if(fileName.length()>0 && !isdir) 
    		{
     		if(currentZip!=null) { return(G.getUrl("file:"+fileName)); }
        	return(G.getUrl(basedir+fileName));
    		}
     	if(!isdir) { G.infoBox(NoFileSelected,SelectFileFirst); }
     	return(G.getUrl((fileName.indexOf(':')>=0)?fileName:prefixdir+fileName));
    }
    
    public String selectSubDir(URL dir)
    { 	String str = dir.toExternalForm();
    	setCurrentDir(dir);
        if ((prefixdir != null) && str.startsWith(prefixdir))
        {
            str = str.substring(prefixdir.length());
            basedir = prefixdir;
        }
        else if((rootdir!=null) && str.startsWith(rootdir.toExternalForm()))
        {
        	String prefix = rootdir.toExternalForm();
        	basedir = prefix;
        	str = str.substring(prefix.length());
        }
        else
        { basedir = ""; 
        }
        return(str);
    }
    
    // this implements a temporary cache of the position in the file system
    // we last used.
    private static Hashtable<String,URL>baseDirs = new Hashtable<String,URL>();
    private URL lastBaseDir(URL base)
    {	if(rootdir!=null)
    	{URL alt = baseDirs.get(rootdir.toExternalForm());
    	if(alt!=null) { return(alt); }
    	}
    	return(base);
    }
    private void setCurrentDir(URL alt)
    {	String path = alt.toExternalForm();
    	String lpath = path.toLowerCase();
    	if(! lpath.endsWith(".zip"))
    		{ int idx = path.lastIndexOf('/');
    		  try {
				alt = new URL(path.substring(0,idx+1));
    		  } catch (MalformedURLException e) 
    		  {
    		  }
    		}
    	if(rootdir!=null) 
    		{ baseDirs.put(rootdir.toExternalForm(), alt); 
    }    
    }
    
    static final String NoFileSelected = "no file selected";
    static final String SelectFileFirst = "select a file first";

    public static String[]FileSourceStrings =
    	{
    			NoFileSelected,
    			SelectFileFirst,
 	
    	};
    
}
