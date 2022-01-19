package bridge;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import lib.G;
import lib.Http;

import com.codename1.io.FileSystemStorage;


public class URL {
	public String urlString;
	private String protocol;
	private String host;
	private int port;
	private String file;
	private InputStream fileStringStream(String prefix,String files[]) throws IOException
	{
		StringWriter p = new StringWriter();
		if(files!=null)
		{
		for(String s : files) 
			{ boolean isDir = (FileSystemStorage.getInstance().isDirectory(prefix+s));
			  p.write(s); 
			  if(isDir) 
			  	{ p.write('/');
			  	}
			  p.write("\n"); 
			}}
		p.close();
		String fill = p.toString();
		//G.print(p);
		return(new StringInputStream(fill));
	}
	private void parse() throws MalformedURLException
	{	if(urlString!=null)
		{
		int colon = urlString.indexOf(":");
		if(colon>0)
		{
			protocol = urlString.substring(0,colon);
			if("http".equalsIgnoreCase(protocol)||"https".equalsIgnoreCase(protocol))
			{
			int colon2 = urlString.indexOf(":",colon+1);
			int slash = urlString.indexOf("//");
			int slash2 = urlString.indexOf("/",slash+2);
			int hostend = ((colon2>colon) && (colon2<slash2)) ? colon2 : slash2;
			if(hostend>0)
				{ host = urlString.substring(slash+2,hostend);
				  port = (hostend+1>=slash2) ? 80 : Integer.parseInt(urlString.substring(hostend+1,slash2));
				  file = urlString.substring(slash2);
				  return;
				}}
			else {
				host = "";
				port = 0;
				file = urlString.substring(colon+1);
				return;
			   }
			throw new MalformedURLException();
		}
		else { file = urlString; }
		}
		return;
	}
	public URL(String s) throws MalformedURLException { urlString = s; parse(); }
	public URL(URL bottomDir, String gamefile) throws MalformedURLException  
		{  this(gamefile);
		   if((bottomDir!=null)&& (protocol==null))
		   {
			   String base = bottomDir.urlString;
			   String file = getFile();
			   if(file.startsWith("/"))
			   {   // file part starts with / , it goes directly on the host
				   int hostIndex = base.indexOf("//");
				   int host2Index = base.indexOf("/",hostIndex+2);
				   if(host2Index>0) { base = base.substring(0,host2Index); }
			   }
			urlString = joinPathComponents(base,file); 		   
			parse();
		   }
		}
	public String getProtocol()
	{	return(protocol);
	}
	public String getHost()
	{	return(host);
	}
	public int getPort() 
	{
		return(port);
	}
	
	public InputStream openStream() throws IOException 
	{ 	String protocol = getProtocol();
		if("https".equalsIgnoreCase(protocol)||"http".equalsIgnoreCase(protocol))
		{
		String host = getHost();
		String file = getFile();
		InputStream reader = Http.getURLStream(host,getPort(),file);
		return(reader);		
		}
		else if(protocol==null) {return(G.getResourceAsStream(getFile())); }
		else if("file".equalsIgnoreCase(protocol)) 
			{ FileSystemStorage storage = FileSystemStorage.getInstance();
			  String activeFile = file;
			  //G.print("open dir "+activeFile);
			  //G.print("Exists "+activeFile+" "+storage.exists(activeFile)+" dir "+storage.isDirectory(activeFile));
			  //if(activeFile.endsWith("/")) 
			  //	{ activeFile = activeFile.substring(0,activeFile.length()-1); 
			  //  System.out.println("Exists "+activeFile+" "+storage.exists(activeFile)+" dir "+storage.isDirectory(activeFile));
			  //	}
			  if(!storage.exists(activeFile)) 
			  	{  throw new FileNotFoundException();
			  	}
			  else if(storage.isDirectory(activeFile))
			  {	String files[] = storage.listFiles(activeFile);
			  	//G.print("got files "+((files==null) ? "null" : files.length));
			  	return fileStringStream(activeFile,files);
			  }
			  else
			  {
			  return(storage.openInputStream(activeFile)); 
			  }
			}
		else {
			throw G.Error("protocol \""+protocol+"\" not supported for "+this); 
		}
	}
	public String toString()
	{ 	// this needs to return a naked string, not something wrapped in a URL identifier.
		return(urlString); 
	}
	public URI toURI() { throw G.Error("toURI Not implemented"); }
	
	public URLConnection openConnection() {
		URLConnection c = new HttpURLConnection(this);
		c.openConnection();
		return(c);
	}
	public URLConnection openConnection(String method) {
		URLConnection c = new HttpURLConnection(this,method);
		c.openConnection();
		return(c);
	}
	public String getFile() {
		return(file);
	}
	public String getDirectory()
	{
		return(file.substring(0,file.lastIndexOf('/')+1));
	}
	public String toExternalForm() { return(urlString);	}
	public String getPath() {
		return urlString;
	}
	/**
	 * joint two file components with exactly one "/" between them
	 * @param base
	 * @param file
	 * @return a new Image
	 */
	private String joinPathComponents(String base,String file)
	{	if(base==null) { return(file); }
		if(file==null) { return(base); }
		return((base.endsWith("/") ? base : base + "/" ) + (file.startsWith("/") ? file.substring(1) : file));
	}

}
