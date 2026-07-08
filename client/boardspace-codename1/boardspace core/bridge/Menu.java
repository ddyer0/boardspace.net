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


import java.util.Hashtable;

import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.list.DefaultListCellRenderer;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;

import lib.FontManager;
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
		  setWidth(im.getSystemImage().getWidth());
		  setHeight(im.getSystemImage().getHeight());
		  Style s = getStyle();
		  s.setBorder(Border.createEmpty());
		}
	public Dimension getPreferredSize() { return(new Dimension(image.getSystemImage().getWidth(),image.getSystemImage().getHeight())); }
	public void paint(Graphics g) 
		{ 	g.setClip(getX(),getY(),getWidth(),getHeight());
			g.drawImage(image.getSystemImage(),getX(),getY()); 
		}
}
// renderer extension that knows about our item types
class BSrenderer<T> extends DefaultListCellRenderer<T>
{	private bridge.Component parent = null;
	public BSrenderer (bridge.Component pa) 
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

@SuppressWarnings("rawtypes")
public class Menu extends JMenuItem implements ActionListener,SizeProvider,NativeMenuInterface
{	private MouseAdapter mouse = null;
	private String title = null;
	public MouseAdapter getMouse() 
	{
		if(mouse==null) { mouse = new MouseAdapter(getMenu()); }
		return(mouse);
	}
	private ComboBox internalmenu =  null;
	private Hashtable<Object,JMenuItem> internalitems = null;
	public Hashtable<Object,JMenuItem>getItems() 
	{ 	if(internalitems== null)
		{
		internalitems = new Hashtable<Object,JMenuItem>();
		}
		return internalitems;
	}
	
	public ComboBox getMenu() 
	{
		if(internalmenu==null) 
			{ String et = title==null ? "" : title;
			  ComboBox menu = internalmenu = new ComboBox();
			  JMenuItem m = new JMenuItem(et);
			  m.setIsLabel(true);
			  Font font = m.getFont();
			  Font de = FontManager.deriveFont(font,FontManager.getFontSize(font)+2,FontManager.BOLD);
			  m.setFont(de);
			  add(m);

			  BSrenderer<JMenuItem> bsRender = new BSrenderer<JMenuItem>(this);
			  bsRender.setShowNumbers(false);
			  setBackground(new Color(0xccccff)/*menu.getBackground()*/);
			  setForeground(menu.getForeground());
			  menu.setRenderer(bsRender);
			}
		return(internalmenu);
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
		title = msg;
		finishInit();
	}
	public int getComponentIndex(JMenu m) 
	{	Object impl = m.getImplementation();
		int count = getItemCount();
		for(int n = 0;n<count;n++)
		{
			Object e = getItem(n);
			if(e==impl) { return n; }
		}
		return -1;
	}
	private com.codename1.ui.Container  showingOn = null;
	private int showingX;
	private int showingY;
	public com.codename1.ui.Container  getShowingOn() { return(showingOn); }
	public void setShowingOn(com.codename1.ui.Container  on) { showingOn = on; }
	public int getWidth()
	{
		return((internalmenu!=null) ? internalmenu.getWidth() : 100);
	}
	public int getHeight()
	{
		return((internalmenu!=null) ? internalmenu.getHeight() : 100);
	}

	private void showMenu(com.codename1.ui.Component parent ,Menu jMenu,int atX,int atY)
	{	
		try {
		//showing = true;
		  MasterForm form = MasterForm.getMasterForm();
		  int ax = parent.getAbsoluteX();
		  int ay = parent.getAbsoluteY();
		  ComboBox m = getMenu();
		 // m.setShouldCalcPreferredSize(true);
		 // m.calcPreferredSize();
		  Dimension menuSize = m.getPreferredSize();
		  showingOn = form;
		  showingX = Math.max(0,Math.min(ax+atX,form.getWidth()-G.Width(menuSize)));
		  showingY = Math.max(G.minimumFeatureSize(),Math.min(ay+atY,form.getHeight()-G.Height(menuSize)));
		 // menu.setSize(menuSize);
		 // G.print("Menu "+menu.getX()+" "+menu.getY() +" "+ menu.getWidth()+" "+menu.getHeight());
		  MouseAdapter ma = getMouse();
		  m.addActionListener(ma);
		  ma.addActionListener(jMenu);
		  form.removeComponent(m);	// just in case
		  form.add(m);
		  m.setX(showingX);
		  m.setY(showingY);
		  m.setWidth(menuSize.getWidth());
		  m.setHeight(menuSize.getHeight());
		  m.setVisible(true);
		  m.pointerReleased(showingX,showingY);
		  form.removeComponent(m);  
		  showingOn = null; 
		}
    	catch (ThreadDeath err) { throw err;}
		catch (NullPointerException err) { showingOn = null; }	// this happens rarely when the menu pops down unexpectedly
		catch (Throwable err)
		{
			 Http.postError(this,"showing menu",err); 
		}
	}
	public void show(Component par,int xx,int yy) throws AccessControlException
	{
		show(par instanceof com.codename1.ui.Container ? (com.codename1.ui.Container)par : par.getParent(),xx,yy);
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
	{	Object item = getMenu().getSelectedItem();
		return(item instanceof JMenuItem ? (JMenuItem)item : null);
	}
	public int getSelectedIndex()
	{
		return(getMenu().getSelectedIndex());
	}
	
	public void actionPerformed(ActionEvent evt)
	{
		Object ob = evt.getSource();
		ComboBox m = getMenu();
		com.codename1.ui.Container so = showingOn;
		// showingOn can be null if multiple events are generated
		// and the first one has completed
		if(ob==m && so!=null)
		{
			JMenuItem item = m.getSelectedItem();
			int index = m.getSelectedIndex();
			if(item!=null)
			{
			
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
			int x = evt.getX();
			int y = evt.getY();
			ItemEvent itemEvent = new ItemEvent(item,x,y);
			item.actionPerformed(evt);
			item.handleItemEvent(itemEvent); 
			ActionEvent itemAction = new ActionEvent(item,showAtX,showAtY);
			item.handleActionEvent(itemAction);
			}
			
			m.setSelectedIndex(0);
			}
		}
		handleActionEvent(evt);
	}

	public void add(JMenuItem b)
		{ JMenuItem imp = b.getImplementation();
		  ComboBox m = getMenu();
		  m.addItem(imp); 
		  getItems().put(imp,b);
		  MouseAdapter ma = getMouse();
		  m.addActionListener(ma);
		  ma.addActionListener(this);
		}
	
	public void remove(JMenuItem m) 
	{ 	// List doesn't expose a removeItem method
		ListModel<JMenuItem> model = getMenu().getModel();
		Object impl = m.getImplementation();
		for(int size=model.getSize()-1; size>=0; size--)
		{	if(model.getItemAt(size)==impl)
				{ model.removeItem(size);
				  getItems().remove(impl);
				  break;
				}

		}
	}
	
	
	public void removeAll() 
	{ 
		// List doesn't expose a removeItem method
		ListModel<JMenuItem> model = getMenu().getModel();
		for(int size=model.getSize()-1; size>=1; size--)
		{	model.removeItem(size); 
		} 
		getItems().clear();
	}
	public JMenuItem getItem(int i) 
	{ 	Object m = getMenu().getModel().getItemAt(i);
		JMenuItem got = getItems().get(m);
		return(got); }

	public int getItemCount() { return(getMenu().getModel().getSize());  }
	
	public void setVisible(boolean b) 
	{ getMenu().setVisible(b); 
	}
	public void paint(Graphics g) {
		getMenu().paint(g);
	}
	
	public NativeMenuItemInterface getMenuItem(int n) { return((NativeMenuItemInterface)getItem(n)); }

	public void show(Object window, int x, int y) throws AccessControlException {
		final Menu jMenu = this;
		 final com.codename1.ui.Component parent = (com.codename1.ui.Component)window;
		 final int atX = x;
		 final int atY = y;
		 Runnable m = new Runnable() 
		 	{ 
		 	  public void run() {
		 	  showMenu(parent,jMenu,atX,atY); 
		 	}};
		 G.runInEdt(m);
	}

	public void hide(Component par)
	{   com.codename1.ui.Container p = par.getParent();
		if(p!=null) { p.removeComponent(getMenu()); }
	}

	public NativeMenuInterface getSubmenu() { return(this); }
	
}
