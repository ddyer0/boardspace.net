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
package lib;

import java.util.*;


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
						G.print("Preloaded ", classname , " "
								, newclass , " : ", (later - now));

						// make a throwaway instance of the new class.  This also has the 
						// side effect of loading the static images for the class if this
						// is a game class.
							int pix = Image.pixelCount;
						G.MakeInstance(classname);
						long even_later = G.Date();
							pix = (Image.pixelCount-pix)/(1024*1024/10);
						G.print("Images " , classname , " "
								, (even_later - later)
								, " ",(pix/10),".",(pix%10)," mpixels");

					
					result = newclass;
					}
					}
				} 
				catch (Throwable e) 
				{	// use throwable instead of exception so everything is included
					error = e;
						}
					}

		if (observer != null) {
			observer.setChanged(this);
		}
		}
		catch(Throwable err)
		{	error = err;
		}
	}
}
