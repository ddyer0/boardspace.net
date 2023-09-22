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

import lib.G;
import lib.SimpleObservable;
import lib.SimpleObserver;
import lib.XFrame;
import lib.FileSelector;
import lib.FileSelector.FileSelectorResult;


public class FileDialog implements SimpleObserver
{	
	public static int SAVE = 0;
	public static int LOAD = 1;
	FileSelector selector = null;
	String dir = "";
	String selectedFile = null;
	String caption = null;
	boolean forSave = false;
	XFrame selectorFrame = null;

	public FileDialog(Frame f,String string,  int key) 
	{ 	forSave = (key==SAVE);
		caption = string;
		selector = new FileSelector(null,"",false,true,true,forSave);
		selector.addObserver(this);
		selectorFrame = selector.startDirectory("file selector",true);

	}
	
	public void setFile(String filter) {
		G.print("setFile ",filter);
	}
	public void setVisible(boolean val)
	{
		selectorFrame.setVisible(val);
		if(val)
		{
		while(!selector.exit) { G.waitAWhile(this, 200); }
		URL fu = selector.selectedUrl;
		if(fu!=null)
			{
			String f = fu.getFile();
			int idx = f.lastIndexOf('/');
			if(idx>=0) 
				{ dir = f.substring(0,idx+1); 
				  selectedFile = f.substring(idx+1); 
				}
				else { dir = ""; selectedFile = f; }
			}
		else { dir = null; selectedFile=null; }
		G.print("file selected : "+dir+" + "+selectedFile);
		selectorFrame.dispose(); 
		}
	}

	public String getDirectory() 
	{  return dir;
	}
	public void setDirectory(String baseDir)
	{
		String root = G.documentBaseDir();
		String base = root + baseDir;	
		new File(base).mkdir();
		selector.setPrefixDir(G.getUrl("file:"+root));
		selector.setDirectory(G.getUrl("file:"+base));
	}


	public String getFile() {
		return selectedFile;
	}

	public void update(SimpleObservable observable, Object from,Object data) {
		// notified then hit
		if(data instanceof FileSelectorResult)
		{	FileSelectorResult d = (FileSelectorResult)data;
			selectedFile = d.file!=null ? d.file.toExternalForm() : null; 
		}
	};

}
