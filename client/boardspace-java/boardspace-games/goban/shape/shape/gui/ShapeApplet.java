package goban.shape.shape.gui;

import java.net.*;
import java.applet.Applet;
import java.awt.Dimension;
import lib.Image;
import java.io.*;
import lib.G;
import lib.NullLayout;
import lib.NullLayoutProtocol;

@SuppressWarnings("deprecation")
public class ShapeApplet extends Applet implements NullLayoutProtocol
{
	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	runner myrunner=null;
	Thread mythread=null;
	PrintStream myconsole=null;
	public void init()
	{
		String nameparm=getParameter("library");
		String sample_nameparm=getParameter("sample-library");
		String lispformat=getParameter("lispformat");
		String background_name = getParameter("background");
		boolean islisp = (lispformat!=null) ? Boolean.parseBoolean(lispformat) : false;

		{ShapePanel p = new ShapePanel();
			Dimension dim = this.getSize();
			p.setSize(G.Width(dim),G.Height(dim));
			setLayout(new NullLayout(this));		//get the flow layout manager out of the way
			
			try {
				URL looking = new URL(getCodeBase(),background_name);
				Image background = (background_name!=null) 
					? Image.createImage(getImage(looking))
					: null; 
				this.add(p);    
				myconsole = System.out;
				
				myrunner = new runner(p,
					nameparm!=null ? new URL(getDocumentBase(),nameparm):null,
					sample_nameparm!=null ? new URL(getDocumentBase(),sample_nameparm):null,
					islisp,myconsole,background);
				mythread = new Thread(myrunner);
				mythread.start();
			}
			catch(MalformedURLException e)
			{
				myconsole.println("Malformed URL " + e);
			}}
		
	}
	public void doNullLayout() {	}

	static final void main(String args[])
	{
		
	}
}
