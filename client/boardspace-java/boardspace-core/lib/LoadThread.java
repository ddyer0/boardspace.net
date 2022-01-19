package lib;

import java.util.*;

import online.common.exCanvas;


/* this is a utility thread class that does two simple functions:
 (1) causing a list of classes to preload,
 (2) if the class is an exCanvas, preload it's images.
 */
public class LoadThread extends Thread  {
	public String classes = null;					// classes to load
	private SimpleObservable observer = null;		// who to tell
	public Throwable error = null;					// if it ended badly, the error
	public Class<?> result = null;					// the class we loaded, if successful

	public LoadThread() {
		super("LoadThread");
	}

	public void setLoadParameters(String inStr, SimpleObserver inParent) {
		classes = inStr;
		addObserver(inParent);
	}

	public SimpleObservable addObserver(SimpleObserver o) {
		if (o != null) {
			if (observer == null) {
				observer = new SimpleObservable();
			}

			observer.addObserver(o);
		}
		return (observer);
	}

	public void run() {
		error = null;
		try {
		if (classes != null) {
				String classname = "";
				String xclassname = "";
				try {
					StringTokenizer s = new StringTokenizer(classes);

					while (s.hasMoreElements()) {
						long now = G.Date();
						classname = s.nextToken();
						xclassname = G.expandClassName(classname);
						// System.out.println("Preloading " + classname);
						Class<?> newclass = G.classForName(xclassname,false);
					if(newclass!=null)
					{
						long later = G.Date();
						G.print("Preloaded " + classname + " "
								+ newclass + " : " + (later - now));

						// make a throwaway instance of the new class, and if
						// it is one of our canvas classes, get it to pre-load
						// the static images. This greatly reduces the apparent lag
						// when launching a new game for the first time.
						Object obj = G.MakeInstance(classname);
						if (obj instanceof exCanvas) {
							exCanvas item = (exCanvas) obj;
							int pix = Image.pixelCount;
							item.lockAndLoadImages();
							long even_later = G.Date();
							pix = (Image.pixelCount-pix)/(1024*1024/10);
							G.print("Images " + classname + " "
									+ (even_later - later)
									+ " "+(pix/10)+"."+(pix%10)+" mpixels"
								);
						}
							result = newclass;
					}
					}
				} 
		    	catch (ThreadDeath err) { throw err;}
				catch (Throwable e) 
				{	// use throwable instead of exception so everything is included
					error = e;
				}
		}

		if (observer != null) {
			observer.setChanged(this);
		}
		}
    	catch (ThreadDeath err) { throw err;}
		catch(Throwable err)
		{	error = err;
		}
	}
}
