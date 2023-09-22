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

import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Hashtable;

//
// maintains a list of users accessible to a chat, including some that
// are virtual users to whom messages are attributed.
//
public class UserBank
{
    private Hashtable<SimpleUser,SimpleUser> users = new Hashtable<SimpleUser,SimpleUser>();
    SimpleUser toSingleUser = null;
    SimpleUser prevToSingle = null;
    SimpleUser myUser = null;
    public SimpleUser getMyUser() { return(myUser); }
    public SimpleUser getToSingleUser() { return(toSingleUser); }
    
    private PopupManager destMenu = new PopupManager();
    public PopupManager getDestMenu() { return(destMenu); }
    
    // add users to the final menu
	private void addUsers(SimpleUser us[],MenuInterface menu,int from,int to)
	{	for(int i=from;i<=to;i++) 
		{ SimpleUser v = us[i];
		  String name = v.name();
		  destMenu.addMenuItem(menu,name,v);
		}
	}
    public void selectDestination(MenuParentInterface parent,ActionListener listen,String prompt,int x,int y)
    {	int maxsz = users.size();
    	int sz = 0;
    	SimpleUser us[] = new SimpleUser[maxsz];
    	destMenu.newPopupMenu(parent,listen);
    	destMenu.addMenuItem(prompt,null);		// to all
    	if(prevToSingle!=null) 	// to that one special user
    		{ destMenu.addMenuItem(prevToSingle.name(),prevToSingle); 
    		}
    	// collect a vector of all the other known users
    	for(Enumeration<SimpleUser> keys=users.elements();
    	    keys.hasMoreElements();)
    	{	SimpleUser thiskey = keys.nextElement();
    		int num = thiskey.channel();
    		if((num>0) 
    				&& (num<ChatInterface.LASTUCHANNEL) 
    				&& (thiskey!=myUser) 
    				&& (thiskey!=prevToSingle))
    		{
    		us[sz++]=thiskey;
     		}
      	}
    	
    	if(sz>0)
    	{
    	Sort.sort(us,0,sz-1);		// sort alphabetically
    	
    	if(sz<20) 
    	{ // simple case with only a few users
    		addUsers(us,null,0,sz-1); 
    	}
    	else 
    	{ int nSegs = (int)Math.sqrt(sz);
    	  int setSz = nSegs;
    	  char start = 'A';
    	  int startIdx = 0;
    	  // break the list into segments. Label the segments with the letters included
    	  while(startIdx<sz)
    	  {	int endIdx = Math.min(sz-1,startIdx+setSz);
    	    String end = us[endIdx].name();
    	    char up = Character.toUpperCase(end.charAt(0));
    	    // find the break in alphabet.  We're screwed if everone has the same first letter
    	    while((endIdx+1)<sz) 
    	    	{ String endplus = us[endIdx+1].name();
    	    	  if(Character.toUpperCase(endplus.charAt(0)) == up) { endIdx++; } else { break; }
    	    	}
    	    // if only a few orphans, include them
    	    if(endIdx+4>=sz) 
    	    	{ endIdx=sz-1; 
    	    	  end = us[endIdx].name();
    	    	  up = Character.toUpperCase(end.charAt(0));
    	    	}	
    	    // add a submenu of some range of users
    	    MenuInterface m = destMenu.newSubMenu(""+start+" .. "+up);
    	    addUsers(us,m,startIdx,endIdx);
    	    destMenu.addMenuItem(m);
    	    
    	    startIdx = endIdx+1;
    	    start = (char)(up+1);
    	  }
    	}}
     	destMenu.show(x,y);
    	    
    }
  
    public void setSingleSend(SimpleUser n,boolean temp) 
		{ if((toSingleUser!=null)&& !temp) { prevToSingle=n; }
		  toSingleUser=n;
		}
    public synchronized SimpleUser getUser(int inNum)
    {	return(users.get(new SimpleUser(inNum,"probe")));
    }
    public synchronized SimpleUser removeUser(int inNum)
    {	SimpleUser prev = getUser(inNum);
    	if(prev!=null)
    		{ users.remove(prev);
    		  if(toSingleUser==prev) { setSingleSend(null,true); }
    		  if(prevToSingle==prev) { prevToSingle=null; }
    		}
    	return(prev);
    }
    public synchronized SimpleUser setUser(int inNum, String inName)
    {	SimpleUser us = getUser(inNum);
    	if(us==null) 
    		{   us = new SimpleUser(inNum,inName);
    			users.put(us,us);
    		}
    	else { us.setName(inName); }
     	return(us);
     }
    public void setMyUser(int inNum,String name)
    {
    	myUser = setUser(inNum,name);
    }

}
