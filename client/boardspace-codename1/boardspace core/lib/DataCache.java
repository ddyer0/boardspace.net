package lib;

import java.io.ByteArrayOutputStream;

/** datacache, based on miniloader but with twists.
 * 
 * The motivation for this is to reduce the size of the build, without otherwise
 * affecting the behavior of the app.  Maintenance for the cache is triggered
 * along with the login screen, so ought to be complete before any games can
 * be launched.
 * 
 * this is currently used to maintain a local copy of codename1 resources used
 * by a few games: the dictionary for word games, the images for Imagine, and
 * all the artwork for Black Death.
 *   
 * @author Ddyer
 *
 */

import bridge.BufferedReader;
import bridge.File;
import bridge.FileInputStream;
import bridge.FileOutputStream;
import bridge.JOptionPane;
import bridge.JTextArea;
import bridge.MalformedURLException;
import bridge.URL;
import bridge.URLConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.Hashtable;


/**
 * Load a directory of auxiliary data files.  On desktops this is used to 
 * maintain a cache of jar files used by a private classloader.  On mobiles
 * we use this to maintain a cache of .res files. 
 * 
 * The web site provides a list of dates and names of jar or res files.  We create
 * a temporary directory with copies of the files.  The cache is revalidated by date
 * whenever a web connection is available at startup.
 * 
 * weburl provides the directory service
 * loadCache sets up the cache from weburl and notes which missing elements need to be reloaded.
 * a background process will load them before they are used.
 * 
 * @author Ddyer
 *
 */
class CacheInfo {
	long date;			// unix date, seconds since the epoch
	String name;		// the name of the remote jar file
	boolean loaded;		// true if this jar has been copied and is up to date
	static boolean test = false; // true when using the test server
	static final int BUFFERSIZE = 100*1024;			// copy jar buffer size, 100K
	static boolean verbose = false;
	static boolean uncache = false;
	//constructor
	CacheInfo(long d,String n,boolean l) 
	{
		date = d;
		name = n;
		loaded = l;
	}
	/**
	 * copy a file from the web host to the cache
	 * @param from
	 * @param to
	 * @param totime
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private boolean copyUrl(String host,String from,File to,long totime) throws MalformedURLException, IOException
	{	if((from!=null) && (to!=null))
		{
		InputStream input = new URL(host+from).openConnection().getInputStream();
		if(input!=null)
		{
		if(verbose) { log("Copying "+from+" > "+to); }

		String name = from.substring(from.lastIndexOf('/')+1);
		File toFile = new File(to,name);
		OutputStream output = new FileOutputStream(toFile);
		byte buffer[] = new byte[BUFFERSIZE];
		int nbytes = 0;
		while( (nbytes = input.read(buffer))>0) 
		{
			output.write(buffer,0,nbytes);
		}
		if(output!=null) { output.close(); }
		if(toFile!=null)
			{ toFile.setLastModified(totime*1000); 
			}
		input.close();
		return(true);
		}}
		return(false);
	}
	private void log(String msg)
	{	G.print(msg);
		//DataCache.out.println("cache "+name);
	}
	// synchronize minimally here, so we don't block the broader cache loader
	synchronized void cacheFile(String host,File localDir) throws IOException
	{
		if(verbose) { log("cache "+name); }
		if(!loaded)	// if multiple threads get caught, only load once
		{	copyUrl(host,name,localDir,date);
			loaded = true;
		}
	}
}


/**
 * Boardspace creates and maintains cache of jar files needed by the application,
 * creates a classloader that will use the cache
 * creates the first class file and launches the app, the rest of the
 * jar files are loaded in background, or on demand if the background hasn't
 * gotten to it yet.
 * in case of errors, errors are logged 3 ways, as a message to Boardspace.out, as a pop-up notification, and as
 * a logged message to an error URL
 * 
 * @author Ddyer
 *
 */
public class DataCache implements Runnable
{	
	static String cacheRoot = "java/appdata/";
	static final String webUrl = "/cgi-bin/applettag.cgi?tagname=appdata";	// url to fetch the list of jars

	static boolean test = false;
	static String activeProtocol = "https:";
	static String webHost() 
	{ 	activeProtocol = Http.getDefaultProtocol();
		return(activeProtocol+"//"+Http.getHostName()); 
	}   
	static final String CACHENAME = "cachename.txt";// file in the temp dir to contain the cache ID
	static boolean verbose = false;
	static boolean uncache = false;
	static boolean debug = false;
	static ByteArrayOutputStream log = new ByteArrayOutputStream();
	static PrintStream out = new PrintStream(log);
	// the local cache directory
	private File localCacheDir = null;
	// map from jar names to cachinfo
	private Hashtable<String,CacheInfo> fileCache = new Hashtable<String,CacheInfo>();
	
	static DataCache instance = null;
	public static DataCache getInstance()
	{
		return(instance);
	}
	// constructor
	public DataCache(URL[] cache) 	// cache will be the local cache url of the main jar
	{	
	}
	
	// name0 identifies a a resource.
	// return true if we got a valid file
	public CacheInfo assureCached(String name0)
	{	String name = name0;
		CacheInfo info = null;
		info = fileCache.get(name); 
		
		if(info==null) 
		{ if(verbose) { DataCache.out.println("No info for "+name0); } 
		}
		if(info!=null && !info.loaded)
		{	
			try {
				if(verbose) { DataCache.out.println("cache "+name); }
				info.cacheFile(webHost(),localCacheDir);
			}
			catch (Exception e)
			{
				showError("Error fetching "+name+" from "+webHost(),e);
			}
			info.loaded = true;
			return(info);
		}
		return(info);
	}

    public File findResource(String name)
	{	if(verbose) { DataCache.out.println("Find "+name); }
    	// resource names have paths with /, and typically filename.type
    	// so we need to strip off the actual name leaving the path
		int ind = name.lastIndexOf('/');
		// paper over a bug on ios
		if(ind<0 && (name.charAt(0)=='/')) { ind = 0; }
		String shortName = name.substring(ind+1);
		CacheInfo info = assureCached(shortName);
		return(info!=null ? new File(localCacheDir,shortName) : null);
	}

	/**
	 * create a temporary directory for this application wherever
	 * java normally creates temporary files.
	 * @return
	 * @throws IOException
	 */
	private static File createTempDir() throws IOException
	{
		File temp=new File(G.documentBaseDir()+(test ? "test-appdata" : "appdata"));
		if(!temp.isDirectory())
			{ if(temp.exists()) { temp.delete(); } 
			  temp.mkdir(); 
			}
		return(temp); 
	}
	// because the inputStream "readAllBytes" method didn't arrive
	// until java 9.
	private static byte[]readAll(InputStream s) throws IOException
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		byte[]buffer = new byte[1024];
		int siz = 0;
		while( (siz = s.read(buffer)) >0)
		{
			bytes.write(buffer,0,siz);
		}
		return(bytes.toByteArray());
	}
	/** talk to the server, get a spec for the jars to be cached.
	the first line is a version string
	the rest are date stamp,jar path
	*/
	private String getCacheSource(String dir) throws MalformedURLException, IOException
	{	String url = webHost()+dir+(test?"&test=1":"");
		if(verbose) { DataCache.out.println("Url : "+url); }
		try { 
		URL activeUrl = new URL(url);
		URLConnection conn = activeUrl.openConnection();
		if(conn!=null)
		{
		InputStream content = conn.getInputStream();
		if(content!=null)
		{
		String all = new String(readAll(content));
		return(all);
		}}
		return(null);
		} catch (IOException e)
		{	if("https".equals(activeProtocol))
			{
			// some old versions of java don't like Boardspace certificate
			activeProtocol = "http";
			return(getCacheSource(dir));
			}
			else {
				throw e;
			}
		}

	}

	/**
	 * make a new class loader for the cache directory, and load the master class "name" from it.
	 * This class should be the root class for the application which contains the manifest for
	 * everything else.
	 *  
	 * @param cacheDir
	 * @param serviceName
	 * @return
	 * @throws MalformedURLException
	 * @throws ClassNotFoundException
	 * @throws URISyntaxException 
	 */
	private static DataCache getDataCache(File cacheDir) throws IOException, ClassNotFoundException, URISyntaxException
	{	URI uri = new File(cacheDir,cacheRoot).toURI();
		URL url = new URL(uri.toString()); 
		URL urls[] = new URL[] { url };
		if(uncache) 
		{ for(File f : cacheDir.listFiles()) 
			{
			if(verbose) { DataCache.out.println("Deleting "+f); }
			f.delete();
			}
		}

		DataCache cl = new DataCache(urls);
		cl.loadCache(cacheDir);
		
		return(cl);
	}
	/**
	 * return a File if it exists and should be used in the list of files.
	 * @param some
	 * @param date
	 * @param files
	 * @return
	 */
	private static File exists(String some, long date, File[]files)
	{	String match = some.substring(some.lastIndexOf('/')+1).toLowerCase();
		long targetDate = date*1000;
		for(File f : files)
		{
			String name = f.getName().toLowerCase();
			if(name.equals(match))
				{ long mod = f.lastModified();
				  // f.setLastModified isn't available on android, so in codename1 we have
				  // to accept that the cached file is newer than the specified file date.
				  // if this doesn't work out, we'll have to maintain a separate file property
				  // telling us what we think the file modification date should be.
				  boolean newenough = G.isCodename1() ? (mod>=targetDate) : mod==targetDate;
				  return( newenough ? f : null);
				}
		}
		return(null);
	}

	/* read cachename.txt, which must match the first line of the new cache specifier
	 * 
	 */
	public static String getCacheName(File cacheDir)
	{
		try {
			@SuppressWarnings("resource")
			InputStream s = new FileInputStream(new File(cacheDir,CACHENAME));
			if(s!=null)
			{
				byte b[] = readAll(s);
				s.close();
				return(new String(b));
			}
		}
		catch (IOException e) {}
		return(null);	// not found
	}
	public static void setCacheName(File cacheDir,String msg) throws IOException
	{
		OutputStream os = new FileOutputStream(new File(cacheDir,CACHENAME));
		Writer s = new OutputStreamWriter(os);
		s.write(msg);
		s.close();
	}
	
	/*
	 *  load the cache by immediately copying all the jar files specified
	 *  if not present or older than the available web jar.  A progress
	 *  note will be visible during the downloads.
	 */
	private void loadCache(File cacheDir) throws MalformedURLException, IOException
	{	localCacheDir = cacheDir;
		File []cachedFiles = cacheDir.listFiles();			// what's already in the cache
		String dir = getCacheSource(webUrl);
		if(dir==null)
		{	// reuse the existing cache, regardless of condition....
			long cacheTime = G.Date();
			String firstLine = getCacheName(cacheDir);
			String specs[] = G.split(firstLine,',');
			String name = specs.length>=3 ? specs[2] : "";
			for(File f :cachedFiles)
			{	String cacheName = f.getName();
				String className = name+cacheName;
				fileCache.put(cacheName,new CacheInfo(cacheTime,className,true));
			}
		}
		else
		{
		// validate the cache against the available files online, reload
		BufferedReader reader = new BufferedReader(new StringReader(dir));
		// the first line of the result is "version,1,xxx" where xxx is the identifier for the cache
		// changing xxx results in the entire cache being reloaded.
		// the rest of the result is a list of dates (unix format, seconds since the epoch),files to be cached,
		//
		String firstLine = reader.readLine().toLowerCase();
		if(verbose) { DataCache.out.println("webinfo:"+firstLine); }
		String specs[] = G.split(firstLine,',');
		boolean mismatch = !(firstLine.equals(getCacheName(cacheDir)));
		if(specs.length==3
			&& "version".equals(specs[0])
			&& "1".equals(specs[1]))		// require an exact match on version we expect
		{
		String line = null;
		int nlines = 0;
		while( (line=reader.readLine())!=null)
			{
			nlines++;
			if(verbose) { DataCache.out.println("webinfo:"+line); }
			if(!line.startsWith("#"))
			{
			String parts[] = G.split(line,',');
			long cacheTime = Long.parseLong(parts[0]);
			String className = parts[1];
			File f = mismatch ? null : exists(className,cacheTime,cachedFiles);
			String cacheName = new File(className).getName();
			fileCache.put(cacheName,new CacheInfo(cacheTime,className,f!=null));
			}}
			//
			// if the server is internally misconfigured, and delivers no details of the jars
			// then alarm bells ought to go off.  Contrast this to the server is simply offline
			// and perhaps there is a plausible purely offline mode available using the cache
			//
			if(nlines==0)
			{	String msg = "Web resource "+firstLine+"\nis not available\nusing the existing cache";
				showMessage("Missing Web Resource",msg);
				Http.postError(this,msg,null);
			}
			else 
			{	// update the cache description
				setCacheName(cacheDir,firstLine); 	// id the cache so if the spec changes, the cache will be discarded
			}
		}
		else if("<br>".equals(firstLine)) {}	// old version of appinfo, shouldn't happen but..
		else {
			if(reader!=null) { reader.close(); } 
			throw new Error("bad cache key:\n"+firstLine);
		}
		if(reader!=null) { reader.close(); } 
		}
	}


	/** print an error message with a stack trace, show a visible pop up, and log to a web url
	 * @param caption
	 * @param e
	 */
	private static void showError(String caption,Throwable e)
	{	try {
		StringWriter w = new StringWriter();
		if(e!=null) { w.write(G.getStackTrace(e)); }
		w.close();
		String msg = w.toString();
		showMessage(caption,msg);
	    Http.postError(null,caption+"\n"+msg,e);
		}
		catch (Throwable er) { DataCache.out.println("Recursive error "+er); };
	}
	private static void showMessage(String caption,String msg)
	{
		DataCache.out.println(caption);
		DataCache.out.println(msg);
	    JOptionPane.showMessageDialog(null, msg, caption, JOptionPane.INFORMATION_MESSAGE);
	}

	public void run() {
		String toload[] = new String[fileCache.size()];
		int idx = 0;
		if(verbose) { out.println("Background start"); }
		for(Enumeration<String> e = fileCache.keys(); e.hasMoreElements();)
		{
			toload[idx++]=e.nextElement();
		}
		G.doDelay(1000);		// delay a while to let the startup happen
		for(String info : toload)
		{	assureCached(info);
		}
		if(verbose) { out.println("Background finished"); }
	}

	public static void construct(String...args)
	{	boolean fastExit = false;
		try {
		for(int i=0;i<args.length;i++)
		{	//Boardspace.out.println("arg "+i+" "+args[i]);
			if("-v".equals(args[i])) { CacheInfo.verbose = verbose = true; }
			else if("-testserver".equals(args[i])) { CacheInfo.test = test = true; }
			else if("-uncache".equals(args[i])) { CacheInfo.uncache = uncache = true; }
			else 
			{
			String msg = "Options are -v -testserver -uncache ";
			out.println(msg);
			showMessage("Loader Message",msg);
			fastExit=true; 
			}
		}
		if(!fastExit)
		{
		// set the runargs as system properties for the benefit of the real start

		File cacheDir = createTempDir();
		DataCache m = instance = getDataCache(cacheDir);
		Thread t = new Thread(m,"Background data cache loader");
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();		// load the rest of the classes in background
		
		out.flush();
		if(log.size()>0)
			{
			String msg = log.toString();
			JTextArea text = new JTextArea(msg,40,80);
			text.setEditable(false);
			JOptionPane.showMessageDialog(null,msg,"Loader log",JOptionPane.INFORMATION_MESSAGE);
			}
		}}
		catch (Throwable e) {
			showError("Error setting up data cache",e);
		} 		
	}
}