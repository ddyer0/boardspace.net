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
import com.codename1.ui.Font;

import bridge.*;
import java.util.Hashtable;

//
// 1/2008 some tweaks to allow non integer values
//
/**
 * this is a replacement class to manage pop up menus.
 * call newPopupMenu() to clear and prepapre for a new pop-up.
 * then call addMenuItem for each item or submenu
 * then call show()
 * then call selectMenuTarget to get the integer index associated with leaf nodes
 * create hierarchical submenus by calling newSubMenu() and adding items to the submenu
 * instead of the main menu.
 * 
 * this class also addresses a nasty conundrum.  The awt based popup menu components do not support
 * languages than need non-roman alphabets well, but linux platforms have
 * a perpetual problem with swing-based menus relating to "setAlwaysOnTop"
 * results in a AccessControlException.  A second problem exists with swing
 * components - lightweight components are clipped behind windows and also
 * do not support non-roman fonts well.
 * After several attempts to square the circle, this version implements
 * a runtime ac/dc strategy which will default to using swing, but back up
 * to old components if that fails.

 */
public class PopupManager extends SimpleObservable implements ActionListener,DrawingObject
{
    // menu support.  
    //

	// on Windows, awt menus have problems with chinese and japanese
	// on Windows, swing menus have reliability problems due to multi threading
	// pick your poison. [ddyer 1/2023]
	// Note: 12/28/2023 cheerpj has problems with both flavors of menus.  AWT menus can overrun
	// the visible window (and are clipped), whereas Swing menus sometimes fail to 
	// appear at all.  Cheerpj was accidentally classified as a unix platform and so defaulted
	// to using awt, which works better.
	//
	// 1/2026 changed to swing on by default.  previous problems with linux seem to be resolved.
	// 
	public static boolean useSwing = true;// || !G.isUnix() || G.isCheerpj();	// default to using swing menus
	public static int menuErrors = 0; 
	public boolean useSimpleMenu = false;
	public boolean useSimpleMenu()
	   { 
		  return menuErrors>0 || useSimpleMenu || G.isUnix() || (G.isCheerpj() && G.isTouchInterface());
	   }
	// constructor
	private static boolean lightweightMenus = !G.isCodename1();
	
	public class bsSwingMenu implements MenuInterface
	{
		public boolean useSimpleMenu() 
		{ 			
		  return PopupManager.this.useSimpleMenu() || popupSwingMenu.useSimpleMenu();
		}
	public int getItemCount() { return popupSwingMenu.getItemCount(); }
	private int ncols = 0;
	public int getNColumns() { return ncols; }
	public void setNColumns(int n) { ncols = n; popupSwingMenu.setNColumns(n); }
	private JPopupMenu popupSwingMenu = null;
	public void setFont(Font f) { popupSwingMenu.setFont(f); }
	public Font getFont() { return popupSwingMenu.getFont(); }
	
	public NativeMenuInterface getNativeMenu() { return(popupSwingMenu); }
	public bsSwingMenu(String msg,Font f)
	{
		popupSwingMenu = new JPopupMenu(msg,f); 
		// just expetimentally, lightweight menus have fewer mystery 
		// crashes [ddyer 1/2023]
		popupSwingMenu.setLightWeightPopupEnabled(lightweightMenus);
	}
	public String toString() { return("<Box "+popupSwingMenu.toString()+">"); }
	   
	// add a leaf item
	public NativeMenuItemInterface add(Text item,DrawingObject parent,ActionListener listener)
	{	
		Icon ic = item.getIcon(parent);
		JMenuItem newitem = ic!=null 
					   ? new JMenuItem(ic,font)
					   : new JMenuItem(item.getString(),font);	// this is where we get an icon instead
		popupSwingMenu.add(newitem);
		newitem.addActionListener(listener);
		return(newitem);			   
	}
		   
	// add a leaf item
	public NativeMenuItemInterface add(String item,ActionListener listener)
	{
		JMenuItem newitem = new JMenuItem(item,font);
		popupSwingMenu.add(newitem);
		newitem.addActionListener(listener);
		return(newitem);
	}

	// add a submenu
	public void add(MenuInterface item)
	{   popupSwingMenu.add((JMenu)item.getNativeMenu());
	}

	public boolean isVisible() 
	{
		return(popupSwingMenu.isVisible());
	}

	public void show(MenuParentInterface parent, int x, int y) 
	{	
		int failed = 0;
		boolean succeeded = false;
    	while(!succeeded && (failed<=1))
    	{
    	try {
		parent.show(this, x, y);
		succeeded = true;
		}
		catch (AccessControlException err)
		{ if(useSwing)
		  {if(failed++ == 0)
			{
			lightweightMenus = !lightweightMenus;
			Plog.log.addLog("trying lightweight ",lightweightMenus," after ",err);
			popupSwingMenu.setLightWeightPopupEnabled(lightweightMenus);
			}
			else 
			{ Plog.log.addLog("use AWT menus after ",err);
			  useSwing = false; 
			}
		  }
		}
    	catch (Throwable err)
    	{
    		menuErrors++;
    		failed++;
    		Plog.log.addLog("unexpected swing menu error "+err+"\n"+G.getStackTrace(err));
    	}
    	}
	}
	public void setVisible(boolean b) 
	{
		popupSwingMenu.setVisible(b);
	}

	public MenuInterface newSubMenu(String msg)
	{	return(new bsSwingSubMenu(msg,font));
	}

	}
	   
	public class bsSwingSubMenu implements MenuInterface
	{
		   private JMenu jsubmenu = null;
		   public void setFont(Font f) { jsubmenu.setFont(f); }
		   public Font getFont() { return jsubmenu.getFont(); }
		   public int getItemCount() { return jsubmenu.getItemCount(); }
		   private int ncols = 0;
		   public int getNColumns() { return ncols; }
		   public void setNColumns(int n) { ncols = n; jsubmenu.setNColumns(n); }
		   public boolean useSimpleMenu() 
		   { 			
				
			   return PopupManager.this.useSimpleMenu() || jsubmenu.useSimpleMenu();
		   }
		   public NativeMenuInterface getNativeMenu() { return(jsubmenu); }
		   // constructor for submenus
		   public bsSwingSubMenu(String msg,Font f)
		   {
			   jsubmenu = new JMenu(msg,f);
		   }
	   
		   public String toString() { return("<Box "+jsubmenu.toString()+">"); }
		   
		   // add a leaf item
		   public NativeMenuItemInterface add(Text item,DrawingObject parent,ActionListener listener)
		   {	
			   Icon ic = item.getIcon(parent);
			   JMenuItem newitem = ic!=null 
					   ? new JMenuItem(ic)
					   : new JMenuItem(item.getString(),font);	// this is where we get an icon instead
			   jsubmenu.add(newitem);
			   newitem.addActionListener(listener);
			   return(newitem);			   
		   }
		   
		   // add a leaf item
		   public NativeMenuItemInterface add(String item,ActionListener listener)
		   {
			   JMenuItem newitem = new JMenuItem(item,font);
			   jsubmenu.add(newitem);
			   newitem.addActionListener(listener);
			   return(newitem);
		   }

		   // add a submenu
		   public void add(MenuInterface item)
		   {   jsubmenu.add((JMenu)item.getNativeMenu());
		   }

		public boolean isVisible() 
		{
			return(jsubmenu.isVisible());
		}
		public void show(MenuParentInterface parent, int x, int y) 
		{	// swing submenu
			G.Error("Can't show");			  
		}

		public void setVisible(boolean b) {
			jsubmenu.setVisible(b);
		}
		public MenuInterface newSubMenu(String msg)
		{	return(new bsSwingSubMenu(msg,font));
		}
	   }
	
   public class bsAwtMenu implements MenuInterface
   {
	   private PopupMenu awtPopupMenu = null;	
	   public void setFont(Font f) { awtPopupMenu.setFont(f); }
	   public Font getFont() { return awtPopupMenu.getFont(); }
	   public int getItemCount() { return awtPopupMenu.getItemCount(); }
	   private int ncols = 0;
	   public int getNColumns() { return ncols; }
	   public void setNColumns(int n) { ncols = n;awtPopupMenu.setNColumns(n); }
	   public boolean useSimpleMenu()
	   { 
		  return PopupManager.this.useSimpleMenu()
				|| awtPopupMenu.useSimpleMenu();

	   }
	   public NativeMenuInterface getNativeMenu() { return(awtPopupMenu); }
	   // constructor for submenus
	   public bsAwtMenu(String msg,Font f)
	   {
		   awtPopupMenu = new PopupMenu(msg,f);  
	   }
	   
	   // add a leaf item
	   public NativeMenuItemInterface add(Text item,DrawingObject parent,ActionListener listener)
	   {	
		   MenuItem newitem = new MenuItem(item.getString(),font);
		   awtPopupMenu.add(newitem);
		   newitem.addActionListener(listener);
		   return(newitem);
	   }
	   
	   // add a leaf item
	   public NativeMenuItemInterface add(String item,ActionListener listener)
	   {
		   MenuItem newitem = new MenuItem(item,font);
		   awtPopupMenu.add(newitem);
		   newitem.addActionListener(listener);
		   return(newitem);
	   }

	   // add a submenu
	   public void add(MenuInterface item)
	   {   awtPopupMenu.add((PopupMenu)item.getNativeMenu()); 
	   }
	   public String toString() { return("<Box "+awtPopupMenu.toString())+">"; }

	   public boolean isVisible() {
		   return false;
	   }
	   public void show(MenuParentInterface parent, int x, int y) 
	   {
		try {
		parent.show(this,x,y);		
		}
		catch (AccessControlException err)
		{
			System.out.println("menu "+err);
		}
		catch (Throwable err)
		{
			menuErrors++;
     		Plog.log.addLog("unexpected awt menu error "+err+"\n"+G.getStackTrace(err));
		}
	   }
	  public void setVisible(boolean b) 
	  {
	  }
	  public void setLightWeightPopupEnabled(boolean b) 
	  {
	  } 
		public MenuInterface newSubMenu(String msg)
		{	return(new bsAwtMenu(msg,font));
		}
  
   }


   private Hashtable<NativeMenuItemInterface,Object> popupTarget=null;
   private ActionListener listener=null;
   private SimpleObserver observer = null;
   private MenuParentInterface parent = null;
   private MenuInterface menu = null;					// the actual menu if swing components
   
   private static Object nullValue=new Object();	// make it an object rather than a string
   public int showAtX = 0;
   public int showAtY = 0;
   // some comparisons won't seem to be ==String
   public int value = -1;
   public Object rawValue=null;
   // constructor
   public PopupManager()
   {	
   }
   public void setNColumns(int n)
   {
	   menu.setNColumns(n);
   }
   /**
    * show the menu at x,y
    * @param x
    * @param y
    */
    public void show(int x,int y)
    {	
  
    	MenuInterface jm = menu;
    	showAtX = x;
    	showAtY = y;
    	if(jm!=null) 
    	{
    		// show a swing menu, try lightweight if it fails, try traditional menus if
    		// it fails twice.
			boolean succeeded = false;
	    	int failed = 0;

	    	while(!succeeded && (failed<=1))
	    	try 
	    	{ 
	    	  jm.show(parent,x,y); 
	    	  succeeded = true;
	    	}
	    	catch (ArrayIndexOutOfBoundsException err) 
	    		{ G.print("error in java menu "+err);// shouldn't happen, but it does
	    		  menuErrors++;
	    		  jm.setVisible(false);
	    		}	
    	}
   }

  /**
   * call this from actionEvent of the listener associated with this menu.
   * @param otarget will be a menu from actionEvent.getTarget()
   * @return true if this is our menu item. IE if this action occurred. If so, rawValue will
   * be the original object associated with the menu item.
   */
    public boolean selectMenuTarget(Object otarget)
	  {	if(menu!=null)
		{Hashtable<NativeMenuItemInterface,Object> h = popupTarget;
		 if(h!=null)
			 { Object v =h.get(otarget);
			 if(v!=null)
				 {popupTarget = null;
				  menu = null;
				 }
			 if(v instanceof Integer) 
		  		{ value = ((Integer)v).intValue();
		  		}
		  		else 
		  		{  if(nullValue==v) { rawValue=null; }
		  		   else { rawValue = v; }
		  		  value=-1; 
		  		}
		  	return(v!=null); 
			 }
		}
		 return(false);
	  }
    /**
     * @return true if the pop-up menu is currently visible
     */
    public boolean isShowing()
    {	if(menu!=null) { return(menu.isVisible()); }
    	return(false);
    }
    /**
     * add a menu item with an associated integer
     * @param item
     * @param v
     */
    public void addMenuItem(String item,int v)
    {	addMenuItem(null,item,v);
    }
    /**
     * add a menu item with an associated integer
     * @param item
     * @param v
     */
    public void addMenuItem(Text item,int v)
    {	addMenuItem(null,item,v);
    }
    
    /**
     * add a menu item with an associated object
     * @param item
     * @param v
     */
    public void addMenuItem(String item,Object v)
    {	addMenuItem(null,item,v);
    }
    
    /**
     * add a menu item with an associated object
     * @param item
     * @param v
     */
    public void addMenuItem(Text item,Object v)
    {	addMenuItem(null,item,v);
    }
    
    /**
     * add a submenu item with an associated integer
     * @param m
     * @param item
     * @param v
     */
    public void addMenuItem(MenuInterface m,String item,int v)
    {	addMenuItem(m,item,Integer.valueOf(v));
    }
    /**
     * add a submenu item with an associated integer
     * 
     * @param m
     * @param item
     * @param v
     */
    public void addMenuItem(MenuInterface m,Text item,int v)
    {	addMenuItem(m,item,Integer.valueOf(v));
    }
    
    /**
     * add a submenu to the main menu
     * @param m
     * @param sub
     */
    public void addMenuItem(MenuInterface m,MenuInterface sub)
    {	((m==null) ? menu : m).add(sub);
    }
    /**
     * add an array of nameproviders
     */
    public void addMenuItem(NameProvider... names)
    {	for(NameProvider n : names)
    	{
    	addMenuItem(n.getName(),n);
    	}
    }
    
    
    /**
     * add an item to a submenu with an associated object
     * @param m
     * @param item
     * @param v
     */
    public void addMenuItem(MenuInterface m,String item,Object v)
    {	NativeMenuItemInterface newitem = ((m==null) ? menu : m).add(item,listener);
		popupTarget.put(newitem,(v==null)?nullValue:v);
    } 
    
    public void addMenuItem(MenuInterface m,Text item,Object v)
    {	NativeMenuItemInterface newitem = ((m==null) ? menu : m).add(item,this,listener);
 		popupTarget.put(newitem,(v==null)?nullValue:v);
    } 
    
    /** add a submenu to the main menu
     * 
     * @param item
     */
    public void addMenuItem(MenuInterface item)
    {	//popupTarget.put(item,menu);
    	menu.add(item);
    }
    /**
     * start a new popup cycle for a menu that will appear on window
     * @param window the window where the menu will appear
     * @param listen the listener for action events associated with the menu
     */
    public void newPopupMenu(MenuParentInterface window,ActionListener listen,SimpleObserver o)
    {	popupTarget = new Hashtable<NativeMenuItemInterface,Object>();	
    	listener = listen;
    	observer = o;
    	parent = window;
    	// simple menus always go through the swing interface, but will be short circuited to the simplemenu instead of used
    	menu = (useSimpleMenu() || useSwing) ? new bsSwingMenu(null,font) : new bsAwtMenu(null,font);
    	//G.print("\nsimple "+useSimpleMenu+" swing "+useSwing+" lightweight "+lightweightMenus);
    }
    private Font font = null;
    public void setFont(Font f)
    {	font = f;
    	menu.setFont(f);
    }
    public Font getFont()
    {
    	return menu.getFont();
    }
    public void newPopupMenu(MenuParentInterface window,ActionListener listen)
    {
    	newPopupMenu(window,listen,null);
    }
    /**
     * show a memu consisting of all the enums in the list, normally
     * derived from <class>.values()
     * @param x
     * @param y
     * @param e some enum that implements EnumMenu
     */
    public void show(int x,int y,EnumMenu e[])
    {	InternationalStrings s = G.getTranslations();
    	for(EnumMenu m : e) { 
    		String message = m.menuItem();
    		if(message!=null) { addMenuItem(s.get(message),m);}
    	}
    	show(x,y);
    }

    /**
     * create a submenu to be added to the menu
     * @param msg
     * @return the handle for the new submenu
     */
    public MenuInterface newSubMenu(String msg)
    {	return(menu.newSubMenu(msg));
    }
    
    public void actionPerformed(ActionEvent e)
    {	if(selectMenuTarget(e.getSource()))
    	{
    	if(observer!=null) { observer.update(this,null, rawValue); }
    	}
    }

    public exCanvas getCanvas() {
		return null;
	}
    
}
