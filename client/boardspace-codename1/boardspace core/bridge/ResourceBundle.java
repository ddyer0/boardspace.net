package bridge;

import java.io.IOException;
import java.io.InputStream;

import com.codename1.ui.Display;
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
}

class CN1Resources  implements ResourceInterface
{	Resources res = null;
	public CN1Resources(String name) throws IOException {
		res = Resources.open(name);	
	}
	public CN1Resources(InputStream name) throws IOException {
		res = Resources.open(name);	
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

class ZipResources implements ResourceInterface
{
	ZipInputStream zip = null;
	
	public ZipResources() {}
	
	public ZipResources(String input) throws IOException
	{
		zip = new ZipInputStream(Display.getInstance().getResourceAsStream(null, input));
	}
	public ZipResources(InputStream input) throws IOException
	{
		zip = new ZipInputStream(input);		
	}
	
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
	String dataNames[] = null;
	String imageNames[] = null;
	boolean loadedOK = false;
	
	ResourceInterface openFile(String f) throws IOException
	{   
		if(f.endsWith(".zip")) { return new ZipResources(f); }
		else { return new CN1Resources(f); }
	}
	ResourceInterface openFile(boolean zip,InputStream f) throws IOException
	{	
		if(zip) { return new ZipResources(f); }
		else { return new CN1Resources(f); }
	}
	// constructor for resources embedded in the app
	 ResourceBundle(String file)
	 {	
		resFile = file;
		loadedOK = false;
		try {
		//G.print("Loading resource bundle "+file+":"+firstName);
		Plog.log.addLog("open ",file);
		res = openFile(resFile);
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
		 Plog.log.addLog("open ",name);
		 res = openFile(resFile.endsWith(".zip"),new FileInputStream(in));
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
		 return((res!=null) && res.getImage(name)!=null); 
	 }

	// return true if name is a data file in this bundle
    public boolean isData(String name)
    {	// 12/2020 codename1 fixed their api to not include a null pointer exception
    	return ((res!=null) && (res.getData(name)!=null));
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
