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
import java.io.InputStream;

import com.codename1.ui.Image;
import com.codename1.ui.util.Resources;

import lib.DataCache;
import lib.G;
import lib.Plog;
import net.sf.jazzlib.ZipInputStream;

/*
 * resource bundle is a cache on the resources provided by codename1,
 * and also wraps some really awful APIs into more reasonable ones.
 * 
 * TODO: do something about resource file size limits.
 * as it is, the underlying codename1 implementation reads the whole thing into memory
 * which effectively limits the size of resource files, especially on cheap tablets.
 * Possible workarounds
 *  - manually or automatically splitting resources into multiple files
 *  - reimplementing .res reader to build a directory id actual files
 *  - switching to .zip (aka .jar) format (assuming the zip reader doesn't have this problem) 
 * 
 */


// this is a step toward replacing .res files with .zip files in the data cache
// and maybe also in the embedded resources
//
interface ResourceInterface
{	
	public InputStream getData(String name) ;

	public Image getImage(String name) ;

	public String[] getDataResourceNames()  ;

	public String[] getImageResourceNames() ;
	
	public boolean isData(String n);
	public boolean isImage(String n);
}

class CN1Resources  implements ResourceInterface
{	Resources res = null;
	public CN1Resources(String name)
	{	try {
			res = Resources.open(name);	
		} catch (IOException e)
		{
			G.Advise(false,"Error opening resource file %s %s",name,e);
		}
	}
	public boolean isData(String name)
	{
		return getData(name)!=null;
	}
	public boolean isImage(String name)
	{
		return getImage(name)!=null;
	}
	public CN1Resources(File name) 
	{	FileInputStream stream = null;
		try {
			stream = new FileInputStream(name);
			res = Resources.open(stream);	
		}
		catch (IOException e) { G.Error("opening resource file "+name); }
		finally {
			if(stream!=null) 
				{ try { stream.close(); } catch (IOException e) {} 
				}
		}
	}
	
	public InputStream getData(String name) {
		return res.getData(name);
	}
	public Image getImage(String name) {
		return res.getImage(name);
	}
	public String[] getDataResourceNames() {
		return res.getDataResourceNames();
	}
	public String[] getImageResourceNames() {
		return res.getImageResourceNames();
	}
}

/**
 * treat a raw file as a single named resource
 */
class RawResources implements ResourceInterface
{
	File resourceFile;
	String resourceName;
	public RawResources(File f,String n)
	{
		resourceFile = f;
		resourceName = n.substring(1);
	}
	public boolean isData(String name) { return name.equals(resourceName);}
	public boolean isImage(String name) { return false; }
	
	public InputStream getData(String name) {
		if(name.equals(resourceName))
		{	try {
				return new FileInputStream(resourceFile);
			} 
			catch (IOException e)
			{
			G.Error("error opening resource ",e);	
			}
		}
		return null;
	}

	public Image getImage(String name) {
		return null;
	}

	public String[] getDataResourceNames() {
		return new String[] { resourceName };
	}

	public String[] getImageResourceNames() {
		return null;
	}
	
}

/**
 * this is not ready to use, but is a placeholder for a future
 * in which it's possible to use a zip file instead of a .res file
 * 
 */
class ZipResources implements ResourceInterface
{
	ZipInputStream zip = null;
	File zipName = null;
	String zipResourceName = null;
	public ZipResources() {}
	
	public ZipResources(String input)
	{	zipResourceName = input; 
		//zip = new ZipInputStream(Display.getInstance().getResourceAsStream(null, input));
	}
	public ZipResources(File input)
	{	zipName = input;

		
	}
	public boolean isData(String name) { return false; }
	public boolean isImage(String name) { return false; }
	
	public InputStream getData(String name) {
		throw G.Error("Not implemented");
	}

	public Image getImage(String name) {
		throw G.Error("Not implemented");
	}

	public String[] getDataResourceNames() {
		throw G.Error("Not implemented");
	}

	public String[] getImageResourceNames() {
		throw G.Error("Not implemented");
	}

	
}
public class ResourceBundle
{	
	ResourceInterface res = null;
	String resFile = "";
	File cacheFile = null;
	String dataNames[] = null;
	String imageNames[] = null;
	boolean loadedOK = false;
	
	ResourceInterface openResourceFile(String f)
	{   
		if(f.endsWith(".zip")) { return new ZipResources(f); }
		else if(f.endsWith(".res")) { return new CN1Resources(f); }
		else { return new RawResources(new File(f),f); }
	}

	@SuppressWarnings("resource")
	ResourceInterface openLocalFile(File f,String name)
	{	resFile = name;
		cacheFile = f;
		if(name.endsWith(".zip")) { return new ZipResources(f); }
		else if(name.endsWith(".res")) { return new CN1Resources(f); }
		else { return new RawResources(f,name); }
	}

	// constructor for resources embedded in the app
	 ResourceBundle(String file)
	 {	
		resFile = file;
		loadedOK = false;
		//G.print("Loading resource bundle "+file+":"+firstName);
		Plog.log.addLog("open ",file);
		res = openResourceFile(resFile);
    	dataNames = res.getDataResourceNames();
    	imageNames = res.getImageResourceNames();
    	loadedOK = true;
	 }
	 // constructor for resources in the data cache
	 ResourceBundle(File in,String name)
	 {	resFile = name;
	 	loadedOK = false;
		 Plog.log.addLog("open ",name);
		 res = openLocalFile(in,name);
		 dataNames = res.getDataResourceNames();
		 imageNames = res.getImageResourceNames();
		 loadedOK = true;
	 }
	// return true of name is an image in this bundle
	 public boolean isImage(String name)
	 {	// 12/2020 codename1 fixed their api to not include a null pointer exception
		 return((res!=null) && res.isImage(name)); 
	 }

	// return true if name is a data file in this bundle
    public boolean isData(String name)
    {	// 12/2020 codename1 fixed their api to not include a null pointer exception
    	return ((res!=null) && (res.isData(name)));
    }
    // get a data file from this bundle
    public InputStream getData(String name)
    {	
    	return(res.getData(name));
    }
 
    // get an image from this bundle
    public void getImageTo(String name,SystemImage im)
    {	im.setImage(res.getImage(name));
    }
    public static ResourceBundle bundle = null;
    public static ResourceBundle appdata = null;
    public static String APPDATA ="/appdata";
    
    public static ResourceBundle getResources(String name)
     {	G.Assert(name!=null,"GetResources with null resource name");
    	String xName = name.charAt(0)=='/' ? name : "/" + name;
    	if(xName.startsWith(APPDATA))
    	{
    	// load from the data cache
    	xName = xName.substring(APPDATA.length());
    	DataCache cache = DataCache.getInstance();
    	int ind = xName.indexOf('/',1);
        String file0 = ind>=0 ? xName.substring(0,ind) : xName;		// resource name including the /
    	String file = (file0.endsWith(".gz")||file0.endsWith(".zip")||file0.endsWith(".res")) 
    					? file0 
    					: file0+".res";
    	ResourceBundle res = appdata;
    	if(res==null || !res.resFile.equals(file))
    	{	File f = cache.findResource(file);
    		if(G.Advise(f!=null,"Appdata resource %s not found",name))
    				{
    				Plog.log.addLog("Load resource data ",file);
    				res = new ResourceBundle(f,file);
    				if(res.loadedOK) 
    					{ appdata = res; } 
    						else 
    					{ res = null; }
    				}
    	}
    	return(res);
    	}
    	else
    	{
    	int ind = xName.indexOf('/',1);
        String file0 = ind>=0 ? xName.substring(0,ind) : xName;
    	String file = file0.endsWith(".res") ? file0 : file0+".res";
    	ResourceBundle res = bundle;
    	if(res==null || !res.resFile.equals(file))
    	{	
    		Plog.log.addLog("Load resource bundle ",file);
    		res = new ResourceBundle(file);
     		if(res.loadedOK) { bundle = res; } else { res = null; }
    	}
    	return(res);
    	}
    }

  
    static void getImage(String name,SystemImage toImage)
    {
    ResourceBundle res = getResources(name);
	if(res!=null)
		{
		int ind = name.lastIndexOf('/');
		String localName = ind>=0 ? name.substring(ind+1) : name;
		if(res.isImage(localName)) { res.getImageTo(localName,toImage); return; }
		}
	Plog.log.addLog("Image ",name," is missing");
	toImage.createBlankImage(1,1);
    }
    
    // getResourceAsStream loads resources from the top level directory
    // of the project, and requires that there is just a /xx in the name
    public static InputStream getResourceAsStream(String name)
			throws IOException
    {
    	String fullName = (name.charAt(0)=='/') ? name : "/"+name;
		int ind = fullName.lastIndexOf('/');
		ResourceBundle res = getResources(name);
		String localName = fullName.substring(ind+1);
		if(res==null) { G.Advise(false,"resource %s not found",name); }
		else if(res.isData(localName)) { return(res.getData(localName)); }
		else if(res.isImage(localName)) { return(null); }
		else { G.print("Resource ",localName," in ",name," not known"); }
	
		return(null);
	}

}
