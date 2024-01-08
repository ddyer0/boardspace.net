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

import com.codename1.ui.geom.Dimension;
import lib.G;
import lib.Graphics;
import lib.MenuInterface;
import lib.MenuParentInterface;
import lib.NullLayout;
import lib.NullLayoutProtocol;

import com.codename1.ui.Component;

public class FullscreenPanel extends JPanel implements FullScreen,NullLayoutProtocol,MenuParentInterface
{
	public FullscreenPanel() 
	{ setOpaque(false);
	  setLayout(new NullLayout(this)); 
	}
	public void doNullLayout()
	{
		setLocalBounds(0,0,getWidth(),getHeight());
	}

	public void addC(Component p)
	{
		add(p);
	}
	
	//
    // these are still used by Tantrix. 
	// get here when calling back to paint the components of a container
	// the affected windows are the main lobby, the main game frame, and the chat applet
	// lobby and game are containers with no contents, the chat, if present, is a container
	// with contents.
	//
	public void actualPaint(Graphics g)
	{
		int y = getY();
		int x = getX();
		//
		// codename1 containers do not have the x,y applied.  They're added
		// when flipping to lib.Graphics paint, and have to be removed here.
		//
		g.translate(-x, -y);
		super.paint(g.getGraphics());
		g.translate(x, y);
		
	}
	public void paint(com.codename1.ui.Graphics g)
	{
		//
		// this is probably not completely correct. Paint in Codename1 containers is
		// called with the X,Y of the container not included in g, so where we break
		// from codename1, we have to apply it to meet the usual expectation.  This
		// is important for tantrix lobby, where the lobby is a 
		int y = getY();
		int x = getX();
		g.translate(x, y);
		paint(lib.Graphics.create(g));
		g.translate(-x, -y);
	}
	public void paint(lib.Graphics g)
	{
		actualPaint(g);
	}
	public void setLocalBounds(int l,int t,int w, int h)
	{	
		for(int nc = getComponentCount()-1 ; nc>=0; nc--)
		{
			Component c = getComponentAt(nc);
			int cw = c.getWidth();
			int ch = c.getHeight();
			if((c instanceof FullScreen)
					&& ((cw!=w)||(ch!=h)))
			{	Dimension minSz = ((FullScreen)c).getMinimumSize();
				int aw = Math.max(minSz.getWidth(),w);
				int ah = Math.max(minSz.getHeight(),h);
				((FullScreen)c).setBounds(0, 0, aw, ah);
			}
		}
	}
	public void show(MenuInterface menu, int x, int y) throws AccessControlException 
	{
		G.show(this, menu, x, y);
	}
	

	public void windowOpened(WindowEvent e) {
		
	}


	public void windowClosing(WindowEvent e) {
		
	}

	public void windowClosed(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
		
	}

	public void windowDeiconified(WindowEvent e) {
	
	}

	public void windowActivated(WindowEvent e) {
	
	}

	public void windowDeactivated(WindowEvent e) {
		
	}
	
	public void setFocused(Component p)
	{
		MasterForm.getMasterForm().setFocused(p);
	}
	
	/*
	public void update(java.awt.Graphics g)
	{	System.out.println("Update "+this);
	
	}
	public void paintComponent(java.awt.Graphics g)
	{
		System.out.println("PaintComponent "+this);
	}
	*/
}
