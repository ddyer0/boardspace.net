package bridge;

import lib.Graphics;
import lib.Image;

@SuppressWarnings("serial")
public class JFrame extends javax.swing.JFrame
{	public JFrame(String name) 
	{ 
	  super(name); 
	}
	public JFrame() 
	{ super(); 
	}
	public Image getIconAsImage() 
	{	java.awt.Image im = super.getIconImage();
		return(im==null ? null : Image.createImage(im,"frame icon")); 
	}
	public void setIconAsImage(Image im) { setIconImage(im==null ? null : im.getImage()); }
	public void paint(Graphics g) 
	{ 	super.paint(g.getGraphics()); 
	}
	public void setOpaque(boolean v)
	{	// don't do anything
	}
	/*
	public void update(Graphics g)
	{ super.update(g); 
	}
	*/
	
}
