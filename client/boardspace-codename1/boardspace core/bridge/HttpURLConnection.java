package bridge;

public class HttpURLConnection extends URLConnection 
{	@SuppressWarnings("unused")
	private URL url;
	public HttpURLConnection(URL u,String method)
	{ super(u); url = u; setRequestMethod(method); }
	public HttpURLConnection(URL u) 
	{ super(u); url = u;  }
	
	public void setRequestMethod(String string)
	{ //G.print("Method "+string+ " for "+url); 
		setHttpMethod(string);
		setPost("POST".equals(string.toUpperCase())); 	// this papers over a bug in android, where http method is effectively ignored.
	}
}
