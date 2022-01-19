import java.applet.Applet;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;

public class imagetest extends Applet implements Runnable {
	public String urls[] = { "/cgi-bin/tlib/getpicture.cgi?pname=ddyer","/cgi-bin/tlib/getpicture.cgi?pname=junnkname" };
    public URL getURL(String name, boolean doc)
    {
        URL base = doc ? getDocumentBase() : getCodeBase();
        try
        {
            return((base==null) 
                	? new URL(name)		// this should be equivalent to the two argument form
                						// but we'll see if this cures the log complaints about
                						// the two arg form with null as the first arg
                	: new URL(base,name));
        }
        catch (MalformedURLException err)
        {
            throw new Error( "couldn't get URL(" + base + "," + name + ")" + err);
        }
    }

	public void run()
	{
	for(int i=0;i<urls.length;i++)
	{
	String url = urls[i];
	Image im = null;
	for(int attempts=1;attempts<10 && (im==null);attempts++)
	{	if(im==null)
		{
		URL imurl =getURL(url, false);
		System.out.println("attempt "+attempts+" to get "+imurl);
		im = getImage(imurl);
		System.out.println("got "+im);
		}
	}
	}
	}
	public void start()
	{	new Thread(this).run();
	}

}
