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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.ui.util.Resources;

import lib.DataCache;
import lib.G;
import lib.Plog;
import net.sf.jazzlib.ZipEntry;
import net.sf.jazzlib.ZipInputStream;

/*
 * resource bundle is a cache on the resources provided by codename1,
 * and also wraps some really awful APIs into more reasonable ones.
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
		catch (IOException e) { G.Error("opening resource file "+name+" "+e); }
		finally {
			if(stream!=null) 
				{ try { stream.close(); } catch (IOException e) {} 
				}
		}
	}
	
	public InputStream getData(String name) {
		return res!=null ? res.getData(name) : null;
	}
	public Image getImage(String name) {
		return res!=null ? res.getImage(name) : null;
	}
	public String[] getDataResourceNames() {
		return res!=null ? res.getDataResourceNames() : null;
	}
	public String[] getImageResourceNames() {
		return res!=null ? res.getImageResourceNames() : null;
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
 * reads resources from a .zip (aka .jar) archive instead of a .res file.
 * entries are indexed by their base name (the part after the last '/'),
 * matching the flat namespace ResourceBundle already uses for .res files,
 * so an archive entry like "icons/euphoria-sample1.jpg" is looked up as
 * just "euphoria-sample1.jpg". If two entries share a base name (e.g. two
 * different subdirectories), the later one encountered wins - archives
 * meant for use here should keep base names unique.
 *
 * two constructors, matching the two real use cases in ResourceBundle:
 *
 *  - ZipResources(File): the data-cache case (a downloaded archive like
 *    icons.jar). This jazzlib build only provides the streaming API
 *    (ZipInputStream/ZipEntry), not a random-access ZipFile, so instead:
 *    one sequential pass over the archive records the byte offset where
 *    each entry's local header begins. A later lookup opens a fresh
 *    stream on the same File, skip()s straight to that offset, and reads
 *    just that one entry - no need to rescan everything before it, and
 *    no need to hold the whole archive in memory.
 *
 *  - ZipResources(String): the embedded-resource case (an archive bundled
 *    into the app itself). CN1 only exposes embedded resources as a
 *    stream, not a filesystem path, so there's no file to skip() within -
 *    this reads the archive sequentially once at construction time and
 *    caches every entry's bytes in memory. Fine for small bundled
 *    archives; for anything large, prefer the File-based constructor.
 */
class ZipResources implements ResourceInterface
{
	// used by the File (offset-index) constructor
	private File zipFile = null;
	private Map<String,Long> offsetByName = null;

	// used by the String (embedded, stream-based) constructor
	private Map<String,byte[]> dataByName = null;

	private String[] dataNames = null;
	private String[] imageNames = null;

	public ZipResources() {}

	// constructor for a zip/jar bundled into the app itself, opened by name
	public ZipResources(String input)
	{	InputStream in = null;
		try
		{
			in = Display.getInstance().getResourceAsStream(null,input);
			if(in!=null) { loadFromStream(in); }
			else { G.Advise(false,"Zip resource %s not found",input); }
		}
		catch (IOException e)
		{
			G.Advise(false,"Error opening zip resource %s %s",input,e);
		}
		finally
		{
			if(in!=null) { try { in.close(); } catch (IOException e) {} }
		}
	}

	// constructor for a zip/jar in the local data cache, opened by File.
	// builds a name -> byte offset index with one sequential scan, so
	// individual entries can be fetched later without rescanning.
	public ZipResources(File input)
	{	FileInputStream fin = null;
		try
		{
			zipFile = input;
			offsetByName = new HashMap<String,Long>();
			List<String> data = new ArrayList<String>();
			List<String> images = new ArrayList<String>();
			fin = new FileInputStream(input);
			CountingInputStream counting = new CountingInputStream(fin);
			ZipInputStream zin = new ZipInputStream(counting);
			ZipEntry entry;
			long offset = counting.count();
			while((entry = zin.getNextEntry())!=null)
			{	if(!entry.isDirectory())
				{	String n = baseName(entry.getName());
					offsetByName.put(n,offset);
					if(isImageName(n)) { images.add(n); } else { data.add(n); }
				}
				// drain the entry so we're positioned at the next local header
				byte buf[] = new byte[4096];
				while(zin.read(buf)>0) {}
				zin.closeEntry();
				offset = counting.count();
			}
			zin.close();
			dataNames = data.toArray(new String[data.size()]);
			imageNames = images.toArray(new String[images.size()]);
		}
		catch (IOException e)
		{
			G.Advise(false,"Error opening zip resource file %s %s",input,e);
		}
		finally
		{
			if(fin!=null) { try { fin.close(); } catch (IOException e) {} }
		}
	}

	private void loadFromStream(InputStream in) throws IOException
	{	dataByName = new HashMap<String,byte[]>();
		List<String> data = new ArrayList<String>();
		List<String> images = new ArrayList<String>();
		ZipInputStream zin = new ZipInputStream(in);
		ZipEntry entry;
		while((entry = zin.getNextEntry())!=null)
		{	if(!entry.isDirectory())
			{	String n = baseName(entry.getName());
				dataByName.put(n,readAll(zin));
				if(isImageName(n)) { images.add(n); } else { data.add(n); }
			}
			zin.closeEntry();
		}
		zin.close();
		dataNames = data.toArray(new String[data.size()]);
		imageNames = images.toArray(new String[images.size()]);
	}

	private static byte[] readAll(InputStream in) throws IOException
	{	ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte buf[] = new byte[4096];
		int len;
		while((len=in.read(buf))>0) { bos.write(buf,0,len); }
		return bos.toByteArray();
	}

	private static String baseName(String entryName)
	{	int ind = entryName.lastIndexOf('/');
		return ind>=0 ? entryName.substring(ind+1) : entryName;
	}

	// treat these image types as images, everything else as data.
	// adjust this list if the archives you're loading use other formats.
	private static boolean isImageName(String n)
	{	String lc = n.toLowerCase();
		return lc.endsWith(".jpg") || lc.endsWith(".jpeg")
				|| lc.endsWith(".png") || lc.endsWith(".gif");
	}

	private byte[] getBytes(String name)
	{	if(dataByName!=null) { return dataByName.get(name); }
		if(offsetByName!=null && zipFile!=null)
		{	Long offset = offsetByName.get(name);
			if(offset!=null)
			{	FileInputStream fin = null;
				try
				{
					fin = new FileInputStream(zipFile);
					long skipped = fin.skip(offset.longValue());
					if(skipped!=offset.longValue())
					{	G.Advise(false,"short skip reading zip entry %s",name);
						return null;
					}
					ZipInputStream zin = new ZipInputStream(fin);
					ZipEntry entry = zin.getNextEntry();
					if(entry!=null)
					{	return readAll(zin); }
				}
				catch (IOException ex)
				{
					G.Advise(false,"Error reading zip entry %s %s",name,ex);
				}
				finally
				{
					if(fin!=null) { try { fin.close(); } catch (IOException ex) {} }
				}
			}
		}
		return null;
	}

	public boolean isData(String name) { return !isImageName(name) && contains(name); }
	public boolean isImage(String name) { return isImageName(name) && contains(name); }

	private boolean contains(String name)
	{	if(dataByName!=null) { return dataByName.containsKey(name); }
		if(offsetByName!=null) { return offsetByName.containsKey(name); }
		return false;
	}

	public InputStream getData(String name)
	{	byte b[] = getBytes(name);
		return b==null ? null : new ByteArrayInputStream(b);
	}

	public Image getImage(String name)
	{	byte b[] = getBytes(name);
		return b==null ? null : Image.createImage(b,0,b.length);
	}

	public String[] getDataResourceNames() { return dataNames; }
	public String[] getImageResourceNames() { return imageNames; }
}

/**
 * wraps an InputStream to track how many bytes have been read through it,
 * so we can record the byte offset at which each zip entry's local header
 * begins during the initial indexing scan. Built on plain InputStream
 * (with manual delegation) rather than FilterInputStream, since the
 * latter isn't part of this CN1 build's java.io subset.
 */
class CountingInputStream extends InputStream
{
	private InputStream in;
	private long count = 0;
	public CountingInputStream(InputStream i) { in = i; }
	public long count() { return count; }
	public int read() throws IOException
	{	int b = in.read();
		if(b>=0) { count++; }
		return b;
	}
	public int read(byte[] b) throws IOException
	{	return read(b,0,b.length); }
	public int read(byte[] b,int off,int len) throws IOException
	{	int n = in.read(b,off,len);
		if(n>0) { count += n; }
		return n;
	}
	public long skip(long n) throws IOException
	{	long skipped = in.skip(n);
		count += skipped;
		return skipped;
	}
	public int available() throws IOException { return in.available(); }
	public void close() throws IOException { in.close(); }
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
		if(f.endsWith(".zip")||f.endsWith(".jar")) { return new ZipResources(f); }
		else if(f.endsWith(".res")) { return new CN1Resources(f); }
		else { return new RawResources(new File(f),f); }
	}

	@SuppressWarnings("resource")
	ResourceInterface openLocalFile(File f,String name)
	{	resFile = name;
		cacheFile = f;
		if(name.endsWith(".zip")||name.endsWith(".jar")) { return new ZipResources(f); }
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
		if(res!=null)
		{
    	dataNames = res.getDataResourceNames();
    	imageNames = res.getImageResourceNames();
    	loadedOK = true;
		}
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
     	//Plog.log.addLog("getresources ",name);
    	String xName = name.charAt(0)=='/' ? name : "/" + name;
    	if(xName.startsWith(APPDATA))
    	{
    	// load from the data cache
    	xName = xName.substring(APPDATA.length());
    	int ind = xName.indexOf('/',1);
        String file0 = ind>=0 ? xName.substring(0,ind) : xName;		// resource name including the /
    	String file = (file0.endsWith(".gz")||file0.endsWith(".zip")||file0.endsWith(".jar")||file0.endsWith(".res")) 
    					? file0 
    					: file0+".res";
    	ResourceBundle res = appdata;
    	if(res==null || !res.resFile.equals(file))
    	{   DataCache cache = DataCache.getInstance();
    		File f = cache==null ? null : cache.findResource(file);
    		if(G.Advise(f!=null,"Appdata resource %s not found",name))
    				{
    				//Plog.log.addLog("Load resource data ",file);
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
    	String file = (file0.endsWith(".zip")||file0.endsWith(".jar")||file0.endsWith(".res")) 
    					? file0 
    					: file0+".res";
    	ResourceBundle res = bundle;
    	if(res==null || !res.resFile.equals(file))
    	{	
    		Plog.log.addLog("Load resource bundle ",file);
    		res = new ResourceBundle(file);
     		if(res!=null && res.loadedOK) { bundle = res; } else { res = null; }
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
		else if(res.isData(localName))
			{ 
			  InputStream s = res.getData(localName);
			  return s;
			}
		else if(res.isImage(localName)) 
			{ 
			return(null); 
			}
		else { G.print("Resource ",localName," in ",name," not known"); }
	
		return(null);
	}

}
