/* Class    :  NCClient - Noughts & Crosses
 * Author   :  Andrew Miller (rhuk@cis.ufl.edu)
 * Version  :  7/1/96  JAVA SDK 1.02
 * Notice   :  Copyright (C) 1996 Andrew Miller
 *
 * This part of the client is referenced from the appropriate HTML page.  It does not require
 * any parameters but a 'host' parameter can specify the location of the GameServer.  The port
 * for the Noughts and Crosses GameServer is 8432.  This class puts a button on the HTML page
 * and when pressed, calls NCWindow which houses the main applet.
 */

import java.awt.*;
import java.applet.Applet;
import java.net.*;
import java.io.*;


// class decleration of NCClient
public class NCClient extends Applet {

	URL host=null;
	String param;
  	NCWindow window; //the popup window to do the work
  	Button start;  //the button to pop up the window

	Image[] images;		//array of images to be loaded
	MediaTracker tracker;	//a media tracker to keep track of loading progress
	CPictButton start_button;		//start button
 
 public void showStatus(String m)
  { super.showStatus(m);
  System.out.println(m);
  }

// initialization routine
public void init(){
	String hostname=getParameter("HOST");

  if(hostname!=null) 
  { try {
  	host=new URL(hostname);
  	System.out.println("Host " + host);
  	} 
  	catch (MalformedURLException e)
  	{ System.out.println("Malformed URL " + e);
  	
  }}

  if(host==null) { host=getCodeBase(); }
	tracker = new MediaTracker(this);

	images = new Image[17];				// set up array for 13 images
	for(int i=0; i<15; i++) {
	  String im = "image"+i+".gif";
		showStatus("Loadimg image "+im);
		images[i] = getImage(getDocumentBase(),im);
		tracker.addImage(images[i],i);
	}
	images[15] = getImage(getDocumentBase(), "main_up.gif");
	tracker.addImage(images[15],15);
	images[16] = getImage(getDocumentBase(), "main_dn.gif");
	tracker.addImage(images[16],16);

	for(int i=0; i<17; i++) {
		showStatus("Loading image " + images[i]);
		try {
			tracker.waitForID(i); 
		} 
		catch (InterruptedException e)
			{
			showStatus("Image loading failed for " + images[i]);
		}
	}
		showStatus("Images fully loaded");

  	setLayout(new GridLayout(1,1));  //add one button
  	add(start_button = new CPictButton(images[15], images[16], null));
}

public boolean mouseUp (Event evt, int x, int y) {
    boolean statusflag;
	int index;

    if (evt.target == (Component)start_button) {
      	window = new NCWindow(host,8432,images);  //set up the window
    	window.start();  
    	window.show();  
    	return true;
    }
	return false;
}



}