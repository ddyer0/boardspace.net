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

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;

import javax.swing.BoxLayout;
import javax.swing.JMenuBar;

import lib.Graphics;
import lib.G;
import lib.Http;
import lib.SizeProvider;

@SuppressWarnings("serial")
public class MasterForm extends JFrame {

	/**
	 * 
	 */
	private static MasterForm masterForm = null;
	private static MasterPanel masterPanel = null;
	private String appname = "";
	private Container tabs = new Container();
	private Container menus = new Container();
	private Container centers = new Container();
	private JMenuBar titleBar = null;
	//Spacer spacer = new Spacer();
	public MasterForm(String app) 
	{
		super(app);
		new BoxLayout(this,BoxLayout.Y_AXIS);
		appname = app; 
		new BoxLayout(this,BoxLayout.Y_AXIS);
		tabs.setLayout(new TabLayout());
		menus.setLayout(new TabLayout());
		centers.setLayout(new TabLayout());
		titleBar =new JMenuBar();
		setJMenuBar(titleBar);
	
		new BoxLayout(titleBar,BoxLayout.X_AXIS);
		  
		titleBar.add("West",tabs);
		titleBar.add("East",menus);
		titleBar.add("Center",centers);  

	}

	public void addToMenus(JButton m)
	{	
		masterForm.getMenus().add(m);
	}
	
    public Rectangle getSafeArea() {
        
        return new Rectangle(0,0,G.getScreenWidth(),G.getScreenHeight());
    }
	public static Component getMyChildContaining(Component p,Component c)
	{	if(p==c) { return(null); }
		Component par = c.getParent();
		if(par==p) { return(c); }
		else { if(par==null) { return(null); }
			   return(getMyChildContaining(p,par));
		}
	}
	public Container getTabs() { return(tabs); }
	public Container getMenus() { return(menus); }

	public static MasterForm getMasterForm()
	{
		if(masterForm==null) { masterForm=new MasterForm(Config.APPNAME); }
		return(masterForm);
	}
	private void addMasterPanel()
	{	try {
	  if(masterPanel==null)
		  {MasterPanel mp = masterPanel = new MasterPanel(this);
		  add(mp);
		  mp.setVisible(true);
		  masterPanel = mp;
		  }
		}
		catch (Throwable err) { Http.postError(this,"adding master panel",err); }
	}
	public static MasterPanel getMasterPanel()
	{	final MasterForm form = getMasterForm();
		if(masterPanel==null)
			{
			G.runInEdt(new Runnable() { public void run() { form.addMasterPanel(); }});
			}
		  return(masterPanel);
	}


	private int globalRotation=0;
	public static int getGlobalRotation() { return(getMasterForm().globalRotation); }
	public static void setGlobalRotation(int n) {
		MasterForm form = getMasterForm();
		form.globalRotation = n&3;
    }

    /* support for rotating windows */
	public static int translateX(SizeProvider client,int x) 
	{ return( ((getGlobalRotation())&2)!=0
				? client.getWidth()-x
				: x);
	}
	public static int translateY(SizeProvider client,int y) 
	{ return( ((getGlobalRotation())&2)!=0
				? client.getHeight()-y
				: y);
	}


	public static boolean canRepaintLocally(Graphics g) { return(true); }	


}
