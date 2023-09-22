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


import java.util.Vector;

import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.list.DefaultListCellRenderer;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;

import lib.AwtComponent;
import lib.G;
import lib.Http;
import lib.Image;
import lib.NativeMenuInterface;
import lib.NativeMenuItemInterface;
import lib.SizeProvider;

class SimpleImage extends Component
{
	Image image;
	SimpleImage(Image im) 
		{ image = im; 
		  setWidth(im.getImage().getWidth());
		  setHeight(im.getImage().getHeight());
		  Style s = getStyle();
		  s.setBorder(Border.createEmpty());
		}
	public Dimension getPreferredSize() { return(new Dimension(image.getImage().getWidth(),image.getImage().getHeight())); }
	public void paint(Graphics g) 
		{ 	g.setClip(getX(),getY(),getWidth(),getHeight());
			g.drawImage(image.getImage(),getX(),getY()); 
		}
}
// renderer extension that knows about our item types
class BSrenderer<T> extends DefaultListCellRenderer<T>
{	private AwtComponent parent = null;
	public BSrenderer (AwtComponent pa) 
	{ parent = pa; 
	}
    public Component getCellRendererComponent(Component list, Object model, T value, int index, boolean isSelected)
    	{
    	//setShowNumbers(false);
    	// handle our menu items represented by icons
    	if(value instanceof Icon)
    	{	Icon im = (Icon)value;
    		SimpleImage l = new SimpleImage(Image.getIconImage(im,parent));
    		return(l);
    	}
    	else
    	if(value instanceof JMenuItem)
    	{	JMenuItem jm = (JMenuItem)value;
    		Image im = jm.getImage(parent);
    		return new SimpleImage(im);
    	}

    	// default, do whatever
    	Component c = super.getCellRendererComponent(list,model,value,index,isSelected);
    	return(c);
    	}
}

public class Menu extends JMenuItem implements ActionListener,SizeProvider,NativeMenuInterface
{	private MouseAdapter mouse = null;
	public MouseAdapter getMouse() 
	{
		if(mouse==null) { mouse = new MouseAdapter(getMenu()); }
		return(mouse);
	}
	private ComboBox<JMenuItem> menu =  null;
	
	public ComboBox<JMenuItem> getMenu() 
	{
		if(menu==null) 
			{ menu = new ComboBox<JMenuItem>();
			  BSrenderer<JMenuItem> bsRender = new BSrenderer<JMenuItem>(this);
			  bsRender.setShowNumbers(false);
			  setBackground(new Color(0xccccff)/*menu.getBackground()*/);
			  setForeground(menu.getForeground());
			  menu.setRenderer(bsRender);
			}
		return(menu);
	}
	private Vector<JMenuItem>items = null;
	public Vector<JMenuItem>getItems()
	{
		if(items==null) { items = new Vector<JMenuItem>(); }
		return(items);
	}
	public boolean isTreeMenu() { return(getItemCount()>0); }
	public boolean getTopLevel() { return(isTreeMenu()); }
	// constructors
	public Menu() 
		{ super();
		  finishInit();
		}
	public Menu(String msg) 
	{ 	super(msg);
		finishInit();
	}
	public int getComponentIndex(JMenu m) 
	{
		Vector<JMenuItem> v = getItems();
		return v.indexOf(m);
	}
	private com.codename1.ui.Container  showingOn = null;
	private int showingX;
	private int showingY;
	public com.codename1.ui.Container  getShowingOn() { return(showingOn); }
	public void setShowingOn(com.codename1.ui.Container  on) { showingOn = on; }
	public int getWidth()
	{
		return((menu!=null) ? menu.getWidth() : 100);
	}
	public int getHeight()
	{
		return((menu!=null) ? menu.getHeight() : 100);
	}

	private void showMenu(com.codename1.ui.Container parent ,Menu jMenu,int atX,int atY)
	{	
		try {
		//showing = true;
		  ComboBox<JMenuItem>m = getMenu();
		  m.calcPreferredSize();
		  Dimension menuSize = m.getPreferredSize();
		  showingOn = parent;
		  showingX = Math.max(0,Math.min(atX,parent.getWidth()-G.Width(menuSize)));
		  showingY = Math.max(G.minimumFeatureSize(),Math.min(atY,parent.getHeight()-G.Height(menuSize)));
		  m.setX(showingX);
		  m.setY(showingY);
		 // menu.setSize(menuSize);
		 // G.print("Menu "+menu.getX()+" "+menu.getY() +" "+ menu.getWidth()+" "+menu.getHeight());
		  MouseAdapter ma = getMouse();
		  parent.removeComponent(m);	// just in case
		  parent.add(m);
		  m.addActionListener(ma);
		  ma.addActionListener(jMenu);
		  
		  m.setVisible(true);
		  m.pointerReleased(showingX,showingY);
		  parent.removeComponent(m);  
		  showingOn = null; 
		}
    	catch (ThreadDeath err) { throw err;}
		catch (NullPointerException err) { showingOn = null; }	// this happens rarely when the menu pops down unexpectedly
		catch (Throwable err)
		{
			 Http.postError(this,"showing menu",err); 
		}
	}
	public void show(com.codename1.ui.Container par, int xx, int yy) throws AccessControlException
		{final Menu jMenu = this;
		 final com.codename1.ui.Container parent = par;
		 final int atX = xx;
		 final int atY = yy;
		 G.runInEdt(new Runnable() { public void run() { showMenu(parent,jMenu,atX,atY); }});
		}
	

	private void finishInit()
	{	MouseAdapter ma = getMouse();
		getMenu().addActionListener(ma); 
		ma.addActionListener(this);
	};
	public JMenuItem getSelectedItem()
	{
		return((JMenuItem)(getMenu().getSelectedItem()));
	}
	public int getSelectedIndex()
	{
		return(getMenu().getSelectedIndex());
	}
	
	public void actionPerformed(ActionEvent evt)
	{
		Object ob = evt.getSource();
		ComboBox<JMenuItem>m = getMenu();
		com.codename1.ui.Container so = showingOn;
		// showingOn can be null if multiple events are generated
		// and the first one has completed
		if(ob==m && so!=null)
		{
			Object impl = m.getSelectedItem();
			int index = m.getSelectedIndex();
			if(index>=0)
			{
			JMenuItem item = getItems().elementAt(index);
			if(item!=null)
			{
			Object itemImpl = item.getImplementation();
			G.Assert(index==0 || impl==itemImpl,"item mismatch, is %s expected %s",impl,itemImpl);
			
			if(item.isTreeMenu())	// item zero is the original menu head
			{
			// pop the next item in the tree
			if(index>0 || getTopLevel())	// item 0 is the tree header
			{
			try {
			int x = evt.getX()+showingX;
			int y = evt.getY()+showingY;
			item.show(so,x,y);
			} catch (AccessControlException|NullPointerException e) {}
			}}
			else
			{
			ItemEvent itemEvent = new ItemEvent(item,evt.getX(),evt.getY());
			item.actionPerformed(evt);
			item.handleItemEvent(itemEvent); 
			ActionEvent itemAction = new ActionEvent(item,item.toString(),showAtX,showAtY);
			item.handleActionEvent(itemAction);
			}
			
			m.setSelectedIndex(0);
			}}
		}
		handleActionEvent(evt);
	}

	public void add(JMenuItem b)
		{ JMenuItem imp = b.getImplementation();
		  ComboBox<JMenuItem>m = getMenu();
		  m.addItem(imp); 
		  getItems().addElement(b);
		  MouseAdapter ma = getMouse();
		  m.addActionListener(ma);
		  ma.addActionListener(this);
		}
	
	public void remove(JMenuItem m) 
	{ 	// List doesn't expose a removeItem method
		ListModel<JMenuItem> model = menu.getModel();
		for(int size=model.getSize()-1; size>=0; size--)
		{	if(model.getItemAt(size)==m.getImplementation())
				{ model.removeItem(size);
				getItems().remove(size);
				}

		}
	}
	
	public void remove(int n) {
		ListModel<JMenuItem> model = getMenu().getModel();
		model.removeItem(n);
		getItems().remove(n);		
	}
	
	public void removeAll() 
	{ 
		// List doesn't expose a removeItem method
		ListModel<JMenuItem> model = getMenu().getModel();
		for(int size=model.getSize()-1; size>=0; size--)
		{	model.removeItem(size); 
			getItems().remove(size);
		} 
	}
	public JMenuItem getItem(int i) { return(getItems().elementAt(i)); }

	public int getItemCount() { return(getMenu().getModel().getSize());  }
	
	public void setVisible(boolean b) 
	{ getMenu().setVisible(b); 
	}
	public void paint(Graphics g) {
		getMenu().paint(g);
	}
	
	public NativeMenuItemInterface getMenuItem(int n) { return((NativeMenuItemInterface)getItem(n)); }

	public void show(bridge.Component window, int x, int y) throws AccessControlException {
		final Menu jMenu = this;
		 final com.codename1.ui.Container parent = (com.codename1.ui.Container)window;
		 final int atX = x;
		 final int atY = y;
		 Runnable m = new Runnable() 
		 	{ 
		 	  public void run() {
		 	  showMenu(parent,jMenu,atX,atY); 
		 	}};
		 G.runInEdt(m);
	}
	public void hide(bridge.Component par)
	{
		par.removeComponent(getMenu());
	}

	public NativeMenuInterface getSubmenu() { return(this); }
	
}
