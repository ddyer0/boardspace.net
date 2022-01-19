package bridge;

import java.io.IOException;
import java.io.InputStream;

import com.codename1.ui.util.Resources;

import lib.DataCache;
import lib.G;
import lib.Plog;

/*
 * resource bundle is a cache on the resources provided by codename1,
 * and also wraps some really awful APIs into more reasonable ones.
 * 
 */
public class ResourceBundle
{	
	Resources res = null;
	String resFile = "";
	String dataNames[] = null;
	String imageNames[] = null;
	boolean loadedOK = false;
	
	// constructor for resources embedded in the app
	 ResourceBundle(String file)
	 {	
		resFile = file;
		loadedOK = false;
		try {
		//G.print("Loading resource bundle "+file+":"+firstName);
		res = Resources.open(file);
    	dataNames = res.getDataResourceNames();
    	imageNames = res.getImageResourceNames();
    	loadedOK = true;
		} catch (IOException err)
		{
 		G.Advise(false,"Resource file %s missing : %s",file,err);
		}	
	 }
	 // constructor for resources in the data cache
	 ResourceBundle(File in,String name)
	 {	resFile = name;
	 	loadedOK = false;
		try {
		 res = Resources.open(new FileInputStream(in));
		 dataNames = res.getDataResourceNames();
		 imageNames = res.getImageResourceNames();
		 loadedOK = true;
	 	} 
	 	catch (IOException err)
 		{
 		G.print("Appdata resource file missing "+in+ ":"+err);
 		}	
	 }

	 // return true of name is an image in this bundle
	 public boolean isImage(String name)
	 {	// 12/2020 codename1 fixed their api to not include a null pointer exception
		 return((res!=null) && res.isImage(name)); 
	 }

	// return true if name is a data file in this bundle
    public boolean isData(String name)
    {
    	try {
    		if(res!=null) { return(res.isData(name)); }
    	} catch (NullPointerException e) { }
    	return(false);
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
    	String file = file0.endsWith(".res") ? file0 : file0+".res";
    	ResourceBundle res = appdata;
    	if(res==null || !res.resFile.equals(file))
    	{	File f = cache.findResource(file);
    		if(G.Advise(f!=null,"Appdata resource %s not found",name))
    				{
    				Plog.log.addLog("Load resource data ",file);
    				res = new ResourceBundle(f,file);
    				if(res.loadedOK) { appdata = res; } else { res = null; }
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
    // of the project, and requires that theres just a /xx in the name
    public static InputStream getResourceAsStream(String name)
			throws IOException
    {
    	String fullName = (name.charAt(0)=='/') ? name : "/"+name;
		int ind = fullName.lastIndexOf('/');
		ResourceBundle res = getResources(name);
		String localName = fullName.substring(ind+1);
		if(res==null) { G.print("resource file "+name+" not found"); }
		else if(res.isData(localName)) { return(res.getData(localName)); }
		else if(res.isImage(localName)) { return(null); }
		else { G.print("Resource "+localName+" in "+name+" not known"); }
	
		return(null);
	}

}
