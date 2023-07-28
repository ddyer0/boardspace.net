
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.net.ssl.SSLHandshakeException;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

//TODO: add special handling for error is : java.io.IOException: write failed: ENOSPC (No space left on device)

/**
 * Load a java application defined by an executable jar loaded from a web site
 * Maintain a lazy cache of all the jar files in the application
 * 
 * (1) The web site provides a list of dates and names of jar files.  We create
 *     a temporary directory, and a ClassLoader that will load from that directory.
 * (2) Seed the cache with the primary jar for the application.
 * (3) Parse the index file for the application found in that jar, which provides
 *     the mapping between package names and the rest of the jar files.
 * (4) start a background process that will load all the jars, eventually
 * (5) start the main application.  Any classes not yet cached in background
 *     are loaded immediately.
 * 
 * For testing purposes, you can run/debug this directly from eclipse, or
 * if you are using a deployed Boardspace.jar, use "java -jar Boardspace.jar -v" to get
 * more detailed information about what's happening.
 * 
 * For actual deployment, boardspace.jar is packaged as a windows executable,
 * as a windows installer, and as a mac .dmg.  See deploy-and-sign\README.txt
 * 
 * Jan/Feb 2023 notes.  There has always been a background of mysterious (and supposedly impossible) "class not found" errors.
 * which came to a head with openjdk 18, which turned an occasional mystery into a certainty. After a lot of investigation,
 * calling addURL turned out to be the cure, but along the way some loopholes and synchronization issues were fixed.
 * 
 * Note about major updates.  The originally deployed version of this launcher expects "version 1" from the applet tag, to for
 * a hard reset, change applettag.cgi to change the version number.   To facilitate softer resets, a "miniloader" tag is 
 * passed to the main class, and the main class can decide what to do based on the totality of circumstances.
 * 
 * @author Ddyer
 *
 */
class CacheInfo {
	long date;			// unix date, seconds since the epoch
	String name;		// the name of the remote jar file
	boolean loaded;		// true if this jar has been copied and is up to date
	static final int BUFFERSIZE = 100*1024;			// copy jar buffer size, 100K
	static boolean verbose = false;
	static boolean uncache = false;
	public String toString() {
		return "<cacheinfo "+name+" loaded "+loaded+">";
	}
	//constructor
	CacheInfo(long d,String n,boolean l) 
	{
		date = d;
		name = n;
		loaded = l;
	}
	private static Stack<CacheInfo> copyInProgress =new Stack<CacheInfo>();
	private static Stack<Thread>waiting = new Stack<Thread>();
	private String nameList()
	{	String n = "(";
		for(int lim=copyInProgress.size()-1; lim>=0; lim--)
		{
			n+=copyInProgress.elementAt(lim).name+" ";
		}
		return n+")";
	}
	
	// slow mode, call copyUrlSync one at a time.
	private void copyUrl(Boardspace parent,String host,String from,File to,long totime) throws MalformedURLException, IOException
	{	boolean delay = false;
		Thread current = Thread.currentThread();
		synchronized (copyInProgress)
		{	if(loaded) { Boardspace.log("avoid double copy "+name);  }
			delay = !copyInProgress.empty();
		    Boardspace.log("Copy "+name+" waiting "+nameList());
			copyInProgress.push(this);
			waiting.push(current);
		}
		
		if(delay) 
		{	
			try { synchronized(current) { current.wait(); } } catch (InterruptedException e) {};
			Boardspace.log("Copy "+name+" resumed");
		}

		copyUrlSync(parent,host,from,to,totime);
		
		synchronized(copyInProgress)
		{
			copyInProgress.removeElement(this);
			waiting.removeElement(current);
			if(!copyInProgress.empty())
			{
				Thread w = waiting.peek();
				CacheInfo c = copyInProgress.peek();
				Boardspace.log("Wake "+c.name+" "+w+" "+nameList());
				
				synchronized (w) { w.interrupt(); }
			}
		}
	}
	/**
	 * copy a file from the web host to the cache
	 * @param parent
	 * @param from
	 * @param to
	 * @param totime
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private synchronized void copyUrlSync(Boardspace parent,String host,String from,File to,long totime) throws MalformedURLException, IOException
	{	// this is synchronized so only one copy operation can ever be in progress for a given file.  The "loaded" flag is set
		// here so there's no race contition.   "parent" is passed in so the addURL method of the classloader can be called, which
		// is important for java 18 and above
		boolean doit = true;
		boolean retry = false;
		while(doit)
		{
		doit = false;
		try {
		InputStream input = new URL(host+from).openConnection().getInputStream();
		if(loaded)
		{
			if(verbose) { Boardspace.log("avoid double copy "+from+" > "+to); }
		}
		else
		{
		if(verbose) { Boardspace.log("Copying "+from+" > "+to); }

		String name = from.substring(from.lastIndexOf('/')+1);
		File toFile = new File(to,name);
		OutputStream output = new FileOutputStream(toFile);
		byte buffer[] = new byte[BUFFERSIZE];
		int nbytes = 0;
		while( (nbytes = input.read(buffer))>0) 
		{	// this is a hack to induce overlap in file transfers
			// try { Thread.sleep(10); } catch (InterruptedException e) {};
			output.write(buffer,0,nbytes);
		}
		if(output!=null) { output.close(); }
		if(toFile!=null)
			{ toFile.setLastModified(totime*1000); 
			}
		if(input!=null) { input.close(); }
		if(verbose) { Boardspace.log("copy "+from+" finished"); }
		loaded = true;
		// register the new jar
		parent.addURL(toFile.toURI().toURL());
		}
		}
		catch (SSLHandshakeException|SocketException err) 
			{ Boardspace.log("retrying after "+err+" copying "+from+" to "+to);
			  if(!retry) { retry = doit = true; }}
		}
		
	}
	
	// copy and cache a file, using the appropriate copy method
	void cacheFile(Boardspace parent,String host,File localDir) throws IOException
	{	
		if(verbose) { Boardspace.log("cache "+name); }
		if(!loaded)	// if multiple threads get caught, only load once
		{	
			if(Boardspace.fastMode)
				{
				// in fastmode (the default) copy the file directly, so potentially several threads
				// might be copying files at the same time.
				copyUrlSync(parent,host,name,localDir,date);
				}
				else 
				{ 
				// in slow mode, a stack of pending copies is maintained
				// and each process takes a turn to copy their jar file
				copyUrl(parent,host,name,localDir,date); 
				}
		}
		if(verbose) { Boardspace.log("cache "+name+" "+loaded); }
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
public class Boardspace extends URLClassLoader implements Runnable,LoaderConfig
{	
	static boolean fastMode = true;			// if true, allow copy operations to overlap in different threads
	static boolean foregroundMode = false;	// if true, do all copying before starting the main application (no background copying)
	static boolean allowPreload = false;	// passed to the main application.  If true, gives the main application permission to use multiple threads
	static String hostName = serverName;	// the server to contact, normally boardspace, might be local development host
	static boolean uncache = false;			// if true, discard the cache
	static boolean testserver = false;		// use the private test server
	static boolean testversion = false;		// use the test version of classes
	static String activeProtocol = protocol;
	static String webHost() { return(activeProtocol+"://"+hostName); }   
	static final String CACHENAME = "cachename.txt";// file in the temp dir to contain the cache ID
	static boolean verbose = false;
	static boolean debug = false;
	static ByteArrayOutputStream log = new ByteArrayOutputStream();
	static PrintStream out = new PrintStream(log);
	// the local cache directory
	private File localCacheDir = null;
	// map from jar names to cachinfo
	private Hashtable<String,CacheInfo> jarCache = new Hashtable<String,CacheInfo>();
	// map from package names to cacheinfo, same cacheinfo object as in the jar cache
	private Hashtable<String,CacheInfo> packageCache = new Hashtable<String,CacheInfo>();
	
	private static long startTime = 0;
	private static Thread startThread = null;

	// this curious construction is so early messages (before the instance is created) and later ones
	// can both use the same interface
	static void log(String message)
	{	long now = System.currentTimeMillis(); 
		Thread thread = Thread.currentThread();
		if(startTime==0) { startTime = now; startThread = thread; }
		Boardspace.out.print("+");
		Boardspace.out.print(now-startTime);
		Boardspace.out.print(" ");
		if(thread!=startThread) { Boardspace.out.print(thread); Boardspace.out.print(" "); }
		Boardspace.out.println(message);
	}
	
	public static final String OS_VERSION = "os.version";
	public static final String OS_ARCH = "os.arch";
	public static final String OS_NAME = "os.name";
	public static final String JAVA_CLASS_VERSION = "java.class.version";
	public static final String JAVA_VERSION = "java.version";
	
	public static final String[] publicSystemProperties = 
			{
			    JAVA_VERSION,		//    Java version number
			    JAVA_CLASS_VERSION, //    Java class version number
			    OS_NAME, 			//    Operating system name
			    OS_ARCH, 			//    Operating system architecture
			    OS_VERSION
			};
	public String getSystemProperties()
	{	StringBuffer b = new StringBuffer();
		b.append(MiniloaderId);
		for (String propname : publicSystemProperties)
		{	
		    String prop = System.getProperty(propname);
		    b.append(",");
		    b.append(propname);
		    b.append("=");
		    b.append(prop);
		}
		return(b.toString());
	}
	// constructor
	public Boardspace(URL[] cache) 	// cache will be the local cache url of the main jar
	{	super(cache);
	}
	
	// name0 identifies a jar, a class, or a resource.
	// return true if we got a valid file
	public boolean assureCached(String name0)
	{	if(verbose) { log("assureCached "+name0); }
		String name = name0;
		CacheInfo info = null;
		if(name.endsWith(".jar")) { info = jarCache.get(name); }
		else 
		{	name = name.replaceAll("/", ".");
			{ int ind = name.lastIndexOf(".");
			  if(ind>=0) { name = name.substring(0,ind); }
			  info = packageCache.get(name);
			  if(verbose) { log("packagecache "+name+" = "+info); }
			}}
		if(info==null) 
		{ if(verbose) { log("No info for "+name0); } 
		  return(false);
		}
		else 
		{if(!info.loaded)
			{	
				try {
					if(verbose) { log("cache "+name); }
					// synchronized so only one copy can be in progress at a time
					info.cacheFile(this,webHost(),localCacheDir);
					if(!info.loaded) { log(""+info+" failed to load"); }
				}
				catch (Exception e)
				{
					showError("Error fetching "+name+" from "+webHost(),e);
				}
			}
		if(verbose) { log("assureCached exit "+name0+" "+info.loaded); }
		return(info.loaded);
		}
		
	}
	@SuppressWarnings("unused")
	private void printStackTrace()
	{
		try { throw new Error("Stack trace");
        } catch (Error e)
        {
        	e.printStackTrace(out);
        }
	}
	/**
	 * get the current list of urls
	 */
	public URL[] getURLs() {
        if(verbose) { log("geturls"); }
		URL[] uu = super.getURLs();
		if(verbose)
		{
			if(uu!=null)
			{
				for(URL u : uu)
				{
				log("Url "+u);	
				}
			}
		}
		return uu;
    }
	/**
	 * this isn't documented.  But starting with jdk19 unless this is called, 
	 * the jars added by findClass won't be used by the class loader.
	 * 
	 */
    public synchronized void addURL(URL url) {
    	if(verbose) { log("addurl "+url); }
        super.addURL(url);
    }

	/**
	 *  this is the main hook to the classloader.  The key to making this work
	 *  is parsing the main jar index to get the package names.
	 */
    public Class<?> findClass(String name) throws ClassNotFoundException
	{	//try {
		if(verbose) { log("findClass "+name); }
		if(assureCached(name))
		{
		Class<?>v  = (super.findClass(name));
		if(verbose) { log("findClass found "+v); }
		return v;
		}
		log("AssureCached "+name+" failed");
		return null;
	}
    /*
    // better not to do this - let the system do the standard things
    // to find a class if they succeed.
    public synchronized final Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
		if(verbose) { log("loadClass "+name); }
		if(assureCached(name))
			{
			return super.loadClass(name,resolve);
            }
		Class<?>v = super.loadClass(name,resolve);
		if(v==null) { log("loadClass "+name+" failed"); }
		return v;
    }
     */
    public URL findResource(String name)
	{	if(verbose) { log("findResource "+name); }
     	// resource names have paths with /, and typically filename.type
    	// so we need to strip off the actual name leaving the path
		if(assureCached(name.substring(0,name.lastIndexOf('/')+1)))
			{
			URL v  = super.findResource(name);
			if(verbose) { log("findResource found "+v); }
			return v;
			}
		if(verbose) { log("findResource found null"); }
		return null;
	}

	/**
	 * create a temporary directory for this application wherever
	 * java normally creates temporary files.
	 * @return
	 * @throws IOException
	 */
	private static File createTempDir() throws IOException
	{
		File temp=null;
			temp = File.createTempFile(hostName,testversion ? "test-jarcache" : "jarcache");	// let java decide where the temp files go
			temp.delete();
			temp = new File(temp.getParent()+"/"+hostName+(testversion?".test-jarcache" : ".jarcache"));	// create a cache directory where
			// the cache will be named xx.com.jarcache, one cache that is updated and reused.
			if(!temp.isDirectory())
			{ 
			  temp.mkdirs(); 
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
	{	String url = webHost()+dir+(testversion?"&test=1":"");
		if(verbose) { log("Url : "+url); }
		try { 
		InputStream content = new URL(url).openConnection().getInputStream();
		String all = new String(readAll(content));
		return(all);
		} catch (IOException e)
		{	if("https".equals(activeProtocol))
			{
			// some old versions of java don't like Boardspace certificate
			if(verbose) { log("https failed: "+e); }
			activeProtocol = "http";
			String all = getCacheSource(dir);
			if((all!=null) && all.startsWith("version,")) 
				{ 
				if(verbose) { log("Downgrade to http succeeded"); }
				return all; 
				}
			String msg = "downgrade https to http failed\n"+all;
			throw new IOException(msg);
			}
			else {
				if(verbose) { log("url failed: "+e); }
				throw e;
			}
		}

	}
	/**
	 parse the master jar's index file.  this gives us the
	 association between packages and jar files.
	*/
	private void parseIndex(URL url) throws IOException
	{
		String file = url.getFile();
		JarFile jf = new JarFile(file);
		JarEntry entry = jf.getJarEntry("META-INF/INDEX.LIST");
		if(entry==null) 
			{ jf.close();
			  throw new IOException("No META-INF/INDEX.LIST in "+url); 
			}
		InputStream in = jf.getInputStream(entry);
		BufferedReader re = new BufferedReader(new InputStreamReader(in));
		String line ;
		CacheInfo info = null;
		// the general format of the index is a jar file
		// followed by a list of packages contained in the jar
		while( (line=re.readLine())!=null)
			{
				if(verbose) { log("jarinfo:"+line); }
				if(line.length()<1) {}
				else if(line.endsWith(".jar"))
					{ info = jarCache.get(line); 
					}
				else if(info!=null) 
					{ String lr = line.replaceAll("/", ".");
					  packageCache.put(lr, info); 
					  if(verbose) { log("packagecache "+lr+" = "+info); }
					}
			}
			jf.close(); 
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
	 */
	private static Boardspace getMiniloader(File cacheDir) throws IOException, ClassNotFoundException
	{	
		URL url = new File(cacheDir,cacheRoot).toURI().toURL(); 
		URL urls[] = new URL[] { url };
		if(uncache) 
		{ for(File f : cacheDir.listFiles()) 
			{
			if(verbose) { log("Deleting "+f); }
			f.delete();
			}
		}

		Boardspace cl = new Boardspace(urls);
		try { cl.loadCache(cacheDir);
		} catch (IOException err)
			{
				System.out.println("miniloader can't contact "+webHost()+ ":" +err.toString());
			}
		cl.assureCached(cacheRoot);
		cl.parseIndex(url);
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
	{	String match = some.substring(some.lastIndexOf('/')+1);
		if(match!=null) { match = match.toLowerCase(); }
		for(File f : files)
		{
			String name = f.getName().toLowerCase();
			if(name.equals(match) && (f.lastModified()==date*1000)) { 
				return(f);	// good cache element
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
		FileWriter s = new FileWriter(new File(cacheDir,CACHENAME));
		s.write(msg);
		s.close();
	}
	public static String masterCache = "";

	/*
	 *  load the cache by immediately copying all the jar files specified
	 *  if not present or older than the available web jar.  A progress
	 *  note will be visible during the downloads.
	 */
	private void loadCache(File cacheDir) throws MalformedURLException, IOException
	{	localCacheDir = cacheDir;
		File []cachedFiles = cacheDir.listFiles();			// what's already in the cache
		String props = getSystemProperties();
		// sending the "environment" parameter is a little bit of future proofing
		// so the receiving script can determine the suitability of this client.
		String dir = getCacheSource(webUrl+"&environment="+escape(props));
		if(!"".equals(dir))
		{
		BufferedReader reader = new BufferedReader(new StringReader(dir));
		// the first line of the result is "version,1,xxx" where xxx is the identifier for the cache
		// changing xxx results in the entire cache being reloaded.
		// the rest of the result is a list of dates (unix format, seconds since the epoch),files to be cached,
		//
		String firstLine = reader.readLine();
		if(firstLine!=null) { firstLine = firstLine.toLowerCase(); }
		if(verbose) { log("webinfo:"+firstLine); }
		String specs[] = firstLine==null ? null : firstLine.split(",");
		if(specs!=null
			&& specs.length==3
			&& "version".equals(specs[0]))
		{
		if("2".equals(specs[1]) || "1".equals(specs[1]))		// require an exact match on version we expect
		{
		boolean mismatch = !(firstLine.equals(getCacheName(cacheDir)));
		String line = null;
		int nlines = 0;
		masterCache = specs[2];
		while( (line=reader.readLine())!=null)
			{
			nlines++;
			if(verbose) { log("webinfo:"+line); }
			if(!line.startsWith("#"))
			{
			String parts[] = line.split(",");
			long cacheTime = Long.valueOf(parts[0]);
			String className = parts[1];
			File f = mismatch ? null : exists(className,cacheTime,cachedFiles);
			String cacheName = new File(className).getName();
			jarCache.put(cacheName,new CacheInfo(cacheTime,className,f!=null));
			}}
			//
			// if the server is internally misconfigured, and delivers no details of the jars
			// then alarm bells ought to go off.  Contrast this to the server is simply offline
			// and perhaps there is a plausible purely offline mode available using the cache
			//
			if(nlines==0)
			{	String msg = "Web resource "+firstLine+"\nis not available\nusing the existing cache";
				showMessage("Missing Web Resource",msg);
				postError(msg);
			}
			else 
			{	// update the cache description
				setCacheName(cacheDir,firstLine); 	// id the cache so if the spec changes, the cache will be discarded
			}
		 }
		else {
			// if the version number has changed, inform the user and try to carry on.
			// this is the big hammer to force reinstallation if (for example) and new jvm has to be installed
			// this should allow offline users to continue
			showMessage("Obsolete, Please Reinstall it",
					"this launcher, version 2, is obsolete\nplease reinstall it from the web site");
		}
		}
		else {
			if(verbose) { log("bad cache key in:\n"+dir+"\n"); }
			throw new Error("bad cache key:\n"+firstLine);
		}}
	}
	
    private static final char[] HexDig = 
        {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
            'E', 'F'
        };
    /**
     * escape an arbitrary string for transmission as http form data. This should be used on the data
     * portion of a parameter, ie; "?foo="+escape(bar)
     */
    static private String escape(String d)
    {
        int idx = 0;
        int len = d.length();
        StringBuffer b = new StringBuffer();

        while (idx < len)
        {
            char c = d.charAt(idx++);

            if (((c >= 'A') && (c <= 'Z'))
            		|| ((c >= 'a') && (c <= 'z'))
            		|| ((c >= '0') && (c <= '9'))
            		)
            {
                b.append(c);
            }
            else if (c == ' ')
            {
                b.append('+');
            }
            else 
            {	// this is not completely correct if some unicode sneaks in
            	b.append("%" + HexDig[(c&0xff) / 16] + HexDig[c % 16]);
             }
        }

        return (b.toString());
    }
    
    private static int postedErrors = 0;
    /**
     * this posts an error to the web site.  The receiving application should be some
     * simple logger that just accepts the body of the message and logs it somehow.
     * @param m
     * @throws IOException 
     */
   private static void postError(String m) throws IOException
   {
	   if(postedErrors++<1)
	   {
	   	String finalmsg = 
	   			"&name=Boardspace&data=" + escape(m);
	   	
 		URL u = new URL(webHost()+errorURL);
 		URLConnection c = u.openConnection();
    	c.setDoOutput(true);
    	if(c instanceof HttpURLConnection)
    		{ ((HttpURLConnection)c).setRequestMethod("POST");
    		}
    	OutputStreamWriter out = new OutputStreamWriter(
    	c.getOutputStream());
    	// output your data here
    	out.write(finalmsg);
    	out.close();
    	InputStream ins = c.getInputStream();
		String all = new String(readAll(ins));
		if(verbose)
			{
			log("web logging message: "+m);
			log("web logging result: "+all); 
			}
	   }
	   else {
		   log("skipped web logging: "+m);
	   }
   }
	/** print an error message with a stack trace, show a visible pop up, and log to a web url
	 * @param caption
	 * @param e
	 */
	private static void showError(String caption,Throwable e)
	{	try {
		StringWriter w = new StringWriter();
		PrintWriter p = new PrintWriter(w);
		if(e!=null) { e.printStackTrace(p); }
		p.close();
		String msg = w.toString();
		out.flush();
		if(log.size()>0)
			{
			msg += "\n"+log.toString();
			}
		showMessage(caption,msg);
	    postError(caption+"\n"+msg);
		}

		catch (Throwable er) { log("Recursive error "+er); };
	}
	private static void showMessage(String caption,String msg)
	{
		log(caption);
		log(msg);
	    JOptionPane.showMessageDialog(null, msg, caption, JOptionPane.INFORMATION_MESSAGE);
	}
	private void delay(int millis)
	{
		try
    	{	// inDel is milliseconds
        Thread.sleep(millis);
    	}
    	catch (InterruptedException e)
    	{
    	}
	}
	public void run() {
		
		if(verbose) { out.println("Background start"); getURLs(); }
		loadAll();
		if(verbose) { getURLs(); out.println("Background finished"); }
	}
	public void loadAll()
	{	String toload[] = new String[jarCache.size()];
		int idx = 0;
		for(Enumeration<String> e = jarCache.keys(); e.hasMoreElements();)
		{
			toload[idx++]=e.nextElement();
		}
		delay(1000);		// delay a while to let the startup happen
		for(String info : toload)
		{	assureCached(info);
			if(foregroundMode) { System.out.print("."); System.out.flush(); }
		}
		if(foregroundMode) { System.out.println(); }
	}
	
	private static void showOut()
	{
		out.flush();
		if(log.size()>0)
			{
			String msg = log.toString();
			JTextArea text = new JTextArea(msg,40,80);
			text.setEditable(false);
			JScrollPane scroll = new JScrollPane(text);
			int v = JOptionPane.showOptionDialog(null, scroll,"Loader log",JOptionPane.INFORMATION_MESSAGE,
					0, null,new String[] { "Ok","Save" },"Ok");
			if(v==1)
				{
				FileDialog fd = new FileDialog((Frame)null,"Save to file",FileDialog.SAVE);
				fd.setFile("boardspace-startup-log.txt");
				fd.setVisible(true);
				// get the selected filename, if any
				try {
				String directory = fd.getDirectory();
				String file = fd.getFile();
				String name =  directory+System.getProperty("file.separator")+file;
				FileOutputStream out = new FileOutputStream(name);
				out.write(msg.getBytes());
				out.close();
				}
				catch (IOException err)
				{
					System.out.println("Save failed "+err);
				}

				}
			}
	}
	public static void main(String[]args)
	{	boolean fastExit = false;
		String runtimeServer = null;
		try {
		for(int i=0;i<args.length;i++)
		{	//log("arg "+i+" "+args[i]);
			if("-v".equals(args[i])) 		// verbose output as a pop-up
				{ CacheInfo.verbose = verbose = true; }
			else if("-vt".equals(args[i])) 	// verbose output to terminal 
				{ CacheInfo.verbose = verbose = true; out = System.out; }
			else if("-testserver".equals(args[i]))	// connect to the test server instead of the main server
				{ testserver = testversion = true; 
				}
			else if("-testversion".equals(args[i]))
				{	// only the class version is test, the connection will be to 
					// the regular server
					testversion = true;	
				}
			else if("-foreground".equals(args[i])) 	// force all downloading to happen before we begin
				{ foregroundMode = true; }
			else if("-uncache".equals(args[i])) 	// flush the cache before beginning
				{ CacheInfo.uncache = uncache = true; }
			else if("-slow".equals(args[i])) 		// copy no more than 1 file at a time
				{ fastMode = false; }
			else if("-server".equals(args[i])) 		// server to connect to
				{ hostName = runtimeServer = args[i+1];  i++; }
			else if("-preload".equals(args[i]))		// allow classes in a separate thread.  overrides the default
				{ allowPreload = "true".equalsIgnoreCase(args[i+1]); i++; }
			else if("-debug".equals(args[i])) 		// more debugging
				{ debug="swat".equalsIgnoreCase(args[i+1]); i++; }
			else 
			{
			String msg = "Options are -v -vt -slow -testserver -testversion -foreground -uncache\n-server <servername> -allowpreload <true|false> -debug <password> -hostname <host>";
			out.println(msg);
			showMessage("Loader Message",msg);
			fastExit=true; 
			}
		}
		if(!fastExit)
		{
		// set the runargs as system properties for the benefit of the real start
		int argnum = 0;
		for(;argnum<runArgs.length;argnum++)
			{ 	String val = runArgs[argnum];
				if( ((argnum&1)!=0)
					&& (runtimeServer!=null)
					&&("servername".equals(runArgs[argnum-1])))
				{ val = runtimeServer;
				  out.println("Runtime server is "+runtimeServer);
				}
				System.setProperty("mainargs-"+argnum,val);
			}
		
		
		if(debug)
		{	
			System.setProperty("mainargs-"+argnum++,"debug");
			System.setProperty("mainargs-"+argnum++,"true");
			System.setProperty("mainargs-"+argnum++,"extraactions");
			System.setProperty("mainargs-"+argnum++,"true");
		}
		if(testserver)
		{
			System.setProperty("mainargs-"+argnum++,"testserver");
			System.setProperty("mainargs-"+argnum++,"true");			
		}
		if(testversion)
		{	// the main program doesn't really need to know this, but in testing, who knows
			System.setProperty("mainargs-"+argnum++,"testversion");
			System.setProperty("mainargs-"+argnum++,"true");			
		}
		

		System.setProperty("mainargs-"+argnum++,"preload");
		System.setProperty("mainargs-"+argnum++,allowPreload ? "true" : "false");
		
		File cacheDir = createTempDir();
		Boardspace m =getMiniloader(cacheDir);
		
		System.setProperty("mainargs-"+argnum++,"classdir");
		System.setProperty("mainargs-"+argnum++,masterCache);

		Class<?> base = m.loadClass(runClass,false);	// get the class to run first
		Thread t = new Thread(m,"Background cache loader");
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();		// load the rest of the classes in background
			
		if(base==null) { 
			out.print("loadClass for base "+runClass+ " is null");
		}
		else
		{
		// foregroundmode forces all the loading to happen before we start
		if(foregroundMode) { t.join(); }
		
		@SuppressWarnings("deprecation")
		Runnable r = (Runnable)base.newInstance();
		new Thread(r).start();
		if(!foregroundMode) { t.join(); }		
		}
		showOut();
		}}
		catch (Throwable e) {
			showError("Error setting up "+hostName,e);
			showOut();
		} 		
	}
}
