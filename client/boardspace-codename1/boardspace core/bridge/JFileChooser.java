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

import lib.FileSelector;
import lib.G;
import lib.SimpleObservable;
import lib.SimpleObserver;
import lib.XFrame;

public class JFileChooser  implements SimpleObserver
{	public static String FILES = "files";
	public static String FILES_AND_DIRECTORIES = "dirs";
	public static String FORSAVE = "save";	

	FileSelector selector = null;
	String dir = "";
	String selectedFile = null;
	String caption = null;
	boolean forSave = false;
	boolean allowDirs = false;
	XFrame selectorFrame = null;

	// constructor
	public JFileChooser(String string)
	{
		caption = string;
		selector = new FileSelector(null,"",false,true,true,forSave);
		String root = G.documentBaseDir();
		selector.setPrefixDir(G.getUrl("file:"+root));
		selector.setDirectory(G.getUrl("file:"+root));
		selector.addObserver(this);
	}

	public void setDialogTitle(String string) {
		caption = string;
	}
	public void showOpenDialog(Object object) {
		selectorFrame = selector.startDirectory("file selector",true);
		setVisible(true);
	}

	public File getSelectedFile() {
		return(selectedFile==null ? null : new File(selectedFile));
	}

	public void setFileSelectionMode(String filesAndDirectories) {
		forSave = FORSAVE.equals(filesAndDirectories);
		allowDirs = FILES_AND_DIRECTORIES.equals(filesAndDirectories);
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
			dir = fu.getDirectory();
			selectedFile = fu.getFile(); 
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

	}

}
