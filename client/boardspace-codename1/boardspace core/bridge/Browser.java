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

import com.codename1.ui.BrowserComponent;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.BrowserNavigationCallback;
import lib.G;
import lib.Http;
import lib.XFrame;
/**
 * this hack implements a minimal but pretty satisfactory browser
 * all in 1 page!
 * 
 * @author ddyer
 *
 */
@SuppressWarnings("rawtypes")
class BC extends BrowserComponent implements ActionListener,com.codename1.ui.events.ScrollListener
{	
	public BC()
	{
		super();
		addPointerPressedListener(this);
		addPointerReleasedListener(this);
		addPointerDraggedListener(this);
		addScrollListener(this);
	}
	public void paint(com.codename1.ui.Graphics g)
	{	
		try {
			super.paint(g);
			//g.setColor(0xff);
			//g.drawLine(0,0,getWidth(),getHeight());
		}
		catch (Throwable e)
		{
			G.print("Error in browser paint ",g,"\n",e.getStackTrace());
		}
	}
    public void waitForReady()
    {
    	//G.print("Waitfor ready ",G.isEdt());
    }

	public void onError(String msg,int code)
	{
		G.print("error ",code,"\n",msg);
	}

	public void actionPerformed(ActionEvent evt) {
		//G.print("Action ",evt.getEventType()," ",evt);
		
	}
	public void scrollChanged(int scrollX, int scrollY, int oldscrollX, int oldscrollY) {
		//G.print("Scrollchanged ",scrollX," ",scrollY," from ",oldscrollX," ",oldscrollY);
	}
}
@SuppressWarnings("rawtypes")
public class Browser extends XFrame implements BrowserNavigationCallback,ActionListener,Runnable
{
	static final String OK = "Ok";
	static final String BACK = "Back";
	boolean deferSetUrl = false;
	boolean FIX_HTTP = false;	// this depends on the build hint android.xapplication_attr=android:usesCleartextTraffic="true"
	BrowserComponent b = new BC();
	JPanel top = new JPanel();
	XTextField url = new XTextField(120);
	JButton back = new JButton("Back");
	public Browser(String name,String initialUrl)
	{
		super(name);
		setEnableRotater(false);
    	setLayout(new BorderLayout());
		addC(BorderLayout.CENTER,b);
		BoxLayout bl = new BoxLayout(top,BoxLayout.X_AXIS);
		top.addC(back);
		top.addC(url);
		top.setLayout(bl);
	    url.setActionCommand(OK);
		url.addActionListener(this);
		back.setActionCommand(BACK);
		back.addActionListener(this);
				
		addC(BorderLayout.NORTH,top);
		if(G.debug()) { b.setDebugMode(true); }
		url.setText(initialUrl);
		url.setEditable(true);
		url.setVisible(true);
		back.setVisible(true);
		//
		// this is a complete hack to paper over android's apparent inability
		// to open pdfs - open it in google docs
		//
		String eventualUrl = reWrite(initialUrl);
		
		b.setURL(eventualUrl);
		b.setPinchToZoomEnabled(true);
		b.addBrowserNavigationCallback(this);
		b.setVisible(true);
		setVisible(true);
		new Thread(this).start();
	}
	public static String GoogleRewrite = "https://docs.google.com/gview?embedded=true&url=";

	public String reWrite(String u)
	{	String initialUrl = u;
		if(FIX_HTTP && initialUrl.toLowerCase().startsWith("http://")) 
		{
			initialUrl = "https"+initialUrl.substring(4);
		}
		if(G.isAndroid() 
				&& !G.isSimulator() 
				&& initialUrl.endsWith(".pdf") 
				&& !initialUrl.startsWith(GoogleRewrite))
		{	// the native android browser doesn't support .pdf files.  This works pretty well,
			// at least for relatively small pdf files found in rules.
			initialUrl =  GoogleRewrite+initialUrl;
		}
		return initialUrl;			
	}
	private String newUrlText = null;
	private String newUrlSet = null;
	
	public boolean shouldNavigate(String urlS) {
		try {
		String rr = reWrite(urlS);
		//
		// note here - to avoid EDT violation, setText has to be run in edt.  But in this context
		// we are already in edt and waiting for completion, so trying to switch to edt will cause
		// a deadlock.  We paper over this by deferring the setText to the per-frame process.
		//
		newUrlText = urlS;
		if(rr == urlS) { 
			return true; 
			}
		if(deferSetUrl) 
			{ 
			newUrlSet = urlS;
			G.wake(this);
			return false; 
			}
			else
			{
			b.setURL(rr);
			return true;
			}
		}
		catch(Throwable err)
		{
			Http.postError(this,"error in browser action",err);
		}
		G.wake(this);
		return false;
	}
	private ActionEvent event = null;
	
	public void actionPerformed(ActionEvent evt) {
		event = evt;
		G.wake(this);
		//G.print("event "+evt);
		//processAction(evt);
	}
	private void processAction(ActionEvent evt)
	{
		// source will be a "Command" object which prints as the original string we fed in.
		try {
		Object source = evt.getSource();
		if(OK.equals(""+source)||(source==url))
			{	String target = url.getText();
				//G.print("Rewrite use ",target);
				b.setURL(reWrite(target));
				b.repaint();
			}
			else if(BACK.equals(""+source))
			{
				b.back();
				String ne = b.getURL();
				//G.print("Back to ",ne);
				url.setText(ne);
				
			}
			else 
				{ G.print("unexpected browser action: "+source+" "+evt);
				}
		}
		catch (Throwable err)
		{	Http.postError(this,"error in browser action",err);
		}
		}
	private boolean exit = false;
	public void killFrame()
	{
		exit = true;
	}
	public void run()
	{
		while(!exit)
		{	ActionEvent e = event;
			event = null;
			if(e!=null) { processAction(e); }
			
			// change the url
			String set = newUrlSet;
			newUrlSet = null;
			if(set!=null) { b.setURL(set); }
			
			// change the text line
			String txt = newUrlText;
			newUrlText = null;
			if(txt!=null) { url.setText(txt); }
			repaint();
			G.timedWait(this,1000);
		}
		G.print("exit browser");
	}
  
}
